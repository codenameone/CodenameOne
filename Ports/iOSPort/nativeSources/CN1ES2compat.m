#import "CN1ES2compat.h"
#ifdef USE_ES2
#import <GLKit/GLKit.h>
#import <OpenGLES/ES2/gl.h>
#import <OpenGLES/ES1/gl.h>
#import <Math.h>

GLKMatrix4 CN1modelViewMatrix;
GLKMatrix4 CN1projectionMatrix;
GLKMatrix4 CN1transformMatrix;
static BOOL CN1ClientState_GL_COLOR_ARRAY = FALSE;
static BOOL CN1ClientState_GL_EDGE_FLAG_ARRAY = FALSE;
static BOOL CN1ClientState_GL_FOG_COORD_ARRAY = FALSE;
static BOOL CN1ClientState_GL_INDEX_ARRAY = FALSE;
static BOOL CN1ClientState_GL_NORMAL_ARRAY = FALSE;
static BOOL CN1ClientState_GL_SECONDARY_COLOR_ARRAY = FALSE;
static BOOL CN1ClientState_GL_TEXTURE_COORD_ARRAY = FALSE;
static BOOL CN1ClientState_GL_VERTEX_ARRAY = FALSE;
static BOOL CN1ClientState_GL_ALPHA_TEXTURE = FALSE;
static GLenum CN1matrixMode;

GLuint CN1activeProgram = NULL;

// Vertex buffers
static GLuint CN1TextureMaskVertexBuffer = NULL;
static GLuint CN1TextureRGBAVertexBuffer = NULL;
static GLuint CN1VertexBuffer = NULL;
static GLuint CN1VertexColorBuffer = NULL;

// Shader Attributes
static GLuint CN1TextureMaskCoordAtt = NULL;
static GLuint CN1TextureRGBACoordAtt = NULL;
static GLuint CN1VertexCoordAtt = NULL;
static GLuint CN1VertexColorCoordAtt = NULL;

// Shader Uniforms
static GLuint CN1TextureMaskUniform = NULL;
static GLuint CN1TextureRGBAUniform = NULL;
static GLuint CN1ColorUniform = NULL;

static GLuint CN1modelViewMatrixUniform = NULL;
static GLuint CN1projectionMatrixUniform = NULL;
static GLuint CN1transformMatrixUniform = NULL;

// Shader Uniform Flags
static GLuint CN1useVertexColorsUniform = NULL;
static GLuint CN1useAlphaMaskTextureUniform = NULL;
static GLuint CN1useRGBATextureUniform = NULL;
static BOOL CN1ProgramLoaded = NO;

static BOOL GL_TEXTURE_ENABLED = NO;


static GLvoid * CN1vertexPointer;
static GLenum CN1vertexPointerType;
static GLsizei CN1vertexPointerSize;

static GLvoid * CN1colorPointer;
static GLenum CN1colorPointerType;
static GLsizei CN1colorPointerSize;

static GLvoid * CN1textureRGBPointer;
static GLenum CN1textureRGBPointerType;
static GLsizei CN1textureRGBPointerSize;

static GLvoid * CN1textureMaskPointer;
static GLenum CN1textureMaskPointerType;
static GLsizei CN1textureMaskPointerSize;

static GLKVector4 CN1currentColor;

static int _getGLSize(GLenum type){
    int elSize = 1;
    switch ( type ){
        case GL_SHORT:
            elSize = sizeof(GLshort);break;
        case GL_INT:
            elSize = sizeof(GLint);break;
        case GL_FLOAT:
            elSize = sizeof(GLfloat);break;
        default:
            NSLog(@"Don't know size for type %i", type);
    }
    return elSize;
}

static int _printGLPointer(GLenum type, GLint len, GLint size, const GLvoid * pointer){
    return 0;
    if ( type == GL_FLOAT ){
        NSLog(@"It's a float");
        for ( int i=0; i< size*len; i++){
            NSLog(@"%f", ((GLfloat*)pointer)[i]);
        }
    } else if ( type == GL_SHORT ){
        NSLog(@"It's a short");
        for ( int i=0; i< size*len; i++){
            NSLog(@"%d", ((GLshort*)pointer)[i]);
        }
    } else if ( type == GL_INT){
        NSLog(@"It's an int");
        for ( int i=0; i< size*len; i++){
            NSLog(@"%d", ((GLint*)pointer)[i]);
        }
    }
    return 0;
}


static void CN1updateTransformMatrixES2();



GLuint CN1compileShaderProgram(NSString *vertexShaderSrc, NSString *fragmentShaderSrc){
    GLuint program = glCreateProgram();
    GLuint fragmentShader = glCreateShader(GL_FRAGMENT_SHADER);
    GLuint vertexShader = glCreateShader(GL_VERTEX_SHADER);

    int len = [fragmentShaderSrc length];
    //NSLog(@"%d", len);
    const char* fragmentShaderSrcUTF = fragmentShaderSrc.UTF8String;
    glShaderSource(fragmentShader,1, &fragmentShaderSrcUTF, &len);
    GLErrorLog;
    glCompileShader(fragmentShader);
    GLErrorLog;
    GLint compileSuccess;
    glGetShaderiv(fragmentShader, GL_COMPILE_STATUS, &compileSuccess);
    if ( compileSuccess == GL_FALSE ){
        GLchar message[256];
        glGetShaderInfoLog(fragmentShader, sizeof(message), 0, &message[0]);
        NSString *messageString = [NSString stringWithUTF8String:message];
        NSLog(@"%@", messageString);
        
        exit(1);
    }
    
    len = [vertexShaderSrc length];
    const char* vertexShaderSrcUTF = vertexShaderSrc.UTF8String;
    glShaderSource(vertexShader, 1, &vertexShaderSrcUTF, &len);
    GLErrorLog;
    glCompileShader(vertexShader);
    GLErrorLog;
    if ( compileSuccess == GL_FALSE ){
        GLchar message[256];
        glGetShaderInfoLog(vertexShader, sizeof(message), 0, &message[0]);
        NSString *messageString = [NSString stringWithUTF8String:message];
        NSLog(@"%@", messageString);
        
        exit(1);
    }
    
    glAttachShader(program, vertexShader);
    GLErrorLog;
    glAttachShader(program, fragmentShader);
    GLErrorLog;
    glLinkProgram(program);
    GLErrorLog;
    
    GLint linkSuccess;
    glGetProgramiv(program, GL_LINK_STATUS, &linkSuccess);
    GLErrorLog;
    if ( linkSuccess == GL_FALSE ){
        GLchar message[256];
        glGetProgramInfoLog(program, sizeof(message), 0, &message[0]);
        GLErrorLog;
        NSString * messageString = [NSString stringWithUTF8String:message];
        NSLog(@"%@", messageString);
        exit(1);
    }
    
    return program;

}

static void CN1compileBasicProgram(){
    if ( CN1ProgramLoaded ){
        return;
    }
    CN1transformMatrix = GLKMatrix4Identity;
    //CN1transformMatrix = GLKMatrix4MakeIdentity();
    
    NSLog(@"Compiling basic program");
    CN1ProgramLoaded = YES;
    CN1activeProgram = glCreateProgram();
    GLuint fragmentShader = glCreateShader(GL_FRAGMENT_SHADER);
    NSString *fragmentShaderSrc =
    @"precision highp float;\n"

    "uniform highp sampler2D uTextureMask;\n"
    "uniform highp sampler2D uTextureRGBA;\n"
    
    "uniform highp vec4 uColor;\n"
    "uniform bool uUseVertexColors;\n"
    "uniform bool uUseAlphaMaskTexture;\n"
    "uniform bool uUseRGBATexture;\n"
    
    "varying highp vec2 vTextureMaskCoord;\n"
    "varying highp vec2 vTextureRGBACoord;\n"
    "varying highp vec4 vColor;\n"
    
    "void main(){\n"
    "   if ( uUseRGBATexture ){ gl_FragColor = texture2D(uTextureRGBA, vTextureRGBACoord);} else {\n"
        "   vec4 color = uColor;\n"
        "   if ( uUseVertexColors ){ color = vColor;}\n"

        "   if ( uUseAlphaMaskTexture ){\n"
        "       color = vec4(color.rgb, texture2D(uTextureMask, vTextureMaskCoord).a*color.a);\n"
        "       if ( color.a < 0.00001 ){\n"
        "           discard;\n"
        "       }\n"
        "   }"
        "   gl_FragColor = color;\n"
        "}\n"
    "}\n"
    
    
     ;
    //NSLog(fragmentShaderSrc);
    
    int len = [fragmentShaderSrc length];
    //NSLog(@"%d", len);
    const char* fragmentShaderSrcUTF = fragmentShaderSrc.UTF8String;
    glShaderSource(fragmentShader,1, &fragmentShaderSrcUTF, &len);
    GLErrorLog;
    glCompileShader(fragmentShader);
    GLErrorLog;
    GLint compileSuccess;
    glGetShaderiv(fragmentShader, GL_COMPILE_STATUS, &compileSuccess);
    if ( compileSuccess == GL_FALSE ){
        GLchar message[256];
        glGetShaderInfoLog(fragmentShader, sizeof(message), 0, &message[0]);
        NSString *messageString = [NSString stringWithUTF8String:message];
        NSLog(@"%@", messageString);
        
        exit(1);
    }
    
    
    GLuint vertexShader = glCreateShader(GL_VERTEX_SHADER);
    
    NSString *vertexShaderSrc =
    @"attribute vec4 aVertexCoord;\n"
    "attribute vec2 aTextureRGBACoord;\n"
    "attribute vec2 aTextureMaskCoord;\n"
    "attribute vec4 aColor;\n"
    
    "uniform mat4 uModelViewMatrix;\n"
    "uniform mat4 uProjectionMatrix;\n"
    "uniform mat4 uTransformMatrix;\n"
    "uniform bool uUseAlphaMaskTexture;\n"
    "uniform bool uUseRGBATexture;\n"
    "uniform bool uUseVertexColors;\n"

    "varying highp vec2 vTextureMaskCoord;\n"
    "varying highp vec2 vTextureRGBACoord;\n"
    "varying highp vec4 vColor;\n"
    
    "void main(){\n"
    "   gl_Position = uProjectionMatrix *  uModelViewMatrix * uTransformMatrix * aVertexCoord;\n"
    //"   gl_Position = vec4(aVertexCoord.x * 2.0 / 640.0 - 1.0,\n"
   // "   aVertexCoord.y * -2.0 / 960.0 + 1.0,\n"
    //"   aVertexCoord.z,\n"
    //"   1.0);"
    
    "   if ( uUseAlphaMaskTexture ){ vTextureMaskCoord = aTextureMaskCoord;}\n"
    "   if ( uUseRGBATexture ){ vTextureRGBACoord = aTextureRGBACoord;}\n"
    "   if ( uUseVertexColors ){ vColor = aColor;}\n"
    "}";
    //NSLog(vertexShaderSrc);
    len = [vertexShaderSrc length];
    const char* vertexShaderSrcUTF = vertexShaderSrc.UTF8String;
    glShaderSource(vertexShader, 1, &vertexShaderSrcUTF, &len);
    GLErrorLog;
    glCompileShader(vertexShader);
    GLErrorLog;
    if ( compileSuccess == GL_FALSE ){
        GLchar message[256];
        glGetShaderInfoLog(vertexShader, sizeof(message), 0, &message[0]);
        NSString *messageString = [NSString stringWithUTF8String:message];
        NSLog(@"%@", messageString);
        
        exit(1);
    }
    
    glAttachShader(CN1activeProgram, vertexShader);
    GLErrorLog;
    glAttachShader(CN1activeProgram, fragmentShader);
    GLErrorLog;
    glLinkProgram(CN1activeProgram);
    GLErrorLog;
    
    GLint linkSuccess;
    glGetProgramiv(CN1activeProgram, GL_LINK_STATUS, &linkSuccess);
    GLErrorLog;
    if ( linkSuccess == GL_FALSE ){
        GLchar message[256];
        glGetProgramInfoLog(CN1activeProgram, sizeof(message), 0, &message[0]);
        GLErrorLog;
        NSString * messageString = [NSString stringWithUTF8String:message];
        NSLog(@"%@", messageString);
        exit(1);
    }
    
    CN1TextureMaskCoordAtt = glGetAttribLocation(CN1activeProgram, "aTextureMaskCoord");
    GLErrorLog;
    CN1TextureRGBACoordAtt = glGetAttribLocation(CN1activeProgram, "aTextureRGBACoord");
    GLErrorLog;
    CN1VertexCoordAtt = glGetAttribLocation(CN1activeProgram, "aVertexCoord");
    GLErrorLog;
    CN1VertexColorCoordAtt = glGetAttribLocation(CN1activeProgram, "aColor");
    GLErrorLog;
    CN1TextureMaskUniform = glGetUniformLocation(CN1activeProgram, "uTextureMask");
    GLErrorLog;
    CN1TextureRGBAUniform = glGetUniformLocation(CN1activeProgram, "uTextureRGBA");
    GLErrorLog;
    CN1ColorUniform = glGetUniformLocation(CN1activeProgram, "uColor");
    GLErrorLog;
    CN1modelViewMatrixUniform = glGetUniformLocation(CN1activeProgram, "uModelViewMatrix");
    GLErrorLog;
    CN1projectionMatrixUniform = glGetUniformLocation(CN1activeProgram, "uProjectionMatrix");
    GLErrorLog;
    CN1transformMatrixUniform = glGetUniformLocation(CN1activeProgram, "uTransformMatrix");
    GLErrorLog;
    CN1useVertexColorsUniform = glGetUniformLocation(CN1activeProgram, "uUseVertexColors");
    GLErrorLog;
    CN1useAlphaMaskTextureUniform = glGetUniformLocation(CN1activeProgram, "uUseAlphaMaskTexture");
    GLErrorLog;
    CN1useRGBATextureUniform = glGetUniformLocation(CN1activeProgram, "uUseRGBATexture");
    GLErrorLog;
    
    //glGenBuffers(1, &CN1TextureRGBAVertexBuffer);
    //GLErrorLog;
    //glBindBuffer(GL_ARRAY_BUFFER, CN1TextureRGBAVertexBuffer);
    //GLErrorLog;
    //glVertexAttribPointer(CN1TextureRGBACoordAtt, 2, GL_FLOAT, GL_FALSE, sizeof(GLfloat)*2, 0);
    //GLErrorLog;
    //glGenBuffers(1, &CN1VertexBuffer);
    //GLErrorLog;
    //glBindBuffer(GL_ARRAY_BUFFER, CN1VertexBuffer);
    //GLErrorLog;
    //glVertexAttribPointer(CN1VertexCoordAtt, 2, GL_FLOAT, GL_FALSE, sizeof(GLfloat)*2, 0);
    //GLErrorLog;
    //glBufferData(GL_ARRAY_BUFFER, sizeof(texCoords), texCoords, GL_STATIC_DRAW);
    
    //glGenBuffers(1, &CN1VertexColorBuffer);
    //GLErrorLog;
    //glBindBuffer(GL_ARRAY_BUFFER, CN1VertexColorBuffer);
    //GLErrorLog;
    //glVertexAttribPointer(CN1VertexColorCoordAtt, 4, GL_FLOAT, GL_FALSE, sizeof(GLfloat)*4, 0 );
    //GLErrorLog;
    
    glUseProgram(CN1activeProgram);
    
    GLErrorLog;
    
    glUniform1i(CN1useAlphaMaskTextureUniform, 0);
    glUniform1i(CN1useRGBATextureUniform, 0);
    glUniform1i(CN1useVertexColorsUniform, 0);
    CN1updateTransformMatrixES2();
    
    glUniform1i(CN1TextureMaskUniform, 1);
    glUniform1i(CN1TextureRGBAUniform, 0);
    
    //glBindBuffer(GL_ARRAY_BUFFER, CN1TextureRGBAVertexBuffer);
    //glBufferData(GL_ARRAY_BUFFER, _getGLSize(GL_SHORT)*4, CN1textureRGBPointer, GL_DYNAMIC_DRAW);
    
    //glBindBuffer(GL_ARRAY_BUFFER, CN1VertexBuffer);
    //glBufferData(GL_ARRAY_BUFFER, _getGLSize(GL_FLOAT)*4, CN1vertexPointer, GL_DYNAMIC_DRAW);
    glBindBuffer(GL_ARRAY_BUFFER, 0);
}

static void checkProgram(){
    CN1compileBasicProgram();
    GLuint currProgram;
    glGetIntegerv(GL_CURRENT_PROGRAM, &currProgram);
    if ( currProgram != CN1activeProgram ){
        glUseProgram(CN1activeProgram);
    }
    
}

static void CN1updateProjectionMatrixES2(){
    CN1compileBasicProgram();
    glUniformMatrix4fv(CN1projectionMatrixUniform, 1, 0, CN1projectionMatrix.m);
    
    GLErrorLog;
    //NSLog(@"Projection matrix now %@", NSStringFromGLKMatrix4(CN1projectionMatrix));
}

static void CN1updateTransformMatrixES2(){
    CN1compileBasicProgram();
    glUniformMatrix4fv(CN1transformMatrixUniform, 1, 0, CN1transformMatrix.m);
    GLErrorLog;
}

static void CN1updateModelViewMatrixES2(){
    CN1compileBasicProgram();
    glUniformMatrix4fv(CN1modelViewMatrixUniform, 1, 0, CN1modelViewMatrix.m);
    
    GLErrorLog;
    //NSLog(@"Model View matrix now %@", NSStringFromGLKMatrix4(CN1modelViewMatrix));
}

static void CN1updateColorES2(){
    //NSLog(@"Updating color to %f,%f,%f,%f", CN1currentColor.v[0], CN1currentColor.v[1], CN1currentColor.v[2], CN1currentColor.v[3]);
    glUniform4fv(CN1ColorUniform, 1, CN1currentColor.v);
}
extern void glLoadIdentityES2();
void glDrawArraysES2(GLenum mode, GLint first, GLsizei count){
    checkProgram();
    glUniform1i(CN1useVertexColorsUniform, 0);
    CN1updateProjectionMatrixES2();
    CN1updateModelViewMatrixES2();
    CN1updateTransformMatrixES2();
    if ( GL_TEXTURE_ENABLED ){
        glEnableVertexAttribArray(CN1TextureRGBACoordAtt);
        glUniform1i(CN1useRGBATextureUniform, 1);
        glUniform1i(CN1TextureRGBAUniform, 0);
    } else {
        glDisableVertexAttribArray(CN1TextureRGBACoordAtt);
        glUniform1i(CN1useRGBATextureUniform, 0);
    }
    if ( CN1ClientState_GL_COLOR_ARRAY ){
        glEnableVertexAttribArray(CN1VertexColorCoordAtt);
        glUniform1i(CN1useVertexColorsUniform, 1);
    } else {
        glDisableVertexAttribArray(CN1VertexColorCoordAtt);
        glUniform1i(CN1useVertexColorsUniform, 0);
    }
    
    if ( CN1ClientState_GL_TEXTURE_COORD_ARRAY ){
        glEnableVertexAttribArray(CN1TextureRGBACoordAtt);
        glUniform1i(CN1useRGBATextureUniform, 1);
    } else {
        glDisableVertexAttribArray(CN1TextureRGBACoordAtt);
        glUniform1i(CN1useRGBATextureUniform, 0);
    }
    
    if ( CN1ClientState_GL_VERTEX_ARRAY ){
        glEnableVertexAttribArray(CN1VertexCoordAtt);
    } else {
        glDisableVertexAttribArray(CN1VertexCoordAtt);
    }
    
    if ( CN1ClientState_GL_ALPHA_TEXTURE ){
        glEnableVertexAttribArray(CN1TextureMaskCoordAtt);
        GLErrorLog;
        glUniform1i(CN1useAlphaMaskTextureUniform, 1);
        GLErrorLog;
        //glEnable(GL_BLEND);
        GLErrorLog;
        glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA);
        GLErrorLog;
    } else {
        glDisableVertexAttribArray(CN1TextureMaskCoordAtt);
        GLErrorLog;
        glUniform1i(CN1useAlphaMaskTextureUniform, 0);
        GLErrorLog;
        //glDisable(GL_BLEND);
        GLErrorLog;
        //glDisable(GL_LINE_SMOOTH);
    }
    
    CN1updateColorES2();
    glDrawArrays(mode, first, count);
    
}

void glMatrixModeES2(GLenum mode){
    CN1matrixMode = mode;
}


void glLoadIdentityES2(){
    //NSLog(@"Loading identity");
    if ( CN1matrixMode == GL_PROJECTION ){
        CN1projectionMatrix =GLKMatrix4Identity;
        //CN1updateProjectionMatrixES2();
    } else if ( CN1matrixMode == GL_MODELVIEW ){
        CN1modelViewMatrix = GLKMatrix4Identity;//GLKMatrix4Make(1, 0, 0, 0, 0, 1, 0, 0, 0, 0, 1, 0, 0, 0, 0, 1);
        //CN1updateModelViewMatrixES2();
    } else {
         NSLog(@"Setting orthof on undefined matrix mode %d", CN1matrixMode);
    }
    
    
}

void glOrthofES2(GLfloat left, GLfloat right, GLfloat bottom, GLfloat top, GLfloat nearZ, GLfloat farZ){
    //NSLog(@"Setting orthographic projection %f %f %f %f %f %f", left, right, bottom, top, nearZ, farZ);
    GLKMatrix4 ortho = GLKMatrix4MakeOrtho(left, right, bottom, top, nearZ, farZ);
    if (CN1matrixMode == GL_PROJECTION ){
        CN1projectionMatrix = GLKMatrix4Multiply(CN1projectionMatrix, ortho);
        //CN1updateProjectionMatrixES2();
    } else if ( CN1matrixMode == GL_MODELVIEW ){
        CN1modelViewMatrix = GLKMatrix4Multiply(CN1modelViewMatrix, ortho);
        //CN1updateModelViewMatrixES2();
    } else {
        NSLog(@"Setting orthof on undefined matrix mode %d", CN1matrixMode);
    }
}

void glDisableES2(GLenum feature){
    switch (feature ){
        case GL_ALPHA_TEST:
            return;
        case GL_TEXTURE_2D:
            GL_TEXTURE_ENABLED=NO;
            //glDisable(GL_TEXTURE);
            //glDisableVertexAttribArray(CN1TextureRGBACoordAtt);
            //glUniform1i(CN1useRGBATextureUniform, 0);
            return;
    }
    
    
    glDisable(feature);
}

void glEnableES2(GLenum feature){
    switch (feature ){
        case GL_ALPHA_TEST:
            return;
        case GL_TEXTURE_2D:
            //glBindTexture(GL_TEXTURE0, CN1Te)
            //glEnable(GL_TEXTURE);
            //
            GL_TEXTURE_ENABLED = YES;
            //glEnableVertexAttribArray(CN1TextureRGBACoordAtt);
            //glUniform1i(CN1useRGBATextureUniform, 1);
            //glUniform1i(CN1TextureRGBAUniform, 0);
            
            return;
    }
    
    glEnable(feature);
}



void glScalefES2(GLfloat xScale, GLfloat yScale, GLfloat zScale){
    //NSLog(@"Scaling %f %f %f", xScale, yScale, zScale);
    GLKMatrix4 scale = GLKMatrix4MakeScale(xScale, yScale, zScale);
    if ( CN1matrixMode == GL_PROJECTION ){
        CN1projectionMatrix = GLKMatrix4Multiply(CN1projectionMatrix, scale);
        //CN1updateProjectionMatrixES2();
    } else {
        CN1modelViewMatrix = GLKMatrix4Multiply(CN1modelViewMatrix, scale);
        //CN1updateModelViewMatrixES2();
        
    }
    
}
void glTranslatefES2(GLfloat x, GLfloat y, GLfloat z){
    //NSLog(@"Translating %f %f %f", x, y, z);
    GLKMatrix4 translate = GLKMatrix4MakeTranslation(x, y, z);
    //NSLog(@"Translation matrix is %@", NSStringFromGLKMatrix4(translate));
    if ( CN1matrixMode == GL_PROJECTION ){
        CN1projectionMatrix = GLKMatrix4Multiply(CN1projectionMatrix, translate);
        //CN1updateProjectionMatrixES2();
    } else {
        CN1modelViewMatrix = GLKMatrix4Multiply(CN1modelViewMatrix, translate);
        //CN1updateModelViewMatrixES2();
    }
}

void glRotatefES2(GLfloat angle, GLfloat x, GLfloat y, GLfloat z){
    //NSLog(@"Rotating %f %f %f %f", angle, x, y, z);
    
    // Convert from degrees to radians
    angle = angle * M_PI / 180.0;
    GLKMatrix4 rotate = GLKMatrix4MakeRotation(angle, x, y, z);
    if ( CN1matrixMode == GL_PROJECTION ){
        CN1projectionMatrix = GLKMatrix4Multiply(CN1projectionMatrix, rotate);
        //CN1updateProjectionMatrixES2();
    } else {
        CN1modelViewMatrix = GLKMatrix4Multiply(CN1modelViewMatrix, rotate);
        //CN1updateModelViewMatrixES2();
    }
}

void glSetTransformES2(GLKMatrix4 t){
    CN1transformMatrix = t;
    //CN1updateTransformMatrixES2();
}

GLKMatrix4 glGetTransformES2(){
    return CN1transformMatrix;
}

void glColor4fES2(GLfloat r, GLfloat g, GLfloat b, GLfloat a){
    CN1currentColor = GLKVector4Make(r,g,b,a);
    //CN1updateColorES2();
    
}

void glEnableClientStateES2(GLenum state){
    switch (state){
        case GL_COLOR_ARRAY:
            CN1ClientState_GL_COLOR_ARRAY = TRUE;
            //glEnableVertexAttribArray(CN1VertexColorCoordAtt);
            //glUniform1i(CN1useVertexColorsUniform, 1);
            break;
        case GL_NORMAL_ARRAY:
            CN1ClientState_GL_NORMAL_ARRAY = TRUE;break;
        case GL_TEXTURE_COORD_ARRAY:
            CN1ClientState_GL_TEXTURE_COORD_ARRAY = TRUE;

            //glEnableVertexAttribArray(CN1TextureRGBACoordAtt);
            //glUniform1i(CN1useRGBATextureUniform, 1);
            //GLErrorLog;
            
            break;
        case GL_VERTEX_ARRAY:
            CN1ClientState_GL_VERTEX_ARRAY = TRUE;
            //glEnableVertexAttribArray(CN1VertexCoordAtt);
            //GLErrorLog;
            break;
    }
}

void glDisableClientStateES2(GLenum state){
    switch (state){
        case GL_COLOR_ARRAY:
            CN1ClientState_GL_COLOR_ARRAY = FALSE;
            //glDisableVertexAttribArray(CN1VertexColorCoordAtt);
            //glUniform1i(CN1useVertexColorsUniform, 0);
            break;
        case GL_NORMAL_ARRAY:
            CN1ClientState_GL_NORMAL_ARRAY = FALSE;break;
        case GL_TEXTURE_COORD_ARRAY:
            CN1ClientState_GL_TEXTURE_COORD_ARRAY = FALSE;
            //glDisableVertexAttribArray(CN1TextureRGBACoordAtt);
            //glUniform1i(CN1useRGBATextureUniform, 0);
            break;
        case GL_VERTEX_ARRAY:
            //CN1ClientState_GL_VERTEX_ARRAY = FALSE;
            //glDisableVertexAttribArray(CN1VertexCoordAtt);
            break;
    }
}




void glTexCoordPointerES2(	GLint size,
                       GLenum type,
                       GLsizei stride,
                          const GLvoid * pointer){
    checkProgram();
    if ( !CN1TextureRGBAVertexBuffer ){
        glGenBuffers(1, &CN1TextureRGBAVertexBuffer);
    }
    //glBindBuffer(GL_ARRAY_BUFFER, CN1TextureRGBAVertexBuffer);
    int elSize = _getGLSize(type);
    glVertexAttribPointer(CN1TextureRGBACoordAtt, size, type, 0, stride, pointer);
    //glBufferData(GL_ARRAY_BUFFER, _getGLSize(type), pointer, GL_DYNAMIC_DRAW);
    //glBindBuffer(GL_ARRAY_BUFFER, 0);
    //NSLog(@"Texture pointer:");
    //_printGLPointer(type, size, pointer);
    CN1textureRGBPointer = pointer;
    CN1textureRGBPointerType = type;
    CN1textureRGBPointerSize = size;
}

void glVertexPointerES2(	GLint size,
                     GLenum type,
                     GLsizei stride,
                     const GLvoid * pointer){
    checkProgram();
    if ( !CN1VertexBuffer ){
        glGenBuffers(1, &CN1VertexBuffer);
    }
    //glBindBuffer(GL_ARRAY_BUFFER, CN1VertexBuffer);
    //if ( type == GL_FLOAT ){
    //    GLfloat * fpointer = (GLfloat*)pointer;
    //
    //    GLErrorLog;
    //}
    glVertexAttribPointer(CN1VertexCoordAtt, size, type, 0, stride, pointer);
    //glBufferData(GL_ARRAY_BUFFER, _getGLSize(type), pointer, GL_DYNAMIC_DRAW);
    //glBindBuffer(GL_ARRAY_BUFFER, 0);
    CN1vertexPointer = pointer;
    CN1vertexPointerType = type;
    CN1vertexPointerSize = size;
    //NSLog(@"Vertex pointer:");
    //_printGLPointer(type, size, pointer);
    
    
}

void glAlphaMaskTexCoordPointerES2( GLint size, GLenum type, GLsizei stride, const GLvoid *pointer){
    checkProgram();
    if ( !CN1TextureMaskVertexBuffer ){
        glGenBuffers(1, &CN1TextureMaskVertexBuffer);
    }
    //glBindBuffer(GL_ARRAY_BUFFER, CN1TextureRGBAVertexBuffer);
    int elSize = _getGLSize(type);
    glVertexAttribPointer(CN1TextureMaskCoordAtt, size, type, 0, stride, pointer);
    //glBufferData(GL_ARRAY_BUFFER, _getGLSize(type), pointer, GL_DYNAMIC_DRAW);
    //glBindBuffer(GL_ARRAY_BUFFER, 0);
    //NSLog(@"Texture pointer:");
    //_printGLPointer(type, size, pointer);
    CN1textureMaskPointer = pointer;
    CN1textureMaskPointerType = type;
    CN1textureMaskPointerSize = size;
}

void glEnableCN1StateES2(enum CN1GLenum state){
    switch ( state ){
        case CN1_GL_ALPHA_TEXTURE:
            CN1ClientState_GL_ALPHA_TEXTURE = YES;
            //glEnableVertexAttribArray(CN1TextureMaskCoordAtt);
            //GLErrorLog;
            //glUniform1i(CN1useAlphaMaskTextureUniform, 1);
            //GLErrorLog;
            
            //GLErrorLog;
            //glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA);
            //GLErrorLog;
            
            
            
            break;
            
        case CN1_GL_VERTEX_COLORS:
            break;
    }
}

void glDisableCN1StateES2(enum CN1GLenum state){
    switch ( state ){
        case CN1_GL_ALPHA_TEXTURE:
            CN1ClientState_GL_ALPHA_TEXTURE = NO;
            //glDisableVertexAttribArray(CN1TextureMaskCoordAtt);
            //GLErrorLog;
            //glUniform1i(CN1useAlphaMaskTextureUniform, 0);
            //GLErrorLog;
            
            //GLErrorLog;
            //glDisable(GL_LINE_SMOOTH);
            break;
        
            
        case CN1_GL_VERTEX_COLORS:
        //    glUniform1i(CN1useVertexColorsUniform, 0);
            
            break;
    }
}





#endif
