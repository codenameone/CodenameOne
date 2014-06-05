//
//  FillPolygon.h
//  HelloWorldCN1
//
//  Created by Steve Hannah on 2014-06-03.
//
//

#import "ExecutableOp.h"
#import "xmlvm.h"

@interface FillPolygon : ExecutableOp {
    int color;
    int alpha;
    JAVA_FLOAT* x;
    JAVA_FLOAT* y;
    int numPoints;
}

-(id)initWithArgs:(JAVA_FLOAT*)xCoords y:(JAVA_FLOAT*)yCoords num:(int)num color:(int)color alpha:(int)alpha;
-(void)execute;

@end
