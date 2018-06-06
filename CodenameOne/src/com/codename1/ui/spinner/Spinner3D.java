/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.codename1.ui.spinner;

import com.codename1.l10n.DateFormat;
import com.codename1.l10n.SimpleDateFormat;
import static com.codename1.ui.ComponentSelector.$;
import com.codename1.ui.Container;
import com.codename1.ui.Display;
import com.codename1.ui.Graphics;
import com.codename1.ui.Transform;
import com.codename1.ui.events.DataChangedListener;
import com.codename1.ui.events.ScrollListener;
import com.codename1.ui.events.SelectionListener;
import com.codename1.ui.geom.Dimension;
import com.codename1.ui.geom.Rectangle2D;
import com.codename1.ui.layouts.BoxLayout;
import com.codename1.ui.layouts.LayeredLayout;
import com.codename1.ui.list.ListModel;
import com.codename1.ui.plaf.Border;
import com.codename1.ui.plaf.Style;
import com.codename1.ui.scene.PerspectiveCamera;
import com.codename1.ui.scene.Scene;
import java.util.Calendar;
import java.util.Date;

/**
 * A spinner widget that tries to look and feel like the iOS picker.
 * 
 * This is used by the Picker widget when in lightweight mode.
 * @author Steve Hannah
 */
class Spinner3D extends Container implements InternalPickerWidget {
    private SpinnerNode root;
    
    private Scene scene; 
    private ScrollingContainer scroller;
    
    
    private static class ScrollingContainer extends Container {
        ScrollingContainer() {
            super(BoxLayout.y());
            getUnselectedStyle().setBorder(Border.createEmpty());
        }
        
        public void setScrollY(int scrollY) {
            super.setScrollY(scrollY);
        }
    }
    
    private static boolean usePerspective() {
        return false;//Transform.isPerspectiveSupported();
    }
    
    public int getSelectedIndex() {
        return root.getSelectedIndex();
    }
    
    /**
     * Creates a new Spinner3D with the given listModel.
     * @param listModel 
     */
    public Spinner3D(ListModel<String> listModel) {
        super(BoxLayout.y());
        setScrollableY(false);
        //getUnselectedStyle().setMargin(3f, 3f, 0, 0);
        //getUnselectedStyle().setMarginUnit(Style.UNIT_TYPE_DIPS, Style.UNIT_TYPE_DIPS, Style.UNIT_TYPE_DIPS, Style.UNIT_TYPE_DIPS);
        root = new SpinnerNode();
        scene = new Scene() {

            @Override
            public void setWidth(int width) {
                super.setWidth(width);
                root.boundsInLocal.get().setWidth(width);
            }

            @Override
            public void setHeight(int height) {
                super.setHeight(height);
                root.boundsInLocal.get().setHeight(height);
            }
            
            
            
        };
        scene.setName("Scene");
        //getAllStyles().setBgColor(root.getRowStyle().getBgColor());
        //getUnselectedStyle().setBgTransparency(255);
        
        
        root.boundsInLocal.get().setWidth(Display.getInstance().getDisplayWidth());
        root.boundsInLocal.get().setHeight(1000);
        setModel(listModel);
        //root.setRenderAsImage(true);
        
        scene.setRoot(root);
        
        if (usePerspective()) {
            scene.camera.set(new PerspectiveCamera(scene, 0.25, 1600, 1600+3000));
        }
        
        scroller = new ScrollingContainer() {
            
            

            @Override
            protected Dimension calcPreferredSize() {
                return new Dimension(500, (int)root.calcViewportHeight());
            }

            @Override
            protected Dimension calcScrollSize() {
                Dimension out = new Dimension(
                        500, 
                        (int)root.calcFlatListHeight() + getHeight() - (int)root.calcRowHeight()
                );
                return out;
            }

            @Override
            protected int getGridPosY() {
                //Log.p("In "+super.getScrollY());
                int rowHeight = (int)root.calcRowHeight();
                int scrollY = getScrollY();
                int out = scrollY;
                if (scrollY % rowHeight < rowHeight - scrollY % rowHeight) {
                    out = scrollY - (scrollY % rowHeight);
                } else {
                    out = scrollY + rowHeight - scrollY % rowHeight;
                }
                if (out > root.calcFlatListHeight() - rowHeight) {
                    out -= rowHeight;
                }
                if (out < 0) {
                    out = 0;
                }
                
                //Log.p("Out "+out);
                return out;
            }

            @Override
            public void pointerPressed(int x, int y) {
                super.pointerPressed(x, y);
                if (root.getSelectedRowOverlay().contains(x, y)) {
                    //Log.p("Hit on selectedRowOverlay");
                }
            }

            
            
            
        };
        scroller.setSnapToGrid(true);
        scroller.setScrollVisible(false);
        scroller.setScrollableY(true);
        scroller.setName("Scroller");
        
        scroller.addScrollListener(new ScrollListener() {

            @Override
            public void scrollChanged(int scrollX, int scrollY, int oldscrollX, int oldscrollY) {
                //Log.p(""+scrollY);
                root.setScrollY(scrollY);
            }
            
        });
        root.addScrollListener(new ScrollListener() {

            @Override
            public void scrollChanged(int scrollX, int scrollY, int oldscrollX, int oldscrollY) {
                if (Math.abs(scroller.getScrollY()-scrollY) > 2) {
                    scroller.setScrollY(scrollY);
                }
            }
            
        });
        $(scroller, scene).setMargin(0).setPadding(0);
        scroller.setScrollY((int)root.getScrollY());
        Container wrapper = LayeredLayout.encloseIn(scene, scroller);
        $(wrapper).setBorder(Border.createEmpty()).setMargin(0).setPadding(0).setBgTransparency(0);
        wrapper.setName("Wrapper");
        LayeredLayout ll = (LayeredLayout)wrapper.getLayout();
        //wrapper.add(scene).add(scroller);
        ll.setInsets(scroller, "0 0 auto 0")
                .setInsets(scene, "0 0 auto 0");
        add(wrapper);
    }

   

   
    
    
    
    
    public void setModel(ListModel model) {
        if (model instanceof SpinnerNumberModel) {
            model = new NumberModelAdapter((SpinnerNumberModel)model);
        }
        if (model instanceof SpinnerDateModel) {
            model = new DateModelAdapter((SpinnerDateModel)model);
        }
        root.setListModel(model);
        if (scroller != null) {
            scroller.setShouldCalcPreferredSize(true);
        }
        
    }
    
    @Override
    public Object getValue() {
        ListModel lm = root.getListModel();
        if (lm instanceof NumberModelAdapter) {
            NumberModelAdapter adapter = (NumberModelAdapter)lm;
            return adapter.inner.getItemAt(adapter.getSelectedIndex());
        }
        if (lm instanceof DateModelAdapter) {
            DateModelAdapter adapter = (DateModelAdapter)lm;
            return adapter.inner.getItemAt(adapter.getSelectedIndex());
        }
        return lm.getItemAt(lm.getSelectedIndex());
    }
    
    /**
     * Creates a new numeric spinner instance
     *
     * @param min lowest value allowed
     * @param max maximum value allowed
     * @param currentValue the starting value for the mode
     * @param step the value by which we increment the entries in the model
     * @return new spinner instance
     */
    public static Spinner3D create(double min, double max, double currentValue, double step) {
        Spinner3D s = new Spinner3D(new SpinnerNumberModel(min, max, currentValue, step));
        return s;
    }
    
    public static Spinner3D create(int min, int max, int currentValue, int step) {
        Spinner3D s = new Spinner3D(new SpinnerNumberModel(min, max, currentValue, step));
        return s;
    }
    
    /**
     * Creates a new date spinner instance
     *
     * @param min lowest value allowed
     * @param max maximum value allowed
     * @param currentValue the starting value for the mode
     * @param separatorChar character to separate the entries during rendering
     * @param format formatting type for the field
     * @return new spinner instance
     */
    public static Spinner3D createDate(long min, long max, long currentValue) {
        Spinner3D s = new Spinner3D(new SpinnerDateModel(min, max, currentValue));
        return s;
    }
    
    
    private static class DateModelAdapter implements ListModel<String> {
        final SpinnerDateModel inner;
        DateFormat fmt = new SimpleDateFormat("EEE MMM d");

        DateModelAdapter(SpinnerDateModel inner) {
            this.inner = inner;
        }
        
        @Override
        public String getItemAt(int index) {
            Date dt = (Date)inner.getItemAt(index);
            Calendar startToday = Calendar.getInstance();
            startToday.setTime(new Date());
            startToday.set(Calendar.HOUR_OF_DAY, 0);
            startToday.set(Calendar.MINUTE, 0);
            
            Calendar endToday = Calendar.getInstance();
            endToday.setTime(new Date());
            endToday.set(Calendar.HOUR_OF_DAY, 23);
            endToday.set(Calendar.MINUTE, 59);
            endToday.set(Calendar.SECOND, 59);
            
            if (dt.getTime() >= startToday.getTime().getTime() && dt.getTime() < endToday.getTime().getTime()) {
                return "Today";
            }
            return fmt.format(dt);
        }

        @Override
        public int getSize() {
            return inner.getSize();
        }

        @Override
        public int getSelectedIndex() {
            return inner.getSelectedIndex();
        }

        @Override
        public void setSelectedIndex(int index) {
            inner.setSelectedIndex(index);
        }

        @Override
        public void addDataChangedListener(DataChangedListener l) {
            inner.addDataChangedListener(l);
        }

        @Override
        public void removeDataChangedListener(DataChangedListener l) {
            inner.removeDataChangedListener(l);
        }

        @Override
        public void addSelectionListener(SelectionListener l) {
            inner.addSelectionListener(l);
        }

        @Override
        public void removeSelectionListener(SelectionListener l) {
            inner.removeSelectionListener(l);
        }

        @Override
        public void addItem(String item) {
            inner.addItem(item);
        }

        @Override
        public void removeItem(int index) {
            inner.removeItem(index);
        }
        
    };
    
    private static class NumberModelAdapter implements ListModel<String> {
        private final SpinnerNumberModel inner;
        
        NumberModelAdapter(SpinnerNumberModel inner) {
            this.inner = inner;
        }
        @Override
        public String getItemAt(int index) {
            return inner.getItemAt(index).toString();
        }

        @Override
        public int getSize() {
            return inner.getSize();
        }

        @Override
        public int getSelectedIndex() {
            return inner.getSelectedIndex();
        }

        @Override
        public void setSelectedIndex(int index) {
            inner.setSelectedIndex(index);
        }

        @Override
        public void addDataChangedListener(DataChangedListener l) {
            inner.addDataChangedListener(l);
        }

        @Override
        public void removeDataChangedListener(DataChangedListener l) {
            inner.removeDataChangedListener(l);
        }

        @Override
        public void addSelectionListener(SelectionListener l) {
            inner.addSelectionListener(l);
        }

        @Override
        public void removeSelectionListener(SelectionListener l) {
            inner.removeSelectionListener(l);
        }

        @Override
        public void addItem(String item) {
            inner.addItem(item);
        }

        @Override
        public void removeItem(int index) {
            inner.removeItem(index);
        }
        
    }
    
    
    public Style getRowStyle() {
        return root.getRowStyle();
    }
    
    public Style getSelectedRowStyle() {
        return root.getSelectedRowStyle();
    }
    
    public Style getSelectedOverlayStyle() {
        return root.getSelectedOverlayStyle();
    }
    
    //public void refreshStyles() {
    //    root.refreshStyles();
    //}
    
    
    void setRowFormatter(SpinnerNode.RowFormatter formatter) {
        root.setRowFormatter(formatter);
    }
    
    @Override
    public void setValue(Object value) {
        ListModel lm = root.getListModel();
        if (lm instanceof NumberModelAdapter) {
            NumberModelAdapter adapter = (NumberModelAdapter)lm;
            adapter.inner.setValue(value);
            return;
        }
        if (lm instanceof DateModelAdapter) {
            DateModelAdapter adapter = (DateModelAdapter)lm;
            adapter.inner.setValue((Date)value);
            return;
        }
        int len = lm.getSize();
        for (int i=0; i<len; i++) {
            Object val = lm.getItemAt(i);
            if (val != null && val.equals(value)) {
                lm.setSelectedIndex(i);
                break;
            }
        }
    }

    @Override
    public void paint(Graphics g) {
        int alpha = g.getAlpha();
        g.setColor(root.getSelectedOverlayStyle().getBgColor());
        g.setAlpha(255);
        g.fillRect(getX(), getY(), getWidth(), getHeight());
        g.setAlpha(alpha);
        super.paint(g);
        
    }

   
    
}
