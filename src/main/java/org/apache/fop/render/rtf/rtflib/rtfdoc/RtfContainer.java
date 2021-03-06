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

/* $Id: RtfContainer.java 679326 2008-07-24 09:35:34Z vhennebert $ */

package org.apache.fop.render.rtf.rtflib.rtfdoc;

/*
 * This file is part of the RTF library of the FOP project, which was originally
 * created by Bertrand Delacretaz <bdelacretaz@codeconsult.ch> and by other
 * contributors to the jfor project (www.jfor.org), who agreed to donate jfor to
 * the FOP project.
 */

import java.io.IOException;
import java.io.Writer;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import org.apache.fop.render.rtf.rtflib.exceptions.RtfStructureException;

/**
 * An RtfElement that can contain other elements.
 * 
 * @author Bertrand Delacretaz bdelacretaz@codeconsult.ch
 */

public class RtfContainer extends RtfElement {
    private LinkedList children; // 'final' removed by Boris Poudérous on
                                 // 07/22/2002
    private RtfOptions options = new RtfOptions();
    private RtfElement lastChild;

    /** Create an RTF container as a child of given container */
    RtfContainer(final RtfContainer parent, final Writer w) throws IOException {
        this(parent, w, null);
    }

    /**
     * Create an RTF container as a child of given container with given
     * attributes
     */
    RtfContainer(final RtfContainer parent, final Writer w,
            final RtfAttributes attr) throws IOException {
        super(parent, w, attr);
        this.children = new LinkedList();
    }

    /**
     * set options
     * 
     * @param opt
     *            options to set
     */
    public void setOptions(final RtfOptions opt) {
        this.options = opt;
    }

    /**
     * add a child element to this
     * 
     * @param e
     *            child element to add
     * @throws RtfStructureException
     *             for trying to add an invalid child (??)
     */
    protected void addChild(final RtfElement e) throws RtfStructureException {
        if (isClosed()) {
            // No childs should be added to a container that has been closed
            final StringBuilder sb = new StringBuilder();
            sb.append("addChild: container already closed (parent=");
            sb.append(this.getClass().getName());
            sb.append(" child=");
            sb.append(e.getClass().getName());
            sb.append(")");
            final String msg = sb.toString();

            // warn of this problem
            final RtfFile rf = getRtfFile();
            // if(rf.getLog() != null) {
            // rf.getLog().logWarning(msg);
            // }

            // TODO this should be activated to help detect XSL-FO constructs
            // that we do not handle properly.
            /*
             * throw new RtfStructureException(msg);
             */
        }

        this.children.add(e);
        this.lastChild = e;
    }

    /**
     * @return a copy of our children's list
     */
    public List getChildren() {
        return (List) this.children.clone();
    }

    /**
     * @return the number of children
     */
    public int getChildCount() {
        return this.children.size();
    }

    /**
     * Add by Boris Poudérous on 07/22/2002 Set the children list
     * 
     * @param list
     *            list of child objects
     * @return true if process succeeded
     */
    public boolean setChildren(final List list) {
        if (list instanceof LinkedList) {
            this.children = (LinkedList) list;
            return true;
        }

        return false;
    }

    /**
     * write RTF code of all our children
     * 
     * @throws IOException
     *             for I/O problems
     */
    @Override
    protected void writeRtfContent() throws IOException {
        for (final Iterator it = this.children.iterator(); it.hasNext();) {
            final RtfElement e = (RtfElement) it.next();
            e.writeRtf();
        }
    }

    /** return our options */
    RtfOptions getOptions() {
        return this.options;
    }

    /** true if this (recursively) contains at least one RtfText object */
    boolean containsText() {
        boolean result = false;
        for (final Iterator it = this.children.iterator(); it.hasNext();) {
            final RtfElement e = (RtfElement) it.next();
            if (e instanceof RtfText) {
                result = !e.isEmpty();
            } else if (e instanceof RtfContainer) {
                if (((RtfContainer) e).containsText()) {
                    result = true;
                }
            }
            if (result) {
                break;
            }
        }
        return result;
    }

    /** debugging to given Writer */
    @Override
    void dump(final Writer w, final int indent) throws IOException {
        super.dump(w, indent);
        for (final Iterator it = this.children.iterator(); it.hasNext();) {
            final RtfElement e = (RtfElement) it.next();
            e.dump(w, indent + 1);
        }
    }

    /**
     * minimal debugging display
     * 
     * @return String representation of object contents
     */
    @Override
    public String toString() {
        return super.toString() + " (" + getChildCount() + " children)";
    }

    /**
     * @return false if empty or if our options block writing
     */
    @Override
    protected boolean okToWriteRtf() {
        boolean result = super.okToWriteRtf() && !isEmpty();
        if (result && !this.options.renderContainer(this)) {
            result = false;
        }
        return result;
    }

    /**
     * @return true if this element would generate no "useful" RTF content, i.e.
     *         (for RtfContainer) true if it has no children where isEmpty() is
     *         false
     */
    @Override
    public boolean isEmpty() {
        boolean result = true;
        for (final Iterator it = this.children.iterator(); it.hasNext();) {
            final RtfElement e = (RtfElement) it.next();
            if (!e.isEmpty()) {
                result = false;
                break;
            }
        }
        return result;
    }
}