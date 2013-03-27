/*
 * Copyright (c) 2012, Codename One and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.  Codename One designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Oracle in the LICENSE file that accompanied this code.
 *  
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 2 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 * 
 * You should have received a copy of the GNU General Public License version
 * 2 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 * 
 * Please contact Codename One through http://www.codenameone.com/ if you 
 * need additional information or have any questions.
 */

using org.xmlvm;
using Microsoft.Xna.Framework;
using Microsoft.Xna.Framework.Content;
using Microsoft.Xna.Framework.Graphics;
using System.Windows;
using System.Windows.Resources;
using System.IO;
using System;
using Microsoft.Phone.Controls;
using System.Windows.Controls;
using System.Threading;
using Microsoft.Xna.Framework.Input;
using System.Windows.Input;
using Microsoft.Devices;
using System.Net;
using System.IO.IsolatedStorage;

namespace com.codename1.impl
{
    public class SilverlightImplementation : global::com.codename1.impl.CodenameOneImplementation, IServiceProvider
    {
        public const int BACK_KEY = -1;
        private static PhoneApplicationPage app;
        public static SilverlightImplementation instance;
        private static ContentManager content;
        private XNAGraphics graphics;
        private XNAFont defaultFont = new XNAFont();
        public RenderTarget2D currentlyShowing;
        private SpriteBatch spriteBatch;
        private static System.Collections.Generic.List<OperationPending> pendingPaints = new System.Collections.Generic.List<OperationPending>();
        private static System.Collections.Generic.List<OperationPending> currentPaints = new System.Collections.Generic.List<OperationPending>();
        private static Object PAINT_LOCK = new Object();
        public static TextBlock textBlockInstance;
        public RasterizerState _rasterizerState = new RasterizerState();
        public static TextBox textInputInstance;
        public com.codename1.ui.TextArea currentlyEditing;
        private UIElementRenderer currentEditingUIR;

        public Object GetService(Type serviceType)
        {
            return null;
        }

        public static void initApp(PhoneApplicationPage app_, ContentManager c)
        {
            app = app_;
            content = c;
        }
        public void @this()
        {
            base.@this();
            instance = this;
        }

        public void systemKeyDown(object sender, KeyEventArgs e) 
        {
            if (currentlyEditing != null)
            {
                return;
            }
            if (e.Key == Key.Escape)
            {
                e.Handled = true;
                keyPressed(BACK_KEY);
                return;
            }
        }

        public void systemKeyUp(object sender, KeyEventArgs e)
        {
            if (currentlyEditing != null)
            {
                return;
            }
            if (e.Key == Key.Escape)
            {
                e.Handled = true;
                keyReleased(BACK_KEY);
                return;
            }
        }

        public override void init(global::java.lang.Object n1)
        {
            _rasterizerState.ScissorTestEnable = true;
            graphics = new XNAGraphics();
            currentlyShowing = new RenderTarget2D(SharedGraphicsDeviceManager.Current.GraphicsDevice, getDisplayWidth(), getDisplayHeight(), false,
                SurfaceFormat.Color, DepthFormat.None,
                0, RenderTargetUsage.PreserveContents);
            SharedGraphicsDeviceManager.Current.GraphicsDevice.RasterizerState = _rasterizerState;
            spriteBatch = new SpriteBatch(SharedGraphicsDeviceManager.Current.GraphicsDevice);
            graphics.target = currentlyShowing;
            graphics.clipW = getDisplayWidth();
            graphics.clipH = getDisplayHeight();

            app.KeyDown += new KeyEventHandler(systemKeyDown);
            app.KeyUp += new KeyEventHandler(systemKeyUp);

            Uri uriResource = new Uri("SplashScreenImage.jpg", UriKind.Relative);
            StreamResourceInfo si = Application.GetResourceStream(uriResource);
            Stream strm = si.Stream;

            Texture2D background = Texture2D.FromStream(SharedGraphicsDeviceManager.Current.GraphicsDevice, strm);
            SharedGraphicsDeviceManager.Current.GraphicsDevice.SetRenderTarget(currentlyShowing);
            spriteBatch.Begin();
            spriteBatch.Draw(background, new Rectangle(0, 0, getDisplayWidth(), getDisplayHeight()), Color.White);
            spriteBatch.End();
            SharedGraphicsDeviceManager.Current.GraphicsDevice.SetRenderTarget(null);

            textInputInstance.TextChanged += textChangedEvent;
        }

        public override int getDisplayWidth()
        {
            return (int)SharedGraphicsDeviceManager.Current.GraphicsDevice.Viewport.Width;
        }

        public override int getDisplayHeight()
        {
            return (int)SharedGraphicsDeviceManager.Current.GraphicsDevice.Viewport.Height;
        }

        public override bool isNativeInputSupported()
        {
            return true;
        }

        public override bool isNativeInputImmediate()
        {
            return true;
        }

        public static void commitEditing()
        {
            instance.currentlyEditing = null;
            instance.currentEditingUIR.Dispose();
            instance.currentEditingUIR = null;
        }

        public override void editString(global::com.codename1.ui.Component n1, int n2, int n3, global::java.lang.String n4, int n5)
        {
            using (AutoResetEvent are = new AutoResetEvent(false))
            {
                System.Windows.Deployment.Current.Dispatcher.BeginInvoke(() =>
                {
                    textInputInstance.Text = toCSharp(n4);
                    textInputInstance.IsEnabled = true;
                    currentlyEditing = (com.codename1.ui.TextArea)n1;
                    com.codename1.ui.Font fnt = (com.codename1.ui.Font)((com.codename1.ui.plaf.Style)currentlyEditing.getStyle()).getFont();
                    XNAFont font = (XNAFont)fnt.getNativeFont();
                    if(font == null) {
                        font = defaultFont;
                    }
                    font.applyFont(textInputInstance);
                    textInputInstance.Height = currentlyEditing.getHeight();
                    textInputInstance.Width = currentlyEditing.getWidth();
                    textInputInstance.Margin = new Thickness(0);
                    textInputInstance.AcceptsReturn = !currentlyEditing.isSingleLineTextArea();
                    textInputInstance.Focus();
                });
            }
            com.codename1.ui.Display d = (com.codename1.ui.Display)com.codename1.ui.Display.getInstance();
            d.invokeAndBlock(new WaitForEdit());
            using (AutoResetEvent are = new AutoResetEvent(false))
            {
                System.Windows.Deployment.Current.Dispatcher.BeginInvoke(() =>
                {
                    textInputInstance.IsEnabled = false;
                    app.Focus();
                });
            }
        }

        void textChangedEvent(object sender, EventArgs e)
        {
            com.codename1.ui.Display disp = (com.codename1.ui.Display)com.codename1.ui.Display.getInstance();
            Tchange t = new Tchange();
            t.currentlyEditing = currentlyEditing;
            t.text = toJava(textInputInstance.Text);
            disp.callSerially(t);
        }

        class Tchange : java.lang.Object, java.lang.Runnable
        {
            public com.codename1.ui.TextArea currentlyEditing;
            public java.lang.String text;
            public virtual void run()
            {
                if (currentlyEditing != null)
                {
                    currentlyEditing.setText(text);
                }
            }
        }

        public override void flushGraphics(int x, int y, int w, int h)
        {
            //Rectangle r = new Rectangle(x, y, w, h);
            //SharedGraphicsDeviceManager.Current.GraphicsDevice.Present(r, r, IntPtr.Zero);
            lock (PAINT_LOCK)
            {
                pendingPaints.Insert(0, new SetClip(x, y, w, h));
                currentPaints.AddRange(pendingPaints);
                pendingPaints.Clear();
            }
            global::System.Threading.Thread.Sleep(10);
        }

        public override void flushGraphics()
        {
            /*lock (DRAW_LOCK)
            {
                RenderTarget2D oldTarget = currentlyShowing;
                currentlyShowing = pendingTarget;
                graphics.target = oldTarget;
                pendingTarget = oldTarget;
                SharedGraphicsDeviceManager.Current.GraphicsDevice.SetRenderTarget(oldTarget);
                SpriteBatch b = new SpriteBatch(oldTarget.GraphicsDevice);
                b.Begin();
                b.Draw(currentlyShowing, new Vector2(0, 0), Color.White);
                b.End();
                SharedGraphicsDeviceManager.Current.GraphicsDevice.SetRenderTarget(null);
            }*/
            lock (PAINT_LOCK)
            {
                currentPaints.AddRange(pendingPaints);
                pendingPaints.Clear();
            }
            global::System.Threading.Thread.Sleep(10);
        }

        public override bool isOpaque(global::com.codename1.ui.Image n1, global::java.lang.Object n2)
        {
            XNAImage i = (XNAImage)n2;
            // TODO: take x/y into consideration
            // FIX error with scaled images!!!
            int[] data = new int[i.image.Width * i.image.Height];
            i.image.GetData<int>(data);
            for (int iter = 0; iter < data.Length; iter++)
            {
                if ((data[iter] & 0xff000000) != 0xff000000)
                {
                    return false;
                }
            }
            return true;
        }

        public override void getRGB(global::java.lang.Object n1, global::org.xmlvm._nArrayAdapter<int> arr, int offset, int x, int y, int w, int h)
        {
            XNAImage i = (XNAImage)n1;
            // TODO: take x/y into consideration
            // FIX error with scaled images!!!
            i.image.GetData<int>(arr.getCSharpArray(), offset, w * h);
        }

        public override global::System.Object createImage(global::org.xmlvm._nArrayAdapter<int> n1, int n2, int n3)
        {
            RenderTarget2D rt = new RenderTarget2D(SharedGraphicsDeviceManager.Current.GraphicsDevice, n2, n3);
            int[] arr = n1.getCSharpArray();
            rt.SetData(arr);
            return new XNAImage(rt);
        }

        public override global::System.Object createImage(global::java.lang.String n1)
        {
            //return new XNAImage(content.Load<Texture2D>(toCSharp(n1)));
            return createImage((global::java.io.InputStream)getResourceAsStream(null, n1));
        }

        public override global::System.Object createImage(global::java.io.InputStream n1)
        {
            global::org.xmlvm._nArrayAdapter<sbyte> b = (global::org.xmlvm._nArrayAdapter<sbyte>)com.codename1.io.Util.readInputStream(n1);
            return createImage(b, 0, b.Length);
        }

        public override global::System.Object createMutableImage(int n1, int n2, int n3)
        {
            RenderTarget2D rt = new RenderTarget2D(SharedGraphicsDeviceManager.Current.GraphicsDevice, n1, n2, false,
                SurfaceFormat.Color, DepthFormat.None,
                0, RenderTargetUsage.PreserveContents);
            return new XNAImage(rt);
        }

        public static sbyte[] toSByteArray(byte[] byteArray)
        {
            sbyte[] sbyteArray = null;
            if (byteArray != null)
            {
                sbyteArray = new sbyte[byteArray.Length];
                for (int index = 0; index < byteArray.Length; index++)
                    sbyteArray[index] = (sbyte)byteArray[index];
            }
            return sbyteArray;
        }

        public static byte[] toByteArray(sbyte[] byteArray)
        {
            byte[] sbyteArray = null;
            if (byteArray != null)
            {
                sbyteArray = new byte[byteArray.Length];
                for (int index = 0; index < byteArray.Length; index++)
                    sbyteArray[index] = (byte)byteArray[index];
            }
            return sbyteArray;
        }

        public override global::System.Object createImage(global::org.xmlvm._nArrayAdapter<sbyte> n1, int n2, int n3)
        {
            Stream s;
            if (n1.Length != n3 || n2 != 0)
            {
                // TODO
                s = new MemoryStream(toByteArray(n1.getCSharpArray()));
            }
            else
            {
                s = new MemoryStream(toByteArray(n1.getCSharpArray()));
            }
            return new XNAImage(Texture2D.FromStream(SharedGraphicsDeviceManager.Current.GraphicsDevice, s));
        }

        public override void systemOut(global::java.lang.String n1)
        {
            System.Diagnostics.Debug.WriteLine(toCSharp(n1));
        }

        public override int getImageWidth(global::java.lang.Object n1)
        {
            return ((XNAImage)n1).Width;
        }

        public override int getImageHeight(global::java.lang.Object n1)
        {
            return ((XNAImage)n1).Height;
        }

        public override global::System.Object scale(global::java.lang.Object n1, int n2, int n3)
        {
            XNAImage x = (XNAImage)n1;
            XNAImage newX = new XNAImage();
            newX.image = x.image;
            newX.Width = n2;
            newX.Height = n3;
            return newX;
        }

        public override int getSoftkeyCount()
        {
            return 0;
        }

        public override global::System.Object getSoftkeyCode(int n1)
        {
            return null;
        }

        public override int getClearKeyCode()
        {
            return 0;
        }

        public override int getBackspaceKeyCode()
        {
            return 0;
        }

        public override int getBackKeyCode()
        {
            return BACK_KEY;
        }

        public override int getGameAction(int n1)
        {
            return 0;
        }

        public override int getKeyCode(int n1)
        {
            return 0;
        }

        public override bool isTouchDevice()
        {
            return true;
        }

        public override int getColor(global::java.lang.Object n1)
        {
            return ((XNAGraphics)n1).Color;
        }

        public override void setColor(global::java.lang.Object n1, int n2)
        {
            ((XNAGraphics)n1).Color = n2;
        }

        public override void setAlpha(global::java.lang.Object n1, int n2)
        {
            ((XNAGraphics)n1).Alpha = n2;
        }

        public override int getAlpha(global::java.lang.Object n1)
        {
            return ((XNAGraphics)n1).Alpha;
        }

        public override void setNativeFont(global::java.lang.Object n1, global::java.lang.Object n2)
        {
            ((XNAGraphics)n1).currentFont = (XNAFont)n2;
        }

        public override int getClipX(global::java.lang.Object n1)
        {
            return ((XNAGraphics)n1).clipX;
        }

        public override int getClipY(global::java.lang.Object n1)
        {
            return ((XNAGraphics)n1).clipY;
        }

        public override int getClipWidth(global::java.lang.Object n1)
        {
            return ((XNAGraphics)n1).clipW;
        }

        public override int getClipHeight(global::java.lang.Object n1)
        {
            return ((XNAGraphics)n1).clipH;
        }

        public override void setClip(global::java.lang.Object n1, int n2, int n3, int n4, int n5)
        {
            ((XNAGraphics)n1).clipX = n2;
            ((XNAGraphics)n1).clipY = n3;
            ((XNAGraphics)n1).clipW = n4;
            ((XNAGraphics)n1).clipH = n5;
            if (pendingPaints.Count > 0)
            {
                OperationPending o = pendingPaints[pendingPaints.Count - 1];
                if (o is SetClip)
                {
                    pendingPaints[pendingPaints.Count - 1] = new SetClip(n2, n3, n4, n5);
                    return;
                }
            }
            pendingPaints.Add(new SetClip(n2, n3, n4, n5));
        }

        public override void clipRect(global::java.lang.Object n1, int n2, int n3, int n4, int n5)
        {
            XNAGraphics x = ((XNAGraphics)n1);
            Rectangle a = new Rectangle(x.clipX, x.clipY, x.clipW, x.clipH);
            Rectangle b = new Rectangle(n2, n3, n4, n5);
            Rectangle r = Rectangle.Intersect(a, b);
            setClip(n1, r.X, r.Y, r.Width, r.Height);
        }

        public override void drawLine(global::java.lang.Object n1, int x1, int y1, int x2, int y2)
        {
            XNAGraphics x = (XNAGraphics)n1;
            x.performDrawOperation(new DrawLine(x1, y1, x2, y2, x.cColor, x.Alpha), pendingPaints);
        }

        public override void fillRect(global::java.lang.Object n1, int x, int y, int w, int h)
        {
            XNAGraphics xg = (XNAGraphics)n1;
            xg.performDrawOperation(new FillRect(x, y, w, h, xg.cColor, xg.Alpha), pendingPaints);
        }

        public override void drawRect(global::java.lang.Object n1, int x, int y, int w, int h)
        {
            XNAGraphics xg = (XNAGraphics)n1;
            xg.performDrawOperation(new DrawRect(x, y, w, h, xg.cColor, xg.Alpha), pendingPaints);
        }

        public override void drawRoundRect(global::java.lang.Object n1, int n2, int n3, int n4, int n5, int n6, int n7)
        {
        }

        public override void fillRoundRect(global::java.lang.Object n1, int n2, int n3, int n4, int n5, int n6, int n7)
        {
        }

        public override void fillArc(global::java.lang.Object n1, int n2, int n3, int n4, int n5, int n6, int n7)
        {
        }

        public override void drawArc(global::java.lang.Object n1, int n2, int n3, int n4, int n5, int n6, int n7)
        {
        }

        public override void drawString(global::java.lang.Object n1, global::java.lang.String n2, int n3, int n4)
        {
            XNAGraphics xg = (XNAGraphics)n1;
            DrawString d = new DrawString(n3, n4, toCSharp(n2), xg.cColor, xg.currentFont, xg.Alpha);
            xg.performDrawOperation(d, pendingPaints);
        }

        public override void drawImage(global::java.lang.Object n1, global::java.lang.Object img, int x, int y)
        {
            XNAGraphics xg = (XNAGraphics)n1;
            XNAImage im = (XNAImage)img;
            xg.performDrawOperation(new DrawImage(x, y, im.Width, im.Height, im, xg.Alpha), pendingPaints);
        }

        public override void drawImage(global::java.lang.Object n1, global::java.lang.Object img, int x, int y, int w, int h)
        {
            XNAGraphics xg = (XNAGraphics)n1;
            XNAImage im = (XNAImage)img;
            im.release();
            xg.performDrawOperation(new DrawImage(x, y, w, h, im, xg.Alpha), pendingPaints);
        }

        public override bool isScaledImageDrawingSupported()
        {
            return true;
        }

        public void drawUIR(SilverPeer peer)
        {
            pendingPaints.Add(new DrawUIR(peer));
        }

        public override void drawRGB(global::java.lang.Object n1, global::org.xmlvm._nArrayAdapter<int> n2, int n3, int n4, int n5, int n6, int n7, bool n8)
        {
        }

        public override global::System.Object getNativeGraphics()
        {
            return graphics;
        }

        public override global::System.Object getNativeGraphics(global::java.lang.Object n1)
        {
            return ((XNAImage)n1).graphics;
        }

        public override int charsWidth(global::java.lang.Object n1, global::org.xmlvm._nArrayAdapter<char> n2, int off, int len)
        {
            java.lang.String str = new java.lang.String();
            str.@this(n2, off, len);
            return stringWidth(n1, str);
        }

        public override int stringWidth(global::java.lang.Object n1, global::java.lang.String n2)
        {
            XNAFont fnt = (XNAFont)n1;
            if (fnt == null)
            {
                fnt = defaultFont;
            }
            return fnt.stringWidth(toCSharp(n2), textBlockInstance);
        }

        public override int charWidth(global::java.lang.Object n1, char n2)
        {
            XNAFont fnt = (XNAFont)n1;
            if (fnt == null)
            {
                fnt = defaultFont;
            }
            return fnt.stringWidth("" + n2, textBlockInstance);
        }

        public override int getFace(global::java.lang.Object n1){
            XNAFont fnt = (XNAFont)n1;
            if (fnt == null)
            {
                fnt = defaultFont;
            }
            return fnt.face;
        }

        public override int getSize(global::java.lang.Object n1)
        {
            XNAFont fnt = (XNAFont)n1;
            if (fnt == null)
            {
                fnt = defaultFont;
            }
            return fnt.size;
        }

        public override int getStyle(global::java.lang.Object n1)
        {
            XNAFont fnt = (XNAFont)n1;
            if (fnt == null)
            {
                fnt = defaultFont;
            }
            return fnt.style;
        }

        public override int getHeight(global::java.lang.Object n1)
        {
            XNAFont fnt = (XNAFont)n1;
            if (fnt == null)
            {
                fnt = defaultFont;
            }
            if (fnt.fontHeight > -1)
            {
                return fnt.fontHeight;
            }
            using (AutoResetEvent are = new AutoResetEvent(false))
            {
                System.Windows.Deployment.Current.Dispatcher.BeginInvoke(() =>
                {
                    textBlockInstance.Text = "X";
                    fnt.applyFont(textBlockInstance);
                    textBlockInstance.Measure(new Size(10000, 10000));
                    fnt.fontHeight = (int)System.Math.Round(textBlockInstance.DesiredSize.Height);
                    are.Set();
                });
                are.WaitOne();
            }

            return fnt.fontHeight;
        }

        public override global::System.Object getDefaultFont()
        {
            return defaultFont;
        }

        public override global::System.Object createFont(int face, int style, int size)
        {
            XNAFont x = new XNAFont();
            x.face = face;
            x.style = style;
            x.size = size;
            return x;
        }

        public override global::System.Object connect(global::java.lang.String n1, bool read, bool write)
        {
            NetworkOperation n = new NetworkOperation();
            n.request = (HttpWebRequest)WebRequest.Create(new Uri(toCSharp(n1)));
            n.request.AllowAutoRedirect = false;
            return n;
        }

        public override void setHeader(global::java.lang.Object n1, global::java.lang.String n2, global::java.lang.String n3)
        {
            NetworkOperation n = (NetworkOperation)n1;
            n.request.Headers[toCSharp(n2)] = toCSharp(n3);
        }

        public override int getContentLength(global::java.lang.Object n1)
        {
            NetworkOperation n = (NetworkOperation)n1;
            return (int)n.response.ContentLength;
        }

        public override global::System.Object openOutputStream(global::java.lang.Object n1)
        {
            NetworkOperation n = (NetworkOperation)n1;
            return new OutputStreamProxy(n.requestStream);
        }

        public override global::System.Object openOutputStream(global::java.lang.Object n1, int n2)
        {
            return null;
        }

        public override global::System.Object openInputStream(global::java.lang.Object n1)
        {
            NetworkOperation n = (NetworkOperation)n1;
            return new InputStreamProxy(n.response.GetResponseStream());
        }

        public override void setPostRequest(global::java.lang.Object n1, bool n2)
        {
            NetworkOperation n = (NetworkOperation)n1;
            if (n2)
            {
                n.request.Method = "POST";
            }
            else
            {
                n.request.Method = "GET";
            }

        }

        public override int getResponseCode(global::java.lang.Object n1)
        {
            NetworkOperation n = (NetworkOperation)n1;
            return (int)n.response.StatusCode;
        }

        public override global::System.Object getResponseMessage(global::java.lang.Object n1)
        {
            return null;
        }

        public override void vibrate(int n1)
        {
            VibrateController vc = VibrateController.Default;
            vc.Start(TimeSpan.FromMilliseconds(n1));
        }

        public override global::System.Object getHeaderField(global::java.lang.String n1, global::java.lang.Object n2)
        {
            NetworkOperation n = (NetworkOperation)n2;
            return toJava(n.response.Headers[toCSharp(n1)]);
        }

        public override global::System.Object getHeaderFieldNames(global::java.lang.Object n1)
        {
            NetworkOperation n = (NetworkOperation)n1;
            int i = n.response.Headers.Count;
            java.lang.String[] arr = new java.lang.String[i];
            _nArrayAdapter<global::System.Object> r = new _nArrayAdapter<global::System.Object>(arr);
            string[] keys = n.response.Headers.AllKeys;
            for (int iter = 0; iter < i; iter++)
            {
                arr[iter] = toJava(keys[iter]);
            }
            return r;
        }

        public override global::System.Object getHeaderFields(global::java.lang.String n1, global::java.lang.Object n2)
        {
            NetworkOperation n = (NetworkOperation)n2;
            String s = n.response.Headers[toCSharp(n1)];
            if (s == null)
            {
                return null;
            }
            return new _nArrayAdapter<global::System.Object>(new java.lang.String[] {toJava(s)});
        }

        public override int getCommandBehavior()
        {
            // COMMAND_BEHAVIOR_BUTTON_BAR
            return 4;
        }

        public override void deleteStorageFile(global::java.lang.String n1)
        {
            IsolatedStorageFile store = IsolatedStorageFile.GetUserStoreForApplication();
            store.DeleteFile(toCSharp(n1));
        }

        public override global::System.Object createStorageOutputStream(global::java.lang.String n1)
        {
            IsolatedStorageFile store = IsolatedStorageFile.GetUserStoreForApplication();
            return new OutputStreamProxy(store.OpenFile(toCSharp(n1), FileMode.Create));
        }
        
        public override bool hasNativeTheme()
        {
            return true;
        }

        public override void installNativeTheme()
        {
            com.codename1.ui.util.Resources r = (com.codename1.ui.util.Resources)com.codename1.ui.util.Resources.open(toJava("winTheme.res"));
            com.codename1.ui.plaf.UIManager uim = (com.codename1.ui.plaf.UIManager)com.codename1.ui.plaf.UIManager.getInstance();
            global::System.Object[] themeNames = ((_nArrayAdapter<global::System.Object>)r.getThemeResourceNames()).getCSharpArray();
            uim.setThemeProps((java.util.Hashtable)r.getTheme((java.lang.String)themeNames[0]));
            com.codename1.ui.plaf.DefaultLookAndFeel dl = (com.codename1.ui.plaf.DefaultLookAndFeel)uim.getLookAndFeel();
            dl.setDefaultEndsWith3Points(false);
        }

        public override global::System.Object createStorageInputStream(global::java.lang.String n1)
        {
            IsolatedStorageFile store = IsolatedStorageFile.GetUserStoreForApplication();
            return new InputStreamProxy(store.OpenFile(toCSharp(n1), FileMode.Open));
        }

        public override bool storageFileExists(global::java.lang.String n1)
        {
            IsolatedStorageFile store = IsolatedStorageFile.GetUserStoreForApplication();
            return store.FileExists(toCSharp(n1));
        }

        public override global::System.Object listStorageEntries()
        {
            IsolatedStorageFile store = IsolatedStorageFile.GetUserStoreForApplication();
            string[] arr = store.GetFileNames();
            java.lang.String[] resp = new java.lang.String[arr.Length];
            for (int iter = 0; iter < resp.Length; iter++)
            {
                resp[iter] = toJava(arr[iter]);
            }
            return new _nArrayAdapter<java.lang.Object>(resp);
        }

        public override global::System.Object listFilesystemRoots()
        {
            return null;
        }

        public override global::System.Object listFiles(global::java.lang.String n1)
        {
            return null;
        }

        public override long getRootSizeBytes(global::java.lang.String n1)
        {
            return 0;
        }

        public override long getRootAvailableSpace(global::java.lang.String n1)
        {
            return 0;
        }

        public override void mkdir(global::java.lang.String n1)
        {
        }

        public override global::System.Object createSoftWeakRef(global::java.lang.Object n1)
        {
            return new SoftRef(n1);
        }

        public override int getDragAutoActivationThreshold()
        {
            return 1000;
        }

        public override global::System.Object extractHardRef(global::java.lang.Object n1)
        {
            if (n1 != null)
            {
                return ((SoftRef)n1).get();
            }
            return null;
        }


        public override void deleteFile(global::java.lang.String n1)
        {
        }

        public override bool isHidden(global::java.lang.String n1)
        {
            return false;
        }

        public override void setHidden(global::java.lang.String n1, bool n2)
        {
        }

        public override long getFileLength(global::java.lang.String n1)
        {
            return 0;
        }

        public override bool isDirectory(global::java.lang.String n1)
        {
            return false;
        }

        public override bool exists(global::java.lang.String n1)
        {
            return false;
        }

        public override void rename(global::java.lang.String n1, global::java.lang.String n2)
        {
        }

        public override char getFileSystemSeparator()
        {
            return '\\';
        }

        public override global::System.Object getPlatformName()
        {
            return toJava("win");
        }

        public override global::System.Object getLocalizationManager()
        {
            //XMLVM_BEGIN_WRAPPER[com.codename1.impl.SilverlightImplementation: com.codename1.l10n.L10NManager getLocalizationManager()]
            global::org.xmlvm._nElement _r0;
            _r0.i = 0;
            _r0.l = 0;
            _r0.f = 0;
            _r0.d = 0;
            global::System.Object _r0_o = null;
            global::org.xmlvm._nElement _r1;
            _r1.i = 0;
            _r1.l = 0;
            _r1.f = 0;
            _r1.d = 0;
            global::System.Object _r1_o = null;
            global::org.xmlvm._nExceptionAdapter _ex = null;
            _r1_o = this;
            _r0_o = new global::com.codename1.impl.SilverlightImplementation_2L10NManagerImpl();
            ((global::com.codename1.impl.SilverlightImplementation_2L10NManagerImpl)_r0_o).@this();
            return (global::com.codename1.l10n.L10NManager)_r0_o;
            //XMLVM_END_WRAPPER[com.codename1.impl.SilverlightImplementation: com.codename1.l10n.L10NManager getLocalizationManager()]
        }


        public override global::System.Object getResourceAsStream(global::java.lang.Class n1, global::java.lang.String n2)
        {
            try
            {
                String uri = toCSharp(n2);
                if (uri.StartsWith("/"))
                {
                    uri = "res\\" + uri.Substring(1);
                }
                Uri uriResource = new Uri(uri, UriKind.Relative);
                StreamResourceInfo si = Application.GetResourceStream(uriResource);
                Stream strm = si.Stream;
                byte[] byteArr = new byte[strm.Length];
                strm.Read(byteArr, 0, byteArr.Length);
                java.io.ByteArrayInputStream bi = new java.io.ByteArrayInputStream();
                bi.@this(new _nArrayAdapter<sbyte>(toSByteArray(byteArr)));
                return bi;
            }
            catch (System.Exception err)
            {
                return null;
            }
        }

        public static java.lang.String toJava(System.String str)
        {
            global::org.xmlvm._nArrayAdapter<char> n = new global::org.xmlvm._nArrayAdapter<char>(str.ToCharArray());
            java.lang.String s = new java.lang.String();
            s.@this(n);
            return s;
        }

        public static string toCSharp(java.lang.String str)
        {
            global::org.xmlvm._nArrayAdapter<char> n = (global::org.xmlvm._nArrayAdapter<char>)str.toCharArray();
            return new string(n.getCSharpArray());
        }

        private static bool mouseDown;

        public static void update()
        {
            MouseState mst = Mouse.GetState();
            if (mouseDown)
            {
                if (mst.LeftButton == ButtonState.Released)
                {
                    mouseDown = false;
                    instance.pointerReleased(mst.X, mst.Y);

                    if (instance.currentlyEditing != null)
                    {
                        com.codename1.ui.Form f = (com.codename1.ui.Form)instance.currentlyEditing.getComponentForm();
                        if (f.getComponentAt(mst.X, mst.Y) != instance.currentlyEditing)
                        {
                            commitEditing();
                        }
                    }
                }
                else
                {
                    instance.pointerDragged(mst.X, mst.Y);
                }
            }
            else
            {
                if (mst.LeftButton == ButtonState.Pressed)
                {
                    mouseDown = true;
                    instance.pointerPressed(mst.X, mst.Y);
                }
            }

            if (currentPaints.Count > 0)
            {
                GraphicsDevice gd = SharedGraphicsDeviceManager.Current.GraphicsDevice;
                gd.SetRenderTarget(instance.currentlyShowing);

                lock (PAINT_LOCK)
                {
                    for (int iter = 0; iter < currentPaints.Count; iter++)
                    {
                        currentPaints[iter].prerender();
                    }
                    if (instance.currentlyEditing != null)
                    {
                        if (instance.currentEditingUIR == null)
                        {
                            instance.currentEditingUIR = new UIElementRenderer(textInputInstance, instance.currentlyEditing.getWidth(), instance.currentlyEditing.getHeight());
                        }
                        instance.currentEditingUIR.Render();
                    }
                    //System.Diagnostics.Debug.WriteLine("Painting " + currrentPaints.Count);
                    while (currentPaints.Count > 0)
                    {
                        OperationPending pen = currentPaints[0];
                        currentPaints.RemoveAt(0);

                        if (pen.isDrawOperation())
                        {
                            instance.spriteBatch.Begin(SpriteSortMode.Immediate, BlendState.NonPremultiplied, null, null, instance._rasterizerState);
                            gd = instance.spriteBatch.GraphicsDevice;
                            gd.RasterizerState = instance._rasterizerState;
                            if (instance.graphics.clip != null)
                            {
                                try
                                {
                                    gd.ScissorRectangle = instance.graphics.clip.Value;
                                }
                                catch (System.Exception err)
                                {
                                    // C# throws exception for invalid clipping means that its our of bounds.
                                    gd.ScissorRectangle = Rectangle.Empty;
                                }
                            }

                            //pen.performBench(instance.graphics, instance.spriteBatch);
                            pen.perform(instance.graphics, instance.spriteBatch);
                            instance.spriteBatch.End();
                        }
                        else
                        {
                            //pen.performBench(instance.graphics, instance.spriteBatch);
                            pen.perform(instance.graphics, instance.spriteBatch);
                        }
                        //pen.printLogging();
                        //pen.printBench();
                    }
                }
                /*for (int iter = 0; iter < pendingPaints.Count; iter++)
                {
                    OperationPending pen = pendingPaints[iter];
                    pen.perform(instance.graphics, instance.spriteBatch);
                }*/

                gd.SetRenderTarget(null);
            }
        }

        private static SpriteBatch sb = null;
        public static void draw()
        {
            GraphicsDevice gd = SharedGraphicsDeviceManager.Current.GraphicsDevice;
            //gd.Clear(Color.Red);
            if (sb == null)
            {
                sb = new SpriteBatch(gd);
            }

            sb.Begin();
            gd.ScissorRectangle = new Rectangle(0, 0, instance.getDisplayWidth(), instance.getDisplayHeight());
            sb.Draw(instance.currentlyShowing, new Vector2(0, 0), Color.White);
            if (instance.currentlyEditing != null && instance.currentEditingUIR != null)
            {
                sb.Draw(instance.currentEditingUIR.Texture, new Vector2(instance.currentlyEditing.getAbsoluteX(), instance.currentlyEditing.getAbsoluteY()), Color.White);                
            }
            sb.End();
        }

        //XMLVM_BEGIN_WRAPPER[com.codename1.impl.SilverlightImplementation]
        //XMLVM_END_WRAPPER[com.codename1.impl.SilverlightImplementation]

    } // end of class: SilverlightImplementation

    public abstract class OperationPending
    {
        int tick;
        public virtual bool isDrawOperation()
        {
            return true;
        }

        public virtual void prerender()
        {
        }

        public void performBench(XNAGraphics xg, SpriteBatch batch)
        {
            tick = System.Environment.TickCount;
            perform(xg, batch);
        }

        public abstract void perform(XNAGraphics xg, SpriteBatch batch);

        public void log(String s)
        {
            //System.Diagnostics.Debug.WriteLine(s);
        }

        public void printBench()
        {
            System.Diagnostics.Debug.WriteLine(GetType().Name + " took " + (System.Environment.TickCount - tick));
        }

        public abstract void printLogging();
    }


    class FillRect : OperationPending
    {
        private int x, y, w, h;
        private Color col;
        public FillRect(int x, int y, int w, int h, Color col, int alpha)
        {
            this.x = x;
            this.y = y;
            this.w = w;
            this.h = h;
            this.col = new Color(col.R, col.G, col.B, alpha);
        }

        public override void perform(XNAGraphics xg, SpriteBatch batch)
        {
            Texture2D t = new Texture2D(xg.target.GraphicsDevice, 1, 1);
            t.SetData(new Color[] { col });
            batch.Draw(t, new Rectangle(x, y, w, h), col);
        }

        public override void printLogging()
        {
            log("FillRect at x: " + x + " y: " + y + " w: " + w + " h: " + h + " color: " + col.ToString());
        }
    }

    class DrawImage : OperationPending
    {
        private int alpha;
        private int x, y, w, h;
        private XNAImage img;
        public DrawImage(int x, int y, int w, int h, XNAImage img, int alpha)
        {
            this.alpha = alpha;
            this.x = x;
            this.y = y;
            this.w = w;
            this.h = h;
            this.img = img;
        }

        public override void prerender()
        {
            img.release();
        }

        public override void perform(XNAGraphics xg, SpriteBatch batch)
        {
            if (alpha != 255)
            {
                batch.Draw(img.image, new Rectangle(x, y, w, h), new Color(255, 255, 255, alpha));
            }
            else
            {
                batch.Draw(img.image, new Rectangle(x, y, w, h), Color.White);
            }
        }

        public override void printLogging()
        {
            log("Drawing image at x: " + x + " y: " + y + " w: " + w + " h: " + h + " image original width: " + img.Width + " height: " + img.Height);
        }
    }

    class DrawUIR : OperationPending
    {
        private SilverPeer peer;
        public DrawUIR(SilverPeer peer)
        {
            this.peer = peer;
        }

        public override void prerender()
        {
            peer.uir.Render();
        }

        public override void perform(XNAGraphics xg, SpriteBatch batch)
        {
            batch.Draw(peer.uir.Texture, new Vector2(peer.getAbsoluteX(), peer.getAbsoluteY()), Color.White);
        }

        public override void printLogging()
        {
            log("Drawing Peer Component " + SilverlightImplementation.toCSharp((java.lang.String)peer.toString()));
        }
    }

    class DrawString : OperationPending
    {
        private static global::System.Collections.Generic.List<DrawString> uirCache = new global::System.Collections.Generic.List<DrawString>();

        private int x, y;
        private Color col;
        private String str;
        private UIElementRenderer uir;
        private XNAFont currentFont;
        public DrawString(int x, int y, String str, Color col, XNAFont currentFont, int alpha)
        {
            this.x = x;
            this.y = y;
            this.str = str;
            this.col = new Color(col.R, col.G, col.B, alpha);
            this.currentFont = currentFont;
        }

        public override bool Equals(System.Object obj)
        {
            DrawString ds = (DrawString)obj;
            return str.Equals(ds.str) && currentFont.Equals(ds.currentFont) && col.Equals(ds.col);
        }
        public override int GetHashCode()
        {
            return str.GetHashCode();
        }

        public override void prerender()
        {
            if (str.Length == 0)
            {
                return;
            }
            if (currentFont == null)
            {
                currentFont = new XNAFont();
            }
            int off = uirCache.IndexOf(this);
            if (off > -1)
            {
                uir = uirCache[off].uir;
                uirCache.RemoveAt(off);
                uirCache.Insert(0, this);
                return;
            }
            SilverlightImplementation.textBlockInstance.Text = str;
            SilverlightImplementation.textBlockInstance.Foreground = new System.Windows.Media.SolidColorBrush(System.Windows.Media.Color.FromArgb(col.A, col.R, col.G, col.B));
            currentFont.applyFont(SilverlightImplementation.textBlockInstance);
            SilverlightImplementation.textBlockInstance.Measure(new Size(10000, 10000));
            int dw = (int)SilverlightImplementation.textBlockInstance.DesiredSize.Width;
            int dh = (int)SilverlightImplementation.textBlockInstance.DesiredSize.Height;

            uir = new UIElementRenderer(SilverlightImplementation.textBlockInstance, dw, dh);
            uir.Render();
            uirCache.Insert(0, this);
            if (uirCache.Count > 50)
            {
                uirCache[50].uir.Dispose();
                uirCache.RemoveAt(50);
            }
        }

        public override void perform(XNAGraphics xg, SpriteBatch batch)
        {
            if (uir != null)
            {
                batch.Draw(uir.Texture, new Vector2(x, y), col);
            }
        }

        public override void printLogging()
        {
            log("Draw String at x: " + x + " y: " + y + " color: " + col.ToString() + " '" + str + "'");
        }
    }


    class DrawLine : OperationPending
    {
        private int x1, y1, x2, y2;
        private Color col;
        public DrawLine(int x1, int y1, int x2, int y2, Color col, int alpha)
        {
            this.x1 = x1;
            this.y1 = y1;
            this.x2 = x2;
            this.y2 = y2;
            this.col = new Color(col.R, col.G, col.B, alpha);
        }

        public override void perform(XNAGraphics xg, SpriteBatch batch)
        {
            VertexPositionColor[] vertices = new VertexPositionColor[2];
            vertices[0].Position = new Vector3(x1, y1, 0);
            vertices[0].Color = xg.cColor;
            vertices[1].Position = new Vector3(x2, y2, 0);
            vertices[1].Color = vertices[0].Color;
            xg.basicEffect.CurrentTechnique.Passes[0].Apply();
            xg.target.GraphicsDevice.DrawUserPrimitives<VertexPositionColor>(PrimitiveType.LineList, vertices, 0, 1);
            /*Vector2 firstPosition = new Vector2(x1, y1);
            Vector2 secondPosition = new Vector2(x2, y2);
            float distance = Vector2.Distance(firstPosition, secondPosition);
            float rotation = (float)Math.Atan2(secondPosition.Y - firstPosition.Y,
                            secondPosition.X - firstPosition.X);

            Texture2D t = new Texture2D(xg.target.GraphicsDevice, 1, 1);
            t.SetData(new Color[] { col });
            //batch.Draw(t, firstPosition, null, col, rotation, Vector2.Zero, new Vector2(distance, 5), SpriteEffects.None, 0);
             */
        }

        public override bool isDrawOperation()
        {
            return false;
        }

        public override void printLogging()
        {
            log("Draw Line x1: " + x1 + " y1: " + y1 + " x2: " + x2 + " y2: " + y2 + " color: " + col.ToString());
        }
    }

    class DrawRect : OperationPending
    {
        private int x, y, w, h;
        private Color col;
        public DrawRect(int x, int y, int w, int h, Color col, int alpha)
        {
            this.x = x;
            this.y = y;
            this.w = w;
            this.h = h;
            this.col = new Color(col.R, col.G, col.B, alpha);
        }

        public override void perform(XNAGraphics xg, SpriteBatch batch)
        {
            Texture2D t = new Texture2D(xg.target.GraphicsDevice, 1, 1);
            t.SetData(new Color[] { col });
            batch.Draw(t, new Rectangle(x, y, w, 1), col);
            batch.Draw(t, new Rectangle(x, y + h - 1, w, 1), col);
            batch.Draw(t, new Rectangle(x, y, 1, h), col);
            batch.Draw(t, new Rectangle(x + w - 1, y, 1, h), col);
        }

        public override bool isDrawOperation()
        {
            return true;
        }

        public override void printLogging()
        {
            log("Draw rect x: " + x + " y: " + y + " w: " + w + " h: " + h + " color: " + col.ToString());
        }
    }

    class SetClip : OperationPending
    {
        private int x, y, w, h;
        public SetClip(int x, int y, int w, int h)
        {
            this.x = x;
            this.y = y;
            this.w = w;
            this.h = h;
        }

        public override bool isDrawOperation()
        {
            return false;
        }

        public override void perform(XNAGraphics xg, SpriteBatch batch)
        {
            xg.clip = new Rectangle(x, y, w, h);
        }

        public override void printLogging()
        {
            log("Clipping to at x: " + x + " y: " + y + " w: " + w + " h: " + h);
        }
    }



    class WaitForEdit : java.lang.Object, java.lang.Runnable
    {
        public virtual void run() {
            while (SilverlightImplementation.instance.currentlyEditing != null)
            {
                global::System.Threading.Thread.Sleep(30);
            }
        }
    }


    class NetworkOperation : java.lang.Object
    {
        private bool responseCompleted;
        private bool postCompleted;
        public HttpWebRequest request;
        
        public Stream requestStream {
            get {
                if(postData == null) {
                    request.BeginGetRequestStream(PostCallback, request);
                    while (!postCompleted)
                    {
                        System.Threading.Thread.Sleep(5);
                    }
                }
                return postData;
            }
        }
        private Stream postData;

        public HttpWebResponse response
        {
            get {
                if (resp == null)
                {
                    request.BeginGetResponse(ResponseCallback, request);
                    while (!responseCompleted)
                    {
                        System.Threading.Thread.Sleep(5);
                    }
                }
                return resp;
            }
        }
        private HttpWebResponse resp;

        private void ResponseCallback(IAsyncResult asyncResult)
        {
            resp = (HttpWebResponse)request.EndGetResponse(asyncResult);
            responseCompleted = true;
        }

        private void PostCallback(IAsyncResult asyncResult)
        {
            postData = request.EndGetRequestStream(asyncResult);
            postCompleted = true;
        }
    }

    class InputStreamProxy : java.io.InputStream
    {
        Stream internalStream;
        public InputStreamProxy(Stream internalStream)
        {
            base.@this();
            this.internalStream = internalStream;
        }

        public override int available()
        {
            return 0;
        }

        public override void close()
        {
            internalStream.Close();
        }


        public override bool markSupported()
        {
            return false;
        }

        public override int read()
        {
            return internalStream.ReadByte();
        }

        public override int read(global::org.xmlvm._nArrayAdapter<sbyte> n1)
        {
            sbyte[] sb = n1.getCSharpArray();
            byte[] buffer = new byte[sb.Length];
            int v = internalStream.Read(buffer, 0, buffer.Length);
            for(int iter = 0 ; iter < v ; iter++) {
                sb[iter] = (sbyte)buffer[iter];
            }
            return v;
        }

        public override int read(global::org.xmlvm._nArrayAdapter<sbyte> n1, int n2, int n3)
        {
            sbyte[] sb = n1.getCSharpArray();
            byte[] buffer = new byte[sb.Length];
            int v = internalStream.Read(buffer, n2, n3);
            for (int iter = n2; iter < v; iter++)
            {
                sb[iter] = (sbyte)buffer[iter];
            }
            return v;
        }

        public override void reset()
        {
        }

    }

    class OutputStreamProxy : java.io.OutputStream
    {
        Stream internalStream;
        public OutputStreamProxy(Stream internalStream)
        {
            base.@this();
            this.internalStream = internalStream;
        }

        public override void close()
        {
            internalStream.Close();
        }

        public override void flush()
        {
            internalStream.Flush();
        }

        public override void write(global::org.xmlvm._nArrayAdapter<sbyte> n1)
        {
            internalStream.Flush();
        }

        public override void write(global::org.xmlvm._nArrayAdapter<sbyte> n1, int n2, int n3)
        {
            internalStream.Write(SilverlightImplementation.toByteArray(n1.getCSharpArray()), n2, n3);
        }

        public override void write(int n1)
        {
            internalStream.WriteByte((byte)n1);
        }

    }
} // end of namespace: com.codename1.impl
