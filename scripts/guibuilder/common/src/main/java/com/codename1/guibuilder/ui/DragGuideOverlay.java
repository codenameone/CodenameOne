package com.codename1.guibuilder.ui;

import com.codename1.ui.Component;
import com.codename1.ui.Container;
import com.codename1.ui.Graphics;
import com.codename1.ui.layouts.BorderLayout;
import java.util.ArrayList;
import java.util.List;

/**
 * Paints insertion and constraint geometry above the visual designer while dragging.
 *
 * <p>This is a glass pane: it draws over the whole canvas area and must never be hit by a pointer.
 * It is layered at inset zero on top of the canvas host, so ordinary hit testing picked it for
 * every press in that area -- including presses on the code editor, which lives inside the canvas
 * host once a source or CSS editor is open. The editor therefore never took focus and appeared to
 * ignore the keyboard entirely.
 */
public final class DragGuideOverlay extends Component {
    /**
     * Never claims a pointer position. Hit testing walks children by containment, so returning
     * false here keeps the overlay purely decorative and lets presses reach whatever it covers.
     */
    @Override
    public boolean contains(int x, int y) {
        return false;
    }

    private String layout = "BoxLayout";
    private String axis = "Y";
    private String constraint;
    private boolean valid = true;
    private int pointerX;
    private int pointerY;
    private int parentX;
    private int parentY;
    private int parentW;
    private int parentH;
    private int targetX;
    private int targetY;
    private int targetW;
    private int targetH;
    private int sourceW;
    private int sourceH;
    private Component guideParent;
    private Component guideSource;
    private int snappedX;
    private int snappedY;
    private String snapDescription;
    private boolean guideVisible;
    private boolean resizeGuide;
    private final List<Component> selections = new ArrayList<>();
    private Component primarySelection;
    private int resizeX;
    private int resizeY;
    private int resizeW;
    private int resizeH;
    private List<GlassItem> simulationItems = new ArrayList<>();
    private List<DependencyLink> simulationLinks = new ArrayList<>();
    private String simulationSummary;

    /** One component in the non-mutating layout simulation painted over the real designer. */
    public static final class GlassItem {
        public final String name;
        public final int oldX, oldY, oldW, oldH;
        public final int newX, newY, newW, newH;
        public final boolean active;

        public GlassItem(String name, int oldX, int oldY, int oldW, int oldH,
                int newX, int newY, int newW, int newH, boolean active) {
            this.name = name;
            this.oldX = oldX; this.oldY = oldY; this.oldW = oldW; this.oldH = oldH;
            this.newX = newX; this.newY = newY; this.newW = newW; this.newH = newH;
            this.active = active;
        }

        public boolean changed() {
            return oldX != newX || oldY != newY || oldW != newW || oldH != newH;
        }
    }

    /** A durable constraint/reference edge in the simulated document. */
    public static final class DependencyLink {
        public final String from;
        public final String to;
        public final int fromX, fromY, toX, toY;
        public final boolean detached;

        public DependencyLink(String from, String to, int fromX, int fromY, int toX, int toY) {
            this(from, to, fromX, fromY, toX, toY, false);
        }

        public DependencyLink(String from, String to, int fromX, int fromY, int toX, int toY,
                boolean detached) {
            this.from = from; this.to = to;
            this.fromX = fromX; this.fromY = fromY; this.toX = toX; this.toY = toY;
            this.detached = detached;
        }
    }

    public DragGuideOverlay() {
        // setIgnorePointerEvents governs event delivery, not hit testing: the overlay was still
        // the component found at a pointer position, which is what decides focus. contains() above
        // is what actually keeps it out of the way.
        setIgnorePointerEvents(true);
        setVisible(false);
    }

    public void showGuide(String layout, String axis, String constraint, boolean valid,
            Component parent, Component target, Component source, int absoluteX, int absoluteY,
            int absoluteSnapX, int absoluteSnapY, int plannedWidth, int plannedHeight, String snapDescription) {
        this.layout = layout == null ? "BoxLayout" : layout;
        this.axis = axis == null ? "Y" : axis;
        this.constraint = constraint;
        this.valid = valid;
        this.guideParent = parent;
        this.guideSource = source;
        int ox = paintOriginX();
        int oy = paintOriginY();
        pointerX = absoluteX - ox;
        pointerY = absoluteY - oy;
        snappedX = absoluteSnapX - ox;
        snappedY = absoluteSnapY - oy;
        this.snapDescription = snapDescription;
        int paddingLeft = parent.getStyle().getPaddingLeftNoRTL();
        int paddingRight = parent.getStyle().getPaddingRightNoRTL();
        int paddingTop = parent.getStyle().getPaddingTop();
        int paddingBottom = parent.getStyle().getPaddingBottom();
        parentX = parent.getAbsoluteX() + paddingLeft - ox;
        parentY = parent.getAbsoluteY() + paddingTop - oy;
        parentW = Math.max(1, parent.getWidth() - paddingLeft - paddingRight);
        parentH = Math.max(1, parent.getHeight() - paddingTop - paddingBottom);
        Component guideTarget = target == null ? parent : target;
        targetX = guideTarget.getAbsoluteX() - ox;
        targetY = guideTarget.getAbsoluteY() - oy;
        targetW = guideTarget.getWidth();
        targetH = guideTarget.getHeight();
        sourceW = plannedWidth > 0 ? plannedWidth : source == null || source.getWidth() < 1 ? Math.max(48, targetW) : source.getWidth();
        sourceH = plannedHeight > 0 ? plannedHeight : source == null || source.getHeight() < 1 ? Math.max(32, targetH) : source.getHeight();
        guideVisible = true;
        resizeGuide = false;
        setVisible(true);
        repaint();
    }

    public void showResize(Container parent, Component source, int absoluteX, int absoluteY,
            int width, int height, String description) {
        guideParent = parent;
        guideSource = source;
        int ox = paintOriginX();
        int oy = paintOriginY();
        resizeX = absoluteX - ox;
        resizeY = absoluteY - oy;
        resizeW = width;
        resizeH = height;
        snapDescription = description;
        guideVisible = true;
        resizeGuide = true;
        setVisible(true);
        repaint();
    }

    public void showSelection(Component component) {
        selections.clear();
        if (component != null) selections.add(component);
        primarySelection = component;
        setVisible(component != null || guideVisible);
        repaint();
    }

    public void showSelections(List<Component> components, Component primary) {
        selections.clear();
        if (components != null) selections.addAll(components);
        primarySelection = primary != null && selections.contains(primary)
                ? primary : selections.isEmpty() ? null : selections.get(selections.size() - 1);
        setVisible(!selections.isEmpty() || guideVisible || !simulationItems.isEmpty());
        repaint();
    }

    public void showSimulation(List<GlassItem> items, List<DependencyLink> links, String summary) {
        simulationItems = items == null ? new ArrayList<>() : new ArrayList<>(items);
        simulationLinks = links == null ? new ArrayList<>() : new ArrayList<>(links);
        simulationSummary = summary;
        setVisible(!selections.isEmpty() || guideVisible || !simulationItems.isEmpty());
        repaint();
    }

    public void clearSimulation() {
        simulationItems.clear();
        simulationLinks.clear();
        simulationSummary = null;
        setVisible(!selections.isEmpty() || guideVisible);
        repaint();
    }

    public void clearSelection() {
        selections.clear();
        primarySelection = null;
        setVisible(guideVisible);
        repaint();
    }

    /** Absolute pixels occupied by the selection rectangle as it will be painted. */
    public int[] selectionPaintBounds() {
        int[] local = selectionPaintLocalBounds();
        if (local == null) return null;
        return new int[]{paintOriginX() + local[0], paintOriginY() + local[1], local[2], local[3]};
    }

    /** Coordinates passed to Graphics, whose origin is the overlay's parent—not the overlay itself. */
    public int[] selectionPaintLocalBounds() {
        if (primarySelection == null || primarySelection.getParent() == null) return null;
        return new int[]{primarySelection.getAbsoluteX() - paintOriginX(), primarySelection.getAbsoluteY() - paintOriginY(),
                primarySelection.getWidth(), primarySelection.getHeight()};
    }

    public List<int[]> selectionPaintBoundsList() {
        List<int[]> result = new ArrayList<>();
        for (Component selection : selections) {
            if (selection == null || selection.getParent() == null) continue;
            result.add(new int[]{selection.getAbsoluteX(), selection.getAbsoluteY(),
                    selection.getWidth(), selection.getHeight()});
        }
        return result;
    }

    private int paintOriginX() { return getParent() == null ? 0 : getParent().getAbsoluteX(); }
    private int paintOriginY() { return getParent() == null ? 0 : getParent().getAbsoluteY(); }

    public void hideGuide() {
        guideVisible = false;
        resizeGuide = false;
        simulationItems.clear();
        simulationLinks.clear();
        simulationSummary = null;
        setVisible(!selections.isEmpty());
        repaint();
    }

    @Override
    public void paint(Graphics g) {
        if (!isVisible()) return;
        super.paint(g);
        int oldAlpha = g.getAlpha();
        paintSelection(g);
        paintSimulation(g);
        if (!guideVisible) {
            g.setAlpha(oldAlpha);
            return;
        }
        if (resizeGuide) {
            g.setColor(0x20b486);
            g.setAlpha(28);
            g.fillRect(resizeX, resizeY, resizeW, resizeH);
            g.setAlpha(255);
            g.drawRect(resizeX, resizeY, resizeW, resizeH);
            paintHandles(g, resizeX, resizeY, resizeW, resizeH, 0x20b486);
            if (snapDescription != null) g.drawString(snapDescription, resizeX + 8, resizeY + 16);
            g.setAlpha(oldAlpha);
            return;
        }
        g.setColor(valid ? 0x20b486 : 0xd64545);
        if ("LayeredLayout".equals(layout)) {
            int x = clamp(snappedX, parentX, parentX + Math.max(0, parentW - sourceW));
            int y = clamp(snappedY, parentY, parentY + Math.max(0, parentH - sourceH));
            int w = Math.min(sourceW, parentW);
            int h = Math.min(sourceH, parentH);
            g.setAlpha(24);
            g.fillRect(x, y, w, h);
            g.setAlpha(190);
            g.drawRect(x, y, w, h);
            paintLayeredRelationships(g, x, y, w, h);
            if (snapDescription != null && snapDescription.indexOf("baseline") >= 0 && guideSource != null) {
                int baseline = guideSource.getBaseline(w, h);
                if (baseline >= 0) {
                    g.setColor(0xbd45d6);
                    g.drawLine(parentX, y + baseline, parentX + parentW, y + baseline);
                    g.drawString("baseline", x + w + 5, y + baseline - 3);
                }
            }
            g.setColor(0x20b486);
            g.drawString(snapDescription == null ? "Free position" : snapDescription, x + 8, y + 14);
        } else if ("BorderLayout".equals(layout)) {
            int[] rect = borderRectangle();
            int x = rect[0], y = rect[1], w = rect[2], h = rect[3];
            g.setAlpha(100);
            g.fillRect(x, y, w, h);
            g.setAlpha(255);
            g.drawRect(x, y, w, h);
            g.drawString((constraint == null ? "CENTER" : constraint.toUpperCase())
                    + (valid ? "" : " — OCCUPIED"), x + 8, y + 14);
        } else if ("GridLayout".equals(layout) || "TableLayout".equals(layout) || "FlowLayout".equals(layout)) {
            g.setAlpha(70);
            g.fillRect(targetX, targetY, targetW, targetH);
            g.setAlpha(255);
            g.drawRect(targetX, targetY, targetW, targetH);
            g.drawString(pointerX < targetX + targetW / 2 ? "Insert before" : "Insert after",
                    targetX + 8, targetY + 14);
        } else {
            g.setAlpha(255);
            if ("X".equals(axis)) {
                int line = pointerX < targetX + targetW / 2 ? targetX : targetX + targetW;
                g.fillRect(line - 2, parentY, 4, parentH);
            } else {
                int line = pointerY < targetY + targetH / 2 ? targetY : targetY + targetH;
                g.fillRect(parentX, line - 2, parentW, 4);
            }
        }
        g.setAlpha(oldAlpha);
    }

    private void paintSimulation(Graphics g) {
        if (simulationItems.isEmpty()) return;
        int ox = paintOriginX();
        int oy = paintOriginY();
        for (DependencyLink link : simulationLinks) {
            g.setColor(link.detached ? 0xd64545 : 0x9b6cff);
            g.setAlpha(105);
            int x1 = link.fromX - ox, y1 = link.fromY - oy;
            int x2 = link.toX - ox, y2 = link.toY - oy;
            drawArrow(g, x1, y1, x2, y2);
        }
        for (GlassItem item : simulationItems) {
            int oldX = item.oldX - ox, oldY = item.oldY - oy;
            int newX = item.newX - ox, newY = item.newY - oy;
            int color = item.active ? 0x4f8cff : 0xf29f3d;
            if (item.changed()) {
                g.setColor(0x7f8b99);
                g.setAlpha(60);
                g.drawRect(oldX, oldY, item.oldW, item.oldH);
                drawArrow(g, oldX + item.oldW / 2, oldY + item.oldH / 2,
                        newX + item.newW / 2, newY + item.newH / 2);
            }
            g.setColor(color);
            g.setAlpha(item.active ? 28 : 14);
            g.fillRect(newX, newY, item.newW, item.newH);
            g.setAlpha(item.active ? 210 : 135);
            g.drawRect(newX, newY, item.newW, item.newH);
            String delta = delta(item);
            if (item.active) paintTag(g, item.name + (delta.length() == 0 ? "" : "  " + delta),
                    newX + 4, newY + 3, color);
        }
        if (simulationSummary != null && simulationSummary.length() > 0) {
            paintTag(g, simulationSummary, 12, 12, 0x263445);
        }
        g.setAlpha(255);
    }

    private String delta(GlassItem item) {
        String out = "";
        int dx = item.newX - item.oldX, dy = item.newY - item.oldY;
        int dw = item.newW - item.oldW, dh = item.newH - item.oldH;
        if (dx != 0 || dy != 0) out = "move " + signed(dx) + "," + signed(dy);
        if (dw != 0 || dh != 0) out += (out.length() == 0 ? "" : " • ") + "size " + signed(dw) + "," + signed(dh);
        return out;
    }

    private String signed(int value) { return value > 0 ? "+" + value : String.valueOf(value); }

    private void paintTag(Graphics g, String text, int x, int y, int background) {
        int width = Math.min(Math.max(20, g.getFont().stringWidth(text) + 10), Math.max(20, getWidth() - x - 4));
        int height = g.getFont().getHeight() + 5;
        g.setColor(background);
        g.setAlpha(235);
        g.fillRect(x, y, width, height);
        g.setColor(0xffffff);
        g.setAlpha(255);
        g.drawString(text, x + 5, y + 2);
    }

    private void drawArrow(Graphics g, int x1, int y1, int x2, int y2) {
        g.drawLine(x1, y1, x2, y2);
        int dx = x2 - x1, dy = y2 - y1;
        int scale = Math.max(1, Math.max(Math.abs(dx), Math.abs(dy)));
        int baseX = x2 - dx * 9 / scale;
        int baseY = y2 - dy * 9 / scale;
        int wingX = -dy * 4 / scale;
        int wingY = dx * 4 / scale;
        g.drawLine(x2, y2, baseX + wingX, baseY + wingY);
        g.drawLine(x2, y2, baseX - wingX, baseY - wingY);
    }

    private void paintSelection(Graphics g) {
        for (Component selection : selections) {
            if (selection == null || selection.getParent() == null) continue;
            int x = selection.getAbsoluteX() - paintOriginX();
            int y = selection.getAbsoluteY() - paintOriginY();
            int w = selection.getWidth();
            int h = selection.getHeight();
            boolean primary = selection == primarySelection;
            g.setAlpha(primary ? 220 : 145);
            g.setColor(0x4f8cff);
            g.drawRect(x, y, w, h);
            paintSelectionHandles(g, x, y, w, h, 0x4f8cff, primary);
        }
    }

    private void paintSelectionHandles(Graphics g, int x, int y, int w, int h, int color,
            boolean primary) {
        int handle = primary ? 11 : 9;
        int half = handle / 2;
        int[] xs = {x, x + w / 2, x + w};
        int[] ys = {y, y + h / 2, y + h};
        for (int xi = 0; xi < xs.length; xi++) {
            for (int yi = 0; yi < ys.length; yi++) {
                if (xi == 1 && yi == 1) continue;
                int hx = xs[xi] - half;
                int hy = ys[yi] - half;
                if (primary) {
                    g.setColor(color);
                    g.setAlpha(245);
                    g.fillRect(hx, hy, handle, handle);
                } else {
                    g.setColor(0xffffff);
                    g.setAlpha(235);
                    g.fillRect(hx, hy, handle, handle);
                    g.setColor(color);
                    g.setAlpha(220);
                    g.drawRect(hx, hy, handle, handle);
                    g.drawRect(hx + 1, hy + 1, handle - 2, handle - 2);
                }
            }
        }
    }

    private void paintHandles(Graphics g, int x, int y, int w, int h, int color) {
        int handle = 8;
        int half = handle / 2;
        int[] xs = {x, x + w / 2, x + w};
        int[] ys = {y, y + h / 2, y + h};
        g.setColor(color);
        for (int xi = 0; xi < xs.length; xi++) {
            for (int yi = 0; yi < ys.length; yi++) {
                if (xi == 1 && yi == 1) continue;
                g.fillRect(xs[xi] - half, ys[yi] - half, handle, handle);
            }
        }
    }

    private int[] borderRectangle() {
        if (!valid && targetW > 0 && targetH > 0) return new int[]{targetX, targetY, targetW, targetH};
        int top = parentY;
        int bottom = parentY + parentH;
        int left = parentX;
        int right = parentX + parentW;
        if (guideParent instanceof Container container && container.getLayout() instanceof BorderLayout border) {
                Component north = border.getNorth();
                Component south = border.getSouth();
                Component west = border.getWest();
                Component east = border.getEast();
                if (north != null && north != guideSource) top = north.getAbsoluteY() - paintOriginY() + north.getHeight();
                if (south != null && south != guideSource) bottom = south.getAbsoluteY() - paintOriginY();
                if (west != null && west != guideSource) left = west.getAbsoluteX() - paintOriginX() + west.getWidth();
                if (east != null && east != guideSource) right = east.getAbsoluteX() - paintOriginX();
        }
        if ("North".equals(constraint)) return new int[]{parentX, parentY, parentW, Math.min(parentH, sourceH)};
        if ("South".equals(constraint)) return new int[]{parentX, Math.max(parentY, parentY + parentH - sourceH), parentW, Math.min(parentH, sourceH)};
        if ("West".equals(constraint)) return new int[]{parentX, top, Math.min(parentW, sourceW), Math.max(1, bottom - top)};
        if ("East".equals(constraint)) return new int[]{Math.max(parentX, parentX + parentW - sourceW), top, Math.min(parentW, sourceW), Math.max(1, bottom - top)};
        return new int[]{left, top, Math.max(1, right - left), Math.max(1, bottom - top)};
    }

    private void paintLayeredRelationships(Graphics g, int x, int y, int w, int h) {
        if (!(guideParent instanceof Container parent)) return;
        int threshold = 2;
        g.setColor(0x4f8cff);
        g.setAlpha(125);
        for (int i = 0; i < parent.getComponentCount(); i++) {
            Component sibling = parent.getComponentAt(i);
            if (sibling == guideSource || sibling.getClientProperty("gui.element") == null) continue;
            int sx = sibling.getAbsoluteX() - paintOriginX();
            int sy = sibling.getAbsoluteY() - paintOriginY();
            int sw = sibling.getWidth();
            int sh = sibling.getHeight();
            if (Math.abs(x - sx) <= threshold || Math.abs(x + w - sx - sw) <= threshold
                    || Math.abs(x + w / 2 - sx - sw / 2) <= threshold) {
                int line = Math.abs(x + w / 2 - sx - sw / 2) <= threshold ? x + w / 2
                        : Math.abs(x - sx) <= threshold ? x : x + w;
                g.drawLine(line, parentY, line, parentY + parentH);
            }
            if (Math.abs(y - sy) <= threshold || Math.abs(y + h - sy - sh) <= threshold
                    || Math.abs(y + h / 2 - sy - sh / 2) <= threshold) {
                int line = Math.abs(y + h / 2 - sy - sh / 2) <= threshold ? y + h / 2
                        : Math.abs(y - sy) <= threshold ? y : y + h;
                g.drawLine(parentX, line, parentX + parentW, line);
            }
        }
    }

    private int clamp(int value, int min, int max) { return Math.max(min, Math.min(max, value)); }
}
