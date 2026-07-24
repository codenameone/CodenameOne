package com.codename1.flutter.widgets;

/**
 * Orders a focusable subtree by an ascending numeric value — Flutter's
 * {@code NumericFocusOrder}. Lower orders are traversed first.
 */
public class NumericFocusOrder extends FocusOrder {

    private final double order;

    public NumericFocusOrder(double order) {
        this.order = order;
    }

    public double getOrder() {
        return order;
    }
}
