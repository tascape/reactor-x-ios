/*
 * Copyright 2016 Nebula Bay.
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

import com.tascape.reactor.ios.driver.UiAutomationDevice;
import com.tascape.reactor.ios.model.UIAElement;
import com.tascape.reactor.ios.model.UIAException;
import com.tascape.reactor.ios.model.UIAWindow;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.image.BufferedImage;
import java.io.File;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import javax.imageio.ImageIO;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTabbedPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.JTree;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.SwingWorker;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeCellRenderer;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreeModel;
import javax.swing.tree.TreePath;
import org.apache.commons.lang3.StringUtils;
import org.junit.Assert;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author linsong wang
 */
public class UiInteraction {
    private static final Logger LOG = LoggerFactory.getLogger(UiInteraction.class);

    protected UiAutomationDevice device;

    private final AtomicBoolean visible = new AtomicBoolean(true);

    private final AtomicBoolean pass = new AtomicBoolean(false);

    private final String tName = Thread.currentThread().getName() + "m";

    private long end;

    private final JDialog jd = new JDialog((JFrame) null, "Manual Device UI Interaction ");

    private final JLabel jlTimeout = new JLabel("xxx seconds left", SwingConstants.CENTER);

    private final JCheckBox jcbTap = new JCheckBox("Enable Tap", null, false);

    private final JButton jbLogUi = new JButton("Log Main Window");

    private final JTextArea jtaResponse = new JTextArea();

    private JLabel jlScreen;

    private final JTree jtView = new JTree();

    public UiInteraction(UiAutomationDevice device) {
        this.device = device;
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
    public void start(int timeoutMinutes) throws Exception {
        LOG.info("Start manual UI interaction");
        end = System.currentTimeMillis() + timeoutMinutes * 60000L;

        SwingUtilities.invokeLater(() -> {
            initUi();
            new TimeoutUpdator().execute();
        });

        while (visible.get()) {
            if (System.currentTimeMillis() > end) {
                LOG.error("Manual UI interaction timeout");
                break;
            }
            Thread.sleep(500);
        }

        if (pass.get()) {
            LOG.info("Manual UI Interaction returns PASS");
        } else {
            Assert.fail("Manual UI Interaction returns FAIL");
        }
    }

    private void initUi() {
        String info = device.getProductDetail() + "" + device.getUuid();
        jd.setTitle("Manual Device UI Interaction " + info);
        jd.setDefaultCloseOperation(JDialog.DO_NOTHING_ON_CLOSE);

        JPanel jpContent = new JPanel(new BorderLayout());
        jd.setContentPane(jpContent);
        jpContent.setPreferredSize(new Dimension(1088, 828));
        jpContent.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));

        JPanel jpInfo = new JPanel();
        jpContent.add(jpInfo, BorderLayout.PAGE_START);
        jpInfo.setLayout(new BorderLayout());
        {
            JButton jb = new JButton("PASS");
            jb.setForeground(Color.green.darker());
            jb.setFont(jb.getFont().deriveFont(Font.BOLD));
            jpInfo.add(jb, BorderLayout.LINE_START);
            jb.addActionListener(event -> {
                pass.set(true);
                jd.dispose();
                visible.set(false);
            });
        }
        {
            JButton jb = new JButton("FAIL");
            jb.setForeground(Color.red);
            jb.setFont(jb.getFont().deriveFont(Font.BOLD));
            jpInfo.add(jb, BorderLayout.LINE_END);
            jb.addActionListener(event -> {
                pass.set(false);
                jd.dispose();
                visible.set(false);
            });
        }

        jpInfo.add(jlTimeout, BorderLayout.CENTER);

        JPanel jpResponse = new JPanel(new BorderLayout());
        JPanel jpProgress = new JPanel(new BorderLayout());
        jpResponse.add(jpProgress, BorderLayout.PAGE_START);

        jtaResponse.setEditable(false);
        jtaResponse.setTabSize(4);
        Font font = jtaResponse.getFont();
        jtaResponse.setFont(new Font("Courier New", font.getStyle(), font.getSize()));

        JTabbedPane jtp = new JTabbedPane();
        jtView.setVisible(false);
        jtView.setCellRenderer(new UIAElementCellRenderer());
        jtView.setExpandsSelectedPaths(true);
        jtp.add("tree", new JScrollPane(jtView));
        jtp.add("text", new JScrollPane(new JScrollPane(jtaResponse)));
        jpResponse.add(jtp, BorderLayout.CENTER);

        JPanel jpScreen = new JPanel(new BorderLayout());
        jlScreen = createSreenshotLabel(null);
        jpScreen.add(jlScreen, BorderLayout.CENTER);
        JScrollPane jspScreen = new JScrollPane(jpScreen);
        jpResponse.add(jspScreen, BorderLayout.LINE_START);

        JPanel jpJs = new JPanel(new BorderLayout());
        JTextArea jtaJs = new JTextArea();
        jpJs.add(new JScrollPane(jtaJs), BorderLayout.CENTER);

        JSplitPane jSplitPane = new JSplitPane(JSplitPane.VERTICAL_SPLIT, jpResponse, jpJs);
        jSplitPane.setResizeWeight(0.88);
        jpContent.add(jSplitPane, BorderLayout.CENTER);

        JPanel jpLog = new JPanel();
        jpLog.setBorder(BorderFactory.createEmptyBorder(5, 0, 0, 0));
        jpLog.setLayout(new BoxLayout(jpLog, BoxLayout.LINE_AXIS));

        jpLog.add(jcbTap);
        jpLog.add(Box.createHorizontalStrut(8));

        jpResponse.add(jpLog, BorderLayout.PAGE_END);
        {
            jpLog.add(jbLogUi);
            jbLogUi.addActionListener(new LogScreenActionListener());
        }
        jpLog.add(Box.createHorizontalStrut(38));
        {
            JButton jbLogMsg = new JButton("Log Message");
            jpLog.add(jbLogMsg);
            JTextField jtMsg = new JTextField(10);
            jpLog.add(jtMsg);
            jtMsg.addFocusListener(new FocusListener() {
                @Override
                public void focusLost(final FocusEvent pE) {
                }

                @Override
                public void focusGained(final FocusEvent pE) {
                    jtMsg.selectAll();
                }
            });
            jtMsg.addKeyListener(new KeyAdapter() {
                @Override
                public void keyPressed(KeyEvent e) {
                    if (e.getKeyCode() == KeyEvent.VK_ENTER) {
                        jbLogMsg.doClick();
                    }
                }
            });
            jbLogMsg.addActionListener(event -> {
                Thread t = new Thread(tName) {
                    @Override
                    public void run() {
                        String msg = jtMsg.getText();
                        if (StringUtils.isNotBlank(msg)) {
                            LOG.info("{}", msg);
                            jtMsg.selectAll();
                        }
                    }
                };
                t.start();
                try {
                    t.join();
                } catch (InterruptedException ex) {
                    LOG.error("Cannot take screenshot", ex);
                }
                jtMsg.requestFocus();
            });
        }
        jpLog.add(Box.createHorizontalStrut(38));
        {
            JButton jbClear = new JButton("Clear Text");
            jpLog.add(jbClear);
            jbClear.addActionListener(event -> {
                jtaResponse.setText("");
            });
        }

        JPanel jpAction = new JPanel();
        jpContent.add(jpAction, BorderLayout.PAGE_END);
        jpAction.setLayout(new BoxLayout(jpAction, BoxLayout.LINE_AXIS));
        jpJs.add(jpAction, BorderLayout.PAGE_END);
        {
            JButton jbJavaScript = new JButton("Run JavaScript");
            jpAction.add(Box.createHorizontalGlue());
            jpAction.add(jbJavaScript);
            jbJavaScript.addActionListener(event -> {
                String js = jtaJs.getSelectedText();
                if (js == null) {
                    js = jtaJs.getText();
                }
                if (StringUtils.isEmpty(js)) {
                    return;
                }
                String javaScript = js;
                Thread t = new Thread(tName) {
                    @Override
                    public void run() {
                        try {
                            device.runJavaScript(javaScript).forEach(l -> {
                                jtaResponse.append(l);
                                jtaResponse.append("\n");
                            });
                        } catch (UIAException ex) {
                            LOG.error("Cannot run javascript", ex);
                            jtaResponse.append("Cannot run javascript");
                            jtaResponse.append("\n");
                        }
                    }
                };
                t.start();
                try {
                    t.join();
                } catch (InterruptedException ex) {
                    LOG.error("Cannot run javascript", ex);
                }
            });
        }

        jd.pack();
        jd.setVisible(true);
        jd.setLocationRelativeTo(null);

        jbLogUi.doClick();
    }

    private TreeModel getModel(UIAWindow window) {
        DefaultMutableTreeNode rootNode = createNode(window);
        DefaultTreeModel treeModel = new DefaultTreeModel(rootNode);
        return treeModel;
    }

    private DefaultMutableTreeNode createNode(UIAElement element) {
        DefaultMutableTreeNode node = new DefaultMutableTreeNode(element);
        for (UIAElement e : element.elements()) {
            node.add(createNode(e));
        }
        return node;
    }

    private class TimeoutUpdator extends SwingWorker<Long, Long> {
        @Override
        protected Long doInBackground() throws Exception {
            while (System.currentTimeMillis() < end) {
                Thread.sleep(1000);
                long left = (end - System.currentTimeMillis()) / 1000;
                this.publish(left);
            }
            return 0L;
        }

        @Override
        protected void process(List<Long> chunks) {
            Long l = chunks.get(chunks.size() - 1);
            jlTimeout.setText(l + " seconds left");
            if (l < 850) {
                jlTimeout.setForeground(Color.red);
            }
        }
    }

    private JLabel createSreenshotLabel(Icon icon) {
        JLabel jl = new JLabel();
        jl.setVerticalAlignment(SwingConstants.TOP);
        if (icon == null) {
            jl.setIcon(new Icon() {
                @Override
                public void paintIcon(Component c, Graphics g, int x, int y) {
                }

                @Override
                public int getIconWidth() {
                    return device.getDisplaySize().width;
                }

                @Override
                public int getIconHeight() {
                    return device.getDisplaySize().height;
                }
            });
        } else {
            jl.setIcon(icon);
        }
        Dimension dimension = new Dimension(device.getDisplaySize().width, device.getDisplaySize().height);
        jl.setMaximumSize(dimension);
        jl.setMinimumSize(dimension);
        jl.setPreferredSize(dimension);
        return jl;
    }

    private class LogScreenActionListener implements ActionListener {
        @Override
        public void actionPerformed(ActionEvent e) {
            jtaResponse.setText("waiting for screenshot...");
            Thread t = new Thread(tName) {
                @Override
                public void run() {
                    LOG.debug("\n\n");
                    try {
                        File png = device.takeDeviceScreenshot();
                        BufferedImage image = ImageIO.read(png);

                        int w = device.getDisplaySize().width;
                        int h = device.getDisplaySize().height;

                        BufferedImage resizedImg = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
                        Graphics2D g2 = resizedImg.createGraphics();
                        g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION,
                            RenderingHints.VALUE_INTERPOLATION_BILINEAR);
                        g2.drawImage(image, 0, 0, w, h, null);
                        g2.dispose();

                        for (MouseListener ml : jlScreen.getMouseListeners()) {
                            jlScreen.removeMouseListener(ml);
                        }
                        Container c = jlScreen.getParent();
                        c.removeAll();
                        jlScreen = createSreenshotLabel(new ImageIcon(resizedImg));
                        c.add(jlScreen);
                        jlScreen.addMouseListener(new ScreenshotMouseListener());

                        UIAWindow window = device.mainWindow();
                        jtView.setModel(getModel(window));
                        jtView.setVisible(true);
                        this.expandAllNodes(jtView, 0, jtView.getRowCount());

                        jtaResponse.setText("");
                        window.logElement().forEach(line -> {
                            LOG.debug(line);
                            jtaResponse.append(line);
                            jtaResponse.append("\n");
                        });

                    } catch (Exception ex) {
                        LOG.error("Cannot log screen", ex);
                        jtaResponse.append("Cannot log screen");
                    }
                    jtaResponse.append("\n\n\n");
                    LOG.debug("\n\n");

                    jd.setSize(jd.getBounds().width + 1, jd.getBounds().height + 1);
                    jd.setSize(jd.getBounds().width - 1, jd.getBounds().height - 1);
                }

                private void expandAllNodes(JTree tree, int startingIndex, int rowCount) {
                    for (int i = startingIndex; i < rowCount; ++i) {
                        tree.expandRow(i);
                    }

                    if (tree.getRowCount() != rowCount) {
                        expandAllNodes(tree, rowCount, tree.getRowCount());
                    }
                }
            };
            t.start();
        }
    }

    private class ScreenshotMouseListener extends MouseAdapter {
        @Override
        public void mouseClicked(MouseEvent e) {
            Point p = e.getPoint();
            LOG.debug("clicked at x:{}, y:{}", p.getX(), p.getY());
            jtView.clearSelection();
            this.highlightElementOnTree(p);

            if (jcbTap.isSelected()) {
                device.tap(e.getPoint().x, e.getPoint().y);
                jbLogUi.doClick();
            }
        }

        private void highlightElementOnTree(Point p) {
            DefaultMutableTreeNode n = (DefaultMutableTreeNode) jtView.getModel().getRoot();
            highlightElementOnTree(p, n);
        }

        private void highlightElementOnTree(Point p, DefaultMutableTreeNode node) {
            Object uo = node.getUserObject();
            if (!(uo instanceof UIAElement)) {
                return;
            }
            UIAElement e = UIAElement.class.cast(uo);
            if (e.rect().contains(p)) {
                if (node.isLeaf()) {
                    TreePath path = new TreePath(node.getPath());
                    jtView.scrollPathToVisible(path);
                    jtView.addSelectionPath(path);
                } else {
                    for (int i = 0, j = node.getChildCount(); i < j; i++) {
                        DefaultMutableTreeNode n = (DefaultMutableTreeNode) node.getChildAt(i);
                        highlightElementOnTree(p, n);
                    }
                }
            }
        }
    }

    private static class UIAElementCellRenderer extends DefaultTreeCellRenderer {
        @Override
        public Component getTreeCellRendererComponent(JTree tree, Object value, boolean selected, boolean expanded,
            boolean leaf, int row, boolean hasFocus) {
            Component c = super.getTreeCellRendererComponent(tree, value, selected, expanded, leaf, row, hasFocus);
            Object uo = ((DefaultMutableTreeNode) value).getUserObject();
            if (uo instanceof UIAElement) {
                UIAElement element = (UIAElement) uo;
                Rectangle.Float rect = element.rect();
                String s = "<html>" + element.getClass().getSimpleName() + " <b>" + element.name() + "</b> "
                    + String.format("[%s,%s,%s,%s]", rect.x, rect.y, rect.width, rect.height);
                this.setText(s);
            }
            return this;
        }
    }
}
