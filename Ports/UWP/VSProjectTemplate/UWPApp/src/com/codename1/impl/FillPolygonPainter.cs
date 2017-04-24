
namespace com.codename1.impl
{
    class FillPolygonPainter : AsyncOp
    {
        private ui.geom.Rectangle clip;
        private int[] p1;
        private int[] p2;
        private int color;
        private int alpha;

        public FillPolygonPainter(ui.geom.Rectangle clip, int[] p1, int[] p2, int color, int alpha) : base(clip)
        {
            // TODO: Complete member initialization
            this.clip = clip;
            this.p1 = p1;
            this.p2 = p2;
            this.color = color;
            this.alpha = alpha;
        }

        public override void execute(WindowsGraphics underlying)
        {
            underlying.setColor(color);
            underlying.setAlpha(alpha);
            underlying.fillPolygon(p1, p2);
        }
    }
}
