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
#import "CN1TapGestureRecognizer.h"
#import "CodenameOne_GLViewController.h"


extern void pointerPressedC(int* x, int* y, int length);
extern void pointerDraggedC(int* x, int* y, int length);
extern void pointerReleasedC(int* x, int* y, int length);
extern NSMutableArray* touchesArray;
extern int CN1lastTouchX;
extern int CN1lastTouchY;
extern float scaleValue;
extern BOOL skipNextTouch;
extern UIView *editingComponent;
extern BOOL isVKBAlwaysOpen();
extern BOOL CN1useTapGestureRecognizer;


/**
 * Use a UITapGestureRecognizer for pointer events rather than directly in 
 * the view controller so that CN1 always gets pointer events - even when 
 * the event is grabbed by a native peer component.
 * @return 
 */
@implementation CN1TapGestureRecognizer 

- (void) install:(CodenameOne_GLViewController*)ctrl {
    [self setCancelsTouchesInView:NO];
    [ctrl.view.window addGestureRecognizer:self];
    CN1useTapGestureRecognizer = YES;
    
    
}

/**
 * Some events need to be ignored.  We only want to receive events originating from our view hierarchy
 * that is controlled by the GLViewController.
 */
-(BOOL)ignoreEvent:(UITouch*)touch {
    CodenameOne_GLViewController *ctrl = [CodenameOne_GLViewController instance];
    // touchesForView should return all of the touches in the GLViewController.view and descendents.
    // the "view" member will either be the EAGLView itself, or a container that includes the
    // EAGLView and peer components.
    // We DO want to process touches from peer components
    // We DO NOT want to process touches from popovers like datepickers and openGallery.
    // See the OpenGalleryTest2793 sample to test events for openGallery.
    return (touch == nil || ![touch.view isDescendantOfView:ctrl.view]);
}

- (void) touchesBegan:(NSSet *)touches withEvent:(UIEvent *)event
{   
    [super touchesBegan:touches withEvent:event];
    POOL_BEGIN();
    if(touchesArray == nil) {
        touchesArray = [[NSMutableArray alloc] init];
    }
    UITouch* touch = [touches anyObject];
    NSArray *ts = [touches allObjects];
    [touchesArray addObjectsFromArray:ts];
    int xArray[[touches count]];
    int yArray[[touches count]];
    CodenameOne_GLViewController *ctrl = [CodenameOne_GLViewController instance];

    if ([self ignoreEvent:touch]) {
        // If the main GLView isn't showing, then just
        // skip this.  We were getting pointer events
        // handled here when the gallery was opened:
        // https://github.com/codenameone/CodenameOne/issues/2793
        POOL_END();
        return;
    }
    CGPoint point = [touch locationInView:ctrl.view];
    if([touches count] > 1) {
        for(int iter = 0 ; iter < [ts count] ; iter++) {
            UITouch* currentTouch = [ts objectAtIndex:iter];
            CGPoint currentPoint = [currentTouch locationInView:ctrl.view];
            xArray[iter] = (int)currentPoint.x * scaleValue;
            yArray[iter] = (int)currentPoint.y * scaleValue;
            CN1lastTouchX = (int)currentPoint.x;
            CN1lastTouchY = (int)currentPoint.y;
        }
    } else {
        xArray[0] = (int)point.x * scaleValue;
        yArray[0] = (int)point.y * scaleValue;
        CN1lastTouchX = (int)point.x;
        CN1lastTouchY = (int)point.y;
    }
    pointerPressedC(xArray, yArray, [touches count]);
    POOL_END();
}

- (void) touchesMoved:(NSSet *)touches withEvent:(UIEvent *)event
{
    [super touchesMoved:touches withEvent:event];
    if(skipNextTouch || (editingComponent != nil && !isVKBAlwaysOpen())) {
        return;
    }
    POOL_BEGIN();
    UITouch* touch = [touches anyObject];
    int xArray[[touchesArray count]];
    int yArray[[touchesArray count]];
    CodenameOne_GLViewController *ctrl = [CodenameOne_GLViewController instance];
    if ([self ignoreEvent:touch]) {
        // If the main GLView isn't showing, then just
        // skip this.  We were getting pointer events
        // handled here when the gallery was opened:
        // https://github.com/codenameone/CodenameOne/issues/2793
        POOL_END();
        return;
    }
    CGPoint point = [touch locationInView:ctrl.view];
    if([touchesArray count] > 1) {
        for(int iter = 0 ; iter < [touchesArray count] ; iter++) {
            UITouch* currentTouch = [touchesArray objectAtIndex:iter];
            CGPoint currentPoint = [currentTouch locationInView:ctrl.view];
            xArray[iter] = (int)currentPoint.x * scaleValue;
            yArray[iter] = (int)currentPoint.y * scaleValue;
            //CN1Log(@"Dragging x: %i y: %i id: %i", xArray[iter], yArray[iter], currentTouch);
        }
        pointerDraggedC(xArray, yArray, [touchesArray count]);
    } else {
        xArray[0] = (int)point.x * scaleValue;
        yArray[0] = (int)point.y * scaleValue;
        pointerDraggedC(xArray, yArray, [touches count]);
    }
    POOL_END();
}

- (void) touchesEnded:(NSSet *)touches withEvent:(UIEvent *)event
{
	
    [super touchesEnded:touches withEvent:event];
    if(skipNextTouch) {
        skipNextTouch = NO;
        return;
    }
    POOL_BEGIN();
    NSArray *ts = [touches allObjects];
    [touchesArray removeObjectsInArray:ts];
    if([touchesArray count] > 0) {
        POOL_END();
        return;
    }
    UITouch* touch = [touches anyObject];
    int xArray[[touches count]];
    int yArray[[touches count]];
    CodenameOne_GLViewController *ctrl = [CodenameOne_GLViewController instance];
    if ([self ignoreEvent:touch]) {
        // If the main GLView isn't showing, then just
        // skip this.  We were getting pointer events
        // handled here when the gallery was opened:
        // https://github.com/codenameone/CodenameOne/issues/2793
        POOL_END();
        return;
    }
    CGPoint point = [touch locationInView:ctrl.view];
    //CN1Log(@"Released %i fingers", [touches count]);
    if([touches count] > 1) {
        for(int iter = 0 ; iter < [ts count] ; iter++) {
            UITouch* currentTouch = [ts objectAtIndex:iter];
            CGPoint currentPoint = [currentTouch locationInView:ctrl.view];
            xArray[iter] = (int)currentPoint.x * scaleValue;
            yArray[iter] = (int)currentPoint.y * scaleValue;
        }
    } else {
        xArray[0] = (int)point.x * scaleValue;
        yArray[0] = (int)point.y * scaleValue;
    }
    if(!isVKBAlwaysOpen()) {
        //CGPoint scaledPoint = CGPointMake(point.x * scaleValue, point.y * scaleValue);
        [ctrl foldKeyboard:point];
    }
    pointerReleasedC(xArray, yArray, [touches count]);
    POOL_END();
}

- (void) touchesCancelled:(NSSet *)touches withEvent:(UIEvent *)event
{
    [super touchesCancelled:touches withEvent:event];
    if(skipNextTouch) {
        skipNextTouch = NO;
        return;
    }
    POOL_BEGIN();
    NSArray *ts = [touches allObjects];
    [touchesArray removeObjectsInArray:ts];
    UITouch* touch = [touches anyObject];
    int xArray[[touches count]];
    int yArray[[touches count]];
    CodenameOne_GLViewController *ctrl = [CodenameOne_GLViewController instance];
    if ([self ignoreEvent:touch]) {
        // If the main GLView isn't showing, then just
        // skip this.  We were getting pointer events
        // handled here when the gallery was opened:
        // https://github.com/codenameone/CodenameOne/issues/2793
        POOL_END();
        return;
    }
    CGPoint point = [touch locationInView:ctrl.view];
    if([touches count] > 1) {
        for(int iter = 0 ; iter < [ts count] ; iter++) {
            UITouch* currentTouch = [ts objectAtIndex:iter];
            CGPoint currentPoint = [currentTouch locationInView:ctrl.view];
            xArray[iter] = (int)currentPoint.x * scaleValue;
            yArray[iter] = (int)currentPoint.y * scaleValue;
        }
    } else {
        xArray[0] = (int)point.x * scaleValue;
        yArray[0] = (int)point.y * scaleValue;
    }
    if(!isVKBAlwaysOpen()) {
        [ctrl foldKeyboard:point];
    }
    pointerReleasedC(xArray, yArray, [touches count]);
    POOL_END();
}
- (void) ignoreTouch:(UITouch *)touch forEvent:(UIEvent *)event
{
	//	Overriding this prevents touchesMoved:withEvent:
	//	not being called after moving a certain threshold
}
@end
