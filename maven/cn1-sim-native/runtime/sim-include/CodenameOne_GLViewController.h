/*
 * Copyright (c) 2012, Codename One and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
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

/*
 * Simulator shim for the device CodenameOne_GLViewController.h. The real
 * header drags in the whole iOS framework surface (UIKit views, MessageUI,
 * StoreKit, social SDKs) which cannot parse in an AppKit JVM process. Port
 * sources compiled for the simulator (the ExecutableOp files) resolve their
 * quote-include here instead - the build copies them into a scratch
 * directory so the include search reaches sim-include before nativeSources.
 *
 * The class itself is the simulator's NSObject-based reimplementation in
 * CN1SimViewController.m: same name, same op-queue selectors, Metal-only
 * frame drain into a CAMetalLayer provided by JAWT.
 */
#ifndef CN1SIM_GLVIEWCONTROLLER_SHIM_H
#define CN1SIM_GLVIEWCONTROLLER_SHIM_H

#import <Foundation/Foundation.h>
#import <QuartzCore/CAMetalLayer.h>
/* JAVA_INT etc. - the real header pulls these via xmlvm.h */
#include "cn1_globals.h"

@class ExecutableOp;
@class UIImage;

/* display scale published by the simulator host (1.0 unless HiDPI) */
extern float scaleValue;

#define CN1Log(str, ...) NSLog(str, ##__VA_ARGS__)

#ifdef CN1_USE_ARC
#define POOL_BEGIN()
#define POOL_END()
#define BRIDGE_CAST __bridge
#else
#define POOL_BEGIN() NSAutoreleasePool *pool = [[NSAutoreleasePool alloc] init];
#define POOL_END() [pool release];
#define BRIDGE_CAST
#endif

@interface CodenameOne_GLViewController : NSObject

+ (CodenameOne_GLViewController *)instance;

/* op queue management (same contract as the device controller) */
- (void)upcomingAdd:(ExecutableOp *)op;
- (void)upcomingAddClip:(ExecutableOp *)op;

/* queue swap + Metal frame drain + present */
- (void)flushBuffer:(UIImage *)buff x:(int)x y:(int)y width:(int)width height:(int)height;

/* simulator host integration */
- (void)attachLayer:(CALayer *)layer width:(int)w height:(int)h;
- (void)resizeSurface:(int)w height:(int)h;

@end

#endif /* CN1SIM_GLVIEWCONTROLLER_SHIM_H */
