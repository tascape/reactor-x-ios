/*
 * Copyright 2016 tascape.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.tascape.qa.th.ios.comm;

import com.google.common.collect.Lists;
import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import org.apache.commons.exec.CommandLine;
import org.apache.commons.exec.DefaultExecuteResultHandler;
import org.apache.commons.exec.ExecuteStreamHandler;
import org.apache.commons.exec.ExecuteWatchdog;
import org.apache.commons.exec.Executor;
import org.apache.commons.io.FileUtils;
import org.libimobiledevice.ios.driver.binding.exceptions.SDKException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.martiansoftware.nailgun.NGServer;
import com.tascape.qa.th.SystemConfiguration;
import com.tascape.qa.th.Utils;
import com.tascape.qa.th.comm.EntityCommunication;
import com.tascape.qa.th.ios.driver.UiAutomationDevice;
import net.sf.lipermi.exception.LipeRMIException;
import net.sf.lipermi.handler.CallHandler;
import net.sf.lipermi.net.Server;
import com.tascape.qa.th.ios.model.UIAException;
import com.tascape.qa.th.libx.DefaultExecutor;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Observable;
import java.util.Observer;
import java.util.UUID;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.TimeUnit;
import org.apache.commons.lang3.StringUtils;

/**
 *
 * @author linsong wang
 */
public class Instruments extends EntityCommunication implements JavaScriptServer, Observer {
    private static final Logger LOG = LoggerFactory.getLogger(Instruments.class);

    public static final String SYSPROP_TIMEOUT_SECOND = "qa.th.driver.ios.TIMEOUT_SECOND";

    public static final String INSTRUMENTS_ERROR = "Error:";

    public static final String TRACE_TEMPLATE = "/Applications/Xcode.app/Contents/Applications/Instruments.app/Contents"
        + "/PlugIns/AutomationInstrument.xrplugin/Contents/Resources/Automation.tracetemplate";

    public static final int JAVASCRIPT_TIMEOUT_SECOND
        = SystemConfiguration.getInstance().getIntProperty(SYSPROP_TIMEOUT_SECOND, 30);

    private static final String INSTRUMENTS_POISON = UUID.randomUUID().toString();

    private final SynchronousQueue<String> javaScriptQueue = new SynchronousQueue<>();

    private final BlockingQueue<String> responseQueue = new ArrayBlockingQueue<>(5000);

    private int ngPort;

    private int rmiPort;

    private NGServer ngServer;

    private Server rmiServer;

    private ExecuteWatchdog instrumentsDog;

    private ESH instrumentsStreamHandler;

    private final String uuid;

    private final String appName;

    private String preTargetJavaScript = "";

    public static String getLogMessage(List<String> lines) {
        String line = lines.stream().filter(l -> StringUtils.contains(l, "Default:")).findFirst().get();
        return line.substring(line.indexOf("Default: ") + 9);
    }

    public Instruments(String uuid, String appName) throws SDKException {
        this.uuid = uuid;
        this.appName = appName;
    }

    public void setPreTargetJavaScript(String javaScript) {
        this.preTargetJavaScript = javaScript;
    }

    @Override
    public void connect() throws Exception {
        LOG.info("Start app {} on {}", appName, uuid);
        ngServer = this.startNailGunServer();
        rmiServer = this.startRmiServer();
        instrumentsDog = this.startInstrumentsServer(appName);
    }

    @Override
    public void disconnect() {
        responseQueue.clear();
        if (instrumentsDog != null) {
            LOG.info("Stop {}", uuid);
            instrumentsStreamHandler.deleteObservers();
            instrumentsDog.stop();
            instrumentsDog.killedProcess();
        }
        if (ngServer != null) {
            ngServer.shutdown(false);
        }
        if (rmiServer != null) {
            rmiServer.close();
        }
    }

    public List<String> runJavaScript(String javaScript) throws UIAException {
        if (responseQueue.contains(INSTRUMENTS_POISON)) {
            throw new UIAException("Instruments error");
        }
        responseQueue.clear();
        String reqId = UUID.randomUUID().toString();
        LOG.trace("sending js {}", javaScript);
        try {
            javaScriptQueue.offer("UIALogger.logMessage('" + reqId + " start');", JAVASCRIPT_TIMEOUT_SECOND,
                TimeUnit.SECONDS);
            javaScriptQueue.offer(javaScript, JAVASCRIPT_TIMEOUT_SECOND, TimeUnit.SECONDS);
            javaScriptQueue.offer("UIALogger.logMessage('" + reqId + " stop');", JAVASCRIPT_TIMEOUT_SECOND,
                TimeUnit.SECONDS);
        } catch (InterruptedException ex) {
            throw new UIAException("Interrupted", ex);
        }
        while (true) {
            String res;
            try {
                res = this.responseQueue.poll(JAVASCRIPT_TIMEOUT_SECOND, TimeUnit.SECONDS);
            } catch (InterruptedException ex) {
                throw new UIAException("Interrupted", ex);
            }
            if (res == null) {
                throw new UIAException("no response from device");
            }
            LOG.trace(res);
            if (res.contains(reqId + " start")) {
                break;
            }
        }
        List<String> lines = new ArrayList<>();
        while (true) {
            String res;
            try {
                res = this.responseQueue.poll(JAVASCRIPT_TIMEOUT_SECOND, TimeUnit.SECONDS);
            } catch (InterruptedException ex) {
                throw new UIAException("Interrupted", ex);
            }
            if (res == null) {
                throw new UIAException("no response from device");
            }
            if (res.contains(reqId + " start")) {
                LOG.trace(res);
                continue;
            }
            if (res.contains(reqId + " stop")) {
                LOG.trace(res);
                break;
            } else {
                lines.add(res);
            }
            if (res.contains(INSTRUMENTS_ERROR)) {
                LOG.error(res);
            } else {
                LOG.debug(res);
            }
        }
        javaScriptQueue.clear();
        if (lines.stream().filter(l -> l.contains(INSTRUMENTS_ERROR)).findAny().isPresent()) {
            throw new UIAException("instruments error");
        }
        return lines;
    }

    @Override
    public String retrieveJavaScript() throws InterruptedException {
        String js = javaScriptQueue.take();
        LOG.trace("got js {}", js);
        return js;
    }

    public boolean addInstrumentsStreamObserver(Observer observer) {
        if (this.instrumentsStreamHandler != null) {
            this.instrumentsStreamHandler.addObserver(observer);
            return true;
        }
        return false;
    }

    @Override
    public void update(Observable o, Object arg) {
        String res = arg.toString();
        try {
            responseQueue.put(res);
        } catch (InterruptedException ex) {
            LOG.error("Cannot save instruments response");
        }
    }

    private NGServer startNailGunServer() throws InterruptedException {
        NGServer ngs = new NGServer(null, 0);
        new Thread(ngs).start();
        Thread.sleep(1000);
        this.ngPort = ngs.getPort();
        LOG.trace("ng port {}", this.ngPort);
        return ngs;
    }

    private Server startRmiServer() throws IOException, LipeRMIException {
        Server rmis = new Server();
        CallHandler callHandler = new CallHandler();
        this.rmiPort = 8000;
        while (true) {
            try {
                rmis.bind(rmiPort, callHandler);
                break;
            } catch (IOException ex) {
                LOG.trace("rmi port {} - {}", this.rmiPort, ex.getMessage());
                this.rmiPort += 7;
            }
        }
        LOG.trace("rmi port {}", this.rmiPort);
        callHandler.registerGlobal(JavaScriptServer.class, this);
        return rmis;
    }

    private ExecuteWatchdog startInstrumentsServer(String appName) throws IOException, InterruptedException {
        StringBuilder sb = new StringBuilder()
            .append(this.preTargetJavaScript).append("\n")
            .append("while (1) {\n")
            .append("  var target = UIATarget.localTarget();\n")
            .append("  var host = target.host();\n")
            .append("  var app = target.frontMostApp();\n")
            .append("  var window = app.mainWindow();\n")
            .append("  var js = host.performTaskWithPathArgumentsTimeout('").append(JavaScriptNail.NG_CLIENT)
            .append("', ['--nailgun-port', '").append(ngPort).append("', '").append(JavaScriptNail.class.getName())
            .append("', '").append(rmiPort).append("'], 10000);\n")
            .append("  UIALogger.logDebug(js.stdout);\n")
            .append("  try {\n")
            .append("    var res = eval(js.stdout);\n")
            .append("  } catch(err) {\n")
            .append("    UIALogger.logError(err.message);\n")
            .append("  }\n")
            .append("}\n");
        File js = File.createTempFile("instruments-", ".js");
        FileUtils.write(js, sb);
        LOG.trace("{}\n{}", js, sb);

        CommandLine cmdLine = new CommandLine("instruments");
        cmdLine.addArgument("-t");
        cmdLine.addArgument(TRACE_TEMPLATE);
        cmdLine.addArgument("-w");
        cmdLine.addArgument(this.uuid);
        cmdLine.addArgument(appName);
        cmdLine.addArgument("-e");
        cmdLine.addArgument("UIASCRIPT");
        cmdLine.addArgument(js.getAbsolutePath());
        cmdLine.addArgument("-e");
        cmdLine.addArgument("UIARESULTSPATH");
        cmdLine.addArgument(Paths.get(System.getProperty("java.io.tmpdir")).toFile().getAbsolutePath());
        LOG.trace("{}", cmdLine.toString());
        ExecuteWatchdog watchdog = new ExecuteWatchdog(Long.MAX_VALUE);
        Executor executor = new DefaultExecutor();
        executor.setWatchdog(watchdog);
        instrumentsStreamHandler = new ESH();
        instrumentsStreamHandler.addObserver(this);
        executor.setStreamHandler(instrumentsStreamHandler);
        executor.execute(cmdLine, new DefaultExecuteResultHandler());
        return watchdog;
    }

    private static final List<String> START_ERRORS = Lists.newArrayList(new String[]{
        "Target failed to run: Device is currently locked with a passcode.",
        "Instruments Usage Error : Specified target process is invalid:",
        "Fail: The target application appears to have died"
    });

    private static final List<String> WARNINGS = Lists.newArrayList(new String[]{
        "WebKit Threading Violation - initial use of WebKit from a secondary thread.",
        "<Error>: CGImageCreateWithImageProvider: invalid image size:",
        "Attempting to change event horizon while disengage"
    });

    private class ESH extends Observable implements ExecuteStreamHandler {

        @Override
        public void setProcessInputStream(OutputStream out) throws IOException {
            LOG.trace("setProcessInputStream");
        }

        @Override
        public void setProcessErrorStream(InputStream in) throws IOException {
            BufferedReader bis = new BufferedReader(new InputStreamReader(in));
            while (true) {
                String line = bis.readLine();
                if (line == null) {
                    break;
                }
                if (isErrorToStart(line)) {
                    try {
                        Instruments.this.responseQueue.put(INSTRUMENTS_POISON);
                    } catch (InterruptedException ex) {
                        LOG.error("interrupted", ex);
                    }
                }
                if (isError(line)) {
                    LOG.error(line);
                    this.notifyObserversX("iERROR " + line);
                } else {
                    LOG.warn(line);
                    this.notifyObserversX(line);
                }
            }
        }

        @Override
        public void setProcessOutputStream(InputStream in) throws IOException {
            BufferedReader bis = new BufferedReader(new InputStreamReader(in));
            while (true) {
                String line = bis.readLine();
                if (line == null) {
                    break;
                }
                LOG.trace(line);
                this.notifyObserversX(line);
            }
        }

        @Override
        public void start() throws IOException {
            LOG.trace("start");
        }

        @Override
        public void stop() {
            LOG.trace("stop");
        }

        private boolean isErrorToStart(String line) {
            return START_ERRORS.stream().anyMatch((error) -> (line.contains(error)));
        }

        private boolean isError(String line) {
            return WARNINGS.stream().noneMatch((warn) -> (line.contains(warn)));
        }

        private void notifyObserversX(String line) {
            this.setChanged();
            this.notifyObservers(line);
            this.clearChanged();
        }
    }

    public static void main(String[] args) throws SDKException {
        String uuid = UiAutomationDevice.getAllUuids().get(0);
        Instruments d = new Instruments(uuid, "Movies");
        try {
            d.connect();
            Utils.sleep(5000, "wait for app to start");

        } catch (Throwable t) {
            LOG.error("", t);
        } finally {
            d.disconnect();
            System.exit(0);
        }
    }
}
