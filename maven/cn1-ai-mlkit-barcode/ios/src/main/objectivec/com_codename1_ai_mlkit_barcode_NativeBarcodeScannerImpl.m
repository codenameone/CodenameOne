#import "com_codename1_ai_mlkit_barcode_NativeBarcodeScannerImpl.h"
#import <UIKit/UIKit.h>
#import <MLKitBarcodeScanning/MLKitBarcodeScanning.h>
#import <MLKitVision/MLKitVision.h>
#import <arpa/inet.h>

@implementation com_codename1_ai_mlkit_barcode_NativeBarcodeScannerImpl

-(NSData*)scan:(NSData*)param {
    UIImage *image = [UIImage imageWithData:param];
    if (!image) return [self packStrings:@[]];
    MLKVisionImage *vision = [[MLKVisionImage alloc] initWithImage:image];
    MLKBarcodeScannerOptions *opts = [[MLKBarcodeScannerOptions alloc] init];
    MLKBarcodeScanner *scanner = [MLKBarcodeScanner barcodeScannerWithOptions:opts];
    __block NSArray<NSString *> *values = @[];
    dispatch_semaphore_t sem = dispatch_semaphore_create(0);
    [scanner processImage:vision completion:^(NSArray<MLKBarcode *> * _Nullable barcodes,
                                               NSError * _Nullable error) {
        NSMutableArray *m = [NSMutableArray array];
        for (MLKBarcode *b in barcodes ?: @[]) {
            if (b.rawValue) [m addObject:b.rawValue];
        }
        values = m;
        dispatch_semaphore_signal(sem);
    }];
    dispatch_semaphore_wait(sem, DISPATCH_TIME_FOREVER);
    return [self packStrings:values];
}

-(NSData*)packStrings:(NSArray<NSString *> *)strings {
    // Encode as length-prefixed UTF-8 (network byte order int + bytes).
    NSMutableData *out = [NSMutableData data];
    uint32_t count = htonl((uint32_t)strings.count);
    [out appendBytes:&count length:sizeof(count)];
    for (NSString *s in strings) {
        NSData *u = [s dataUsingEncoding:NSUTF8StringEncoding];
        uint32_t len = htonl((uint32_t)u.length);
        [out appendBytes:&len length:sizeof(len)];
        [out appendData:u];
    }
    return out;
}

-(BOOL)isSupported{
    return YES;
}

@end
