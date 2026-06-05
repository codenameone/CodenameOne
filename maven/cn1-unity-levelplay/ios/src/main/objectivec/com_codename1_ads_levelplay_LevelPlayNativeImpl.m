/*
 * iOS implementation of the Unity LevelPlay (ironSource) native bridge. Shipped
 * as source and compiled by the Codename One iOS build; validated on device.
 * Events are reported back to Java through the single static fan-in method
 * com.codename1.ads.levelplay.LevelPlayCallback.fire(...), keyed by an integer
 * handle. LevelPlay interstitial/rewarded placements are singletons, so the
 * active full screen handle is routed to the singleton delegate callbacks.
 */
#import "com_codename1_ads_levelplay_LevelPlayNativeImpl.h"
#import <UIKit/UIKit.h>
#import <IronSource/IronSource.h>
#import <AppTrackingTransparency/AppTrackingTransparency.h>

extern void com_codename1_ads_levelplay_LevelPlayCallback_fire___int_int_int_java_lang_String_java_lang_String_int(
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

static int cn1ActiveInterstitial = -1;
static int cn1ActiveRewarded = -1;
static int cn1CurrentBannerHandle = -1;
static NSMutableDictionary *cn1Formats;     // handle -> format
static NSMutableDictionary *cn1BannerViews; // handle -> wrapper UIView

static void cn1Fire(int handle, int event, int code, NSString *message, NSString *rewardType, int rewardAmount) {
    if (handle < 0) { return; }
    JAVA_OBJECT jMessage = message == nil ? JAVA_NULL : fromNSString(CN1_THREAD_GET_STATE_PASS_ARG message);
    JAVA_OBJECT jReward = rewardType == nil ? JAVA_NULL : fromNSString(CN1_THREAD_GET_STATE_PASS_ARG rewardType);
    com_codename1_ads_levelplay_LevelPlayCallback_fire___int_int_int_java_lang_String_java_lang_String_int(
            CN1_THREAD_GET_STATE_PASS_ARG handle, event, code, jMessage, jReward, rewardAmount);
}

static UIViewController *cn1RootController() {
    return [UIApplication sharedApplication].keyWindow.rootViewController;
}

static NSString *cn1AppKey() {
    NSString *k = [[NSBundle mainBundle] objectForInfoDictionaryKey:@"LevelPlayAppKey"];
    return k == nil ? @"" : k;
}

// Singleton delegate bridging IronSource interstitial + rewarded + banner.
@interface CN1LevelPlayDelegate : NSObject <ISInterstitialDelegate, ISRewardedVideoDelegate, ISBannerDelegate>
@end

@implementation CN1LevelPlayDelegate
// Interstitial
- (void)interstitialDidLoad { cn1Fire(cn1ActiveInterstitial, CN1_AD_LOADED, 0, nil, nil, 0); }
- (void)interstitialDidFailToLoadWithError:(NSError *)error { cn1Fire(cn1ActiveInterstitial, CN1_AD_FAILED, (int)error.code, error.localizedDescription, nil, 0); }
- (void)interstitialDidOpen { cn1Fire(cn1ActiveInterstitial, CN1_AD_SHOWN, 0, nil, nil, 0); cn1Fire(cn1ActiveInterstitial, CN1_AD_IMPRESSION, 0, nil, nil, 0); }
- (void)interstitialDidShow {}
- (void)interstitialDidFailToShowWithError:(NSError *)error { cn1Fire(cn1ActiveInterstitial, CN1_AD_SHOW_FAILED, (int)error.code, error.localizedDescription, nil, 0); }
- (void)didClickInterstitial { cn1Fire(cn1ActiveInterstitial, CN1_AD_CLICKED, 0, nil, nil, 0); }
- (void)interstitialDidClose { cn1Fire(cn1ActiveInterstitial, CN1_AD_DISMISSED, 0, nil, nil, 0); }
// Rewarded
- (void)rewardedVideoHasChangedAvailability:(BOOL)available { if (available) cn1Fire(cn1ActiveRewarded, CN1_AD_LOADED, 0, nil, nil, 0); }
- (void)rewardedVideoDidOpen { cn1Fire(cn1ActiveRewarded, CN1_AD_SHOWN, 0, nil, nil, 0); cn1Fire(cn1ActiveRewarded, CN1_AD_IMPRESSION, 0, nil, nil, 0); }
- (void)rewardedVideoDidFailToShowWithError:(NSError *)error { cn1Fire(cn1ActiveRewarded, CN1_AD_SHOW_FAILED, (int)error.code, error.localizedDescription, nil, 0); }
- (void)didClickRewardedVideo:(ISPlacementInfo *)placementInfo { cn1Fire(cn1ActiveRewarded, CN1_AD_CLICKED, 0, nil, nil, 0); }
- (void)didReceiveRewardForPlacement:(ISPlacementInfo *)placementInfo { cn1Fire(cn1ActiveRewarded, CN1_AD_REWARD, 0, nil, placementInfo.rewardName, placementInfo.rewardAmount.intValue); }
- (void)rewardedVideoDidClose { cn1Fire(cn1ActiveRewarded, CN1_AD_DISMISSED, 0, nil, nil, 0); }
- (void)rewardedVideoDidStart {}
- (void)rewardedVideoDidEnd {}
// Banner: the loaded ISBannerView is inserted into the wrapper handed to CN1.
- (void)bannerDidLoad:(ISBannerView *)bannerView {
    UIView *wrapper = cn1BannerViews[@(cn1CurrentBannerHandle)];
    if (wrapper != nil) {
        bannerView.frame = wrapper.bounds;
        bannerView.autoresizingMask = UIViewAutoresizingFlexibleWidth | UIViewAutoresizingFlexibleHeight;
        [wrapper addSubview:bannerView];
    }
    cn1Fire(cn1CurrentBannerHandle, CN1_AD_LOADED, 0, nil, nil, 0);
    cn1Fire(cn1CurrentBannerHandle, CN1_AD_IMPRESSION, 0, nil, nil, 0);
}
- (void)bannerDidFailToLoadWithError:(NSError *)error { cn1Fire(cn1CurrentBannerHandle, CN1_AD_FAILED, (int)error.code, error.localizedDescription, nil, 0); }
- (void)didClickBanner { cn1Fire(cn1CurrentBannerHandle, CN1_AD_CLICKED, 0, nil, nil, 0); }
- (void)bannerWillPresentScreen {}
- (void)bannerDidDismissScreen {}
- (void)bannerWillLeaveApplication {}
@end

static CN1LevelPlayDelegate *cn1Delegate;

@implementation com_codename1_ads_levelplay_LevelPlayNativeImpl

-(void)initialize:(NSString*)param param1:(BOOL)param1 param2:(int)param2 param3:(int)param3 param4:(int)param4 {
    if (cn1Formats == nil) {
        cn1Formats = [[NSMutableDictionary alloc] init];
        cn1BannerViews = [[NSMutableDictionary alloc] init];
        cn1Delegate = [[CN1LevelPlayDelegate alloc] init];
    }
    dispatch_async(dispatch_get_main_queue(), ^{
        [IronSource setInterstitialDelegate:cn1Delegate];
        [IronSource setRewardedVideoDelegate:cn1Delegate];
        [IronSource setBannerDelegate:cn1Delegate];
        [IronSource initWithAppKey:cn1AppKey()];
    });
}

-(BOOL)createFullScreen:(int)param param1:(int)param1 param2:(NSString*)param2 {
    if (param1 != CN1_FORMAT_INTERSTITIAL && param1 != CN1_FORMAT_REWARDED) {
        return NO; // LevelPlay has no dedicated app-open / rewarded-interstitial
    }
    cn1Formats[@(param)] = @(param1);
    return YES;
}

-(void)setServerSideVerification:(int)param param1:(NSString*)param1 param2:(NSString*)param2 {
    if (param1 != nil) { [IronSource setUserId:param1]; }
}

-(void)loadFullScreen:(int)param param1:(NSString*)param1 param2:(NSString*)param2 param3:(BOOL)param3 {
    NSNumber *fmt = cn1Formats[@(param)];
    if (fmt == nil) { return; }
    dispatch_async(dispatch_get_main_queue(), ^{
        if (fmt.intValue == CN1_FORMAT_INTERSTITIAL) {
            cn1ActiveInterstitial = param;
            [IronSource loadInterstitial];
        } else {
            cn1ActiveRewarded = param;
            if ([IronSource hasRewardedVideo]) { cn1Fire(param, CN1_AD_LOADED, 0, nil, nil, 0); }
        }
    });
}

-(BOOL)isFullScreenLoaded:(int)param {
    NSNumber *fmt = cn1Formats[@(param)];
    if (fmt == nil) { return NO; }
    if (fmt.intValue == CN1_FORMAT_INTERSTITIAL) { return [IronSource hasInterstitial]; }
    return [IronSource hasRewardedVideo];
}

-(void)showFullScreen:(int)param {
    NSNumber *fmt = cn1Formats[@(param)];
    if (fmt == nil) {
        cn1Fire(param, CN1_AD_SHOW_FAILED, 100, @"No ad loaded", nil, 0);
        return;
    }
    dispatch_async(dispatch_get_main_queue(), ^{
        UIViewController *root = cn1RootController();
        if (fmt.intValue == CN1_FORMAT_INTERSTITIAL) {
            cn1ActiveInterstitial = param;
            [IronSource showInterstitialWithViewController:root];
        } else {
            cn1ActiveRewarded = param;
            [IronSource showRewardedVideoWithViewController:root];
        }
    });
}

-(void)setAppOpenAutoShow:(int)param param1:(BOOL)param1 {}

-(void)disposeFullScreen:(int)param {
    [cn1Formats removeObjectForKey:@(param)];
}

-(void*)createBanner:(int)param param1:(NSString*)param1 param2:(int)param2 param3:(int)param3 {
    __block UIView *wrapper = nil;
    dispatch_sync(dispatch_get_main_queue(), ^{
        // The ISBannerView arrives asynchronously via the delegate, so hand
        // Codename One a wrapper view and add the banner to it on load.
        wrapper = [[UIView alloc] initWithFrame:CGRectZero];
        cn1BannerViews[@(param)] = wrapper;
    });
    return (BRIDGE_RETAINED void*)wrapper;
}

-(void)loadBanner:(int)param param1:(NSString*)param1 param2:(NSString*)param2 param3:(BOOL)param3 {
    dispatch_async(dispatch_get_main_queue(), ^{
        cn1CurrentBannerHandle = param;
        [IronSource loadBannerWithViewController:cn1RootController() size:ISBannerSize_BANNER];
    });
}

-(void)disposeBanner:(int)param {
    [cn1BannerViews removeObjectForKey:@(param)];
}

-(void)requestConsent:(BOOL)param {
    dispatch_async(dispatch_get_main_queue(), ^{
        if (@available(iOS 14, *)) {
            [ATTrackingManager requestTrackingAuthorizationWithCompletionHandler:^(ATTrackingManagerAuthorizationStatus status) {
                cn1Fire(0, CN1_AD_CONSENT_COMPLETE, 2, nil, nil, 0);
            }];
        } else {
            cn1Fire(0, CN1_AD_CONSENT_COMPLETE, 2, nil, nil, 0);
        }
    });
}

-(int)getConsentStatus { return 2; }
-(BOOL)canRequestAds { return YES; }
-(void)resetConsent {}
-(BOOL)isSupported { return YES; }

@end
