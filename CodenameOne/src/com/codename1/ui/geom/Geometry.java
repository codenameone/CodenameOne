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

import com.codename1.ui.Graphics;
import com.codename1.ui.Stroke;
import com.codename1.ui.Transform;
import com.codename1.ui.geom.GeneralPath.ShapeUtil;
import com.codename1.util.MathUtil;
import java.util.Arrays;
import java.util.List;

/**
 * A utility class to assist with geometry elements like bezier curves
 * @author Steve Hannah
 */
class Geometry {
    
    /**
     * Encapsulates a BezierCurve.  Some functionality supports curves of arbitrary degree,
     * but the most useful stuff only supports quadratic and cubic curves.
     * 
     * The main point of this class is to provide the ability to segment bezier curves
     * into smaller components so that {@link GeneralPath#intersection(com.codename1.ui.geom.Rectangle) } 
     * will work for paths that contain curves.
     */
    static class BezierCurve {
        
        /**
         * The x, and y points used for the bezier curves.  {@literal (x[0], y[0])} is the starting point
         * , {@literal (x[x.length-1], y[y.length-1])} is the end point, and all indices in between 
         * are the control points.  Cubic curves will have 4 points, quadratic curves, 3 points, lines
         * 2 points.
         */
        final double[] x, y;
        
        private Point2D startPoint, endPoint;
        private Rectangle2D boundingRect;
        
        
        /**
         * Creates a bezier curve with the provided points.  Points should be entered as {@literal (x1, y1, x2, y2, ..., xn, yn}.
         * @param pts The points.
         */
        public BezierCurve(double... pts) {
            int len = pts.length;
            if (len % 2 != 0) {
                throw new IllegalArgumentException("Length of points array must be even.");
            }
            x = new double[len/2];
            y = new double[len/2];
            
            for (int i=0; i<len; i+=2) {
                x[i/2] = pts[i];
                y[i/2] = pts[i+1];
            }
                    
        }

        @Override
        public String toString() {
            return "Curve{x="+Arrays.toString(x)+", y="+Arrays.toString(y)+"}";
        }
        
        /**
         * Gets the end point of the curve.
         * @return 
         */
        public Point2D getEndPoint() {
            if (endPoint == null) {
                endPoint = new Point2D(x[n()], y[n()]);
            }
            return endPoint;
        }
        
        /**
         * Gets the start point of the curve.
         * @return 
         */
        public Point2D getStartPoint() {
            if (startPoint == null) {
                startPoint = new Point2D(x[0], y[0]);
            }
            return startPoint;
        }
        
        
        /**
         * Creates a new bezier curve as a copy of another.
         * @param toCopy The curve to copy.
         */
        public BezierCurve(BezierCurve toCopy) {
            x = new double[toCopy.x.length];
            y = new double[toCopy.y.length];
            System.arraycopy(toCopy.x, 0, x, 0, toCopy.x.length);
            System.arraycopy(toCopy.y, 0, y, 0, toCopy.y.length);
        }
        
        public int n() {
            return x.length-1;
        }
        
        /**
         * ith polynomial coefficient for x(t) (i.e. the x component of B(t).  See https://en.wikipedia.org/wiki/B%C3%A9zier_curve#Polynomial_form
         * @param j The power of t for which this is a coefficient in sum(t^j * C[j], j=0..n
         * @return The coefficient
         */
        public double cx(int j) {
            return factorial(n()) / factorial(n()-j) * sumFactorX(j, j);
        }
        
        /**
         * Calculates the "summation" factor in {@link #cx(int) }.  See https://en.wikipedia.org/wiki/B%C3%A9zier_curve#Polynomial_form
         * sum(-1^(i+j)*x[i]/(i!*(j-i)!), i=0..j
         * @param j The coefficient that this factor belongs to.
         * @param i 
         * @return 
         */
        private double sumFactorX(int j, int i) {
            if (i==0) {
                return MathUtil.pow(-1, j) * x[0] / factorial(j);
            }
            return MathUtil.pow(-1, i+j) * x[i] / factorial(i)/factorial(j-i) + sumFactorX(j, i-1);
            
        }
        
        /**
         * The y component of the curve function.  See {@link #cx(int) }.
         * @param j
         * @return 
         */
        public double cy(int j) {
            return factorial(n()) / factorial(n()-j) * sumFactorY(j, j);
        }
        
        
        /**
         * The y counterpart of {@link #sumFactorY(int, int) }
         * @param j
         * @param i
         * @return 
         */
        private double sumFactorY(int j, int i) {
            if (i==0) {
                return MathUtil.pow(-1, j) * y[0] / factorial(j);
            }
            return MathUtil.pow(-1, i+j) * y[i] / factorial(i)/factorial(j-i) + sumFactorY(j, i-1);
            
        }
        
        /**
         * Gets x coord for t value.
         * @param t The t value in the curve.  t must be in [0 .. 1]
         * @return The x value corresponding to the given t.
         */
        public double x(double t) {
            return termX(t, n());
        }
        
        /**
         * Calculates up to the jth term of the x func.   See https://en.wikipedia.org/wiki/B%C3%A9zier_curve#Polynomial_form
         * 
         */
        private double termX(double t, int j) {
            if (j == 0) {
                return cx(j);
            }
            return cx(j) * MathUtil.pow(t, j) + termX(t, j-1);
        }
        
        /**
         * Gets derivative's coefficients in increasing order of degree in t.
         * @return 
         */
        public double[] getDerivativeCoefficientsX() {
            switch (n()) {
                case 1:
                    return new double[]{cx(1), 0, 0};
                case 2:
                    return new double[]{cx(1), 2 * cx(2), 0};
                case 3:
                    return new double[]{cx(1), 2 * cx(2), 3 * cx(3)};
                default:
                    throw new IllegalArgumentException("Derivative coefficients only implements for beziers of order 3 or lower.");
            }
        }
        /**
         * Gets derivative's coefficients in increasing order of degree in t.
         * @return 
         */
        public double[] getDerivativeCoefficientsY() {
            switch (n()) {
                case 1:
                    return new double[]{cy(1), 0, 0};
                case 2:
                    return new double[]{cy(1), 2 * cy(2), 0};
                case 3:
                    return new double[]{cy(1), 2 * cy(2), 3 * cy(3)};
                default:
                    throw new IllegalArgumentException("Derivative coefficients only implements for beziers of order 3 or lower.");
            }
        }
        
        
        
        /**
         * Gets y coord for t value
         * @param t The t value on the bezier function to retrieve y at.  T must be in [0 .. 1].
         * @return 
         */
        public double y(double t) {
            return termY(t, n());
        }
        
        /**
         * Calculates up to the jth term of the x func.   See https://en.wikipedia.org/wiki/B%C3%A9zier_curve#Polynomial_form
         * 
         */
        private double termY(double t, int j) {
            if (j == 0) {
                return cy(j);
            }
            return cy(j) * MathUtil.pow(t, j) + termY(t, j-1);
        }
        
        /**
         * Finds all t values that intersect the give x vertical line.
         * @param x 
         * @param res Output array.  For quadratics, this should be length at least 2.  For cubics, length must be at least 3.
         * @return The number of x values found.  
         */
        private int findTValuesForX(double x, double[] res) {
            
            switch (n()) {
                case 2:
                    return ShapeUtil.solveQuad(new double[]{cx(0)-x, cx(1), cx(2), 0}, res);
                    
                case 3:
                    return ShapeUtil.solveCubic(new double[]{cx(0) - x, cx(1), cx(2), cx(3)}, res);
                default:
                    
                    throw new IllegalArgumentException("Only 2 and 3 degree bezier curves are supported");
                    
            }
        }
        /**
         * Finds all t values that intersect the give y horizontal line.
         * @param x 
         * @param res Output array.  For quadratics, this should be length at least 2.  For cubics, length must be at least 3.
         * @return The number of x values found.  
         */
        private int findTValuesForY(double y, double[] res) {
            
            switch (n()) {
                case 2:
                    return ShapeUtil.solveQuad(new double[]{cy(0) - y, cy(1), cy(2) }, res);
                    
                case 3:
                    return ShapeUtil.solveCubic(new double[]{cy(0) - y, cy(1), cy(2), cy(3)}, res);
                default:
                    
                    throw new IllegalArgumentException("Only 2 and 3 degree bezier curves are supported");
                    
            }
        }
        
        /**
         * Returns bezier curve running in the opposite direction.
         * @return 
         */
        public BezierCurve reverse() {
            int len = x.length;
            double[] params = new double[len*2];
            int index = 0;
            for (int i=len-1; i>=0; i--) {
                params[index++] = x[i];
                params[index++] = y[i];
                
            }
            return new BezierCurve(params);
            
        }
        
        /**
         * Segements this curve into two shorter curves.  Split at point t.
         * @param t t value where split should occur.  0 < t < 1
         * @param out List where the two segemeted curves will be added.
         */
        public void segment(double t, List<BezierCurve> out) {
            out.add(segment(t));
            out.add(reverse().segment(1-t).reverse());
        }
        
        /**
         * Adds bezier curve to a path.
         * @param p The path to add to.
         * @param join If false, it will first add a moveTo() command to the path.
         */
        public void addToPath(GeneralPath p, boolean join) {
            if (n() == 2) {
                if (!join) p.moveTo(x[0], y[0]);
                p.quadTo(x[1], y[1], x[2], y[2]);
            } else if (n() == 3) {
                if (!join) p.moveTo(x[0], y[0]);
                p.curveTo(x[1], y[1], x[2], y[2], x[3], y[3]);
            } else if (n() == 1) {
                if (join) p.moveTo(x[0], y[0]);
                p.lineTo(x[1], y[1]);
            }
        }
        
        /**
         * Strokes the bezier curve on a graphics context.
         * @param g
         * @param stroke
         * @param translateX
         * @param translateY 
         */
        public void stroke(Graphics g, Stroke stroke, int translateX, int translateY) {
            if (stroke == null) {
                stroke = new Stroke(1, Stroke.CAP_BUTT, Stroke.JOIN_MITER, 1f);
            }
            GeneralPath p = new GeneralPath();
            addToPath(p, false);
            p.transform(Transform.makeTranslation(translateX, translateY));
            g.drawShape(p, stroke);
        }
        
        
        
        public static void extractBezierCurvesFromPath(Shape shape, List<BezierCurve> out) {
            PathIterator it = shape.getPathIterator();
            int type;
            double[] buf = new double[6];
            double prevX = 0;
            double prevY = 0;
            double markX = 0;
            double markY = 0;
            while (!it.isDone()) {
                type = it.currentSegment(buf);
                switch (type) {
                    case PathIterator.SEG_MOVETO:
                        prevX = buf[0];
                        prevY = buf[1];
                        markX = prevX;
                        markY = prevY;
                        break;
                    case PathIterator.SEG_LINETO:
                        prevX = buf[0];
                        prevY = buf[1];
                        break;
                    case PathIterator.SEG_CLOSE:
                        prevX = markX;
                        prevY = markY;
                        break;
                    case PathIterator.SEG_QUADTO:
                        out.add(new BezierCurve(prevX, prevY, buf[0], buf[1], buf[2], buf[3]));
                        prevX = buf[2];
                        prevY = buf[3];
                        break;
                    case PathIterator.SEG_CUBICTO:
                        out.add(new BezierCurve(prevX, prevY, buf[0], buf[1], buf[2], buf[3], buf[4], buf[5]));
                        prevX = buf[4];
                        prevY = buf[5];
                        break;
                        
                     
                }
                it.next();
                
            }
        }
        
        public Rectangle2D getBoundingRect() {
            if (boundingRect == null) {
                Point2D start = getStartPoint();
                Point2D end = getEndPoint();
                int numSolutions = 0;
                double[] res = new double[3];
                double x1 = Math.min(start.getX(), end.getX());
                double y1 = Math.min(start.getY(), end.getY());
                double x2 = Math.max(start.getX(), end.getX());
                double y2 = Math.max(start.getY(), end.getY());
                switch (n()) {
                    case 1:
                        break;
                    case 0:
                        break;
                    case 2:
                    case 3:

                        numSolutions = ShapeUtil.solveQuad(getDerivativeCoefficientsX(), res);
                        if (numSolutions > 0) {
                            for (int i=0; i<numSolutions; i++) {
                                double t = res[i];
                                if (t < 0 || t > 1) {
                                    continue;
                                }
                                double xt = x(t);
                                double yt = y(t);
                                x1 = Math.min(x1, xt);
                                y1 = Math.min(y1, yt);
                                x2 = Math.max(x2, xt);
                                y2 = Math.max(y2, yt);
                            }

                        }
                        numSolutions = ShapeUtil.solveQuad(getDerivativeCoefficientsY(), res);
                        if (numSolutions > 0) {
                            for (int i=0; i<numSolutions; i++) {
                                double t = res[i];
                                if (t < 0 || t > 1) {
                                    continue;
                                }
                                double xt = x(t);
                                double yt = y(t);
                                x1 = Math.min(x1, xt);
                                y1 = Math.min(y1, yt);
                                x2 = Math.max(x2, xt);
                                y2 = Math.max(y2, yt);
                            }

                        }
                    break;
                    default:
                        throw new IllegalArgumentException("getBoundingRect() only supported for bezier curves of order 3 or less");




                }
                boundingRect = new Rectangle2D(x1, y1, x2-x1, y2-y1);
            }
            return boundingRect;
        }
        
        /**
         * Segments the curve into 2 smaller component curves split at the given t value.  t in [0 .. 1].
         * Returns only the first segment.  You can use reverse().segment(1-t).reverse() to get the other segment.
         * @param t The value of t to segment on.
         * @return The first segment.
         */
        public BezierCurve segment(double t) {
            return segment(0, t);
        }
        
        /**
         * Finds all of the t values that cross the given x vertical between y=minY and y=maxY.  The
         * t values are added to the {@literal out} array, and the number of matches is returned.
         * On quadratic curves, there should be a maximum of 2 results.  On cubic curves, there will be 
         * a maximum 3 results.
         * @param x The x value for which we wish to find t.
         * @param minY Minimum y value we are interested in.
         * @param maxY Maximum y value we are interested in.
         * @param out Out array.  For quadratics, this needs to have length at least 2.  For cubics, at least 3.
         * @return The number of results found.
         */
        public int findTValuesForX(double x, double minY, double maxY, double[] out) {
            
            int numMatches = findTValuesForX(x, out);
            int numFiltered = 0;
            for (int i=0; i<numMatches;i++) {
                if (out[i] < 0 || out[i] > 1) {
                    continue;
                }
                double ty = y(out[i]);
                if (ty >= minY && ty <= maxY) {
                    out[numFiltered] = out[i];
                    numFiltered++;
                }
            }
            return numFiltered;
        }
        
        /**
         * Finds all of the t values that cross the given y horizontal between x=minX and x=maxX.  The
         * t values are added to the {@literal out} array, and the number of matches is returned.
         * On quadratic curves, there should be a maximum of 2 results.  On cubic curves, there will be 
         * a maximum 3 results.
         * @param y The x value for which we wish to find t.
         * @param minX Minimum x value we are interested in.
         * @param maxX Maximum x value we are interested in.
         * @param out Out array.  For quadratics, this needs to have length at least 2.  For cubics, at least 3.
         * @return The number of results found.
         */
        public int findTValuesForY(double y, double minX, double maxX, double[] out) {
            
            int numMatches = findTValuesForY(y, out);
            int numFiltered = 0;
            for (int i=0; i<numMatches;i++) {
                if (out[i] < 0 || out[i] > 1) {
                    continue;
                }
                double tx = x(out[i]);
                if (tx >= minX && tx <= maxX) {
                    out[numFiltered] = out[i];
                    numFiltered++;
                }
            }
            return numFiltered;
        }
        
        /**
         * Compares two bezier curves to see if they are equal (within epsilon margin of error).
         * @param c The bezier curve to compare to
         * @param epsilon Curves are equal if all x and y values are within epsilon of the corresponding x/y value
         * in the other curve.  epsilon must be greater than 0
         * @return True if curves are equal within epsilon margin of error.
         */
        public boolean equals(BezierCurve c, double epsilon) {
            if (c.n() != n()) return false;
            int len = x.length;
            for (int i=0; i<len; i++) {
                if (Math.abs(x[i]-c.x[i]) > epsilon) {
                    return false;
                }
                if (Math.abs(y[i]-c.y[i]) > epsilon) {
                    return false;
                }
            }
            return true;
        }
        
        /**
         * Segments the bezier curve on all intersection points of the provided rectangle.
         * @param rect The rectangle on which to segment the curve.
         * @param out list where the segmented curves are appended.
         */
        public void segment(Rectangle2D rect, List<BezierCurve> out) {
            //System.out.println("segment("+rect+") on  "+this);
            int numIntersections = (n() == 2) ? ShapeUtil.intersectQuad(x[0], y[0], x[1], y[1], x[2], y[2], rect.getX(), rect.getY(), rect.getX()+rect.getWidth(), rect.getY() + rect.getHeight()) :
                    n() == 3 ? ShapeUtil.intersectCubic(x[0], y[0], x[1], y[1], x[2], y[2], x[3], y[3], rect.getX(), rect.getY(), rect.getX() + rect.getWidth(), rect.getHeight() + rect.getY()) :
                    n() == 1 ? ShapeUtil.intersectLine(x[0], y[0], x[1], y[1], rect.getX(), rect.getY(), rect.getX()+rect.getWidth(), rect.getY()+rect.getHeight()) :
                    -1;
            if (numIntersections == -1) {
                throw new IllegalArgumentException("Cannot segment bezier curve of this order: "+n());
            }
            
            if (numIntersections == 0) {
                out.add(new BezierCurve(this));
                return;
            }
            
            
            // To store t values of intersection points
            double[] tvals = new double[numIntersections];
            int nextTvalIndex = 0;
            double[] res = new double[3];
            int numMatches = 0;
            double epsilon = 0.01;
            // left edge
            
            if ((numMatches = findTValuesForX(rect.getX(), rect.getY(), rect.getY() + rect.getHeight(), res)) > 0) {
                //System.out.println("left: "+numMatches);
                nextTvalIndex += arraycopy(res, 0, tvals, nextTvalIndex, numMatches, epsilon);
            }
            // right edge
            if ((numMatches = findTValuesForX(rect.getX()+rect.getWidth(), rect.getY(), rect.getY() + rect.getHeight(), res)) > 0) {
                //System.out.println("right: "+numMatches);
                nextTvalIndex += arraycopy(res, 0, tvals, nextTvalIndex, numMatches, epsilon);
            }
            // top edge
            if ((numMatches = findTValuesForY(rect.getY(), rect.getX(), rect.getX() + rect.getWidth(), res)) > 0) {
                //System.out.println("Top: "+numMatches);
                nextTvalIndex += arraycopy(res, 0, tvals, nextTvalIndex, numMatches, epsilon);
            }
            // bottom edge
            if ((numMatches = findTValuesForY(rect.getY() + rect.getHeight(), rect.getX(), rect.getX() + rect.getWidth(), res)) > 0) {
                //System.out.println("Bottom: "+numMatches+" "+Arrays.toString(res));
                nextTvalIndex += arraycopy(res, 0, tvals, nextTvalIndex, numMatches, epsilon);
            }
            
            Arrays.sort(tvals, 0, nextTvalIndex);
            //System.out.println("tvals="+Arrays.toString(tvals)+"; numSegments="+(nextTvalIndex+1));
            int numSegments = nextTvalIndex+1;
            switch (numSegments) {
                case 1:
                    out.add(new BezierCurve(this));
                    return;
                case 2:
                    if (tvals[0] > epsilon && tvals[0] < 1-epsilon) {
                        segment(tvals[0], out);
                    } else {
                        out.add(new BezierCurve(this));
                    }
                    return;
                default:
                    int tIndex = 0;
                    while (tvals[tIndex] < epsilon || tvals[tIndex] > 1-epsilon) {
                        tIndex++;
                        if (tIndex >= nextTvalIndex) {
                            out.add(new BezierCurve(this));
                            return;
                        }
                    }
                    
                    segment(tvals[tIndex], out);
                    BezierCurve last = out.remove(out.size()-1);
                    if (last.equals(this, epsilon)) {
                        out.add(last);
                    } else {
                        last.segment(rect, out);
                    }
                    return;
            }
            
           
        }
        
        /**
         * Checks an array to see if it already contains a value within the desired epsilon range.
         * @param needle The value to search for
         * @param haystack The array to check
         * @param epsilon The range considered to be a match.
         * @param startIndex Start index to check
         * @param endIndex End index to check
         * @return 
         */
        private static boolean contains(double needle, double[] haystack, double epsilon, int startIndex, int endIndex) {
            for (int i=startIndex; i<endIndex; i++) {
                if (Math.abs(needle-haystack[i]) < epsilon) {
                    return true;
                }
            }
            return false;
        }
        
        /**
         * Copies range from one array to another.  Only unique values are copied (epsilon arg is used for equality range).
         * Returns the number of items that were copied.
         * @param src Source array
         * @param srcStart Start index in source array to copy from
         * @param dst Destination array
         * @param dstStart Start index in destination array to copy to.
         * @param len Number of elements to copy (max).
         * @param epsilon Range considered for equality.
         * @return The number of elements that were actually copied.
         */
        private static int arraycopy(double[] src, int srcStart, double[] dst, int dstStart, int len, double epsilon) {
            int numCopied = 0;
            for (int i=0; i<len; i++) {
                if (!contains(src[srcStart+i], dst, epsilon, 0, dstStart)) {
                    dst[dstStart++] = src[srcStart+i];
                    numCopied++;
                }
            }
            return numCopied;
        }
        
        /**
         * Creates a new bezier curve on 0 to t1.
         * @param t0 Start t
         * @param t1 End t
         * @return A new bezier curve on 0 to t1.
         */
        private BezierCurve segment(double t0, double t1) {
            if (t1 <= 0 || t1 >= 1) {
                throw new IllegalArgumentException("t must be between 0 and 1 but found "+t1);
            }
            if (n() == 2) {
                double x0 = x(t0);
                double y0 = y(t0);
                double xt = x(t1);
                double yt = y(t1);
                double x3 = (x[1] - x[0]) * t1 + x[0];
                double y3 = (y[1] - y[0]) * t1 + y[0];
                
                BezierCurve b1 = new BezierCurve(x0, y0, 
                        x3, y3,
                        xt, yt
                );
                
                return b1;
                
            } else if (n() == 3) {
                if (t0 != 0) {
                    throw new IllegalArgumentException("Only supports t0=0 right now with cubics");
                }
                double t = t1;
                double x1 = x[0];
                double y1 = y[0];
                double x2 = x[1];
                double y2 = y[1];
                double x3 = x[2];
                double y3 = y[2];
                double x4 = x[3];
                double y4 = y[3];
                double x12 = (x2-x1)*t+x1;
                double y12 = (y2-y1)*t+y1;

                double x23 = (x3-x2)*t+x2;
                double y23 = (y3-y2)*t+y2;

                double x34 = (x4-x3)*t+x3;
                double y34 = (y4-y3)*t+y3;

                double x123 = (x23-x12)*t+x12;
                double y123 = (y23-y12)*t+y12;

                double x234 = (x34-x23)*t+x23;
                double y234 = (y34-y23)*t+y23;

                double x1234 = (x234-x123)*t+x123;
                double y1234 = (y234-y123)*t+y123;
                
                return new BezierCurve(x1, y1, x12, y12, x123, y123, x1234, y1234);
                
            }
            throw new IllegalArgumentException("Cannot segment bezier curves with order "+n());
        }
        
        
        
        
    }
    
    private static int factorial(int n) {
        if (n < 0) {
            throw new IllegalArgumentException("factorial does not support negative numbers");
        }
        if (n == 0) return 1;
        return n * factorial(n-1);
    }
    
    
   
}
