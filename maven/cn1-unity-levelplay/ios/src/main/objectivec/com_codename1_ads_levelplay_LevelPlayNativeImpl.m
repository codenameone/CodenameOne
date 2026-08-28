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
 * iOS implementation of the Unity LevelPlay (ironSource) native bridge. Shipped
 * as source and compiled by the Codename One iOS build, and by
 * ad-cn1lib-ios-native-check.yml against the pod pinned in
 * codenameone_library_required.properties.
 *
 * Written against the unified LevelPlay API (LPMInterstitialAd / LPMRewardedAd
 * / LPMBannerAdView), where every ad is an object bound to one ad unit. The
 * singleton ironSource entry points this bridge used to call are gone from the
 * SDK, and they forced callbacks through a "currently active handle" field that
 * lost events whenever two ads of the same format were in flight; one ad object
 * per handle removes that.
 *
 * Events are reported back to Java through the single static fan-in method
 * com.codename1.ads.levelplay.LevelPlayCallback.fire(...), keyed by an integer
 * handle.
 */
#import "com_codename1_ads_levelplay_LevelPlayNativeImpl.h"
#import <UIKit/UIKit.h>
#import <IronSource/IronSource.h>
#import <AppTrackingTransparency/AppTrackingTransparency.h>

// Handing a UIView to Codename One as a native peer is a pointer cast whose
// spelling depends on the memory model the file is compiled under. There is no
// BRIDGE_RETAINED anywhere in the port, and a retained cast would be wrong here
// anyway: NativeIPhoneView retains the peer itself and releases it when the
// component is collected, while the banner dictionary holds the view for as
// long as the banner exists.
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

static NSMutableDictionary *cn1FullScreen; // handle -> CN1LPFullScreen
static NSMutableDictionary *cn1Banners;    // handle -> CN1LPBanner

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

// One delegate per full screen handle. LPM ads hold their delegate weakly, so
// the holder below owns both the ad and its delegate.
@interface CN1LPFullScreenDelegate : NSObject <LPMInterstitialAdDelegate, LPMRewardedAdDelegate>
@property (nonatomic) int handle;
@end

@implementation CN1LPFullScreenDelegate
- (void)didLoadAdWithAdInfo:(LPMAdInfo *)adInfo {
    cn1Fire(self.handle, CN1_AD_LOADED, 0, nil, nil, 0);
}
- (void)didFailToLoadAdWithAdUnitId:(NSString *)adUnitId error:(NSError *)error {
    cn1Fire(self.handle, CN1_AD_FAILED, (int)error.code, error.localizedDescription, nil, 0);
}
- (void)didDisplayAdWithAdInfo:(LPMAdInfo *)adInfo {
    cn1Fire(self.handle, CN1_AD_SHOWN, 0, nil, nil, 0);
    cn1Fire(self.handle, CN1_AD_IMPRESSION, 0, nil, nil, 0);
}
- (void)didFailToDisplayAdWithAdInfo:(LPMAdInfo *)adInfo error:(NSError *)error {
    cn1Fire(self.handle, CN1_AD_SHOW_FAILED, (int)error.code, error.localizedDescription, nil, 0);
}
- (void)didClickAdWithAdInfo:(LPMAdInfo *)adInfo {
    cn1Fire(self.handle, CN1_AD_CLICKED, 0, nil, nil, 0);
}
- (void)didCloseAdWithAdInfo:(LPMAdInfo *)adInfo {
    cn1Fire(self.handle, CN1_AD_DISMISSED, 0, nil, nil, 0);
}
- (void)didRewardAdWithAdInfo:(LPMAdInfo *)adInfo reward:(LPMReward *)reward {
    cn1Fire(self.handle, CN1_AD_REWARD, 0, nil, reward.name, (int)reward.amount);
}
@end

@interface CN1LPBannerDelegate : NSObject <LPMBannerAdViewDelegate>
@property (nonatomic) int handle;
@end

@implementation CN1LPBannerDelegate
- (void)didLoadAdWithAdInfo:(LPMAdInfo *)adInfo {
    cn1Fire(self.handle, CN1_AD_LOADED, 0, nil, nil, 0);
    cn1Fire(self.handle, CN1_AD_IMPRESSION, 0, nil, nil, 0);
}
- (void)didFailToLoadAdWithAdUnitId:(NSString *)adUnitId error:(NSError *)error {
    cn1Fire(self.handle, CN1_AD_FAILED, (int)error.code, error.localizedDescription, nil, 0);
}
- (void)didClickAdWithAdInfo:(LPMAdInfo *)adInfo {
    cn1Fire(self.handle, CN1_AD_CLICKED, 0, nil, nil, 0);
}
@end

@interface CN1LPFullScreen : NSObject
@property (nonatomic) int format;
@property (nonatomic, strong) LPMInterstitialAd *interstitial;
@property (nonatomic, strong) LPMRewardedAd *rewarded;
@property (nonatomic, strong) CN1LPFullScreenDelegate *delegate;
@end

@implementation CN1LPFullScreen
#if !__has_feature(objc_arc)
- (void)dealloc {
    // MRR releases nothing for us when the holder goes away. Clearing through
    // the synthesized setters does it without naming the ivars.
    self.interstitial = nil;
    self.rewarded = nil;
    self.delegate = nil;
    [super dealloc];
}
#endif
@end

@interface CN1LPBanner : NSObject
@property (nonatomic, strong) LPMBannerAdView *view;
@property (nonatomic, strong) CN1LPBannerDelegate *delegate;
@end

@implementation CN1LPBanner
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

// sizeType matches the SIZE_* constants in com.codename1.ads.BannerAd.
static LPMAdSize *cn1BannerSize(int sizeType, int widthDp) {
    switch (sizeType) {
        case 1: return [LPMAdSize bannerSize];
        case 2: return [LPMAdSize largeSize];
        case 3: return [LPMAdSize mediumRectangleSize];
        case 4: return [LPMAdSize leaderBoardSize];
        default: {
            CGFloat width = widthDp > 0 ? widthDp : [UIScreen mainScreen].bounds.size.width;
            LPMAdSize *adaptive = [LPMAdSize createAdaptiveAdSizeWithWidth:width];
            // The adaptive factory returns nil when no adaptive size fits the
            // width it was given.
            return adaptive == nil ? [LPMAdSize bannerSize] : adaptive;
        }
    }
}

@implementation com_codename1_ads_levelplay_LevelPlayNativeImpl

-(void)initialize:(NSString*)param param1:(BOOL)param1 param2:(int)param2 param3:(int)param3 param4:(int)param4 {
    if (cn1FullScreen == nil) {
        cn1FullScreen = [[NSMutableDictionary alloc] init];
        cn1Banners = [[NSMutableDictionary alloc] init];
    }
    dispatch_async(dispatch_get_main_queue(), ^{
        // LevelPlay has no test device list: test ads are switched on per ad
        // unit in the dashboard, so param and param1 (AdConfig's testDeviceIds
        // and testMode) have no counterpart on this platform.
        if (param2 == 1) {
            [LPMPrivacySettings setCOPPA:YES];
        } else if (param2 == 2) {
            [LPMPrivacySettings setCOPPA:NO];
        }
        LPMInitRequestBuilder *initBuilder =
                CN1_HANDOVER([[LPMInitRequestBuilder alloc] initWithAppKey:cn1AppKey()]);
        LPMInitRequest *request = [initBuilder build];
        [LevelPlay initWithRequest:request
                        completion:^(LPMConfiguration *config, NSError *error) {
            if (error != nil) {
                cn1Fire(0, CN1_AD_FAILED, (int)error.code, error.localizedDescription, nil, 0);
            }
        }];
    });
}

-(BOOL)createFullScreen:(int)param param1:(int)param1 param2:(NSString*)param2 {
    if (param1 != CN1_FORMAT_INTERSTITIAL && param1 != CN1_FORMAT_REWARDED) {
        return NO; // LevelPlay has no dedicated app-open / rewarded-interstitial
    }
    CN1LPFullScreen *fs = CN1_HANDOVER([[CN1LPFullScreen alloc] init]);
    fs.format = param1;
    fs.delegate = CN1_HANDOVER([[CN1LPFullScreenDelegate alloc] init]);
    fs.delegate.handle = param;
    if (param1 == CN1_FORMAT_REWARDED) {
        fs.rewarded = CN1_HANDOVER([[LPMRewardedAd alloc] initWithAdUnitId:param2]);
        [fs.rewarded setDelegate:fs.delegate];
    } else {
        fs.interstitial = CN1_HANDOVER([[LPMInterstitialAd alloc] initWithAdUnitId:param2]);
        [fs.interstitial setDelegate:fs.delegate];
    }
    cn1FullScreen[@(param)] = fs;
    return YES;
}

-(void)setServerSideVerification:(int)param param1:(NSString*)param1 param2:(NSString*)param2 {
    if (param1 != nil) { [LevelPlay setDynamicUserId:param1]; }
}

-(void)loadFullScreen:(int)param param1:(NSString*)param1 param2:(NSString*)param2 param3:(BOOL)param3 {
    CN1LPFullScreen *fs = cn1FullScreen[@(param)];
    if (fs == nil) { return; }
    dispatch_async(dispatch_get_main_queue(), ^{
        if (fs.rewarded != nil) {
            [fs.rewarded loadAd];
        } else {
            [fs.interstitial loadAd];
        }
    });
}

-(BOOL)isFullScreenLoaded:(int)param {
    CN1LPFullScreen *fs = cn1FullScreen[@(param)];
    if (fs == nil) { return NO; }
    if (fs.rewarded != nil) { return [fs.rewarded isAdReady]; }
    return [fs.interstitial isAdReady];
}

-(void)showFullScreen:(int)param {
    CN1LPFullScreen *fs = cn1FullScreen[@(param)];
    if (fs == nil) {
        cn1Fire(param, CN1_AD_SHOW_FAILED, 100, @"No ad loaded", nil, 0);
        return;
    }
    dispatch_async(dispatch_get_main_queue(), ^{
        UIViewController *root = cn1RootController();
        if (fs.rewarded != nil) {
            [fs.rewarded showAdWithViewController:root placementName:nil];
        } else {
            [fs.interstitial showAdWithViewController:root placementName:nil];
        }
    });
}

-(void)setAppOpenAutoShow:(int)param param1:(BOOL)param1 {}

-(void)disposeFullScreen:(int)param {
    [cn1FullScreen removeObjectForKey:@(param)];
}

-(void*)createBanner:(int)param param1:(NSString*)param1 param2:(int)param2 param3:(int)param3 {
    __block LPMBannerAdView *bannerView = nil;
    dispatch_sync(dispatch_get_main_queue(), ^{
        LPMBannerAdViewConfigBuilder *builder =
                CN1_HANDOVER([[LPMBannerAdViewConfigBuilder alloc] init]);
        LPMBannerAdViewConfig *config =
                [[builder setWithAdSize:cn1BannerSize(param2, param3)] build];
        bannerView = CN1_HANDOVER([[LPMBannerAdView alloc] initWithAdUnitId:param1 config:config]);
        CN1LPBanner *holder = CN1_HANDOVER([[CN1LPBanner alloc] init]);
        holder.view = bannerView;
        holder.delegate = CN1_HANDOVER([[CN1LPBannerDelegate alloc] init]);
        holder.delegate.handle = param;
        [bannerView setDelegate:holder.delegate];
        cn1Banners[@(param)] = holder;
    });
    // LPMBannerAdView is a UIView, so it is the peer itself.
    return (BRIDGE_CAST void*)bannerView;
}

-(void)loadBanner:(int)param param1:(NSString*)param1 param2:(NSString*)param2 param3:(BOOL)param3 {
    CN1LPBanner *holder = cn1Banners[@(param)];
    if (holder == nil) { return; }
    dispatch_async(dispatch_get_main_queue(), ^{
        [holder.view loadAdWithViewController:cn1RootController()];
    });
}

-(void)disposeBanner:(int)param {
    CN1LPBanner *holder = cn1Banners[@(param)];
    if (holder == nil) { return; }
    // The dictionary is the holder's only owner under MRR, so removing the
    // entry first would deallocate it before the block below is copied and
    // leave that block holding a dangling pointer. Reading the view out here
    // means the block captures -- and so retains -- the object it needs, and
    // the entry can go immediately afterwards rather than inside the block,
    // which would let a banner recreated on the same handle be removed by a
    // disposal still in flight.
    LPMBannerAdView *view = holder.view;
    dispatch_async(dispatch_get_main_queue(), ^{
        [view destroy];
    });
    [cn1Banners removeObjectForKey:@(param)];
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
