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
#import <UIKit/UIKit.h>

#if !TARGET_OS_WATCH

/// A transparent first responder view that binds the platform soft keyboard, hardware / bluetooth
/// keyboard and IME to a Codename One pure editor through the low level text input source. It conforms
/// to the full UITextInput protocol so iOS drives edits by range (typed text, dictation, marked text /
/// CJK composition, hardware keys) and can position the IME candidate window and selection loupe using
/// the caret geometry Codename One reports. The view keeps a shadow mirror of the document text and
/// selection (pushed down from Java) purely to answer UITextInput's text and position queries; Codename
/// One renders the document itself and remains authoritative.
@interface CN1TextInputView : UIView <UITextInput>

@property (nonatomic) UITextAutocorrectionType autocorrectionType;
@property (nonatomic) UITextAutocapitalizationType autocapitalizationType;
@property (nonatomic) UIKeyboardType keyboardType;
@property (nonatomic) UIReturnKeyType returnKeyType;
@property (nonatomic) UITextSpellCheckingType spellCheckingType;
@property (nonatomic, getter=isSecureTextEntry) BOOL secureTextEntry;
@property (nonatomic) BOOL multiline;
/// The TextInputConfig.ACTION_* code delivered through tiEditorAction when Return is pressed on a
/// single line field.
@property (nonatomic) int actionType;

/// Replaces the shadow mirror with the authoritative Java state (text + selection + caret rect in
/// screen pixels) without echoing the change back to Java.
- (void)cn1SyncText:(NSString *)text selStart:(NSInteger)selStart selEnd:(NSInteger)selEnd
          caretRect:(CGRect)caretPixels;

/// Clears any leftover IME composition state; called when a session starts or stops so a stale marked
/// range from a previous session can never block Java-state syncing.
- (void)cn1ResetComposition;

@end

#endif
