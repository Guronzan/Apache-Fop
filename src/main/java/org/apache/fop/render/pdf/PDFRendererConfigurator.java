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

/* $Id: PDFRendererConfigurator.java 746664 2009-02-22 12:40:44Z jeremias $ */

package org.apache.fop.render.pdf;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import lombok.extern.slf4j.Slf4j;

import org.apache.avalon.framework.configuration.Configuration;
import org.apache.avalon.framework.configuration.ConfigurationException;
import org.apache.fop.apps.FOPException;
import org.apache.fop.apps.FOUserAgent;
import org.apache.fop.pdf.PDFAMode;
import org.apache.fop.pdf.PDFEncryptionParams;
import org.apache.fop.pdf.PDFFilterList;
import org.apache.fop.pdf.PDFXMode;
import org.apache.fop.render.PrintRendererConfigurator;
import org.apache.fop.render.Renderer;
import org.apache.fop.render.intermediate.IFDocumentHandler;
import org.apache.fop.util.LogUtil;

/**
 * PDF renderer configurator.
 */
@Slf4j
public class PDFRendererConfigurator extends PrintRendererConfigurator {

    /**
     * Default constructor
     *
     * @param userAgent
     *            user agent
     */
    public PDFRendererConfigurator(final FOUserAgent userAgent) {
        super(userAgent);
    }

    /**
     * Configure the PDF renderer. Get the configuration to be used for pdf
     * stream filters, fonts etc.
     *
     * @param renderer
     *            pdf renderer
     * @throws FOPException
     *             fop exception
     */
    @Override
    public void configure(final Renderer renderer) throws FOPException {
        final Configuration cfg = super.getRendererConfig(renderer);
        if (cfg != null) {
            final PDFRenderer pdfRenderer = (PDFRenderer) renderer;
            super.configure(renderer);

            final PDFRenderingUtil pdfUtil = pdfRenderer.getPDFUtil();
            configure(cfg, pdfUtil);
        }
    }

    private void configure(final Configuration cfg,
            final PDFRenderingUtil pdfUtil) throws FOPException {
        // PDF filters
        try {
            final Map<String, List<String>> filterMap = buildFilterMapFromConfiguration(cfg);
            if (filterMap != null) {
                pdfUtil.setFilterMap(filterMap);
            }
        } catch (final ConfigurationException e) {
            LogUtil.handleException(log, e, false);
        }

        String s = cfg.getChild(PDFConfigurationConstants.PDF_A_MODE, true)
                .getValue(null);
        if (s != null) {
            pdfUtil.setAMode(PDFAMode.valueOf(s));
        }
        s = cfg.getChild(PDFConfigurationConstants.PDF_X_MODE, true).getValue(
                null);
        if (s != null) {
            pdfUtil.setXMode(PDFXMode.valueOf(s));
        }
        final Configuration encryptionParamsConfig = cfg.getChild(
                PDFConfigurationConstants.ENCRYPTION_PARAMS, false);
        if (encryptionParamsConfig != null) {
            final PDFEncryptionParams encryptionParams = new PDFEncryptionParams();
            final Configuration ownerPasswordConfig = encryptionParamsConfig
                    .getChild(PDFConfigurationConstants.OWNER_PASSWORD, false);
            if (ownerPasswordConfig != null) {
                final String ownerPassword = ownerPasswordConfig.getValue(null);
                if (ownerPassword != null) {
                    encryptionParams.setOwnerPassword(ownerPassword);
                }
            }
            final Configuration userPasswordConfig = encryptionParamsConfig
                    .getChild(PDFConfigurationConstants.USER_PASSWORD, false);
            if (userPasswordConfig != null) {
                final String userPassword = userPasswordConfig.getValue(null);
                if (userPassword != null) {
                    encryptionParams.setUserPassword(userPassword);
                }
            }
            final Configuration noPrintConfig = encryptionParamsConfig
                    .getChild(PDFConfigurationConstants.NO_PRINT, false);
            if (noPrintConfig != null) {
                encryptionParams.setAllowPrint(false);
            }
            final Configuration noCopyContentConfig = encryptionParamsConfig
                    .getChild(PDFConfigurationConstants.NO_COPY_CONTENT, false);
            if (noCopyContentConfig != null) {
                encryptionParams.setAllowCopyContent(false);
            }
            final Configuration noEditContentConfig = encryptionParamsConfig
                    .getChild(PDFConfigurationConstants.NO_EDIT_CONTENT, false);
            if (noEditContentConfig != null) {
                encryptionParams.setAllowEditContent(false);
            }
            final Configuration noAnnotationsConfig = encryptionParamsConfig
                    .getChild(PDFConfigurationConstants.NO_ANNOTATIONS, false);
            if (noAnnotationsConfig != null) {
                encryptionParams.setAllowEditAnnotations(false);
            }
            pdfUtil.setEncryptionParams(encryptionParams);
        }
        s = cfg.getChild(PDFConfigurationConstants.KEY_OUTPUT_PROFILE, true)
                .getValue(null);
        if (s != null) {
            pdfUtil.setOutputProfileURI(s);
        }
        final Configuration disableColorSpaceConfig = cfg.getChild(
                PDFConfigurationConstants.KEY_DISABLE_SRGB_COLORSPACE, false);
        if (disableColorSpaceConfig != null) {
            pdfUtil.setDisableSRGBColorSpace(disableColorSpaceConfig
                    .getValueAsBoolean(false));
        }
    }

    /**
     * Builds a filter map from an Avalon Configuration object.
     *
     * @param cfg
     *            the Configuration object
     * @return Map the newly built filter map
     * @throws ConfigurationException
     *             if a filter list is defined twice
     */
    public static Map<String, List<String>> buildFilterMapFromConfiguration(
            final Configuration cfg) throws ConfigurationException {
        final Map<String, List<String>> filterMap = new HashMap<>();
        final Configuration[] filterLists = cfg.getChildren("filterList");
        for (final Configuration filters : filterLists) {
            String type = filters.getAttribute("type", null);
            final Configuration[] filt = filters.getChildren("value");
            final List<String> filterList = new ArrayList<>();
            for (final Configuration element : filt) {
                final String name = element.getValue();
                filterList.add(name);
            }

            if (type == null) {
                type = PDFFilterList.DEFAULT_FILTER;
            }

            if (!filterList.isEmpty() && log.isDebugEnabled()) {
                final StringBuilder debug = new StringBuilder(
                        "Adding PDF filter");
                if (filterList.size() != 1) {
                    debug.append("s");
                }
                debug.append(" for type ").append(type).append(": ");
                for (int j = 0; j < filterList.size(); j++) {
                    if (j != 0) {
                        debug.append(", ");
                    }
                    debug.append(filterList.get(j));
                }
                log.debug(debug.toString());
            }

            if (filterMap.get(type) != null) {
                throw new ConfigurationException("A filterList of type '"
                        + type + "' has already been defined");
            }
            filterMap.put(type, filterList);
        }
        return filterMap;
    }

    // ---=== IFDocumentHandler configuration ===---

    /** {@inheritDoc} */
    @Override
    public void configure(final IFDocumentHandler documentHandler)
            throws FOPException {
        final Configuration cfg = super.getRendererConfig(documentHandler
                .getMimeType());
        if (cfg != null) {
            final PDFDocumentHandler pdfDocumentHandler = (PDFDocumentHandler) documentHandler;
            final PDFRenderingUtil pdfUtil = pdfDocumentHandler.getPDFUtil();
            configure(cfg, pdfUtil);
        }
    }

}
