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
