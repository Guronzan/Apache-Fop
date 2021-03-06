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

/* $Id: DataStream.java 894384 2009-12-29 13:38:52Z cbowditch $ */

package org.apache.fop.afp;

import java.awt.Color;
import java.awt.Point;
import java.io.IOException;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.util.Iterator;
import java.util.Map;

import lombok.extern.slf4j.Slf4j;

import org.apache.fop.afp.fonts.AFPFont;
import org.apache.fop.afp.fonts.AFPFontAttributes;
import org.apache.fop.afp.fonts.CharacterSet;
import org.apache.fop.afp.modca.AbstractPageObject;
import org.apache.fop.afp.modca.Document;
import org.apache.fop.afp.modca.InterchangeSet;
import org.apache.fop.afp.modca.Overlay;
import org.apache.fop.afp.modca.PageGroup;
import org.apache.fop.afp.modca.PageObject;
import org.apache.fop.afp.modca.ResourceGroup;
import org.apache.fop.afp.modca.TagLogicalElementBean;
import org.apache.fop.afp.modca.triplets.FullyQualifiedNameTriplet;
import org.apache.fop.afp.ptoca.PtocaBuilder;
import org.apache.fop.afp.ptoca.PtocaProducer;
import org.apache.fop.fonts.Font;
import org.apache.fop.util.CharUtilities;

/**
 * A data stream is a continuous ordered stream of data elements and objects
 * conforming to a given format. Application programs can generate data streams
 * destined for a presentation service, archive library, presentation device or
 * another application program. The strategic presentation data stream
 * architectures used is Mixed Object Document Content Architecture (MO:DCA).
 *
 * The MO:DCA architecture defines the data stream used by applications to
 * describe documents and object envelopes for interchange with other
 * applications and application services. Documents defined in the MO:DCA format
 * may be archived in a database, then later retrieved, viewed, annotated and
 * printed in local or distributed systems environments. Presentation fidelity
 * is accommodated by including resource objects in the documents that reference
 * them.
 */
@Slf4j
public class DataStream {

    /** Boolean completion indicator */
    private boolean complete = false;

    /** The AFP document object */
    private Document document = null;

    /** The current page group object */
    private PageGroup currentPageGroup = null;

    /** The current page object */
    private PageObject currentPageObject = null;

    /** The current overlay object */
    private Overlay currentOverlay = null;

    /** The current page */
    private AbstractPageObject currentPage = null;

    /** Sequence number for TLE's. */
    private int tleSequence = 0;

    /** The MO:DCA interchange set in use (default to MO:DCA-P IS/2 set) */
    private InterchangeSet interchangeSet = InterchangeSet
            .valueOf(InterchangeSet.MODCA_PRESENTATION_INTERCHANGE_SET_2);

    private final Factory factory;

    private OutputStream outputStream;

    /** the afp painting state */
    private final AFPPaintingState paintingState;

    /**
     * Default constructor for the AFPDocumentStream.
     *
     * @param factory
     *            the resource factory
     * @param paintingState
     *            the AFP painting state
     * @param outputStream
     *            the outputstream to write to
     */
    public DataStream(final Factory factory,
            final AFPPaintingState paintingState,
            final OutputStream outputStream) {
        this.paintingState = paintingState;
        this.factory = factory;
        this.outputStream = outputStream;
    }

    /**
     * Returns the outputstream
     *
     * @return the outputstream
     */
    public OutputStream getOutputStream() {
        return this.outputStream;
    }

    /**
     * Returns the document object
     *
     * @return the document object
     */
    private Document getDocument() {
        return this.document;
    }

    /**
     * Returns the current page
     *
     * @return the current page
     */
    public AbstractPageObject getCurrentPage() {
        return this.currentPage;
    }

    /**
     * The document is started by invoking this method which creates an instance
     * of the AFP Document object.
     *
     * @param name
     *            the name of this document.
     */
    public void setDocumentName(final String name) {
        if (name != null) {
            getDocument().setFullyQualifiedName(
                    FullyQualifiedNameTriplet.TYPE_BEGIN_DOCUMENT_REF,
                    FullyQualifiedNameTriplet.FORMAT_CHARSTR, name);
        }
    }

    /**
     * Helper method to mark the end of the current document.
     *
     * @throws IOException
     *             thrown if an I/O exception of some sort has occurred
     */
    public void endDocument() throws IOException {
        if (this.complete) {
            final String msg = "Invalid state - document already ended.";
            log.warn("endDocument():: " + msg);
            throw new IllegalStateException(msg);
        }

        if (this.currentPageObject != null) {
            // End the current page if necessary
            endPage();
        }

        if (this.currentPageGroup != null) {
            // End the current page group if necessary
            endPageGroup();
        }

        // Write out document
        if (this.document != null) {
            this.document.endDocument();
            this.document.writeToStream(this.outputStream);
        }

        this.outputStream.flush();

        this.complete = true;

        this.document = null;

        this.outputStream = null;
    }

    /**
     * Start a new page. When processing has finished on the current page, the
     * {@link #endPage()}method must be invoked to mark the page ending.
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
     */
    public void startPage(final int pageWidth, final int pageHeight,
            final int pageRotation, final int pageWidthRes,
            final int pageHeightRes) {
        this.currentPageObject = this.factory.createPage(pageWidth, pageHeight,
                pageRotation, pageWidthRes, pageHeightRes);
        this.currentPage = this.currentPageObject;
        this.currentOverlay = null;
    }

    /**
     * Start a new overlay. When processing has finished on the current overlay,
     * the {@link #endOverlay()}method must be invoked to mark the overlay
     * ending.
     *
     * @param x
     *            the x position of the overlay on the page
     * @param y
     *            the y position of the overlay on the page
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
     */
    public void startOverlay(final int x, final int y, final int width,
            final int height, final int widthRes, final int heightRes,
            final int overlayRotation) {
        this.currentOverlay = this.factory.createOverlay(width, height,
                widthRes, heightRes, overlayRotation);

        final String overlayName = this.currentOverlay.getName();
        this.currentPageObject.createIncludePageOverlay(overlayName, x, y, 0);
        this.currentPage = this.currentOverlay;
    }

    /**
     * Helper method to mark the end of the current overlay.
     *
     * @throws IOException
     *             thrown if an I/O exception of some sort has occurred
     */
    public void endOverlay() throws IOException {
        if (this.currentOverlay != null) {
            this.currentOverlay.endPage();
            this.currentOverlay = null;
            this.currentPage = this.currentPageObject;
        }
    }

    /**
     * Helper method to save the current page.
     *
     * @return current page object that was saved
     */
    public PageObject savePage() {
        final PageObject pageObject = this.currentPageObject;
        if (this.currentPageGroup != null) {
            this.currentPageGroup.addPage(this.currentPageObject);
        } else {
            this.document.addPage(this.currentPageObject);
        }
        this.currentPageObject = null;
        this.currentPage = null;
        return pageObject;
    }

    /**
     * Helper method to restore the current page.
     *
     * @param pageObject
     *            page object
     */
    public void restorePage(final PageObject pageObject) {
        this.currentPageObject = pageObject;
        this.currentPage = pageObject;
    }

    /**
     * Helper method to mark the end of the current page.
     *
     * @throws IOException
     *             thrown if an I/O exception of some sort has occurred
     */
    public void endPage() throws IOException {
        if (this.currentPageObject != null) {
            this.currentPageObject.endPage();
            if (this.currentPageGroup != null) {
                this.currentPageGroup.addPage(this.currentPageObject);
                this.currentPageGroup.writeToStream(this.outputStream);
            } else {
                this.document.addPage(this.currentPageObject);
                this.document.writeToStream(this.outputStream);
            }
            this.currentPageObject = null;
            this.currentPage = null;
        }
    }

    /**
     * Creates the given page fonts in the current page
     *
     * @param pageFonts
     *            a collection of AFP font attributes
     */
    public void addFontsToCurrentPage(final Map pageFonts) {
        final Iterator iter = pageFonts.values().iterator();
        while (iter.hasNext()) {
            final AFPFontAttributes afpFontAttributes = (AFPFontAttributes) iter
                    .next();
            createFont(afpFontAttributes.getFontReference(),
                    afpFontAttributes.getFont(),
                    afpFontAttributes.getPointSize());
        }
    }

    /**
     * Helper method to create a map coded font object on the current page, this
     * method delegates the construction of the map coded font object to the
     * active environment group on the current page.
     *
     * @param fontReference
     *            the font number used as the resource identifier
     * @param font
     *            the font
     * @param size
     *            the point size of the font
     */
    public void createFont(final int fontReference, final AFPFont font,
            final int size) {
        this.currentPage.createFont(fontReference, font, size);
    }

    /**
     * Returns a point on the current page
     *
     * @param x
     *            the X-coordinate
     * @param y
     *            the Y-coordinate
     * @return a point on the current page
     */
    private Point getPoint(final int x, final int y) {
        return this.paintingState.getPoint(x, y);
    }

    /**
     * Helper method to create text on the current page, this method delegates
     * to the current presentation text object in order to construct the text.
     *
     * @param textDataInfo
     *            the afp text data
     * @param letterSpacing
     *            letter spacing to draw text with
     * @param wordSpacing
     *            word Spacing to draw text with
     * @param font
     *            is the font to draw text with
     * @param charSet
     *            is the AFP Character Set to use with the text
     * @throws UnsupportedEncodingException
     *             thrown if character encoding is not supported
     */
    public void createText(final AFPTextDataInfo textDataInfo,
            final int letterSpacing, final int wordSpacing, final Font font,
            final CharacterSet charSet) throws UnsupportedEncodingException {
        final int rotation = this.paintingState.getRotation();
        if (rotation != 0) {
            textDataInfo.setRotation(rotation);
            final Point p = getPoint(textDataInfo.getX(), textDataInfo.getY());
            textDataInfo.setX(p.x);
            textDataInfo.setY(p.y);
        }
        // use PtocaProducer to create PTX records
        final PtocaProducer producer = new PtocaProducer() {

            @Override
            public void produce(final PtocaBuilder builder) throws IOException {
                builder.setTextOrientation(textDataInfo.getRotation());
                builder.absoluteMoveBaseline(textDataInfo.getY());
                builder.absoluteMoveInline(textDataInfo.getX());

                builder.setExtendedTextColor(textDataInfo.getColor());
                builder.setCodedFont((byte) textDataInfo.getFontReference());

                final int l = textDataInfo.getString().length();
                final StringBuilder sb = new StringBuilder();

                int interCharacterAdjustment = 0;
                final AFPUnitConverter unitConv = DataStream.this.paintingState
                        .getUnitConverter();
                if (letterSpacing != 0) {
                    interCharacterAdjustment = Math.round(unitConv
                            .mpt2units(letterSpacing));
                }
                builder.setInterCharacterAdjustment(interCharacterAdjustment);

                final int spaceWidth = font.getCharWidth(CharUtilities.SPACE);
                final int spacing = spaceWidth + letterSpacing;
                final int fixedSpaceCharacterIncrement = Math.round(unitConv
                        .mpt2units(spacing));
                int varSpaceCharacterIncrement = fixedSpaceCharacterIncrement;
                if (wordSpacing != 0) {
                    varSpaceCharacterIncrement = Math
                            .round(unitConv.mpt2units(spaceWidth + wordSpacing
                                    + letterSpacing));
                }
                builder.setVariableSpaceCharacterIncrement(varSpaceCharacterIncrement);

                boolean fixedSpaceMode = false;

                for (int i = 0; i < l; ++i) {
                    final char orgChar = textDataInfo.getString().charAt(i);
                    float glyphAdjust = 0;
                    if (CharUtilities.isFixedWidthSpace(orgChar)) {
                        flushText(builder, sb, charSet);
                        builder.setVariableSpaceCharacterIncrement(fixedSpaceCharacterIncrement);
                        fixedSpaceMode = true;
                        sb.append(CharUtilities.SPACE);
                        final int charWidth = font.getCharWidth(orgChar);
                        glyphAdjust += charWidth - spaceWidth;
                    } else {
                        if (fixedSpaceMode) {
                            flushText(builder, sb, charSet);
                            builder.setVariableSpaceCharacterIncrement(varSpaceCharacterIncrement);
                            fixedSpaceMode = false;
                        }
                        char ch;
                        if (orgChar == CharUtilities.NBSPACE) {
                            ch = ' '; // converted to normal space to allow word
                            // spacing
                        } else {
                            ch = orgChar;
                        }
                        sb.append(ch);
                    }

                    if (glyphAdjust != 0) {
                        flushText(builder, sb, charSet);
                        final int increment = Math.round(unitConv
                                .mpt2units(glyphAdjust));
                        builder.relativeMoveInline(increment);
                    }
                }
                flushText(builder, sb, charSet);
            }

            private void flushText(final PtocaBuilder builder,
                    final StringBuilder sb, final CharacterSet charSet)
                            throws IOException {
                if (sb.length() > 0) {
                    builder.addTransparentData(charSet.encodeChars(sb));
                    sb.setLength(0);
                }
            }

        };

        this.currentPage.createText(producer);
    }

    /**
     * Method to create a line on the current page.
     *
     * @param lineDataInfo
     *            the line data information.
     */
    public void createLine(final AFPLineDataInfo lineDataInfo) {
        this.currentPage.createLine(lineDataInfo);
    }

    /**
     * This method will create shading on the page using the specified
     * coordinates (the shading contrast is controlled via the red, green, blue
     * parameters, by converting this to grey scale).
     *
     * @param x
     *            the x coordinate of the shading
     * @param y
     *            the y coordinate of the shading
     * @param w
     *            the width of the shaded area
     * @param h
     *            the height of the shaded area
     * @param col
     *            the shading color
     */
    public void createShading(final int x, final int y, final int w,
            final int h, final Color col) {
        this.currentPageObject.createShading(x, y, w, h, col.getRed(),
                col.getGreen(), col.getBlue());
    }

    /**
     * Helper method which allows creation of the MPO object, via the AEG. And
     * the IPO via the Page. (See actual object for descriptions.)
     *
     * @param name
     *            the name of the static overlay
     * @param x
     *            x-coordinate
     * @param y
     *            y-coordinate
     */
    public void createIncludePageOverlay(final String name, final int x,
            final int y) {
        this.currentPageObject.createIncludePageOverlay(name, x, y,
                this.paintingState.getRotation());
        this.currentPageObject.getActiveEnvironmentGroup().createOverlay(name);
    }

    /**
     * Helper method which allows creation of the IMM object.
     *
     * @param name
     *            the name of the medium map
     */
    public void createInvokeMediumMap(final String name) {
        this.currentPageGroup.createInvokeMediumMap(name);
    }

    /**
     * Creates an IncludePageSegment on the current page.
     *
     * @param name
     *            the name of the include page segment
     * @param x
     *            the x coordinate for the overlay
     * @param y
     *            the y coordinate for the overlay
     * @param width
     *            the width of the image
     * @param height
     *            the height of the image
     */
    public void createIncludePageSegment(final String name, final int x,
            final int y, final int width, final int height) {
        int xOrigin;
        int yOrigin;
        final int orientation = this.paintingState.getRotation();
        switch (orientation) {
        case 90:
            xOrigin = x - height;
            yOrigin = y;
            break;
        case 180:
            xOrigin = x - width;
            yOrigin = y - height;
            break;
        case 270:
            xOrigin = x;
            yOrigin = y - width;
            break;
        default:
            xOrigin = x;
            yOrigin = y;
            break;
        }
        final boolean createHardPageSegments = true;
        this.currentPage.createIncludePageSegment(name, xOrigin, yOrigin,
                createHardPageSegments);
    }

    /**
     * Creates a TagLogicalElement on the current page.
     *
     * @param attributes
     *            the array of key value pairs.
     */
    public void createPageTagLogicalElement(
            final TagLogicalElementBean[] attributes) {
        for (final TagLogicalElementBean attribute : attributes) {
            final String name = attribute.getKey();
            final String value = attribute.getValue();
            this.currentPage.createTagLogicalElement(name, value,
                    this.tleSequence++);
        }
    }

    /**
     * Creates a TagLogicalElement on the current page group.
     *
     * @param attributes
     *            the array of key value pairs.
     */
    public void createPageGroupTagLogicalElement(
            final TagLogicalElementBean[] attributes) {
        for (final TagLogicalElementBean attribute : attributes) {
            final String name = attribute.getKey();
            final String value = attribute.getValue();
            this.currentPageGroup.createTagLogicalElement(name, value);
        }
    }

    /**
     * Creates a TagLogicalElement on the current page or page group
     *
     * @param name
     *            The tag name
     * @param value
     *            The tag value
     */
    public void createTagLogicalElement(final String name, final String value) {
        if (this.currentPage != null) {
            this.currentPage.createTagLogicalElement(name, value,
                    this.tleSequence++);
        } else {
            this.currentPageGroup.createTagLogicalElement(name, value);
        }
    }

    /**
     * Creates a NoOperation item
     *
     * @param content
     *            byte data
     */
    public void createNoOperation(final String content) {
        this.currentPage.createNoOperation(content);
    }

    /**
     * Returns the current page group
     *
     * @return the current page group
     */
    public PageGroup getCurrentPageGroup() {
        return this.currentPageGroup;
    }

    /**
     * Start a new document.
     *
     * @throws IOException
     *             thrown if an I/O exception of some sort has occurred
     */
    public void startDocument() throws IOException {
        this.document = this.factory.createDocument();
        this.document.writeToStream(this.outputStream);
    }

    /**
     * Start a new page group. When processing has finished on the current page
     * group the {@link #endPageGroup()}method must be invoked to mark the page
     * group ending.
     *
     * @throws IOException
     *             thrown if an I/O exception of some sort has occurred
     */
    public void startPageGroup() throws IOException {
        endPageGroup();
        this.currentPageGroup = this.factory.createPageGroup(this.tleSequence);
    }

    /**
     * Helper method to mark the end of the page group.
     *
     * @throws IOException
     *             thrown if an I/O exception of some sort has occurred
     */
    public void endPageGroup() throws IOException {
        if (this.currentPageGroup != null) {
            this.currentPageGroup.endPageGroup();
            this.tleSequence = this.currentPageGroup.getTleSequence();
            this.document.addPageGroup(this.currentPageGroup);
            this.document.writeToStream(this.outputStream);
            this.currentPageGroup = null;
        }
    }

    /**
     * Sets the MO:DCA interchange set to use
     *
     * @param interchangeSet
     *            the MO:DCA interchange set
     */
    public void setInterchangeSet(final InterchangeSet interchangeSet) {
        this.interchangeSet = interchangeSet;
    }

    /**
     * Returns the MO:DCA interchange set in use
     *
     * @return the MO:DCA interchange set in use
     */
    public InterchangeSet getInterchangeSet() {
        return this.interchangeSet;
    }

    /**
     * Returns the resource group for a given resource info
     *
     * @param level
     *            a resource level
     * @return a resource group for the given resource info
     */
    public ResourceGroup getResourceGroup(final AFPResourceLevel level) {
        ResourceGroup resourceGroup = null;
        if (level.isDocument()) {
            resourceGroup = this.document.getResourceGroup();
        } else if (level.isPageGroup()) {
            resourceGroup = this.currentPageGroup.getResourceGroup();
        } else if (level.isPage()) {
            resourceGroup = this.currentPageObject.getResourceGroup();
        }
        return resourceGroup;
    }

}
