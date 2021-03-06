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

/* $Id: AFPLineDataInfo.java 937315 2010-04-23 14:39:20Z jeremias $ */

package org.apache.fop.afp;

import java.awt.Color;

/** Line data information */
public class AFPLineDataInfo {

    /** the x1 coordinate */
    int x1;

    /** the y1 coordinate */
    int y1;

    /** the x2 coordinate */
    int x2;

    /** the y2 coordinate */
    int y2;

    /** the thickness */
    int thickness;

    /** the painting color */
    Color color;

    /** the rotation */
    int rotation = 0;

    /**
     * Default constructor
     */
    public AFPLineDataInfo() {
    }

    /**
     * Copy constructor.
     * 
     * @param template
     *            the object to copy
     */
    public AFPLineDataInfo(final AFPLineDataInfo template) {
        this.x1 = template.x1;
        this.y1 = template.y1;
        this.x2 = template.x2;
        this.y2 = template.y2;
        this.thickness = template.thickness;
        this.color = template.color;
        this.rotation = template.rotation;
    }

    /**
     * Returns the X1 coordinate
     *
     * @return the X1 coordinate
     */
    public int getX1() {
        return this.x1;
    }

    /**
     * Sets the X1 coordinate
     *
     * @param x1
     *            the X1 coordinate
     */
    public void setX1(final int x1) {
        this.x1 = x1;
    }

    /**
     * Returns the Y1 coordinate
     *
     * @return the Y1 coordinate
     */
    public int getY1() {
        return this.y1;
    }

    /**
     * Sets the Y1 coordinate
     *
     * @param y1
     *            the Y1 coordinate
     */
    public void setY1(final int y1) {
        this.y1 = y1;
    }

    /**
     * Returns the X2 coordinate
     *
     * @return the X2 coordinate
     */
    public int getX2() {
        return this.x2;
    }

    /**
     * Sets the X2 coordinate
     *
     * @param x2
     *            the X2 coordinate
     */
    public void setX2(final int x2) {
        this.x2 = x2;
    }

    /**
     * Returns the Y2 coordinate
     *
     * @return the Y2 coordinate
     */
    public int getY2() {
        return this.y2;
    }

    /**
     * Sets the Y2 coordinate
     *
     * @param y2
     *            the Y2 coordinate
     */
    public void setY2(final int y2) {
        this.y2 = y2;
    }

    /**
     * Returns the line thickness
     *
     * @return the line thickness
     */
    public int getThickness() {
        return this.thickness;
    }

    /**
     * Sets the line thickness
     *
     * @param thickness
     *            the line thickness
     */
    public void setThickness(final int thickness) {
        this.thickness = thickness;
    }

    /**
     * Returns line color
     *
     * @return the line color
     */
    public Color getColor() {
        return this.color;
    }

    /**
     * Sets the line color
     *
     * @param color
     *            the line color
     */
    public void setColor(final Color color) {
        this.color = color;
    }

    /**
     * Returns line rotation
     *
     * @return the line rotation
     */
    public int getRotation() {
        return this.rotation;
    }

    /**
     * Sets the line rotation
     *
     * @param rotation
     *            the line rotation
     */
    public void setRotation(final int rotation) {
        this.rotation = rotation;
    }

    /** {@inheritDoc} */
    @Override
    public String toString() {
        return "AFPLineDataInfo{x1=" + this.x1 + ", y1=" + this.y1 + ", x2="
                + this.x2 + ", y2=" + this.y2 + ", thickness=" + this.thickness
                + ", color=" + this.color + ", rotation=" + this.rotation + "}";
    }

}
