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

import com.codename1.impl.CodenameOneImplementation;
import com.codename1.ui.geom.Matrix;
import com.codename1.ui.geom.PathIterator;
import com.codename1.ui.geom.Rectangle;
import com.codename1.ui.geom.Shape;
import java.util.Arrays;

/**
 * A general geometric path, consisting of any number of subpaths constructed
 * out of straight lines and cubic or quadratic Bezier curves. The inside of the
 * curve is defined for drawing purposes by a winding rule. Either the
 * {@link #WIND_EVEN_ODD} or {@link #WIND_NON_ZERO} winding rule can be chosen.
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
 * <!--(Note: This description and image were copied from <a
 * href="http://developer.classpath.org/doc/java/awt/geom/GeneralPath.html">the
 * GNU classpath</a>
 * docs). License here http://www.gnu.org/licenses/licenses.html#FDL -->
 *
 * @author shannah
 *
 * @see com.codename1.ui.Graphics#drawShape
 * @see com.codename1.ui.Graphics#fillShape
 */
public final class GeneralPath implements Shape {

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

        Matrix transform;

        /**
         * Constructs a new GeneralPath.Iterator for given general path
         *
         * @param path - the source GeneralPath object
         */
        Iterator(GeneralPath path) {
            this.p = path;

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

        public int currentSegment(double[] coords) {
            if (isDone()) {
                // awt.4B=Iterator out of bounds
                throw new IndexOutOfBoundsException("Path done"); //$NON-NLS-1$
            }
            int type = p.types[typeIndex];
            int count = GeneralPath.pointShift[type];
            for (int i = 0; i < count; i++) {
                coords[i] = p.points[pointIndex + i];
            }

            pointIndex += count;
            return type;
        }

        private float[] buf = new float[2];

        public int currentSegment(float[] coords) {
            if (isDone()) {
                // awt.4B=Iterator out of bounds
                throw new IndexOutOfBoundsException("Path done"); //$NON-NLS-1$
            }
            int type = p.types[typeIndex];
            int count = GeneralPath.pointShift[type];
            System.arraycopy(p.points, pointIndex, coords, 0, count);
            if (transform != null) {

                for (int i = 0; i <= count; i += 2) {
                    buf[0] = coords[i];
                    buf[1] = coords[i + 1];
                    transform.transformCoord(buf, buf);
                    coords[i] = buf[0];
                    coords[i + 1] = buf[1];
                }
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
     * Constructs a GeneralPath from an arbitrary shape object. The Shapes
     * PathIterator path and winding rule will be used.
     *
     * @param shape
     */
    public GeneralPath(Shape shape) {
        this(WIND_NON_ZERO, BUFFER_SIZE);
        PathIterator p = shape.getPathIterator();
        setWindingRule(p.getWindingRule());
        append(p, false);
    }
    
    public String toString(){
        StringBuilder sb = new StringBuilder();
        sb.append("[General Path: ");
        PathIterator it = getPathIterator();
        float[] buf = new float[6];
        while (!it.isDone() ){
            int type = it.currentSegment(buf);
            switch ( type ){
                case PathIterator.SEG_MOVETO:
                    sb.append("Move ("+buf[0]+","+buf[1]+"), ");
                    break;
                case PathIterator.SEG_LINETO:
                    sb.append("Line ("+buf[0]+","+buf[1]+"), ");
                    break;
                case PathIterator.SEG_CLOSE:
                    sb.append(" CLOSE]");
                    break;
            }
            it.next();
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
        PathIterator p = shape.getPathIterator();
        append(p, connect);
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
        while (!path.isDone()) {
            float coords[] = new float[6];
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
        return new float[]{points[j], points[j + 1]};
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
        return new float[]{rx1, ry1, rx2 - rx1, ry2 - ry1};
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
    
    public boolean isRectangle(){
        Rectangle bounds = getBounds();
        PathIterator it = getPathIterator();
        float[] buf = new float[6];
        boolean[] corners = new boolean[4];
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
    }

    /**
     * Creates a PathIterator for iterating along the segments of the path.
     *
     * @return
     */
    public PathIterator getPathIterator() {
        return new Iterator(this);
    }

    public PathIterator getPathIterator(Matrix m) {
        Iterator out = (Iterator) getPathIterator();
        out.transform = m;
        return out;
    }
    
    public Shape createTransformedShape(Matrix m){
        
        GeneralPath out = new GeneralPath();
        out.append(getPathIterator(m), false);
        return out;
    }

    public void intersect(Shape shape) {
        //Log.p("Start intersect");
        if ( !(shape instanceof Rectangle) ){
            throw new RuntimeException("GeneralPath.intersect() only supports Rectangles");
        }
        Rectangle r = (Rectangle)shape;
        GeneralPath tmp = (GeneralPath)ShapeUtil.intersection(r, this);
        this.reset();
        this.append(tmp, false);
        //Log.p("End intersect");
    }
    
    public Shape intersection(Rectangle rect){
        return ShapeUtil.intersection(rect, this);
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

    public boolean contains(float x, float y) {
       return isInside(ShapeUtil.crossShape(this, x, y));
    }
    
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
        Shape segmentedShape = segmentShape(r, s);
        PathIterator it = segmentedShape.getPathIterator(null);
        GeneralPath out = new GeneralPath();
        float[] buf = new float[6];
        boolean started = false;
        int count = 0;

        float x1 = r.getX();
        float x2 = r.getX() + r.getWidth();
        float y1 = r.getY();
        float y2 = r.getY() + r.getHeight();

        //System.out.println("x1` is "+x1);
        
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
                    count++;
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
        PathIterator it = s.getPathIterator(null);
        GeneralPath out = new GeneralPath();
        float[] buf = new float[6];     // buffer to hold segment coordinates from PathIterator.currentSegment
        float[] curr = new float[2];    // Placeholder for current point
        float[] prev = new float[2];    // Placeholder for previous point
        float[] mark = new float[2];    // Placeholder for the moveTo point
        float[] buf4 = new float[4];    // Reusable buffer to hold two points.

        float prevX = -1;               // Placeholder for previous X coord.
        float prevY = -1;               // Placeholder for previous Y coord.
        float currX = 0;                // Placeholder for current X coord.
        float currY = 0;                // Placeholder for current Y coord.
        float[] intersects = null;      // Placeholder for intersection points
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
                    intersects = intersectLineWithRectAsHash(prevX, prevY, currX, currY, r);
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
                    intersects = intersectLineWithRectAsHash(prevX, prevY, currX, currY, r);
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
    }

    
    static float[] intersectLineWithRectAsHash(float x1, float y1, float x2, float y2, Rectangle rect){
        float[] out = new float[9]; // max 4 points here
        float[] x = new float[4];
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
        
    }
    
    
        
        //public class Crossing {

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
            double t = Math.acos(R / Math.sqrt(Q3)) / 3.0;
            double p = 2.0 * Math.PI / 3.0;
            double m = -2.0 * Math.sqrt(Q);
            res[rc++] = m * Math.cos(t) + n;
            res[rc++] = m * Math.cos(t + p) + n;
            res[rc++] = m * Math.cos(t - p) + n;
        } else {
//          Debug.println("R2 >= Q3 (" + R2 + "/" + Q3 + ")");
            double A = Math.pow(Math.abs(R) + Math.sqrt(R2 - Q3), 1.0 / 3.0);
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
