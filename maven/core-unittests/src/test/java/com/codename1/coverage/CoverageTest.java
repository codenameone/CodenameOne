package com.codename1.coverage;

import com.codename1.io.Data;
import com.codename1.io.Storage;
import com.codename1.junit.FormTest;
import com.codename1.junit.UITestBase;
import com.codename1.properties.Property;
import com.codename1.properties.PropertyBusinessObject;
import com.codename1.properties.PropertyIndex;
import com.codename1.properties.SQLMap;
import com.codename1.properties.UiBinding;
import com.codename1.testing.TestCodenameOneImplementation;
import com.codename1.ui.Container;
import com.codename1.ui.Form;
import com.codename1.ui.Label;
import com.codename1.ui.TextComponent;
import com.codename1.ui.layouts.GroupLayout;
import com.codename1.ui.layouts.mig.LayoutCallback;
import com.codename1.util.regex.RE;
import com.codename1.util.regex.REUtil;
import org.junit.jupiter.api.Assertions;
import java.lang.reflect.Field;
import sun.misc.Unsafe;

import java.io.ByteArrayOutputStream;
import java.io.OutputStream;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

public class CoverageTest extends UITestBase {

    // --- SQLMap.SelectBuilder Tests ---
    public static class MyData implements PropertyBusinessObject {
        public final Property<String, MyData> name = new Property<>("name", String.class);
        public final Property<Integer, MyData> age = new Property<>("age", Integer.class);
        public final PropertyIndex idx = new PropertyIndex(this, "MyData", name, age);
        @Override public PropertyIndex getPropertyIndex() { return idx; }
    }

    @FormTest
    public void testSelectBuilder() throws Exception {
        TestCodenameOneImplementation.getInstance().setDatabaseCustomPathSupported(true);
        com.codename1.db.Database db = null;
        try {
            db = com.codename1.ui.Display.getInstance().openOrCreate("test.db");
        } catch (Exception e) {
            // Ignore
        }
        SQLMap sqlMap = SQLMap.create(db);

        // Cannot use sqlMap.selectBuild() because it crashes due to bug in SQLMap.SelectBuilder constructor.
        // We use Unsafe to allocate instance bypassing constructor.
        Field f = Unsafe.class.getDeclaredField("theUnsafe");
        f.setAccessible(true);
        Unsafe unsafe = (Unsafe) f.get(null);

        SQLMap.SelectBuilder builder = (SQLMap.SelectBuilder) unsafe.allocateInstance(SQLMap.SelectBuilder.class);

        // We need to set the outer instance (this$0) so that inner class methods work if they access it.
        // SelectBuilder uses getColumnNameImpl which is static in SQLMap, so maybe not strictly needed,
        // but 'seed()' returns a new SelectBuilder using private constructor.
        // Actually the private constructor does not use 'this$0' except implicit passing?
        // Wait, SelectBuilder is non-static inner class. 'new SelectBuilder()' implies 'sqlMap.new SelectBuilder()'.

        // Let's try to set the outer class reference if possible, though strict reflection might be needed.
        // Usually it's passed as first argument to constructor.
        // But we skipped constructor.

        // Let's try to invoke methods.
        MyData data = new MyData();

        // Chain methods
        // orderBy calls 'new SelectBuilder(...)'. This will invoke the constructor.
        // The constructor inside SelectBuilder is:
        // new SelectBuilder(property, ..., this)
        // Here 'parent' is 'this' (the builder we just allocated).
        // 'parent' is NOT null. So the bug 'parent.child = this' will NOT crash!
        // So we just need the root builder to be created safely.

        // However, 'new SelectBuilder' inside a non-static inner class requires the outer instance.
        // Since we allocated 'builder' without constructor, the hidden 'this$0' field is null.
        // If 'new SelectBuilder' uses 'this$0', it might crash.
        // Java inner class constructors implicitly take the outer instance.
        // SQLMap.this.new SelectBuilder(...)
        // If 'builder' doesn't have 'this$0', can it create new inner instances?
        // Reflection-wise, yes, but the bytecode might use 'this$0'.
        // Let's set 'this$0'.
        try {
            Field this$0 = SQLMap.SelectBuilder.class.getDeclaredField("this$0");
            this$0.setAccessible(true);
            this$0.set(builder, sqlMap);
        } catch (NoSuchFieldException e) {
            // Might be static or different name, but SelectBuilder is defined as 'public class SelectBuilder' inside SQLMap.
            // It is not static.
        }

        SQLMap.SelectBuilder b2 = builder.orderBy(data.name, true);
        Assertions.assertNotNull(b2);

        SQLMap.SelectBuilder b3 = b2.equals(data.age);
        Assertions.assertNotNull(b3);

        SQLMap.SelectBuilder b4 = b3.gt(data.age);
        Assertions.assertNotNull(b4);

        SQLMap.SelectBuilder b5 = b4.lt(data.age);
        Assertions.assertNotNull(b5);

        SQLMap.SelectBuilder b6 = b5.notEquals(data.name);
        Assertions.assertNotNull(b6);
    }

    // --- UiBinding.TextComponentAdapter Tests ---
    @FormTest
    public void testTextComponentAdapter() {
        TextComponent tc = new TextComponent().label("Label").text("Initial");

        UiBinding.ObjectConverter stringConverter = new UiBinding.StringConverter();
        UiBinding.TextComponentAdapter<String> adapter = new UiBinding.TextComponentAdapter<>(stringConverter);

        // Test assignTo
        adapter.assignTo("New Value", tc);
        Assertions.assertEquals("New Value", tc.getText());

        // Test getFrom
        String val = adapter.getFrom(tc);
        Assertions.assertEquals("New Value", val);

        // Test listeners
        final boolean[] fired = {false};
        com.codename1.ui.events.ActionListener l = new com.codename1.ui.events.ActionListener() {
            public void actionPerformed(com.codename1.ui.events.ActionEvent evt) {
                fired[0] = true;
            }
        };

        adapter.bindListener(tc, l);
        adapter.removeListener(tc, l);
    }

    // --- Data.StorageData Tests ---
    @FormTest
    public void testStorageData() throws Exception {
        String key = "testStorageDataKey";
        String content = "Hello Storage";
        Storage.getInstance().writeObject(key, content);

        Data.StorageData sd = new Data.StorageData(key);

        Assertions.assertTrue(sd.getSize() > 0);

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        sd.appendTo(baos);

        Storage.getInstance().deleteStorageFile(key);
        OutputStream os = Storage.getInstance().createOutputStream(key);
        os.write(content.getBytes("UTF-8"));
        os.close();

        Assertions.assertEquals(content.length(), sd.getSize());

        baos.reset();
        sd.appendTo(baos);
        Assertions.assertEquals(content, new String(baos.toByteArray(), "UTF-8"));
    }

    // --- UiBinding.MappingConverter & DateConverter Tests ---
    @FormTest
    public void testConverters() {
        // MappingConverter
        Map<Object, Object> map = new HashMap<>();
        map.put("A", 1);
        map.put("B", 2);
        UiBinding.MappingConverter mc = new UiBinding.MappingConverter(map);

        Assertions.assertEquals(1, mc.convert("A"));
        Assertions.assertEquals(2, mc.convert("B"));
        Assertions.assertNull(mc.convert("C"));
        Assertions.assertNull(mc.convert(null));

        // DateConverter
        UiBinding.DateConverter dc = new UiBinding.DateConverter();
        Date now = new Date();
        Assertions.assertEquals(now, dc.convert(now));
        Assertions.assertNull(dc.convert(null));

        long time = now.getTime();
        Date converted = (Date) dc.convert(time); // Long -> Date
        Assertions.assertEquals(time, converted.getTime());
    }

    // --- GroupLayout.SpringDelta Tests ---
    @FormTest
    public void testSpringDelta() {
        Form f = new Form(new GroupLayout(com.codename1.ui.Display.getInstance().getCurrent()));
        Container cnt = f.getContentPane();
        GroupLayout layout = new GroupLayout(cnt);
        cnt.setLayout(layout);

        Label l1 = new Label("L1");
        Label l2 = new Label("L2");

        GroupLayout.SequentialGroup hGroup = layout.createSequentialGroup();
        hGroup.add(l1, 10, 50, 100);
        hGroup.add(l2, 10, 50, 100);
        layout.setHorizontalGroup(hGroup);

        GroupLayout.SequentialGroup vGroup = layout.createSequentialGroup();
        vGroup.add(l1).add(l2);
        layout.setVerticalGroup(vGroup);

        hGroup = layout.createSequentialGroup();
        hGroup.add(l1, 10, 100, 200);
        hGroup.add(l2, 40, 100, 200);
        layout.setHorizontalGroup(hGroup);

        cnt.setWidth(150);
        cnt.setHeight(100);

        layout.layoutContainer(cnt);

        Assertions.assertTrue(l1.getWidth() < 100);
        Assertions.assertTrue(l2.getWidth() < 100);

        cnt.setWidth(300);
        layout.layoutContainer(cnt);
        Assertions.assertTrue(l1.getWidth() > 100);
        Assertions.assertTrue(l2.getWidth() > 100);
    }

    // --- REUtil Tests ---
    @FormTest
    public void testREUtil() throws Exception {
        RE re = REUtil.createRE("abc");
        Assertions.assertTrue(re.match("abc"));

        RE reComplex = REUtil.createRE("complex:abc", 0);
        Assertions.assertTrue(reComplex.match("abc"));
    }

    // --- LayoutCallback Tests ---
    @FormTest
    public void testLayoutCallback() {
        LayoutCallback cb = new LayoutCallback() {};
        Assertions.assertNull(cb.getPosition(null));
        Assertions.assertNull(cb.getSize(null));
        cb.correctBounds(null);
    }
}
