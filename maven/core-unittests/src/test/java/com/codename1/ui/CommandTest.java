package com.codename1.ui;

import com.codename1.test.UITestBase;
import com.codename1.ui.events.ActionEvent;
import com.codename1.ui.events.ActionListener;
import org.junit.jupiter.api.Test;

import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;

class CommandTest extends UITestBase {

    @Test
    void testConstructorsAndAccessors() {
        Image icon = mock(Image.class);
        Command command = new Command("Save", icon, 7);

        assertEquals(7, command.getId());
        assertEquals("Save", command.getCommandName());
        assertSame(icon, command.getIcon());
        assertEquals("Save", command.toString());

        Image pressed = mock(Image.class);
        Image rollover = mock(Image.class);
        Image disabled = mock(Image.class);
        command.setPressedIcon(pressed);
        command.setRolloverIcon(rollover);
        command.setDisabledIcon(disabled);
        command.setMaterialIcon('A');
        command.setMaterialIconSize(18f);
        command.setIconGapMM(2.5f);
        command.setIconFont(Font.getDefaultFont());

        assertSame(pressed, command.getPressedIcon());
        assertSame(rollover, command.getRolloverIcon());
        assertSame(disabled, command.getDisabledIcon());
        assertEquals('A', command.getMaterialIcon());
        assertEquals(18f, command.getMaterialIconSize(), 0.01f);
        assertEquals(2.5f, command.getIconGapMM(), 0.01f);
        assertNotNull(command.getIconFont());
    }

    @Test
    void testCreateConvenienceMethodsInvokeProvidedListener() {
        AtomicInteger invocationCount = new AtomicInteger();
        ActionListener listener = new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent evt) {
                invocationCount.incrementAndGet();
            }
        };
        Image icon = mock(Image.class);

        Command created = Command.create("Share", icon, listener);
        created.actionPerformed(null);
        assertEquals(1, invocationCount.get());
        assertSame(icon, created.getIcon());

        Command material = Command.createMaterial("Refresh", '\uE5D5', listener);
        material.actionPerformed(null);
        assertEquals(2, invocationCount.get());
        assertEquals('\uE5D5', material.getMaterialIcon());
    }

    @Test
    void testClientPropertiesCreateAndRemoveEntries() {
        Command command = new Command("Edit");
        assertNull(command.getClientProperty("missing"));

        command.putClientProperty("key", "value");
        assertEquals("value", command.getClientProperty("key"));

        command.putClientProperty("key", null);
        assertNull(command.getClientProperty("key"));
    }

    @Test
    void testEqualsAndHashCodeDependOnCoreFields() {
        Image icon = mock(Image.class);
        Command first = new Command("Open", icon, 3);
        first.setMaterialIcon('B');
        first.setMaterialIconSize(11f);
        first.putClientProperty("mode", "full");

        Command second = new Command("Open", icon, 3);
        second.setMaterialIcon('B');
        second.setMaterialIconSize(11f);
        second.putClientProperty("mode", "full");

        assertEquals(first, second);
        assertEquals(second, first);
        assertEquals(first.hashCode(), second.hashCode());

        second.setCommandName("Other");
        assertNotEquals(first, second);

        Command nullName = new Command(null, icon, 4);
        nullName.setMaterialIcon('C');
        nullName.setMaterialIconSize(6f);
        nullName.putClientProperty("mode", "full");

        Command nullNameMatch = new Command(null, icon, 4);
        nullNameMatch.setMaterialIcon('C');
        nullNameMatch.setMaterialIconSize(6f);
        nullNameMatch.putClientProperty("mode", "full");

        assertEquals(nullName, nullNameMatch);
    }
}
