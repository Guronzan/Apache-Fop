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

/* $Id: FormattingResults.java 679326 2008-07-24 09:35:34Z vhennebert $ */

package org.apache.fop.apps;

import java.util.ArrayList;
import java.util.List;

import org.apache.fop.fo.pagination.AbstractPageSequence;

/**
 * Class for reporting back formatting results to the calling application.
 */
public class FormattingResults {

    private int pageCount = 0;
    private List<PageSequenceResults> pageSequences = null;

    /**
     * Constructor for the FormattingResults object
     */
    public FormattingResults() {
    }

    /**
     * Gets the number of pages rendered
     *
     * @return The number of pages overall
     */
    public int getPageCount() {
        return this.pageCount;
    }

    /**
     * Gets the results for the individual page-sequences.
     *
     * @return A List with PageSequenceResults objects
     */
    public List<PageSequenceResults> getPageSequences() {
        return this.pageSequences;
    }

    /**
     * Resets this object
     */
    public void reset() {
        this.pageCount = 0;
        if (this.pageSequences != null) {
            this.pageSequences.clear();
        }
    }

    /**
     * Reports the result of one page sequence rendering back into this object.
     *
     * @param pageSequence
     *            the page sequence which just completed rendering
     * @param pageCount
     *            the number of pages rendered for that PageSequence
     */
    public void haveFormattedPageSequence(
            final AbstractPageSequence pageSequence, final int pageCount) {
        this.pageCount += pageCount;
        if (this.pageSequences == null) {
            this.pageSequences = new ArrayList<>();
        }
        this.pageSequences.add(new PageSequenceResults(pageSequence.getId(),
                pageCount));
    }
}
