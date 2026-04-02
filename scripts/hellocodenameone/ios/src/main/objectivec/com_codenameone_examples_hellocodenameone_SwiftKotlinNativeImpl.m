#import "com_codenameone_examples_hellocodenameone_SwiftKotlinNativeImpl.h"
#import "HelloCodenameOne-Swift.h"

@implementation com_codenameone_examples_hellocodenameone_SwiftKotlinNativeImpl

-(id)getBridgeInstance {
    return [[CN1SwiftKotlinNativeBridge alloc] init];
}

-(NSString*)implementationLanguage {
    id bridge = [self getBridgeInstance];
    if (bridge != nil && [bridge respondsToSelector:@selector(implementationLanguage)]) {
        return [bridge implementationLanguage];
    }
    return @"swift-bridge-missing";
}

-(NSString*)diagnostics {
    id bridge = [self getBridgeInstance];
    if (bridge != nil && [bridge respondsToSelector:@selector(diagnostics)]) {
        return [bridge diagnostics];
    }
    return @"ios-swift-bridge-missing";
}

-(BOOL)isSupported {
    id bridge = [self getBridgeInstance];
    if (bridge != nil && [bridge respondsToSelector:@selector(isSupported)]) {
        return [bridge isSupported];
    }
    return NO;
}

@end
