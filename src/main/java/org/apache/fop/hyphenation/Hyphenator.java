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

/* $Id: Hyphenator.java 679326 2008-07-24 09:35:34Z vhennebert $ */

package org.apache.fop.hyphenation;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;

import javax.xml.transform.Source;
import javax.xml.transform.stream.StreamSource;

import lombok.extern.slf4j.Slf4j;

import org.apache.commons.io.IOUtils;
import org.xml.sax.InputSource;

/**
 * This class is the main entry point to the hyphenation package. You can use
 * only the static methods or create an instance.
 *
 * @author Carlos Villegas <cav@uniscope.co.jp>
 */
@Slf4j
public class Hyphenator {

    private static HyphenationTreeCache hTreeCache = null;

    private HyphenationTree hyphenTree = null;
    private int remainCharCount = 2;
    private int pushCharCount = 2;
    /**
     * Enables a dump of statistics. Note: If activated content is sent to
     * System.out!
     */
    private static boolean statisticsDump = false;

    /**
     * Creates a new hyphenator.
     *
     * @param lang
     *            the language
     * @param country
     *            the country (may be null or "none")
     * @param leftMin
     *            the minimum number of characters before the hyphenation point
     * @param rightMin
     *            the minimum number of characters after the hyphenation point
     */
    public Hyphenator(final String lang, final String country,
            final int leftMin, final int rightMin) {
        this.hyphenTree = getHyphenationTree(lang, country);
        this.remainCharCount = leftMin;
        this.pushCharCount = rightMin;
    }

    /** @return the default (static) hyphenation tree cache */
    public static synchronized HyphenationTreeCache getHyphenationTreeCache() {
        if (hTreeCache == null) {
            hTreeCache = new HyphenationTreeCache();
        }
        return hTreeCache;
    }

    /**
     * Returns a hyphenation tree for a given language and country. The
     * hyphenation trees are cached.
     *
     * @param lang
     *            the language
     * @param country
     *            the country (may be null or "none")
     * @return the hyphenation tree
     */
    public static HyphenationTree getHyphenationTree(final String lang,
            final String country) {
        return getHyphenationTree(lang, country, null);
    }

    /**
     * Returns a hyphenation tree for a given language and country. The
     * hyphenation trees are cached.
     *
     * @param lang
     *            the language
     * @param country
     *            the country (may be null or "none")
     * @param resolver
     *            resolver to find the hyphenation files
     * @return the hyphenation tree
     */
    public static HyphenationTree getHyphenationTree(final String lang,
            final String country, final HyphenationTreeResolver resolver) {
        final String key = HyphenationTreeCache.constructKey(lang, country);
        final HyphenationTreeCache cache = getHyphenationTreeCache();

        // See if there was an error finding this hyphenation tree before
        if (cache.isMissing(key)) {
            return null;
        }

        HyphenationTree hTree;
        // first try to find it in the cache
        hTree = getHyphenationTreeCache().getHyphenationTree(lang, country);
        if (hTree != null) {
            return hTree;
        }

        if (resolver != null) {
            hTree = getUserHyphenationTree(key, resolver);
        }
        if (hTree == null) {
            hTree = getFopHyphenationTree(key);
        }

        // put it into the pattern cache
        if (hTree != null) {
            cache.cache(key, hTree);
        } else {
            log.error("Couldn't find hyphenation pattern " + key);
            cache.noteMissing(key);
        }
        return hTree;
    }

    private static InputStream getResourceStream(final String key) {
        InputStream is = null;
        // Try to use Context Class Loader to load the properties file.
        try {
            final java.lang.reflect.Method getCCL = Thread.class.getMethod(
                    "getContextClassLoader", new Class[0]);
            if (getCCL != null) {
                final ClassLoader contextClassLoader = (ClassLoader) getCCL
                        .invoke(Thread.currentThread(), new Object[0]);
                is = contextClassLoader.getResourceAsStream("hyph/" + key
                        + ".hyp");
            }
        } catch (final Exception e) {
            // ignore, fallback further down
        }

        if (is == null) {
            is = Hyphenator.class.getResourceAsStream("/hyph/" + key + ".hyp");
        }

        return is;
    }

    private static HyphenationTree readHyphenationTree(final InputStream in) {
        HyphenationTree hTree = null;
        try {
            final ObjectInputStream ois = new ObjectInputStream(in);
            hTree = (HyphenationTree) ois.readObject();
        } catch (final IOException ioe) {
            log.error(
                    "I/O error while loading precompiled hyphenation pattern file",
                    ioe);
        } catch (final ClassNotFoundException cnfe) {
            log.error("Error while reading hyphenation object from file", cnfe);
        }
        return hTree;
    }

    /**
     * Returns a hyphenation tree. This method looks in the resources
     * (getResourceStream) for the hyphenation patterns.
     *
     * @param key
     *            the language/country key
     * @return the hyphenation tree or null if it wasn't found in the resources
     */
    public static HyphenationTree getFopHyphenationTree(final String key) {
        HyphenationTree hTree = null;
        final ObjectInputStream ois = null;
        InputStream is = null;
        try {
            is = getResourceStream(key);
            if (is == null) {
                if (key.length() == 5) {
                    final String lang = key.substring(0, 2);
                    is = getResourceStream(lang);
                    if (is != null) {
                        if (log.isDebugEnabled()) {
                            log.debug("Couldn't find hyphenation pattern '"
                                    + key
                                    + "'. Using general language pattern '"
                                    + lang + "' instead.");
                        }
                    } else {
                        if (log.isDebugEnabled()) {
                            log.debug("Couldn't find precompiled hyphenation pattern "
                                    + lang + " in resources.");
                        }
                        return null;
                    }
                } else {
                    if (log.isDebugEnabled()) {
                        log.debug("Couldn't find precompiled hyphenation pattern "
                                + key + " in resources");
                    }
                    return null;
                }
            }
            hTree = readHyphenationTree(is);
        } finally {
            IOUtils.closeQuietly(ois);
        }
        return hTree;
    }

    /**
     * Load tree from serialized file or xml file using configuration settings
     *
     * @param key
     *            language key for the requested hyphenation file
     * @param hyphenDir
     *            base directory to find hyphenation files in
     * @return the requested HypenationTree or null if it is not available
     */
    public static HyphenationTree getUserHyphenationTree(final String key,
            final String hyphenDir) {
        final File baseDir = new File(hyphenDir);
        final HyphenationTreeResolver resolver = new HyphenationTreeResolver() {
            @Override
            public Source resolve(final String href) {
                final File f = new File(baseDir, href);
                return new StreamSource(f);
            }
        };
        return getUserHyphenationTree(key, resolver);
    }

    /**
     * Load tree from serialized file or xml file using configuration settings
     *
     * @param key
     *            language key for the requested hyphenation file
     * @param resolver
     *            resolver to find the hyphenation files
     * @return the requested HypenationTree or null if it is not available
     */
    public static HyphenationTree getUserHyphenationTree(final String key,
            final HyphenationTreeResolver resolver) {
        HyphenationTree hTree = null;
        // I use here the following convention. The file name specified in
        // the configuration is taken as the base name. First we try
        // name + ".hyp" assuming a serialized HyphenationTree. If that fails
        // we try name + ".xml", assumming a raw hyphenation pattern file.

        // first try serialized object
        String name = key + ".hyp";
        Source source = resolver.resolve(name);
        if (source != null) {
            try {
                InputStream in = null;
                if (source instanceof StreamSource) {
                    in = ((StreamSource) source).getInputStream();
                }
                if (in == null) {
                    if (source.getSystemId() != null) {
                        in = new java.net.URL(source.getSystemId())
                        .openStream();
                    } else {
                        throw new UnsupportedOperationException(
                                "Cannot load hyphenation pattern file"
                                        + " with the supplied Source object: "
                                        + source);
                    }
                }
                in = new BufferedInputStream(in);
                try {
                    hTree = readHyphenationTree(in);
                } finally {
                    IOUtils.closeQuietly(in);
                }
                return hTree;
            } catch (final IOException ioe) {
                if (log.isDebugEnabled()) {
                    log.debug("I/O problem while trying to load " + name, ioe);
                }
            }
        }

        // try the raw XML file
        name = key + ".xml";
        source = resolver.resolve(name);
        if (source != null) {
            hTree = new HyphenationTree();
            try {
                InputStream in = null;
                if (source instanceof StreamSource) {
                    in = ((StreamSource) source).getInputStream();
                }
                if (in == null) {
                    if (source.getSystemId() != null) {
                        in = new java.net.URL(source.getSystemId())
                        .openStream();
                    } else {
                        throw new UnsupportedOperationException(
                                "Cannot load hyphenation pattern file"
                                        + " with the supplied Source object: "
                                        + source);
                    }
                }
                if (!(in instanceof BufferedInputStream)) {
                    in = new BufferedInputStream(in);
                }
                try {
                    final InputSource src = new InputSource(in);
                    src.setSystemId(source.getSystemId());
                    hTree.loadPatterns(src);
                } finally {
                    IOUtils.closeQuietly(in);
                }
                if (statisticsDump) {
                    log.info("Stats: ");
                    hTree.printStats();
                }
                return hTree;
            } catch (final HyphenationException ex) {
                log.error("Can't load user patterns from XML file "
                        + source.getSystemId() + ": " + ex.getMessage());
                return null;
            } catch (final IOException ioe) {
                if (log.isDebugEnabled()) {
                    log.debug("I/O problem while trying to load " + name, ioe);
                }
                return null;
            }
        } else {
            if (log.isDebugEnabled()) {
                log.debug("Could not load user hyphenation file for '" + key
                        + "'.");
            }
            return null;
        }
    }

    /**
     * Hyphenates a word.
     *
     * @param lang
     *            the language
     * @param country
     *            the optional country code (may be null or "none")
     * @param resolver
     *            resolver to find the hyphenation files
     * @param word
     *            the word to hyphenate
     * @param leftMin
     *            the minimum number of characters before the hyphenation point
     * @param rightMin
     *            the minimum number of characters after the hyphenation point
     * @return the hyphenation result
     */
    public static Hyphenation hyphenate(final String lang,
            final String country, final HyphenationTreeResolver resolver,
            final String word, final int leftMin, final int rightMin) {
        final HyphenationTree hTree = getHyphenationTree(lang, country,
                resolver);
        if (hTree == null) {
            return null;
        }
        return hTree.hyphenate(word, leftMin, rightMin);
    }

    /**
     * Hyphenates a word.
     *
     * @param lang
     *            the language
     * @param country
     *            the optional country code (may be null or "none")
     * @param word
     *            the word to hyphenate
     * @param leftMin
     *            the minimum number of characters before the hyphenation point
     * @param rightMin
     *            the minimum number of characters after the hyphenation point
     * @return the hyphenation result
     */
    public static Hyphenation hyphenate(final String lang,
            final String country, final String word, final int leftMin,
            final int rightMin) {
        return hyphenate(lang, country, null, word, leftMin, rightMin);
    }

    /**
     * Hyphenates a word.
     *
     * @param lang
     *            the language
     * @param country
     *            the optional country code (may be null or "none")
     * @param resolver
     *            resolver to find the hyphenation files
     * @param word
     *            the word to hyphenate
     * @param offset
     *            the offset of the first character in the "word" character
     *            array
     * @param len
     *            the length of the word
     * @param leftMin
     *            the minimum number of characters before the hyphenation point
     * @param rightMin
     *            the minimum number of characters after the hyphenation point
     * @return the hyphenation result
     */
    public static Hyphenation hyphenate(final String lang,
            final String country, final HyphenationTreeResolver resolver,
            final char[] word, final int offset, final int len,
            final int leftMin, final int rightMin) {
        final HyphenationTree hTree = getHyphenationTree(lang, country,
                resolver);
        if (hTree == null) {
            return null;
        }
        return hTree.hyphenate(word, offset, len, leftMin, rightMin);
    }

    /**
     * Hyphenates a word.
     *
     * @param lang
     *            the language
     * @param country
     *            the optional country code (may be null or "none")
     * @param word
     *            the word to hyphenate
     * @param offset
     *            the offset of the first character in the "word" character
     *            array
     * @param len
     *            the length of the word
     * @param leftMin
     *            the minimum number of characters before the hyphenation point
     * @param rightMin
     *            the minimum number of characters after the hyphenation point
     * @return the hyphenation result
     */
    public static Hyphenation hyphenate(final String lang,
            final String country, final char[] word, final int offset,
            final int len, final int leftMin, final int rightMin) {
        return hyphenate(lang, country, null, word, offset, len, leftMin,
                rightMin);
    }

    /**
     * Sets the minimum number of characters before the hyphenation point
     *
     * @param min
     *            the number of characters
     */
    public void setMinRemainCharCount(final int min) {
        this.remainCharCount = min;
    }

    /**
     * Sets the minimum number of characters after the hyphenation point
     *
     * @param min
     *            the number of characters
     */
    public void setMinPushCharCount(final int min) {
        this.pushCharCount = min;
    }

    /**
     * Sets the language and country for the hyphenation process.
     *
     * @param lang
     *            the language
     * @param country
     *            the country (may be null or "none")
     */
    public void setLanguage(final String lang, final String country) {
        this.hyphenTree = getHyphenationTree(lang, country);
    }

    /**
     * Hyphenates a word.
     *
     * @param word
     *            the word to hyphenate
     * @param offset
     *            the offset of the first character in the "word" character
     *            array
     * @param len
     *            the length of the word
     * @return the hyphenation result
     */
    public Hyphenation hyphenate(final char[] word, final int offset,
            final int len) {
        if (this.hyphenTree == null) {
            return null;
        }
        return this.hyphenTree.hyphenate(word, offset, len,
                this.remainCharCount, this.pushCharCount);
    }

    /**
     * Hyphenates a word.
     *
     * @param word
     *            the word to hyphenate
     * @return the hyphenation result
     */
    public Hyphenation hyphenate(final String word) {
        if (this.hyphenTree == null) {
            return null;
        }
        return this.hyphenTree.hyphenate(word, this.remainCharCount,
                this.pushCharCount);
    }

}
