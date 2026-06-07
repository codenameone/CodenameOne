import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;

/// Turns a form model (from `tools/DumpForm.java`) into a concise, designer-oriented
/// outline so an LLM can "see" a screen and iterate on it WITHOUT vision. Each line
/// is one component: its role, UIID, text, geometry and the few style facts that
/// matter for layout reasoning (background only when painted, border, scrolling).
///
/// Usage:
///     java tools/DumpForm.java com.example.MyApp --out target/form-model.tsv   # capture
///     java tools/DescribeForm.java target/form-model.tsv                       # describe
///
/// Self-contained: reads the TAB-delimited model, no Codename One jars needed.
/// Exit codes: 0 ok, 2 usage / bad model.
public class DescribeForm {
    public static void main(String[] args) throws Exception {
        if (args.length != 1) { System.err.println("Usage: java DescribeForm.java <form-model.tsv>"); System.exit(2); }
        List<String> lines = Files.readAllLines(Paths.get(args[0]));
        StringBuilder out = new StringBuilder();
        for (String line : lines) {
            if (line.startsWith("DISPLAY\t")) {
                String[] d = line.split("\t");
                out.append("Display ").append(d[1]).append('x').append(d[2]).append(" (desktop render)\n");
                continue;
            }
            if (!line.startsWith("C\t")) continue;
            String[] f = line.split("\t", -1);
            if (f.length < 18) continue;
            int depth = Integer.parseInt(f[1]);
            String cls = f[2], uiid = f[3];
            int x = pi(f[4]), y = pi(f[5]), w = pi(f[6]), h = pi(f[7]);
            boolean sx = f[8].equals("1"), sy = f[9].equals("1"), opaque = f[11].equals("1");
            String bg = f[12], border = f[14], layout = f[16];
            boolean imgBorder = f[15].equals("1");
            String text = f.length > 18 ? f[18] : "";

            StringBuilder l = new StringBuilder();
            l.append("  ".repeat(depth));
            // role: class, with the layout for containers (Box / Border / Flow / Grid...)
            String role = cls;
            if (cls.equals("Container") && !layout.equals("-")) {
                role = "Container/" + layout.replace("Layout", "");
            }
            l.append(role);
            if (!uiid.equals("-") && !uiid.equals(cls)) l.append(" \"").append(uiid).append('"');
            if (!text.isEmpty()) l.append(" '").append(trunc(text, 52)).append("'");
            l.append(" @").append(x).append(',').append(y).append(' ').append(w).append('x').append(h);
            if (opaque && !bg.equals("-")) l.append("  bg #").append(bg);
            if (!border.equals("none") && !border.equals("empty")) {
                l.append("  bd:").append(border);
                if (imgBorder) l.append("(image!)");
            }
            if (sy) l.append("  scroll-y");
            if (sx) l.append("  scroll-x");
            out.append(l).append('\n');
        }
        System.out.print(out);
    }

    private static int pi(String s) { try { return Integer.parseInt(s); } catch (Exception e) { return 0; } }

    private static String trunc(String s, int n) {
        return s.length() <= n ? s : s.substring(0, n - 1) + "...";
    }
}
