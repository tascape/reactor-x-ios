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
package com.tascape.reactor.ios.model;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 *
 * @author linsong wang
 */
@Retention(value = RetentionPolicy.RUNTIME)
@Target(value = {ElementType.FIELD, ElementType.TYPE})
public @interface FindBy {

    /**
     * Returns path for locating the element (UIAElement or one of its child types). Example:
     * {@code window.elements()[1].buttons()[1]}, where {@code window} is pre-defined JavaScript object of iOS app
     * main window.
     *
     * @return Apple UIAutomation JavaScript path of an element on main window element tree
     */
    public String jsPath() default "";

    /**
     * Returns name for locating the element (UIAElement or one of its child types).
     *
     * @return name of the element
     */
    public String name() default "";

    /**
     * Returns partial name for locating the element (UIAElement or one of its child types).
     * {@link String#contains(CharSequence)} and {@link String#matches(String)} are tried in that order.
     *
     * @return partial name of the element, Java regular expression is supported
     */
    public String partialName() default "";
}
