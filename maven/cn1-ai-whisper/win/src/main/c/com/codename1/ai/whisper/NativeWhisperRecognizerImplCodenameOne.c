#include "cn1_globals.h"

#include <stdint.h>
#include <stdio.h>
#include <stdlib.h>
#include <string.h>

#ifdef _WIN32
#include <windows.h>
#else
#include <dlfcn.h>
#endif

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

extern const char* stringToUTF8(CODENAME_ONE_THREAD_STATE, JAVA_OBJECT str);
extern JAVA_OBJECT newStringFromCString(CODENAME_ONE_THREAD_STATE, const char *str);

typedef struct whisper_context* (*cn1_whisper_init_from_file_fn)(const char*);
typedef int (*cn1_whisper_full_fn)(struct whisper_context*, struct whisper_full_params, const float*, int);
typedef int (*cn1_whisper_full_n_segments_fn)(struct whisper_context*);
typedef const char* (*cn1_whisper_full_get_segment_text_fn)(struct whisper_context*, int);
typedef int64_t (*cn1_whisper_full_get_segment_t_fn)(struct whisper_context*, int);
typedef void (*cn1_whisper_free_fn)(struct whisper_context*);

static cn1_whisper_init_from_file_fn cn1_whisper_init_from_file;
static cn1_whisper_full_fn cn1_whisper_full;
static cn1_whisper_full_n_segments_fn cn1_whisper_full_n_segments;
static cn1_whisper_full_get_segment_text_fn cn1_whisper_full_get_segment_text;
static cn1_whisper_full_get_segment_t_fn cn1_whisper_full_get_segment_t0;
static cn1_whisper_full_get_segment_t_fn cn1_whisper_full_get_segment_t1;
static cn1_whisper_free_fn cn1_whisper_free;
static int cn1_whisper_load_attempted;
static int cn1_whisper_loaded;

static void* cn1w_symbol(void* lib, const char* name) {
#ifdef _WIN32
    return (void*)GetProcAddress((HMODULE)lib, name);
#else
    return dlsym(lib, name);
#endif
}

static void* cn1w_open_library() {
#ifdef _WIN32
    void* lib = (void*)LoadLibraryA("whisper.dll");
    if (lib == NULL) {
        lib = (void*)LoadLibraryA("libwhisper.dll");
    }
    return lib;
#else
    void* lib = dlopen("libwhisper.so", RTLD_NOW | RTLD_LOCAL);
    if (lib == NULL) {
        lib = dlopen("./libwhisper.so", RTLD_NOW | RTLD_LOCAL);
    }
    return lib;
#endif
}

static int cn1w_load_whisper() {
    void* lib;
    if (cn1_whisper_load_attempted) {
        return cn1_whisper_loaded;
    }
    cn1_whisper_load_attempted = 1;
    lib = cn1w_open_library();
    if (lib == NULL) {
        return 0;
    }
    cn1_whisper_init_from_file = (cn1_whisper_init_from_file_fn)cn1w_symbol(lib, "whisper_init_from_file");
    cn1_whisper_full = (cn1_whisper_full_fn)cn1w_symbol(lib, "whisper_full");
    cn1_whisper_full_n_segments = (cn1_whisper_full_n_segments_fn)cn1w_symbol(lib, "whisper_full_n_segments");
    cn1_whisper_full_get_segment_text = (cn1_whisper_full_get_segment_text_fn)cn1w_symbol(lib, "whisper_full_get_segment_text");
    cn1_whisper_full_get_segment_t0 = (cn1_whisper_full_get_segment_t_fn)cn1w_symbol(lib, "whisper_full_get_segment_t0");
    cn1_whisper_full_get_segment_t1 = (cn1_whisper_full_get_segment_t_fn)cn1w_symbol(lib, "whisper_full_get_segment_t1");
    cn1_whisper_free = (cn1_whisper_free_fn)cn1w_symbol(lib, "whisper_free");
    cn1_whisper_loaded = cn1_whisper_init_from_file != NULL
            && cn1_whisper_full != NULL
            && cn1_whisper_full_n_segments != NULL
            && cn1_whisper_full_get_segment_text != NULL
            && cn1_whisper_full_get_segment_t0 != NULL
            && cn1_whisper_full_get_segment_t1 != NULL
            && cn1_whisper_free != NULL;
    return cn1_whisper_loaded;
}

static char* cn1w_strdup(const char* text) {
    size_t len;
    char* out;
    if (text == NULL) {
        return NULL;
    }
    len = strlen(text);
    out = (char*)malloc(len + 1);
    if (out != NULL) {
        memcpy(out, text, len + 1);
    }
    return out;
}

static float* cn1w_read_wav16(const char* path, int* sample_count) {
    FILE* f;
    long size;
    unsigned char* bytes;
    int count;
    float* samples;
    int i;
    *sample_count = 0;
    if (path == NULL) {
        return NULL;
    }
    f = fopen(path, "rb");
    if (f == NULL) {
        return NULL;
    }
    if (fseek(f, 0, SEEK_END) != 0) {
        fclose(f);
        return NULL;
    }
    size = ftell(f);
    if (size < 44) {
        fclose(f);
        return NULL;
    }
    if (fseek(f, 44, SEEK_SET) != 0) {
        fclose(f);
        return NULL;
    }
    bytes = (unsigned char*)malloc((size_t)(size - 44));
    if (bytes == NULL) {
        fclose(f);
        return NULL;
    }
    if (fread(bytes, 1, (size_t)(size - 44), f) != (size_t)(size - 44)) {
        free(bytes);
        fclose(f);
        return NULL;
    }
    fclose(f);
    count = (int)((size - 44) / 2);
    samples = (float*)malloc(sizeof(float) * (size_t)count);
    if (samples == NULL) {
        free(bytes);
        return NULL;
    }
    for (i = 0; i < count; i++) {
        int16_t value = (int16_t)((uint16_t)bytes[i * 2] | ((uint16_t)bytes[i * 2 + 1] << 8));
        samples[i] = (float)value / 32768.0f;
    }
    free(bytes);
    *sample_count = count;
    return samples;
}

static int cn1w_reserve(char** buffer, size_t* capacity, size_t needed) {
    char* next;
    size_t cap = *capacity == 0 ? 256 : *capacity;
    while (cap < needed) {
        cap *= 2;
    }
    if (cap == *capacity) {
        return 1;
    }
    next = (char*)realloc(*buffer, cap);
    if (next == NULL) {
        free(*buffer);
        *buffer = NULL;
        *capacity = 0;
        return 0;
    }
    *buffer = next;
    *capacity = cap;
    return 1;
}

static int cn1w_append(char** buffer, size_t* length, size_t* capacity, const char* text) {
    size_t n = text == NULL ? 0 : strlen(text);
    if (!cn1w_reserve(buffer, capacity, *length + n + 1)) {
        return 0;
    }
    if (n > 0) {
        memcpy(*buffer + *length, text, n);
        *length += n;
    }
    (*buffer)[*length] = 0;
    return 1;
}

static int cn1w_append_escaped(char** buffer, size_t* length, size_t* capacity, const char* text) {
    const unsigned char* p = (const unsigned char*)(text == NULL ? "" : text);
    while (*p != 0) {
        char ch = (char)*p++;
        const char* replacement = NULL;
        if (ch == '\\') {
            replacement = "\\\\";
        } else if (ch == '\t') {
            replacement = "\\t";
        } else if (ch == '\n') {
            replacement = "\\n";
        } else if (ch == '\r') {
            replacement = "\\r";
        }
        if (replacement != NULL) {
            if (!cn1w_append(buffer, length, capacity, replacement)) {
                return 0;
            }
        } else {
            if (!cn1w_reserve(buffer, capacity, *length + 2)) {
                return 0;
            }
            (*buffer)[(*length)++] = ch;
            (*buffer)[*length] = 0;
        }
    }
    return 1;
}

static struct whisper_context* cn1w_run(const char* model_path, const char* audio_path) {
    int sample_count = 0;
    float* samples;
    struct whisper_context* ctx;
    struct whisper_full_params params;
    if (!cn1w_load_whisper()) {
        return NULL;
    }
    samples = cn1w_read_wav16(audio_path, &sample_count);
    if (samples == NULL || sample_count <= 0) {
        free(samples);
        return NULL;
    }
    ctx = cn1_whisper_init_from_file(model_path);
    if (ctx == NULL) {
        free(samples);
        return NULL;
    }
    memset(&params, 0, sizeof(params));
    params.n_threads = 4;
    if (cn1_whisper_full(ctx, params, samples, sample_count) != 0) {
        cn1_whisper_free(ctx);
        free(samples);
        return NULL;
    }
    free(samples);
    return ctx;
}

JAVA_BOOLEAN com_codename1_ai_whisper_NativeWhisperRecognizerImplCodenameOne_isSupported___R_boolean(CODENAME_ONE_THREAD_STATE) {
    return cn1w_load_whisper() ? JAVA_TRUE : JAVA_FALSE;
}

JAVA_OBJECT com_codename1_ai_whisper_NativeWhisperRecognizerImplCodenameOne_transcribe___java_lang_String_java_lang_String_R_java_lang_String(CODENAME_ONE_THREAD_STATE, JAVA_OBJECT param1, JAVA_OBJECT param2) {
    char* model_path = param1 == JAVA_NULL ? NULL : cn1w_strdup(stringToUTF8(threadStateData, param1));
    char* audio_path = param2 == JAVA_NULL ? NULL : cn1w_strdup(stringToUTF8(threadStateData, param2));
    struct whisper_context* ctx = cn1w_run(model_path, audio_path);
    char* buffer = NULL;
    size_t length = 0;
    size_t capacity = 0;
    int i;
    int n;
    JAVA_OBJECT result;
    if (ctx == NULL) {
        free(model_path);
        free(audio_path);
        return newStringFromCString(threadStateData, "");
    }
    n = cn1_whisper_full_n_segments(ctx);
    for (i = 0; i < n; i++) {
        cn1w_append(&buffer, &length, &capacity, cn1_whisper_full_get_segment_text(ctx, i));
    }
    result = newStringFromCString(threadStateData, buffer == NULL ? "" : buffer);
    free(buffer);
    cn1_whisper_free(ctx);
    free(model_path);
    free(audio_path);
    return result;
}

JAVA_OBJECT com_codename1_ai_whisper_NativeWhisperRecognizerImplCodenameOne_transcribeSegments___java_lang_String_java_lang_String_R_java_lang_String(CODENAME_ONE_THREAD_STATE, JAVA_OBJECT param1, JAVA_OBJECT param2) {
    char* model_path = param1 == JAVA_NULL ? NULL : cn1w_strdup(stringToUTF8(threadStateData, param1));
    char* audio_path = param2 == JAVA_NULL ? NULL : cn1w_strdup(stringToUTF8(threadStateData, param2));
    struct whisper_context* ctx = cn1w_run(model_path, audio_path);
    char* buffer = NULL;
    size_t length = 0;
    size_t capacity = 0;
    int i;
    int n;
    JAVA_OBJECT result;
    if (ctx == NULL) {
        free(model_path);
        free(audio_path);
        return newStringFromCString(threadStateData, "");
    }
    n = cn1_whisper_full_n_segments(ctx);
    for (i = 0; i < n; i++) {
        char line[64];
        long long start_ms = (long long)cn1_whisper_full_get_segment_t0(ctx, i) * 10LL;
        long long end_ms = (long long)cn1_whisper_full_get_segment_t1(ctx, i) * 10LL;
        snprintf(line, sizeof(line), "%lld\t%lld\t", start_ms, end_ms);
        cn1w_append(&buffer, &length, &capacity, line);
        cn1w_append_escaped(&buffer, &length, &capacity, cn1_whisper_full_get_segment_text(ctx, i));
        cn1w_append(&buffer, &length, &capacity, "\n");
    }
    result = newStringFromCString(threadStateData, buffer == NULL ? "" : buffer);
    free(buffer);
    cn1_whisper_free(ctx);
    free(model_path);
    free(audio_path);
    return result;
}
