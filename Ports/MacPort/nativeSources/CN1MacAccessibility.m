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

#import "CN1MacAccessibility.h"
#import "CN1MacHost.h"
#import "cn1_globals.h"
#import "com_codename1_impl_ios_IOSImplementation.h"

/// One node of the semantics tree, as an accessibility element.
///
/// NSAccessibilityElement already stores role, label, value, frame and parent,
/// so this adds only what the framework needs to route an action back: the node
/// id, and the actions that node declared.
@interface CN1MacAccessibilityElement : NSAccessibilityElement
@property (nonatomic, assign) long long cn1NodeId;
@property (nonatomic, retain) NSArray *cn1Actions;
@end

static id CN1MacJSONValue(NSDictionary *node, NSString *key) {
    id value = [node objectForKey:key];
    return value == [NSNull null] ? nil : value;
}

/// The id of `wanted` if the node declares it and it is enabled, else nil.
static NSString *CN1MacActionId(NSArray *actions, NSString *wanted) {
    for (NSDictionary *action in actions) {
        if ([wanted isEqualToString:[action objectForKey:@"id"]]
                && ![[action objectForKey:@"enabled"] isEqual:@NO]) {
            return wanted;
        }
    }
    return nil;
}

static void CN1MacPerformAction(long long nodeId, NSString *actionId) {
    if (actionId == nil) {
        return;
    }
    struct ThreadLocalData *threadStateData = getThreadLocalData();
    com_codename1_impl_ios_IOSImplementation_performAccessibilityActionFromNative___long_java_lang_String_java_lang_String(
            threadStateData, (JAVA_LONG)nodeId,
            fromNSString(threadStateData, actionId), JAVA_NULL);
}

@implementation CN1MacAccessibilityElement

- (void)dealloc {
    [_cn1Actions release];
    [super dealloc];
}

/// Only the actions this node actually declares are advertised.
///
/// AppKit asks before offering: a rotor listing Press on something with nothing
/// to press is worse than not listing it, because VoiceOver then reports that
/// the action did nothing.
- (BOOL)isAccessibilitySelectorAllowed:(SEL)selector {
    if (selector == @selector(accessibilityPerformPress)) {
        return CN1MacActionId(self.cn1Actions, @"activate") != nil
                || CN1MacActionId(self.cn1Actions, @"focus") != nil;
    }
    // Increment and decrement also carry scrolling, which AccessibilityManager
    // declares for a List as scrollForward and scrollBackward. NSAccessibility
    // has no scroll action of its own -- the UIKit projection has
    // accessibilityScroll: and AppKit's element protocol simply does not -- so a
    // list that declared them offered VoiceOver nothing at all. Increment is the
    // nearest action the platform does have, and it is what the rotor presents
    // for stepping through a collection.
    if (selector == @selector(accessibilityPerformIncrement)) {
        return CN1MacActionId(self.cn1Actions, @"increment") != nil
                || CN1MacActionId(self.cn1Actions, @"scrollForward") != nil;
    }
    if (selector == @selector(accessibilityPerformDecrement)) {
        return CN1MacActionId(self.cn1Actions, @"decrement") != nil
                || CN1MacActionId(self.cn1Actions, @"scrollBackward") != nil;
    }
    if (selector == @selector(accessibilityPerformCancel)) {
        return CN1MacActionId(self.cn1Actions, @"dismiss") != nil;
    }
    return [super isAccessibilitySelectorAllowed:selector];
}

- (BOOL)accessibilityPerformPress {
    NSString *action = CN1MacActionId(self.cn1Actions, @"activate");
    if (action == nil) {
        action = CN1MacActionId(self.cn1Actions, @"focus");
    }
    if (action == nil) {
        return NO;
    }
    CN1MacPerformAction(self.cn1NodeId, action);
    return YES;
}

- (BOOL)accessibilityPerformIncrement {
    // increment first: a node that declares both is a stepper inside a scroller,
    // and the value is what the user reached for.
    NSString *action = CN1MacActionId(self.cn1Actions, @"increment");
    if (action == nil) {
        action = CN1MacActionId(self.cn1Actions, @"scrollForward");
    }
    if (action == nil) {
        return NO;
    }
    CN1MacPerformAction(self.cn1NodeId, action);
    return YES;
}

- (BOOL)accessibilityPerformDecrement {
    NSString *action = CN1MacActionId(self.cn1Actions, @"decrement");
    if (action == nil) {
        action = CN1MacActionId(self.cn1Actions, @"scrollBackward");
    }
    if (action == nil) {
        return NO;
    }
    CN1MacPerformAction(self.cn1NodeId, action);
    return YES;
}

- (BOOL)accessibilityPerformCancel {
    NSString *action = CN1MacActionId(self.cn1Actions, @"dismiss");
    if (action == nil) {
        return NO;
    }
    CN1MacPerformAction(self.cn1NodeId, action);
    return YES;
}

@end

/// The AppKit role closest to the framework's portable one.
///
/// AccessibilityRole is deliberately platform neutral, so a few of its members
/// have no AppKit counterpart and take the nearest honest one rather than being
/// dropped: a role nobody maps still keeps its label and value, which is what
/// the API documents a port should do. HEADING has no NSAccessibility role at
/// all, so it becomes static text and says "heading" in its role description --
/// that is what VoiceOver reads out.
static NSString *CN1MacRoleForNode(NSDictionary *node, NSString **roleDescription) {
    NSString *role = CN1MacJSONValue(node, @"role");
    *roleDescription = nil;
    // A heading level makes a node a heading whatever its role says, which is
    // how the UIKit projection reads it too: a Label marked as level 2 is a
    // heading, and matching only the HEADING role missed every one of them.
    if ([CN1MacJSONValue(node, @"headingLevel") intValue] > 0) {
        *roleDescription = @"heading";
        return NSAccessibilityStaticTextRole;
    }
    if (role == nil) {
        return NSAccessibilityUnknownRole;
    }
    if ([role isEqualToString:@"BUTTON"]) return NSAccessibilityButtonRole;
    if ([role isEqualToString:@"TOGGLE_BUTTON"] || [role isEqualToString:@"SWITCH"]) {
        return NSAccessibilityCheckBoxRole;
    }
    if ([role isEqualToString:@"CHECKBOX"]) return NSAccessibilityCheckBoxRole;
    if ([role isEqualToString:@"RADIO_BUTTON"]) return NSAccessibilityRadioButtonRole;
    if ([role isEqualToString:@"LINK"]) return NSAccessibilityLinkRole;
    if ([role isEqualToString:@"IMAGE"]) return NSAccessibilityImageRole;
    if ([role isEqualToString:@"STATIC_TEXT"]) return NSAccessibilityStaticTextRole;
    if ([role isEqualToString:@"TEXT_FIELD"] || [role isEqualToString:@"SEARCH_FIELD"]) {
        return NSAccessibilityTextFieldRole;
    }
    if ([role isEqualToString:@"SLIDER"]) return NSAccessibilitySliderRole;
    if ([role isEqualToString:@"SPIN_BUTTON"]) return NSAccessibilityIncrementorRole;
    if ([role isEqualToString:@"PROGRESS_BAR"]) return NSAccessibilityProgressIndicatorRole;
    if ([role isEqualToString:@"LIST"] || [role isEqualToString:@"TREE"]) {
        return NSAccessibilityOutlineRole;
    }
    if ([role isEqualToString:@"GRID"]) return NSAccessibilityTableRole;
    if ([role isEqualToString:@"ROW"] || [role isEqualToString:@"TREE_ITEM"]
            || [role isEqualToString:@"LIST_ITEM"]) {
        return NSAccessibilityRowRole;
    }
    if ([role isEqualToString:@"CELL"]) return NSAccessibilityCellRole;
    if ([role isEqualToString:@"COLUMN_HEADER"] || [role isEqualToString:@"ROW_HEADER"]) {
        return NSAccessibilityCellRole;
    }
    if ([role isEqualToString:@"TAB_LIST"]) return NSAccessibilityTabGroupRole;
    if ([role isEqualToString:@"TAB"]) return NSAccessibilityRadioButtonRole;
    if ([role isEqualToString:@"TAB_PANEL"] || [role isEqualToString:@"GENERIC"]) {
        return NSAccessibilityGroupRole;
    }
    if ([role isEqualToString:@"DIALOG"] || [role isEqualToString:@"ALERT"]) {
        return NSAccessibilityGroupRole;
    }
    if ([role isEqualToString:@"MENU"]) return NSAccessibilityMenuRole;
    if ([role isEqualToString:@"MENU_ITEM"]) return NSAccessibilityMenuItemRole;
    if ([role isEqualToString:@"TOOLBAR"]) return NSAccessibilityToolbarRole;
    if ([role isEqualToString:@"SCROLL_BAR"]) return NSAccessibilityScrollBarRole;
    if ([role isEqualToString:@"COMBO_BOX"]) return NSAccessibilityComboBoxRole;
    if ([role isEqualToString:@"HEADING"]) {
        *roleDescription = @"heading";
        return NSAccessibilityStaticTextRole;
    }
    return NSAccessibilityUnknownRole;
}

/// The spoken value: the node's own, plus the states that are part of what a
/// control currently IS rather than what it is called. Mirrors the UIKit port,
/// so the two read out the same sentence.
static NSString *CN1MacValueForNode(NSDictionary *node) {
    NSString *value = CN1MacJSONValue(node, @"value");
    NSDictionary *range = CN1MacJSONValue(node, @"range");
    if (value == nil && range != nil) {
        value = CN1MacJSONValue(range, @"text");
        if (value == nil) {
            value = [[range objectForKey:@"current"] stringValue];
        }
    }
    NSString *checked = CN1MacJSONValue(node, @"checked");
    if ([checked isEqualToString:@"CHECKED"]) {
        value = value == nil ? @"Checked" : [value stringByAppendingString:@", Checked"];
    } else if ([checked isEqualToString:@"UNCHECKED"]) {
        value = value == nil ? @"Unchecked" : [value stringByAppendingString:@", Unchecked"];
    } else if ([checked isEqualToString:@"MIXED"]) {
        // A real framework state, not a gap in the enum. Left out, a partially
        // selected control was indistinguishable from one with no checked state
        // at all -- VoiceOver said nothing either way. The UIKit projection
        // says "Mixed" and this now says the same word.
        value = value == nil ? @"Mixed" : [value stringByAppendingString:@", Mixed"];
    }
    id expanded = CN1MacJSONValue(node, @"expanded");
    if (expanded != nil) {
        NSString *state = [expanded boolValue] ? @"Expanded" : @"Collapsed";
        value = value == nil ? state : [value stringByAppendingFormat:@", %@", state];
    }
    id invalid = CN1MacJSONValue(node, @"invalid");
    if (invalid != nil && [invalid boolValue]) {
        value = value == nil ? @"Invalid" : [value stringByAppendingString:@", Invalid"];
    }
    return value;
}

/// Last announced text per live-region node, so a region is only spoken when it
/// actually changed rather than on every tree update.
static NSMutableDictionary *cn1MacLiveValues = nil;

void CN1MacAccessibilityUpdateTree(NSString *json, int changeType) {
    if (json == nil) {
        return;
    }
    NSData *data = [json dataUsingEncoding:NSUTF8StringEncoding];
    NSDictionary *tree = [NSJSONSerialization JSONObjectWithData:data options:0 error:nil];
    NSArray *nodes = [tree objectForKey:@"nodes"];
    if (nodes == nil) {
        return;
    }
    dispatch_async(dispatch_get_main_queue(), ^{
        NSView *container = [CN1MacHost sharedHost].renderingView;
        if (container == nil || container.window == nil) {
            return;
        }
        CGFloat scale = CN1MacHostViewScale(container);
        if (cn1MacLiveValues == nil) {
            cn1MacLiveValues = [[NSMutableDictionary alloc] init];
        }
        NSMutableArray *elements = [NSMutableArray arrayWithCapacity:[nodes count]];
        for (NSDictionary *node in nodes) {
            CN1MacAccessibilityElement *element = [[CN1MacAccessibilityElement alloc] init];
            NSNumber *nodeId = [node objectForKey:@"id"];
            element.cn1NodeId = [nodeId longLongValue];
            element.cn1Actions = CN1MacJSONValue(node, @"actions");

            NSString *roleDescription = nil;
            [element setAccessibilityRole:CN1MacRoleForNode(node, &roleDescription)];
            if (roleDescription != nil) {
                [element setAccessibilityRoleDescription:roleDescription];
            }
            [element setAccessibilityLabel:CN1MacJSONValue(node, @"label")];
            [element setAccessibilityValue:CN1MacValueForNode(node)];
            [element setAccessibilityIdentifier:CN1MacJSONValue(node, @"identifier")];
            // Help is AppKit's tooltip-shaped channel and the closest thing to
            // UIKit's hint. The validation error joins it rather than the label:
            // a label is what the control is called, and renaming a field to its
            // own error message is how a screen reader loses track of it.
            NSString *hint = CN1MacJSONValue(node, @"hint");
            NSString *error = CN1MacJSONValue(node, @"error");
            if (error != nil) {
                hint = hint == nil ? error : [NSString stringWithFormat:@"%@. %@", hint, error];
            }
            [element setAccessibilityHelp:hint];

            id enabled = CN1MacJSONValue(node, @"enabled");
            [element setAccessibilityEnabled:enabled == nil || [enabled boolValue]];
            id selected = CN1MacJSONValue(node, @"selected");
            if (selected != nil) {
                [element setAccessibilitySelected:[selected boolValue]];
            }

            NSArray *bounds = [node objectForKey:@"bounds"];
            if ([bounds count] == 4) {
                // The framework measures in its own device pixels from the top
                // left of the surface; NSAccessibility wants screen coordinates.
                // The view is flipped, so the framework's rectangle is already in
                // its coordinate space and only has to be scaled and converted
                // out -- doing the y flip by hand here would undo the one
                // isFlipped already did.
                NSRect inView = NSMakeRect([[bounds objectAtIndex:0] doubleValue] / scale,
                                           [[bounds objectAtIndex:1] doubleValue] / scale,
                                           [[bounds objectAtIndex:2] doubleValue] / scale,
                                           [[bounds objectAtIndex:3] doubleValue] / scale);
                NSRect inWindow = [container convertRect:inView toView:nil];
                [element setAccessibilityFrame:[container.window convertRectToScreen:inWindow]];
            }
            // Everything the node declares that is not one of the four standard
            // selectors, as a custom action.
            //
            // The UIKit projection does exactly this, and without it a node
            // declaring longPress, expand, collapse, a clipboard operation or an
            // application's own action ID offered VoiceOver nothing: the four
            // selectors above did not match it, and the superclass has never
            // heard of cn1Actions. The handler is what invokes it, so the id is
            // captured rather than looked up again later -- the tree is rebuilt
            // on every change and the array this came from will not be the one
            // in place when the user chooses it.
            NSMutableArray<NSAccessibilityCustomAction *> *custom = [NSMutableArray array];
            for (NSDictionary *action in element.cn1Actions) {
                NSString *actionId = [action objectForKey:@"id"];
                if (actionId == nil
                        || [[action objectForKey:@"enabled"] isEqual:@NO]
                        || [actionId isEqualToString:@"activate"]
                        || [actionId isEqualToString:@"focus"]
                        || [actionId isEqualToString:@"increment"]
                        || [actionId isEqualToString:@"decrement"]
                        || [actionId isEqualToString:@"dismiss"]
                        || [actionId isEqualToString:@"scrollForward"]
                        || [actionId isEqualToString:@"scrollBackward"]) {
                    continue;
                }
                NSString *name = CN1MacJSONValue(action, @"label");
                if (name == nil) {
                    name = actionId;
                }
                long long nodeId = element.cn1NodeId;
                NSString *captured = actionId;
                NSAccessibilityCustomAction *ca = [[NSAccessibilityCustomAction alloc]
                        initWithName:name handler:^BOOL {
                    CN1MacPerformAction(nodeId, captured);
                    return YES;
                }];
                [custom addObject:ca];
                [ca release];
            }
            if ([custom count] > 0) {
                [element setAccessibilityCustomActions:custom];
            }
            [element setAccessibilityParent:container];
            [elements addObject:element];
            [element release];

            NSString *live = CN1MacJSONValue(node, @"liveRegion");
            if (live != nil && ![live isEqualToString:@"OFF"]) {
                NSString *label = [element accessibilityLabel];
                NSString *value = [element accessibilityValue];
                NSString *spoken = [NSString stringWithFormat:@"%@|%@",
                        label == nil ? @"" : label, value == nil ? @"" : value];
                NSString *previous = [cn1MacLiveValues objectForKey:nodeId];
                if (previous != nil && ![previous isEqualToString:spoken]) {
                    NSString *text = label != nil ? label : value;
                    if (text != nil) {
                        NSAccessibilityPostNotificationWithUserInfo(container.window,
                                NSAccessibilityAnnouncementRequestedNotification,
                                @{ NSAccessibilityAnnouncementKey: text,
                                   NSAccessibilityPriorityKey: @(NSAccessibilityPriorityMedium) });
                    }
                }
                [cn1MacLiveValues setObject:spoken forKey:nodeId];
            }
        }
        // YES here, unlike the UIKit port which sets isAccessibilityElement NO on
        // its container. The two frameworks mean different things by the flag:
        // UIKit uses it to decide whether a view is a LEAF, so a container
        // exposing accessibilityElements must answer NO, while AppKit uses it to
        // decide whether the element is exposed to assistive technology at all --
        // answering NO here would hide the group and everything under it.
        [container setAccessibilityElement:YES];
        [container setAccessibilityRole:NSAccessibilityGroupRole];
        [container setAccessibilityChildren:elements];
        // 256 is the framework's "the screen itself changed" flag. VoiceOver has
        // no separate screen-changed notification the way UIKit does, so both
        // cases post a layout change; the screen case also drops the remembered
        // live-region text, because a region on the previous screen has nothing
        // to compare against and would otherwise announce itself once on arrival.
        if ((changeType & 256) != 0) {
            [cn1MacLiveValues removeAllObjects];
        }
        NSAccessibilityPostNotification(container, NSAccessibilityLayoutChangedNotification);
    });
}

#endif
