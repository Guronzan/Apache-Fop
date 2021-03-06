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

/* $Id: RecursiveCharIterator.java 679326 2008-07-24 09:35:34Z vhennebert $ */

package org.apache.fop.fo;

import java.util.Iterator;
import java.util.NoSuchElementException;

/**
 * Kind of a super-iterator that iterates through child nodes of an FONode, in
 * turn managing character iterators for each of them. Caveat: Because this
 * class is itself a CharIterator, and manages a collection of CharIterators, it
 * is easy to get confused.
 */
public class RecursiveCharIterator extends CharIterator {
    /** parent node for whose child nodes this iterator iterates */
    private final FONode fobj;
    /** iterator for the child nodes */
    private Iterator childIter = null;

    /** current child object that is being managed by childIter */
    private FONode curChild;
    /** CharIterator for curChild's characters */
    private CharIterator curCharIter = null;

    /**
     * Constructor which creates an iterator for all child nodes
     *
     * @param fobj
     *            FONode for which an iterator should be created
     */
    public RecursiveCharIterator(final FObj fobj) {
        // Set up first child iterator
        this.fobj = fobj;
        this.childIter = fobj.getChildNodes();
        getNextCharIter();
    }

    /**
     * Constructor which creates an iterator for only some child nodes
     *
     * @param fobj
     *            FObj for which an iterator should be created
     * @param child
     *            FONode of the first child to include in iterator
     */
    public RecursiveCharIterator(final FObj fobj, final FONode child) {
        // Set up first child iterator
        this.fobj = fobj;
        this.childIter = fobj.getChildNodes(child);
        getNextCharIter();
    }

    /**
     * @return clone of this, cast as a CharIterator
     */
    public CharIterator mark() {
        return (CharIterator) clone();
    }

    /**
     * @return a clone of this
     */
    @Override
    public Object clone() {
        final RecursiveCharIterator ci = (RecursiveCharIterator) super.clone();
        ci.childIter = this.fobj.getChildNodes(ci.curChild);
        // Need to advance to the next child, else we get the same one!!!
        ci.childIter.next();
        ci.curCharIter = (CharIterator) this.curCharIter.clone();
        return ci;
    }

    /**
     * Replaces the current character in the CharIterator with a specified
     * character
     *
     * @param c
     *            the character which should be used to replace the current
     *            character
     */
    @Override
    public void replaceChar(final char c) {
        if (this.curCharIter != null) {
            this.curCharIter.replaceChar(c);
        }
    }

    /**
     * advances curChild to the next child in the collection, and curCharIter to
     * the CharIterator for that item, or sets them to null if the iterator has
     * no more items
     */
    private void getNextCharIter() {
        if (this.childIter != null && this.childIter.hasNext()) {
            this.curChild = (FONode) this.childIter.next();
            this.curCharIter = this.curChild.charIterator();
        } else {
            this.curChild = null;
            this.curCharIter = null;
        }
    }

    /**
     * @return true if there are more items in the CharIterator
     */
    @Override
    public boolean hasNext() {
        while (this.curCharIter != null) {
            if (!this.curCharIter.hasNext()) {
                getNextCharIter();
            } else {
                return true;
            }
        }
        return false;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public char nextChar() throws NoSuchElementException {
        if (this.curCharIter != null) {
            return this.curCharIter.nextChar();
        } else {
            throw new NoSuchElementException();
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void remove() {
        if (this.curCharIter != null) {
            this.curCharIter.remove();
        }
    }
}
