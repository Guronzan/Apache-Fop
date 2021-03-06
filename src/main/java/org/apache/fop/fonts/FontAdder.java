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

/* $Id: FontAdder.java 815383 2009-09-15 16:15:11Z maxberger $ */

package org.apache.fop.fonts;

import java.net.URL;
import java.util.Iterator;
import java.util.List;

import org.apache.fop.fonts.autodetect.FontInfoFinder;

/**
 * Adds a list of fonts to a given font info list
 */
public class FontAdder {
    private final FontEventListener listener;
    private final FontResolver resolver;
    private final FontManager manager;

    /**
     * Main constructor
     * 
     * @param manager
     *            a font manager
     * @param resolver
     *            a font resolver
     * @param listener
     *            a font event handler
     */
    public FontAdder(final FontManager manager, final FontResolver resolver,
            final FontEventListener listener) {
        this.manager = manager;
        this.resolver = resolver;
        this.listener = listener;
    }

    /**
     * Iterates over font url list adding to font info list
     * 
     * @param fontURLList
     *            font file list
     * @param fontInfoList
     *            a configured font info list
     */
    public void add(final List/* <URL> */fontURLList, final List/*
                                                                 * <EmbedFontInfo
                                                                 * >
                                                                 */fontInfoList) {
        final FontCache cache = this.manager.getFontCache();
        final FontInfoFinder finder = new FontInfoFinder();
        finder.setEventListener(this.listener);

        for (final Iterator iter = fontURLList.iterator(); iter.hasNext();) {
            final URL fontUrl = (URL) iter.next();
            final EmbedFontInfo[] embedFontInfos = finder.find(fontUrl,
                    this.resolver, cache);
            if (embedFontInfos == null) {
                continue;
            }
            for (final EmbedFontInfo fontInfo : embedFontInfos) {
                if (fontInfo != null) {
                    fontInfoList.add(fontInfo);
                }
            }
        }
    }
}
