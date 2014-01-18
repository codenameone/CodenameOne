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
#import <QuartzCore/QuartzCore.h>

#import "EAGLView.h"
#import "ExecutableOp.h"
#import "CodenameOne_GLViewController.h"
#include "com_codename1_impl_ios_IOSImplementation.h"


extern void stringEdit(int finished, int cursorPos, const char* text);
extern UIView *editingComponent;
extern BOOL vkbAlwaysOpen;

@interface EAGLView (PrivateMethods)
- (void)createFramebuffer;
- (void)deleteFramebuffer;
@end

@implementation EAGLView

@synthesize context;

// You must implement this method
+ (Class)layerClass
{
    return [CAEAGLLayer class];
}

extern BOOL isRetina();

//The EAGL view is stored in the nib file. When it's unarchived it's sent -initWithCoder:.
- (id)initWithCoder:(NSCoder*)coder
{
    self = [super initWithCoder:coder];
	if (self) {
        self.clearsContextBeforeDrawing = NO;
        if ([[UIScreen mainScreen] respondsToSelector:@selector(scale)] && isRetina()) {
            if(isRetinaBug()) {
                self.contentScaleFactor = 1.0;
            } else {
                self.contentScaleFactor = 2.0;
            }
        }
        CAEAGLLayer *eaglLayer = (CAEAGLLayer *)self.layer;
        
        eaglLayer.opaque = TRUE;
        eaglLayer.drawableProperties = [NSDictionary dictionaryWithObjectsAndKeys:
                                        [NSNumber numberWithBool:YES], kEAGLDrawablePropertyRetainedBacking,
                                        kEAGLColorFormatRGBA8, kEAGLDrawablePropertyColorFormat,
                                        nil];
    }
    
    return self;
}

- (void)dealloc
{
    [self deleteFramebuffer];
    [context release];
    
    [super dealloc];
}

- (void)setContext:(EAGLContext *)newContext
{
    if (context != newContext) {
        [self deleteFramebuffer];
        
        [context release];
        context = [newContext retain];
        
        [EAGLContext setCurrentContext:nil];
    }
}

- (void)createFramebuffer
{
    if (context && !defaultFramebuffer) {
        [EAGLContext setCurrentContext:context];
        
        GLErrorLog;
        // Create default framebuffer object.
        glGenFramebuffers(1, &defaultFramebuffer);
        GLErrorLog;
        glBindFramebuffer(GL_FRAMEBUFFER, defaultFramebuffer);
        GLErrorLog;
        
        // Create color render buffer and allocate backing store.
        glGenRenderbuffers(1, &colorRenderbuffer);
        GLErrorLog;
        glBindRenderbuffer(GL_RENDERBUFFER, colorRenderbuffer);
        GLErrorLog;
        [context renderbufferStorage:GL_RENDERBUFFER fromDrawable:(CAEAGLLayer *)self.layer];
        GLErrorLog;
        glGetRenderbufferParameteriv(GL_RENDERBUFFER, GL_RENDERBUFFER_WIDTH, &framebufferWidth);
        GLErrorLog;
        glGetRenderbufferParameteriv(GL_RENDERBUFFER, GL_RENDERBUFFER_HEIGHT, &framebufferHeight);
        GLErrorLog;
        
        glFramebufferRenderbuffer(GL_FRAMEBUFFER, GL_COLOR_ATTACHMENT0, GL_RENDERBUFFER, colorRenderbuffer);
        GLErrorLog;
        
        if (glCheckFramebufferStatus(GL_FRAMEBUFFER) != GL_FRAMEBUFFER_COMPLETE)
            NSLog(@"Failed to make complete framebuffer object %x", glCheckFramebufferStatus(GL_FRAMEBUFFER));
        //NSLog(@"Created framebuffer: %i %i", (int)framebufferWidth, (int)framebufferHeight);
        glClearColor(0, 0, 0, 1.0f);
        glClear(GL_COLOR_BUFFER_BIT);
    }
}

- (void)deleteFramebuffer
{
    if(editingComponent != nil) {
        return;
    }
    if (context) {
        [EAGLContext setCurrentContext:context];
        GLErrorLog;
        
        if (defaultFramebuffer) {
            glDeleteFramebuffers(1, &defaultFramebuffer);
            defaultFramebuffer = 0;
        }
        GLErrorLog;
        
        if (colorRenderbuffer) {
            glDeleteRenderbuffers(1, &colorRenderbuffer);
            colorRenderbuffer = 0;
        }
        GLErrorLog;
    }
}


-(void)updateFrameBufferSize:(int)w h:(int)h {
    /*[self deleteFramebuffer];
     framebufferWidth = w;
     framebufferHeight = h;
     //[self.layer setBounds:CGRectMake(0, 0, w, h)];
     //[self setBounds:CGRectMake(0, 0, w, h)];
     NSLog(@"Deleted framebuffer: %i %i", w, h);*/
}

- (void)setFramebuffer
{
    if (context) {
        GLErrorLog;
        [EAGLContext setCurrentContext:context];
        GLErrorLog;
        
        if (!defaultFramebuffer)
            [self createFramebuffer];
        
        glBindFramebuffer(GL_FRAMEBUFFER, defaultFramebuffer);
        GLErrorLog;
        
        glBlendFunc(GL_ONE, GL_ONE_MINUS_SRC_ALPHA);
        GLErrorLog;
        glDisable(GL_DEPTH_TEST);
        GLErrorLog;
        
        glViewport(0, 0, framebufferWidth, framebufferHeight);
        GLErrorLog;
        glMatrixMode(GL_PROJECTION);
        GLErrorLog;
        glLoadIdentity();
        GLErrorLog;
        glOrthof(0, framebufferWidth, 0, framebufferHeight, -1, 1);
        //NSLog(@"glOrtho %i, %i  bounds.size.height: %i", framebufferWidth, framebufferHeight, (int)self.bounds.size.height);
        GLErrorLog;
        glMatrixMode(GL_MODELVIEW);
        GLErrorLog;
        glLoadIdentity();
        GLErrorLog;
        glEnable(GL_BLEND);
        GLErrorLog;
        glDisable(GL_ALPHA_TEST);
        GLErrorLog;
    }
}

/**
 * User clicked Done or Next button above the keyboard
 */
-(void) keyboardDoneClicked {
    if(editingComponent != nil) {
        if([editingComponent isKindOfClass:[UITextView class]]) {
            stringEdit(YES, -2, ((UITextView*)editingComponent).text);
        } else {
            stringEdit(YES, -2, ((UITextField*)editingComponent).text);
        }
        if(vkbAlwaysOpen) {
            com_codename1_impl_ios_IOSImplementation_foldKeyboard__();
        } else {
            [editingComponent resignFirstResponder];
            [editingComponent removeFromSuperview];
            [editingComponent release];
            editingComponent = nil;
        }
        repaintUI();
        
    }
}

/**
 * User clicked Done or Next button above the keyboard
 */
-(void) keyboardNextClicked {
    if(editingComponent != nil) {
        if([editingComponent isKindOfClass:[UITextView class]]) {
            stringEdit(YES, -2, ((UITextView*)editingComponent).text);
        } else {
            stringEdit(YES, -2, ((UITextField*)editingComponent).text);
        }
        if(vkbAlwaysOpen) {
            com_codename1_impl_ios_TextEditUtil_editNextTextArea__();
        } else {
            [editingComponent resignFirstResponder];
            [editingComponent removeFromSuperview];
            [editingComponent release];
            editingComponent = nil;
        }
        repaintUI();
        
    }
}

-(void)textViewDidChange:(UITextView *)textView {
    if(editingComponent.hidden) {
        editingComponent.hidden = NO;
        com_codename1_impl_ios_IOSImplementation_showTextEditorAgain__();
    }
    if([editingComponent isKindOfClass:[UITextView class]]) {
        stringEdit(NO, -1, ((UITextView*)editingComponent).text);
    } else {
        stringEdit(NO, -1, ((UITextField*)editingComponent).text);
    }
}

-(void)textFieldDidChange {
    if(editingComponent.hidden) {
        editingComponent.hidden = NO;
        com_codename1_impl_ios_IOSImplementation_showTextEditorAgain__();
    }
    if([editingComponent isKindOfClass:[UITextView class]]) {
        stringEdit(NO, -1, ((UITextView*)editingComponent).text);
    } else {
        stringEdit(NO, -1, ((UITextField*)editingComponent).text);
    }
}

extern int currentlyEditingMaxLength;
- (BOOL)textField:(UITextField *)textField shouldChangeCharactersInRange:(NSRange)range replacementString:(NSString *)string {
    NSUInteger newLength = (textField.text.length - range.length) + string.length;
    return (newLength <= currentlyEditingMaxLength);
}

-(BOOL)textView:(UITextView *)textView shouldChangeTextInRange:(NSRange)range replacementText:(NSString *)text {
    NSUInteger newLength = (textView.text.length - range.length) + text.length;
    return (newLength <= currentlyEditingMaxLength);
}


- (BOOL)textFieldShouldReturn:(UITextField *)theTextField {
    if(editingComponent != nil) {
        if([editingComponent isKindOfClass:[UITextView class]]) {
            stringEdit(YES, -2, ((UITextView*)editingComponent).text);
        } else {
            stringEdit(YES, -2, ((UITextField*)editingComponent).text);
        }
        //if there is one then goto the edit next textarea
        //com_codename1_impl_ios_TextEditUtil_editNextTextArea__();
        if(vkbAlwaysOpen) {
            com_codename1_impl_ios_TextEditUtil_editNextTextArea__();
        } else {
            [editingComponent resignFirstResponder];
            [editingComponent removeFromSuperview];
            [editingComponent release];
            editingComponent = nil;
        }
        repaintUI();
    }
    return YES;
}

- (BOOL)presentFramebuffer
{
    BOOL success = FALSE;
    
    if (context) {
        [EAGLContext setCurrentContext:context];
        
        glBindRenderbuffer(GL_RENDERBUFFER, colorRenderbuffer);
        GLErrorLog;
        
        success = [context presentRenderbuffer:GL_RENDERBUFFER];
        GLErrorLog;
        
        if(!success) {
            NSLog(@"Present render buffer unsuccessful!");
        }
    }
    
    return success;
}

- (void)layoutSubviews
{
    // The framebuffer will be re-created at the beginning of the next setFramebuffer method call.
    [self deleteFramebuffer];
    [super layoutSubviews];
}

/*-(void)drawRect:(CGRect)rect {
 [[CodenameOne_GLViewController instance] drawFrame:rect];
 }*/


@end
