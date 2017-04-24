using com.codename1.impl;
using System;
using System.Collections.Generic;
using System.IO;
using System.Linq;
using System.Runtime.InteropServices.WindowsRuntime;
using Windows.Foundation;
using Windows.Foundation.Collections;
using Windows.UI.Xaml;
using Windows.UI.Xaml.Controls;
using Windows.UI.Xaml.Controls.Primitives;
using Windows.UI.Xaml.Data;
using Windows.UI.Xaml.Input;
using Windows.UI.Xaml.Media;
using Windows.UI.Xaml.Navigation;
using IKVM.Runtime;


// The Blank Page item template is documented at http://go.microsoft.com/fwlink/?LinkId=402352&clcid=0x409

namespace UWPApp
{
    /// <summary>
    /// An empty page that can be used on its own or navigated to within a Frame.
    /// </summary>
    public sealed partial class MainPage : Page
    {
        public static String BUILD_KEY = "null";
        public static String PACKAGE_NAME = "com.mycompany.myapp";
        public static String BUILT_BY_USER = "null";
        public static String APP_NAME = "TestProject";
        public static String APP_VERSION = "1.0";
        private Main instance;

        // Constructor
        public MainPage()
        {


            IKVMReflectionHelper.Initialize();
            NativeThreadHelper.setInstance(new NativeThreadHelperImpl());
            ImplementationFactory.getInstance().setImplementation(new SilverlightImplementation());
            //java.io.FileDescriptor.init();
            System.Diagnostics.Debug.WriteLine("About to init4.5");
            InitializeComponent();
            System.Diagnostics.Debug.WriteLine("About to init5.5");
            Loaded += delegate {
                System.Diagnostics.Debug.WriteLine("About to init7.1");

                if (instance == null)
                {
                    //java.io.FileDescriptor.init();
                    System.Diagnostics.Debug.WriteLine("About to init6");

                    try
                    {
                        com.codename1.impl.SilverlightImplementation.setCanvas(this, LayoutRoot);
                        //com.codename1.impl.SilverlightImplementation.setPushCallback(null);
                    }
                    finally { System.Diagnostics.Debug.WriteLine("About to init"); }
 SilverlightImplementation.screen.Loaded += delegate {
                        com.codename1.ui.util.Resources.setFailOnMissingTruetype(false);
                        System.Diagnostics.Debug.WriteLine("Display instance " + typeof(com.codename1.ui.Display));
                        com.codename1.ui.Display.init(null);
                        System.Diagnostics.Debug.WriteLine("after init");
                        com.codename1.ui.Display disp = (com.codename1.ui.Display)com.codename1.ui.Display.getInstance();
                        disp.setProperty("package_name", PACKAGE_NAME);
                        disp.setProperty("built_by_user", BUILT_BY_USER);
                        disp.setProperty("build_key", BUILD_KEY);
                        disp.setProperty("AppName", APP_NAME);
                        disp.setProperty("AppVersion", APP_VERSION);
                        instance = new Main();
                        instance.start();
                    };
                }

            };
            System.Diagnostics.Debug.WriteLine("About to init7");
        }

        //void Current_Deactivated(object sender, DeactivatedEventArgs e)
        //{
        //    instance.stop();
        //}

        //void Current_Activated(object sender, ActivatedEventArgs e)
        //{
        //    if (!com.codename1.impl.SilverlightImplementation.exitLock)
        //    {
        //        instance.start();
        //    }
        // }

        protected override void OnNavigatedFrom(NavigationEventArgs e)
        {
            instance.stop();
        }
    }
    public class Request : com.codename1.io.ConnectionRequest
    {
        public Request()
        {
            System.Diagnostics.Debug.WriteLine("In Request()");
            //@this();
        }

        protected override void readResponse(global::java.io.InputStream n1)
        {
            System.Diagnostics.Debug.WriteLine("In readResponse()");
            java.io.DataInputStream di = new java.io.DataInputStream(n1);
            //di.@this(n1);
            long l = di.readLong();
            com.codename1.io.Preferences.set("DeviceId__$", l);
        }
    }


}
