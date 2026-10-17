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
package com.codename1.desktopcompat.org.fife.ui.rsyntaxtextarea;

/// The names of the syntax styles a [RSyntaxTextArea] can be told to use.
///
/// Every name is accepted. This layer colors by the shape of a language
/// rather than by each language, as [RSyntaxTextArea] describes, and a
/// style it has no shape for is shown as plain text.
public interface SyntaxConstants {

    String SYNTAX_STYLE_NONE = "text/plain";
    String SYNTAX_STYLE_ACTIONSCRIPT = "text/actionscript";
    String SYNTAX_STYLE_ASSEMBLER_X86 = "text/asm";
    String SYNTAX_STYLE_ASSEMBLER_6502 = "text/asm6502";
    String SYNTAX_STYLE_BBCODE = "text/bbcode";
    String SYNTAX_STYLE_C = "text/c";
    String SYNTAX_STYLE_CLOJURE = "text/clojure";
    String SYNTAX_STYLE_CPLUSPLUS = "text/cpp";
    String SYNTAX_STYLE_CSHARP = "text/cs";
    String SYNTAX_STYLE_CSS = "text/css";
    String SYNTAX_STYLE_CSV = "text/csv";
    String SYNTAX_STYLE_D = "text/d";
    String SYNTAX_STYLE_DOCKERFILE = "text/dockerfile";
    String SYNTAX_STYLE_DART = "text/dart";
    String SYNTAX_STYLE_DELPHI = "text/delphi";
    String SYNTAX_STYLE_DTD = "text/dtd";
    String SYNTAX_STYLE_FORTRAN = "text/fortran";
    String SYNTAX_STYLE_GO = "text/golang";
    String SYNTAX_STYLE_GROOVY = "text/groovy";
    String SYNTAX_STYLE_HANDLEBARS = "text/handlebars";
    String SYNTAX_STYLE_HOSTS = "text/hosts";
    String SYNTAX_STYLE_HTACCESS = "text/htaccess";
    String SYNTAX_STYLE_HTML = "text/html";
    String SYNTAX_STYLE_INI = "text/ini";
    String SYNTAX_STYLE_JAVA = "text/java";
    String SYNTAX_STYLE_JAVASCRIPT = "text/javascript";
    String SYNTAX_STYLE_JSON = "text/json";
    String SYNTAX_STYLE_JSON_WITH_COMMENTS = "text/jshintrc";
    String SYNTAX_STYLE_JSP = "text/jsp";
    String SYNTAX_STYLE_KOTLIN = "text/kotlin";
    String SYNTAX_STYLE_LATEX = "text/latex";
    String SYNTAX_STYLE_LESS = "text/less";
    String SYNTAX_STYLE_LISP = "text/lisp";
    String SYNTAX_STYLE_LUA = "text/lua";
    String SYNTAX_STYLE_MAKEFILE = "text/makefile";
    String SYNTAX_STYLE_MARKDOWN = "text/markdown";
    String SYNTAX_STYLE_MXML = "text/mxml";
    String SYNTAX_STYLE_NSIS = "text/nsis";
    String SYNTAX_STYLE_PERL = "text/perl";
    String SYNTAX_STYLE_PHP = "text/php";
    String SYNTAX_STYLE_PROTO = "text/proto";
    String SYNTAX_STYLE_PROPERTIES_FILE = "text/properties";
    String SYNTAX_STYLE_PYTHON = "text/python";
    String SYNTAX_STYLE_RUBY = "text/ruby";
    String SYNTAX_STYLE_RUST = "text/rust";
    String SYNTAX_STYLE_SAS = "text/sas";
    String SYNTAX_STYLE_SCALA = "text/scala";
    String SYNTAX_STYLE_SQL = "text/sql";
    String SYNTAX_STYLE_TCL = "text/tcl";
    String SYNTAX_STYLE_TYPESCRIPT = "text/typescript";
    String SYNTAX_STYLE_UNIX_SHELL = "text/unix";
    String SYNTAX_STYLE_VISUAL_BASIC = "text/vb";
    String SYNTAX_STYLE_VHDL = "text/vhdl";
    String SYNTAX_STYLE_WINDOWS_BATCH = "text/bat";
    String SYNTAX_STYLE_XML = "text/xml";
    String SYNTAX_STYLE_YAML = "text/yaml";
}
