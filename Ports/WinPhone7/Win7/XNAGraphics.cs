using Microsoft.Xna.Framework.Graphics;
using Microsoft.Xna.Framework;
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
using System.Collections.Generic;
using System.Threading;

namespace com.codename1.impl
{
    public class XNAGraphics : java.lang.Object
    {
        public Nullable<Microsoft.Xna.Framework.Rectangle> clip = null;
        private static XNAGraphics currentGraphics;
        private List<OperationPending> paints = new List<OperationPending>();
        public void performDrawOperation(OperationPending op, List<OperationPending> pendingPaints)
        {
            if (!mutable)
            {
                pendingPaints.Add(op);
            }
            else
            {
                paints.Add(op);
            }

        }

        public void release()
        {
            if (mutable && paints.Count > 0)
            {
                grab();
                for (int iter = 0; iter < paints.Count; iter++)
                {
                    paints[iter].prerender();
                }
                GraphicsDevice gd = target.GraphicsDevice;
                SpriteBatch spriteBatch = new SpriteBatch(gd);
                while (paints.Count > 0)
                {
                    OperationPending pen = paints[0];
                    paints.RemoveAt(0);

                    if (pen.isDrawOperation())
                    {
                        spriteBatch.Begin(SpriteSortMode.Immediate, BlendState.NonPremultiplied, null, null, SilverlightImplementation.instance._rasterizerState);
                        gd = spriteBatch.GraphicsDevice;
                        gd.RasterizerState = SilverlightImplementation.instance._rasterizerState;
                        if (clip != null)
                        {
                            try
                            {
                                gd.ScissorRectangle = clip.Value;
                            }
                            catch (System.Exception err)
                            {
                                // C# throws exception for invalid clipping means that its our of bounds.
                                gd.ScissorRectangle = Microsoft.Xna.Framework.Rectangle.Empty;
                            }
                        }

                        //pen.performBench(instance.graphics, instance.spriteBatch);
                        pen.perform(this, spriteBatch);
                        spriteBatch.End();
                    }
                    else
                    {
                        //pen.performBench(instance.graphics, instance.spriteBatch);
                        pen.perform(this, spriteBatch);
                    }
                    //pen.printLogging();
                    //pen.printBench();
                }

                target.GraphicsDevice.SetRenderTarget(null);
            }
        }

        public void grab()
        {
            if (currentGraphics != this)
            {
                currentGraphics = this;
                if (target != null)
                {
                    target.GraphicsDevice.SetRenderTarget(target);
                }
            }
        }

        public BasicEffect basicEffect {
            get
            {
                if (be == null)
                {
                    be = new BasicEffect(target.GraphicsDevice);
                    be.VertexColorEnabled = true;
                    
                    be.Projection = Microsoft.Xna.Framework.Matrix.CreateOrthographicOffCenter
                       (0, target.GraphicsDevice.Viewport.Width,     // left, right
                        target.GraphicsDevice.Viewport.Height, 0,    // bottom, top
                        0, 1);
                }
                return be;
            }
        }
        public bool mutable;
        public int clipX;
        public int clipY;
        public int clipW;
        public int clipH;
        private BasicEffect be;
        public RenderTarget2D target { get; set; }
        public XNAFont currentFont;
        public int Color { get; set; }
        public Microsoft.Xna.Framework.Color cColor
        {
            get
            {
                Microsoft.Xna.Framework.Color cc = new Microsoft.Xna.Framework.Color();
                cc.A = (byte)Alpha;
                cc.B = (byte)(Color & 0xff);
                cc.R = (byte)((Color >> 16) & 0xff);
                cc.G = (byte)((Color >> 8) & 0xff);
                return cc;
            }
        }
        public System.Windows.Media.Color sColor
        {
            get
            {
                System.Windows.Media.Color cc = new System.Windows.Media.Color();
                cc.A = (byte)Alpha;
                cc.B = (byte)(Color & 0xff);
                cc.R = (byte)((Color >> 16) & 0xff);
                cc.G = (byte)((Color >> 8) & 0xff);
                return cc;
            }
        }
        public int Alpha { get; set; }

        public XNAGraphics()
        {
            Alpha = 255;
        }
    }
}
