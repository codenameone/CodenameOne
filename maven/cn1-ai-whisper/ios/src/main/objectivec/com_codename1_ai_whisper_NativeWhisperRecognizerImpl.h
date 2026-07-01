#import <Foundation/Foundation.h>

@interface com_codename1_ai_whisper_NativeWhisperRecognizerImpl : NSObject {
}

-(NSString*)transcribe:(NSString*)param param1:(NSString*)param1;
-(NSString*)transcribeSegments:(NSString*)param param1:(NSString*)param1;
-(NSString*)escapeSegmentText:(NSString*)text;
-(BOOL)isSupported;
@end
