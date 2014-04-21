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

/* $Id: PDFColor.java 959945 2010-07-02 10:44:18Z jeremias $ */

package org.apache.fop.pdf;

import java.awt.Color;
import java.awt.color.ColorSpace;
import java.awt.color.ICC_ColorSpace;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import lombok.extern.slf4j.Slf4j;

import org.apache.fop.util.ColorExt;
import org.apache.xmlgraphics.java2d.color.DeviceCMYKColorSpace;

/**
 * PDF Color object. This is used to output color to a PDF content stream.
 */
@Slf4j
public class PDFColor extends PDFPathPaint {
    // could be 3.0 as well.
    private static double blackFactor = 2.0;
    private double red = -1.0;
    private double green = -1.0;
    private double blue = -1.0;

    private double cyan = -1.0;
    private double magenta = -1.0;
    private double yellow = -1.0;
    private double black = -1.0;

    // TODO - It would probably be better to reorganize
    // PDFPathPaint/PDFColor/PDFColorSpace
    // class hierarchy. However, at this early stages of my FOP understanding, I
    // can
    // not really oversee the consequences of such a switch (nor whether it
    // would be
    // appropriate).
    private ColorExt colorExt = null;

    /**
     * Create a PDF color with double values ranging from 0 to 1
     *
     * @param theRed
     *            the red double value
     * @param theGreen
     *            the green double value
     * @param theBlue
     *            the blue double value
     */
    public PDFColor(final double theRed, final double theGreen,
            final double theBlue) {
        // super(theNumber);
        this.colorSpace = new PDFDeviceColorSpace(
                PDFDeviceColorSpace.DEVICE_RGB);

        this.red = theRed;
        this.green = theGreen;
        this.blue = theBlue;
    }

    /**
     * Create PDFColor for the given document and based on the java.awt.Color
     * object
     *
     * In case the java.awt.Color is an instance of the ColorExt class a
     * PDFICCStream is added to the PDFDocument that is being created
     *
     * @param pdfDoc
     *            PDFDocument that is being created
     * @param col
     *            Color object from which to create this PDFColor
     */
    public PDFColor(final PDFDocument pdfDoc, final Color col) {
        this(col);
        // TODO - 1) There is a potential conflict when FOP and Batik elements
        // use the same color
        // profile name for different profiles.
        // 2) In case the same color profile is used with different names it
        // will be
        // included multiple times in the PDF
        //
        if (this.colorExt != null
                && pdfDoc.getResources().getColorSpace(
                        this.colorExt.getIccProfileName()) == null) {
            final PDFICCStream pdfIccStream = new PDFICCStream();
            final ColorSpace ceCs = this.colorExt.getOrigColorSpace();
            try {
                pdfIccStream.setColorSpace(
                        ((ICC_ColorSpace) ceCs).getProfile(), null);
                pdfIccStream.setData(((ICC_ColorSpace) this.colorExt
                        .getColorSpace()).getProfile().getData());
            } catch (final IOException ioe) {
                log.error("Failed to set profile data for "
                        + this.colorExt.getIccProfileName());
            }
            pdfDoc.registerObject(pdfIccStream);
            pdfDoc.getFactory().makeICCBasedColorSpace(null,
                    this.colorExt.getIccProfileName(), pdfIccStream);
            if (log.isInfoEnabled()) {
                log.info("Adding PDFICCStream "
                        + this.colorExt.getIccProfileName() + " for "
                        + this.colorExt.getIccProfileSrc());
            }
        }
    }

    /**
     * Create a PDF color from a java.awt.Color object.
     *
     * Different Color objects are handled differently. Cases recognized are.
     *
     * 1. CMYK color 2. ColorExt color 3. 'Normal' java.awt.Color (RGB case
     * assumed)
     *
     * @param col
     *            the java.awt.Color object for which to create a PDFColor
     *            object
     */
    public PDFColor(final java.awt.Color col) {
        ColorSpace cs = col.getColorSpace();
        ColorExt ce = null;
        if (col instanceof ColorExt) {
            ce = (ColorExt) col;
            cs = ce.getOrigColorSpace();
        }
        if (cs != null && cs instanceof DeviceCMYKColorSpace) {
            // CMYK case
            this.colorSpace = new PDFDeviceColorSpace(
                    PDFDeviceColorSpace.DEVICE_CMYK);
            final float[] cmyk = ce == null ? col.getColorComponents(null) : ce
                    .getOriginalColorComponents();
            this.cyan = cmyk[0];
            this.magenta = cmyk[1];
            this.yellow = cmyk[2];
            this.black = cmyk[3];
        } else if (ce != null) {
            // ColorExt (ICC) case
            this.colorExt = ce;
            final float[] rgb = col.getRGBColorComponents(null);
            this.red = rgb[0];
            this.green = rgb[1];
            this.blue = rgb[2];
            // TODO - See earlier todo
            this.colorSpace = new PDFDeviceColorSpace(
                    PDFDeviceColorSpace.DEVICE_RGB);
        } else {
            // Default (RGB) Color
            this.colorSpace = new PDFDeviceColorSpace(
                    PDFDeviceColorSpace.DEVICE_RGB);
            float[] comps = new float[3];
            comps = col.getColorComponents(comps);
            this.red = comps[0];
            this.green = comps[1];
            this.blue = comps[2];
        }
    }

    /**
     * Create a PDF color with int values ranging from 0 to 255
     *
     * @param theRed
     *            the red integer value
     * @param theGreen
     *            the green integer value
     * @param theBlue
     *            the blue integer value
     */
    public PDFColor(final int theRed, final int theGreen, final int theBlue) {
        this(theRed / 255d, theGreen / 255d, theBlue / 255d);

    }

    /**
     * Create a PDF color with CMYK values.
     *
     * @param theCyan
     *            the cyan value
     * @param theMagenta
     *            the magenta value
     * @param theYellow
     *            the yellow value
     * @param theBlack
     *            the black value
     */
    public PDFColor(final double theCyan, final double theMagenta,
            final double theYellow, final double theBlack) {
        // super(theNumber);//?

        this.colorSpace = new PDFDeviceColorSpace(
                PDFDeviceColorSpace.DEVICE_CMYK);

        this.cyan = theCyan;
        this.magenta = theMagenta;
        this.yellow = theYellow;
        this.black = theBlack;
    }

    /**
     * Return a vector representation of the color in the appropriate
     * colorspace.
     *
     * @return a list containing the Double values of the color
     */
    public List<Double> getList() {
        final List<Double> theColorList = new ArrayList<>();
        if (this.colorSpace.getColorSpace() == PDFDeviceColorSpace.DEVICE_RGB) {
            // RGB
            theColorList.add(new Double(this.red));
            theColorList.add(new Double(this.green));
            theColorList.add(new Double(this.blue));
        } else if (this.colorSpace.getColorSpace() == PDFDeviceColorSpace.DEVICE_CMYK) {
            // CMYK
            theColorList.add(new Double(this.cyan));
            theColorList.add(new Double(this.magenta));
            theColorList.add(new Double(this.yellow));
            theColorList.add(new Double(this.black));
        } else {
            // GRAY
            theColorList.add(new Double(this.black));
        }
        return theColorList;
    }

    /**
     * Get the red component.
     *
     * @return the red double value
     */
    public double red() {
        return this.red;
    }

    /**
     * Get the green component.
     *
     * @return the green double value
     */
    public double green() {
        return this.green;
    }

    /**
     * Get the blue component.
     *
     * @return the blue double value
     */
    public double blue() {
        return this.blue;
    }

    /**
     * Get the red integer component.
     *
     * @return the red integer value
     */
    public int red255() {
        return (int) (this.red * 255d);
    }

    /**
     * Get the green integer component.
     *
     * @return the green integer value
     */
    public int green255() {
        return (int) (this.green * 255d);
    }

    /**
     * Get the blue integer component.
     *
     * @return the blue integer value
     */
    public int blue255() {
        return (int) (this.blue * 255d);
    }

    /**
     * Get the cyan component.
     *
     * @return the cyan double value
     */
    public double cyan() {
        return this.cyan;
    }

    /**
     * Get the magenta component.
     *
     * @return the magenta double value
     */
    public double magenta() {
        return this.magenta;
    }

    /**
     * Get the yellow component.
     *
     * @return the yellow double value
     */
    public double yellow() {
        return this.yellow;
    }

    /**
     * Get the black component.
     *
     * @return the black double value
     */
    public double black() {
        return this.black;
    }

    /**
     * Set the color space for this color. If the new color space is different
     * the values are converted to the new color space.
     *
     * @param theColorSpace
     *            the new color space
     */
    @Override
    public void setColorSpace(final int theColorSpace) {
        final int theOldColorSpace = this.colorSpace.getColorSpace();
        if (theOldColorSpace != theColorSpace) {
            if (theOldColorSpace == PDFDeviceColorSpace.DEVICE_RGB) {
                if (theColorSpace == PDFDeviceColorSpace.DEVICE_CMYK) {
                    convertRGBtoCMYK();
                } else {
                    // convert to Gray?
                    convertRGBtoGRAY();
                }
            } else if (theOldColorSpace == PDFDeviceColorSpace.DEVICE_CMYK) {
                if (theColorSpace == PDFDeviceColorSpace.DEVICE_RGB) {
                    convertCMYKtoRGB();
                } else {
                    // convert to Gray?
                    convertCMYKtoGRAY();
                }
            } else {
                // used to be Gray
                if (theColorSpace == PDFDeviceColorSpace.DEVICE_RGB) {
                    convertGRAYtoRGB();
                } else {
                    // convert to CMYK?
                    convertGRAYtoCMYK();
                }
            }
            this.colorSpace.setColorSpace(theColorSpace);
        }
    }

    /**
     * Get the PDF output string for this color. This returns the string to be
     * inserted into PDF for setting the current color.
     *
     * @param fillNotStroke
     *            whether to return fill or stroke command
     * @return the PDF string for setting the fill/stroke color
     */
    @Override
    public String getColorSpaceOut(final boolean fillNotStroke) {
        final StringBuilder p = new StringBuilder("");

        if (this.colorExt != null) {
            if (fillNotStroke) {
                p.append("/" + this.colorExt.getIccProfileName() + " cs ");
            } else {
                p.append("/" + this.colorExt.getIccProfileName() + " CS ");
            }
            float[] colorArgs;
            colorArgs = this.colorExt.getOriginalColorComponents();
            if (colorArgs == null) {
                colorArgs = this.colorExt.getColorComponents(null);
            }
            for (final float colorArg : colorArgs) {
                p.append(colorArg + " ");
            }
            if (fillNotStroke) {
                p.append("sc\n");
            } else {
                p.append("SC\n");
            }
        } else if (this.colorSpace.getColorSpace() == PDFDeviceColorSpace.DEVICE_RGB) { // colorspace
            // is
            // RGB
            // according to pdfspec 12.1 p.399
            // if the colors are the same then just use the g or G operator
            boolean same = false;
            if (this.red == this.green && this.red == this.blue) {
                same = true;
            }
            // output RGB
            if (fillNotStroke) {
                // fill
                if (same) {
                    p.append(PDFNumber.doubleOut(this.red) + " g\n");
                } else {
                    p.append(PDFNumber.doubleOut(this.red) + " "
                            + PDFNumber.doubleOut(this.green) + " "
                            + PDFNumber.doubleOut(this.blue) + " rg\n");
                }
            } else {
                // stroke/border
                if (same) {
                    p.append(PDFNumber.doubleOut(this.red) + " G\n");
                } else {
                    p.append(PDFNumber.doubleOut(this.red) + " "
                            + PDFNumber.doubleOut(this.green) + " "
                            + PDFNumber.doubleOut(this.blue) + " RG\n");
                }
            }
        } else if (this.colorSpace.getColorSpace() == PDFDeviceColorSpace.DEVICE_CMYK) {
            // colorspace is CMYK

            if (fillNotStroke) {
                // fill
                p.append(PDFNumber.doubleOut(this.cyan) + " "
                        + PDFNumber.doubleOut(this.magenta) + " "
                        + PDFNumber.doubleOut(this.yellow) + " "
                        + PDFNumber.doubleOut(this.black) + " k\n");
            } else {
                // stroke
                p.append(PDFNumber.doubleOut(this.cyan) + " "
                        + PDFNumber.doubleOut(this.magenta) + " "
                        + PDFNumber.doubleOut(this.yellow) + " "
                        + PDFNumber.doubleOut(this.black) + " K\n");
            }

        } else {
            // means we're in DeviceGray or Unknown.
            // assume we're in DeviceGray, because otherwise we're screwed.

            if (fillNotStroke) {
                p.append(PDFNumber.doubleOut(this.black) + " g\n");
            } else {
                p.append(PDFNumber.doubleOut(this.black) + " G\n");
            }

        }
        return p.toString();
    }

    /**
     * Convert the color from CMYK to RGB.
     */
    protected void convertCMYKtoRGB() {
        // convert CMYK to RGB
        this.red = 1.0 - this.cyan;
        this.green = 1.0 - this.green;
        this.blue = 1.0 - this.yellow;

        this.red = this.black / PDFColor.blackFactor + this.red;
        this.green = this.black / PDFColor.blackFactor + this.green;
        this.blue = this.black / PDFColor.blackFactor + this.blue;

    }

    /**
     * Convert the color from RGB to CMYK.
     */
    protected void convertRGBtoCMYK() {
        // convert RGB to CMYK
        this.cyan = 1.0 - this.red;
        this.magenta = 1.0 - this.green;
        this.yellow = 1.0 - this.blue;

        this.black = 0.0;
        /*
         * If you want to calculate black, uncomment this //pick the lowest
         * color tempDouble = this.red;
         * 
         * if (this.green < tempDouble) tempDouble = this.green;
         * 
         * if (this.blue < tempDouble) tempDouble = this.blue;
         * 
         * this.black = tempDouble / this.blackFactor;
         */
    }

    /**
     * Convert the color from Gray to RGB.
     */
    protected void convertGRAYtoRGB() {
        this.red = 1.0 - this.black;
        this.green = 1.0 - this.black;
        this.blue = 1.0 - this.black;
    }

    /**
     * Convert the color from Gray to CMYK.
     */
    protected void convertGRAYtoCMYK() {
        this.cyan = this.black;
        this.magenta = this.black;
        this.yellow = this.black;
        // this.black=0.0;//?
    }

    /**
     * Convert the color from CMYK to Gray.
     */
    protected void convertCMYKtoGRAY() {
        double tempDouble = 0.0;

        // pick the lowest color
        tempDouble = this.cyan;

        if (this.magenta < tempDouble) {
            tempDouble = this.magenta;
        }

        if (this.yellow < tempDouble) {
            tempDouble = this.yellow;
        }

        this.black = tempDouble / PDFColor.blackFactor;

    }

    /**
     * Convert the color from RGB to Gray.
     */
    protected void convertRGBtoGRAY() {
        double tempDouble = 0.0;

        // pick the lowest color
        tempDouble = this.red;

        if (this.green < tempDouble) {
            tempDouble = this.green;
        }

        if (this.blue < tempDouble) {
            tempDouble = this.blue;
        }

        this.black = 1.0 - tempDouble / PDFColor.blackFactor;
    }

    /**
     * Create pdf. Not used for this object.
     *
     * @return the bytes for the pdf
     */
    @Override
    public byte[] toPDF() {
        return new byte[0];
    }

    /** {@inheritDoc} */
    @Override
    protected boolean contentEquals(final PDFObject obj) {
        if (!(obj instanceof PDFColor)) {
            return false;
        }
        final PDFColor color = (PDFColor) obj;

        if (color.red == this.red && color.green == this.green
                && color.blue == this.blue) {
            return true;
        }
        return false;
    }

}
