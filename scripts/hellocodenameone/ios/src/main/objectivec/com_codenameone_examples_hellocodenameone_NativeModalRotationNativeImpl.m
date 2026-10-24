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
#import "com_codenameone_examples_hellocodenameone_NativeModalRotationNativeImpl.h"

#if TARGET_OS_IOS && !TARGET_OS_MACCATALYST
#import "CodenameOne_GLViewController.h"
#import <UIKit/UIKit.h>

extern float scaleValue;

// Stands in for UIImagePickerController's camera, which keeps its interface
// portrait however the phone is held (issue #6080).
@interface CN1PortraitOnlyModalController : UIViewController
@end

@implementation CN1PortraitOnlyModalController
- (UIInterfaceOrientationMask)supportedInterfaceOrientations {
    return UIInterfaceOrientationMaskPortrait;
}
- (BOOL)shouldAutorotate {
    return NO;
}
- (UIInterfaceOrientation)preferredInterfaceOrientationForPresentation {
    return UIInterfaceOrientationPortrait;
}
@end

// Written on the main thread only, read from the EDT. A stale read is one poll
// behind, which the test's polling absorbs.
static UIViewController* modalController = nil;
static int modalPresented = 0;
static int viewPixelWidth = 0;
static int viewPixelHeight = 0;

static void snapshotState() {
    dispatch_async(dispatch_get_main_queue(), ^{
        UIView* v = [CodenameOne_GLViewController instance].view;
        viewPixelWidth = (int)(v.bounds.size.width * scaleValue);
        viewPixelHeight = (int)(v.bounds.size.height * scaleValue);
        modalPresented = (modalController != nil && modalController.presentingViewController != nil) ? 1 : 0;
    });
}

@implementation com_codenameone_examples_hellocodenameone_NativeModalRotationNativeImpl

-(BOOL)presentPortraitOnlyModal {
    dispatch_async(dispatch_get_main_queue(), ^{
        if (modalController != nil) {
            return;
        }
        CN1PortraitOnlyModalController* c = [[CN1PortraitOnlyModalController alloc] init];
        c.view.backgroundColor = [UIColor blackColor];
        c.modalPresentationStyle = UIModalPresentationFullScreen;
        modalController = c;
        [[CodenameOne_GLViewController instance] presentViewController:c animated:NO completion:nil];
    });
    snapshotState();
    return YES;
}

-(BOOL)dismissModal {
    dispatch_async(dispatch_get_main_queue(), ^{
        UIViewController* c = modalController;
        if (c == nil) {
            return;
        }
        [c dismissViewControllerAnimated:YES completion:^{
            if (modalController == c) {
#ifndef CN1_USE_ARC
                [modalController release];
#endif
                modalController = nil;
            }
        }];
    });
    snapshotState();
    return YES;
}

-(BOOL)isModalPresented {
    snapshotState();
    return modalPresented != 0;
}

-(int)getViewPixelWidth {
    snapshotState();
    return viewPixelWidth;
}

-(int)getViewPixelHeight {
    snapshotState();
    return viewPixelHeight;
}

-(BOOL)isSupported {
    return YES;
}

@end

#else

// The watch, tv and Catalyst slices compile this file too; none of them has
// interface orientation to rotate under a modal.
@implementation com_codenameone_examples_hellocodenameone_NativeModalRotationNativeImpl

-(BOOL)presentPortraitOnlyModal {
    return NO;
}

-(BOOL)dismissModal {
    return NO;
}

-(BOOL)isModalPresented {
    return NO;
}

-(int)getViewPixelWidth {
    return 0;
}

-(int)getViewPixelHeight {
    return 0;
}

-(BOOL)isSupported {
    return NO;
}

@end

#endif
