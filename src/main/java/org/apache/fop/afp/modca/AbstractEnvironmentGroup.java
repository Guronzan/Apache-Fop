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

/* $Id: AbstractEnvironmentGroup.java 815383 2009-09-15 16:15:11Z maxberger $ */

package org.apache.fop.afp.modca;

import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;

import lombok.extern.slf4j.Slf4j;

/**
 * A base class that encapsulates common features of ActiveEnvironmentGroup and
 * ResourceEnvironmentGroup
 */
@Slf4j
public abstract class AbstractEnvironmentGroup extends AbstractNamedAFPObject {

    /** the collection of MapDataResource objects */
    protected final List<MapDataResource> mapDataResources = null;

    /** the collection of MapPageOverlay objects */
    protected List<MapPageOverlay> mapPageOverlays = null;

    /**
     * Main constructor
     *
     * @param name
     *            the object name
     */
    public AbstractEnvironmentGroup(final String name) {
        super(name);
    }

    private List<MapPageOverlay> getMapPageOverlays() {
        if (this.mapPageOverlays == null) {
            this.mapPageOverlays = new ArrayList<>();
        }
        return this.mapPageOverlays;
    }

    /**
     * Actually creates the MPO object. Also creates the supporting object (an
     * IPO)
     *
     * @param name
     *            the name of the overlay to be used
     */
    public void createOverlay(final String name) {
        MapPageOverlay mpo = getCurrentMapPageOverlay();
        if (mpo == null) {
            mpo = new MapPageOverlay();
            getMapPageOverlays().add(mpo);
        }

        try {
            mpo.addOverlay(name);
        } catch (final MaximumSizeExceededException msee) {
            mpo = new MapPageOverlay();
            getMapPageOverlays().add(mpo);
            try {
                mpo.addOverlay(name);
            } catch (final MaximumSizeExceededException ex) {
                // Should never happen (but log just in case)
                log.error(
                        "createOverlay():: resulted in a MaximumSizeExceededException",
                        ex);
            }
        }
    }

    /**
     * Getter method for the most recent MapPageOverlay added to the Active
     * Environment Group (returns null if no MapPageOverlay exist)
     *
     * @return the most recent Map Coded Font
     */
    private MapPageOverlay getCurrentMapPageOverlay() {
        return getLastElement(this.mapPageOverlays);
    }

    protected <T> T getLastElement(final List<T> list) {
        if (list != null && !list.isEmpty()) {
            return list.get(list.size() - 1);
        } else {
            return null;
        }
    }

    /** {@inheritDoc} */
    @Override
    protected void writeContent(final OutputStream os) throws IOException {
        super.writeContent(os);
    }
}
