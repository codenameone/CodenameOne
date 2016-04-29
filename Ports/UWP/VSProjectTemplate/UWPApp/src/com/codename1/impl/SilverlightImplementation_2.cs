using System.IO;
using System.Linq;
using System;
using System.Threading;
using System.Collections.Generic;
using System.Net;
using Windows.Devices.Geolocation;
using System.Diagnostics;
using Microsoft.Graphics.Canvas;
using Windows.UI.Xaml.Controls;
using Windows.UI.Xaml;
using Windows.ApplicationModel;
using System.Threading.Tasks;
using Windows.UI.Core;
using Windows.Phone.UI.Input;
using Windows.UI.Xaml.Input;
using Windows.Foundation;
using Windows.UI.Input;
using Windows.Storage;
using Windows.Devices.Sensors;
using Windows.UI.Xaml.Navigation;
using Windows.ApplicationModel.Contacts;
using Windows.ApplicationModel.Email;
using Windows.Storage.Streams;
using Windows.UI.Xaml.Media;
using Windows.Phone.Devices.Notification;
using Windows.Media.Capture;
using Windows.Media.MediaProperties;
using Windows.Storage.Pickers;
using Microsoft.Graphics.Canvas.UI.Xaml;
using Microsoft.Graphics.Canvas.Text;
using Windows.Storage.Search;
using System.Runtime.InteropServices.WindowsRuntime;
using com.codename1.ui.animations;
using com.codename1.ui;
using com.codename1.ui.geom;
using com.codename1.impl;
using System.Net.Http;
using Windows.UI;
using Microsoft.Graphics.Canvas.UI;
using Windows.Graphics.Imaging;
using Windows.UI.Xaml.Media.Imaging;
using Windows.Graphics.Display;
using Windows.UI.Text;
using com.codename1.payment;
using Windows.ApplicationModel.Store;
using System.Collections.Concurrent;
using System.Numerics;
using Windows.System;
using Windows.ApplicationModel.Activation;
using System.Text;
using Windows.Devices.Enumeration;
using Windows.ApplicationModel.Core;
using com.codename1.ui.events;
using Microsoft.Graphics.Canvas.Effects;
using Windows.Data.Xml.Dom;
using System.Runtime.InteropServices.WindowsRuntime;


namespace com.codename1.impl
{
    public class NativeGraphics : object
    {
        public WindowsGraphics destination;
        private int clipX, clipY, clipW, clipH;
        private Rectangle actualClip;
        public Rectangle clip
        {
            set
            {
                actualClip = value;
                destination.setClip(value);
                clipX = actualClip.getX();
                clipY = actualClip.getY();
                clipW = actualClip.getWidth();
                clipH = actualClip.getHeight();
            }
            get { return actualClip; }
        }
        private NativeFont actualFont;
        public NativeFont font
        {
            set
            {
                actualFont = value;
                destination.setFont(value.font);
            }
            get { return actualFont; }
        }

        public void resetClip()
        {
            Rectangle r = new Rectangle(0, 0, Convert.ToInt32(SilverlightImplementation.screen.ActualWidth), Convert.ToInt32(SilverlightImplementation.screen.ActualHeight));
            //r.@this(0, 0, Convert.ToInt32(SilverlightImplementation.screen.ActualWidth), Convert.ToInt32(SilverlightImplementation.screen.ActualHeight));
            clip = r;
        }

        public int getClipX() { return clipX; }
        public int getClipY() { return clipY; }
        public int getClipW() { return clipW; }
        public int getClipH() { return clipH; }
    }

    public class DrawStringCache
    {
        public string str;
        public int color;
        public NativeFont font;
        public long lastAccess;

        public override bool Equals(object obj)
        {
            DrawStringCache d = (DrawStringCache)obj;
            return str.Equals(d.str) && color == d.color && font == d.font;
        }

        public override int GetHashCode()
        {
            return str.GetHashCode() + color;
        }
    }

    public class CodenameOneImage 
    {
        public long lastAccess = System.DateTime.Now.Ticks;
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
                initGraphics(); ///
            }

            get { return actualImage; }
        }
        private int width = -1;
        private int height = -1;
        public NativeGraphics graphics = new NativeGraphics();

        

        public void initGraphics() ///
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
                    } catch (System.ArgumentException ex)
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
           //  Debug.WriteLine("FonSize:" + this.font.FontSize + "  " + this.font.ToString());

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
            } catch (System.ArgumentException ex)
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
                Task.Run(() => global::System.Threading.Tasks.Task.Delay(TimeSpan.FromMilliseconds(1))).GetAwaiter().GetResult();
            }
        }
    }

    class EditString : java.lang.Object, java.lang.Runnable
    {
        private global::com.codename1.ui.Component n1;
        private int n2;
        private int n3;
        private string n4;
        private int n5;

        public EditString(global::com.codename1.ui.Component n1, int n2, int n3, string n4, int n5)
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
            ((com.codename1.ui.Form)n1.getComponentForm()).repaint();
        }
    }

    class WaitForNativeEdit : java.lang.Object, java.lang.Runnable
    {
        public virtual void run()
        {
            while (SilverlightImplementation.textInputInstance != null)
            {
                Task.Run(() => global::System.Threading.Tasks.Task.Delay(TimeSpan.FromMilliseconds(1))).GetAwaiter().GetResult();
            }
        }
    }

    public class SoftRef 
    {
        global::System.WeakReference w;
        //java.lang.Object o;

        public SoftRef(object obj)
        {
            w = new WeakReference(obj);
            //o = obj;
        }

        public object get()
        {
            object o = (object)w.Target;
            return o;
            //return o;
        }
    }

    class NetworkOperation 
    {
        private bool responseCompleted;
        private bool postCompleted;
        public HttpWebRequest request;
        ///public HttpClientHandler handler;

        public Stream requestStream
        {
            get
            {
                if (postData == null)
                {
                    request.BeginGetRequestStream(PostCallback, request);
                    while (!postCompleted)
                    {
                        Task.Run(() => System.Threading.Tasks.Task.Delay(TimeSpan.FromMilliseconds(5))).GetAwaiter().GetResult();
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
                        postData.Dispose();
                    }
                    request.BeginGetResponse(ResponseCallback, request);

                    while (!responseCompleted)
                    {
                        Task.Run(() => Task.Delay(TimeSpan.FromMilliseconds(5))).GetAwaiter().GetResult();
                    }
                    if (resp == null)
                    {
                        global::java.io.IOException io = null;  new global::java.io.IOException();
                        if (error != null)
                        {
                            io = new global::java.io.IOException(error.Message);
                            //io.@this(SilverlightImplementation.toJava(error.Message));
                        }
                        else
                        {
                            io = new global::java.io.IOException("Null response");
                            //io.@this(SilverlightImplementation.toJava("Null response"));
                        }
                        throw io;
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
    /*
    class InputStreamProxy : java.io.InputStream
    {
        private Stream internalStream;
        private long markedPosition = 0;

        public InputStreamProxy(Stream internalStream)
        {
            //base.@this();
            this.internalStream = internalStream;
        }

        public override int available()
        {
            return 0;
        }

        //public override void close()
        //{
        //    internalStream.Dispose();
        // }

        public override void close()
        {
            internalStream.Dispose();
        }

        public override bool markSupported()
        {
            return internalStream.CanSeek;
        }
        public override void mark(int readlimit)
        {
            markedPosition = internalStream.Position;
        }

        public override int read()
        {
            return internalStream.ReadByte();
        }

        public override int read(byte[] n1)
        {
            return read(n1, 0, n1.Length);

        }

        public override int read(byte[] sb, int n2, int n3)
        {
            int v = internalStream.Read((byte[])(Array)sb, n2, n3);
            if (v <= 0)
            {
                return -1;

            }
            return v;
        }

        public override void reset()
        {
            internalStream.Seek(markedPosition, SeekOrigin.Begin);
        }

        public override long skip(long n)
        {
            internalStream.Seek(n, SeekOrigin.Current);
            return n;
        }
    }
    
    class OutputStreamProxy : java.io.OutputStream
    {
        private Stream internalStream;
        public OutputStreamProxy(Stream internalStream)
        {
            //base.@this();
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
                lock (internalStream)
                    internalStream.Dispose();
            }
            catch (Exception)
            {
                //internalStream = null;
            }
            internalStream = null;
        }

        public override void flush()
        {
            if (internalStream == null)
            {
                return;
            }
            try
            {
                lock (internalStream)
                    internalStream.Flush();
            }
            catch (Exception)
            {
                // internalStream = null;
            }
        }

        public override void write(byte[] n1)
        {
            write(n1, 0, n1.Length);
        }

        public override void write(byte[] n1, int n2, int n3)
        {
            if (internalStream != null)
                lock (internalStream)
                    internalStream.Write(n1, n2, n3);
        }

        public override void write(int n1)
        {
            if (internalStream != null)
                lock (internalStream)
                    internalStream.WriteByte((byte)n1);

        }

    }
    */
    public class StringFontPair
    {
        public String str;
        public NativeFont font;

        public StringFontPair(String str, NativeFont font)
        {
            this.str = str;
            this.font = font;
        }

        public override bool Equals(Object stfpo)
        {
            StringFontPair stfp = (StringFontPair)stfpo;
            return str.Equals(stfp.str) && font.Equals(stfp.font);
        }

        public override int GetHashCode()
        {
            return str.GetHashCode();
        }
    }

    public class SilverlightPeer : com.codename1.ui.PeerComponent
    {
        public FrameworkElement element;
        private bool lightweightMode;
        public WebView webview;
        private object peerImage;
        public SilverlightPeer(FrameworkElement element) : base(element)
        {
            this.element = element;
            //@this(null);
        }

        public override bool isFocusable()
        {
            return true;
        }

        public override void setFocus(bool n1)
        {
        }

        protected override com.codename1.ui.geom.Dimension calcPreferredSize()
        {
            int w = 0;
            int h = 0;
            using (AutoResetEvent are = new AutoResetEvent(false))
            {
                SilverlightImplementation.dispatcher.RunAsync(CoreDispatcherPriority.Normal, () =>
                {
                    element.Measure(new Size(1000000, 1000000));
                    w = SilverlightImplementation.screen.ConvertDipsToPixels((float)(element.DesiredSize.Width * SilverlightImplementation.scaleFactor), CanvasDpiRounding.Round);
                    h = SilverlightImplementation.screen.ConvertDipsToPixels((float)(element.DesiredSize.Height * SilverlightImplementation.scaleFactor), CanvasDpiRounding.Round);
                    are.Set();
                }).AsTask().ConfigureAwait(false).GetAwaiter().GetResult();
                are.WaitOne();
            }
            com.codename1.ui.geom.Dimension d = new com.codename1.ui.geom.Dimension(Math.Max(2, w), Math.Max(2, h));
            //d.@this(Math.Max(2, w), Math.Max(2, h));
            return d;
        }

        protected override void onPositionSizeChange()
        {
            int width = getWidth();
            int height = getHeight();
            int x = getAbsoluteX();
            int y = getAbsoluteY();
            if (width > 0 && height > 0)
            {
                SilverlightImplementation.dispatcher.RunAsync(CoreDispatcherPriority.Normal, () =>
                  {
                      if (SilverlightImplementation.cl.Children.Contains(element))
                      {
                          Canvas.SetLeft(element, x / SilverlightImplementation.scaleFactor);
                          Canvas.SetTop(element, y / SilverlightImplementation.scaleFactor);
                          element.Width = width / SilverlightImplementation.scaleFactor;
                          element.Height = height / SilverlightImplementation.scaleFactor;
                      }
                  }).AsTask().ConfigureAwait(false).GetAwaiter().GetResult();
            }
        }

        protected override void initComponent()
        {
            int width = getWidth();
            int height = getHeight();
            int x = getAbsoluteX();
            int y = getAbsoluteY();
            SilverlightImplementation.dispatcher.RunAsync(CoreDispatcherPriority.Normal, () =>
            {
                if (!SilverlightImplementation.cl.Children.Contains(element))
                {
                    SilverlightImplementation.cl.Children.Add(element);
                }
                Canvas.SetLeft(element, x / SilverlightImplementation.scaleFactor);
                Canvas.SetTop(element, y / SilverlightImplementation.scaleFactor);
                element.Width = width / SilverlightImplementation.scaleFactor;
                element.Height = height / SilverlightImplementation.scaleFactor;
            }).AsTask().ConfigureAwait(false).GetAwaiter().GetResult();
        }

        protected override void deinitialize()
        {
            SilverlightImplementation.dispatcher.RunAsync(CoreDispatcherPriority.Normal, () =>
            {
                if (SilverlightImplementation.cl.Children.Contains(element))
                {
                    // peerImage = generatePeerImage();
                    SilverlightImplementation.cl.Children.Remove(element);
                }
            }).AsTask().ConfigureAwait(false).GetAwaiter().GetResult();
        }

        protected override void setLightweightMode(bool n1)
        {
            if (lightweightMode != n1)
            {
                lightweightMode = n1;
                SilverlightImplementation.dispatcher.RunAsync(CoreDispatcherPriority.Normal, () =>
                {
                    if (n1)
                    {
                        element.Visibility = Visibility.Collapsed;
                    }
                    else
                    {
                        element.Visibility = Visibility.Visible;
                    }
                }).AsTask().ConfigureAwait(false).GetAwaiter().GetResult();
                ((com.codename1.ui.Form)getComponentForm()).repaint();
            }
        }

        private async void loadWebViewToStream(WebView webview, IRandomAccessStream stream)
        {
            await Task.Delay(TimeSpan.FromMilliseconds(10));
            await webview.CapturePreviewToStreamAsync(stream);
            await stream.FlushAsync();
            stream.Seek(0);
        }

        public static byte[] ReadFully(Stream input)
        {
            using (MemoryStream ms = new MemoryStream())
            {
                input.CopyTo(ms);
                return ms.ToArray();
            }
        }

        protected override com.codename1.ui.Image generatePeerImage()
        {
            int width = getWidth();
            int height = getHeight();
            if (width <= 0 || height <= 0)
            {
                width = getPreferredW();
                height = getPreferredH();
            }
            if (element.Parent == null)
            {
                if (peerImage != null)
                {
                    return (com.codename1.ui.Image)peerImage;
                }
                return com.codename1.ui.Image.createImage(width, height);
            }
            CodenameOneImage img = new CodenameOneImage();
            //img.@this();
            img.name = "PeerImage: " + element.ToString();
            IRandomAccessStream stream = new InMemoryRandomAccessStream();
            CanvasBitmap cb = null;
            using (AutoResetEvent are = new AutoResetEvent(false))
            {
                SilverlightImplementation.dispatcher.RunAsync(CoreDispatcherPriority.Normal, () =>
              {
                  if (element is WebView)
                  {
                      try
                      {
                          Task.Delay(TimeSpan.FromTicks(4).Duration()).GetAwaiter().GetResult();
                          ((WebView)element).CapturePreviewToStreamAsync(stream).AsTask().GetAwaiter().GetResult();
                          stream.FlushAsync().AsTask().GetAwaiter().GetResult();
                          stream.Seek(0);
                          Task.Delay(TimeSpan.FromMilliseconds(10)).GetAwaiter().GetResult();
                          cb = CanvasBitmap.LoadAsync(SilverlightImplementation.screen, stream).AsTask().GetAwaiter().GetResult();
                      }
                      catch (Exception)
                      {

                          throw;
                      }

                  }
                  else
                  {
                      RenderTargetBitmap renderTargetBitmap = new RenderTargetBitmap();
                      renderTargetBitmap.RenderAsync(element).AsTask().ConfigureAwait(false).GetAwaiter().GetResult();
                      byte[] buf = renderTargetBitmap.GetPixelsAsync().AsTask().ConfigureAwait(false).GetAwaiter().GetResult().ToArray();
                      cb = CanvasBitmap.CreateFromBytes(SilverlightImplementation.screen, buf, width, height, SilverlightImplementation.pixelFormat, SilverlightImplementation.screen.Dpi);
                  }
                  img.image = new CanvasRenderTarget(SilverlightImplementation.screen, cb.SizeInPixels.Width, cb.SizeInPixels.Height, cb.Dpi);
                  img.graphics.destination.drawImage(cb, 0, 0);
                  img.graphics.destination.dispose();
                  are.Set();
              }).AsTask().ConfigureAwait(false).GetAwaiter().GetResult();
                are.WaitOne();
            }
            return com.codename1.ui.Image.createImage(img);

            // return com.codename1.ui.Image.createImage(width, height);
        }

        protected override bool shouldRenderPeerImage()
        {
            return lightweightMode || !isInitialized();
        }
    }

    public class CN1Media :  com.codename1.media.Media
    {
        private MediaElement elem;
        private SilverlightPeer peer;
        private bool video;
        private java.lang.Runnable onComplete;
        private Canvas cl;

        public CN1Media(string uri, bool video, java.lang.Runnable onComplete, Canvas cl)
        {
            this.cl = cl;
            using (AutoResetEvent are = new AutoResetEvent(false))
            {
                SilverlightImplementation.dispatcher.RunAsync(CoreDispatcherPriority.Normal, () =>
                {
                    elem = new MediaElement();
                    cl.Children.Add(elem);
                    elem.MediaOpened += elem_MediaOpened;
                    elem.Source = new Uri(uri, UriKind.RelativeOrAbsolute);
                    this.video = video;
                    this.onComplete = onComplete;
                    elem.MediaEnded += elem_MediaEnded;
                    are.Set();
                }).AsTask().ConfigureAwait(false).GetAwaiter().GetResult();
                are.WaitOne();
            }
        }

        void elem_MediaOpened(object sender, RoutedEventArgs e)
        {
            //ready = true;
        }

        public CN1Media(Stream s, string mime, java.lang.Runnable onComplete, Canvas cl)
        {
            this.cl = cl;
            using (AutoResetEvent are = new AutoResetEvent(false))
            {
                SilverlightImplementation.dispatcher.RunAsync(CoreDispatcherPriority.Normal, () =>
                {
                    elem = new MediaElement();
                    cl.Children.Add(elem);
                    elem.MediaOpened += elem_MediaOpened;
                    elem.SetSource(s.AsRandomAccessStream(), "");
                    video = mime.StartsWith("video");
                    this.onComplete = onComplete;
                    elem.MediaEnded += elem_MediaEnded;
                    are.Set();
                }).AsTask().ConfigureAwait(false).GetAwaiter().GetResult();
                are.WaitOne();
            }
        }

        void elem_MediaEnded(object sender, RoutedEventArgs e)
        {
            if (onComplete != null)
            {
                com.codename1.ui.Display disp = (com.codename1.ui.Display)com.codename1.ui.Display.getInstance();
                disp.callSerially(onComplete);
            }
        }

        public virtual void prepare()
        {
        }

        public virtual void play()
        {
            /*while (!ready)
            {
                Thread.Sleep(10);
            }*/
            SilverlightImplementation.dispatcher.RunAsync(CoreDispatcherPriority.Normal, () =>
            {
                elem.Play();
            }).AsTask().ConfigureAwait(false).GetAwaiter().GetResult();
        }

        public virtual void pause()
        {
            SilverlightImplementation.dispatcher.RunAsync(CoreDispatcherPriority.Normal, () =>
            {
                elem.Pause();
            }).AsTask().ConfigureAwait(false).GetAwaiter().GetResult();
        }

        public virtual void cleanup()
        {
            SilverlightImplementation.dispatcher.RunAsync(CoreDispatcherPriority.Normal, () =>
            {
                if (elem != null)
                {
                    cl.Children.Remove(elem);
                    elem = null;
                }
            }).AsTask().ConfigureAwait(false).GetAwaiter().GetResult();
        }

        public virtual int getTime()
        {
            int v = 0;
            using (AutoResetEvent are = new AutoResetEvent(false))
            {
                SilverlightImplementation.dispatcher.RunAsync(CoreDispatcherPriority.Normal, () =>
                {
                    v = Convert.ToInt32(elem.Position.TotalMilliseconds);
                    are.Set();
                }).AsTask().ConfigureAwait(false).GetAwaiter().GetResult();
                are.WaitOne();
            }
            return v;
        }

        public virtual void setTime(int n1)
        {
            SilverlightImplementation.dispatcher.RunAsync(CoreDispatcherPriority.Normal, () =>
            {
                elem.Position = TimeSpan.FromMilliseconds(n1);
            }).AsTask().ConfigureAwait(false).GetAwaiter().GetResult();
        }

        public virtual int getDuration()
        {
            int v = 0;
            using (AutoResetEvent are = new AutoResetEvent(false))
            {
                SilverlightImplementation.dispatcher.RunAsync(CoreDispatcherPriority.Normal, () =>
                {
                    v = Convert.ToInt32(elem.NaturalDuration.TimeSpan.TotalMilliseconds);
                    are.Set();
                }).AsTask().GetAwaiter().GetResult();
                are.WaitOne();
            }
            return v;
        }

        public virtual void setVolume(int n1)
        {
            SilverlightImplementation.dispatcher.RunAsync(CoreDispatcherPriority.Normal, () =>
            {
                elem.Volume = ((double)n1) / 100.0;
            }).AsTask().ConfigureAwait(false).GetAwaiter().GetResult();
        }

        public virtual int getVolume()
        {
            int v = 0;
            using (AutoResetEvent are = new AutoResetEvent(false))
            {
                SilverlightImplementation.dispatcher.RunAsync(CoreDispatcherPriority.Normal, () =>
                {

                    v = Convert.ToInt32(elem.Volume * 100.0);
                    are.Set();
                }).AsTask().ConfigureAwait(false).GetAwaiter().GetResult();
                are.WaitOne();
            }
            return v;
        }

        public virtual bool isPlaying()
        {
            bool b = false;
            using (AutoResetEvent are = new AutoResetEvent(false))
            {
                SilverlightImplementation.dispatcher.RunAsync(CoreDispatcherPriority.Normal, () =>
                {
                    b = elem.CurrentState == MediaElementState.Playing || elem.CurrentState == MediaElementState.Buffering;
                    are.Set();
                }).AsTask().ConfigureAwait(false).GetAwaiter().GetResult();
                are.WaitOne();
            }
            return b;
        }

        public virtual com.codename1.ui.Component getVideoComponent()
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

        public virtual void setNativePlayerMode(bool nativePlayer)
        {
        }

        public virtual bool isNativePlayerMode()
        {
            return false;
        }

        public virtual void setVariable(string str, object obj)
        {
        }

        public virtual global::System.Object getVariable(string str)
        {
            return null;
        }
    }

    public class LocationManager : com.codename1.location.LocationManager
    {

        private Geolocator watcher;
        private Geoposition lastPosition;

        public LocationManager()
        {
            //base.@this();
        }

        protected override void bindListener()
        {

            if (watcher == null)
            {
                watcher = new Geolocator()
                {
                    MovementThreshold = 1,
                    DesiredAccuracy = PositionAccuracy.High,
                    ReportInterval = (uint)TimeSpan.FromSeconds(1).TotalMilliseconds,  ///FA 1s is used in track/navi apps
                };
            }
            this.watcher.StatusChanged += new TypedEventHandler<Geolocator, StatusChangedEventArgs>(watcher_StatusChanged);
            this.watcher.PositionChanged += new TypedEventHandler<Geolocator, PositionChangedEventArgs>(watcher_PositionChanged);
        }

        void watcher_StatusChanged(Geolocator sender, StatusChangedEventArgs eventArgs)
        {
            switch (eventArgs.Status)
            {
                case PositionStatus.Disabled:
                    setStatus(OUT_OF_SERVICE);
                    break;
                case PositionStatus.Initializing:
                case PositionStatus.NoData:
                    setStatus(TEMPORARILY_UNAVAILABLE);
                    break;
                case PositionStatus.Ready:
                    setStatus(AVAILABLE);
                    break;
            }
        }

        async void watcher_PositionChanged(Geolocator sender, PositionChangedEventArgs e)
        {
            lastPosition = await sender.GetGeopositionAsync();
            if (lastPosition != null && lastPosition.Coordinate.AltitudeAccuracy.HasValue)
            {
                ((com.codename1.location.LocationListener)getLocationListener()).locationUpdated(convert(lastPosition));
            }
        }

        protected override void clearListener()
        {
            if (watcher != null)
            {
                watcher.StatusChanged -= new TypedEventHandler<Geolocator, StatusChangedEventArgs>(watcher_StatusChanged);
                watcher.PositionChanged -= new TypedEventHandler<Geolocator, PositionChangedEventArgs>(watcher_PositionChanged);
            }
        }

        public override com.codename1.location.Location getCurrentLocation()
        {
            if (lastPosition != null)
            {
                return convert(lastPosition);
            }
            return null;
        }

        public override com.codename1.location.Location getLastKnownLocation()
        {
            if (lastPosition != null)
            {
                return convert(lastPosition);
            }
            return null;
        }

        private com.codename1.location.Location convert(Geoposition position)
        {
            com.codename1.location.Location location = new com.codename1.location.Location();
            //location.@this();
            location.setTimeStamp(position.Coordinate.Timestamp.UtcTicks / 10000);
            location.setLatitude(position.Coordinate.Point.Position.Latitude);
            location.setLongitude(position.Coordinate.Point.Position.Longitude);
            location.setAltitude((float)position.Coordinate.Point.Position.Altitude);
            location.setDirection((float)position.Coordinate.Heading);
            location.setVelocity((float)position.Coordinate.Speed);
            location.setAccuracy((float)position.Coordinate.Accuracy); ///FA vertical is more usufull AltitudeAccuracy);
            location.setStatus(getStatus());
            return location;
        }
    }

    public class SilverlightImageIO : com.codename1.ui.util.ImageIO
    {
        public SilverlightImageIO()
        {
            //@this();
        }

        public override bool isFormatSupported(string n1)
        {
            return com.codename1.ui.util.ImageIO.FORMAT_JPEG.Equals(n1) || com.codename1.ui.util.ImageIO.FORMAT_PNG.Equals(n1);
        }

        public override void save(java.io.InputStream image, java.io.OutputStream response, string format, int width, int height, float quality)
        {
            CanvasBitmapFileFormat fileFormat = CanvasBitmapFileFormat.Png;
            if (format.Equals(FORMAT_JPEG))
            {
                fileFormat = CanvasBitmapFileFormat.Jpeg;
            }
            CodenameOneImage img = (CodenameOneImage)SilverlightImplementation.instance.createImage(image);
            CodenameOneImage scaledImage = (CodenameOneImage)SilverlightImplementation.instance.scale(img, width, height);
            InMemoryRandomAccessStream ms = new InMemoryRandomAccessStream();
            scaledImage.image.SaveAsync(ms, fileFormat, quality).AsTask().ConfigureAwait(false).GetAwaiter().GetResult();
            ms.Seek(0);
            byte[] buf = new byte[ms.Size];
            var dr = new Windows.Storage.Streams.DataReader(ms);
            dr.LoadAsync((uint)ms.Size).AsTask().ConfigureAwait(false).GetAwaiter().GetResult();
            dr.ReadBytes(buf);
            response.write(buf);
        }

        protected override void saveImage(com.codename1.ui.Image image, java.io.OutputStream response, string format, float quality)
        {
            CanvasBitmapFileFormat fileFormat = CanvasBitmapFileFormat.Png;
            if (format.Equals(FORMAT_JPEG))
            {
                fileFormat = CanvasBitmapFileFormat.Jpeg;
            }
            CodenameOneImage img = (CodenameOneImage)image.getImage();
            CanvasBitmap cb = img.image;
            InMemoryRandomAccessStream ms = new InMemoryRandomAccessStream();
            cb.SaveAsync(ms, fileFormat, quality).AsTask().ConfigureAwait(false).GetAwaiter().GetResult(); ;
            ms.Seek(0);
            byte[] buf = new byte[ms.Size];
            var dr = new Windows.Storage.Streams.DataReader(ms);
            dr.LoadAsync((uint)ms.Size).AsTask().ConfigureAwait(false).GetAwaiter().GetResult(); ;
            dr.ReadBytes(buf);
            response.write(buf);
        }
    }

    public class ImageHelper
    {
        public static string GetContentType(byte[] imageBytes)
        {
            imageFormatDecoders.Keys.OrderByDescending(x => x.Length);
            foreach (var kvPair in imageFormatDecoders)
            {
                if (IsMatch(imageBytes, kvPair.Key))
                {
                    return kvPair.Value;
                }
            }
            return "unknown";
        }

        static bool IsMatch(byte[] array, byte[] candidate)
        {
            if (candidate.Length > (array.Length))
                return false;

            for (int i = 0; i < candidate.Length; i++)
                if (array[i] != candidate[i])
                    return false;

            return true;
        }

        private static Dictionary<byte[], string> imageFormatDecoders = new Dictionary<byte[], string>()
        {
            { new byte[]{ 0x42, 0x4D }, "image/x-ms-bmp"},
            { new byte[]{ 0x47, 0x49, 0x46, 0x38, 0x37, 0x61 }, "image/gif" },
            { new byte[]{ 0x47, 0x49, 0x46, 0x38, 0x39, 0x61 }, "image/gif" },
            { new byte[]{ 0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A }, "image/png" },
            { new byte[]{ 0xff, 0xd8 }, "image/jpeg" },
        };
    }
    //very fast MemoryRandomAcceess class but only for byte[] array input !!!
    // and for class that is not using stream olny IBuffer!!!
    class MemoryRandomAccessStream : IRandomAccessStream {
        private byte[] m_Bytes;
        private int m_Position = 0;

        public MemoryRandomAccessStream(byte[] bytes) {
            this.m_Bytes = bytes;
        }

        public IInputStream GetInputStreamAt(ulong position) {
            throw new NotSupportedException();
        }

        public IOutputStream GetOutputStreamAt(ulong position) {
            throw new NotSupportedException();
        }

        public ulong Size
        {
            get { return (ulong)this.m_Bytes.Length; } // m_InternalStream.Length; }
            set { throw new NotSupportedException(); }
        }

        public bool CanRead
        {
            get { return true; }
        }

        public bool CanWrite
        {
            get { return false; }
        }

        public IRandomAccessStream CloneStream() {
            throw new NotSupportedException();
        }

        public ulong Position
        {
            get { return (ulong)m_Position; } // this m_this.m_InternalStream.Position; }
        }

        public void Seek(ulong position) {
            m_Position = (int)position;
        }

        public void Dispose() {
        }

        public Windows.Foundation.IAsyncOperationWithProgress<IBuffer, uint> ReadAsync(IBuffer buffer, uint count, InputStreamOptions options) {

            return AsyncInfo.Run<IBuffer, uint>(async (token, progress) =>
            {
                byte[] b = new byte[count];
                Array.Copy(m_Bytes, m_Position, b,0, b.Length);
                progress.Report(100);
                return b.AsBuffer(); 
            });
        }

        public Windows.Foundation.IAsyncOperation<bool> FlushAsync() {
            throw new NotSupportedException();
        }

        public Windows.Foundation.IAsyncOperationWithProgress<uint, uint> WriteAsync(IBuffer buffer) {
            throw new NotSupportedException();
        }
    }
}// end of namespace: com.codename1.impl
