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

/* $Id: AbstractPathOrientedRenderer.java 746664 2009-02-22 12:40:44Z jeremias $ */

package org.apache.fop.render;

import java.awt.Color;
import java.awt.Rectangle;
import java.awt.geom.AffineTransform;
import java.awt.geom.Rectangle2D;
import java.util.List;
import java.util.Map;

import org.apache.batik.parser.AWTTransformProducer;
import org.apache.fop.area.Area;
import org.apache.fop.area.Block;
import org.apache.fop.area.BlockViewport;
import org.apache.fop.area.CTM;
import org.apache.fop.area.NormalFlow;
import org.apache.fop.area.RegionReference;
import org.apache.fop.area.RegionViewport;
import org.apache.fop.area.Trait;
import org.apache.fop.area.inline.ForeignObject;
import org.apache.fop.area.inline.InlineArea;
import org.apache.fop.area.inline.Viewport;
import org.apache.fop.fo.Constants;
import org.apache.fop.fo.extensions.ExtensionElementMapping;
import org.apache.fop.fonts.FontMetrics;
import org.apache.fop.traits.BorderProps;
import org.apache.xmlgraphics.image.loader.ImageSize;
import org.apache.xmlgraphics.util.QName;
import org.apache.xmlgraphics.util.UnitConv;
import org.w3c.dom.Document;

/**
 * Abstract base class for renderers like PDF and PostScript where many painting
 * operations follow similar patterns which makes it possible to share some
 * code.
 */
public abstract class AbstractPathOrientedRenderer<T> extends PrintRenderer {

    /**
     * Handle block traits. The block could be any sort of block with any
     * positioning so this should render the traits such as border and
     * background in its position.
     *
     * @param block
     *            the block to render the traits
     */
    @Override
    protected void handleBlockTraits(final Block block) {
        final int borderPaddingStart = block.getBorderAndPaddingWidthStart();
        final int borderPaddingBefore = block.getBorderAndPaddingWidthBefore();

        float startx = this.currentIPPosition / 1000f;
        final float starty = this.currentBPPosition / 1000f;
        float width = block.getIPD() / 1000f;
        float height = block.getBPD() / 1000f;

        /*
         * using start-indent now Integer spaceStart = (Integer)
         * block.getTrait(Trait.SPACE_START); if (spaceStart != null) { startx
         * += spaceStart.floatValue() / 1000f; }
         */
        startx += block.getStartIndent() / 1000f;
        startx -= block.getBorderAndPaddingWidthStart() / 1000f;

        width += borderPaddingStart / 1000f;
        width += block.getBorderAndPaddingWidthEnd() / 1000f;
        height += borderPaddingBefore / 1000f;
        height += block.getBorderAndPaddingWidthAfter() / 1000f;

        drawBackAndBorders(block, startx, starty, width, height);
    }

    /**
     * Handle the traits for a region This is used to draw the traits for the
     * given page region. (See Sect. 6.4.1.2 of XSL-FO spec.)
     *
     * @param region
     *            the RegionViewport whose region is to be drawn
     */
    @Override
    protected void handleRegionTraits(final RegionViewport region) {
        final Rectangle2D viewArea = region.getViewArea();
        final RegionReference referenceArea = region.getRegionReference();
        final float startx = (float) (viewArea.getX() / 1000f);
        final float starty = (float) (viewArea.getY() / 1000f);
        final float width = (float) (viewArea.getWidth() / 1000f);
        final float height = (float) (viewArea.getHeight() / 1000f);

        // adjust the current position according to region borders and padding
        this.currentBPPosition = referenceArea.getBorderAndPaddingWidthBefore();
        this.currentIPPosition = referenceArea.getBorderAndPaddingWidthStart();
        // draw background (traits are in the RegionViewport)
        // and borders (traits are in the RegionReference)
        drawBackAndBorders(region, referenceArea, startx, starty, width, height);
    }

    /**
     * Draw the background and borders. This draws the background and border
     * traits for an area given the position.
     *
     * @param area
     *            the area to get the traits from
     * @param startx
     *            the start x position
     * @param starty
     *            the start y position
     * @param width
     *            the width of the area
     * @param height
     *            the height of the area
     */
    protected void drawBackAndBorders(final Area area, final float startx,
            final float starty, final float width, final float height) {
        drawBackAndBorders(area, area, startx, starty, width, height);
    }

    /**
     * Draw the background and borders. This draws the background and border
     * traits for an area given the position.
     *
     * @param backgroundArea
     *            the area to get the background traits from
     * @param borderArea
     *            the area to get the border traits from
     * @param startx
     *            the start x position
     * @param starty
     *            the start y position
     * @param width
     *            the width of the area
     * @param height
     *            the height of the area
     */
    protected void drawBackAndBorders(final Area backgroundArea,
            final Area borderArea, final float startx, final float starty,
            final float width, final float height) {
        // draw background then border

        final BorderProps bpsBefore = (BorderProps) borderArea
                .getTrait(Trait.BORDER_BEFORE);
        final BorderProps bpsAfter = (BorderProps) borderArea
                .getTrait(Trait.BORDER_AFTER);
        final BorderProps bpsStart = (BorderProps) borderArea
                .getTrait(Trait.BORDER_START);
        final BorderProps bpsEnd = (BorderProps) borderArea
                .getTrait(Trait.BORDER_END);

        drawBackground(startx, starty, width, height,
                (Trait.Background) backgroundArea.getTrait(Trait.BACKGROUND),
                bpsBefore, bpsAfter, bpsStart, bpsEnd);
        drawBorders(startx, starty, width, height, bpsBefore, bpsAfter,
                bpsStart, bpsEnd);
    }

    /**
     * Draw the background. This draws the background given the position and the
     * traits.
     *
     * @param startx
     *            the start x position
     * @param starty
     *            the start y position
     * @param width
     *            the width of the area
     * @param height
     *            the height of the area
     * @param back
     *            the background traits
     * @param bpsBefore
     *            the border-before traits
     * @param bpsAfter
     *            the border-after traits
     * @param bpsStart
     *            the border-start traits
     * @param bpsEnd
     *            the border-end traits
     */
    protected void drawBackground(final float startx, final float starty,
            final float width, final float height, final Trait.Background back,
            final BorderProps bpsBefore, final BorderProps bpsAfter,
            final BorderProps bpsStart, final BorderProps bpsEnd) {
        if (back != null) {
            endTextObject();

            // Calculate padding rectangle
            float sx = startx;
            float sy = starty;
            float paddRectWidth = width;
            float paddRectHeight = height;
            if (bpsStart != null) {
                sx += bpsStart.width / 1000f;
                paddRectWidth -= bpsStart.width / 1000f;
            }
            if (bpsBefore != null) {
                sy += bpsBefore.width / 1000f;
                paddRectHeight -= bpsBefore.width / 1000f;
            }
            if (bpsEnd != null) {
                paddRectWidth -= bpsEnd.width / 1000f;
            }
            if (bpsAfter != null) {
                paddRectHeight -= bpsAfter.width / 1000f;
            }

            if (back.getColor() != null) {
                updateColor(back.getColor(), true);
                fillRect(sx, sy, paddRectWidth, paddRectHeight);
            }
            if (back.getImageInfo() != null) {
                final ImageSize imageSize = back.getImageInfo().getSize();
                saveGraphicsState();
                clipRect(sx, sy, paddRectWidth, paddRectHeight);
                int horzCount = (int) (paddRectWidth * 1000
                        / imageSize.getWidthMpt() + 1.0f);
                int vertCount = (int) (paddRectHeight * 1000
                        / imageSize.getHeightMpt() + 1.0f);
                if (back.getRepeat() == EN_NOREPEAT) {
                    horzCount = 1;
                    vertCount = 1;
                } else if (back.getRepeat() == EN_REPEATX) {
                    vertCount = 1;
                } else if (back.getRepeat() == EN_REPEATY) {
                    horzCount = 1;
                }
                // change from points to millipoints
                sx *= 1000;
                sy *= 1000;
                if (horzCount == 1) {
                    sx += back.getHoriz();
                }
                if (vertCount == 1) {
                    sy += back.getVertical();
                }
                for (int x = 0; x < horzCount; x++) {
                    for (int y = 0; y < vertCount; y++) {
                        // place once
                        Rectangle2D pos;
                        // Image positions are relative to the currentIP/BP
                        pos = new Rectangle2D.Float(sx - this.currentIPPosition
                                + x * imageSize.getWidthMpt(), sy
                                - this.currentBPPosition + y
                                * imageSize.getHeightMpt(),
                                imageSize.getWidthMpt(),
                                imageSize.getHeightMpt());
                        drawImage(back.getURL(), pos);
                    }
                }

                restoreGraphicsState();
            }
        }
    }

    /**
     * Draw the borders. This draws the border traits given the position and the
     * traits.
     *
     * @param startx
     *            the start x position
     * @param starty
     *            the start y position
     * @param width
     *            the width of the area
     * @param height
     *            the height of the area
     * @param bpsBefore
     *            the border-before traits
     * @param bpsAfter
     *            the border-after traits
     * @param bpsStart
     *            the border-start traits
     * @param bpsEnd
     *            the border-end traits
     */
    protected void drawBorders(final float startx, final float starty,
            final float width, final float height, final BorderProps bpsBefore,
            final BorderProps bpsAfter, final BorderProps bpsStart,
            final BorderProps bpsEnd) {
        final Rectangle2D.Float borderRect = new Rectangle2D.Float(startx,
                starty, width, height);
        drawBorders(borderRect, bpsBefore, bpsAfter, bpsStart, bpsEnd);
    }

    private static final int BEFORE = 0;
    private static final int END = 1;
    private static final int AFTER = 2;
    private static final int START = 3;

    /**
     * Draws borders.
     *
     * @param borderRect
     *            the border rectangle
     * @param bpsBefore
     *            the border specification on the before side
     * @param bpsAfter
     *            the border specification on the after side
     * @param bpsStart
     *            the border specification on the start side
     * @param bpsEnd
     *            the border specification on the end side
     */
    protected void drawBorders(final Rectangle2D.Float borderRect,
            final BorderProps bpsBefore, final BorderProps bpsAfter,
            final BorderProps bpsStart, final BorderProps bpsEnd) {
        // TODO generalize each of the four conditions into using a
        // parameterized drawBorder()
        final boolean[] border = new boolean[] { bpsBefore != null,
                bpsEnd != null, bpsAfter != null, bpsStart != null };
        float startx = borderRect.x;
        float starty = borderRect.y;
        float width = borderRect.width;
        float height = borderRect.height;
        final float[] borderWidth = new float[] {
                border[BEFORE] ? bpsBefore.width / 1000f : 0.0f,
                        border[END] ? bpsEnd.width / 1000f : 0.0f,
                                border[AFTER] ? bpsAfter.width / 1000f : 0.0f,
                                        border[START] ? bpsStart.width / 1000f : 0.0f };
        final float[] clipw = new float[] {
                BorderProps.getClippedWidth(bpsBefore) / 1000f,
                BorderProps.getClippedWidth(bpsEnd) / 1000f,
                BorderProps.getClippedWidth(bpsAfter) / 1000f,
                BorderProps.getClippedWidth(bpsStart) / 1000f };
        starty += clipw[BEFORE];
        height -= clipw[BEFORE];
        height -= clipw[AFTER];
        startx += clipw[START];
        width -= clipw[START];
        width -= clipw[END];

        final boolean[] slant = new boolean[] {
                border[START] && border[BEFORE], border[BEFORE] && border[END],
                border[END] && border[AFTER], border[AFTER] && border[START] };
        if (bpsBefore != null) {
            endTextObject();

            final float sx1 = startx;
            final float sx2 = slant[BEFORE] ? sx1 + borderWidth[START]
                    - clipw[START] : sx1;
                    final float ex1 = startx + width;
                    final float ex2 = slant[END] ? ex1 - borderWidth[END] + clipw[END]
                            : ex1;
                    final float outery = starty - clipw[BEFORE];
                    final float clipy = outery + clipw[BEFORE];
                    final float innery = outery + borderWidth[BEFORE];

                    saveGraphicsState();
                    moveTo(sx1, clipy);
                    float sx1a = sx1;
                    float ex1a = ex1;
                    if (bpsBefore.mode == BorderProps.COLLAPSE_OUTER) {
                        if (bpsStart != null
                                && bpsStart.mode == BorderProps.COLLAPSE_OUTER) {
                            sx1a -= clipw[START];
                        }
                        if (bpsEnd != null && bpsEnd.mode == BorderProps.COLLAPSE_OUTER) {
                            ex1a += clipw[END];
                        }
                        lineTo(sx1a, outery);
                        lineTo(ex1a, outery);
                    }
                    lineTo(ex1, clipy);
                    lineTo(ex2, innery);
                    lineTo(sx2, innery);
                    closePath();
                    clip();
                    drawBorderLine(sx1a, outery, ex1a, innery, true, true,
                            bpsBefore.style, bpsBefore.color);
                    restoreGraphicsState();
        }
        if (bpsEnd != null) {
            endTextObject();

            final float sy1 = starty;
            final float sy2 = slant[END] ? sy1 + borderWidth[BEFORE]
                    - clipw[BEFORE] : sy1;
                    final float ey1 = starty + height;
                    final float ey2 = slant[AFTER] ? ey1 - borderWidth[AFTER]
                            + clipw[AFTER] : ey1;
                            final float outerx = startx + width + clipw[END];
                            final float clipx = outerx - clipw[END];
                            final float innerx = outerx - borderWidth[END];

                            saveGraphicsState();
                            moveTo(clipx, sy1);
                            float sy1a = sy1;
                            float ey1a = ey1;
                            if (bpsEnd.mode == BorderProps.COLLAPSE_OUTER) {
                                if (bpsBefore != null
                                        && bpsBefore.mode == BorderProps.COLLAPSE_OUTER) {
                                    sy1a -= clipw[BEFORE];
                                }
                                if (bpsAfter != null
                                        && bpsAfter.mode == BorderProps.COLLAPSE_OUTER) {
                                    ey1a += clipw[AFTER];
                                }
                                lineTo(outerx, sy1a);
                                lineTo(outerx, ey1a);
                            }
                            lineTo(clipx, ey1);
                            lineTo(innerx, ey2);
                            lineTo(innerx, sy2);
                            closePath();
                            clip();
                            drawBorderLine(innerx, sy1a, outerx, ey1a, false, false,
                                    bpsEnd.style, bpsEnd.color);
                            restoreGraphicsState();
        }
        if (bpsAfter != null) {
            endTextObject();

            final float sx1 = startx;
            final float sx2 = slant[START] ? sx1 + borderWidth[START]
                    - clipw[START] : sx1;
                    final float ex1 = startx + width;
                    final float ex2 = slant[AFTER] ? ex1 - borderWidth[END]
                            + clipw[END] : ex1;
                            final float outery = starty + height + clipw[AFTER];
                            final float clipy = outery - clipw[AFTER];
                            final float innery = outery - borderWidth[AFTER];

                            saveGraphicsState();
                            moveTo(ex1, clipy);
                            float sx1a = sx1;
                            float ex1a = ex1;
                            if (bpsAfter.mode == BorderProps.COLLAPSE_OUTER) {
                                if (bpsStart != null
                                        && bpsStart.mode == BorderProps.COLLAPSE_OUTER) {
                                    sx1a -= clipw[START];
                                }
                                if (bpsEnd != null && bpsEnd.mode == BorderProps.COLLAPSE_OUTER) {
                                    ex1a += clipw[END];
                                }
                                lineTo(ex1a, outery);
                                lineTo(sx1a, outery);
                            }
                            lineTo(sx1, clipy);
                            lineTo(sx2, innery);
                            lineTo(ex2, innery);
                            closePath();
                            clip();
                            drawBorderLine(sx1a, innery, ex1a, outery, true, false,
                                    bpsAfter.style, bpsAfter.color);
                            restoreGraphicsState();
        }
        if (bpsStart != null) {
            endTextObject();

            final float sy1 = starty;
            final float sy2 = slant[BEFORE] ? sy1 + borderWidth[BEFORE]
                    - clipw[BEFORE] : sy1;
                    final float ey1 = sy1 + height;
                    final float ey2 = slant[START] ? ey1 - borderWidth[AFTER]
                            + clipw[AFTER] : ey1;
                            final float outerx = startx - clipw[START];
                            final float clipx = outerx + clipw[START];
                            final float innerx = outerx + borderWidth[START];

                            saveGraphicsState();
                            moveTo(clipx, ey1);
                            float sy1a = sy1;
                            float ey1a = ey1;
                            if (bpsStart.mode == BorderProps.COLLAPSE_OUTER) {
                                if (bpsBefore != null
                                        && bpsBefore.mode == BorderProps.COLLAPSE_OUTER) {
                                    sy1a -= clipw[BEFORE];
                                }
                                if (bpsAfter != null
                                        && bpsAfter.mode == BorderProps.COLLAPSE_OUTER) {
                                    ey1a += clipw[AFTER];
                                }
                                lineTo(outerx, ey1a);
                                lineTo(outerx, sy1a);
                            }
                            lineTo(clipx, sy1);
                            lineTo(innerx, sy2);
                            lineTo(innerx, ey2);
                            closePath();
                            clip();
                            drawBorderLine(outerx, sy1a, innerx, ey1a, false, true,
                                    bpsStart.style, bpsStart.color);
                            restoreGraphicsState();
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
    @Override
    protected void renderInlineAreaBackAndBorders(final InlineArea area) {
        final float borderPaddingStart = area.getBorderAndPaddingWidthStart() / 1000f;
        final float borderPaddingBefore = area.getBorderAndPaddingWidthBefore() / 1000f;
        final float bpwidth = borderPaddingStart
                + area.getBorderAndPaddingWidthEnd() / 1000f;
        final float bpheight = borderPaddingBefore
                + area.getBorderAndPaddingWidthAfter() / 1000f;

        final float height = area.getBPD() / 1000f;
        if (height != 0.0f || bpheight != 0.0f && bpwidth != 0.0f) {
            final float x = this.currentIPPosition / 1000f;
            final float y = (this.currentBPPosition + area.getOffset()) / 1000f;
            final float width = area.getIPD() / 1000f;
            drawBackAndBorders(area, x, y - borderPaddingBefore, width
                    + bpwidth, height + bpheight);
        }
    }

    /** Constant for the fox:transform extension attribute */
    protected static final QName FOX_TRANSFORM = new QName(
            ExtensionElementMapping.URI, "fox:transform");

    /** {@inheritDoc} */
    @Override
    protected void renderBlockViewport(final BlockViewport bv,
            final List children) {
        // clip and position viewport if necessary

        // save positions
        final int saveIP = this.currentIPPosition;
        final int saveBP = this.currentBPPosition;

        CTM ctm = bv.getCTM();
        final int borderPaddingBefore = bv.getBorderAndPaddingWidthBefore();

        final int positioning = bv.getPositioning();
        if (positioning == Block.ABSOLUTE || positioning == Block.FIXED) {

            // For FIXED, we need to break out of the current viewports to the
            // one established by the page. We save the state stack for
            // restoration
            // after the block-container has been painted. See below.
            List breakOutList = null;
            if (positioning == Block.FIXED) {
                breakOutList = breakOutOfStateStack();
            }

            final AffineTransform positionTransform = new AffineTransform();
            positionTransform.translate(bv.getXOffset(), bv.getYOffset());

            final int borderPaddingStart = bv.getBorderAndPaddingWidthStart();

            // "left/"top" (bv.getX/YOffset()) specify the position of the
            // content rectangle
            positionTransform.translate(-borderPaddingStart,
                    -borderPaddingBefore);

            // Free transformation for the block-container viewport
            String transf;
            transf = bv.getForeignAttributeValue(FOX_TRANSFORM);
            if (transf != null) {
                final AffineTransform freeTransform = AWTTransformProducer
                        .createAffineTransform(transf);
                positionTransform.concatenate(freeTransform);
            }

            // Viewport position
            if (!positionTransform.isIdentity()) {
                establishTransformationMatrix(positionTransform);
            }

            // This is the content-rect
            final float width = bv.getIPD() / 1000f;
            final float height = bv.getBPD() / 1000f;

            // Background and borders
            final float borderPaddingWidth = (borderPaddingStart + bv
                    .getBorderAndPaddingWidthEnd()) / 1000f;
            final float borderPaddingHeight = (borderPaddingBefore + bv
                    .getBorderAndPaddingWidthAfter()) / 1000f;
            drawBackAndBorders(bv, 0, 0, width + borderPaddingWidth, height
                    + borderPaddingHeight);

            // Shift to content rectangle after border painting
            final AffineTransform contentRectTransform = new AffineTransform();
            contentRectTransform.translate(borderPaddingStart,
                    borderPaddingBefore);

            if (!contentRectTransform.isIdentity()) {
                establishTransformationMatrix(contentRectTransform);
            }

            // Clipping
            if (bv.getClip()) {
                clipRect(0f, 0f, width, height);
            }

            // Set up coordinate system for content rectangle
            final AffineTransform contentTransform = ctm.toAffineTransform();
            if (!contentTransform.isIdentity()) {
                establishTransformationMatrix(contentTransform);
            }

            this.currentIPPosition = 0;
            this.currentBPPosition = 0;
            renderBlocks(bv, children);

            if (!contentTransform.isIdentity()) {
                restoreGraphicsState();
            }

            if (!contentRectTransform.isIdentity()) {
                restoreGraphicsState();
            }

            if (!positionTransform.isIdentity()) {
                restoreGraphicsState();
            }

            // For FIXED, we need to restore break out now we are done
            if (positioning == Block.FIXED) {
                if (breakOutList != null) {
                    restoreStateStackAfterBreakOut(breakOutList);
                }
            }

            this.currentIPPosition = saveIP;
            this.currentBPPosition = saveBP;
        } else {

            this.currentBPPosition += bv.getSpaceBefore();

            // borders and background in the old coordinate system
            handleBlockTraits(bv);

            // Advance to start of content area
            this.currentIPPosition += bv.getStartIndent();

            final CTM tempctm = new CTM(this.containingIPPosition,
                    this.currentBPPosition);
            ctm = tempctm.multiply(ctm);

            // Now adjust for border/padding
            this.currentBPPosition += borderPaddingBefore;

            Rectangle2D clippingRect = null;
            if (bv.getClip()) {
                clippingRect = new Rectangle(this.currentIPPosition,
                        this.currentBPPosition, bv.getIPD(), bv.getBPD());
            }

            startVParea(ctm, clippingRect);
            this.currentIPPosition = 0;
            this.currentBPPosition = 0;
            renderBlocks(bv, children);
            endVParea();

            this.currentIPPosition = saveIP;
            this.currentBPPosition = saveBP;

            this.currentBPPosition += bv.getAllocBPD();
        }
    }

    /** {@inheritDoc} */
    @Override
    protected void renderReferenceArea(final Block block) {
        // save position and offset
        final int saveIP = this.currentIPPosition;
        final int saveBP = this.currentBPPosition;

        // Establish a new coordinate system
        final AffineTransform at = new AffineTransform();
        at.translate(this.currentIPPosition, this.currentBPPosition);
        at.translate(block.getXOffset(), block.getYOffset());
        at.translate(0, block.getSpaceBefore());

        if (!at.isIdentity()) {
            establishTransformationMatrix(at);
        }

        this.currentIPPosition = 0;
        this.currentBPPosition = 0;
        handleBlockTraits(block);

        final List<Block> children = block.getChildAreas();
        if (children != null) {
            renderBlocks(block, children);
        }

        if (!at.isIdentity()) {
            restoreGraphicsState();
        }

        // stacked and relative blocks effect stacking
        this.currentIPPosition = saveIP;
        this.currentBPPosition = saveBP;
    }

    /** {@inheritDoc} */
    @Override
    protected void renderFlow(final NormalFlow flow) {
        // save position and offset
        final int saveIP = this.currentIPPosition;
        final int saveBP = this.currentBPPosition;

        // Establish a new coordinate system
        final AffineTransform at = new AffineTransform();
        at.translate(this.currentIPPosition, this.currentBPPosition);

        if (!at.isIdentity()) {
            establishTransformationMatrix(at);
        }

        this.currentIPPosition = 0;
        this.currentBPPosition = 0;
        super.renderFlow(flow);

        if (!at.isIdentity()) {
            restoreGraphicsState();
        }

        // stacked and relative blocks effect stacking
        this.currentIPPosition = saveIP;
        this.currentBPPosition = saveBP;
    }

    /**
     * Concatenates the current transformation matrix with the given one,
     * therefore establishing a new coordinate system.
     *
     * @param at
     *            the transformation matrix to process (coordinates in points)
     */
    protected abstract void concatenateTransformationMatrix(
            final AffineTransform at);

    /**
     * Render an inline viewport. This renders an inline viewport by clipping if
     * necessary.
     *
     * @param viewport
     *            the viewport to handle
     */
    @Override
    public void renderViewport(final Viewport viewport) {

        final float x = this.currentIPPosition / 1000f;
        final float y = (this.currentBPPosition + viewport.getOffset()) / 1000f;
        final float width = viewport.getIPD() / 1000f;
        final float height = viewport.getBPD() / 1000f;
        // TODO: Calculate the border rect correctly.
        final float borderPaddingStart = viewport
                .getBorderAndPaddingWidthStart() / 1000f;
        final float borderPaddingBefore = viewport
                .getBorderAndPaddingWidthBefore() / 1000f;
        final float bpwidth = borderPaddingStart
                + viewport.getBorderAndPaddingWidthEnd() / 1000f;
        final float bpheight = borderPaddingBefore
                + viewport.getBorderAndPaddingWidthAfter() / 1000f;

        drawBackAndBorders(viewport, x, y, width + bpwidth, height + bpheight);

        if (viewport.getClip()) {
            saveGraphicsState();

            clipRect(x + borderPaddingStart, y + borderPaddingBefore, width,
                    height);
        }
        super.renderViewport(viewport);

        if (viewport.getClip()) {
            restoreGraphicsState();
        }
    }

    /**
     * Restores the state stack after a break out.
     *
     * @param breakOutList
     *            the state stack to restore.
     */
    protected abstract void restoreStateStackAfterBreakOut(
            final List<T> breakOutList);

    /**
     * Breaks out of the state stack to handle fixed block-containers.
     *
     * @return the saved state stack to recreate later
     */
    protected abstract List<T> breakOutOfStateStack();

    /** Saves the graphics state of the rendering engine. */
    protected abstract void saveGraphicsState();

    /** Restores the last graphics state of the rendering engine. */
    protected abstract void restoreGraphicsState();

    /** Indicates the beginning of a text object. */
    protected abstract void beginTextObject();

    /** Indicates the end of a text object. */
    protected abstract void endTextObject();

    /**
     * Paints the text decoration marks.
     *
     * @param fm
     *            Current typeface
     * @param fontsize
     *            Current font size
     * @param inline
     *            inline area to paint the marks for
     * @param baseline
     *            position of the baseline
     * @param startx
     *            start IPD
     */
    protected void renderTextDecoration(final FontMetrics fm,
            final int fontsize, final InlineArea inline, final int baseline,
            final int startx) {
        final boolean hasTextDeco = inline.hasUnderline()
                || inline.hasOverline() || inline.hasLineThrough();
        if (hasTextDeco) {
            endTextObject();
            final float descender = fm.getDescender(fontsize) / 1000f;
            final float capHeight = fm.getCapHeight(fontsize) / 1000f;
            final float halfLineWidth = descender / -8f / 2f;
            final float endx = (startx + inline.getIPD()) / 1000f;
            if (inline.hasUnderline()) {
                final Color ct = (Color) inline.getTrait(Trait.UNDERLINE_COLOR);
                final float y = baseline - descender / 2f;
                drawBorderLine(startx / 1000f, (y - halfLineWidth) / 1000f,
                        endx, (y + halfLineWidth) / 1000f, true, true,
                        Constants.EN_SOLID, ct);
            }
            if (inline.hasOverline()) {
                final Color ct = (Color) inline.getTrait(Trait.OVERLINE_COLOR);
                final float y = (float) (baseline - 1.1 * capHeight);
                drawBorderLine(startx / 1000f, (y - halfLineWidth) / 1000f,
                        endx, (y + halfLineWidth) / 1000f, true, true,
                        Constants.EN_SOLID, ct);
            }
            if (inline.hasLineThrough()) {
                final Color ct = (Color) inline
                        .getTrait(Trait.LINETHROUGH_COLOR);
                final float y = (float) (baseline - 0.45 * capHeight);
                drawBorderLine(startx / 1000f, (y - halfLineWidth) / 1000f,
                        endx, (y + halfLineWidth) / 1000f, true, true,
                        Constants.EN_SOLID, ct);
            }
        }
    }

    /** Clip using the current path. */
    protected abstract void clip();

    /**
     * Clip using a rectangular area.
     *
     * @param x
     *            the x coordinate (in points)
     * @param y
     *            the y coordinate (in points)
     * @param width
     *            the width of the rectangle (in points)
     * @param height
     *            the height of the rectangle (in points)
     */
    protected abstract void clipRect(final float x, final float y,
            final float width, final float height);

    /**
     * Moves the current point to (x, y), omitting any connecting line segment.
     *
     * @param x
     *            x coordinate
     * @param y
     *            y coordinate
     */
    protected abstract void moveTo(final float x, final float y);

    /**
     * Appends a straight line segment from the current point to (x, y). The new
     * current point is (x, y).
     *
     * @param x
     *            x coordinate
     * @param y
     *            y coordinate
     */
    protected abstract void lineTo(final float x, final float y);

    /**
     * Closes the current subpath by appending a straight line segment from the
     * current point to the starting point of the subpath.
     */
    protected abstract void closePath();

    /**
     * Fill a rectangular area.
     *
     * @param x
     *            the x coordinate
     * @param y
     *            the y coordinate
     * @param width
     *            the width of the rectangle
     * @param height
     *            the height of the rectangle
     */
    protected abstract void fillRect(final float x, final float y,
            final float width, final float height);

    /**
     * Establishes a new foreground or fill color.
     *
     * @param col
     *            the color to apply (null skips this operation)
     * @param fill
     *            true to set the fill color, false for the foreground color
     */
    protected abstract void updateColor(final Color col, final boolean fill);

    /**
     * Draw an image at the indicated location.
     *
     * @param url
     *            the URI/URL of the image
     * @param pos
     *            the position of the image
     * @param foreignAttributes
     *            an optional Map with foreign attributes, may be null
     */
    protected abstract void drawImage(final String url, final Rectangle2D pos,
            final Map<QName, String> foreignAttributes);

    /**
     * Draw an image at the indicated location.
     *
     * @param url
     *            the URI/URL of the image
     * @param pos
     *            the position of the image
     */
    protected final void drawImage(final String url, final Rectangle2D pos) {
        drawImage(url, pos, null);
    }

    /**
     * Draw a border segment of an XSL-FO style border.
     *
     * @param x1
     *            starting x coordinate
     * @param y1
     *            starting y coordinate
     * @param x2
     *            ending x coordinate
     * @param y2
     *            ending y coordinate
     * @param horz
     *            true for horizontal border segments, false for vertical border
     *            segments
     * @param startOrBefore
     *            true for border segments on the start or before edge, false
     *            for end or after.
     * @param style
     *            the border style (one of Constants.EN_DASHED etc.)
     * @param col
     *            the color for the border segment
     */
    protected abstract void drawBorderLine(final float x1, final float y1,
            final float x2, final float y2, final boolean horz,
            final boolean startOrBefore, final int style, final Color col);

    /** {@inheritDoc} */
    @Override
    public void renderForeignObject(final ForeignObject fo,
            final Rectangle2D pos) {
        endTextObject();
        final Document doc = fo.getDocument();
        final String ns = fo.getNameSpace();
        renderDocument(doc, ns, pos, fo.getForeignAttributes());
    }

    /**
     * Establishes a new coordinate system with the given transformation matrix.
     * The current graphics state is saved and the new coordinate system is
     * concatenated.
     *
     * @param block
     *
     * @param at
     *            the transformation matrix
     */
    protected void establishTransformationMatrix(final AffineTransform at) {
        saveGraphicsState();
        concatenateTransformationMatrix(UnitConv.mptToPt(at));
    }

}
