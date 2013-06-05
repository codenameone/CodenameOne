
using org.xmlvm;
using System.Windows;
using System.Windows.Resources;
using System.IO;
using System;
using Microsoft.Phone.Controls;
using System.Windows.Controls;
using System.Threading;
using System.Windows.Input;
using System.Windows.Media.Imaging;
using System.Windows.Media;
using System.Windows.Shapes;
using System.Collections.Generic;
using System.Net;
using Microsoft.Devices;
using System.IO.IsolatedStorage;
using Microsoft.Phone.Tasks;

namespace com.codename1.impl
{
    public class SilverlightImplementation : global::com.codename1.impl.CodenameOneImplementation //, IServiceProvider
    {
        private NativeGraphics globalGraphics = new NativeGraphics();
        private static Object PAINT_LOCK = new Object();

        public static SilverlightImplementation instance;
        private static Canvas cl;
        private int displayWidth = -1, displayHeight = -1;
        private NativeFont defaultFont;
        private com.codename1.ui.Component currentlyPainting;
        public com.codename1.ui.TextArea currentlyEditing;
        public static TextBox textInputInstance;
        public static PhoneApplicationPage app;
        public static LinkedList<UIElement> currentPaintDestination;


        public static void setCanvas(PhoneApplicationPage page, Canvas LayoutRoot)
        {
            cl = LayoutRoot;
            app = page;
        }

        void page_BackKeyPress(object sender, System.ComponentModel.CancelEventArgs e)
        {
            keyPressed(getBackKeyCode());
            keyReleased(getBackKeyCode());
            e.Cancel = true;
        }


        public void @this()
        {
            base.@this();
            instance = this;
        }

        public override bool shouldWriteUTFAsGetBytes()
        {
            return true;
        }


        public override global::System.Object getResourceAsStream(global::java.lang.Class n1, global::java.lang.String n2)
        {
            try
            {
                String uri = toCSharp(n2);
                if (uri.StartsWith("/"))
                {
                    uri = "res/" + uri.Substring(1);
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

        public override void init(java.lang.Object n1)
        {
            instance = this;
            System.Windows.Deployment.Current.Dispatcher.BeginInvoke(() =>
            {
                app.BackKeyPress += page_BackKeyPress;
                Touch.FrameReported += new TouchFrameEventHandler(Touch_FrameReported);
                app.SupportedOrientations = SupportedPageOrientation.PortraitOrLandscape;
                app.OrientationChanged += app_OrientationChanged;
            });
        }

        void app_OrientationChanged(object sender, OrientationChangedEventArgs e)
        {
            displayHeight = (int)cl.ActualHeight;
            displayWidth = (int)cl.ActualWidth;
            sizeChanged(displayWidth, displayHeight);
        }

        public override bool canForceOrientation()
        {
            return true;
        }

        public override global::System.Object getProperty(global::java.lang.String n1, global::java.lang.String n2)
        {
            // TODO
            return base.getProperty(n1, n2);
        }

        public override void exitApplication()
        {
            Application.Current.Terminate();
        }

        public override global::System.Object createMedia(global::java.lang.String uri, bool video, global::java.lang.Runnable onComplete)
        {
            return new CN1Media(toCSharp(uri), video, onComplete);
        }

        public override global::System.Object createMedia(global::java.io.InputStream n1, global::java.lang.String n2, global::java.lang.Runnable n3)
        {
            java.io.OutputStream os = (java.io.OutputStream)createStorageOutputStream(toJava("CN1TempVideodu73aFljhuiw3yrindo87.mp4"));
            com.codename1.io.Util.copy(n1, os);
            IsolatedStorageFile store = IsolatedStorageFile.GetUserStoreForApplication();
            Stream s = store.OpenFile("CN1TempVideodu73aFljhuiw3yrindo87.mp4", FileMode.Open);
            return new CN1Media(s, toCSharp(n2), n3);
        }

        /*public virtual global::System.Object createMediaRecorder(global::java.lang.String n1, global::java.lang.String n2)
        {
        }*/

        public override void lockOrientation(bool n1)
        {
            System.Windows.Deployment.Current.Dispatcher.BeginInvoke(() =>
            {
                if (n1)
                {
                    app.SupportedOrientations = SupportedPageOrientation.Portrait;
                }
                else
                {
                    app.SupportedOrientations = SupportedPageOrientation.Landscape;
                }
            });
        }

        public override void unlockOrientation()
        {
            System.Windows.Deployment.Current.Dispatcher.BeginInvoke(() =>
            {
                app.SupportedOrientations = SupportedPageOrientation.PortraitOrLandscape;
            });
        }

        public override bool hasNativeTheme()
        {
            return true;
        }

        public override void installNativeTheme()
        {
            com.codename1.ui.util.Resources r = (com.codename1.ui.util.Resources)com.codename1.ui.util.Resources.open(toJava("/winTheme.res"));
            com.codename1.ui.plaf.UIManager uim = (com.codename1.ui.plaf.UIManager)com.codename1.ui.plaf.UIManager.getInstance();
            global::System.Object[] themeNames = ((_nArrayAdapter<global::System.Object>)r.getThemeResourceNames()).getCSharpArray();
            uim.setThemeProps((java.util.Hashtable)r.getTheme((java.lang.String)themeNames[0]));
            com.codename1.ui.plaf.DefaultLookAndFeel dl = (com.codename1.ui.plaf.DefaultLookAndFeel)uim.getLookAndFeel();
            dl.setDefaultEndsWith3Points(false);
        }

        void Touch_FrameReported(object sender, TouchFrameEventArgs args)
        {
            TouchPointCollection col = args.GetTouchPoints(cl);
            TouchAction act = args.GetPrimaryTouchPoint(cl).Action;
            if (col.Count == 1)
            {
                if (instance.currentlyEditing != null)
                {
                    com.codename1.ui.Form f = (com.codename1.ui.Form)instance.currentlyEditing.getComponentForm();
                    if (f.getComponentAt((int)col[0].Position.X, (int)col[0].Position.Y) == instance.currentlyEditing) {
                        return;
                    }
                }
                if (act == TouchAction.Down)
                {
                    pointerPressed((int)col[0].Position.X, (int)col[0].Position.Y);
                    return;
                }
                if (act == TouchAction.Up)
                {
                    if (instance.currentlyEditing != null)
                    {
                        com.codename1.ui.Form f = (com.codename1.ui.Form)instance.currentlyEditing.getComponentForm();
                        if (f.getComponentAt((int)col[0].Position.X, (int)col[0].Position.Y) != instance.currentlyEditing)
                        {
                            commitEditing();
                        }
                    }

                    pointerReleased((int)col[0].Position.X, (int)col[0].Position.Y);
                    return;
                }
                if (act == TouchAction.Move)
                {
                    pointerDragged((int)col[0].Position.X, (int)col[0].Position.Y);
                    return;
                }
                return;
            }

            int[] x = new int[col.Count];
            int[] y = new int[x.Length];
            for(int iter = 0 ; iter < col.Count ; iter++) {
                x[iter] = (int)col[iter].Position.X;
                x[iter] = (int)col[iter].Position.Y;
            }
            _nArrayAdapter<int> xarr = new _nArrayAdapter<int>(x);
            _nArrayAdapter<int> yarr = new _nArrayAdapter<int>(y);
            if (act == TouchAction.Down)
            {
                pointerPressed(xarr, yarr);
                return;
            }
            if (act == TouchAction.Up)
            {
                pointerReleased(xarr, yarr);
                return;
            }
            if (act == TouchAction.Move)
            {
                pointerDragged(xarr, yarr);
                return;
            }
        }

        public override int getDisplayWidth()
        {
            if (displayWidth < 0)
            {
                using (AutoResetEvent are = new AutoResetEvent(false))
                {
                    System.Windows.Deployment.Current.Dispatcher.BeginInvoke(() =>
                    {
                        displayWidth = (int)cl.ActualWidth;
                        are.Set();
                    });
                    are.WaitOne();
                }
            }
            return displayWidth;
        }

        public override int getDisplayHeight()
        {
            if (displayHeight < 0)
            {
                using (AutoResetEvent are = new AutoResetEvent(false))
                {
                    System.Windows.Deployment.Current.Dispatcher.BeginInvoke(() =>
                    {
                        displayHeight = (int)cl.ActualHeight;
                        are.Set();
                    });
                    are.WaitOne();
                }
            }
            return displayHeight;
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
        }

        public override void editString(global::com.codename1.ui.Component n1, int n2, int n3, global::java.lang.String n4, int n5)
        {
            com.codename1.ui.Display d = (com.codename1.ui.Display)com.codename1.ui.Display.getInstance();
            if (textInputInstance != null)
            {
                commitEditing();
                d.callSerially(new EditString(n1, n2, n3, n4, n5));
                return;
            }
            currentlyEditing = (com.codename1.ui.TextArea)n1;
            using (AutoResetEvent are = new AutoResetEvent(false))
            {
                System.Windows.Deployment.Current.Dispatcher.BeginInvoke(() =>
                {
                    textInputInstance = new TextBox();
                    textInputInstance.TextChanged += textChangedEvent;
                    cl.Children.Add(textInputInstance);
                    Canvas.SetZIndex(textInputInstance, 50000);
                    textInputInstance.Text = toCSharp(n4);
                    textInputInstance.IsEnabled = true;
                    com.codename1.ui.Font fnt = (com.codename1.ui.Font)((com.codename1.ui.plaf.Style)currentlyEditing.getStyle()).getFont();
                    NativeFont font = f((java.lang.Object)fnt.getNativeFont());

                    // workaround forsome weird unspecified margin that appears around the text box
                    Canvas.SetTop(textInputInstance, currentlyEditing.getAbsoluteY() - 10);
                    Canvas.SetLeft(textInputInstance, currentlyEditing.getAbsoluteX() - 10);
                    textInputInstance.Height = currentlyEditing.getHeight() + 20;
                    textInputInstance.Width = currentlyEditing.getWidth() + 20;
                    textInputInstance.BorderThickness = new Thickness(0);
                    textInputInstance.FontSize = font.height;
                    textInputInstance.Margin = new Thickness(0);
                    textInputInstance.Padding = new Thickness(0);
                    textInputInstance.Clip = null;
                    textInputInstance.AcceptsReturn = !currentlyEditing.isSingleLineTextArea();
                    textInputInstance.Focus();
                    are.Set();
                });
                are.WaitOne();
            }
            d.invokeAndBlock(new WaitForEdit());
            using (AutoResetEvent are = new AutoResetEvent(false))
            {
                System.Windows.Deployment.Current.Dispatcher.BeginInvoke(() =>
                {
                    cl.Children.Remove(textInputInstance);
                    textInputInstance = null;
                    //cl.Focus();
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

        public override void flushGraphics(int n1, int n2, int n3, int n4)
        {
            /*if (n1 == 0 && n2 == 0 && n3 == getDisplayWidth() && n4 == getDisplayHeight())
            {
                flushGraphics();
                return;
            }
            System.Collections.Generic.List<OperationPending> currentPaints = new System.Collections.Generic.List<OperationPending>();
            currentPaints.AddRange(globalGraphics.pendingPaints);
            globalGraphics.pendingPaints.Clear();
            paintCurrent(currentPaints, n1, n2, n3, n4);*/
            flushGraphics();
        }

        public override void flushGraphics()
        {
            System.Collections.Generic.List<OperationPending> currentPaints = new System.Collections.Generic.List<OperationPending>();
            currentPaints.AddRange(globalGraphics.pendingPaintsList);
            globalGraphics.pendingPaintsList.Clear();
            paintCurrent(currentPaints);
        }

        private static java.lang.String componentKey = toJava("$$wpt");
        public WipeComponent pendingWipe;
        public override void beforeComponentPaint(global::com.codename1.ui.Component n1, global::com.codename1.ui.Graphics n2)
        {
            if (n2.getGraphics() != globalGraphics)
            {
                return;
            }
            currentlyPainting = n1;
            CSharpListHolder cl = (CSharpListHolder)n1.getClientProperty(componentKey);
            if (cl != null)
            {
                if (cl.ll.Count > 0)
                {
                    //System.Diagnostics.Debug.WriteLine("Painting component with " + cl.ll.Count + " elements");
                    //System.Diagnostics.Debug.WriteLine(toCSharp((java.lang.String)n1.toString()));
                    pendingWipe = new WipeComponent(globalGraphics, cl.ll, toCSharp((java.lang.String)n1.toString()));
                    cl.ll.Clear();
                }
            }
            else
            {
                //System.Diagnostics.Debug.WriteLine("Painting component with no previous elements");
                //System.Diagnostics.Debug.WriteLine(toCSharp((java.lang.String)n1.toString()));
                cl = new CSharpListHolder();
                currentlyPainting.putClientProperty(componentKey, cl);
            }
            currentPaintDestination = cl.ll;
            /*if (n1 is global::com.codename1.ui.Form)
            {
                pendingPaints.Clear();
                pendingPaints.Add(new WipeScreen(globalGraphics));
            }*/
        }

        public override void componentRemoved(global::com.codename1.ui.Component c)
        {
            beforeComponentPaint(c, (global::com.codename1.ui.Graphics)getCodenameOneGraphics());
            afterComponentPaint(c, (global::com.codename1.ui.Graphics)getCodenameOneGraphics());
        }

        
        public override void afterComponentPaint(global::com.codename1.ui.Component n1, global::com.codename1.ui.Graphics n2)
        {
            if (n2.getGraphics() != globalGraphics)
            {
                return;
            }
            if (pendingWipe != null)
            {
                globalGraphics.paint(pendingWipe);
                pendingWipe = null;
            }
            currentlyPainting = null;
            currentPaintDestination = null;
            /*if (n1 is global::com.codename1.ui.Form)
            {
                pendingPaints.Clear();
                pendingPaints.Add(new WipeScreen(globalGraphics));
            }*/
        }

        public override void systemOut(global::java.lang.String n1)
        {
            System.Diagnostics.Debug.WriteLine(toCSharp(n1));
        }

        private void wipe()
        {
            globalGraphics.pendingPaintsList.Clear();
            globalGraphics.pendingPaintsList.Add(new WipeScreen(globalGraphics));
        }

        public override void repaint(ui.animations.Animation n1)
        {
            if(n1 is global::com.codename1.ui.Form) {
                wipe();
            }
            base.repaint(n1);
        }

        private void paintCurrent(System.Collections.Generic.List<OperationPending> currentPaints)
        {
            using (AutoResetEvent are = new AutoResetEvent(false))
            {
                System.Windows.Deployment.Current.Dispatcher.BeginInvoke(() =>
                {
                    int count = currentPaints.Count;
                    //System.Diagnostics.Debug.WriteLine("Drawing " + count + " elements on " + cl.Children.Count + " that are on the screen");
                    for (int iter = 0; iter < count ; iter++)
                    {
                        //currentPaints[iter].printLogging();
                        currentPaints[iter].perform(cl);
                    }
                    are.Set();
                });
                are.WaitOne();
            }
        }

        private void paintCurrent(System.Collections.Generic.List<OperationPending> currentPaints, int x, int y, int h, int w)
        {
            using (AutoResetEvent are = new AutoResetEvent(false))
            {
                System.Windows.Deployment.Current.Dispatcher.BeginInvoke(() =>
                {
                    Microsoft.Xna.Framework.Rectangle a = new Microsoft.Xna.Framework.Rectangle(x, y, w, h);
                    int count = currentPaints.Count;
                    //System.Diagnostics.Debug.WriteLine("Drawing " + count + " elements on " + cl.Children.Count + " that are on the screen");
                    for (int iter = 0; iter < count; iter++)
                    {
                        //currentPaints[iter].printLogging();
                        if (!currentPaints[iter].clipSet)
                        {
                            currentPaints[iter].clipSet = true;
                            currentPaints[iter].clipX = x;
                            currentPaints[iter].clipY = y;
                            currentPaints[iter].clipW = w;
                            currentPaints[iter].clipH = h;
                        }
                        else
                        {
                            Microsoft.Xna.Framework.Rectangle b = new Microsoft.Xna.Framework.Rectangle(currentPaints[iter].clipX, currentPaints[iter].clipY, currentPaints[iter].clipW, currentPaints[iter].clipH);
                            Microsoft.Xna.Framework.Rectangle r = Microsoft.Xna.Framework.Rectangle.Intersect(a, b);
                            currentPaints[iter].clipX = r.Left;
                            currentPaints[iter].clipY = r.Top;
                            currentPaints[iter].clipW = r.Width;
                            currentPaints[iter].clipH = r.Height;
                        }
                        currentPaints[iter].perform(cl);
                    }
                });
                are.WaitOne();
            }
        }

        public override void getRGB(java.lang.Object n1, _nArrayAdapter<int> n2, int n3, int n4, int n5, int n6, int n7)
        {
            CodenameOneImage cn = (CodenameOneImage)n1;
            if(cn.img != null) {
                using (AutoResetEvent are = new AutoResetEvent(false))
                {
                    System.Windows.Deployment.Current.Dispatcher.BeginInvoke(() =>
                    {
                        WriteableBitmap wb = new WriteableBitmap((BitmapSource)cn.img);
                        int[] p = wb.Pixels;
                        for (int iter = 0; iter < p.Length; iter++)
                        {
                            n2.getCSharpArray()[iter] = p[iter];
                        }
                        are.Set();
                    });
                    are.WaitOne();
                }
                return;
            }
            throw new global::org.xmlvm._nExceptionAdapter(new global::java.lang.UnsupportedOperationException());
        }

        public override void setImageName(global::java.lang.Object n1, global::java.lang.String n2)
        {
            if (n2 != null)
            {
                ((CodenameOneImage)n1).name = toCSharp(n2);
            }
        }

        public override object createImage(_nArrayAdapter<int> n1, int n2, int n3)
        {
            CodenameOneImage ci = new CodenameOneImage();
            using (AutoResetEvent are = new AutoResetEvent(false))
            {
                System.Windows.Deployment.Current.Dispatcher.BeginInvoke(() =>
                {
                    ci.@this();
                    WriteableBitmap wb = new WriteableBitmap(n2, n3);
                    int[] p = wb.Pixels;
                    for (int iter = 0; iter < p.Length; iter++)
                    {
                        p[iter] = n1.getCSharpArray()[iter];
                    }
                    ci.img = wb;
                    ci.width = n2;
                    ci.height = n3;
                    are.Set();
                });
                are.WaitOne();
            }
            return ci;
        }

        public override object createImage(java.lang.String n1)
        {
            if (n1.startsWith(toJava("file:")))
            {
                createImage((java.io.InputStream)openFileInputStream(n1));
            }
            return createImage((global::java.io.InputStream)getResourceAsStream(null, n1));
        }

        public override object createImage(java.io.InputStream n1)
        {
            global::org.xmlvm._nArrayAdapter<sbyte> b = (global::org.xmlvm._nArrayAdapter<sbyte>)com.codename1.io.Util.readInputStream(n1);
            return createImage(b, 0, b.Length);
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
            CodenameOneImage ci = null;
            using (AutoResetEvent are = new AutoResetEvent(false))
            {
                System.Windows.Deployment.Current.Dispatcher.BeginInvoke(() =>
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
                    BitmapImage bitmapImage = new BitmapImage();
                    CodenameOneImage cim = new CodenameOneImage();
                    cim.@this();
                    bitmapImage.SetSource(s);
                    cim.img = bitmapImage;
                    cim.width = (int)bitmapImage.PixelWidth;
                    cim.height = (int)bitmapImage.PixelHeight;
                    ci = cim;
                    are.Set();
                });
                are.WaitOne();
            }
            return ci;
        }

        /*public override global::System.Object createNativePeer(global::java.lang.Object n1)
        {
        }*/

        public static bool exitLock;
        private global::com.codename1.ui.events.ActionListener pendingCaptureCallback;

        public override void capturePhoto(global::com.codename1.ui.events.ActionListener n1)
        {
            exitLock = true;
            pendingCaptureCallback = n1;
            System.Windows.Deployment.Current.Dispatcher.BeginInvoke(() =>
            {
                CameraCaptureTask t = new CameraCaptureTask();
                t.Completed += t_Completed;
                t.Show();
            });
        }

        private void fireCapture(com.codename1.ui.events.ActionEvent ev)
        {
            com.codename1.ui.util.EventDispatcher ed = new com.codename1.ui.util.EventDispatcher();
            ed.@this();
            ed.addListener((java.lang.Object)pendingCaptureCallback);
            ed.fireActionEvent(ev);
            pendingCaptureCallback = null;
            exitLock = false;
        }

        void t_Completed(object sender, PhotoResult e)
        {
            if (e.OriginalFileName != null)
            {
                IsolatedStorageFile store = IsolatedStorageFile.GetUserStoreForApplication();
                string file = e.OriginalFileName.Substring(e.OriginalFileName.LastIndexOf('\\') + 1);
                e.ChosenPhoto.CopyTo(store.OpenFile(file, FileMode.OpenOrCreate));
                com.codename1.ui.events.ActionEvent ac = new com.codename1.ui.events.ActionEvent();
                ac.@this(toJava("file:/" + file));
                fireCapture(ac);
            }
            else
            {
                fireCapture(null);
            }
        }

        public override bool isNativeBrowserComponentSupported()
        {
            return true;
        }

        com.codename1.ui.BrowserComponent currentBrowser;
        public override global::System.Object createBrowserComponent(global::java.lang.Object n1)
        {
            SilverlightPeer sp =null;
            using (AutoResetEvent are = new AutoResetEvent(false))
            {
                System.Windows.Deployment.Current.Dispatcher.BeginInvoke(() =>
                {
                    Microsoft.Phone.Controls.WebBrowser wb = new WebBrowser();
                    currentBrowser = (com.codename1.ui.BrowserComponent)n1;
                    wb.Navigating += wb_Navigating;
                    wb.Navigated += wb_Navigated;
                    wb.NavigationFailed += wb_NavigationFailed;
                    sp = new SilverlightPeer(wb);
                    are.Set();
                });
                are.WaitOne();
            }
            return sp;
        }

        void wb_NavigationFailed(object sender, System.Windows.Navigation.NavigationFailedEventArgs e)
        {
            com.codename1.ui.events.BrowserNavigationCallback bn = (com.codename1.ui.events.BrowserNavigationCallback)currentBrowser.getBrowserNavigationCallback();
            com.codename1.ui.events.ActionEvent ev = new com.codename1.ui.events.ActionEvent();
            ev.@this(toJava(e.Uri.OriginalString));
            currentBrowser.fireWebEvent(toJava("onError"), ev);
        }

        void wb_Navigated(object sender, System.Windows.Navigation.NavigationEventArgs e)
        {
            com.codename1.ui.events.BrowserNavigationCallback bn = (com.codename1.ui.events.BrowserNavigationCallback)currentBrowser.getBrowserNavigationCallback();
            com.codename1.ui.events.ActionEvent ev = new com.codename1.ui.events.ActionEvent();
            ev.@this(toJava(e.Uri.OriginalString));
            currentBrowser.fireWebEvent(toJava("onLoad"), ev);
        }

        void wb_Navigating(object sender, NavigatingEventArgs e)
        {
            com.codename1.ui.events.BrowserNavigationCallback bn = (com.codename1.ui.events.BrowserNavigationCallback)currentBrowser.getBrowserNavigationCallback();
            if (!bn.shouldNavigate(toJava(e.Uri.OriginalString)))
            {
                e.Cancel = true;
            }
            com.codename1.ui.events.ActionEvent ev = new com.codename1.ui.events.ActionEvent();
            ev.@this(toJava(e.Uri.OriginalString));
            currentBrowser.fireWebEvent(toJava("onStart"), ev);
        }

        public override global::System.Object getBrowserTitle(global::com.codename1.ui.PeerComponent n1)
        {
            WebBrowser s = (WebBrowser)((SilverlightPeer)n1).element;
            return toJava((string)s.InvokeScript("eval", "document.title.toString()"));
        }

        public override global::System.Object getBrowserURL(global::com.codename1.ui.PeerComponent n1)
        {
            WebBrowser s = (WebBrowser)((SilverlightPeer)n1).element;
            return toJava(s.Source.OriginalString);
        }

        public override void setBrowserURL(global::com.codename1.ui.PeerComponent n1, global::java.lang.String n2)
        {
            System.Windows.Deployment.Current.Dispatcher.BeginInvoke(() =>
            {
                WebBrowser s = (WebBrowser)((SilverlightPeer)n1).element;
                string uri = toCSharp(n2);
                if (uri.StartsWith("jar:/"))
                {
                    uri = uri.Substring(5);
                    while (uri[0] == '/')
                    {
                        uri = uri.Substring(1);
                    }
                    uri = "res/" + uri;
                    s.Source = new Uri(uri, UriKind.Relative);
                    return;
                }
                s.Source = new Uri(uri);
            });
        }

        public override void browserReload(global::com.codename1.ui.PeerComponent n1)
        {
            System.Windows.Deployment.Current.Dispatcher.BeginInvoke(() =>
            {
                WebBrowser s = (WebBrowser)((SilverlightPeer)n1).element;
                s.Source = s.Source;
            });
        }

        public override bool browserHasBack(global::com.codename1.ui.PeerComponent n1)
        {
            WebBrowser s = (WebBrowser)((SilverlightPeer)n1).element;
            return s.CanGoBack;
        }

        public override bool browserHasForward(global::com.codename1.ui.PeerComponent n1)
        {
            WebBrowser s = (WebBrowser)((SilverlightPeer)n1).element;
            return s.CanGoForward;
        }

        public override void browserBack(global::com.codename1.ui.PeerComponent n1)
        {
            WebBrowser s = (WebBrowser)((SilverlightPeer)n1).element;
            s.GoBack();
        }

        public override void browserStop(global::com.codename1.ui.PeerComponent n1)
        {
        }

        public override void browserDestroy(global::com.codename1.ui.PeerComponent n1)
        {
        }

        public override void browserForward(global::com.codename1.ui.PeerComponent n1)
        {
            WebBrowser s = (WebBrowser)((SilverlightPeer)n1).element;
            s.GoForward();
        }

        public override void browserClearHistory(global::com.codename1.ui.PeerComponent n1)
        {
            System.Windows.Deployment.Current.Dispatcher.BeginInvoke(() =>
            {
                WebBrowser s = (WebBrowser)((SilverlightPeer)n1).element;
                s.ClearCookiesAsync();
                s.ClearInternetCacheAsync();
            });
        }

        public override void setBrowserPage(global::com.codename1.ui.PeerComponent n1, global::java.lang.String n2, global::java.lang.String n3)
        {
            System.Windows.Deployment.Current.Dispatcher.BeginInvoke(() =>
            {
                WebBrowser s = (WebBrowser)((SilverlightPeer)n1).element;
                s.NavigateToString(toCSharp(n2));
            });
        }

        public virtual void execute(global::java.lang.String n1)
        {
            System.Windows.Deployment.Current.Dispatcher.BeginInvoke(() =>
            {
                Microsoft.Phone.Tasks.WebBrowserTask t = new Microsoft.Phone.Tasks.WebBrowserTask();
                t.Uri = new Uri(toCSharp(n1), UriKind.RelativeOrAbsolute);
                t.Show();
            });
        }

        public override void browserExecute(global::com.codename1.ui.PeerComponent n1, global::java.lang.String n2)
        {
            System.Windows.Deployment.Current.Dispatcher.BeginInvoke(() =>
            {
                WebBrowser s = (WebBrowser)((SilverlightPeer)n1).element;
                s.InvokeScript(toCSharp(n2));
            });
        }

        public override global::System.Object browserExecuteAndReturnString(global::com.codename1.ui.PeerComponent n1, global::java.lang.String n2)
        {
            string st = null;
            using (AutoResetEvent are = new AutoResetEvent(false))
            {
                System.Windows.Deployment.Current.Dispatcher.BeginInvoke(() =>
                {
                    WebBrowser s = (WebBrowser)((SilverlightPeer)n1).element;
                    st = (string)s.InvokeScript(toCSharp(n2));
                    are.Set();
                });
                are.WaitOne();
            }
            return toJava(st);
        }

        public override object createMutableImage(int n1, int n2, int n3)
        {
            CodenameOneMutableImage ci = new CodenameOneMutableImage();
            ci.@this();
            ci.width = n1;
            ci.height = n2;
            ci.opaque =((n3 & 0xff000000) == 0xff000000);
            if((n3 & 0xff000000) != 0) {
                ci.graphics.color = n3;
                ci.imagePaints.Add(new FillRect(ci.graphics, pendingWipe, 0, 0, n1, n2));
            }
            return ci;
        }

        public override int getImageWidth(java.lang.Object n1)
        {
            return ((CodenameOneImage)n1).getImageWidth();
        }

        public override int getImageHeight(java.lang.Object n1)
        {
            return ((CodenameOneImage)n1).getImageHeight();
        }

        public override object scale(java.lang.Object n1, int n2, int n3)
        {
            CodenameOneImage ci = new CodenameOneImage();
            ci.@this();
            CodenameOneImage source = (CodenameOneImage)n1;
            ci.img = source.img;
            ci.width = n2;
            ci.height = n3;
            return ci;
        }

        public override int getSoftkeyCount()
        {
            return 0;
        }

        public override object getSoftkeyCode(int n1)
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
            return -20;
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

        public override int getColor(java.lang.Object n1)
        {
            return ((NativeGraphics)n1).color;
        }

        public override void setColor(java.lang.Object n1, int n2)
        {
            ((NativeGraphics)n1).color = n2;
        }

        public override void setAlpha(java.lang.Object n1, int n2)
        {
            ((NativeGraphics)n1).alpha = n2;
        }

        public override int getAlpha(java.lang.Object n1)
        {
            return ((NativeGraphics)n1).alpha;
        }

        public override void setNativeFont(java.lang.Object n1, java.lang.Object n2)
        {
            ((NativeGraphics)n1).font = (NativeFont)n2;
        }

        public override int getClipX(java.lang.Object n1)
        {
            return ((NativeGraphics)n1).clipX;
        }

        public override int getClipY(java.lang.Object n1)
        {
            return ((NativeGraphics)n1).clipY;
        }

        public override int getClipWidth(java.lang.Object n1)
        {
            return ((NativeGraphics)n1).clipW;
        }

        public override int getClipHeight(java.lang.Object n1)
        {
            return ((NativeGraphics)n1).clipH;
        }

        public override void setClip(java.lang.Object n1, int n2, int n3, int n4, int n5)
        {
            NativeGraphics ng = (NativeGraphics)n1;
            ng.clipSet = true;
            ng.clipX = n2;
            ng.clipY = n3;
            ng.clipW = n4;
            ng.clipH = n5;
        }

        public override void clipRect(java.lang.Object n1, int n2, int n3, int n4, int n5)
        {
            NativeGraphics ng = (NativeGraphics)n1;
            ng.clipSet = true;
            Microsoft.Xna.Framework.Rectangle a = new Microsoft.Xna.Framework.Rectangle(ng.clipX, ng.clipY, ng.clipW, ng.clipH);
            Microsoft.Xna.Framework.Rectangle b = new Microsoft.Xna.Framework.Rectangle(n2, n3, n4, n5);
            Microsoft.Xna.Framework.Rectangle r = Microsoft.Xna.Framework.Rectangle.Intersect(a, b);
            setClip(n1, r.X, r.Y, r.Width, r.Height);
        }

        public override void drawLine(java.lang.Object n1, int x1, int y1, int x2, int y2)
        {
            NativeGraphics ng = (NativeGraphics)n1;
            ng.paint(new DrawLine(ng, pendingWipe, x1, y1, x2, y2));
        }

        public override void fillRect(java.lang.Object n1, int x, int y, int w, int h)
        {
            NativeGraphics ng = (NativeGraphics)n1;
            if (ng.alpha > 0)
            {
                // wipe the screen if the operation will clear the whole screen
                int dw = getDisplayWidth();
                int dh = getDisplayHeight();
                if (ng == globalGraphics && x == 0 && y == 0 && w == dw && h == dh && ng.clipX == 0 && ng.clipY == 0 && ng.clipW == w &&
                    ng.clipH == h && ng.alpha == 255)
                {
                    wipe();
                }

                ng.paint(new FillRect(ng, pendingWipe, x, y, w, h));
            }
        }

        public override void drawRect(java.lang.Object n1, int x, int y, int w, int h)
        {
            NativeGraphics ng = (NativeGraphics)n1;
            ng.paint(new DrawRect(ng, pendingWipe, x, y, w, h, 1));
        }

        public override void drawRect(java.lang.Object n1, int x, int y, int w, int h, int t)
        {
            NativeGraphics ng = (NativeGraphics)n1;
            ng.paint(new DrawRect(ng, pendingWipe, x, y, w, h, t));
        }

        public override void drawRoundRect(java.lang.Object n1, int x, int y, int w, int h, int arcW, int arcH)
        {
            NativeGraphics ng = (NativeGraphics)n1;
            ng.paint(new DrawRoundRect(ng, pendingWipe, x, y, w, h, arcW, arcH, false));
        }

        public override void fillRoundRect(java.lang.Object n1, int x, int y, int w, int h, int arcW, int arcH)
        {
            NativeGraphics ng = (NativeGraphics)n1;
            ng.paint(new DrawRoundRect(ng, pendingWipe, x, y, w, h, arcW, arcH, true));
        }

        public override void fillArc(java.lang.Object n1, int n2, int n3, int n4, int n5, int n6, int n7)
        {
        }

        public override void drawArc(java.lang.Object n1, int n2, int n3, int n4, int n5, int n6, int n7)
        {
        }

        public override void drawString(java.lang.Object n1, java.lang.String n2, int n3, int n4)
        {
            NativeGraphics ng = (NativeGraphics)n1;
            //ng.pendingPaints.Add(new FillRect(ng, n3, n4, 50, 30));
            ng.paint(new DrawString(ng, pendingWipe, f(ng.font), toCSharp(n2), n3, n4));
        }

        public override void drawImage(java.lang.Object n1, java.lang.Object n2, int x, int y)
        {
            CodenameOneImage img = (CodenameOneImage)n2;

            NativeGraphics ng = (NativeGraphics)n1;

            // wipe the screen if the operation will clear the whole screen
            int dw = getDisplayWidth();
            int dh = getDisplayHeight();
            if (ng == globalGraphics && x == 0 && y == 0 && img.width == dw && img.height == dh && ng.clipX == 0 && ng.clipY == 0 && ng.clipW == img.width &&
                ng.clipH == img.height && ng.alpha == 255)
            {
                wipe();
            }

            if (n2 is CodenameOneMutableImage)
            {
                int cx = getClipX(ng);
                int cy = getClipY(ng);
                int cw = getClipWidth(ng);
                int ch = getClipHeight(ng);
                clipRect(ng, cx, cy, img.width, img.height);
                foreach (OperationPending p in ((CodenameOneMutableImage)n2).imagePaints)
                {
                    ng.paint(p.clone(pendingWipe, x, y));
                }
                setClip(ng, cx, cy, cw, ch);
            }
            else
            {
                ng.paint(new DrawImage(ng, pendingWipe, n2, x, y));
            }
        }

        public override bool areMutableImagesFast()
        {
            return false;
        }

        public override bool shouldPaintBackground()
        {
            return false;
        }

        public override void drawImage(java.lang.Object n1, java.lang.Object n2, int x, int y, int w, int h)
        {
            NativeGraphics ng = (NativeGraphics)n1;

            // wipe the screen if the operation will clear the whole screen
            int dw = getDisplayWidth();
            int dh = getDisplayHeight();
            if (ng == globalGraphics && x == 0 && y == 0 && w == dw && h == dh && ng.clipX == 0 && ng.clipY == 0 && ng.clipW == w &&
                ng.clipH == h && ng.alpha == 255)
            {
                wipe();
            }
            ng.paint(new DrawImage(ng, pendingWipe, n2, x, y, w, h));
        }

        public override void tileImage(global::java.lang.Object n1, global::java.lang.Object n2, int x, int y, int w, int h)
        {
            NativeGraphics ng = (NativeGraphics)n1;

            // wipe the screen if the operation will clear the whole screen
            int dw = getDisplayWidth();
            int dh = getDisplayHeight();
            if (ng == globalGraphics && x == 0 && y == 0 && w == dw && h == dh && ng.clipX == 0 && ng.clipY == 0 && ng.clipW == w &&
                ng.clipH == h && ng.alpha == 255)
            {
                wipe();
            }
            base.tileImage(n1, n2, x, y, w, h);
        }

        public override void drawRGB(java.lang.Object n1, _nArrayAdapter<int> rgb, int offset, int x, int y, int w, int h, bool n8)
        {
            CodenameOneImage ci = new CodenameOneImage();
            ci.@this();
            WriteableBitmap wb = new WriteableBitmap(w, h);
            int[] p = wb.Pixels;
            for (int iter = 0; iter < p.Length; iter++)
            {
                p[iter] = rgb.getCSharpArray()[iter];
            }
            ci.img = wb;
            ci.width = w;
            ci.height = h;

            NativeGraphics ng = (NativeGraphics)n1;
            ng.paint(new DrawImage(ng, pendingWipe, ci, x, y));
        }

        public override object getNativeGraphics()
        {
            return globalGraphics;
        }

        public override object getNativeGraphics(java.lang.Object n1)
        {
            ((CodenameOneMutableImage)n1).graphics.clipSet = false;
            ((CodenameOneMutableImage)n1).graphics.clipW = ((CodenameOneMutableImage)n1).width;
            ((CodenameOneMutableImage)n1).graphics.clipH = ((CodenameOneMutableImage)n1).height;
            return ((CodenameOneMutableImage)n1).graphics;
        }

        public override int charsWidth(java.lang.Object n1, _nArrayAdapter<char> n2, int n3, int n4)
        {
            global::java.lang.String s = new global::java.lang.String();
            s.@this(n2, n3, n4);
            return stringWidth(n1, s);
        }

        private Dictionary<StringFontPair, Int32> stringWidthCache = new Dictionary<StringFontPair,Int32>();
        public override int stringWidth(java.lang.Object n1, java.lang.String n2)
        {
            NativeFont font = f(n1);
            string str = toCSharp(n2);
            StringFontPair sfp = new StringFontPair(str, font);
            if (stringWidthCache.ContainsKey(sfp))
            {
                return stringWidthCache[sfp];
            }
            int result = 0;
            using (AutoResetEvent are = new AutoResetEvent(false))
            {
                System.Windows.Deployment.Current.Dispatcher.BeginInvoke(() =>
                {
                    TextBlock tb = new TextBlock();
                    tb.FontSize = font.height;
                    tb.Text = str;
                    tb.Measure(new Size(1000000, 1000000));
                    result = (int)tb.ActualWidth;
                    are.Set();
                });
                are.WaitOne();
            }
            stringWidthCache.Add(sfp, result);
            return result;
        }

        public override int charWidth(java.lang.Object n1, char n2)
        {
            return stringWidth(n1, toJava("" + n2));
        }

        public override int getFace(global::java.lang.Object n1)
        {
            return f(n1).systemFace;
        }

        public override int getSize(global::java.lang.Object n1)
        {
            return f(n1).systemSize;
        }

        public override int getStyle(global::java.lang.Object n1)
        {
            return f(n1).systemStyle;
        }

        public override int getHeight(java.lang.Object n1)
        {
            return f(n1).actualHeight;
        }

        public override object getDefaultFont()
        {
            if (defaultFont == null)
            {
                defaultFont = (NativeFont)createFont(com.codename1.ui.Font._fFACE_1SYSTEM, com.codename1.ui.Font._fSTYLE_1PLAIN, com.codename1.ui.Font._fSIZE_1MEDIUM);
            }
            return defaultFont;
        }


        private Dictionary<int, object> fontCache = new Dictionary<int, object>();
        public override object createFont(int face, int style, int size)
        {
            if (fontCache.ContainsKey(face | style | size))
            {
                return fontCache[face | style | size];
            }

            int a = 24;
            switch (size)
            {
                case 8: //com.codename1.ui.Font._fSIZE_1SMALL:
                    a = 15;
                    break;
                case 16: //com.codename1.ui.Font._fSIZE_1LARGE:
                    a = 56;
                    break;
            }
            
            NativeFont nf = new NativeFont();
            nf.height = a;
            nf.systemFace = face;
            nf.systemSize = size;
            nf.systemStyle = style;
            using (AutoResetEvent are = new AutoResetEvent(false))
            {
                System.Windows.Deployment.Current.Dispatcher.BeginInvoke(() =>
                {
                    TextBlock tb = new TextBlock();
                    tb.FontSize = nf.height;
                    tb.Text = "Xp";
                    tb.Measure(new Size(1000000, 1000000));
                    nf.actualHeight = (int)tb.ActualHeight;
                    are.Set();
                });
                are.WaitOne();
            }

            if ((style & com.codename1.ui.Font._fSTYLE_1BOLD) == com.codename1.ui.Font._fSTYLE_1BOLD)
            {
                nf.bold = true;
            }
            if ((style & com.codename1.ui.Font._fSTYLE_1ITALIC) == com.codename1.ui.Font._fSTYLE_1ITALIC)
            {
                nf.italic = true;
            }

            fontCache[face | style | size] = nf;
            return nf;
        }

        private NativeFont f(java.lang.Object fnt)
        {
            if (fnt == null) return (NativeFont)getDefaultFont();
            return (NativeFont)fnt;
        }

        public override bool isScaledImageDrawingSupported()
        {
            return true;
        }

        public override bool isOpaque(global::com.codename1.ui.Image n1, global::java.lang.Object n2)
        {
            CodenameOneImage c = (CodenameOneImage)n2;
            if (c is CodenameOneMutableImage)
            {
                return false;
            }
            // TODO!
            return true;
        }

        public override global::System.Object createSoftWeakRef(global::java.lang.Object n1)
        {
            return new SoftRef(n1);
        }

        public override global::System.Object extractHardRef(global::java.lang.Object n1)
        {
            if (n1 != null)
            {
                return ((SoftRef)n1).get();
            }
            return null;
        }

        public override global::System.Object connect(global::java.lang.String n1, bool read, bool write)
        {
            NetworkOperation n = new NetworkOperation();
            string s = toCSharp(n1);
            n.request = (HttpWebRequest)WebRequest.Create(new Uri(s));
            n.request.AllowAutoRedirect = false;
            return n;
        }

        public override void setHeader(global::java.lang.Object n1, global::java.lang.String n2, global::java.lang.String n3)
        {
            NetworkOperation n = (NetworkOperation)n1;
            string key = toCSharp(n2);
            string value = toCSharp(n3);
            if (key.ToLower().Equals("accept"))
            {
                n.request.Accept = value;
                return;
            }
            if (key.ToLower().Equals("connection") || key.ToLower().Equals("keepalive") ||
                key.ToLower().Equals("expect") || key.ToLower().Equals("date") || key.ToLower().Equals("host") ||
                key.ToLower().Equals("if-modified-since") || key.ToLower().Equals("range") ||
                key.ToLower().Equals("referer") || key.ToLower().Equals("transfer-encoding") || 
                key.ToLower().Equals("user-agent"))
            {
                return;
            }
            if (key.ToLower().Equals("content-length"))
            {
                n.request.ContentLength = Int32.Parse(value);
                return;
            }
            if (key.ToLower().Equals("content-type"))
            {
                if (n.request.Method.ToLower().Equals("get"))
                {
                    // if content type is set on a get request silverlight throws an exception, correct but a
                    // common bug!
                    return;
                }
                n.request.ContentType = value;
                return;
            }
            n.request.Headers[key] = value;
        }

        public override int getContentLength(global::java.lang.Object n1)
        {
            NetworkOperation n = (NetworkOperation)n1;
            return (int)n.response.ContentLength;
        }

        public override global::System.Object openOutputStream(global::java.lang.Object n1)
        {
            NetworkOperation n = (NetworkOperation)n1;
            com.codename1.io.BufferedOutputStream bo = new com.codename1.io.BufferedOutputStream();
            bo.@this(new OutputStreamProxy(n.requestStream));
            return bo;
        }

        public override global::System.Object openOutputStream(global::java.lang.Object n1, int n2)
        {
            if (n1 is java.lang.String)
            {
                IsolatedStorageFile store = IsolatedStorageFile.GetUserStoreForApplication();
                IsolatedStorageFileStream s = store.OpenFile(nativePath((java.lang.String)n1), FileMode.OpenOrCreate);
                return new OutputStreamProxy(s);
            }
            return null;
        }

        public override global::System.Object openInputStream(global::java.lang.Object n1)
        {
            if (n1 is java.lang.String)
            {
                IsolatedStorageFile store = IsolatedStorageFile.GetUserStoreForApplication();
                string file = nativePath((java.lang.String)n1);
                IsolatedStorageFileStream s = store.OpenFile(file, FileMode.Open);
                return new InputStreamProxy(s);
            }
            NetworkOperation n = (NetworkOperation)n1;
            com.codename1.io.BufferedInputStream bo = new com.codename1.io.BufferedInputStream();
            bo.@this(new InputStreamProxy(n.response.GetResponseStream()));
            return bo;
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
            int i = 0;
            HttpWebResponse res = n.response;
            using (AutoResetEvent are = new AutoResetEvent(false))
            {
                System.Windows.Deployment.Current.Dispatcher.BeginInvoke(() =>
                {
                    i = (int)res.StatusCode;
                    are.Set();
                });
                are.WaitOne();
            }
            return i;
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
            return new _nArrayAdapter<global::System.Object>(new java.lang.String[] { toJava(s) });
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

        private object convertArray(string[] arr)
        {
            java.lang.String[] resp = new java.lang.String[arr.Length];
            for (int iter = 0; iter < resp.Length; iter++)
            {
                resp[iter] = toJava(arr[iter]);
            }
            return new _nArrayAdapter<java.lang.Object>(resp);
        }

        public override global::System.Object listStorageEntries()
        {
            IsolatedStorageFile store = IsolatedStorageFile.GetUserStoreForApplication();
            string[] arr = store.GetFileNames();
            return convertArray(store.GetFileNames());
        }

        public override object listFilesystemRoots()
        {
            return new _nArrayAdapter<java.lang.Object>(new java.lang.String[] {toJava("file:/")});
        }

        private string nativePath(java.lang.String s)
        {
            string ss = toCSharp(s);
            if (ss.StartsWith("file:/"))
            {
                ss = ss.Substring(6).Replace('/', '\\');
            }
            return ss;
        }

        private string[] prependFile(string[] arr)
        {
            for (int iter = 0; iter < arr.Length; iter++)
            {
                if (!arr[iter].StartsWith("file:/"))
                {
                    arr[iter] = "file:/" + arr[iter];
                }
            }
            return arr;
        }

        public override object listFiles(java.lang.String n1)
        {
            IsolatedStorageFile store = IsolatedStorageFile.GetUserStoreForApplication();
            return convertArray(prependFile(store.GetFileNames(nativePath(n1) + "\\*.*")));
        }

        public override long getRootSizeBytes(java.lang.String n1)
        {
            return 0;
        }

        public override long getRootAvailableSpace(java.lang.String n1)
        {
            IsolatedStorageFile store = IsolatedStorageFile.GetUserStoreForApplication();
            return store.Quota;
        }

        public override void mkdir(java.lang.String n1)
        {
            IsolatedStorageFile store = IsolatedStorageFile.GetUserStoreForApplication();
            store.CreateDirectory(nativePath(n1));
        }

        public override void deleteFile(java.lang.String n1)
        {
            IsolatedStorageFile store = IsolatedStorageFile.GetUserStoreForApplication();
            store.DeleteFile(nativePath(n1));
        }

        public override bool isHidden(java.lang.String n1)
        {
            return false;
        }

        public override void setHidden(java.lang.String n1, bool n2)
        {
        }

        public override long getFileLength(java.lang.String n1)
        {
            IsolatedStorageFile store = IsolatedStorageFile.GetUserStoreForApplication();
            IsolatedStorageFileStream f = store.OpenFile(nativePath(n1), FileMode.Open);
            long l = f.Length;
            f.Close();
            return l;
        }

        public override bool isDirectory(java.lang.String n1)
        {
            IsolatedStorageFile store = IsolatedStorageFile.GetUserStoreForApplication();
            return store.DirectoryExists(nativePath(n1));
        }

        public override bool exists(java.lang.String n1)
        {
            IsolatedStorageFile store = IsolatedStorageFile.GetUserStoreForApplication();
            return store.FileExists(nativePath(n1)) || store.DirectoryExists(nativePath(n1));
        }

        public override void rename(java.lang.String n1, java.lang.String n2)
        {
            IsolatedStorageFile store = IsolatedStorageFile.GetUserStoreForApplication();
            store.MoveFile(nativePath(n1), nativePath(n2));
        }

        public override char getFileSystemSeparator()
        {
            return '\\';
        }

        public override object getPlatformName()
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


        public override bool instanceofObjArray(global::java.lang.Object n1)
        {
            return n1 is global::org.xmlvm._nArrayAdapter<global::System.Object>;
        }

        public override bool instanceofByteArray(global::java.lang.Object n1)
        {
            return n1 is global::org.xmlvm._nArrayAdapter<sbyte>;
        }

        public override bool instanceofShortArray(global::java.lang.Object n1)
        {
            return n1 is global::org.xmlvm._nArrayAdapter<short>;
        }

        public override bool instanceofLongArray(global::java.lang.Object n1)
        {
            return n1 is global::org.xmlvm._nArrayAdapter<long>;
        }

        public override bool instanceofIntArray(global::java.lang.Object n1)
        {
            return n1 is global::org.xmlvm._nArrayAdapter<int>;
        }

        public override bool instanceofFloatArray(global::java.lang.Object n1)
        {
            return n1 is global::org.xmlvm._nArrayAdapter<float>;
        }

        public override bool instanceofDoubleArray(global::java.lang.Object n1)
        {
            return n1 is global::org.xmlvm._nArrayAdapter<double>;
        }
    }

    public abstract class OperationPending
    {
        int tick;
        public int clipX, clipY, clipW, clipH;
        public bool clipSet = false;
        protected LinkedList<UIElement> componentPaints;
        protected WipeComponent pendingWipe;

        public OperationPending(NativeGraphics gr, WipeComponent pendingWipe)
        {
            clipSet = gr.clipSet;
            clipX = gr.clipX;
            clipY = gr.clipY;
            clipW = gr.clipW;
            clipH = gr.clipH;
            componentPaints = SilverlightImplementation.currentPaintDestination;
            this.pendingWipe = pendingWipe;
        }

        protected OperationPending(OperationPending p, WipeComponent w)
        {
            clipX = p.clipX;
            clipY = p.clipY;
            clipW = p.clipW;
            clipH = p.clipH;
            clipSet = p.clipSet;
            pendingWipe = w;
        }

        protected void add(UIElement uim)
        {
            if (componentPaints != null)
            {
                componentPaints.AddLast(uim);
            }
        }

        protected void updateClip(FrameworkElement i, int x, int y)
        {
            if (clipSet)
            {
                i.Clip = new RectangleGeometry()
                {
                    Rect = new Rect(clipX - x, clipY - y, clipW, clipH)
                };
            }
        }

        public string clipString()
        {
            if (clipSet)
            {
                return "[clip x: " + clipX + " y: " + clipY + " w: " + clipW + " h: " + clipH + "]";
            }
            return "[no clipping]";
        }

        public virtual bool isDrawOperation()
        {
            return true;
        }

        public virtual void prerender()
        {
        }

        public void performBench(Canvas cl)
        {
            tick = System.Environment.TickCount;
            perform(cl);
        }

        public abstract void perform(Canvas cl);

        public void log(String s)
        {
            System.Diagnostics.Debug.WriteLine(s);
        }

        public void printBench()
        {
            System.Diagnostics.Debug.WriteLine(GetType().Name + " took " + (System.Environment.TickCount - tick));
        }

        public abstract OperationPending clone(WipeComponent w, int translateX, int translateY);

        public abstract void printLogging();
    }

    // wipes the content of the canvas to preserve memory. We do this when we know data was removed
    class WipeScreen : OperationPending
    {
        public WipeScreen(NativeGraphics ng)
            : base(ng, null)
        {
        }

        public override void perform(Canvas cl)
        {
            cl.Children.Clear();
            if (SilverlightImplementation.textInputInstance != null)
            {
                cl.Children.Add(SilverlightImplementation.textInputInstance);
            }
        }

        public override void printLogging()
        {
            log("Wiping screen");
        }

        public override OperationPending clone(WipeComponent w, int translateX, int translateY)
        {
            return null;
        }
    }

    class CSharpListHolder : global::java.lang.Object
    {
        public LinkedList<UIElement> ll = new LinkedList<UIElement>();
    }

    public  class WipeComponent : OperationPending
    {
        public LinkedList<UIElement> ll;
        string componentName;
        public WipeComponent(NativeGraphics ng, LinkedList<UIElement> ll, string componentName)
            : base(ng, null)
        {
            this.ll = new LinkedList<UIElement>();
            foreach (UIElement e in ll)
            {
                this.ll.AddLast(e);
            }
            this.componentName = componentName;
        }

        public override void perform(Canvas cl)
        {
            foreach(UIElement v in ll)
            {
                cl.Children.Remove(v);
            }
            //log("Wiped " + ll.Count + " components from " + componentName);
        }

        public Image getExistingImage(CodenameOneImage source)
        {
            if (ll != null)
            {
                //log("Checking image existance for: " + source.name);
                foreach (UIElement uim in ll)
                {
                    if (uim is Image)
                    {
                        Image im = (Image)uim;
                        if (im.Source == source.img)
                        {
                            ll.Remove(im);
                            //log("Image found for: " + source.name);
                            return im;
                        }
                    }
                }
            }
            if (source.imageCache != null && source.imageCache.Parent == null)
            {
                //log("Returning cached image for: " + source.name);
                return source.imageCache;
            }
            else
            {
                //log("Image not found for: " + source.name);
            }
            return null;
        }

        public override OperationPending clone(WipeComponent w, int translateX, int translateY)
        {
            return null;
        }

        public override void printLogging()
        {
            string s = "";
            foreach (UIElement uim in ll)
            {
                if (uim is TextBlock)
                {
                    s += "Text:'" + ((TextBlock)uim).Text + "',";
                }
                else
                {
                    s += uim.GetType().Name + ", ";
                }
            }
            log("Wiping component, removing " + ll.Count + " from " + componentName + " containing: [" + s + "]");
        }
    }

    class DrawImage : OperationPending
    {
        java.lang.Object image;
        int x;
        int y;
        int w;
        int h;
        int alpha;
        public DrawImage(NativeGraphics ng, WipeComponent pendingWipe, java.lang.Object image, int x, int y)
            : base(ng, pendingWipe)
        {
            this.image = image;
            this.x = x;
            this.y = y;
            w = -1;
            alpha = ng.alpha;
        }

        public DrawImage(NativeGraphics ng, WipeComponent pendingWipe, java.lang.Object image, int x, int y, int w, int h)
            : base(ng, pendingWipe)
        {
            this.image = image;
            this.x = x;
            this.y = y;
            this.w = w;
            this.h = h;
            alpha = ng.alpha;
        }

        protected DrawImage(DrawImage p, WipeComponent pendingWipe, int translateX, int translateY)
            : base(p, pendingWipe)
        {
            this.image = p.image;
            this.x = p.x + translateX;
            this.y = p.y + translateY;
            this.w = p.w;
            this.h = p.h;
            alpha = p.alpha;
        }

        public override OperationPending clone(WipeComponent w, int translateX, int translateY)
        {
            return new DrawImage(this, w, translateX, translateY);
        }

        public override void perform(Canvas cl)
        {
            CodenameOneImage img = (CodenameOneImage)image;
            if (img.img != null)
            {
                Image i = null;
                if (pendingWipe != null)
                {
                    i = pendingWipe.getExistingImage(img);
                    if (i == null)
                    {
                        i = new Image();
                        i.Source = img.img;
                    }
                    else
                    {
                        cl.Children.Remove(i);
                    }
                }
                else
                {
                    i = new Image();
                    i.Source = img.img;
                }
                cl.Children.Add(i);
                i.Opacity = ((double)alpha) / 255.0;
                Canvas.SetLeft(i, x);
                Canvas.SetTop(i, y);
                updateClip(i, x, y);
                if (w > 0)
                {
                    i.Width = w;
                    i.Height = h;
                }
                else
                {
                    i.Width = img.width;
                    i.Height = img.height;
                }
                add(i);
                img.imageCache = i;
            }
        }

        public override void printLogging()
        {
            CodenameOneImage img = (CodenameOneImage)image;
            bool isNull = img.img == null;
            log("Draw image x: " + x + " y: " + y +
                " w: " + w + " h: " + h + " image null: " + isNull + clipString());
        }
    }

    class FillRect : OperationPending
    {
        int x;
        int y;
        int w;
        int h;
        Color color;

        public FillRect(NativeGraphics ng, WipeComponent pendingWipe, int x, int y, int w, int h)
            : base(ng, pendingWipe)
        {
            this.color = ng.sColor;
            this.x = x;
            this.y = y;
            this.w = w;
            this.h = h;
        }

        protected FillRect(FillRect p, WipeComponent pendingWipe, int translateX, int translateY)
            : base(p, pendingWipe)
        {
            this.x = p.x + translateX;
            this.y = p.y + translateY;
            this.w = p.w;
            this.h = p.h;
            color = p.color;
        }

        public override OperationPending clone(WipeComponent w, int translateX, int translateY)
        {
            return new FillRect(this, w, translateX, translateY);
        }

        public override void perform(Canvas cl)
        {
            Rectangle i = new Rectangle();
            cl.Children.Add(i);
            Canvas.SetLeft(i, x);
            Canvas.SetTop(i, y);
            i.Width = w;
            i.Height = h;
            updateClip(i, x, y);
            i.Stroke = null;
            i.Fill = new SolidColorBrush(color);
            add(i);
        }

        public override void printLogging()
        {
            log("Fill rect x: " + x + " y: " + y +
                " w: " + w + " h: " + h + " color: " + color + " " + clipString());
        }
    }

    class DrawRect : OperationPending
    {
        int x;
        int y;
        int w;
        int h;
        int thickness;
        Color color;

        public DrawRect(NativeGraphics ng, WipeComponent pendingWipe, int x, int y, int w, int h, int thickness)
            : base(ng, pendingWipe)
        {
            this.color = ng.sColor;
            this.x = x;
            this.y = y;
            this.w = w;
            this.h = h;
            this.thickness = thickness;
        }

        protected DrawRect(DrawRect p, WipeComponent pendingWipe, int translateX, int translateY)
            : base(p, pendingWipe)
        {
            this.x = p.x + translateX;
            this.y = p.y + translateY;
            this.w = p.w;
            this.h = p.h;
            this.thickness = thickness;
            color = p.color;
        }

        public override OperationPending clone(WipeComponent w, int translateX, int translateY)
        {
            return new DrawRect(this, w, translateX, translateY);
        }

        public override void perform(Canvas cl)
        {
            Rectangle i = new Rectangle();
            cl.Children.Add(i);
            Canvas.SetLeft(i, x);
            Canvas.SetTop(i, y);
            i.Width = w;
            i.Height = h;
            updateClip(i, x, y);
            i.Stroke = new SolidColorBrush(color);
            i.Fill = null;
            i.StrokeThickness = thickness;
            add(i);
        }

        public override void printLogging()
        {
            log("Draw rect x: " + x + " y: " + y +
                " w: " + w + " h: " + h + " color: " + color + " " + clipString());
        }
    }

    class DrawRoundRect : OperationPending
    {
        int x;
        int y;
        int w;
        int h;
        int arcW;
        int arcH;
        SolidColorBrush fill = null;
        SolidColorBrush stroke = null;

        public DrawRoundRect(NativeGraphics ng, WipeComponent pendingWipe, int x, int y, int w, int h, int arcW, int arcH, bool fill)
            : base(ng, pendingWipe)
        {
            Color color = ng.sColor;
            this.x = x;
            this.y = y;
            this.w = w;
            this.h = h;
            this.arcW = arcW;
            this.arcH = arcH;
            if (fill)
            {
                this.fill = new SolidColorBrush(color);
            }
            else
            {
                stroke = new SolidColorBrush(color);
            }
        }

        protected DrawRoundRect(DrawRoundRect p, WipeComponent pendingWipe, int translateX, int translateY)
            : base(p, pendingWipe)
        {
            this.x = p.x + translateX;
            this.y = p.y + translateY;
            this.w = p.w;
            this.h = p.h;
            this.fill = p.fill;
            this.stroke = p.stroke;
            this.arcW = p.arcW;
            this.arcH = p.arcH;
        }

        public override OperationPending clone(WipeComponent w, int translateX, int translateY)
        {
            return new DrawRoundRect(this, w, translateX, translateY);
        }

        public override void perform(Canvas cl)
        {
            Rectangle i = new Rectangle();
            cl.Children.Add(i);
            Canvas.SetLeft(i, x);
            Canvas.SetTop(i, y);
            i.RadiusX = arcW;
            i.RadiusY = arcH;
            i.Width = w;
            i.Height = h;
            updateClip(i, x, y);
            i.Stroke = stroke;
            i.Fill = fill;
            add(i);
        }

        public override void printLogging()
        {
            log("Draw round rect x: " + x + " y: " + y +
                " w: " + w + " h: " + h +  " " + clipString());
        }
    }

    class DrawLine : OperationPending
    {
        int x1;
        int y1;
        int x2;
        int y2;
        Color color;

        public DrawLine(NativeGraphics ng, WipeComponent pendingWipe, int x1, int y1, int x2, int y2)
            : base(ng, pendingWipe)
        {
            this.color = ng.sColor;
            this.x1 = x1;
            this.y1 = y1;
            this.x2 = x2;
            this.y2 = y2;
        }

        protected DrawLine(DrawLine p, WipeComponent pendingWipe, int translateX, int translateY)
            : base(p, pendingWipe)
        {
            this.x1 = p.x1 + translateX;
            this.y1 = p.y1 + translateY;
            this.x2 = p.x2 + translateX;
            this.y2 = p.y2 + translateY;
            this.color = p.color;
        }

        public override OperationPending clone(WipeComponent w, int translateX, int translateY)
        {
            return new DrawLine(this, w, translateX, translateY);
        }

        public override void perform(Canvas cl)
        {
            Line i = new Line();
            cl.Children.Add(i);
            Canvas.SetLeft(i, 0);
            Canvas.SetTop(i, 0);
            i.X1 = x1;
            i.Y1 = y1;
            i.X2 = x2;
            i.Y2 = y2;
            updateClip(i, 0, 0);
            i.Stroke = new SolidColorBrush(color);
            i.Fill = null;
            add(i);
        }

        public override void printLogging()
        {
            log("Draw line x1: " + x1 + " y1: " + y1 +
                " x2: " + x2 + " y2: " + y2 + " color: " + color + " " + clipString());
        }
    }

    class PlacePeer : OperationPending
    {
        int x;
        int y;
        int w;
        int h;
        FrameworkElement elem;

        public PlacePeer(NativeGraphics ng, WipeComponent pendingWipe, int x, int y, int w, int h, FrameworkElement elem)
            : base(ng, pendingWipe)
        {
            this.elem = elem;
            this.x = x;
            this.y = y;
            this.w = w;
            this.h = h;
        }

        public override OperationPending clone(WipeComponent w, int translateX, int translateY)
        {
            return null;
        }

        public override void perform(Canvas cl)
        {
            if (elem.Parent == null)
            {
                cl.Children.Add(elem);
            }
            if (pendingWipe != null && pendingWipe.ll != null)
            {
                pendingWipe.ll.Remove(elem);
            }
            Canvas.SetLeft(elem, x);
            Canvas.SetTop(elem, y);
            elem.Width = w;
            elem.Height = h;
            updateClip(elem, x, y);
            add(elem);
        }

        public override void printLogging()
        {
            log("Painting native peer");
        }
    }

    class DrawString : OperationPending
    {
        int x;
        int y;
        string str;
        Color color;
        NativeFont font;

        public DrawString(NativeGraphics ng, WipeComponent pendingWipe, NativeFont font, string str, int x, int y)
            : base(ng, pendingWipe)
        {
            this.color = ng.sColor;
            this.x = x;
            this.y = y;
            this.str = str;
            this.font = font;
        }

        protected DrawString(DrawString p, WipeComponent pendingWipe, int translateX, int translateY)
            : base(p, pendingWipe)
        {
            this.x = p.x + translateX;
            this.y = p.y + translateY;
            this.str = p.str;
            this.font = p.font;
            this.color = p.color;
        }

        public override OperationPending clone(WipeComponent w, int translateX, int translateY)
        {
            return new DrawString(this, w, translateX, translateY);
        }

        public override void perform(Canvas cl)
        {
            TextBlock i = new TextBlock();
            i.FontSize = font.height;
            i.Text = str;
            i.Foreground = new SolidColorBrush(color);
            //i.Measure(new Size(100000, 100000));
            //i.Width = i.DesiredSize.Width;
            //i.Height = font.actualHeight;
            cl.Children.Add(i);
            Canvas.SetLeft(i, x);
            Canvas.SetTop(i, y);
            updateClip(i, x, y);
            add(i);
        }

        public override void printLogging()
        {
            log("Draw string x: " + x + " y: " + y +
                " str: " + str + " color: " + color + " " + clipString());
        }
    }

    public class NativeGraphics : global::java.lang.Object
    {
        public System.Collections.Generic.List<OperationPending> pendingPaintsList = new System.Collections.Generic.List<OperationPending>();
        public int clipX, clipY, clipW, clipH;
        public bool clipSet = false;
        public int color;
        public int alpha = 255;
        public NativeFont font;
        public Color sColor
        {
            get
            {
                Color cc = new Color();
                cc.A = (byte)alpha;
                cc.B = (byte)(color & 0xff);
                cc.R = (byte)((color >> 16) & 0xff);
                cc.G = (byte)((color >> 8) & 0xff);
                return cc;
            }
        }

        public virtual void paint(OperationPending o) {
            pendingPaintsList.Add(o);
        }
    }

    public class CodenameOneImage : java.lang.Object
    {
        public ImageSource img;
        public int width = 10;
        public int height = 10;
        public string name;
        public Image imageCache;

        public void @this()
        {
            base.@this();
        }

        public virtual int getImageWidth()
        {
            return width;
        }

        public virtual int getImageHeight()
        {
            return height;
        }
    }

    /**
     * A mutable image is just a series of paints
     */
    class CodenameOneMutableImage : CodenameOneImage
    {
        public MutableImageGraphics graphics;
        public bool opaque;
        public System.Collections.Generic.List<OperationPending> imagePaints = new System.Collections.Generic.List<OperationPending>();

        public void @this()
        {
            base.@this();
        }

        public CodenameOneMutableImage()
        {
            graphics = new MutableImageGraphics(this);
        }
    }

    /**
     * A mutable image is just a series of paints
     */
    class MutableImageGraphics : NativeGraphics
    {
        private CodenameOneMutableImage parent;
        public MutableImageGraphics(CodenameOneMutableImage parent)
        {
            this.parent = parent;
        }

        public override void paint(OperationPending o)
        {
            parent.imagePaints.Add(o);
        }
    }

    public class NativeFont : global::java.lang.Object
    {
        public int height;
        public int systemSize;
        public int systemFace;
        public int systemStyle;
        public bool bold;
        public bool italic;
        public int actualHeight;

        public override bool Equals(object o)
        {
            NativeFont f = (NativeFont)o;
            return f.height == height && f.systemFace == systemFace && f.systemSize == systemSize && f.systemStyle == systemStyle;
        }

        public override int GetHashCode()
        {
            return height;
        }
    }

    class WaitForEdit : java.lang.Object, java.lang.Runnable
    {
        public virtual void run()
        {
            while (SilverlightImplementation.instance.currentlyEditing != null)
            {
                global::System.Threading.Thread.Sleep(1);
            }
        }
    }

    class EditString : java.lang.Object, java.lang.Runnable
    {
        private global::com.codename1.ui.Component n1;
        private int n2;
        private int n3;
        private global::java.lang.String n4;
        private int n5;

        public EditString(global::com.codename1.ui.Component n1, int n2, int n3, global::java.lang.String n4, int n5)
        {
            this.n1 = n1;
            this.n2 = n2;
            this.n3 = n3;
            this.n4 = n4;
            this.n5 = n5;
        }

        public virtual void run()
        {
            com.codename1.ui.Display d = (com.codename1.ui.Display)com.codename1.ui.Display.getInstance();
            d.editString(n1, n2, n3, n4, n5);
        }
    }

    class WaitForNativeEdit : java.lang.Object, java.lang.Runnable
    {
        public virtual void run()
        {
            while (SilverlightImplementation.textInputInstance != null)
            {
                global::System.Threading.Thread.Sleep(1);
            }
        }
    }

    public class SoftRef : java.lang.Object
    {
        global::System.WeakReference w;
        //java.lang.Object o;

        public SoftRef(java.lang.Object obj)
        {
            w = new WeakReference(obj);
            //o = obj;
        }

        public java.lang.Object get()
        {
            java.lang.Object o = (java.lang.Object)w.Target;
            return o;
            //return o;
        }
    }

    class NetworkOperation : java.lang.Object
    {
        private bool responseCompleted;
        private bool postCompleted;
        public HttpWebRequest request;

        public Stream requestStream
        {
            get
            {
                if (postData == null)
                {
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
            get
            {
                if (resp == null)
                {
                    if (postData != null)
                    {
                        using (AutoResetEvent are = new AutoResetEvent(false))
                        {
                            System.Windows.Deployment.Current.Dispatcher.BeginInvoke(() =>
                            {
                                try
                                {
                                    postData.Close();
                                }
                                catch (Exception e) { }
                                are.Set();
                            });
                            are.WaitOne();
                        }
                    }
                    request.BeginGetResponse(ResponseCallback, request);
                    while (!responseCompleted)
                    {
                        System.Threading.Thread.Sleep(5);
                    }
                    if (resp == null)
                    {
                        global::java.io.IOException io = new global::java.io.IOException();
                        if (error != null)
                        {
                            io.@this(SilverlightImplementation.toJava(error.Message));
                        }
                        else
                        {
                            io.@this(SilverlightImplementation.toJava("Null response"));
                        }
                        throw new global::org.xmlvm._nExceptionAdapter(io);
                    }
                }
                return resp;
            }
        }
        private HttpWebResponse resp;
        private WebException error;

        private void ResponseCallback(IAsyncResult asyncResult)
        {
            try
            {
                resp = (HttpWebResponse)request.EndGetResponse(asyncResult);
            }
            catch (WebException we)
            {
                error = we;
                if (we.Response != null)
                {
                    resp = (HttpWebResponse)we.Response;
                }
            }
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
            if (v <= 0)
            {
                return -1;
            }

            for (int iter = 0; iter < v; iter++)
            {
                sb[iter] = (sbyte)buffer[iter];
            }
            return v;
        }

        public override int read(global::org.xmlvm._nArrayAdapter<sbyte> n1, int n2, int n3)
        {
            sbyte[] sb = n1.getCSharpArray();
            byte[] buffer = new byte[sb.Length];
            int v = internalStream.Read(buffer, n2, n3);
            if (v <= 0)
            {
                return -1;
            }
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
            if (internalStream == null)
            {
                return;
            }
            try
            {
                internalStream.Close();
            }
            catch (Exception err) {
                internalStream = null;
            }
        }

        public override void flush()
        {
            if (internalStream == null)
            {
                return;
            }
            try
            {
                internalStream.Flush();
            }
            catch (Exception err) {
                internalStream = null;
            }
        }

        public override void write(global::org.xmlvm._nArrayAdapter<sbyte> n1)
        {
            internalStream.Write(SilverlightImplementation.toByteArray(n1.getCSharpArray()), 0, n1.Length);
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

    public class StringFontPair
    {
        public string str;
        public NativeFont font;

        public StringFontPair(string str, NativeFont font)
        {
            this.str = str;
            this.font = font;
        }

        public override bool Equals(Object o)
        {
            StringFontPair sp = (StringFontPair)o;
            return str.Equals(sp.str) && font.Equals(sp.font);
        }

        public override int GetHashCode()
        {
            return str.GetHashCode();
        }
    }

    public class SilverlightPeer : com.codename1.ui.PeerComponent 
    {
        public FrameworkElement element;
        public SilverlightPeer(FrameworkElement element)
        {
            this.element = element;
            @this(null);
        }

        public override void paint(com.codename1.ui.Graphics g)
        {
            ((NativeGraphics)g.getGraphics()).paint(new PlacePeer((NativeGraphics)g.getGraphics(), SilverlightImplementation.instance.pendingWipe, getAbsoluteX(), getAbsoluteY(), getWidth(), getHeight(), element));
        }

        public override global::System.Object calcPreferredSize()
        {
            int w = 0;
            int h = 0;
            using (AutoResetEvent are = new AutoResetEvent(false))
            {
                System.Windows.Deployment.Current.Dispatcher.BeginInvoke(() =>
                {
                    element.Measure(new Size(1000000, 1000000));
                    w = (int)element.DesiredSize.Width;
                    h = (int)element.DesiredSize.Height;
                    are.Set();
                });
                are.WaitOne();
            }
            com.codename1.ui.geom.Dimension d = new com.codename1.ui.geom.Dimension();
            d.@this(Math.Max(2, w), Math.Max(2, h));
            return d;
        }
    }

    public class CN1Media : com.codename1.media.Media
    {
        private MediaElement elem;
        private SilverlightPeer peer;
        private bool video;
        private java.lang.Runnable onComplete;

        public CN1Media(string uri, bool video, java.lang.Runnable onComplete)
        {
            System.Windows.Deployment.Current.Dispatcher.BeginInvoke(() =>
            {
                elem = new MediaElement();
                elem.Source = new Uri(uri, UriKind.RelativeOrAbsolute);
                this.video = video;
                this.onComplete = onComplete;
                elem.MediaEnded += elem_MediaEnded;
            });
        }

        public CN1Media(Stream s, string mime, java.lang.Runnable onComplete)
        {
            System.Windows.Deployment.Current.Dispatcher.BeginInvoke(() =>
            {
                elem = new MediaElement();
                elem.SetSource(s);
                video = true;
                this.onComplete = onComplete;
                elem.MediaEnded += elem_MediaEnded;
            });
        }

        void elem_MediaEnded(object sender, RoutedEventArgs e)
        {
            if (onComplete != null)
            {
                com.codename1.ui.Display disp = (com.codename1.ui.Display)com.codename1.ui.Display.getInstance();
                disp.callSerially(onComplete);
            }
        }

        public virtual void play()
        {
            elem.Play();
        }

        public virtual void pause()
        {
            elem.Pause();
        }

        public virtual void cleanup()
        {
            elem = null;
        }

        public virtual int getTime()
        {
            int v = 0;
            using (AutoResetEvent are = new AutoResetEvent(false))
            {
                System.Windows.Deployment.Current.Dispatcher.BeginInvoke(() =>
                {
                    v = (int)elem.Position.TotalMilliseconds;
                    are.Set();
                });
                are.WaitOne();
            }
            return v;
        }

        public virtual void setTime(int n1)
        {
            elem.Position = TimeSpan.FromMilliseconds(n1);
        }

        public virtual int getDuration()
        {
            int v = 0;
            using (AutoResetEvent are = new AutoResetEvent(false))
            {
                System.Windows.Deployment.Current.Dispatcher.BeginInvoke(() =>
                {
                    v = (int)elem.NaturalDuration.TimeSpan.TotalMilliseconds;
                    are.Set();
                });
                are.WaitOne();
            }
            return v;
        }

        public virtual void setVolume(int n1)
        {
            System.Windows.Deployment.Current.Dispatcher.BeginInvoke(() =>
            {
                elem.Volume = ((double)n1) / 100.0;
            });
        }

        public virtual int getVolume()
        {
            int v = 0;
            using (AutoResetEvent are = new AutoResetEvent(false))
            {
                System.Windows.Deployment.Current.Dispatcher.BeginInvoke(() =>
                {
                    v = (int)(elem.Volume * 100.0);
                    are.Set();
                });
                are.WaitOne();
            }
            return v;
        }

        public virtual bool isPlaying()
        {
            bool b = false;
            using (AutoResetEvent are = new AutoResetEvent(false))
            {
                System.Windows.Deployment.Current.Dispatcher.BeginInvoke(() =>
                {
                    b = elem.CurrentState == MediaElementState.Playing || elem.CurrentState == MediaElementState.Buffering;
                    are.Set();
                });
                are.WaitOne();
            }
            return b;
        }

        public virtual global::System.Object getVideoComponent()
        {
            if (peer == null)
            {
                peer = new SilverlightPeer(elem);
            }
            return peer;
        }

        public virtual bool isVideo()
        {
            return video;
        }

        public virtual bool isFullScreen()
        {
            return false;
        }

        public virtual void setFullScreen(bool n1)
        {
        }

        public virtual void setNativePlayerMode(bool n1)
        {
        }

        public virtual bool isNativePlayerMode()
        {
            return false;
        }
    }
} // end of namespace: com.codename1.impl
