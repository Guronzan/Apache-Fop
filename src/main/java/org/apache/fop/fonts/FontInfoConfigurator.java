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

/* $Id: FontInfoConfigurator.java 821058 2009-10-02 15:31:14Z jeremias $ */

package org.apache.fop.fonts;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.List;

import javax.xml.transform.Source;
import javax.xml.transform.stream.StreamSource;

import lombok.extern.slf4j.Slf4j;

import org.apache.avalon.framework.configuration.Configuration;
import org.apache.avalon.framework.configuration.ConfigurationException;
import org.apache.commons.io.IOUtils;
import org.apache.fop.apps.FOPException;
import org.apache.fop.fonts.autodetect.FontFileFinder;
import org.apache.fop.fonts.autodetect.FontInfoFinder;
import org.apache.fop.util.LogUtil;

/**
 * An abstract FontInfo configurator
 */
@Slf4j
public class FontInfoConfigurator {

    private final Configuration cfg;
    private final FontManager fontManager;
    private final FontResolver fontResolver;
    private final FontEventListener listener;
    private final boolean strict;

    /**
     * Main constructor
     *
     * @param cfg
     *            the configuration object
     * @param fontManager
     *            the font manager
     * @param fontResolver
     *            the font resolver
     * @param listener
     *            the font event listener
     * @param strict
     *            true if an Exception should be thrown if an error is found.
     */
    public FontInfoConfigurator(final Configuration cfg,
            final FontManager fontManager, final FontResolver fontResolver,
            final FontEventListener listener, final boolean strict) {
        this.cfg = cfg;
        this.fontManager = fontManager;
        this.fontResolver = fontResolver;
        this.listener = listener;
        this.strict = strict;
    }

    /**
     * Initializes font info settings from the user configuration
     *
     * @param fontInfoList
     *            a font info list
     * @throws FOPException
     *             if an exception occurs while processing the configuration
     */
    public void configure(final List/* <EmbedFontInfo> */fontInfoList)
            throws FOPException {
        final Configuration fonts = this.cfg.getChild("fonts", false);
        if (fonts != null) {
            long start = 0;
            if (log.isDebugEnabled()) {
                log.debug("Starting font configuration...");
                start = System.currentTimeMillis();
            }

            final FontAdder fontAdder = new FontAdder(this.fontManager,
                    this.fontResolver, this.listener);

            // native o/s search (autodetect) configuration
            final boolean autodetectFonts = fonts
                    .getChild("auto-detect", false) != null;
            if (autodetectFonts) {
                final FontDetector fontDetector = new FontDetector(
                        this.fontManager, fontAdder, this.strict);
                fontDetector.detect(fontInfoList);
            }

            // Add configured directories to FontInfo
            addDirectories(fonts, fontAdder, fontInfoList);

            // Add configured fonts to FontInfo
            final FontCache fontCache = this.fontManager.getFontCache();
            addFonts(fonts, fontCache, fontInfoList);

            // Update referenced fonts (fonts which are not to be embedded)
            this.fontManager.updateReferencedFonts(fontInfoList);

            // Renderer-specific referenced fonts
            final Configuration referencedFontsCfg = fonts.getChild(
                    "referenced-fonts", false);
            if (referencedFontsCfg != null) {
                final FontTriplet.Matcher matcher = FontManagerConfigurator
                        .createFontsMatcher(referencedFontsCfg, this.strict);
                this.fontManager.updateReferencedFonts(fontInfoList, matcher);
            }

            // Update font cache if it has changed
            if (fontCache != null && fontCache.hasChanged()) {
                fontCache.save();
            }

            if (log.isDebugEnabled()) {
                log.debug("Finished font configuration in "
                        + (System.currentTimeMillis() - start) + "ms");
            }
        }
    }

    private void addDirectories(final Configuration fontsCfg,
            final FontAdder fontAdder, final List/* <URL> */fontInfoList)
                    throws FOPException {
        // directory (multiple font) configuration
        final Configuration[] directories = fontsCfg.getChildren("directory");
        for (final Configuration directorie : directories) {
            final boolean recursive = directorie.getAttributeAsBoolean(
                    "recursive", false);
            String directory = null;
            try {
                directory = directorie.getValue();
            } catch (final ConfigurationException e) {
                LogUtil.handleException(log, e, this.strict);
                continue;
            }
            if (directory == null) {
                LogUtil.handleException(log, new FOPException(
                        "directory defined without value"), this.strict);
                continue;
            }

            // add fonts found in directory
            final FontFileFinder fontFileFinder = new FontFileFinder(
                    recursive ? -1 : 1);
            List/* <URL> */fontURLList;
            try {
                fontURLList = fontFileFinder.find(directory);
                fontAdder.add(fontURLList, fontInfoList);
            } catch (final IOException e) {
                LogUtil.handleException(log, e, this.strict);
            }
        }
    }

    /**
     * Populates the font info list from the fonts configuration
     *
     * @param fontsCfg
     *            a fonts configuration
     * @param fontCache
     *            a font cache
     * @param fontInfoList
     *            a font info list
     * @throws FOPException
     *             if an exception occurs while processing the configuration
     */
    protected void addFonts(final Configuration fontsCfg,
            final FontCache fontCache,
            final List/* <EmbedFontInfo> */fontInfoList) throws FOPException {
        // font file (singular) configuration
        final Configuration[] font = fontsCfg.getChildren("font");
        for (final Configuration element : font) {
            final EmbedFontInfo embedFontInfo = getFontInfo(element, fontCache);
            if (embedFontInfo != null) {
                fontInfoList.add(embedFontInfo);
            }
        }
    }

    private static void closeSource(final Source src) {
        if (src instanceof StreamSource) {
            final StreamSource streamSource = (StreamSource) src;
            IOUtils.closeQuietly(streamSource.getInputStream());
            IOUtils.closeQuietly(streamSource.getReader());
        }
    }

    /**
     * Returns a font info from a font node Configuration definition
     *
     * @param fontCfg
     *            Configuration object (font node)
     * @param fontCache
     *            the font cache (or null if it is disabled)
     * @return the embedded font info
     * @throws FOPException
     *             if something's wrong with the config data
     */
    protected EmbedFontInfo getFontInfo(final Configuration fontCfg,
            final FontCache fontCache) throws FOPException {
        final String metricsUrl = fontCfg.getAttribute("metrics-url", null);
        final String embedUrl = fontCfg.getAttribute("embed-url", null);
        final String subFont = fontCfg.getAttribute("sub-font", null);

        if (metricsUrl == null && embedUrl == null) {
            LogUtil.handleError(
                    log,
                    "Font configuration without metric-url or embed-url attribute",
                    this.strict);
            return null;
        }
        if (this.strict) {
            // This section just checks early whether the URIs can be resolved
            // Stream are immediately closed again since they will never be used
            // anyway
            if (embedUrl != null) {
                final Source source = this.fontResolver.resolve(embedUrl);
                closeSource(source);
                if (source == null) {
                    LogUtil.handleError(log,
                            "Failed to resolve font with embed-url '"
                                    + embedUrl + "'", this.strict);
                    return null;
                }
            }
            if (metricsUrl != null) {
                final Source source = this.fontResolver.resolve(metricsUrl);
                closeSource(source);
                if (source == null) {
                    LogUtil.handleError(log,
                            "Failed to resolve font with metric-url '"
                                    + metricsUrl + "'", this.strict);
                    return null;
                }
            }
        }

        final Configuration[] tripletCfg = fontCfg.getChildren("font-triplet");

        // no font triplet info
        if (tripletCfg.length == 0) {
            LogUtil.handleError(log, "font without font-triplet", this.strict);

            final File fontFile = FontCache.getFileFromUrls(new String[] {
                    embedUrl, metricsUrl });
            URL fontUrl;
            try {
                fontUrl = fontFile.toURI().toURL();
            } catch (final MalformedURLException e) {
                // Should never happen
                log.debug("Malformed Url: " + e.getMessage());
                return null;
            }
            if (fontFile != null) {
                final FontInfoFinder finder = new FontInfoFinder();
                finder.setEventListener(this.listener);
                final EmbedFontInfo[] infos = finder.find(fontUrl,
                        this.fontResolver, fontCache);
                return infos[0]; // When subFont is set, only one font is
                // returned
            } else {
                return null;
            }
        }

        final List/* <FontTriplet> */tripletList = new java.util.ArrayList/*
         * <
         * FontTriplet
         * >
         */();
        for (final Configuration element : tripletCfg) {
            final FontTriplet fontTriplet = getFontTriplet(element);
            tripletList.add(fontTriplet);
        }

        final boolean useKerning = fontCfg.getAttributeAsBoolean("kerning",
                true);
        final EncodingMode encodingMode = EncodingMode.valueOf(fontCfg
                .getAttribute("encoding-mode", EncodingMode.AUTO.getName()));
        final EmbedFontInfo embedFontInfo = new EmbedFontInfo(metricsUrl,
                useKerning, tripletList, embedUrl, subFont);
        embedFontInfo.setEncodingMode(encodingMode);
        if (fontCache != null) {
            if (!fontCache.containsFont(embedFontInfo)) {
                fontCache.addFont(embedFontInfo);
            }
        }

        if (log.isDebugEnabled()) {
            final String embedFile = embedFontInfo.getEmbedFile();
            log.debug("Adding font "
                    + (embedFile != null ? embedFile + ", " : "")
                    + "metric file " + embedFontInfo.getMetricsFile());
            for (int j = 0; j < tripletList.size(); ++j) {
                final FontTriplet triplet = (FontTriplet) tripletList.get(j);
                log.debug("  Font triplet " + triplet.getName() + ", "
                        + triplet.getStyle() + ", " + triplet.getWeight());
            }
        }
        return embedFontInfo;
    }

    /**
     * Creates a new FontTriplet given a triple Configuration
     *
     * @param tripletCfg
     *            a triplet configuration
     * @return a font triplet font key
     * @throws FOPException
     *             thrown if a FOP exception occurs
     */
    private FontTriplet getFontTriplet(final Configuration tripletCfg)
            throws FOPException {
        try {
            final String name = tripletCfg.getAttribute("name");
            if (name == null) {
                LogUtil.handleError(log, "font-triplet without name",
                        this.strict);
                return null;
            }

            final String weightStr = tripletCfg.getAttribute("weight");
            if (weightStr == null) {
                LogUtil.handleError(log, "font-triplet without weight",
                        this.strict);
                return null;
            }
            final int weight = FontUtil.parseCSS2FontWeight(FontUtil
                    .stripWhiteSpace(weightStr));

            String style = tripletCfg.getAttribute("style");
            if (style == null) {
                LogUtil.handleError(log, "font-triplet without style",
                        this.strict);
                return null;
            } else {
                style = FontUtil.stripWhiteSpace(style);
            }
            return FontInfo.createFontKey(name, style, weight);
        } catch (final ConfigurationException e) {
            LogUtil.handleException(log, e, this.strict);
        }
        return null;
    }

}
