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
#include <assert.h>

#define JAVA_VOID void
#define JAVA_INT int
#define JAVA_BOOLEAN int
#define JAVA_TRUE 1
#define JAVA_FALSE 0
#define JAVA_OBJECT id
#define JAVA_NULL nil
#define CODENAME_ONE_THREAD_STATE void *thread
#define CN1_THREAD_GET_STATE_PASS_ARG NULL,
struct ThreadLocalData { int unused; };
static struct ThreadLocalData *getThreadLocalData(void) { return NULL; }
static NSString *toNSString(void *thread, id text) { return text; }
static NSMutableArray *mainQueue;
static void enqueue(dispatch_block_t block) {
    dispatch_block_t saved = [block copy];
    [mainQueue addObject:saved];
    [saved release];
}
#define dispatch_async(queue, block) enqueue(block)

@interface CN1MacTextInputSession : NSObject {
    int editSeq, sessionGeneration, pendingEditorCommand, deferredEditGeneration;
    NSMutableArray *deferredEdits;
}
@property BOOL active, pureEditor, multiline;
@property NSUInteger maxSize, selectionAnchor;
@property(copy) NSString *text;
@property NSRange selectedRange, markedRange;
@property CGRect caretRect;
+ (instancetype)sharedSession;
- (int)nextEditSeq;
- (int)currentEditSeq;
- (int)currentSessionGeneration;
- (void)reset:(NSString *)text;
- (void)commitFinished:(BOOL)finished;
- (BOOL)deferNativeEdit:(void (^)(void))edit;
- (int)beginEditorCommand;
- (void)completeEditorCommand:(int)sequence;
- (void)resetDeferredEditsIfNeeded;
@end
@implementation CN1MacTextInputSession
+ (instancetype)sharedSession { static id value; if (!value) value = [self new]; return value; }
- (int)nextEditSeq { return ++editSeq; }
- (int)currentEditSeq { return editSeq; }
- (int)currentSessionGeneration { return sessionGeneration; }
- (void)reset:(NSString *)text {
    sessionGeneration++;
    self.active = YES; self.pureEditor = YES; self.multiline = YES;
    self.text = text; self.selectedRange = NSMakeRange(1, 2);
    self.markedRange = NSMakeRange(NSNotFound, 0);
    self.selectionAnchor = self.selectedRange.location;
    [self resetDeferredEditsIfNeeded];
}
- (void)commitFinished:(BOOL)finished { }
#include "queue.h"
@end

static int command, commandSequence, commandCount, replacements;
static NSRange replacedRange;
static BOOL cn1OwnsSession(id view, CN1MacTextInputSession *session) { return session.active; }
static BOOL cn1CutBlocked(id view, CN1MacTextInputSession *session) { return NO; }
static BOOL cn1PasteBlocked(id view, CN1MacTextInputSession *session) { return NO; }
static void CN1MacTextInputNotifyReplace(NSRange range, NSString *text) {
    replacedRange = range; replacements++;
    [[CN1MacTextInputSession sharedSession] nextEditSeq];
}
static void CN1MacTextInputNotifyFinishComposing(void) { }
void com_codename1_impl_ios_IOSImplementation_tiKeyCommandWithSequence___int_int_int(
        void *thread, int cmd, int modifiers, int sequence) {
    command = cmd; commandSequence = sequence; commandCount++;
}
static int reportedAnchor, reportedFocus;
void com_codename1_impl_ios_IOSImplementation_tiSetSelection___int_int_int(
        void *thread, int anchor, int focus, int sequence) {
    reportedAnchor = anchor; reportedFocus = focus;
}
void com_codename1_impl_ios_IOSImplementation_tiPublishGeometry___int_boolean(
        void *thread, int captureId, int accepted) { }
void com_codename1_impl_mac_MacImplementation_macTraverseTextEditing___boolean(
        void *thread, int forward) { }
#include "echo.h"

@interface TestView : NSResponder {
    NSUInteger selectionAnchor;
    BOOL selectionAnchorValid;
}
- (BOOL)cn1DeferNativeEdit:(void (^)(void))edit;
- (void)cn1SendEditorKeyCommand:(int)command modifiers:(int)modifiers;
- (void)cn1SendEditorKeyCommand:(int)command;
- (void)cn1ForgetAnchor;
- (NSRange)cn1SelectionForEditing;
- (void)copy:(id)sender;
- (void)delete:(id)sender;
- (void)cut:(id)sender;
- (void)paste:(id)sender;
- (void)insertText:(id)string replacementRange:(NSRange)range;
@end
@implementation TestView
- (NSRange)cn1SelectionForEditing { return [CN1MacTextInputSession sharedSession].selectedRange; }
- (void)copy:(id)sender { }
- (void)delete:(id)sender { }
#include "view.h"
@end

static void drain(void) {
    while (mainQueue.count) {
        dispatch_block_t block = [mainQueue[0] copy];
        [mainQueue removeObjectAtIndex:0]; block(); [block release];
    }
}
static void echo(NSString *text, int caret, int sequence) {
    com_codename1_impl_ios_IOSNative_updateTextInputState___java_lang_String_int_int_int_int_int_int_int_int(
        NULL, nil, text, caret, caret, 0, 0, 1, 20, sequence, 0);
}
int main(void) {
    @autoreleasepool {
        mainQueue = [NSMutableArray new];
        CN1MacTextInputSession *session = [CN1MacTextInputSession sharedSession];
        TestView *view = [TestView new];
        [session reset:@"abcd"];
        [view cut:nil];
        assert(command == 13);
        int cutSequence = commandSequence;
        [view insertText:@"X" replacementRange:NSMakeRange(NSNotFound, 0)];
        assert([session.text isEqualToString:@"abcd"] && replacements == 0);
        echo(@"abcd", 3, cutSequence - 1); drain(); // stale echo cannot release typing
        assert(replacements == 0);
        echo(@"ad", 1, cutSequence); drain();
        assert([session.text isEqualToString:@"aXd"]);
        assert(NSEqualRanges(replacedRange, NSMakeRange(1, 0)));

        [session reset:@"abcd"];
        [view paste:nil];
        int firstPaste = commandSequence;
        int beforeCommands = commandCount;
        [view paste:nil];
        [view insertText:@"X" replacementRange:NSMakeRange(NSNotFound, 0)];
        assert(commandCount == beforeCommands);
        // Rich paste result intentionally differs from any plain-text clipboard guess.
        echo(@"a[image]d", 8, firstPaste); drain();
        assert(command == 14 && commandCount == beforeCommands + 1);
        assert([session.text isEqualToString:@"a[image]d"]);
        echo(@"a[image][table]d", 15, commandSequence); drain();
        assert([session.text isEqualToString:@"a[image][table]Xd"]);

        [session reset:@"abcd"];
        [view cut:nil];
        [view insertText:@"lost" replacementRange:NSMakeRange(NSNotFound, 0)];
        echo(@"ad", 1, commandSequence); // queued for old session
        [session reset:@"new document"];
        drain();
        assert([session.text isEqualToString:@"new document"]);
        [view insertText:@"X" replacementRange:NSMakeRange(NSNotFound, 0)];
        assert([session.text isEqualToString:@"nX document"]);
        for (NSNumber *pure in @[@YES, @NO]) {
            for (NSString *grapheme in @[@"e\u0301", @"\u2764\ufe0f", @"👩‍💻", @"🇮🇱"]) {
                NSString *text = [@"A" stringByAppendingString:[grapheme stringByAppendingString:@"B"]];
                [session reset:text]; session.pureEditor = pure.boolValue;
                NSUInteger end = 1 + grapheme.length;
                session.selectedRange = NSMakeRange(end, 0);
                session.selectionAnchor = end;
                [view cn1ForgetAnchor];
                [view doCommandBySelector:@selector(moveBackward:)];
                assert(NSEqualRanges(session.selectedRange, NSMakeRange(1, 0)));
                [view doCommandBySelector:@selector(moveForward:)];
                assert(NSEqualRanges(session.selectedRange, NSMakeRange(end, 0)));
                [view doCommandBySelector:@selector(moveBackwardAndModifySelection:)];
                assert(NSEqualRanges(session.selectedRange, NSMakeRange(1, grapheme.length)));
                [view doCommandBySelector:@selector(moveForwardAndModifySelection:)];
                assert(NSEqualRanges(session.selectedRange, NSMakeRange(end, 0)));
                session.selectedRange = NSMakeRange(1, 0); session.selectionAnchor = 1;
                [view cn1ForgetAnchor];
                [view doCommandBySelector:@selector(moveForwardAndModifySelection:)];
                assert(NSEqualRanges(session.selectedRange, NSMakeRange(1, grapheme.length)));
                [view insertText:@"X" replacementRange:NSMakeRange(NSNotFound, 0)];
                assert([session.text isEqualToString:@"AXB"]);
            }
            for (NSNumber *backward in @[@YES, @NO]) {
                for (NSNumber *forward in @[@YES, @NO]) {
                    [session reset:@"abcdefgh"]; session.pureEditor = pure.boolValue;
                    session.selectedRange = NSMakeRange(2, 3);
                    session.selectionAnchor = backward.boolValue ? 5 : 2;
                    [view cn1ForgetAnchor];
                    [view doCommandBySelector:forward.boolValue ? @selector(moveForward:) : @selector(moveBackward:)];
                    assert(NSEqualRanges(session.selectedRange, NSMakeRange(forward.boolValue ? 5 : 2, 0)));
                }
            }
        }
        for (NSNumber *menu in @[@YES, @NO]) {
            [session reset:@"abc"];
            session.selectedRange = NSMakeRange(3, 0); session.selectionAnchor = 3;
            if (menu.boolValue) { [view selectAll:nil]; }
            else { [view doCommandBySelector:@selector(selectAll:)]; }
            assert(reportedAnchor == 0 && reportedFocus == 3);
            [view doCommandBySelector:@selector(moveForwardAndModifySelection:)];
            assert(NSEqualRanges(session.selectedRange, NSMakeRange(0, 3)));
            [view doCommandBySelector:@selector(moveBackwardAndModifySelection:)];
            assert(NSEqualRanges(session.selectedRange, NSMakeRange(0, 2)));
            assert(reportedAnchor == 0 && reportedFocus == 2);
        }
        puts("MAC_EDITOR_COMMAND_ORDER_AND_SELECTION_OK");
    }
    return 0;
}
