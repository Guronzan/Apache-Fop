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

/* $Id: FootnoteBodyLayoutManager.java 893238 2009-12-22 17:20:51Z vhennebert $ */

package org.apache.fop.layoutmgr;

import java.util.LinkedList;

import org.apache.fop.area.Area;
import org.apache.fop.fo.flow.FootnoteBody;

/**
 * Layout manager for footnote bodies.
 */
public class FootnoteBodyLayoutManager extends BlockStackingLayoutManager {

    /**
     * Creates a new FootnoteBodyLayoutManager.
     *
     * @param body
     *            the footnote-body element
     */
    public FootnoteBodyLayoutManager(final FootnoteBody body) {
        super(body);
    }

    /** {@inheritDoc} */
    @Override
    public void addAreas(final PositionIterator parentIter,
            final LayoutContext layoutContext) {
        LayoutManager childLM = null;
        LayoutManager lastLM = null;
        final LayoutContext lc = new LayoutContext(0);

        // "unwrap" the NonLeafPositions stored in parentIter
        // and put them in a new list;
        final LinkedList positionList = new LinkedList();
        Position pos;
        while (parentIter.hasNext()) {
            pos = (Position) parentIter.next();
            // log.trace("pos = " + pos.getClass().getName() + "; " + pos);
            Position innerPosition = pos;
            if (pos instanceof NonLeafPosition) {
                innerPosition = ((NonLeafPosition) pos).getPosition();
                if (innerPosition.getLM() == this) {
                    // pos was created by this LM and was inside a penalty
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
        }

        // the Positions in positionList were inside the elements
        // created by the LineLM
        final StackingIter childPosIter = new StackingIter(
                positionList.listIterator());

        while ((childLM = childPosIter.getNextChildLM()) != null) {
            // set last area flag
            lc.setFlags(LayoutContext.LAST_AREA, layoutContext.isLastArea()
                    && childLM == lastLM);
            // Add the line areas to Area
            childLM.addAreas(childPosIter, lc);
        }
    }

    /** {@inheritDoc} */
    @Override
    public void addChildArea(final Area childArea) {
        childArea.setAreaClass(Area.CLASS_FOOTNOTE);
        this.parentLayoutManager.addChildArea(childArea);
    }

    /** @return the FootnoteBody node */
    protected FootnoteBody getFootnodeBodyFO() {
        return (FootnoteBody) this.fobj;
    }

    /** {@inheritDoc} */
    @Override
    public Keep getKeepTogether() {
        return getParentKeepTogether();
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
