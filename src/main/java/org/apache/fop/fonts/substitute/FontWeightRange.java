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

/* $Id: FontWeightRange.java 1036179 2010-11-17 19:45:27Z spepping $ */

package org.apache.fop.fonts.substitute;

import java.util.StringTokenizer;

import lombok.extern.slf4j.Slf4j;

/**
 * Encapsulates a range of font weight values
 */
@Slf4j
 public class FontWeightRange {

    /**
      * Returns an <code>FontWeightRange</code> object holding the range values
     * of the specified <code>String</code>.
      *
      * @param weightRangeString
     *            the value range string
     * @return an <code>FontWeightRange</code> object holding the value ranges
      */
     public static FontWeightRange valueOf(final String weightRangeString) {
         final StringTokenizer rangeToken = new StringTokenizer(
                weightRangeString, "..");
         FontWeightRange weightRange = null;
         if (rangeToken.countTokens() == 2) {
             final String weightString = rangeToken.nextToken().trim();
             try {
                 final int start = Integer.parseInt(weightString);
                 if (start % 100 != 0) {
                     log.error("font-weight start range is not a multiple of 100");
                 }
                 final int end = Integer.parseInt(rangeToken.nextToken());
                 if (end % 100 != 0) {
                     log.error("font-weight end range is not a multiple of 100");
                 }
                 if (start <= end) {
                     weightRange = new FontWeightRange(start, end);
                 } else {
                     log.error("font-weight start range is greater than end range");
                 }
             } catch (final NumberFormatException e) {
                 log.error("invalid font-weight value " + weightString);
             }
         }
         return weightRange;
     }

     /** the start range */
     private final int start;

     /** the end range */
     private final int end;

     /**
      * Main constructor
      * 
     * @param start
     *            the start value range
     * @param end
     *            the end value range
      */
     public FontWeightRange(final int start, final int end) {
         this.start = start;
         this.end = end;
     }

     /**
      * Returns true if the given integer value is within this integer range
      * 
     * @param value
     *            the integer value
     * @return true if the given integer value is within this integer range
      */
     public boolean isWithinRange(final int value) {
         return value >= this.start && value <= this.end;
     }

     /**
      * {@inheritDoc}
      */
     @Override
     public String toString() {
         return this.start + ".." + this.end;
     }

     /**
      * @return an integer array containing the weight ranges
      */
     public int[] toArray() {
         int cnt = 0;
         for (int i = this.start; i <= this.end; i += 100) {
             cnt++;
         }
         final int[] range = new int[cnt];
         for (int i = 0; i < cnt; ++i) {
             range[i] = this.start + i * 100;
         }
         return range;
     }
 }
