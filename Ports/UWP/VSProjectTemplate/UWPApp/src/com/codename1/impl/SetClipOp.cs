using com.codename1.ui.geom;
using System.Numerics;

namespace com.codename1.impl
{
    class SetClipOp : AsyncOp
    {
        private Shape clip;
        private com.codename1.ui.Transform transform;

        public SetClipOp(Shape clip) : base(null)
        {
            this.clip = clip;
        }
        public override void execute(WindowsGraphics underlying)
        {
            underlying.setRawClip(clip);

        }

        
    }
}
