#import "com_codenameone_examples_hellocodenameone_StatusBarTapDiagnosticNativeImpl.h"

extern void cn1FireStatusBarTap();
extern int cn1GetStatusBarTapCount();

@implementation com_codenameone_examples_hellocodenameone_StatusBarTapDiagnosticNativeImpl

-(BOOL)simulateStatusBarTap {
    cn1FireStatusBarTap();
    return YES;
}

-(int)getTapCount {
    return cn1GetStatusBarTapCount();
}

-(BOOL)isSupported {
    return YES;
}

@end
