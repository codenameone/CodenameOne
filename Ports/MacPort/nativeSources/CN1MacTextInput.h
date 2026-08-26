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
/*
 * Reporting one edit to the pure Codename One editor. Each is a no-op on the
 * legacy path, whose edits travel instead as a whole-document stringEdit when
 * the session commits -- so the rendering view calls these unconditionally and
 * the session decides.
 */
void CN1MacTextInputNotifyReplace(NSRange range, NSString *text);
void CN1MacTextInputNotifySelection(NSRange sel);
void CN1MacTextInputNotifyComposing(NSString *text, NSInteger rel);
void CN1MacTextInputNotifyFinishComposing(void);
void CN1MacTextInputNotifyEditorAction(void);

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

/// YES when the pure Codename One editor engine drives this session -- an
/// EditField, CodeEditor or RichTextArea, started through startTextInput -- and
/// NO for the legacy TextField/TextArea path started through editStringAt.
///
/// The two report back to entirely different Java callbacks: the legacy one to
/// stringEdit/editingUpdate, which only ever updates currentEditing, and the
/// pure one to the ti* family bound to the TextInputClient. Sending a pure
/// editor's edits through stringEdit reaches a null currentEditing and is
/// silently dropped, which is a text field that types nothing.
@property (nonatomic) BOOL pureEditor;

/// Monotonic edit generation. Every edit reported to the pure editor carries
/// one, and Java echoes the last generation it APPLIED back with each state
/// push, so a slow round trip cannot regress the shadow under fast typing.
- (int)nextEditSeq;

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
