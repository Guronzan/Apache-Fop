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

/* $Id: Position.java 808157 2009-08-26 18:50:10Z vhennebert $ */

package org.apache.fop.layoutmgr;

public class Position {

    private final LayoutManager layoutManager;
    private int index = -1;

    public Position(final LayoutManager lm) {
        this.layoutManager = lm;
    }

    public Position(final LayoutManager lm, final int index) {
        this(lm);
        setIndex(index);
    }

    public LayoutManager getLM() {
        return this.layoutManager;
    }

    /**
     * Overridden by NonLeafPosition to return the Position of its child LM.
     */
    public Position getPosition() {
        return null;
    }

    public boolean generatesAreas() {
        return false;
    }

    /**
     * Sets the index of this position in the sequence of Position elements.
     *
     * @param value
     *            this position's index
     */
    public void setIndex(final int value) {
        this.index = value;
    }

    /**
     * Returns the index of this position in the sequence of Position elements.
     *
     * @return the index of this position in the sequence of Position elements
     */
    public int getIndex() {
        return this.index;
    }

    public String getShortLMName() {
        if (getLM() != null) {
            final String lm = getLM().toString();
            final int idx = lm.lastIndexOf('.');
            if (idx >= 0 && lm.indexOf('@') > 0) {
                return lm.substring(idx + 1);
            } else {
                return lm;
            }
        } else {
            return "null";
        }
    }

    /** {@inheritDoc} */
    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder();
        sb.append("Position:").append(getIndex()).append("(");
        sb.append(getShortLMName());
        sb.append(")");
        return sb.toString();
    }
}
