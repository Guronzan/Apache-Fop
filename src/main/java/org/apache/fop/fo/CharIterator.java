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

/* $Id: CharIterator.java 679326 2008-07-24 09:35:34Z vhennebert $ */

package org.apache.fop.fo;

import java.util.Iterator;
import java.util.NoSuchElementException;

/**
 * Abstract base class for iterators that should iterate through a series of
 * characters. Extends the java.util.Iterator interface with some additional
 * functions useful for FOP's management of text.
 */
public abstract class CharIterator implements Iterator, Cloneable {

    /**
     * {@inheritDoc}
     */
    @Override
    public abstract boolean hasNext();

    /**
     * @return the character that is the next character in the collection
     * @throws NoSuchElementException
     *             if there are no more characters (test for this condition with
     *             java.util.Iterator.hasNext()).
     */
    public abstract char nextChar() throws NoSuchElementException;

    /**
     * {@inheritDoc}
     */
    @Override
    public Object next() throws NoSuchElementException {
        return new Character(nextChar());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void remove() {
        throw new UnsupportedOperationException();
    }

    /**
     * Replace the current character managed by the iterator with a specified
     * character?
     *
     * @param c
     *            character
     */
    public void replaceChar(final char c) {
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Object clone() {
        try {
            return super.clone();
        } catch (final CloneNotSupportedException ex) {
            return null;
        }
    }
}
