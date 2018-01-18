using com.codename1.ui.geom;
using Microsoft.Graphics.Canvas;
using Microsoft.Graphics.Canvas.Brushes;
using Microsoft.Graphics.Canvas.Effects;
using Microsoft.Graphics.Canvas.Geometry;
#if WINDOWS_UWP
using System.Numerics;
#else
using Microsoft.Graphics.Canvas.Numerics;
#endif
using Microsoft.Graphics.Canvas.Text;
using System;
using System.Collections.Generic;
using Windows.Foundation;
using Windows.UI;
using Windows.UI.Xaml.Shapes;
using com.codename1.ui;

namespace com.codename1.impl
{
    public class WindowsGraphics
    {
        private CanvasDrawingSession graphics;
        private Color c = Colors.Black;
        private CanvasTextFormat font = new CanvasTextFormat();
        private CanvasActiveLayer layer;
        private bool disposed = false;
        private int alpha = 0xff;
        private com.codename1.ui.Transform transform = com.codename1.ui.Transform.makeTranslation(0,0,0);

        public WindowsGraphics(CanvasDrawingSession graphics)
        {
            this.graphics = graphics;
            this.graphics.Units = CanvasUnits.Pixels;
        }


        internal void setGraphics(CanvasDrawingSession graphics)
        {
            this.graphics = graphics;
            this.graphics.Units = CanvasUnits.Pixels;
        }

        public virtual com.codename1.ui.Transform getTransform()
        {
            return transform.copy();
            //return graphics.Transform;
        }

        public virtual void setTransform(com.codename1.ui.Transform transform)
        {
            this.transform.setTransform(transform);
            if (!transform.isIdentity())
            {
                graphics.Transform = SilverlightImplementation.clamp(((SilverlightImplementation.NativeTransform)transform.getNativeTransform()).m);
            } else
            {
                graphics.Transform = Matrix3x2.Identity;
            }
           
        }

        internal void removeClip()
        {
            if (layer != null)
            {
                layer.Dispose();
                layer = null;
            }
        }

        internal void dispose()
        {
            graphics.Dispose();
            disposed = true;
        }

        internal bool isDisposed()
        {
            return disposed;
        }

        internal virtual void setClip(ui.geom.Rectangle clip)
        {
            if (clip == null)
            {
                return;
            }
            if (clip.getWidth() <= 0)
            {
               // System.Diagnostics.Debug.WriteLine("aaaaaaaaaaaaaaaaaaaa width");
                clip.setWidth(1);
            }
            if (clip.getHeight() <= 0)
            {
               // System.Diagnostics.Debug.WriteLine("aaaaaaaaaaaaaaaaaaaa height");
                clip.setHeight(1);
            }
            
            layer = graphics.CreateLayer(1f, new Rect(
                clip.getX(),
                clip.getY(),
                clip.getWidth(),
                clip.getHeight()
            ));
            
        }

        private CanvasActiveLayer createAlphaLayer()
        {
            return graphics.CreateLayer(alpha/255f);
        }

        internal virtual void setAlpha(int p)
        {
            //c.A = (byte)(p & 0xff);
            alpha = (p & 0xff); 
        }

        
        internal virtual void setColor(int p)
        {
            byte alpha = (byte)((p >> 24) & 0xff);
            if (alpha == 0)
            {
                alpha = 0xff;
            }
            setColor(p, alpha);
        }

        internal void setColor(int p, byte alpha)
        {
            c.R = (byte)((p >> 16) & 0xff);
            c.G = (byte)((p >> 8) & 0xff);
            c.B = (byte)(p & 0xff);
            c.A = alpha;
        }

        internal virtual void setFont(CanvasTextFormat font)
        {
            this.font = font;
            //font.FontFamily = "Arial";
        }

        internal virtual CanvasTextFormat getFont()
        {
            return font;
        }

        internal virtual int getColor()
        {
            return (c.R << 16) + (c.G << 8) + c.B;
        }

        internal virtual void drawLine(int x1, int y1, int x2, int y2)
        {
            //using (createAlphaLayer())
            //{
                graphics.DrawLine(x1, y1, x2, y2, Color.FromArgb((byte)alpha, c.R, c.G, c.B));
            //}
                
        }

        internal virtual void fillRect(int x, int y, int w, int h)
        {
            //using (createAlphaLayer())
            //{
                graphics.FillRectangle(x, y, w, h, Color.FromArgb((byte)alpha, c.R, c.G, c.B));
            //}
                
        }

        internal virtual void clearRect(int x, int y, int w, int h)
        {
            CanvasBlend oldBlend = graphics.Blend;
            graphics.Blend = CanvasBlend.Copy;
            CanvasSolidColorBrush brush = new CanvasSolidColorBrush(graphics, Colors.Transparent);
            
            brush.Color = Colors.Transparent;
            brush.Opacity = 1;
            graphics.FillRectangle(x, y, w, h, brush);
            graphics.Blend = oldBlend;
        }

        internal virtual void drawRect(int x, int y, int w, int h, int stroke)
        {
            //using (createAlphaLayer())
            //{
                graphics.DrawRectangle(x, y, w, h, Color.FromArgb((byte)alpha, c.R, c.G, c.B), stroke);
            //}
                
        }

        internal virtual void drawRoundRect(int x, int y, int w, int h, int arcW, int arcH)
        {
            //using (createAlphaLayer())
            //{
                graphics.DrawRoundedRectangle(x, y, w, h, arcW, arcH, Color.FromArgb((byte)alpha, c.R, c.G, c.B));
            //}
               
        }

        internal virtual void fillRoundRect(int x, int y, int w, int h, int arcW, int arcH)
        {
            //using (createAlphaLayer())
            //{
                graphics.FillRoundedRectangle(x, y, w, h, arcW, arcH, Color.FromArgb((byte)alpha, c.R, c.G, c.B));
            //}
                
        }

        internal virtual void fillPolygon(int[] p1, int[] p2)
        {
            //using (createAlphaLayer())
            //{
                if (p1.Length < 3 || p2.Length < 3 || p1.Length != p2.Length)
                {
                    return;
                }
                List<Vector2> pointsList = new List<Vector2>();
                pointsList.ToArray();
                for (int pos = 0; pos < p1.Length; pos++)
                {
                    Vector2 p = new Vector2();
                    p.X = p1[pos];
                    p.Y = p2[pos];
                    pointsList.Add(p);
                }
                graphics.FillGeometry(CanvasGeometry.CreatePolygon(graphics, pointsList.ToArray()), Color.FromArgb((byte)alpha, c.R, c.G, c.B));
            //}
        }
               

        internal virtual void fillArc(int x, int y, int w, int h, int startAngle, int arcAngle)
        {
            //using (createAlphaLayer())
            //{
                Vector2 center = new Vector2();
                center.X = x + w / 2;
                center.Y = y + h / 2;
                if (arcAngle == 360)
                    graphics.FillEllipse(center, w / 2, h / 2, Color.FromArgb((byte)alpha, c.R, c.G, c.B));
                else
                {
                    CanvasPathBuilder builder = new CanvasPathBuilder(graphics);
                    builder.BeginFigure(center);
                    builder.AddArc(center, w / 2, h / 2, -(float)(2 * Math.PI * startAngle / 360), -(float)(2 * Math.PI * arcAngle / 360));
                    builder.EndFigure(CanvasFigureLoop.Closed);
                    graphics.FillGeometry(CanvasGeometry.CreatePath(builder), Color.FromArgb((byte)alpha, c.R, c.G, c.B));
                }
            //}
               
        }

        internal virtual void drawArc(int x, int y, int w, int h, int startAngle, int arcAngle)
        {
            //using (createAlphaLayer())
            //{
                Vector2 center = new Vector2();
                center.X = x + w / 2;
                center.Y = y + h / 2;
                if (arcAngle == 360)
                    graphics.DrawEllipse(center, w / 2, h / 2, Color.FromArgb((byte)alpha, c.R, c.G, c.B));
                else
                {
                    CanvasPathBuilder builder = new CanvasPathBuilder(graphics);
                    builder.BeginFigure(center);
                    builder.AddArc(center, w / 2, h / 2, -(float)(2 * Math.PI * startAngle / 360), -(float)(2 * Math.PI * arcAngle / 360));
                    builder.EndFigure(CanvasFigureLoop.Closed);
                    graphics.DrawGeometry(CanvasGeometry.CreatePath(builder), Color.FromArgb((byte)alpha, c.R, c.G, c.B));
                }
            //}
               
        }

        internal virtual void drawString(string str, int x, int y)
        {
            //using (createAlphaLayer())
            //{
                CanvasTextLayout l = new CanvasTextLayout(graphics, str, font, 0.0f, 0.0f);
                graphics.DrawTextLayout(l, x, y, Color.FromArgb((byte)alpha, c.R, c.G, c.B));
            //}
                
        }

        private void drawImageImpl(CanvasBitmap canvasBitmap, int x, int y)
        {
            if (isMutable())
            {

                graphics.DrawImage(image2Premultiply(canvasBitmap), x, y);

            }
            else
            {
                graphics.DrawImage(canvasBitmap, x, y);
            }
        }

        internal virtual void drawImage(CanvasBitmap canvasBitmap, int x, int y)
        {
            if (alpha == 0)
            {
                return;
            }
            if (alpha < 255)
            {
                using (createAlphaLayer())
                {
                    drawImageImpl(canvasBitmap, x, y);
                }
            }
            else
            {
                drawImageImpl(canvasBitmap, x, y);
            }
           
               
 
        }

        private ICanvasImage image2Premultiply(ICanvasImage aImage)
        {
            return new PremultiplyEffect()
            {
                Source = aImage
            };
        }

        private void drawImageImpl(CanvasBitmap canvasBitmap, int x, int y, int w, int h)
        {
            Rect destRect = new Rect();
            destRect.X = x;
            destRect.Y = y;
            destRect.Width = w;
            destRect.Height = h;


            graphics.DrawImage(canvasBitmap, destRect);
        }

        internal virtual void drawImage(CanvasBitmap canvasBitmap, int x, int y, int w, int h)
        {
            if (alpha == 0)
            {
                return;
            }
            if (alpha < 255)
            {
                using (createAlphaLayer())
                {
                    drawImageImpl(canvasBitmap, x, y, w, h);
                }
            } else
            {
                drawImageImpl(canvasBitmap, x, y, w, h);
            }
 
        }

        private void tileImageImpl(CanvasBitmap canvasBitmap, int x, int y, int w, int h)
        {
            CanvasImageBrush brush = new CanvasImageBrush(graphics.Device, canvasBitmap);
            brush.ExtendX = CanvasEdgeBehavior.Wrap;
            brush.ExtendY = CanvasEdgeBehavior.Wrap;
            System.Numerics.Matrix3x2 currentTransform = graphics.Transform;
            graphics.Transform = System.Numerics.Matrix3x2.CreateTranslation(x, y);
            graphics.FillRectangle(0, 0, w, h, brush);
            graphics.Transform = currentTransform;
        }

        internal virtual void tileImage(CanvasBitmap canvasBitmap, int x, int y, int w, int h)
        {
            if (alpha == 0)
            {
                return;
            }
            if (alpha < 255)
            {
                using (createAlphaLayer())
                {
                    tileImageImpl(canvasBitmap, x, y, w, h);
                }
            } else
            {
                tileImageImpl(canvasBitmap, x, y, w, h);
            }
            
                
        }

        internal virtual void clear()
        {
            graphics.Clear(c);
        }

        internal virtual int getAlpha()
        {
            return alpha;
        }

        internal virtual void fillLinearGradient( int startColor, int endColor, int x, int y, int width, int height, bool horizontal)
        {
            //using (createAlphaLayer())
            //{
                var starcolor = new Color() { A = (byte)alpha, B = (byte)(startColor & 0xff), G = (byte)((startColor >> 8) & 0xff), R = (byte)((startColor >> 16) & 0xff) };
                var endcolor = new Color() { A = (byte)alpha, B = (byte)(endColor & 0xff), G = (byte)((endColor >> 8) & 0xff), R = (byte)((endColor >> 16) & 0xff) };

                CanvasLinearGradientBrush brush = new CanvasLinearGradientBrush(graphics, starcolor, endcolor);
                brush.StartPoint = new Vector2()
                {
                    X = x,
                    Y = y,
                };
                if (horizontal)
                {
                    brush.EndPoint = new Vector2()
                    {
                        X = x + width,
                        Y = y,
                    };
                }
                else
                {
                    brush.EndPoint = new Vector2()
                    {
                        X = x,
                        Y = y + height,
                    };
                }
                graphics.FillRectangle(x, y, width, height, brush);
            //}
                
        }

        internal virtual void fillRadialGradient(int startColor, int endColor, int x, int y, int width, int height)
        {
            //using (createAlphaLayer())
            //{
                var startcolor = new Color() { A =(byte)alpha, B = (byte)(startColor & 0xff), G = (byte)((startColor >> 8) & 0xff), R = (byte)((startColor >> 16) & 0xff) };
                var endcolor = new Color() { A = (byte)alpha, B = (byte)(endColor & 0xff), G = (byte)((endColor >> 8) & 0xff), R = (byte)((endColor >> 16) & 0xff) };

                CanvasRadialGradientBrush brush = new CanvasRadialGradientBrush(graphics, startcolor, endcolor);
                brush.Center = new Vector2()
                {
                    X = x + width / 2,
                    Y = y + height / 2,
                };
                brush.RadiusX = width / 2;
                brush.RadiusY = height / 2;

                graphics.FillRectangle(x, y, width, height, brush);
            //}
               
            
        }

        internal virtual bool isMutable()
        {
            return false;
        }

        internal virtual void fillPath(CanvasPathBuilder p)
        {
            //using (createAlphaLayer())
            //{
            //    graphics.DrawGeometry(CanvasGeometry.CreatePath(p), c);
            //}
            using (p)
            {
                graphics.FillGeometry(CanvasGeometry.CreatePath(p), Color.FromArgb((byte)alpha, c.R, c.G, c.B));
            }
                
        }

        internal virtual void drawPath(CanvasPathBuilder p, Stroke stroke)
        {
            using (p)
            {
                graphics.DrawGeometry(CanvasGeometry.CreatePath(p), Color.FromArgb((byte)alpha, c.R, c.G, c.B), stroke.getLineWidth());
            }
               
        }
    }
}
