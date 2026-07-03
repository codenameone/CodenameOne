package com.codenameone.developerguide.screenshots;

import com.codename1.components.Accordion;
import com.codename1.components.FloatingActionButton;
import com.codename1.components.FloatingHint;
import com.codename1.components.InteractionDialog;
import com.codename1.components.MultiButton;
import com.codename1.components.OnOffSwitch;
import com.codename1.components.SpanButton;
import com.codename1.components.SpanLabel;
import com.codename1.components.SplitPane;
import com.codename1.ui.Button;
import com.codename1.ui.ButtonGroup;
import com.codename1.ui.CheckBox;
import com.codename1.ui.ComboBox;
import com.codename1.ui.Component;
import com.codename1.ui.ComponentGroup;
import com.codename1.ui.Container;
import com.codename1.ui.Dialog;
import com.codename1.ui.Display;
import com.codename1.ui.Font;
import com.codename1.ui.FontImage;
import com.codename1.ui.Form;
import com.codename1.ui.Graphics;
import com.codename1.ui.Image;
import com.codename1.ui.Label;
import com.codename1.ui.RadioButton;
import com.codename1.ui.Slider;
import com.codename1.ui.Stroke;
import com.codename1.ui.Tabs;
import com.codename1.ui.TextArea;
import com.codename1.ui.TextField;
import com.codename1.ui.geom.Dimension;
import com.codename1.ui.geom.GeneralPath;
import com.codename1.ui.layouts.BorderLayout;
import com.codename1.ui.layouts.BoxLayout;
import com.codename1.ui.layouts.FlowLayout;
import com.codename1.ui.layouts.GridLayout;
import com.codename1.ui.layouts.LayeredLayout;
import com.codename1.ui.plaf.Border;
import com.codename1.ui.plaf.Style;
import com.codename1.ui.spinner.Picker;
import com.codenameone.developerguide.Demo;

import java.util.Date;

/**
 * JavaSE-safe forms used to refresh developer-guide screenshots from live code.
 */
public final class GuideStaticScreenshotDemos {

    private GuideStaticScreenshotDemos() {
    }

    public static Demo flowLayout(String title, int align, int valign) {
        return formDemo(title, "FlowLayout alignment", () -> {
            Form form = new Form(title, new BorderLayout());
            Container content = new Container(new FlowLayout(align, valign));
            content.setUIID("PaddedContainer");
            addTiles(content, "One", "Two", "Three", "Four");
            form.add(BorderLayout.CENTER, content);
            return form;
        });
    }

    public static Demo boxLayoutY() {
        return formDemo("BoxLayout Y", "Vertical BoxLayout", () -> {
            Form form = new Form("BoxLayout Y", BoxLayout.y());
            form.add(coloredLabel("First", 0x0d47a1)).
                    add(coloredLabel("Second", 0x1565c0)).
                    add(coloredLabel("Third", 0x1976d2));
            return form;
        });
    }

    public static Demo boxLayoutX(boolean noGrow) {
        return formDemo(noGrow ? "BoxLayout X No Grow" : "BoxLayout X", "Horizontal BoxLayout", () -> {
            Form form = new Form(noGrow ? "Box X No Grow" : "BoxLayout X", new BorderLayout());
            Container row = new Container(new BoxLayout(noGrow ? BoxLayout.X_AXIS_NO_GROW : BoxLayout.X_AXIS));
            row.setUIID("PaddedContainer");
            addTiles(row, "A", "B", "C");
            form.add(BorderLayout.NORTH, row);
            form.add(BorderLayout.CENTER, centeredHint("Horizontal axis"));
            return form;
        });
    }

    public static Demo borderLayout(String title, boolean centerBehavior, boolean rtl) {
        return formDemo(title, "BorderLayout regions", () -> {
            Form form = new Form(title, new BorderLayout(centerBehavior ? BorderLayout.CENTER_BEHAVIOR_CENTER : BorderLayout.CENTER_BEHAVIOR_SCALE));
            form.setRTL(rtl);
            form.add(BorderLayout.NORTH, coloredLabel("North", 0x455a64));
            form.add(BorderLayout.SOUTH, coloredLabel("South", 0x546e7a));
            form.add(BorderLayout.EAST, coloredLabel("East", 0x00838f));
            form.add(BorderLayout.WEST, coloredLabel("West", 0x00695c));
            form.add(BorderLayout.CENTER, coloredLabel("Center", 0x2e7d32));
            return form;
        });
    }

    public static Demo gridLayout(String title, int rows, int cols) {
        return formDemo(title, "GridLayout", () -> {
            Form form = new Form(title, new GridLayout(rows, cols));
            String[] labels;
            if ("Grid Layout 2x2".equals(title)) {
                labels = new String[]{"First", "Second", "Third", "Fourth", "Fifth"};
            } else if ("Grid Layout 2x4".equals(title)) {
                labels = new String[]{
                        "First item with longer text",
                        "Second item with longer text",
                        "Third item with longer text",
                        "Fourth item with longer text",
                        "Fifth item with longer text",
                        "Sixth item with longer text",
                        "Seventh item with longer text",
                        "Eighth item with longer text"
                };
            } else {
                labels = new String[rows * cols];
                for (int i = 0; i < labels.length; i++) {
                    labels[i] = "Cell " + (i + 1);
                }
            }
            for (int i = 0; i < labels.length; i++) {
                form.add(coloredLabel(labels[i], 0x1565c0 + ((i + 1) * 0x050505)));
            }
            return form;
        });
    }

    public static Demo gridAutoFit(String title, boolean landscape) {
        return formDemo(title, "GridLayout auto-fit", () -> {
            Form form = new Form(title, new BorderLayout());
            Container grid = new Container(new GridLayout(landscape ? 2 : 4, landscape ? 4 : 2));
            for (int i = 1; i <= 8; i++) {
                grid.add(coloredLabel("Item " + i, 0x00695c + (i * 0x030303)));
            }
            form.add(BorderLayout.CENTER, grid);
            return form;
        });
    }

    public static Demo buttonDemo() {
        return formDemo("Button", "Button component", () -> {
            Form form = simpleForm("Button");
            form.add(new Button("My Button"));
            return form;
        });
    }

    public static Demo linkButtonDemo() {
        return formDemo("Link Button", "Button styled as a link", () -> {
            Form form = simpleForm("Link Button");
            Button link = new Button("Open Account Settings");
            link.setUIID("Link");
            form.add(link);
            return form;
        });
    }

    public static Demo raisedFlatButtonsDemo() {
        return formDemo("Raised and Flat Buttons", "Button styles", () -> {
            Form form = simpleForm("Buttons");
            Button raised = new Button("Raised");
            raised.setUIID("RaisedButton");
            Button flat = new Button("Flat");
            form.add(raised).add(flat);
            return form;
        });
    }

    public static Demo radioCheckboxDemo() {
        return formDemo("RadioButton and CheckBox", "Selection controls", () -> {
            Form form = simpleForm("Selection");
            RadioButton first = new RadioButton("First Choice");
            RadioButton second = new RadioButton("Second Choice");
            new ButtonGroup(first, second);
            first.setSelected(true);
            CheckBox updates = new CheckBox("Enable Updates");
            updates.setSelected(true);
            form.add(first).add(second).add(updates);
            return form;
        });
    }

    public static Demo componentGroupDemo() {
        return formDemo("ComponentGroup", "Grouped controls", () -> {
            Form form = simpleForm("ComponentGroup");
            form.add(ComponentGroup.enclose(
                    new TextField("Ada", "First Name"),
                    new TextField("Lovelace", "Last Name"),
                    new TextField("ada@example.com", "Email")));
            return form;
        });
    }

    public static Demo multiButtonDemo() {
        return formDemo("MultiButton", "Multi-line buttons", () -> {
            Form form = simpleForm("MultiButton");
            for (int i = 1; i <= 3; i++) {
                MultiButton button = new MultiButton("Inbox item " + i);
                button.setTextLine2("Secondary text with a short summary");
                button.setTextLine3("Today");
                button.setMaterialIcon(FontImage.MATERIAL_MAIL, 4);
                form.add(button);
            }
            return form;
        });
    }

    public static Demo spanButtonDemo() {
        return formDemo("SpanButton", "Wrapped button text", () -> {
            Form form = simpleForm("SpanButton");
            SpanButton button = new SpanButton("A SpanButton wraps long text over several lines while remaining clickable.");
            button.setMaterialIcon(FontImage.MATERIAL_INFO, 4);
            form.add(button);
            return form;
        });
    }

    public static Demo spanLabelDemo() {
        return formDemo("SpanLabel", "Wrapped label text", () -> {
            Form form = simpleForm("SpanLabel");
            SpanLabel label = new SpanLabel("SpanLabel is useful when a label needs enough room to explain a result in normal prose.");
            label.setMaterialIcon(FontImage.MATERIAL_DESCRIPTION, 4);
            form.add(label);
            return form;
        });
    }

    public static Demo onOffSwitchDemo() {
        return formDemo("OnOffSwitch", "Switch states", () -> {
            Form form = simpleForm("OnOffSwitch");
            OnOffSwitch on = new OnOffSwitch();
            on.setValue(true);
            OnOffSwitch off = new OnOffSwitch();
            form.add(labeled("Enabled", on)).add(labeled("Disabled", off));
            return form;
        });
    }

    public static Demo tabsDemo(int selectedIndex) {
        String title = selectedIndex == 0 ? "Tabs" : "Swipeable Tabs Page " + selectedIndex;
        return formDemo(title, "Tabs component", () -> {
            Form form = new Form("Tabs", new BorderLayout());
            Tabs tabs = new Tabs();
            tabs.addTab("News", coloredPanel("News feed", 0xe3f2fd));
            tabs.addTab("Messages", coloredPanel("Messages", 0xe8f5e9));
            tabs.addTab("Settings", coloredPanel("Settings", 0xfff3e0));
            tabs.setSelectedIndex(selectedIndex, false);
            form.add(BorderLayout.CENTER, tabs);
            return form;
        });
    }

    public static Demo pickerDemo() {
        return formDemo("Picker", "Picker component", () -> {
            Form form = simpleForm("Picker");
            Picker datePicker = new Picker();
            datePicker.setType(Display.PICKER_TYPE_DATE);
            datePicker.setDate(new Date(1704067200000L));
            Picker timePicker = new Picker();
            timePicker.setType(Display.PICKER_TYPE_TIME);
            timePicker.setTime(10 * 60);
            Picker strings = new Picker();
            strings.setType(Display.PICKER_TYPE_STRINGS);
            strings.setStrings("Alpha", "Beta", "Gamma");
            strings.setSelectedString("Alpha");
            form.add(datePicker).add(timePicker).add(strings);
            return form;
        });
    }

    public static Demo floatingHintDemo() {
        return formDemo("FloatingHint", "Floating text fields", () -> {
            Form form = simpleForm("Floating Hint");
            TextField first = new TextField("", "First Field");
            TextField second = new TextField("", "Second Field");
            form.add(new FloatingHint(first));
            form.add(new FloatingHint(second));
            form.add(new Button("Go"));
            return form;
        });
    }

    public static Demo accordionDemo() {
        return formDemo("Accordion", "Accordion component", () -> {
            Form form = simpleForm("Accordion");
            Accordion accordion = new Accordion();
            accordion.addContent("Account", new SpanLabel("Email, password and profile details."));
            accordion.addContent("Notifications", new SpanLabel("Push, email and weekly summaries."));
            accordion.addContent("Billing", new SpanLabel("Invoices and payment methods."));
            form.add(accordion);
            return form;
        });
    }

    public static Demo floatingActionDemo(boolean badge) {
        return formDemo(badge ? "Badge Floating Button" : "Floating Action", "Floating action button", () -> {
            if (badge) {
                Form form = simpleForm("Badge");
                Button chat = new Button("Chat");
                FontImage.setMaterialIcon(chat, FontImage.MATERIAL_CHAT, 7);

                FloatingActionButton badgeButton = FloatingActionButton.createBadge("33");
                Container badgedChat = badgeButton.bindFabToContainer(chat, Component.RIGHT, Component.TOP);
                form.add(FlowLayout.encloseCenter(badgedChat));

                TextField changeBadgeValue = new TextField("33");
                changeBadgeValue.addDataChangedListener((type, index) -> {
                    badgeButton.setText(changeBadgeValue.getText());
                    badgeButton.getParent().revalidate();
                });
                form.add(changeBadgeValue);
                return form;
            }
            FloatingActionButton fab = FloatingActionButton.createFAB(FontImage.MATERIAL_ADD);
            FloatingActionButton people = fab.createSubFAB(FontImage.MATERIAL_PEOPLE, "");
            FloatingActionButton contacts = fab.createSubFAB(FontImage.MATERIAL_IMPORT_CONTACTS, "");

            Form form = new Form("Floating Action", new BorderLayout());
            Container content = new Container(new BorderLayout());
            content.setUIID("PaddedContainer");
            content.add(BorderLayout.NORTH, new Label("Expanded actions"));
            Container actions = new Container(BoxLayout.y());
            actions.add(FlowLayout.encloseRight(new Label("People"), people));
            actions.add(FlowLayout.encloseRight(new Label("Contacts"), contacts));
            actions.add(FlowLayout.encloseRight(fab));
            content.add(BorderLayout.CENTER, actions);
            form.add(BorderLayout.CENTER, content);
            return form;
        });
    }

    public static Demo splitPaneDemo() {
        return formDemo("SplitPane", "Split pane component", () -> {
            Label left = coloredLabel("Master", 0x0d47a1);
            Label right = coloredLabel("Detail", 0x00695c);
            Form form = new Form("SplitPane", new BorderLayout());
            form.add(BorderLayout.CENTER, new SplitPane(SplitPane.HORIZONTAL_SPLIT, left, right, "20%", "40%", "80%"));
            return form;
        });
    }

    public static Demo graphicsHiWorldDemo() {
        return formDemo("Hi World", "Graphics paint", () -> newPaintForm("Hi World", (g, cmp) -> {
            g.setColor(0xff0000);
            g.fillRect(cmp.getX(), cmp.getY(), cmp.getWidth(), cmp.getHeight());
            g.setColor(0xffffff);
            g.setFont(Font.createSystemFont(Font.FACE_SYSTEM, Font.STYLE_BOLD, Font.SIZE_LARGE));
            g.drawString("Hi World", cmp.getX() + 60, cmp.getY() + 90);
        }));
    }

    public static Demo graphicsGlassPaneDemo() {
        return formDemo("Glass Pane", "Glass pane drawing", () -> {
            Form form = simpleForm("Glass Pane");
            Style warningStyle = new Style(0xff0000, 0, Font.getDefaultFont(), (byte) 0);
            Image warningImage = FontImage.createMaterial(FontImage.MATERIAL_WARNING, warningStyle).toImage();
            TextField field = new TextField("My Field");
            field.getAllStyles().setMarginUnit(Style.UNIT_TYPE_DIPS);
            field.getAllStyles().setMargin(5, 5, 5, 5);
            form.add(field);
            form.setGlassPane((g, rect) -> {
                int x = field.getAbsoluteX() + field.getWidth() - warningImage.getWidth() / 2;
                int y = field.getAbsoluteY() + field.getHeight() / 2 - warningImage.getHeight() / 2;
                g.drawImage(warningImage, x, y);
            });
            return form;
        });
    }

    public static Demo shapedClippingDemo() {
        return formDemo("Shaped Clipping", "Graphics clipping", () -> newPaintForm("Shaped Clipping", (g, cmp) -> {
            GeneralPath path = new GeneralPath();
            int x = cmp.getX();
            int y = cmp.getY();
            int width = cmp.getWidth();
            int height = cmp.getHeight();
            int shapeWidth = Math.min(width - 120, 360);
            int shapeHeight = Math.min(height - 160, 720);
            int left = x + (width - shapeWidth) / 2;
            int top = y + 80;
            path.moveTo(left + shapeWidth * 2 / 5, top);
            path.lineTo(left + shapeWidth * 3 / 5, top);
            path.lineTo(left + shapeWidth * 3 / 5, top + shapeHeight);
            path.lineTo(left + shapeWidth * 2 / 5, top + shapeHeight);
            path.lineTo(left + shapeWidth * 2 / 5, top + shapeHeight / 6);
            path.lineTo(left + shapeWidth / 10, top + shapeHeight * 2 / 5);
            path.lineTo(left + shapeWidth / 10, top + shapeHeight / 4);
            path.lineTo(left + shapeWidth * 2 / 5, top);

            g.setColor(0xeeeeee);
            g.fillRect(x, y, width, height);
            g.setClip(path);
            g.drawImage(createGuideIcon(), left, top, shapeWidth, shapeHeight);
            g.setClip(path.getBounds());
            g.setColor(0x0d47a1);
            g.drawShape(path, new Stroke(2, Stroke.CAP_ROUND, Stroke.JOIN_ROUND, 4));
        }));
    }

    public static Demo fontImageDemo(String title, int mode) {
        return formDemo(title, "FontImage", () -> {
            Form form = simpleForm(title);
            Button button = new Button("Save");
            button.setMaterialIcon(FontImage.MATERIAL_SAVE, 5);
            if (mode == 1) {
                button.getAllStyles().setFgColor(0xd81b60);
            } else if (mode == 2) {
                button.setText("Material Save");
                button.getAllStyles().setFgColor(0x00796b);
            }
            form.add(button);
            return form;
        });
    }

    public static Demo sliderDemo() {
        return formDemo("Slider", "Slider values", () -> {
            Form form = new Form("Star Slider", BoxLayout.y());
            Slider slider = createStarRankSlider();
            slider.setProgress(5);
            form.add(FlowLayout.encloseCenter(slider));
            return form;
        });
    }

    public static Demo dialogDemo(String title, DialogCustomizer customizer) {
        return new FormDemo(title, "Dialog screenshot", () -> {
            Form form = simpleForm(title);
            form.add(centeredHint("Dialog host"));
            return form;
        }) {
            @Override
            public void show(Form parent) {
                Form form = createForm();
                addBack(form, parent);
                form.show();
                Display.getInstance().callSerially(() -> {
                    Dialog dialog = new Dialog("Title");
                    dialog.setLayout(new BorderLayout());
                    dialog.add(BorderLayout.CENTER, new SpanLabel("Dialog Body", "DialogBody"));
                    customizer.customize(dialog);
                });
            }
        };
    }

    public static Demo interactionDialogDemo() {
        return new FormDemo("Interaction Dialog", "InteractionDialog screenshot", () -> {
            Form form = new Form("Interaction Dialog", new BorderLayout());
            Container content = new Container(BoxLayout.y());
            content.setUIID("PaddedContainer");
            content.add(new Label("Form"));
            content.add(new Button("Refresh"));
            content.add(new Button("Details"));
            form.add(BorderLayout.WEST, content);
            return form;
        }) {
            @Override
            public void show(Form parent) {
                Form form = createForm();
                addBack(form, parent);
                form.show();
                Display.getInstance().callSerially(() -> {
                    InteractionDialog dialog = new InteractionDialog("Hello");
                    dialog.setAnimateShow(false);
                    dialog.setRepositionAnimation(false);
                    dialog.setLayout(new BorderLayout());
                    dialog.add(BorderLayout.CENTER, new Label("Hello Dialog"));
                    Button close = new Button("Close");
                    close.addActionListener(event -> dialog.dispose());
                    dialog.add(BorderLayout.SOUTH, close);
                    Dimension preferred = dialog.getContentPane().getPreferredSize();
                    int displayWidth = Display.getInstance().getDisplayWidth();
                    int dialogWidth = Math.max(preferred.getWidth() + preferred.getWidth() / 6, displayWidth * 2 / 3);
                    int left = displayWidth - dialogWidth;
                    dialog.show(0, 0, left, 0);
                });
            }
        };
    }

    public interface DialogCustomizer {
        void customize(Dialog dialog);
    }

    private interface FormFactory {
        Form create();
    }

    private interface Painter {
        void paint(Graphics graphics, Component component);
    }

    private static Demo formDemo(String title, String description, FormFactory factory) {
        return new FormDemo(title, description, factory);
    }

    private static class FormDemo implements Demo {
        private final String title;
        private final String description;
        private final FormFactory factory;

        FormDemo(String title, String description, FormFactory factory) {
            this.title = title;
            this.description = description;
            this.factory = factory;
        }

        @Override
        public String getTitle() {
            return title;
        }

        @Override
        public String getDescription() {
            return description;
        }

        @Override
        public void show(Form parent) {
            Form form = createForm();
            addBack(form, parent);
            form.show();
        }

        Form createForm() {
            return factory.create();
        }
    }

    private static Form simpleForm(String title) {
        Form form = new Form(title, BoxLayout.y());
        form.setScrollableY(true);
        return form;
    }

    private static Form newPaintForm(String title, Painter painter) {
        Form form = new Form(title, new BorderLayout());
        form.add(BorderLayout.CENTER, new Component() {
            @Override
            public void paint(Graphics graphics) {
                painter.paint(graphics, this);
            }
        });
        return form;
    }

    private static Container coloredPanel(String text, int color) {
        Container panel = new Container(new BorderLayout());
        panel.getAllStyles().setBgColor(color);
        panel.getAllStyles().setBgTransparency(255);
        panel.add(BorderLayout.CENTER, centeredHint(text));
        return panel;
    }

    private static Label coloredLabel(String text, int color) {
        Label label = new Label(text);
        label.setUIID("Label");
        label.getAllStyles().setFgColor(0xffffff);
        label.getAllStyles().setBgColor(color & 0xffffff);
        label.getAllStyles().setBgTransparency(255);
        label.getAllStyles().setPaddingUnit(Style.UNIT_TYPE_DIPS);
        label.getAllStyles().setPadding(4, 4, 4, 4);
        return label;
    }

    private static Label centeredHint(String text) {
        Label label = new Label(text);
        label.setAlignment(Component.CENTER);
        return label;
    }

    private static Container labeled(String text, Component component) {
        Container row = new Container(new BorderLayout());
        row.add(BorderLayout.CENTER, new Label(text));
        row.add(BorderLayout.EAST, component);
        return row;
    }

    private static void addTiles(Container container, String... labels) {
        int[] colors = {0x0d47a1, 0x00695c, 0x6a1b9a, 0xad1457, 0xef6c00};
        for (int i = 0; i < labels.length; i++) {
            container.add(coloredLabel(labels[i], colors[i % colors.length]));
        }
    }

    private static void addBack(Form form, Form parent) {
        if (parent != null) {
            form.getToolbar().addCommandToLeftBar("Back", null, ignored -> parent.showBack());
        }
    }

    private static Slider createStarRankSlider() {
        Slider starRank = new Slider();
        starRank.setEditable(true);
        starRank.setMinValue(0);
        starRank.setMaxValue(10);
        Font font = Font.createTrueTypeFont("native:MainLight", "native:MainLight").
                derive(Display.getInstance().convertToPixels(5, true), Font.STYLE_PLAIN);
        Style style = new Style(0xffff33, 0, font, (byte) 0);
        Image fullStar = FontImage.createMaterial(FontImage.MATERIAL_STAR, style).toImage();
        style.setOpacity(100);
        style.setFgColor(0);
        Image emptyStar = FontImage.createMaterial(FontImage.MATERIAL_STAR, style).toImage();
        initStarRankStyle(starRank.getSliderEmptySelectedStyle(), emptyStar);
        initStarRankStyle(starRank.getSliderEmptyUnselectedStyle(), emptyStar);
        initStarRankStyle(starRank.getSliderFullSelectedStyle(), fullStar);
        initStarRankStyle(starRank.getSliderFullUnselectedStyle(), fullStar);
        starRank.setPreferredSize(new Dimension(fullStar.getWidth() * 5, fullStar.getHeight()));
        return starRank;
    }

    private static void initStarRankStyle(Style style, Image star) {
        style.setBackgroundType(Style.BACKGROUND_IMAGE_TILE_BOTH);
        style.setBorder(Border.createEmpty());
        style.setBgImage(star);
        style.setBgTransparency(0);
    }

    private static Image createGuideIcon() {
        Image image = Image.createImage(50, 100, 0xffffffff);
        Graphics graphics = image.getGraphics();
        graphics.setAntiAliased(true);
        graphics.setColor(0x42a5f5);
        graphics.fillArc(6, 2, 38, 38, 0, 360);
        graphics.setColor(0xffca28);
        graphics.fillArc(12, 10, 8, 8, 0, 360);
        graphics.fillArc(30, 10, 8, 8, 0, 360);
        graphics.setColor(0x1e88e5);
        graphics.fillRoundRect(10, 38, 30, 50, 8, 8);
        graphics.setColor(0x0d47a1);
        graphics.drawLine(18, 88, 12, 98);
        graphics.drawLine(32, 88, 38, 98);
        return image;
    }
}
