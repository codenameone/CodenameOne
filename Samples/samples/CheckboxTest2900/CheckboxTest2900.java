package com.codename1.samples;


import com.codename1.ui.Component;
import com.codename1.ui.Container;

import com.codename1.ui.Display;
import com.codename1.ui.Form;
import com.codename1.ui.Graphics;
import com.codename1.ui.Image;
import com.codename1.ui.List;
import com.codename1.ui.geom.Dimension;
import com.codename1.ui.layouts.BorderLayout;
import com.codename1.ui.layouts.BoxLayout;
import com.codename1.ui.layouts.FlowLayout;
import com.codename1.ui.plaf.DefaultLookAndFeel;
import com.codename1.ui.plaf.UIManager;
import com.codename1.ui.util.Resources;


class DCheckbox extends com.codename1.ui.CheckBox 
{	
	public DCheckbox(String string, boolean b) { super(string); setSelected(b); }

	public void setState(boolean b) { setSelected(b); }

}



public class CheckboxTest2900  {
	
    /**
     * Draw an image from/to particular rectangles with rescaling.  Note that
     * this uses left,top,right,bottomt coordinates rather than left,top,width,height 
     * @param gc	the gc to be written to
     * @param im	the image to be written from
     * @param dx	dest left
     * @param dy	dest top
     * @param dx2 dest right
     * @param dy2 dest bottom
     * @param fx  source left
     * @param fy  source top
     * @param fx2 source right
     * @param fy2 source bottom
     * @param c   the image observer (not used in codename1) 
     */
    public static void drawImage(Graphics gc,Image im,
			int dx,int dy,int dx2,int dy2,
			int fx,int fy,int fx2,int fy2
			)
{	if(gc!=null)
	{	int w = dx2-dx;
		int h = dy2-dy;
		int sw = fx2-fx;
		int sh = fy2-fy;
		int imw = im.getWidth();
		int imh = im.getHeight();
		double xscale = w/(double)sw;
		double yscale = h/(double)sh;
	   	int[]clip = gc.getClip();

		if(clip!=null && (clip instanceof int[]) && (clip.length>=4))
		{
	   	gc.clipRect(dx,dy,w,h);			// combine with proper clipping region
	   	//gc.setClip(dx,dy,w,h);		// runs wild, can write anywhere!
	   	int finx = dx-(int)(fx*xscale);
	   	int finy = dy-(int)(fy*yscale);
	   	int finw = (int)(imw*xscale);
	   	int finh = (int)(imh*yscale);
	   	gc.drawImage(im,finx,finy,finw,finh);
	   	gc.setClip(clip);
		}

	}
}	
 
    private Form current;
    @SuppressWarnings("unused")
	private Resources theme;
    private Image background;
    public void init(Object context) {
        theme = UIManager.initFirstTheme("/theme");
        // Pro only feature, uncomment if you have a pro subscription
        // Log.bindCrashProtection(true);
    }
    int loops = 0;
   
    public void start() {
        if(current != null){
            current.show();
            return;
        }
        Form hi = new Form("Hi >>0 World",new BorderLayout(BorderLayout.CENTER_BEHAVIOR_CENTER_ABSOLUTE)); 

        UIManager man = UIManager.getInstance();
  	man.setLookAndFeel(new DefaultLookAndFeel(man));
  	  	
        current = hi;
        Container vpanel = new Container();
        current.add(BorderLayout.CENTER,vpanel);

        vpanel.setLayout(new FlowLayout());//new BoxLayout(BoxLayout.Y_AXIS));
         
 
        for(int i=0;i<20;i++)
         {
             DCheckbox cb1 = new DCheckbox("Test Checkbox #"+i,true);
             cb1.setOppositeSide((i&1)==0);	 
             vpanel.add(cb1);
         }

         hi.show();
        Runnable rr = new Runnable (){
        	public void run() {
        	System.out.println("running");
        	while(true)
        	{
        	hi.repaint();
        	try {
        	Thread.sleep(1);
        	}

        		  catch (InterruptedException e) {};
        		}}};
        	new Thread(rr).start();
   
    }

    public void stop() {
        current = Display.getInstance().getCurrent();
    }
    
    public void destroy() {
    }
    


}
