#import <TargetConditionals.h>
#import <AVFoundation/AVFoundation.h>
#import <QuartzCore/QuartzCore.h>
#if TARGET_OS_OSX
#import <AppKit/AppKit.h>
typedef NSView CN1View;
#else
#import <UIKit/UIKit.h>
typedef UIView CN1View;
#endif
#import "PreviewProduction.inc"

static void checkLayout(void) {
    CN1View *container = [[CN1View alloc] initWithFrame:CGRectMake(0, 0, 900, 900)];
    CN1CameraPreviewView *view = [[CN1CameraPreviewView alloc] initWithFrame:CGRectMake(0, 0, 320, 480)];
    [container addSubview:view];
#if TARGET_OS_OSX
    view.wantsLayer = YES;
#endif
    AVCaptureVideoPreviewLayer *preview = [[AVCaptureVideoPreviewLayer alloc] init];
    preview.frame = view.bounds;
    [view.layer addSublayer:preview];
    CALayer *other = [CALayer layer];
    other.frame = CGRectMake(2, 3, 4, 5);
    [view.layer addSublayer:other];
    CGSize sizes[] = {{393, 734}, {734, 393}, {210, 160}, {0, 0}, {430, 800}};
    for (int i = 0; i < 5; i++) {
        view.frame = CGRectMake(12, 24, sizes[i].width, sizes[i].height);
#if TARGET_OS_OSX
        [view setNeedsLayout:YES];
        [view layoutSubtreeIfNeeded];
#else
        [view setNeedsLayout];
        [view layoutIfNeeded];
#endif
        if (!CGRectEqualToRect(preview.frame, view.bounds)) {
            fprintf(stderr, "FAIL: preview does not follow layout %d\n", i);
            exit(1);
        }
        if (!CGRectEqualToRect(other.frame, CGRectMake(2, 3, 4, 5))) {
            fprintf(stderr, "FAIL: unrelated sublayer resized\n");
            exit(1);
        }
    }
    fprintf(stdout, "PASS: camera preview portrait, landscape, embedded, zero and restored bounds\n");
    fflush(stdout);
}

#if TARGET_OS_OSX
int main(void) {
    @autoreleasepool { checkLayout(); }
    return 0;
}
#else
@interface TestSceneDelegate : UIResponder <UIWindowSceneDelegate>
@property(strong, nonatomic) UIWindow *window;
@end
@implementation TestSceneDelegate
- (void)scene:(UIScene *)scene willConnectToSession:(UISceneSession *)session
      options:(UISceneConnectionOptions *)options {
    self.window = [[UIWindow alloc] initWithWindowScene:(UIWindowScene *)scene];
    self.window.rootViewController = [[UIViewController alloc] init];
    [self.window makeKeyAndVisible];
    dispatch_async(dispatch_get_main_queue(), ^{ checkLayout(); exit(0); });
}
@end
@interface TestDelegate : UIResponder <UIApplicationDelegate>
@end
@implementation TestDelegate
- (UISceneConfiguration *)application:(UIApplication *)application
    configurationForConnectingSceneSession:(UISceneSession *)session
    options:(UISceneConnectionOptions *)options {
    UISceneConfiguration *config = [[UISceneConfiguration alloc] initWithName:@"Test" sessionRole:session.role];
    config.delegateClass = [TestSceneDelegate class];
    return config;
}
@end
int main(int argc, char **argv) {
    @autoreleasepool { return UIApplicationMain(argc, argv, nil, @"TestDelegate"); }
}
#endif
