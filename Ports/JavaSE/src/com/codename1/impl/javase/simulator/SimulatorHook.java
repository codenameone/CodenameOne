package com.codename1.impl.javase.simulator;

/**
 * One positional action contributed by a cn1lib (or the app) to the simulator.
 *
 * <p>Hooks are positional within each {@code simulator-hooks.properties}
 * file: {@code item1} is the first menu entry, {@code item2} the second,
 * etc. A hook with a non-empty {@link #getLabel() label} renders as a menu
 * item; a label-less hook is API-only -- invisible in the menu but still
 * callable via {@code CN.execute("namespace:itemN")} for test scaffolding.</p>
 */
public final class SimulatorHook {
    private final String namespace;
    private final int index;
    private final String menuName;
    private final String label;
    private final Runnable invoke;

    public SimulatorHook(String namespace, int index, String menuName, String label, Runnable invoke) {
        this.namespace = namespace;
        this.index = index;
        this.menuName = menuName;
        this.label = label;
        this.invoke = invoke;
    }

    /** Stable namespace token (one per properties file). */
    public String getNamespace() { return namespace; }

    /** 1-based position of this item within its properties file. */
    public int getIndex() { return index; }

    /** URL passed to {@code CN.execute} to trigger this hook -- {@code namespace + ":item" + index}. */
    public String getExecutorKey() { return namespace + ":item" + index; }

    /** Display title of the menu this hook belongs to (one per properties file). */
    public String getMenuName() { return menuName; }

    /**
     * Display label for the menu item, or {@code null}/empty if this hook is
     * API-only (callable through {@link #getExecutorKey()} / {@code CN.execute}
     * but invisible in the simulator menu).
     */
    public String getLabel() { return label; }

    /** Invokes the configured static action on the CN1 EDT. */
    public Runnable getInvoke() { return invoke; }

    /** True if this hook should render as a menu item (label is non-empty). */
    public boolean hasMenuLabel() {
        return label != null && label.trim().length() > 0;
    }
}
