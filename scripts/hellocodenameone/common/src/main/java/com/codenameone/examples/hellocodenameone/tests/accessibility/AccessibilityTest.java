package com.codenameone.examples.hellocodenameone.tests.accessibility;

import com.codename1.ui.Button;
import com.codename1.ui.AccessibilityColorVisionDeficiency;
import com.codename1.ui.CN;
import com.codename1.ui.CheckBox;
import com.codename1.ui.Component;
import com.codename1.ui.Container;
import com.codename1.ui.Display;
import com.codename1.ui.Form;
import com.codename1.ui.Label;
import com.codename1.ui.Slider;
import com.codename1.ui.accessibility.AccessibilityAction;
import com.codename1.ui.accessibility.AccessibilityAssertions;
import com.codename1.ui.accessibility.AccessibilityCheckedState;
import com.codename1.ui.accessibility.AccessibilityCollectionInfo;
import com.codename1.ui.accessibility.AccessibilityCollectionItemInfo;
import com.codename1.ui.accessibility.AccessibilityGrouping;
import com.codename1.ui.accessibility.AccessibilityInspector;
import com.codename1.ui.accessibility.AccessibilityLiveRegion;
import com.codename1.ui.accessibility.AccessibilityManager;
import com.codename1.ui.accessibility.AccessibilityNode;
import com.codename1.ui.accessibility.AccessibilityNodeSnapshot;
import com.codename1.ui.accessibility.AccessibilityRange;
import com.codename1.ui.accessibility.AccessibilityRole;
import com.codename1.ui.accessibility.AccessibilityTreeSnapshot;
import com.codename1.ui.geom.Rectangle;
import com.codenameone.examples.hellocodenameone.tests.BaseTest;

/** Cross-port semantics conformance test.  This runs in every native cn1ss job. */
public class AccessibilityTest extends BaseTest {
    private volatile boolean customActionPerformed;

    @Override
    public boolean runTest() throws Exception {
        CN.callSerially(new Runnable() {
            public void run() {
                try {
                    runSemanticAssertions();
                } catch (Throwable t) {
                    fail("Accessibility semantic assertions failed: " + t);
                }
            }
        });
        return true;
    }

    private void runSemanticAssertions() {
        assertAccessibilityPreferencesAvailable();
        final Form form = new Form("Accessibility conformance");
        size(form, 0, 0, 600, 1200);
        size(form.getContentPane(), 0, 0, 600, 1200);
        form.getSemantics().setPaneTitle("Settings").setRole(AccessibilityRole.GENERIC);

        Label heading = add(form, new Label("Preferences"), 10, 10, 300, 40);
        heading.getSemantics().setHeadingLevel(2).setIdentifier("preferences-heading");

        Button save = add(form, new Button("Save"), 10, 60, 120, 40);
        save.getSemantics().setHint("Saves all changes").setSortKey(2)
                .addAction(new AccessibilityAction("share", "Share settings",
                        new AccessibilityAction.Handler() {
                            public boolean perform(Component component, Object argument) {
                                customActionPerformed = true;
                                return true;
                            }
                        }));

        CheckBox notifications = add(form, new CheckBox("Notifications"), 10, 110, 220, 40);
        notifications.setSelected(true);
        notifications.getSemantics().setSortKey(1).setRequired(Boolean.TRUE);

        Slider volume = add(form, new Slider(), 10, 160, 260, 40);
        volume.setEditable(true);
        volume.getSemantics().setLabel("Volume")
                .setRange(new AccessibilityRange(0, 100, 35, 5, "35 percent"));

        Label error = add(form, new Label("Password is too short"), 10, 210, 300, 40);
        error.getSemantics().setRole(AccessibilityRole.ALERT).setLiveRegion(AccessibilityLiveRegion.ASSERTIVE)
                .setValidationError("Password is too short").setInvalid(Boolean.TRUE);

        Container collection = add(form, new Container(), 10, 260, 300, 150);
        collection.getSemantics().setRole(AccessibilityRole.GRID)
                .setLabel("Accounts")
                .setCollectionInfo(new AccessibilityCollectionInfo(2, 1, false,
                        AccessibilityCollectionInfo.SELECTION_SINGLE));
        Label firstRow = add(collection, new Label("Personal"), 0, 0, 250, 50);
        firstRow.getSemantics().setRole(AccessibilityRole.CELL).setSelected(Boolean.TRUE)
                .setCollectionItemInfo(new AccessibilityCollectionItemInfo(0, 1, 0, 1, 1, 2, 1, false));
        Label secondRow = add(collection, new Label("Business"), 0, 55, 250, 50);
        secondRow.getSemantics().setRole(AccessibilityRole.CELL).setSelected(Boolean.FALSE)
                .setCollectionItemInfo(new AccessibilityCollectionItemInfo(1, 1, 0, 1, 2, 2, 1, false));

        Container merged = add(form, new Container(), 10, 420, 300, 80);
        merged.getSemantics().setRole(AccessibilityRole.GENERIC).setGrouping(AccessibilityGrouping.MERGE_DESCENDANTS);
        add(merged, new Label("Billing"), 0, 0, 100, 30);
        add(merged, new Label("monthly"), 110, 0, 100, 30);

        Label semanticSwitch = add(form, new Label("Automatic updates"), 10, 510, 260, 40);
        semanticSwitch.getSemantics().setRole(AccessibilityRole.SWITCH)
                .setChecked(AccessibilityCheckedState.MIXED).setExpanded(Boolean.TRUE);

        Container semanticList = add(form, new Container(), 10, 560, 300, 90);
        semanticList.getSemantics().setRole(AccessibilityRole.LIST).setLabel("Recent projects")
                .setCollectionInfo(new AccessibilityCollectionInfo(2, 1, false,
                        AccessibilityCollectionInfo.SELECTION_NONE));
        Label semanticListItem = add(semanticList, new Label("Project Aurora"), 0, 0, 250, 35);
        semanticListItem.getSemantics().setRole(AccessibilityRole.LIST_ITEM)
                .setCollectionItemInfo(new AccessibilityCollectionItemInfo(1, 2));

        Label semanticTab = add(form, new Label("Overview"), 10, 660, 160, 40);
        semanticTab.getSemantics().setRole(AccessibilityRole.TAB).setSelected(Boolean.TRUE);

        Container semanticDialog = add(form, new Container(), 10, 710, 300, 90);
        semanticDialog.getSemantics().setRole(AccessibilityRole.DIALOG).setLabel("Confirm deletion")
                .setPaneTitle("Confirmation").setModal(true);

        Container virtualHost = add(form, new Container(), 10, 810, 300, 100);
        virtualHost.getSemantics().setRole(AccessibilityRole.LIST).setLabel("Virtual results")
                .addChild(new AccessibilityNode("result-42").setRole(AccessibilityRole.LIST_ITEM)
                        .setLabel("Virtual result").setBounds(new Rectangle(0, 0, 280, 45))
                        .setCollectionItemInfo(new AccessibilityCollectionItemInfo(1, 1)));

        Button disabled = add(form, new Button("Unavailable action"), 10, 920, 220, 40);
        disabled.setEnabled(false);

        AccessibilityTreeSnapshot first = AccessibilityInspector.snapshot(form);
        AccessibilityNodeSnapshot headingNode = find(first, heading);
        AccessibilityNodeSnapshot saveNode = find(first, save);
        AccessibilityNodeSnapshot checkboxNode = find(first, notifications);
        AccessibilityNodeSnapshot volumeNode = find(first, volume);
        AccessibilityNodeSnapshot errorNode = find(first, error);
        AccessibilityNodeSnapshot collectionNode = find(first, collection);
        AccessibilityNodeSnapshot mergedNode = find(first, merged);
        AccessibilityNodeSnapshot switchNode = find(first, semanticSwitch);
        AccessibilityNodeSnapshot listNode = find(first, semanticList);
        AccessibilityNodeSnapshot tabNode = find(first, semanticTab);
        AccessibilityNodeSnapshot dialogNode = find(first, semanticDialog);
        AccessibilityNodeSnapshot virtualHostNode = find(first, virtualHost);

        assertEqual(AccessibilityRole.HEADING, headingNode.getRole(), "heading role");
        assertEqual(2, headingNode.getHeadingLevel(), "heading level");
        assertEqual("preferences-heading", headingNode.getIdentifier(), "automation identifier");
        assertEqual(AccessibilityRole.BUTTON, saveNode.getRole(), "inferred button role");
        assertTrue(saveNode.getAction(AccessibilityAction.ACTIVATE) != null, "standard activation action");
        assertTrue(saveNode.getAction("share") != null, "custom action");
        assertEqual(AccessibilityCheckedState.CHECKED, checkboxNode.getChecked(), "checked state");
        assertEqual(Boolean.TRUE, checkboxNode.getRequired(), "required state");
        assertEqual(35.0, volumeNode.getRange().getCurrent(), "range value");
        assertEqual(5.0, volumeNode.getRange().getStep(), "range step");
        assertEqual(AccessibilityLiveRegion.ASSERTIVE, errorNode.getLiveRegion(), "live region");
        assertEqual(Boolean.TRUE, errorNode.getInvalid(), "validation state");
        assertEqual(2, collectionNode.getCollectionInfo().getRowCount(), "collection rows");
        assertEqual(2, collectionNode.getChildIds().size(), "collection children");
        assertEqual(2, find(first, secondRow).getCollectionItemInfo().getPositionInSet(), "set position");
        assertTrue(mergedNode.getLabel().indexOf("Billing") >= 0, "merged first label");
        assertTrue(mergedNode.getLabel().indexOf("monthly") >= 0, "merged second label");
        assertEqual(0, mergedNode.getChildIds().size(), "merged descendants hidden");
        assertTrue(findOrNull(first, firstRow) != null, "collection descendant retained");
        assertTrue(findOrNull(first, merged.getComponentAt(0)) == null, "merged descendant omitted");
        assertEqual(AccessibilityRole.SWITCH, switchNode.getRole(), "switch role");
        assertEqual(AccessibilityCheckedState.MIXED, switchNode.getChecked(), "mixed switch state");
        assertEqual(Boolean.TRUE, switchNode.getExpanded(), "expanded state");
        assertEqual(AccessibilityRole.LIST, listNode.getRole(), "list role");
        assertEqual(AccessibilityRole.LIST_ITEM, find(first, semanticListItem).getRole(), "list item role");
        assertEqual(AccessibilityRole.TAB, tabNode.getRole(), "tab role");
        assertEqual(Boolean.TRUE, tabNode.getSelected(), "selected tab state");
        assertEqual(AccessibilityRole.DIALOG, dialogNode.getRole(), "dialog role");
        assertTrue(dialogNode.isModal(), "modal dialog state");
        assertEqual("Confirmation", dialogNode.getPaneTitle(), "pane transition title");
        assertEqual(Boolean.FALSE, find(first, disabled).getEnabled(), "disabled state");
        assertEqual(1, virtualHostNode.getChildIds().size(), "virtual accessibility descendant");
        AccessibilityNodeSnapshot virtualNode = first.getNode(virtualHostNode.getChildIds().get(0).longValue());
        assertEqual("result-42", virtualNode.getVirtualKey(), "stable virtual key");
        assertEqual(AccessibilityRole.LIST_ITEM, virtualNode.getRole(), "virtual child role");
        assertEqual("Virtual result", first.getNodeAt(20, 830).getLabel(), "virtual child hit testing");

        AccessibilityNodeSnapshot formNode = find(first, form);
        int checkboxPosition = formNode.getChildIds().indexOf(Long.valueOf(checkboxNode.getId()));
        int savePosition = formNode.getChildIds().indexOf(Long.valueOf(saveNode.getId()));
        assertTrue(checkboxPosition >= 0 && checkboxPosition < savePosition, "explicit traversal order");
        assertTrue(first.getNodeAt(20, 70) != null, "semantic hit testing");
        AccessibilityAssertions.assertNoErrors(first);
        AccessibilityAssertions.assertNoUnlabeledInteractiveNodes(first);

        String json = first.toJson();
        assertTrue(json.indexOf("\"role\":\"HEADING\"") >= 0, "serialized role");
        assertTrue(json.indexOf("\"liveRegion\":\"ASSERTIVE\"") >= 0, "serialized live region");
        assertTrue(json.indexOf("\"collection\"") >= 0, "serialized collection metadata");

        long stableSaveId = saveNode.getId();
        notifications.setSelected(false);
        AccessibilityTreeSnapshot second = AccessibilityInspector.snapshot(form);
        assertEqual(stableSaveId, find(second, save).getId(), "stable semantic id");
        assertEqual(AccessibilityCheckedState.UNCHECKED, find(second, notifications).getChecked(), "updated state");
        assertTrue(second.getGeneration() > first.getGeneration(), "tree generation changes");

        assertTrue(Display.getInstance().isAccessibilityTreeSupported(),
                "native port exposes the virtual accessibility tree");
        form.show();
        assertTrue(AccessibilityManager.getInstance().performAction(stableSaveId, "share", null),
                "custom action accepted");
        CN.callSerially(new Runnable() {
            public void run() {
                try {
                    assertTrue(customActionPerformed, "custom action dispatched on EDT");
                    AccessibilityTreeSnapshot nativeSnapshot = AccessibilityInspector.currentSnapshot();
                    assertEqual(stableSaveId, find(nativeSnapshot, save).getId(),
                            "current native semantic snapshot");
                    AccessibilityAssertions.assertNoErrors(nativeSnapshot);
                    Display.getInstance().announceForAccessibility("Testing accessibility announcement");
                    done();
                } catch (Throwable t) {
                    fail("Accessibility action assertions failed: " + t);
                }
            }
        });
    }

    private void assertAccessibilityPreferencesAvailable() {
        AccessibilityColorVisionDeficiency colorVision = Display.getInstance().getColorVisionDeficiency();
        assertTrue(colorVision != null, "color-vision preference has a portable value");
        assertTrue(Display.getInstance().getLargerTextScale() > 0,
                "larger-text scale is positive");

        // Invoke every portable preference on every native CI target. The values
        // legitimately depend on the host, but none of these calls may throw or
        // require an initialized semantic tree.
        Display.getInstance().isHighContrastEnabled();
        Display.getInstance().isDifferentiateWithoutColorEnabled();
        Display.getInstance().isReduceMotionEnabled();
        Display.getInstance().isReduceTransparencyEnabled();
        Display.getInstance().isBoldTextEnabled();
        Display.getInstance().isInvertColorsEnabled();
        Display.getInstance().isGrayscaleEnabled();
        Display.getInstance().isOnOffSwitchLabelsEnabled();
        Display.getInstance().isScreenReaderEnabled();

        if (colorVision == AccessibilityColorVisionDeficiency.MONOCHROMACY) {
            assertTrue(Display.getInstance().isGrayscaleEnabled(),
                    "monochromacy implies grayscale presentation");
        }
    }

    private <T extends Component> T add(Container parent, T component, int x, int y, int width, int height) {
        parent.add(component);
        size(component, x, y, width, height);
        return component;
    }

    private void size(Component component, int x, int y, int width, int height) {
        component.setX(x);
        component.setY(y);
        component.setWidth(width);
        component.setHeight(height);
    }

    private AccessibilityNodeSnapshot find(AccessibilityTreeSnapshot tree, Component component) {
        AccessibilityNodeSnapshot result = findOrNull(tree, component);
        assertTrue(result != null, "semantic node exists for " + component.getClass().getName());
        return result;
    }

    private AccessibilityNodeSnapshot findOrNull(AccessibilityTreeSnapshot tree, Component component) {
        for (AccessibilityNodeSnapshot node : tree.getNodes().values()) {
            if (node.getComponent() == component && node.getVirtualKey() == null) return node;
        }
        return null;
    }

    @Override
    public boolean shouldTakeScreenshot() {
        return false;
    }
}
