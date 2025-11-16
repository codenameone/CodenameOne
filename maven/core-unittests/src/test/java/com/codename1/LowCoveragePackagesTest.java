package com.codename1;

import com.codename1.capture.Capture;
import com.codename1.capture.VideoCaptureConstraints;
import com.codename1.charts.ChartUtil;
import com.codename1.charts.compat.Canvas;
import com.codename1.charts.compat.Paint;
import com.codename1.charts.models.Point;
import com.codename1.charts.models.SeriesSelection;
import com.codename1.charts.renderers.SimpleSeriesRenderer;
import com.codename1.charts.views.AbstractChart;
import com.codename1.db.Cursor;
import com.codename1.db.Database;
import com.codename1.db.Row;
import com.codename1.junit.FormTest;
import com.codename1.junit.UITestBase;
import com.codename1.properties.Property;
import com.codename1.properties.PropertyBusinessObject;
import com.codename1.properties.PropertyIndex;
import com.codename1.testing.TestCodenameOneImplementation;
import com.codename1.testing.TestUtils;
import com.codename1.ui.Form;
import com.codename1.ui.Image;
import com.codename1.ui.Label;
import com.codename1.ui.List;
import com.codename1.ui.Button;
import com.codename1.ui.Container;
import com.codename1.ui.TextField;
import com.codename1.ui.events.ActionEvent;
import com.codename1.ui.events.ActionListener;
import com.codename1.ui.events.DataChangedListener;
import com.codename1.ui.list.DefaultListModel;
import com.codename1.ui.list.FilterProxyListModel;
import com.codename1.ui.list.ContainerList;
import com.codename1.ui.spinner.SpinnerNumberModel;
import com.codename1.ui.util.EventDispatcher;
import com.codename1.ui.validation.LengthConstraint;
import com.codename1.ui.validation.RegexConstraint;
import com.codename1.ui.validation.Validator;
import com.codename1.util.AsyncResource;
import com.codename1.util.AsyncExecutionException;
import com.codename1.util.SuccessCallback;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

class LowCoveragePackagesTest extends UITestBase {

    @FormTest
    void testFilterProxyListModelFiltersAndSorts() {
        DefaultListModel<String> base = new DefaultListModel<String>(new String[]{"Charlie", "Alice", "Bob"});
        FilterProxyListModel<String> proxy = new FilterProxyListModel<String>(base);
        final AtomicInteger changeCount = new AtomicInteger();
        proxy.addDataChangedListener(new DataChangedListener() {
            public void dataChanged(int type, int index) {
                changeCount.incrementAndGet();
            }
        });

        proxy.filter("o");
        assertEquals(1, proxy.getSize());
        assertEquals("Bob", proxy.getItemAt(0));

        proxy.sort(true);
        assertEquals("Bob", proxy.getItemAt(0));
        assertTrue(changeCount.get() > 0);

        proxy.filter("a");
        assertEquals(2, proxy.getSize());
        assertEquals("Charlie", proxy.getItemAt(1));
    }

    @FormTest
    void testFilterProxyListModelRefreshesWithUnderlyingChanges() {
        DefaultListModel<String> base = new DefaultListModel<String>(new String[]{"dog", "cat"});
        FilterProxyListModel<String> proxy = new FilterProxyListModel<String>(base);
        final AtomicInteger changes = new AtomicInteger();
        proxy.addDataChangedListener(new DataChangedListener() {
            public void dataChanged(int type, int index) {
                changes.incrementAndGet();
            }
        });

        proxy.filter("o");
        assertEquals(1, proxy.getSize());
        base.addItem("otter");
        proxy.update();
        assertEquals(2, proxy.getSize());

        proxy.filter(null);
        assertEquals(3, proxy.getSize());
        assertTrue(changes.get() >= 2);
    }

    private static class Person extends PropertyBusinessObject {
        final Property<String, String> name = new Property<String, String>("name", String.class);
        final Property<Integer, Integer> age = new Property<Integer, Integer>("age", Integer.class);
        private final PropertyIndex index = new PropertyIndex(this, "Person", name, age);

        public PropertyIndex getPropertyIndex() {
            return index;
        }
    }

    @FormTest
    void testPropertyIndexTracksChangesAndSerialization() throws IOException {
        final Person person = new Person();
        final AtomicInteger callbacks = new AtomicInteger();
        person.getPropertyIndex().addPropertyChangeListener(new com.codename1.properties.PropertyChangeListener() {
            public void propertyChanged(String propertyName, Object oldValue, Object newValue) {
                callbacks.incrementAndGet();
                assertEquals(person.name.getName(), propertyName);
            }
        });

        person.name.set("Eve");
        person.age.set(30);
        assertEquals(2, callbacks.get());

        ByteArrayOutputStream bout = new ByteArrayOutputStream();
        DataOutputStream dout = new DataOutputStream(bout);
        person.getPropertyIndex().getPropertyIndexState().save(dout);

        ByteArrayInputStream bin = new ByteArrayInputStream(bout.toByteArray());
        DataInputStream din = new DataInputStream(bin);
        Person restored = new Person();
        restored.getPropertyIndex().getPropertyIndexState().load(din);
        assertEquals("Eve", restored.name.get());
        assertEquals(Integer.valueOf(30), restored.age.get());
    }

    @FormTest
    void testPropertyIndexLookupAndJSONList() {
        Person alice = new Person();
        alice.name.set("Alice");
        alice.age.set(18);
        Person bob = new Person();
        bob.name.set("Bob");
        bob.age.set(20);

        PropertyIndex index = alice.getPropertyIndex();
        assertEquals(2, index.getSize());
        assertSame(alice.name, index.get("name"));
        assertSame(alice.age, index.getIgnoreCase("AGE"));

        String json = PropertyIndex.toJSONList(Arrays.asList(alice, bob));
        assertTrue(json.contains("Alice"));
        assertTrue(json.contains("Bob"));
    }

    private static class CountingChart extends AbstractChart {
        private final AtomicInteger drawCount = new AtomicInteger();

        public int getDrawCount() {
            return drawCount.get();
        }

        public SeriesSelection getSeriesAndPointForScreenCoordinate(Point point) {
            return new SeriesSelection(0, 0, 1, 1);
        }

        public void draw(Canvas canvas, int x, int y, int width, int height, Paint paint) {
            drawCount.incrementAndGet();
        }

        public int getLegendShapeWidth(int seriesIndex) {
            return 1;
        }

        public void drawLegendShape(Canvas canvas, SimpleSeriesRenderer renderer, float x, float y, Paint paint) {
            paint.setColor(0xff00ff);
        }

        public String getChartType() {
            return "Counting";
        }
    }

    @FormTest
    void testChartUtilDelegatesToChart() {
        Image img = Image.createImage(10, 10);
        ChartUtil util = new ChartUtil();
        CountingChart chart = new CountingChart();

        util.paintChart(img.getGraphics(), chart, new com.codename1.ui.geom.Rectangle(0, 0, 10, 10), 2, 3);
        assertEquals(1, chart.getDrawCount());
    }

    @FormTest
    void testValidatorEnforcesConstraints() {
        Validator validator = new Validator();
        TextField field = new TextField();
        validator.addConstraint(field, new LengthConstraint(2, "Too short"));
        validator.addConstraint(field, new RegexConstraint("[A-Z].+", "Must start with capital"));

        field.setText("a");
        assertFalse(validator.isValid());

        field.setText("Abc");
        assertTrue(validator.isValid());
    }

    @FormTest
    void testValidatorGlobalConstraintAppliesToAllFields() {
        Validator validator = new Validator();
        TextField fieldA = new TextField();
        TextField fieldB = new TextField();
        LengthConstraint minLength = new LengthConstraint(3, "Too short");

        validator.addConstraint(fieldA, new RegexConstraint("[a-z]+", "lower"));
        validator.addGlobalConstraint(minLength);
        fieldA.setText("ab");
        fieldB.setText("c");
        assertFalse(validator.isValid());

        validator.removeConstraint(fieldA, minLength);
        fieldA.setText("abcd");
        fieldB.setText("def");
        assertTrue(validator.isValid());
    }

    @FormTest
    void testCaptureApisReturnMockedPaths() {
        implementation.setNextCapturePhotoPath("file://photo.jpg");
        implementation.setNextCaptureAudioPath("file://audio.wav");
        implementation.setNextCaptureVideoPath("file://video.mp4");

        final List<String> captured = new ArrayList<String>();
        Capture.capturePhoto(new ActionListener() {
            public void actionPerformed(ActionEvent evt) {
                captured.add((String) evt.getSource());
            }
        });
        Capture.captureAudio(new ActionListener() {
            public void actionPerformed(ActionEvent evt) {
                captured.add((String) evt.getSource());
            }
        });
        Capture.captureVideo(new ActionListener() {
            public void actionPerformed(ActionEvent evt) {
                captured.add((String) evt.getSource());
            }
        });

        assertEquals("file://photo.jpg", captured.get(0));
        assertEquals("file://audio.wav", captured.get(1));
        assertEquals("file://video.mp4", captured.get(2));
        assertNotNull(implementation.getLastMediaRecorderBuilder());
    }

    @FormTest
    void testCaptureVideoWithConstraintsRecordsRequest() {
        VideoCaptureConstraints constraints = new VideoCaptureConstraints();
        constraints.setResolution(VideoCaptureConstraints.RESOLUTION_1080P);
        constraints.setFrameRate(30);
        implementation.setNextCaptureVideoPath("file://with-constraints.mp4");

        final List<String> results = new ArrayList<String>();
        Capture.captureVideo(constraints, new ActionListener() {
            public void actionPerformed(ActionEvent evt) {
                results.add((String) evt.getSource());
            }
        });

        assertEquals("file://with-constraints.mp4", results.get(0));
        assertEquals(constraints, implementation.getLastVideoConstraints());
    }

    @FormTest
    void testTestUtilsSelectsListEntries() {
        List list = new List(new DefaultListModel<String>(new String[]{"a", "b", "c"}));
        list.setName("myList");
        Form f = new Form();
        f.setLayout(new com.codename1.ui.layouts.BorderLayout());
        f.addComponent(com.codename1.ui.layouts.BorderLayout.CENTER, list);
        f.show();

        TestUtils.selectInList("myList", 2);
        assertEquals(2, list.getSelectedIndex());
    }

    @FormTest
    void testTestUtilsFindLabelAndContainerList() {
        Container root = new Container(new com.codename1.ui.layouts.BoxLayout(com.codename1.ui.layouts.BoxLayout.Y_AXIS));
        Label label = new Label("search-me");
        label.setName("targetLabel");
        root.add(label);
        ContainerList list = new ContainerList();
        list.setName("containerList");
        root.add(list);

        Form f = new Form(root);
        f.show();

        assertSame(label, TestUtils.findByName("targetLabel"));
        TestUtils.selectInList("containerList", 0);
        assertEquals(0, list.getSelectedIndex());
    }

    @FormTest
    void testShareServiceFinishRestoresForm() {
        final AtomicBoolean finished = new AtomicBoolean();
        Form original = new Form(new Label("Origin"));
        original.show();

        ShareHarness share = new ShareHarness("Share", null);
        share.setOriginalForm(original);
        Form target = new Form(new Label("Target"));
        target.show();

        share.finish();
        assertSame(original, com.codename1.ui.Display.getInstance().getCurrent());
        finished.set(true);
        assertTrue(finished.get());
    }

    @FormTest
    void testShareServiceMetadata() {
        Image icon = Image.createImage(5, 5);
        ShareHarness share = new ShareHarness("ShareMe", icon);
        share.setOriginalForm(new Form("Original"));
        assertEquals("ShareMe", share.getDisplayName());
        assertSame(icon, share.getIcon());
        assertTrue(share.canShareImage());
    }

    private static class ShareHarness extends com.codename1.share.ShareService {
        ShareHarness(String name, Image icon) {
            super(name, icon);
        }

        public void share(String text) {
        }

        public boolean canShareImage() {
            return true;
        }
    }

    @FormTest
    void testEventDispatcherNotifiesListeners() {
        EventDispatcher dispatcher = new EventDispatcher(true);
        final AtomicInteger calls = new AtomicInteger();
        ActionListener listener = new ActionListener() {
            public void actionPerformed(ActionEvent evt) {
                calls.incrementAndGet();
            }
        };
        dispatcher.addListener(listener);
        dispatcher.fireActionEvent(new ActionEvent("payload"));
        assertEquals(1, calls.get());
        dispatcher.removeListener(listener);
        dispatcher.fireActionEvent(new ActionEvent("payload"));
        assertEquals(1, calls.get());
    }

    @FormTest
    void testEventDispatcherNonEdtDataChange() {
        EventDispatcher dispatcher = new EventDispatcher(true);
        final AtomicInteger dataChanges = new AtomicInteger();
        dispatcher.addListener(new DataChangedListener() {
            public void dataChanged(int type, int index) {
                dataChanges.incrementAndGet();
            }
        });

        dispatcher.fireDataChangeEvent(1, 2);
        flushSerialCalls();
        assertEquals(1, dataChanges.get());
    }

    @FormTest
    void testSpinnerModelFiresChange() {
        SpinnerNumberModel model = new SpinnerNumberModel(1, 0, 10, 1);
        final AtomicInteger changes = new AtomicInteger();
        model.addDataChangedListener(new DataChangedListener() {
            public void dataChanged(int type, int index) {
                changes.incrementAndGet();
            }
        });

        model.setValue(5);
        assertEquals(5, model.getValue());
        assertEquals(1, changes.get());
    }

    @FormTest
    void testSpinnerModelSelectionEvents() {
        SpinnerNumberModel model = new SpinnerNumberModel(0, 0, 2, 1);
        final AtomicInteger selections = new AtomicInteger();
        model.addSelectionListener(new com.codename1.ui.events.SelectionListener() {
            public void selectionChanged(int oldSelected, int newSelected) {
                selections.incrementAndGet();
            }
        });

        model.setSelectedIndex(1);
        model.setSelectedIndex(2);
        assertEquals(2, selections.get());
    }

    @FormTest
    void testAsyncResourceResolvesCallbacks() {
        AsyncResource<String> resource = new AsyncResource<String>();
        final AtomicBoolean success = new AtomicBoolean();
        resource.onSuccess(new SuccessCallback<String>() {
            public void onSucess(String value) {
                success.set(true);
            }
        });

        resource.complete("done");
        assertTrue(success.get());
        assertEquals("done", resource.get());
    }

    @FormTest
    void testAsyncResourceAllAndAwait() throws AsyncExecutionException {
        AsyncResource<String> first = new AsyncResource<String>();
        AsyncResource<String> second = new AsyncResource<String>();
        AsyncResource<Boolean> combined = AsyncResource.all(first, second);

        final AtomicBoolean ready = new AtomicBoolean();
        combined.onSuccess(new SuccessCallback<Boolean>() {
            public void onSucess(Boolean value) {
                ready.set(value.booleanValue());
            }
        });

        first.complete("one");
        assertFalse(ready.get());
        second.complete("two");
        assertTrue(ready.get());

        AsyncResource.await(Arrays.asList(first, second));
    }

    @FormTest
    void testDatabaseWrapperTracksStatements() throws Exception {
        Database db = implementation.openOrCreateDB("test.db");
        TestCodenameOneImplementation.TestDatabase testDb = implementation.getTestDatabase("test.db");
        assertNotNull(testDb);
        db.execute("create table demo(id int)");
        db.execute("insert into demo values(?)", new String[]{"1"});

        testDb.setQueryResult(new String[]{"id"}, new Object[][]{{"1"}});
        Cursor cursor = db.executeQuery("select * from demo");
        assertTrue(cursor.first());
        Row row = cursor.getRow();
        assertEquals("1", row.getString(0));
        assertEquals(2, testDb.getExecutedStatements().size());
        assertEquals("insert into demo values(?)", testDb.getExecutedStatements().get(1));
        db.close();
        assertTrue(testDb.isClosed());
    }
}

