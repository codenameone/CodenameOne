using System;
using System.Collections.Generic;
using System.Linq;
using System.Text;

namespace com.codename1.impl
{
    class WindowsMutableGraphics : WindowsGraphics
    {
        private Microsoft.Graphics.Canvas.CanvasRenderTarget canvas;
        private ui.geom.Rectangle clip;

        public WindowsMutableGraphics(Microsoft.Graphics.Canvas.CanvasRenderTarget canvas)
            : base(canvas.CreateDrawingSession())
        {
            
            this.canvas = canvas;
            dispose();
        }

        internal override void setClip(ui.geom.Rectangle clip)
        {
            this.clip = clip;
        }

        internal override void fillRect(int x, int y, int w, int h)
        {
            setGraphics(canvas.CreateDrawingSession());
            base.setClip(clip);
            base.fillRect(x, y, w, h);
            base.removeClip();
            dispose();
        }

        internal override void fillArc(int x, int y, int w, int h, int startAngle, int arcAngle)
        {
            setGraphics(canvas.CreateDrawingSession());
            base.setClip(clip);
            base.fillArc(x, y, w, h, startAngle, arcAngle);
            base.removeClip();
            dispose();
        }

        internal override void drawImage(Microsoft.Graphics.Canvas.CanvasBitmap canvasBitmap, int x, int y)
        {
            setGraphics(canvas.CreateDrawingSession());
            base.setClip(clip);
            base.drawImage(canvasBitmap, x, y);
            base.removeClip();
            dispose();
        }

        internal override void drawImage(Microsoft.Graphics.Canvas.CanvasBitmap canvasBitmap, int x, int y, int w, int h)
        {
            setGraphics(canvas.CreateDrawingSession());
            base.setClip(clip);
            base.drawImage(canvasBitmap, x, y, w, h);
            base.removeClip();
            dispose();
        }

        internal override void drawLine(int x1, int y1, int x2, int y2)
        {
            setGraphics(canvas.CreateDrawingSession());
            base.setClip(clip);
            base.drawLine(x1, y1, x2, y2);
            base.removeClip();
            dispose();
        }

        internal override void drawString(string str, int x, int y)
        {
            setGraphics(canvas.CreateDrawingSession());
            base.setClip(clip);
            base.drawString(str, x, y);
            base.removeClip();
            dispose();
        }

        internal override void tileImage(Microsoft.Graphics.Canvas.CanvasBitmap canvasBitmap, int x, int y, int w, int h)
        {
            setGraphics(canvas.CreateDrawingSession());
            base.setClip(clip);
            base.tileImage(canvasBitmap, x, y, w, h);
            base.removeClip();
            dispose();
        }

        internal override void clear()
        {
            setGraphics(canvas.CreateDrawingSession());
            base.setClip(clip);
            base.clear();
            base.removeClip();
            dispose();
        }
        internal override void fillPolygon(int[] p1, int[] p2)
        {
            setGraphics(canvas.CreateDrawingSession());
            base.setClip(clip);
            base.fillPolygon(p1, p2);
            base.removeClip();
            dispose();
        }
        internal override void drawArc(int x, int y, int w, int h, int startAngle, int arcAngle)
        {
            setGraphics(canvas.CreateDrawingSession());
            base.setClip(clip);
            base.drawArc(x, y, w, h, startAngle, arcAngle);
            base.removeClip();
            dispose();
        }
        internal override void drawRect(int x, int y, int w, int h, int stroke)
        {
            setGraphics(canvas.CreateDrawingSession());
            base.setClip(clip);
            base.drawRect(x, y, w, h, stroke);
            base.removeClip();
            dispose();
        }
        internal override void drawRoundRect(int x, int y, int w, int h, int arcW, int arcH)
        {
            setGraphics(canvas.CreateDrawingSession());
            base.setClip(clip);
            base.drawRoundRect(x, y, w, h, arcW, arcH);
            base.removeClip();
            dispose();
        }
        internal override void fillRoundRect(int x, int y, int w, int h, int arcW, int arcH)
        {
            setGraphics(canvas.CreateDrawingSession());
            base.setClip(clip);
            base.fillRoundRect(x, y, w, h, arcW, arcH);
            base.removeClip();
            dispose();
        }

        internal override void fillLinearGradient(int startColor, int endColor, int x, int y, int width, int height, bool horizontal)
        {
            setGraphics(canvas.CreateDrawingSession());
            base.setClip(clip);
            base.fillLinearGradient(startColor, endColor, x, y, width, height, horizontal);
            base.removeClip();
            dispose();
        }
        internal override void fillRadialGradient(int startColor, int endColor, int x, int y, int width, int height)
        {
            setGraphics(canvas.CreateDrawingSession());
            base.setClip(clip);
            base.fillRadialGradient(startColor, endColor, x, y, width, height);
            base.removeClip();
            dispose();
        }
        internal override bool isMutable()
        {
            return true;
        }

        //internal override bool isMutable()
        //{
        //    return true;
        //}

        #region nao implementar
        //internal override int getAlpha()
        //{
        //    setGraphics(canvas.CreateDrawingSession());
        //    return base.getAlpha();  
        //}

        //internal override int getColor()
        //{
        //    setGraphics(canvas.CreateDrawingSession());
        //    return base.getColor();
        //}

        //internal override Microsoft.Graphics.Canvas.Text.CanvasTextFormat getFont()
        //{
        //    setGraphics(canvas.CreateDrawingSession());
        //    return base.getFont();
           
        //}

        //internal override void setAlpha(int p)
        //{
        //    setGraphics(canvas.CreateDrawingSession());
        //    base.setAlpha(p);
        //    dispose();
        //}

        //internal override void setColor(int p)
        //{
        //    setGraphics(canvas.CreateDrawingSession());
        //    base.setColor(p);
        //    dispose();
        //}

        //internal override void setFont(Microsoft.Graphics.Canvas.Text.CanvasTextFormat font)
        //{
        //    setGraphics(canvas.CreateDrawingSession());
        //    base.setFont(font);
        //    dispose();
        //}
        #endregion
    }
}
