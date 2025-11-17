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
#import "DrawRect.h"
#import "CodenameOne_GLViewController.h"
#include "xmlvm.h"

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
static GLuint colorUniform;
static GLuint vertexCoordAtt;
static GLuint textureCoordAtt;
static int currentCN1modelViewMatrixVersion=-1;
static int currentCN1projectionMatrixVersion=-1;
static int currentCN1transformMatrixVersion=-1;


static NSString *fragmentShaderSrc =
@"precision highp float;\n"
"uniform lowp vec4 uColor;\n"

"void main(){\n"
"   gl_FragColor = uColor; \n"
"}\n";

static NSString *vertexShaderSrc =
@"attribute vec4 aVertexCoord;\n"

"uniform mat4 uModelViewMatrix;\n"
"uniform mat4 uProjectionMatrix;\n"
"uniform mat4 uTransformMatrix;\n"

"void main(){\n"
"   gl_Position = uProjectionMatrix *  uModelViewMatrix * uTransformMatrix * aVertexCoord;\n"
"}";

static GLuint getOGLProgram(){
    if ( program == 0  ){
        program = CN1compileShaderProgram(vertexShaderSrc, fragmentShaderSrc);
        GLErrorLog;
        vertexCoordAtt = glGetAttribLocation(program, "aVertexCoord");
        GLErrorLog;
        
        modelViewMatrixUniform = glGetUniformLocation(program, "uModelViewMatrix");
        GLErrorLog;
        projectionMatrixUniform = glGetUniformLocation(program, "uProjectionMatrix");
        GLErrorLog;
        transformMatrixUniform = glGetUniformLocation(program, "uTransformMatrix");
        GLErrorLog;

        colorUniform = glGetUniformLocation(program, "uColor");
        GLErrorLog;
        
        
        glBindBuffer(GL_ARRAY_BUFFER, 0);
        GLErrorLog;
        
        
    }
    return program;
}

#endif

@implementation DrawRect
-(id)initWithArgs:(int)c a:(int)a xpos:(int)xpos ypos:(int)ypos w:(int)w h:(int)h {
    color = c;
    alpha = a;
    x = xpos;
    y = ypos;
    width = w;
    height = h;
    return self;
}

#ifdef CN1_USE_METAL
-(void)execute {
    // Metal rendering path - draws rectangle outline
    id<MTLRenderCommandEncoder> encoder = [self makeRenderCommandEncoder];
    if (!encoder) {
        NSLog(@"DrawRect: No encoder available!");
        return;
    }

    // Get or create pipeline state (same as FillRect - uses solid color shader)
    static id<MTLRenderPipelineState> pipelineState = nil;
    static dispatch_once_t onceToken;
    dispatch_once(&onceToken, ^{
        id<MTLDevice> device = [self device];
        id<MTLLibrary> library = [device newDefaultLibrary];

        MTLRenderPipelineDescriptor *pipelineDescriptor = [[MTLRenderPipelineDescriptor alloc] init];
        pipelineDescriptor.vertexFunction = [library newFunctionWithName:@"solidColor_vertex"];
        pipelineDescriptor.fragmentFunction = [library newFunctionWithName:@"solidColor_fragment"];
        pipelineDescriptor.colorAttachments[0].pixelFormat = MTLPixelFormatBGRA8Unorm;

        // Configure vertex descriptor
        MTLVertexDescriptor *vertexDescriptor = [[MTLVertexDescriptor alloc] init];
        vertexDescriptor.attributes[0].format = MTLVertexFormatFloat2;
        vertexDescriptor.attributes[0].offset = 0;
        vertexDescriptor.attributes[0].bufferIndex = 0;
        vertexDescriptor.layouts[0].stride = sizeof(float) * 2;
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
            NSLog(@"Error creating DrawRect pipeline state: %@", error);
        }
#ifndef CN1_USE_ARC
        [pipelineDescriptor release];
#endif
    });

    [encoder setRenderPipelineState:pipelineState];

    // Create vertex data - 4 corners with 0.5 pixel offset for proper line alignment
    float vertices[] = {
        (float)x + 0.5f,         (float)y + 0.5f,          // Top-left
        (float)(x + width) + 0.5f, (float)y + 0.5f,        // Top-right
        (float)(x + width) + 0.5f, (float)(y + height) + 0.5f, // Bottom-right
        (float)x + 0.5f,         (float)(y + height) + 0.5f  // Bottom-left
    };

    [encoder setVertexBytes:vertices length:sizeof(vertices) atIndex:0];

    // Set uniforms (MVP matrix + color)
    typedef struct {
        simd_float4x4 mvpMatrix;
        simd_float4 color;
    } Uniforms;

    Uniforms uniforms;
    uniforms.mvpMatrix = [self getMVPMatrix];
    uniforms.color = [self colorToFloat4:color alpha:alpha];

    [encoder setVertexBytes:&uniforms length:sizeof(uniforms) atIndex:1];

    // Draw rectangle outline as line strip (4 lines forming a closed loop)
    // Metal doesn't have LINE_LOOP, so we need to draw lines manually
    // Draw 4 line segments: 0->1, 1->2, 2->3, 3->0
    uint16_t indices[] = {0, 1, 1, 2, 2, 3, 3, 0};
    [encoder drawIndexedPrimitives:MTLPrimitiveTypeLine
                        indexCount:8
                         indexType:MTLIndexTypeUInt16
                       indexBuffer:[encoder.device newBufferWithBytes:indices length:sizeof(indices) options:MTLResourceStorageModeShared]
                 indexBufferOffset:0];
}
#elif USE_ES2
-(void)execute {
    glUseProgram(getOGLProgram());
    
    GLKVector4 colorV = GLKVector4Make(((float)((color >> 16) & 0xff))/255.0, \
                   ((float)((color >> 8) & 0xff))/255.0, ((float)(color & 0xff))/255.0, ((float)alpha)/255.0);
    
    //GLKVector4 colorV = GLKVector4FromRGB(color, alpha);

    GLfloat vertexes[] = {
        x+0.5, y+0.5,
        x + width+0.5, y+0.5,
        x + width+0.5, y + height+0.5,
        x+0.5, y + height+0.5,
    };

   
    //_glEnableClientState(GL_VERTEX_ARRAY);
    glEnableVertexAttribArray(vertexCoordAtt);
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
    glUniform4fv(colorUniform, 1, colorV.v);
    GLErrorLog;
    
    //_glVertexPointer(2, GL_FLOAT, 0, vertexes);
    //GLErrorLog;
    glVertexAttribPointer(vertexCoordAtt, 2, GL_FLOAT, GL_FALSE, 0, vertexes);
    GLErrorLog;
    
    glDrawArrays(GL_LINE_LOOP, 0, 4);
    GLErrorLog;
    
    //_glDisableClientState(GL_VERTEX_ARRAY);
    //GLErrorLog;
    glDisableVertexAttribArray(vertexCoordAtt);
    GLErrorLog;
    
    //glUseProgram(CN1activeProgram);
    //GLErrorLog;
}
#else
-(void)execute {
    GlColorFromRGB(color, alpha);
    GLfloat vertexes[] = {
        x + 1, y + 1,
        x + width, y + 1,
        x + width, y + height,
        x + 1, y + height,
    };
    _glEnableClientState(GL_VERTEX_ARRAY);
    _glVertexPointer(2, GL_FLOAT, 0, vertexes);
    GLErrorLog;
    _glDrawArrays(GL_LINE_LOOP, 0, 4);
    GLErrorLog;
    _glDisableClientState(GL_VERTEX_ARRAY);
    GLErrorLog;
}
#endif

#ifndef CN1_USE_ARC
-(void)dealloc {
	[super dealloc];
}
#endif

-(NSString*)getName {
    return @"DrawRect";
}

@end
