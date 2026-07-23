/*
 * Copyright (c) 2012, Codename One and/or its affiliates. All rights reserved.
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
package com.codename1.impl.javase;

import com.codename1.bluetooth.Bluetooth;
import com.codename1.bluetooth.BluetoothError;
import com.codename1.bluetooth.BluetoothUuid;
import com.codename1.bluetooth.le.server.GattLocalCharacteristic;
import com.codename1.bluetooth.le.server.GattLocalDescriptor;
import com.codename1.bluetooth.le.server.GattLocalService;
import com.codename1.impl.javase.bluetooth.BluetoothFixture;
import com.codename1.impl.javase.bluetooth.BluetoothSimulator;
import com.codename1.impl.javase.bluetooth.FixtureRecorder;
import com.codename1.impl.javase.bluetooth.JavaSEBluetooth;
import com.codename1.impl.javase.bluetooth.SimulatedBluetoothStack;
import com.codename1.impl.javase.bluetooth.StackEventListener;
import com.codename1.impl.javase.bluetooth.VirtualCharacteristic;
import com.codename1.impl.javase.bluetooth.VirtualDescriptor;
import com.codename1.impl.javase.bluetooth.VirtualPeripheral;
import com.codename1.impl.javase.bluetooth.VirtualService;

import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.Toolkit;
import java.awt.datatransfer.StringSelection;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.prefs.Preferences;

import javax.swing.BorderFactory;
import javax.swing.ButtonGroup;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JScrollPane;
import javax.swing.JSpinner;
import javax.swing.JSplitPane;
import javax.swing.JTable;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.JToolBar;
import javax.swing.JTree;
import javax.swing.SpinnerNumberModel;
import javax.swing.SwingUtilities;
import javax.swing.Timer;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.event.TreeSelectionEvent;
import javax.swing.event.TreeSelectionListener;
import javax.swing.table.DefaultTableModel;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreePath;
import javax.swing.tree.TreeSelectionModel;

/**
 * Simulator window over the virtual Bluetooth world managed by
 * {@link BluetoothSimulator}: a tree of the simulated adapter, virtual
 * peripherals and the app's own peripheral role, detail editors for the
 * selected node, and a live event log fed by a {@link StackEventListener}.
 * Everything the window does goes through the scriptable
 * {@code com.codename1.impl.javase.bluetooth} facade, so anything staged
 * here can equally be staged from tests.
 *
 * <p>Swing runs on the AWT EDT while the stack dispatches its callbacks on
 * its own scheduler thread; every stack-originated UI update is marshaled
 * through {@link SwingUtilities#invokeLater(Runnable)}.</p>
 */
public class BluetoothSimulation extends JFrame {

    private static final int MAX_LOG_ROWS = 2000;

    /** Operation keys accepted by {@code SimulatedBluetoothStack.failNext}. */
    private static final String[] FAILURE_OPS = {
        "connect", "disconnect", "read", "write", "discover", "subscribe",
        "scan", "rssi", "mtu", "bond", "rfcommConnect", "l2cap"
    };

    private static final String CARD_EMPTY = "empty";
    private static final String CARD_ADAPTER = "adapter";
    private static final String CARD_PERIPHERAL = "peripheral";
    private static final String CARD_CHARACTERISTIC = "characteristic";
    private static final String CARD_INFO = "info";

    private final Preferences pref =
            Preferences.userNodeForPackage(BluetoothSimulation.class);

    // ------------------------------------------------------------------
    // tree
    // ------------------------------------------------------------------

    private final DefaultMutableTreeNode rootNode =
            new DefaultMutableTreeNode("Bluetooth");
    private final DefaultTreeModel treeModel = new DefaultTreeModel(rootNode);
    private final JTree tree = new JTree(treeModel);
    private final Timer treeRefreshTimer;

    // ------------------------------------------------------------------
    // right-hand cards
    // ------------------------------------------------------------------

    private final CardLayout cardLayout = new CardLayout();
    private final JPanel cards = new JPanel(cardLayout);
    private boolean updatingUi;
    private boolean rebuildingTree;
    private String shownCardKey;

    // adapter card
    private final JCheckBox adapterEnabledCheck =
            new JCheckBox("Adapter enabled");
    private final JSpinner latencySpinner = new JSpinner(
            new SpinnerNumberModel(
                    (int) SimulatedBluetoothStack.DEFAULT_LATENCY_MILLIS,
                    0, 60000, 10));
    private final JComboBox<String> failureOpCombo =
            new JComboBox<String>(FAILURE_OPS);
    private final JComboBox<BluetoothError> failureErrorCombo =
            new JComboBox<BluetoothError>(BluetoothError.values());
    private final JTextField failureMessageField = new JTextField(20);
    private final JComboBox<String> backendCombo = new JComboBox<String>(
            new String[] {JavaSEBluetooth.BACKEND_SIMULATOR,
                    JavaSEBluetooth.BACKEND_NATIVE});

    // peripheral card
    private final JLabel peripheralAddressLabel = new JLabel();
    private final JTextField peripheralNameField = new JTextField(20);
    private final JSpinner peripheralRssiSpinner =
            new JSpinner(new SpinnerNumberModel(-60, -127, 20, 1));
    private final JCheckBox peripheralConnectableCheck =
            new JCheckBox("Connectable");
    private final JLabel peripheralStateLabel = new JLabel(" ");
    private VirtualPeripheral selectedPeripheral;

    // characteristic card
    private final JLabel characteristicTitleLabel = new JLabel();
    private final JLabel characteristicSubscribedLabel = new JLabel(" ");
    private final JTextArea characteristicValueArea = new JTextArea(4, 30);
    private final JRadioButton hexModeRadio = new JRadioButton("Hex", true);
    private final JRadioButton utf8ModeRadio = new JRadioButton("UTF-8");
    private String selectedCharacteristicAddress;
    private BluetoothUuid selectedCharacteristicService;
    private VirtualCharacteristic selectedCharacteristic;

    // generic info card (services, descriptors, app-as-peripheral nodes)
    private final JTextArea infoArea = new JTextArea();

    // ------------------------------------------------------------------
    // event log
    // ------------------------------------------------------------------

    private final DefaultTableModel logModel = new DefaultTableModel(
            new Object[] {"Time", "Operation", "Detail"}, 0) {
        @Override
        public boolean isCellEditable(int row, int column) {
            return false;
        }
    };
    private final JTable logTable = new JTable(logModel);
    private final SimpleDateFormat timeFormat =
            new SimpleDateFormat("HH:mm:ss.SSS");
    private StackEventListener stackListener;

    /**
     * Central subscriptions observed through the event feed while the
     * window is open ({@code centralSubscribe} events); keyed by central
     * address. The stack has no introspection API for these yet.
     */
    private final Map<String, Set<String>> centralSubscriptions =
            new HashMap<String, Set<String>>();

    public BluetoothSimulation() {
        super("Bluetooth Simulation");
        setDefaultCloseOperation(JFrame.HIDE_ON_CLOSE);
        setLayout(new BorderLayout());

        applyPersistedStackPrefs();

        treeRefreshTimer = new Timer(200, new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                refreshTree();
            }
        });
        treeRefreshTimer.setRepeats(false);

        add(buildToolbar(), BorderLayout.NORTH);

        tree.setRootVisible(false);
        tree.setShowsRootHandles(true);
        tree.getSelectionModel().setSelectionMode(
                TreeSelectionModel.SINGLE_TREE_SELECTION);
        tree.addTreeSelectionListener(new TreeSelectionListener() {
            @Override
            public void valueChanged(TreeSelectionEvent e) {
                showCardForSelection();
            }
        });
        JScrollPane treeScroll = new JScrollPane(tree);
        treeScroll.setPreferredSize(new Dimension(320, 380));

        buildCards();
        JSplitPane split = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT,
                treeScroll, cards);
        split.setResizeWeight(0.35);

        JSplitPane vertical = new JSplitPane(JSplitPane.VERTICAL_SPLIT,
                split, buildLogPanel());
        vertical.setResizeWeight(0.7);
        add(vertical, BorderLayout.CENTER);

        addComponentListener(new ComponentAdapter() {
            @Override
            public void componentShown(ComponentEvent e) {
                attachStackListener();
                refreshTree();
            }

            @Override
            public void componentHidden(ComponentEvent e) {
                detachStackListener();
            }

            @Override
            public void componentMoved(ComponentEvent e) {
                persistBounds();
            }

            @Override
            public void componentResized(ComponentEvent e) {
                persistBounds();
            }
        });

        refreshTree();
        expandDefaultRows();

        pack();
        restoreBounds();
    }

    // ------------------------------------------------------------------
    // toolbar
    // ------------------------------------------------------------------

    private JToolBar buildToolbar() {
        JToolBar toolbar = new JToolBar();
        toolbar.setFloatable(false);
        toolbar.add(button("Add Peripheral", new Runnable() {
            @Override
            public void run() {
                showAddPeripheralDialog();
            }
        }));
        toolbar.add(button("Add Demo Peripheral", new Runnable() {
            @Override
            public void run() {
                BluetoothSimulator.addPeripheral(
                        BluetoothSimulatorHooks.createDemoPeripheral());
                scheduleTreeRefresh();
            }
        }));
        toolbar.add(button("Record from real hardware", new Runnable() {
            @Override
            public void run() {
                showRecordFixtureDialog();
            }
        }));
        toolbar.addSeparator();
        toolbar.add(button("Reset", new Runnable() {
            @Override
            public void run() {
                int answer = JOptionPane.showConfirmDialog(
                        BluetoothSimulation.this,
                        "Reset the simulated Bluetooth stack?\nAll "
                                + "peripherals, connections and scripted "
                                + "failures are cleared.",
                        "Reset Bluetooth Simulation",
                        JOptionPane.OK_CANCEL_OPTION);
                if (answer == JOptionPane.OK_OPTION) {
                    centralSubscriptions.clear();
                    BluetoothSimulator.reset();
                    scheduleTreeRefresh();
                }
            }
        }));
        return toolbar;
    }

    private JButton button(String label, final Runnable action) {
        JButton b = new JButton(label);
        b.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                action.run();
            }
        });
        return b;
    }

    private void showAddPeripheralDialog() {
        JTextField address = new JTextField("00:11:22:33:44:55", 17);
        JTextField name = new JTextField("Virtual Device", 17);
        JSpinner rssi = new JSpinner(new SpinnerNumberModel(-60, -127, 20, 1));
        JCheckBox demoService = new JCheckBox(
                "Include demo GATT service (0x180D)", true);

        JPanel panel = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(2, 4, 2, 4);
        gbc.anchor = GridBagConstraints.WEST;
        addRow(panel, gbc, 0, "Address:", address);
        addRow(panel, gbc, 1, "Name:", name);
        addRow(panel, gbc, 2, "RSSI (dBm):", rssi);
        gbc.gridx = 0;
        gbc.gridy = 3;
        gbc.gridwidth = 2;
        panel.add(demoService, gbc);

        int answer = JOptionPane.showConfirmDialog(this, panel,
                "Add Peripheral", JOptionPane.OK_CANCEL_OPTION,
                JOptionPane.PLAIN_MESSAGE);
        if (answer != JOptionPane.OK_OPTION) {
            return;
        }
        try {
            VirtualPeripheral p =
                    new VirtualPeripheral(address.getText().trim())
                            .setName(name.getText().trim())
                            .setRssi(((Number) rssi.getValue()).intValue());
            if (demoService.isSelected()) {
                p.addAdvertisedServiceUuid(
                        BluetoothSimulatorHooks.DEMO_SERVICE);
                p.withService(BluetoothSimulatorHooks.createDemoService());
            }
            BluetoothSimulator.addPeripheral(p);
            scheduleTreeRefresh();
        } catch (IllegalArgumentException ex) {
            JOptionPane.showMessageDialog(this, ex.getMessage(),
                    "Invalid peripheral", JOptionPane.ERROR_MESSAGE);
        }
    }

    /**
     * "Record from real hardware": scans this machine's real radio for a
     * chosen duration through a fresh {@code NativeBleBackend}
     * ({@link FixtureRecorder}), scrambles the trace with the chosen seed
     * ({@code FixtureScrambler}) and imports the resulting devices as
     * virtual peripherals -- optionally also saving the fixture JSON.
     * The capture runs on a worker thread; Swing only shows the results.
     */
    private void showRecordFixtureDialog() {
        JSpinner duration = new JSpinner(
                new SpinnerNumberModel(10, 1, 300, 1));
        JTextField seedField = new JTextField("42", 12);
        final JCheckBox saveCheck = new JCheckBox("Save fixture JSON to:");
        JTextField savePath = new JTextField(new java.io.File(
                System.getProperty("user.home", "."),
                "bluetooth-fixture.json").getAbsolutePath(), 28);

        JPanel panel = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(2, 4, 2, 4);
        gbc.anchor = GridBagConstraints.WEST;
        addRow(panel, gbc, 0, "Scan duration (s):", duration);
        addRow(panel, gbc, 1, "Scramble seed:", seedField);
        addRow(panel, gbc, 2, "", saveCheck);
        addRow(panel, gbc, 3, "", savePath);

        int answer = JOptionPane.showConfirmDialog(this, panel,
                "Record from real hardware", JOptionPane.OK_CANCEL_OPTION,
                JOptionPane.PLAIN_MESSAGE);
        if (answer != JOptionPane.OK_OPTION) {
            return;
        }
        final long seed;
        try {
            seed = Long.parseLong(seedField.getText().trim());
        } catch (NumberFormatException ex) {
            JOptionPane.showMessageDialog(this,
                    "The scramble seed must be a number",
                    "Invalid seed", JOptionPane.ERROR_MESSAGE);
            return;
        }
        final long scanMillis =
                ((Number) duration.getValue()).longValue() * 1000L;
        final String saveTo = saveCheck.isSelected()
                ? savePath.getText().trim() : null;
        Thread worker = new Thread(new Runnable() {
            @Override
            public void run() {
                recordFixture(scanMillis, seed, saveTo);
            }
        }, "cn1-bluetooth-fixture-recorder");
        worker.setDaemon(true);
        worker.start();
    }

    /** Worker-thread body of the record-from-real-hardware flow. */
    private void recordFixture(long scanMillis, long seed, String saveTo) {
        FixtureRecorder recorder = null;
        try {
            recorder = FixtureRecorder.forNativeBackend();
            BluetoothFixture fixture = recorder.recordScrambled(
                    scanMillis, null, false, seed);
            if (saveTo != null && saveTo.length() > 0) {
                java.io.Writer w = new java.io.OutputStreamWriter(
                        new java.io.FileOutputStream(saveTo), "UTF-8");
                try {
                    w.write(fixture.toJson());
                } finally {
                    w.close();
                }
            }
            BluetoothSimulator.loadFixture(fixture);
            final int count = fixture.getDevices().size();
            final String path = saveTo;
            SwingUtilities.invokeLater(new Runnable() {
                @Override
                public void run() {
                    scheduleTreeRefresh();
                    JOptionPane.showMessageDialog(BluetoothSimulation.this,
                            "Recorded " + count + " device(s) from the "
                                    + "real radio (identities scrambled)."
                                    + "\nTheir advertisement timelines are "
                                    + "replaying into the simulation now."
                                    + (path == null ? ""
                                            : "\nFixture saved to " + path),
                            "Recording complete",
                            JOptionPane.INFORMATION_MESSAGE);
                }
            });
        } catch (final Exception ex) {
            SwingUtilities.invokeLater(new Runnable() {
                @Override
                public void run() {
                    JOptionPane.showMessageDialog(BluetoothSimulation.this,
                            "Recording from the real radio failed:\n"
                                    + ex.getMessage()
                                    + "\n\nThe native backend needs the "
                                    + "bundled cn1-ble-helper binary and "
                                    + "OS Bluetooth permission for the "
                                    + "JVM process.",
                            "Recording failed", JOptionPane.ERROR_MESSAGE);
                }
            });
        } finally {
            if (recorder != null) {
                recorder.close();
            }
        }
    }

    private void addRow(JPanel panel, GridBagConstraints gbc, int row,
            String label, Component field) {
        gbc.gridwidth = 1;
        gbc.gridx = 0;
        gbc.gridy = row;
        panel.add(new JLabel(label), gbc);
        gbc.gridx = 1;
        panel.add(field, gbc);
    }

    // ------------------------------------------------------------------
    // tree building
    // ------------------------------------------------------------------

    /** Marker user objects carrying a stable key for expansion restore. */
    private abstract static class Node {
        abstract String key();
    }

    private static final class AdapterNode extends Node {
        final boolean enabled;

        AdapterNode(boolean enabled) {
            this.enabled = enabled;
        }

        @Override
        String key() {
            return "adapter";
        }

        @Override
        public String toString() {
            return "Adapter (" + (enabled ? "on" : "off") + ")";
        }
    }

    private static final class PeripheralNode extends Node {
        final VirtualPeripheral peripheral;
        final boolean connected;

        PeripheralNode(VirtualPeripheral peripheral, boolean connected) {
            this.peripheral = peripheral;
            this.connected = connected;
        }

        @Override
        String key() {
            return "p:" + peripheral.getAddress();
        }

        @Override
        public String toString() {
            String name = peripheral.getName();
            return peripheral.getAddress()
                    + (name == null ? "" : " — " + name)
                    + (connected ? " [connected]" : "");
        }
    }

    private static final class ServiceNode extends Node {
        final String address;
        final VirtualService service;

        ServiceNode(String address, VirtualService service) {
            this.address = address;
            this.service = service;
        }

        @Override
        String key() {
            return "s:" + address + ":" + service.getUuid();
        }

        @Override
        public String toString() {
            return "Service " + uuidLabel(service.getUuid())
                    + (service.isPrimary() ? "" : " (secondary)");
        }
    }

    private static final class CharacteristicNode extends Node {
        final String address;
        final BluetoothUuid serviceUuid;
        final VirtualCharacteristic characteristic;

        CharacteristicNode(String address, BluetoothUuid serviceUuid,
                VirtualCharacteristic characteristic) {
            this.address = address;
            this.serviceUuid = serviceUuid;
            this.characteristic = characteristic;
        }

        @Override
        String key() {
            return "c:" + address + ":" + serviceUuid + ":"
                    + characteristic.getUuid();
        }

        @Override
        public String toString() {
            StringBuilder props = new StringBuilder();
            if (characteristic.canRead()) {
                props.append("R");
            }
            if (characteristic.canWrite()) {
                props.append("W");
            }
            if (characteristic.canNotifyOrIndicate()) {
                props.append("N");
            }
            return "Characteristic " + uuidLabel(characteristic.getUuid())
                    + (props.length() == 0 ? "" : " [" + props + "]");
        }
    }

    private static final class DescriptorNode extends Node {
        final String address;
        final BluetoothUuid serviceUuid;
        final BluetoothUuid characteristicUuid;
        final VirtualDescriptor descriptor;

        DescriptorNode(String address, BluetoothUuid serviceUuid,
                BluetoothUuid characteristicUuid,
                VirtualDescriptor descriptor) {
            this.address = address;
            this.serviceUuid = serviceUuid;
            this.characteristicUuid = characteristicUuid;
            this.descriptor = descriptor;
        }

        @Override
        String key() {
            return "d:" + address + ":" + serviceUuid + ":"
                    + characteristicUuid + ":" + descriptor.getUuid();
        }

        @Override
        public String toString() {
            return "Descriptor " + uuidLabel(descriptor.getUuid());
        }
    }

    private static final class AppNode extends Node {
        @Override
        String key() {
            return "app";
        }

        @Override
        public String toString() {
            return "App as Peripheral";
        }
    }

    private static final class InfoNode extends Node {
        final String label;
        final String keySuffix;
        final String details;

        InfoNode(String keySuffix, String label, String details) {
            this.keySuffix = keySuffix;
            this.label = label;
            this.details = details;
        }

        @Override
        String key() {
            return "i:" + keySuffix;
        }

        @Override
        public String toString() {
            return label;
        }
    }

    private static String uuidLabel(BluetoothUuid uuid) {
        if (uuid.isShortUuid()) {
            return String.format("0x%04X", Integer.valueOf(
                    uuid.getShortValue()));
        }
        return uuid.toString();
    }

    private void scheduleTreeRefresh() {
        treeRefreshTimer.restart();
    }

    private void refreshTree() {
        rebuildingTree = true;
        try {
            Set<String> expanded = new HashSet<String>();
            for (int i = 0; i < tree.getRowCount(); i++) {
                TreePath path = tree.getPathForRow(i);
                if (tree.isExpanded(path)) {
                    expanded.add(pathKey(path));
                }
            }
            TreePath selection = tree.getSelectionPath();
            String selectedKey = selection == null ? null : pathKey(selection);

            rootNode.removeAllChildren();
            rootNode.add(buildAdapterSubtree());
            rootNode.add(buildAppSubtree());
            treeModel.reload();

            // restore expansion + selection; expanding grows the row count
            // so the loop naturally visits newly revealed children too
            for (int i = 0; i < tree.getRowCount(); i++) {
                TreePath path = tree.getPathForRow(i);
                String key = pathKey(path);
                if (expanded.contains(key)) {
                    tree.expandPath(path);
                }
                if (key.equals(selectedKey)) {
                    tree.setSelectionPath(path);
                }
            }
        } finally {
            rebuildingTree = false;
        }
        if (tree.getSelectionPath() == null) {
            // the previously selected node disappeared (or none was selected)
            showCardForSelection();
        }
        updateAdapterWidgets();
    }

    private void expandDefaultRows() {
        for (int i = tree.getRowCount() - 1; i >= 0; i--) {
            tree.expandRow(i);
        }
    }

    private String pathKey(TreePath path) {
        StringBuilder sb = new StringBuilder();
        Object[] parts = path.getPath();
        for (int i = 0; i < parts.length; i++) {
            Object userObject =
                    ((DefaultMutableTreeNode) parts[i]).getUserObject();
            sb.append('/');
            if (userObject instanceof Node) {
                sb.append(((Node) userObject).key());
            } else {
                sb.append(String.valueOf(userObject));
            }
        }
        return sb.toString();
    }

    private DefaultMutableTreeNode buildAdapterSubtree() {
        SimulatedBluetoothStack stack = BluetoothSimulator.getStack();
        DefaultMutableTreeNode adapter = new DefaultMutableTreeNode(
                new AdapterNode(stack.isAdapterEnabled()));
        for (String address : stack.getPeripheralAddresses()) {
            VirtualPeripheral p = stack.getPeripheral(address);
            if (p == null) {
                continue;
            }
            DefaultMutableTreeNode peripheralNode = new DefaultMutableTreeNode(
                    new PeripheralNode(p, stack.isConnected(address)));
            for (VirtualService s : p.getServices()) {
                DefaultMutableTreeNode serviceNode =
                        new DefaultMutableTreeNode(
                                new ServiceNode(address, s));
                for (VirtualCharacteristic c : s.getCharacteristics()) {
                    DefaultMutableTreeNode characteristicNode =
                            new DefaultMutableTreeNode(new CharacteristicNode(
                                    address, s.getUuid(), c));
                    for (VirtualDescriptor d : c.getDescriptors()) {
                        characteristicNode.add(new DefaultMutableTreeNode(
                                new DescriptorNode(address, s.getUuid(),
                                        c.getUuid(), d)));
                    }
                    serviceNode.add(characteristicNode);
                }
                peripheralNode.add(serviceNode);
            }
            adapter.add(peripheralNode);
        }
        return adapter;
    }

    private DefaultMutableTreeNode buildAppSubtree() {
        SimulatedBluetoothStack stack = BluetoothSimulator.getStack();
        DefaultMutableTreeNode app =
                new DefaultMutableTreeNode(new AppNode());

        List<Object> payloads = stack.getAdvertisingPayloads();
        String advertising = payloads.isEmpty() ? "Advertising: off"
                : "Advertising: on (" + payloads.size() + " payload(s))";
        app.add(new DefaultMutableTreeNode(new InfoNode("advertising",
                advertising, describeAdvertising(payloads))));

        for (GattLocalService s : stack.getAppServices()) {
            DefaultMutableTreeNode serviceNode = new DefaultMutableTreeNode(
                    new InfoNode("as:" + s.getUuid(),
                            "Service " + uuidLabel(s.getUuid())
                                    + (s.isPrimary() ? "" : " (secondary)"),
                            describeAppService(s)));
            for (GattLocalCharacteristic c : s.getCharacteristics()) {
                DefaultMutableTreeNode characteristicNode =
                        new DefaultMutableTreeNode(new InfoNode(
                                "ac:" + s.getUuid() + ":" + c.getUuid(),
                                "Characteristic " + uuidLabel(c.getUuid()),
                                describeAppCharacteristic(c)));
                for (GattLocalDescriptor d : c.getDescriptors()) {
                    characteristicNode.add(new DefaultMutableTreeNode(
                            new InfoNode("ad:" + s.getUuid() + ":"
                                    + c.getUuid() + ":" + d.getUuid(),
                                    "Descriptor " + uuidLabel(d.getUuid()),
                                    "Descriptor " + d.getUuid() + "\nValue: "
                                            + toHex(d.getValue()))));
                }
                serviceNode.add(characteristicNode);
            }
            app.add(serviceNode);
        }

        DefaultMutableTreeNode centrals = new DefaultMutableTreeNode(
                new InfoNode("centrals", "Connected centrals",
                        "Virtual centrals connected to the app's GATT "
                                + "server."));
        for (String address : stack.getConnectedCentralAddresses()) {
            Set<String> subs = centralSubscriptions.get(address);
            DefaultMutableTreeNode centralNode = new DefaultMutableTreeNode(
                    new InfoNode("central:" + address, address,
                            describeCentral(address, subs)));
            if (subs != null) {
                for (String sub : subs) {
                    centralNode.add(new DefaultMutableTreeNode(new InfoNode(
                            "sub:" + address + ":" + sub,
                            "Subscribed: " + sub,
                            "Central " + address
                                    + " is subscribed to " + sub)));
                }
            }
            centrals.add(centralNode);
        }
        app.add(centrals);
        return app;
    }

    private String describeAdvertising(List<Object> payloads) {
        StringBuilder sb = new StringBuilder();
        sb.append("App advertising state\n\n");
        if (payloads.isEmpty()) {
            sb.append("Not advertising.");
        } else {
            sb.append(payloads.size()).append(" live advertisement(s):\n");
            for (Object payload : payloads) {
                sb.append("  • ").append(payload).append('\n');
            }
        }
        return sb.toString();
    }

    private String describeAppService(GattLocalService s) {
        StringBuilder sb = new StringBuilder();
        sb.append("App GATT service ").append(s.getUuid()).append('\n');
        sb.append("Primary: ").append(s.isPrimary()).append('\n');
        sb.append("Characteristics: ")
                .append(s.getCharacteristics().size());
        return sb.toString();
    }

    private String describeAppCharacteristic(GattLocalCharacteristic c) {
        StringBuilder sb = new StringBuilder();
        sb.append("App characteristic ").append(c.getUuid()).append('\n');
        sb.append("Properties: 0x")
                .append(Integer.toHexString(c.getProperties())).append('\n');
        sb.append("Permissions: 0x")
                .append(Integer.toHexString(c.getPermissions())).append('\n');
        byte[] value = c.getValue();
        sb.append(value == null
                ? "Value: dynamic (served by read requests)"
                : "Value: " + toHex(value));
        return sb.toString();
    }

    private String describeCentral(String address, Set<String> subs) {
        StringBuilder sb = new StringBuilder();
        sb.append("Virtual central ").append(address).append('\n');
        if (subs == null || subs.isEmpty()) {
            sb.append("No subscriptions observed while this window was "
                    + "open.");
        } else {
            sb.append("Subscriptions:\n");
            for (String sub : subs) {
                sb.append("  • ").append(sub).append('\n');
            }
        }
        return sb.toString();
    }

    // ------------------------------------------------------------------
    // cards
    // ------------------------------------------------------------------

    private void buildCards() {
        JPanel empty = new JPanel(new BorderLayout());
        empty.add(new JLabel("Select a node in the tree", JLabel.CENTER),
                BorderLayout.CENTER);
        cards.add(empty, CARD_EMPTY);
        cards.add(buildAdapterCard(), CARD_ADAPTER);
        cards.add(buildPeripheralCard(), CARD_PERIPHERAL);
        cards.add(buildCharacteristicCard(), CARD_CHARACTERISTIC);

        infoArea.setEditable(false);
        infoArea.setLineWrap(true);
        infoArea.setWrapStyleWord(true);
        infoArea.setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));
        cards.add(new JScrollPane(infoArea), CARD_INFO);
    }

    private JPanel buildAdapterCard() {
        JPanel card = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(4, 8, 4, 8);
        gbc.anchor = GridBagConstraints.WEST;
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.gridwidth = 2;

        adapterEnabledCheck.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if (updatingUi) {
                    return;
                }
                boolean enabled = adapterEnabledCheck.isSelected();
                BluetoothSimulator.setAdapterEnabled(enabled);
                pref.putBoolean("BluetoothSim.adapterEnabled", enabled);
                scheduleTreeRefresh();
            }
        });
        card.add(adapterEnabledCheck, gbc);

        gbc.gridy++;
        gbc.gridwidth = 1;
        card.add(new JLabel("Latency (ms):"), gbc);
        gbc.gridx = 1;
        latencySpinner.addChangeListener(new ChangeListener() {
            @Override
            public void stateChanged(ChangeEvent e) {
                if (updatingUi) {
                    return;
                }
                int millis = ((Number) latencySpinner.getValue()).intValue();
                BluetoothSimulator.setLatencyMillis(millis);
                pref.putLong("BluetoothSim.latencyMillis", millis);
            }
        });
        card.add(latencySpinner, gbc);

        gbc.gridx = 0;
        gbc.gridy++;
        card.add(new JLabel("Backend:"), gbc);
        gbc.gridx = 1;
        backendCombo.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if (updatingUi) {
                    return;
                }
                switchBackendFromCombo();
            }
        });
        card.add(backendCombo, gbc);

        JPanel failure = new JPanel(new GridBagLayout());
        failure.setBorder(
                BorderFactory.createTitledBorder("Failure injection"));
        GridBagConstraints fgbc = new GridBagConstraints();
        fgbc.insets = new Insets(2, 4, 2, 4);
        fgbc.anchor = GridBagConstraints.WEST;
        fgbc.gridx = 0;
        fgbc.gridy = 0;
        failure.add(new JLabel("Operation:"), fgbc);
        fgbc.gridx = 1;
        failure.add(failureOpCombo, fgbc);
        fgbc.gridx = 0;
        fgbc.gridy = 1;
        failure.add(new JLabel("Error:"), fgbc);
        fgbc.gridx = 1;
        failure.add(failureErrorCombo, fgbc);
        fgbc.gridx = 0;
        fgbc.gridy = 2;
        failure.add(new JLabel("Message:"), fgbc);
        fgbc.gridx = 1;
        fgbc.fill = GridBagConstraints.HORIZONTAL;
        failure.add(failureMessageField, fgbc);
        fgbc.gridx = 1;
        fgbc.gridy = 3;
        fgbc.fill = GridBagConstraints.NONE;
        failure.add(button("Arm next failure", new Runnable() {
            @Override
            public void run() {
                String op = (String) failureOpCombo.getSelectedItem();
                BluetoothError error =
                        (BluetoothError) failureErrorCombo.getSelectedItem();
                String message = failureMessageField.getText().trim();
                BluetoothSimulator.failNext(op, error,
                        message.length() == 0 ? null : message);
            }
        }), fgbc);

        gbc.gridx = 0;
        gbc.gridy++;
        gbc.gridwidth = 2;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        card.add(failure, gbc);

        // push everything to the top-left
        gbc.gridy++;
        gbc.weightx = 1;
        gbc.weighty = 1;
        card.add(new JPanel(), gbc);
        return card;
    }

    private void switchBackendFromCombo() {
        String name = (String) backendCombo.getSelectedItem();
        JavaSEBluetooth impl = bluetoothImpl();
        if (impl == null) {
            JOptionPane.showMessageDialog(this,
                    "The Bluetooth port is not initialized yet -- open the "
                            + "Bluetooth API from the app first.",
                    "Backend unavailable", JOptionPane.ERROR_MESSAGE);
            updateAdapterWidgets();
            return;
        }
        try {
            impl.switchBackend(name);
        } catch (RuntimeException ex) {
            JOptionPane.showMessageDialog(this, ex.getMessage(),
                    "Backend unavailable", JOptionPane.ERROR_MESSAGE);
        }
        updateAdapterWidgets();
    }

    private JavaSEBluetooth bluetoothImpl() {
        try {
            Bluetooth bt = Bluetooth.getInstance();
            if (bt instanceof JavaSEBluetooth) {
                return (JavaSEBluetooth) bt;
            }
        } catch (Throwable t) {
            // Display not initialized yet
        }
        return null;
    }

    /** Syncs the adapter-card widgets with the live stack state. */
    private void updateAdapterWidgets() {
        updatingUi = true;
        try {
            SimulatedBluetoothStack stack = BluetoothSimulator.getStack();
            adapterEnabledCheck.setSelected(stack.isAdapterEnabled());
            latencySpinner.setValue(
                    Integer.valueOf((int) stack.getLatencyMillis()));
            JavaSEBluetooth impl = bluetoothImpl();
            if (impl != null) {
                backendCombo.setSelectedItem(impl.activeBackendName());
            }
        } finally {
            updatingUi = false;
        }
    }

    private JPanel buildPeripheralCard() {
        JPanel card = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(4, 8, 4, 8);
        gbc.anchor = GridBagConstraints.WEST;

        gbc.gridx = 0;
        gbc.gridy = 0;
        card.add(new JLabel("Address:"), gbc);
        gbc.gridx = 1;
        card.add(peripheralAddressLabel, gbc);

        gbc.gridx = 0;
        gbc.gridy++;
        card.add(new JLabel("Name:"), gbc);
        gbc.gridx = 1;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        peripheralNameField.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                applyPeripheralName();
            }
        });
        peripheralNameField.addFocusListener(
                new java.awt.event.FocusAdapter() {
                    @Override
                    public void focusLost(java.awt.event.FocusEvent e) {
                        applyPeripheralName();
                    }
                });
        card.add(peripheralNameField, gbc);
        gbc.fill = GridBagConstraints.NONE;

        gbc.gridx = 0;
        gbc.gridy++;
        card.add(new JLabel("RSSI (dBm):"), gbc);
        gbc.gridx = 1;
        peripheralRssiSpinner.addChangeListener(new ChangeListener() {
            @Override
            public void stateChanged(ChangeEvent e) {
                if (updatingUi || selectedPeripheral == null) {
                    return;
                }
                selectedPeripheral.setRssi(((Number)
                        peripheralRssiSpinner.getValue()).intValue());
            }
        });
        card.add(peripheralRssiSpinner, gbc);

        gbc.gridx = 0;
        gbc.gridy++;
        gbc.gridwidth = 2;
        peripheralConnectableCheck.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if (updatingUi || selectedPeripheral == null) {
                    return;
                }
                selectedPeripheral.setConnectable(
                        peripheralConnectableCheck.isSelected());
            }
        });
        card.add(peripheralConnectableCheck, gbc);

        gbc.gridy++;
        card.add(peripheralStateLabel, gbc);

        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.LEFT));
        buttons.add(button("Disconnect from remote", new Runnable() {
            @Override
            public void run() {
                if (selectedPeripheral != null) {
                    BluetoothSimulator.disconnectFromRemote(
                            selectedPeripheral.getAddress());
                }
            }
        }));
        buttons.add(button("Remove", new Runnable() {
            @Override
            public void run() {
                if (selectedPeripheral != null) {
                    BluetoothSimulator.removePeripheral(
                            selectedPeripheral.getAddress());
                    scheduleTreeRefresh();
                }
            }
        }));
        gbc.gridy++;
        card.add(buttons, gbc);

        gbc.gridy++;
        gbc.weightx = 1;
        gbc.weighty = 1;
        card.add(new JPanel(), gbc);
        return card;
    }

    private void applyPeripheralName() {
        if (updatingUi || selectedPeripheral == null) {
            return;
        }
        String name = peripheralNameField.getText().trim();
        selectedPeripheral.setName(name.length() == 0 ? null : name);
        scheduleTreeRefresh();
    }

    private JPanel buildCharacteristicCard() {
        JPanel card = new JPanel(new BorderLayout(8, 8));
        card.setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));

        JPanel north = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(2, 0, 2, 0);
        gbc.anchor = GridBagConstraints.WEST;
        gbc.gridx = 0;
        gbc.gridy = 0;
        north.add(characteristicTitleLabel, gbc);
        gbc.gridy++;
        north.add(characteristicSubscribedLabel, gbc);
        card.add(north, BorderLayout.NORTH);

        JPanel center = new JPanel(new BorderLayout(4, 4));
        JPanel modes = new JPanel(new FlowLayout(FlowLayout.LEFT, 4, 0));
        ButtonGroup group = new ButtonGroup();
        group.add(hexModeRadio);
        group.add(utf8ModeRadio);
        ActionListener remodel = new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                renderCharacteristicValue();
            }
        };
        hexModeRadio.addActionListener(remodel);
        utf8ModeRadio.addActionListener(remodel);
        modes.add(new JLabel("Value as:"));
        modes.add(hexModeRadio);
        modes.add(utf8ModeRadio);
        center.add(modes, BorderLayout.NORTH);
        center.add(new JScrollPane(characteristicValueArea),
                BorderLayout.CENTER);
        card.add(center, BorderLayout.CENTER);

        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.LEFT));
        buttons.add(button("Set Value", new Runnable() {
            @Override
            public void run() {
                byte[] value = parseValueEditor();
                if (value != null && selectedCharacteristic != null) {
                    selectedCharacteristic.setValue(value);
                    BluetoothSimulator.getStack().logEvent("edit",
                            "characteristic "
                                    + selectedCharacteristic.getUuid()
                                    + " value set to " + value.length
                                    + " byte(s) from the Simulate window");
                }
            }
        }));
        buttons.add(button("Push Notify", new Runnable() {
            @Override
            public void run() {
                byte[] value = parseValueEditor();
                if (value != null && selectedCharacteristic != null) {
                    selectedCharacteristic.setValue(value);
                    BluetoothSimulator.pushNotification(
                            selectedCharacteristicAddress,
                            selectedCharacteristicService,
                            selectedCharacteristic.getUuid(), value);
                }
            }
        }));
        card.add(buttons, BorderLayout.SOUTH);
        return card;
    }

    /** Renders the selected characteristic's live value into the editor. */
    private void renderCharacteristicValue() {
        if (selectedCharacteristic == null) {
            characteristicValueArea.setText("");
            return;
        }
        byte[] value = selectedCharacteristic.getValue();
        if (hexModeRadio.isSelected()) {
            characteristicValueArea.setText(toHex(value));
        } else {
            try {
                characteristicValueArea.setText(new String(value, "UTF-8"));
            } catch (java.io.UnsupportedEncodingException ex) {
                characteristicValueArea.setText("");
            }
        }
    }

    /** Parses the value editor; shows an error dialog and returns null on bad hex. */
    private byte[] parseValueEditor() {
        String text = characteristicValueArea.getText();
        if (utf8ModeRadio.isSelected()) {
            try {
                return text.getBytes("UTF-8");
            } catch (java.io.UnsupportedEncodingException ex) {
                return text.getBytes();
            }
        }
        String compact = text.replaceAll("[\\s,]", "");
        if (compact.startsWith("0x") || compact.startsWith("0X")) {
            compact = compact.substring(2);
        }
        if (compact.length() % 2 != 0
                || !compact.matches("[0-9a-fA-F]*")) {
            JOptionPane.showMessageDialog(this,
                    "Enter an even number of hex digits, e.g. \"00 48\"",
                    "Invalid hex value", JOptionPane.ERROR_MESSAGE);
            return null;
        }
        byte[] out = new byte[compact.length() / 2];
        for (int i = 0; i < out.length; i++) {
            out[i] = (byte) Integer.parseInt(
                    compact.substring(i * 2, i * 2 + 2), 16);
        }
        return out;
    }

    private static String toHex(byte[] value) {
        if (value == null || value.length == 0) {
            return "";
        }
        StringBuilder sb = new StringBuilder(value.length * 3);
        for (int i = 0; i < value.length; i++) {
            if (i > 0) {
                sb.append(' ');
            }
            sb.append(String.format("%02X", Integer.valueOf(value[i] & 0xFF)));
        }
        return sb.toString();
    }

    // ------------------------------------------------------------------
    // selection routing
    // ------------------------------------------------------------------

    private void showCardForSelection() {
        TreePath path = tree.getSelectionPath();
        if (rebuildingTree) {
            // transient selection churn while the tree rebuilds: ignore the
            // momentary null and keep the editors (with any in-progress
            // typing) when the same node is re-selected afterwards
            if (path == null || pathKey(path).equals(shownCardKey)) {
                return;
            }
        }
        shownCardKey = path == null ? null : pathKey(path);
        Object userObject = null;
        if (path != null) {
            userObject = ((DefaultMutableTreeNode)
                    path.getLastPathComponent()).getUserObject();
        }
        selectedPeripheral = null;
        selectedCharacteristic = null;
        selectedCharacteristicAddress = null;
        selectedCharacteristicService = null;

        if (userObject instanceof AdapterNode) {
            updateAdapterWidgets();
            cardLayout.show(cards, CARD_ADAPTER);
        } else if (userObject instanceof PeripheralNode) {
            showPeripheralCard((PeripheralNode) userObject);
        } else if (userObject instanceof CharacteristicNode) {
            showCharacteristicCard((CharacteristicNode) userObject);
        } else if (userObject instanceof ServiceNode) {
            ServiceNode node = (ServiceNode) userObject;
            infoArea.setText("GATT service " + node.service.getUuid()
                    + "\nOn peripheral: " + node.address
                    + "\nPrimary: " + node.service.isPrimary()
                    + "\nCharacteristics: "
                    + node.service.getCharacteristics().size());
            cardLayout.show(cards, CARD_INFO);
        } else if (userObject instanceof DescriptorNode) {
            DescriptorNode node = (DescriptorNode) userObject;
            infoArea.setText("Descriptor " + node.descriptor.getUuid()
                    + "\nOn characteristic: " + node.characteristicUuid
                    + "\nValue: " + toHex(node.descriptor.getValue()));
            cardLayout.show(cards, CARD_INFO);
        } else if (userObject instanceof AppNode) {
            infoArea.setText(describeAppRole());
            cardLayout.show(cards, CARD_INFO);
        } else if (userObject instanceof InfoNode) {
            infoArea.setText(((InfoNode) userObject).details);
            cardLayout.show(cards, CARD_INFO);
        } else {
            cardLayout.show(cards, CARD_EMPTY);
        }
    }

    private String describeAppRole() {
        SimulatedBluetoothStack stack = BluetoothSimulator.getStack();
        StringBuilder sb = new StringBuilder();
        sb.append("The app's own peripheral role.\n\n");
        sb.append("Published GATT services: ")
                .append(stack.getAppServices().size()).append('\n');
        sb.append("Advertising: ")
                .append(stack.isAdvertising() ? "on" : "off").append('\n');
        sb.append("Connected virtual centrals: ")
                .append(stack.getConnectedCentralAddresses().size());
        return sb.toString();
    }

    private void showPeripheralCard(PeripheralNode node) {
        selectedPeripheral = node.peripheral;
        updatingUi = true;
        try {
            peripheralAddressLabel.setText(node.peripheral.getAddress());
            String name = node.peripheral.getName();
            peripheralNameField.setText(name == null ? "" : name);
            peripheralRssiSpinner.setValue(
                    Integer.valueOf(node.peripheral.getRssi()));
            peripheralConnectableCheck.setSelected(
                    node.peripheral.isConnectable());
            SimulatedBluetoothStack stack = BluetoothSimulator.getStack();
            String address = node.peripheral.getAddress();
            peripheralStateLabel.setText("Connected: "
                    + stack.isConnected(address) + "    Bonded: "
                    + stack.isBonded(address));
        } finally {
            updatingUi = false;
        }
        cardLayout.show(cards, CARD_PERIPHERAL);
    }

    private void showCharacteristicCard(CharacteristicNode node) {
        selectedCharacteristic = node.characteristic;
        selectedCharacteristicAddress = node.address;
        selectedCharacteristicService = node.serviceUuid;
        characteristicTitleLabel.setText("Characteristic "
                + node.characteristic.getUuid() + " on " + node.address);
        boolean subscribed = BluetoothSimulator.getStack().isSubscribed(
                node.address, node.serviceUuid,
                node.characteristic.getUuid());
        characteristicSubscribedLabel.setText(
                "App subscribed: " + (subscribed ? "yes" : "no"));
        renderCharacteristicValue();
        cardLayout.show(cards, CARD_CHARACTERISTIC);
    }

    // ------------------------------------------------------------------
    // event log
    // ------------------------------------------------------------------

    private JPanel buildLogPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBorder(BorderFactory.createTitledBorder("Event log"));

        logTable.getColumnModel().getColumn(0).setPreferredWidth(90);
        logTable.getColumnModel().getColumn(0).setMaxWidth(120);
        logTable.getColumnModel().getColumn(1).setPreferredWidth(120);
        logTable.getColumnModel().getColumn(1).setMaxWidth(180);
        logTable.setFillsViewportHeight(true);
        JScrollPane scroll = new JScrollPane(logTable);
        scroll.setPreferredSize(new Dimension(640, 160));
        panel.add(scroll, BorderLayout.CENTER);

        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.RIGHT, 4, 2));
        buttons.add(button("Clear", new Runnable() {
            @Override
            public void run() {
                logModel.setRowCount(0);
            }
        }));
        buttons.add(button("Copy", new Runnable() {
            @Override
            public void run() {
                copyLogToClipboard();
            }
        }));
        panel.add(buttons, BorderLayout.SOUTH);
        return panel;
    }

    private void copyLogToClipboard() {
        StringBuilder sb = new StringBuilder();
        int rows = logModel.getRowCount();
        for (int i = 0; i < rows; i++) {
            sb.append(logModel.getValueAt(i, 0)).append('\t')
                    .append(logModel.getValueAt(i, 1)).append('\t')
                    .append(logModel.getValueAt(i, 2)).append('\n');
        }
        Toolkit.getDefaultToolkit().getSystemClipboard().setContents(
                new StringSelection(sb.toString()), null);
    }

    private void appendLog(String op, String detail) {
        while (logModel.getRowCount() >= MAX_LOG_ROWS) {
            logModel.removeRow(0);
        }
        logModel.addRow(new Object[] {
            timeFormat.format(new Date()), op, detail});
        int last = logTable.getRowCount() - 1;
        if (last >= 0) {
            logTable.scrollRectToVisible(
                    logTable.getCellRect(last, 0, true));
        }
    }

    private void attachStackListener() {
        if (stackListener != null) {
            return;
        }
        stackListener = new StackEventListener() {
            @Override
            public void event(final String op, final String detail) {
                // stack scheduler thread -> AWT EDT
                SwingUtilities.invokeLater(new Runnable() {
                    @Override
                    public void run() {
                        appendLog(op, detail);
                        trackCentralSubscriptions(op, detail);
                        scheduleTreeRefresh();
                    }
                });
            }
        };
        BluetoothSimulator.addEventListener(stackListener);
    }

    private void detachStackListener() {
        if (stackListener != null) {
            BluetoothSimulator.removeEventListener(stackListener);
            stackListener = null;
        }
    }

    /**
     * Best-effort mirror of the virtual centrals' subscriptions from the
     * event feed ({@code "centralSubscribe"} events look like
     * {@code "<address> <characteristicUuid> subscribe=<bool>"}).
     */
    private void trackCentralSubscriptions(String op, String detail) {
        if ("centralSubscribe".equals(op)) {
            String[] parts = detail.split(" ");
            if (parts.length >= 3) {
                String address = parts[0];
                String characteristic = parts[1];
                boolean subscribe = "subscribe=true".equals(parts[2]);
                Set<String> subs = centralSubscriptions.get(address);
                if (subs == null) {
                    subs = new TreeSet<String>();
                    centralSubscriptions.put(address, subs);
                }
                if (subscribe) {
                    subs.add(characteristic);
                } else {
                    subs.remove(characteristic);
                }
            }
        } else if ("central".equals(op) && detail.startsWith("disconnected ")) {
            centralSubscriptions.remove(
                    detail.substring("disconnected ".length()));
        } else if ("reset".equals(op)
                || ("gattServer".equals(op) && "close".equals(detail))) {
            centralSubscriptions.clear();
        }
    }

    // ------------------------------------------------------------------
    // preferences
    // ------------------------------------------------------------------

    /** Re-applies the persisted adapter/latency settings to the stack. */
    private void applyPersistedStackPrefs() {
        boolean enabled = pref.getBoolean("BluetoothSim.adapterEnabled", true);
        long latency = pref.getLong("BluetoothSim.latencyMillis",
                SimulatedBluetoothStack.DEFAULT_LATENCY_MILLIS);
        SimulatedBluetoothStack stack = BluetoothSimulator.getStack();
        if (enabled != stack.isAdapterEnabled()) {
            stack.setAdapterEnabled(enabled);
        }
        if (latency != stack.getLatencyMillis()) {
            stack.setLatencyMillis(latency);
        }
    }

    private void restoreBounds() {
        int w = pref.getInt("BluetoothSim.w", -1);
        int h = pref.getInt("BluetoothSim.h", -1);
        if (w > 100 && h > 100) {
            setBounds(pref.getInt("BluetoothSim.x", 100),
                    pref.getInt("BluetoothSim.y", 100), w, h);
        } else {
            setLocationByPlatform(true);
        }
    }

    private void persistBounds() {
        if (!isShowing()) {
            return;
        }
        pref.putInt("BluetoothSim.x", getX());
        pref.putInt("BluetoothSim.y", getY());
        pref.putInt("BluetoothSim.w", getWidth());
        pref.putInt("BluetoothSim.h", getHeight());
    }
}
