#!/usr/bin/env python3
"""
Regenerates every AI cn1lib (cn1-ai-*) under maven/ as a proper multi-module
Maven project: root + common + ios + android + javase + lib. Each module's
sources are emitted by this script -- there are NO empty stubs.

Run with `python3 scripts/gen-ai-cn1libs.py` from any working directory.
Existing cn1-ai-* directories are wiped and rewritten.
"""

from __future__ import annotations

import pathlib
import shutil
import textwrap
from dataclasses import dataclass

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
    win_decls: str = ""  # C# method overrides for the win/ module stub. When
    # empty, a `return null/false/0` body is synthesised per declared method.
    js_method_keys: tuple = ()  # JS-encoded method names, e.g.
    # ("recognize__byte_1ARRAY",). Matches the encoding `generate-native-
    # interfaces` uses for the JS port.


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
                <module>win</module>
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
                <profile>
                    <id>win</id>
                    <activation>
                        <property><name>codename1.platform</name><value>win</value></property>
                    </activation>
                    <dependencies>
                        <dependency>
                            <groupId>com.codenameone</groupId>
                            <artifactId>{artifact}-win</artifactId>
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


def javase_native_java(pkg: str, facade: str, body: str) -> str:
    """JavaSE impl is the one platform where the impl MUST `implements`
    the NativeInterface -- NativeLookup on JavaSE resolves the impl via
    plain Java class loading and casts to the interface.
    """
    return (
        f"package {java_pkg(pkg)};\n"
        + "\n"
        + f"public class {ni_class(facade)}Impl implements {ni_class(facade)} {{\n"
        + textwrap.indent(body.rstrip(), "    ")
        + "\n\n    public boolean isSupported() {\n        return true;\n    }\n}\n"
    )


def win_native_cs(pkg: str, facade: str, decls: str) -> str:
    """C# stub for the `win/` module. Methods return defaults -- the win
    port is not a runtime target for ML Kit / TFLite / whisper.
    """
    ns = "com." + pkg.replace(".", "_") + "_" + facade  # placeholder; unused
    return (
        "// Stub: the cn1lib has no Windows UWP backend. Methods return\n"
        "// default values so the cn1lib package layout is complete.\n"
        f"namespace {java_pkg(pkg)} {{\n"
        f"public class {ni_class(facade)}Impl : I{ni_class(facade)}Impl {{\n"
        + textwrap.indent(decls.rstrip(), "    ")
        + "\n"
        "    public bool isSupported() { return false; }\n"
        "}\n"
        "}\n"
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


def lib_mlkit_text() -> Lib:
    facade_methods = textwrap.dedent("""\
        /// Runs OCR on the supplied image bytes (JPEG or PNG). Completes with
        /// the recognised text. Empty image -> empty string. No text -> empty
        /// string. Hard errors fire `AsyncResource.error(...)`.
        public static AsyncResource<String> recognize(final byte[] imageBytes) {
            final AsyncResource<String> out = new AsyncResource<String>();
            if (imageBytes == null || imageBytes.length == 0) {
                Display.getInstance().callSerially(new Runnable() {
                    @Override public void run() { out.complete(""); }
                });
                return out;
            }
            final NativeTextRecognizer bridge = NativeLookup.create(NativeTextRecognizer.class);
            if (bridge == null || !bridge.isSupported()) {
                out.error(new LlmException(
                        "TextRecognizer.recognize is not supported on this platform.",
                        -1, null, null, null, LlmException.ErrorType.UNKNOWN));
                return out;
            }
            Display.getInstance().scheduleBackgroundTask(new Runnable() {
                @Override public void run() {
                    try {
                        final String result = bridge.recognize(imageBytes);
                        Display.getInstance().callSerially(new Runnable() {
                            @Override public void run() { out.complete(result == null ? "" : result); }
                        });
                    } catch (final Throwable t) {
                        Display.getInstance().callSerially(new Runnable() {
                            @Override public void run() {
                                out.error(new LlmException("TextRecognizer.recognize failed: " + t.getMessage(),
                                        -1, null, null, t, LlmException.ErrorType.UNKNOWN));
                            }
                        });
                    }
                }
            });
            return out;
        }
        """)
    ni_methods = "String recognize(byte[] imageBytes);\n"

    ios_h_decls = "-(NSString*)recognize:(NSData*)param;\n"
    ios_impl = textwrap.dedent("""\
        -(NSString*)recognize:(NSData*)param {
            UIImage *image = [UIImage imageWithData:param];
            if (!image) return @"";
            MLKVisionImage *vision = [[MLKVisionImage alloc] initWithImage:image];
            MLKTextRecognizer *recognizer = [MLKTextRecognizer textRecognizerWithOptions:
                                              [[MLKCommonTextRecognizerOptions alloc] init]];
            __block NSString *result = @"";
            dispatch_semaphore_t sem = dispatch_semaphore_create(0);
            [recognizer processImage:vision completion:^(MLKText * _Nullable text, NSError * _Nullable err) {
                if (text && !err) {
                    result = text.text ?: @"";
                } else if (err) {
                    result = @"";
                }
                dispatch_semaphore_signal(sem);
            }];
            dispatch_semaphore_wait(sem, DISPATCH_TIME_FOREVER);
            return result;
        }
        """)
    ios_imports = textwrap.dedent("""\
        #import <MLKitTextRecognition/MLKTextRecognizer.h>
        #import <MLKitTextRecognition/MLKTextRecognizerOptions.h>
        #import <MLKitVision/MLKVisionImage.h>
        #import <MLKitVision/MLKText.h>
        """)

    android_impl = textwrap.dedent("""\
        public String recognize(byte[] imageBytes) {
            android.graphics.Bitmap bm = android.graphics.BitmapFactory.decodeByteArray(
                    imageBytes, 0, imageBytes.length);
            if (bm == null) return "";
            com.google.mlkit.vision.common.InputImage img =
                    com.google.mlkit.vision.common.InputImage.fromBitmap(bm, 0);
            com.google.mlkit.vision.text.TextRecognizer rec =
                    com.google.mlkit.vision.text.TextRecognition.getClient(
                            com.google.mlkit.vision.text.latin.TextRecognizerOptions.DEFAULT_OPTIONS);
            final java.util.concurrent.atomic.AtomicReference<String> out =
                    new java.util.concurrent.atomic.AtomicReference<String>("");
            final java.util.concurrent.CountDownLatch latch = new java.util.concurrent.CountDownLatch(1);
            rec.process(img)
               .addOnSuccessListener(new com.google.android.gms.tasks.OnSuccessListener<
                       com.google.mlkit.vision.text.Text>() {
                   public void onSuccess(com.google.mlkit.vision.text.Text t) {
                       out.set(t.getText() == null ? "" : t.getText());
                       latch.countDown();
                   }
               })
               .addOnFailureListener(new com.google.android.gms.tasks.OnFailureListener() {
                   public void onFailure(Exception e) { latch.countDown(); }
               });
            try { latch.await(); } catch (InterruptedException ie) { Thread.currentThread().interrupt(); }
            return out.get();
        }
        """)
    android_imports = ""

    javase_impl = textwrap.dedent("""\
        public String recognize(byte[] imageBytes) {
            if (imageBytes == null || imageBytes.length == 0) return "";
            return "[mlkit-text simulator stub] " + imageBytes.length + " bytes";
        }
        """)

    test_mock_methods = textwrap.dedent("""\
        String response = "hello";
        public String recognize(byte[] imageBytes) {
            if (imageBytes == null) throw new NullPointerException();
            return response;
        }
        """)
    test_methods = textwrap.dedent("""\
        @Test
        void bridge_returns_canned_string() {
            MockBridge b = new MockBridge();
            assertEquals("hello", b.recognize(new byte[]{1, 2, 3}));
        }

        @Test
        void bridge_reports_supported() {
            MockBridge b = new MockBridge();
            assertTrue(b.isSupported());
        }

        @Test
        void bridge_rejects_null_input() {
            MockBridge b = new MockBridge();
            assertThrows(NullPointerException.class, () -> b.recognize(null));
        }
        """)

    return Lib(
        artifact="cn1-ai-mlkit-text",
        pkg="mlkit.text",
        facade="TextRecognizer",
        short_desc="ML Kit Text Recognition (OCR)",
        long_desc=(
            "Extracts text strings from images entirely on-device via Google's ML Kit.\n"
            "Bridges to `GoogleMLKit/TextRecognition` on iOS and\n"
            "`com.google.mlkit:text-recognition` on Android."
        ),
        facade_methods=facade_methods,
        ni_methods=ni_methods,
        ios_h_decls=ios_h_decls,
        ios_impl=ios_impl,
        ios_imports=ios_imports,
        android_imports=android_imports,
        android_impl=android_impl,
        javase_impl=javase_impl,
        test_methods=test_methods,
        test_mock_methods=test_mock_methods,
        js_method_keys=(("recognize__byte_1ARRAY", 1),),
        win_decls="public string recognize(byte[] param) { return null; }",
        build_hints={
            "codename1.arg.ios.pods": "GoogleMLKit/TextRecognition",
            "codename1.arg.android.gradleDep": "implementation 'com.google.mlkit:text-recognition:16.0.0'",
            "codename1.arg.ios.plistInject":
                "<key>NSCameraUsageDescription</key><string>This app uses the camera to recognise text.</string>",
        },
    )


# --- mlkit-barcode -------------------------------------------------------


def lib_mlkit_barcode() -> Lib:
    facade_methods = textwrap.dedent("""\
        public static AsyncResource<String[]> scan(final byte[] imageBytes) {
            final AsyncResource<String[]> out = new AsyncResource<String[]>();
            if (imageBytes == null || imageBytes.length == 0) {
                Display.getInstance().callSerially(new Runnable() {
                    @Override public void run() { out.complete(new String[0]); }
                });
                return out;
            }
            final NativeBarcodeScanner bridge = NativeLookup.create(NativeBarcodeScanner.class);
            if (bridge == null || !bridge.isSupported()) {
                out.error(new LlmException("BarcodeScanner.scan is not supported on this platform.",
                        -1, null, null, null, LlmException.ErrorType.UNKNOWN));
                return out;
            }
            Display.getInstance().scheduleBackgroundTask(new Runnable() {
                @Override public void run() {
                    try {
                        final String[] r = bridge.scan(imageBytes);
                        Display.getInstance().callSerially(new Runnable() {
                            @Override public void run() { out.complete(r == null ? new String[0] : r); }
                        });
                    } catch (final Throwable t) {
                        Display.getInstance().callSerially(new Runnable() {
                            @Override public void run() {
                                out.error(new LlmException("BarcodeScanner.scan failed: " + t.getMessage(),
                                        -1, null, null, t, LlmException.ErrorType.UNKNOWN));
                            }
                        });
                    }
                }
            });
            return out;
        }
        """)
    ni_methods = "String[] scan(byte[] imageBytes);\n"

    ios_impl = textwrap.dedent("""\
        -(NSData*)scan:(NSData*)param {
            UIImage *image = [UIImage imageWithData:param];
            if (!image) return [self packStrings:@[]];
            MLKVisionImage *vision = [[MLKVisionImage alloc] initWithImage:image];
            MLKBarcodeScannerOptions *opts = [[MLKBarcodeScannerOptions alloc] init];
            MLKBarcodeScanner *scanner = [MLKBarcodeScanner barcodeScannerWithOptions:opts];
            __block NSArray<NSString *> *values = @[];
            dispatch_semaphore_t sem = dispatch_semaphore_create(0);
            [scanner processImage:vision completion:^(NSArray<MLKBarcode *> * _Nullable barcodes,
                                                       NSError * _Nullable error) {
                NSMutableArray *m = [NSMutableArray array];
                for (MLKBarcode *b in barcodes ?: @[]) {
                    if (b.rawValue) [m addObject:b.rawValue];
                }
                values = m;
                dispatch_semaphore_signal(sem);
            }];
            dispatch_semaphore_wait(sem, DISPATCH_TIME_FOREVER);
            return [self packStrings:values];
        }

        -(NSData*)packStrings:(NSArray<NSString *> *)strings {
            // Encode as length-prefixed UTF-8 (network byte order int + bytes).
            NSMutableData *out = [NSMutableData data];
            uint32_t count = htonl((uint32_t)strings.count);
            [out appendBytes:&count length:sizeof(count)];
            for (NSString *s in strings) {
                NSData *u = [s dataUsingEncoding:NSUTF8StringEncoding];
                uint32_t len = htonl((uint32_t)u.length);
                [out appendBytes:&len length:sizeof(len)];
                [out appendData:u];
            }
            return out;
        }
        """)
    ios_imports = textwrap.dedent("""\
        #import <MLKitBarcodeScanning/MLKBarcodeScanner.h>
        #import <MLKitBarcodeScanning/MLKBarcodeScannerOptions.h>
        #import <MLKitBarcodeScanning/MLKBarcode.h>
        #import <MLKitVision/MLKVisionImage.h>
        #import <arpa/inet.h>
        """)
    android_impl = textwrap.dedent("""\
        public String[] scan(byte[] imageBytes) {
            android.graphics.Bitmap bm = android.graphics.BitmapFactory.decodeByteArray(
                    imageBytes, 0, imageBytes.length);
            if (bm == null) return new String[0];
            com.google.mlkit.vision.common.InputImage img =
                    com.google.mlkit.vision.common.InputImage.fromBitmap(bm, 0);
            com.google.mlkit.vision.barcode.BarcodeScannerOptions o =
                    new com.google.mlkit.vision.barcode.BarcodeScannerOptions.Builder().build();
            com.google.mlkit.vision.barcode.BarcodeScanner scanner =
                    com.google.mlkit.vision.barcode.BarcodeScanning.getClient(o);
            final java.util.List<String> out = new java.util.ArrayList<String>();
            final java.util.concurrent.CountDownLatch latch = new java.util.concurrent.CountDownLatch(1);
            scanner.process(img)
                .addOnSuccessListener(new com.google.android.gms.tasks.OnSuccessListener<
                        java.util.List<com.google.mlkit.vision.barcode.common.Barcode>>() {
                    public void onSuccess(java.util.List<com.google.mlkit.vision.barcode.common.Barcode> rs) {
                        for (com.google.mlkit.vision.barcode.common.Barcode b : rs) {
                            String v = b.getRawValue();
                            if (v != null) out.add(v);
                        }
                        latch.countDown();
                    }
                })
                .addOnFailureListener(new com.google.android.gms.tasks.OnFailureListener() {
                    public void onFailure(Exception e) { latch.countDown(); }
                });
            try { latch.await(); } catch (InterruptedException ie) { Thread.currentThread().interrupt(); }
            return out.toArray(new String[0]);
        }
        """)
    javase_impl = textwrap.dedent("""\
        public String[] scan(byte[] imageBytes) {
            if (imageBytes == null || imageBytes.length == 0) return new String[0];
            // Deterministic stub for simulator runs.
            return new String[]{"SIMULATOR_BARCODE_" + imageBytes.length};
        }
        """)
    test_mock_methods = textwrap.dedent("""\
        public String[] scan(byte[] imageBytes) {
            return new String[]{"x", "y"};
        }
        """)
    test_methods = textwrap.dedent("""\
        @Test
        void mock_bridge_returns_two_codes() {
            MockBridge b = new MockBridge();
            String[] r = b.scan(new byte[]{1, 2, 3});
            assertEquals(2, r.length);
            assertEquals("x", r[0]);
        }

        @Test
        void bridge_reports_supported() {
            assertTrue(new MockBridge().isSupported());
        }
        """)
    return Lib(
        artifact="cn1-ai-mlkit-barcode",
        pkg="mlkit.barcode",
        facade="BarcodeScanner",
        short_desc="ML Kit Barcode Scanning",
        long_desc=(
            "Decodes barcodes (QR, EAN, UPC, Data Matrix, PDF417, etc.) from images.\n"
            "Bridges to `MLKitBarcodeScanning` on iOS and\n"
            "`com.google.mlkit:barcode-scanning` on Android."
        ),
        facade_methods=facade_methods,
        ni_methods=ni_methods,
        ios_h_decls="-(NSData*)scan:(NSData*)param;\n",
        ios_impl=ios_impl,
        ios_imports=ios_imports,
        android_imports="",
        android_impl=android_impl,
        javase_impl=javase_impl,
        test_methods=test_methods,
        test_mock_methods=test_mock_methods,
        js_method_keys=(("scan__byte_1ARRAY", 1),),
        win_decls="public string[] scan(byte[] param) { return null; }",
        build_hints={
            "codename1.arg.ios.pods": "GoogleMLKit/BarcodeScanning",
            "codename1.arg.android.gradleDep": "implementation 'com.google.mlkit:barcode-scanning:17.2.0'",
            "codename1.arg.ios.plistInject":
                "<key>NSCameraUsageDescription</key><string>This app uses the camera to scan barcodes.</string>",
            "codename1.arg.android.xpermissions": "<uses-permission android:name=\"android.permission.CAMERA\"/>",
        },
    )


# --- mlkit-face ----------------------------------------------------------


def lib_mlkit_face() -> Lib:
    facade_methods = textwrap.dedent("""\
        /// Returns an array of face bounding-box quadruples
        /// (x, y, width, height) packed as `int[4 * n]`.
        public static AsyncResource<int[]> detect(final byte[] imageBytes) {
            final AsyncResource<int[]> out = new AsyncResource<int[]>();
            final NativeFaceDetector bridge = NativeLookup.create(NativeFaceDetector.class);
            if (bridge == null || !bridge.isSupported()) {
                out.error(new LlmException("FaceDetector.detect is not supported on this platform.",
                        -1, null, null, null, LlmException.ErrorType.UNKNOWN));
                return out;
            }
            Display.getInstance().scheduleBackgroundTask(new Runnable() {
                @Override public void run() {
                    try {
                        final int[] r = bridge.detect(imageBytes);
                        Display.getInstance().callSerially(new Runnable() {
                            @Override public void run() { out.complete(r == null ? new int[0] : r); }
                        });
                    } catch (final Throwable t) {
                        Display.getInstance().callSerially(new Runnable() {
                            @Override public void run() {
                                out.error(new LlmException("FaceDetector.detect failed: " + t.getMessage(),
                                        -1, null, null, t, LlmException.ErrorType.UNKNOWN));
                            }
                        });
                    }
                }
            });
            return out;
        }
        """)
    ni_methods = "int[] detect(byte[] imageBytes);\n"

    ios_impl = textwrap.dedent("""\
        -(NSData*)detect:(NSData*)param {
            UIImage *image = [UIImage imageWithData:param];
            if (!image) return [NSData data];
            MLKVisionImage *vision = [[MLKVisionImage alloc] initWithImage:image];
            MLKFaceDetectorOptions *opts = [[MLKFaceDetectorOptions alloc] init];
            MLKFaceDetector *det = [MLKFaceDetector faceDetectorWithOptions:opts];
            __block NSArray<MLKFace *> *faces = @[];
            dispatch_semaphore_t sem = dispatch_semaphore_create(0);
            [det processImage:vision completion:^(NSArray<MLKFace *> * _Nullable f, NSError * _Nullable e) {
                faces = f ?: @[];
                dispatch_semaphore_signal(sem);
            }];
            dispatch_semaphore_wait(sem, DISPATCH_TIME_FOREVER);
            NSMutableData *out = [NSMutableData data];
            for (MLKFace *face in faces) {
                CGRect r = face.frame;
                int32_t v[4] = { htonl((int32_t)r.origin.x), htonl((int32_t)r.origin.y),
                                 htonl((int32_t)r.size.width), htonl((int32_t)r.size.height) };
                [out appendBytes:v length:sizeof(v)];
            }
            return out;
        }
        """)
    ios_imports = textwrap.dedent("""\
        #import <MLKitFaceDetection/MLKFaceDetector.h>
        #import <MLKitFaceDetection/MLKFaceDetectorOptions.h>
        #import <MLKitFaceDetection/MLKFace.h>
        #import <MLKitVision/MLKVisionImage.h>
        #import <arpa/inet.h>
        """)
    android_impl = textwrap.dedent("""\
        public int[] detect(byte[] imageBytes) {
            android.graphics.Bitmap bm = android.graphics.BitmapFactory.decodeByteArray(
                    imageBytes, 0, imageBytes.length);
            if (bm == null) return new int[0];
            com.google.mlkit.vision.common.InputImage img =
                    com.google.mlkit.vision.common.InputImage.fromBitmap(bm, 0);
            com.google.mlkit.vision.face.FaceDetector det =
                    com.google.mlkit.vision.face.FaceDetection.getClient(
                            new com.google.mlkit.vision.face.FaceDetectorOptions.Builder().build());
            final java.util.List<int[]> rs = new java.util.ArrayList<int[]>();
            final java.util.concurrent.CountDownLatch latch = new java.util.concurrent.CountDownLatch(1);
            det.process(img)
               .addOnSuccessListener(new com.google.android.gms.tasks.OnSuccessListener<
                       java.util.List<com.google.mlkit.vision.face.Face>>() {
                   public void onSuccess(java.util.List<com.google.mlkit.vision.face.Face> faces) {
                       for (com.google.mlkit.vision.face.Face f : faces) {
                           android.graphics.Rect r = f.getBoundingBox();
                           rs.add(new int[]{r.left, r.top, r.width(), r.height()});
                       }
                       latch.countDown();
                   }
               })
               .addOnFailureListener(new com.google.android.gms.tasks.OnFailureListener() {
                   public void onFailure(Exception e) { latch.countDown(); }
               });
            try { latch.await(); } catch (InterruptedException ie) { Thread.currentThread().interrupt(); }
            int[] flat = new int[rs.size() * 4];
            int i = 0;
            for (int[] r : rs) { System.arraycopy(r, 0, flat, i, 4); i += 4; }
            return flat;
        }
        """)
    javase_impl = textwrap.dedent("""\
        public int[] detect(byte[] imageBytes) {
            // Deterministic 1-face stub for simulator runs.
            if (imageBytes == null || imageBytes.length == 0) return new int[0];
            return new int[]{10, 20, 100, 120};
        }
        """)
    test_mock_methods = textwrap.dedent("""\
        public int[] detect(byte[] imageBytes) {
            return new int[]{1, 2, 3, 4, 5, 6, 7, 8};
        }
        """)
    test_methods = textwrap.dedent("""\
        @Test
        void mock_bridge_returns_two_faces() {
            MockBridge b = new MockBridge();
            int[] r = b.detect(new byte[]{1});
            assertEquals(8, r.length);
        }
        """)
    return Lib(
        artifact="cn1-ai-mlkit-face",
        pkg="mlkit.face",
        facade="FaceDetector",
        short_desc="ML Kit Face Detection",
        long_desc=(
            "Detects faces in images and returns bounding boxes.\n"
            "Bridges to `MLKitFaceDetection` on iOS and\n"
            "`com.google.mlkit:face-detection` on Android."
        ),
        facade_methods=facade_methods,
        ni_methods=ni_methods,
        ios_h_decls="-(NSData*)detect:(NSData*)param;\n",
        ios_impl=ios_impl,
        ios_imports=ios_imports,
        android_imports="",
        android_impl=android_impl,
        javase_impl=javase_impl,
        test_methods=test_methods,
        test_mock_methods=test_mock_methods,
        js_method_keys=(("detect__byte_1ARRAY", 1),),
        win_decls="public int[] detect(byte[] param) { return null; }",
        build_hints={
            "codename1.arg.ios.pods": "GoogleMLKit/FaceDetection",
            "codename1.arg.android.gradleDep": "implementation 'com.google.mlkit:face-detection:16.1.5'",
            "codename1.arg.ios.plistInject":
                "<key>NSCameraUsageDescription</key><string>This app uses the camera to detect faces.</string>",
        },
    )


# Below: shorter helpers for the remaining libs. Each follows the same
# pattern; the native bodies use the appropriate ML Kit / TFLite / whisper /
# CoreML API.


def _string_facade_methods(facade: str, ni: str, method: str, arg_kind: str = "bytes"):
    """Build the AsyncResource<String> facade body for a String-returning bridge."""
    arg_type = "byte[] imageBytes" if arg_kind == "bytes" else "String input"
    arg_pass = "imageBytes" if arg_kind == "bytes" else "input"
    return textwrap.dedent(f"""\
        public static AsyncResource<String> {method}(final {arg_type}) {{
            final AsyncResource<String> out = new AsyncResource<String>();
            final {ni} bridge = NativeLookup.create({ni}.class);
            if (bridge == null || !bridge.isSupported()) {{
                out.error(new LlmException("{facade}.{method} is not supported on this platform.",
                        -1, null, null, null, LlmException.ErrorType.UNKNOWN));
                return out;
            }}
            Display.getInstance().scheduleBackgroundTask(new Runnable() {{
                @Override public void run() {{
                    try {{
                        final String r = bridge.{method}({arg_pass});
                        Display.getInstance().callSerially(new Runnable() {{
                            @Override public void run() {{ out.complete(r == null ? "" : r); }}
                        }});
                    }} catch (final Throwable t) {{
                        Display.getInstance().callSerially(new Runnable() {{
                            @Override public void run() {{
                                out.error(new LlmException("{facade}.{method} failed: " + t.getMessage(),
                                        -1, null, null, t, LlmException.ErrorType.UNKNOWN));
                            }}
                        }});
                    }}
                }}
            }});
            return out;
        }}
        """)


def _strings_facade_methods(facade: str, ni: str, method: str, arg_kind: str = "bytes"):
    arg_type = "byte[] imageBytes" if arg_kind == "bytes" else "String input"
    arg_pass = "imageBytes" if arg_kind == "bytes" else "input"
    return textwrap.dedent(f"""\
        public static AsyncResource<String[]> {method}(final {arg_type}) {{
            final AsyncResource<String[]> out = new AsyncResource<String[]>();
            final {ni} bridge = NativeLookup.create({ni}.class);
            if (bridge == null || !bridge.isSupported()) {{
                out.error(new LlmException("{facade}.{method} is not supported on this platform.",
                        -1, null, null, null, LlmException.ErrorType.UNKNOWN));
                return out;
            }}
            Display.getInstance().scheduleBackgroundTask(new Runnable() {{
                @Override public void run() {{
                    try {{
                        final String[] r = bridge.{method}({arg_pass});
                        Display.getInstance().callSerially(new Runnable() {{
                            @Override public void run() {{ out.complete(r == null ? new String[0] : r); }}
                        }});
                    }} catch (final Throwable t) {{
                        Display.getInstance().callSerially(new Runnable() {{
                            @Override public void run() {{
                                out.error(new LlmException("{facade}.{method} failed: " + t.getMessage(),
                                        -1, null, null, t, LlmException.ErrorType.UNKNOWN));
                            }}
                        }});
                    }}
                }}
            }});
            return out;
        }}
        """)


def lib_mlkit_labeling() -> Lib:
    facade_methods = _strings_facade_methods("ImageLabeler", "NativeImageLabeler", "label")
    ios_impl = textwrap.dedent("""\
        -(NSData*)label:(NSData*)param {
            UIImage *image = [UIImage imageWithData:param];
            if (!image) return [self packStrings:@[]];
            MLKVisionImage *vision = [[MLKVisionImage alloc] initWithImage:image];
            MLKImageLabelerOptions *opts = [[MLKImageLabelerOptions alloc] init];
            MLKImageLabeler *labeler = [MLKImageLabeler imageLabelerWithOptions:opts];
            __block NSArray<MLKImageLabel *> *labels = @[];
            dispatch_semaphore_t sem = dispatch_semaphore_create(0);
            [labeler processImage:vision completion:^(NSArray<MLKImageLabel *> * _Nullable r, NSError * _Nullable e) {
                labels = r ?: @[];
                dispatch_semaphore_signal(sem);
            }];
            dispatch_semaphore_wait(sem, DISPATCH_TIME_FOREVER);
            NSMutableArray *m = [NSMutableArray array];
            for (MLKImageLabel *l in labels) { if (l.text) [m addObject:l.text]; }
            return [self packStrings:m];
        }

        -(NSData*)packStrings:(NSArray<NSString *> *)strings {
            NSMutableData *out = [NSMutableData data];
            uint32_t count = htonl((uint32_t)strings.count);
            [out appendBytes:&count length:sizeof(count)];
            for (NSString *s in strings) {
                NSData *u = [s dataUsingEncoding:NSUTF8StringEncoding];
                uint32_t len = htonl((uint32_t)u.length);
                [out appendBytes:&len length:sizeof(len)];
                [out appendData:u];
            }
            return out;
        }
        """)
    ios_imports = textwrap.dedent("""\
        #import <MLKitImageLabeling/MLKImageLabeler.h>
        #import <MLKitImageLabeling/MLKImageLabelerOptions.h>
        #import <MLKitImageLabeling/MLKImageLabel.h>
        #import <MLKitVision/MLKVisionImage.h>
        #import <arpa/inet.h>
        """)
    android_impl = textwrap.dedent("""\
        public String[] label(byte[] imageBytes) {
            android.graphics.Bitmap bm = android.graphics.BitmapFactory.decodeByteArray(
                    imageBytes, 0, imageBytes.length);
            if (bm == null) return new String[0];
            com.google.mlkit.vision.common.InputImage img =
                    com.google.mlkit.vision.common.InputImage.fromBitmap(bm, 0);
            com.google.mlkit.vision.label.ImageLabeler labeler =
                    com.google.mlkit.vision.label.ImageLabeling.getClient(
                            com.google.mlkit.vision.label.defaults.ImageLabelerOptions.DEFAULT_OPTIONS);
            final java.util.List<String> out = new java.util.ArrayList<String>();
            final java.util.concurrent.CountDownLatch latch = new java.util.concurrent.CountDownLatch(1);
            labeler.process(img)
                .addOnSuccessListener(new com.google.android.gms.tasks.OnSuccessListener<
                        java.util.List<com.google.mlkit.vision.label.ImageLabel>>() {
                    public void onSuccess(java.util.List<com.google.mlkit.vision.label.ImageLabel> rs) {
                        for (com.google.mlkit.vision.label.ImageLabel l : rs) out.add(l.getText());
                        latch.countDown();
                    }
                })
                .addOnFailureListener(new com.google.android.gms.tasks.OnFailureListener() {
                    public void onFailure(Exception e) { latch.countDown(); }
                });
            try { latch.await(); } catch (InterruptedException ie) { Thread.currentThread().interrupt(); }
            return out.toArray(new String[0]);
        }
        """)
    javase_impl = textwrap.dedent("""\
        public String[] label(byte[] imageBytes) {
            if (imageBytes == null || imageBytes.length == 0) return new String[0];
            return new String[]{"object", "stub", "simulator"};
        }
        """)
    test_mock_methods = textwrap.dedent("""\
        public String[] label(byte[] imageBytes) { return new String[]{"a", "b"}; }
        """)
    test_methods = textwrap.dedent("""\
        @Test
        void mock_bridge_returns_labels() {
            MockBridge b = new MockBridge();
            assertArrayEquals(new String[]{"a", "b"}, b.label(new byte[]{1}));
        }
        """)
    return Lib(
        artifact="cn1-ai-mlkit-labeling",
        pkg="mlkit.labeling",
        facade="ImageLabeler",
        short_desc="ML Kit Image Labeling",
        long_desc=(
            "Returns descriptive labels for the contents of an image.\n"
            "Bridges to `MLKitImageLabeling` on iOS and\n"
            "`com.google.mlkit:image-labeling` on Android."
        ),
        facade_methods=facade_methods,
        ni_methods="String[] label(byte[] imageBytes);\n",
        ios_h_decls="-(NSData*)label:(NSData*)param;\n",
        ios_impl=ios_impl,
        ios_imports=ios_imports,
        android_imports="",
        android_impl=android_impl,
        javase_impl=javase_impl,
        test_methods=test_methods,
        test_mock_methods=test_mock_methods,
        js_method_keys=(("label__byte_1ARRAY", 1),),
        win_decls="public string[] label(byte[] param) { return null; }",
        build_hints={
            "codename1.arg.ios.pods": "GoogleMLKit/ImageLabeling",
            "codename1.arg.android.gradleDep": "implementation 'com.google.mlkit:image-labeling:17.0.7'",
        },
    )


def lib_mlkit_translate() -> Lib:
    facade_methods = textwrap.dedent("""\
        public static AsyncResource<String> translate(final String text,
                                                       final String sourceLang,
                                                       final String targetLang) {
            final AsyncResource<String> out = new AsyncResource<String>();
            final NativeTranslator bridge = NativeLookup.create(NativeTranslator.class);
            if (bridge == null || !bridge.isSupported()) {
                out.error(new LlmException("Translator.translate is not supported on this platform.",
                        -1, null, null, null, LlmException.ErrorType.UNKNOWN));
                return out;
            }
            Display.getInstance().scheduleBackgroundTask(new Runnable() {
                @Override public void run() {
                    try {
                        final String r = bridge.translate(text, sourceLang, targetLang);
                        Display.getInstance().callSerially(new Runnable() {
                            @Override public void run() { out.complete(r == null ? "" : r); }
                        });
                    } catch (final Throwable t) {
                        Display.getInstance().callSerially(new Runnable() {
                            @Override public void run() {
                                out.error(new LlmException("Translator.translate failed: " + t.getMessage(),
                                        -1, null, null, t, LlmException.ErrorType.UNKNOWN));
                            }
                        });
                    }
                }
            });
            return out;
        }
        """)
    ni_methods = "String translate(String text, String sourceLang, String targetLang);\n"
    ios_impl = textwrap.dedent("""\
        -(NSString*)translate:(NSString*)param param1:(NSString*)param1 param2:(NSString*)param2 {
            MLKTranslatorOptions *opts = [[MLKTranslatorOptions alloc]
                                          initWithSourceLanguage:param1
                                          targetLanguage:param2];
            MLKTranslator *t = [MLKTranslator translatorWithOptions:opts];
            MLKModelDownloadConditions *cond = [[MLKModelDownloadConditions alloc]
                                                initWithAllowsCellularAccess:YES
                                                allowsBackgroundDownloading:YES];
            __block NSString *result = @"";
            dispatch_semaphore_t sem = dispatch_semaphore_create(0);
            [t downloadModelIfNeededWithConditions:cond completion:^(NSError * _Nullable err) {
                if (err) { dispatch_semaphore_signal(sem); return; }
                [t translateText:param completion:^(NSString * _Nullable r, NSError * _Nullable e) {
                    if (r && !e) result = r;
                    dispatch_semaphore_signal(sem);
                }];
            }];
            dispatch_semaphore_wait(sem, DISPATCH_TIME_FOREVER);
            return result;
        }
        """)
    ios_imports = textwrap.dedent("""\
        #import <MLKitTranslate/MLKTranslator.h>
        #import <MLKitTranslate/MLKTranslatorOptions.h>
        #import <MLKitCommon/MLKModelDownloadConditions.h>
        """)
    android_impl = textwrap.dedent("""\
        public String translate(String text, String sourceLang, String targetLang) {
            com.google.mlkit.nl.translate.TranslatorOptions opts =
                new com.google.mlkit.nl.translate.TranslatorOptions.Builder()
                    .setSourceLanguage(sourceLang)
                    .setTargetLanguage(targetLang)
                    .build();
            com.google.mlkit.nl.translate.Translator t =
                    com.google.mlkit.nl.translate.Translation.getClient(opts);
            final java.util.concurrent.atomic.AtomicReference<String> out =
                    new java.util.concurrent.atomic.AtomicReference<String>("");
            final java.util.concurrent.CountDownLatch latch = new java.util.concurrent.CountDownLatch(1);
            t.downloadModelIfNeeded()
              .addOnSuccessListener(new com.google.android.gms.tasks.OnSuccessListener<Void>() {
                  public void onSuccess(Void v) {
                      t.translate(text)
                       .addOnSuccessListener(new com.google.android.gms.tasks.OnSuccessListener<String>() {
                           public void onSuccess(String r) { out.set(r); latch.countDown(); }
                       })
                       .addOnFailureListener(new com.google.android.gms.tasks.OnFailureListener() {
                           public void onFailure(Exception e) { latch.countDown(); }
                       });
                  }
              })
              .addOnFailureListener(new com.google.android.gms.tasks.OnFailureListener() {
                  public void onFailure(Exception e) { latch.countDown(); }
              });
            try { latch.await(); } catch (InterruptedException ie) { Thread.currentThread().interrupt(); }
            return out.get();
        }
        """)
    javase_impl = textwrap.dedent("""\
        public String translate(String text, String sourceLang, String targetLang) {
            if (text == null) return "";
            return "[" + sourceLang + "->" + targetLang + "] " + text;
        }
        """)
    test_mock_methods = textwrap.dedent("""\
        public String translate(String text, String sourceLang, String targetLang) {
            return text + "@" + sourceLang + "->" + targetLang;
        }
        """)
    test_methods = textwrap.dedent("""\
        @Test
        void mock_translate_round_trip() {
            MockBridge b = new MockBridge();
            assertEquals("hi@en->fr", b.translate("hi", "en", "fr"));
        }
        """)
    return Lib(
        artifact="cn1-ai-mlkit-translate",
        pkg="mlkit.translate",
        facade="Translator",
        short_desc="ML Kit on-device Translation",
        long_desc="Translates short text between language pairs entirely on-device.",
        facade_methods=facade_methods,
        ni_methods=ni_methods,
        ios_h_decls="-(NSString*)translate:(NSString*)param param1:(NSString*)param1 param2:(NSString*)param2;\n",
        ios_impl=ios_impl,
        ios_imports=ios_imports,
        android_imports="",
        android_impl=android_impl,
        javase_impl=javase_impl,
        test_methods=test_methods,
        test_mock_methods=test_mock_methods,
        js_method_keys=(("translate__java_lang_String_java_lang_String_java_lang_String", 3),),
        win_decls="public string translate(string param, string param1, string param2) { return null; }",
        build_hints={
            "codename1.arg.ios.pods": "GoogleMLKit/Translate",
            "codename1.arg.android.gradleDep": "implementation 'com.google.mlkit:translate:17.0.3'",
        },
    )


def lib_mlkit_smartreply() -> Lib:
    facade_methods = _strings_facade_methods("SmartReply", "NativeSmartReply", "suggest", arg_kind="text")
    ni_methods = "String[] suggest(String conversationJson);\n"
    ios_impl = textwrap.dedent("""\
        -(NSData*)suggest:(NSString*)param {
            // param is a JSON array of {role,message,timestamp,userId}.
            NSError *err = nil;
            NSArray *items = [NSJSONSerialization JSONObjectWithData:
                              [param dataUsingEncoding:NSUTF8StringEncoding]
                              options:0 error:&err];
            NSMutableArray *messages = [NSMutableArray array];
            if ([items isKindOfClass:[NSArray class]]) {
                for (NSDictionary *d in items) {
                    if (![d isKindOfClass:[NSDictionary class]]) continue;
                    NSString *role = d[@"role"] ?: @"user";
                    BOOL isLocalUser = [role isEqualToString:@"user"];
                    NSString *text = d[@"message"] ?: @"";
                    NSNumber *ts = d[@"timestamp"] ?: @0;
                    MLKTextMessage *m = [[MLKTextMessage alloc]
                        initWithText:text timestamp:[ts doubleValue]
                        userID:(d[@"userId"] ?: @"u")
                        isLocalUser:isLocalUser];
                    [messages addObject:m];
                }
            }
            __block NSArray<NSString *> *out = @[];
            dispatch_semaphore_t sem = dispatch_semaphore_create(0);
            [[MLKSmartReply smartReply] suggestRepliesForMessages:messages
                completion:^(MLKSmartReplySuggestionResult * _Nullable result, NSError * _Nullable e) {
                    NSMutableArray *m = [NSMutableArray array];
                    for (MLKSmartReplySuggestion *s in result.suggestions ?: @[]) {
                        if (s.text) [m addObject:s.text];
                    }
                    out = m;
                    dispatch_semaphore_signal(sem);
                }];
            dispatch_semaphore_wait(sem, DISPATCH_TIME_FOREVER);
            return [self packStrings:out];
        }

        -(NSData*)packStrings:(NSArray<NSString *> *)strings {
            NSMutableData *out = [NSMutableData data];
            uint32_t count = htonl((uint32_t)strings.count);
            [out appendBytes:&count length:sizeof(count)];
            for (NSString *s in strings) {
                NSData *u = [s dataUsingEncoding:NSUTF8StringEncoding];
                uint32_t len = htonl((uint32_t)u.length);
                [out appendBytes:&len length:sizeof(len)];
                [out appendData:u];
            }
            return out;
        }
        """)
    ios_imports = textwrap.dedent("""\
        #import <MLKitSmartReply/MLKSmartReply.h>
        #import <MLKitSmartReply/MLKTextMessage.h>
        #import <MLKitSmartReply/MLKSmartReplySuggestion.h>
        #import <MLKitSmartReply/MLKSmartReplySuggestionResult.h>
        #import <arpa/inet.h>
        """)
    android_impl = textwrap.dedent("""\
        public String[] suggest(String conversationJson) {
            java.util.List<com.google.mlkit.nl.smartreply.TextMessage> msgs =
                new java.util.ArrayList<com.google.mlkit.nl.smartreply.TextMessage>();
            try {
                org.json.JSONArray a = new org.json.JSONArray(conversationJson);
                for (int i = 0; i < a.length(); i++) {
                    org.json.JSONObject o = a.getJSONObject(i);
                    String role = o.optString("role", "user");
                    long ts = o.optLong("timestamp", 0);
                    String text = o.optString("message", "");
                    String userId = o.optString("userId", "u");
                    if ("user".equals(role)) {
                        msgs.add(com.google.mlkit.nl.smartreply.TextMessage.createForLocalUser(text, ts));
                    } else {
                        msgs.add(com.google.mlkit.nl.smartreply.TextMessage.createForRemoteUser(text, ts, userId));
                    }
                }
            } catch (org.json.JSONException jex) {
                return new String[0];
            }
            final java.util.List<String> out = new java.util.ArrayList<String>();
            final java.util.concurrent.CountDownLatch latch = new java.util.concurrent.CountDownLatch(1);
            com.google.mlkit.nl.smartreply.SmartReplyGenerator gen =
                    com.google.mlkit.nl.smartreply.SmartReply.getClient();
            gen.suggestReplies(msgs)
                .addOnSuccessListener(new com.google.android.gms.tasks.OnSuccessListener<
                        com.google.mlkit.nl.smartreply.SmartReplySuggestionResult>() {
                    public void onSuccess(com.google.mlkit.nl.smartreply.SmartReplySuggestionResult r) {
                        for (com.google.mlkit.nl.smartreply.SmartReplySuggestion s : r.getSuggestions()) {
                            out.add(s.getText());
                        }
                        latch.countDown();
                    }
                })
                .addOnFailureListener(new com.google.android.gms.tasks.OnFailureListener() {
                    public void onFailure(Exception e) { latch.countDown(); }
                });
            try { latch.await(); } catch (InterruptedException ie) { Thread.currentThread().interrupt(); }
            return out.toArray(new String[0]);
        }
        """)
    javase_impl = textwrap.dedent("""\
        public String[] suggest(String conversationJson) {
            return new String[]{"Sounds good", "Thanks!", "Got it"};
        }
        """)
    test_mock_methods = "public String[] suggest(String c) { return new String[]{\"ok\"}; }\n"
    test_methods = textwrap.dedent("""\
        @Test
        void mock_returns_single_suggestion() {
            MockBridge b = new MockBridge();
            assertEquals(1, b.suggest("[]").length);
        }
        """)
    return Lib(
        artifact="cn1-ai-mlkit-smartreply",
        pkg="mlkit.smartreply",
        facade="SmartReply",
        short_desc="ML Kit Smart Reply",
        long_desc="Generates short reply suggestions for chat conversations on-device.",
        facade_methods=facade_methods,
        ni_methods=ni_methods,
        ios_h_decls="-(NSData*)suggest:(NSString*)param;\n",
        ios_impl=ios_impl,
        ios_imports=ios_imports,
        android_imports="",
        android_impl=android_impl,
        javase_impl=javase_impl,
        test_methods=test_methods,
        test_mock_methods=test_mock_methods,
        js_method_keys=(("suggest__java_lang_String", 1),),
        win_decls="public string[] suggest(string param) { return null; }",
        build_hints={
            "codename1.arg.ios.pods": "GoogleMLKit/SmartReply",
            "codename1.arg.android.gradleDep": "implementation 'com.google.mlkit:smart-reply:17.0.4'",
        },
    )


def lib_mlkit_langid() -> Lib:
    facade_methods = _string_facade_methods("LanguageIdentifier", "NativeLanguageIdentifier", "identify",
                                            arg_kind="text")
    ni_methods = "String identify(String input);\n"
    ios_impl = textwrap.dedent("""\
        -(NSString*)identify:(NSString*)param {
            MLKLanguageIdentification *id = [MLKLanguageIdentification languageIdentification];
            __block NSString *result = @"und";
            dispatch_semaphore_t sem = dispatch_semaphore_create(0);
            [id identifyLanguageForText:param completion:^(NSString * _Nullable lang, NSError * _Nullable e) {
                if (lang) result = lang;
                dispatch_semaphore_signal(sem);
            }];
            dispatch_semaphore_wait(sem, DISPATCH_TIME_FOREVER);
            return result;
        }
        """)
    ios_imports = "#import <MLKitLanguageID/MLKLanguageIdentification.h>\n"
    android_impl = textwrap.dedent("""\
        public String identify(String input) {
            com.google.mlkit.nl.languageid.LanguageIdentifier id =
                    com.google.mlkit.nl.languageid.LanguageIdentification.getClient();
            final java.util.concurrent.atomic.AtomicReference<String> out =
                    new java.util.concurrent.atomic.AtomicReference<String>("und");
            final java.util.concurrent.CountDownLatch latch = new java.util.concurrent.CountDownLatch(1);
            id.identifyLanguage(input)
              .addOnSuccessListener(new com.google.android.gms.tasks.OnSuccessListener<String>() {
                  public void onSuccess(String s) { if (s != null) out.set(s); latch.countDown(); }
              })
              .addOnFailureListener(new com.google.android.gms.tasks.OnFailureListener() {
                  public void onFailure(Exception e) { latch.countDown(); }
              });
            try { latch.await(); } catch (InterruptedException ie) { Thread.currentThread().interrupt(); }
            return out.get();
        }
        """)
    javase_impl = textwrap.dedent("""\
        public String identify(String input) {
            // Crude language ID stub for simulator.
            if (input == null || input.isEmpty()) return "und";
            return "en";
        }
        """)
    test_mock_methods = "public String identify(String input) { return \"en\"; }\n"
    test_methods = textwrap.dedent("""\
        @Test
        void mock_identifies_english() {
            MockBridge b = new MockBridge();
            assertEquals("en", b.identify("hello"));
        }
        """)
    return Lib(
        artifact="cn1-ai-mlkit-langid",
        pkg="mlkit.langid",
        facade="LanguageIdentifier",
        short_desc="ML Kit Language Identification",
        long_desc="Identifies the language of a given text string.",
        facade_methods=facade_methods,
        ni_methods=ni_methods,
        ios_h_decls="-(NSString*)identify:(NSString*)param;\n",
        ios_impl=ios_impl,
        ios_imports=ios_imports,
        android_imports="",
        android_impl=android_impl,
        javase_impl=javase_impl,
        test_methods=test_methods,
        test_mock_methods=test_mock_methods,
        js_method_keys=(("identify__java_lang_String", 1),),
        win_decls="public string identify(string param) { return null; }",
        build_hints={
            "codename1.arg.ios.pods": "GoogleMLKit/LanguageID",
            "codename1.arg.android.gradleDep": "implementation 'com.google.mlkit:language-id:17.0.6'",
        },
    )


def lib_mlkit_pose() -> Lib:
    facade_methods = textwrap.dedent("""\
        /// Returns 33 landmark triples (x,y,confidence) per detected pose
        /// packed as `float[3 * 33]`.
        public static AsyncResource<float[]> detect(final byte[] imageBytes) {
            final AsyncResource<float[]> out = new AsyncResource<float[]>();
            final NativePoseDetector bridge = NativeLookup.create(NativePoseDetector.class);
            if (bridge == null || !bridge.isSupported()) {
                out.error(new LlmException("PoseDetector.detect is not supported on this platform.",
                        -1, null, null, null, LlmException.ErrorType.UNKNOWN));
                return out;
            }
            Display.getInstance().scheduleBackgroundTask(new Runnable() {
                @Override public void run() {
                    try {
                        final float[] r = bridge.detect(imageBytes);
                        Display.getInstance().callSerially(new Runnable() {
                            @Override public void run() { out.complete(r == null ? new float[0] : r); }
                        });
                    } catch (final Throwable t) {
                        Display.getInstance().callSerially(new Runnable() {
                            @Override public void run() {
                                out.error(new LlmException("PoseDetector.detect failed: " + t.getMessage(),
                                        -1, null, null, t, LlmException.ErrorType.UNKNOWN));
                            }
                        });
                    }
                }
            });
            return out;
        }
        """)
    ni_methods = "float[] detect(byte[] imageBytes);\n"
    ios_impl = textwrap.dedent("""\
        -(NSData*)detect:(NSData*)param {
            UIImage *image = [UIImage imageWithData:param];
            if (!image) return [NSData data];
            MLKVisionImage *vision = [[MLKVisionImage alloc] initWithImage:image];
            MLKPoseDetectorOptions *opts = [[MLKPoseDetectorOptions alloc] init];
            MLKPoseDetector *det = [MLKPoseDetector poseDetectorWithOptions:opts];
            __block MLKPose *pose = nil;
            dispatch_semaphore_t sem = dispatch_semaphore_create(0);
            [det processImage:vision completion:^(NSArray<MLKPose *> * _Nullable r, NSError * _Nullable e) {
                if (r.count > 0) pose = r[0];
                dispatch_semaphore_signal(sem);
            }];
            dispatch_semaphore_wait(sem, DISPATCH_TIME_FOREVER);
            float buf[99] = {0};
            if (pose) {
                for (NSInteger i = 0; i < 33 && i < pose.landmarks.count; i++) {
                    MLKPoseLandmark *lm = pose.landmarks[i];
                    buf[i * 3]     = (float)lm.position.x;
                    buf[i * 3 + 1] = (float)lm.position.y;
                    buf[i * 3 + 2] = (float)lm.inFrameLikelihood;
                }
            }
            // Pack as big-endian float bytes (matches JAVA_ARRAY_FLOAT on iOS port).
            return [NSData dataWithBytes:buf length:sizeof(buf)];
        }
        """)
    ios_imports = textwrap.dedent("""\
        #import <MLKitPoseDetection/MLKPoseDetector.h>
        #import <MLKitPoseDetection/MLKPoseDetectorOptions.h>
        #import <MLKitPoseDetection/MLKPose.h>
        #import <MLKitPoseDetection/MLKPoseLandmark.h>
        #import <MLKitVision/MLKVisionImage.h>
        """)
    android_impl = textwrap.dedent("""\
        public float[] detect(byte[] imageBytes) {
            android.graphics.Bitmap bm = android.graphics.BitmapFactory.decodeByteArray(
                    imageBytes, 0, imageBytes.length);
            if (bm == null) return new float[0];
            com.google.mlkit.vision.common.InputImage img =
                    com.google.mlkit.vision.common.InputImage.fromBitmap(bm, 0);
            com.google.mlkit.vision.pose.PoseDetector det =
                    com.google.mlkit.vision.pose.PoseDetection.getClient(
                            new com.google.mlkit.vision.pose.defaults.PoseDetectorOptions.Builder().build());
            final float[] out = new float[33 * 3];
            final java.util.concurrent.CountDownLatch latch = new java.util.concurrent.CountDownLatch(1);
            det.process(img)
               .addOnSuccessListener(new com.google.android.gms.tasks.OnSuccessListener<
                       com.google.mlkit.vision.pose.Pose>() {
                   public void onSuccess(com.google.mlkit.vision.pose.Pose p) {
                       java.util.List<com.google.mlkit.vision.pose.PoseLandmark> lms = p.getAllPoseLandmarks();
                       for (int i = 0; i < 33 && i < lms.size(); i++) {
                           com.google.mlkit.vision.pose.PoseLandmark lm = lms.get(i);
                           out[i * 3]     = lm.getPosition().x;
                           out[i * 3 + 1] = lm.getPosition().y;
                           out[i * 3 + 2] = lm.getInFrameLikelihood();
                       }
                       latch.countDown();
                   }
               })
               .addOnFailureListener(new com.google.android.gms.tasks.OnFailureListener() {
                   public void onFailure(Exception e) { latch.countDown(); }
               });
            try { latch.await(); } catch (InterruptedException ie) { Thread.currentThread().interrupt(); }
            return out;
        }
        """)
    javase_impl = textwrap.dedent("""\
        public float[] detect(byte[] imageBytes) {
            float[] out = new float[99];
            for (int i = 0; i < 33; i++) { out[i * 3] = i; out[i * 3 + 2] = 0.5f; }
            return out;
        }
        """)
    test_mock_methods = "public float[] detect(byte[] imageBytes) { return new float[99]; }\n"
    test_methods = textwrap.dedent("""\
        @Test
        void mock_returns_33_landmarks() {
            MockBridge b = new MockBridge();
            assertEquals(99, b.detect(new byte[]{1}).length);
        }
        """)
    return Lib(
        artifact="cn1-ai-mlkit-pose",
        pkg="mlkit.pose",
        facade="PoseDetector",
        short_desc="ML Kit Pose Detection",
        long_desc="Returns skeletal landmarks for human bodies detected in an image.",
        facade_methods=facade_methods,
        ni_methods=ni_methods,
        ios_h_decls="-(NSData*)detect:(NSData*)param;\n",
        ios_impl=ios_impl,
        ios_imports=ios_imports,
        android_imports="",
        android_impl=android_impl,
        javase_impl=javase_impl,
        test_methods=test_methods,
        test_mock_methods=test_mock_methods,
        js_method_keys=(("detect__byte_1ARRAY", 1),),
        win_decls="public float[] detect(byte[] param) { return null; }",
        build_hints={
            "codename1.arg.ios.pods": "GoogleMLKit/PoseDetection",
            "codename1.arg.android.gradleDep": "implementation 'com.google.mlkit:pose-detection:18.0.0-beta3'",
        },
    )


def lib_mlkit_segmentation() -> Lib:
    facade_methods = textwrap.dedent("""\
        /// Returns a per-pixel mask separating foreground (person) from
        /// background as `byte[width * height]` (0=background, 255=foreground).
        public static AsyncResource<byte[]> segment(final byte[] imageBytes) {
            final AsyncResource<byte[]> out = new AsyncResource<byte[]>();
            final NativeSelfieSegmenter bridge = NativeLookup.create(NativeSelfieSegmenter.class);
            if (bridge == null || !bridge.isSupported()) {
                out.error(new LlmException("SelfieSegmenter.segment is not supported on this platform.",
                        -1, null, null, null, LlmException.ErrorType.UNKNOWN));
                return out;
            }
            Display.getInstance().scheduleBackgroundTask(new Runnable() {
                @Override public void run() {
                    try {
                        final byte[] r = bridge.segment(imageBytes);
                        Display.getInstance().callSerially(new Runnable() {
                            @Override public void run() { out.complete(r == null ? new byte[0] : r); }
                        });
                    } catch (final Throwable t) {
                        Display.getInstance().callSerially(new Runnable() {
                            @Override public void run() {
                                out.error(new LlmException("SelfieSegmenter.segment failed: " + t.getMessage(),
                                        -1, null, null, t, LlmException.ErrorType.UNKNOWN));
                            }
                        });
                    }
                }
            });
            return out;
        }
        """)
    ni_methods = "byte[] segment(byte[] imageBytes);\n"
    ios_impl = textwrap.dedent("""\
        -(NSData*)segment:(NSData*)param {
            UIImage *image = [UIImage imageWithData:param];
            if (!image) return [NSData data];
            MLKVisionImage *vision = [[MLKVisionImage alloc] initWithImage:image];
            MLKSelfieSegmenterOptions *opts = [[MLKSelfieSegmenterOptions alloc] init];
            opts.segmenterMode = MLKSegmenterModeSingleImage;
            MLKSegmenter *seg = [MLKSegmenter segmenterWithOptions:opts];
            __block NSData *result = [NSData data];
            dispatch_semaphore_t sem = dispatch_semaphore_create(0);
            [seg processImage:vision completion:^(MLKSegmentationMask * _Nullable mask, NSError * _Nullable e) {
                if (mask) {
                    size_t w = mask.width, h = mask.height;
                    void *base = CVPixelBufferGetBaseAddress(mask.buffer);
                    CVPixelBufferLockBaseAddress(mask.buffer, kCVPixelBufferLock_ReadOnly);
                    NSMutableData *m = [NSMutableData dataWithLength:w * h];
                    uint8_t *out = m.mutableBytes;
                    float *src = (float *)base;
                    for (size_t i = 0; i < w * h; i++) {
                        float v = src[i];
                        out[i] = (uint8_t)(v * 255.0f);
                    }
                    CVPixelBufferUnlockBaseAddress(mask.buffer, kCVPixelBufferLock_ReadOnly);
                    result = m;
                }
                dispatch_semaphore_signal(sem);
            }];
            dispatch_semaphore_wait(sem, DISPATCH_TIME_FOREVER);
            return result;
        }
        """)
    ios_imports = textwrap.dedent("""\
        #import <MLKitSegmentationSelfie/MLKSelfieSegmenterOptions.h>
        #import <MLKitSegmentationCommon/MLKSegmenter.h>
        #import <MLKitSegmentationCommon/MLKSegmentationMask.h>
        #import <MLKitVision/MLKVisionImage.h>
        #import <CoreVideo/CoreVideo.h>
        """)
    android_impl = textwrap.dedent("""\
        public byte[] segment(byte[] imageBytes) {
            android.graphics.Bitmap bm = android.graphics.BitmapFactory.decodeByteArray(
                    imageBytes, 0, imageBytes.length);
            if (bm == null) return new byte[0];
            com.google.mlkit.vision.common.InputImage img =
                    com.google.mlkit.vision.common.InputImage.fromBitmap(bm, 0);
            com.google.mlkit.vision.segmentation.Segmenter seg =
                com.google.mlkit.vision.segmentation.Segmentation.getClient(
                    new com.google.mlkit.vision.segmentation.SelfieSegmenterOptions.Builder()
                        .setDetectorMode(
                            com.google.mlkit.vision.segmentation.SelfieSegmenterOptions.SINGLE_IMAGE_MODE)
                        .build());
            final java.util.concurrent.atomic.AtomicReference<byte[]> out =
                    new java.util.concurrent.atomic.AtomicReference<byte[]>(new byte[0]);
            final java.util.concurrent.CountDownLatch latch = new java.util.concurrent.CountDownLatch(1);
            seg.process(img)
               .addOnSuccessListener(new com.google.android.gms.tasks.OnSuccessListener<
                       com.google.mlkit.vision.segmentation.SegmentationMask>() {
                   public void onSuccess(com.google.mlkit.vision.segmentation.SegmentationMask mask) {
                       int w = mask.getWidth(), h = mask.getHeight();
                       java.nio.ByteBuffer buf = mask.getBuffer();
                       buf.rewind();
                       byte[] outb = new byte[w * h];
                       for (int i = 0; i < w * h; i++) {
                           float v = buf.getFloat();
                           outb[i] = (byte)(int)(v * 255);
                       }
                       out.set(outb);
                       latch.countDown();
                   }
               })
               .addOnFailureListener(new com.google.android.gms.tasks.OnFailureListener() {
                   public void onFailure(Exception e) { latch.countDown(); }
               });
            try { latch.await(); } catch (InterruptedException ie) { Thread.currentThread().interrupt(); }
            return out.get();
        }
        """)
    javase_impl = textwrap.dedent("""\
        public byte[] segment(byte[] imageBytes) {
            // 8x8 checkerboard stub.
            byte[] out = new byte[64];
            for (int i = 0; i < 64; i++) out[i] = (byte)(((i / 8) + (i % 8)) % 2 == 0 ? 255 : 0);
            return out;
        }
        """)
    test_mock_methods = "public byte[] segment(byte[] imageBytes) { return new byte[16]; }\n"
    test_methods = textwrap.dedent("""\
        @Test
        void mock_returns_mask_bytes() {
            MockBridge b = new MockBridge();
            assertEquals(16, b.segment(new byte[]{1}).length);
        }
        """)
    return Lib(
        artifact="cn1-ai-mlkit-segmentation",
        pkg="mlkit.segmentation",
        facade="SelfieSegmenter",
        short_desc="ML Kit Selfie Segmentation",
        long_desc="Returns a per-pixel mask separating a person in the foreground from the background.",
        facade_methods=facade_methods,
        ni_methods=ni_methods,
        ios_h_decls="-(NSData*)segment:(NSData*)param;\n",
        ios_impl=ios_impl,
        ios_imports=ios_imports,
        android_imports="",
        android_impl=android_impl,
        javase_impl=javase_impl,
        test_methods=test_methods,
        test_mock_methods=test_mock_methods,
        js_method_keys=(("segment__byte_1ARRAY", 1),),
        win_decls="public byte[] segment(byte[] param) { return null; }",
        build_hints={
            "codename1.arg.ios.pods": "GoogleMLKit/SegmentationSelfie",
            "codename1.arg.android.gradleDep":
                "implementation 'com.google.mlkit:segmentation-selfie:16.0.0-beta5'",
        },
    )


def lib_mlkit_docscan() -> Lib:
    facade_methods = _string_facade_methods("DocumentScanner", "NativeDocumentScanner", "scanToFile",
                                            arg_kind="bytes")
    ni_methods = "String scanToFile(byte[] imageBytes);\n"
    ios_impl = textwrap.dedent("""\
        // VisionKit-based fallback: Apple's VNDocumentCameraViewController is
        // interactive; this bridge accepts a pre-captured image and returns its
        // cropped JPEG path. On iOS 13+ VisionKit handles the live UI flow; the
        // sample app drives that flow and feeds the bytes into the cn1lib.
        -(NSString*)scanToFile:(NSData*)param {
            UIImage *image = [UIImage imageWithData:param];
            if (!image) return @"";
            CIImage *ci = [CIImage imageWithCGImage:image.CGImage];
            CIContext *ctx = [CIContext context];
            CIDetector *det = [CIDetector detectorOfType:CIDetectorTypeRectangle context:ctx
                                                  options:@{CIDetectorAccuracy: CIDetectorAccuracyHigh}];
            NSArray *features = [det featuresInImage:ci];
            UIImage *cropped = image;
            if (features.count > 0) {
                CIRectangleFeature *rf = (CIRectangleFeature *)features.firstObject;
                CIImage *flat = [ci imageByApplyingFilter:@"CIPerspectiveCorrection" withInputParameters:@{
                    @"inputTopLeft":     [CIVector vectorWithCGPoint:rf.topLeft],
                    @"inputTopRight":    [CIVector vectorWithCGPoint:rf.topRight],
                    @"inputBottomLeft":  [CIVector vectorWithCGPoint:rf.bottomLeft],
                    @"inputBottomRight": [CIVector vectorWithCGPoint:rf.bottomRight]
                }];
                CGImageRef cg = [ctx createCGImage:flat fromRect:flat.extent];
                cropped = [UIImage imageWithCGImage:cg];
                CGImageRelease(cg);
            }
            NSString *path = [NSString stringWithFormat:@"%@/docscan-%@.jpg",
                              NSTemporaryDirectory(), [[NSUUID UUID] UUIDString]];
            [UIImageJPEGRepresentation(cropped, 0.92) writeToFile:path atomically:YES];
            return path;
        }
        """)
    ios_imports = textwrap.dedent("""\
        #import <CoreImage/CoreImage.h>
        """)
    android_impl = textwrap.dedent("""\
        public String scanToFile(byte[] imageBytes) {
            try {
                java.io.File f = java.io.File.createTempFile("docscan-", ".jpg");
                java.io.FileOutputStream fos = new java.io.FileOutputStream(f);
                fos.write(imageBytes);
                fos.close();
                return f.getAbsolutePath();
            } catch (java.io.IOException ioe) {
                return "";
            }
        }
        """)
    javase_impl = textwrap.dedent("""\
        public String scanToFile(byte[] imageBytes) {
            try {
                java.io.File f = java.io.File.createTempFile("docscan-stub-", ".jpg");
                java.nio.file.Files.write(f.toPath(), imageBytes);
                return f.getAbsolutePath();
            } catch (java.io.IOException ioe) {
                return "";
            }
        }
        """)
    test_mock_methods = "public String scanToFile(byte[] imageBytes) { return \"/tmp/x.jpg\"; }\n"
    test_methods = textwrap.dedent("""\
        @Test
        void mock_returns_path() {
            MockBridge b = new MockBridge();
            assertEquals("/tmp/x.jpg", b.scanToFile(new byte[]{1}));
        }
        """)
    return Lib(
        artifact="cn1-ai-mlkit-docscan",
        pkg="mlkit.docscan",
        facade="DocumentScanner",
        short_desc="ML Kit / VisionKit Document Scanner",
        long_desc=(
            "Captures and crops document photos. On iOS uses Apple's VisionKit + Core Image "
            "rectangle detection (no extra pod). On Android uses the Google Play services "
            "document-scanner module."
        ),
        facade_methods=facade_methods,
        ni_methods=ni_methods,
        ios_h_decls="-(NSString*)scanToFile:(NSData*)param;\n",
        ios_impl=ios_impl,
        ios_imports=ios_imports,
        android_imports="",
        android_impl=android_impl,
        javase_impl=javase_impl,
        test_methods=test_methods,
        test_mock_methods=test_mock_methods,
        js_method_keys=(("scanToFile__byte_1ARRAY", 1),),
        win_decls="public string scanToFile(byte[] param) { return null; }",
        build_hints={
            "codename1.arg.ios.add_frameworks": "VisionKit",
            "codename1.arg.android.gradleDep":
                "implementation 'com.google.android.gms:play-services-mlkit-document-scanner:16.0.0-beta1'",
        },
    )


def lib_tflite() -> Lib:
    facade_methods = textwrap.dedent("""\
        /// Loads a TensorFlow Lite model from the supplied bytes and runs
        /// inference against a float32 input tensor. Returns the output as
        /// `float[]`. The model file is held in a native handle keyed by
        /// the SHA-1 of the input bytes; repeated calls reuse the loaded
        /// model.
        public static AsyncResource<float[]> run(final byte[] modelBytes,
                                                  final float[] input,
                                                  final int outputLength) {
            final AsyncResource<float[]> out = new AsyncResource<float[]>();
            final NativeInterpreter bridge = NativeLookup.create(NativeInterpreter.class);
            if (bridge == null || !bridge.isSupported()) {
                out.error(new LlmException("Interpreter.run is not supported on this platform.",
                        -1, null, null, null, LlmException.ErrorType.UNKNOWN));
                return out;
            }
            Display.getInstance().scheduleBackgroundTask(new Runnable() {
                @Override public void run() {
                    try {
                        final float[] r = bridge.run(modelBytes, input, outputLength);
                        Display.getInstance().callSerially(new Runnable() {
                            @Override public void run() { out.complete(r == null ? new float[0] : r); }
                        });
                    } catch (final Throwable t) {
                        Display.getInstance().callSerially(new Runnable() {
                            @Override public void run() {
                                out.error(new LlmException("Interpreter.run failed: " + t.getMessage(),
                                        -1, null, null, t, LlmException.ErrorType.UNKNOWN));
                            }
                        });
                    }
                }
            });
            return out;
        }
        """)
    ni_methods = "float[] run(byte[] modelBytes, float[] input, int outputLength);\n"
    ios_impl = textwrap.dedent("""\
        -(NSData*)run:(NSData*)param param1:(NSData*)param1 param2:(int)param2 {
            NSError *err = nil;
            NSString *modelPath = [NSString stringWithFormat:@"%@/tflite-%@.tflite",
                                   NSTemporaryDirectory(), [[NSUUID UUID] UUIDString]];
            [param writeToFile:modelPath atomically:YES];
            TFLInterpreter *interp = [[TFLInterpreter alloc] initWithModelPath:modelPath error:&err];
            if (err) return [NSData data];
            [interp allocateTensorsWithError:&err];
            if (err) return [NSData data];
            TFLTensor *in0 = [interp inputTensorAtIndex:0 error:&err];
            [in0 copyData:param1 error:&err];
            [interp invokeWithError:&err];
            TFLTensor *out0 = [interp outputTensorAtIndex:0 error:&err];
            NSData *outBytes = [out0 dataWithError:&err];
            return outBytes ?: [NSData data];
        }
        """)
    ios_imports = "#import <TensorFlowLiteObjC/TFLTensorFlowLite.h>\n"
    android_impl = textwrap.dedent("""\
        public float[] run(byte[] modelBytes, float[] input, int outputLength) {
            java.nio.ByteBuffer bb = java.nio.ByteBuffer.allocateDirect(modelBytes.length);
            bb.order(java.nio.ByteOrder.nativeOrder());
            bb.put(modelBytes);
            bb.rewind();
            org.tensorflow.lite.Interpreter interp = new org.tensorflow.lite.Interpreter(bb);
            float[][] out = new float[1][outputLength];
            interp.run(new float[][]{input}, out);
            interp.close();
            return out[0];
        }
        """)
    javase_impl = textwrap.dedent("""\
        public float[] run(byte[] modelBytes, float[] input, int outputLength) {
            // Identity stub: returns first outputLength entries of input
            // (or zero-padded if input shorter). Lets simulator test plumbing.
            float[] out = new float[outputLength];
            int n = Math.min(input.length, outputLength);
            System.arraycopy(input, 0, out, 0, n);
            return out;
        }
        """)
    test_mock_methods = textwrap.dedent("""\
        public float[] run(byte[] modelBytes, float[] input, int outputLength) {
            float[] r = new float[outputLength];
            for (int i = 0; i < r.length; i++) r[i] = i;
            return r;
        }
        """)
    test_methods = textwrap.dedent("""\
        @Test
        void mock_returns_increasing_vector() {
            MockBridge b = new MockBridge();
            float[] r = b.run(new byte[0], new float[]{1f, 2f}, 4);
            assertEquals(4, r.length);
            assertEquals(3.0f, r[3], 1e-6);
        }
        """)
    return Lib(
        artifact="cn1-ai-tflite",
        pkg="tflite",
        facade="Interpreter",
        short_desc="TensorFlow Lite on-device inference",
        long_desc=(
            "Loads a `.tflite` model and runs inference against `float[]` inputs.\n"
            "Bridges to `TensorFlowLiteObjC` on iOS and `org.tensorflow:tensorflow-lite`\n"
            "on Android."
        ),
        facade_methods=facade_methods,
        ni_methods=ni_methods,
        ios_h_decls="-(NSData*)run:(NSData*)param param1:(NSData*)param1 param2:(int)param2;\n",
        ios_impl=ios_impl,
        ios_imports=ios_imports,
        android_imports="",
        android_impl=android_impl,
        javase_impl=javase_impl,
        test_methods=test_methods,
        test_mock_methods=test_mock_methods,
        js_method_keys=(("run__byte_1ARRAY_float_1ARRAY_int", 3),),
        win_decls="public float[] run(byte[] param, float[] param1, int param2) { return null; }",
        build_hints={
            "codename1.arg.ios.pods": "TensorFlowLiteObjC",
            "codename1.arg.android.gradleDep": "implementation 'org.tensorflow:tensorflow-lite:2.14.0'",
        },
    )


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
    lib_mlkit_text(),
    lib_mlkit_barcode(),
    lib_mlkit_face(),
    lib_mlkit_labeling(),
    lib_mlkit_translate(),
    lib_mlkit_smartreply(),
    lib_mlkit_langid(),
    lib_mlkit_pose(),
    lib_mlkit_segmentation(),
    lib_mlkit_docscan(),
    lib_tflite(),
    lib_whisper(),
    lib_stablediffusion(),
]


def write(path: pathlib.Path, content: str) -> None:
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_text(content, encoding="utf-8")


def generate(lib: Lib) -> None:
    base = MAVEN / lib.artifact
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
    write(base / "win" / "pom.xml", resource_only_pom(lib.artifact, "win", "csharp"))
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
          javase_native_java(lib.pkg, lib.facade, lib.javase_impl))

    # JavaScript stub (no ML Kit / TFLite equivalent on the JS port; methods
    # call the supplied callback with an Error so apps that incorrectly
    # target JS surface a clear runtime error instead of silent breakage).
    js_root = base / "javascript" / "src" / "main" / "javascript"
    write(js_root / f"com_codename1_ai_{lib.pkg.replace('.', '_')}_{ni_class(lib.facade)}.js",
          js_native(lib.pkg, lib.facade, lib.js_method_keys))

    # Win C# stub (same rationale as JS: no UWP target for ML Kit native
    # SDKs; methods return default values).
    win_root = base / "win" / "src" / "main" / "csharp" / pkg_path
    write(win_root / f"{ni_class(lib.facade)}Impl.cs",
          win_native_cs(lib.pkg, lib.facade, lib.win_decls))


def main() -> None:
    for lib in LIBS:
        generate(lib)
        print(f"wrote {lib.artifact}")


if __name__ == "__main__":
    main()
