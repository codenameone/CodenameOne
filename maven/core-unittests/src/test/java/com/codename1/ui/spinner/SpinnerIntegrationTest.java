package com.codename1.ui.spinner;

import com.codename1.junit.FormTest;
import com.codename1.junit.UITestBase;
import com.codename1.ui.Form;
import com.codename1.ui.layouts.BoxLayout;

import java.util.Calendar;
import java.util.Date;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

class SpinnerIntegrationTest extends UITestBase {

    @FormTest
    void spinnerNumberModelSupportsBoundariesAndSelectionListeners() {
        SpinnerNumberModel model = new SpinnerNumberModel(0, -1, 2, 1);
        final AtomicInteger selectionChanges = new AtomicInteger();
        model.addSelectionListener(new com.codename1.ui.events.SelectionListener() {
            public void selectionChanged(int oldSelected, int newSelected) {
                selectionChanges.incrementAndGet();
            }
        });

        model.setValue(1);
        model.setSelectedIndex(2);
        assertEquals(2, model.getValue());
        assertEquals(2, selectionChanges.get());
    }

    @FormTest
    void dateModelReportsSelectedIndex() {
        Calendar cal = Calendar.getInstance();
        cal.set(2020, 0, 1, 10, 0, 0);
        Date baseDate = cal.getTime();
        long millis = baseDate.getTime();
        SpinnerDateModel dateModel = new SpinnerDateModel(millis, millis + 2 * 24 * 60 * 60 * 1000L, millis);
        assertEquals(0, dateModel.getSelectedIndex());
        dateModel.setSelectedIndex(1);
        assertEquals(1, dateModel.getSelectedIndex());
    }

    @FormTest
    void genericSpinnerUpdatesRendererDuringNavigation() {
        SpinnerNumberModel model = new SpinnerNumberModel(1, 0, 5, 1);
        GenericSpinner spinner = new GenericSpinner();
        spinner.setModel(model);
        spinner.setName("spinner");
        SpinnerRenderer renderer = new SpinnerRenderer();
        spinner.setRenderer(renderer);

        Form form = new Form(BoxLayout.y());
        form.add(spinner);
        form.show();

        spinner.setValue(3);
        spinner.revalidate();
        assertEquals(3, spinner.getValue());
        assertNotNull(renderer.getCellRendererComponent(spinner, spinner.getModel(), spinner.getValue(), 0, true));
    }
}
