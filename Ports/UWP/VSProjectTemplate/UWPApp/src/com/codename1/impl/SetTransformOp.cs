using com.codename1.ui.geom;
using System.Numerics;

namespace com.codename1.impl
{
    class SetTransformOp : AsyncOp
    {
        private Rectangle clip;
        private com.codename1.ui.Transform transform;

        public SetTransformOp(Rectangle clip, com.codename1.ui.Transform transform) : base(clip)
        {
            this.clip = clip;
            this.transform = transform.copy();
        }
        public override void execute(WindowsGraphics underlying)
        {
            underlying.setTransform(transform);

        }
    }
}
