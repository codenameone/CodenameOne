package com.codename1.fxcompat;

import org.junit.Rule;
import org.junit.Test;

import com.codename1.compat.testing.HeadlessImplementation;
import com.codename1.compat.testing.MainThreadRule;
import com.codename1.fxcompat.runtime.Units;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

public class ZzLabelProbeTest {
    @Rule
    public final MainThreadRule mainThread = new MainThreadRule();

    @Test
    public void probe() {
        HeadlessImplementation.install();
        Units.setScale(2);
        Label icon = new Label();
        Label title = new Label();
        title.setGraphic(icon);
        title.setText("JavaFX");
        title.setMinWidth(0);
        VBox cell = new VBox(title);
        cell.setPrefWidth(0);
        cell.setMinWidth(0);
        HBox.setHgrow(cell, Priority.ALWAYS);
        HBox row = new HBox(cell, new VBox(), new VBox());
        Scene scene = new Scene(row, 300, 200);
        scene.cn1Layout(300, 200);
        icon.setText("☕☕☕");
        icon.setStyle("-fx-font-size: 30px;");
        scene.cn1Layout(300, 200);
        scene.cn1Layout(300, 200);
        com.codename1.ui.Component c = title.cn1Native();
        System.out.println("PROBE icon x=" + icon.getLayoutX() + " w=" + icon.getWidth() + " pref=" + icon.prefWidth(-1)
                + " title w=" + title.getWidth() + " pad=" + (c == null ? -1 : c.getStyle().getPaddingLeftNoRTL()));
    }
}
