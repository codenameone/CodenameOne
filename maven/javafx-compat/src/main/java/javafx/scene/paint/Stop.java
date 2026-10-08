/*
 * Copyright (c) 2026, Codename One and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.  Codename One designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Oracle in the LICENSE file that accompanied this code.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 2 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 *
 * You should have received a copy of the GNU General Public License version
 * 2 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Please contact Codename One through http://www.codenameone.com/ if you
 * need additional information or have any questions.
 */
package javafx.scene.paint;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import javafx.beans.NamedArg;

/// One colour of a gradient and where along it the colour sits, from 0 at
/// the start to 1 at the end.
public final class Stop {

    private final double offset;
    private final Color color;

    /// Creates a stop.
    public Stop(@NamedArg("offset") double offset, @NamedArg(value = "color", defaultValue = "BLACK") Color color) {
        this.offset = offset;
        this.color = color;
    }

    /// Returns the position, 0 to 1.
    public final double getOffset() {
        return offset;
    }

    /// Returns the colour.
    public final Color getColor() {
        return color;
    }

    /// Returns stops as a gradient uses them: without the ones that have no
    /// colour, sorted, clamped to 0..1 and with a stop at both ends.
    static List<Stop> normalize(List<Stop> stops) {
        ArrayList<Stop> out = new ArrayList<Stop>();
        if (stops != null) {
            for (int i = 0; i < stops.size(); i++) {
                Stop s = stops.get(i);
                if (s == null || s.color == null) {
                    continue;
                }
                double o = s.offset < 0 ? 0 : (s.offset > 1 ? 1 : s.offset);
                Stop clamped = o == s.offset ? s : new Stop(o, s.color);
                int at = out.size();
                while (at > 0 && out.get(at - 1).offset > o) {
                    at--;
                }
                out.add(at, clamped);
            }
        }
        if (out.isEmpty()) {
            out.add(new Stop(0, Color.TRANSPARENT));
            out.add(new Stop(1, Color.TRANSPARENT));
        } else {
            if (out.get(0).offset > 0) {
                out.add(0, new Stop(0, out.get(0).color));
            }
            if (out.get(out.size() - 1).offset < 1) {
                out.add(new Stop(1, out.get(out.size() - 1).color));
            }
        }
        return Collections.unmodifiableList(out);
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this) {
            return true;
        }
        if (obj instanceof Stop) {
            Stop other = (Stop) obj;
            return Double.compare(offset, other.offset) == 0
                    && (color == null ? other.color == null : color.equals(other.color));
        }
        return false;
    }

    @Override
    public int hashCode() {
        long bits = Double.doubleToLongBits(offset);
        return 37 * (int) (bits ^ (bits >>> 32)) + (color == null ? 0 : color.hashCode());
    }

    @Override
    public String toString() {
        return color + " " + offset * 100 + "%";
    }
}
