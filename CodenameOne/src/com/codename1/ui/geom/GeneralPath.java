/*
 * Copyright (c) 2012, Codename One and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.  Codename One designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Oracle in the LICENSE file that accompanied this code.
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
package com.codename1.ui.geom;


import com.codename1.io.Log;
import com.codename1.ui.Transform;
import com.codename1.util.MathUtil;
import java.util.ArrayList;
import java.util.Arrays;

/**
 * <p>A general geometric path, consisting of any number of subpaths constructed
 * out of straight lines and cubic or quadratic Bezier curves. The inside of the
 * curve is defined for drawing purposes by a winding rule. Either the
 * {@link #WIND_EVEN_ODD} or {@link #WIND_NON_ZERO} winding rule can be chosen.</p>
 *
 * <h4>A drawing of a GeneralPath</h4>
 *
 * <img
 * src="http://developer.classpath.org/doc/java/awt/geom/doc-files/GeneralPath-1.png"/>
 *
 * <p>
 * The {@link #WIND_EVEN_ODD} winding rule defines a point as inside a path if:
 * A ray from the point towards infinity in an arbitrary direction intersects
 * the path an odd number of times. Points {@literal A} and {@literal C} in the
 * image are considered to be outside the path. (both intersect twice) Point
 * {@literal B} intersects once, and is inside.</p>
 *
 * <p>
 * The {@link #WIND_NON_ZERO} winding rule defines a point as inside a path if:
 * The path intersects the ray in an equal number of opposite directions. Point
 * {@link A} in the image is outside (one intersection in the 'up' direction,
 * one in the 'down' direction) Point {@literal B} in the image is inside (one
 * intersection 'down') Point C in the image is inside (two intersections in the
 * 'down' direction)</p>
 *
 * <script src="https://gist.github.com/codenameone/3f2f8cdaabb7780eae6f.js"></script>
 * <img src="https://www.codenameone.com/img/developer-guide/graphics-shape-fill.png" alt="Fill a shape general path" />
 * 
 * <p>Note: This description and image were copied from <a
 * href="http://developer.classpath.org/doc/java/awt/geom/GeneralPath.html">the
 * GNU classpath</a>
 * docs). License here http://www.gnu.org/licenses/licenses.html#FDL</p>
 *
 * @author shannah
 *
 * @see com.codename1.ui.Graphics#drawShape
 * @see com.codename1.ui.Graphics#fillShape
 */
public final class GeneralPath implements Shape {

    private static int MAX_POOL_SIZE=20;
    
    private static ArrayList<GeneralPath> pathPool;
    private static ArrayList<Rectangle> rectPool;
    private static ArrayList<float[]> floatPool;
    private static ArrayList<boolean[]> boolPool;
    private static ArrayList<Iterator> iteratorPool;
    
    
    private static ArrayList<GeneralPath> pathPool() {
        if (pathPool == null) {
            pathPool = new ArrayList<GeneralPath>();
        }
        return pathPool;
    }
    
    private static ArrayList<Rectangle> rectPool() {
        if (rectPool == null) {
            rectPool = new ArrayList<Rectangle>();
        }
        return rectPool;
    }
    
    private static ArrayList<float[]> floatPool() {
        if (floatPool == null) {
            floatPool = new ArrayList<float[]>();
        }
        return floatPool;
    }
    
    private static ArrayList<boolean[]> boolPool() {
        if (boolPool == null) {
            boolPool = new ArrayList<boolean[]>();
        }
        return boolPool;
    }
    
    private static ArrayList<Iterator> iteratorPool() {
        if (iteratorPool == null) {
            iteratorPool = new ArrayList<Iterator>();
        }
        return iteratorPool;
    }
    
    private static synchronized GeneralPath createPathFromPool() {
        if (!pathPool().isEmpty()) {
            GeneralPath out = pathPool.remove(pathPool.size()-1);
            out.reset();
            return out;
        }
        return new GeneralPath();
    }
    
    private static synchronized Rectangle createRectFromPool() {
        if (!rectPool().isEmpty()) {
            return rectPool.remove(rectPool.size()-1);
        }
        return new Rectangle();
    }
    
    private static synchronized float[] createFloatArrayFromPool(int size) {
        int len = floatPool().size();
        for (int i=0; i<len; i++) {
            float[] arr = floatPool.get(i);
            if (arr.length == size) {
                return floatPool.remove(i);
            }
        }
        return new float[size];
    }
    
    
    private static synchronized boolean[] createBoolArrayFromPool(int size) {
        int len = boolPool().size();
        for (int i=0; i<len; i++) {
            boolean[] arr = boolPool.get(i);
            if (arr.length == size) {
                return boolPool.remove(i);
            }
        }
        return new boolean[size];
    }
    
    private static synchronized Iterator createIteratorFromPool(GeneralPath p, Transform t) {
        if (!iteratorPool().isEmpty()) {
            Iterator it = iteratorPool.remove(iteratorPool.size()-1);
            it.p = p;
            it.transform = t;
            it.reset();
            return it;
        }
        return (Iterator)p.getPathIterator(t);
        
    }
    
    /**
     * Returns a GeneralPath to the reusable object pool for GeneralPaths.
     * @param p The path to recycle.
     * 
     * @see #createFromPool() 
     */
    public static synchronized void recycle(GeneralPath p) {
        if (pathPool().size() >= MAX_POOL_SIZE || p == null) return;
        pathPool.add(p);
    }
    
    private static synchronized void recycle(Rectangle r) {
        if (rectPool.size() >= MAX_POOL_SIZE || r == null) return;
        rectPool.add(r);
    }
    
    private static synchronized void recycle(float[] a) {
        if (floatPool().size() >= MAX_POOL_SIZE || a == null) return;
        floatPool.add(a);
    }
    
    
    private static synchronized void recycle(boolean[] b) {
        if (boolPool().size() >= MAX_POOL_SIZE || b == null) {
            return;
        }
        boolPool.add(b);
    }
    
    private static synchronized void recycle(Iterator it) {
        if (iteratorPool().size() >= MAX_POOL_SIZE || it == null) {
            return;
        }
        iteratorPool.add(it);
    }
    
    /**
     * Creates a new GeneralPath from an object Pool.  This is useful
     * if you need to create a temporary General path that you wish
     * to dispose of after using.  
     * 
     * <p>You should return this object back to the pool when you are done
     * using the {@link #recycle(com.codename1.ui.geom.GeneralPath) } method.
     * @return 
     */
    public static GeneralPath createFromPool() {
        return createPathFromPool();
    }
    
    
    
    private boolean dirty = false;

    // END Alpha Mask Caching Functionality
    //--------------------------------------------------------------------------
    /**
     * Same constant as {@link PathIterator#WIND_EVEN_ODD}
     */
    public static final int WIND_EVEN_ODD = PathIterator.WIND_EVEN_ODD;
    /**
     * Same constant as {@link PathIterator#WIND_NON_ZERO}
     */
    public static final int WIND_NON_ZERO = PathIterator.WIND_NON_ZERO;

    /**
     * The buffers size
     */
    private static final int BUFFER_SIZE = 10;

    /**
     * The buffers capacity
     */
    private static final int BUFFER_CAPACITY = 10;

    /**
     * The point's types buffer
     */
    private byte[] types;

    /**
     * The points buffer
     */
    private float[] points;

    /**
     * The point's type buffer size
     */
    private int typeSize;

    /**
     * The points buffer size
     */
    private int pointSize;

    /**
     * The path rule
     */
    private int rule;
    
    

    /**
     * The space amount in points buffer for different segmenet's types
     */
    private static int pointShift[] = {
        2, // MOVETO
        2, // LINETO
        4, // QUADTO
        6, // CUBICTO
        0}; // CLOSE

    /*
     * GeneralPath path iterator 
     */
    private class Iterator implements PathIterator {

        /**
         * The current cursor position in types buffer
         */
        int typeIndex;

        /**
         * The current cursor position in points buffer
         */
        int pointIndex;

        /**
         * The source GeneralPath object
         */
        GeneralPath p;

        Transform transform;

        /**
         * Constructs a new GeneralPath.Iterator for given general path
         *
         * @param path - the source GeneralPath object
         */
        Iterator(GeneralPath path) {
            this.p = path;

        }
        
        private void reset() {
            typeIndex = 0;
            pointIndex = 0;
        }

        public int getWindingRule() {
            return p.getWindingRule();
        }

        public boolean isDone() {
            return typeIndex >= p.typeSize;
        }

        public void next() {
            typeIndex++;
        }

        private void transformSegmentInPlace() {
            if (isDone()) {
                // awt.4B=Iterator out of bounds
                throw new IndexOutOfBoundsException("Path done"); //$NON-NLS-1$
            }
            if (transform == null) {
                return;
            }
            int type = p.types[typeIndex];
            int count = GeneralPath.pointShift[type];
            for (int i=0; i < count; i+=2) {
                buf[0] = p.points[pointIndex + i];
                buf[1] = p.points[pointIndex + i + 1];
                transform.transformPoint(buf, buf);
                p.points[pointIndex+i] = buf[0];
                p.points[pointIndex+i+1] = buf[1];
            }
        }
        
        
        public int currentSegment(double[] coords) {
            float[] fcoords = createFloatArrayFromPool(6);
            try {
                int res = currentSegment(fcoords);
                int type = p.types[typeIndex];
                int count = GeneralPath.pointShift[type];
            
                for (int i=0; i< count; i ++) {
                    coords[i] = fcoords[i];
                }
                return res;
            } finally {
                recycle(fcoords);
            }
        }

        private float[] buf = new float[2];

        public int currentSegment(float[] coords) {
            if (isDone()) {
                // awt.4B=Iterator out of bounds
                throw new IndexOutOfBoundsException("Path done"); //$NON-NLS-1$
            }
            int type = p.types[typeIndex];
            int count = GeneralPath.pointShift[type];
            if (transform == null) {
                System.arraycopy(p.points, pointIndex, coords, 0, count);
            } else {
                transform.transformPoints(2, p.points, pointIndex, coords, 0, count / 2);
            }
            pointIndex += count;
            return type;
        }

    }

    /**
     * Constructs a GeneralPath with the default ({@link #WIND_NON_ZERO})
     * winding rule and initial capacity (10).
     */
    public GeneralPath() {
        this(WIND_NON_ZERO, BUFFER_SIZE);
    }

    /**
     * Constructs a GeneralPath with a specific winding rule and the default
     * initial capacity (10).
     *
     * @param rule The winding rule. One of {@link #WIND_NON_ZERO} and
     * {@link #WIND_EVEN_ODD}
     * @see #WIND_NON_ZERO
     * @see #WIND_EVEN_ODD
     */
    public GeneralPath(int rule) {
        this(rule, BUFFER_SIZE);
    }

    /**
     * Constructs a GeneralPath with a specific winding rule and the initial
     * capacity. The initial capacity should be the approximate number of path
     * segments to be used.
     *
     * @param rule The winding rule. ({@link #WIND_NON_ZERO} or
     * {@link #WIND_EVEN_ODD}).
     * @param initialCapacity the inital capacity, in path segments
     */
    public GeneralPath(int rule, int initialCapacity) {
        setWindingRule(rule);
        types = new byte[initialCapacity];
        points = new float[initialCapacity * 2];
    }
    
    /**
     * Checks to see if this path forms a polygon.
     * @return True if the path is a polygon.
     */
    public boolean isPolygon() {
        
        if (isRectangle()) {
            return true;
        }
        Iterator it = createIteratorFromPool(this, null);
        float[] curr = createFloatArrayFromPool(6);
        float[] firstPoint = createFloatArrayFromPool(2);
        try {
            boolean firstMove = false;
            int cmd = -1;
            while (!it.isDone()) {
                switch (cmd = it.currentSegment(curr)) {
                    case PathIterator.SEG_MOVETO: {
                        if (firstMove) {
                            return false;
                        }
                        firstMove = true;
                        firstPoint[0] = curr[0];
                        firstPoint[1] = curr[1];
                        break;
                    }
                    case PathIterator.SEG_CUBICTO:
                    case PathIterator.SEG_QUADTO:
                    return false;
                }
                it.next();
            }
            
            return cmd == PathIterator.SEG_CLOSE || (curr[0] == firstPoint[0] && curr[1] == firstPoint[1]);
            
        } finally {
            recycle(it);
            recycle(curr);
            recycle(firstPoint);
        }
        
    }
    
    /**
     * Returns the number of path commands in this path.
     * @return The number of path commands in this path.
     */
    public int getTypesSize() {
        return typeSize;
    }
    
    /**
     * Returns the number of points in this path.
     * @return The number of points in this path.
     */
    public int getPointsSize() {
        return pointSize;
    }
    
    /**
     * Returns a copy of the types (aka path commands) in this path.
     * @param out An array to copy the path commands into.
     */
    public void getTypes(byte[] out) {
        System.arraycopy(types, 0, out, 0, Math.min(types.length, out.length));
    }
    
    /**
     * Returns a copy of the points in this path.
     * @param out An array to copy the points into.
     */
    public void getPoints(float[] out) {
        System.arraycopy(points, 0, out, 0, Math.min(points.length, out.length));
    }

    /**
     * Constructs a GeneralPath from an arbitrary shape object. The Shapes
     * PathIterator path and winding rule will be used.
     *
     * @param shape
     */
    public GeneralPath(Shape shape) {
        this(WIND_NON_ZERO, BUFFER_SIZE);
        if (shape.getClass() == GeneralPath.class) {
            setPath((GeneralPath)shape, null);
        } else {
            PathIterator p = shape.getPathIterator();
            setWindingRule(p.getWindingRule());
            append(p, false);
        }
    }
    
    public String toString(){
        StringBuilder sb = new StringBuilder();
        sb.append("[General Path: ");
        Iterator it = createIteratorFromPool(this, null);
        float[] buf = createFloatArrayFromPool(6);//new float[6];
        try {
            while (!it.isDone() ){
                int type = it.currentSegment(buf);
                switch ( type ){
                    case PathIterator.SEG_MOVETO:
                        sb.append("Move ("+buf[0]+","+buf[1]+"), ");
                        break;
                    case PathIterator.SEG_LINETO:
                        sb.append("Line ("+buf[0]+","+buf[1]+"), ");
                        break;
                    case PathIterator.SEG_CUBICTO:
                        sb.append("Curve ("+buf[0]+","+buf[1]+".."+buf[2]+","+buf[3]+".."+buf[4]+","+buf[5]+")");
                        break;
                    case PathIterator.SEG_QUADTO:
                        sb.append("Curve ("+buf[0]+","+buf[1]+".."+buf[2]+","+buf[3]+")");
                        break;
                    case PathIterator.SEG_CLOSE:
                        sb.append(" CLOSE]");
                        break;
                }
                it.next();
            }
        } finally {
            recycle(buf);
            recycle(it);
        }
        return sb.toString();
    }

    /**
     * Sets the path's winding rule, which controls which areas are considered
     * 'inside' or 'outside' the path on drawing. Valid rules are
     * {@link #WIND_EVEN_ODD} for an even-odd winding rule, or
     * {@link #WIND_NON_ZERO} for a non-zero winding rule.
     *
     * @param rule the rule. ({@link #WIND_NON_ZERO} or {@link #WIND_EVEN_ODD}).
     */
    public void setWindingRule(int rule) {
        if (rule != WIND_EVEN_ODD && rule != WIND_NON_ZERO) {
            // awt.209=Invalid winding rule value
            throw new java.lang.IllegalArgumentException("Invalid winding rule"); //$NON-NLS-1$
        }
        dirty = true;
        this.rule = rule;
    }
    
    
    public boolean equals(Shape shape, Transform t) {
        if (t != null && !t.isIdentity()) {
            GeneralPath p = createPathFromPool();
            p.setShape(shape, t);
            try {
                return equals(p, (Transform)null);
            } finally {
                recycle(p);
            }
        }
        if (shape == this) return true;
        if (shape instanceof Rectangle) {
            Rectangle r = (Rectangle)shape;
            Rectangle tmpRect = createRectFromPool();
            try {
                getBounds(tmpRect);
                return r.equals(tmpRect);
            } finally {
                recycle(tmpRect);
            }
        } else if (shape instanceof GeneralPath) {
            GeneralPath tmpPath = (GeneralPath)shape;
            return Arrays.equals(points, tmpPath.points) && Arrays.equals(types, tmpPath.types);
        } else {
            GeneralPath tmpPath2 = createPathFromPool();
            try {
                tmpPath2.setShape(shape, null);
                return equals(tmpPath2, (Transform)null);
            } finally {
                recycle(tmpPath2);
            }
        }
    }

    /**
     * Returns the path's current winding rule.
     *
     * @return {@link #WIND_NON_ZERO} or {@link #WIND_EVEN_ODD}
     */
    public int getWindingRule() {
        return rule;
    }

    /**
     * Checks points and types buffer size to add pointCount points. If
     * necessary realloc buffers to enlarge size.
     *
     * @param pointCount - the point count to be added in buffer
     */
    private void checkBuf(int pointCount, boolean checkMove) {
        if (checkMove && typeSize == 0) {
            // awt.20A=First segment should be SEG_MOVETO type
            throw new IndexOutOfBoundsException("First segment must be a moveto"); //$NON-NLS-1$
        }
        if (typeSize == types.length) {
            byte tmp[] = new byte[typeSize + BUFFER_CAPACITY];
            System.arraycopy(types, 0, tmp, 0, typeSize);
            types = tmp;
        }
        if (pointSize + pointCount > points.length) {
            float tmp[] = new float[pointSize + Math.max(BUFFER_CAPACITY * 2, pointCount)];
            System.arraycopy(points, 0, tmp, 0, pointSize);
            points = tmp;
        }
    }
    

    public void moveTo(double x, double y){
        moveTo((float)x, (float)y);
    }
    
    /**
     * Adds a new point to a path.
     *
     * @param x the x-coordinate.
     * @param y the y-coordinate.
     */
    public void moveTo(float x, float y) {
        if (typeSize > 0 && types[typeSize - 1] == PathIterator.SEG_MOVETO) {
            points[pointSize - 2] = x;
            points[pointSize - 1] = y;
        } else {
            checkBuf(2, false);
            types[typeSize++] = PathIterator.SEG_MOVETO;
            points[pointSize++] = x;
            points[pointSize++] = y;
        }
        dirty = true;
    }

    
    public void lineTo(double x, double y){
        lineTo((float)x, (float)y);
    }
    /**
     * Appends a straight line to the current path.
     *
     * @param x x coordinate of the line endpoint.
     * @param y y coordinate of the line endpoint.
     */
    public void lineTo(float x, float y) {
        checkBuf(2, true);
        types[typeSize++] = PathIterator.SEG_LINETO;
        points[pointSize++] = x;
        points[pointSize++] = y;
        dirty = true;
    }

    public void quadTo(double x1, double y1, double x2, double y2){
        quadTo((float)x1, (float)y1, (float)x2, (float)y2);
    }
    
    /**
     * Appends a quadratic Bezier curve to the current path.
     *
     * @param x1 x coordinate of the control point
     * @param y1 y coordinate of the control point
     * @param x2 x coordinate of the curve endpoint.
     * @param y2 y coordinate of the curve endpoint.
     */
    public void quadTo(float x1, float y1, float x2, float y2) {
        checkBuf(4, true);
        types[typeSize++] = PathIterator.SEG_QUADTO;
        points[pointSize++] = x1;
        points[pointSize++] = y1;
        points[pointSize++] = x2;
        points[pointSize++] = y2;
        dirty = true;
    }

    public void curveTo(double x1, double y1, double x2, double y2, double x3, double y3){
        curveTo((float)x1, (float)y1, (float)x2, (float)y2, (float)x3, (float)y3);
    }
    
    /**
     * Appends a cubic Bezier curve to the current path.
     *
     * @param x1 x coordinate of the first control point
     * @param y1 y coordinate of the first control point
     * @param x2 x coordinate of the second control point
     * @param y2 y coordinate of the second control point
     * @param x3 x coordinate of the curve endpoint.
     * @param y3 y coordinate of the curve endpoint.
     */
    public void curveTo(float x1, float y1, float x2, float y2, float x3, float y3) {
        checkBuf(6, true);
        types[typeSize++] = PathIterator.SEG_CUBICTO;
        points[pointSize++] = x1;
        points[pointSize++] = y1;
        points[pointSize++] = x2;
        points[pointSize++] = y2;
        points[pointSize++] = x3;
        points[pointSize++] = y3;
        dirty = true;
    }
    
    /**
     * Draws an elliptical arc on the path given the provided bounds.
     * @param x Left x coord of bounding rect.
     * @param y Top y coordof bounding rect.
     * @param w Width of bounding rect.
     * @param h Height of bounding rect.
     * @param startAngle Start angle on ellipse in radians.  Counter-clockwise from 3-o'clock.
     * @param sweepAngle Sweep angle in radians. Counter-clockwise.
     */
    public void arc(float x, float y, float w, float h, float startAngle, float sweepAngle) {
        
        arc(x, y, w, h, startAngle, sweepAngle, false);
    }
    
    /**
     * Draws an elliptical arc on the path given the provided bounds.
     * @param x Left x coord of bounding rect.
     * @param y Top y coordof bounding rect.
     * @param w Width of bounding rect.
     * @param h Height of bounding rect.
     * @param startAngle Start angle on ellipse in radians.  Counter-clockwise from 3-o'clock.
     * @param sweepAngle Sweep angle in radians. Counter-clockwise.
     * @param joinPath If true, then this will join the arc to the existing path with a line.
     */
    public void arc(float x, float y, float w, float h, float startAngle, float sweepAngle, boolean joinPath) {
        
        Ellipse e = new Ellipse();
        Ellipse.initWithBounds(e, x, y, w, h);
        e.addToPath(this, -startAngle, -sweepAngle, joinPath);
    }
    
    
    /**
     * Draws an elliptical arc on the path given the provided bounds.
     * @param x Left x coord of bounding rect.
     * @param y Top y coordof bounding rect.
     * @param w Width of bounding rect.
     * @param h Height of bounding rect.
     * @param startAngle Start angle on ellipse in radians.  Counter-clockwise from 3-o'clock.
     * @param sweepAngle Sweep angle in radians. Counter-clockwise.
     */
    public void arc(double x, double y, double w, double h, double startAngle, double sweepAngle) {
        arc(x, y, w, h, startAngle, sweepAngle, false);
    }
    
    
    /**
     * Draws an elliptical arc on the path given the provided bounds.
     * @param x Left x coord of bounding rect.
     * @param y Top y coordof bounding rect.
     * @param w Width of bounding rect.
     * @param h Height of bounding rect.
     * @param startAngle Start angle on ellipse in radians.  Counter-clockwise from 3-o'clock.
     * @param sweepAngle Sweep angle in radians. Counter-clockwise.
     * @param joinPath If true then this will join the arc to the existing path with a line.
     */
    public void arc(double x, double y, double w, double h, double startAngle, double sweepAngle, boolean joinPath) {
        arc((float)x, (float)y, (float)w, (float)h, (float)startAngle, (float)sweepAngle, joinPath);
    }
    
    
    private static void addBezierArcToPath(GeneralPath path, double cx, double cy,
                                          double startX, double startY, double endX, double endY) {
        addBezierArcToPath(path, cx, cy, startX, startY, endX, endY, false);
    }
    
    
    /**
     * 
     * @param path
     * @param cx
     * @param cy
     * @param startX
     * @param startY
     * @param endX
     * @param endY
     * @param clockwise 
     */
    private static void addBezierArcToPath(GeneralPath path, double cx, double cy,
                                          double startX, double startY, double endX, double endY, boolean clockwise) {
        if ( startX != endX || startY != endY ){
            double ax = startX - cx;
            double ay = startY - cy;
            double bx = endX - cx;
            double by = endY- cy;
            
            final double r1s = ax * ax + ay * ay;
            final double r2s = bx * bx + by * by;
            double ellipseScaleY = 0;
            if (Math.abs(r1s - r2s) > 2) {
                // This is not a circle
                // Let's get the arc for the circle
                ellipseScaleY = Math.sqrt(((ax*ax) - (bx*bx)) / (by*by - ay*ay));
                startY = cy + ellipseScaleY * (startY-cy);
                endY = cy + ellipseScaleY * (endY-cy);
                
                ay = startY - cy;
                by = endY - cy;
            } else {
                double startAngle = MathUtil.atan2(ay, ax);
                double endAngle = MathUtil.atan2(by, bx);
                
                double dist = Math.abs(endAngle - startAngle);
                if (clockwise) {
                    if (startAngle > endAngle) {
                        dist = Math.PI*2-dist;
                    }
                } else {
                    if (startAngle < endAngle) {
                        dist = Math.PI*2-dist;
                    }
                }
                
                //System.out.println("dist: "+dist+" startAngle: "+startAngle+" endAngle: "+endAngle);
                if (dist > Math.PI/3) {
                    // We bisect
                    double r = Math.sqrt(r1s);
                    double bisectAngle = (startAngle + endAngle)/2;
                    if (clockwise) {
                        if (startAngle > endAngle) {
                            bisectAngle += Math.PI;
                        }
                    } else {
                        if (startAngle < endAngle) {
                            bisectAngle += Math.PI;
                        }
                    }
                    double bisectX = cx + r * Math.cos(bisectAngle);
                    double bisectY = cy + r * Math.sin(bisectAngle);
                    addBezierArcToPath(path, cx, cy, startX, startY, bisectX, bisectY, clockwise);
                    addBezierArcToPath(path, cx, cy, bisectX, bisectY, endX, endY, clockwise);
                    return;
                }
                
                
            }
            
            final double q1 = r1s;//ax * ax + ay * ay;
            final double q2 = q1 + ax * bx + ay * by;
            final double k2 = 4d / 3d * (Math.sqrt(2d * q1 * q2) - q2) / (ax * by - ay * bx);
            final float x2 = (float)(cx + ax - k2 * ay);
            float y2 = (float)(cy + ay + k2 * ax);
            final float x3 = (float)(cx + bx + k2 * by);
             float y3 = (float)(cy + by - k2 * bx);
            if (ellipseScaleY != 0) {
                y2 = (float)(cy + (y2-cy)/ellipseScaleY);
                y3 = (float)(cy + (y3-cy)/ellipseScaleY);
                endY = (float)(cy + (endY-cy)/ellipseScaleY);
            }
            path.curveTo(x2, y2, x3, y3, endX, endY);
            
        } 
    }
    
    
    static class Ellipse {
        private double a;
        private double b;
        private double cx;
        private double cy;
        private EPoint _tmp1=new EPoint();
        
        static void initWithBounds(Ellipse e, double x, double y, double w, double h) {
            e.cx = x+w/2;
            e.cy = y+h/2;
            e.a = w/2;
            e.b = h/2;
        }
        
        static void initWithPerimeterPoints(Ellipse e, double cx, double cy, double p1x, double p1y, double p2x, double p2y) {

            /*
            e.cx = cx;
            e.cy = cy;
            double x1 = p1x-cx;
            double y1 = p1y-cy;
            double x2 = p2x-cx;
            double y2 = p2y-cy;
            double x1s = x1*x1;
            double x2s = x2*x2;
            double y1s = y1*y1;
            double y2s = y2*y2;
            if (Math.abs(x1s-x2s) < 0.001 ||Math.abs(y1s-y2s) < 0.001) {
                a = b = Math.max(Math.sqrt(Math.abs(y2)));
            }
            if (Math.abs(x1s-x2s) > 0.001) {
                e.b = Math.sqrt((x1s*y2s-x2s-y1s)/(x1s-x2s));
                double bs = e.b*e.b;
                e.a = Math.sqrt(x1s*bs/(bs-y1s));
            } else {
                e.a = Math.sqrt((y1s*x2s-y2s-x1s)/(y1s-y2s));
                double as = e.a*e.a;
                e.b = Math.sqrt(y1s*as/(as-x1s));
            }
            */
            
        }
        
        @Override
        public String toString() {
            
            return "Ellipse center=("+cx+","+cy+") a="+a+", b="+b+")";
        }
        
        void getPointAtAngle(double theta, EPoint out) {
            double tanTheta = Math.tan(theta);
            double tanThetas = tanTheta*tanTheta;
            double bs = b*b;
            double as = a*a;
            double x = a*b/Math.sqrt(bs+as*tanThetas);
            if (Math.cos(theta)<0) {
                x = -x;
            }
            double y = a*b/Math.sqrt(as+bs/tanThetas);
            if (Math.sin(theta)<0) {
                y = -y;
            }
            out.x = x + cx;
            out.y = y + cy;
        }
        
        double getAngleAtPoint(double px, double py) {
            px -= cx;
            py -= cy;
            
            return MathUtil.atan2(py, px);
        }
        
        void addToPath(GeneralPath p, double startAngle, double sweepAngle, boolean join) {
            getPointAtAngle(startAngle, _tmp1);
            if (join) {
                
                p.lineTo(_tmp1.x, _tmp1.y);
            } else {
                p.moveTo(_tmp1.x, _tmp1.y);
            }
            _addToPath(p, startAngle, sweepAngle);
        }
        
        private void _addToPath(GeneralPath p, double startAngle, double sweepAngle) {
            double _2pi = Math.PI*2;
            
            if (Math.abs(sweepAngle) > Math.PI/4) {
                //double halfAngle = sweepAngle/2;
                double diff = Math.PI/4;
                if (sweepAngle < 0) {
                    diff = -diff;
                }
                _addToPath(p, startAngle, diff);
                _addToPath(p, startAngle+diff, sweepAngle-diff);
            } else {
                getPointAtAngle(startAngle+sweepAngle, _tmp1);
                //System.out.println("Line to "+_tmp1.x+", "+_tmp1.y);
                EPoint controlPoint = new EPoint();
                calculateBezierControlPoint(startAngle, sweepAngle, controlPoint);
                p.quadTo(controlPoint.x, controlPoint.y, _tmp1.x, _tmp1.y);
                //p.lineTo(_tmp1.x, _tmp1.y);
            }
        }
        
        private void calculateBezierControlPoint(double startAngle, double sweepAngle, EPoint point) {
            EPoint p1 = new EPoint();
            
            getPointAtAngle(startAngle, p1);
            p1.x-= cx;
            p1.y -= cy;
            
            EPoint p2 = new EPoint();
            getPointAtAngle(startAngle+sweepAngle, p2);
            p2.x -= cx;
            p2.y -= cy;
            
            //System.out.println("p1: "+p1.x+", "+p1.y+", p2:"+p2.x+","+p2.y);
            double x1s = p1.x*p1.x;
            double y1s = p1.y*p1.y;
            double x2s = p2.x*p2.x;
            double y2s = p2.y*p2.y;
            
            double as = a*a;
            double bs = b*b;
            //point.x = (x2s*bs/(p2.y*as) + p2.y - x1s*bs/(p1.y*as) - p1.y) / (-p1.x*bs/(p1.y*as) + p2.x*bs/(p2.y*as));
            //point.y = (-p1.x*bs/(p1.y*as))*point.x + x1s*bs/(p1.y*as) + p1.y;
            
            
            point.x = -(p1.y*(-as*y2s-bs*x2s)+as*y1s*p2.y+bs*x1s*p2.y)/(bs*p2.x*p1.y-bs*p1.x*p2.y);
            point.y = (p1.x*(-as*y2s-bs*x2s)+as*p2.x*y1s+bs*x1s*p2.x)/(as*p2.x*p1.y-as*p1.x*p2.y);
            
            point.x += cx;
            point.y += cy;
            //System.out.println("control: "+point.x+","+point.y);
        }
    
        
    }
    
    static class EPoint {
        double x;
        double y;
    }
    
    /**
     * Adds a circular arc to the given path by approximating it through a cubic BÈzier curve, splitting it if
     * necessary. The precision of the approximation can be adjusted through {@code pointsOnCircle} and
     * {@code overlapPoints} parameters.
     * <p>
     * <strong>Example:</strong> imagine an arc starting from 0? and sweeping 100? with a value of
     * {@code pointsOnCircle} equal to 12 (threshold -> 360? / 12 = 30?):
     * <ul>
     * <li>if {@code overlapPoints} is {@code true}, it will be split as following:
     * <ul>
     * <li>from 0? to 30? (sweep 30?)</li>
     * <li>from 30? to 60? (sweep 30?)</li>
     * <li>from 60? to 90? (sweep 30?)</li>
     * <li>from 90? to 100? (sweep 10?)</li>
     * </ul>
     * </li>
     * <li>if {@code overlapPoints} is {@code false}, it will be split into 4 equal arcs:
     * <ul>
     * <li>from 0? to 25? (sweep 25?)</li>
     * <li>from 25? to 50? (sweep 25?)</li>
     * <li>from 50? to 75? (sweep 25?)</li>
     * <li>from 75? to 100? (sweep 25?)</li>
     * </ul>
     * </li>
     * </ul>
     * </p>
     * <p/>
     * For a technical explanation:
     * <a href="http://hansmuller-flex.blogspot.de/2011/10/more-about-approximating-circular-arcs.html">
     * http://hansmuller-flex.blogspot.de/2011/10/more-about-approximating-circular-arcs.html
     * </a>
     *
     * @param center            The center of the circle.
     * @param radius            The radius of the circle.
     * @param startAngleRadians The starting angle on the circle (in radians).
     * @param sweepAngleRadians How long to make the total arc (in radians).
     * @param pointsOnCircle    Defines a <i>threshold</i> (360? /{@code pointsOnCircle}) to split the BÈzier arc to
     *                          better approximate a circular arc, depending also on the value of {@code overlapPoints}.
     *                          The suggested number to have a reasonable approximation of a circle is at least 4 (90?).
     *                          Less than 1 will be ignored (the arc will not be split).
     * @param overlapPoints     Given the <i>threshold</i> defined through {@code pointsOnCircle}:
     *                          <ul>
     *                          <li>if {@code true}, split the arc on every angle which is a multiple of the
     *                          <i>threshold</i> (yields better results if drawing precision is required,
     *                          especially when stacking multiple arcs, but can potentially use more points)</li>
     *                          <li>if {@code false}, split the arc equally so that each part is shorter than
     *                          the <i>threshold</i></li>
     *                          </ul>
     * @param addToPath         An existing path where to add the arc to, or {@code null} to create a new path.
     *
     *
     * @see #createBezierArcDegrees(android.graphics.PointF, float, float, float, int, boolean, android.graphics.Path)
     */
    private static void createBezierArcRadians(float cx, float cy, float radiusX, float radiusY, double startAngleRadians,
                                              double sweepAngleRadians, int pointsOnCircle, boolean overlapPoints,
                                              GeneralPath addToPath, boolean joinPath)
    {
        final GeneralPath path = addToPath;
        if (sweepAngleRadians == 0d) { return; }
 
        float radius = radiusX;
        float yScale = radiusY / radius;
        
        if (pointsOnCircle >= 1)
        {
            final double threshold = Math.PI * 2d / pointsOnCircle;
            if (Math.abs(sweepAngleRadians) > threshold)
            {
                double angle = normalizeRadians(startAngleRadians);
                //PointF end, start = pointFromAngleRadians(center, radius, angle);
                double endX, endY;
                double startX = cx + radius * Math.cos(angle);
                double startY = cy + radius * Math.sin(angle) * yScale;
                if (joinPath) {
                    path.lineTo(startX, startY);
                } else {
                    path.moveTo(startX, startY);
                }
                if (overlapPoints)
                {
                    final boolean cw = sweepAngleRadians > 0; // clockwise?
                    final double angleEnd = angle + sweepAngleRadians;
                    while (true)
                    {
                        double next = (cw ? Math.ceil(angle / threshold) : Math.floor(angle / threshold)) * threshold;
                        if (angle == next) { next += threshold * (cw ? 1d : -1d); }
                        final boolean isEnd = cw ? angleEnd <= next : angleEnd >= next;
                        //end = pointFromAngleRadians(center, radius, isEnd ? angleEnd : next);
                        endX = cx + radius * Math.cos(isEnd ? angleEnd : next);
                        endY = cy + radius * Math.sin(isEnd ? angleEnd : next) *yScale;
                        addBezierArcToPath(path, cx, cy, startX, startY, endX, endY);
                        if (isEnd) { break; }
                        angle = next;
                        startX = endX;
                        startY = endY;
                    }
                }
                else
                {
                    final int n = Math.abs((int)Math.ceil(sweepAngleRadians / threshold));
                    final double sweep = sweepAngleRadians / (double)n;
                    for (int i = 0;
                         i < n;
                         i++, startX = endX, startY = endY)
                    {
                        angle += sweep;
                        //end = pointFromAngleRadians(center, radius, angle);
                        endX = cx + radius * Math.cos(angle);
                        endY = cy + radius * Math.sin(angle) * yScale;
                        addBezierArcToPath(path, cx, cy, startX, startY, endX, endY);
                    }
                    
                }
                return;
            }
        }
 
        startAngleRadians = normalizeRadians(startAngleRadians);
        double startX = cx + radius * Math.cos(startAngleRadians);
        double startY = cy + radius * Math.sin(startAngleRadians) * yScale;
        
        double endX = cx + radius * Math.cos(startAngleRadians + sweepAngleRadians);
        double endY = cy + radius * Math.sin(startAngleRadians + sweepAngleRadians) * yScale;
        if (joinPath) {
            path.lineTo(startX, startY);
        } else {
            path.moveTo(startX, startY);
        }
        addBezierArcToPath(path, cx, cy, startX, startY, endX, endY);
        
    }
    
     /**
     * Normalize the input radians in the range 360? > x >= 0?.
     *
     * @param radians The angle to normalize (in radians).
     *
     * @return The angle normalized in the range 360? > x >= 0?.
     */
    private static double normalizeRadians(double radians)
    {
        double PI2 = Math.PI*2d;
        radians %= PI2;
        if (radians < 0d) { radians += PI2; }
        if (radians == PI2) { radians = 0d; }
        return radians;
    }
    
    
    /**
     * Adds an arc to the path.  This method uses an approximation of an arc using
     * a cubic path.  It is not a precise arc.
     * 
     * <p>Note:  The arc is drawn counter-clockwise around the center point.  See {@link #arcTo(double, double, double, double, boolean) } 
     * to draw clockwise.</p>
     * 
     * @param cX The x-coordinate of the oval center.
     * @param cY The y-coordinate of the oval center.
     * @param endX The end X coordinate.
     * @param endY The end Y coordinate.
     */
    public void arcTo(float cX, float cY, float endX, float endY) {
        arcTo(cX, cY, endX, endY, false);
    }
    /**
     * Adds an arc to the path.  This method uses an approximation of an arc using
     * a cubic path.  It is not a precise arc.
     * @param cX The x-coordinate of the oval center.
     * @param cY The y-coordinate of the oval center.
     * @param endX The end X coordinate.
     * @param endY The end Y coordinate.
     * @param clockwise If true, the arc is drawn clockwise around the center point.
     */
    public void arcTo(float cX, float cY, float endX, float endY, boolean clockwise){
        if ( pointSize < 2 ){
            throw new RuntimeException("Cannot add arc to path if it doesn't already have a starting point.");
            
        }
        float startX = points[pointSize-2];
        float startY = points[pointSize-1];
        
        float dx = endX-cX;
        float dy = endY-cY;
        double r2 = Math.sqrt(dx*dx+dy*dy);
        double dx1 = startX-cX;
        double dy1 = startY-cY;
        double r1 = Math.sqrt(dx1*dx1*dy1*dy1);
        if (Math.abs(r1-r2) > 1) {
            Log.p("arcTo() called with start and end points that don't lie on the same arc.", Log.WARNING);
        }
        Ellipse e = new Ellipse();
        Ellipse.initWithBounds(e, cX-r2, cY-r2, r2*2, r2*2);
        double startAngle = e.getAngleAtPoint(startX, startY);
        double endAngle = e.getAngleAtPoint(endX, endY);
        double sweepAngle = endAngle-startAngle;
        if (clockwise && sweepAngle > 0) {
            sweepAngle = -sweepAngle;
        } else if (!clockwise && sweepAngle > 0) {
            sweepAngle = 2*Math.PI-sweepAngle;
        }
        
        arc(cX-r2, cY-r2, r2*2, r2*2, -startAngle, sweepAngle, true);
        lineTo(endX, endY);
    }
    
    /**
     * Adds an arc to the path.  This method uses an approximation of an arc using
     * a cubic path.  It is not a precise arc.
     * <p>Note:  The arc is drawn counter-clockwise around the center point.  See {@link #arcTo(double, double, double, double, boolean) } 
     * to draw clockwise.</p>
     * @param cX The x-coordinate of the oval center.
     * @param cY The y-coordinate of the oval center.
     * @param endX The end X coordinate.
     * @param endY The end Y coordinate.
     */
    public void arcTo(double cX, double cY, double endX, double endY) {
        arcTo(cX, cY, endX, endY, false);
    }
    
    /**
     * Adds an arc to the path.  This method uses an approximation of an arc using
     * a cubic path.  It is not a precise arc.
     * @param cX The x-coordinate of the oval center.
     * @param cY The y-coordinate of the oval center.
     * @param endX The end X coordinate.
     * @param endY The end Y coordinate.
     * @param clockwise If true, the arc is drawn clockwise around the center point.
     * 
     */
    public void arcTo(double cX, double cY, double endX, double endY, boolean clockwise){
        arcTo((float)cX, (float)cY, (float)endX, (float)endY, clockwise);
    }

    /**
     * Closes the current subpath by drawing a line back to the point of the
     * last moveTo, unless the path is already closed.
     */
    public void closePath() {
        if (typeSize == 0 || types[typeSize - 1] != PathIterator.SEG_CLOSE) {
            checkBuf(0, true);
            types[typeSize++] = PathIterator.SEG_CLOSE;
            dirty = true;
        }
    }

    /**
     * Appends the segments of a Shape to the path. If connect is
     * {@literal true}, the new path segments are connected to the existing one
     * with a line. The winding rule of the Shape is ignored.
     *
     * @param shape the shape (null not permitted).
     * @param connect whether to connect the new shape to the existing path.
     */
    public void append(Shape shape, boolean connect) {
        if (shape.getClass() == GeneralPath.class) {
            Iterator it = createIteratorFromPool((GeneralPath)shape, null);
            try {
                append(it, connect);
            } finally {
                recycle(it);
            }
        } else {
            PathIterator p = shape.getPathIterator();
            append(p, connect);
        }
        
        dirty = true;
    }

    /**
     * Appends the segments of a PathIterator to this GeneralPath. Optionally,
     * the initial {@link PathIterator#SEG_MOVETO} segment of the appended path
     * is changed into a {@link PathIterator#SEG_LINETO} segment.
     *
     * @param path the PathIterator specifying which segments shall be appended
     * (null not permitted).
     * @param connect {@literal true} for substituting the initial
     * {@link PathIterator#SEG_MOVETO} segment by a
     * {@link PathIterator#SEG_LINETO}, or false for not performing any
     * substitution. If this {@code GeneralPath} is currently empty, connect is
     * assumed to be {@literal false}, thus leaving the initial
     * {@link PathIterator#SEG_MOVETO} unchanged.
     */
    public void append(PathIterator path, boolean connect) {
        float coords[] = createFloatArrayFromPool(6);//new float[6];
        append(path, connect, coords);
        recycle(coords);
    }
    
    private void append(PathIterator path, boolean connect, float[] tmpCoordsBuf) {
        float coords[] = tmpCoordsBuf;
        while (!path.isDone()) {
            
            switch (path.currentSegment(coords)) {
                case PathIterator.SEG_MOVETO:
                    if (!connect || typeSize == 0) {
                        moveTo(coords[0], coords[1]);
                        break;
                    }
                    if (types[typeSize - 1] != PathIterator.SEG_CLOSE
                            && points[pointSize - 2] == coords[0]
                            && points[pointSize - 1] == coords[1]) {
                        break;
                    }
                // NO BREAK;
                case PathIterator.SEG_LINETO:
                    lineTo(coords[0], coords[1]);
                    break;
                case PathIterator.SEG_QUADTO:
                    quadTo(coords[0], coords[1], coords[2], coords[3]);
                    break;
                case PathIterator.SEG_CUBICTO:
                    curveTo(coords[0], coords[1], coords[2], coords[3], coords[4], coords[5]);
                    break;
                case PathIterator.SEG_CLOSE:
                    closePath();
                    break;
            }
            path.next();
            connect = false;
        }
        dirty = true;
    }

    /**
     * Returns the current appending point of the path.
     *
     * @return 2-element array of the form {@code [x,y]} representing {@code x}
     * and {@code y} coordinate of the current appending point of the path..
     */
    public float[] getCurrentPoint() {
        if (typeSize == 0) {
            return null;
        }
        float[] out = new float[2];
        getCurrentPoint(out);
        return out;
    }
    
    /**
     * Sets the coordinates of the given point to the current point in the path.
     *
     * @param point Out parameter.  Will be filled with the coords of the current point.
     */
    public void getCurrentPoint(float[] point) {
        if (typeSize == 0) {
            throw new RuntimeException("Cannot get point because the size of this command is 0");
        }
        int j = pointSize - 2;
        if (types[typeSize - 1] == PathIterator.SEG_CLOSE) {

            for (int i = typeSize - 2; i > 0; i--) {
                int type = types[i];
                if (type == PathIterator.SEG_MOVETO) {
                    break;
                }
                j -= pointShift[type];
            }
        }
        point[0] = points[j];
        point[1] = points[j+1];
        //return new float[]{points[j], points[j + 1]};
    }

    /**
     * Resets the path. All points and segments are destroyed.
     */
    public void reset() {
        typeSize = 0;
        pointSize = 0;
        dirty = true;
    }

    /**
     * Returns the path's bounding box, in float precision.
     *
     * @return 4-element array of the form {@code [x, y, width, height]}.
     */
    public float[] getBounds2D() {
        float[] out = new float[4];
        getBounds2D(out);
        return out;
    }
    
    /**
     * Sets the 4-element array to the bounding box coordinates of the path.  x, y, width, height.
     * @param out 4-element float[] array.
     */
    public void getBounds2D(float[] out) {
        float rx1, ry1, rx2, ry2;
        if (pointSize == 0) {
            rx1 = ry1 = rx2 = ry2 = 0.0f;
        } else {
            int i = pointSize - 1;
            ry1 = ry2 = points[i--];
            rx1 = rx2 = points[i--];
            while (i > 0) {
                float y = points[i--];
                float x = points[i--];
                if (x < rx1) {
                    rx1 = x;
                } else if (x > rx2) {
                    rx2 = x;
                }
                if (y < ry1) {
                    ry1 = y;
                } else if (y > ry2) {
                    ry2 = y;
                }
            }
        }
        out[0] = rx1;
        out[1] = ry1;
        out[2] = rx2-rx1;
        out[3] = ry2-ry1;
    }

    /**
     * Returns the path's bounding box.
     *
     * @return The bounding box of the path.
     */
    public Rectangle getBounds() {
        
        float[] r = getBounds2D();
        int x1 = (int)Math.floor(r[0]);
        int y1 = (int)Math.floor(r[1]);
        int x2 = (int)Math.ceil(r[0]+r[2]);
        int y2 = (int)Math.ceil(r[1]+r[3]);
        return new Rectangle(x1, y1, x2-x1, y2-y1);
        /*
        float[] r = getBounds2D();
        return new Rectangle((int)r[0], (int)r[1], (int)r[2], (int)r[3]);*/

    }
    
    /**
     * Sets the coordinates of the provided rectangle to the bounding box of this path.
     * @param out 
     */
    public void getBounds(Rectangle out) {
        float rx1, ry1, rx2, ry2;
        if (pointSize == 0) {
            rx1 = ry1 = rx2 = ry2 = 0.0f;
        } else {
            int i = pointSize - 1;
            ry1 = ry2 = points[i--];
            rx1 = rx2 = points[i--];
            while (i > 0) {
                float y = points[i--];
                float x = points[i--];
                if (x < rx1) {
                    rx1 = x;
                } else if (x > rx2) {
                    rx2 = x;
                }
                if (y < ry1) {
                    ry1 = y;
                } else if (y > ry2) {
                    ry2 = y;
                }
            }
        }
        int x1 = (int)Math.floor(rx1);
        int y1 = (int)Math.floor(ry1);
        int x2 = (int)Math.ceil(rx2);
        int y2 = (int)Math.ceil(ry2);
        out.setX(x1);
        out.setY(y1);
        out.setWidth(x2-x1);
        out.setHeight(y2-y1);
        
    }
    
    /**
     * Checks to see if this path is a rectangle.
     * @return True if this path forms a rectangle.  False otherwise.
     */
    public boolean isRectangle() {
        float[] tmpPointsBuf = createFloatArrayFromPool(6);
        boolean[] tmpCornersBuf = createBoolArrayFromPool(4);
        Iterator it = createIteratorFromPool(this, null);
        Rectangle bounds = createRectFromPool();
        try {
            getBounds(bounds);
            if (tmpPointsBuf.length != 6) {
                throw new RuntimeException("points buffer must be length 6");
            }
            float[] buf = tmpPointsBuf;
            if (tmpCornersBuf.length != 4) {
                throw new RuntimeException("corners buffer must be length 4");
            }
            boolean[] corners = tmpCornersBuf;
            int prevX = 0; 
            int prevY = 0;
            while ( !it.isDone() ){
                int type = it.currentSegment(buf);

                // Rectangulars only support moves, lines, and closes
                if ( type != PathIterator.SEG_CLOSE && type != PathIterator.SEG_LINETO && type != PathIterator.SEG_MOVETO ){
                    return false;
                }


                // Get the current point
                int x = (int)buf[0];
                int y = (int)buf[1];

                // Make sure there are no diagonal lines
                if ( type == PathIterator.SEG_LINETO && !(x == prevX || y == prevY )){
                    return false;
                }

                // Make sure point is on the perimeter.
                if ( x != bounds.getX() && y != bounds.getY() && x != bounds.getX()+bounds.getWidth() && y != bounds.getY()+bounds.getHeight() ){
                    return false;
                }

                // Make sure that all corners are accounted for.
                for ( int i=0; i<4; i++){
                    if ( corners[i] ){
                        continue;
                    }
                    switch (i){
                        case 0:
                            corners[i] = (x == bounds.getX() && y == bounds.getY());
                            break;
                        case 1:
                            corners[i] = (x == bounds.getX()+bounds.getWidth() && y == bounds.getY());
                            break;
                        case 2:
                            corners[i] = (x == bounds.getX()+bounds.getWidth() && y == bounds.getY() + bounds.getHeight());
                            break;
                        case 3:
                            corners[i] = (x== bounds.getX() && y == bounds.getY()+bounds.getHeight());
                            break;
                    }
                }

                prevX = x;
                prevY = y;
                it.next();
            }

            return corners[0] && corners[1] && corners[2] && corners[3];
        } finally {
            recycle(tmpPointsBuf);
            recycle(tmpCornersBuf);
            recycle(it);
            recycle(bounds);
        }
    }
    
    
    /**
     * {{@inheritDoc}}
     */
    public PathIterator getPathIterator() {
        return new Iterator(this);
    }

    /**
     * {{@inheritDoc}}
     */
    public PathIterator getPathIterator(Transform m) {
        Iterator out = (Iterator) getPathIterator();
        out.transform = m;
        return out;
    }
    
    /**
     * Returns a shape formed by transforming the current shape with the provided
     * transform.  
     * <p>Note: If {@link com.codename1.ui.Transform#isSupported} is false, this may throw a RuntimeException.</p>
     * @param m The transform to be used to transform the shape.
     * @return The transformed shape.
     */
    public Shape createTransformedShape(Transform m){
        
        GeneralPath out = new GeneralPath();
        out.setPath(this, m);
        return out;
    }
    
    /**
     * Sets this path to be identical to the provided path {@code p} with the given
     * Transform {@code t} applied to it.
     * @param p The path to copy.
     * @param t The transform to apply to all points in the path.
     */
    public void setPath(GeneralPath p, Transform t) {
        dirty = true;
        typeSize = p.typeSize;
        pointSize = p.pointSize;
        rule = p.rule;
        if (points == null || points.length < pointSize) {
            points = new float[pointSize];
        }
        if (types == null || types.length < typeSize) {
            types = new byte[typeSize];
        }
        System.arraycopy(p.types, 0, types, 0, typeSize);
        if (t == null || t.isIdentity()) {
            System.arraycopy(p.points, 0, points, 0, pointSize);
            
        } else {
            t.transformPoints(2, p.points, 0, points, 0, pointSize / 2);
            
        }
        
    }
    
    /**
     * Sets this path to be a rectangle with the provided bounds, but with 
     * the given transform applied to it.
     * @param r Rectangle to copy.
     * @param t The transform to apply to the points in in {@code r}.
     */
    public void setRect(Rectangle r, Transform t) {
        reset();
        int x = r.getX();
        int y = r.getY();
        Dimension size = r.getSize();
        int w = size.getWidth();
        int h = size.getHeight();

        if (t == null) {
            moveTo(x, y);
            lineTo(x + w, y);
            lineTo(x + w, y + h);
            lineTo(x, y+ h);
            closePath();
        } else {
            float[] pointBuffer = createFloatArrayFromPool(6);
            try {
                pointBuffer[0] = x;
                pointBuffer[1] = y;
                pointBuffer[2] = 0;
                t.transformPoint(pointBuffer, pointBuffer);
                moveTo(pointBuffer[0], pointBuffer[1]);
                pointBuffer[0] = x+w;
                pointBuffer[1] = y;
                pointBuffer[2] = 0;
                t.transformPoint(pointBuffer, pointBuffer);
                lineTo(pointBuffer[0], pointBuffer[1]);
                pointBuffer[0] = x+w;
                pointBuffer[1] = y+h;
                pointBuffer[2] = 0;
                t.transformPoint(pointBuffer, pointBuffer);
                lineTo(pointBuffer[0], pointBuffer[1]);
                pointBuffer[0] = x;
                pointBuffer[1] = y+h;
                pointBuffer[2] = 0;
                t.transformPoint(pointBuffer, pointBuffer);
                lineTo(pointBuffer[0], pointBuffer[1]);
                closePath();
            } finally {
                recycle(pointBuffer);
            }
        }
    }
    
    /**
     * Sets this path to be a copy of the provided shape, but with the provided
     * transform applied to it.
     * @param s The shape to copy.
     * @param t The transform to apply to all points in the shape.
     */
    public void setShape(Shape s, Transform t) {
        if (s.getClass() == GeneralPath.class) {
            setPath((GeneralPath)s, t);
        } else if (s.getClass() == Rectangle.class) {
            setRect((Rectangle)s, t);
        } else {
            reset();
            append(s.getPathIterator(t), false);
        }
    }
    
    /**
     * Sets the current path to the intersection of itself and the provided rectangle.
     * @param rect The rectangle to intersect with this path.
     * @return True if {@code rect} intersects the current path.  False otherwise.  If there is no intersection, the
     * path will be reset to be empty.
     */
    public boolean intersect(Rectangle rect) {
        GeneralPath intersectionScratchPath = createPathFromPool();
        try {
            
            Shape result = ShapeUtil.intersection(rect, this, intersectionScratchPath);
            if (result != null) {
                this.setPath(intersectionScratchPath, null);
                return true;
            }
            reset();
            return false;

        } finally {
            recycle(intersectionScratchPath);
        }
    }
    
    public boolean intersect(int x, int y, int w, int h) {
        Rectangle r = createRectFromPool();
        try {
            r.setBounds(x, y, w, h);
            return intersect(r);
        } finally {
            recycle(r);
        }
    }
    
    /**
     * Transforms the current path in place using the given transform.
     * @param m The transform to apply to the path.
     */
    public void transform(Transform m) {
        if (m != null && !m.isIdentity()) {
            m.transformPoints(2, points, 0, points, 0, pointSize / 2 );
        }
    }
    
    /**
     * Resets this path to be the intersection of itself with the given shape.  Note that only 
     * {@link com.codename1.ui.geom.Rectangle}s are current supported.  If you pass any other
     * shape, it will throw a RuntimeException.
     * <p>Note: If {@link com.codename1.ui.TransformisSupported} is false, this will throw a Runtime Exception</p>
     * @param shape The shape to intersect with the current shape.
     */
    public void intersect(Shape shape) {
        //Log.p("Start intersect");
        if ( !(shape instanceof Rectangle) ){
            throw new RuntimeException("GeneralPath.intersect() only supports Rectangles");
        }
        intersect((Rectangle)shape);
    }
    
    /**
     * {{@inheritDoc}}
     */
    public Shape intersection(Rectangle rect){
        Shape out = ShapeUtil.intersection(rect, this);
        if (out == null) {
            return new Rectangle(rect.getX(), rect.getY(), 0, 0);
        }
        return out;
    }
    
    /**
     * Checks cross count according to path rule to define is it point inside shape or not. 
     * @param cross - the point cross count
     * @return true if point is inside path, or false otherwise 
     */
    boolean isInside(int cross) {
        if (rule == WIND_NON_ZERO) {
            return ShapeUtil.isInsideNonZero(cross);
        }
        return ShapeUtil.isInsideEvenOdd(cross);
    }

    /**
     * Checks if the given point is contained in the current shape.
     * @param x The x coordinate to check
     * @param y The y coordinate to check
     * @return True if the point is inside the shape.
     */
    public boolean contains(float x, float y) {
       return isInside(ShapeUtil.crossShape(this, x, y));
    }
    
    /**
     * {{@inheritDoc}}
     */
    public boolean contains(int x, int y){
        return contains((float)x, (float)y);
    }

    /**
     *
     * @author shannah
     */
    private static class ShapeUtil {
    
    
        

    /**
     * Generates the intersection of a given shape and a given rectangle.  Only supported convex polygons.
     *
     * @param r A rectangle.
     * @param s A shape
     * @return The shape that is the intersected area of the shape and
     * rectangle.
     */
    static Shape intersection(Rectangle r, Shape s) {
        return intersection(r, s, new GeneralPath());
    }
        
    
    private static Shape intersection(Rectangle r, Shape s, GeneralPath out) {
        
        Shape segmentedShape = segmentShape(r, s);
        Iterator it = createIteratorFromPool((GeneralPath)segmentedShape, null);
        //GeneralPath out = new GeneralPath();
        float[] buf = createFloatArrayFromPool(6);//new float[6];
        try {
            boolean started = false;
            float x1 = r.getX();
            float x2 = r.getX() + r.getWidth();
            float y1 = r.getY();
            float y2 = r.getY() + r.getHeight();

            float minX = -1;
            float minY = -1;
            float maxX = -1;
            float maxY = -1;

            float prevX=0; 
            float prevY=0;

            while (!it.isDone()) {
                int type = it.currentSegment(buf);

                switch (type) {

                    case PathIterator.SEG_CLOSE:
                        //System.out.println("Closing path");
                        out.closePath();
                        break;

                    case PathIterator.SEG_MOVETO:
                    case PathIterator.SEG_LINETO:
                        if (buf[0] < x1) {
                            buf[0] = x1;
                        } else if (buf[0] > x2) {
                            buf[0] = x2;
                        }
                        if (buf[1] < y1) {
                            buf[1] = y1;
                        } else if (buf[1] > y2) {
                            buf[1] = y2;
                        }

                        if (!started || (buf[0] < minX)) {
                            minX = buf[0];
                        }
                        if (!started || (buf[0] > maxX)) {
                            maxX = buf[0];
                        }

                        if (!started || (buf[1] < minY)) {
                            minY = buf[1];
                        }
                        if (!started || (buf[1] > maxY)) {
                            maxY = buf[1];
                        }

                        if (type == PathIterator.SEG_MOVETO) {

                            //System.out.println("Moving to "+buf[0]+","+buf[1]);
                            out.moveTo(buf[0], buf[1]);
                        } else { // type == PathITerator.SEG_LINETO

                            if ( prevX != buf[0] || prevY != buf[1]){
                                //System.out.println("Line to "+buf[0]+","+buf[1]);
                                out.lineTo(buf[0], buf[1]);
                            }
                        }
                        prevX = buf[0];
                        prevY = buf[1];
                        started = true;
                        //count++;
                        break;
                    default:
                        throw new RuntimeException("Intersection only supports polygons currently");
                }
                it.next();

            }

            if (maxX - minX <= 1f || maxY - minY <= 1f) {
                return null;
            }

            return out;
        } finally {
            recycle(it);
            recycle(buf);
        }

    }

    /**
     * Segments a given shape so that all points of the shape that intersect the
     * provided rectangle edges are nodes of the shape path. This operation
     * makes it easier to form the intersection.
     * 
     * Only supports convex polygons.
     *
     * @param r A rectangle.
     * @param s A shape
     * @return A shape that is identical to the input shape except that it may
     * include additional path segments so that all points of intersection are
     * start/end points of a segment.
     */
    static Shape segmentShape(Rectangle r, Shape s) {
        return segmentShape(r, s, new GeneralPath());
    }
    private static GeneralPath segmentShape(Rectangle r, Shape s, GeneralPath out) {
        GeneralPath tmpGeneralPath = null;
        if (s.getClass() != GeneralPath.class) {
            tmpGeneralPath = createPathFromPool();
            tmpGeneralPath.setShape(s, null);
            s = tmpGeneralPath;
        }
        
        Iterator it = createIteratorFromPool((GeneralPath)s, null);
        //GeneralPath out = new GeneralPath();
        float[] buf = createFloatArrayFromPool(6); // buffer to hold segment coordinates from PathIterator.currentSegment
        float[] curr = createFloatArrayFromPool(2);    // Placeholder for current point
        float[] prev = createFloatArrayFromPool(2);   // Placeholder for previous point
        float[] mark = createFloatArrayFromPool(2);   // Placeholder for the moveTo point
        //float[] buf4 = new float[4];    // Reusable buffer to hold two points.
        float[] intersects = createFloatArrayFromPool(9);
        try {
            float prevX = -1;               // Placeholder for previous X coord.
            float prevY = -1;               // Placeholder for previous Y coord.
            float currX = 0;                // Placeholder for current X coord.
            float currY = 0;                // Placeholder for current Y coord.
            //float[] intersects = null;      // Placeholder for intersection points
            while (!it.isDone()) {

                int type = it.currentSegment(buf);
                switch (type) {

                    case PathIterator.SEG_MOVETO:
                        // Move to segment is transferred straight through
                        prevX = prev[0] = mark[0] = buf[0];
                        prevY = prev[1] = mark[1] = buf[1];
                        out.moveTo(prevX, prevY);

                        //System.out.println("Moving to "+prevX+","+prevY);
                        break;

                    case PathIterator.SEG_LINETO:
                        // Line Segment may need to be partitioned if it crosses
                        // an edge of the rectangle.
                        currX = curr[0] = buf[0];
                        currY = curr[1] = buf[1];

                        // Check if line intersects rectangle
                        intersectLineWithRectAsHash(prevX, prevY, currX, currY, r, intersects);
                        //System.out.println("Looking for intersections between "+prevX+","+prevY+" and "+currX+","+currY);
                        //System.out.println("Intersects: "+intersects[0]+", "+intersects[1]+"  "+intersects[2]+","+intersects[3]);
                        if (intersects[8] >= 1) {
                            int num = (int)intersects[8];
                            int len = num*2;
                            for ( int i=0; i<len; i+=2){
                                out.lineTo(intersects[i], intersects[i+1]);

                            }

                        } 
                        //System.out.println("Line to "+currX+","+currY);
                        out.lineTo(currX, currY);

                        // Set current position to prev for next iteration.
                        prevX = currX;
                        prevY = currY;
                        float[] tmp = curr;
                        curr = prev;
                        prev = tmp;

                        break;
                    case PathIterator.SEG_CLOSE:

                        // Closing the path.  Need to check if there is an intersection
                        // on this last closing path.
                        currX = curr[0] = mark[0];
                        currY = curr[1] = mark[1];
                        intersectLineWithRectAsHash(prevX, prevY, currX, currY, r, intersects);
                        if (intersects[8] >= 1) {
                            int num = (int)intersects[8];
                            int len = num*2;
                            for ( int i=0; i<len; i+=2){
                                out.lineTo(intersects[i], intersects[i+1]);

                            }

                        } 
                        out.closePath();

                        break;
                    default:
                        throw new RuntimeException("Shape segmentation only supported for polygons");
                }
                it.next();
            }
            return out;
        } finally {
            recycle(it);
            recycle(buf);
            recycle(curr);
            recycle(prev);
            recycle(mark);
            recycle(intersects);
            recycle(tmpGeneralPath);
        }
    }

    private static float[] intersectLineWithRectAsHash(float x1, float y1, float x2, float y2, Rectangle rect,
            float[] out
            ){
        //float[] out = new float[9]; // max 4 points here
        float[] x = createFloatArrayFromPool(4);
        try {
            //float[] y = new float[4];
            float rx1 = rect.getX();
            float ry1 = rect.getY();
            float rx2 = rect.getX()+rect.getWidth();
            float ry2 = rect.getY()+rect.getHeight();

            float dx = x2-x1;
            float dy = y2-y1;
            int num=0;

            float minY = Math.min(y1,y2);
            float maxY = Math.max(y1,y2);
            float minX = Math.min(x1, x2);
            float maxX = Math.max(x1, x2);
            int i = 0;
            if ( dx == 0 ){
                if ( ry1 > minY && ry1 < maxY ){
                    num++;
                    x[i++] = ry1;
                    //out[i++] = ry1;
                }
                if ( ry2 > minY && ry2 < maxY ){
                    num++;
                    x[i++] = ry2;
                    //out[i++] = ry2;
                }

                Arrays.sort(x, 0, num);
                if ( y1 <= y2 ){
                    for ( i=0; i<num; i++){
                        int j = 2*i;
                        out[j] = x1;
                        out[j+1] = x[i];
                    }
                } else {
                    for ( i=0; i<num; i++){
                        int j = 2*(num-i-1);
                        out[j] = x1;
                        out[j+1] = x[i];
                    }
                }


                out[8] = num;


            } else if ( dy == 0 ){
                if ( rx1 > minX && rx1 < maxX ){
                    num++;
                    x[i++] = rx1;
                    //out[i++] = y1;
                }
                if ( rx2 > minX && rx2 < maxX ){
                    num++;
                    x[i++] = rx2;
                    //out[i++] = y1;
                }
                Arrays.sort(x, 0, num);
                if ( x1 <= x2 ){
                    for ( i=0; i<num; i++){
                        int j = 2*i;
                        out[j] = x[i];
                        out[j+1] = y1;
                    }
                } else {
                    for ( i=0; i<num; i++){
                        int j = 2*(num-i-1);
                        out[j] = x[i];
                        out[j+1] = y1;
                    }
                }
                out[8] = num;
            } else {
                float m = dy/dx;

                if ( rx1 > minX && rx1 < maxX ){
                    num++;
                    x[i] = rx1;
                    //y[i] = y1+(rx1-x1)*m;
                    i++;
                }
                if ( rx2 > minX && rx2 < maxX ){
                    num++;
                    x[i] = rx2;
                    //y[i++] = y1+(rx2-x1)*m;
                    i++;
                }
                if ( ry1 > minY && ry1 < maxY ){
                    num++;
                    x[i] = x1+(ry1-y1)/m;
                    //out[i++] = ry1;
                    i++;
                }
                if ( ry2 > minY && ry2 < maxY ){
                    num++;
                    x[i] = x1+(ry2-y1)/m;
                    //out[i++] = ry2;
                    i++;
                }

                Arrays.sort(x, 0, num);
                if ( x1 < x2 ){
                    for ( i=0; i<num; i++){
                        int j = 2*i;
                        out[j] = x[i];
                        out[j+1] = y1 + (x[i]-x1)*m;
                    }
                } else {
                    for ( i=0; i<num; i++){
                        int j = 2*(num-i-1);
                        out[j] = x[i];
                        out[j+1] = y1 + (x[i]-x1)*m;
                    }
                }
                out[8] = num;
            }
            return out;
        } finally {
            recycle(x);
        }
        
    }
    
    /**
     * Allowable tolerance for bounds comparison
     */
    static final double DELTA = 1E-5;
    
    /**
     * If roots have distance less then <code>ROOT_DELTA</code> they are double
     */
    static final double ROOT_DELTA = 1E-10;
    
    /**
     * Rectangle cross segment
     */
    public static final int CROSSING = 255;
    
    /**
     * Unknown crossing result
     */
    static final int UNKNOWN = 254;

    /**
     * Solves quadratic equation
     * @param eqn - the coefficients of the equation
     * @param res - the roots of the equation
     * @return a number of roots
     */
    public static int solveQuad(double eqn[], double res[]) {
        double a = eqn[2];
        double b = eqn[1];
        double c = eqn[0];
        int rc = 0;
        if (a == 0.0) {
            if (b == 0.0) {
                return -1;
            }
            res[rc++] = -c / b;
        } else {
            double d = b * b - 4.0 * a * c;
            // d < 0.0
            if (d < 0.0) {
                return 0;
            }
            d = Math.sqrt(d);
            res[rc++] = (- b + d) / (a * 2.0);
            // d != 0.0
            if (d != 0.0) {
                res[rc++] = (- b - d) / (a * 2.0);
            }
        }
        return fixRoots(res, rc);
    }

    /**
     * Solves cubic equation
     * @param eqn - the coefficients of the equation
     * @param res - the roots of the equation
     * @return a number of roots
     */
    public static int solveCubic(double eqn[], double res[]) {
        double d = eqn[3];
        if (d == 0) {
            return solveQuad(eqn, res);
        }
        double a = eqn[2] / d;
        double b = eqn[1] / d;
        double c = eqn[0] / d;
        int rc = 0;

        double Q = (a * a - 3.0 * b) / 9.0;
        double R = (2.0 * a * a * a - 9.0 * a * b + 27.0 * c) / 54.0;
        double Q3 = Q * Q * Q;
        double R2 = R * R;
        double n = - a / 3.0;

        if (R2 < Q3) {

            double t = MathUtil.acos(R / Math.sqrt(Q3)) / 3.0;
            double p = 2.0 * Math.PI / 3.0;
            double m = -2.0 * Math.sqrt(Q);
            res[rc++] = m * Math.cos(t) + n;
            res[rc++] = m * Math.cos(t + p) + n;
            res[rc++] = m * Math.cos(t - p) + n;
        } else {
//          Debug.println("R2 >= Q3 (" + R2 + "/" + Q3 + ")");

            double A = MathUtil.pow(Math.abs(R) + Math.sqrt(R2 - Q3), 1.0 / 3.0);

            if (R > 0.0) {
                A = -A;
            }
//          if (A == 0.0) {
            if (-ROOT_DELTA < A && A < ROOT_DELTA) {
                res[rc++] = n;
            } else {
                double B = Q / A;
                res[rc++] = A + B + n;
//              if (R2 == Q3) {
                double delta = R2 - Q3;
                if (-ROOT_DELTA < delta && delta < ROOT_DELTA) {
                    res[rc++] = - (A + B) / 2.0 + n;
                }
            }

        }
        return fixRoots(res, rc);
    }

    /**
     * Excludes double roots. Roots are double if they lies enough close with each other. 
     * @param res - the roots 
     * @param rc - the roots count
     * @return new roots count
     */
    static int fixRoots(double res[], int rc) {
        int tc = 0;
        for(int i = 0; i < rc; i++) {
            out: {
                for(int j = i + 1; j < rc; j++) {
                    if (isZero(res[i] - res[j])) {
                        break out;
                    }
                }
                res[tc++] = res[i];
            }
        }
        return tc;
    }

    /**
     * QuadCurve class provides basic functionality to find curve crossing and calculating bounds
     */
    public static class QuadCurve {

        double ax, ay, bx, by;
        double Ax, Ay, Bx, By;

        public QuadCurve(double x1, double y1, double cx, double cy, double x2, double y2) {
            ax = x2 - x1;
            ay = y2 - y1;
            bx = cx - x1;
            by = cy - y1;

            Bx = bx + bx;   // Bx = 2.0 * bx
            Ax = ax - Bx;   // Ax = ax - 2.0 * bx

            By = by + by;   // By = 2.0 * by
            Ay = ay - By;   // Ay = ay - 2.0 * by
        }

        int cross(double res[], int rc, double py1, double py2) {
            int cross = 0;

            for (int i = 0; i < rc; i++) {
                double t = res[i];

                // CURVE-OUTSIDE
                if (t < -DELTA || t > 1 + DELTA) {
                    continue;
                }
                // CURVE-START
                if (t < DELTA) {
                    if (py1 < 0.0 && (bx != 0.0 ? bx : ax - bx) < 0.0) {
                        cross--;
                    }
                    continue;
                }
                // CURVE-END
                if (t > 1 - DELTA) {
                    if (py1 < ay && (ax != bx ? ax - bx : bx) > 0.0) {
                        cross++;
                    }
                    continue;
                }
                // CURVE-INSIDE
                double ry = t * (t * Ay + By);
                // ry = t * t * Ay + t * By
                if (ry > py2) {
                    double rxt = t * Ax + bx;
                    // rxt = 2.0 * t * Ax + Bx = 2.0 * t * Ax + 2.0 * bx
                    if (rxt > -DELTA && rxt < DELTA) {
                        continue;
                    }
                    cross += rxt > 0.0 ? 1 : -1;
                }
            } // for

            return cross;
        }

        int solvePoint(double res[], double px) {
            double eqn[] = {-px, Bx, Ax};
            return solveQuad(eqn, res);
        }

        int solveExtrem(double res[]) {
            int rc = 0;
            if (Ax != 0.0) {
                res[rc++] = - Bx / (Ax + Ax);
            }
            if (Ay != 0.0) {
                res[rc++] = - By / (Ay + Ay);
            }
            return rc;
        }

        int addBound(double bound[], int bc, double res[], int rc, double minX, double maxX, boolean changeId, int id) {
            for(int i = 0; i < rc; i++) {
                double t = res[i];
                if (t > -DELTA && t < 1 + DELTA) {
                    double rx = t * (t * Ax + Bx);
                    if (minX <= rx && rx <= maxX) {
                        bound[bc++] = t;
                        bound[bc++] = rx;
                        bound[bc++] = t * (t * Ay + By);
                        bound[bc++] = id;
                        if (changeId) {
                            id++;
                        }
                    }
                }
            }
            return bc;
        }

    }

    /**
     * CubicCurve class provides basic functionality to find curve crossing and calculating bounds
     */
    public static class CubicCurve {

        double ax, ay, bx, by, cx, cy;
        double Ax, Ay, Bx, By, Cx, Cy;
        double Ax3, Bx2;

        public CubicCurve(double x1, double y1, double cx1, double cy1, double cx2, double cy2, double x2, double y2) {
            ax = x2 - x1;
            ay = y2 - y1;
            bx = cx1 - x1;
            by = cy1 - y1;
            cx = cx2 - x1;
            cy = cy2 - y1;

            Cx = bx + bx + bx;           // Cx = 3.0 * bx
            Bx = cx + cx + cx - Cx - Cx; // Bx = 3.0 * cx - 6.0 * bx
            Ax = ax - Bx - Cx;           // Ax = ax - 3.0 * cx + 3.0 * bx

            Cy = by + by + by;           // Cy = 3.0 * by
            By = cy + cy + cy - Cy - Cy; // By = 3.0 * cy - 6.0 * by
            Ay = ay - By - Cy;           // Ay = ay - 3.0 * cy + 3.0 * by

            Ax3 = Ax + Ax + Ax;
            Bx2 = Bx + Bx;
        }

        int cross(double res[], int rc, double py1, double py2) {
            int cross = 0;
            for (int i = 0; i < rc; i++) {
                double t = res[i];

                // CURVE-OUTSIDE
                if (t < -DELTA || t > 1 + DELTA) {
                    continue;
                }
                // CURVE-START
                if (t < DELTA) {
                    if (py1 < 0.0 && (bx != 0.0 ? bx : (cx != bx ? cx - bx : ax - cx)) < 0.0) {
                        cross--;
                    }
                    continue;
                }
                // CURVE-END
                if (t > 1 - DELTA) {
                    if (py1 < ay && (ax != cx ? ax - cx : (cx != bx ? cx - bx : bx)) > 0.0) {
                        cross++;
                    }
                    continue;
                }
                // CURVE-INSIDE
                double ry = t * (t * (t * Ay + By) + Cy);
                // ry = t * t * t * Ay + t * t * By + t * Cy
                if (ry > py2) {
                    double rxt = t * (t * Ax3 + Bx2) + Cx;
                    // rxt = 3.0 * t * t * Ax + 2.0 * t * Bx + Cx
                    if (rxt > -DELTA && rxt < DELTA) {
                        rxt = t * (Ax3 + Ax3) + Bx2;
                        // rxt = 6.0 * t * Ax + 2.0 * Bx
                        if (rxt < -DELTA || rxt > DELTA) {
                            // Inflection point
                            continue;
                        }
                        rxt = ax;
                    }
                    cross += rxt > 0.0 ? 1 : -1;
                }
            } //for

            return cross;
        }

        int solvePoint(double res[], double px) {
            double eqn[] = {-px, Cx, Bx, Ax};
            return solveCubic(eqn, res);
        }

        int solveExtremX(double res[]) {
            double eqn[] = {Cx, Bx2, Ax3};
            return solveQuad(eqn, res);
        }

        int solveExtremY(double res[]) {
            double eqn[] = {Cy, By + By, Ay + Ay + Ay};
            return solveQuad(eqn, res);
        }

        int addBound(double bound[], int bc, double res[], int rc, double minX, double maxX, boolean changeId, int id) {
            for(int i = 0; i < rc; i++) {
                double t = res[i];
                if (t > -DELTA && t < 1 + DELTA) {
                    double rx = t * (t * (t * Ax + Bx) + Cx);
                    if (minX <= rx && rx <= maxX) {
                        bound[bc++] = t;
                        bound[bc++] = rx;
                        bound[bc++] = t * (t * (t * Ay + By) + Cy);
                        bound[bc++] = id;
                        if (changeId) {
                            id++;
                        }
                    }
                }
            }
            return bc;
        }

    }

    /**
     * Returns how many times ray from point (x,y) cross line.
     */
    public static int crossLine(double x1, double y1, double x2, double y2, double x, double y) {

        // LEFT/RIGHT/UP/EMPTY
        if ((x < x1 && x < x2) ||
            (x > x1 && x > x2) ||
            (y > y1 && y > y2) ||
            (x1 == x2))
        {
            return 0;
        }

        // DOWN
        if (y < y1 && y < y2) {
        } else {
            // INSIDE
            if ((y2 - y1) * (x - x1) / (x2 - x1) <= y - y1) {
                // INSIDE-UP
                return 0;
            }
        }

        // START
        if (x == x1) {
        	return x1 < x2 ? 0 : -1;        
        }
        
        // END
        if (x == x2) {
        	return x1 < x2 ? 1 : 0;        
        }

        // INSIDE-DOWN
        return x1 < x2 ? 1 : -1;
    }

    /**
     * Returns how many times ray from point (x,y) cross quard curve
     */
    public static int crossQuad(double x1, double y1, double cx, double cy, double x2, double y2, double x, double y) {

        // LEFT/RIGHT/UP/EMPTY
        if ((x < x1 && x < cx && x < x2) ||
            (x > x1 && x > cx && x > x2) ||
            (y > y1 && y > cy && y > y2) ||
            (x1 == cx && cx == x2))
        {
            return 0;
        }

        // DOWN
        if (y < y1 && y < cy && y < y2 && x != x1 && x != x2) {
            if (x1 < x2) {
                return x1 < x && x < x2 ? 1 : 0;
            }
            return x2 < x && x < x1 ? -1 : 0;
        }

        // INSIDE
        QuadCurve c = new QuadCurve(x1, y1, cx, cy, x2, y2);
        double px = x - x1;
        double py = y - y1;
        double res[] = new double[3];
        int rc = c.solvePoint(res, px);

        return c.cross(res, rc, py, py);
    }

    /**
     * Returns how many times ray from point (x,y) cross cubic curve
     */
    public static int crossCubic(double x1, double y1, double cx1, double cy1, double cx2, double cy2, double x2, double y2, double x, double y) {

        // LEFT/RIGHT/UP/EMPTY
        if ((x < x1 && x < cx1 && x < cx2 && x < x2) ||
            (x > x1 && x > cx1 && x > cx2 && x > x2) ||
            (y > y1 && y > cy1 && y > cy2 && y > y2) ||
            (x1 == cx1 && cx1 == cx2 && cx2 == x2))
        {
            return 0;
        }

        // DOWN
        if (y < y1 && y < cy1 && y < cy2 && y < y2 && x != x1 && x != x2) {
            if (x1 < x2) {
                return x1 < x && x < x2 ? 1 : 0;
            }
            return x2 < x && x < x1 ? -1 : 0;
        }

        // INSIDE
        CubicCurve c = new CubicCurve(x1, y1, cx1, cy1, cx2, cy2, x2, y2);
        double px = x - x1;
        double py = y - y1;
        double res[] = new double[3];
        int rc = c.solvePoint(res, px);
        return c.cross(res, rc, py, py);
    }

    /**
     * Returns how many times ray from point (x,y) cross path
     */
    public static int crossPath(PathIterator p, double x, double y) {
        int cross = 0;
        double mx, my, cx, cy;
        mx = my = cx = cy = 0.0;
        double coords[] = new double[6];

        while (!p.isDone()) {
            switch (p.currentSegment(coords)) {
            case PathIterator.SEG_MOVETO:
                if (cx != mx || cy != my) {
                    cross += crossLine(cx, cy, mx, my, x, y);
                }
                mx = cx = coords[0];
                my = cy = coords[1];
                break;
            case PathIterator.SEG_LINETO:
                cross += crossLine(cx, cy, cx = coords[0], cy = coords[1], x, y);
                break;
            case PathIterator.SEG_QUADTO:
                cross += crossQuad(cx, cy, coords[0], coords[1], cx = coords[2], cy = coords[3], x, y);
                break;
            case PathIterator.SEG_CUBICTO:
                cross += crossCubic(cx, cy, coords[0], coords[1], coords[2], coords[3], cx = coords[4], cy = coords[5], x, y);
                break;
            case PathIterator.SEG_CLOSE:
                if (cy != my || cx != mx) {
                    cross += crossLine(cx, cy, cx = mx, cy = my, x, y);
                }
                break;
            }
            
            // checks if the point (x,y) is the vertex of shape with PathIterator p           
            if (x == cx && y == cy) {
            	cross = 0;
            	cy = my;
            	break;
            }
            p.next();
        }
        if (cy != my) {
            cross += crossLine(cx, cy, mx, my, x, y);
        }
        return cross;
    }

    /**
     * Returns how many times ray from point (x,y) cross shape
     */
    public static int crossShape(Shape s, double x, double y) {
        if (!s.getBounds().contains((int)x, (int)y)) {
            return 0;
        }
        
        return crossPath(s.getPathIterator(null), x, y);
    }

    /**
     * Returns true if value enough small
     */
    public static boolean isZero(double val) {
        return -DELTA < val && val < DELTA;
    }

    /**
     * Sort bound array
     */
    static void sortBound(double bound[], int bc) {
        for(int i = 0; i < bc - 4; i += 4) {
            int k = i;
            for(int j = i + 4; j < bc; j += 4) {
                if (bound[k] > bound[j]) {
                    k = j;
                }
            }
            if (k != i) {
                double tmp = bound[i];
                bound[i] = bound[k];
                bound[k] = tmp;
                tmp = bound[i + 1];
                bound[i + 1] = bound[k + 1];
                bound[k + 1] = tmp;
                tmp = bound[i + 2];
                bound[i + 2] = bound[k + 2];
                bound[k + 2] = tmp;
                tmp = bound[i + 3];
                bound[i + 3] = bound[k + 3];
                bound[k + 3] = tmp;
            }
        }
    }
    
    /**
     * Returns are bounds intersect or not intersect rectangle 
     */
    static int crossBound(double bound[], int bc, double py1, double py2) {

        // LEFT/RIGHT
        if (bc == 0) {
            return 0;
        }

        // Check Y coordinate
        int up = 0;
        int down = 0;
        for(int i = 2; i < bc; i += 4) {
            if (bound[i] < py1) {
                up++;
                continue;
            }
            if (bound[i] > py2) {
                down++;
                continue;
            }
            return CROSSING;
        }

        // UP
        if (down == 0) {
            return 0;
        }

        if (up != 0) {
            // bc >= 2
            sortBound(bound, bc);
            boolean sign = bound[2] > py2;
            for(int i = 6; i < bc; i += 4) {
                boolean sign2 = bound[i] > py2;
                if (sign != sign2 && bound[i + 1] != bound[i - 3]) {
                    return CROSSING;
                }
                sign = sign2;
            }
        }
        return UNKNOWN;
    }

    /**
     * Returns how many times rectangle stripe cross line or the are intersect
     */
    public static int intersectLine(double x1, double y1, double x2, double y2, double rx1, double ry1, double rx2, double ry2) {

        // LEFT/RIGHT/UP
        if ((rx2 < x1 && rx2 < x2) ||
            (rx1 > x1 && rx1 > x2) ||
            (ry1 > y1 && ry1 > y2))
        {
            return 0;
        }

        // DOWN
        if (ry2 < y1 && ry2 < y2) {
        } else {

            // INSIDE
            if (x1 == x2) {
                return CROSSING;
            }

            // Build bound
            double bx1, bx2;
            if (x1 < x2) {
                bx1 = x1 < rx1 ? rx1 : x1;
                bx2 = x2 < rx2 ? x2 : rx2;
            } else {
                bx1 = x2 < rx1 ? rx1 : x2;
                bx2 = x1 < rx2 ? x1 : rx2;
            }
            double k = (y2 - y1) / (x2 - x1);
            double by1 = k * (bx1 - x1) + y1;
            double by2 = k * (bx2 - x1) + y1;

            // BOUND-UP
            if (by1 < ry1 && by2 < ry1) {
                return 0;
            }

            // BOUND-DOWN
            if (by1 > ry2 && by2 > ry2) {
            } else {
                return CROSSING;
            }
        }

        // EMPTY
        if (x1 == x2) {
            return 0;
        }

        // CURVE-START
        if (rx1 == x1) {
            return x1 < x2 ? 0 : -1;
        }

        // CURVE-END
        if (rx1 == x2) {
            return x1 < x2 ? 1 : 0;
        }

        if (x1 < x2) {
            return x1 < rx1 && rx1 < x2 ? 1 : 0;
        }
        return x2 < rx1 && rx1 < x1 ? -1 : 0;

    }

    /**
     * Returns how many times rectangle stripe cross quad curve or the are intersect
     */
    public static int intersectQuad(double x1, double y1, double cx, double cy, double x2, double y2, double rx1, double ry1, double rx2, double ry2) {

        // LEFT/RIGHT/UP ------------------------------------------------------
        if ((rx2 < x1 && rx2 < cx && rx2 < x2) ||
            (rx1 > x1 && rx1 > cx && rx1 > x2) ||
            (ry1 > y1 && ry1 > cy && ry1 > y2))
        {
            return 0;
        }

        // DOWN ---------------------------------------------------------------
        if (ry2 < y1 && ry2 < cy && ry2 < y2 && rx1 != x1 && rx1 != x2) {
            if (x1 < x2) {
                return x1 < rx1 && rx1 < x2 ? 1 : 0;
            }
            return x2 < rx1 && rx1 < x1 ? -1 : 0;
        }

        // INSIDE -------------------------------------------------------------
        QuadCurve c = new QuadCurve(x1, y1, cx, cy, x2, y2);
        double px1 = rx1 - x1;
        double py1 = ry1 - y1;
        double px2 = rx2 - x1;
        double py2 = ry2 - y1;

        double res1[] = new double[3];
        double res2[] = new double[3];
        int rc1 = c.solvePoint(res1, px1);
        int rc2 = c.solvePoint(res2, px2);

        // INSIDE-LEFT/RIGHT
        if (rc1 == 0 && rc2 == 0) {
            return 0;
        }

        // Build bound --------------------------------------------------------
        double minX = px1 - DELTA;
        double maxX = px2 + DELTA;
        double bound[] = new double[28];
        int bc = 0;
        // Add roots
        bc = c.addBound(bound, bc, res1, rc1, minX, maxX, false, 0);
        bc = c.addBound(bound, bc, res2, rc2, minX, maxX, false, 1);
        // Add extremal points`
        rc2 = c.solveExtrem(res2);
        bc = c.addBound(bound, bc, res2, rc2, minX, maxX, true, 2);
        // Add start and end
        if (rx1 < x1 && x1 < rx2) {
            bound[bc++] = 0.0;
            bound[bc++] = 0.0;
            bound[bc++] = 0.0;
            bound[bc++] = 4;
        }
        if (rx1 < x2 && x2 < rx2) {
            bound[bc++] = 1.0;
            bound[bc++] = c.ax;
            bound[bc++] = c.ay;
            bound[bc++] = 5;
        }
        // End build bound ----------------------------------------------------

        int cross = crossBound(bound, bc, py1, py2);
        if (cross != UNKNOWN) {
            return cross;
        }
        return c.cross(res1, rc1, py1, py2);
    }

    /**
     * Returns how many times rectangle stripe cross cubic curve or the are intersect
     */
    public static int intersectCubic(double x1, double y1, double cx1, double cy1, double cx2, double cy2, double x2, double y2, double rx1, double ry1, double rx2, double ry2) {

        // LEFT/RIGHT/UP
        if ((rx2 < x1 && rx2 < cx1 && rx2 < cx2 && rx2 < x2) ||
            (rx1 > x1 && rx1 > cx1 && rx1 > cx2 && rx1 > x2) ||
            (ry1 > y1 && ry1 > cy1 && ry1 > cy2 && ry1 > y2))
        {
            return 0;
        }

        // DOWN
        if (ry2 < y1 && ry2 < cy1 && ry2 < cy2 && ry2 < y2 && rx1 != x1 && rx1 != x2) {
            if (x1 < x2) {
                return x1 < rx1 && rx1 < x2 ? 1 : 0;
            }
            return x2 < rx1 && rx1 < x1 ? -1 : 0;
        }

        // INSIDE
        CubicCurve c = new CubicCurve(x1, y1, cx1, cy1, cx2, cy2, x2, y2);
        double px1 = rx1 - x1;
        double py1 = ry1 - y1;
        double px2 = rx2 - x1;
        double py2 = ry2 - y1;

        double res1[] = new double[3];
        double res2[] = new double[3];
        int rc1 = c.solvePoint(res1, px1);
        int rc2 = c.solvePoint(res2, px2);

        // LEFT/RIGHT
        if (rc1 == 0 && rc2 == 0) {
            return 0;
        }

        double minX = px1 - DELTA;
        double maxX = px2 + DELTA;

        // Build bound --------------------------------------------------------
        double bound[] = new double[40];
        int bc = 0;
        // Add roots
        bc = c.addBound(bound, bc, res1, rc1, minX, maxX, false, 0);
        bc = c.addBound(bound, bc, res2, rc2, minX, maxX, false, 1);
        // Add extrimal points
        rc2 = c.solveExtremX(res2);
        bc = c.addBound(bound, bc, res2, rc2, minX, maxX, true, 2);
        rc2 = c.solveExtremY(res2);
        bc = c.addBound(bound, bc, res2, rc2, minX, maxX, true, 4);
        // Add start and end
        if (rx1 < x1 && x1 < rx2) {
            bound[bc++] = 0.0;
            bound[bc++] = 0.0;
            bound[bc++] = 0.0;
            bound[bc++] = 6;
        }
        if (rx1 < x2 && x2 < rx2) {
            bound[bc++] = 1.0;
            bound[bc++] = c.ax;
            bound[bc++] = c.ay;
            bound[bc++] = 7;
        }
        // End build bound ----------------------------------------------------

        int cross = crossBound(bound, bc, py1, py2);
        if (cross != UNKNOWN) {
            return cross;
        }
        return c.cross(res1, rc1, py1, py2);
    }

    /**
     * Returns how many times rectangle stripe cross path or the are intersect
     */
    public static int intersectPath(PathIterator p, double x, double y, double w, double h) {

        int cross = 0;
        int count;
        double mx, my, cx, cy;
        mx = my = cx = cy = 0.0;
        double coords[] = new double[6];

        double rx1 = x;
        double ry1 = y;
        double rx2 = x + w;
        double ry2 = y + h;

        while (!p.isDone()) {
            count = 0;
            switch (p.currentSegment(coords)) {
            case PathIterator.SEG_MOVETO:
                if (cx != mx || cy != my) {
                    count = intersectLine(cx, cy, mx, my, rx1, ry1, rx2, ry2);
                }
                mx = cx = coords[0];
                my = cy = coords[1];
                break;
            case PathIterator.SEG_LINETO:
                count = intersectLine(cx, cy, cx = coords[0], cy = coords[1], rx1, ry1, rx2, ry2);
                break;
            case PathIterator.SEG_QUADTO:
                count = intersectQuad(cx, cy, coords[0], coords[1], cx = coords[2], cy = coords[3], rx1, ry1, rx2, ry2);
                break;
            case PathIterator.SEG_CUBICTO:
                count = intersectCubic(cx, cy, coords[0], coords[1], coords[2], coords[3], cx = coords[4], cy = coords[5], rx1, ry1, rx2, ry2);
                break;
            case PathIterator.SEG_CLOSE:
                if (cy != my || cx != mx) {
                    count = intersectLine(cx, cy, mx, my, rx1, ry1, rx2, ry2);
                }
                cx = mx;
                cy = my;
                break;
            }
            if (count == CROSSING) {
                return CROSSING;
            }
            cross += count;
            p.next();
        }
        if (cy != my) {
            count = intersectLine(cx, cy, mx, my, rx1, ry1, rx2, ry2);
            if (count == CROSSING) {
                return CROSSING;
            }
            cross += count;
        }
        return cross;
    }

    /**
     * Returns how many times rectangle stripe cross shape or the are intersect
     */
    public static int intersectShape(Shape s, double x, double y, double w, double h) {
        if (!s.getBounds().intersects((int)x, (int)y, (int)w, (int)h)) {
            return 0;
        }
        return intersectPath(s.getPathIterator(null), x, y, w, h);
    }

    /**
     * Returns true if cross count correspond inside location for non zero path rule
     */
    public static boolean isInsideNonZero(int cross) {
        return cross != 0;
    }

    /**
     * Returns true if cross count correspond inside location for even-odd path rule
     */
    public static boolean isInsideEvenOdd(int cross) {
        return (cross & 1) != 0;
    }
//}

    }

}