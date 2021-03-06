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

/* $Id: Factory.java 746664 2009-02-22 12:40:44Z jeremias $ */

package org.apache.fop.afp;

import java.io.OutputStream;

import org.apache.fop.afp.goca.GraphicsData;
import org.apache.fop.afp.ioca.ImageContent;
import org.apache.fop.afp.ioca.ImageRasterData;
import org.apache.fop.afp.ioca.ImageSegment;
import org.apache.fop.afp.ioca.ImageSizeParameter;
import org.apache.fop.afp.modca.ActiveEnvironmentGroup;
import org.apache.fop.afp.modca.ContainerDataDescriptor;
import org.apache.fop.afp.modca.Document;
import org.apache.fop.afp.modca.GraphicsDataDescriptor;
import org.apache.fop.afp.modca.GraphicsObject;
import org.apache.fop.afp.modca.IMImageObject;
import org.apache.fop.afp.modca.ImageDataDescriptor;
import org.apache.fop.afp.modca.ImageObject;
import org.apache.fop.afp.modca.IncludeObject;
import org.apache.fop.afp.modca.IncludePageSegment;
import org.apache.fop.afp.modca.InvokeMediumMap;
import org.apache.fop.afp.modca.MapCodedFont;
import org.apache.fop.afp.modca.MapContainerData;
import org.apache.fop.afp.modca.MapDataResource;
import org.apache.fop.afp.modca.ObjectAreaDescriptor;
import org.apache.fop.afp.modca.ObjectAreaPosition;
import org.apache.fop.afp.modca.ObjectContainer;
import org.apache.fop.afp.modca.ObjectEnvironmentGroup;
import org.apache.fop.afp.modca.Overlay;
import org.apache.fop.afp.modca.PageDescriptor;
import org.apache.fop.afp.modca.PageGroup;
import org.apache.fop.afp.modca.PageObject;
import org.apache.fop.afp.modca.PresentationEnvironmentControl;
import org.apache.fop.afp.modca.PresentationTextDescriptor;
import org.apache.fop.afp.modca.PresentationTextObject;
import org.apache.fop.afp.modca.ResourceEnvironmentGroup;
import org.apache.fop.afp.modca.ResourceGroup;
import org.apache.fop.afp.modca.ResourceObject;
import org.apache.fop.afp.modca.StreamedResourceGroup;
import org.apache.fop.afp.modca.TagLogicalElement;
import org.apache.fop.afp.util.StringUtils;

/**
 * Creator of MO:DCA structured field objects
 */
public class Factory {

    private static final String OBJECT_ENVIRONMENT_GROUP_NAME_PREFIX = "OEG";

    private static final String ACTIVE_ENVIRONMENT_GROUP_NAME_PREFIX = "AEG";

    private static final String IMAGE_NAME_PREFIX = "IMG";

    private static final String GRAPHIC_NAME_PREFIX = "GRA";

    private static final String OBJECT_CONTAINER_NAME_PREFIX = "OC";

    private static final String RESOURCE_NAME_PREFIX = "RES";

    private static final String RESOURCE_GROUP_NAME_PREFIX = "RG";

    private static final String PAGE_GROUP_NAME_PREFIX = "PGP";

    private static final String PAGE_NAME_PREFIX = "PGN";

    private static final String OVERLAY_NAME_PREFIX = "OVL";

    private static final String PRESENTATION_TEXT_NAME_PREFIX = "PT";

    private static final String DOCUMENT_NAME_PREFIX = "DOC";

    private static final String IM_IMAGE_NAME_PREFIX = "IMIMG";

    private static final String IMAGE_SEGMENT_NAME_PREFIX = "IS";

    /** the page group count */
    private int pageGroupCount = 0;

    /** the page count */
    private int pageCount = 0;

    /** the image count */
    private int imageCount = 0;

    /** the im image count */
    private int imImageCount = 0;

    /** the image segment count */
    private int imageSegmentCount = 0;

    /** the graphic count */
    private int graphicCount = 0;

    /** the object container count */
    private int objectContainerCount = 0;

    /** the resource count */
    private int resourceCount = 0;

    /** the resource group count */
    private int resourceGroupCount = 0;

    /** the overlay count */
    private int overlayCount = 0;

    /** the presentation text object count */
    private int textObjectCount = 0;

    /** the active environment group count */
    private int activeEnvironmentGroupCount = 0;

    /** the document count */
    private int documentCount = 0;

    /** the object environment group count */
    private int objectEnvironmentGroupCount = 0;

    /**
     * Main constructor
     */
    public Factory() {
    }

    /**
     * Creates a new IOCA {@link ImageObject}
     *
     * @return a new {@link ImageObject}
     */
    public ImageObject createImageObject() {
        final String name = IMAGE_NAME_PREFIX
                + StringUtils.lpad(String.valueOf(++this.imageCount), '0', 5);
        final ImageObject imageObject = new ImageObject(this, name);
        return imageObject;
    }

    /**
     * Creates an IOCA {@link IMImageObject}
     *
     * @return a new {@link IMImageObject}
     */
    public IMImageObject createIMImageObject() {
        final String name = IM_IMAGE_NAME_PREFIX
                + StringUtils.lpad(String.valueOf(++this.imImageCount), '0', 3);
        final IMImageObject imImageObject = new IMImageObject(name);
        return imImageObject;
    }

    /**
     * Creates a new GOCA {@link GraphicsObject}
     *
     * @return a new {@link GraphicsObject}
     */
    public GraphicsObject createGraphicsObject() {
        final String name = GRAPHIC_NAME_PREFIX
                + StringUtils.lpad(String.valueOf(++this.graphicCount), '0', 5);
        final GraphicsObject graphicsObj = new GraphicsObject(this, name);
        return graphicsObj;
    }

    /**
     * Creates a new MO:DCA {@link ObjectContainer}
     *
     * @return a new {@link ObjectContainer}
     */
    public ObjectContainer createObjectContainer() {
        final String name = OBJECT_CONTAINER_NAME_PREFIX
                + StringUtils.lpad(String.valueOf(++this.objectContainerCount),
                        '0', 6);
        return new ObjectContainer(this, name);
    }

    /**
     * Creates a new MO:DCA {@link ResourceObject}
     *
     * @param resourceName
     *            the resource object name
     * @return a new {@link ResourceObject}
     */
    public ResourceObject createResource(final String resourceName) {
        return new ResourceObject(resourceName);
    }

    /**
     * Creates a new MO:DCA {@link ResourceObject}
     *
     * @return a new {@link ResourceObject}
     */
    public ResourceObject createResource() {
        final String name = RESOURCE_NAME_PREFIX
                + StringUtils
                .lpad(String.valueOf(++this.resourceCount), '0', 5);
        return createResource(name);
    }

    /**
     * Creates a new MO:DCA {@link PageGroup}
     *
     * @param tleSequence
     *            current start tle sequence number within stream
     * @return a new {@link PageGroup}
     */
    public PageGroup createPageGroup(final int tleSequence) {
        final String name = PAGE_GROUP_NAME_PREFIX
                + StringUtils.lpad(String.valueOf(++this.pageGroupCount), '0',
                        5);
        return new PageGroup(this, name, tleSequence);
    }

    /**
     * Creates a new MO:DCA {@link ActiveEnvironmentGroup}
     *
     * @param width
     *            the page width
     * @param height
     *            the page height
     * @param widthRes
     *            the page width resolution
     * @param heightRes
     *            the page height resolution
     * @return a new {@link ActiveEnvironmentGroup}
     */
    public ActiveEnvironmentGroup createActiveEnvironmentGroup(final int width,
            final int height, final int widthRes, final int heightRes) {
        final String name = ACTIVE_ENVIRONMENT_GROUP_NAME_PREFIX
                + StringUtils.lpad(
                        String.valueOf(++this.activeEnvironmentGroupCount),
                        '0', 5);
        return new ActiveEnvironmentGroup(this, name, width, height, widthRes,
                heightRes);
    }

    /**
     * Creates a new MO:DCA {@link ResourceGroup}
     *
     * @return a new {@link ResourceGroup}
     */
    public ResourceGroup createResourceGroup() {
        final String name = RESOURCE_GROUP_NAME_PREFIX
                + StringUtils.lpad(String.valueOf(++this.resourceGroupCount),
                        '0', 6);
        return new ResourceGroup(name);
    }

    /**
     * Creates a new MO:DCA {@link StreamedResourceGroup}
     *
     * @param os
     *            the outputstream of the streamed resource group
     * @return a new {@link StreamedResourceGroup}
     */
    public StreamedResourceGroup createStreamedResourceGroup(
            final OutputStream os) {
        final String name = RESOURCE_GROUP_NAME_PREFIX
                + StringUtils.lpad(String.valueOf(++this.resourceGroupCount),
                        '0', 6);
        return new StreamedResourceGroup(name, os);
    }

    /**
     * Creates a new MO:DCA {@link PageObject}.
     *
     * @param pageWidth
     *            the width of the page
     * @param pageHeight
     *            the height of the page
     * @param pageRotation
     *            the rotation of the page
     * @param pageWidthRes
     *            the width resolution of the page
     * @param pageHeightRes
     *            the height resolution of the page
     *
     * @return a new {@link PageObject}
     */
    public PageObject createPage(final int pageWidth, final int pageHeight,
            final int pageRotation, final int pageWidthRes,
            final int pageHeightRes) {
        final String pageName = PAGE_NAME_PREFIX
                + StringUtils.lpad(String.valueOf(++this.pageCount), '0', 5);
        return new PageObject(this, pageName, pageWidth, pageHeight,
                pageRotation, pageWidthRes, pageHeightRes);
    }

    /**
     * Creates a new MO:DCA {@link PresentationTextObject}.
     *
     * @return a new {@link PresentationTextObject}
     */
    public PresentationTextObject createPresentationTextObject() {
        final String textObjectName = PRESENTATION_TEXT_NAME_PREFIX
                + StringUtils.lpad(String.valueOf(++this.textObjectCount), '0',
                        6);
        return new PresentationTextObject(textObjectName);
    }

    /**
     * Creates a new MO:DCA {@link Overlay}.
     *
     * @param width
     *            the width of the overlay
     * @param height
     *            the height of the overlay
     * @param widthRes
     *            the width resolution of the overlay
     * @param heightRes
     *            the height resolution of the overlay
     * @param overlayRotation
     *            the rotation of the overlay
     *
     * @return a new {@link Overlay}.
     */
    public Overlay createOverlay(final int width, final int height,
            final int widthRes, final int heightRes, final int overlayRotation) {
        final String overlayName = OVERLAY_NAME_PREFIX
                + StringUtils.lpad(String.valueOf(++this.overlayCount), '0', 5);
        final Overlay overlay = new Overlay(this, overlayName, width, height,
                overlayRotation, widthRes, heightRes);
        return overlay;
    }

    /**
     * Creates a MO:DCA {@link Document}
     *
     * @return a new {@link Document}
     */
    public Document createDocument() {
        final String documentName = DOCUMENT_NAME_PREFIX
                + StringUtils
                .lpad(String.valueOf(++this.documentCount), '0', 5);
        final Document document = new Document(this, documentName);
        return document;
    }

    /**
     * Creates a MO:DCA {@link MapCodedFont}
     *
     * @return a new {@link MapCodedFont}
     */
    public MapCodedFont createMapCodedFont() {
        final MapCodedFont mapCodedFont = new MapCodedFont();
        return mapCodedFont;
    }

    /**
     * Creates a MO:DCA {@link IncludePageSegment}
     *
     * @param name
     *            the page segment name
     * @param x
     *            the x coordinate
     * @param y
     *            the y coordinate
     *
     * @return a new {@link IncludePageSegment}
     */
    public IncludePageSegment createIncludePageSegment(final String name,
            final int x, final int y) {
        final IncludePageSegment includePageSegment = new IncludePageSegment(
                name, x, y);
        return includePageSegment;
    }

    /**
     * Creates a MO:DCA {@link IncludeObject}
     *
     * @param name
     *            the name of this include object
     * @return a new {@link IncludeObject}
     */
    public IncludeObject createInclude(final String name) {
        final IncludeObject includeObject = new IncludeObject(name);
        return includeObject;
    }

    /**
     * Creates a MO:DCA {@link TagLogicalElement}
     *
     * @param name
     *            name of the element
     * @param value
     *            value of the element
     * @param tleSequence
     *            current start tle sequence number within stream*
     * @return a new {@link TagLogicalElement}
     */
    public TagLogicalElement createTagLogicalElement(final String name,
            final String value, final int tleSequence) {
        final TagLogicalElement tle = new TagLogicalElement(name, value,
                tleSequence);
        return tle;
    }

    /**
     * Creates a new {@link DataStream}
     *
     * @param paintingState
     *            the AFP painting state
     * @param outputStream
     *            an outputstream to write to
     * @return a new {@link DataStream}
     */
    public DataStream createDataStream(final AFPPaintingState paintingState,
            final OutputStream outputStream) {
        final DataStream dataStream = new DataStream(this, paintingState,
                outputStream);
        return dataStream;
    }

    /**
     * Creates a new MO:DCA {@link PageDescriptor}
     *
     * @param width
     *            the page width.
     * @param height
     *            the page height.
     * @param widthRes
     *            the page width resolution.
     * @param heightRes
     *            the page height resolution.
     * @return a new {@link PageDescriptor}
     */
    public PageDescriptor createPageDescriptor(final int width,
            final int height, final int widthRes, final int heightRes) {
        final PageDescriptor pageDescriptor = new PageDescriptor(width, height,
                widthRes, heightRes);
        return pageDescriptor;
    }

    /**
     * Returns a new MO:DCA {@link ObjectEnvironmentGroup}
     *
     * @return a new {@link ObjectEnvironmentGroup}
     */
    public ObjectEnvironmentGroup createObjectEnvironmentGroup() {
        final String oegName = OBJECT_ENVIRONMENT_GROUP_NAME_PREFIX
                + StringUtils.lpad(
                        String.valueOf(++this.objectEnvironmentGroupCount),
                        '0', 5);
        final ObjectEnvironmentGroup objectEnvironmentGroup = new ObjectEnvironmentGroup(
                oegName);
        return objectEnvironmentGroup;
    }

    /**
     * Creates a new GOCA {@link GraphicsData}
     *
     * @return a new {@link GraphicsData}
     */
    public GraphicsData createGraphicsData() {
        final GraphicsData graphicsData = new GraphicsData();
        return graphicsData;
    }

    /**
     * Creates a new {@link ObjectAreaDescriptor}
     *
     * @param width
     *            the object width.
     * @param height
     *            the object height.
     * @param widthRes
     *            the object width resolution.
     * @param heightRes
     *            the object height resolution.
     * @return a new {@link ObjectAreaDescriptor}
     */
    public ObjectAreaDescriptor createObjectAreaDescriptor(final int width,
            final int height, final int widthRes, final int heightRes) {
        final ObjectAreaDescriptor objectAreaDescriptor = new ObjectAreaDescriptor(
                width, height, widthRes, heightRes);
        return objectAreaDescriptor;
    }

    /**
     * Creates a new {@link ObjectAreaPosition}
     *
     * @param x
     *            the x coordinate.
     * @param y
     *            the y coordinate.
     * @param rotation
     *            the coordinate system rotation (must be 0, 90, 180, 270).
     * @return a new {@link ObjectAreaPosition}
     */
    public ObjectAreaPosition createObjectAreaPosition(final int x,
            final int y, final int rotation) {
        final ObjectAreaPosition objectAreaPosition = new ObjectAreaPosition(x,
                y, rotation);
        return objectAreaPosition;
    }

    /**
     * Creates a new {@link ImageDataDescriptor}
     *
     * @param width
     *            the image width
     * @param height
     *            the image height
     * @param widthRes
     *            the x resolution of the image
     * @param heightRes
     *            the y resolution of the image
     * @return a new {@link ImageDataDescriptor}
     */
    public ImageDataDescriptor createImageDataDescriptor(final int width,
            final int height, final int widthRes, final int heightRes) {
        final ImageDataDescriptor imageDataDescriptor = new ImageDataDescriptor(
                width, height, widthRes, heightRes);
        return imageDataDescriptor;
    }

    /**
     * Creates a new GOCA {@link GraphicsDataDescriptor}
     *
     * @param xlwind
     *            the left edge of the graphics window
     * @param xrwind
     *            the right edge of the graphics window
     * @param ybwind
     *            the top edge of the graphics window
     * @param ytwind
     *            the bottom edge of the graphics window
     * @param widthRes
     *            the x resolution of the graphics window
     * @param heightRes
     *            the y resolution of the graphics window
     * @return a new {@link GraphicsDataDescriptor}
     */
    public GraphicsDataDescriptor createGraphicsDataDescriptor(
            final int xlwind, final int xrwind, final int ybwind,
            final int ytwind, final int widthRes, final int heightRes) {
        final GraphicsDataDescriptor graphicsDataDescriptor = new GraphicsDataDescriptor(
                xlwind, xrwind, ybwind, ytwind, widthRes, heightRes);
        return graphicsDataDescriptor;
    }

    /**
     * Creates a new MO:DCA {@link ContainerDataDescriptor}
     *
     * @param dataWidth
     *            the container data width
     * @param dataHeight
     *            the container data height
     * @param widthRes
     *            the container data width resolution
     * @param heightRes
     *            the container data height resolution
     * @return a new {@link ContainerDataDescriptor}
     */
    public ContainerDataDescriptor createContainerDataDescriptor(
            final int dataWidth, final int dataHeight, final int widthRes,
            final int heightRes) {
        final ContainerDataDescriptor containerDataDescriptor = new ContainerDataDescriptor(
                dataWidth, dataHeight, widthRes, heightRes);
        return containerDataDescriptor;
    }

    /**
     * Creates a new MO:DCA {@link MapContainerData}
     *
     * @param optionValue
     *            the option value
     * @return a new {@link MapContainerData}
     */
    public MapContainerData createMapContainerData(final byte optionValue) {
        final MapContainerData mapContainerData = new MapContainerData(
                optionValue);
        return mapContainerData;
    }

    /**
     * Creates a new MO:DCA {@link MapDataResource}
     *
     * @return a new {@link MapDataResource}
     */
    public MapDataResource createMapDataResource() {
        final MapDataResource mapDataResource = new MapDataResource();
        return mapDataResource;
    }

    /**
     * Creates a new PTOCA {@link PresentationTextDescriptor}
     *
     * @return a new {@link PresentationTextDescriptor}
     */
    public PresentationTextDescriptor createPresentationTextDataDescriptor(
            final int width, final int height, final int widthRes,
            final int heightRes) {
        final PresentationTextDescriptor presentationTextDescriptor = new PresentationTextDescriptor(
                width, height, widthRes, heightRes);
        return presentationTextDescriptor;
    }

    /**
     * Creates a new MO:DCA {@link PresentationEnvironmentControl}
     *
     * @return a new {@link PresentationEnvironmentControl}
     */
    public PresentationEnvironmentControl createPresentationEnvironmentControl() {
        final PresentationEnvironmentControl presentationEnvironmentControl = new PresentationEnvironmentControl();
        return presentationEnvironmentControl;
    }

    /**
     * Creates a new MO:DCA {@link InvokeMediumMap}
     *
     * @param name
     *            the object name
     * @return a new {@link InvokeMediumMap}
     */
    public InvokeMediumMap createInvokeMediumMap(final String name) {
        final InvokeMediumMap invokeMediumMap = new InvokeMediumMap(name);
        return invokeMediumMap;
    }

    /**
     * Creates a new MO:DCA {@link ResourceEnvironmentGroup}
     *
     * @return a new {@link ResourceEnvironmentGroup}
     */
    public ResourceEnvironmentGroup createResourceEnvironmentGroup() {
        final ResourceEnvironmentGroup resourceEnvironmentGroup = new ResourceEnvironmentGroup();
        return resourceEnvironmentGroup;
    }

    /**
     * Creates a new IOCA {@link ImageSegment}
     *
     * @return a new {@link ImageSegment}
     */
    public ImageSegment createImageSegment() {
        final String name = IMAGE_SEGMENT_NAME_PREFIX
                + StringUtils.lpad(String.valueOf(++this.imageSegmentCount),
                        '0', 2);
        final ImageSegment imageSegment = new ImageSegment(this, name);
        return imageSegment;
    }

    /**
     * Creates an new IOCA {@link ImageContent}
     *
     * @return an {@link ImageContent}
     */
    public ImageContent createImageContent() {
        final ImageContent imageContent = new ImageContent();
        return imageContent;
    }

    /**
     * Creates a new IOCA {@link ImageRasterData}
     *
     * @param rasterData
     *            raster data
     * @return a new {@link ImageRasterData}
     */
    public ImageRasterData createImageRasterData(final byte[] rasterData) {
        final ImageRasterData imageRasterData = new ImageRasterData(rasterData);
        return imageRasterData;
    }

    /**
     * Creates an new IOCA {@link ImageSizeParameter}.
     *
     * @param hsize
     *            The horizontal size of the image.
     * @param vsize
     *            The vertical size of the image.
     * @param hresol
     *            The horizontal resolution of the image.
     * @param vresol
     *            The vertical resolution of the image.
     * @return a new {@link ImageSizeParameter}
     */
    public ImageSizeParameter createImageSizeParameter(final int hsize,
            final int vsize, final int hresol, final int vresol) {
        final ImageSizeParameter imageSizeParameter = new ImageSizeParameter(
                hsize, vsize, hresol, vresol);
        return imageSizeParameter;
    }

}
