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

package com.codenameone.developerguide.snippets.generated;

import com.codename1.ui.CodeCompletion;
import com.codename1.ui.CodeDiagnostic;
import com.codename1.ui.CodeEditor;
import com.codename1.ui.RichTextArea;
import com.codename1.ui.RichTextFormat;
import com.codename1.ui.TextArea;
import com.codename1.ui.TextField;
import com.codename1.ui.editor.SyntaxHighlighter;
import com.codename1.ui.editor.SyntaxHighlightResult;
import com.codename1.ui.editor.SyntaxToken;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

class RichTextAndCodeEditingJavaSnippet {
    void richFormats() {
        RichTextArea editor = new RichTextArea();
        // tag::rich-text-and-code-editing-java-formats[]
        editor.setContent("# Release notes\n\nThis is **ready**.", RichTextFormat.MARKDOWN);
        editor.insertContent("{\\rtf1\\ansi {\\i pasted notes}}", RichTextFormat.RTF);
        editor.insertContent("== Details\n\n* Portable\n* Lightweight", RichTextFormat.ASCIIDOC);
        // end::rich-text-and-code-editing-java-formats[]
    }

    void lightweightFields() {
        // tag::rich-text-and-code-editing-java-lightweight-fields[]
        TextArea.setLightweightEditingEnabled(true);

        TextField title = new TextField("", "Title", 80, TextArea.ANY);
        TextArea notes = new TextArea("", 5, 30);
        // end::rich-text-and-code-editing-java-lightweight-fields[]
    }

    void completion() {
        CodeEditor editor = new CodeEditor();
        // tag::rich-text-and-code-editing-java-completion[]
        editor.setCompletionProvider((sourceEditor, code, cursor, results) -> {
            List<CodeCompletion> proposals = new ArrayList<>();
            proposals.add(new CodeCompletion("println(String value)", "println()")
                    .setType("method")
                    .setDetail("void"));
            proposals.add(new CodeCompletion("private").setType("keyword"));
            results.onSucess(proposals);
        });
        // end::rich-text-and-code-editing-java-completion[]
    }

    void syntaxHighlighter() {
        // tag::rich-text-and-code-editing-java-highlighter[]
        SyntaxHighlighter properties = (line, state) -> {
            List<SyntaxToken> tokens = new ArrayList<>();
            int equals = line.indexOf('=');
            if (equals > 0) {
                tokens.add(new SyntaxToken(0, equals, SyntaxToken.PROPERTY,
                        0x005cc5, 0x9cdcfe));
            }
            return new SyntaxHighlightResult(tokens, 0);
        };
        CodeEditor.registerSyntaxHighlighter("properties", properties);

        CodeEditor editor = new CodeEditor("properties", "accent=#0a66c2");
        // end::rich-text-and-code-editing-java-highlighter[]
    }

    void diagnostics() {
        CodeEditor editor = new CodeEditor();
        // tag::rich-text-and-code-editing-java-diagnostics[]
        editor.setDiagnostics(Arrays.asList(
                new CodeDiagnostic(6, 9, 6, 16, "Cannot resolve method prinln")
                        .setSeverity(CodeDiagnostic.ERROR),
                new CodeDiagnostic(3, 5, "Value is never read")
                        .setSeverity(CodeDiagnostic.WARNING)
        ));
        // end::rich-text-and-code-editing-java-diagnostics[]
    }
}
