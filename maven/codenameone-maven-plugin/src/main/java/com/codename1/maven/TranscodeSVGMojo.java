package com.codename1.maven;

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.plugins.annotations.ResolutionScope;

import java.io.File;
import java.util.List;

/**
 * Scans an application module for vector animation files (SVG and
 * Lottie / Bodymovin JSON), transcodes each into a
 * {@code com.codename1.ui.GeneratedSVGImage} subclass under
 * {@code target/generated-sources/svg}, and emits a registry class that
 * installs each transcoded image into a Resources instance (and the global
 * fallback) under its source filename.
 *
 * <h3>Source layout</h3>
 * Files are picked up from format-specific directories plus the shared
 * {@code src/main/css/} directory so designers can drop assets next to the
 * theme CSS that references them:
 * <ul>
 *   <li>{@code src/main/svg/} -- {@code *.svg}</li>
 *   <li>{@code src/main/lottie/} -- {@code *.json}, {@code *.lottie}</li>
 *   <li>{@code src/main/css/} -- either of the above</li>
 * </ul>
 * Theme CSS keeps the natural {@code background: url(spinner.svg);}
 * reference for SVG, and the same {@code url(...)} syntax resolves
 * {@code .json} / {@code .lottie} at runtime.
 *
 * <h3>CSS hints</h3>
 * For each {@code url(*.svg)} occurrence the mojo also looks at the rule's
 * {@code cn1-source-dpi:} declaration (the same hint used for multi-images).
 * The transcoded SVG is then constructed with that source density so its
 * intrinsic dimensions scale to the device-pixel size CN1 multi-images
 * normally produce. Without a {@code cn1-source-dpi} the SVG's declared
 * dimensions are treated as design pixels at {@code DENSITY_MEDIUM}.
 *
 * <h3>CSS placeholders</h3>
 * To keep the standalone CSS compiler from failing on the {@code .svg} URL
 * (it expects to rasterize the referenced file), the mojo emits a 1x1
 * transparent PNG next to each CSS-referenced SVG. The placeholder lands in
 * {@code target/css-resources/} (a directory added to the compile-time CSS
 * search path); the runtime SVGRegistry's {@code install()} then overrides
 * the placeholder entry in the resources bundle with the real transcoded
 * SVG instance.
 */
@Mojo(name = "transcode-svg", defaultPhase = LifecyclePhase.GENERATE_SOURCES,
        requiresDependencyResolution = ResolutionScope.NONE,
        requiresDependencyCollection = ResolutionScope.NONE)
public class TranscodeSVGMojo extends AbstractCN1Mojo {

    @Parameter(property = "cn1.svg.sourceDirs")
    private List<String> svgSourceDirs;

    @Parameter(property = "cn1.svg.outputDir",
            defaultValue = "${project.build.directory}/generated-sources/svg")
    private File svgOutputDir;

    @Parameter(property = "cn1.svg.placeholderDir",
            defaultValue = "${project.build.directory}/css-resources")
    private File svgPlaceholderDir;

    @Parameter(property = "cn1.svg.package", defaultValue = SvgTranscodeRunner.DEFAULT_PACKAGE)
    private String svgPackage;

    @Override
    protected void executeImpl() throws MojoExecutionException, MojoFailureException {
        // The work lives in SvgTranscodeRunner because the self-repair in
        // AbstractCN1Mojo.ensureSvgTranscoderWired has to perform it for a
        // project whose pom never binds this mojo. See that class.
        new SvgTranscodeRunner(project.getBasedir(), svgSourceDirs, svgOutputDir,
                svgPlaceholderDir, svgPackage, getLog()).run();
        registerSourceRoot(svgOutputDir);
    }
}
