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
#import <TargetConditionals.h>
#if TARGET_OS_OSX

#import <AppKit/AppKit.h>
#import "CN1MacHost.h"
#import "CN1MacShare.h"
#import "cn1_globals.h"

extern void com_codename1_impl_ios_IOSImplementation_socialShareCallback___int_int_java_lang_String_java_lang_String(
        CODENAME_ONE_THREAD_STATE, JAVA_INT callbackId, JAVA_INT status, JAVA_OBJECT target, JAVA_OBJECT message);

/// Carries a share through to its real outcome.
///
/// NSSharingServicePicker reports what the user did in two steps, and neither is
/// a completion handler: the picker's delegate learns which service was chosen
/// (or that the sheet was dismissed), and the SERVICE's delegate learns whether
/// the share then succeeded or failed. Reporting success when the sheet opens --
/// which is all the picker itself tells you -- means a dismissed sheet is
/// indistinguishable from a completed share.
///
/// One of these lives per share, retains itself and the picker for as long as
/// AppKit may still call back, and releases both once it has reported. Reporting
/// is one-shot: the picker delegate fires again on dismissal after a service was
/// already chosen.
@interface CN1MacShareDelegate : NSObject <NSSharingServicePickerDelegate, NSSharingServiceDelegate>
@property (nonatomic, assign) int callbackId;
@property (nonatomic, retain) NSSharingServicePicker *picker;
@property (nonatomic, assign) BOOL reported;
@end

@implementation CN1MacShareDelegate

- (void)reportStatus:(int)status target:(NSString *)target message:(NSString *)message {
    if (self.reported) {
        return;
    }
    self.reported = YES;
    struct ThreadLocalData *threadStateData = getThreadLocalData();
    JAVA_OBJECT jTarget = target != nil ? fromNSString(threadStateData, target) : JAVA_NULL;
    JAVA_OBJECT jMessage = message != nil ? fromNSString(threadStateData, message) : JAVA_NULL;
    com_codename1_impl_ios_IOSImplementation_socialShareCallback___int_int_java_lang_String_java_lang_String(
            threadStateData, (JAVA_INT)self.callbackId, (JAVA_INT)status, jTarget, jMessage);
    self.picker.delegate = nil;
    self.picker = nil;
    [self autorelease];
}

- (void)sharingServicePicker:(NSSharingServicePicker *)picker
     didChooseSharingService:(NSSharingService *)service {
    if (service == nil) {
        // The sheet closed without a choice. DISMISSED, not a failure.
        [self reportStatus:2 target:nil message:nil];
        return;
    }
    // Held so the service can report the outcome of the share it is about to
    // perform; the picker's own job is finished here.
    service.delegate = self;
}

- (void)sharingService:(NSSharingService *)service didShareItems:(NSArray *)items {
    // Unset before reporting, which is what releases this object: the service's
    // delegate is unretained, and AppKit outlives the share.
    service.delegate = nil;
    [self reportStatus:1 target:service.title message:nil];
}

- (void)sharingService:(NSSharingService *)service
   didFailToShareItems:(NSArray *)items
                 error:(NSError *)error {
    // Cancelling inside the service's own sheet is a dismissal rather than a
    // failure, and it is the common case -- opening Mail and closing it again.
    service.delegate = nil;
    if ([error.domain isEqualToString:NSCocoaErrorDomain] && error.code == NSUserCancelledError) {
        [self reportStatus:2 target:service.title message:nil];
        return;
    }
    [self reportStatus:3 target:service.title message:error.localizedDescription];
}

@end

void CN1MacPresentSharePicker(NSArray *items, BOOL useRect, CGRect rectInPixels, int callbackId) {
    if (items.count == 0) {
        if (callbackId > 0) {
            struct ThreadLocalData *threadStateData = getThreadLocalData();
            com_codename1_impl_ios_IOSImplementation_socialShareCallback___int_int_java_lang_String_java_lang_String(
                    threadStateData, (JAVA_INT)callbackId, 2, JAVA_NULL, JAVA_NULL);
        }
        return;
    }
    // No pending owner is named here, unlike showNativePicker(). rectInPixels is
    // relative to the source component's window, so anchoring in the key window is
    // wrong for a share opened programmatically for a component in a visible
    // non-key window -- but nothing on this side can say which window that is.
    // CodenameOneImplementation.share() receives the rectangle and nothing else,
    // and every window the port could name from here (the key window,
    // Desktop.getFocusedWindow()) is the one already resolved below, so naming it
    // would constrain nothing. Fixing it means the share SPI carrying its source
    // component the way showNativePicker(int, Component, ...) does.
    NSView *host = CN1MacKeyRenderingHostView();
    CGFloat scale = CN1MacHostViewScale(host);
    NSRect anchor;
    if (useRect) {
        anchor = NSMakeRect(rectInPixels.origin.x / scale, rectInPixels.origin.y / scale,
                            MAX(rectInPixels.size.width / scale, 1),
                            MAX(rectInPixels.size.height / scale, 1));
    } else {
        anchor = NSMakeRect(host.bounds.size.width / 2, host.bounds.size.height / 2, 1, 1);
    }
    NSSharingServicePicker *picker = [[NSSharingServicePicker alloc] initWithItems:items];
    if (callbackId > 0) {
        CN1MacShareDelegate *delegate = [[CN1MacShareDelegate alloc] init];
        delegate.callbackId = callbackId;
        // The delegate owns the picker and itself until it reports. Showing the
        // sheet is asynchronous, so releasing the picker here -- which is what a
        // caller with no callback does, and what AppKit tolerates because it
        // keeps the sheet alive itself -- would leave the delegate holding a
        // reference to nothing to unset.
        delegate.picker = picker;
        picker.delegate = delegate;
    }
    [picker showRelativeToRect:anchor ofView:host preferredEdge:NSMinYEdge];
    [picker release];
}

#endif
