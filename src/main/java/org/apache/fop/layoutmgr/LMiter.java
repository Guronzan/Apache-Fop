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

/* $Id: LMiter.java 679326 2008-07-24 09:35:34Z vhennebert $ */

package org.apache.fop.layoutmgr;

import java.util.List;
import java.util.ListIterator;
import java.util.NoSuchElementException;

public class LMiter implements ListIterator<LayoutManager> {

    protected List<LayoutManager> listLMs;
    protected int curPos = 0;
    /** The LayoutManager to which this LMiter is attached **/
    private final LayoutManager lp;

    public LMiter(final LayoutManager lp) {
        this.lp = lp;
        this.listLMs = lp.getChildLMs();
    }

    @Override
    public boolean hasNext() {
        return this.curPos < this.listLMs.size() ? true : this.lp
                .createNextChildLMs(this.curPos);
    }

    @Override
    public boolean hasPrevious() {
        return this.curPos > 0;
    }

    @Override
    public LayoutManager previous() throws NoSuchElementException {
        if (this.curPos > 0) {
            return this.listLMs.get(--this.curPos);
        } else {
            throw new NoSuchElementException();
        }
    }

    @Override
    public LayoutManager next() throws NoSuchElementException {
        if (this.curPos < this.listLMs.size()) {
            return this.listLMs.get(this.curPos++);
        } else {
            throw new NoSuchElementException();
        }
    }

    @Override
    public void remove() throws NoSuchElementException {
        if (this.curPos > 0) {
            this.listLMs.remove(--this.curPos);
            // Note: doesn't actually remove it from the base!
        } else {
            throw new NoSuchElementException();
        }
    }

    @Override
    public void add(final LayoutManager o) throws UnsupportedOperationException {
        throw new UnsupportedOperationException("LMiter doesn't support add");
    }

    @Override
    public void set(final LayoutManager o) throws UnsupportedOperationException {
        throw new UnsupportedOperationException("LMiter doesn't support set");
    }

    @Override
    public int nextIndex() {
        return this.curPos;
    }

    @Override
    public int previousIndex() {
        return this.curPos - 1;
    }

}
