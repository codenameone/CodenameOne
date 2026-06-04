/*
 * iOS implementation of the AppLovin MAX native bridge (MAInterstitialAd /
 * MARewardedAd / MAAppOpenAd / MAAdView). Shipped as source and compiled by the
 * Codename One iOS build; validated on device. Every event is reported back to
 * Java through the single static fan-in method
 * com.codename1.ads.applovin.AppLovinCallback.fire(...), keyed by an integer
 * handle.
 */
#import "com_codename1_ads_applovin_AppLovinNativeImpl.h"
#import <UIKit/UIKit.h>
#import <AppLovinSDK/AppLovinSDK.h>
#import <AppTrackingTransparency/AppTrackingTransparency.h>

extern void com_codename1_ads_applovin_AppLovinCallback_fire___int_int_int_java_lang_String_java_lang_String_int(
        CN1_THREAD_STATE_MULTI_ARG JAVA_INT handle, JAVA_INT event, JAVA_INT code,
        JAVA_OBJECT message, JAVA_OBJECT rewardType, JAVA_INT rewardAmount);

#define CN1_AD_LOADED 1
#define CN1_AD_FAILED 2
#define CN1_AD_SHOWN 3
#define CN1_AD_SHOW_FAILED 4
#define CN1_AD_DISMISSED 5
#define CN1_AD_IMPRESSION 6
#define CN1_AD_CLICKED 7
#define CN1_AD_REWARD 8
#define CN1_AD_CONSENT_COMPLETE 9

#define CN1_FORMAT_INTERSTITIAL 1
#define CN1_FORMAT_REWARDED 2
#define CN1_FORMAT_APP_OPEN 4

static void cn1FireAd(int handle, int event, int code, NSString *message, NSString *rewardType, int rewardAmount) {
    JAVA_OBJECT jMessage = message == nil ? JAVA_NULL : fromNSString(CN1_THREAD_GET_STATE_PASS_ARG message);
    JAVA_OBJECT jReward = rewardType == nil ? JAVA_NULL : fromNSString(CN1_THREAD_GET_STATE_PASS_ARG rewardType);
    com_codename1_ads_applovin_AppLovinCallback_fire___int_int_int_java_lang_String_java_lang_String_int(
            CN1_THREAD_GET_STATE_PASS_ARG handle, event, code, jMessage, jReward, rewardAmount);
}

static UIViewController *cn1RootController() {
    return [UIApplication sharedApplication].keyWindow.rootViewController;
}

// Full screen delegate (MAAdDelegate + reward). One per handle.
@interface CN1MaxDelegate : NSObject <MAAdDelegate, MARewardedAdDelegate>
@property (nonatomic) int handle;
@end

@implementation CN1MaxDelegate
- (void)didLoadAd:(MAAd *)ad { cn1FireAd(self.handle, CN1_AD_LOADED, 0, nil, nil, 0); }
- (void)didFailToLoadAdForAdUnitIdentifier:(NSString *)adUnitIdentifier withError:(MAError *)error {
    cn1FireAd(self.handle, CN1_AD_FAILED, (int)error.code, error.message, nil, 0);
}
- (void)didDisplayAd:(MAAd *)ad { cn1FireAd(self.handle, CN1_AD_SHOWN, 0, nil, nil, 0); cn1FireAd(self.handle, CN1_AD_IMPRESSION, 0, nil, nil, 0); }
- (void)didFailToDisplayAd:(MAAd *)ad withError:(MAError *)error { cn1FireAd(self.handle, CN1_AD_SHOW_FAILED, (int)error.code, error.message, nil, 0); }
- (void)didClickAd:(MAAd *)ad { cn1FireAd(self.handle, CN1_AD_CLICKED, 0, nil, nil, 0); }
- (void)didHideAd:(MAAd *)ad { cn1FireAd(self.handle, CN1_AD_DISMISSED, 0, nil, nil, 0); }
- (void)didRewardUserForAd:(MAAd *)ad withReward:(MAReward *)reward {
    cn1FireAd(self.handle, CN1_AD_REWARD, 0, nil, reward.label, (int)reward.amount);
}
@end

// Banner delegate (MAAdViewAdDelegate).
@interface CN1MaxBannerDelegate : NSObject <MAAdViewAdDelegate>
@property (nonatomic) int handle;
@end

@implementation CN1MaxBannerDelegate
- (void)didLoadAd:(MAAd *)ad { cn1FireAd(self.handle, CN1_AD_LOADED, 0, nil, nil, 0); }
- (void)didFailToLoadAdForAdUnitIdentifier:(NSString *)adUnitIdentifier withError:(MAError *)error {
    cn1FireAd(self.handle, CN1_AD_FAILED, (int)error.code, error.message, nil, 0);
}
- (void)didDisplayAd:(MAAd *)ad { cn1FireAd(self.handle, CN1_AD_IMPRESSION, 0, nil, nil, 0); }
- (void)didClickAd:(MAAd *)ad { cn1FireAd(self.handle, CN1_AD_CLICKED, 0, nil, nil, 0); }
- (void)didFailToDisplayAd:(MAAd *)ad withError:(MAError *)error {}
- (void)didHideAd:(MAAd *)ad {}
- (void)didExpandAd:(MAAd *)ad {}
- (void)didCollapseAd:(MAAd *)ad {}
@end

@interface CN1MaxFullScreen : NSObject
@property (nonatomic) int format;
@property (nonatomic, strong) NSString *adUnitId;
@property (nonatomic, strong) id ad;
@property (nonatomic, strong) CN1MaxDelegate *delegate;
@property (nonatomic) BOOL loaded;
@end
@implementation CN1MaxFullScreen
@end

@interface CN1MaxBanner : NSObject
@property (nonatomic, strong) MAAdView *view;
@property (nonatomic, strong) CN1MaxBannerDelegate *delegate;
@end
@implementation CN1MaxBanner
@end

static NSMutableDictionary *cn1FullScreen;
static NSMutableDictionary *cn1Banners;

@implementation com_codename1_ads_applovin_AppLovinNativeImpl

-(void)initialize:(NSString*)param param1:(BOOL)param1 param2:(int)param2 param3:(int)param3 param4:(int)param4 {
    if (cn1FullScreen == nil) {
        cn1FullScreen = [[NSMutableDictionary alloc] init];
        cn1Banners = [[NSMutableDictionary alloc] init];
    }
    dispatch_async(dispatch_get_main_queue(), ^{
        [ALSdk shared].mediationProvider = @"max";
        [[ALSdk shared] initializeSdkWithCompletionHandler:^(ALSdkConfiguration *configuration) {}];
    });
}

-(BOOL)createFullScreen:(int)param param1:(int)param1 param2:(NSString*)param2 {
    if (param1 == 3) { return NO; } // no rewarded-interstitial in MAX
    CN1MaxFullScreen *fs = [[CN1MaxFullScreen alloc] init];
    fs.format = param1;
    fs.adUnitId = param2;
    fs.delegate = [[CN1MaxDelegate alloc] init];
    fs.delegate.handle = param;
    cn1FullScreen[@(param)] = fs;
    return YES;
}

-(void)setServerSideVerification:(int)param param1:(NSString*)param1 param2:(NSString*)param2 {
}

-(void)loadFullScreen:(int)param param1:(NSString*)param1 param2:(NSString*)param2 param3:(BOOL)param3 {
    int handle = param;
    CN1MaxFullScreen *fs = cn1FullScreen[@(handle)];
    if (fs == nil) { return; }
    dispatch_async(dispatch_get_main_queue(), ^{
        if (fs.format == CN1_FORMAT_INTERSTITIAL) {
            MAInterstitialAd *ad = [[MAInterstitialAd alloc] initWithAdUnitIdentifier:fs.adUnitId];
            ad.delegate = fs.delegate;
            fs.ad = ad;
            [ad loadAd];
        } else if (fs.format == CN1_FORMAT_REWARDED) {
            MARewardedAd *ad = [MARewardedAd sharedWithAdUnitIdentifier:fs.adUnitId];
            ad.delegate = fs.delegate;
            fs.ad = ad;
            [ad loadAd];
        } else if (fs.format == CN1_FORMAT_APP_OPEN) {
            MAAppOpenAd *ad = [[MAAppOpenAd alloc] initWithAdUnitIdentifier:fs.adUnitId];
            ad.delegate = fs.delegate;
            fs.ad = ad;
            [ad loadAd];
        }
    });
}

-(BOOL)isFullScreenLoaded:(int)param {
    CN1MaxFullScreen *fs = cn1FullScreen[@(param)];
    if (fs == nil || fs.ad == nil) { return NO; }
    if ([fs.ad respondsToSelector:@selector(isReady)]) {
        return [[fs.ad valueForKey:@"ready"] boolValue];
    }
    return YES;
}

-(void)showFullScreen:(int)param {
    int handle = param;
    CN1MaxFullScreen *fs = cn1FullScreen[@(handle)];
    if (fs == nil || fs.ad == nil) {
        cn1FireAd(handle, CN1_AD_SHOW_FAILED, 100, @"No ad loaded", nil, 0);
        return;
    }
    dispatch_async(dispatch_get_main_queue(), ^{
        if ([fs.ad isKindOfClass:[MAInterstitialAd class]]) {
            [(MAInterstitialAd *)fs.ad showAd];
        } else if ([fs.ad isKindOfClass:[MARewardedAd class]]) {
            [(MARewardedAd *)fs.ad showAd];
        } else if ([fs.ad isKindOfClass:[MAAppOpenAd class]]) {
            [(MAAppOpenAd *)fs.ad showAd];
        }
    });
}

-(void)setAppOpenAutoShow:(int)param param1:(BOOL)param1 {
}

-(void)disposeFullScreen:(int)param {
    [cn1FullScreen removeObjectForKey:@(param)];
}

-(void*)createBanner:(int)param param1:(NSString*)param1 param2:(int)param2 param3:(int)param3 {
    __block MAAdView *bannerView = nil;
    dispatch_sync(dispatch_get_main_queue(), ^{
        bannerView = [[MAAdView alloc] initWithAdUnitIdentifier:param1];
        CN1MaxBanner *holder = [[CN1MaxBanner alloc] init];
        holder.view = bannerView;
        holder.delegate = [[CN1MaxBannerDelegate alloc] init];
        holder.delegate.handle = param;
        bannerView.delegate = holder.delegate;
        cn1Banners[@(param)] = holder;
    });
    return (BRIDGE_RETAINED void*)bannerView;
}

-(void)loadBanner:(int)param param1:(NSString*)param1 param2:(NSString*)param2 param3:(BOOL)param3 {
    CN1MaxBanner *holder = cn1Banners[@(param)];
    if (holder == nil) { return; }
    dispatch_async(dispatch_get_main_queue(), ^{
        [holder.view loadAd];
    });
}

-(void)disposeBanner:(int)param {
    [cn1Banners removeObjectForKey:@(param)];
}

-(void)requestConsent:(BOOL)param {
    dispatch_async(dispatch_get_main_queue(), ^{
        if (@available(iOS 14, *)) {
            [ATTrackingManager requestTrackingAuthorizationWithCompletionHandler:^(ATTrackingManagerAuthorizationStatus status) {
                cn1FireAd(0, CN1_AD_CONSENT_COMPLETE, 2, nil, nil, 0);
            }];
        } else {
            cn1FireAd(0, CN1_AD_CONSENT_COMPLETE, 2, nil, nil, 0);
        }
    });
}

-(int)getConsentStatus { return 2; }
-(BOOL)canRequestAds { return YES; }
-(void)resetConsent {}
-(BOOL)isSupported { return YES; }

@end
