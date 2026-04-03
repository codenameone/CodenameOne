#import "com_codenameone_examples_hellocodenameone_SwiftKotlinNativeImpl.h"
#import <objc/runtime.h>
#include <stdlib.h>

@implementation com_codenameone_examples_hellocodenameone_SwiftKotlinNativeImpl

-(id)getBridgeInstance {
    Class bridgeClass = NSClassFromString(@"CN1SwiftKotlinNativeBridge");
    if (bridgeClass == Nil) {
        unsigned int classCount = 0;
        Class *classList = objc_copyClassList(&classCount);
        NSString *targetName = @"CN1SwiftKotlinNativeBridge";
        NSString *dottedSuffix = [@".CN1SwiftKotlinNativeBridge" copy];
        for (unsigned int i = 0; i < classCount; i++) {
            NSString *runtimeName = [NSString stringWithUTF8String:class_getName(classList[i])];
            if ([runtimeName isEqualToString:targetName] || [runtimeName hasSuffix:dottedSuffix] || [runtimeName hasSuffix:targetName]) {
                bridgeClass = classList[i];
                NSLog(@"[CN1] Found Swift bridge class as %@", runtimeName);
                break;
            }
        }
        if (classList != NULL) {
            free(classList);
        }
    }
    if (bridgeClass == Nil) {
        NSLog(@"[CN1] Swift bridge class CN1SwiftKotlinNativeBridge was not found");
        return nil;
    }
    return [[bridgeClass alloc] init];
}

-(NSString*)implementationLanguage {
    id bridge = [self getBridgeInstance];
    if (bridge != nil && [bridge respondsToSelector:@selector(implementationLanguage)]) {
        return [bridge implementationLanguage];
    }
    return @"swift";
}

-(NSString*)diagnostics {
    id bridge = [self getBridgeInstance];
    if (bridge != nil && [bridge respondsToSelector:@selector(diagnostics)]) {
        return [bridge diagnostics];
    }
    return @"ios-swift-bridge-missing-using-objc-shim";
}

-(BOOL)isSupported {
    id bridge = [self getBridgeInstance];
    if (bridge != nil && [bridge respondsToSelector:@selector(isSupported)]) {
        return [bridge isSupported];
    }
    return YES;
}

@end
