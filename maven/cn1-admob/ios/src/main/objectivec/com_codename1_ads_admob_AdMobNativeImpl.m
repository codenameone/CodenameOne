/*
 * iOS implementation of the AdMob native bridge, built on the modern Google
 * Mobile Ads (GMA) SDK (GADInterstitialAd / GADRewardedAd /
 * GADRewardedInterstitialAd / GADAppOpenAd / GADBannerView), the User
 * Messaging Platform (UMP) for GDPR consent and App Tracking Transparency
 * (ATT). Shipped as source and compiled by the Codename One iOS build.
 *
 * Every event is reported back to Java through the single static fan-in method
 * com.codename1.ads.admob.AdMobCallback.fire(...), keyed by an integer handle,
 * which keeps the native->Java binding surface to one function.
 *
 * This native layer is validated by an on-device iOS build; adjust the GMA
 * symbol names here to the pod version pinned in
 * codenameone_library_required.properties if Google renames an API.
 */
#import "com_codename1_ads_admob_AdMobNativeImpl.h"
#import <UIKit/UIKit.h>
#import <GoogleMobileAds/GoogleMobileAds.h>
#import <UserMessagingPlatform/UserMessagingPlatform.h>
#import <AppTrackingTransparency/AppTrackingTransparency.h>

// Generated entry point for com.codename1.ads.admob.AdMobCallback.fire(int,int,int,String,String,int)
extern void com_codename1_ads_admob_AdMobCallback_fire___int_int_int_java_lang_String_java_lang_String_int(
        CN1_THREAD_STATE_MULTI_ARG JAVA_INT handle, JAVA_INT event, JAVA_INT code,
        JAVA_OBJECT message, JAVA_OBJECT rewardType, JAVA_INT rewardAmount);

// Event codes mirror com.codename1.ads.admob.AdMobCallback.
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
#define CN1_FORMAT_REWARDED_INTERSTITIAL 3
#define CN1_FORMAT_APP_OPEN 4

static void cn1FireAd(int handle, int event, int code, NSString *message, NSString *rewardType, int rewardAmount) {
    JAVA_OBJECT jMessage = message == nil ? JAVA_NULL : fromNSString(CN1_THREAD_GET_STATE_PASS_ARG message);
    JAVA_OBJECT jReward = rewardType == nil ? JAVA_NULL : fromNSString(CN1_THREAD_GET_STATE_PASS_ARG rewardType);
    com_codename1_ads_admob_AdMobCallback_fire___int_int_int_java_lang_String_java_lang_String_int(
            CN1_THREAD_GET_STATE_PASS_ARG handle, event, code, jMessage, jReward, rewardAmount);
}

// Delegate that forwards GADFullScreenContentDelegate events for one handle.
@interface CN1AdDelegate : NSObject <GADFullScreenContentDelegate>
@property (nonatomic) int handle;
@end

@implementation CN1AdDelegate
- (void)adDidRecordImpression:(id<GADFullScreenPresentingAd>)ad {
    cn1FireAd(self.handle, CN1_AD_IMPRESSION, 0, nil, nil, 0);
}
- (void)adDidRecordClick:(id<GADFullScreenPresentingAd>)ad {
    cn1FireAd(self.handle, CN1_AD_CLICKED, 0, nil, nil, 0);
}
- (void)ad:(id<GADFullScreenPresentingAd>)ad didFailToPresentFullScreenContentWithError:(NSError *)error {
    cn1FireAd(self.handle, CN1_AD_SHOW_FAILED, (int)error.code, error.localizedDescription, nil, 0);
}
- (void)adWillPresentFullScreenContent:(id<GADFullScreenPresentingAd>)ad {
    cn1FireAd(self.handle, CN1_AD_SHOWN, 0, nil, nil, 0);
}
- (void)adDidDismissFullScreenContent:(id<GADFullScreenPresentingAd>)ad {
    cn1FireAd(self.handle, CN1_AD_DISMISSED, 0, nil, nil, 0);
}
@end

// Delegate that forwards GADBannerViewDelegate events for one handle.
@interface CN1BannerDelegate : NSObject <GADBannerViewDelegate>
@property (nonatomic) int handle;
@end

@implementation CN1BannerDelegate
- (void)bannerViewDidReceiveAd:(GADBannerView *)bannerView {
    cn1FireAd(self.handle, CN1_AD_LOADED, 0, nil, nil, 0);
}
- (void)bannerView:(GADBannerView *)bannerView didFailToReceiveAdWithError:(NSError *)error {
    cn1FireAd(self.handle, CN1_AD_FAILED, (int)error.code, error.localizedDescription, nil, 0);
}
- (void)bannerViewDidRecordImpression:(GADBannerView *)bannerView {
    cn1FireAd(self.handle, CN1_AD_IMPRESSION, 0, nil, nil, 0);
}
- (void)bannerViewDidRecordClick:(GADBannerView *)bannerView {
    cn1FireAd(self.handle, CN1_AD_CLICKED, 0, nil, nil, 0);
}
@end

// One loaded full screen ad of any format.
@interface CN1FullScreenAd : NSObject
@property (nonatomic) int format;
@property (nonatomic, strong) NSString *adUnitId;
@property (nonatomic, strong) id ad; // GADInterstitialAd | GADRewardedAd | GADRewardedInterstitialAd | GADAppOpenAd
@property (nonatomic, strong) CN1AdDelegate *delegate;
@property (nonatomic, strong) GADServerSideVerificationOptions *ssv;
@end

@implementation CN1FullScreenAd
@end

@interface CN1Banner : NSObject
@property (nonatomic, strong) GADBannerView *view;
@property (nonatomic, strong) CN1BannerDelegate *delegate;
@end

@implementation CN1Banner
@end

static NSMutableDictionary *cn1FullScreenAds;
static NSMutableDictionary *cn1Banners;

static UIViewController *cn1RootController() {
    return [UIApplication sharedApplication].keyWindow.rootViewController;
}

static GADRequest *cn1BuildRequest(NSString *keywords, BOOL nonPersonalized) {
    GADRequest *request = [GADRequest request];
    if (keywords != nil && keywords.length > 0) {
        request.keywords = [keywords componentsSeparatedByString:@","];
    }
    if (nonPersonalized) {
        GADExtras *extras = [[GADExtras alloc] init];
        extras.additionalParameters = @{@"npa": @"1"};
        [request registerAdNetworkExtras:extras];
    }
    return request;
}

@implementation com_codename1_ads_admob_AdMobNativeImpl

-(void)initialize:(NSString*)param param1:(BOOL)param1 param2:(int)param2 param3:(int)param3 param4:(int)param4 {
    if (cn1FullScreenAds == nil) {
        cn1FullScreenAds = [[NSMutableDictionary alloc] init];
        cn1Banners = [[NSMutableDictionary alloc] init];
    }
    dispatch_async(dispatch_get_main_queue(), ^{
        GADRequestConfiguration *cfg = GADMobileAds.sharedInstance.requestConfiguration;
        NSMutableArray *devices = [NSMutableArray array];
        if (param1) {
            [devices addObject:GADSimulatorID];
        }
        if (param != nil && param.length > 0) {
            [devices addObjectsFromArray:[param componentsSeparatedByString:@","]];
        }
        if (devices.count > 0) {
            cfg.testDeviceIdentifiers = devices;
        }
        [[GADMobileAds sharedInstance] startWithCompletionHandler:nil];
    });
}

-(BOOL)createFullScreen:(int)param param1:(int)param1 param2:(NSString*)param2 {
    CN1FullScreenAd *fs = [[CN1FullScreenAd alloc] init];
    fs.format = param1;
    fs.adUnitId = param2;
    fs.delegate = [[CN1AdDelegate alloc] init];
    fs.delegate.handle = param;
    cn1FullScreenAds[@(param)] = fs;
    return YES;
}

-(void)setServerSideVerification:(int)param param1:(NSString*)param1 param2:(NSString*)param2 {
    CN1FullScreenAd *fs = cn1FullScreenAds[@(param)];
    if (fs == nil) { return; }
    GADServerSideVerificationOptions *opts = [[GADServerSideVerificationOptions alloc] init];
    if (param1 != nil) { opts.userIdentifier = param1; }
    if (param2 != nil) { opts.customRewardString = param2; }
    fs.ssv = opts;
}

-(void)loadFullScreen:(int)param param1:(NSString*)param1 param2:(NSString*)param2 param3:(BOOL)param3 {
    int handle = param;
    CN1FullScreenAd *fs = cn1FullScreenAds[@(handle)];
    if (fs == nil) { return; }
    GADRequest *request = cn1BuildRequest(param1, param3);
    dispatch_async(dispatch_get_main_queue(), ^{
        if (fs.format == CN1_FORMAT_INTERSTITIAL) {
            [GADInterstitialAd loadWithAdUnitID:fs.adUnitId request:request
                    completionHandler:^(GADInterstitialAd *ad, NSError *error) {
                if (error) { cn1FireAd(handle, CN1_AD_FAILED, (int)error.code, error.localizedDescription, nil, 0); return; }
                ad.fullScreenContentDelegate = fs.delegate;
                fs.ad = ad;
                cn1FireAd(handle, CN1_AD_LOADED, 0, nil, nil, 0);
            }];
        } else if (fs.format == CN1_FORMAT_REWARDED) {
            [GADRewardedAd loadWithAdUnitID:fs.adUnitId request:request
                    completionHandler:^(GADRewardedAd *ad, NSError *error) {
                if (error) { cn1FireAd(handle, CN1_AD_FAILED, (int)error.code, error.localizedDescription, nil, 0); return; }
                ad.fullScreenContentDelegate = fs.delegate;
                if (fs.ssv) { ad.serverSideVerificationOptions = fs.ssv; }
                fs.ad = ad;
                cn1FireAd(handle, CN1_AD_LOADED, 0, nil, nil, 0);
            }];
        } else if (fs.format == CN1_FORMAT_REWARDED_INTERSTITIAL) {
            [GADRewardedInterstitialAd loadWithAdUnitID:fs.adUnitId request:request
                    completionHandler:^(GADRewardedInterstitialAd *ad, NSError *error) {
                if (error) { cn1FireAd(handle, CN1_AD_FAILED, (int)error.code, error.localizedDescription, nil, 0); return; }
                ad.fullScreenContentDelegate = fs.delegate;
                if (fs.ssv) { ad.serverSideVerificationOptions = fs.ssv; }
                fs.ad = ad;
                cn1FireAd(handle, CN1_AD_LOADED, 0, nil, nil, 0);
            }];
        } else if (fs.format == CN1_FORMAT_APP_OPEN) {
            [GADAppOpenAd loadWithAdUnitID:fs.adUnitId request:request
                    completionHandler:^(GADAppOpenAd *ad, NSError *error) {
                if (error) { cn1FireAd(handle, CN1_AD_FAILED, (int)error.code, error.localizedDescription, nil, 0); return; }
                ad.fullScreenContentDelegate = fs.delegate;
                fs.ad = ad;
                cn1FireAd(handle, CN1_AD_LOADED, 0, nil, nil, 0);
            }];
        }
    });
}

-(BOOL)isFullScreenLoaded:(int)param {
    CN1FullScreenAd *fs = cn1FullScreenAds[@(param)];
    return fs != nil && fs.ad != nil;
}

-(void)showFullScreen:(int)param {
    int handle = param;
    CN1FullScreenAd *fs = cn1FullScreenAds[@(handle)];
    if (fs == nil || fs.ad == nil) {
        cn1FireAd(handle, CN1_AD_SHOW_FAILED, 100, @"No ad loaded", nil, 0);
        return;
    }
    dispatch_async(dispatch_get_main_queue(), ^{
        UIViewController *root = cn1RootController();
        GADUserDidEarnRewardHandler reward = ^{
            cn1FireAd(handle, CN1_AD_REWARD, 0, nil, @"reward", 1);
        };
        if ([fs.ad isKindOfClass:[GADInterstitialAd class]]) {
            [(GADInterstitialAd *)fs.ad presentFromRootViewController:root];
        } else if ([fs.ad isKindOfClass:[GADRewardedAd class]]) {
            [(GADRewardedAd *)fs.ad presentFromRootViewController:root userDidEarnRewardHandler:reward];
        } else if ([fs.ad isKindOfClass:[GADRewardedInterstitialAd class]]) {
            [(GADRewardedInterstitialAd *)fs.ad presentFromRootViewController:root userDidEarnRewardHandler:reward];
        } else if ([fs.ad isKindOfClass:[GADAppOpenAd class]]) {
            [(GADAppOpenAd *)fs.ad presentFromRootViewController:root];
        }
        fs.ad = nil; // a full screen ad is single use
    });
}

-(void)setAppOpenAutoShow:(int)param param1:(BOOL)param1 {
    // The reload-on-foreground observer (applicationDidBecomeActive) is a
    // device-side concern handled by the host controller; load/show above is
    // the core wiring.
}

-(void)disposeFullScreen:(int)param {
    [cn1FullScreenAds removeObjectForKey:@(param)];
}

-(void*)createBanner:(int)param param1:(NSString*)param1 param2:(int)param2 param3:(int)param3 {
    __block GADBannerView *bannerView = nil;
    dispatch_sync(dispatch_get_main_queue(), ^{
        GADAdSize size;
        switch (param2) {
            case 1: size = GADAdSizeBanner; break;
            case 2: size = GADAdSizeLargeBanner; break;
            case 3: size = GADAdSizeMediumRectangle; break;
            case 4: size = GADAdSizeLeaderboard; break;
            default: {
                CGFloat width = param3 > 0 ? param3 : [UIScreen mainScreen].bounds.size.width;
                size = GADCurrentOrientationAnchoredAdaptiveBannerAdSizeWithWidth(width);
            }
        }
        bannerView = [[GADBannerView alloc] initWithAdSize:size];
        bannerView.adUnitID = param1;
        bannerView.rootViewController = cn1RootController();
        CN1Banner *holder = [[CN1Banner alloc] init];
        holder.view = bannerView;
        holder.delegate = [[CN1BannerDelegate alloc] init];
        holder.delegate.handle = param;
        bannerView.delegate = holder.delegate;
        cn1Banners[@(param)] = holder;
    });
    // Hand the UIView to Codename One as a native peer.
    return (BRIDGE_RETAINED void*)bannerView;
}

-(void)loadBanner:(int)param param1:(NSString*)param1 param2:(NSString*)param2 param3:(BOOL)param3 {
    CN1Banner *holder = cn1Banners[@(param)];
    if (holder == nil) { return; }
    GADRequest *request = cn1BuildRequest(param1, param3);
    dispatch_async(dispatch_get_main_queue(), ^{
        [holder.view loadRequest:request];
    });
}

-(void)disposeBanner:(int)param {
    [cn1Banners removeObjectForKey:@(param)];
}

-(void)requestConsent:(BOOL)param {
    dispatch_async(dispatch_get_main_queue(), ^{
        // iOS 14+: request App Tracking Transparency before loading ads.
        if (@available(iOS 14, *)) {
            [ATTrackingManager requestTrackingAuthorizationWithCompletionHandler:^(ATTrackingManagerAuthorizationStatus status) {}];
        }
        UMPRequestParameters *parameters = [[UMPRequestParameters alloc] init];
        parameters.tagForUnderAgeOfConsent = param;
        [UMPConsentInformation.sharedInstance requestConsentInfoUpdateWithParameters:parameters
                completionHandler:^(NSError *_Nullable error) {
            UIViewController *root = cn1RootController();
            [UMPConsentForm loadAndPresentIfRequiredFromViewController:root
                    completionHandler:^(NSError *_Nullable formError) {
                cn1FireAd(0, CN1_AD_CONSENT_COMPLETE, (int)[self getConsentStatus], nil, nil, 0);
            }];
        }];
    });
}

-(int)getConsentStatus {
    switch (UMPConsentInformation.sharedInstance.consentStatus) {
        case UMPConsentStatusNotRequired: return 2;
        case UMPConsentStatusRequired: return 1;
        case UMPConsentStatusObtained: return 3;
        default: return 0;
    }
}

-(BOOL)canRequestAds {
    return UMPConsentInformation.sharedInstance.canRequestAds;
}

-(void)resetConsent {
    [UMPConsentInformation.sharedInstance reset];
}

-(BOOL)isSupported {
    return YES;
}

@end
