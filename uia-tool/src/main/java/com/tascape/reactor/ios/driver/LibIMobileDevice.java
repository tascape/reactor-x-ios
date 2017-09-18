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

import com.google.common.collect.Lists;
import com.tascape.reactor.SystemConfiguration;
import com.tascape.reactor.driver.EntityDriver;
import com.tascape.reactor.exception.EntityDriverException;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import org.apache.commons.imaging.ImageReadException;
import org.apache.commons.lang3.StringUtils;
import org.libimobiledevice.ios.driver.binding.exceptions.SDKException;
import org.libimobiledevice.ios.driver.binding.model.ApplicationInfo;
import org.libimobiledevice.ios.driver.binding.model.ProvisioningProfileInfo;
import org.libimobiledevice.ios.driver.binding.services.AppContainerService;
import org.libimobiledevice.ios.driver.binding.services.DebugService;
import org.libimobiledevice.ios.driver.binding.services.DeviceCallBack;
import org.libimobiledevice.ios.driver.binding.services.DeviceService;
import org.libimobiledevice.ios.driver.binding.services.IOSDevice;
import org.libimobiledevice.ios.driver.binding.services.ImageMountingService;
import org.libimobiledevice.ios.driver.binding.services.InformationService;
import org.libimobiledevice.ios.driver.binding.services.InstallCallback;
import org.libimobiledevice.ios.driver.binding.services.InstallerService;
import org.libimobiledevice.ios.driver.binding.services.ProvisioningService;
import org.libimobiledevice.ios.driver.binding.services.ScreenshotService;
import org.libimobiledevice.ios.driver.binding.services.SysLogLine;
import org.libimobiledevice.ios.driver.binding.services.SysLogListener;
import org.libimobiledevice.ios.driver.binding.services.SysLogService;
import org.libimobiledevice.ios.driver.binding.services.WebInspectorService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author linsong wang
 */
public class LibIMobileDevice extends EntityDriver {
    private static final Logger LOG = LoggerFactory.getLogger(LibIMobileDevice.class);

    public static final int DEVICE_DETECTION_TIMEOUT_MS = 2000;

    public static final String SYSPROP_UUIDS = "reactor.driver.ios.UUIDS";

    private static final List<String> UUIDS = new ArrayList<>();

    private final IOSDevice iosDevice;

    private SysLogListener sysLogListener;

    private final AppContainerService appContainerService;

    private final DebugService debugService;

    private final ImageMountingService imageMountingService;

    private final InformationService informationService;

    private final InstallerService installerService;

    private final ProvisioningService provisioningService;

    private final ScreenshotService screenshotService;

    private final SysLogService sysLogService;

    private final WebInspectorService webInspectorService;

    private final String uuid;

    /**
     * Gets UUID of all attached devices. Throws RuntimeException is anything goes wrong, or no device detected.
     *
     * @return list of UUIDs
     */
    public static synchronized List<String> getAllUuids() {
        if (UUIDS.isEmpty()) {
            loadAllUuids();
        }
        return UUIDS;
    }

    private static void loadAllUuids() {
        UUIDS.clear();

        String ids = SystemConfiguration.getInstance().getProperty(SYSPROP_UUIDS);
        if (StringUtils.isNotBlank(ids)) {
            LOG.info("Use specified devices from system property {}={}", SYSPROP_UUIDS, ids);
            UUIDS.addAll(Lists.newArrayList(ids.split(",")));
        } else {
            LOG.info("Detect attached devices");
            try {
                DeviceService.INSTANCE.startDetection(new DeviceCallBack() {
                    @Override
                    protected void onDeviceAdded(String uuid) {
                        LOG.info("uuid {}", uuid);
                        try {
                            UUIDS.add(uuid);
                        } catch (Exception ex) {
                            throw new RuntimeException(ex);
                        }
                    }

                    @Override
                    protected void onDeviceRemoved(String uuid) {
                        LOG.info("uuid {}", uuid);
                        UUIDS.remove(uuid);
                    }
                });
                Thread.sleep(DEVICE_DETECTION_TIMEOUT_MS);
            } catch (SDKException | InterruptedException ex) {
                throw new RuntimeException(ex);
            } finally {
                try {
                    DeviceService.INSTANCE.stopDetection();
                } catch (SDKException ex) {
                    throw new RuntimeException(ex);
                }
            }
        }
        if (UUIDS.isEmpty()) {
            throw new RuntimeException("No device detected.");
        }
    }

    LibIMobileDevice(String uuid) throws SDKException {
        this.uuid = uuid;

        this.iosDevice = DeviceService.get(uuid);
        this.debugService = new DebugService(iosDevice);
        this.appContainerService = new AppContainerService(iosDevice);
        this.imageMountingService = new ImageMountingService(iosDevice);
        this.informationService = new InformationService(iosDevice);
        this.installerService = new InstallerService(iosDevice);
        this.provisioningService = new ProvisioningService(iosDevice);
        this.screenshotService = new ScreenshotService(iosDevice);
        this.sysLogService = this.iosDevice.getSysLogService();
        this.webInspectorService = new WebInspectorService(iosDevice);

        LOG.debug("{}: {}, {}-{}", this.uuid,
            informationService.getDeviceName(),
            informationService.getDeviceType(),
            informationService.getProductVersion());
    }

    public String getUuid() {
        return uuid;
    }

    public IOSDevice getIosDevice() {
        return iosDevice;
    }

    @Override
    public String getName() {
        try {
            return informationService.getDeviceName();
        } catch (SDKException ex) {
            LOG.warn(ex.getMessage());
            return LibIMobileDevice.class.getSimpleName();
        }
    }

    @Override
    public String getVersion() {
        try {
            return informationService.getProductVersion();
        } catch (SDKException ex) {
            LOG.warn(ex.getMessage());
            return "na";
        }
    }

    @Override
    public void reset() throws Exception {
        LOG.debug("NA");
    }

    public String getType() {
        try {
            return informationService.getDeviceType();
        } catch (SDKException ex) {
            LOG.warn(ex.getMessage());
            return "na";
        }
    }
    
    public String getProductDetail() {
        return getType() + " "  + getVersion() + " " + getName();
    }

    public int launchApp(String bundleId) throws SDKException {
        return debugService.launch(bundleId);
    }

    public void killApp(String bundleId) throws SDKException, IOException {
        debugService.killApp(bundleId);
    }

    public void startSafari() throws IOException, SDKException {
        debugService.startSafari();
    }

    public void stopSafari() {
        debugService.stopSafari();
    }

    public void installApp(File ipa) throws SDKException {
        installerService.install(ipa, new InstallCallback() {
            @Override
            protected void onUpdate(String operation, int percent, String message) {
                LOG.debug("{} - {} - {}", operation, percent, message);
            }
        });
    }

    public void uninstallApp(String bundleId) throws SDKException {
        installerService.uninstall(bundleId);
    }

    public void cleanApp(String bundleId) throws SDKException {
        appContainerService.clean(bundleId);
    }

    public ApplicationInfo getApplicationInfo(String bundleId) throws SDKException {
        return installerService.getApplication(bundleId);
    }

    public String getAppVersion(String bundleId) throws SDKException {
        return installerService.getApplication(bundleId).getProperty("CFBundleVersion") + "";
    }

    public List<String> getApps() throws SDKException {
        return installerService.listApplications(InstallerService.ApplicationType.USER).stream()
            .map(app -> app.getApplicationId()).collect(Collectors.toList());
    }

    public AppContainerService getAppContainerService() {
        return appContainerService;
    }

    public void setLanguage(String language) throws SDKException {
        informationService.setLanguage(language);
    }

    public void setLocale(String locale) throws SDKException {
        informationService.setLocale(locale);
    }

    public List<ProvisioningProfileInfo> getProvisionProfiles() throws Exception {
        return provisioningService.getProfiles();
    }

    public File takeDeviceScreenshot() throws EntityDriverException {
        try {
            LOG.debug("Take screenshot");
            File png = this.saveIntoFile("ss", "png", "");
            this.screenshotService.takeScreenshot(png);
            return png;
        } catch (IOException | SDKException | ImageReadException ex) {
            throw new EntityDriverException(ex);
        }
    }

    public File startSysLog() throws IOException, SDKException {
        File log = this.saveAsTempTextFile("syslog-", "");
        PrintWriter pw = new PrintWriter(new FileOutputStream(log));
        sysLogListener = (SysLogLine line) -> {
            pw.println(line.toString());
        };
        sysLogService.addListener(sysLogListener);
        return log;
    }

    public void stopSysLog() {
        sysLogService.remove(sysLogListener);
    }

    public DebugService getDebugService() {
        return debugService;
    }

    public ImageMountingService getImageMountingService() {
        return imageMountingService;
    }

    public InformationService getInformationService() {
        return informationService;
    }

    public InstallerService getInstallerService() {
        return installerService;
    }

    public ProvisioningService getProvisioningService() {
        return provisioningService;
    }

    public ScreenshotService getScreenshotService() {
        return screenshotService;
    }

    public SysLogService getSysLogService() {
        return sysLogService;
    }

    public WebInspectorService getWebInspectorService() {
        return webInspectorService;
    }

    public static void main(String[] args) throws Exception {
        SystemConfiguration.getInstance();
        List<String> uuids = LibIMobileDevice.getAllUuids();
        LibIMobileDevice device = new LibIMobileDevice(uuids.get(0));
        device.getInstallerService().listApplications(InstallerService.ApplicationType.USER).forEach(app -> {
            LOG.debug("{} - {}", app.getApplicationId(), app.getProperty("CFBundleVersion"));
        });
    }
}
