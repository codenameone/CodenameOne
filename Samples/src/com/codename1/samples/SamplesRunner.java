/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.codename1.samples;

import com.codename1.samples.SamplesPanel.Delegate;
import java.awt.Desktop;
import java.awt.EventQueue;
import java.io.IOException;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.SwingWorker;

/**
 *
 * @author shannah
 */
public class SamplesRunner implements SamplesPanel.Delegate {

    private SamplesContext ctx;
    private SamplesPanel view;
    private String currentSearch;
    
    public static void main(String[] args)throws Exception {
        new SamplesRunner().launch(args);
    }
    /**
     * @param args the command line arguments
     */
    public void launch(String[] args) throws Exception  {
        // TODO code application logic here
        ctx = SamplesContext.createSystemContext();
        SampleList samples = ctx.loadSamples();
        if (currentSearch != null && !currentSearch.isEmpty()) {
            samples = samples.filter(ctx, currentSearch);
        }
        final SampleList fSamples = samples;
        EventQueue.invokeLater(()->{
            view= new SamplesPanel(fSamples);
            view.setDelegate(this);
            JFrame dlg = view.showDialog(null);
            dlg.setDefaultCloseOperation(JDialog.EXIT_ON_CLOSE);
            
        });
        
    }

    @Override
    public void launchSample(Sample sample) {
        new Thread(()->{
            try {
                sample.run(ctx);
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        }).start();
    }

    @Override
    public void createNewSample() {
        String name = JOptionPane.showInputDialog(view, "Enter a sample name", "MySample");
        if (name.isEmpty()) {
            return;
        }
        try {
            Sample sample = new Sample(name);
            sample.save(ctx, getSampleTemplate(name));
            sample.openJavaSourceFile(ctx);
            ctx = SamplesContext.createSystemContext();
            SampleList samples = ctx.loadSamples();
            view.setSamples(samples);
            
        } catch (Exception ex) {
            ex.printStackTrace();
            JOptionPane.showMessageDialog(view, "Failed to save sample: "+ex.getMessage(), "Failed", JOptionPane.ERROR_MESSAGE);
        }
        
        
        
        
        
    }
    
    private String getSampleTemplate(String name) throws IOException {
        String tpl = IOUtil.readToString(SamplesRunner.class.getResourceAsStream("MyApplication.txt"));
        tpl = tpl.replace("{{MyApplication}}", name);
        return tpl;
    }

    @Override
    public void viewSource(Sample sample) {
        try {
            sample.openJavaSourceFile(ctx);
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    public void updateSearch() {
        try {
            SwingWorker worker = new SwingWorker() {
                SampleList samples;
                @Override
                protected Object doInBackground() throws Exception {
                    samples = ctx.loadSamples();
                    if (currentSearch != null && !currentSearch.isEmpty()) {
                        //System.out.println("Filtering samples on "+currentSearch);
                        samples = samples.filter(ctx, currentSearch);
                    }
                    return null;
                }

                @Override
                protected void done() {
                    //System.out.println("Updating samples");
                    view.setSamples(samples);
                }
                
                
                
            };
            worker.execute();
            
            
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }
    
    @Override
    public void searchChanged(String newSearch) {
        currentSearch = newSearch;
        updateSearch();
    }
    
}
