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

/* $Id: AFPBorderPainter.java 937315 2010-04-23 14:39:20Z jeremias $ */

package org.apache.fop.afp;

import java.awt.geom.AffineTransform;

import lombok.extern.slf4j.Slf4j;

import org.apache.fop.fo.Constants;
import org.apache.fop.util.ColorUtil;

/**
 * Handles the drawing of borders/lines in AFP
 */
@Slf4j
public class AFPBorderPainter extends AbstractAFPPainter {

    /**
     * Main constructor
     *
     * @param paintingState
     *            the AFP painting state converter
     * @param dataStream
     *            the AFP datastream
     */
    public AFPBorderPainter(final AFPPaintingState paintingState,
            final DataStream dataStream) {
        super(paintingState, dataStream);
    }

    /** {@inheritDoc} */
    @Override
    public void paint(final PaintingInfo paintInfo) {
        final BorderPaintingInfo borderPaintInfo = (BorderPaintingInfo) paintInfo;
        final float w = borderPaintInfo.getX2() - borderPaintInfo.getX1();
        final float h = borderPaintInfo.getY2() - borderPaintInfo.getY1();
        if (w < 0 || h < 0) {
            log.error("Negative extent received. Border won't be painted.");
            return;
        }

        final int pageWidth = this.dataStream.getCurrentPage().getWidth();
        final int pageHeight = this.dataStream.getCurrentPage().getHeight();
        final AFPUnitConverter unitConv = this.paintingState.getUnitConverter();
        final AffineTransform at = this.paintingState.getData().getTransform();

        float x1 = unitConv.pt2units(borderPaintInfo.getX1());
        float y1 = unitConv.pt2units(borderPaintInfo.getY1());
        float x2 = unitConv.pt2units(borderPaintInfo.getX2());
        float y2 = unitConv.pt2units(borderPaintInfo.getY2());

        switch (this.paintingState.getRotation()) {
        case 0:
            x1 += at.getTranslateX();
            y1 += at.getTranslateY();
            x2 += at.getTranslateX();
            y2 += at.getTranslateY();
            break;
        case 90:
            x1 += at.getTranslateY();
            y1 += (float) (pageWidth - at.getTranslateX());
            x2 += at.getTranslateY();
            y2 += (float) (pageWidth - at.getTranslateX());
            break;
        case 180:
            x1 += (float) (pageWidth - at.getTranslateX());
            y1 += (float) (pageHeight - at.getTranslateY());
            x2 += (float) (pageWidth - at.getTranslateX());
            y2 += (float) (pageHeight - at.getTranslateY());
            break;
        case 270:
            x1 = (float) (pageHeight - at.getTranslateY());
            y1 += (float) at.getTranslateX();
            x2 += x1;
            y2 += (float) at.getTranslateX();
            break;
        }

        AFPLineDataInfo lineDataInfo = new AFPLineDataInfo();
        lineDataInfo.setColor(borderPaintInfo.getColor());
        lineDataInfo.setRotation(this.paintingState.getRotation());
        lineDataInfo.x1 = Math.round(x1);
        lineDataInfo.y1 = Math.round(y1);
        float thickness;
        if (borderPaintInfo.isHorizontal()) {
            thickness = y2 - y1;
        } else {
            thickness = x2 - x1;
        }
        lineDataInfo.setThickness(Math.round(thickness));

        // handle border-*-style
        switch (borderPaintInfo.getStyle()) {
        case Constants.EN_DOUBLE:
            final int thickness3 = (int) Math.floor(thickness / 3f);
            lineDataInfo.setThickness(thickness3);
            if (borderPaintInfo.isHorizontal()) {
                lineDataInfo.x2 = Math.round(x2);
                lineDataInfo.y2 = lineDataInfo.y1;
                this.dataStream.createLine(lineDataInfo);
                final int distance = thickness3 * 2;
                lineDataInfo = new AFPLineDataInfo(lineDataInfo);
                lineDataInfo.y1 += distance;
                lineDataInfo.y2 += distance;
                this.dataStream.createLine(lineDataInfo);
            } else {
                lineDataInfo.x2 = lineDataInfo.x1;
                lineDataInfo.y2 = Math.round(y2);
                this.dataStream.createLine(lineDataInfo);
                final int distance = thickness3 * 2;
                lineDataInfo = new AFPLineDataInfo(lineDataInfo);
                lineDataInfo.x1 += distance;
                lineDataInfo.x2 += distance;
                this.dataStream.createLine(lineDataInfo);
            }
            break;
        case Constants.EN_DASHED:
            final int thick = lineDataInfo.thickness * 3;
            if (borderPaintInfo.isHorizontal()) {
                lineDataInfo.x2 = lineDataInfo.x1 + thick;
                lineDataInfo.y2 = lineDataInfo.y1;
                final int ex2 = Math.round(x2);
                while (lineDataInfo.x1 + thick < ex2) {
                    this.dataStream.createLine(lineDataInfo);
                    lineDataInfo.x1 += 2 * thick;
                    lineDataInfo.x2 = lineDataInfo.x1 + thick;
                }
            } else {
                lineDataInfo.x2 = lineDataInfo.x1;
                lineDataInfo.y2 = lineDataInfo.y1 + thick;
                final int ey2 = Math.round(y2);
                while (lineDataInfo.y1 + thick < ey2) {
                    this.dataStream.createLine(lineDataInfo);
                    lineDataInfo.y1 += 2 * thick;
                    lineDataInfo.y2 = lineDataInfo.y1 + thick;
                }
            }
            break;
        case Constants.EN_DOTTED:
            if (borderPaintInfo.isHorizontal()) {
                lineDataInfo.x2 = lineDataInfo.x1 + lineDataInfo.thickness;
                lineDataInfo.y2 = lineDataInfo.y1;
                final int ex2 = Math.round(x2);
                while (lineDataInfo.x1 + lineDataInfo.thickness < ex2) {
                    this.dataStream.createLine(lineDataInfo);
                    lineDataInfo.x1 += 3 * lineDataInfo.thickness;
                    lineDataInfo.x2 = lineDataInfo.x1 + lineDataInfo.thickness;
                }
            } else {
                lineDataInfo.x2 = lineDataInfo.x1;
                lineDataInfo.y2 = lineDataInfo.y1 + lineDataInfo.thickness;
                final int ey2 = Math.round(y2);
                while (lineDataInfo.y1 + lineDataInfo.thickness < ey2) {
                    this.dataStream.createLine(lineDataInfo);
                    lineDataInfo.y1 += 3 * lineDataInfo.thickness;
                    lineDataInfo.y2 = lineDataInfo.y1 + lineDataInfo.thickness;
                }
            }
            break;
        case Constants.EN_GROOVE:
        case Constants.EN_RIDGE:
            // TODO
            lineDataInfo.x2 = Math.round(x2);
            final float colFactor = borderPaintInfo.getStyle() == Constants.EN_GROOVE ? 0.4f
                    : -0.4f;
            final float h3 = (y2 - y1) / 3;
            lineDataInfo.color = ColorUtil.lightenColor(
                    borderPaintInfo.getColor(), -colFactor);
            lineDataInfo.thickness = Math.round(h3);
            lineDataInfo.y1 = lineDataInfo.y2 = Math.round(y1);
            this.dataStream.createLine(lineDataInfo);
            lineDataInfo.color = borderPaintInfo.getColor();
            lineDataInfo.y1 = lineDataInfo.y2 = Math.round(y1 + h3);
            this.dataStream.createLine(lineDataInfo);
            lineDataInfo.color = ColorUtil.lightenColor(
                    borderPaintInfo.getColor(), colFactor);
            lineDataInfo.y1 = lineDataInfo.y2 = Math.round(y1 + h3 + h3);
            this.dataStream.createLine(lineDataInfo);
            break;
        case Constants.EN_HIDDEN:
            break;
        case Constants.EN_INSET:
        case Constants.EN_OUTSET:
        case Constants.EN_SOLID:
        default:
            if (borderPaintInfo.isHorizontal()) {
                lineDataInfo.x2 = Math.round(x2);
                lineDataInfo.y2 = lineDataInfo.y1;
            } else {
                lineDataInfo.x2 = lineDataInfo.x1;
                lineDataInfo.y2 = Math.round(y2);
            }
            this.dataStream.createLine(lineDataInfo);
        }
    }

}
