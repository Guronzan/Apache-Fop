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

/* $Id: FODimension.java 679326 2008-07-24 09:35:34Z vhennebert $ */

package org.apache.fop.datatypes;

/**
 * This datatype hold a pair of resolved lengths, specifiying the dimensions in
 * both inline and block-progression-directions.
 */
public class FODimension {

    /** distance (in millipoints) on the inline-progression-direction */
    public int ipd;
    /** distance (in millipoints) on the block-progression-direction */
    public int bpd;

    /**
     * Constructor
     * 
     * @param ipd
     *            length (in millipoints) of the inline-progression-direction
     * @param bpd
     *            length (in millipoints) of the block-progression-direction
     */
    public FODimension(final int ipd, final int bpd) {
        this.ipd = ipd;
        this.bpd = bpd;
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder(super.toString());
        sb.append(" {ipd=").append(Integer.toString(this.ipd));
        sb.append(", bpd=").append(Integer.toString(this.bpd));
        sb.append("}");
        return sb.toString();
    }
}
