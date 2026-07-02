#include <jni.h>

#include <algorithm>
#include <cstdint>
#include <cstdio>
#include <cstring>
#include <string>
#include <thread>
#include <vector>

#include "whisper.h"

namespace {

class JStringUtf {
public:
    JStringUtf(JNIEnv* env, jstring value) : env_(env), value_(value), chars_(nullptr) {
        if (value_ != nullptr) {
            chars_ = env_->GetStringUTFChars(value_, nullptr);
        }
    }

    ~JStringUtf() {
        if (chars_ != nullptr) {
            env_->ReleaseStringUTFChars(value_, chars_);
        }
    }

    const char* c_str() const {
        return chars_;
    }

private:
    JNIEnv* env_;
    jstring value_;
    const char* chars_;
};

uint16_t u16le(const uint8_t* p) {
    return static_cast<uint16_t>(p[0]) | static_cast<uint16_t>(p[1] << 8);
}

uint32_t u32le(const uint8_t* p) {
    return static_cast<uint32_t>(p[0])
            | (static_cast<uint32_t>(p[1]) << 8)
            | (static_cast<uint32_t>(p[2]) << 16)
            | (static_cast<uint32_t>(p[3]) << 24);
}

bool readFile(const char* path, std::vector<uint8_t>* out) {
    if (path == nullptr) {
        return false;
    }
    FILE* f = std::fopen(path, "rb");
    if (f == nullptr) {
        return false;
    }
    std::fseek(f, 0, SEEK_END);
    long size = std::ftell(f);
    if (size <= 0) {
        std::fclose(f);
        return false;
    }
    std::fseek(f, 0, SEEK_SET);
    out->resize(static_cast<size_t>(size));
    bool ok = std::fread(out->data(), 1, out->size(), f) == out->size();
    std::fclose(f);
    return ok;
}

bool readWav16(const char* path, std::vector<float>* samples) {
    std::vector<uint8_t> bytes;
    if (!readFile(path, &bytes) || bytes.size() < 44) {
        return false;
    }
    if (std::memcmp(bytes.data(), "RIFF", 4) != 0 || std::memcmp(bytes.data() + 8, "WAVE", 4) != 0) {
        return false;
    }

    const uint8_t* fmt = nullptr;
    size_t fmtSize = 0;
    const uint8_t* data = nullptr;
    size_t dataSize = 0;
    size_t pos = 12;
    while (pos + 8 <= bytes.size()) {
        const uint8_t* chunk = bytes.data() + pos;
        uint32_t chunkSize = u32le(chunk + 4);
        size_t chunkData = pos + 8;
        if (chunkData + chunkSize > bytes.size()) {
            return false;
        }
        if (std::memcmp(chunk, "fmt ", 4) == 0) {
            fmt = bytes.data() + chunkData;
            fmtSize = chunkSize;
        } else if (std::memcmp(chunk, "data", 4) == 0) {
            data = bytes.data() + chunkData;
            dataSize = chunkSize;
        }
        pos = chunkData + chunkSize + (chunkSize & 1);
    }

    if (fmt == nullptr || fmtSize < 16 || data == nullptr || dataSize == 0) {
        return false;
    }
    uint16_t audioFormat = u16le(fmt);
    uint16_t channels = u16le(fmt + 2);
    uint16_t bitsPerSample = u16le(fmt + 14);
    if (audioFormat != 1 || channels == 0 || bitsPerSample != 16) {
        return false;
    }

    size_t frameSize = static_cast<size_t>(channels) * 2;
    size_t frameCount = dataSize / frameSize;
    samples->clear();
    samples->reserve(frameCount);
    for (size_t frame = 0; frame < frameCount; frame++) {
        int mixed = 0;
        const uint8_t* frameData = data + frame * frameSize;
        for (uint16_t ch = 0; ch < channels; ch++) {
            const uint8_t* sampleData = frameData + ch * 2;
            int16_t value = static_cast<int16_t>(u16le(sampleData));
            mixed += value;
        }
        samples->push_back(static_cast<float>(mixed / static_cast<int>(channels)) / 32768.0f);
    }
    return !samples->empty();
}

std::string escapePayloadText(const char* text) {
    std::string out;
    const char* value = text == nullptr ? "" : text;
    while (*value != 0) {
        char ch = *value++;
        switch (ch) {
            case '\\':
                out += "\\\\";
                break;
            case '\t':
                out += "\\t";
                break;
            case '\n':
                out += "\\n";
                break;
            case '\r':
                out += "\\r";
                break;
            default:
                out += ch;
                break;
        }
    }
    return out;
}

int whisperThreadCount() {
    unsigned int count = std::thread::hardware_concurrency();
    if (count == 0) {
        return 2;
    }
    return static_cast<int>(std::min(count, 4u));
}

whisper_context* runWhisper(const char* modelPath, const char* audioPath) {
    if (modelPath == nullptr || audioPath == nullptr) {
        return nullptr;
    }
    std::vector<float> samples;
    if (!readWav16(audioPath, &samples)) {
        return nullptr;
    }

    whisper_context_params contextParams = whisper_context_default_params();
    whisper_context* ctx = whisper_init_from_file_with_params(modelPath, contextParams);
    if (ctx == nullptr) {
        return nullptr;
    }

    whisper_full_params params = whisper_full_default_params(WHISPER_SAMPLING_GREEDY);
    params.n_threads = whisperThreadCount();
    params.print_progress = false;
    params.print_realtime = false;
    params.print_timestamps = false;
    params.print_special = false;

    if (whisper_full(ctx, params, samples.data(), static_cast<int>(samples.size())) != 0) {
        whisper_free(ctx);
        return nullptr;
    }
    return ctx;
}

jstring newString(JNIEnv* env, const std::string& value) {
    return env->NewStringUTF(value.c_str());
}

} // namespace

extern "C" JNIEXPORT jstring JNICALL
Java_com_codename1_ai_whisper_NativeWhisperRecognizerImpl_nativeTranscribe(
        JNIEnv* env, jobject, jstring modelPath, jstring audioPath) {
    JStringUtf model(env, modelPath);
    JStringUtf audio(env, audioPath);
    whisper_context* ctx = runWhisper(model.c_str(), audio.c_str());
    if (ctx == nullptr) {
        return newString(env, "");
    }

    std::string out;
    int segments = whisper_full_n_segments(ctx);
    for (int i = 0; i < segments; i++) {
        const char* text = whisper_full_get_segment_text(ctx, i);
        if (text != nullptr) {
            out += text;
        }
    }
    whisper_free(ctx);
    return newString(env, out);
}

extern "C" JNIEXPORT jstring JNICALL
Java_com_codename1_ai_whisper_NativeWhisperRecognizerImpl_nativeTranscribeSegments(
        JNIEnv* env, jobject, jstring modelPath, jstring audioPath) {
    JStringUtf model(env, modelPath);
    JStringUtf audio(env, audioPath);
    whisper_context* ctx = runWhisper(model.c_str(), audio.c_str());
    if (ctx == nullptr) {
        return newString(env, "");
    }

    std::string out;
    int segments = whisper_full_n_segments(ctx);
    for (int i = 0; i < segments; i++) {
        int64_t startMs = whisper_full_get_segment_t0(ctx, i) * 10LL;
        int64_t endMs = whisper_full_get_segment_t1(ctx, i) * 10LL;
        out += std::to_string(startMs);
        out += '\t';
        out += std::to_string(endMs);
        out += '\t';
        out += escapePayloadText(whisper_full_get_segment_text(ctx, i));
        out += '\n';
    }
    whisper_free(ctx);
    return newString(env, out);
}
