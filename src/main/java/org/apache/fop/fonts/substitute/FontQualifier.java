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

/* $Id: FontQualifier.java 679326 2008-07-24 09:35:34Z vhennebert $ */

package org.apache.fop.fonts.substitute;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import lombok.extern.slf4j.Slf4j;

import org.apache.fop.fonts.Font;
import org.apache.fop.fonts.FontInfo;
import org.apache.fop.fonts.FontTriplet;
import org.apache.fop.fonts.FontUtil;

/**
 * Encapsulates a font substitution qualifier
 */
@Slf4j
public class FontQualifier {

    /** font family attribute value */
    private AttributeValue fontFamilyAttributeValue = null;

    /** font style attribute value */
    private AttributeValue fontStyleAttributeValue = null;

    /** font weight attribute value */
    private AttributeValue fontWeightAttributeValue = null;

    /**
     * Default constructor
     */
    public FontQualifier() {
    }

    /**
     * Sets the font family
     *
     * @param fontFamily
     *            the font family
     */
    public void setFontFamily(final String fontFamily) {
        final AttributeValue fontFamilyAttribute = AttributeValue
                .valueOf(fontFamily);
        if (fontFamilyAttribute == null) {
            log.error("Invalid font-family value '" + fontFamily + "'");
            return;
        }
        this.fontFamilyAttributeValue = fontFamilyAttribute;
    }

    /**
     * Sets the font style
     *
     * @param fontStyle
     *            the font style
     */
    public void setFontStyle(final String fontStyle) {
        final AttributeValue fontStyleAttribute = AttributeValue
                .valueOf(fontStyle);
        if (fontStyleAttribute != null) {
            this.fontStyleAttributeValue = fontStyleAttribute;
        }
    }

    /**
     * Sets the font weight
     *
     * @param fontWeight
     *            the font weight
     */
    public void setFontWeight(final String fontWeight) {
        final AttributeValue fontWeightAttribute = AttributeValue
                .valueOf(fontWeight);
        if (fontWeightAttribute != null) {
            for (final Object weightObj : fontWeightAttribute) {
                if (weightObj instanceof String) {
                    final String weightString = ((String) weightObj).trim();
                    try {
                        FontUtil.parseCSS2FontWeight(weightString);
                    } catch (final IllegalArgumentException ex) {
                        log.error("Invalid font-weight value '" + weightString
                                + "'");
                        return;
                    }
                }
            }
            this.fontWeightAttributeValue = fontWeightAttribute;
        }
    }

    /**
     * @return the font family attribute
     */
    public AttributeValue getFontFamily() {
        return this.fontFamilyAttributeValue;
    }

    /**
     * @return the font style attribute
     */
    public AttributeValue getFontStyle() {
        if (this.fontStyleAttributeValue == null) {
            return AttributeValue.valueOf(Font.STYLE_NORMAL);
        }
        return this.fontStyleAttributeValue;
    }

    /**
     * @return the font weight attribute
     */
    public AttributeValue getFontWeight() {
        if (this.fontWeightAttributeValue == null) {
            return AttributeValue.valueOf(Integer.toString(Font.WEIGHT_NORMAL));
        }
        return this.fontWeightAttributeValue;
    }

    /**
     * @return true if this rule has a font weight
     */
    public boolean hasFontWeight() {
        return this.fontWeightAttributeValue != null;
    }

    /**
     * @return true if this rule has a font style
     */
    public boolean hasFontStyle() {
        return this.fontStyleAttributeValue != null;
    }

    /**
     * Returns a list of matching font triplet found in a given font info
     *
     * @param fontInfo
     *            the font info
     * @return a list of matching font triplets
     */
    protected List<FontTriplet> match(final FontInfo fontInfo) {
        final AttributeValue fontFamilyValue = getFontFamily();
        final AttributeValue weightValue = getFontWeight();
        final AttributeValue styleValue = getFontStyle();

        final List<FontTriplet> matchingTriplets = new ArrayList<>();

        // try to find matching destination font triplet
        for (final Object element : fontFamilyValue) {
            final String fontFamilyString = (String) element;
            final Map<FontTriplet, String> triplets = fontInfo
                    .getFontTriplets();
            if (triplets != null) {
                final Set<FontTriplet> tripletSet = triplets.keySet();
                for (final FontTriplet fontTriplet : tripletSet) {
                    final FontTriplet triplet = fontTriplet;
                    final String fontName = triplet.getName();

                    // matched font family name
                    if (fontFamilyString.toLowerCase().equals(
                            fontName.toLowerCase())) {

                        // try and match font weight
                        boolean weightMatched = false;
                        final int fontWeight = triplet.getWeight();
                        for (final Object weightObj : weightValue) {
                            if (weightObj instanceof FontWeightRange) {
                                final FontWeightRange intRange = (FontWeightRange) weightObj;
                                if (intRange.isWithinRange(fontWeight)) {
                                    weightMatched = true;
                                }
                            } else if (weightObj instanceof String) {
                                final String fontWeightString = (String) weightObj;
                                final int fontWeightValue = FontUtil
                                        .parseCSS2FontWeight(fontWeightString);
                                if (fontWeightValue == fontWeight) {
                                    weightMatched = true;
                                }
                            } else if (weightObj instanceof Integer) {
                                final Integer fontWeightInteger = (Integer) weightObj;
                                final int fontWeightValue = fontWeightInteger
                                        .intValue();
                                if (fontWeightValue == fontWeight) {
                                    weightMatched = true;
                                }
                            }
                        }

                        // try and match font style
                        boolean styleMatched = false;
                        final String fontStyleString = triplet.getStyle();
                        for (final Object element2 : styleValue) {
                            final String style = (String) element2;
                            if (fontStyleString.equals(style)) {
                                styleMatched = true;
                            }
                        }

                        if (weightMatched && styleMatched) {
                            matchingTriplets.add(triplet);
                        }
                    }
                }
            }
        }
        return matchingTriplets;
    }

    /**
     * Returns the highest priority matching font triplet found in a given font
     * info
     *
     * @param fontInfo
     *            the font info
     * @return the highest priority matching font triplet
     */
    protected FontTriplet bestMatch(final FontInfo fontInfo) {
        final List<FontTriplet> matchingTriplets = match(fontInfo);
        FontTriplet bestTriplet = null;
        if (matchingTriplets.size() == 1) {
            bestTriplet = matchingTriplets.get(0);
        } else {
            for (final Object element : matchingTriplets) {
                final FontTriplet triplet = (FontTriplet) element;
                if (bestTriplet == null) {
                    bestTriplet = triplet;
                } else {
                    final int priority = triplet.getPriority();
                    if (priority < bestTriplet.getPriority()) {
                        bestTriplet = triplet;
                    }
                }
            }
        }
        return bestTriplet;
    }

    /**
     * @return a list of font triplets matching this qualifier
     */
    public List<FontTriplet> getTriplets() {
        final List<FontTriplet> triplets = new java.util.ArrayList<>();

        final AttributeValue fontFamilyValue = getFontFamily();
        for (final Object element2 : fontFamilyValue) {
            final String name = (String) element2;

            final AttributeValue styleValue = getFontStyle();
            for (final Object element3 : styleValue) {
                final String style = (String) element3;

                final AttributeValue weightValue = getFontWeight();
                for (final Object weightObj : weightValue) {
                    if (weightObj instanceof FontWeightRange) {
                        final FontWeightRange fontWeightRange = (FontWeightRange) weightObj;
                        final int[] weightRange = fontWeightRange.toArray();
                        for (final int element : weightRange) {
                            triplets.add(new FontTriplet(name, style, element));
                        }
                    } else if (weightObj instanceof String) {
                        final String weightString = (String) weightObj;
                        final int weight = FontUtil
                                .parseCSS2FontWeight(weightString);
                        triplets.add(new FontTriplet(name, style, weight));
                    } else if (weightObj instanceof Integer) {
                        final Integer weightInteger = (Integer) weightObj;
                        final int weight = weightInteger.intValue();
                        triplets.add(new FontTriplet(name, style, weight));
                    }
                }
            }
        }
        return triplets;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String toString() {
        String str = new String();
        if (this.fontFamilyAttributeValue != null) {
            str += "font-family=" + this.fontFamilyAttributeValue;
        }
        if (this.fontStyleAttributeValue != null) {
            if (str.length() > 0) {
                str += ", ";
            }
            str += "font-style=" + this.fontStyleAttributeValue;
        }
        if (this.fontWeightAttributeValue != null) {
            if (str.length() > 0) {
                str += ", ";
            }
            str += "font-weight=" + this.fontWeightAttributeValue;
        }
        return str;
    }
}
