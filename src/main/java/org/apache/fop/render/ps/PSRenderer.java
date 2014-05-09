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

/* $Id: PSRenderer.java 932497 2010-04-09 16:34:29Z vhennebert $ */

package org.apache.fop.render.ps;

// Java
import java.awt.Color;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.geom.AffineTransform;
import java.awt.geom.Rectangle2D;
import java.awt.image.RenderedImage;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.xml.transform.Source;

import lombok.extern.slf4j.Slf4j;

import org.apache.commons.io.IOUtils;
import org.apache.fop.ResourceEventProducer;
import org.apache.fop.apps.FOUserAgent;
import org.apache.fop.area.Area;
import org.apache.fop.area.BlockViewport;
import org.apache.fop.area.CTM;
import org.apache.fop.area.OffDocumentExtensionAttachment;
import org.apache.fop.area.OffDocumentItem;
import org.apache.fop.area.PageViewport;
import org.apache.fop.area.RegionViewport;
import org.apache.fop.area.Trait;
import org.apache.fop.area.inline.AbstractTextArea;
import org.apache.fop.area.inline.Image;
import org.apache.fop.area.inline.InlineParent;
import org.apache.fop.area.inline.Leader;
import org.apache.fop.area.inline.SpaceArea;
import org.apache.fop.area.inline.TextArea;
import org.apache.fop.area.inline.WordArea;
import org.apache.fop.datatypes.URISpecification;
import org.apache.fop.fo.extensions.ExtensionAttachment;
import org.apache.fop.fonts.Font;
import org.apache.fop.fonts.FontMetrics;
import org.apache.fop.fonts.LazyFont;
import org.apache.fop.fonts.SingleByteFont;
import org.apache.fop.fonts.Typeface;
import org.apache.fop.render.AbstractPathOrientedRenderer;
import org.apache.fop.render.Graphics2DAdapter;
import org.apache.fop.render.ImageAdapter;
import org.apache.fop.render.ImageHandler;
import org.apache.fop.render.ImageHandlerRegistry;
import org.apache.fop.render.RendererContext;
import org.apache.fop.render.RendererEventProducer;
import org.apache.fop.render.ps.extensions.PSCommentAfter;
import org.apache.fop.render.ps.extensions.PSCommentBefore;
import org.apache.fop.render.ps.extensions.PSExtensionAttachment;
import org.apache.fop.render.ps.extensions.PSSetPageDevice;
import org.apache.fop.render.ps.extensions.PSSetupCode;
import org.apache.fop.traits.RuleStyle;
import org.apache.fop.util.CharUtilities;
import org.apache.xmlgraphics.image.loader.ImageException;
import org.apache.xmlgraphics.image.loader.ImageFlavor;
import org.apache.xmlgraphics.image.loader.ImageInfo;
import org.apache.xmlgraphics.image.loader.ImageManager;
import org.apache.xmlgraphics.image.loader.ImageSessionContext;
import org.apache.xmlgraphics.image.loader.util.ImageUtil;
import org.apache.xmlgraphics.ps.DSCConstants;
import org.apache.xmlgraphics.ps.PSDictionary;
import org.apache.xmlgraphics.ps.PSDictionaryFormatException;
import org.apache.xmlgraphics.ps.PSGenerator;
import org.apache.xmlgraphics.ps.PSPageDeviceDictionary;
import org.apache.xmlgraphics.ps.PSProcSets;
import org.apache.xmlgraphics.ps.PSResource;
import org.apache.xmlgraphics.ps.PSState;
import org.apache.xmlgraphics.ps.dsc.DSCException;
import org.apache.xmlgraphics.ps.dsc.ResourceTracker;
import org.apache.xmlgraphics.ps.dsc.events.DSCCommentBoundingBox;
import org.apache.xmlgraphics.ps.dsc.events.DSCCommentHiResBoundingBox;
import org.apache.xmlgraphics.util.QName;

/**
 * Renderer that renders to PostScript. <br>
 * This class currently generates PostScript Level 2 code. The only exception is
 * the FlateEncode filter which is a Level 3 feature. The filters in use are
 * hardcoded at the moment. <br>
 * This class follows the Document Structuring Conventions (DSC) version 3.0. If
 * anyone modifies this renderer please make sure to also follow the DSC to make
 * it simpler to programmatically modify the generated Postscript files (ex.
 * extract pages etc.). <br>
 * This renderer inserts FOP-specific comments into the PostScript stream which
 * may help certain users to do certain types of post-processing of the output.
 * These comments all start with "%FOP".
 *
 * @author <a href="mailto:fop-dev@xmlgraphics.apache.org">Apache FOP
 *         Development Team</a>
 * @version $Id: PSRenderer.java 932497 2010-04-09 16:34:29Z vhennebert $
 */
@Slf4j
public class PSRenderer extends AbstractPathOrientedRenderer implements
ImageAdapter, PSSupportedFlavors, PSConfigurationConstants {

    /** The MIME type for PostScript */
    public static final String MIME_TYPE = "application/postscript";

    /** The application producing the PostScript */
    private int currentPageNumber = 0;

    /** the OutputStream the PS file is written to */
    private OutputStream outputStream;
    /** the temporary file in case of two-pass processing */
    private File tempFile;

    /** The PostScript generator used to output the PostScript */
    protected PSGenerator gen;
    private boolean ioTrouble = false;

    private boolean inTextMode = false;

    /**
     * Used to temporarily store PSSetupCode instance until they can be written.
     */
    private List<PSSetupCode> setupCodeList;

    /**
     * This is a map of PSResource instances of all fonts defined (key: font
     * key)
     */
    private Map<String, PSResource> fontResources;
    /** This is a map of PSResource instances of all forms (key: uri) */
    private Map<String, PSResource> formResources;

    /** encapsulation of dictionary used in setpagedevice instruction **/
    private PSPageDeviceDictionary pageDeviceDictionary;

    /**
     * Utility class which enables all sorts of features that are not directly
     * connected to the normal rendering process.
     */
    protected PSRenderingUtil psUtil;
    private PSBorderPainter borderPainter;

    /** Is used to determine the document's bounding box */
    private Rectangle2D documentBoundingBox;

    /** This is a collection holding all document header comments */
    private Collection<ExtensionAttachment> headerComments;

    /** This is a collection holding all document footer comments */
    private Collection<ExtensionAttachment> footerComments;

    /** {@inheritDoc} */
    @Override
    public void setUserAgent(final FOUserAgent agent) {
        super.setUserAgent(agent);
        this.psUtil = new PSRenderingUtil(getUserAgent());
    }

    PSRenderingUtil getPSUtil() {
        return this.psUtil;
    }

    /**
     * Sets the landscape mode for this renderer.
     *
     * @param value
     *            false will normally generate a "pseudo-portrait" page, true
     *            will rotate a "wider-than-long" page by 90 degrees.
     */
    public void setAutoRotateLandscape(final boolean value) {
        getPSUtil().setAutoRotateLandscape(value);
    }

    /** @return true if the renderer is configured to rotate landscape pages */
    public boolean isAutoRotateLandscape() {
        return getPSUtil().isAutoRotateLandscape();
    }

    /**
     * Sets the PostScript language level that the renderer should produce.
     *
     * @param level
     *            the language level (currently allowed: 2 or 3)
     */
    public void setLanguageLevel(final int level) {
        getPSUtil().setLanguageLevel(level);
    }

    /**
     * Return the PostScript language level that the renderer produces.
     *
     * @return the language level
     */
    public int getLanguageLevel() {
        return getPSUtil().getLanguageLevel();
    }

    /**
     * Sets the resource optimization mode. If set to true, the renderer does
     * two passes to only embed the necessary resources in the PostScript file.
     * This is slower, but produces smaller files.
     *
     * @param value
     *            true to enable the resource optimization
     */
    public void setOptimizeResources(final boolean value) {
        getPSUtil().setOptimizeResources(value);
    }

    /**
     * @return true if the renderer does two passes to optimize PostScript
     *         resources
     */
    public boolean isOptimizeResources() {
        return getPSUtil().isOptimizeResources();
    }

    /** {@inheritDoc} */
    @Override
    public Graphics2DAdapter getGraphics2DAdapter() {
        return new PSGraphics2DAdapter(this);
    }

    /** {@inheritDoc} */
    @Override
    public ImageAdapter getImageAdapter() {
        return this;
    }

    /**
     * Write out a command
     *
     * @param cmd
     *            PostScript command
     */
    protected void writeln(final String cmd) {
        try {
            this.gen.writeln(cmd);
        } catch (final IOException ioe) {
            handleIOTrouble(ioe);
        }
    }

    /**
     * Central exception handler for I/O exceptions.
     *
     * @param ioe
     *            IOException to handle
     */
    protected void handleIOTrouble(final IOException ioe) {
        if (!this.ioTrouble) {
            final RendererEventProducer eventProducer = RendererEventProducer.Provider
                    .get(getUserAgent().getEventBroadcaster());
            eventProducer.ioError(this, ioe);
            this.ioTrouble = true;
        }
    }

    /**
     * Write out a comment
     *
     * @param comment
     *            Comment to write
     */
    protected void comment(final String comment) {
        try {
            if (comment.startsWith("%")) {
                this.gen.commentln(comment);
                writeln(comment);
            } else {
                this.gen.commentln("%" + comment);
            }
        } catch (final IOException ioe) {
            handleIOTrouble(ioe);
        }
    }

    /**
     * Make sure the cursor is in the right place.
     */
    protected void movetoCurrPosition() {
        moveTo(this.currentIPPosition, this.currentBPPosition);
    }

    /** {@inheritDoc} */
    @Override
    protected void clip() {
        writeln("clip newpath");
    }

    /** {@inheritDoc} */
    @Override
    protected void clipRect(final float x, final float y, final float width,
            final float height) {
        try {
            this.gen.defineRect(x, y, width, height);
            clip();
        } catch (final IOException ioe) {
            handleIOTrouble(ioe);
        }
    }

    /** {@inheritDoc} */
    @Override
    protected void moveTo(final float x, final float y) {
        writeln(this.gen.formatDouble(x) + " " + this.gen.formatDouble(y)
                + " M");
    }

    /**
     * Moves the current point by (x, y) relative to the current position,
     * omitting any connecting line segment.
     *
     * @param x
     *            x coordinate
     * @param y
     *            y coordinate
     */
    protected void rmoveTo(final float x, final float y) {
        writeln(this.gen.formatDouble(x) + " " + this.gen.formatDouble(y)
                + " RM");
    }

    /** {@inheritDoc} */
    @Override
    protected void lineTo(final float x, final float y) {
        writeln(this.gen.formatDouble(x) + " " + this.gen.formatDouble(y)
                + " lineto");
    }

    /** {@inheritDoc} */
    @Override
    protected void closePath() {
        writeln("cp");
    }

    /** {@inheritDoc} */
    @Override
    protected void fillRect(final float x, final float y, final float width,
            final float height) {
        if (width != 0 && height != 0) {
            try {
                this.gen.defineRect(x, y, width, height);
                this.gen.writeln("fill");
            } catch (final IOException ioe) {
                handleIOTrouble(ioe);
            }
        }
    }

    /** {@inheritDoc} */
    @Override
    protected void updateColor(final Color col, final boolean fill) {
        try {
            useColor(col);
        } catch (final IOException ioe) {
            handleIOTrouble(ioe);
        }
    }

    /** {@inheritDoc} */
    @Override
    protected void drawImage(String uri, final Rectangle2D pos,
            final Map<QName, String> foreignAttributes) {
        endTextObject();
        final int x = this.currentIPPosition + (int) Math.round(pos.getX());
        final int y = this.currentBPPosition + (int) Math.round(pos.getY());
        uri = URISpecification.getURL(uri);
        if (log.isDebugEnabled()) {
            log.debug("Handling image: " + uri);
        }
        final int width = (int) pos.getWidth();
        final int height = (int) pos.getHeight();
        final Rectangle targetRect = new Rectangle(x, y, width, height);

        final ImageManager manager = getUserAgent().getFactory()
                .getImageManager();
        ImageInfo info = null;
        try {
            final ImageSessionContext sessionContext = getUserAgent()
                    .getImageSessionContext();
            info = manager.getImageInfo(uri, sessionContext);

            final PSRenderingContext renderingContext = new PSRenderingContext(
                    getUserAgent(), this.gen, getFontInfo());

            if (!isOptimizeResources()
                    || PSImageUtils.isImageInlined(info, renderingContext)) {
                if (log.isDebugEnabled()) {
                    log.debug("Image " + info + " is inlined");
                }

                // Determine supported flavors
                ImageFlavor[] flavors;
                final ImageHandlerRegistry imageHandlerRegistry = this.userAgent
                        .getFactory().getImageHandlerRegistry();
                flavors = imageHandlerRegistry
                        .getSupportedFlavors(renderingContext);

                // Only now fully load/prepare the image
                final Map<Object, Object> hints = ImageUtil
                        .getDefaultHints(sessionContext);
                final org.apache.xmlgraphics.image.loader.Image img = manager
                        .getImage(info, flavors, hints, sessionContext);

                // Get handler for image
                final ImageHandler basicHandler = imageHandlerRegistry
                        .getHandler(renderingContext, img);

                // ...and embed as inline image
                basicHandler.handleImage(renderingContext, img, targetRect);
            } else {
                log.debug("Image {} is embedded as a form later", info);
                // Don't load image at this time, just put a form placeholder in
                // the stream
                final PSResource form = getFormForImage(info.getOriginalURI());
                PSImageUtils.drawForm(form, info, targetRect, this.gen);
            }

        } catch (final ImageException ie) {
            final ResourceEventProducer eventProducer = ResourceEventProducer.Provider
                    .get(getUserAgent().getEventBroadcaster());
            eventProducer.imageError(this,
                    info != null ? info.toString() : uri, ie, null);
        } catch (final FileNotFoundException fe) {
            final ResourceEventProducer eventProducer = ResourceEventProducer.Provider
                    .get(getUserAgent().getEventBroadcaster());
            eventProducer.imageNotFound(this, info != null ? info.toString()
                    : uri, fe, null);
        } catch (final IOException ioe) {
            final ResourceEventProducer eventProducer = ResourceEventProducer.Provider
                    .get(getUserAgent().getEventBroadcaster());
            eventProducer.imageIOError(this, info != null ? info.toString()
                    : uri, ioe, null);
        }
    }

    /**
     * Returns a PSResource instance representing a image as a PostScript form.
     *
     * @param uri
     *            the image URI
     * @return a PSResource instance
     */
    protected PSResource getFormForImage(final String uri) {
        if (uri == null || "".equals(uri)) {
            throw new IllegalArgumentException("uri must not be empty or null");
        }
        if (this.formResources == null) {
            this.formResources = new HashMap<>();
        }
        PSResource form = this.formResources.get(uri);
        if (form == null) {
            form = new PSImageFormResource(this.formResources.size() + 1, uri);
            this.formResources.put(uri, form);
        }
        return form;
    }

    /** {@inheritDoc} */
    @Override
    public void paintImage(final RenderedImage image,
            final RendererContext context, int x, int y, final int width,
            final int height) throws IOException {
        final float fx = x / 1000f;
        x += this.currentIPPosition / 1000f;
        final float fy = y / 1000f;
        y += this.currentBPPosition / 1000f;
        final float fw = width / 1000f;
        final float fh = height / 1000f;
        org.apache.xmlgraphics.ps.PSImageUtils.renderBitmapImage(image, fx, fy,
                fw, fh, this.gen);
    }

    /** Saves the graphics state of the rendering engine. */
    @Override
    public void saveGraphicsState() {
        endTextObject();
        try {
            // delegate
            this.gen.saveGraphicsState();
        } catch (final IOException ioe) {
            handleIOTrouble(ioe);
        }
    }

    /** Restores the last graphics state of the rendering engine. */
    @Override
    public void restoreGraphicsState() {
        try {
            endTextObject();
            // delegate
            this.gen.restoreGraphicsState();
        } catch (final IOException ioe) {
            handleIOTrouble(ioe);
        }
    }

    /**
     * Concats the transformation matrix.
     *
     * @param a
     *            A part
     * @param b
     *            B part
     * @param c
     *            C part
     * @param d
     *            D part
     * @param e
     *            E part
     * @param f
     *            F part
     */
    protected void concatMatrix(final double a, final double b, final double c,
            final double d, final double e, final double f) {
        try {
            this.gen.concatMatrix(a, b, c, d, e, f);
        } catch (final IOException ioe) {
            handleIOTrouble(ioe);
        }
    }

    /**
     * Concats the transformations matrix.
     *
     * @param matrix
     *            Matrix to use
     */
    protected void concatMatrix(final double[] matrix) {
        try {
            this.gen.concatMatrix(matrix);
        } catch (final IOException ioe) {
            handleIOTrouble(ioe);
        }
    }

    /** {@inheritDoc} */
    @Override
    protected void concatenateTransformationMatrix(final AffineTransform at) {
        try {
            this.gen.concatMatrix(at);
        } catch (final IOException ioe) {
            handleIOTrouble(ioe);
        }
    }

    private String getPostScriptNameForFontKey(String key) {
        final int pos = key.indexOf('_');
        String postFix = null;
        if (pos > 0) {
            postFix = key.substring(pos);
            key = key.substring(0, pos);
        }
        final Map<String, FontMetrics> fonts = this.fontInfo.getFonts();
        Typeface tf = (Typeface) fonts.get(key);
        if (tf instanceof LazyFont) {
            tf = ((LazyFont) tf).getRealFont();
        }
        if (tf == null) {
            throw new IllegalStateException("Font not available: " + key);
        }
        if (postFix == null) {
            return tf.getFontName();
        } else {
            return tf.getFontName() + postFix;
        }
    }

    /**
     * Returns the PSResource for the given font key.
     *
     * @param key
     *            the font key ("F*")
     * @return the matching PSResource
     */
    protected PSResource getPSResourceForFontKey(final String key) {
        PSResource res = null;
        if (this.fontResources != null) {
            res = this.fontResources.get(key);
        } else {
            this.fontResources = new HashMap<>();
        }
        if (res == null) {
            res = new PSResource(PSResource.TYPE_FONT,
                    getPostScriptNameForFontKey(key));
            this.fontResources.put(key, res);
        }
        return res;
    }

    /**
     * Changes the currently used font.
     *
     * @param key
     *            key of the font ("F*")
     * @param size
     *            font size
     */
    protected void useFont(final String key, final int size) {
        try {
            final PSResource res = getPSResourceForFontKey(key);
            this.gen.useFont("/" + res.getName(), size / 1000f);
            this.gen.getResourceTracker().notifyResourceUsageOnPage(res);
        } catch (final IOException ioe) {
            handleIOTrouble(ioe);
        }
    }

    private void useColor(final Color col) throws IOException {
        this.gen.useColor(col);
    }

    /** {@inheritDoc} */
    @Override
    protected void drawBackAndBorders(final Area area, final float startx,
            final float starty, final float width, final float height) {
        if (area.hasTrait(Trait.BACKGROUND)
                || area.hasTrait(Trait.BORDER_BEFORE)
                || area.hasTrait(Trait.BORDER_AFTER)
                || area.hasTrait(Trait.BORDER_START)
                || area.hasTrait(Trait.BORDER_END)) {
            comment("%FOPBeginBackgroundAndBorder: " + startx + " " + starty
                    + " " + width + " " + height);
            super.drawBackAndBorders(area, startx, starty, width, height);
            comment("%FOPEndBackgroundAndBorder");
        }
    }

    /** {@inheritDoc} */
    @Override
    protected void drawBorderLine(final float x1, final float y1,
            final float x2, final float y2, final boolean horz,
            final boolean startOrBefore, final int style, final Color col) {
        try {
            PSBorderPainter.drawBorderLine(this.gen, x1, y1, x2, y2, horz,
                    startOrBefore, style, col);
        } catch (final IOException ioe) {
            handleIOTrouble(ioe);
        }
    }

    /** {@inheritDoc} */
    @Override
    public void startRenderer(final OutputStream outputStream)
            throws IOException {
        log.debug("Rendering areas to PostScript...");

        this.outputStream = outputStream;
        OutputStream out;
        if (isOptimizeResources()) {
            this.tempFile = File.createTempFile("fop", null);
            out = new java.io.FileOutputStream(this.tempFile);
            out = new java.io.BufferedOutputStream(out);
        } else {
            out = this.outputStream;
        }

        // Setup for PostScript generation
        this.gen = new PSGenerator(out) {
            /** Need to subclass PSGenerator to have better URI resolution */
            @Override
            public Source resolveURI(final String uri) {
                return PSRenderer.this.userAgent.resolveURI(uri);
            }
        };
        this.gen.setPSLevel(getLanguageLevel());
        this.borderPainter = new PSBorderPainter(this.gen);
        this.currentPageNumber = 0;

        // Initial default page device dictionary settings
        this.pageDeviceDictionary = new PSPageDeviceDictionary();
        this.pageDeviceDictionary.setFlushOnRetrieval(!getPSUtil()
                .isDSCComplianceEnabled());
        this.pageDeviceDictionary.put("/ImagingBBox", "null");
    }

    private void writeHeader() throws IOException {
        // PostScript Header
        writeln(DSCConstants.PS_ADOBE_30);
        this.gen.writeDSCComment(DSCConstants.CREATOR,
                new String[] { this.userAgent.getProducer() });
        this.gen.writeDSCComment(DSCConstants.CREATION_DATE,
                new Object[] { new Date() });
        this.gen.writeDSCComment(DSCConstants.LANGUAGE_LEVEL,
                this.gen.getPSLevel());
        this.gen.writeDSCComment(DSCConstants.PAGES,
                new Object[] { DSCConstants.ATEND });
        this.gen.writeDSCComment(DSCConstants.BBOX, DSCConstants.ATEND);
        this.gen.writeDSCComment(DSCConstants.HIRES_BBOX, DSCConstants.ATEND);
        this.documentBoundingBox = new Rectangle2D.Double();
        this.gen.writeDSCComment(DSCConstants.DOCUMENT_SUPPLIED_RESOURCES,
                new Object[] { DSCConstants.ATEND });
        if (this.headerComments != null) {
            for (final Object element : this.headerComments) {
                final PSExtensionAttachment comment = (PSExtensionAttachment) element;
                this.gen.writeln("%" + comment.getContent());
            }
        }
        this.gen.writeDSCComment(DSCConstants.END_COMMENTS);

        // Defaults
        this.gen.writeDSCComment(DSCConstants.BEGIN_DEFAULTS);
        this.gen.writeDSCComment(DSCConstants.END_DEFAULTS);

        // Prolog and Setup written right before the first page-sequence, see
        // startPageSequence()
        // Do this only once, as soon as we have all the content for the Setup
        // section!
        // Prolog
        this.gen.writeDSCComment(DSCConstants.BEGIN_PROLOG);
        PSProcSets.writeStdProcSet(this.gen);
        PSProcSets.writeEPSProcSet(this.gen);
        this.gen.writeDSCComment(DSCConstants.END_PROLOG);

        // Setup
        this.gen.writeDSCComment(DSCConstants.BEGIN_SETUP);
        PSRenderingUtil.writeSetupCodeList(this.gen, this.setupCodeList,
                "SetupCode");
        if (!isOptimizeResources()) {
            this.fontResources = PSFontUtils.writeFontDict(this.gen,
                    this.fontInfo);
        } else {
            this.gen.commentln("%FOPFontSetup"); // Place-holder, will be
            // replaced in the second pass
        }
        this.gen.writeDSCComment(DSCConstants.END_SETUP);
    }

    /** {@inheritDoc} */
    @Override
    public void stopRenderer() throws IOException {
        // Write trailer
        this.gen.writeDSCComment(DSCConstants.TRAILER);
        if (this.footerComments != null) {
            for (final Object element : this.footerComments) {
                final PSExtensionAttachment comment = (PSExtensionAttachment) element;
                this.gen.commentln("%" + comment.getContent());
            }
            this.footerComments.clear();
        }
        this.gen.writeDSCComment(DSCConstants.PAGES, this.currentPageNumber);
        new DSCCommentBoundingBox(this.documentBoundingBox).generate(this.gen);
        new DSCCommentHiResBoundingBox(this.documentBoundingBox)
        .generate(this.gen);
        this.gen.getResourceTracker().writeResources(false, this.gen);
        this.gen.writeDSCComment(DSCConstants.EOF);
        this.gen.flush();
        log.debug("Rendering to PostScript complete.");
        if (isOptimizeResources()) {
            IOUtils.closeQuietly(this.gen.getOutputStream());
            rewritePostScriptFile();
        }
        if (this.footerComments != null) {
            this.headerComments.clear();
        }
        if (this.pageDeviceDictionary != null) {
            this.pageDeviceDictionary.clear();
        }
        this.borderPainter = null;
        this.gen = null;
    }

    /**
     * Used for two-pass production. This will rewrite the PostScript file from
     * the temporary file while adding all needed resources.
     *
     * @throws IOException
     *             In case of an I/O error.
     */
    private void rewritePostScriptFile() throws IOException {
        log.debug("Processing PostScript resources...");
        final long startTime = System.currentTimeMillis();
        final ResourceTracker resTracker = this.gen.getResourceTracker();
        InputStream in = new java.io.FileInputStream(this.tempFile);
        in = new java.io.BufferedInputStream(in);
        try {
            try {
                final ResourceHandler handler = new ResourceHandler(
                        this.userAgent, this.fontInfo, resTracker,
                        this.formResources);
                handler.process(in, this.outputStream, this.currentPageNumber,
                        this.documentBoundingBox);
                this.outputStream.flush();
            } catch (final DSCException e) {
                throw new RuntimeException(e.getMessage());
            }
        } finally {
            IOUtils.closeQuietly(in);
            if (!this.tempFile.delete()) {
                this.tempFile.deleteOnExit();
                log.warn("Could not delete temporary file: " + this.tempFile);
            }
        }
        if (log.isDebugEnabled()) {
            final long duration = System.currentTimeMillis() - startTime;
            log.debug("Resource Processing complete in " + duration + " ms.");
        }
    }

    /** {@inheritDoc} */
    @Override
    public void processOffDocumentItem(final OffDocumentItem oDI) {
        if (log.isDebugEnabled()) {
            log.debug("Handling OffDocumentItem: " + oDI.getName());
        }
        if (oDI instanceof OffDocumentExtensionAttachment) {
            final ExtensionAttachment attachment = ((OffDocumentExtensionAttachment) oDI)
                    .getAttachment();
            if (attachment != null) {
                if (PSExtensionAttachment.CATEGORY.equals(attachment
                        .getCategory())) {
                    if (attachment instanceof PSSetupCode) {
                        if (this.setupCodeList == null) {
                            this.setupCodeList = new ArrayList<>();
                        }
                        if (!this.setupCodeList.contains(attachment)) {
                            this.setupCodeList.add((PSSetupCode) attachment);
                        }
                    } else if (attachment instanceof PSSetPageDevice) {
                        /**
                         * Extract all PSSetPageDevice instances from the
                         * attachment list on the s-p-m and add all dictionary
                         * entries to our internal representation of the the
                         * page device dictionary.
                         */
                        final PSSetPageDevice setPageDevice = (PSSetPageDevice) attachment;
                        final String content = setPageDevice.getContent();
                        if (content != null) {
                            try {
                                this.pageDeviceDictionary.putAll(PSDictionary
                                        .valueOf(content));
                            } catch (final PSDictionaryFormatException e) {
                                final PSEventProducer eventProducer = PSEventProducer.Provider
                                        .get(getUserAgent()
                                                .getEventBroadcaster());
                                eventProducer.postscriptDictionaryParseError(
                                        this, content, e);
                            }
                        }
                    } else if (attachment instanceof PSCommentBefore) {
                        if (this.headerComments == null) {
                            this.headerComments = new ArrayList<>();
                        }
                        this.headerComments.add(attachment);
                    } else if (attachment instanceof PSCommentAfter) {
                        if (this.footerComments == null) {
                            this.footerComments = new ArrayList<>();
                        }
                        this.footerComments.add(attachment);
                    }
                }
            }
        }
        super.processOffDocumentItem(oDI);
    }

    /** {@inheritDoc} */
    @Override
    public void renderPage(final PageViewport page) throws IOException {
        log.debug("renderPage(): " + page);

        if (this.currentPageNumber == 0) {
            writeHeader();
        }

        this.currentPageNumber++;

        this.gen.getResourceTracker().notifyStartNewPage();
        this.gen.getResourceTracker().notifyResourceUsageOnPage(
                PSProcSets.STD_PROCSET);
        this.gen.writeDSCComment(DSCConstants.PAGE,
                new Object[] { page.getPageNumberString(),
                this.currentPageNumber });

        final double pageWidth = page.getViewArea().width / 1000f;
        final double pageHeight = page.getViewArea().height / 1000f;
        boolean rotate = false;
        final List<Long> pageSizes = new ArrayList<>();
        if (getPSUtil().isAutoRotateLandscape() && pageHeight < pageWidth) {
            rotate = true;
            pageSizes.add(Long.valueOf(Math.round(pageHeight)));
            pageSizes.add(Long.valueOf(Math.round(pageWidth)));
        } else {
            pageSizes.add(Long.valueOf(Math.round(pageWidth)));
            pageSizes.add(Long.valueOf(Math.round(pageHeight)));
        }
        this.pageDeviceDictionary.put("/PageSize", pageSizes);

        if (page.hasExtensionAttachments()) {
            for (final ExtensionAttachment attachment : page
                    .getExtensionAttachments()) {
                if (attachment instanceof PSSetPageDevice) {
                    /**
                     * Extract all PSSetPageDevice instances from the attachment
                     * list on the s-p-m and add all dictionary entries to our
                     * internal representation of the the page device
                     * dictionary.
                     */
                    final PSSetPageDevice setPageDevice = (PSSetPageDevice) attachment;
                    final String content = setPageDevice.getContent();
                    if (content != null) {
                        try {
                            this.pageDeviceDictionary.putAll(PSDictionary
                                    .valueOf(content));
                        } catch (final PSDictionaryFormatException e) {
                            final PSEventProducer eventProducer = PSEventProducer.Provider
                                    .get(getUserAgent().getEventBroadcaster());
                            eventProducer.postscriptDictionaryParseError(this,
                                    content, e);
                        }
                    }
                }
            }
        }

        try {
            if (this.setupCodeList != null) {
                PSRenderingUtil.writeEnclosedExtensionAttachments(this.gen,
                        this.setupCodeList);
                this.setupCodeList.clear();
            }
        } catch (final IOException e) {
            log.error(e.getMessage());
        }
        final Integer zero = 0;
        final Rectangle2D pageBoundingBox = new Rectangle2D.Double();
        if (rotate) {
            pageBoundingBox.setRect(0, 0, pageHeight, pageWidth);
            this.gen.writeDSCComment(
                    DSCConstants.PAGE_BBOX,
                    new Object[] { zero, zero,
                            Long.valueOf(Math.round(pageHeight)),
                            Long.valueOf(Math.round(pageWidth)) });
            this.gen.writeDSCComment(DSCConstants.PAGE_HIRES_BBOX,
                    new Object[] { zero, zero, new Double(pageHeight),
                    new Double(pageWidth) });
            this.gen.writeDSCComment(DSCConstants.PAGE_ORIENTATION, "Landscape");
        } else {
            pageBoundingBox.setRect(0, 0, pageWidth, pageHeight);
            this.gen.writeDSCComment(
                    DSCConstants.PAGE_BBOX,
                    new Object[] { zero, zero,
                            Long.valueOf(Math.round(pageWidth)),
                            Long.valueOf(Math.round(pageHeight)) });
            this.gen.writeDSCComment(DSCConstants.PAGE_HIRES_BBOX,
                    new Object[] { zero, zero, new Double(pageWidth),
                    new Double(pageHeight) });
            if (getPSUtil().isAutoRotateLandscape()) {
                this.gen.writeDSCComment(DSCConstants.PAGE_ORIENTATION,
                        "Portrait");
            }
        }
        this.documentBoundingBox.add(pageBoundingBox);
        this.gen.writeDSCComment(DSCConstants.PAGE_RESOURCES,
                new Object[] { DSCConstants.ATEND });

        this.gen.commentln("%FOPSimplePageMaster: "
                + page.getSimplePageMasterName());

        this.gen.writeDSCComment(DSCConstants.BEGIN_PAGE_SETUP);

        if (page.hasExtensionAttachments()) {
            final List<ExtensionAttachment> extensionAttachments = page
                    .getExtensionAttachments();
            for (final ExtensionAttachment attObj : extensionAttachments) {
                if (attObj instanceof PSExtensionAttachment) {
                    final PSExtensionAttachment attachment = (PSExtensionAttachment) attObj;
                    if (attachment instanceof PSCommentBefore) {
                        this.gen.commentln("%" + attachment.getContent());
                    } else if (attachment instanceof PSSetupCode) {
                        this.gen.writeln(attachment.getContent());
                    }
                }
            }
        }

        // Write any unwritten changes to page device dictionary
        if (!this.pageDeviceDictionary.isEmpty()) {
            String content = this.pageDeviceDictionary.getContent();
            if (getPSUtil().isSafeSetPageDevice()) {
                content += " SSPD";
            } else {
                content += " setpagedevice";
            }
            PSRenderingUtil.writeEnclosedExtensionAttachment(this.gen,
                    new PSSetPageDevice(content));
        }

        if (rotate) {
            this.gen.writeln(Math.round(pageHeight) + " 0 translate");
            this.gen.writeln("90 rotate");
        }
        concatMatrix(1, 0, 0, -1, 0, pageHeight);

        this.gen.writeDSCComment(DSCConstants.END_PAGE_SETUP);

        // Process page
        super.renderPage(page);

        // Show page
        this.gen.showPage();
        this.gen.writeDSCComment(DSCConstants.PAGE_TRAILER);
        if (page.hasExtensionAttachments()) {
            final List<ExtensionAttachment> extensionAttachments = page
                    .getExtensionAttachments();
            for (final ExtensionAttachment attObj : extensionAttachments) {
                if (attObj instanceof PSExtensionAttachment) {
                    final PSExtensionAttachment attachment = (PSExtensionAttachment) attObj;
                    if (attachment instanceof PSCommentAfter) {
                        this.gen.commentln("%" + attachment.getContent());
                    }
                }
            }
        }
        this.gen.getResourceTracker().writeResources(true, this.gen);
    }

    /** {@inheritDoc} */
    @Override
    protected void renderRegionViewport(final RegionViewport port) {
        if (port != null) {
            comment("%FOPBeginRegionViewport: "
                    + port.getRegionReference().getRegionName());
            super.renderRegionViewport(port);
            comment("%FOPEndRegionViewport");
        }
    }

    /** Indicates the beginning of a text object. */
    @Override
    protected void beginTextObject() {
        if (!this.inTextMode) {
            saveGraphicsState();
            writeln("BT");
            this.inTextMode = true;
        }
    }

    /** Indicates the end of a text object. */
    @Override
    protected void endTextObject() {
        if (this.inTextMode) {
            this.inTextMode = false; // set before restoreGraphicsState() to
            // avoid recursion
            writeln("ET");
            restoreGraphicsState();
        }
    }

    /** {@inheritDoc} */
    @Override
    public void renderText(final TextArea area) {
        renderInlineAreaBackAndBorders(area);
        final String fontkey = getInternalFontNameForArea(area);
        final int fontsize = area.getTraitAsInteger(Trait.FONT_SIZE);

        // This assumes that *all* CIDFonts use a /ToUnicode mapping
        final Typeface tf = (Typeface) this.fontInfo.getFonts().get(fontkey);

        // Determine position
        final int rx = this.currentIPPosition
                + area.getBorderAndPaddingWidthStart();
        final int bl = this.currentBPPosition + area.getOffset()
                + area.getBaselineOffset();

        final Color ct = (Color) area.getTrait(Trait.COLOR);
        if (ct != null) {
            try {
                useColor(ct);
            } catch (final IOException ioe) {
                handleIOTrouble(ioe);
            }
        }

        beginTextObject();
        writeln("1 0 0 -1 " + this.gen.formatDouble(rx / 1000f) + " "
                + this.gen.formatDouble(bl / 1000f) + " Tm");

        super.renderText(area); // Updates IPD

        renderTextDecoration(tf, fontsize, area, bl, rx);
    }

    /** {@inheritDoc} */
    @Override
    protected void renderWord(final WordArea word) {
        renderText((TextArea) word.getParentArea(), word.getWord(),
                word.getLetterAdjustArray());
        super.renderWord(word);
    }

    /** {@inheritDoc} */
    @Override
    protected void renderSpace(final SpaceArea space) {
        final AbstractTextArea textArea = (AbstractTextArea) space
                .getParentArea();
        final String s = space.getSpace();
        final char sp = s.charAt(0);
        final Font font = getFontFromArea(textArea);

        final int tws = space.isAdjustable() ? ((TextArea) space
                .getParentArea()).getTextWordSpaceAdjust()
                + 2
                * textArea.getTextLetterSpaceAdjust() : 0;

        rmoveTo((font.getCharWidth(sp) + tws) / 1000f, 0);
        super.renderSpace(space);
    }

    private Typeface getTypeface(final String fontName) {
        Typeface tf = (Typeface) this.fontInfo.getFonts().get(fontName);
        if (tf instanceof LazyFont) {
            tf = ((LazyFont) tf).getRealFont();
        }
        return tf;
    }

    private void renderText(final AbstractTextArea area, final String text,
            final int[] letterAdjust) {
        final String fontkey = getInternalFontNameForArea(area);
        final int fontSize = area.getTraitAsInteger(Trait.FONT_SIZE);
        final Font font = getFontFromArea(area);
        final Typeface tf = getTypeface(font.getFontName());
        SingleByteFont singleByteFont = null;
        if (tf instanceof SingleByteFont) {
            singleByteFont = (SingleByteFont) tf;
        }

        final int textLen = text.length();
        if (singleByteFont != null && singleByteFont.hasAdditionalEncodings()) {
            int start = 0;
            int currentEncoding = -1;
            for (int i = 0; i < textLen; ++i) {
                final char c = text.charAt(i);
                final char mapped = tf.mapChar(c);
                final int encoding = mapped / 256;
                if (currentEncoding != encoding) {
                    if (i > 0) {
                        writeText(area, text, start, i - start, letterAdjust,
                                fontSize, tf);
                    }
                    if (encoding == 0) {
                        useFont(fontkey, fontSize);
                    } else {
                        useFont(fontkey + "_" + Integer.toString(encoding),
                                fontSize);
                    }
                    currentEncoding = encoding;
                    start = i;
                }
            }
            writeText(area, text, start, textLen - start, letterAdjust,
                    fontSize, tf);
        } else {
            useFont(fontkey, fontSize);
            writeText(area, text, 0, textLen, letterAdjust, fontSize, tf);
        }
    }

    private void writeText(final AbstractTextArea area, final String text,
            final int start, final int len, final int[] letterAdjust,
            final int fontsize, final Typeface tf) {
        final int end = start + len;
        int initialSize = text.length();
        initialSize += initialSize / 2;
        final StringBuilder sb = new StringBuilder(initialSize);
        if (letterAdjust == null && area.getTextLetterSpaceAdjust() == 0
                && area.getTextWordSpaceAdjust() == 0) {
            sb.append("(");
            for (int i = start; i < end; ++i) {
                final char c = text.charAt(i);
                final char mapped = (char) (tf.mapChar(c) % 256);
                PSGenerator.escapeChar(mapped, sb);
            }
            sb.append(") t");
        } else {
            sb.append("(");
            final int[] offsets = new int[len];
            for (int i = start; i < end; ++i) {
                final char c = text.charAt(i);
                final char mapped = tf.mapChar(c);
                final char codepoint = (char) (mapped % 256);
                int wordSpace;

                if (CharUtilities.isAdjustableSpace(mapped)) {
                    wordSpace = area.getTextWordSpaceAdjust();
                } else {
                    wordSpace = 0;
                }
                final int cw = tf.getWidth(mapped, fontsize) / 1000;
                final int ladj = letterAdjust != null && i < end - 1 ? letterAdjust[i + 1]
                        : 0;
                final int tls = i < end - 1 ? area.getTextLetterSpaceAdjust()
                        : 0;
                offsets[i - start] = cw + ladj + tls + wordSpace;
                PSGenerator.escapeChar(codepoint, sb);
            }
            sb.append(")" + PSGenerator.LF + "[");
            for (int i = 0; i < len; ++i) {
                if (i > 0) {
                    if (i % 8 == 0) {
                        sb.append(PSGenerator.LF);
                    } else {
                        sb.append(" ");
                    }
                }
                sb.append(this.gen.formatDouble(offsets[i] / 1000f));
            }
            sb.append("]" + PSGenerator.LF + "xshow");
        }
        writeln(sb.toString());
    }

    /** {@inheritDoc} */
    @Override
    protected List<PSState> breakOutOfStateStack() {
        try {
            final List<PSState> breakOutList = new ArrayList<>();
            PSState state;
            while (true) {
                if (breakOutList.size() == 0) {
                    endTextObject();
                    comment("------ break out!");
                }
                state = this.gen.getCurrentState();
                if (!this.gen.restoreGraphicsState()) {
                    break;
                }
                breakOutList.add(0, state); // Insert because of stack-popping
            }
            return breakOutList;
        } catch (final IOException ioe) {
            handleIOTrouble(ioe);
            return null;
        }
    }

    /** {@inheritDoc} */
    @Override
    protected void restoreStateStackAfterBreakOut(final List breakOutList) {
        try {
            comment("------ restoring context after break-out...");
            for (final Object state : breakOutList) {
                saveGraphicsState();
                ((PSState) state).reestablish(this.gen);
            }
            comment("------ done.");
        } catch (final IOException ioe) {
            handleIOTrouble(ioe);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void startVParea(final CTM ctm, final Rectangle2D clippingRect) {
        saveGraphicsState();
        if (clippingRect != null) {
            clipRect((float) clippingRect.getX() / 1000f,
                    (float) clippingRect.getY() / 1000f,
                    (float) clippingRect.getWidth() / 1000f,
                    (float) clippingRect.getHeight() / 1000f);
        }
        // multiply with current CTM
        final double[] matrix = ctm.toArray();
        matrix[4] /= 1000f;
        matrix[5] /= 1000f;
        concatMatrix(matrix);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void endVParea() {
        restoreGraphicsState();
    }

    /** {@inheritDoc} */
    @Override
    protected void renderBlockViewport(final BlockViewport bv,
            final List children) {
        comment("%FOPBeginBlockViewport: " + bv.toString());
        super.renderBlockViewport(bv, children);
        comment("%FOPEndBlockViewport");
    }

    /** {@inheritDoc} */
    @Override
    protected void renderInlineParent(final InlineParent ip) {
        super.renderInlineParent(ip);
    }

    /** {@inheritDoc} */
    @Override
    public void renderLeader(final Leader area) {
        renderInlineAreaBackAndBorders(area);

        final int style = area.getRuleStyle();
        final int ruleThickness = area.getRuleThickness();
        final int startx = this.currentIPPosition
                + area.getBorderAndPaddingWidthStart();
        final int starty = this.currentBPPosition + area.getOffset()
                + ruleThickness / 2;
        final int endx = this.currentIPPosition
                + area.getBorderAndPaddingWidthStart() + area.getIPD();
        final Color col = (Color) area.getTrait(Trait.COLOR);

        endTextObject();
        try {
            this.borderPainter
            .drawLine(new Point(startx, starty),
                    new Point(endx, starty), ruleThickness, col,
                    RuleStyle.valueOf(style));
        } catch (final IOException ioe) {
            handleIOTrouble(ioe);
        }

        super.renderLeader(area);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void renderImage(final Image image, final Rectangle2D pos) {
        drawImage(image.getURL(), pos, image.getForeignAttributes());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected RendererContext createRendererContext(final int x, final int y,
            final int width, final int height, final Map foreignAttributes) {
        final RendererContext context = super.createRendererContext(x, y,
                width, height, foreignAttributes);
        context.setProperty(PSRendererContextConstants.PS_GENERATOR, this.gen);
        context.setProperty(PSRendererContextConstants.PS_FONT_INFO,
                this.fontInfo);
        return context;
    }

    /** {@inheritDoc} */
    @Override
    public String getMimeType() {
        return MIME_TYPE;
    }

    /**
     * Sets whether or not the safe set page device macro should be used (as
     * opposed to directly invoking setpagedevice) when setting the postscript
     * page device.
     *
     * This option is a useful option when you want to guard against the
     * possibility of invalid/unsupported postscript key/values being placed in
     * the page device.
     *
     * @param safeSetPageDevice
     *            setting to false and the renderer will make a standard
     *            "setpagedevice" call, setting to true will make a safe set
     *            page device macro call (default is false).
     */
    public void setSafeSetPageDevice(final boolean safeSetPageDevice) {
        getPSUtil().setSafeSetPageDevice(safeSetPageDevice);
    }

    /**
     * Sets whether or not PostScript Document Structuring Conventions (dsc)
     * compliance are enforced.
     * <p>
     * It can cause problems (unwanted PostScript subsystem
     * initgraphics/erasepage calls) on some printers when the pagedevice is
     * set. If this causes problems on a particular implementation then use this
     * setting with a 'false' value to try and minimize the number of
     * setpagedevice calls in the postscript document output.
     * <p>
     * Set this value to false if you experience unwanted blank pages in your
     * postscript output.
     *
     * @param dscCompliant
     *            boolean value (default is true)
     */
    public void setDSCCompliant(final boolean dscCompliant) {
        getPSUtil().setDSCComplianceEnabled(dscCompliant);
    }

}
