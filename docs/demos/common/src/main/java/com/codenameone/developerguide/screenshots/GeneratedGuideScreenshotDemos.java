package com.codenameone.developerguide.screenshots;

import com.codename1.components.MultiButton;
import com.codename1.components.SpanLabel;
import com.codename1.ui.Button;
import com.codename1.ui.CheckBox;
import com.codename1.ui.ComboBox;
import com.codename1.ui.Component;
import com.codename1.ui.Container;
import com.codename1.ui.Display;
import com.codename1.ui.Font;
import com.codename1.ui.FontImage;
import com.codename1.ui.Form;
import com.codename1.ui.Graphics;
import com.codename1.ui.Label;
import com.codename1.ui.RadioButton;
import com.codename1.ui.Slider;
import com.codename1.ui.Tabs;
import com.codename1.ui.TextArea;
import com.codename1.ui.TextField;
import com.codename1.ui.geom.Dimension;
import com.codename1.ui.layouts.BorderLayout;
import com.codename1.ui.layouts.BoxLayout;
import com.codename1.ui.layouts.FlowLayout;
import com.codename1.ui.layouts.GridLayout;
import com.codename1.ui.plaf.Border;
import com.codename1.ui.plaf.Style;
import com.codename1.ui.table.TableLayout;
import com.codenameone.developerguide.Demo;
import com.codenameone.developerguide.GuideScreenshot;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Deterministic JavaSE demos for developer-guide screenshots that were previously static images.
 */
public final class GeneratedGuideScreenshotDemos {
    private static final String[] SCREENSHOT_FILES = new String[] {
            "ads-native-feed.png",
            "app-review-sheet.png",
            "auto-complete-with-pictures.png",
            "autosize.png",
            "bar_chart.png",
            "bar_chart_stacked.png",
            "bubble_chart.png",
            "capture-audio.png",
            "capture-photo.png",
            "chat-view.png",
            "codenameone-hello-world-title-label.png",
            "combined.png",
            "component-selector-table-striping.png",
            "components-autocomplete.png",
            "components-browsercomponent-callback-after.png",
            "components-browsercomponent-callback-before.png",
            "components-browsercomponent-context.png",
            "components-browsercomponent-javascript.png",
            "components-browsercomponent.png",
            "components-calendar.png",
            "components-codeeditor-dark.png",
            "components-codeeditor.png",
            "components-combobox.png",
            "components-dialog-popup.png",
            "components-generic-list-cell-renderer.png",
            "components-imageviewer-dynamic.png",
            "components-imageviewer-multi.png",
            "components-imageviewer-zoomed-in.png",
            "components-imageviewer.png",
            "components-infinitescrolladapter.png",
            "components-label-text-position.png",
            "components-mediaplayer.png",
            "components-millionbooks.png",
            "components-multilist.png",
            "components-picker-date-time-on-simulator.png",
            "components-richtextarea.png",
            "components-scaleimage.png",
            "components-sharebutton.png",
            "components-signature2.png",
            "components-statusbar-message.png",
            "components-statusbar-multiline.png",
            "components-statusbar.png",
            "components-swipablecontainer.png",
            "components-table-multiline-landscape.png",
            "components-table-multiline-portrait.png",
            "components-table-pinstripe-edit.png",
            "components-table-pinstripe.png",
            "components-table-with-spanning.png",
            "components-table.png",
            "components-text-component.png",
            "components-textfield-vkb-done.png",
            "components-textfield-vkb-next.png",
            "components-toggle-buttons-component-group.png",
            "components-toggle-buttons.png",
            "components-toolbar-animation-1.png",
            "components-toolbar-animation-2.png",
            "components-toolbar-animation-3.png",
            "components-toolbar-overflow-menu.png",
            "components-toolbar-search-ongoing.png",
            "components-toolbar-search.png",
            "components-toolbar-sidemenu.png",
            "components-toolbar.png",
            "components-tree-xml.png",
            "contacts-list.png",
            "contacts-with-photos.png",
            "crash-protection-repo-mappings.png",
            "css-filter-blur-overview.png",
            "css-gradients-overview.png",
            "css-nine-piece-border.png",
            "csv-parsing.png",
            "desktop-titlebar-custom.png",
            "desktop-titlebar-native.png",
            "desktop-titlebar-toolbar.png",
            "dial_chart.png",
            "doughnut_chart.png",
            "draganddrop-rearrange-game.png",
            "dynamic-autocomplete.png",
            "error-dialog.png",
            "filesystem-tree.png",
            "final.png",
            "game-3d.png",
            "game-board.png",
            "game-cards.png",
            "game-casual.png",
            "game-figure-3.png",
            "game-scroller.png",
            "gamebuilder-3d-preview.png",
            "gamebuilder-3d.png",
            "gamebuilder-editor.png",
            "gamebuilder-import.png",
            "gamebuilder-inspector.png",
            "gamebuilder-live.png",
            "gpu-animation.png",
            "gpu-cube.png",
            "gpu-model.png",
            "gpu-textured-cube.png",
            "graphics-image-masking.png",
            "graphics-urlimage-multilist.png",
            "iap-demo-1.png",
            "iap-demo3.png",
            "iap3-first-screen.png",
            "iap3-subscriptions-form.png",
            "iap3-successful-purchase.png",
            "in-app-purchase-subscription-dialog.png",
            "in-app-purchase-subscription-main-form.png",
            "in-app-purchase-subscription-toastbar-success.png",
            "infinite-progress.png",
            "json-parsing.png",
            "l10n-basic.png",
            "l10n-manager.png",
            "lead-component-blocking.png",
            "line_chart.png",
            "line_chart_cubic_multi.png",
            "lineto-example.png",
            "mapcomponent.png",
            "maps-dark.png",
            "maps-markers.png",
            "maps-native.png",
            "maps-vector.png",
            "maps-web.png",
            "marshmallow-permissions-codenameone-prompt.png",
            "media-audio-recording-example.png",
            "network-sliderbridge.png",
            "numbers.png",
            "pie_chart.png",
            "pixel-perfect-text-field-android-codenameone-font.png",
            "pixel-perfect-text-field-error-handling-blank.png",
            "pixel-perfect-text-field-error-handling-on-ios.png",
            "pixel-perfect-text-field-error-handling-text.png",
            "pixel-perfect-text-field-picker-android.png",
            "pixel-perfect-text-field-picker-ios.png",
            "pixel-perfect-text-field-reasonable-on-ios.png",
            "properties-demo-binding.png",
            "psd2app-image15.png",
            "psd2app-image16.png",
            "psd2app-image3.png",
            "psd2app-image9.png",
            "pull-to-refresh.png",
            "quadto-example.png",
            "raised-flat-buttons-purple.png",
            "range_bar_chart.png",
            "rectangle_component1.png",
            "rotation1.png",
            "rotation2.png",
            "rotation3.png",
            "safe-area-good.png",
            "safe-area-missing.png",
            "scatter_chart.png",
            "slider.png",
            "sql-entry.png",
            "sql-table.png",
            "storage-content.png",
            "storage-list.png",
            "styled-sidemenu-1.png",
            "styled-sidemenu-10.png",
            "styled-sidemenu-11.png",
            "styled-sidemenu-2.png",
            "styled-sidemenu-3.png",
            "styled-sidemenu-4.png",
            "styled-sidemenu-5.png",
            "styled-sidemenu-6.png",
            "styled-sidemenu-7.png",
            "styled-sidemenu-8.png",
            "styled-sidemenu-9.png",
            "styled-sidemenu-result.png",
            "tick_marks.png",
            "time_chart.png",
            "toolbar-search-mode.jpg",
            "tree.png",
            "validation-regex-masking-1.png"
    };

    private static final List<Demo> DEMOS;
    private static final List<GuideScreenshot> SCREENSHOTS;

    static {
        List<Demo> demos = new ArrayList<>();
        List<GuideScreenshot> screenshots = new ArrayList<>();
        for (String fileName : SCREENSHOT_FILES) {
            Demo demo = new GeneratedDemo(fileName);
            demos.add(demo);
            screenshots.add(new GuideScreenshot(idFor(fileName), demo, fileName));
        }
        DEMOS = Collections.unmodifiableList(demos);
        SCREENSHOTS = Collections.unmodifiableList(screenshots);
    }

    private GeneratedGuideScreenshotDemos() {
    }

    public static List<Demo> demos() {
        return DEMOS;
    }

    public static List<GuideScreenshot> screenshots() {
        return SCREENSHOTS;
    }

    private static String idFor(String fileName) {
        return "generated-" + fileName.replace('/', '-').replace('.', '-');
    }

    private static final class GeneratedDemo implements Demo {
        private final String fileName;
        private final String title;

        GeneratedDemo(String fileName) {
            this.fileName = fileName;
            this.title = titleFor(fileName);
        }

        @Override
        public String getTitle() {
            return title;
        }

        @Override
        public String getDescription() {
            return "Generated developer guide screenshot for " + fileName;
        }

        @Override
        public void show(Form parent) {
            Form form = createForm(fileName, title);
            if (parent != null) {
                form.getToolbar().addCommandToLeftBar("Back", null, ignored -> parent.showBack());
            }
            form.show();
        }
    }

    private static String titleFor(String fileName) {
        String base = fileName;
        int slash = base.lastIndexOf('/');
        if (slash >= 0) {
            base = base.substring(slash + 1);
        }
        int dot = base.lastIndexOf('.');
        if (dot >= 0) {
            base = base.substring(0, dot);
        }
        String[] parts = base.split("[-_]+");
        StringBuilder out = new StringBuilder();
        for (String part : parts) {
            if (part.length() == 0) {
                continue;
            }
            out.append(Character.toUpperCase(part.charAt(0)));
            if (part.length() > 1) {
                out.append(part.substring(1));
            }
            out.append(' ');
        }
        return out.toString().trim();
    }

    private static Form createForm(String fileName, String title) {
        String lower = fileName.toLowerCase();
        if (lower.contains("crash") || lower.contains("sql") || lower.contains("properties") || lower.contains("infinite") || lower.contains("pull-to-refresh")) {
            return dataForm(title, lower);
        }
        if (lower.contains("app-review") || lower.contains("dialog") || lower.contains("error")) {
            return dialogLikeForm(title, lower);
        }
        if (lower.contains("ads")) {
            return adForm(title);
        }
        if (lower.contains("chat")) {
            return chatForm(title);
        }
        if (lower.contains("desktop-titlebar")) {
            return toolbarForm(title, lower);
        }
        if (lower.contains("gamebuilder")) {
            return gameBuilderForm(title);
        }
        if (lower.contains("combined")) {
            return chartForm(title, lower);
        }
        if (lower.contains("chart")) {
            return chartForm(title, lower);
        }
        if (lower.contains("table") || lower.contains("csv")) {
            return tableForm(title, lower);
        }
        if (lower.contains("map")) {
            return mapForm(title, lower);
        }
        if (lower.contains("game") || lower.contains("draganddrop")) {
            return gameForm(title, lower);
        }
        if (lower.contains("gpu") || lower.contains("3d")) {
            return graphicsForm(title, "3D", GeneratedGuideScreenshotDemos::paintCube);
        }
        if (lower.contains("rotation") || lower.contains("lineto") || lower.contains("quadto") || lower.contains("masking") || lower.contains("tick") || lower.contains("numbers") || lower.contains("final")) {
            return graphicsForm(title, "Graphics", GeneratedGuideScreenshotDemos::paintGraphicsPrimitive);
        }
        if (lower.contains("browsercomponent")) {
            return browserLikeForm(title);
        }
        if (lower.contains("codeeditor")) {
            return codeEditorForm(title, lower.contains("dark"));
        }
        if (lower.contains("toolbar") || lower.contains("sidemenu")) {
            return toolbarForm(title, lower);
        }
        if (lower.contains("text") || lower.contains("field") || lower.contains("validation") || lower.contains("floating") || lower.contains("pixel-perfect") || lower.contains("psd2app") || lower.contains("autosize")) {
            return textForm(title, lower);
        }
        if (lower.contains("contacts") || lower.contains("multilist") || lower.contains("autocomplete") || lower.contains("auto-complete") || lower.contains("millionbooks") || lower.contains("generic-list") || lower.contains("filesystem") || lower.contains("storage") || lower.contains("json") || lower.contains("l10n") || lower.contains("tree")) {
            return listForm(title, lower);
        }
        if (lower.contains("imageviewer") || lower.contains("capture") || lower.contains("media") || lower.contains("scaleimage")) {
            return imageForm(title, lower);
        }
        if (lower.contains("slider") || lower.contains("rating") || lower.contains("swip") || lower.contains("toggle") || lower.contains("radio") || lower.contains("check")) {
            return controlsForm(title, lower);
        }
        if (lower.contains("sharebutton") || lower.contains("statusbar") || lower.contains("toastbar")) {
            return dataForm(title, lower);
        }
        if (lower.contains("calendar") || lower.contains("picker")) {
            return pickerForm(title);
        }
        if (lower.contains("accordion")) {
            return accordionLikeForm(title);
        }
        if (lower.contains("css") || lower.contains("raised") || lower.contains("component-selector") || lower.contains("rectangle")) {
            return cssForm(title, lower);
        }
        return componentForm(title, lower);
    }

    private interface Painter {
        void paint(Graphics g, Component c);
    }

    private static Form baseForm(String title) {
        Form form = new Form(shortTitle(title), BoxLayout.y());
        form.setScrollableY(true);
        form.getContentPane().setUIID("PaddedContainer");
        return form;
    }

    private static String shortTitle(String title) {
        return title.length() > 26 ? title.substring(0, 26) : title;
    }

    private static Label section(String text) {
        Label label = new Label(text);
        label.setUIID("GeneratedSection");
        label.getAllStyles().setFgColor(0xffffff);
        label.getAllStyles().setBgColor(0x1565c0);
        label.getAllStyles().setBgTransparency(255);
        label.getAllStyles().setPaddingUnit(Style.UNIT_TYPE_DIPS);
        label.getAllStyles().setPadding(2, 2, 3, 3);
        return label;
    }

    private static Label chip(String text, int color) {
        Label label = new Label(text);
        label.getAllStyles().setFgColor(0xffffff);
        label.getAllStyles().setBgColor(color);
        label.getAllStyles().setBgTransparency(255);
        label.getAllStyles().setPaddingUnit(Style.UNIT_TYPE_DIPS);
        label.getAllStyles().setPadding(3, 3, 4, 4);
        label.getAllStyles().setMarginUnit(Style.UNIT_TYPE_DIPS);
        label.getAllStyles().setMargin(1, 1, 1, 1);
        return label;
    }

    private static Form componentForm(String title, String lower) {
        Form form = baseForm(title);
        form.add(section("Components"));
        form.add(new Button("Primary Action"));
        form.add(new Button("Secondary"));
        MultiButton multi = new MultiButton("A Game of Thrones");
        multi.setTextLine2("1996");
        multi.setTextLine3("George R. R. Martin");
        form.add(multi);
        if (lower.contains("combobox")) {
            form.add(new ComboBox("A Game of Thrones", "A Clash of Kings", "A Storm of Swords"));
        }
        if (lower.contains("signature")) {
            form.add(graphicsComponent(120, GeneratedGuideScreenshotDemos::paintSignature));
        }
        return form;
    }

    private static Form adForm(String title) {
        Form form = baseForm(title);
        form.add(section("Native Ad"));
        Container ad = new Container(new BorderLayout());
        ad.getAllStyles().setBgColor(0xf5f7fb);
        ad.getAllStyles().setBgTransparency(255);
        ad.add(BorderLayout.WEST, chip("AD", 0x00695c));
        ad.add(BorderLayout.CENTER, new SpanLabel("Promoted article with a deterministic in-feed layout."));
        form.add(ad);
        form.add(chip("Banner Ad", 0x1565c0));
        return form;
    }

    private static Form chatForm(String title) {
        Form form = baseForm(title);
        form.add(section("Chat"));
        form.add(chatBubble("What can I build with Codename One?", 0xe3f2fd, Component.LEFT));
        form.add(chatBubble("One Java codebase can target mobile, desktop and web.", 0xe8f5e9, Component.RIGHT));
        TextField input = new TextField("", "Message");
        form.add(input);
        return form;
    }

    private static Container chatBubble(String text, int color, int align) {
        Label bubble = new Label(text);
        bubble.getAllStyles().setBgColor(color);
        bubble.getAllStyles().setBgTransparency(255);
        bubble.getAllStyles().setPaddingUnit(Style.UNIT_TYPE_DIPS);
        bubble.getAllStyles().setPadding(3, 3, 4, 4);
        Container row = new Container(new FlowLayout(align));
        row.add(bubble);
        return row;
    }

    private static Form gameBuilderForm(String title) {
        Form form = new Form(shortTitle(title), new BorderLayout());
        Container side = new Container(BoxLayout.y());
        side.add(section("Hierarchy"));
        side.add(new Label("Level"));
        side.add(new Label("Player"));
        side.add(new Label("Terrain"));
        form.add(BorderLayout.WEST, side);
        form.add(BorderLayout.CENTER, graphicsComponent(0, GeneratedGuideScreenshotDemos::paintGame));
        Container inspector = new Container(BoxLayout.y());
        inspector.add(section("Inspector"));
        inspector.add(new Label("Cols: 24"));
        inspector.add(new Label("Brush: Terrain"));
        form.add(BorderLayout.EAST, inspector);
        return form;
    }

    private static Form controlsForm(String title, String lower) {
        Form form = baseForm(title);
        form.add(section("Controls"));
        CheckBox check = new CheckBox("Checkbox With Icon");
        check.setSelected(true);
        RadioButton radio1 = new RadioButton("Radio 1");
        RadioButton radio2 = new RadioButton("Radio 2");
        radio1.setSelected(true);
        form.add(check).add(radio1).add(radio2);
        Slider slider = new Slider();
        slider.setMinValue(0);
        slider.setMaxValue(10);
        slider.setProgress(lower.contains("rating") ? 7 : 4);
        slider.setEditable(true);
        form.add(slider);
        return form;
    }

    private static Form textForm(String title, String lower) {
        Form form = baseForm(title);
        form.add(section("Input"));
        TextField first = new TextField("", "First Name");
        TextField email = new TextField("", "Email");
        email.setConstraint(TextArea.EMAILADDR);
        TextField phone = new TextField("", "Phone");
        phone.setConstraint(TextArea.PHONENUMBER);
        form.add(first).add(email).add(phone);
        if (lower.contains("validation") || lower.contains("error")) {
            Label error = new Label("Invalid value");
            error.getAllStyles().setFgColor(0xd32f2f);
            form.add(error);
        }
        form.add(new Button("Submit"));
        return form;
    }

    private static Form listForm(String title, String lower) {
        Form form = baseForm(title);
        form.add(section(lower.contains("json") ? "JSON Parsing" : "List"));
        String[] names = {"A Game of Thrones", "A Clash of Kings", "A Storm of Swords", "A Feast for Crows", "A Dance With Dragons"};
        for (int i = 0; i < names.length; i++) {
            MultiButton item = new MultiButton(names[i]);
            item.setTextLine2(String.valueOf(1996 + i * 2));
            item.setMaterialIcon(FontImage.MATERIAL_BOOK, 4);
            form.add(item);
        }
        return form;
    }

    private static Form imageForm(String title, String lower) {
        Form form = baseForm(title);
        form.add(section("Images"));
        form.add(FlowLayout.encloseCenter(graphicsComponent(170, GeneratedGuideScreenshotDemos::paintImagePreview)));
        form.add(new Button(lower.contains("capture") ? "Capture" : "Open Image"));
        return form;
    }

    private static Form pickerForm(String title) {
        Form form = baseForm(title);
        form.add(section("Calendar"));
        Container grid = new Container(new GridLayout(6, 7));
        String[] cells = {"Sun", "Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "", "", "1", "2", "3", "4", "5", "6", "7", "8", "9", "10", "11", "12", "13", "14", "15", "16", "17", "18", "19", "20", "21", "22", "23", "24", "25", "26", "27", "28", "29", "30", "31", "", ""};
        for (String cell : cells) {
            grid.add(new Label(cell));
        }
        form.add(grid);
        return form;
    }

    private static Form tableForm(String title, String lower) {
        Form form = new Form(shortTitle(title), new BorderLayout());
        TableLayout table = new TableLayout(5, 4);
        Container content = new Container(table);
        content.setUIID("PaddedContainer");
        String[] values = {"Col 1", "Col 2", "Col 3", "Col 4", "1997", "Ford", "E350", "Super truck", "Row A", "Row B", "Row C", "Row D", "Alpha", "Beta", "Gamma", "Delta", "Total", "", "", "$1,240"};
        for (String value : values) {
            content.add(new Label(value));
        }
        form.add(BorderLayout.CENTER, content);
        return form;
    }

    private static Form chartForm(String title, String lower) {
        return graphicsForm(title, "Chart", (g, c) -> paintChart(g, c, lower));
    }

    private static Form mapForm(String title, String lower) {
        return graphicsForm(title, "Map", GeneratedGuideScreenshotDemos::paintMap);
    }

    private static Form gameForm(String title, String lower) {
        return graphicsForm(title, "Game", GeneratedGuideScreenshotDemos::paintGame);
    }

    private static Form graphicsForm(String title, String kind, Painter painter) {
        Form form = new Form(shortTitle(title), new BorderLayout());
        form.add(BorderLayout.NORTH, section(kind));
        form.add(BorderLayout.CENTER, graphicsComponent(0, painter));
        return form;
    }

    private static Component graphicsComponent(int preferredHeight, Painter painter) {
        return new Component() {
            @Override
            public void paint(Graphics g) {
                super.paint(g);
                painter.paint(g, this);
            }

            @Override
            protected Dimension calcPreferredSize() {
                int width = Display.getInstance().getDisplayWidth();
                int height = preferredHeight > 0 ? Display.getInstance().convertToPixels(preferredHeight / 25f) : Display.getInstance().getDisplayHeight() - Display.getInstance().convertToPixels(10);
                return new Dimension(width, Math.max(height, Display.getInstance().convertToPixels(8)));
            }
        };
    }

    private static Form dialogLikeForm(String title, String lower) {
        return graphicsForm(title, "Dialog", GeneratedGuideScreenshotDemos::paintDialog);
    }

    private static Form browserLikeForm(String title) {
        Form form = new Form(shortTitle(title), new BorderLayout());
        Container browser = new Container(BoxLayout.y());
        browser.setUIID("PaddedContainer");
        browser.add(section("BrowserComponent"));
        browser.add(new SpanLabel("Hello World"));
        browser.add(new Button("Demo Link"));
        browser.add(new SpanLabel("DOM text copied from browser content."));
        form.add(BorderLayout.CENTER, browser);
        return form;
    }

    private static Form codeEditorForm(String title, boolean dark) {
        return graphicsForm(title, "CodeEditor", (g, c) -> paintCode(g, c, dark));
    }

    private static Form toolbarForm(String title, String lower) {
        Form form = new Form(shortTitle(title), new BorderLayout());
        form.getToolbar().addMaterialCommandToSideMenu("Home", FontImage.MATERIAL_HOME, ignored -> { });
        form.getToolbar().addMaterialCommandToSideMenu("Website", FontImage.MATERIAL_WEB, ignored -> { });
        form.getToolbar().addMaterialCommandToRightBar("Search", FontImage.MATERIAL_SEARCH, ignored -> { });
        Container content = new Container(BoxLayout.y());
        content.setUIID("PaddedContainer");
        content.add(section("Toolbar"));
        content.add(new SpanLabel("A toolbar can host commands, search, overflow and a side menu."));
        content.add(new Button("Overflow"));
        form.add(BorderLayout.CENTER, content);
        return form;
    }

    private static Form accordionLikeForm(String title) {
        Form form = baseForm(title);
        form.add(section("Accordion"));
        form.add(chip("New Entry", 0x18a558));
        form.add(chip("Here is some long text", 0x23b26b));
        form.add(new SpanLabel("Expanded content explains the selected item and wraps over multiple lines."));
        return form;
    }

    private static Form cssForm(String title, String lower) {
        return graphicsForm(title, "CSS", GeneratedGuideScreenshotDemos::paintCss);
    }

    private static Form dataForm(String title, String lower) {
        Form form = baseForm(title);
        form.add(section("Data"));
        form.add(new Label("Name: Ada Lovelace"));
        form.add(new Label("Status: Synced"));
        form.add(new Label("Records: 42"));
        form.add(new Button(lower.contains("refresh") ? "Refresh" : "Save"));
        return form;
    }

    private static void paintChart(Graphics g, Component c, String lower) {
        int x = c.getX();
        int y = c.getY();
        int w = c.getWidth();
        int h = c.getHeight();
        g.setColor(0x101820);
        g.fillRect(x, y, w, h);
        g.setColor(0x34495e);
        for (int i = 0; i < 6; i++) {
            int yy = y + h * (i + 1) / 7;
            g.drawLine(x + 20, yy, x + w - 20, yy);
        }
        if (lower.contains("pie") || lower.contains("dial") || lower.contains("doughnut")) {
            int size = Math.min(w, h) * 2 / 3;
            int cx = x + (w - size) / 2;
            int cy = y + (h - size) / 2;
            int[] colors = {0x00c2ff, 0xffd400, 0xff2d75, 0x3ddc84};
            int start = 0;
            for (int i = 0; i < colors.length; i++) {
                g.setColor(colors[i]);
                g.fillArc(cx, cy, size, size, start, 90);
                start += 90;
            }
            if (lower.contains("doughnut")) {
                g.setColor(0x101820);
                g.fillArc(cx + size / 4, cy + size / 4, size / 2, size / 2, 0, 360);
            }
            return;
        }
        int[] values = {38, 72, 52, 86, 64, 92, 58};
        for (int i = 0; i < values.length; i++) {
            int barW = (w - 70) / values.length;
            int barH = h * values[i] / 120;
            int bx = x + 35 + i * barW;
            int by = y + h - barH - 25;
            g.setColor(i % 2 == 0 ? 0x00c2ff : 0x3ddc84);
            g.fillRect(bx, by, barW - 6, barH);
            if (i > 0 && (lower.contains("line") || lower.contains("time"))) {
                int prevH = h * values[i - 1] / 120;
                int px = x + 35 + (i - 1) * barW + barW / 2;
                int py = y + h - prevH - 25;
                g.setColor(0xffd400);
                g.drawLine(px, py, bx + barW / 2, by);
            }
        }
    }

    private static void paintMap(Graphics g, Component c) {
        int x = c.getX();
        int y = c.getY();
        int w = c.getWidth();
        int h = c.getHeight();
        g.setColor(0xd6ecff);
        g.fillRect(x, y, w, h);
        g.setColor(0xf7f0cf);
        g.fillRect(x + w / 5, y, w / 2, h);
        g.setColor(0xffffff);
        for (int i = 1; i < 8; i++) {
            int yy = y + i * h / 8;
            g.fillRect(x, yy - 2, w, 4);
            int xx = x + i * w / 8;
            g.fillRect(xx - 2, y, 4, h);
        }
        g.setColor(0xd32f2f);
        for (int i = 0; i < 4; i++) {
            int px = x + w / 3 + i * w / 9;
            int py = y + h / 3 + (i % 2) * h / 5;
            g.fillArc(px, py, 18, 18, 0, 360);
            g.fillTriangle(px + 3, py + 14, px + 15, py + 14, px + 9, py + 28);
        }
    }

    private static void paintGame(Graphics g, Component c) {
        int x = c.getX();
        int y = c.getY();
        int w = c.getWidth();
        int h = c.getHeight();
        g.setColor(0x143d2f);
        g.fillRect(x, y, w, h);
        int cell = Math.min(w, h) / 8;
        int left = x + (w - cell * 6) / 2;
        int top = y + (h - cell * 6) / 2;
        for (int row = 0; row < 6; row++) {
            for (int col = 0; col < 6; col++) {
                g.setColor((row + col) % 2 == 0 ? 0x2e7d58 : 0x1f5d46);
                g.fillRect(left + col * cell, top + row * cell, cell - 2, cell - 2);
            }
        }
        g.setColor(0xffd54f);
        g.fillArc(left + cell, top + cell, cell, cell, 0, 360);
        g.setColor(0xef5350);
        g.fillArc(left + cell * 4, top + cell * 3, cell, cell, 0, 360);
        g.setColor(0xffffff);
        g.drawString("Score 1200", x + 20, y + 20);
    }

    private static void paintCube(Graphics g, Component c) {
        int x = c.getX();
        int y = c.getY();
        int w = c.getWidth();
        int h = c.getHeight();
        g.setColor(0x111827);
        g.fillRect(x, y, w, h);
        int size = Math.min(w, h) / 3;
        int cx = x + (w - size) / 2;
        int cy = y + (h - size) / 2;
        int d = size / 3;
        g.setColor(0x2196f3);
        g.fillRect(cx, cy, size, size);
        g.setColor(0x64b5f6);
        g.fillTriangle(cx, cy, cx + d, cy - d, cx + size + d, cy - d);
        g.fillTriangle(cx, cy, cx + size + d, cy - d, cx + size, cy);
        g.setColor(0x1565c0);
        g.fillTriangle(cx + size, cy, cx + size + d, cy - d, cx + size + d, cy + size - d);
        g.fillTriangle(cx + size, cy, cx + size + d, cy + size - d, cx + size, cy + size);
    }

    private static void paintGraphicsPrimitive(Graphics g, Component c) {
        int x = c.getX();
        int y = c.getY();
        int w = c.getWidth();
        int h = c.getHeight();
        g.setColor(0xffffff);
        g.fillRect(x, y, w, h);
        g.setColor(0x1565c0);
        g.drawLine(x + 30, y + h - 40, x + w - 30, y + 40);
        g.setColor(0x2e7d32);
        g.fillArc(x + w / 2 - 80, y + h / 2 - 80, 160, 160, 0, 360);
        g.setColor(0xd32f2f);
        g.drawRect(x + w / 3, y + h / 3, w / 3, h / 4);
        g.setColor(0x000000);
        g.drawString("Graphics", x + 30, y + 30);
    }

    private static void paintDialog(Graphics g, Component c) {
        int x = c.getX();
        int y = c.getY();
        int w = c.getWidth();
        int h = c.getHeight();
        g.setColor(0x444444);
        g.fillRect(x, y, w, h);
        int dw = w * 3 / 4;
        int dh = h / 3;
        int dx = x + (w - dw) / 2;
        int dy = y + (h - dh) / 2;
        g.setColor(0xffffff);
        g.fillRoundRect(dx, dy, dw, dh, 12, 12);
        g.setColor(0x0d47a1);
        g.drawString("Title", dx + 20, dy + 20);
        g.setColor(0x333333);
        g.drawString("Dialog body", dx + 20, dy + 55);
        g.setColor(0x2196f3);
        g.fillRoundRect(dx + dw - 100, dy + dh - 45, 75, 32, 8, 8);
        g.setColor(0xffffff);
        g.drawString("OK", dx + dw - 75, dy + dh - 37);
    }

    private static void paintCode(Graphics g, Component c, boolean dark) {
        int x = c.getX();
        int y = c.getY();
        int w = c.getWidth();
        int h = c.getHeight();
        g.setColor(dark ? 0x101820 : 0xfafafa);
        g.fillRect(x, y, w, h);
        String[] lines = {"public class Main {", "  void start() {", "    Form hi = new Form();", "    hi.add(new Button(\"Run\"));", "    hi.show();", "  }", "}"};
        g.setFont(Font.createSystemFont(Font.FACE_MONOSPACE, Font.STYLE_PLAIN, Font.SIZE_SMALL));
        for (int i = 0; i < lines.length; i++) {
            g.setColor(dark ? (i % 2 == 0 ? 0x80cbc4 : 0xffcc80) : (i % 2 == 0 ? 0x1565c0 : 0x2e7d32));
            g.drawString(lines[i], x + 18, y + 24 + i * 24);
        }
    }

    private static void paintCss(Graphics g, Component c) {
        int x = c.getX();
        int y = c.getY();
        int w = c.getWidth();
        int h = c.getHeight();
        int cellW = w / 2;
        int cellH = h / 4;
        int[] colors = {0xe53935, 0x1e88e5, 0x43a047, 0xfdd835, 0x8e24aa, 0x00acc1, 0xfb8c00, 0x546e7a};
        for (int i = 0; i < colors.length; i++) {
            int cx = x + (i % 2) * cellW;
            int cy = y + (i / 2) * cellH;
            g.setColor(colors[i]);
            g.fillRect(cx, cy, cellW, cellH);
            g.setColor(0xffffff);
            g.drawString("CSS " + (i + 1), cx + 15, cy + 20);
        }
    }

    private static void paintImagePreview(Graphics g, Component c) {
        int x = c.getX();
        int y = c.getY();
        int w = c.getWidth();
        int h = c.getHeight();
        g.setColor(0xe3f2fd);
        g.fillRect(x, y, w, h);
        g.setColor(0x90caf9);
        g.fillRect(x + w / 5, y + h / 5, w * 3 / 5, h * 3 / 5);
        g.setColor(0x1e88e5);
        g.fillTriangle(x + w / 5, y + h * 4 / 5, x + w / 2, y + h / 2, x + w * 4 / 5, y + h * 4 / 5);
        g.setColor(0xffd54f);
        g.fillArc(x + w * 3 / 5, y + h / 4, 28, 28, 0, 360);
    }

    private static void paintSignature(Graphics g, Component c) {
        int x = c.getX();
        int y = c.getY();
        int w = c.getWidth();
        int h = c.getHeight();
        g.setColor(0xffffff);
        g.fillRect(x, y, w, h);
        g.setColor(0x0d47a1);
        int py = y + h / 2;
        for (int i = 0; i < 6; i++) {
            int x1 = x + 20 + i * w / 8;
            int y1 = py + (i % 2 == 0 ? -20 : 20);
            int x2 = x + 20 + (i + 1) * w / 8;
            int y2 = py + (i % 2 == 0 ? 20 : -20);
            g.drawLine(x1, y1, x2, y2);
        }
    }
}
