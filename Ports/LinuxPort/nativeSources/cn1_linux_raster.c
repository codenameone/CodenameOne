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
 * The embedded target's 2D renderer; cn1_linux_raster.h says what it covers and
 * why it exists.
 *
 * Coverage: every pixel row is sampled on CN1R_SAMPLES horizontal lines, and on
 * each line the inside intervals (by the fill rule) are added with their exact
 * fractional ends. That is exact area coverage horizontally and 1/16 steps
 * vertically, and because the winding is counted per sample line, overlapping
 * pieces of one path -- a stroke is built as overlapping quads -- union exactly
 * rather than adding up.
 *
 * Shared by the drawing thread and whichever thread decodes or scales an image,
 * so the scratch buffers are per thread and nothing else is global.
 */
#ifdef CN1_LINUX_EMBEDDED

#include "cn1_linux_raster.h"
#ifdef CN1_EMBEDDED_GPU
#include "cn1_linux_gpu.h"
#endif
#include <math.h>
#include <png.h>
#include <stdio.h>
#include <stdlib.h>
#include <string.h>

#ifndef M_PI
#define M_PI 3.14159265358979323846
#endif

#define CN1R_SAMPLES 16
#define CN1R_TOLERANCE 0.1
#define CN1R_MITER_LIMIT 10.0
/* Coordinates past this are clamped: nothing on a screen is that far out, and an
 * unclamped NaN or 1e300 would otherwise walk a loop for ever. */
#define CN1R_FAR 1.0e6


enum { CN1R_SOLID = 0, CN1R_SURFACE = 1 };

struct cn1r_pattern {
    int refs;
    int kind;
    uint32_t pixel;             /* solid: premultiplied ARGB */
    cairo_surface_t* surface;   /* surface: not owned (Cairo references it; the port
                                 * never frees a source while it is set) */
    cairo_matrix_t toSource;    /* device space -> surface pixels */
    double bx0, by0, bx1, by1;  /* the surface's extent in device space */
    int filter;
};

typedef struct {
    int refs;
    int x0, y0, x1, y1;         /* bounds, device pixels, x1/y1 exclusive */
    unsigned char* mask;        /* NULL: the whole rectangle; else (x1-x0)*(y1-y0) */
} CN1RClip;

typedef struct CN1RState {
    cairo_matrix_t matrix;
    cairo_pattern_t* source;
    int op;
    double lineWidth;
    int fillRule;
    CN1RClip* clip;             /* NULL: no clip */
    struct CN1RState* next;
} CN1RState;

typedef struct {
    int start;
    int count;
    int closed;
} CN1RSub;

struct cn1r_context {
    cairo_surface_t* target;
    CN1RState state;
    CN1RState* saved;
    double* pts;                /* device space x,y pairs */
    int ptCount;
    int ptCapacity;
    CN1RSub* subs;
    int subCount;
    int subCapacity;
    int hasCurrent;
    int reopen;                 /* close_path ran: the next segment starts a subpath */
    double curX, curY;          /* current point, device space */
    double startX, startY;      /* start of the current subpath, device space */
};

typedef struct {
    double x0, y0, x1, y1;      /* y0 < y1 */
    double dxdy;
    int dir;
} CN1REdge;

typedef struct {
    CN1REdge* edges;
    int count;
    int capacity;
    double minX, minY, maxX, maxY;
} CN1REdges;

/* ------------------------------------------------------------------ scratch */

static __thread float* cn1rCov = 0;
static __thread float* cn1rRun = 0;
static __thread int cn1rCovCapacity = 0;
static __thread double* cn1rCross = 0;
static __thread int* cn1rCrossDir = 0;
static __thread int cn1rCrossCapacity = 0;
static __thread CN1REdge** cn1rActive = 0;
static __thread int* cn1rDirty = 0;
static __thread int cn1rDirtyCount = 0;
static __thread int cn1rDirtyCapacity = 0;

static __thread unsigned char* cn1rFlag = 0;

/* Records that cell i of the row holds a partial coverage or a run delta. Every
 * sample line crosses the same few cells, so each is listed once. */
static inline void cn1rMarkDirty(int i) {
    if (cn1rFlag[i]) {
        return;
    }
    if (cn1rDirtyCount == cn1rDirtyCapacity) {
        int capacity = cn1rDirtyCapacity == 0 ? 256 : cn1rDirtyCapacity * 2;
        int* grown = (int*) realloc(cn1rDirty, sizeof(int) * (size_t) capacity);
        if (grown == 0) {
            return;
        }
        cn1rDirty = grown;
        cn1rDirtyCapacity = capacity;
    }
    cn1rFlag[i] = 1;
    cn1rDirty[cn1rDirtyCount++] = i;
}

static int cn1rIntOrder(const void* a, const void* b) {
    int x = *(const int*) a, y = *(const int*) b;
    return x < y ? -1 : x > y;
}
static __thread int cn1rActiveCapacity = 0;

static int cn1rEnsureCov(int width) {
    if (width + 2 > cn1rCovCapacity) {
        int capacity = width + 64;
        float* cov = (float*) realloc(cn1rCov, sizeof(float) * (size_t) capacity);
        float* run;
        unsigned char* flag;
        if (cov == 0) {
            return 0;
        }
        cn1rCov = cov;
        run = (float*) realloc(cn1rRun, sizeof(float) * (size_t) capacity);
        if (run == 0) {
            return 0;
        }
        cn1rRun = run;
        flag = (unsigned char*) realloc(cn1rFlag, (size_t) capacity);
        if (flag == 0) {
            return 0;
        }
        cn1rFlag = flag;
        cn1rCovCapacity = capacity;
    }
    return 1;
}

static int cn1rEnsureCross(int count) {
    if (count > cn1rCrossCapacity) {
        int capacity = count + 64;
        double* cross = (double*) realloc(cn1rCross, sizeof(double) * (size_t) capacity);
        int* dir;
        CN1REdge** active;
        if (cross == 0) {
            return 0;
        }
        cn1rCross = cross;
        dir = (int*) realloc(cn1rCrossDir, sizeof(int) * (size_t) capacity);
        if (dir == 0) {
            return 0;
        }
        cn1rCrossDir = dir;
        active = (CN1REdge**) realloc(cn1rActive, sizeof(CN1REdge*) * (size_t) capacity);
        if (active == 0) {
            return 0;
        }
        cn1rActive = active;
        cn1rCrossCapacity = capacity;
        cn1rActiveCapacity = capacity;
    }
    return 1;
}

/* ------------------------------------------------------------------- matrix */

void cn1r_matrix_init(cairo_matrix_t* m, double xx, double yx, double xy, double yy, double x0, double y0) {
    m->xx = xx;
    m->yx = yx;
    m->xy = xy;
    m->yy = yy;
    m->x0 = x0;
    m->y0 = y0;
}

void cn1r_matrix_init_identity(cairo_matrix_t* m) {
    cn1r_matrix_init(m, 1, 0, 0, 1, 0, 0);
}

void cn1r_matrix_transform_point(const cairo_matrix_t* m, double* x, double* y) {
    double nx = m->xx * *x + m->xy * *y + m->x0;
    double ny = m->yx * *x + m->yy * *y + m->y0;
    *x = nx;
    *y = ny;
}

static int cn1rInvert(const cairo_matrix_t* m, cairo_matrix_t* out) {
    double det = m->xx * m->yy - m->xy * m->yx;
    if (det == 0 || !isfinite(det)) {
        return 0;
    }
    out->xx = m->yy / det;
    out->xy = -m->xy / det;
    out->yx = -m->yx / det;
    out->yy = m->xx / det;
    out->x0 = -(out->xx * m->x0 + out->xy * m->y0);
    out->y0 = -(out->yx * m->x0 + out->yy * m->y0);
    return 1;
}

/* The largest factor the matrix stretches a length by (for flattening). */
static double cn1rMaxScale(const cairo_matrix_t* m) {
    double a = sqrt(m->xx * m->xx + m->yx * m->yx);
    double b = sqrt(m->xy * m->xy + m->yy * m->yy);
    return a > b ? a : b;
}

/* ------------------------------------------------------------------ surface */

cairo_surface_t* cn1r_image_surface_create(cairo_format_t format, int width, int height) {
    cairo_surface_t* s;
    (void) format;
    if (width < 1) {
        width = 1;
    }
    if (height < 1) {
        height = 1;
    }
    s = (cairo_surface_t*) calloc(1, sizeof(cairo_surface_t));
    if (s == 0) {
        return 0;
    }
    s->width = width;
    s->height = height;
    s->stride = width * 4;
    /* Zeroed, as Cairo's are: a new surface is transparent. */
    s->data = (uint32_t*) calloc((size_t) width * (size_t) height, 4);
    if (s->data == 0) {
        free(s);
        return 0;
    }
    return s;
}

unsigned char* cn1r_image_surface_get_data(cairo_surface_t* s) {
    if (s == 0) {
        return 0;
    }
    cn1r_surface_flush(s);
    /* The caller may write through the pointer: a texture made before is stale. */
    s->version++;
    return (unsigned char*) s->data;
}

int cn1r_image_surface_get_stride(cairo_surface_t* s) {
    return s ? s->stride : 0;
}

int cn1r_image_surface_get_width(cairo_surface_t* s) {
    return s ? s->width : 0;
}

int cn1r_image_surface_get_height(cairo_surface_t* s) {
    return s ? s->height : 0;
}

void cn1r_surface_destroy(cairo_surface_t* s) {
    if (s) {
#ifdef CN1_EMBEDDED_GPU
        if (s->texture != 0) {
            cn1GpuForgetTexture(s->texture);
        }
#endif
        free(s->data);
        free(s);
    }
}

/* Cairo's contract: after flush, data holds everything drawn. On the GPU window
 * that means reading it back. */
void cn1r_surface_flush(cairo_surface_t* s) {
#ifdef CN1_EMBEDDED_GPU
    if (s != 0 && s->gpu && s->gpuDirty) {
        cn1GpuSync(s);
    }
#else
    (void) s;
#endif
}

void cn1r_surface_mark_dirty(cairo_surface_t* s) {
    if (s != 0) {
        s->version++;
    }
}

cairo_status_t cn1r_surface_status(cairo_surface_t* s) {
    return s ? CAIRO_STATUS_SUCCESS : CAIRO_STATUS_NO_MEMORY;
}

/* ---------------------------------------------------------------- png write */

typedef struct {
    cairo_write_func_t write;
    void* closure;
    int failed;
} CN1RPngSink;

static void cn1rPngWriteData(png_structp png, png_bytep data, png_size_t length) {
    CN1RPngSink* sink = (CN1RPngSink*) png_get_io_ptr(png);
    if (!sink->failed && sink->write(sink->closure, data, (unsigned int) length) != CAIRO_STATUS_SUCCESS) {
        sink->failed = 1;
    }
}

static void cn1rPngFlush(png_structp png) {
    (void) png;
}

cairo_status_t cn1r_surface_write_to_png_stream(cairo_surface_t* s, cairo_write_func_t write, void* closure) {
    png_structp png;
    png_infop info;
    unsigned char* row;
    CN1RPngSink sink;
    int x, y;
    if (s == 0 || write == 0) {
        return CAIRO_STATUS_WRITE_ERROR;
    }
    cn1r_surface_flush(s);
    row = (unsigned char*) malloc((size_t) s->width * 4);
    if (row == 0) {
        return CAIRO_STATUS_NO_MEMORY;
    }
    png = png_create_write_struct(PNG_LIBPNG_VER_STRING, 0, 0, 0);
    info = png ? png_create_info_struct(png) : 0;
    if (info == 0) {
        png_destroy_write_struct(&png, 0);
        free(row);
        return CAIRO_STATUS_NO_MEMORY;
    }
    if (setjmp(png_jmpbuf(png))) {
        png_destroy_write_struct(&png, &info);
        free(row);
        return CAIRO_STATUS_WRITE_ERROR;
    }
    sink.write = write;
    sink.closure = closure;
    sink.failed = 0;
    png_set_write_fn(png, &sink, cn1rPngWriteData, cn1rPngFlush);
    png_set_IHDR(png, info, (png_uint_32) s->width, (png_uint_32) s->height, 8, PNG_COLOR_TYPE_RGB_ALPHA,
                 PNG_INTERLACE_NONE, PNG_COMPRESSION_TYPE_DEFAULT, PNG_FILTER_TYPE_DEFAULT);
    png_write_info(png, info);
    for (y = 0; y < s->height; y++) {
        const uint32_t* src = s->data + (size_t) y * (size_t) s->width;
        for (x = 0; x < s->width; x++) {
            uint32_t p = src[x];
            unsigned a = p >> 24, r = (p >> 16) & 0xff, g = (p >> 8) & 0xff, b = p & 0xff;
            if (a != 0 && a != 255) {
                r = (r * 255 + a / 2) / a;
                g = (g * 255 + a / 2) / a;
                b = (b * 255 + a / 2) / a;
                if (r > 255) r = 255;
                if (g > 255) g = 255;
                if (b > 255) b = 255;
            }
            row[x * 4] = (unsigned char) r;
            row[x * 4 + 1] = (unsigned char) g;
            row[x * 4 + 2] = (unsigned char) b;
            row[x * 4 + 3] = (unsigned char) a;
        }
        png_write_row(png, row);
    }
    png_write_end(png, info);
    png_destroy_write_struct(&png, &info);
    free(row);
    return sink.failed ? CAIRO_STATUS_WRITE_ERROR : CAIRO_STATUS_SUCCESS;
}

static cairo_status_t cn1rFileWrite(void* closure, const unsigned char* data, unsigned int length) {
    return fwrite(data, 1, length, (FILE*) closure) == length ? CAIRO_STATUS_SUCCESS : CAIRO_STATUS_WRITE_ERROR;
}

cairo_status_t cn1r_surface_write_to_png(cairo_surface_t* s, const char* filename) {
    FILE* f = filename ? fopen(filename, "wb") : 0;
    cairo_status_t st;
    if (f == 0) {
        return CAIRO_STATUS_WRITE_ERROR;
    }
    st = cn1r_surface_write_to_png_stream(s, cn1rFileWrite, f);
    if (fclose(f) != 0 && st == CAIRO_STATUS_SUCCESS) {
        st = CAIRO_STATUS_WRITE_ERROR;
    }
    return st;
}

/* --------------------------------------------------------- pattern and clip */

static cairo_pattern_t* cn1rSolid(double r, double g, double b, double a) {
    cairo_pattern_t* p = (cairo_pattern_t*) calloc(1, sizeof(cairo_pattern_t));
    unsigned ia, ir, ig, ib;
    if (p == 0) {
        return 0;
    }
    if (a < 0) a = 0;
    if (a > 1) a = 1;
    if (r < 0) r = 0;
    if (r > 1) r = 1;
    if (g < 0) g = 0;
    if (g > 1) g = 1;
    if (b < 0) b = 0;
    if (b > 1) b = 1;
    ia = (unsigned) (a * 255 + 0.5);
    ir = (unsigned) (r * a * 255 + 0.5);
    ig = (unsigned) (g * a * 255 + 0.5);
    ib = (unsigned) (b * a * 255 + 0.5);
    p->refs = 1;
    p->kind = CN1R_SOLID;
    p->pixel = (ia << 24) | (ir << 16) | (ig << 8) | ib;
    return p;
}

static void cn1rPatternRelease(cairo_pattern_t* p) {
    if (p != 0 && --p->refs == 0) {
        free(p);
    }
}

static void cn1rClipRelease(CN1RClip* c) {
    if (c != 0 && --c->refs == 0) {
        free(c->mask);
        free(c);
    }
}

void cn1r_pattern_set_filter(cairo_pattern_t* p, cairo_filter_t filter) {
    if (p) {
        p->filter = (int) filter;
    }
}

/* ------------------------------------------------------------------ context */

cairo_t* cn1r_create(cairo_surface_t* target) {
    cairo_t* cr = (cairo_t*) calloc(1, sizeof(cairo_t));
    if (cr == 0) {
        return 0;
    }
    cr->target = target;
    cn1r_matrix_init_identity(&cr->state.matrix);
    cr->state.source = cn1rSolid(0, 0, 0, 1);
    cr->state.op = CAIRO_OPERATOR_OVER;
    cr->state.lineWidth = 2.0;
    cr->state.fillRule = CAIRO_FILL_RULE_WINDING;
    return cr;
}

void cn1r_destroy(cairo_t* cr) {
    if (cr == 0) {
        return;
    }
    while (cr->saved != 0) {
        cn1r_restore(cr);
    }
    cn1rPatternRelease(cr->state.source);
    cn1rClipRelease(cr->state.clip);
    free(cr->pts);
    free(cr->subs);
    free(cr);
}

void cn1r_save(cairo_t* cr) {
    CN1RState* s = (CN1RState*) malloc(sizeof(CN1RState));
    if (s == 0) {
        return;
    }
    *s = cr->state;
    if (s->source) s->source->refs++;
    if (s->clip) s->clip->refs++;
    s->next = cr->saved;
    cr->saved = s;
}

void cn1r_restore(cairo_t* cr) {
    CN1RState* s = cr->saved;
    if (s == 0) {
        return;
    }
    cn1rPatternRelease(cr->state.source);
    cn1rClipRelease(cr->state.clip);
    cr->saved = s->next;
    cr->state = *s;
    cr->state.next = 0;
    free(s);
}

static void cn1rSetSource(cairo_t* cr, cairo_pattern_t* p) {
    if (p == 0) {
        return;
    }
    cn1rPatternRelease(cr->state.source);
    cr->state.source = p;
}

void cn1r_set_source_rgb(cairo_t* cr, double r, double g, double b) {
    cn1rSetSource(cr, cn1rSolid(r, g, b, 1));
}

void cn1r_set_source_rgba(cairo_t* cr, double r, double g, double b, double a) {
    cn1rSetSource(cr, cn1rSolid(r, g, b, a));
}

void cn1r_set_source_surface(cairo_t* cr, cairo_surface_t* surface, double x, double y) {
    cairo_pattern_t* p;
    cairo_matrix_t toUser;
    double cx[4], cy[4];
    int i;
    if (surface == 0) {
        return;
    }
    p = (cairo_pattern_t*) calloc(1, sizeof(cairo_pattern_t));
    if (p == 0) {
        return;
    }
    p->refs = 1;
    p->kind = CN1R_SURFACE;
    p->surface = surface;
    p->filter = CAIRO_FILTER_GOOD;
    if (!cn1rInvert(&cr->state.matrix, &toUser)) {
        /* A singular matrix draws nothing; an empty extent says so. */
        p->bx0 = p->by0 = p->bx1 = p->by1 = 0;
        cn1r_matrix_init_identity(&p->toSource);
        cn1rSetSource(cr, p);
        return;
    }
    /* Device -> user, then user -> surface: the surface's origin is at (x, y). */
    p->toSource = toUser;
    p->toSource.x0 -= x;
    p->toSource.y0 -= y;
    cx[0] = x; cy[0] = y;
    cx[1] = x + surface->width; cy[1] = y;
    cx[2] = x; cy[2] = y + surface->height;
    cx[3] = x + surface->width; cy[3] = y + surface->height;
    p->bx0 = p->by0 = 1e30;
    p->bx1 = p->by1 = -1e30;
    for (i = 0; i < 4; i++) {
        cn1r_matrix_transform_point(&cr->state.matrix, &cx[i], &cy[i]);
        if (cx[i] < p->bx0) p->bx0 = cx[i];
        if (cx[i] > p->bx1) p->bx1 = cx[i];
        if (cy[i] < p->by0) p->by0 = cy[i];
        if (cy[i] > p->by1) p->by1 = cy[i];
    }
    cn1rSetSource(cr, p);
}

cairo_pattern_t* cn1r_get_source(cairo_t* cr) {
    return cr->state.source;
}

void cn1r_set_operator(cairo_t* cr, cairo_operator_t op) {
    cr->state.op = (int) op;
}

void cn1r_set_line_width(cairo_t* cr, double width) {
    cr->state.lineWidth = width;
}

void cn1r_set_fill_rule(cairo_t* cr, cairo_fill_rule_t rule) {
    cr->state.fillRule = (int) rule;
}

void cn1r_set_matrix(cairo_t* cr, const cairo_matrix_t* m) {
    cr->state.matrix = *m;
}

void cn1r_get_matrix(cairo_t* cr, cairo_matrix_t* m) {
    *m = cr->state.matrix;
}

void cn1r_identity_matrix(cairo_t* cr) {
    cn1r_matrix_init_identity(&cr->state.matrix);
}

void cn1r_translate(cairo_t* cr, double tx, double ty) {
    cairo_matrix_t* m = &cr->state.matrix;
    m->x0 += m->xx * tx + m->xy * ty;
    m->y0 += m->yx * tx + m->yy * ty;
}

void cn1r_scale(cairo_t* cr, double sx, double sy) {
    cairo_matrix_t* m = &cr->state.matrix;
    m->xx *= sx;
    m->yx *= sx;
    m->xy *= sy;
    m->yy *= sy;
}

/* --------------------------------------------------------------------- path */

static double cn1rClamp(double v) {
    if (!(v > -CN1R_FAR)) {
        return -CN1R_FAR;
    }
    return v < CN1R_FAR ? v : CN1R_FAR;
}

static void cn1rAddPoint(cairo_t* cr, double x, double y) {
    if (cr->ptCount == cr->ptCapacity) {
        int capacity = cr->ptCapacity == 0 ? 64 : cr->ptCapacity * 2;
        double* pts = (double*) realloc(cr->pts, sizeof(double) * 2 * (size_t) capacity);
        if (pts == 0) {
            return;
        }
        cr->pts = pts;
        cr->ptCapacity = capacity;
    }
    cr->pts[cr->ptCount * 2] = cn1rClamp(x);
    cr->pts[cr->ptCount * 2 + 1] = cn1rClamp(y);
    cr->ptCount++;
    cr->subs[cr->subCount - 1].count++;
    cr->curX = x;
    cr->curY = y;
}

static void cn1rBeginSub(cairo_t* cr, double x, double y) {
    if (cr->subCount == cr->subCapacity) {
        int capacity = cr->subCapacity == 0 ? 8 : cr->subCapacity * 2;
        CN1RSub* subs = (CN1RSub*) realloc(cr->subs, sizeof(CN1RSub) * (size_t) capacity);
        if (subs == 0) {
            return;
        }
        cr->subs = subs;
        cr->subCapacity = capacity;
    }
    cr->subs[cr->subCount].start = cr->ptCount;
    cr->subs[cr->subCount].count = 0;
    cr->subs[cr->subCount].closed = 0;
    cr->subCount++;
    cr->hasCurrent = 1;
    cr->reopen = 0;
    cr->startX = x;
    cr->startY = y;
    cn1rAddPoint(cr, x, y);
}

/* A segment ending at device (x, y): starts a subpath at the current point when
 * the previous one was closed, as Cairo's implicit move_to does. */
static void cn1rLineDevice(cairo_t* cr, double x, double y) {
    if (!cr->hasCurrent) {
        cn1rBeginSub(cr, x, y);
        return;
    }
    if (cr->reopen) {
        cn1rBeginSub(cr, cr->curX, cr->curY);
    }
    cn1rAddPoint(cr, x, y);
}

void cn1r_new_path(cairo_t* cr) {
    cr->ptCount = 0;
    cr->subCount = 0;
    cr->hasCurrent = 0;
    cr->reopen = 0;
}

void cn1r_new_sub_path(cairo_t* cr) {
    cr->hasCurrent = 0;
}

void cn1r_move_to(cairo_t* cr, double x, double y) {
    cn1r_matrix_transform_point(&cr->state.matrix, &x, &y);
    cn1rBeginSub(cr, x, y);
}

void cn1r_line_to(cairo_t* cr, double x, double y) {
    cn1r_matrix_transform_point(&cr->state.matrix, &x, &y);
    cn1rLineDevice(cr, x, y);
}

void cn1r_curve_to(cairo_t* cr, double x1, double y1, double x2, double y2, double x3, double y3) {
    double p0x, p0y, ddx, ddy, dd, d2;
    int n, i;
    if (!cr->hasCurrent) {
        cn1r_move_to(cr, x1, y1);
    }
    cn1r_matrix_transform_point(&cr->state.matrix, &x1, &y1);
    cn1r_matrix_transform_point(&cr->state.matrix, &x2, &y2);
    cn1r_matrix_transform_point(&cr->state.matrix, &x3, &y3);
    p0x = cr->curX;
    p0y = cr->curY;
    /* An affine map keeps a Bezier a Bezier, so it is flattened in device space;
     * the second differences bound how far a chord strays from the curve. */
    ddx = fabs(p0x - 2 * x1 + x2);
    ddy = fabs(p0y - 2 * y1 + y2);
    dd = ddx > ddy ? ddx : ddy;
    ddx = fabs(x1 - 2 * x2 + x3);
    ddy = fabs(y1 - 2 * y2 + y3);
    d2 = ddx > ddy ? ddx : ddy;
    if (d2 > dd) {
        dd = d2;
    }
    n = (int) ceil(sqrt(0.75 * dd / CN1R_TOLERANCE));
    if (n < 1 || !isfinite(dd)) {
        n = 1;
    }
    if (n > 200) {
        n = 200;
    }
    for (i = 1; i <= n; i++) {
        double t = (double) i / n, u = 1 - t;
        double a = u * u * u, b = 3 * u * u * t, c = 3 * u * t * t, d = t * t * t;
        cn1rLineDevice(cr, a * p0x + b * x1 + c * x2 + d * x3, a * p0y + b * y1 + c * y2 + d * y3);
    }
}

static void cn1rArc(cairo_t* cr, double xc, double yc, double r, double a1, double a2) {
    double sweep = a2 - a1;
    double rDevice = fabs(r) * cn1rMaxScale(&cr->state.matrix);
    double step;
    int n, i;
    if (!isfinite(sweep) || !isfinite(rDevice)) {
        return;
    }
    if (rDevice > CN1R_TOLERANCE) {
        step = 2 * acos(1 - CN1R_TOLERANCE / rDevice);
    } else {
        step = M_PI / 2;
    }
    n = (int) ceil(fabs(sweep) / step);
    if (n < 1) {
        n = 1;
    }
    if (n > 4096) {
        n = 4096;
    }
    for (i = 0; i <= n; i++) {
        double a = a1 + sweep * i / n;
        double x = xc + r * cos(a), y = yc + r * sin(a);
        cn1r_matrix_transform_point(&cr->state.matrix, &x, &y);
        /* Cairo joins an arc to the current point with a line. */
        cn1rLineDevice(cr, x, y);
    }
}

void cn1r_arc(cairo_t* cr, double xc, double yc, double r, double a1, double a2) {
    if (!(a2 >= a1)) {
        /* Cairo's rule: the end angle is advanced by whole turns past the start. */
        double turns = ceil((a1 - a2) / (2 * M_PI));
        if (!isfinite(turns)) {
            return;
        }
        a2 += turns * 2 * M_PI;
    }
    cn1rArc(cr, xc, yc, r, a1, a2);
}

void cn1r_arc_negative(cairo_t* cr, double xc, double yc, double r, double a1, double a2) {
    if (!(a2 <= a1)) {
        double turns = ceil((a2 - a1) / (2 * M_PI));
        if (!isfinite(turns)) {
            return;
        }
        a2 -= turns * 2 * M_PI;
    }
    cn1rArc(cr, xc, yc, r, a1, a2);
}

void cn1r_close_path(cairo_t* cr) {
    if (!cr->hasCurrent || cr->subCount == 0) {
        return;
    }
    cr->subs[cr->subCount - 1].closed = 1;
    cr->curX = cr->startX;
    cr->curY = cr->startY;
    cr->reopen = 1;
}

void cn1r_rectangle(cairo_t* cr, double x, double y, double w, double h) {
    cn1r_move_to(cr, x, y);
    cn1r_line_to(cr, x + w, y);
    cn1r_line_to(cr, x + w, y + h);
    cn1r_line_to(cr, x, y + h);
    cn1r_close_path(cr);
}

/* -------------------------------------------------------------------- edges */

static void cn1rEdge(CN1REdges* e, double x0, double y0, double x1, double y1) {
    CN1REdge* edge;
    int dir = 1;
    if (y0 == y1) {
        return;
    }
    if (y0 > y1) {
        double t = x0;
        x0 = x1;
        x1 = t;
        t = y0;
        y0 = y1;
        y1 = t;
        dir = -1;
    }
    if (e->count == e->capacity) {
        int capacity = e->capacity == 0 ? 64 : e->capacity * 2;
        CN1REdge* edges = (CN1REdge*) realloc(e->edges, sizeof(CN1REdge) * (size_t) capacity);
        if (edges == 0) {
            return;
        }
        e->edges = edges;
        e->capacity = capacity;
    }
    edge = &e->edges[e->count++];
    edge->x0 = x0;
    edge->y0 = y0;
    edge->x1 = x1;
    edge->y1 = y1;
    edge->dxdy = (x1 - x0) / (y1 - y0);
    edge->dir = dir;
    if (x0 < e->minX) e->minX = x0;
    if (x1 < e->minX) e->minX = x1;
    if (x0 > e->maxX) e->maxX = x0;
    if (x1 > e->maxX) e->maxX = x1;
    if (y0 < e->minY) e->minY = y0;
    if (y1 > e->maxY) e->maxY = y1;
}

static void cn1rEdgesInit(CN1REdges* e) {
    memset(e, 0, sizeof(*e));
    e->minX = e->minY = 1e30;
    e->maxX = e->maxY = -1e30;
}

static void cn1rPolygon(CN1REdges* e, const double* p, int n) {
    int i;
    for (i = 0; i < n; i++) {
        int j = i + 1 == n ? 0 : i + 1;
        cn1rEdge(e, p[i * 2], p[i * 2 + 1], p[j * 2], p[j * 2 + 1]);
    }
}

/* Every subpath, closed whether or not close_path ran (a fill closes them). */
static void cn1rPathEdges(cairo_t* cr, CN1REdges* e) {
    int s;
    for (s = 0; s < cr->subCount; s++) {
        if (cr->subs[s].count >= 2) {
            cn1rPolygon(e, cr->pts + cr->subs[s].start * 2, cr->subs[s].count);
        }
    }
}

static int cn1rEdgeOrder(const void* a, const void* b) {
    double d = ((const CN1REdge*) a)->y0 - ((const CN1REdge*) b)->y0;
    return d < 0 ? -1 : d > 0 ? 1 : 0;
}

/* ------------------------------------------------------------------ stroker */

static double cn1rSignedArea(const double* p, int n) {
    double a = 0;
    int i;
    for (i = 0; i < n; i++) {
        int j = i + 1 == n ? 0 : i + 1;
        a += p[i * 2] * p[j * 2 + 1] - p[j * 2] * p[i * 2 + 1];
    }
    return a;
}

/* Adds a piece of a stroke wound the same way as every other piece, so the
 * nonzero rule unions them instead of letting opposite windings cancel. */
static void cn1rStrokePiece(CN1REdges* e, double* p, int n) {
    if (cn1rSignedArea(p, n) < 0) {
        int i;
        for (i = 0; i < n / 2; i++) {
            double tx = p[i * 2], ty = p[i * 2 + 1];
            p[i * 2] = p[(n - 1 - i) * 2];
            p[i * 2 + 1] = p[(n - 1 - i) * 2 + 1];
            p[(n - 1 - i) * 2] = tx;
            p[(n - 1 - i) * 2 + 1] = ty;
        }
    }
    cn1rPolygon(e, p, n);
}

static void cn1rJoin(CN1REdges* e, double px, double py, double ux1, double uy1, double ux2, double uy2, double hw) {
    double cross = ux1 * uy2 - uy1 * ux2;
    double dot = ux1 * ux2 + uy1 * uy2;
    double o, ax, ay, bx, by, q[8];
    if (fabs(cross) < 1e-9 && dot > 0) {
        return;
    }
    /* The gap opens on the side away from the turn. */
    o = cross > 0 ? -1 : 1;
    ax = px + o * -uy1 * hw;
    ay = py + o * ux1 * hw;
    bx = px + o * -uy2 * hw;
    by = py + o * ux2 * hw;
    q[0] = px;
    q[1] = py;
    q[2] = ax;
    q[3] = ay;
    if ((1 + dot) / 2 >= 1 / (CN1R_MITER_LIMIT * CN1R_MITER_LIMIT)) {
        /* The miter tip: where the two outer edges meet. */
        double mx = -uy1 - uy2, my = ux1 + ux2;
        double k = o * hw / (1 + dot);
        q[4] = px + mx * k;
        q[5] = py + my * k;
        q[6] = bx;
        q[7] = by;
        cn1rStrokePiece(e, q, 4);
    } else {
        q[4] = bx;
        q[5] = by;
        cn1rStrokePiece(e, q, 3);
    }
}

static void cn1rStrokeEdges(cairo_t* cr, CN1REdges* e) {
    const cairo_matrix_t* m = &cr->state.matrix;
    double hw = cr->state.lineWidth * sqrt(fabs(m->xx * m->yy - m->xy * m->yx)) / 2;
    int s;
    if (!(hw > 0)) {
        return;
    }
    for (s = 0; s < cr->subCount; s++) {
        const double* src = cr->pts + cr->subs[s].start * 2;
        int count = cr->subs[s].count;
        int closed = cr->subs[s].closed;
        double* p;
        int n = 0, i, segs;
        if (count < 2) {
            continue;
        }
        p = (double*) malloc(sizeof(double) * 2 * (size_t) count);
        if (p == 0) {
            continue;
        }
        /* Drop repeated points; they have no direction. */
        for (i = 0; i < count; i++) {
            if (n == 0 || fabs(src[i * 2] - p[(n - 1) * 2]) > 1e-9 || fabs(src[i * 2 + 1] - p[(n - 1) * 2 + 1]) > 1e-9) {
                p[n * 2] = src[i * 2];
                p[n * 2 + 1] = src[i * 2 + 1];
                n++;
            }
        }
        if (closed && n > 2 && fabs(p[0] - p[(n - 1) * 2]) < 1e-9 && fabs(p[1] - p[(n - 1) * 2 + 1]) < 1e-9) {
            n--;
        }
        if (n < 2) {
            free(p);
            continue;
        }
        segs = closed ? n : n - 1;
        for (i = 0; i < segs; i++) {
            int j = i + 1 == n ? 0 : i + 1;
            double dx = p[j * 2] - p[i * 2], dy = p[j * 2 + 1] - p[i * 2 + 1];
            double len = sqrt(dx * dx + dy * dy);
            double nx, ny, q[8];
            if (len <= 0) {
                continue;
            }
            nx = -dy / len * hw;
            ny = dx / len * hw;
            q[0] = p[i * 2] + nx;
            q[1] = p[i * 2 + 1] + ny;
            q[2] = p[j * 2] + nx;
            q[3] = p[j * 2 + 1] + ny;
            q[4] = p[j * 2] - nx;
            q[5] = p[j * 2 + 1] - ny;
            q[6] = p[i * 2] - nx;
            q[7] = p[i * 2 + 1] - ny;
            cn1rStrokePiece(e, q, 4);
            /* The join at the segment's end, when another segment follows. */
            if (i + 1 < segs || closed) {
                int k = j + 1 == n ? 0 : j + 1;
                double ex = p[k * 2] - p[j * 2], ey = p[k * 2 + 1] - p[j * 2 + 1];
                double elen = sqrt(ex * ex + ey * ey);
                if (elen > 0) {
                    cn1rJoin(e, p[j * 2], p[j * 2 + 1], dx / len, dy / len, ex / elen, ey / elen, hw);
                }
            }
        }
        free(p);
    }
}

/* -------------------------------------------------------------- compositing */

static inline uint32_t cn1rScale(uint32_t p, unsigned c) {
    /* Each channel times c/255, rounded: two channels per multiply. */
    uint32_t rb = (p & 0x00ff00ffu) * c + 0x00800080u;
    uint32_t ag = ((p >> 8) & 0x00ff00ffu) * c + 0x00800080u;
    rb = ((rb + ((rb >> 8) & 0x00ff00ffu)) >> 8) & 0x00ff00ffu;
    ag = (ag + ((ag >> 8) & 0x00ff00ffu)) & 0xff00ff00u;
    return rb | ag;
}

static inline uint32_t cn1rOver(uint32_t s, uint32_t d) {
    unsigned sa = s >> 24;
    if (sa == 255) {
        return s;
    }
    if (sa == 0 && s == 0) {
        return d;
    }
    return s + cn1rScale(d, 255 - sa);
}

static inline uint32_t cn1rBlend(int op, uint32_t s, uint32_t d, unsigned cov) {
    if (op == CAIRO_OPERATOR_OVER) {
        return cn1rOver(cov == 255 ? s : cn1rScale(s, cov), d);
    }
    if (op == CAIRO_OPERATOR_SOURCE) {
        return cov == 255 ? s : cn1rScale(s, cov) + cn1rScale(d, 255 - cov);
    }
    /* CLEAR */
    return cov == 255 ? 0 : cn1rScale(d, 255 - cov);
}

static inline uint32_t cn1rTexel(const cairo_surface_t* s, int x, int y) {
    if (x < 0 || y < 0 || x >= s->width || y >= s->height) {
        return 0;
    }
    return s->data[(size_t) y * (size_t) s->width + (size_t) x];
}

static uint32_t cn1rBilinear(const cairo_surface_t* s, double sx, double sy) {
    double fx = sx - 0.5, fy = sy - 0.5;
    int x0 = (int) floor(fx), y0 = (int) floor(fy);
    unsigned wx = (unsigned) ((fx - x0) * 256), wy = (unsigned) ((fy - y0) * 256);
    uint32_t a = cn1rTexel(s, x0, y0), b = cn1rTexel(s, x0 + 1, y0);
    uint32_t c = cn1rTexel(s, x0, y0 + 1), d = cn1rTexel(s, x0 + 1, y0 + 1);
    uint32_t out = 0;
    int shift;
    if (wx > 255) wx = 255;
    if (wy > 255) wy = 255;
    for (shift = 0; shift < 32; shift += 8) {
        unsigned top = ((a >> shift) & 0xff) * (256 - wx) + ((b >> shift) & 0xff) * wx;
        unsigned bottom = ((c >> shift) & 0xff) * (256 - wx) + ((d >> shift) & 0xff) * wx;
        unsigned v = (top * (256 - wy) + bottom * wy + 32768) >> 16;
        out |= (v > 255 ? 255 : v) << shift;
    }
    return out;
}

/* Composites one device row [x0, x1) of the source, weighted by cov (NULL: full
 * coverage), the clip's mask and alpha (0..255). */
static void cn1rRow(cairo_t* cr, int y, int x0, int x1, const float* cov, unsigned alpha) {
    cairo_surface_t* dst = cr->target;
    cairo_pattern_t* src = cr->state.source;
    CN1RClip* clip = cr->state.clip;
    const unsigned char* mask = 0;
    uint32_t* row = dst->data + (size_t) y * (size_t) dst->width;
    int op = cr->state.op;
    int x;
    /* OVER may scale the source by the operation's alpha. SOURCE and CLEAR
     * cannot: as in Cairo, the alpha of paint_with_alpha or of a mask is
     * coverage to them, so they keep dst * (1 - alpha * coverage). */
    unsigned weight = 255;
    if (op != CAIRO_OPERATOR_OVER && alpha != 255) {
        weight = alpha;
        alpha = 255;
    }
    if (clip != 0 && clip->mask != 0) {
        mask = clip->mask + (size_t) (y - clip->y0) * (size_t) (clip->x1 - clip->x0) - clip->x0;
    }
    if (src->kind == CN1R_SOLID) {
        uint32_t pixel = alpha == 255 ? src->pixel : cn1rScale(src->pixel, alpha);
        if (cov == 0 && mask == 0 && weight == 255 && (op == CAIRO_OPERATOR_SOURCE || (pixel >> 24) == 255)) {
            for (x = x0; x < x1; x++) {
                row[x] = pixel;
            }
            return;
        }
        int store = mask == 0 && weight == 255 && (op == CAIRO_OPERATOR_SOURCE || (pixel >> 24) == 255);
        for (x = x0; x < x1; x++) {
            unsigned c = 255;
            if (cov != 0) {
                float f = cov[x - x0];
                if (f <= 0) {
                    continue;
                }
                if (f >= 1 && store) {
                    /* The inside of a shape: a run of whole pixels, stored. Most of
                     * what a fill covers is here, and only its edges blend. */
                    int end = x + 1;
                    while (end < x1 && cov[end - x0] >= 1) {
                        end++;
                    }
                    for (; x < end; x++) {
                        row[x] = pixel;
                    }
                    x--;
                    continue;
                }
                c = f >= 1 ? 255 : (unsigned) (f * 255 + 0.5f);
            }
            if (mask != 0) {
                c = (c * mask[x] + 127) / 255;
            }
            if (weight != 255) {
                c = (c * weight + 127) / 255;
            }
            if (c != 0) {
                row[x] = cn1rBlend(op, pixel, row[x], c);
            }
        }
        return;
    }
    {
        const cairo_matrix_t* t = &src->toSource;
        const cairo_surface_t* s = src->surface;
        double px = x0 + 0.5, py = y + 0.5;
        double sx = t->xx * px + t->xy * py + t->x0;
        double sy = t->yx * px + t->yy * py + t->y0;
        /* Whole pixels moved by whole pixels: the sample IS a texel, whatever the
         * filter, so copy it. Every unscaled image the UI draws comes here. */
        int exact = t->xx == 1 && t->yy == 1 && t->xy == 0 && t->yx == 0
                && t->x0 == floor(t->x0) && t->y0 == floor(t->y0);
        int nearest = src->filter == CAIRO_FILTER_NEAREST || src->filter == CAIRO_FILTER_FAST;
        for (x = x0; x < x1; x++, sx += t->xx, sy += t->yx) {
            unsigned c = 255;
            uint32_t texel;
            if (cov != 0) {
                float f = cov[x - x0];
                if (f <= 0) {
                    continue;
                }
                c = f >= 1 ? 255 : (unsigned) (f * 255 + 0.5f);
            }
            if (mask != 0) {
                c = (c * mask[x] + 127) / 255;
            }
            if (weight != 255) {
                c = (c * weight + 127) / 255;
            }
            if (c == 0) {
                continue;
            }
            if (exact || nearest) {
                texel = cn1rTexel(s, (int) floor(sx), (int) floor(sy));
            } else {
                texel = cn1rBilinear(s, sx, sy);
            }
            if (alpha != 255) {
                texel = cn1rScale(texel, alpha);
            }
            if (texel != 0 || op != CAIRO_OPERATOR_OVER) {
                row[x] = cn1rBlend(op, texel, row[x], c);
            }
        }
    }
}

/* The device rectangle drawing may touch: the surface, narrowed by the clip. */
static void cn1rBounds(cairo_t* cr, int* x0, int* y0, int* x1, int* y1) {
    *x0 = 0;
    *y0 = 0;
    *x1 = cr->target->width;
    *y1 = cr->target->height;
    if (cr->state.clip != 0) {
        CN1RClip* c = cr->state.clip;
        if (c->x0 > *x0) *x0 = c->x0;
        if (c->y0 > *y0) *y0 = c->y0;
        if (c->x1 < *x1) *x1 = c->x1;
        if (c->y1 < *y1) *y1 = c->y1;
    }
}

static void cn1rNarrow(int* x0, int* y0, int* x1, int* y1, double fx0, double fy0, double fx1, double fy1) {
    int a = (int) floor(fx0), b = (int) floor(fy0), c = (int) ceil(fx1), d = (int) ceil(fy1);
    if (a > *x0) *x0 = a;
    if (b > *y0) *y0 = b;
    if (c < *x1) *x1 = c;
    if (d < *y1) *y1 = d;
}

/*
 * Scan converts the edges over the device rectangle [x0,x1) x [y0,y1) and hands
 * every covered span of every row to emit: either the coverage of each pixel of
 * [rx0, rx1), or, with cov NULL, one coverage for all of them.
 *
 * Only the cells an edge crosses get a coverage of their own. Between them the
 * coverage is constant, so the inside of a shape is one span whatever its width,
 * and nothing walks the row pixel by pixel.
 */
typedef void (*CN1RRowSink)(void* ctx, int y, int rx0, int rx1, const float* cov, float constant);

static void cn1rScan(CN1REdges* e, int rule, int x0, int y0, int x1, int y1, CN1RRowSink emit, void* ctx) {
    int width = x1 - x0;
    int next = 0, activeCount = 0;
    int y;
    if (e->count == 0 || width <= 0 || y1 <= y0) {
        return;
    }
    if (!cn1rEnsureCov(width) || !cn1rEnsureCross(e->count)) {
        return;
    }
    qsort(e->edges, (size_t) e->count, sizeof(CN1REdge), cn1rEdgeOrder);
    {
        double top = floor(e->minY);
        if (top > y0) {
            y0 = (int) top;
        }
        if (ceil(e->maxY) < y1) {
            y1 = (int) ceil(e->maxY);
        }
    }
    /* Both rows start clear, and each row clears the cells it dirtied. */
    memset(cn1rCov, 0, sizeof(float) * (size_t) (width + 2));
    memset(cn1rRun, 0, sizeof(float) * (size_t) (width + 2));
    memset(cn1rFlag, 0, (size_t) (width + 2));
    for (y = y0; y < y1; y++) {
        int s, i;
        cn1rDirtyCount = 0;
        for (s = 0; s < CN1R_SAMPLES; s++) {
            double sy = y + (s + 0.5) / CN1R_SAMPLES;
            int n = 0, winding = 0, inside = 0;
            double start = 0;
            while (next < e->count && e->edges[next].y0 <= sy) {
                cn1rActive[activeCount++] = &e->edges[next++];
            }
            for (i = 0; i < activeCount;) {
                CN1REdge* edge = cn1rActive[i];
                if (edge->y1 <= sy) {
                    cn1rActive[i] = cn1rActive[--activeCount];
                    continue;
                }
                if (edge->y0 <= sy) {
                    double cx = edge->x0 + (sy - edge->y0) * edge->dxdy;
                    int j = n;
                    /* Insertion sort: a row crosses a handful of edges. */
                    while (j > 0 && cn1rCross[j - 1] > cx) {
                        cn1rCross[j] = cn1rCross[j - 1];
                        cn1rCrossDir[j] = cn1rCrossDir[j - 1];
                        j--;
                    }
                    cn1rCross[j] = cx;
                    cn1rCrossDir[j] = edge->dir;
                    n++;
                }
                i++;
            }
            for (i = 0; i < n; i++) {
                int now;
                winding += cn1rCrossDir[i];
                now = rule == CAIRO_FILL_RULE_EVEN_ODD ? (winding & 1) : winding != 0;
                if (now && !inside) {
                    start = cn1rCross[i];
                } else if (!now && inside) {
                    double a = start - x0, b = cn1rCross[i] - x0;
                    const float w = 1.0f / CN1R_SAMPLES;
                    if (a < 0) a = 0;
                    if (b > width) b = width;
                    if (b > a) {
                        int ia = (int) a, ib = (int) b;
                        if (ia == ib) {
                            cn1rCov[ia] += (float) (b - a) * w;
                            cn1rMarkDirty(ia);
                        } else {
                            cn1rCov[ia] += (float) (ia + 1 - a) * w;
                            cn1rCov[ib] += (float) (b - ib) * w;
                            cn1rMarkDirty(ia);
                            cn1rMarkDirty(ib);
                            if (ib > ia + 1) {
                                /* The run starts after cell ia and stops at ib. */
                                cn1rRun[ia + 1] += w;
                                cn1rRun[ib] -= w;
                                cn1rMarkDirty(ia + 1);
                            }
                        }
                    }
                }
                inside = now;
            }
        }
        if (cn1rDirtyCount > 0) {
            float run = 0;
            int pos = 0, k = 0, n = 0;
            /* Sorted and unique; a row has a handful, so insertion sort. */
            if (cn1rDirtyCount > 64) {
                qsort(cn1rDirty, (size_t) cn1rDirtyCount, sizeof(int), cn1rIntOrder);
            } else {
                for (i = 1; i < cn1rDirtyCount; i++) {
                    int v = cn1rDirty[i], j = i;
                    while (j > 0 && cn1rDirty[j - 1] > v) {
                        cn1rDirty[j] = cn1rDirty[j - 1];
                        j--;
                    }
                    cn1rDirty[j] = v;
                }
            }
            for (i = 0; i < cn1rDirtyCount; i++) {
                cn1rFlag[cn1rDirty[i]] = 0;
            }
            n = cn1rDirtyCount;
            while (k < n) {
                int start = cn1rDirty[k], end = start;
                if (start > pos && run > 0 && pos < width) {
                    emit(ctx, y, x0 + pos, x0 + (start < width ? start : width), 0, run);
                }
                /* A group of adjacent dirty cells, each with its own coverage. */
                while (1) {
                    run += cn1rRun[end];
                    cn1rCov[end] += run;
                    cn1rRun[end] = 0;
                    if (k + 1 < n && cn1rDirty[k + 1] == end + 1) {
                        k++;
                        end++;
                    } else {
                        break;
                    }
                }
                if (start < width) {
                    emit(ctx, y, x0 + start, x0 + (end < width ? end + 1 : width), cn1rCov + start, 0);
                }
                memset(cn1rCov + start, 0, sizeof(float) * (size_t) (end - start + 1));
                pos = end + 1;
                k++;
            }
        }
        if (next >= e->count && activeCount == 0) {
            break;
        }
    }
}

typedef struct {
    cairo_t* cr;
    unsigned alpha;
} CN1RFillCtx;

static void cn1rFillRow(void* ctx, int y, int rx0, int rx1, const float* cov, float constant) {
    CN1RFillCtx* f = (CN1RFillCtx*) ctx;
    if (cov == 0 && constant < 1 && f->cr->state.op != CAIRO_OPERATOR_OVER) {
        /* SOURCE and CLEAR mix with what is there by the coverage, which a
         * scaled source cannot express: hand the span over as coverage. */
        float buf[64];
        int i;
        for (i = 0; i < 64; i++) {
            buf[i] = constant;
        }
        for (; rx0 < rx1; rx0 += 64) {
            cn1rRow(f->cr, y, rx0, rx1 - rx0 < 64 ? rx1 : rx0 + 64, buf, f->alpha);
        }
        return;
    }
    if (cov == 0 && constant < 1) {
        unsigned a = (unsigned) (constant * f->alpha + 0.5f);
        if (a != 0) {
            cn1rRow(f->cr, y, rx0, rx1, 0, a);
        }
        return;
    }
    cn1rRow(f->cr, y, rx0, rx1, cov, f->alpha);
}

/* An integer, axis-aligned rectangle in device space, which needs no scan. */
static int cn1rPixelRect(cairo_t* cr, int* x0, int* y0, int* x1, int* y1) {
    const double* p;
    double ax, ay, bx, by;
    int i;
    if (cr->subCount != 1 || cr->subs[0].count < 4 || cr->subs[0].count > 5) {
        return 0;
    }
    p = cr->pts + cr->subs[0].start * 2;
    if (cr->subs[0].count == 5 && (p[8] != p[0] || p[9] != p[1])) {
        return 0;
    }
    ax = bx = p[0];
    ay = by = p[1];
    for (i = 0; i < 4; i++) {
        double x = p[i * 2], y = p[i * 2 + 1];
        if (x != floor(x) || y != floor(y)) {
            return 0;
        }
        if (x < ax) ax = x;
        if (x > bx) bx = x;
        if (y < ay) ay = y;
        if (y > by) by = y;
    }
    for (i = 0; i < 4; i++) {
        int j = (i + 1) % 4;
        if (p[i * 2] != p[j * 2] && p[i * 2 + 1] != p[j * 2 + 1]) {
            return 0;
        }
    }
    *x0 = (int) ax;
    *y0 = (int) ay;
    *x1 = (int) bx;
    *y1 = (int) by;
    return 1;
}


/* ------------------------------------------------------------------- gpu */

#ifdef CN1_EMBEDDED_GPU
/*
 * The GPU back end (cn1_linux_gpu.h). Each operation is reduced to what the CPU
 * path would composite -- a device rectangle, the source and the coverage the
 * scan computes -- and handed over whole. The coverage is the CPU's own, folded
 * with the clip's mask, so both paths antialias a shape identically.
 */
static __thread unsigned char* cn1rGpuCov = 0;
static __thread size_t cn1rGpuCovCapacity = 0;

/* A zeroed mask of n bytes, reused between operations; NULL if out of memory. */
static unsigned char* cn1rGpuMask(size_t n) {
    if (n > cn1rGpuCovCapacity) {
        size_t capacity = cn1rGpuCovCapacity == 0 ? 65536 : cn1rGpuCovCapacity;
        unsigned char* grown;
        while (capacity < n) {
            capacity *= 2;
        }
        grown = (unsigned char*) realloc(cn1rGpuCov, capacity);
        if (grown == 0) {
            return 0;
        }
        cn1rGpuCov = grown;
        cn1rGpuCovCapacity = capacity;
    }
    memset(cn1rGpuCov, 0, n);
    return cn1rGpuCov;
}

static void cn1rGpuSource(cairo_t* cr, CN1GpuSource* g) {
    cairo_pattern_t* p = cr->state.source;
    memset(g, 0, sizeof(*g));
    if (p->kind == CN1R_SOLID) {
        g->solid = 1;
        g->pixel = p->pixel;
    } else {
        g->surface = p->surface;
        g->toSource = p->toSource;
        g->filter = p->filter;
    }
}

typedef struct {
    unsigned char* mask;
    int x0, y0, w;
    const CN1RClip* clip;
} CN1RGpuCtx;

static void cn1rGpuRow(void* ctx, int y, int rx0, int rx1, const float* cov, float constant) {
    CN1RGpuCtx* g = (CN1RGpuCtx*) ctx;
    unsigned char* row = g->mask + (size_t) (y - g->y0) * (size_t) g->w - g->x0;
    const unsigned char* clip = 0;
    int x;
    if (g->clip != 0 && g->clip->mask != 0) {
        clip = g->clip->mask + (size_t) (y - g->clip->y0) * (size_t) (g->clip->x1 - g->clip->x0) - g->clip->x0;
    }
    for (x = rx0; x < rx1; x++) {
        float f = cov != 0 ? cov[x - rx0] : constant;
        unsigned v = f <= 0 ? 0 : f >= 1 ? 255 : (unsigned) (f * 255 + 0.5f);
        if (clip != 0) {
            v = (v * clip[x] + 127) / 255;
        }
        row[x] = (unsigned char) v;
    }
}

/* A device rectangle at full coverage, under the clip. */
static void cn1rGpuRect(cairo_t* cr, int x0, int y0, int x1, int y1, unsigned alpha) {
    CN1RClip* clip = cr->state.clip;
    CN1GpuSource src;
    if (x1 <= x0 || y1 <= y0) {
        return;
    }
    cn1rGpuSource(cr, &src);
    if (clip != 0 && clip->mask != 0) {
        int w = x1 - x0, y;
        int cw = clip->x1 - clip->x0;
        unsigned char* mask = cn1rGpuMask((size_t) w * (size_t) (y1 - y0));
        if (mask == 0) {
            return;
        }
        /* cn1rBounds keeps the rectangle inside the clip's bounds. */
        for (y = y0; y < y1; y++) {
            memcpy(mask + (size_t) (y - y0) * (size_t) w,
                   clip->mask + (size_t) (y - clip->y0) * (size_t) cw + (x0 - clip->x0), (size_t) w);
        }
        cn1GpuDraw(cr->target, x0, y0, x1, y1, &src, alpha, cr->state.op, mask, w);
    } else {
        cn1GpuDraw(cr->target, x0, y0, x1, y1, &src, alpha, cr->state.op, 0, 0);
    }
}

/* A path's edges, scanned into a mask over their bounds. */
static void cn1rGpuEdges(cairo_t* cr, CN1REdges* e, int rule) {
    int x0, y0, x1, y1;
    CN1RGpuCtx ctx;
    CN1GpuSource src;
    cn1rBounds(cr, &x0, &y0, &x1, &y1);
    cn1rNarrow(&x0, &y0, &x1, &y1, e->minX, e->minY, e->maxX, e->maxY);
    if (cr->state.source->kind == CN1R_SURFACE) {
        cairo_pattern_t* p = cr->state.source;
        cn1rNarrow(&x0, &y0, &x1, &y1, p->bx0, p->by0, p->bx1, p->by1);
    }
    if (x1 <= x0 || y1 <= y0) {
        return;
    }
    ctx.mask = cn1rGpuMask((size_t) (x1 - x0) * (size_t) (y1 - y0));
    if (ctx.mask == 0) {
        return;
    }
    ctx.x0 = x0;
    ctx.y0 = y0;
    ctx.w = x1 - x0;
    ctx.clip = cr->state.clip;
    cn1rScan(e, rule, x0, y0, x1, y1, cn1rGpuRow, &ctx);
    cn1rGpuSource(cr, &src);
    cn1GpuDraw(cr->target, x0, y0, x1, y1, &src, 255, cr->state.op, ctx.mask, ctx.w);
}
#endif /* CN1_EMBEDDED_GPU */

static void cn1rFillEdges(cairo_t* cr, CN1REdges* e, int rule, unsigned alpha) {
    int x0, y0, x1, y1;
    CN1RFillCtx ctx;
    cn1rBounds(cr, &x0, &y0, &x1, &y1);
    cn1rNarrow(&x0, &y0, &x1, &y1, e->minX, e->minY, e->maxX, e->maxY);
    if (cr->state.source->kind == CN1R_SURFACE) {
        cairo_pattern_t* p = cr->state.source;
        cn1rNarrow(&x0, &y0, &x1, &y1, p->bx0, p->by0, p->bx1, p->by1);
    }
    ctx.cr = cr;
    ctx.alpha = alpha;
    cn1rScan(e, rule, x0, y0, x1, y1, cn1rFillRow, &ctx);
}

void cn1r_fill(cairo_t* cr) {
    int rx0, ry0, rx1, ry1;
    if (cr->target == 0 || cr->state.source == 0) {
        cn1r_new_path(cr);
        return;
    }
    if (cn1rPixelRect(cr, &rx0, &ry0, &rx1, &ry1)) {
        int x0, y0, x1, y1, y;
        cn1rBounds(cr, &x0, &y0, &x1, &y1);
        if (rx0 > x0) x0 = rx0;
        if (ry0 > y0) y0 = ry0;
        if (rx1 < x1) x1 = rx1;
        if (ry1 < y1) y1 = ry1;
        if (cr->state.source->kind == CN1R_SURFACE) {
            cairo_pattern_t* p = cr->state.source;
            cn1rNarrow(&x0, &y0, &x1, &y1, p->bx0, p->by0, p->bx1, p->by1);
        }
#ifdef CN1_EMBEDDED_GPU
        if (cr->target->gpu) {
            cn1rGpuRect(cr, x0, y0, x1, y1, 255);
            cn1r_new_path(cr);
            return;
        }
#endif
        for (y = y0; y < y1; y++) {
            if (x1 > x0) {
                cn1rRow(cr, y, x0, x1, 0, 255);
            }
        }
    } else {
        CN1REdges e;
        cn1rEdgesInit(&e);
        cn1rPathEdges(cr, &e);
#ifdef CN1_EMBEDDED_GPU
        if (cr->target->gpu) {
            cn1rGpuEdges(cr, &e, cr->state.fillRule);
        } else
#endif
        cn1rFillEdges(cr, &e, cr->state.fillRule, 255);
        free(e.edges);
    }
    cr->target->version++;
    cn1r_new_path(cr);
}

void cn1r_stroke(cairo_t* cr) {
    CN1REdges e;
    if (cr->target == 0 || cr->state.source == 0) {
        cn1r_new_path(cr);
        return;
    }
    cn1rEdgesInit(&e);
    cn1rStrokeEdges(cr, &e);
#ifdef CN1_EMBEDDED_GPU
    if (cr->target->gpu) {
        cn1rGpuEdges(cr, &e, CAIRO_FILL_RULE_WINDING);
    } else
#endif
    cn1rFillEdges(cr, &e, CAIRO_FILL_RULE_WINDING, 255);
    free(e.edges);
    cr->target->version++;
    cn1r_new_path(cr);
}

void cn1r_paint_with_alpha(cairo_t* cr, double alpha) {
    int x0, y0, x1, y1, y;
    unsigned a;
    if (cr->target == 0 || cr->state.source == 0 || !(alpha > 0)) {
        return;
    }
    a = alpha >= 1 ? 255 : (unsigned) (alpha * 255 + 0.5);
    cn1rBounds(cr, &x0, &y0, &x1, &y1);
    if (cr->state.source->kind == CN1R_SURFACE) {
        cairo_pattern_t* p = cr->state.source;
        cn1rNarrow(&x0, &y0, &x1, &y1, p->bx0, p->by0, p->bx1, p->by1);
    }
    cr->target->version++;
#ifdef CN1_EMBEDDED_GPU
    if (cr->target->gpu) {
        cn1rGpuRect(cr, x0, y0, x1, y1, a);
        return;
    }
#endif
    for (y = y0; y < y1; y++) {
        if (x1 > x0) {
            cn1rRow(cr, y, x0, x1, 0, a);
        }
    }
}

void cn1r_paint(cairo_t* cr) {
    cn1r_paint_with_alpha(cr, 1.0);
}

/* ---------------------------------------------------------------------- clip */

typedef struct {
    CN1RClip* clip;
    const CN1RClip* old;
} CN1RClipCtx;

static void cn1rClipRow(void* ctx, int y, int rx0, int rx1, const float* cov, float constant) {
    CN1RClipCtx* c = (CN1RClipCtx*) ctx;
    int w = c->clip->x1 - c->clip->x0;
    unsigned char* row = c->clip->mask + (size_t) (y - c->clip->y0) * (size_t) w - c->clip->x0;
    const unsigned char* old = 0;
    int x;
    if (c->old != 0 && c->old->mask != 0) {
        old = c->old->mask + (size_t) (y - c->old->y0) * (size_t) (c->old->x1 - c->old->x0) - c->old->x0;
    }
    for (x = rx0; x < rx1; x++) {
        float f = cov != 0 ? cov[x - rx0] : constant;
        unsigned v = f <= 0 ? 0 : f >= 1 ? 255 : (unsigned) (f * 255 + 0.5f);
        if (old != 0) {
            v = (v * old[x] + 127) / 255;
        }
        row[x] = (unsigned char) v;
    }
}

void cn1r_clip(cairo_t* cr) {
    const CN1RClip* old = cr->state.clip;
    CN1RClip* clip;
    int x0, y0, x1, y1, rx0, ry0, rx1, ry1;
    if (cr->target == 0) {
        cn1r_new_path(cr);
        return;
    }
    clip = (CN1RClip*) calloc(1, sizeof(CN1RClip));
    if (clip == 0) {
        cn1r_new_path(cr);
        return;
    }
    clip->refs = 1;
    cn1rBounds(cr, &x0, &y0, &x1, &y1);
    if (cn1rPixelRect(cr, &rx0, &ry0, &rx1, &ry1)) {
        if (rx0 > x0) x0 = rx0;
        if (ry0 > y0) y0 = ry0;
        if (rx1 < x1) x1 = rx1;
        if (ry1 < y1) y1 = ry1;
        if (x1 < x0) x1 = x0;
        if (y1 < y0) y1 = y0;
        clip->x0 = x0;
        clip->y0 = y0;
        clip->x1 = x1;
        clip->y1 = y1;
        if (old != 0 && old->mask != 0 && x1 > x0 && y1 > y0) {
            /* A rectangle inside a shaped clip keeps the shape's mask. */
            int y, w = x1 - x0;
            clip->mask = (unsigned char*) malloc((size_t) w * (size_t) (y1 - y0));
            if (clip->mask != 0) {
                for (y = y0; y < y1; y++) {
                    memcpy(clip->mask + (size_t) (y - y0) * (size_t) w,
                           old->mask + (size_t) (y - old->y0) * (size_t) (old->x1 - old->x0) + (x0 - old->x0), (size_t) w);
                }
            }
        }
    } else {
        CN1REdges e;
        CN1RClipCtx ctx;
        cn1rEdgesInit(&e);
        cn1rPathEdges(cr, &e);
        cn1rNarrow(&x0, &y0, &x1, &y1, e.minX, e.minY, e.maxX, e.maxY);
        if (x1 < x0) x1 = x0;
        if (y1 < y0) y1 = y0;
        clip->x0 = x0;
        clip->y0 = y0;
        clip->x1 = x1;
        clip->y1 = y1;
        if (x1 > x0 && y1 > y0) {
            clip->mask = (unsigned char*) calloc((size_t) (x1 - x0) * (size_t) (y1 - y0), 1);
            if (clip->mask != 0) {
                ctx.clip = clip;
                ctx.old = old;
                cn1rScan(&e, cr->state.fillRule, x0, y0, x1, y1, cn1rClipRow, &ctx);
            }
        }
        free(e.edges);
    }
    cn1rClipRelease(cr->state.clip);
    cr->state.clip = clip;
    cn1r_new_path(cr);
}

/* ---------------------------------------------------------------- a8 masks */

void cn1r_mask_a8(cairo_t* cr, const unsigned char* mask, int pitch, int width, int height, int x, int y) {
    int x0, y0, x1, y1, row;
    if (cr->target == 0 || cr->state.source == 0 || mask == 0) {
        return;
    }
    cn1rBounds(cr, &x0, &y0, &x1, &y1);
    if (x > x0) x0 = x;
    if (y > y0) y0 = y;
    if (x + width < x1) x1 = x + width;
    if (y + height < y1) y1 = y + height;
    if (x1 <= x0 || y1 <= y0) {
        return;
    }
    cr->target->version++;
#ifdef CN1_EMBEDDED_GPU
    if (cr->target->gpu) {
        CN1RClip* clip = cr->state.clip;
        CN1GpuSource src;
        const unsigned char* m = mask + (size_t) (y0 - y) * (size_t) pitch + (x0 - x);
        cn1rGpuSource(cr, &src);
        if (clip != 0 && clip->mask != 0) {
            int w = x1 - x0, cw = clip->x1 - clip->x0, i;
            unsigned char* folded = cn1rGpuMask((size_t) w * (size_t) (y1 - y0));
            if (folded == 0) {
                return;
            }
            for (row = y0; row < y1; row++) {
                const unsigned char* g = m + (size_t) (row - y0) * (size_t) pitch;
                const unsigned char* c = clip->mask + (size_t) (row - clip->y0) * (size_t) cw + (x0 - clip->x0);
                unsigned char* o = folded + (size_t) (row - y0) * (size_t) w;
                for (i = 0; i < w; i++) {
                    o[i] = (unsigned char) ((g[i] * c[i] + 127) / 255);
                }
            }
            cn1GpuDraw(cr->target, x0, y0, x1, y1, &src, 255, cr->state.op, folded, w);
        } else {
            cn1GpuDraw(cr->target, x0, y0, x1, y1, &src, 255, cr->state.op, m, pitch);
        }
        return;
    }
#endif
    if (!cn1rEnsureCov(x1 - x0)) {
        return;
    }
    for (row = y0; row < y1; row++) {
        const unsigned char* m = mask + (size_t) (row - y) * (size_t) pitch + (x0 - x);
        int i, any = 0;
        for (i = 0; i < x1 - x0; i++) {
            cn1rCov[i] = m[i] * (1.0f / 255);
            any |= m[i];
        }
        if (any) {
            cn1rRow(cr, row, x0, x1, cn1rCov, 255);
        }
    }
}

#endif /* CN1_LINUX_EMBEDDED */
