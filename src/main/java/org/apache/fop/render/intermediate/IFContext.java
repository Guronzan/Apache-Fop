/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

/* $Id: IFContext.java 830293 2009-10-27 19:07:52Z vhennebert $ */

package org.apache.fop.render.intermediate;

import java.util.Collections;
import java.util.Locale;
import java.util.Map;

import org.apache.fop.apps.FOUserAgent;
import org.apache.xmlgraphics.util.QName;

/**
 * This class provides a context object that is valid for a single processing
 * run to create an output file using the intermediate format. It allows access
 * to the user agent and other context information, such as foreign attributes
 * for certain elements in the intermediate format.
 * <p>
 * Foreign attributes are usually specific to a particular output format
 * implementation. Most implementations will just ignore all foreign attributes
 * for most elements. That's why the main IF interfaces are not burdened with
 * this.
 */
public class IFContext {

    private FOUserAgent userAgent;

    /** foreign attributes: Map<QName, Object> */
    private Map<QName, String> foreignAttributes = Collections.emptyMap();

    private Locale language;

    private String structurePointer;

    /**
     * Main constructor.
     *
     * @param ua
     *            the user agent
     */
    public IFContext(final FOUserAgent ua) {
        setUserAgent(ua);
    }

    /**
     * Set the user agent.
     *
     * @param ua
     *            the user agent
     */
    public void setUserAgent(final FOUserAgent ua) {
        if (this.userAgent != null) {
            throw new IllegalStateException("The user agent was already set");
        }
        this.userAgent = ua;
    }

    /**
     * Returns the associated user agent.
     *
     * @return the user agent
     */
    public FOUserAgent getUserAgent() {
        return this.userAgent;
    }

    /**
     * Returns the currently applicable foreign attributes.
     *
     * @return a Map<QName, Object>
     */
    public Map<QName, String> getForeignAttributes() {
        return this.foreignAttributes;
    }

    /**
     * Returns a foreign attribute.
     *
     * @param qName
     *            the qualified name of the foreign attribute
     * @return the value of the foreign attribute or null if the attribute isn't
     *         specified
     */
    public String getForeignAttribute(final QName qName) {
        return this.foreignAttributes.get(qName);
    }

    /**
     * Sets the currently applicable foreign attributes.
     *
     * @param foreignAttributes
     *            a Map<QName, Object> or null to reset
     */
    public void setForeignAttributes(final Map<QName, String> foreignAttributes) {
        if (foreignAttributes != null) {
            this.foreignAttributes = foreignAttributes;
        } else {
            // Make sure there is always at least an empty map so we don't have
            // to check
            // in the implementation code
            this.foreignAttributes = Collections.emptyMap();
        }
    }

    /**
     * Resets the foreign attributes to "no foreign attributes".
     */
    public void resetForeignAttributes() {
        setForeignAttributes(null);
    }

    /**
     * Sets the currently applicable language.
     *
     * @param lang
     *            the language
     */
    public void setLanguage(final Locale lang) {
        this.language = lang;
    }

    /**
     * Returns the currently applicable language.
     *
     * @return the language (or null if the language is undefined)
     */
    public Locale getLanguage() {
        return this.language;
    }

    /**
     * Sets the structure pointer for the following painted marks. This method
     * is used when accessibility features are enabled.
     *
     * @param ptr
     *            the structure pointer
     */
    public void setStructurePointer(final String ptr) {
        this.structurePointer = ptr;
    }

    /**
     * Resets the current structure pointer.
     *
     * @see #setStructurePointer(String)
     */
    public void resetStructurePointer() {
        setStructurePointer(null);
    }

    /**
     * Returns the current structure pointer.
     *
     * @return the structure pointer (or null if no pointer is active)
     * @see #setStructurePointer(String)
     */
    public String getStructurePointer() {
        return this.structurePointer;
    }

}
