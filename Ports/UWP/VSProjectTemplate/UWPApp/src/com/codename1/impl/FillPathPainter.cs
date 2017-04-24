
namespace com.codename1.impl
{
    class FillPathPainter : AsyncOp
    {
        private ui.geom.Rectangle clip;
        private int color;
        private int alpha;
        private Microsoft.Graphics.Canvas.Geometry.CanvasPathBuilder path;

        public FillPathPainter(ui.geom.Rectangle clip, int color, int alpha, Microsoft.Graphics.Canvas.Geometry.CanvasPathBuilder path)
            : base(clip)
        {
            this.clip = clip;
            this.color = color;
            this.alpha = alpha;
            this.path = path;
            
        }
        public override void execute(WindowsGraphics underlying)
        {
            underlying.setAlpha(alpha);
            underlying.setColor(color);
            underlying.fillPath(path);
        }
    }
}