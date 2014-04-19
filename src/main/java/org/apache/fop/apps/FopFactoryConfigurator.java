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

/* $Id: FopFactoryConfigurator.java 924860 2010-03-18 15:24:25Z jeremias $ */

package org.apache.fop.apps;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;

import lombok.extern.slf4j.Slf4j;

import org.apache.avalon.framework.configuration.Configuration;
import org.apache.avalon.framework.configuration.ConfigurationException;
import org.apache.avalon.framework.configuration.DefaultConfigurationBuilder;
import org.apache.fop.fonts.FontManager;
import org.apache.fop.fonts.FontManagerConfigurator;
import org.apache.fop.util.LogUtil;
import org.apache.xmlgraphics.image.loader.spi.ImageImplRegistry;
import org.apache.xmlgraphics.image.loader.util.Penalty;
import org.xml.sax.SAXException;

/**
 * FopFactory configurator
 */
@Slf4j
public class FopFactoryConfigurator {

    /** Defines if FOP should use an alternative rule to determine text indents */
    public static final boolean DEFAULT_BREAK_INDENT_INHERITANCE = false;

    /** Defines if FOP should validate the user config strictly */
    public static final boolean DEFAULT_STRICT_USERCONFIG_VALIDATION = true;

    /** Defines if FOP should use strict validation for FO and user config */
    public static final boolean DEFAULT_STRICT_FO_VALIDATION = true;

    /** Defines the default page-width */
    public static final String DEFAULT_PAGE_WIDTH = "8.26in";

    /** Defines the default page-height */
    public static final String DEFAULT_PAGE_HEIGHT = "11in";

    /** Defines the default source resolution (72dpi) for FOP */
    public static final float DEFAULT_SOURCE_RESOLUTION = 72.0f; // dpi

    /** Defines the default target resolution (72dpi) for FOP */
    public static final float DEFAULT_TARGET_RESOLUTION = 72.0f; // dpi

    private static final String PREFER_RENDERER = "prefer-renderer";

    /** Fop factory */
    private FopFactory factory = null;

    /** Fop factory configuration */
    private Configuration cfg = null;

    /**
     * Default constructor
     *
     * @param factory
     *            fop factory
     */
    public FopFactoryConfigurator(final FopFactory factory) {
        super();
        this.factory = factory;
    }

    /**
     * Initializes user agent settings from the user configuration file, if
     * present: baseURL, resolution, default page size,...
     *
     * @param factory
     *            fop factory
     * @throws FOPException
     *             fop exception
     */
    public void configure(final FopFactory factory) throws FOPException {
        if (log.isDebugEnabled()) {
            log.debug("Initializing FopFactory Configuration");
        }

        if (this.cfg.getChild("accessibility", false) != null) {
            try {
                this.factory.setAccessibility(this.cfg
                        .getChild("accessibility").getValueAsBoolean());
            } catch (final ConfigurationException e) {
                throw new FOPException(e);
            }
        }

        // strict configuration
        if (this.cfg.getChild("strict-configuration", false) != null) {
            try {
                factory.setStrictUserConfigValidation(this.cfg.getChild(
                        "strict-configuration").getValueAsBoolean());
            } catch (final ConfigurationException e) {
                LogUtil.handleException(log, e, false);
            }
        }
        final boolean strict = factory.validateUserConfigStrictly();

        // strict fo validation
        if (this.cfg.getChild("strict-validation", false) != null) {
            try {
                factory.setStrictValidation(this.cfg.getChild(
                        "strict-validation").getValueAsBoolean());
            } catch (final ConfigurationException e) {
                LogUtil.handleException(log, e, strict);
            }
        }

        // base definitions for relative path resolution
        if (this.cfg.getChild("base", false) != null) {
            try {
                factory.setBaseURL(this.cfg.getChild("base").getValue(null));
            } catch (final MalformedURLException mfue) {
                LogUtil.handleException(log, mfue, strict);
            }
        }
        if (this.cfg.getChild("hyphenation-base", false) != null) {
            try {
                factory.setHyphenBaseURL(this.cfg.getChild("hyphenation-base")
                        .getValue(null));
            } catch (final MalformedURLException mfue) {
                LogUtil.handleException(log, mfue, strict);
            }
        }

        // renderer options
        if (this.cfg.getChild("source-resolution", false) != null) {
            factory.setSourceResolution(this.cfg.getChild("source-resolution")
                    .getValueAsFloat(
                            FopFactoryConfigurator.DEFAULT_SOURCE_RESOLUTION));
            if (log.isDebugEnabled()) {
                log.debug("source-resolution set to: "
                        + factory.getSourceResolution() + "dpi (px2mm="
                        + factory.getSourcePixelUnitToMillimeter() + ")");
            }
        }
        if (this.cfg.getChild("target-resolution", false) != null) {
            factory.setTargetResolution(this.cfg.getChild("target-resolution")
                    .getValueAsFloat(
                            FopFactoryConfigurator.DEFAULT_TARGET_RESOLUTION));
            if (log.isDebugEnabled()) {
                log.debug("target-resolution set to: "
                        + factory.getTargetResolution() + "dpi (px2mm="
                        + factory.getTargetPixelUnitToMillimeter() + ")");
            }
        }
        if (this.cfg.getChild("break-indent-inheritance", false) != null) {
            try {
                factory.setBreakIndentInheritanceOnReferenceAreaBoundary(this.cfg
                        .getChild("break-indent-inheritance")
                        .getValueAsBoolean());
            } catch (final ConfigurationException e) {
                LogUtil.handleException(log, e, strict);
            }
        }
        final Configuration pageConfig = this.cfg
                .getChild("default-page-settings");
        if (pageConfig.getAttribute("height", null) != null) {
            factory.setPageHeight(pageConfig.getAttribute("height",
                    FopFactoryConfigurator.DEFAULT_PAGE_HEIGHT));
            if (log.isInfoEnabled()) {
                log.info("Default page-height set to: "
                        + factory.getPageHeight());
            }
        }
        if (pageConfig.getAttribute("width", null) != null) {
            factory.setPageWidth(pageConfig.getAttribute("width",
                    FopFactoryConfigurator.DEFAULT_PAGE_WIDTH));
            if (log.isInfoEnabled()) {
                log.info("Default page-width set to: " + factory.getPageWidth());
            }
        }

        // prefer Renderer over IFDocumentHandler
        if (this.cfg.getChild(PREFER_RENDERER, false) != null) {
            try {
                factory.getRendererFactory().setRendererPreferred(
                        this.cfg.getChild(PREFER_RENDERER).getValueAsBoolean());
            } catch (final ConfigurationException e) {
                LogUtil.handleException(log, e, strict);
            }
        }

        // configure font manager
        final FontManager fontManager = factory.getFontManager();
        final FontManagerConfigurator fontManagerConfigurator = new FontManagerConfigurator(
                this.cfg);
        fontManagerConfigurator.configure(fontManager, strict);

        // configure image loader framework
        configureImageLoading(this.cfg.getChild("image-loading", false), strict);
    }

    private void configureImageLoading(final Configuration parent,
            final boolean strict) throws FOPException {
        if (parent == null) {
            return;
        }
        final ImageImplRegistry registry = this.factory.getImageManager()
                .getRegistry();
        final Configuration[] penalties = parent.getChildren("penalty");
        try {
            for (final Configuration penaltyCfg : penalties) {
                final String className = penaltyCfg.getAttribute("class");
                final String value = penaltyCfg.getAttribute("value");
                Penalty p = null;
                if (value.toUpperCase().startsWith("INF")) {
                    p = Penalty.INFINITE_PENALTY;
                } else {
                    try {
                        p = Penalty.toPenalty(Integer.parseInt(value));
                    } catch (final NumberFormatException nfe) {
                        LogUtil.handleException(log, nfe, strict);
                    }
                }
                if (p != null) {
                    registry.setAdditionalPenalty(className, p);
                }
            }
        } catch (final ConfigurationException e) {
            LogUtil.handleException(log, e, strict);
        }
    }

    /**
     * Set the user configuration.
     *
     * @param userConfigFile
     *            the configuration file
     * @throws IOException
     *             if an I/O error occurs
     * @throws SAXException
     *             if a parsing error occurs
     */
    public void setUserConfig(final File userConfigFile) throws SAXException,
    IOException {
        try {
            final DefaultConfigurationBuilder cfgBuilder = new DefaultConfigurationBuilder();
            setUserConfig(cfgBuilder.buildFromFile(userConfigFile));
        } catch (final ConfigurationException e) {
            throw new FOPException(e);
        }
    }

    /**
     * Set the user configuration from an URI.
     *
     * @param uri
     *            the URI to the configuration file
     * @throws IOException
     *             if an I/O error occurs
     * @throws SAXException
     *             if a parsing error occurs
     */
    public void setUserConfig(final String uri) throws SAXException,
    IOException {
        try {
            final DefaultConfigurationBuilder cfgBuilder = new DefaultConfigurationBuilder();
            setUserConfig(cfgBuilder.build(uri));
        } catch (final ConfigurationException e) {
            throw new FOPException(e);
        }
    }

    /**
     * Set the user configuration.
     *
     * @param cfg
     *            avalon configuration
     * @throws FOPException
     *             if a configuration problem occurs
     */
    public void setUserConfig(final Configuration cfg) throws FOPException {
        this.cfg = cfg;
        configure(this.factory);
    }

    /**
     * Get the avalon user configuration.
     *
     * @return the user configuration
     */
    public Configuration getUserConfig() {
        return this.cfg;
    }
}
