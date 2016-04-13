using Microsoft.Graphics.Canvas;
using System.Collections.Generic;
using System.Linq;
using com.codename1.impl;
using Microsoft.Graphics.Canvas.UI.Xaml;
using Microsoft.Graphics.Canvas.Geometry;
using Microsoft.Graphics.Canvas.Text;
using System.Diagnostics;
using com.codename1.ui.geom;
using System.Threading.Tasks;
using Microsoft.Graphics.Canvas.UI;
using System;

namespace com.codename1.impl
{

    public class WindowsAsyncView 
    {
        private static CanvasControl _screen;
        //private SilverlightImplementation _implementation;
        private NativeGraphics _graphics;
        private static IList<AsyncOp> renderingOperations = new List<AsyncOp>();
        private static IList<AsyncOp> pendingRenderingOperations = new List<AsyncOp>();

        public WindowsAsyncView(CanvasControl screen)
        {
           _screen = screen;
           _screen.Draw += OnDraw;
           // _implementation = implementation;
        }
        private void OnDraw(CanvasControl sender, CanvasDrawEventArgs args)
        {
            if (_graphics == null)
            {
               // Debug.WriteLine("OnDraw - initializing graphics");
                _graphics = new NativeGraphics();
                _graphics.destination = new AsyncGraphics(args.DrawingSession);
                _graphics.resetClip();
            }
            else
            {
                ((AsyncGraphics)_graphics.destination).getInternal().setGraphics(args.DrawingSession);
            }
            if (renderingOperations.Count > 0)
            {
                foreach (AsyncOp o in renderingOperations)
                {
                    //Debug.WriteLine("OnDraw - execute " + o);
                    o.executeWithClip(((AsyncGraphics)_graphics.destination).getInternal());
                }
            }
            args.DrawingSession.Dispose();
            renderingOperations.Clear();
        }

        internal void flushGraphics(Rectangle rect)
        {
            //int counter = 0;
            //while (renderingOperations.Count() > 0)
            //{
            //    try
            //    {
            //        global::System.Threading.Tasks.Task.Run(() => global::System.Threading.Tasks.Task.Delay(global::System.TimeSpan.FromMilliseconds(5))).ConfigureAwait(false).GetAwaiter().GetResult();

            //        // don't let the EDT die here
            //        counter++;
            //        if (counter > 10)
            //        {
            //            Debug.WriteLine("Flush graphics timed out!!!");
            //            return;
            //        }
            //    }
            //    catch (System.Exception e)
            //    {
            //        global::System.Diagnostics.Debug.WriteLine(e);
            //    }
            //}

            using (System.Threading.AutoResetEvent are = new System.Threading.AutoResetEvent(false))
            {
                SilverlightImplementation.dispatcher.RunAsync(Windows.UI.Core.CoreDispatcherPriority.Normal, () =>
                {
                    IList<AsyncOp> tmp = renderingOperations;
                    renderingOperations = pendingRenderingOperations;
                    pendingRenderingOperations = tmp;

                    if (rect == null)
                    {
                        _screen.Invalidate();
                    }
                    else
                    {
                        _graphics.clip = rect;
                        _graphics.destination.setClip(rect);
                        _screen.Invalidate();
                    }
                    _graphics.destination.setAlpha(255);
                    _graphics.destination.setColor(0);
                    are.Set();
                }).AsTask().GetAwaiter().GetResult();
                are.WaitOne();
            }
        }

        internal void flushGraphics()
        {
            flushGraphics(null);
        }

        internal NativeGraphics getGraphics()
        {
            return _graphics;
        }
        public class AsyncGraphics : WindowsGraphics
        {
            private Rectangle clip;
            private int alpha;
            private int color;
            private CanvasTextFormat font = new CanvasTextFormat();
            private WindowsGraphics internalGraphics;

            public AsyncGraphics(CanvasDrawingSession graphics)
                : base(graphics)
            { 
                internalGraphics = new WindowsGraphics(graphics);
            }

            public WindowsGraphics getInternal()
            {
                return internalGraphics;
            }

            internal override void setClip(Rectangle clip)
            {
                this.clip = clip;
            }

            internal override void setAlpha(int alpha)
            {
                this.alpha = alpha;
            }

            internal override int getAlpha()
            {
                return alpha;
            }

            internal override void setColor(int color)
            {
                this.color = color;
            }

            internal override int getColor()
            {
                return color;
            }

            internal override void setFont(Microsoft.Graphics.Canvas.Text.CanvasTextFormat font)
            {
                this.font = font;
            }

            internal override Microsoft.Graphics.Canvas.Text.CanvasTextFormat getFont()
            {
                return font;
            }

            internal override void drawRect(int x, int y, int w, int h, int stroke)
            {
                pendingRenderingOperations.Add(new DrawRectPainter(clip, x, y, w, h, stroke, color, alpha));
            }

            internal override void fillRect(int x, int y, int w, int h)
            {
                if (alpha == 0)
                {
                    return;
                }
                pendingRenderingOperations.Add(new FillRectPainter(clip, x, y, w, h, color, alpha));
            }

            internal override void drawRoundRect(int x, int y, int w, int h, int arcW, int arcH)
            {
                pendingRenderingOperations.Add(new DrawRoundRectPainter(clip, x, y, w, h, color, alpha, arcW, arcH));
            }

            internal override void fillRoundRect(int x, int y, int w, int h, int arcW, int arcH)
            {
                pendingRenderingOperations.Add(new FillRoundRectPainter(clip, x, y, w, h, color, alpha, arcW, arcH));
            }

            internal override void fillPolygon(int[] p1, int[] p2)
            {
                pendingRenderingOperations.Add(new FillPolygonPainter(clip, p1, p2, color, alpha));
            }

            internal override void drawArc(int x, int y, int w, int h, int startAngle, int arcAngle)
            {
                pendingRenderingOperations.Add(new DrawArcPainter(clip, x, y, w, h, color, alpha, startAngle, arcAngle));
            }

            internal override void fillArc(int x, int y, int w, int h, int startAngle, int arcAngle)
            {
                pendingRenderingOperations.Add(new FillArcPainter(clip, x, y, w, h, color, alpha, startAngle, arcAngle));
            }

            internal override void drawString(string str, int x, int y)
            {
                pendingRenderingOperations.Add(new DrawStringPainter(clip, str, x, y, font, color, alpha));
            }

            internal override void drawImage(CanvasBitmap canvasBitmap, int x, int y)
            {
                pendingRenderingOperations.Add(new DrawImagePainter(clip, canvasBitmap, x, y, alpha));
            }

            internal override void drawImage(CanvasBitmap canvasBitmap, int x, int y, int w, int h)
            {
                pendingRenderingOperations.Add(new DrawImagePainter(clip, canvasBitmap, x, y, w, h, alpha));
            }

            internal override void tileImage(CanvasBitmap canvasBitmap, int x, int y, int w, int h)
            {
                pendingRenderingOperations.Add(new TileImagePainter(clip, canvasBitmap, x, y, w, h, alpha));
            }

            internal override void drawLine(int x1, int y1, int x2, int y2)
            {
                pendingRenderingOperations.Add(new DrawLinePainter(clip, x1, y1, x2, y2, color, alpha));
            }

            internal override void clear()
            {
                pendingRenderingOperations.Add(new ClearColorPainter(clip, color, alpha));
            }

            internal override void fillLinearGradient(int startColor, int endColor, int x, int y, int width, int height, bool horizontal)
            {
                pendingRenderingOperations.Add(new FillLinearGradientPainter(clip, startColor, endColor, x, y, width, height, horizontal));
            }
            internal override void fillRadialGradient(int startColor, int endColor, int x, int y, int width, int height)
            {
                pendingRenderingOperations.Add(new FillRadialGradientPainter(clip, startColor, endColor, x, y, width, height));
            }
        }
    }
}
