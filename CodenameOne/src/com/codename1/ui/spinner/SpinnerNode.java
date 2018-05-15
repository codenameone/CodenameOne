/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.codename1.ui.spinner;

import com.codename1.charts.util.ColorUtil;
import com.codename1.ui.Component;
import com.codename1.ui.Font;
import com.codename1.ui.Graphics;
import com.codename1.ui.Label;
import com.codename1.ui.Transform;
import com.codename1.ui.events.DataChangedListener;
import com.codename1.ui.events.ScrollListener;
import com.codename1.ui.events.SelectionListener;
import com.codename1.ui.geom.Rectangle;
import com.codename1.ui.geom.Rectangle2D;
import com.codename1.ui.list.ListModel;
import com.codename1.ui.plaf.Border;
import com.codename1.ui.plaf.Style;
import com.codename1.ui.scene.Bounds;
import com.codename1.ui.scene.Node;
import com.codename1.ui.scene.Point3D;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 *
 * @author shannah
 */
class SpinnerNode extends Node {
    
    private Map<Integer,Node> childIndex = new HashMap<Integer,Node>();
    private List<ScrollListener> scrollListeners;

    private SelectionListener selectionListener = new SelectionListener() {

        @Override
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

        @Override
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
    
    
    //private double flatListHeight;
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
        Component overlayRenderer = new Label();
        overlayRenderer.setUIID("Spinner3DOverlay");
        ((Label)overlayRenderer).setShowEvenIfBlank(true);
        selectedRowOverlay.setRenderer(overlayRenderer);
        
    }
    
    public void setRowFormatter(RowFormatter formatter) {
        if (rowFormatter != formatter) {
            rowFormatter = formatter;
            rebuildChildren();
        }
    }
    
    public Style getRowStyle() {
        return renderer.getUnselectedStyle();
    
    }
    
    public Style getSelectedRowStyle() {
        return renderer.getSelectedStyle();
    }
    
    
    
    public Style getSelectedOverlayStyle() {
        return selectedRowOverlay.getRenderer().getUnselectedStyle();
    }
    
    public ListModel<String> getListModel() {
        return listModel;
    }
    
    private void rebuildChildren() {
        childIndex.clear();
        removeAll();
        /*
        if (listModel != null) {
            ListModel<String> list = listModel;
            int len = list.getSize();
            for (int i=0; i<len; i++) {
                final Node n = new Node();
                //String lbl = list.getItemAt(i);
                //System.out.println("Adding label with text "+list.getItemAt(i));
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
                //renderer.setUIID("Spinner3DRow");
                renderer.setSelectedStyle(getSelectedRowStyle());
                renderer.setUnselectedStyle(getRowStyle());
                n.setRenderer(renderer);
                add(n);
            }
            
            
            setSelectedIndex(listModel.getSelectedIndex());
        }
        
                */
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
    
    //public void refreshStyles() {
    //    if (hasChildren()) {
    //        for (Node n : getChildNodes()) {
    //            if (n == selectedRowOverlay) {
    //            
    //            } else {
    //                //n.getRenderer().setUnselectedStyle(getRowStyle());
    //                //n.getRenderer().setSelectedStyle(getSelectedRowStyle());
    //            }
    //            
    //        }
    //    }
    //}
    
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
        if (index < 0 || index > listModel.getSize()-1) {
            throw new ArrayIndexOutOfBoundsException("Index out of bounds");
        }
        
        setScrollY(index * calcFlatListHeight() / listModel.getSize());
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
        boolean changed = Math.abs(pos-flatScrollPos) > 2;
        this.flatScrollPos = pos;
        setNeedsLayout(true);
        if (getScene() != null) {
            getScene().repaint();
        }
        updateSelectedIndex();
        if (changed) {
            fireScrollEvent((int)pos);
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
            int len = list.getSize();
            //for (int i=0; i<len; i++) {
            final Node n = new Node();
            //String lbl = list.getItemAt(i);
            //System.out.println("Adding label with text "+list.getItemAt(i));
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
            //renderer.setUIID("Spinner3DRow");
            renderer.setSelectedStyle(getSelectedRowStyle());
            renderer.setUnselectedStyle(getRowStyle());
            n.setRenderer(renderer);
            remove(selectedRowOverlay);
            add(n);
            add(selectedRowOverlay);
            childIndex.put(i, n);
            return n;
            
            //}

            //add(selectedRowOverlay);
            //setSelectedIndex(listModel.getSelectedIndex());
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
        //System.out.println("Min visible index: "+minVisibleIndex+" max: "+maxVisibleIndex);
        if (hasChildren()) {
            int len = listModel.getSize();
            for (int i=0; i<len; i++) {
                if ((minVisibleIndex > index || maxVisibleIndex < index) && !childIndex.containsKey(i)) {
                    index++;
                    continue;
                }
                Node child = getOrCreateChild(i);
            //for (Node child : getChildNodes()) {
                
                
                if (minVisibleIndex > index || maxVisibleIndex < index) {
                    child.visible.set(false);
                    index++;
                    continue;
                }
                //renderer.setText(listModel.getItemAt(index));
                //child.setRenderer(renderer);
                child.visible.set(true);
                Bounds localBounds = child.boundsInLocal.get();
                localBounds.setWidth(width);
                localBounds.setDepth(diameter);
                localBounds.setHeight(diameter);
                localBounds.setMinX(0.0);
                localBounds.setMinY(0.0);
                child.paintingRect.set(new Rectangle(0, (int)(diameter/2 - rendererHeight/2), (int)width, (int)rendererHeight));
                //child.localCanvasZ.set(diameter/2); // So that rotation works correctly
                if (usePerspective()) {
                    double angle = calculateRotationForChild(index);
                    if (Math.abs(angle) < 10) {
                        //Log.p("Settin focus");
                        child.addTags("selected");
                    } else {
                        child.removeTags("selected");
                        int opacity = (int)(Math.cos(angle * Math.PI / 180) * 255);
                        //Log.p("Opacity="+opacity);
                        child.getRenderer().getStyle().setOpacity(Math.min(255, Math.max(0, opacity)));
                    }
                    child.rotate.set(-angle);
                    child.rotationAxis.set(new Point3D(1, 0, 0)); // Rotate along X-Axis
                    child.layoutX.set(0.0);
                    child.layoutY.set(0.0);
                    child.layoutZ.set(-diameter/2);
                } else {
                    
                    //System.out.println("Item "+listModel.getItemAt(index));
                    double angle = calculateRotationForChild(index) * Math.PI / 180.0;
                    
                    //Log.p("Angle["+index+"]="+angle);
                    //System.out.println("Angle "+angle);
                    double minAngle = angle + getRotationRangeForSide() * Math.PI/180.0 / 2;
                    //System.out.println("Min angle: "+minAngle);
                    
                    double maxAngle = angle - getRotationRangeForSide() * Math.PI/180.0 /2;
                    //Log.p("Angle: "+Math.abs(angle)+" rot range: "+(getRotationRangeForSide() * Math.PI/180.0));
                    if (Math.abs(angle) < 10 * Math.PI/180) {
                        //Log.p("Settin focus");
                        child.addTags("selected");
                    } else {
                        child.removeTags("selected");
                        int opacity = (int)(Math.cos(angle) * 255);
                        //Log.p("Opacity="+opacity);
                        child.getRenderer().getStyle().setOpacity(Math.min(255, Math.max(0, opacity)));
                    }
                    //System.out.println("Max angle:"+maxAngle);
                    double projectedHeight = Math.abs((diameter/2) * (Math.sin(minAngle) - Math.sin(maxAngle)));
                    //System.out.println("Projected height "+projectedHeight);
                    child.layoutX.set(0.0);
                    child.layoutY.set(-(diameter/2) * Math.sin(angle));
                    child.layoutZ.set(0.0);
                    child.scaleY.set(projectedHeight / rendererHeight);
                }
                
                index++;

            }
            
            //if (child == selectedRowOverlay) {
            Bounds b = selectedRowOverlay.boundsInLocal.get();
            b.setWidth(width);
            b.setHeight(rendererHeight);
            b.setMinX(0);
            b.setMinY(0);
            selectedRowOverlay.layoutX.set(0.0);
            selectedRowOverlay.layoutY.set(diameter/2 - rendererHeight/2);
                    
                    //index++;
                    //continue;
               // }
        }
        
        
    }

    @Override
    public void render(Graphics g) {
        
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
