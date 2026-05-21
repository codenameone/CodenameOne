package com.codename1.impl.javase.simulator;

import com.codename1.system.SimulatorHookExecutor;
import com.codename1.ui.Display;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.reflect.Method;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

/**
 * Discovers cn1lib-contributed simulator menu items by scanning the classpath
 * for {@code META-INF/codenameone/simulator-hooks.properties}. Each file
 * declares one named group of positional items:
 *
 * <pre>
 * name=Bluetooth
 * namespace=bluetooth          # optional; defaults to slugified `name`
 *
 * item1=com.example.bt.sim.Hooks#toggleAdapter
 * label1=Toggle adapter
 *
 * item2=com.example.bt.sim.Hooks#addDemoPeripheral
 * label2=Add demo peripheral
 *
 * # Label omitted -> API-only hook. Callable from tests via
 * # CN.execute("bluetooth:item3"), invisible in the menu.
 * item3=com.example.bt.sim.Hooks#primeReadFailure
 * </pre>
 *
 * <p>Items are positional: the loader reads {@code item1}, {@code item2},
 * {@code item3}, ... and stops at the first missing {@code itemN}. Each
 * {@code itemN} value is a {@code fqcn#staticMethodName} reference; the
 * matching {@code labelN} (optional) becomes the menu item text.</p>
 *
 * <p>Actions are resolved to {@code public static void method()} via reflection
 * using the same classloader that loaded {@link Display}. Invocations are
 * dispatched via {@link Display#callSeriallyAndWait(Runnable)} so menu clicks
 * and {@code CN.execute} from off-EDT test code both run on the CN1 EDT and
 * are synchronous to the caller.</p>
 *
 * <p>Every successful load also registers each hook with {@link SimulatorHookExecutor}
 * under {@code "namespace:itemN"} keys so {@link com.codename1.ui.CN#execute(String)}
 * can drive it cross-platform.</p>
 */
public final class SimulatorHookLoader {

    private static final String RESOURCE_PATH = "META-INF/codenameone/simulator-hooks.properties";

    private SimulatorHookLoader() {}

    /**
     * Discovers all hooks visible to the JavaSE port's classloader (the one
     * that loaded {@link Display}). Safe to call multiple times; each call
     * re-scans the classpath and re-registers the executor. Errors in any
     * single file (missing keys, unresolvable class, no such method) are
     * logged and that entry is skipped; the rest are returned.
     */
    public static List<SimulatorHook> load() {
        ClassLoader cl = Display.class.getClassLoader();
        if (cl == null) {
            cl = ClassLoader.getSystemClassLoader();
        }
        return load(cl);
    }

    /**
     * Same as {@link #load()} but scans an explicit classloader. Primary
     * caller is tests that want to inject a fixture classpath; production
     * code should prefer {@link #load()}.
     */
    public static List<SimulatorHook> load(ClassLoader cl) {
        List<SimulatorHook> out = new ArrayList<SimulatorHook>();
        Enumeration<URL> urls;
        try {
            urls = cl.getResources(RESOURCE_PATH);
        } catch (IOException ex) {
            System.err.println("SimulatorHookLoader: failed to enumerate " + RESOURCE_PATH);
            ex.printStackTrace();
            SimulatorHookExecutor.register(Collections.<String, Runnable>emptyMap());
            return out;
        }
        while (urls.hasMoreElements()) {
            URL url = urls.nextElement();
            try {
                loadOne(url, cl, out);
            } catch (Throwable t) {
                System.err.println("SimulatorHookLoader: failed to parse " + url);
                t.printStackTrace();
            }
        }
        // Republish the registry so CN.execute reflects what we just loaded.
        Map<String, Runnable> registered = new LinkedHashMap<String, Runnable>();
        for (SimulatorHook h : out) {
            registered.put(h.getExecutorKey(), h.getInvoke());
        }
        SimulatorHookExecutor.register(registered);
        return out;
    }

    private static void loadOne(URL url, ClassLoader cl, List<SimulatorHook> out) throws IOException {
        Properties props = new Properties();
        InputStream in = url.openStream();
        try {
            // Reader form forces UTF-8; the default load(InputStream) is ISO-8859-1.
            props.load(new BufferedReader(new InputStreamReader(in, "UTF-8")));
        } finally {
            in.close();
        }
        String menuName = props.getProperty("name");
        if (menuName == null || menuName.trim().length() == 0) {
            System.err.println("SimulatorHookLoader: " + url + " is missing required 'name' property; skipping");
            return;
        }
        menuName = menuName.trim();
        String namespace = props.getProperty("namespace");
        if (namespace == null || namespace.trim().length() == 0) {
            namespace = slugify(menuName);
        } else {
            namespace = namespace.trim();
        }

        // Items are positional: read item1, item2, ... and stop at the
        // first missing N. labelN is optional (no label = API-only hook).
        int index = 1;
        while (true) {
            String action = props.getProperty("item" + index);
            if (action == null || action.trim().length() == 0) {
                break;
            }
            String label = props.getProperty("label" + index);
            if (label != null) {
                label = label.trim();
                if (label.length() == 0) {
                    label = null;
                }
            }
            Runnable invoke = buildInvoker(cl, action.trim(), url, index);
            if (invoke != null) {
                out.add(new SimulatorHook(namespace, index, menuName, label, invoke));
            }
            index++;
        }
    }

    /**
     * Reduces a free-form menu name to a stable, ASCII-only namespace token.
     * Used when the properties file doesn't declare {@code namespace=...}
     * explicitly. Lowercases letters, keeps digits, replaces anything else
     * with a single hyphen, trims leading/trailing hyphens.
     */
    static String slugify(String name) {
        StringBuilder sb = new StringBuilder(name.length());
        boolean lastDash = true;
        for (int i = 0; i < name.length(); i++) {
            char c = name.charAt(i);
            if (c >= 'A' && c <= 'Z') {
                sb.append((char)(c + 32));
                lastDash = false;
            } else if ((c >= 'a' && c <= 'z') || (c >= '0' && c <= '9')) {
                sb.append(c);
                lastDash = false;
            } else if (!lastDash) {
                sb.append('-');
                lastDash = true;
            }
        }
        int end = sb.length();
        while (end > 0 && sb.charAt(end - 1) == '-') end--;
        return sb.substring(0, end);
    }

    private static Runnable buildInvoker(ClassLoader cl, String action, URL source, int index) {
        int hash = action.indexOf('#');
        if (hash <= 0 || hash == action.length() - 1) {
            System.err.println("SimulatorHookLoader: " + source + " item" + index
                    + " has malformed action '" + action + "'; expected fqcn#methodName");
            return null;
        }
        String fqcn = action.substring(0, hash).trim();
        final String methodName = action.substring(hash + 1).trim();
        final Class<?> targetClass;
        final Method method;
        try {
            targetClass = Class.forName(fqcn, false, cl);
        } catch (ClassNotFoundException ex) {
            System.err.println("SimulatorHookLoader: " + source + " item" + index
                    + " references unknown class '" + fqcn + "'");
            return null;
        }
        try {
            method = targetClass.getDeclaredMethod(methodName, new Class<?>[0]);
        } catch (NoSuchMethodException ex) {
            System.err.println("SimulatorHookLoader: " + source + " item" + index
                    + " references unknown no-arg method '" + fqcn + "#" + methodName + "'");
            return null;
        }
        if (!java.lang.reflect.Modifier.isStatic(method.getModifiers())) {
            System.err.println("SimulatorHookLoader: " + source + " item" + index
                    + " references non-static method '" + fqcn + "#" + methodName + "'");
            return null;
        }
        method.setAccessible(true);
        final URL src = source;
        return new Runnable() {
            @Override
            public void run() {
                // callSeriallyAndWait so off-EDT callers (every CN1 UnitTest's
                // runTest()) block until the hook completes -- tests would
                // otherwise assert state changes before the EDT got to run
                // the action. On the EDT, callSeriallyAndWait runs the body
                // inline without re-entering the dispatch queue.
                Display.getInstance().callSeriallyAndWait(new Runnable() {
                    @Override
                    public void run() {
                        try {
                            method.invoke(null);
                        } catch (Throwable t) {
                            System.err.println("SimulatorHookLoader: action from " + src + " threw");
                            t.printStackTrace();
                        }
                    }
                });
            }
        };
    }
}
