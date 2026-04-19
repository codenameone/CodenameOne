// Regression fixture for the ParparVM lambda-synthesis stack bug.
//
// LambdaMetafactory-based lambda classes are synthesized by `Parser.visitInvokeDynamicInsn`.
// Before the fix, that synthesis emitted TWO aload_0 instructions for every logical
// `this` push (once via the no-index `addInstruction(Opcodes.ALOAD)` BasicInstruction,
// once via the canonical `addVariableOperation(Opcodes.ALOAD, 0)` VarOp). The C backend
// silently ignored the BasicInstruction (no `case Opcodes.ALOAD` in its switch), but both
// JavaScript backends translated it as a real `push locals[0]`, corrupting the operand
// stack so invokevirtual popped from the wrong slots.
//
// For a 3-capture lambda, the bug caused `run()` to invoke its target method on the
// second capture (a Form in the real failure; a String here) instead of on the first
// capture (the enclosing `this`). That surfaced as
//   VIRTUAL_FAIL missing_interface_default_method
// because the second capture's class doesn't have the lambda's body method.
//
// This fixture constructs exactly that shape: a 3-capture lambda that invokes a
// 2-argument method on its first capture. If the stack is right, the method gets
// called on the enclosing instance and `result` lands at a specific value. If the
// stack is shifted, either a `VirtualMethod`-style error trips or the method runs
// on the wrong receiver and `result` differs.
public class JsCapturingLambdaDispatchApp {
    public static int result;

    interface Body {
        void run();
    }

    static int compute(JsCapturingLambdaDispatchApp self, String tag, int seed) {
        // Deliberately takes `self` explicitly so the javac-generated bridge
        // `lambda$registerReadyCallback$0(String, int)` on JsCapturingLambdaDispatchApp
        // performs a real instance dispatch rather than being desugared to a static.
        // 7 * 3 + "sheet".length() (=5) = 26, plus the instance base (=11) = 37.
        return self.base + seed * 3 + tag.length();
    }

    final int base;

    JsCapturingLambdaDispatchApp(int base) {
        this.base = base;
    }

    void schedule(String tag, int seed, int[] out) {
        // The Body lambda captures:
        //   arg_1 = this (JsCapturingLambdaDispatchApp)
        //   arg_2 = tag  (String)
        //   arg_3 = seed (int)
        // and calls this.recordInto(out, tag, seed) — a 3-arg instance method whose
        // `this` comes from arg_1 and whose two payload args come from arg_2/arg_3.
        Body b = () -> this.recordInto(out, tag, seed);
        b.run();
    }

    void recordInto(int[] out, String tag, int seed) {
        // Writes result into out[0]. If the lambda dispatch runs on the wrong
        // receiver this method won't fire (receiver will throw, or it will
        // dispatch to a method that doesn't exist on the misidentified target).
        out[0] = compute(this, tag, seed);
    }

    public static void main(String[] args) {
        int[] out = new int[]{-1};
        JsCapturingLambdaDispatchApp app = new JsCapturingLambdaDispatchApp(11);
        app.schedule("sheet", 7, out);
        // Expected: base(11) + 7*3 + "sheet".length()(5) = 37
        result = out[0];
    }
}
