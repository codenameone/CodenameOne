#import "DrawImage.h"
#import "CodenameOne_GLViewController.h"
#include "xmlvm.h"

#ifdef USE_ES2
extern GLKMatrix4 CN1modelViewMatrix;
extern GLKMatrix4 CN1projectionMatrix;
extern GLKMatrix4 CN1transformMatrix;
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


@implementation DrawImage
-(id)initWithArgs:(int)a xpos:(int)xpos ypos:(int)ypos i:(GLUIImage*)i w:(int)w h:(int)h {
    alpha = a;
    x = xpos;
    y = ypos;
    width = w;
    height = h;
    img = i;
#ifndef CN1_USE_ARC
    [img retain];
#endif
    return self;
}
#ifdef USE_ES2
-(void)execute {
    glUseProgram(getOGLProgram());
    GLKVector4 color = GLKVector4Make(((float)alpha) / 255.0f, ((float)alpha) / 255.0f, ((float)alpha) / 255.0f, ((float)alpha) / 255.0f);
    
    //_glEnable(GL_TEXTURE_2D);
    
    float w = width;
    float h = height;
    float actualImageWidth = [img getImage].size.width;
    float actualImageHeight = [img getImage].size.height;
    float actualImageWidthP2 = nextPowerOf2((int)actualImageWidth);
    float actualImageHeightP2 = nextPowerOf2((int)actualImageHeight);
    GLuint tex = [img getTexture:(int)actualImageWidth texHeight:(int)actualImageHeight];
    glActiveTexture(GL_TEXTURE0);
    GLErrorLog;
    glBindTexture(GL_TEXTURE_2D, tex);
    GLErrorLog;
    float blankPixelsW = actualImageWidthP2 - actualImageWidth;
    float blankPixelsH = actualImageHeightP2 - actualImageHeight;
    w += ceil(blankPixelsW * w / actualImageWidth);//nextPowerOf2(w);//actualImageWidthP2 - actualImageWidth;
    h += ceil(blankPixelsH * h/ actualImageHeight); //nextPowerOf2(h);//actualImageHeightP2 - actualImageHeight;
    
    // offset from y coord... to deal with differences between simulator and
    // device
    float dy = 0;
#if TARGET_IPHONE_SIMULATOR
    // for some lame reason, the simulator positions things just a tad different
    // than the device.  This is an attempt to play an out-of-tune piano.
    dy=0.5;
#endif
    
    GLfloat vertexes[] = {
        x, y+dy,
        x + w, y+dy,
        x, y + h,
        x + w, y + h
    };
    //NSLog(@"drawImage(%i, %i, %i, %i, %i, %i)", x, y, w, h, width, height);
    
    //static const GLshort textureCoordinates[] = {
    //    0, 1,
    //    1, 1,
    //    0, 0,
    //    1, 0,
    //};
    
    //glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_LINEAR);
    //glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_LINEAR);
    //GLErrorLog;
    
    glEnableVertexAttribArray(textureCoordAtt);
    GLErrorLog;
    
    glEnableVertexAttribArray(vertexCoordAtt);
    GLErrorLog;

    glVertexAttribPointer(textureCoordAtt, 2, GL_SHORT, 0, 0, textureCoordinates);
    GLErrorLog;
    
    glUniformMatrix4fv(projectionMatrixUniform, 1, 0, CN1projectionMatrix.m);
    GLErrorLog;
    glUniformMatrix4fv(modelViewMatrixUniform, 1, 0, CN1modelViewMatrix.m);
    GLErrorLog;
    glUniformMatrix4fv(transformMatrixUniform, 1, 0, CN1transformMatrix.m);
    GLErrorLog;
    
    glUniform1i(textureUniform, 0);
    GLErrorLog;
    glUniform4fv(colorUniform, 1, color.v);
    GLErrorLog;
    
    glVertexAttribPointer(vertexCoordAtt, 2, GL_FLOAT, GL_FALSE, 0, vertexes);
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
    _glColor4f(((float)alpha) / 255.0f, ((float)alpha) / 255.0f, ((float)alpha) / 255.0f, ((float)alpha) / 255.0f);
    glActiveTexture(GL_TEXTURE0);
    _glEnable(GL_TEXTURE_2D);
    GLErrorLog;
    int w = width;
    int h = height;
    float actualImageWidth = [img getImage].size.width;
    float actualImageHeight = [img getImage].size.height;
    int actualImageWidthP2 = nextPowerOf2((int)actualImageWidth);
    int actualImageHeightP2 = nextPowerOf2((int)actualImageHeight);
    GLuint tex = [img getTexture:(int)actualImageWidth texHeight:(int)actualImageHeight];
    glBindTexture(GL_TEXTURE_2D, tex);
    GLErrorLog;
    float blankPixelsW = actualImageWidthP2 - actualImageWidth;
    float blankPixelsH = actualImageHeightP2 - actualImageHeight;
    w += blankPixelsW * (w / actualImageWidth);//nextPowerOf2(w);//actualImageWidthP2 - actualImageWidth;
    h += blankPixelsH * (h / actualImageHeight); //nextPowerOf2(h);//actualImageHeightP2 - actualImageHeight;
    GLfloat vertexes[] = {
        x, y,
        x + w, y,
        x, y + h,
        x + w, y + h
    };
    //NSLog(@"drawImage(%i, %i, %i, %i, %i, %i)", x, y, w, h, width, height);
    
    static const GLshort textureCoordinates[] = {
        0, 1,
        1, 1,
        0, 0,
        1, 0,
    };
    
    //glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_S, GL_CLAMP_TO_EDGE);
    //glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_T, GL_CLAMP_TO_EDGE);
    glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_LINEAR);
    glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_LINEAR);
    GLErrorLog;
    _glEnableClientState(GL_VERTEX_ARRAY);
    GLErrorLog;
    _glEnableClientState(GL_TEXTURE_COORD_ARRAY);
    GLErrorLog;
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
    _glDisable(GL_TEXTURE_2D);
    GLErrorLog;
}
#endif
-(void)dealloc {
#ifndef CN1_USE_ARC
    [img release];
    [super dealloc];
#endif
}

-(NSString*)getName {
    return @"DrawImage";
}

@end
