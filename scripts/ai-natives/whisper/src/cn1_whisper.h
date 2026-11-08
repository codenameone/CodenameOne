/*
 * Codename One's C API over whisper.cpp. The ports call only this, never
 * whisper.h: whisper_full_params is a large struct whose layout changes
 * between releases, and a port that declared it by hand (as the iOS native
 * once did, with twelve ints) passes garbage to whisper_full. The
 * implementation is compiled into the same artifact as the whisper.cpp
 * revision it was written for, and the iOS archive exports these symbols and
 * nothing else.
 */
#ifndef CN1_WHISPER_H
#define CN1_WHISPER_H

#ifdef __cplusplus
extern "C" {
#endif

/* Transcribes a 16-bit PCM WAV file (any channel count, 16 kHz expected).
 * Returns malloc'd UTF-8 to release with cn1whisper_free: the plain text, or
 * with segments != 0 one "<startMs>\t<endMs>\t<text>\n" line per segment
 * (backslash, tab, CR and LF in the text escaped). Returns "" on failure and
 * NULL only when out of memory. Blocks for the whole transcription. */
char *cn1whisper_transcribe(const char *model_path, const char *wav_path, int segments);
void cn1whisper_free(char *text);

#ifdef __cplusplus
}
#endif
#endif
