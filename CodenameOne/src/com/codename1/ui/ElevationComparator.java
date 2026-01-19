package com.codename1.ui;

import java.util.Comparator;

class ElevationComparator implements Comparator<Component> {
    @Override
    public int compare(Component o1, Component o2) {
        int e1 = o1.getStyle().getElevation();
        int e2 = o2.getStyle().getElevation();
        if (e1 < e2) {
            return -1;
        } else if (e1 > e2) {
            return 1;
        } else {
            return o1.renderedElevationComponentIndex - o2.renderedElevationComponentIndex;
        }
    }
}
