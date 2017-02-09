using com.codename1.ui;
using Microsoft.Graphics.Canvas;
using System;
using System.Runtime.InteropServices.WindowsRuntime;
using Windows.Foundation;
using Windows.Storage.Streams;
using Windows.UI.Core;
using Windows.UI.Xaml;
using Windows.UI.Xaml.Controls;
using Windows.UI.Xaml.Media.Imaging;
using System.Threading.Tasks;

namespace com.codename1.impl
{
    public class SilverlightPeer : PeerComponent
    {
        public FrameworkElement element;
        private bool lightweightMode;
        private object peerImage = null;

        public SilverlightPeer(FrameworkElement element):base(element)
        {
            this.element = element;
        }

        public override bool isFocusable()
        {
            return true;
        }

        protected override ui.geom.Dimension calcPreferredSize()
        {
            int w = 0;
            int h = 0;
            SilverlightImplementation.dispatcher.RunAsync(CoreDispatcherPriority.Normal, () =>
            {
                element.Measure(new Size(1000000, 1000000));
                w = SilverlightImplementation.screen.ConvertDipsToPixels((float)(element.DesiredSize.Width * SilverlightImplementation.scaleFactor), CanvasDpiRounding.Round);
                h = SilverlightImplementation.screen.ConvertDipsToPixels((float)(element.DesiredSize.Height * SilverlightImplementation.scaleFactor), CanvasDpiRounding.Round);

            }).AsTask().ConfigureAwait(false).GetAwaiter().GetResult();
            ui.geom.Dimension d = new ui.geom.Dimension(Math.Max(2, w), Math.Max(2, h));
            return d;
        }

         protected override void onPositionSizeChange()
        {
            layoutPeer();
        }

        public void layoutPeer()
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
                        element.SetValue(Canvas.ZIndexProperty, 100);
                        Canvas.SetLeft(element, x / SilverlightImplementation.scaleFactor);
                        Canvas.SetTop(element, y / SilverlightImplementation.scaleFactor);
                        element.Width = width / SilverlightImplementation.scaleFactor;
                        element.Height = height / SilverlightImplementation.scaleFactor;
                    }
                }).AsTask();
            }
        }

        protected override void initComponent()
        {
            SilverlightImplementation.dispatcher.RunAsync(CoreDispatcherPriority.Normal, () =>
            {
                if (!SilverlightImplementation.cl.Children.Contains(element))
                {
                    SilverlightImplementation.cl.Children.Add(element);
                }
            }).AsTask();
            layoutPeer();
            setPeerImage(null);
        }

        protected override void deinitialize()
        {

            peerImage = generatePeerImage();
            setPeerImage((ui.Image)peerImage);
            SilverlightImplementation.dispatcher.RunAsync(CoreDispatcherPriority.Normal, () =>
            {
                if (SilverlightImplementation.cl.Children.Contains(element))
                {
                    SilverlightImplementation.cl.Children.Remove(element);
                }
            }).AsTask();
        }

        protected override void setLightweightMode(bool l)
        {
            if (lightweightMode != l)
            {
                lightweightMode = l;
                SilverlightImplementation.dispatcher.RunAsync(CoreDispatcherPriority.Normal, async () =>
                {
                    if (l)
                    {
                        if (element is WebView && element.Visibility == Visibility.Visible && element.RenderSize.Width > 0 && element.RenderSize.Height > 0)
                        {
                            IRandomAccessStream stream = new InMemoryRandomAccessStream();
                            CanvasBitmap cb = null;
                            await ((WebView)element).CapturePreviewToStreamAsync(stream);
                            await stream.FlushAsync();
                            stream.Seek(0);
                            lastWebViewPeerImage = await CanvasBitmap.LoadAsync(SilverlightImplementation.screen, stream);

                        }
                        element.Visibility = Visibility.Collapsed;
                    }
                    else
                    {
                        element.Visibility = Visibility.Visible;
                    }
                }).AsTask().ConfigureAwait(false).GetAwaiter().GetResult();
                getComponentForm().repaint();
            }
        }

        private CanvasBitmap lastWebViewPeerImage;

        protected override ui.Image generatePeerImage()
        {
            if (element is MediaElement)
            {
                return null;
            }
            
            int width = getWidth();
            int height = getHeight();
            if (width <= 0 || height <= 0)
            {
                width = getPreferredW();
                height = getPreferredH();
            }
            CodenameOneImage img = new CodenameOneImage();
            img.name = "PeerImage: " + element.ToString();
            IRandomAccessStream stream = new InMemoryRandomAccessStream();
            CanvasBitmap cb = null;
            SilverlightImplementation.dispatcher.RunAsync(CoreDispatcherPriority.Normal, async () =>
            {
                if (element is WebView)
                {
                    if (lastWebViewPeerImage != null)
                    {
                        cb = lastWebViewPeerImage;
                    }
                    else if (element.Visibility == Visibility.Visible && element.RenderSize.Width > 0 && element.RenderSize.Height > 0)
                    {
                        await ((WebView)element).CapturePreviewToStreamAsync(stream);
                        await stream.FlushAsync();
                        stream.Seek(0);
                        cb = await CanvasBitmap.LoadAsync(SilverlightImplementation.screen, stream);
                    } else
                    {
                        return;
                    }
                }
                else
                {
                    RenderTargetBitmap renderTargetBitmap = new RenderTargetBitmap();
                    await renderTargetBitmap.RenderAsync(element);
                    byte[] buf = renderTargetBitmap.GetPixelsAsync().AsTask().ConfigureAwait(false).GetAwaiter().GetResult().ToArray();

                    cb = CanvasBitmap.CreateFromBytes(SilverlightImplementation.screen, buf, width, height,
                    SilverlightImplementation.pixelFormat, SilverlightImplementation.screen.Dpi);
                }
                img.image = new CanvasRenderTarget(SilverlightImplementation.screen, (int)cb.SizeInPixels.Width, (int)cb.SizeInPixels.Height, cb.Dpi);
                img.graphics.destination.drawImage(cb, 0, 0);
                img.graphics.destination.dispose();

            }).AsTask().GetAwaiter().GetResult();
            if (cb == null)
            {
                return null;
            }
            return ui.Image.createImage(img);
        }

        protected override bool shouldRenderPeerImage()
        {
            // You can't take a screenshot of a media element like you can with other 
            // XAML elements. 
            return lightweightMode || !(element is MediaElement) && !isInitialized();
        }
    }
}
