package com.codename1.impl.javase;

import java.util.List;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TestRecorderScriptModelTest {

    @Test
    void shouldAllowDisablingAndEnablingBlocks() {
        TestRecorderScriptModel model = new TestRecorderScriptModel();
        model.appendToActiveBlock("line1\n");
        model.createBlock("Second");
        model.appendToActiveBlock("line2\n");

        assertEquals("line1\nline2\n", model.getEnabledCode());

        model.setBlockEnabled(1, false);
        assertEquals("line1\n", model.getEnabledCode());

        model.setBlockEnabled(1, true);
        assertEquals("line1\nline2\n", model.getEnabledCode());
    }

    @Test
    void shouldAllowEditingBlockCode() {
        TestRecorderScriptModel model = new TestRecorderScriptModel();
        model.appendToActiveBlock("old\n");
        model.setBlockCode(0, "updated\n");

        assertEquals("updated\n", model.getEnabledCode());
    }

    @Test
    void shouldExposeBlockMetadataForUiFacelift() {
        TestRecorderScriptModel model = new TestRecorderScriptModel();
        model.createBlock("Assertions");
        List<TestRecorderScriptModel.ScriptBlock> blocks = model.getBlocks();

        assertEquals(2, blocks.size());
        assertEquals("Recorded Actions", blocks.get(0).getName());
        assertEquals("Assertions", blocks.get(1).getName());
        assertTrue(blocks.get(0).isEnabled());
        model.setBlockEnabled(0, false);
        assertFalse(model.getBlocks().get(0).isEnabled());
    }

    @Test
    void shouldSupportSwitchingActiveBlock() {
        TestRecorderScriptModel model = new TestRecorderScriptModel();
        model.appendToActiveBlock("first\n");
        model.createBlock("Second");
        model.appendToActiveBlock("second\n");

        model.setActiveBlock(0);
        model.appendToActiveBlock("again\n");

        assertEquals(0, model.getActiveBlockIndex());
        assertEquals("first\nagain\n", model.getBlocks().get(0).getCode());
        assertEquals("second\n", model.getBlocks().get(1).getCode());
    }
}
