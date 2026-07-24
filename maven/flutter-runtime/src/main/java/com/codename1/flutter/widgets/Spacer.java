package com.codename1.flutter.widgets;

/**
 * Flexible empty space in a {@link Row}/{@link Column} — Flutter's
 * {@code Spacer}. It is an {@link Expanded} wrapping an empty box, so a Flex
 * lays it out by consuming a {@code flex}-proportional share of the free
 * main-axis space. The default flex factor is 1.
 */
public class Spacer extends Expanded {

    public Spacer() {
        child(new SizedBox());
    }
}
