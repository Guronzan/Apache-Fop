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

/* $Id: RelSide.java 679326 2008-07-24 09:35:34Z vhennebert $ */

package org.apache.fop.layoutmgr;

/** Enum class for relative sides. */
public final class RelSide {

    /** the before side */
    public static final RelSide BEFORE = new RelSide("before");
    /** the after side */
    public static final RelSide AFTER = new RelSide("after");
    /** the start side */
    public static final RelSide START = new RelSide("start");
    /** the end side */
    public static final RelSide END = new RelSide("end");

    private final String name;

    /**
     * Constructor to add a new named item.
     * 
     * @param name
     *            Name of the item.
     */
    private RelSide(final String name) {
        this.name = name;
    }

    /** @return the name of the enum */
    public String getName() {
        return this.name;
    }

    /** {@inheritDoc} */
    @Override
    public String toString() {
        return "RelSide:" + this.name;
    }

}
