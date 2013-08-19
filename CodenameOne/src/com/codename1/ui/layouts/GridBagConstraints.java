/*
 *  Licensed to the Apache Software Foundation (ASF) under one or more
 *  contributor license agreements.  See the NOTICE file distributed with
 *  this work for additional information regarding copyright ownership.
 *  The ASF licenses this file to You under the Apache License, Version 2.0
 *  (the "License"); you may not use this file except in compliance with
 *  the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package com.codename1.ui.layouts;

/**
 * Port of the GridBag code from Apache's Harmony
 * @author Michael Danilov
 */
public class GridBagConstraints {
    public static final int RELATIVE = -1;
    public static final int REMAINDER = 0;

    public static final int NONE = 0;
    public static final int BOTH = 1;
    public static final int HORIZONTAL = 2;
    public static final int VERTICAL = 3;

    public static final int CENTER = 10;
    public static final int NORTH = 11;
    public static final int NORTHEAST = 12;
    public static final int EAST = 13;
    public static final int SOUTHEAST = 14;
    public static final int SOUTH = 15;
    public static final int SOUTHWEST = 16;
    public static final int WEST = 17;
    public static final int NORTHWEST = 18;

    public static final int PAGE_START = 19;
    public static final int PAGE_END = 20;
    public static final int LINE_START = 21;
    public static final int LINE_END = 22;
    public static final int FIRST_LINE_START = 23;
    public static final int FIRST_LINE_END = 24;
    public static final int LAST_LINE_START = 25;
    public static final int LAST_LINE_END = 26;

    public int gridx;
    public int gridy;
    public int gridwidth;
    public int gridheight;

    public double weightx;
    public double weighty;

    public int anchor;
    public int fill;

    public Insets insets;
    public int ipadx;
    public int ipady;

    public GridBagConstraints(int gridx, int gridy, int gridwidth,
            int gridheight, double weightx, double weighty, int anchor,
            int fill, Insets insets, int ipadx, int ipady)
    {
        this.gridx = gridx;
        this.gridy = gridy;
        this.gridwidth = gridwidth;
        this.gridheight = gridheight;
        this.weightx = weightx;
        this.weighty = weighty;
        this.anchor = anchor;
        this.fill = fill;
        this.insets = (insets == null) ? null:(Insets) insets.clone();
        this.ipadx = ipadx;
        this.ipady = ipady;
    }

    public GridBagConstraints() {
        gridx = RELATIVE;
        gridy = RELATIVE;
        gridwidth = 1;
        gridheight = 1;
        weightx = 0.;
        weighty = 0.;
        anchor = CENTER;
        fill = NONE;
        insets = new Insets(0, 0, 0, 0);
        ipadx = 0;
        ipady = 0;
    }

    public Object clone() {
        return new GridBagConstraints(gridx, gridy, gridwidth, gridheight,
                weightx, weighty, anchor, fill, insets, ipadx, ipady);
    }

    void verify() throws IllegalArgumentException {
        int maxN = GridBagLayout.MAXGRIDSIZE - 1;

        if (((gridx != RELATIVE) && (gridx < 0)) || (gridx >= maxN)) {
            // awt.9C=wrong value of GridBagConstraints: {0}
            throw new IllegalArgumentException("wrong value of GridBagConstraints: " + gridx); //$NON-NLS-1$ //$NON-NLS-2$
        }
        if (((gridy != RELATIVE) && (gridy < 0)) || (gridy >= maxN)) {
            // awt.9C= {0}
            throw new IllegalArgumentException("wrong value of GridBagConstraints: " + gridy); //$NON-NLS-1$  //$NON-NLS-2$
        }
        if (((gridwidth != RELATIVE) && (gridwidth != REMAINDER) && (gridwidth < 0))
                || (gridwidth > maxN))
        {
            // awt.9C={0}
            throw new IllegalArgumentException("wrong value of GridBagConstraints: " + gridwidth); //$NON-NLS-1$ //$NON-NLS-2$
        }
        if (((gridheight != RELATIVE) && (gridheight != REMAINDER) && (gridheight < 0))
                || (gridheight > maxN))
        {
            // awt.9C=
            throw new IllegalArgumentException("wrong value of GridBagConstraints: " + gridheight); //$NON-NLS-1$ //$NON-NLS-2$
        }
        if (((gridx >= 0) || (gridy >= 0))
                && ((gridwidth <= 0) || (gridheight <= 0)))
        {
            throw new IllegalArgumentException("relative grid size parameter goes after absolute grid coordinate"); //$NON-NLS-1$
        }
        if ((gridx != RELATIVE) && ((gridwidth + gridx) > maxN)) {
            throw new IllegalArgumentException("wrong values sum of GridBagConstraints' gridwidth and gridx"); //$NON-NLS-1$
        }
        if ((gridy != RELATIVE) && ((gridheight + gridy) > maxN)) {
            // awt.9F=
            throw new IllegalArgumentException("wrong values sum of GridBagConstraints' gridheight and gridy"); //$NON-NLS-1$
        }
        if ((gridwidth == RELATIVE) && (gridheight == RELATIVE)) {
            // awt.100=
            throw new IllegalArgumentException("component has RELATIVE width and height"); //$NON-NLS-1$
        }

        if (weightx < 0.) {
            // awt.9C= {0}
            throw new IllegalArgumentException("wrong value of GridBagConstraints: " + weightx); //$NON-NLS-1$ //$NON-NLS-2$
        }
        if (weighty < 0.) {
            // awt.9C=wrong value of GridBagConstraints: {0}
            throw new IllegalArgumentException("wrong value of GridBagConstraints: " + weighty); //$NON-NLS-1$ //$NON-NLS-2$
        }

        if ((anchor != CENTER) && (anchor != NORTH) && (anchor != NORTHEAST)
                && (anchor != EAST) && (anchor != SOUTHEAST)
                && (anchor != SOUTH) && (anchor != SOUTHWEST)
                && (anchor != WEST) && (anchor != NORTHWEST)
                && (anchor != PAGE_START) && (anchor != PAGE_END)
                && (anchor != LINE_START) && (anchor != LINE_END)
                && (anchor != FIRST_LINE_START) && (anchor != FIRST_LINE_END)
                && (anchor != LAST_LINE_START) && (anchor != LAST_LINE_END)) {
            // awt.9C=wrong value of GridBagConstraints: {0}
            throw new IllegalArgumentException("wrong value of GridBagConstraints: " + anchor); //$NON-NLS-1$ //$NON-NLS-2$
        }
        if ((fill != NONE) && (fill != HORIZONTAL) && (fill != VERTICAL)
                && (fill != BOTH)) {
            // awt.9C=wrong value of GridBagConstraints: {0}
            throw new IllegalArgumentException("wrong value of GridBagConstraints: " + fill); //$NON-NLS-1$ //$NON-NLS-2$
        }

        if (ipadx < 0) {
            // awt.9C=wrong value of GridBagConstraints: {0}
            throw new IllegalArgumentException("wrong value of GridBagConstraints: " + ipadx); //$NON-NLS-1$ //$NON-NLS-2$
        }
        if (ipady < 0) {
            // awt.9C=wrong value of GridBagConstraints: {0}
            throw new IllegalArgumentException("wrong value of GridBagConstraints: " + ipady); //$NON-NLS-1$ //$NON-NLS-2$
        }
        if ((insets == null) || (insets.left < 0) || (insets.left < 0)
                || (insets.left < 0) || (insets.left < 0)) {
            // awt.9C=wrong value of GridBagConstraints: {0}
            throw new IllegalArgumentException("wrong value of GridBagConstraints: " + insets); //$NON-NLS-1$ //$NON-NLS-2$
        }
    }
}
