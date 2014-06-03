//#define DETECT_HEADPHONE
#ifdef DETECT_HEADPHONE
#import "HeadphonesDetector.h"
#import "AudioToolbox/AudioToolbox.h"
#import "CodenameOne_GLViewController.h"
#include "com_codename1_impl_ios_IOSImplementation.h"

static HeadphonesDetector *headphonesDetector;

@implementation HeadphonesDetector

void audioRouteChangeListenerCallback (void *inUserData, AudioSessionPropertyID inPropertyID,
									   UInt32 inPropertyValueSize, const void *inPropertyValue);

@dynamic headphonesArePlugged;

+ (HeadphonesDetector *) sharedDetector {
	if (headphonesDetector == nil) {
		headphonesDetector = [ [self alloc] init];
	}
	return headphonesDetector;
}

- (BOOL) headphonesArePlugged {
	BOOL result = NO;
	CFStringRef route;
	UInt32 propertySize = sizeof(CFStringRef);
	if (AudioSessionGetProperty(kAudioSessionProperty_AudioRoute, &propertySize, &route) == 0)	{
		NSString *routeString = (NSString *) route;
		if ([routeString isEqualToString: @"Headphone"] == YES) {
			result = YES;
		}
	}
	return result;
}

- (id) init {
	if (self = [super init]) {
		AudioSessionInitialize(NULL, NULL, NULL, NULL);
		AudioSessionAddPropertyListener(kAudioSessionProperty_AudioRouteChange, audioRouteChangeListenerCallback, self);
        
		return self;
	}
	return nil;
}

- (void) dealloc {
	[super dealloc];
}

void audioRouteChangeListenerCallback (void *inUserData, AudioSessionPropertyID inPropertyID,
									   UInt32 inPropertyValueSize, const void *inPropertyValue) {
	CFDictionaryRef routeChangeDictionary = inPropertyValue;
	CFNumberRef routeChangeReasonRef = CFDictionaryGetValue (routeChangeDictionary, CFSTR(kAudioSession_AudioRouteChangeKey_Reason));
	SInt32 routeChangeReason;
	CFNumberGetValue(routeChangeReasonRef, kCFNumberSInt32Type, &routeChangeReason);
    
	if (routeChangeReason == kAudioSessionRouteChangeReason_OldDeviceUnavailable) {
            com_codename1_impl_ios_IOSImplementation_headphonesDisconnected__(CN1_THREAD_GET_STATE_PASS_SINGLE_ARG);
            return;
	}
	if (routeChangeReason == kAudioSessionRouteChangeReason_NewDeviceAvailable) {
            com_codename1_impl_ios_IOSImplementation_headphonesConnected__(CN1_THREAD_GET_STATE_PASS_SINGLE_ARG);
            return;
	}
}

@end
#endif
