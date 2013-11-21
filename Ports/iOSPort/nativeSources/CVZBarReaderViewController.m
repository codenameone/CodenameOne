#import "CVZBarReaderViewController.h"
#if !TARGET_IPHONE_SIMULATOR

@implementation CVZBarReaderViewController 
- (void) loadView
{
    self.view = [[[UIView alloc] initWithFrame: CGRectMake(0, 0, 320, 480)] autorelease];
}
@end
#endif