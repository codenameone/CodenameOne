/*
 * Copyright (c) 2026, Codename One and/or its affiliates. All rights reserved.
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
package com.codename1.builders;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Handle;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;

import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Which createMedia overload was called, and whether the build can tell.
 *
 * <p>The two do different things.
 * {@code createMedia(String,boolean)} hands a URI to the platform, which
 * may resolve it against the MediaStore and so needs READ_MEDIA_* on API
 * 33 and up. {@code createMedia(InputStream,String)} copies the stream
 * into app-private storage and reads no shared media at all. Keyed on the
 * method name -- the only thing the scanner used to report -- they are
 * indistinguishable, and an app playing a bundled sound through a stream
 * was built asking to read the user's photos and videos (issue
 * #5507).</p>
 */
class MediaPlaybackPermissionScanTest {

    private static final String MEDIA_MANAGER =
            "com/codename1/media/MediaManager";
    private static final String DISPLAY = "com/codename1/ui/Display";
    private static final String MEDIA = "com/codename1/media/Media";

    /** Records the descriptor reported for each call. */
    private static final class Recorder implements Executor.ClassScanner {
        private final List<String> calls = new ArrayList<String>();

        @Override
        public void usesClass(String cls) {
        }

        @Override
        public void usesClassMethod(String cls, String method) {
        }

        @Override
        public void implementsInterface(String cls, String iface) {
        }

        @Override
        public void usesClassMethodWithDescriptor(String cls, String method,
                String descriptor) {
            if (MEDIA_MANAGER.equals(cls) || DISPLAY.equals(cls)) {
                calls.add(method + descriptor);
            }
        }
    }

    /** Executor is abstract; the scan itself needs none of these. */
    private static final class Scanner extends Executor {
        @Override
        public boolean build(File sourceZip, BuildRequest request) {
            return false;
        }

        @Override
        protected String getDeviceIdCode() {
            return "";
        }

        @Override
        protected String generatePeerComponentCreationCode(
                String methodCallString) {
            return "";
        }

        @Override
        protected String convertPeerComponentToNative(String param) {
            return "";
        }
    }

    private static void write(File dir, String name, ClassWriter w)
            throws Exception {
        w.visitEnd();
        File pkg = new File(dir, "app");
        assertTrue(pkg.isDirectory() || pkg.mkdirs());
        OutputStream out = new FileOutputStream(new File(pkg, name + ".class"));
        try {
            out.write(w.toByteArray());
        } finally {
            out.close();
        }
    }

    /** Emits a class calling {@code owner.name} with {@code descriptor}. */
    private static void writeCall(File dir, String name, String owner,
            String method, String descriptor) throws Exception {
        ClassWriter w = new ClassWriter(ClassWriter.COMPUTE_MAXS);
        w.visit(Opcodes.V1_8, Opcodes.ACC_PUBLIC, "app/" + name, null,
                "java/lang/Object", null);
        MethodVisitor m = w.visitMethod(Opcodes.ACC_PUBLIC
                | Opcodes.ACC_STATIC, "run", "()V", null, null);
        m.visitCode();
        for (Type arg : Type.getArgumentTypes(descriptor)) {
            switch (arg.getSort()) {
                case Type.BOOLEAN:
                    m.visitInsn(Opcodes.ICONST_0);
                    break;
                default:
                    m.visitInsn(Opcodes.ACONST_NULL);
                    break;
            }
        }
        m.visitMethodInsn(Opcodes.INVOKESTATIC, owner, method, descriptor,
                false);
        if (Type.getReturnType(descriptor).getSort() != Type.VOID) {
            m.visitInsn(Opcodes.POP);
        }
        m.visitInsn(Opcodes.RETURN);
        m.visitMaxs(0, 0);
        m.visitEnd();
        write(dir, name, w);
    }

    private static List<String> scan(File dir) throws Exception {
        Recorder r = new Recorder();
        new Scanner().scanClassesForPermissions(dir, r);
        return r.calls;
    }

    // ---- the scanner reports the descriptor -------------------------

    @Test
    void theDescriptorOfACallIsReported(@TempDir File dir) throws Exception {
        writeCall(dir, "Stream", MEDIA_MANAGER, "createMedia",
                "(Ljava/io/InputStream;Ljava/lang/String;)L" + MEDIA + ";");
        assertEquals("[createMedia(Ljava/io/InputStream;Ljava/lang/String;)L"
                + MEDIA + ";]", scan(dir).toString(),
                "the name alone cannot select an overload");
    }

    /**
     * A method reference resolves to one overload just as a call does, so
     * {@code MediaManager::createMedia} must arrive with its descriptor
     * rather than as an unknown that has to be assumed permission-worthy.
     */
    @Test
    void aMethodReferenceReportsItsDescriptor(@TempDir File dir)
            throws Exception {
        String desc = "(Ljava/io/InputStream;Ljava/lang/String;)L" + MEDIA + ";";
        ClassWriter w = new ClassWriter(ClassWriter.COMPUTE_MAXS);
        w.visit(Opcodes.V1_8, Opcodes.ACC_PUBLIC, "app/Ref", null,
                "java/lang/Object", null);
        MethodVisitor m = w.visitMethod(Opcodes.ACC_PUBLIC
                | Opcodes.ACC_STATIC, "run", "()V", null, null);
        m.visitCode();
        Handle metafactory = new Handle(Opcodes.H_INVOKESTATIC,
                "java/lang/invoke/LambdaMetafactory", "metafactory",
                "(Ljava/lang/invoke/MethodHandles$Lookup;Ljava/lang/String;"
                        + "Ljava/lang/invoke/MethodType;"
                        + "Ljava/lang/invoke/MethodType;"
                        + "Ljava/lang/invoke/MethodHandle;"
                        + "Ljava/lang/invoke/MethodType;)"
                        + "Ljava/lang/invoke/CallSite;", false);
        Handle target = new Handle(Opcodes.H_INVOKESTATIC, MEDIA_MANAGER,
                "createMedia", desc, false);
        m.visitInvokeDynamicInsn("apply",
                "()Ljava/lang/Object;", metafactory,
                Type.getType("(Ljava/lang/Object;Ljava/lang/Object;)"
                        + "Ljava/lang/Object;"),
                target,
                Type.getType("(Ljava/lang/Object;Ljava/lang/Object;)"
                        + "Ljava/lang/Object;"));
        m.visitInsn(Opcodes.POP);
        m.visitInsn(Opcodes.RETURN);
        m.visitMaxs(0, 0);
        m.visitEnd();
        write(dir, "Ref", w);

        assertEquals("[createMedia" + desc + "]", scan(dir).toString());
    }

    // ---- which overload needs the permission -------------------------

    @Test
    void theUriOverloadsNeedTheReadMediaPermissions() {
        assertTrue(AndroidGradleBuilder.readsSharedMediaForPlayback(
                MEDIA_MANAGER, "createMedia",
                "(Ljava/lang/String;Z)L" + MEDIA + ";"));
        assertTrue(AndroidGradleBuilder.readsSharedMediaForPlayback(
                MEDIA_MANAGER, "createMedia",
                "(Ljava/lang/String;ZLjava/lang/Runnable;)L" + MEDIA + ";"));
        assertTrue(AndroidGradleBuilder.readsSharedMediaForPlayback(
                MEDIA_MANAGER, "createMediaAsync",
                "(Ljava/lang/String;ZLjava/lang/Runnable;)"
                        + "Lcom/codename1/util/AsyncResource;"));
        assertTrue(AndroidGradleBuilder.readsSharedMediaForPlayback(
                DISPLAY, "createMedia",
                "(Ljava/lang/String;ZLjava/lang/Runnable;)L" + MEDIA + ";"),
                "the Display facade is the same call");
    }

    /**
     * The stream overloads read nothing shared: the Android
     * implementation plays an already-open FileInputStream's descriptor
     * or copies the stream to a temp file in app-private storage, and
     * asks for no permission on either path.
     */
    @Test
    void theStreamOverloadsNeedNothing() {
        assertFalse(AndroidGradleBuilder.readsSharedMediaForPlayback(
                MEDIA_MANAGER, "createMedia",
                "(Ljava/io/InputStream;Ljava/lang/String;)L" + MEDIA + ";"));
        assertFalse(AndroidGradleBuilder.readsSharedMediaForPlayback(
                MEDIA_MANAGER, "createMedia",
                "(Ljava/io/InputStream;Ljava/lang/String;Ljava/lang/Runnable;)"
                        + "L" + MEDIA + ";"),
                "the overload from issue #5507");
        assertFalse(AndroidGradleBuilder.readsSharedMediaForPlayback(
                MEDIA_MANAGER, "createMediaAsync",
                "(Ljava/io/InputStream;Ljava/lang/String;Ljava/lang/Runnable;)"
                        + "Lcom/codename1/util/AsyncResource;"));
        assertFalse(AndroidGradleBuilder.readsSharedMediaForPlayback(
                DISPLAY, "createMedia",
                "(Ljava/io/InputStream;Ljava/lang/String;Ljava/lang/Runnable;)"
                        + "L" + MEDIA + ";"));
    }

    /**
     * The recorder writes, it does not read shared media, and its
     * descriptor starts with a String like the URI overloads do -- so the
     * name exclusion still has to hold.
     */
    @Test
    void theRecorderIsNotPlayback() {
        assertFalse(AndroidGradleBuilder.readsSharedMediaForPlayback(
                MEDIA_MANAGER, "createMediaRecorder",
                "(Ljava/lang/String;)L" + MEDIA + ";"));
        assertFalse(AndroidGradleBuilder.readsSharedMediaForPlayback(
                MEDIA_MANAGER, "createMediaRecorder",
                "(Ljava/lang/String;Ljava/lang/String;)L" + MEDIA + ";"));
    }

    @Test
    void unrelatedClassesAndMethodsAreIgnored() {
        assertFalse(AndroidGradleBuilder.readsSharedMediaForPlayback(
                "app/MyMediaManager", "createMedia",
                "(Ljava/lang/String;Z)L" + MEDIA + ";"));
        assertFalse(AndroidGradleBuilder.readsSharedMediaForPlayback(
                MEDIA_MANAGER, "addCompletionHandler",
                "(L" + MEDIA + ";Ljava/lang/Runnable;)V"));
        assertFalse(AndroidGradleBuilder.readsSharedMediaForPlayback(
                null, null, null));
    }

    /**
     * Unknown reads as the URI overload. Over-declaring costs a
     * permission the app does not use; under-declaring costs a
     * SecurityException on a user's device.
     */
    @Test
    void anUnknownDescriptorIsAssumedToNeedThePermission() {
        assertTrue(AndroidGradleBuilder.readsSharedMediaForPlayback(
                MEDIA_MANAGER, "createMedia", null));
    }

    // ---- which permissions reach the manifest ------------------------

    /**
     * Playback declares video and audio. Not images: nothing in the
     * Android port ever requests READ_MEDIA_IMAGES, and pairing it with
     * READ_MEDIA_VIDEO is what puts the app under Play's Photo and Video
     * Permissions policy.
     */
    @Test
    void playbackDeclaresVideoAndAudioButNotImages() {
        assertEquals("[android.permission.READ_MEDIA_VIDEO,"
                + " android.permission.READ_MEDIA_AUDIO]",
                AndroidGradleBuilder.readMediaPermissionNames(
                        false, 36, true, false).toString());
    }

    /** The explicit opt-in still declares all three. */
    @Test
    void theOptInHintDeclaresImagesToo() {
        assertEquals("[android.permission.READ_MEDIA_IMAGES,"
                + " android.permission.READ_MEDIA_VIDEO,"
                + " android.permission.READ_MEDIA_AUDIO]",
                AndroidGradleBuilder.readMediaPermissionNames(
                        false, 36, false, true).toString());
    }

    @Test
    void nothingIsDeclaredWithoutPlaybackOrTheOptIn() {
        assertTrue(AndroidGradleBuilder.readMediaPermissionNames(
                false, 36, false, false).isEmpty());
    }

    @Test
    void theBlockHintWins() {
        assertTrue(AndroidGradleBuilder.readMediaPermissionNames(
                true, 36, true, true).isEmpty(),
                "android.blockReadMediaPermissions must override both");
    }

    /** The permissions do not exist before API 33. */
    @Test
    void nothingIsDeclaredBelowApi33() {
        assertTrue(AndroidGradleBuilder.readMediaPermissionNames(
                false, 32, true, true).isEmpty());
    }
}
