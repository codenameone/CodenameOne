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
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Rect;
import android.graphics.Typeface;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputConnection;
import com.codename1.ui.Component;
import com.codename1.ui.Display;
import com.codename1.ui.TextField;
import com.codename1.ui.geom.Rectangle;
import java.util.ArrayList;
import com.codename1.ui.Transform;

public class AndroidAsyncView extends View implements CodenameOneSurface {

    abstract class AsyncOp {

        int clipX;
        int clipY;
        int clipW;
        int clipH;

        public AsyncOp(Rectangle clip) {
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

        public void executeWithClip(AndroidGraphics underlying) {
            underlying.setClip(clipX, clipY, clipW, clipH);
            execute(underlying);
        }

        public abstract void execute(AndroidGraphics underlying);
    }
    private ArrayList<AsyncOp> renderingOperations = new ArrayList<AsyncOp>();
    private ArrayList<AsyncOp> pendingRenderingOperations = new ArrayList<AsyncOp>();
    private final CodenameOneView cn1View;
    private int clipX, clipY, clipW, clipH;
    private final AndroidGraphics graphics;
    private final AndroidGraphics internalGraphics;
    private final AndroidImplementation implementation;

    public AndroidAsyncView(Activity activity, AndroidImplementation implementation) {
        super(activity);
        setId(2001);
        this.implementation = implementation;
        graphics = new AsyncGraphics(implementation);
        internalGraphics = new AndroidGraphics(implementation, null);
        cn1View = new CodenameOneView(activity, this, implementation, false);
        setWillNotCacheDrawing(true);
        setWillNotDraw(false);
        setBackgroundDrawable(null);
    }

    @Override
    protected void onDraw(Canvas c) {
        boolean paintOnBuffer = implementation.isEditingText() || InPlaceEditView.isKeyboardShowing() || implementation.nativePeers.size() > 0;

        internalGraphics.setCanvasNoSave(c);
        AndroidGraphics g = internalGraphics;
        if (paintOnBuffer) {
            g = cn1View.getGraphics();
        }
        for (AsyncOp o : renderingOperations) {
            o.executeWithClip(g);
        }
        renderingOperations.clear();

        if (paintOnBuffer) {
            cn1View.d(c);
        }

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
        // method used for View implementation. is it still
        // required with a SurfaceView?
        super.onWindowVisibilityChanged(visibility);
        this.visibilityChangedTo(visibility == View.VISIBLE);
    }

    @Override
    protected void onSizeChanged(final int w, final int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);

        if (!Display.isInitialized()) {
            return;
        }
        Display.getInstance().callSerially(new Runnable() {
            public void run() {
                cn1View.handleSizeChange(w, h);
            }
        });
    }

    @Override
    public void flushGraphics(Rect rect) {
        //Log.d(Display.getInstance().getProperty("AppName", "CodenameOne"), "Flush graphics invoked with pending: " + pendingRenderingOperations.size() + " and current " + renderingOperations.size());

        // we might have pending entries in the rendering queue
        int counter = 0;
        while (!renderingOperations.isEmpty()) {
            try {
                Thread.sleep(5);

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
        if (rect == null) {
            postInvalidate();
        } else {
            clipX = rect.left;
            clipY = rect.top;
            clipW = rect.right - rect.left;
            clipH = rect.bottom - rect.top;
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
    public boolean onTouchEvent(MotionEvent event) {
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

    class AsyncGraphics extends AndroidGraphics {

        private Rectangle clip = null;
        private int alpha;
        private int color;
        private Paint imagePaint = new Paint();
        private Transform transform;
        

        AsyncGraphics(AndroidImplementation impl) {
            super(impl, null);
        }

        @Override
        public void rotate(final float angle, final int x, final int y) {
            pendingRenderingOperations.add(new AsyncOp(clip) {
                @Override
                public void execute(AndroidGraphics underlying) {
                    underlying.rotate(angle, x, y);
                }
            });
        }

        @Override
        public void rotate(final float angle) {
            pendingRenderingOperations.add(new AsyncOp(clip) {
                @Override
                public void execute(AndroidGraphics underlying) {
                    underlying.rotate(angle);
                }
            });
        }

        @Override
        public void scale(final float x, final float y) {
            pendingRenderingOperations.add(new AsyncOp(clip) {
                @Override
                public void execute(AndroidGraphics underlying) {
                    underlying.scale(x, y);
                }
            });
        }

        @Override
        public void resetAffine() {
            pendingRenderingOperations.add(new AsyncOp(clip) {
                @Override
                public void execute(AndroidGraphics underlying) {
                    underlying.resetAffine();
                }
            });
        }

        @Override
        public int getColor() {
            return color;
        }

        @Override
        public void clipRect(final int x, final int y, final int width, final int height) {
            if (clip == null) {
                clip = new Rectangle(x, y, width, height);
            } else {
                clip = clip.intersection(x, y, width, height);
            }
        }

        @Override
        public void setClip(final int x, final int y, final int width, final int height) {
            if (clip == null) {
                clip = new Rectangle(x, y, width, height);
            } else {
                clip.setX(x);
                clip.setY(y);
                clip.setWidth(width);
                clip.setHeight(height);
            }
        }

        @Override
        public int getClipY() {
            if (clip != null) {
                return clip.getY();
            }
            return 0;
        }

        @Override
        public int getClipX() {
            if (clip != null) {
                return clip.getX();
            }
            return 0;
        }

        @Override
        public int getClipWidth() {
            if (clip != null) {
                return clip.getWidth();
            }
            return cn1View.width;
        }

        @Override
        public int getClipHeight() {
            if (clip != null) {
                return clip.getHeight();
            }
            return cn1View.height;
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
            pendingRenderingOperations.add(new AsyncOp(clip) {
                @Override
                public void execute(AndroidGraphics underlying) {
                    underlying.setAlpha(alph);
                    underlying.setColor(col);
                    underlying.fillRoundRect(x, y, width, height, arcWidth, arcHeight);
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
            pendingRenderingOperations.add(new AsyncOp(clip) {
                @Override
                public void execute(AndroidGraphics underlying) {
                    underlying.setColor(col);
                    underlying.setAlpha(al);
                    underlying.fillRect(x, y, width, height);
                }
            });
        }

        @Override
        public void fillArc(final int x, final int y, final int width, final int height, final int startAngle, final int arcAngle) {
            final int alph = alpha;
            final int col = color;
            pendingRenderingOperations.add(new AsyncOp(clip) {
                @Override
                public void execute(AndroidGraphics underlying) {
                    underlying.setAlpha(alph);
                    underlying.setColor(col);
                    underlying.fillArc(x, y, width, height, startAngle, arcAngle);
                }
            });
        }

        @Override
        public void drawArc(final int x, final int y, final int width, final int height, final int startAngle, final int arcAngle) {
            final int alph = alpha;
            final int col = color;
            pendingRenderingOperations.add(new AsyncOp(clip) {
                @Override
                public void execute(AndroidGraphics underlying) {
                    underlying.setAlpha(alph);
                    underlying.setColor(col);
                    underlying.drawArc(x, y, width, height, startAngle, arcAngle);
                }
            });
        }

        @Override
        public void drawString(final String str, final int x, final int y) {
            final int col = this.color;
            final float size = getFont().getTextSize();
            final Typeface type = getFont().getTypeface();
            final int alph = this.alpha;
            pendingRenderingOperations.add(new AsyncOp(clip) {
                @Override
                public void execute(AndroidGraphics underlying) {
                    Paint p = underlying.getFont();
                    p.setTypeface(type);
                    p.setTextSize(size);
                    p.setAntiAlias(true);
                    p.setColor(col);
                    p.setAlpha(alph);                    
                    underlying.drawString(str, x, y);
                }
            });
        }

        @Override
        public void drawRoundRect(final int x, final int y, final int width, final int height, final int arcWidth, final int arcHeight) {
            final int alph = alpha;
            final int col = color;
            pendingRenderingOperations.add(new AsyncOp(clip) {
                @Override
                public void execute(AndroidGraphics underlying) {
                    underlying.setAlpha(alph);
                    underlying.setColor(col);
                    underlying.drawRoundRect(x, y, width, height, arcWidth, arcHeight);
                }
            });
        }

        @Override
        public void drawRect(final int x, final int y, final int width, final int height) {
            final int alph = alpha;
            final int col = color;
            pendingRenderingOperations.add(new AsyncOp(clip) {
                @Override
                public void execute(AndroidGraphics underlying) {
                    underlying.setAlpha(alph);
                    underlying.setColor(col);
                    underlying.drawRect(x, y, width, height);
                }
            });
        }

        @Override
        public void drawRGB(final int[] rgbData, final int offset, final int x, final int y, final int w, final int h, final boolean processAlpha) {
            pendingRenderingOperations.add(new AsyncOp(clip) {
                @Override
                public void execute(AndroidGraphics underlying) {
                    Paint p = underlying.getPaint();
                    underlying.setPaint(imagePaint);
                    underlying.drawRGB(rgbData, offset, x, y, w, h, processAlpha);
                    underlying.setPaint(p);
                }
            });
        }

        @Override
        public void fillPolygon(final int[] xPoints, final int[] yPoints, final int nPoints) {
            final int alph = alpha;
            final int col = color;
            pendingRenderingOperations.add(new AsyncOp(clip) {
                @Override
                public void execute(AndroidGraphics underlying) {
                    underlying.setAlpha(alph);
                    underlying.setColor(col);
                    underlying.fillPolygon(xPoints, yPoints, nPoints);
                }
            });
        }

        @Override
        public void drawPolygon(final int[] xPoints, final int[] yPoints, final int nPoints) {
            final int alph = alpha;
            final int col = color;
            pendingRenderingOperations.add(new AsyncOp(clip) {
                @Override
                public void execute(AndroidGraphics underlying) {
                    underlying.setAlpha(alph);
                    underlying.setColor(col);
                    underlying.drawPolygon(xPoints, yPoints, nPoints);
                }
            });
        }

        @Override
        public void drawLine(final int x1, final int y1, final int x2, final int y2) {
            final int alph = alpha;
            final int col = color;
            pendingRenderingOperations.add(new AsyncOp(clip) {
                @Override
                public void execute(AndroidGraphics underlying) {
                    underlying.setAlpha(alph);
                    underlying.setColor(col);
                    underlying.drawLine(x1, y1, x2, y2);
                }
            });
        }

        @Override
        public void drawImage(final Object img, final int x, final int y, final int w, final int h) {
            final int alph = alpha;
            pendingRenderingOperations.add(new AsyncOp(clip) {
                @Override
                public void execute(AndroidGraphics underlying) {
                    Paint p = underlying.getPaint();
                    underlying.setPaint(imagePaint);
                    imagePaint.setAlpha(alph);
                    underlying.drawImage(img, x, y, w, h);
                    underlying.setPaint(p);
                }
            });
        }

        @Override
        public void drawImage(final Object img, final int x, final int y) {
            final int alph = alpha;
            pendingRenderingOperations.add(new AsyncOp(clip) {
                @Override
                public void execute(AndroidGraphics underlying) {
                    Paint p = underlying.getPaint();
                    underlying.setPaint(imagePaint);
                    imagePaint.setAlpha(alph);
                    underlying.drawImage(img, x, y);
                    underlying.setPaint(p);
                }
            });
        }

        public void drawPath(final Path p) {
            final int alph = alpha;
            final int col = color;
            pendingRenderingOperations.add(new AsyncOp(clip) {
                @Override
                public void execute(AndroidGraphics underlying) {
                    underlying.setAlpha(alph);
                    underlying.setColor(col);
                    underlying.drawPath(p);
                }
            });
        }

        public void fillPath(final Path p) {
            final int alph = alpha;
            final int col = color;
            
            pendingRenderingOperations.add(new AsyncOp(clip) {
                @Override
                public void execute(AndroidGraphics underlying) {
                    underlying.setAlpha(alph);
                    underlying.setColor(col);
                    //underlying.setTransform(transform);
                    underlying.fillPath(p);
                }
            });
        } 
        
        public void setTransform(final Transform transform) {
            this.transform = transform;
            pendingRenderingOperations.add(new AsyncOp(clip) {
                @Override
                public void execute(AndroidGraphics underlying) {
                    underlying.setTransform(transform);
                }
            });
        }

        public Transform getTransform() {
            return transform;
        }

        @Override
        Paint getPaint() {
            return super.getPaint();
        }

        @Override
        void setColor(final int clr) {
            this.color = clr;
            /*pendingRenderingOperations.add(new AsyncOp(clip) {
             @Override
             public void execute(AndroidGraphics underlying) {
             underlying.setColor(clr);
             }
             });*/
        }

        @Override
        void setFont(final Paint font) {
            super.setFont(font);
        }

        @Override
        Paint getFont() {
            return super.getFont();
        }

        @Override
        void setCanvas(Canvas canvas) {
            //super.setCanvas(canvas); 
        }
    }

    public boolean alwaysRepaintAll() {
        return true;
    }
}
