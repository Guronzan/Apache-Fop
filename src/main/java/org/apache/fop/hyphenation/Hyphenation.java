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

/* $Id: Hyphenation.java 679326 2008-07-24 09:35:34Z vhennebert $ */

package org.apache.fop.hyphenation;

/**
 * This class represents a hyphenated word.
 *
 * @author Carlos Villegas <cav@uniscope.co.jp>
 */
public class Hyphenation {

    private final int[] hyphenPoints;
    private final String word;

    /**
     * number of hyphenation points in word
     */
    private final int len;

    /**
     * rawWord as made of alternating strings and {@link Hyphen Hyphen}
     * instances
     */
    Hyphenation(final String word, final int[] points) {
        this.word = word;
        this.hyphenPoints = points;
        this.len = points.length;
    }

    /**
     * @return the number of hyphenation points in the word
     */
    public int length() {
        return this.len;
    }

    /**
     * @return the pre-break text, not including the hyphen character
     */
    public String getPreHyphenText(final int index) {
        return this.word.substring(0, this.hyphenPoints[index]);
    }

    /**
     * @return the post-break text
     */
    public String getPostHyphenText(final int index) {
        return this.word.substring(this.hyphenPoints[index]);
    }

    /**
     * @return the hyphenation points
     */
    public int[] getHyphenationPoints() {
        return this.hyphenPoints;
    }

    @Override
    public String toString() {
        final StringBuilder str = new StringBuilder();
        int start = 0;
        for (int i = 0; i < this.len; i++) {
            str.append(this.word.substring(start, this.hyphenPoints[i]) + "-");
            start = this.hyphenPoints[i];
        }
        str.append(this.word.substring(start));
        return str.toString();
    }

}
