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

/* $Id: AFPObjectAreaInfo.java 746664 2009-02-22 12:40:44Z jeremias $ */

package org.apache.fop.afp;

/**
 * A common class used to convey locations, dimensions and resolutions of data
 * objects.
 */
public class AFPObjectAreaInfo {
    private int x;
    private int y;
    private int width;
    private int height;
    private int widthRes;
    private int heightRes;
    private int rotation = 0;

    /**
     * Sets the x position of the data object
     *
     * @param x
     *            the x position of the data object
     */
    public void setX(final int x) {
        this.x = x;
    }

    /**
     * Sets the y position of the data object
     *
     * @param y
     *            the y position of the data object
     */
    public void setY(final int y) {
        this.y = y;
    }

    /**
     * Sets the data object width
     *
     * @param width
     *            the width of the data object
     */
    public void setWidth(final int width) {
        this.width = width;
    }

    /**
     * Sets the data object height
     *
     * @param height
     *            the height of the data object
     */
    public void setHeight(final int height) {
        this.height = height;
    }

    /**
     * Sets the width resolution
     *
     * @param widthRes
     *            the width resolution
     */
    public void setWidthRes(final int widthRes) {
        this.widthRes = widthRes;
    }

    /**
     * Sets the height resolution
     *
     * @param heightRes
     *            the height resolution
     */
    public void setHeightRes(final int heightRes) {
        this.heightRes = heightRes;
    }

    /**
     * Returns the x coordinate of this data object
     *
     * @return the x coordinate of this data object
     */
    public int getX() {
        return this.x;
    }

    /**
     * Returns the y coordinate of this data object
     *
     * @return the y coordinate of this data object
     */
    public int getY() {
        return this.y;
    }

    /**
     * Returns the width of this data object
     *
     * @return the width of this data object
     */
    public int getWidth() {
        return this.width;
    }

    /**
     * Returns the height of this data object
     *
     * @return the height of this data object
     */
    public int getHeight() {
        return this.height;
    }

    /**
     * Returns the width resolution of this data object
     *
     * @return the width resolution of this data object
     */
    public int getWidthRes() {
        return this.widthRes;
    }

    /**
     * Returns the height resolution of this data object
     *
     * @return the height resolution of this data object
     */
    public int getHeightRes() {
        return this.heightRes;
    }

    /**
     * Returns the rotation of this data object
     *
     * @return the rotation of this data object
     */
    public int getRotation() {
        return this.rotation;
    }

    /**
     * Sets the data object rotation
     *
     * @param rotation
     *            the data object rotation
     */
    public void setRotation(final int rotation) {
        this.rotation = rotation;
    }

    /** {@inheritDoc} */
    @Override
    public String toString() {
        return "x=" + this.x + ", y=" + this.y + ", width=" + this.width
                + ", height=" + this.height + ", widthRes=" + this.widthRes
                + ", heightRes=" + this.heightRes + ", rotation="
                + this.rotation;
    }

}
