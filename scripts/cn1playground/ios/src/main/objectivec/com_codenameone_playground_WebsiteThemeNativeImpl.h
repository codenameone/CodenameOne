#import <Foundation/Foundation.h>

@interface com_codenameone_playground_WebsiteThemeNativeImpl : NSObject {
}

-(BOOL)isDarkMode;
-(BOOL)isSupported;
-(void)notifyUiReady;
-(NSString*)locationHref;
-(NSString*)takeInlineCode;
-(void)notifyInlineResult:(BOOL)param param1:(NSString*)param1 param2:(NSString*)param2;

@end
