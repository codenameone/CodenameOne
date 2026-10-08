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
package com.codename1.fxml;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.ListResourceBundle;
import java.util.ResourceBundle;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import com.codename1.compat.testing.HeadlessImplementation;
import com.codename1.compat.testing.MainThreadRule;
import com.codename1.fxcompat.runtime.StyleEngine;
import com.codename1.fxcompat.runtime.Units;

import javafx.collections.ObservableList;
import javafx.fxml.FXMLLoader;
import javafx.fxml.LoadException;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.RadioButton;
import javafx.scene.control.TextField;
import javafx.scene.control.ToggleGroup;
import javafx.scene.image.ImageView;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import javafx.util.Callback;

/// Every FXML feature the compiler supports, through the whole of what a
/// build does: document to Java source, javac, the generated dispatcher,
/// and the tree the document builds on a headless device.
public class FxmlCompilerTest {

    private static final String HEAD = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n"
            + "<?import javafx.scene.control.*?>\n"
            + "<?import javafx.scene.layout.*?>\n"
            + "<?import javafx.geometry.Insets?>\n";
    private static final String NS = " xmlns=\"http://javafx.com/javafx/17\" xmlns:fx=\"http://javafx.com/fxml/1\"";

    @Rule
    public final MainThreadRule mainThread = new MainThreadRule();

    private FxmlHarness app;

    @Before
    public void setUp() {
        HeadlessImplementation.install();
        Units.setScale(2);
    }

    @After
    public void tearDown() throws IOException {
        if (app != null) {
            app.close();
        }
        Units.setScale(0);
        StyleEngine.setInstance(null);
    }

    private FxmlHarness app(String name) throws IOException {
        app = new FxmlHarness(name);
        return app;
    }

    /// The single error of a document that must not compile.
    private String errorOf(String name, String document) throws IOException {
        List<String> errors = app(name).resource("bad.fxml", document).compile();
        assertEquals(errors.toString(), 1, errors.size());
        return errors.get(0);
    }

    // ------------------------------------------------- the object graph

    @Test
    public void attributesAreCoercedToThePropertyType() throws Exception {
        app("coerce").resource("main.fxml", HEAD + "<?import javafx.scene.shape.Rectangle?>\n"
                + "<VBox" + NS + " spacing=\"8\" alignment=\"CENTER_RIGHT\" fillWidth=\"false\" id=\"box\""
                + " styleClass=\"panel, wide\" prefWidth=\"120.5\" maxHeight=\"Infinity\" padding=\"1 2 3 4\">\n"
                + "  <Label text=\"Hello\" wrapText=\"true\" textFill=\"#ff0000\" opacity=\".5\"/>\n"
                + "  <Rectangle width=\"10\" height=\"20\" fill=\"BLUE\" arcWidth=\"4\"/>\n"
                + "  <Label text=\"\\%not a key\"/>\n"
                + "</VBox>\n").build();
        VBox box = app.load("main.fxml");
        assertEquals(8, box.getSpacing(), 0);
        assertEquals(Pos.CENTER_RIGHT, box.getAlignment());
        assertFalse(box.isFillWidth());
        assertEquals("box", box.getId());
        assertTrue(box.getStyleClass().contains("panel"));
        assertTrue(box.getStyleClass().contains("wide"));
        assertEquals(120.5, box.getPrefWidth(), 0);
        assertEquals(Double.POSITIVE_INFINITY, box.getMaxHeight(), 0);
        assertEquals(new Insets(1, 2, 3, 4), box.getPadding());
        assertEquals(3, box.getChildren().size());
        Label label = (Label) box.getChildren().get(0);
        assertEquals("Hello", label.getText());
        assertTrue(label.isWrapText());
        assertEquals(Color.RED, label.getTextFill());
        assertEquals(.5, label.getOpacity(), 0);
        Rectangle r = (Rectangle) box.getChildren().get(1);
        assertEquals(10, r.getWidth(), 0);
        assertEquals(20, r.getHeight(), 0);
        assertEquals(Color.BLUE, r.getFill());
        assertEquals("%not a key", ((Label) box.getChildren().get(2)).getText());
    }

    @Test
    public void propertyElementsListsAndTextContent() throws Exception {
        app("elements").resource("main.fxml", HEAD + "<?import java.lang.String?>\n"
                + "<BorderPane" + NS + ">\n"
                + "  <padding><Insets top=\"1\" right=\"2\" bottom=\"3\" left=\"4\"/></padding>\n"
                + "  <top><Label><text>Title</text></Label></top>\n"
                + "  <center>\n"
                + "    <HBox>\n"
                + "      <children>\n"
                + "        <Button text=\"One\"/>\n"
                + "        <Button>Two</Button>\n"
                + "      </children>\n"
                + "    </HBox>\n"
                + "  </center>\n"
                + "  <bottom>\n"
                + "    <ComboBox>\n"
                + "      <items><String fx:value=\"a\"/><String fx:value=\"b\"/></items>\n"
                + "    </ComboBox>\n"
                + "  </bottom>\n"
                + "  <styleClass><String fx:value=\"frame\"/></styleClass>\n"
                + "</BorderPane>\n").build();
        BorderPane pane = app.load("main.fxml");
        assertEquals(new Insets(1, 2, 3, 4), pane.getPadding());
        assertEquals("Title", ((Label) pane.getTop()).getText());
        HBox center = (HBox) pane.getCenter();
        assertEquals(2, center.getChildren().size());
        assertEquals("One", ((Button) center.getChildren().get(0)).getText());
        assertEquals("Two", ((Button) center.getChildren().get(1)).getText());
        ComboBox<?> combo = (ComboBox<?>) pane.getBottom();
        assertEquals(2, combo.getItems().size());
        assertEquals("b", combo.getItems().get(1));
        assertTrue(pane.getStyleClass().contains("frame"));
    }

    @Test
    public void staticPropertiesAsAttributesAndElements() throws Exception {
        app("static").resource("main.fxml", HEAD
                + "<VBox" + NS + ">\n"
                + "  <GridPane VBox.vgrow=\"ALWAYS\">\n"
                + "    <Label text=\"a\" GridPane.rowIndex=\"2\" GridPane.columnIndex=\"1\"/>\n"
                + "    <Label text=\"b\">\n"
                + "      <GridPane.rowIndex>3</GridPane.rowIndex>\n"
                + "      <VBox.margin><Insets topRightBottomLeft=\"6\"/></VBox.margin>\n"
                + "    </Label>\n"
                + "  </GridPane>\n"
                + "</VBox>\n").build();
        VBox box = app.load("main.fxml");
        GridPane grid = (GridPane) box.getChildren().get(0);
        assertEquals(Priority.ALWAYS, VBox.getVgrow(grid));
        Node a = grid.getChildren().get(0);
        assertEquals(Integer.valueOf(2), GridPane.getRowIndex(a));
        assertEquals(Integer.valueOf(1), GridPane.getColumnIndex(a));
        Node b = grid.getChildren().get(1);
        assertEquals(Integer.valueOf(3), GridPane.getRowIndex(b));
        assertEquals(new Insets(6), VBox.getMargin(b));
    }

    @Test
    public void valueFactoryAndConstant() throws Exception {
        app("factory").resource("main.fxml", HEAD + "<?import javafx.collections.FXCollections?>\n"
                + "<?import javafx.scene.paint.Color?>\n<?import java.lang.Double?>\n<?import java.lang.String?>\n"
                + "<VBox" + NS + ">\n"
                + "  <fx:define>\n"
                + "    <Double fx:id=\"gap\" fx:value=\"12.5\"/>\n"
                + "    <FXCollections fx:id=\"names\" fx:factory=\"observableArrayList\">\n"
                + "      <String fx:value=\"x\"/><String fx:value=\"y\"/>\n"
                + "    </FXCollections>\n"
                + "  </fx:define>\n"
                + "  <Label fx:id=\"tinted\" text=\"t\">\n"
                + "    <textFill><Color fx:constant=\"GREEN\"/></textFill>\n"
                + "  </Label>\n"
                + "  <ComboBox fx:id=\"combo\" items=\"$names\"/>\n"
                + "  <HBox fx:id=\"row\" spacing=\"$gap\"/>\n"
                + "</VBox>\n").build();
        FXMLLoader loader = app.loader("main.fxml");
        VBox box = loader.load();
        assertEquals(3, box.getChildren().size());
        assertEquals(Double.valueOf(12.5), loader.getNamespace().get("gap"));
        ObservableList<?> names = (ObservableList<?>) loader.getNamespace().get("names");
        assertEquals(2, names.size());
        assertEquals(Color.GREEN, ((Label) box.getChildren().get(0)).getTextFill());
        assertSame(names, ((ComboBox<?>) box.getChildren().get(1)).getItems());
        assertEquals(12.5, ((HBox) box.getChildren().get(2)).getSpacing(), 0);
        // An element with an fx:id and no id of its own is found by it.
        assertEquals("row", box.getChildren().get(2).getId());
    }

    @Test
    public void defineAndReference() throws Exception {
        app("reference").resource("main.fxml", HEAD
                + "<VBox" + NS + ">\n"
                + "  <fx:define>\n"
                + "    <ToggleGroup fx:id=\"group\"/>\n"
                + "    <Insets fx:id=\"gap\" topRightBottomLeft=\"5\"/>\n"
                + "    <Label fx:id=\"shared\" text=\"shared\"/>\n"
                + "  </fx:define>\n"
                + "  <RadioButton text=\"a\" toggleGroup=\"$group\" VBox.margin=\"$gap\"/>\n"
                + "  <RadioButton text=\"b\"><toggleGroup><fx:reference source=\"group\"/></toggleGroup></RadioButton>\n"
                + "  <fx:reference source=\"shared\"/>\n"
                + "</VBox>\n").build();
        FXMLLoader loader = app.loader("main.fxml");
        VBox box = loader.load();
        assertEquals(3, box.getChildren().size());
        ToggleGroup group = (ToggleGroup) loader.getNamespace().get("group");
        assertNotNull(group);
        RadioButton a = (RadioButton) box.getChildren().get(0);
        RadioButton b = (RadioButton) box.getChildren().get(1);
        assertSame(group, a.getToggleGroup());
        assertSame(group, b.getToggleGroup());
        assertEquals(new Insets(5), VBox.getMargin(a));
        assertSame(loader.getNamespace().get("shared"), box.getChildren().get(2));
    }

    @Test
    public void resourceKeysAndLocations() throws Exception {
        app("bundle").resource("views/main.fxml", HEAD + "<?import javafx.scene.image.*?>\n"
                + "<VBox" + NS + " stylesheets=\"@main.css\">\n"
                + "  <Label text=\"%greeting\"/>\n"
                + "  <ImageView><image><Image url=\"@../img/logo.png\"/></image></ImageView>\n"
                + "  <ImageView><Image url=\"@/abs/icon.png\"/></ImageView>\n"
                + "</VBox>\n")
                .resource("views/main.css", ".label { -fx-text-fill: red; }\n").build();
        ResourceBundle bundle = new ListResourceBundle() {
            @Override
            protected Object[][] getContents() {
                return new Object[][] {{"greeting", "Bonjour"}};
            }
        };
        VBox box = FXMLLoader.load(FxmlHarness.location("views/main.fxml"), bundle);
        assertEquals("Bonjour", ((Label) box.getChildren().get(0)).getText());
        assertEquals("cn1res:/views/main.css", box.getStylesheets().get(0));
        assertEquals("cn1res:/img/logo.png", ((ImageView) box.getChildren().get(1)).getImage().getUrl());
        assertEquals("cn1res:/abs/icon.png", ((ImageView) box.getChildren().get(2)).getImage().getUrl());
        try {
            app.load("views/main.fxml");
            fail("A %key without a bundle must fail");
        } catch (LoadException e) {
            assertTrue(e.getMessage(), e.getMessage().contains("views/main.fxml"));
            assertTrue(e.getMessage(), e.getMessage().contains("%greeting"));
        }
    }

    @Test
    public void expressionBindingsFollowTheirSource() throws Exception {
        app("binding").resource("main.fxml", HEAD
                + "<VBox" + NS + ">\n"
                + "  <TextField fx:id=\"name\" text=\"ann\" prefWidth=\"90\"/>\n"
                + "  <Label fx:id=\"echo\" text=\"${name.text}\"/>\n"
                + "  <Label fx:id=\"wide\" text=\"${name.prefWidth}\" prefWidth=\"${name.prefWidth}\"/>\n"
                + "  <Button fx:id=\"go\" disable=\"${name.disable}\"/>\n"
                + "</VBox>\n").build();
        VBox box = app.load("main.fxml");
        TextField name = (TextField) box.getChildren().get(0);
        Label echo = (Label) box.getChildren().get(1);
        Label wide = (Label) box.getChildren().get(2);
        Button go = (Button) box.getChildren().get(3);
        assertEquals("ann", echo.getText());
        name.setText("bob");
        assertEquals("bob", echo.getText());
        assertEquals("90.0", wide.getText());
        assertEquals(90, wide.getPrefWidth(), 0);
        name.setPrefWidth(40);
        assertEquals(40, wide.getPrefWidth(), 0);
        assertFalse(go.isDisable());
        name.setDisable(true);
        assertTrue(go.isDisable());
    }

    // -------------------------------------------------------- controllers

    private static final String BASE = "package app;\n"
            + "import javafx.fxml.FXML;\nimport javafx.scene.control.Label;\n"
            + "public class Base {\n"
            + "    @FXML private Label status;\n"
            + "    private int baseCalls;\n"
            + "    @FXML private void onBase() { baseCalls++; }\n"
            + "    Label status() { return status; }\n"
            + "}\n";

    private static final String MAIN = "package app;\n"
            + "import javafx.event.ActionEvent;\nimport javafx.fxml.FXML;\nimport javafx.scene.control.*;\n"
            + "import javafx.scene.layout.VBox;\n"
            + "public class Main extends Base {\n"
            + "    @FXML private Label title;\n"
            + "    @FXML private Button go;\n"
            + "    @FXML Button plain;\n"
            + "    public Button open;\n"
            + "    private Button unmarked;\n"
            + "    @FXML private VBox sub;\n"
            + "    @FXML private Sub subController;\n"
            + "    @FXML private java.net.URL location;\n"
            + "    @FXML private java.util.ResourceBundle resources;\n"
            + "    private int events;\n    private int plains;\n    private String seen = \"\";\n"
            + "    private Main() { }\n"
            + "    @FXML private void onGo(ActionEvent e) { events++; seen += e.getSource() == go ? \"go\" : \"?\"; }\n"
            + "    @FXML private void onPlain() { plains++; }\n"
            + "    public void onOpen() { seen += \"open\"; }\n"
            + "    @FXML private void initialize() {\n"
            + "        seen += title != null && sub != null && subController != null ? \"init\" : \"early\";\n"
            + "    }\n"
            + "}\n";

    private static final String SUB = "package app;\n"
            + "import javafx.fxml.FXML;\nimport javafx.fxml.Initializable;\nimport javafx.scene.control.Label;\n"
            + "public class Sub implements Initializable {\n"
            + "    @FXML private Label inner;\n"
            + "    String state = \"\";\n"
            + "    @Override public void initialize(java.net.URL location, java.util.ResourceBundle resources) {\n"
            + "        state = inner.getText() + \"@\" + location;\n"
            + "    }\n"
            + "}\n";

    private FxmlHarness controllers(String name) throws Exception {
        return app(name).source("app.Base", BASE).source("app.Main", MAIN).source("app.Sub", SUB)
                .resource("ui/main.fxml", HEAD
                        + "<VBox" + NS + " fx:controller=\"app.Main\">\n"
                        + "  <Label fx:id=\"title\" text=\"T\"/>\n"
                        + "  <Label fx:id=\"status\" text=\"S\"/>\n"
                        + "  <Button fx:id=\"go\" onAction=\"#onGo\"/>\n"
                        + "  <Button fx:id=\"plain\" onAction=\"#onPlain\"/>\n"
                        + "  <Button fx:id=\"open\" onAction=\"#onOpen\"/>\n"
                        + "  <Button fx:id=\"unmarked\" onAction=\"#onBase\"/>\n"
                        + "  <fx:include fx:id=\"sub\" source=\"parts/sub.fxml\"/>\n"
                        + "</VBox>\n")
                .resource("ui/parts/sub.fxml", HEAD
                        + "<VBox" + NS + " fx:controller=\"app.Sub\" spacing=\"3\">\n"
                        + "  <Label fx:id=\"inner\" text=\"in\"/>\n"
                        + "</VBox>\n")
                .build();
    }

    @Test
    public void controllerMembersAreInjectedThroughTheDispatcher() throws Exception {
        controllers("controller");
        assertEquals(2, app.generator().documents());
        FXMLLoader loader = app.loader("ui/main.fxml");
        VBox box = loader.load();
        Object controller = loader.getController();
        assertEquals("app.Main", controller.getClass().getName());
        assertSame(controller, loader.getNamespace().get("controller"));
        // Private, package and public fields, and one the superclass
        // declares privately.
        assertSame(box.getChildren().get(0), FxmlHarness.field(controller, "title"));
        assertSame(box.getChildren().get(1), FxmlHarness.field(controller, "status"));
        assertSame(box.getChildren().get(2), FxmlHarness.field(controller, "go"));
        assertSame(box.getChildren().get(3), FxmlHarness.field(controller, "plain"));
        assertSame(box.getChildren().get(4), FxmlHarness.field(controller, "open"));
        // A private field without @FXML is not the document's to set.
        assertNull(FxmlHarness.field(controller, "unmarked"));
        assertEquals(FxmlHarness.location("ui/main.fxml"), FxmlHarness.field(controller, "location"));
        assertNull(FxmlHarness.field(controller, "resources"));
        // initialize() ran once, after every field was set.
        assertEquals("init", FxmlHarness.field(controller, "seen"));
    }

    @Test
    public void handlersCallControllerMethods() throws Exception {
        controllers("handlers");
        FXMLLoader loader = app.loader("ui/main.fxml");
        VBox box = loader.load();
        Object controller = loader.getController();
        ((Button) box.getChildren().get(2)).fire();
        ((Button) box.getChildren().get(2)).fire();
        ((Button) box.getChildren().get(3)).fire();
        ((Button) box.getChildren().get(4)).fire();
        ((Button) box.getChildren().get(5)).fire();
        assertEquals(2, FxmlHarness.field(controller, "events"));
        assertEquals(1, FxmlHarness.field(controller, "plains"));
        assertEquals("initgogoopen", FxmlHarness.field(controller, "seen"));
        assertEquals(1, FxmlHarness.field(controller, "baseCalls"));
    }

    @Test
    public void includesLoadTheirOwnDocumentAndController() throws Exception {
        controllers("include");
        FXMLLoader loader = app.loader("ui/main.fxml");
        VBox box = loader.load();
        Object controller = loader.getController();
        VBox sub = (VBox) box.getChildren().get(6);
        assertEquals(3, sub.getSpacing(), 0);
        assertSame(sub, FxmlHarness.field(controller, "sub"));
        Object subController = FxmlHarness.field(controller, "subController");
        assertEquals("app.Sub", subController.getClass().getName());
        assertSame(subController, loader.getNamespace().get("subController"));
        assertSame(sub.getChildren().get(0), FxmlHarness.field(subController, "inner"));
        // The included document is located relative to the including one.
        assertEquals("in@" + FxmlHarness.location("ui/parts/sub.fxml"), FxmlHarness.field(subController, "state"));
        // The included document's ids stay its own.
        assertFalse(loader.getNamespace().containsKey("inner"));
    }

    @Test
    public void controllerFactoryAndSetController() throws Exception {
        controllers("factory2");
        final Class<?> main = app.type("app.Main");
        final Object[] made = new Object[1];
        final int[] asked = new int[1];
        FXMLLoader loader = app.loader("ui/parts/sub.fxml");
        loader.setControllerFactory(new Callback<Class<?>, Object>() {
            @Override
            public Object call(Class<?> type) {
                asked[0]++;
                assertEquals("app.Sub", type.getName());
                try {
                    made[0] = type.newInstance();
                } catch (Exception e) {
                    throw new IllegalStateException(e);
                }
                return made[0];
            }
        });
        loader.load();
        assertEquals(1, asked[0]);
        assertSame(made[0], loader.getController());
        assertNotNull(main);
        // A controller the application made itself is the one used.
        Object own = app.type("app.Sub").newInstance();
        FXMLLoader second = app.loader("ui/parts/sub.fxml");
        second.setController(own);
        VBox root = second.load();
        assertSame(own, second.getController());
        assertSame(root.getChildren().get(0), FxmlHarness.field(own, "inner"));
    }

    @Test
    public void rootElementBuildsOnTheObjectTheLoaderWasGiven() throws Exception {
        app("root").source("app.Card", "package app;\n"
                + "import javafx.fxml.FXML;\nimport javafx.scene.control.Label;\nimport javafx.scene.layout.VBox;\n"
                + "public class Card extends VBox {\n"
                + "    @FXML private Label caption;\n"
                + "    public String caption() { return caption.getText(); }\n"
                + "}\n")
                .resource("card.fxml", HEAD
                        + "<fx:root type=\"VBox\"" + NS + " spacing=\"4\">\n"
                        + "  <Label fx:id=\"caption\" text=\"cap\"/>\n"
                        + "</fx:root>\n").build();
        Object card = app.type("app.Card").newInstance();
        FXMLLoader loader = app.loader("card.fxml");
        loader.setRoot(card);
        loader.setController(card);
        assertSame(card, loader.load());
        assertEquals(4, ((VBox) card).getSpacing(), 0);
        assertEquals(1, ((VBox) card).getChildren().size());
        assertSame(((VBox) card).getChildren().get(0), FxmlHarness.field(card, "caption"));
        try {
            app.loader("card.fxml").load();
            fail("fx:root without setRoot must fail");
        } catch (LoadException e) {
            assertTrue(e.getMessage(), e.getMessage().contains("setRoot"));
        }
    }

    @Test
    public void aDocumentThatWasNotCompiledSaysSo() throws Exception {
        controllers("missing");
        try {
            app.load("ui/other.fxml");
            fail("A document that is not there must fail");
        } catch (LoadException e) {
            assertTrue(e.getMessage(), e.getMessage().contains("No compiled FXML document"));
            assertTrue(e.getMessage(), e.getMessage().contains("ui/other.fxml"));
        }
    }

    @Test
    public void aWrongFieldTypeFailsWithTheNames() throws Exception {
        app("mismatch").source("app.Wrong", "package app;\n"
                + "public class Wrong {\n    @javafx.fxml.FXML private javafx.scene.control.Button title;\n}\n")
                .resource("main.fxml", HEAD + "<VBox" + NS + " fx:controller=\"app.Wrong\">\n"
                        + "  <Label fx:id=\"title\"/>\n</VBox>\n").build();
        try {
            app.load("main.fxml");
            fail("A Label cannot be injected into a Button field");
        } catch (LoadException e) {
            assertTrue(e.getMessage(), e.getMessage().contains("title"));
            assertTrue(e.getMessage(), e.getMessage().contains("Button"));
            assertTrue(e.getMessage(), e.getMessage().contains("Label"));
        }
    }

    // ------------------------------------------------------------- errors

    @Test
    public void unknownClassIsAnErrorWithItsPosition() throws Exception {
        String error = errorOf("unknown-class", HEAD + "<VBox" + NS + ">\n"
                + "  <Label text=\"a\"/>\n"
                + "  <Lable text=\"b\"/>\n"
                + "</VBox>\n");
        assertTrue(error, error.startsWith(app.file("bad.fxml") + ":7:"));
        assertTrue(error, error.contains("Lable"));
        assertTrue(error, error.contains("<Lable text=\"b\">"));
    }

    @Test
    public void unknownPropertyIsAnErrorWithItsPosition() throws Exception {
        String error = errorOf("unknown-property", HEAD + "<VBox" + NS + ">\n"
                + "  <Label txt=\"b\"/>\n"
                + "</VBox>\n");
        assertTrue(error, error.startsWith(app.file("bad.fxml") + ":6:"));
        assertTrue(error, error.contains("txt=\"b\""));
        assertTrue(error, error.contains("has no property txt"));
    }

    @Test
    public void aValueThatDoesNotConvertIsAnError() throws Exception {
        String error = errorOf("bad-value", HEAD + "<VBox" + NS + " spacing=\"wide\"/>\n");
        assertTrue(error, error.startsWith(app.file("bad.fxml") + ":5:"));
        assertTrue(error, error.contains("wide"));
        error = errorOf("bad-enum", HEAD + "<VBox" + NS + " alignment=\"MIDDLE\"/>\n");
        assertTrue(error, error.contains("MIDDLE"));
    }

    @Test
    public void scriptsAreAnError() throws Exception {
        String error = errorOf("script", HEAD + "<VBox" + NS + ">\n"
                + "  <fx:script>var x = 1;</fx:script>\n</VBox>\n");
        assertTrue(error, error.startsWith(app.file("bad.fxml") + ":6:"));
        assertTrue(error, error.contains("<fx:script> is not supported"));
        error = errorOf("language", "<?xml version=\"1.0\"?>\n<?language javascript?>\n"
                + "<?import javafx.scene.layout.VBox?>\n<VBox" + NS + "/>\n");
        assertTrue(error, error.startsWith(app.file("bad.fxml") + ":2:"));
        assertTrue(error, error.contains("scripts are not supported"));
        error = errorOf("script-handler", HEAD + "<Button" + NS + " onAction=\"go()\"/>\n");
        assertTrue(error, error.contains("#method"));
    }

    @Test
    public void structuralMistakesAreErrors() throws Exception {
        String error = errorOf("malformed", HEAD + "<VBox" + NS + ">\n  <Label>\n</VBox>\n");
        assertTrue(error, error.contains("not well formed XML"));
        assertTrue(error, error.startsWith(app.file("bad.fxml") + ":7:"));
        error = errorOf("no-controller", HEAD + "<Button" + NS + " onAction=\"#go\"/>\n");
        assertTrue(error, error.contains("fx:controller"));
        error = errorOf("no-include", HEAD + "<VBox" + NS + "><fx:include source=\"none.fxml\"/></VBox>\n");
        assertTrue(error, error.contains("none.fxml"));
        error = errorOf("bad-reference", HEAD + "<VBox" + NS + "><Label text=\"$nothing\"/></VBox>\n");
        assertTrue(error, error.contains("nothing"));
        error = errorOf("twice", HEAD + "<VBox" + NS + "><Label fx:id=\"a\"/><Label fx:id=\"a\"/></VBox>\n");
        assertTrue(error, error.contains("used twice"));
        error = errorOf("wrong-type", HEAD + "<VBox" + NS + "><padding><Label/></padding></VBox>\n");
        assertTrue(error, error.contains("Insets"));
        error = errorOf("expression", HEAD + "<VBox" + NS + "><TextField fx:id=\"n\"/>"
                + "<Label text=\"${n.text + '!'}\"/></VBox>\n");
        assertTrue(error, error.contains("${"));
    }

    @Test
    public void everyBrokenDocumentIsReportedAndNoneStopsTheOthers() throws Exception {
        List<String> errors = app("several")
                .resource("a.fxml", HEAD + "<VBox" + NS + "><Nope/></VBox>\n")
                .resource("b.fxml", HEAD + "<VBox" + NS + " nope=\"1\"/>\n")
                .resource("c.fxml", HEAD + "<VBox" + NS + "/>\n").compile();
        assertEquals(errors.toString(), 2, errors.size());
        assertTrue(app.generated("c.fxml").contains("class Fxml_c"));
    }

    // ------------------------------------------------- custom controls

    private static final String BADGE = "package shop;\n"
            + "import javafx.fxml.FXML;\n"
            + "import javafx.fxml.FXMLLoader;\n"
            + "import javafx.scene.control.Label;\n"
            + "import javafx.scene.layout.HBox;\n"
            + "public class Badge extends HBox {\n"
            + "    @FXML private Label caption;\n"
            + "    private int level;\n"
            + "    public Badge() {\n"
            + "        try {\n"
            // What Badge.class.getResource("badge.fxml") answers in a built
            // application; this class loader has no resources of its own.
            + "            FXMLLoader loader = new FXMLLoader(\n"
            + "                    new java.net.URL(\"file:/work/app/target/classes/shop/badge.fxml\"));\n"
            + "            loader.setRoot(this);\n"
            + "            loader.setController(this);\n"
            + "            loader.load();\n"
            + "        } catch (java.io.IOException e) {\n"
            + "            throw new RuntimeException(e);\n"
            + "        }\n"
            + "    }\n"
            + "    public String getText() { return caption.getText(); }\n"
            + "    public void setText(String text) { caption.setText(text); }\n"
            + "    public int getLevel() { return level; }\n"
            + "    public void setLevel(int level) { this.level = level; }\n"
            + "}\n";

    /// A class of the application as an element, itself built from a
    /// document with `<fx:root>`: the documents are compiled after the
    /// application, so the control's own setters are read like any other's.
    @Test
    public void aCustomControlOfTheApplicationIsAnElement() throws Exception {
        app("custom").source("shop.Badge", BADGE)
                .resource("shop/badge.fxml", HEAD + "<fx:root type=\"HBox\"" + NS + " spacing=\"3\">\n"
                        + "  <Label fx:id=\"caption\" text=\"-\"/>\n"
                        + "</fx:root>\n")
                .resource("shop/main.fxml", HEAD + "<?import shop.Badge?>\n"
                        + "<VBox" + NS + ">\n"
                        + "  <Badge fx:id=\"first\" text=\"New\" level=\"7\" VBox.vgrow=\"ALWAYS\"/>\n"
                        + "  <shop.Badge text=\"Sale\"/>\n"
                        + "</VBox>\n").build();
        VBox box = app.load("shop/main.fxml");
        assertEquals(2, box.getChildren().size());
        Node first = box.getChildren().get(0);
        assertEquals("shop.Badge", first.getClass().getName());
        assertEquals(3, ((HBox) first).getSpacing(), 0);
        assertEquals(Priority.ALWAYS, VBox.getVgrow(first));
        assertEquals("New", ((Label) ((HBox) first).getChildren().get(0)).getText());
        assertEquals(Integer.valueOf(7), first.getClass().getMethod("getLevel").invoke(first));
        assertEquals("Sale", ((Label) ((HBox) box.getChildren().get(1)).getChildren().get(0)).getText());
    }

    @Test
    public void aPropertyACustomControlLacksIsAnErrorOfTheDocument() throws Exception {
        List<String> errors = app("custom-bad").source("shop.Badge", BADGE)
                .resource("shop/badge.fxml", HEAD + "<fx:root type=\"HBox\"" + NS + "><Label fx:id=\"caption\"/>"
                        + "</fx:root>\n")
                .resource("shop/main.fxml", HEAD + "<?import shop.*?>\n<VBox" + NS + "><Badge colour=\"red\"/></VBox>\n")
                .compile();
        assertEquals(errors.toString(), 1, errors.size());
        assertTrue(errors.get(0), errors.get(0).contains("main.fxml:6:"));
        assertTrue(errors.get(0), errors.get(0).contains("shop.Badge has no property colour"));
    }

    /// A build in which javac did not run finds the classes directory as
    /// the last build's relocation left it, and must leave the documents'
    /// classes alone: compiled again they would not fit the relocated
    /// classes around them.
    @Test
    public void aRelocatedClassesDirectoryIsLeftAlone() throws Exception {
        app("relocated").resource("main.fxml", HEAD + "<VBox" + NS + "/>\n").build();
        File dir = new File(app.classes(), "com/codename1/generated/fxml");
        File marker = new File(dir, "FxmlDocuments.class");
        File document = new File(dir, "Fxml_main.class");
        assertTrue(marker.isFile());
        assertTrue(document.isFile());
        assertFalse(FxmlClassCompiler.relocated(app.classes()));

        // What the relocation does to the one name the class holds.
        final org.objectweb.asm.ClassWriter moved = new org.objectweb.asm.ClassWriter(0);
        new org.objectweb.asm.ClassReader(java.nio.file.Files.readAllBytes(marker.toPath())).accept(
                new org.objectweb.asm.ClassVisitor(org.objectweb.asm.Opcodes.ASM9, moved) {
                    @Override
                    public org.objectweb.asm.MethodVisitor visitMethod(int access, String name, String descriptor,
                            String signature, String[] exceptions) {
                        return new org.objectweb.asm.MethodVisitor(org.objectweb.asm.Opcodes.ASM9,
                                super.visitMethod(access, name, descriptor, signature, exceptions)) {
                            @Override
                            public void visitLdcInsn(Object value) {
                                super.visitLdcInsn(value instanceof org.objectweb.asm.Type
                                        ? org.objectweb.asm.Type.getObjectType("com/codename1/fxcompat/"
                                                + ((org.objectweb.asm.Type) value).getInternalName()) : value);
                            }
                        };
                    }
                }, 0);
        java.nio.file.Files.write(marker.toPath(), moved.toByteArray());
        assertTrue(FxmlClassCompiler.relocated(app.classes()));

        java.nio.file.Files.write(document.toPath(), new byte[] {1, 2, 3});
        FxmlClassCompiler again = new FxmlClassCompiler(java.util.Collections.singletonList(
                new File(app.file("main.fxml")).getParentFile()), FxmlHarness.classpath(),
                java.util.Collections.<File>emptyList(), app.classes(), null, new DesktopResourceCompiler.Log() {
                    @Override
                    public void info(String message) {
                    }

                    @Override
                    public void warn(String message) {
                    }
                });
        assertTrue(again.run().isEmpty());
        assertEquals(0, again.compiled());
        assertEquals("The document's class was not compiled again", 3, document.length());
    }

    @Test
    public void classNamesAreStableAndDistinct() {
        assertEquals("Fxml_main", FxmlCompiler.className("main.fxml"));
        assertEquals("Fxml_ui__main_mview", FxmlCompiler.className("ui/main-view.fxml"));
        assertEquals("Fxml_a_ub", FxmlCompiler.className("a_b.fxml"));
        assertFalse(FxmlCompiler.className("a/b.fxml").equals(FxmlCompiler.className("a__b.fxml")));
    }
}
