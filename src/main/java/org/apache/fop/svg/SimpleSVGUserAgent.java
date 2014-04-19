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

/* $Id: SimpleSVGUserAgent.java 679326 2008-07-24 09:35:34Z vhennebert $ */

package org.apache.fop.svg;

import java.awt.Dimension;
import java.awt.geom.AffineTransform;
import java.awt.geom.Dimension2D;

import javax.xml.parsers.SAXParserFactory;

import org.apache.batik.bridge.UserAgentAdapter;

/**
 * A simple SVG user agent. This is an implementation of the Batik SVG user
 * agent. It ignores any message output sent by Batik.
 */
public class SimpleSVGUserAgent extends UserAgentAdapter {

    private AffineTransform currentTransform = null;
    private float pixelUnitToMillimeter = 0.0f;

    /**
     * Creates a new user agent.
     * 
     * @param pixelUnitToMM
     *            the pixel to millimeter conversion factor currently in effect
     * @param at
     *            the current transform
     */
    public SimpleSVGUserAgent(final float pixelUnitToMM,
            final AffineTransform at) {
        this.pixelUnitToMillimeter = pixelUnitToMM;
        this.currentTransform = at;
    }

    /**
     * Returns a customized the pixel to mm factor.
     * 
     * @return the pixel unit to millimeter conversion factor
     */
    @Override
    public float getPixelUnitToMillimeter() {
        return this.pixelUnitToMillimeter;
    }

    /**
     * Returns the language settings.
     * 
     * @return the languages supported
     */
    @Override
    public String getLanguages() {
        return "en"; // userLanguages;
    }

    /**
     * Returns the media type for this rendering.
     * 
     * @return the media for FO documents is "print"
     */
    @Override
    public String getMedia() {
        return "print";
    }

    /**
     * Returns the user stylesheet URI.
     * 
     * @return null if no user style sheet was specified.
     */
    @Override
    public String getUserStyleSheetURI() {
        return null; // userStyleSheetURI;
    }

    /**
     * Returns the class name of the XML parser.
     * 
     * @return the XML parser class name
     */
    @Override
    public String getXMLParserClassName() {
        try {
            final SAXParserFactory factory = SAXParserFactory.newInstance();
            return factory.newSAXParser().getXMLReader().getClass().getName();
        } catch (final Exception e) {
            return null;
        }
    }

    /**
     * Is the XML parser validating.
     * 
     * @return true if the XML parser is validating
     */
    @Override
    public boolean isXMLParserValidating() {
        return false;
    }

    /**
     * Get the transform of the SVG document.
     * 
     * @return the transform
     */
    @Override
    public AffineTransform getTransform() {
        return this.currentTransform;
    }

    /** {@inheritDoc} */
    @Override
    public void setTransform(final AffineTransform at) {
        this.currentTransform = at;
    }

    /**
     * Get the default viewport size for an SVG document. This returns a default
     * value of 100x100.
     * 
     * @return the default viewport size
     */
    @Override
    public Dimension2D getViewportSize() {
        return new Dimension(100, 100);
    }

}
