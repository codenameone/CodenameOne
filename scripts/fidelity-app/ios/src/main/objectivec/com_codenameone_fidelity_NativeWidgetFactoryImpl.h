#import <Foundation/Foundation.h>
#import <UIKit/UIKit.h>

@interface com_codenameone_fidelity_NativeWidgetFactoryImpl : NSObject {
}

-(BOOL)renderWidgetToFile:(NSString*)kind param1:(NSString*)state param2:(NSString*)appearance param3:(NSString*)text param4:(NSString*)outPath param5:(int)widthPx param6:(int)heightPx;
-(BOOL)isWidgetSupported:(NSString*)kind;
-(BOOL)isSupported;

@end
