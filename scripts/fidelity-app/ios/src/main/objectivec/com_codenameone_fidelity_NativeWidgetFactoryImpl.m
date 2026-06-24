#import "com_codenameone_fidelity_NativeWidgetFactoryImpl.h"

// iOS side of NativeWidgetFactory: build a REAL UIKit widget, lay it out centered
// in a fixed w x h tile, and rasterize it off-screen to PNG bytes (returned as
// NSData, which the CN1 bridge maps to a Java byte[]). Off-screen rendering via
// -[CALayer renderInContext:] is synchronous and Metal/compositor-independent,
// so it works reliably on the simulator regardless of the rendering backend.

@implementation com_codenameone_fidelity_NativeWidgetFactoryImpl

-(BOOL)isSupported {
    return YES;
}

-(BOOL)isWidgetSupported:(NSString*)kind {
    return [self knownKind:kind];
}

-(BOOL)knownKind:(NSString*)kind {
    static NSSet* kinds = nil;
    if (kinds == nil) {
        kinds = [NSSet setWithObjects:
                 @"ios_uibutton_system", @"ios_uibutton_filled", @"ios_uibutton_plain",
                 @"ios_uitextfield", @"ios_check_glyph", @"ios_radio_glyph",
                 @"ios_uiswitch", @"ios_uislider", @"ios_uiprogress",
                 @"ios_uitabbar", @"ios_uinavbar", nil];
    }
    return [kinds containsObject:kind];
}

-(BOOL)renderWidgetToFile:(NSString*)kind param1:(NSString*)state param2:(NSString*)appearance param3:(NSString*)text param4:(NSString*)outPath param5:(int)widthPx param6:(int)heightPx {
    NSLog(@"CN1SS:NATIVE enter kind=%@ state=%@ appearance=%@ w=%d h=%d main=%d out=%@",
          kind, state, appearance, widthPx, heightPx, (int)[NSThread isMainThread], outPath);
    @try {
        if (![self knownKind:kind] || widthPx <= 0 || heightPx <= 0 || outPath == nil) {
            NSLog(@"CN1SS:NATIVE reject kind=%@ known=%d w=%d h=%d out=%@", kind, (int)[self knownKind:kind], widthPx, heightPx, outPath);
            return NO;
        }
        // KNOWN BLOCKER (ParparVM): this native method renders correctly only as a
        // trivial stub. As soon as it does real UIKit work (buildAndRender:
        // UIButton/UISwitch construction + CALayer renderInContext), a
        // java.lang.NullPointerException surfaces on the Java side after the
        // method's NSLog "enter" but before "done" -- with NO Objective-C exception
        // raised. It reproduces identically whether the UIKit build runs via
        // dispatch_sync to the main queue OR directly on the calling thread, so it
        // is neither a threading nor an argument/return-marshaling fault (String
        // args + BOOL return marshal cleanly; the stub delivers). The signature is
        // that substantial ObjC work inside a ParparVM native method (which is not
        // a GC safepoint and allocates UIKit objects the concurrent GC then scans)
        // corrupts the thread state. Resolving it needs a ParparVM runtime fix, or
        // a redesign that renders the native reference via a PeerComponent +
        // Display.screenshot() (normal main-loop flow) instead of a native method.
        // Until then the iOS fidelity round cannot collect native references.
        NSData* result = nil;
        @try {
            result = [self buildAndRender:kind state:state appearance:appearance text:text w:widthPx h:heightPx];
        } @catch (NSException* ex) {
            NSLog(@"CN1SS:NATIVE exception kind=%@ : %@", kind, ex);
        }
        if (result == nil) {
            NSLog(@"CN1SS:NATIVE nil-result kind=%@ -> fallback", kind);
            result = [self fallbackPng:widthPx h:heightPx];
        }
        NSString* fsPath = outPath;
        if ([fsPath hasPrefix:@"file://"]) {
            fsPath = [[NSURL URLWithString:fsPath] path];
        }
        BOOL ok = [result writeToFile:fsPath atomically:YES];
        NSLog(@"CN1SS:NATIVE done kind=%@ bytes=%lu wrote=%d path=%@", kind, (unsigned long)result.length, (int)ok, fsPath);
        return ok;
    } @catch (id ex) {
        NSLog(@"CN1SS:NATIVE objc-exception kind=%@ : %@", kind, ex);
        return NO;
    }
}

// Never return nil to the bridge (nsDataToByteArr(nil) NPEs on the Java side):
// a solid-color tile is a visible, diff-able placeholder when a widget fails.
-(NSData*)fallbackPng:(int)w h:(int)h {
    int ww = w > 0 ? w : 2;
    int hh = h > 0 ? h : 2;
    UIGraphicsImageRendererFormat* fmt = [UIGraphicsImageRendererFormat defaultFormat];
    fmt.scale = 1.0;
    fmt.opaque = YES;
    UIGraphicsImageRenderer* r = [[UIGraphicsImageRenderer alloc] initWithSize:CGSizeMake(ww, hh) format:fmt];
    return [r PNGDataWithActions:^(UIGraphicsImageRendererContext* ctx) {
        [[UIColor magentaColor] setFill];
        UIRectFill(CGRectMake(0, 0, ww, hh));
    }];
}

-(NSData*)buildAndRender:(NSString*)kind state:(NSString*)state appearance:(NSString*)appearance text:(NSString*)text w:(int)w h:(int)h {
    BOOL dark = [@"dark" isEqualToString:appearance];
    UIView* tile = [[UIView alloc] initWithFrame:CGRectMake(0, 0, w, h)];
    tile.backgroundColor = dark ? [UIColor blackColor] : [UIColor whiteColor];
    if (@available(iOS 13.0, *)) {
        tile.overrideUserInterfaceStyle = dark ? UIUserInterfaceStyleDark : UIUserInterfaceStyleLight;
    }

    UIView* control = [self buildControl:kind state:state text:text w:w h:h];
    if (control == nil) {
        return nil;
    }
    // Anchor the control TOP-LEFT at its natural size within the tile, matching
    // the CN1 side (which anchors its component top-left at preferred size in an
    // identically sized tile), so the two renders are directly comparable.
    [control sizeToFit];
    CGSize cs = control.bounds.size;
    if (cs.width <= 0 || cs.width > w) { cs.width = w; }
    if (cs.height <= 0 || cs.height > h) { cs.height = h; }
    control.frame = CGRectMake(0, 0, cs.width, cs.height);
    [tile addSubview:control];
    [tile layoutIfNeeded];

    UIGraphicsImageRendererFormat* fmt = [UIGraphicsImageRendererFormat defaultFormat];
    fmt.scale = 1.0;     // 1 point == 1 pixel, so the PNG is exactly w x h pixels
    fmt.opaque = YES;
    UIGraphicsImageRenderer* renderer = [[UIGraphicsImageRenderer alloc] initWithSize:CGSizeMake(w, h) format:fmt];
    NSData* png = [renderer PNGDataWithActions:^(UIGraphicsImageRendererContext* ctx) {
        [tile.layer renderInContext:ctx.CGContext];
    }];
    return png;
}

-(UIView*)buildControl:(NSString*)kind state:(NSString*)state text:(NSString*)text w:(int)w h:(int)h {
    BOOL disabled = [@"disabled" isEqualToString:state];
    BOOL pressed = [@"pressed" isEqualToString:state];
    BOOL selected = [@"selected" isEqualToString:state];
    NSString* label = text != nil ? text : @"";

    if ([@"ios_uibutton_system" isEqualToString:kind]) {
        UIButton* b = [UIButton buttonWithType:UIButtonTypeSystem];
        [b setTitle:label forState:UIControlStateNormal];
        b.enabled = !disabled;
        b.highlighted = pressed;
        return b;
    }
    if ([@"ios_uibutton_filled" isEqualToString:kind]) {
        UIButton* b;
        if (@available(iOS 15.0, *)) {
            UIButtonConfiguration* cfg = [UIButtonConfiguration filledButtonConfiguration];
            cfg.title = label;
            b = [UIButton buttonWithConfiguration:cfg primaryAction:nil];
        } else {
            b = [UIButton buttonWithType:UIButtonTypeSystem];
            [b setTitle:label forState:UIControlStateNormal];
            b.backgroundColor = [UIColor systemBlueColor];
        }
        b.enabled = !disabled;
        b.highlighted = pressed;
        return b;
    }
    if ([@"ios_uibutton_plain" isEqualToString:kind]) {
        UIButton* b = [UIButton buttonWithType:UIButtonTypeSystem];
        [b setTitle:label forState:UIControlStateNormal];
        b.enabled = !disabled;
        b.highlighted = pressed;
        return b;
    }
    if ([@"ios_uitextfield" isEqualToString:kind]) {
        UITextField* tf = [[UITextField alloc] initWithFrame:CGRectMake(0, 0, w, h)];
        tf.borderStyle = UITextBorderStyleRoundedRect;
        tf.text = label;
        tf.enabled = !disabled;
        return tf;
    }
    if ([@"ios_check_glyph" isEqualToString:kind]) {
        // iOS has no native checkbox; the closest analogue is an SF Symbol.
        UIButton* b = [UIButton buttonWithType:UIButtonTypeSystem];
        if (@available(iOS 13.0, *)) {
            NSString* sym = selected ? @"checkmark.circle.fill" : @"circle";
            [b setImage:[UIImage systemImageNamed:sym] forState:UIControlStateNormal];
        }
        b.enabled = !disabled;
        return b;
    }
    if ([@"ios_radio_glyph" isEqualToString:kind]) {
        UIButton* b = [UIButton buttonWithType:UIButtonTypeSystem];
        if (@available(iOS 13.0, *)) {
            NSString* sym = selected ? @"largecircle.fill.circle" : @"circle";
            [b setImage:[UIImage systemImageNamed:sym] forState:UIControlStateNormal];
        }
        b.enabled = !disabled;
        return b;
    }
    if ([@"ios_uiswitch" isEqualToString:kind]) {
        UISwitch* sw = [[UISwitch alloc] init];
        [sw setOn:selected];
        sw.enabled = !disabled;
        return sw;
    }
    if ([@"ios_uislider" isEqualToString:kind]) {
        UISlider* s = [[UISlider alloc] initWithFrame:CGRectMake(0, 0, w, h)];
        s.minimumValue = 0;
        s.maximumValue = 100;
        s.value = 50;
        s.enabled = !disabled;
        return s;
    }
    if ([@"ios_uiprogress" isEqualToString:kind]) {
        UIProgressView* p = [[UIProgressView alloc] initWithProgressViewStyle:UIProgressViewStyleDefault];
        p.frame = CGRectMake(0, 0, w, h);
        p.progress = 0.5;
        return p;
    }
    if ([@"ios_uitabbar" isEqualToString:kind]) {
        UITabBar* bar = [[UITabBar alloc] initWithFrame:CGRectMake(0, 0, w, h)];
        UITabBarItem* a = [[UITabBarItem alloc] initWithTabBarSystemItem:UITabBarSystemItemFeatured tag:0];
        UITabBarItem* b = [[UITabBarItem alloc] initWithTabBarSystemItem:UITabBarSystemItemSearch tag:1];
        UITabBarItem* c = [[UITabBarItem alloc] initWithTabBarSystemItem:UITabBarSystemItemMore tag:2];
        bar.items = @[a, b, c];
        bar.selectedItem = a;
        return bar;
    }
    if ([@"ios_uinavbar" isEqualToString:kind]) {
        UINavigationBar* nav = [[UINavigationBar alloc] initWithFrame:CGRectMake(0, 0, w, h)];
        UINavigationItem* item = [[UINavigationItem alloc] initWithTitle:label];
        nav.items = @[item];
        return nav;
    }
    return nil;
}

@end
