// ASM: a very small and fast Java bytecode manipulation framework
// Copyright (c) 2000-2011 INRIA, France Telecom
// All rights reserved.
//
// Redistribution and use in source and binary forms, with or without
// modification, are permitted provided that the following conditions
// are met:
// 1. Redistributions of source code must retain the above copyright
//    notice, this list of conditions and the following disclaimer.
// 2. Redistributions in binary form must reproduce the above copyright
//    notice, this list of conditions and the following disclaimer in the
//    documentation and/or other materials provided with the distribution.
// 3. Neither the name of the copyright holders nor the names of its
//    contributors may be used to endorse or promote products derived from
//    this software without specific prior written permission.
//
// THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
// AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
// IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
// ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE
// LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
// CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
// SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
// INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
// CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
// ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF
// THE POSSIBILITY OF SUCH DAMAGE.
//
// Codename One modifications Copyright (c) 2026, Codename One and/or its
// affiliates, distributed under the license above.
//
// This file is a rewrite of ASM's org.objectweb.asm.ClassVisitor for ParparVM: it keeps
// ASM's API and design, reduced to what the translator uses, so the translator
// can read class files without ASM and still be translated by itself (see
// vm/ByteCodeTranslator/src/com/codename1/tools/translator/classfile/README.md).
package com.codename1.tools.translator.classfile;

/**
 * Receives the parts of a class as {@link ClassReader} decodes them, in class file
 * order: {@code visit}, {@code visitSource}, {@code visitOuterClass}, annotations,
 * inner classes, fields, methods, {@code visitEnd}. Every method forwards to the
 * delegate, if there is one, so a subclass overrides only what it reads.
 */
public abstract class ClassVisitor {
    protected ClassVisitor cv;

    protected ClassVisitor() {
    }

    protected ClassVisitor(ClassVisitor cv) {
        this.cv = cv;
    }

    public void visit(int version, int access, String name, String signature, String superName, String[] interfaces) {
        if (cv != null) {
            cv.visit(version, access, name, signature, superName, interfaces);
        }
    }

    public void visitSource(String source, String debug) {
        if (cv != null) {
            cv.visitSource(source, debug);
        }
    }

    public void visitOuterClass(String owner, String name, String desc) {
        if (cv != null) {
            cv.visitOuterClass(owner, name, desc);
        }
    }

    public AnnotationVisitor visitAnnotation(String desc, boolean visible) {
        return cv != null ? cv.visitAnnotation(desc, visible) : null;
    }

    public void visitInnerClass(String name, String outerName, String innerName, int access) {
        if (cv != null) {
            cv.visitInnerClass(name, outerName, innerName, access);
        }
    }

    public FieldVisitor visitField(int access, String name, String desc, String signature, Object value) {
        return cv != null ? cv.visitField(access, name, desc, signature, value) : null;
    }

    public MethodVisitor visitMethod(int access, String name, String desc, String signature, String[] exceptions) {
        return cv != null ? cv.visitMethod(access, name, desc, signature, exceptions) : null;
    }

    public void visitEnd() {
        if (cv != null) {
            cv.visitEnd();
        }
    }
}
