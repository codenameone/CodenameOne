        #import "com_codename1_ai_tflite_NativeInterpreterImpl.h"
        #import <UIKit/UIKit.h>
        #import <TensorFlowLiteObjC/TFLTensorFlowLite.h>


        @implementation com_codename1_ai_tflite_NativeInterpreterImpl

        - (NSData *)run:(NSData *)modelBytes :(NSData *)input :(int)outputLength {
    NSError *err = nil;
    NSString *modelPath = [NSString stringWithFormat:@"%@/tflite-%@.tflite",
                           NSTemporaryDirectory(), [[NSUUID UUID] UUIDString]];
    [modelBytes writeToFile:modelPath atomically:YES];
    TFLInterpreter *interp = [[TFLInterpreter alloc] initWithModelPath:modelPath error:&err];
    if (err) return [NSData data];
    [interp allocateTensorsWithError:&err];
    if (err) return [NSData data];
    TFLTensor *in0 = [interp inputTensorAtIndex:0 error:&err];
    [in0 copyData:input error:&err];
    [interp invokeWithError:&err];
    TFLTensor *out0 = [interp outputTensorAtIndex:0 error:&err];
    NSData *outBytes = [out0 dataWithError:&err];
    return outBytes ?: [NSData data];
}


        -(BOOL)isSupported {
            return YES;
        }

        @end
