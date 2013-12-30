//#define DETECT_HEADPHONE2
#ifdef DETECT_HEADPHONE2
#import <Foundation/Foundation.h>


@interface HeadphonesDetector : NSObject {
}

@property (nonatomic, readonly) BOOL headphonesArePlugged;

+ (HeadphonesDetector *) sharedDetector;

@end
#endif