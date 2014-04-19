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

/* $Id: AFMWritingDirectionMetrics.java 679326 2008-07-24 09:35:34Z vhennebert $ */

package org.apache.fop.fonts.type1;

/**
 * Represents a writing direction metrics section from an AFM file.
 */
public class AFMWritingDirectionMetrics {

    private Number underlinePosition;
    private Number underlineThickness;
    private double italicAngle;
    private boolean isFixedPitch;

    /**
     * Returns the UnderlinePosition value.
     * 
     * @return the underlinePosition
     */
    public Number getUnderlinePosition() {
        return this.underlinePosition;
    }

    /**
     * Sets the UnderlinePosition value.
     * 
     * @param underlinePosition
     *            the underlinePosition to set
     */
    public void setUnderlinePosition(final Number underlinePosition) {
        this.underlinePosition = underlinePosition;
    }

    /**
     * Returns the UnderlineThickness value.
     * 
     * @return the underlineThickness
     */
    public Number getUnderlineThickness() {
        return this.underlineThickness;
    }

    /**
     * Sets the UnderlineThickness value.
     * 
     * @param underlineThickness
     *            the underlineThickness to set
     */
    public void setUnderlineThickness(final Number underlineThickness) {
        this.underlineThickness = underlineThickness;
    }

    /**
     * Returns the ItalicAngle value.
     * 
     * @return the italicAngle
     */
    public double getItalicAngle() {
        return this.italicAngle;
    }

    /**
     * Sets the ItalicAngle value.
     * 
     * @param italicAngle
     *            the italicAngle to set
     */
    public void setItalicAngle(final double italicAngle) {
        this.italicAngle = italicAngle;
    }

    /**
     * Returns the IsFixedPitch value.
     * 
     * @return the isFixedPitch
     */
    public boolean isFixedPitch() {
        return this.isFixedPitch;
    }

    /**
     * Set the IsFixedPitch value.
     * 
     * @param value
     *            the isFixedPitch to set
     */
    public void setFixedPitch(final boolean value) {
        this.isFixedPitch = value;
    }

}
