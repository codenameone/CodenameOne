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
/// Rounds to the nearest multiple of step, for negative values as well.
///
/// C division truncates toward zero, so (v + step/2) / step is nearest-rounding
/// only for v >= 0. An instant before 1970 -- a date picker showing a historical
/// date -- rounded the wrong way: eight minutes before the epoch with a quarter
/// hour step moved to the epoch rather than to the quarter hour before it, and
/// the same expression seeds the control, so merely opening the picker shifted
/// the value it was given.
static long long cn1RoundToStep(long long value, long long step) {
    if (step <= 0) {
        return value;
    }
    long long shifted = value + step / 2;
    long long q = shifted / step;
    // C truncates toward zero, so for a negative shifted value the quotient is
    // one too high: floor is what nearest-rounding needs. -8 minutes at a
    // quarter-hour step gives shifted = -30s, q = 0 by truncation and 0 as the
    // answer -- the epoch, when the quarter hour BELOW it is nearer.
    if (shifted < 0 && q * step != shifted) {
        q--;
    }
    return q * step;
}

@interface CN1MacPickerController : NSViewController <NSPopoverDelegate>
@property (nonatomic, assign) int pickerType;
@property (nonatomic, retain) NSDatePicker *datePicker;
@property (nonatomic, retain) NSTableView *listView;
@property (nonatomic, retain) NSArray<NSString *> *choices;
@property (nonatomic, assign) NSInteger initialSelection;
@property (nonatomic, assign) BOOL answered;
/// Minutes the application allows between selectable values, or 1 for every
/// minute. NSDatePicker has no step of its own, so this is enforced on commit.
@property (nonatomic, assign) int minuteStep;
/// The duration fields, when this is a duration picker. NSDatePicker cannot
/// express a duration at all -- see cn1BuildDurationView.
@property (nonatomic, retain) NSTextField *durationHoursField;
@property (nonatomic, retain) NSTextField *durationMinutesField;
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
    if (self.durationHoursField != nil || self.durationMinutesField != nil) {
        // Read straight out of the fields. A duration is a count, not an
        // instant, so there is no date here to convert.
        long long hours = self.durationHoursField != nil
            ? (long long)self.durationHoursField.integerValue : 0;
        long long minutes = self.durationMinutesField != nil
            ? (long long)self.durationMinutesField.integerValue : 0;
        long long total = hours * 3600000LL + minutes * 60000LL;
        if (self.minuteStep > 1) {
            long long stepMs = (long long)self.minuteStep * 60000LL;
            total = (long long)cn1RoundToStep((long long)total, (long long)stepMs);
        }
        cn1PickerFinish((JAVA_LONG)total);
        return;
    }
    NSDate *date = self.datePicker.dateValue;
    // Milliseconds for every type, duration included. Picker.setDuration stores
    // hours*3600000 + minutes*60000 and Picker.getDuration hands that value back
    // untouched, so returning minutes turned 1h30m into 90 rather than
    // 5,400,000 -- every duration the user picked was corrupted.
    //
    // And read as an interval rather than through calendar components. A
    // duration picker is seeded with an offset from the epoch and pinned to GMT
    // (see the seeding below), so the interval IS the duration; reading hour and
    // minute back through the current time zone shifted it by the UTC offset,
    // which also meant the value shown when the popover opened was not the value
    // that went in.
    JAVA_LONG millis = (JAVA_LONG)llround([date timeIntervalSince1970] * 1000.0);
    // Types 2 and 3 are TIME and DATE_AND_TIME, 5 to 7 the durations -- every
    // type whose value carries minutes the step is meant to constrain. 3 was
    // omitted, so a date-and-time picker configured for quarter hours took 10:07
    // and returned it: the step was advertised and not enforced, which is the
    // same defect this snap exists to fix for TIME.
    if (self.minuteStep > 1
            && (self.pickerType == 2 || self.pickerType == 3
                || self.pickerType == 5 || self.pickerType == 6
                || self.pickerType == 7)) {
        // Snapped rather than configured: NSDatePicker has no minute-step
        // property, so the control lets the user land anywhere and the value is
        // rounded to the nearest allowed one on commit. Without this the
        // application's configured step was simply not enforced -- a picker set
        // to quarter hours committed 07 minutes quite happily.
        JAVA_LONG stepMs = (JAVA_LONG)self.minuteStep * 60000;
        millis = (JAVA_LONG)cn1RoundToStep((long long)millis, (long long)stepMs);
    }
    cn1PickerFinish(millis);
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
    // The duration fields are retain properties too, and were added to the
    // class without being added here -- so a duration picker leaked both of
    // them, and the stepper each one is bound to, every time it was opened.
    // Unbound first: the binding is what holds the stepper, and a value binding
    // left in place on a deallocated field is a dangling observer.
    [_durationHoursField unbind:NSValueBinding];
    [_durationMinutesField unbind:NSValueBinding];
    [_durationHoursField release];
    [_durationMinutesField release];
    [super dealloc];
}
#endif

@end

/// Builds the popover's content: the chooser, a Cancel and a Done.
/// One labelled number field with a stepper beside it.
static CGFloat cn1AddDurationField(NSView *parent, CGFloat x, NSTextField **outField,
                                   NSString *label, NSInteger value,
                                   NSInteger maximum, NSInteger step) {
    NSTextField *field = [[NSTextField alloc] initWithFrame:NSMakeRect(x, 26, 54, 22)];
    NSNumberFormatter *fmt = [[NSNumberFormatter alloc] init];
    fmt.numberStyle = NSNumberFormatterNoStyle;
    fmt.minimum = @0;
    fmt.maximum = @(maximum);
    fmt.allowsFloats = NO;
    field.formatter = fmt;
    field.alignment = NSTextAlignmentRight;
    field.integerValue = value;
    [parent addSubview:field];

    NSStepper *stepper = [[NSStepper alloc] initWithFrame:NSMakeRect(x + 56, 26, 15, 22)];
    stepper.minValue = 0;
    stepper.maxValue = (double)maximum;
    stepper.increment = (double)(step < 1 ? 1 : step);
    stepper.integerValue = value;
    stepper.valueWraps = NO;
    // Bound rather than wired through an action: the two stay in step in both
    // directions with no target, which is what a Mac stepper beside a field is.
    [field bind:NSValueBinding toObject:stepper withKeyPath:@"objectValue" options:nil];
    [parent addSubview:stepper];

    NSTextField *caption = [NSTextField labelWithString:label];
    caption.frame = NSMakeRect(x + 74, 29, 46, 17);
    [parent addSubview:caption];

    if (outField != NULL) {
        *outField = field;
    }
#ifndef CN1_USE_ARC
    [fmt release];
    [field release];
    [stepper release];
#endif
    return x + 124;
}

/// The duration chooser.
///
/// NSDatePicker has no duration mode, and the hour/minute elements it does have
/// are a CLOCK: they run 0..23 and wrap. A duration is a count -- the portable
/// spinner allows up to 999 hours -- so a 30 hour value displayed there showed
/// 6 and committed 6. Number fields with steppers are what a duration is.
static NSView *cn1BuildDurationView(CN1MacPickerController *controller, int type,
                                    long long millis, int minuteStep) {
    NSView *view = [[NSView alloc] initWithFrame:NSMakeRect(0, 0, 10, 56)];
    long long totalMinutes = millis / 60000LL;
    CGFloat x = 8;
    NSTextField *hoursField = nil;
    NSTextField *minutesField = nil;
    // 6 is hours only and 7 is minutes only; 5 carries both. The ranges are the
    // portable spinner's: whichever field stands alone counts up to 999, and
    // minutes beside hours are the 0..59 of an hour.
    if (type == 5 || type == 6) {
        // Truncated, never rounded -- including for the hours-only picker, which
        // is where rounding is most tempting because the minutes have nowhere to
        // show. Rounding made merely opening and accepting the picker change the
        // value: 1h30m opened on 2 hours and committed 2h with the user touching
        // nothing. The portable picker seeds its hour field with v / 1000 / 60 /
        // 60 for every duration type, so this is also what keeps the two showing
        // the same number for the same duration.
        long long hours = totalMinutes / 60;
        x = cn1AddDurationField(view, x, &hoursField, @"hours",
                                (NSInteger)MIN(hours, 999LL), 999, 1);
    }
    if (type == 5 || type == 7) {
        long long minutes = type == 7 ? totalMinutes : totalMinutes % 60;
        NSInteger maximum = type == 7 ? 999 : 59;
        x = cn1AddDurationField(view, x, &minutesField, @"min",
                                (NSInteger)MIN(minutes, (long long)maximum),
                                maximum, minuteStep < 1 ? 1 : minuteStep);
    }
    controller.durationHoursField = hoursField;
    controller.durationMinutesField = minutesField;
    view.frame = NSMakeRect(0, 0, x, 56);
#ifndef CN1_USE_ARC
    [view autorelease];
#endif
    return view;
}

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

    // Anchored in the window the picker was opened from, not always the main
    // one. A Picker inside a secondary Window supplies coordinates relative to
    // THAT window, so hard-coding the host's view opened the chooser over the
    // main window at the wrong place -- and could not present it at all when the
    // main window was hidden.
    NSView *host = CN1MacKeyRenderingHostView();
    CGFloat scale = CN1MacHostViewScale(host);
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
        controller.minuteStep = minuteStep > 0 ? minuteStep : 1;

        if (type == 5 || type == 6 || type == 7) {
            // A duration gets its own control. NSDatePicker's hour field is a
            // clock -- 0..23, wrapping -- and a duration is a count that the
            // portable spinner allows up to 999 hours, so anything past a day
            // could not be shown or chosen at all.
            NSView *duration = cn1BuildDurationView(controller, type, time,
                                                    controller.minuteStep);
            NSView *durationContent = cn1PickerContentView(controller, duration,
                                                           MAX(duration.frame.size.width + 16, 220),
                                                           duration.frame.size.height + 8);
            cn1PickerPresent(controller, durationContent, x, y, w, h);
            // Released like the date path below. The popover retains what it
            // presents, so these are the creator's own references -- and this
            // early return used to skip them, leaking a controller and a whole
            // view graph every time a duration picker was opened.
#ifndef CN1_USE_ARC
            [durationContent release];
            [controller release];
#endif
            POOL_END();
            return;
        }

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
        if (type == 5 || type == 6 || type == 7) {
            // A duration is an elapsed amount, not an instant, and NSDatePicker
            // has no duration mode -- so it edits an offset from the epoch. GMT
            // makes what is displayed and what is read back the same number; in
            // the local zone the picker opened showing the duration plus the UTC
            // offset and handed that back.
            NSTimeZone *gmt = [NSTimeZone timeZoneForSecondsFromGMT:0];
            NSCalendar *cal = [NSCalendar calendarWithIdentifier:NSCalendarIdentifierGregorian];
            cal.timeZone = gmt;
            picker.timeZone = gmt;
            picker.calendar = cal;
        }
        // Opened on an allowed value too, so the picker does not start on a
        // minute it will refuse to commit.
        //
        // Never for a date-only picker. It has no minute element for a step to
        // constrain, but rounding still moved the instant -- 23:59 with the
        // default five-minute step becomes 00:00 the NEXT DAY, and since the
        // control shows no time there is nothing on screen to reveal that the
        // date shifted. The user accepts a day they never chose.
        long long seeded = time;
        if (type != 1 && controller.minuteStep > 1) {
            long long stepMs = (long long)controller.minuteStep * 60000;
            seeded = (long long)cn1RoundToStep((long long)seeded, (long long)stepMs);
        }
        picker.dateValue = [NSDate dateWithTimeIntervalSince1970:seeded / 1000.0];
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
