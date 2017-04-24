using Microsoft.Graphics.Canvas;
using System;

namespace com.codename1.impl
{
        public class CodenameOneImage 
        {
            public long lastAccess = DateTime.Now.Ticks;
            public string name;
            public bool opaque = false;
            public bool mutable = false;
            private CanvasRenderTarget actualImage;
            public CanvasRenderTarget image
            {
                set
                {
                    actualImage = value;
                    width = Convert.ToInt32(Math.Ceiling((double)actualImage.SizeInPixels.Width));
                    height = Convert.ToInt32(Math.Ceiling((double)actualImage.SizeInPixels.Height));
                    initGraphics();
            }
                get { return actualImage; }
            }
            private int width = -1;
            private int height = -1;
            public NativeGraphics graphics = new NativeGraphics();

            public void initGraphics()
            {
                if (mutable)
                {
                    graphics.destination = new WindowsMutableGraphics(image);
                }
                else
                {
                    graphics.destination = new WindowsGraphics(image.CreateDrawingSession());
                }
            }
            public void setSize(int w, int h)
            {
                width = Convert.ToInt32(Math.Ceiling((double)w));
                height = Convert.ToInt32(Math.Ceiling((double)h));
            }

            public int getImageWidth()
            {
                return width;
            }

            public int getImageHeight()
            {
                return height;
            }
        }
    }
