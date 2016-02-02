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

package com.codename1.charts.util;

/**
 *
 * @author shannah
 */
public class ColorUtil {
    public static int LTGRAY = IColor.LightGray.argb;
    public static int BLUE = IColor.Blue.argb;
    public static int BLACK = IColor.Black.argb;
    public static int WHITE = IColor.White.argb;
    public static int CYAN = IColor.Cyan.argb;
    public static int GREEN = IColor.Green.argb;
    public static int YELLOW = IColor.Yellow.argb;
    public static int MAGENTA = IColor.Magenta.argb;
    public static int GRAY = IColor.Gray.argb;
    
    

    public static int argb(int a, int r, int g, int b) {
        IColor c = new IColor(a,r,g,b);
        return c.argb;
    }

    public static int alpha(int c) {
        IColor pc = new IColor(c);
        return pc.alpha;
    }

    public static int red(int c) {
        IColor pc = new IColor(c);
        return pc.red;
    }

    public static int green(int c) {
        IColor pc = new IColor(c);
        return pc.green;
    }

    public static int blue(int c) {
        IColor pc = new IColor(c);
        return pc.blue;
    }

    public static int rgb(int r, int g, int b) {
        IColor c = new IColor(r,g,b);
        return c.argb;
    }
    
    
    /*
 * Pisces User
 * Copyright (C) 2009 John Pritchard
 * Codename One Modifications Copyright (C) 2013 Steve Hannah
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License as
 * published by the Free Software Foundation; either version 2 of the
 * License, or (at your option) any later version.  The copyright
 * holders designate particular file as subject to the "Classpath"
 * exception as provided in the LICENSE file that accompanied this
 * code.
 * 
 * This program is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA
 * 02110-1301 USA.
 */


/**
 * 
 * 
 * @see Graphics
 */
private static class IColor
    extends Object
    implements Cloneable
{

    public final static IColor White     = new IColor(255, 255, 255);
    public final static IColor LightGray = new IColor(192, 192, 192);
    public final static IColor Gray      = new IColor(128, 128, 128);
    public final static IColor DarkGray  = new IColor( 64,  64,  64);
    public final static IColor Black     = new IColor(  0,   0,   0);
    public final static IColor Red       = new IColor(255,   0,   0);
    public final static IColor Pink      = new IColor(255, 175, 175);
    public final static IColor Orange    = new IColor(255, 200,   0);
    public final static IColor Yellow    = new IColor(255, 255,   0);
    public final static IColor Green     = new IColor(  0, 255,   0);
    public final static IColor Magenta   = new IColor(255,   0, 255);
    public final static IColor Cyan      = new IColor(  0, 255, 255);
    public final static IColor Blue      = new IColor(  0,   0, 255);


    public static class Transparent
        extends IColor
    {

        public final static Transparent White     = new Transparent(255, 255, 255);
        public final static Transparent LightGray = new Transparent(192, 192, 192);
        public final static Transparent Gray      = new Transparent(128, 128, 128);
        public final static Transparent DarkGray  = new Transparent( 64,  64,  64);
        public final static Transparent Black     = new Transparent(  0,   0,   0);
        public final static Transparent Red       = new Transparent(255,   0,   0);
        public final static Transparent Pink      = new Transparent(255, 175, 175);
        public final static Transparent Orange    = new Transparent(255, 200,   0);
        public final static Transparent Yellow    = new Transparent(255, 255,   0);
        public final static Transparent Green     = new Transparent(  0, 255,   0);
        public final static Transparent Magenta   = new Transparent(255,   0, 255);
        public final static Transparent Cyan      = new Transparent(  0, 255, 255);
        public final static Transparent Blue      = new Transparent(  0,   0, 255);


        public Transparent(int r, int g, int b){
            super(0,r,g,b);
        }
    }



    public final int alpha, red, green, blue;

    public final int argb;


    public IColor(int argb){
        super();

        int a = ((argb >>> 24) & 0xff);
        if (0 == a)
            this.alpha = 255;
        else
            this.alpha = a;

        this.red = (argb >>> 16) & 0xff;
        this.green = (argb >>> 8) & 0xff;
        this.blue = (argb & 0xff);

        this.argb = ToARGB(this);
    }
    public IColor(int r, int g, int b){
        this(0xff,r,g,b);
    }
    public IColor(int a, int r, int g, int b){
        super();
        this.alpha = (a & 0xff);
        this.red   = (r & 0xff);
        this.green = (g & 0xff);
        this.blue  = (b & 0xff);

        this.argb = ToARGB(this);
    }


    public IColor clone(){
        //try {
        //    return (IColor)super.clone();
        //}
        //catch (CloneNotSupportedException err){
            throw new RuntimeException();
        //}
    }
    public int hashCode(){
        return this.argb;
    }
    public boolean equals(Object that){
        if (this == that)
            return true;
        else if (null == that)
            return false;
        else if (that instanceof IColor)
            return (this.hashCode() == that.hashCode());
        else
            return false;
    }



    private final static int ToARGB(IColor c){
        return ((c.alpha<<24) | 
                (c.red<<16)|
                (c.green<<8)|
                (c.blue & 0xff));
    }
    
    public String toString(){
        return "{Red:"+this.red+" Green:"+this.green+" Blue:"+this.blue+" Alpha:"+this.alpha+"}";
    }

}

}
