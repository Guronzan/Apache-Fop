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

/* $Id: PDFDocument.java 1305467 2012-03-26 17:39:20Z vhennebert $ */

package org.apache.fop.pdf;

// Java
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import lombok.extern.slf4j.Slf4j;
import pdf.xref.CrossReferenceStream;
import pdf.xref.CrossReferenceTable;
import pdf.xref.TrailerDictionary;

/* image support modified from work of BoBoGi */
/* font support based on work by Takayuki Takeuchi */

/**
 * Class representing a PDF document.
 *
 * The document is built up by calling various methods and then finally output
 * to given filehandle using output method.
 *
 * A PDF document consists of a series of numbered objects preceded by a header
 * and followed by an xref table and trailer. The xref table allows for quick
 * access to objects by listing their character positions within the document.
 * For this reason the PDF document must keep track of the character position of
 * each object. The document also keeps direct track of the /Root, /Info and
 * /Resources objects.
 *
 * Modified by Mark Lillywhite, mark-fop@inomial.com. The changes involve:
 * ability to output pages one-at-a-time in a streaming fashion (rather than
 * storing them all for output at the end); ability to write the /Pages object
 * after writing the rest of the document; ability to write to a stream and
 * flush the object list; enhanced trailer output; cleanups.
 *
 */
@Slf4j
public class PDFDocument {

    /** the encoding to use when converting strings to PDF commands */
    public static final String ENCODING = "ISO-8859-1";

    /** the counter for object numbering */
    protected int objectcount;

    /** the current character position */
    private long position;

    /** the character position of each object */
    private final List<Long> indirectObjectOffsets = new ArrayList<>();

    private Collection<PDFStructElem> structureTreeElements;

    /** List of objects to write in the trailer */
    private final List<PDFObject> trailerObjects = new ArrayList<>();

    /** the objects themselves */
    private final List<PDFObject> objects = new LinkedList<>();

    /** Controls the PDF version of this document */
    private VersionController versionController;

    /** Indicates which PDF profiles are active (PDF/A, PDF/X etc.) */
    private final PDFProfile pdfProfile = new PDFProfile(this);

    /** the /Root object */
    private final PDFRoot root;

    /** The root outline object */
    private PDFOutline outlineRoot;

    /** The /Pages object (mark-fop@inomial.com) */
    private final PDFPages pages;

    /** the /Info object */
    private final PDFInfo info;

    /** the /Resources object */
    private final PDFResources resources;

    /** the document's encryption, if it exists */
    private PDFEncryption encryption;

    /** the colorspace (0=RGB, 1=CMYK) */
    private final PDFDeviceColorSpace colorspace = new PDFDeviceColorSpace(
            PDFDeviceColorSpace.DEVICE_RGB);

    /** the counter for Pattern name numbering (e.g. 'Pattern1') */
    private int patternCount;

    /** the counter for Shading name numbering */
    private int shadingCount;

    /** the counter for XObject numbering */
    private int xObjectCount;

    /* TODO: Should be modified (works only for image subtype) */
    private final Map<String, PDFXObject> xObjectsMap = new HashMap<>();

    private final Map<String, PDFFont> fontMap = new HashMap<>();

    private Map<String, List<String>> filterMap = new HashMap<>();

    private final List<PDFGState> gstates = new ArrayList<>();

    private final List<PDFFunction> functions = new ArrayList<>();

    private final List<PDFShading> shadings = new ArrayList<>();

    private final List<PDFPattern> patterns = new ArrayList<>();

    private final List<PDFLink> links = new ArrayList<>();

    private List<PDFDestination> destinations;

    private final List<PDFFileSpec> filespecs = new ArrayList<>();

    private final List<PDFGoToRemote> gotoremotes = new ArrayList<>();

    private final List<PDFGoTo> gotos = new ArrayList<>();

    private final List<PDFLaunch> launches = new ArrayList<>();

    private final PDFFactory factory;

    private FileIDGenerator fileIDGenerator;

    private boolean accessibilityEnabled;

    /**
     * Creates an empty PDF document.
     *
     * The constructor creates a /Root and /Pages object to track the document
     * but does not write these objects until the trailer is written. Note that
     * the object ID of the pages object is determined now, and the xref table
     * is updated later. This allows Pages to refer to their Parent before we
     * write it out.
     *
     * @param prod
     *            the name of the producer of this pdf document
     */
    public PDFDocument(final String prod) {
        this(prod, null);
        this.versionController = VersionController.getDynamicVersionController(
                Version.V1_4, this);
    }

    /**
     * Creates an empty PDF document.
     *
     * The constructor creates a /Root and /Pages object to track the document
     * but does not write these objects until the trailer is written. Note that
     * the object ID of the pages object is determined now, and the xref table
     * is updated later. This allows Pages to refer to their Parent before we
     * write it out.
     *
     * @param prod
     *            the name of the producer of this pdf document
     * @param versionController
     *            the version controller of this PDF document
     */
    public PDFDocument(final String prod,
            final VersionController versionController) {

        this.factory = new PDFFactory(this);

        /* create the /Root, /Info and /Resources objects */
        this.pages = getFactory().makePages();

        // Create the Root object
        this.root = getFactory().makeRoot(this.pages);

        // Create the Resources object
        this.resources = getFactory().makeResources();

        // Make the /Info record
        this.info = getFactory().makeInfo(prod);

        this.versionController = versionController;
    }

    /**
     * Returns the current PDF version.
     *
     * @return returns the PDF version
     */
    public Version getPDFVersion() {
        return this.versionController.getPDFVersion();
    }

    /**
     * Sets the PDF version of this document.
     *
     * @param version
     *            the PDF version
     * @throws IllegalStateException
     *             if the version of this PDF is not allowed to change.
     */
    public void setPDFVersion(final Version version) {
        this.versionController.setPDFVersion(version);
    }

    /** @return the String representing the current PDF version */
    public String getPDFVersionString() {
        return this.versionController.getPDFVersion().toString();
    }

    /** @return the PDF profile currently active. */
    public PDFProfile getProfile() {
        return this.pdfProfile;
    }

    /**
     * Returns the factory for PDF objects.
     *
     * @return the {@link PDFFactory} object
     */
    public PDFFactory getFactory() {
        return this.factory;
    }

    /**
     * Converts text to a byte array for writing to a PDF file.
     *
     * @param text
     *            text to convert/encode
     * @return the resulting <code>byte</code> array
     */
    public static byte[] encode(final String text) {
        try {
            return text.getBytes(ENCODING);
        } catch (final UnsupportedEncodingException uee) {
            return text.getBytes();
        }
    }

    /**
     * Flushes the given text buffer to an output stream with the right encoding
     * and resets the text buffer. This is used to efficiently switch between
     * outputting text and binary content.
     *
     * @param textBuffer
     *            the text buffer
     * @param out
     *            the output stream to flush the text content to
     * @throws IOException
     *             if an I/O error occurs while writing to the output stream
     */
    public static void flushTextBuffer(final StringBuilder textBuffer,
            final OutputStream out) throws IOException {
        out.write(encode(textBuffer.toString()));
        textBuffer.setLength(0);
    }

    /**
     * Sets the producer of the document.
     *
     * @param producer
     *            string indicating application producing the PDF
     */
    public void setProducer(final String producer) {
        this.info.setProducer(producer);
    }

    /**
     * Sets the creation date of the document.
     *
     * @param date
     *            Date to be stored as creation date in the PDF.
     */
    public void setCreationDate(final Date date) {
        this.info.setCreationDate(date);
    }

    /**
     * Sets the creator of the document.
     *
     * @param creator
     *            string indicating application creating the document
     */
    public void setCreator(final String creator) {
        this.info.setCreator(creator);
    }

    /**
     * Sets the filter map to use for filters in this document.
     *
     * @param map
     *            the map of filter lists for each stream type
     */
    public void setFilterMap(final Map<String, List<String>> map) {
        this.filterMap = map;
    }

    /**
     * Returns the {@link PDFFilter}s map used for filters in this document.
     *
     * @return the map of filters being used
     */
    public Map<String, List<String>> getFilterMap() {
        return this.filterMap;
    }

    /**
     * Returns the {@link PDFPages} object associated with the root object.
     *
     * @return the {@link PDFPages} object
     */
    public PDFPages getPages() {
        return this.pages;
    }

    /**
     * Get the {@link PDFRoot} object for this document.
     *
     * @return the {@link PDFRoot} object
     */
    public PDFRoot getRoot() {
        return this.root;
    }

    /**
     * Creates and returns a StructTreeRoot object.
     *
     * @param parentTree
     *            the value of the ParenTree entry
     * @return the structure tree root
     */
    public PDFStructTreeRoot makeStructTreeRoot(final PDFParentTree parentTree) {
        final PDFStructTreeRoot structTreeRoot = new PDFStructTreeRoot(
                parentTree);
        assignObjectNumber(structTreeRoot);
        addTrailerObject(structTreeRoot);
        this.root.setStructTreeRoot(structTreeRoot);
        this.structureTreeElements = new ArrayList<>();
        return structTreeRoot;
    }

    /**
     * Creates and returns a structure element.
     *
     * @param structureType
     *            the structure type of the new element (value for the S entry)
     * @param parent
     *            the parent of the new structure element in the structure
     *            hierarchy
     * @return a dictionary of type StructElem
     */
    public PDFStructElem makeStructureElement(final PDFName structureType,
            final PDFObject parent) {
        final PDFStructElem structElem = new PDFStructElem(parent,
                structureType);
        assignObjectNumber(structElem);
        this.structureTreeElements.add(structElem);
        return structElem;
    }

    /**
     * Get the {@link PDFInfo} object for this document.
     *
     * @return the {@link PDFInfo} object
     */
    public PDFInfo getInfo() {
        return this.info;
    }

    /**
     * Registers a {@link PDFObject} in this PDF document. The object is
     * assigned a new object number.
     *
     * @param obj
     *            {@link PDFObject} to add
     * @return the added {@link PDFObject} added (with its object number set)
     */
    public PDFObject registerObject(final PDFObject obj) {
        assignObjectNumber(obj);
        addObject(obj);
        return obj;
    }

    /**
     * Assigns the {@link PDFObject} an object number, and sets the parent of
     * the {@link PDFObject} to this document.
     *
     * @param obj
     *            {@link PDFObject} to assign a number to
     */
    public void assignObjectNumber(final PDFObject obj) {
        if (obj == null) {
            throw new NullPointerException("obj must not be null");
        }
        if (obj.hasObjectNumber()) {
            throw new IllegalStateException("Error registering a PDFObject: "
                    + "PDFObject already has an object number");
        }
        final PDFDocument currentParent = obj.getDocument();
        if (currentParent != null && currentParent != this) {
            throw new IllegalStateException("Error registering a PDFObject: "
                    + "PDFObject already has a parent PDFDocument");
        }

        obj.setObjectNumber(++this.objectcount);

        if (currentParent == null) {
            obj.setDocument(this);
        }
    }

    /**
     * Adds a {@link PDFObject} to this document. The object <em>MUST</em> have
     * an object number assigned.
     *
     * @param obj
     *            {@link PDFObject} to add
     */
    public void addObject(final PDFObject obj) {
        if (obj == null) {
            throw new NullPointerException("obj must not be null");
        }
        if (!obj.hasObjectNumber()) {
            throw new IllegalStateException("Error adding a PDFObject: "
                    + "PDFObject doesn't have an object number");
        }

        // Add object to list
        this.objects.add(obj);

        // Add object to special lists where necessary
        if (obj instanceof PDFFunction) {
            this.functions.add((PDFFunction) obj);
        }
        if (obj instanceof PDFShading) {
            final String shadingName = "Sh" + (++this.shadingCount);
            ((PDFShading) obj).setName(shadingName);
            this.shadings.add((PDFShading) obj);
        }
        if (obj instanceof PDFPattern) {
            final String patternName = "Pa" + (++this.patternCount);
            ((PDFPattern) obj).setName(patternName);
            this.patterns.add((PDFPattern) obj);
        }
        if (obj instanceof PDFFont) {
            final PDFFont font = (PDFFont) obj;
            this.fontMap.put(font.getName(), font);
        }
        if (obj instanceof PDFGState) {
            this.gstates.add((PDFGState) obj);
        }
        if (obj instanceof PDFPage) {
            this.pages.notifyKidRegistered((PDFPage) obj);
        }
        if (obj instanceof PDFLaunch) {
            this.launches.add((PDFLaunch) obj);
        }
        if (obj instanceof PDFLink) {
            this.links.add((PDFLink) obj);
        }
        if (obj instanceof PDFFileSpec) {
            this.filespecs.add((PDFFileSpec) obj);
        }
        if (obj instanceof PDFGoToRemote) {
            this.gotoremotes.add((PDFGoToRemote) obj);
        }
    }

    /**
     * Add trailer object. Adds an object to the list of trailer objects.
     *
     * @param obj
     *            the PDF object to add
     */
    public void addTrailerObject(final PDFObject obj) {
        this.trailerObjects.add(obj);

        if (obj instanceof PDFGoTo) {
            this.gotos.add((PDFGoTo) obj);
        }
    }

    /**
     * Apply the encryption filter to a PDFStream if encryption is enabled.
     *
     * @param stream
     *            PDFStream to encrypt
     */
    public void applyEncryption(final AbstractPDFStream stream) {
        if (isEncryptionActive()) {
            this.encryption.applyFilter(stream);
        }
    }

    /**
     * Enables PDF encryption.
     *
     * @param params
     *            The encryption parameters for the pdf file
     */
    public void setEncryption(final PDFEncryptionParams params) {
        getProfile().verifyEncryptionAllowed();
        this.fileIDGenerator = FileIDGenerator.getRandomFileIDGenerator();
        this.encryption = PDFEncryptionManager.newInstance(++this.objectcount,
                params, this);
        if (this.encryption != null) {
            final PDFObject pdfObject = (PDFObject) this.encryption;
            addTrailerObject(pdfObject);
        } else {
            log.warn("PDF encryption is unavailable. PDF will be "
                    + "generated without encryption.");
        }
    }

    /**
     * Indicates whether encryption is active for this PDF or not.
     *
     * @return boolean True if encryption is active
     */
    public boolean isEncryptionActive() {
        return this.encryption != null;
    }

    /**
     * Returns the active Encryption object.
     *
     * @return the Encryption object
     */
    public PDFEncryption getEncryption() {
        return this.encryption;
    }

    private Object findPDFObject(final List<? extends PDFObject> list,
            final PDFObject compare) {
        for (final PDFObject obj : list) {
            if (compare.contentEquals(obj)) {
                return obj;
            }
        }
        return null;
    }

    /**
     * Looks through the registered functions to see if one that is equal to a
     * reference object exists
     *
     * @param compare
     *            reference object
     * @return the function if it was found, null otherwise
     */
    protected PDFFunction findFunction(final PDFFunction compare) {
        return (PDFFunction) findPDFObject(this.functions, compare);
    }

    /**
     * Looks through the registered shadings to see if one that is equal to a
     * reference object exists
     *
     * @param compare
     *            reference object
     * @return the shading if it was found, null otherwise
     */
    protected PDFShading findShading(final PDFShading compare) {
        return (PDFShading) findPDFObject(this.shadings, compare);
    }

    /**
     * Find a previous pattern. The problem with this is for tiling patterns the
     * pattern data stream is stored and may use up memory, usually this would
     * only be a small amount of data.
     *
     * @param compare
     *            reference object
     * @return the shading if it was found, null otherwise
     */
    protected PDFPattern findPattern(final PDFPattern compare) {
        return (PDFPattern) findPDFObject(this.patterns, compare);
    }

    /**
     * Finds a font.
     *
     * @param fontname
     *            name of the font
     * @return PDFFont the requested font, null if it wasn't found
     */
    protected PDFFont findFont(final String fontname) {
        return this.fontMap.get(fontname);
    }

    /**
     * Finds a named destination.
     *
     * @param compare
     *            reference object to use as search template
     * @return the link if found, null otherwise
     */
    protected PDFDestination findDestination(final PDFDestination compare) {
        final int index = getDestinationList().indexOf(compare);
        if (index >= 0) {
            return getDestinationList().get(index);
        } else {
            return null;
        }
    }

    /**
     * Finds a link.
     *
     * @param compare
     *            reference object to use as search template
     * @return the link if found, null otherwise
     */
    protected PDFLink findLink(final PDFLink compare) {
        return (PDFLink) findPDFObject(this.links, compare);
    }

    /**
     * Finds a file spec.
     *
     * @param compare
     *            reference object to use as search template
     * @return the file spec if found, null otherwise
     */
    protected PDFFileSpec findFileSpec(final PDFFileSpec compare) {
        return (PDFFileSpec) findPDFObject(this.filespecs, compare);
    }

    /**
     * Finds a goto remote.
     *
     * @param compare
     *            reference object to use as search template
     * @return the goto remote if found, null otherwise
     */
    protected PDFGoToRemote findGoToRemote(final PDFGoToRemote compare) {
        return (PDFGoToRemote) findPDFObject(this.gotoremotes, compare);
    }

    /**
     * Finds a goto.
     *
     * @param compare
     *            reference object to use as search template
     * @return the goto if found, null otherwise
     */
    protected PDFGoTo findGoTo(final PDFGoTo compare) {
        return (PDFGoTo) findPDFObject(this.gotos, compare);
    }

    /**
     * Finds a launch.
     *
     * @param compare
     *            reference object to use as search template
     * @return the launch if found, null otherwise
     */
    protected PDFLaunch findLaunch(final PDFLaunch compare) {
        return (PDFLaunch) findPDFObject(this.launches, compare);
    }

    /**
     * Looks for an existing GState to use
     *
     * @param wanted
     *            requested features
     * @param current
     *            currently active features
     * @return the GState if found, null otherwise
     */
    protected PDFGState findGState(final PDFGState wanted,
            final PDFGState current) {
        PDFGState poss;
        final Iterator<PDFGState> iter = this.gstates.iterator();
        while (iter.hasNext()) {
            final PDFGState avail = iter.next();
            poss = new PDFGState();
            poss.addValues(current);
            poss.addValues(avail);
            if (poss.equals(wanted)) {
                return avail;
            }
        }
        return null;
    }

    /**
     * Returns the PDF color space object.
     *
     * @return the color space
     */
    public PDFDeviceColorSpace getPDFColorSpace() {
        return this.colorspace;
    }

    /**
     * Returns the color space.
     *
     * @return the color space
     */
    public int getColorSpace() {
        return getPDFColorSpace().getColorSpace();
    }

    /**
     * Set the color space. This is used when creating gradients.
     *
     * @param theColorspace
     *            the new color space
     */
    public void setColorSpace(final int theColorspace) {
        this.colorspace.setColorSpace(theColorspace);
    }

    /**
     * Returns the font map for this document.
     *
     * @return the map of fonts used in this document
     */
    public Map<String, PDFFont> getFontMap() {
        return this.fontMap;
    }

    /**
     * Resolve a URI.
     *
     * @param uri
     *            the uri to resolve
     * @throws java.io.FileNotFoundException
     *             if the URI could not be resolved
     * @return the InputStream from the URI.
     */
    protected InputStream resolveURI(final String uri)
            throws java.io.FileNotFoundException {
        try {
            /* TODO: Temporary hack to compile, improve later */
            return new java.net.URL(uri).openStream();
        } catch (final Exception e) {
            throw new java.io.FileNotFoundException(
                    "URI could not be resolved (" + e.getMessage() + "): "
                            + uri);
        }
    }

    /**
     * Get an image from the image map.
     *
     * @param key
     *            the image key to look for
     * @return the image or PDFXObject for the key if found
     * @deprecated Use getXObject instead (so forms are treated in the same way)
     */
    @Deprecated
    public PDFImageXObject getImage(final String key) {
        return (PDFImageXObject) this.xObjectsMap.get(key);
    }

    /**
     * Get an XObject from the image map.
     *
     * @param key
     *            the XObject key to look for
     * @return the PDFXObject for the key if found
     */
    public PDFXObject getXObject(final String key) {
        return this.xObjectsMap.get(key);
    }

    /**
     * Adds a destination to the document.
     *
     * @param destination
     *            the destination object
     */
    public void addDestination(final PDFDestination destination) {
        if (this.destinations == null) {
            this.destinations = new ArrayList<>();
        }
        this.destinations.add(destination);
    }

    /**
     * Gets the list of named destinations.
     *
     * @return the list of named destinations.
     */
    public List<PDFDestination> getDestinationList() {
        if (hasDestinations()) {
            return this.destinations;
        } else {
            return Collections.emptyList();
        }
    }

    /**
     * Gets whether the document has named destinations.
     *
     * @return whether the document has named destinations.
     */
    public boolean hasDestinations() {
        return this.destinations != null && !this.destinations.isEmpty();
    }

    /**
     * Add an image to the PDF document. This adds an image to the PDF objects.
     * If an image with the same key already exists it will return the old
     * {@link PDFXObject}.
     *
     * @param res
     *            the PDF resource context to add to, may be null
     * @param img
     *            the PDF image to add
     * @return the PDF XObject that references the PDF image data
     */
    public PDFImageXObject addImage(final PDFResourceContext res,
            final PDFImage img) {
        // check if already created
        final String key = img.getKey();
        PDFImageXObject xObject = (PDFImageXObject) this.xObjectsMap.get(key);
        if (xObject != null) {
            if (res != null) {
                res.getPDFResources().addXObject(xObject);
            }
            return xObject;
        }

        // setup image
        img.setup(this);
        // create a new XObject
        xObject = new PDFImageXObject(++this.xObjectCount, img);
        registerObject(xObject);
        this.resources.addXObject(xObject);
        if (res != null) {
            res.getPDFResources().addXObject(xObject);
        }
        this.xObjectsMap.put(key, xObject);
        return xObject;
    }

    /**
     * Add a form XObject to the PDF document. This adds a Form XObject to the
     * PDF objects. If a Form XObject with the same key already exists it will
     * return the old {@link PDFFormXObject}.
     *
     * @param res
     *            the PDF resource context to add to, may be null
     * @param cont
     *            the PDF Stream contents of the Form XObject
     * @param formres
     *            a reference to the PDF Resources for the Form XObject data
     * @param key
     *            the key for the object
     * @return the PDF Form XObject that references the PDF data
     */
    public PDFFormXObject addFormXObject(final PDFResourceContext res,
            final PDFStream cont, final PDFReference formres, final String key) {

        // check if already created
        PDFFormXObject xObject = (PDFFormXObject) this.xObjectsMap.get(key);
        if (xObject != null) {
            if (res != null) {
                res.getPDFResources().addXObject(xObject);
            }
            return xObject;
        }

        xObject = new PDFFormXObject(++this.xObjectCount, cont, formres);
        registerObject(xObject);
        this.resources.addXObject(xObject);
        if (res != null) {
            res.getPDFResources().addXObject(xObject);
        }
        this.xObjectsMap.put(key, xObject);
        return xObject;
    }

    /**
     * Get the root Outlines object. This method does not write the outline to
     * the PDF document, it simply creates a reference for later.
     *
     * @return the PDF Outline root object
     */
    public PDFOutline getOutlineRoot() {
        if (this.outlineRoot != null) {
            return this.outlineRoot;
        }

        this.outlineRoot = new PDFOutline(null, null, true);
        assignObjectNumber(this.outlineRoot);
        addTrailerObject(this.outlineRoot);
        this.root.setRootOutline(this.outlineRoot);
        return this.outlineRoot;
    }

    /**
     * Get the /Resources object for the document
     *
     * @return the /Resources object
     */
    public PDFResources getResources() {
        return this.resources;
    }

    public void enableAccessibility(final boolean enableAccessibility) {
        this.accessibilityEnabled = enableAccessibility;
    }

    /**
     * Writes out the entire document
     *
     * @param stream
     *            the OutputStream to output the document to
     * @throws IOException
     *             if there is an exception writing to the output stream
     */
    public void output(final OutputStream stream) throws IOException {
        // Write out objects until the list is empty. This approach (used with a
        // LinkedList) allows for output() methods to create and register
        // objects
        // on the fly even during serialization.
        while (this.objects.size() > 0) {
            final PDFObject object = this.objects.remove(0);
            streamIndirectObject(object, stream);
        }
    }

    private void streamIndirectObject(final PDFObject o,
            final OutputStream stream) throws IOException {
        recordObjectOffset(o);
        this.position += outputIndirectObject(o, stream);
    }

    private void streamIndirectObjects(
            final Collection<? extends PDFObject> objects,
            final OutputStream stream) throws IOException {
        for (final PDFObject o : objects) {
            streamIndirectObject(o, stream);
        }
    }

    private void recordObjectOffset(final PDFObject object) {
        final int index = object.getObjectNumber() - 1;
        while (this.indirectObjectOffsets.size() <= index) {
            this.indirectObjectOffsets.add(null);
        }
        this.indirectObjectOffsets.set(index, this.position);
    }

    /**
     * Outputs the given object, wrapped by obj/endobj, to the given stream.
     *
     * @param object
     *            an indirect object, as described in Section 3.2.9 of the PDF
     *            1.5 Reference.
     * @param stream
     *            the stream to which the object must be output
     * @throws IllegalArgumentException
     *             if the object is not an indirect object
     */
    public static int outputIndirectObject(final PDFObject object,
            final OutputStream stream) throws IOException {
        if (!object.hasObjectNumber()) {
            throw new IllegalArgumentException("Not an indirect object");
        }
        final byte[] obj = encode(object.getObjectID());
        stream.write(obj);
        final int length = object.output(stream);
        final byte[] endobj = encode("\nendobj\n");
        stream.write(endobj);
        return obj.length + length + endobj.length;
    }

    /**
     * Write the PDF header.
     *
     * This method must be called prior to formatting and outputting AreaTrees.
     *
     * @param stream
     *            the OutputStream to write the header to
     * @throws IOException
     *             if there is an exception writing to the output stream
     */
    public void outputHeader(final OutputStream stream) throws IOException {
        this.position = 0;

        getProfile().verifyPDFVersion();

        final byte[] pdf = encode("%PDF-" + getPDFVersionString() + "\n");
        stream.write(pdf);
        this.position += pdf.length;

        // output a binary comment as recommended by the PDF spec (3.4.1)
        final byte[] bin = { (byte) '%', (byte) 0xAA, (byte) 0xAB, (byte) 0xAC,
                (byte) 0xAD, (byte) '\n' };
        stream.write(bin);
        this.position += bin.length;
    }

    /**
     * Write the trailer
     *
     * @param stream
     *            the OutputStream to write the trailer to
     * @throws IOException
     *             if there is an exception writing to the output stream
     */
    public void outputTrailer(final OutputStream stream) throws IOException {
        createDestinations();
        output(stream);
        outputTrailerObjectsAndXref(stream);
    }

    private void createDestinations() {
        if (hasDestinations()) {
            Collections.sort(this.destinations, new DestinationComparator());
            final PDFDests dests = getFactory().makeDests(this.destinations);
            if (this.root.getNames() == null) {
                this.root.setNames(getFactory().makeNames());
            }
            this.root.getNames().setDests(dests);
        }
    }

    private void outputTrailerObjectsAndXref(final OutputStream stream)
            throws IOException {
        final TrailerOutputHelper trailerOutputHelper = mayCompressStructureTreeElements() ? new CompressedTrailerOutputHelper()
                : new UncompressedTrailerOutputHelper();
        if (this.structureTreeElements != null) {
            trailerOutputHelper.outputStructureTreeElements(stream);
        }
        streamIndirectObjects(this.trailerObjects, stream);
        final TrailerDictionary trailerDictionary = createTrailerDictionary();
        final long startxref = trailerOutputHelper.outputCrossReferenceObject(
                stream, trailerDictionary);
        final String trailer = "startxref\n" + startxref + "\n%%EOF\n";
        stream.write(encode(trailer));
    }

    private boolean mayCompressStructureTreeElements() {
        return this.accessibilityEnabled
                && this.versionController.getPDFVersion().compareTo(
                        Version.V1_5) >= 0;
    }

    private TrailerDictionary createTrailerDictionary() {
        final FileIDGenerator gen = getFileIDGenerator();
        final TrailerDictionary trailerDictionary = new TrailerDictionary(this)
                .setRoot(this.root).setInfo(this.info)
                .setFileID(gen.getOriginalFileID(), gen.getUpdatedFileID());
        if (isEncryptionActive()) {
            trailerDictionary.setEncryption(this.encryption);
        }
        return trailerDictionary;
    }

    private interface TrailerOutputHelper {

        void outputStructureTreeElements(final OutputStream stream)
                throws IOException;

        /**
         * @return the offset of the cross-reference object (the value of
         *         startxref)
         */
        long outputCrossReferenceObject(final OutputStream stream,
                final TrailerDictionary trailerDictionary) throws IOException;
    }

    private class UncompressedTrailerOutputHelper implements
    TrailerOutputHelper {

        @Override
        public void outputStructureTreeElements(final OutputStream stream)
                throws IOException {
            streamIndirectObjects(PDFDocument.this.structureTreeElements,
                    stream);
        }

        @Override
        public long outputCrossReferenceObject(final OutputStream stream,
                final TrailerDictionary trailerDictionary) throws IOException {
            new CrossReferenceTable(trailerDictionary,
                    PDFDocument.this.position,
                    PDFDocument.this.indirectObjectOffsets).output(stream);
            return PDFDocument.this.position;
        }
    }

    private class CompressedTrailerOutputHelper implements TrailerOutputHelper {

        private ObjectStreamManager structureTreeObjectStreams;

        @Override
        public void outputStructureTreeElements(final OutputStream stream)
                throws IOException {
            assert PDFDocument.this.structureTreeElements.size() > 0;
            this.structureTreeObjectStreams = new ObjectStreamManager(
                    PDFDocument.this);
            for (final PDFStructElem structElem : PDFDocument.this.structureTreeElements) {
                this.structureTreeObjectStreams.add(structElem);
            }
        }

        @Override
        public long outputCrossReferenceObject(final OutputStream stream,
                final TrailerDictionary trailerDictionary) throws IOException {
            // Outputting the object streams should not have created new
            // indirect objects
            assert PDFDocument.this.objects.isEmpty();
            new CrossReferenceStream(PDFDocument.this,
                    ++PDFDocument.this.objectcount, trailerDictionary,
                    PDFDocument.this.position,
                    PDFDocument.this.indirectObjectOffsets,
                    this.structureTreeObjectStreams
                    .getCompressedObjectReferences()).output(stream);
            return PDFDocument.this.position;
        }
    }

    long getCurrentFileSize() {
        return this.position;
    }

    FileIDGenerator getFileIDGenerator() {
        if (this.fileIDGenerator == null) {
            try {
                this.fileIDGenerator = FileIDGenerator
                        .getDigestFileIDGenerator(this);
            } catch (final NoSuchAlgorithmException e) {
                this.fileIDGenerator = FileIDGenerator
                        .getRandomFileIDGenerator();
            }
        }
        return this.fileIDGenerator;
    }
}
