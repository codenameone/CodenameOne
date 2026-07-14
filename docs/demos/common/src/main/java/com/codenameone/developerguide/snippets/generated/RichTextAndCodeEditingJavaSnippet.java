package com.codenameone.developerguide.snippets.generated;

import com.codename1.ui.CodeCompletion;
import com.codename1.ui.CodeDiagnostic;
import com.codename1.ui.CodeEditor;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

class RichTextAndCodeEditingJavaSnippet {
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
