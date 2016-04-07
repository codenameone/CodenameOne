using System;
using System.Collections.Generic;
using System.Linq;
using System.Text;

namespace com.codename1.impl 
{
    class FillRadialGradientPainter : AsyncOp
    {
        private global::com.codename1.ui.geom.Rectangle clip;
        private int startColor;
        private int endColor;
        private int x;
        private int y;
        private int width;
        private int height;

        public FillRadialGradientPainter(global::com.codename1.ui.geom.Rectangle clip, int startColor, int endColor, int x, int y, int width, int height):base(clip)
        {
            this.clip = clip;
            this.startColor = startColor;
            this.endColor = endColor;
            this.x = x;
            this.y = y;
            this.width = width;
            this.height = height;
        }
        public override void execute(WindowsGraphics underlying)
        {
            underlying.fillRadialGradient(startColor, endColor, x, y, width, height);
        }
    }
}
