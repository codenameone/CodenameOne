#import <Foundation/Foundation.h>

@interface com_codenameone_examples_hellocodenameone_TestDiagnosticsNativeImpl : NSObject {
}

-(void)dumpNativeThreads:(NSString*)reason;
-(BOOL)isSupported;

@end
