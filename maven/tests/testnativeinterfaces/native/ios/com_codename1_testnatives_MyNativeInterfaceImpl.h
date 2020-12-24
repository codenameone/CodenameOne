#import <Foundation/Foundation.h>

@interface com_codename1_testnatives_MyNativeInterfaceImpl : NSObject {

}

-(NSData*)getDouble;
-(NSData*)getBytes;
-(NSData*)setInts:(NSData*)param;
-(NSData)setDoubles:(NSData*)param;
-(NSData*)getInts;
-(NSData)setBytes:(NSData*)param;
-(BOOL)isSupported;
@end
