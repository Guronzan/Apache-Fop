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

/* $Id: ObjectEnvironmentGroup.java 746664 2009-02-22 12:40:44Z jeremias $ */

package org.apache.fop.afp.modca;

import java.io.IOException;
import java.io.OutputStream;

import org.apache.fop.afp.util.BinaryUtils;

/**
 * An Object Environment Group (OEG) may be associated with an object and is
 * contained within the object's begin-end envelope. The object environment
 * group defines the object's origin and orientation on the page, and can
 * contain font and color attribute table information. The scope of an object
 * environment group is the scope of its containing object.
 *
 * An application that creates a data-stream document may omit some of the
 * parameters normally contained in the object environment group, or it may
 * specify that one or more default values are to be used.
 */
public final class ObjectEnvironmentGroup extends AbstractNamedAFPObject {

    /** the PresentationEnvironmentControl for the object environment group */
    private PresentationEnvironmentControl presentationEnvironmentControl;

    /** the ObjectAreaDescriptor for the object environment group */
    private ObjectAreaDescriptor objectAreaDescriptor;

    /** the ObjectAreaPosition for the object environment group */
    private ObjectAreaPosition objectAreaPosition;

    /** the MapImageObject for the object environment group (optional) */
    private MapImageObject mapImageObject;

    /** the DataDescriptor for the object environment group */
    private AbstractDescriptor dataDescriptor;

    /** the MapDataResource for the object environment group */
    private MapDataResource mapDataResource;

    /** the MapContainerData for the object environment group */
    private MapContainerData mapContainerData;

    /**
     * Constructor for the ObjectEnvironmentGroup, this takes a name parameter
     * which must be 8 characters long.
     *
     * @param name
     *            the object environment group name
     */
    public ObjectEnvironmentGroup(final String name) {
        super(name);
    }

    /**
     * Sets the Object Area Descriptor
     *
     * @param objectAreaDescriptor
     *            the object area descriptor
     */
    public void setObjectAreaDescriptor(
            final ObjectAreaDescriptor objectAreaDescriptor) {
        this.objectAreaDescriptor = objectAreaDescriptor;
    }

    /**
     * Sets the Object Area Position
     *
     * @param objectAreaPosition
     *            the object area position
     */
    public void setObjectAreaPosition(
            final ObjectAreaPosition objectAreaPosition) {
        this.objectAreaPosition = objectAreaPosition;
    }

    /**
     * Sets the Map Image Object (MIO).
     *
     * @param mapImageObject
     *            the MIO structured field
     */
    public void setMapImageObject(final MapImageObject mapImageObject) {
        this.mapImageObject = mapImageObject;
    }

    /** {@inheritDoc} */
    @Override
    protected void writeStart(final OutputStream os) throws IOException {
        final byte[] data = new byte[17];
        copySF(data, Type.BEGIN, Category.OBJECT_ENVIRONMENT_GROUP);

        final int tripletDataLength = getTripletDataLength();
        final int sfLen = data.length + tripletDataLength - 1;
        final byte[] len = BinaryUtils.convert(sfLen, 2);
        data[1] = len[0];
        data[2] = len[1];

        os.write(data);

        writeTriplets(os);
    }

    /** {@inheritDoc} */
    @Override
    protected void writeContent(final OutputStream os) throws IOException {
        super.writeContent(os);

        if (this.presentationEnvironmentControl != null) {
            this.presentationEnvironmentControl.writeToStream(os);
        }

        if (this.objectAreaDescriptor != null) {
            this.objectAreaDescriptor.writeToStream(os);
        }

        if (this.objectAreaPosition != null) {
            this.objectAreaPosition.writeToStream(os);
        }

        if (this.mapImageObject != null) {
            this.mapImageObject.writeToStream(os);
        }

        if (this.mapContainerData != null) {
            this.mapContainerData.writeToStream(os);
        }

        if (this.mapDataResource != null) {
            this.mapDataResource.writeToStream(os);
        }

        if (this.dataDescriptor != null) {
            this.dataDescriptor.writeToStream(os);
        }
    }

    /** {@inheritDoc} */
    @Override
    protected void writeEnd(final OutputStream os) throws IOException {
        final byte[] data = new byte[17];
        copySF(data, Type.END, Category.OBJECT_ENVIRONMENT_GROUP);
        os.write(data);
    }

    /**
     * Sets the presentation environment control
     *
     * @param presentationEnvironmentControl
     *            the presentation environment control
     */
    public void setPresentationEnvironmentControl(
            final PresentationEnvironmentControl presentationEnvironmentControl) {
        this.presentationEnvironmentControl = presentationEnvironmentControl;
    }

    /**
     * Sets the data descriptor
     *
     * @param dataDescriptor
     *            the data descriptor
     */
    public void setDataDescriptor(final AbstractDescriptor dataDescriptor) {
        this.dataDescriptor = dataDescriptor;
    }

    /**
     * Sets the map data resource
     *
     * @param mapDataResource
     *            the map data resource
     */
    public void setMapDataResource(final MapDataResource mapDataResource) {
        this.mapDataResource = mapDataResource;
    }

    /**
     * Sets the map container data
     *
     * @param mapContainerData
     *            the map container data
     */
    public void setMapContainerData(final MapContainerData mapContainerData) {
        this.mapContainerData = mapContainerData;
    }

    /**
     * Returns the object area descriptor
     *
     * @return the object area descriptor
     */
    public ObjectAreaDescriptor getObjectAreaDescriptor() {
        return this.objectAreaDescriptor;
    }

}
