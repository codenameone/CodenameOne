#import "com_codenameone_examples_hellocodenameone_Base64NativeImpl.h"

@implementation com_codenameone_examples_hellocodenameone_Base64NativeImpl

-(NSString*)encodeUtf8:(NSString*)plainText {
    if (plainText == nil) {
        return nil;
    }
    NSData *data = [plainText dataUsingEncoding:NSUTF8StringEncoding];
    return [data base64EncodedStringWithOptions:0];
}

-(NSString*)decodeToUtf8:(NSString*)base64Text {
    if (base64Text == nil) {
        return nil;
    }
    NSData *data = [[NSData alloc] initWithBase64EncodedString:base64Text options:0];
    if (data == nil) {
        return nil;
    }
    return [[NSString alloc] initWithData:data encoding:NSUTF8StringEncoding];
}

-(BOOL)isSupported {
    return YES;
}

@end
