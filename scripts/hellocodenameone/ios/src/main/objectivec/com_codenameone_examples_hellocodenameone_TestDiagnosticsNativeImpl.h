#import <Foundation/Foundation.h>

@interface com_codenameone_examples_hellocodenameone_TestDiagnosticsNativeImpl : NSObject {
}

-(void)dumpNativeThreads:(NSString*)reason;
-(void)failFastWithNativeThreadDump:(NSString*)reason;
-(BOOL)isSupported;

@end
