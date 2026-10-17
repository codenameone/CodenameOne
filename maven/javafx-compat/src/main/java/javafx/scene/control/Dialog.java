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

import java.util.Optional;

import com.codename1.fxcompat.runtime.EventHandlerManager;

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.ReadOnlyBooleanProperty;
import javafx.beans.property.ReadOnlyDoubleProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.StringProperty;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.event.Event;
import javafx.event.EventDispatchChain;
import javafx.event.EventHandler;
import javafx.event.EventTarget;
import javafx.event.EventType;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.ButtonBar.ButtonData;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import javafx.stage.Window;
import javafx.stage.WindowEvent;
import javafx.util.Callback;

/// A window that asks the user something and answers with a result.
///
/// A dialog shows a [DialogPane] in a stage of its own, modal to the
/// application unless [#initModality(Modality)] says otherwise.
/// [#showAndWait()] shows it and returns the result once it closed;
/// [#show()] returns at once, and the result is read from
/// [#resultProperty()] later.
///
/// Invoking a button of the pane hands its [ButtonType] to the result
/// converter, makes what that returns the result, and closes the dialog.
/// Without a converter the button type itself is the result. Setting the
/// result from code closes the dialog too.
///
/// [#close()] without a result closes the dialog only if the pane has a
/// single button or a cancel button, as in JavaFX; the result is then
/// what the converter makes of the cancel button, or of `null` when the
/// single button is not one. The same holds for the user closing the
/// window, which a handler of `DialogEvent.DIALOG_CLOSE_REQUEST` can
/// refuse by consuming the event.
///
/// `showAndWait()` returns a `java.util.Optional`, which the device class
/// library provides and the older CLDC profile of Codename One does not.
public class Dialog<R> implements EventTarget {

    private final Stage stage = new Stage();
    private final Scene scene;
    private final ObjectProperty<DialogPane> dialogPane = new SimpleObjectProperty<DialogPane>(this, "dialogPane");
    private final ObjectProperty<R> result = new SimpleObjectProperty<R>(this, "result");
    private final ObjectProperty<Callback<ButtonType, R>> resultConverter =
            new SimpleObjectProperty<Callback<ButtonType, R>>(this, "resultConverter");
    private final EventHandlerManager events = new EventHandlerManager(this);
    private boolean closing;

    /// Creates a dialog with an empty pane.
    public Dialog() {
        stage.initModality(Modality.APPLICATION_MODAL);
        DialogPane pane = new DialogPane();
        pane.attach(this);
        dialogPane.set(pane);
        scene = new Scene(pane);
        stage.setScene(scene);
        dialogPane.addListener(new ChangeListener<DialogPane>() {
            @Override
            public void changed(ObservableValue<? extends DialogPane> observable, DialogPane oldValue,
                    DialogPane newValue) {
                if (oldValue != null) {
                    oldValue.attach(null);
                }
                DialogPane next = newValue;
                if (next == null) {
                    next = new DialogPane();
                    dialogPane.set(next);
                    return;
                }
                next.attach(Dialog.this);
                scene.setRoot(next);
            }
        });
        result.addListener(new ChangeListener<R>() {
            @Override
            public void changed(ObservableValue<? extends R> observable, R oldValue, R newValue) {
                close();
            }
        });
        relay(WindowEvent.WINDOW_SHOWING, DialogEvent.DIALOG_SHOWING);
        relay(WindowEvent.WINDOW_SHOWN, DialogEvent.DIALOG_SHOWN);
        relay(WindowEvent.WINDOW_HIDING, DialogEvent.DIALOG_HIDING);
        relay(WindowEvent.WINDOW_HIDDEN, DialogEvent.DIALOG_HIDDEN);
        stage.addEventHandler(WindowEvent.WINDOW_CLOSE_REQUEST, new EventHandler<WindowEvent>() {
            @Override
            public void handle(WindowEvent event) {
                DialogEvent request = new DialogEvent(Dialog.this, DialogEvent.DIALOG_CLOSE_REQUEST);
                Event.fireEvent(Dialog.this, request);
                if (!request.isConsumed()) {
                    close();
                }
                if (stage.isShowing()) {
                    // Refused: the window stays.
                    event.consume();
                }
            }
        });
    }

    private void relay(EventType<WindowEvent> from, final EventType<DialogEvent> to) {
        stage.addEventHandler(from, new EventHandler<WindowEvent>() {
            @Override
            public void handle(WindowEvent event) {
                Event.fireEvent(Dialog.this, new DialogEvent(Dialog.this, to));
            }
        });
    }

    /// Shows the dialog and returns at once.
    public final void show() {
        stage.show();
    }

    /// Shows the dialog and returns its result once it closed: empty when
    /// it closed without one. Events keep being handled meanwhile.
    public final Optional<R> showAndWait() {
        stage.showAndWait();
        return Optional.ofNullable(getResult());
    }

    /// Closes the dialog, if it has a result or may close without one.
    public final void close() {
        if (closing) {
            return;
        }
        closing = true;
        try {
            if (getResult() == null) {
                DialogPane pane = getDialogPane();
                ButtonType cancel = null;
                boolean allowed = pane.getButtonTypes().size() == 1;
                for (int i = 0; i < pane.getButtonTypes().size(); i++) {
                    ButtonType type = pane.getButtonTypes().get(i);
                    ButtonData data = type == null ? null : type.getButtonData();
                    if (data == null) {
                        continue;
                    }
                    if (data == ButtonData.CANCEL_CLOSE) {
                        cancel = type;
                        break;
                    }
                    if (data.isCancelButton()) {
                        cancel = type;
                    }
                }
                if (cancel == null && !allowed) {
                    return;
                }
                answer(cancel, false);
            }
            stage.hide();
        } finally {
            closing = false;
        }
    }

    /// Closes the dialog; the same as [#close()].
    public final void hide() {
        close();
    }

    /// Makes the result of a chosen button and, when asked to and the
    /// result did not change, closes the dialog; a changed result closes
    /// it by itself.
    @SuppressWarnings("unchecked")
    final void answer(ButtonType chosen, boolean close) {
        Callback<ButtonType, R> converter = getResultConverter();
        R prior = getResult();
        // Without a converter the dialog answers with the button type, as
        // a dialog declared for that type expects.
        R next = converter == null ? (R) chosen : converter.call(chosen);
        setResult(next);
        if (close && prior == next) {
            close();
        }
    }

    /// Sets how the dialog blocks other windows; before it is shown.
    public final void initModality(Modality modality) {
        stage.initModality(modality);
    }

    /// Returns how the dialog blocks other windows.
    public final Modality getModality() {
        return stage.getModality();
    }

    /// Sets the style of the window of the dialog; before it is shown.
    public final void initStyle(StageStyle style) {
        stage.initStyle(style);
    }

    /// Sets the window the dialog belongs to; before it is shown.
    public final void initOwner(Window window) {
        stage.initOwner(window);
    }

    /// Returns the window the dialog belongs to.
    public final Window getOwner() {
        return stage.getOwner();
    }

    /// The pane the dialog shows. Setting `null` puts an empty pane.
    public final ObjectProperty<DialogPane> dialogPaneProperty() {
        return dialogPane;
    }

    /// Returns the pane the dialog shows.
    public final DialogPane getDialogPane() {
        return dialogPane.get();
    }

    /// Sets the pane the dialog shows.
    public final void setDialogPane(DialogPane value) {
        dialogPane.set(value);
    }

    /// The content text of the pane.
    public final StringProperty contentTextProperty() {
        return getDialogPane().contentTextProperty();
    }

    /// Returns the content text of the pane.
    public final String getContentText() {
        return getDialogPane().getContentText();
    }

    /// Sets the content text of the pane.
    public final void setContentText(String contentText) {
        getDialogPane().setContentText(contentText);
    }

    /// The header text of the pane.
    public final StringProperty headerTextProperty() {
        return getDialogPane().headerTextProperty();
    }

    /// Returns the header text of the pane.
    public final String getHeaderText() {
        return getDialogPane().getHeaderText();
    }

    /// Sets the header text of the pane.
    public final void setHeaderText(String headerText) {
        getDialogPane().setHeaderText(headerText);
    }

    /// The graphic of the pane; recorded.
    public final ObjectProperty<Node> graphicProperty() {
        return getDialogPane().graphicProperty();
    }

    /// Returns the graphic of the pane.
    public final Node getGraphic() {
        return getDialogPane().getGraphic();
    }

    /// Sets the graphic of the pane; recorded.
    public final void setGraphic(Node graphic) {
        getDialogPane().setGraphic(graphic);
    }

    /// The result. Setting it closes the dialog.
    public final ObjectProperty<R> resultProperty() {
        return result;
    }

    /// Returns the result, `null` while there is none.
    public final R getResult() {
        return result.get();
    }

    /// Sets the result, which closes the dialog.
    public final void setResult(R value) {
        result.set(value);
    }

    /// Makes the result out of the button the user chose.
    public final ObjectProperty<Callback<ButtonType, R>> resultConverterProperty() {
        return resultConverter;
    }

    /// Returns what makes the result out of the button the user chose.
    public final Callback<ButtonType, R> getResultConverter() {
        return resultConverter.get();
    }

    /// Sets what makes the result out of the button the user chose.
    public final void setResultConverter(Callback<ButtonType, R> value) {
        resultConverter.set(value);
    }

    /// Whether the dialog is on screen.
    public final ReadOnlyBooleanProperty showingProperty() {
        return stage.showingProperty();
    }

    /// Returns whether the dialog is on screen.
    public final boolean isShowing() {
        return stage.isShowing();
    }

    /// Whether the user may resize the window of the dialog.
    public final BooleanProperty resizableProperty() {
        return stage.resizableProperty();
    }

    /// Returns whether the user may resize the window of the dialog.
    public final boolean isResizable() {
        return stage.isResizable();
    }

    /// Sets whether the user may resize the window of the dialog.
    public final void setResizable(boolean resizable) {
        stage.setResizable(resizable);
    }

    /// The title of the window of the dialog.
    public final StringProperty titleProperty() {
        return stage.titleProperty();
    }

    /// Returns the title of the window of the dialog.
    public final String getTitle() {
        return stage.getTitle();
    }

    /// Sets the title of the window of the dialog.
    public final void setTitle(String title) {
        stage.setTitle(title);
    }

    /// The width of the window of the dialog.
    public final ReadOnlyDoubleProperty widthProperty() {
        return stage.widthProperty();
    }

    /// Returns the width of the window of the dialog.
    public final double getWidth() {
        return stage.getWidth();
    }

    /// Sets the width of the window of the dialog.
    public final void setWidth(double width) {
        stage.setWidth(width);
    }

    /// The height of the window of the dialog.
    public final ReadOnlyDoubleProperty heightProperty() {
        return stage.heightProperty();
    }

    /// Returns the height of the window of the dialog.
    public final double getHeight() {
        return stage.getHeight();
    }

    /// Sets the height of the window of the dialog.
    public final void setHeight(double height) {
        stage.setHeight(height);
    }

    /// The x of the window of the dialog.
    public final ReadOnlyDoubleProperty xProperty() {
        return stage.xProperty();
    }

    /// Returns the x of the window of the dialog.
    public final double getX() {
        return stage.getX();
    }

    /// Sets the x of the window of the dialog.
    public final void setX(double x) {
        stage.setX(x);
    }

    /// The y of the window of the dialog.
    public final ReadOnlyDoubleProperty yProperty() {
        return stage.yProperty();
    }

    /// Returns the y of the window of the dialog.
    public final double getY() {
        return stage.getY();
    }

    /// Sets the y of the window of the dialog.
    public final void setY(double y) {
        stage.setY(y);
    }

    @Override
    public EventDispatchChain buildEventDispatchChain(EventDispatchChain tail) {
        return tail.prepend(events);
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private ObjectProperty<EventHandler<DialogEvent>> slot(EventType<DialogEvent> type, String name) {
        // The slot is typed for any handler of a super type of the event;
        // these properties only ever hold handlers of the event itself.
        return (ObjectProperty) events.slot(type, name);
    }

    /// The handler called before the dialog shows.
    public final ObjectProperty<EventHandler<DialogEvent>> onShowingProperty() {
        return slot(DialogEvent.DIALOG_SHOWING, "onShowing");
    }

    /// Sets the handler called before the dialog shows.
    public final void setOnShowing(EventHandler<DialogEvent> value) {
        onShowingProperty().set(value);
    }

    /// Returns the handler called before the dialog shows.
    public final EventHandler<DialogEvent> getOnShowing() {
        return onShowingProperty().get();
    }

    /// The handler called after the dialog showed.
    public final ObjectProperty<EventHandler<DialogEvent>> onShownProperty() {
        return slot(DialogEvent.DIALOG_SHOWN, "onShown");
    }

    /// Sets the handler called after the dialog showed.
    public final void setOnShown(EventHandler<DialogEvent> value) {
        onShownProperty().set(value);
    }

    /// Returns the handler called after the dialog showed.
    public final EventHandler<DialogEvent> getOnShown() {
        return onShownProperty().get();
    }

    /// The handler called before the dialog hides.
    public final ObjectProperty<EventHandler<DialogEvent>> onHidingProperty() {
        return slot(DialogEvent.DIALOG_HIDING, "onHiding");
    }

    /// Sets the handler called before the dialog hides.
    public final void setOnHiding(EventHandler<DialogEvent> value) {
        onHidingProperty().set(value);
    }

    /// Returns the handler called before the dialog hides.
    public final EventHandler<DialogEvent> getOnHiding() {
        return onHidingProperty().get();
    }

    /// The handler called after the dialog hid.
    public final ObjectProperty<EventHandler<DialogEvent>> onHiddenProperty() {
        return slot(DialogEvent.DIALOG_HIDDEN, "onHidden");
    }

    /// Sets the handler called after the dialog hid.
    public final void setOnHidden(EventHandler<DialogEvent> value) {
        onHiddenProperty().set(value);
    }

    /// Returns the handler called after the dialog hid.
    public final EventHandler<DialogEvent> getOnHidden() {
        return onHiddenProperty().get();
    }

    /// The handler called when the user asks the window to close.
    public final ObjectProperty<EventHandler<DialogEvent>> onCloseRequestProperty() {
        return slot(DialogEvent.DIALOG_CLOSE_REQUEST, "onCloseRequest");
    }

    /// Sets the handler called when the user asks the window to close.
    public final void setOnCloseRequest(EventHandler<DialogEvent> value) {
        onCloseRequestProperty().set(value);
    }

    /// Returns the handler called when the user asks the window to close.
    public final EventHandler<DialogEvent> getOnCloseRequest() {
        return onCloseRequestProperty().get();
    }
}
