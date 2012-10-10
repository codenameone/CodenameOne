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
using System.Threading;
using Microsoft.Xna.Framework.Graphics;

namespace com.codename1.impl
{
    public class SilverPeer : global::com.codename1.ui.PeerComponent
    {
        public UIElementRenderer uir
        {
            get
            {
                if (uirInternal == null)
                {
                    ui.Measure(new Size(10000, 10000));
                    uirInternal = new UIElementRenderer(ui, (int)System.Math.Max(1, ui.DesiredSize.Width), (int)System.Math.Max(1, ui.DesiredSize.Height));
                }
                return uirInternal;
            }
        }
        private UIElementRenderer uirInternal;
        public UIElement ui;

        public SilverPeer(UIElement ui)
        {
            this.ui = ui;
            @this(this);
        }

        /*public override bool animate()
        {
        }*/

        public void draw()
        {
            if (uirInternal != null)
            {
                uirInternal.Dispose();
            }
            uirInternal = null;
            repaint();
        }

        public override global::System.Object calcPreferredSize()
        {
            global::com.codename1.ui.geom.Dimension response = null;
            using (AutoResetEvent are = new AutoResetEvent(false))
            {
                System.Windows.Deployment.Current.Dispatcher.BeginInvoke(() =>
                {
                    ui.Measure(new Size(10000, 10000));
                    response = new global::com.codename1.ui.geom.Dimension();
                    response.@this((int)System.Math.Round(ui.DesiredSize.Width),
                        (int)System.Math.Round(ui.DesiredSize.Height));
                    are.Set();
                });
                are.WaitOne();
            }

            return response;
        }

        public override void deinitialize()
        {
            base.deinitialize();
        }

        public override void initComponent()
        {
            base.initComponent();
        }

        public override void paint(global::com.codename1.ui.Graphics n1)
        {
            SilverlightImplementation.instance.drawUIR(this);
        }
    }
}
