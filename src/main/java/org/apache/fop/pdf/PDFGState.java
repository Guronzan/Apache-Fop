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

/* $Id: PDFGState.java 815358 2009-09-15 15:07:51Z maxberger $ */

package org.apache.fop.pdf;

import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

/**
 * Class representing a /ExtGState object.
 */
public class PDFGState extends PDFObject {

    /** Line width (LW) */
    public static final String GSTATE_LINE_WIDTH = "LW";
    /** Line cap (LC) */
    public static final String GSTATE_LINE_CAP = "LC";
    /** Line join (LJ) */
    public static final String GSTATE_LINE_JOIN = "LJ";
    /** Miter limit (ML) */
    public static final String GSTATE_MITER_LIMIT = "ML";
    /** Dash pattern (D) */
    public static final String GSTATE_DASH_PATTERN = "D";
    /** Rendering intent (RI) */
    public static final String GSTATE_RENDERING_INTENT = "RI";
    /** Overprint for stroke (OP) */
    public static final String GSTATE_OVERPRINT_STROKE = "OP";
    /** Overprint for fill (op) */
    public static final String GSTATE_OVERPRINT_FILL = "op";
    /** Overprint mode (OPM) */
    public static final String GSTATE_OVERPRINT_MODE = "OPM";
    /** Font (Font) */
    public static final String GSTATE_FONT = "Font";
    /** Black generation (BG) */
    public static final String GSTATE_BLACK_GENERATION = "BG";
    /** Black generation with default (BG2) */
    public static final String GSTATE_BLACK_GENERATION2 = "BG2";
    /** Undercolor removal function (UCR) */
    public static final String GSTATE_UNDERCOLOR_REMOVAL = "UCR";
    /** Undercolor removal function with default (UCR2) */
    public static final String GSTATE_UNDERCOLOR_REMOVAL2 = "UCR2";
    /** Transfer function (TR) */
    public static final String GSTATE_TRANSFER_FUNCTION = "TR";
    /** Transfer function with default (TR2) */
    public static final String GSTATE_TRANSFER_FUNCTION2 = "TR2";
    /** Halftone dictionary or stream (HT) */
    public static final String GSTATE_HALFTONE_DICT = "HT";
    /** Halftone phase (HTP, does not show up anymore in PDF 1.4) */
    public static final String GSTATE_HALFTONE_PHASE = "HTP";
    /** Flatness (FL) */
    public static final String GSTATE_FLATNESS = "FL";
    /** Smoothness (SM) */
    public static final String GSTATE_SMOOTHNESS = "SM";
    /** Strike adjustment (SA) */
    public static final String GSTATE_STRIKE_ADJ = "SA";
    /** Blend mode (BM, PDF 1.4) */
    public static final String GSTATE_BLEND_MODE = "BM";
    /** Soft mask (SMask, PDF 1.4) */
    public static final String GSTATE_SOFT_MASK = "SMask";
    /** Stroking Alpha (CA, PDF 1.4) */
    public static final String GSTATE_ALPHA_STROKE = "CA";
    /** Nonstroking Alpha (ca, PDF 1.4) */
    public static final String GSTATE_ALPHA_NONSTROKE = "ca";
    /** Alpha Source Flag (AIS, PDF 1.4) */
    public static final String GSTATE_ALPHA_SOURCE_FLAG = "AIS";
    /** Text Knockout Flag (TK, PDF 1.4) */
    public static final String GSTATE_TEXT_KNOCKOUT = "TK";

    /** Default GState object */
    public static final PDFGState DEFAULT;

    static {
        DEFAULT = new PDFGState();
        final Map<String, Float> vals = DEFAULT.values;
        /*
         * vals.put(LW, new Float(1.0)); vals.put(LC, (0)); vals.put(LJ, (0));
         * vals.put(ML, new Float(10.0)); vals.put(D, "0 []"); vals.put(RI,
         * "RelativeColorimetric"); vals.put(OP, Boolean.FALSE); vals.put(op,
         * Boolean.FALSE); vals.put(OPM, (1)); vals.put(Font, "");
         */

        vals.put(GSTATE_ALPHA_STROKE, new Float(1.0));
        vals.put(GSTATE_ALPHA_NONSTROKE, new Float(1.0));
    }

    private final Map<String, Float> values = new HashMap<>();

    /**
     * Returns the name of this object
     *
     * @return the name
     */
    public String getName() {
        return "GS" + getObjectNumber();
    }

    /**
     * Sets the alpha value.
     *
     * @param val
     *            alpha value (0.0 - 1.0)
     * @param fill
     *            True if alpha should be set for non-stroking operations, False
     *            if for stroking operations
     */
    public void setAlpha(final float val, final boolean fill) {
        if (fill) {
            this.values.put(GSTATE_ALPHA_NONSTROKE, new Float(val));
        } else {
            this.values.put(GSTATE_ALPHA_STROKE, new Float(val));
        }
    }

    /**
     * Adds all values from another GState object to this one.
     *
     * @param state
     *            source object to copy from
     */
    public void addValues(final PDFGState state) {
        this.values.putAll(state.values);
    }

    /**
     * Adds all values from a Map to this object.
     *
     * @param vals
     *            source object to copy from
     */
    public void addValues(final Map<String, Float> vals) {
        this.values.putAll(vals);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String toPDFString() {
        final StringBuilder sb = new StringBuilder(64);
        sb.append(getObjectID());
        sb.append("<<\n/Type /ExtGState\n");
        appendVal(sb, GSTATE_ALPHA_NONSTROKE);
        appendVal(sb, GSTATE_ALPHA_STROKE);

        sb.append(">>\nendobj\n");
        return sb.toString();
    }

    private void appendVal(final StringBuilder sb, final String name) {
        final Object val = this.values.get(name);
        if (val != null) {
            sb.append("/" + name + " " + val + "\n");
        }
    }

    /*
     * example 29 0 obj << /Type /ExtGState /ca 0.5 >> endobj
     */

    /** {@inheritDoc} */
    @Override
    protected boolean contentEquals(final PDFObject obj) {
        if (obj == this) {
            return true;
        }
        if (!(obj instanceof PDFGState)) {
            return false;
        }
        final Map<String, Float> vals1 = this.values;
        final Map<String, Float> vals2 = ((PDFGState) obj).values;
        if (vals1.size() != vals2.size()) {
            return false;
        }
        for (final Entry<String, Float> entry : vals1.entrySet()) {
            final String str = entry.getKey();
            final Float obj1 = entry.getValue();
            if (!obj1.equals(vals2.get(str))) {
                return false;
            }
        }
        return true;
    }
}
