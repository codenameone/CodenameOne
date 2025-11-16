package com.codename1.properties;

import com.codename1.junit.FormTest;
import com.codename1.junit.UITestBase;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.function.Executable;

import static org.junit.jupiter.api.Assertions.*;

class PrimitivePropertiesTest extends UITestBase {

    static class PrimitiveHolder extends PropertyBusinessObject {
        final BooleanProperty<Boolean> boolDefault = new BooleanProperty<Boolean>("flag");
        final BooleanProperty<Boolean> boolValue = new BooleanProperty<Boolean>("flagWithValue", Boolean.TRUE);
        final ByteProperty<PrimitiveHolder> byteDefault = new ByteProperty<PrimitiveHolder>("byteDefault");
        final ByteProperty<PrimitiveHolder> byteValue = new ByteProperty<PrimitiveHolder>("byteValue", new Byte((byte) 7));
        final CharProperty<PrimitiveHolder> charDefault = new CharProperty<PrimitiveHolder>("charDefault");
        final CharProperty<PrimitiveHolder> charValue = new CharProperty<PrimitiveHolder>("charValue", new Character('q'));
        final DoubleProperty<PrimitiveHolder> doubleDefault = new DoubleProperty<PrimitiveHolder>("doubleDefault");
        final DoubleProperty<PrimitiveHolder> doubleValue = new DoubleProperty<PrimitiveHolder>("doubleValue", new Double(3.14d));
        final FloatProperty<PrimitiveHolder> floatDefault = new FloatProperty<PrimitiveHolder>("floatDefault");
        final FloatProperty<PrimitiveHolder> floatValue = new FloatProperty<PrimitiveHolder>("floatValue", new Float(1.5f));
        final LongProperty<PrimitiveHolder> longDefault = new LongProperty<PrimitiveHolder>("longDefault");
        final LongProperty<PrimitiveHolder> longValue = new LongProperty<PrimitiveHolder>("longValue", new Long(99L));
        final PropertyIndex index = new PropertyIndex(this, "PrimitiveHolder", boolDefault, boolValue, byteDefault, byteValue,
                charDefault, charValue, doubleDefault, doubleValue, floatDefault, floatValue, longDefault, longValue);
    }

    static class ListHolder extends PropertyBusinessObject {
        final ListProperty<String, ListHolder> names = new ListProperty<String, ListHolder>("names", "alpha", "beta");
        final ListProperty<String, ListHolder> typedNames = new ListProperty<String, ListHolder>("typedNames", String.class, "one", "two");
        final ListProperty<String, ListHolder> emptyNames = new ListProperty<String, ListHolder>("emptyNames");
        final PropertyIndex index = new PropertyIndex(this, "ListHolder", names, typedNames, emptyNames);
    }

    @FormTest
    void primitiveAccessorsReturnPrimitiveValues() {
        PrimitiveHolder holder = new PrimitiveHolder();

        holder.boolDefault.set(Boolean.FALSE);
        assertFalse(holder.boolDefault.getBoolean());
        assertTrue(holder.boolValue.getBoolean());

        holder.byteDefault.set(new Byte((byte) 2));
        assertEquals(2, holder.byteDefault.getByte());
        assertEquals(7, holder.byteValue.getByte());

        holder.charDefault.set(new Character('a'));
        assertEquals('a', holder.charDefault.getChar());
        assertEquals('q', holder.charValue.getChar());

        holder.doubleDefault.set(new Double(10.5d));
        assertEquals(10.5d, holder.doubleDefault.getDouble(), 0.0d);
        assertEquals(3.14d, holder.doubleValue.getDouble(), 0.0d);

        holder.floatDefault.set(new Float(4.5f));
        assertEquals(4.5f, holder.floatDefault.getFloat(), 0.0f);
        assertEquals(1.5f, holder.floatValue.getFloat(), 0.0f);

        holder.longDefault.set(new Long(42L));
        assertEquals(42L, holder.longDefault.getLong());
        assertEquals(99L, holder.longValue.getLong());
    }

    @FormTest
    void numericPropertyRespectsNullableGuardrails() {
        final ByteProperty<PrimitiveHolder> limited = new ByteProperty<PrimitiveHolder>("limited");
        limited.setNullable(false);
        assertFalse(limited.isNullable());
        assertThrows(NullPointerException.class, new Executable() {
            public void execute() {
                limited.set(null);
            }
        });
        limited.setNullable(true);
        assertTrue(limited.isNullable());
        assertNull(limited.set(null));

        NumericProperty<Integer, Object> raw = new NumericProperty<Integer, Object>("raw") {
        };
        assertTrue(raw.isNullable());
        raw.setNullable(false);
        assertFalse(raw.isNullable());
        assertThrows(NullPointerException.class, new Executable() {
            public void execute() {
                raw.set(null);
            }
        });

        NumericProperty<Integer, Object> rawWithValue = new NumericProperty<Integer, Object>("rawWithValue", new Integer(5)) {
        };
        assertFalse(rawWithValue.isNullable());
        rawWithValue.setNullable(true);
        assertNull(rawWithValue.set(new Integer(10)));
        assertEquals(new Integer(10), rawWithValue.get());
    }

    @FormTest
    void listPropertySupportsMutationAndComparison() {
        ListHolder holder = new ListHolder();
        assertEquals("alpha", holder.names.get(0));
        assertEquals(2, holder.names.size());
        assertEquals("one", holder.typedNames.get(0));

        final AtomicInteger changeCount = new AtomicInteger();
        holder.names.addChangeListener(new PropertyChangeListener<String, ListHolder>() {
            public void propertyChanged(PropertyBase<String, ListHolder> source) {
                changeCount.incrementAndGet();
            }
        });

        holder.names.set(1, "beta-updated");
        assertEquals("beta-updated", holder.names.get(1));
        holder.names.set(Arrays.asList("first", "second"));
        holder.names.setList(Arrays.asList("third", "fourth"));
        holder.names.add(0, "zero");
        holder.names.add("fifth");
        holder.names.addAll(Arrays.asList("sixth", "seventh"));
        holder.names.remove(0);
        holder.names.remove("sixth");
        holder.names.removeAll(Arrays.asList("fourth", "fifth"));
        assertTrue(changeCount.get() >= 8);

        ListHolder mirror = new ListHolder();
        mirror.names.setList(holder.names.asList());
        assertEquals(holder.names, mirror.names);
        assertEquals(holder.names.hashCode(), mirror.names.hashCode());

        List<String> snapshot = holder.names.asList();
        assertEquals(snapshot, new ArrayList<String>(holder.names.asList()));
        assertFalse(holder.names.contains("missing"));
        assertTrue(holder.names.contains(snapshot.get(0)));
        assertEquals(0, holder.names.indexOf(snapshot.get(0)));

        Iterator<String> iterator = holder.names.iterator();
        List<String> iterated = new ArrayList<String>();
        while (iterator.hasNext()) {
            iterated.add(iterator.next());
        }
        assertEquals(snapshot, iterated);

        holder.emptyNames.add("valueFromEmpty");
        holder.emptyNames.clear();
        assertEquals(0, holder.emptyNames.size());
    }
}
