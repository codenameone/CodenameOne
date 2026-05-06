/*
 * Copyright (c) 2012, Codename One and/or its affiliates. All rights reserved.
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
#import "CN1ES2compat.h"
#import "DrawTextureAlphaMask.h"
#import "PaintOp.h"
#import "RadialGradientPaint.h"
#import "CodenameOne_GLViewController.h"
#ifdef CN1_USE_METAL
#import "CN1Metalcompat.h"
#endif

#ifdef USE_ES2
extern GLKMatrix4 CN1modelViewMatrix;
extern GLKMatrix4 CN1projectionMatrix;
extern GLKMatrix4 CN1transformMatrix;
extern int CN1modelViewMatrixVersion;
extern int CN1projectionMatrixVersion;
extern int CN1transformMatrixVersion;
extern GLuint CN1activeProgram;

@interface DrawTextureAlphaMaskOGLProgram : NSObject {
    NSString* fragmentShaderSrc;
    NSString* vertexShaderSrc;
    GLuint program;
    GLuint modelViewMatrixUniform;
    GLuint projectionMatrixUniform;
    GLuint transformMatrixUniform;
    GLuint centerPointUniform;
    GLuint radiusXUniform;
    GLuint radiusYUniform;
    GLuint textureUniform;
    GLuint startColorUniform;
    GLuint endColorUniform;
    GLuint colorUniform;
    GLuint vertexCoordAtt;
    GLuint textureCoordAtt;
    
    int currentCN1modelViewMatrixVersion;
    int currentCN1projectionMatrixVersion;
    int currentCN1transformMatrixVersion;
}


@property GLuint program;
@property GLuint textureCoordAtt;
@property GLuint vertexCoordAtt;
@property GLuint textureUniform;
@property GLuint colorUniform;
@property GLuint centerPointUniform;
@property GLuint startColorUniform;
@property GLuint endColorUniform;
@property GLuint radiusXUniform;
@property GLuint radiusYUniform;

-(id)init:(NSString*) vs fragmentShader:(NSString*)fs;
+(DrawTextureAlphaMaskOGLProgram*)createBasicProgram;
+(DrawTextureAlphaMaskOGLProgram*)createRadialGradientProgram;
-(void)updateMatrices;

@end

@implementation DrawTextureAlphaMaskOGLProgram

@synthesize program;
@synthesize textureCoordAtt;
@synthesize vertexCoordAtt;
@synthesize textureUniform;
@synthesize colorUniform;
@synthesize centerPointUniform;
@synthesize radiusXUniform;
@synthesize radiusYUniform;
@synthesize startColorUniform;
@synthesize endColorUniform;

-(id)init:(NSString*) vs fragmentShader:(NSString*)fs
{
    vertexShaderSrc = vs;
    fragmentShaderSrc = fs;
    currentCN1modelViewMatrixVersion=-1;
    currentCN1projectionMatrixVersion=-1;
    currentCN1transformMatrixVersion=-1;
    
    
    
    program = CN1compileShaderProgram(vertexShaderSrc, fragmentShaderSrc);
    GLErrorLog;
    vertexCoordAtt = glGetAttribLocation(program, "aVertexCoord");
    GLErrorLog;
    
    textureCoordAtt = glGetAttribLocation(program, "aTextureRGBACoord");
    GLErrorLog;
    
    modelViewMatrixUniform = glGetUniformLocation(program, "uModelViewMatrix");
    GLErrorLog;
    
    centerPointUniform = glGetUniformLocation(program, "uCenterPoint");
    GLErrorLog;
    
    radiusXUniform = glGetUniformLocation(program, "uRadiusX");
    GLErrorLog;
    
    radiusYUniform = glGetUniformLocation(program, "uRadiusY");
    GLErrorLog;
    
    startColorUniform = glGetUniformLocation(program, "uStartColor");
    GLErrorLog;
    
    endColorUniform = glGetUniformLocation(program, "uEndColor");
    GLErrorLog;
    
    projectionMatrixUniform = glGetUniformLocation(program, "uProjectionMatrix");
    GLErrorLog;
    transformMatrixUniform = glGetUniformLocation(program, "uTransformMatrix");
    GLErrorLog;
    textureUniform = glGetUniformLocation(program, "uTextureRGBA");
    GLErrorLog;
    colorUniform = glGetUniformLocation(program, "uColor");
    GLErrorLog;
    
    glBindBuffer(GL_ARRAY_BUFFER, 0);
    GLErrorLog;
    return self;
}

+(DrawTextureAlphaMaskOGLProgram*)createBasicProgram
{
    NSString *fragmentShaderSrc =
    @"precision highp float;\n"
    "uniform lowp vec4 uColor;\n"
    "uniform highp sampler2D uTextureRGBA;\n"
    "varying highp vec2 vTextureRGBACoord;\n"
    
    "void main(){\n"
    //"   gl_FragColor = texture2D(uTextureRGBA, vTextureRGBACoord) * uColor; \n"
    "   float texA = texture2D(uTextureRGBA, vTextureRGBACoord).a;\n"
    "   vec4 color = vec4(uColor.rgb*texA, texA*uColor.a);\n"
    //"   gl_FragColor = color;\n"
    "   if ( color.a < .0001 ){ discard;} else {gl_FragColor = color;}\n"
    //"   if ( color.a < .01 ){ color.a = 0.0; gl_FragColor=color;} else {gl_FragColor = color;}\n"
    "}\n";
    
    
    NSString *vertexShaderSrc =
    @"attribute vec4 aVertexCoord;\n"
    "attribute vec2 aTextureRGBACoord;\n"
    
    "uniform mat4 uModelViewMatrix;\n"
    "uniform mat4 uProjectionMatrix;\n"
    "uniform mat4 uTransformMatrix;\n"
    
    "varying highp vec2 vTextureRGBACoord;\n"
    
    "void main(){\n"
    "   gl_Position = uProjectionMatrix *  uModelViewMatrix * uTransformMatrix * aVertexCoord;\n"
    "   vTextureRGBACoord = aTextureRGBACoord;\n"
    "}";
    
    return [[DrawTextureAlphaMaskOGLProgram alloc] init:vertexShaderSrc fragmentShader:fragmentShaderSrc];
    
}
+(DrawTextureAlphaMaskOGLProgram*)createRadialGradientProgram
{
    NSString *fragmentShaderSrc =
    @"precision highp float;\n"
    "uniform highp sampler2D uTextureRGBA;\n"
    "uniform lowp vec4 uStartColor;\n"
    "uniform lowp vec4 uEndColor;\n"
    "uniform lowp vec4 uCenterPoint;\n"
    "uniform lowp float uRadiusX;\n"
    "uniform lowp float uRadiusY;\n"
    "varying highp vec2 vTextureRGBACoord;\n"
    
    "void main(){\n"
    //"   gl_FragColor = texture2D(uTextureRGBA, vTextureRGBACoord) * uColor; \n"
    "   float texA = texture2D(uTextureRGBA, vTextureRGBACoord).a;\n"
    "   float dist = distance(vTextureRGBACoord.xy, uCenterPoint.xy);\n"
    "   vec2 p1 = vTextureRGBACoord.xy - uCenterPoint.xy;\n"

    "   float x = p1.x*uRadiusX*uRadiusY / sqrt(p1.x*p1.x*uRadiusY*uRadiusY+p1.y*p1.y*uRadiusX*uRadiusX);\n "
    "   float y = p1.y * x / p1.x;\n"
    "   vec2 p2 = vec2(x, y) + uCenterPoint.xy;\n"
    "   vec4 color = mix(uStartColor, uEndColor, dist/distance(uCenterPoint.xy, p2.xy))*texA;\n"
    //"   color = vec4(color.rgb*texA, texA);\n"
    //"   gl_FragColor = color;\n"
    "   if ( color.a < .0001 ){ discard;} else {gl_FragColor = color;}\n"
    //"   if ( color.a < .01 ){ color.a = 0.0; gl_FragColor=color;} else {gl_FragColor = color;}\n"
    "}\n";
    
    
    NSString *vertexShaderSrc =
    @"attribute vec4 aVertexCoord;\n"
    "attribute vec2 aTextureRGBACoord;\n"
    
    "uniform mat4 uModelViewMatrix;\n"
    "uniform mat4 uProjectionMatrix;\n"
    "uniform mat4 uTransformMatrix;\n"
    
    "varying highp vec2 vTextureRGBACoord;\n"
    
    "void main(){\n"
    "   gl_Position = uProjectionMatrix *  uModelViewMatrix * uTransformMatrix * aVertexCoord;\n"
    "   vTextureRGBACoord = aTextureRGBACoord;\n"
    "}";
    
    return [[DrawTextureAlphaMaskOGLProgram alloc] init:vertexShaderSrc fragmentShader:fragmentShaderSrc];
    
}


-(void)updateMatrices {
    if (currentCN1projectionMatrixVersion != CN1projectionMatrixVersion) {
        glUniformMatrix4fv(projectionMatrixUniform, 1, 0, CN1projectionMatrix.m);
        currentCN1projectionMatrixVersion = CN1projectionMatrixVersion;
        
        GLErrorLog;
    }
    if (currentCN1modelViewMatrixVersion != CN1modelViewMatrixVersion) {
        glUniformMatrix4fv(modelViewMatrixUniform, 1, 0, CN1modelViewMatrix.m);
        currentCN1modelViewMatrixVersion = CN1modelViewMatrixVersion;
        GLErrorLog;
    }
    if (currentCN1transformMatrixVersion != CN1transformMatrixVersion) {
        glUniformMatrix4fv(transformMatrixUniform, 1, 0, CN1transformMatrix.m);
        GLErrorLog;
        currentCN1transformMatrixVersion = CN1transformMatrixVersion;
    }
}
@end

static DrawTextureAlphaMaskOGLProgram* basicProgram = NULL;
static DrawTextureAlphaMaskOGLProgram* radialGradientProgram = NULL;


static const GLshort textureCoordinates[] = {
    0, 0,
    1, 0,
    0, 1,
    1, 1,
};


static DrawTextureAlphaMaskOGLProgram* getOGLProgram() {
    if ([PaintOp getCurrent] != NULL && [[PaintOp getCurrent] isKindOfClass:[RadialGradientPaint class]]) {
        if (radialGradientProgram == NULL) {
            radialGradientProgram = [DrawTextureAlphaMaskOGLProgram createRadialGradientProgram];
        }
        return radialGradientProgram;
    } else {
        if (basicProgram == NULL) {
            basicProgram = [DrawTextureAlphaMaskOGLProgram createBasicProgram];
        }
        return basicProgram;
    }
}


#endif

@implementation DrawTextureAlphaMask

-(id)initWithArgs:(JAVA_LONG)pTextName color:(int)pColor alpha:(int)pAlpha x:(int)pX y:(int)pY w:(int)pW h:(int)pH
{
    textureName = pTextName;
    color = pColor;
    alpha = pAlpha;
    x = pX;
    y = pY;
    w = pW;
    h = pH;
#ifdef CN1_USE_METAL
    // The handle is a CFBridgingRetain'd id<MTLTexture> owned by the Java
    // TextureAlphaMask object (which the textureCache holds only as a
    // SoftWeak ref). With the concurrent GC, finalize() can run between
    // op queueing and drawFrame draining; finalize -> dispose ->
    // nativeDeleteTexture -> CFBridgingRelease drops the +1, and we'd
    // sample a freed texture at execute time. Take an additional retain
    // here so the texture survives until this op deallocs after drain.
    if (textureName != 0) {
        CFRetain((CFTypeRef)(void *)(uintptr_t)textureName);
    }
    // Snapshot the active RadialGradientPaint at queue time so the op
    // renders the gradient even after the mutable-side unapplyPaint clears
    // PaintOp.currentMutable. Read from currentMutable -- the screen path
    // already queues a RadialGradientPaint op that runs in-order with
    // execute, so its mutation happens between this op's queue and execute.
    hasRadialPaint = NO;
    PaintOp *snapshot = [PaintOp getCurrentMutable];
    if (snapshot != NULL && [snapshot isKindOfClass:[RadialGradientPaint class]]) {
        RadialGradientPaint *g = (RadialGradientPaint *)snapshot;
        hasRadialPaint = YES;
        radialStartColor = g.startColor;
        radialEndColor = g.endColor;
        radialX = g.x;
        radialY = g.y;
        radialWidth = g.width;
        radialHeight = g.height;
    }
#endif
    return self;
}

#ifdef CN1_USE_METAL
-(void)dealloc {
    if (textureName != 0) {
        CFRelease((CFTypeRef)(void *)(uintptr_t)textureName);
    }
#ifndef CN1_USE_ARC
    [super dealloc];
#endif
}
#endif
#ifdef USE_ES2
-(void)execute
{
#ifdef CN1_USE_METAL
    if (textureName == 0) {
        CN1Log(@"Attempt to draw null alpha-mask texture. Skipping");
        return;
    }
    // textureName is a CFBridgingRetain'd id<MTLTexture> set up by
    // IOSNative.m's nativePathRendererCreateTexture under Metal. Just bridge
    // back to the Obj-C handle (no transfer of ownership) and dispatch.
    id<MTLTexture> tex = (__bridge id<MTLTexture>)(void *)(uintptr_t)textureName;
    // Resolve the radial-gradient paint differently per target:
    //
    //   Mutable (target != nil): the Java-side applyPaint() and
    //     unapplyPaint() invoke applyRadialGradientPaintMutable /
    //     clearRadialGradientPaintMutable as direct C calls -- they set and
    //     clear PaintOp.currentMutable synchronously around the queue call,
    //     before drainOps runs. Reading PaintOp.currentMutable at execute
    //     time is too late; use the snapshot captured at init.
    //
    //   Screen (target == nil): the Java-side applyPaint() invokes
    //     applyRadialGradientPaintGlobal which queues a RadialGradientPaint
    //     op into the same queue as this DrawTextureAlphaMask op. Its
    //     execute sets PaintOp.current just before our execute runs, so
    //     reading it here is correct.
    if (target != nil && hasRadialPaint) {
        CN1MetalDrawAlphaMaskRadial(tex, x, y, w, h,
                                    radialStartColor, radialEndColor,
                                    (float)radialX, (float)radialY,
                                    (float)radialWidth, (float)radialHeight);
        return;
    }
    if (target == nil) {
        PaintOp *paint = [PaintOp getCurrent];
        if (paint != NULL && [paint isKindOfClass:[RadialGradientPaint class]]) {
            RadialGradientPaint *g = (RadialGradientPaint *)paint;
            CN1MetalDrawAlphaMaskRadial(tex, x, y, w, h,
                                        g.startColor, g.endColor,
                                        (float)g.x, (float)g.y,
                                        (float)g.width, (float)g.height);
            return;
        }
    }
    CN1MetalDrawAlphaMask(tex, color, alpha, x, y, w, h);
#else
    //RadialGradientPaint * gp = [[RadialGradientPaint alloc ]initWithArgs:0 y:0 width:[CodenameOne_GLViewController instance].view.bounds.size.width*2 height:[CodenameOne_GLViewController instance].view.bounds.size.height*2 startColor:0x0 endColor:0xffffff];
    //[PaintOp setCurrent:gp];

    if ( textureName == 0 ){
        CN1Log(@"Attempt to draw null texture.  Skipping");
    }
    DrawTextureAlphaMaskOGLProgram* p = getOGLProgram();

    glUseProgram(p.program);
    float alph = ((float)alpha)/255.0;
    GLKVector4 colorV = GLKVector4Make(((float)((color >> 16) & 0xff))/255.0*alph, \
                                       ((float)((color >> 8) & 0xff))/255.0*alph, ((float)(color & 0xff))/255.0*alph, alph);
    
    
    GLfloat vertexes[] = {
        (GLfloat)x, (GLfloat)y,
        (GLfloat)(x+w), (GLfloat)y,
        (GLfloat)x, (GLfloat)(y+h),
        (GLfloat)(x+w), (GLfloat)(y+h)
    };
    
    glActiveTexture(GL_TEXTURE0);
    GLErrorLog;
    glBindTexture(GL_TEXTURE_2D, (GLuint)textureName);
    GLErrorLog;
    glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_LINEAR);
    GLErrorLog;
    glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_LINEAR);
    GLErrorLog;
    glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_S, GL_CLAMP_TO_EDGE);
    GLErrorLog;
    glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_T, GL_CLAMP_TO_EDGE);
    GLErrorLog;
    
    glPixelStorei(GL_UNPACK_ALIGNMENT, 1);
    GLErrorLog;
    
    
    glEnableVertexAttribArray(p.textureCoordAtt);
    GLErrorLog;
    
    glEnableVertexAttribArray(p.vertexCoordAtt);
    GLErrorLog;
    
    glVertexAttribPointer(p.textureCoordAtt, 2, GL_SHORT, 0, 0, textureCoordinates);
    GLErrorLog;
    
    [p updateMatrices];
    
    glUniform1i(p.textureUniform, 0);
    GLErrorLog;
    
    
    
    if ([PaintOp getCurrent] != NULL && [[PaintOp getCurrent] isKindOfClass:[RadialGradientPaint class]]) {
        RadialGradientPaint *paint = (RadialGradientPaint*)[PaintOp getCurrent];
        int scolor = paint.startColor;
        int ecolor = paint.endColor;
        GLKVector4 startColorV = GLKVector4Make(((float)((scolor >> 16) & 0xff))/255.0, \
                                                ((float)((scolor >> 8) & 0xff))/255.0, ((float)(scolor & 0xff))/255.0, 1.0);
        GLErrorLog;
        GLKVector4 endColorV = GLKVector4Make(((float)((ecolor >> 16) & 0xff))/255.0, \
                                              ((float)((ecolor >> 8) & 0xff))/255.0, ((float)(ecolor & 0xff))/255.0, 1.0);
        GLErrorLog;
        glUniform4fv(p.startColorUniform, 1, startColorV.v);
        GLErrorLog;
        glUniform4fv(p.endColorUniform, 1, endColorV.v);
        GLErrorLog;
        
        //GLKVector2 centerPoint = GLKVector2Make(x+w/2, y+h/2);
        
        // We need to find the center point in texture coordinates.
        GLfloat gradientCenterX = (paint.x + paint.width / 2.0 - x) / w;
        GLfloat gradientCenterY = (paint.y + paint.height / 2.0 - y) / h;
        // This should give is the coordinates of the gradient center
        // relative to [(0,0),(1,1)] texture space.
        // Because we want to know the position of the gradient center
        // in comparison to the texture coordinate
        
        
        glUniform4f(p.centerPointUniform, gradientCenterX, gradientCenterY, 0, 0);
        GLErrorLog;
        
        glUniform1f(p.radiusXUniform, paint.width /2.0 /w);
        GLErrorLog;
        
        glUniform1f(p.radiusYUniform, paint.height /2.0 /h);
        GLErrorLog;
        
    } else {
        glUniform4fv(p.colorUniform, 1, colorV.v);
        GLErrorLog;
    }
    
    
    glVertexAttribPointer(p.vertexCoordAtt, 2, GL_FLOAT, GL_FALSE, 0, vertexes);
    GLErrorLog;
    glDrawArrays(GL_TRIANGLE_STRIP, 0, 4);
    GLErrorLog;
    
    glBindTexture(GL_TEXTURE_2D, 0);
    GLErrorLog;
    
    glBindBuffer(GL_ARRAY_BUFFER, 0);
    glDisableVertexAttribArray(p.textureCoordAtt);
    GLErrorLog;
    
    glDisableVertexAttribArray(p.vertexCoordAtt);
    GLErrorLog;

    glBindTexture(GL_TEXTURE_2D, 0);
    GLErrorLog;
#endif // CN1_USE_METAL
}
#else
-(void)execute {}
#endif
@end
