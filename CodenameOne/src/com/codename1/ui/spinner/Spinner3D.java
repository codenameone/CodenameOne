/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.codename1.ui.spinner;

import com.codename1.l10n.DateFormat;
import com.codename1.l10n.SimpleDateFormat;
import com.codename1.ui.Container;
import com.codename1.ui.Display;
import com.codename1.ui.Graphics;
import com.codename1.ui.events.DataChangedListener;
import com.codename1.ui.events.ScrollListener;
import com.codename1.ui.events.SelectionListener;
import com.codename1.ui.geom.Dimension;
import com.codename1.ui.layouts.BoxLayout;
import com.codename1.ui.layouts.LayeredLayout;
import com.codename1.ui.list.ListModel;
import com.codename1.ui.plaf.Border;
import com.codename1.ui.plaf.Style;
import com.codename1.ui.scene.PerspectiveCamera;
import com.codename1.ui.scene.Scene;

import java.util.Calendar;
import java.util.Date;

import static com.codename1.ui.ComponentSelector.$;

/**
 * A spinner widget that tries to look and feel like the iOS picker.
 * <p>
 * This is used by the Picker widget when in lightweight mode.
 *
 * @author Steve Hannah
 */
class Spinner3D extends Container implements InternalPickerWidget {
    private SpinnerNode root;

    private Scene scene;
    private ScrollingContainer scroller;

    private boolean gridPosDirty = true;


    /**
     * Creates a new Spinner3D with the given listModel.
     *
     * @param listModel
     */
    public Spinner3D(ListModel<String> listModel) {
        super(BoxLayout.y());
        setScrollableY(false);
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
        root.boundsInLocal.get().setWidth(Display.getInstance().getDisplayWidth());
        root.boundsInLocal.get().setHeight(1000);
        setModel(listModel);
        scene.setRoot(root);

        if (usePerspective()) {
            scene.camera.set(new PerspectiveCamera(scene, 0.25, 1600, 1600 + 3000));
        }

        scroller = new ScrollingContainer() {
            @Override
            protected Dimension calcPreferredSize() {
                return new Dimension(500, (int) root.calcViewportHeight());
            }

            @Override
            protected Dimension calcScrollSize() {
                final int viewportHeight = (int) root.calcViewportHeight();
                final int listHeight = (int) root.calcFlatListHeight();
                final int rowHeight = (int) root.calcRowHeight();
                return new Dimension(
                        500, // Width doesn't matter - only doing Y scroll.
                        Math.max(viewportHeight, listHeight + (int) (6 * rowHeight))
                );
            }

            @Override
            protected int getGridPosY() {
                final int rowHeight = (int) root.calcRowHeight();
                final int scrollY = getScrollY();
                final int rowOffsetY = scrollY % rowHeight;
                final boolean roundUp = rowOffsetY > rowHeight - rowOffsetY;
                return (int) Math.min(
                        root.calcFlatListHeight() - rowHeight,
                        Math.max(
                                0,
                                roundUp
                                        ? (scrollY + rowHeight - rowOffsetY)
                                        : (scrollY - rowOffsetY)
                        )
                );
            }

            @Override
            protected void onScrollY(int scrollY) {
                super.onScrollY(scrollY);
            }
        };
        scroller.setSnapToGrid(true);
        scroller.setScrollVisible(false);
        scroller.setScrollableY(true);
        scroller.setName("Scroller");
        scroller.addScrollListener(new ScrollListener() {
            public void scrollChanged(int scrollX, int scrollY, int oldscrollX, int oldscrollY) {
                root.setScrollY(scrollY);
            }
        });
        root.addScrollListener(new ScrollListener() {
            public void scrollChanged(int scrollX, int scrollY, int oldscrollX, int oldscrollY) {
                scroller.setScrollY(scrollY);
            }
        });

        $(scroller, scene).setMargin(0).setPadding(0);
        scroller.setScrollY((int) root.getScrollY());
        Container wrapper = LayeredLayout.encloseIn(scene, scroller);
        $(wrapper).setBorder(Border.createEmpty()).setMargin(0).setPadding(0).setBgTransparency(0);
        wrapper.setName("Wrapper");
        LayeredLayout ll = (LayeredLayout) wrapper.getLayout();
        ll.setInsets(scroller, "0 0 auto 0")
                .setInsets(scene, "0 0 auto 0");
        add(wrapper);
    }

    private static boolean usePerspective() {
        return false;//Transform.isPerspectiveSupported();
    }

    /**
     * Creates a new numeric spinner instance
     *
     * @param min          lowest value allowed
     * @param max          maximum value allowed
     * @param currentValue the starting value for the mode
     * @param step         the value by which we increment the entries in the model
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
     * @param min          lowest value allowed
     * @param max          maximum value allowed
     * @param currentValue the starting value for the mode
     * @return new spinner instance
     */
    public static Spinner3D createDate(long min, long max, long currentValue) {
        Spinner3D s = new Spinner3D(new SpinnerDateModel(min, max, currentValue));
        return s;
    }

    public int getSelectedIndex() {
        return root.getSelectedIndex();
    }

    public void setModel(ListModel model) {
        if (model instanceof SpinnerNumberModel) {
            model = new NumberModelAdapter((SpinnerNumberModel) model);
        }
        if (model instanceof SpinnerDateModel) {
            model = new DateModelAdapter((SpinnerDateModel) model);
        }
        root.setListModel(model);
        if (scroller != null) {
            scroller.setShouldCalcPreferredSize(true);
        }

    }

    public Object getValue() {
        ListModel lm = root.getListModel();
        if (lm instanceof NumberModelAdapter) {
            NumberModelAdapter adapter = (NumberModelAdapter) lm;
            int selectedIndex = adapter.getSelectedIndex();
            Object out = adapter.inner.getItemAt(selectedIndex);

            return out;
        }
        if (lm instanceof DateModelAdapter) {
            DateModelAdapter adapter = (DateModelAdapter) lm;
            return adapter.inner.getItemAt(adapter.getSelectedIndex());
        }
        return lm.getItemAt(lm.getSelectedIndex());
    }

    public void setValue(Object value) {
        ListModel lm = root.getListModel();
        if (lm instanceof NumberModelAdapter) {
            NumberModelAdapter adapter = (NumberModelAdapter) lm;
            adapter.inner.setValue(value);
            return;
        }
        if (lm instanceof DateModelAdapter) {
            DateModelAdapter adapter = (DateModelAdapter) lm;
            adapter.inner.setValue((Date) value);
            return;
        }
        int len = lm.getSize();
        for (int i = 0; i < len; i++) {
            Object val = lm.getItemAt(i);
            if (val != null && val.equals(value)) {
                lm.setSelectedIndex(i);
                break;
            }
        }
    }

    public Style getRowStyle() {
        return root.getRowStyle();
    }

    ;

    public Style getSelectedRowStyle() {
        return root.getSelectedRowStyle();
    }

    public Style getSelectedOverlayStyle() {
        return root.getSelectedOverlayStyle();
    }

    void setRowFormatter(SpinnerNode.RowFormatter formatter) {
        root.setRowFormatter(formatter);
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

    private static class ScrollingContainer extends Container {
        ScrollingContainer() {
            super(BoxLayout.y());
            getUnselectedStyle().setBorder(Border.createEmpty());
        }

        public void setScrollY(int scrollY) {
            super.setScrollY(scrollY);
        }
    }

    private static class DateModelAdapter implements ListModel<String> {
        final SpinnerDateModel inner;
        DateFormat fmt = new SimpleDateFormat("EEE MMM d");

        DateModelAdapter(SpinnerDateModel inner) {
            this.inner = inner;
        }

        public String getItemAt(int index) {
            Date dt = (Date) inner.getItemAt(index);
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

        public int getSize() {
            return inner.getSize();
        }

        public int getSelectedIndex() {
            return inner.getSelectedIndex();
        }

        public void setSelectedIndex(int index) {
            inner.setSelectedIndex(index);
        }

        public void addDataChangedListener(DataChangedListener l) {
            inner.addDataChangedListener(l);
        }

        public void removeDataChangedListener(DataChangedListener l) {
            inner.removeDataChangedListener(l);
        }

        public void addSelectionListener(SelectionListener l) {
            inner.addSelectionListener(l);
        }

        public void removeSelectionListener(SelectionListener l) {
            inner.removeSelectionListener(l);
        }

        public void addItem(String item) {
            inner.addItem(item);
        }

        public void removeItem(int index) {
            inner.removeItem(index);
        }

    }

    private static class NumberModelAdapter implements ListModel<String> {
        private final SpinnerNumberModel inner;

        NumberModelAdapter(SpinnerNumberModel inner) {
            this.inner = inner;
        }

        public String getItemAt(int index) {
            return inner.getItemAt(index).toString();
        }

        public int getSize() {
            return inner.getSize();
        }

        public int getSelectedIndex() {
            return inner.getSelectedIndex();
        }

        public void setSelectedIndex(int index) {
            inner.setSelectedIndex(index);
        }

        public void addDataChangedListener(DataChangedListener l) {
            inner.addDataChangedListener(l);
        }

        public void removeDataChangedListener(DataChangedListener l) {
            inner.removeDataChangedListener(l);
        }

        public void addSelectionListener(SelectionListener l) {
            inner.addSelectionListener(l);
        }

        public void removeSelectionListener(SelectionListener l) {
            inner.removeSelectionListener(l);
        }

        public void addItem(String item) {
            inner.addItem(item);
        }

        public void removeItem(int index) {
            inner.removeItem(index);
        }

    }
}

