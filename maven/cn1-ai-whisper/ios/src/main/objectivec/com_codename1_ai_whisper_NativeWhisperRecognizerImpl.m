#import "com_codename1_ai_whisper_NativeWhisperRecognizerImpl.h"
#include "cn1_globals.h"
#include <stdlib.h>

// whisper.cpp comes from the cn1-whisper-ios xcframework, which the build
// links for every app that depends on this cn1lib (PlatformFeatureCatalog).
// It exports Codename One's own C API over whisper.cpp,
// scripts/ai-natives/whisper/src/cn1_whisper.h, and nothing else -- these
// prototypes are that header's. Never declare whisper.h's structs here: a
// hand-written whisper_full_params is how this file once handed whisper_full
// a struct a fraction of the real size.
extern char *cn1whisper_transcribe(const char *model_path, const char *wav_path, int segments);
extern void cn1whisper_free(char *text);

@implementation com_codename1_ai_whisper_NativeWhisperRecognizerImpl

// A transcription takes seconds, and ParparVM's collector can stop a thread
// only at a safepoint, so the call runs between CN1_YIELD_THREAD and
// CN1_RESUME_THREAD. Only C strings are touched in between: the arguments
// are already NSStrings and the result becomes one after the resume.
-(NSString*)run:(NSString*)model audio:(NSString*)audio segments:(int)segments {
    if (model == nil || audio == nil) {
        return @"";
    }
    char *m = strdup([model UTF8String]);
    char *a = strdup([audio UTF8String]);
    CN1_YIELD_THREAD;
    char *text = cn1whisper_transcribe(m, a, segments);
    CN1_RESUME_THREAD;
    free(m);
    free(a);
    if (text == NULL) {
        return @"";
    }
    NSString *out = [[NSString alloc] initWithBytes:text length:strlen(text)
                                           encoding:NSUTF8StringEncoding];
    cn1whisper_free(text);
#if !__has_feature(objc_arc)
    [out autorelease];
#endif
    return out == nil ? @"" : out;
}

-(NSString*)transcribe:(NSString*)param param1:(NSString*)param1 {
    return [self run:param audio:param1 segments:0];
}

-(NSString*)transcribeSegments:(NSString*)param param1:(NSString*)param1 {
    return [self run:param audio:param1 segments:1];
}

-(BOOL)isSupported{
    return YES;
}

@end
