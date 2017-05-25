using IKVM.Attributes;
using IKVM.Internal;
using System;
using System.Linq;
using System.Reflection;
using Windows.ApplicationModel;
using Windows.ApplicationModel.Activation;
using Windows.UI.Xaml;
using Windows.UI.Xaml.Controls;
using Windows.UI.Xaml.Media;
#if WINDOWS_UWP
using Windows.UI.Xaml.Navigation;
#else
using Windows.UI.Xaml.Media.Animation;
using Windows.UI.Xaml.Navigation;
#endif


namespace UWPApp
{
   /// <summary>
    /// Provides application-specific behavior to supplement the default Application class.
    /// </summary>
    public sealed partial class App : Application
    {
#if WINDOWS_PHONE_APP
        private TransitionCollection transitions;
#endif
        /// <summary>
        /// Initializes the singleton application object.  This is the first line of authored code
        /// executed, and as such is the logical equivalent of main() or WinMain().
        /// </summary>
        public App()
        {
#if WINDOWS_UWP
             Microsoft.ApplicationInsights.WindowsAppInitializer.InitializeAsync(
                Microsoft.ApplicationInsights.WindowsCollectors.Metadata |
                Microsoft.ApplicationInsights.WindowsCollectors.Session);
#endif
            this.InitializeComponent();
            this.Suspending += this.OnSuspending;
        }

        private void startCN1(string args)
        {
#if DEBUG
            if (System.Diagnostics.Debugger.IsAttached)
            {
                this.DebugSettings.EnableFrameRateCounter = true;
            }
#endif

            if (args.StartsWith("cn1push:"))
            {
                Main.pushLaunchArg = args.Substring(8);
            }

            Frame rootFrame = Window.Current.Content as Frame;
            // Do not repeat app initialization when the Window already has content,
            // just ensure that the window is active
            if (rootFrame == null)
            {

                // Create a Frame to act as the navigation context and navigate to the first page
                rootFrame = new Frame();
#if WINDOWS_UWP
                rootFrame.NavigationFailed += OnNavigationFailed;
#endif
                //   rootFrame.Background = new ImageBrush
                //    {
                //        Stretch = Windows.UI.Xaml.Media.Stretch.UniformToFill,
                //        ImageSource = new Windows.UI.Xaml.Media.Imaging.BitmapImage { UriSource = new Uri("ms-appx:///res/SplashScreen.png") },
                //    };
                // TODO: change this value to a cache size that is appropriate for your application
                rootFrame.CacheSize = 1;

                //if (e.PreviousExecutionState == ApplicationExecutionState.Terminated)
                //{
                //    // TODO: Load state from previously suspended application
                //}

                // Place the frame in the current Window
                Window.Current.Content = rootFrame;
            }

            if (rootFrame.Content == null)
            {
#if WINDOWS_PHONE_APP
                // Removes the turnstile navigation for startup.
                if (rootFrame.ContentTransitions != null)
                {
                    this.transitions = new TransitionCollection();
                    foreach (var c in rootFrame.ContentTransitions)
                    {
                        this.transitions.Add(c);
                    }
                }

                rootFrame.ContentTransitions = null;
                rootFrame.Navigated += this.RootFrame_FirstNavigated;
#endif
#if WINDOWS_UWP
                rootFrame.Navigate(typeof(MainPage), args);
#else
                // When the navigation stack isn't restored navigate to the first page,
                // configuring the new page by passing required information as a navigation
                // parameter
                if (!rootFrame.Navigate(typeof(MainPage), e.Arguments))
                {
                    throw new Exception("Failed to create initial page");
                }
#endif
            }

            // Ensure the current window is active
            Window.Current.Activate();
        }

        /// <summary>
        /// Invoked when the application is launched normally by the end user.  Other entry points
        /// will be used when the application is launched to open a specific file, to display
        /// search results, and so forth.
        /// </summary>
        /// <param name="e">Details about the launch request and process.</param>
        protected override void OnLaunched(LaunchActivatedEventArgs e)
        {
            startCN1(e.Arguments);

        }
#if WINDOWS_UWP
         void OnNavigationFailed(object sender, NavigationFailedEventArgs e)
        {
            throw new Exception("Failed to load Page " + e.SourcePageType.FullName);
        }

        protected override void OnActivated(IActivatedEventArgs args)
        {
            if (args.Kind == ActivationKind.Protocol)
            {
                ProtocolActivatedEventArgs eventArgs = args as ProtocolActivatedEventArgs;
                // TODO: Handle URI activation
                // The received URI is eventArgs.Uri.AbsoluteUri
                if (args.PreviousExecutionState != ApplicationExecutionState.Running)
                {
                    startCN1("");
                    Main.appArgStr = eventArgs.Uri.AbsoluteUri;
                } else
                {
                    Main.stopStatic();
                    Main.appArgStr = eventArgs.Uri.AbsoluteUri;
                    Main.startStatic();
                }
            }

        }

        protected override async void OnShareTargetActivated(ShareTargetActivatedEventArgs args)
        {
            ShareTargetActivatedEventArgs shareArgs = args as ShareTargetActivatedEventArgs;
            Windows.ApplicationModel.DataTransfer.ShareTarget.ShareOperation op = shareArgs.ShareOperation;
            Windows.Storage.StorageFile storageArg = null;
            string stringArg = null;
            if (op.Data.Contains(Windows.ApplicationModel.DataTransfer.StandardDataFormats.StorageItems))
            {
                System.Collections.Generic.IReadOnlyList<Windows.Storage.IStorageItem> items = await op.Data.GetStorageItemsAsync();
                Windows.Storage.IStorageItem item = items.First();
                if (item.IsOfType(Windows.Storage.StorageItemTypes.File))
                {
                    storageArg = item as Windows.Storage.StorageFile;
                }
            } else if (op.Data.Contains(Windows.ApplicationModel.DataTransfer.StandardDataFormats.Uri))
            {
                Uri uri = await op.Data.GetUriAsync();
                stringArg = uri.AbsoluteUri;
            }
            
            
                if (args.PreviousExecutionState != ApplicationExecutionState.Running)
            {
                startCN1("");
                if (storageArg != null)
                {
                    Main.appArg = storageArg;
                } else if (stringArg != null)
                {
                    Main.appArgStr = stringArg;
                }
                
            }
            else
            {
                Main.stopStatic();
                if (storageArg != null)
                {
                    Main.appArg = storageArg;
                }
                else if (stringArg != null)
                {
                    Main.appArgStr = stringArg;
                }
                Main.startStatic();
            }
        }

        protected override void OnFileActivated(FileActivatedEventArgs args)
        {
            //int i = 0;
            //i++;
            //System.Diagnostics.Debug.WriteLine("On File Activated");
            
            
            
            if (args.PreviousExecutionState != ApplicationExecutionState.Running )
            {
                // TODO: Load state from previously suspended application
                startCN1("");
                Windows.Storage.IStorageItem item = args.Files.First();
                //System.Diagnostics.Debug.WriteLine("Item is " + item);
                if (item.IsOfType(Windows.Storage.StorageItemTypes.File))
                {
                    Main.appArg = item as Windows.Storage.StorageFile;
                }
            } else
            {
                Windows.Storage.IStorageItem item = args.Files.First();
                //System.Diagnostics.Debug.WriteLine("Item is " + item);
                if (item.IsOfType(Windows.Storage.StorageItemTypes.File))
                {
                    Main.appArg = item as Windows.Storage.StorageFile;
                }
                if (item.IsOfType(Windows.Storage.StorageItemTypes.File))
                {
                    
                    Main.stopStatic();
                    Main.appArg = item as Windows.Storage.StorageFile;
                    Main.startStatic();
                }
            }
            

        }
#endif
#if WINDOWS_PHONE_APP
        /// <summary>
        /// Restores the content transitions after the app has launched.
        /// </summary>
        /// <param name="sender">The object where the handler is attached.</param>
        /// <param name="e">Details about the navigation event.</param>
        private void RootFrame_FirstNavigated(object sender, NavigationEventArgs e)
        {
            var rootFrame = sender as Frame;
            rootFrame.ContentTransitions = this.transitions ?? new TransitionCollection() { new NavigationThemeTransition() };
            rootFrame.Navigated -= this.RootFrame_FirstNavigated;
        }
#endif
        /// <summary>
        /// Invoked when application execution is being suspended.  Application state is saved
        /// without knowing whether the application will be terminated or resumed with the contents
        /// of memory still intact.
        /// </summary>
        /// <param name="sender">The source of the suspend request.</param>
        /// <param name="e">Details about the suspend request.</param>
        private void OnSuspending(object sender, SuspendingEventArgs e)
        {
            var deferral = e.SuspendingOperation.GetDeferral();
            // TODO: Save application state and stop any background activity
            deferral.Complete();
        }
    } 


    // Uncomment this and modify the debug() method for pseudo-debugging in Visual studio.
    // This only works if the project was built with the "windows.injectDebug" build hint.
    // See https://github.com/shannah/CN1UWPPort/wiki/Using-the-injectDebug-flag for more information.
    /* 
    class IKVMDebugger : com.codename1.debug.Debugger
    {
        public static int len = 100;
        public static int pos = 0;
        public static string[] lastLocation = new string[len];
        public static bool inDebugger;
        public static bool activated;

        public override void debug(string className, string methodName, int lineNum)
        {
            lock (this)
            {
                if (inDebugger) return;
                try
                {

                    inDebugger = true;
                    if (activated)
                    {
                        int stopHere = 1;
                    }
                    if ("monitorEnter".Equals(methodName))
                    {
                        int holdsLock = 1;
                    }
                    //Debug.WriteLine(java.lang.Thread.currentThread().getName());
                    if ("EDT".Equals(java.lang.Thread.currentThread().getName()))
                    {
                        if (className.StartsWith("java/lang/Class") && lineNum > 3235 && lineNum < 3240)
                        {
                            activated = true;
                        }
                        //java.util.FoobarTest  testvar = new java.util.FoobarTest();
                        //string testvar2 = testvar.getString();
                        lastLocation[pos] = className + "." + methodName + ":" + lineNum;
                        pos = (pos + 1) % len;
                        lastLocation[pos] = null;
                        //pos = (pos + 1) % len;
                    }
                    else if ("java/util/Properties".Equals(className) || "java/lang/Props".Equals(className) || "java/util/Hashtable".Equals(className))
                    {
                        int noop = 1;

                    }
                }
                finally
                {
                    inDebugger = false;
                }
            }


            //Debug.WriteLine("Hello "+className+" "+methodName+" "+lineNum);
        }
    }
   */
    class IKVMReflectionHelper : RuntimeReflectionHelper
    {

        
        public static void Initialize()
        {
            Instance = new IKVMReflectionHelper();
            java.lang.System.setOut(new DebugPrintStream());
            java.lang.System.setErr(new DebugPrintStream());
            // Uncomment only if using windows.injectDebug build hint and you are debugging
            // the application in Visual Studio.
            //com.codename1.debug.Debugger.setDebugger(new IKVMDebugger());
        }

        private IKVMReflectionHelper()
        {

            java.lang.System.setOut(new DebugPrintStream());
        }

        public override System.String convertToStringImpl(System.Object obj)
        {
            if (obj != null && obj is java.lang.Object)
            {
                return ((java.lang.Object)obj).toString();
            }
            return base.convertToStringImpl(obj);
        }

        public override bool checkEqualsImpl(System.Object a, System.Object b)
        {
            if (a != null && a is java.lang.Object && b != null && b is java.lang.Object)
            {
                return ((java.lang.Object)a).equals((java.lang.Object)b);
            }
            return base.checkEqualsImpl(a, b);
        }

        public override int generateHashCodeImpl(System.Object a)
        {
            if (a != null && a is java.lang.Object)
            {
                return ((java.lang.Object)a).hashCode();
            }
            return base.generateHashCodeImpl(a);
        }

        public override string getCurrentStackTrace()
        {
            return Environment.StackTrace;
        }

        public override String getTimezoneId()
        {
            return TimeZoneInfo.Local.Id;
        }
        public override int getTimezoneOffset(string name, int year, int month, int day, int timeOfDayMillis)
        {
            int hours = timeOfDayMillis / 1000 / 60 / 60;
            int minutes = timeOfDayMillis / 1000 / 60 - hours * 60;
            int seconds = timeOfDayMillis / 1000 - (hours * 60 * 60) - (minutes * 60);
            int millis = timeOfDayMillis % 1000;
            return (int)TimeZoneInfo.FindSystemTimeZoneById(name).GetUtcOffset(new DateTime(year, month, day, hours, minutes, seconds, DateTimeKind.Local)).TotalMilliseconds;

        }
        public override int getTimezoneRawOffset(string name)
        {
            return (int)TimeZoneInfo.FindSystemTimeZoneById(name).GetUtcOffset(new DateTime()).TotalMilliseconds;
        }
        public override bool isTimezoneDST(string name, long millis)
        {
            Int64 ticks = millis * 10000;
            if (ticks < DateTime.MinValue.Ticks) ticks = DateTime.MinValue.Ticks;
            if (ticks > DateTime.MaxValue.Ticks) ticks = DateTime.MaxValue.Ticks;
            return TimeZoneInfo.FindSystemTimeZoneById(name).IsDaylightSavingTime(new DateTime(ticks));
        }

        public override string getOSLanguage()
        {
            string tag = Windows.Globalization.ApplicationLanguages.Languages.First();
            if (tag.IndexOf("-") >= 0)
            {
                tag = tag.Substring(tag.IndexOf("-") + 1);
            }
            return tag;
        }


        public override Module GetTypeModule(Type type)
        {
            System.Diagnostics.Debug.WriteLine("In getTypeModule 1");
            return type.GetTypeInfo().Module;
        }

        public override object[] GetCustomAttributesImpl(Module mod, Type attributeType, bool inherit)
        {
            System.Diagnostics.Debug.WriteLine("In GetCustomAttributesImpl 1");
#if !DEBUG || true
            var name = mod.Name;
            if (mod.Name.Contains("IKVM.OpenJDK") || name.Contains("CodenameOne") || name.Contains("HelloWindows"))
            {
                return new JavaModuleAttribute[] { new JavaModuleAttribute() };
            }
#endif
            var s = mod.GetCustomAttributes(attributeType).ToArray();
            return s;
        }

        public override int SortFieldByTokenImpl(FieldInfo field1, FieldInfo field2)
        {
            return 1;
        }

        public override Module[] GetModules(Assembly assembly)

        {
            //System.Diagnostics.Debug.WriteLine("In getModules 1");
#if WINDOWS_UAP
            return assembly.GetModules();
#else
            return assembly.Modules.ToArray();
#endif
        }

        public override Type getTimeZoneInfo() {
            return typeof(TimeZoneInfo);
        }

        public override RemappedClassAttribute[] GetRemappedClasses(Assembly assambly)
        {
#if !DEBUG || true
            return new[] {
                new RemappedClassAttribute("java.lang.AutoCloseable", typeof(IDisposable)),
                new RemappedClassAttribute("java.lang.Comparable", typeof(IComparable)),
                new RemappedClassAttribute("java.lang.Object", typeof(object)),
                new RemappedClassAttribute("java.lang.String", typeof(string)),
                new RemappedClassAttribute("java.lang.Throwable", typeof(Exception))
            };
#else
            var s = assambly.GetCustomAttributes<RemappedClassAttribute>().ToArray();
            return s;
#endif
        }

        public override Type GetModuleType(Module mod, string className)
        {
            //System.Diagnostics.Debug.WriteLine("In getModuleType 1");
            return mod.Assembly.GetType(className);
        }

        public override Assembly GetCoreAssembly()
        {
            //System.Diagnostics.Debug.WriteLine("In getCoreAssemblt 1");
            return typeof(java.lang.Object).GetTypeInfo().Assembly;
        }

        public override void Mark(object obj)

        {
            //System.Diagnostics.Debug.WriteLine("In Mark " + obj);
        }

        class DebugPrintStream : java.io.PrintStream
        {
            public DebugPrintStream()
                : base(new DebugOutputStream())
            {
            }

            public override void println(object x)
            {
                System.Diagnostics.Debug.WriteLine(x);

            }

            public override void println(string x)
            {
                System.Diagnostics.Debug.WriteLine(x);
            }

            public override void println()
            {
                System.Diagnostics.Debug.WriteLine("");
            }

        }

        class DebugOutputStream : java.io.OutputStream
        {
            public override void write(int b)
            {
                // ignore
            }

            
        }
    }
    
    
}
