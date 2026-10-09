/*
 * Copyright (c) 2026, Codename One and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.  Codename One designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Oracle in the LICENSE file that accompanied this code.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 2 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 *
 * You should have received a copy of the GNU General Public License version
 * 2 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Please contact Codename One through http://www.codenameone.com/ if you
 * need additional information or have any questions.
 */
package javafx.scene.control;

import com.codename1.ui.events.ActionListener;

import javafx.beans.property.ObjectProperty;
import javafx.beans.property.ReadOnlyBooleanProperty;
import javafx.beans.property.ReadOnlyBooleanWrapper;
import javafx.css.PseudoClass;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.scene.Node;

/// The base of the controls that are pressed: buttons, check boxes,
/// toggles, hyperlinks.
///
/// The native component's own action, however the user triggered it,
/// calls [#fire()], which a subclass overrides to change its state and
/// then fire an `ActionEvent`.
public abstract class ButtonBase extends Labeled {

    private static final PseudoClass ARMED = PseudoClass.getPseudoClass("armed");

    private final ReadOnlyBooleanWrapper armed = new ReadOnlyBooleanWrapper(this, "armed", false);

    /// Creates a button with no text.
    public ButtonBase() {
        setMnemonicParsing(true);
    }

    /// Creates a button with text.
    public ButtonBase(String text) {
        super(text);
        setMnemonicParsing(true);
    }

    /// Creates a button with text and a graphic.
    public ButtonBase(String text, Node graphic) {
        super(text, graphic);
        setMnemonicParsing(true);
    }

    /// Returns the listener a subclass adds to its native button: it
    /// calls [#fire()] unless the control is disabled.
    protected final ActionListener<com.codename1.ui.events.ActionEvent> cn1ActionBridge() {
        return new ActionListener<com.codename1.ui.events.ActionEvent>() {
            @Override
            public void actionPerformed(com.codename1.ui.events.ActionEvent evt) {
                if (!isDisabled()) {
                    fire();
                }
            }
        };
    }

    /// Returns whether a release now would fire the button.
    public final boolean isArmed() {
        return armed.get();
    }

    /// Whether a release now would fire the button.
    public final ReadOnlyBooleanProperty armedProperty() {
        return armed.getReadOnlyProperty();
    }

    /// Arms the button.
    public void arm() {
        if (!armed.get()) {
            armed.set(true);
            pseudoClassStateChanged(ARMED, true);
        }
    }

    /// Disarms the button.
    public void disarm() {
        if (armed.get()) {
            armed.set(false);
            pseudoClassStateChanged(ARMED, false);
        }
    }

    /// Invokes the button as a press and release would.
    public abstract void fire();

    /// Sets the handler of the button being invoked.
    public final void setOnAction(EventHandler<ActionEvent> value) {
        cn1Events().setSlot(ActionEvent.ACTION, "onAction", value);
    }

    /// Returns the handler of the button being invoked.
    public final EventHandler<? super ActionEvent> getOnAction() {
        return cn1Events().getSlot(ActionEvent.ACTION);
    }

    /// The handler of the button being invoked.
    public final ObjectProperty<EventHandler<? super ActionEvent>> onActionProperty() {
        return cn1Events().slot(ActionEvent.ACTION, "onAction");
    }
}
