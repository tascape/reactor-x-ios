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

import com.tascape.reactor.ios.comm.Instruments;

/**
 *
 * @author linsong wang
 */
public interface UIAApplication {

    UIAWindow mainWindow() throws UIAException;

    UIAWindow windows(int index) throws UIAException;

//    String bundleID() throws UIAException;
    UIAKeyboard keyboard() throws UIAException;

//    String version() throws UIAException;

    default UIAKeyboard getKeyboard(Instruments instruments) throws UIAException {
        UIAKeyboard kb = new UIAKeyboard();
        kb.setInstruments(instruments);
        return kb;
    }
}
