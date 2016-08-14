/*
 * Copyright 2015 - 2016 Nebula Bay.
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
package com.tascape.reactor.ios.driver;

import com.tascape.reactor.ios.model.UIA;
import org.libimobiledevice.ios.driver.binding.exceptions.SDKException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.tascape.reactor.SystemConfiguration;
import com.tascape.reactor.Utils;
import com.tascape.reactor.ios.comm.Instruments;
import com.tascape.reactor.ios.model.DeviceOrientation;
import com.tascape.reactor.ios.model.UIAAlert;
import com.tascape.reactor.ios.model.UIAApplication;
import com.tascape.reactor.ios.model.UIAElement;
import com.tascape.reactor.ios.model.UIAException;
import com.tascape.reactor.ios.model.UIAKeyboard;
import com.tascape.reactor.ios.model.UIATarget;
import com.tascape.reactor.ios.model.UIAWindow;
import java.awt.Desktop;
import java.awt.Dimension;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;

/**
 *
 * @author linsong wang
 */
public class UiAutomationDevice extends LibIMobileDevice implements UIATarget, UIAApplication {
    private static final Logger LOG = LoggerFactory.getLogger(UiAutomationDevice.class);

    private static final List<UiAutomationDevice> DEVICES = new ArrayList<>();

    public static final String SYSPROP_TIMEOUT_SECOND = "reactor.driver.ios.TIMEOUT_SECOND";

    public static final String TRACE_TEMPLATE = "/Applications/Xcode.app/Contents/Applications/Instruments.app/Contents"
        + "/PlugIns/AutomationInstrument.xrplugin/Contents/Resources/Automation.tracetemplate";

    public static final int TIMEOUT_SECOND
        = SystemConfiguration.getInstance().getIntProperty(SYSPROP_TIMEOUT_SECOND, 120);

    private Instruments instruments;

    private Dimension screenDimension;

    private UIAWindow currentWindow;

    private String alertHandler = "";

    public static synchronized List<UiAutomationDevice> getAllDevices() {
        if (DEVICES.isEmpty()) {
            List<String> UUIDS = LibIMobileDevice.getAllUuids();
            for (String uuid : UUIDS) {
                try {
                    DEVICES.add(new UiAutomationDevice(uuid));
                } catch (SDKException ex) {
                    LOG.warn("Cannnot debug device {}", uuid, ex);
                }
            }
            if (DEVICES.isEmpty()) {
                throw new UIAException("Cannot debug any attached device");
            }
        }
        return DEVICES;
    }

    public UiAutomationDevice() throws SDKException {
        this(LibIMobileDevice.getAllUuids().get(0));
    }

    public UiAutomationDevice(String uuid) throws SDKException {
        super(uuid);
    }

    /**
     * Launches app by name, and verifies the main widown is on screen.
     *
     * @param appName     app name
     * @param delayMillis wait for app to start
     *
     * @throws Exception if app does not launch
     */
    public void start(String appName, int delayMillis) throws Exception {
        this.start(appName, 1, delayMillis);
    }

    /**
     * Launches app by name, and verifies the main window is on screen.
     *
     * @param appName     app name
     * @param tries       number of tries of instruments command
     * @param delayMillis wait for app to start
     *
     * @throws Exception if app does not launch
     */
    public void start(String appName, int tries, int delayMillis) throws Exception {
        if (instruments != null) {
            instruments.disconnect();
        } else {
            instruments = new Instruments(getUuid(), appName);
        }
        if (StringUtils.isNotEmpty(alertHandler)) {
            instruments.setPreTargetJavaScript(alertHandler);
        }
        for (int i = 0; i < tries; i++) {
            instruments.connect();
            Utils.sleep(delayMillis, "Wait for app to start");
            long end = System.currentTimeMillis() + TIMEOUT_SECOND * 500;
            while (end > System.currentTimeMillis()) {
                try {
                    if (this.instruments.runJavaScript("app.logElement();").stream()
                        .filter(l -> l.contains(UIAApplication.class.getSimpleName())).findAny().isPresent()) {
                        return;
                    }
                } catch (Exception ex) {
                    LOG.warn("cannot start app", ex);
                    Thread.sleep(5000);
                }
            }
        }
        throw new UIAException("Cannot start app ");
    }

    public void stop() {
        if (instruments != null) {
            instruments.shutdown();
        }
    }

    public void install(App app) {
        app.setDevice(this);
    }

    public void setAlertHandler(String javaScript) {
        this.alertHandler = javaScript;
    }

    public List<String> runJavaScript(String javaScript) {
        return instruments.runJavaScript(javaScript);
    }

    public List<String> loadElementTree() {
        return instruments.runJavaScript("window.logElementTree();");
    }

    /**
     * Gets the screen size in points.
     * http://www.paintcodeapp.com/news/ultimate-guide-to-iphone-resolutions
     *
     * @return the screen size in points
     */
    public Dimension getDisplaySize() {
        if (screenDimension == null) {
            screenDimension = loadDisplaySize();
        }
        return screenDimension;
    }

    /**
     * Checks if an element is valid (exists) on current UI. This requires a call to the underlying Accessibility
     * framework to ensure the validity of the result. See Apple UIAElement Class Reference .
     *
     * @param javaScript such as "window.tabBars()['MainTabBar']"
     *
     * @return true if element identified by javascript exists
     */
    public boolean checkIsValid(String javaScript) {
        String js = "var e = " + javaScript + "; UIALogger.logMessage(e.checkIsValid().toString());";
        String res = Instruments.getLogMessage(instruments.runJavaScript(js));
        return Boolean.parseBoolean(res);
    }

    /**
     * Checks if an element exists on current UI, based on element type.
     *
     * @param <T>        sub-class of UIAElement
     * @param javaScript such as "window.tabBars()['MainTabBar']"
     * @param type       type of uia element, such as UIATabBar
     *
     * @return true if element identified by javascript exists
     */
    public <T extends UIAElement> boolean doesElementExist(String javaScript, Class<T> type) {
        return checkIsValid(javaScript);
    }

    /**
     * Checks if an element exists on current UI, based on element type and name.
     *
     * @param <T>        sub-class of UIAElement
     * @param javaScript the javascript that uniquely identify the element, such as "window.tabBars()['MainTabBar']",
     *                   or "window.elements()[1].buttons()[0]"
     * @param type       type of uia element, such as UIATabBar
     * @param name       name of an element, such as "MainTabBar"
     *
     * @return true if element identified by javascript exists
     */
    public <T extends UIAElement> boolean doesElementExist(String javaScript, Class<T> type, String name) {
        String js = "var e = " + javaScript + "; e.logElement();";
        return instruments.runJavaScript(js).stream()
            .filter(line -> line.contains(type.getSimpleName()))
            .filter(line -> StringUtils.isEmpty(name) ? true : line.contains(name))
            .findFirst().isPresent();
    }

    /**
     * Checks if an element exists on current UI, based on element type and name. This method loads full element tree.
     *
     * @param <T>  sub-class of UIAElement
     * @param type type of uia element, such as UIATabBar
     * @param name name of an element, such as "MainTabBar"
     *
     * @return true if element identified by type and name exists, or false if timeout
     */
    public <T extends UIAElement> boolean doesElementExist(Class<T> type, String name) {
        return mainWindow().findElement(type, name) != null;
    }

    /**
     * Waits for an element exists on current UI, based on element type.
     *
     * @param <T>        sub-class of UIAElement
     * @param javaScript the javascript that uniquely identify the element, such as "window.tabBars()['MainTabBar']",
     *                   or "window.elements()[1].buttons()[0]"
     * @param type       type of uia element, such as UIATabBar
     *
     * @return true if element identified by javascript exists, or false if timeout
     *
     * @throws java.lang.InterruptedException in case of interruption
     */
    public <T extends UIAElement> boolean waitForElement(String javaScript, Class<T> type) throws InterruptedException {
        return this.waitForElement(javaScript, type, null);
    }

    /**
     * Waits for an element exists on current UI, based on element type and name.
     *
     * @param <T>        sub-class of UIAElement
     * @param javaScript the javascript that uniquely identify the element, such as "window.tabBars()['MainTabBar']",
     *                   or "window.elements()[1].buttons()[0]"
     * @param type       type of uia element, such as UIATabBar
     * @param name       name of an element, such as "MainTabBar"
     *
     * @return true if element identified by javascript exists, or false if timeout
     *
     * @throws java.lang.InterruptedException in case of interruption
     */
    public <T extends UIAElement> boolean waitForElement(String javaScript, Class<T> type, String name)
        throws InterruptedException {
        long end = System.currentTimeMillis() + TIMEOUT_SECOND * 1000;
        while (System.currentTimeMillis() < end) {
            if (doesElementExist(javaScript, type, name)) {
                return true;
            }
            Utils.sleep(1000, "wait for " + type + "[" + name + "]");
        }
        return false;
    }

    /**
     * Waits for an element exists on current UI, based on element type and name. This method loads full element tree.
     *
     * @param <T>  sub-class of UIAElement
     * @param type type of uia element, such as UIATabBar
     * @param name name of an element, such as "MainTabBar"
     *
     * @return element object if element identified by type and name exists, or null if timeout
     *
     * @throws java.lang.InterruptedException in case of interruption
     */
    public <T extends UIAElement> T waitForElement(Class<T> type, String name) throws InterruptedException {
        long end = System.currentTimeMillis() + TIMEOUT_SECOND * 1000;
        while (System.currentTimeMillis() < end) {
            try {
                T element = mainWindow().findElement(type, name);
                if (element != null) {
                    return element;
                }
            } catch (Exception ex) {
                LOG.warn("{}", ex.getMessage());
                Thread.sleep(10000);
            }
            Utils.sleep(5000, "wait for " + type.getSimpleName() + "[" + name + "]");
        }
        return null;
    }

    /**
     * Waits for an element disappear on current UI, based on element type and name. This method loads full element
     * tree.
     *
     * @param <T>  sub-class of UIAElement
     * @param type type of uia element, such as UIATabBar
     * @param name name of an element, such as "MainTabBar"
     *
     * @throws java.lang.InterruptedException in case of interruption
     */
    public <T extends UIAElement> void waitForNoElement(Class<T> type, String name) throws InterruptedException {
        long end = System.currentTimeMillis() + TIMEOUT_SECOND * 1000;
        while (System.currentTimeMillis() < end) {
            try {
                T element = mainWindow().findElement(type, name);
                if (element == null) {
                    return;
                }
            } catch (Exception ex) {
                LOG.warn("{}", ex.getMessage());
                Thread.sleep(10000);
            }
            Utils.sleep(5000, "wait for no " + type.getSimpleName() + "[" + name + "]");
        }
    }

    public <T extends UIAElement> String getElementName(String javaScript, Class<T> type) {
        String js = "var e = " + javaScript + "; e.logElement();";
        String line = instruments.runJavaScript(js).stream()
            .filter(l -> l.contains(type.getSimpleName())).findFirst().get();
        return UIA.newInstance().parseUIAElement(line).name();
    }

    public <T extends UIAElement> String getElementValue(String javaScript, Class<T> type) {
        String js = "var e = " + javaScript + "; UIALogger.logMessage(e.value());";
        return Instruments.getLogMessage(instruments.runJavaScript(js));
    }

    public void setTextField(String javaScript, String value) {
        String js = "var e = " + javaScript + "; e.setValue('" + value + "');";
        instruments.runJavaScript(js).forEach(l -> LOG.trace(l));
    }

    @Override
    public File takeDeviceScreenshot() {
        long start = System.currentTimeMillis();
        try {
            LOG.debug("Take screenshot");
            File png = this.saveIntoFile("ss", "png", "");
            String name = UUID.randomUUID().toString();
            this.captureScreenWithName(name);
            File f = FileUtils.listFiles(instruments.getUiaResultsPath().toFile(), new String[]{"png"}, true).stream()
                .filter(p -> p.getName().contains(name)).findFirst().get();
            LOG.trace("{}", f);
            FileUtils.copyFile(f, png);
            LOG.trace("time {} ms", System.currentTimeMillis() - start);
            return png;
        } catch (IOException ex) {
            throw new UIAException("Cannot take screenshot", ex);
        }
    }

    /**
     * The internal currentWindow is also updated upon the successful return of this method.
     *
     * @return a UIAWindow object representing current window element tree
     */
    @Override
    public UIAWindow mainWindow() {
        try {
            return mw();
        } catch (Exception ex) {
            LOG.warn(ex.getMessage());
        }
        return mw();
    }

    @Override
    public UIAWindow windows(int index) throws UIAException {
        long start = System.currentTimeMillis();
        List<String> lines = runJavaScript("app.windows()[" + index + "].logElementTree();");
        try {
            File f = this.saveIntoFile("window-element-tree", "txt", "");
            FileUtils.writeLines(f, lines);
        } catch (IOException ex) {
            LOG.warn(ex.getMessage());
        }
        UIAWindow window = UIA.newInstance().parseElementTree(index, lines);
        window.setDevice(this);
        LOG.trace("time {} ms", System.currentTimeMillis() - start);
        return window;
    }

    @Override
    public void captureRectWithName(Rectangle2D rect, String imageName) {
        instruments.runJavaScript("target.captureScreenWithName(,'" + imageName + "');");
    }

    @Override
    public void captureScreenWithName(String imageName) {
        instruments.runJavaScript("target.captureScreenWithName('" + imageName + "');");
    }

    @Override
    public void deactivateAppForDuration(int duration) {
        instruments.runJavaScript("UIALogger.logMessage(target.deactivateAppForDuration(" + duration + "));");
    }

    @Override
    public String model() {
        return Instruments.getLogMessage(instruments.runJavaScript("UIALogger.logMessage(target.model());"));
    }

    @Override
    public String name() {
        return Instruments.getLogMessage(instruments.runJavaScript("UIALogger.logMessage(target.name());"));
    }

    @Override
    public Rectangle2D rect() {
        List<String> lines = instruments.runJavaScript("UIALogger.logMessage(target.rect());");
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public String systemName() {
        return Instruments.getLogMessage(instruments.runJavaScript("UIALogger.logMessage(target.systemName());"));
    }

    @Override
    public String systemVersion() {
        return Instruments.getLogMessage(instruments.runJavaScript("UIALogger.logMessage(target.systemVersion());"));
    }

    @Override
    public DeviceOrientation deviceOrientation() {
        instruments.runJavaScript("UIALogger.logMessage(target.deviceOrientation());");
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public void setDeviceOrientation(DeviceOrientation orientation) {
        instruments.runJavaScript("target.setDeviceOrientation(" + orientation.ordinal() + ");");
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public void setLocation(double latitude, double longitude) {
        instruments.runJavaScript("target.setLocation({latitude:" + latitude + ", longitude:" + longitude + "});");
    }

    @Override
    public void clickVolumeDown() {
        this.instruments.runJavaScript("target.clickVolumeDown();");
    }

    @Override
    public void clickVolumeUp() {
        this.instruments.runJavaScript("target.clickVolumeUp();");
    }

    @Override
    public void holdVolumeDown(int duration) {
        this.instruments.runJavaScript("target.holdVolumeDown(" + duration + ");");
    }

    @Override
    public void holdVolumeUp(int duration) {
        this.instruments.runJavaScript("target.holdVolumeUp(" + duration + ");");
    }

    @Override
    public void lockForDuration(int duration) {
        this.instruments.runJavaScript("target.lockForDuration(" + duration + ");");
    }

    @Override
    public void shake() {
        this.instruments.runJavaScript("target.shake();");
    }

    public void dragHalfScreenUp() {
        Dimension dimension = this.getDisplaySize();
        this.dragFromToForDuration(new Point2D.Float(dimension.width / 2, dimension.height / 2),
            new Point2D.Float(dimension.width / 2, 0), 1);
    }

    public void dragHalfScreenDown() {
        Dimension dimension = this.getDisplaySize();
        this.dragFromToForDuration(new Point2D.Float(dimension.width / 2, dimension.height / 2),
            new Point2D.Float(dimension.width / 2, dimension.height), 1);
    }

    @Override
    public void dragFromToForDuration(Point2D.Float from, Point2D.Float to, int duration) {
        this.instruments.runJavaScript("target.dragFromToForDuration(" + toCGString(from) + ", "
            + toCGString(to) + ", " + duration + ");");
    }

    @Override
    public void dragFromToForDuration(UIAElement fromElement, UIAElement toElement, int duration) {
        this.dragFromToForDuration(fromElement.getJsPath(), toElement.getJsPath(), duration);
    }

    @Override
    public void dragFromToForDuration(String fromJavaScript, String toJavaScript, int duration) {
        this.instruments.runJavaScript("var e1 = " + fromJavaScript + "; var e2 = " + toJavaScript + "; "
            + "target.dragFromToForDuration(e1, e2, " + duration + ");");
    }

    @Override
    public void doubleTap(float x, float y) {
        this.instruments.runJavaScript("target.doubleTap(" + toCGString(x, y) + ");");
    }

    @Override
    public void doubleTap(UIAElement element) {
        element.doubleTap();
    }

    @Override
    public void doubleTap(String javaScript) {
        this.instruments.runJavaScript("var e = " + javaScript + "; e.doubleTap();");
    }

    public void flickHalfScreenUp() {
        Dimension dimension = this.getDisplaySize();
        this.flickFromTo(new Point2D.Float(dimension.width / 2, dimension.height / 2),
            new Point2D.Float(dimension.width / 2, 0));
    }

    public void flickHalfScreenDown() {
        Dimension dimension = this.getDisplaySize();
        this.flickFromTo(new Point2D.Float(dimension.width / 2, dimension.height / 2),
            new Point2D.Float(dimension.width / 2, dimension.height));
    }

    @Override
    public void flickFromTo(Point2D.Float from, Point2D.Float to) {
        this.instruments.runJavaScript(
            "target.flickFromTo(" + toCGString(from) + ", " + toCGString(to) + ");");
    }

    @Override
    public void flickFromTo(UIAElement fromElement, UIAElement toElement) {
        this.flickFromTo(fromElement.getJsPath(), toElement.getJsPath());
    }

    @Override
    public void flickFromTo(String fromJavaScript, String toJavaScript) {
        this.instruments.runJavaScript("var e1 = " + fromJavaScript + "; var e2 = " + toJavaScript + "; "
            + "target.flickFromTo(e1, e2);");
    }

    @Override
    public void pinchCloseFromToForDuration(Point2D.Float from, Point2D.Float to, int duration) {
        this.instruments.runJavaScript("target.pinchCloseFromToForDuration(" + toCGString(from) + ", "
            + toCGString(to) + ", " + duration + ");");
    }

    @Override
    public void pinchCloseFromToForDuration(UIAElement fromElement, UIAElement toElement, int duration) {
        this.pinchCloseFromToForDuration(fromElement.getJsPath(), toElement.getJsPath(), duration);
    }

    @Override
    public void pinchCloseFromToForDuration(String fromJavaScript, String toJavaScript, int duration) {
        this.instruments.runJavaScript("var e1 = " + fromJavaScript + "; var e2 = " + toJavaScript + "; "
            + "target.pinchCloseFromToForDuration(e1, e2, " + duration + ");");
    }

    @Override
    public void pinchOpenFromToForDuration(Point2D.Float from, Point2D.Float to, int duration) {
        this.instruments.runJavaScript("target.pinchOpenFromToForDuration(" + toCGString(from) + ", "
            + toCGString(to) + ", " + duration + ");");
    }

    @Override
    public void pinchOpenFromToForDuration(UIAElement fromElement, UIAElement toElement, int duration) {
        this.pinchOpenFromToForDuration(fromElement.getJsPath(), toElement.getJsPath(), duration);
    }

    @Override
    public void pinchOpenFromToForDuration(String fromJavaScript, String toJavaScript, int duration) {
        this.instruments.runJavaScript("var e1 = " + fromJavaScript + "; var e2 = " + toJavaScript + "; "
            + "target.pinchOpenFromToForDuration(e1, e2, " + duration + ");");
    }

    @Override
    public void tap(float x, float y) {
        this.instruments.runJavaScript("target.tap(" + toCGString(x, y) + ");");
    }

    public void tap(Class<? extends UIAElement> type, String name) {
        UIAElement element = this.mainWindow().findElement(type, name);
        this.tap(element);
    }

    @Override
    public void tap(UIAElement element) {
        element.tap();
    }

    @Override
    public void tap(String javaScript) {
        this.instruments.runJavaScript("var e = " + javaScript + "; e.tap();");
    }

    @Override
    public void touchAndHold(Point2D.Float point, int duration) {
        this.instruments.runJavaScript("target.touchAndHold(" + toCGString(point) + ", " + duration + ");");
    }

    @Override
    public void touchAndHold(UIAElement element, int duration) {
        this.instruments.runJavaScript("var e = " + element.getJsPath() + "; e.touchAndHold(e, " + duration + ");");
    }

    @Override
    public void touchAndHold(String javaScript, int duration) {
        this.instruments.runJavaScript("var e = " + javaScript + "; target.touchAndHold(e, " + duration + ");");
    }

    @Override
    public void popTimeout() {
        this.instruments.runJavaScript("target.popTimeout();");
    }

    @Override
    public void pushTimeout(int timeoutValue) {
        this.instruments.runJavaScript("target.pushTimeout(" + timeoutValue + ");");
    }

    @Override
    public void setTimeout(int timeout) {
        this.instruments.runJavaScript("target.setTimeout(" + timeout + ");");
    }

    /**
     * Unsupported yet.
     *
     * @return int
     */
    @Override
    public int timeout() {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public void delay(int timeInterval) {
        this.instruments.runJavaScript("target.delay(" + timeInterval + ");");
    }

    /**
     * Not supported yet.
     *
     * @param alert alert object
     *
     * @return true/false
     */
    @Override
    public boolean onAlert(UIAAlert alert) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    public void setAlertAutoDismiss() {
        this.alertHandler
            = "UIATarget.onAlert = function onAlert(alert) {UIALogger.logWarning(alert.name()); return false;}";
    }

    public List<String> logElementTree() {
        return instruments.runJavaScript("window.logElementTree();");
    }

    public Instruments getInstruments() {
        return instruments;
    }

//    @Override
//    public String bundleID() {
//        String js = "UIALogger.logMessage(app.bundleID());";
//        return Instruments.getLogMessage(instruments.runJavaScript(js));
//    }
    @Override
    public UIAKeyboard keyboard() {
        return UIAApplication.super.getKeyboard(this);
    }

    public UIAWindow getCurrentWindow() {
        return currentWindow;
    }

//    @Override
//    public String version() {
//        String js = "UIALogger.logMessage(app.version());";
//        return Instruments.getLogMessage(instruments.runJavaScript(js));
//    }
    private Dimension loadDisplaySize() {
        List<String> lines = this.instruments.runJavaScript("window.logElement();");
        Dimension dimension = new Dimension();
        String line = lines.stream().filter((l) -> (l.startsWith("UIAWindow"))).findFirst().get();
        if (StringUtils.isNotEmpty(line)) {
            String s = line.split("\\{", 2)[1].replaceAll("\\{", "").replaceAll("\\}", "");
            String[] ds = s.split(",");
            dimension.setSize(Integer.parseInt(ds[2].trim()), Integer.parseInt(ds[3].trim()));
        }
        return dimension;
    }

    private UIAWindow mw() {
        long start = System.currentTimeMillis();
        List<String> lines = loadElementTree();
        try {
            File f = this.saveIntoFile("window-element-tree", "txt", "");
            FileUtils.writeLines(f, lines);
        } catch (IOException ex) {
            LOG.warn(ex.getMessage());
        }
        UIAWindow window = UIA.newInstance().parseElementTree(lines);
        window.setDevice(this);
        this.currentWindow = window;
        LOG.trace("time {} ms", System.currentTimeMillis() - start);
        return window;
    }

    public static void main(String[] args) throws SDKException {
        UiAutomationDevice d = new UiAutomationDevice();
        try {
            d.start("Movies", 5000);
            LOG.debug("model {}", d.model());

            File png = d.takeDeviceScreenshot();
            LOG.debug("png {}", png);
            Desktop.getDesktop().open(png);
        } catch (Throwable t) {
            LOG.error("", t);
        } finally {
            d.stop();
            System.exit(0);
        }
    }
}
