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

/* $Id: CommonAbsolutePosition.java 679326 2008-07-24 09:35:34Z vhennebert $ */

package org.apache.fop.fo.properties;

import org.apache.fop.datatypes.Length;
import org.apache.fop.fo.Constants;
import org.apache.fop.fo.PropertyList;
import org.apache.fop.fo.expr.PropertyException;

/**
 * Store all common absolute position properties. See Sec. 7.5 of the XSL-FO
 * Standard. Public "structure" allows direct member access.
 */
public class CommonAbsolutePosition {
    /**
     * The "absolute-position" property.
     */
    public int absolutePosition;

    /**
     * The "top" property.
     */
    public Length top;

    /**
     * The "right" property.
     */
    public Length right;

    /**
     * The "bottom" property.
     */
    public Length bottom;

    /**
     * The "left" property.
     */
    public Length left;

    /**
     * Create a CommonAbsolutePosition object.
     * 
     * @param pList
     *            The PropertyList with propery values.
     */
    public CommonAbsolutePosition(final PropertyList pList)
            throws PropertyException {
        this.absolutePosition = pList.get(Constants.PR_ABSOLUTE_POSITION)
                .getEnum();
        this.top = pList.get(Constants.PR_TOP).getLength();
        this.bottom = pList.get(Constants.PR_BOTTOM).getLength();
        this.left = pList.get(Constants.PR_LEFT).getLength();
        this.right = pList.get(Constants.PR_RIGHT).getLength();
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder("CommonAbsolutePosition{");
        sb.append(" absPos=");
        sb.append(this.absolutePosition);
        sb.append(" top=");
        sb.append(this.top);
        sb.append(" bottom=");
        sb.append(this.bottom);
        sb.append(" left=");
        sb.append(this.left);
        sb.append(" right=");
        sb.append(this.right);
        sb.append("}");
        return sb.toString();
    }
}
