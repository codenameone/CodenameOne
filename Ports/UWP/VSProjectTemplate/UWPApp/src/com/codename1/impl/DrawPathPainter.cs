
namespace com.codename1.impl
{
    class DrawPathPainter : AsyncOp
    {
        private ui.geom.Rectangle clip;
        private int color;
        private int alpha;
        private com.codename1.ui.Stroke stroke;
        private Microsoft.Graphics.Canvas.Geometry.CanvasPathBuilder path;

        public DrawPathPainter(ui.geom.Rectangle clip, int color, int alpha, Microsoft.Graphics.Canvas.Geometry.CanvasPathBuilder path, com.codename1.ui.Stroke stroke)
            : base(clip)
        {
            this.clip = clip;
            this.color = color;
            this.alpha = alpha;
            this.path = path;
            this.stroke = new ui.Stroke(stroke.getLineWidth(), stroke.getCapStyle(), stroke.getJoinStyle(), stroke.getMiterLimit());
        }
        public override void execute(WindowsGraphics underlying)
        {
            underlying.setColor(color);
            underlying.setAlpha(alpha);
            underlying.drawPath(path, stroke);
        }
    }
}