using com.codename1.ui;
using com.codename1.ui.geom;
using System;

namespace com.codename1.impl
{
    public class NativeGraphics : global::java.lang.Object
    {
        public WindowsGraphics destination;
        private int clipX, clipY, clipW, clipH;
        private Rectangle actualClip;

        public Rectangle clip
        {
            set
            {
                actualClip = value;
                destination.setClip(value);
                clipX = actualClip.getX();
                clipY = actualClip.getY();
                clipW = actualClip.getWidth();
                clipH = actualClip.getHeight();
            }
            get { return actualClip; }
        }
        private NativeFont actualFont;
        public NativeFont font
        {
            set
            {
                actualFont = value;
                destination.setFont(value.font);
            }
            get { return actualFont; }
        }

        public void resetClip()
        {
            Rectangle r = new Rectangle(0, 0, Convert.ToInt32(SilverlightImplementation.screen.ActualWidth), Convert.ToInt32(SilverlightImplementation.screen.ActualHeight));
            clip = r;
        }

        public int getClipX() { return clipX; }
        public int getClipY() { return clipY; }
        public int getClipW() { return clipW; }
        public int getClipH() { return clipH; }
    
    }
}
