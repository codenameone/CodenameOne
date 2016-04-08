using System;
using System.Collections.Generic;
using System.Linq;
using System.Text;
using System.Threading.Tasks;
using System.Windows;

namespace UWPApp
{
    class Main : object,global::java.lang.Runnable
    {
        public static global::com.codename1.tests.hellowin.HelloWindows i;
        public Main()
        {
            //@this();
            ((com.codename1.ui.Display)com.codename1.ui.Display.getInstance()).callSerially(this);
        }
        public void run()
        {
        i = new global::com.codename1.tests.hellowin.HelloWindows();
            //i.@this();
            i.init(this);
        }
        public void start() {
            ((com.codename1.ui.Display)com.codename1.ui.Display.getInstance()).callSerially(new StartClass());

        }
        public void stop() {
            ((com.codename1.ui.Display)com.codename1.ui.Display.getInstance()).callSerially(new StopClass());
        }
    }
    class StartClass : object, java.lang.Runnable {
        public void run() {
Main.i.start();
        }
    }
    class StopClass : object, java.lang.Runnable {
        public void run() {
            Main.i.stop();
        }
    }
}
