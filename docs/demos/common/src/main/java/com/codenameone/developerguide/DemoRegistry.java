package com.codenameone.developerguide;

import com.codenameone.developerguide.animations.AnimationManagerDemo;
import com.codenameone.developerguide.animations.AnimationSynchronicityDemo;
import com.codenameone.developerguide.animations.BubbleTransitionDemo;
import com.codenameone.developerguide.animations.HiddenComponentDemo;
import com.codenameone.developerguide.animations.HierarchyAnimationDemo;
import com.codenameone.developerguide.animations.LayoutAnimationsDemo;
import com.codenameone.developerguide.animations.LowLevelAnimationDemo;
import com.codenameone.developerguide.animations.MorphTransitionDemo;
import com.codenameone.developerguide.animations.ReplaceTransitionDemo;
import com.codenameone.developerguide.animations.SlideTransitionsDemo;
import com.codenameone.developerguide.animations.SwipeBackSupportDemo;
import com.codenameone.developerguide.animations.UnlayoutAnimationsDemo;
import com.codenameone.developerguide.screenshots.GuideStaticScreenshotDemos;
import com.codename1.ui.Component;
import com.codename1.ui.Display;
import com.codename1.ui.layouts.BorderLayout;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * Registry of available demos so that the browser can enumerate them.
 */
public final class DemoRegistry {
    private static final List<Demo> DEMOS = Collections.unmodifiableList(
            Arrays.asList(
                    new LayoutAnimationsDemo(),
                    new UnlayoutAnimationsDemo(),
                    new HiddenComponentDemo(),
                    new AnimationSynchronicityDemo(),
                    new HierarchyAnimationDemo(),
                    new AnimationManagerDemo(),
                    new LowLevelAnimationDemo(),
                    new ReplaceTransitionDemo(),
                    new SlideTransitionsDemo(),
                    new BubbleTransitionDemo(),
                    new MorphTransitionDemo(),
                    new SwipeBackSupportDemo(),
                    GuideStaticScreenshotDemos.flowLayout("Flow Layout", Component.LEFT, Component.TOP),
                    GuideStaticScreenshotDemos.flowLayout("Flow Layout Center", Component.CENTER, Component.TOP),
                    GuideStaticScreenshotDemos.flowLayout("Flow Layout Right", Component.RIGHT, Component.TOP),
                    GuideStaticScreenshotDemos.flowLayout("Flow Layout Center Middle", Component.CENTER, Component.CENTER),
                    GuideStaticScreenshotDemos.boxLayoutY(),
                    GuideStaticScreenshotDemos.boxLayoutX(false),
                    GuideStaticScreenshotDemos.boxLayoutX(true),
                    GuideStaticScreenshotDemos.borderLayout("Border Layout", false, false),
                    GuideStaticScreenshotDemos.borderLayout("Border Layout Center", true, false),
                    GuideStaticScreenshotDemos.borderLayout("Border Layout RTL", false, true),
                    GuideStaticScreenshotDemos.gridLayout("Grid Layout 2x2", 2, 2),
                    GuideStaticScreenshotDemos.gridLayout("Grid Layout 2x4", 2, 4),
                    GuideStaticScreenshotDemos.gridAutoFit("Grid AutoFit Portrait", false),
                    GuideStaticScreenshotDemos.gridAutoFit("Grid AutoFit Landscape", true),
                    GuideStaticScreenshotDemos.tableLayoutOverflow(),
                    GuideStaticScreenshotDemos.tableLayoutEnclose(),
                    GuideStaticScreenshotDemos.tableLayoutConstraints(),
                    GuideStaticScreenshotDemos.buttonDemo(),
                    GuideStaticScreenshotDemos.linkButtonDemo(),
                    GuideStaticScreenshotDemos.raisedFlatButtonsDemo(),
                    GuideStaticScreenshotDemos.radioCheckboxDemo(),
                    GuideStaticScreenshotDemos.componentGroupDemo(),
                    GuideStaticScreenshotDemos.multiButtonDemo(),
                    GuideStaticScreenshotDemos.spanButtonDemo(),
                    GuideStaticScreenshotDemos.spanLabelDemo(),
                    GuideStaticScreenshotDemos.onOffSwitchDemo(),
                    GuideStaticScreenshotDemos.tabsDemo(0),
                    GuideStaticScreenshotDemos.tabsDemo(1),
                    GuideStaticScreenshotDemos.tabsDemo(2),
                    GuideStaticScreenshotDemos.pickerDemo(),
                    GuideStaticScreenshotDemos.floatingHintDemo(),
                    GuideStaticScreenshotDemos.accordionDemo(),
                    GuideStaticScreenshotDemos.floatingActionDemo(false),
                    GuideStaticScreenshotDemos.floatingActionDemo(true),
                    GuideStaticScreenshotDemos.splitPaneDemo(),
                    GuideStaticScreenshotDemos.sliderDemo(),
                    GuideStaticScreenshotDemos.graphicsHiWorldDemo(),
                    GuideStaticScreenshotDemos.graphicsGlassPaneDemo(),
                    GuideStaticScreenshotDemos.shapedClippingDemo(),
                    GuideStaticScreenshotDemos.fontImageDemo("FontImage Fixed", 0),
                    GuideStaticScreenshotDemos.fontImageDemo("FontImage Style", 1),
                    GuideStaticScreenshotDemos.fontImageDemo("FontImage Material", 2),
                    GuideStaticScreenshotDemos.dialogDemo("Dialog South", dialog -> dialog.showPacked(BorderLayout.SOUTH, true)),
                    GuideStaticScreenshotDemos.dialogDemo("Dialog Bottom Half", dialog ->
                            dialog.show(0, Display.getInstance().getDisplayHeight() / 2, 0, 0)),
                    GuideStaticScreenshotDemos.dialogDemo("Dialog Tint", dialog -> dialog.showPacked(BorderLayout.CENTER, true)),
                    GuideStaticScreenshotDemos.dialogDemo("Dialog Green Tint", dialog -> {
                        dialog.setTintColor(0x3300aa44);
                        dialog.showPacked(BorderLayout.CENTER, true);
                    }),
                    GuideStaticScreenshotDemos.dialogDemo("Dialog Blur", dialog -> {
                        dialog.setBlurBackgroundRadius(8);
                        dialog.showPacked(BorderLayout.CENTER, true);
                    }),
                    GuideStaticScreenshotDemos.dialogDemo("Dialog Blur No Tint", dialog -> {
                        dialog.setTintColor(0);
                        dialog.setBlurBackgroundRadius(8);
                        dialog.showPacked(BorderLayout.CENTER, true);
                    }),
                    GuideStaticScreenshotDemos.interactionDialogDemo()
            )
    );
    private static final List<GuideScreenshot> SCREENSHOTS = Collections.unmodifiableList(
            Arrays.asList(
                    screenshot("layout-animations", "Layout Animations", "layout-animation-1.png"),
                    screenshot("layout-animations", "Layout Animations", "layout-animation-2.png"),
                    screenshot("layout-animations", "Layout Animations", "layout-animation-3.png"),
                    screenshot("layout-animations", "Layout Animations", "layout-animation-4.png"),
                    screenshot("layout-animations", "Layout Animations", "layout-animation-5.png"),
                    screenshot("layout-animations", "Layout Animations", "layout-animation-6.png"),
                    screenshot("layout-animations", "Layout Animations", "layout-animation-7.png"),
                    screenshot("slide-transitions", "Slide Transitions", "transition-slide.png"),
                    screenshot("slide-transitions", "Slide Transitions", "transition-slide-vertical.png"),
                    screenshot("slide-transitions", "Slide Transitions", "transition-slide-fade.png"),
                    screenshot("slide-transitions", "Slide Transitions", "transition-cover.png"),
                    screenshot("slide-transitions", "Slide Transitions", "transition-uncover.png"),
                    screenshot("slide-transitions", "Slide Transitions", "transition-fade.png"),
                    screenshot("slide-transitions", "Slide Transitions", "transition-flip.png"),
                    screenshot("bubble-transition", "Bubble Transition", "transition-bubble.png"),
                    screenshot("morph-transition", "Morph Transition", "mighty-morphing-components-1.png"),
                    screenshot("flow-layout", "Flow Layout", "flow-layout.png"),
                    screenshot("flow-layout-center", "Flow Layout Center", "flow-layout-center.png"),
                    screenshot("flow-layout-right", "Flow Layout Right", "flow-layout-right.png"),
                    screenshot("flow-layout-center-middle", "Flow Layout Center Middle", "flow-layout-center-middle.png"),
                    screenshot("box-layout-y", "BoxLayout Y", "box-layout-y.png"),
                    screenshot("box-layout-x", "BoxLayout X", "box-layout-x.png"),
                    screenshot("box-layout-x-no-grow", "BoxLayout X No Grow", "box-layout-x-no-grow.png"),
                    screenshot("border-layout", "Border Layout", "border-layout.png"),
                    screenshot("border-layout-center", "Border Layout Center", "border-layout-center.png"),
                    screenshot("border-layout-rtl", "Border Layout RTL", "border-layout-RTL.png"),
                    screenshot("grid-layout-2x2", "Grid Layout 2x2", "grid-layout-2x2.png"),
                    screenshot("grid-layout-2x4", "Grid Layout 2x4", "grid-layout-2x4.png"),
                    screenshot("grid-layout-autofit-portrait", "Grid AutoFit Portrait", "grid-layout-autofit-portrait.png"),
                    screenshot("grid-layout-autofit-landscape", "Grid AutoFit Landscape", "grid-layout-autofit-landscape.png"),
                    screenshot("table-layout-2x2", "TableLayout 2x2", "table-layout-2x2.png"),
                    screenshot("table-layout-enclose", "TableLayout Enclose", "table-layout-enclose.png"),
                    screenshot("table-layout-constraints", "TableLayout Constraints", "table-layout-constraints.png"),
                    screenshot("components-button", "Button", "components-button.png"),
                    screenshot("components-link-button", "Link Button", "components-link-button.png"),
                    screenshot("raised-flat-buttons", "Raised and Flat Buttons", "raised-flat-buttons.png"),
                    screenshot("components-radiobutton-checkbox", "RadioButton and CheckBox", "components-radiobutton-checkbox.png"),
                    screenshot("components-componentgroup", "ComponentGroup", "components-componentgroup.png"),
                    screenshot("components-multibutton", "MultiButton", "components-multibutton.png"),
                    screenshot("components-spanbutton", "SpanButton", "components-spanbutton.png"),
                    screenshot("components-spanlabel", "SpanLabel", "components-spanlabel.png"),
                    screenshot("components-onoffswitch", "OnOffSwitch", "components-onoffswitch.png"),
                    screenshot("components-tabs", "Tabs", "components-tabs.png"),
                    screenshot("components-tabs-swipe1", "Swipeable Tabs Page 1", "components-tabs-swipe1.png"),
                    screenshot("components-tabs-swipe2", "Swipeable Tabs Page 2", "components-tabs-swipe2.png"),
                    screenshot("components-picker", "Picker", "components-picker.png"),
                    screenshot("components-floatinghint", "FloatingHint", "components-floatinghint.png"),
                    screenshot("components-accordion", "Accordion", "components-accordion.png"),
                    screenshot("floating-action", "Floating Action", "floating-action.png"),
                    screenshot("badge-floating-button", "Badge Floating Button", "badge-floating-button.png"),
                    screenshot("splitpane", "SplitPane", "splitpane.png"),
                    screenshot("components-slider", "Slider", "components-slider.png"),
                    screenshot("graphics-hiworld", "Hi World", "graphics-hiworld.png"),
                    screenshot("graphics-glasspane", "Glass Pane", "graphics-glasspane.png"),
                    screenshot("shaped-clipping", "Shaped Clipping", "shaped-clipping.png"),
                    screenshot("graphics-fontimage-fixed", "FontImage Fixed", "graphics-fontimage-fixed.png"),
                    screenshot("graphics-fontimage-style", "FontImage Style", "graphics-fontimage-style.png"),
                    screenshot("graphics-fontimage-material", "FontImage Material", "graphics-fontimage-material.png"),
                    screenshot("components-dialog-modal-south", "Dialog South", "components-dialog-modal-south.png"),
                    screenshot("components-dialog-modal-bottom-half", "Dialog Bottom Half", "components-dialog-modal-bottom-half.png"),
                    screenshot("components-dialog-tint", "Dialog Tint", "components-dialog-tint.png"),
                    screenshot("components-dialog-green-tint", "Dialog Green Tint", "components-dialog-green-tint.png"),
                    screenshot("components-dialog-blur", "Dialog Blur", "components-dialog-blur.png"),
                    screenshot("components-dialog-blur-no-tint", "Dialog Blur No Tint", "components-dialog-blur-no-tint.png"),
                    screenshot("components-interaction-dialog", "Interaction Dialog", "components-interaction-dialog.png")
            )
    );

    private DemoRegistry() {
        // utility class
    }

    public static List<Demo> getDemos() {
        return DEMOS;
    }

    public static List<GuideScreenshot> getScreenshots() {
        return SCREENSHOTS;
    }

    private static GuideScreenshot screenshot(String id, String demoTitle, String fileName) {
        for (Demo demo : DEMOS) {
            if (demo.getTitle().equals(demoTitle)) {
                return new GuideScreenshot(id, demo, fileName);
            }
        }
        throw new IllegalStateException("No guide demo registered with title: " + demoTitle);
    }
}
