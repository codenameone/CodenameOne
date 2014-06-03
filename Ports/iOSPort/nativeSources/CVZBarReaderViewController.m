#import "CVZBarReaderViewController.h"
#if !TARGET_IPHONE_SIMULATOR

@implementation CVZBarReaderViewController 
- (void) loadView
{
#ifdef CN1_USE_ARC
    self.view = [[UIView alloc] initWithFrame: CGRectMake(0, 0, 320, 480)];
#else
    self.view = [[[UIView alloc] initWithFrame: CGRectMake(0, 0, 320, 480)] autorelease];
#endif
}
@end
#endif
