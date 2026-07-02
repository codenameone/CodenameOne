#import "com_codename1_ai_whisper_NativeWhisperRecognizerImpl.h"
#import <UIKit/UIKit.h>
#import <stdint.h>

// whisper.cpp's C API. The cn1lib bundles the prebuilt static
// library; linking against `libwhisper.a` is handled by the build
// server (see codenameone_library_required.properties).
struct whisper_context;

struct whisper_full_params {
    int strategy;
    int n_threads;
    int n_max_text_ctx;
    int offset_ms;
    int duration_ms;
    int translate;
    int no_context;
    int single_segment;
    int print_special;
    int print_progress;
    int print_realtime;
    int print_timestamps;
};

extern struct whisper_context *whisper_init_from_file(const char *path);
extern int whisper_full(struct whisper_context *ctx,
                        struct whisper_full_params params,
                        const float *samples, int n_samples);
extern int whisper_full_n_segments(struct whisper_context *ctx);
extern const char *whisper_full_get_segment_text(struct whisper_context *ctx, int i);
extern int64_t whisper_full_get_segment_t0(struct whisper_context *ctx, int i);
extern int64_t whisper_full_get_segment_t1(struct whisper_context *ctx, int i);
extern void whisper_free(struct whisper_context *ctx);

@implementation com_codename1_ai_whisper_NativeWhisperRecognizerImpl

-(NSString*)transcribe:(NSString*)param param1:(NSString*)param1 {
    // Decode 16kHz mono PCM samples from a WAV file.
    NSData *wav = [NSData dataWithContentsOfFile:param1];
    if (wav.length < 44) return @"";
    const uint8_t *bytes = wav.bytes;
    const int16_t *samples16 = (const int16_t *)(bytes + 44);
    NSInteger nSamples = (wav.length - 44) / 2;
    float *samples = (float *)malloc(sizeof(float) * nSamples);
    for (NSInteger i = 0; i < nSamples; i++) samples[i] = samples16[i] / 32768.0f;
    struct whisper_context *ctx = whisper_init_from_file([param UTF8String]);
    if (!ctx) { free(samples); return @""; }
    struct whisper_full_params p = {0};
    p.n_threads = 4;
    whisper_full(ctx, p, samples, (int)nSamples);
    NSMutableString *out = [NSMutableString string];
    int n = whisper_full_n_segments(ctx);
    for (int i = 0; i < n; i++) {
        [out appendString:[NSString stringWithUTF8String:
                            whisper_full_get_segment_text(ctx, i)]];
    }
    whisper_free(ctx);
    free(samples);
    return out;
}

-(NSString*)transcribeSegments:(NSString*)param param1:(NSString*)param1 {
    NSData *wav = [NSData dataWithContentsOfFile:param1];
    if (wav.length < 44) return @"";
    const uint8_t *bytes = wav.bytes;
    const int16_t *samples16 = (const int16_t *)(bytes + 44);
    NSInteger nSamples = (wav.length - 44) / 2;
    float *samples = (float *)malloc(sizeof(float) * nSamples);
    for (NSInteger i = 0; i < nSamples; i++) samples[i] = samples16[i] / 32768.0f;
    struct whisper_context *ctx = whisper_init_from_file([param UTF8String]);
    if (!ctx) { free(samples); return @""; }
    struct whisper_full_params p = {0};
    p.n_threads = 4;
    whisper_full(ctx, p, samples, (int)nSamples);
    NSMutableString *out = [NSMutableString string];
    int n = whisper_full_n_segments(ctx);
    for (int i = 0; i < n; i++) {
        const char *text = whisper_full_get_segment_text(ctx, i);
        NSString *s = text ? [NSString stringWithUTF8String:text] : @"";
        long long startMs = (long long)whisper_full_get_segment_t0(ctx, i) * 10LL;
        long long endMs = (long long)whisper_full_get_segment_t1(ctx, i) * 10LL;
        [out appendFormat:@"%lld\t%lld\t%@\n", startMs, endMs, [self escapeSegmentText:s]];
    }
    whisper_free(ctx);
    free(samples);
    return out;
}

-(NSString*)escapeSegmentText:(NSString*)text {
    NSMutableString *out = [NSMutableString string];
    for (NSUInteger i = 0; i < text.length; i++) {
        unichar ch = [text characterAtIndex:i];
        switch (ch) {
            case '\\': [out appendString:@"\\\\"]; break;
            case '\t': [out appendString:@"\\t"]; break;
            case '\n': [out appendString:@"\\n"]; break;
            case '\r': [out appendString:@"\\r"]; break;
            default: [out appendFormat:@"%C", ch]; break;
        }
    }
    return out;
}

-(BOOL)isSupported{
    return YES;
}

@end
