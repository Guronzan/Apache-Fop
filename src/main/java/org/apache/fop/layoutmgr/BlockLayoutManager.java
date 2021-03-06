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

/* $Id: BlockLayoutManager.java 893238 2009-12-22 17:20:51Z vhennebert $ */

package org.apache.fop.layoutmgr;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.ListIterator;
import java.util.Stack;

import lombok.extern.slf4j.Slf4j;

import org.apache.fop.area.Area;
import org.apache.fop.area.Block;
import org.apache.fop.area.LineArea;
import org.apache.fop.datatypes.Length;
import org.apache.fop.fo.properties.KeepProperty;
import org.apache.fop.fonts.Font;
import org.apache.fop.fonts.FontInfo;
import org.apache.fop.fonts.FontTriplet;
import org.apache.fop.layoutmgr.inline.InlineLevelLayoutManager;
import org.apache.fop.layoutmgr.inline.LineLayoutManager;
import org.apache.fop.traits.MinOptMax;
import org.apache.fop.traits.SpaceVal;

/**
 * LayoutManager for a block FO.
 */
@Slf4j
public class BlockLayoutManager extends BlockStackingLayoutManager implements
        ConditionalElementListener {

    private Block curBlockArea;

    /** Iterator over the child layout managers. */
    protected ListIterator<LayoutManager> proxyLMiter;

    private int lead = 12000;
    private Length lineHeight;
    private int follow = 2000;

    private boolean discardBorderBefore;
    private boolean discardBorderAfter;
    private boolean discardPaddingBefore;
    private boolean discardPaddingAfter;
    private MinOptMax effSpaceBefore;
    private MinOptMax effSpaceAfter;

    /**
     * Creates a new BlockLayoutManager.
     *
     * @param inBlock
     *            the block FO object to create the layout manager for.
     */
    public BlockLayoutManager(final org.apache.fop.fo.flow.Block inBlock) {
        super(inBlock);
        this.proxyLMiter = new ProxyLMiter();
    }

    /** {@inheritDoc} */
    @Override
    public void initialize() {
        super.initialize();
        final FontInfo fi = getBlockFO().getFOEventHandler().getFontInfo();
        final FontTriplet[] fontkeys = getBlockFO().getCommonFont()
                .getFontState(fi);
        final Font initFont = fi.getFontInstance(fontkeys[0], getBlockFO()
                .getCommonFont().fontSize.getValue(this));
        this.lead = initFont.getAscender();
        this.follow = -initFont.getDescender();
        // middleShift = -fs.getXHeight() / 2;
        this.lineHeight = getBlockFO().getLineHeight().getOptimum(this)
                .getLength();
        this.startIndent = getBlockFO().getCommonMarginBlock().startIndent
                .getValue(this);
        this.endIndent = getBlockFO().getCommonMarginBlock().endIndent
                .getValue(this);
        this.foSpaceBefore = new SpaceVal(
                getBlockFO().getCommonMarginBlock().spaceBefore, this)
                .getSpace();
        this.foSpaceAfter = new SpaceVal(
                getBlockFO().getCommonMarginBlock().spaceAfter, this)
                .getSpace();
        this.bpUnit = 0; // non-standard extension
        if (this.bpUnit == 0) {
            // use optimum space values
            this.adjustedSpaceBefore = getBlockFO().getCommonMarginBlock().spaceBefore
                    .getSpace().getOptimum(this).getLength().getValue(this);
            this.adjustedSpaceAfter = getBlockFO().getCommonMarginBlock().spaceAfter
                    .getSpace().getOptimum(this).getLength().getValue(this);
        } else {
            // use minimum space values
            this.adjustedSpaceBefore = getBlockFO().getCommonMarginBlock().spaceBefore
                    .getSpace().getMinimum(this).getLength().getValue(this);
            this.adjustedSpaceAfter = getBlockFO().getCommonMarginBlock().spaceAfter
                    .getSpace().getMinimum(this).getLength().getValue(this);
        }
    }

    /** {@inheritDoc} */
    @Override
    public List<ListElement> getNextKnuthElements(final LayoutContext context,
            final int alignment) {
        return getNextKnuthElements(context, alignment, null, null, null);
    }

    /** {@inheritDoc} */
    @Override
    public List<ListElement> getNextKnuthElements(final LayoutContext context,
            final int alignment, final Stack lmStack,
            final Position restartPosition, final LayoutManager restartAtLM) {
        resetSpaces();
        if (lmStack == null) {
            return super.getNextKnuthElements(context, alignment);
        } else {
            return super.getNextKnuthElements(context, alignment, lmStack,
                    restartPosition, restartAtLM);
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

    /**
     * Proxy iterator for Block LM. This iterator creates and holds the complete
     * list of child LMs. It uses fobjIter as its base iterator. Block LM's
     * createNextChildLMs uses this iterator as its base iterator.
     */
    protected class ProxyLMiter extends LMiter {

        /**
         * Constructs a proxy iterator for Block LM.
         */
        public ProxyLMiter() {
            super(BlockLayoutManager.this);
            this.listLMs = new ArrayList<>(10);
        }

        /**
         * @return true if there are more child lms
         */
        @Override
        public boolean hasNext() {
            return this.curPos < this.listLMs.size()
                    || createNextChildLMs(this.curPos);
        }

        /**
         * @param pos
         *            ...
         * @return true if new child lms were added
         */
        protected boolean createNextChildLMs(final int pos) {
            final List<LayoutManager> newLMs = createChildLMs(pos + 1
                    - this.listLMs.size());
            if (newLMs != null) {
                this.listLMs.addAll(newLMs);
            }
            return pos < this.listLMs.size();
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean createNextChildLMs(final int pos) {

        while (this.proxyLMiter.hasNext()) {
            final LayoutManager lm = this.proxyLMiter.next();
            if (lm instanceof InlineLevelLayoutManager) {
                final LineLayoutManager lineLM = createLineManager(lm);
                addChildLM(lineLM);
            } else {
                addChildLM(lm);
            }
            if (pos < this.childLMs.size()) {
                return true;
            }
        }
        return false;
    }

    /**
     * Create a new LineLM, and collect all consecutive inline generating LMs as
     * its child LMs.
     *
     * @param firstlm
     *            First LM in new LineLM
     * @return the newly created LineLM
     */
    private LineLayoutManager createLineManager(final LayoutManager firstlm) {
        final LineLayoutManager llm = new LineLayoutManager(getBlockFO(),
                this.lineHeight, this.lead, this.follow);
        final List<LayoutManager> inlines = new ArrayList<>();
        inlines.add(firstlm);
        while (this.proxyLMiter.hasNext()) {
            final LayoutManager lm = this.proxyLMiter.next();
            if (lm instanceof InlineLevelLayoutManager) {
                inlines.add(lm);
            } else {
                this.proxyLMiter.previous();
                break;
            }
        }
        llm.addChildLMs(inlines);
        return llm;
    }

    /** {@inheritDoc} */
    @Override
    public KeepProperty getKeepTogetherProperty() {
        return getBlockFO().getKeepTogether();
    }

    /** {@inheritDoc} */
    @Override
    public KeepProperty getKeepWithPreviousProperty() {
        return getBlockFO().getKeepWithPrevious();
    }

    /** {@inheritDoc} */
    @Override
    public KeepProperty getKeepWithNextProperty() {
        return getBlockFO().getKeepWithNext();
    }

    /** {@inheritDoc} */
    @Override
    public void addAreas(final PositionIterator parentIter,
            final LayoutContext layoutContext) {
        getParentArea(null);

        // if this will create the first block area in a page
        // and display-align is after or center, add space before
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
        PositionIterator childPosIter;

        // "unwrap" the NonLeafPositions stored in parentIter
        // and put them in a new list;
        final LinkedList<Position> positionList = new LinkedList<>();
        Position pos;
        boolean spaceBefore = false;
        boolean spaceAfter = false;
        Position firstPos = null;
        Position lastPos = null;
        while (parentIter.hasNext()) {
            pos = (Position) parentIter.next();
            // log.trace("pos = " + pos.getClass().getName() + "; " + pos);
            if (pos.getIndex() >= 0) {
                if (firstPos == null) {
                    firstPos = pos;
                }
                lastPos = pos;
            }
            Position innerPosition = pos;
            if (pos instanceof NonLeafPosition) {
                // Not all elements are wrapped
                innerPosition = pos.getPosition();
            }
            if (innerPosition == null) {
                // pos was created by this BlockLM and was inside an element
                // representing space before or after
                // this means the space was not discarded
                if (positionList.size() == 0) {
                    // pos was in the element representing space-before
                    spaceBefore = true;
                    // log.trace(" space before");
                } else {
                    // pos was in the element representing space-after
                    spaceAfter = true;
                    // log.trace(" space-after");
                }
            } else if (innerPosition.getLM() == this
                    && !(innerPosition instanceof MappingPosition)) {
                // pos was created by this BlockLM and was inside a penalty
                // allowing or forbidding a page break
                // nothing to do
                // log.trace(" penalty");
            } else {
                // innerPosition was created by another LM
                positionList.add(innerPosition);
                lastLM = innerPosition.getLM();
                // log.trace(" " + innerPosition.getClass().getName());
            }
        }

        addId();

        addMarkersToPage(true, isFirst(firstPos), isLast(lastPos));

        if (this.bpUnit == 0) {
            // the Positions in positionList were inside the elements
            // created by the LineLM
            childPosIter = new StackingIter(positionList.listIterator());
        } else {
            // the Positions in positionList were inside the elements
            // created by the BlockLM in the createUnitElements() method
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
            final LinkedList<KnuthElement> splitList = new LinkedList<>();
            int splitLength = 0;
            final int iFirst = ((MappingPosition) positionList.getFirst())
                    .getFirstIndex();
            final int iLast = ((MappingPosition) positionList.getLast())
                    .getLastIndex();
            // copy from storedList to splitList all the elements from
            // iFirst to iLast
            final ListIterator<ListElement> storedListIterator = this.storedList
                    .listIterator(iFirst);
            while (storedListIterator.nextIndex() <= iLast) {
                final KnuthElement element = (KnuthElement) storedListIterator
                        .next();
                // some elements in storedList (i.e. penalty items) were created
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
            if (spaceBefore && spaceAfter) {
                this.foSpaceBefore = new SpaceVal(getBlockFO()
                        .getCommonMarginBlock().spaceBefore, this).getSpace();
                this.foSpaceAfter = new SpaceVal(getBlockFO()
                        .getCommonMarginBlock().spaceAfter, this).getSpace();
                this.adjustedSpaceBefore = (neededUnits(splitLength
                        + this.foSpaceBefore.getMin()
                        + this.foSpaceAfter.getMin())
                        * this.bpUnit - splitLength) / 2;
                this.adjustedSpaceAfter = neededUnits(splitLength
                        + this.foSpaceBefore.getMin()
                        + this.foSpaceAfter.getMin())
                        * this.bpUnit - splitLength - this.adjustedSpaceBefore;
            } else if (spaceBefore) {
                this.adjustedSpaceBefore = neededUnits(splitLength
                        + this.foSpaceBefore.getMin())
                        * this.bpUnit - splitLength;
            } else {
                this.adjustedSpaceAfter = neededUnits(splitLength
                        + this.foSpaceAfter.getMin())
                        * this.bpUnit - splitLength;
            }
            // log.debug("spazio prima = " + adjustedSpaceBefore
            // + " spazio dopo = " + adjustedSpaceAfter + " totale = " +
            // (adjustedSpaceBefore + adjustedSpaceAfter + splitLength));
            childPosIter = new KnuthPossPosIter(splitList, 0, splitList.size());
            // }
        }

        while ((childLM = childPosIter.getNextChildLM()) != null) {
            // set last area flag
            lc.setFlags(LayoutContext.LAST_AREA, layoutContext.isLastArea()
                    && childLM == lastLM);
            lc.setStackLimitBP(layoutContext.getStackLimitBP());
            // Add the line areas to Area
            childLM.addAreas(childPosIter, lc);
        }

        addMarkersToPage(false, isFirst(firstPos), isLast(lastPos));

        TraitSetter.addPtr(this.curBlockArea, getBlockFO().getPtr()); // used
        // for
        // accessibility
        TraitSetter.addSpaceBeforeAfter(this.curBlockArea,
                layoutContext.getSpaceAdjust(), this.effSpaceBefore,
                this.effSpaceAfter);
        flush();

        this.curBlockArea = null;
        resetSpaces();

        // Notify end of block layout manager to the PSLM
        checkEndOfLayout(lastPos);
    }

    /**
     * Return an Area which can contain the passed childArea. The childArea may
     * not yet have any content, but it has essential traits set. In general, if
     * the LayoutManager already has an Area it simply returns it. Otherwise, it
     * makes a new Area of the appropriate class. It gets a parent area for its
     * area by calling its parent LM. Finally, based on the dimensions of the
     * parent area, it initializes its own area. This includes setting the
     * content IPD and the maximum BPD.
     *
     * @param childArea
     *            area to get the parent area for
     * @return the parent area
     */
    @Override
    public Area getParentArea(final Area childArea) {
        if (this.curBlockArea == null) {
            this.curBlockArea = new Block();

            this.curBlockArea.setIPD(super.getContentAreaIPD());

            TraitSetter.addBreaks(this.curBlockArea, getBlockFO()
                    .getBreakBefore(), getBlockFO().getBreakAfter());

            // Must get dimensions from parent area
            // Don't optimize this line away. It can have ugly side-effects.
            /* Area parentArea = */this.parentLayoutManager
                    .getParentArea(this.curBlockArea);

            // set traits
            TraitSetter.setProducerID(this.curBlockArea, getBlockFO().getId());
            TraitSetter.addBorders(this.curBlockArea, getBlockFO()
                    .getCommonBorderPaddingBackground(),
                    this.discardBorderBefore, this.discardBorderAfter, false,
                    false, this);
            TraitSetter.addPadding(this.curBlockArea, getBlockFO()
                    .getCommonBorderPaddingBackground(),
                    this.discardPaddingBefore, this.discardPaddingAfter, false,
                    false, this);
            TraitSetter.addMargins(this.curBlockArea, getBlockFO()
                    .getCommonBorderPaddingBackground(), this.startIndent,
                    this.endIndent, this);

            setCurrentArea(this.curBlockArea); // ??? for generic operations
        }
        return this.curBlockArea;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void addChildArea(final Area childArea) {
        if (this.curBlockArea != null) {
            if (childArea instanceof LineArea) {
                this.curBlockArea.addLineArea((LineArea) childArea);
            } else {
                this.curBlockArea.addBlock((Block) childArea);
            }
        }
    }

    /**
     * Force current area to be added to parent area. {@inheritDoc}
     */
    @Override
    protected void flush() {
        if (this.curBlockArea != null) {
            TraitSetter.addBackground(this.curBlockArea, getBlockFO()
                    .getCommonBorderPaddingBackground(), this);
            super.flush();
        }
    }

    /**
     * convenience method that returns the Block node
     *
     * @return the block node
     */
    protected org.apache.fop.fo.flow.Block getBlockFO() {
        return (org.apache.fop.fo.flow.Block) this.fobj;
    }

    // --------- Property Resolution related functions --------- //

    /**
     * Returns the IPD of the content area
     *
     * @return the IPD of the content area
     */
    @Override
    public int getContentAreaIPD() {
        if (this.curBlockArea != null) {
            return this.curBlockArea.getIPD();
        }
        return super.getContentAreaIPD();
    }

    /**
     * Returns the BPD of the content area
     *
     * @return the BPD of the content area
     */
    @Override
    public int getContentAreaBPD() {
        if (this.curBlockArea != null) {
            return this.curBlockArea.getBPD();
        }
        return -1;
    }

    /**
     * {@inheritDoc}
     */
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

    /** {@inheritDoc} */
    @Override
    public boolean isRestartable() {
        return true;
    }

}
