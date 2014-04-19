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

/* $Id: RtfBefore.java 679326 2008-07-24 09:35:34Z vhennebert $ */

package org.apache.fop.render.rtf.rtflib.rtfdoc;

/*
 * This file is part of the RTF library of the FOP project, which was originally
 * created by Bertrand Delacretaz <bdelacretaz@codeconsult.ch> and by other
 * contributors to the jfor project (www.jfor.org), who agreed to donate jfor to
 * the FOP project.
 */

import java.io.IOException;
import java.io.Writer;

/** The opposite of RtfAfter */
public class RtfBefore extends RtfAfterBeforeBase {
    /** RtfBefore attributes */
    public static final String HEADER = "header";

    /** String array of attribute names */
    public static final String[] HEADER_ATTR = new String[] { HEADER };

    RtfBefore(final RtfSection parent, final Writer w, final RtfAttributes attrs)
            throws IOException {
        super(parent, w, attrs);
    }

    /**
     * Write the attributes for this element
     * 
     * @throws IOException
     *             for I/O problems
     */
    @Override
    protected void writeMyAttributes() throws IOException {
        writeAttributes(this.attrib, HEADER_ATTR);
    }
}