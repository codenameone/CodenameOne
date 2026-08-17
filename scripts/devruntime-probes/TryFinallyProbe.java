import com.codename1.ui.*;
public class TryFinallyProbe {
    static StringBuilder r = new StringBuilder();
    static int f() { try { r.append("t"); return 1; } finally { r.append("f"); } }
    static void nested() {
        try { try { throw new IllegalStateException("inner"); } finally { r.append("F1"); } }
        catch (IllegalStateException e) { r.append("C:").append(e.getMessage()); }
    }
    public static void main(String[] a) {
        r.append(" f=").append(f());
        nested();
        try { Object o = null; o.toString(); } catch (NullPointerException e) { r.append(" npe"); }
        try { int[] x = new int[1]; int y = x[3]; r.append(y); }
        catch (ArrayIndexOutOfBoundsException e) { r.append(" aioobe"); }
        try { int z = 1 / Integer.parseInt("0"); r.append(z); }
        catch (ArithmeticException e) { r.append(" arith"); }
        System.out.println("PROBE TryFinallyProbe: " + r);
        new Form("TryFinally").show();
    }
}
