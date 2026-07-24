package com.codename1.flutter.semantics;

/**
 * A sort key that orders semantics nodes numerically — Flutter's
 * {@code OrdinalSortKey}. Nodes with a lower {@code order} are traversed
 * first; an optional {@code name} scopes the ordering to a named group. This
 * pass captures the values for API shape; the accessibility traversal order is
 * applied by the semantics layer later.
 */
public class OrdinalSortKey {

    private final double order;
    private String name;

    public OrdinalSortKey(double order) {
        this.order = order;
    }

    public void name(String v) {
        this.name = v;
    }

    public double getOrder() {
        return order;
    }

    public String getName() {
        return name;
    }
}
