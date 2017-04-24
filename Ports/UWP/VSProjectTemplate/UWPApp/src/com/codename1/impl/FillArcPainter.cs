using com.codename1.ui.geom;

namespace com.codename1.impl
{
    class FillArcPainter : AsyncOp
    {
        private Rectangle clip;
        private int x;
        private int y;
        private int w;
        private int h;
        private int color;
        private int alpha;
        private int startAngle;
        private int arcAngle;

        public FillArcPainter(Rectangle clip, int x, int y, int w, int h, int color, int alpha, int startAngle, int arcAngle)
            : base(clip)
        {
            this.clip = clip;
            this.x = x;
            this.y = y;
            this.w = w;
            this.h = h;
            this.color = color;
            this.alpha = alpha;
            this.startAngle = startAngle;
            this.arcAngle = arcAngle;
        }

        public override void execute(WindowsGraphics underlying)
        {
            underlying.setColor(color);
            underlying.setAlpha(alpha);
            underlying.fillArc(x, y, w, h, startAngle, arcAngle);
        }
    }
}
