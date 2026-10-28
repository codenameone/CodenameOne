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
 * The embedded target's GPU renderer; cn1_linux_gpu.h says what it is and how it
 * fits beside the software renderer.
 *
 * Everything drawn into the window is a quad. A frame accumulates quads in one
 * vertex buffer and draws them in as few calls as the state allows: a batch ends
 * when the source image, its filter or the operator changes, or when the mask
 * atlas fills. The window is a texture behind a framebuffer object, so a paint
 * that repaints only part of the screen -- which is every paint Codename One
 * makes after the first -- leaves the rest as it was, exactly like the software
 * back buffer. Presenting draws that texture to the screen, rotated if the panel
 * is, and flips.
 *
 * Pixel formats: an image surface is premultiplied ARGB in a uint32_t, so its
 * bytes are B, G, R, A. It is uploaded as RGBA unchanged and swizzled in the
 * shader, which every GLES 2 device can do, rather than through
 * GL_EXT_texture_format_BGRA8888, which not all of them have. The window
 * texture holds true RGBA and is swizzled on the way back by cn1GpuSync.
 *
 * Threads: GL is used only on the thread that paints (the EDT), and the context
 * is made current there before each use, since the 3D API (cn1_linux_gl.c) has
 * its own context on the same thread. A surface can be freed on another thread,
 * so a texture to delete is queued and deleted between batches. The cursor moves
 * on the input thread through the cursor plane, which is an ioctl, not GL.
 */
#if defined(CN1_LINUX_EMBEDDED) && defined(CN1_EMBEDDED_GPU)

#include "cn1_linux_gpu.h"
#include <EGL/egl.h>
#include <EGL/eglext.h>
#include <GLES2/gl2.h>
#include <gbm.h>
#include <poll.h>
#include <pthread.h>
#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <sys/mman.h>
#include <xf86drm.h>
#include <xf86drmMode.h>

#define CN1G_VERTEX_FLOATS 11   /* x, y, u, v, coverage u, v, r, g, b, a, operation alpha */
#define CN1G_MAX_QUADS 4096     /* the index buffer is 16 bit: 4 vertices a quad */
#define CN1G_ATLAS_MAX 2048
#define CN1G_FULL 2             /* the atlas's top-left 2x2 is full coverage */

static EGLDisplay cn1gDpy = EGL_NO_DISPLAY;
static EGLContext cn1gCtx = EGL_NO_CONTEXT;
static EGLSurface cn1gSurface = EGL_NO_SURFACE;
static int cn1gActive = 0;
static int cn1gScanout = 0;
static int cn1gMaxTexture = 2048;

static struct gbm_device* cn1gGbm = 0;
static struct gbm_surface* cn1gGbmSurface = 0;
static int cn1gDrmFd = -1;
static unsigned cn1gCrtc = 0, cn1gConnector = 0;
static drmModeModeInfo cn1gMode;
static int cn1gCrtcSet = 0;
static struct gbm_bo* cn1gFrontBo = 0;     /* on screen */
static struct gbm_bo* cn1gNextBo = 0;      /* flip queued */
static int cn1gFlipPending = 0;
static int cn1gPhysW = 0, cn1gPhysH = 0, cn1gRotation = 0;

static GLuint cn1gProgram = 0, cn1gPresentProgram = 0;
static GLint cn1gScaleLoc, cn1gUseTexLoc, cn1gModeLoc, cn1gTexLoc, cn1gCovLoc;
static GLint cn1gPresentTexLoc;
static GLuint cn1gVbo = 0, cn1gIbo = 0, cn1gPresentVbo = 0;
static GLuint cn1gAtlas = 0;
static GLuint cn1gFbo = 0, cn1gFboTexture = 0;
static GLuint cn1gBlank = 0;               /* 1x1 texture bound when a batch has no image */

static cairo_surface_t* cn1gWindow = 0;
static int cn1gW = 0, cn1gH = 0;

static int cn1gAtlasSize = 0;
static unsigned char* cn1gAtlasData = 0;
static int cn1gShelfX, cn1gShelfY, cn1gShelfH;
static int cn1gDirtyY0, cn1gDirtyY1;       /* atlas rows to upload, y1 exclusive */

static float cn1gVerts[CN1G_MAX_QUADS * 4 * CN1G_VERTEX_FLOATS];
static int cn1gQuads = 0;
static int cn1gBatchOp = -1;
static GLuint cn1gBatchTexture = 0;
static int cn1gBatchNearest = 0;

static pthread_mutex_t cn1gDeadLock = PTHREAD_MUTEX_INITIALIZER;
static GLuint* cn1gDead = 0;
static int cn1gDeadCount = 0, cn1gDeadCapacity = 0;

/* ----------------------------------------------------------------- shaders */

static const char* const cn1gVertexSource =
    "attribute vec2 aPos;\n"
    "attribute vec2 aTex;\n"
    "attribute vec2 aCov;\n"
    "attribute vec4 aColor;\n"
    "attribute float aAlpha;\n"
    "uniform vec2 uScale;\n"
    "varying vec2 vTex;\n"
    "varying vec2 vCov;\n"
    "varying vec4 vColor;\n"
    "varying float vAlpha;\n"
    "void main() {\n"
    "    vTex = aTex;\n"
    "    vCov = aCov;\n"
    "    vColor = aColor;\n"
    "    vAlpha = aAlpha;\n"
    /* Device row 0 lands on framebuffer row 0, so the window reads back top down. */
    "    gl_Position = vec4(aPos.x * uScale.x - 1.0, aPos.y * uScale.y - 1.0, 0.0, 1.0);\n"
    "}\n";

/* uMode 0 composites; 1 writes the coverage alone, for the first pass of the
 * SOURCE and CLEAR operators, which scale what is there by (1 - coverage). */
static const char* const cn1gFragmentSource =
    "#ifdef GL_FRAGMENT_PRECISION_HIGH\n"
    "precision highp float;\n"
    "#else\n"
    "precision mediump float;\n"
    "#endif\n"
    "uniform sampler2D uTex;\n"
    "uniform sampler2D uCov;\n"
    "uniform float uUseTex;\n"
    "uniform float uMode;\n"
    "varying vec2 vTex;\n"
    "varying vec2 vCov;\n"
    "varying vec4 vColor;\n"
    "varying float vAlpha;\n"
    "void main() {\n"
    "    float c = texture2D(uCov, vCov).a;\n"
    "    if (uMode > 0.5) {\n"
    /* SOURCE and CLEAR keep dst * (1 - alpha * c): the operation's own alpha,
     * not the source colour's, is how much of the destination they replace. */
    "        gl_FragColor = vec4(c * vAlpha);\n"
    "        return;\n"
    "    }\n"
    "    vec4 s = vColor;\n"
    "    if (uUseTex > 0.5) {\n"
    /* Outside the image is transparent, as the software sampler has it. */
    "        if (vTex.x < 0.0 || vTex.y < 0.0 || vTex.x > 1.0 || vTex.y > 1.0) {\n"
    "            discard;\n"
    "        }\n"
    "        s = texture2D(uTex, vTex).bgra * vColor.a;\n"
    "    }\n"
    "    gl_FragColor = s * c;\n"
    "}\n";

static const char* const cn1gPresentVertexSource =
    "attribute vec2 aPos;\n"
    "attribute vec2 aTex;\n"
    "varying vec2 vTex;\n"
    "void main() {\n"
    "    vTex = aTex;\n"
    "    gl_Position = vec4(aPos, 0.0, 1.0);\n"
    "}\n";

static const char* const cn1gPresentFragmentSource =
    "precision mediump float;\n"
    "uniform sampler2D uTex;\n"
    "varying vec2 vTex;\n"
    "void main() {\n"
    "    gl_FragColor = vec4(texture2D(uTex, vTex).rgb, 1.0);\n"
    "}\n";

static GLuint cn1gShader(GLenum kind, const char* source) {
    GLint ok = 0;
    GLuint s = glCreateShader(kind);
    glShaderSource(s, 1, &source, 0);
    glCompileShader(s);
    glGetShaderiv(s, GL_COMPILE_STATUS, &ok);
    if (!ok) {
        char log[512];
        glGetShaderInfoLog(s, sizeof(log), 0, log);
        fprintf(stderr, "CN1: GPU shader failed: %s\n", log);
        glDeleteShader(s);
        return 0;
    }
    return s;
}

static GLuint cn1gLink(const char* vs, const char* fs, int withQuadAttributes) {
    GLint ok = 0;
    GLuint v = cn1gShader(GL_VERTEX_SHADER, vs);
    GLuint f = cn1gShader(GL_FRAGMENT_SHADER, fs);
    GLuint p;
    if (v == 0 || f == 0) {
        if (v) glDeleteShader(v);
        if (f) glDeleteShader(f);
        return 0;
    }
    p = glCreateProgram();
    glAttachShader(p, v);
    glAttachShader(p, f);
    glBindAttribLocation(p, 0, "aPos");
    glBindAttribLocation(p, 1, "aTex");
    if (withQuadAttributes) {
        glBindAttribLocation(p, 2, "aCov");
        glBindAttribLocation(p, 3, "aColor");
        glBindAttribLocation(p, 4, "aAlpha");
    }
    glLinkProgram(p);
    glDeleteShader(v);
    glDeleteShader(f);
    glGetProgramiv(p, GL_LINK_STATUS, &ok);
    if (!ok) {
        glDeleteProgram(p);
        return 0;
    }
    return p;
}

/* ------------------------------------------------------------- EGL bring-up */

static int cn1gMakeCurrent(void) {
    static int reported = 0;
    if (eglGetCurrentContext() == cn1gCtx) {
        return 1;
    }
    if (eglMakeCurrent(cn1gDpy, cn1gSurface, cn1gSurface, cn1gCtx) == EGL_TRUE) {
        return 1;
    }
    if (!reported) {
        reported = 1;
        fprintf(stderr, "CN1: the GPU context cannot be made current (EGL error 0x%x)\n", (unsigned) eglGetError());
    }
    return 0;
}

static EGLDisplay cn1gPlatformDisplay(EGLenum platform, void* native) {
    const char* ext = eglQueryString(EGL_NO_DISPLAY, EGL_EXTENSIONS);
    PFNEGLGETPLATFORMDISPLAYEXTPROC get;
    if (ext == 0 || strstr(ext, "EGL_EXT_platform_base") == 0) {
        return EGL_NO_DISPLAY;
    }
    get = (PFNEGLGETPLATFORMDISPLAYEXTPROC) eglGetProcAddress("eglGetPlatformDisplayEXT");
    return get != 0 ? get(platform, native, 0) : EGL_NO_DISPLAY;
}

/* Creates the context, the programs, the atlas and the buffers on an
 * initialised display. */
static int cn1gSetUp(EGLConfig config) {
    static const EGLint ctxAttribs[] = { EGL_CONTEXT_CLIENT_VERSION, 2, EGL_NONE };
    unsigned short* indices;
    GLint maxTexture = 0;
    int i;
    cn1gCtx = eglCreateContext(cn1gDpy, config, EGL_NO_CONTEXT, ctxAttribs);
    if (cn1gCtx == EGL_NO_CONTEXT || !cn1gMakeCurrent()) {
        return -1;
    }
    cn1gProgram = cn1gLink(cn1gVertexSource, cn1gFragmentSource, 1);
    cn1gPresentProgram = cn1gLink(cn1gPresentVertexSource, cn1gPresentFragmentSource, 0);
    if (cn1gProgram == 0 || cn1gPresentProgram == 0) {
        return -1;
    }
    cn1gScaleLoc = glGetUniformLocation(cn1gProgram, "uScale");
    cn1gUseTexLoc = glGetUniformLocation(cn1gProgram, "uUseTex");
    cn1gModeLoc = glGetUniformLocation(cn1gProgram, "uMode");
    cn1gTexLoc = glGetUniformLocation(cn1gProgram, "uTex");
    cn1gCovLoc = glGetUniformLocation(cn1gProgram, "uCov");
    cn1gPresentTexLoc = glGetUniformLocation(cn1gPresentProgram, "uTex");

    glGetIntegerv(GL_MAX_TEXTURE_SIZE, &maxTexture);
    cn1gMaxTexture = maxTexture;
    cn1gAtlasSize = maxTexture >= CN1G_ATLAS_MAX ? CN1G_ATLAS_MAX : maxTexture;
    if (cn1gAtlasSize < 256) {
        return -1;
    }
    cn1gAtlasData = (unsigned char*) calloc((size_t) cn1gAtlasSize * (size_t) cn1gAtlasSize, 1);
    if (cn1gAtlasData == 0) {
        return -1;
    }
    for (i = 0; i < CN1G_FULL; i++) {
        memset(cn1gAtlasData + (size_t) i * (size_t) cn1gAtlasSize, 255, CN1G_FULL);
    }
    glPixelStorei(GL_UNPACK_ALIGNMENT, 1);
    glPixelStorei(GL_PACK_ALIGNMENT, 4);
    glGenTextures(1, &cn1gAtlas);
    glBindTexture(GL_TEXTURE_2D, cn1gAtlas);
    glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_NEAREST);
    glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_NEAREST);
    glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_S, GL_CLAMP_TO_EDGE);
    glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_T, GL_CLAMP_TO_EDGE);
    glTexImage2D(GL_TEXTURE_2D, 0, GL_ALPHA, cn1gAtlasSize, cn1gAtlasSize, 0, GL_ALPHA, GL_UNSIGNED_BYTE, cn1gAtlasData);
    cn1gShelfX = CN1G_FULL + 2;
    cn1gShelfY = 0;
    cn1gShelfH = CN1G_FULL;
    cn1gDirtyY0 = cn1gDirtyY1 = 0;

    {
        static const unsigned char white[4] = { 255, 255, 255, 255 };
        glGenTextures(1, &cn1gBlank);
        glBindTexture(GL_TEXTURE_2D, cn1gBlank);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_NEAREST);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_NEAREST);
        glTexImage2D(GL_TEXTURE_2D, 0, GL_RGBA, 1, 1, 0, GL_RGBA, GL_UNSIGNED_BYTE, white);
    }

    indices = (unsigned short*) malloc(sizeof(unsigned short) * CN1G_MAX_QUADS * 6);
    if (indices == 0) {
        return -1;
    }
    for (i = 0; i < CN1G_MAX_QUADS; i++) {
        unsigned short b = (unsigned short) (i * 4);
        indices[i * 6 + 0] = b;
        indices[i * 6 + 1] = (unsigned short) (b + 1);
        indices[i * 6 + 2] = (unsigned short) (b + 2);
        indices[i * 6 + 3] = (unsigned short) (b + 2);
        indices[i * 6 + 4] = (unsigned short) (b + 1);
        indices[i * 6 + 5] = (unsigned short) (b + 3);
    }
    glGenBuffers(1, &cn1gIbo);
    glBindBuffer(GL_ELEMENT_ARRAY_BUFFER, cn1gIbo);
    glBufferData(GL_ELEMENT_ARRAY_BUFFER, (GLsizeiptr) (sizeof(unsigned short) * CN1G_MAX_QUADS * 6), indices, GL_STATIC_DRAW);
    free(indices);
    glGenBuffers(1, &cn1gVbo);
    glGenBuffers(1, &cn1gPresentVbo);
    glDisable(GL_DEPTH_TEST);
    glDisable(GL_CULL_FACE);
    return glGetError() == GL_NO_ERROR ? 0 : -1;
}

static void cn1gTearDown(void) {
    if (cn1gDpy != EGL_NO_DISPLAY) {
        eglMakeCurrent(cn1gDpy, EGL_NO_SURFACE, EGL_NO_SURFACE, EGL_NO_CONTEXT);
        if (cn1gSurface != EGL_NO_SURFACE) {
            eglDestroySurface(cn1gDpy, cn1gSurface);
        }
        if (cn1gCtx != EGL_NO_CONTEXT) {
            eglDestroyContext(cn1gDpy, cn1gCtx);
        }
        eglTerminate(cn1gDpy);
    }
    if (cn1gGbmSurface != 0) {
        if (cn1gNextBo != 0) {
            gbm_surface_release_buffer(cn1gGbmSurface, cn1gNextBo);
        }
        if (cn1gFrontBo != 0) {
            gbm_surface_release_buffer(cn1gGbmSurface, cn1gFrontBo);
        }
        cn1gNextBo = 0;
        cn1gFrontBo = 0;
        cn1gFlipPending = 0;
        gbm_surface_destroy(cn1gGbmSurface);
    }
    if (cn1gGbm != 0) {
        gbm_device_destroy(cn1gGbm);
    }
    free(cn1gAtlasData);
    cn1gAtlasData = 0;
    cn1gDpy = EGL_NO_DISPLAY;
    cn1gCtx = EGL_NO_CONTEXT;
    cn1gSurface = EGL_NO_SURFACE;
    cn1gGbmSurface = 0;
    cn1gGbm = 0;
    cn1gActive = 0;
    cn1gScanout = 0;
}

int cn1GpuInitKms(int drmFd, unsigned crtc, unsigned connector, const void* mode,
        int physW, int physH, int rotation) {
    static const EGLint attribs[] = {
        EGL_SURFACE_TYPE, EGL_WINDOW_BIT,
        EGL_RENDERABLE_TYPE, EGL_OPENGL_ES2_BIT,
        EGL_RED_SIZE, 8, EGL_GREEN_SIZE, 8, EGL_BLUE_SIZE, 8,
        EGL_NONE
    };
    EGLConfig configs[64];
    EGLConfig config = 0;
    EGLint count = 0, major, minor, i;
    cn1gGbm = gbm_create_device(drmFd);
    if (cn1gGbm == 0) {
        return -1;
    }
    cn1gDpy = cn1gPlatformDisplay(EGL_PLATFORM_GBM_KHR, cn1gGbm);
    if (cn1gDpy == EGL_NO_DISPLAY) {
        cn1gDpy = eglGetDisplay((EGLNativeDisplayType) cn1gGbm);
    }
    if (cn1gDpy == EGL_NO_DISPLAY || !eglInitialize(cn1gDpy, &major, &minor)
            || !eglBindAPI(EGL_OPENGL_ES_API)
            || !eglChooseConfig(cn1gDpy, attribs, configs, 64, &count) || count < 1) {
        cn1gTearDown();
        return -1;
    }
    /* The scanout format is XRGB8888; the config must render exactly that. */
    for (i = 0; i < count; i++) {
        EGLint id = 0;
        if (eglGetConfigAttrib(cn1gDpy, configs[i], EGL_NATIVE_VISUAL_ID, &id) && id == GBM_FORMAT_XRGB8888) {
            config = configs[i];
            break;
        }
    }
    if (config == 0) {
        cn1gTearDown();
        return -1;
    }
    cn1gGbmSurface = gbm_surface_create(cn1gGbm, (uint32_t) physW, (uint32_t) physH, GBM_FORMAT_XRGB8888,
            GBM_BO_USE_SCANOUT | GBM_BO_USE_RENDERING);
    if (cn1gGbmSurface == 0) {
        cn1gTearDown();
        return -1;
    }
    cn1gSurface = eglCreateWindowSurface(cn1gDpy, config, (EGLNativeWindowType) cn1gGbmSurface, 0);
    if (cn1gSurface == EGL_NO_SURFACE || cn1gSetUp(config) != 0) {
        cn1gTearDown();
        return -1;
    }
    /* A swap waits for nothing: the page flip is what is paced by the display. */
    eglSwapInterval(cn1gDpy, 0);
    cn1gDrmFd = drmFd;
    cn1gCrtc = crtc;
    cn1gConnector = connector;
    memcpy(&cn1gMode, mode, sizeof(cn1gMode));
    cn1gPhysW = physW;
    cn1gPhysH = physH;
    cn1gRotation = rotation;
    cn1gActive = 1;
    cn1gScanout = 1;
    /* The EDT makes it current again at its first paint. */
    eglMakeCurrent(cn1gDpy, EGL_NO_SURFACE, EGL_NO_SURFACE, EGL_NO_CONTEXT);
    return 0;
}

int cn1GpuInitOffscreen(void) {
    static const EGLint attribs[] = {
        EGL_SURFACE_TYPE, EGL_PBUFFER_BIT,
        EGL_RENDERABLE_TYPE, EGL_OPENGL_ES2_BIT,
        EGL_RED_SIZE, 8, EGL_GREEN_SIZE, 8, EGL_BLUE_SIZE, 8, EGL_ALPHA_SIZE, 8,
        EGL_NONE
    };
    static const EGLint pbufferAttribs[] = { EGL_WIDTH, 1, EGL_HEIGHT, 1, EGL_NONE };
    EGLConfig config;
    EGLint count = 0, major, minor;
    cn1gDpy = cn1gPlatformDisplay(EGL_PLATFORM_SURFACELESS_MESA, EGL_DEFAULT_DISPLAY);
    if (cn1gDpy == EGL_NO_DISPLAY) {
        cn1gDpy = eglGetDisplay(EGL_DEFAULT_DISPLAY);
    }
    if (cn1gDpy == EGL_NO_DISPLAY || !eglInitialize(cn1gDpy, &major, &minor)
            || !eglBindAPI(EGL_OPENGL_ES_API)
            || !eglChooseConfig(cn1gDpy, attribs, &config, 1, &count) || count < 1) {
        cn1gTearDown();
        return -1;
    }
    /* Surfaceless where the stack allows it; a 1x1 pbuffer where it does not.
     * Either way everything is drawn into the window's framebuffer object. */
    {
        const char* ext = eglQueryString(cn1gDpy, EGL_EXTENSIONS);
        if (ext == 0 || strstr(ext, "EGL_KHR_surfaceless_context") == 0) {
            cn1gSurface = eglCreatePbufferSurface(cn1gDpy, config, pbufferAttribs);
            if (cn1gSurface == EGL_NO_SURFACE) {
                cn1gTearDown();
                return -1;
            }
        }
    }
    if (cn1gSetUp(config) != 0) {
        cn1gTearDown();
        return -1;
    }
    cn1gActive = 1;
    cn1gScanout = 0;
    eglMakeCurrent(cn1gDpy, EGL_NO_SURFACE, EGL_NO_SURFACE, EGL_NO_CONTEXT);
    return 0;
}

int cn1GpuActive(void) {
    return cn1gActive;
}

int cn1GpuScansOut(void) {
    return cn1gScanout;
}

int cn1GpuAttachWindow(cairo_surface_t* window) {
    if (!cn1gActive || window == 0 || !cn1gMakeCurrent()) {
        return -1;
    }
    if (cn1gFbo == 0) {
        glGenFramebuffers(1, &cn1gFbo);
        glGenTextures(1, &cn1gFboTexture);
    }
    glBindTexture(GL_TEXTURE_2D, cn1gFboTexture);
    glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_NEAREST);
    glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_NEAREST);
    glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_S, GL_CLAMP_TO_EDGE);
    glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_T, GL_CLAMP_TO_EDGE);
    glTexImage2D(GL_TEXTURE_2D, 0, GL_RGBA, window->width, window->height, 0, GL_RGBA, GL_UNSIGNED_BYTE, 0);
    glBindFramebuffer(GL_FRAMEBUFFER, cn1gFbo);
    glFramebufferTexture2D(GL_FRAMEBUFFER, GL_COLOR_ATTACHMENT0, GL_TEXTURE_2D, cn1gFboTexture, 0);
    if (glCheckFramebufferStatus(GL_FRAMEBUFFER) != GL_FRAMEBUFFER_COMPLETE) {
        return -1;
    }
    glViewport(0, 0, window->width, window->height);
    glClearColor(0, 0, 0, 1);
    glClear(GL_COLOR_BUFFER_BIT);
    cn1gWindow = window;
    cn1gW = window->width;
    cn1gH = window->height;
    window->gpu = 1;
    window->gpuDirty = 1;
    glFinish();
    /* initDisplay may run on another thread than the one that paints; a context
     * current here could not be made current there. */
    eglMakeCurrent(cn1gDpy, EGL_NO_SURFACE, EGL_NO_SURFACE, EGL_NO_CONTEXT);
    return 0;
}

/* ---------------------------------------------------------------- batching */

static void cn1gDrainDead(void) {
    pthread_mutex_lock(&cn1gDeadLock);
    if (cn1gDeadCount > 0) {
        glDeleteTextures(cn1gDeadCount, cn1gDead);
        cn1gDeadCount = 0;
    }
    pthread_mutex_unlock(&cn1gDeadLock);
}

void cn1GpuForgetTexture(unsigned texture) {
    pthread_mutex_lock(&cn1gDeadLock);
    if (cn1gDeadCount == cn1gDeadCapacity) {
        int capacity = cn1gDeadCapacity == 0 ? 64 : cn1gDeadCapacity * 2;
        GLuint* grown = (GLuint*) realloc(cn1gDead, sizeof(GLuint) * (size_t) capacity);
        if (grown == 0) {
            /* Leaks one texture rather than deleting one still in use. */
            pthread_mutex_unlock(&cn1gDeadLock);
            return;
        }
        cn1gDead = grown;
        cn1gDeadCapacity = capacity;
    }
    cn1gDead[cn1gDeadCount++] = texture;
    pthread_mutex_unlock(&cn1gDeadLock);
}

static void cn1gUploadAtlas(void) {
    if (cn1gDirtyY1 > cn1gDirtyY0) {
        glBindTexture(GL_TEXTURE_2D, cn1gAtlas);
        glTexSubImage2D(GL_TEXTURE_2D, 0, 0, cn1gDirtyY0, cn1gAtlasSize, cn1gDirtyY1 - cn1gDirtyY0,
                GL_ALPHA, GL_UNSIGNED_BYTE, cn1gAtlasData + (size_t) cn1gDirtyY0 * (size_t) cn1gAtlasSize);
        cn1gDirtyY0 = cn1gDirtyY1 = 0;
    }
}

static void cn1gDrawBatch(int mode) {
    glUniform1f(cn1gModeLoc, (float) mode);
    glDrawElements(GL_TRIANGLES, cn1gQuads * 6, GL_UNSIGNED_SHORT, 0);
}

static void cn1gFlush(void) {
    GLsizei stride = (GLsizei) (sizeof(float) * CN1G_VERTEX_FLOATS);
    if (cn1gQuads == 0) {
        return;
    }
    cn1gUploadAtlas();
    glBindFramebuffer(GL_FRAMEBUFFER, cn1gFbo);
    glViewport(0, 0, cn1gW, cn1gH);
    glUseProgram(cn1gProgram);
    glUniform2f(cn1gScaleLoc, 2.0f / (float) cn1gW, 2.0f / (float) cn1gH);
    glUniform1f(cn1gUseTexLoc, cn1gBatchTexture != 0 ? 1.0f : 0.0f);
    glUniform1i(cn1gTexLoc, 0);
    glUniform1i(cn1gCovLoc, 1);
    glActiveTexture(GL_TEXTURE1);
    glBindTexture(GL_TEXTURE_2D, cn1gAtlas);
    glActiveTexture(GL_TEXTURE0);
    glBindTexture(GL_TEXTURE_2D, cn1gBatchTexture != 0 ? cn1gBatchTexture : cn1gBlank);
    if (cn1gBatchTexture != 0) {
        GLint f = cn1gBatchNearest ? GL_NEAREST : GL_LINEAR;
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, f);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, f);
    }
    glBindBuffer(GL_ARRAY_BUFFER, cn1gVbo);
    glBufferData(GL_ARRAY_BUFFER, (GLsizeiptr) (sizeof(float) * CN1G_VERTEX_FLOATS * 4 * (size_t) cn1gQuads),
            cn1gVerts, GL_STREAM_DRAW);
    glBindBuffer(GL_ELEMENT_ARRAY_BUFFER, cn1gIbo);
    glEnableVertexAttribArray(0);
    glEnableVertexAttribArray(1);
    glEnableVertexAttribArray(2);
    glEnableVertexAttribArray(3);
    glEnableVertexAttribArray(4);
    glVertexAttribPointer(0, 2, GL_FLOAT, GL_FALSE, stride, (const void*) 0);
    glVertexAttribPointer(1, 2, GL_FLOAT, GL_FALSE, stride, (const void*) (sizeof(float) * 2));
    glVertexAttribPointer(2, 2, GL_FLOAT, GL_FALSE, stride, (const void*) (sizeof(float) * 4));
    glVertexAttribPointer(3, 4, GL_FLOAT, GL_FALSE, stride, (const void*) (sizeof(float) * 6));
    glVertexAttribPointer(4, 1, GL_FLOAT, GL_FALSE, stride, (const void*) (sizeof(float) * 10));
    glEnable(GL_BLEND);
    switch (cn1gBatchOp) {
        case CAIRO_OPERATOR_CLEAR:
            glBlendFunc(GL_ZERO, GL_ONE_MINUS_SRC_ALPHA);
            cn1gDrawBatch(1);
            break;
        case CAIRO_OPERATOR_SOURCE:
            /* dst = src * c + dst * (1 - c), in two passes. */
            glBlendFunc(GL_ZERO, GL_ONE_MINUS_SRC_ALPHA);
            cn1gDrawBatch(1);
            glBlendFunc(GL_ONE, GL_ONE);
            cn1gDrawBatch(0);
            break;
        default:
            glBlendFunc(GL_ONE, GL_ONE_MINUS_SRC_ALPHA);
            cn1gDrawBatch(0);
            break;
    }
    cn1gQuads = 0;
    cn1gDrainDead();
}

/* Room for a w x h mask in the atlas: its top-left, or 0 when it is full. */
static int cn1gAllocate(int w, int h, int* ax, int* ay) {
    if (cn1gShelfX + w > cn1gAtlasSize) {
        cn1gShelfY += cn1gShelfH + 1;
        cn1gShelfX = 0;
        cn1gShelfH = 0;
    }
    if (cn1gShelfY + h > cn1gAtlasSize) {
        return 0;
    }
    *ax = cn1gShelfX;
    *ay = cn1gShelfY;
    cn1gShelfX += w + 1;
    if (h > cn1gShelfH) {
        cn1gShelfH = h;
    }
    return 1;
}

/* Starts the atlas over. Only after a flush: the batch drawn reads it. */
static void cn1gResetAtlas(void) {
    cn1gShelfX = CN1G_FULL + 2;
    cn1gShelfY = 0;
    cn1gShelfH = CN1G_FULL;
}

/* An image's texture, uploaded when its pixels changed since the last upload. */
/* An image wider or taller than the GPU's texture limit (2048 on VideoCore IV)
 * is uploaded downscaled to fit, by averaging boxes of texels. Texture
 * coordinates are normalised, so the quad samples the smaller texture at the
 * same places; such an image loses detail on that GPU, never disappears. */
static const void* cn1gFitTexture(const cairo_surface_t* s, int* w, int* h, uint32_t** scratch) {
    int tw = s->width, th = s->height, x, y;
    uint32_t* out;
    if (tw <= cn1gMaxTexture && th <= cn1gMaxTexture) {
        *w = tw;
        *h = th;
        return s->data;
    }
    if (tw > cn1gMaxTexture) tw = cn1gMaxTexture;
    if (th > cn1gMaxTexture) th = cn1gMaxTexture;
    out = (uint32_t*) malloc((size_t) tw * (size_t) th * 4);
    if (out == 0) {
        return 0;
    }
    for (y = 0; y < th; y++) {
        int sy0 = (int) ((long long) y * s->height / th);
        int sy1 = (int) ((long long) (y + 1) * s->height / th);
        for (x = 0; x < tw; x++) {
            int sx0 = (int) ((long long) x * s->width / tw);
            int sx1 = (int) ((long long) (x + 1) * s->width / tw);
            unsigned long sum[4] = { 0, 0, 0, 0 };
            unsigned long n = 0;
            int sx, sy, c;
            for (sy = sy0; sy < sy1; sy++) {
                const uint32_t* row = s->data + (size_t) sy * (size_t) s->width;
                for (sx = sx0; sx < sx1; sx++, n++) {
                    for (c = 0; c < 4; c++) {
                        sum[c] += (row[sx] >> (c * 8)) & 0xffu;
                    }
                }
            }
            out[(size_t) y * (size_t) tw + x] = n == 0 ? 0 : (uint32_t) (sum[0] / n)
                    | (uint32_t) (sum[1] / n) << 8 | (uint32_t) (sum[2] / n) << 16
                    | (uint32_t) (sum[3] / n) << 24;
        }
    }
    *w = tw;
    *h = th;
    *scratch = out;
    return out;
}

static GLuint cn1gTexture(cairo_surface_t* s) {
    if (s->gpu) {
        /* The window drawn into itself: sample a copy, never the target. */
        cn1gFlush();
        if (s->gpuDirty) {
            cn1GpuSync(s);
            s->version++;
        }
    }
    if (s->texture != 0 && s->textureVersion == s->version) {
        return s->texture;
    }
    if (s->texture != 0 && s->texture == cn1gBatchTexture) {
        /* The pending batch samples the old pixels. */
        cn1gFlush();
    }
    if (s->texture == 0) {
        glGenTextures(1, &s->texture);
        glBindTexture(GL_TEXTURE_2D, s->texture);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_S, GL_CLAMP_TO_EDGE);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_T, GL_CLAMP_TO_EDGE);
    } else {
        glBindTexture(GL_TEXTURE_2D, s->texture);
    }
    {
        uint32_t* scratch = 0;
        int w = 0, h = 0;
        const void* pixels = cn1gFitTexture(s, &w, &h, &scratch);
        GLenum err;
        while (glGetError() != GL_NO_ERROR) {
        }
        if (pixels != 0) {
            glTexImage2D(GL_TEXTURE_2D, 0, GL_RGBA, w, h, 0, GL_RGBA, GL_UNSIGNED_BYTE, pixels);
        }
        free(scratch);
        err = pixels == 0 ? GL_OUT_OF_MEMORY : glGetError();
        if (err != GL_NO_ERROR) {
            /* Not recorded as uploaded, so the next draw tries again. */
            static int reported = 0;
            if (!reported) {
                reported = 1;
                fprintf(stderr, "CN1: GPU texture upload of %dx%d failed (0x%x)\n", w, h, (unsigned) err);
            }
            return 0;
        }
    }
    s->textureVersion = s->version;
    return s->texture;
}

static void cn1gQuad(float x0, float y0, float x1, float y1, const CN1GpuSource* src,
        float cu0, float cv0, float cu1, float cv1, const float color[5]) {
    float* v = cn1gVerts + (size_t) cn1gQuads * 4 * CN1G_VERTEX_FLOATS;
    float xs[4] = { x0, x1, x0, x1 };
    float ys[4] = { y0, y0, y1, y1 };
    float cus[4] = { cu0, cu1, cu0, cu1 };
    float cvs[4] = { cv0, cv0, cv1, cv1 };
    int i;
    for (i = 0; i < 4; i++, v += CN1G_VERTEX_FLOATS) {
        v[0] = xs[i];
        v[1] = ys[i];
        if (src->solid) {
            v[2] = v[3] = 0;
        } else {
            /* The affine map is exact at the corners and interpolates exactly. */
            const cairo_matrix_t* t = &src->toSource;
            v[2] = (float) ((t->xx * xs[i] + t->xy * ys[i] + t->x0) / src->surface->width);
            v[3] = (float) ((t->yx * xs[i] + t->yy * ys[i] + t->y0) / src->surface->height);
        }
        v[4] = cus[i];
        v[5] = cvs[i];
        v[6] = color[0];
        v[7] = color[1];
        v[8] = color[2];
        v[9] = color[3];
        v[10] = color[4];
    }
    cn1gQuads++;
}

void cn1GpuDraw(cairo_surface_t* target, int x0, int y0, int x1, int y1,
        const CN1GpuSource* src, unsigned alpha, int op, const unsigned char* cov, int pitch) {
    GLuint texture = 0;
    int nearest = 0;
    float color[5];
    if (!cn1gActive || target != cn1gWindow || x1 <= x0 || y1 <= y0 || !cn1gMakeCurrent()) {
        return;
    }
    if (!src->solid) {
        if (src->surface == 0) {
            return;
        }
        texture = cn1gTexture(src->surface);
        if (texture == 0) {
            return;
        }
        nearest = src->filter == CAIRO_FILTER_NEAREST || src->filter == CAIRO_FILTER_FAST;
        /* An unscaled image at whole pixels samples texel centres either way. */
        if (src->toSource.xx == 1 && src->toSource.yy == 1 && src->toSource.xy == 0 && src->toSource.yx == 0) {
            nearest = 1;
        }
    }
    if (cn1gQuads > 0 && (op != cn1gBatchOp || texture != cn1gBatchTexture
            || (texture != 0 && nearest != cn1gBatchNearest))) {
        cn1gFlush();
    }
    cn1gBatchOp = op;
    cn1gBatchTexture = texture;
    cn1gBatchNearest = nearest;
    if (src->solid) {
        float a = (float) alpha / 255.0f / 255.0f;
        color[0] = (float) ((src->pixel >> 16) & 0xff) * a;
        color[1] = (float) ((src->pixel >> 8) & 0xff) * a;
        color[2] = (float) (src->pixel & 0xff) * a;
        color[3] = (float) ((src->pixel >> 24) & 0xff) * a;
    } else {
        color[0] = color[1] = color[2] = color[3] = (float) alpha / 255.0f;
    }
    color[4] = (float) alpha / 255.0f;
    target->gpuDirty = 1;
    if (cov == 0) {
        float c = (float) CN1G_FULL * 0.5f / (float) cn1gAtlasSize;
        if (cn1gQuads == CN1G_MAX_QUADS) {
            cn1gFlush();
        }
        cn1gQuad((float) x0, (float) y0, (float) x1, (float) y1, src, c, c, c, c, color);
    } else {
        /* Tiles, for a mask larger than the atlas. */
        int tile = cn1gAtlasSize - CN1G_FULL - 4, ty, tx;
        for (ty = y0; ty < y1; ty += tile) {
            int th = y1 - ty < tile ? y1 - ty : tile;
            for (tx = x0; tx < x1; tx += tile) {
                int tw = x1 - tx < tile ? x1 - tx : tile;
                int ax, ay, row;
                float inv = 1.0f / (float) cn1gAtlasSize;
                if (cn1gQuads == CN1G_MAX_QUADS) {
                    cn1gFlush();
                }
                if (!cn1gAllocate(tw, th, &ax, &ay)) {
                    cn1gFlush();
                    cn1gResetAtlas();
                    if (!cn1gAllocate(tw, th, &ax, &ay)) {
                        return;
                    }
                }
                for (row = 0; row < th; row++) {
                    memcpy(cn1gAtlasData + (size_t) (ay + row) * (size_t) cn1gAtlasSize + ax,
                           cov + (size_t) (ty - y0 + row) * (size_t) pitch + (tx - x0), (size_t) tw);
                }
                if (cn1gDirtyY1 == cn1gDirtyY0) {
                    cn1gDirtyY0 = ay;
                    cn1gDirtyY1 = ay + th;
                } else {
                    if (ay < cn1gDirtyY0) cn1gDirtyY0 = ay;
                    if (ay + th > cn1gDirtyY1) cn1gDirtyY1 = ay + th;
                }
                cn1gQuad((float) tx, (float) ty, (float) (tx + tw), (float) (ty + th), src,
                        ax * inv, ay * inv, (ax + tw) * inv, (ay + th) * inv, color);
            }
        }
    }
    if (op != CAIRO_OPERATOR_OVER) {
        cn1gFlush();
    }
}

/* ---------------------------------------------------------------- read back */

static void cn1gReadRect(cairo_surface_t* target, int x, int y, int w, int h);

static void cn1gSwizzle(uint32_t* p, size_t n) {
    size_t i;
    for (i = 0; i < n; i++) {
        uint32_t v = p[i];
        /* Bytes R, G, B, A read as a little-endian word are 0xAABBGGRR. */
        p[i] = (v & 0xff00ff00u) | ((v & 0xffu) << 16) | ((v >> 16) & 0xffu);
    }
}

int cn1GpuReadRect(cairo_surface_t* target, int x, int y, int w, int h) {
    /* A read from a thread that does not paint -- initDisplay's first present,
     * a capture -- leaves the context free for the one that does. */
    int borrowed = eglGetCurrentContext() != cn1gCtx;
    if (!cn1gActive || target != cn1gWindow || !cn1gMakeCurrent()) {
        return 0;
    }
    cn1gReadRect(target, x, y, w, h);
    if (borrowed) {
        eglMakeCurrent(cn1gDpy, EGL_NO_SURFACE, EGL_NO_SURFACE, EGL_NO_CONTEXT);
    }
    return 1;
}

static void cn1gReadRect(cairo_surface_t* target, int x, int y, int w, int h) {
    int row;
    if (x < 0) { w += x; x = 0; }
    if (y < 0) { h += y; y = 0; }
    if (x + w > cn1gW) w = cn1gW - x;
    if (y + h > cn1gH) h = cn1gH - y;
    if (w <= 0 || h <= 0) {
        return;
    }
    cn1gFlush();
    glBindFramebuffer(GL_FRAMEBUFFER, cn1gFbo);
    if (x == 0 && w == cn1gW) {
        uint32_t* dst = target->data + (size_t) y * (size_t) target->width;
        glReadPixels(0, y, w, h, GL_RGBA, GL_UNSIGNED_BYTE, dst);
        cn1gSwizzle(dst, (size_t) w * (size_t) h);
        return;
    }
    for (row = 0; row < h; row++) {
        uint32_t* dst = target->data + (size_t) (y + row) * (size_t) target->width + x;
        glReadPixels(x, y + row, w, 1, GL_RGBA, GL_UNSIGNED_BYTE, dst);
        cn1gSwizzle(dst, (size_t) w);
    }
}

void cn1GpuSync(cairo_surface_t* target) {
    if (target == 0 || target != cn1gWindow) {
        return;
    }
    /* A read that could not run -- the context is current on a thread that is
     * painting -- leaves the window dirty, so the next flush reads it back. */
    if (cn1GpuReadRect(target, 0, 0, cn1gW, cn1gH)) {
        target->gpuDirty = 0;
    }
}

/* ------------------------------------------------------------------ present */

static void cn1gDestroyFb(struct gbm_bo* bo, void* data) {
    unsigned id = (unsigned) (uintptr_t) data;
    (void) bo;
    if (id != 0 && cn1gDrmFd >= 0) {
        drmModeRmFB(cn1gDrmFd, id);
    }
}

static unsigned cn1gFbFor(struct gbm_bo* bo) {
    unsigned id = (unsigned) (uintptr_t) gbm_bo_get_user_data(bo);
    if (id == 0) {
        uint32_t handle = gbm_bo_get_handle(bo).u32;
        if (drmModeAddFB(cn1gDrmFd, gbm_bo_get_width(bo), gbm_bo_get_height(bo), 24, 32,
                gbm_bo_get_stride(bo), handle, &id) != 0) {
            return 0;
        }
        gbm_bo_set_user_data(bo, (void*) (uintptr_t) id, cn1gDestroyFb);
    }
    return id;
}

static void cn1gFlipDone(int fd, unsigned frame, unsigned sec, unsigned usec, void* data) {
    (void) fd; (void) frame; (void) sec; (void) usec; (void) data;
    cn1gFlipPending = 0;
}

/* Waits for the pending flip; answers 0 if it has not landed after 100 ms. Both
 * buffers then stay locked -- the kernel may still be scanning out either --
 * until the event arrives, and the caller drops the frame. A display that is
 * blanked or unplugged thus slows the EDT to ten presents a second instead of
 * hanging it, and never has a buffer it shows handed back for drawing. */
static int cn1gWaitFlip(void) {
    drmEventContext ev;
    struct pollfd p;
    memset(&ev, 0, sizeof(ev));
    ev.version = 2;
    ev.page_flip_handler = cn1gFlipDone;
    while (cn1gFlipPending) {
        p.fd = cn1gDrmFd;
        p.events = POLLIN;
        p.revents = 0;
        if (poll(&p, 1, 100) <= 0) {
            return 0;
        }
        drmHandleEvent(cn1gDrmFd, &ev);
    }
    if (cn1gNextBo != 0) {
        if (cn1gFrontBo != 0) {
            gbm_surface_release_buffer(cn1gGbmSurface, cn1gFrontBo);
        }
        cn1gFrontBo = cn1gNextBo;
        cn1gNextBo = 0;
    }
    return 1;
}

/* The screen corner (px, py) shows the window point the display unit's
 * cn1ToLogical gives for it, here in continuous coordinates. */
static void cn1gCorner(float px, float py, float* u, float* v) {
    float lx, ly;
    switch (cn1gRotation) {
        case 90:  lx = py; ly = (float) cn1gPhysW - px; break;
        case 180: lx = (float) cn1gPhysW - px; ly = (float) cn1gPhysH - py; break;
        case 270: lx = (float) cn1gPhysH - py; ly = px; break;
        default:  lx = px; ly = py; break;
    }
    *u = lx / (float) cn1gW;
    *v = ly / (float) cn1gH;
}

void cn1GpuPresent(void) {
    float quad[16];
    float pxs[4], pys[4];
    struct gbm_bo* bo;
    unsigned fb;
    int i;
    if (!cn1gActive || !cn1gScanout || cn1gWindow == 0 || !cn1gMakeCurrent()) {
        return;
    }
    cn1gFlush();
    glBindFramebuffer(GL_FRAMEBUFFER, 0);
    glViewport(0, 0, cn1gPhysW, cn1gPhysH);
    glDisable(GL_BLEND);
    glUseProgram(cn1gPresentProgram);
    glUniform1i(cn1gPresentTexLoc, 0);
    glActiveTexture(GL_TEXTURE0);
    glBindTexture(GL_TEXTURE_2D, cn1gFboTexture);
    pxs[0] = 0; pys[0] = 0;
    pxs[1] = (float) cn1gPhysW; pys[1] = 0;
    pxs[2] = 0; pys[2] = (float) cn1gPhysH;
    pxs[3] = (float) cn1gPhysW; pys[3] = (float) cn1gPhysH;
    for (i = 0; i < 4; i++) {
        quad[i * 4 + 0] = pxs[i] / (float) cn1gPhysW * 2.0f - 1.0f;
        quad[i * 4 + 1] = 1.0f - pys[i] / (float) cn1gPhysH * 2.0f;
        cn1gCorner(pxs[i], pys[i], &quad[i * 4 + 2], &quad[i * 4 + 3]);
    }
    glBindBuffer(GL_ARRAY_BUFFER, cn1gPresentVbo);
    glBufferData(GL_ARRAY_BUFFER, sizeof(quad), quad, GL_STREAM_DRAW);
    glDisableVertexAttribArray(2);
    glDisableVertexAttribArray(3);
    glDisableVertexAttribArray(4);
    glEnableVertexAttribArray(0);
    glEnableVertexAttribArray(1);
    glVertexAttribPointer(0, 2, GL_FLOAT, GL_FALSE, sizeof(float) * 4, (const void*) 0);
    glVertexAttribPointer(1, 2, GL_FLOAT, GL_FALSE, sizeof(float) * 4, (const void*) (sizeof(float) * 2));
    glDrawArrays(GL_TRIANGLE_STRIP, 0, 4);
    if (!eglSwapBuffers(cn1gDpy, cn1gSurface)) {
        return;
    }
    bo = gbm_surface_lock_front_buffer(cn1gGbmSurface);
    if (bo == 0) {
        return;
    }
    fb = cn1gFbFor(bo);
    /* The previous flip must land before its buffer's predecessor is released. */
    if (!cn1gWaitFlip() || fb == 0) {
        gbm_surface_release_buffer(cn1gGbmSurface, bo);
        return;
    }
    if (!cn1gCrtcSet || drmModePageFlip(cn1gDrmFd, cn1gCrtc, fb, DRM_MODE_PAGE_FLIP_EVENT, 0) != 0) {
        /* The first frame, or a driver that refused the flip: show it directly. */
        drmModeSetCrtc(cn1gDrmFd, cn1gCrtc, fb, 0, 0, &cn1gConnector, 1, &cn1gMode);
        cn1gCrtcSet = 1;
        if (cn1gFrontBo != 0) {
            gbm_surface_release_buffer(cn1gGbmSurface, cn1gFrontBo);
        }
        cn1gFrontBo = bo;
        return;
    }
    cn1gNextBo = bo;
    cn1gFlipPending = 1;
}

/* ------------------------------------------------------------------ cursor */

/* A 12x19 arrow, as the software cursor draws it: 1 = black outline, 2 = white. */
static const char* const cn1gCursorShape[19] = {
    "1           ", "11          ", "121         ", "1221        ", "12221       ",
    "122221      ", "1222221     ", "12222221    ", "122222221   ", "1222222221  ",
    "12222222221 ", "122222111111", "1221221     ", "121 1221    ", "11  1221    ",
    "1    1221   ", "     1221   ", "      1221  ", "       11   " };

static unsigned cn1gCursorHandle = 0;
static int cn1gCursorShown = 0;

/* The arrow in a 64x64 buffer with its tip at (32, 32), turned with the panel so
 * it points the same way the application's content does. */
static int cn1gCursorBuffer(void) {
    struct drm_mode_create_dumb create;
    struct drm_mode_map_dumb map;
    uint32_t* pixels;
    int x, y;
    memset(&create, 0, sizeof(create));
    create.width = 64;
    create.height = 64;
    create.bpp = 32;
    if (drmIoctl(cn1gDrmFd, DRM_IOCTL_MODE_CREATE_DUMB, &create) < 0) {
        return -1;
    }
    memset(&map, 0, sizeof(map));
    map.handle = create.handle;
    if (drmIoctl(cn1gDrmFd, DRM_IOCTL_MODE_MAP_DUMB, &map) < 0) {
        return -1;
    }
    pixels = (uint32_t*) mmap(0, create.size, PROT_READ | PROT_WRITE, MAP_SHARED, cn1gDrmFd, (off_t) map.offset);
    if (pixels == MAP_FAILED) {
        return -1;
    }
    memset(pixels, 0, create.size);
    for (y = 0; y < 19; y++) {
        for (x = 0; x < 12; x++) {
            char c = cn1gCursorShape[y][x];
            int dx, dy;
            if (c == ' ') continue;
            switch (cn1gRotation) {
                case 90:  dx = -y; dy = x; break;
                case 180: dx = -x; dy = -y; break;
                case 270: dx = y; dy = -x; break;
                default:  dx = x; dy = y; break;
            }
            pixels[(size_t) (32 + dy) * (create.pitch / 4) + (size_t) (32 + dx)] = c == '1' ? 0xff000000u : 0xffffffffu;
        }
    }
    munmap(pixels, create.size);
    cn1gCursorHandle = create.handle;
    return 0;
}

int cn1GpuCursor(int show, int px, int py) {
    if (!cn1gScanout) {
        return 0;
    }
    if (!show) {
        if (cn1gCursorShown) {
            drmModeSetCursor(cn1gDrmFd, cn1gCrtc, 0, 0, 0);
            cn1gCursorShown = 0;
        }
        return 1;
    }
    if (cn1gCursorHandle == 0 && cn1gCursorBuffer() != 0) {
        return 0;
    }
    if (!cn1gCursorShown) {
        if (drmModeSetCursor(cn1gDrmFd, cn1gCrtc, cn1gCursorHandle, 64, 64) != 0) {
            return 0;
        }
        cn1gCursorShown = 1;
    }
    return drmModeMoveCursor(cn1gDrmFd, cn1gCrtc, px - 32, py - 32) == 0;
}

void cn1GpuShutdown(void) {
    if (!cn1gActive) {
        return;
    }
    if (cn1gScanout) {
        (void) cn1gWaitFlip();
        cn1GpuCursor(0, 0, 0);
    }
    /* Also the software fallback's path when the window cannot be attached:
     * the GBM device and EGL context must go, and cn1GpuScansOut must answer
     * 0 so the cursor is drawn in software rather than on the cursor plane. */
    cn1gTearDown();
}

#endif /* CN1_LINUX_EMBEDDED && CN1_EMBEDDED_GPU */
