package com.codename1.svg.transcoder.codegen;

import com.codename1.svg.transcoder.SVGTranscoder;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import javax.tools.JavaCompiler;
import javax.tools.JavaFileObject;
import javax.tools.SimpleJavaFileObject;
import javax.tools.StandardJavaFileManager;
import javax.tools.StandardLocation;
import javax.tools.ToolProvider;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.StringWriter;
import java.net.URI;
import java.util.Arrays;
import java.util.Collections;

import static org.junit.Assert.*;

/**
 * Hands-on end-to-end test: transcode each fixture SVG, hand the resulting
 * Java source to the in-process JDK compiler, and fail if the result doesn't
 * compile against the real {@code com.codename1.ui.GeneratedSVGImage} +
 * graphics API. This is the test that catches sloppy code emission in the
 * generator, where a syntactic check would not be enough.
 */
public class CompileGeneratedSourceTest {

    @Rule
    public TemporaryFolder tmp = new TemporaryFolder();

    private JavaCompiler compiler;

    @Before
    public void setUp() {
        compiler = ToolProvider.getSystemJavaCompiler();
        org.junit.Assume.assumeNotNull("JDK compiler available", compiler);
    }

    @Test
    public void shapesCompile() throws Exception {
        compileSvg("<svg xmlns='http://www.w3.org/2000/svg' viewBox='0 0 100 100'>"
                + "<rect x='10' y='10' width='80' height='80' fill='#FF8800' stroke='black' stroke-width='2' rx='8'/>"
                + "<circle cx='50' cy='50' r='30' fill='blue'/>"
                + "<ellipse cx='60' cy='60' rx='20' ry='10' fill='red'/>"
                + "<line x1='0' y1='0' x2='100' y2='100' stroke='green'/>"
                + "<polyline points='0,0 10,10 20,0' fill='none' stroke='black'/>"
                + "<polygon points='40,40 60,40 50,60' fill='purple'/>"
                + "</svg>", "Shapes");
    }

    @Test
    public void pathCompiles() throws Exception {
        compileSvg("<svg xmlns='http://www.w3.org/2000/svg' viewBox='0 0 24 24'>"
                + "<path d='M12 2 L22 22 L2 22 Z' fill='red'/>"
                + "<path d='M5 12 C8 5, 16 5, 19 12 S 30 25, 12 22' stroke='blue' fill='none' stroke-width='2'/>"
                + "<path d='M2 12 A5 5 0 0 1 22 12' fill='green'/>"
                + "</svg>", "Pathy");
    }

    @Test
    public void transformsCompile() throws Exception {
        compileSvg("<svg xmlns='http://www.w3.org/2000/svg' viewBox='0 0 100 100'>"
                + "<g transform='translate(10,10) rotate(45,40,40) scale(2)'>"
                + "<rect x='0' y='0' width='10' height='10' fill='red'/>"
                + "</g></svg>", "Transformed");
    }

    @Test
    public void linearGradientCompiles() throws Exception {
        compileSvg("<svg xmlns='http://www.w3.org/2000/svg' viewBox='0 0 100 100'>"
                + "<defs><linearGradient id='g1' x1='0' y1='0' x2='1' y2='0'>"
                + "<stop offset='0' stop-color='red'/>"
                + "<stop offset='1' stop-color='blue'/>"
                + "</linearGradient></defs>"
                + "<rect x='0' y='0' width='100' height='100' fill='url(#g1)'/>"
                + "</svg>", "Gradient");
    }

    @Test
    public void animationCompiles() throws Exception {
        compileSvg("<svg xmlns='http://www.w3.org/2000/svg' viewBox='0 0 100 100'>"
                + "<circle cx='50' cy='50' r='10' fill='red'>"
                + "<animate attributeName='r' from='10' to='40' dur='2s' repeatCount='indefinite'/>"
                + "</circle>"
                + "<g transform='translate(10,10)'>"
                + "<animateTransform attributeName='transform' type='rotate'"
                + " from='0 50 50' to='360 50 50' dur='3s' repeatCount='indefinite'/>"
                + "<rect x='40' y='40' width='20' height='20' fill='blue'/>"
                + "</g></svg>", "Animated");
    }

    @Test
    public void registryCompiles() throws Exception {
        StringWriter sw = new StringWriter();
        SVGTranscoder.writeRegistry("com.test.gen", "SVGRegistry",
                Arrays.asList(
                        new SVGTranscoder.GeneratedClass("com.test.gen", "FooSvg", "foo.svg"),
                        new SVGTranscoder.GeneratedClass("com.test.gen", "BarSvg", "bar.svg")
                ), sw);
        // We can't compile the registry without the actual generated classes, so
        // just sanity-check the source contents.
        String src = sw.toString();
        assertTrue(src.contains("package com.test.gen;"));
        assertTrue(src.contains("public static void install(Resources r)"));
        assertTrue(src.contains("new com.test.gen.FooSvg()"));
        assertTrue(src.contains("Resources.registerGeneratedImage(\"foo.svg\""));
    }

    private void compileSvg(String svg, String className) throws Exception {
        StringWriter sw = new StringWriter();
        SVGTranscoder.transcode(new ByteArrayInputStream(svg.getBytes("UTF-8")),
                "gen", className, sw);
        String source = sw.toString();
        File outDir = tmp.newFolder("classes");

        StandardJavaFileManager fileManager = compiler.getStandardFileManager(null, null, null);
        try {
            fileManager.setLocation(StandardLocation.CLASS_OUTPUT, Collections.singleton(outDir));
            JavaFileObject src = new InMemorySource("gen." + className, source);
            JavaCompiler.CompilationTask task = compiler.getTask(null, fileManager, null,
                    Arrays.asList("-Xlint:none", "-proc:none"), null, Collections.singleton(src));
            boolean ok = task.call();
            if (!ok) {
                fail("Generated source failed to compile:\n" + source);
            }
        } finally {
            fileManager.close();
        }
    }

    private static final class InMemorySource extends SimpleJavaFileObject {
        private final String content;
        InMemorySource(String fqn, String content) {
            super(URI.create("mem:///" + fqn.replace('.', '/') + Kind.SOURCE.extension), Kind.SOURCE);
            this.content = content;
        }
        @Override
        public CharSequence getCharContent(boolean ignoreEncodingErrors) throws IOException {
            return content;
        }
    }
}
