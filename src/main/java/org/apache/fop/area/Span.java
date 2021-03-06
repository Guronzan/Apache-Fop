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

/* $Id: Span.java 679326 2008-07-24 09:35:34Z vhennebert $ */

package org.apache.fop.area;

import java.util.List;

/**
 * The span-reference-area. This is a block-area with 0 border and padding that
 * is stacked within the main-reference-area This object holds one or more
 * normal-flow-reference-area children based on the column-count trait in effect
 * for this span. See fo:region-body definition in the XSL Rec for more
 * information.
 */
public class Span extends Area {
    /**
     *
     */
    private static final long serialVersionUID = -5551430053660081549L;
    // the list of flow reference areas in this span area
    private List flowAreas;
    private final int colCount;
    private final int colGap;
    private int colWidth; // width for each normal flow, calculated value
    private int curFlowIdx; // n-f-r-a currently being processed, zero-based

    /**
     * Create a span area with the number of columns for this span area.
     *
     * @param colCount
     *            the number of columns in the span
     * @param colGap
     *            the column gap between each column
     * @param ipd
     *            the total ipd of the span
     */
    public Span(final int colCount, final int colGap, final int ipd) {
        addTrait(Trait.IS_REFERENCE_AREA, Boolean.TRUE);
        this.colCount = colCount;
        this.colGap = colGap;
        this.ipd = ipd;
        this.curFlowIdx = 0;
        createNormalFlows();
    }

    /**
     * Create the normal flows for this Span
     */
    private void createNormalFlows() {
        this.flowAreas = new java.util.ArrayList(this.colCount);
        this.colWidth = (this.ipd - (this.colCount - 1) * this.colGap)
                / this.colCount;

        for (int i = 0; i < this.colCount; ++i) {
            final NormalFlow newFlow = new NormalFlow(this.colWidth);
            this.flowAreas.add(newFlow);
        }
    }

    /**
     * Get the column count for this span area.
     *
     * @return the number of columns defined for this span area
     */
    public int getColumnCount() {
        return this.colCount;
    }

    /**
     * Get the width of a single column within this Span
     *
     * @return the width of a single column
     */
    public int getColumnWidth() {
        return this.colWidth;
    }

    /**
     * Get the height of this span area.
     *
     * @return the height of this span area
     */
    public int getHeight() {
        return getBPD();
    }

    /**
     * Get the normal flow area for a particular column.
     *
     * @param colRequested
     *            the zero-based column number of the flow
     * @return the flow area for the requested column
     */
    public NormalFlow getNormalFlow(final int colRequested) {
        if (colRequested >= 0 && colRequested < this.colCount) {
            return (NormalFlow) this.flowAreas.get(colRequested);
        } else { // internal error
            throw new IllegalArgumentException("Invalid column number "
                    + colRequested + " requested; only 0-"
                    + (this.colCount - 1) + " available.");
        }
    }

    /**
     * Get the NormalFlow area currently being processed
     *
     * @return the current NormalFlow
     */
    public NormalFlow getCurrentFlow() {
        return getNormalFlow(this.curFlowIdx);
    }

    /** @return the index of the current normal flow */
    public int getCurrentFlowIndex() {
        return this.curFlowIdx;
    }

    /**
     * Indicate to the Span that the next column is being processed.
     *
     * @return the new NormalFlow (in the next column)
     */
    public NormalFlow moveToNextFlow() {
        if (hasMoreFlows()) {
            this.curFlowIdx++;
            return getNormalFlow(this.curFlowIdx);
        } else {
            throw new IllegalStateException(
                    "(Internal error.) No more flows left in span.");
        }
    }

    /**
     * Indicates if the Span has unprocessed flows.
     *
     * @return true if Span can increment to the next flow, false otherwise.
     */
    public boolean hasMoreFlows() {
        return this.curFlowIdx < this.colCount - 1;
    }

    /**
     * Called to notify the span that all its flows have been fully generated so
     * it can update its own BPD extent.
     */
    public void notifyFlowsFinished() {
        int maxFlowBPD = Integer.MIN_VALUE;
        for (int i = 0; i < this.colCount; ++i) {
            maxFlowBPD = Math.max(maxFlowBPD, getNormalFlow(i).getAllocBPD());
        }
        this.bpd = maxFlowBPD;
    }

    /**
     * Indicates whether any child areas have been added to this span area.
     *
     * This is achieved by looping through each flow.
     * 
     * @return true if no child areas have been added yet.
     */
    public boolean isEmpty() {
        int areaCount = 0;
        for (int i = 0; i < getColumnCount(); ++i) {
            final NormalFlow flow = getNormalFlow(i);
            if (flow != null) {
                if (flow.getChildAreas() != null) {
                    areaCount += flow.getChildAreas().size();
                }
            }
        }
        return areaCount == 0;
    }

    /** {@inheritDoc} */
    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder(super.toString());
        if (this.colCount > 1) {
            sb.append(" {colCount=").append(this.colCount);
            sb.append(", colWidth=").append(this.colWidth);
            sb.append(", curFlowIdx=").append(this.curFlowIdx);
            sb.append("}");
        }
        return sb.toString();
    }

}
