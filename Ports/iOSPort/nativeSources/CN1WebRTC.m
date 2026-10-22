/*
 * Copyright (c) 2026, Codename One and/or its affiliates. All rights reserved.
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

// The com.codename1.webrtc native bridge over the WebRTC framework (the
// WebRTC-SDK pod / stasel/WebRTC package): RTCPeerConnectionFactory, the
// camera capturer and RTCMTLVideoView. See IOSWebRtc.java for the Java half.
//
// Everything runs on the main queue. The video view and the capturer need
// it, and one queue owning every registry below is what lets them go
// without locks; the two natives that answer a value use dispatch_sync.
//
// The Java callbacks (com.codename1.webrtc.WebRtc.deliver* and friends) are
// kept alive BY their symbols appearing in this file: the translator's
// dead-code pass keeps a method only when its C name is in the native
// sources, so a misspelled name here would strip the Java method and ship
// the feature inert with a green build.
//
// Off (no CN1_INCLUDE_WEBRTC, or no framework headers), the natives are
// stubs and isWebRtcSupported answers false, so IOSImplementation never
// hands out the bridge and the stubs are never asked a request. They cannot
// answer one anyway: in such a build the WebRtc class was not translated,
// so its callbacks and their header do not exist.

#import "CodenameOne_GLViewController.h"

#if defined(CN1_INCLUDE_WEBRTC) && __has_include(<WebRTC/WebRTC.h>)
#define CN1_WEBRTC_HAS_SDK 1
#endif

#ifdef CN1_WEBRTC_HAS_SDK

#import <WebRTC/WebRTC.h>
#import <AVFoundation/AVFoundation.h>
#include "com_codename1_webrtc_WebRtc.h"
#import "java_lang_String.h"

extern JAVA_OBJECT fromNSString(CODENAME_ONE_THREAD_STATE, NSString *str);
extern NSString *toNSString(CODENAME_ONE_THREAD_STATE, JAVA_OBJECT str);

#if __has_feature(objc_arc)
#define CN1W_AUTORELEASE(x) (x)
#else
#define CN1W_AUTORELEASE(x) [(x) autorelease]
#endif

static JAVA_OBJECT cn1wJString(NSString *s) {
    return s == nil ? JAVA_NULL : fromNSString(getThreadLocalData(), s);
}

static NSString *cn1wNSString(JAVA_OBJECT s) {
    return s == JAVA_NULL ? @"" : toNSString(getThreadLocalData(), s);
}

static void cn1wOnMain(dispatch_block_t block) {
    if ([NSThread isMainThread]) {
        block();
    } else {
        dispatch_async(dispatch_get_main_queue(), block);
    }
}

static void cn1wOnMainSync(dispatch_block_t block) {
    if ([NSThread isMainThread]) {
        block();
    } else {
        dispatch_sync(dispatch_get_main_queue(), block);
    }
}

// ---- Upward calls (exact ParparVM names of the static WebRtc methods) ----

static void cn1wDeliverTracks(int requestId, int audio, int video, NSString *error) {
    com_codename1_webrtc_WebRtc_deliverTracks___int_int_int_java_lang_String(
            getThreadLocalData(), requestId, audio, video, cn1wJString(error));
}

static void cn1wDeliverDescription(int requestId, NSString *type, NSString *sdp, NSString *error) {
    com_codename1_webrtc_WebRtc_deliverDescription___int_java_lang_String_java_lang_String_java_lang_String(
            getThreadLocalData(), requestId, cn1wJString(type), cn1wJString(sdp),
            cn1wJString(error));
}

static void cn1wDeliverResult(int requestId, NSString *error) {
    com_codename1_webrtc_WebRtc_deliverResult___int_java_lang_String(
            getThreadLocalData(), requestId, cn1wJString(error));
}

static void cn1wIceCandidate(int peer, NSString *mid, int index, NSString *candidate) {
    com_codename1_webrtc_WebRtc_iceCandidate___int_java_lang_String_int_java_lang_String(
            getThreadLocalData(), peer, cn1wJString(mid), index, cn1wJString(candidate));
}

static void cn1wStateChanged(int peer, NSString *state) {
    com_codename1_webrtc_WebRtc_stateChanged___int_java_lang_String(
            getThreadLocalData(), peer, cn1wJString(state));
}

static void cn1wRemoteTrack(int peer, int track, BOOL video) {
    com_codename1_webrtc_WebRtc_remoteTrack___int_int_boolean(
            getThreadLocalData(), peer, track, video ? JAVA_TRUE : JAVA_FALSE);
}

// ---- Registries, owned by the main queue ----

/// A local camera track's capture side, kept for switchCamera/releaseTrack.
@interface CN1WebRtcCamera : NSObject
@property (nonatomic, retain) RTCCameraVideoCapturer *capturer;
@property (nonatomic, assign) BOOL front;
@end
@implementation CN1WebRtcCamera
@end

@interface CN1WebRtcPeer : NSObject <RTCPeerConnectionDelegate>
@property (nonatomic, assign) int handle;
@property (nonatomic, retain) RTCPeerConnection *connection;
// Candidates that arrived before the remote description was set. The SDK
// rejects a candidate it has no description for, and trickle ICE routinely
// delivers the first ones while setRemoteDescription is still running, so
// they wait here and are added when it completes -- as the API promises.
@property (nonatomic, retain) NSMutableArray *pendingCandidates;
@property (nonatomic, assign) BOOL remoteDescriptionSet;
// The handles of the remote tracks this connection produced, so closing it
// releases them: MediaTrack.release() leaves remote tracks to their peer.
@property (nonatomic, retain) NSMutableArray *remoteTracks;
@end

static RTCPeerConnectionFactory *cn1wFactory = nil;
static NSMutableDictionary *cn1wPeers = nil;    // NSNumber -> CN1WebRtcPeer
static NSMutableDictionary *cn1wTracks = nil;   // NSNumber -> RTCMediaStreamTrack
static NSMutableDictionary *cn1wCameras = nil;  // NSNumber -> CN1WebRtcCamera
static NSMutableDictionary *cn1wViews = nil;    // NSValue(view) -> NSNumber(track)
static NSMutableDictionary *cn1wViewObjects = nil; // NSValue(view) -> view
static int cn1wNextHandle = 1;
// The audio session as the app had it before a call changed it, put back when
// the last peer closes. Nil while no call holds the session.
static NSString *cn1wSavedCategory = nil;
static NSString *cn1wSavedMode = nil;
static AVAudioSessionCategoryOptions cn1wSavedOptions = 0;

static void cn1wRestoreAudioSession(void) {
    if (cn1wSavedCategory == nil) {
        return;
    }
    AVAudioSession *session = [AVAudioSession sharedInstance];
    NSError *error = nil;
    [session overrideOutputAudioPort:AVAudioSessionPortOverrideNone error:&error];
    [session setCategory:cn1wSavedCategory mode:cn1wSavedMode options:cn1wSavedOptions error:&error];
    if (error != nil) {
        NSLog(@"CN1WebRTC: restoring the audio session failed: %@", error);
    }
    cn1wSavedCategory = nil;
    cn1wSavedMode = nil;
}

static void cn1wInit(void) {
    if (cn1wFactory != nil) {
        return;
    }
    RTCInitializeSSL();
    RTCDefaultVideoEncoderFactory *enc = CN1W_AUTORELEASE([[RTCDefaultVideoEncoderFactory alloc] init]);
    RTCDefaultVideoDecoderFactory *dec = CN1W_AUTORELEASE([[RTCDefaultVideoDecoderFactory alloc] init]);
    cn1wFactory = [[RTCPeerConnectionFactory alloc] initWithEncoderFactory:enc decoderFactory:dec];
    cn1wPeers = [[NSMutableDictionary alloc] init];
    cn1wTracks = [[NSMutableDictionary alloc] init];
    cn1wCameras = [[NSMutableDictionary alloc] init];
    cn1wViews = [[NSMutableDictionary alloc] init];
    cn1wViewObjects = [[NSMutableDictionary alloc] init];
}

static int cn1wRegisterTrack(RTCMediaStreamTrack *track) {
    int handle = cn1wNextHandle++;
    [cn1wTracks setObject:track forKey:@(handle)];
    return handle;
}

static NSString *cn1wStateName(RTCPeerConnectionState state) {
    switch (state) {
        case RTCPeerConnectionStateNew: return @"new";
        case RTCPeerConnectionStateConnecting: return @"connecting";
        case RTCPeerConnectionStateConnected: return @"connected";
        case RTCPeerConnectionStateDisconnected: return @"disconnected";
        case RTCPeerConnectionStateFailed: return @"failed";
        case RTCPeerConnectionStateClosed: return @"closed";
    }
    return @"new";
}

static RTCMediaConstraints *cn1wConstraints(void) {
    return CN1W_AUTORELEASE([[RTCMediaConstraints alloc] initWithMandatoryConstraints:nil
                                                                optionalConstraints:nil]);
}

@implementation CN1WebRtcPeer

- (void)peerConnection:(RTCPeerConnection *)peerConnection didChangeSignalingState:(RTCSignalingState)stateChanged {}
- (void)peerConnection:(RTCPeerConnection *)peerConnection didAddStream:(RTCMediaStream *)stream {}
- (void)peerConnection:(RTCPeerConnection *)peerConnection didRemoveStream:(RTCMediaStream *)stream {}
- (void)peerConnectionShouldNegotiate:(RTCPeerConnection *)peerConnection {}
- (void)peerConnection:(RTCPeerConnection *)peerConnection didChangeIceConnectionState:(RTCIceConnectionState)newState {}
- (void)peerConnection:(RTCPeerConnection *)peerConnection didChangeIceGatheringState:(RTCIceGatheringState)newState {}
- (void)peerConnection:(RTCPeerConnection *)peerConnection didRemoveIceCandidates:(NSArray<RTCIceCandidate *> *)candidates {}
- (void)peerConnection:(RTCPeerConnection *)peerConnection didOpenDataChannel:(RTCDataChannel *)dataChannel {}

- (void)peerConnection:(RTCPeerConnection *)peerConnection didGenerateIceCandidate:(RTCIceCandidate *)candidate {
    int handle = self.handle;
    NSString *mid = candidate.sdpMid;
    int index = candidate.sdpMLineIndex;
    NSString *sdp = candidate.sdp;
    cn1wOnMain(^{
        cn1wIceCandidate(handle, mid, index, sdp);
    });
}

- (void)peerConnection:(RTCPeerConnection *)peerConnection didChangeConnectionState:(RTCPeerConnectionState)newState {
    int handle = self.handle;
    cn1wOnMain(^{
        cn1wStateChanged(handle, cn1wStateName(newState));
    });
}

- (void)peerConnection:(RTCPeerConnection *)peerConnection didAddReceiver:(RTCRtpReceiver *)rtpReceiver
               streams:(NSArray<RTCMediaStream *> *)mediaStreams {
    RTCMediaStreamTrack *track = rtpReceiver.track;
    if (track == nil) {
        return;
    }
    int peer = self.handle;
    cn1wOnMain(^{
        if ([cn1wPeers objectForKey:@(peer)] == nil) {
            return;
        }
        int handle = cn1wRegisterTrack(track);
        CN1WebRtcPeer *owner = [cn1wPeers objectForKey:@(peer)];
        [owner.remoteTracks addObject:@(handle)];
        cn1wRemoteTrack(peer, handle, [track.kind isEqualToString:kRTCMediaStreamTrackKindVideo]);
    });
}

@end

static void cn1wAddCandidate(RTCPeerConnection *pc, RTCIceCandidate *c) {
    [pc addIceCandidate:c completionHandler:^(NSError *error) {
        if (error != nil) {
            NSLog(@"CN1WebRTC: candidate rejected: %@", error);
        }
    }];
}

static RTCPeerConnection *cn1wPeer(int handle) {
    CN1WebRtcPeer *p = [cn1wPeers objectForKey:@(handle)];
    return p == nil ? nil : p.connection;
}

/// The capture device for a side, or nil when this device has none.
static AVCaptureDevice *cn1wCameraDevice(BOOL front) {
    AVCaptureDevicePosition want = front ? AVCaptureDevicePositionFront : AVCaptureDevicePositionBack;
    for (AVCaptureDevice *d in [RTCCameraVideoCapturer captureDevices]) {
        if (d.position == want) {
            return d;
        }
    }
    return [[RTCCameraVideoCapturer captureDevices] firstObject];
}

/// Starts capturing at the format closest to 1280x720, at most 30 fps.
static BOOL cn1wStartCapture(CN1WebRtcCamera *camera, BOOL front) {
    AVCaptureDevice *device = cn1wCameraDevice(front);
    if (device == nil) {
        return NO;
    }
    AVCaptureDeviceFormat *best = nil;
    int bestDiff = INT_MAX;
    for (AVCaptureDeviceFormat *f in [RTCCameraVideoCapturer supportedFormatsForDevice:device]) {
        CMVideoDimensions dim = CMVideoFormatDescriptionGetDimensions(f.formatDescription);
        int diff = abs(dim.width - 1280) + abs(dim.height - 720);
        if (diff < bestDiff) {
            bestDiff = diff;
            best = f;
        }
    }
    if (best == nil) {
        return NO;
    }
    Float64 fps = 30;
    for (AVFrameRateRange *r in best.videoSupportedFrameRateRanges) {
        if (r.maxFrameRate < fps) {
            fps = r.maxFrameRate;
        }
    }
    [camera.capturer startCaptureWithDevice:device format:best fps:(NSInteger)fps];
    camera.front = front;
    return YES;
}

static void cn1wCreateTracks(int requestId, BOOL audio, BOOL video, BOOL front) {
    cn1wInit();
    int audioHandle = 0;
    int videoHandle = 0;
    if (audio) {
        RTCAudioTrack *track = [cn1wFactory audioTrackWithTrackId:[[NSUUID UUID] UUIDString]];
        audioHandle = cn1wRegisterTrack(track);
    }
    if (video) {
        RTCVideoSource *source = [cn1wFactory videoSource];
        CN1WebRtcCamera *camera = CN1W_AUTORELEASE([[CN1WebRtcCamera alloc] init]);
        camera.capturer = CN1W_AUTORELEASE([[RTCCameraVideoCapturer alloc] initWithDelegate:source]);
        if (!cn1wStartCapture(camera, front)) {
            if (audioHandle != 0) {
                [cn1wTracks removeObjectForKey:@(audioHandle)];
            }
            cn1wDeliverTracks(requestId, 0, 0, @"No camera is available");
            return;
        }
        RTCVideoTrack *track = [cn1wFactory videoTrackWithSource:source trackId:[[NSUUID UUID] UUIDString]];
        videoHandle = cn1wRegisterTrack(track);
        [cn1wCameras setObject:camera forKey:@(videoHandle)];
    }
    cn1wDeliverTracks(requestId, audioHandle, videoHandle, nil);
}

/// Asks for one capture permission, then continues on the main queue.
static void cn1wRequestAccess(BOOL needed, AVMediaType type, void (^next)(BOOL granted)) {
    if (!needed) {
        next(YES);
        return;
    }
    void (^copied)(BOOL) = [next copy];
    [AVCaptureDevice requestAccessForMediaType:type completionHandler:^(BOOL granted) {
        dispatch_async(dispatch_get_main_queue(), ^{
            copied(granted);
        });
    }];
#if !__has_feature(objc_arc)
    [copied release];
#endif
}

// ---- Natives ----

JAVA_BOOLEAN com_codename1_impl_ios_IOSNative_isWebRtcSupported___R_boolean(
        CODENAME_ONE_THREAD_STATE, JAVA_OBJECT __cn1ThisObject) {
    return JAVA_TRUE;
}

JAVA_INT com_codename1_impl_ios_IOSNative_webRtcCreatePeerConnection___java_lang_String_boolean_R_int(
        CODENAME_ONE_THREAD_STATE, JAVA_OBJECT __cn1ThisObject, JAVA_OBJECT iceServers, JAVA_BOOLEAN relayOnly) {
    NSString *wire = cn1wNSString(iceServers);
    __block int result = -1;
    cn1wOnMainSync(^{
        cn1wInit();
        RTCConfiguration *config = CN1W_AUTORELEASE([[RTCConfiguration alloc] init]);
        NSMutableArray *servers = [NSMutableArray array];
        for (NSString *line in [wire componentsSeparatedByString:@"\n"]) {
            NSArray *fields = [line componentsSeparatedByString:@"\t"];
            NSString *urls = [[fields objectAtIndex:0]
                    stringByTrimmingCharactersInSet:[NSCharacterSet whitespaceAndNewlineCharacterSet]];
            if ([urls length] == 0) {
                continue;
            }
            NSMutableArray *list = [NSMutableArray array];
            for (NSString *u in [urls componentsSeparatedByString:@","]) {
                NSString *t = [u stringByTrimmingCharactersInSet:[NSCharacterSet whitespaceCharacterSet]];
                if ([t length] > 0) {
                    [list addObject:t];
                }
            }
            NSString *user = [fields count] > 1 ? [fields objectAtIndex:1] : @"";
            NSString *cred = [fields count] > 2 ? [fields objectAtIndex:2] : @"";
            [servers addObject:CN1W_AUTORELEASE([[RTCIceServer alloc] initWithURLStrings:list
                    username:([user length] > 0 ? user : nil)
                    credential:([cred length] > 0 ? cred : nil)])];
        }
        config.iceServers = servers;
        config.sdpSemantics = RTCSdpSemanticsUnifiedPlan;
        config.iceTransportPolicy = relayOnly ? RTCIceTransportPolicyRelay : RTCIceTransportPolicyAll;
        CN1WebRtcPeer *peer = CN1W_AUTORELEASE([[CN1WebRtcPeer alloc] init]);
        RTCPeerConnection *pc = [cn1wFactory peerConnectionWithConfiguration:config
                constraints:cn1wConstraints() delegate:peer];
        if (pc == nil) {
            return;
        }
        peer.handle = cn1wNextHandle++;
        peer.connection = pc;
        peer.pendingCandidates = CN1W_AUTORELEASE([[NSMutableArray alloc] init]);
        peer.remoteTracks = CN1W_AUTORELEASE([[NSMutableArray alloc] init]);
        [cn1wPeers setObject:peer forKey:@(peer.handle)];
        result = peer.handle;
    });
    return result;
}

void com_codename1_impl_ios_IOSNative_webRtcOpenUserMedia___int_boolean_boolean_boolean(
        CODENAME_ONE_THREAD_STATE, JAVA_OBJECT __cn1ThisObject, JAVA_INT requestId,
        JAVA_BOOLEAN audio, JAVA_BOOLEAN video, JAVA_BOOLEAN frontCamera) {
    BOOL a = audio != JAVA_FALSE, v = video != JAVA_FALSE, f = frontCamera != JAVA_FALSE;
    int req = requestId;
    cn1wOnMain(^{
        cn1wRequestAccess(a, AVMediaTypeAudio, ^(BOOL micOk) {
            if (!micOk) {
                cn1wDeliverTracks(req, 0, 0, @"Microphone permission was denied");
                return;
            }
            cn1wRequestAccess(v, AVMediaTypeVideo, ^(BOOL camOk) {
                if (!camOk) {
                    cn1wDeliverTracks(req, 0, 0, @"Camera permission was denied");
                    return;
                }
                cn1wCreateTracks(req, a, v, f);
            });
        });
    });
}

void com_codename1_impl_ios_IOSNative_webRtcAddTrack___int_int(
        CODENAME_ONE_THREAD_STATE, JAVA_OBJECT __cn1ThisObject, JAVA_INT peer, JAVA_INT track) {
    int p = peer, t = track;
    cn1wOnMain(^{
        RTCPeerConnection *pc = cn1wPeer(p);
        RTCMediaStreamTrack *mt = [cn1wTracks objectForKey:@(t)];
        if (pc != nil && mt != nil) {
            [pc addTrack:mt streamIds:@[@"cn1"]];
        }
    });
}

void com_codename1_impl_ios_IOSNative_webRtcCreateDescription___int_int_boolean(
        CODENAME_ONE_THREAD_STATE, JAVA_OBJECT __cn1ThisObject, JAVA_INT peer, JAVA_INT requestId,
        JAVA_BOOLEAN answer) {
    int p = peer, req = requestId;
    BOOL isAnswer = answer != JAVA_FALSE;
    cn1wOnMain(^{
        RTCPeerConnection *pc = cn1wPeer(p);
        if (pc == nil) {
            cn1wDeliverDescription(req, nil, nil, @"The peer connection is closed");
            return;
        }
        void (^done)(RTCSessionDescription *, NSError *) = ^(RTCSessionDescription *sdp, NSError *error) {
            NSString *type = sdp == nil ? nil : [RTCSessionDescription stringForType:sdp.type];
            NSString *text = sdp == nil ? nil : sdp.sdp;
            NSString *message = error == nil ? (sdp == nil ? @"failed" : nil) : [error localizedDescription];
            dispatch_async(dispatch_get_main_queue(), ^{
                if (message != nil) {
                    cn1wDeliverDescription(req, nil, nil, message);
                } else {
                    cn1wDeliverDescription(req, type, text, nil);
                }
            });
        };
        if (isAnswer) {
            [pc answerForConstraints:cn1wConstraints() completionHandler:done];
        } else {
            [pc offerForConstraints:cn1wConstraints() completionHandler:done];
        }
    });
}

void com_codename1_impl_ios_IOSNative_webRtcSetDescription___int_int_boolean_java_lang_String_java_lang_String(
        CODENAME_ONE_THREAD_STATE, JAVA_OBJECT __cn1ThisObject, JAVA_INT peer, JAVA_INT requestId,
        JAVA_BOOLEAN remote, JAVA_OBJECT type, JAVA_OBJECT sdp) {
    int p = peer, req = requestId;
    BOOL isRemote = remote != JAVA_FALSE;
    NSString *typeName = cn1wNSString(type);
    NSString *text = cn1wNSString(sdp);
    cn1wOnMain(^{
        RTCPeerConnection *pc = cn1wPeer(p);
        if (pc == nil) {
            cn1wDeliverResult(req, @"The peer connection is closed");
            return;
        }
        // Compared exactly: the SPI carries the lowercase wire names.
        RTCSdpType sdpType;
        if ([typeName isEqualToString:@"offer"]) {
            sdpType = RTCSdpTypeOffer;
        } else if ([typeName isEqualToString:@"answer"]) {
            sdpType = RTCSdpTypeAnswer;
        } else {
            cn1wDeliverResult(req, [@"Unknown description type: " stringByAppendingString:typeName]);
            return;
        }
        RTCSessionDescription *desc = CN1W_AUTORELEASE([[RTCSessionDescription alloc]
                initWithType:sdpType sdp:text]);
        void (^done)(NSError *) = ^(NSError *error) {
            NSString *message = error == nil ? nil : [error localizedDescription];
            dispatch_async(dispatch_get_main_queue(), ^{
                cn1wDeliverResult(req, message);
            });
        };
        if (isRemote) {
            [pc setRemoteDescription:desc completionHandler:^(NSError *error) {
                dispatch_async(dispatch_get_main_queue(), ^{
                    CN1WebRtcPeer *entry = [cn1wPeers objectForKey:@(p)];
                    if (error == nil && entry != nil) {
                        entry.remoteDescriptionSet = YES;
                        NSArray *waiting = [NSArray arrayWithArray:entry.pendingCandidates];
                        [entry.pendingCandidates removeAllObjects];
                        for (RTCIceCandidate *each in waiting) {
                            cn1wAddCandidate(entry.connection, each);
                        }
                    }
                });
                done(error);
            }];
        } else {
            [pc setLocalDescription:desc completionHandler:done];
        }
    });
}

void com_codename1_impl_ios_IOSNative_webRtcAddIceCandidate___int_java_lang_String_int_java_lang_String(
        CODENAME_ONE_THREAD_STATE, JAVA_OBJECT __cn1ThisObject, JAVA_INT peer, JAVA_OBJECT sdpMid,
        JAVA_INT sdpMLineIndex, JAVA_OBJECT candidate) {
    int p = peer, index = sdpMLineIndex;
    NSString *mid = cn1wNSString(sdpMid);
    NSString *text = cn1wNSString(candidate);
    cn1wOnMain(^{
        RTCPeerConnection *pc = cn1wPeer(p);
        if (pc == nil || [text length] == 0) {
            return;
        }
        RTCIceCandidate *c = CN1W_AUTORELEASE([[RTCIceCandidate alloc] initWithSdp:text
                sdpMLineIndex:index sdpMid:([mid length] > 0 ? mid : nil)]);
        CN1WebRtcPeer *entry = [cn1wPeers objectForKey:@(p)];
        if (!entry.remoteDescriptionSet) {
            [entry.pendingCandidates addObject:c];
            return;
        }
        cn1wAddCandidate(pc, c);
    });
}

void com_codename1_impl_ios_IOSNative_webRtcClosePeerConnection___int(
        CODENAME_ONE_THREAD_STATE, JAVA_OBJECT __cn1ThisObject, JAVA_INT peer) {
    int p = peer;
    cn1wOnMain(^{
        CN1WebRtcPeer *entry = [cn1wPeers objectForKey:@(p)];
        if (entry == nil) {
            return;
        }
        [entry.connection close];
        entry.connection.delegate = nil;
        // Its remote tracks go with it, detached from any view still showing
        // them; the views themselves are released by their components.
        for (NSNumber *t in entry.remoteTracks) {
            RTCMediaStreamTrack *mt = [cn1wTracks objectForKey:t];
            if ([mt isKindOfClass:[RTCVideoTrack class]]) {
                for (NSValue *key in [cn1wViews allKeys]) {
                    if ([[cn1wViews objectForKey:key] isEqual:t]) {
                        RTCMTLVideoView *v = [cn1wViewObjects objectForKey:key];
                        if (v != nil) {
                            [(RTCVideoTrack *)mt removeRenderer:v];
                        }
                        [cn1wViews removeObjectForKey:key];
                    }
                }
            }
            [cn1wTracks removeObjectForKey:t];
        }
        [entry.remoteTracks removeAllObjects];
        [entry.pendingCandidates removeAllObjects];
        [cn1wPeers removeObjectForKey:@(p)];
        if ([cn1wPeers count] == 0) {
            cn1wRestoreAudioSession();
        }
    });
}

void com_codename1_impl_ios_IOSNative_webRtcSetTrackEnabled___int_boolean(
        CODENAME_ONE_THREAD_STATE, JAVA_OBJECT __cn1ThisObject, JAVA_INT track, JAVA_BOOLEAN enabled) {
    int t = track;
    BOOL on = enabled != JAVA_FALSE;
    cn1wOnMain(^{
        RTCMediaStreamTrack *mt = [cn1wTracks objectForKey:@(t)];
        mt.isEnabled = on;
    });
}

void com_codename1_impl_ios_IOSNative_webRtcSwitchCamera___int(
        CODENAME_ONE_THREAD_STATE, JAVA_OBJECT __cn1ThisObject, JAVA_INT track) {
    int t = track;
    cn1wOnMain(^{
        CN1WebRtcCamera *camera = [cn1wCameras objectForKey:@(t)];
        if (camera == nil) {
            return;
        }
        BOOL next = !camera.front;
        [camera.capturer stopCaptureWithCompletionHandler:^{
            dispatch_async(dispatch_get_main_queue(), ^{
                if ([cn1wCameras objectForKey:@(t)] == camera) {
                    cn1wStartCapture(camera, next);
                }
            });
        }];
    });
}

void com_codename1_impl_ios_IOSNative_webRtcSetSpeakerphone___boolean(
        CODENAME_ONE_THREAD_STATE, JAVA_OBJECT __cn1ThisObject, JAVA_BOOLEAN on) {
    BOOL speaker = on != JAVA_FALSE;
    cn1wOnMain(^{
        AVAudioSession *session = [AVAudioSession sharedInstance];
        NSError *error = nil;
        if (cn1wSavedCategory == nil) {
            // The category and mode are AVFoundation's own constants, which
            // live as long as the process, so neither needs retaining here.
            cn1wSavedCategory = session.category;
            cn1wSavedMode = session.mode;
            cn1wSavedOptions = session.categoryOptions;
        }
        [session setCategory:AVAudioSessionCategoryPlayAndRecord
                        mode:AVAudioSessionModeVoiceChat
                     options:0
                       error:&error];
        [session overrideOutputAudioPort:(speaker ? AVAudioSessionPortOverrideSpeaker
                : AVAudioSessionPortOverrideNone) error:&error];
        if (error != nil) {
            NSLog(@"CN1WebRTC: speaker route failed: %@", error);
        }
    });
}

void com_codename1_impl_ios_IOSNative_webRtcReleaseTrack___int(
        CODENAME_ONE_THREAD_STATE, JAVA_OBJECT __cn1ThisObject, JAVA_INT track) {
    int t = track;
    cn1wOnMain(^{
        CN1WebRtcCamera *camera = [cn1wCameras objectForKey:@(t)];
        if (camera != nil) {
            [camera.capturer stopCapture];
            [cn1wCameras removeObjectForKey:@(t)];
        }
        RTCMediaStreamTrack *mt = [cn1wTracks objectForKey:@(t)];
        mt.isEnabled = NO;
        [cn1wTracks removeObjectForKey:@(t)];
    });
}

JAVA_LONG com_codename1_impl_ios_IOSNative_webRtcCreateVideoView___int_boolean_R_long(
        CODENAME_ONE_THREAD_STATE, JAVA_OBJECT __cn1ThisObject, JAVA_INT track, JAVA_BOOLEAN mirror) {
    int t = track;
    BOOL flip = mirror != JAVA_FALSE;
    __block JAVA_LONG result = 0;
    cn1wOnMainSync(^{
        cn1wInit();
        RTCMediaStreamTrack *mt = [cn1wTracks objectForKey:@(t)];
        if (![mt isKindOfClass:[RTCVideoTrack class]]) {
            return;
        }
        RTCMTLVideoView *view = CN1W_AUTORELEASE([[RTCMTLVideoView alloc] initWithFrame:CGRectZero]);
        view.videoContentMode = UIViewContentModeScaleAspectFill;
        view.clipsToBounds = YES;
        if (flip) {
            view.transform = CGAffineTransformMakeScale(-1.0, 1.0);
        }
        [(RTCVideoTrack *)mt addRenderer:view];
        NSValue *key = [NSValue valueWithPointer:(__bridge void *)view];
        // The registry holds the view, so the pointer stays valid until
        // webRtcReleaseVideoView whatever the Java peer does with it.
        [cn1wViewObjects setObject:view forKey:key];
        [cn1wViews setObject:@(t) forKey:key];
        result = (JAVA_LONG)(intptr_t)(__bridge void *)view;
    });
    return result;
}

void com_codename1_impl_ios_IOSNative_webRtcReleaseVideoView___long(
        CODENAME_ONE_THREAD_STATE, JAVA_OBJECT __cn1ThisObject, JAVA_LONG view) {
    void *ptr = (void *)(intptr_t)view;
    cn1wOnMain(^{
        NSValue *key = [NSValue valueWithPointer:ptr];
        RTCMTLVideoView *v = [cn1wViewObjects objectForKey:key];
        NSNumber *t = [cn1wViews objectForKey:key];
        if (v == nil) {
            return;
        }
        RTCMediaStreamTrack *mt = t == nil ? nil : [cn1wTracks objectForKey:t];
        if ([mt isKindOfClass:[RTCVideoTrack class]]) {
            [(RTCVideoTrack *)mt removeRenderer:v];
        }
        [cn1wViews removeObjectForKey:key];
        [cn1wViewObjects removeObjectForKey:key];
    });
}

#else /* !CN1_WEBRTC_HAS_SDK */

// Stubs. isWebRtcSupported answers false, so IOSImplementation never hands
// out the bridge and none of the rest is ever reached.

JAVA_BOOLEAN com_codename1_impl_ios_IOSNative_isWebRtcSupported___R_boolean(
        CODENAME_ONE_THREAD_STATE, JAVA_OBJECT __cn1ThisObject) {
    return JAVA_FALSE;
}

JAVA_INT com_codename1_impl_ios_IOSNative_webRtcCreatePeerConnection___java_lang_String_boolean_R_int(
        CODENAME_ONE_THREAD_STATE, JAVA_OBJECT __cn1ThisObject, JAVA_OBJECT iceServers, JAVA_BOOLEAN relayOnly) {
    return -1;
}

void com_codename1_impl_ios_IOSNative_webRtcOpenUserMedia___int_boolean_boolean_boolean(
        CODENAME_ONE_THREAD_STATE, JAVA_OBJECT __cn1ThisObject, JAVA_INT requestId,
        JAVA_BOOLEAN audio, JAVA_BOOLEAN video, JAVA_BOOLEAN frontCamera) {
}

void com_codename1_impl_ios_IOSNative_webRtcAddTrack___int_int(
        CODENAME_ONE_THREAD_STATE, JAVA_OBJECT __cn1ThisObject, JAVA_INT peer, JAVA_INT track) {
}

void com_codename1_impl_ios_IOSNative_webRtcCreateDescription___int_int_boolean(
        CODENAME_ONE_THREAD_STATE, JAVA_OBJECT __cn1ThisObject, JAVA_INT peer, JAVA_INT requestId,
        JAVA_BOOLEAN answer) {
}

void com_codename1_impl_ios_IOSNative_webRtcSetDescription___int_int_boolean_java_lang_String_java_lang_String(
        CODENAME_ONE_THREAD_STATE, JAVA_OBJECT __cn1ThisObject, JAVA_INT peer, JAVA_INT requestId,
        JAVA_BOOLEAN remote, JAVA_OBJECT type, JAVA_OBJECT sdp) {
}

void com_codename1_impl_ios_IOSNative_webRtcAddIceCandidate___int_java_lang_String_int_java_lang_String(
        CODENAME_ONE_THREAD_STATE, JAVA_OBJECT __cn1ThisObject, JAVA_INT peer, JAVA_OBJECT sdpMid,
        JAVA_INT sdpMLineIndex, JAVA_OBJECT candidate) {
}

void com_codename1_impl_ios_IOSNative_webRtcClosePeerConnection___int(
        CODENAME_ONE_THREAD_STATE, JAVA_OBJECT __cn1ThisObject, JAVA_INT peer) {
}

void com_codename1_impl_ios_IOSNative_webRtcSetTrackEnabled___int_boolean(
        CODENAME_ONE_THREAD_STATE, JAVA_OBJECT __cn1ThisObject, JAVA_INT track, JAVA_BOOLEAN enabled) {
}

void com_codename1_impl_ios_IOSNative_webRtcSwitchCamera___int(
        CODENAME_ONE_THREAD_STATE, JAVA_OBJECT __cn1ThisObject, JAVA_INT track) {
}

void com_codename1_impl_ios_IOSNative_webRtcSetSpeakerphone___boolean(
        CODENAME_ONE_THREAD_STATE, JAVA_OBJECT __cn1ThisObject, JAVA_BOOLEAN on) {
}

void com_codename1_impl_ios_IOSNative_webRtcReleaseTrack___int(
        CODENAME_ONE_THREAD_STATE, JAVA_OBJECT __cn1ThisObject, JAVA_INT track) {
}

JAVA_LONG com_codename1_impl_ios_IOSNative_webRtcCreateVideoView___int_boolean_R_long(
        CODENAME_ONE_THREAD_STATE, JAVA_OBJECT __cn1ThisObject, JAVA_INT track, JAVA_BOOLEAN mirror) {
    return 0;
}

void com_codename1_impl_ios_IOSNative_webRtcReleaseVideoView___long(
        CODENAME_ONE_THREAD_STATE, JAVA_OBJECT __cn1ThisObject, JAVA_LONG view) {
}

#endif /* CN1_WEBRTC_HAS_SDK */
