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
package com.codename1.desktopcompat.javax.swing;

import com.codename1.desktopcompat.java.awt.BorderLayout;
import com.codename1.desktopcompat.java.awt.Component;
import com.codename1.desktopcompat.java.awt.Container;
import com.codename1.desktopcompat.java.awt.Dialog;
import com.codename1.desktopcompat.java.awt.Window;
import com.codename1.desktopcompat.java.awt.event.ActionEvent;
import com.codename1.desktopcompat.java.awt.event.ActionListener;
import com.codename1.desktopcompat.java.awt.event.MouseAdapter;
import com.codename1.desktopcompat.java.awt.event.MouseEvent;
import com.codename1.desktopcompat.java.awt.event.WindowAdapter;
import com.codename1.desktopcompat.java.awt.event.WindowEvent;
import com.codename1.desktopcompat.javax.swing.event.ListSelectionEvent;
import com.codename1.desktopcompat.javax.swing.event.ListSelectionListener;
import com.codename1.desktopcompat.javax.swing.filechooser.FileFilter;
import com.codename1.desktopcompat.javax.swing.filechooser.FileNameExtensionFilter;
import com.codename1.desktopcompat.rt.FilePicker;
import com.codename1.io.FileSystemStorage;
import java.io.File;
import java.util.ArrayList;

/// `javax.swing.JFileChooser`: asks the user for a file. Each `show...`
/// method blocks its caller until the user answered.
///
/// **Two ways of asking.**
///
/// - Opening a file goes to the platform's own picker
///   (`Display.openFileChooser`): the document picker of a phone, the file
///   dialog of a desktop. That is the only way to reach a file outside the
///   application's own storage on a phone. It answers with one file, also
///   when multiple selection is enabled, and it takes the extensions of a
///   `FileNameExtensionFilter` only; any other filter is not applied to it.
/// - Saving, and choosing a directory, show a dialog of this layer that
///   lists the directories the file API can reach, starting at the current
///   directory, which is the application's home directory unless one was
///   set. On a phone that is the application's own storage.
///
/// The accessory, the file view and the file system view are not provided.
public class JFileChooser extends JComponent {

    public static final int OPEN_DIALOG = 0;
    public static final int SAVE_DIALOG = 1;
    public static final int CUSTOM_DIALOG = 2;

    public static final int CANCEL_OPTION = 1;
    public static final int APPROVE_OPTION = 0;
    public static final int ERROR_OPTION = -1;

    public static final int FILES_ONLY = 0;
    public static final int DIRECTORIES_ONLY = 1;
    public static final int FILES_AND_DIRECTORIES = 2;

    public static final String CANCEL_SELECTION = "CancelSelection";
    public static final String APPROVE_SELECTION = "ApproveSelection";

    public static final String APPROVE_BUTTON_TEXT_CHANGED_PROPERTY = "ApproveButtonTextChangedProperty";
    public static final String DIRECTORY_CHANGED_PROPERTY = "directoryChanged";
    public static final String SELECTED_FILE_CHANGED_PROPERTY = "SelectedFileChangedProperty";
    public static final String SELECTED_FILES_CHANGED_PROPERTY = "SelectedFilesChangedProperty";
    public static final String MULTI_SELECTION_ENABLED_CHANGED_PROPERTY = "MultiSelectionEnabledChangedProperty";
    public static final String FILE_FILTER_CHANGED_PROPERTY = "fileFilterChanged";
    public static final String FILE_SELECTION_MODE_CHANGED_PROPERTY = "fileSelectionChanged";
    public static final String FILE_HIDING_CHANGED_PROPERTY = "FileHidingChanged";
    public static final String DIALOG_TITLE_CHANGED_PROPERTY = "DialogTitleChangedProperty";
    public static final String DIALOG_TYPE_CHANGED_PROPERTY = "DialogTypeChangedProperty";
    public static final String CHOOSABLE_FILE_FILTER_CHANGED_PROPERTY = "ChoosableFileFilterChangedProperty";
    public static final String ACCEPT_ALL_FILE_FILTER_USED_CHANGED_PROPERTY = "acceptAllFileFilterUsedChanged";

    private final FileFilter acceptAll = new AcceptAll();
    private final ArrayList<FileFilter> filters = new ArrayList<FileFilter>();
    private final ArrayList<ActionListener> listeners = new ArrayList<ActionListener>();

    private File currentDirectory;
    private File selectedFile;
    private File[] selectedFiles;
    private int dialogType = OPEN_DIALOG;
    private String dialogTitle;
    private String approveButtonText;
    private int mode = FILES_ONLY;
    private boolean multi;
    private boolean hiding = true;
    private boolean useAcceptAll = true;
    private FileFilter fileFilter;
    private int returnValue = ERROR_OPTION;

    private JDialog dialog;
    private JList<String> listing;
    private JLabel where;
    private JTextField nameField;
    private File[] entries = new File[0];

    public JFileChooser() {
        this((File) null);
    }

    public JFileChooser(String currentDirectoryPath) {
        this(currentDirectoryPath == null ? null : new File(currentDirectoryPath));
    }

    public JFileChooser(File currentDirectory) {
        filters.add(acceptAll);
        fileFilter = acceptAll;
        if (currentDirectory != null) {
            setCurrentDirectory(currentDirectory);
        }
    }

    private static final class AcceptAll extends FileFilter {
        AcceptAll() {
        }

        @Override
        public boolean accept(File f) {
            return true;
        }

        @Override
        public String getDescription() {
            String s = UIManager.getString("FileChooser.acceptAllFileFilterText");
            return s == null ? "All Files" : s;
        }
    }

    // ---- the selection ----

    public File getSelectedFile() {
        return selectedFile;
    }

    public void setSelectedFile(File file) {
        File old = selectedFile;
        selectedFile = file;
        if (file != null) {
            File parent = file.getParentFile();
            if (parent != null && file.isAbsolute()) {
                currentDirectory = parent;
            }
            if (nameField != null) {
                nameField.setText(file.getName());
            }
        }
        firePropertyChange(SELECTED_FILE_CHANGED_PROPERTY, old, selectedFile);
    }

    public File[] getSelectedFiles() {
        if (selectedFiles == null) {
            return new File[0];
        }
        File[] r = new File[selectedFiles.length];
        System.arraycopy(selectedFiles, 0, r, 0, r.length);
        return r;
    }

    public void setSelectedFiles(File[] files) {
        if (files == null || files.length == 0) {
            selectedFiles = null;
            setSelectedFile(null);
        } else {
            selectedFiles = new File[files.length];
            System.arraycopy(files, 0, selectedFiles, 0, files.length);
            setSelectedFile(selectedFiles[0]);
        }
    }

    public File getCurrentDirectory() {
        if (currentDirectory == null) {
            currentDirectory = home();
        }
        return currentDirectory;
    }

    private static File home() {
        String home = null;
        if (com.codename1.ui.Display.isInitialized()) {
            home = FileSystemStorage.getInstance().getAppHomePath();
        }
        return new File(home == null || home.length() == 0 ? "/" : home);
    }

    public void setCurrentDirectory(File dir) {
        File old = currentDirectory;
        File d = dir;
        // As on the desktop, a file that is not a directory stands for the
        // closest directory above it.
        while (d != null && !d.isDirectory()) {
            d = d.getParentFile();
        }
        currentDirectory = d == null ? home() : d;
        firePropertyChange(DIRECTORY_CHANGED_PROPERTY, old, currentDirectory);
        rescanCurrentDirectory();
    }

    public void changeToParentDirectory() {
        File parent = getCurrentDirectory().getParentFile();
        if (parent != null) {
            setCurrentDirectory(parent);
        }
    }

    /// Lists the current directory again: the directories first, then the
    /// files the filter accepts, each group by name.
    public void rescanCurrentDirectory() {
        if (listing == null) {
            return;
        }
        File dir = getCurrentDirectory();
        File[] all = dir.listFiles();
        ArrayList<File> shown = new ArrayList<File>();
        if (all != null) {
            for (int pass = 0; pass < 2; pass++) {
                int first = shown.size();
                for (int i = 0; i < all.length; i++) {
                    File f = all[i];
                    if (f == null || hiding && f.isHidden()) {
                        continue;
                    }
                    boolean isDir = f.isDirectory();
                    if (isDir != (pass == 0) || !isDir && (mode == DIRECTORIES_ONLY || !accept(f))) {
                        continue;
                    }
                    int at = shown.size();
                    while (at > first && shown.get(at - 1).getName().compareTo(f.getName()) > 0) {
                        at--;
                    }
                    shown.add(at, f);
                }
            }
        }
        entries = shown.toArray(new File[shown.size()]);
        String[] names = new String[entries.length];
        for (int i = 0; i < names.length; i++) {
            names[i] = entries[i].isDirectory() ? entries[i].getName() + "/" : entries[i].getName();
        }
        listing.setListData(names);
        where.setText(dir.getPath());
    }

    public void ensureFileIsVisible(File f) {
        if (listing == null || f == null) {
            return;
        }
        for (int i = 0; i < entries.length; i++) {
            if (entries[i].equals(f)) {
                listing.ensureIndexIsVisible(i);
                return;
            }
        }
    }

    // ---- showing ----

    public int showOpenDialog(Component parent) {
        setDialogType(OPEN_DIALOG);
        return showDialog(parent, null);
    }

    public int showSaveDialog(Component parent) {
        setDialogType(SAVE_DIALOG);
        return showDialog(parent, null);
    }

    public int showDialog(Component parent, String approveButtonText) {
        if (approveButtonText != null) {
            setApproveButtonText(approveButtonText);
            setDialogType(CUSTOM_DIALOG);
        }
        returnValue = CANCEL_OPTION;
        if (dialogType == OPEN_DIALOG && mode != DIRECTORIES_ONLY && FilePicker.available()) {
            String path = FilePicker.pick(pickerAccept());
            if (path == null) {
                cancelSelection();
            } else {
                File f = new File(path);
                selectedFiles = new File[] {f};
                selectedFile = f;
                File parentDir = f.getParentFile();
                if (parentDir != null) {
                    currentDirectory = parentDir;
                }
                approveSelection();
            }
            return returnValue;
        }
        JDialog d = createDialog(parent);
        dialog = d;
        rescanCurrentDirectory();
        d.setVisible(true);
        d.dispose();
        dialog = null;
        listing = null;
        where = null;
        nameField = null;
        return returnValue;
    }

    /// What the platform's picker is asked to offer: the extensions of an
    /// extension filter, or every file.
    private String pickerAccept() {
        if (!(fileFilter instanceof FileNameExtensionFilter)) {
            return null;
        }
        String[] ext = ((FileNameExtensionFilter) fileFilter).getExtensions();
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < ext.length; i++) {
            if (i > 0) {
                sb.append(',');
            }
            sb.append(ext[i]);
        }
        return sb.toString();
    }

    private String approveText() {
        if (approveButtonText != null) {
            return approveButtonText;
        }
        String key = dialogType == SAVE_DIALOG ? "FileChooser.saveButtonText" : "FileChooser.openButtonText";
        String s = UIManager.getString(key);
        if (s != null) {
            return s;
        }
        return dialogType == SAVE_DIALOG ? "Save" : "Open";
    }

    protected JDialog createDialog(Component parent) {
        Window owner = null;
        if (parent instanceof Window) {
            owner = (Window) parent;
        } else if (parent != null) {
            owner = SwingUtilities.getWindowAncestor(parent);
        }
        String title = dialogTitle != null ? dialogTitle : approveText();
        JDialog d = new JDialog(owner, title, Dialog.ModalityType.APPLICATION_MODAL);
        removeAll();
        setLayout(new BorderLayout(6, 6));
        setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));

        JPanel top = new JPanel(new BorderLayout(6, 0));
        JButton up = new JButton("..");
        up.setToolTipText("Up one level");
        up.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                changeToParentDirectory();
            }
        });
        where = new JLabel(" ");
        top.add(up, BorderLayout.WEST);
        top.add(where, BorderLayout.CENTER);
        add(top, BorderLayout.NORTH);

        listing = new JList<String>();
        listing.addListSelectionListener(new ListSelectionListener() {
            @Override
            public void valueChanged(ListSelectionEvent e) {
                File f = entryAt(listing == null ? -1 : listing.getSelectedIndex());
                if (f != null && nameField != null && (mode != FILES_ONLY || !f.isDirectory())) {
                    nameField.setText(f.getName());
                }
            }
        });
        listing.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() >= 2 && listing != null) {
                    open(entryAt(listing.getSelectedIndex()));
                }
            }
        });
        add(new JScrollPane(listing), BorderLayout.CENTER);

        JPanel bottom = new JPanel();
        bottom.setLayout(new BoxLayout(bottom, BoxLayout.Y_AXIS));
        nameField = new JTextField(20);
        if (selectedFile != null) {
            nameField.setText(selectedFile.getName());
        }
        nameField.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                approvePressed();
            }
        });
        bottom.add(nameField);
        if (filters.size() > 1) {
            String[] names = new String[filters.size()];
            for (int i = 0; i < names.length; i++) {
                names[i] = filters.get(i).getDescription();
            }
            final JComboBox<String> choice = new JComboBox<String>(names);
            int current = filters.indexOf(fileFilter);
            if (current >= 0) {
                choice.setSelectedItem(names[current]);
            }
            choice.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    int i = choice.getSelectedIndex();
                    if (i >= 0 && i < filters.size() && filters.get(i) != fileFilter) {
                        setFileFilter(filters.get(i));
                    }
                }
            });
            bottom.add(choice);
        }
        JPanel buttons = new JPanel();
        JButton approve = new JButton(approveText());
        approve.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                approvePressed();
            }
        });
        JButton cancel = new JButton(cancelText());
        cancel.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                cancelSelection();
            }
        });
        buttons.add(approve);
        buttons.add(cancel);
        bottom.add(buttons);
        add(bottom, BorderLayout.SOUTH);

        Container content = d.getContentPane();
        content.setLayout(new BorderLayout());
        content.add(this, BorderLayout.CENTER);
        d.getRootPane().setDefaultButton(approve);
        d.addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                returnValue = CANCEL_OPTION;
            }
        });
        d.setSize(380, 420);
        d.setLocationRelativeTo(parent);
        return d;
    }

    private static String cancelText() {
        String s = UIManager.getString("FileChooser.cancelButtonText");
        return s == null ? "Cancel" : s;
    }

    private File entryAt(int i) {
        return i >= 0 && i < entries.length ? entries[i] : null;
    }

    /// A double click on an entry: into a directory, or approve a file.
    private void open(File f) {
        if (f == null) {
            return;
        }
        if (f.isDirectory()) {
            if (nameField != null) {
                nameField.setText("");
            }
            setCurrentDirectory(f);
        } else {
            setSelectedFile(f);
            selectedFiles = new File[] {f};
            approveSelection();
        }
    }

    /// The approve button: the name typed, taken against the current
    /// directory. A directory named where only files can be chosen is
    /// entered instead of chosen.
    private void approvePressed() {
        String name = nameField == null ? "" : nameField.getText().trim();
        File chosen = null;
        if (name.length() > 0) {
            chosen = name.startsWith("/") || name.startsWith("file:") ? new File(name)
                    : new File(getCurrentDirectory(), name);
        } else if (mode != FILES_ONLY) {
            chosen = getCurrentDirectory();
        }
        if (chosen == null) {
            return;
        }
        if (mode == FILES_ONLY && chosen.isDirectory()) {
            nameField.setText("");
            setCurrentDirectory(chosen);
            return;
        }
        File[] many = null;
        if (multi && listing != null) {
            int[] picked = listing.getSelectedIndices();
            if (picked.length > 1) {
                many = new File[picked.length];
                for (int i = 0; i < picked.length; i++) {
                    File f = entryAt(picked[i]);
                    many[i] = f == null ? chosen : f;
                }
            }
        }
        selectedFile = chosen;
        selectedFiles = many != null ? many : new File[] {chosen};
        approveSelection();
    }

    public void approveSelection() {
        returnValue = APPROVE_OPTION;
        if (dialog != null) {
            dialog.setVisible(false);
        }
        fireActionPerformed(APPROVE_SELECTION);
    }

    public void cancelSelection() {
        returnValue = CANCEL_OPTION;
        if (dialog != null) {
            dialog.setVisible(false);
        }
        fireActionPerformed(CANCEL_SELECTION);
    }

    public void addActionListener(ActionListener l) {
        if (l != null) {
            listeners.add(l);
        }
    }

    public void removeActionListener(ActionListener l) {
        listeners.remove(l);
    }

    public ActionListener[] getActionListeners() {
        return listeners.toArray(new ActionListener[listeners.size()]);
    }

    protected void fireActionPerformed(String command) {
        ActionListener[] ls = getActionListeners();
        if (ls.length == 0) {
            return;
        }
        ActionEvent e = new ActionEvent(this, ActionEvent.ACTION_PERFORMED, command);
        for (int i = ls.length - 1; i >= 0; i--) {
            ls[i].actionPerformed(e);
        }
    }

    // ---- properties ----

    public int getDialogType() {
        return dialogType;
    }

    public void setDialogType(int dialogType) {
        if (dialogType != OPEN_DIALOG && dialogType != SAVE_DIALOG && dialogType != CUSTOM_DIALOG) {
            throw new IllegalArgumentException("Incorrect Dialog Type: " + dialogType);
        }
        int old = this.dialogType;
        this.dialogType = dialogType;
        if (dialogType != CUSTOM_DIALOG && old != dialogType) {
            approveButtonText = null;
        }
        firePropertyChange(DIALOG_TYPE_CHANGED_PROPERTY, old, dialogType);
    }

    public void setDialogTitle(String dialogTitle) {
        String old = this.dialogTitle;
        this.dialogTitle = dialogTitle;
        if (dialog != null) {
            dialog.setTitle(dialogTitle);
        }
        firePropertyChange(DIALOG_TITLE_CHANGED_PROPERTY, old, dialogTitle);
    }

    public String getDialogTitle() {
        return dialogTitle;
    }

    public void setApproveButtonText(String approveButtonText) {
        String old = this.approveButtonText;
        this.approveButtonText = approveButtonText;
        firePropertyChange(APPROVE_BUTTON_TEXT_CHANGED_PROPERTY, old, approveButtonText);
    }

    public String getApproveButtonText() {
        return approveButtonText;
    }

    public void setFileSelectionMode(int mode) {
        if (mode != FILES_ONLY && mode != DIRECTORIES_ONLY && mode != FILES_AND_DIRECTORIES) {
            throw new IllegalArgumentException("Incorrect Mode for file selection: " + mode);
        }
        int old = this.mode;
        this.mode = mode;
        firePropertyChange(FILE_SELECTION_MODE_CHANGED_PROPERTY, old, mode);
        rescanCurrentDirectory();
    }

    public int getFileSelectionMode() {
        return mode;
    }

    public boolean isFileSelectionEnabled() {
        return mode == FILES_ONLY || mode == FILES_AND_DIRECTORIES;
    }

    public boolean isDirectorySelectionEnabled() {
        return mode == DIRECTORIES_ONLY || mode == FILES_AND_DIRECTORIES;
    }

    public void setMultiSelectionEnabled(boolean b) {
        boolean old = multi;
        multi = b;
        firePropertyChange(MULTI_SELECTION_ENABLED_CHANGED_PROPERTY, old, b);
    }

    public boolean isMultiSelectionEnabled() {
        return multi;
    }

    public boolean isFileHidingEnabled() {
        return hiding;
    }

    public void setFileHidingEnabled(boolean b) {
        boolean old = hiding;
        hiding = b;
        firePropertyChange(FILE_HIDING_CHANGED_PROPERTY, old, b);
        rescanCurrentDirectory();
    }

    // ---- filters ----

    public void setFileFilter(FileFilter filter) {
        FileFilter old = fileFilter;
        fileFilter = filter;
        if (filter != null && !filters.contains(filter)) {
            filters.add(filter);
        }
        firePropertyChange(FILE_FILTER_CHANGED_PROPERTY, old, fileFilter);
        rescanCurrentDirectory();
    }

    public FileFilter getFileFilter() {
        return fileFilter;
    }

    public FileFilter[] getChoosableFileFilters() {
        return filters.toArray(new FileFilter[filters.size()]);
    }

    public void addChoosableFileFilter(FileFilter filter) {
        if (filter != null && !filters.contains(filter)) {
            filters.add(filter);
            firePropertyChange(CHOOSABLE_FILE_FILTER_CHANGED_PROPERTY, null, filter);
            if (fileFilter == null && filters.size() == 1) {
                setFileFilter(filter);
            }
        }
    }

    public boolean removeChoosableFileFilter(FileFilter f) {
        if (!filters.remove(f)) {
            return false;
        }
        if (fileFilter == f) {
            setFileFilter(useAcceptAll ? acceptAll : filters.isEmpty() ? null : filters.get(0));
        }
        firePropertyChange(CHOOSABLE_FILE_FILTER_CHANGED_PROPERTY, f, null);
        return true;
    }

    public void resetChoosableFileFilters() {
        filters.clear();
        fileFilter = null;
        if (useAcceptAll) {
            filters.add(acceptAll);
            fileFilter = acceptAll;
        }
        firePropertyChange(CHOOSABLE_FILE_FILTER_CHANGED_PROPERTY, null, null);
        rescanCurrentDirectory();
    }

    public FileFilter getAcceptAllFileFilter() {
        return acceptAll;
    }

    public boolean isAcceptAllFileFilterUsed() {
        return useAcceptAll;
    }

    public void setAcceptAllFileFilterUsed(boolean b) {
        boolean old = useAcceptAll;
        useAcceptAll = b;
        if (!b) {
            removeChoosableFileFilter(acceptAll);
        } else if (!filters.contains(acceptAll)) {
            filters.add(0, acceptAll);
        }
        firePropertyChange(ACCEPT_ALL_FILE_FILTER_USED_CHANGED_PROPERTY, old, b);
    }

    public boolean accept(File f) {
        return fileFilter == null || fileFilter.accept(f);
    }

    public String getName(File f) {
        return f == null ? null : f.getName();
    }

    public boolean isTraversable(File f) {
        return f != null && f.isDirectory();
    }

    // ------------------------------------------------------------ drag

    /// Records whether dragging out of the component is wanted. The layer
    /// starts no drag of its own, so this is a property and nothing more.
    public void setDragEnabled(boolean b) {
        cn1DragEnabled = b;
    }

    public boolean getDragEnabled() {
        return cn1DragEnabled;
    }

    private boolean cn1DragEnabled;
}
