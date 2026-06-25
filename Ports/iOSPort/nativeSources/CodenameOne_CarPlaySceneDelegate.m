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
// Import the header first (it pulls in CodenameOne_GLViewController.h which carries the
// CN1_USE_CARPLAY define) so the guard below evaluates correctly in this translation unit.
#import "CodenameOne_CarPlaySceneDelegate.h"
#ifdef CN1_USE_CARPLAY
#include "xmlvm.h"
#include "cn1_globals.h"
#include "com_codename1_impl_ios_IOSCarPlayCallbacks.h"

// Bridges a CarPlay selection back to Java (IOSCarPlayCallbacks.nativeElementSelected).
static void cn1CarPlayFireSelect(int screenId, NSString *elementId) {
    if (elementId == nil) {
        return;
    }
    JAVA_OBJECT jel = xmlvm_create_java_string([elementId UTF8String]);
    com_codename1_impl_ios_IOSCarPlayCallbacks_nativeElementSelected___int_java_lang_String(
            getThreadLocalData(), (JAVA_INT)screenId, jel);
}

@implementation CN1CarPlayManager {
    NSMutableDictionary<NSString *, UIImage *> *_imageTable;
}

+ (instancetype)sharedManager {
    static CN1CarPlayManager *instance = nil;
    static dispatch_once_t once;
    dispatch_once(&once, ^{
        instance = [[CN1CarPlayManager alloc] init];
    });
    return instance;
}

- (instancetype)init {
    self = [super init];
    if (self) {
        _imageTable = [NSMutableDictionary dictionary];
    }
    return self;
}

- (void)didConnect:(CPInterfaceController *)controller {
    self.interfaceController = controller;
    self.connected = YES;
    com_codename1_impl_ios_IOSCarPlayCallbacks_nativeCarConnected__(getThreadLocalData());
}

- (void)didDisconnect {
    self.connected = NO;
    self.interfaceController = nil;
    [_imageTable removeAllObjects];
    com_codename1_impl_ios_IOSCarPlayCallbacks_nativeCarDisconnected__(getThreadLocalData());
}

- (void)registerImage:(NSString *)key data:(NSData *)data {
    if (key == nil || data == nil) {
        return;
    }
    UIImage *img = [UIImage imageWithData:data];
    if (img != nil) {
        _imageTable[key] = img;
    }
}

- (UIImage *)imageForKey:(id)key {
    if (![key isKindOfClass:[NSString class]]) {
        return nil;
    }
    return _imageTable[(NSString *)key];
}

- (void)showToast:(NSString *)message seconds:(int)seconds {
    if (self.interfaceController == nil || message == nil) {
        return;
    }
    CPAlertAction *ok = [[CPAlertAction alloc] initWithTitle:@"OK"
                                                       style:CPAlertActionStyleDefault
                                                     handler:^(CPAlertAction *a) {}];
    CPActionSheetTemplate *sheet = [[CPActionSheetTemplate alloc] initWithTitle:nil
                                                                        message:message
                                                                        actions:@[ok]];
    [self.interfaceController presentTemplate:sheet animated:YES completion:nil];
}

- (void)setTemplate:(int)screenId json:(NSString *)json isRoot:(BOOL)isRoot {
    CPTemplate *t = [self buildTemplate:screenId json:json];
    if (t == nil || self.interfaceController == nil) {
        return;
    }
    if (isRoot) {
        [self.interfaceController setRootTemplate:t animated:YES completion:nil];
    } else {
        [self.interfaceController pushTemplate:t animated:YES completion:nil];
    }
}

- (void)updateTemplate:(int)screenId json:(NSString *)json {
    // CarPlay templates are largely immutable once pushed; the portable contract for
    // CarScreen.invalidate() is "rebuild and show the latest". Replacing the top template is the
    // closest faithful behaviour for the common case (the top screen invalidating itself).
    CPTemplate *t = [self buildTemplate:screenId json:json];
    if (t == nil || self.interfaceController == nil) {
        return;
    }
    NSArray<CPTemplate *> *stack = self.interfaceController.templates;
    if (stack.count <= 1) {
        [self.interfaceController setRootTemplate:t animated:NO completion:nil];
    } else {
        [self.interfaceController popTemplateAnimated:NO completion:^(BOOL done, NSError *err) {
            [self.interfaceController pushTemplate:t animated:NO completion:nil];
        }];
    }
}

- (void)popTemplate {
    if (self.interfaceController == nil) {
        return;
    }
    [self.interfaceController popTemplateAnimated:YES completion:nil];
}

// --- JSON -> CPTemplate ---------------------------------------------------

- (CPTemplate *)buildTemplate:(int)screenId json:(NSString *)json {
    if (json == nil) {
        return nil;
    }
    NSData *data = [json dataUsingEncoding:NSUTF8StringEncoding];
    NSError *err = nil;
    NSDictionary *d = [NSJSONSerialization JSONObjectWithData:data options:0 error:&err];
    if (![d isKindOfClass:[NSDictionary class]]) {
        return nil;
    }
    NSString *type = d[@"type"];
    if ([type isEqualToString:@"list"]) {
        return [self buildList:screenId dict:d];
    } else if ([type isEqualToString:@"grid"]) {
        return [self buildGrid:screenId dict:d];
    } else if ([type isEqualToString:@"pane"] || [type isEqualToString:@"message"]) {
        return [self buildInformation:screenId dict:d];
    } else if ([type isEqualToString:@"nowplaying"]) {
        return [CPNowPlayingTemplate sharedTemplate];
    } else if ([type isEqualToString:@"navigation"]) {
        return [self buildNavigation:screenId dict:d];
    }
    return nil;
}

- (NSString *)str:(id)v {
    return [v isKindOfClass:[NSString class]] ? (NSString *)v : nil;
}

- (CPListTemplate *)buildList:(int)screenId dict:(NSDictionary *)d {
    NSMutableArray<CPListSection *> *sections = [NSMutableArray array];
    for (NSDictionary *sec in d[@"sections"]) {
        NSMutableArray<CPListItem *> *items = [NSMutableArray array];
        for (NSDictionary *row in sec[@"rows"]) {
            NSString *title = [self str:row[@"title"]];
            NSString *detail = [self str:row[@"text"]];
            UIImage *img = [self imageForKey:row[@"image"]];
            CPListItem *item = [[CPListItem alloc] initWithText:(title ?: @"")
                                                     detailText:detail
                                                          image:img];
            NSString *elementId = [self str:row[@"id"]];
            int sid = screenId;
            item.handler = ^(id<CPSelectableListItem> it, dispatch_block_t completion) {
                cn1CarPlayFireSelect(sid, elementId);
                if (completion) {
                    completion();
                }
            };
            [items addObject:item];
        }
        CPListSection *section = [[CPListSection alloc] initWithItems:items
                                                              header:[self str:sec[@"header"]]
                                                       sectionIndexTitle:nil];
        [sections addObject:section];
    }
    CPListTemplate *t = [[CPListTemplate alloc] initWithTitle:[self str:d[@"title"]]
                                                     sections:sections];
    t.leadingNavigationBarButtons = [self barButtons:screenId list:d[@"headerActions"]];
    return t;
}

- (CPGridTemplate *)buildGrid:(int)screenId dict:(NSDictionary *)d {
    NSMutableArray<CPGridButton *> *buttons = [NSMutableArray array];
    for (NSDictionary *it in d[@"items"]) {
        UIImage *img = [self imageForKey:it[@"image"]];
        if (img == nil) {
            // CPGridButton requires a non-nil image; skip image-less items to avoid a runtime throw.
            continue;
        }
        NSString *title = [self str:it[@"title"]] ?: @"";
        NSString *elementId = [self str:it[@"id"]];
        int sid = screenId;
        CPGridButton *b = [[CPGridButton alloc] initWithTitleVariants:@[title]
                                                                image:img
                                                              handler:^(CPGridButton *gb) {
            cn1CarPlayFireSelect(sid, elementId);
        }];
        [buttons addObject:b];
    }
    CPGridTemplate *t = [[CPGridTemplate alloc] initWithTitle:[self str:d[@"title"]]
                                                  gridButtons:buttons];
    return t;
}

- (CPInformationTemplate *)buildInformation:(int)screenId dict:(NSDictionary *)d {
    NSMutableArray<CPInformationItem *> *items = [NSMutableArray array];
    NSString *message = [self str:d[@"message"]];
    if (message != nil) {
        [items addObject:[[CPInformationItem alloc] initWithTitle:nil detail:message]];
    }
    for (NSDictionary *row in d[@"rows"]) {
        [items addObject:[[CPInformationItem alloc] initWithTitle:[self str:row[@"title"]]
                                                           detail:[self str:row[@"text"]]]];
    }
    NSMutableArray<CPTextButton *> *actions = [NSMutableArray array];
    for (NSDictionary *a in d[@"actions"]) {
        NSString *title = [self str:a[@"title"]] ?: @"";
        NSString *elementId = [self str:a[@"id"]];
        int sid = screenId;
        CPTextButton *b = [[CPTextButton alloc] initWithTitle:title
                                                    textStyle:CPTextButtonStyleNormal
                                                      handler:^(CPTextButton *tb) {
            cn1CarPlayFireSelect(sid, elementId);
        }];
        [actions addObject:b];
    }
    CPInformationTemplate *t = [[CPInformationTemplate alloc] initWithTitle:([self str:d[@"title"]] ?: @"")
                                                                    layout:CPInformationTemplateLayoutLeading
                                                                     items:items
                                                                   actions:actions];
    return t;
}

- (CPMapTemplate *)buildNavigation:(int)screenId dict:(NSDictionary *)d {
    // Note: the moving-map pixel surface (CarSurfaceCallback) requires the CarPlay map window
    // provided by the scene's root view controller and is not yet blitted here; the controls,
    // header and ETA strip below are wired. A full map surface is a follow-on native task.
    CPMapTemplate *t = [[CPMapTemplate alloc] init];
    t.leadingNavigationBarButtons = [self barButtons:screenId list:d[@"headerActions"]];
    NSMutableArray<CPMapButton *> *mapButtons = [NSMutableArray array];
    for (NSDictionary *a in d[@"mapActions"]) {
        NSString *elementId = [self str:a[@"id"]];
        int sid = screenId;
        CPMapButton *b = [[CPMapButton alloc] initWithHandler:^(CPMapButton *mb) {
            cn1CarPlayFireSelect(sid, elementId);
        }];
        b.image = [self imageForKey:a[@"icon"]];
        [mapButtons addObject:b];
    }
    t.mapButtons = mapButtons;
    return t;
}

- (NSArray<CPBarButton *> *)barButtons:(int)screenId list:(NSArray *)actions {
    if (![actions isKindOfClass:[NSArray class]]) {
        return @[];
    }
    NSMutableArray<CPBarButton *> *result = [NSMutableArray array];
    for (NSDictionary *a in actions) {
        NSString *title = [self str:a[@"title"]] ?: @"";
        NSString *elementId = [self str:a[@"id"]];
        int sid = screenId;
        CPBarButton *b = [[CPBarButton alloc] initWithTitle:title handler:^(CPBarButton *bb) {
            cn1CarPlayFireSelect(sid, elementId);
        }];
        [result addObject:b];
    }
    return result;
}

@end

@implementation CodenameOne_CarPlaySceneDelegate

- (void)templateApplicationScene:(CPTemplateApplicationScene *)templateApplicationScene
    didConnectInterfaceController:(CPInterfaceController *)interfaceController {
    [[CN1CarPlayManager sharedManager] didConnect:interfaceController];
}

- (void)templateApplicationScene:(CPTemplateApplicationScene *)templateApplicationScene
 didDisconnectInterfaceController:(CPInterfaceController *)interfaceController {
    [[CN1CarPlayManager sharedManager] didDisconnect];
}

@end

#endif // CN1_USE_CARPLAY
