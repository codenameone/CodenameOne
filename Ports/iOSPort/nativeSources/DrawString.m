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
#import "DrawString.h"
#import "CodenameOne_GLViewController.h"
#import "DrawStringTextureCache.h"
#include "xmlvm.h"

#ifdef CN1_USE_METAL
#import <Metal/Metal.h>
#import <simd/simd.h>
#import "CN1METALTransform.h"
#endif

extern float scaleValue;
#ifdef USE_ES2
extern GLKMatrix4 CN1modelViewMatrix;
extern GLKMatrix4 CN1projectionMatrix;
extern GLKMatrix4 CN1transformMatrix;
extern int CN1modelViewMatrixVersion;
extern int CN1projectionMatrixVersion;
extern int CN1transformMatrixVersion;
extern GLuint CN1activeProgram;
static GLuint program=0;
static GLuint vertexShader;
static GLuint fragmentShader;
static GLuint modelViewMatrixUniform;
static GLuint projectionMatrixUniform;
static GLuint transformMatrixUniform;
static GLuint textureUniform;
static GLuint colorUniform;
static GLuint vertexCoordAtt;
static GLuint textureCoordAtt;
static const GLshort textureCoordinates[] = {
    0, 1,
    1, 1,
    0, 0,
    1, 0,
};
static int currentCN1modelViewMatrixVersion=-1;
static int currentCN1projectionMatrixVersion=-1;
static int currentCN1transformMatrixVersion=-1;


static NSString *fragmentShaderSrc =
@"precision highp float;\n"
"uniform lowp vec4 uColor;\n"
"uniform highp sampler2D uTextureRGBA;\n"
"varying highp vec2 vTextureRGBACoord;\n"

"void main(){\n"
"   gl_FragColor = texture2D(uTextureRGBA, vTextureRGBACoord) * uColor; \n"
"}\n";

static NSString *vertexShaderSrc =
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

static GLuint getOGLProgram(){
    if ( program == 0  ){
        program = CN1compileShaderProgram(vertexShaderSrc, fragmentShaderSrc);
        GLErrorLog;
        vertexCoordAtt = glGetAttribLocation(program, "aVertexCoord");
        GLErrorLog;
        
        textureCoordAtt = glGetAttribLocation(program, "aTextureRGBACoord");
        GLErrorLog;
        
        modelViewMatrixUniform = glGetUniformLocation(program, "uModelViewMatrix");
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
        
        
    }
    return program;
}

#endif

@implementation DrawString
-(id)initWithArgs:(int)c a:(int)a xpos:(int)xpos ypos:(int)ypos s:(NSString*)s f:(UIFont*)f {
    color = c;
    alpha = a;
    x = xpos;
    y = ypos;
    str = s;
    font = f;
#ifndef CN1_USE_ARC
    [str retain];
    [font retain];
#endif
    return self;
}

#ifdef CN1_USE_METAL
-(void)execute {
    // Metal rendering path
    id<MTLRenderCommandEncoder> encoder = [self makeRenderCommandEncoder];
    if (!encoder) {
        return;
    }

    // Get pipeline state (same as DrawImage - uses textured shader)
    static id<MTLRenderPipelineState> pipelineState = nil;
    static dispatch_once_t onceToken;
    dispatch_once(&onceToken, ^{
        id<MTLDevice> device = [self device];
        id<MTLLibrary> library = [device newDefaultLibrary];

        MTLRenderPipelineDescriptor *pipelineDescriptor = [[MTLRenderPipelineDescriptor alloc] init];
        pipelineDescriptor.vertexFunction = [library newFunctionWithName:@"textured_vertex"];
        pipelineDescriptor.fragmentFunction = [library newFunctionWithName:@"textured_fragment"];
        pipelineDescriptor.colorAttachments[0].pixelFormat = MTLPixelFormatBGRA8Unorm;

        // Configure vertex descriptor for textured shader
        MTLVertexDescriptor *vertexDescriptor = [[MTLVertexDescriptor alloc] init];
        // Position attribute (float2) at attribute 0
        vertexDescriptor.attributes[0].format = MTLVertexFormatFloat2;
        vertexDescriptor.attributes[0].offset = 0;
        vertexDescriptor.attributes[0].bufferIndex = 0;
        // TexCoord attribute (float2) at attribute 1
        vertexDescriptor.attributes[1].format = MTLVertexFormatFloat2;
        vertexDescriptor.attributes[1].offset = sizeof(float) * 2;
        vertexDescriptor.attributes[1].bufferIndex = 0;
        // Layout for buffer 0 (interleaved position + texCoord)
        vertexDescriptor.layouts[0].stride = sizeof(float) * 4;
        vertexDescriptor.layouts[0].stepRate = 1;
        vertexDescriptor.layouts[0].stepFunction = MTLVertexStepFunctionPerVertex;
        pipelineDescriptor.vertexDescriptor = vertexDescriptor;

        // Enable blending for alpha
        pipelineDescriptor.colorAttachments[0].blendingEnabled = YES;
        pipelineDescriptor.colorAttachments[0].rgbBlendOperation = MTLBlendOperationAdd;
        pipelineDescriptor.colorAttachments[0].alphaBlendOperation = MTLBlendOperationAdd;
        pipelineDescriptor.colorAttachments[0].sourceRGBBlendFactor = MTLBlendFactorOne;
        pipelineDescriptor.colorAttachments[0].sourceAlphaBlendFactor = MTLBlendFactorOne;
        pipelineDescriptor.colorAttachments[0].destinationRGBBlendFactor = MTLBlendFactorOneMinusSourceAlpha;
        pipelineDescriptor.colorAttachments[0].destinationAlphaBlendFactor = MTLBlendFactorOneMinusSourceAlpha;

        NSError *error = nil;
        pipelineState = [device newRenderPipelineStateWithDescriptor:pipelineDescriptor error:&error];
        if (error) {
            NSLog(@"Error creating DrawString pipeline state: %@", error);
        }
#ifndef CN1_USE_ARC
        [pipelineDescriptor release];
#endif
    });

    [encoder setRenderPipelineState:pipelineState];

    // Calculate text dimensions
    int w = (int)[str sizeWithAttributes:@{NSFontAttributeName: font}].width;
    int h = (int)ceil([font lineHeight] + 1.0 * scaleValue);
    int p2w = nextPowerOf2(w);
    int p2h = nextPowerOf2(h);

    // Create text texture - same as ES2 version
    CGColorSpaceRef colorSpace = CGColorSpaceCreateDeviceRGB();
    void* imageData = malloc(p2h * p2w * 4);
    CGContextRef context = CGBitmapContextCreate(imageData, p2w, p2h, 8, 4 * p2w, colorSpace, kCGImageAlphaPremultipliedLast);
    // Note: ES2 version has flip commented out, so we don't flip either
    CGColorSpaceRelease(colorSpace);
    CGContextClearRect(context, CGRectMake(0, 0, p2w, p2h));

    UIGraphicsPushContext(context);
    UIColor *textColor = UIColorFromRGB(color, 255);
    [str drawAtPoint:CGPointZero withAttributes:@{
        NSFontAttributeName: font,
        NSForegroundColorAttributeName: textColor
    }];
    UIGraphicsPopContext();

    // Create Metal texture
    MTLTextureDescriptor *textureDescriptor = [MTLTextureDescriptor texture2DDescriptorWithPixelFormat:MTLPixelFormatRGBA8Unorm
                                                                                                  width:p2w
                                                                                                 height:p2h
                                                                                              mipmapped:NO];
    textureDescriptor.usage = MTLTextureUsageShaderRead;

    id<MTLTexture> texture = [[self device] newTextureWithDescriptor:textureDescriptor];
    MTLRegion region = MTLRegionMake2D(0, 0, p2w, p2h);
    [texture replaceRegion:region mipmapLevel:0 withBytes:imageData bytesPerRow:4 * p2w];

    CGContextRelease(context);
    free(imageData);

    // Create vertex data (position + texCoord interleaved)
    typedef struct {
        float position[2];
        float texCoord[2];
    } Vertex;

    // Match ES2 texture coordinates exactly
    GLfloat nY = 1.0;
    GLfloat wX = 0;
    GLfloat sY = 1.0 - (GLfloat)h / (GLfloat)p2h;
    GLfloat eX = (GLfloat)w / (GLfloat)p2w;

    Vertex vertices[] = {
        {{(float)x,     (float)y},     {wX, nY}}, // Top-left
        {{(float)(x+w), (float)y},     {eX, nY}}, // Top-right
        {{(float)x,     (float)(y+h)}, {wX, sY}}, // Bottom-left
        {{(float)(x+w), (float)(y+h)}, {eX, sY}}  // Bottom-right
    };

    // Set vertex buffer
    [encoder setVertexBytes:vertices length:sizeof(vertices) atIndex:0];

    // Set uniforms (MVP matrix + color)
    typedef struct {
        simd_float4x4 mvpMatrix;
        simd_float4 color;
    } Uniforms;

    Uniforms uniforms;
    uniforms.mvpMatrix = [self getMVPMatrix];

    // The text color is already in the texture, we just need alpha modulation
    // Use white (1,1,1) to preserve texture color, with alpha for transparency
    float alphaFloat = ((float)alpha) / 255.0f;
    uniforms.color = simd_make_float4(1.0f, 1.0f, 1.0f, alphaFloat);

    [encoder setVertexBytes:&uniforms length:sizeof(uniforms) atIndex:1];

    // Set texture
    [encoder setFragmentTexture:texture atIndex:0];

    // Create sampler state
    static id<MTLSamplerState> samplerState = nil;
    static dispatch_once_t samplerOnce;
    dispatch_once(&samplerOnce, ^{
        MTLSamplerDescriptor *samplerDescriptor = [[MTLSamplerDescriptor alloc] init];
        samplerDescriptor.minFilter = MTLSamplerMinMagFilterLinear;
        samplerDescriptor.magFilter = MTLSamplerMinMagFilterLinear;
        samplerDescriptor.sAddressMode = MTLSamplerAddressModeClampToEdge;
        samplerDescriptor.tAddressMode = MTLSamplerAddressModeClampToEdge;
        samplerState = [[self device] newSamplerStateWithDescriptor:samplerDescriptor];
#ifndef CN1_USE_ARC
        [samplerDescriptor release];
#endif
    });

    [encoder setFragmentSamplerState:samplerState atIndex:0];

    // Draw rectangle as triangle strip
    [encoder drawPrimitives:MTLPrimitiveTypeTriangleStrip vertexStart:0 vertexCount:4];
}

#elif USE_ES2
-(void)execute {
    glUseProgram(getOGLProgram());
    GLuint textureName = 0;
    DrawStringTextureCache *cachedTex = [DrawStringTextureCache checkCache:str f:font c:color a:255];
    int w = -1;
    if (cachedTex != nil) {
        textureName = [cachedTex textureName];
        w = [cachedTex stringWidth];
    } else {
        w = (int)[str sizeWithFont:font].width;
    }
    
    // Add one point to the height to prevent cutting off bottom in some fonts
    // E.g. Caecilia Bold_8986
    int h = (int)ceil([font lineHeight]+1.0*scaleValue);
    int p2w = nextPowerOf2(w);
    int p2h = nextPowerOf2(h);
    glEnableVertexAttribArray(vertexCoordAtt);
    GLErrorLog;
    //_glEnableClientState(GL_VERTEX_ARRAY);
    //glEnableClientState(GL_NORMAL_ARRAY);
    //GLErrorLog;
    //_glEnableClientState(GL_TEXTURE_COORD_ARRAY);
    //GLErrorLog;
    glEnableVertexAttribArray(textureCoordAtt);
    GLErrorLog;
    //_glEnable(GL_TEXTURE_2D);
    glActiveTexture(GL_TEXTURE0);
    GLErrorLog;
    BOOL textureBound = NO;
    if(textureName == 0) {
        glGenTextures(1, &textureName);
        GLErrorLog;
        glBindTexture(GL_TEXTURE_2D, textureName);
        textureBound = YES;
        GLErrorLog;
        CGColorSpaceRef colorSpace = CGColorSpaceCreateDeviceRGB();
        void* imageData = malloc(p2h * p2w * 4);
        CGContextRef context = CGBitmapContextCreate(imageData, p2w, p2h, 8, 4 * p2w, colorSpace, kCGImageAlphaPremultipliedLast);
        //CGContextTranslateCTM(context, 0, p2h);
        //CGContextScaleCTM(context, 1, -1);
        CGColorSpaceRelease(colorSpace);
        CGContextClearRect(context, CGRectMake(0, 0, p2w, p2h));
        
        UIGraphicsPushContext(context);
        [UIColorFromRGB(color, 255) set];
        [str drawAtPoint:CGPointZero withFont:font];
        UIGraphicsPopContext();
        
        glTexImage2D(GL_TEXTURE_2D, 0, GL_RGBA, p2w, p2h, 0, GL_RGBA, GL_UNSIGNED_BYTE, imageData);
        GLErrorLog;
        CGContextRelease(context);
        GLErrorLog;
        free(imageData);
        [DrawStringTextureCache cache:str f:font t:textureName c:color a:255];
    }
    //_glColor4f(((float)alpha) / 255.0f, ((float)alpha) / 255.0f, ((float)alpha) / 255.0f, ((float)alpha) / 255.0f);
    GLKVector4 color = GLKVector4Make(((float)alpha) / 255.0f, ((float)alpha) / 255.0f, ((float)alpha) / 255.0f, ((float)alpha) / 255.0f);
    if (!textureBound) {
        glBindTexture(GL_TEXTURE_2D, textureName);
        GLErrorLog;
    }
    
    
    glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_LINEAR);
    glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_LINEAR);
    GLErrorLog;
    
    GLfloat vertexes[] = {
        x, y,
        x + w, y,
        x, y + h,
        x + w, y + h
    };
    
    GLfloat nY = 1.0;
    GLfloat wX = 0;
    GLfloat sY = 1.0 - (GLfloat)h / (GLfloat)p2h;
    GLfloat eX = (GLfloat)w/(GLfloat)p2w;
    
    GLfloat textureCoordinates[] = {
        wX, nY,
        eX, nY,
        wX, sY,
        eX, sY
    };
    
    //_glTexCoordPointer(2, GL_SHORT, 0, textureCoordinates);
    //GLErrorLog;
    glVertexAttribPointer(textureCoordAtt, 2, GL_FLOAT, 0, 0, textureCoordinates);
    GLErrorLog;
    
    //_glVertexPointer(2, GL_FLOAT, 0, vertexes);
    //GLErrorLog;
    glVertexAttribPointer(vertexCoordAtt, 2, GL_FLOAT, GL_FALSE, 0, vertexes);
    GLErrorLog;
    
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
    
    glUniform1i(textureUniform, 0);
    GLErrorLog;
    glUniform4fv(colorUniform, 1, color.v);
    GLErrorLog;
    
    glDrawArrays(GL_TRIANGLE_STRIP, 0, 4);
    GLErrorLog;
    
    glBindBuffer(GL_ARRAY_BUFFER, 0);
    glDisableVertexAttribArray(textureCoordAtt);
    GLErrorLog;
    
    glDisableVertexAttribArray(vertexCoordAtt);
    GLErrorLog;
    
    glBindTexture(GL_TEXTURE_2D, 0);
    GLErrorLog;
    
    //glUseProgram(CN1activeProgram);
    //GLErrorLog;
}

#else
-(void)execute {
    DrawStringTextureCache* cache = [DrawStringTextureCache checkCache:str f:font c:color a:255];
    GLuint textureName = [cache textureName];
    int w = (int)[str sizeWithFont:font].width;
    int h = (int)[font lineHeight];
    int p2w = nextPowerOf2(w);
    int p2h = nextPowerOf2(h);
    _glEnableClientState(GL_VERTEX_ARRAY);
    //glEnableClientState(GL_NORMAL_ARRAY);
    GLErrorLog;
    _glEnableClientState(GL_TEXTURE_COORD_ARRAY);
    GLErrorLog;
    _glEnable(GL_TEXTURE_2D);
    glActiveTexture(GL_TEXTURE0);
    GLErrorLog;
    if(textureName == 0) {
        glGenTextures(1, &textureName);
        GLErrorLog;
        glBindTexture(GL_TEXTURE_2D, textureName);
        GLErrorLog;
        CGColorSpaceRef colorSpace = CGColorSpaceCreateDeviceRGB();
        void* imageData = malloc(p2h * p2w * 4);
        CGContextRef context = CGBitmapContextCreate(imageData, p2w, p2h, 8, 4 * p2w, colorSpace, kCGImageAlphaPremultipliedLast);
        //CGContextTranslateCTM(context, 0, p2h);
        //CGContextScaleCTM(context, 1, -1);
        CGColorSpaceRelease(colorSpace);
        CGContextClearRect(context, CGRectMake(0, 0, p2w, p2h));
        
        UIGraphicsPushContext(context);
        [UIColorFromRGB(color, 255) set];
        [str drawAtPoint:CGPointZero withFont:font];
        UIGraphicsPopContext();
        
        glTexImage2D(GL_TEXTURE_2D, 0, GL_RGBA, p2w, p2h, 0, GL_RGBA, GL_UNSIGNED_BYTE, imageData);
        GLErrorLog;
        CGContextRelease(context);
        GLErrorLog;
        free(imageData);
        [DrawStringTextureCache cache:str f:font t:textureName c:color a:255];
    }    
    _glColor4f(((float)alpha) / 255.0f, ((float)alpha) / 255.0f, ((float)alpha) / 255.0f, ((float)alpha) / 255.0f);
    glBindTexture(GL_TEXTURE_2D, textureName);
    GLErrorLog;
    glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_LINEAR);
    glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_LINEAR);
    GLErrorLog;
    
        GLfloat vertexes[] = {
            x, y,
            x + p2w, y,
            x, y + p2h,
            x + p2w, y + p2h
        };
        
        static const GLshort textureCoordinates[] = {
            0, 1,
            1, 1,
            0, 0,
            1, 0,
        };
        
        _glTexCoordPointer(2, GL_SHORT, 0, textureCoordinates);
        GLErrorLog;
        _glVertexPointer(2, GL_FLOAT, 0, vertexes);
        GLErrorLog;
        _glDrawArrays(GL_TRIANGLE_STRIP, 0, 4);
        GLErrorLog;

    _glDisableClientState(GL_VERTEX_ARRAY);
    GLErrorLog;
    _glDisableClientState(GL_TEXTURE_COORD_ARRAY);
    GLErrorLog;
    glBindTexture(GL_TEXTURE_2D, 0);
    GLErrorLog;
    //glDisableClientState(GL_NORMAL_ARRAY);
    GLErrorLog;
    _glDisable(GL_TEXTURE_2D);
    GLErrorLog;
}

#endif

#ifndef CN1_USE_ARC
-(void)dealloc {
    [str release];
    [font release];
    [super dealloc];
}
#endif

-(NSString*)getName {
    return @"DrawString";
}

@end
