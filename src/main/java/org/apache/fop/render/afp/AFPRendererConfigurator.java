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

/* $Id: AFPRendererConfigurator.java 953952 2010-06-12 08:19:48Z jeremias $ */

package org.apache.fop.render.afp;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;

import lombok.extern.slf4j.Slf4j;

import org.apache.avalon.framework.configuration.Configuration;
import org.apache.avalon.framework.configuration.ConfigurationException;
import org.apache.fop.afp.AFPResourceLevel;
import org.apache.fop.afp.AFPResourceLevelDefaults;
import org.apache.fop.afp.fonts.AFPFont;
import org.apache.fop.afp.fonts.AFPFontCollection;
import org.apache.fop.afp.fonts.AFPFontInfo;
import org.apache.fop.afp.fonts.CharacterSet;
import org.apache.fop.afp.fonts.CharacterSetBuilder;
import org.apache.fop.afp.fonts.DoubleByteFont;
import org.apache.fop.afp.fonts.OutlineFont;
import org.apache.fop.afp.fonts.RasterFont;
import org.apache.fop.afp.util.DefaultFOPResourceAccessor;
import org.apache.fop.afp.util.ResourceAccessor;
import org.apache.fop.apps.FOPException;
import org.apache.fop.apps.FOUserAgent;
import org.apache.fop.fonts.EmbedFontInfo;
import org.apache.fop.fonts.FontCollection;
import org.apache.fop.fonts.FontInfo;
import org.apache.fop.fonts.FontManager;
import org.apache.fop.fonts.FontManagerConfigurator;
import org.apache.fop.fonts.FontTriplet;
import org.apache.fop.fonts.FontUtil;
import org.apache.fop.fonts.Typeface;
import org.apache.fop.render.PrintRendererConfigurator;
import org.apache.fop.render.Renderer;
import org.apache.fop.render.intermediate.IFDocumentHandler;
import org.apache.fop.util.LogUtil;

/**
 * AFP Renderer configurator
 */
@Slf4j
public class AFPRendererConfigurator extends PrintRendererConfigurator {

    /**
     * Default constructor
     *
     * @param userAgent
     *            user agent
     */
    public AFPRendererConfigurator(final FOUserAgent userAgent) {
        super(userAgent);
    }

    private AFPFontInfo buildFont(final Configuration fontCfg,
            final String fontPath) throws ConfigurationException {

        final FontManager fontManager = this.userAgent.getFactory()
                .getFontManager();

        final Configuration[] triple = fontCfg.getChildren("font-triplet");
        final List<FontTriplet> tripletList = new ArrayList<>();
        if (triple.length == 0) {
            log.error("Mandatory font configuration element '<font-triplet...' is missing");
            return null;
        }
        for (final Configuration element : triple) {
            final int weight = FontUtil.parseCSS2FontWeight(element
                    .getAttribute("weight"));
            final FontTriplet triplet = new FontTriplet(
                    element.getAttribute("name"),
                    element.getAttribute("style"), weight);
            tripletList.add(triplet);
        }

        // build the fonts
        final Configuration afpFontCfg = fontCfg.getChild("afp-font");
        if (afpFontCfg == null) {
            log.error("Mandatory font configuration element '<afp-font...' is missing");
            return null;
        }

        URI baseURI = null;
        final String uri = afpFontCfg.getAttribute("base-uri", fontPath);
        if (uri == null) {
            // Fallback for old attribute which only supports local filenames
            final String path = afpFontCfg.getAttribute("path", fontPath);
            if (path != null) {
                final File f = new File(path);
                baseURI = f.toURI();
            }
        } else {
            try {
                baseURI = new URI(uri);
            } catch (final URISyntaxException e) {
                log.error("Invalid URI: " + e.getMessage());
                return null;
            }
        }
        final ResourceAccessor accessor = new DefaultFOPResourceAccessor(
                this.userAgent, fontManager.getFontBaseURL(), baseURI);

        final String type = afpFontCfg.getAttribute("type");
        if (type == null) {
            log.error("Mandatory afp-font configuration attribute 'type=' is missing");
            return null;
        }
        final String codepage = afpFontCfg.getAttribute("codepage");
        if (codepage == null) {
            log.error("Mandatory afp-font configuration attribute 'code=' is missing");
            return null;
        }
        final String encoding = afpFontCfg.getAttribute("encoding");

        if (encoding == null) {
            log.error("Mandatory afp-font configuration attribute 'encoding=' is missing");
            return null;
        }

        final AFPFont font = fontFromType(type, codepage, encoding, accessor,
                afpFontCfg);

        return font != null ? new AFPFontInfo(font, tripletList) : null;
    }

    /**
     * Create the AFPFont based on type and type-dependent configuration.
     *
     * @param type
     *            font type e.g. 'raster', 'outline'
     * @param codepage
     *            codepage file
     * @param encoding
     *            character encoding e.g. 'Cp500', 'UnicodeBigUnmarked'
     * @param accessor
     * @param afpFontCfg
     * @return
     * @throws ConfigurationException
     */
    private AFPFont fontFromType(final String type, final String codepage,
            final String encoding, final ResourceAccessor accessor,
            final Configuration afpFontCfg) throws ConfigurationException {

        if ("raster".equalsIgnoreCase(type)) {

            final String name = afpFontCfg.getAttribute("name", "Unknown");

            // Create a new font object
            final RasterFont font = new RasterFont(name);

            final Configuration[] rasters = afpFontCfg
                    .getChildren("afp-raster-font");
            if (rasters.length == 0) {
                log.error("Mandatory font configuration elements '<afp-raster-font...'"
                        + " are missing at " + afpFontCfg.getLocation());
                return null;
            }
            for (final Configuration rasterCfg : rasters) {
                final String characterset = rasterCfg
                        .getAttribute("characterset");
                if (characterset == null) {
                    log.error("Mandatory afp-raster-font configuration attribute 'characterset=' is missing");
                    return null;
                }
                final float size = rasterCfg.getAttributeAsFloat("size");
                final int sizeMpt = (int) (size * 1000);
                final String base14 = rasterCfg.getAttribute("base14-font",
                        null);

                if (base14 != null) {
                    try {
                        final Class clazz = Class
                                .forName("org.apache.fop.fonts.base14."
                                        + base14);
                        try {
                            final Typeface tf = (Typeface) clazz.newInstance();
                            font.addCharacterSet(
                                    sizeMpt,
                                    CharacterSetBuilder.getInstance().build(
                                            characterset, codepage, encoding,
                                            tf));
                        } catch (final Exception ie) {
                            final String msg = "The base 14 font class "
                                    + clazz.getName()
                                    + " could not be instantiated";
                            log.error(msg);
                        }
                    } catch (final ClassNotFoundException cnfe) {
                        final String msg = "The base 14 font class for "
                                + characterset + " could not be found";
                        log.error(msg);
                    }
                } else {
                    try {
                        font.addCharacterSet(
                                sizeMpt,
                                CharacterSetBuilder.getInstance().build(
                                        characterset, codepage, encoding,
                                        accessor));
                    } catch (final IOException ioe) {
                        toConfigurationException(codepage, characterset, ioe);
                    }
                }
            }
            return font;

        } else if ("outline".equalsIgnoreCase(type)) {
            final String characterset = afpFontCfg.getAttribute("characterset");
            if (characterset == null) {
                log.error("Mandatory afp-font configuration attribute 'characterset=' is missing");
                return null;
            }
            final String name = afpFontCfg.getAttribute("name", characterset);
            CharacterSet characterSet = null;
            final String base14 = afpFontCfg.getAttribute("base14-font", null);
            if (base14 != null) {
                try {
                    final Class clazz = Class
                            .forName("org.apache.fop.fonts.base14." + base14);
                    try {
                        final Typeface tf = (Typeface) clazz.newInstance();
                        characterSet = CharacterSetBuilder.getInstance().build(
                                characterset, codepage, encoding, tf);
                    } catch (final Exception ie) {
                        final String msg = "The base 14 font class "
                                + clazz.getName()
                                + " could not be instantiated";
                        log.error(msg);
                    }
                } catch (final ClassNotFoundException cnfe) {
                    final String msg = "The base 14 font class for "
                            + characterset + " could not be found";
                    log.error(msg);
                }
            } else {
                try {
                    characterSet = CharacterSetBuilder.getInstance().build(
                            characterset, codepage, encoding, accessor);
                } catch (final IOException ioe) {
                    toConfigurationException(codepage, characterset, ioe);
                }
            }
            // Return new font object
            return new OutlineFont(name, characterSet);

        } else if ("CIDKeyed".equalsIgnoreCase(type)) {
            final String characterset = afpFontCfg.getAttribute("characterset");
            if (characterset == null) {
                log.error("Mandatory afp-font configuration attribute 'characterset=' is missing");
                return null;
            }
            final String name = afpFontCfg.getAttribute("name", characterset);
            CharacterSet characterSet = null;
            try {
                characterSet = CharacterSetBuilder.getDoubleByteInstance()
                        .build(characterset, codepage, encoding, accessor);
            } catch (final IOException ioe) {
                toConfigurationException(codepage, characterset, ioe);
            }

            // Create a new font object
            final DoubleByteFont font = new DoubleByteFont(name, characterSet);
            return font;

        } else {
            log.error("No or incorrect type attribute: " + type);
        }

        return null;
    }

    private void toConfigurationException(final String codepage,
            final String characterset, final IOException ioe)
                    throws ConfigurationException {
        final String msg = "Failed to load the character set metrics "
                + characterset + " with code page " + codepage
                + ". I/O error: " + ioe.getMessage();
        throw new ConfigurationException(msg, ioe);
    }

    /**
     * Builds a list of AFPFontInfo objects for use with the setup() method.
     *
     * @param cfg
     *            Configuration object
     * @return List the newly created list of fonts
     * @throws ConfigurationException
     *             if something's wrong with the config data
     */
    private List<EmbedFontInfo> buildFontListFromConfiguration(
            final Configuration cfg) throws FOPException,
            ConfigurationException {

        final Configuration fonts = cfg.getChild("fonts");
        final FontManager fontManager = this.userAgent.getFactory()
                .getFontManager();

        // General matcher
        final FontTriplet.Matcher referencedFontsMatcher = fontManager
                .getReferencedFontsMatcher();
        // Renderer-specific matcher
        FontTriplet.Matcher localMatcher = null;

        // Renderer-specific referenced fonts
        final Configuration referencedFontsCfg = fonts.getChild(
                "referenced-fonts", false);
        if (referencedFontsCfg != null) {
            localMatcher = FontManagerConfigurator.createFontsMatcher(
                    referencedFontsCfg, this.userAgent.getFactory()
                    .validateUserConfigStrictly());
        }

        final List fontList = new ArrayList<>();
        final Configuration[] font = fonts.getChildren("font");
        final String fontPath = null;
        for (final Configuration element : font) {
            final AFPFontInfo afi = buildFont(element, fontPath);
            if (afi != null) {
                log.debug("Adding font {}", afi.getAFPFont().getFontName());
                final List<FontTriplet> fontTriplets = afi.getFontTriplets();
                for (final FontTriplet triplet : fontTriplets) {
                    if (log.isDebugEnabled()) {
                        log.debug("  Font triplet " + triplet.getName() + ", "
                                + triplet.getStyle() + ", "
                                + triplet.getWeight());
                    }

                    if (referencedFontsMatcher != null
                            && referencedFontsMatcher.matches(triplet)
                            || localMatcher != null
                            && localMatcher.matches(triplet)) {
                        afi.getAFPFont().setEmbeddable(false);
                        break;
                    }
                }

                fontList.add(afi);
            }
        }
        return fontList;
    }

    /** images are converted to grayscale bitmapped IOCA */
    private static final String IMAGES_MODE_GRAYSCALE = "b+w";

    /** images are converted to color bitmapped IOCA */
    private static final String IMAGES_MODE_COLOR = "color";

    /**
     * Configure the AFP renderer.
     *
     * @param renderer
     *            AFP renderer
     * @throws FOPException
     *             fop exception
     * @see org.apache.fop.render.PrintRendererConfigurator#configure(Renderer)
     */
    @Override
    public void configure(final Renderer renderer) throws FOPException {
        final Configuration cfg = super.getRendererConfig(renderer);
        if (cfg != null) {
            final AFPRenderer afpRenderer = (AFPRenderer) renderer;

            try {
                final List<EmbedFontInfo> fontList = buildFontListFromConfiguration(cfg);
                afpRenderer.setFontList(fontList);
            } catch (final ConfigurationException e) {
                LogUtil.handleException(log, e, this.userAgent.getFactory()
                        .validateUserConfigStrictly());
            }

            configure(afpRenderer, cfg);
        }
    }

    private void configure(final AFPCustomizable customizable,
            final Configuration cfg) throws FOPException {

        // image information
        final Configuration imagesCfg = cfg.getChild("images");

        // default to grayscale images
        final String imagesMode = imagesCfg.getAttribute("mode",
                IMAGES_MODE_GRAYSCALE);
        if (IMAGES_MODE_COLOR.equals(imagesMode)) {
            customizable.setColorImages(true);

            final boolean cmyk = imagesCfg.getAttributeAsBoolean("cmyk", false);
            customizable.setCMYKImagesSupported(cmyk);
        } else {
            customizable.setColorImages(false);
            // default to 8 bits per pixel
            final int bitsPerPixel = imagesCfg.getAttributeAsInteger(
                    "bits-per-pixel", 8);
            customizable.setBitsPerPixel(bitsPerPixel);
        }

        final String dithering = imagesCfg.getAttribute("dithering-quality",
                "medium");
        float dq = 0.5f;
        if (dithering.startsWith("min")) {
            dq = 0.0f;
        } else if (dithering.startsWith("max")) {
            dq = 1.0f;
        } else {
            try {
                dq = Float.parseFloat(dithering);
            } catch (final NumberFormatException nfe) {
                // ignore and leave the default above
            }
        }
        customizable.setDitheringQuality(dq);

        // native image support
        final boolean nativeImageSupport = imagesCfg.getAttributeAsBoolean(
                "native", false);
        customizable.setNativeImagesSupported(nativeImageSupport);

        // shading (filled rectangles)
        final Configuration shadingCfg = cfg.getChild("shading");
        final AFPShadingMode shadingMode = AFPShadingMode.valueOf(shadingCfg
                .getValue(AFPShadingMode.COLOR.getName()));
        customizable.setShadingMode(shadingMode);

        // renderer resolution
        final Configuration rendererResolutionCfg = cfg.getChild(
                "renderer-resolution", false);
        if (rendererResolutionCfg != null) {
            customizable.setResolution(rendererResolutionCfg
                    .getValueAsInteger(240));
        }

        // a default external resource group file setting
        final Configuration resourceGroupFileCfg = cfg.getChild(
                "resource-group-file", false);
        if (resourceGroupFileCfg != null) {
            String resourceGroupDest = null;
            try {
                resourceGroupDest = resourceGroupFileCfg.getValue();
                if (resourceGroupDest != null) {
                    final File resourceGroupFile = new File(resourceGroupDest);
                    resourceGroupFile.createNewFile();
                    if (resourceGroupFile.canWrite()) {
                        customizable
                        .setDefaultResourceGroupFilePath(resourceGroupDest);
                    } else {
                        log.warn("Unable to write to default external resource group file '"
                                + resourceGroupDest + "'");
                    }
                }
            } catch (final ConfigurationException e) {
                LogUtil.handleException(log, e, this.userAgent.getFactory()
                        .validateUserConfigStrictly());
            } catch (final IOException ioe) {
                throw new FOPException(
                        "Could not create default external resource group file",
                        ioe);
            }
        }

        final Configuration defaultResourceLevelCfg = cfg.getChild(
                "default-resource-levels", false);
        if (defaultResourceLevelCfg != null) {
            final AFPResourceLevelDefaults defaults = new AFPResourceLevelDefaults();
            final String[] types = defaultResourceLevelCfg.getAttributeNames();
            for (final String type : types) {
                try {
                    final String level = defaultResourceLevelCfg
                            .getAttribute(type);
                    defaults.setDefaultResourceLevel(type,
                            AFPResourceLevel.valueOf(level));
                } catch (final IllegalArgumentException iae) {
                    LogUtil.handleException(log, iae, this.userAgent
                            .getFactory().validateUserConfigStrictly());
                } catch (final ConfigurationException e) {
                    LogUtil.handleException(log, e, this.userAgent.getFactory()
                            .validateUserConfigStrictly());
                }
            }
            customizable.setResourceLevelDefaults(defaults);
        }
    }

    /**
     * {@inheritDoc}
     *
     * @throws FOPException
     */
    @Override
    public void configure(final IFDocumentHandler documentHandler)
            throws FOPException {
        final Configuration cfg = super.getRendererConfig(documentHandler
                .getMimeType());
        if (cfg != null) {
            final AFPDocumentHandler afpDocumentHandler = (AFPDocumentHandler) documentHandler;
            configure(afpDocumentHandler, cfg);
        }
    }

    /** {@inheritDoc} */
    @Override
    public void setupFontInfo(final IFDocumentHandler documentHandler,
            final FontInfo fontInfo) throws FOPException {
        final FontManager fontManager = this.userAgent.getFactory()
                .getFontManager();
        final List<FontCollection> fontCollections = new ArrayList<>();

        final Configuration cfg = super.getRendererConfig(documentHandler
                .getMimeType());
        if (cfg != null) {
            try {
                final List fontList = buildFontListFromConfiguration(cfg);
                fontCollections.add(new AFPFontCollection(this.userAgent
                        .getEventBroadcaster(), fontList));
            } catch (final ConfigurationException e) {
                LogUtil.handleException(log, e, this.userAgent.getFactory()
                        .validateUserConfigStrictly());
            }
        } else {
            fontCollections.add(new AFPFontCollection(this.userAgent
                    .getEventBroadcaster(), null));
        }

        fontManager.setup(fontInfo, fontCollections
                .toArray(new FontCollection[fontCollections.size()]));
        documentHandler.setFontInfo(fontInfo);
    }
}
