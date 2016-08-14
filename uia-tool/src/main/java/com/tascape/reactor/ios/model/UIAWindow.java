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

import com.tascape.reactor.ios.driver.UiAutomationDevice;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author linsong wang
 */
public class UIAWindow extends UIAElement {
    private static final Logger LOG = LoggerFactory.getLogger(UIAWindow.class);

    @Override
    public void setDevice(UiAutomationDevice device) {
        super.setDevice(device);
    }

    /**
     * Finds UI element recursively of the first appearance.
     *
     * @param <T>  type of element
     * @param type type of element
     *
     * @return element found, or null
     */
    public <T extends UIAElement> T findElement(Class<T> type) {
        return this.findElement(type, null);
    }

    /**
     * Finds UI element recursively.
     *
     * @param <T>  type of element
     * @param type type of element
     * @param name name of element
     *
     * @return element found, or null
     */
    @Override
    public <T extends UIAElement> T findElement(Class<T> type, String name) {
        LOG.debug("Look for {}{}", type.getSimpleName(), name == null ? "" : "['" + name + "']");
        return type.cast(super.findElement(type, name));
    }

    /**
     * Finds UI element recursively.
     *
     * @param <T>         type of element
     * @param type        type of element
     * @param partialName name of element
     *
     * @return element found, or null
     */
    @Override
    public <T extends UIAElement> T findElementPartialName(Class<T> type, String partialName) {
        LOG.debug("Look for {}['{}'] (partial name)", type.getSimpleName(), partialName);
        return type.cast(super.findElementPartialName(type, partialName));
    }

    public UIAButton findButton(String name) {
        return this.findElement(UIAButton.class, name);
    }

    public UIAStaticText findStaticText(String name) {
        return this.findElement(UIAStaticText.class, name);
    }

    public UIATextView findTextView(String name) {
        return this.findElement(UIATextView.class, name);
    }

    public UIALink findLink(String name) {
        return this.findElement(UIALink.class, name);
    }

    public UIATableCell findCell(String name) {
        return this.findElement(UIATableCell.class, name);
    }
}
