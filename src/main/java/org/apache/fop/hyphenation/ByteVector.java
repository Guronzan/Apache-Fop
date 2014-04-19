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

/* $Id: ByteVector.java 679326 2008-07-24 09:35:34Z vhennebert $ */

package org.apache.fop.hyphenation;

import java.io.Serializable;

/**
 * This class implements a simple byte vector with access to the underlying
 * array.
 *
 * @author Carlos Villegas <cav@uniscope.co.jp>
 */
public class ByteVector implements Serializable {

    /**
     *
     */
    private static final long serialVersionUID = 1554572867863466772L;
    /**
     * Capacity increment size
     */
    private static final int DEFAULT_BLOCK_SIZE = 2048;
    private int blockSize;

    /**
     * The encapsulated array
     */
    private byte[] array;

    /**
     * Points to next free item
     */
    private int n;

    public ByteVector() {
        this(DEFAULT_BLOCK_SIZE);
    }

    public ByteVector(final int capacity) {
        if (capacity > 0) {
            this.blockSize = capacity;
        } else {
            this.blockSize = DEFAULT_BLOCK_SIZE;
        }
        this.array = new byte[this.blockSize];
        this.n = 0;
    }

    public ByteVector(final byte[] a) {
        this.blockSize = DEFAULT_BLOCK_SIZE;
        this.array = a;
        this.n = 0;
    }

    public ByteVector(final byte[] a, final int capacity) {
        if (capacity > 0) {
            this.blockSize = capacity;
        } else {
            this.blockSize = DEFAULT_BLOCK_SIZE;
        }
        this.array = a;
        this.n = 0;
    }

    public byte[] getArray() {
        return this.array;
    }

    /**
     * return number of items in array
     */
    public int length() {
        return this.n;
    }

    /**
     * returns current capacity of array
     */
    public int capacity() {
        return this.array.length;
    }

    public void put(final int index, final byte val) {
        this.array[index] = val;
    }

    public byte get(final int index) {
        return this.array[index];
    }

    /**
     * This is to implement memory allocation in the array. Like malloc().
     */
    public int alloc(final int size) {
        final int index = this.n;
        final int len = this.array.length;
        if (this.n + size >= len) {
            final byte[] aux = new byte[len + this.blockSize];
            System.arraycopy(this.array, 0, aux, 0, len);
            this.array = aux;
        }
        this.n += size;
        return index;
    }

    public void trimToSize() {
        if (this.n < this.array.length) {
            final byte[] aux = new byte[this.n];
            System.arraycopy(this.array, 0, aux, 0, this.n);
            this.array = aux;
        }
    }

}
