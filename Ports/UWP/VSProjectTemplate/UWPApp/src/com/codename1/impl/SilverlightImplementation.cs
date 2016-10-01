using System.IO;
using System.Linq;
using System;
using System.Threading;
using System.Collections.Generic;
using System.Net;
using System.Diagnostics;
using Microsoft.Graphics.Canvas;
using Windows.UI.Xaml.Controls;
using Windows.UI.Xaml;
using System.Threading.Tasks;
using Windows.UI.Core;
using Windows.UI.Input;
using Windows.UI.Xaml.Input;
using Windows.Foundation;
using Windows.Storage;
using Windows.Devices.Sensors;
using Windows.ApplicationModel.Contacts;
using Windows.ApplicationModel;
using Windows.Storage.Streams;
using Windows.UI.Xaml.Media;
using Windows.Media.Capture;
using Windows.Storage.Pickers;
using Microsoft.Graphics.Canvas.UI.Xaml;
using Microsoft.Graphics.Canvas.Text;
using System.Runtime.InteropServices.WindowsRuntime;
using com.codename1.ui.animations;
using com.codename1.ui;
using com.codename1.ui.geom;
using Windows.UI.Xaml.Media.Imaging;
using Windows.Graphics.Display;
using Windows.UI.Text;
using com.codename1.payment;
using System.Collections.Concurrent;
using System.Numerics;
using Windows.ApplicationModel.Activation;
using Windows.ApplicationModel.Core;
using com.codename1.ui.events;
using System.Collections;
using Windows.Data.Xml.Dom;
using Windows.Devices.Input;
using Windows.Graphics.Imaging;
using Microsoft.Graphics.Canvas.Geometry;
using java.io;
using Windows.Foundation.Metadata;
using Windows.ApplicationModel.DataTransfer;
using com.codename1.db;
using Windows.UI.ViewManagement;
#if WINDOWS_UWP
using Windows.Graphics.DirectX;
#else
using Microsoft.Graphics.Canvas.DirectX;
#endif
using IKVM.Runtime;



namespace com.codename1.impl
{

    public class SilverlightImplementation : CodenameOneImplementation
    {
        private LocationManager locationManager;
        private static object PAINT_LOCK = new object();
        public static SilverlightImplementation instance;
        public static Canvas cl;
        private int displayWidth = -1, displayHeight = -1;
        private CanvasTextFormat defaulFontCanvas;
        private NativeFont defaultFont;
        public TextArea currentlyEditing;
        public static Control textInputInstance;
        public static Page app;
        public static CanvasControl screen;
        public static double scaleFactor = 1;
        public static double logicalDpi = -1;
        public static ResolutionScale resolutionScale;
        public static double? diagonalSizeInInches = -1;
        public static CoreDispatcher dispatcher;
        public static StorageFolder iDefaultStore;
        private static CoreApplicationView view;
        public static bool isPhone = true;
        private static string ss;
#if WINDOWS_PHONE_APP
        public FileOpenPickerContinuationEventArgs FilePickerContinuationArgs { get; set; }
#endif
        public static Dictionary<string, string> iCN1Settings; // = new Dictionary<string, string>();

        public static void setCanvas(Page page, Canvas LayoutRoot)
        {
            iCN1Settings = loadSettings("/install:/CN1WindowsPort.xml"); // ms-appx:///CN1WindowsPort.xml");
            view = CoreApplication.GetCurrentView();
            dispatcher = CoreWindow.GetForCurrentThread().Dispatcher;
            cl = LayoutRoot;
            app = page;
#if WINDOWS_PHONE_APP
            scaleFactor = Windows.Graphics.Display.DisplayInformation.GetForCurrentView().RawPixelsPerViewPixel;
               ss = getDictValue(iCN1Settings, "DefaultStorageFolder", "Cache:");
#elif WINDOWS_UWP
              ss = getDictValue(iCN1Settings, "DefaultStorageFolder", "Local:");
              scaleFactor = Windows.Graphics.Display.DisplayInformation.GetForCurrentView().RawPixelsPerViewPixel;
#elif WINDOWS_APP
             ss = getDictValue(iCN1Settings, "DefaultStorageFolder", "Local:");
            scaleFactor = ScaleDesktop.ScaleFactor;
#endif
            iDefaultStore = getStore(ss); // storeApplicationData.Current.CacheFolder; // Faster, avoid cloud backup. See https://www.suchan.cz/2014/07/file-io-best-practices-in-windows-and-phone-apps-part-1-available-apis-and-file-exists-checking/
            rawDpiy = DisplayInformation.GetForCurrentView().RawDpiY;
            rawDpix = DisplayInformation.GetForCurrentView().RawDpiX;
            logicalDpi = DisplayInformation.GetForCurrentView().LogicalDpi;
            diagonalSizeInInches = DisplayInformation.GetForCurrentView().DiagonalSizeInInches;
            resolutionScale = DisplayInformation.GetForCurrentView().ResolutionScale;
            screen = new CanvasControl();
            screen.Width = cl.ActualWidth * scaleFactor;
            screen.Height = cl.ActualHeight * scaleFactor;
            cl.Children.Add(screen);
            screen.ClearColor = Windows.UI.Colors.Transparent; // Maybe white ?
            Canvas.SetLeft(screen, 0);
            Canvas.SetTop(screen, 0);
            myView = new WindowsAsyncView(screen);
        }

        private static string getDictValue(Dictionary<string, string> aSettings, string aKey, string aDefaultValue)
        {
            if (aSettings.Keys.Contains(aKey))
                return aSettings[aKey];
            else
                return aDefaultValue;
        }

        private static Dictionary<string, string> loadSettings(string aUrl)
        {
            Dictionary<string, string> settings = new Dictionary<string, string>();
            XmlDocument doc = null; // = new XmlDocument();
            try
            {
                StorageFile file = getFile(aUrl);
                if (file != null) doc = XmlDocument.LoadFromFileAsync(file).AsTask().GetAwaiter().GetResult();
            }
            catch (Exception) { }
            if (doc == null)
                return settings;
            XmlNodeList elemList = doc.GetElementsByTagName("add");
            foreach (var e in elemList)
            {
                var atr = e.Attributes;
                string key = null;
                string value = null;
                foreach (var a in atr)
                {
                    var name = a.NodeName;
                    if (name.Equals("key"))
                        key = (string)a.NodeValue;
                    if (name.Equals("value"))
                        value = (string)a.NodeValue;
                    if (value != null && key != null)
                    {
                        settings.Add(key, value);
                        break;
                    }
                }
            }
            return settings;
        }
#if WINDOWS_PHONE_APP
        private void page_BackKeyPress(object sender, Windows.Phone.UI.Input.BackPressedEventArgs e)
        {
            keyPressed(getBackKeyCode());
            keyReleased(getBackKeyCode());
            e.Handled = true;
        }
#endif
        public override bool shouldWriteUTFAsGetBytes()
        {
            return true;
        }

        public override java.io.InputStream getResourceAsStream(global::java.lang.Class n1, string n2)
        {
            try
            {
                string uri = n2;
                if (uri.StartsWith("/"))
                {
                    uri = @"res\" + uri.Substring(1);
                }
                uri = uri.Replace('/', '\\');
                StorageFolder installFolder = Package.Current.InstalledLocation;
                StorageFile file = installFolder.GetFileAsync(uri).AsTask().GetAwaiter().GetResult();
                Stream strm = Task.Run(() => file.OpenStreamForReadAsync()).GetAwaiter().GetResult();
                byte[] byteArr = new byte[strm.Length];
                strm.Read(byteArr, 0, byteArr.Length);
                java.io.ByteArrayInputStream bi = new java.io.ByteArrayInputStream(byteArr);
                //bi.@this(new byte[](toSByteArray(byteArr)));
                return bi;
            }
            catch (System.Exception)
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
                System.Buffer.BlockCopy(byteArray, 0, sbyteArray, 0, byteArray.Length);
            }
            return sbyteArray;
        }

        public override void init(object n1)
        {

           
            instance = this;
            dispatcher.RunAsync(CoreDispatcherPriority.Normal, () =>
          {
#if WINDOWS_PHONE_APP
              Windows.Phone.UI.Input.HardwareButtons.BackPressed += page_BackKeyPress;
               isPhone = false;
#elif WINDOWS_UWP
              if (Windows.Foundation.Metadata.ApiInformation.IsTypePresent("Windows.Phone.UI.Input.HardwareButtons"))
              {
                  isPhone = false;
                  Windows.UI.Core.SystemNavigationManager.GetForCurrentView().BackRequested += SilverlightImplementation_BackRequested;
              }
#endif
              cl.SizeChanged += cl_SizeChanged;
              cl.ManipulationMode = ManipulationModes.All;
              screen.ManipulationMode = ManipulationModes.All;
              screen.PointerPressed += new PointerEventHandler(LayoutRoot_PointerPressed);
              screen.PointerReleased += new PointerEventHandler(LayoutRoot_PointerReleased);
              screen.PointerMoved += new PointerEventHandler(LayoutRoot_PointerMoved);
          }).AsTask().GetAwaiter();
            Display.getInstance().getDragSpeed(true);
            _sensor = SimpleOrientationSensor.GetDefault();
            if (_sensor != null)
            {
                _sensor.OrientationChanged += new TypedEventHandler<SimpleOrientationSensor, SimpleOrientationSensorOrientationChangedEventArgs>(app_OrientationChanged);
            }
            Display.getInstance().setTransitionYield(0);
            setDragStartPercentage(3);
        }
#if WINDOWS_UWP
        private void SilverlightImplementation_BackRequested(object sender,Windows.UI.Core.BackRequestedEventArgs e)
        {
            keyPressed(getBackKeyCode());
            keyReleased(getBackKeyCode());
            e.Handled = true;
        }
#endif
         void cl_SizeChanged(object sender, SizeChangedEventArgs e)
        {
            InvalidateLayout();
        }
 
        void app_OrientationChanged(object sender, SimpleOrientationSensorOrientationChangedEventArgs e)
        {
            InvalidateLayout();
        }

        private async void InvalidateLayout()
        {
            await dispatcher.RunAsync(CoreDispatcherPriority.Normal, () =>
            {
                screen.Height = displayHeight = Convert.ToInt32(cl.ActualHeight * scaleFactor);
                screen.Width = displayWidth = Convert.ToInt32(cl.ActualWidth * scaleFactor);
                sizeChanged(displayWidth, displayHeight);
                Form f = getCurrentForm();
                if (f != null)
                {
                    base.repaint(f);
                }
            });
        }

        public override bool canForceOrientation()
        {
            return true;
        }

        public override string getProperty(string n1, string n2)
        {
            string p = n1.ToLower();

            if (p.Equals("os"))
            {
                return "Windows Phone";
            }
            if (p.Equals("platform"))
            {
                return "" + Package.Current.Id.Version.Build;
            }
            if (p.Equals("osver"))
            {
                return "" + Package.Current.Id.Version.Major + "." + Package.Current.Id.Version.Minor;
            }
            if (p.Equals("user-agent"))
            {
                return "M";
            }
            if (n1.Equals("windows.DisplayInformation.LogicalDpi"))
            {
                //return "" + DisplayInformation.GetForCurrentView().LogicalDpi;
                return logicalDpi + "";

            }
            if (n1.Equals("windows.DisplayInformation.DiagonalSizeInInches"))
            {
                //return ""+DisplayInformation.GetForCurrentView().DiagonalSizeInInches;
                return diagonalSizeInInches + "";
            }
            if (n1.Equals("windows.DisplayInformation.RawDpiX"))
            {
                //return "" + DisplayInformation.GetForCurrentView().RawDpiX;
                return rawDpix+"";
            }
            if (n1.Equals("windows.DisplayInformation.RawDpiY"))
            {
                //return "" + DisplayInformation.GetForCurrentView().RawDpiY;
                return "" + rawDpiy;
            }
            if (n1.Equals("windows.DisplayInformation.RawPixelsPerViewPixel"))
            {
                //return "" + DisplayInformation.GetForCurrentView().RawPixelsPerViewPixel;
                return scaleFactor+"";
            }
            if (n1.Equals("windows.DisplayInformation.ResolutionScale"))
            {
                //return "" + DisplayInformation.GetForCurrentView().ResolutionScale;
                return resolutionScale + "";
            }
            
            return base.getProperty(n1, n2);
        }
        /// <summary>
        ///  not ideal but I couldn't find any other way...
        ///  Application.Current.Exit(); // TODO - suspending handler
        ///  if back command is not defined the minimizeApplication is called so better is to do nothing
        /// </summary>
        /// <returns></returns>
        public override bool minimizeApplication()
        {
            return true;
        }
        public override void exitApplication()
        {
            Application.Current.Exit();
        }

        public override media.Media createMedia(java.io.InputStream n1, string n2, java.lang.Runnable n3)
        {
            object ss = createStorageOutputStream("CN1TempVideodu73aFljhuiw3yrindo87.mp4");
            java.io.OutputStream os = (java.io.OutputStream)ss;
            io.Util.copy(n1, os);
            StorageFile storageTask = getFile("CN1TempVideodu73aFljhuiw3yrindo87.mp4");
            StorageFile file = storageTask;
            Task<Stream> streamTask = file.OpenStreamForReadAsync();
            Stream s = streamTask.Result;
            return new CN1Media(s, n2, n3, cl);
        }

        public override media.Media createMedia(string uri, bool isVideo, java.lang.Runnable onCompletion)
        {
            if (isTempFile(uri))
            {
                StorageFile sf = getTempFile(uri);
                uri = sf.Path;
            }
            else if (uri.StartsWith("file://"))
            {
                StorageFile sf;
                openInputStream(uri, true, out sf);
                uri = sf.Path;
            }
            return new CN1Media(uri, isVideo, onCompletion, cl);
        }

        

        public override void lockOrientation(bool portrait)
        {
            dispatcher.RunAsync(CoreDispatcherPriority.Normal, () =>
            {
                if (portrait)
                {
                    DisplayInformation.AutoRotationPreferences = DisplayOrientations.Portrait;
                }
                else
                {
                    DisplayInformation.AutoRotationPreferences = DisplayOrientations.Landscape;
                }

            }).AsTask().ConfigureAwait(false).GetAwaiter().GetResult();
        }

        public override void unlockOrientation()
        {
            dispatcher.RunAsync(CoreDispatcherPriority.Normal, () =>
            {
                DisplayInformation.AutoRotationPreferences = DisplayOrientations.None;
            }).AsTask().ConfigureAwait(false).GetAwaiter().GetResult();
        }

        public override bool hasNativeTheme()
        {
            return true;
        }

        public override void installNativeTheme()
        {
            ui.util.Resources r = ui.util.Resources.open("/winTheme.res");
            ui.plaf.UIManager uim = ui.plaf.UIManager.getInstance();
            string[] themeNames = r.getThemeResourceNames();
            uim.setThemeProps(r.getTheme((string)themeNames[0]));
            ui.plaf.DefaultLookAndFeel dl = (ui.plaf.DefaultLookAndFeel)uim.getLookAndFeel();
            dl.setDefaultEndsWith3Points(false);
        }

        public override bool isMultiTouch()
        {
            return true;
        }

        private void LayoutRoot_PointerMoved(object sender, PointerRoutedEventArgs e)
        {
            var pointerId = e.Pointer;
            point = e.GetCurrentPoint(cl).Position;
            var BB = e.GetIntermediatePoints(cl);
            if (pointerId.PointerDeviceType == PointerDeviceType.Touch || pointerId.PointerDeviceType == PointerDeviceType.Mouse || pointerId.PointerDeviceType == PointerDeviceType.Pen)
            {
                PointerPoint ptrPt = e.GetCurrentPoint(screen);
                if (ptrPt.Properties.IsLeftButtonPressed)
                {
                    pointerDragged(Convert.ToInt32(point.X * scaleFactor), Convert.ToInt32(point.Y * scaleFactor));
                }
                if (ptrPt.Properties.IsMiddleButtonPressed)
                {
                    pointerDragged(Convert.ToInt32(point.X * scaleFactor), Convert.ToInt32(point.Y * scaleFactor));
                }
                if (ptrPt.Properties.IsRightButtonPressed)
                {
                    pointerDragged(Convert.ToInt32(point.X * scaleFactor), Convert.ToInt32(point.Y * scaleFactor));
                }
            }
            e.Handled = true;
        }

        private void LayoutRoot_PointerPressed(object sender, PointerRoutedEventArgs e)
        {
            point = e.GetCurrentPoint(cl).Position;
            if (instance.currentlyEditing != null)
            {
                Form f = instance.currentlyEditing.getComponentForm();
                if (f.getComponentAt(Convert.ToInt32(point.X * scaleFactor), Convert.ToInt32(point.Y * scaleFactor)) == instance.currentlyEditing)
                {
                    return;
                }
            }
            pointerPressed(Convert.ToInt32(point.X * scaleFactor), Convert.ToInt32(point.Y * scaleFactor));
            screen.CapturePointer(e.Pointer);
            e.Handled = true;
        }

        private void LayoutRoot_PointerReleased(object sender, PointerRoutedEventArgs e)
        {
            point = e.GetCurrentPoint(cl).Position;
            if (instance.currentlyEditing != null)
            {
                Form f = instance.currentlyEditing.getComponentForm();
                if (f.getComponentAt(Convert.ToInt32(point.X * scaleFactor), Convert.ToInt32(point.Y * scaleFactor)) != instance.currentlyEditing)
                {
                    commitEditing();
                }
            }
            pointerReleased(Convert.ToInt32(point.X * scaleFactor), Convert.ToInt32(point.Y * scaleFactor));
            e.Handled = true;
            return;
        }

        protected override int getDragAutoActivationThreshold()
        {
            return 1000000;
        }

        public override int getDisplayWidth()
        {
            if (displayWidth < 0)
            {
                dispatcher.RunAsync(CoreDispatcherPriority.Normal, () =>
                {
                    displayWidth = Convert.ToInt32(cl.ActualWidth * scaleFactor);

                }).AsTask().ConfigureAwait(false).GetAwaiter().GetResult();
            }
            return displayWidth;
        }

        public override int getDisplayHeight()
        {
            if (displayHeight < 0)
            {
                dispatcher.RunAsync(CoreDispatcherPriority.Normal, () =>
                {
                    displayHeight = Convert.ToInt32(cl.ActualHeight * scaleFactor);

                }).AsTask().ConfigureAwait(false).GetAwaiter().GetResult();

            }
            return displayHeight;
        }

        public override int getActualDisplayHeight()
        {
            return getDisplayHeight();
        }

        public override bool isNativeInputSupported()
        {
            return true;
        }

        public override bool isNativeInputImmediate()
        {
            return true;
        }

        public override int getKeyboardType()
        {
            KeyboardCapabilities keyboardCapabilities = new Windows.Devices.Input.KeyboardCapabilities();
            if (keyboardCapabilities.KeyboardPresent != 0)
            {
                return com.codename1.ui.Display.KEYBOARD_TYPE_QWERTY;
            } else
            {
                return com.codename1.ui.Display.KEYBOARD_TYPE_VIRTUAL;
            }
            
        }

        public static void commitEditing()
        {
            instance.currentlyEditing = null;
        }

        private void setConstraint(TextBox tb, InputScopeNameValue v)
        {
            InputScope ins = new InputScope();
            InputScopeName insane = new InputScopeName();
            insane.NameValue = v;
            ins.Names.Add(insane);
            tb.InputScope = ins;
        }

        private bool lockEditing;

        public override void editString(Component n1, int n2, int n3, string n4, int n5)
        {
            Display d = Display.getInstance();
            if (textInputInstance != null)
            {
                commitEditing();
                d.callSerially(new EditString(n1, n2, n3, n4, n5));
                return;
            }
            if (lockEditing)
            {
                d.callSerially(new EditString(n1, n2, n3, n4, n5));
                return;
            }
            lockEditing = true;
            currentlyEditing = (TextArea)n1;
            using (AutoResetEvent are = new AutoResetEvent(false))
            {
                dispatcher.RunAsync(CoreDispatcherPriority.Normal, () =>
               {
                   int constraints = currentlyEditing.getConstraint();
                   bool isPassword = (constraints & TextArea.PASSWORD) == TextArea.PASSWORD;
                   if (n1.getClientProperty("disableWinPassword") != null)
                   {
                       isPassword = false;
                   }
                   if (isPassword)
                   {
                       textInputInstance = new PasswordBox();
                       ((PasswordBox)textInputInstance).PasswordChanged += textChangedEvent;
                       ((PasswordBox)textInputInstance).Password = n4;
                       ((PasswordBox)textInputInstance).MaxLength = n2;
                   }
                   else
                   {
                       textInputInstance = new TextBox();
                       ((TextBox)textInputInstance).IsTextPredictionEnabled = true;
                       ((TextBox)textInputInstance).TextChanged += textChangedEvent;
                       ((TextBox)textInputInstance).Text = n4;
                       ((TextBox)textInputInstance).AcceptsReturn = !currentlyEditing.isSingleLineTextArea();
                       ((TextBox)textInputInstance).MaxLength = n2;

                       if ((constraints & TextArea.NON_PREDICTIVE) == TextArea.NON_PREDICTIVE)
                       {
                           ((TextBox)textInputInstance).InputScope = new InputScope();
                       }

                       if ((constraints & TextArea.NUMERIC) == TextArea.NUMERIC)
                       {
                           setConstraint((TextBox)textInputInstance, InputScopeNameValue.NumberFullWidth);
                       }
                       else
                       {
                           if ((constraints & TextArea.DECIMAL) == TextArea.DECIMAL)
                           {
                               setConstraint((TextBox)textInputInstance, InputScopeNameValue.Number);
                           }
                           else
                           {
                               if ((constraints & TextArea.EMAILADDR) == TextArea.EMAILADDR)
                               {
                                   setConstraint((TextBox)textInputInstance, InputScopeNameValue.EmailSmtpAddress);
                               }
                               else
                               {
                                   if ((constraints & TextArea.URL) == TextArea.URL)
                                   {
                                       setConstraint((TextBox)textInputInstance, InputScopeNameValue.Url);
                                   }
                                   else
                                   {
                                       if ((constraints & TextArea.PHONENUMBER) == TextArea.PHONENUMBER)
                                       {
                                           setConstraint((TextBox)textInputInstance, InputScopeNameValue.TelephoneNumber);
                                       }
                                   }
                               }
                           }
                       }
                   }
                   cl.Children.Add(textInputInstance);
                   Canvas.SetZIndex(textInputInstance, 50000);
                   textInputInstance.IsEnabled = true;
                   Font fnt = currentlyEditing.getStyle().getFont();
                   NativeFont font = f(fnt.getNativeFont());
                   // workaround forsome weird unspecified margin that appears around the text box
                   Canvas.SetTop(textInputInstance, (currentlyEditing.getAbsoluteY() / scaleFactor));
                   Canvas.SetLeft(textInputInstance, (currentlyEditing.getAbsoluteX() / scaleFactor));
                   textInputInstance.Height = (currentlyEditing.getHeight() / scaleFactor);
                   textInputInstance.Width = (currentlyEditing.getWidth() / scaleFactor);
                   textInputInstance.BorderThickness = new Thickness();
                   textInputInstance.FontSize = (font.font.FontSize / scaleFactor);
                   int h = Convert.ToInt32((textInputInstance.Height - textInputInstance.FontSize) / 3);
                   textInputInstance.Margin = new Thickness();
                   textInputInstance.Padding = new Thickness(10, h, 0, 0);
                   textInputInstance.Clip = null;
                   textInputInstance.Focus(FocusState.Programmatic);
                   are.Set();
               }).AsTask().GetAwaiter();
                are.WaitOne();
            }
            d.invokeAndBlock(new WaitForEdit());
            using (AutoResetEvent are = new AutoResetEvent(false))
            {
                dispatcher.RunAsync(CoreDispatcherPriority.Normal, async () =>
               {
                   cl.Children.Remove(textInputInstance);
                   //wait for textChangedEvent
                    await Task.Delay(TimeSpan.FromMilliseconds(100));
                   textInputInstance = null;
                   // cl.Focus;
               }).AsTask().ConfigureAwait(false).GetAwaiter().GetResult();
            }
            lockEditing = false;
        }

        void textChangedEvent(object sender, RoutedEventArgs e)
        {
            if (textInputInstance == null)
            {
                Debug.WriteLine("[textChangeEvent] textInput is null. Shoud not happen!!");
                return;
            }
            Display disp = Display.getInstance();
            Tchange t = new Tchange();
            t.currentlyEditing = currentlyEditing;
            if (textInputInstance is TextBox)
            {
                t.text = ((TextBox)textInputInstance).Text;
            }
            else {
                t.text = ((PasswordBox)textInputInstance).Password;
            }
            disp.callSerially(t);
        }

        public override bool hasPendingPaints()
        {
            //if the view is not visible make sure the edt won't wait.
            return base.hasPendingPaints();
        }
      
        public override void repaint(Animation cmp)
        {
            if (myView != null)
            {
                if (cmp is Component)
                {
                    Component c = (Component)cmp;
                    c.setDirtyRegion(null);
                    if (c.getParent() != null)
                    {
                        cmp = c.getComponentForm();
                    }
                    else
                    {
                        Form f = getCurrentForm();
                        if (f != null)
                        {
                            cmp = f;
                        }
                    }
                }
                else
                {
                    // make sure the form is repainted for standalone anims e.g. in the case
                    // of replace animation
                    Form f = getCurrentForm();
                    if (f != null)
                    {
                        base.repaint(f);
                    }
                }
            }
            cmp.animate();
            base.repaint(cmp);
        }

        public override void setCurrentForm(Form n1)
        {
            if (getCurrentForm() == null)
            {
                flushGraphics();
            }
            base.setCurrentForm(n1);
        }

        public override void flushGraphics(int x, int y, int width, int height)
        {
            if (width <= 0 || height <= 0) return;
            Rectangle rect = new Rectangle(x, y, width, height);
            myView.flushGraphics(rect);
        }

        public override void flushGraphics()
        {
            myView.flushGraphics();
        }

        public override void systemOut(string n1)
        {
            Debug.WriteLine(n1);
        }

        public override bool isOpaque(ui.Image n1, object n2)
        {
            return ((CodenameOneImage)n1.getImage()).opaque;
        }

        public override void getRGB(object img, int[] arr, int offset, int x, int y, int w, int h)
        {
            CodenameOneImage cn = (CodenameOneImage)img;
            byte[] buffer = cn.image.GetPixelBytes(x, y, w, h);
            System.Buffer.BlockCopy(buffer, 0, arr, 0, buffer.Length);
        }

        public override void setImageName(object n1, string n2)
        {
            if (n2 != null)
            {
                ((CodenameOneImage)n1).name = n2;
            }
        }

        public override void clipRect(object graphics, Rectangle rect)
        {
            base.clipRect(graphics, rect);
        }

        public override object rotate(object img, int degrees)
        {
            CodenameOneImage cn = (CodenameOneImage)img;

            CanvasRenderTarget cr = new CanvasRenderTarget(screen, (float)cn.image.Size.Width, (float)cn.image.Size.Height, cn.image.Dpi);
            using (var ds = cr.CreateDrawingSession())
            {
                float angle = (float)Math.PI * degrees / 180;
                ds.Transform = Matrix3x2.CreateRotation(angle, new Vector2((float)cn.image.Size.Width / 2, (float)cn.image.Size.Height / 2));
                ds.DrawImage(cn.image);
                ds.Dispose();
            }
            CodenameOneImage ci = new CodenameOneImage();
            ci.image = cr;
            return ci;
        }


        public static Matrix3x2 clamp(Matrix3x2 m)
        {
            if (m.IsIdentity)
            {
                return m;
            }
            m.M11 = clamp(m.M11);
            m.M12 = clamp(m.M12);
            m.M21 = clamp(m.M21);
            m.M22 = clamp(m.M22);
            m.M31 = clamp(m.M31);
            m.M32 = clamp(m.M32);
            return m;

        }

        public static double clamp(double d)
        {
            if (d > -0.0001 && d < 0.0001)
            {
                return 0;
            } else if (d > 0.9999 && d < 1.0001)
            {
                return 1;
            }
            return d;
        }

        public static float clamp(float d)
        {
            if (d > -0.0001 && d < 0.0001)
            {
                return 0;
            }
            else if (d > 0.9999 && d < 1.0001)
            {
                return 1;
            }
            return d;
        }

        public override object makeTransformIdentity()
        {
            return new NativeTransform(Matrix3x2.Identity);
        }



        public override object makeTransformTranslation(float translateX, float translateY, float translateZ)
        {
            return new NativeTransform(Matrix3x2.CreateTranslation(new Vector2(translateX, translateY)));
        }

        public override object makeTransformScale(float scaleX, float scaleY, float scaleZ)
        {
            return new NativeTransform(Matrix3x2.CreateScale(new Vector2(scaleX, scaleY)));
        }

        public override object makeTransformRotation(float angle, float x, float y, float z)
        {
            return new NativeTransform(Matrix3x2.CreateRotation(angle, new Vector2(x, y)));
        }

        public override object makeTransformInverse(object nativeTransform)
        {
            NativeTransform nt = (NativeTransform)nativeTransform;
            Matrix3x2 t = nt.m;
            Matrix3x2 inv;
            Matrix3x2.Invert(t, out inv);
            return new NativeTransform(inv);
        }

        public override object makeTransformPerspective(float fovy, float aspect, float zNear, float zFar)
        {
            //return Matrix4x4.CreatePerspectiveFieldOfView(fovy, aspect, zNear, zFar);
            return base.makeTransformPerspective(fovy, aspect, zNear, zFar);
        }

        public override object makeTransformOrtho(float left, float right, float bottom, float top, float near, float far)
        {
            //return Matrix4x4.CreateOrthographicOffCenter(left, right, bottom, top, near, far);
            return base.makeTransformOrtho(left, right, bottom, top, near, far);
        }

        public override object makeTransformCamera(float eyeX, float eyeY, float eyeZ, float centerX, float centerY, float centerZ, float upX, float upY, float upZ)
        {
            //return Matrix4x4.CreateLookAt(new Vector3(eyeX, eyeY, eyeZ), new Vector3(centerX, centerY, centerZ), new Vector3(upX, upY, upZ));
            return base.makeTransformCamera(eyeX, eyeY, eyeZ, centerX, centerY, centerZ, upX, upY, upZ);
        }

        private void copyMatrix4x4(Matrix4x4 src, Matrix4x4 dest)
        {
            dest.M11 = src.M11;
            dest.M12 = src.M12;
            dest.M13 = src.M13;
            dest.M14 = src.M14;
            dest.M21 = src.M21;
            dest.M22 = src.M22;
            dest.M23 = src.M23;
            dest.M24 = src.M24;
            dest.M31 = src.M31;
            dest.M32 = src.M32;
            dest.M33 = src.M33;
            dest.M34 = src.M34;
            dest.M41 = src.M41;
            dest.M42 = src.M42;
            dest.M43 = src.M43;
            dest.M44 = src.M44;
        }

        private void copyMatrix3x2(Matrix3x2 src, Matrix3x2 dest)
        {
            dest.M11 = src.M11;
            dest.M12 = src.M12;
            //dest.M13 = src.M13;
            //dest.M14 = src.M14;
            dest.M21 = src.M21;
            dest.M22 = src.M22;
            //dest.M23 = src.M23;
            //dest.M24 = src.M24;
            dest.M31 = src.M31;
            dest.M32 = src.M32;
            
            //dest.M33 = src.M33;
            //dest.M34 = src.M34;
            //dest.M41 = src.M41;
            //dest.M42 = src.M42;
            //dest.M43 = src.M43;
            //dest.M44 = src.M44;
        }

        public override void concatenateTransform(object t1, object t2)
        {
            NativeTransform m1 = (NativeTransform)t1;
            NativeTransform m2 = (NativeTransform)t2;
            Matrix3x2 res = Matrix3x2.Multiply(m2.m, m1.m);
            m1.m = res;
        }

        public override void copyTransform(object src, object dest)
        {
            NativeTransform m1 = (NativeTransform)src;
            NativeTransform m2 = (NativeTransform)dest;
            m2.m = m1.m;
        }

        public override bool isPerspectiveTransformSupported()
        {
            return false;
        }

        public override bool transformEqualsImpl(ui.Transform t1, ui.Transform t2)
        {
            NativeTransform m1 = (NativeTransform)t1.getNativeTransform();
            NativeTransform m2 = (NativeTransform)t2.getNativeTransform();
            return m1.m.Equals(m2.m);
        }

        

        protected override bool cacheLinearGradients()
        {
            return false;
        }

        protected override bool cacheRadialGradients()
        {
            return false;
        }

        public override void fillLinearGradient(object graphics, int startColor, int endColor, int x, int y, int width, int height, bool horizontal)
        {
            ((NativeGraphics)graphics).destination.fillLinearGradient(startColor, endColor, x, y, width, height, horizontal);
        }

        public override void fillRadialGradient(object graphics, int startColor, int endColor, int x, int y, int width, int height)
        {
            ((NativeGraphics)graphics).destination.fillRadialGradient(endColor, startColor, x, y, width, height); // win2d start and end color are inverted
        }

        public override void drawImageArea(object nativeGraphics, object img, int x, int y, int imageX, int imageY, int imageWidth, int imageHeight)
        {  
          base.drawImageArea(nativeGraphics, img, x, y, imageX, imageY, imageWidth, imageHeight);
        }

        public override void fillRectRadialGradient(object graphics, int startColor, int endColor, int x, int y, int width, int height, float relativeX, float relativeY, float relativeSize)
        {
            int centerX = (int)(width * (1 - relativeX));
            int centerY = (int)(height * (1 - relativeY));
            int size = (int)(Math.Min(width, height) * relativeSize);
            int x2 = (int)(width / 2 - (size * relativeX));
            int y2 = (int)(height / 2 - (size * relativeY));
            ((NativeGraphics)graphics).destination.fillRadialGradient(endColor, startColor, x + x2, y + y2, size, size); // win2d start and end color are inverted
        }
       
        public override void releaseImage(object n1)
        {
            ((CodenameOneImage)n1).image.Dispose();
        }
      
        public override int convertToPixels(int mm, bool horizontal)
        {
            // 55.5mm ~ 400dip
            // return screen.ConvertDipsToPixels(mm * 7.207f, CanvasDpiRounding.Round);
            if (horizontal != true) return Convert.ToInt32((mm * rawDpiy) / 25.4);
            return Convert.ToInt32((mm * rawDpix) / 25.4);
        }

        public override void fillTriangle(object graphics, int x1, int y1, int x2, int y2, int x3, int y3)
        {
            ((NativeGraphics)graphics).destination.fillPolygon(new int[] { x1, x2, x3 }, new int[] { y1, y2, y3 });
        }

        public override void fillPolygon(object graphics, int[] xPoints, int[] yPoints, int nPoints)
        {
            ((NativeGraphics)graphics).destination.fillPolygon(xPoints, yPoints);
        }

        public override ui.Image flipImageHorizontally(ui.Image image, bool maintainOpacity)
        {
            return base.flipImageHorizontally(image, maintainOpacity);
        }

        public override ui.Image flipImageVertically(ui.Image image, bool maintainOpacity)
        {
            return base.flipImageVertically(image, maintainOpacity);
        }

        public override bool isTransformSupported()
        {
            return true;
        }

        public override bool isTransformSupported(object n1)
        {
            return true;
        }

        public override bool isTranslationSupported()
        {
            return base.isTranslationSupported();
        }


        public override bool isAffineSupported()
        {
            return true;

        }

        public override bool isPerspectiveTransformSupported(object graphics)
        {
            return false;
        }

        public override bool isShapeSupported(object graphics)
        {
            return true;
        }

        public override void rotate(object nativeGraphics, float angle)
        {
            WindowsGraphics g = (WindowsGraphics)((NativeGraphics)nativeGraphics).destination;
            ui.Transform t = g.getTransform();
            t.rotate(angle, 0, 0);
            g.setTransform(t);
            
        }

        public override void rotate(object nativeGraphics, float angle, int x, int y)
        {
            WindowsGraphics g = (WindowsGraphics)((NativeGraphics)nativeGraphics).destination;
            ui.Transform t = g.getTransform();
            t.rotate(angle, x, y);
            g.setTransform(t);
        }

        public override void resetAffine(object nativeGraphics)
        {
            WindowsGraphics g = ((NativeGraphics)nativeGraphics).destination;
            g.setTransform(com.codename1.ui.Transform.makeTranslation(0, 0, 0));
        }

        public override ui.Transform getTransform(object graphics)
        {
            return ((WindowsGraphics)((NativeGraphics)graphics).destination).getTransform();
        }

        public override void getTransform(object nativeGraphics, ui.Transform t)
        {
            t.setTransform(((WindowsGraphics)((NativeGraphics)nativeGraphics).destination).getTransform());
        }

        public class NativeTransform
        {

            public NativeTransform(Matrix3x2 m)
            {
                this.m = m;
            }
            public Matrix3x2 m;
        }

        public override void transformPoint(object nativeTransform, float[] @in, float[] @out)
        {
            NativeTransform nt = (NativeTransform)nativeTransform;
            Matrix3x2 t = nt.m;
            Vector2 res = Vector2.Transform(new Vector2(@in[0], @in[1]), t);
            @out[0] = res.X;
            @out[1] = res.Y;
            @out[2] = 0;
        }

        

        public override ui.Image rotate180Degrees(ui.Image image, bool maintainOpacity)
        {
            return base.rotate180Degrees(image, maintainOpacity);
        }

        public override ui.Image rotate270Degrees(ui.Image image, bool maintainOpacity)
        {
            return base.rotate270Degrees(image, maintainOpacity);
        }

        public override ui.Image rotate90Degrees(ui.Image image, bool maintainOpacity)
        {
            return base.rotate90Degrees(image, maintainOpacity);
        }

        public override void scale(object nativeGraphics, float x, float y)
        {
            WindowsGraphics g = (WindowsGraphics)((NativeGraphics)nativeGraphics).destination;
            ui.Transform t = g.getTransform();
            t.scale(x, y);
            g.setTransform(t);
            setTransform(nativeGraphics, t);
        }

        public override void setTransform(object n1, ui.Transform n2)
        {
            WindowsGraphics g = (WindowsGraphics)((NativeGraphics)n1).destination;
            g.setTransform(n2);
        }

        public override void translate(object n1, int n2, int n3)
        {
            base.translate(n1, n2, n3);
        }

        public override bool isRotationDrawingSupported()
        {
            return false;
        }
       
        

        public override object createImage(int[] n1, int n2, int n3)
        {
            CodenameOneImage ci = (CodenameOneImage)createMutableImage(n2, n3, 0);
            byte[] buf = new byte[n1.Length * 4];
            System.Buffer.BlockCopy(n1, 0, buf, 0, buf.Length);
            CanvasBitmap cb = CanvasBitmap.CreateFromBytes(screen, buf, n2, n3, pixelFormat);
            ci.graphics.destination.drawImage(cb, 0, 0);
            return ci;
        }

        public override object createImage(string path)
        {
            if (path.StartsWith("file:") || path.StartsWith("tmp://"))
            {
                return createImage(openFileInputStream(path));
            }
            java.io.InputStream s = getResourceAsStream(null, path);
            return createImage(s);
        }

        public override object createImage(java.io.InputStream n1)
        {
            byte[] b = io.Util.readInputStream(n1);
            return createImage(b, 0, b.Length);
        }

        public static byte[] toByteArray(sbyte[] byteArray)
        {
            byte[] sbyteArray = null;
            if (byteArray != null)
            {
                sbyteArray = new byte[byteArray.Length];
                System.Buffer.BlockCopy(byteArray, 0, sbyteArray, 0, byteArray.Length);
            }
            return sbyteArray;
        }

        const int maxCacheSize = 50;
        private static ConcurrentDictionary<int, CodenameOneImage> imageCache = new ConcurrentDictionary<int, CodenameOneImage>();

        public override object createImage(byte[] bytes, int offset, int len)
        {

            if (imageCache.ContainsKey(bytes.GetHashCode()))
            {
                CodenameOneImage cached;
                imageCache.TryGetValue(bytes.GetHashCode(), out cached);
                cached.lastAccess = DateTime.Now.Ticks;
                return cached;
            }
            if (bytes.Length == 0)
            {
                // workaround for empty images
                return createMutableImage(1, 1, 0xffffff);
            }
            CodenameOneImage ci = null;
            string contentType;
            CanvasBitmap canvasbitmap;
            byte[] imageArray = bytes;
            contentType = ImageHelper.GetContentType(imageArray);
            using (InMemoryRandomAccessStream s = new InMemoryRandomAccessStream())
            {              
                using (DataWriter writer = new DataWriter(s.GetOutputStreamAt((ulong)offset)))
                {
                    writer.WriteBytes(imageArray);
                    writer.StoreAsync().AsTask().GetAwaiter().GetResult();
                    writer.FlushAsync().AsTask().GetAwaiter().GetResult();
                    writer.DetachStream().AsStreamForWrite();                 
                    canvasbitmap = CanvasBitmap.LoadAsync(screen, s).AsTask().GetAwaiter().GetResult();    
                }
            }
        
            try
            {
                CodenameOneImage cim = new CodenameOneImage();
                if (contentType.Equals("image/jpeg") || contentType.Equals("image/x-ms-bmp"))
                {
                    cim.opaque = true;
                }
                CanvasRenderTarget cr = new CanvasRenderTarget(screen, float.Parse(canvasbitmap.Size.Width.ToString()), float.Parse(canvasbitmap.Size.Height.ToString()), canvasbitmap.Dpi);
                cim.image = cr;
                cim.graphics.destination.drawImage(canvasbitmap, 0, 0);
                cim.graphics.destination.dispose();
                ci = cim;
                canvasbitmap.Dispose();
                dispatcher.RunAsync(CoreDispatcherPriority.Low, () =>
                {
                    imageCache.TryAdd(bytes.GetHashCode(), ci);
                    while (imageCache.Count > maxCacheSize)
                    {
                        int toRemove = imageCache.OrderBy(m => m.Value.lastAccess).First().Key;
                        CodenameOneImage ignored;
                        imageCache.TryRemove(toRemove, out ignored);
                    }
                }).AsTask();
            }
            catch (Exception)
            {
                Debug.WriteLine("\n Failed to create image " + bytes.GetHashCode() + "\n Position: " + offset + "\n Size: " + len);
            }

            return ci;
        }

        public override void tileImage(object graphics, object image, int x, int y, int w, int h)
        {
            CodenameOneImage img = (CodenameOneImage)image;
            img.lastAccess = DateTime.Now.Ticks;
            NativeGraphics ng = (NativeGraphics)graphics;
            ng.destination.tileImage(img.image, x, y, w, h);
        }

        public static bool exitLock;
        private ActionListener pendingCaptureCallback;
        public override void capturePhoto(ActionListener response)
        {
            capture(response, CameraCaptureUIMode.Photo);
        }

        public override void captureVideo(ActionListener response)
        {
            capture(response, CameraCaptureUIMode.Video);
        }

        private void capture(ActionListener response, CameraCaptureUIMode mode)
        {
#if WINDOWS_PHONE_APP
            openGaleriaCamera();
#else
            exitLock = true;
            pendingCaptureCallback = response;
            dispatcher.RunAsync(CoreDispatcherPriority.Normal, async () =>
            {
                try
                {
                    // Using Windows.Media.Capture.CameraCaptureUI API to capture a photo
                    CameraCaptureUI dialog = new CameraCaptureUI();
                    if (mode == CameraCaptureUIMode.Photo)
                    {
                        dialog.PhotoSettings.CroppedAspectRatio = new Size(16, 9);
                        dialog.PhotoSettings.Format = CameraCaptureUIPhotoFormat.Jpeg;
                        dialog.PhotoSettings.MaxResolution = CameraCaptureUIMaxPhotoResolution.HighestAvailable;
                    }
                    
                    else if (mode == CameraCaptureUIMode.Video)
                    {
                        
                        dialog.VideoSettings.Format = CameraCaptureUIVideoFormat.Mp4;
                        dialog.VideoSettings.MaxResolution = CameraCaptureUIMaxVideoResolution.HighestAvailable;
                    }
                    
                    StorageFile photo = await dialog.CaptureFileAsync(mode);
                    if (photo != null)
                    {
                        string tmpPath = addTempFile(photo);
                        fireCapture(new ActionEvent(tmpPath));
                        //var foldersave = KnownFolders.CameraRoll;
                        //await photo.MoveAsync(foldersave);

                        //BitmapImage bitmapImage = new BitmapImage();
                        //Windows.UI.Xaml.Controls.Image Capturedphoto = new Windows.UI.Xaml.Controls.Image();
                        //Capturedphoto.Source = bitmapImage;
                        //toSendPhoto(photo);
                    } else
                    {
                        fireCapture(new ActionEvent(null));
                    }
                }
                catch (Exception)
                {
                   
                }
            }).AsTask().ConfigureAwait(false).GetAwaiter().GetResult();
#endif
        }

        public async void toSendPhoto(StorageFile photo)
        {
            try
            {
                fileName = photo.Name;
#if WINDOWS_PHONE_APP
              
                ActionEvent ac = new ActionEvent("cameraroll:/" + fileName);
                fireCapture(ac);
#else
                StorageFolder folder = await photo.GetParentAsync();
                string folderName = folder.Name.ToLower();
                folderName = folderName.Replace(" ", "");
                ActionEvent ac = new ActionEvent("file:/"+folderName + ":/" + fileName);
                fireCapture(ac);
#endif
            }
            catch (Exception )
            {

            }

        }

        private List<StorageFile> tmpFiles = new List<StorageFile>();
        private string addTempFile(StorageFile f)
        {
            int index = tmpFiles.Count;
            tmpFiles.Insert(index, f);
            return "tmp://" + index + "-" + f.Name;
        }

        private bool isTempFile(string path)
        {
            return path.StartsWith("tmp://");
        }

        private StorageFile getTempFile(string path)
        {
            if (!path.StartsWith("tmp://"))
            {
                return null;
            }
            path = path.Substring(6);
            if (path.IndexOf("-") < 0)
            {
                return null;
            }
            int index = Convert.ToInt32(path.Substring(0, path.IndexOf("-")));
            if (index < 0 || index >= tmpFiles.Count)
            {
                return null;
            }
            return tmpFiles.ElementAt(index);
        }

        private void removeTempFile(string path)
        {
            StorageFile f = getTempFile(path);
            if (f != null)
            {
                tmpFiles.Remove(f);
            }
        }

        public async void toSendFile(StorageFile file)
        {
            try
            {
                fileName = file.Name;
#if WINDOWS_PHONE_APP
              
                ActionEvent ac = new ActionEvent("cameraroll:/" + fileName);
                fireCapture(ac);
#else
                //tmpFiles.Add(file);
                string tmpPath = addTempFile(file);
                //StorageFolder folder = await file.GetParentAsync();
                //string folderName = folder.Name.ToLower();
                //folderName = folderName.Replace(" ", "");
                ActionEvent ac = new ActionEvent(tmpPath);
                fireCapture(ac);
#endif
            }
            catch (Exception)
            {

            }

        }


        public override void openGallery(ActionListener response, int type)
        {
            exitLock = true;
            pendingCaptureCallback = response;
            dispatcher.RunAsync(CoreDispatcherPriority.Normal, async () =>
            {
                FileOpenPicker openPicker = new FileOpenPicker();
                openPicker.ViewMode = PickerViewMode.Thumbnail;
                openPicker.SuggestedStartLocation = PickerLocationId.PicturesLibrary;
                if (type == Display.GALLERY_IMAGE)
                {
                    openPicker.ViewMode = PickerViewMode.Thumbnail;
                    openPicker.SuggestedStartLocation = PickerLocationId.PicturesLibrary;
                    openPicker.FileTypeFilter.Add(".jpg");
                    openPicker.FileTypeFilter.Add(".jpeg");
                    openPicker.FileTypeFilter.Add(".png");
                } else if (type == Display.GALLERY_VIDEO)
                {
                    openPicker.FileTypeFilter.Add(".mp4");
                    openPicker.FileTypeFilter.Add(".avi");
                    openPicker.FileTypeFilter.Add(".mpg");
                    openPicker.FileTypeFilter.Add(".mov");
                    openPicker.FileTypeFilter.Add(".mpeg");
                    openPicker.FileTypeFilter.Add(".wmv");
                } else
                {
                    openPicker.FileTypeFilter.Add("*");
                }
#if WINDOWS_PHONE_APP
                openPicker.PickSingleFileAndContinue();
                view.Activated += view_Activated;
#else
                StorageFile file = await openPicker.PickSingleFileAsync();
                if (file != null)
                {
                    toSendFile(file);
                }
#endif
            }).AsTask().ConfigureAwait(false).GetAwaiter().GetResult();
            //base.openGallery(response, type);
        }

        public override void openImageGallery(ActionListener response)
        {
            exitLock = true;
            pendingCaptureCallback = response;
            openGaleriaCamera();
        }

        private void openGaleriaCamera()
        {
            dispatcher.RunAsync(CoreDispatcherPriority.Normal, async () =>
            {
                FileOpenPicker openPicker = new FileOpenPicker();
                openPicker.ViewMode = PickerViewMode.Thumbnail;
                openPicker.SuggestedStartLocation = PickerLocationId.PicturesLibrary;
                openPicker.FileTypeFilter.Add(".jpg");
                openPicker.FileTypeFilter.Add(".jpeg");
                openPicker.FileTypeFilter.Add(".png");
#if WINDOWS_PHONE_APP
                openPicker.PickSingleFileAndContinue();
                view.Activated += view_Activated;
#else
                StorageFile photo = await openPicker.PickSingleFileAsync();
                if (photo != null)
                {
                    toSendPhoto(photo);
                }
#endif
            }).AsTask().ConfigureAwait(false).GetAwaiter().GetResult();

        }
#if WINDOWS_PHONE_APP
        void view_Activated(CoreApplicationView sender, IActivatedEventArgs args)
        {

            var filePickerContinuationArgs = args as FileOpenPickerContinuationEventArgs;
            if (filePickerContinuationArgs != null)
            {
                this.FilePickerContinuationArgs = filePickerContinuationArgs;
                this.ContinueFileOpenPicker(FilePickerContinuationArgs);
            }
            else
            {
                return;
            }

        }
        public void ContinueFileOpenPicker(FileOpenPickerContinuationEventArgs args)
        {
            if (args != null)
            {
                if (args.Files.Count == 0) return;
                view.Activated -= view_Activated;
                fileName = args.Files[0].Name;
                com.codename1.ui.events.ActionEvent ac = new com.codename1.ui.events.ActionEvent("cameraroll:/" + fileName);
                fireCapture(ac);
            }
        }
#endif
        private void fireCapture(ActionEvent ev)
        {
            ui.util.EventDispatcher ed = new ui.util.EventDispatcher();
            ed.addListener(pendingCaptureCallback);
            ed.fireActionEvent(ev);
            pendingCaptureCallback = null;
            exitLock = false;
        }

        public override bool isNativeBrowserComponentSupported()
        {
            return true;
        }

        BrowserComponent currentBrowser;
        public WebView webView;
        public override PeerComponent createBrowserComponent(object n1)
        {
            SilverlightPeer sp = null;
            using (AutoResetEvent are = new AutoResetEvent(false))
            {
                dispatcher.RunAsync(CoreDispatcherPriority.Normal, () =>
                {
                    webView = new WebView();
                    currentBrowser = (BrowserComponent)n1;
                    webView.DOMContentLoaded += webview_DOMContentLoaded;
                    webView.NavigationStarting += webview_Navigating;
                    webView.ContentLoading += webview_ContentLoading;
                    webView.IsTapEnabled = true;
                    webView.NavigationCompleted += webview_NavigationCompleted;
                    sp = new SilverlightPeer(webView);
                    are.Set();
                }).AsTask().GetAwaiter().GetResult();
                are.WaitOne();
            }
            return sp;
        }

        void webview_DOMContentLoaded(WebView sender, WebViewDOMContentLoadedEventArgs e)
        {
            BrowserNavigationCallback bn = currentBrowser.getBrowserNavigationCallback();
            ActionEvent ev = new ActionEvent(e.Uri.OriginalString);
            currentBrowser.fireWebEvent("onLoadResource", ev);

        }

        void webview_NavigationCompleted(WebView sender, WebViewNavigationCompletedEventArgs e)
        {
            BrowserNavigationCallback bn = currentBrowser.getBrowserNavigationCallback();
           
            if (e.IsSuccess == false)
            {
                ActionEvent ev = new ActionEvent(e.Uri.OriginalString);
                currentBrowser.fireWebEvent("onError", ev);
            }
        }

        void webview_ContentLoading(WebView sender, WebViewContentLoadingEventArgs e)
        {
            BrowserNavigationCallback bn = currentBrowser.getBrowserNavigationCallback();
            ActionEvent ev = new ActionEvent(e.Uri.OriginalString);
            currentBrowser.fireWebEvent("onLoad", ev);
        }

        void webview_Navigating(WebView sender, WebViewNavigationStartingEventArgs e)
        {
            BrowserNavigationCallback bn = currentBrowser.getBrowserNavigationCallback();
            if (!bn.shouldNavigate(e.Uri.ToString()))
            {
                e.Cancel = true;
            }
            ActionEvent ev = new ActionEvent(e.Uri.OriginalString);
            currentBrowser.fireWebEvent("onStart", ev);
        }

        public override string getBrowserTitle(PeerComponent n1)
        {
            string st = null;
            using (AutoResetEvent are = new AutoResetEvent(false))
            {
                dispatcher.RunAsync(CoreDispatcherPriority.Normal, async () =>
                {
                    webView = (WebView)((SilverlightPeer)n1).element;
                    st = await webView.InvokeScriptAsync("eval", new string[] { "document.title.toString()" });
                    are.Set();
                }).AsTask().GetAwaiter().GetResult();
                are.WaitOne();
            }
            return st;
        }

        public override string getBrowserURL(PeerComponent n1)
        {
            dispatcher.RunAsync(CoreDispatcherPriority.Normal, () =>
           {
            webView = (WebView)((SilverlightPeer)n1).element;
           
           }).AsTask().GetAwaiter().GetResult();
            return webView.Source.OriginalString;
        }

        public override void setBrowserURL(PeerComponent browserPeer, string url)
        {
            dispatcher.RunAsync(CoreDispatcherPriority.Normal, () =>
           {
               webView = (WebView)((SilverlightPeer)browserPeer).element;
               string uri = url;
               if (uri.StartsWith("jar:/"))
               {
                   uri = uri.Substring(5);
                   while (uri[0] == '/')
                   {
                       uri = uri.Substring(1);
                   }
                   uri = "res/" + uri;
                   webView.Source = new Uri(uri, UriKind.Relative);
                   return;
               }
               webView.Source = new Uri(uri);
           }).AsTask().GetAwaiter().GetResult();
        }
 
        public override void browserReload(PeerComponent n1)
        {
            dispatcher.RunAsync(CoreDispatcherPriority.Normal, () =>
            {
                webView = (WebView)((SilverlightPeer)n1).element;
                webView.Source = webView.Source;
            }).AsTask().GetAwaiter().GetResult();
        }

        public override bool browserHasBack(PeerComponent n1)
        {
            bool ret = false;
            using (AutoResetEvent are = new AutoResetEvent(false))
            {
                dispatcher.RunAsync(CoreDispatcherPriority.Normal, () =>
               {
                   webView = (WebView)((SilverlightPeer)n1).element;
                   ret = webView.CanGoBack;
                   are.Set();
               }).AsTask().GetAwaiter().GetResult();
                are.WaitOne();
            }
            return ret;
        }

        public override bool browserHasForward(PeerComponent n1)
        {
            bool ret = false;
            using (AutoResetEvent are = new AutoResetEvent(false))
            {
                dispatcher.RunAsync(CoreDispatcherPriority.Normal, () =>
                 {
                     webView = (WebView)((SilverlightPeer)n1).element;
                     ret = webView.CanGoForward;
                     are.Set();
                 }).AsTask().GetAwaiter().GetResult();
                are.WaitOne();
            }
            return ret;
        }

        public override void browserBack(PeerComponent n1)
        {
            dispatcher.RunAsync(CoreDispatcherPriority.Normal, () =>
            {
                webView = (WebView)((SilverlightPeer)n1).element;
                webView.GoBack();
            }).AsTask().GetAwaiter().GetResult();
        }

        public override void browserStop(PeerComponent n1)
        {
          dispatcher.RunAsync(CoreDispatcherPriority.Normal, () =>
         {
             webView = (WebView)((SilverlightPeer)n1).element;
             webView.Stop();
         }).AsTask().GetAwaiter().GetResult();
        }

        public override void browserForward(PeerComponent n1)
        {
              dispatcher.RunAsync(CoreDispatcherPriority.Normal, () =>
         {
            webView = (WebView)((SilverlightPeer)n1).element;
            webView.GoForward();
         }).AsTask().GetAwaiter().GetResult();
        }

        public override void setBrowserPage(PeerComponent n1, string n2, string n3)
        {
            dispatcher.RunAsync(CoreDispatcherPriority.Normal, () =>
           {
               webView = (WebView)((SilverlightPeer)n1).element;
               webView.NavigateToString(n2);
           }).AsTask().GetAwaiter();
        }

        public override void browserDestroy(PeerComponent n1)
        {
            dispatcher.RunAsync(CoreDispatcherPriority.Normal, () =>
            {
                webView = (WebView)((SilverlightPeer)n1).element;
                webView.Visibility = Visibility.Collapsed;
            }).AsTask().GetAwaiter();
        }

        public override void execute(string n1) {
            var uri = new Uri(n1, UriKind.RelativeOrAbsolute);
            using (AutoResetEvent are = new AutoResetEvent(false)) {
                dispatcher.RunAsync(CoreDispatcherPriority.Normal, () => {
                    var result = Windows.System.Launcher.LaunchUriAsync(uri);
                    are.Set();
                }).AsTask().GetAwaiter().GetResult();
                are.WaitOne();
            }
        }

        /**
        * Returns true if the underlying OS supports opening the native navigation
        * application
        * @return true if the underlying OS supports launch of native navigation app
        */
        public override bool isOpenNativeNavigationAppSupported(){
            return true;
        }

       /**
        * Opens the native navigation app in the given coordinate.
        * @param latitude 
        * @param longitude 
        */ 
        public override void openNativeNavigationApp(double latitude, double longitude){    
            using (AutoResetEvent are = new AutoResetEvent(false)) {

                dispatcher.RunAsync(CoreDispatcherPriority.Normal, () => {
                    var uri = new Uri(@"bingmaps:?cp="+latitude+"~"+longitude);

                    // Launch the Windows Maps app
                    var launcherOptions = new Windows.System.LauncherOptions();
                    launcherOptions.TargetApplicationPackageFamilyName = "Microsoft.WindowsMaps_8wekyb3d8bbwe";
                    var result = Windows.System.Launcher.LaunchUriAsync(uri, launcherOptions);
                    are.Set();
                }).AsTask().GetAwaiter().GetResult();
                are.WaitOne();
            }
        }
        
        public override void browserExecute(PeerComponent n1, string n2)
        {
            dispatcher.RunAsync(CoreDispatcherPriority.Normal, async () =>
            {
                webView = (WebView)((SilverlightPeer)n1).element;
                await webView.InvokeScriptAsync(n2, new string[] { "document.title.toString()" });
            }).AsTask().GetAwaiter();
        }

        public override string browserExecuteAndReturnString(PeerComponent n1, string n2)
        {
            string st = null;
            using (AutoResetEvent are = new AutoResetEvent(false))
            {
                dispatcher.RunAsync(CoreDispatcherPriority.Normal, async () =>
                {
                    webView = (WebView)((SilverlightPeer)n1).element;
                    st = await webView.InvokeScriptAsync(n2, new string[] { "document.title.toString()" });
                    are.Set();
                }).AsTask().GetAwaiter().GetResult();
                are.WaitOne();
            }
            return st;
        }

        private Purchase pur;
        private WindowsPurchase windPur;

        public override Purchase getInAppPurchase()
        {
            windPur = new WindowsPurchase(screen);
            try
            {
                pur = windPur;
                return pur;
            }
            catch (Exception)
            {
                return base.getInAppPurchase();
            }
        }

        public override void sendMessage(string[] n1, string n2, messaging.Message n3)
        {

            string subject = n2;
            var contactPicker = new ContactPicker();
            contactPicker.DesiredFieldsWithContactFieldType.Add(ContactFieldType.Email);
            Contact recipient = contactPicker.PickContactAsync().AsTask().ConfigureAwait(false).GetAwaiter().GetResult();
            if (recipient != null)
            {
                IList<ContactEmail> fields = recipient.Emails;

                if (fields.Count > 0)
                {
                    if (fields[0].GetType() == typeof(ContactEmail))
                    {
                        foreach (ContactEmail email in fields as IList<ContactEmail>)
                        {
#if WINDOWS_PHONE_APP
                            Windows.ApplicationModel.Email.EmailMessage emailMessage = new Windows.ApplicationModel.Email.EmailMessage();
                             emailMessage.Body = n3.getContent();
                            emailMessage.Subject = subject;
                            var emailRecipient = new Windows.ApplicationModel.Email.EmailRecipient(email.Address);
                            emailMessage.To.Add(emailRecipient);
                            Windows.ApplicationModel.Email.EmailManager.ShowComposeNewEmailAsync(emailMessage).AsTask().ConfigureAwait(false).GetAwaiter().GetResult();
                            break;
#elif WINDOWS_UWP
                            Windows.ApplicationModel.Email.EmailMessage emailMessage = new Windows.ApplicationModel.Email.EmailMessage();
                            emailMessage.Body = n3.getContent(); 
                            emailMessage.Subject = subject;
                            var emailRecipient = new Windows.ApplicationModel.Email.EmailRecipient(email.Address);
                            emailMessage.To.Add(emailRecipient);
                            Windows.ApplicationModel.Email.EmailManager.ShowComposeNewEmailAsync(emailMessage).AsTask().ConfigureAwait(false).GetAwaiter().GetResult();
                            break;
#endif
                        }
                    }
                }
                else
                {
                    Debug.WriteLine("No recipient emailid Contact found");
                }
            }
            else
            {
                Debug.WriteLine("No recipient emailid Contact found");
            }
        }

        public override void sendSMS(string n1, string n2, bool n)
        {
#if WINDOWS_PHONE_APP
            var chatMessage = new Windows.ApplicationModel.Chat.ChatMessage();
            chatMessage.Body = n2;
            chatMessage.Recipients.Add(n1);
            Windows.ApplicationModel.Chat.ChatMessageManager.ShowComposeSmsMessageAsync(chatMessage).AsTask().ConfigureAwait(false).GetAwaiter().GetResult();
#elif WINDOWS_UWP
             var chatMessage = new Windows.ApplicationModel.Chat.ChatMessage();
            chatMessage.Body = n2;
            chatMessage.Recipients.Add(n1);
            Windows.ApplicationModel.Chat.ChatMessageManager.ShowComposeSmsMessageAsync(chatMessage).AsTask().ConfigureAwait(false).GetAwaiter().GetResult();
#endif
        }

        public override void dial(string phoneNumber) {
            execute("tel:" + phoneNumber);/*
            //java.lang.System.@out.println("Trying to dial phone number "+phoneNumber);
            dispatcher.RunAsync(CoreDispatcherPriority.Normal, async () =>
                {
                    if (ApiInformation.IsApiContractPresent("Windows.ApplicationModel.Calls.CallsPhoneContract", 1, 0))
                    {
                        Windows.ApplicationModel.Calls.PhoneCallStore phoneCallStore = await Windows.ApplicationModel.Calls.PhoneCallManager.RequestStoreAsync();
                        Guid LineGuid = await phoneCallStore.GetDefaultLineAsync();

                        Windows.ApplicationModel.Calls.PhoneLine phoneLine = await Windows.ApplicationModel.Calls.PhoneLine.FromIdAsync(LineGuid);
                        phoneLine.Dial(phoneNumber, phoneNumber);
                    }
                }).AsTask().GetAwaiter().GetResult();*/
        }

        public override object createMutableImage(int width, int height, int color)
        {
            CodenameOneImage ci = new CodenameOneImage();
            ci.mutable = true;
            ci.image = new CanvasRenderTarget(screen, screen.ConvertPixelsToDips(width), screen.ConvertPixelsToDips(height));
            ci.graphics.destination.setColor(color, (byte)((color >> 24) & 0xff));
            //ci.graphics.destination.setAlpha((color >> 24) & 0xff);
            ci.graphics.destination.clear();
            return ci;
        }

        public override int getImageWidth(object n1)
        {
            return ((CodenameOneImage)n1).getImageWidth();
        }

        public override int getImageHeight(object n1)
        {
            return ((CodenameOneImage)n1).getImageHeight();
        }

        public override bool isAlphaMutableImageSupported()
        {
            return true;
        }

        public override object scale(object sourceImage, int width, int height)
        {
            CodenameOneImage image = (CodenameOneImage)sourceImage;

            int srcWidth = image.getImageWidth();
            int srcHeight = image.getImageHeight();

            // no need to scale
            if (srcWidth == width && srcHeight == height)
            {
                return image;
            }

            CodenameOneImage ci = new CodenameOneImage();
            ci.opaque = image.opaque;
            if (width > 0 && height < 0)
            {
                height = srcHeight * width / srcWidth;
            }
            if (width < 0 && height > 0)
            {
                width = srcWidth * height / srcHeight;
            }

            CanvasRenderTarget cr = new CanvasRenderTarget(image.image.Device, image.image.ConvertPixelsToDips(width), image.image.ConvertPixelsToDips(height), image.image.Dpi);
            ci.image = cr;
            ci.graphics.destination.drawImage(image.image, 0, 0, width, height);
            ci.graphics.destination.dispose();
            return ci;
        }

        public override int getSoftkeyCount()
        {
            return 0;
        }

        public override int[] getSoftkeyCode(int n1)
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
        public override bool isPortrait()
        {
            return false;
        }

        public override int getColor(object graphics)
        {
            return ((NativeGraphics)graphics).destination.getColor();
        }

        public override void setColor(object graphics, int RGB)
        {
            try
            {
                ((NativeGraphics)graphics).destination.setColor(RGB);
            }
            catch (Exception)
            {
                ((NativeGraphics)graphics).destination.setColor((int)(getColor(graphics) & 0xff000000) | RGB);
            }
        }

        public override void setAlpha(object graphics, int alpha)
        {
            ((NativeGraphics)graphics).destination.setAlpha(alpha);
            ((NativeGraphics)graphics).destination.setColor(getColor(graphics) | (alpha << 24));
        }

        public override int getAlpha(object graphics)
        {
            return ((NativeGraphics)graphics).destination.getAlpha();
        }

        public override void setNativeFont(object graphics, object font)
        {
            NativeFont f;
            if (font == null)
            {
                f = (NativeFont)getDefaultFont();
            }
            else
            {
                f = (NativeFont)font;
            }
            ((NativeGraphics)graphics).font = f;
        }

        public override bool isBaselineTextSupported()
        {
            return true;
        }

        public override int getFontAscent(object nativeFont)
        {
            CanvasTextFormat font = (nativeFont == null ? defaulFontCanvas : ((NativeFont)nativeFont).font);
            return (int)-Math.Round(font.FontSize);
        }

        public override int getFontDescent(object nativeFont)
        {
            CanvasTextFormat font = (nativeFont == null ? defaulFontCanvas : ((NativeFont)nativeFont).font);
            return (int)Math.Abs(Math.Round(font.FontSize));
        }

        public override int getClipX(object graphics)
        {
            return ((NativeGraphics)graphics).getClipX();
        }

        public override int getClipY(object graphics)
        {
            return ((NativeGraphics)graphics).getClipY();
        }

        public override int getClipWidth(object graphics)
        {
            return ((NativeGraphics)graphics).getClipW();
        }

        public override int getClipHeight(object graphics)
        {
            return ((NativeGraphics)graphics).getClipH();
        }
        
        public override void setClip(object graphics, int clipX, int clipY, int clipW, int clipH)
        {
            Rectangle clip = new Rectangle(clipX, clipY, clipW, clipH);
            ((NativeGraphics)graphics).clip = clip;
        }

        public override void clipRect(object graphics, int x, int y, int w, int h)
        {
            NativeGraphics ng = (NativeGraphics)graphics;
            Rectangle clip = new Rectangle(x, y, w, h);
            if (ng.clip != null)
            {
                ng.clip = clip.intersection(ng.clip);
            }
            else
            {
                Debug.WriteLine("clipRect nulo " + ng.destination);
                ng.clip = clip;
            }
        }
        //Line drawLineLineInstance;
        public override void drawLine(object graphics, int x1, int y1, int x2, int y2)
        {
            ((NativeGraphics)graphics).destination.drawLine(x1, y1, x2, y2);
        }
        //Rectangle fillDrawRectInstance;
        public override void fillRect(object graphics, int x, int y, int w, int h)
        {
            ((NativeGraphics)graphics).destination.fillRect(x, y, w, h);
        }

        public override void drawRect(object graphics, int x, int y, int w, int h)
        {
            drawRect(graphics, x, y, w, h, 1);
        }

        public override void drawRect(object graphics, int x, int y, int w, int h, int stroke)
        {
            ((NativeGraphics)graphics).destination.drawRect(x, y, w, h, stroke);
        }

        public override void drawRoundRect(object graphics, int x, int y, int w, int h, int arcW, int arcH)
        {
            ((NativeGraphics)graphics).destination.drawRoundRect(x, y, w, h, arcW, arcH);
        }

        public override void fillRoundRect(object graphics, int x, int y, int w, int h, int arcW, int arcH)
        {
            ((NativeGraphics)graphics).destination.fillRoundRect(x, y, w, h, arcW, arcH);
        }

        public override void fillArc(object graphics, int x, int y, int w, int h, int startAngle, int arcAngle)
        {
            ((NativeGraphics)graphics).destination.fillArc(x, y, w, h, startAngle, arcAngle);
        }

        public override void drawArc(object graphics, int x, int y, int w, int h, int startAngle, int arcAngle)
        {
            ((NativeGraphics)graphics).destination.drawArc(x, y, w, h, startAngle, arcAngle);
        }

        public override void drawString(object graphics, string str, int x, int y)
        {
            ((NativeGraphics)graphics).destination.drawString(str, x, y);
        }
        public override void drawImage(object graphics, object img, int x, int y)
        {
            CodenameOneImage ci = (CodenameOneImage)img;
            ci.lastAccess = DateTime.Now.Ticks;
            if (ci.image != null)
            {
                ((NativeGraphics)graphics).destination.drawImage(ci.image, x, y);
            }
            else
            {
                Debug.WriteLine("SilverlightImplementatio.drawImage:" + " ci.image generated null");
            }
          
        }

        public override bool areMutableImagesFast()
        {
            return false; // async painter
        }

        public override void drawImage(object graphics, object img, int x, int y, int w, int h)
        {
            CodenameOneImage ci = (CodenameOneImage)img;
            ci.lastAccess = DateTime.Now.Ticks;
            if(ci.image != null){
                ((NativeGraphics)graphics).destination.drawImage(ci.image, x, y, w, h);
            }
            else
            {
                Debug.WriteLine("SilverlightImplementatio.drawImage:" + " ci.image generated null");
            }
        }

        public override void drawRGB(object graphics, int[] rgb, int offset, int x, int y, int w, int h, bool n8)
        {
            CodenameOneImage ci = (CodenameOneImage)createImage(rgb, w, h);
            drawImage(graphics, ci, x, y);
        }

        public override object getNativeGraphics()
        {
            if (globalGraphics == null)
            {
                globalGraphics = myView.getGraphics();
            }
            return globalGraphics;
        }

        public override object getNativeGraphics(object img)
        {
            CodenameOneImage image = (CodenameOneImage)img;
            if (image.graphics == null || image.graphics.destination.isDisposed())
            {
                image.initGraphics(); //// image = new CanvasRenderTarget(screen, image.getImageWidth(), image.getImageHeight(), 96.0f); //to use with pixel DPI=96!!! not screen.Dpi);
            }
            setClip(image.graphics, 0, 0, image.getImageWidth(), image.getImageHeight());
            return image.graphics;
        }

        public override int charsWidth(object n1, char[] n2, int n3, int n4)
        {
            string s = new string(n2, n3, n4);
            return stringWidth(n1, s);
        }

        private static readonly Dictionary<StringFontPair, int> stringWidthCache = new Dictionary<StringFontPair, int>();

        public override int stringWidth(object n1, string n2)
        {
            StringFontPair sfp = new StringFontPair(n2, f(n1));
            
            
            if (!stringWidthCache.ContainsKey(sfp))
            {
                int result = f(n1).getStringWidth(n2);
                stringWidthCache.Add(sfp, result);
            }
            return stringWidthCache[sfp];
        }

        public override int charWidth(object n1, char n2)
        {
            return stringWidth(n1, n2.ToString());
        }

        public override int getFace(object n1)
        {
            return f(n1).face;
        }

        public override int getSize(object n1)
        {
            return f(n1).size;
        }

        public override int getStyle(object n1)
        {
            return f(n1).style;
        }

        public override int getHeight(object n1)
        {
            return f(n1).height;
        }

        public override bool isLookupFontSupported()
        {
            return true;
        }

        public override object loadNativeFont(string lookupStr)
        {
            string lookup = nativePath(lookupStr);
            string[] fonts = lookup.Split(new char[] { ';' });
            foreach (string f in fonts)
            {
                try
                {
                    string[] split = f.Split(new char[] { '-' });
                    string familyName = split[0];
                    string style = split[1];
                    string size = split[2];

                    NativeFont nf = new NativeFont(0, 0, 0, new CanvasTextFormat());
                    using (AutoResetEvent are = new AutoResetEvent(false))
                    {
                        dispatcher.RunAsync(CoreDispatcherPriority.Normal, () =>
                        {
                            nf.font.FontFamily = familyName;

                            if (style.Equals("bolditalic"))
                            {
                                nf.font.FontWeight = FontWeights.Bold;
                                nf.font.FontStyle = FontStyle.Italic;
                                nf.style = 2 & 1;
                            }
                            else if (style.Equals("italic"))
                            {
                                nf.font.FontStyle = FontStyle.Italic;
                                nf.style = 2;
                            }
                            else if (style.Equals("bold"))
                            {
                                nf.font.FontWeight = FontWeights.Bold;
                                nf.style = 1;
                            }
                            nf.font.FontSize = Convert.ToInt32(size);
                            nf.font.WordWrapping = CanvasWordWrapping.NoWrap;
                            are.Set();
                        }).AsTask().GetAwaiter().GetResult();
                        are.WaitOne();
                    }

                    return nf;
                }
                catch (Exception)
                {
                    Debug.WriteLine("loadNativeFont failed");
                }
            }
            return null;
        }

        public override bool isTrueTypeSupported()
        {
            return true;
        }

        public override bool isNativeFontSchemeSupported()
        {
            return true;
        }

        public override object loadTrueTypeFont(string fontName, string fileName)
        {
            
            

            NativeFont nf = new NativeFont(0, 0, 0, new CanvasTextFormat());
            string file = fileName != null ? nativePath(fileName) : null;
            string family = fontName;
            using (AutoResetEvent are = new AutoResetEvent(false))
            {
                dispatcher.RunAsync(CoreDispatcherPriority.Normal, () =>
                {
                    if (fontName.StartsWith("native:"))
                    {
                        if ("native:MainThin".Equals(fontName))
                        {
                            nf.font.FontFamily = "Segoe UI";
                            nf.font.FontWeight = FontWeights.Thin;

                        }
                        else if ("native:MainLight".Equals(fontName))
                        {
                            nf.font.FontFamily = "Segoe UI";
                            nf.font.FontWeight = FontWeights.Light;
                        }
                        else if ("native:MainRegular".Equals(fontName))
                        {
                            nf.font.FontFamily = "Segoe UI";
                        }

                        else if ("native:MainBold".Equals(fontName))
                        {
                            nf.font.FontFamily = "Segoe UI";
                            nf.font.FontWeight = FontWeights.Bold;
                        }

                        else if ("native:MainBlack".Equals(fontName))
                        {
                            nf.font.FontFamily = "Segoe UI";
                            nf.font.FontWeight = FontWeights.Black;
                        }

                        else if ("native:ItalicThin".Equals(fontName))
                        {
                            nf.font.FontFamily = "Segoe UI";
                            nf.font.FontStyle = FontStyle.Italic;
                            nf.font.FontWeight = FontWeights.Thin;

                        }

                        else if ("native:ItalicLight".Equals(fontName))
                        {
                            nf.font.FontFamily = "Segoe UI";
                            nf.font.FontStyle = FontStyle.Italic;
                            nf.font.FontWeight = FontWeights.Light;
                        }

                        else if ("native:ItalicRegular".Equals(fontName))
                        {
                            nf.font.FontFamily = "Segoe UI";
                            nf.font.FontStyle = FontStyle.Italic;
                        }

                        else if ("native:ItalicBold".Equals(fontName))
                        {
                            nf.font.FontFamily = "Segoe UI";
                            nf.font.FontStyle = FontStyle.Italic;
                            nf.font.FontWeight = FontWeights.Bold;
                        }

                        else if ("native:ItalicBlack".Equals(fontName))
                        {
                            nf.font.FontFamily = "Segoe UI";
                            nf.font.FontStyle = FontStyle.Italic;
                            nf.font.FontWeight = FontWeights.Black;
                        }
                    }
                    else
                    {

                        nf.font.FontFamily = generateFontFamilyFromFontName(family, fileName);
                    }
                    nf.font.WordWrapping = CanvasWordWrapping.NoWrap;
                    are.Set();
                }).AsTask().GetAwaiter().GetResult();
                are.WaitOne();
            }
            return nf;
        }

        private string generateFontFamilyFromFontName(string fontName, string fileName)
        {
            string file = nativePath(fileName);
             Microsoft.Graphics.Canvas.Text.CanvasFontSet fontSet = new Microsoft.Graphics.Canvas.Text.CanvasFontSet(new Uri(@"ms-appx:///res/" + file));
            string first = null;
            foreach (var font in fontSet.Fonts)
            {
                foreach (var fam in font.FamilyNames)
                {
                    if (first == null)
                    {
                        first = @"res\" + file + "#" + fam.Value;
                    }
                    if (fam.Value.Equals(fontName))
                    {
                        return @"res\" + file + "#" + fam.Value;
                    }
                }
            }

            if (first != null)
            {
                return first;
            }

            return @"res\" + file + "#" + fontName;

        }

        public override object deriveTrueTypeFont(object font, float size, int weight)
        {
            NativeFont fnt = (NativeFont)font;
            NativeFont n = new NativeFont(0, 0, 0, new CanvasTextFormat());
            using (AutoResetEvent are = new AutoResetEvent(false))
            {
                dispatcher.RunAsync(CoreDispatcherPriority.Normal, () =>
                {
                    n.font.FontFamily = fnt.font.FontFamily;
                    n.font.FontSize = size;
                    n.font.FontStyle = fnt.font.FontStyle;
                    n.font.FontWeight = fnt.font.FontWeight;
                    if ((weight & 1) != 0) // bold
                    {
                        n.font.FontWeight = FontWeights.Bold;
                    }
                    if ((weight & 2) != 0) // italic
                    {
                        n.font.FontStyle = FontStyle.Italic;
                    }
                    n.font.WordWrapping = CanvasWordWrapping.NoWrap;
                    are.Set();
                }).AsTask().GetAwaiter().GetResult();
                are.WaitOne();
            }
            return n;
        }

        public override object getDefaultFont()
        {
            if (defaultFont == null)
            {
                object defaul = createFont(Font.FACE_SYSTEM, Font.STYLE_PLAIN, Font.SIZE_MEDIUM);
                defaultFont = (NativeFont)defaul;
            }
            return defaultFont;
        }

        public override object createFont(int face, int style, int size)
        {
            NativeFont nf = new NativeFont(face, style, size, new CanvasTextFormat());
            using (AutoResetEvent are = new AutoResetEvent(false))
            {
                dispatcher.RunAsync(CoreDispatcherPriority.Normal, () =>
                {
                    int a = Convert.ToInt32((nf.font.FontSize - 5) * (float)scaleFactor);
                    if (scaleFactor == 1.2f) a = (int)nf.font.FontSize;
                    switch (size)
                    {
                        case 8: //com.codename1.ui.Font._fSIZE_1SMALL:
                            a = Convert.ToInt32(nf.font.FontSize * (float)1.8 / 3 * (float)scaleFactor);
                            break;
                        case 16: //com.codename1.ui.Font._fSIZE_1LARGE:
                            a = Convert.ToInt32(nf.font.FontSize * (float)3.5 / 3 * (float)scaleFactor);
                            break;
                    }
                    nf.font.FontSize = a;
                    if ((style & 2) != 0) // com.codename1.ui.Font._fSTYLE_1ITALIC
                    {
                        nf.font.FontStyle = FontStyle.Italic;
                    }
                    if ((style & 1) != 0) // com.codename1.ui.Font._fSTYLE_1BOLD
                    {
                        nf.font.FontWeight = FontWeights.Bold;
                    }
                    nf.font.WordWrapping = CanvasWordWrapping.NoWrap;
                    are.Set();
                }).AsTask().GetAwaiter().GetResult();
                are.WaitOne();
            }
            return nf;
        }

        public virtual NativeFont f(object fnt)
        {
            if (fnt == null) return (NativeFont)getDefaultFont();
            return (NativeFont)fnt;
        }

        public override bool isScaledImageDrawingSupported()
        {
            return true;
        }

        public override object createSoftWeakRef(object n1)
        {
            return new SoftRef(n1);
        }

        public override object extractHardRef(object n1)
        {
            if (n1 != null)
            {
                return ((SoftRef)n1).get();
            }
            return null;
        }
        
        public override object connect(string n1, bool read, bool write)
        {
            NetworkOperation n = new NetworkOperation();
            Uri uri = new Uri(nativePath(n1));
            n.request = (HttpWebRequest)WebRequest.Create(uri);
            n.request.Headers["User-agent"] = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/46.0.2486.0 Safari/537.36 Edge/13.10586";
            return n;
        }


        public override void setHttpMethod(object connection, string method)
        {
            NetworkOperation n = (NetworkOperation)connection;
            n.request.Method = method;
        }

        public override void setHeader(object n1, string n2, string n3)
        {
            NetworkOperation n = (NetworkOperation)n1;
            string key = n2;
            string value = n3;
            if (key.ToLower().Equals("accept"))
            {
                n.request.Accept = value;
                return;
            }
            if (key.ToLower().Equals("connection") || key.ToLower().Equals("keepalive") ||
                key.ToLower().Equals("expect") || key.ToLower().Equals("date") || key.ToLower().Equals("host") ||
                key.ToLower().Equals("if-modified-since") || key.ToLower().Equals("range") ||
                key.ToLower().Equals("referer") || key.ToLower().Equals("transfer-encoding"))
            {
                return;
            }
            if (key.ToLower().Equals("content-length"))
            {
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

        public override int getContentLength(object n1)
        {
            return Convert.ToInt32(((NetworkOperation)n1).response.ContentLength);
        }

        public override java.io.OutputStream openOutputStream(object connection)
        {
            if (connection is string)
            {

                StorageFolder store = getStore((string)connection);
                try
                {
                    Stream s = Task.Run(() => store.OpenStreamForWriteAsync(nativePathStore((string)connection), CreationCollisionOption.OpenIfExists)).GetAwaiter().GetResult();
                    s.Seek(0, SeekOrigin.Begin);
                    return new java.io.OutputStreamProxy(s);
                }
                catch (Exception e)
                {
                    java.io.FileNotFoundException ex = new java.io.FileNotFoundException("FileNotFoundException - " + e.Message);
                    throw ex;
                }
            }
            io.BufferedOutputStream bo = new io.BufferedOutputStream(new java.io.OutputStreamProxy(((NetworkOperation)connection).requestStream));
            return bo;
        }

        public override java.io.OutputStream openOutputStream(object connection, int offset)
        {
            if (connection is string)
            {
                try
                {
                    StorageFolder store = getStore((string)connection);
                    Stream stream = Task.Run(() => store.OpenStreamForWriteAsync(nativePathStore((string)connection), CreationCollisionOption.OpenIfExists)).ConfigureAwait(false).GetAwaiter().GetResult();
                    stream.Seek(offset, SeekOrigin.Current);
                    return new java.io.OutputStreamProxy(stream);
                }
                catch (Exception e)
                {
                    java.io.FileNotFoundException ex = new java.io.FileNotFoundException("FileNotFoundException - " + e.Message);
                    throw ex;
                }
            }
            return null;
        }

        public override java.io.InputStream openInputStream(object connection)
        {
            StorageFile output;
            return openInputStream(connection, false, out output);
        }

        private java.io.InputStream openInputStream(object connection, bool outputFile, out StorageFile outFile)
        {
           if (connection is string)
            {
                try
                {
                    if (isTempFile((string)connection))
                    {
                        StorageFile tempFile = getTempFile((string)connection);
                        if (outputFile)
                        {
                            outFile = tempFile;
                            return null;
                        } else {
                            outFile = null;
                            return new InputStreamProxy(Task.Run(() => tempFile.OpenStreamForReadAsync())
                               .ConfigureAwait(false)
                               .GetAwaiter()
                               .GetResult());
                        }
                    }
                    StorageFolder store = getStore((string)connection); //KnownFolders.CameraRoll
                    string file = nativePathStore((string)connection);
                    StorageFile sfile = store.CreateFileAsync((string)file, CreationCollisionOption.OpenIfExists)
                        .AsTask()
                        .ConfigureAwait(false)
                        .GetAwaiter()
                        .GetResult();
                    if (outputFile)
                    {
                        outFile =  sfile;
                        return null;
                    } else
                    {
                        outFile = null;
                        Stream stream = null;
                        try
                        {
                            stream = Task.Run(() => store.OpenStreamForReadAsync(file)).ConfigureAwait(false).GetAwaiter().GetResult();
                            return new InputStreamProxy(stream);
                        }
                        catch (Exception ex2)
                        {
                            java.lang.System.err.println("Failed to create file input stream by simply opening the file.  Trying to open a copy instead " + ex2.Message);
                            // If it failed to create the input stream, it is possible that
                            // it is because the file is already being used (e.g. an output stream is writing to it).
                            // In this case, we'll create a copy to a temp file, and create an inputstream from that one.
                            try
                            {
                                StorageFile tempFile = sfile.CopyAsync(ApplicationData.Current.TemporaryFolder, sfile.Name, NameCollisionOption.GenerateUniqueName)
                                    .AsTask()
                                    .ConfigureAwait(false)
                                    .GetAwaiter()
                                    .GetResult();
                                return new InputStreamProxy(Task.Run(() => tempFile.OpenStreamForReadAsync())
                                    .ConfigureAwait(false)
                                    .GetAwaiter()
                                    .GetResult());
                            }
                            catch (Exception e3)
                            {
                                java.lang.System.err.println("Failed to open storage file via copy " + e3.Message);
                            }

                        }
                    }
                }
                catch (Exception e)
                {
                    java.io.FileNotFoundException ex = new java.io.FileNotFoundException("FileNotFoundException - " + e.Message);
                    throw ex;
                }
            }
            //io.BufferedInputStream bo = new io.BufferedInputStream(new InputStreamProxy(((NetworkOperation)connection).response.GetResponseStream()));
            //return bo;
            outFile = null;
            return new java.io.InputStreamProxy(((NetworkOperation)connection).response.GetResponseStream());
        }

        public override void setPostRequest(object n1, bool n2)
        {
            if (n2)
            {
                ((NetworkOperation)n1).request.Method = "POST";
            }
            else
            {
                ((NetworkOperation)n1).request.Method = "GET";
            }
        }

        public override int getResponseCode(object n1)
        {
            return Convert.ToInt32(((NetworkOperation)n1).response.StatusCode);
        }

        public override string getResponseMessage(object n1)
        {
            return ((NetworkOperation)n1).response.StatusDescription;
        }

        public override void vibrate(int n1)
        {
#if WINDOWS_PHONE_APP

            Windows.Phone.Devices.Notification.VibrationDevice vc = Windows.Phone.Devices.Notification.VibrationDevice.GetDefault();
            vc.Vibrate(TimeSpan.FromMilliseconds(n1));
#endif
        }

        public override string getHeaderField(string n1, object n2)
        {
            return ((NetworkOperation)n2).response.Headers[n1];
        }

        public override string[] getHeaderFieldNames(object n1)
        {
            int i = ((NetworkOperation)n1).response.Headers.Count;
            string[] arr = new string[i];
            string[] keys = ((NetworkOperation)n1).response.Headers.AllKeys;
            for (int iter = 0; iter < i; iter++)
            {
                arr[iter] = keys[iter];
            }
            return arr;
        }

        public override string[] getHeaderFields(string n1, object n2)
        {
            string s = ((NetworkOperation)n2).response.Headers[n1];
            if (s == null)
            {
                return null;
            }
            return new string[] {s};
        }

        public override int getCommandBehavior()
        {
            // COMMAND_BEHAVIOR_BUTTON_BAR
            return 4;
        }

        public override void deleteStorageFile(string name)
        {
            deleteFile(iDefaultStore, nativePath(name));
        }

        public override int getStorageEntrySize(string name)
        {
            string f = nativePath(name);
            StorageFile file = iDefaultStore.GetFileAsync(f).AsTask().ConfigureAwait(false).GetAwaiter().GetResult(); ;
            if (file == null) return 0; //.Name != nativePath(name))
            Stream st = Task.Run(() => file.OpenStreamForReadAsync()).ConfigureAwait(false).GetAwaiter().GetResult();
            long size = st.Length;
            st.Dispose();
            return Convert.ToInt32(size);
        }

        public override java.io.OutputStream createStorageOutputStream(string name)
        {
            try
            {
                StorageFile file = iDefaultStore.CreateFileAsync(nativePath(name), CreationCollisionOption.OpenIfExists)
                    .AsTask()
                    .ConfigureAwait(false)
                    .GetAwaiter()
                    .GetResult();
                return new java.io.OutputStreamProxy(
                    Task.Run(() => file.OpenStreamForWriteAsync())
                    .ConfigureAwait(false)
                    .GetAwaiter()
                    .GetResult()
                    
                    );
            }
            catch (Exception e)
            {
                java.io.FileNotFoundException ex = new java.io.FileNotFoundException("FileNotFoundException - " + e.Message);
                throw ex;
            }
        }



        public override java.io.InputStream createStorageInputStream(string name)
        {
            try
            {
                StorageFile file = iDefaultStore.CreateFileAsync(nativePath(name), CreationCollisionOption.OpenIfExists)
                    .AsTask()
                    .ConfigureAwait(false)
                    .GetAwaiter()
                    .GetResult();

                try
                {
                    return new InputStreamProxy(Task.Run(() => file.OpenStreamForReadAsync())
                        .ConfigureAwait(false)
                        .GetAwaiter()
                        .GetResult());
                }
                catch (Exception ex2)
                {
                    java.lang.System.err.println("Failed to create storage input stream by simply opening the file.  Trying to open a copy instead " + ex2.Message);
                    // If it failed to create the input stream, it is possible that
                    // it is because the file is already being used (e.g. an output stream is writing to it).
                    // In this case, we'll create a copy to a temp file, and create an inputstream from that one.
                    try
                    {
                        StorageFile tempFile = file.CopyAsync(ApplicationData.Current.TemporaryFolder, file.Name, NameCollisionOption.GenerateUniqueName)
                            .AsTask()
                            .ConfigureAwait(false)
                            .GetAwaiter()
                            .GetResult();
                        return new InputStreamProxy(Task.Run(() => tempFile.OpenStreamForReadAsync())
                            .ConfigureAwait(false)
                            .GetAwaiter()
                            .GetResult());
                    }
                    catch (Exception e3)
                    {
                        java.lang.System.err.println("Failed to open storage file via copy " + e3.Message);
                    }

                }
            }
            catch (Exception) { }
            
            // If our first attempt failed, let's try again a different way.
            try
            {
                StorageFile file = iDefaultStore.CreateFileAsync(nativePath(name), CreationCollisionOption.GenerateUniqueName)
                    .AsTask()
                    .ConfigureAwait(false)
                    .GetAwaiter()
                    .GetResult();
                return new InputStreamProxy(file.OpenReadAsync()
                    .AsTask()
                    .ConfigureAwait(false)
                    .GetAwaiter()
                    .GetResult()
                    .AsStream());
                        
            }
            catch (Exception e)
            {
                java.io.FileNotFoundException ex = new java.io.FileNotFoundException("FileNotFoundException - " + e.Message);
                throw ex;
            }
            
        }

        public override bool storageFileExists(string name)
        {
            string uri = nativePath(name);
            if (uri.StartsWith("/"))
            {
                uri = @"res\" + uri.Substring(1);
            }
            uri = uri.Replace('/', '\\');
            return exists(iDefaultStore, uri);
        }

        public override string[] listStorageEntries()
        {
            IReadOnlyList<StorageFile> filesInFolder = iDefaultStore.GetFilesAsync().AsTask().ConfigureAwait(false).GetAwaiter().GetResult();

            string[] ss = new string[filesInFolder.Count];
            for (int i = 0; i < filesInFolder.Count; i++)
            {
                ss[i] = filesInFolder.ElementAt(i).Name;
            }
            return ss;
        }

        public override void printStackTraceToStream(Exception t, Writer o)
        {
            string trace = t.StackTrace;
            if (t is java.lang.Throwable)
            {
                java.lang.Throwable th = (java.lang.Throwable)t;
                trace = "\n" + th.getStackTraceString();
            }
            java.lang.System.@out.println("Stack trace is " + trace);
            char[] buf = trace.ToCharArray();
            int len = buf.Length;
            o.write(buf, 0, len);
        }

        public override Database openOrCreateDB(string databaseName)
        {
            string path = Path.Combine(Windows.Storage.ApplicationData.Current.LocalFolder.Path,
            databaseName+".sqlite");
            //return new WinDB(new SQLite.Net.SQLiteConnection(new
            //SQLite.Net.Platform.WinRT.SQLitePlatformWinRT(), path));
            return WinDBRaw.openOrCreate(path);
            //return null;
        }

        public override void deleteDB(string databaseName)
        {
            string path = Path.Combine(Windows.Storage.ApplicationData.Current.LocalFolder.Path,
            databaseName + ".sqlite");
            StorageFile filed = ApplicationData.Current.LocalFolder.GetFileAsync(databaseName + ".sqlite").AsTask().GetAwaiter().GetResult();
            if (filed != null)
            {
                filed.DeleteAsync().AsTask().GetAwaiter().GetResult();


            }
            

        }
        public override bool existsDB(string databaseName)
        {
            string fileName = databaseName + ".sqlite";
            var item = ApplicationData.Current.LocalFolder.TryGetItemAsync(fileName).AsTask().GetAwaiter().GetResult();
            return item != null;
        }

        internal class CodenameOneUWPThread : com.codename1.impl.CodenameOneThread
        {
            public CodenameOneUWPThread(java.lang.Runnable r, string name) : base(r, name)
            {

            }

            public override void run()
            {
                try {
                    base.run();
                } catch (System.Exception ex)
                {
                    java.lang.System.err.println(ex.StackTrace);
                    CodenameOneThread.handleException(new java.lang.Exception("Native UWP Exception: " + ex.Message + "\n" + ex.StackTrace));
                }
                
            }
        }

        public override void startThread(string name, java.lang.Runnable r)
        {
            CodenameOneUWPThread t = new CodenameOneUWPThread(r, name);
            t.start();

        }

        public override string getAppHomePath()
        {
#if WINDOWS_PHONE_APP

            return "file:///" + getDictValue(iCN1Settings, "DefaultStorageFolder", "Cache:");
#else
            return "file:///" + getDictValue(iCN1Settings, "DefaultStorageFolder", "Local:");
#endif
        }

        public override string[] listFilesystemRoots()
        {
            var roots = new List<string>();
            roots.Add("local:");
            var sdcards = KnownFolders.RemovableDevices;
            if (sdcards != null)
            {
                roots.Add("removable:");
            }
            roots.Add("install:");
            roots.Add("roaming:");
            roots.Add("cache:");
            roots.Add("temp:");
            roots.Add("CameraRoll:");
            roots.Add("Pictures:");
            roots.Add("SDCard:");
           

            var rootsAll = roots.ToArray();
            return rootsAll;
        }


        private static StorageFolder getStore(string ss)
        {
            try
            {
                if (ss != null) // && (ss[0] == '/' || ss[0] == '\\'))
                {
                    if (ss.StartsWith("file:/"))
                    {
                        ss = ss.Substring(6);

                        while (ss.Length > 0 && ss[0] == '/')
                        {
                            ss = ss.Substring(1);
                        }

                    }
                    if (ss.Length > 0 && (ss[0] == '/' || ss[0] == '\\'))
                        ss = ss.Substring(1);
                    var pos = ss.IndexOfAny(new char[] { ':', '/', '\\' }, 0);
                    if (pos > 0 && ss[pos] == ':')
                    {
                        ss = ss.Substring(0, pos);
                        ss = ss.ToLower();
                        //    else
                        //        ss = ss.Substring(1);
                        if (ss.Equals("local"))
                            return ApplicationData.Current.LocalFolder;
#if WINDOWS_PHONE_APP
                        else if (ss.Equals("cache"))
                            return ApplicationData.Current.LocalCacheFolder;
#endif
                        else if (ss.Equals("roaming"))
                            return ApplicationData.Current.RoamingFolder;
                        else if (ss.Equals("install"))
                            return Package.Current.InstalledLocation;
                        else if (ss.Equals("temp"))
                            return ApplicationData.Current.TemporaryFolder;
                        else if (ss.Equals("cameraroll"))
                            return KnownFolders.CameraRoll;
                        else if (ss.Equals("pictures"))
                            return KnownFolders.PicturesLibrary;
                        else if (ss.Equals("removable"))
                            return KnownFolders.RemovableDevices;
                        else if (ss.Equals("sdcard"))
                        {
                            StorageFolder s = KnownFolders.RemovableDevices; //get first D://
                            var folders = s.GetFoldersAsync().AsTask().ConfigureAwait(false).GetAwaiter().GetResult();
                            StorageFolder s1 = folders.FirstOrDefault();
                            return s1;
                        }
                    }
                }
                if (iDefaultStore == null)
                {
#if WINDOWS_PHONE_APP
                    return ApplicationData.Current.LocalCacheFolder;
#else
                    return ApplicationData.Current.LocalFolder;
#endif
                }
                else
                    return iDefaultStore;
            }
            catch (Exception )
            {
            }
            return null;
        }

        private static StorageFile getFile(string aUrl)
        {
            try
            {
                if (instance.isTempFile(aUrl))
                {
                    return instance.getTempFile(aUrl);
                }
                StorageFolder folder = getStore(aUrl);
                int pos = 0; //remove root name ex. intall: from /intall:/test.txt
                if (aUrl[0] == '/' || aUrl[0] == '\\') pos++;
                pos = aUrl.IndexOfAny(new char[] { ':', '/', '\\' }, pos);
                if (pos > 0 && aUrl[pos] == ':')
                    aUrl = aUrl.Substring(pos + 2);
                return folder.GetFileAsync(aUrl).AsTask().ConfigureAwait(false).GetAwaiter().GetResult();
            }
            catch (System.IO.FileNotFoundException)
            {
            }
            return null;
        }
        /// <summary>
        /// Use only for storage not for filesystem
        /// </summary>
        /// <param name="s"></param>
        /// <returns></returns>
        private string nativePath(string s)
        {
            string ss = s;
            if (ss.StartsWith("file:/"))
            {
                ss = ss.Substring(6);
                while (ss[0] == '/')
                {
                    ss = ss.Substring(1);
                }
                ss = ss.Replace('/', '\\');
            }
            return ss;
        }
        /// <summary>
        /// Translate Java uri to C# and remove root name part ex. file:///intall:/t.png -> t.png
        /// do not use for http url or storage
        /// </summary>
        /// <param name="s"></param>
        /// <returns></returns>
        ///  THis method is an absolute mess.... needs to be simplified.
         private string nativePathStore(string s)
        {
            
            string ss = s;
            if (ss.StartsWith("file:/"))
            {
                ss = ss.Substring(6);

                while (ss.Length > 0 && ss[0] == '/')
                {
                    ss = ss.Substring(1);
                }

            }
            if (ss.Length == 0) return ss;
            ss = ss.Replace('/', '\\');
            int pos = 0; //remove root name ex. intall: from /intall:/test.txt
            if (ss[0] == '/' || ss[0] == '\\') pos++;
            if (pos >= ss.Length)
            {
                return "";
            }
            pos = ss.IndexOfAny(new char[] { ':', '/', '\\' }, pos);
            if (pos < 0)
            {
                return ss;
            }
            if (ss.Length <= pos)
            {
                return "";
            }
            if (ss.Length > pos && pos >= 0 && ss[pos] == ':')
            {
                pos++;
                while (ss.Length > pos && ((ss[pos] == '/') || (ss[pos] == '\\')))
                {
                    pos++;
                }
                if (pos >= ss.Length)
                {
                    return "";
                }
                ss = ss.Substring(pos);
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

        public override string[] listFiles(string directory)
        {
            StorageFolder store = getStore(directory);
            var f = nativePathStore(directory);
            if (f.Length == 0)
                folder = store;
            else
                folder = store.GetFolderAsync(f).AsTask().ConfigureAwait(false).GetAwaiter().GetResult();
            IReadOnlyList<StorageFolder> directoryNames = folder.GetFoldersAsync().AsTask().ConfigureAwait(false).GetAwaiter().GetResult();
            string[] dirnames = new string[directoryNames.Count];
            for (int i = 0; i < directoryNames.Count; i++)
            {
                var s = directoryNames.ElementAt(i).Name;
                s = s.Replace('\\', '/');
                if (!s.EndsWith("/"))
                    s = s + "/";
                dirnames[i] = s;
            }
            IReadOnlyList<StorageFile> fileNames = folder.GetFilesAsync().AsTask().ConfigureAwait(false).GetAwaiter().GetResult();
            string[] filenames = new string[fileNames.Count];
            for (int i = 0; i < fileNames.Count; i++)
            {
                filenames[i] = fileNames.ElementAt(i).Name;
            }
            string[] all = new string[dirnames.Length + filenames.Length];
            dirnames.CopyTo(all, 0);
            filenames.CopyTo(all, dirnames.Length);
            return all;
        }

        public override long getRootSizeBytes(string root)
        {
            return 0;
        }

        public override long getRootAvailableSpace(string root)
        {

            StorageFolder s = getStore(root);
            if (s == null)  return 0;
            var properties = s.GetBasicPropertiesAsync().AsTask().ConfigureAwait(false).GetAwaiter().GetResult();
            var filteredProperties = properties.RetrievePropertiesAsync(new[] { "System.FreeSpace" }).AsTask().ConfigureAwait(false).GetAwaiter().GetResult();
            var free = filteredProperties["System.FreeSpace"];
            return Convert.ToInt64(free);
        }

        public override void mkdir(string directory)
        {
            StorageFolder store = getStore(directory);
            string f = nativePathStore(directory);
            store.CreateFolderAsync(f).AsTask().ConfigureAwait(false).GetAwaiter().GetResult();
        }

        public override void deleteFile(string file1) //or Folder
        {
            StorageFolder store = getStore(file1);
            string f = nativePathStore(file1);
            deleteFile(store, f);
        }

        private void deleteFile(StorageFolder store, string f) //or Folder
        {
            try
            {
                if (f.Length == 0 || (f.Length == 1 && f[0] == '\\')) return; //root folder
                var item = store.GetItemAsync(f).AsTask().ConfigureAwait(false).GetAwaiter().GetResult();
                item.DeleteAsync().AsTask().ConfigureAwait(false).GetAwaiter().GetResult();
            }
            catch (Exception err)
            {
                //ignore file deletion errors 
                io.Log.e(err);
            }
        }

        public override bool isHidden(string n1)
        {
            return false;
        }

        public override void setHidden(string n1, bool n2)
        {
            //StorageFolder store = getStore(n1);
        }

        public override bool isTablet()
        {
            return isPhone;
        }

        public override object getDisplayLock()
        {
            return base.getDisplayLock();
        }

        public override long getFileLength(string aFile)
        {
            if (isTempFile(aFile))
            {
                long l2;
                try
                {
                    
                    StorageFile tempFile = getTempFile(aFile);
                    if (tempFile == null)
                    {
                        return -1;
                    }
                    Stream stream = tempFile.OpenReadAsync().AsTask().ConfigureAwait(false).GetAwaiter().GetResult().AsStream();
                    l2 = stream.Length;
                    stream.Dispose();
                }
                catch (Exception)
                {
                    throw;
                }
                return l2;
            }
            StorageFolder store = getStore(aFile);
            string name = nativePathStore(aFile);
            long l1;
            try
            {
                StorageFile file = store.GetFileAsync(name).AsTask().ConfigureAwait(false).GetAwaiter().GetResult();
                Stream stream = file.OpenReadAsync().AsTask().ConfigureAwait(false).GetAwaiter().GetResult().AsStream();
                l1 = stream.Length;
                stream.Dispose();
            }
            catch (Exception)
            {
                throw;
            }
    
            return l1;
        }

        public override bool isDirectory(string file)
        {
            if (isTempFile(file))
            {
                return false;
            }
            StorageFolder store = getStore(file);
            var f = nativePathStore(file);
            try
            {
                if (f.Length == 0 || (f.Length == 1 && f[0] == '\\'))
                    return true; //root folder
                var item = store.GetItemAsync(f).AsTask().ConfigureAwait(false).GetAwaiter().GetResult();
                return item.IsOfType(StorageItemTypes.Folder);
            }
            catch (Exception )
            {
                return false;

            }
        }

        public override bool exists(string file)
        {
            if (isTempFile(file))
            {
                return getTempFile(file) != null;
            }
            StorageFolder store = getStore(file);
            string f = nativePathStore(file);
            return exists(store, f);
        }

        private bool exists(StorageFolder aStore, string aFile)
        {  
            bool fileExists;
            try
            {
                IStorageItem file = aStore.TryGetItemAsync(aFile).AsTask().GetAwaiter().GetResult();
                fileExists = file != null;
            }
            catch (Exception)
            {
                fileExists = false;
            }
            return fileExists;
        }

        public override void rename(string file, string newName)
        {
            StorageFolder store = getStore(file);
            store.RenameAsync(nativePathStore(newName), NameCollisionOption.ReplaceExisting).AsTask().GetAwaiter().GetResult();
        }

        public override char getFileSystemSeparator()
        {
            return '\\';
        }

        public override string getPlatformName()
        {
            return "win";
        }

        public override l10n.L10NManager getLocalizationManager()
        {
            return new SilverlightImplementation_2L10NManagerImpl();
        }

        public override location.LocationManager getLocationManager()
        {
            if (locationManager == null)
            {
                locationManager = new LocationManager();
            }
            return locationManager;
        }

        private SilverlightImageIO imageIO;
        private SimpleOrientationSensor _sensor;
        private static WindowsAsyncView myView;

#if WINDOWS_UWP
          public static DirectXPixelFormat pixelFormat = DirectXPixelFormat.B8G8R8A8UIntNormalized;
#else
        public static DirectXPixelFormat pixelFormat = DirectXPixelFormat.B8G8R8A8UIntNormalized;
#endif
        private NativeGraphics globalGraphics;
        private Windows.Foundation.Point point;
        private static float rawDpiy;
        private StorageFolder folder;
        private string fileName;
        private static float rawDpix;

        public override ui.util.ImageIO getImageIO()
        {
            if (imageIO == null)
            {
                imageIO = new SilverlightImageIO();
            }
            return imageIO;
        }

        public override bool instanceofObjArray(object n1)
        {
            return n1 is string[];
        }

        public override bool instanceofByteArray(object n1)
        {
            return n1 is byte[];
        }

        public override bool instanceofShortArray(object n1)
        {
            return n1 is short[];
        }

        public override bool instanceofLongArray(object n1)
        {
            return n1 is long[];
        }

        public override bool instanceofIntArray(object n1)
        {
            return n1 is int[];
        }

        public override bool instanceofFloatArray(object n1)
        {
            return n1 is float[];
        }

        public override bool instanceofDoubleArray(object n1)
        {
            return n1 is double[];
        }
        public override bool isNativeShareSupported()
        {
            return true;
        }

        public class ShareInfo
        {
            public string text;
            public string image;
            public string mimeType;

            public ShareInfo(string text, string image, string mimeType)
            {
                this.text = text;
                this.image = image;
                this.mimeType = mimeType;
            }
        }

        private ShareInfo shareInfo;

        public override void share(string text, string image, string mimeType, Rectangle sourceRect)
        {
            dispatcher.RunAsync(CoreDispatcherPriority.Normal, () =>
            {
                DataTransferManager dtm = DataTransferManager.GetForCurrentView();
                shareInfo = new ShareInfo(text, image, mimeType);
                dtm.DataRequested += new TypedEventHandler<DataTransferManager, DataRequestedEventArgs>(this.DataRequested);
                shareInfo = new ShareInfo(text, image, mimeType);
                DataTransferManager.ShowShareUI();
            });

        }

        private Windows.UI.Color parseColor(int p)
        
        {
            byte r = (byte)((p >> 16) & 0xff);
            byte g = (byte)((p >> 8) & 0xff);
            byte b = (byte)(p & 0xff);
            return Windows.UI.Color.FromArgb((byte)255, r, g, b);
        }
    

        private int getPropertyAsInt(string name, int defaultValue)
        {
            string val = Display.getInstance().getProperty(name, null);
            if (val == null)
            {
                return defaultValue;
            }
            return int.Parse(val);
        }

        public override void onShow(Form f)
        {
            base.onShow(f);
            int fgColor = getPropertyAsInt("windows.StatusBar.ForegroundColor", f.getUnselectedStyle().getFgColor());
            int bgColor = getPropertyAsInt("windows.StatusBar.BackgroundColor", f.getUnselectedStyle().getBgColor());
            int bgTransparency = getPropertyAsInt("windows.StatusBar.BackgroundOpacity", f.getUnselectedStyle().getBgTransparency());
            dispatcher.RunAsync(CoreDispatcherPriority.Normal, () =>
            {
                if (ApiInformation.IsTypePresent("Windows.UI.ViewManagement.StatusBar"))
                {

                    var statusBar = StatusBar.GetForCurrentView();
                    if (statusBar != null)
                    {
                        statusBar.BackgroundOpacity = bgTransparency / 255.0;
                        statusBar.BackgroundColor = parseColor(bgColor);
                        statusBar.ForegroundColor = parseColor(fgColor);
                    }
                }
            });
            //Mobile customization

           
        }

        private void DataRequested(DataTransferManager sender, DataRequestedEventArgs e)
        {
            DataRequest request = e.Request;
            if (shareInfo == null)
            {
                return;
            }

            request.Data.Properties.Title = Display.getInstance().getProperty("windows.share.title", "Content from " + Display.getInstance().getProperty("AppName", "Codename One App"));
            request.Data.Properties.Description = Display.getInstance().getProperty("windows.share.title","");
            if (shareInfo.image == null)
            {
               request.Data.SetText(shareInfo.text);
            }
            else
            {
                if (shareInfo.text != null)
                {
                    request.Data.Properties.Title = shareInfo.text;
                }
                StorageFile f;
                openInputStream(shareInfo.image, true, out f);
                request.Data.SetBitmap(RandomAccessStreamReference.CreateFromFile(f));

            }
        }

        public override void fillShape(object graphics, Shape shape)
        {
            WindowsGraphics ag = ((NativeGraphics)graphics).destination;
            CanvasPathBuilder p = cn1ShapeToAndroidPath(shape);
            ag.fillPath(p);
        }
        public override void drawShape(object graphics, Shape shape, Stroke stroke)
        {
           
            WindowsGraphics ag = (WindowsGraphics)((NativeGraphics)graphics).destination;
            CanvasPathBuilder p = cn1ShapeToAndroidPath(shape);
            ag.drawPath(p, stroke);
            
                
        }
        private CanvasPathBuilder cn1ShapeToAndroidPath(Shape shape)
        {
            CanvasPathBuilder canvasPath = new CanvasPathBuilder(screen);
            

            PathIterator it = shape.getPathIterator();
            //p.setWindingRule(it.getWindingRule() == com.codename1.ui.geom.PathIterator.WIND_EVEN_ODD ? GeneralPath.WIND_EVEN_ODD : GeneralPath.WIND_NON_ZERO);
            float[] buf = new float[6];
            bool started = false;
            int size = 0;
            while (!it.isDone())
            {
                int type = it.currentSegment(buf);
                switch (type)
                {
                    case PathIterator.SEG_MOVETO:
                        // p.moveTo(buf[0], buf[1]);
                        if (!started)
                        {
                            started = true;
                        } else
                        {
                            canvasPath.EndFigure(CanvasFigureLoop.Open);
                        }
                        canvasPath.BeginFigure(buf[0], buf[1]);
                        
                        break;
                    case PathIterator.SEG_LINETO:
                        //  p.lineTo(buf[0], buf[1]);
                        canvasPath.AddLine(buf[0], buf[1]);
                            break;
                    case PathIterator.SEG_QUADTO:
                        //p.quadTo(buf[0], buf[1], buf[2], buf[3]);
                        canvasPath.AddQuadraticBezier(new Vector2(buf[0], buf[1]), new Vector2(buf[2], buf[3]));
                        break;
                    case PathIterator.SEG_CUBICTO:
                        // p.cubicTo(buf[0], buf[1], buf[2], buf[3], buf[4], buf[5]);
                        canvasPath.AddCubicBezier(new Vector2(buf[0], buf[1]),new Vector2(buf[3], buf[4]), new Vector2(buf[5]));
                        break;
                    case PathIterator.SEG_CLOSE:
                        canvasPath.EndFigure(CanvasFigureLoop.Closed);
                        started = false;
                       // p.close();
                        break;

                }
                size++;
                it.next();
            }

            if (started && size > 0)
            {
                canvasPath.EndFigure(CanvasFigureLoop.Open);
            }

            return canvasPath;
        }
    }
}// end of namespace: com.codename1.impl