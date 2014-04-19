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

/* $Id: NonLeafPosition.java 679326 2008-07-24 09:35:34Z vhennebert $ */

package org.apache.fop.layoutmgr;

public class NonLeafPosition extends Position {

    private final Position subPos;

    public NonLeafPosition(final LayoutManager lm, final Position sub) {
        super(lm);
        this.subPos = sub;
    }

    @Override
    public Position getPosition() {
        return this.subPos;
    }

    @Override
    public boolean generatesAreas() {
        return this.subPos != null ? this.subPos.generatesAreas() : false;
    }

    /** {@inheritDoc} */
    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder();
        sb.append("NonLeafPos:").append(getIndex()).append("(");
        sb.append(getShortLMName());
        sb.append(", ");
        if (getPosition() != null) {
            sb.append(getPosition().toString());
        } else {
            sb.append("null");
        }
        sb.append(")");
        return sb.toString();
    }
}
