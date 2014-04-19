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

/* $Id: BasicLinkLayoutManager.java 830293 2009-10-27 19:07:52Z vhennebert $ */

package org.apache.fop.layoutmgr.inline;

import org.apache.fop.area.LinkResolver;
import org.apache.fop.area.Trait;
import org.apache.fop.area.inline.InlineArea;
import org.apache.fop.datatypes.URISpecification;
import org.apache.fop.fo.Constants;
import org.apache.fop.fo.flow.BasicLink;
import org.apache.fop.layoutmgr.PageSequenceLayoutManager;
import org.apache.fop.layoutmgr.TraitSetter;

/**
 * LayoutManager for the fo:basic-link formatting object
 */
public class BasicLinkLayoutManager extends InlineLayoutManager {

    /**
     * Create an fo:basic-link layout manager.
     *
     * @param node
     *            the formatting object that creates the area
     */
    public BasicLinkLayoutManager(final BasicLink node) {
        super(node);
    }

    /** {@inheritDoc} */
    @Override
    protected InlineArea createArea(final boolean bInlineParent) {
        final InlineArea area = super.createArea(bInlineParent);
        setupBasicLinkArea(area);
        return area;
    }

    /*
     * Detect internal or external link and add it as an area trait
     * 
     * @param area the basic-link's area
     */
    private void setupBasicLinkArea(final InlineArea area) {
        final BasicLink fobj = (BasicLink) this.fobj;
        // internal destinations take precedence:
        TraitSetter.addPtr(area, fobj.getPtr()); // used for accessibility
        if (fobj.hasInternalDestination()) {
            final String idref = fobj.getInternalDestination();
            final PageSequenceLayoutManager pslm = getPSLM();
            // the INTERNAL_LINK trait is added by the LinkResolver
            // if and when the link is resolved:
            final LinkResolver res = new LinkResolver(idref, area);
            res.resolveIDRef(idref, pslm.getFirstPVWithID(idref));
            if (!res.isResolved()) {
                pslm.addUnresolvedArea(idref, res);
            }
        } else if (fobj.hasExternalDestination()) {
            final String url = URISpecification.getURL(fobj
                    .getExternalDestination());
            final boolean newWindow = fobj.getShowDestination() == Constants.EN_NEW;
            if (url.length() > 0) {
                area.addTrait(Trait.EXTERNAL_LINK, new Trait.ExternalLink(url,
                        newWindow));
            }
        }
    }
}
