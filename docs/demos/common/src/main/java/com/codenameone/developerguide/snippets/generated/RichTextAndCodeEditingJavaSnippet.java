package com.codenameone.developerguide.snippets.generated;

import com.codename1.ui.CodeCompletion;
import com.codename1.ui.CodeDiagnostic;
import com.codename1.ui.CodeEditor;
import com.codename1.ui.RichTextArea;
import com.codename1.ui.RichTextFormat;
import com.codename1.ui.TextArea;
import com.codename1.ui.TextField;
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
        TextField title = new TextField("", "Title", 80, TextArea.ANY);
        title.setLightweightEditingEnabled(true);

        TextArea notes = new TextArea("", 5, 30);
        notes.setLightweightEditingEnabled(true);
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
