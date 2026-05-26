#import <Foundation/Foundation.h>

@interface com_codename1_ai_tflite_NativeInterpreterImpl : NSObject {
}

-(NSData*)run:(NSData*)param param1:(NSData*)param1 param2:(int)param2;
-(BOOL)isSupported;
@end
