#import "DrawImage.h"
#import "CodenameOne_GLViewController.h"

@implementation DrawImage
-(id)initWithArgs:(int)a xpos:(int)xpos ypos:(int)ypos i:(GLUIImage*)i w:(int)w h:(int)h {
    alpha = a;
    x = xpos;
    y = ypos;
    width = w;
    height = h;
    img = i;
    [img retain];
    return self;
}

-(void)execute {
    glColor4f(((float)alpha) / 255.0f, ((float)alpha) / 255.0f, ((float)alpha) / 255.0f, ((float)alpha) / 255.0f);
    glEnable(GL_TEXTURE_2D);
    GLErrorLog;
    GLuint tex = [img getTexture];
    glBindTexture(GL_TEXTURE_2D, tex);
    GLErrorLog;
    int w = nextPowerOf2(width);
    int h = nextPowerOf2(height);
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
    glEnableClientState(GL_VERTEX_ARRAY);
    GLErrorLog;
    glEnableClientState(GL_TEXTURE_COORD_ARRAY);
    GLErrorLog;
    glTexCoordPointer(2, GL_SHORT, 0, textureCoordinates);
    GLErrorLog;
    glVertexPointer(2, GL_FLOAT, 0, vertexes);
    GLErrorLog;
    glDrawArrays(GL_TRIANGLE_STRIP, 0, 4);
    GLErrorLog;
    glDisableClientState(GL_VERTEX_ARRAY);
    GLErrorLog;
    glDisableClientState(GL_TEXTURE_COORD_ARRAY);
    GLErrorLog;
    glBindTexture(GL_TEXTURE_2D, 0);
    GLErrorLog;
    glDisable(GL_TEXTURE_2D);
    GLErrorLog;
}

-(void)dealloc {
    [img release];
	[super dealloc];
}

-(NSString*)getName {
    return @"DrawImage";
}

@end
