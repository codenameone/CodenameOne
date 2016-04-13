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
        Rectangle clip;

        public AsyncOp(Rectangle clip)
        {
            this.clip = clip;
        }

        public void executeWithClip(WindowsGraphics underlying)
        {
            if (clip.getWidth() > 0 && clip.getHeight() > 0)
            {
                underlying.setClip(clip);
                execute(underlying);
                underlying.removeClip();
            }
        }

        public abstract void execute(WindowsGraphics underlying);
    }
}
