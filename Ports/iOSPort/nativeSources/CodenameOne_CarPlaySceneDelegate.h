/*
 * Copyright (c) 2026, Codename One and/or its affiliates. All rights reserved.
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

// Apple CarPlay support for Codename One. This whole unit is compiled only when
// CN1_USE_CARPLAY is defined -- the build flips that on (and links
// CarPlay.framework + adds the CarPlay entitlement) when the app references
// com.codename1.car. Builds without it never import CarPlay.framework.
// CodenameOne_GLViewController.h carries the CN1_USE_CARPLAY define; import it
// first (before the guard) so the define is visible here, mirroring CN1Camera.h.
#import "CodenameOne_GLViewController.h"
#ifdef CN1_USE_CARPLAY
#import <Foundation/Foundation.h>
#import <CarPlay/CarPlay.h>

// Singleton that owns the CarPlay CPInterfaceController, the registered image
// table and the JSON -> CPTemplate translation. The C trampolines in IOSNative.m
// forward the IOSNative.carPlay* native calls here; selections are reported back
// to Java via IOSCarPlayCallbacks.
API_AVAILABLE(ios(14.0))
@interface CN1CarPlayManager : NSObject

@property (nonatomic, assign) BOOL connected;
@property (nonatomic, strong) CPInterfaceController *interfaceController;

+ (instancetype)sharedManager;

- (void)didConnect:(CPInterfaceController *)controller;
- (void)didDisconnect;

- (void)setTemplate:(int)screenId json:(NSString *)json isRoot:(BOOL)isRoot;
- (void)updateTemplate:(int)screenId json:(NSString *)json;
- (void)popTemplate;
- (void)registerImage:(NSString *)key data:(NSData *)data;
- (void)showToast:(NSString *)message seconds:(int)seconds;

@end

// CPTemplateApplicationSceneDelegate referenced from the Info.plist scene manifest
// the builder injects. iOS instantiates it for the CarPlay scene.
API_AVAILABLE(ios(14.0))
@interface CodenameOne_CarPlaySceneDelegate : UIResponder <CPTemplateApplicationSceneDelegate>
@end

#endif // CN1_USE_CARPLAY
