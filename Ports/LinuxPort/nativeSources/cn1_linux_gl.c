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
 * OpenGL ES 3.0 backend for the portable Codename One 3D API (com.codename1.gpu)
 * on the native Linux port. Rendering is offscreen (EGL surfaceless context +
 * an FBO), matching the Windows D3D backend's "render a frame, read it back as a
 * peer image" model: gl3dBeginFrame binds the FBO, the device issues draws, and
 * gl3dCaptureFrame reads the pixels back and PNG-encodes them. libepoxy supplies
 * the GLES/EGL entry points.
 *
 * The shaders come from the core com.codename1.impl.gpu.GlslShaderGenerator
 * (GLSL ES 1.00, which an ES 3.0 context accepts). LinuxGraphicsDevice joins the
 * vertex and fragment stages with "//@@CN1_FRAGMENT@@"; gl3dGetOrCreatePipeline
 * splits, compiles and links them (a_position/a_normal/a_texcoord are bound to
 * locations 0/1/2), records which optional vertex attributes the shader declared
 * and caches the named-uniform locations. Draws receive one packed float array
 * whose layout is CN1_UNIFORMS below. The shadow pass and the post process chain
 * (bright pass, blur, composite) run here against offscreen targets.
 */

#define _GNU_SOURCE
#include "cn1_linux.h"
#include <epoxy/egl.h>
#include <epoxy/gl.h>
#include <EGL/eglext.h>
#ifdef CN1_LINUX_EMBEDDED
#include "cn1_linux_raster.h"
#else
#include <cairo.h>
#endif
#include <stdlib.h>
#include <string.h>
#include <stdio.h>

/* From cn1_linux_window.c: PNG-encode an ARGB32 cairo surface. */
extern int cn1LinuxSurfaceToPng(cairo_surface_t* surface, unsigned char** outData, int* outLen);

#define CN1_STAGE_SEP "//@@CN1_FRAGMENT@@"

/* An offscreen colour target (plus optional depth) for the shadow map and the
 * post processing chain. */
typedef struct {
    GLuint fbo;
    GLuint tex;
    GLuint depth;
    int w, h;
} CN1GlTarget;

typedef struct {
    EGLDisplay dpy;
    EGLContext ctx;
    GLuint fbo;
    GLuint colorTex;
    GLuint depthRbo;
    GLuint vao;
    int w, h;
    CN1GlTarget shadow;
    CN1GlTarget scene;
    CN1GlTarget bloomA;
    CN1GlTarget bloomB;
    GLuint quadVbo;
    GLint savedFbo;
    GLint savedVp[4];
} CN1GlContext;

/* Layout of the packed uniform array a draw passes in; must match
 * LinuxGraphicsDevice.packUniforms (112 floats). A size of 16 is a mat4. */
typedef struct {
    const char* name;
    int size;
} CN1GlUniformDef;

static const CN1GlUniformDef CN1_UNIFORMS[] = {
    {"u_mvp", 16}, {"u_model", 16}, {"u_normalMatrix", 16}, {"u_shadowMatrix", 16},
    {"u_color", 4}, {"u_lightDir", 3}, {"u_lightColor", 3}, {"u_ambient", 3},
    {"u_eye", 3}, {"u_shininess", 1}, {"u_fogColor", 3}, {"u_fog", 3},
    {"u_sky", 3}, {"u_ground", 3}, {"u_exposure", 1}, {"u_pbr", 2},
    {"u_emissive", 3}, {"u_zenith", 3}, {"u_horizon", 3}, {"u_sun", 4},
    {"u_shadow", 3}
};
#define CN1_UNIFORM_COUNT ((int) (sizeof(CN1_UNIFORMS) / sizeof(CN1_UNIFORMS[0])))
#define CN1_UNIFORM_FLOATS 112

typedef struct {
    GLuint program;
    int hasNormal;
    int hasTexcoord;
    int blend, cull, depthTest, depthWrite;
    GLint loc[CN1_UNIFORM_COUNT];
    GLint uTexture;
    GLint uShadowMap;
} CN1GlPipeline;

static int cn1GlMakeCurrent(CN1GlContext* c) {
    return eglMakeCurrent(c->dpy, EGL_NO_SURFACE, EGL_NO_SURFACE, c->ctx);
}

static void cn1GlReleaseTarget(CN1GlTarget* t) {
    if (t->fbo) {
        glDeleteFramebuffers(1, &t->fbo);
    }
    if (t->tex) {
        glDeleteTextures(1, &t->tex);
    }
    if (t->depth) {
        glDeleteRenderbuffers(1, &t->depth);
    }
    memset(t, 0, sizeof(*t));
}

JAVA_LONG com_codename1_impl_linux_LinuxNative_gl3dCreateContext___R_long(CODENAME_ONE_THREAD_STATE) {
    CN1GlContext* c;
    EGLint major, minor;
    EGLConfig config;
    EGLint numConfig;
    const EGLint configAttribs[] = {
        EGL_SURFACE_TYPE, EGL_PBUFFER_BIT,
        EGL_RENDERABLE_TYPE, EGL_OPENGL_ES2_BIT,
        EGL_RED_SIZE, 8, EGL_GREEN_SIZE, 8, EGL_BLUE_SIZE, 8, EGL_ALPHA_SIZE, 8,
        EGL_DEPTH_SIZE, 16, EGL_NONE
    };
    const EGLint ctxAttribs[] = { EGL_CONTEXT_MAJOR_VERSION, 3, EGL_NONE };
    EGLDisplay dpy = EGL_NO_DISPLAY;
    const char* clientExts = eglQueryString(EGL_NO_DISPLAY, EGL_EXTENSIONS);

    /* Prefer the surfaceless Mesa platform (no X/Wayland needed -> headless GL).
     * Reach eglGetPlatformDisplayEXT through eglGetProcAddress and only when the
     * client extension is advertised, so this links and runs on an EGL 1.4 stack
     * (calling the EGL 1.5 core eglGetPlatformDisplay directly would abort there).
     * Fall back to the default display otherwise. */
    if (clientExts && strstr(clientExts, "EGL_MESA_platform_surfaceless")
            && strstr(clientExts, "EGL_EXT_platform_base")) {
        PFNEGLGETPLATFORMDISPLAYEXTPROC getPlatformDisplay =
                (PFNEGLGETPLATFORMDISPLAYEXTPROC) eglGetProcAddress("eglGetPlatformDisplayEXT");
        if (getPlatformDisplay) {
            dpy = getPlatformDisplay(EGL_PLATFORM_SURFACELESS_MESA, EGL_DEFAULT_DISPLAY, 0);
        }
    }
    if (dpy == EGL_NO_DISPLAY) {
        dpy = eglGetDisplay(EGL_DEFAULT_DISPLAY);
    }
    if (dpy == EGL_NO_DISPLAY || !eglInitialize(dpy, &major, &minor)) {
        return 0;
    }
    if (!eglBindAPI(EGL_OPENGL_ES_API)) {
        return 0;
    }
    if (!eglChooseConfig(dpy, configAttribs, &config, 1, &numConfig) || numConfig < 1) {
        return 0;
    }
    c = (CN1GlContext*) calloc(1, sizeof(CN1GlContext));
    c->dpy = dpy;
    c->ctx = eglCreateContext(dpy, config, EGL_NO_CONTEXT, ctxAttribs);
    if (c->ctx == EGL_NO_CONTEXT) {
        free(c);
        return 0;
    }
    if (!cn1GlMakeCurrent(c)) {
        eglDestroyContext(dpy, c->ctx);
        free(c);
        return 0;
    }
    glGenFramebuffers(1, &c->fbo);
    glGenTextures(1, &c->colorTex);
    glGenRenderbuffers(1, &c->depthRbo);
    glGenVertexArrays(1, &c->vao);
    return (JAVA_LONG) (intptr_t) c;
}

JAVA_VOID com_codename1_impl_linux_LinuxNative_gl3dDestroyContext___long(CODENAME_ONE_THREAD_STATE, JAVA_LONG contextPeer) {
    CN1GlContext* c = (CN1GlContext*) (intptr_t) contextPeer;
    if (!c) {
        return;
    }
    cn1GlMakeCurrent(c);
    glDeleteFramebuffers(1, &c->fbo);
    glDeleteTextures(1, &c->colorTex);
    glDeleteRenderbuffers(1, &c->depthRbo);
    cn1GlReleaseTarget(&c->shadow);
    cn1GlReleaseTarget(&c->scene);
    cn1GlReleaseTarget(&c->bloomA);
    cn1GlReleaseTarget(&c->bloomB);
    if (c->quadVbo) {
        glDeleteBuffers(1, &c->quadVbo);
    }
    glDeleteVertexArrays(1, &c->vao);
    eglMakeCurrent(c->dpy, EGL_NO_SURFACE, EGL_NO_SURFACE, EGL_NO_CONTEXT);
    eglDestroyContext(c->dpy, c->ctx);
    eglTerminate(c->dpy);
    free(c);
}

JAVA_VOID com_codename1_impl_linux_LinuxNative_gl3dBeginFrame___long_int_int(CODENAME_ONE_THREAD_STATE, JAVA_LONG contextPeer, JAVA_INT width, JAVA_INT height) {
    CN1GlContext* c = (CN1GlContext*) (intptr_t) contextPeer;
    if (!c || width <= 0 || height <= 0) {
        return;
    }
    cn1GlMakeCurrent(c);
    if (c->w != width || c->h != height) {
        glBindTexture(GL_TEXTURE_2D, c->colorTex);
        glTexImage2D(GL_TEXTURE_2D, 0, GL_RGBA, width, height, 0, GL_RGBA, GL_UNSIGNED_BYTE, 0);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_LINEAR);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_LINEAR);
        glBindRenderbuffer(GL_RENDERBUFFER, c->depthRbo);
        glRenderbufferStorage(GL_RENDERBUFFER, GL_DEPTH_COMPONENT16, width, height);
        glBindFramebuffer(GL_FRAMEBUFFER, c->fbo);
        glFramebufferTexture2D(GL_FRAMEBUFFER, GL_COLOR_ATTACHMENT0, GL_TEXTURE_2D, c->colorTex, 0);
        glFramebufferRenderbuffer(GL_FRAMEBUFFER, GL_DEPTH_ATTACHMENT, GL_RENDERBUFFER, c->depthRbo);
        c->w = width;
        c->h = height;
    }
    glBindFramebuffer(GL_FRAMEBUFFER, c->fbo);
    glViewport(0, 0, width, height);
}

/* Reads the FBO back into a freshly allocated ARGB32 (premultiplied) cairo
 * surface. Caller destroys the surface. */
static cairo_surface_t* cn1GlReadback(CN1GlContext* c) {
    int w = c->w, h = c->h;
    unsigned char* rgba;
    cairo_surface_t* surface;
    unsigned char* dst;
    int stride, x, y;
    if (w <= 0 || h <= 0) {
        return 0;
    }
    rgba = (unsigned char*) malloc((size_t) w * h * 4);
    glBindFramebuffer(GL_FRAMEBUFFER, c->fbo);
    glReadPixels(0, 0, w, h, GL_RGBA, GL_UNSIGNED_BYTE, rgba);
    surface = cairo_image_surface_create(CAIRO_FORMAT_ARGB32, w, h);
    dst = cairo_image_surface_get_data(surface);
    stride = cairo_image_surface_get_stride(surface);
    /* GL origin is bottom-left; flip vertically into the top-left cairo surface,
     * and convert straight RGBA -> premultiplied native-endian ARGB32. */
    for (y = 0; y < h; y++) {
        uint32_t* row = (uint32_t*) (dst + y * stride);
        const unsigned char* src = rgba + (size_t) (h - 1 - y) * w * 4;
        for (x = 0; x < w; x++) {
            uint32_t r = src[x * 4 + 0], g = src[x * 4 + 1], b = src[x * 4 + 2], a = src[x * 4 + 3];
            r = r * a / 255; g = g * a / 255; b = b * a / 255;
            row[x] = (a << 24) | (r << 16) | (g << 8) | b;
        }
    }
    cairo_surface_mark_dirty(surface);
    free(rgba);
    return surface;
}

JAVA_OBJECT com_codename1_impl_linux_LinuxNative_gl3dCaptureFrame___long_R_byte_1ARRAY(CODENAME_ONE_THREAD_STATE, JAVA_LONG contextPeer) {
    CN1GlContext* c = (CN1GlContext*) (intptr_t) contextPeer;
    cairo_surface_t* surface;
    unsigned char* png = 0;
    int len = 0;
    JAVA_OBJECT result;
    if (!c) {
        return JAVA_NULL;
    }
    cn1GlMakeCurrent(c);
    surface = cn1GlReadback(c);
    if (!surface || !cn1LinuxSurfaceToPng(surface, &png, &len)) {
        if (surface) cairo_surface_destroy(surface);
        return JAVA_NULL;
    }
    cairo_surface_destroy(surface);
    result = cn1LinuxNewByteArray(threadStateData, png, len);
    free(png);
    return result;
}

JAVA_BOOLEAN com_codename1_impl_linux_LinuxNative_gl3dCaptureToFile___long_java_lang_String_R_boolean(CODENAME_ONE_THREAD_STATE, JAVA_LONG contextPeer, JAVA_OBJECT path) {
    extern const char* stringToUTF8(CODENAME_ONE_THREAD_STATE, JAVA_OBJECT);
    CN1GlContext* c = (CN1GlContext*) (intptr_t) contextPeer;
    const char* p = path == JAVA_NULL ? 0 : stringToUTF8(threadStateData, path);
    cairo_surface_t* surface;
    cairo_status_t st;
    if (!c || !p) {
        return JAVA_FALSE;
    }
    cn1GlMakeCurrent(c);
    surface = cn1GlReadback(c);
    if (!surface) {
        return JAVA_FALSE;
    }
    st = cairo_surface_write_to_png(surface, p);
    cairo_surface_destroy(surface);
    return st == CAIRO_STATUS_SUCCESS ? JAVA_TRUE : JAVA_FALSE;
}

/* ------------------------------------------------------------- buffers */

JAVA_LONG com_codename1_impl_linux_LinuxNative_gl3dCreateFloatBuffer___float_1ARRAY_int_R_long(CODENAME_ONE_THREAD_STATE, JAVA_OBJECT data, JAVA_INT floatCount) {
    GLuint buf = 0;
    if (data == JAVA_NULL) {
        return 0;
    }
    glGenBuffers(1, &buf);
    glBindBuffer(GL_ARRAY_BUFFER, buf);
    glBufferData(GL_ARRAY_BUFFER, (GLsizeiptr) floatCount * 4, CN1_ARRAY_DATA(data), GL_STATIC_DRAW);
    return (JAVA_LONG) buf;
}

JAVA_VOID com_codename1_impl_linux_LinuxNative_gl3dUpdateFloatBuffer___long_float_1ARRAY_int(CODENAME_ONE_THREAD_STATE, JAVA_LONG bufferPeer, JAVA_OBJECT data, JAVA_INT floatCount) {
    if (data == JAVA_NULL) {
        return;
    }
    glBindBuffer(GL_ARRAY_BUFFER, (GLuint) bufferPeer);
    glBufferData(GL_ARRAY_BUFFER, (GLsizeiptr) floatCount * 4, CN1_ARRAY_DATA(data), GL_STATIC_DRAW);
}

JAVA_LONG com_codename1_impl_linux_LinuxNative_gl3dCreateShortBuffer___short_1ARRAY_int_R_long(CODENAME_ONE_THREAD_STATE, JAVA_OBJECT data, JAVA_INT indexCount) {
    GLuint buf = 0;
    if (data == JAVA_NULL) {
        return 0;
    }
    glGenBuffers(1, &buf);
    glBindBuffer(GL_ELEMENT_ARRAY_BUFFER, buf);
    glBufferData(GL_ELEMENT_ARRAY_BUFFER, (GLsizeiptr) indexCount * 2, CN1_ARRAY_DATA(data), GL_STATIC_DRAW);
    return (JAVA_LONG) buf;
}

JAVA_VOID com_codename1_impl_linux_LinuxNative_gl3dUpdateShortBuffer___long_short_1ARRAY_int(CODENAME_ONE_THREAD_STATE, JAVA_LONG bufferPeer, JAVA_OBJECT data, JAVA_INT indexCount) {
    if (data == JAVA_NULL) {
        return;
    }
    glBindBuffer(GL_ELEMENT_ARRAY_BUFFER, (GLuint) bufferPeer);
    glBufferData(GL_ELEMENT_ARRAY_BUFFER, (GLsizeiptr) indexCount * 2, CN1_ARRAY_DATA(data), GL_STATIC_DRAW);
}

JAVA_LONG com_codename1_impl_linux_LinuxNative_gl3dCreateTexture___int_1ARRAY_int_int_R_long(CODENAME_ONE_THREAD_STATE, JAVA_OBJECT argb, JAVA_INT width, JAVA_INT height) {
    GLuint tex = 0;
    JAVA_INT* px;
    unsigned char* rgba;
    int i, n;
    if (argb == JAVA_NULL || width <= 0 || height <= 0) {
        return 0;
    }
    px = (JAVA_INT*) CN1_ARRAY_DATA(argb);
    n = width * height;
    rgba = (unsigned char*) malloc((size_t) n * 4);
    for (i = 0; i < n; i++) {
        uint32_t p = (uint32_t) px[i];
        rgba[i * 4 + 0] = (p >> 16) & 0xff; /* R */
        rgba[i * 4 + 1] = (p >> 8) & 0xff;  /* G */
        rgba[i * 4 + 2] = p & 0xff;         /* B */
        rgba[i * 4 + 3] = (p >> 24) & 0xff; /* A */
    }
    glGenTextures(1, &tex);
    glBindTexture(GL_TEXTURE_2D, tex);
    glTexImage2D(GL_TEXTURE_2D, 0, GL_RGBA, width, height, 0, GL_RGBA, GL_UNSIGNED_BYTE, rgba);
    glGenerateMipmap(GL_TEXTURE_2D);
    free(rgba);
    return (JAVA_LONG) tex;
}

JAVA_VOID com_codename1_impl_linux_LinuxNative_gl3dDisposeBuffer___long(CODENAME_ONE_THREAD_STATE, JAVA_LONG bufferPeer) {
    GLuint b = (GLuint) bufferPeer;
    if (b) {
        glDeleteBuffers(1, &b);
    }
}

JAVA_VOID com_codename1_impl_linux_LinuxNative_gl3dDisposeTexture___long(CODENAME_ONE_THREAD_STATE, JAVA_LONG texturePeer) {
    GLuint t = (GLuint) texturePeer;
    if (t) {
        glDeleteTextures(1, &t);
    }
}

JAVA_VOID com_codename1_impl_linux_LinuxNative_gl3dDisposePipeline___long(CODENAME_ONE_THREAD_STATE, JAVA_LONG pipelinePeer) {
    CN1GlPipeline* p = (CN1GlPipeline*) (intptr_t) pipelinePeer;
    if (p) {
        glDeleteProgram(p->program);
        free(p);
    }
}

/* ----------------------------------------------------------- pipelines */

static GLuint cn1GlCompile(GLenum type, const char* src) {
    GLuint sh = glCreateShader(type);
    GLint ok = 0;
    glShaderSource(sh, 1, &src, 0);
    glCompileShader(sh);
    glGetShaderiv(sh, GL_COMPILE_STATUS, &ok);
    if (!ok) {
        char log[1024];
        glGetShaderInfoLog(sh, sizeof(log), 0, log);
        fprintf(stderr, "[cn1-linux-gl] shader compile failed: %s\n", log);
        glDeleteShader(sh);
        return 0;
    }
    return sh;
}

JAVA_LONG com_codename1_impl_linux_LinuxNative_gl3dGetOrCreatePipeline___long_java_lang_String_java_lang_String_int_int_int_int_R_long(CODENAME_ONE_THREAD_STATE, JAVA_LONG contextPeer, JAVA_OBJECT key, JAVA_OBJECT glslSource, JAVA_INT blendMode, JAVA_INT cullMode, JAVA_INT depthTest, JAVA_INT depthWrite) {
    extern const char* stringToUTF8(CODENAME_ONE_THREAD_STATE, JAVA_OBJECT);
    CN1GlContext* c = (CN1GlContext*) (intptr_t) contextPeer;
    const char* combined;
    char* vsrc;
    char* fsrc;
    char* sep;
    GLuint vs, fs, prog;
    GLint linkOk = 0;
    int i;
    CN1GlPipeline* p;
    (void) key;
    if (!c || glslSource == JAVA_NULL) {
        return 0;
    }
    cn1GlMakeCurrent(c);
    combined = stringToUTF8(threadStateData, glslSource);
    vsrc = strdup(combined);
    sep = strstr(vsrc, CN1_STAGE_SEP);
    if (!sep) {
        free(vsrc);
        return 0;
    }
    *sep = 0;
    fsrc = sep + strlen(CN1_STAGE_SEP);
    vs = cn1GlCompile(GL_VERTEX_SHADER, vsrc);
    fs = cn1GlCompile(GL_FRAGMENT_SHADER, fsrc);
    if (!vs || !fs) {
        free(vsrc);
        if (vs) glDeleteShader(vs);
        if (fs) glDeleteShader(fs);
        return 0;
    }
    prog = glCreateProgram();
    glAttachShader(prog, vs);
    glAttachShader(prog, fs);
    glBindAttribLocation(prog, 0, "a_position");
    glBindAttribLocation(prog, 1, "a_normal");
    glBindAttribLocation(prog, 2, "a_texcoord");
    glLinkProgram(prog);
    glGetProgramiv(prog, GL_LINK_STATUS, &linkOk);
    glDeleteShader(vs);
    glDeleteShader(fs);
    if (!linkOk) {
        char log[1024];
        glGetProgramInfoLog(prog, sizeof(log), 0, log);
        fprintf(stderr, "[cn1-linux-gl] program link failed: %s\n", log);
        glDeleteProgram(prog);
        free(vsrc);
        return 0;
    }
    p = (CN1GlPipeline*) calloc(1, sizeof(CN1GlPipeline));
    p->program = prog;
    for (i = 0; i < CN1_UNIFORM_COUNT; i++) {
        p->loc[i] = glGetUniformLocation(prog, CN1_UNIFORMS[i].name);
    }
    p->uTexture = glGetUniformLocation(prog, "u_texture");
    p->uShadowMap = glGetUniformLocation(prog, "u_shadowMap");
    /* Record which optional interleaved attributes the shader declared. */
    p->hasNormal = strstr(vsrc, "attribute vec3 a_normal") != 0;
    p->hasTexcoord = strstr(vsrc, "attribute vec2 a_texcoord") != 0;
    p->blend = blendMode;
    p->cull = cullMode;
    p->depthTest = depthTest;
    p->depthWrite = depthWrite;
    free(vsrc);
    return (JAVA_LONG) (intptr_t) p;
}

JAVA_VOID com_codename1_impl_linux_LinuxNative_gl3dClear___long_int_boolean_boolean(CODENAME_ONE_THREAD_STATE, JAVA_LONG contextPeer, JAVA_INT argbColor, JAVA_BOOLEAN clearColor, JAVA_BOOLEAN clearDepth) {
    CN1GlContext* c = (CN1GlContext*) (intptr_t) contextPeer;
    GLbitfield mask = 0;
    if (!c) {
        return;
    }
    cn1GlMakeCurrent(c);
    if (clearColor) {
        glClearColor(((argbColor >> 16) & 0xff) / 255.0f, ((argbColor >> 8) & 0xff) / 255.0f,
                (argbColor & 0xff) / 255.0f, ((argbColor >> 24) & 0xff) / 255.0f);
        mask |= GL_COLOR_BUFFER_BIT;
    }
    if (clearDepth) {
        glClearDepthf(1.0f);
        glDepthMask(GL_TRUE);
        mask |= GL_DEPTH_BUFFER_BIT;
    }
    glClear(mask);
}

JAVA_VOID com_codename1_impl_linux_LinuxNative_gl3dSetViewport___long_int_int_int_int(CODENAME_ONE_THREAD_STATE, JAVA_LONG contextPeer, JAVA_INT x, JAVA_INT y, JAVA_INT width, JAVA_INT height) {
    CN1GlContext* c = (CN1GlContext*) (intptr_t) contextPeer;
    if (c) {
        cn1GlMakeCurrent(c);
        glViewport(x, y, width, height);
    }
}

/* Applies pipeline render state, binds the program + UBO + interleaved vertex
 * attributes + optional texture; shared by the indexed and array draw paths. */
static GLenum cn1GlPrimitive(int primitive) {
    switch (primitive) {
        case 0: return GL_POINTS;
        case 1: return GL_LINES;
        case 2: return GL_LINE_STRIP;
        case 4: return GL_TRIANGLE_STRIP;
        case 3:
        default: return GL_TRIANGLES;
    }
}

static void cn1GlBindDraw(CN1GlContext* c, CN1GlPipeline* p, GLuint vbo, int strideBytes,
        float* uniforms, int uniformFloats, GLuint texture, int texFilter, int texWrap,
        int useShadowMap) {
    glUseProgram(p->program);

    if (p->blend == 0) {
        glDisable(GL_BLEND);
    } else {
        glEnable(GL_BLEND);
        /* The colour is weighted by the source alpha; the ALPHA channel is
         * not. It accumulates coverage (a = sa + da * (1 - sa)), so a frame
         * cleared opaque stays opaque whatever is blended onto it. With one
         * glBlendFunc for all four channels the alpha was weighted by itself
         * (sa * sa + da * (1 - sa)): a 40% black quad over an opaque clear
         * left 0.76 in the FBO, cn1GlReadback handed that to cairo as the
         * frame's own transparency and the form behind showed through the
         * whole quad -- lighter than the background it was meant to darken.
         * This is RenderState.BlendMode's contract and what the Direct3D
         * backend does (cn1_windows_d3d.cpp). */
        if (p->blend == 2) {
            glBlendFuncSeparate(GL_SRC_ALPHA, GL_ONE,
                    GL_ONE, GL_ONE_MINUS_SRC_ALPHA);      /* additive */
        } else {
            glBlendFuncSeparate(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA,
                    GL_ONE, GL_ONE_MINUS_SRC_ALPHA);      /* alpha */
        }
    }
    if (p->cull == 0) {
        glDisable(GL_CULL_FACE);
    } else {
        glEnable(GL_CULL_FACE);
        glFrontFace(GL_CCW);
        glCullFace(p->cull == 2 ? GL_FRONT : GL_BACK);
    }
    if (p->depthTest) {
        glEnable(GL_DEPTH_TEST);
    } else {
        glDisable(GL_DEPTH_TEST);
    }
    glDepthMask(p->depthWrite ? GL_TRUE : GL_FALSE);

    /* Named uniforms from the packed array (locations were cached at link). */
    if (uniformFloats >= CN1_UNIFORM_FLOATS) {
        const float* u = uniforms;
        int i;
        for (i = 0; i < CN1_UNIFORM_COUNT; i++) {
            GLint l = p->loc[i];
            int n = CN1_UNIFORMS[i].size;
            if (l >= 0) {
                switch (n) {
                    case 16: glUniformMatrix4fv(l, 1, GL_FALSE, u); break;
                    case 4: glUniform4fv(l, 1, u); break;
                    case 3: glUniform3fv(l, 1, u); break;
                    case 2: glUniform2fv(l, 1, u); break;
                    default: glUniform1f(l, u[0]); break;
                }
            }
            u += n;
        }
    }

    /* Interleaved vertex attributes (position @0, normal @12, texcoord after). */
    glBindVertexArray(c->vao);
    glBindBuffer(GL_ARRAY_BUFFER, vbo);
    glEnableVertexAttribArray(0);
    glVertexAttribPointer(0, 3, GL_FLOAT, GL_FALSE, strideBytes, (void*) 0);
    {
        int offset = 12;
        if (p->hasNormal) {
            glEnableVertexAttribArray(1);
            glVertexAttribPointer(1, 3, GL_FLOAT, GL_FALSE, strideBytes, (void*) (intptr_t) offset);
            offset += 12;
        } else {
            glDisableVertexAttribArray(1);
        }
        if (p->hasTexcoord) {
            /* The texcoord is the last two floats of every VertexFormat, so its
             * offset comes from the stride, not from what the shader declares:
             * an unlit textured shader has no a_normal, yet its buffer may still
             * carry normals (POSITION_NORMAL_TEXCOORD), and counting from the
             * shader then sampled the normal as the texcoord. */
            offset = strideBytes - 8;
            glEnableVertexAttribArray(2);
            glVertexAttribPointer(2, 2, GL_FLOAT, GL_FALSE, strideBytes, (void*) (intptr_t) offset);
        } else {
            glDisableVertexAttribArray(2);
        }
    }

    if (texture) {
        glActiveTexture(GL_TEXTURE0);
        glBindTexture(GL_TEXTURE_2D, texture);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, texFilter ? GL_LINEAR : GL_NEAREST);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, texFilter ? GL_LINEAR : GL_NEAREST);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_S, texWrap ? GL_REPEAT : GL_CLAMP_TO_EDGE);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_T, texWrap ? GL_REPEAT : GL_CLAMP_TO_EDGE);
        if (p->uTexture >= 0) {
            glUniform1i(p->uTexture, 0);
        }
    }
    if (useShadowMap && c->shadow.tex && p->uShadowMap >= 0) {
        glActiveTexture(GL_TEXTURE1);
        glBindTexture(GL_TEXTURE_2D, c->shadow.tex);
        glUniform1i(p->uShadowMap, 1);
        glActiveTexture(GL_TEXTURE0);
    }
}

JAVA_VOID com_codename1_impl_linux_LinuxNative_gl3dDrawIndexed___long_long_long_int_long_int_int_float_1ARRAY_int_long_int_int_int(CODENAME_ONE_THREAD_STATE, JAVA_LONG contextPeer, JAVA_LONG pipelinePeer, JAVA_LONG vboPeer, JAVA_INT strideBytes, JAVA_LONG iboPeer, JAVA_INT indexCount, JAVA_INT primitive, JAVA_OBJECT uniforms, JAVA_INT uniformFloats, JAVA_LONG texturePeer, JAVA_INT texFilter, JAVA_INT texWrap, JAVA_INT shadowMap) {
    CN1GlContext* c = (CN1GlContext*) (intptr_t) contextPeer;
    CN1GlPipeline* p = (CN1GlPipeline*) (intptr_t) pipelinePeer;
    if (!c || !p || uniforms == JAVA_NULL) {
        return;
    }
    cn1GlMakeCurrent(c);
    cn1GlBindDraw(c, p, (GLuint) vboPeer, strideBytes, (float*) CN1_ARRAY_DATA(uniforms),
            uniformFloats, (GLuint) texturePeer, texFilter, texWrap, shadowMap);
    glBindBuffer(GL_ELEMENT_ARRAY_BUFFER, (GLuint) iboPeer);
    glDrawElements(cn1GlPrimitive(primitive), indexCount, GL_UNSIGNED_SHORT, 0);
}

JAVA_VOID com_codename1_impl_linux_LinuxNative_gl3dDrawArrays___long_long_long_int_int_int_float_1ARRAY_int_long_int_int_int(CODENAME_ONE_THREAD_STATE, JAVA_LONG contextPeer, JAVA_LONG pipelinePeer, JAVA_LONG vboPeer, JAVA_INT strideBytes, JAVA_INT vertexCount, JAVA_INT primitive, JAVA_OBJECT uniforms, JAVA_INT uniformFloats, JAVA_LONG texturePeer, JAVA_INT texFilter, JAVA_INT texWrap, JAVA_INT shadowMap) {
    CN1GlContext* c = (CN1GlContext*) (intptr_t) contextPeer;
    CN1GlPipeline* p = (CN1GlPipeline*) (intptr_t) pipelinePeer;
    if (!c || !p || uniforms == JAVA_NULL) {
        return;
    }
    cn1GlMakeCurrent(c);
    cn1GlBindDraw(c, p, (GLuint) vboPeer, strideBytes, (float*) CN1_ARRAY_DATA(uniforms),
            uniformFloats, (GLuint) texturePeer, texFilter, texWrap, shadowMap);
    glDrawArrays(cn1GlPrimitive(primitive), 0, vertexCount);
}

/* ------------------------------------------------- shadow pass and post */

static void cn1GlSaveTarget(CN1GlContext* c) {
    glGetIntegerv(GL_FRAMEBUFFER_BINDING, &c->savedFbo);
    glGetIntegerv(GL_VIEWPORT, c->savedVp);
}

static void cn1GlRestoreTarget(CN1GlContext* c) {
    glBindFramebuffer(GL_FRAMEBUFFER, (GLuint) c->savedFbo);
    glViewport(c->savedVp[0], c->savedVp[1], c->savedVp[2], c->savedVp[3]);
}

/* (Re)creates an RGBA8 colour target, with a 16 bit depth renderbuffer when
 * asked, unless one of that size exists. Leaves its FBO bound. */
static int cn1GlEnsureTarget(CN1GlTarget* t, int w, int h, int depth, GLint filter) {
    GLenum status;
    if (t->fbo && t->w == w && t->h == h) {
        glBindFramebuffer(GL_FRAMEBUFFER, t->fbo);
        return 1;
    }
    cn1GlReleaseTarget(t);
    t->w = w;
    t->h = h;
    glGenTextures(1, &t->tex);
    glBindTexture(GL_TEXTURE_2D, t->tex);
    glTexImage2D(GL_TEXTURE_2D, 0, GL_RGBA, w, h, 0, GL_RGBA, GL_UNSIGNED_BYTE, 0);
    glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, filter);
    glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, filter);
    glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_S, GL_CLAMP_TO_EDGE);
    glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_T, GL_CLAMP_TO_EDGE);
    glBindTexture(GL_TEXTURE_2D, 0);
    glGenFramebuffers(1, &t->fbo);
    glBindFramebuffer(GL_FRAMEBUFFER, t->fbo);
    glFramebufferTexture2D(GL_FRAMEBUFFER, GL_COLOR_ATTACHMENT0, GL_TEXTURE_2D, t->tex, 0);
    if (depth) {
        glGenRenderbuffers(1, &t->depth);
        glBindRenderbuffer(GL_RENDERBUFFER, t->depth);
        glRenderbufferStorage(GL_RENDERBUFFER, GL_DEPTH_COMPONENT16, w, h);
        glFramebufferRenderbuffer(GL_FRAMEBUFFER, GL_DEPTH_ATTACHMENT, GL_RENDERBUFFER, t->depth);
        glBindRenderbuffer(GL_RENDERBUFFER, 0);
    }
    status = glCheckFramebufferStatus(GL_FRAMEBUFFER);
    if (status != GL_FRAMEBUFFER_COMPLETE) {
        cn1GlReleaseTarget(t);
        return 0;
    }
    return 1;
}

JAVA_BOOLEAN com_codename1_impl_linux_LinuxNative_gl3dBeginShadowPass___long_int_R_boolean(CODENAME_ONE_THREAD_STATE, JAVA_LONG contextPeer, JAVA_INT size) {
    CN1GlContext* c = (CN1GlContext*) (intptr_t) contextPeer;
    if (!c || size <= 0) {
        return JAVA_FALSE;
    }
    cn1GlMakeCurrent(c);
    cn1GlSaveTarget(c);
    /* packed depth must not be interpolated between texels */
    if (!cn1GlEnsureTarget(&c->shadow, size, size, 1, GL_NEAREST)) {
        cn1GlRestoreTarget(c);
        return JAVA_FALSE;
    }
    glViewport(0, 0, size, size);
    glClearColor(1.0f, 1.0f, 1.0f, 1.0f);
    glClearDepthf(1.0f);
    glDepthMask(GL_TRUE);
    glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT);
    return JAVA_TRUE;
}

JAVA_VOID com_codename1_impl_linux_LinuxNative_gl3dEndShadowPass___long(CODENAME_ONE_THREAD_STATE, JAVA_LONG contextPeer) {
    CN1GlContext* c = (CN1GlContext*) (intptr_t) contextPeer;
    if (c) {
        cn1GlMakeCurrent(c);
        cn1GlRestoreTarget(c);
    }
}

JAVA_BOOLEAN com_codename1_impl_linux_LinuxNative_gl3dBeginPost___long_boolean_R_boolean(CODENAME_ONE_THREAD_STATE, JAVA_LONG contextPeer, JAVA_BOOLEAN bloom) {
    CN1GlContext* c = (CN1GlContext*) (intptr_t) contextPeer;
    int w, h;
    if (!c) {
        return JAVA_FALSE;
    }
    cn1GlMakeCurrent(c);
    cn1GlSaveTarget(c);
    w = c->savedVp[2];
    h = c->savedVp[3];
    if (w <= 0 || h <= 0) {
        return JAVA_FALSE;
    }
    if (!cn1GlEnsureTarget(&c->scene, w, h, 1, GL_LINEAR)) {
        cn1GlRestoreTarget(c);
        return JAVA_FALSE;
    }
    if (bloom) {
        int bw = w / 4 > 1 ? w / 4 : 1;
        int bh = h / 4 > 1 ? h / 4 : 1;
        if (!cn1GlEnsureTarget(&c->bloomA, bw, bh, 0, GL_LINEAR)
                || !cn1GlEnsureTarget(&c->bloomB, bw, bh, 0, GL_LINEAR)) {
            cn1GlRestoreTarget(c);
            return JAVA_FALSE;
        }
    }
    glBindFramebuffer(GL_FRAMEBUFFER, c->scene.fbo);
    glViewport(0, 0, w, h);
    return JAVA_TRUE;
}

static void cn1GlSampler(GLuint program, const char* name, int unit, GLuint tex) {
    GLint l = glGetUniformLocation(program, name);
    if (l >= 0) {
        glActiveTexture(GL_TEXTURE0 + unit);
        glBindTexture(GL_TEXTURE_2D, tex);
        glUniform1i(l, unit);
        glActiveTexture(GL_TEXTURE0);
    }
}

static void cn1GlUniform1(GLuint program, const char* name, float a) {
    GLint l = glGetUniformLocation(program, name);
    if (l >= 0) glUniform1f(l, a);
}

static void cn1GlUniform2(GLuint program, const char* name, float a, float b) {
    GLint l = glGetUniformLocation(program, name);
    if (l >= 0) glUniform2f(l, a, b);
}

static void cn1GlUniform3(GLuint program, const char* name, float a, float b, float d) {
    GLint l = glGetUniformLocation(program, name);
    if (l >= 0) glUniform3f(l, a, b, d);
}

static void cn1GlPassInto(const CN1GlTarget* t) {
    glBindFramebuffer(GL_FRAMEBUFFER, t->fbo);
    glViewport(0, 0, t->w, t->h);
}

/* Full screen triangle strip with a_position in clip space. */
static void cn1GlQuad(CN1GlContext* c) {
    static const float quad[12] = {-1.f, -1.f, 0.f, 1.f, -1.f, 0.f, -1.f, 1.f, 0.f, 1.f, 1.f, 0.f};
    glBindVertexArray(c->vao);
    if (!c->quadVbo) {
        glGenBuffers(1, &c->quadVbo);
        glBindBuffer(GL_ARRAY_BUFFER, c->quadVbo);
        glBufferData(GL_ARRAY_BUFFER, sizeof(quad), quad, GL_STATIC_DRAW);
    }
    glBindBuffer(GL_ARRAY_BUFFER, c->quadVbo);
    glEnableVertexAttribArray(0);
    glVertexAttribPointer(0, 3, GL_FLOAT, GL_FALSE, 12, (void*) 0);
    glDisableVertexAttribArray(1);
    glDisableVertexAttribArray(2);
    glDrawArrays(GL_TRIANGLE_STRIP, 0, 4);
    glBindBuffer(GL_ARRAY_BUFFER, 0);
}

/* params: threshold, bloomIntensity, contrast, saturation, lift rgb, gain rgb,
 * vignette, grain, time (13 floats). */
JAVA_VOID com_codename1_impl_linux_LinuxNative_gl3dEndPost___long_long_long_long_boolean_float_1ARRAY(CODENAME_ONE_THREAD_STATE, JAVA_LONG contextPeer, JAVA_LONG brightPeer, JAVA_LONG blurPeer, JAVA_LONG compositePeer, JAVA_BOOLEAN bloom, JAVA_OBJECT params) {
    CN1GlContext* c = (CN1GlContext*) (intptr_t) contextPeer;
    CN1GlPipeline* bright = (CN1GlPipeline*) (intptr_t) brightPeer;
    CN1GlPipeline* blur = (CN1GlPipeline*) (intptr_t) blurPeer;
    CN1GlPipeline* comp = (CN1GlPipeline*) (intptr_t) compositePeer;
    const float* pr;
    GLuint bloomTex;
    int i;
    if (!c || !comp || params == JAVA_NULL || !c->scene.fbo) {
        return;
    }
    pr = (const float*) CN1_ARRAY_DATA(params);
    cn1GlMakeCurrent(c);
    glDisable(GL_DEPTH_TEST);
    glDepthMask(GL_FALSE);
    glDisable(GL_BLEND);
    glDisable(GL_CULL_FACE);
    bloomTex = c->scene.tex;
    if (bloom && bright && blur && c->bloomA.fbo && c->bloomB.fbo) {
        cn1GlPassInto(&c->bloomA);
        glUseProgram(bright->program);
        cn1GlSampler(bright->program, "u_scene", 0, c->scene.tex);
        cn1GlUniform1(bright->program, "u_threshold", pr[0]);
        cn1GlUniform2(bright->program, "u_texel", 1.0f / c->scene.w, 1.0f / c->scene.h);
        cn1GlQuad(c);
        for (i = 0; i < 2; i++) {
            float spread = 1.0f + (float) i;
            cn1GlPassInto(&c->bloomB);
            glUseProgram(blur->program);
            cn1GlSampler(blur->program, "u_scene", 0, c->bloomA.tex);
            cn1GlUniform2(blur->program, "u_dir", spread / c->bloomA.w, 0.0f);
            cn1GlQuad(c);
            cn1GlPassInto(&c->bloomA);
            cn1GlSampler(blur->program, "u_scene", 0, c->bloomB.tex);
            cn1GlUniform2(blur->program, "u_dir", 0.0f, spread / c->bloomA.h);
            cn1GlQuad(c);
        }
        bloomTex = c->bloomA.tex;
    } else {
        bloom = JAVA_FALSE;
    }
    cn1GlRestoreTarget(c);
    glUseProgram(comp->program);
    cn1GlSampler(comp->program, "u_scene", 0, c->scene.tex);
    cn1GlSampler(comp->program, "u_bloom", 1, bloomTex);
    cn1GlUniform1(comp->program, "u_bloomIntensity", bloom ? pr[1] : 0.0f);
    cn1GlUniform2(comp->program, "u_grade", pr[2], pr[3]);
    cn1GlUniform3(comp->program, "u_lift", pr[4], pr[5], pr[6]);
    cn1GlUniform3(comp->program, "u_gain", pr[7], pr[8], pr[9]);
    cn1GlUniform1(comp->program, "u_vignette", pr[10]);
    cn1GlUniform1(comp->program, "u_grain", pr[11]);
    cn1GlUniform1(comp->program, "u_time", pr[12]);
    cn1GlUniform2(comp->program, "u_resolution", (float) c->scene.w, (float) c->scene.h);
    cn1GlQuad(c);
    glDepthMask(GL_TRUE);
}
