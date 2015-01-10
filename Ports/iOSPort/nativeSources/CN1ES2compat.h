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
#define NO_USE_ES2 1
enum CN1GLenum {
    CN1_GL_ALPHA_TEXTURE,
    CN1_GL_VERTEX_COLORS
};

#ifdef USE_ES2
#import <GLKit/GLKit.h>
#import <OpenGLES/ES2/gl.h>
#import "ExecutableOp.h"



extern void glMatrixModeES2(GLenum);
extern void glOrthofES2(GLfloat,GLfloat,GLfloat,GLfloat,GLfloat,GLfloat);
extern void glDisableES2(GLenum);
extern void glEnableES2(GLenum);
extern void glScalefES2(GLfloat,GLfloat,GLfloat);
extern void glTranslatefES2(GLfloat,GLfloat,GLfloat);
extern void glColor4fES2(GLfloat,GLfloat,GLfloat,GLfloat);
extern void glEnableClientStateES2(GLenum);
extern void glDisableClientStateES2(GLenum);
extern void glTexCoordPointerES2(GLint,GLenum,GLsizei,const GLvoid*);
extern void glVertexPointerES2(	GLint, GLenum , GLsizei, const GLvoid *);
extern void glDrawArraysES2(GLenum, GLint, GLsizei);
extern void glRotatefES2(GLfloat,GLfloat,GLfloat,GLfloat);
extern void glEnableCN1StateES2(enum CN1GLenum);
extern void glDisableCN1StateES2(enum CN1GLenum);
extern void glAlphaMaskTexCoordPointerES2( GLint , GLenum , GLsizei , const GLvoid *);
extern void glSetTransformES2(GLKMatrix4);
extern GLKMatrix4 glGetTransformES2();
extern GLuint CN1compileShaderProgram(NSString *vertexShaderSrc, NSString *fragmentShaderSrc);
extern void glLoadIdentityES2();

#define _glMatrixMode(foo) glMatrixModeES2(foo)
#define _glLoadIdentity()  glLoadIdentityES2()
#define _glOrthof(p1,p2,p3,p4,p5,p6) glOrthofES2(p1,p2,p3,p4,p5,p6)
#define _glDisable(foo) glDisableES2(foo)
#define _glEnable(foo) glEnableES2(foo)
#define _glScalef(xScale,yScale,zScale) glScalefES2(xScale,yScale,zScale)
#define _glTranslatef(xScale,yScale,zScale) glTranslatefES2(xScale,yScale,zScale)
#define _glColor4f(r,g,b,a) glColor4fES2(r,g,b,a)
#define _glEnableClientState(s) glEnableClientStateES2(s)
#define _glDisableClientState(s) glDisableClientStateES2(s)
#define _glTexCoordPointer(size,type,stride,pointer) glTexCoordPointerES2(size,type,stride,pointer)
#define _glVertexPointer(size,type,stride,pointer) glVertexPointerES2(size,type,stride,pointer)
#define _glDrawArrays(mode,first,count) glDrawArraysES2(mode,first,count)
#define _glRotatef(angle,x,y,z) glRotatefES2(angle,x,y,z)
#define _glEnableCN1State(state) glEnableCN1StateES2(state)
#define _glDisableCN1State(state) glDisableCN1StateES2(state)
#define _glAlphaMaskTexCoordPointer(size,type,stride,pointer) glAlphaMaskTexCoordPointerES2(size,type,stride,pointer)
#else
#import <OpenGLES/ES1/gl.h>
extern void glEnableCN1StateES1(enum CN1GLenum);
extern void glDisableCN1StateES1(enum CN1GLenum);
extern void glAlphaMaskTexCoordPointerES1( GLint , GLenum , GLsizei , const GLvoid *);
#define _glMatrixMode(foo) glMatrixMode(foo)
#define _glLoadIdentity()  glLoadIdentity()
#define _glOrthof(p1,p2,p3,p4,p5,p6) glOrthof(p1,p2,p3,p4,p5,p6)
#define _glDisable(foo) glDisable(foo)
#define _glEnable(foo) glEnable(foo)
#define _glScalef(xScale,yScale,zScale) glScalef(xScale,yScale,zScale)
#define _glTranslatef(xScale,yScale,zScale) glTranslatef(xScale,yScale,zScale)
#define _glColor4f(r,g,b,a) glColor4f(r,g,b,a)
#define _glEnableClientState(s) glEnableClientState(s)
#define _glDisableClientState(s) glDisableClientState(s)
#define _glTexCoordPointer(size,type,stride,pointer) glTexCoordPointer(size,type,stride,pointer)
#define _glVertexPointer(size, type, stride, pointer) glVertexPointer(size, type, stride, pointer)
#define _glDrawArrays(mode,first,count) glDrawArrays(mode,first,count)
#define _glRotatef(angle,x,y,z) glRotatef(angle,x,y,z)
#define _glEnableCN1State(state) glEnableCN1StateES1(state)
#define _glDisableCN1State(state) glDisableCN1StateES1(state)
#define _glAlphaMaskTexCoordPointer(size,type,stride,pointer) glAlphaMaskTexCoordPointerES1(size,type,stride,pointer)
#endif
