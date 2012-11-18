/*
 * Copyright 2009 Pader-Sync Ltd. & Co. KG.  All Rights Reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.
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
 * Visit http://www.pader-sync.com/ for contact information.
 *
 *
 * Linking this library statically or dynamically with other modules is making a
 * combined work based on this library. Thus, the terms and conditions of the GNU
 * General Public License cover the whole combination.
 *
 *    As a special exception, the copyright holders of this library give you
 *    permission to link this library with independent modules to produce an
 *    executable, regardless of the license terms of these independent modules, and
 *    to copy and distribute the resulting executable under terms of your choice,
 *    provided that you also meet, for each linked independent module, the terms
 *    and conditions of the license of that module. An independent module is a
 *    module which is not derived from or based on this library. If you modify this
 *    library, you may extend this exception to your version of the library, but you
 *    are not obligated to do so. If you do not wish to do so, delete this exception
 *    statement from your version.
 *
 * March 2009
 * Thorsten Schemm
 * http://www.pader-sync.com/
 *
 */
package com.codename1.impl.android;

import android.graphics.Canvas;
import android.graphics.Paint;

/**
 * #######################################################################
 * #######################################################################
 *
 * Bundle one canvas and two paints to get one graphics object.
 */
class AndroidGraphics {

    private Canvas canvas;
    private Paint paint;
    private Paint font;

    AndroidGraphics(AndroidImplementation impl, Canvas canvas) {
        this.canvas = canvas;
        this.paint = new Paint();
        paint.setAntiAlias(true);
        this.font = (Paint) ((AndroidImplementation.NativeFont)impl.getDefaultFont()).font;
        if(canvas != null) {
            canvas.save();
        }
    }

    Canvas getCanvas() {
        return canvas;
    }

    void setCanvas(Canvas canvas) {
        this.canvas = canvas;
        if(canvas != null) {
            canvas.save();
        }
    }

    Paint getFont() {
        return font;
    }

    void setFont(Paint font) {
        this.font = font;
        this.font.setColor(this.paint.getColor());
    }

    void setColor(int color){
        this.paint.setColor(color);
        this.font.setColor(color);
    }

    Paint getPaint() {
        return paint;
    }

}