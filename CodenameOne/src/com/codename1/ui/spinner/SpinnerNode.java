/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.codename1.ui.spinner;

import com.codename1.ui.Component;
import com.codename1.ui.Graphics;
import com.codename1.ui.Label;
import com.codename1.ui.Painter;
import com.codename1.ui.events.DataChangedListener;
import com.codename1.ui.events.ScrollListener;
import com.codename1.ui.events.SelectionListener;
import com.codename1.ui.geom.Rectangle;
import com.codename1.ui.geom.Rectangle2D;
import com.codename1.ui.list.ListModel;
import com.codename1.ui.plaf.Style;
import com.codename1.ui.scene.Bounds;
import com.codename1.ui.scene.Node;
import com.codename1.ui.scene.NodePainter;
import com.codename1.ui.scene.Point3D;
import com.codename1.ui.scene.TextPainter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * A spinner node for rendering spinnable lists. 
 * @author shannah
 */
class SpinnerNode extends Node {
    private Label rowTemplate = new Label("", "Spinner3DRow");
    private Label overlayTemplate = new Label("", "Spinner3DOverlay");
    private Style rowStyle, selectedRowStyle, overlayStyle;
    private Map<Integer,Node> childIndex = new HashMap<Integer,Node>();
    private List<ScrollListener> scrollListeners;

    private boolean setSelectedIndexReentrantLock;
    private boolean setScrollYReentrantLock;

    private SelectionListener selectionListener = new SelectionListener() {
        public void selectionChanged(int oldSelected, int newSelected) {
            if (newSelected < 0 && listModel != null) {
                newSelected = listModel.getSelectedIndex();
            }
            if (newSelected >= 0 && newSelected < listModel.getSize() && newSelected != selectedIndex) {
                setSelectedIndex(newSelected);
            }
        }
    };
    
    private DataChangedListener listChangedListener = new DataChangedListener() {
        public void dataChanged(int type, int index) {
            rebuildChildren();
        }
    };
    
    public void addScrollListener(ScrollListener l) {
        if (scrollListeners == null) {
            scrollListeners = new ArrayList<ScrollListener>();
        }
        scrollListeners.add(l);
    }
    
    public void removeScrollListener(ScrollListener l) {
        if (scrollListeners != null) {
            scrollListeners.remove(l);
            if (scrollListeners.isEmpty()) {
                scrollListeners = null;
            }
        }
    }
    
    private void fireScrollEvent(int scrollPos) {
        if (scrollListeners != null) {
            for (ScrollListener l : scrollListeners) {
                l.scrollChanged(-1, scrollPos, -1, -1);
            }
        }
    }
    
    public static interface RowFormatter {
        public String format(String input);
    }

    private RowFormatter rowFormatter;
    private double flatScrollPos;
    private int numSides = 14;
    private Label renderer = new Label("Testing", "Spinner3DRow");
    ListModel<String> listModel;
    Node selectedRowOverlay = new Node();

    private static boolean usePerspective() {
        // Disabling perspective for now because need to work out a few issues
        return false;
    }
    
    public SpinnerNode() {
        rowStyle = rowTemplate.getUnselectedStyle();
        selectedRowStyle = rowTemplate.getSelectedStyle();
        overlayStyle = overlayTemplate.getUnselectedStyle();
        selectedRowOverlay.setStyle(overlayStyle);
        selectedRowOverlay.setRenderer(new NodePainter() {
            public void paint(Graphics g, Rectangle bounds, Node node) {
                Style style = node.getStyle();
                g.setColor(style.getBgColor());
                g.fillRect(bounds.getX(), bounds.getY(), bounds.getWidth(), bounds.getHeight());
                g.setColor(style.getFgColor());
                int alpha = g.concatenateAlpha(style.getFgAlpha());
                g.drawLine(bounds.getX(), bounds.getY(), bounds.getWidth() + bounds.getX(), bounds.getY());
                g.drawLine(bounds.getX(), bounds.getY()+bounds.getHeight(), 
                        bounds.getX() + bounds.getWidth(), bounds.getY() + bounds.getHeight()
                );
                g.setAlpha(alpha);
            }
        });
    }
    
    public void setRowFormatter(RowFormatter formatter) {
        if (rowFormatter != formatter) {
            rowFormatter = formatter;
            rebuildChildren();
        }
    }
    
    public Style getRowStyle() {
        return rowStyle;
    }
    
    public int getNumSides() {
        return numSides;
    }
    public Style getSelectedRowStyle() {
        return selectedRowStyle;
    }
    public Style getSelectedOverlayStyle() {
        return overlayStyle;
    }
    
    public ListModel<String> getListModel() {
        return listModel;
    }
    
    private void rebuildChildren() {
        childIndex.clear();
        removeAll();
        setSelectedIndex(listModel.getSelectedIndex());
        add(selectedRowOverlay);
    }
    
    public void setListModel(ListModel<String> list) {
        if (listModel != null) {
            listModel.removeSelectionListener(selectionListener);
            listModel.removeDataChangedListener(listChangedListener);
        }
        listModel = list;
        if (listModel != null) {
            listModel.addSelectionListener(selectionListener);
            listModel.addDataChangedListener(listChangedListener);
        }
        rebuildChildren();
    }
    
    public Node getSelectedRowOverlay() {
        return selectedRowOverlay;
    }
    
    public double calcRowHeight() {
        return renderer.getPreferredH();
    }
    
    public double calcFlatListHeight() {
        return renderer.getPreferredH() * listModel.getSize();
    }
    
    public double calcViewportHeight() {
        double circumference = renderer.getPreferredH() * numSides;
        double diameter = circumference / Math.PI;
        return diameter;
    }
    
    private double calculateRotationForChild(int index) {
        double degreeOffset = flatScrollPos * 360.0 / (numSides * renderer.getPreferredH());
        
        int pos = index % numSides;
        return (-(360.0 / numSides) * pos + degreeOffset) % 360;
    }
    
    private double getRotationRangeForSide() {
        return 360.0 / numSides;
    }
    
    private double getFlatVisibleHeight() {
        return renderer.getPreferredH() * numSides / 2;
    }
    
    public int getSelectedIndex() {
        return (int)(flatScrollPos / calcFlatListHeight() * listModel.getSize());
    }

    public void setSelectedIndex(int index) {
        if (setSelectedIndexReentrantLock) {
            return;
        }
        setSelectedIndexReentrantLock = true;
        try {
            if (index < 0 || index > listModel.getSize() - 1) {
                throw new ArrayIndexOutOfBoundsException("Index out of bounds:" + index + ", must be between 0 and " + (listModel.getSize()-1));
            }

            setScrollY(index * calcFlatListHeight() / listModel.getSize());
        } finally {
            setSelectedIndexReentrantLock = false;
        }
    }
    
    private int selectedIndex=-1;
    private List<SelectionListener> selectionListeners;
    private void updateSelectedIndex() {
        int newSelectedIndex = getSelectedIndex();
        if (newSelectedIndex != selectedIndex) {
            int oldSelectedIndex = selectedIndex;
            selectedIndex = newSelectedIndex;
            listModel.setSelectedIndex(newSelectedIndex);
            if (selectionListeners != null && !selectionListeners.isEmpty()) {
                for (SelectionListener l : selectionListeners) {
                    l.selectionChanged(oldSelectedIndex, newSelectedIndex);
                }
            }
            
        }
    }
    
    public void addSelectionListener(SelectionListener l) {
        if (selectionListeners == null) {
            selectionListeners = new ArrayList<SelectionListener>();
        }
        selectionListeners.add(l);
    }
    
    public void removeSelectionListener(SelectionListener l) {
        if (selectionListeners != null) {
            selectionListeners.remove(l);
        }
        if (selectionListeners.isEmpty()) {
            selectionListeners = null;
        }
    }
    
    private int getMinVisibleIndex(int selectedIndex) {
        return selectedIndex - numSides/4;
    }
    
    private int getMaxVisibleIndex(int selectedIndex) {
        return selectedIndex + numSides/4;
    }
    public void setScrollY(double pos) {
        if (setScrollYReentrantLock) {
            return;
        }
        setScrollYReentrantLock = true;
        try {
            final int prevScrollPos = (int) flatScrollPos;
            final int posInt = (int) pos;
            if (prevScrollPos != posInt) {
                this.flatScrollPos = posInt;
                setNeedsLayout(true);
                if (getScene() != null) {
                    getScene().repaint();
                }
                updateSelectedIndex();
                fireScrollEvent((int) pos);
            }
        } finally {
            setScrollYReentrantLock = false;
        }
    }
    
    public double getScrollY() {
        return flatScrollPos;
    }
    
    private Node getOrCreateChild(int i) {
        if (childIndex.containsKey(i)) {
            return childIndex.get(i);
        }
        if (listModel != null) {
            ListModel<String> list = listModel;
            final Node n = new Node();
            String lbl = list.getItemAt(i);
            if (rowFormatter != null) {
                lbl = rowFormatter.format(lbl);
            }
            Label renderer = new Label(lbl, "Spinner3DRow") {
                @Override
                public Style getStyle() {
                    if (n.hasTag("selected")) {
                        return this.getSelectedStyle();
                    } else {
                        return this.getUnselectedStyle();
                    }
                }
            };

            renderer.setSelectedStyle(getSelectedRowStyle());
            renderer.setUnselectedStyle(getRowStyle());
            n.setRenderer(new TextPainter(lbl, Component.CENTER));
            remove(selectedRowOverlay);
            add(n);
            add(selectedRowOverlay);
            childIndex.put(i, n);

            return n;
        }

        return null;
    }
    
    @Override
    protected void layoutChildren() {
        double width = boundsInLocal.get().getWidth();
        double height = boundsInLocal.get().getHeight();
        double rendererHeight = renderer.getPreferredH();
        double circumference = rendererHeight * numSides;
        double diameter = circumference / Math.PI;
        int index = 0;
        int selectedIndex = getSelectedIndex();
        int minVisibleIndex = getMinVisibleIndex(selectedIndex);
        int maxVisibleIndex = getMaxVisibleIndex(selectedIndex);
        if (hasChildren()) {
            int len = listModel.getSize();
            for (int i=0; i<len; i++) {
                if ((minVisibleIndex > index || maxVisibleIndex < index) && !childIndex.containsKey(i)) {
                    index++;
                    continue;
                }
                Node child = getOrCreateChild(i);
                if (minVisibleIndex > index || maxVisibleIndex < index) {
                    child.visible.set(false);
                    index++;
                    continue;
                }
                child.visible.set(true);
                Bounds localBounds = child.boundsInLocal.get();
                localBounds.setWidth(width);
                localBounds.setDepth(0);
                localBounds.setHeight(diameter);
                localBounds.setMinX(0.0);
                localBounds.setMinY(0.0);
                child.paintingRect.set(new Rectangle(0, (int)(diameter/2 - rendererHeight/2), (int)width, (int)rendererHeight));
                if (usePerspective()) {
                    localBounds.setDepth(diameter);
                    double angle = calculateRotationForChild(index);
                    if (Math.abs(angle) < 10) {
                        child.addTags("selected");
                        child.setStyle(getSelectedRowStyle());
                        child.opacity.set(1.0);
                    } else {
                        child.removeTags("selected");
                        double opacity = Math.cos(angle * Math.PI / 180);
                        child.setStyle(getRowStyle());
                        child.opacity.set(opacity);
                    }
                    child.rotate.set(-angle);
                    child.rotationAxis.set(new Point3D(1, 0, 0)); // Rotate along X-Axis
                    child.layoutX.set(0.0);
                    child.layoutY.set(0.0);
                    child.layoutZ.set(-diameter/2);
                } else {
                    double angle = calculateRotationForChild(index) * Math.PI / 180.0;
                    double minAngle = angle + getRotationRangeForSide() * Math.PI/180.0 / 2;
                    double maxAngle = angle - getRotationRangeForSide() * Math.PI/180.0 /2;
                    if (Math.abs(angle) < 10 * Math.PI/180) {
                        child.addTags("selected");
                        child.setStyle(selectedRowStyle);
                        child.opacity.set(1.0);
                    } else {
                        child.removeTags("selected");
                        child.setStyle(rowStyle);
                        double opacity = Math.cos(angle);
                        child.opacity.set(opacity);
                    }
                    double projectedHeight = Math.abs((diameter/2) * (Math.sin(minAngle) - Math.sin(maxAngle)));
                    child.layoutX.set(0.0);
                    child.layoutY.set(-(diameter/2) * Math.sin(angle));
                    child.layoutZ.set(0.0);
                    child.scaleY.set(projectedHeight / rendererHeight);
                }
                
                index++;
            }

            Bounds b = selectedRowOverlay.boundsInLocal.get();
            b.setWidth(width);
            b.setHeight(rendererHeight);
            b.setMinX(0);
            b.setMinY(0);
            selectedRowOverlay.layoutX.set(0.0);
            selectedRowOverlay.layoutY.set(diameter/2 - rendererHeight/2);
        }
    }

    public void render(Graphics g) {
        g.setColor(overlayStyle.getBgColor());
        int alpha = g.getAlpha();
        g.setAlpha(255);
        g.fillRect(0, 0, (int)boundsInLocal.get().getWidth(), (int)boundsInLocal.get().getHeight());
        g.setAlpha(alpha);
        super.render(g);
        
        int clipX = g.getClipX();
        int clipY = g.getClipY();
        int clipW = g.getClipWidth();
        int clipH = g.getClipHeight();
        Rectangle2D overlayRect = selectedRowOverlay.getBoundsInScene(new Rectangle2D());
        
        double magnification = 1.35;
        double oldScaleX = scaleX.get();
        double oldScaleY = scaleY.get();
        double oldTranslateX = translateX.get();
        scaleX.set(oldScaleX*magnification);
        scaleY.set(oldScaleY*magnification);
        switch (getRowStyle().getAlignment()) {
            case Component.LEFT:
                translateX.set(oldTranslateX + boundsInLocal.get().getWidth() * (magnification - 1.0)/2/magnification);
                break;
            case Component.RIGHT:
                translateX.set(oldTranslateX - boundsInLocal.get().getWidth() * (magnification - 1.0)/2/magnification);
                break;
        }

        selectedRowOverlay.visible.set(false);
        g.setClip(
                (int)overlayRect.getX(), 
                (int)overlayRect.getY()+1, 
                (int)overlayRect.getWidth(), 
                (int)overlayRect.getHeight()-2
        );
        super.render(g);
        selectedRowOverlay.visible.set(true);
        g.setClip(clipX, clipY, clipW, clipH);
        scaleX.set(oldScaleX);
        scaleY.set(oldScaleY);
        translateX.set(oldTranslateX);
    }

}
