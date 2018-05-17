/*
 * Copyright (c) 2012, Codename One and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.  Codename One designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Codename One in the LICENSE file that accompanied this code.
 *  
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 2 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 * 
 * You should have received a copy of the GNU General Public License version
 * 2 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 * 
 * Please contact Codename One through http://www.codenameone.com/ if you 
 * need additional information or have any questions.
 */
package com.codename1.ui.scene;

import com.codename1.properties.Property;
import com.codename1.ui.Component;
import com.codename1.ui.Container;
import com.codename1.ui.Graphics;

import com.codename1.ui.Transform;
import com.codename1.ui.geom.GeneralPath;
import com.codename1.ui.geom.Rectangle;
import com.codename1.ui.geom.Rectangle2D;
import com.codename1.ui.layouts.BorderLayout;
import com.codename1.ui.plaf.Border;
import com.codename1.ui.plaf.Style;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Encapsulates a Node in the scene-graph.  This wraps a component (the "renderer" of the Node) and an associated transform
 * that describes where, in the 3D space of the scene graph the component should be rendered.
 * @author shannah
 * @deprecated For internal use only.
 */
public class Node {
    /**
     * The scene that the node is currently attached to.
     */
    private Scene scene;
    
    /**
     * The parent node.
     */
    private Node parent;
    
    /**
     * Child nodes.
     */
    private List<Node> children;
    
    /**
     * Tags that are attached to this node. These are like CSS classes.
     */
    private HashSet<String> tags;
    
    /**
     * The component that should be rendered inside this node.
     */
    private Component renderer;
    
    /**
     * Private wrapper around the renderer component.  This gives us
     * access to some package-private methods in Component that we need to use
     * for rendering.
     */
    private RenderableContainer rendererCnt;
    
    /**
     * Flag of whether to render the component onto an image buffer before rendering.
     * Components that use 9-piece image borders may show lines between the border 
     * segments if renderered directly onto the scene graph. Setting this flag will
     * resolve that (because it will first render to an image with the identity transform
     * and then transform that image on the Graphics context).
     */
    private boolean renderAsImage=false;
    
    public final Property<Double,Node> 
            /**
             * The scale to apply to the node, along the x-axis.
             */
            scaleX, 
            
            /**
             * The scale to apply to the node along the y-axis.
             */
            scaleY, 
            /**
             * The scale to apply to the node along the z-axis.
             */
            scaleZ, 
            
            /**
             * X-coordinate in the Scene-graph where node should be rendered.
             */
            layoutX, 
            
            /**
             * Y-coordinate in the scene graph where node should be rendered.
             */
            layoutY, 
            
            /**
             * Z-coordinate in the scene graph where node should be rendered.
             */
            layoutZ, 
            
            /**
             * X-translation to apply to the node.
             */
            translateX, 
            
            
            /**
             * Y-translation to apply to the node.
             */
            translateY, 
            
            /**
             * 
             * Z-translation to apply to the node.
             */
            translateZ, 
            
            /**
             * Rotation to apply to the node.  In degrees.  Node is always rotated around its center.
             */
            rotate, 
            
            /**
             * The depth of the coordinate at which the renderer should paint itself.
             * By default nodes will have no depth, and renderer will be painted onto 
             * a rectangle on the Z=0 plane.  If you need to perform 3D rotations, you may want to
             * give the node depth by setting the depth of {@link #boundsInLocal} and changing the
             * {@link #localCanvasZ} to something other than 0 since the node will be rotated at its 
             * center point.
             */
            localCanvasZ;
    
    /**
     * The rotation axis around which rotations should be performed. (0, 0, 1) results in a rotation
     * around the z-axis, (1, 0, 0) results in a rotation around the x-axis, and (0, 1, 0) a rotation
     * around the y-axis.
     */
    public final Property<Point3D,Node> rotationAxis;
    
    /**
     * The local bounds of the node (without any of the transforms applied to it).  
     */
    public final Property<Bounds,Node> boundsInLocal;
    
    /**
     * Flag to indicate whether the node should be visible or not.
     */
    public final Property<Boolean,Node> visible;
    
    /**
     * The painting rectangle, into which the renderer should be painted inside the 
     * node's local bounds.
     */
    public final Property<Rectangle,Node> paintingRect;
    
    
    /**
     * Flag to specify whether the node should have its children re-laid out 
     */
    private boolean needsLayout=true;
    
    
    public Node() {
        scaleX = new Property<Double,Node>("scaleX", 1.0);
        scaleY = new Property<Double,Node>("scaleY", 1.0);
        scaleZ = new Property<Double,Node>("scaleZ", 1.0);
        layoutX = new Property<Double,Node>("layoutX", 0.0);
        layoutY = new Property<Double,Node>("layoutY", 0.0);
        layoutZ = new Property<Double,Node>("layoutZ", 0.0);
        translateX = new Property<Double,Node>("translateX", 0.0);
        translateY = new Property<Double,Node>("translateY", 0.0);
        translateZ = new Property<Double,Node>("translateZ", 0.0);
        rotate = new Property<Double,Node>("rotate", 0.0);
        rotationAxis = new Property<Point3D,Node>("rotationAxis", (Point3D)null);
        boundsInLocal = new Property<Bounds,Node>("boundsInLocal", new Bounds(0, 0, 0, 0, 0, 0));
        localCanvasZ = new Property<Double,Node>("localCanvasZ", 0.0);
        visible = new Property<Boolean,Node>("visible", true);
        paintingRect = new Property<Rectangle,Node>("paintingRect", (Rectangle)null);
        
    }
    
    /**
     * Sets the render as image flag.  True to render this Node as an image.
     * @param t 
     */
    public void setRenderAsImage(boolean t) {
        renderAsImage = t;
    }
    
    /**
     * Adds tags to this node.
     * @param tags 
     */
    public void addTags(String... tags) {
        if (this.tags == null) {
            this.tags = new HashSet<String>();
        }
        for (String tag : tags) {
            this.tags.add(tag);
        }
    }
    
    public Rectangle2D getBoundsInScene(Rectangle2D out) {
        Transform t = getLocalToSceneTransform();
        Bounds localBounds = boundsInLocal.get();
        float[] pt = new float[]{(float)localBounds.getMinX(), (float)localBounds.getMinY(), 0};
        t.transformPoint(pt, pt);
        out.setX(pt[0]);
        out.setY(pt[1]);
        pt[0] = (float)(localBounds.getMinX() + localBounds.getWidth());
        pt[1] = (float)(localBounds.getMinY() + localBounds.getHeight());
        
        t.transformPoint(pt, pt);
        out.setWidth(pt[0] - out.getX());
        out.setHeight(pt[1] - out.getY());
        return out;
    }
    
    /**
     * Removes tags from this node.
     * @param tags 
     */
    public void removeTags(String... tags) {
        if (this.tags != null) {
            for (String tag : tags) {
                this.tags.remove(tag);
            }
        }
    }
    
    /**
     * Check if this node has a tag.
     * @param tag
     * @return 
     */
    public boolean hasTag(String tag) {
        return (tags != null && tags.contains(tag));
    }
    
    /**
     * Sets the scene that this node is attached to.
     * @param scene 
     */
    void setScene(Scene scene) {
        this.scene = scene;
        if (children != null) {
            for (Node child : children) {
                child.setScene(scene);
            }
        }
    }
    
    //public void setLocalToParentTransform(Transform t) {
    //    this.localToParentTransform = t.copy();
    //}
    
    private static void translate(Transform t, double tx, double ty, double tz) {
        t.translate((float)tx, (float)ty, (float)tz);
    }
    
    private static void rotate(Transform t, double angle, double px, double py, double pz) {
        t.rotate((float)angle, (float)px, (float)py, (float)pz);
    }
    
    private static void scale(Transform t, double x, double y, double z) {
        t.scale((float)x, (float)y, (float)z);
    }
    
    /**
     * Gets the transform to use to transform the Node into its parent node's space.
     * @return 
     */
    public Transform getLocalToParentTransform() {
        Transform t = Transform.makeIdentity();
        Point3D rotationAxis = this.rotationAxis.get();
        if (rotationAxis == null) {
            rotationAxis = new Point3D(0, 0, 1);
        }
        Bounds localBounds = boundsInLocal.get();
        
        
        
        // Do translation
        translate(t, layoutX.get() + translateX.get(), layoutY.get() + translateY.get(), layoutZ.get() + translateZ.get());
        
        // Do scale
        
        
        // Do rotation
        translate(t, localBounds.getWidth()/2, localBounds.getHeight()/2, localBounds.getDepth()/2);
        scale(t, scaleX.get(), scaleY.get(), scaleZ.get());
        rotate(t, rotate.get() * Math.PI/180.0, rotationAxis.getX(), rotationAxis.getY(), rotationAxis.getZ());
        translate(t, -localBounds.getWidth()/2, -localBounds.getHeight()/2, -localBounds.getDepth()/2);
        translate(t, 0, 0, localCanvasZ.get());
        return t;
        
    }
    
    /**
     * Gets the transform to use to go from the local coordinates to scene coordinates.
     * @return 
     */
    public Transform getLocalToSceneTransform() {
        if (parent == null) {
            return getLocalToParentTransform();
        } else {
            Transform t = parent.getLocalToSceneTransform();
            t.concatenate(getLocalToParentTransform());
            return t;
        }
    }

    /**
     * Gets the scene that this node is attached to.
     * @return the scene
     */
    public Scene getScene() {
        return scene;
    }

    /**
     * Returns true if this node needs to have its children re-laid out before rendering
     * @return the needsLayout
     */
    public boolean isNeedsLayout() {
        return needsLayout;
    }

    /**
     * @param needsLayout the needsLayout to set
     */
    public void setNeedsLayout(boolean needsLayout) {
        this.needsLayout = needsLayout;
    }
    
    
    
    private class RenderableContainer extends Container {

        public RenderableContainer() {
            super(new BorderLayout(), "NodeWrapper");
            Style s = getAllStyles();
            s.setPadding(0, 0, 0, 0);
            s.setMargin(0, 0, 0, 0);
            s.setBgTransparency(0);
            s.setBorder(Border.createEmpty());
           
        }
        
        
        
        void render(Graphics g) {
            try {
                paintComponentBackground(g);
                paint(g);
                paintBorder(g);
            } catch (Throwable t) {
                
            }
        }
    }
       
    
    /**
     * Sets the component that should be used to render the node's contents.
     * @param comp 
     */
    public void setRenderer(Component comp) {
        if (rendererCnt == null) {
            rendererCnt = new RenderableContainer();
        }
        rendererCnt.removeAll();
        rendererCnt.add(BorderLayout.CENTER, comp);
        renderer = comp;
        boundsInLocal.get().setWidth(rendererCnt.getPreferredW());
        boundsInLocal.get().setHeight(rendererCnt.getPreferredH());
    }
    
    private Rectangle getPaintingRect() {
        if (paintingRect != null && paintingRect.get() != null) {
            return paintingRect.get();
        } else {
            return new Rectangle(0, 0, (int)boundsInLocal.get().getWidth(), (int)boundsInLocal.get().getHeight());
        }
    }
    
    /**
     * This can be used to hit test pointer events against this node.  It checks whether
     * a given absolute (x, y) coordinate hits the node.
     * @param x
     * @param y
     * @return 
     */
    public boolean contains(int x, int y) {
        GeneralPath p = GeneralPath.createFromPool();
        try {
            p.setRect(getPaintingRect(), getLocalToScreenTransform());
            return p.contains(x, y);
        } finally {
            GeneralPath.recycle(p);
        }
    }
    
    private void findNodesWithTag(Set<Node> out, String tag) {
        if (hasTag(tag)) {
            out.add(this);
        }
        for (Node child : getChildNodes()) {
            child.findNodesWithTag(out, tag);
        }
    }
    
    public Collection<Node> findNodesWithTag(String tag) {
        Set<Node> out = new HashSet<Node>();
        findNodesWithTag(out, tag);
        return out;
    }
    
    public Transform getLocalToScreenTransform() {
        Transform newT = Transform.isPerspectiveSupported() && scene.camera.get() != null ? 
                scene.camera.get().getTransform() : Transform.makeIdentity();
        newT.translate(getScene().getAbsoluteX(), getScene().getAbsoluteY());
        newT.concatenate(getLocalToSceneTransform());
        return newT;
    }
    
    /**
     * Renders the node onto a graphics context.
     * @param g 
     */
    public void render(Graphics g) {
        if (!visible.get()) {
            return;
        }
        Transform existingT = Transform.makeIdentity();
        g.getTransform(existingT);
        
        Transform newT = Transform.isPerspectiveSupported() && scene.camera.get() != null ? 
                scene.camera.get().getTransform() :
                Transform.makeIdentity();
        newT.translate(getScene().getAbsoluteX(), getScene().getAbsoluteY());
        newT.concatenate(getLocalToSceneTransform());
        newT.translate(-scene.getAbsoluteX(), -scene.getAbsoluteY());
        g.setTransform(newT);
        try {
            if (renderer != null) {
                Rectangle paintingRect = getPaintingRect();
                //System.out.println("Rendering on paint rect "+paintingRect);
                //System.out.println("Renderer: "+renderer);
                rendererCnt.setX(paintingRect.getX());
                rendererCnt.setY(paintingRect.getY());
                rendererCnt.setWidth(Math.max(0, paintingRect.getWidth()));
                rendererCnt.setHeight(Math.max(paintingRect.getHeight(), 0));
                rendererCnt.layoutContainer();
                if (renderAsImage) {
                    g.drawImage(rendererCnt.toImage(), paintingRect.getX(), paintingRect.getY());
                } else {
                    rendererCnt.render(g);
                }
            }
            layoutChildrenInternal();
            renderChildren(g);
        } finally {
            g.setTransform(existingT);
        }
        
    }
    
    /**
     * Renders the node's children.
     * @param g 
     */
    public void renderChildren(Graphics g) {
        if (children != null) {
            for (Node child : children) {
                child.render(g);
            }
        }
    }
    
    /**
     * Gets the renderer component for this node.
     * @return 
     */
    public Component getRenderer() {
        return renderer;
    }
    
    /**
     * Adds a child node.
     * @param child 
     */
    public void add(Node child) {
        if (children == null) {
            children = new ArrayList<Node>();
        }
        children.add(child);
        child.parent = this;
        child.setScene(getScene());
    }
    
    /**
     * Removes a child node.
     * @param child 
     */
    public void remove(Node child) {
        if (child.parent != this) {
            return;
        }
        if (children != null) {
            children.remove(child);
            child.parent = null;
            child.setScene(null);
        }
    }
    
    private void layoutChildrenInternal() {
        if (isNeedsLayout()) {
            setNeedsLayout(false);
            layoutChildren();
        }
    }
    
    /**
     * Can be overridden by subclasses to layout children.  Called before node is rendered.
     */
    protected void layoutChildren() {
        
    }
    
    /**
     * Gets the child nodes of this node.
     * @return 
     */
    public Iterable<Node> getChildNodes() {
        return children;
    }
    
    /**
     * Gets number of children in this node.
     * @return 
     */
    public int getChildCount() {
        if (children == null) {
            return 0;
        }
        return children.size();
    }
    
    /**
     * Gets child node at index.
     * @param index
     * @return 
     */
    public Node getChildAt(int index) {
        if (children == null) {
            return null;
        }
        return children.get(index);
    }
    
    /**
     * Checks if node has children.
     * @return 
     */
    public boolean hasChildren() {
        return children != null && !children.isEmpty();
    
    }
    
    
    /**
     * Removes all child nodes.
     */
    public void removeAll() {
        if (children != null) {
            children.clear();
        }
    }
}
    
