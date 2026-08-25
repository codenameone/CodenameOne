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
#import <AppKit/AppKit.h>

/// The state of one native editing session.
///
/// Codename One's editing model is that Java owns the text and the native editor
/// is a keyboard front end for it: the framework pushes authoritative state in
/// through updateTextInputState and the editor pushes user edits back out
/// through stringEdit. AppKit's NSTextInputClient is a protocol on a view rather
/// than a control, so the session lives here and the rendering view forwards to
/// it -- which also keeps input-method state out of the renderer.
@interface CN1MacTextInputSession : NSObject

+ (instancetype)sharedSession;

/// YES between startTextInput and stopTextInput. While NO the view treats keys
/// as ordinary game/shortcut keys rather than text.
@property (nonatomic, readonly) BOOL active;

/// The text being edited, including any uncommitted marked text.
@property (nonatomic, copy) NSString *text;

@property (nonatomic) NSRange selectedRange;
@property (nonatomic) NSRange markedRange;

/// Whether the session accepts a newline as text rather than as "done".
@property (nonatomic) BOOL multiline;

/// Caret rectangle in Codename One pixels, as last reported by the framework.
@property (nonatomic) CGRect caretRect;

/// Editor bounds in Codename One pixels.
@property (nonatomic) CGRect editorBounds;

- (void)startWithText:(NSString *)initialText
             selStart:(NSInteger)selStart
               selEnd:(NSInteger)selEnd
            multiline:(BOOL)multiline;
- (void)stop;

/// Pushes the current text and caret back to the framework. Pass YES for
/// finished to end the edit rather than update it.
- (void)commitFinished:(BOOL)finished;

@end
