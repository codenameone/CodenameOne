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
    void dateAndDurationModelsAdjustValues() {
        Calendar cal = Calendar.getInstance();
        cal.set(2020, 0, 1, 10, 0, 0);
        Date baseDate = cal.getTime();
        SpinnerDateModel dateModel = new SpinnerDateModel(baseDate);
        dateModel.setValue(baseDate);
        dateModel.setStep(SpinnerDateModel.DAY_STEP);
        dateModel.increment();
        assertTrue(((Date) dateModel.getValue()).after(baseDate));

        DurationSpinner3D duration = new DurationSpinner3D();
        duration.setDuration(90 * 60 * 1000);
        duration.increment();
        assertTrue(duration.getDuration() > 90 * 60 * 1000);
    }

    @FormTest
    void genericSpinnerUpdatesRendererDuringNavigation() {
        SpinnerNumberModel model = new SpinnerNumberModel(1, 0, 5, 1);
        GenericSpinner spinner = new GenericSpinner(model);
        spinner.setName("spinner");
        SpinnerRenderer renderer = new SpinnerRenderer();
        spinner.setRenderer(renderer);

        Form form = new Form(BoxLayout.y());
        form.add(spinner);
        form.show();

        spinner.setValue(3);
        spinner.revalidate();
        assertEquals(3, spinner.getValue());
        renderer.updateValue(spinner, spinner.getValue());
        assertNotNull(renderer.getCurrentValue());
    }
}
