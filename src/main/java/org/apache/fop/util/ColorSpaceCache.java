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

/* $Id: ColorSpaceCache.java 679326 2008-07-24 09:35:34Z vhennebert $ */

package org.apache.fop.util;

import java.awt.color.ColorSpace;
import java.awt.color.ICC_ColorSpace;
import java.awt.color.ICC_Profile;
import java.util.Collections;
import java.util.Map;

import javax.xml.transform.Source;
import javax.xml.transform.URIResolver;
import javax.xml.transform.stream.StreamSource;

import lombok.extern.slf4j.Slf4j;

/**
 * Map with cached ICC based ColorSpace objects.
 */
@Slf4j
public class ColorSpaceCache {

    private final URIResolver resolver;
    private final Map colorSpaceMap = Collections
            .synchronizedMap(new java.util.HashMap());

    /**
     * Default constructor
     *
     * @param resolver
     *            uri resolver
     */
    public ColorSpaceCache(final URIResolver resolver) {
        this.resolver = resolver;
    }

    /**
     * Create (if needed) and return an ICC ColorSpace instance.
     *
     * The ICC profile source is taken from the src attribute of the
     * color-profile FO element. If the ICC ColorSpace is not yet in the cache a
     * new one is created and stored in the cache.
     *
     * The FOP URI resolver is used to try and locate the ICC file. If that
     * fails null is returned.
     *
     * @param base
     *            a base URI to resolve relative URIs
     * @param iccProfileSrc
     *            ICC Profile source to return a ColorSpace for
     * @return ICC ColorSpace object or null if ColorSpace could not be created
     */
    public ColorSpace get(final String base, final String iccProfileSrc) {
        ColorSpace colorSpace = null;
        if (!this.colorSpaceMap.containsKey(base + iccProfileSrc)) {
            try {
                ICC_Profile iccProfile = null;
                // First attempt to use the FOP URI resolver to locate the ICC
                // profile
                final Source src = this.resolver.resolve(iccProfileSrc, base);
                if (src != null && src instanceof StreamSource) {
                    // FOP URI resolver found ICC profile - create ICC profile
                    // from the Source
                    iccProfile = ICC_Profile.getInstance(((StreamSource) src)
                            .getInputStream());
                } else {
                    // TODO - Would it make sense to fall back on VM ICC
                    // resolution
                    // Problem is the cache might be more difficult to maintain
                    //
                    // FOP URI resolver did not find ICC profile - perhaps the
                    // Java VM can find it?
                    // iccProfile = ICC_Profile.getInstance(iccProfileSrc);
                }
                if (iccProfile != null) {
                    colorSpace = new ICC_ColorSpace(iccProfile);
                }
            } catch (final Exception e) {
                // Ignore exception - will be logged a bit further down
                // (colorSpace == null case)
            }

            if (colorSpace != null) {
                // Put in cache (not when VM resolved it as we can't control
                this.colorSpaceMap.put(base + iccProfileSrc, colorSpace);
            } else {
                // TODO To avoid an excessive amount of warnings perhaps
                // register a null ColorMap in the colorSpaceMap
                log.warn("Color profile '" + iccProfileSrc + "' not found.");
            }
        } else {
            colorSpace = (ColorSpace) this.colorSpaceMap.get(base
                    + iccProfileSrc);
        }
        return colorSpace;
    }
}
