#import "com_codename1_ai_mlkit_face_NativeFaceDetectorImpl.h"
#import <UIKit/UIKit.h>
#import <MLKitFaceDetection/MLKFaceDetector.h>
#import <MLKitFaceDetection/MLKFaceDetectorOptions.h>
#import <MLKitFaceDetection/MLKFace.h>
#import <MLKitVision/MLKVisionImage.h>
#import <arpa/inet.h>

@implementation com_codename1_ai_mlkit_face_NativeFaceDetectorImpl

-(NSData*)detect:(NSData*)param {
    UIImage *image = [UIImage imageWithData:param];
    if (!image) return [NSData data];
    MLKVisionImage *vision = [[MLKVisionImage alloc] initWithImage:image];
    MLKFaceDetectorOptions *opts = [[MLKFaceDetectorOptions alloc] init];
    MLKFaceDetector *det = [MLKFaceDetector faceDetectorWithOptions:opts];
    __block NSArray<MLKFace *> *faces = @[];
    dispatch_semaphore_t sem = dispatch_semaphore_create(0);
    [det processImage:vision completion:^(NSArray<MLKFace *> * _Nullable f, NSError * _Nullable e) {
        faces = f ?: @[];
        dispatch_semaphore_signal(sem);
    }];
    dispatch_semaphore_wait(sem, DISPATCH_TIME_FOREVER);
    NSMutableData *out = [NSMutableData data];
    for (MLKFace *face in faces) {
        CGRect r = face.frame;
        int32_t v[4] = { htonl((int32_t)r.origin.x), htonl((int32_t)r.origin.y),
                         htonl((int32_t)r.size.width), htonl((int32_t)r.size.height) };
        [out appendBytes:v length:sizeof(v)];
    }
    return out;
}

-(BOOL)isSupported{
    return YES;
}

@end
