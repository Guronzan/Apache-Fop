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

/* $Id: IFGraphicContext.java 746664 2009-02-22 12:40:44Z jeremias $ */

package org.apache.fop.render.intermediate;

import java.awt.Dimension;
import java.awt.Rectangle;
import java.awt.geom.AffineTransform;
import java.util.ArrayList;
import java.util.List;

import org.apache.xmlgraphics.java2d.GraphicContext;

/**
 * Specialized graphic context class for the intermediate format renderer.
 */
public class IFGraphicContext extends GraphicContext {

    private static final AffineTransform[] EMPTY_TRANSFORM_ARRAY = new AffineTransform[0];

    private final List<Group> groupList = new ArrayList<>();

    /**
     * Default constructor.
     */
    public IFGraphicContext() {
        super();
    }

    /**
     * Copy constructor.
     *
     * @param graphicContext
     *            the graphic context to make a copy of
     */
    protected IFGraphicContext(final IFGraphicContext graphicContext) {
        super(graphicContext);
        // We don't clone groupDepth!
    }

    /** {@inheritDoc} */
    @Override
    public Object clone() {
        return new IFGraphicContext(this);
    }

    public void pushGroup(final Group group) {
        // this.groupDepth++;
        this.groupList.add(group);
        for (final AffineTransform transform : group.getTransforms()) {
            transform(transform);
        }
    }

    public Group[] getGroups() {
        return this.groupList.toArray(new Group[getGroupStackSize()]);
    }

    public Group[] dropGroups() {
        final Group[] groups = getGroups();
        this.groupList.clear();
        return groups;
    }

    public int getGroupStackSize() {
        return this.groupList.size();
    }

    public static class Group {

        private final AffineTransform[] transforms;

        public Group(final AffineTransform[] transforms) {
            this.transforms = transforms;
        }

        public Group(final AffineTransform transform) {
            this(new AffineTransform[] { transform });
        }

        public Group() {
            this(EMPTY_TRANSFORM_ARRAY);
        }

        public AffineTransform[] getTransforms() {
            return this.transforms;
        }

        public void start(final IFPainter painter) throws IFException {
            painter.startGroup(this.transforms);
        }

        public void end(final IFPainter painter) throws IFException {
            painter.endGroup();
        }

        /** {@inheritDoc} */
        @Override
        public String toString() {
            final StringBuilder sb = new StringBuilder("group: ");
            IFUtil.toString(getTransforms(), sb);
            return sb.toString();
        }

    }

    public static class Viewport extends Group {

        private final Dimension size;
        private final Rectangle clipRect;

        public Viewport(final AffineTransform[] transforms,
                final Dimension size, final Rectangle clipRect) {
            super(transforms);
            this.size = size;
            this.clipRect = clipRect;
        }

        public Viewport(final AffineTransform transform, final Dimension size,
                final Rectangle clipRect) {
            this(new AffineTransform[] { transform }, size, clipRect);
        }

        public Dimension getSize() {
            return this.size;
        }

        public Rectangle getClipRect() {
            return this.clipRect;
        }

        @Override
        public void start(final IFPainter painter) throws IFException {
            painter.startViewport(getTransforms(), this.size, this.clipRect);
        }

        @Override
        public void end(final IFPainter painter) throws IFException {
            painter.endViewport();
        }

        /** {@inheritDoc} */
        @Override
        public String toString() {
            final StringBuilder sb = new StringBuilder("viewport: ");
            IFUtil.toString(getTransforms(), sb);
            sb.append(", ").append(getSize());
            if (getClipRect() != null) {
                sb.append(", ").append(getClipRect());
            }
            return sb.toString();
        }

    }

}
