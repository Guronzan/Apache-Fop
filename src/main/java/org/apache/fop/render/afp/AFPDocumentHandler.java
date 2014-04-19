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

/* $Id: AFPDocumentHandler.java 953952 2010-06-12 08:19:48Z jeremias $ */

package org.apache.fop.render.afp;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.geom.AffineTransform;
import java.io.IOException;
import java.util.Map;

import org.apache.fop.afp.AFPDitheredRectanglePainter;
import org.apache.fop.afp.AFPPaintingState;
import org.apache.fop.afp.AFPRectanglePainter;
import org.apache.fop.afp.AFPResourceLevelDefaults;
import org.apache.fop.afp.AFPResourceManager;
import org.apache.fop.afp.AFPUnitConverter;
import org.apache.fop.afp.AbstractAFPPainter;
import org.apache.fop.afp.DataStream;
import org.apache.fop.afp.fonts.AFPFontCollection;
import org.apache.fop.afp.fonts.AFPPageFonts;
import org.apache.fop.afp.modca.ResourceObject;
import org.apache.fop.afp.util.DefaultFOPResourceAccessor;
import org.apache.fop.afp.util.ResourceAccessor;
import org.apache.fop.fonts.FontCollection;
import org.apache.fop.fonts.FontEventAdapter;
import org.apache.fop.fonts.FontInfo;
import org.apache.fop.fonts.FontManager;
import org.apache.fop.render.afp.extensions.AFPElementMapping;
import org.apache.fop.render.afp.extensions.AFPIncludeFormMap;
import org.apache.fop.render.afp.extensions.AFPInvokeMediumMap;
import org.apache.fop.render.afp.extensions.AFPPageOverlay;
import org.apache.fop.render.afp.extensions.AFPPageSetup;
import org.apache.fop.render.intermediate.AbstractBinaryWritingIFDocumentHandler;
import org.apache.fop.render.intermediate.IFDocumentHandler;
import org.apache.fop.render.intermediate.IFDocumentHandlerConfigurator;
import org.apache.fop.render.intermediate.IFException;
import org.apache.fop.render.intermediate.IFPainter;

/**
 * {@link IFDocumentHandler} implementation that produces AFP (MO:DCA).
 */
public class AFPDocumentHandler extends AbstractBinaryWritingIFDocumentHandler
implements AFPCustomizable {

    // ** logging instance */
    // private static Log log = LogFactory.getLog(AFPDocumentHandler.class);

    /** the resource manager */
    private AFPResourceManager resourceManager;

    /** the painting state */
    private final AFPPaintingState paintingState;

    /** unit converter */
    private final AFPUnitConverter unitConv;

    /** the AFP datastream */
    private DataStream dataStream;

    /** the map of page segments */
    private final Map/* <String,String> */pageSegmentMap = new java.util.HashMap/*
     * <
     * String
     * ,
     * String
     * >
     */();

    /** Medium Map referenced on previous page **/
    private String lastMediumMap;

    private static final int LOC_ELSEWHERE = 0;
    private static final int LOC_FOLLOWING_PAGE_SEQUENCE = 1;
    private static final int LOC_IN_PAGE_HEADER = 2;

    private int location = LOC_ELSEWHERE;

    /** the shading mode for filled rectangles */
    private AFPShadingMode shadingMode = AFPShadingMode.COLOR;

    /**
     * Default constructor.
     */
    public AFPDocumentHandler() {
        this.resourceManager = new AFPResourceManager();
        this.paintingState = new AFPPaintingState();
        this.unitConv = this.paintingState.getUnitConverter();
    }

    /** {@inheritDoc} */
    @Override
    public boolean supportsPagesOutOfOrder() {
        return false;
    }

    /** {@inheritDoc} */
    @Override
    public String getMimeType() {
        return org.apache.xmlgraphics.util.MimeConstants.MIME_AFP;
    }

    /** {@inheritDoc} */
    @Override
    public IFDocumentHandlerConfigurator getConfigurator() {
        return new AFPRendererConfigurator(getUserAgent());
    }

    /** {@inheritDoc} */
    @Override
    public void setDefaultFontInfo(final FontInfo fontInfo) {
        final FontManager fontManager = getUserAgent().getFactory()
                .getFontManager();
        final FontCollection[] fontCollections = new FontCollection[] { new AFPFontCollection(
                getUserAgent().getEventBroadcaster(), null) };

        final FontInfo fi = fontInfo != null ? fontInfo : new FontInfo();
        fi.setEventListener(new FontEventAdapter(getUserAgent()
                .getEventBroadcaster()));
        fontManager.setup(fi, fontCollections);
        setFontInfo(fi);
    }

    AFPPaintingState getPaintingState() {
        return this.paintingState;
    }

    DataStream getDataStream() {
        return this.dataStream;
    }

    AFPResourceManager getResourceManager() {
        return this.resourceManager;
    }

    AbstractAFPPainter createRectanglePainter() {
        if (AFPShadingMode.DITHERED.equals(this.shadingMode)) {
            return new AFPDitheredRectanglePainter(getPaintingState(),
                    getDataStream(), getResourceManager());
        } else {
            return new AFPRectanglePainter(getPaintingState(), getDataStream());
        }
    }

    /** {@inheritDoc} */
    @Override
    public void startDocument() throws IFException {
        super.startDocument();
        try {
            this.paintingState.setColor(Color.WHITE);

            this.dataStream = this.resourceManager.createDataStream(
                    this.paintingState, this.outputStream);

            this.dataStream.startDocument();
        } catch (final IOException e) {
            throw new IFException("I/O error in startDocument()", e);
        }
    }

    /** {@inheritDoc} */
    @Override
    public void endDocumentHeader() throws IFException {
    }

    /** {@inheritDoc} */
    @Override
    public void endDocument() throws IFException {
        try {
            this.dataStream.endDocument();
            this.dataStream = null;
            this.resourceManager.writeToStream();
            this.resourceManager = null;
        } catch (final IOException ioe) {
            throw new IFException("I/O error in endDocument()", ioe);
        }
        super.endDocument();
    }

    /** {@inheritDoc} */
    @Override
    public void startPageSequence(final String id) throws IFException {
        try {
            this.dataStream.startPageGroup();
        } catch (final IOException ioe) {
            throw new IFException("I/O error in startPageSequence()", ioe);
        }
        this.location = LOC_FOLLOWING_PAGE_SEQUENCE;
    }

    /** {@inheritDoc} */
    @Override
    public void endPageSequence() throws IFException {
        // nop
    }

    /**
     * Returns the base AFP transform
     *
     * @return the base AFP transform
     */
    private AffineTransform getBaseTransform() {
        final AffineTransform baseTransform = new AffineTransform();
        final double scale = this.unitConv.mpt2units(1);
        baseTransform.scale(scale, scale);
        return baseTransform;
    }

    /** {@inheritDoc} */
    @Override
    public void startPage(final int index, final String name,
            final String pageMasterName, final Dimension size)
                    throws IFException {
        this.location = LOC_ELSEWHERE;
        this.paintingState.clear();
        this.pageSegmentMap.clear();

        final AffineTransform baseTransform = getBaseTransform();
        this.paintingState.concatenate(baseTransform);

        final int pageWidth = Math.round(this.unitConv.mpt2units(size.width));
        this.paintingState.setPageWidth(pageWidth);

        final int pageHeight = Math.round(this.unitConv.mpt2units(size.height));
        this.paintingState.setPageHeight(pageHeight);

        final int pageRotation = this.paintingState.getPageRotation();
        final int resolution = this.paintingState.getResolution();

        this.dataStream.startPage(pageWidth, pageHeight, pageRotation,
                resolution, resolution);
    }

    /** {@inheritDoc} */
    @Override
    public void startPageHeader() throws IFException {
        super.startPageHeader();
        this.location = LOC_IN_PAGE_HEADER;
    }

    /** {@inheritDoc} */
    @Override
    public void endPageHeader() throws IFException {
        this.location = LOC_ELSEWHERE;
        super.endPageHeader();
    }

    /** {@inheritDoc} */
    @Override
    public IFPainter startPageContent() throws IFException {
        return new AFPPainter(this);
    }

    /** {@inheritDoc} */
    @Override
    public void endPageContent() throws IFException {
    }

    /** {@inheritDoc} */
    @Override
    public void endPage() throws IFException {
        try {
            final AFPPageFonts pageFonts = this.paintingState.getPageFonts();
            if (pageFonts != null && !pageFonts.isEmpty()) {
                this.dataStream.addFontsToCurrentPage(pageFonts);
            }

            this.dataStream.endPage();
        } catch (final IOException ioe) {
            throw new IFException("I/O error in endPage()", ioe);
        }
    }

    /** {@inheritDoc} */
    @Override
    public void handleExtensionObject(final Object extension)
            throws IFException {
        if (extension instanceof AFPPageSetup) {
            final AFPPageSetup aps = (AFPPageSetup) extension;
            final String element = aps.getElementName();
            if (AFPElementMapping.TAG_LOGICAL_ELEMENT.equals(element)) {
                if (this.location != LOC_IN_PAGE_HEADER
                        && this.location != LOC_FOLLOWING_PAGE_SEQUENCE) {
                    throw new IFException(
                            "TLE extension must be in the page header or between page-sequence"
                                    + " and the first page: " + aps, null);
                }
                final String name = aps.getName();
                final String value = aps.getValue();
                this.dataStream.createTagLogicalElement(name, value);
            } else {
                if (this.location != LOC_IN_PAGE_HEADER) {
                    throw new IFException(
                            "AFP page setup extension encountered outside the page header: "
                                    + aps, null);
                }
                if (AFPElementMapping.INCLUDE_PAGE_SEGMENT.equals(element)) {
                    final String name = aps.getName();
                    final String source = aps.getValue();
                    this.pageSegmentMap.put(source, name);
                } else if (AFPElementMapping.NO_OPERATION.equals(element)) {
                    final String content = aps.getContent();
                    if (content != null) {
                        this.dataStream.createNoOperation(content);
                    }
                }
            }
        } else if (extension instanceof AFPPageOverlay) {
            final AFPPageOverlay ipo = (AFPPageOverlay) extension;
            if (this.location != LOC_IN_PAGE_HEADER) {
                throw new IFException(
                        "AFP page overlay extension encountered outside the page header: "
                                + ipo, null);
            }
            final String overlay = ipo.getName();
            if (overlay != null) {
                this.dataStream.createIncludePageOverlay(overlay, ipo.getX(),
                        ipo.getY());
            }
        } else if (extension instanceof AFPInvokeMediumMap) {
            if (this.location != LOC_FOLLOWING_PAGE_SEQUENCE
                    && this.location != LOC_IN_PAGE_HEADER) {

                throw new IFException(
                        "AFP IMM extension must be between page-sequence"
                                + " and the first page or child of page-header: "
                                + extension, null);
            }
            final AFPInvokeMediumMap imm = (AFPInvokeMediumMap) extension;
            final String mediumMap = imm.getName();
            if (mediumMap != null && !mediumMap.equals(this.lastMediumMap)) {
                this.dataStream.createInvokeMediumMap(mediumMap);
                this.lastMediumMap = mediumMap;
            }
        } else if (extension instanceof AFPIncludeFormMap) {
            final AFPIncludeFormMap formMap = (AFPIncludeFormMap) extension;
            final ResourceAccessor accessor = new DefaultFOPResourceAccessor(
                    getUserAgent(), null, null);
            try {
                getResourceManager()
                .createIncludedResource(formMap.getName(),
                        formMap.getSrc(), accessor,
                        ResourceObject.TYPE_FORMDEF);
            } catch (final IOException ioe) {
                throw new IFException(
                        "I/O error while embedding form map resource: "
                                + formMap.getName(), ioe);
            }
        }
    }

    // ---=== AFPCustomizable ===---

    /** {@inheritDoc} */
    @Override
    public void setBitsPerPixel(final int bitsPerPixel) {
        this.paintingState.setBitsPerPixel(bitsPerPixel);
    }

    /** {@inheritDoc} */
    @Override
    public void setColorImages(final boolean colorImages) {
        this.paintingState.setColorImages(colorImages);
    }

    /** {@inheritDoc} */
    @Override
    public void setNativeImagesSupported(final boolean nativeImages) {
        this.paintingState.setNativeImagesSupported(nativeImages);
    }

    /** {@inheritDoc} */
    @Override
    public void setCMYKImagesSupported(final boolean value) {
        this.paintingState.setCMYKImagesSupported(value);
    }

    /** {@inheritDoc} */
    @Override
    public void setDitheringQuality(final float quality) {
        this.paintingState.setDitheringQuality(quality);
    }

    /** {@inheritDoc} */
    @Override
    public void setShadingMode(final AFPShadingMode shadingMode) {
        this.shadingMode = shadingMode;
    }

    /** {@inheritDoc} */
    @Override
    public void setResolution(final int resolution) {
        this.paintingState.setResolution(resolution);
    }

    /** {@inheritDoc} */
    @Override
    public int getResolution() {
        return this.paintingState.getResolution();
    }

    /** {@inheritDoc} */
    @Override
    public void setDefaultResourceGroupFilePath(final String filePath) {
        this.resourceManager.setDefaultResourceGroupFilePath(filePath);
    }

    /** {@inheritDoc} */
    @Override
    public void setResourceLevelDefaults(final AFPResourceLevelDefaults defaults) {
        this.resourceManager.setResourceLevelDefaults(defaults);
    }

    /**
     * Returns the page segment name for a given URI if it actually represents a
     * page segment. Otherwise, it just returns null.
     *
     * @param uri
     *            the URI that identifies the page segment
     * @return the page segment name or null if there's no page segment for the
     *         given URI
     */
    String getPageSegmentNameFor(final String uri) {
        return (String) this.pageSegmentMap.get(uri);
    }

}
