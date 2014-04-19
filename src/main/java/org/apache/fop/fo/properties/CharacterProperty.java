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

/* $Id: CharacterProperty.java 679326 2008-07-24 09:35:34Z vhennebert $ */

package org.apache.fop.fo.properties;

import org.apache.fop.fo.FObj;
import org.apache.fop.fo.PropertyList;

/**
 * Superclass for properties that wrap a character value
 */
public final class CharacterProperty extends Property {

    /**
     * Inner class for creating instances of CharacterProperty
     */
    public static class Maker extends PropertyMaker {

        /**
         * @param propId
         *            the id of the property for which a Maker should be created
         */
        public Maker(final int propId) {
            super(propId);
        }

        @Override
        public Property make(final PropertyList propertyList,
                final String value, final FObj fo) {
            final char c = value.charAt(0);
            return CharacterProperty.getInstance(c);
        }

    }

    /** cache containing all canonical CharacterProperty instances */
    private static final PropertyCache cache = new PropertyCache(
            CharacterProperty.class);

    private final char character;

    /**
     * @param character
     *            character value to be wrapped in this property
     */
    private CharacterProperty(final char character) {
        this.character = character;
    }

    public static CharacterProperty getInstance(final char character) {
        return (CharacterProperty) cache
                .fetch(new CharacterProperty(character));
    }

    /**
     * @return this.character cast as an Object
     */
    @Override
    public Object getObject() {
        return new Character(this.character);
    }

    /**
     * @return this.character
     */
    @Override
    public char getCharacter() {
        return this.character;
    }

    /**
     * @return this.character cast as a String
     */
    @Override
    public String getString() {
        return new Character(this.character).toString();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean equals(final Object obj) {
        if (obj instanceof CharacterProperty) {
            return ((CharacterProperty) obj).character == this.character;
        } else {
            return false;
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int hashCode() {
        return this.character;
    }

}
