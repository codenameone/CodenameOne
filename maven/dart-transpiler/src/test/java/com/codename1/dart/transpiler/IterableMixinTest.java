package com.codename1.dart.transpiler;

import com.codename1.dart.transpiler.harness.TestSupport;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The dart:collection mixins are registered as transpiler built-in stubs, so a class that applies
 * {@code with IterableMixin<T>} resolves the mixin (no E0402) and inherited mixin members resolve.
 */
public class IterableMixinTest {

    @Test
    public void iterableMixinIsRecognized() {
        String src =
                "class BoardPoint {}\n"
                        + "class Board extends Object with IterableMixin<BoardPoint> {\n"
                        + "  final List<BoardPoint> _points = [];\n"
                        + "  @override\n"
                        + "  Iterator<BoardPoint> get iterator => _points.iterator;\n"
                        + "}\n";
        TestSupport.Result r = TestSupport.transpile(new String[][] {{"board.dart", src}});
        String diagnostics = r.diags.asList().toString();
        assertFalse(diagnostics.contains("E0402"), "unexpected unknown-mixin error: " + diagnostics);
    }

    @Test
    public void inheritedMixinMemberResolvesInBareCall() {
        // forEach is contributed by IterableMixin as an inherited default; a bare call must resolve.
        String src =
                "class BoardPoint {}\n"
                        + "class Board extends Object with IterableMixin<BoardPoint> {\n"
                        + "  final List<BoardPoint> _points = [];\n"
                        + "  @override\n"
                        + "  Iterator<BoardPoint> get iterator => _points.iterator;\n"
                        + "  void dump() {\n"
                        + "    forEach((p) => print(p));\n"
                        + "  }\n"
                        + "}\n";
        TestSupport.Result r = TestSupport.transpile(new String[][] {{"board.dart", src}});
        String diagnostics = r.diags.asList().toString();
        assertFalse(diagnostics.contains("E0402"), "unknown-mixin error: " + diagnostics);
        assertTrue(!diagnostics.contains("E0137") || !diagnostics.contains("forEach"),
                "forEach should resolve via the mixin: " + diagnostics);
    }

    @Test
    public void inheritedMixinMemberResolvesOnReceiver() {
        // board.forEach(...) — a call on a typed receiver must resolve through the applied mixin.
        String src =
                "class BoardPoint {}\n"
                        + "class Board extends Object with IterableMixin<BoardPoint> {\n"
                        + "  final List<BoardPoint> _points = [];\n"
                        + "  @override\n"
                        + "  Iterator<BoardPoint> get iterator => _points.iterator;\n"
                        + "}\n"
                        + "void paint(Board board) {\n"
                        + "  board.forEach((p) => print(p));\n"
                        + "}\n";
        TestSupport.Result r = TestSupport.transpile(new String[][] {{"board.dart", src}});
        String diagnostics = r.diags.asList().toString();
        assertTrue(!diagnostics.contains("E0137") || !diagnostics.contains("forEach"),
                "forEach on a Board receiver should resolve via the mixin: " + diagnostics);
    }
}
