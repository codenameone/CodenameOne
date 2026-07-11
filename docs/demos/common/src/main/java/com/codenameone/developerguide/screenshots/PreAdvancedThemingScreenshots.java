package com.codenameone.developerguide.screenshots;

import com.codename1.components.SpanLabel;
import com.codenameone.developerguide.snippets.generated.BasicsJava034Snippet;
import com.codename1.ui.Button;
import com.codename1.ui.Component;
import com.codename1.ui.Container;
import com.codename1.ui.CN;
import com.codename1.ui.FontImage;
import com.codename1.ui.Font;
import com.codename1.ui.Form;
import com.codename1.ui.Graphics;
import com.codename1.ui.Image;
import com.codename1.ui.Label;
import com.codename1.ui.TextField;
import com.codename1.ui.layouts.BorderLayout;
import com.codename1.ui.layouts.BoxLayout;
import com.codename1.ui.layouts.FlowLayout;
import com.codename1.ui.layouts.GridBagConstraints;
import com.codename1.ui.layouts.GridBagLayout;
import com.codename1.ui.layouts.GridLayout;
import com.codename1.ui.layouts.LayeredLayout;
import com.codename1.ui.layouts.mig.MigLayout;
import com.codename1.ui.plaf.Border;
import com.codename1.ui.plaf.Style;
import com.codename1.ui.table.TableLayout;
import com.codename1.ui.util.ImageIO;

import java.io.IOException;
import java.io.OutputStream;

/**
 * Generates the app screenshots used before the Advanced Theming chapter.
 */
public final class PreAdvancedThemingScreenshots {
    private static final int PORTRAIT_WIDTH = 320;
    private static final int PORTRAIT_HEIGHT = 480;
    private static final int LANDSCAPE_WIDTH = 480;
    private static final int LANDSCAPE_HEIGHT = 320;
    private static final int BLUE = 0x0b57d0;
    private static final int GREEN = 0x06a806;
    private static final int WHITE = 0xffffff;
    private static final Font TITLE_FONT = Font.createSystemFont(Font.FACE_SYSTEM, Font.STYLE_PLAIN, Font.SIZE_LARGE)
            .derive(35, Font.STYLE_PLAIN);
    private static final Font BLOCK_FONT = Font.createSystemFont(Font.FACE_SYSTEM, Font.STYLE_PLAIN, Font.SIZE_LARGE)
            .derive(24, Font.STYLE_PLAIN);

    private PreAdvancedThemingScreenshots() {
    }

    public interface ScreenshotSink {
        OutputStream open(String fileName) throws IOException;
    }

    public static void generate(ScreenshotSink sink) throws IOException {
        write(sink, "flow-layout.png", flow("Flow Layout", Component.LEFT, Component.TOP), PORTRAIT_WIDTH, PORTRAIT_HEIGHT);
        write(sink, "flow-layout-center.png", flow("Flow Layout", Component.CENTER, Component.TOP), PORTRAIT_WIDTH, PORTRAIT_HEIGHT);
        write(sink, "flow-layout-right.png", flow("Flow Layout", Component.RIGHT, Component.TOP), PORTRAIT_WIDTH, PORTRAIT_HEIGHT);
        write(sink, "flow-layout-center-middle.png", flow("Flow Layout", Component.CENTER, Component.CENTER), PORTRAIT_WIDTH, PORTRAIT_HEIGHT);
        write(sink, "box-layout-y.png", boxY(), PORTRAIT_WIDTH, PORTRAIT_HEIGHT);
        write(sink, "box-layout-x.png", boxX(true), PORTRAIT_WIDTH, PORTRAIT_HEIGHT);
        write(sink, "box-layout-x-no-grow.png", boxX(false), PORTRAIT_WIDTH, PORTRAIT_HEIGHT);
        write(sink, "border-layout.png", border(false, false), LANDSCAPE_WIDTH, LANDSCAPE_HEIGHT);
        write(sink, "border-layout-center.png", border(true, false), LANDSCAPE_WIDTH, LANDSCAPE_HEIGHT);
        write(sink, "border-layout-RTL.png", border(false, true), LANDSCAPE_WIDTH, LANDSCAPE_HEIGHT);
        write(sink, "grid-layout-2x2.png", grid(2, 2, "Grid 2x2 Layout"), PORTRAIT_WIDTH, PORTRAIT_HEIGHT);
        write(sink, "grid-layout-2x4.png", grid(2, 4, "Grid 2x4 Layout"), PORTRAIT_WIDTH, PORTRAIT_HEIGHT);
        write(sink, "grid-layout-autofit-portrait.png", gridAutofit("Grid Layout AutoFit"), PORTRAIT_WIDTH, PORTRAIT_HEIGHT);
        write(sink, "grid-layout-autofit-landscape.png", gridAutofit("Grid Layout AutoFit"), LANDSCAPE_WIDTH, LANDSCAPE_HEIGHT);
        write(sink, "table-layout-2x2.png", table2x2(), PORTRAIT_WIDTH, PORTRAIT_HEIGHT);
        write(sink, "table-layout-enclose.png", tableEnclose(), LANDSCAPE_WIDTH, LANDSCAPE_HEIGHT);
        write(sink, "table-layout-constraints.png", tableConstraints(), LANDSCAPE_WIDTH, LANDSCAPE_HEIGHT);
        write(sink, "layered-layout.png", layeredCloseButton(), PORTRAIT_WIDTH, PORTRAIT_HEIGHT);
        write(sink, "guibuilder-2-insets-1.png", insetsLowerRight(), PORTRAIT_WIDTH, PORTRAIT_HEIGHT);
        write(sink, "guibuilder-2-insets-2.png", insetsCenterLeft(), PORTRAIT_WIDTH, PORTRAIT_HEIGHT);
        write(sink, "guibuilder-2-insets-3.png", insetsReference(), LANDSCAPE_WIDTH, LANDSCAPE_HEIGHT);
        write(sink, "gridbag-layout.png", gridBag(), LANDSCAPE_WIDTH, LANDSCAPE_HEIGHT);
        write(sink, "group-layout.png", groupLayout(), PORTRAIT_WIDTH, PORTRAIT_HEIGHT);
        write(sink, "mig-layout.png", migLayout(), PORTRAIT_WIDTH, PORTRAIT_HEIGHT);
    }

    private static Form flow(String title, int align, int valign) {
        Form hi = new Form(title, new FlowLayout(align, valign));
        addDemoLabels(hi);
        return hi;
    }

    private static Form boxY() {
        Form hi = new Form("Box Y Layout", BoxLayout.y());
        addDemoLabels(hi);
        return hi;
    }

    private static Form boxX(boolean grow) {
        int axis = grow ? BoxLayout.X_AXIS : BoxLayout.X_AXIS_NO_GROW;
        Form hi = new Form(grow ? "Box X Layout" : "Box X No Grow", new BoxLayout(axis));
        addDemoLabels(hi);
        return hi;
    }

    private static Form border(boolean centerBehavior, boolean rtl) {
        Form hi = new Form("Border Layout", new BorderLayout());
        if (centerBehavior) {
            ((BorderLayout) hi.getLayout()).setCenterBehavior(BorderLayout.CENTER_BEHAVIOR_CENTER);
        }
        hi.add(BorderLayout.CENTER, demoLabel("Center"))
                .add(BorderLayout.SOUTH, demoLabel("South"))
                .add(BorderLayout.NORTH, demoLabel("North"))
                .add(BorderLayout.EAST, demoLabel(rtl ? "West" : "East"))
                .add(BorderLayout.WEST, demoLabel(rtl ? "East" : "West"));
        return hi;
    }

    private static Form grid(int rows, int columns, String title) {
        Form hi = new Form(title, new GridLayout(rows, columns));
        addDemoLabels(hi);
        return hi;
    }

    private static Form gridAutofit(String title) {
        Form hi = new Form(title, GridLayout.autoFit());
        addDemoLabels(hi);
        return hi;
    }

    private static Form table2x2() {
        Form hi = new Form("Table Layout 2x2", new TableLayout(2, 2));
        addDemoLabels(hi);
        return hi;
    }

    private static Form tableEnclose() {
        Form hi = new Form("TableLayout Enclose 2", new BorderLayout());
        Container table = TableLayout.encloseIn(2,
                demoLabel("First"),
                demoLabel("Second"),
                demoLabel("Third"),
                demoLabel("Fourth"),
                demoLabel("Fifth"));
        hi.add(BorderLayout.CENTER, table);
        return hi;
    }

    private static Form tableConstraints() {
        TableLayout layout = new TableLayout(4, 3);
        layout.setGrowHorizontally(true);
        Form hi = new Form("Table Layout", layout);

        TableLayout.Constraint title = layout.createConstraint();
        title.setHorizontalSpan(3);
        title.setHorizontalAlign(Component.CENTER);
        hi.add(title, demoLabel("Invoice"));

        hi.add(demoLabel("Item"));
        hi.add(demoLabel("Qty"));
        hi.add(demoLabel("Total"));
        hi.add(demoLabel("Design"));
        hi.add(demoLabel("2"));
        hi.add(demoLabel("$120"));

        TableLayout.Constraint notes = layout.createConstraint();
        notes.setHorizontalSpan(2);
        notes.setHeightPercentage(40);
        hi.add(notes, demoSpanLabel("Notes span two columns"));
        hi.add(demoButton("Pay"));
        return hi;
    }

    private static Form layeredCloseButton() {
        Form hi = new Form("Layered Layout");
        Label settingsLabel = demoLabel("Settings");
        settingsLabel.setIcon(FontImage.createMaterial(FontImage.MATERIAL_SETTINGS, settingsLabel.getUnselectedStyle()));
        Button close = new Button("X");
        close.setUIID("Container");
        close.getAllStyles().setFgColor(0xd32f2f);
        close.getAllStyles().setAlignment(Component.CENTER);
        hi.add(LayeredLayout.encloseIn(settingsLabel, FlowLayout.encloseRight(close)));
        return hi;
    }

    private static Form insetsLowerRight() {
        Form f = new Form("Layered Insets", new BorderLayout());
        Container cnt = new Container(new LayeredLayout());
        cnt.add(demoButton("Submit"));
        ((LayeredLayout) cnt.getLayout()).setInsets(cnt.getComponentAt(0), "auto 0 0 auto");
        f.add(BorderLayout.CENTER, cnt);
        return f;
    }

    private static Form insetsCenterLeft() {
        Form f = new Form("Layered Insets", new BorderLayout());
        Container cnt = new Container(new LayeredLayout());
        cnt.add(demoButton("Submit"));
        ((LayeredLayout) cnt.getLayout()).setInsets(cnt.getComponentAt(0), "auto auto auto 5mm");
        f.add(BorderLayout.CENTER, cnt);
        return f;
    }

    private static Form insetsReference() {
        Form f = new Form("Layered References", new BorderLayout());
        Container cnt = new Container(new LayeredLayout());
        LayeredLayout ll = (LayeredLayout) cnt.getLayout();
        Button btn = demoButton("Submit");
        TextField tf = new TextField();
        tf.setHint("Name");
        cnt.add(tf).add(btn);
        ll.setInsets(tf, "auto")
                .setInsets(btn, "auto auto auto 0")
                .setReferenceComponentLeft(btn, tf, 1f);
        f.add(BorderLayout.CENTER, cnt);
        return f;
    }

    private static Form gridBag() {
        Form hi = new Form("GridBagLayout", new BorderLayout());
        Container grid = new Container(new GridBagLayout());
        GridBagConstraints c = new GridBagConstraints();
        c.fill = GridBagConstraints.HORIZONTAL;
        c.weightx = 0.5;
        c.gridx = 0;
        c.gridy = 0;
        grid.addComponent(c, demoButton("Button 1"));
        c.gridx = 1;
        grid.addComponent(c, demoButton("Button 2"));
        c.gridx = 2;
        grid.addComponent(c, demoButton("Button 3"));

        c.gridx = 0;
        c.gridy = 1;
        c.gridwidth = 3;
        grid.addComponent(c, demoButton("Long-Named Button 4"));

        c.ipady = 40;
        c.weighty = 1.0;
        c.gridx = 1;
        c.gridy = 2;
        c.gridwidth = 2;
        grid.addComponent(c, demoButton("5"));
        hi.add(BorderLayout.CENTER, grid);
        return hi;
    }

    private static Form groupLayout() {
        return BasicsJava034Snippet.createForm();
    }

    private static Form migLayout() {
        Form hi = new Form("MigLayout", new MigLayout("wrap 3", "[grow][grow][grow]", ""));
        hi.add(demoLabel("First"));
        hi.add("span 1 2,grow", demoLabel("Second"));
        hi.add(demoLabel("Third"));
        hi.add(demoLabel("Fourth"));
        hi.add(demoLabel("Fifth"));
        hi.add(demoLabel("Sixth"));
        hi.add("span 2", demoLabel("Seventh"));
        return hi;
    }

    private static void addDemoLabels(Container container) {
        container.add(demoLabel("First"))
                .add(demoLabel("Second"))
                .add(demoLabel("Third"))
                .add(demoLabel("Fourth"))
                .add(demoLabel("Fifth"));
    }

    private static Label demoLabel(String text) {
        Label label = new Label(text);
        styleBlock(label);
        return label;
    }

    private static SpanLabel demoSpanLabel(String text) {
        SpanLabel label = new SpanLabel(text);
        styleBlock(label);
        return label;
    }

    private static Button demoButton(String text) {
        Button button = new Button(text);
        styleBlock(button);
        return button;
    }

    private static void styleBlock(Component component) {
        Style style = component.getAllStyles();
        style.setBgTransparency(255);
        style.setBgColor(GREEN);
        style.setFgColor(WHITE);
        style.setAlignment(Component.CENTER);
        style.setFont(BLOCK_FONT);
        style.setPaddingUnit(Style.UNIT_TYPE_DIPS);
        style.setPadding(1, 1, 2, 2);
        style.setMarginUnit(Style.UNIT_TYPE_PIXELS);
        style.setMargin(1, 1, 1, 1);
        style.setBorder(Border.createLineBorder(1, WHITE));
    }

    private static void prepare(Form form, int width, int height) {
        CN.setWindowSize(width, height);
        applyScreenshotStyle(form);
        form.setScrollable(false);
        form.show();
        form.setWidth(width);
        form.setHeight(height);
        form.getContentPane().setWidth(width);
        form.getContentPane().setHeight(Math.max(0, height - form.getTitleArea().getHeight()));
        form.revalidate();
    }

    private static void applyScreenshotStyle(Form form) {
        Style titleArea = form.getTitleArea().getAllStyles();
        titleArea.setBgTransparency(255);
        titleArea.setBgColor(BLUE);
        titleArea.setPaddingUnit(Style.UNIT_TYPE_PIXELS);
        titleArea.setPadding(0, 0, 0, 0);

        Style title = form.getTitleStyle();
        title.setFgColor(WHITE);
        title.setBgTransparency(0);
        title.setFont(TITLE_FONT);
        title.setPaddingUnit(Style.UNIT_TYPE_PIXELS);
        title.setPadding(4, 5, 0, 0);

        applyBlockStyleToContent(form.getContentPane());
    }

    private static void applyBlockStyleToContent(Container container) {
        for (int i = 0; i < container.getComponentCount(); i++) {
            Component component = container.getComponentAt(i);
            if (component instanceof Label || component instanceof Button) {
                styleBlock(component);
            }
            if (component instanceof Container) {
                applyBlockStyleToContent((Container) component);
            }
        }
    }

    private static void write(ScreenshotSink sink, String fileName, Form form, int width, int height) throws IOException {
        prepare(form, width, height);
        Image screenshot = Image.createImage(width, height, 0xffffff);
        Graphics graphics = screenshot.getGraphics();
        form.paintComponent(graphics, true);
        try (OutputStream out = sink.open(fileName)) {
            ImageIO.getImageIO().save(screenshot, out, ImageIO.FORMAT_PNG, 1);
        }
    }

}
