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

/* $Id: AbstractBreaker.java 915406 2010-02-23 16:13:59Z vhennebert $ */

package org.apache.fop.layoutmgr;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.ListIterator;

import lombok.extern.slf4j.Slf4j;

import org.apache.fop.events.EventBroadcaster;
import org.apache.fop.fo.Constants;
import org.apache.fop.layoutmgr.BreakingAlgorithm.KnuthNode;
import org.apache.fop.traits.MinOptMax;
import org.apache.fop.util.ListUtil;

/**
 * Abstract base class for breakers (page breakers, static region handlers
 * etc.).
 */
@Slf4j
public abstract class AbstractBreaker {

    public static class PageBreakPosition extends LeafPosition {
        double bpdAdjust; // Percentage to adjust (stretch or shrink)
        int difference;
        int footnoteFirstListIndex;
        int footnoteFirstElementIndex;
        int footnoteLastListIndex;
        int footnoteLastElementIndex;

        PageBreakPosition(final LayoutManager lm, final int breakIndex,
                final int ffli, final int ffei, final int flli, final int flei,
                final double bpdA, final int diff) {
            super(lm, breakIndex);
            this.bpdAdjust = bpdA;
            this.difference = diff;
            this.footnoteFirstListIndex = ffli;
            this.footnoteFirstElementIndex = ffei;
            this.footnoteLastListIndex = flli;
            this.footnoteLastElementIndex = flei;
        }
    }

    /**
     * Helper method, mainly used to improve debug/trace output
     *
     * @param breakClassId
     *            the {@link Constants} enum value.
     * @return the break class name
     */
    static String getBreakClassName(final int breakClassId) {
        switch (breakClassId) {
        case Constants.EN_ALL:
            return "ALL";
        case Constants.EN_ANY:
            return "ANY";
        case Constants.EN_AUTO:
            return "AUTO";
        case Constants.EN_COLUMN:
            return "COLUMN";
        case Constants.EN_EVEN_PAGE:
            return "EVEN PAGE";
        case Constants.EN_LINE:
            return "LINE";
        case Constants.EN_NONE:
            return "NONE";
        case Constants.EN_ODD_PAGE:
            return "ODD PAGE";
        case Constants.EN_PAGE:
            return "PAGE";
        default:
            return "??? (" + String.valueOf(breakClassId) + ")";
        }
    }

    /**
     * Helper class, extending the functionality of the basic
     * {@link BlockKnuthSequence}.
     */
    public class BlockSequence extends BlockKnuthSequence {

        /**
         *
         */
        private static final long serialVersionUID = -6237054309058615341L;
        /** Number of elements to ignore at the beginning of the list. */
        public int ignoreAtStart = 0;
        /** Number of elements to ignore at the end of the list. */
        public int ignoreAtEnd = 0;

        /**
         * startOn represents where on the page/which page layout should start
         * for this BlockSequence. Acceptable values: Constants.EN_ANY (can
         * continue from finished location of previous BlockSequence?),
         * EN_COLUMN, EN_ODD_PAGE, EN_EVEN_PAGE.
         */
        private final int startOn;

        private final int displayAlign;

        /**
         * Creates a new BlockSequence.
         *
         * @param startOn
         *            the kind of page the sequence should start on. One of
         *            {@link Constants#EN_ANY}, {@link Constants#EN_COLUMN},
         *            {@link Constants#EN_ODD_PAGE}, or
         *            {@link Constants#EN_EVEN_PAGE}.
         * @param displayAlign
         *            the value for the display-align property
         */
        public BlockSequence(final int startOn, final int displayAlign) {
            super();
            this.startOn = startOn;
            this.displayAlign = displayAlign;
        }

        /**
         * @return the kind of page the sequence should start on. One of
         *         {@link Constants#EN_ANY}, {@link Constants#EN_COLUMN},
         *         {@link Constants#EN_ODD_PAGE}, or
         *         {@link Constants#EN_EVEN_PAGE}.
         */
        public int getStartOn() {
            return this.startOn;
        }

        /** @return the value for the display-align property */
        public int getDisplayAlign() {
            return this.displayAlign;
        }

        /**
         * Finalizes a Knuth sequence.
         *
         * @return a finalized sequence.
         */
        @Override
        public KnuthSequence endSequence() {
            return endSequence(null);
        }

        /**
         * Finalizes a Knuth sequence.
         *
         * @param breakPosition
         *            a Position instance for the last penalty (may be null)
         * @return a finalized sequence.
         */
        public KnuthSequence endSequence(final Position breakPosition) {
            // remove glue and penalty item at the end of the paragraph
            while (size() > this.ignoreAtStart
                    && !((KnuthElement) ListUtil.getLast(this)).isBox()) {
                ListUtil.removeLast(this);
            }
            if (size() > this.ignoreAtStart) {
                // add the elements representing the space at the end of the
                // last line
                // and the forced break
                if (getDisplayAlign() == Constants.EN_X_DISTRIBUTE
                        && isSinglePartFavored()) {
                    this.add(new KnuthPenalty(0, -KnuthElement.INFINITE, false,
                            breakPosition, false));
                    this.ignoreAtEnd = 1;
                } else {
                    this.add(new KnuthPenalty(0, KnuthElement.INFINITE, false,
                            null, false));
                    this.add(new KnuthGlue(0, 10000000, 0, null, false));
                    this.add(new KnuthPenalty(0, -KnuthElement.INFINITE, false,
                            breakPosition, false));
                    this.ignoreAtEnd = 3;
                }
                return this;
            } else {
                clear();
                return null;
            }
        }

        /**
         * Finalizes a this {@link BlockSequence}, adding a terminating
         * penalty-glue-penalty sequence
         *
         * @param breakPosition
         *            a Position instance pointing to the last penalty
         * @return the finalized {@link BlockSequence}
         */
        public BlockSequence endBlockSequence(final Position breakPosition) {
            final KnuthSequence temp = endSequence(breakPosition);
            if (temp != null) {
                final BlockSequence returnSequence = new BlockSequence(
                        this.startOn, this.displayAlign);
                returnSequence.addAll(temp);
                returnSequence.ignoreAtEnd = this.ignoreAtEnd;
                return returnSequence;
            } else {
                return null;
            }
        }

    }

    /** blockListIndex of the current BlockSequence in blockLists */
    private int blockListIndex = 0;

    private List<Object> blockLists = null;

    protected int alignment;
    private int alignmentLast;

    protected MinOptMax footnoteSeparatorLength = MinOptMax.ZERO;

    protected abstract int getCurrentDisplayAlign();

    protected abstract boolean hasMoreContent();

    protected abstract void addAreas(final PositionIterator posIter,
            final LayoutContext context);

    protected abstract LayoutManager getTopLevelLM();

    protected abstract LayoutManager getCurrentChildLM();

    /**
     * Controls the behaviour of the algorithm in cases where the first element
     * of a part overflows a line/page.
     *
     * @return true if the algorithm should try to send the element to the next
     *         line/page.
     */
    protected boolean isPartOverflowRecoveryActivated() {
        return true;
    }

    /**
     * @return true if one a single part should be produced if possible (ex. for
     *         block-containers)
     */
    protected boolean isSinglePartFavored() {
        return false;
    }

    /**
     * Returns the PageProvider if any. PageBreaker overrides this method
     * because each page may have a different available BPD which needs to be
     * accessible to the breaking algorithm.
     *
     * @return the applicable PageProvider, or null if not applicable
     */
    protected PageProvider getPageProvider() {
        return null;
    }

    /**
     * Creates and returns a PageBreakingLayoutListener for the
     * PageBreakingAlgorithm to notify about layout problems.
     *
     * @return the listener instance or null if no notifications are needed
     */
    protected PageBreakingAlgorithm.PageBreakingLayoutListener createLayoutListener() {
        return null;
    }

    /*
     * This method is to contain the logic to determine the LM's
     * getNextKnuthElements() implementation(s) that are to be called.
     * 
     * @return LinkedList of Knuth elements.
     */
    protected abstract <T> List<T> getNextKnuthElements(
            final LayoutContext context, final int alignment);

    protected List<KnuthElement> getNextKnuthElements(
            final LayoutContext context, final int alignment,
            final Position positionAtIPDChange, final LayoutManager restartAtLM) {
        throw new UnsupportedOperationException(
                "TODO: implement acceptable fallback");
    }

    /** @return true if there's no content that could be handled. */
    public boolean isEmpty() {
        return this.blockLists.isEmpty();
    }

    protected void startPart(final BlockSequence list, final int breakClass) {
        // nop
    }

    /**
     * This method is called when no content is available for a part. Used to
     * force empty pages.
     */
    protected void handleEmptyContent() {
        // nop
    }

    protected abstract void finishPart(final PageBreakingAlgorithm alg,
            final PageBreakPosition pbp);

    /**
     * Creates the top-level LayoutContext for the breaker operation.
     *
     * @return the top-level LayoutContext
     */
    protected LayoutContext createLayoutContext() {
        return new LayoutContext(0);
    }

    /**
     * Used to update the LayoutContext in subclasses prior to starting a new
     * element list.
     *
     * @param context
     *            the LayoutContext to update
     */
    protected void updateLayoutContext(final LayoutContext context) {
        // nop
    }

    /**
     * Used for debugging purposes. Notifies all registered observers about the
     * element list. Override to set different parameters.
     *
     * @param elementList
     *            the Knuth element list
     */
    protected void observeElementList(final List<ListElement> elementList) {
        ElementListObserver.observe(elementList, "breaker", null);
    }

    /**
     * Starts the page breaking process.
     *
     * @param flowBPD
     *            the constant available block-progression-dimension (used for
     *            every part)
     * @param autoHeight
     *            true if warnings about overflows should be disabled because
     *            the the BPD is really undefined (for footnote-separators, for
     *            example)
     */
    public void doLayout(final int flowBPD, final boolean autoHeight) {
        final LayoutContext childLC = createLayoutContext();
        childLC.setStackLimitBP(MinOptMax.getInstance(flowBPD));

        if (getCurrentDisplayAlign() == Constants.EN_X_FILL) {
            // EN_X_FILL is non-standard (by LF)
            this.alignment = Constants.EN_JUSTIFY;
        } else if (getCurrentDisplayAlign() == Constants.EN_X_DISTRIBUTE) {
            // EN_X_DISTRIBUTE is non-standard (by LF)
            this.alignment = Constants.EN_JUSTIFY;
        } else {
            this.alignment = Constants.EN_START;
        }
        this.alignmentLast = Constants.EN_START;
        if (isSinglePartFavored() && this.alignment == Constants.EN_JUSTIFY) {
            this.alignmentLast = Constants.EN_JUSTIFY;
        }
        childLC.setBPAlignment(this.alignment);

        BlockSequence blockList;
        this.blockLists = new ArrayList<>();

        log.debug("PLM> flow BPD =" + flowBPD);

        // *** Phase 1: Get Knuth elements ***
        int nextSequenceStartsOn = Constants.EN_ANY;
        while (hasMoreContent()) {
            this.blockLists.clear();

            nextSequenceStartsOn = getNextBlockList(childLC,
                    nextSequenceStartsOn);

            // *** Phase 2: Alignment and breaking ***
            log.debug("PLM> blockLists.size() = " + this.blockLists.size());
            for (this.blockListIndex = 0; this.blockListIndex < this.blockLists
                    .size(); this.blockListIndex++) {
                blockList = (BlockSequence) this.blockLists
                        .get(this.blockListIndex);

                // debug code start
                if (log.isDebugEnabled()) {
                    log.debug("  blockListIndex = " + this.blockListIndex);
                    log.debug("  sequence starts on "
                            + getBreakClassName(blockList.startOn));
                }
                observeElementList(blockList);
                // debug code end

                log.debug("PLM> start of algorithm ("
                        + this.getClass().getName() + "), flow BPD =" + flowBPD);
                final PageBreakingAlgorithm alg = new PageBreakingAlgorithm(
                        getTopLevelLM(), getPageProvider(),
                        createLayoutListener(), this.alignment,
                        this.alignmentLast, this.footnoteSeparatorLength,
                        isPartOverflowRecoveryActivated(), autoHeight,
                        isSinglePartFavored());

                BlockSequence effectiveList;
                if (getCurrentDisplayAlign() == Constants.EN_X_FILL) {
                    /* justification */
                    effectiveList = justifyBoxes(blockList, alg, flowBPD);
                } else {
                    /* no justification */
                    effectiveList = blockList;
                }

                alg.setConstantLineWidth(flowBPD);
                final int optimalPageCount = alg.findBreakingPoints(
                        effectiveList, 1, true, BreakingAlgorithm.ALL_BREAKS);
                if (alg.getIPDdifference() != 0) {
                    final KnuthNode optimalBreak = alg
                            .getBestNodeBeforeIPDChange();
                    int positionIndex = optimalBreak.position;
                    final KnuthElement elementAtBreak = alg
                            .getElement(positionIndex);
                    Position positionAtBreak = elementAtBreak.getPosition();
                    if (!(positionAtBreak instanceof SpaceResolver.SpaceHandlingBreakPosition)) {
                        throw new UnsupportedOperationException(
                                "Don't know how to restart at position"
                                        + positionAtBreak);
                    }
                    /*
                     * Retrieve the original position wrapped into this space
                     * position
                     */
                    positionAtBreak = positionAtBreak.getPosition();
                    LayoutManager restartAtLM = null;
                    List<KnuthElement> firstElements = Collections.emptyList();
                    if (containsNonRestartableLM(positionAtBreak)) {
                        if (alg.getIPDdifference() > 0) {
                            final EventBroadcaster eventBroadcaster = getCurrentChildLM()
                                    .getFObj().getUserAgent()
                                    .getEventBroadcaster();
                            final BlockLevelEventProducer eventProducer = BlockLevelEventProducer.Provider
                                    .get(eventBroadcaster);
                            eventProducer
                                    .nonRestartableContentFlowingToNarrowerPage(this);
                        }
                        firstElements = new LinkedList<>();
                        boolean boxFound = false;
                        final Iterator<ListElement> iter = effectiveList
                                .listIterator(positionIndex + 1);
                        Position position = null;
                        while (iter.hasNext()
                                && (position == null || containsNonRestartableLM(position))) {
                            ++positionIndex;
                            final KnuthElement element = (KnuthElement) iter
                                    .next();
                            position = element.getPosition();
                            if (element.isBox()) {
                                boxFound = true;
                                firstElements.add(element);
                            } else if (boxFound) {
                                firstElements.add(element);
                            }
                        }
                        if (position instanceof SpaceResolver.SpaceHandlingBreakPosition) {
                            /*
                             * Retrieve the original position wrapped into this
                             * space position
                             */
                            positionAtBreak = position.getPosition();
                        } else {
                            positionAtBreak = null;
                        }
                    }
                    if (positionAtBreak != null
                            && positionAtBreak.getIndex() == -1) {
                        /*
                         * This is an indication that we are between two blocks
                         * (possibly surrounded by another block), not inside a
                         * paragraph.
                         */
                        Position position;
                        final Iterator<ListElement> iter = effectiveList
                                .listIterator(positionIndex + 1);
                        do {
                            final ListElement nextElement = iter.next();
                            position = nextElement.getPosition();
                        } while (position == null
                                || position instanceof SpaceResolver.SpaceHandlingPosition
                                || position instanceof SpaceResolver.SpaceHandlingBreakPosition
                                && position.getPosition().getIndex() == -1);
                        final LayoutManager surroundingLM = positionAtBreak
                                .getLM();
                        while (position.getLM() != surroundingLM) {
                            position = position.getPosition();
                        }
                        restartAtLM = position.getPosition().getLM();
                    }
                    log.trace("IPD changes after page " + optimalPageCount
                            + " at index " + optimalBreak.position);
                    addAreas(alg, optimalPageCount, blockList, effectiveList);

                    this.blockLists.clear();
                    this.blockListIndex = -1;
                    nextSequenceStartsOn = getNextBlockList(childLC,
                            Constants.EN_COLUMN, positionAtBreak, restartAtLM,
                            firstElements);
                } else {
                    log.debug("PLM> iOptPageCount= " + optimalPageCount
                            + " pageBreaks.size()= "
                            + alg.getPageBreaks().size());

                    // *** Phase 3: Add areas ***
                    doPhase3(alg, optimalPageCount, blockList, effectiveList);
                }
            }
        }

    }

    /**
     * Returns {@code true} if the given position or one of its descendants
     * corresponds to a non-restartable LM.
     *
     * @param position
     *            a position
     * @return {@code true} if there is a non-restartable LM in the hierarchy
     */
    private boolean containsNonRestartableLM(final Position position) {
        final LayoutManager lm = position.getLM();
        if (lm != null && !lm.isRestartable()) {
            return true;
        } else {
            final Position subPosition = position.getPosition();
            if (subPosition == null) {
                return false;
            } else {
                return containsNonRestartableLM(subPosition);
            }
        }
    }

    /**
     * Phase 3 of Knuth algorithm: Adds the areas
     *
     * @param alg
     *            PageBreakingAlgorithm instance which determined the breaks
     * @param partCount
     *            number of parts (pages) to be rendered
     * @param originalList
     *            original Knuth element list
     * @param effectiveList
     *            effective Knuth element list (after adjustments)
     */
    protected abstract void doPhase3(final PageBreakingAlgorithm alg,
            final int partCount, final BlockSequence originalList,
            final BlockSequence effectiveList);

    /**
     * Phase 3 of Knuth algorithm: Adds the areas
     *
     * @param alg
     *            PageBreakingAlgorithm instance which determined the breaks
     * @param partCount
     *            number of parts (pages) to be rendered
     * @param originalList
     *            original Knuth element list
     * @param effectiveList
     *            effective Knuth element list (after adjustments)
     */
    protected void addAreas(final PageBreakingAlgorithm alg,
            final int partCount, final BlockSequence originalList,
            final BlockSequence effectiveList) {
        addAreas(alg, 0, partCount, originalList, effectiveList);
    }

    /**
     * Phase 3 of Knuth algorithm: Adds the areas
     *
     * @param alg
     *            PageBreakingAlgorithm instance which determined the breaks
     * @param startPart
     *            index of the first part (page) to be rendered
     * @param partCount
     *            number of parts (pages) to be rendered
     * @param originalList
     *            original Knuth element list
     * @param effectiveList
     *            effective Knuth element list (after adjustments)
     */
    protected void addAreas(final PageBreakingAlgorithm alg,
            final int startPart, final int partCount,
            final BlockSequence originalList, final BlockSequence effectiveList) {
        LayoutContext childLC;
        // add areas
        ListIterator<ListElement> effectiveListIterator = effectiveList
                .listIterator();
        int startElementIndex = 0;
        int endElementIndex = 0;
        int lastBreak = -1;
        for (int p = startPart; p < startPart + partCount; p++) {
            final PageBreakPosition pbp = alg.getPageBreaks().get(p);

            // Check the last break position for forced breaks
            int lastBreakClass;
            if (p == 0) {
                lastBreakClass = effectiveList.getStartOn();
            } else {
                final ListElement lastBreakElement = effectiveList
                        .getElement(endElementIndex);
                if (lastBreakElement.isPenalty()) {
                    final KnuthPenalty pen = (KnuthPenalty) lastBreakElement;
                    lastBreakClass = pen.getBreakClass();
                } else {
                    lastBreakClass = Constants.EN_COLUMN;
                }
            }

            // the end of the new part
            endElementIndex = pbp.getLeafPos();

            // ignore the first elements added by the
            // PageSequenceLayoutManager
            startElementIndex += startElementIndex == 0 ? effectiveList.ignoreAtStart
                    : 0;

            log.debug("PLM> part: " + (p + 1) + ", start at pos "
                    + startElementIndex + ", break at pos " + endElementIndex
                    + ", break class = " + getBreakClassName(lastBreakClass));

            startPart(effectiveList, lastBreakClass);

            final int displayAlign = getCurrentDisplayAlign();

            // The following is needed by
            // SpaceResolver.performConditionalsNotification()
            // further down as there may be important Position elements in the
            // element list trailer
            final int notificationEndElementIndex = endElementIndex;

            // ignore the last elements added by the
            // PageSequenceLayoutManager
            endElementIndex -= endElementIndex == originalList.size() - 1 ? effectiveList.ignoreAtEnd
                    : 0;

            // ignore the last element in the page if it is a KnuthGlue
            // object
            if (((KnuthElement) effectiveList.get(endElementIndex)).isGlue()) {
                endElementIndex--;
            }

            // ignore KnuthGlue and KnuthPenalty objects
            // at the beginning of the line
            effectiveListIterator = effectiveList
                    .listIterator(startElementIndex);
            while (effectiveListIterator.hasNext()
                    && !((KnuthElement) effectiveListIterator.next()).isBox()) {
                startElementIndex++;
            }

            if (startElementIndex <= endElementIndex) {
                if (log.isDebugEnabled()) {
                    log.debug("     addAreas from " + startElementIndex
                            + " to " + endElementIndex);
                }
                childLC = new LayoutContext(0);
                // set the space adjustment ratio
                childLC.setSpaceAdjust(pbp.bpdAdjust);
                // add space before if display-align is center or bottom
                // add space after if display-align is distribute and
                // this is not the last page
                if (pbp.difference != 0 && displayAlign == Constants.EN_CENTER) {
                    childLC.setSpaceBefore(pbp.difference / 2);
                } else if (pbp.difference != 0
                        && displayAlign == Constants.EN_AFTER) {
                    childLC.setSpaceBefore(pbp.difference);
                } else if (pbp.difference != 0
                        && displayAlign == Constants.EN_X_DISTRIBUTE
                        && p < partCount - 1) {
                    // count the boxes whose width is not 0
                    int boxCount = 0;
                    effectiveListIterator = effectiveList
                            .listIterator(startElementIndex);
                    while (effectiveListIterator.nextIndex() <= endElementIndex) {
                        final KnuthElement tempEl = (KnuthElement) effectiveListIterator
                                .next();
                        if (tempEl.isBox() && tempEl.getWidth() > 0) {
                            boxCount++;
                        }
                    }
                    // split the difference
                    if (boxCount >= 2) {
                        childLC.setSpaceAfter(pbp.difference / (boxCount - 1));
                    }
                }

                /* *** *** non-standard extension *** *** */
                if (displayAlign == Constants.EN_X_FILL) {
                    final int averageLineLength = optimizeLineLength(
                            effectiveList, startElementIndex, endElementIndex);
                    if (averageLineLength != 0) {
                        childLC.setStackLimitBP(MinOptMax
                                .getInstance(averageLineLength));
                    }
                }
                /* *** *** non-standard extension *** *** */

                // Handle SpaceHandling(Break)Positions, see SpaceResolver!
                SpaceResolver.performConditionalsNotification(effectiveList,
                        startElementIndex, notificationEndElementIndex,
                        lastBreak);

                // Add areas now!
                addAreas(new KnuthPossPosIter(effectiveList, startElementIndex,
                        endElementIndex + 1), childLC);
            } else {
                // no content for this part
                handleEmptyContent();
            }

            finishPart(alg, pbp);

            lastBreak = endElementIndex;
            startElementIndex = pbp.getLeafPos() + 1;
        }
    }

    /**
     * Notifies the layout managers about the space and conditional length
     * situation based on the break decisions.
     *
     * @param effectiveList
     *            Element list to be painted
     * @param startElementIndex
     *            start index of the part
     * @param endElementIndex
     *            end index of the part
     * @param lastBreak
     *            index of the last break element
     */
    /**
     * Handles span changes reported through the <code>LayoutContext</code>.
     * Only used by the PSLM and called by <code>getNextBlockList()</code>.
     *
     * @param childLC
     *            the LayoutContext
     * @param nextSequenceStartsOn
     *            previous value for break handling
     * @return effective value for break handling
     */
    protected int handleSpanChange(final LayoutContext childLC,
            final int nextSequenceStartsOn) {
        return nextSequenceStartsOn;
    }

    /**
     * Gets the next block list (sequence) and adds it to a list of block lists
     * if it's not empty.
     *
     * @param childLC
     *            LayoutContext to use
     * @param nextSequenceStartsOn
     *            indicates on what page the next sequence should start
     * @return the page on which the next content should appear after a hard
     *         break
     */
    protected int getNextBlockList(final LayoutContext childLC,
            final int nextSequenceStartsOn) {
        return getNextBlockList(childLC, nextSequenceStartsOn, null, null, null);
    }

    /**
     * Gets the next block list (sequence) and adds it to a list of block lists
     * if it's not empty.
     *
     * @param childLC
     *            LayoutContext to use
     * @param nextSequenceStartsOn
     *            indicates on what page the next sequence should start
     * @param positionAtIPDChange
     *            last element on the part before an IPD change
     * @param restartAtLM
     *            the layout manager from which to restart, if IPD change occurs
     *            between two LMs
     * @param firstElements
     *            elements from non-restartable LMs on the new page
     * @return the page on which the next content should appear after a hard
     *         break
     */
    protected int getNextBlockList(final LayoutContext childLC,
            int nextSequenceStartsOn, final Position positionAtIPDChange,
            final LayoutManager restartAtLM,
            final List<KnuthElement> firstElements) {
        updateLayoutContext(childLC);
        // Make sure the span change signal is reset
        childLC.signalSpanChange(Constants.NOT_SET);

        BlockSequence blockList;
        List<KnuthElement> returnedList;
        if (firstElements == null) {
            returnedList = getNextKnuthElements(childLC, this.alignment);
        } else if (positionAtIPDChange == null) {
            /*
             * No restartable element found after changing IPD break. Simply add
             * the non-restartable elements found after the break.
             */
            returnedList = firstElements;
            /*
             * Remove the last 3 penalty-filler-forced break elements that were
             * added by the Knuth algorithm. They will be re-added later on.
             */
            final ListIterator<KnuthElement> iter = returnedList
                    .listIterator(returnedList.size());
            for (int i = 0; i < 3; ++i) {
                iter.previous();
                iter.remove();
            }
        } else {
            returnedList = getNextKnuthElements(childLC, this.alignment,
                    positionAtIPDChange, restartAtLM);
            returnedList.addAll(0, firstElements);
        }
        if (returnedList != null) {
            if (returnedList.isEmpty()) {
                nextSequenceStartsOn = handleSpanChange(childLC,
                        nextSequenceStartsOn);
                return nextSequenceStartsOn;
            }
            blockList = new BlockSequence(nextSequenceStartsOn,
                    getCurrentDisplayAlign());

            // Only implemented by the PSLM
            nextSequenceStartsOn = handleSpanChange(childLC,
                    nextSequenceStartsOn);

            Position breakPosition = null;
            if (ElementListUtils.endsWithForcedBreak(returnedList)) {
                final KnuthPenalty breakPenalty = (KnuthPenalty) ListUtil
                        .removeLast(returnedList);
                breakPosition = breakPenalty.getPosition();
                log.debug("PLM> break - "
                        + getBreakClassName(breakPenalty.getBreakClass()));
                switch (breakPenalty.getBreakClass()) {
                case Constants.EN_PAGE:
                    nextSequenceStartsOn = Constants.EN_ANY;
                    break;
                case Constants.EN_COLUMN:
                    // TODO Fix this when implementing multi-column layout
                    nextSequenceStartsOn = Constants.EN_COLUMN;
                    break;
                case Constants.EN_ODD_PAGE:
                    nextSequenceStartsOn = Constants.EN_ODD_PAGE;
                    break;
                case Constants.EN_EVEN_PAGE:
                    nextSequenceStartsOn = Constants.EN_EVEN_PAGE;
                    break;
                default:
                    throw new IllegalStateException("Invalid break class: "
                            + breakPenalty.getBreakClass());
                }
            }
            blockList.addAll(returnedList);
            final BlockSequence seq = blockList.endBlockSequence(breakPosition);
            if (seq != null) {
                this.blockLists.add(seq);
            }
        }
        return nextSequenceStartsOn;
    }

    /**
     * Returns the average width of all the lines in the given range.
     *
     * @param effectiveList
     *            effective block list to work on
     * @param startElementIndex
     *            index of the element starting the range
     * @param endElementIndex
     *            index of the element ending the range
     * @return the average line length, 0 if there's no content
     */
    private int optimizeLineLength(final KnuthSequence effectiveList,
            final int startElementIndex, final int endElementIndex) {
        // optimize line length
        int boxCount = 0;
        int accumulatedLineLength = 0;
        int greatestMinimumLength = 0;
        final ListIterator<ListElement> effectiveListIterator = effectiveList
                .listIterator(startElementIndex);
        while (effectiveListIterator.nextIndex() <= endElementIndex) {
            final KnuthElement tempEl = (KnuthElement) effectiveListIterator
                    .next();
            if (tempEl instanceof KnuthBlockBox) {
                final KnuthBlockBox blockBox = (KnuthBlockBox) tempEl;
                if (blockBox.getBPD() > 0) {
                    log.debug("PSLM> nominal length of line = "
                            + blockBox.getBPD());
                    log.debug("      range = " + blockBox.getIPDRange());
                    boxCount++;
                    accumulatedLineLength += ((KnuthBlockBox) tempEl).getBPD();
                }
                if (blockBox.getIPDRange().getMin() > greatestMinimumLength) {
                    greatestMinimumLength = blockBox.getIPDRange().getMin();
                }
            }
        }
        int averageLineLength = 0;
        if (accumulatedLineLength > 0 && boxCount > 0) {
            averageLineLength = accumulatedLineLength / boxCount;
            log.debug("Average line length = " + averageLineLength);
            if (averageLineLength < greatestMinimumLength) {
                averageLineLength = greatestMinimumLength;
                log.debug("  Correction to: " + averageLineLength);
            }
        }
        return averageLineLength;
    }

    /**
     * Justifies the boxes and returns them as a new KnuthSequence.
     *
     * @param blockList
     *            block list to justify
     * @param alg
     *            reference to the algorithm instance
     * @param availableBPD
     *            the available BPD
     * @return the effective list
     */
    private BlockSequence justifyBoxes(final BlockSequence blockList,
            final PageBreakingAlgorithm alg, final int availableBPD) {
        int iOptPageNumber;
        alg.setConstantLineWidth(availableBPD);
        iOptPageNumber = alg.findBreakingPoints(blockList, /* availableBPD, */
                1, true, BreakingAlgorithm.ALL_BREAKS);
        log.debug("PLM> iOptPageNumber= " + iOptPageNumber);

        //
        final ListIterator<ListElement> sequenceIterator = blockList
                .listIterator();
        final ListIterator<PageBreakPosition> breakIterator = alg
                .getPageBreaks().listIterator();
        KnuthElement thisElement = null;
        PageBreakPosition thisBreak;
        int adjustedDiff; // difference already adjusted
        int firstElementIndex;

        while (breakIterator.hasNext()) {
            thisBreak = breakIterator.next();
            if (log.isDebugEnabled()) {
                log.debug("| first page: break= " + thisBreak.getLeafPos()
                        + " difference= " + thisBreak.difference + " ratio= "
                        + thisBreak.bpdAdjust);
            }
            adjustedDiff = 0;

            // glue and penalty items at the beginning of the page must
            // be ignored:
            // the first element returned by sequenceIterator.next()
            // inside the
            // while loop must be a box
            KnuthElement firstElement;
            while (!(firstElement = (KnuthElement) sequenceIterator.next())
                    .isBox()) {
                //
                log.debug("PLM> ignoring glue or penalty element "
                        + "at the beginning of the sequence");
                if (firstElement.isGlue()) {
                    ((BlockLevelLayoutManager) firstElement.getLayoutManager())
                            .discardSpace((KnuthGlue) firstElement);
                }
            }
            firstElementIndex = sequenceIterator.previousIndex();
            sequenceIterator.previous();

            // scan the sub-sequence representing a page,
            // collecting information about potential adjustments
            MinOptMax lineNumberMaxAdjustment = MinOptMax.ZERO;
            MinOptMax spaceMaxAdjustment = MinOptMax.ZERO;
            double spaceAdjustmentRatio = 0.0;
            final LinkedList<KnuthGlue> blockSpacesList = new LinkedList<>();
            final LinkedList<KnuthElement> unconfirmedList = new LinkedList<>();
            final LinkedList<KnuthElement> adjustableLinesList = new LinkedList<>();
            boolean bBoxSeen = false;
            while (sequenceIterator.hasNext()
                    && sequenceIterator.nextIndex() <= thisBreak.getLeafPos()) {
                thisElement = (KnuthElement) sequenceIterator.next();
                if (thisElement.isGlue()) {
                    // glue elements are used to represent adjustable
                    // lines
                    // and adjustable spaces between blocks
                    final Adjustment adjustment = ((KnuthGlue) thisElement)
                            .getAdjustmentClass();
                    if (adjustment.equals(Adjustment.SPACE_BEFORE_ADJUSTMENT)
                            || adjustment
                                    .equals(Adjustment.SPACE_AFTER_ADJUSTMENT)) {
                        // potential space adjustment
                        // glue items before the first box or after the
                        // last one
                        // must be ignored
                        unconfirmedList.add(thisElement);
                    } else if (adjustment
                            .equals(Adjustment.LINE_NUMBER_ADJUSTMENT)) {
                        // potential line number adjustment
                        lineNumberMaxAdjustment = lineNumberMaxAdjustment
                                .plusMax(thisElement.getStretch());
                        lineNumberMaxAdjustment = lineNumberMaxAdjustment
                                .minusMin(thisElement.getShrink());
                        adjustableLinesList.add(thisElement);
                    } else if (adjustment
                            .equals(Adjustment.LINE_HEIGHT_ADJUSTMENT)) {
                        // potential line height adjustment
                    }
                } else if (thisElement.isBox()) {
                    if (!bBoxSeen) {
                        // this is the first box met in this page
                        bBoxSeen = true;
                    } else {
                        while (!unconfirmedList.isEmpty()) {
                            // glue items in unconfirmedList were not after
                            // the last box
                            // in this page; they must be added to
                            // blockSpaceList
                            final KnuthGlue blockSpace = (KnuthGlue) unconfirmedList
                                    .removeFirst();
                            spaceMaxAdjustment = spaceMaxAdjustment
                                    .plusMax(blockSpace.getStretch());
                            spaceMaxAdjustment = spaceMaxAdjustment
                                    .minusMin(blockSpace.getShrink());
                            blockSpacesList.add(blockSpace);
                        }
                    }
                }
            }
            log.debug("| line number adj= " + lineNumberMaxAdjustment);
            log.debug("| space adj      = " + spaceMaxAdjustment);

            if (thisElement.isPenalty() && thisElement.getWidth() > 0) {
                log.debug("  mandatory variation to the number of lines!");
                ((BlockLevelLayoutManager) thisElement.getLayoutManager())
                        .negotiateBPDAdjustment(thisElement.getWidth(),
                                thisElement);
            }

            if (thisBreak.bpdAdjust != 0 && thisBreak.difference > 0
                    && thisBreak.difference <= spaceMaxAdjustment.getMax()
                    || thisBreak.difference < 0
                    && thisBreak.difference >= spaceMaxAdjustment.getMin()) {
                // modify only the spaces between blocks
                spaceAdjustmentRatio = (double) thisBreak.difference
                        / (thisBreak.difference > 0 ? spaceMaxAdjustment
                                .getMax() : spaceMaxAdjustment.getMin());
                adjustedDiff += adjustBlockSpaces(blockSpacesList,
                        thisBreak.difference,
                        thisBreak.difference > 0 ? spaceMaxAdjustment.getMax()
                                : -spaceMaxAdjustment.getMin());
                log.debug("single space: "
                        + (adjustedDiff == thisBreak.difference
                        || thisBreak.bpdAdjust == 0 ? "ok" : "ERROR"));
            } else if (thisBreak.bpdAdjust != 0) {
                adjustedDiff += adjustLineNumbers(
                        adjustableLinesList,
                        thisBreak.difference,
                        thisBreak.difference > 0 ? lineNumberMaxAdjustment
                                .getMax() : -lineNumberMaxAdjustment.getMin());
                adjustedDiff += adjustBlockSpaces(
                        blockSpacesList,
                        thisBreak.difference - adjustedDiff,
                        thisBreak.difference - adjustedDiff > 0 ? spaceMaxAdjustment
                                .getMax() : -spaceMaxAdjustment.getMin());
                log.debug("lines and space: "
                        + (adjustedDiff == thisBreak.difference
                        || thisBreak.bpdAdjust == 0 ? "ok" : "ERROR"));

            }
        }

        // create a new sequence: the new elements will contain the
        // Positions
        // which will be used in the addAreas() phase
        final BlockSequence effectiveList = new BlockSequence(
                blockList.getStartOn(), blockList.getDisplayAlign());
        effectiveList.addAll(getCurrentChildLM().getChangedKnuthElements(
                blockList.subList(0, blockList.size() - blockList.ignoreAtEnd),
                /* 0, */0));
        effectiveList.endSequence();

        ElementListObserver.observe(effectiveList, "breaker-effective", null);

        alg.getPageBreaks().clear(); // Why this?
        return effectiveList;
    }

    private int adjustBlockSpaces(final LinkedList<KnuthGlue> spaceList,
            final int difference, final int total) {
        log.debug(
                "AdjustBlockSpaces: difference {} / {} on {} spaces in block",
                difference, total, spaceList.size());
        final ListIterator<KnuthGlue> spaceListIterator = spaceList
                .listIterator();
        int adjustedDiff = 0;
        int partial = 0;
        while (spaceListIterator.hasNext()) {
            final KnuthGlue blockSpace = spaceListIterator.next();
            partial += difference > 0 ? blockSpace.getStretch() : blockSpace
                    .getShrink();
            log.debug("available = {} / {}", partial, total);
            log.debug("competenza  = {} / {} ", (int) ((float) partial
                    * difference / total)
                    - adjustedDiff, difference);
            final int newAdjust = ((BlockLevelLayoutManager) blockSpace
                    .getLayoutManager())
                    .negotiateBPDAdjustment(
                            (int) ((float) partial * difference / total)
                                    - adjustedDiff, blockSpace);
            adjustedDiff += newAdjust;
        }
        return adjustedDiff;
    }

    private int adjustLineNumbers(final LinkedList<KnuthElement> lineList,
            final int difference, final int total) {
        if (log.isDebugEnabled()) {
            log.debug("AdjustLineNumbers: difference " + difference + " / "
                    + total + " on " + lineList.size() + " elements");
        }

        final ListIterator<KnuthElement> lineListIterator = lineList
                .listIterator();
        int adjustedDiff = 0;
        int partial = 0;
        while (lineListIterator.hasNext()) {
            final KnuthGlue line = (KnuthGlue) lineListIterator.next();
            partial += difference > 0 ? line.getStretch() : line.getShrink();
            final int newAdjust = ((BlockLevelLayoutManager) line
                    .getLayoutManager())
                    .negotiateBPDAdjustment(
                            (int) ((float) partial * difference / total)
                                    - adjustedDiff, line);
            adjustedDiff += newAdjust;
        }
        return adjustedDiff;
    }

}
