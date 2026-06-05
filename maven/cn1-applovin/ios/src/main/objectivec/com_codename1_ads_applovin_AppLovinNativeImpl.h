#import <Foundation/Foundation.h>

@interface com_codename1_ads_applovin_AppLovinNativeImpl : NSObject {
}

-(void)initialize:(NSString*)param param1:(BOOL)param1 param2:(int)param2 param3:(int)param3 param4:(int)param4;
-(BOOL)createFullScreen:(int)param param1:(int)param1 param2:(NSString*)param2;
-(void)setServerSideVerification:(int)param param1:(NSString*)param1 param2:(NSString*)param2;
-(void)loadFullScreen:(int)param param1:(NSString*)param1 param2:(NSString*)param2 param3:(BOOL)param3;
-(BOOL)isFullScreenLoaded:(int)param;
-(void)showFullScreen:(int)param;
-(void)setAppOpenAutoShow:(int)param param1:(BOOL)param1;
-(void)disposeFullScreen:(int)param;
-(void*)createBanner:(int)param param1:(NSString*)param1 param2:(int)param2 param3:(int)param3;
-(void)loadBanner:(int)param param1:(NSString*)param1 param2:(NSString*)param2 param3:(BOOL)param3;
-(void)disposeBanner:(int)param;
-(void)requestConsent:(BOOL)param;
-(int)getConsentStatus;
-(BOOL)canRequestAds;
-(void)resetConsent;
-(BOOL)isSupported;
@end
