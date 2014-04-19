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

/* $Id: BlockStackingLayoutManager.java 893238 2009-12-22 17:20:51Z vhennebert $ */

package org.apache.fop.layoutmgr;

import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.ListIterator;
import java.util.Stack;

import lombok.extern.slf4j.Slf4j;

import org.apache.fop.area.Area;
import org.apache.fop.area.Block;
import org.apache.fop.area.BlockParent;
import org.apache.fop.fo.Constants;
import org.apache.fop.fo.FObj;
import org.apache.fop.fo.properties.BreakPropertySet;
import org.apache.fop.fo.properties.CommonBorderPaddingBackground;
import org.apache.fop.fo.properties.KeepProperty;
import org.apache.fop.fo.properties.SpaceProperty;
import org.apache.fop.layoutmgr.inline.InlineLayoutManager;
import org.apache.fop.layoutmgr.inline.LineLayoutManager;
import org.apache.fop.traits.MinOptMax;
import org.apache.fop.util.BreakUtil;
import org.apache.fop.util.ListUtil;

/**
 * Base LayoutManager class for all areas which stack their child areas in the
 * block-progression direction, such as Flow, Block, ListBlock.
 */
@Slf4j
public abstract class BlockStackingLayoutManager extends AbstractLayoutManager
implements BlockLevelLayoutManager {

    protected BlockParent parentArea;

    /** Value of the block-progression-unit (non-standard property) */
    protected int bpUnit;
    /** space-before value adjusted for block-progression-unit handling */
    protected int adjustedSpaceBefore;
    /** space-after value adjusted for block-progression-unit handling */
    protected int adjustedSpaceAfter;
    /** Only used to store the original list when createUnitElements is called */
    protected List storedList;
    /** Indicates whether break before has been served or not */
    protected boolean breakBeforeServed;
    /**
     * Indicates whether the first visible mark has been returned by this LM,
     * yet
     */
    protected boolean firstVisibleMarkServed;
    /** Reference IPD available */
    protected int referenceIPD;
    /** the effective start-indent value */
    protected int startIndent;
    /** the effective end-indent value */
    protected int endIndent;
    /**
     * Holds the (one-time use) fo:block space-before and -after properties.
     * Large fo:blocks are split into multiple Area. Blocks to accomodate the
     * subsequent regions (pages) they are placed on. space-before is applied at
     * the beginning of the first Block and space-after at the end of the last
     * Block used in rendering the fo:block.
     */
    protected MinOptMax foSpaceBefore;
    /** see foSpaceBefore */
    protected MinOptMax foSpaceAfter;

    private Position auxiliaryPosition;

    private int contentAreaIPD;

    /**
     * @param node
     *            the fo this LM deals with
     */
    public BlockStackingLayoutManager(final FObj node) {
        super(node);
        setGeneratesBlockArea(true);
    }

    /**
     * @return current area being filled
     */
    protected BlockParent getCurrentArea() {
        return this.parentArea;
    }

    /**
     * Set the current area being filled.
     *
     * @param parentArea
     *            the current area to be filled
     */
    protected void setCurrentArea(final BlockParent parentArea) {
        this.parentArea = parentArea;
    }

    /**
     * Add a block spacer for space before and space after a block. This adds an
     * empty Block area that acts as a block space.
     *
     * @param adjust
     *            the adjustment value
     * @param minoptmax
     *            the min/opt/max value of the spacing
     */
    public void addBlockSpacing(final double adjust, final MinOptMax minoptmax) {
        final int sp = TraitSetter.getEffectiveSpace(adjust, minoptmax);
        if (sp != 0) {
            final Block spacer = new Block();
            spacer.setBPD(sp);
            this.parentLayoutManager.addChildArea(spacer);
        }
    }

    /**
     * Add the childArea to the passed area. Called by child LayoutManager when
     * it has filled one of its areas. The LM should already have an Area in
     * which to put the child. See if the area will fit in the current area. If
     * so, add it. Otherwise initiate breaking.
     *
     * @param childArea
     *            the area to add: will be some block-stacked Area.
     * @param parentArea
     *            the area in which to add the childArea
     */
    protected void addChildToArea(final Area childArea,
            final BlockParent parentArea) {
        // This should be a block-level Area (Block in the generic sense)
        if (!(childArea instanceof Block)) {
            // log.error("Child not a Block in BlockStackingLM!");
        }

        parentArea.addBlock((Block) childArea);
        flush(); // hand off current area to parent
    }

    /**
     * Add the childArea to the current area. Called by child LayoutManager when
     * it has filled one of its areas. The LM should already have an Area in
     * which to put the child. See if the area will fit in the current area. If
     * so, add it. Otherwise initiate breaking.
     *
     * @param childArea
     *            the area to add: will be some block-stacked Area.
     */
    @Override
    public void addChildArea(final Area childArea) {
        addChildToArea(childArea, getCurrentArea());
    }

    /** {@inheritDoc} */
    @Override
    protected void notifyEndOfLayout() {
        super.notifyEndOfLayout();
        // Free memory of the area tree
        // this.parentArea = null;
    }

    /**
     * Force current area to be added to parent area.
     */
    protected void flush() {
        if (getCurrentArea() != null) {
            this.parentLayoutManager.addChildArea(getCurrentArea());
        }
    }

    /** @return a cached auxiliary Position instance used for things like spaces. */
    protected Position getAuxiliaryPosition() {
        if (this.auxiliaryPosition == null) {
            this.auxiliaryPosition = new NonLeafPosition(this, null);
        }
        return this.auxiliaryPosition;
    }

    /**
     * @param len
     *            length in millipoints to span with bp units
     * @return the minimum integer n such that n * bpUnit >= len
     */
    protected int neededUnits(final int len) {
        return (int) Math.ceil((float) len / this.bpUnit);
    }

    /**
     * Determines and sets the content area IPD based on available reference
     * area IPD, start- and end-indent properties. end-indent is adjusted based
     * on overconstrained geometry rules, if necessary.
     *
     * @return the resulting content area IPD
     */
    protected int updateContentAreaIPDwithOverconstrainedAdjust() {
        int ipd = this.referenceIPD - (this.startIndent + this.endIndent);
        if (ipd < 0) {
            // 5.3.4, XSL 1.0, Overconstrained Geometry
            log.debug("Adjusting end-indent based on overconstrained geometry rules for "
                    + this.fobj);
            final BlockLevelEventProducer eventProducer = BlockLevelEventProducer.Provider
                    .get(getFObj().getUserAgent().getEventBroadcaster());
            eventProducer.overconstrainedAdjustEndIndent(this, getFObj()
                    .getName(), ipd, getFObj().getLocator());
            this.endIndent += ipd;
            ipd = 0;
            // TODO Should we skip layout for a block that has ipd=0?
        }
        setContentAreaIPD(ipd);
        return ipd;
    }

    /**
     * Sets the content area IPD by directly supplying the value. end-indent is
     * adjusted based on overconstrained geometry rules, if necessary.
     *
     * @param contentIPD
     *            the IPD of the content
     * @return the resulting content area IPD
     */
    protected int updateContentAreaIPDwithOverconstrainedAdjust(
            final int contentIPD) {
        final int ipd = this.referenceIPD
                - (contentIPD + this.startIndent + this.endIndent);
        if (ipd < 0) {
            // 5.3.4, XSL 1.0, Overconstrained Geometry
            log.debug("Adjusting end-indent based on overconstrained geometry rules for "
                    + this.fobj);
            final BlockLevelEventProducer eventProducer = BlockLevelEventProducer.Provider
                    .get(getFObj().getUserAgent().getEventBroadcaster());
            eventProducer.overconstrainedAdjustEndIndent(this, getFObj()
                    .getName(), ipd, getFObj().getLocator());
            this.endIndent += ipd;
        }
        setContentAreaIPD(contentIPD);
        return contentIPD;
    }

    /** {@inheritDoc} */
    @Override
    public List getNextKnuthElements(final LayoutContext context,
            final int alignment) {
        this.referenceIPD = context.getRefIPD();
        updateContentAreaIPDwithOverconstrainedAdjust();

        final List contentList = new LinkedList();
        final List elements = new LinkedList();

        if (!this.breakBeforeServed) {
            this.breakBeforeServed = true;
            if (!context.suppressBreakBefore()) {
                if (addKnuthElementsForBreakBefore(elements, context)) {
                    return elements;
                }
            }
        }

        if (!this.firstVisibleMarkServed) {
            addKnuthElementsForSpaceBefore(elements, alignment);
            context.updateKeepWithPreviousPending(getKeepWithPrevious());
        }

        addKnuthElementsForBorderPaddingBefore(elements,
                !this.firstVisibleMarkServed);
        this.firstVisibleMarkServed = true;

        // Spaces, border and padding to be repeated at each break
        addPendingMarks(context);

        // Used to indicate a special break-after case when all content has
        // already been generated.
        BreakElement forcedBreakAfterLast = null;

        LayoutManager currentChildLM;
        while ((currentChildLM = getChildLM()) != null) {
            final LayoutContext childLC = new LayoutContext(0);

            final List childrenElements = getNextChildElements(currentChildLM,
                    context, childLC, alignment);

            if (contentList.isEmpty()) {
                // Propagate keep-with-previous up from the first child
                context.updateKeepWithPreviousPending(childLC
                        .getKeepWithPreviousPending());
            }
            if (childrenElements != null && !childrenElements.isEmpty()) {
                if (!contentList.isEmpty()
                        && !ElementListUtils
                        .startsWithForcedBreak(childrenElements)) {
                    // there is a block handled by prevLM before the one
                    // handled by curLM, and the one handled
                    // by the current LM does not begin with a break
                    addInBetweenBreak(contentList, context, childLC);
                }
                if (childrenElements.size() == 1
                        && ElementListUtils
                        .startsWithForcedBreak(childrenElements)) {

                    if (currentChildLM.isFinished() && !hasNextChildLM()) {
                        // a descendant of this block has break-before
                        forcedBreakAfterLast = (BreakElement) childrenElements
                                .get(0);
                        context.clearPendingMarks();
                        break;
                    }

                    if (contentList.isEmpty()) {
                        // Empty fo:block, zero-length box makes sure the IDs
                        // and/or markers
                        // are registered and borders/padding are painted.
                        elements.add(new KnuthBox(0, notifyPos(new Position(
                                this)), false));
                    }
                    // a descendant of this block has break-before
                    contentList.addAll(childrenElements);

                    wrapPositionElements(contentList, elements);

                    return elements;
                } else {
                    contentList.addAll(childrenElements);
                    if (ElementListUtils.endsWithForcedBreak(childrenElements)) {
                        // a descendant of this block has break-after
                        if (currentChildLM.isFinished() && !hasNextChildLM()) {
                            forcedBreakAfterLast = (BreakElement) ListUtil
                                    .removeLast(contentList);
                            context.clearPendingMarks();
                            break;
                        }

                        wrapPositionElements(contentList, elements);

                        return elements;
                    }
                }
                context.updateKeepWithNextPending(childLC
                        .getKeepWithNextPending());
            }
        }

        if (!contentList.isEmpty()) {
            wrapPositionElements(contentList, elements);
        } else if (forcedBreakAfterLast == null) {
            // Empty fo:block, zero-length box makes sure the IDs and/or markers
            // are registered.
            elements.add(new KnuthBox(0, notifyPos(new Position(this)), true));
        }

        addKnuthElementsForBorderPaddingAfter(elements, true);
        addKnuthElementsForSpaceAfter(elements, alignment);

        // All child content is processed. Only break-after can occur now, so...
        context.clearPendingMarks();
        if (forcedBreakAfterLast == null) {
            addKnuthElementsForBreakAfter(elements, context);
        } else {
            forcedBreakAfterLast.clearPendingMarks();
            elements.add(forcedBreakAfterLast);
        }

        context.updateKeepWithNextPending(getKeepWithNext());

        setFinished(true);

        return elements;
    }

    /** {@inheritDoc} */
    @Override
    public List getNextKnuthElements(final LayoutContext context,
            final int alignment, final Stack lmStack,
            final Position restartPosition, final LayoutManager restartAtLM) {
        this.referenceIPD = context.getRefIPD();
        updateContentAreaIPDwithOverconstrainedAdjust();

        final List contentList = new LinkedList();
        final List elements = new LinkedList();

        if (!this.breakBeforeServed) {
            this.breakBeforeServed = true;
            if (!context.suppressBreakBefore()) {
                if (addKnuthElementsForBreakBefore(elements, context)) {
                    return elements;
                }
            }
        }

        if (!this.firstVisibleMarkServed) {
            addKnuthElementsForSpaceBefore(elements, alignment);
            context.updateKeepWithPreviousPending(getKeepWithPrevious());
        }

        addKnuthElementsForBorderPaddingBefore(elements,
                !this.firstVisibleMarkServed);
        this.firstVisibleMarkServed = true;

        // Spaces, border and padding to be repeated at each break
        addPendingMarks(context);

        // Used to indicate a special break-after case when all content has
        // already been generated.
        BreakElement forcedBreakAfterLast = null;

        LayoutContext childLC = new LayoutContext(0);
        List childrenElements;
        LayoutManager currentChildLM;
        if (lmStack.isEmpty()) {
            assert restartAtLM != null && restartAtLM.getParent() == this;
            currentChildLM = restartAtLM;
            currentChildLM.reset();
            setCurrentChildLM(currentChildLM);

            childrenElements = getNextChildElements(currentChildLM, context,
                    childLC, alignment);
        } else {
            currentChildLM = (BlockLevelLayoutManager) lmStack.pop();
            setCurrentChildLM(currentChildLM);
            childrenElements = getNextChildElements(currentChildLM, context,
                    childLC, alignment, lmStack, restartPosition, restartAtLM);
        }

        if (contentList.isEmpty()) {
            // Propagate keep-with-previous up from the first child
            context.updateKeepWithPreviousPending(childLC
                    .getKeepWithPreviousPending());
        }
        if (childrenElements != null && !childrenElements.isEmpty()) {
            if (!contentList.isEmpty()
                    && !ElementListUtils
                    .startsWithForcedBreak(childrenElements)) {
                // there is a block handled by prevLM before the one
                // handled by curLM, and the one handled
                // by the current LM does not begin with a break
                addInBetweenBreak(contentList, context, childLC);
            }
            if (childrenElements.size() == 1
                    && ElementListUtils.startsWithForcedBreak(childrenElements)) {

                if (currentChildLM.isFinished() && !hasNextChildLM()) {
                    // a descendant of this block has break-before
                    forcedBreakAfterLast = (BreakElement) childrenElements
                            .get(0);
                    context.clearPendingMarks();
                    // break; TODO
                }

                if (contentList.isEmpty()) {
                    // Empty fo:block, zero-length box makes sure the IDs and/or
                    // markers
                    // are registered and borders/padding are painted.
                    elements.add(new KnuthBox(0, notifyPos(new Position(this)),
                            false));
                }
                // a descendant of this block has break-before
                contentList.addAll(childrenElements);

                wrapPositionElements(contentList, elements);

                return elements;
            } else {
                contentList.addAll(childrenElements);
                if (ElementListUtils.endsWithForcedBreak(childrenElements)) {
                    // a descendant of this block has break-after
                    if (currentChildLM.isFinished() && !hasNextChildLM()) {
                        forcedBreakAfterLast = (BreakElement) ListUtil
                                .removeLast(contentList);
                        context.clearPendingMarks();
                        // break; TODO
                    }

                    wrapPositionElements(contentList, elements);

                    return elements;
                }
            }
            context.updateKeepWithNextPending(childLC.getKeepWithNextPending());
        }

        while ((currentChildLM = getChildLM()) != null) {
            currentChildLM.reset(); // TODO won't work with forced breaks

            childLC = new LayoutContext(0);

            childrenElements = getNextChildElements(currentChildLM, context,
                    childLC, alignment);

            if (contentList.isEmpty()) {
                // Propagate keep-with-previous up from the first child
                context.updateKeepWithPreviousPending(childLC
                        .getKeepWithPreviousPending());
            }
            if (childrenElements != null && !childrenElements.isEmpty()) {
                if (!contentList.isEmpty()
                        && !ElementListUtils
                        .startsWithForcedBreak(childrenElements)) {
                    // there is a block handled by prevLM before the one
                    // handled by curLM, and the one handled
                    // by the current LM does not begin with a break
                    addInBetweenBreak(contentList, context, childLC);
                }
                if (childrenElements.size() == 1
                        && ElementListUtils
                        .startsWithForcedBreak(childrenElements)) {

                    if (currentChildLM.isFinished() && !hasNextChildLM()) {
                        // a descendant of this block has break-before
                        forcedBreakAfterLast = (BreakElement) childrenElements
                                .get(0);
                        context.clearPendingMarks();
                        break;
                    }

                    if (contentList.isEmpty()) {
                        // Empty fo:block, zero-length box makes sure the IDs
                        // and/or markers
                        // are registered and borders/padding are painted.
                        elements.add(new KnuthBox(0, notifyPos(new Position(
                                this)), false));
                    }
                    // a descendant of this block has break-before
                    contentList.addAll(childrenElements);

                    wrapPositionElements(contentList, elements);

                    return elements;
                } else {
                    contentList.addAll(childrenElements);
                    if (ElementListUtils.endsWithForcedBreak(childrenElements)) {
                        // a descendant of this block has break-after
                        if (currentChildLM.isFinished() && !hasNextChildLM()) {
                            forcedBreakAfterLast = (BreakElement) ListUtil
                                    .removeLast(contentList);
                            context.clearPendingMarks();
                            break;
                        }

                        wrapPositionElements(contentList, elements);

                        return elements;
                    }
                }
                context.updateKeepWithNextPending(childLC
                        .getKeepWithNextPending());
            }
        }

        if (!contentList.isEmpty()) {
            wrapPositionElements(contentList, elements);
        } else if (forcedBreakAfterLast == null) {
            // Empty fo:block, zero-length box makes sure the IDs and/or markers
            // are registered.
            elements.add(new KnuthBox(0, notifyPos(new Position(this)), true));
        }

        addKnuthElementsForBorderPaddingAfter(elements, true);
        addKnuthElementsForSpaceAfter(elements, alignment);

        // All child content is processed. Only break-after can occur now, so...
        context.clearPendingMarks();
        if (forcedBreakAfterLast == null) {
            addKnuthElementsForBreakAfter(elements, context);
        } else {
            forcedBreakAfterLast.clearPendingMarks();
            elements.add(forcedBreakAfterLast);
        }

        context.updateKeepWithNextPending(getKeepWithNext());

        setFinished(true);

        return elements;
    }

    private List getNextChildElements(final LayoutManager childLM,
            final LayoutContext context, final LayoutContext childLC,
            final int alignment) {
        return getNextChildElements(childLM, context, childLC, alignment, null,
                null, null);
    }

    private List getNextChildElements(final LayoutManager childLM,
            final LayoutContext context, final LayoutContext childLC,
            final int alignment, final Stack lmStack,
            final Position restartPosition, final LayoutManager restartAtLM) {
        childLC.copyPendingMarksFrom(context);
        childLC.setStackLimitBP(context.getStackLimitBP());
        if (childLM instanceof LineLayoutManager) {
            childLC.setRefIPD(getContentAreaIPD());
        } else {
            childLC.setRefIPD(this.referenceIPD);
        }
        if (childLM == this.childLMs.get(0)) {
            childLC.setFlags(LayoutContext.SUPPRESS_BREAK_BEFORE);
            // Handled already by the parent (break collapsing, see above)
        }

        if (lmStack == null) {
            return childLM.getNextKnuthElements(childLC, alignment);
        } else {
            if (childLM instanceof LineLayoutManager) {
                return ((LineLayoutManager) childLM).getNextKnuthElements(
                        childLC, alignment, (LeafPosition) restartPosition);
            } else {
                return childLM.getNextKnuthElements(childLC, alignment,
                        lmStack, restartPosition, restartAtLM);
            }
        }
    }

    /**
     * Adds a break element to the content list between individual child
     * elements.
     *
     * @param contentList
     * @param parentLC
     * @param childLC
     *            the currently active child layout context
     */
    protected void addInBetweenBreak(final List contentList,
            final LayoutContext parentLC, final LayoutContext childLC) {

        if (mustKeepTogether() || parentLC.isKeepWithNextPending()
                || childLC.isKeepWithPreviousPending()) {

            Keep keep = getKeepTogether();

            // Handle pending keep-with-next
            keep = keep.compare(parentLC.getKeepWithNextPending());
            parentLC.clearKeepWithNextPending();

            // Handle pending keep-with-previous from child LM
            keep = keep.compare(childLC.getKeepWithPreviousPending());
            childLC.clearKeepWithPreviousPending();

            // add a penalty to forbid or discourage a break between blocks
            contentList.add(new BreakElement(new Position(this), keep
                    .getPenalty(), keep.getContext(), parentLC));
            return;
        }

        final ListElement last = (ListElement) ListUtil.getLast(contentList);
        if (last.isGlue()) {
            // the last element in contentList is a glue;
            // it is a feasible breakpoint, there is no need to add
            // a penalty
            log.warn("glue-type break possibility not handled properly, yet");
            // TODO Does this happen? If yes, need to deal with border and
            // padding
            // at the break possibility
        } else if (!ElementListUtils.endsWithNonInfinitePenalty(contentList)) {

            // TODO vh: this is hacky
            // The getNextKnuthElements method of TableCellLM must not be called
            // twice, otherwise some settings like indents or borders will be
            // counted several times and lead to a wrong output. Anyway the
            // getNextKnuthElements methods should be called only once
            // eventually
            // (i.e., when multi-threading the code), even when there are forced
            // breaks.
            // If we add a break possibility after a forced break the
            // AreaAdditionUtil.addAreas method will act on a sequence starting
            // with a SpaceResolver.SpaceHandlingBreakPosition element, having
            // no
            // LM associated to it. Thus it will stop early instead of adding
            // areas for following Positions. The above test aims at preventing
            // such a situation from occurring. add a null penalty to allow a
            // break
            // between blocks

            // add a null penalty to allow a break between blocks
            contentList.add(new BreakElement(new Position(this), 0,
                    Constants.EN_AUTO, parentLC));
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int negotiateBPDAdjustment(final int adj,
            final KnuthElement lastElement) {
        /* LF */// log.debug("  BLM.negotiateBPDAdjustment> " + adj);
        /* LF */// log.debug("  lastElement e' " + (lastElement.isPenalty()
        // ? "penalty" : (lastElement.isGlue() ? "glue" : "box" )));
        /* LF */// log.debug("  position e' " +
        // lastElement.getPosition().getClass().getName());
        /* LF */// log.debug("  " + (bpUnit > 0 ? "unit" : ""));
        final Position innerPosition = lastElement.getPosition().getPosition();

        if (innerPosition == null && lastElement.isGlue()) {
            // this adjustment applies to space-before or space-after of this
            // block
            if (((KnuthGlue) lastElement).getAdjustmentClass() == Adjustment.SPACE_BEFORE_ADJUSTMENT) {
                // this adjustment applies to space-before
                this.adjustedSpaceBefore += adj;
                /* LF */// log.debug("  BLM.negotiateBPDAdjustment> spazio prima: "
                // + adj);
            } else {
                // this adjustment applies to space-after
                this.adjustedSpaceAfter += adj;
                /* LF */// log.debug("  BLM.negotiateBPDAdjustment> spazio dopo: "
                // + adj);
            }
            return adj;
        } else if (innerPosition instanceof MappingPosition) {
            // this block has block-progression-unit > 0: the adjustment can
            // concern
            // - the space-before or space-after of this block,
            // - the line number of a descendant of this block
            final MappingPosition mappingPos = (MappingPosition) innerPosition;
            if (lastElement.isGlue()) {
                // lastElement is a glue
                /* LF */// log.debug("  BLM.negotiateBPDAdjustment> bpunit con glue");
                final ListIterator storedListIterator = this.storedList
                        .listIterator(mappingPos.getFirstIndex());
                int newAdjustment = 0;
                while (storedListIterator.nextIndex() <= mappingPos
                        .getLastIndex()) {
                    final KnuthElement storedElement = (KnuthElement) storedListIterator
                            .next();
                    if (storedElement.isGlue()) {
                        newAdjustment += ((BlockLevelLayoutManager) storedElement
                                .getLayoutManager()).negotiateBPDAdjustment(adj
                                        - newAdjustment, storedElement);
                        /* LF */// log.debug("  BLM.negotiateBPDAdjustment> (progressivo) righe: "
                        // + newAdjustment);
                    }
                }
                newAdjustment = newAdjustment > 0 ? this.bpUnit
                        * neededUnits(newAdjustment) : -this.bpUnit
                        * neededUnits(-newAdjustment);
                return newAdjustment;
            } else {
                // lastElement is a penalty: this means that the paragraph
                // has been split between consecutive pages:
                // this may involve a change in the number of lines
                /* LF */// log.debug("  BLM.negotiateBPDAdjustment> bpunit con penalty");
                final KnuthPenalty storedPenalty = (KnuthPenalty) this.storedList
                        .get(mappingPos.getLastIndex());
                if (storedPenalty.getWidth() > 0) {
                    // the original penalty has width > 0
                    /* LF */// log.debug("  BLM.negotiateBPDAdjustment> chiamata passata");
                    return ((BlockLevelLayoutManager) storedPenalty
                            .getLayoutManager()).negotiateBPDAdjustment(
                                    storedPenalty.getWidth(), storedPenalty);
                } else {
                    // the original penalty has width = 0
                    // the adjustment involves only the spaces before and after
                    /* LF */// log.debug("  BLM.negotiateBPDAdjustment> chiamata gestita");
                    return adj;
                }
            }
        } else if (innerPosition.getLM() != this) {
            // this adjustment concerns another LM
            final NonLeafPosition savedPos = (NonLeafPosition) lastElement
                    .getPosition();
            lastElement.setPosition(innerPosition);
            final int returnValue = ((BlockLevelLayoutManager) lastElement
                    .getLayoutManager()).negotiateBPDAdjustment(adj,
                            lastElement);
            lastElement.setPosition(savedPos);
            /* LF */// log.debug("  BLM.negotiateBPDAdjustment> righe: " +
            // returnValue);
            return returnValue;
        } else {
            // this should never happen
            log.error("BlockLayoutManager.negotiateBPDAdjustment(): unexpected Position");
            return 0;
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void discardSpace(final KnuthGlue spaceGlue) {
        // log.debug("  BLM.discardSpace> " +
        // spaceGlue.getPosition().getClass().getName());
        final Position innerPosition = ((NonLeafPosition) spaceGlue
                .getPosition()).getPosition();

        if (innerPosition == null || innerPosition.getLM() == this) {
            // if this block has block-progression-unit > 0, innerPosition can
            // be
            // a MappingPosition
            // spaceGlue represents space before or space after of this block
            if (spaceGlue.getAdjustmentClass() == Adjustment.SPACE_BEFORE_ADJUSTMENT) {
                // space-before must be discarded
                this.adjustedSpaceBefore = 0;
                this.foSpaceBefore = MinOptMax.ZERO;
            } else {
                // space-after must be discarded
                this.adjustedSpaceAfter = 0;
                this.foSpaceAfter = MinOptMax.ZERO;
                // TODO Why are both cases handled in the same way?
            }
        } else {
            // this element was not created by this BlockLM
            final NonLeafPosition savedPos = (NonLeafPosition) spaceGlue
                    .getPosition();
            spaceGlue.setPosition(innerPosition);
            ((BlockLevelLayoutManager) spaceGlue.getLayoutManager())
            .discardSpace(spaceGlue);
            spaceGlue.setPosition(savedPos);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List getChangedKnuthElements(final List oldList, final int alignment) {
        /* LF */// log.debug("");
        /* LF */// log.debug("  BLM.getChangedKnuthElements> inizio: oldList.size() = "
        // + oldList.size());
        ListIterator oldListIterator = oldList.listIterator();
        KnuthElement returnedElement;
        KnuthElement currElement = null;
        KnuthElement prevElement = null;
        List returnedList = new LinkedList();
        final List returnList = new LinkedList();
        int fromIndex = 0;

        // "unwrap" the Positions stored in the elements
        KnuthElement oldElement = null;
        while (oldListIterator.hasNext()) {
            oldElement = (KnuthElement) oldListIterator.next();
            final Position innerPosition = ((NonLeafPosition) oldElement
                    .getPosition()).getPosition();
            // log.debug(" BLM> unwrapping: "
            // + (oldElement.isBox() ? "box    " : (oldElement.isGlue() ?
            // "glue   " : "penalty"))
            // + " creato da " +
            // oldElement.getLayoutManager().getClass().getName());
            // log.debug(" BLM> unwrapping:         "
            // + oldElement.getPosition().getClass().getName());
            if (innerPosition != null) {
                // oldElement was created by a descendant of this BlockLM
                oldElement.setPosition(innerPosition);
            } else {
                // thisElement was created by this BlockLM
                // modify its position in order to recognize it was not created
                // by a child
                oldElement.setPosition(new Position(this));
            }
        }

        // create the iterator
        List workList;
        if (this.bpUnit == 0) {
            workList = oldList;
        } else {
            // the storedList must be used instead of oldList;
            // find the index of the first element of returnedList
            // corresponding to the first element of oldList
            oldListIterator = oldList.listIterator();
            KnuthElement el = (KnuthElement) oldListIterator.next();
            while (!(el.getPosition() instanceof MappingPosition)) {
                el = (KnuthElement) oldListIterator.next();
            }
            final int iFirst = ((MappingPosition) el.getPosition())
                    .getFirstIndex();

            // find the index of the last element of returnedList
            // corresponding to the last element of oldList
            oldListIterator = oldList.listIterator(oldList.size());
            el = (KnuthElement) oldListIterator.previous();
            while (!(el.getPosition() instanceof MappingPosition)) {
                el = (KnuthElement) oldListIterator.previous();
            }
            final int iLast = ((MappingPosition) el.getPosition())
                    .getLastIndex();

            // log-debug("  si usa storedList da " + iFirst + " a " + iLast
            // + " compresi su " + storedList.size() + " elementi totali");
            workList = this.storedList.subList(iFirst, iLast + 1);
        }
        final ListIterator workListIterator = workList.listIterator();

        // log.debug("  BLM.getChangedKnuthElements> workList.size() = "
        // + workList.size() + " da 0 a " + (workList.size() - 1));

        while (workListIterator.hasNext()) {
            currElement = (KnuthElement) workListIterator.next();
            // log.debug("elemento n. " + workListIterator.previousIndex()
            // + " nella workList");
            if (prevElement != null
                    && prevElement.getLayoutManager() != currElement
                    .getLayoutManager()) {
                // prevElement is the last element generated by the same LM
                final BlockLevelLayoutManager prevLM = (BlockLevelLayoutManager) prevElement
                        .getLayoutManager();
                final BlockLevelLayoutManager currLM = (BlockLevelLayoutManager) currElement
                        .getLayoutManager();
                boolean bSomethingAdded = false;
                if (prevLM != this) {
                    // log.debug(" BLM.getChangedKnuthElements> chiamata da "
                    // + fromIndex + " a " + workListIterator.previousIndex() +
                    // " su "
                    // + prevLM.getClass().getName());
                    returnedList.addAll(prevLM.getChangedKnuthElements(
                            workList.subList(fromIndex,
                                    workListIterator.previousIndex()),
                                    alignment));
                    bSomethingAdded = true;
                } else {
                    // prevLM == this
                    // do nothing
                    // log.debug(" BLM.getChangedKnuthElements> elementi propri, "
                    // + "ignorati, da " + fromIndex + " a " +
                    // workListIterator.previousIndex()
                    // + " su " + prevLM.getClass().getName());
                }
                fromIndex = workListIterator.previousIndex();

                /*
                 * TODO: why are KnuthPenalties added here, while in getNextKE
                 * they were changed to BreakElements?
                 */
                // there is another block after this one
                if (bSomethingAdded
                        && (mustKeepTogether() || prevLM.mustKeepWithNext() || currLM
                                .mustKeepWithPrevious())) {
                    // add an infinite penalty to forbid a break between blocks
                    returnedList.add(new KnuthPenalty(0, KnuthElement.INFINITE,
                            false, new Position(this), false));
                } else if (bSomethingAdded
                        && !((KnuthElement) ListUtil.getLast(returnedList))
                                .isGlue()) {
                    // add a null penalty to allow a break between blocks
                    returnedList.add(new KnuthPenalty(0, 0, false,
                            new Position(this), false));
                }
            }
            prevElement = currElement;
        }
        if (currElement != null) {
            final BlockLevelLayoutManager currLM = (BlockLevelLayoutManager) currElement
                    .getLayoutManager();
            if (currLM != this) {
                // log.debug(" BLM.getChangedKnuthElements> chiamata da " +
                // fromIndex
                // + " a " + oldList.size() + " su " +
                // currLM.getClass().getName());
                returnedList
                .addAll(currLM.getChangedKnuthElements(
                        workList.subList(fromIndex, workList.size()),
                        alignment));
            } else {
                // currLM == this
                // there are no more elements to add
                // remove the last penalty added to returnedList
                if (!returnedList.isEmpty()) {
                    ListUtil.removeLast(returnedList);
                }
                // log.debug(" BLM.getChangedKnuthElements> elementi propri, ignorati, da "
                // + fromIndex + " a " + workList.size());
            }
        }

        // append elements representing space-before
        boolean spaceBeforeIsConditional = true;
        if (this.fobj instanceof org.apache.fop.fo.flow.Block) {
            spaceBeforeIsConditional = ((org.apache.fop.fo.flow.Block) this.fobj)
                    .getCommonMarginBlock().spaceBefore.getSpace().isDiscard();
        }
        if (this.bpUnit > 0 || this.adjustedSpaceBefore != 0) {
            if (!spaceBeforeIsConditional) {
                // add elements to prevent the glue to be discarded
                returnList.add(new KnuthBox(0, new NonLeafPosition(this, null),
                        false));
                returnList.add(new KnuthPenalty(0, KnuthElement.INFINITE,
                        false, new NonLeafPosition(this, null), false));
            }
            if (this.bpUnit > 0) {
                returnList.add(new KnuthGlue(0, 0, 0,
                        Adjustment.SPACE_BEFORE_ADJUSTMENT,
                        new NonLeafPosition(this, null), true));
            } else {
                returnList.add(new KnuthGlue(this.adjustedSpaceBefore, 0, 0,
                        Adjustment.SPACE_BEFORE_ADJUSTMENT,
                        new NonLeafPosition(this, null), true));
            }
        }

        // log.debug("  BLM.getChangedKnuthElements> intermedio: returnedList.size() = "
        // + returnedList.size());

        /* estensione: conversione complessiva */
        /* LF */if (this.bpUnit > 0) {
            /* LF */this.storedList = returnedList;
            /* LF */returnedList = createUnitElements(returnedList);
        /* LF */}
        /* estensione */

        // "wrap" the Position stored in each element of returnedList
        // and add elements to returnList
        final ListIterator listIter = returnedList.listIterator();
        while (listIter.hasNext()) {
            returnedElement = (KnuthElement) listIter.next();
            returnedElement.setPosition(new NonLeafPosition(this,
                    returnedElement.getPosition()));
            returnList.add(returnedElement);
        }

        // append elements representing space-after
        boolean spaceAfterIsConditional = true;
        if (this.fobj instanceof org.apache.fop.fo.flow.Block) {
            spaceAfterIsConditional = ((org.apache.fop.fo.flow.Block) this.fobj)
                    .getCommonMarginBlock().spaceAfter.getSpace().isDiscard();
        }
        if (this.bpUnit > 0 || this.adjustedSpaceAfter != 0) {
            if (!spaceAfterIsConditional) {
                returnList.add(new KnuthPenalty(0, KnuthElement.INFINITE,
                        false, new NonLeafPosition(this, null), false));
            }
            if (this.bpUnit > 0) {
                returnList.add(new KnuthGlue(0, 0, 0,
                        Adjustment.SPACE_AFTER_ADJUSTMENT, new NonLeafPosition(
                                this, null), spaceAfterIsConditional));
            } else {
                returnList.add(new KnuthGlue(this.adjustedSpaceAfter, 0, 0,
                        Adjustment.SPACE_AFTER_ADJUSTMENT, new NonLeafPosition(
                                this, null), spaceAfterIsConditional));
            }
            if (!spaceAfterIsConditional) {
                returnList.add(new KnuthBox(0, new NonLeafPosition(this, null),
                        true));
            }
        }

        // log.debug("  BLM.getChangedKnuthElements> finished: returnList.size() = "
        // + returnList.size());
        return returnList;
    }

    /**
     * Retrieves and returns the keep-together strength from the parent element.
     *
     * @return the keep-together strength
     */
    protected Keep getParentKeepTogether() {
        Keep keep = Keep.KEEP_AUTO;
        if (getParent() instanceof BlockLevelLayoutManager) {
            keep = ((BlockLevelLayoutManager) getParent()).getKeepTogether();
        } else if (getParent() instanceof InlineLayoutManager) {
            if (((InlineLayoutManager) getParent()).mustKeepTogether()) {
                keep = Keep.KEEP_ALWAYS;
            }
            // TODO Fix me
            // strength = ((InlineLayoutManager)
            // getParent()).getKeepTogetherStrength();
        }
        return keep;
    }

    /** {@inheritDoc} */
    @Override
    public boolean mustKeepTogether() {
        return !getKeepTogether().isAuto();
    }

    /** {@inheritDoc} */
    @Override
    public boolean mustKeepWithPrevious() {
        return !getKeepWithPrevious().isAuto();
    }

    /** {@inheritDoc} */
    @Override
    public boolean mustKeepWithNext() {
        return !getKeepWithNext().isAuto();
    }

    /** {@inheritDoc} */
    @Override
    public Keep getKeepTogether() {
        Keep keep = Keep.getKeep(getKeepTogetherProperty());
        keep = keep.compare(getParentKeepTogether());
        return keep;
    }

    /** {@inheritDoc} */
    @Override
    public Keep getKeepWithPrevious() {
        return Keep.getKeep(getKeepWithPreviousProperty());
    }

    /** {@inheritDoc} */
    @Override
    public Keep getKeepWithNext() {
        return Keep.getKeep(getKeepWithNextProperty());
    }

    /**
     * {@inheritDoc} Default implementation throws a
     * {@link IllegalStateException}. Must be implemented by the subclass, if
     * applicable.
     */
    @Override
    public KeepProperty getKeepTogetherProperty() {
        throw new IllegalStateException();
    }

    /**
     * {@inheritDoc} Default implementation throws a
     * {@link IllegalStateException}. Must be implemented by the subclass, if
     * applicable.
     */
    @Override
    public KeepProperty getKeepWithPreviousProperty() {
        throw new IllegalStateException();
    }

    /**
     * {@inheritDoc} Default implementation throws a
     * {@link IllegalStateException}. Must be implemented by the subclass, if
     * applicable.
     */
    @Override
    public KeepProperty getKeepWithNextProperty() {
        throw new IllegalStateException();
    }

    /**
     * Adds the unresolved elements for border and padding to a layout context
     * so break possibilities can be properly constructed.
     *
     * @param context
     *            the layout context
     */
    protected void addPendingMarks(final LayoutContext context) {
        final CommonBorderPaddingBackground borderAndPadding = getBorderPaddingBackground();
        if (borderAndPadding != null) {
            if (borderAndPadding.getBorderBeforeWidth(false) > 0) {
                context.addPendingBeforeMark(new BorderElement(
                        getAuxiliaryPosition(), borderAndPadding.getBorderInfo(
                                CommonBorderPaddingBackground.BEFORE)
                                .getWidth(), RelSide.BEFORE, false, false, this));
            }
            if (borderAndPadding.getPaddingBefore(false, this) > 0) {
                context.addPendingBeforeMark(new PaddingElement(
                        getAuxiliaryPosition(),
                        borderAndPadding
                        .getPaddingLengthProperty(CommonBorderPaddingBackground.BEFORE),
                        RelSide.BEFORE, false, false, this));
            }
            if (borderAndPadding.getBorderAfterWidth(false) > 0) {
                context.addPendingAfterMark(new BorderElement(
                        getAuxiliaryPosition(),
                        borderAndPadding.getBorderInfo(
                                CommonBorderPaddingBackground.AFTER).getWidth(),
                                RelSide.AFTER, false, false, this));
            }
            if (borderAndPadding.getPaddingAfter(false, this) > 0) {
                context.addPendingAfterMark(new PaddingElement(
                        getAuxiliaryPosition(),
                        borderAndPadding
                        .getPaddingLengthProperty(CommonBorderPaddingBackground.AFTER),
                        RelSide.AFTER, false, false, this));
            }
        }
    }

    /** @return the border, padding and background info structure */
    private CommonBorderPaddingBackground getBorderPaddingBackground() {
        if (this.fobj instanceof org.apache.fop.fo.flow.Block) {
            return ((org.apache.fop.fo.flow.Block) this.fobj)
                    .getCommonBorderPaddingBackground();
        } else if (this.fobj instanceof org.apache.fop.fo.flow.BlockContainer) {
            return ((org.apache.fop.fo.flow.BlockContainer) this.fobj)
                    .getCommonBorderPaddingBackground();
        } else if (this.fobj instanceof org.apache.fop.fo.flow.ListBlock) {
            return ((org.apache.fop.fo.flow.ListBlock) this.fobj)
                    .getCommonBorderPaddingBackground();
        } else if (this.fobj instanceof org.apache.fop.fo.flow.ListItem) {
            return ((org.apache.fop.fo.flow.ListItem) this.fobj)
                    .getCommonBorderPaddingBackground();
        } else if (this.fobj instanceof org.apache.fop.fo.flow.table.Table) {
            return ((org.apache.fop.fo.flow.table.Table) this.fobj)
                    .getCommonBorderPaddingBackground();
        } else {
            return null;
        }
    }

    /** @return the space-before property */
    private SpaceProperty getSpaceBeforeProperty() {
        if (this.fobj instanceof org.apache.fop.fo.flow.Block) {
            return ((org.apache.fop.fo.flow.Block) this.fobj)
                    .getCommonMarginBlock().spaceBefore;
        } else if (this.fobj instanceof org.apache.fop.fo.flow.BlockContainer) {
            return ((org.apache.fop.fo.flow.BlockContainer) this.fobj)
                    .getCommonMarginBlock().spaceBefore;
        } else if (this.fobj instanceof org.apache.fop.fo.flow.ListBlock) {
            return ((org.apache.fop.fo.flow.ListBlock) this.fobj)
                    .getCommonMarginBlock().spaceBefore;
        } else if (this.fobj instanceof org.apache.fop.fo.flow.ListItem) {
            return ((org.apache.fop.fo.flow.ListItem) this.fobj)
                    .getCommonMarginBlock().spaceBefore;
        } else if (this.fobj instanceof org.apache.fop.fo.flow.table.Table) {
            return ((org.apache.fop.fo.flow.table.Table) this.fobj)
                    .getCommonMarginBlock().spaceBefore;
        } else {
            return null;
        }
    }

    /** @return the space-after property */
    private SpaceProperty getSpaceAfterProperty() {
        if (this.fobj instanceof org.apache.fop.fo.flow.Block) {
            return ((org.apache.fop.fo.flow.Block) this.fobj)
                    .getCommonMarginBlock().spaceAfter;
        } else if (this.fobj instanceof org.apache.fop.fo.flow.BlockContainer) {
            return ((org.apache.fop.fo.flow.BlockContainer) this.fobj)
                    .getCommonMarginBlock().spaceAfter;
        } else if (this.fobj instanceof org.apache.fop.fo.flow.ListBlock) {
            return ((org.apache.fop.fo.flow.ListBlock) this.fobj)
                    .getCommonMarginBlock().spaceAfter;
        } else if (this.fobj instanceof org.apache.fop.fo.flow.ListItem) {
            return ((org.apache.fop.fo.flow.ListItem) this.fobj)
                    .getCommonMarginBlock().spaceAfter;
        } else if (this.fobj instanceof org.apache.fop.fo.flow.table.Table) {
            return ((org.apache.fop.fo.flow.table.Table) this.fobj)
                    .getCommonMarginBlock().spaceAfter;
        } else {
            return null;
        }
    }

    /**
     * Creates Knuth elements for before border padding and adds them to the
     * return list.
     *
     * @param returnList
     *            return list to add the additional elements to
     * @param isFirst
     *            true if this is the first time a layout manager instance needs
     *            to generate border and padding
     */
    protected void addKnuthElementsForBorderPaddingBefore(
            final List returnList, final boolean isFirst) {
        // Border and Padding (before)
        final CommonBorderPaddingBackground borderAndPadding = getBorderPaddingBackground();
        if (borderAndPadding != null) {
            if (borderAndPadding.getBorderBeforeWidth(false) > 0) {
                returnList.add(new BorderElement(getAuxiliaryPosition(),
                        borderAndPadding.getBorderInfo(
                                CommonBorderPaddingBackground.BEFORE)
                                .getWidth(), RelSide.BEFORE, isFirst, false,
                                this));
            }
            if (borderAndPadding.getPaddingBefore(false, this) > 0) {
                returnList
                .add(new PaddingElement(
                        getAuxiliaryPosition(),
                        borderAndPadding
                        .getPaddingLengthProperty(CommonBorderPaddingBackground.BEFORE),
                        RelSide.BEFORE, isFirst, false, this));
            }
        }
    }

    /**
     * Creates Knuth elements for after border padding and adds them to the
     * return list.
     *
     * @param returnList
     *            return list to add the additional elements to
     * @param isLast
     *            true if this is the last time a layout manager instance needs
     *            to generate border and padding
     */
    protected void addKnuthElementsForBorderPaddingAfter(final List returnList,
            final boolean isLast) {
        // Border and Padding (after)
        final CommonBorderPaddingBackground borderAndPadding = getBorderPaddingBackground();
        if (borderAndPadding != null) {
            if (borderAndPadding.getPaddingAfter(false, this) > 0) {
                returnList
                .add(new PaddingElement(
                        getAuxiliaryPosition(),
                        borderAndPadding
                        .getPaddingLengthProperty(CommonBorderPaddingBackground.AFTER),
                        RelSide.AFTER, false, isLast, this));
            }
            if (borderAndPadding.getBorderAfterWidth(false) > 0) {
                returnList
                .add(new BorderElement(getAuxiliaryPosition(),
                        borderAndPadding.getBorderInfo(
                                CommonBorderPaddingBackground.AFTER)
                                .getWidth(), RelSide.AFTER, false,
                                isLast, this));
            }
        }
    }

    /**
     * Creates Knuth elements for break-before and adds them to the return list.
     *
     * @param returnList
     *            return list to add the additional elements to
     * @param context
     *            the layout context
     * @return true if an element has been added due to a break-before.
     */
    protected boolean addKnuthElementsForBreakBefore(final List returnList,
            final LayoutContext context) {
        final int breakBefore = getBreakBefore();
        if (breakBefore == EN_PAGE || breakBefore == EN_COLUMN
                || breakBefore == EN_EVEN_PAGE || breakBefore == EN_ODD_PAGE) {
            // return a penalty element, representing a forced page break
            returnList.add(new BreakElement(getAuxiliaryPosition(), 0,
                    -KnuthElement.INFINITE, breakBefore, context));
            return true;
        } else {
            return false;
        }
    }

    /**
     * Returns the break-before value of the current formatting object.
     *
     * @return the break-before value (Constants.EN_*)
     */
    private int getBreakBefore() {
        int breakBefore = EN_AUTO;
        if (this.fobj instanceof BreakPropertySet) {
            breakBefore = ((BreakPropertySet) this.fobj).getBreakBefore();
        }
        if (true /* uncomment to only partially merge: && breakBefore != EN_AUTO */) {
            final LayoutManager lm = getChildLM();
            // It is assumed this is only called when the first LM is active.
            if (lm instanceof BlockStackingLayoutManager) {
                final BlockStackingLayoutManager bslm = (BlockStackingLayoutManager) lm;
                breakBefore = BreakUtil.compareBreakClasses(breakBefore,
                        bslm.getBreakBefore());
            }
        }
        return breakBefore;
    }

    /**
     * Creates Knuth elements for break-after and adds them to the return list.
     *
     * @param returnList
     *            return list to add the additional elements to
     * @param context
     *            the layout context
     * @return true if an element has been added due to a break-after.
     */
    protected boolean addKnuthElementsForBreakAfter(final List returnList,
            final LayoutContext context) {
        int breakAfter = -1;
        if (this.fobj instanceof BreakPropertySet) {
            breakAfter = ((BreakPropertySet) this.fobj).getBreakAfter();
        }
        if (breakAfter == EN_PAGE || breakAfter == EN_COLUMN
                || breakAfter == EN_EVEN_PAGE || breakAfter == EN_ODD_PAGE) {
            // add a penalty element, representing a forced page break
            returnList.add(new BreakElement(getAuxiliaryPosition(), 0,
                    -KnuthElement.INFINITE, breakAfter, context));
            return true;
        } else {
            return false;
        }
    }

    /**
     * Creates Knuth elements for space-before and adds them to the return list.
     *
     * @param returnList
     *            return list to add the additional elements to
     * @param alignment
     *            vertical alignment
     */
    protected void addKnuthElementsForSpaceBefore(final List returnList/*
     * ,
     * Position
     * returnPosition
     */,
     final int alignment) {
        final SpaceProperty spaceBefore = getSpaceBeforeProperty();
        // append elements representing space-before
        if (spaceBefore != null
                && !(spaceBefore.getMinimum(this).getLength().getValue(this) == 0 && spaceBefore
                .getMaximum(this).getLength().getValue(this) == 0)) {
            returnList.add(new SpaceElement(getAuxiliaryPosition(),
                    spaceBefore, RelSide.BEFORE, true, false, this));
        }
        /*
         * if (bpUnit > 0 || spaceBefore != null &&
         * !(spaceBefore.getMinimum(this).getLength().getValue(this) == 0 &&
         * spaceBefore.getMaximum(this).getLength().getValue(this) == 0)) { if
         * (spaceBefore != null && !spaceBefore.getSpace().isDiscard()) { // add
         * elements to prevent the glue to be discarded returnList.add(new
         * KnuthBox(0, getAuxiliaryPosition(), false)); returnList.add(new
         * KnuthPenalty(0, KnuthElement.INFINITE, false, getAuxiliaryPosition(),
         * false)); } if (bpUnit > 0) { returnList.add(new KnuthGlue(0, 0, 0,
         * BlockLevelLayoutManager.SPACE_BEFORE_ADJUSTMENT,
         * getAuxiliaryPosition(), true)); } else { //if (alignment ==
         * EN_JUSTIFY) { returnList.add(new KnuthGlue(
         * spaceBefore.getOptimum(this).getLength().getValue(this),
         * spaceBefore.getMaximum(this).getLength().getValue(this) -
         * spaceBefore.getOptimum(this).getLength().getValue(this),
         * spaceBefore.getOptimum(this).getLength().getValue(this) -
         * spaceBefore.getMinimum(this).getLength().getValue(this),
         * BlockLevelLayoutManager.SPACE_BEFORE_ADJUSTMENT,
         * getAuxiliaryPosition(), true)); // } else { // returnList.add(new
         * KnuthGlue( // spaceBefore.getOptimum().getLength().getValue(this), //
         * 0, 0, BlockLevelLayoutManager.SPACE_BEFORE_ADJUSTMENT, //
         * returnPosition, true)); } }
         */
    }

    /**
     * Creates Knuth elements for space-after and adds them to the return list.
     *
     * @param returnList
     *            return list to add the additional elements to
     * @param alignment
     *            vertical alignment
     */
    protected void addKnuthElementsForSpaceAfter(final List returnList/*
     * ,
     * Position
     * returnPosition
     */,
     final int alignment) {
        final SpaceProperty spaceAfter = getSpaceAfterProperty();
        // append elements representing space-after
        if (spaceAfter != null
                && !(spaceAfter.getMinimum(this).getLength().getValue(this) == 0 && spaceAfter
                .getMaximum(this).getLength().getValue(this) == 0)) {
            returnList.add(new SpaceElement(getAuxiliaryPosition(), spaceAfter,
                    RelSide.AFTER, false, true, this));
        }
        /*
         * if (bpUnit > 0 || spaceAfter != null &&
         * !(spaceAfter.getMinimum(this).getLength().getValue(this) == 0 &&
         * spaceAfter.getMaximum(this).getLength().getValue(this) == 0)) { if
         * (spaceAfter != null && !spaceAfter.getSpace().isDiscard()) {
         * returnList.add(new KnuthPenalty(0, KnuthElement.INFINITE, false,
         * getAuxiliaryPosition(), false)); } if (bpUnit > 0) {
         * returnList.add(new KnuthGlue(0, 0, 0,
         * BlockLevelLayoutManager.SPACE_AFTER_ADJUSTMENT,
         * getAuxiliaryPosition(), true)); } else { //if (alignment ==
         * EN_JUSTIFY) { returnList.add(new KnuthGlue(
         * spaceAfter.getOptimum(this).getLength().getValue(this),
         * spaceAfter.getMaximum(this).getLength().getValue(this) -
         * spaceAfter.getOptimum(this).getLength().getValue(this),
         * spaceAfter.getOptimum(this).getLength().getValue(this) -
         * spaceAfter.getMinimum(this).getLength().getValue(this),
         * BlockLevelLayoutManager.SPACE_AFTER_ADJUSTMENT,
         * getAuxiliaryPosition(), (!spaceAfter.getSpace().isDiscard()) ? false
         * : true)); // } else { // returnList.add(new KnuthGlue( //
         * spaceAfter.getOptimum().getLength().getValue(this), 0, 0, //
         * BlockLevelLayoutManager.SPACE_AFTER_ADJUSTMENT, returnPosition, //
         * (!spaceAfter.getSpace().isDiscard()) ? false : true)); } if
         * (spaceAfter != null && !spaceAfter.getSpace().isDiscard()) {
         * returnList.add(new KnuthBox(0, getAuxiliaryPosition(), true)); } }
         */
    }

    protected List createUnitElements(final List oldList) {
        // log.debug("Start conversion: " + oldList.size()
        // + " elements, space-before.min=" +
        // layoutProps.spaceBefore.getSpace().min
        // + " space-after.min=" + layoutProps.spaceAfter.getSpace().min);
        // add elements at the beginning and at the end of oldList
        // representing minimum spaces
        final LayoutManager lm = ((KnuthElement) oldList.get(0))
                .getLayoutManager();
        boolean bAddedBoxBefore = false;
        boolean bAddedBoxAfter = false;
        if (this.adjustedSpaceBefore > 0) {
            oldList.add(0, new KnuthBox(this.adjustedSpaceBefore, new Position(
                    lm), true));
            bAddedBoxBefore = true;
        }
        if (this.adjustedSpaceAfter > 0) {
            oldList.add(new KnuthBox(this.adjustedSpaceAfter, new Position(lm),
                    true));
            bAddedBoxAfter = true;
        }

        MinOptMax totalLength = MinOptMax.ZERO;
        final LinkedList newList = new LinkedList();

        // log.debug(" Prima scansione");
        // scan the list once to compute total min, opt and max length
        ListIterator oldListIterator = oldList.listIterator();
        while (oldListIterator.hasNext()) {
            final KnuthElement element = (KnuthElement) oldListIterator.next();
            if (element.isBox()) {
                totalLength = totalLength.plus(element.getWidth());
                // log.debug("box " + element.getWidth());
            } else if (element.isGlue()) {
                totalLength = totalLength.minusMin(element.getShrink());
                totalLength = totalLength.plusMax(element.getStretch());
                // leafValue = ((LeafPosition)
                // element.getPosition()).getLeafPos();
                // log.debug("glue " + element.getWidth() + " + "
                // + ((KnuthGlue) element).getStretch() + " - "
                // + ((KnuthGlue) element).getShrink());
            } else {
                // log.debug((((KnuthPenalty)element).getPenalty() ==
                // KnuthElement.INFINITE
                // ? "PENALTY " : "penalty ") + element.getWidth());
            }
        }
        // compute the total amount of "units"
        final MinOptMax totalUnits = MinOptMax.getInstance(
                neededUnits(totalLength.getMin()),
                neededUnits(totalLength.getOpt()),
                neededUnits(totalLength.getMax()));
        // log.debug(" totalLength= " + totalLength);
        // log.debug(" unita'= " + totalUnits);

        // log.debug(" Seconda scansione");
        // scan the list once more, stopping at every breaking point
        // in order to compute partial min, opt and max length
        // and create the new elements
        oldListIterator = oldList.listIterator();
        boolean prevIsBox;
        MinOptMax lengthBeforeBreak = MinOptMax.ZERO;
        MinOptMax lengthAfterBreak = totalLength;
        MinOptMax unitsBeforeBreak;
        MinOptMax unitsAfterBreak;
        MinOptMax unsuppressibleUnits = MinOptMax.ZERO;
        int firstIndex = 0;
        int lastIndex = -1;
        while (oldListIterator.hasNext()) {
            final KnuthElement element = (KnuthElement) oldListIterator.next();
            lastIndex++;
            if (element.isBox()) {
                lengthBeforeBreak = lengthBeforeBreak.plus(element.getWidth());
                lengthAfterBreak = lengthAfterBreak.minus(element.getWidth());
                prevIsBox = true;
            } else if (element.isGlue()) {
                lengthBeforeBreak = lengthBeforeBreak.minusMin(element
                        .getShrink());
                lengthAfterBreak = lengthAfterBreak
                        .plusMin(element.getShrink());
                lengthBeforeBreak = lengthBeforeBreak.plusMax(element
                        .getStretch());
                lengthAfterBreak = lengthAfterBreak.minusMax(element
                        .getStretch());
                prevIsBox = false;
            } else {
                lengthBeforeBreak = lengthBeforeBreak.plus(element.getWidth());
                prevIsBox = false;
            }

            // create the new elements
            if (element.isPenalty()
                    && element.getPenalty() < KnuthElement.INFINITE
                    || element.isGlue() && prevIsBox
                    || !oldListIterator.hasNext()) {
                // suppress elements after the breaking point
                int iStepsForward = 0;
                while (oldListIterator.hasNext()) {
                    final KnuthElement el = (KnuthElement) oldListIterator
                            .next();
                    iStepsForward++;
                    if (el.isGlue()) {
                        // suppressed glue
                        lengthAfterBreak = lengthAfterBreak.plusMin(el
                                .getShrink());
                        lengthAfterBreak = lengthAfterBreak.minusMax(el
                                .getStretch());
                    } else if (el.isPenalty()) {
                        // suppressed penalty, do nothing
                    } else {
                        // box, end of suppressions
                        break;
                    }
                }
                // compute the partial amount of "units" before and after the
                // break
                unitsBeforeBreak = MinOptMax.getInstance(
                        neededUnits(lengthBeforeBreak.getMin()),
                        neededUnits(lengthBeforeBreak.getOpt()),
                        neededUnits(lengthBeforeBreak.getMax()));
                unitsAfterBreak = MinOptMax.getInstance(
                        neededUnits(lengthAfterBreak.getMin()),
                        neededUnits(lengthAfterBreak.getOpt()),
                        neededUnits(lengthAfterBreak.getMax()));

                // rewind the iterator and lengthAfterBreak
                for (int i = 0; i < iStepsForward; i++) {
                    final KnuthElement el = (KnuthElement) oldListIterator
                            .previous();
                    if (el.isGlue()) {
                        lengthAfterBreak = lengthAfterBreak.minusMin(el
                                .getShrink());
                        lengthAfterBreak = lengthAfterBreak.plusMax(el
                                .getStretch());
                    }
                }

                // compute changes in length, stretch and shrink
                final int uLengthChange = unitsBeforeBreak.getOpt()
                        + unitsAfterBreak.getOpt() - totalUnits.getOpt();
                final int uStretchChange = unitsBeforeBreak.getStretch()
                        + unitsAfterBreak.getStretch()
                        - totalUnits.getStretch();
                final int uShrinkChange = unitsBeforeBreak.getShrink()
                        + unitsAfterBreak.getShrink() - totalUnits.getShrink();

                // compute the number of normal, stretch and shrink unit
                // that must be added to the new sequence
                final int uNewNormal = unitsBeforeBreak.getOpt()
                        - unsuppressibleUnits.getOpt();
                final int uNewStretch = unitsBeforeBreak.getStretch()
                        - unsuppressibleUnits.getStretch();
                final int uNewShrink = unitsBeforeBreak.getShrink()
                        - unsuppressibleUnits.getShrink();

                // log.debug("("
                // + unsuppressibleUnits.min + "-" + unsuppressibleUnits.opt +
                // "-"
                // + unsuppressibleUnits.max + ") "
                // + " -> " + unitsBeforeBreak.min + "-" + unitsBeforeBreak.opt
                // + "-"
                // + unitsBeforeBreak.max
                // + " + " + unitsAfterBreak.min + "-" + unitsAfterBreak.opt +
                // "-"
                // + unitsAfterBreak.max
                // + (uLengthChange != 0 ? " [length " + uLengthChange + "] " :
                // "")
                // + (uStretchChange != 0 ? " [stretch " + uStretchChange + "] "
                // : "")
                // + (uShrinkChange != 0 ? " [shrink " + uShrinkChange + "]" :
                // ""));

                // create the MappingPosition which will be stored in the new
                // elements
                // correct firstIndex and lastIndex
                int firstIndexCorrection = 0;
                int lastIndexCorrection = 0;
                if (bAddedBoxBefore) {
                    if (firstIndex != 0) {
                        firstIndexCorrection++;
                    }
                    lastIndexCorrection++;
                }
                if (bAddedBoxAfter && lastIndex == oldList.size() - 1) {
                    lastIndexCorrection++;
                }
                final MappingPosition mappingPos = new MappingPosition(this,
                        firstIndex - firstIndexCorrection, lastIndex
                        - lastIndexCorrection);

                // new box
                newList.add(new KnuthBox((uNewNormal - uLengthChange)
                        * this.bpUnit, mappingPos, false));
                unsuppressibleUnits = unsuppressibleUnits.plus(uNewNormal
                        - uLengthChange);
                // log.debug("        box " + (uNewNormal - uLengthChange));

                // new infinite penalty, glue and box, if necessary
                if (uNewStretch - uStretchChange > 0
                        || uNewShrink - uShrinkChange > 0) {
                    final int iStretchUnits = uNewStretch - uStretchChange > 0 ? uNewStretch
                            - uStretchChange
                            : 0;
                    final int iShrinkUnits = uNewShrink - uShrinkChange > 0 ? uNewShrink
                            - uShrinkChange
                            : 0;
                    newList.add(new KnuthPenalty(0, KnuthElement.INFINITE,
                            false, mappingPos, false));
                    newList.add(new KnuthGlue(0, iStretchUnits * this.bpUnit,
                            iShrinkUnits * this.bpUnit,
                            Adjustment.LINE_NUMBER_ADJUSTMENT, mappingPos,
                            false));
                    // log.debug("        PENALTY");
                    // log.debug("        glue 0 " + iStretchUnits + " " +
                    // iShrinkUnits);
                    unsuppressibleUnits = unsuppressibleUnits
                            .plusMax(iStretchUnits);
                    unsuppressibleUnits = unsuppressibleUnits
                            .minusMin(iShrinkUnits);
                    if (!oldListIterator.hasNext()) {
                        newList.add(new KnuthBox(0, mappingPos, false));
                        // log.debug("        box 0");
                    }
                }

                // new breaking sequence
                if (uStretchChange != 0 || uShrinkChange != 0) {
                    // new infinite penalty, glue, penalty and glue
                    newList.add(new KnuthPenalty(0, KnuthElement.INFINITE,
                            false, mappingPos, false));
                    newList.add(new KnuthGlue(0, uStretchChange * this.bpUnit,
                            uShrinkChange * this.bpUnit,
                            Adjustment.LINE_NUMBER_ADJUSTMENT, mappingPos,
                            false));
                    newList.add(new KnuthPenalty(uLengthChange * this.bpUnit,
                            0, false, element.getPosition(), false));
                    newList.add(new KnuthGlue(0, -uStretchChange * this.bpUnit,
                            -uShrinkChange * this.bpUnit,
                            Adjustment.LINE_NUMBER_ADJUSTMENT, mappingPos,
                            false));
                    // log.debug("        PENALTY");
                    // log.debug("        glue 0 " + uStretchChange + " " +
                    // uShrinkChange);
                    // log.debug("        penalty " + uLengthChange +
                    // " * unit");
                    // log.debug("        glue 0 " + (- uStretchChange) + " "
                    // + (- uShrinkChange));
                } else if (oldListIterator.hasNext()) {
                    // new penalty
                    newList.add(new KnuthPenalty(uLengthChange * this.bpUnit,
                            0, false, mappingPos, false));
                    // log.debug("        penalty " + uLengthChange +
                    // " * unit");
                }
                // update firstIndex
                firstIndex = lastIndex + 1;
            }

            if (element.isPenalty()) {
                lengthBeforeBreak = lengthBeforeBreak.minus(element.getWidth());
            }

        }

        // remove elements at the beginning and at the end of oldList
        // representing minimum spaces
        if (this.adjustedSpaceBefore > 0) {
            oldList.remove(0);
        }
        if (this.adjustedSpaceAfter > 0) {
            ListUtil.removeLast(oldList);
        }

        // if space-before.conditionality is "discard", correct newList
        boolean correctFirstElement = false;
        if (this.fobj instanceof org.apache.fop.fo.flow.Block) {
            correctFirstElement = ((org.apache.fop.fo.flow.Block) this.fobj)
                    .getCommonMarginBlock().spaceBefore.getSpace().isDiscard();
        }
        if (correctFirstElement) {
            // remove the wrong element
            final KnuthBox wrongBox = (KnuthBox) newList.removeFirst();
            // if this paragraph is at the top of a page, the space before
            // must be ignored; compute the length change
            final int decreasedLength = (neededUnits(totalLength.getOpt()) - neededUnits(totalLength
                    .getOpt() - this.adjustedSpaceBefore))
                    * this.bpUnit;
            // insert the correct elements
            newList.addFirst(new KnuthBox(
                    wrongBox.getWidth() - decreasedLength, wrongBox
                    .getPosition(), false));
            newList.addFirst(new KnuthGlue(decreasedLength, 0, 0,
                    Adjustment.SPACE_BEFORE_ADJUSTMENT, wrongBox.getPosition(),
                    false));
            // log.debug("        rimosso box " +
            // neededUnits(wrongBox.getWidth()));
            // log.debug("        aggiunto glue " + neededUnits(decreasedLength)
            // + " 0 0");
            // log.debug("        aggiunto box " + neededUnits(
            // wrongBox.getWidth() - decreasedLength));
        }

        // if space-after.conditionality is "discard", correct newList
        boolean correctLastElement = false;
        if (this.fobj instanceof org.apache.fop.fo.flow.Block) {
            correctLastElement = ((org.apache.fop.fo.flow.Block) this.fobj)
                    .getCommonMarginBlock().spaceAfter.getSpace().isDiscard();
        }
        if (correctLastElement) {
            // remove the wrong element
            KnuthBox wrongBox = (KnuthBox) newList.removeLast();
            // if the old sequence is box(h) penalty(inf) glue(x,y,z) box(0)
            // (it cannot be parted and has some stretch or shrink)
            // the wrong box is the first one, not the last one
            final LinkedList preserveList = new LinkedList();
            if (wrongBox.getWidth() == 0) {
                preserveList.add(wrongBox);
                preserveList.addFirst(newList.removeLast());
                preserveList.addFirst(newList.removeLast());
                wrongBox = (KnuthBox) newList.removeLast();
            }

            // if this paragraph is at the bottom of a page, the space after
            // must be ignored; compute the length change
            final int decreasedLength = (neededUnits(totalLength.getOpt()) - neededUnits(totalLength
                    .getOpt() - this.adjustedSpaceAfter))
                    * this.bpUnit;
            // insert the correct box
            newList.addLast(new KnuthBox(wrongBox.getWidth() - decreasedLength,
                    wrongBox.getPosition(), false));
            // add preserved elements
            if (!preserveList.isEmpty()) {
                newList.addAll(preserveList);
            }
            // insert the correct glue
            newList.addLast(new KnuthGlue(decreasedLength, 0, 0,
                    Adjustment.SPACE_AFTER_ADJUSTMENT, wrongBox.getPosition(),
                    false));
            // log.debug("        rimosso box " +
            // neededUnits(wrongBox.getWidth()));
            // log.debug("        aggiunto box " + neededUnits(
            // wrongBox.getWidth() - decreasedLength));
            // log.debug("        aggiunto glue " + neededUnits(decreasedLength)
            // + " 0 0");
        }

        return newList;
    }

    protected static class StackingIter extends PositionIterator {
        StackingIter(final Iterator parentIter) {
            super(parentIter);
        }

        @Override
        protected LayoutManager getLM(final Object nextObj) {
            return ((Position) nextObj).getLM();
        }

        @Override
        protected Position getPos(final Object nextObj) {
            return (Position) nextObj;
        }
    }

    protected static class MappingPosition extends Position {
        private final int iFirstIndex;
        private final int iLastIndex;

        public MappingPosition(final LayoutManager lm, final int first,
                final int last) {
            super(lm);
            this.iFirstIndex = first;
            this.iLastIndex = last;
        }

        public int getFirstIndex() {
            return this.iFirstIndex;
        }

        public int getLastIndex() {
            return this.iLastIndex;
        }
    }

    /**
     * "wrap" the Position inside each element moving the elements from
     * SourceList to targetList
     *
     * @param sourceList
     *            source list
     * @param targetList
     *            target list receiving the wrapped position elements
     */
    protected void wrapPositionElements(final List sourceList,
            final List targetList) {
        wrapPositionElements(sourceList, targetList, false);
    }

    /**
     * "wrap" the Position inside each element moving the elements from
     * SourceList to targetList
     *
     * @param sourceList
     *            source list
     * @param targetList
     *            target list receiving the wrapped position elements
     * @param force
     *            if true, every Position is wrapped regardless of its LM of
     *            origin
     */
    protected void wrapPositionElements(final List sourceList,
            final List targetList, final boolean force) {

        final ListIterator listIter = sourceList.listIterator();
        Object tempElement;
        while (listIter.hasNext()) {
            tempElement = listIter.next();
            if (tempElement instanceof ListElement) {
                wrapPositionElement((ListElement) tempElement, targetList,
                        force);
            } else if (tempElement instanceof List) {
                wrapPositionElements((List) tempElement, targetList, force);
            }
        }
    }

    /**
     * "wrap" the Position inside the given element and add it to the target
     * list.
     *
     * @param el
     *            the list element
     * @param targetList
     *            target list receiving the wrapped position elements
     * @param force
     *            if true, every Position is wrapped regardless of its LM of
     *            origin
     */
    protected void wrapPositionElement(final ListElement el,
            final List targetList, final boolean force) {
        if (force || el.getLayoutManager() != this) {
            el.setPosition(notifyPos(new NonLeafPosition(this, el.getPosition())));
        }
        targetList.add(el);
    }

    /** @return the sum of start-indent and end-indent */
    protected int getIPIndents() {
        return this.startIndent + this.endIndent;
    }

    /**
     * Returns the IPD of the content area
     *
     * @return the IPD of the content area
     */
    @Override
    public int getContentAreaIPD() {
        return this.contentAreaIPD;
    }

    /**
     * Sets the IPD of the content area
     *
     * @param contentAreaIPD
     *            the IPD of the content area
     */
    protected void setContentAreaIPD(final int contentAreaIPD) {
        this.contentAreaIPD = contentAreaIPD;
    }

    /**
     * Returns the BPD of the content area
     *
     * @return the BPD of the content area
     */
    @Override
    public int getContentAreaBPD() {
        return -1;
    }

    /** {@inheritDoc} */
    @Override
    public void reset() {
        super.reset();
        this.breakBeforeServed = false;
        this.firstVisibleMarkServed = false;
        // TODO startIndent, endIndent
    }

}
