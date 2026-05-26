#import "com_codename1_ai_mlkit_docscan_NativeDocumentScannerImpl.h"
#import <UIKit/UIKit.h>
#import <CoreImage/CoreImage.h>

@implementation com_codename1_ai_mlkit_docscan_NativeDocumentScannerImpl

// VisionKit-based fallback: Apple's VNDocumentCameraViewController is
// interactive; this bridge accepts a pre-captured image and returns its
// cropped JPEG path. On iOS 13+ VisionKit handles the live UI flow; the
// sample app drives that flow and feeds the bytes into the cn1lib.
- (NSString *)scanToFile:(NSData *)imageBytes {
    UIImage *image = [UIImage imageWithData:imageBytes];
    if (!image) return @"";
    CIImage *ci = [CIImage imageWithCGImage:image.CGImage];
    CIContext *ctx = [CIContext context];
    CIDetector *det = [CIDetector detectorOfType:CIDetectorTypeRectangle context:ctx
                                          options:@{CIDetectorAccuracy: CIDetectorAccuracyHigh}];
    NSArray *features = [det featuresInImage:ci];
    UIImage *cropped = image;
    if (features.count > 0) {
        CIRectangleFeature *rf = (CIRectangleFeature *)features.firstObject;
        CIImage *flat = [ci imageByApplyingFilter:@"CIPerspectiveCorrection" withInputParameters:@{
            @"inputTopLeft":     [CIVector vectorWithCGPoint:rf.topLeft],
            @"inputTopRight":    [CIVector vectorWithCGPoint:rf.topRight],
            @"inputBottomLeft":  [CIVector vectorWithCGPoint:rf.bottomLeft],
            @"inputBottomRight": [CIVector vectorWithCGPoint:rf.bottomRight]
        }];
        CGImageRef cg = [ctx createCGImage:flat fromRect:flat.extent];
        cropped = [UIImage imageWithCGImage:cg];
        CGImageRelease(cg);
    }
    NSString *path = [NSString stringWithFormat:@"%@/docscan-%@.jpg",
                      NSTemporaryDirectory(), [[NSUUID UUID] UUIDString]];
    [UIImageJPEGRepresentation(cropped, 0.92) writeToFile:path atomically:YES];
    return path;
}

- (BOOL)isSupported {
    return YES;
}

@end
