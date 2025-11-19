package com.codename1.ui.spinner;

import com.codename1.junit.FormTest;
import com.codename1.junit.UITestBase;
import com.codename1.testing.TestCodenameOneImplementation;
import com.codename1.ui.Component;
import com.codename1.ui.Display;
import com.codename1.ui.Form;
import com.codename1.ui.Label;
import com.codename1.ui.List;
import com.codename1.ui.list.DefaultListModel;
import com.codename1.ui.layouts.BorderLayout;
import com.codename1.ui.layouts.BoxLayout;
import org.junit.jupiter.api.AfterEach;

import java.util.Calendar;
import java.util.Date;
import java.util.TimeZone;

import static org.junit.jupiter.api.Assertions.*;

class SpinnerComponentCoverageTest extends UITestBase {

    @AfterEach
    void resetRendererMode() {
        SpinnerRenderer.iOS7Mode = false;
    }

    @FormTest
    void calendarPickerTracksDateChanges() {
        CalendarPicker picker = new CalendarPicker();
        Calendar calendar = Calendar.getInstance();
        calendar.setTimeZone(TimeZone.getTimeZone("UTC"));
        calendar.set(2023, Calendar.FEBRUARY, 10, 0, 0, 0);
        Date target = calendar.getTime();

        picker.setValue(target);
        Date picked = (Date) picker.getValue();
        Calendar pickedCalendar = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
        pickedCalendar.setTime(picked);

        assertEquals(calendar.get(Calendar.YEAR), pickedCalendar.get(Calendar.YEAR));
        assertEquals(calendar.get(Calendar.MONTH), pickedCalendar.get(Calendar.MONTH));
        assertEquals(calendar.get(Calendar.DAY_OF_MONTH), pickedCalendar.get(Calendar.DAY_OF_MONTH));
    }

    @FormTest
    void dateTimeRendererFormatsValues() {
        DateTimeRenderer dateRenderer = DateTimeRenderer.createDateRenderer('-', Spinner.DATE_FORMAT_DOW_MON_DD_YY);
        Calendar calendar = Calendar.getInstance();
        calendar.set(2023, Calendar.MARCH, 4, 0, 0, 0);
        Date renderedDate = calendar.getTime();
        List list = new List(new DefaultListModel(renderedDate));

        Component dateComponent = dateRenderer.getListCellRendererComponent(list, renderedDate, 0, true);
        assertEquals("Sat-Mar-04-2023", ((Label) dateComponent).getText());

        DateTimeRenderer timeRenderer = DateTimeRenderer.createTimeRenderer(true, true);
        List timeList = new List(new DefaultListModel(Integer.valueOf(60 * 60 + 5)));
        Component timeComponent = timeRenderer.getListCellRendererComponent(timeList, Integer.valueOf(60 * 60 + 5), 0, true);
        assertEquals("01:00:05", ((Label) timeComponent).getText());
        assertTrue(timeRenderer.isShowSeconds());
    }

    @FormTest
    void spinnerRendererCalculatesPerspectiveAroundSelection() {
        SpinnerRenderer.iOS7Mode = true;
        SpinnerRenderer<Object> renderer = new SpinnerRenderer<Object>();
        DefaultListModel model = new DefaultListModel("A", "B", "C", "D");
        List list = new List(model);
        list.setFixedSelection(List.FIXED_CENTER);
        list.setSelectedIndex(1);

        renderer.getCellRendererComponent(list, model, "A", 0, false);
        assertEquals(1, renderer.perspective);

        renderer.getCellRendererComponent(list, model, "C", 2, false);
        assertEquals(6, renderer.perspective);
    }

    @FormTest
    void genericSpinnerHandlesMultipleColumnsAndValues() {
        GenericSpinner spinner = new GenericSpinner();
        spinner.setColumns(2);
        spinner.setModel(0, new DefaultListModel("One", "Two"));
        spinner.setModel(1, new DefaultListModel("First", "Second"));
        spinner.setRenderer(1, new SpinnerRenderer<Object>());
        spinner.setRenderingPrototype(0, "WWWWW");
        spinner.setRenderingPrototype(1, "WWWWWW");

        spinner.setValue(0, "Two");
        spinner.setValue(1, "Second");
        spinner.initSpinner();
        assertEquals("Two", spinner.getValue(0));
        assertEquals("Second", spinner.getValue(1));

        spinner.setColumns(1);
        assertEquals(1, spinner.getColumns());
        assertNotNull(spinner.getModel());
    }

    @FormTest
    void spinner3DAdaptersSyncValueAndSelection() {
        Spinner3D numeric = Spinner3D.create(0, 10, 5, 1);
        numeric.setValue(Integer.valueOf(7));
        assertEquals(Integer.valueOf(7), numeric.getValue());

        Calendar cal = Calendar.getInstance();
        cal.set(2024, Calendar.JANUARY, 2, 0, 0, 0);
        SpinnerDateModel dateModel = new SpinnerDateModel(cal.getTime().getTime(), cal.getTime().getTime() + 86400000L, cal.getTime().getTime());
        Spinner3D dates = Spinner3D.createDate(cal.getTime().getTime(), cal.getTime().getTime() + 86400000L * 2, cal.getTime().getTime());
        dates.setModel(dateModel);
        Date chosen = new Date(cal.getTime().getTime() + 86400000L);
        dates.setValue(chosen);
        assertEquals(chosen, dates.getValue());
    }

    @FormTest
    void durationSpinner3DConvertsMillisecondsToFields() {
        DurationSpinner3D spinner3D = new DurationSpinner3D(DurationSpinner3D.FIELD_DAY | DurationSpinner3D.FIELD_HOUR | DurationSpinner3D.FIELD_MINUTE | DurationSpinner3D.FIELD_SECOND);
        long oneDayOneHour = 1000L * 60L * 60L * 25L + 1000L * 90L;
        spinner3D.setValue(Long.valueOf(oneDayOneHour));

        long computed = ((Long) spinner3D.getValue()).longValue();
        assertEquals(1000L * 60L * 60L * 25L + 1000L * 60L + 1000L * 30L, computed);
    }

    @FormTest
    void dateAndTimeSpinnersUsePropertiesAndEvents() {
        DateSpinner dateSpinner = new DateSpinner();
        dateSpinner.setStartYear(2000);
        dateSpinner.setEndYear(2002);
        dateSpinner.setCurrentYear(2001);
        dateSpinner.setCurrentDay(5);
        dateSpinner.setCurrentMonth(6);
        dateSpinner.initSpinner();
        dateSpinner.setMonthDayYear(false);
        assertEquals(Boolean.FALSE, dateSpinner.getPropertyValue("monthDayYear"));

        TimeSpinner timeSpinner = new TimeSpinner();
        timeSpinner.setCurrentHour(3);
        timeSpinner.setCurrentMinute(30);
        timeSpinner.setMinuteStep(15);
        timeSpinner.setDurationMode(true);
        timeSpinner.initSpinner();

        Form form = new Form(BoxLayout.y());
        form.add(dateSpinner);
        form.add(timeSpinner);
        form.show();

        TestCodenameOneImplementation impl = TestCodenameOneImplementation.getInstance();
        impl.dispatchKeyPress('4');
        impl.dispatchKeyPress('5');
        assertEquals(30, timeSpinner.getCurrentMinute());
    }

    @FormTest
    void threeDimensionalDateAndTimeSpinnersExposeProperties() {
        DateTimeSpinner3D dateTime = new DateTimeSpinner3D();
        TimeSpinner3D time = new TimeSpinner3D();
        DateSpinner3D date = new DateSpinner3D();

        dateTime.setPropertyValue("currentDate", new Date(1700000000000L));
        assertNotNull(dateTime.getPropertyValue("currentDate"));

        time.setPropertyValue("currentHour", Integer.valueOf(10));
        time.setPropertyValue("currentMinute", Integer.valueOf(15));
        assertEquals(Integer.valueOf(10), time.getPropertyValue("currentHour"));

        date.setPropertyValue("currentYear", Integer.valueOf(2025));
        date.setPropertyValue("currentDay", Integer.valueOf(12));
        assertEquals(Integer.valueOf(2025), date.getPropertyValue("currentYear"));
    }

    @FormTest
    void pickerUsesInternalWidgetsWhenInvoked() {
        Picker picker = new Picker();
        picker.setType(Display.PICKER_TYPE_DATE_AND_TIME);
        picker.setUseLightweightPopup(true);
        picker.setUIID("Picker");

        Form form = new Form(new BorderLayout());
        form.add(BorderLayout.CENTER, picker);
        form.show();

        TestCodenameOneImplementation impl = TestCodenameOneImplementation.getInstance();
        impl.dispatchPointerPressAndRelease(form.getAbsoluteX() + picker.getX() + 1, form.getAbsoluteY() + picker.getY() + 1);
        picker.setDate(new Date(1700000000000L));
        assertEquals(1700000000000L, picker.getDate().getTime());
    }
}
