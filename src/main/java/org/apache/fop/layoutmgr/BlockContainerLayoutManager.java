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

/* $Id: BlockContainerLayoutManager.java 893238 2009-12-22 17:20:51Z vhennebert $ */

package org.apache.fop.layoutmgr;

import java.awt.Point;
import java.awt.geom.Rectangle2D;
import java.util.LinkedList;
import java.util.List;
import java.util.ListIterator;
import java.util.Stack;

import lombok.extern.slf4j.Slf4j;

import org.apache.fop.area.Area;
import org.apache.fop.area.Block;
import org.apache.fop.area.BlockViewport;
import org.apache.fop.area.CTM;
import org.apache.fop.area.Trait;
import org.apache.fop.datatypes.FODimension;
import org.apache.fop.datatypes.Length;
import org.apache.fop.fo.flow.BlockContainer;
import org.apache.fop.fo.properties.CommonAbsolutePosition;
import org.apache.fop.fo.properties.KeepProperty;
import org.apache.fop.traits.MinOptMax;
import org.apache.fop.traits.SpaceVal;
import org.apache.fop.util.ListUtil;

/**
 * LayoutManager for a block-container FO.
 */
@Slf4j
public class BlockContainerLayoutManager extends BlockStackingLayoutManager
implements ConditionalElementListener {

    private BlockViewport viewportBlockArea;
    private Block referenceArea;

    private CommonAbsolutePosition abProps;
    private FODimension relDims;
    private CTM absoluteCTM;
    private Length width;
    private Length height;
    // private int vpContentIPD;
    private int vpContentBPD;

    // When viewport should grow with the content.
    private boolean autoHeight = true;
    private boolean inlineElementList = false;

    /*
     * holds the (one-time use) fo:block space-before and -after properties.
     * Large fo:blocks are split into multiple Area.Blocks to accomodate the
     * subsequent regions (pages) they are placed on. space-before is applied at
     * the beginning of the first Block and space-after at the end of the last
     * Block used in rendering the fo:block.
     */
    // TODO space-before|after: handle space-resolution rules
    private MinOptMax foBlockSpaceBefore;
    private MinOptMax foBlockSpaceAfter;

    private boolean discardBorderBefore;
    private boolean discardBorderAfter;
    private boolean discardPaddingBefore;
    private boolean discardPaddingAfter;
    private MinOptMax effSpaceBefore;
    private MinOptMax effSpaceAfter;

    /**
     * Create a new block container layout manager.
     *
     * @param node
     *            block-container node to create the layout manager for.
     */
    public BlockContainerLayoutManager(final BlockContainer node) {
        super(node);
    }

    /** {@inheritDoc} */
    @Override
    public void initialize() {
        this.abProps = getBlockContainerFO().getCommonAbsolutePosition();
        this.foBlockSpaceBefore = new SpaceVal(getBlockContainerFO()
                .getCommonMarginBlock().spaceBefore, this).getSpace();
        this.foBlockSpaceAfter = new SpaceVal(getBlockContainerFO()
                .getCommonMarginBlock().spaceAfter, this).getSpace();
        this.startIndent = getBlockContainerFO().getCommonMarginBlock().startIndent
                .getValue(this);
        this.endIndent = getBlockContainerFO().getCommonMarginBlock().endIndent
                .getValue(this);

        if (blockProgressionDirectionChanges()) {
            this.height = getBlockContainerFO().getInlineProgressionDimension()
                    .getOptimum(this).getLength();
            this.width = getBlockContainerFO().getBlockProgressionDimension()
                    .getOptimum(this).getLength();
        } else {
            this.height = getBlockContainerFO().getBlockProgressionDimension()
                    .getOptimum(this).getLength();
            this.width = getBlockContainerFO().getInlineProgressionDimension()
                    .getOptimum(this).getLength();
        }

        this.bpUnit = 0; // layoutProps.blockProgressionUnit;
        if (this.bpUnit == 0) {
            // use optimum space values
            this.adjustedSpaceBefore = getBlockContainerFO()
                    .getCommonMarginBlock().spaceBefore.getSpace()
                    .getOptimum(this).getLength().getValue(this);
            this.adjustedSpaceAfter = getBlockContainerFO()
                    .getCommonMarginBlock().spaceAfter.getSpace()
                    .getOptimum(this).getLength().getValue(this);
        } else {
            // use minimum space values
            this.adjustedSpaceBefore = getBlockContainerFO()
                    .getCommonMarginBlock().spaceBefore.getSpace()
                    .getMinimum(this).getLength().getValue(this);
            this.adjustedSpaceAfter = getBlockContainerFO()
                    .getCommonMarginBlock().spaceAfter.getSpace()
                    .getMinimum(this).getLength().getValue(this);
        }
    }

    private void resetSpaces() {
        this.discardBorderBefore = false;
        this.discardBorderAfter = false;
        this.discardPaddingBefore = false;
        this.discardPaddingAfter = false;
        this.effSpaceBefore = null;
        this.effSpaceAfter = null;
    }

    /** @return the content IPD */
    protected int getRotatedIPD() {
        return getBlockContainerFO().getInlineProgressionDimension()
                .getOptimum(this).getLength().getValue(this);
    }

    private boolean needClip() {
        final int overflow = getBlockContainerFO().getOverflow();
        return overflow == EN_HIDDEN || overflow == EN_ERROR_IF_OVERFLOW;
    }

    private int getBPIndents() {
        int indents = 0;
        /*
         * TODO This is wrong isn't it? indents +=
         * getBlockContainerFO().getCommonMarginBlock()
         * .spaceBefore.getOptimum(this).getLength().getValue(this); indents +=
         * getBlockContainerFO().getCommonMarginBlock()
         * .spaceAfter.getOptimum(this).getLength().getValue(this);
         */
        indents += getBlockContainerFO().getCommonBorderPaddingBackground()
                .getBPPaddingAndBorder(false, this);
        return indents;
    }

    private boolean isAbsoluteOrFixed() {
        return this.abProps.absolutePosition == EN_ABSOLUTE
                || this.abProps.absolutePosition == EN_FIXED;
    }

    private boolean isFixed() {
        return this.abProps.absolutePosition == EN_FIXED;
    }

    /** {@inheritDoc} */
    @Override
    public int getContentAreaBPD() {
        if (this.autoHeight) {
            return -1;
        } else {
            return this.vpContentBPD;
        }
    }

    /** {@inheritDoc} */
    @Override
    public List<ListElement> getNextKnuthElements(final LayoutContext context,
            final int alignment) {
        resetSpaces();
        if (isAbsoluteOrFixed()) {
            return getNextKnuthElementsAbsolute(context, alignment);
        }

        this.autoHeight = false;
        // boolean rotated = (getBlockContainerFO().getReferenceOrientation() %
        // 180 != 0);
        final int maxbpd = context.getStackLimitBP().getOpt();
        int allocBPD;
        if (this.height.getEnum() == EN_AUTO || !this.height.isAbsolute()
                && getAncestorBlockAreaBPD() <= 0) {
            // auto height when height="auto" or "if that dimension is not
            // specified explicitly
            // (i.e., it depends on content's block-progression-dimension)" (XSL
            // 1.0, 7.14.1)
            allocBPD = maxbpd;
            this.autoHeight = true;
            if (getBlockContainerFO().getReferenceOrientation() == 0) {
                // Cannot easily inline element list when ref-or="180"
                this.inlineElementList = true;
            }
        } else {
            allocBPD = this.height.getValue(this); // this is the content-height
            allocBPD += getBPIndents();
        }
        this.vpContentBPD = allocBPD - getBPIndents();

        this.referenceIPD = context.getRefIPD();
        if (this.width.getEnum() == EN_AUTO) {
            updateContentAreaIPDwithOverconstrainedAdjust();
        } else {
            final int contentWidth = this.width.getValue(this);
            updateContentAreaIPDwithOverconstrainedAdjust(contentWidth);
        }

        double contentRectOffsetX = 0;
        contentRectOffsetX += getBlockContainerFO().getCommonMarginBlock().startIndent
                .getValue(this);
        double contentRectOffsetY = 0;
        contentRectOffsetY += getBlockContainerFO()
                .getCommonBorderPaddingBackground().getBorderBeforeWidth(false);
        contentRectOffsetY += getBlockContainerFO()
                .getCommonBorderPaddingBackground().getPaddingBefore(false,
                        this);

        updateRelDims(contentRectOffsetX, contentRectOffsetY, this.autoHeight);

        final int availableIPD = this.referenceIPD - getIPIndents();
        if (getContentAreaIPD() > availableIPD) {
            final BlockLevelEventProducer eventProducer = BlockLevelEventProducer.Provider
                    .get(getBlockContainerFO().getUserAgent()
                            .getEventBroadcaster());
            eventProducer.objectTooWide(this, getBlockContainerFO().getName(),
                    getContentAreaIPD(), context.getRefIPD(),
                    getBlockContainerFO().getLocator());
        }

        final MinOptMax stackLimit = MinOptMax.getInstance(this.relDims.bpd);

        List<ListElement> returnedList;
        final List<ListElement> contentList = new LinkedList<>();
        final List<ListElement> returnList = new LinkedList<>();

        if (!this.breakBeforeServed) {
            this.breakBeforeServed = true;
            if (!context.suppressBreakBefore()) {
                if (addKnuthElementsForBreakBefore(returnList, context)) {
                    return returnList;
                }
            }
        }

        if (!this.firstVisibleMarkServed) {
            addKnuthElementsForSpaceBefore(returnList, alignment);
            context.updateKeepWithPreviousPending(getKeepWithPrevious());
        }

        addKnuthElementsForBorderPaddingBefore(returnList,
                !this.firstVisibleMarkServed);
        this.firstVisibleMarkServed = true;

        if (this.autoHeight && this.inlineElementList) {
            // Spaces, border and padding to be repeated at each break
            addPendingMarks(context);

            LayoutManager curLM; // currently active LM
            LayoutManager prevLM = null; // previously active LM
            while ((curLM = getChildLM()) != null) {
                final LayoutContext childLC = new LayoutContext(0);
                childLC.copyPendingMarksFrom(context);
                // curLM is a ?
                childLC.setStackLimitBP(context.getStackLimitBP().minus(
                        stackLimit));
                childLC.setRefIPD(this.relDims.ipd);
                childLC.setWritingMode(getBlockContainerFO().getWritingMode());
                if (curLM == this.childLMs.get(0)) {
                    childLC.setFlags(LayoutContext.SUPPRESS_BREAK_BEFORE);
                    // Handled already by the parent (break collapsing, see
                    // above)
                }

                // get elements from curLM
                returnedList = curLM.getNextKnuthElements(childLC, alignment);
                if (contentList.isEmpty()
                        && childLC.isKeepWithPreviousPending()) {
                    // Propagate keep-with-previous up from the first child
                    context.updateKeepWithPreviousPending(childLC
                            .getKeepWithPreviousPending());
                    childLC.clearKeepWithPreviousPending();
                }
                if (returnedList.size() == 1
                        && returnedList.get(0).isForcedBreak()) {
                    // a descendant of this block has break-before
                    /*
                     * if (returnList.size() == 0) { // the first child (or its
                     * first child ...) has // break-before; // all this block,
                     * including space before, will be put in // the //
                     * following page bSpaceBeforeServed = false; }
                     */
                    contentList.addAll(returnedList);

                    // "wrap" the Position inside each element
                    // moving the elements from contentList to returnList
                    returnedList = new LinkedList<>();
                    wrapPositionElements(contentList, returnList);

                    return returnList;
                } else {
                    if (prevLM != null) {
                        // there is a block handled by prevLM
                        // before the one handled by curLM
                        addInBetweenBreak(contentList, context, childLC);
                    }
                    contentList.addAll(returnedList);
                    if (returnedList.isEmpty()) {
                        // Avoid NoSuchElementException below (happens with
                        // empty blocks)
                        continue;
                    }
                    if (ElementListUtils.endsWithForcedBreak(returnedList)) {
                        // a descendant of this block has break-after
                        if (curLM.isFinished()) {
                            // there is no other content in this block;
                            // it's useless to add space after before a page
                            // break
                            setFinished(true);
                        }

                        returnedList = new LinkedList<>();
                        wrapPositionElements(contentList, returnList);

                        return returnList;
                    }
                }
                // propagate and clear
                context.updateKeepWithNextPending(childLC
                        .getKeepWithNextPending());
                childLC.clearKeepsPending();
                prevLM = curLM;
            }

            returnedList = new LinkedList<>();
            wrapPositionElements(contentList, returnList);

        } else {
            returnList.add(refactoredBecauseOfDuplicateCode(contentRectOffsetX,
                    contentRectOffsetY));
        }
        addKnuthElementsForBorderPaddingAfter(returnList, true);
        addKnuthElementsForSpaceAfter(returnList, alignment);

        // All child content is processed. Only break-after can occur now, so...
        context.clearPendingMarks();
        addKnuthElementsForBreakAfter(returnList, context);

        context.updateKeepWithNextPending(getKeepWithNext());

        setFinished(true);
        return returnList;
    }

    private KnuthBox refactoredBecauseOfDuplicateCode(
            final double contentRectOffsetX, final double contentRectOffsetY) {

        final MinOptMax range = MinOptMax.getInstance(this.relDims.ipd);
        final BlockContainerBreaker breaker = new BlockContainerBreaker(this,
                range);
        breaker.doLayout(this.relDims.bpd, this.autoHeight);
        final boolean contentOverflows = breaker.isOverflow();
        if (this.autoHeight) {
            // Update content BPD now that it is known
            final int newHeight = breaker.deferredAlg.totalWidth;
            if (blockProgressionDirectionChanges()) {
                setContentAreaIPD(newHeight);
            } else {
                this.vpContentBPD = newHeight;
            }
            updateRelDims(contentRectOffsetX, contentRectOffsetY, false);
        }

        final Position bcPosition = new BlockContainerPosition(this, breaker);
        final KnuthBox knuthBox = new KnuthBox(this.vpContentBPD,
                notifyPos(bcPosition), false);
        // TODO Handle min/opt/max for block-progression-dimension
        /*
         * These two elements will be used to add stretchability to the above
         * box returnList.add(new KnuthPenalty(0, KnuthElement.INFINITE, false,
         * returnPosition, false)); returnList.add(new KnuthGlue(0, 1 *
         * constantLineHeight, 0, LINE_NUMBER_ADJUSTMENT, returnPosition,
         * false));
         */

        if (contentOverflows) {
            final BlockLevelEventProducer eventProducer = BlockLevelEventProducer.Provider
                    .get(getBlockContainerFO().getUserAgent()
                            .getEventBroadcaster());
            final boolean canRecover = getBlockContainerFO().getOverflow() != EN_ERROR_IF_OVERFLOW;
            eventProducer.viewportOverflow(this, getBlockContainerFO()
                    .getName(), breaker.getOverflowAmount(), needClip(),
                    canRecover, getBlockContainerFO().getLocator());
        }
        return knuthBox;
    }

    private boolean blockProgressionDirectionChanges() {
        return getBlockContainerFO().getReferenceOrientation() % 180 != 0;
    }

    /** {@inheritDoc} */
    @Override
    public List<ListElement> getNextKnuthElements(final LayoutContext context,
            final int alignment, final Stack<LayoutManager> lmStack,
            final Position restartPosition, final LayoutManager restartAtLM) {
        resetSpaces();
        if (isAbsoluteOrFixed()) {
            return getNextKnuthElementsAbsolute(context, alignment);
        }

        this.autoHeight = false;
        // boolean rotated = (getBlockContainerFO().getReferenceOrientation() %
        // 180 != 0);
        final int maxbpd = context.getStackLimitBP().getOpt();
        int allocBPD;
        if (this.height.getEnum() == EN_AUTO || !this.height.isAbsolute()
                && getAncestorBlockAreaBPD() <= 0) {
            // auto height when height="auto" or "if that dimension is not
            // specified explicitly
            // (i.e., it depends on content's block-progression-dimension)" (XSL
            // 1.0, 7.14.1)
            allocBPD = maxbpd;
            this.autoHeight = true;
            if (getBlockContainerFO().getReferenceOrientation() == 0) {
                // Cannot easily inline element list when ref-or="180"
                this.inlineElementList = true;
            }
        } else {
            allocBPD = this.height.getValue(this); // this is the content-height
            allocBPD += getBPIndents();
        }
        this.vpContentBPD = allocBPD - getBPIndents();

        this.referenceIPD = context.getRefIPD();
        if (this.width.getEnum() == EN_AUTO) {
            updateContentAreaIPDwithOverconstrainedAdjust();
        } else {
            final int contentWidth = this.width.getValue(this);
            updateContentAreaIPDwithOverconstrainedAdjust(contentWidth);
        }

        double contentRectOffsetX = 0;
        contentRectOffsetX += getBlockContainerFO().getCommonMarginBlock().startIndent
                .getValue(this);
        double contentRectOffsetY = 0;
        contentRectOffsetY += getBlockContainerFO()
                .getCommonBorderPaddingBackground().getBorderBeforeWidth(false);
        contentRectOffsetY += getBlockContainerFO()
                .getCommonBorderPaddingBackground().getPaddingBefore(false,
                        this);

        updateRelDims(contentRectOffsetX, contentRectOffsetY, this.autoHeight);

        final int availableIPD = this.referenceIPD - getIPIndents();
        if (getContentAreaIPD() > availableIPD) {
            final BlockLevelEventProducer eventProducer = BlockLevelEventProducer.Provider
                    .get(getBlockContainerFO().getUserAgent()
                            .getEventBroadcaster());
            eventProducer.objectTooWide(this, getBlockContainerFO().getName(),
                    getContentAreaIPD(), context.getRefIPD(),
                    getBlockContainerFO().getLocator());
        }

        final MinOptMax stackLimit = MinOptMax.getInstance(this.relDims.bpd);

        List<ListElement> returnedList;
        final List<ListElement> contentList = new LinkedList<>();
        final List<ListElement> returnList = new LinkedList<>();

        if (!this.breakBeforeServed) {
            this.breakBeforeServed = true;
            if (!context.suppressBreakBefore()) {
                if (addKnuthElementsForBreakBefore(returnList, context)) {
                    return returnList;
                }
            }
        }

        if (!this.firstVisibleMarkServed) {
            addKnuthElementsForSpaceBefore(returnList, alignment);
            context.updateKeepWithPreviousPending(getKeepWithPrevious());
        }

        addKnuthElementsForBorderPaddingBefore(returnList,
                !this.firstVisibleMarkServed);
        this.firstVisibleMarkServed = true;

        if (this.autoHeight && this.inlineElementList) {
            // Spaces, border and padding to be repeated at each break
            addPendingMarks(context);

            BlockLevelLayoutManager curLM; // currently active LM
            BlockLevelLayoutManager prevLM = null; // previously active LM

            LayoutContext childLC = new LayoutContext(0);
            if (lmStack.isEmpty()) {
                assert restartAtLM != null && restartAtLM.getParent() == this;
                curLM = (BlockLevelLayoutManager) restartAtLM;
                curLM.reset();
                setCurrentChildLM(curLM);

                childLC.copyPendingMarksFrom(context);
                childLC.setStackLimitBP(context.getStackLimitBP().minus(
                        stackLimit));
                childLC.setRefIPD(this.relDims.ipd);
                childLC.setWritingMode(getBlockContainerFO().getWritingMode());
                if (curLM == this.childLMs.get(0)) {
                    childLC.setFlags(LayoutContext.SUPPRESS_BREAK_BEFORE);
                    // Handled already by the parent (break collapsing, see
                    // above)
                }

                // get elements from curLM
                returnedList = curLM.getNextKnuthElements(childLC, alignment);
            } else {
                curLM = (BlockLevelLayoutManager) lmStack.pop();
                setCurrentChildLM(curLM);

                childLC.copyPendingMarksFrom(context);
                childLC.setStackLimitBP(context.getStackLimitBP().minus(
                        stackLimit));
                childLC.setRefIPD(this.relDims.ipd);
                childLC.setWritingMode(getBlockContainerFO().getWritingMode());
                if (curLM == this.childLMs.get(0)) {
                    childLC.setFlags(LayoutContext.SUPPRESS_BREAK_BEFORE);
                    // Handled already by the parent (break collapsing, see
                    // above)
                }

                // get elements from curLM
                returnedList = curLM.getNextKnuthElements(childLC, alignment,
                        lmStack, restartPosition, restartAtLM);
            }
            if (contentList.isEmpty() && childLC.isKeepWithPreviousPending()) {
                // Propagate keep-with-previous up from the first child
                context.updateKeepWithPreviousPending(childLC
                        .getKeepWithPreviousPending());
                childLC.clearKeepWithPreviousPending();
            }
            if (returnedList.size() == 1 && returnedList.get(0).isForcedBreak()) {
                // a descendant of this block has break-before
                /*
                 * if (returnList.size() == 0) { // the first child (or its
                 * first child ...) has // break-before; // all this block,
                 * including space before, will be put in // the // following
                 * page bSpaceBeforeServed = false; }
                 */
                contentList.addAll(returnedList);

                // "wrap" the Position inside each element
                // moving the elements from contentList to returnList
                returnedList = new LinkedList<>();
                wrapPositionElements(contentList, returnList);

                return returnList;
            } else {
                if (prevLM != null) {
                    // there is a block handled by prevLM
                    // before the one handled by curLM
                    addInBetweenBreak(contentList, context, childLC);
                }
                contentList.addAll(returnedList);
                if (!returnedList.isEmpty()) {
                    if (ListUtil.getLast(returnedList).isForcedBreak()) {
                        // a descendant of this block has break-after
                        if (curLM.isFinished()) {
                            // there is no other content in this block;
                            // it's useless to add space after before a page
                            // break
                            setFinished(true);
                        }

                        returnedList = new LinkedList<>();
                        wrapPositionElements(contentList, returnList);

                        return returnList;
                    }
                }
            }
            // propagate and clear
            context.updateKeepWithNextPending(childLC.getKeepWithNextPending());
            childLC.clearKeepsPending();
            prevLM = curLM;

            while ((curLM = (BlockLevelLayoutManager) getChildLM()) != null) {
                curLM.reset();
                childLC = new LayoutContext(0);
                childLC.copyPendingMarksFrom(context);
                // curLM is a ?
                childLC.setStackLimitBP(context.getStackLimitBP().minus(
                        stackLimit));
                childLC.setRefIPD(this.relDims.ipd);
                childLC.setWritingMode(getBlockContainerFO().getWritingMode());
                if (curLM == this.childLMs.get(0)) {
                    childLC.setFlags(LayoutContext.SUPPRESS_BREAK_BEFORE);
                    // Handled already by the parent (break collapsing, see
                    // above)
                }

                // get elements from curLM
                returnedList = curLM.getNextKnuthElements(childLC, alignment);
                if (contentList.isEmpty()
                        && childLC.isKeepWithPreviousPending()) {
                    // Propagate keep-with-previous up from the first child
                    context.updateKeepWithPreviousPending(childLC
                            .getKeepWithPreviousPending());
                    childLC.clearKeepWithPreviousPending();
                }
                if (returnedList.size() == 1
                        && returnedList.get(0).isForcedBreak()) {
                    // a descendant of this block has break-before
                    /*
                     * if (returnList.size() == 0) { // the first child (or its
                     * first child ...) has // break-before; // all this block,
                     * including space before, will be put in // the //
                     * following page bSpaceBeforeServed = false; }
                     */
                    contentList.addAll(returnedList);

                    // "wrap" the Position inside each element
                    // moving the elements from contentList to returnList
                    returnedList = new LinkedList<>();
                    wrapPositionElements(contentList, returnList);

                    return returnList;
                } else {
                    if (prevLM != null) {
                        // there is a block handled by prevLM
                        // before the one handled by curLM
                        addInBetweenBreak(contentList, context, childLC);
                    }
                    contentList.addAll(returnedList);
                    if (returnedList.isEmpty()) {
                        // Avoid NoSuchElementException below (happens with
                        // empty blocks)
                        continue;
                    }
                    if (ListUtil.getLast(returnedList).isForcedBreak()) {
                        // a descendant of this block has break-after
                        if (curLM.isFinished()) {
                            // there is no other content in this block;
                            // it's useless to add space after before a page
                            // break
                            setFinished(true);
                        }

                        returnedList = new LinkedList<>();
                        wrapPositionElements(contentList, returnList);

                        return returnList;
                    }
                }
                // propagate and clear
                context.updateKeepWithNextPending(childLC
                        .getKeepWithNextPending());
                childLC.clearKeepsPending();
                prevLM = curLM;
            }

            returnedList = new LinkedList<>();
            wrapPositionElements(contentList, returnList);

        } else {
            returnList.add(refactoredBecauseOfDuplicateCode(contentRectOffsetX,
                    contentRectOffsetY));
        }
        addKnuthElementsForBorderPaddingAfter(returnList, true);
        addKnuthElementsForSpaceAfter(returnList, alignment);

        // All child content is processed. Only break-after can occur now, so...
        context.clearPendingMarks();
        addKnuthElementsForBreakAfter(returnList, context);

        context.updateKeepWithNextPending(getKeepWithNext());

        setFinished(true);
        return returnList;
    }

    /** {@inheritDoc} */
    @Override
    public boolean isRestartable() {
        return true;
    }

    private List<ListElement> getNextKnuthElementsAbsolute(
            final LayoutContext context, final int alignment) {
        this.autoHeight = false;

        final boolean bpDirectionChanges = blockProgressionDirectionChanges();
        final Point offset = getAbsOffset();
        int allocBPD, allocIPD;
        if (this.height.getEnum() == EN_AUTO || !this.height.isAbsolute()
                && getAncestorBlockAreaBPD() <= 0) {
            // auto height when height="auto" or "if that dimension is not
            // specified explicitly
            // (i.e., it depends on content's blockprogression-dimension)" (XSL
            // 1.0, 7.14.1)
            allocBPD = 0;
            if (this.abProps.bottom.getEnum() != EN_AUTO) {
                int availHeight;
                if (isFixed()) {
                    availHeight = (int) getCurrentPV().getViewArea()
                            .getHeight();
                } else {
                    availHeight = context.getStackLimitBP().getOpt();
                }
                allocBPD = availHeight;
                allocBPD -= offset.y;
                if (this.abProps.bottom.getEnum() != EN_AUTO) {
                    allocBPD -= this.abProps.bottom.getValue(this);
                    if (allocBPD < 0) {
                        // TODO Fix absolute b-c layout, layout may need to be
                        // defferred until
                        // after page breaking when the size of the containing
                        // box is known.
                        /*
                         * Warning disabled due to a interpretation mistake.
                         * See:
                         * http://marc.theaimsgroup.com/?l=fop-dev&m=113189981926163
                         * &w=2 log.error(
                         * "The current combination of top and bottom properties results"
                         * +
                         * " in a negative extent for the block-container. 'bottom' may be"
                         * + " at most " + (allocBPD +
                         * abProps.bottom.getValue(this)) + " mpt," +
                         * " but was actually " + abProps.bottom.getValue(this)
                         * + " mpt." + " The nominal available height is " +
                         * availHeight + " mpt.");
                         */
                        allocBPD = 0;
                    }
                } else {
                    if (allocBPD < 0) {
                        /*
                         * Warning disabled due to a interpretation mistake.
                         * See:
                         * http://marc.theaimsgroup.com/?l=fop-dev&m=113189981926163
                         * &w=2 log.error(
                         * "The current combination of top and bottom properties results"
                         * +
                         * " in a negative extent for the block-container. 'top' may be"
                         * + " at most " + availHeight + " mpt," +
                         * " but was actually " + offset.y + " mpt." +
                         * " The nominal available height is " + availHeight +
                         * " mpt.");
                         */
                        allocBPD = 0;
                    }
                }
            } else {
                allocBPD = context.getStackLimitBP().getOpt();
                if (!bpDirectionChanges) {
                    this.autoHeight = true;
                }
            }
        } else {
            allocBPD = this.height.getValue(this); // this is the content-height
            allocBPD += getBPIndents();
        }
        if (this.width.getEnum() == EN_AUTO) {
            int availWidth;
            if (isFixed()) {
                availWidth = (int) getCurrentPV().getViewArea().getWidth();
            } else {
                availWidth = context.getRefIPD();
            }
            allocIPD = availWidth;
            if (this.abProps.left.getEnum() != EN_AUTO) {
                allocIPD -= this.abProps.left.getValue(this);
            }
            if (this.abProps.right.getEnum() != EN_AUTO) {
                allocIPD -= this.abProps.right.getValue(this);
                if (allocIPD < 0) {
                    /*
                     * Warning disabled due to a interpretation mistake. See:
                     * http
                     * ://marc.theaimsgroup.com/?l=fop-dev&m=113189981926163&w=2
                     * log.error(
                     * "The current combination of left and right properties results"
                     * +
                     * " in a negative extent for the block-container. 'right' may be"
                     * + " at most " + (allocIPD + abProps.right.getValue(this))
                     * + " mpt," + " but was actually " +
                     * abProps.right.getValue(this) + " mpt." +
                     * " The nominal available width is " + availWidth +
                     * " mpt.");
                     */
                    allocIPD = 0;
                }
            } else {
                if (allocIPD < 0) {
                    /*
                     * Warning disabled due to a interpretation mistake. See:
                     * http
                     * ://marc.theaimsgroup.com/?l=fop-dev&m=113189981926163&w=2
                     * log.error(
                     * "The current combination of left and right properties results"
                     * +
                     * " in a negative extent for the block-container. 'left' may be"
                     * + " at most " + allocIPD + " mpt," + " but was actually "
                     * + abProps.left.getValue(this) + " mpt." +
                     * " The nominal available width is " + availWidth +
                     * " mpt.");
                     */
                    allocIPD = 0;
                }
                if (bpDirectionChanges) {
                    this.autoHeight = true;
                }
            }
        } else {
            allocIPD = this.width.getValue(this); // this is the content-width
            allocIPD += getIPIndents();
        }

        this.vpContentBPD = allocBPD - getBPIndents();
        setContentAreaIPD(allocIPD - getIPIndents());

        updateRelDims(0, 0, this.autoHeight);

        final MinOptMax range = MinOptMax.getInstance(this.relDims.ipd);
        final BlockContainerBreaker breaker = new BlockContainerBreaker(this,
                range);
        breaker.doLayout(this.autoHeight ? 0 : this.relDims.bpd,
                this.autoHeight);
        final boolean contentOverflows = breaker.isOverflow();
        if (this.autoHeight) {
            // Update content BPD now that it is known
            final int newHeight = breaker.deferredAlg.totalWidth;
            if (bpDirectionChanges) {
                setContentAreaIPD(newHeight);
            } else {
                this.vpContentBPD = newHeight;
            }
            updateRelDims(0, 0, false);
        }
        final List<ListElement> returnList = new LinkedList<>();
        if (!breaker.isEmpty()) {
            final Position bcPosition = new BlockContainerPosition(this,
                    breaker);
            returnList.add(new KnuthBox(0, notifyPos(bcPosition), false));

            // TODO Maybe check for page overflow when autoHeight=true
            if (!this.autoHeight & contentOverflows) {
                final BlockLevelEventProducer eventProducer = BlockLevelEventProducer.Provider
                        .get(getBlockContainerFO().getUserAgent()
                                .getEventBroadcaster());
                final boolean canRecover = getBlockContainerFO().getOverflow() != EN_ERROR_IF_OVERFLOW;
                eventProducer.viewportOverflow(this, getBlockContainerFO()
                        .getName(), breaker.getOverflowAmount(), needClip(),
                        canRecover, getBlockContainerFO().getLocator());
            }
        }

        setFinished(true);
        return returnList;
    }

    private void updateRelDims(final double xOffset, final double yOffset,
            final boolean skipAutoHeight) {
        final Rectangle2D rect = new Rectangle2D.Double(xOffset, yOffset,
                getContentAreaIPD(), this.vpContentBPD);
        this.relDims = new FODimension(0, 0);
        this.absoluteCTM = CTM.getCTMandRelDims(getBlockContainerFO()
                .getReferenceOrientation(), getBlockContainerFO()
                .getWritingMode(), rect, this.relDims);
    }

    private class BlockContainerPosition extends NonLeafPosition {

        private final BlockContainerBreaker breaker;

        public BlockContainerPosition(final LayoutManager lm,
                final BlockContainerBreaker breaker) {
            super(lm, null);
            this.breaker = breaker;
        }

        public BlockContainerBreaker getBreaker() {
            return this.breaker;
        }

    }

    private class BlockContainerBreaker extends AbstractBreaker {

        private final BlockContainerLayoutManager bclm;
        private final MinOptMax ipd;

        // Info for deferred adding of areas
        private PageBreakingAlgorithm deferredAlg;
        private BlockSequence deferredOriginalList;
        private BlockSequence deferredEffectiveList;

        public BlockContainerBreaker(final BlockContainerLayoutManager bclm,
                final MinOptMax ipd) {
            this.bclm = bclm;
            this.ipd = ipd;
        }

        /** {@inheritDoc} */
        @Override
        protected void observeElementList(final List<ListElement> elementList) {
            ElementListObserver.observe(elementList, "block-container",
                    this.bclm.getBlockContainerFO().getId());
        }

        /** {@inheritDoc} */
        @Override
        protected boolean isPartOverflowRecoveryActivated() {
            // For block-containers, this must be disabled because of wanted
            // overflow.
            return false;
        }

        /** {@inheritDoc} */
        @Override
        protected boolean isSinglePartFavored() {
            return true;
        }

        public int getDifferenceOfFirstPart() {
            final PageBreakPosition pbp = this.deferredAlg.getPageBreaks()
                    .getFirst();
            return pbp.difference;
        }

        public boolean isOverflow() {
            return !isEmpty()
                    && (this.deferredAlg.getPageBreaks().size() > 1 || this.deferredAlg.totalWidth
                            - this.deferredAlg.totalShrink > this.deferredAlg
                            .getLineWidth());
        }

        public int getOverflowAmount() {
            return this.deferredAlg.totalWidth - this.deferredAlg.totalShrink
                    - this.deferredAlg.getLineWidth();
        }

        @Override
        protected LayoutManager getTopLevelLM() {
            return this.bclm;
        }

        @Override
        protected LayoutContext createLayoutContext() {
            final LayoutContext lc = super.createLayoutContext();
            lc.setRefIPD(this.ipd.getOpt());
            lc.setWritingMode(getBlockContainerFO().getWritingMode());
            return lc;
        }

        @Override
        protected List<ListElement> getNextKnuthElements(
                final LayoutContext context, final int alignment) {
            LayoutManager curLM; // currently active LM
            final List<ListElement> returnList = new LinkedList<>();

            while ((curLM = getChildLM()) != null) {
                final LayoutContext childLC = new LayoutContext(0);
                childLC.setStackLimitBP(context.getStackLimitBP());
                childLC.setRefIPD(context.getRefIPD());
                childLC.setWritingMode(getBlockContainerFO().getWritingMode());

                List<ListElement> returnedList = null;
                if (!curLM.isFinished()) {
                    returnedList = curLM.getNextKnuthElements(childLC,
                            alignment);
                }
                if (returnedList != null) {
                    this.bclm.wrapPositionElements(returnedList, returnList);
                }
            }
            SpaceResolver.resolveElementList(returnList);
            setFinished(true);
            return returnList;
        }

        @Override
        protected int getCurrentDisplayAlign() {
            return getBlockContainerFO().getDisplayAlign();
        }

        @Override
        protected boolean hasMoreContent() {
            return !isFinished();
        }

        @Override
        protected void addAreas(final PositionIterator posIter,
                final LayoutContext context) {
            AreaAdditionUtil.addAreas(this.bclm, posIter, context);
        }

        @Override
        protected void doPhase3(final PageBreakingAlgorithm alg,
                final int partCount, final BlockSequence originalList,
                final BlockSequence effectiveList) {
            // Defer adding of areas until addAreas is called by the parent LM
            this.deferredAlg = alg;
            this.deferredOriginalList = originalList;
            this.deferredEffectiveList = effectiveList;
        }

        @Override
        protected void finishPart(final PageBreakingAlgorithm alg,
                final PageBreakPosition pbp) {
            // nop for bclm
        }

        @Override
        protected LayoutManager getCurrentChildLM() {
            return BlockContainerLayoutManager.this.curChildLM;
        }

        public void addContainedAreas() {
            if (isEmpty()) {
                return;
            }
            // Rendering all parts (not just the first) at once for the case
            // where the parts that
            // overflow should be visible.
            this.deferredAlg.removeAllPageBreaks();
            this.addAreas(this.deferredAlg, this.deferredAlg.getPageBreaks()
                    .size(), this.deferredOriginalList,
                    this.deferredEffectiveList);
        }

    }

    private Point getAbsOffset() {
        int x = 0;
        int y = 0;
        if (this.abProps.left.getEnum() != EN_AUTO) {
            x = this.abProps.left.getValue(this);
        } else if (this.abProps.right.getEnum() != EN_AUTO
                && this.width.getEnum() != EN_AUTO) {
            x = getReferenceAreaIPD() - this.abProps.right.getValue(this)
                    - this.width.getValue(this);
        }
        if (this.abProps.top.getEnum() != EN_AUTO) {
            y = this.abProps.top.getValue(this);
        } else if (this.abProps.bottom.getEnum() != EN_AUTO
                && this.height.getEnum() != EN_AUTO) {
            y = getReferenceAreaBPD() - this.abProps.bottom.getValue(this)
                    - this.height.getValue(this);
        }
        return new Point(x, y);
    }

    /** {@inheritDoc} */
    @Override
    public void addAreas(final PositionIterator parentIter,
            final LayoutContext layoutContext) {
        getParentArea(null);

        // if this will create the first block area in a page
        // and display-align is bottom or center, add space before
        if (layoutContext.getSpaceBefore() > 0) {
            addBlockSpacing(0.0,
                    MinOptMax.getInstance(layoutContext.getSpaceBefore()));
        }

        LayoutManager childLM;
        LayoutManager lastLM = null;
        final LayoutContext lc = new LayoutContext(0);
        lc.setSpaceAdjust(layoutContext.getSpaceAdjust());
        // set space after in the LayoutContext for children
        if (layoutContext.getSpaceAfter() > 0) {
            lc.setSpaceAfter(layoutContext.getSpaceAfter());
        }
        BlockContainerPosition bcpos = null;
        PositionIterator childPosIter;

        // "unwrap" the NonLeafPositions stored in parentIter
        // and put them in a new list;
        final List<Position> positionList = new LinkedList<>();
        Position pos;
        boolean bSpaceBefore = false;
        boolean bSpaceAfter = false;
        Position firstPos = null;
        Position lastPos = null;
        while (parentIter.hasNext()) {
            pos = parentIter.next();
            if (pos.getIndex() >= 0) {
                if (firstPos == null) {
                    firstPos = pos;
                }
                lastPos = pos;
            }
            Position innerPosition = pos;
            if (pos instanceof NonLeafPosition) {
                innerPosition = pos.getPosition();
            }
            if (pos instanceof BlockContainerPosition) {
                if (bcpos != null) {
                    throw new IllegalStateException(
                            "Only one BlockContainerPosition allowed");
                }
                bcpos = (BlockContainerPosition) pos;
                // Add child areas inside the reference area
                // bcpos.getBreaker().addContainedAreas();
            } else if (innerPosition == null) {
                if (pos instanceof NonLeafPosition) {
                    // pos was created by this BCLM and was inside an element
                    // representing space before or after
                    // this means the space was not discarded
                    if (positionList.isEmpty() && bcpos == null) {
                        // pos was in the element representing space-before
                        bSpaceBefore = true;
                    } else {
                        // pos was in the element representing space-after
                        bSpaceAfter = true;
                    }
                } else {
                    // ignore (probably a Position for a simple penalty between
                    // blocks)
                }
            } else if (innerPosition.getLM() == this
                    && !(innerPosition instanceof MappingPosition)) {
                // pos was created by this BlockLM and was inside a penalty
                // allowing or forbidding a page break
                // nothing to do
            } else {
                // innerPosition was created by another LM
                positionList.add(innerPosition);
                lastLM = innerPosition.getLM();
            }
        }

        addId();

        addMarkersToPage(true, isFirst(firstPos), isLast(lastPos));

        if (bcpos == null) {
            if (this.bpUnit == 0) {
                // the Positions in positionList were inside the elements
                // created by the LineLM
                childPosIter = new StackingIter(positionList.listIterator());
            } else {
                // the Positions in positionList were inside the elements
                // created by the BCLM in the createUnitElements() method
                // if (((Position) positionList.getLast()) instanceof
                // LeafPosition) {
                // // the last item inside positionList is a LeafPosition
                // // (a LineBreakPosition, more precisely); this means that
                // // the whole paragraph is on the same page
                // childPosIter = new KnuthPossPosIter(storedList, 0,
                // storedList.size());
                // } else {
                // // the last item inside positionList is a Position;
                // // this means that the paragraph has been split
                // // between consecutive pages
                final List<ListElement> splitList = new LinkedList<>();
                int splitLength = 0;
                final int iFirst = ((MappingPosition) positionList.get(0))
                        .getFirstIndex();
                final int iLast = ((MappingPosition) ListUtil
                        .getLast(positionList)).getLastIndex();
                // copy from storedList to splitList all the elements from
                // iFirst to iLast
                final ListIterator<ListElement> storedListIterator = this.storedList
                        .listIterator(iFirst);
                while (storedListIterator.nextIndex() <= iLast) {
                    final KnuthElement element = (KnuthElement) storedListIterator
                            .next();
                    // some elements in storedList (i.e. penalty items) were
                    // created
                    // by this BlockLM, and must be ignored
                    if (element.getLayoutManager() != this) {
                        splitList.add(element);
                        splitLength += element.getWidth();
                        lastLM = element.getLayoutManager();
                    }
                }
                // log.debug("Adding areas from " + iFirst + " to " + iLast);
                // log.debug("splitLength= " + splitLength
                // + " (" + neededUnits(splitLength) + " units') "
                // + (neededUnits(splitLength) * bpUnit - splitLength)
                // + " spacing");
                // add space before and / or after the paragraph
                // to reach a multiple of bpUnit
                if (bSpaceBefore && bSpaceAfter) {
                    this.foBlockSpaceBefore = new SpaceVal(
                            getBlockContainerFO().getCommonMarginBlock().spaceBefore,
                            this).getSpace();
                    this.foBlockSpaceAfter = new SpaceVal(getBlockContainerFO()
                            .getCommonMarginBlock().spaceAfter, this)
                    .getSpace();
                    this.adjustedSpaceBefore = (neededUnits(splitLength
                            + this.foBlockSpaceBefore.getMin()
                            + this.foBlockSpaceAfter.getMin())
                            * this.bpUnit - splitLength) / 2;
                    this.adjustedSpaceAfter = neededUnits(splitLength
                            + this.foBlockSpaceBefore.getMin()
                            + this.foBlockSpaceAfter.getMin())
                            * this.bpUnit
                            - splitLength
                            - this.adjustedSpaceBefore;
                } else if (bSpaceBefore) {
                    this.adjustedSpaceBefore = neededUnits(splitLength
                            + this.foBlockSpaceBefore.getMin())
                            * this.bpUnit - splitLength;
                } else {
                    this.adjustedSpaceAfter = neededUnits(splitLength
                            + this.foBlockSpaceAfter.getMin())
                            * this.bpUnit - splitLength;
                }
                // log.debug("space before = " + adjustedSpaceBefore
                // + " space after = " + adjustedSpaceAfter + " total = " +
                // (adjustedSpaceBefore + adjustedSpaceAfter + splitLength));
                childPosIter = new KnuthPossPosIter(splitList, 0,
                        splitList.size());
                // }
            }

            while ((childLM = childPosIter.getNextChildLM()) != null) {
                // set last area flag
                lc.setFlags(LayoutContext.LAST_AREA, layoutContext.isLastArea()
                        && childLM == lastLM);
                /* LF */lc.setStackLimitBP(layoutContext.getStackLimitBP());
                // Add the line areas to Area
                childLM.addAreas(childPosIter, lc);
            }
        } else {
            // Add child areas inside the reference area
            bcpos.getBreaker().addContainedAreas();
        }

        addMarkersToPage(false, isFirst(firstPos), isLast(lastPos));

        TraitSetter.addSpaceBeforeAfter(this.viewportBlockArea,
                layoutContext.getSpaceAdjust(), this.effSpaceBefore,
                this.effSpaceAfter);
        flush();

        this.viewportBlockArea = null;
        this.referenceArea = null;
        resetSpaces();

        notifyEndOfLayout();
    }

    /**
     * Get the parent area for children of this block container. This returns
     * the current block container area and creates it if required.
     *
     * {@inheritDoc}
     */
    @Override
    public Area getParentArea(final Area childArea) {
        if (this.referenceArea == null) {
            final boolean switchedProgressionDirection = blockProgressionDirectionChanges();
            final boolean allowBPDUpdate = this.autoHeight
                    && !switchedProgressionDirection;

            this.viewportBlockArea = new BlockViewport(allowBPDUpdate);
            this.viewportBlockArea.addTrait(Trait.IS_VIEWPORT_AREA,
                    Boolean.TRUE);

            this.viewportBlockArea.setIPD(getContentAreaIPD());
            if (allowBPDUpdate) {
                this.viewportBlockArea.setBPD(0);
            } else {
                this.viewportBlockArea.setBPD(this.vpContentBPD);
            }
            transferForeignAttributes(this.viewportBlockArea);

            TraitSetter.setProducerID(this.viewportBlockArea,
                    getBlockContainerFO().getId());
            TraitSetter.addBorders(this.viewportBlockArea,
                    getBlockContainerFO().getCommonBorderPaddingBackground(),
                    this.discardBorderBefore, this.discardBorderAfter, false,
                    false, this);
            TraitSetter.addPadding(this.viewportBlockArea,
                    getBlockContainerFO().getCommonBorderPaddingBackground(),
                    this.discardPaddingBefore, this.discardPaddingAfter, false,
                    false, this);
            // TraitSetter.addBackground(viewportBlockArea,
            // getBlockContainerFO().getCommonBorderPaddingBackground(),
            // this);
            TraitSetter.addMargins(this.viewportBlockArea,
                    getBlockContainerFO().getCommonBorderPaddingBackground(),
                    this.startIndent, this.endIndent, this);

            this.viewportBlockArea.setCTM(this.absoluteCTM);
            this.viewportBlockArea.setClip(needClip());
            /*
             * if (getSpaceBefore() != 0) {
             * viewportBlockArea.addTrait(Trait.SPACE_BEFORE, new
             * Integer(getSpaceBefore())); } if (foBlockSpaceAfter.opt != 0) {
             * viewportBlockArea.addTrait(Trait.SPACE_AFTER, new
             * Integer(foBlockSpaceAfter.opt)); }
             */

            if (this.abProps.absolutePosition == EN_ABSOLUTE
                    || this.abProps.absolutePosition == EN_FIXED) {
                final Point offset = getAbsOffset();
                this.viewportBlockArea.setXOffset(offset.x);
                this.viewportBlockArea.setYOffset(offset.y);
            } else {
                // nop
            }

            this.referenceArea = new Block();
            this.referenceArea.addTrait(Trait.IS_REFERENCE_AREA, Boolean.TRUE);
            TraitSetter.setProducerID(this.referenceArea, getBlockContainerFO()
                    .getId());

            if (this.abProps.absolutePosition == EN_ABSOLUTE) {
                this.viewportBlockArea.setPositioning(Block.ABSOLUTE);
            } else if (this.abProps.absolutePosition == EN_FIXED) {
                this.viewportBlockArea.setPositioning(Block.FIXED);
            }

            // Set up dimensions
            // Must get dimensions from parent area
            /* Area parentArea = */this.parentLayoutManager
            .getParentArea(this.referenceArea);
            // int referenceIPD = parentArea.getIPD();
            this.referenceArea.setIPD(this.relDims.ipd);
            // Get reference IPD from parentArea
            setCurrentArea(this.viewportBlockArea); // ??? for generic
            // operations
        }
        return this.referenceArea;
    }

    /**
     * Add the child to the block container.
     *
     * {@inheritDoc}
     */
    @Override
    public void addChildArea(final Area childArea) {
        if (this.referenceArea != null) {
            this.referenceArea.addBlock((Block) childArea);
        }
    }

    /**
     * Force current area to be added to parent area. {@inheritDoc}
     */
    @Override
    protected void flush() {
        this.viewportBlockArea.addBlock(this.referenceArea, this.autoHeight);

        TraitSetter.addBackground(this.viewportBlockArea, getBlockContainerFO()
                .getCommonBorderPaddingBackground(), this);

        super.flush();
    }

    /** {@inheritDoc} */
    @Override
    public int negotiateBPDAdjustment(final int adj,
            final KnuthElement lastElement) {
        // TODO Auto-generated method stub
        return 0;
    }

    /** {@inheritDoc} */
    @Override
    public void discardSpace(final KnuthGlue spaceGlue) {
        // TODO Auto-generated method stub

    }

    /** {@inheritDoc} */
    @Override
    public KeepProperty getKeepTogetherProperty() {
        return getBlockContainerFO().getKeepTogether();
    }

    /** {@inheritDoc} */
    @Override
    public KeepProperty getKeepWithPreviousProperty() {
        return getBlockContainerFO().getKeepWithPrevious();
    }

    /** {@inheritDoc} */
    @Override
    public KeepProperty getKeepWithNextProperty() {
        return getBlockContainerFO().getKeepWithNext();
    }

    /**
     * @return the BlockContainer node
     */
    protected BlockContainer getBlockContainerFO() {
        return (BlockContainer) this.fobj;
    }

    // --------- Property Resolution related functions --------- //

    /** {@inheritDoc} */
    @Override
    public boolean getGeneratesReferenceArea() {
        return true;
    }

    /** {@inheritDoc} */
    @Override
    public boolean getGeneratesBlockArea() {
        return true;
    }

    /** {@inheritDoc} */
    @Override
    public void notifySpace(final RelSide side, final MinOptMax effectiveLength) {
        if (RelSide.BEFORE == side) {
            if (log.isDebugEnabled()) {
                log.debug(this + ": Space " + side + ", " + this.effSpaceBefore
                        + "-> " + effectiveLength);
            }
            this.effSpaceBefore = effectiveLength;
        } else {
            if (log.isDebugEnabled()) {
                log.debug(this + ": Space " + side + ", " + this.effSpaceAfter
                        + "-> " + effectiveLength);
            }
            this.effSpaceAfter = effectiveLength;
        }
    }

    /** {@inheritDoc} */
    @Override
    public void notifyBorder(final RelSide side, final MinOptMax effectiveLength) {
        if (effectiveLength == null) {
            if (RelSide.BEFORE == side) {
                this.discardBorderBefore = true;
            } else {
                this.discardBorderAfter = true;
            }
        }
        if (log.isDebugEnabled()) {
            log.debug(this + ": Border " + side + " -> " + effectiveLength);
        }
    }

    /** {@inheritDoc} */
    @Override
    public void notifyPadding(final RelSide side,
            final MinOptMax effectiveLength) {
        if (effectiveLength == null) {
            if (RelSide.BEFORE == side) {
                this.discardPaddingBefore = true;
            } else {
                this.discardPaddingAfter = true;
            }
        }
        if (log.isDebugEnabled()) {
            log.debug(this + ": Padding " + side + " -> " + effectiveLength);
        }
    }

}
