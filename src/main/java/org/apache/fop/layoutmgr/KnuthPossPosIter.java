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

/* $Id: KnuthPossPosIter.java 679326 2008-07-24 09:35:34Z vhennebert $ */

package org.apache.fop.layoutmgr;

import java.util.List;

public class KnuthPossPosIter extends PositionIterator<ListElement> {

    private int iterCount;

    /**
     * Main constructor
     *
     * @param elementList
     *            List of Knuth elements
     * @param startPos
     *            starting position, inclusive
     * @param endPos
     *            ending position, exclusive
     */
    public KnuthPossPosIter(final List<ListElement> elementList,
            final int startPos, final int endPos) {
        super(elementList.listIterator(startPos));
        this.iterCount = endPos - startPos;
    }

    /**
     * Auxiliary constructor
     *
     * @param elementList
     *            List of Knuth elements
     */
    public KnuthPossPosIter(final List<ListElement> elementList) {
        this(elementList, 0, elementList.size());
    }

    // Check position < endPos

    /**
     * {@inheritDoc}
     */
    @Override
    protected boolean checkNext() {
        if (this.iterCount > 0) {
            return super.checkNext();
        } else {
            endIter();
            return false;
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Position next() {
        --this.iterCount;
        return super.next();
    }

    public ListElement getKE() {
        return peekNext();
    }

    @Override
    protected LayoutManager getLM(final ListElement nextObj) {
        return nextObj.getLayoutManager();
    }

    @Override
    protected Position getPos(final ListElement nextObj) {
        return nextObj.getPosition();
    }
}
