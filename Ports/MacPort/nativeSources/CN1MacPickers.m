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
#import "CN1MacHost.h"
#import "CodenameOne_GLViewController.h"
#include "cn1_globals.h"
#include "com_codename1_impl_ios_IOSImplementation.h"

/*
 * Native pickers.
 *
 * The UIKit ports slide a UIDatePicker or a UIPickerView up from the bottom of
 * the screen, because that is where a phone puts a chooser. A Mac does not have
 * that gesture: it shows a popover anchored to the control that asked. So the
 * geometry the framework passes -- which on iOS is only used on the iPad, to
 * anchor a popover -- is what this port uses on every machine.
 *
 * The result travels back through the same datePickerResult callback all the
 * Apple ports use: milliseconds since the epoch for a date or a time, minutes
 * for a duration, an index for a list, and -1 for a cancel. Getting that wrong
 * is not a wrong value but a hang, because showNativePicker blocks on it.
 */

/// The popover currently on screen, if any. One at a time: the framework blocks
/// its calling thread until the open one answers.
static NSPopover *cn1MacPicker = nil;

/// Codename One pixels to AppKit points; see CN1MacHost, which sets it.
extern float scaleValue;

/// Reports the choice and takes the popover down. Every path out of a picker
/// goes through here, including the close-by-clicking-away one, so the blocked
/// Java thread is always released exactly once.
static void cn1PickerFinish(JAVA_LONG result) {
    if (cn1MacPicker == nil) {
        return;
    }
    NSPopover *closing = cn1MacPicker;
    cn1MacPicker = nil;
    [closing performClose:nil];
#ifndef CN1_USE_ARC
    [closing release];
#endif
    struct ThreadLocalData *threadStateData = getThreadLocalData();
    com_codename1_impl_ios_IOSImplementation_datePickerResult___long(threadStateData, result);
}

/// Owns the controls inside a picker popover and turns their actions into a
/// result. A separate object because a popover's content is a view controller
/// and the targets have to outlive the call that built them.
@interface CN1MacPickerController : NSViewController <NSPopoverDelegate>
@property (nonatomic, assign) int pickerType;
@property (nonatomic, retain) NSDatePicker *datePicker;
@property (nonatomic, retain) NSTableView *listView;
@property (nonatomic, retain) NSArray<NSString *> *choices;
@property (nonatomic, assign) NSInteger initialSelection;
@property (nonatomic, assign) BOOL answered;
@end

@implementation CN1MacPickerController

- (void)done:(id)sender {
    if (self.answered) {
        return;
    }
    self.answered = YES;
    if (self.listView != nil) {
        NSInteger row = self.listView.selectedRow;
        cn1PickerFinish(row < 0 ? -1 : (JAVA_LONG)row);
        return;
    }
    NSDate *date = self.datePicker.dateValue;
    if (self.pickerType == 5 || self.pickerType == 6 || self.pickerType == 7) {
        // A duration is minutes rather than an instant. NSDatePicker has no
        // duration mode, so hours and minutes are read back off the date the
        // picker was seeded with.
        NSCalendar *cal = [NSCalendar currentCalendar];
        NSDateComponents *parts = [cal components:(NSCalendarUnitHour | NSCalendarUnitMinute)
                                         fromDate:date];
        cn1PickerFinish((JAVA_LONG)(parts.hour * 60 + parts.minute));
        return;
    }
    cn1PickerFinish((JAVA_LONG)([date timeIntervalSince1970] * 1000));
}

- (void)cancel:(id)sender {
    if (self.answered) {
        return;
    }
    self.answered = YES;
    cn1PickerFinish(-1);
}

/// Clicking away is a cancel, and it has to be reported: without this the
/// framework's thread waits on a popover that is no longer on screen.
- (void)popoverDidClose:(NSNotification *)notification {
    if (!self.answered) {
        self.answered = YES;
        cn1PickerFinish(-1);
    }
}

- (NSInteger)numberOfRowsInTableView:(NSTableView *)tableView {
    return (NSInteger)self.choices.count;
}

- (id)tableView:(NSTableView *)tableView
    objectValueForTableColumn:(NSTableColumn *)tableColumn
                          row:(NSInteger)row {
    return row >= 0 && row < (NSInteger)self.choices.count ? self.choices[row] : @"";
}

#ifndef CN1_USE_ARC
- (void)dealloc {
    [_datePicker release];
    [_listView release];
    [_choices release];
    [super dealloc];
}
#endif

@end

/// Builds the popover's content: the chooser, a Cancel and a Done.
static NSView *cn1PickerContentView(CN1MacPickerController *controller, NSView *chooser,
                                    CGFloat width, CGFloat height) {
    CGFloat buttonRow = 32;
    NSView *content = [[NSView alloc] initWithFrame:NSMakeRect(0, 0, width, height + buttonRow)];
    chooser.frame = NSMakeRect(8, buttonRow, width - 16, height - 8);
    [content addSubview:chooser];

    NSButton *cancel = [NSButton buttonWithTitle:@"Cancel" target:controller action:@selector(cancel:)];
    cancel.frame = NSMakeRect(8, 4, 90, 24);
    cancel.bezelStyle = NSBezelStyleRounded;
    [content addSubview:cancel];

    NSButton *done = [NSButton buttonWithTitle:@"Done" target:controller action:@selector(done:)];
    done.frame = NSMakeRect(width - 98, 4, 90, 24);
    done.bezelStyle = NSBezelStyleRounded;
    // Return activates it, which is what a Mac sheet or popover does.
    done.keyEquivalent = @"\r";
    [content addSubview:done];
    return content;
}

/// Shows the popover over the rendering view, anchored to the rectangle the
/// framework reported for the component that asked.
static void cn1PickerPresent(CN1MacPickerController *controller, NSView *content,
                             JAVA_INT x, JAVA_INT y, JAVA_INT w, JAVA_INT h) {
    NSViewController *vc = controller;
    vc.view = content;

    NSPopover *popover = [[NSPopover alloc] init];
    popover.contentViewController = vc;
    popover.contentSize = content.frame.size;
    popover.behavior = NSPopoverBehaviorTransient;
    popover.delegate = controller;
    cn1MacPicker = popover;

    NSView *host = [CN1MacHost sharedHost].renderingView;
    CGFloat scale = scaleValue > 0 ? scaleValue : 1;
    NSRect anchor = NSMakeRect(x / scale, y / scale, MAX(w / scale, 1), MAX(h / scale, 1));
    if (w <= 0 || h <= 0) {
        anchor = NSMakeRect(host.bounds.size.width / 2, host.bounds.size.height / 2, 1, 1);
    }
    [popover showRelativeToRect:anchor ofView:host preferredEdge:NSMaxYEdge];
}

/// Entry points are plain C rather than ParparVM natives: the Java side of
/// pickers is the shared IOSNative declaration, and its macOS arm calls through
/// to here. Declaring a second pair of natives on MacNative would mean two Java
/// methods for one feature, and the one the framework does not call would be
/// dropped as dead.
void CN1MacOpenDatePicker(int type, long long time, int x, int y, int w, int h, int minuteStep) {
    dispatch_async(dispatch_get_main_queue(), ^{
        POOL_BEGIN();
        CN1MacPickerController *controller = [[CN1MacPickerController alloc] init];
        controller.pickerType = type;

        NSDatePicker *picker = [[NSDatePicker alloc] initWithFrame:NSMakeRect(0, 0, 260, 154)];
        picker.datePickerStyle = NSDatePickerStyleTextFieldAndStepper;
        switch (type) {
            case 1: // date
                picker.datePickerElements = NSDatePickerElementFlagYearMonthDay;
                // A graphical calendar for a plain date, which is what a Mac
                // shows and what the stepper style cannot express.
                picker.datePickerStyle = NSDatePickerStyleClockAndCalendar;
                picker.frame = NSMakeRect(0, 0, 260, 160);
                break;
            case 3: // date and time
                picker.datePickerElements = NSDatePickerElementFlagYearMonthDay
                                          | NSDatePickerElementFlagHourMinute;
                break;
            default: // time, and the three duration types
                picker.datePickerElements = NSDatePickerElementFlagHourMinute;
                break;
        }
        picker.dateValue = [NSDate dateWithTimeIntervalSince1970:time / 1000.0];
        controller.datePicker = picker;

        NSView *content = cn1PickerContentView(controller, picker,
                                               MAX(picker.frame.size.width + 16, 220),
                                               picker.frame.size.height + 8);
        cn1PickerPresent(controller, content, x, y, w, h);
#ifndef CN1_USE_ARC
        [picker release];
        [content release];
        [controller release];
#endif
        POOL_END();
    });
}

void CN1MacOpenStringPicker(JAVA_OBJECT stringArray, int selection, int x, int y, int w, int h) {
    NSMutableArray<NSString *> *choices = [NSMutableArray array];
    if (stringArray != JAVA_NULL) {
        JAVA_ARRAY arr = (JAVA_ARRAY)stringArray;
        JAVA_OBJECT *items = (JAVA_OBJECT *)arr->data;
        for (int i = 0; i < arr->length; i++) {
            NSString *s = items[i] == JAVA_NULL
                ? @""
                : toNSString(CN1_THREAD_GET_STATE_PASS_ARG items[i]);
            [choices addObject:s == nil ? @"" : s];
        }
    }
#ifndef CN1_USE_ARC
    [choices retain];
#endif
    dispatch_async(dispatch_get_main_queue(), ^{
        POOL_BEGIN();
        CN1MacPickerController *controller = [[CN1MacPickerController alloc] init];
        controller.pickerType = 4;
        controller.choices = choices;
        controller.initialSelection = selection;

        NSTableView *table = [[NSTableView alloc] initWithFrame:NSMakeRect(0, 0, 240, 180)];
        NSTableColumn *column = [[NSTableColumn alloc] initWithIdentifier:@"value"];
        column.width = 224;
        [table addTableColumn:column];
        table.headerView = nil;
        table.dataSource = (id)controller;
        table.delegate = (id)controller;
        // Double clicking a row is the same as choosing it and pressing Done,
        // which is how every Mac list chooser behaves.
        table.target = controller;
        table.doubleAction = @selector(done:);
        controller.listView = table;
        if (selection >= 0 && selection < (int)choices.count) {
            [table selectRowIndexes:[NSIndexSet indexSetWithIndex:selection]
               byExtendingSelection:NO];
            [table scrollRowToVisible:selection];
        }

        NSScrollView *scroll = [[NSScrollView alloc] initWithFrame:NSMakeRect(0, 0, 240, 180)];
        scroll.documentView = table;
        scroll.hasVerticalScroller = YES;
        scroll.borderType = NSBezelBorder;

        NSView *content = cn1PickerContentView(controller, scroll, 256, 188);
        cn1PickerPresent(controller, content, x, y, w, h);
#ifndef CN1_USE_ARC
        [column release];
        [table release];
        [scroll release];
        [content release];
        [controller release];
        [choices release];
#endif
        POOL_END();
    });
}
