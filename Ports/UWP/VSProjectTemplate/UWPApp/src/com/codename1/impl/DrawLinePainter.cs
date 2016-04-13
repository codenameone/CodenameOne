using com.codename1.ui.geom;
using System;
using System.Collections.Generic;
using System.Linq;
using System.Text;

namespace com.codename1.impl
{
    class DrawLinePainter : AsyncOp
    {
        private Rectangle clip;
        private int x1;
        private int y1;
        private int x2;
        private int y2;
        private int color;
        private int alpha;

        public DrawLinePainter(Rectangle clip, int x1, int y1, int x2, int y2, int color, int alpha)
            : base(clip)
        {
            this.clip = clip;
            this.x1 = x1;
            this.y1 = y1;
            this.x2 = x2;
            this.y2 = y2;
            this.color = color;
            this.alpha = alpha;
        }

        public override void execute(WindowsGraphics underlying)
        {
            underlying.setColor(color);
            underlying.setAlpha(alpha);
            underlying.drawLine(x1, y1, x2, y2);
        }
    }
}
