package dart.runtime;

import dart.core.TypeError;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class DartRuntimeTest {

    @Test
    public void modIsNonNegativeLikeDart() {
        assertEquals(2, DartRuntime.mod(-3, 5));
        assertEquals(3, DartRuntime.mod(3, 5));
        assertEquals(2, DartRuntime.mod(-3, -5));
        assertEquals(1.5, DartRuntime.mod(-3.5, 5.0), 1e-9);
    }

    @Test
    public void tdivTruncatesTowardZero() {
        assertEquals(-2, DartRuntime.tdiv(-7, 3));
        assertEquals(2, DartRuntime.tdiv(7, 3));
        assertEquals(-2, DartRuntime.tdiv(-7.5, 3.0));
    }

    @Test
    public void doubleStrMatchesDartFormatting() {
        assertEquals("1.0", DartRuntime.doubleStr(1.0));
        assertEquals("-1.0", DartRuntime.doubleStr(-1.0));
        assertEquals("0.0", DartRuntime.doubleStr(0.0));
        assertEquals("-0.0", DartRuntime.doubleStr(-0.0));
        assertEquals("2.5", DartRuntime.doubleStr(2.5));
        assertEquals("NaN", DartRuntime.doubleStr(Double.NaN));
        assertEquals("Infinity", DartRuntime.doubleStr(Double.POSITIVE_INFINITY));
        assertEquals("-Infinity", DartRuntime.doubleStr(Double.NEGATIVE_INFINITY));
    }

    @Test
    public void strHandlesNullAndDoubles() {
        assertEquals("null", DartRuntime.str((Object) null));
        assertEquals("3.0", DartRuntime.str((Object) Double.valueOf(3)));
        assertEquals("7", DartRuntime.str(7L));
        assertEquals("true", DartRuntime.str(true));
    }

    @Test
    public void nnThrowsDartTypeError() {
        assertEquals("x", DartRuntime.nn("x"));
        assertThrows(TypeError.class, () -> DartRuntime.nn(null));
    }

    @Test
    public void eqIsNullSafe() {
        assertTrue(DartRuntime.eq(null, null));
        assertFalse(DartRuntime.eq(null, "a"));
        assertFalse(DartRuntime.eq("a", null));
        assertTrue(DartRuntime.eq("a", "a"));
    }

    @Test
    public void printSinkCapturesOutput() {
        StringBuilder sb = new StringBuilder();
        DartRuntime.setPrintSink(s -> sb.append(s).append('\n'));
        try {
            DartRuntime.print("hello");
            DartRuntime.print(42L);
            DartRuntime.print(1.0);
        } finally {
            DartRuntime.setPrintSink(null);
        }
        assertEquals("hello\n42\n1.0\n", sb.toString());
    }
}
