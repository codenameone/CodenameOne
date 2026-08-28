/*
 * Copyright (c) 2012, Codename One and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.  Codename One designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Oracle in the LICENSE file that accompanied this code.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 2 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 *
 * You should have received a copy of the GNU General Public License version
 * 2 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Please contact Codename One through http://www.codenameone.com/ if you
 * need additional information or have any questions.
 */
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
 * The GMA symbol names here have to match the pod pinned in
 * codenameone_library_required.properties, and Google does rename and remove
 * them across major versions. Nothing in an app build catches that before the
 * customer's Xcode does, so ad-cn1lib-ios-native-check.yml compiles this file
 * against that pod on every PR that touches it.
 */
#import "com_codename1_ads_admob_AdMobNativeImpl.h"
#import <UIKit/UIKit.h>
#import <GoogleMobileAds/GoogleMobileAds.h>
#import <UserMessagingPlatform/UserMessagingPlatform.h>
#import <AppTrackingTransparency/AppTrackingTransparency.h>

// Handing a UIView to Codename One as a native peer is a pointer cast whose
// spelling depends on the memory model the file is compiled under. There is no
// BRIDGE_RETAINED anywhere in the port, and a retained cast would be wrong here
// anyway: NativeIPhoneView retains the peer itself and releases it when the
// component is collected, while cn1Banners holds the view for as long as the
// banner exists.
#ifndef BRIDGE_CAST
#if __has_feature(objc_arc)
#define BRIDGE_CAST __bridge
#else
#define BRIDGE_CAST
#endif
#endif

// The generated app target is manual retain/release (CLANG_ENABLE_OBJC_ARC = NO
// in the translator's template project), so an object handed to one of the
// dictionaries or to a strong property below is owned twice over: once by the
// alloc and once by the container that retains it. Releasing the extra
// reference outright would not compile under ARC, where it does not exist and
// release is forbidden, so ownership is handed over through this macro.
#if __has_feature(objc_arc)
#define CN1_HANDOVER(x) (x)
#else
#define CN1_HANDOVER(x) [(x) autorelease]
#endif

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
#if !__has_feature(objc_arc)
- (void)dealloc {
    // MRR releases nothing for us when the holder goes away. Clearing through
    // the synthesized setters does it without naming the ivars.
    self.adUnitId = nil;
    self.ad = nil;
    self.delegate = nil;
    self.ssv = nil;
    [super dealloc];
}
#endif
@end

@interface CN1Banner : NSObject
@property (nonatomic, strong) GADBannerView *view;
@property (nonatomic, strong) CN1BannerDelegate *delegate;
@end

@implementation CN1Banner
#if !__has_feature(objc_arc)
- (void)dealloc {
    // MRR releases nothing for us when the holder goes away. Clearing through
    // the synthesized setters does it without naming the ivars.
    self.view = nil;
    self.delegate = nil;
    [super dealloc];
}
#endif
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
        GADExtras *extras = CN1_HANDOVER([[GADExtras alloc] init]);
        extras.additionalParameters = @{@"npa": @"1"};
        [request registerAdNetworkExtras:extras];
    }
    return request;
}

// The three state privacy flags AdConfig sends: 1 means yes, 2 means no and
// anything else leaves the signal unset. tagForChildDirectedTreatment and
// tagForUnderAgeOfConsent are marked deprecated in favour of
// ageRestrictedTreatment, which collapses child, teen and unspecified into one
// enum and therefore cannot express an explicit "no". These two can, and they
// are what the Android side sends, so the bridge keeps using them.
static void cn1ApplyPrivacyFlags(GADRequestConfiguration *cfg, int childDirected,
        int underAge, int maxRating) {
#pragma clang diagnostic push
#pragma clang diagnostic ignored "-Wdeprecated-declarations"
    if (childDirected == 1) {
        cfg.tagForChildDirectedTreatment = @YES;
    } else if (childDirected == 2) {
        cfg.tagForChildDirectedTreatment = @NO;
    }
    if (underAge == 1) {
        cfg.tagForUnderAgeOfConsent = @YES;
    } else if (underAge == 2) {
        cfg.tagForUnderAgeOfConsent = @NO;
    }
#pragma clang diagnostic pop
    switch (maxRating) {
        case 1: cfg.maxAdContentRating = GADMaxAdContentRatingGeneral; break;
        case 2: cfg.maxAdContentRating = GADMaxAdContentRatingParentalGuidance; break;
        case 3: cfg.maxAdContentRating = GADMaxAdContentRatingTeen; break;
        case 4: cfg.maxAdContentRating = GADMaxAdContentRatingMatureAudience; break;
        default: break;
    }
}

@implementation com_codename1_ads_admob_AdMobNativeImpl

-(void)initialize:(NSString*)param param1:(BOOL)param1 param2:(int)param2 param3:(int)param3 param4:(int)param4 {
    if (cn1FullScreenAds == nil) {
        cn1FullScreenAds = [[NSMutableDictionary alloc] init];
        cn1Banners = [[NSMutableDictionary alloc] init];
    }
    dispatch_async(dispatch_get_main_queue(), ^{
        GADRequestConfiguration *cfg = GADMobileAds.sharedInstance.requestConfiguration;
        // param1 is AdConfig.testMode, and it deliberately adds nothing to the
        // list: the SDK counts every simulator as a test device on its own, and
        // the GADSimulatorID constant that used to say so was removed in SDK 12.
        // Explicit device ids still arrive through param.
        if (param != nil && param.length > 0) {
            NSMutableArray *devices = [NSMutableArray array];
            [devices addObjectsFromArray:[param componentsSeparatedByString:@","]];
            if (devices.count > 0) {
                cfg.testDeviceIdentifiers = devices;
            }
        }
        cn1ApplyPrivacyFlags(cfg, param2, param3, param4);
        [[GADMobileAds sharedInstance] startWithCompletionHandler:nil];
    });
}

-(BOOL)createFullScreen:(int)param param1:(int)param1 param2:(NSString*)param2 {
    CN1FullScreenAd *fs = CN1_HANDOVER([[CN1FullScreenAd alloc] init]);
    fs.format = param1;
    fs.adUnitId = param2;
    fs.delegate = CN1_HANDOVER([[CN1AdDelegate alloc] init]);
    fs.delegate.handle = param;
    cn1FullScreenAds[@(param)] = fs;
    return YES;
}

-(void)setServerSideVerification:(int)param param1:(NSString*)param1 param2:(NSString*)param2 {
    CN1FullScreenAd *fs = cn1FullScreenAds[@(param)];
    if (fs == nil) { return; }
    GADServerSideVerificationOptions *opts =
            CN1_HANDOVER([[GADServerSideVerificationOptions alloc] init]);
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
        bannerView = CN1_HANDOVER([[GADBannerView alloc] initWithAdSize:size]);
        bannerView.adUnitID = param1;
        bannerView.rootViewController = cn1RootController();
        CN1Banner *holder = CN1_HANDOVER([[CN1Banner alloc] init]);
        holder.view = bannerView;
        holder.delegate = CN1_HANDOVER([[CN1BannerDelegate alloc] init]);
        holder.delegate.handle = param;
        bannerView.delegate = holder.delegate;
        cn1Banners[@(param)] = holder;
    });
    // Hand the UIView to Codename One as a native peer.
    return (BRIDGE_CAST void*)bannerView;
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
        UMPRequestParameters *parameters = CN1_HANDOVER([[UMPRequestParameters alloc] init]);
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
