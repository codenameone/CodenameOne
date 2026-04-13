/*
 * Copyright (c) 2012, Codename One and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.  Codename One designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Oracle in the LICENSE file that accompanied this code.
 */
package com.codename1.impl.javase;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Mutable model for test recorder script blocks.
 */
class TestRecorderScriptModel {
    static class ScriptBlock {
        private final String name;
        private String code = "";
        private boolean enabled = true;

        ScriptBlock(String name) {
            this.name = name;
        }

        String getName() {
            return name;
        }

        String getCode() {
            return code;
        }

        void setCode(String code) {
            this.code = code == null ? "" : code;
        }

        boolean isEnabled() {
            return enabled;
        }

        void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }
    }

    private final List<ScriptBlock> blocks = new ArrayList<ScriptBlock>();
    private ScriptBlock activeBlock;

    TestRecorderScriptModel() {
        activeBlock = createBlock("Recorded Actions");
    }

    ScriptBlock createBlock(String name) {
        ScriptBlock out = new ScriptBlock(name == null ? "Block " + (blocks.size() + 1) : name);
        blocks.add(out);
        activeBlock = out;
        return out;
    }

    int getActiveBlockIndex() {
        return blocks.indexOf(activeBlock);
    }

    ScriptBlock getActiveBlock() {
        return activeBlock;
    }

    void setActiveBlock(int index) {
        activeBlock = blocks.get(index);
    }

    void appendToActiveBlock(String snippet) {
        if (snippet == null || snippet.length() == 0) {
            return;
        }
        activeBlock.setCode(activeBlock.getCode() + snippet);
    }

    List<ScriptBlock> getBlocks() {
        return Collections.unmodifiableList(blocks);
    }

    void setBlockEnabled(int index, boolean enabled) {
        blocks.get(index).setEnabled(enabled);
    }

    void setBlockCode(int index, String code) {
        blocks.get(index).setCode(code);
    }

    String getEnabledCode() {
        StringBuilder out = new StringBuilder();
        for (ScriptBlock block : blocks) {
            if (block.isEnabled()) {
                out.append(block.getCode());
            }
        }
        return out.toString();
    }
}
