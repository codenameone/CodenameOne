using System;
using System.Net;
using System.Windows;
using System.Windows.Controls;
using System.Windows.Documents;
using System.Windows.Ink;
using System.Windows.Input;
using System.Windows.Media;
using System.Windows.Media.Animation;
using System.Windows.Shapes;
using Microsoft.Xna.Framework.Graphics;

namespace com.codename1.impl
{
    public class XNAImage : java.lang.Object
    {
        public XNAGraphics graphics { 
            get {
                if(g == null && image is RenderTarget2D) {
                    g = new XNAGraphics();
                    g.target = (RenderTarget2D)image;
                    g.clipW = image.Width;
                    g.clipH = image.Height;
                    g.mutable = true;
                }
                return g;
            } 
        }

        public void release()
        {
            if (g != null)
            {
                g.release();
            }
        }

        private XNAGraphics g;
        public int Width
        {
            get
            {
                if(aWidth < 0) {
                    return image.Width;
                }
                return aWidth;
            }
            set
            {
                aWidth = value;
            }
        }

        public int Height
        {
            get
            {
                if (aWidth < 0)
                {
                    return image.Height;
                }
                return aHeight;
            }
            set
            {
                aHeight = value;
            }
        }
        private int aWidth = -1, aHeight;

        public Texture2D image;
        public XNAImage() { }
        public XNAImage(Texture2D t) {
            image = t;
        }
    }
}
