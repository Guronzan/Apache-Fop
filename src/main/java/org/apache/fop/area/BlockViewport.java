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

/* $Id: BlockViewport.java 679326 2008-07-24 09:35:34Z vhennebert $ */

package org.apache.fop.area;

/**
 * A BlockViewport. This is used for block level Viewport/reference pairs. The
 * block-container creates this area.
 */
public class BlockViewport extends Block {
    /**
     *
     */
    private static final long serialVersionUID = -7840580922580735157L;
    // clipping for this viewport
    private boolean clip = false;
    // transform if rotated or absolute
    private CTM viewportCTM;

    /**
     * Create a new block viewport area.
     */
    public BlockViewport() {
        this(false);
    }

    /**
     * Create a new block viewport area.
     * 
     * @param allowBPDUpdate
     *            true allows the BPD to be updated when children are added
     */
    public BlockViewport(final boolean allowBPDUpdate) {
        this.allowBPDUpdate = allowBPDUpdate;
    }

    /**
     * Set the transform of this viewport. If the viewport is rotated or has an
     * absolute positioning this transform will do the work.
     *
     * @param ctm
     *            the transformation
     */
    public void setCTM(final CTM ctm) {
        this.viewportCTM = ctm;
    }

    /**
     * Get the transform of this block viewport.
     *
     * @return the transformation of this viewport or null if normally stacked
     *         without rotation
     */
    public CTM getCTM() {
        return this.viewportCTM;
    }

    /**
     * Set the clipping for this viewport.
     *
     * @param cl
     *            the clipping for the viewport
     */
    public void setClip(final boolean cl) {
        this.clip = cl;
    }

    /**
     * Get the clipping for this viewport.
     *
     * @return the clipping for the viewport true if the contents should be
     *         clipped for this viewport
     */
    public boolean getClip() {
        return this.clip;
    }
}
