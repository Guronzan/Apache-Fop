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

/* $Id: BorderPaintingInfo.java 746664 2009-02-22 12:40:44Z jeremias $ */

package org.apache.fop.afp;

import java.awt.Color;

/**
 * Border painting information
 */
public class BorderPaintingInfo implements PaintingInfo {

    private final float x1;
    private final float y1;
    private final float x2;
    private final float y2;
    private final boolean isHorizontal;
    private final int style;
    private final Color color;

    /**
     * Main constructor
     *
     * @param x1
     *            the x1 coordinate
     * @param y1
     *            the y1 coordinate
     * @param x2
     *            the x2 coordinate
     * @param y2
     *            the y2 coordinate
     * @param isHorizontal
     *            true when the border line is horizontal
     * @param style
     *            the border style
     * @param color
     *            the border color
     */
    public BorderPaintingInfo(final float x1, final float y1, final float x2,
            final float y2, final boolean isHorizontal, final int style,
            final Color color) {
        this.x1 = x1;
        this.y1 = y1;
        this.x2 = x2;
        this.y2 = y2;
        this.isHorizontal = isHorizontal;
        this.style = style;
        this.color = color;
    }

    /**
     * Returns the x1 coordinate
     *
     * @return the x1 coordinate
     */
    public float getX1() {
        return this.x1;
    }

    /**
     * Returns the y1 coordinate
     *
     * @return the y1 coordinate
     */
    public float getY1() {
        return this.y1;
    }

    /**
     * Returns the x2 coordinate
     *
     * @return the x2 coordinate
     */
    public float getX2() {
        return this.x2;
    }

    /**
     * Returns the y2 coordinate
     *
     * @return the y2 coordinate
     */
    public float getY2() {
        return this.y2;
    }

    /**
     * Returns true when this is a horizontal line
     *
     * @return true when this is a horizontal line
     */
    public boolean isHorizontal() {
        return this.isHorizontal;
    }

    /**
     * Returns the style
     *
     * @return the style
     */
    public int getStyle() {
        return this.style;
    }

    /**
     * Returns the color
     *
     * @return the color
     */
    public Color getColor() {
        return this.color;
    }
}
