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
#import "CN1MacTextInput.h"
#import "CN1MacHost.h"
#include "cn1_globals.h"
#include "java_lang_String.h"

// Defined in the shared IOSNative.m: hands a text edit back to
// IOSImplementation.editingUpdate, which is where every Apple port's editing
// round trip lands.
extern void stringEdit(int finished, int cursorPos, NSString *text);

/*
 * The pure-editor callbacks. Declared here rather than pulled from a header
 * because ParparVM names them by their full Java signature and there is no
 * header for generated symbols; IOSImplementation.tiKeepNativeCallbacksAlive
 * already holds a reachable reference to every one of them, which is what stops
 * the dead-code pass dropping a method only C calls.
 */
extern void com_codename1_impl_ios_IOSImplementation_tiReplaceRange___int_int_java_lang_String_int(
        CODENAME_ONE_THREAD_STATE, JAVA_INT start, JAVA_INT end, JAVA_OBJECT text, JAVA_INT seq);
extern void com_codename1_impl_ios_IOSImplementation_tiSetSelection___int_int_int(
        CODENAME_ONE_THREAD_STATE, JAVA_INT start, JAVA_INT end, JAVA_INT seq);
extern void com_codename1_impl_ios_IOSImplementation_tiSetComposing___java_lang_String_int_int(
        CODENAME_ONE_THREAD_STATE, JAVA_OBJECT text, JAVA_INT rel, JAVA_INT seq);
extern void com_codename1_impl_ios_IOSImplementation_tiFinishComposing___int(
        CODENAME_ONE_THREAD_STATE, JAVA_INT seq);
extern void com_codename1_impl_ios_IOSImplementation_tiEditorAction___int(
        CODENAME_ONE_THREAD_STATE, JAVA_INT action);

/// Reports one replacement to the pure editor. No-op for the legacy path, whose
/// edits travel as a whole-document stringEdit instead.
void CN1MacTextInputNotifyReplace(NSRange range, NSString *text) {
    CN1MacTextInputSession *session = [CN1MacTextInputSession sharedSession];
    if (!session.pureEditor) {
        return;
    }
    struct ThreadLocalData *threadStateData = getThreadLocalData();
    com_codename1_impl_ios_IOSImplementation_tiReplaceRange___int_int_java_lang_String_int(
            threadStateData, (JAVA_INT)range.location, (JAVA_INT)NSMaxRange(range),
            fromNSString(threadStateData, text != nil ? text : @""),
            (JAVA_INT)[session nextEditSeq]);
}

void CN1MacTextInputNotifySelection(NSRange sel) {
    CN1MacTextInputSession *session = [CN1MacTextInputSession sharedSession];
    if (!session.pureEditor) {
        return;
    }
    com_codename1_impl_ios_IOSImplementation_tiSetSelection___int_int_int(
            CN1_THREAD_GET_STATE_PASS_ARG (JAVA_INT)sel.location, (JAVA_INT)NSMaxRange(sel),
            (JAVA_INT)[session nextEditSeq]);
}

void CN1MacTextInputNotifyComposing(NSString *text, NSInteger rel) {
    CN1MacTextInputSession *session = [CN1MacTextInputSession sharedSession];
    if (!session.pureEditor) {
        return;
    }
    struct ThreadLocalData *threadStateData = getThreadLocalData();
    com_codename1_impl_ios_IOSImplementation_tiSetComposing___java_lang_String_int_int(
            threadStateData, fromNSString(threadStateData, text != nil ? text : @""),
            (JAVA_INT)rel, (JAVA_INT)[session nextEditSeq]);
}

void CN1MacTextInputNotifyFinishComposing(void) {
    CN1MacTextInputSession *session = [CN1MacTextInputSession sharedSession];
    if (!session.pureEditor) {
        return;
    }
    com_codename1_impl_ios_IOSImplementation_tiFinishComposing___int(
            CN1_THREAD_GET_STATE_PASS_ARG (JAVA_INT)[session nextEditSeq]);
}

/// The Return / action key, which is how a single-line pure editor learns the
/// user is done. tiEditorAction rather than tiKeyCommand: the former is the
/// editor-action channel, the latter is for caret and editing key commands.
void CN1MacTextInputNotifyEditorAction(void) {
    com_codename1_impl_ios_IOSImplementation_tiEditorAction___int(
            CN1_THREAD_GET_STATE_PASS_ARG 0);
}

@implementation CN1MacTextInputSession {
    int editSeq;
}

+ (instancetype)sharedSession {
    static CN1MacTextInputSession *shared = nil;
    static dispatch_once_t once;
    dispatch_once(&once, ^{
        shared = [[CN1MacTextInputSession alloc] init];
    });
    return shared;
}

- (instancetype)init {
    self = [super init];
    if (self != nil) {
        _text = @"";
        _selectedRange = NSMakeRange(0, 0);
        _markedRange = NSMakeRange(NSNotFound, 0);
        _caretRect = CGRectZero;
        _editorBounds = CGRectZero;
    }
    return self;
}

- (void)startWithText:(NSString *)initialText
             selStart:(NSInteger)selStart
               selEnd:(NSInteger)selEnd
            multiline:(BOOL)multiline {
    self.text = initialText != nil ? initialText : @"";
    NSInteger len = (NSInteger)self.text.length;
    // The framework can hand over a selection recorded against text it has since
    // changed, so clamp rather than trust it -- an out of range NSRange is a
    // crash, not a wrong caret.
    selStart = MIN(MAX(selStart, 0), len);
    selEnd = MIN(MAX(selEnd, selStart), len);
    self.selectedRange = NSMakeRange(selStart, selEnd - selStart);
    self.markedRange = NSMakeRange(NSNotFound, 0);
    self.multiline = multiline;
    _active = YES;

    dispatch_async(dispatch_get_main_queue(), ^{
        // The window the editing started in, not always the main one. Editing a
        // component in a secondary NSWindow used to make the MAIN host's view
        // first responder, which took key focus away from the window the user
        // was typing in and routed every subsequent keystroke to the wrong one.
        //
        // Chosen by conformance rather than by class: the thing that has to
        // become first responder is whatever can receive text input, and asking
        // that question directly needs no import of the view's header.
        NSView *view = nil;
        NSWindow *key = [NSApp keyWindow];
        if (key != nil
                && [key.contentView conformsToProtocol:@protocol(NSTextInputClient)]) {
            view = key.contentView;
        }
        if (view == nil) {
            view = [CN1MacHost sharedHost].renderingView;
        }
        [view.window makeFirstResponder:view];
    });
}

- (void)stop {
    if (!_active) {
        return;
    }
    _active = NO;
    self.markedRange = NSMakeRange(NSNotFound, 0);
    dispatch_async(dispatch_get_main_queue(), ^{
        [[NSTextInputContext currentInputContext] discardMarkedText];
    });
}

- (int)nextEditSeq {
    return ++editSeq;
}

- (void)commitFinished:(BOOL)finished {
    if (self.pureEditor) {
        // The pure editor has already been told about the edit itself, operation
        // by operation, through the ti* callbacks. stringEdit would reach
        // editingUpdate, which only ever touches currentEditing -- null on this
        // path -- so it is not merely redundant here, it is the whole reason a
        // pure editor typed nothing.
        //
        // "finished" still has to travel: it is how a single-line editor learns
        // the user pressed Return.
        if (finished) {
            CN1MacTextInputNotifyEditorAction();
        }
        return;
    }
    stringEdit(finished ? 1 : 0, (int)NSMaxRange(self.selectedRange), self.text);
}

@end

JAVA_VOID com_codename1_impl_ios_IOSNative_setTextInputBounds___int_int_int_int(CODENAME_ONE_THREAD_STATE, JAVA_OBJECT __cn1ThisObject, JAVA_INT x, JAVA_INT y, JAVA_INT w, JAVA_INT h) {
    CN1MacTextInputSession *session = [CN1MacTextInputSession sharedSession];
    session.editorBounds = CGRectMake(x, y, w, h);
}

JAVA_VOID com_codename1_impl_ios_IOSNative_startTextInput___int_boolean_boolean_boolean_java_lang_String_int_int_int(CODENAME_ONE_THREAD_STATE, JAVA_OBJECT __cn1ThisObject, JAVA_INT constraint, JAVA_BOOLEAN autoCorrect, JAVA_BOOLEAN autoCapitalize, JAVA_BOOLEAN multiline, JAVA_OBJECT initialText, JAVA_INT selStart, JAVA_INT selEnd, JAVA_INT actionType) {
    // autoCorrect / autoCapitalize / constraint / actionType have no AppKit
    // equivalent for a custom drawn surface: correction and capitalization are a
    // system wide input source behaviour rather than a per field one, and there
    // is no return key to relabel. They are accepted and ignored so the shared
    // Java side needs no macOS special case.
    NSString *initial = initialText != JAVA_NULL
        ? toNSString(CN1_THREAD_GET_STATE_PASS_ARG initialText)
        : @"";
    // The pure editor engine's entry point. editStringAt, below, is the legacy
    // TextField/TextArea one, and the two report to different Java callbacks.
    [CN1MacTextInputSession sharedSession].pureEditor = YES;
    // No limit: the pure editor owns its own document and enforces whatever
    // limit it has there.
    [CN1MacTextInputSession sharedSession].maxSize = 0;
    [[CN1MacTextInputSession sharedSession] startWithText:initial
                                                selStart:selStart
                                                  selEnd:selEnd
                                               multiline:multiline != 0];
}

JAVA_VOID com_codename1_impl_ios_IOSNative_stopTextInput__(CODENAME_ONE_THREAD_STATE, JAVA_OBJECT __cn1ThisObject) {
    [[CN1MacTextInputSession sharedSession] stop];
}

JAVA_VOID com_codename1_impl_ios_IOSNative_updateTextInputState___java_lang_String_int_int_int_int_int_int_int(CODENAME_ONE_THREAD_STATE, JAVA_OBJECT __cn1ThisObject, JAVA_OBJECT text, JAVA_INT selStart, JAVA_INT selEnd, JAVA_INT caretX, JAVA_INT caretY, JAVA_INT caretW, JAVA_INT caretH, JAVA_INT seq) {
    CN1MacTextInputSession *session = [CN1MacTextInputSession sharedSession];
    session.caretRect = CGRectMake(caretX, caretY, caretW, caretH);
    if (session.markedRange.location != NSNotFound) {
        // Mid composition the input method owns the text. Taking the
        // framework's copy back here would drop the marked run and make the
        // composition look like it did nothing.
        return;
    }
    if (text != JAVA_NULL) {
        session.text = toNSString(CN1_THREAD_GET_STATE_PASS_ARG text);
    }
    NSInteger len = (NSInteger)session.text.length;
    NSInteger start = MIN(MAX((NSInteger)selStart, 0), len);
    NSInteger end = MIN(MAX((NSInteger)selEnd, start), len);
    session.selectedRange = NSMakeRange(start, end - start);
}

/// Pushes the framework's text into the live session without disturbing the
/// caret any more than it has to. Called from the shared
/// updateNativeEditorText native, which on the UIKit ports writes into a
/// UITextView standing beside the rendering surface; here the surface is the
/// editor, so there is nowhere else for the text to go.
void CN1MacTextInputSetText(NSString *text) {
    CN1MacTextInputSession *session = [CN1MacTextInputSession sharedSession];
    if (!session.active || text == nil) {
        return;
    }
    if (session.markedRange.location != NSNotFound) {
        // Mid composition the input method owns the text; see the matching
        // guard in updateTextInputState.
        return;
    }
    if ([text isEqualToString:session.text]) {
        return;
    }
    NSUInteger caret = MIN(NSMaxRange(session.selectedRange), text.length);
    session.text = text;
    session.selectedRange = NSMakeRange(caret, 0);
}

/// Starts an editing session for the legacy TextField / TextArea path.
///
/// Called from the shared editStringAt native. Unlike startTextInput, which the
/// pure editor engine drives with an explicit selection, this one is handed only
/// the text -- so the caret goes to the end, which is where a Mac puts it when a
/// field takes focus by being clicked into rather than tabbed into.
void CN1MacTextInputBegin(NSString *text, BOOL multiline, CGRect bounds, int maxSize) {
    CN1MacTextInputSession *session = [CN1MacTextInputSession sharedSession];
    NSString *initial = text != nil ? text : @"";
    session.pureEditor = NO;
    // Carried into the session because nothing downstream enforces it: an
    // over-long value reaches TextArea.setText(), which raises maxSize to fit
    // rather than refusing, so the configured limit is gone for good.
    session.maxSize = maxSize > 0 ? (NSUInteger)maxSize : 0;
    session.editorBounds = bounds;
    // A caret rectangle is needed before the first updateTextInputState arrives,
    // or an input method opened on the first keystroke has nowhere to put its
    // candidate window. The editor's own top left is the honest guess.
    session.caretRect = CGRectMake(bounds.origin.x, bounds.origin.y, 1, bounds.size.height);
    [session startWithText:initial
                  selStart:(NSInteger)initial.length
                    selEnd:(NSInteger)initial.length
                 multiline:multiline];
}
