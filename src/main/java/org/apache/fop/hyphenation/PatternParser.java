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

/* $Id: PatternParser.java 808166 2009-08-26 20:00:24Z spepping $ */

package org.apache.fop.hyphenation;

// SAX
// Java
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintStream;
import java.net.MalformedURLException;
import java.util.ArrayList;
import java.util.List;

import javax.xml.parsers.SAXParserFactory;

import lombok.extern.slf4j.Slf4j;

import org.xml.sax.Attributes;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;
import org.xml.sax.XMLReader;
import org.xml.sax.helpers.DefaultHandler;

/**
 * A SAX document handler to read and parse hyphenation patterns from a XML
 * file.
 *
 * @author Carlos Villegas <cav@uniscope.co.jp>
 */
@Slf4j
public class PatternParser extends DefaultHandler implements PatternConsumer {

    private XMLReader parser;
    private int currElement;
    private PatternConsumer consumer;
    private final StringBuilder token;
    private ArrayList<Object> exception;
    private char hyphenChar;
    private String errMsg;
    private boolean hasClasses = false;

    static final int ELEM_CLASSES = 1;
    static final int ELEM_EXCEPTIONS = 2;
    static final int ELEM_PATTERNS = 3;
    static final int ELEM_HYPHEN = 4;

    public PatternParser() {
        this.consumer = this;
        this.token = new StringBuilder();
        this.parser = createParser();
        this.parser.setContentHandler(this);
        this.parser.setErrorHandler(this);
        this.hyphenChar = '-'; // default
    }

    public PatternParser(final PatternConsumer consumer) {
        this();
        this.consumer = consumer;
    }

    /**
     * Parses a hyphenation pattern file.
     *
     * @param filename
     *            the filename
     * @throws HyphenationException
     *             In case of an exception while parsing
     */
    public void parse(final String filename) throws HyphenationException {
        parse(new File(filename));
    }

    /**
     * Parses a hyphenation pattern file.
     *
     * @param file
     *            the pattern file
     * @throws HyphenationException
     *             In case of an exception while parsing
     */
    public void parse(final File file) throws HyphenationException {
        try {
            final InputSource src = new InputSource(file.toURI().toURL()
                    .toExternalForm());
            parse(src);
        } catch (final MalformedURLException e) {
            throw new HyphenationException("Error converting the File '" + file
                    + "' to a URL: " + e.getMessage());
        }
    }

    /**
     * Parses a hyphenation pattern file.
     *
     * @param source
     *            the InputSource for the file
     * @throws HyphenationException
     *             In case of an exception while parsing
     */
    public void parse(final InputSource source) throws HyphenationException {
        try {
            this.parser.parse(source);
        } catch (final FileNotFoundException fnfe) {
            throw new HyphenationException("File not found: "
                    + fnfe.getMessage());
        } catch (final IOException ioe) {
            throw new HyphenationException(ioe.getMessage());
        } catch (final SAXException e) {
            throw new HyphenationException(this.errMsg);
        }
    }

    /**
     * Creates a SAX parser using JAXP
     *
     * @return the created SAX parser
     */
    static XMLReader createParser() {
        try {
            final SAXParserFactory factory = SAXParserFactory.newInstance();
            factory.setNamespaceAware(true);
            return factory.newSAXParser().getXMLReader();
        } catch (final Exception e) {
            throw new RuntimeException("Couldn't create XMLReader: "
                    + e.getMessage());
        }
    }

    protected String readToken(final StringBuilder chars) {
        String word;
        boolean space = false;
        int i;
        for (i = 0; i < chars.length(); ++i) {
            if (Character.isWhitespace(chars.charAt(i))) {
                space = true;
            } else {
                break;
            }
        }
        if (space) {
            // chars.delete(0,i);
            for (int countr = i; countr < chars.length(); countr++) {
                chars.setCharAt(countr - i, chars.charAt(countr));
            }
            chars.setLength(chars.length() - i);
            if (this.token.length() > 0) {
                word = this.token.toString();
                this.token.setLength(0);
                return word;
            }
        }
        space = false;
        for (i = 0; i < chars.length(); ++i) {
            if (Character.isWhitespace(chars.charAt(i))) {
                space = true;
                break;
            }
        }
        this.token.append(chars.toString().substring(0, i));
        // chars.delete(0,i);
        for (int countr = i; countr < chars.length(); countr++) {
            chars.setCharAt(countr - i, chars.charAt(countr));
        }
        chars.setLength(chars.length() - i);
        if (space) {
            word = this.token.toString();
            this.token.setLength(0);
            return word;
        }
        this.token.append(chars);
        return null;
    }

    protected static String getPattern(final String word) {
        final StringBuilder pat = new StringBuilder();
        final int len = word.length();
        for (int i = 0; i < len; ++i) {
            if (!Character.isDigit(word.charAt(i))) {
                pat.append(word.charAt(i));
            }
        }
        return pat.toString();
    }

    protected ArrayList<Object> normalizeException(final ArrayList<Object> ex) {
        final ArrayList<Object> res = new ArrayList<>();
        for (int i = 0; i < ex.size(); ++i) {
            final Object item = ex.get(i);
            if (item instanceof String) {
                final String str = (String) item;
                final StringBuilder buf = new StringBuilder();
                for (int j = 0; j < str.length(); j++) {
                    final char c = str.charAt(j);
                    if (c != this.hyphenChar) {
                        buf.append(c);
                    } else {
                        res.add(buf.toString());
                        buf.setLength(0);
                        final char[] h = new char[1];
                        h[0] = this.hyphenChar;
                        // we use here hyphenChar which is not necessarily
                        // the one to be printed
                        res.add(new Hyphen(new String(h), null, null));
                    }
                }
                if (buf.length() > 0) {
                    res.add(buf.toString());
                }
            } else {
                res.add(item);
            }
        }
        return res;
    }

    protected String getExceptionWord(final ArrayList<Object> ex) {
        final StringBuilder res = new StringBuilder();
        for (final Object item : ex) {
            if (item instanceof String) {
                res.append((String) item);
            } else {
                if (((Hyphen) item).noBreak != null) {
                    res.append(((Hyphen) item).noBreak);
                }
            }
        }
        return res.toString();
    }

    protected static String getInterletterValues(final String pat) {
        final StringBuilder il = new StringBuilder();
        final String word = pat + "a"; // add dummy letter to serve as sentinel
        final int len = word.length();
        for (int i = 0; i < len; ++i) {
            final char c = word.charAt(i);
            if (Character.isDigit(c)) {
                il.append(c);
                ++i;
            } else {
                il.append('0');
            }
        }
        return il.toString();
    }

    protected void getExternalClasses() throws SAXException {
        final XMLReader mainParser = this.parser;
        this.parser = createParser();
        this.parser.setContentHandler(this);
        this.parser.setErrorHandler(this);
        try (final InputStream stream = this.getClass().getResourceAsStream(
                "classes.xml")) {
            final InputSource source = new InputSource(stream);
            this.parser.parse(source);
        } catch (final IOException ioe) {
            throw new SAXException(ioe.getMessage());
        } finally {
            this.parser = mainParser;
        }
    }

    //
    // ContentHandler methods
    //

    /**
     * {@inheritDoc}
     *
     * @throws SAXException
     */
    @Override
    public void startElement(final String uri, final String local,
            final String raw, final Attributes attrs) throws SAXException {
        if (local.equals("hyphen-char")) {
            final String h = attrs.getValue("value");
            if (h != null && h.length() == 1) {
                this.hyphenChar = h.charAt(0);
            }
        } else if (local.equals("classes")) {
            this.currElement = ELEM_CLASSES;
        } else if (local.equals("patterns")) {
            if (!this.hasClasses) {
                getExternalClasses();
            }
            this.currElement = ELEM_PATTERNS;
        } else if (local.equals("exceptions")) {
            if (!this.hasClasses) {
                getExternalClasses();
            }
            this.currElement = ELEM_EXCEPTIONS;
            this.exception = new ArrayList<>();
        } else if (local.equals("hyphen")) {
            if (this.token.length() > 0) {
                this.exception.add(this.token.toString());
            }
            this.exception.add(new Hyphen(attrs.getValue("pre"), attrs
                    .getValue("no"), attrs.getValue("post")));
            this.currElement = ELEM_HYPHEN;
        }
        this.token.setLength(0);
    }

    /**
     * {@inheritDoc}
     */
    @SuppressWarnings("unchecked")
    @Override
    public void endElement(final String uri, final String local,
            final String raw) {

        if (this.token.length() > 0) {
            final String word = this.token.toString();
            switch (this.currElement) {
            case ELEM_CLASSES:
                this.consumer.addClass(word);
                break;
            case ELEM_EXCEPTIONS:
                this.exception.add(word);
                this.exception = normalizeException(this.exception);
                this.consumer.addException(getExceptionWord(this.exception),
                        (ArrayList<Object>) this.exception.clone());
                break;
            case ELEM_PATTERNS:
                this.consumer.addPattern(getPattern(word),
                        getInterletterValues(word));
                break;
            case ELEM_HYPHEN:
                // nothing to do
                break;
            }
            if (this.currElement != ELEM_HYPHEN) {
                this.token.setLength(0);
            }
        }
        if (this.currElement == ELEM_CLASSES) {
            this.hasClasses = true;
        }
        if (this.currElement == ELEM_HYPHEN) {
            this.currElement = ELEM_EXCEPTIONS;
        } else {
            this.currElement = 0;
        }

    }

    /**
     * {@inheritDoc}
     */
    @SuppressWarnings("unchecked")
    @Override
    public void characters(final char ch[], final int start, final int length) {
        final StringBuilder chars = new StringBuilder(length);
        chars.append(ch, start, length);
        String word = readToken(chars);
        while (word != null) {
            // log.info("\"" + word + "\"");
            switch (this.currElement) {
            case ELEM_CLASSES:
                this.consumer.addClass(word);
                break;
            case ELEM_EXCEPTIONS:
                this.exception.add(word);
                this.exception = normalizeException(this.exception);
                this.consumer.addException(getExceptionWord(this.exception),
                        (ArrayList<Object>) this.exception.clone());
                this.exception.clear();
                break;
            case ELEM_PATTERNS:
                this.consumer.addPattern(getPattern(word),
                        getInterletterValues(word));
                break;
            }
            word = readToken(chars);
        }

    }

    //
    // ErrorHandler methods
    //

    /**
     * {@inheritDoc}
     */
    @Override
    public void warning(final SAXParseException ex) {
        this.errMsg = "[Warning] " + getLocationString(ex) + ": "
                + ex.getMessage();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void error(final SAXParseException ex) {
        this.errMsg = "[Error] " + getLocationString(ex) + ": "
                + ex.getMessage();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void fatalError(final SAXParseException ex) throws SAXException {
        this.errMsg = "[Fatal Error] " + getLocationString(ex) + ": "
                + ex.getMessage();
        throw ex;
    }

    /**
     * Returns a string of the location.
     */
    private String getLocationString(final SAXParseException ex) {
        final StringBuilder str = new StringBuilder();

        String systemId = ex.getSystemId();
        if (systemId != null) {
            final int index = systemId.lastIndexOf('/');
            if (index != -1) {
                systemId = systemId.substring(index + 1);
            }
            str.append(systemId);
        }
        str.append(':');
        str.append(ex.getLineNumber());
        str.append(':');
        str.append(ex.getColumnNumber());

        return str.toString();

    } // getLocationString(SAXParseException):String

    // PatternConsumer implementation for testing purposes
    @Override
    public void addClass(final String c) {
        this.testOut.println("class: " + c);
    }

    @Override
    public void addException(final String w, final List<Object> e) {
        log.error("exception: " + w + " : " + e.toString());
    }

    @Override
    public void addPattern(final String p, final String v) {
        this.testOut.println("pattern: " + p + " : " + v);
    }

    private PrintStream testOut = System.out;

    /**
     * @param testOut
     *            the testOut to set
     */
    public void setTestOut(final PrintStream testOut) {
        this.testOut = testOut;
    }

    public void closeTestOut() {
        this.testOut.flush();
        this.testOut.close();
    }

    public static void main(final String[] args) throws Exception {
        if (args.length > 0) {
            final PatternParser pp = new PatternParser();
            PrintStream p = null;
            if (args.length > 1) {
                final FileOutputStream f = new FileOutputStream(args[1]);
                p = new PrintStream(f, false, "utf-8");
                pp.setTestOut(p);
            }
            pp.parse(args[0]);
            pp.closeTestOut();
        }
    }

}
