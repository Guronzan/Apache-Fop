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

/* $Id: KnuthSequence.java 825646 2009-10-15 20:43:13Z acumiskey $ */

package org.apache.fop.layoutmgr;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;

import org.apache.fop.util.ListUtil;

/**
 * Represents a list of {@link KnuthElement Knuth elements}.
 */
public abstract class KnuthSequence implements List<ListElement> {

    /**
     * Creates a new and empty list.
     */
    public KnuthSequence() {
        super();
    }

    /**
     * Creates a new list from an existing list.
     *
     * @param list
     *            The list from which to create the new list.
     */
    public KnuthSequence(final List<ListElement> list) {
        super();
    }

    /**
     * Marks the start of the sequence.
     */
    public void startSequence() {
    }

    /**
     * Finalizes a Knuth sequence.
     *
     * @return a finalized sequence.
     */
    public abstract KnuthSequence endSequence();

    /**
     * Can sequence be appended to this sequence?
     *
     * @param sequence
     *            The sequence that may be appended.
     * @return whether the sequence can be appended to this sequence.
     */
    public abstract boolean canAppendSequence(final KnuthSequence sequence);

    /**
     * Append sequence to this sequence if it can be appended.
     *
     * @param sequence
     *            The sequence that is to be appended.
     * @param keepTogether
     *            Whether the two sequences must be kept together.
     * @param breakElement
     *            The BreakElement that may be inserted between the two
     *            sequences.
     * @return whether the sequence was succesfully appended to this sequence.
     */
    public abstract boolean appendSequence(final KnuthSequence sequence,
            final boolean keepTogether, final BreakElement breakElement);

    /**
     * Append sequence to this sequence if it can be appended.
     *
     * @param sequence
     *            The sequence that is to be appended.
     * @return whether the sequence was succesfully appended to this sequence.
     */
    public abstract boolean appendSequence(final KnuthSequence sequence);

    /**
     * Append sequence to this sequence if it can be appended. If that is not
     * possible, close this sequence.
     *
     * @param sequence
     *            The sequence that is to be appended.
     * @return whether the sequence was succesfully appended to this sequence.
     */
    public boolean appendSequenceOrClose(final KnuthSequence sequence) {
        if (!appendSequence(sequence)) {
            endSequence();
            return false;
        } else {
            return true;
        }
    }

    /**
     * Append sequence to this sequence if it can be appended. If that is not
     * possible, close this sequence.
     *
     * @param sequence
     *            The sequence that is to be appended.
     * @param keepTogether
     *            Whether the two sequences must be kept together.
     * @param breakElement
     *            The BreakElement that may be inserted between the two
     *            sequences.
     * @return whether the sequence was succesfully appended to this sequence.
     */
    public boolean appendSequenceOrClose(final KnuthSequence sequence,
            final boolean keepTogether, final BreakElement breakElement) {
        if (!appendSequence(sequence, keepTogether, breakElement)) {
            endSequence();
            return false;
        } else {
            return true;
        }
    }

    /**
     * Wrap the Positions of the elements of this sequence in a Position for
     * LayoutManager lm.
     *
     * @param lm
     *            The LayoutManager for the Positions that will be created.
     */
    public void wrapPositions(final LayoutManager lm) {
        final ListIterator<ListElement> listIter = listIterator();
        ListElement element;
        while (listIter.hasNext()) {
            element = listIter.next();
            element.setPosition(lm.notifyPos(new NonLeafPosition(lm, element
                    .getPosition())));
        }
    }

    /**
     * @return the last element of this sequence.
     */
    public ListElement getLast() {
        return isEmpty() ? null : (ListElement) ListUtil.getLast(this);
    }

    /**
     * Remove the last element of this sequence.
     *
     * @return the removed element.
     */
    public ListElement removeLast() {
        return isEmpty() ? null : (ListElement) ListUtil.removeLast(this);
    }

    /**
     * @param index
     *            The index of the element to be returned
     * @return the element at index index.
     */
    public ListElement getElement(final int index) {
        return index >= size() || index < 0 ? null : (ListElement) get(index);
    }

    /** @return the position index of the first box in this sequence */
    protected int getFirstBoxIndex() {
        if (isEmpty()) {
            return -1;
        } else {
            return getFirstBoxIndex(0);
        }
    }

    /**
     * Get the position index of the first box in this sequence, starting at the
     * given index. If there is no box after the passed {@code startIndex}, the
     * starting index itself is returned.
     *
     * @param startIndex
     *            the starting index for the lookup
     * @return the absolute position index of the next box element
     */
    protected int getFirstBoxIndex(final int startIndex) {
        if (isEmpty() || startIndex < 0 || startIndex >= size()) {
            return -1;
        } else {
            ListElement element = null;
            int posIndex = startIndex;
            final int lastIndex = size();
            while (posIndex < lastIndex
                    && !(element = getElement(posIndex)).isBox()) {
                posIndex++;
            }
            if (posIndex != startIndex && element.isBox()) {
                return posIndex - 1;
            } else {
                return startIndex;
            }
        }
    }

    /**
     * Is this an inline or a block sequence?
     *
     * @return true if this is an inline sequence
     */
    public abstract boolean isInlineSequence();

    /** {@inheritDoc} */
    @Override
    public String toString() {
        return "<KnuthSequence " + super.toString() + ">";
    }

    private final List<ListElement> elements = new ArrayList<>();

    /*
     * (non-Javadoc)
     *
     * @see java.util.List#size()
     */
    @Override
    public int size() {
        return this.elements.size();
    }

    /*
     * (non-Javadoc)
     *
     * @see java.util.List#isEmpty()
     */
    @Override
    public boolean isEmpty() {
        return this.elements.isEmpty();
    }

    /*
     * (non-Javadoc)
     *
     * @see java.util.List#contains(java.lang.Object)
     */
    @Override
    public boolean contains(final Object o) {
        return this.elements.contains(o);
    }

    /*
     * (non-Javadoc)
     *
     * @see java.util.List#iterator()
     */
    @Override
    public Iterator<ListElement> iterator() {
        return this.elements.iterator();
    }

    /*
     * (non-Javadoc)
     *
     * @see java.util.List#toArray()
     */
    @Override
    public Object[] toArray() {
        return this.elements.toArray();
    }

    /*
     * (non-Javadoc)
     *
     * @see java.util.List#toArray(java.lang.Object[])
     */
    @SuppressWarnings("unchecked")
    @Override
    public <T> T[] toArray(final T[] a) {
        return (T[]) this.elements.toArray();
    }

    /*
     * (non-Javadoc)
     *
     * @see java.util.List#add(java.lang.Object)
     */
    @Override
    public boolean add(final ListElement e) {
        return this.elements.add(e);
    }

    /*
     * (non-Javadoc)
     *
     * @see java.util.List#remove(java.lang.Object)
     */
    @Override
    public boolean remove(final Object o) {
        return this.elements.remove(o);
    }

    /*
     * (non-Javadoc)
     *
     * @see java.util.List#containsAll(java.util.Collection)
     */
    @Override
    public boolean containsAll(final Collection<?> c) {
        return this.elements.containsAll(c);
    }

    /*
     * (non-Javadoc)
     *
     * @see java.util.List#addAll(java.util.Collection)
     */
    @Override
    public boolean addAll(final Collection<? extends ListElement> c) {
        return this.elements.addAll(c);
    }

    /*
     * (non-Javadoc)
     *
     * @see java.util.List#addAll(int, java.util.Collection)
     */
    @Override
    public boolean addAll(final int index,
            final Collection<? extends ListElement> c) {
        return this.elements.addAll(index, c);
    }

    /*
     * (non-Javadoc)
     *
     * @see java.util.List#removeAll(java.util.Collection)
     */
    @Override
    public boolean removeAll(final Collection<?> c) {
        return this.elements.removeAll(c);
    }

    /*
     * (non-Javadoc)
     *
     * @see java.util.List#retainAll(java.util.Collection)
     */
    @Override
    public boolean retainAll(final Collection<?> c) {
        return this.elements.retainAll(c);
    }

    /*
     * (non-Javadoc)
     *
     * @see java.util.List#clear()
     */
    @Override
    public void clear() {
        this.elements.clear();
    }

    /*
     * (non-Javadoc)
     *
     * @see java.util.List#get(int)
     */
    @Override
    public ListElement get(final int index) {
        return this.elements.get(index);
    }

    /*
     * (non-Javadoc)
     *
     * @see java.util.List#set(int, java.lang.Object)
     */
    @Override
    public ListElement set(final int index, final ListElement element) {
        return this.elements.set(index, element);
    }

    /*
     * (non-Javadoc)
     *
     * @see java.util.List#add(int, java.lang.Object)
     */
    @Override
    public void add(final int index, final ListElement element) {
        this.elements.add(index, element);
    }

    /*
     * (non-Javadoc)
     *
     * @see java.util.List#remove(int)
     */
    @Override
    public ListElement remove(final int index) {
        return this.elements.remove(index);
    }

    /*
     * (non-Javadoc)
     *
     * @see java.util.List#indexOf(java.lang.Object)
     */
    @Override
    public int indexOf(final Object o) {
        return this.elements.indexOf(o);
    }

    /*
     * (non-Javadoc)
     *
     * @see java.util.List#lastIndexOf(java.lang.Object)
     */
    @Override
    public int lastIndexOf(final Object o) {
        return this.elements.lastIndexOf(o);
    }

    /*
     * (non-Javadoc)
     *
     * @see java.util.List#listIterator()
     */
    @Override
    public ListIterator<ListElement> listIterator() {
        return this.elements.listIterator();
    }

    /*
     * (non-Javadoc)
     *
     * @see java.util.List#listIterator(int)
     */
    @Override
    public ListIterator<ListElement> listIterator(final int index) {
        return this.elements.listIterator(index);
    }

    /*
     * (non-Javadoc)
     *
     * @see java.util.List#subList(int, int)
     */
    @Override
    public List<ListElement> subList(final int fromIndex, final int toIndex) {
        return this.elements.subList(fromIndex, toIndex);
    }

}
