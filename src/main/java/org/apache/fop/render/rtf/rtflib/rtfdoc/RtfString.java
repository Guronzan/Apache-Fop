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

/* $Id: RtfString.java 679326 2008-07-24 09:35:34Z vhennebert $ */

/*
 * This file is part of the RTF library of the FOP project, which was originally
 * created by Bertrand Delacretaz <bdelacretaz@codeconsult.ch> and by other
 * contributors to the jfor project (www.jfor.org), who agreed to donate jfor to
 * the FOP project.
 */

package org.apache.fop.render.rtf.rtflib.rtfdoc;

import java.io.IOException;
import java.io.Writer;

/**
 * Plain text in a RTF file, without any formatings.
 * 
 * @author Peter Herweg, pherweg@web.de
 */

public class RtfString extends RtfElement {
    private String text = "";

    RtfString(final RtfContainer parent, final Writer w, final String s)
            throws IOException {
        super(parent, w);

        this.text = s;
    }

    /**
     * @return true if this element would generate no "useful" RTF content
     */
    @Override
    public boolean isEmpty() {
        return this.text.trim().equals("");
    }

    /**
     * write RTF code of all our children
     * 
     * @throws IOException
     *             for I/O problems
     */
    @Override
    protected void writeRtfContent() throws IOException {
        RtfStringConverter.getInstance().writeRtfString(this.writer, this.text);
    }

    public String getText() {
        return this.text;
    }

    public void setText(final String s) {
        this.text = s;
    }
}