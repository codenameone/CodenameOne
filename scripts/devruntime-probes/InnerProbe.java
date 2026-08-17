import com.codename1.ui.*;
public class InnerProbe {
    private int field = 5;
    class Inner { int get() { return field * 2; } }
    static class Nested { int get() { return 3; } }
    interface Cb { int call(); }
    Cb closure(final int base) { return new Cb() { public int call() { return base + field; } }; }
    public static void main(String[] a) {
        InnerProbe p = new InnerProbe();
        InnerProbe.Inner in = p.new Inner();
        System.out.println("PROBE InnerProbe: inner=" + in.get()
            + " nested=" + new Nested().get() + " closure=" + p.closure(10).call());
        new Form("Inner").show();
    }
}
