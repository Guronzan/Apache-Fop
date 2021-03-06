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

/* $Id: LayoutMasterSet.java 679326 2008-07-24 09:35:34Z vhennebert $ */

package org.apache.fop.fo.pagination;

// Java
import java.util.HashMap;
import java.util.Map;

import org.apache.fop.apps.FOPException;
import org.apache.fop.fo.FONode;
import org.apache.fop.fo.FObj;
import org.apache.fop.fo.PropertyList;
import org.apache.fop.fo.ValidationException;
import org.xml.sax.Locator;

/**
 * Class modelling the <a href="http://www.w3.org/TR/xsl/#fo_layout-master-set">
 * <code>fo:layout-master-set</code></a> object.
 *
 * This class maintains the set of simple page master and page sequence masters.
 * The masters are stored so that the page sequence can obtain the required page
 * master to create a page. The page sequence masters can be reset as they hold
 * state information for a page sequence.
 */
public class LayoutMasterSet extends FObj {

    private Map<String, SimplePageMaster> simplePageMasters;
    private Map<String, PageSequenceMaster> pageSequenceMasters;

    /**
     * Create a LayoutMasterSet instance that is a child of the given parent
     * {@link FONode}.
     *
     * @param parent
     *            {@link FONode} that is the parent of this object
     */
    public LayoutMasterSet(final FONode parent) {
        super(parent);
    }

    /** {@inheritDoc} */
    @Override
    public void bind(final PropertyList pList) {
        // No properties in layout-master-set.
    }

    /** {@inheritDoc} */
    @Override
    protected void startOfNode() {
        getRoot().setLayoutMasterSet(this);
        this.simplePageMasters = new HashMap<>();
        this.pageSequenceMasters = new HashMap<>();
    }

    /** {@inheritDoc} */
    @Override
    protected void endOfNode() throws FOPException {
        if (this.firstChild == null) {
            missingChildElementError("(simple-page-master|page-sequence-master)+");
        }
        checkRegionNames();
    }

    /**
     * {@inheritDoc} <br>
     * XSL/FOP: (simple-page-master|page-sequence-master)+
     */
    @Override
    protected void validateChildNode(final Locator loc, final String nsURI,
            final String localName) throws ValidationException {
        if (FO_URI.equals(nsURI)) {
            if (!localName.equals("simple-page-master")
                    && !localName.equals("page-sequence-master")) {
                invalidChildError(loc, nsURI, localName);
            }
        }
    }

    /**
     * Section 7.25.7: check to see that if a region-name is a duplicate, that
     * it maps to the same fo region-class.
     *
     * @throws ValidationException
     *             if there's a name duplication
     */
    private void checkRegionNames() throws ValidationException {
        // (user-entered) region-name to default region map.
        final Map<String, String> allRegions = new HashMap<>();
        for (final SimplePageMaster simplePageMaster : this.simplePageMasters
                .values()) {
            final Map<String, Region> spmRegions = simplePageMaster
                    .getRegions();
            for (final Region region : spmRegions.values()) {
                if (allRegions.containsKey(region.getRegionName())) {
                    final String defaultRegionName = allRegions.get(region
                            .getRegionName());
                    if (!defaultRegionName
                            .equals(region.getDefaultRegionName())) {
                        getFOValidationEventProducer()
                                .regionNameMappedToMultipleRegionClasses(this,
                                        region.getRegionName(),
                                        defaultRegionName,
                                        region.getDefaultRegionName(),
                                        getLocator());
                    }
                }
                allRegions.put(region.getRegionName(),
                        region.getDefaultRegionName());
            }
        }
    }

    /**
     * Add a simple page master. The name is checked to throw an error if
     * already added.
     *
     * @param sPM
     *            simple-page-master to add
     * @throws ValidationException
     *             if there's a problem with name uniqueness
     */
    protected void addSimplePageMaster(final SimplePageMaster sPM)
            throws ValidationException {

        // check for duplication of master-name
        final String masterName = sPM.getMasterName();
        if (existsName(masterName)) {
            getFOValidationEventProducer().masterNameNotUnique(this, getName(),
                    masterName, sPM.getLocator());
        }
        this.simplePageMasters.put(masterName, sPM);
    }

    private boolean existsName(final String masterName) {
        return this.simplePageMasters.containsKey(masterName)
                || this.pageSequenceMasters.containsKey(masterName);
    }

    /**
     * Get a simple page master by name. This is used by the page sequence to
     * get a page master for creating pages.
     *
     * @param masterName
     *            the name of the page master
     * @return the requested simple-page-master
     */
    public SimplePageMaster getSimplePageMaster(final String masterName) {
        return this.simplePageMasters.get(masterName);
    }

    /**
     * Add a page sequence master. The name is checked to throw an error if
     * already added.
     *
     * @param masterName
     *            name for the master
     * @param pSM
     *            PageSequenceMaster instance
     * @throws ValidationException
     *             if there's a problem with name uniqueness
     */
    protected void addPageSequenceMaster(final String masterName,
            final PageSequenceMaster pSM) throws ValidationException {
        // check against duplication of master-name
        if (existsName(masterName)) {
            getFOValidationEventProducer().masterNameNotUnique(this, getName(),
                    masterName, pSM.getLocator());
        }
        this.pageSequenceMasters.put(masterName, pSM);
    }

    /**
     * Get a page sequence master by name. This is used by the page sequence to
     * get a page master for creating pages.
     *
     * @param masterName
     *            name of the master
     * @return the requested PageSequenceMaster instance
     */
    public PageSequenceMaster getPageSequenceMaster(final String masterName) {
        return this.pageSequenceMasters.get(masterName);
    }

    /**
     * Checks whether or not a region name exists in this master set.
     *
     * @param regionName
     *            name of the region
     * @return true when the region name specified has a region in this
     *         LayoutMasterSet
     */
    public boolean regionNameExists(final String regionName) {
        for (final SimplePageMaster simplePageMaster : this.simplePageMasters
                .values()) {
            if (simplePageMaster.regionNameExists(regionName)) {
                return true;
            }
        }
        return false;
    }

    /** {@inheritDoc} */
    @Override
    public String getLocalName() {
        return "layout-master-set";
    }

    /**
     * {@inheritDoc}
     *
     * @return {@link org.apache.fop.fo.Constants#FO_LAYOUT_MASTER_SET}
     */
    @Override
    public int getNameId() {
        return FO_LAYOUT_MASTER_SET;
    }
}
