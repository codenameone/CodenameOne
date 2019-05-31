/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.codename1.samples;

import static com.codename1.samples.PropertiesUtil.saveProperties;
import com.codename1.samples.SamplesPanel.Delegate;
import java.awt.Desktop;
import java.awt.EventQueue;
import java.awt.FileDialog;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Properties;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;
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
            Process p = sample.getThreadLocalProcess();
            if (p != null) {
                view.removeProcess(p);
            }
        }).start();
    }
    
    @Override
    public void sendWindowsDesktopBuild(Sample sample) {
        new Thread(()->{
            try {
                sample.sendWindowsDesktopBuild(ctx);
            } catch (Exception ex) {
                ex.printStackTrace();
            }
            Process p = sample.getThreadLocalProcess();
            if (p != null) {
                view.removeProcess(p);
            }
        }).start();
    }
    
     @Override
    public void sendMacDesktopBuild(Sample sample) {
        new Thread(()->{
            try {
                sample.sendMacDesktopBuild(ctx);
            } catch (Exception ex) {
                ex.printStackTrace();
            }
            Process p = sample.getThreadLocalProcess();
            if (p != null) {
                view.removeProcess(p);
            }
        }).start();
    }

    
    @Override
    public void launchJSSample(Sample sample) {
        new Thread(()->{
            try {
                sample.runJavascript(ctx, p->{
                    view.addProcess(p, sample.getName());
                });
                
            } catch (Exception ex) {
                ex.printStackTrace();
            }
            Process p = sample.getThreadLocalProcess();
            if (p != null) {
                view.removeProcess(p);
            }
        }).start();
    }

    @Override
    public void createNewSample() {
        String name = JOptionPane.showInputDialog(view, "Enter a sample name", "MySample");
        if (name == null || name.isEmpty()) {
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

    @Override
    public void editGlobalBuildHints() {
        ctx.getConfigDir().mkdirs();
        File configFile = ctx.getGlobalPrivateCodenameOneSettingsFile();
        if (!configFile.exists()) {
            try (FileOutputStream fos = new FileOutputStream(configFile)) {
                Properties props = ctx.getGlobalPrivateCodenameOneSettingsProperties();
                props.store(fos, "Add your custom codenameone_settings.properties key/value pairs to be used for building samples");
            } catch (Exception ex) {
                ex.printStackTrace();
                
            }
        }
        try {
            if (Desktop.isDesktopSupported()) {
                Desktop.getDesktop().edit(configFile);
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    @Override
    public void editPrivateBuildHints(Sample sample) {
        sample.getPrivateConfigDir(ctx).mkdirs();
        
        File configFile = sample.getPrivateCodenameOneSettingsFile(ctx);
        if (!configFile.exists()) {
            
            try {
                saveProperties(new Properties(), configFile);
            } catch (Exception ex) {
                ex.printStackTrace();
                
            }
        }
        try {
            if (Desktop.isDesktopSupported()) {
                Desktop.getDesktop().edit(configFile);
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }
    
    @Override
    public void editPublicBuildHints(Sample sample) {
        sample.getPrivateConfigDir(ctx).mkdirs();
        
        File configFile = sample.getPublicCodenameOneSettingsFile(ctx);
        if (!configFile.exists()) {
            
            try {
                saveProperties(new Properties(), configFile);
            } catch (Exception ex) {
                ex.printStackTrace();
                
            }
        }
        try {
            if (Desktop.isDesktopSupported()) {
                Desktop.getDesktop().edit(configFile);
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }
    
    @Override
    public void editCSSFile(Sample sample) {
        try {
            if (!sample.isCSSProject(ctx)) {
                int res = JOptionPane.showConfirmDialog(view, "This sample doesn't currently use CSS. Activate CSS now?", "Activate CSS?", JOptionPane.YES_NO_OPTION);
                if (res == JOptionPane.YES_OPTION) {
                    sample.activateCSS(ctx);
                } else {
                    return;
                }
            }
            if (Desktop.isDesktopSupported()) {
                Desktop.getDesktop().edit(sample.getThemeCSSFile(ctx));
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    @Override
    public void refreshCSS(Sample sample) {
        try {
            if (sample.isCSSProject(ctx)) {
                sample.refreshCSS(ctx);
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }



    @Override
    public void launchIOSDebug(Sample sample) {
        new Thread(()->{
            try {
                sample.buildIOSDebug(ctx);
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        }).start();
    }
    
    @Override
    public void launchIOSRelease(Sample sample) {
        new Thread(()->{
            try {
                sample.buildIOSRelease(ctx);
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        }).start();
    }

    

    
    @Override
    public void launchAndroid(Sample sample) {
        new Thread(()->{
            try {
                sample.buildAndroid(ctx);
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        }).start();
    }

    @Override
    public void launchUWP(Sample sample) {
        new Thread(()->{
            try {
                sample.buildUWP(ctx);
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        }).start();
    }
    
    

    @Override
    public void stopProcess(Process p, String name) {
        int res = JOptionPane.showConfirmDialog(view, "Stop process "+name+"?");
        if (res == JOptionPane.OK_OPTION) {
            try {
                p.destroy();
                JOptionPane.showMessageDialog(view, "Process has been stopped");
                view.removeProcess(p);
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        }
    }

    @Override
    public void exportAsNetbeansProject(Sample sample) {
        FileDialog fileSelector = new FileDialog((JFrame)SwingUtilities.getWindowAncestor(view), "Select Destination");
        fileSelector.setMode(FileDialog.SAVE);
        fileSelector.setVisible(true);
        String selectedFile = fileSelector.getFile();
        if (selectedFile == null) {
            return;
        }
        File f = new File(new File(fileSelector.getDirectory()), selectedFile);
        System.out.println(f);
        new Thread(()->{
            try {
                sample.exportAsNetbeansProject(ctx, f);
            } catch (Exception ex) {
                ex.printStackTrace();
                JOptionPane.showMessageDialog(view, ex.getMessage(), "Export Failed", JOptionPane.ERROR_MESSAGE);

            }
        }).start();
        
        
    }

    

    
    
    
    
}
