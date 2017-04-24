using com.codename1.ui.geom;

namespace com.codename1.impl
{
    class ClearRectPainter : AsyncOp
    {
        private Rectangle clip;
        private int x;
        private int y;
        private int w;
        private int h;


        public ClearRectPainter(Rectangle clip, int x, int y, int w, int h)
            : base(clip)
        {
            this.clip = clip;
            this.x = x;
            this.y = y;
            this.w = w;
            this.h = h;

        }

        public override void execute(WindowsGraphics underlying)
        {
            underlying.clearRect(x, y, w, h);
        }

     
    }
}
