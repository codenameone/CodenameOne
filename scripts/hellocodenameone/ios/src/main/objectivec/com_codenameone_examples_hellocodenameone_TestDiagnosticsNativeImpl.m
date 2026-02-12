#import "com_codenameone_examples_hellocodenameone_TestDiagnosticsNativeImpl.h"

@implementation com_codenameone_examples_hellocodenameone_TestDiagnosticsNativeImpl

-(void)dumpNativeThreads:(NSString*)reason {
    @try {
        NSString *label = reason != nil ? reason : @"unspecified";
        NSLog(@"CN1SS:NATIVE:THREAD_DUMP:BEGIN reason=%@", label);
        NSLog(@"CN1SS:NATIVE:THREAD_DUMP:current=%@ isMain=%@", [NSThread currentThread], [NSThread isMainThread] ? @"true" : @"false");
        NSArray *symbols = [NSThread callStackSymbols];
        for (NSString *line in symbols) {
            NSLog(@"CN1SS:NATIVE:STACK:%@", line);
        }
        NSLog(@"CN1SS:NATIVE:THREAD_DUMP:END reason=%@", label);
    } @catch (NSException *ex) {
        NSLog(@"CN1SS:NATIVE:THREAD_DUMP:ERROR reason=%@ exception=%@", reason, ex);
    }
}

-(void)failFastWithNativeThreadDump:(NSString*)reason {
    NSString *label = reason != nil ? reason : @"unspecified";
    NSLog(@"CN1SS:NATIVE:FAIL_FAST:BEGIN reason=%@", label);
    [self dumpNativeThreads:label];
    NSLog(@"CN1SS:NATIVE:FAIL_FAST:ABORT reason=%@", label);
    abort();
}

-(BOOL)isSupported{
    return YES;
}

@end
