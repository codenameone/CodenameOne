package com.codename1.dart.transpiler;

import com.codename1.dart.transpiler.parser.Dart2Lexer;
import com.codename1.dart.transpiler.parser.Dart2Parser;
import org.antlr.v4.runtime.BaseErrorListener;
import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.RecognitionException;
import org.antlr.v4.runtime.Recognizer;
import org.junit.jupiter.api.Test;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class ParserSmokeTest {

    @Test
    public void counterAppParsesWithoutSyntaxErrors() throws Exception {
        InputStream in = getClass().getResourceAsStream("/fixtures/counter_main.dart");
        assertNotNull(in);
        Dart2Lexer lexer = new Dart2Lexer(CharStreams.fromStream(in));
        Dart2Parser parser = new Dart2Parser(new CommonTokenStream(lexer));
        List<String> errors = new ArrayList<>();
        parser.removeErrorListeners();
        parser.addErrorListener(new BaseErrorListener() {
            @Override
            public void syntaxError(Recognizer<?, ?> recognizer, Object offendingSymbol, int line,
                                    int charPositionInLine, String msg, RecognitionException e) {
                errors.add(line + ":" + charPositionInLine + " " + msg);
            }
        });
        Dart2Parser.CompilationUnitContext unit = parser.compilationUnit();
        assertNotNull(unit);
        assertTrue(errors.isEmpty(), "syntax errors: " + errors);
    }
}
