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

/* $Id: RtfGenerator.java 679326 2008-07-24 09:35:34Z vhennebert $ */

package org.apache.fop.render.rtf.rtflib.rtfdoc;

import java.io.IOException;
import java.io.Writer;

/**
 * Represents a generator element which says who generated the RTF document.
 */
public class RtfGenerator extends RtfElement {

    /** Default constructor for the generator element. */
    public RtfGenerator(final RtfHeader h, final Writer w) throws IOException {
        super(h, w);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void writeRtfContent() throws IOException {
        newLine();
        writeGroupMark(true);
        writeStarControlWord("generator");
        this.writer.write("Apache XML Graphics RTF Library");
        this.writer.write(";");
        writeGroupMark(false);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isEmpty() {
        return false;
    }

}
