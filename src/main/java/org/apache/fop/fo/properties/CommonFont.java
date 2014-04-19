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

/* $Id: CommonFont.java 824686 2009-10-13 10:52:29Z vhennebert $ */

package org.apache.fop.fo.properties;

// FOP
import java.util.List;

import org.apache.fop.datatypes.Length;
import org.apache.fop.datatypes.Numeric;
import org.apache.fop.fo.Constants;
import org.apache.fop.fo.PropertyList;
import org.apache.fop.fo.expr.PropertyException;
import org.apache.fop.fonts.FontInfo;
import org.apache.fop.fonts.FontTriplet;

/**
 * Collection of CommonFont properties
 */
public final class CommonFont {

    /**
     * cache holding canonical CommonFont instances (only those with absolute
     * font-size and font-size-adjust)
     */
    private static final PropertyCache cache = new PropertyCache(
            CommonFont.class);

    /** hashcode of this instance */
    private int hash = 0;

    /** The "font-family" property. */
    private final FontFamilyProperty fontFamily;

    /** The "font-selection-strategy" property. */
    private final EnumProperty fontSelectionStrategy;

    /** The "font-stretch" property. */
    private final EnumProperty fontStretch;

    /** The "font-style" property. */
    private final EnumProperty fontStyle;

    /** The "font-variant" property. */
    private final EnumProperty fontVariant;

    /** The "font-weight" property. */
    private final EnumProperty fontWeight;

    /** The "font-size" property. */
    public final Length fontSize;

    /** The "font-size-adjust" property. */
    public final Numeric fontSizeAdjust;

    /**
     * Construct a CommonFont instance
     *
     * @param fontFamily
     *            the font-family property
     * @param fontSelectionStrategy
     *            the font-selection-strategy property
     * @param fontStretch
     *            the font-stretch property
     * @param fontStyle
     *            the font-style property
     * @param fontVariant
     *            the font-variant property
     * @param fontWeight
     *            the font-weight property
     * @param fontSize
     *            the font-size (possibly non-cached)
     * @param fontSizeAdjust
     *            the font-size-adjust (possibly non-cached)
     */
    private CommonFont(final FontFamilyProperty fontFamily,
            final EnumProperty fontSelectionStrategy,
            final EnumProperty fontStretch, final EnumProperty fontStyle,
            final EnumProperty fontVariant, final EnumProperty fontWeight,
            final Length fontSize, final Numeric fontSizeAdjust) {
        this.fontFamily = fontFamily;
        this.fontSelectionStrategy = fontSelectionStrategy;
        this.fontStretch = fontStretch;
        this.fontStyle = fontStyle;
        this.fontVariant = fontVariant;
        this.fontWeight = fontWeight;
        this.fontSize = fontSize;
        this.fontSizeAdjust = fontSizeAdjust;
    }

    /**
     * Returns a CommonFont instance for the given PropertyList If the font-size
     * and font-size-adjust properties are absolute the entire instance will be
     * cached. If not, then a distinct instance will be returned, with as much
     * cached information as possible.
     *
     * @param pList
     *            the PropertyList to get the properties from
     * @return a CommonFont instance corresponding to the properties
     * @throws PropertyException
     *             if there was a problem getting the properties
     */
    public static CommonFont getInstance(final PropertyList pList)
            throws PropertyException {
        final FontFamilyProperty fontFamily = (FontFamilyProperty) pList
                .get(Constants.PR_FONT_FAMILY);
        final EnumProperty fontSelectionStrategy = (EnumProperty) pList
                .get(Constants.PR_FONT_SELECTION_STRATEGY);
        final EnumProperty fontStretch = (EnumProperty) pList
                .get(Constants.PR_FONT_STRETCH);
        final EnumProperty fontStyle = (EnumProperty) pList
                .get(Constants.PR_FONT_STYLE);
        final EnumProperty fontVariant = (EnumProperty) pList
                .get(Constants.PR_FONT_VARIANT);
        final EnumProperty fontWeight = (EnumProperty) pList
                .get(Constants.PR_FONT_WEIGHT);
        final Numeric fontSizeAdjust = pList.get(Constants.PR_FONT_SIZE_ADJUST)
                .getNumeric();
        final Length fontSize = pList.get(Constants.PR_FONT_SIZE).getLength();

        final CommonFont commonFont = new CommonFont(fontFamily,
                fontSelectionStrategy, fontStretch, fontStyle, fontVariant,
                fontWeight, fontSize, fontSizeAdjust);

        return cache.fetch(commonFont);
    }

    /** @return an array with the font-family names */
    private String[] getFontFamily() {
        final List lst = this.fontFamily.getList();
        final String[] fontFamily = new String[lst.size()];
        for (int i = 0, c = lst.size(); i < c; i++) {
            fontFamily[i] = ((Property) lst.get(i)).getString();
        }
        return fontFamily;
    }

    /** @return the first font-family name in the list */
    public String getFirstFontFamily() {
        return ((Property) this.fontFamily.list.get(0)).getString();
    }

    /** @return the "font-selection-strategy" property */
    public int getFontSelectionStrategy() {
        return this.fontSelectionStrategy.getEnum();
    }

    /** @return the "font-stretch" property */
    public int getFontStretch() {
        return this.fontStretch.getEnum();
    }

    /** @return the "font-style" property */
    public int getFontStyle() {
        return this.fontStyle.getEnum();
    }

    /** @return the "font-variant" property */
    public int getFontVariant() {
        return this.fontVariant.getEnum();
    }

    /** @return the "font-weight" property */
    public int getFontWeight() {
        return this.fontWeight.getEnum();
    }

    /** @return the "font-size" property. */
    public Length getFontSize() {
        return this.fontSize;
    }

    /** @return the "font-size-adjust" property. */
    public Numeric getFontSizeAdjust() {
        return this.fontSizeAdjust;
    }

    /**
     * Create and return an array of <code>FontTriplets</code> based on the
     * properties stored in the instance variables.
     *
     * @param fontInfo
     * @return a Font object.
     */
    public FontTriplet[] getFontState(final FontInfo fontInfo) {
        int font_weight;
        switch (this.fontWeight.getEnum()) {
        case Constants.EN_100:
            font_weight = 100;
            break;
        case Constants.EN_200:
            font_weight = 200;
            break;
        case Constants.EN_300:
            font_weight = 300;
            break;
        case Constants.EN_400:
            font_weight = 400;
            break;
        case Constants.EN_500:
            font_weight = 500;
            break;
        case Constants.EN_600:
            font_weight = 600;
            break;
        case Constants.EN_700:
            font_weight = 700;
            break;
        case Constants.EN_800:
            font_weight = 800;
            break;
        case Constants.EN_900:
            font_weight = 900;
            break;
        default:
            font_weight = 400;
        }

        String style;
        switch (this.fontStyle.getEnum()) {
        case Constants.EN_ITALIC:
            style = "italic";
            break;
        case Constants.EN_OBLIQUE:
            style = "oblique";
            break;
        case Constants.EN_BACKSLANT:
            style = "backslant";
            break;
        default:
            style = "normal";
        }
        // NOTE: this is incomplete. font-size may be specified with
        // various kinds of keywords too
        // int fontVariant = propertyList.get("font-variant").getEnum();
        final FontTriplet[] triplets = fontInfo.fontLookup(getFontFamily(),
                style, font_weight);
        return triplets;
    }

    /** {@inheritDoc} */
    @Override
    public boolean equals(final Object o) {

        if (o == null) {
            return false;
        }

        if (this == o) {
            return true;
        }

        if (o instanceof CommonFont) {
            final CommonFont cf = (CommonFont) o;
            return cf.fontFamily == this.fontFamily
                    && cf.fontSelectionStrategy == this.fontSelectionStrategy
                    && cf.fontStretch == this.fontStretch
                    && cf.fontStyle == this.fontStyle
                    && cf.fontVariant == this.fontVariant
                    && cf.fontWeight == this.fontWeight
                    && cf.fontSize == this.fontSize
                    && cf.fontSizeAdjust == this.fontSizeAdjust;
        }
        return false;

    }

    /** {@inheritDoc} */
    @Override
    public int hashCode() {

        if (this.hash == -1) {
            int hash = 17;
            hash = 37 * hash
                    + (this.fontSize == null ? 0 : this.fontSize.hashCode());
            hash = 37
                    * hash
                    + (this.fontSizeAdjust == null ? 0 : this.fontSizeAdjust
                            .hashCode());
            hash = 37
                    * hash
                    + (this.fontFamily == null ? 0 : this.fontFamily.hashCode());
            hash = 37
                    * hash
                    + (this.fontSelectionStrategy == null ? 0
                            : this.fontSelectionStrategy.hashCode());
            hash = 37
                    * hash
                    + (this.fontStretch == null ? 0 : this.fontStretch
                            .hashCode());
            hash = 37 * hash
                    + (this.fontStyle == null ? 0 : this.fontStyle.hashCode());
            hash = 37
                    * hash
                    + (this.fontVariant == null ? 0 : this.fontVariant
                            .hashCode());
            hash = 37
                    * hash
                    + (this.fontStretch == null ? 0 : this.fontStretch
                            .hashCode());
            this.hash = hash;
        }
        return this.hash;

    }
}
