package com.codename1.samples;

import com.codename1.io.Log;
import com.codename1.ui.Button;
import static com.codename1.ui.CN.requestFullScreen;
import static com.codename1.ui.CN.updateNetworkThreadCount;
import com.codename1.ui.ComboBox;
import static com.codename1.ui.ComponentSelector.$;
import com.codename1.ui.Form;
import com.codename1.ui.TextArea;
import com.codename1.ui.Toolbar;
import com.codename1.ui.UIFragment;
import com.codename1.ui.layouts.BorderLayout;
import com.codename1.ui.layouts.BoxLayout;
import com.codename1.ui.plaf.RoundRectBorder;
import com.codename1.ui.plaf.UIManager;
import com.codename1.ui.util.Resources;

public class UIFragmentSample {
    Resources theme;
    public void init(Object o){ 
         // use two network threads instead of one
        updateNetworkThreadCount(2);
        requestFullScreen();

        theme = UIManager.initFirstTheme("/theme");

        // Enable Toolbar on all Forms by default
        Toolbar.setGlobalToolbar(true);

        // Pro only feature
        Log.bindCrashProtection(true);
    }
    
    public void start(){
        testUIFragment();
    }
    
    public void stop() {
        
    }
    
    public void destroy() {
        
    }
    
    private void testUIFragment() {
        Form f = new Form("Test Fragments", BoxLayout.y());
        TextArea ta = new TextArea();
        ta.setMaxSize(5000);
        
        String[] examples = new String[]{
            "<borderAbs><$button1 constraint='center'/><xng constraint='south'><$button2/><$button3/><$button4/></xng></borderAbs>",
            
            "{centerAbs:$button1, south:{xng:[$button2, $button3, $button4]}}",
            
            "{cs:$button1, south:{xng:[$button2, $button3, $button4]}}",
            
            "{ca:$button1, south:{xng:[$button2, $button3, $button4]}}",
            
            "{centerScale:$button1, south:{xng:[$button2, $button3, $button4]}}",
            
            "{ctb:$button1, south:{xng:[$button2, $button3, $button4]}}",
            
            "{centerTotalBelow:$button1, south:{xng:[$button2, $button3, $button4]}}",
            
            "{s:$button1, c:{xng:[$button2, $button3, $button4]}}",
            
            "{s:$button1, c:{x:[$button2, $button3, $button4]}}",
            
            "{s:$button1, c:{y:[$button2, $button3, $button4]}}",
            
            "{s:$button1, c:{yBottomLast:[$button2, $button3, $button4]}}",
            
            "{s:$button1, c:{ybl:[$button2, $button3, $button4]}}"
            
            
            
            
        };
        
        ComboBox cb = new ComboBox(examples);
        cb.addActionListener(e->{
            ta.setText(examples[cb.getSelectedIndex()]);
        });
        
        ta.setText("<borderAbs><$button1 constraint='center'/><xng constraint='south'><$button2/><$button3/><$button4/></xng></borderAbs>");
        Button b = new Button("Compile");
        b.addActionListener(e->{
            Form f2 = new Form("Result", new BorderLayout());
            f2.setToolbar(new Toolbar());
            f2.setTitle("Result");
            f2.setBackCommand("Back", null, evt->{
                f.showBack();
            });
            f2.getToolbar().addCommandToLeftBar("Back", null, evt->{
                f.showBack();
            });
            Button b1 = new Button("Button 1");
            Button b2 = new Button("Button 2");
            Button b3 = new Button("Button 3");
            Button b4 = new Button("Button 4");
            $(b1, b2, b3, b4).selectAllStyles().setBorder(RoundRectBorder.create().cornerRadius(2)).setBgColor(0x003399).setBgTransparency(0xff);
            UIFragment frag;
            if (ta.getText().charAt(0) == '<') {
                frag = UIFragment.parseXML(ta.getText());
            } else {
                System.out.println("Parsing "+ta.getText());
                frag = UIFragment.parseJSON(ta.getText());
                
            }
            f2.add(BorderLayout.CENTER,frag 
                    .set("button1", b1)
                    .set("button2", b2)
                    .set("button3", b3)
                    .set("button4", b4)
                    .getView()
            );
            f2.show();
        });
        ta.setRows(5);
        
        
        f.addAll(cb, ta, b);
        
        
        f.show();
    }
}
