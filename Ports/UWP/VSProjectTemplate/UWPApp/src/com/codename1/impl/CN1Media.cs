using System;
using System.IO;
using System.Threading;
using Windows.UI.Core;
using Windows.UI.Xaml;
using Windows.UI.Xaml.Controls;
using Windows.UI.Xaml.Media;

namespace com.codename1.impl
{
    public class CN1Media : media.Media
    {
        private MediaElement elem;
        private SilverlightPeer peer;
        private bool video;
        //private java.lang.Runnable onComplete;
        private Canvas cl;
        private System.Collections.Generic.List<java.lang.Runnable> completionHandlers;

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
                    //this.onComplete = onComplete;
                    if (onComplete != null)
                    {
                        addCompletionHandler(onComplete);
                    }
                    elem.MediaEnded += elem_MediaEnded;
                    are.Set();
                }).AsTask().ConfigureAwait(false).GetAwaiter().GetResult();
                are.WaitOne();
            }
        }

        public void addCompletionHandler(java.lang.Runnable onComplete)
        {
            if (completionHandlers == null)
            {
                completionHandlers = new System.Collections.Generic.List<java.lang.Runnable>();
            }
            completionHandlers.Add(onComplete);
        }

        public void removeCompletionHandler(java.lang.Runnable onComplete)
        {
            if (completionHandlers != null)
            {
                completionHandlers.Remove(onComplete);
            }
        }

        void elem_MediaOpened(object sender, RoutedEventArgs e)
        {
            //ready = true;a
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
                    //this.onComplete = onComplete;
                    if (onComplete != null)
                    {
                        addCompletionHandler(onComplete);
                    }
                    elem.MediaEnded += elem_MediaEnded;
                    are.Set();
                }).AsTask().ConfigureAwait(false).GetAwaiter().GetResult();
                are.WaitOne();
            }
        }

        void elem_MediaEnded(object sender, RoutedEventArgs e)
        {
            if (completionHandlers != null)
            {
                ui.Display disp = ui.Display.getInstance();
                System.Collections.Generic.List<java.lang.Runnable> toRun = new System.Collections.Generic.List<java.lang.Runnable>();
                toRun.AddRange(completionHandlers);
                foreach (var completionHandler in toRun)
                {
                    disp.callSerially(completionHandler);
                }
                
            }
        }

        public virtual void prepare()
        {
        }

        public virtual void play()
        {
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
                elem.Volume = n1 / 100.0;
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

        public virtual ui.Component getVideoComponent()
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

        public virtual object getVariable(string str)
        {
            return null;
        }
    }
}
