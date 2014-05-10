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

/* $Id: ACIUtils.java 1304264 2012-03-23 10:26:13Z vhennebert $ */

package org.apache.fop.svg;

import java.awt.font.TextAttribute;
import java.text.AttributedCharacterIterator;
import java.text.AttributedCharacterIterator.Attribute;
import java.text.CharacterIterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import lombok.extern.slf4j.Slf4j;

import org.apache.batik.bridge.SVGFontFamily;
import org.apache.batik.gvt.font.GVTFont;
import org.apache.batik.gvt.font.GVTFontFamily;
import org.apache.batik.gvt.text.GVTAttributedCharacterIterator;
import org.apache.fop.fonts.Font;
import org.apache.fop.fonts.FontInfo;
import org.apache.fop.fonts.FontTriplet;

/**
 * Utilities for java.text.AttributedCharacterIterator.
 */
@Slf4j
public final class ACIUtils {

    private ACIUtils() {
        // This class shouldn't be instantiated.
    }

    /**
     * Tries to find matching fonts in FOP's {@link FontInfo} instance for fonts
     * used by Apache Batik. The method inspects the various GVT attributes
     * found in the ACI.
     *
     * @param aci
     *            the ACI to find matching fonts for
     * @param fontInfo
     *            the font info instance with FOP's fonts
     * @return an array of matching fonts
     */
    public static Font[] findFontsForBatikACI(
            final AttributedCharacterIterator aci, final FontInfo fontInfo) {
        final List<Font> fonts = new java.util.ArrayList<>();
        @SuppressWarnings("unchecked")
        final List<GVTFontFamily> gvtFonts = (List<GVTFontFamily>) aci
                .getAttribute(GVTAttributedCharacterIterator.TextAttribute.GVT_FONT_FAMILIES);
        final Float posture = (Float) aci.getAttribute(TextAttribute.POSTURE);
        final Float taWeight = (Float) aci.getAttribute(TextAttribute.WEIGHT);
        final Float fontSize = (Float) aci.getAttribute(TextAttribute.SIZE);

        final String style = toStyle(posture);
        final int weight = toCSSWeight(taWeight);
        final int fsize = (int) (fontSize.floatValue() * 1000);

        String firstFontFamily = null;

        // GVT_FONT can sometimes be different from the fonts in
        // GVT_FONT_FAMILIES
        // or GVT_FONT_FAMILIES can even be empty and only GVT_FONT is set
        /*
         * The following code section is not available until Batik 1.7 is
         * released.
         */
        final GVTFont gvtFont = (GVTFont) aci
                .getAttribute(GVTAttributedCharacterIterator.TextAttribute.GVT_FONT);
        // TODO fixme
        // if (gvtFont != null) {
        // final String gvtFontFamily = gvtFont.getFamilyName();
        // if (fontInfo.hasFont(gvtFontFamily, style, weight)) {
        // final FontTriplet triplet = fontInfo.fontLookup(gvtFontFamily,
        // style, weight);
        // final Font f = fontInfo.getFontInstance(triplet, fsize);
        // if (log.isDebugEnabled()) {
        // log.debug("Found a font that matches the GVT font: "
        // + gvtFontFamily + ", " + weight + ", " + style
        // + " -> " + f);
        // }
        // fonts.add(f);
        // }
        // firstFontFamily = gvtFontFamily;
        // }

        if (gvtFonts != null) {
            boolean haveInstanceOfSVGFontFamily = false;
            for (final GVTFontFamily fam : gvtFonts) {
                if (fam instanceof SVGFontFamily) {
                    haveInstanceOfSVGFontFamily = true;
                }
                final String fontFamily = fam.getFamilyName();
                if (fontInfo.hasFont(fontFamily, style, weight)) {
                    final FontTriplet triplet = fontInfo.fontLookup(fontFamily,
                            style, weight);
                    final Font f = fontInfo.getFontInstance(triplet, fsize);
                    if (log.isDebugEnabled()) {
                        log.debug("Found a font that matches the GVT font family: "
                                + fontFamily
                                + ", "
                                + weight
                                + ", "
                                + style
                                + " -> " + f);
                    }
                    fonts.add(f);
                }
                if (firstFontFamily == null) {
                    firstFontFamily = fontFamily;
                }
            }
            // SVG fonts are embedded fonts in the SVG document and are rarely
            // used; however if they
            // are used but the fonts also exists in the system and are known to
            // FOP then FOP should
            // use them; then the decision whether Batik should stroke the text
            // should be made after
            // no matching fonts are found
            if (fonts.isEmpty() && haveInstanceOfSVGFontFamily) {
                fontInfo.notifyStrokingSVGTextAsShapes(firstFontFamily);
                return null; // Let Batik paint this text!
            }
        }
        if (fonts.isEmpty()) {
            if (firstFontFamily == null) {
                // This will probably never happen. Just to be on the safe side.
                firstFontFamily = "any";
            }
            // lookup with fallback possibility (incl. substitution
            // notification)
            final FontTriplet triplet = fontInfo.fontLookup(firstFontFamily,
                    style, weight);
            final Font f = fontInfo.getFontInstance(triplet, fsize);
            if (log.isDebugEnabled()) {
                log.debug("Falling back to adjustable font lookup up for: "
                        + firstFontFamily + ", " + weight + ", " + style
                        + " -> " + f);
            }
            fonts.add(f);
        }
        return fonts.toArray(new Font[fonts.size()]);
    }

    private static int toCSSWeight(final Float weight) {
        if (weight == null) {
            return 400;
        } else if (weight <= TextAttribute.WEIGHT_EXTRA_LIGHT.floatValue()) {
            return 100;
        } else if (weight <= TextAttribute.WEIGHT_LIGHT.floatValue()) {
            return 200;
        } else if (weight <= TextAttribute.WEIGHT_DEMILIGHT.floatValue()) {
            return 300;
        } else if (weight <= TextAttribute.WEIGHT_REGULAR.floatValue()) {
            return 400;
        } else if (weight <= TextAttribute.WEIGHT_SEMIBOLD.floatValue()) {
            return 500;
        } else if (weight < TextAttribute.WEIGHT_BOLD.floatValue()) {
            return 600;
        } else if (weight == TextAttribute.WEIGHT_BOLD.floatValue()) {
            return 700;
        } else if (weight <= TextAttribute.WEIGHT_HEAVY.floatValue()) {
            return 800;
        } else if (weight <= TextAttribute.WEIGHT_EXTRABOLD.floatValue()) {
            return 900;
        } else {
            return 900;
        }
    }

    private static String toStyle(final Float posture) {
        return posture != null && posture.floatValue() > 0.0 ? Font.STYLE_ITALIC
                : Font.STYLE_NORMAL;
    }

    /**
     * Dumps the contents of an ACI to System.out. Used for debugging only.
     *
     * @param aci
     *            the ACI to dump
     */
    public static void dumpAttrs(final AttributedCharacterIterator aci) {
        aci.first();
        final Set<Entry<Attribute, Object>> entries = aci.getAttributes()
                .entrySet();
        for (final Map.Entry<Attribute, Object> entry : entries) {
            if (entry.getValue() != null) {
                log.info(entry.getKey() + ": " + entry.getValue());
            }
        }
        int start = aci.getBeginIndex();
        log.info("AttrRuns: ");
        while (aci.current() != CharacterIterator.DONE) {
            final int end = aci.getRunLimit();
            log.info("" + (end - start) + ", ");
            aci.setIndex(end);
            if (start == end) {
                break;
            }
            start = end;
        }
        log.info("");
    }

}
