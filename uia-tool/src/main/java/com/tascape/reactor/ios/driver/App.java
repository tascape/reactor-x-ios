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

import com.tascape.reactor.ios.tools.UiInteraction;
import com.tascape.reactor.driver.EntityDriver;
import com.tascape.reactor.ios.model.CacheLookup;
import com.tascape.reactor.ios.model.FindBy;
import com.tascape.reactor.ios.model.UIAElement;
import java.lang.reflect.Field;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author linsong wang
 */
@SuppressWarnings("ProtectedField")
public abstract class App extends EntityDriver {
    private static final Logger LOG = LoggerFactory.getLogger(App.class);

    protected UiAutomationDevice device;

    public abstract String getBundleId();

    public abstract int getLaunchDelayMillis();

    protected String version;

    public <W extends AppWindow> W getWindow(Class<W> window) throws InstantiationException, IllegalAccessException {
        W win = window.newInstance();
        win.app = this;
        win.device = this.getDevice();

        for (Field f : window.getDeclaredFields()) {
            FindBy fb = f.getAnnotation(FindBy.class);
            if (fb != null) {
                LOG.debug("process FindBy annotated field {}", f.getName());
                f.setAccessible(true);

                if (!UIAElement.class.isAssignableFrom(f.getType())) {
                    LOG.warn("{} annotation only works with {}, and its child classes", FindBy.class.getName(),
                        UIAElement.class.getName());
                    continue;
                }
                UIAElement ele = (UIAElement) f.getType().newInstance();
                ele.setIsFindby(true);
                String jsPath = fb.jsPath();
                if (StringUtils.isNotEmpty(jsPath)) {
                    ele.setJsPath(jsPath);
                }
                String name = fb.name();
                if (StringUtils.isNotEmpty(name)) {
                    ele.setName(name);
                }
                String partialName = fb.partialName();
                if (StringUtils.isNotEmpty(partialName)) {
                    ele.setPartialName(partialName);
                }
                f.set(win, f.getType().cast(ele));

                LOG.debug("process CacheLookup annotation");
                ele.setIsCacheLookup(f.getAnnotation(CacheLookup.class) != null);
            }
        }
        return win;
    }

    @Override
    public String getVersion() {
        if (StringUtils.isBlank(version)) {
            try {
                version = device.getAppVersion(getBundleId());
            } catch (Exception ex) {
                LOG.warn(ex.getMessage());
                version = "";
            }
        }
        return version;
    }

    public int getLaunchTries() {
        return 2;
    }

    public void launch() throws Exception {
        device.getDebugService().killApp(this.getBundleId());

        device.setAlertAutoDismiss();
        device.start(this.getName(), getLaunchTries(), getLaunchDelayMillis());
    }

    public UiAutomationDevice getDevice() {
        return device;
    }

    public void setDevice(UiAutomationDevice device) {
        this.device = device;
    }

    public void interactManually() throws Exception {
        interactManually(30);
    }

    /**
     * The method starts a GUI to let an user inspect element tree and take screenshot when the user is interacting
     * with the app-under-task manually. It is also possible to run UI Automation instruments JavaScript via this UI.
     * Please make sure to set timeout long enough for manual interaction.
     *
     * @param timeoutMinutes timeout in minutes to fail the manual steps
     *
     * @throws Exception if case of error
     */
    public void interactManually(int timeoutMinutes) throws Exception {
        UiInteraction ui = new UiInteraction(device);
        ui.start(timeoutMinutes);
    }
}
