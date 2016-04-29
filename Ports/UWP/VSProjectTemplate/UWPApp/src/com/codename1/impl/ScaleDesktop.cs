using System;
using System.Collections.Generic;
using System.Linq;
using System.Text;
using System.Threading.Tasks;
using Windows.Graphics.Display;

namespace com.codename1.impl
{
    class ScaleDesktop
    {
        private static double scaleFactor;
        public static double ScaleFactor
        {
            get {

                switch (Windows.Graphics.Display.DisplayInformation.GetForCurrentView().ResolutionScale)
                {
                    case ResolutionScale.Invalid:
                    case ResolutionScale.Scale100Percent:
                    default:
                        scaleFactor = 1.0;
                        break;
                    case ResolutionScale.Scale140Percent:
                        scaleFactor = 1.4;
                        break;
                    case ResolutionScale.Scale150Percent:
                        scaleFactor = 1.5;
                        break;
                    case ResolutionScale.Scale160Percent:
                        scaleFactor = 1.6;
                        break;
                    case ResolutionScale.Scale180Percent:
                        scaleFactor = 1.8;
                        break;
                    case ResolutionScale.Scale225Percent:
                        scaleFactor = 2.25;
                        break;
                }
                return scaleFactor;
            }
            set { scaleFactor = value; }
        }
    }
}
