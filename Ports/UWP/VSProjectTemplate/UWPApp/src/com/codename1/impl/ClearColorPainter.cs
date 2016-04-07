using com.codename1.ui.geom;
using System;
using System.Collections.Generic;
using System.Linq;
using System.Text;

namespace com.codename1.impl
{
    class ClearColorPainter : AsyncOp
    {
        private Rectangle clip;
        private int color;
        private int alpha;

        public ClearColorPainter(Rectangle clip, int color, int alpha) : base(clip)
        {
            this.clip = clip;
            this.color = color;
            this.alpha = alpha;
        }

        public override void execute(WindowsGraphics underlying)
        {
            underlying.setColor(color);
            underlying.setAlpha(alpha);
            underlying.clear();
        }
    }
}
