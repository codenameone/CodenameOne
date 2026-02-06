package com.codename1.ui.layouts.mig;

import java.util.ArrayList;
/*
 * License (BSD):
 * ==============
 *
 * Copyright (c) 2004, Mikael Grev, MiG InfoCom AB. (miglayout (at) miginfocom (dot) com)
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without modification,
 * are permitted provided that the following conditions are met:
 * Redistributions of source code must retain the above copyright notice, this list
 * of conditions and the following disclaimer.
 * Redistributions in binary form must reproduce the above copyright notice, this
 * list of conditions and the following disclaimer in the documentation and/or other
 * materials provided with the distribution.
 * Neither the name of the MiG InfoCom AB nor the names of its contributors may be
 * used to endorse or promote products derived from this software without specific
 * prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND
 * ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED.
 * IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT,
 * INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING,
 * BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA,
 * OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY,
 * WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY
 * OF SUCH DAMAGE.
 *
 * @version 1.0
 * @author Mikael Grev, MiG InfoCom AB
 *         Date: 2006-sep-08
 */

/// A simple value holder for one component's constraint.
public final class CC {
    static final String[] DOCK_SIDES = {"north", "west", "south", "east"};
    private static final BoundSize DEF_GAP = BoundSize.NULL_SIZE;    // Only used to denote default wrap/newline gap.

    // See the getters and setters for information about the properties below.
    private static final String[] EMPTY_ARR = new String[0];
    private static final CellCall[] INVOKERS = new CellCall[]{
            new CellCall() {
                @Override
                public void cell(CC parent, int val) {
                    parent.setCellX(val);
                }

                @Override
                public void gap(CC parent, String s) {
                    parent.gapLeft(s);
                }
            },
            new CellCall() {
                @Override
                public void cell(CC parent, int val) {
                    parent.setCellY(val);
                }

                @Override
                public void gap(CC parent, String s) {
                    parent.gapRight(s);
                }
            },
            new CellCall() {
                @Override
                public void cell(CC parent, int val) {
                    parent.setSpanX(val);
                }

                @Override
                public void gap(CC parent, String s) {
                    parent.gapTop(s);
                }
            },
            new CellCall() {
                @Override
                public void cell(CC parent, int val) {
                    parent.setSpanY(val);
                }

                @Override
                public void gap(CC parent, String s) {
                    parent.gapBottom(s);
                }
            }
    };
    private int dock = -1;
    private UnitValue[] pos = null; // [x1, y1, x2, y2]
    private UnitValue[] padding = null;   // top, left, bottom, right
    private UnitValue[] visualPadding = null;   // top, left, bottom, right
    private Boolean flowX = null;
    private int skip = 0;
    private int split = 1;
    private int spanX = 1;
    private int spanY = 1;
    private int cellX = -1; // If cellX is -1 then cellY is also considered -1. cellY is never negative.
    private int cellY = 0;
    private String tag = null;
    private String id = null;
    private int hideMode = -1;
    private DimConstraint hor = new DimConstraint();
    private DimConstraint ver = new DimConstraint();
    private BoundSize newline = null;
    private BoundSize wrap = null;
    private boolean boundsInGrid = true;
    private boolean external = false;
    // ***** Tmp cache field
    private Float pushX = null;
    private Float pushY = null;
    private transient String[] linkTargets = null;

    String[] getLinkTargets() {
        if (linkTargets == null) {
            final ArrayList<String> targets = new ArrayList<String>(2);

            if (pos != null) {
                for (UnitValue value : pos) {
                    addLinkTargetIDs(targets, value);
                }
            }

            linkTargets = targets.isEmpty() ? EMPTY_ARR : targets.toArray(new String[targets.size()]);
        }
        return linkTargets;
    }

    // **********************************************************
    // Chaining constraint setters
    // **********************************************************

    private void addLinkTargetIDs(ArrayList<String> targets, UnitValue uv) {
        if (uv != null) {
            String linkId = uv.getLinkTargetId();
            if (linkId != null) {
                targets.add(linkId);
            } else {
                for (int i = uv.getSubUnitCount() - 1; i >= 0; i--) {
                    UnitValue subUv = uv.getSubUnitValue(i);
                    if (subUv.isLinkedDeep()) {
                        addLinkTargetIDs(targets, subUv);
                    }
                }
            }
        }
    }

    /// Specifies that the component should be put in the end group `s` and will thus share the same ending
    /// coordinate as them within the group.
    ///
    /// For a more thorough explanation of what this constraint does see the white paper or cheat Sheet at www.migcomponents.com.
    ///
    /// #### Parameters
    ///
    /// - `s`: A name to associate on the group that should be the same for other rows/columns in the same group.
    ///
    /// #### Returns
    ///
    /// `this` so it is possible to chain calls. E.g. `new ComponentConstraint().noGrid().gap().fill()`.
    public CC endGroupX(String s) {
        hor.setEndGroup(s);
        return this;
    }

    /// Specifies that the component should be put in the size group `s` and will thus share the same size
    /// as them within the group.
    ///
    /// For a more thorough explanation of what this constraint does see the white paper or cheat Sheet at www.migcomponents.com.
    ///
    /// #### Parameters
    ///
    /// - `s`: A name to associate on the group that should be the same for other rows/columns in the same group.
    ///
    /// #### Returns
    ///
    /// `this` so it is possible to chain calls. E.g. `new ComponentConstraint().noGrid().gap().fill()`.
    public CC sizeGroupX(String s) {
        hor.setSizeGroup(s);
        return this;
    }

    /// The minimum size for the component. The value will override any value that is set on the component itself.
    ///
    /// For a more thorough explanation of what this constraint does see the white paper or cheat Sheet at www.migcomponents.com.
    ///
    /// #### Parameters
    ///
    /// - `size`: The size expressed as a `UnitValue`. E.g. "100px" or "200mm".
    ///
    /// #### Returns
    ///
    /// `this` so it is possible to chain calls. E.g. `new ComponentConstraint().noGrid().gap().fill()`.
    public CC minWidth(String size) {
        hor.setSize(LayoutUtil.derive(hor.getSize(), ConstraintParser.parseUnitValue(size, true), null, null));
        return this;
    }

    /// The size for the component as a min and/or preferred and/or maximum size. The value will override any value that is set on
    /// the component itself.
    ///
    /// For a more thorough explanation of what this constraint does see the white paper or cheat Sheet at www.migcomponents.com.
    ///
    /// #### Parameters
    ///
    /// - `size`: The size expressed as a `BoundSize`. E.g. "50:100px:200mm" or "100px".
    ///
    /// #### Returns
    ///
    /// `this` so it is possible to chain calls. E.g. `new ComponentConstraint().noGrid().gap().fill()`.
    public CC width(String size) {
        hor.setSize(ConstraintParser.parseBoundSize(size, false, true));
        return this;
    }

    /// The maximum size for the component. The value will override any value that is set on the component itself.
    ///
    /// For a more thorough explanation of what this constraint does see the white paper or cheat Sheet at www.migcomponents.com.
    ///
    /// #### Parameters
    ///
    /// - `size`: The size expressed as a `UnitValue`. E.g. "100px" or "200mm".
    ///
    /// #### Returns
    ///
    /// `this` so it is possible to chain calls. E.g. `new ComponentConstraint().noGrid().gap().fill()`.
    public CC maxWidth(String size) {
        hor.setSize(LayoutUtil.derive(hor.getSize(), null, null, ConstraintParser.parseUnitValue(size, true)));
        return this;
    }

    /// The horizontal gap before and/or after the component. The gap is towards cell bounds and/or other component bounds.
    ///
    /// For a more thorough explanation of what this constraint does see the white paper or cheat Sheet at www.migcomponents.com.
    ///
    /// #### Parameters
    ///
    /// - `before`: The size of the gap expressed as a `BoundSize`. E.g. "50:100px:200mm" or "100px!".
    ///
    /// - `after`: The size of the gap expressed as a `BoundSize`. E.g. "50:100px:200mm" or "100px!".
    ///
    /// #### Returns
    ///
    /// `this` so it is possible to chain calls. E.g. `new ComponentConstraint().noGrid().gap().fill()`.
    public CC gapX(String before, String after) {
        if (before != null) {
            hor.setGapBefore(ConstraintParser.parseBoundSize(before, true, true));
        }

        if (after != null) {
            hor.setGapAfter(ConstraintParser.parseBoundSize(after, true, true));
        }

        return this;
    }

    /// Same functionality as `getHorizontal().setAlign(ConstraintParser.parseUnitValue(unitValue, true))` only this method
    /// returns `this` for chaining multiple calls.
    ///
    /// For a more thorough explanation of what this constraint does see the white paper or cheat Sheet at www.migcomponents.com.
    ///
    /// #### Parameters
    ///
    /// - `align`: The align keyword or for instance "100px". E.g "left", "right", "leading" or "trailing".
    ///
    /// #### Returns
    ///
    /// `this` so it is possible to chain calls. E.g. `new ComponentConstraint().noGrid().gap().fill()`.
    public CC alignX(String align) {
        hor.setAlign(ConstraintParser.parseUnitValueOrAlign(align, true, null));
        return this;
    }

    /// The grow priority compared to other components in the same cell.
    ///
    /// For a more thorough explanation of what this constraint does see the white paper or cheat Sheet at www.migcomponents.com.
    ///
    /// #### Parameters
    ///
    /// - `p`: The grow priority.
    ///
    /// #### Returns
    ///
    /// `this` so it is possible to chain calls. E.g. `new ComponentConstraint().noGrid().gap().fill()`.
    public CC growPrioX(int p) {
        hor.setGrowPriority(p);
        return this;
    }

    /// Grow priority for the component horizontally and optionally vertically.
    ///
    /// For a more thorough explanation of what this constraint does see the white paper or cheat Sheet at www.migcomponents.com.
    ///
    /// #### Parameters
    ///
    /// - `widthHeight`: The new shrink weight and height. 1-2 arguments, never null.
    ///
    /// #### Returns
    ///
    /// `this` so it is possible to chain calls. E.g. `new ComponentConstraint().noGrid().gap().fill()`.
    ///
    /// #### Since
    ///
    /// 3.7.2
    public CC growPrio(int... widthHeight) {
        if (widthHeight.length == 0 || widthHeight.length > 2) {
            throw new IllegalArgumentException("Illegal argument count: " + widthHeight.length);
        }
        if (widthHeight.length == 2) {
            growPrioY(widthHeight[1]);
        }
        growPrioX(widthHeight[0]);
        return this;
    }

    /// Grow weight for the component horizontally. It default to weight `100`.
    ///
    /// For a more thorough explanation of what this constraint does see the white paper or cheat Sheet at www.migcomponents.com.
    ///
    /// #### Returns
    ///
    /// `this` so it is possible to chain calls. E.g. `new ComponentConstraint().noGrid().gap().fill()`.
    ///
    /// #### See also
    ///
    /// - #growX(float)
    public CC growX() {
        hor.setGrow(ResizeConstraint.WEIGHT_100);
        return this;
    }

    /// Grow weight for the component horizontally.
    ///
    /// For a more thorough explanation of what this constraint does see the white paper or cheat Sheet at www.migcomponents.com.
    ///
    /// #### Parameters
    ///
    /// - `w`: The new grow weight.
    ///
    /// #### Returns
    ///
    /// `this` so it is possible to chain calls. E.g. `new ComponentConstraint().noGrid().gap().fill()`.
    public CC growX(float w) {
        hor.setGrow(Float.valueOf(w));
        return this;
    }

    /// grow weight for the component horizontally and optionally vertically.
    ///
    /// For a more thorough explanation of what this constraint does see the white paper or cheat Sheet at www.migcomponents.com.
    ///
    /// #### Parameters
    ///
    /// - `widthHeight`: The new shrink weight and height. 1-2 arguments, never null.
    ///
    /// #### Returns
    ///
    /// `this` so it is possible to chain calls. E.g. `new ComponentConstraint().noGrid().gap().fill()`.
    ///
    /// #### Since
    ///
    /// 3.7.2
    public CC grow(float... widthHeight) {
        if (widthHeight.length == 0 || widthHeight.length > 2) {
            throw new IllegalArgumentException("Illegal argument count: " + widthHeight.length);
        }
        if (widthHeight.length == 2) {
            growY(widthHeight[1]);
        }
        growX(widthHeight[0]);
        return this;
    }

    /// The shrink priority compared to other components in the same cell.
    ///
    /// For a more thorough explanation of what this constraint does see the white paper or cheat Sheet at www.migcomponents.com.
    ///
    /// #### Parameters
    ///
    /// - `p`: The shrink priority.
    ///
    /// #### Returns
    ///
    /// `this` so it is possible to chain calls. E.g. `new ComponentConstraint().noGrid().gap().fill()`.
    public CC shrinkPrioX(int p) {
        hor.setShrinkPriority(p);
        return this;
    }

    /// Shrink priority for the component horizontally and optionally vertically.
    ///
    /// For a more thorough explanation of what this constraint does see the white paper or cheat Sheet at www.migcomponents.com.
    ///
    /// #### Parameters
    ///
    /// - `widthHeight`: The new shrink weight and height. 1-2 arguments, never null.
    ///
    /// #### Returns
    ///
    /// `this` so it is possible to chain calls. E.g. `new ComponentConstraint().noGrid().gap().fill()`.
    ///
    /// #### Since
    ///
    /// 3.7.2
    public CC shrinkPrio(int... widthHeight) {
        if (widthHeight.length == 0 || widthHeight.length > 2) {
            throw new IllegalArgumentException("Illegal argument count: " + widthHeight.length);
        }
        if (widthHeight.length == 2) {
            shrinkPrioY(widthHeight[1]);
        }
        shrinkPrioX(widthHeight[0]);
        return this;
    }

    /// Shrink weight for the component horizontally.
    ///
    /// For a more thorough explanation of what this constraint does see the white paper or cheat Sheet at www.migcomponents.com.
    ///
    /// #### Parameters
    ///
    /// - `w`: The new shrink weight.
    ///
    /// #### Returns
    ///
    /// `this` so it is possible to chain calls. E.g. `new ComponentConstraint().noGrid().gap().fill()`.
    public CC shrinkX(float w) {
        hor.setShrink(Float.valueOf(w));
        return this;
    }

    /// Shrink weight for the component horizontally and optionally vertically.
    ///
    /// For a more thorough explanation of what this constraint does see the white paper or cheat Sheet at www.migcomponents.com.
    ///
    /// #### Parameters
    ///
    /// - `widthHeight`: The new shrink weight and height. 1-2 arguments, never null.
    ///
    /// #### Returns
    ///
    /// `this` so it is possible to chain calls. E.g. `new ComponentConstraint().noGrid().gap().fill()`.
    ///
    /// #### Since
    ///
    /// 3.7.2
    public CC shrink(float... widthHeight) {
        if (widthHeight.length == 0 || widthHeight.length > 2) {
            throw new IllegalArgumentException("Illegal argument count: " + widthHeight.length);
        }
        if (widthHeight.length == 2) {
            shrinkY(widthHeight[1]);
        }
        shrinkX(widthHeight[0]);
        return this;
    }

    /// The end group that this component should be placed in.
    ///
    /// For a more thorough explanation of what this constraint does see the white paper or cheat Sheet at www.migcomponents.com.
    ///
    /// #### Parameters
    ///
    /// - `s`: The name of the group. If `null` that means no group (default)
    ///
    /// #### Returns
    ///
    /// `this` so it is possible to chain calls. E.g. `new ComponentConstraint().noGrid().gap().fill()`.
    public CC endGroupY(String s) {
        ver.setEndGroup(s);
        return this;
    }

    /// The end group(s) that this component should be placed in.
    ///
    /// For a more thorough explanation of what this constraint does see the white paper or cheat Sheet at www.migcomponents.com.
    ///
    /// #### Parameters
    ///
    /// - `xy`: The end group for x and y respectively. 1-2 arguments, not null.
    ///
    /// #### Returns
    ///
    /// `this` so it is possible to chain calls. E.g. `new ComponentConstraint().noGrid().gap().fill()`.
    ///
    /// #### Since
    ///
    /// 3.7.2
    public CC endGroup(String... xy) {
        if (xy.length > 2 || xy.length == 0) {
            throw new IllegalArgumentException("Illegal argument count: " + xy.length);
        }
        if (xy.length == 2) {
            endGroupY(xy[1]);
        }
        endGroupX(xy[0]);
        return this;
    }

    /// The size group that this component should be placed in.
    ///
    /// For a more thorough explanation of what this constraint does see the white paper or cheat Sheet at www.migcomponents.com.
    ///
    /// #### Parameters
    ///
    /// - `s`: The name of the group. If `null` that means no group (default)
    ///
    /// #### Returns
    ///
    /// `this` so it is possible to chain calls. E.g. `new ComponentConstraint().noGrid().gap().fill()`.
    public CC sizeGroupY(String s) {
        ver.setSizeGroup(s);
        return this;
    }

    /// The size group(s) that this component should be placed in.
    ///
    /// For a more thorough explanation of what this constraint does see the white paper or cheat Sheet at www.migcomponents.com.
    ///
    /// #### Parameters
    ///
    /// - `xy`: The size group for x and y respectively. 1-2 arguments, not null.
    ///
    /// #### Returns
    ///
    /// `this` so it is possible to chain calls. E.g. `new ComponentConstraint().noGrid().gap().fill()`.
    ///
    /// #### Since
    ///
    /// 3.7.2
    public CC sizeGroup(String... xy) {
        if (xy.length > 2 || xy.length == 0) {
            throw new IllegalArgumentException("Illegal argument count: " + xy.length);
        }
        if (xy.length == 2) {
            sizeGroupY(xy[1]);
        }
        sizeGroupX(xy[0]);
        return this;
    }

    /// The minimum size for the component. The value will override any value that is set on the component itself.
    ///
    /// For a more thorough explanation of what this constraint does see the white paper or cheat Sheet at www.migcomponents.com.
    ///
    /// #### Parameters
    ///
    /// - `size`: The size expressed as a `UnitValue`. E.g. "100px" or "200mm".
    ///
    /// #### Returns
    ///
    /// `this` so it is possible to chain calls. E.g. `new ComponentConstraint().noGrid().gap().fill()`.
    public CC minHeight(String size) {
        ver.setSize(LayoutUtil.derive(ver.getSize(), ConstraintParser.parseUnitValue(size, false), null, null));
        return this;
    }

    /// The size for the component as a min and/or preferred and/or maximum size. The value will override any value that is set on
    /// the component itself.
    ///
    /// For a more thorough explanation of what this constraint does see the white paper or cheat Sheet at www.migcomponents.com.
    ///
    /// #### Parameters
    ///
    /// - `size`: The size expressed as a `BoundSize`. E.g. "50:100px:200mm" or "100px".
    ///
    /// #### Returns
    ///
    /// `this` so it is possible to chain calls. E.g. `new ComponentConstraint().noGrid().gap().fill()`.
    public CC height(String size) {
        ver.setSize(ConstraintParser.parseBoundSize(size, false, false));
        return this;
    }

    /// The maximum size for the component. The value will override any value that is set on the component itself.
    ///
    /// For a more thorough explanation of what this constraint does see the white paper or cheat Sheet at www.migcomponents.com.
    ///
    /// #### Parameters
    ///
    /// - `size`: The size expressed as a `UnitValue`. E.g. "100px" or "200mm".
    ///
    /// #### Returns
    ///
    /// `this` so it is possible to chain calls. E.g. `new ComponentConstraint().noGrid().gap().fill()`.
    public CC maxHeight(String size) {
        ver.setSize(LayoutUtil.derive(ver.getSize(), null, null, ConstraintParser.parseUnitValue(size, false)));
        return this;
    }

    /// The vertical gap before (normally above) and/or after (normally below) the component. The gap is towards cell bounds and/or other component bounds.
    ///
    /// For a more thorough explanation of what this constraint does see the white paper or cheat Sheet at www.migcomponents.com.
    ///
    /// #### Parameters
    ///
    /// - `before`: The size of the gap expressed as a `BoundSize`. E.g. "50:100px:200mm" or "100px!".
    ///
    /// - `after`: The size of the gap expressed as a `BoundSize`. E.g. "50:100px:200mm" or "100px!".
    ///
    /// #### Returns
    ///
    /// `this` so it is possible to chain calls. E.g. `new ComponentConstraint().noGrid().gap().fill()`.
    public CC gapY(String before, String after) {
        if (before != null) {
            ver.setGapBefore(ConstraintParser.parseBoundSize(before, true, false));
        }

        if (after != null) {
            ver.setGapAfter(ConstraintParser.parseBoundSize(after, true, false));
        }

        return this;
    }

    /// Same functionality as `getVertical().setAlign(ConstraintParser.parseUnitValue(unitValue, true))` only this method
    /// returns `this` for chaining multiple calls.
    ///
    /// For a more thorough explanation of what this constraint does see the white paper or cheat Sheet at www.migcomponents.com.
    ///
    /// #### Parameters
    ///
    /// - `align`: The align keyword or for instance "100px". E.g "top" or "bottom".
    ///
    /// #### Returns
    ///
    /// `this` so it is possible to chain calls. E.g. `new ComponentConstraint().noGrid().gap().fill()`.
    public CC alignY(String align) {
        ver.setAlign(ConstraintParser.parseUnitValueOrAlign(align, false, null));
        return this;
    }

    /// The grow priority compared to other components in the same cell.
    ///
    /// For a more thorough explanation of what this constraint does see the white paper or cheat Sheet at www.migcomponents.com.
    ///
    /// #### Parameters
    ///
    /// - `p`: The grow priority.
    ///
    /// #### Returns
    ///
    /// `this` so it is possible to chain calls. E.g. `new ComponentConstraint().noGrid().gap().fill()`.
    public CC growPrioY(int p) {
        ver.setGrowPriority(p);
        return this;
    }

    /// Grow weight for the component vertically. Defaults to `100`.
    ///
    /// For a more thorough explanation of what this constraint does see the white paper or cheat Sheet at www.migcomponents.com.
    ///
    /// #### Returns
    ///
    /// `this` so it is possible to chain calls. E.g. `new ComponentConstraint().noGrid().gap().fill()`.
    ///
    /// #### See also
    ///
    /// - #growY(Float)
    public CC growY() {
        ver.setGrow(ResizeConstraint.WEIGHT_100);
        return this;
    }

    /// Grow weight for the component vertically.
    ///
    /// For a more thorough explanation of what this constraint does see the white paper or cheat Sheet at www.migcomponents.com.
    ///
    /// #### Parameters
    ///
    /// - `w`: The new grow weight.
    ///
    /// #### Returns
    ///
    /// `this` so it is possible to chain calls. E.g. `new ComponentConstraint().noGrid().gap().fill()`.
    public CC growY(Float w) {
        ver.setGrow(w);
        return this;
    }

    /// The shrink priority compared to other components in the same cell.
    ///
    /// For a more thorough explanation of what this constraint does see the white paper or cheat Sheet at www.migcomponents.com.
    ///
    /// #### Parameters
    ///
    /// - `p`: The shrink priority.
    ///
    /// #### Returns
    ///
    /// `this` so it is possible to chain calls. E.g. `new ComponentConstraint().noGrid().gap().fill()`.
    public CC shrinkPrioY(int p) {
        ver.setShrinkPriority(p);
        return this;
    }

    /// Shrink weight for the component horizontally.
    ///
    /// For a more thorough explanation of what this constraint does see the white paper or cheat Sheet at www.migcomponents.com.
    ///
    /// #### Parameters
    ///
    /// - `w`: The new shrink weight.
    ///
    /// #### Returns
    ///
    /// `this` so it is possible to chain calls. E.g. `new ComponentConstraint().noGrid().gap().fill()`.
    public CC shrinkY(float w) {
        ver.setShrink(Float.valueOf(w));
        return this;
    }

    /// How this component, if hidden (not visible), should be treated.
    ///
    /// For a more thorough explanation of what this constraint does see the white paper or cheat Sheet at www.migcomponents.com.
    ///
    /// #### Parameters
    ///
    /// - `mode`: @param mode The mode. Default to the mode in the `net.miginfocom.layout.LC`.
    /// 0 == Normal. Bounds will be calculated as if the component was visible.
    ///
    /// 1 == If hidden the size will be 0, 0 but the gaps remain.
    ///
    /// 2 == If hidden the size will be 0, 0 and gaps set to zero.
    ///
    /// 3 == If hidden the component will be disregarded completely and not take up a cell in the grid..
    ///
    /// #### Returns
    ///
    /// `this` so it is possible to chain calls. E.g. `new ComponentConstraint().noGrid().gap().fill()`.
    public CC hideMode(int mode) {
        setHideMode(mode);
        return this;
    }

    /// The id used to reference this component in some constraints.
    ///
    /// For a more thorough explanation of what this constraint does see the white paper or cheat Sheet at www.migcomponents.com.
    ///
    /// #### Parameters
    ///
    /// - `s`: @param s The id or `null`. May consist of a groupID and a componentID which are separated by a dot: ".". E.g. "grp1.id1".
    /// The dot should never be first or last if present.
    ///
    /// #### Returns
    ///
    /// `this` so it is possible to chain calls. E.g. `new ComponentConstraint().noGrid().gap().fill()`.
    public CC id(String s) {
        setId(s);
        return this;
    }

    /// Same functionality as `tag)` only this method returns `this` for chaining multiple calls.
    ///
    /// For a more thorough explanation of what this constraint does see the white paper or cheat Sheet at www.migcomponents.com.
    ///
    /// #### Parameters
    ///
    /// - `tag`: The new tag. May be `null`.
    ///
    /// #### Returns
    ///
    /// `this` so it is possible to chain calls. E.g. `new LayoutConstraint().noGrid().gap().fill()`.
    ///
    /// #### See also
    ///
    /// - #setTag(String)
    public CC tag(String tag) {
        setTag(tag);
        return this;
    }

    /// Set the cell(s) that the component should occupy in the grid. Same functionality as `col)` and
    /// `row)` together with `width)` and `height)`. This method
    /// returns `this` for chaining multiple calls.
    ///
    /// For a more thorough explanation of what this constraint does see the white paper or cheat Sheet at www.migcomponents.com.
    ///
    /// #### Parameters
    ///
    /// - `colRowWidthHeight`: cellX, cellY, spanX, spanY repectively. 1-4 arguments, not null.
    ///
    /// #### Returns
    ///
    /// `this` so it is possible to chain calls. E.g. `new LayoutConstraint().noGrid().gap().fill()`.
    ///
    /// #### Since
    ///
    /// 3.7.2. Replacing cell(int, int) and cell(int, int, int, int)
    ///
    /// #### See also
    ///
    /// - #setCellX(int)
    ///
    /// - #setCellY(int)
    ///
    /// - #setSpanX(int)
    ///
    /// - #setSpanY(int)
    public CC cell(int... colRowWidthHeight) {
        if (colRowWidthHeight.length > INVOKERS.length) {
            throw new IllegalArgumentException("Illegal argument count: " + colRowWidthHeight.length);
        }
        for (int iter = 0; iter < colRowWidthHeight.length; iter++) {
            INVOKERS[iter].cell(this, colRowWidthHeight[iter]);
        }
        return this;
    }

    /// Same functionality as `spanX(cellsX).spanY(cellsY)` which means this cell will span cells in both x and y.
    /// This method returns `this` for chaining multiple calls.
    ///
    /// For a more thorough explanation of what this constraint does see the white paper or cheat Sheet at www.migcomponents.com.
    /// Since 3.7.2 this takes an array/vararg whereas it previously only took two specific values, xSpan and ySpan.
    ///
    /// #### Parameters
    ///
    /// - `cells`: spanX and spanY, when present, and in that order.
    ///
    /// #### Returns
    ///
    /// `this` so it is possible to chain calls. E.g. `new LayoutConstraint().noGrid().gap().fill()`.
    ///
    /// #### Since
    ///
    /// 3.7.2 Replaces span(int, int).
    ///
    /// #### See also
    ///
    /// - #setSpanY(int)
    ///
    /// - #setSpanX(int)
    ///
    /// - #spanY()
    ///
    /// - #spanX()
    public CC span(int... cells) {
        if (cells == null || cells.length == 0) {
            setSpanX(LayoutUtil.INF);
            setSpanY(1);
        } else if (cells.length == 1) {
            setSpanX(cells[0]);
            setSpanY(1);
        } else {
            setSpanX(cells[0]);
            setSpanY(cells[1]);
        }
        return this;
    }

    /// Corresponds exactly to the "gap left right top bottom" keyword.
    ///
    /// #### Parameters
    ///
    /// - `args`: Same as for the "gap" keyword. Length 1-4, never null buf elements can be null.
    ///
    /// #### Returns
    ///
    /// `this` so it is possible to chain calls. E.g. `new LayoutConstraint().noGrid().gap().fill()`.
    ///
    /// #### Since
    ///
    /// 3.7.2
    public CC gap(String... args) {
        if (INVOKERS.length < args.length) {
            throw new IllegalArgumentException("Illegal argument count: " + args.length);
        }
        for (int iter = 0; iter < args.length; iter++) {
            INVOKERS[iter].gap(this, args[iter]);
        }
        return this;
    }

    /// Sets the horizontal gap before the component.
    ///
    /// Note! This is currently same as gapLeft(). This might change in 4.x.
    ///
    /// #### Parameters
    ///
    /// - `boundsSize`: The size of the gap expressed as a `BoundSize`. E.g. "50:100px:200mm" or "100px!".
    ///
    /// #### Returns
    ///
    /// `this` so it is possible to chain calls. E.g. `new LayoutConstraint().noGrid().gap().fill()`.
    ///
    /// #### Since
    ///
    /// 3.7.2
    public CC gapBefore(String boundsSize) {
        hor.setGapBefore(ConstraintParser.parseBoundSize(boundsSize, true, true));
        return this;
    }

    /// Sets the horizontal gap after the component.
    ///
    /// Note! This is currently same as gapLeft(). This might change in 4.x.
    ///
    /// #### Parameters
    ///
    /// - `boundsSize`: The size of the gap expressed as a `BoundSize`. E.g. "50:100px:200mm" or "100px!".
    ///
    /// #### Returns
    ///
    /// `this` so it is possible to chain calls. E.g. `new LayoutConstraint().noGrid().gap().fill()`.
    ///
    /// #### Since
    ///
    /// 3.7.2
    public CC gapAfter(String boundsSize) {
        hor.setGapAfter(ConstraintParser.parseBoundSize(boundsSize, true, true));
        return this;
    }

    /// Sets the gap above the component.
    ///
    /// #### Parameters
    ///
    /// - `boundsSize`: The size of the gap expressed as a `BoundSize`. E.g. "50:100px:200mm" or "100px!".
    ///
    /// #### Returns
    ///
    /// `this` so it is possible to chain calls. E.g. `new LayoutConstraint().noGrid().gap().fill()`.
    ///
    /// #### Since
    ///
    /// 3.7.2
    public CC gapTop(String boundsSize) {
        ver.setGapBefore(ConstraintParser.parseBoundSize(boundsSize, true, false));
        return this;
    }

    /// Sets the gap to the left the component.
    ///
    /// #### Parameters
    ///
    /// - `boundsSize`: The size of the gap expressed as a `BoundSize`. E.g. "50:100px:200mm" or "100px!".
    ///
    /// #### Returns
    ///
    /// `this` so it is possible to chain calls. E.g. `new LayoutConstraint().noGrid().gap().fill()`.
    ///
    /// #### Since
    ///
    /// 3.7.2
    public CC gapLeft(String boundsSize) {
        hor.setGapBefore(ConstraintParser.parseBoundSize(boundsSize, true, true));
        return this;
    }

    /// Sets the gap below the component.
    ///
    /// #### Parameters
    ///
    /// - `boundsSize`: The size of the gap expressed as a `BoundSize`. E.g. "50:100px:200mm" or "100px!".
    ///
    /// #### Returns
    ///
    /// `this` so it is possible to chain calls. E.g. `new LayoutConstraint().noGrid().gap().fill()`.
    ///
    /// #### Since
    ///
    /// 3.7.2
    public CC gapBottom(String boundsSize) {
        ver.setGapAfter(ConstraintParser.parseBoundSize(boundsSize, true, false));
        return this;
    }

    /// Sets the gap to the right of the component.
    ///
    /// #### Parameters
    ///
    /// - `boundsSize`: The size of the gap expressed as a `BoundSize`. E.g. "50:100px:200mm" or "100px!".
    ///
    /// #### Returns
    ///
    /// `this` so it is possible to chain calls. E.g. `new LayoutConstraint().noGrid().gap().fill()`.
    ///
    /// #### Since
    ///
    /// 3.7.2
    public CC gapRight(String boundsSize) {
        hor.setGapAfter(ConstraintParser.parseBoundSize(boundsSize, true, true));
        return this;
    }

    /// Same functionality as calling `#setSpanY(int)` with `LayoutUtil.INF` which means this cell will span the rest of the column.
    /// This method returns `this` for chaining multiple calls.
    ///
    /// For a more thorough explanation of what this constraint does see the white paper or cheat Sheet at www.migcomponents.com.
    ///
    /// #### Returns
    ///
    /// `this` so it is possible to chain calls. E.g. `new LayoutConstraint().noGrid().gap().fill()`.
    ///
    /// #### See also
    ///
    /// - #setSpanY(int)
    ///
    /// - #spanY()
    public CC spanY() {
        return spanY(LayoutUtil.INF);
    }

    /// Same functionality as `#setSpanY(int)` only this method returns `this` for chaining multiple calls.
    ///
    /// For a more thorough explanation of what this constraint does see the white paper or cheat Sheet at www.migcomponents.com.
    ///
    /// #### Parameters
    ///
    /// - `cells`: The number of cells to span (i.e. merge).
    ///
    /// #### Returns
    ///
    /// `this` so it is possible to chain calls. E.g. `new LayoutConstraint().noGrid().gap().fill()`.
    ///
    /// #### See also
    ///
    /// - #setSpanY(int)
    public CC spanY(int cells) {
        setSpanY(cells);
        return this;
    }

    /// Same functionality as `#setSpanX(int)` which means this cell will span the rest of the row.
    /// This method returns `this` for chaining multiple calls.
    ///
    /// For a more thorough explanation of what this constraint does see the white paper or cheat Sheet at www.migcomponents.com.
    ///
    /// #### Returns
    ///
    /// `this` so it is possible to chain calls. E.g. `new LayoutConstraint().noGrid().gap().fill()`.
    ///
    /// #### See also
    ///
    /// - #setSpanX(int)
    ///
    /// - #spanX()
    public CC spanX() {
        return spanX(LayoutUtil.INF);
    }

    /// Same functionality as `#setSpanX(int)` only this method returns `this` for chaining multiple calls.
    ///
    /// For a more thorough explanation of what this constraint does see the white paper or cheat Sheet at www.migcomponents.com.
    ///
    /// #### Parameters
    ///
    /// - `cells`: The number of cells to span (i.e. merge).
    ///
    /// #### Returns
    ///
    /// `this` so it is possible to chain calls. E.g. `new LayoutConstraint().noGrid().gap().fill()`.
    ///
    /// #### See also
    ///
    /// - #setSpanY(int)
    public CC spanX(int cells) {
        setSpanX(cells);
        return this;
    }

    /// Same functionality as `pushX().pushY()` which means this cell will push in both x and y dimensions.
    /// This method returns `this` for chaining multiple calls.
    ///
    /// For a more thorough explanation of what this constraint does see the white paper or cheat Sheet at www.migcomponents.com.
    ///
    /// #### Returns
    ///
    /// `this` so it is possible to chain calls. E.g. `new LayoutConstraint().noGrid().gap().fill()`.
    ///
    /// #### See also
    ///
    /// - #setPushX(Float)
    ///
    /// - #setPushX(Float)
    ///
    /// - #pushY()
    ///
    /// - #pushX()
    public CC push() {
        return pushX().pushY();
    }

    /// Same functionality as `pushX(weightX).pushY(weightY)` which means this cell will push in both x and y dimensions.
    /// This method returns `this` for chaining multiple calls.
    ///
    /// For a more thorough explanation of what this constraint does see the white paper or cheat Sheet at www.migcomponents.com.
    ///
    /// #### Parameters
    ///
    /// - `weightX`: The weight used in the push.
    ///
    /// - `weightY`: The weight used in the push.
    ///
    /// #### Returns
    ///
    /// `this` so it is possible to chain calls. E.g. `new LayoutConstraint().noGrid().gap().fill()`.
    ///
    /// #### See also
    ///
    /// - #setPushY(Float)
    ///
    /// - #setPushX(Float)
    ///
    /// - #pushY()
    ///
    /// - #pushX()
    public CC push(Float weightX, Float weightY) {
        return pushX(weightX).pushY(weightY);
    }

    /// Same functionality as `#setPushY(Float)` which means this cell will push the rest of the column.
    /// This method returns `this` for chaining multiple calls.
    ///
    /// For a more thorough explanation of what this constraint does see the white paper or cheat Sheet at www.migcomponents.com.
    ///
    /// #### Returns
    ///
    /// `this` so it is possible to chain calls. E.g. `new LayoutConstraint().noGrid().gap().fill()`.
    ///
    /// #### See also
    ///
    /// - #setPushY(Float)
    public CC pushY() {
        return pushY(ResizeConstraint.WEIGHT_100);
    }

    /// Same functionality as `weight)` only this method returns `this` for chaining multiple calls.
    ///
    /// For a more thorough explanation of what this constraint does see the white paper or cheat Sheet at www.migcomponents.com.
    ///
    /// #### Parameters
    ///
    /// - `weight`: The weight used in the push.
    ///
    /// #### Returns
    ///
    /// `this` so it is possible to chain calls. E.g. `new LayoutConstraint().noGrid().gap().fill()`.
    ///
    /// #### See also
    ///
    /// - #setPushY(Float)
    public CC pushY(Float weight) {
        setPushY(weight);
        return this;
    }

    /// Same functionality as `#setPushX(Float)` which means this cell will push the rest of the row.
    /// This method returns `this` for chaining multiple calls.
    ///
    /// For a more thorough explanation of what this constraint does see the white paper or cheat Sheet at www.migcomponents.com.
    ///
    /// #### Returns
    ///
    /// `this` so it is possible to chain calls. E.g. `new LayoutConstraint().noGrid().gap().fill()`.
    ///
    /// #### See also
    ///
    /// - #setPushX(Float)
    public CC pushX() {
        return pushX(ResizeConstraint.WEIGHT_100);
    }

    /// Same functionality as `weight)` only this method returns `this` for chaining multiple calls.
    ///
    /// For a more thorough explanation of what this constraint does see the white paper or cheat Sheet at www.migcomponents.com.
    ///
    /// #### Parameters
    ///
    /// - `weight`: The weight used in the push.
    ///
    /// #### Returns
    ///
    /// `this` so it is possible to chain calls. E.g. `new LayoutConstraint().noGrid().gap().fill()`.
    ///
    /// #### See also
    ///
    /// - #setPushY(Float)
    public CC pushX(Float weight) {
        setPushX(weight);
        return this;
    }

    /// Same functionality as `parts)` only this method returns `this` for chaining multiple calls.
    ///
    /// For a more thorough explanation of what this constraint does see the white paper or cheat Sheet at www.migcomponents.com.
    ///
    /// #### Parameters
    ///
    /// - `parts`: The number of parts (i.e. component slots) the cell should be divided into.
    ///
    /// #### Returns
    ///
    /// `this` so it is possible to chain calls. E.g. `new ComponentConstraint().noGrid().gap().fill()`.
    ///
    /// #### See also
    ///
    /// - #setSplit(int)
    public CC split(int parts) {
        setSplit(parts);
        return this;
    }

    /// Same functionality as split(LayoutUtil.INF), which means split until one of the keywords that breaks the split is found for
    /// a component after this one (e.g. wrap, newline and skip).
    ///
    /// For a more thorough explanation of what this constraint does see the white paper or cheat Sheet at www.migcomponents.com.
    ///
    /// #### Returns
    ///
    /// `this` so it is possible to chain calls. E.g. `new ComponentConstraint().noGrid().gap().fill()`.
    ///
    /// #### Since
    ///
    /// 3.7.2
    ///
    /// #### See also
    ///
    /// - #setSplit(int)
    public CC split() {
        setSplit(LayoutUtil.INF);
        return this;
    }

    /// Same functionality as `#setSkip(int)` only this method returns `this` for chaining multiple calls.
    ///
    /// For a more thorough explanation of what this constraint does see the white paper or cheat Sheet at www.migcomponents.com.
    ///
    /// #### Parameters
    ///
    /// - `cells`: How many cells in the grid that should be skipped **before** the component that this constraint belongs to
    ///
    /// #### Returns
    ///
    /// `this` so it is possible to chain calls. E.g. `new ComponentConstraint().noGrid().gap().fill()`.
    ///
    /// #### See also
    ///
    /// - #setSkip(int)
    public CC skip(int cells) {
        setSkip(cells);
        return this;
    }

    /// Same functionality as skip(1).
    ///
    /// For a more thorough explanation of what this constraint does see the white paper or cheat Sheet at www.migcomponents.com.
    ///
    /// #### Returns
    ///
    /// `this` so it is possible to chain calls. E.g. `new ComponentConstraint().noGrid().gap().fill()`.
    ///
    /// #### Since
    ///
    /// 3.7.2
    ///
    /// #### See also
    ///
    /// - #setSkip(int)
    public CC skip() {
        setSkip(1);
        return this;
    }

    /// Same functionality as calling `#setExternal(boolean)` with `true` only this method returns `this` for chaining multiple calls.
    ///
    /// For a more thorough explanation of what this constraint does see the white paper or cheat Sheet at www.migcomponents.com.
    ///
    /// #### Returns
    ///
    /// `this` so it is possible to chain calls. E.g. `new ComponentConstraint().noGrid().gap().fill()`.
    ///
    /// #### See also
    ///
    /// - #setExternal(boolean)
    public CC external() {
        setExternal(true);
        return this;
    }

    /// Same functionality as calling `#setFlowX(Boolean)` with `Boolean.TRUE` only this method returns `this` for chaining multiple calls.
    ///
    /// For a more thorough explanation of what this constraint does see the white paper or cheat Sheet at www.migcomponents.com.
    ///
    /// #### Returns
    ///
    /// `this` so it is possible to chain calls. E.g. `new ComponentConstraint().noGrid().gap().fill()`.
    ///
    /// #### See also
    ///
    /// - #setFlowX(Boolean)
    public CC flowX() {
        setFlowX(Boolean.TRUE);
        return this;
    }

    /// Same functionality as calling `#setFlowX(Boolean)` with `Boolean.FALSE` only this method returns `this` for chaining multiple calls.
    ///
    /// For a more thorough explanation of what this constraint does see the white paper or cheat Sheet at www.migcomponents.com.
    ///
    /// #### Returns
    ///
    /// `this` so it is possible to chain calls. E.g. `new ComponentConstraint().noGrid().gap().fill()`.
    ///
    /// #### See also
    ///
    /// - #setFlowX(Boolean)
    public CC flowY() {
        setFlowX(Boolean.FALSE);
        return this;
    }

    /// Same functionality as `#growX()` and `#growY()`.
    ///
    /// For a more thorough explanation of what this constraint does see the white paper or cheat Sheet at www.migcomponents.com.
    ///
    /// #### Returns
    ///
    /// `this` so it is possible to chain calls. E.g. `new ComponentConstraint().noGrid().gap().fill()`.
    ///
    /// #### See also
    ///
    /// - #growX()
    ///
    /// - #growY()
    public CC grow() {
        growX();
        growY();
        return this;
    }

    /// Same functionality as calling `#setNewline(boolean)` with `true` only this method returns `this` for chaining multiple calls.
    ///
    /// For a more thorough explanation of what this constraint does see the white paper or cheat Sheet at www.migcomponents.com.
    ///
    /// #### Returns
    ///
    /// `this` so it is possible to chain calls. E.g. `new ComponentConstraint().noGrid().gap().fill()`.
    ///
    /// #### See also
    ///
    /// - #setNewline(boolean)
    public CC newline() {
        setNewline(true);
        return this;
    }

    /// Same functionality as `#setNewlineGapSize(BoundSize)` only this method returns `this` for chaining multiple calls.
    ///
    /// For a more thorough explanation of what this constraint does see the white paper or cheat Sheet at www.migcomponents.com.
    ///
    /// #### Parameters
    ///
    /// - `gapSize`: @param gapSize The gap size that will override the gap size in the row/colum constraints if `!= null`. E.g. "5px" or "unrel".
    /// If `null` or `""` the newline size will be set to the default size and turned on. This is different compared to
    /// `#setNewlineGapSize(BoundSize)`.
    ///
    /// #### Returns
    ///
    /// `this` so it is possible to chain calls. E.g. `new ComponentConstraint().noGrid().gap().fill()`.
    ///
    /// #### See also
    ///
    /// - #setNewlineGapSize(BoundSize)
    public CC newline(String gapSize) {
        BoundSize bs = ConstraintParser.parseBoundSize(gapSize, true, (flowX != null && !flowX));
        if (bs != null) {
            setNewlineGapSize(bs);
        } else {
            setNewline(true);
        }
        return this;
    }

    /// Same functionality as calling `#setWrap(boolean)` with `true` only this method returns `this` for chaining multiple calls.
    ///
    /// For a more thorough explanation of what this constraint does see the white paper or cheat Sheet at www.migcomponents.com.
    ///
    /// #### Returns
    ///
    /// `this` so it is possible to chain calls. E.g. `new ComponentConstraint().noGrid().gap().fill()`.
    ///
    /// #### See also
    ///
    /// - #setWrap(boolean)
    public CC wrap() {
        setWrap(true);
        return this;
    }

    /// Same functionality as `#setWrapGapSize(BoundSize)` only this method returns `this` for chaining multiple calls.
    ///
    /// For a more thorough explanation of what this constraint does see the white paper or cheat Sheet at www.migcomponents.com.
    ///
    /// #### Parameters
    ///
    /// - `gapSize`: @param gapSize The gap size that will override the gap size in the row/colum constraints if `!= null`. E.g. "5px" or "unrel".
    /// If `null` or `""` the wrap size will be set to the default size and turned on. This is different compared to
    /// `#setWrapGapSize(BoundSize)`.
    ///
    /// #### Returns
    ///
    /// `this` so it is possible to chain calls. E.g. `new ComponentConstraint().noGrid().gap().fill()`.
    ///
    /// #### See also
    ///
    /// - #setWrapGapSize(BoundSize)
    public CC wrap(String gapSize) {
        BoundSize bs = ConstraintParser.parseBoundSize(gapSize, true, (flowX != null && !flowX));
        if (bs != null) {
            setWrapGapSize(bs);
        } else {
            setWrap(true);
        }
        return this;
    }

    /// Same functionality as calling `#setDockSide(int)` with `0` only this method returns `this` for chaining multiple calls.
    ///
    /// For a more thorough explanation of what this constraint does see the white paper or cheat Sheet at www.migcomponents.com.
    ///
    /// #### Returns
    ///
    /// `this` so it is possible to chain calls. E.g. `new ComponentConstraint().noGrid().gap().fill()`.
    ///
    /// #### See also
    ///
    /// - #setDockSide(int)
    public CC dockNorth() {
        setDockSide(0);
        return this;
    }

    /// Same functionality as calling `#setDockSide(int)` with `1` only this method returns `this` for chaining multiple calls.
    ///
    /// For a more thorough explanation of what this constraint does see the white paper or cheat Sheet at www.migcomponents.com.
    ///
    /// #### Returns
    ///
    /// `this` so it is possible to chain calls. E.g. `new ComponentConstraint().noGrid().gap().fill()`.
    ///
    /// #### See also
    ///
    /// - #setDockSide(int)
    public CC dockWest() {
        setDockSide(1);
        return this;
    }

    /// Same functionality as calling `#setDockSide(int)` with `2` only this method returns `this` for chaining multiple calls.
    ///
    /// For a more thorough explanation of what this constraint does see the white paper or cheat Sheet at www.migcomponents.com.
    ///
    /// #### Returns
    ///
    /// `this` so it is possible to chain calls. E.g. `new ComponentConstraint().noGrid().gap().fill()`.
    ///
    /// #### See also
    ///
    /// - #setDockSide(int)
    public CC dockSouth() {
        setDockSide(2);
        return this;
    }

    /// Same functionality as calling `#setDockSide(int)` with `3` only this method returns `this` for chaining multiple calls.
    ///
    /// For a more thorough explanation of what this constraint does see the white paper or cheat Sheet at www.migcomponents.com.
    ///
    /// #### Returns
    ///
    /// `this` so it is possible to chain calls. E.g. `new ComponentConstraint().noGrid().gap().fill()`.
    ///
    /// #### See also
    ///
    /// - #setDockSide(int)
    public CC dockEast() {
        setDockSide(3);
        return this;
    }

    /// Sets the x-coordinate for the component. This is used to set the x coordinate position to a specific value. The component
    /// bounds is still precalculated to the grid cell and this method should be seen as a way to correct the x position.
    ///
    /// For a more thorough explanation of what this constraint does see the white paper or cheat Sheet at www.migcomponents.com.
    ///
    /// #### Parameters
    ///
    /// - `x`: The x position as a UnitValue. E.g. "10" or "40mm" or "container.x+10".
    ///
    /// #### Returns
    ///
    /// `this` so it is possible to chain calls. E.g. `new ComponentConstraint().noGrid().gap().fill()`.
    ///
    /// #### See also
    ///
    /// - #setPos(UnitValue[])
    ///
    /// - #setBoundsInGrid(boolean)
    public CC x(String x) {
        return corrPos(x, 0);
    }

    /// Sets the y-coordinate for the component. This is used to set the y coordinate position to a specific value. The component
    /// bounds is still precalculated to the grid cell and this method should be seen as a way to correct the y position.
    ///
    /// For a more thorough explanation of what this constraint does see the white paper or cheat Sheet at www.migcomponents.com.
    ///
    /// #### Parameters
    ///
    /// - `y`: The y position as a UnitValue. E.g. "10" or "40mm" or "container.x+10".
    ///
    /// #### Returns
    ///
    /// `this` so it is possible to chain calls. E.g. `new ComponentConstraint().noGrid().gap().fill()`.
    ///
    /// #### See also
    ///
    /// - #setPos(UnitValue[])
    ///
    /// - #setBoundsInGrid(boolean)
    public CC y(String y) {
        return corrPos(y, 1);
    }

    /// Sets the x2-coordinate for the component (right side). This is used to set the x2 coordinate position to a specific value. The component
    /// bounds is still precalculated to the grid cell and this method should be seen as a way to correct the x position.
    ///
    /// For a more thorough explanation of what this constraint does see the white paper or cheat Sheet at www.migcomponents.com.
    ///
    /// #### Parameters
    ///
    /// - `x2`: The x2 side's position as a UnitValue. E.g. "10" or "40mm" or "container.x2 - 10".
    ///
    /// #### Returns
    ///
    /// `this` so it is possible to chain calls. E.g. `new ComponentConstraint().noGrid().gap().fill()`.
    ///
    /// #### See also
    ///
    /// - #setPos(UnitValue[])
    ///
    /// - #setBoundsInGrid(boolean)
    public CC x2(String x2) {
        return corrPos(x2, 2);
    }

    /// Sets the y2-coordinate for the component (bottom side). This is used to set the y2 coordinate position to a specific value. The component
    /// bounds is still precalculated to the grid cell and this method should be seen as a way to correct the y position.
    ///
    /// For a more thorough explanation of what this constraint does see the white paper or cheat Sheet at www.migcomponents.com.
    ///
    /// #### Parameters
    ///
    /// - `y2`: The y2 side's position as a UnitValue. E.g. "10" or "40mm" or "container.x2 - 10".
    ///
    /// #### Returns
    ///
    /// `this` so it is possible to chain calls. E.g. `new ComponentConstraint().noGrid().gap().fill()`.
    ///
    /// #### See also
    ///
    /// - #setPos(UnitValue[])
    ///
    /// - #setBoundsInGrid(boolean)
    public CC y2(String y2) {
        return corrPos(y2, 3);
    }

    private CC corrPos(String uv, int ix) {
        UnitValue[] b = getPos();
        if (b == null) {
            b = new UnitValue[4];
        }

        b[ix] = ConstraintParser.parseUnitValue(uv, (ix % 2 == 0));
        setPos(b);

        setBoundsInGrid(true);
        return this;
    }

    /// Same functionality as `x)` and `y)` toghether.
    ///
    /// For a more thorough explanation of what this constraint does see the white paper or cheat Sheet at www.migcomponents.com.
    ///
    /// #### Parameters
    ///
    /// - `x`: The x position as a UnitValue. E.g. "10" or "40mm" or "container.x+10".
    ///
    /// - `y`: The y position as a UnitValue. E.g. "10" or "40mm" or "container.x+10".
    ///
    /// #### Returns
    ///
    /// `this` so it is possible to chain calls. E.g. `new ComponentConstraint().noGrid().gap().fill()`.
    ///
    /// #### See also
    ///
    /// - #setPos(UnitValue[])
    public CC pos(String x, String y) {
        UnitValue[] b = getPos();
        if (b == null) {
            b = new UnitValue[4];
        }

        b[0] = ConstraintParser.parseUnitValue(x, true);
        b[1] = ConstraintParser.parseUnitValue(y, false);
        setPos(b);

        setBoundsInGrid(false);
        return this;
    }

    /// Same functionality as `x)`, `y)`, `y)` and `y)` toghether.
    ///
    /// For a more thorough explanation of what this constraint does see the white paper or cheat Sheet at www.migcomponents.com.
    ///
    /// #### Parameters
    ///
    /// - `x`: The x position as a UnitValue. E.g. "10" or "40mm" or "container.x+10".
    ///
    /// - `y`: The y position as a UnitValue. E.g. "10" or "40mm" or "container.x+10".
    ///
    /// - `x2`: The x2 side's position as a UnitValue. E.g. "10" or "40mm" or "container.x2 - 10".
    ///
    /// - `y2`: The y2 side's position as a UnitValue. E.g. "10" or "40mm" or "container.x2 - 10".
    ///
    /// #### Returns
    ///
    /// `this` so it is possible to chain calls. E.g. `new ComponentConstraint().noGrid().gap().fill()`.
    ///
    /// #### See also
    ///
    /// - #setPos(UnitValue[])
    public CC pos(String x, String y, String x2, String y2) {
        setPos(new UnitValue[]{
                ConstraintParser.parseUnitValue(x, true),
                ConstraintParser.parseUnitValue(y, false),
                ConstraintParser.parseUnitValue(x2, true),
                ConstraintParser.parseUnitValue(y2, false),
        });
        setBoundsInGrid(false);
        return this;
    }

    /// Same functionality as `#setPadding(UnitValue[])` but the unit values as absolute pixels. This method returns `this` for chaining multiple calls.
    ///
    /// For a more thorough explanation of what this constraint does see the white paper or cheat Sheet at www.migcomponents.com.
    ///
    /// #### Parameters
    ///
    /// - `top`: The top padding that will be added to the y coordinate at the last stage in the layout.
    ///
    /// - `left`: The top padding that will be added to the x coordinate at the last stage in the layout.
    ///
    /// - `bottom`: The top padding that will be added to the y2 coordinate at the last stage in the layout.
    ///
    /// - `right`: The top padding that will be added to the x2 coordinate at the last stage in the layout.
    ///
    /// #### Returns
    ///
    /// `this` so it is possible to chain calls. E.g. `new ComponentConstraint().noGrid().gap().fill()`.
    ///
    /// #### See also
    ///
    /// - #setTag(String)
    public CC pad(int top, int left, int bottom, int right) {
        setPadding(new UnitValue[]{
                new UnitValue(top), new UnitValue(left), new UnitValue(bottom), new UnitValue(right)
        });
        return this;
    }

    /// Same functionality as `setPadding(ConstraintParser.parseInsets(pad, false))}` only this method returns `this` for chaining multiple calls.
    ///
    /// For a more thorough explanation of what this constraint does see the white paper or cheat Sheet at www.migcomponents.com.
    ///
    /// #### Parameters
    ///
    /// - `pad`: The string to parse. E.g. "10 10 10 10" or "20". If less than 4 groups the last will be used for the missing.
    ///
    /// #### Returns
    ///
    /// `this` so it is possible to chain calls. E.g. `new ComponentConstraint().noGrid().gap().fill()`.
    ///
    /// #### See also
    ///
    /// - #setTag(String)
    public CC pad(String pad) {
        setPadding(pad != null ? ConstraintParser.parseInsets(pad, false) : null);
        return this;
    }

    /// Returns the horizontal dimension constraint for this component constraint. It has constraints for the horizontal size
    /// and grow/shink priorities and weights.
    ///
    /// Note! If any changes is to be made it must be made direct when the object is returned. It is not allowed to save the
    /// constraint for later use.
    ///
    /// #### Returns
    ///
    /// The current dimension constraint. Never `null`.
    public DimConstraint getHorizontal() {
        return hor;
    }

    // **********************************************************
    // Bean properties
    // **********************************************************

    /// Sets the horizontal dimension constraint for this component constraint. It has constraints for the horizontal size
    /// and grow/shrink priorities and weights.
    ///
    /// #### Parameters
    ///
    /// - `h`: The new dimension constraint. If `null` it will be reset to `new DimConstraint();`
    public void setHorizontal(DimConstraint h) {
        hor = h != null ? h : new DimConstraint();
    }

    /// Returns the vertical dimension constraint for this component constraint. It has constraints for the vertical size
    /// and grow/shrink priorities and weights.
    ///
    /// Note! If any changes is to be made it must be made direct when the object is returned. It is not allowed to save the
    /// constraint for later use.
    ///
    /// #### Returns
    ///
    /// The current dimension constraint. Never `null`.
    public DimConstraint getVertical() {
        return ver;
    }

    /// Sets the vertical dimension constraint for this component constraint. It has constraints for the vertical size
    /// and grow/shrink priorities and weights.
    ///
    /// #### Parameters
    ///
    /// - `v`: The new dimension constraint. If `null` it will be reset to `new DimConstraint();`
    public void setVertical(DimConstraint v) {
        ver = v != null ? v : new DimConstraint();
    }

    /// Returns the vertical or horizontal dim constraint.
    ///
    /// Note! If any changes is to be made it must be made direct when the object is returned. It is not allowed to save the
    /// constraint for later use.
    ///
    /// #### Parameters
    ///
    /// - `isHor`: If the horizontal constraint should be returned.
    ///
    /// #### Returns
    ///
    /// The dim constraint. Never `null`.
    public DimConstraint getDimConstraint(boolean isHor) {
        return isHor ? hor : ver;
    }

    /// Returns the absolute positioning of one or more of the edges. This will be applied last in the layout cycle and will not
    /// affect the flow or grid positions. The positioning is relative to the parent and can not (as padding) be used
    /// to adjust the edges relative to the old value. May be `null` and elements may be `null`.
    /// `null` value(s) for the x2 and y2 will be interpreted as to keep the preferred size and thus the x1
    /// and x2 will just absolutely positions the component.
    ///
    /// Note that `#setBoundsInGrid(boolean)` changes the interpretation of thisproperty slightly.
    ///
    /// For a more thorough explanation of what this constraint does see the white paper or cheat Sheet at www.migcomponents.com.
    ///
    /// #### Returns
    ///
    /// The current value as a new array, free to modify.
    public UnitValue[] getPos() {
        return pos != null ? new UnitValue[]{pos[0], pos[1], pos[2], pos[3]} : null;
    }

    /// Sets absolute positioning of one or more of the edges. This will be applied last in the layout cycle and will not
    /// affect the flow or grid positions. The positioning is relative to the parent and can not (as padding) be used
    /// to adjust the edges relative to the old value. May be `null` and elements may be `null`.
    /// `null` value(s) for the x2 and y2 will be interpreted as to keep the preferred size and thus the x1
    /// and x2 will just absolutely positions the component.
    ///
    /// Note that `#setBoundsInGrid(boolean)` changes the interpretation of thisproperty slightly.
    ///
    /// For a more thorough explanation of what this constraint does see the white paper or cheat Sheet at www.migcomponents.com.
    ///
    /// #### Parameters
    ///
    /// - `pos`: `UnitValue[] {x, y, x2, y2}`. Must be `null` or of length 4. Elements can be `null`.
    public void setPos(UnitValue[] pos) {
        this.pos = pos != null ? new UnitValue[]{pos[0], pos[1], pos[2], pos[3]} : null;
        linkTargets = null;
    }

    /// Returns if the absolute `pos` value should be corrections to the component that is in a normal cell. If `false`
    /// the value of `pos` is truly absolute in that it will not affect the grid or have a default bounds in the grid.
    ///
    /// For a more thorough explanation of what this constraint does see the white paper or cheat Sheet at www.migcomponents.com.
    ///
    /// #### Returns
    ///
    /// The current value.
    ///
    /// #### See also
    ///
    /// - #getPos()
    public boolean isBoundsInGrid() {
        return boundsInGrid;
    }

    /// Sets if the absolute `pos` value should be corrections to the component that is in a normal cell. If `false`
    /// the value of `pos` is truly absolute in that it will not affect the grid or have a default bounds in the grid.
    ///
    /// For a more thorough explanation of what this constraint does see the white paper or cheat Sheet at www.migcomponents.com.
    ///
    /// #### Parameters
    ///
    /// - `b`: `true` for bounds taken from the grid position. `false` is default.
    ///
    /// #### See also
    ///
    /// - #setPos(UnitValue[])
    void setBoundsInGrid(boolean b) {
        this.boundsInGrid = b;
    }

    /// Returns the absolute cell position in the grid or `-1` if cell positioning is not used.
    ///
    /// For a more thorough explanation of what this constraint does see the white paper or cheat Sheet at www.migcomponents.com.
    ///
    /// #### Returns
    ///
    /// The current value.
    public int getCellX() {
        return cellX;
    }

    /// Set an absolute cell x-position in the grid. If >= 0 this point points to the absolute cell that this constaint's component should occupy.
    /// If there's already a component in that cell they will split the cell. The flow will then continue after this cell.
    ///
    /// For a more thorough explanation of what this constraint does see the white paper or cheat Sheet at www.migcomponents.com.
    ///
    /// #### Parameters
    ///
    /// - `x`: The x-position or `-1` to disable cell positioning.
    public void setCellX(int x) {
        cellX = x;
    }

    /// Returns the absolute cell position in the grid or `-1` if cell positioning is not used.
    ///
    /// For a more thorough explanation of what this constraint does see the white paper or cheat Sheet at www.migcomponents.com.
    ///
    /// #### Returns
    ///
    /// The current value.
    public int getCellY() {
        return cellX < 0 ? -1 : cellY;
    }

    /// Set an absolute cell x-position in the grid. If >= 0 this point points to the absolute cell that this constaint's component should occupy.
    /// If there's already a component in that cell they will split the cell. The flow will then continue after this cell.
    ///
    /// For a more thorough explanation of what this constraint does see the white paper or cheat Sheet at www.migcomponents.com.
    ///
    /// #### Parameters
    ///
    /// - `y`: The y-position or `-1` to disable cell positioning.
    public void setCellY(int y) {
        if (y < 0) {
            cellX = -1;
        }
        cellY = y < 0 ? 0 : y;
    }

    /// Sets the docking side. -1 means no docking.
    ///
    /// Valid sides are: `north = 0, west = 1, south = 2, east = 3`.
    ///
    /// For a more thorough explanation of what this constraint does see the white paper or cheat Sheet at www.migcomponents.com.
    ///
    /// #### Returns
    ///
    /// The current side.
    public int getDockSide() {
        return dock;
    }

    /// Sets the docking side. -1 means no docking.
    ///
    /// Valid sides are: `north = 0, west = 1, south = 2, east = 3`.
    ///
    /// For a more thorough explanation of what this constraint does see the white paper or cheat Sheet at www.migcomponents.com.
    ///
    /// #### Parameters
    ///
    /// - `side`: -1 or 0-3.
    public void setDockSide(int side) {
        if (side < -1 || side > 3) {
            throw new IllegalArgumentException("Illegal dock side: " + side);
        }
        dock = side;
    }

    /// Returns if this component should have its bounds handled by an external source and not this layout manager.
    ///
    /// For a more thorough explanation of what this constraint does see the white paper or cheat Sheet at www.migcomponents.com.
    ///
    /// #### Returns
    ///
    /// The current value.
    public boolean isExternal() {
        return external;
    }

    /// If this boolean is true this component is not handled in any way by the layout manager and the component can have its bounds set by an external
    /// handler which is normally by the use of some `component.setBounds(x, y, width, height)` directly (for Swing).
    ///
    /// The bounds **will not** affect the minimum and preferred size of the container.
    ///
    /// For a more thorough explanation of what this constraint does see the white paper or cheat Sheet at www.migcomponents.com.
    ///
    /// #### Parameters
    ///
    /// - `b`: `true` means that the bounds are not changed.
    public void setExternal(boolean b) {
        this.external = b;
    }

    /// Returns if the flow in the **cell** is in the horizontal dimension. Vertical if `false`. Only the first
    /// component is a cell can set the flow.
    ///
    /// If `null` the flow direction is inherited by from the `net.miginfocom.layout.LC`.
    ///
    /// For a more thorough explanation of what this constraint does see the white paper or cheat Sheet at www.migcomponents.com.
    ///
    /// #### Returns
    ///
    /// The current value.
    public Boolean getFlowX() {
        return flowX;
    }

    /// Sets if the flow in the **cell** is in the horizontal dimension. Vertical if `false`. Only the first
    /// component is a cell can set the flow.
    ///
    /// If `null` the flow direction is inherited by from the `net.miginfocom.layout.LC`.
    ///
    /// For a more thorough explanation of what this constraint does see the white paper or cheat Sheet at www.migcomponents.com.
    ///
    /// #### Parameters
    ///
    /// - `b`: `Boolean.TRUE` means horizontal flow in the cell.
    public void setFlowX(Boolean b) {
        this.flowX = b;
    }

    /// Sets how a component that is hidden (not visible) should be treated by default.
    /// For a more thorough explanation of what this constraint does see the white paper or cheat Sheet at www.migcomponents.com.
    ///
    /// #### Returns
    ///
    /// @return The mode:
    ///
    /// 0 == Normal. Bounds will be calculated as if the component was visible.
    ///
    /// 1 == If hidden the size will be 0, 0 but the gaps remain.
    ///
    /// 2 == If hidden the size will be 0, 0 and gaps set to zero.
    ///
    /// 3 == If hidden the component will be disregarded completely and not take up a cell in the grid..
    public int getHideMode() {
        return hideMode;
    }

    /// Sets how a component that is hidden (not visible) should be treated by default.
    ///
    /// #### Parameters
    ///
    /// - `mode`: @param mode The mode:
    ///
    /// 0 == Normal. Bounds will be calculated as if the component was visible.
    ///
    /// 1 == If hidden the size will be 0, 0 but the gaps remain.
    ///
    /// 2 == If hidden the size will be 0, 0 and gaps set to zero.
    ///
    /// 3 == If hidden the component will be disregarded completely and not take up a cell in the grid..
    public void setHideMode(int mode) {
        if (mode < -1 || mode > 3) {
            throw new IllegalArgumentException("Wrong hideMode: " + mode);
        }

        hideMode = mode;
    }

    /// Returns the id used to reference this component in some constraints.
    ///
    /// For a more thorough explanation of what this constraint does see the white paper or cheat Sheet at www.migcomponents.com.
    ///
    /// #### Returns
    ///
    /// @return The id or `null`. May consist of a groupID and a componentID which are separated by a dot: ".". E.g. "grp1.id1".
    /// The dot should never be first or last if present.
    public String getId() {
        return id;
    }

    /// Sets the id used to reference this component in some constraints.
    ///
    /// For a more thorough explanation of what this constraint does see the white paper or cheat Sheet at www.migcomponents.com.
    ///
    /// #### Parameters
    ///
    /// - `id`: @param id The id or `null`. May consist of a groupID and a componentID which are separated by a dot: ".". E.g. "grp1.id1".
    /// The dot should never be first or last if present.
    public void setId(String id) {
        this.id = id;
    }

    /// Returns the absolute resizing in the last stage of the layout cycle. May be `null` and elements may be `null`.
    ///
    /// For a more thorough explanation of what this constraint does see the white paper or cheat Sheet at www.migcomponents.com.
    ///
    /// #### Returns
    ///
    /// The current value. `null` or of length 4.
    public UnitValue[] getPadding() {
        return padding != null ? new UnitValue[]{padding[0], padding[1], padding[2], padding[3]} : null;
    }

    /// Sets the absolute resizing in the last stage of the layout cycle. These values are added to the edges and can thus for
    /// instance be used to grow or reduce the size or move the component an absolute number of pixels. May be `null`
    /// and elements may be `null`.
    ///
    /// For a more thorough explanation of what this constraint does see the white paper or cheat Sheet at www.migcomponents.com.
    ///
    /// #### Parameters
    ///
    /// - `sides`: top, left, bottom right. Must be `null` or of length 4.
    public void setPadding(UnitValue[] sides) {
        this.padding = sides != null ? new UnitValue[]{sides[0], sides[1], sides[2], sides[3]} : null;
    }

    /// Returns the visual padding used when laying out this Component. May be `null` and elements may be `null`.
    ///
    /// For a more thorough explanation of what this constraint does see the white paper or cheat Sheet at www.migcomponents.com.
    ///
    /// #### Returns
    ///
    /// The current value. `null` or of length 4.
    public UnitValue[] getVisualPadding() {
        return visualPadding != null ? new UnitValue[]{visualPadding[0], visualPadding[1], visualPadding[2], visualPadding[3]} : null;
    }

    /// Sets the visual padding used when laying out this Component.
    ///
    /// For a more thorough explanation of what this constraint does see the white paper or cheat Sheet at www.migcomponents.com.
    ///
    /// #### Parameters
    ///
    /// - `sides`: top, left, bottom right. Must be `null` or of length 4.
    public void setVisualPadding(UnitValue[] sides) {
        this.visualPadding = sides != null ? new UnitValue[]{sides[0], sides[1], sides[2], sides[3]} : null;
    }

    /// Returns how many cells in the grid that should be skipped **before** the component that this constraint belongs to.
    ///
    /// Note that only the first component will be checked for this property.
    ///
    /// For a more thorough explanation of what this constraint does see the white paper or cheat Sheet at www.migcomponents.com.
    ///
    /// #### Returns
    ///
    /// The current value. 0 if no skip.
    public int getSkip() {
        return skip;
    }

    /// Sets how many cells in the grid that should be skipped **before** the component that this constraint belongs to.
    ///
    /// Note that only the first component will be checked for this property.
    ///
    /// For a more thorough explanation of what this constraint does see the white paper or cheat Sheet at www.migcomponents.com.
    ///
    /// #### Parameters
    ///
    /// - `cells`: How many cells in the grid that should be skipped **before** the component that this constraint belongs to
    public void setSkip(int cells) {
        this.skip = cells;
    }

    /// Returns the number of cells the cell that this constraint's component will span in the indicated dimension. `1` is default and
    /// means that it only spans the current cell. `LayoutUtil.INF` is used to indicate a span to the end of the column/row.
    ///
    /// Note that only the first component will be checked for this property.
    ///
    /// For a more thorough explanation of what this constraint does see the white paper or cheat Sheet at www.migcomponents.com.
    ///
    /// #### Returns
    ///
    /// The current value.
    public int getSpanX() {
        return spanX;
    }

    /// Sets the number of cells the cell that this constraint's component will span in the indicated dimension. `1` is default and
    /// means that it only spans the current cell. `LayoutUtil.INF` is used to indicate a span to the end of the column/row.
    ///
    /// Note that only the first component will be checked for this property.
    ///
    /// For a more thorough explanation of what this constraint does see the white paper or cheat Sheet at www.migcomponents.com.
    ///
    /// #### Parameters
    ///
    /// - `cells`: The number of cells to span (i.e. merge).
    public void setSpanX(int cells) {
        this.spanX = cells;
    }

    /// Returns the number of cells the cell that this constraint's component will span in the indicated dimension. `1` is default and
    /// means that it only spans the current cell. `LayoutUtil.INF` is used to indicate a span to the end of the column/row.
    ///
    /// Note that only the first component will be checked for this property.
    ///
    /// For a more thorough explanation of what this constraint does see the white paper or cheat Sheet at www.migcomponents.com.
    ///
    /// #### Returns
    ///
    /// The current value.
    public int getSpanY() {
        return spanY;
    }

    /// Sets the number of cells the cell that this constraint's component will span in the indicated dimension. `1` is default and
    /// means that it only spans the current cell. `LayoutUtil.INF` is used to indicate a span to the end of the column/row.
    ///
    /// Note that only the first component will be checked for this property.
    ///
    /// For a more thorough explanation of what this constraint does see the white paper or cheat Sheet at www.migcomponents.com.
    ///
    /// #### Parameters
    ///
    /// - `cells`: The number of cells to span (i.e. merge).
    public void setSpanY(int cells) {
        this.spanY = cells;
    }

    /// "pushx" indicates that the column that this component is in (this first if the component spans) should default to growing.
    /// If any other column has been set to grow this push value on the component does nothing as the column's explicit grow weight
    /// will take precedence. Push is normally used when the grid has not been defined in the layout.
    ///
    /// If multiple components in a column has push weights set the largest one will be used for the column.
    ///
    /// #### Returns
    ///
    /// The current push value. Default is `null`.
    public Float getPushX() {
        return pushX;
    }

    /// "pushx" indicates that the column that this component is in (this first if the component spans) should default to growing.
    /// If any other column has been set to grow this push value on the component does nothing as the column's explicit grow weight
    /// will take precedence. Push is normally used when the grid has not been defined in the layout.
    ///
    /// If multiple components in a column has push weights set the largest one will be used for the column.
    ///
    /// #### Parameters
    ///
    /// - `weight`: The new push value. Default is `null`.
    public void setPushX(Float weight) {
        this.pushX = weight;
    }

    /// "pushx" indicates that the row that this component is in (this first if the component spans) should default to growing.
    /// If any other row has been set to grow this push value on the component does nothing as the row's explicit grow weight
    /// will take precedence. Push is normally used when the grid has not been defined in the layout.
    ///
    /// If multiple components in a row has push weights set the largest one will be used for the row.
    ///
    /// #### Returns
    ///
    /// The current push value. Default is `null`.
    public Float getPushY() {
        return pushY;
    }

    /// "pushx" indicates that the row that this component is in (this first if the component spans) should default to growing.
    /// If any other row has been set to grow this push value on the component does nothing as the row's explicit grow weight
    /// will take precedence. Push is normally used when the grid has not been defined in the layout.
    ///
    /// If multiple components in a row has push weights set the largest one will be used for the row.
    ///
    /// #### Parameters
    ///
    /// - `weight`: The new push value. Default is `null`.
    public void setPushY(Float weight) {
        this.pushY = weight;
    }

    /// Returns in how many parts the current cell (that this constraint's component will be in) should be split in. If for instance
    /// it is split in two, the next component will also share the same cell. Note that the cell can also span a number of
    /// cells, which means that you can for instance span three cells and split that big cell for two components. Split can be
    /// set to a very high value to make all components in the same row/column share the same cell (e.g. `LayoutUtil.INF`).
    ///
    /// Note that only the first component will be checked for this property.
    ///
    /// For a more thorough explanation of what this constraint does see the white paper or cheat Sheet at www.migcomponents.com.
    ///
    /// #### Returns
    ///
    /// The current value.
    public int getSplit() {
        return split;
    }

    /// Sets in how many parts the current cell (that this constraint's component will be in) should be split in. If for instance
    /// it is split in two, the next component will also share the same cell. Note that the cell can also span a number of
    /// cells, which means that you can for instance span three cells and split that big cell for two components. Split can be
    /// set to a very high value to make all components in the same row/column share the same cell (e.g. `LayoutUtil.INF`).
    ///
    /// Note that only the first component will be checked for this property.
    ///
    /// For a more thorough explanation of what this constraint does see the white paper or cheat Sheet at www.migcomponents.com.
    ///
    /// #### Parameters
    ///
    /// - `parts`: The number of parts (i.e. component slots) the cell should be divided into.
    public void setSplit(int parts) {
        this.split = parts;
    }

    /// Tags the component with metadata. Currently only used to tag buttons with for instance "cancel" or "ok" to make them
    /// show up in the correct order depending on platform. See `PlatformDefaults#setButtonOrder(String)` for information.
    ///
    /// For a more thorough explanation of what this constraint does see the white paper or cheat Sheet at www.migcomponents.com.
    ///
    /// #### Returns
    ///
    /// The current value. May be `null`.
    public String getTag() {
        return tag;
    }

    /// Optinal tag that gives more context to this constraint's component. It is for instance used to tag buttons in a
    /// button bar with the button type such as "ok", "help" or "cancel".
    ///
    /// For a more thorough explanation of what this constraint does see the white paper or cheat Sheet at www.migcomponents.com.
    ///
    /// #### Parameters
    ///
    /// - `tag`: The new tag. May be `null`.
    public void setTag(String tag) {
        this.tag = tag;
    }

    /// Returns if the flow should wrap to the next line/column **after** the component that this constraint belongs to.
    ///
    /// For a more thorough explanation of what this constraint does see the white paper or cheat Sheet at www.migcomponents.com.
    ///
    /// #### Returns
    ///
    /// The current value.
    public boolean isWrap() {
        return wrap != null;
    }

    /// Sets if the flow should wrap to the next line/column **after** the component that this constraint belongs to.
    ///
    /// For a more thorough explanation of what this constraint does see the white paper or cheat Sheet at www.migcomponents.com.
    ///
    /// #### Parameters
    ///
    /// - `b`: `true` means wrap after.
    public void setWrap(boolean b) {
        wrap = b ? (wrap == null ? DEF_GAP : wrap) : null;
    }

    /// Returns the wrap size if it is a custom size. If wrap was set to true with `#setWrap(boolean)` then this method will
    /// return `null` since that means that the gap size should be the default one as defined in the rows spec.
    ///
    /// #### Returns
    ///
    /// The custom gap size. NOTE! Will return `null` for both no wrap **and** default wrap.
    ///
    /// #### Since
    ///
    /// 2.4.2
    ///
    /// #### See also
    ///
    /// - #isWrap()
    ///
    /// - #setWrap(boolean)
    public BoundSize getWrapGapSize() {
        return wrap == DEF_GAP ? null : wrap;
    }

    /// Set the wrap size and turns wrap on if `!= null`.
    ///
    /// #### Parameters
    ///
    /// - `s`: @param s The custom gap size. NOTE! `null` will not turn on or off wrap, it will only set the wrap gap size to "default".
    /// A non-null value will turn on wrap though.
    ///
    /// #### Since
    ///
    /// 2.4.2
    ///
    /// #### See also
    ///
    /// - #isWrap()
    ///
    /// - #setWrap(boolean)
    public void setWrapGapSize(BoundSize s) {
        wrap = s == null ? (wrap != null ? DEF_GAP : null) : s;
    }

    /// Returns if the flow should wrap to the next line/column **before** the component that this constraint belongs to.
    ///
    /// For a more thorough explanation of what this constraint does see the white paper or cheat Sheet at www.migcomponents.com.
    ///
    /// #### Returns
    ///
    /// The current value.
    public boolean isNewline() {
        return newline != null;
    }

    /// Sets if the flow should wrap to the next line/column **before** the component that this constraint belongs to.
    ///
    /// For a more thorough explanation of what this constraint does see the white paper or cheat Sheet at www.migcomponents.com.
    ///
    /// #### Parameters
    ///
    /// - `b`: `true` means wrap before.
    public void setNewline(boolean b) {
        newline = b ? (newline == null ? DEF_GAP : newline) : null;
    }

    /// Returns the newline size if it is a custom size. If newline was set to true with `#setNewline(boolean)` then this method will
    /// return `null` since that means that the gap size should be the default one as defined in the rows spec.
    ///
    /// #### Returns
    ///
    /// The custom gap size. NOTE! Will return `null` for both no newline **and** default newline.
    ///
    /// #### Since
    ///
    /// 2.4.2
    ///
    /// #### See also
    ///
    /// - #isNewline()
    ///
    /// - #setNewline(boolean)
    public BoundSize getNewlineGapSize() {
        return newline == DEF_GAP ? null : newline;
    }

    /// Set the newline size and turns newline on if `!= null`.
    ///
    /// #### Parameters
    ///
    /// - `s`: @param s The custom gap size. NOTE! `null` will not turn on or off newline, it will only set the newline gap size to "default".
    /// A non-null value will turn on newline though.
    ///
    /// #### Since
    ///
    /// 2.4.2
    ///
    /// #### See also
    ///
    /// - #isNewline()
    ///
    /// - #setNewline(boolean)
    public void setNewlineGapSize(BoundSize s) {
        newline = s == null ? (newline != null ? DEF_GAP : null) : s;
    }

    interface CellCall {
        void cell(CC parent, int val);

        void gap(CC parent, String s);
    }
}
