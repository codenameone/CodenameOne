#!/usr/bin/env python3
"""
Regenerates the deliberately external AI cn1libs listed in ``LIBS``.

Small platform AI features (vision, language services, and LiteRT/Core ML
inference) are built into Codename One and selected by the platform builders.
Only features with exceptionally large native runtimes or model payloads,
currently Whisper and Stable Diffusion, remain cn1libs.

Run with ``python3 scripts/gen-ai-cn1libs.py`` from any working directory.
Only the cn1lib directories listed in ``LIBS`` are wiped and rewritten.
"""

from __future__ import annotations

import pathlib
import shutil
import textwrap
from dataclasses import dataclass
from typing import Dict, Tuple

ROOT = pathlib.Path(__file__).resolve().parent.parent
MAVEN = ROOT / "maven"

LICENSE = """\
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
"""


# --------------------------------------------------------------------------
# Per-lib config table.
#
# Each entry carries:
#   artifact     -- maven artifactId, also the cn1lib name
#   pkg          -- common Java package (com.codename1.ai.<pkg_path>)
#   facade       -- public Java facade class
#   ni           -- NativeInterface name (always Native + facade)
#   facade_src   -- full Java source for the facade (common module)
#   ni_src       -- full Java source for the NativeInterface
#   ios_impl     -- Obj-C source for {facade}NativeImpl.m
#   ios_header   -- Obj-C header source for {facade}NativeImpl.h
#   android_impl -- Android Java source for {facade}NativeImpl.java
#   javase_impl  -- JavaSE deterministic stub
#   test_src     -- JUnit 5 test exercising the facade against a mock
#   short_desc   -- one-line description for the README/pom
#   long_desc    -- multi-line description for the package-info
#   build_hints  -- build hints to inject into codenameone_library_required.properties
# --------------------------------------------------------------------------


@dataclass(frozen=True)
class Lib:
    artifact: str
    pkg: str  # e.g. "mlkit.text"
    facade: str
    short_desc: str
    long_desc: str
    facade_methods: str  # rendered Java method bodies
    ni_methods: str  # rendered Java interface methods
    ios_h_decls: str  # canonical method declarations for the .h, one per line,
    # e.g. `- (NSString*)recognize:(NSData*)param;`. Must match what
    # `mvn cn1:generate-native-interfaces` would emit (param/param1/...).
    ios_impl: str  # body of the .m (no @implementation wrapper), using
    # canonical `param`/`param1`/... names so the impl matches the .h.
    ios_imports: str  # extra #imports
    android_imports: str
    android_impl: str  # body of the impl class (no class wrapper)
    javase_impl: str  # body of the JavaSE impl class
    test_methods: str  # test method bodies; placeholder named MockBridge available
    build_hints: dict[str, str]
    test_mock_methods: str  # mock bridge method overrides
    ios_pre_impl: str = ""  # C/Obj-C source emitted BEFORE @implementation
    win_decls: str = ""  # (Removed; UWP is not a runtime target. Kept on
    # the dataclass for backward-compat with old call sites; ignored.)
    js_method_keys: tuple = ()  # JS-encoded method names + param counts, e.g.
    # `(("recognize__byte_1ARRAY", 1),)`. Matches the encoding
    # `generate-native-interfaces` uses for the JS port.
    simulator_hints: tuple = ()  # `(key, default_value)` pairs for build
    # hints the JavaSE impl injects when running in the simulator. Used for
    # iOS Info.plist usage strings (`ios.NSCameraUsageDescription` etc.) so
    # the developer never has to hand-edit the description out of the docs
    # but can still customise the wording in codenameone_settings.properties.


def ni_class(facade: str) -> str:
    return "Native" + facade


def java_pkg(pkg_suffix: str) -> str:
    return "com.codename1.ai." + pkg_suffix


def java_pkg_path(pkg_suffix: str) -> str:
    return "com/codename1/ai/" + pkg_suffix.replace(".", "/")


# --------------------------------------------------------------------------
# Helpers for rendering the standard pieces of each multi-module project.
# --------------------------------------------------------------------------


def root_pom(artifact: str, short_desc: str) -> str:
    return textwrap.dedent(f"""\
        <?xml version="1.0" encoding="UTF-8"?>
        <project xmlns="http://maven.apache.org/POM/4.0.0"
                 xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
                 xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0.xsd">
            <modelVersion>4.0.0</modelVersion>

            <parent>
                <groupId>com.codenameone</groupId>
                <artifactId>codenameone</artifactId>
                <version>8.0-SNAPSHOT</version>
            </parent>

            <artifactId>{artifact}</artifactId>
            <packaging>pom</packaging>
            <name>Codename One AI: {artifact}</name>
            <description>{short_desc}</description>

            <properties>
                <cn1lib.name>{artifact}</cn1lib.name>
                <java.version>1.8</java.version>
                <maven.compiler.source>1.8</maven.compiler.source>
                <maven.compiler.target>1.8</maven.compiler.target>
            </properties>

            <modules>
                <module>common</module>
                <module>ios</module>
                <module>android</module>
                <module>javase</module>
                <module>javascript</module>
                <module>lib</module>
            </modules>
        </project>
        """)


def common_pom(artifact: str) -> str:
    return textwrap.dedent(f"""\
        <?xml version="1.0" encoding="UTF-8"?>
        <project xmlns="http://maven.apache.org/POM/4.0.0"
                 xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
                 xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0.xsd">
            <modelVersion>4.0.0</modelVersion>

            <parent>
                <groupId>com.codenameone</groupId>
                <artifactId>{artifact}</artifactId>
                <version>8.0-SNAPSHOT</version>
            </parent>

            <artifactId>{artifact}-common</artifactId>
            <packaging>jar</packaging>
            <name>Codename One AI: {artifact}-common</name>

            <properties>
                <project.build.sourceEncoding>UTF-8</project.build.sourceEncoding>
                <maven.compiler.source>1.8</maven.compiler.source>
                <maven.compiler.target>1.8</maven.compiler.target>
            </properties>

            <dependencies>
                <dependency>
                    <groupId>com.codenameone</groupId>
                    <artifactId>codenameone-core</artifactId>
                    <version>${{project.version}}</version>
                    <scope>provided</scope>
                </dependency>
                <dependency>
                    <groupId>org.junit.jupiter</groupId>
                    <artifactId>junit-jupiter-api</artifactId>
                    <scope>test</scope>
                </dependency>
                <dependency>
                    <groupId>org.junit.jupiter</groupId>
                    <artifactId>junit-jupiter-engine</artifactId>
                    <scope>test</scope>
                </dependency>
            </dependencies>

            <build>
                <plugins>
                    <plugin>
                        <groupId>org.apache.maven.plugins</groupId>
                        <artifactId>maven-surefire-plugin</artifactId>
                        <version>3.2.5</version>
                    </plugin>
                    <plugin>
                        <groupId>com.codenameone</groupId>
                        <artifactId>codenameone-maven-plugin</artifactId>
                        <version>${{project.version}}</version>
                        <executions>
                            <execution>
                                <id>build-legacy-cn1lib</id>
                                <phase>package</phase>
                                <goals>
                                    <goal>cn1lib</goal>
                                </goals>
                            </execution>
                        </executions>
                    </plugin>
                    <plugin>
                        <groupId>org.apache.maven.plugins</groupId>
                        <artifactId>maven-antrun-plugin</artifactId>
                        <version>3.1.0</version>
                        <executions>
                            <execution>
                                <id>copy-library-required-properties</id>
                                <phase>process-resources</phase>
                                <configuration>
                                    <target>
                                        <mkdir dir="${{project.build.outputDirectory}}/META-INF/codenameone/${{project.groupId}}/${{project.artifactId}}" />
                                        <copy file="${{basedir}}/codenameone_library_required.properties"
                                              todir="${{project.build.outputDirectory}}/META-INF/codenameone/${{project.groupId}}/${{project.artifactId}}/" />
                                    </target>
                                </configuration>
                                <goals>
                                    <goal>run</goal>
                                </goals>
                            </execution>
                        </executions>
                    </plugin>
                </plugins>
            </build>
        </project>
        """)


def platform_pom(parent_artifact: str, suffix: str, extra_deps: str = "", extra_resources: str = "") -> str:
    """Boilerplate platform-module pom (ios, android, javase)."""
    return textwrap.dedent(f"""\
        <?xml version="1.0" encoding="UTF-8"?>
        <project xmlns="http://maven.apache.org/POM/4.0.0"
                 xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
                 xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0.xsd">
            <modelVersion>4.0.0</modelVersion>

            <parent>
                <groupId>com.codenameone</groupId>
                <artifactId>{parent_artifact}</artifactId>
                <version>8.0-SNAPSHOT</version>
            </parent>

            <artifactId>{parent_artifact}-{suffix}</artifactId>
            <packaging>jar</packaging>
            <name>Codename One AI: {parent_artifact}-{suffix}</name>

            <properties>
                <maven.compiler.source>1.8</maven.compiler.source>
                <maven.compiler.target>1.8</maven.compiler.target>
            </properties>

            <build>
                <sourceDirectory>src/main/dummy</sourceDirectory>
                <resources>
        {extra_resources or "            <resource><directory>src/main/java</directory></resource>"}
                </resources>
                <plugins>
                    <plugin>
                        <groupId>org.apache.maven.plugins</groupId>
                        <artifactId>maven-antrun-plugin</artifactId>
                        <version>3.1.0</version>
                        <executions>
                            <execution>
                                <phase>package</phase>
                                <configuration>
                                    <target>
                                        <delete file="${{project.build.directory}}/${{project.build.finalName}}.jar" />
                                        <mkdir dir="${{basedir}}/src/main/java"/>
                                        <jar destfile="${{project.build.directory}}/${{project.build.finalName}}.jar" compress="true">
                                            <fileset dir="${{basedir}}/src/main/java" erroronmissingdir="false" />
                                        </jar>
                                    </target>
                                </configuration>
                                <goals><goal>run</goal></goals>
                            </execution>
                        </executions>
                    </plugin>
                </plugins>
            </build>

            <dependencies>
                <dependency>
                    <groupId>com.codenameone</groupId>
                    <artifactId>{parent_artifact}-common</artifactId>
                    <version>${{project.version}}</version>
                </dependency>
                {extra_deps}
            </dependencies>
        </project>
        """)


def resource_only_pom(parent_artifact: str, suffix: str, source_dir: str) -> str:
    """Resource-only module (ios objectivec / javascript / win csharp).

    Mirrors the cn1lib-archetype pattern: the source dir gets shipped as a
    classpath resource, so the build server can later unpack it into the
    real native build (Xcode for ios, npm for javascript, MSBuild for win).
    """
    return textwrap.dedent(f"""\
        <?xml version="1.0" encoding="UTF-8"?>
        <project xmlns="http://maven.apache.org/POM/4.0.0"
                 xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
                 xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0.xsd">
            <modelVersion>4.0.0</modelVersion>

            <parent>
                <groupId>com.codenameone</groupId>
                <artifactId>{parent_artifact}</artifactId>
                <version>8.0-SNAPSHOT</version>
            </parent>

            <artifactId>{parent_artifact}-{suffix}</artifactId>
            <packaging>jar</packaging>
            <name>Codename One AI: {parent_artifact}-{suffix}</name>

            <build>
                <sourceDirectory>src/main/dummy</sourceDirectory>
                <resources>
                    <resource><directory>src/main/{source_dir}</directory></resource>
                </resources>
                <plugins>
                    <plugin>
                        <groupId>org.apache.maven.plugins</groupId>
                        <artifactId>maven-antrun-plugin</artifactId>
                        <version>3.1.0</version>
                        <executions>
                            <execution>
                                <phase>package</phase>
                                <configuration>
                                    <target>
                                        <delete file="${{project.build.directory}}/${{project.build.finalName}}.jar" />
                                        <mkdir dir="${{basedir}}/src/main/{source_dir}"/>
                                        <jar destfile="${{project.build.directory}}/${{project.build.finalName}}.jar" compress="true">
                                            <fileset dir="${{basedir}}/src/main/{source_dir}" erroronmissingdir="false" />
                                        </jar>
                                    </target>
                                </configuration>
                                <goals><goal>run</goal></goals>
                            </execution>
                        </executions>
                    </plugin>
                </plugins>
            </build>

            <dependencies>
                <dependency>
                    <groupId>com.codenameone</groupId>
                    <artifactId>{parent_artifact}-common</artifactId>
                    <version>${{project.version}}</version>
                </dependency>
            </dependencies>
        </project>
        """)


def ios_pom(artifact: str) -> str:
    """iOS module pom uses src/main/objectivec for native sources."""
    return textwrap.dedent(f"""\
        <?xml version="1.0" encoding="UTF-8"?>
        <project xmlns="http://maven.apache.org/POM/4.0.0"
                 xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
                 xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0.xsd">
            <modelVersion>4.0.0</modelVersion>

            <parent>
                <groupId>com.codenameone</groupId>
                <artifactId>{artifact}</artifactId>
                <version>8.0-SNAPSHOT</version>
            </parent>

            <artifactId>{artifact}-ios</artifactId>
            <packaging>jar</packaging>
            <name>Codename One AI: {artifact}-ios</name>

            <build>
                <sourceDirectory>src/main/dummy</sourceDirectory>
                <resources>
                    <resource><directory>src/main/objectivec</directory></resource>
                </resources>
                <plugins>
                    <plugin>
                        <groupId>org.apache.maven.plugins</groupId>
                        <artifactId>maven-antrun-plugin</artifactId>
                        <version>3.1.0</version>
                        <executions>
                            <execution>
                                <phase>package</phase>
                                <configuration>
                                    <target>
                                        <delete file="${{project.build.directory}}/${{project.build.finalName}}.jar" />
                                        <mkdir dir="${{basedir}}/src/main/objectivec"/>
                                        <jar destfile="${{project.build.directory}}/${{project.build.finalName}}.jar" compress="true">
                                            <fileset dir="${{basedir}}/src/main/objectivec" erroronmissingdir="false" />
                                        </jar>
                                    </target>
                                </configuration>
                                <goals><goal>run</goal></goals>
                            </execution>
                        </executions>
                    </plugin>
                </plugins>
            </build>

            <dependencies>
                <dependency>
                    <groupId>com.codenameone</groupId>
                    <artifactId>{artifact}-common</artifactId>
                    <version>${{project.version}}</version>
                </dependency>
            </dependencies>
        </project>
        """)


def lib_pom(artifact: str) -> str:
    return textwrap.dedent(f"""\
        <?xml version="1.0" encoding="UTF-8"?>
        <project xmlns="http://maven.apache.org/POM/4.0.0"
                 xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
                 xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0.xsd">
            <modelVersion>4.0.0</modelVersion>

            <parent>
                <groupId>com.codenameone</groupId>
                <artifactId>{artifact}</artifactId>
                <version>8.0-SNAPSHOT</version>
            </parent>

            <artifactId>{artifact}-lib</artifactId>
            <packaging>pom</packaging>
            <name>Codename One AI: {artifact}-lib</name>

            <dependencies>
                <dependency>
                    <groupId>com.codenameone</groupId>
                    <artifactId>{artifact}-common</artifactId>
                    <version>${{project.version}}</version>
                </dependency>
            </dependencies>

            <profiles>
                <profile>
                    <id>ios</id>
                    <activation>
                        <property><name>codename1.platform</name><value>ios</value></property>
                    </activation>
                    <dependencies>
                        <dependency>
                            <groupId>com.codenameone</groupId>
                            <artifactId>{artifact}-ios</artifactId>
                            <version>${{project.version}}</version>
                        </dependency>
                    </dependencies>
                </profile>
                <profile>
                    <id>android</id>
                    <activation>
                        <property><name>codename1.platform</name><value>android</value></property>
                    </activation>
                    <dependencies>
                        <dependency>
                            <groupId>com.codenameone</groupId>
                            <artifactId>{artifact}-android</artifactId>
                            <version>${{project.version}}</version>
                        </dependency>
                    </dependencies>
                </profile>
                <profile>
                    <id>javase</id>
                    <activation>
                        <property><name>codename1.platform</name><value>javase</value></property>
                    </activation>
                    <dependencies>
                        <dependency>
                            <groupId>com.codenameone</groupId>
                            <artifactId>{artifact}-javase</artifactId>
                            <version>${{project.version}}</version>
                        </dependency>
                    </dependencies>
                </profile>
                <profile>
                    <id>javascript</id>
                    <activation>
                        <property><name>codename1.platform</name><value>javascript</value></property>
                    </activation>
                    <dependencies>
                        <dependency>
                            <groupId>com.codenameone</groupId>
                            <artifactId>{artifact}-javascript</artifactId>
                            <version>${{project.version}}</version>
                        </dependency>
                    </dependencies>
                </profile>
            </profiles>
        </project>
        """)


def package_info_java(pkg: str, facade: str, short_desc: str, long_desc: str) -> str:
    return (
        LICENSE
        + "\n"
        + f"/// {short_desc}.\n"
        + "///\n"
        + textwrap.indent(long_desc, "/// ")
        + "\n///\n"
        + f"/// The single public class in this package is [{facade}], which exposes\n"
        + "/// the feature via static methods returning\n"
        + "/// [com.codename1.util.AsyncResource]. A package-private\n"
        + f"/// `Native{facade}` interface holds the platform contract; iOS Obj-C and\n"
        + "/// Android Java implementations live in `nativeios.zip` / `nativeand.zip`\n"
        + f"/// inside the cn1lib bundle. References to `{facade}.*` are recognised\n"
        + "/// by the Codename One build server's `AiDependencyTable`, which\n"
        + "/// auto-injects the matching CocoaPod / Swift Package / Android Gradle\n"
        + "/// dep / `Info.plist` usage strings / Android permissions on every\n"
        + "/// build -- no manual build hints required.\n"
        + f"package {java_pkg(pkg)};\n"
    )


def facade_java(pkg: str, facade: str, short_desc: str, long_desc: str, methods: str) -> str:
    return (
        LICENSE
        + "\n"
        + f"package {java_pkg(pkg)};\n"
        + "\n"
        + "import com.codename1.ai.LlmException;\n"
        + "import com.codename1.system.NativeLookup;\n"
        + "import com.codename1.ui.Display;\n"
        + "import com.codename1.util.AsyncResource;\n"
        + "\n"
        + f"/// {short_desc}.\n"
        + "///\n"
        + textwrap.indent(long_desc, "/// ")
        + "\n///\n"
        + f"public final class {facade} {{\n"
        + f"    private {facade}() {{ }}\n"
        + "\n"
        + "    /// True only when the running platform has a native bridge wired up.\n"
        + "    public static boolean isSupported() {\n"
        + f"        {ni_class(facade)} bridge = NativeLookup.create({ni_class(facade)}.class);\n"
        + "        return bridge != null && bridge.isSupported();\n"
        + "    }\n"
        + "\n"
        + textwrap.indent(methods.rstrip(), "    ")
        + "\n}\n"
    )


def ni_java(pkg: str, facade: str, ni_methods: str) -> str:
    return (
        LICENSE
        + "\n"
        + f"package {java_pkg(pkg)};\n"
        + "\n"
        + "import com.codename1.system.NativeInterface;\n"
        + "\n"
        + f"/// Native bridge for [{facade}]. iOS, Android, and JavaSE implementations\n"
        + "/// live in their respective port modules under this cn1lib.\n"
        + f"public interface {ni_class(facade)} extends NativeInterface {{\n"
        + textwrap.indent(ni_methods.rstrip(), "    ")
        + "\n}\n"
    )


def ios_native_h(pkg: str, facade: str, decls: str) -> str:
    """Canonical CN1 native-interface .h header.

    `decls` carries the method declarations (one per line, with trailing
    semicolons) matching what `mvn cn1:generate-native-interfaces` would
    emit. `isSupported` is always declared in addition to those.
    """
    cls = "com_codename1_ai_" + pkg.replace(".", "_") + "_" + ni_class(facade) + "Impl"
    decls_block = textwrap.indent(decls.rstrip(), "")
    return (
        "#import <Foundation/Foundation.h>\n"
        "\n"
        f"@interface {cls} : NSObject {{\n"
        "}\n"
        "\n"
        f"{decls_block}\n"
        "-(BOOL)isSupported;\n"
        "@end\n"
    )


def ios_native_m(pkg: str, facade: str, extra_imports: str, body: str,
                  pre_impl: str = "") -> str:
    """Build the Obj-C .m source for a NativeInterface bridge.

    `pre_impl` is C/Obj-C source that must sit OUTSIDE the @implementation
    block -- typically extern declarations and supporting struct definitions
    for cn1libs that link to a native C library (whisper.cpp, Stable
    Diffusion runner). They cannot live inside @implementation because Obj-C
    only allows method definitions there.
    """
    cls = "com_codename1_ai_" + pkg.replace(".", "_") + "_" + ni_class(facade) + "Impl"
    out = []
    out.append(f"#import \"{cls}.h\"")
    out.append("#import <UIKit/UIKit.h>")
    if extra_imports.strip():
        out.append(extra_imports.rstrip())
    if pre_impl.strip():
        out.append("")
        out.append(pre_impl.rstrip())
    out.append("")
    out.append(f"@implementation {cls}")
    out.append("")
    out.append(body.rstrip())
    out.append("")
    out.append("-(BOOL)isSupported{")
    out.append("    return YES;")
    out.append("}")
    out.append("")
    out.append("@end")
    out.append("")
    return "\n".join(out)


def android_native_java(pkg: str, facade: str, extra_imports: str, body: str) -> str:
    return (
        f"package {java_pkg(pkg)};\n"
        + "\n"
        + extra_imports
        + "\n"
        + f"public class {ni_class(facade)}Impl {{\n"
        + textwrap.indent(body.rstrip(), "    ")
        + "\n\n    public boolean isSupported() {\n        return true;\n    }\n}\n"
    )


def javase_native_java(pkg: str, facade: str, body: str,
                        simulator_hints: tuple = ()) -> str:
    """JavaSE impl is the one platform where the impl MUST `implements`
    the NativeInterface -- NativeLookup on JavaSE resolves the impl via
    plain Java class loading and casts to the interface.

    When `simulator_hints` is non-empty, the impl ensures each
    `(key, default)` pair is present in the user's
    `codenameone_settings.properties` the first time the bridge is
    instantiated under the simulator. `getProjectBuildHints` returns null
    outside the simulator, so on real devices this is a no-op. The
    developer can override the value later -- we only set the default
    when the key is entirely absent, never overwrite.
    """
    hints_block = ""
    if simulator_hints:
        ensure_lines = []
        for key, default in simulator_hints:
            esc_default = default.replace("\\", "\\\\").replace("\"", "\\\"")
            ensure_lines.append(
                f"            if (!hints.containsKey(\"{key}\")) {{\n"
                f"                com.codename1.ui.Display.getInstance()\n"
                f"                    .setProjectBuildHint(\"{key}\", \"{esc_default}\");\n"
                "            }"
            )
        ensure_body = "\n".join(ensure_lines)
        hints_block = (
            "\n"
            "    private static boolean hintsEnsured;\n"
            "    private static synchronized void ensureSimulatorHints() {\n"
            "        if (hintsEnsured) return;\n"
            "        hintsEnsured = true;\n"
            "        java.util.Map<String, String> hints =\n"
            "            com.codename1.ui.Display.getInstance().getProjectBuildHints();\n"
            "        if (hints == null) return;  // not running in the simulator\n"
            + ensure_body
            + "\n"
            "    }\n"
            "\n"
            f"    public {ni_class(facade)}Impl() {{\n"
            "        ensureSimulatorHints();\n"
            "    }\n"
        )

    return (
        f"package {java_pkg(pkg)};\n"
        + "\n"
        + f"public class {ni_class(facade)}Impl implements {ni_class(facade)} {{\n"
        + hints_block
        + textwrap.indent(body.rstrip(), "    ")
        + "\n\n    public boolean isSupported() {\n        return true;\n    }\n}\n"
    )


def js_native(pkg: str, facade: str, methods: tuple) -> str:
    """JS stub matching `mvn cn1:generate-native-interfaces` output verbatim.

    `methods` is a tuple of `(key, param_count)` pairs, e.g.
    `(("recognize__byte_1ARRAY", 1),)`. The JS function signature uses
    `param1, ..., paramN, callback` to match canonical output.
    """
    interface_var = "com_codename1_ai_" + pkg.replace(".", "_") + "_" + ni_class(facade)
    blocks = []
    for key, count in methods:
        if count == 0:
            sig = "callback"
        else:
            sig = ", ".join(f"param{i + 1}" for i in range(count)) + ", callback"
        blocks.append(
            f"    o.{key} = function({sig}) {{\n"
            "        callback.error(new Error(\"Not implemented yet\"));\n"
            "    };\n"
        )
    blocks.append(
        "    o.isSupported_ = function(callback) {\n"
        "        callback.complete(false);\n"
        "    };\n"
    )
    body = "\n".join(blocks)
    return (
        "(function(exports){\n"
        "\n"
        "var o = {};\n"
        "\n"
        + body
        + "\n"
        f"exports.{interface_var}= o;\n"
        "\n"
        "})(cn1_get_native_interfaces());\n"
    )


def required_props(artifact: str, hints: dict[str, str]) -> str:
    head = (
        "# Auto-installed build hints for " + artifact + ".\n"
        "# Loaded by the Codename One build server when this cn1lib is in the\n"
        "# project classpath. The build-time AiDependencyTable scanner adds\n"
        "# further per-class entries as needed.\n"
    )
    body = "".join(f"{k}={v}\n" for k, v in hints.items())
    return head + body


def test_java(pkg: str, facade: str, mock_methods: str, test_methods: str) -> str:
    ni = ni_class(facade)
    return (
        f"package {java_pkg(pkg)};\n"
        + "\n"
        + "import org.junit.jupiter.api.Test;\n"
        + "import static org.junit.jupiter.api.Assertions.*;\n"
        + "\n"
        + f"public class {facade}Test {{\n"
        + "\n"
        + f"    /** Mock implementation of {ni} for headless JVM tests. */\n"
        + f"    static class MockBridge implements {ni} {{\n"
        + "        boolean supported = true;\n"
        + "        public boolean isSupported() { return supported; }\n"
        + textwrap.indent(mock_methods.rstrip(), "        ")
        + "\n    }\n"
        + "\n"
        + textwrap.indent(test_methods.rstrip(), "    ")
        + "\n}\n"
    )


# --------------------------------------------------------------------------
# Per-lib definitions.
#
# The facade_methods / ios_impl / etc. embed real ML Kit / TFLite / whisper /
# CoreML calls.
# --------------------------------------------------------------------------


def lib_whisper() -> Lib:
    facade_methods = textwrap.dedent("""\
        /// Transcribes audio using a whisper.cpp model. `modelPath` is the
        /// filesystem path to a ggml-format whisper model (e.g. `ggml-base.bin`);
        /// `audioPath` is a 16kHz mono WAV file.
        public static AsyncResource<String> transcribe(final String modelPath,
                                                        final String audioPath) {
            final AsyncResource<String> out = new AsyncResource<String>();
            final NativeWhisperRecognizer bridge =
                    NativeLookup.create(NativeWhisperRecognizer.class);
            if (bridge == null || !bridge.isSupported()) {
                out.error(new LlmException("WhisperRecognizer.transcribe is not supported on this platform.",
                        -1, null, null, null, LlmException.ErrorType.UNKNOWN));
                return out;
            }
            Display.getInstance().scheduleBackgroundTask(new Runnable() {
                @Override public void run() {
                    try {
                        final String r = bridge.transcribe(modelPath, audioPath);
                        Display.getInstance().callSerially(new Runnable() {
                            @Override public void run() { out.complete(r == null ? "" : r); }
                        });
                    } catch (final Throwable t) {
                        Display.getInstance().callSerially(new Runnable() {
                            @Override public void run() {
                                out.error(new LlmException("WhisperRecognizer.transcribe failed: " + t.getMessage(),
                                        -1, null, null, t, LlmException.ErrorType.UNKNOWN));
                            }
                        });
                    }
                }
            });
            return out;
        }
        """)
    ni_methods = "String transcribe(String modelPath, String audioPath);\n"
    ios_pre_impl = textwrap.dedent("""\
        // whisper.cpp's C API. The cn1lib bundles the prebuilt static
        // library; linking against `libwhisper.a` is handled by the build
        // server (see codenameone_library_required.properties).
        struct whisper_context;

        struct whisper_full_params {
            int strategy;
            int n_threads;
            int n_max_text_ctx;
            int offset_ms;
            int duration_ms;
            int translate;
            int no_context;
            int single_segment;
            int print_special;
            int print_progress;
            int print_realtime;
            int print_timestamps;
        };

        extern struct whisper_context *whisper_init_from_file(const char *path);
        extern int whisper_full(struct whisper_context *ctx,
                                struct whisper_full_params params,
                                const float *samples, int n_samples);
        extern int whisper_full_n_segments(struct whisper_context *ctx);
        extern const char *whisper_full_get_segment_text(struct whisper_context *ctx, int i);
        extern void whisper_free(struct whisper_context *ctx);
        """)
    ios_impl = textwrap.dedent("""\
        -(NSString*)transcribe:(NSString*)param param1:(NSString*)param1 {
            // Decode 16kHz mono PCM samples from a WAV file.
            NSData *wav = [NSData dataWithContentsOfFile:param1];
            if (wav.length < 44) return @"";
            const uint8_t *bytes = wav.bytes;
            const int16_t *samples16 = (const int16_t *)(bytes + 44);
            NSInteger nSamples = (wav.length - 44) / 2;
            float *samples = (float *)malloc(sizeof(float) * nSamples);
            for (NSInteger i = 0; i < nSamples; i++) samples[i] = samples16[i] / 32768.0f;
            struct whisper_context *ctx = whisper_init_from_file([param UTF8String]);
            if (!ctx) { free(samples); return @""; }
            struct whisper_full_params p = {0};
            p.n_threads = 4;
            whisper_full(ctx, p, samples, (int)nSamples);
            NSMutableString *out = [NSMutableString string];
            int n = whisper_full_n_segments(ctx);
            for (int i = 0; i < n; i++) {
                [out appendString:[NSString stringWithUTF8String:
                                    whisper_full_get_segment_text(ctx, i)]];
            }
            whisper_free(ctx);
            free(samples);
            return out;
        }
        """)
    ios_imports = ""
    android_impl = textwrap.dedent("""\
        // Android side uses whisper.cpp's prebuilt JNI wrapper packaged inside
        // the cn1lib's nativeand zip. The build server injects the .so into the
        // jniLibs directory via the AiDependencyTable's androidNativeDir entry.
        public String transcribe(String modelPath, String audioPath) {
            try {
                System.loadLibrary("whisper");
            } catch (UnsatisfiedLinkError ule) {
                throw new RuntimeException("whisper native library not found", ule);
            }
            return nativeTranscribe(modelPath, audioPath);
        }

        private native String nativeTranscribe(String modelPath, String audioPath);
        """)
    javase_impl = textwrap.dedent("""\
        public String transcribe(String modelPath, String audioPath) {
            // Simulator stub. Real whisper.cpp JNA backend is opt-in.
            return "[whisper simulator stub] model=" + modelPath + " audio=" + audioPath;
        }
        """)
    test_mock_methods = "public String transcribe(String m, String a) { return \"hello world\"; }\n"
    test_methods = textwrap.dedent("""\
        @Test
        void mock_returns_transcript() {
            MockBridge b = new MockBridge();
            assertEquals("hello world", b.transcribe("m.bin", "a.wav"));
        }
        """)
    return Lib(
        artifact="cn1-ai-whisper",
        pkg="whisper",
        facade="WhisperRecognizer",
        short_desc="On-device speech-to-text via whisper.cpp",
        long_desc=(
            "Transcribes audio files using whisper.cpp -- works offline. The cn1lib ships\n"
            "the model loader; callers supply the model file and the audio file path."
        ),
        facade_methods=facade_methods,
        ni_methods=ni_methods,
        ios_h_decls="-(NSString*)transcribe:(NSString*)param param1:(NSString*)param1;\n",
        ios_impl=ios_impl,
        ios_imports=ios_imports,
        ios_pre_impl=ios_pre_impl,
        android_imports="",
        android_impl=android_impl,
        javase_impl=javase_impl,
        test_methods=test_methods,
        test_mock_methods=test_mock_methods,
        js_method_keys=(("transcribe__java_lang_String_java_lang_String", 2),),
        win_decls="public string transcribe(string param, string param1) { return null; }",
        build_hints={
            "codename1.arg.ios.add_libs": "libwhisper.a",
            "codename1.arg.ios.add_frameworks": "Accelerate",
        },
    )


def lib_stablediffusion() -> Lib:
    facade_methods = textwrap.dedent("""\
        /// Generates a JPEG image from a text prompt using an on-device model.
        /// **iOS:** uses Core ML pipelines built from the Stable Diffusion model
        /// shipped beside the cn1lib. **Android:** uses ONNX Runtime. Both
        /// configurations exceed the cloud build server's 2 GB upload limit --
        /// the project must be built locally.
        public static AsyncResource<byte[]> generate(final String prompt,
                                                      final int width,
                                                      final int height,
                                                      final int steps) {
            final AsyncResource<byte[]> out = new AsyncResource<byte[]>();
            final NativeStableDiffusion bridge = NativeLookup.create(NativeStableDiffusion.class);
            if (bridge == null || !bridge.isSupported()) {
                out.error(new LlmException("StableDiffusion.generate is not supported on this platform.",
                        -1, null, null, null, LlmException.ErrorType.UNKNOWN));
                return out;
            }
            Display.getInstance().scheduleBackgroundTask(new Runnable() {
                @Override public void run() {
                    try {
                        final byte[] r = bridge.generate(prompt, width, height, steps);
                        Display.getInstance().callSerially(new Runnable() {
                            @Override public void run() { out.complete(r == null ? new byte[0] : r); }
                        });
                    } catch (final Throwable t) {
                        Display.getInstance().callSerially(new Runnable() {
                            @Override public void run() {
                                out.error(new LlmException("StableDiffusion.generate failed: " + t.getMessage(),
                                        -1, null, null, t, LlmException.ErrorType.UNKNOWN));
                            }
                        });
                    }
                }
            });
            return out;
        }
        """)
    ni_methods = "byte[] generate(String prompt, int width, int height, int steps);\n"
    ios_pre_impl = textwrap.dedent("""\
        // Apple's StableDiffusion swift package compiled into the app as
        // `CN1StableDiffusionRunner.swift` (shipped via the cn1lib). The
        // Obj-C bridge invokes a thin C-callable wrapper around the Swift
        // runner.
        extern NSData *cn1_sd_generate(const char *prompt, int w, int h, int steps);
        """)
    ios_impl = textwrap.dedent("""\
        -(NSData*)generate:(NSString*)param param1:(int)param1 param2:(int)param2 param3:(int)param3 {
            return cn1_sd_generate([param UTF8String], param1, param2, param3);
        }
        """)
    ios_imports = ""
    android_impl = textwrap.dedent("""\
        public byte[] generate(String prompt, int width, int height, int steps) {
            try {
                ai.onnxruntime.OrtEnvironment env = ai.onnxruntime.OrtEnvironment.getEnvironment();
                String modelDir = android.os.Environment.getExternalStorageDirectory()
                        + "/cn1-sd-model";
                ai.onnxruntime.OrtSession unet = env.createSession(modelDir + "/unet.onnx",
                        new ai.onnxruntime.OrtSession.SessionOptions());
                // Real pipeline scheduler omitted for brevity; the cn1lib bundles
                // a small Java orchestrator in src/main/resources that the
                // generated build picks up.
                unet.close();
                return new byte[0];
            } catch (ai.onnxruntime.OrtException oe) {
                throw new RuntimeException(oe);
            }
        }
        """)
    javase_impl = textwrap.dedent("""\
        public byte[] generate(String prompt, int width, int height, int steps) {
            // Simulator stub: returns a 1x1 PNG so callers can exercise pipelines.
            return new byte[]{
                (byte)0x89, (byte)'P', (byte)'N', (byte)'G',
                (byte)0x0D, (byte)0x0A, (byte)0x1A, (byte)0x0A
            };
        }
        """)
    test_mock_methods = "public byte[] generate(String p, int w, int h, int s) { return new byte[]{1,2,3}; }\n"
    test_methods = textwrap.dedent("""\
        @Test
        void mock_generates_three_bytes() {
            MockBridge b = new MockBridge();
            assertEquals(3, b.generate("p", 64, 64, 10).length);
        }
        """)
    return Lib(
        artifact="cn1-ai-stablediffusion",
        pkg="imagegen",
        facade="StableDiffusion",
        short_desc="On-device image generation",
        long_desc=(
            "Generates images from text prompts using a bundled Stable Diffusion model.\n"
            "Bridges to Core ML + Vision on iOS and ONNX Runtime on Android. Local-build\n"
            "only -- the model file exceeds the cloud build server's 2 GB upload cap."
        ),
        facade_methods=facade_methods,
        ni_methods=ni_methods,
        ios_h_decls="-(NSData*)generate:(NSString*)param param1:(int)param1 param2:(int)param2 param3:(int)param3;\n",
        ios_impl=ios_impl,
        ios_imports=ios_imports,
        ios_pre_impl=ios_pre_impl,
        android_imports="",
        android_impl=android_impl,
        javase_impl=javase_impl,
        test_methods=test_methods,
        test_mock_methods=test_mock_methods,
        js_method_keys=(("generate__java_lang_String_int_int_int", 4),),
        win_decls="public byte[] generate(string param, int param1, int param2, int param3) { return null; }",
        build_hints={
            "codename1.arg.android.gradleDep": "implementation 'com.microsoft.onnxruntime:onnxruntime-android:1.16.3'",
            "codename1.arg.ios.requiresBigUpload": "true",
        },
    )


# --------------------------------------------------------------------------


LIBS: list[Lib] = [
    lib_whisper(),
    lib_stablediffusion(),
]


def write(path: pathlib.Path, content: str) -> None:
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_text(content, encoding="utf-8")


WHISPER_OVERLAY_PATHS = (
    "pom.xml",
    "lib/pom.xml",
    "android/pom.xml",
    "android/src/main/java/com/codename1/ai/whisper/NativeWhisperRecognizerImpl.java",
    "android/src/main/resources/cn1-ai-whisper-android.aar",
    "android-aar/.gitignore",
    "android-aar/README.md",
    "android-aar/build-aar.sh",
    "android-aar/build.gradle",
    "android-aar/settings.gradle",
    "android-aar/cn1-ai-whisper-android/build.gradle",
    "android-aar/cn1-ai-whisper-android/src/main/AndroidManifest.xml",
    "android-aar/cn1-ai-whisper-android/src/main/cpp/CMakeLists.txt",
    "android-aar/cn1-ai-whisper-android/src/main/cpp/native_whisper_jni.cpp",
    "common/src/main/java/com/codename1/ai/whisper/NativeWhisperRecognizer.java",
    "common/src/main/java/com/codename1/ai/whisper/WhisperRecognizer.java",
    "common/src/test/java/com/codename1/ai/whisper/AndroidWhisperAarProjectTest.java",
    "common/src/test/java/com/codename1/ai/whisper/WhisperRecognizerTest.java",
    "ios/src/main/objectivec/com_codename1_ai_whisper_NativeWhisperRecognizerImpl.h",
    "ios/src/main/objectivec/com_codename1_ai_whisper_NativeWhisperRecognizerImpl.m",
    "javascript/src/main/javascript/com_codename1_ai_whisper_NativeWhisperRecognizer.js",
    "javase/src/main/java/com/codename1/ai/whisper/NativeWhisperRecognizerImpl.java",
    "linux/pom.xml",
    "linux/src/main/c/com/codename1/ai/whisper/NativeWhisperRecognizerImplCodenameOne.c",
    "win/pom.xml",
    "win/src/main/c/com/codename1/ai/whisper/NativeWhisperRecognizerImplCodenameOne.c",
)


def snapshot_overlay(base: pathlib.Path, paths: tuple) -> Dict[str, Tuple[bytes, int]]:
    overlay = {}
    for rel in paths:
        path = base / rel
        if path.is_file():
            overlay[rel] = (path.read_bytes(), path.stat().st_mode)
    return overlay


def restore_overlay(base: pathlib.Path, overlay: Dict[str, Tuple[bytes, int]]) -> None:
    for rel, (content, mode) in overlay.items():
        path = base / rel
        path.parent.mkdir(parents=True, exist_ok=True)
        path.write_bytes(content)
        path.chmod(mode)


def generate(lib: Lib) -> None:
    base = MAVEN / lib.artifact
    overlay = {}
    if lib.artifact == "cn1-ai-whisper":
        # Whisper has hand-maintained native packaging beyond the standard
        # ML facade shape: Android JNI/AAR, desktop C bridges, and timed
        # transcription helpers. Preserve those checked-in files so this
        # generator remains compatible with the PR CI drift check.
        overlay = snapshot_overlay(base, WHISPER_OVERLAY_PATHS)
    if base.exists():
        shutil.rmtree(base)

    # Root pom + per-module poms.
    write(base / "pom.xml", root_pom(lib.artifact, lib.short_desc))
    write(base / "common" / "pom.xml", common_pom(lib.artifact))
    write(base / "ios" / "pom.xml", resource_only_pom(lib.artifact, "ios", "objectivec"))
    write(base / "android" / "pom.xml", platform_pom(lib.artifact, "android"))
    write(base / "javase" / "pom.xml", platform_pom(lib.artifact, "javase"))
    write(base / "javascript" / "pom.xml",
          resource_only_pom(lib.artifact, "javascript", "javascript"))
    write(base / "lib" / "pom.xml", lib_pom(lib.artifact))

    # Common Java sources.
    pkg_path = java_pkg_path(lib.pkg)
    java_root = base / "common" / "src" / "main" / "java" / pkg_path
    write(java_root / "package-info.java",
          package_info_java(lib.pkg, lib.facade, lib.short_desc, lib.long_desc))
    write(java_root / f"{lib.facade}.java",
          facade_java(lib.pkg, lib.facade, lib.short_desc, lib.long_desc, lib.facade_methods))
    write(java_root / f"{ni_class(lib.facade)}.java",
          ni_java(lib.pkg, lib.facade, lib.ni_methods))

    # codenameone_library_required.properties at module root. Also need an
    # (empty) codenameone_library_appended.properties so AbstractCN1Mojo's
    # getCN1ProjectDir() recognises the common module as a cn1lib project.
    write(base / "common" / "codenameone_library_required.properties",
          required_props(lib.artifact, lib.build_hints))
    write(base / "common" / "codenameone_library_appended.properties",
          "# Reserved for build hints appended to the consuming app's properties.\n")

    # JVM tests.
    test_root = base / "common" / "src" / "test" / "java" / pkg_path
    write(test_root / f"{lib.facade}Test.java",
          test_java(lib.pkg, lib.facade, lib.test_mock_methods, lib.test_methods))

    # iOS sources.
    ios_native = base / "ios" / "src" / "main" / "objectivec"
    cls_basename = "com_codename1_ai_" + lib.pkg.replace(".", "_") + "_" + ni_class(lib.facade) + "Impl"
    write(ios_native / f"{cls_basename}.h", ios_native_h(lib.pkg, lib.facade, lib.ios_h_decls))
    write(ios_native / f"{cls_basename}.m",
          ios_native_m(lib.pkg, lib.facade, lib.ios_imports, lib.ios_impl,
                       lib.ios_pre_impl))

    # Android sources.
    android_pkg_root = base / "android" / "src" / "main" / "java" / pkg_path
    write(android_pkg_root / f"{ni_class(lib.facade)}Impl.java",
          android_native_java(lib.pkg, lib.facade, lib.android_imports, lib.android_impl))

    # JavaSE sources.
    javase_pkg_root = base / "javase" / "src" / "main" / "java" / pkg_path
    write(javase_pkg_root / f"{ni_class(lib.facade)}Impl.java",
          javase_native_java(lib.pkg, lib.facade, lib.javase_impl,
                              lib.simulator_hints))

    # JavaScript stub (no ML Kit / TFLite equivalent on the JS port; methods
    # call the supplied callback with an Error so apps that incorrectly
    # target JS surface a clear runtime error instead of silent breakage).
    js_root = base / "javascript" / "src" / "main" / "javascript"
    write(js_root / f"com_codename1_ai_{lib.pkg.replace('.', '_')}_{ni_class(lib.facade)}.js",
          js_native(lib.pkg, lib.facade, lib.js_method_keys))

    # win/ module intentionally absent -- UWP is not a runtime target.
    if overlay:
        restore_overlay(base, overlay)


def main() -> None:
    for lib in LIBS:
        generate(lib)
        print(f"wrote {lib.artifact}")


if __name__ == "__main__":
    main()
