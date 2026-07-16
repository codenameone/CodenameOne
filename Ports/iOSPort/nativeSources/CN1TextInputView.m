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
#import "CN1TextInputView.h"

#if !TARGET_OS_WATCH

#include "xmlvm.h"
#include "java_lang_String.h"
#ifndef NEW_CODENAME_ONE_VM
#include "xmlvm-util.h"
#else
#include "cn1_globals.h"
#endif
#import "CodenameOne_GLViewController.h"

// Codename One string conversion helpers (defined in IOSNative.m)
extern JAVA_OBJECT fromNSString(CN1_THREAD_STATE_MULTI_ARG NSString* str);
extern NSString* toNSString(CN1_THREAD_STATE_MULTI_ARG JAVA_OBJECT o);

// Java callbacks (generated for the static methods on IOSImplementation)
extern void com_codename1_impl_ios_IOSImplementation_tiReplaceRange___int_int_java_lang_String_int(CN1_THREAD_STATE_MULTI_ARG JAVA_INT start, JAVA_INT end, JAVA_OBJECT text, JAVA_INT seq);
extern void com_codename1_impl_ios_IOSImplementation_tiSetSelection___int_int_int(CN1_THREAD_STATE_MULTI_ARG JAVA_INT start, JAVA_INT end, JAVA_INT seq);
extern void com_codename1_impl_ios_IOSImplementation_tiCommit___java_lang_String_int(CN1_THREAD_STATE_MULTI_ARG JAVA_OBJECT text, JAVA_INT seq);
extern void com_codename1_impl_ios_IOSImplementation_tiSetComposing___java_lang_String_int_int(CN1_THREAD_STATE_MULTI_ARG JAVA_OBJECT text, JAVA_INT rel, JAVA_INT seq);
extern void com_codename1_impl_ios_IOSImplementation_tiFinishComposing___int(CN1_THREAD_STATE_MULTI_ARG JAVA_INT seq);
extern void com_codename1_impl_ios_IOSImplementation_tiKeyCommand___int_int(CN1_THREAD_STATE_MULTI_ARG JAVA_INT command, JAVA_INT modifiers);
extern void com_codename1_impl_ios_IOSImplementation_tiEditorAction___int(CN1_THREAD_STATE_MULTI_ARG JAVA_INT action);
extern JAVA_OBJECT com_codename1_impl_ios_IOSImplementation_tiSelectionRects___int_int_R_int_1ARRAY(CN1_THREAD_STATE_MULTI_ARG JAVA_INT start, JAVA_INT end);
extern JAVA_INT com_codename1_impl_ios_IOSImplementation_tiOffsetAtPoint___int_int_R_int(CN1_THREAD_STATE_MULTI_ARG JAVA_INT x, JAVA_INT y);

// mirror of com.codename1.ui.TextInputClient key command + modifier constants
#define CN1_KEY_LEFT 1
#define CN1_KEY_RIGHT 2
#define CN1_KEY_UP 3
#define CN1_KEY_DOWN 4
#define CN1_KEY_HOME 5
#define CN1_KEY_END 6
#define CN1_KEY_DELETE 10
#define CN1_KEY_ESCAPE 11
#define CN1_KEY_COPY 12
#define CN1_KEY_CUT 13
#define CN1_KEY_PASTE 14
#define CN1_KEY_SELECT_ALL 15
#define CN1_KEY_UNDO 16
#define CN1_KEY_REDO 17
#define CN1_MOD_SHIFT 1
#define CN1_MOD_CTRL 2
#define CN1_MOD_ALT 4

static CN1TextInputView* cn1TextInputView = nil;

#pragma mark - UITextPosition / UITextRange backed by integer offsets

@interface CN1TextPosition : UITextPosition
@property (nonatomic) NSInteger index;
+ (instancetype)positionWithIndex:(NSInteger)index;
@end

@implementation CN1TextPosition
+ (instancetype)positionWithIndex:(NSInteger)index {
    CN1TextPosition* p = [[[CN1TextPosition alloc] init] autorelease];
    p.index = index;
    return p;
}
@end

@interface CN1TextRange : UITextRange
@property (nonatomic) NSInteger startIndex;
@property (nonatomic) NSInteger endIndex;
+ (instancetype)rangeFrom:(NSInteger)start to:(NSInteger)end;
@end

@implementation CN1TextRange
+ (instancetype)rangeFrom:(NSInteger)start to:(NSInteger)end {
    CN1TextRange* r = [[[CN1TextRange alloc] init] autorelease];
    r.startIndex = MIN(start, end);
    r.endIndex = MAX(start, end);
    return r;
}
- (UITextPosition *)start { return [CN1TextPosition positionWithIndex:self.startIndex]; }
- (UITextPosition *)end { return [CN1TextPosition positionWithIndex:self.endIndex]; }
- (BOOL)isEmpty { return self.startIndex == self.endIndex; }
@end

/// One rectangle of a selection, reported to iOS so it can draw the selection highlight, the drag handles
/// (at the rects flagged containsStart / containsEnd) and the magnifier loupe over the range.
@interface CN1TextSelectionRect : UITextSelectionRect
@property (nonatomic) CGRect rectValue;
@property (nonatomic) BOOL startFlag;
@property (nonatomic) BOOL endFlag;
@end

@implementation CN1TextSelectionRect
- (CGRect)rect { return self.rectValue; }
- (NSWritingDirection)writingDirection { return NSWritingDirectionNatural; }
- (BOOL)containsStart { return self.startFlag; }
- (BOOL)containsEnd { return self.endFlag; }
- (BOOL)isVertical { return NO; }
@end

#pragma mark - CN1TextInputView

@interface CN1TextInputView () {
    NSMutableString* _shadow;
    NSRange _selectedRange;
    NSRange _markedRange;        // location == NSNotFound when there is no marked (IME) text
    int _editSeq;                // generation of the last local edit forwarded to Java
    CGRect _caretRectPoints;     // last caret rectangle from Java, converted to points
    CGRect _editorBoundsPoints;  // the editor component's bounds in points; the touch region we own
    id _textInteraction;         // UITextInteraction (iOS 13+) driving the system loupe/handles/menu
    // UITextInput declares inputDelegate as a weak property, but ParparVM compiles under manual
    // reference counting where weak is unavailable; back it with an unsafe-unretained ivar instead.
    __unsafe_unretained id<UITextInputDelegate> _inputDelegate;
}
@property (nonatomic, retain) UITextInputStringTokenizer* cn1Tokenizer;
@end

@implementation CN1TextInputView

@synthesize markedTextStyle = _markedTextStyle;

- (id<UITextInputDelegate>)inputDelegate { return _inputDelegate; }
- (void)setInputDelegate:(id<UITextInputDelegate>)inputDelegate { _inputDelegate = inputDelegate; }

- (instancetype)init {
    self = [super initWithFrame:CGRectMake(0, 0, 1, 1)];
    if (self) {
        self.backgroundColor = [UIColor clearColor];
        self.opaque = NO;
        // Interactive so physical-keyboard UIPress events reach us, but pointInside: returns NO below
        // so the view never claims a touch; Codename One keeps owning the canvas.
        self.userInteractionEnabled = YES;
        self.autocorrectionType = UITextAutocorrectionTypeDefault;
        self.autocapitalizationType = UITextAutocapitalizationTypeSentences;
        self.spellCheckingType = UITextSpellCheckingTypeDefault;
        self.keyboardType = UIKeyboardTypeDefault;
        self.returnKeyType = UIReturnKeyDefault;
        self.secureTextEntry = NO;
        self.multiline = YES;
        self.actionType = 0;
        _shadow = [[NSMutableString alloc] init];
        _selectedRange = NSMakeRange(0, 0);
        _markedRange = NSMakeRange(NSNotFound, 0);
        _caretRectPoints = CGRectMake(0, 0, 2, 16);
    }
    return self;
}

- (void)dealloc {
    [_shadow release];
    [_markedTextStyle release];
    [_cn1Tokenizer release];
    [_textInteraction release];
    [super dealloc];
}

- (BOOL)canBecomeFirstResponder { return YES; }
- (UIView *)textInputView { return self; }

// While the native text interaction (system loupe/handles) is active we OWN touches inside the editor's
// bounds so its gesture recognizers fire there; everywhere else (and when no interaction is attached)
// touches pass through to Codename One's canvas. UIPress (hardware key) events always arrive regardless
// because they target the first responder directly, not via hit-testing.
- (BOOL)pointInside:(CGPoint)point withEvent:(UIEvent *)event {
    if (_textInteraction == nil || CGRectIsEmpty(_editorBoundsPoints)) {
        return NO;
    }
    return CGRectContainsPoint(_editorBoundsPoints, point);
}

- (void)cn1SetEditorBounds:(CGRect)b {
    _editorBoundsPoints = b;
}

/// Lazily installs the UITextInteraction that makes iOS draw its own selection magnifier, drag handles and
/// edit menu over the editor. Unavailable on tvOS and before iOS 13, where the cross-platform Codename One
/// drawn selection UI is used instead.
- (void)cn1EnsureInteraction {
#if !TARGET_OS_TV
    if (@available(iOS 13.0, *)) {
        if (_textInteraction == nil) {
            UITextInteraction* ti = [UITextInteraction textInteractionForMode:UITextInteractionModeEditable];
            ti.textInput = self;
            [self addInteraction:ti];
            _textInteraction = [ti retain];
        }
    }
#endif
}

#pragma mark hardware / bluetooth keyboard (UIPress)

- (int)cn1ModsFor:(UIKeyModifierFlags)mf {
    int mods = 0;
    if (mf & UIKeyModifierShift) {
        mods |= CN1_MOD_SHIFT;
    }
    if (mf & (UIKeyModifierControl | UIKeyModifierCommand)) {
        mods |= CN1_MOD_CTRL;
    }
    if (mf & UIKeyModifierAlternate) {
        mods |= CN1_MOD_ALT;
    }
    return mods;
}

- (void)cn1KeyCommand:(int)cmd mods:(int)mods {
    com_codename1_impl_ios_IOSImplementation_tiKeyCommand___int_int(CN1_THREAD_GET_STATE_PASS_ARG cmd, mods);
}

- (BOOL)cn1HandleKey:(UIKey *)key {
    if (key == nil) {
        return NO;
    }
    UIKeyModifierFlags mf = key.modifierFlags;
    int mods = [self cn1ModsFor:mf];
    UIKeyboardHIDUsage code = key.keyCode;
    switch (code) {
        case UIKeyboardHIDUsageKeyboardDeleteOrBackspace:
            [self deleteBackward];
            return YES;
        case UIKeyboardHIDUsageKeyboardDeleteForward:
            [self cn1KeyCommand:CN1_KEY_DELETE mods:mods];
            return YES;
        case UIKeyboardHIDUsageKeyboardReturnOrEnter:
        case UIKeyboardHIDUsageKeypadEnter:
            [self insertText:@"\n"];
            return YES;
        case UIKeyboardHIDUsageKeyboardTab:
            [self insertText:@"\t"];
            return YES;
        case UIKeyboardHIDUsageKeyboardLeftArrow:
            [self cn1KeyCommand:((mf & UIKeyModifierCommand) ? CN1_KEY_HOME : CN1_KEY_LEFT) mods:mods];
            return YES;
        case UIKeyboardHIDUsageKeyboardRightArrow:
            [self cn1KeyCommand:((mf & UIKeyModifierCommand) ? CN1_KEY_END : CN1_KEY_RIGHT) mods:mods];
            return YES;
        case UIKeyboardHIDUsageKeyboardUpArrow:
            [self cn1KeyCommand:CN1_KEY_UP mods:mods];
            return YES;
        case UIKeyboardHIDUsageKeyboardDownArrow:
            [self cn1KeyCommand:CN1_KEY_DOWN mods:mods];
            return YES;
        case UIKeyboardHIDUsageKeyboardEscape:
            [self cn1KeyCommand:CN1_KEY_ESCAPE mods:mods];
            return YES;
        default:
            break;
    }
    NSString* chars = key.characters;
    if (mf & UIKeyModifierCommand) {
        // Cmd shortcuts -> editor commands; swallow anything else so it is not typed as a character.
        NSString* lc = [chars lowercaseString];
        if ([lc isEqualToString:@"c"]) {
            [self cn1KeyCommand:CN1_KEY_COPY mods:mods];
        } else if ([lc isEqualToString:@"x"]) {
            [self cn1KeyCommand:CN1_KEY_CUT mods:mods];
        } else if ([lc isEqualToString:@"v"]) {
            [self cn1KeyCommand:CN1_KEY_PASTE mods:mods];
        } else if ([lc isEqualToString:@"a"]) {
            [self cn1KeyCommand:CN1_KEY_SELECT_ALL mods:mods];
        } else if ([lc isEqualToString:@"z"]) {
            [self cn1KeyCommand:((mf & UIKeyModifierShift) ? CN1_KEY_REDO : CN1_KEY_UNDO) mods:mods];
        }
        return YES;
    }
    if (chars != nil && chars.length > 0) {
        unichar c0 = [chars characterAtIndex:0];
        // Only insert printable text; ignore control characters and the private-use arrow codes.
        if (c0 >= 0x20 && c0 != 0x7F && !(c0 >= 0xF700 && c0 <= 0xF8FF)) {
            [self insertText:chars];
            return YES;
        }
    }
    return NO;
}

- (void)pressesBegan:(NSSet<UIPress *> *)presses withEvent:(UIPressesEvent *)event {
    BOOL handled = NO;
    if (@available(iOS 13.4, tvOS 13.4, *)) {
        for (UIPress* press in presses) {
            if ([self cn1HandleKey:press.key]) {
                handled = YES;
            }
        }
    }
    if (!handled) {
        [super pressesBegan:presses withEvent:event];
    }
}

// Arrow keys are otherwise consumed by UIKit's own UITextInput navigation (which relies on per-offset
// caret geometry we do not provide), so claim them explicitly here and route to the editor's caret
// navigation. wantsPriorityOverSystemBehavior ensures we win over the system text navigation.
- (NSArray<UIKeyCommand *> *)keyCommands {
    NSArray* inputs = @[UIKeyInputLeftArrow, UIKeyInputRightArrow, UIKeyInputUpArrow, UIKeyInputDownArrow];
    NSArray* mods = @[@0,
                      @(UIKeyModifierShift),
                      @(UIKeyModifierAlternate),
                      @(UIKeyModifierCommand),
                      @(UIKeyModifierShift | UIKeyModifierAlternate),
                      @(UIKeyModifierShift | UIKeyModifierCommand)];
    NSMutableArray<UIKeyCommand *>* cmds = [NSMutableArray array];
    for (NSString* inp in inputs) {
        for (NSNumber* m in mods) {
            UIKeyCommand* kc = [UIKeyCommand keyCommandWithInput:inp
                                                   modifierFlags:(UIKeyModifierFlags)[m integerValue]
                                                          action:@selector(cn1Arrow:)];
            if (@available(iOS 15.0, tvOS 15.0, *)) {
                kc.wantsPriorityOverSystemBehavior = YES;
            }
            [cmds addObject:kc];
        }
    }
    return cmds;
}

- (void)cn1Arrow:(UIKeyCommand *)cmd {
    NSString* inp = cmd.input;
    UIKeyModifierFlags mf = cmd.modifierFlags;
    int mods = [self cn1ModsFor:mf];
    int c = 0;
    if ([inp isEqualToString:UIKeyInputLeftArrow]) {
        c = (mf & UIKeyModifierCommand) ? CN1_KEY_HOME : CN1_KEY_LEFT;
    } else if ([inp isEqualToString:UIKeyInputRightArrow]) {
        c = (mf & UIKeyModifierCommand) ? CN1_KEY_END : CN1_KEY_RIGHT;
    } else if ([inp isEqualToString:UIKeyInputUpArrow]) {
        c = CN1_KEY_UP;
    } else if ([inp isEqualToString:UIKeyInputDownArrow]) {
        c = CN1_KEY_DOWN;
    }
    if (c != 0) {
        [self cn1KeyCommand:c mods:mods];
    }
}

#pragma mark helpers

- (NSInteger)clampIndex:(NSInteger)i {
    if (i < 0) return 0;
    if (i > (NSInteger)_shadow.length) return (NSInteger)_shadow.length;
    return i;
}

- (NSRange)clampRange:(NSRange)r {
    NSInteger loc = [self clampIndex:(NSInteger)r.location];
    NSInteger end = [self clampIndex:(NSInteger)(r.location + r.length)];
    return NSMakeRange(loc, end - loc);
}

// Bumps and returns the local edit generation. Every edit that notifies Java carries a seq;
// Java echoes the seq of the last edit it APPLIED with each state push, and cn1SyncText drops
// echoes whose seq is stale so a slow round trip can never regress the shadow under fast typing.
- (int)cn1NextSeq {
    return ++_editSeq;
}

// Applies an edit to the shadow mirror and (optionally) forwards it to Java as a range replacement.
- (void)cn1EditRange:(NSRange)range with:(NSString *)text notify:(BOOL)notify {
    if (text == nil) {
        text = @"";
    }
    range = [self clampRange:range];
    [_shadow replaceCharactersInRange:range withString:text];
    _selectedRange = NSMakeRange(range.location + text.length, 0);
    _markedRange = NSMakeRange(NSNotFound, 0);
    if (notify) {
        com_codename1_impl_ios_IOSImplementation_tiReplaceRange___int_int_java_lang_String_int(CN1_THREAD_GET_STATE_PASS_ARG
                (JAVA_INT)range.location, (JAVA_INT)(range.location + range.length),
                fromNSString(CN1_THREAD_GET_STATE_PASS_ARG text), (JAVA_INT)[self cn1NextSeq]);
    }
}

#pragma mark UIKeyInput

- (BOOL)hasText { return _shadow.length > 0; }

- (void)insertText:(NSString *)text {
    if (text == nil) {
        return;
    }
    if (!self.multiline && [text isEqualToString:@"\n"]) {
        // Return on a single line field is the configured editor action (Done / Next / Search /
        // Send), never a literal newline; the client decides what the action does.
        com_codename1_impl_ios_IOSImplementation_tiEditorAction___int(CN1_THREAD_GET_STATE_PASS_ARG
                (JAVA_INT)self.actionType);
        return;
    }
    // UIKit contract: insertText replaces the marked (composing) text when present, otherwise the
    // selection. Deliver it as a COMMIT (like the Android and JS ports) so the client finalizes an
    // active composition correctly and typed-text hooks (auto indent, bracket pairing) run.
    NSRange target = (_markedRange.location != NSNotFound) ? _markedRange : _selectedRange;
    target = [self clampRange:target];
    [_shadow replaceCharactersInRange:target withString:text];
    _selectedRange = NSMakeRange(target.location + text.length, 0);
    _markedRange = NSMakeRange(NSNotFound, 0);
    com_codename1_impl_ios_IOSImplementation_tiCommit___java_lang_String_int(CN1_THREAD_GET_STATE_PASS_ARG
            fromNSString(CN1_THREAD_GET_STATE_PASS_ARG text), (JAVA_INT)[self cn1NextSeq]);
}

- (void)deleteBackward {
    if (_selectedRange.length > 0) {
        [self cn1EditRange:_selectedRange with:@"" notify:YES];
    } else if (_selectedRange.location > 0) {
        // delete the full composed character sequence so surrogate pairs / emoji clusters are
        // never split into a lone surrogate
        NSRange r = [_shadow rangeOfComposedCharacterSequenceAtIndex:(NSUInteger)(_selectedRange.location - 1)];
        [self cn1EditRange:r with:@"" notify:YES];
    }
}

#pragma mark UITextInput - text

- (NSString *)textInRange:(UITextRange *)range {
    CN1TextRange* r = (CN1TextRange *)range;
    NSRange nr = [self clampRange:NSMakeRange(r.startIndex, r.endIndex - r.startIndex)];
    return [_shadow substringWithRange:nr];
}

- (void)replaceRange:(UITextRange *)range withText:(NSString *)text {
    CN1TextRange* r = (CN1TextRange *)range;
    [self cn1EditRange:NSMakeRange(r.startIndex, r.endIndex - r.startIndex) with:text notify:YES];
}

#pragma mark UITextInput - selection

- (UITextRange *)selectedTextRange {
    return [CN1TextRange rangeFrom:_selectedRange.location to:_selectedRange.location + _selectedRange.length];
}

- (void)setSelectedTextRange:(UITextRange *)selectedTextRange {
    NSRange newRange;
    if (selectedTextRange == nil) {
        newRange = NSMakeRange(_shadow.length, 0);
    } else {
        CN1TextRange* r = (CN1TextRange *)selectedTextRange;
        NSInteger s = [self clampIndex:r.startIndex];
        NSInteger e = [self clampIndex:r.endIndex];
        newRange = NSMakeRange(s, e - s);
    }
    if (NSEqualRanges(newRange, _selectedRange)) {
        return;
    }
    _selectedRange = newRange;
    com_codename1_impl_ios_IOSImplementation_tiSetSelection___int_int_int(CN1_THREAD_GET_STATE_PASS_ARG
            (JAVA_INT)newRange.location, (JAVA_INT)(newRange.location + newRange.length),
            (JAVA_INT)[self cn1NextSeq]);
}

#pragma mark UITextInput - marked text (IME composition)

- (UITextRange *)markedTextRange {
    if (_markedRange.location == NSNotFound) {
        return nil;
    }
    return [CN1TextRange rangeFrom:_markedRange.location to:_markedRange.location + _markedRange.length];
}

- (void)setMarkedText:(NSString *)markedText selectedRange:(NSRange)selectedRange {
    if (markedText == nil) {
        markedText = @"";
    }
    NSRange rep = (_markedRange.location != NSNotFound) ? _markedRange : _selectedRange;
    rep = [self clampRange:rep];
    [_shadow replaceCharactersInRange:rep withString:markedText];
    _markedRange = NSMakeRange(rep.location, markedText.length);
    NSInteger sel = (NSInteger)selectedRange.location;
    if (sel < 0) {
        sel = 0;
    }
    if (sel > (NSInteger)markedText.length) {
        sel = (NSInteger)markedText.length;
    }
    _selectedRange = NSMakeRange(rep.location + sel, 0);
    // Forward the composition through the client's composing contract (like Android's
    // setComposingText and the JS port's composition events) so the editor tracks the marked
    // range, defers undo recording until the composition finalizes, and reports the composing
    // span back through its editing state.
    com_codename1_impl_ios_IOSImplementation_tiSetComposing___java_lang_String_int_int(CN1_THREAD_GET_STATE_PASS_ARG
            fromNSString(CN1_THREAD_GET_STATE_PASS_ARG markedText), (JAVA_INT)sel,
            (JAVA_INT)[self cn1NextSeq]);
}

- (void)unmarkText {
    if (_markedRange.location != NSNotFound) {
        _markedRange = NSMakeRange(NSNotFound, 0);
        // the composed text stays; the client finalizes it as one undo unit
        com_codename1_impl_ios_IOSImplementation_tiFinishComposing___int(CN1_THREAD_GET_STATE_PASS_ARG
                (JAVA_INT)[self cn1NextSeq]);
    }
}

#pragma mark UITextInput - positions and ranges

- (UITextPosition *)beginningOfDocument { return [CN1TextPosition positionWithIndex:0]; }
- (UITextPosition *)endOfDocument { return [CN1TextPosition positionWithIndex:_shadow.length]; }

- (UITextRange *)textRangeFromPosition:(UITextPosition *)fromPosition toPosition:(UITextPosition *)toPosition {
    return [CN1TextRange rangeFrom:((CN1TextPosition *)fromPosition).index to:((CN1TextPosition *)toPosition).index];
}

- (UITextPosition *)positionFromPosition:(UITextPosition *)position offset:(NSInteger)offset {
    NSInteger idx = ((CN1TextPosition *)position).index + offset;
    if (idx < 0 || idx > (NSInteger)_shadow.length) {
        return nil;
    }
    return [CN1TextPosition positionWithIndex:idx];
}

- (UITextPosition *)positionFromPosition:(UITextPosition *)position inDirection:(UITextLayoutDirection)direction offset:(NSInteger)offset {
    NSInteger idx = ((CN1TextPosition *)position).index;
    if (direction == UITextLayoutDirectionLeft || direction == UITextLayoutDirectionUp) {
        idx -= offset;
    } else {
        idx += offset;
    }
    return [CN1TextPosition positionWithIndex:[self clampIndex:idx]];
}

- (NSComparisonResult)comparePosition:(UITextPosition *)position toPosition:(UITextPosition *)other {
    NSInteger a = ((CN1TextPosition *)position).index;
    NSInteger b = ((CN1TextPosition *)other).index;
    if (a < b) return NSOrderedAscending;
    if (a > b) return NSOrderedDescending;
    return NSOrderedSame;
}

- (NSInteger)offsetFromPosition:(UITextPosition *)from toPosition:(UITextPosition *)toPosition {
    return ((CN1TextPosition *)toPosition).index - ((CN1TextPosition *)from).index;
}

- (UITextPosition *)positionWithinRange:(UITextRange *)range farthestInDirection:(UITextLayoutDirection)direction {
    CN1TextRange* r = (CN1TextRange *)range;
    BOOL toStart = (direction == UITextLayoutDirectionLeft || direction == UITextLayoutDirectionUp);
    return [CN1TextPosition positionWithIndex:(toStart ? r.startIndex : r.endIndex)];
}

- (UITextRange *)characterRangeByExtendingPosition:(UITextPosition *)position inDirection:(UITextLayoutDirection)direction {
    NSInteger idx = ((CN1TextPosition *)position).index;
    if (direction == UITextLayoutDirectionLeft || direction == UITextLayoutDirectionUp) {
        return [CN1TextRange rangeFrom:[self clampIndex:idx - 1] to:idx];
    }
    return [CN1TextRange rangeFrom:idx to:[self clampIndex:idx + 1]];
}

#pragma mark UITextInput - writing direction

- (UITextWritingDirection)baseWritingDirectionForPosition:(UITextPosition *)position inDirection:(UITextStorageDirection)direction {
    return UITextWritingDirectionLeftToRight;
}

- (void)setBaseWritingDirection:(UITextWritingDirection)writingDirection forRange:(UITextRange *)range {
}

#pragma mark UITextInput - geometry

- (CGRect)caretRectForPosition:(UITextPosition *)position {
    return _caretRectPoints;
}

- (CGRect)firstRectForRange:(UITextRange *)range {
    NSArray<UITextSelectionRect *>* rects = [self selectionRectsForRange:range];
    if (rects.count > 0) {
        return rects[0].rect;
    }
    return _caretRectPoints;
}

- (NSArray<UITextSelectionRect *> *)selectionRectsForRange:(UITextRange *)range {
    NSMutableArray<UITextSelectionRect *>* result = [NSMutableArray array];
    CN1TextRange* r = (CN1TextRange *)range;
    JAVA_OBJECT arr = com_codename1_impl_ios_IOSImplementation_tiSelectionRects___int_int_R_int_1ARRAY(CN1_THREAD_GET_STATE_PASS_ARG
            (JAVA_INT)r.startIndex, (JAVA_INT)r.endIndex);
    if (arr == JAVA_NULL) {
        return result;
    }
    int len = (int)((JAVA_ARRAY)arr)->length;
    JAVA_ARRAY_INT* data = (JAVA_ARRAY_INT*)((JAVA_ARRAY)arr)->data;
    CGFloat scale = [UIScreen mainScreen].scale;
    if (scale <= 0) {
        scale = 1;
    }
    int count = len / 4;
    for (int i = 0; i < count; i++) {
        // Java reports absolute screen pixels; the input view is full screen so its coordinate space is
        // screen points -> divide by scale.
        CGRect rc = CGRectMake(data[i * 4] / scale, data[i * 4 + 1] / scale,
                data[i * 4 + 2] / scale, data[i * 4 + 3] / scale);
        CN1TextSelectionRect* sr = [[[CN1TextSelectionRect alloc] init] autorelease];
        sr.rectValue = rc;
        sr.startFlag = (i == 0);
        sr.endFlag = (i == count - 1);
        [result addObject:sr];
    }
    return result;
}

- (UITextPosition *)closestPositionToPoint:(CGPoint)point {
    CGFloat scale = [UIScreen mainScreen].scale;
    if (scale <= 0) {
        scale = 1;
    }
    // point is in the view's (full screen) coordinate space = screen points; Java hit-tests in pixels.
    JAVA_INT off = com_codename1_impl_ios_IOSImplementation_tiOffsetAtPoint___int_int_R_int(CN1_THREAD_GET_STATE_PASS_ARG
            (JAVA_INT)(point.x * scale), (JAVA_INT)(point.y * scale));
    return [CN1TextPosition positionWithIndex:[self clampIndex:off]];
}

- (UITextPosition *)closestPositionToPoint:(CGPoint)point withinRange:(UITextRange *)range {
    CN1TextPosition* p = (CN1TextPosition *)[self closestPositionToPoint:point];
    CN1TextRange* r = (CN1TextRange *)range;
    NSInteger idx = p.index;
    if (idx < r.startIndex) {
        idx = r.startIndex;
    }
    if (idx > r.endIndex) {
        idx = r.endIndex;
    }
    return [CN1TextPosition positionWithIndex:idx];
}

- (UITextRange *)characterRangeAtPoint:(CGPoint)point {
    CN1TextPosition* p = (CN1TextPosition *)[self closestPositionToPoint:point];
    NSInteger idx = p.index;
    NSInteger end = [self clampIndex:idx + 1];
    return [CN1TextRange rangeFrom:idx to:end];
}

#pragma mark UITextInput - tokenizer

- (id<UITextInputTokenizer>)tokenizer {
    if (self.cn1Tokenizer == nil) {
        self.cn1Tokenizer = [[[UITextInputStringTokenizer alloc] initWithTextInput:self] autorelease];
    }
    return self.cn1Tokenizer;
}

#pragma mark sync from Java

- (void)cn1SyncText:(NSString *)text selStart:(NSInteger)selStart selEnd:(NSInteger)selEnd
          caretRect:(CGRect)caretPixels seq:(NSInteger)seq {
    CGFloat scale = [UIScreen mainScreen].scale;
    if (scale <= 0) {
        scale = 1;
    }
    _caretRectPoints = CGRectMake(caretPixels.origin.x / scale, caretPixels.origin.y / scale,
            MAX((CGFloat)2, caretPixels.size.width / scale), caretPixels.size.height / scale);
    // A stale echo (the state push for edit N arriving after the user already typed edit N+1)
    // must not regress the shadow text or caret; only the caret geometry above is refreshed.
    if (seq != (NSInteger)_editSeq) {
        return;
    }
    // Never disturb an active IME composition; only the caret geometry is refreshed above.
    if (_markedRange.location != NSNotFound) {
        return;
    }
    if (text != nil) {
        [_shadow setString:text];
    }
    NSInteger len = (NSInteger)_shadow.length;
    NSInteger s = MAX((NSInteger)0, MIN(selStart, len));
    NSInteger e = MAX((NSInteger)0, MIN(selEnd, len));
    _selectedRange = NSMakeRange(MIN(s, e), (NSUInteger)(s > e ? s - e : e - s));
}

- (void)cn1ResetComposition {
    // A marked range or edit generation left over from a previous session would make cn1SyncText
    // refuse the new session's state forever; both reset when a session starts or stops.
    _markedRange = NSMakeRange(NSNotFound, 0);
    _editSeq = 0;
}

@end

#pragma mark - native bridge (declared native in IOSNative.java)

void com_codename1_impl_ios_IOSNative_startTextInput___int_boolean_boolean_boolean_java_lang_String_int_int_int(CN1_THREAD_STATE_MULTI_ARG JAVA_OBJECT instanceObject, JAVA_INT constraint, JAVA_BOOLEAN autoCorrect, JAVA_BOOLEAN autoCapitalize, JAVA_BOOLEAN multiline, JAVA_OBJECT initialText, JAVA_INT selStart, JAVA_INT selEnd, JAVA_INT actionType) {
    NSString* startText = initialText != NULL ? toNSString(CN1_THREAD_GET_STATE_PASS_ARG initialText) : @"";
    dispatch_async(dispatch_get_main_queue(), ^{
        @autoreleasepool {
            if (cn1TextInputView == nil) {
                cn1TextInputView = [[CN1TextInputView alloc] init];
            }
            cn1TextInputView.autocorrectionType = autoCorrect ? UITextAutocorrectionTypeYes : UITextAutocorrectionTypeNo;
            cn1TextInputView.autocapitalizationType = autoCapitalize ? UITextAutocapitalizationTypeSentences : UITextAutocapitalizationTypeNone;
            cn1TextInputView.spellCheckingType = autoCorrect ? UITextSpellCheckingTypeDefault : UITextSpellCheckingTypeNo;
            cn1TextInputView.multiline = multiline != 0;
            cn1TextInputView.actionType = (int)actionType;
            // keyboard style + secure entry follow the field's constraint; a password field with
            // the default keyboard would otherwise feed autocorrect learning
            BOOL secure = (constraint & 0x10000) != 0; // TextArea.PASSWORD
            UIKeyboardType kt = UIKeyboardTypeDefault;
            switch (constraint & 0xffff) {
                case 1: // TextArea.EMAILADDR
                    kt = UIKeyboardTypeEmailAddress;
                    break;
                case 2: // TextArea.NUMERIC
                    kt = UIKeyboardTypeNumbersAndPunctuation;
                    break;
                case 3: // TextArea.PHONENUMBER
                    kt = UIKeyboardTypePhonePad;
                    break;
                case 4: // TextArea.URL
                    kt = UIKeyboardTypeURL;
                    break;
                case 5: // TextArea.DECIMAL
                    kt = UIKeyboardTypeDecimalPad;
                    break;
                default:
                    break;
            }
            cn1TextInputView.keyboardType = kt;
            cn1TextInputView.secureTextEntry = secure;
            if (secure) {
                cn1TextInputView.autocorrectionType = UITextAutocorrectionTypeNo;
                cn1TextInputView.spellCheckingType = UITextSpellCheckingTypeNo;
                cn1TextInputView.autocapitalizationType = UITextAutocapitalizationTypeNone;
            }
            UIReturnKeyType rk = UIReturnKeyDefault;
            switch (actionType) {
                case 1: // TextInputConfig.ACTION_DONE
                    rk = UIReturnKeyDone;
                    break;
                case 2: // TextInputConfig.ACTION_NEXT
                    rk = UIReturnKeyNext;
                    break;
                case 3: // TextInputConfig.ACTION_SEARCH
                    rk = UIReturnKeySearch;
                    break;
                case 4: // TextInputConfig.ACTION_SEND
                    rk = UIReturnKeySend;
                    break;
                default:
                    break;
            }
            cn1TextInputView.returnKeyType = rk;
            [cn1TextInputView cn1ResetComposition];
            [cn1TextInputView cn1SyncText:(startText != nil ? startText : @"") selStart:selStart selEnd:selEnd caretRect:CGRectMake(0, 0, 2, 16) seq:0];
            UIViewController* vc = [CodenameOne_GLViewController instance];
            if (vc != nil) {
                cn1TextInputView.frame = vc.view.bounds;
                if (cn1TextInputView.superview == nil) {
                    [vc.view addSubview:cn1TextInputView];
                }
            }
            [cn1TextInputView becomeFirstResponder];
            [cn1TextInputView cn1EnsureInteraction];
        }
    });
}

void com_codename1_impl_ios_IOSNative_setTextInputBounds___int_int_int_int(CN1_THREAD_STATE_MULTI_ARG JAVA_OBJECT instanceObject, JAVA_INT x, JAVA_INT y, JAVA_INT w, JAVA_INT h) {
    dispatch_async(dispatch_get_main_queue(), ^{
        @autoreleasepool {
            if (cn1TextInputView != nil) {
                CGFloat scale = [UIScreen mainScreen].scale;
                if (scale <= 0) {
                    scale = 1;
                }
                [cn1TextInputView cn1SetEditorBounds:CGRectMake(x / scale, y / scale, w / scale, h / scale)];
            }
        }
    });
}

void com_codename1_impl_ios_IOSNative_updateTextInputState___java_lang_String_int_int_int_int_int_int_int(CN1_THREAD_STATE_MULTI_ARG JAVA_OBJECT instanceObject, JAVA_OBJECT text, JAVA_INT selStart, JAVA_INT selEnd, JAVA_INT caretX, JAVA_INT caretY, JAVA_INT caretW, JAVA_INT caretH, JAVA_INT seq) {
    NSString* nsText = text != NULL ? toNSString(CN1_THREAD_GET_STATE_PASS_ARG text) : nil;
    CGRect caret = CGRectMake(caretX, caretY, caretW, caretH);
    dispatch_async(dispatch_get_main_queue(), ^{
        @autoreleasepool {
            if (cn1TextInputView != nil) {
                [cn1TextInputView cn1SyncText:nsText selStart:selStart selEnd:selEnd caretRect:caret seq:seq];
            }
        }
    });
}

void com_codename1_impl_ios_IOSNative_stopTextInput__(CN1_THREAD_STATE_MULTI_ARG JAVA_OBJECT instanceObject) {
    dispatch_async(dispatch_get_main_queue(), ^{
        @autoreleasepool {
            if (cn1TextInputView != nil) {
                [cn1TextInputView cn1ResetComposition];
                [cn1TextInputView resignFirstResponder];
                [cn1TextInputView removeFromSuperview];
            }
        }
    });
}

#else

// watchOS has no UIKeyInput / UITextInput keyboard support; provide stubs so the symbols still link.
void com_codename1_impl_ios_IOSNative_startTextInput___int_boolean_boolean_boolean_java_lang_String_int_int_int(CN1_THREAD_STATE_MULTI_ARG JAVA_OBJECT instanceObject, JAVA_INT constraint, JAVA_BOOLEAN autoCorrect, JAVA_BOOLEAN autoCapitalize, JAVA_BOOLEAN multiline, JAVA_OBJECT initialText, JAVA_INT selStart, JAVA_INT selEnd, JAVA_INT actionType) {
}
void com_codename1_impl_ios_IOSNative_updateTextInputState___java_lang_String_int_int_int_int_int_int_int(CN1_THREAD_STATE_MULTI_ARG JAVA_OBJECT instanceObject, JAVA_OBJECT text, JAVA_INT selStart, JAVA_INT selEnd, JAVA_INT caretX, JAVA_INT caretY, JAVA_INT caretW, JAVA_INT caretH, JAVA_INT seq) {
}
void com_codename1_impl_ios_IOSNative_setTextInputBounds___int_int_int_int(CN1_THREAD_STATE_MULTI_ARG JAVA_OBJECT instanceObject, JAVA_INT x, JAVA_INT y, JAVA_INT w, JAVA_INT h) {
}
void com_codename1_impl_ios_IOSNative_stopTextInput__(CN1_THREAD_STATE_MULTI_ARG JAVA_OBJECT instanceObject) {
}

#endif
