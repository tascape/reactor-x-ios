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
package com.tascape.reactor.ios.comm;

import com.martiansoftware.nailgun.NGContext;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Paths;
import net.sf.lipermi.handler.CallHandler;
import net.sf.lipermi.net.Client;
import org.apache.commons.io.IOUtils;
import org.slf4j.LoggerFactory;

/**
 *
 * @author linsong wang
 */
public class JavaScriptNail {
    private static final org.slf4j.Logger LOG = LoggerFactory.getLogger(JavaScriptNail.class);

    public static final String NG_CLIENT;

    static {
        InputStream in = JavaScriptNail.class.getResourceAsStream("/ng");
        File ng = Paths.get(System.getProperty("java.io.tmpdir")).resolve(System.currentTimeMillis() + "")
            .resolve("ng").toFile();
        File parent = ng.getParentFile();
        try {
            if (!(parent.mkdirs() && ng.createNewFile())) {
                throw new RuntimeException("Cannot create ng client file " + ng);
            }
        } catch (IOException ex) {
            throw new RuntimeException("Cannot create ng client file " + ng, ex);
        }
        ng.deleteOnExit();
        try {
            IOUtils.copy(in, new FileOutputStream(ng));
        } catch (IOException ex) {
            throw new RuntimeException("Cannot copy ng client file", ex);
        }
        if (!ng.setExecutable(true)) {
            throw new RuntimeException("Cannot mark ng client executable " + ng);
        }
        NG_CLIENT = ng.getAbsolutePath();
    }

    public static void nailMain(NGContext context) throws Exception {
        int port = Integer.parseInt(context.getArgs()[0]);
        CallHandler callHandler = new CallHandler();
        Client client = new Client("localhost", port, callHandler);
        JavaScriptServer jss = JavaScriptServer.class.cast(client.getGlobal(JavaScriptServer.class));
        String js = jss.retrieveJavaScript();
        System.out.println(js);
        client.close();
    }
}
