#import <Foundation/Foundation.h>

@interface com_codenameone_examples_hellocodenameone_Base64NativeImpl : NSObject {
}

-(NSString*)encodeUtf8:(NSString*)plainText;
-(NSString*)decodeToUtf8:(NSString*)base64Text;
-(BOOL)isSupported;

@end
