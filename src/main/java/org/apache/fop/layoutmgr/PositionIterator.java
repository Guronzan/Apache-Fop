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

/* $Id: PositionIterator.java 679326 2008-07-24 09:35:34Z vhennebert $ */

package org.apache.fop.layoutmgr;

import java.util.Iterator;
import java.util.NoSuchElementException;

public abstract class PositionIterator<T> implements Iterator<Position> {

    private final Iterator<T> parentIter;
    private T nextObj;
    private LayoutManager childLM;
    private boolean bHasNext;

    protected PositionIterator(final Iterator<T> pIter) {
        this.parentIter = pIter;
        lookAhead();
        // checkNext();
    }

    public LayoutManager getNextChildLM() {
        // Move to next "segment" of iterator, ie: new childLM
        if (this.childLM == null && this.nextObj != null) {
            this.childLM = getLM(this.nextObj);
            this.bHasNext = true;
        }
        return this.childLM;
    }

    protected abstract LayoutManager getLM(final T nextObj);

    protected abstract Position getPos(final T nextObj);

    private void lookAhead() {
        if (this.parentIter.hasNext()) {
            this.bHasNext = true;
            this.nextObj = this.parentIter.next();
        } else {
            endIter();
        }
    }

    protected boolean checkNext() {
        final LayoutManager lm = getLM(this.nextObj);
        if (this.childLM == null) {
            this.childLM = lm;
        } else if (this.childLM != lm && lm != null) {
            // End of this sub-sequence with same child LM
            this.bHasNext = false;
            this.childLM = null;
            return false;
        }
        return true;
    }

    protected void endIter() {
        this.bHasNext = false;
        this.nextObj = null;
        this.childLM = null;
    }

    @Override
    public boolean hasNext() {
        return this.bHasNext && checkNext();
    }

    @Override
    public Position next() throws NoSuchElementException {
        if (this.bHasNext) {
            final Position retObj = getPos(this.nextObj);
            lookAhead();
            return retObj;
        } else {
            throw new NoSuchElementException("PosIter");
        }
    }

    public T peekNext() {
        return this.nextObj;
    }

    @Override
    public void remove() throws UnsupportedOperationException {
        throw new UnsupportedOperationException(
                "PositionIterator doesn't support remove");
    }
}
