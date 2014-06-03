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
#import <Foundation/Foundation.h>

extern void logGlErrorAt(const char *f, int l);
extern int nextPowerOf2(int val);

#define GLErrorLog logGlErrorAt(__FILE__, __LINE__)

#define GlColorFromRGB(rgbValue,alphaColor) _glColor4f(((float)((rgbValue >> 16) & 0xff))/255.0, \
((float)((rgbValue >> 8) & 0xff))/255.0, ((float)(rgbValue & 0xff))/255.0, ((float)alphaColor)/255.0); GLErrorLog;

#define GlColorFromARGB(rgbValue) _glColor4f(((float)((rgbValue >> 16) & 0xff))/255.0, \
((float)((rgbValue >> 8) & 0xff))/255.0, ((float)(rgbValue & 0xff))/255.0, (((rgbValue >> 24) & 0xff) /255.0)); GLErrorLog;


#define UIColorFromRGB(rgbValue,alphaColor) [UIColor colorWithRed:((float)((rgbValue >> 16) & 0xFF))/255.0 \
green:((float)((rgbValue >> 8) & 0xff))/255.0 blue:((float)(rgbValue & 0xff))/255.0 alpha:alphaColor/255.0]

#define CGColorFromRGB(context,rgbValue,alphaColor) CGContextSetRGBStrokeColor(context, ((float)((rgbValue >> 16) & 0xFF))/255.0, ((float)((rgbValue >> 8) & 0xff))/255.0, ((float)(rgbValue & 0xff))/255.0, alphaColor/255.0);

#define UIColorFromARGB(rgbValue) [UIColor colorWithRed:((float)((rgbValue >> 16) & 0xFF))/255.0 \
green:((float)((rgbValue >> 8) & 0xff))/255.0 blue:((float)(rgbValue & 0xff))/255.0 alpha:(((rgbValue >> 24) & 0xff) /255.0)]

@interface ExecutableOp : NSObject {

}

+(natural_t) get_free_memory;
-(void)clipBlock:(BOOL)b;
-(void)executeWithClipping;
-(void)execute;
-(void)executeWithLog;
-(NSString*)getName;

@end
