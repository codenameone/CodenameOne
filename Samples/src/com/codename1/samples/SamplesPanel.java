/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.codename1.samples;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.EventQueue;
import java.awt.FlowLayout;
import java.awt.Point;
import java.awt.Window;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.HashMap;
import java.util.Map;
import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;
import javax.swing.border.BevelBorder;

/**
 *
 * @author shannah
 */
public class SamplesPanel extends JPanel {
    private JFrame dlg;
    /**
     * @return the delegate
     */
    public Delegate getDelegate() {
        return delegate;
    }

    /**
     * @param delegate the delegate to set
     */
    public void setDelegate(Delegate delegate) {
        this.delegate = delegate;
    }
    private SampleList samples;
    private Delegate delegate;
    private JPanel samplesWrapper;

    public void setSamples(SampleList samples) {
        this.samples = samples;
        update();
        revalidate();
        repaint();
        
    }
    
    public static interface Delegate {
        public void launchSample(Sample sample);
        public void launchJSSample(Sample sample);
        public void createNewSample();
        public void viewSource(Sample sample);
        public void searchChanged(String newSearch);
        public void editGlobalBuildHints();

        public void editPrivateBuildHints(Sample sample);

        public void launchIOSDebug(Sample sample);

        public void stopProcess(Process p, String name);

        public void sendWindowsDesktopBuild(Sample sample);

        public void editPublicBuildHints(Sample sample);

        public void editCSSFile(Sample sample);

        public void refreshCSS(Sample sample);

        public void sendMacDesktopBuild(Sample sample);

        public void launchAndroid(Sample sample);

        public void exportAsNetbeansProject(Sample sample);

        public void launchIOSRelease(Sample sample);

        public void launchUWP(Sample sample);
    }
    public SamplesPanel(SampleList list) {
        setLayout(new BorderLayout());
        this.samples = list;
        initUI();
        update();
    }
    
    private JComponent wrapFlowLeft(JComponent cmp) {
        JPanel out = new JPanel(new FlowLayout(FlowLayout.LEFT));
        out.setOpaque(false);
        out.add(cmp);
        return out;
    }
    
    private void initUI() {
        JTextField searchField = new JTextField();
        searchField.addActionListener(e->{
            if (delegate != null) {
                delegate.searchChanged(searchField.getText());
            }
        });
        searchField.setToolTipText("Search for samples containing text");
        JLabel searchLabel = new JLabel("Search");
        JLabel descriptionLabel = new JLabel("Find samples containing your search term.");
        JPanel searchPanel = new JPanel(new BorderLayout());
        searchPanel.add(wrapFlowLeft(searchLabel), BorderLayout.WEST);
        JPanel searchCenter = new JPanel();
        searchCenter.setLayout(new BoxLayout(searchCenter, BoxLayout.Y_AXIS));
        searchCenter.add(searchField);
        searchCenter.add(descriptionLabel);
        searchPanel.add(searchCenter, BorderLayout.CENTER);
        searchPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        add(searchPanel, BorderLayout.NORTH);
        
        samplesWrapper = new JPanel();
        samplesWrapper.setLayout(new BoxLayout(samplesWrapper, BoxLayout.Y_AXIS));
        JScrollPane scrollPane = new JScrollPane();
        scrollPane.setViewportView(samplesWrapper);
        add(scrollPane, BorderLayout.CENTER);
               
    }
    
    
    private void update() {
        samplesWrapper.removeAll();
        samples.sort();
        
        for (Sample sample : samples) {
            //System.out.println("Updating with "+sample.getName());
            samplesWrapper.add(createCell(sample));
        }
    }
    
    private void preventDoubleClick(JButton btn) {
        btn.setEnabled(false);
        btn.repaint();
        javax.swing.Timer timer = new javax.swing.Timer(5000, e->{
           btn.setEnabled(true);
           btn.repaint();
        });
        timer.start();
    }
    
    private JComponent createCell(Sample sample) {
        JPanel wrapper = new JPanel(new BorderLayout());
        JLabel name = new JLabel(sample.getName());
        JButton launch = new JButton("Launch");
        launch.setToolTipText("Run this sample in the Codename One simulator");
        launch.addActionListener(e->{
            preventDoubleClick(launch);
            if (delegate != null) {
                delegate.launchSample(sample);
            }
        });
        JButton viewSource = new JButton("View Source");
        viewSource.addActionListener(e->{
            preventDoubleClick(viewSource);
            if (delegate != null) {
                delegate.viewSource(sample);
            }
        });
        JMenuItem launchJS = new JMenuItem("Launch JS");
        launchJS.setToolTipText("Build and run sample in web browser.  This will take 1 to 2 minutes to complete the build.");
        launchJS.addActionListener(e->{
            //preventDoubleClick(launchJS);
            if (delegate != null) {
                delegate.launchJSSample(sample);
            }
        });
        
        JMenuItem launchIOS = new JMenuItem("Send iOS Debug Build");
        launchIOS.setToolTipText("Send iOS debug build.");
        launchIOS.addActionListener(e->{
            if (delegate != null) {
                delegate.launchIOSDebug(sample);
            }
        });
        
        JMenuItem launchIOSRelease = new JMenuItem("Send iOS Release Build");
        launchIOSRelease.setToolTipText("Send iOS release build.");
        launchIOSRelease.addActionListener(e->{
            if (delegate != null) {
                delegate.launchIOSRelease(sample);
            }
        });
        
        JMenuItem launchAndroid = new JMenuItem("Send Android Build");
        launchAndroid.setToolTipText("Send Android build.");
        launchAndroid.addActionListener(e->{
            if (delegate != null) {
                delegate.launchAndroid(sample);
            }
        });
        
        JMenuItem launchUWP = new JMenuItem("Send UWP Build");
        launchUWP.setToolTipText("Send UWP build.");
        launchUWP.addActionListener(e->{
            if (delegate != null) {
                delegate.launchUWP(sample);
            }
        });
        
        JMenuItem winDesktopBuild = new JMenuItem("Send Windows Desktop Build");
        winDesktopBuild.setToolTipText("Send Windows desktop build.");
        winDesktopBuild.addActionListener(e->{
            if (delegate != null) {
                delegate.sendWindowsDesktopBuild(sample);
            }
        });
        
        JMenuItem macDesktopBuild = new JMenuItem("Send Mac Desktop Build");
        macDesktopBuild.setToolTipText("Send Mac desktop build.");
        macDesktopBuild.addActionListener(e->{
            if (delegate != null) {
                delegate.sendMacDesktopBuild(sample);
            }
        });
        
        JMenuItem editPrivateBuildHints = new JMenuItem("Edit Private Build Hints");
        editPrivateBuildHints.setToolTipText("Edit the private custom build hints for this sample.");
        editPrivateBuildHints.addActionListener(e->{
            if (delegate != null) {
                delegate.editPrivateBuildHints(sample);
            }
        });
        
        JMenuItem editPublicBuildHints = new JMenuItem("Edit Public Build Hints");
        editPublicBuildHints.setToolTipText("Edit the public custom build hints for this sample.");
        editPublicBuildHints.addActionListener(e->{
            if (delegate != null) {
                delegate.editPublicBuildHints(sample);
            }
        });
        
        JMenuItem editCSS = new JMenuItem("Edit CSS File");
        editCSS.setToolTipText("Edit the CSS file for this sample.");
        editCSS.addActionListener(e->{
            if (delegate != null) {
                delegate.editCSSFile(sample);
            }
        });
        
        JMenuItem refreshCSS = new JMenuItem("Refresh CSS File");
        refreshCSS.setToolTipText("Update the CSS file in the currently running sample.  This should update the styles in the sample automatically.  May take a few seconds.");
        refreshCSS.addActionListener(e->{
            if (delegate != null) {
                delegate.refreshCSS(sample);
            }
        });
        
        JMenu export = new JMenu("Export as...");
        export.setToolTipText("Export this sample as an IDE project");
        
        JMenuItem exportNB = new JMenuItem("Netbeans");
        exportNB.setToolTipText("Export this sample as a Netbeans project");
        exportNB.addActionListener(e->{
            if (delegate != null) {
                delegate.exportAsNetbeansProject(sample);
            }
        });
        export.add(exportNB);
        
        JButton more = new JButton("More...");
        JPopupMenu moreMenu = new JPopupMenu("More...");
        more.addActionListener(e->{
            EventQueue.invokeLater(()->{
                Point p = more.getLocationOnScreen();
                moreMenu.show(more, 0, 0);
                moreMenu.setLocation(p.x, p.y+more.getHeight());
            });
            
        });
        moreMenu.add(launchJS);
        moreMenu.add(launchIOS);
        moreMenu.add(launchIOSRelease);
        moreMenu.add(launchAndroid);
        moreMenu.add(launchUWP);
        moreMenu.add(winDesktopBuild);
        moreMenu.add(macDesktopBuild);
        moreMenu.addSeparator();
        moreMenu.add(editPrivateBuildHints);
        moreMenu.add(editPublicBuildHints);
        moreMenu.addSeparator();
        moreMenu.add(editCSS);
        moreMenu.add(refreshCSS);
        moreMenu.addSeparator();
        moreMenu.add(export);
        
        JPanel buttons = new JPanel(new FlowLayout());
        buttons.setOpaque(false);
        buttons.add(viewSource);
        buttons.add(launch);
        buttons.add(more);
        
        wrapper.add(buttons, BorderLayout.EAST);
        
        
        wrapper.add(name, BorderLayout.CENTER);
        
        wrapper.setBackground(Color.white);
        wrapper.setBorder(BorderFactory.createBevelBorder(BevelBorder.RAISED));
        Dimension launchPrefSize = launch.getPreferredSize();
        wrapper.setMaximumSize(new Dimension(10000, launchPrefSize.height * 2));
        int foo = 5;
        return wrapper;
        
    }
    
    private JMenuBar createMenuBar() {
        menu = new JMenuBar();
        JMenuItem addSample = new JMenuItem("Create New Sample");
        addSample.addActionListener(e->{
            if (delegate != null) {
                delegate.createNewSample();
            }
        });
        
        JMenuItem editGlobalBuildHints = new JMenuItem("Edit Global Build Hints");
        editGlobalBuildHints.addActionListener(e->{
            if (delegate != null) {
                delegate.editGlobalBuildHints();
            }
        });
        
        JMenu fileMenu = new JMenu("File");
        fileMenu.add(addSample);
        fileMenu.add(editGlobalBuildHints);
        menu.add(fileMenu);
        
        
        return menu;
        
        
    }
    
    public JFrame showDialog(Window owner) {
        if (dlg == null) {
            dlg = new JFrame("Codename One Samples");
            dlg.setJMenuBar(createMenuBar());
            dlg.getContentPane().setLayout(new BorderLayout());
            dlg.getContentPane().add(this, BorderLayout.CENTER);
            dlg.setMinimumSize(new Dimension(640, 480));
            dlg.pack();
            
        }
        dlg.setVisible(true);
        return dlg;
        
    }
    
    private JMenuBar menu;
    private JMenu processMenu;
    private Map<Process, JMenuItem> runningProcessMap = new HashMap<Process, JMenuItem>();
    
    
    private void updateProcessMenu() {
        if (processMenu == null) {
            processMenu = new JMenu("Running Processes");
            menu.add(processMenu);
        }
        processMenu.removeAll();
        for (Process p : runningProcessMap.keySet()) {
            processMenu.add(runningProcessMap.get(p));
        }
        revalidate();
        repaint();
        processMenu.revalidate();
        processMenu.repaint();
    }
    
    public void addProcess(Process p, String name) {
        if (!EventQueue.isDispatchThread()) {
            EventQueue.invokeLater(()->{
                addProcess(p, name);
            });
            
            return;
        }
        
        JMenuItem jmi = new JMenuItem(name);
        jmi.addActionListener(e->{
            if (delegate != null) {
                delegate.stopProcess(p, name);
            }
        });
        runningProcessMap.put(p, jmi);
        updateProcessMenu();
    }
    
    public void removeProcess(Process p) {
        if (!EventQueue.isDispatchThread()) {
            EventQueue.invokeLater(()->{
                removeProcess(p);
            });
            
            return;
        }
        runningProcessMap.remove(p);
        updateProcessMenu();
    }
    
    
}
