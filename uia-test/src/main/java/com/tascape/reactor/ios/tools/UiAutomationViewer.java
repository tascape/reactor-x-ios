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
package com.tascape.reactor.ios.tools;

import com.tascape.reactor.ios.driver.App;
import com.tascape.reactor.ios.driver.UiAutomationDevice;
import com.tascape.reactor.SystemConfiguration;
import com.tascape.reactor.ui.ViewerParameterDialog;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.util.List;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.ComboBoxModel;
import javax.swing.DefaultComboBoxModel;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JSpinner;
import javax.swing.JTextField;
import javax.swing.SpinnerNumberModel;
import javax.swing.SwingUtilities;
import org.apache.commons.lang3.StringUtils;
import org.libimobiledevice.ios.driver.binding.exceptions.SDKException;
import org.slf4j.LoggerFactory;

/**
 *
 * @author linsong wang
 */
public class UiAutomationViewer extends App {
    private static final org.slf4j.Logger LOG = LoggerFactory.getLogger(UiAutomationViewer.class);

    private String appName = "";

    private final ViewerParameterDialog jd = new ViewerParameterDialog("Launch iOS App");

    private final JButton jbLaunch = new JButton("Launch");

    private final JComboBox<String> jcbDevices = new JComboBox<>(new String[]{"detecting devices..."});

    private final JSpinner jsDebugMinutes = new JSpinner(new SpinnerNumberModel(180, 15, 180, 15));

    private final JTextField jtfApp = new JTextField();

    @Override
    public String getBundleId() {
        return "";
    }

    @Override
    public int getLaunchDelayMillis() {
        return 5000;
    }

    @Override
    public String getName() {
        return this.jtfApp.getText();
    }

    @Override
    public void reset() throws Exception {
        LOG.debug("na");
    }

    private void start() throws Exception {
        SwingUtilities.invokeLater(() -> {
            JPanel jpParameters = new JPanel();
            jpParameters.setLayout(new BoxLayout(jpParameters, BoxLayout.PAGE_AXIS));
            jd.setParameterPanel(jpParameters);
            {
                JPanel jp = new JPanel();
                jpParameters.add(jp);
                jp.setLayout(new BoxLayout(jp, BoxLayout.LINE_AXIS));
                jp.add(new JLabel("Devices"));
                jp.add(jcbDevices);
            }
            {
                JPanel jp = new JPanel();
                jpParameters.add(jp);
                jp.setLayout(new BoxLayout(jp, BoxLayout.LINE_AXIS));
                jp.add(new JLabel("App Name"));
                jp.add(jtfApp);
                jtfApp.setToolTipText("name of app-under-test");
                if (StringUtils.isNotEmpty(appName)) {
                    jtfApp.setText(appName);
                }

                jtfApp.addKeyListener(new KeyAdapter() {
                    @Override
                    public void keyPressed(KeyEvent e) {
                        if (e.getKeyCode() == KeyEvent.VK_ENTER) {
                            jbLaunch.doClick();
                        }
                    }
                });
            }
            {
                JPanel jp = new JPanel();
                jpParameters.add(jp);
                jp.setLayout(new BoxLayout(jp, BoxLayout.LINE_AXIS));
                jp.add(new JLabel("Interaction time (minute)"));
                jsDebugMinutes.getEditor().setEnabled(false);
                jp.add(jsDebugMinutes);
            }
            {
                JPanel jp = new JPanel();
                jpParameters.add(jp);
                jp.setLayout(new BoxLayout(jp, BoxLayout.LINE_AXIS));
                jp.add(Box.createRigidArea(new Dimension(518, 2)));
            }

            JPanel jpAction = new JPanel();
            jd.setActionPanel(jpAction);;
            jpAction.setBorder(BorderFactory.createEmptyBorder(10, 0, 0, 0));
            jpAction.setLayout(new BoxLayout(jpAction, BoxLayout.LINE_AXIS));
            jbLaunch.setFont(jbLaunch.getFont().deriveFont(Font.BOLD));
            jbLaunch.setBorder(BorderFactory.createEtchedBorder());
            jbLaunch.setEnabled(false);
            jpAction.add(jbLaunch);
            jbLaunch.addActionListener(event -> {
                new Thread() {
                    @Override
                    public void run() {
                        launchApp();
                    }
                }.start();
            });

            jd.showDialog();

            new Thread() {
                @Override
                public void run() {
                    try {
                        detectDevices();
                    } catch (SDKException | InterruptedException ex) {
                        throw new RuntimeException(ex);
                    }

                    if (jcbDevices.getItemCount() == 1 && StringUtils.isNotBlank(jtfApp.getText())) {
                        jbLaunch.doClick();
                    }
                }
            }.start();
        });
    }

    private void detectDevices() throws SDKException, InterruptedException {
        List<String> uuids = UiAutomationDevice.getAllUuids();
        ComboBoxModel<String> model = new DefaultComboBoxModel<>(uuids.toArray(new String[0]));
        jcbDevices.setModel(model);
        if (model.getSize() == 0) {
            JOptionPane.showMessageDialog(jcbDevices.getTopLevelAncestor(), "No attached iOS device found.");
            this.jbLaunch.setEnabled(false);
        } else {
            this.jbLaunch.setEnabled(true);
        }
    }

    private void launchApp() {
        try {
            jd.setCursor(new Cursor(Cursor.WAIT_CURSOR));
            device = new UiAutomationDevice((String) this.jcbDevices.getSelectedItem());
            this.setDevice(device);
            this.launch();
        } catch (Throwable ex) {
            LOG.error("Error", ex);
            JOptionPane.showMessageDialog(jbLaunch.getTopLevelAncestor(), "Cannot start app");
            return;
        } finally {
            jd.setCursor(Cursor.getDefaultCursor());
        }

        int debugMinutes = (int) jsDebugMinutes.getValue();
        jd.dispose();
        try {
            this.interactManually(debugMinutes);
        } catch (Throwable ex) {
            LOG.error("Error", ex);
            System.exit(1);
        }
        System.exit(0);
    }

    public static void main(String[] args) {
        SystemConfiguration.getInstance();
        UiAutomationViewer viewer = new UiAutomationViewer();

        if (args.length > 0) {
            viewer.appName = args[0];
        }

        try {
            viewer.start();
        } catch (Throwable ex) {
            LOG.error("fail to acquire device", ex);
            System.exit(1);
        }
    }
}
