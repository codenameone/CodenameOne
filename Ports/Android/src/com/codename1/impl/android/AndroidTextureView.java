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
import android.graphics.Rect;
import android.graphics.SurfaceTexture;
import android.util.Log;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.TextureView;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputConnection;
import com.codename1.ui.Component;
import com.codename1.ui.Display;
import com.codename1.ui.TextField;

public class AndroidTextureView extends TextureView implements CodenameOneSurface{

    private CodenameOneView cn1View;
    private volatile boolean created = false;
    private AndroidImplementation implementation;

    public AndroidTextureView(Activity activity, AndroidImplementation implementation) {
        super(activity);
        this.implementation = implementation;
        setId(2001);

        cn1View = new CodenameOneView(activity, this, implementation, false);
        setSurfaceTextureListener(new SurfaceTextureListener() {

            public void onSurfaceTextureAvailable(SurfaceTexture surface, int width, int height) {
                created = true;
                cn1View.onSurfaceCreated();
            }

            public void onSurfaceTextureSizeChanged(SurfaceTexture surface, int width, int height) {
                cn1View.onSurfaceChanged(width, height);
            }

            public boolean onSurfaceTextureDestroyed(SurfaceTexture surface) {
                created = false;
                cn1View.onSurfaceDestroyed();
                return true;
            }

            public void onSurfaceTextureUpdated(SurfaceTexture surface) {
                
            }
        });
    }

    @Override
    public Rect getSafeAreaInsets() {
        return cn1View.getSafeArea();
    }

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


    public void flushGraphics(Rect rect) {
        if (!created) {
            return;
        }
        Canvas c = null;
        try {
            c = lockCanvas(rect);
            if (c != null) {
                cn1View.d(c);
            }
            
        } catch (Throwable e) {
            Log.e("Codename One", "paint problem.", e);
        } finally {
            if (c != null) {
                try {
                    c.restoreToCount(1);
                    this.unlockCanvasAndPost(c);                    
                } catch (Exception e) {
                    Log.e("Codename One", "unlockCanvasAndPost err", e);                    
                }
            }
        }
        if (implementation.isAsyncEditMode() && implementation.isEditingText()) {
            InPlaceEditView.reLayoutEdit();
        }
    }

    public void flushGraphics() {
        if (!created) {
            return;
        }
        Canvas c = null;
        try {
            c = lockCanvas();
            if (c != null) {
                cn1View.d(c);
            }            
        } catch (Throwable e) {
            Log.e("Codename One", "paint problem.", e);
        } finally {
            if (c != null) {
                try {
                    c.restoreToCount(1);
                    this.unlockCanvasAndPost(c);                    
                } catch (Exception e) {
                    Log.e("Codename One", "unlockCanvasAndPost err", e);                    
                }
            }
        }
        if (implementation.isAsyncEditMode() && implementation.isEditingText()) {
            InPlaceEditView.reLayoutEdit();
        }
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

    public AndroidGraphics getGraphics() {
        return cn1View.buffy;
    }

    public int getViewHeight() {
        return cn1View.height;
    }

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

    public boolean alwaysRepaintAll() {
        return false;
    }
}
