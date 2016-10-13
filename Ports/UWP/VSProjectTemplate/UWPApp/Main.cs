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
        public static bool running;
        public static Windows.Storage.StorageFile appArg;
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
        static public void startStatic() {
            ((com.codename1.ui.Display)com.codename1.ui.Display.getInstance()).callSerially(new StartClass());

        }

        public void start()
        {
            startStatic();
        }
        static public void stopStatic() {
            appArg = null;
            ((com.codename1.ui.Display)com.codename1.ui.Display.getInstance()).callSerially(new StopClass());
        }

        public void stop()
        {
            stopStatic();
        }
    }
    class StartClass : object, java.lang.Runnable {
        public void run() {
Main.running = true;
            if (Main.appArg != null)
            {
                string path = com.codename1.impl.SilverlightImplementation.instance.addTempFile(Main.appArg);
                //java.lang.System.@out.println("Setting app arg to " + path + " just before calling start()");
                com.codename1.impl.SilverlightImplementation.instance.setAppArg(path);
            } else
            {
                com.codename1.impl.SilverlightImplementation.instance.setAppArg(null);
            }
            
            Main.i.start();
        }
    }
    class StopClass : object, java.lang.Runnable {
        public void run() {
            Main.running = false;
            
            Main.i.stop();
            //com.codename1.impl.SilverlightImplementation.instance.setAppArg(null);

        }
    }
}
