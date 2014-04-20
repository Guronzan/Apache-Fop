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

/* $Id: FontManagerConfigurator.java 821058 2009-10-02 15:31:14Z jeremias $ */

package org.apache.fop.fonts;

import java.net.MalformedURLException;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

import lombok.extern.slf4j.Slf4j;

import org.apache.avalon.framework.configuration.Configuration;
import org.apache.avalon.framework.configuration.ConfigurationException;
import org.apache.fop.apps.FOPException;
import org.apache.fop.fonts.FontTriplet.Matcher;
import org.apache.fop.fonts.substitute.FontSubstitutions;
import org.apache.fop.fonts.substitute.FontSubstitutionsConfigurator;
import org.apache.fop.util.LogUtil;

/**
 * Configurator of the FontManager
 */
@Slf4j
public class FontManagerConfigurator {

    private final Configuration cfg;

    /**
     * Main constructor
     *
     * @param cfg
     *            the font manager configuration object
     */
    public FontManagerConfigurator(final Configuration cfg) {
        this.cfg = cfg;
    }

    /**
     * Initializes font settings from the user configuration
     *
     * @param fontManager
     *            a font manager
     * @param strict
     *            true if strict checking of the configuration is enabled
     * @throws FOPException
     *             if an exception occurs while processing the configuration
     */
    public void configure(final FontManager fontManager, final boolean strict)
            throws FOPException {

        // caching (fonts)
        if (this.cfg.getChild("use-cache", false) != null) {
            try {
                fontManager.setUseCache(this.cfg.getChild("use-cache")
                        .getValueAsBoolean());
            } catch (final ConfigurationException mfue) {
                LogUtil.handleException(log, mfue, true);
            }
        }
        if (this.cfg.getChild("font-base", false) != null) {
            try {
                fontManager.setFontBaseURL(this.cfg.getChild("font-base")
                        .getValue(null));
            } catch (final MalformedURLException mfue) {
                LogUtil.handleException(log, mfue, true);
            }
        }

        // global font configuration
        final Configuration fontsCfg = this.cfg.getChild("fonts", false);
        if (fontsCfg != null) {
            // font substitution
            final Configuration substitutionsCfg = fontsCfg.getChild(
                    "substitutions", false);
            if (substitutionsCfg != null) {
                final FontSubstitutionsConfigurator fontSubstitutionsConfigurator = new FontSubstitutionsConfigurator(
                        substitutionsCfg);
                final FontSubstitutions substitutions = new FontSubstitutions();
                fontSubstitutionsConfigurator.configure(substitutions);
                fontManager.setFontSubstitutions(substitutions);
            }

            // referenced fonts (fonts which are not to be embedded)
            final Configuration referencedFontsCfg = fontsCfg.getChild(
                    "referenced-fonts", false);
            if (referencedFontsCfg != null) {
                final FontTriplet.Matcher matcher = createFontsMatcher(
                        referencedFontsCfg, strict);
                fontManager.setReferencedFontsMatcher(matcher);
            }

        }
    }

    /**
     * Creates a font triplet matcher from a configuration object.
     *
     * @param cfg
     *            the configuration object
     * @param strict
     *            true for strict configuraton error handling
     * @return the font matcher
     * @throws FOPException
     *             if an error occurs while building the matcher
     */
    public static FontTriplet.Matcher createFontsMatcher(
            final Configuration cfg, final boolean strict) throws FOPException {
        final List<FontFamilyRegExFontTripletMatcher> matcherList = new ArrayList<>();
        final Configuration[] matches = cfg.getChildren("match");
        for (final Configuration matche : matches) {
            try {
                matcherList.add(new FontFamilyRegExFontTripletMatcher(matche
                        .getAttribute("font-family")));
            } catch (final ConfigurationException ce) {
                LogUtil.handleException(log, ce, strict);
                continue;
            }
        }
        final FontTriplet.Matcher orMatcher = new OrFontTripletMatcher(
                matcherList.toArray(new FontTriplet.Matcher[matcherList.size()]));
        return orMatcher;
    }

    private static class OrFontTripletMatcher implements FontTriplet.Matcher {

        private final FontTriplet.Matcher[] matchers;

        public OrFontTripletMatcher(final FontTriplet.Matcher[] matchers) {
            this.matchers = matchers;
        }

        /** {@inheritDoc} */
        @Override
        public boolean matches(final FontTriplet triplet) {
            for (final Matcher matcher : this.matchers) {
                if (matcher.matches(triplet)) {
                    return true;
                }
            }
            return false;
        }

    }

    private static class FontFamilyRegExFontTripletMatcher implements
            FontTriplet.Matcher {

        private final Pattern regex;

        public FontFamilyRegExFontTripletMatcher(final String regex) {
            this.regex = Pattern.compile(regex);
        }

        /** {@inheritDoc} */
        @Override
        public boolean matches(final FontTriplet triplet) {
            return this.regex.matcher(triplet.getName()).matches();
        }

    }

}
