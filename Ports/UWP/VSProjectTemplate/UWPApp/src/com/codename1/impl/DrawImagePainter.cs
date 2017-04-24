using com.codename1.ui.geom;

namespace com.codename1.impl
{
    class DrawImagePainter : AsyncOp
    {
        private Rectangle clip;
        private Microsoft.Graphics.Canvas.CanvasBitmap canvasBitmap;
        private int x;
        private int y;
        private int alpha;
        private int w = -1;
        private int h = -1;

        public DrawImagePainter(Rectangle clip, Microsoft.Graphics.Canvas.CanvasBitmap canvasBitmap, int x, int y, int alpha)
            : base(clip)
        {
            this.clip = clip;
            this.canvasBitmap = canvasBitmap;
            this.x = x;
            this.y = y;
            this.alpha = alpha;
        }

        public DrawImagePainter(Rectangle clip, Microsoft.Graphics.Canvas.CanvasBitmap canvasBitmap, int x, int y, int w, int h, int alpha)
            : base(clip)
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
            underlying.setAlpha(alpha);
            //underlying.setColor(0xff0000);
            if (w <= 0 || h <= 0)
            {
                //underlying.drawRect(x, y, (int)canvasBitmap.SizeInPixels.Width, (int)canvasBitmap.SizeInPixels.Height);
                underlying.drawImage(canvasBitmap, x, y);
            }
            else
            {
                //underlying.drawRect(x, y, w, h);
                underlying.drawImage(canvasBitmap, x, y, w, h);
            }
        }
    }
}
