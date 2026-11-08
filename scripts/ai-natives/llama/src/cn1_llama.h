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
 * Codename One's C API over llama.cpp. The ports call only this, never
 * llama.h, so a port is never compiled against a llama.cpp struct layout it
 * did not build with: the implementation (cn1_llama.cpp) is compiled into the
 * same artifact as the llama.cpp revision it was written for, and the iOS
 * archive exports these symbols and nothing else.
 *
 * Handles are positive; 0 means failure and cn1llama_last_error(0) says why.
 * Calls on one handle must not overlap; different handles may run on
 * different threads.
 */
#ifndef CN1_LLAMA_H
#define CN1_LLAMA_H

#ifdef __cplusplus
extern "C" {
#endif

int cn1llama_open(const char *path, int n_ctx, int n_gpu_layers, int n_threads);
const char *cn1llama_last_error(int handle);
int cn1llama_start(int handle, int n_messages, const char **roles, const char **contents,
                   int max_tokens, float temperature, float top_p, int seed,
                   const char *grammar);
/* 1 and *out set to a NUL-terminated piece (valid until the next call on the
 * handle; may be "" while a UTF-8 sequence is incomplete), or 0 once done. */
int cn1llama_next(int handle, const char **out);
int cn1llama_prompt_tokens(int handle);
int cn1llama_completion_tokens(int handle);
/* The embedding's length, with *out valid until the next call; 0 on failure. */
int cn1llama_embed(int handle, const char *text, const float **out);
void cn1llama_close(int handle);

#ifdef __cplusplus
}
#endif
#endif
