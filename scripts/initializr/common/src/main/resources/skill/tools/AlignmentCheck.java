import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/// Detects whether UI elements line up on a consistent grid - the kind of thing a
/// designer's alignment guides catch by eye. It reads a form model (from
/// `tools/DumpForm.java`), finds the dominant left/right edge "guides" (columns
/// many elements share), then flags elements that sit ALMOST on a guide but are
/// off by a few pixels - the classic "this one label is nudged 10px right" bug.
///
/// Usage:
///     java tools/DumpForm.java com.example.MyApp --out target/form-model.tsv
///     java tools/AlignmentCheck.java target/form-model.tsv [--tol 12] [--min-shared 3]
///
///   --tol N          a guide "attracts" edges within N px (default 12)
///   --min-shared N   an edge column is a guide only if >= N elements share it (default 3)
///   --min-off N      ignore edges off a guide by < N px (sub-pixel rounding) (default 3)
///
/// Self-contained; no Codename One jars needed.
/// Exit codes: 0 no misalignments found, 1 misalignments reported, 2 usage error.
public class AlignmentCheck {

    static final class N {
        String label; int x, y, w, h;
    }

    public static void main(String[] args) throws Exception {
        if (args.length < 1) { System.err.println("Usage: java AlignmentCheck.java <form-model.tsv> [--tol N] [--min-shared N]"); System.exit(2); }
        int tol = 12, minShared = 3, minOff = 3;
        for (int i = 1; i < args.length; i++) {
            if (args[i].equals("--tol")) tol = Integer.parseInt(args[++i]);
            else if (args[i].equals("--min-shared")) minShared = Integer.parseInt(args[++i]);
            else if (args[i].equals("--min-off")) minOff = Integer.parseInt(args[++i]);
        }
        List<N> nodes = new ArrayList<>();
        for (String line : Files.readAllLines(Paths.get(args[0]))) {
            if (!line.startsWith("C\t")) continue;
            String[] f = line.split("\t", -1);
            if (f.length < 18) continue;
            int w = pi(f[6]), h = pi(f[7]);
            if (w <= 1 || h <= 1) continue; // skip zero/sliver components
            N n = new N();
            String uiid = f[3], cls = f[2], text = f.length > 18 ? f[18] : "";
            n.label = (!uiid.equals("-") ? uiid : cls) + (text.isEmpty() ? "" : " '" + trunc(text, 24) + "'");
            n.x = pi(f[4]); n.y = pi(f[5]); n.w = w; n.h = h;
            nodes.add(n);
        }

        StringBuilder out = new StringBuilder();
        int offenders = 0;
        offenders += axis(out, "LEFT edges (x)", nodes, true, tol, minShared, minOff);
        offenders += axis(out, "RIGHT edges (x+w)", nodes, false, tol, minShared, minOff);

        if (offenders == 0) {
            System.out.println("ALIGNED - no near-miss edges found (tol=" + tol + ", min-shared=" + minShared + ").");
            System.out.print(out);
            System.exit(0);
        }
        System.out.println("MISALIGNED - " + offenders + " element edge(s) sit just off a shared guide (tol="
                + tol + "). Snap them to the guide or move them clearly off it.\n");
        System.out.print(out);
        System.exit(1);
    }

    /// Returns the offender count for one axis (left or right edges).
    private static int axis(StringBuilder out, String title, List<N> nodes, boolean left, int tol, int minShared, int minOff) {
        // edge value -> elements sharing it exactly
        TreeMap<Integer, List<N>> byEdge = new TreeMap<>();
        for (N n : nodes) {
            int e = left ? n.x : n.x + n.w;
            byEdge.computeIfAbsent(e, k -> new ArrayList<>()).add(n);
        }
        // guides = edges shared by >= minShared elements
        List<Integer> guides = new ArrayList<>();
        for (Map.Entry<Integer, List<N>> e : byEdge.entrySet()) {
            if (e.getValue().size() >= minShared) guides.add(e.getKey());
        }
        out.append("== ").append(title).append(" ==\n");
        if (guides.isEmpty()) {
            out.append("  (no shared guide columns)\n\n");
            return 0;
        }
        out.append("  grid: ");
        for (int g : guides) out.append(g).append("px(x").append(byEdge.get(g).size()).append(") ");
        out.append('\n');

        int offenders = 0;
        for (Map.Entry<Integer, List<N>> e : byEdge.entrySet()) {
            int edge = e.getKey();
            if (guides.contains(edge)) continue;
            int nearest = nearest(guides, edge);
            int d = Math.abs(edge - nearest);
            if (d >= minOff && d <= tol) {
                for (N n : e.getValue()) {
                    out.append("  ! ").append(n.label).append("  edge=").append(edge)
                       .append("  guide=").append(nearest).append("  off by ").append(edge - nearest).append("px\n");
                    offenders++;
                }
            }
        }
        if (offenders == 0) out.append("  ok - every off-grid edge is clearly distinct\n");
        out.append('\n');
        return offenders;
    }

    private static int nearest(List<Integer> guides, int v) {
        int best = guides.get(0);
        for (int g : guides) if (Math.abs(g - v) < Math.abs(best - v)) best = g;
        return best;
    }

    private static int pi(String s) { try { return Integer.parseInt(s); } catch (Exception e) { return 0; } }
    private static String trunc(String s, int n) { return s.length() <= n ? s : s.substring(0, n - 1) + "..."; }
}
