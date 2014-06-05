//
//  FillPolygon.m
//  HelloWorldCN1
//
//  Created by Steve Hannah on 2014-06-03.
//
//

#import "FillPolygon.h"
#import <OpenGLES/ES2/gl.h>
#import "CN1ES2compat.h"
#import "xmlvm.h"

@implementation FillPolygon
-(id)initWithArgs:(JAVA_FLOAT*)xCoords y:(JAVA_FLOAT*)yCoords num:(int)num color:(int)theColor alpha:(int)theAlpha
{
    color = theColor;
    alpha = theAlpha;
    size_t size = sizeof(JAVA_FLOAT)*num;
    
    x = malloc(size);
    memcpy(x, xCoords, size);
    y = malloc(size);
    memcpy(y, yCoords, size);
    numPoints = num;
    //NSLog(@"Num points: %d", numPoints);
    return self;
}
-(void)execute
{
    
    
    GlColorFromRGB(color, alpha);
    GLfloat vertexes[numPoints*2];// = malloc(sizeof(GLfloat)*numPoints*2);
    GLErrorLog;
    int j = 0;
    for ( int i=0; i<numPoints; i++){
        //NSLog(@"Point: %f %f", x[i], y[i]);
        vertexes[j++] = (GLfloat)x[i];
        vertexes[j++] = (GLfloat)y[i];
    }
    
    _glVertexPointer(2, GL_FLOAT, 0, vertexes);
    _glEnableClientState(GL_VERTEX_ARRAY);
    GLErrorLog;
    _glDrawArrays(GL_TRIANGLE_FAN, 0, numPoints);
    GLErrorLog;
    _glDisableClientState(GL_VERTEX_ARRAY);
    GLErrorLog;
    //free(vertexes);

}

-(void)dealloc
{
    free(x);
    free(y);
}
@end
