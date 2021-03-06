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

/* $Id: LeafPosition.java 893238 2009-12-22 17:20:51Z vhennebert $ */

package org.apache.fop.layoutmgr;

public class LeafPosition extends Position {

    private final int leafPos;

    public LeafPosition(final LayoutManager layoutManager, final int pos) {
        super(layoutManager);
        this.leafPos = pos;
    }

    public LeafPosition(final LayoutManager layoutManager, final int pos,
            final int index) {
        super(layoutManager, index);
        this.leafPos = pos;
    }

    public int getLeafPos() {
        return this.leafPos;
    }

    @Override
    public boolean generatesAreas() {
        return getLM() != null;
    }

    /** {@inheritDoc} */
    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder();
        sb.append("LeafPos:").append(getIndex()).append("(");
        sb.append("pos=").append(getLeafPos());
        sb.append(", lm=").append(getShortLMName()).append(")");
        return sb.toString();
    }
}
