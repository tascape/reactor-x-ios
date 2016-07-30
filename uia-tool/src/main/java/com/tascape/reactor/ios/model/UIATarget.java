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

import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;

/**
 * See https://developer.apple.com/library/ios/documentation/ToolsLanguages/Reference/UIATargetClassReference/index.html
 * for more detail.
 *
 * @author linsong wang
 */
public interface UIATarget {

    void deactivateAppForDuration(int duration) throws UIAException;

    String model() throws UIAException;

    String name() throws UIAException;

    Rectangle2D rect() throws UIAException;

    String systemName() throws UIAException;

    String systemVersion() throws UIAException;

    DeviceOrientation deviceOrientation() throws UIAException;

    void setDeviceOrientation(DeviceOrientation orientation) throws UIAException;

    void setLocation(double latitude, double longitude) throws UIAException;

    void captureRectWithName(Rectangle2D rect, String imageName) throws UIAException;

    void captureScreenWithName(String imageName) throws UIAException;

    void clickVolumeDown() throws UIAException;

    void clickVolumeUp() throws UIAException;

    void holdVolumeDown(int duration) throws UIAException;

    void holdVolumeUp(int duration) throws UIAException;

    /**
     * Locks screen for specified seconds, then tries to unlock. Simulating passcode entry is currently unsupported.
     * Set the Settings - General - Passcode Lock feature to Off prior to running your cases.
     *
     * @param duration in second
     *
     * @throws UIAException any error
     */
    void lockForDuration(int duration) throws UIAException;

    void shake() throws UIAException;

    void dragFromToForDuration(Point2D.Float from, Point2D.Float to, int duration) throws UIAException;

    void dragFromToForDuration(UIAElement fromElement, UIAElement toElement, int duration) throws UIAException;

    void dragFromToForDuration(String fromJavaScript, String toJavaScript, int duration) throws UIAException;

    void doubleTap(float x, float y) throws UIAException;

    void doubleTap(UIAElement element) throws UIAException;

    void doubleTap(String javaScript) throws UIAException;

    void flickFromTo(Point2D.Float from, Point2D.Float to) throws UIAException;

    void flickFromTo(UIAElement fromElement, UIAElement toElement) throws UIAException;

    void flickFromTo(String fromJavaScript, String toJavaScript) throws UIAException;

    void pinchCloseFromToForDuration(Point2D.Float from, Point2D.Float to, int duration) throws UIAException;

    void pinchCloseFromToForDuration(UIAElement fromElement, UIAElement toElement, int duration) throws UIAException;

    void pinchCloseFromToForDuration(String fromJavaScript, String toJavaScript, int duration) throws UIAException;

    void pinchOpenFromToForDuration(Point2D.Float from, Point2D.Float to, int duration) throws UIAException;

    void pinchOpenFromToForDuration(UIAElement fromElement, UIAElement toElement, int duration) throws UIAException;

    void pinchOpenFromToForDuration(String fromJavaScript, String toJavaScript, int duration) throws UIAException;

    void tap(float x, float y) throws UIAException;

    void tap(UIAElement element) throws UIAException;

    /**
     * Taps on an element specified by JavaScript.
     *
     * @param javaScript the javascript that uniquely identify the element, such as "window.tabBars()['MainTabBar']",
     *                   or "window.elements()[1].buttons()[0]"
     *
     * @throws UIAException any error
     */
    void tap(String javaScript) throws UIAException;

    void touchAndHold(Point2D.Float point, int duration) throws UIAException;

    void touchAndHold(UIAElement element, int duration) throws UIAException;

    void touchAndHold(String javaScript, int duration) throws UIAException;

    void popTimeout() throws UIAException;

    void pushTimeout(int timeoutValue) throws UIAException;

    void setTimeout(int timeout) throws UIAException;

    int timeout() throws UIAException;

    void delay(int timeInterval) throws UIAException;

    boolean onAlert(UIAAlert alert) throws UIAException;

    default String toCGString(Point2D.Float point) {
        return String.format("{x:%f, y:%f}", point.x, point.y);
    }

    default String toCGString(float x, float y) {
        return String.format("{x:%f, y:%f}", x, y);
    }
}
