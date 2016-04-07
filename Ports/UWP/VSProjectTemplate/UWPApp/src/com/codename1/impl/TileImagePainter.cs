using com.codename1.ui.geom;
using System;
using System.Collections.Generic;
using System.Linq;
using System.Text;

namespace com.codename1.impl
{
    class TileImagePainter : AsyncOp
    {
        private Rectangle clip;
        private Microsoft.Graphics.Canvas.CanvasBitmap canvasBitmap;
        private int x;
        private int y;
        private int w;
        private int h;
        private int alpha;

        public TileImagePainter(Rectangle clip, Microsoft.Graphics.Canvas.CanvasBitmap canvasBitmap, int x, int y, int w, int h, int alpha) : base(clip)
        {
            this.clip = clip;
            this.canvasBitmap = canvasBitmap;
            this.x = x;
            this.y = y;
            this.w = w;
            this.h = h;
            this.alpha = alpha;
        }

        public override void execute(WindowsGraphics underlying)
        {
            underlying.setAlpha(0xff);
            underlying.tileImage(canvasBitmap, x, y, w, h);
        }
    }
}
