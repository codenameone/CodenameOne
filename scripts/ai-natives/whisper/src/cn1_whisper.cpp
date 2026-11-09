/*
 * Copyright (c) 2026, Codename One and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.  Codename One designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Oracle in the LICENSE file that accompanied this code.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 2 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 *
 * You should have received a copy of the GNU General Public License version
 * 2 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Please contact Codename One through http://www.codenameone.com/ if you
 * need additional information or have any questions.
 */
/*
 * Implementation of cn1_whisper.h over whisper.cpp. Compiled into the same
 * artifact as the whisper.cpp revision it was written for; see cn1_whisper.h.
 */
#include "cn1_whisper.h"

#include <algorithm>
#include <cstdint>
#include <cstdio>
#include <cstdlib>
#include <cstring>
#include <string>
#include <thread>
#include <vector>

#include "whisper.h"

namespace {



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

std::string transcribe(const char* modelPath, const char* audioPath, bool segments) {
    whisper_context* ctx = runWhisper(modelPath, audioPath);
    if (ctx == nullptr) {
        return std::string();
    }
    std::string out;
    int n = whisper_full_n_segments(ctx);
    for (int i = 0; i < n; i++) {
        const char* text = whisper_full_get_segment_text(ctx, i);
        if (!segments) {
            if (text != nullptr) {
                out += text;
            }
            continue;
        }
        out += std::to_string(static_cast<long long>(whisper_full_get_segment_t0(ctx, i)) * 10LL);
        out += '\t';
        out += std::to_string(static_cast<long long>(whisper_full_get_segment_t1(ctx, i)) * 10LL);
        out += '\t';
        out += escapePayloadText(text);
        out += '\n';
    }
    whisper_free(ctx);
    return out;
}

} // namespace

extern "C" char* cn1whisper_transcribe(const char* model_path, const char* wav_path,
                                       int segments) {
    std::string out = transcribe(model_path, wav_path, segments != 0);
    char* copy = static_cast<char*>(std::malloc(out.size() + 1));
    if (copy != nullptr) {
        std::memcpy(copy, out.c_str(), out.size() + 1);
    }
    return copy;
}

extern "C" void cn1whisper_free(char* text) {
    std::free(text);
}
