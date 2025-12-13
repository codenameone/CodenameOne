package com.codename1.properties;

import com.codename1.junit.FormTest;
import com.codename1.junit.UITestBase;

import static org.junit.jupiter.api.Assertions.*;

class PrimitivePropertyBasicsTest extends UITestBase {

    static class PrimitiveHolder implements PropertyBusinessObject {
        final BooleanProperty<PrimitiveHolder> active = new BooleanProperty<PrimitiveHolder>("active");
        final ByteProperty<PrimitiveHolder> code = new ByteProperty<PrimitiveHolder>("code");
        final CharProperty<PrimitiveHolder> initial = new CharProperty<PrimitiveHolder>("initial");
        final DoubleProperty<PrimitiveHolder> ratio = new DoubleProperty<PrimitiveHolder>("ratio");
        final FloatProperty<PrimitiveHolder> percentage = new FloatProperty<PrimitiveHolder>("percentage");
        final LongProperty<PrimitiveHolder> count = new LongProperty<PrimitiveHolder>("count");
        final ListProperty<String, PrimitiveHolder> names = new ListProperty<String, PrimitiveHolder>("names", String.class);
        final SetProperty<String, PrimitiveHolder> tags = new SetProperty<String, PrimitiveHolder>("tags", String.class);
        final PropertyIndex index = new PropertyIndex(this, "PrimitiveHolder", active, code, initial, ratio, percentage, count, names, tags);

        public PropertyIndex getPropertyIndex() {
            return index;
        }
    }

    @FormTest
    void primitiveAccessorsReturnTypedValues() {
        PrimitiveHolder holder = new PrimitiveHolder();
        holder.active.set(Boolean.TRUE);
        holder.code.set(Byte.valueOf((byte) 5));
        holder.initial.set(Character.valueOf('Z'));
        holder.ratio.set(Double.valueOf(1.5d));
        holder.percentage.set(Float.valueOf(2.5f));
        holder.count.set(Long.valueOf(9L));

        assertTrue(holder.active.getBoolean());
        assertEquals(5, holder.code.getByte());
        assertEquals('Z', holder.initial.getChar());
        assertEquals(1.5d, holder.ratio.getDouble(), 0.0d);
        assertEquals(2.5f, holder.percentage.getFloat(), 0.0f);
        assertEquals(9L, holder.count.getLong());
    }

    @FormTest
    void collectionPropertiesSupportMutation() {
        PrimitiveHolder holder = new PrimitiveHolder();

        holder.names.add("one");
        holder.names.add("two");
        assertEquals(2, holder.names.size());
        assertEquals("one", holder.names.get(0));

        holder.names.set(0, "uno");
        assertEquals("uno", holder.names.get(0));

        holder.tags.add("alpha");
        holder.tags.add(holder.names.get(0));
        holder.tags.add(holder.names.get(1));
        assertTrue(holder.tags.contains("alpha"));
        assertEquals(3, holder.tags.size());

        holder.tags.remove("alpha");
        assertFalse(holder.tags.contains("alpha"));
        holder.tags.clear();
        assertEquals(0, holder.tags.size());
    }
}
