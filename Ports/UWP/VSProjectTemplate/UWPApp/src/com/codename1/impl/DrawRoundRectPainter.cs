using com.codename1.ui.geom;

namespace com.codename1.impl
{
    class DrawRoundRectPainter : AsyncOp
    {
        private Rectangle clip;
        private int x;
        private int y;
        private int w;
        private int h;
        private int color;
        private int alpha;
        private int arcW;
        private int arcH;

        public DrawRoundRectPainter(Rectangle clip, int x, int y, int w, int h, int color, int alpha, int arcW, int arcH)
            : base(clip)
        {
            this.clip = clip;
            this.x = x;
            this.y = y;
            this.w = w;
            this.h = h;
            this.color = color;
            this.alpha = alpha;
            this.arcW = arcW;
            this.arcH = arcH;
        }
        public override void execute(WindowsGraphics underlying)
        {
            underlying.setColor(color);
            underlying.setAlpha(alpha);
            underlying.drawRoundRect(x, y, w, h, arcW, arcH);
        }
    }
}
