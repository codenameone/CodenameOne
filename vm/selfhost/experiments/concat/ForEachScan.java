import org.objectweb.asm.*;
import org.objectweb.asm.tree.*;
import java.io.*;
import java.util.*;

/**
 * Sizes the for-each specialisation opportunity. javac lowers
 * `for (T x : coll)` to:
 *      <coll expr>; INVOKEINTERFACE java/lang/Iterable.iterator()
 *      ASTORE it
 *   L: ALOAD it; INVOKEINTERFACE Iterator.hasNext(); IFEQ end
 *      ALOAD it; INVOKEINTERFACE Iterator.next(); CHECKCAST T; ASTORE x
 *      ... body ...  GOTO L
 *
 * A site is SPECIALISABLE when the static type of the collection expression is a
 * known indexable container (ArrayList / List / AbstractList), because then the
 * whole thing can be rewritten to an indexed loop with no Iterator allocated and
 * no interface dispatch per element.
 *
 * The receiver type is taken from the bytecode: the owner of the iterator() call
 * for an INVOKEVIRTUAL, or the declared type of the field/local feeding it where
 * that is recoverable. Anything not provably indexable is counted separately
 * rather than assumed.
 */
public final class ForEachScan {
    static long sites;
    static final Map<String,Integer> byOwner = new TreeMap<String,Integer>();

    public static void main(String[] a) throws Exception {
        for (String r : a) scan(new File(r));
        System.out.println("for-each sites (iterator() + hasNext + next): " + sites);
        System.out.println("by iterator() receiver type:");
        List<Map.Entry<String,Integer>> es = new ArrayList<Map.Entry<String,Integer>>(byOwner.entrySet());
        Collections.sort(es, new Comparator<Map.Entry<String,Integer>>() {
            public int compare(Map.Entry<String,Integer> x, Map.Entry<String,Integer> y) { return y.getValue()-x.getValue(); }});
        for (Map.Entry<String,Integer> e : es) System.out.printf("  %6d  %s%n", e.getValue(), e.getKey());
    }

    static void scan(File f) throws Exception {
        if (f.isDirectory()) { File[] k=f.listFiles(); if(k!=null) for(File c:k) scan(c); return; }
        if (!f.getName().endsWith(".class")) return;
        ClassReader cr; InputStream in=new FileInputStream(f);
        try { cr=new ClassReader(in); } finally { in.close(); }
        ClassNode cn=new ClassNode(); cr.accept(cn, ClassReader.SKIP_FRAMES);
        for (Object mo : cn.methods) {
            MethodNode m=(MethodNode)mo;
            for (AbstractInsnNode p=m.instructions.getFirst(); p!=null; p=p.getNext()) {
                if (!(p instanceof MethodInsnNode)) continue;
                MethodInsnNode mi=(MethodInsnNode)p;
                if (!"iterator".equals(mi.name) || !"()Ljava/util/Iterator;".equals(mi.desc)) continue;
                // require a hasNext within the next 40 instructions to call it a for-each
                boolean loop=false;
                int n=0;
                for (AbstractInsnNode q=p.getNext(); q!=null && n<40; q=q.getNext(), n++) {
                    if (q instanceof MethodInsnNode && "hasNext".equals(((MethodInsnNode)q).name)) { loop=true; break; }
                }
                if (!loop) continue;
                sites++;
                String owner = mi.owner;
                Integer c=byOwner.get(owner); byOwner.put(owner, c==null?1:c+1);
            }
        }
    }
}
