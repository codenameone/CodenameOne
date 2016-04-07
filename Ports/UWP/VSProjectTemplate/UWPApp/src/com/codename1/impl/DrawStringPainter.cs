using com.codename1.ui.geom;
using System;
using System.Collections.Generic;
using System.Linq;
using System.Text;

namespace com.codename1.impl
{
    class DrawStringPainter : AsyncOp
    {
        private Rectangle clip;
        private string str;
        private int x;
        private int y;
        private Microsoft.Graphics.Canvas.Text.CanvasTextFormat font;
        private int color;
        private int alpha;

        public DrawStringPainter(Rectangle clip, string str, int x, int y, Microsoft.Graphics.Canvas.Text.CanvasTextFormat font, int color, int alpha)
            : base(clip)
        {
            this.clip = clip;
            this.str = str;
            this.x = x;
            this.y = y;
            this.font = font;
            this.color = color;
            this.alpha = alpha;
        }

        public override void execute(WindowsGraphics underlying)
        {
            underlying.setColor(color);
            underlying.setAlpha(alpha);
            underlying.setFont(font);
            underlying.drawString(str, x, y);
        }
    }
}
