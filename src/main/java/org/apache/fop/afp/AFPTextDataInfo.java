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

/* $Id: AFPTextDataInfo.java 746664 2009-02-22 12:40:44Z jeremias $ */

package org.apache.fop.afp;

import java.awt.Color;

/**
 * Text data information
 */
public class AFPTextDataInfo {

    /** the text font reference */
    private int fontReference;

    /** the text x coordinate position */
    private int x;

    /** the text y coordinate position */
    private int y;

    /** the text color */
    private Color color;

    /** the text variable space adjustment */
    private int variableSpaceCharacterIncrement;

    /** the text inter character adjustment */
    private int interCharacterAdjustment;

    /** the text orientation */
    private int rotation;

    /** the text encoding */
    private String textEncoding;

    /** the text string */
    private String textString;

    /**
     * Returns the font reference
     *
     * @return the font reference
     */
    public int getFontReference() {
        return this.fontReference;
    }

    /**
     * Sets the font reference
     *
     * @param fontReference
     *            the font reference
     */
    public void setFontReference(final int fontReference) {
        this.fontReference = fontReference;
    }

    /**
     * Returns the x coordinate
     *
     * @return the x coordinate
     */
    public int getX() {
        return this.x;
    }

    /**
     * Sets the X coordinate
     *
     * @param x
     *            the X coordinate
     */
    public void setX(final int x) {
        this.x = x;
    }

    /**
     * Returns the y coordinate
     *
     * @return the y coordinate
     */
    public int getY() {
        return this.y;
    }

    /**
     * Sets the Y coordinate
     *
     * @param y
     *            the Y coordinate
     */
    public void setY(final int y) {
        this.y = y;
    }

    /**
     * Returns the color
     *
     * @return the color
     */
    public Color getColor() {
        return this.color;
    }

    /**
     * Sets the color
     *
     * @param color
     *            the color
     */
    public void setColor(final Color color) {
        this.color = color;
    }

    /**
     * Return the variable space character increment
     *
     * @return the variable space character increment
     */
    public int getVariableSpaceCharacterIncrement() {
        return this.variableSpaceCharacterIncrement;
    }

    /**
     * Sets the variable space character increment
     *
     * @param variableSpaceCharacterIncrement
     *            the variable space character increment
     */
    public void setVariableSpaceCharacterIncrement(
            final int variableSpaceCharacterIncrement) {
        this.variableSpaceCharacterIncrement = variableSpaceCharacterIncrement;
    }

    /**
     * Return the inter character adjustment
     *
     * @return the inter character adjustment
     */
    public int getInterCharacterAdjustment() {
        return this.interCharacterAdjustment;
    }

    /**
     * Sets the inter character adjustment
     *
     * @param interCharacterAdjustment
     *            the inter character adjustment
     */
    public void setInterCharacterAdjustment(final int interCharacterAdjustment) {
        this.interCharacterAdjustment = interCharacterAdjustment;
    }

    /**
     * Sets the text orientation
     *
     * @param rotation
     *            the text rotation
     */
    public void setRotation(final int rotation) {
        this.rotation = rotation;
    }

    /**
     * Returns the text rotation
     *
     * @return the text rotation
     */
    public int getRotation() {
        return this.rotation;
    }

    /**
     * Sets the text encoding
     *
     * @param textEncoding
     *            the text encoding
     */
    public void setEncoding(final String textEncoding) {
        this.textEncoding = textEncoding;
    }

    /**
     * Returns the text encoding
     *
     * @return the text encoding
     */
    public String getEncoding() {
        return this.textEncoding;
    }

    /**
     * Sets the text string
     *
     * @param textString
     *            the text string
     */
    public void setString(final String textString) {
        this.textString = textString;
    }

    /**
     * Returns the text string
     *
     * @return the text string
     */
    public String getString() {
        return this.textString;
    }

    /** {@inheritDoc} */
    @Override
    public String toString() {
        return "TextDataInfo{fontReference=" + this.fontReference + ", x="
                + this.x + ", y=" + this.y + ", color=" + this.color
                + ", vsci=" + this.variableSpaceCharacterIncrement + ", ica="
                + this.interCharacterAdjustment + ", orientation="
                + this.rotation + ", textString=" + this.textString
                + ", textEncoding=" + this.textEncoding + "}";
    }
}