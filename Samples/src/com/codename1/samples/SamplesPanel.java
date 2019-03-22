/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.codename1.samples;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Window;
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
import javax.swing.JPanel;
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
        public void createNewSample();
        public void viewSource(Sample sample);
        public void searchChanged(String newSearch);
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
        
        
        for (Sample sample : samples) {
            //System.out.println("Updating with "+sample.getName());
            samplesWrapper.add(createCell(sample));
        }
    }
    
    private JComponent createCell(Sample sample) {
        JPanel wrapper = new JPanel(new BorderLayout());
        JLabel name = new JLabel(sample.getName());
        JButton launch = new JButton("Launch Sample");
        launch.addActionListener(e->{
            if (delegate != null) {
                delegate.launchSample(sample);
            }
        });
        JButton viewSource = new JButton("View Source");
        viewSource.addActionListener(e->{
            if (delegate != null) {
                delegate.viewSource(sample);
            }
        });
        
        
        JPanel buttons = new JPanel(new FlowLayout());
        buttons.setOpaque(false);
        buttons.add(viewSource);
        buttons.add(launch);
        
        wrapper.add(buttons, BorderLayout.EAST);
        
        
        wrapper.add(name, BorderLayout.CENTER);
        
        wrapper.setBackground(Color.white);
        wrapper.setBorder(BorderFactory.createBevelBorder(BevelBorder.RAISED));
        wrapper.setMaximumSize(new Dimension(10000, launch.getPreferredSize().height * 2));
        return wrapper;
        
    }
    
    private JMenuBar createMenuBar() {
        JMenuBar menu = new JMenuBar();
        JMenuItem addSample = new JMenuItem("Create New Sample");
        addSample.addActionListener(e->{
            if (delegate != null) {
                delegate.createNewSample();
            }
        });
        JMenu fileMenu = new JMenu("File");
        fileMenu.add(addSample);
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
}
