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

/* $Id: PageObject.java 746664 2009-02-22 12:40:44Z jeremias $ */

package org.apache.fop.afp.modca;

import java.io.IOException;
import java.io.OutputStream;

import org.apache.fop.afp.Factory;
import org.apache.fop.afp.Streamable;
import org.apache.fop.afp.ioca.ImageCellPosition;
import org.apache.fop.afp.ioca.ImageInputDescriptor;
import org.apache.fop.afp.ioca.ImageOutputControl;
import org.apache.fop.afp.ioca.ImageRasterData;
import org.apache.fop.afp.ioca.ImageRasterPattern;

/**
 * Pages contain the data objects that comprise a presentation document. Each
 * page has a set of data objects associated with it. Each page within a
 * document is independent from any other page, and each must establish its own
 * environment parameters.
 *
 * The page is the level in the document component hierarchy that is used for
 * printing or displaying a document's content. The data objects contained in
 * the page envelope in the data stream are presented when the page is
 * presented. Each data object has layout information associated with it that
 * directs the placement and orientation of the data on the page. In addition,
 * each page contains layout information that specifies the measurement units,
 * page width, and page depth.
 *
 * A page is initiated by a begin page structured field and terminated by an end
 * page structured field. Structured fields that define objects and active
 * environment groups or that specify attributes of the page may be encountered
 * in page state.
 *
 */
public class PageObject extends AbstractResourceGroupContainer {

    /**
     * Construct a new page object for the specified name argument, the page
     * name should be an 8 character identifier.
     *
     * @param factory
     *            the resource manager
     *
     * @param name
     *            the name of the page.
     * @param width
     *            the width of the page.
     * @param height
     *            the height of the page.
     * @param rotation
     *            the rotation of the page.
     * @param widthRes
     *            the width resolution of the page.
     * @param heightRes
     *            the height resolution of the page.
     */
    public PageObject(final Factory factory, final String name,
            final int width, final int height, final int rotation,
            final int widthRes, final int heightRes) {
        super(factory, name, width, height, rotation, widthRes, heightRes);
    }

    /**
     * Creates an IncludePageOverlay on the page.
     *
     * @param name
     *            the name of the overlay
     * @param x
     *            the x position of the overlay
     * @param y
     *            the y position of the overlay
     * @param orientation
     *            the orientation required for the overlay
     */
    public void createIncludePageOverlay(final String name, final int x,
            final int y, final int orientation) {
        getActiveEnvironmentGroup().createOverlay(name);
        final IncludePageOverlay ipo = new IncludePageOverlay(name, x, y,
                orientation);
        addObject(ipo);
    }

    /**
     * This method will create shading on the page using the specified
     * coordinates (the shading contrast is controlled via the red, green blue
     * parameters, by converting this to grayscale).
     *
     * @param x
     *            the x coordinate of the shading
     * @param y
     *            the y coordinate of the shading
     * @param w
     *            the width of the shaded area
     * @param h
     *            the height of the shaded area
     * @param red
     *            the red value
     * @param green
     *            the green value
     * @param blue
     *            the blue value
     */
    public void createShading(final int x, final int y, final int w,
            final int h, final int red, final int green, final int blue) {
        int xCoord = 0;
        int yCoord = 0;
        int areaWidth = 0;
        int areaHeight = 0;
        switch (this.rotation) {
        case 90:
            xCoord = areaWidth - y - h;
            yCoord = x;
            areaWidth = h;
            areaHeight = w;
            break;
        case 180:
            xCoord = areaWidth - x - w;
            yCoord = areaHeight - y - h;
            areaWidth = w;
            areaHeight = h;
            break;
        case 270:
            xCoord = y;
            yCoord = areaHeight - x - w;
            areaWidth = h;
            areaHeight = w;
            break;
        default:
            xCoord = x;
            yCoord = y;
            areaWidth = w;
            areaHeight = h;
            break;
        }

        // Convert the color to grey scale
        final float shade = (float) (red * 0.3 + green * 0.59 + blue * 0.11);

        final int grayscale = Math.round(shade / 255 * 16);

        final IMImageObject imImageObject = this.factory.createIMImageObject();

        final ImageOutputControl imageOutputControl = new ImageOutputControl(0,
                0);
        final ImageInputDescriptor imageInputDescriptor = new ImageInputDescriptor();
        final ImageCellPosition imageCellPosition = new ImageCellPosition(
                xCoord, yCoord);
        imageCellPosition.setXFillSize(areaWidth);
        imageCellPosition.setYFillSize(areaHeight);
        imageCellPosition.setXSize(64);
        imageCellPosition.setYSize(8);

        // defining this as a resource
        final byte[] rasterData = ImageRasterPattern.getRasterData(grayscale);
        final ImageRasterData imageRasterData = this.factory
                .createImageRasterData(rasterData);

        imImageObject.setImageOutputControl(imageOutputControl);
        imImageObject.setImageInputDescriptor(imageInputDescriptor);
        imImageObject.setImageCellPosition(imageCellPosition);
        imImageObject.setImageRasterData(imageRasterData);
        addObject(imImageObject);
    }

    /** {@inheritDoc} */
    @Override
    protected void writeStart(final OutputStream os) throws IOException {
        final byte[] data = new byte[17];
        copySF(data, Type.BEGIN, Category.PAGE);
        os.write(data);
    }

    /** {@inheritDoc} */
    @Override
    protected void writeContent(final OutputStream os) throws IOException {
        writeTriplets(os);

        getActiveEnvironmentGroup().writeToStream(os);

        writeObjects(this.tagLogicalElements, os);
        writeObjects(this.objects, os);
    }

    /** {@inheritDoc} */
    @Override
    protected void writeEnd(final OutputStream os) throws IOException {
        final byte[] data = new byte[17];
        copySF(data, Type.END, Category.PAGE);
        os.write(data);
    }

    /**
     * Adds an AFP object reference to this page
     *
     * @param obj
     *            an AFP object
     */
    @Override
    public void addObject(final Streamable obj) {
        endPresentationObject();
        super.addObject(obj);
    }

    /** {@inheritDoc} */
    @Override
    public String toString() {
        return getName();
    }

    /** {@inheritDoc} */
    @Override
    protected boolean canWrite(final AbstractAFPObject ao) {
        return true;
    }
}