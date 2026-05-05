// Regression fixture for the JavaScript port's invokevirtual peephole bug.
//
// For Java source ``s.setBgTransparency((int) f);`` (a void instance method
// taking an int, called with an explicit float-to-int cast on the argument),
// the unpeepholed translation is:
//
//     stack.p(s);                    // aload s   — receiver
//     stack.p(f);                    // fload f   — float arg
//     stack.p(stack.q() | 0);        // f2i       — coerce
//     { let __arg0 = stack.q(); yield* cn1_iv1(stack.q(), "MID", __arg0); pc; break; }
//
// Rule 9b in JavascriptMethodGenerator.applyMethodPeephole originally
// matched two consecutive ``stack.p(...)`` followed by the call block with
// a ``balanced parens'' EXPR pattern. That pattern accepted ``stack.q() | 0``
// — but ``stack.q()`` consumes the FIRST push, so the rule's invariant
// (that the call block's ``stack.q()`` will pop the second push) breaks.
// The peephole inlined the second push as the receiver and emitted
// ``stack.q() | 0`` as the arg, swapping the two and producing a call on
// the float value instead of on ``s``. In Toolbar.show*SidemenuImpl this
// surfaced as ``Missing virtual method $iA on undefined`` (setBgTransparency
// dispatched on a JS Number — clicking the hamburger menu in the
// Initializr sample crashed every time).
//
// This fixture reproduces the exact shape: a 3-arg static helper that
// receives (Holder s, float f, int marker), invokes
// ``s.setMaskedValue((int) f)`` (the analogue of setBgTransparency), and
// reads back the field. If receiver/arg are swapped, ``setMaskedValue`` runs
// on the boxed float wrapper and either throws ``Missing virtual method``
// or records the wrong value, depending on the dispatch fallback. If the
// fix holds, the field receives ``(int) f`` and the round-trip equals the
// expected mask.
public class JsF2IInvokeReceiverApp {
    public static int result;

    static class Holder {
        int masked;

        void setMaskedValue(int v) {
            // Adds a sentinel so a wrong dispatch (e.g. running on a
            // float wrapper that happens to also have a masked field)
            // can't accidentally match the expected value.
            masked = v ^ 0x5A;
        }

        int read() {
            return masked;
        }
    }

    static int callIt(Holder s, float f, int marker) {
        // Three pushes precede the invokevirtual:
        //   aload s            (receiver)
        //   fload f            (arg as float)
        //   f2i                (coerce)
        //   invokevirtual setMaskedValue(I)V
        s.setMaskedValue((int) f);
        return s.read() ^ marker;
    }

    public static void main(String[] args) {
        Holder h = new Holder();
        // f = 7.9 → (int) f = 7. setMaskedValue stores (7 ^ 0x5A) = 0x5D.
        // marker = 0x33; final result = 0x5D ^ 0x33 = 0x6E = 110.
        int v = callIt(h, 7.9f, 0x33);
        result = v;
    }
}
