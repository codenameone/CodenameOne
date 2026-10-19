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

package com.codename1.impl.javase;

import com.codename1.io.filechooser.FileDialogOption;
import com.codename1.io.filechooser.FileDialogRequest;
import com.codename1.io.filechooser.FileDialogResult;
import com.codename1.io.filechooser.FilePreviewProvider;
import com.codename1.io.filechooser.FileTypeFilter;
import com.codename1.io.filechooser.OpenFileRequest;
import com.codename1.io.filechooser.SaveFileRequest;
import com.codename1.ui.Component;
import com.codename1.ui.Container;
import com.codename1.ui.Display;
import com.codename1.util.SuccessCallback;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.EventQueue;
import java.awt.FileDialog;
import java.awt.Frame;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.File;
import java.io.FilenameFilter;
import java.util.HashMap;
import java.util.Map;
import javax.swing.BoxLayout;
import javax.swing.ImageIcon;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.SwingConstants;
import javax.swing.filechooser.FileFilter;

/// The simulator and desktop file dialogs.
///
/// A plain request uses AWT's `FileDialog`, which is the operating system's own
/// panel. A request that needs what that panel cannot do -- a filter list,
/// options, a preview or folder picking -- uses Swing's `JFileChooser`, whose
/// accessory hosts the options and a rendering of the Codename One preview.
/// A Codename One accessory component cannot be embedded in either, so
/// `FileDialogFeatures#ACCESSORY_COMPONENT` is not reported here.
final class JavaSEFileDialogs {
    private static final int PREVIEW_SIZE = 220;

    private JavaSEFileDialogs() {
    }

    interface SaveCompleter {
        void complete(SaveFileRequest request, FileDialogResult result, SuccessCallback<FileDialogResult> callback);
    }

    static void showOpen(final OpenFileRequest request, final SuccessCallback<FileDialogResult> callback) {
        EventQueue.invokeLater(new Runnable() {
            public void run() {
                final FileDialogResult result = needsSwing(request) ? openSwing(request) : openAwt(request);
                Display.getInstance().callSerially(new Runnable() {
                    public void run() {
                        callback.onSucess(result);
                    }
                });
            }
        });
    }

    static void showSave(final SaveFileRequest request, final SuccessCallback<FileDialogResult> callback,
                         final SaveCompleter completer) {
        EventQueue.invokeLater(new Runnable() {
            public void run() {
                final FileDialogResult result = needsSwing(request) ? saveSwing(request) : saveAwt(request);
                Display.getInstance().callSerially(new Runnable() {
                    public void run() {
                        if (result.isCancelled()) {
                            callback.onSucess(result);
                        } else {
                            completer.complete(request, result, callback);
                        }
                    }
                });
            }
        });
    }

    private static boolean needsSwing(FileDialogRequest<?> request) {
        if (request.getFilters().length > 1 || request.getOptions().length > 0
                || request.getBooleanHint(com.codename1.io.filechooser.FileDialogHints.DESKTOP_SHOW_HIDDEN, false)) {
            return true;
        }
        if (request instanceof OpenFileRequest) {
            OpenFileRequest o = (OpenFileRequest) request;
            return o.getPreview() != null || o.isDirectories();
        }
        return false;
    }

    private static Frame parentFrame() {
        Frame[] frames = Frame.getFrames();
        for (int iter = 0; iter < frames.length; iter++) {
            if (frames[iter].isShowing()) {
                return frames[iter];
            }
        }
        return null;
    }

    private static File toFile(String path) {
        if (path == null) {
            return null;
        }
        if (path.startsWith("file://")) {
            path = path.substring(7);
        }
        return new File(path);
    }

    private static String toPath(File f) {
        return "file://" + f.getAbsolutePath().replace('\\', '/');
    }

    private static String title(FileDialogRequest<?> request, String def) {
        return request.getTitle() == null ? def : request.getTitle();
    }

    private static FileDialogResult openAwt(OpenFileRequest request) {
        FileDialog picker = new FileDialog(parentFrame(), title(request, "Open"), FileDialog.LOAD);
        picker.setMultipleMode(request.isMultiSelect());
        File dir = toFile(request.getInitialDirectory());
        if (dir != null) {
            picker.setDirectory(dir.getAbsolutePath());
        }
        final int sel = request.getSelectedFilter();
        if (sel >= 0) {
            final FileTypeFilter filter = request.getFilters()[sel];
            picker.setFilenameFilter(new FilenameFilter() {
                public boolean accept(File d, String name) {
                    return filter.acceptsName(name);
                }
            });
        }
        picker.setVisible(true);
        File[] files = picker.getFiles();
        if (files == null || files.length == 0) {
            return FileDialogResult.cancelled(request);
        }
        String[] paths = new String[files.length];
        for (int i = 0; i < files.length; i++) {
            paths[i] = toPath(files[i]);
        }
        return new FileDialogResult(request, paths, sel, null);
    }

    private static FileDialogResult saveAwt(SaveFileRequest request) {
        FileDialog picker = new FileDialog(parentFrame(), title(request, "Save"), FileDialog.SAVE);
        File dir = toFile(request.getInitialDirectory());
        if (dir != null) {
            picker.setDirectory(dir.getAbsolutePath());
        }
        if (request.getSuggestedName() != null) {
            picker.setFile(request.getSuggestedName());
        }
        picker.setVisible(true);
        if (picker.getFile() == null) {
            return FileDialogResult.cancelled(request);
        }
        int sel = request.getSelectedFilter();
        String name = withExtension(picker.getFile(), sel < 0 ? null : request.getFilters()[sel]);
        return new FileDialogResult(request, new String[]{toPath(new File(picker.getDirectory(), name))}, sel, null);
    }

    private static JFileChooser createChooser(FileDialogRequest<?> request, JComponent[] editors) {
        JFileChooser chooser = new JFileChooser();
        chooser.setDialogTitle(request.getTitle());
        File dir = toFile(request.getInitialDirectory());
        if (dir != null) {
            chooser.setCurrentDirectory(dir);
        }
        chooser.setFileHidingEnabled(!request.getBooleanHint(
                com.codename1.io.filechooser.FileDialogHints.DESKTOP_SHOW_HIDDEN, false));
        if (request.getAcceptLabel() != null) {
            chooser.setApproveButtonText(request.getAcceptLabel());
        }
        FileTypeFilter[] filters = request.getFilters();
        if (filters.length > 0) {
            chooser.setAcceptAllFileFilterUsed(false);
            for (int i = 0; i < filters.length; i++) {
                chooser.addChoosableFileFilter(new SwingFilter(filters[i], i));
            }
            chooser.setFileFilter(chooser.getChoosableFileFilters()[request.getSelectedFilter()]);
        }
        FileDialogOption[] options = request.getOptions();
        if (options.length > 0) {
            JPanel panel = new JPanel();
            panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
            for (int i = 0; i < options.length; i++) {
                FileDialogOption o = options[i];
                if (o.getType() == FileDialogOption.TYPE_CHECKBOX) {
                    editors[i] = new JCheckBox(o.getLabel(), "true".equals(o.getDefaultValue()));
                } else {
                    panel.add(new JLabel(o.getLabel()));
                    if (o.getType() == FileDialogOption.TYPE_CHOICE) {
                        JComboBox<String> box = new JComboBox<String>(o.getChoices());
                        box.setSelectedItem(o.getDefaultValue());
                        editors[i] = box;
                    } else {
                        editors[i] = new JTextField(o.getDefaultValue() == null ? "" : o.getDefaultValue(), 12);
                    }
                }
                editors[i].setAlignmentX(JComponent.LEFT_ALIGNMENT);
                panel.add(editors[i]);
            }
            chooser.setAccessory(panel);
        }
        return chooser;
    }

    private static Map<String, String> optionValues(FileDialogRequest<?> request, JComponent[] editors) {
        Map<String, String> values = new HashMap<String, String>();
        FileDialogOption[] options = request.getOptions();
        for (int i = 0; i < options.length; i++) {
            JComponent c = editors[i];
            String v;
            if (c instanceof JCheckBox) {
                v = ((JCheckBox) c).isSelected() ? "true" : "false";
            } else if (c instanceof JComboBox) {
                Object o = ((JComboBox<?>) c).getSelectedItem();
                v = o == null ? null : o.toString();
            } else {
                v = ((JTextField) c).getText();
            }
            values.put(options[i].getId(), v);
        }
        return values;
    }

    private static int filterIndex(JFileChooser chooser) {
        FileFilter f = chooser.getFileFilter();
        return f instanceof SwingFilter ? ((SwingFilter) f).index : -1;
    }

    private static FileDialogResult openSwing(OpenFileRequest request) {
        JComponent[] editors = new JComponent[request.getOptions().length];
        JFileChooser chooser = createChooser(request, editors);
        chooser.setMultiSelectionEnabled(request.isMultiSelect());
        if (request.isDirectories()) {
            chooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
        }
        if (request.getPreview() != null) {
            installPreview(chooser, request.getPreview());
        }
        if (chooser.showOpenDialog(parentFrame()) != JFileChooser.APPROVE_OPTION) {
            return FileDialogResult.cancelled(request);
        }
        File[] files = request.isMultiSelect() ? chooser.getSelectedFiles() : new File[]{chooser.getSelectedFile()};
        if (files == null || files.length == 0 || files[0] == null) {
            return FileDialogResult.cancelled(request);
        }
        String[] paths = new String[files.length];
        for (int i = 0; i < files.length; i++) {
            paths[i] = toPath(files[i]);
        }
        return new FileDialogResult(request, paths, filterIndex(chooser), optionValues(request, editors));
    }

    private static FileDialogResult saveSwing(SaveFileRequest request) {
        JComponent[] editors = new JComponent[request.getOptions().length];
        JFileChooser chooser = createChooser(request, editors);
        File dir = chooser.getCurrentDirectory();
        if (request.getSuggestedName() != null) {
            chooser.setSelectedFile(new File(dir, request.getSuggestedName()));
        }
        while (true) {
            if (chooser.showSaveDialog(parentFrame()) != JFileChooser.APPROVE_OPTION || chooser.getSelectedFile() == null) {
                return FileDialogResult.cancelled(request);
            }
            int sel = filterIndex(chooser);
            File f = chooser.getSelectedFile();
            f = new File(f.getParentFile(), withExtension(f.getName(), sel < 0 ? null : request.getFilters()[sel]));
            if (f.exists() && request.isConfirmOverwrite()
                    && JOptionPane.showConfirmDialog(chooser, f.getName() + " already exists. Replace it?", "Replace",
                    JOptionPane.YES_NO_OPTION) != JOptionPane.YES_OPTION) {
                continue;
            }
            return new FileDialogResult(request, new String[]{toPath(f)}, sel, optionValues(request, editors));
        }
    }

    static String withExtension(String name, FileTypeFilter filter) {
        if (filter == null || filter.acceptsName(name) || filter.getExtensions().length == 0) {
            return name;
        }
        return name + "." + filter.getExtensions()[0];
    }

    /// Shows the Codename One preview beside the chooser: the provider runs on
    /// the EDT, its component is painted into an image there, and the image is
    /// handed back to Swing.
    private static void installPreview(JFileChooser chooser, final FilePreviewProvider provider) {
        final JLabel label = new JLabel("", SwingConstants.CENTER);
        label.setPreferredSize(new Dimension(PREVIEW_SIZE, PREVIEW_SIZE));
        JPanel holder = new JPanel(new BorderLayout());
        holder.add(label, BorderLayout.CENTER);
        if (chooser.getAccessory() != null) {
            holder.add(chooser.getAccessory(), BorderLayout.SOUTH);
        }
        chooser.setAccessory(holder);
        chooser.addPropertyChangeListener(JFileChooser.SELECTED_FILE_CHANGED_PROPERTY, new PropertyChangeListener() {
            public void propertyChange(PropertyChangeEvent evt) {
                final File f = (File) evt.getNewValue();
                label.setIcon(null);
                if (f == null || f.isDirectory()) {
                    return;
                }
                Display.getInstance().callSerially(new Runnable() {
                    public void run() {
                        final java.awt.Image img = renderPreview(provider, toPath(f));
                        EventQueue.invokeLater(new Runnable() {
                            public void run() {
                                label.setIcon(img == null ? null : new ImageIcon(img));
                            }
                        });
                    }
                });
            }
        });
    }

    private static java.awt.Image renderPreview(FilePreviewProvider provider, String path) {
        Component cmp;
        try {
            cmp = provider.createPreview(path);
        } catch (RuntimeException err) {
            com.codename1.io.Log.e(err);
            return null;
        }
        if (cmp == null) {
            return null;
        }
        Dimension size = new Dimension(Math.min(PREVIEW_SIZE, Math.max(1, cmp.getPreferredW())),
                Math.min(PREVIEW_SIZE, Math.max(1, cmp.getPreferredH())));
        cmp.setX(0);
        cmp.setY(0);
        cmp.setWidth(size.width);
        cmp.setHeight(size.height);
        if (cmp instanceof Container) {
            ((Container) cmp).layoutContainer();
        }
        com.codename1.ui.Image img = com.codename1.ui.Image.createImage(size.width, size.height, 0);
        cmp.paintComponent(img.getGraphics(), true);
        Object peer = img.getImage();
        return peer instanceof java.awt.Image ? (java.awt.Image) peer : null;
    }

    private static final class SwingFilter extends FileFilter {
        private final FileTypeFilter filter;
        private final int index;

        SwingFilter(FileTypeFilter filter, int index) {
            this.filter = filter;
            this.index = index;
        }

        public boolean accept(File f) {
            return f.isDirectory() || filter.acceptsName(f.getName());
        }

        public String getDescription() {
            return filter.getLabel();
        }
    }
}
