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

namespace com.codename1.impl
{
    public class XNAFont : java.lang.Object
    {
        private global::System.Collections.Generic.Dictionary<string, int> width = new global::System.Collections.Generic.Dictionary<string, int>();

        public int fontHeight = -1;
        public int face { get; set; }
        public int size { get; set; }
        public int style { get; set; }

        public void applyFont(TextBlock ui)
        {
            if (size == com.codename1.ui.Font._fSIZE_1SMALL)
            {
                ui.FontSize = 15;
            }
            else
            {
                if (size == com.codename1.ui.Font._fSIZE_1LARGE)
                {
                    ui.FontSize = 54;
                }
                else
                {
                    ui.FontSize = 24;
                }
            }
            if ((style & com.codename1.ui.Font._fSTYLE_1BOLD) == com.codename1.ui.Font._fSTYLE_1BOLD)
            {
                ui.FontWeight = FontWeights.Bold;
            }
            else
            {
                ui.FontWeight = FontWeights.Normal;
            }
            if ((style & com.codename1.ui.Font._fSTYLE_1ITALIC) == com.codename1.ui.Font._fSTYLE_1ITALIC)
            {
                ui.FontStyle = FontStyles.Italic;
            }
            else
            {
                ui.FontStyle = FontStyles.Normal;
            }
        }

        public void applyFont(TextBox ui)
        {
            if (size == com.codename1.ui.Font._fSIZE_1SMALL)
            {
                ui.FontSize = 15;
            }
            else
            {
                if (size == com.codename1.ui.Font._fSIZE_1LARGE)
                {
                    ui.FontSize = 54;
                }
                else
                {
                    ui.FontSize = 24;
                }
            }
            if ((style & com.codename1.ui.Font._fSTYLE_1BOLD) == com.codename1.ui.Font._fSTYLE_1BOLD)
            {
                ui.FontWeight = FontWeights.Bold;
            }
            else
            {
                ui.FontWeight = FontWeights.Normal;
            }
            if ((style & com.codename1.ui.Font._fSTYLE_1ITALIC) == com.codename1.ui.Font._fSTYLE_1ITALIC)
            {
                ui.FontStyle = FontStyles.Italic;
            }
            else
            {
                ui.FontStyle = FontStyles.Normal;
            }
        }

        public int stringWidth(string s, TextBlock tb)
        {
            if (width.ContainsKey(s))
            {
                return width[s];
            }
            int response = 0;
            using (AutoResetEvent are = new AutoResetEvent(false))
            {
                System.Windows.Deployment.Current.Dispatcher.BeginInvoke(() =>
                {
                    applyFont(tb);
                    tb.Text = s;
                    tb.Measure(new Size(10000, 10000));
                    response = (int)System.Math.Round(tb.DesiredSize.Width);
                    width.Add(s, response);
                    are.Set();
                });
                are.WaitOne();
            }

            return response;
        }
    }

}
