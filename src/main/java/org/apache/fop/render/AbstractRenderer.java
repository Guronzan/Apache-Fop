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

/* $Id: AbstractRenderer.java 932497 2010-04-09 16:34:29Z vhennebert $ */

package org.apache.fop.render;

// Java
import java.awt.Rectangle;
import java.awt.geom.AffineTransform;
import java.awt.geom.Rectangle2D;
import java.io.IOException;
import java.io.OutputStream;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.fop.ResourceEventProducer;
import org.apache.fop.apps.FOPException;
import org.apache.fop.apps.FOUserAgent;
import org.apache.fop.area.Area;
import org.apache.fop.area.BeforeFloat;
import org.apache.fop.area.Block;
import org.apache.fop.area.BlockViewport;
import org.apache.fop.area.BodyRegion;
import org.apache.fop.area.CTM;
import org.apache.fop.area.Footnote;
import org.apache.fop.area.LineArea;
import org.apache.fop.area.MainReference;
import org.apache.fop.area.NormalFlow;
import org.apache.fop.area.OffDocumentItem;
import org.apache.fop.area.Page;
import org.apache.fop.area.PageSequence;
import org.apache.fop.area.PageViewport;
import org.apache.fop.area.RegionReference;
import org.apache.fop.area.RegionViewport;
import org.apache.fop.area.Span;
import org.apache.fop.area.Trait;
import org.apache.fop.area.inline.Container;
import org.apache.fop.area.inline.ForeignObject;
import org.apache.fop.area.inline.Image;
import org.apache.fop.area.inline.InlineArea;
import org.apache.fop.area.inline.InlineBlockParent;
import org.apache.fop.area.inline.InlineParent;
import org.apache.fop.area.inline.Leader;
import org.apache.fop.area.inline.Space;
import org.apache.fop.area.inline.SpaceArea;
import org.apache.fop.area.inline.TextArea;
import org.apache.fop.area.inline.Viewport;
import org.apache.fop.area.inline.WordArea;
import org.apache.fop.fo.Constants;
import org.apache.fop.fonts.FontInfo;
import org.w3c.dom.Document;

/**
 * Abstract base class for all renderers. The Abstract renderer does all the top
 * level processing of the area tree and adds some abstract methods to handle
 * viewports. This keeps track of the current block and inline position.
 */
public abstract class AbstractRenderer implements Renderer, Constants {

    /**
     * user agent
     */
    protected FOUserAgent userAgent = null;

    /**
     * block progression position
     */
    protected int currentBPPosition = 0;

    /**
     * inline progression position
     */
    protected int currentIPPosition = 0;

    /**
     * the block progression position of the containing block used for
     * absolutely positioned blocks
     */
    protected int containingBPPosition = 0;

    /**
     * the inline progression position of the containing block used for
     * absolutely positioned blocks
     */
    protected int containingIPPosition = 0;

    /** the currently active PageViewport */
    protected PageViewport currentPageViewport;

    private Set<String> warnedXMLHandlers;

    /** {@inheritDoc} */
    @Override
    public abstract void setupFontInfo(final FontInfo fontInfo)
            throws FOPException;

    /** {@inheritDoc} */
    @Override
    public void setUserAgent(final FOUserAgent agent) {
        this.userAgent = agent;
    }

    /** {@inheritDoc} */
    @Override
    public FOUserAgent getUserAgent() {
        if (this.userAgent == null) {
            throw new IllegalStateException(
                    "FOUserAgent has not been set on Renderer");
        }
        return this.userAgent;
    }

    /**
     * {@inheritDoc}
     *
     * @throws IOException
     */
    @Override
    public void startRenderer(final OutputStream outputStream)
            throws IOException {
        if (this.userAgent == null) {
            throw new IllegalStateException(
                    "FOUserAgent has not been set on Renderer");
        }
    }

    /**
     * {@inheritDoc}
     * 
     * @throws IOException
     */
    @Override
    public void stopRenderer() throws IOException {
    }

    /**
     * Check if this renderer supports out of order rendering. If this renderer
     * supports out of order rendering then it means that the pages that are not
     * ready will be prepared and a future page will be rendered.
     *
     * @return True if the renderer supports out of order rendering
     */
    @Override
    public boolean supportsOutOfOrder() {
        return false;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void processOffDocumentItem(final OffDocumentItem odi) {
    }

    /** {@inheritDoc} */
    @Override
    public Graphics2DAdapter getGraphics2DAdapter() {
        return null;
    }

    /** {@inheritDoc} */
    @Override
    public ImageAdapter getImageAdapter() {
        return null;
    }

    /** @return the current PageViewport or null, if none is active */
    protected PageViewport getCurrentPageViewport() {
        return this.currentPageViewport;
    }

    /** {@inheritDoc} */
    @Override
    public void preparePage(final PageViewport page) {
    }

    /**
     * Utility method to convert a page sequence title to a string. Some
     * renderers may only be able to use a string title. A title is a sequence
     * of inline areas that this method attempts to convert to an equivalent
     * string.
     *
     * @param title
     *            The Title to convert
     * @return An expanded string representing the title
     */
    protected String convertTitleToString(final LineArea title) {
        final List<InlineArea> children = title.getInlineAreas();
        final String str = convertToString(children);
        return str.trim();
    }

    private String convertToString(final List<InlineArea> children) {
        final StringBuilder sb = new StringBuilder();
        for (int count = 0; count < children.size(); count++) {
            final InlineArea inline = children.get(count);
            // if (inline instanceof Character) {
            // sb.append(((Character) inline).getChar());
            /* } else */if (inline instanceof TextArea) {
                sb.append(((TextArea) inline).getText());
            } else if (inline instanceof InlineParent) {
                sb.append(convertToString(((InlineParent) inline)
                        .getChildAreas()));
            } else {
                sb.append(" ");
            }
        }
        return sb.toString();
    }

    /** {@inheritDoc} */
    @Override
    public void startPageSequence(final LineArea seqTitle) {
        // do nothing
    }

    /** {@inheritDoc} */
    @Override
    public void startPageSequence(final PageSequence pageSequence) {
        startPageSequence(pageSequence.getTitle());
    }

    // normally this would be overriden to create a page in the
    // output
    /**
     * {@inheritDoc}
     *
     * @throws IOException
     */
    @Override
    public void renderPage(final PageViewport page) throws IOException {

        this.currentPageViewport = page;
        try {
            final Page p = page.getPage();
            renderPageAreas(p);
        } finally {
            this.currentPageViewport = null;
        }
    }

    /**
     * Renders page areas.
     *
     * @param page
     *            The page whos page areas are to be rendered
     */
    protected void renderPageAreas(final Page page) {
        /*
         * Spec does not appear to specify whether fo:region-body should appear
         * above or below side regions in cases of overlap. FOP decision is to
         * have fo:region-body on top, hence it is rendered last here.
         */
        RegionViewport viewport;
        viewport = page.getRegionViewport(FO_REGION_BEFORE);
        if (viewport != null) {
            renderRegionViewport(viewport);
        }
        viewport = page.getRegionViewport(FO_REGION_START);
        if (viewport != null) {
            renderRegionViewport(viewport);
        }
        viewport = page.getRegionViewport(FO_REGION_END);
        if (viewport != null) {
            renderRegionViewport(viewport);
        }
        viewport = page.getRegionViewport(FO_REGION_AFTER);
        if (viewport != null) {
            renderRegionViewport(viewport);
        }
        viewport = page.getRegionViewport(FO_REGION_BODY);
        if (viewport != null) {
            renderRegionViewport(viewport);
        }
    }

    /**
     * Renders a region viewport.
     * <p>
     *
     * The region may clip the area and it establishes a position from where the
     * region is placed.
     * </p>
     *
     * @param port
     *            The region viewport to be rendered
     */
    protected void renderRegionViewport(final RegionViewport port) {
        final Rectangle2D view = port.getViewArea();
        // The CTM will transform coordinates relative to
        // this region-reference area into page coords, so
        // set origin for the region to 0,0.
        this.currentBPPosition = 0;
        this.currentIPPosition = 0;

        final RegionReference regionReference = port.getRegionReference();
        handleRegionTraits(port);

        // shouldn't the viewport have the CTM
        startVParea(regionReference.getCTM(), port.isClip() ? view : null);
        // do after starting viewport area
        if (regionReference.getRegionClass() == FO_REGION_BODY) {
            renderBodyRegion((BodyRegion) regionReference);
        } else {
            renderRegion(regionReference);
        }
        endVParea();
    }

    /**
     * Establishes a new viewport area.
     *
     * @param ctm
     *            the coordinate transformation matrix to use
     * @param clippingRect
     *            the clipping rectangle if the viewport should be clipping,
     *            null if no clipping is performed.
     */
    protected abstract void startVParea(final CTM ctm,
            final Rectangle2D clippingRect);

    /**
     * Signals exit from a viewport area. Subclasses can restore transformation
     * matrices valid before the viewport area was started.
     */
    protected abstract void endVParea();

    /**
     * Handle the traits for a region This is used to draw the traits for the
     * given page region. (See Sect. 6.4.1.2 of XSL-FO spec.)
     *
     * @param rv
     *            the RegionViewport whose region is to be drawn
     */
    protected void handleRegionTraits(final RegionViewport rv) {
        // draw border and background
    }

    /**
     * Renders a region reference area.
     *
     * @param region
     *            The region reference area
     */
    protected void renderRegion(final RegionReference region) {
        renderBlocks(null, region.getBlocks());
    }

    /**
     * Renders a body region area.
     *
     * @param region
     *            The body region
     */
    protected void renderBodyRegion(final BodyRegion region) {
        final BeforeFloat bf = region.getBeforeFloat();
        if (bf != null) {
            renderBeforeFloat(bf);
        }
        final MainReference mr = region.getMainReference();
        if (mr != null) {
            renderMainReference(mr);
        }
        final Footnote foot = region.getFootnote();
        if (foot != null) {
            renderFootnote(foot);
        }
    }

    /**
     * Renders a before float area.
     *
     * @param bf
     *            The before float area
     */
    protected void renderBeforeFloat(final BeforeFloat bf) {
        final List blocks = bf.getChildAreas();
        if (blocks != null) {
            renderBlocks(null, blocks);
            final Block sep = bf.getSeparator();
            if (sep != null) {
                renderBlock(sep);
            }
        }
    }

    /**
     * Renders a footnote
     *
     * @param footnote
     *            The footnote
     */
    protected void renderFootnote(final Footnote footnote) {
        this.currentBPPosition += footnote.getTop();
        final List blocks = footnote.getChildAreas();
        if (blocks != null) {
            final Block sep = footnote.getSeparator();
            if (sep != null) {
                renderBlock(sep);
            }
            renderBlocks(null, blocks);
        }
    }

    /**
     * Renders the main reference area.
     * <p>
     * The main reference area contains a list of spans that are stacked on the
     * page. The spans contain a list of normal flow reference areas that are
     * positioned into columns.
     * </p>
     *
     * @param mr
     *            The main reference area
     */
    protected void renderMainReference(final MainReference mr) {
        final int saveIPPos = this.currentIPPosition;

        Span span = null;
        final List spans = mr.getSpans();
        final int saveBPPos = this.currentBPPosition;
        int saveSpanBPPos = saveBPPos;
        for (int count = 0; count < spans.size(); count++) {
            span = (Span) spans.get(count);
            for (int c = 0; c < span.getColumnCount(); c++) {
                final NormalFlow flow = span.getNormalFlow(c);

                if (flow != null) {
                    this.currentBPPosition = saveSpanBPPos;
                    renderFlow(flow);
                    this.currentIPPosition += flow.getIPD();
                    this.currentIPPosition += mr.getColumnGap();
                }
            }
            this.currentIPPosition = saveIPPos;
            this.currentBPPosition = saveSpanBPPos + span.getHeight();
            saveSpanBPPos = this.currentBPPosition;
        }
        this.currentBPPosition = saveBPPos;
    }

    /**
     * Renders a flow reference area.
     *
     * @param flow
     *            The flow reference area
     */
    protected void renderFlow(final NormalFlow flow) {
        // the normal flow reference area contains stacked blocks
        final List<Block> blocks = flow.getChildAreas();
        if (blocks != null) {
            renderBlocks(null, blocks);
        }
    }

    /**
     * Handle block traits. This method is called when the correct ip and bp
     * posiiton is set. This should be overridden to draw border and background
     * traits for the block area.
     *
     * @param block
     *            the block area
     */
    protected void handleBlockTraits(final Block block) {
        // draw border and background
    }

    /**
     * Renders a block viewport.
     *
     * @param bv
     *            The block viewport
     * @param children
     *            The children to render within the block viewport
     */
    protected void renderBlockViewport(final BlockViewport bv,
            final List children) {
        // clip and position viewport if necessary
        if (bv.getPositioning() == Block.ABSOLUTE) {
            // save positions
            final int saveIP = this.currentIPPosition;
            final int saveBP = this.currentBPPosition;

            Rectangle2D clippingRect = null;
            if (bv.getClip()) {
                clippingRect = new Rectangle(saveIP, saveBP, bv.getIPD(),
                        bv.getBPD());
            }

            final CTM ctm = bv.getCTM();
            this.currentIPPosition = 0;
            this.currentBPPosition = 0;

            startVParea(ctm, clippingRect);
            handleBlockTraits(bv);
            renderBlocks(bv, children);
            endVParea();

            // clip if necessary

            this.currentIPPosition = saveIP;
            this.currentBPPosition = saveBP;
        } else {
            // save position and offset
            final int saveIP = this.currentIPPosition;
            final int saveBP = this.currentBPPosition;

            handleBlockTraits(bv);
            renderBlocks(bv, children);

            this.currentIPPosition = saveIP;
            this.currentBPPosition = saveBP + bv.getAllocBPD();
        }
    }

    /**
     * Renders a block area that represents a reference area. The reference area
     * establishes a new coordinate system.
     *
     * @param block
     *            the block area
     */
    protected abstract void renderReferenceArea(final Block block);

    /**
     * Renders a list of block areas.
     *
     * @param parent
     *            the parent block if the parent is a block, otherwise a null
     *            value.
     * @param blocks
     *            The block areas
     */
    protected void renderBlocks(final Block parent, final List<Block> blocks) {
        final int saveIP = this.currentIPPosition;
        // int saveBP = currentBPPosition;

        // Calculate the position of the content rectangle.
        if (parent != null && !parent.getTraitAsBoolean(Trait.IS_VIEWPORT_AREA)) {
            this.currentBPPosition += parent.getBorderAndPaddingWidthBefore();
            /*
             * This is unnecessary now as we're going to use the *-indent traits
             * currentIPPosition += parent.getBorderAndPaddingWidthStart();
             * Integer spaceStart = (Integer)
             * parent.getTrait(Trait.SPACE_START); if (spaceStart != null) {
             * currentIPPosition += spaceStart.intValue(); }
             */
        }

        // the position of the containing block is used for
        // absolutely positioned areas
        final int contBP = this.currentBPPosition;
        final int contIP = this.currentIPPosition;
        this.containingBPPosition = this.currentBPPosition;
        this.containingIPPosition = this.currentIPPosition;

        for (int count = 0; count < blocks.size(); count++) {
            final Object obj = blocks.get(count);
            if (obj instanceof Block) {
                this.currentIPPosition = contIP;
                this.containingBPPosition = contBP;
                this.containingIPPosition = contIP;
                renderBlock((Block) obj);
                this.containingBPPosition = contBP;
                this.containingIPPosition = contIP;
            } else {
                // a line area is rendered from the top left position
                // of the line, each inline object is offset from there
                final LineArea line = (LineArea) obj;
                this.currentIPPosition = contIP + parent.getStartIndent()
                        + line.getStartIndent();
                renderLineArea(line);
                // InlineArea child = (InlineArea) line.getInlineAreas().get(0);
                this.currentBPPosition += line.getAllocBPD();
            }
            this.currentIPPosition = saveIP;
        }
    }

    /**
     * Renders a block area.
     *
     * @param block
     *            The block area
     */
    protected void renderBlock(final Block block) {
        final List children = block.getChildAreas();
        if (block instanceof BlockViewport) {
            if (children != null) {
                renderBlockViewport((BlockViewport) block, children);
            } else {
                handleBlockTraits(block);
                // simply move position
                this.currentBPPosition += block.getAllocBPD();
            }
        } else if (block.getTraitAsBoolean(Trait.IS_REFERENCE_AREA)) {
            renderReferenceArea(block);
        } else {
            // save position and offset
            final int saveIP = this.currentIPPosition;
            final int saveBP = this.currentBPPosition;

            this.currentIPPosition += block.getXOffset();
            this.currentBPPosition += block.getYOffset();
            this.currentBPPosition += block.getSpaceBefore();

            handleBlockTraits(block);

            if (children != null) {
                renderBlocks(block, children);
            }

            if (block.getPositioning() == Block.ABSOLUTE) {
                // absolute blocks do not effect the layout
                this.currentBPPosition = saveBP;
            } else {
                // stacked and relative blocks effect stacking
                this.currentIPPosition = saveIP;
                this.currentBPPosition = saveBP + block.getAllocBPD();
            }
        }
    }

    /**
     * Renders a line area.
     * <p>
     *
     * A line area may have grouped styling for its children such as underline,
     * background.
     * </p>
     *
     * @param line
     *            The line area
     */
    protected void renderLineArea(final LineArea line) {
        final List<InlineArea> children = line.getInlineAreas();
        final int saveBP = this.currentBPPosition;
        this.currentBPPosition += line.getSpaceBefore();
        for (int count = 0; count < children.size(); count++) {
            final InlineArea inline = children.get(count);
            renderInlineArea(inline);
        }
        this.currentBPPosition = saveBP;
    }

    /**
     * Render the given InlineArea.
     *
     * @param inlineArea
     *            inline area text to render
     */
    protected void renderInlineArea(final InlineArea inlineArea) {
        if (inlineArea instanceof TextArea) {
            renderText((TextArea) inlineArea);
            // } else if (inlineArea instanceof Character) {
            // renderCharacter((Character) inlineArea);
        } else if (inlineArea instanceof WordArea) {
            renderWord((WordArea) inlineArea);
        } else if (inlineArea instanceof SpaceArea) {
            renderSpace((SpaceArea) inlineArea);
        } else if (inlineArea instanceof InlineParent) {
            renderInlineParent((InlineParent) inlineArea);
        } else if (inlineArea instanceof InlineBlockParent) {
            renderInlineBlockParent((InlineBlockParent) inlineArea);
        } else if (inlineArea instanceof Space) {
            renderInlineSpace((Space) inlineArea);
        } else if (inlineArea instanceof Viewport) {
            renderViewport((Viewport) inlineArea);
        } else if (inlineArea instanceof Leader) {
            renderLeader((Leader) inlineArea);
        }
    }

    /**
     * Common method to render the background and borders for any inline area.
     * The all borders and padding are drawn outside the specified area.
     *
     * @param area
     *            the inline area for which the background, border and padding
     *            is to be rendered
     */
    protected abstract void renderInlineAreaBackAndBorders(final InlineArea area);

    /**
     * Render the given Space.
     *
     * @param space
     *            the space to render
     */
    protected void renderInlineSpace(final Space space) {
        renderInlineAreaBackAndBorders(space);
        // an inline space moves the inline progression position
        // for the current block by the width or height of the space
        // it may also have styling (only on this object) that needs
        // handling
        this.currentIPPosition += space.getAllocIPD();
    }

    /**
     * Render the given Leader.
     *
     * @param area
     *            the leader to render
     */
    protected void renderLeader(final Leader area) {
        this.currentIPPosition += area.getAllocIPD();
    }

    /**
     * Render the given TextArea.
     *
     * @param text
     *            the text to render
     */
    protected void renderText(final TextArea text) {
        final int saveIP = this.currentIPPosition;
        final int saveBP = this.currentBPPosition;
        for (final Area area : text.getChildAreas()) {
            renderInlineArea((InlineArea) area);
        }
        this.currentIPPosition = saveIP + text.getAllocIPD();
    }

    /**
     * Render the given WordArea.
     *
     * @param word
     *            the word to render
     */
    protected void renderWord(final WordArea word) {
        this.currentIPPosition += word.getAllocIPD();
    }

    /**
     * Render the given SpaceArea.
     *
     * @param space
     *            the space to render
     */
    protected void renderSpace(final SpaceArea space) {
        this.currentIPPosition += space.getAllocIPD();
    }

    /**
     * Render the given InlineParent.
     *
     * @param ip
     *            the inline parent to render
     */
    protected void renderInlineParent(final InlineParent ip) {
        renderInlineAreaBackAndBorders(ip);
        final int saveIP = this.currentIPPosition;
        final int saveBP = this.currentBPPosition;
        this.currentIPPosition += ip.getBorderAndPaddingWidthStart();
        this.currentBPPosition += ip.getOffset();
        for (final Area area : ip.getChildAreas()) {
            renderInlineArea((InlineArea) area);
        }
        this.currentIPPosition = saveIP + ip.getAllocIPD();
        this.currentBPPosition = saveBP;
    }

    /**
     * Render the given InlineBlockParent.
     *
     * @param ibp
     *            the inline block parent to render
     */
    protected void renderInlineBlockParent(final InlineBlockParent ibp) {
        renderInlineAreaBackAndBorders(ibp);
        this.currentIPPosition += ibp.getBorderAndPaddingWidthStart();
        // For inline content the BP position is updated by the enclosing line
        // area
        final int saveBP = this.currentBPPosition;
        this.currentBPPosition += ibp.getOffset();
        renderBlock(ibp.getChildArea());
        this.currentBPPosition = saveBP;
    }

    /**
     * Render the given Viewport.
     *
     * @param viewport
     *            the viewport to render
     */
    protected void renderViewport(final Viewport viewport) {
        final Area content = viewport.getContent();
        final int saveBP = this.currentBPPosition;
        this.currentBPPosition += viewport.getOffset();
        final Rectangle2D contpos = viewport.getContentPosition();
        if (content instanceof Image) {
            renderImage((Image) content, contpos);
        } else if (content instanceof Container) {
            renderContainer((Container) content);
        } else if (content instanceof ForeignObject) {
            renderForeignObject((ForeignObject) content, contpos);
        } else if (content instanceof InlineBlockParent) {
            renderInlineBlockParent((InlineBlockParent) content);
        }
        this.currentIPPosition += viewport.getAllocIPD();
        this.currentBPPosition = saveBP;
    }

    /**
     * Renders an image area.
     *
     * @param image
     *            The image
     * @param pos
     *            The target position of the image (todo) Make renderImage()
     *            protected
     */
    public void renderImage(final Image image, final Rectangle2D pos) {
        // Default: do nothing.
        // Some renderers (ex. Text) don't support images.
    }

    /**
     * Tells the renderer to render an inline container.
     *
     * @param cont
     *            The inline container area
     */
    protected void renderContainer(final Container cont) {
        final int saveIP = this.currentIPPosition;
        final int saveBP = this.currentBPPosition;

        final List<Block> blocks = cont.getBlocks();
        renderBlocks(null, blocks);
        this.currentIPPosition = saveIP;
        this.currentBPPosition = saveBP;
    }

    /**
     * Renders a foreign object area.
     *
     * @param fo
     *            The foreign object area
     * @param pos
     *            The target position of the foreign object (todo) Make
     *            renderForeignObject() protected
     */
    protected void renderForeignObject(final ForeignObject fo,
            final Rectangle2D pos) {
        // Default: do nothing.
        // Some renderers (ex. Text) don't support foreign objects.
    }

    /**
     * Render the xml document with the given xml namespace. The Render Context
     * is by the handle to render into the current rendering target.
     *
     * @param ctx
     *            rendering context
     * @param doc
     *            DOM Document containing the source document
     * @param namespace
     *            Namespace URI of the document
     */
    public void renderXML(final RendererContext ctx, final Document doc,
            final String namespace) {
        final XMLHandler handler = this.userAgent.getXMLHandlerRegistry()
                .getXMLHandler(this, namespace);
        if (handler != null) {
            try {
                final XMLHandlerConfigurator configurator = new XMLHandlerConfigurator(
                        this.userAgent);
                configurator.configure(ctx, namespace);
                handler.handleXML(ctx, doc, namespace);
            } catch (final Exception e) {
                // could not handle document
                final ResourceEventProducer eventProducer = ResourceEventProducer.Provider
                        .get(ctx.getUserAgent().getEventBroadcaster());
                eventProducer
                .foreignXMLProcessingError(this, doc, namespace, e);
            }
        } else {
            if (this.warnedXMLHandlers == null) {
                this.warnedXMLHandlers = new HashSet<>();
            }
            if (!this.warnedXMLHandlers.contains(namespace)) {
                // no handler found for document
                this.warnedXMLHandlers.add(namespace);
                final ResourceEventProducer eventProducer = ResourceEventProducer.Provider
                        .get(ctx.getUserAgent().getEventBroadcaster());
                eventProducer.foreignXMLNoHandler(this, doc, namespace);
            }
        }
    }

    /**
     * Converts a millipoint-based transformation matrix to points.
     *
     * @param at
     *            a millipoint-based transformation matrix
     * @return a point-based transformation matrix
     */
    protected AffineTransform mptToPt(final AffineTransform at) {
        final double[] matrix = new double[6];
        at.getMatrix(matrix);
        // Convert to points
        matrix[4] = matrix[4] / 1000;
        matrix[5] = matrix[5] / 1000;
        return new AffineTransform(matrix);
    }

    /**
     * Converts a point-based transformation matrix to millipoints.
     *
     * @param at
     *            a point-based transformation matrix
     * @return a millipoint-based transformation matrix
     */
    protected AffineTransform ptToMpt(final AffineTransform at) {
        final double[] matrix = new double[6];
        at.getMatrix(matrix);
        // Convert to millipoints
        // Math.round() because things like this can happen: 65.6 * 1000 =
        // 65.599999999999999
        // which is bad for testing
        matrix[4] = Math.round(matrix[4] * 1000);
        matrix[5] = Math.round(matrix[5] * 1000);
        return new AffineTransform(matrix);
    }
}
