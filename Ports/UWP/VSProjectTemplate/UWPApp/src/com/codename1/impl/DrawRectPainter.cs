using com.codename1.ui.geom;
using System;
using System.Collections.Generic;
using System.Linq;
using System.Text;

namespace com.codename1.impl
{
    class DrawRectPainter : AsyncOp
    {
        private Rectangle clip;
        private int x;
        private int y;
        private int w;
        private int h;
        private int stroke;
        private int color;
        private int alpha;

        public DrawRectPainter(Rectangle clip, int x, int y, int w, int h, int stroke, int color, int alpha) : base(clip)
        {
            this.clip = clip;
            this.x = x;
            this.y = y;
            this.w = w;
            this.h = h;
            this.stroke = stroke;
            this.color = color;
            this.alpha = alpha;
        }
        public override void execute(WindowsGraphics underlying)
        {
            underlying.setColor(color);
            underlying.setAlpha(alpha);
            underlying.drawRect(x, y, w, h, stroke);
        }
    }
}
