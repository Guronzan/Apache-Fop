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

/* $Id: ListProperty.java 679326 2008-07-24 09:35:34Z vhennebert $ */

package org.apache.fop.fo.properties;

import java.util.ArrayList;
import java.util.List;

import org.apache.fop.fo.FObj;
import org.apache.fop.fo.PropertyList;

/**
 * Superclass for properties that are lists of other properties
 */
public class ListProperty extends Property {

    /**
     * Inner class for creating instances of ListProperty
     */
    public static class Maker extends PropertyMaker {

        /**
         * @param propId
         *            ID of the property for which Maker should be created
         */
        public Maker(final int propId) {
            super(propId);
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public Property convertProperty(final Property p,
                final PropertyList propertyList, final FObj fo) {
            if (p instanceof ListProperty) {
                return p;
            } else {
                return new ListProperty(p);
            }
        }

    }

    /** List containing the list of sub-properties */
    protected List<Property> list = new ArrayList<>();

    /**
     * Simple constructor used by subclasses to do some special processing.
     */
    protected ListProperty() {
        // nop
    }

    /**
     * @param prop
     *            the first Property to be added to the list
     */
    public ListProperty(final Property prop) {
        this();
        addProperty(prop);
    }

    /**
     * Add a new property to the list
     *
     * @param prop
     *            Property to be added to the list
     */
    public void addProperty(final Property prop) {
        this.list.add(prop);
    }

    /**
     * @return this.list
     */
    @Override
    public List<Property> getList() {
        return this.list;
    }

    /**
     * @return this.list cast as an Object
     */
    @Override
    public Object getObject() {
        return this.list;
    }

}
