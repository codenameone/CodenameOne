/*
 * Copyright (c) 2025, Codename One and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.
 */
package com.codename1.lottie.transcoder;

import com.codename1.lottie.transcoder.parser.LottieParser;
import com.codename1.svg.transcoder.codegen.JavaCodeGenerator;
import com.codename1.svg.transcoder.model.SVGDocument;

import java.io.BufferedInputStream;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;

/**
 * Top-level entry point: parse a Lottie {@code .json} (or {@code .lottie}
 * archive container) and emit a Codename One
 * {@code GeneratedSVGImage} subclass. The output is byte-identical in
 * structure to what {@code SVGTranscoder.transcode} emits -- Lottie is
 * just lowered into the SVG model first, so the same {@code SVGRegistry}
 * and per-port wiring picks it up at runtime.
 */
public final class LottieTranscoder {

    private LottieTranscoder() { }

    public static void transcode(InputStream in, String packageName, String className, Writer out) throws IOException {
        SVGDocument doc = LottieParser.parse(in);
        new JavaCodeGenerator(doc, packageName, className).generate(out);
    }

    public static void transcode(File file, String packageName, String className, File outFile) throws IOException {
        if (outFile.getParentFile() != null) {
            outFile.getParentFile().mkdirs();
        }
        InputStream in = new BufferedInputStream(new FileInputStream(file));
        try {
            Writer w = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(outFile), "UTF-8"));
            try {
                transcode(in, packageName, className, w);
            } finally {
                w.close();
            }
        } finally {
            in.close();
        }
    }
}
