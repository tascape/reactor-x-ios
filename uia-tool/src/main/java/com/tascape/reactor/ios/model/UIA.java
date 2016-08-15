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
package com.tascape.reactor.ios.model;

import java.awt.geom.Rectangle2D;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author linsong wang
 */
public class UIA {
    private static final Logger LOG = LoggerFactory.getLogger(UIA.class);

    private static final Pattern PATTERN_UIA = Pattern.compile("(UIA.+?) \"(.+?)\" (\\{\\{.+?\\}, \\{.+?\\}\\})",
        Pattern.DOTALL | Pattern.MULTILINE);

    private static final String UIA_CLASS = "UIA";

    private static final String ELEMENTS = "elements: {";

    public static UIA newInstance() {
        return new UIA();
    }

    public UIAWindow parseElementTree(List<String> elementTree) throws UIAException {
        return this.parseElementTree(0, elementTree);
    }

    public UIAWindow parseElementTree(int index, List<String> elementTree) throws UIAException {
        while (!elementTree.get(0).startsWith(UIAWindow.class.getSimpleName())) {
            elementTree.remove(0);
        }
        UIAWindow window = null;
        while (elementTree.size() > 0) {
            String line = elementTree.remove(0);
            if (line.startsWith("UIAWindow")) {
                window = (UIAWindow) parseUIAElement(line);
                break;
            }
        }
        if (window == null) {
            throw new UIAException("Cannot parse element tree, no UIAWindow found");
        }
        window.setIndex(index);
        if (!elementTree.isEmpty()) {
            parseElements(window, elementTree.subList(0, elementTree.size()));
        }
        return window;
    }

    public UIAElement parseUIAElement(String uiaLine) throws UIAException {
        Matcher m = PATTERN_UIA.matcher(uiaLine);
        if (m.matches()) {
            UIAElement e = newElement(m.group(1));
            e.setName(m.group(2));
            String[] r = m.group(3).replaceAll("\\{", "").replaceAll("\\}", "").split(",");
            e.setRect(new Rectangle2D.Float(Float.parseFloat(r[0]), Float.parseFloat(r[1]),
                Float.parseFloat(r[2]), Float.parseFloat(r[3])));
            return e;
        }
        throw new UIAException("Cannot parse " + uiaLine);
    }

    private void clean(List<String> elementTree) {
        while (!elementTree.get(0).startsWith(UIAWindow.class.getSimpleName())) {
            elementTree.remove(0);
        }
    }

    private void parseElements(UIAElement root, List<String> elementTree) throws UIAException {
        List<String> lines = elementTree.stream().map(l -> StringUtils.replace(l, "\t", "", 1))
            .collect(Collectors.toList());
        UIAElement element = null;
        List<String> childLines = new ArrayList<>();
        String uiaLine = "";
        while (lines.size() > 0) {
            String line = lines.remove(0);

            if (line.startsWith(UIA_CLASS)) {
                uiaLine = line;

            } else if (line.startsWith(ELEMENTS)) {
                childLines = new ArrayList<>();

            } else if (line.startsWith("\t")) {
                childLines.add(line);

            } else if (line.startsWith("}")) {
                parseElements(element, childLines);
                childLines = new ArrayList<>();

            } else if (!childLines.isEmpty()) {
                childLines.add(line);

            } else if (StringUtils.isNotEmpty(uiaLine)) {
                uiaLine += "\n" + line;
            }

            if (uiaLine.endsWith("}}")) {
                element = parseUIAElement(uiaLine);
                uiaLine = "";
                root.addElement(element);
            }
        }
    }

    private static UIAElement newElement(String uia) {
        switch (uia) {
            case "UIAActionSheet":
                return new UIAActionSheet();
            case "UIAActivityIndicator":
                return new UIAActivityIndicator();
            case "UIAActivityView":
                return new UIAActivityView();
            case "UIAAlert":
                return new UIAAlert();
            case "UIAButton":
                return new UIAButton();
            case "UIACollectionCell":
                return new UIACollectionCell();
            case "UIACollectionView":
                return new UIACollectionView();
            case "UIAEditingMenu":
                return new UIAEditingMenu();
            case "UIAElement":
                return new UIAElement();
            case "UIAImage":
                return new UIAImage();
            case "UIAKey":
                return new UIAKey();
            case "UIAKeyboard":
                return new UIAKeyboard();
            case "UIALink":
                return new UIALink();
            case "UIANavigationBar":
                return new UIANavigationBar();
            case "UIAPageIndicator":
                return new UIAPageIndicator();
            case "UIAPicker":
                return new UIAPicker();
            case "UIAPickerWheel":
                return new UIAPickerWheel();
            case "UIAPopover":
                return new UIAPopover();
            case "UIAProgressIndicator":
                return new UIAProgressIndicator();
            case "UIAScrollView":
                return new UIAScrollView();
            case "UIASearchBar":
                return new UIASearchBar();
            case "UIASecureTextField":
                return new UIASecureTextField();
            case "UIASegmentedControl":
                return new UIASegmentedControl();
            case "UIASlider":
                return new UIASlider();
            case "UIAStaticText":
                return new UIAStaticText();
            case "UIAStatusBar":
                return new UIAStatusBar();
            case "UIASwitch":
                return new UIASwitch();
            case "UIATabBar":
                return new UIATabBar();
            case "UIATableCell":
                return new UIATableCell();
            case "UIATableGroup":
                return new UIATableGroup();
            case "UIATableView":
                return new UIATableView();
            case "UIATextField":
                return new UIATextField();
            case "UIATextView":
                return new UIATextView();
            case "UIAToolbar":
                return new UIAToolbar();
            case "UIAWebView":
                return new UIAWebView();
            case "UIAWindow":
                return new UIAWindow();
            default:
                LOG.warn("Unkown element type {}, use UIAElement", uia);
                return new UIAElement();
        }
    }

    public static void main(String[] args) throws Exception {
        UIA uia = UIA.newInstance();
        {
            List<String> elementTree = IOUtils.readLines(UIA.class.getResourceAsStream("element-tree.txt"),
                Charset.defaultCharset());
            UIAWindow w = uia.parseElementTree(elementTree);
            LOG.debug("element tree\n{}", w);
            LOG.debug("json\n{}", w.toJson().toString(2));

            try {
                UIAElement element = w.findElement(UIAStaticText.class, "Recipes");
                LOG.debug("{}", element.getJsPath());
                LOG.debug("{}", element.toJson().toString(2));
                LOG.debug("{}", element.toString());
            } catch (Exception ex) {
                LOG.error("", ex);
            }
        }
        {
            List<String> elementTree = IOUtils.readLines(UIA.class.getResourceAsStream("element-tree-0.txt"),
                Charset.defaultCharset());
            UIAWindow w = uia.parseElementTree(elementTree);
            LOG.debug(w.toJson().toString(2));
        }
        {
            List<String> elementTree = IOUtils.readLines(UIA.class.getResourceAsStream("element-tree-1.txt"),
                Charset.defaultCharset());
            UIAWindow w = uia.parseElementTree(elementTree);
            LOG.debug(w.toJson().toString(2));
        }
    }
}
