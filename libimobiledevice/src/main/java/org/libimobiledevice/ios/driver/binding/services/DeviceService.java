/*
 * Copyright 2012-2013 eBay Software Foundation and ios-driver committers
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */
package org.libimobiledevice.ios.driver.binding.services;

import org.libimobiledevice.ios.driver.binding.exceptions.SDKException;
import java.util.HashMap;
import java.util.Map;
import org.libimobiledevice.ios.driver.binding.raw.JNAInit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import static org.libimobiledevice.ios.driver.binding.exceptions.SDKErrorCode.throwIfNeeded;
import static org.libimobiledevice.ios.driver.binding.raw.ImobiledeviceSdkLibrary.sdk_idevice_event_subscribe;
import static org.libimobiledevice.ios.driver.binding.raw.ImobiledeviceSdkLibrary.sdk_idevice_event_unsubscribe;

public class DeviceService {
    private static final Logger LOG = LoggerFactory.getLogger(DeviceService.class);

    static {
        JNAInit.init();
    }

    public static final DeviceService INSTANCE = new DeviceService();

    private final Map<String, IOSDevice> devices = new HashMap<>();

    public synchronized static IOSDevice get(String uuid) throws SDKException {
        if (uuid == null) {
            throw new IllegalArgumentException("device id cannot be null.");
        }
        IOSDevice res = INSTANCE.devices.get(uuid);
        if (res == null) {
            res = new IOSDevice(uuid);
            INSTANCE.devices.put(uuid, res);
        }
        return res;
    }

    public static void free() throws SDKException {
        for (String uuid : INSTANCE.devices.keySet()) {
            IOSDevice device = INSTANCE.get(uuid);
            device.free();
        }
        INSTANCE.devices.clear();
    }

    public static void remove(String uuid) {
        DeviceService.INSTANCE.devices.remove(uuid);
    }

    public void startDetection(DeviceCallBack cb) throws SDKException {
        if (cb == null) {
            cb = new DeviceCallBack() {
                @Override
                protected void onDeviceAdded(String uuid) {
                    LOG.info("Added: {}",uuid);
                }

                @Override
                protected void onDeviceRemoved(String uuid) {
                    LOG.info("Removed: {}",uuid);
                }
            };
        }
        throwIfNeeded(sdk_idevice_event_subscribe(cb, null));
    }

    public void stopDetection() throws SDKException {
        throwIfNeeded(sdk_idevice_event_unsubscribe());
    }
}
