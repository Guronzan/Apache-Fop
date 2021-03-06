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

/* $Id: ListItemContentLayoutManager.java 893238 2009-12-22 17:20:51Z vhennebert $ */

package org.apache.fop.layoutmgr.list;

import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import org.apache.fop.area.Area;
import org.apache.fop.area.Block;
import org.apache.fop.fo.flow.AbstractListItemPart;
import org.apache.fop.fo.flow.ListItemBody;
import org.apache.fop.fo.flow.ListItemLabel;
import org.apache.fop.fo.properties.KeepProperty;
import org.apache.fop.layoutmgr.BlockStackingLayoutManager;
import org.apache.fop.layoutmgr.Keep;
import org.apache.fop.layoutmgr.LayoutContext;
import org.apache.fop.layoutmgr.LayoutManager;
import org.apache.fop.layoutmgr.ListElement;
import org.apache.fop.layoutmgr.NonLeafPosition;
import org.apache.fop.layoutmgr.Position;
import org.apache.fop.layoutmgr.PositionIterator;
import org.apache.fop.layoutmgr.SpaceResolver.SpaceHandlingBreakPosition;
import org.apache.fop.layoutmgr.TraitSetter;

/**
 * LayoutManager for a list-item-label or list-item-body FO.
 */
public class ListItemContentLayoutManager extends BlockStackingLayoutManager {

    private Block curBlockArea;

    private int xoffset;
    private int itemIPD;

    private static class StackingIter extends PositionIterator {
        StackingIter(final Iterator<Position> parentIter) {
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

    /**
     * Create a new Cell layout manager.
     *
     * @param node
     *            list-item-label node
     */
    public ListItemContentLayoutManager(final ListItemLabel node) {
        super(node);
    }

    /**
     * Create a new Cell layout manager.
     *
     * @param node
     *            list-item-body node
     */
    public ListItemContentLayoutManager(final ListItemBody node) {
        super(node);
    }

    /**
     * Convenience method.
     *
     * @return the ListBlock node
     */
    protected AbstractListItemPart getPartFO() {
        return (AbstractListItemPart) this.fobj;
    }

    /**
     * Set the x offset of this list item. This offset is used to set the
     * absolute position of the list item within the parent block area.
     *
     * @param off
     *            the x offset
     */
    public void setXOffset(final int off) {
        this.xoffset = off;
    }

    /** {@inheritDoc} */
    @Override
    public List<ListElement> getChangedKnuthElements(
            final List<ListElement> oldList, final int alignment) {
        // log.debug("  ListItemContentLayoutManager.getChanged>");
        return super.getChangedKnuthElements(oldList, alignment);
    }

    /**
     * Add the areas for the break points. The list item contains block stacking
     * layout managers that add block areas.
     *
     * @param parentIter
     *            the iterator of the break positions
     * @param layoutContext
     *            the layout context for adding the areas
     */
    @Override
    public void addAreas(final PositionIterator parentIter,
            final LayoutContext layoutContext) {
        getParentArea(null);

        addId();

        LayoutManager childLM;
        final LayoutContext lc = new LayoutContext(0);
        LayoutManager firstLM = null;
        LayoutManager lastLM = null;
        Position firstPos = null;
        Position lastPos = null;

        // "unwrap" the NonLeafPositions stored in parentIter
        // and put them in a new list;
        final LinkedList<Position> positionList = new LinkedList<>();
        Position pos;
        while (parentIter.hasNext()) {
            pos = (Position) parentIter.next();
            if (pos == null) {
                continue;
            }
            if (pos.getIndex() >= 0) {
                if (firstPos == null) {
                    firstPos = pos;
                }
                lastPos = pos;
            }
            if (pos instanceof NonLeafPosition) {
                // pos was created by a child of this ListBlockLM
                positionList.add(pos.getPosition());
                lastLM = pos.getPosition().getLM();
                if (firstLM == null) {
                    firstLM = lastLM;
                }
            } else if (pos instanceof SpaceHandlingBreakPosition) {
                positionList.add(pos);
            } else {
                // pos was created by this ListBlockLM, so it must be ignored
            }
        }

        addMarkersToPage(true, isFirst(firstPos), isLast(lastPos));

        final StackingIter childPosIter = new StackingIter(
                positionList.listIterator());
        while ((childLM = childPosIter.getNextChildLM()) != null) {
            // Add the block areas to Area
            lc.setFlags(LayoutContext.FIRST_AREA, childLM == firstLM);
            lc.setFlags(LayoutContext.LAST_AREA, childLM == lastLM);
            // set the space adjustment ratio
            lc.setSpaceAdjust(layoutContext.getSpaceAdjust());
            lc.setStackLimitBP(layoutContext.getStackLimitBP());
            childLM.addAreas(childPosIter, lc);
        }

        addMarkersToPage(false, isFirst(firstPos), isLast(lastPos));

        flush();

        this.curBlockArea = null;

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
     *            the child area to get the parent for
     * @return the parent area
     */
    @Override
    public Area getParentArea(final Area childArea) {
        if (this.curBlockArea == null) {
            this.curBlockArea = new Block();
            this.curBlockArea.setPositioning(Block.ABSOLUTE);
            // set position
            this.curBlockArea.setXOffset(this.xoffset);
            this.curBlockArea.setIPD(this.itemIPD);
            // curBlockArea.setHeight();

            TraitSetter.setProducerID(this.curBlockArea, getPartFO().getId());

            // Set up dimensions
            final Area parentArea = this.parentLayoutManager
                    .getParentArea(this.curBlockArea);
            final int referenceIPD = parentArea.getIPD();
            this.curBlockArea.setIPD(referenceIPD);
            // Get reference IPD from parentArea
            setCurrentArea(this.curBlockArea); // ??? for generic operations
        }
        return this.curBlockArea;
    }

    /**
     * Add the child to the list item area.
     *
     * @param childArea
     *            the child to add to the cell
     */
    @Override
    public void addChildArea(final Area childArea) {
        if (this.curBlockArea != null) {
            this.curBlockArea.addBlock((Block) childArea);
        }
    }

    /** {@inheritDoc} */
    @Override
    public KeepProperty getKeepTogetherProperty() {
        return getPartFO().getKeepTogether();
    }

    /** {@inheritDoc} */
    @Override
    public Keep getKeepWithNext() {
        return Keep.KEEP_AUTO;
    }

    /** {@inheritDoc} */
    @Override
    public Keep getKeepWithPrevious() {
        return Keep.KEEP_AUTO;
    }

}
