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
package com.tascape.reactor.ios.suite;

import com.tascape.reactor.ios.driver.UiAutomationDevice;
import com.tascape.reactor.ios.model.UIAException;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;
import org.libimobiledevice.ios.driver.binding.exceptions.SDKException;

/**
 * This suite supports plug-n-play for multiple DEVICES.
 *
 * @author linsong wang
 */
public interface UiAutomationSuite {

    BlockingQueue<UiAutomationDevice> DEVICES
        = new ArrayBlockingQueue<>(UiAutomationDevice.getAllDevices().size(), true, UiAutomationDevice.getAllDevices());

    default UiAutomationDevice getAvailableDevice() throws SDKException, InterruptedException {
        UiAutomationDevice device = DEVICES.poll(1, TimeUnit.SECONDS);
        if (device == null) {
            throw new UIAException("Cannot find a device available");
        }
        return device;
    }

    default int getNumberOfDevices() {
        return DEVICES.size();
    }
}
