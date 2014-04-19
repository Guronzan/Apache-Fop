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

/* $Id: AbstractTripletStructuredObject.java 828678 2009-10-22 13:20:53Z acumiskey $ */

package org.apache.fop.afp.modca;

import java.io.IOException;
import java.io.OutputStream;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;

import lombok.extern.slf4j.Slf4j;

import org.apache.fop.afp.modca.Registry.ObjectType;
import org.apache.fop.afp.modca.triplets.AbstractTriplet;
import org.apache.fop.afp.modca.triplets.CommentTriplet;
import org.apache.fop.afp.modca.triplets.FullyQualifiedNameTriplet;
import org.apache.fop.afp.modca.triplets.ObjectClassificationTriplet;
import org.apache.fop.afp.modca.triplets.Triplet;

/**
 * A MODCA structured object base class providing support for Triplets
 */
@Slf4j
public class AbstractTripletStructuredObject extends AbstractStructuredObject {

    /** list of object triplets */
    protected List/* <Triplet> */triplets = new java.util.ArrayList/* <Triplet> */();

    /**
     * Returns the triplet data length
     *
     * @return the triplet data length
     */
    protected int getTripletDataLength() {
        int dataLength = 0;
        if (hasTriplets()) {
            final Iterator it = this.triplets.iterator();
            while (it.hasNext()) {
                final AbstractTriplet triplet = (AbstractTriplet) it.next();
                dataLength += triplet.getDataLength();
            }
        }
        return dataLength;
    }

    /**
     * Returns true when this structured field contains triplets
     *
     * @return true when this structured field contains triplets
     */
    public boolean hasTriplets() {
        return this.triplets.size() > 0;
    }

    /**
     * Writes any triplet data
     *
     * @param os
     *            The stream to write to
     * @throws IOException
     *             The stream to write to
     */
    protected void writeTriplets(final OutputStream os) throws IOException {
        if (hasTriplets()) {
            writeObjects(this.triplets, os);
            this.triplets = null; // gc
        }
    }

    /**
     * Returns the first matching triplet found in the structured field triplet
     * list
     *
     * @param tripletId
     *            the triplet identifier
     */
    private AbstractTriplet getTriplet(final byte tripletId) {
        final Iterator it = getTriplets().iterator();
        while (it.hasNext()) {
            final AbstractTriplet triplet = (AbstractTriplet) it.next();
            if (triplet.getId() == tripletId) {
                return triplet;
            }
        }
        return null;
    }

    /**
     * Returns true of this structured field has the given triplet
     *
     * @param tripletId
     *            the triplet identifier
     * @return true if the structured field has the given triplet
     */
    public boolean hasTriplet(final byte tripletId) {
        return getTriplet(tripletId) != null;
    }

    /**
     * Adds a triplet to this structured object
     *
     * @param triplet
     *            the triplet to add
     */
    protected void addTriplet(final Triplet triplet) {
        this.triplets.add(triplet);
    }

    /**
     * Adds a list of triplets to the triplets contained within this structured
     * field
     *
     * @param tripletCollection
     *            a collection of triplets
     */
    public void addTriplets(final Collection/* <Triplet> */tripletCollection) {
        if (tripletCollection != null) {
            this.triplets.addAll(tripletCollection);
        }
    }

    /** @return the triplet list pertaining to this resource */
    protected List/* <Triplet> */getTriplets() {
        return this.triplets;
    }

    /**
     * Sets the fully qualified name of this structured field
     *
     * @param fqnType
     *            the fully qualified name type of this resource
     * @param fqnFormat
     *            the fully qualified name format of this resource
     * @param fqName
     *            the fully qualified name of this resource
     */
    public void setFullyQualifiedName(final byte fqnType, final byte fqnFormat,
            final String fqName) {
        addTriplet(new FullyQualifiedNameTriplet(fqnType, fqnFormat, fqName));
    }

    /**
     * @return the fully qualified name of this triplet or null if it does not
     *         exist
     */
    public String getFullyQualifiedName() {
        final FullyQualifiedNameTriplet fqNameTriplet = (FullyQualifiedNameTriplet) getTriplet(Triplet.FULLY_QUALIFIED_NAME);
        if (fqNameTriplet != null) {
            return fqNameTriplet.getFullyQualifiedName();
        }
        log.warn(this + " has no fully qualified name");
        return null;
    }

    /**
     * Sets the objects classification
     *
     * @param objectClass
     *            the classification of the object
     * @param objectType
     *            the MOD:CA registry object type entry for the given
     *            object/component type of the object
     * @param dataInContainer
     *            whether the data resides in the container
     * @param containerHasOEG
     *            whether the container has an object environment group
     * @param dataInOCD
     *            whether the data resides in a object container data structured
     *            field
     */
    public void setObjectClassification(final byte objectClass,
            final ObjectType objectType, final boolean dataInContainer,
            final boolean containerHasOEG, final boolean dataInOCD) {
        addTriplet(new ObjectClassificationTriplet(objectClass, objectType,
                dataInContainer, containerHasOEG, dataInOCD));
    }

    /**
     * Sets a comment on this resource
     *
     * @param commentString
     *            a comment string
     */
    public void setComment(final String commentString) {
        addTriplet(new CommentTriplet(Triplet.COMMENT, commentString));
    }

}
