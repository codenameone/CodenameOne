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
        private com.codename1.ui.Transform clipTransform;

        public Rectangle clip
        {
            set
            {
                actualClip = value;
                
                clipX = actualClip.getX();
                clipY = actualClip.getY();
                clipW = actualClip.getWidth();
                clipH = actualClip.getHeight();
                clipTransform = destination.getTransform();
                destination.setClip(value);
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
            clipTransform = null;
        }

        public int getClipX() { return clipX; }
        public int getClipY() { return clipY; }
        public int getClipW() { return clipW; }
        public int getClipH() { return clipH; }

        public com.codename1.ui.geom.Shape getClipProjection()
        {
            com.codename1.ui.Transform t = destination.getTransform();
            com.codename1.ui.geom.Shape s = actualClip;
            if (clipTransform != null && !clipTransform.isIdentity())
            {
                com.codename1.ui.geom.GeneralPath gp = new com.codename1.ui.geom.GeneralPath();
                gp.setShape(s, clipTransform.getInverse());
                s = gp;

            }
            if (t != null && !t.isIdentity()) {
                com.codename1.ui.geom.GeneralPath gp = new com.codename1.ui.geom.GeneralPath();
                gp.setShape(s, t);
                s = gp;
            }
            return s;
        }
    
    }
}
