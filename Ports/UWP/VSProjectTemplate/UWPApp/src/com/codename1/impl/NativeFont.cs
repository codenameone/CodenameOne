using Microsoft.Graphics.Canvas.Text;
using System;
using System.Threading.Tasks;

namespace com.codename1.impl
{
    public class NativeFont : object
    {
        public int face;
        public int style;
        public int size;
        public CanvasTextFormat font = new CanvasTextFormat();

        string fileName;
        private int actualHeight = -1;
        public int height
        {
            get
            {
                if (actualHeight < 0)
                {
                    try
                    {
                        CanvasTextLayout fontLayout = new CanvasTextLayout(SilverlightImplementation.screen, "Mg", font, 0.0f, 0.0f);
                        actualHeight = Convert.ToInt32(Math.Ceiling(fontLayout.LayoutBounds.Height));
                    }
                    catch (System.ArgumentException)
                    {
                        return 1;
                    }

                }
                return actualHeight;
            }
            set
            {
                actualHeight = value;
            }
        }
        public bool bold;
        public bool italic;
        int weight;

        public NativeFont(int face, int style, int size, CanvasTextFormat font, String fileName, int height, int weight)
            : this(face, style, size, font)
        {
            this.fileName = fileName;
            this.height = height;
            this.weight = weight;
        }

        public NativeFont(int face, int style, int size, CanvasTextFormat font)
        {
            this.face = face;
            this.style = style;
            this.size = size;
            this.font = font;
            this.font.FontSize = this.font.FontSize;


        }

       public override bool Equals(object o)
        {
            NativeFont f = (NativeFont)o;
            return f.height == height && f.face == face && f.size == size && f.style == style;
        }

        public override int GetHashCode()
        {
            return face | style | size;
        }

        internal int getStringWidth(string str)
        {
            try
            {
                String aux = str.Trim();
                if (aux.Length < str.Length)
                {
                    // WIN2D does not take space size into account
                    int spaceSize = getStringWidth("_ _") - getStringWidth("__");
                    CanvasTextLayout fontLayout = new CanvasTextLayout(SilverlightImplementation.screen, aux, font, 0.0f, 0.0f);
                    return Convert.ToInt32(Math.Ceiling(fontLayout.LayoutBounds.Width)) + spaceSize * (str.Length - aux.Length);
                }
                else
                {
                    CanvasTextLayout fontLayout = new CanvasTextLayout(SilverlightImplementation.screen, str, font, 0.0f, 0.0f);
                    return Convert.ToInt32(Math.Ceiling(fontLayout.LayoutBounds.Width));
                }
            }
            catch (System.ArgumentException ex)
            {
                return str.Length * size;
            }
        }
    }

    class WaitForEdit : java.lang.Object, java.lang.Runnable
    {
        public virtual void run()
        {
            while (SilverlightImplementation.instance.currentlyEditing != null)
            {
                Task.Run(() => Task.Delay(TimeSpan.FromMilliseconds(1))).GetAwaiter().GetResult();
            }
        }
    }
}
