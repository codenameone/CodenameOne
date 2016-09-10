
namespace com.codename1.impl
{
    class FillLinearGradientPainter : AsyncOp
    {
        private ui.geom.Rectangle clip;
        private int startColor;
        private int endColor;
        private int x;
        private int y;
        private int width;
        private int height;
        private bool horizontal;

        public FillLinearGradientPainter(ui.geom.Rectangle clip, int startColor, int endColor, int x, int y, int width, int height, bool horizontal)
            : base(clip)
        {
            this.clip = clip;
            this.startColor = startColor;
            this.endColor = endColor;
            this.x = x;
            this.y = y;
            this.width = width;
            this.height = height;
            this.horizontal = horizontal;
        }
        public override void execute(WindowsGraphics underlying)
        {
            underlying.fillLinearGradient(startColor, endColor, x, y, width, height, horizontal);
        }
    }
}