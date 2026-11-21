/*
 * MIT License
 *
 * Copyright (c) 2020 Rive
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 *
 * Java rewrite of rive-runtime for Codename One.
 * Codename One modifications Copyright (c) 2026 Codename One.
 */
package com.codename1.impl.rive;

/// Port of `NestedInput`: drives input `inputId` of the parent
/// [NestedStateMachine]'s machine.
public abstract class NestedInput extends NestedInputBase {
    @Override
    public int onAddedDirty(Artboard context) {
        int code = super.onAddedDirty(context);
        if (code != STATUS_OK) {
            return code;
        }
        if (parent instanceof NestedStateMachine) {
            ((NestedStateMachine) parent).addNestedInput(this);
        }
        return STATUS_OK;
    }

    /// The machine, or null before the nested artboard is instanced.
    protected StateMachineInstance machine() {
        return parent instanceof NestedStateMachine ? ((NestedStateMachine) parent).getStateMachineInstance() : null;
    }

    /// Pushes the stored value into the machine.
    public abstract void applyValue();

    protected void poke(float value) {
        StateMachineInstance m = machine();
        if (m != null && mInputId >= 0 && mInputId < m.getInputCount()) {
            m.pokeInput(mInputId, value);
        }
    }
}
