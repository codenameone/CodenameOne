using com.codename1.impl;
using com.codename1.ui.geom;
using System;
using System.Collections.Generic;
using System.Linq;
using System.Text;

namespace com.codename1.impl
{
    public abstract class AsyncOp
    {
        Shape clip;

        public AsyncOp(Shape clip)
        {
            this.clip = clip;
        }

        public void executeWithClip(WindowsGraphics underlying)
        {
            if (clip == null)
            {
                execute(underlying);
                return;
            }

            if (clip.getBounds().getWidth() > 0 && clip.getBounds().getHeight() > 0)
            {
                //underlying.setRawClip(clip.getBounds());
                execute(underlying);
                //underlying.removeClip();
            }
           
            
        }

        public abstract void execute(WindowsGraphics underlying);
    }
}
