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
package com.codename1.impl.android;

import android.app.Activity;
import android.graphics.Bitmap;
import android.graphics.BitmapShader;
import android.graphics.Canvas;
import android.graphics.LinearGradient;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RadialGradient;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.Shader;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputConnection;

import com.codename1.io.Log;
import com.codename1.ui.Component;
import com.codename1.ui.Display;
import com.codename1.ui.Font;
import com.codename1.ui.Image;
import com.codename1.ui.Label;
import com.codename1.ui.PeerComponent;
import com.codename1.ui.Stroke;
import com.codename1.ui.TextField;
import com.codename1.ui.geom.GeneralPath;
import com.codename1.ui.geom.Rectangle;
import java.util.ArrayList;

import com.codename1.ui.Transform;
import com.codename1.ui.plaf.Style;
import com.codename1.ui.plaf.StyleAccessor;
import java.util.WeakHashMap;

public class AndroidAsyncView extends ViewGroup implements CodenameOneSurface {


    private RectF tmpRectf = new RectF();

    public static class LayoutParams extends ViewGroup.MarginLayoutParams {
        public PeerComponent pc;
        public LayoutParams(int x, int y, int w, int h, PeerComponent pc) {
            super(w, h);
            this.pc = pc;
            this.x = x;
            this.y = y;
            this.w = w;
            this.h = h;
        }

        public int x, y, w, h;
        public boolean dirty;
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        final int count = getChildCount();
        for(int iter = 0 ; iter < count ; iter++) {
            View child = getChildAt(iter);
            LayoutParams lp = (LayoutParams) child.getLayoutParams();
            child.measure(View.MeasureSpec.makeMeasureSpec(lp.w, MeasureSpec.EXACTLY),
                    View.MeasureSpec.makeMeasureSpec(lp.h, MeasureSpec.EXACTLY));
        }
        setMeasuredDimension(widthMeasureSpec, heightMeasureSpec);
    }

    @Override
    protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
        final int count = getChildCount();
        for(int iter = 0 ; iter < count ; iter++) {
            View child = getChildAt(iter);
            LayoutParams lp = (LayoutParams) child.getLayoutParams();
            child.layout(lp.x, lp.y, lp.x + lp.w, lp.y + lp.h);
        }
    }


    @Override
    public boolean shouldDelayChildPressedState() {
        return false;
    }


    abstract class AsyncOp {

        int clipX;
        int clipY;
        int clipW;
        int clipH;
        
        Path clipPath;
        boolean clipIsPath;

        public AsyncOp(Rectangle clip, Path clipP, boolean clipIsPath) {
            this.clipIsPath = clipIsPath;
            if (clipIsPath) {
                this.clipPath = new Path();
                this.clipPath.set(clipP);
            } else {
                if (clip == null) {
                    clipW = cn1View.width;
                    clipH = cn1View.height;
                } else {
                    clipX = clip.getX();
                    clipY = clip.getY();
                    clipW = clip.getWidth();
                    clipH = clip.getHeight();
                }
            }

        }
        
        public void prepare() {}
        
        public void executeWithClip(AndroidGraphics underlying) {
            if(clipIsPath){
                underlying.setClipRaw(clipPath);
            }else{
                underlying.setClipRaw(clipX, clipY, clipW, clipH);
            }
            execute(underlying);
        }

        public abstract void execute(AndroidGraphics underlying);
    }
    private static final Object RENDERING_OPERATIONS_LOCK = new Object();
    private ArrayList<AsyncOp> renderingOperations = new ArrayList<AsyncOp>();
    private ArrayList<AsyncOp> pendingRenderingOperations = new ArrayList<AsyncOp>();
    private ArrayList<AsyncOp> currentlyRendering = new ArrayList<AsyncOp>();
    private final CodenameOneView cn1View;
    private final AndroidGraphics graphics;
    private final AndroidGraphics internalGraphics;
    private final AndroidImplementation implementation;
    static boolean legacyPaintLogic = true;
    private static ArrayList<Path> pathPool = new ArrayList<Path>();

    private synchronized static Path createPathFromPool() {
        if (!pathPool.isEmpty()) {
            Path out = pathPool.remove(pathPool.size()-1);
            out.rewind();
            return out;
        }
        return new Path();
    }

    private synchronized static void recycle(Path p) {
        if (pathPool.size() < 5 && p != null) {
            pathPool.add(p);
        }
    }


    public AndroidAsyncView(Activity activity, AndroidImplementation implementation) {
        super(activity);
        setId(2001);
        this.implementation = implementation;
        graphics = new AsyncGraphics(implementation);
        internalGraphics = new AndroidGraphics(implementation, null, false);
        cn1View = new CodenameOneView(activity, this, implementation, false);
        setWillNotCacheDrawing(true);
        setWillNotDraw(true);
        setBackgroundDrawable(null);
        this.setFocusable(true);
        this.setFocusableInTouchMode(false);
        this.setDescendantFocusability(ViewGroup.FOCUS_AFTER_DESCENDANTS);
    }


    @Override
    protected void dispatchDraw(Canvas c) {
        // **************************
        // Commented out debuging code useful for hand tuning the drawing logic performance
        // **************************
        //long time = System.currentTimeMillis();
        
        //boolean paintOnBuffer = implementation.isEditingText() ||
        //        InPlaceEditView.isKeyboardShowing();
        
        boolean paintOnBuffer = false;
        
        internalGraphics.setCanvasNoSave(c);
        AndroidGraphics g = internalGraphics;
        if (paintOnBuffer) {
            g = cn1View.getGraphics();
        }

        //final HashMap<String, Long> slowest = new HashMap<>();
        //final HashMap<String, Long> counts = new HashMap<>();
        int count = renderingOperations.size();

        // this works around the case of a blank screen when an invalidate occurs out of nowhere
        // and no operations are in the queue
        if(count > 0) {
            currentlyRendering.clear();
            currentlyRendering.addAll(renderingOperations);
        } else {
            count = currentlyRendering.size();
        }
        int offset = 0;
        for(; offset < count ; offset++) {
            AsyncOp o = currentlyRendering.get(offset);
            o.executeWithClip(g);
            /*ntime = System.nanoTime() - ntime;
            String s = o.toString();
            Long l = slowest.get(s);
            if(l == null) {
                slowest.put(s, ntime);
                counts.put(s, Long.valueOf(1));
            } else {
                slowest.put(s, l.longValue() + ntime);
                Long ll = counts.get(s);
                counts.put(s, ll.longValue() + 1);
            }*/
        }
        
        synchronized (RENDERING_OPERATIONS_LOCK) {
            renderingOperations.clear();
            RENDERING_OPERATIONS_LOCK.notify();
        }

        if (paintOnBuffer) {
            cn1View.d(c);
        }
        if (implementation.isAsyncEditMode() && implementation.isEditingText()) {
            InPlaceEditView.reLayoutEdit();
        }
        /*long etime = System.currentTimeMillis();
        if(etime - time >= 16) {
            System.out.println("JANK... " + (etime - time));

            ArrayList<String> sortedSlowest = new ArrayList<>(slowest.keySet());
            Collections.sort(sortedSlowest, new Comparator<String>() {
                @Override
                public int compare(String lhs, String rhs) {
                    return (int) (slowest.get(rhs).longValue() - slowest.get(lhs).longValue());
                }
            });


            ArrayList<String> sortedCounts = new ArrayList<>(counts.keySet());
            Collections.sort(sortedCounts, new Comparator<String>() {
                @Override
                public int compare(String lhs, String rhs) {
                    return (int) (counts.get(rhs).longValue() - counts.get(lhs).longValue());
                }
            });
            for(int iter = 0 ; iter < Math.min(5, sortedSlowest.size()) ; iter++) {
                String name = sortedSlowest.get(iter);
                System.out.println("Slowest entry " + iter + " is " + name + " for " + slowest.get(name));

                name = sortedCounts.get(iter);
                System.out.println("Most entries " + iter + " is " + name + " for " + counts.get(name));
            }
        }*/
    }

    public void setPaintViewOnBuffer(boolean paintViewOnBuffer) {
        //this.paintViewOnBuffer = paintViewOnBuffer;
    }

    @Override
    public boolean isOpaque() {
        return true;
    }

    private void visibilityChangedTo(boolean visible) {
        cn1View.visibilityChangedTo(visible);
    }

    @Override
    protected void onWindowVisibilityChanged(int visibility) {
        super.onWindowVisibilityChanged(visibility);
        this.visibilityChangedTo(visibility == View.VISIBLE);
        /*if(visibility != View.VISIBLE){
            paintViewOnBuffer = true;
        }*/
    }

    @Override
    protected void onSizeChanged(final int w, final int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);

        if (!Display.isInitialized()) {
            return;
        }
        //if the input is pan mode, there is no need to resize the screen
        // It turns out that this was only necessary because the InPlaceEditView was causing
        // an onSizeChanged event to be fired when its visibility was set to GONE.
        // Removing that visibility setting removes the erroneous resize event.
        // Keeping this was problematic because sometimes a resize event is correct even when
        // the keyboard is showing, e.g. when the device is rotated.
        //if((InPlaceEditView.isEditing() || InPlaceEditView.isKeyboardShowing()) && !InPlaceEditView.isInputResize()){
        //    return;
        //}
        Display.getInstance().callSerially(new Runnable() {
            public void run() {
                cn1View.handleSizeChange(w, h);
            }
        });
    }

    public void resizeViews() {
        int children = getChildCount();
        if(children > 0) {
            for (int iter = 0; iter < children; iter++) {
                final View v = getChildAt(iter);
                final AndroidAsyncView.LayoutParams lp = (AndroidAsyncView.LayoutParams) v.getLayoutParams();
                if (lp.dirty) {
                    lp.dirty = false;
                    v.post(new Runnable() {
                        @Override
                        public void run() {
                            v.requestLayout();

                            Display.getInstance().getInstance().callSerially(new Runnable() {
                                @Override
                                public void run() {
                                    Display.getInstance().getCurrent().repaint();
                                }
                            });
                        }
                    });
                }
            }
        }
    }

    @Override
    public void flushGraphics(Rect rect) {
        //Log.d(Display.getInstance().getProperty("AppName", "CodenameOne"), "Flush graphics invoked with pending: " + pendingRenderingOperations.size() + " and current " + renderingOperations.size());

        // we might have pending entries in the rendering queue
        int counter = 0;
        while (!renderingOperations.isEmpty()) {
            try {
                synchronized (RENDERING_OPERATIONS_LOCK) {
                    RENDERING_OPERATIONS_LOCK.wait(5);
                }

                // don't let the EDT die here
                counter++;
                if (counter > 10) {
                    //Log.d(Display.getInstance().getProperty("AppName", "CodenameOne"), "Flush graphics timed out!!!");
                    return;
                }
            } catch (InterruptedException err) {
            }
        }
        ArrayList<AsyncOp> tmp = renderingOperations;
        renderingOperations = pendingRenderingOperations;
        pendingRenderingOperations = tmp;
        
        for (AsyncOp o : renderingOperations) {
            o.prepare();
        }

        int children = getChildCount();
        if(children > 0) {
            com.codename1.ui.Form c = Display.getInstance().getCurrent();
            for (int iter = 0; iter < children; iter++) {
                final View v = getChildAt(iter);
                final AndroidAsyncView.LayoutParams lp = (AndroidAsyncView.LayoutParams) v.getLayoutParams();
                //if (lp != null && c == lp.pc.getComponentForm()) {
                    //v.postInvalidate();

                    /*if(lp.dirty) {
                        lp.dirty = false;
                        v.post(new Runnable() {
                            @Override
                            public void run() {
                                if (v.getVisibility() == View.INVISIBLE) {
                                    v.setVisibility(View.VISIBLE);
                                }
                                v.requestLayout();
                            }
                        });
                    } else {
                        if (v.getVisibility() == View.INVISIBLE) {
                            v.post(new Runnable() {
                                @Override
                                public void run() {
                                    v.setVisibility(View.VISIBLE);
                                }
                            });
                        }
                    }*/
                //} else {
                    /*if(v.getVisibility() == View.VISIBLE) {
                        v.post(new Runnable() {
                            @Override
                            public void run() {
                                v.setVisibility(View.INVISIBLE);
                            }
                        });
                    }*/
                //}
            }
        }
        if (rect == null) {
            postInvalidate();
        } else {
            postInvalidate(rect.left, rect.top, rect.right, rect.bottom);
        }
        graphics.setClip(0, 0, cn1View.width, cn1View.height);
        graphics.setAlpha(255);
        graphics.setColor(0);
    }

    @Override
    public void flushGraphics() {
        flushGraphics(null);
    }

    @Override
    public boolean onKeyMultiple(int keyCode, int repeatCount, KeyEvent event) {
        return true;
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (InPlaceEditView.isEditing()) {
            return true;
        }
        return cn1View.onKeyUpDown(true, keyCode, event);
    }

    @Override
    public boolean onKeyUp(int keyCode, KeyEvent event) {
        if (InPlaceEditView.isEditing()) {
            return true;
        }
        return cn1View.onKeyUpDown(false, keyCode, event);
    }

    @Override
    public boolean dispatchTouchEvent(MotionEvent event) {
        super.dispatchTouchEvent(event);
        return cn1View.onTouchEvent(event);
    }

    @Override
    public AndroidGraphics getGraphics() {
        return graphics;
    }

    @Override
    public int getViewHeight() {
        return cn1View.height;
    }

    @Override
    public int getViewWidth() {
        return cn1View.width;
    }

    @Override
    public InputConnection onCreateInputConnection(EditorInfo editorInfo) {
        if (!Display.isInitialized() || Display.getInstance().getCurrent() == null) {
            return super.onCreateInputConnection(editorInfo);
        }
        cn1View.setInputType(editorInfo);
        return super.onCreateInputConnection(editorInfo);
    }

    @Override
    public boolean onCheckIsTextEditor() {
        if (!Display.isInitialized() || Display.getInstance().getCurrent() == null) {
            return false;
        }
        Component txtCmp = Display.getInstance().getCurrent().getFocused();
        if (txtCmp != null && txtCmp instanceof TextField) {
            return true;
        }
        return false;
    }

    @Override
    public View getAndroidView() {
        return this;
    }
    
    public void removePeerView(final View v) {
        if(v.getParent() != null) {
            ((Activity)v.getContext()).runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    // this might happen twice if draw view is invoked twice before the screen has time to draw
                    if(v.getParent() != null) {
                        removeView(v);
                    }
                }
            });
        }
    }
    

    class AsyncGraphics extends AndroidGraphics {

        private boolean clipIsPath;
        private Matrix convertedTransform;
        private boolean clipFresh;
        private boolean transformDirty=true;
        private Rectangle clip = null;
        private Path clipP = null;
        private GeneralPath clipGP = null;
        private int clipX, clipY, clipW, clipH;

        private int alpha;
        private int color;
        private Paint imagePaint = new Paint();
        private Transform transform;
        private Transform inverseTransform;
        private boolean inverseTransformDirty=true;
        private com.codename1.ui.geom.Rectangle tmpClipBoundsRect = new com.codename1.ui.geom.Rectangle();
        private com.codename1.ui.geom.Rectangle tmpRect = new com.codename1.ui.geom.Rectangle();
        private GeneralPath tmpClipBoundsPath = new GeneralPath();

        private float[] tmpMatrix3x3 = new float[9];

        private Transform getInverseTransform() {
            if (inverseTransform == null) {
                inverseTransform = Transform.makeIdentity();
                inverseTransformDirty = true;
            }
            if (inverseTransformDirty) {

                try {
                    getTransform().getInverse(inverseTransform);
                    inverseTransformDirty = false;
                } catch (Exception ex) {
                    ex.printStackTrace();
                }
            }
            return inverseTransform;
        }

        AsyncGraphics(AndroidImplementation impl) {
            super(impl, null, false);
        }


        private void freshClip() {
            if (!clipFresh) {
                clipFresh = true;
                if (clipIsPath) {
                    if (clipGP == null) {
                        clipX = 0;
                        clipY = 0;
                        clipW = cn1View.width;
                        clipH = cn1View.height;
                    } else {
                        tmpClipBoundsPath.setPath(clipGP, getInverseTransform());
                        tmpClipBoundsPath.getBounds(tmpClipBoundsRect);
                        clipX = tmpClipBoundsRect.getX();
                        clipY = tmpClipBoundsRect.getY();
                        clipW = tmpClipBoundsRect.getWidth();
                        clipH = tmpClipBoundsRect.getHeight();
                    }
                } else {
                    if (clip == null) {
                        clip = new Rectangle(0, 0, cn1View.width, cn1View.height);
                        clipX = 0;
                        clipY = 0;
                        clipW = cn1View.width;
                        clipH = cn1View.height;
                    } else {
                        clipX = clip.getX();
                        clipY = clip.getY();
                        clipW = clip.getWidth();
                        clipH = clip.getHeight();
                    }
                    if (!getTransform().isIdentity()) {
                        tmpClipBoundsPath.setRect(clip, getInverseTransform());
                        tmpClipBoundsPath.getBounds(tmpClipBoundsRect);
                        clipX = tmpClipBoundsRect.getX();
                        clipY = tmpClipBoundsRect.getY();
                        clipW = tmpClipBoundsRect.getWidth();
                        clipH = tmpClipBoundsRect.getHeight();
                    }
                }
            }
        }

        @Override
        public void drawView(final View v, final AndroidAsyncView.LayoutParams lp) {
            if(v.getParent() == null) {
                ((Activity)v.getContext()).runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        // this might happen twice if draw view is invoked twice before the screen has time to draw
                        if(v.getParent() == null) {
                            v.setLayoutParams(lp);
                            addView(v);
                        }
                    }
                });
            }
            pendingRenderingOperations.add(new AsyncOp(clip, clipP, clipIsPath) {
                @Override
                public void execute(AndroidGraphics underlying) {
                    drawChild(underlying.canvas, v, getDrawingTime());
                }
                public String toString() {
                    return "drawView(PeerComponent)";
                }
            });
        }

        @Override
        public void rotate(final float angle, final int x, final int y) {
            getTransform().rotate(angle, x, y);
            transformDirty = true;
            inverseTransformDirty = true;
            clipFresh = false;
            pendingRenderingOperations.add(new AsyncOp(clip, clipP, clipIsPath) {
                @Override
                public void execute(AndroidGraphics underlying) {
                    underlying.rotate(angle, x, y);
                }

                @Override
                public void executeWithClip(AndroidGraphics underlying) {
                    execute(underlying);
                }

                public String toString() {
                    return "rotate";
                }
            });
        }

        @Override
        public void rotate(final float angle) {
            getTransform().rotate(angle, 0, 0);
            transformDirty = true;
            inverseTransformDirty = true;
            clipFresh = false;
            pendingRenderingOperations.add(new AsyncOp(clip,clipP, clipIsPath) {
                @Override
                public void execute(AndroidGraphics underlying) {
                    underlying.rotate(angle);
                }

                @Override
                public void executeWithClip(AndroidGraphics underlying) {
                    execute(underlying);
                }

                public String toString() {
                    return "rotate (no pivot)";
                }
            });
        }


        @Override
        public void scale(final float x, final float y) {
            getTransform().scale(x, y);
            transformDirty = true;
            inverseTransformDirty = true;
            clipFresh = false;
            pendingRenderingOperations.add(new AsyncOp(clip, clipP, clipIsPath) {
                @Override
                public void execute(AndroidGraphics underlying) {
                    underlying.scale(x, y);
                }

                @Override
                public void executeWithClip(AndroidGraphics underlying) {
                    execute(underlying);
                }

                public String toString() {
                    return "scale";
                }
            });
        }

        @Override
        public void resetAffine() {
            getTransform().setIdentity();
            transformDirty = true;
            inverseTransformDirty = true;
            clipFresh = false;
            pendingRenderingOperations.add(new AsyncOp(clip, clipP, clipIsPath) {
                @Override
                public void execute(AndroidGraphics underlying) {
                    underlying.resetAffine();
                }

                @Override
                public void executeWithClip(AndroidGraphics underlying) {
                    execute(underlying);
                }

                public String toString() {
                    return "resetAffine";
                }
            });
        }


        /*
        private Matrix getTransformMatrix(){
            if ( transformDirty ){
                // Conversion from 4x4 to 3x3
                // See http://www.w3.org/TR/2009/WD-SVG-Transforms-20090320/#_4x4-to-3x3-conversion
                // for formula
                CN1Matrix4f m = (CN1Matrix4f)getTransform().getNativeTransform();
                float[] mMatrix3x3 = tmpMatrix3x3;
                float[] mMatrix4x4 = m.getData();


                mMatrix3x3[0] = mMatrix4x4[0];
                mMatrix3x3[1] = mMatrix4x4[4];
                mMatrix3x3[2] = mMatrix4x4[12];
                mMatrix3x3[3] = mMatrix4x4[1];
                mMatrix3x3[4] = mMatrix4x4[5];
                mMatrix3x3[5] = mMatrix4x4[13];
                mMatrix3x3[6] = mMatrix4x4[3];
                mMatrix3x3[7] = mMatrix4x4[7];
                mMatrix3x3[8] = mMatrix4x4[15];

                if (convertedTransform == null) {
                    convertedTransform = new Matrix();
                }
                convertedTransform.setValues(mMatrix3x3);

                transformDirty = false;

            }
            return convertedTransform;

        }
        */

        @Override
        public int getColor() {
            return color;
        }


        private void getClipShape(GeneralPath out) {
            if (clipIsPath) {
                out.reset();
                if (getTransform().isIdentity()) {
                    out.setPath(clipGP, null);
                } else {
                    out.setPath(clipGP, getInverseTransform());
                }
            } else {
                out.reset();
                if (clip == null) {
                    clip = new Rectangle(0, 0, cn1View.width, cn1View.height);
                }
                out.setRect(clip, getInverseTransform());
            }
        }

        @Override
        public void clipRect(final int x, final int y, final int width, final int height) {
            boolean isIdentity = getTransform().isIdentity();
            clipFresh = false;
            if (clipIsPath) {
                if (clipGP == null) {
                    setClip(x, y, width, height);
                    return;
                }
                if (!isIdentity) {
                    clipGP.transform(getInverseTransform());
                }
                if (!clipGP.intersect(x, y, width, height)) {
                    if (clip == null) {
                        clip = new Rectangle();
                    }
                    clip.setBounds(x,y,0,0);
                    clipIsPath = false;
                    return;
                }
                if (!isIdentity) {
                    clipGP.transform(getTransform());
                }
                if (clipGP.isRectangle()) {
                    if (clip == null) {
                        clip = new Rectangle();
                    }
                    clipGP.getBounds(clip);
                    clipIsPath = false;
                } else {
                    if (clipP == null) {
                        clipP = new Path();
                    }
                    AndroidImplementation.cn1ShapeToAndroidPath(clipGP, clipP);
                }
            } else {
                if (clip == null) {
                    setClip(x, y, width, height);
                    return;
                }
                if (clip.getWidth() <= 0 || clip.getHeight() <= 0) {
                    return;
                }
                if (isIdentity) {
                    tmpRect.setBounds(x, y, width, height);
                    clip.intersection(tmpRect, clip);
                } else {
                    clipIsPath = true;
                    if (clipGP == null) {
                        clipGP = new GeneralPath();
                    }
                    clipGP.setRect(clip, null);
                    clipGP.transform(getInverseTransform());
                    if (!clipGP.intersect(x, y, width, height)) {
                        clip.setBounds(x, y, 0, 0);
                        clipIsPath = false;
                        return;
                    }
                    clipGP.transform(getTransform());

                    if (clipGP.isRectangle()) {
                        clipGP.getBounds(clip);
                        clipIsPath = false;
                    } else {
                        if (clipP == null) {
                            clipP = new Path();
                        }
                        AndroidImplementation.cn1ShapeToAndroidPath(clipGP, clipP);
                    }
                }
            }
            //not implemented - if the current clipping is a Path we should intersect it with 
            //the shape
        }

        @Override
        public void setClip(final int x, final int y, final int width, final int height) {
            boolean isIdentity = getTransform().isIdentity();
            clipFresh = false;
            if (isIdentity) {
                clipIsPath = false;
                if (clip == null) {
                    clip = new Rectangle(x, y, width, height);
                } else {
                    clip.setBounds(x, y, width, height);
                }
            } else {
                clipIsPath = true;
                if (clipGP == null) {
                    clipGP = new GeneralPath();
                }
                if (clip == null) {
                    clip = new Rectangle();
                }
                clip.setBounds(x, y, width, height);
                clipGP.setRect(clip, getTransform());

                if (clipGP.isRectangle()) {
                    clipIsPath = false;
                    if (clip == null) {
                        clip = new Rectangle();
                    }
                    clipGP.getBounds(clip);
                } else {
                    if (clipP == null) {
                        clipP = new Path();
                    }
                    AndroidImplementation.cn1ShapeToAndroidPath(clipGP, clipP);
                }
            }

        }

        @Override
        public void setClip(com.codename1.ui.geom.Shape shape) {
            clipIsPath = true;
            clipFresh = false;
            if (clipGP == null) {
                clipGP = new GeneralPath();
            }
            boolean isIdentity = getTransform().isIdentity();
            if (isIdentity) {
                clipGP.setShape(shape, null);
            } else {
                clipGP.setShape(shape, getTransform());
            }
            if (clipGP.isRectangle()) {
                clipIsPath = false;
                if (clip == null) {
                    clip = new Rectangle();
                }
                clipGP.getBounds(clip);
            } else {
                if (clipP == null) {
                    clipP = new Path();
                }
                AndroidImplementation.cn1ShapeToAndroidPath(clipGP, clipP);

            }

        }

        @Override
        public int getClipY() {
            freshClip();
            return clipY;
        }

        @Override
        public int getClipX() {
            freshClip();
            return clipX;

        }
        @Override
        public int getClipWidth() {
            freshClip();
            return clipW;
        }

        @Override
        public int getClipHeight() {
            freshClip();
            return clipH;
        }

        @Override
        public void setAlpha(final int a) {
            this.alpha = a;
        }

        @Override
        public int getAlpha() {
            return alpha;
        }

        @Override
        public void fillRoundRect(final int x, final int y, final int width, final int height, final int arcWidth, final int arcHeight) {
            final int alph = alpha;
            final int col = color;
            pendingRenderingOperations.add(new AsyncOp(clip, clipP, clipIsPath) {
                @Override
                public void execute(AndroidGraphics underlying) {
                    underlying.setAlpha(alph);
                    underlying.setColor(col);
                    underlying.fillRoundRect(x, y, width, height, arcWidth, arcHeight);
                }
                public String toString() {
                    return "fillRoundRect";
                }
            });
        }

        @Override
        public void fillRect(final int x, final int y, final int width, final int height) {
            if (alpha == 0) {
                return;
            }
            final int al = alpha;
            final int col = color;
            pendingRenderingOperations.add(new AsyncOp(clip, clipP, clipIsPath) {
                @Override
                public void execute(AndroidGraphics underlying) {
                    underlying.setColor(col);
                    underlying.setAlpha(al);
                    underlying.fillRect(x, y, width, height);
                }
                public String toString() {
                    return "fillRectA";
                }
            });
        }

        @Override
        public void fillRect(final int x, final int y, final int w, final int h, byte alpha) {
            if (alpha == 0) {
                return;
            }
            final int preAlpha = this.alpha;
            final int al = alpha & 0xff;
            final int col = color;
            pendingRenderingOperations.add(new AsyncOp(clip, clipP, clipIsPath) {
                @Override
                public void execute(AndroidGraphics underlying) {
                    underlying.setColor(col);
                    underlying.setAlpha(al);
                    underlying.fillRect(x, y, w, h);
                    underlying.setAlpha(preAlpha);
                }
                public String toString() {
                    return "fillRectB";
                }
            });
        }

        @Override
        public void fillLinearGradient(final int startColor, final int endColor, final int x, final int y, final int width, final int height, final boolean horizontal) {
            if (alpha == 0) {
                return;
            }
            final int al = alpha;
            pendingRenderingOperations.add(new AsyncOp(clip, clipP, clipIsPath) {
                @Override
                public void execute(AndroidGraphics underlying) {
                    underlying.setAlpha(al);
                    underlying.fillLinearGradient(startColor, endColor, x, y, width, height, horizontal);
                }
                public String toString() {
                    return "fillLinearGradient";
                }
            });
        }

        @Override
        public void fillRectRadialGradient(final int startColor, final int endColor, final int x, final int y,
                                           final int width, final int height, final float relativeX, final float relativeY, final float relativeSize) {
            if (alpha == 0) {
                return;
            }
            final int al = alpha;
            pendingRenderingOperations.add(new AsyncOp(clip, clipP, clipIsPath) {
                @Override
                public void execute(AndroidGraphics underlying) {
                    underlying.setAlpha(al);
                    underlying.fillRectRadialGradient(startColor, endColor, x, y, width, height, relativeX, relativeY, relativeSize);
                }
                public String toString() {
                    return "fillRectRadialGradient";
                }
            });
        }

        @Override
        public void fillRadialGradient(final int startColor, final int endColor, final int x, final int y, final int width, final int height) {
            fillRadialGradient(startColor, endColor, x, y, width, height, 0, 360);
            
        }
        
        @Override
        public void fillRadialGradient(final int startColor, final int endColor, final int x, final int y, final int width, final int height, final int startAngle, final int arcAngle) {
            if (alpha == 0) {
                return;
            }
            final int al = alpha;
            pendingRenderingOperations.add(new AsyncOp(clip, clipP, clipIsPath) {
                @Override
                public void execute(AndroidGraphics underlying) {
                    underlying.setAlpha(al);
                    underlying.fillRadialGradient(startColor, endColor, x, y, width, height, startAngle, arcAngle);
                }
                public String toString() {
                    return "fillRadialGradient";
                }
            });
        }
        
        class AndroidStyleCache {
            AsyncPaintPosition backgroundPainter;
            CodenameOneTextPaint textPaint;
            Paint iconPaint;
        }
        
        abstract class AsyncPaintPosition extends AsyncOp{
            int x;
            int y;
            int width;
            int height;
            int alpha;
            
            // the pending values allow changing this op on the Codename One EDT while the Android thread
            // renders it
            int pendingX;
            int pendingY;
            int pendingWidth;
            int pendingHeight;
            int pendingAlpha;

            int pendingClipX;
            int pendingClipY;
            int pendingClipW;
            int pendingClipH;
            
            Path pendingClipP;
            boolean pendingClipIsPath;
            
            public AsyncPaintPosition(Rectangle clip, Path clipP, boolean clipIsPath) {
                super(clip, clipP, clipIsPath);
                pendingClipIsPath = clipIsPath;


                if (clipIsPath) {
                    pendingClipP = new Path();
                    pendingClipP.set(this.clipPath);
                } else {
                    pendingClipX = clipX;
                    pendingClipY = clipY;
                    pendingClipW = clipW;
                    pendingClipH= clipH;
                }
            }

            public void updateClip(Rectangle clip, Path clipP, boolean clipIsPath) {
                pendingClipIsPath = clipIsPath;
                if (clipIsPath) {
                    if (pendingClipP == null) {
                        pendingClipP = new Path();
                    }
                    pendingClipP.set(clipP);
                } else {
                    pendingClipX = clip.getX();
                    pendingClipY = clip.getY();
                    pendingClipW = clip.getWidth();
                    pendingClipH = clip.getHeight();
                }
            }

            @Override
            public void prepare() {
                x = pendingX;
                y = pendingY;
                height = pendingHeight;
                width = pendingWidth;
                alpha = pendingAlpha;
                clipIsPath = pendingClipIsPath;
                if (clipIsPath) {
                    if (clipPath == null) {
                        clipPath = new Path();
                    }
                    clipPath.set(pendingClipP);
                } else {
                    clipX = pendingClipX;
                    clipY = pendingClipY;
                    clipW = pendingClipW;
                    clipH = pendingClipH;
                }
            }

            @Override
            public void execute(AndroidGraphics underlying) {
                executeImpl(underlying);
            }
            
            public abstract void executeImpl(AndroidGraphics underlying);
        }
                
        AsyncPaintPosition createGradientPaint(Style s) {
            final Paint paint = new Paint();
            paint.setStyle(Paint.Style.FILL);
            paint.setAntiAlias(true);
            paint.setAlpha(255);
            final byte bgType = s.getBackgroundType();
            final int startColor = s.getBackgroundGradientStartColor();
            final int endColor = s.getBackgroundGradientEndColor();
            
            AsyncPaintPosition ap = new AsyncPaintPosition(clip, clipP, clipIsPath) {
                int lastHeight;
                int lastWidth;
                @Override
                public void executeImpl(AndroidGraphics underlying) {
                    if(width != lastWidth || height != lastHeight) {
                        lastWidth = width;
                        lastHeight = height;
                        switch(bgType) {
                            case Style.BACKGROUND_GRADIENT_LINEAR_VERTICAL:
                                paint.setShader(new LinearGradient(0, 0, 0, height, startColor, endColor, Shader.TileMode.MIRROR));
                                break;
                            case Style.BACKGROUND_GRADIENT_LINEAR_HORIZONTAL:
                                paint.setShader(new LinearGradient(0, 0, width, 0, startColor, endColor, Shader.TileMode.MIRROR));
                                break;
                            case Style.BACKGROUND_GRADIENT_RADIAL:
                                paint.setShader(new RadialGradient(x, y, Math.max(width, height), startColor, endColor, Shader.TileMode.MIRROR));
                                break;
                        }
                        underlying.canvas.drawRect(x, y, x + width, y + height, paint);
                    }
                }
                public String toString() {
                    return "GradientPaint";
                }
            };
            return ap;
        }
        
        @Override
        public void paintComponentBackground(final int x, final int y, final int width, final int height, final Style s) {
            if (alpha == 0 || width <= 0 || height <= 0) {
                return;
            }
            
            if(legacyPaintLogic || StyleAccessor.isRendererStyle(s)) {
                final int al = alpha;
                final byte backgroundType = s.getBackgroundType();
                final Image bgImage = s.getBgImage();
                final int bgColor = s.getBgColor();
                final byte bgTransparency = s.getBgTransparency();
                final int backgroundGradientStartColor = s.getBackgroundGradientStartColor();
                final int backgroundGradientEndColor = s.getBackgroundGradientEndColor();
                final float backgroundGradientRelativeX = s.getBackgroundGradientRelativeX();
                final float backgroundGradientRelativeY = s.getBackgroundGradientRelativeY();
                final float backgroundGradientRelativeSize = s.getBackgroundGradientRelativeSize();
                pendingRenderingOperations.add(new AsyncOp(clip, clipP, clipIsPath) {
                    @Override
                    public void execute(AndroidGraphics underlying) {
                        underlying.setAlpha(al);
                        underlying.paintComponentBackground(backgroundType, bgImage, bgColor, bgTransparency,
                                backgroundGradientStartColor, backgroundGradientEndColor,
                                backgroundGradientRelativeX, backgroundGradientRelativeY,
                                backgroundGradientRelativeSize, x, y, width, height);
                    }
                    public String toString() {
                        return "paintComponentBackground - Legacy";
                    }
                });
                return;
            }

            AndroidStyleCache sc = (AndroidStyleCache)StyleAccessor.getCachedData(s);
            AsyncPaintPosition bgPaint = null;
            if(sc != null) {
                bgPaint = sc.backgroundPainter;
            }
            if(bgPaint == null) {
                final byte backgroundType = s.getBackgroundType() ;
                Image bgImageOrig = s.getBgImage();
                if (bgImageOrig == null) {
                    if(backgroundType >= Style.BACKGROUND_GRADIENT_LINEAR_VERTICAL) {
                        bgPaint = createGradientPaint(s);
                    } else {
                        // solid color paint
                        byte bgt = s.getBgTransparency();
                        if(bgt == 0) {
                            return;
                        }
                        bgPaint = paintBackgroundSolidColor(bgt, s, bgPaint);
                    }
                } else {
                    switch(backgroundType) {
                        case Style.BACKGROUND_NONE:
                            byte bgt = s.getBgTransparency();
                            if(bgt == 0) {
                                return;
                            }
                            bgPaint = paintBackgroundSolidColor(bgt, s, bgPaint);
                            break;
                        case Style.BACKGROUND_IMAGE_SCALED:
                            final Paint bgImageScalePaint = new Paint();
                            bgImageScalePaint.setXfermode(PORTER);
                            final Bitmap b = (Bitmap) bgImageOrig.getImage();
                            final Rect src = new Rect();
                            src.top = 0;
                            src.bottom = b.getHeight();
                            src.left = 0;
                            src.right = b.getWidth();
                            
                            bgPaint = new AsyncPaintPosition(clip, clipP, clipIsPath) {
                                @Override
                                public void executeImpl(AndroidGraphics underlying) {
                                    Rect dest = new Rect();
                                    dest.top = y;
                                    dest.bottom = y + height;
                                    dest.left = x;
                                    dest.right = x + width;
                                    bgImageScalePaint.setAlpha(alpha);
                                    underlying.canvas.drawBitmap(b, src, dest, bgImageScalePaint);
                                }
                                public String toString() {
                                    return "BackgroundScaledImage";
                                }
                            };
                            break;
                        case Style.BACKGROUND_IMAGE_SCALED_FILL:
                            final Paint bgImageScaleFillPaint = new Paint();
                            bgImageScaleFillPaint.setXfermode(PORTER);
                            final Bitmap bFill = (Bitmap) bgImageOrig.getImage();
                            final Rect srcFill = new Rect();
                            srcFill.top = 0;
                            srcFill.bottom = bFill.getHeight();
                            srcFill.left = 0;
                            srcFill.right = bFill.getWidth();
                            final int iW = bgImageOrig.getWidth();
                            final int iH = bgImageOrig.getHeight();
                            
                            bgPaint = new AsyncPaintPosition(clip, clipP, clipIsPath) {
                                @Override
                                public void executeImpl(AndroidGraphics underlying) {
                                    Rect dest = new Rect();
                                    float r = Math.max(((float)width) / ((float)iW), ((float)height) / ((float)iH));
                                    int bwidth = (int)(((float)iW) * r);
                                    int bheight = (int)(((float)iH) * r);
                                    x = x + (width - bwidth) / 2;
                                    y = y + (height - bheight) / 2; 
                                    dest.top = y;
                                    dest.bottom = y + bheight;
                                    dest.left = x;
                                    dest.right = x + bwidth;
                                    bgImageScaleFillPaint.setAlpha(alpha);
                                    underlying.canvas.drawBitmap(bFill, srcFill, dest, bgImageScaleFillPaint);
                                }
                                public String toString() {
                                    return "ScaledImageFill";
                                }
                            };
                            break;
                        case Style.BACKGROUND_IMAGE_SCALED_FIT:
                            final Paint bgImageScaleFitPaint = new Paint();
                            final Paint bgImageScaleFitColorPaint = new Paint();
                            final byte bgtScaleFitColorAlpha = s.getBgTransparency();
                            int cc = ((bgtScaleFitColorAlpha << 24) & 0xff000000) | (s.getBgColor() & 0xffffff);
                            bgImageScaleFitColorPaint.setAntiAlias(false);
                            bgImageScaleFitColorPaint.setStyle(Paint.Style.FILL);
                            bgImageScaleFitColorPaint.setColor(cc);
                            bgImageScaleFitPaint.setAlpha(255);

                            final Bitmap bFit = (Bitmap) bgImageOrig.getImage();
                            final Rect srcFit = new Rect();
                            srcFit.top = 0;
                            srcFit.bottom = bFit.getHeight();
                            srcFit.left = 0;
                            srcFit.right = bFit.getWidth();
                            final int iWFit = bgImageOrig.getWidth();
                            final int iHFit = bgImageOrig.getHeight();
                            
                            bgPaint = new AsyncPaintPosition(clip, clipP, clipIsPath) {
                                @Override
                                public void executeImpl(AndroidGraphics underlying) {
                                    if(alpha > 0) {
                                        bgImageScaleFitColorPaint.setAlpha(alpha);
                                        underlying.canvas.drawRect(x, y, x + width, y + height, bgImageScaleFitColorPaint);
                                    }
                                    Rect dest = new Rect();
                                    float r2 = Math.min(((float)width) / ((float)iWFit), ((float)height) / ((float)iHFit));
                                    int awidth = (int)(((float)iWFit) * r2);
                                    int aheight = (int)(((float)iHFit) * r2);

                                    x = x + (width - awidth) / 2;
                                    y = y + (height - aheight) / 2; 
                                    dest.top = y;
                                    dest.bottom = y + aheight;
                                    dest.left = x;
                                    dest.right = x + awidth;
                                    underlying.canvas.drawBitmap(bFit, srcFit, dest, bgImageScaleFitPaint);
                                }
                                public String toString() {
                                    return "ScaledImageFit";
                                }
                            };
                            break;
                        case Style.BACKGROUND_IMAGE_TILE_BOTH:
                            final Paint bgImageTiledBothPaint = new Paint();
                            Bitmap bitmapTileBoth = (Bitmap) bgImageOrig.getImage();
                            BitmapShader shader = new BitmapShader(bitmapTileBoth, Shader.TileMode.REPEAT, Shader.TileMode.REPEAT);
                            bgImageTiledBothPaint.setShader(shader);
                            bgImageTiledBothPaint.setAntiAlias(false);
                            
                            bgPaint = new AsyncPaintPosition(clip, clipP, clipIsPath) {
                                @Override
                                public void executeImpl(AndroidGraphics underlying) {
                                    Rect dest = new Rect();
                                    dest.top = 0;
                                    dest.bottom = height;
                                    dest.left = 0;
                                    dest.right = width;
                                    bgImageTiledBothPaint.setAlpha(alpha);
                                    underlying.canvas.save();
                                    underlying.canvas.translate(x, y);
                                    underlying.applyTransform();
                                    underlying.canvas.drawRect(dest, bgImageTiledBothPaint);
                                    underlying.unapplyTransform();
                                    underlying.canvas.restore();
                                }
                                public String toString() {
                                    return "ImageTileBoth";
                                }
                            };
                            break;
                        case Style.BACKGROUND_IMAGE_TILE_HORIZONTAL_ALIGN_TOP:
                        case Style.BACKGROUND_IMAGE_TILE_HORIZONTAL_ALIGN_CENTER:
                        case Style.BACKGROUND_IMAGE_TILE_HORIZONTAL_ALIGN_BOTTOM:
                        case Style.BACKGROUND_IMAGE_TILE_VERTICAL_ALIGN_LEFT:
                        case Style.BACKGROUND_IMAGE_TILE_VERTICAL_ALIGN_CENTER:
                        case Style.BACKGROUND_IMAGE_TILE_VERTICAL_ALIGN_RIGHT:
                            final Paint bgImageTiledPaint = new Paint();
                            final Paint bgColorTiledPaint = new Paint();
                            final byte bgtTiledColorAlpha = s.getBgTransparency();
                            int c = ((bgtTiledColorAlpha << 24) & 0xff000000) | (s.getBgColor() & 0xffffff);
                            bgColorTiledPaint.setAntiAlias(false);
                            bgColorTiledPaint.setStyle(Paint.Style.FILL);
                            bgColorTiledPaint.setColor(c);
                            bgImageTiledPaint.setAlpha(255);
                            final Bitmap bitmapTile = (Bitmap) bgImageOrig.getImage();
                            BitmapShader shaderTile = new BitmapShader(bitmapTile, Shader.TileMode.REPEAT, Shader.TileMode.REPEAT);
                            bgImageTiledPaint.setShader(shaderTile);
                            bgImageTiledPaint.setAntiAlias(false);
                            
                            bgPaint = new AsyncPaintPosition(clip, clipP, clipIsPath) {
                                @Override
                                public void executeImpl(AndroidGraphics underlying) {
                                    // fill the solid color
                                    underlying.canvas.drawRect(x, y, x + width, y + height, bgColorTiledPaint);

                                    switch(backgroundType) {
                                        case Style.BACKGROUND_IMAGE_TILE_HORIZONTAL_ALIGN_TOP:
                                            height = Math.min(bitmapTile.getHeight(), height);
                                            break;
                                                    
                                        case Style.BACKGROUND_IMAGE_TILE_HORIZONTAL_ALIGN_CENTER: {
                                            int iH = bitmapTile.getHeight();
                                            y = y + (height / 2 - iH / 2);
                                            height = iH;
                                            break;
                                        }
                                            
                                        case Style.BACKGROUND_IMAGE_TILE_HORIZONTAL_ALIGN_BOTTOM: {
                                            int iH = bitmapTile.getHeight();
                                            y = y + (height - iH);
                                            height = iH;
                                            break;
                                        }
                                        
                                        case Style.BACKGROUND_IMAGE_TILE_VERTICAL_ALIGN_LEFT: 
                                            width = bitmapTile.getWidth();
                                            break;
                                        
                                        case Style.BACKGROUND_IMAGE_TILE_VERTICAL_ALIGN_CENTER: {
                                            int iW = bitmapTile.getWidth();
                                            x = x + (width / 2 - iW / 2);
                                            width = iW;
                                            break;
                                        }
                                        
                                        case Style.BACKGROUND_IMAGE_TILE_VERTICAL_ALIGN_RIGHT: {
                                            int iW = bitmapTile.getWidth();
                                            x = x + width - iW;
                                            width = iW;
                                        }                                        
                                    }
                                    Rect dest = new Rect();
                                    dest.top = 0;
                                    dest.bottom = height;
                                    dest.left = 0;
                                    dest.right = width;
                                    underlying.canvas.save();
                                    underlying.canvas.translate(x, y);
                                    underlying.applyTransform();
                                    underlying.canvas.drawRect(dest, bgImageTiledPaint);
                                    underlying.unapplyTransform();
                                    underlying.canvas.restore();
                                }
                                public String toString() {
                                    return "BackgroundImageTile";
                                }
                            };
                            break;
                            
                            case Style.BACKGROUND_IMAGE_ALIGNED_TOP:
                            case Style.BACKGROUND_IMAGE_ALIGNED_BOTTOM:
                            case Style.BACKGROUND_IMAGE_ALIGNED_LEFT:
                            case Style.BACKGROUND_IMAGE_ALIGNED_RIGHT:
                            case Style.BACKGROUND_IMAGE_ALIGNED_CENTER:
                            case Style.BACKGROUND_IMAGE_ALIGNED_TOP_LEFT:
                            case Style.BACKGROUND_IMAGE_ALIGNED_TOP_RIGHT:
                            case Style.BACKGROUND_IMAGE_ALIGNED_BOTTOM_LEFT:
                            case Style.BACKGROUND_IMAGE_ALIGNED_BOTTOM_RIGHT:
                                bgPaint = paintAlignedImage(s, bgImageOrig, backgroundType);
                                break;

                            case Style.BACKGROUND_GRADIENT_LINEAR_HORIZONTAL:
                            case Style.BACKGROUND_GRADIENT_LINEAR_VERTICAL:
                            case Style.BACKGROUND_GRADIENT_RADIAL:
                                bgPaint = createGradientPaint(s);
                                break;                            
                    }
                }
                if(sc == null) {
                    sc = new AndroidStyleCache();
                    StyleAccessor.setCachedData(s, sc);
                }
                sc.backgroundPainter = bgPaint;
            } else {
                bgPaint.updateClip(clip, clipP, clipIsPath);
                /*
                if (clip == null) {
                    bgPaint.pendingClipW = cn1View.width;
                    bgPaint.pendingClipH = cn1View.height;
                    bgPaint.pendingClipX = 0;
                    bgPaint.pendingClipY = 0;
                } else {
                    bgPaint.pendingClipW = clip.getWidth();
                    bgPaint.pendingClipH = clip.getHeight();
                    bgPaint.pendingClipX = clip.getX();
                    bgPaint.pendingClipY = clip.getY();
                }
                */
            }
            bgPaint.pendingX = x;
            bgPaint.pendingY = y;
            bgPaint.pendingHeight = height;
            bgPaint.pendingWidth = width;
            bgPaint.pendingAlpha = alpha;
            
            pendingRenderingOperations.add(bgPaint);
        }

        private AsyncPaintPosition paintAlignedImage(Style s, final Image bgImageOrig, final byte position) {
            final Paint bgImageAlignPaint = new Paint();
            final Paint bgImageAlignColorPaint = new Paint();
            final byte bgt = s.getBgTransparency();
            int cc = ((bgt << 24) & 0xff000000) | (s.getBgColor() & 0xffffff);
            bgImageAlignColorPaint.setAntiAlias(false);
            bgImageAlignColorPaint.setStyle(Paint.Style.FILL);
            bgImageAlignColorPaint.setColor(cc);
            bgImageAlignPaint.setAlpha(255);

            final Bitmap b = (Bitmap) bgImageOrig.getImage();
            final Rect src = new Rect();
            final int iW = bgImageOrig.getWidth();
            final int iH = bgImageOrig.getHeight();
            src.top = 0;
            src.bottom = iH;
            src.left = 0;
            src.right = iW;
                            
            return new AsyncPaintPosition(clip, clipP, clipIsPath) {
                @Override
                public void executeImpl(AndroidGraphics underlying) {
                    if(alpha > 0) {
                        bgImageAlignColorPaint.setAlpha(alpha);
                        underlying.canvas.drawRect(x, y, x + width, y + height, bgImageAlignColorPaint);
                    }
                    Rect dest = new Rect();
                    switch(position) {
                        case Style.BACKGROUND_IMAGE_ALIGNED_TOP:
                            dest.top = y;
                            dest.left = x + (width / 2 - iW / 2);
                            break;
                        case Style.BACKGROUND_IMAGE_ALIGNED_BOTTOM:
                            dest.top = y + height - iH;
                            dest.left = x + (width / 2 - iW / 2);
                            break;
                        case Style.BACKGROUND_IMAGE_ALIGNED_LEFT:
                            dest.top = y + (height / 2 - iH / 2);
                            dest.left = x;
                            break;
                        case Style.BACKGROUND_IMAGE_ALIGNED_RIGHT:
                            dest.top = y + (height / 2 - iH / 2);
                            dest.left = x + width - iW;
                            break;
                        case Style.BACKGROUND_IMAGE_ALIGNED_CENTER:
                            dest.top = y + (height / 2 - iH / 2);
                            dest.left = x + (width / 2 - iW / 2);
                            break;
                        case Style.BACKGROUND_IMAGE_ALIGNED_TOP_LEFT:
                            dest.top = y;
                            dest.left = x;
                            break;
                        case Style.BACKGROUND_IMAGE_ALIGNED_TOP_RIGHT:
                            dest.top = y;
                            dest.left = x + width - iW;
                            break;
                        case Style.BACKGROUND_IMAGE_ALIGNED_BOTTOM_LEFT:
                            dest.top = y + (height - iH);
                            dest.left = x;
                            break;
                        case Style.BACKGROUND_IMAGE_ALIGNED_BOTTOM_RIGHT:
                            dest.top = y + (height - iH);
                            dest.left = x + width - iW;
                            break;
                    }
                    dest.bottom = y + iH;
                    dest.right = x + iW;
                    underlying.canvas.drawBitmap(b, src, dest, bgImageAlignPaint);
                }
                public String toString() {
                    return "BackgroundImageAlign";
                }
            };
        }
        
        private AsyncPaintPosition paintBackgroundSolidColor(final byte bgt, Style s, AsyncPaintPosition bgPaint) {
            int c = ((bgt << 24) & 0xff000000) | (s.getBgColor() & 0xffffff);
            final Paint pnt = new Paint();
            pnt.setStyle(Paint.Style.FILL);
            pnt.setColor(c);
            pnt.setAntiAlias(false);
            bgPaint = new AsyncPaintPosition(clip, clipP, clipIsPath) {
                @Override
                public void executeImpl(AndroidGraphics underlying) {
                    if(bgt == 0) {
                        return;
                    }
                    underlying.canvas.drawRect(x, y, x + width, y + height, pnt);
                }
                public String toString() {
                    return "SolidColorBackground";
                }
            };
            return bgPaint;
        }

        class DrawStringCache {
            String text;
            int fgColor;
            CodenameOneTextPaint font;

            public DrawStringCache(String text, int color, CodenameOneTextPaint font) {
                this.text = text;
                this.fgColor = color;
                this.font = font;
                if(font == null) {
                    this.font = impl.defaultFont;
                } 
            }

            public DrawStringCache(String text, Style s) {
                this.text = text;
                this.fgColor = s.getFgColor();
                Object nativeFont = s.getFont().getNativeFont();
                if(nativeFont == null) {
                    this.font = impl.defaultFont;
                } else {
                    if(nativeFont instanceof AndroidImplementation.NativeFont) {
                        nativeFont = ((AndroidImplementation.NativeFont)nativeFont).font;
                    }
                    if(nativeFont == null) {
                        nativeFont = impl.defaultFont;
                    } 
                }
                this.font = (CodenameOneTextPaint)nativeFont;
            }

            public boolean equals(Object o) {
                return font != null && o != null && text != null &&
                        (text == ((DrawStringCache)o).text || text.equals(((DrawStringCache)o).text)) &&
                        fgColor == ((DrawStringCache)o).fgColor &&
                        (font == ((DrawStringCache)o).font || font.equals(((DrawStringCache)o).font));
            }

            public int hashCode() {
                return text.hashCode();
            }
        }

        WeakHashMap<DrawStringCache, Bitmap> drawStringCache = new WeakHashMap<DrawStringCache, Bitmap>();
        
        @Override
        public void drawLabelComponent(final int cmpX, final int cmpY, final int cmpHeight, int cmpWidth, final Style style, String text,
                                       final Bitmap icon, final Bitmap stateIcon, int preserveSpaceForState, final int gap, final boolean rtl, final boolean isOppositeSide,
                                       final int textPosition, final int stringWidth, final boolean isTickerRunning, final int tickerShiftText, final boolean endsWith3Points, final int valign) {
            if (text == null || text.equals(" ")) {
                text = "";
            }
            int fontHeight = 0;
            Font cn1Font = style.getFont();
            if (text.length() > 0) {
                fontHeight = cn1Font.getHeight();
            } else {
                if (icon == null) {
                    return;
                }
            }
            
            AndroidStyleCache styleCache =  (AndroidStyleCache)StyleAccessor.getCachedData(style);
            if(styleCache == null) {
                styleCache = new AndroidStyleCache();
                StyleAccessor.setCachedData(style, styleCache);
            }
            if(styleCache.textPaint == null) {
                Object nativeFont = cn1Font.getNativeFont();
                if (nativeFont == null) {
                    nativeFont = impl.defaultFont;
                    
                    // this is happening too early, some things aren't initialized yet
                    if(nativeFont == null) {
                        return;
                    }
                }
                if (nativeFont instanceof AndroidImplementation.NativeFont) {
                    styleCache.textPaint = new CodenameOneTextPaint((CodenameOneTextPaint) ((AndroidImplementation.NativeFont) nativeFont).font);
                } else {
                    styleCache.textPaint = new CodenameOneTextPaint((CodenameOneTextPaint) nativeFont);
                }
                
                int c = (alpha << 24) | (style.getFgColor() & 0xffffff);
                styleCache.textPaint.setColor(c);
            }          
            final CodenameOneTextPaint nativeFont = styleCache.textPaint;
            
            int iconWidth = 0;
            int iconHeight = 0;
            if(icon != null) {
                iconWidth = icon.getWidth();
                iconHeight = icon.getHeight();
            }

            int textDecoration = style.getTextDecoration();
            int stateIconSize = 0;
            int stateIconYPosition = 0;

            int leftPadding = style.getPaddingLeft(rtl);
            int rightPadding = style.getPaddingRight(rtl);
            int topPadding = style.getPaddingTop();
            int bottomPadding = style.getPaddingBottom();

            if (stateIcon != null) {
                stateIconSize = stateIcon.getWidth();
                stateIconYPosition = cmpY + topPadding
                        + (cmpHeight - topPadding
                        - bottomPadding) / 2 - stateIconSize / 2;
                int tX = cmpX;
                if (isOppositeSide) {
                    if (rtl) {
                        tX += leftPadding;
                    } else {
                        tX = tX + cmpWidth - leftPadding - stateIconSize;
                    }
                    cmpWidth -= leftPadding - stateIconSize;
                } else {
                    preserveSpaceForState = stateIconSize + gap;
                    if (rtl) {
                        tX = tX + cmpWidth - leftPadding - stateIconSize;
                    } else {
                        tX += leftPadding;
                    }
                }

                drawImage(stateIcon, tX, stateIconYPosition);
            }

            //default for bottom left alignment
            int x = cmpX + leftPadding + preserveSpaceForState;
            int y = cmpY + topPadding;

            int align = reverseAlignForBidi(rtl, style.getAlignment());

            int textPos = reverseAlignForBidi(rtl, textPosition);

            //set initial x,y position according to the alignment and textPosition
            switch (align) {
                case Component.LEFT:
                    switch (textPos) {
                        case Label.LEFT:
                        case Label.RIGHT:
                            y = y + (cmpHeight - (topPadding + bottomPadding + Math.max(((icon != null) ? iconHeight : 0), fontHeight))) / 2;
                            break;
                        case Label.BOTTOM:
                        case Label.TOP:
                            y = y + (cmpHeight - (topPadding + bottomPadding + ((icon != null) ? iconHeight + gap : 0) + fontHeight)) / 2;
                            break;
                    }
                    break;
                case Component.CENTER:
                    switch (textPos) {
                        case Label.LEFT:
                        case Label.RIGHT:
                            x = x + (cmpWidth - (preserveSpaceForState
                                    + leftPadding
                                    + rightPadding
                                    + ((icon != null) ? iconWidth + gap : 0)
                                    + stringWidth)) / 2;
                            x = Math.max(x, cmpX + leftPadding + preserveSpaceForState);
                            y = y + (cmpHeight - (topPadding
                                    + bottomPadding
                                    + Math.max(((icon != null) ? iconHeight : 0),
                                            fontHeight))) / 2;
                            break;
                        case Label.BOTTOM:
                        case Label.TOP:
                            x = x + (cmpWidth - (preserveSpaceForState + leftPadding
                                    + rightPadding
                                    + Math.max(((icon != null) ? iconWidth + gap : 0),
                                            stringWidth))) / 2;
                            x = Math.max(x, cmpX + leftPadding + preserveSpaceForState);
                            y = y + (cmpHeight - (topPadding
                                    + bottomPadding
                                    + ((icon != null) ? iconHeight + gap : 0)
                                    + fontHeight)) / 2;
                            break;
                    }
                    break;
                case Component.RIGHT:
                    switch (textPos) {
                        case Label.LEFT:
                        case Label.RIGHT:
                            x = cmpX + cmpWidth - rightPadding
                                    - (((icon != null) ? (iconWidth + gap) : 0)
                                    + stringWidth);
                            if (rtl) {
                                x = Math.max(x - preserveSpaceForState, cmpX + leftPadding);
                            } else {
                                x = Math.max(x, cmpX + leftPadding + preserveSpaceForState);
                            }
                            y = y + (cmpHeight - (topPadding
                                    + bottomPadding
                                    + Math.max(((icon != null) ? iconHeight : 0),
                                            fontHeight))) / 2;
                            break;
                        case Label.BOTTOM:
                        case Label.TOP:
                            x = cmpX + cmpWidth - rightPadding
                                    - (Math.max(((icon != null) ? (iconWidth) : 0),
                                            stringWidth));
                            x = Math.max(x, cmpX + leftPadding + preserveSpaceForState);
                            y = y + (cmpHeight - (topPadding
                                    + bottomPadding
                                    + ((icon != null) ? iconHeight + gap : 0) + fontHeight)) / 2;
                            break;
                    }
                    break;
                default:
                    break;
            }

            int textSpaceW = cmpWidth - rightPadding - leftPadding;

            if (icon != null && (textPos == Label.RIGHT || textPos == Label.LEFT)) {
                textSpaceW = textSpaceW - iconWidth;
            }

            if (stateIcon != null) {
                textSpaceW = textSpaceW - stateIconSize;
            } else {
                textSpaceW = textSpaceW - preserveSpaceForState;
            }
            
            final GeneralPath prevClip = new GeneralPath();
            getClipShape(prevClip);

            clipRect(cmpX, cmpY, cmpWidth, cmpHeight);
            /*
            if (clip == null) {
                clip = new Rectangle(cmpX, cmpY, cmpWidth, cmpHeight);
            } else {
                clip = clip.intersection(cmpX, cmpY, cmpWidth, cmpHeight);
            }*/

            Bitmap stringBmp = null;
            if(style.getTextDecoration() == 0 && cmpWidth > 0 && stringWidth > 0 && text.length() > 0 && fontHeight > 0) {
                DrawStringCache dc = new DrawStringCache(text, style);
                stringBmp = drawStringCache.get(dc);
                if (stringBmp == null) {
                    Bitmap bitmap = Bitmap.createBitmap(Math.min(cmpWidth, stringWidth), fontHeight,
                            Bitmap.Config.ARGB_8888);
                    Canvas cnv = new Canvas(bitmap);
                    cnv.drawText(text, 0, nativeFont.getFontAscent() * -1, nativeFont);
                    stringBmp = bitmap;
                    drawStringCache.put(dc, stringBmp);
                }
            }

            final Bitmap textCache = stringBmp;

            //final int al = alpha;
            final String finalText = text;
            final int finalTextSpaceW = textSpaceW;
            final int finalTextDecoration = textDecoration;
            final int finalFontHeight = fontHeight;
            final int finalTextPos = textPos;

            final int finalIconHeight = iconHeight;
            final int finalIconWidth = iconWidth;
            /*
            final int clipXX = getClipX();
            final int clipYX = getClipY();
            final int clipWX = getClipWidth();
            final int clipHX = getClipHeight();
            */
            final int finalX = x;
            final int finalY = y;

            pendingRenderingOperations.add(new AsyncOp(clip, clipP, clipIsPath) {
                @Override
                public void execute(AndroidGraphics underlying) {
                    if (icon == null) {
                        // no icon only string
                        drawLabelString(underlying, nativeFont, finalText, finalX, finalY, finalTextSpaceW, isTickerRunning, tickerShiftText,
                                finalTextDecoration, rtl, endsWith3Points, stringWidth, finalFontHeight, textCache);
                    } else {
                        int strWidth = stringWidth;
                        int iconStringWGap;
                        int iconStringHGap;

                        switch (finalTextPos) {
                            case Label.LEFT:
                                if (finalIconHeight > finalFontHeight) {
                                    iconStringHGap = (finalIconHeight - finalFontHeight) / 2;
                                    strWidth = drawLabelStringValign(underlying, nativeFont, finalText, finalX, finalY, finalTextSpaceW, isTickerRunning,
                                            tickerShiftText, finalTextDecoration, rtl, endsWith3Points, strWidth, iconStringHGap, finalIconHeight,
                                            finalFontHeight, valign, textCache);

                                    underlying.canvas.drawBitmap(icon, finalX + strWidth + gap, finalY, underlying.paint);
                                } else {
                                    iconStringHGap = (finalFontHeight - finalIconHeight) / 2;
                                    strWidth = drawLabelString(underlying, nativeFont, finalText, finalX, finalY, finalTextSpaceW, isTickerRunning,
                                            tickerShiftText, finalTextDecoration, rtl, endsWith3Points, strWidth, finalFontHeight, textCache);

                                    underlying.canvas.drawBitmap(icon, finalX + strWidth + gap, finalY + iconStringHGap, underlying.paint);
                                }
                                break;
                            case Label.RIGHT:
                                if (finalIconHeight > finalFontHeight) {
                                    iconStringHGap = (finalIconHeight - finalFontHeight) / 2;
                                    underlying.canvas.drawBitmap(icon, finalX, finalY, underlying.paint);
                                    underlying.canvas.drawBitmap(icon, finalX, finalY, underlying.paint);
                                    drawLabelStringValign(underlying, nativeFont, finalText, finalX + finalIconWidth + gap, finalY, finalTextSpaceW, isTickerRunning,
                                            tickerShiftText, finalTextDecoration, rtl, endsWith3Points, strWidth, iconStringHGap, finalIconHeight, finalFontHeight,
                                            valign, textCache);
                                } else {
                                    iconStringHGap = (finalFontHeight - finalIconHeight) / 2;
                                    underlying.canvas.drawBitmap(icon, finalX, finalY + iconStringHGap, underlying.paint);
                                    drawLabelString(underlying, nativeFont, finalText, finalX + finalIconWidth + gap, finalY, finalTextSpaceW, isTickerRunning,
                                            tickerShiftText, finalTextDecoration, rtl, endsWith3Points, strWidth, finalFontHeight, textCache);
                                }
                                break;
                            case Label.BOTTOM:
                                //center align the smaller
                                if (finalIconWidth > strWidth) {
                                    iconStringWGap = (finalIconWidth - strWidth) / 2;
                                    underlying.canvas.drawBitmap(icon, finalX, finalY, underlying.paint);
                                    drawLabelString(underlying, nativeFont, finalText, finalX + iconStringWGap, finalY + finalIconHeight + gap, finalTextSpaceW,
                                            isTickerRunning, tickerShiftText, finalTextDecoration, rtl, endsWith3Points, strWidth, finalFontHeight, textCache);
                                } else {
                                    iconStringWGap = (Math.min(strWidth, finalTextSpaceW) - finalIconWidth) / 2;
                                    underlying.canvas.drawBitmap(icon, finalX + iconStringWGap, finalY, underlying.paint);

                                    drawLabelString(underlying, nativeFont, finalText, finalX, finalY + finalIconHeight + gap, finalTextSpaceW, isTickerRunning,
                                            tickerShiftText, finalTextDecoration, rtl, endsWith3Points, strWidth, finalFontHeight, textCache);
                                }
                                break;
                            case Label.TOP:
                                //center align the smaller
                                if (finalIconWidth > strWidth) {
                                    iconStringWGap = (finalIconWidth - strWidth) / 2;
                                    drawLabelString(underlying, nativeFont, finalText, finalX + iconStringWGap, finalY, finalTextSpaceW, isTickerRunning,
                                            tickerShiftText, finalTextDecoration, rtl, endsWith3Points, strWidth, finalFontHeight, textCache);
                                    underlying.canvas.drawBitmap(icon, finalX, finalY + finalFontHeight + gap, underlying.paint);
                                } else {
                                    iconStringWGap = (Math.min(strWidth, finalTextSpaceW) - finalIconWidth) / 2;
                                    drawLabelString(underlying, nativeFont, finalText, finalX, finalY, finalTextSpaceW, isTickerRunning, tickerShiftText,
                                            finalTextDecoration, rtl, endsWith3Points, strWidth, finalFontHeight, textCache);
                                    underlying.canvas.drawBitmap(icon, finalX + iconStringWGap, finalY + finalFontHeight + gap, underlying.paint);
                                }
                                break;
                        }
                    }
                    //underlying.setClip(clipXX, clipYX, clipWX, clipHX);
                    underlying.setClip(prevClip);
                }
                public String toString() {
                    if(icon == null) {
                        return "drawLabelComponent null icon";
                    }
                    if(finalText.length() > 0) {
                        return "drawLabelComponent with icon & text";
                    } else {
                        return "drawLabelComponent with icon";
                    }
                }
            });
            setClip(prevClip);
            
        }
                
        /**
         * Implements the drawString for the text component and adjust the valign
         * assuming the icon is in one of the sides
         */
        private int drawLabelStringValign(AndroidGraphics underlying, 
                CodenameOneTextPaint nativeFont, String str, int x, int y, int textSpaceW,
                boolean isTickerRunning, int tickerShiftText, int textDecoration, boolean rtl,
                boolean endsWith3Points, int textWidth,
                int iconStringHGap, int iconHeight, int fontHeight, int valign, Bitmap textCache) {
            if(str.length() == 0) {
                return 0;
            }
            switch (valign) {
                case Component.TOP:
                    return drawLabelString(underlying, nativeFont, str, x, y, textSpaceW, isTickerRunning, tickerShiftText, textDecoration, rtl, endsWith3Points, textWidth, fontHeight, textCache);
                case Component.CENTER:
                    return drawLabelString(underlying, nativeFont, str, x, y + iconHeight / 2 - fontHeight / 2, textSpaceW, isTickerRunning, tickerShiftText, textDecoration, rtl, endsWith3Points, textWidth, fontHeight, textCache);
                default:
                    return drawLabelString(underlying, nativeFont, str, x, y + iconStringHGap, textSpaceW, isTickerRunning, tickerShiftText, textDecoration, rtl, endsWith3Points, textWidth, fontHeight, textCache);
            }
        }

        /**
         * Implements the drawString for the text component and adjust the valign
         * assuming the icon is in one of the sides
         */
        private int drawLabelString(AndroidGraphics underlying, CodenameOneTextPaint nativeFont, String text, int x, int y, int textSpaceW,
                boolean isTickerRunning, int tickerShiftText, int textDecoration, boolean rtl, boolean endsWith3Points, int textWidth,
                int fontHeight, Bitmap textCache) {
            if(text.length() == 0) {
                return 0;
            }
            int cx = underlying.getClipX();
            int cy = underlying.getClipY();
            int cw = underlying.getClipWidth();
            int ch = underlying.getClipHeight();
            underlying.clipRect(x, cy, textSpaceW, ch);

            int drawnW = drawLabelText(underlying, textDecoration, rtl, isTickerRunning, endsWith3Points, nativeFont,
                    textWidth, textSpaceW, tickerShiftText, text, x, y, fontHeight, textCache);

            underlying.setClip(cx, cy, cw, ch);
            return drawnW;
            //underlying.canvas.drawText(text, x, y - nativeFont.getFontAscent(), nativeFont);

            //return textSpaceW;
        }
        
        private boolean fastCharWidthCheck(String s, int length, int width, int charWidth, Object f) {
            if (length * charWidth < width) {
                return true;
            }
            length = Math.min(s.length(), length);
            return impl.stringWidth(f, s.substring(0, length)) < width;
        }

        /**
         * Draws the text of a label
         *
         * @param textDecoration decoration information for the text
         * @param text the text for the label
         * @param x position for the label
         * @param y position for the label
         * @param txtW stringWidth(text) equivalent which is faster than just
         * invoking string width all the time
         * @param textSpaceW the width available for the component
         * @return the space used by the drawing
         */
        protected int drawLabelText(AndroidGraphics underlying, int textDecoration, boolean rtl, boolean isTickerRunning,
                boolean endsWith3Points, CodenameOneTextPaint nativeFont, int txtW, int textSpaceW, int shiftText, String text,
                int x, int y, int fontHeight, Bitmap textCache) {
            if ((!isTickerRunning) || rtl) {
                //if there is no space to draw the text add ... at the end
                if (txtW > textSpaceW && textSpaceW > 0) {
                    // Handling of adding 3 points and in fact all text positioning when the text is bigger than
                    // the allowed space is handled differently in RTL, this is due to the reverse algorithm
                    // effects - i.e. when the text includes both Hebrew/Arabic and English/numbers then simply
                    // trimming characters from the end of the text (as done with LTR) won't do.
                    // Instead we simple reposition the text, and draw the 3 points, this is quite simple, but
                    // the downside is that a part of a letter may be shown here as well.

                    if (rtl) {
                        if ((!isTickerRunning) && endsWith3Points) {
                            String points = "...";
                            int pointsW = impl.stringWidth(nativeFont, points);
                            drawString(underlying, nativeFont, points, shiftText + x, y, textDecoration, fontHeight, textCache);
                            clipRect(pointsW + shiftText + x, y, textSpaceW - pointsW, fontHeight);
                        }
                        x = x - txtW + textSpaceW;
                    } else if (endsWith3Points) {
                        String points = "...";
                        int index = 1;
                        int widest = impl.charWidth(nativeFont, 'W');
                        int pointsW = impl.stringWidth(nativeFont, points);
                        while (fastCharWidthCheck(text, index, textSpaceW - pointsW, widest, nativeFont) && index < text.length()) {
                            index++;
                        }
                        text = text.substring(0, Math.min(text.length(), Math.max(1, index - 1))) + points;
                        txtW = impl.stringWidth(nativeFont, text);
                    }
                }
            }

            drawString(underlying, nativeFont, text, shiftText + x, y, textDecoration, fontHeight, textCache);
            return Math.min(txtW, textSpaceW);
        }

        /**
         * Draw a string using the current font and color in the x,y coordinates.
         * The font is drawn from the top position and not the baseline.
         *
         * @param nativeFont the font used
         * @param str the string to be drawn.
         * @param x the x coordinate.
         * @param y the y coordinate.
         * @param textDecoration Text decoration bitmask (See Style's
         * TEXT_DECORATION_* constants)
         */
        private void drawString(AndroidGraphics underlying, CodenameOneTextPaint nativeFont, String str, int x, int y, int textDecoration, int fontHeight, Bitmap textCache) {
            // this if has only the minor effect of providing a slighly faster execution path
            if (textDecoration != 0) {
                boolean raised = (textDecoration & Style.TEXT_DECORATION_3D) != 0;
                boolean lowerd = (textDecoration & Style.TEXT_DECORATION_3D_LOWERED) != 0;
                boolean north = (textDecoration & Style.TEXT_DECORATION_3D_SHADOW_NORTH) != 0;
                if (raised || lowerd || north) {
                    textDecoration = textDecoration & (~Style.TEXT_DECORATION_3D) & (~Style.TEXT_DECORATION_3D_LOWERED) & (~Style.TEXT_DECORATION_3D_SHADOW_NORTH);
                    int c = getColor();
                    int a = getAlpha();
                    int newColor = 0;
                    int offset = -2;
                    if (lowerd) {
                        offset = 2;
                        newColor = 0xffffff;
                    } else if (north) {
                        offset = 2;
                    }
                    setColor(newColor);
                    if (a == 0xff) {
                        setAlpha(140);
                    }
                    drawString(underlying, nativeFont, str, x, y + offset, textDecoration, fontHeight, textCache);
                    setAlpha(a);
                    setColor(c);
                    drawString(underlying, nativeFont, str, x, y, textDecoration, fontHeight, textCache);
                    return;
                }
                underlying.canvas.drawText(str, x, y - font.getFontAscent(), nativeFont);
                if ((textDecoration & Style.TEXT_DECORATION_UNDERLINE) != 0) {
                    underlying.paint.setStyle(Paint.Style.FILL);
                    underlying.canvas.drawLine(x, y + fontHeight - 1, x + impl.stringWidth(nativeFont, str), y + fontHeight - 1, underlying.paint);
                }
                if ((textDecoration & Style.TEXT_DECORATION_STRIKETHRU) != 0) {
                    underlying.paint.setStyle(Paint.Style.FILL);
                    underlying.canvas.drawLine(x, y + fontHeight / 2, x + impl.stringWidth(nativeFont, str), y + fontHeight / 2, underlying.paint);
                }
                if ((textDecoration & Style.TEXT_DECORATION_OVERLINE) != 0) {
                    underlying.paint.setStyle(Paint.Style.FILL);
                    underlying.canvas.drawLine(x, y, x + impl.stringWidth(nativeFont, str), y, underlying.paint);
                }
            } else {
                if(textCache != null) {
                    underlying.canvas.drawBitmap(textCache, x, y, nativeFont);
                } else {
                    underlying.canvas.drawText(str, x, y - nativeFont.getFontAscent(), nativeFont);
                }
            }
        }
        

        @Override
        public void fillArc(final int x, final int y, final int width, final int height, final int startAngle, final int arcAngle) {
            final int alph = alpha;
            final int col = color;
            pendingRenderingOperations.add(new AsyncOp(clip, clipP, clipIsPath) {
                @Override
                public void execute(AndroidGraphics underlying) {
                    underlying.setAlpha(alph);
                    underlying.setColor(col);
                    underlying.fillArc(x, y, width, height, startAngle, arcAngle);
                }
                public String toString() {
                    return "fillArc";
                }
            });
        }

        @Override
        public void drawArc(final int x, final int y, final int width, final int height, final int startAngle, final int arcAngle) {
            final int alph = alpha;
            final int col = color;
            pendingRenderingOperations.add(new AsyncOp(clip, clipP, clipIsPath) {
                @Override
                public void execute(AndroidGraphics underlying) {
                    underlying.setAlpha(alph);
                    underlying.setColor(col);
                    underlying.drawArc(x, y, width, height, startAngle, arcAngle);
                }
                public String toString() {
                    return "drawArc";
                }
            });
        }

        @Override
        public void drawString(final String str, final int x, final int y) {
            final int col = this.color;
            Paint fnt = getFont();
            if(fnt == null) {
                fnt = impl.defaultFont;
            }

            final CodenameOneTextPaint font = (CodenameOneTextPaint)fnt;
            final int alph = this.alpha;

            Bitmap stringBmp = null;
            if(!legacyPaintLogic) {
                int stringWidth = (int) Math.ceil(font.measureText(str));
                if (font.fontHeight < 0) {
                    font.fontHeight = font.getFontMetricsInt(font.getFontMetricsInt());
                }

                if (stringWidth > 0 && font.fontHeight > 0) {
                    DrawStringCache dc = new DrawStringCache(str, col, font);
                    stringBmp = drawStringCache.get(dc);
                    if (stringBmp == null) {
                        Bitmap bitmap = Bitmap.createBitmap(stringWidth, font.fontHeight,
                                Bitmap.Config.ARGB_8888);
                        Canvas cnv = new Canvas(bitmap);
                        int c = 0xff000000 | color;
                        font.setColor(c);
                        cnv.drawText(str, 0, font.getFontAscent() * -1, font);
                        stringBmp = bitmap;
                        drawStringCache.put(dc, stringBmp);
                    }
                }
            }

            final Bitmap textCache = stringBmp;
            pendingRenderingOperations.add(new AsyncOp(clip, clipP, clipIsPath) {
                @Override
                public void execute(AndroidGraphics underlying) {
                    if(textCache != null) {
                        underlying.canvas.drawBitmap(textCache, x, y, underlying.paint);
                    } else {
                        underlying.setFont(font);
                        font.setColor(col);
                        font.setAlpha(alph);
                        underlying.drawString(str, x, y);
                    }
                }
                public String toString() {
                    return "drawString";
                }
            });
        }

        @Override
        public void drawRoundRect(final int x, final int y, final int width, final int height, final int arcWidth, final int arcHeight) {
            final int alph = alpha;
            final int col = color;
            pendingRenderingOperations.add(new AsyncOp(clip, clipP, clipIsPath) {
                @Override
                public void execute(AndroidGraphics underlying) {
                    underlying.setAlpha(alph);
                    underlying.setColor(col);
                    underlying.drawRoundRect(x, y, width, height, arcWidth, arcHeight);
                }
                public String toString() {
                    return "drawRoundRect";
                }
            });
        }

        @Override
        public void drawRect(final int x, final int y, final int width, final int height) {
            final int alph = alpha;
            final int col = color;
            pendingRenderingOperations.add(new AsyncOp(clip, clipP, clipIsPath) {
                @Override
                public void execute(AndroidGraphics underlying) {
                    underlying.setAlpha(alph);
                    underlying.setColor(col);
                    underlying.drawRect(x, y, width, height);
                }
                public String toString() {
                    return "drawRect";
                }
            });
        }

        @Override
        public void drawRGB(final int[] rgbData, final int offset, final int x, final int y, final int w, final int h, final boolean processAlpha) {
            pendingRenderingOperations.add(new AsyncOp(clip, clipP, clipIsPath) {
                @Override
                public void execute(AndroidGraphics underlying) {
                    Paint p = underlying.getPaint();
                    underlying.setPaint(imagePaint);
                    underlying.drawRGB(rgbData, offset, x, y, w, h, processAlpha);
                    underlying.setPaint(p);
                }
                public String toString() {
                    return "drawRGB";
                }
            });
        }

        @Override
        public void fillPolygon(final int[] xPoints, final int[] yPoints, final int nPoints) {
            final int alph = alpha;
            final int col = color;
            pendingRenderingOperations.add(new AsyncOp(clip, clipP, clipIsPath) {
                @Override
                public void execute(AndroidGraphics underlying) {
                    underlying.setAlpha(alph);
                    underlying.setColor(col);
                    underlying.fillPolygon(xPoints, yPoints, nPoints);
                }
                public String toString() {
                    return "fillPolygon";
                }
            });
        }

        @Override
        public void drawPolygon(final int[] xPoints, final int[] yPoints, final int nPoints) {
            final int alph = alpha;
            final int col = color;
            pendingRenderingOperations.add(new AsyncOp(clip, clipP, clipIsPath) {
                @Override
                public void execute(AndroidGraphics underlying) {
                    underlying.setAlpha(alph);
                    underlying.setColor(col);
                    underlying.drawPolygon(xPoints, yPoints, nPoints);
                }

                public String toString() {
                    return "drawPolygon";
                }
            });
        }

        @Override
        public void drawLine(final int x1, final int y1, final int x2, final int y2) {
            final int alph = alpha;
            final int col = color;
            pendingRenderingOperations.add(new AsyncOp(clip, clipP, clipIsPath) {
                @Override
                public void execute(AndroidGraphics underlying) {
                    underlying.setAlpha(alph);
                    underlying.setColor(col);
                    underlying.drawLine(x1, y1, x2, y2);
                }
                public String toString() {
                    return "drawLine";
                }
            });
        }

        @Override
        public void tileImage(final Object img, final int x, final int y, final int w, final int h) {
            final int alph = alpha;
            pendingRenderingOperations.add(new AsyncOp(clip, clipP, clipIsPath) {
                @Override
                public void execute(AndroidGraphics underlying) {
                    Paint p = underlying.getPaint();
                    underlying.setPaint(imagePaint);
                    imagePaint.setAlpha(alph);
                    underlying.tileImage(img, x, y, w, h);
                    underlying.setPaint(p);
                }
                public String toString() {
                    return "tileImage";
                }
            });
        }

        @Override
        public void drawImage(final Object img, final int x, final int y, final int w, final int h) {
            final int alph = alpha;
            pendingRenderingOperations.add(new AsyncOp(clip, clipP, clipIsPath) {
                @Override
                public void execute(AndroidGraphics underlying) {
                    Paint p = underlying.getPaint();
                    underlying.setPaint(imagePaint);
                    imagePaint.setAlpha(alph);
                    underlying.drawImage(img, x, y, w, h);
                    underlying.setPaint(p);
                }
                public String toString() {
                    return "drawImageWH";
                }
            });
        }

        @Override
        public void drawImage(final Object img, final int x, final int y) {
            final int alph = alpha;
            pendingRenderingOperations.add(new AsyncOp(clip, clipP, clipIsPath) {
                @Override
                public void execute(AndroidGraphics underlying) {
                    Paint p = underlying.getPaint();
                    underlying.setPaint(imagePaint);
                    imagePaint.setAlpha(alph);
                    underlying.drawImage(img, x, y);
                    underlying.setPaint(p);
                }
                public String toString() {
                    return "drawImage";
                }
            });
        }

        public void drawPath(final Path p, final Stroke stroke) {
            final int alph = alpha;
            final int col = color;
            final Path path = new Path();
            path.set(p);
            final Stroke st = new Stroke();
            st.setStroke(stroke);
            pendingRenderingOperations.add(new AsyncOp(clip, clipP, clipIsPath) {
                @Override
                public void execute(AndroidGraphics underlying) {
                    underlying.setAlpha(alph);
                    underlying.setColor(col);
                    underlying.drawPath(path, st);
                }
                public String toString() {
                    return "drawPath";
                }
            });
        }

        public void fillPath(final Path p) {
            final int alph = alpha;
            final int col = color;
            final Path path = new Path();
            path.set(p);
            pendingRenderingOperations.add(new AsyncOp(clip, clipP, clipIsPath) {
                @Override
                public void execute(AndroidGraphics underlying) {
                    underlying.setAlpha(alph);
                    underlying.setColor(col);
                    //underlying.setTransform(transform);
                    underlying.fillPath(path);
                }
                public String toString() {
                    return "fillPath";
                }
            });
        }

        public void setTransform(final Transform transform) {
            getTransform().setTransform(transform);
            transformDirty = true;
            inverseTransformDirty = true;
            clipFresh = false;
            final Transform fTransform = Transform.makeIdentity();
            fTransform.setTransform(transform);
            //this.transform = fTransform;
            pendingRenderingOperations.add(new AsyncOp(clip, clipP, clipIsPath) {
                @Override
                public void execute(AndroidGraphics underlying) {
                    underlying.setTransform(fTransform);
                }

                @Override
                public void executeWithClip(AndroidGraphics underlying) {
                    // We don't need the clip for the setTransform() op
                    execute(underlying);
                }

                public String toString() {
                    return "setTransform";
                }
            });
        }

        public Transform getTransform() {
            if (transform == null) {
                transform = Transform.makeIdentity();
            }
            return transform;
        }

        @Override
        Paint getPaint() {
            return super.getPaint();
        }
        
        @Override
        void setColor(final int clr) {
            this.color = clr;
            
            // workaround for potential OOM
            if(pendingRenderingOperations.size() > 20000 && impl.isMinimized()) {
                pendingRenderingOperations.clear();
            }
        }

        private CodenameOneTextPaint font;

        @Override
        void setFont(final CodenameOneTextPaint font) {
            this.font = font;
        }

        @Override
        Paint getFont() {
            return font;
        }

        @Override
        void setCanvas(Canvas canvas) {
            //super.setCanvas(canvas); 
        }
    }

    public boolean alwaysRepaintAll() {
        return true;
    }

    @Override
    protected void onFocusChanged(boolean gainFocus, int direction, Rect previouslyFocusedRect) {
        super.onFocusChanged(gainFocus, direction, previouslyFocusedRect);
        //do not let other views steal the focus from the main view
        if(!gainFocus && implementation.hasViewAboveBelow()){
            requestFocus();
            if(implementation.getCurrentForm() != null){
                implementation.getCurrentForm().repaint();
            }
        }
    }

}
