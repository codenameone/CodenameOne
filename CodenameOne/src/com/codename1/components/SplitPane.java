/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.codename1.components;

import com.codename1.ui.Button;
import com.codename1.ui.Component;
import static com.codename1.ui.ComponentSelector.$;
import com.codename1.ui.Container;
import com.codename1.ui.Display;
import com.codename1.ui.FontImage;
import com.codename1.ui.Graphics;
import com.codename1.ui.Image;
import com.codename1.ui.Label;
import com.codename1.ui.events.ActionEvent;
import com.codename1.ui.events.ActionListener;
import com.codename1.ui.geom.Dimension;
import com.codename1.ui.layouts.BorderLayout;
import com.codename1.ui.layouts.LayeredLayout;
import com.codename1.ui.layouts.LayeredLayout.LayeredLayoutConstraint;
import com.codename1.ui.layouts.LayeredLayout.LayeredLayoutConstraint.Inset;
import com.codename1.ui.plaf.Border;
import java.util.HashSet;
import java.util.Set;

/**
 *
 * @author shannah
 */
public class SplitPane extends Container {
    private final int orientation;
    public static final int HORIZONTAL_SPLIT = 0;
    public static final int VERTICAL_SPLIT = 1;
    
    private final Container topOrLeft;
    private final Container bottomOrRight;
    private final Divider divider;
    
    private LayeredLayoutConstraint minInset;
    private LayeredLayoutConstraint maxInset;
    private LayeredLayoutConstraint preferredInset;
    
    private boolean isExpanded;
    private boolean isCollapsed;

    
    
    public SplitPane(int orientation, Component topOrLeft, Component bottomOrRight, String minInset, String preferredInset, String maxInset) {
        super(new LayeredLayout());
        
        this.orientation = orientation;
        this.topOrLeft = BorderLayout.center(topOrLeft);
        this.bottomOrRight = BorderLayout.center(bottomOrRight);
        

        divider = new Divider();
        add(this.topOrLeft).add(this.bottomOrRight).add(divider);
        
        LayeredLayout l = (LayeredLayout)getLayout();
        this.preferredInset = initDividerInset(l.createConstraint(), preferredInset);
        this.minInset = initDividerInset(l.createConstraint(), minInset);
        this.maxInset = initDividerInset(l.createConstraint(), maxInset);
        
        l.setInsets(this.topOrLeft, "0 0 0 0")
                .setInsets(this.topOrLeft, "0 0 0 0");
        this.preferredInset.copyTo(l.getOrCreateConstraint(divider));
        
        switch (orientation) {
            case HORIZONTAL_SPLIT : {
                l.setReferenceComponentRight(this.topOrLeft, divider, 1f);
                l.setReferenceComponentLeft(this.bottomOrRight, divider, 1f);
                break;
            } 
            
            default : {
                l.setReferenceComponentBottom(this.topOrLeft, divider, 1f);
                l.setReferenceComponentTop(this.bottomOrRight, divider, 1f);
                break;
            }
            
        }
        
        
    }
    
    /**
     * The active inset of the divider.
     * @return 
     */
    private Inset getDividerInset() {
        LayeredLayoutConstraint cnst = ((LayeredLayout)getLayout()).getOrCreateConstraint(divider);
        return getFixedInset(cnst);
    }
    
    private Inset getAutoInset() {
        LayeredLayoutConstraint cnst = ((LayeredLayout)getLayout()).getOrCreateConstraint(divider);
        return getAutoInset(cnst);
    }
    
    private Inset getFixedInset(LayeredLayoutConstraint cnst) {
        switch (orientation) {
            case VERTICAL_SPLIT :
                return cnst.top();
            default:
                return cnst.left();
        }
    }
    
    private Inset getMinDividerInset() {
        return getFixedInset(minInset);
    }
    
    private Inset getMaxDividerInset() {
        return getFixedInset(maxInset);
    }
    
    private void setDividerInset(String inset) {
        getDividerInset().setValueAsString(inset);
        clampInset();
    }
    
    private Inset getAutoInset(LayeredLayoutConstraint cnst) {
        switch (orientation) {
            case VERTICAL_SPLIT:
                return cnst.bottom();
            default:
                return cnst.right();
        }
    }
    
    private Set<Inset> getZeroInsets(LayeredLayoutConstraint cnst) {
        Set<Inset> out = new HashSet<Inset>();
        switch (orientation) {
            case VERTICAL_SPLIT:
                out.add(cnst.left());
                out.add(cnst.right());
                break;
            default :
                out.add(cnst.top());
                out.add(cnst.bottom());
                
        }
        return out;
    }
    
    private LayeredLayoutConstraint initDividerInset(LayeredLayoutConstraint cnst, String insetVal) {
        getFixedInset(cnst).setValueAsString(insetVal);
        getAutoInset(cnst).setValueAsString("auto");
        for (Inset i : getZeroInsets(cnst)) {
            i.setValueAsString("0");
        }
        return cnst;
    }
    
    
    
    private void clampInset() {
        
        int px = getDividerInset().getAbsolutePixels(divider);
        
        Inset minInset = getMinDividerInset();
        if (minInset.getAbsolutePixels(divider) >= px) {
            minInset.copyTo(getDividerInset());
            isCollapsed = true;
            isExpanded = false;
        }
        Inset maxInset = getMaxDividerInset();
        if (maxInset.getAbsolutePixels(divider) <= px) {
            maxInset.copyTo(getDividerInset());
            isExpanded = true;
            isCollapsed = false;
        }

        px = getAutoInset().getAbsolutePixels(divider);
        
        if (px < 0) {
            // Make sure that the divider is fully visible
            getDividerInset().translatePixels(px, true, divider);
            isExpanded = true;
            isCollapsed = false;

        }
    }
    
    
    private void setTopOrLeftComponent(Component cmp) {
        topOrLeft.removeAll();
        topOrLeft.add(BorderLayout.CENTER, cmp);
    }
    
    public void setTop(Component cmp) {
        setTopOrLeftComponent(cmp);
    }
    
    public void setLeft(Component cmp) {
        setTopOrLeftComponent(cmp);
    }
    
    private void setBottomOrRightComponent(Component cmp) {
        bottomOrRight.removeAll();
        bottomOrRight.add(BorderLayout.CENTER, cmp);
    }
    
    
    public void setBottom(Component cmp) {
        setBottomOrRightComponent(cmp);
    }
    
    public void setRight(Component cmp) {
        setBottomOrRightComponent(cmp);
    }
   
    public void toggleCollapsePreferred() {
        if (isCollapsed) {
            expand();
        } else if (isExpanded) {
            collapse(true);
        } else {
            collapse();
        }
    }
    
    public void toggleExpandPreferred() {
        if (isExpanded) {
            collapse();
        } else if (isCollapsed) {
            expand(true);
        } else {
            expand();
        }
    }
    
    public void expand() {
        expand(false);
    }
    
    public void expand(boolean force) {
        if (isCollapsed && !force) {
            getFixedInset(preferredInset).copyTo(getDividerInset());
            clampInset();
            isCollapsed = false;
            SplitPane.this.animateLayout(300);
        } else if (isExpanded) {
            // do nothing
        } else {
            getFixedInset(maxInset).copyTo(getDividerInset());
            clampInset();
            isExpanded = true;
            SplitPane.this.animateLayout(300);
        }
    }
    
    public void collapse() {
        collapse(false);
    }
    
    public void collapse(boolean force) {
        if (isCollapsed) {
            // do nothing
        } else if (isExpanded && !force) {
            getFixedInset(preferredInset).copyTo(getDividerInset());
            clampInset();
            isExpanded = false;
            SplitPane.this.animateLayout(300);
        } else {
            getFixedInset(minInset).copyTo(getDividerInset());
            clampInset();
            isCollapsed = true;
            SplitPane.this.animateLayout(300);
        }
        
    }
    
    public void setInset(String inset) {
        getDividerInset().setValueAsString(inset);
        isExpanded = false;
        isCollapsed = false;
        clampInset();
    }
    
    public void setPreferredInset(String inset) {
        getFixedInset(preferredInset).setValueAsString(inset);
    }
    
    public void setMinInset(String inset) {
        getFixedInset(minInset).setValueAsString(inset);
    }
    
    public void setMaxInset(String inset) {
        getFixedInset(maxInset).setValueAsString(inset);
    }
    
    private class Divider extends Container {
        int pressedX, pressedY, draggedX, draggedY;
        LayeredLayoutConstraint pressedConstraint;
        private final Button btnCollapse;
        private final Button btnExpand;
        private final Label dragHandle;
        private boolean inDrag;
        
        
        private char getCollapseIcon() {
            switch (orientation) {
                case HORIZONTAL_SPLIT:
                    return 0xe314;
                default:
                    return 0xe316;
            }
        }
        
        private char getExpandIcon() {
            switch (orientation) {
                case HORIZONTAL_SPLIT:
                    return 0xe315;
                default :
                    return 0xe313;
            }
        }
        
        private Image getDragIconImage() {
            Image img = FontImage.createMaterial(FontImage.MATERIAL_DRAG_HANDLE, getStyle(), 3);
            switch (orientation) {
                case HORIZONTAL_SPLIT:
                    return img.rotate90Degrees(true);
                default:
                    return img;
            }
            
        }
        
        private int getDragCursor() {
            return orientation == HORIZONTAL_SPLIT ? Component.E_RESIZE_CURSOR : Component.N_RESIZE_CURSOR;
        }
        
        private Border createBorder() {
            return orientation == HORIZONTAL_SPLIT ? Border.createCompoundBorder(
                            Border.createEmpty(), 
                            Border.createEmpty(), 
                            Border.createBevelRaised(),
                            Border.createBevelRaised()) : 
                    Border.createCompoundBorder(
                            Border.createBevelRaised(),
                            Border.createBevelRaised(),
                            Border.createEmpty(),
                            Border.createEmpty()
                    );
                    
        }
        Divider() {
            super(new LayeredLayout());
            btnCollapse = $(new Button())
                    .setUIID("Label")
                    .setCursor(Component.HAND_CURSOR)
                    .setIcon(getCollapseIcon())
                    .addActionListener(new ActionListener() {
                        public void actionPerformed(ActionEvent evt) {
                            collapse();
                        }
                    })
                    .selectAllStyles()
                    .setMargin(0)
                    .setPadding(0)
                    .asComponent(Button.class);
            btnExpand =  $(new Button())
                    .setCursor(Component.HAND_CURSOR)
                    .setUIID("Label")
                    .setIcon(getExpandIcon())
                    .addActionListener(new ActionListener() {
                        public void actionPerformed(ActionEvent evt) {
                            expand();
                        }
                    })
                    .selectAllStyles()
                    .setMargin(0)
                    .setPadding(0)
                    .asComponent(Button.class);
            
            dragHandle = $(new Label())
                    .setIcon(getDragIconImage())
                    .setMargin(0)
                    .setIgnorePointerEvents(true)
                    .setPadding(0)
                    .setDraggable(true)
                    .setCursor(getDragCursor())
                    .asComponent(Label.class);
            
            add(btnCollapse).add(btnExpand).add(dragHandle);
            
            LayeredLayout l = (LayeredLayout)getLayout();
            switch (orientation) {
                case HORIZONTAL_SPLIT: {
                    l.setInsets(btnCollapse, "0 0 auto 0")
                            .setInsets(btnExpand, "0 0 auto 0")
                            .setInsets(dragHandle, "auto auto auto auto")
                            .setReferenceComponentTop(btnExpand, btnCollapse, 1f);
                    break;
                }
                default: {
                    l.setInsets(btnCollapse, "0 auto 0 0")
                            .setInsets(btnExpand, "0 auto 0 0")
                            .setInsets(dragHandle, "auto auto auto auto")
                            .setReferenceComponentLeft(btnExpand, btnCollapse, 1f);
                }
            }
            
            $(this)
                    .setBorder(createBorder())
                    .setCursor(getDragCursor())
                    .setDraggable(true)
                    
                    ;
            
            
            
            
            
        }

        @Override
        protected boolean isStickyDrag() {
            return true;
        }
        
        

        @Override
        protected void initComponent() {
            super.initComponent(); 
            getComponentForm().setEnableCursors(true);
        }
        
        

        @Override
        protected Dimension calcPreferredSize() {
            Display d = Display.getInstance();
            switch (orientation) {
                case VERTICAL_SPLIT : return new Dimension(d.getDisplayWidth(), d.convertToPixels(3));
                default: return new Dimension(d.convertToPixels(3), d.getDisplayHeight());
            }
        }
        
        
        
        @Override
        public void pointerPressed(int x, int y) {
            
            
            super.pointerPressed(x, y);
            
            pressedX = x;
            pressedY = y;
            pressedConstraint = ((LayeredLayout)getLayout()).getOrCreateConstraint(this).copy();
            inDrag = true;
            pointerDragged(x, y);
        }

        @Override
        public void pointerDragged(int x, int y) {

            super.pointerDragged(x, y);
            if (!inDrag) {
                return;
            }
            setVisible(true);
            draggedX = x;
            draggedY = y;
            updateInsets();
            SplitPane.this.revalidate();
        }

        @Override
        public void pointerReleased(int x, int y) {
            super.pointerReleased(x, y); 
            inDrag = false;
        }

        @Override
        protected void dragFinished(int x, int y) {
            super.dragFinished(x, y); 
            inDrag = false;
        }
        
        
        
        private void updateInsets() {
            LayeredLayout ll = (LayeredLayout)SplitPane.this.getLayout();
            LayeredLayoutConstraint cnst = pressedConstraint.copy();
            if (orientation == HORIZONTAL_SPLIT) {
                cnst.left().translatePixels(draggedX - pressedX, false, this);
            } else {
                cnst.top().translatePixels(draggedY - pressedY, false, this);
            }
            cnst.copyTo(ll.getOrCreateConstraint(this));
            cnst.copyTo(preferredInset);
            clampInset();
            
        }

        @Override
        protected Image getDragImage() {
            return null;
        }

        @Override
        protected void drawDraggedImage(Graphics g, Image img, int x, int y) {
            
        }
        
        
        
        
        
        
    }
    
}
