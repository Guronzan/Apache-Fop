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

/* $Id: RtfFootnote.java 679326 2008-07-24 09:35:34Z vhennebert $ */

package org.apache.fop.render.rtf.rtflib.rtfdoc;

//Java
import java.io.IOException;
import java.io.Writer;
//FOP

/**
 * Model of an RTF footnote
 * 
 * @author Peter Herweg, pherweg@web.de
 * @author Marc Wilhelm Kuester
 */
public class RtfFootnote extends RtfContainer implements IRtfTextrunContainer,
        IRtfListContainer {
    RtfTextrun textrunInline = null;
    RtfContainer body = null;
    RtfList list = null;
    boolean bBody = false;

    /**
     * Create an RTF list item as a child of given container with default
     * attributes
     */
    RtfFootnote(final RtfContainer parent, final Writer w) throws IOException {
        super(parent, w);
        this.textrunInline = new RtfTextrun(this, this.writer, null);
        this.body = new RtfContainer(this, this.writer);
    }

    @Override
    public RtfTextrun getTextrun() throws IOException {
        if (this.bBody) {
            final RtfTextrun textrun = RtfTextrun.getTextrun(this.body,
                    this.writer, null);
            textrun.setSuppressLastPar(true);

            return textrun;
        } else {
            return this.textrunInline;
        }
    }

    /**
     * write RTF code of all our children
     * 
     * @throws IOException
     *             for I/O problems
     */
    @Override
    protected void writeRtfContent() throws IOException {
        this.textrunInline.writeRtfContent();

        writeGroupMark(true);
        writeControlWord("footnote");
        writeControlWord("ftnalt");

        this.body.writeRtfContent();

        writeGroupMark(false);
    }

    @Override
    public RtfList newList(final RtfAttributes attrs) throws IOException {
        if (this.list != null) {
            this.list.close();
        }

        this.list = new RtfList(this.body, this.writer, attrs);

        return this.list;
    }

    public void startBody() {
        this.bBody = true;
    }

    public void endBody() {
        this.bBody = false;
    }
}
