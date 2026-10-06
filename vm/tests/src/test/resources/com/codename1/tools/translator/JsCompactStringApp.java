public class JsCompactStringApp {
    static int result;

    public static void main(String[] args) {
        char[] latin = new char[256];
        for (int i = 0; i < latin.length; i++) latin[i] = (char) i;
        String compact = new String(latin, 0, latin.length);
        // charAt, equals and getChars cross the Java-to-JavaScript string boundary.
        // Testing ASCII alone misses signed-byte corruption in U+0080..U+00FF.
        boolean valid = true;
        for (int i = 0; i < latin.length; i++) valid &= compact.charAt(i) == latin[i];
        if (valid) result |= 1;
        String expected = "Caff\u00e8, perch\u00e9, citt\u00e0, pi\u00f9, per\u00f2.";
        String copy = new String(expected.toCharArray());
        if (expected.equals(copy)) result |= 2;
        String slice = ("prefix" + copy + "suffix").substring(6, 6 + copy.length());
        if (expected.equals(slice)) result |= 4;
        String wide = "\u3053\u3093\u306b\u3061\u306f | \u05e9\u05dc\u05d5\u05dd | \u0645\u0631\u062d\u0628\u0627 | \ud83d\ude00 | e\u0301";
        if (wide.equals(new String(wide.toCharArray()))) result |= 8;
        StringBuilder builder = new StringBuilder(copy);
        builder.append(wide);
        if ((expected + wide).equals(builder.toString())) result |= 16;
        char[] roundTrip = compact.toCharArray();
        valid = roundTrip.length == 256;
        for (int i = 0; i < roundTrip.length; i++) valid &= roundTrip[i] == latin[i];
        if (valid) result |= 32;
        System.out.println("RESULT=" + result);
    }
}
