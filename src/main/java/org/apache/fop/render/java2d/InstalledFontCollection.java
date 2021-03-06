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

/* $Id: InstalledFontCollection.java 679326 2008-07-24 09:35:34Z vhennebert $ */

package org.apache.fop.render.java2d;

import java.awt.Graphics2D;
import java.awt.GraphicsEnvironment;
import java.util.HashSet;
import java.util.Set;

import lombok.extern.slf4j.Slf4j;

import org.apache.fop.fonts.Font;
import org.apache.fop.fonts.FontCollection;
import org.apache.fop.fonts.FontInfo;
import org.apache.fop.fonts.FontTriplet;
import org.apache.fop.fonts.FontUtil;

/**
 * A custom AWT font collection
 */
@Slf4j
public class InstalledFontCollection implements FontCollection {

    private static final Set<String> HARDCODED_FONT_NAMES;

    static {
        HARDCODED_FONT_NAMES = new HashSet<>();
        HARDCODED_FONT_NAMES.add("any");
        HARDCODED_FONT_NAMES.add("sans-serif");
        HARDCODED_FONT_NAMES.add("serif");
        HARDCODED_FONT_NAMES.add("monospace");

        HARDCODED_FONT_NAMES.add("Helvetica");
        HARDCODED_FONT_NAMES.add("Times");
        HARDCODED_FONT_NAMES.add("Courier");
        HARDCODED_FONT_NAMES.add("Symbol");
        HARDCODED_FONT_NAMES.add("ZapfDingbats");
        HARDCODED_FONT_NAMES.add("Times Roman");
        HARDCODED_FONT_NAMES.add("Times-Roman");
        HARDCODED_FONT_NAMES.add("Computer-Modern-Typewriter");
    }

    private Graphics2D graphics2D = null;

    /**
     * Main constructor
     *
     * @param graphics2D
     *            a graphics 2D
     */
    public InstalledFontCollection(final Graphics2D graphics2D) {
        this.graphics2D = graphics2D;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int setup(final int start, final FontInfo fontInfo) {
        int num = start;
        final GraphicsEnvironment env = GraphicsEnvironment
                .getLocalGraphicsEnvironment();

        final java.awt.Font[] fonts = env.getAllFonts();
        for (final java.awt.Font font : fonts) {
            final java.awt.Font f = font;
            if (HARDCODED_FONT_NAMES.contains(f.getName())) {
                continue; // skip
            }

            if (log.isTraceEnabled()) {
                log.trace("AWT Font: " + f.getFontName() + ", family: "
                        + f.getFamily() + ", PS: " + f.getPSName() + ", Name: "
                        + f.getName() + ", Angle: " + f.getItalicAngle()
                        + ", Style: " + f.getStyle());
            }

            final String searchName = FontUtil.stripWhiteSpace(f.getName())
                    .toLowerCase();
            final String guessedStyle = FontUtil.guessStyle(searchName);
            final int guessedWeight = FontUtil.guessWeight(searchName);

            num++;
            final String fontKey = "F" + num;
            final int style = convertToAWTFontStyle(guessedStyle, guessedWeight);
            addFontMetricsMapper(fontInfo, f.getName(), fontKey,
                    this.graphics2D, style);

            // Register appropriate font triplets matching the font. Two
            // different strategies:
            // Example: "Arial Bold", normal, normal
            addFontTriplet(fontInfo, f.getName(), Font.STYLE_NORMAL,
                    Font.WEIGHT_NORMAL, fontKey);
            if (!f.getName().equals(f.getFamily())) {
                // Example: "Arial", bold, normal
                addFontTriplet(fontInfo, f.getFamily(), guessedStyle,
                        guessedWeight, fontKey);
            }
        }
        return num;
    }

    private static void addFontTriplet(final FontInfo fontInfo,
            final String fontName, final String fontStyle,
            final int fontWeight, final String fontKey) {
        final FontTriplet triplet = FontInfo.createFontKey(fontName, fontStyle,
                fontWeight);
        fontInfo.addFontProperties(fontKey, triplet);
    }

    private static void addFontMetricsMapper(final FontInfo fontInfo,
            final String family, final String fontKey,
            final Graphics2D graphics, final int style) {
        final FontMetricsMapper metric = new SystemFontMetricsMapper(family,
                style, graphics);
        fontInfo.addMetrics(fontKey, metric);
    }

    private static int convertToAWTFontStyle(final String fontStyle,
            final int fontWeight) {
        int style = java.awt.Font.PLAIN;
        if (fontWeight >= Font.WEIGHT_BOLD) {
            style |= java.awt.Font.BOLD;
        }
        if (!"normal".equals(fontStyle)) {
            style |= java.awt.Font.ITALIC;
        }
        return style;
    }
}
