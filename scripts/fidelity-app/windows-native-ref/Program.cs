/*
 * Copyright (c) 2026, Codename One and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.  Codename One designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Oracle in the LICENSE file that accompanied this code.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 2 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 *
 * You should have received a copy of the GNU General Public License version
 * 2 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Please contact Codename One through http://www.codenameone.com/ if you
 * need additional information or have any questions.
 */
// Windows (WinUI 3 / Fluent) native reference app for the Codename One fidelity suite.
//
// Desktop counterpart to ios-native-ref/NativeRef.swift. It renders real WinUI controls in
// a real, composited window and writes reference tiles plus a capture-manifest.json.
//
// The manifest is not bookkeeping here, it is the point. `windows-latest` is Windows
// SERVER, where Mica and Acrylic fall back to a plain solid brush rather than failing, and
// where Segoe UI Variable -- the font every WinUI control uses -- may be absent. Capture a
// reference under those conditions and you get Fluent-with-the-materials-off, commit it as
// "the Windows 11 reference", tune the Codename One theme until it matches the fallback,
// and ship a theme that looks wrong on every actual Windows 11 machine. The fidelity
// metric cannot detect this: two sides that degrade into the same flat render score HIGH.
//
// So this app refuses to produce a reference set it cannot vouch for. In probe mode it
// answers the environment questions and exits; in capture mode it additionally writes
// tiles, but only after the same assertions pass.
// TWO STATIC-ANALYSIS RULES FIRE ALL OVER THIS FILE AND ARE ANSWERED HERE ONCE, because a
// review thread is not read by whoever edits this next.
//
// "Calls to unmanaged code -- replace with managed code if possible." There is no managed
// equivalent for any of it. This program exists to photograph what the Windows compositor
// actually put on screen: PrintWindow with PW_RENDERFULLCONTENT, BitBlt with CAPTUREBLT,
// DwmFlush, TrackMouseEvent, SystemParametersInfo. .NET exposes none of those, and a
// managed screenshot API would answer a different question -- "what does XAML think it
// drew" rather than "what did DWM composite" -- which is precisely the substitution that
// made an earlier version of this app report success while capturing unstyled controls.
//
// "Generic catch clause." Deliberate, and narrowing them would make this app worse at its
// one job. It is a capture probe whose contract is that it refuses to produce a reference
// set it cannot vouch for: every failure has to become a recorded blocker, not an escaping
// exception that kills the process with no manifest and no log. The broad catches are each
// paired with a documented fallback (a zero rect, "(unidentifiable)", false, "") or with a
// blocker that names the stage and the HRESULT. A typed list would have to enumerate every
// COM failure a hosted runner can produce, and the ones it missed would crash silently
// instead of being reported -- trading a described failure for an undescribed one.
//
// The floating-point finding on RasterizationScale was real and is fixed at its site.

using System.Runtime.InteropServices;
using System.Text;
using Microsoft.UI.Composition.SystemBackdrops;
using Microsoft.UI.Xaml;
using Microsoft.UI.Xaml.Controls;
using Microsoft.UI.Xaml.Controls.Primitives;
using Microsoft.UI.Xaml.Media;
using Microsoft.UI;
using Microsoft.UI.Windowing;
using Windows.UI.ViewManagement;

namespace Cn1NativeRef;

public static class Program
{
    [STAThread]
    static void Main(string[] args)
    {
        Microsoft.UI.Xaml.Application.Start(_ => new App());
    }
}

public partial class App : Application
{
    public App()
    {
        // Generated from App.xaml, which is where the style dictionary is merged. Doing that
        // merge from C# instead -- in this constructor, and then in OnLaunched -- crashed the
        // process with 0xC000027B both times, because without a XAML file the project
        // produces no app resources.pri and XamlControlsResources cannot load its dictionary.
        InitializeComponent();
    }

    /// Merged in OnLaunched, not in the constructor.
    ///
    /// Merges WinUI's default style dictionary.
    ///
    /// The template projects do this in App.xaml, which this app does not have -- it is pure
    /// C# with no XAML file at all. Without it there are no control styles: a Button renders
    /// as a flat square grey rectangle instead of a #FDFDFD Fluent capsule with 4px corners,
    /// and nothing errors, because an unstyled control is still a perfectly valid control.
    ///
    /// This was three commits of chasing the wrong cause. The controls were unstyled, and the
    /// resource index was missing too, so the missing PRI looked like the explanation; it
    /// was not, and the build output carrying both framework PRIs while the button stayed
    /// (194,194,194) is what finally ruled it out.
    ///
    /// Doing this in the App constructor crashed the process outright with 0xC000027B, a
    /// stowed WinRT exception: Application.Resources is not ready to be touched that early.

    private Window _window;
    private readonly List<string> _blockers = new();
    private string _outDir;
    private bool _occluded;
    private string _stage = "(not started)";
    private string _clientBackground = "(not sampled)";
    private IntPtr _hwnd;
    private FrameworkElement _probeControl;
    private Grid _tileHost;
    private bool _animationsDisabled;
    private byte[] _lastWrittenTile;
    private Windows.Foundation.Rect _probeBounds;
    private bool _isProbe;

    /// The tile the widget is anchored top-left in. Mirrors tile_width_px / tile_height_px
    /// in fidelity-tests.yaml; if those change, this must change with them.
    private const int TileW = 240;
    private const int TileH = 56;

    private int _tilesWritten;

    /// Hash of each "<id>_normal_<appearance>" tile, and the states that came out
    /// byte-identical to it.
    ///
    /// Identical is not automatically wrong: a platform genuinely may not restyle a
    /// control for a state (AppKit draws no hover at all). It is only wrong when it is a
    /// SURPRISE, so it is recorded in the manifest instead of being left for whoever
    /// later wonders why a theme's hover rule scores the same either way.
    private readonly Dictionary<string, string> _normalHashes = new();
    private readonly List<string> _identicalToNormal = new();

    /// Backdrop colour sampled from each appearance's tiles, and the check that the two
    /// are not the same. See AssertAppearancesDiffer().
    private readonly Dictionary<string, string> _backdropByAppearance = new();

    /// One row of the desktop matrix. Kind is the native_win key in fidelity-tests.yaml,
    /// and the ids and states are that file's too: the two lists must agree or the
    /// comparator pairs a CN1 render against nothing.
    private sealed record Spec(string Id, string Kind, string[] States);

    private static readonly Spec[] Specs =
    {
        new("DesktopButton",       "winui_button",        new[] { "normal", "hover", "pressed", "disabled" }),
        new("DesktopAccentButton", "winui_button_accent", new[] { "normal", "hover", "pressed", "disabled" }),
        new("DesktopTextField",    "winui_textbox",       new[] { "normal", "hover", "disabled" }),
        new("DesktopCheckBox",     "winui_checkbox",      new[] { "normal", "selected", "hover", "disabled" }),
        new("DesktopRadioButton",  "winui_radiobutton",   new[] { "normal", "selected", "hover", "disabled" }),
        new("DesktopSwitch",       "winui_toggleswitch",  new[] { "normal", "selected", "hover", "disabled" }),
        new("DesktopSlider",       "winui_slider",        new[] { "normal", "hover", "disabled" }),
        new("DesktopProgressBar",  "winui_progressbar",   new[] { "normal" }),
        new("DesktopComboBox",     "winui_combobox",      new[] { "normal", "hover", "disabled" }),

        // Second wave. Windows carries three rows the other two references cannot: its menu
        // bar and menu item are ordinary Controls that render into a view, and so is its
        // ToolTip, where the AppKit and GTK equivalents are window-server surfaces. The
        // scrollbar is here and not on macOS for the same reason -- see the DesktopScrollBar
        // note in fidelity-tests.yaml for what was measured.
        new("DesktopScrollBar",    "winui_scrollbar",       new[] { "normal" }),
        new("DesktopSeparator",    "winui_separator",       new[] { "normal" }),
        new("DesktopGroupBox",     "winui_groupbox",        new[] { "normal" }),
        new("DesktopStepper",      "winui_numberbox",       new[] { "normal", "disabled" }),
        new("DesktopLinkButton",   "winui_hyperlinkbutton", new[] { "normal", "hover", "disabled" }),
        new("DesktopSearchField",  "winui_autosuggestbox",  new[] { "normal", "disabled" }),
        new("DesktopListRow",      "winui_listviewitem",    new[] { "normal", "selected" }),
        new("DesktopTabs",         "winui_tabview",         new[] { "normal" }),
        new("DesktopToolbar",      "winui_commandbar",      new[] { "normal" }),
        new("DesktopDisclosure",   "winui_expander",        new[] { "normal" }),
        new("DesktopMenuBar",      "winui_menubar",         new[] { "normal" }),
        new("DesktopMenuItem",     "winui_menuflyoutitem",  new[] { "normal", "hover", "disabled" }),
        new("DesktopTooltip",      "winui_tooltip",         new[] { "normal" }),
    };

    /// Controls with no natural width: layout always assigns one, so the tile width is the
    /// honest answer. Kept in sync BY HAND with FULL_WIDTH_KINDS in the other reference apps
    /// and FULL_WIDTH_IDS in DesktopTileRunner. If one side stretches a control and the
    /// other does not, the comparison is between two geometries and the score means nothing.
    private static bool IsFullWidth(string kind) =>
        kind is "winui_slider" or "winui_progressbar" or "winui_textbox"
            // Second wave, same rule: a search field measures to its placeholder, and a row,
            // a box, a tab strip, a command bar and a menu bar are containers that take the
            // width they are given.
            or "winui_autosuggestbox" or "winui_listviewitem"
            or "winui_groupbox" or "winui_tabview" or "winui_commandbar" or "winui_menubar"
            or "winui_separator";

    /// Controls with no natural HEIGHT, the same rule on the other axis. A group box is a
    /// frame around other things -- left to measure itself it collapses onto its own header
    /// and draws no frame, which is a heading rather than a group box -- and a vertical
    /// scrollbar is defined by its length. Kept in step BY HAND with FULL_HEIGHT_KINDS in the
    /// macOS and GNOME references and FULL_HEIGHT_IDS in DesktopTileRunner.
    private static bool IsFullHeight(string kind) =>
        kind is "winui_groupbox" or "winui_scrollbar";

    private static FrameworkElement MakeWidget(string kind) => kind switch
    {
        "winui_button" => new Button { Content = "Button" },
        // The accent-filled button is a STYLE in WinUI, not a control: AccentButtonStyle is
        // the documented resource key, and tinting a plain Button by hand produces a colour
        // the system never draws.
        "winui_button_accent" => new Button
        {
            Content = "Button",
            Style = (Style)Application.Current.Resources["AccentButtonStyle"],
        },
        "winui_textbox" => new TextBox { Text = "Text" },
        "winui_checkbox" => new CheckBox { Content = "Check" },
        "winui_radiobutton" => new RadioButton { Content = "Radio" },
        "winui_toggleswitch" => new ToggleSwitch(),
        "winui_slider" => new Slider { Minimum = 0, Maximum = 1, Value = 0.5, StepFrequency = 0.01 },
        "winui_progressbar" => new ProgressBar { Minimum = 0, Maximum = 1, Value = 0.6 },
        "winui_combobox" => MakeComboBox(),

        // A ScrollBar in its always-visible form with the thumb at the top covering two
        // fifths of the track. Both sides have to agree about where the thumb is before
        // anything about its colour or shape can be compared, and the CN1 side is drawn at
        // the same proportion and offset.
        "winui_scrollbar" => new ScrollBar
        {
            Orientation = Orientation.Vertical,
            Minimum = 0,
            Maximum = 100,
            Value = 0,
            ViewportSize = 40,
            IndicatorMode = ScrollingIndicatorMode.MouseIndicator,
            Visibility = Visibility.Visible,
        },
        "winui_separator" => MakeSeparator(),
        "winui_groupbox" => MakeGroupBox(),
        // NumberBox with its spin buttons shown inline, which is the WinUI stepper. Without
        // SpinButtonPlacementMode it is a plain number field and the control under
        // comparison would be missing half of itself.
        "winui_numberbox" => new NumberBox
        {
            Value = 1,
            Minimum = 0,
            Maximum = 10,
            SpinButtonPlacementMode = NumberBoxSpinButtonPlacementMode.Inline,
        },
        "winui_hyperlinkbutton" => new HyperlinkButton { Content = "Link" },
        "winui_autosuggestbox" => new AutoSuggestBox
        {
            Text = "Search",
            QueryIcon = new SymbolIcon(Symbol.Find),
        },
        "winui_listviewitem" => new ListViewItem { Content = "Row" },
        "winui_tabview" => MakeTabView(),
        "winui_commandbar" => MakeCommandBar(),
        "winui_expander" => new Expander { Header = "Details", Content = new TextBlock { Text = "Item" } },
        "winui_menubar" => MakeMenuBar(),
        "winui_menuflyoutitem" => new MenuFlyoutItem { Text = "Open" },
        // A WinUI ToolTip is an ordinary Control and renders into a view, which is why this
        // row exists here and nowhere else: the AppKit and GTK tooltips are separate windows
        // the capture path cannot see.
        "winui_tooltip" => new ToolTip { Content = "Tooltip" },
        _ => null,
    };

    private static FrameworkElement MakeSeparator()
    {
        // WinUI has no Separator control for content: the platform draws a horizontal rule as a
        // one-pixel Border in DividerStrokeColorDefaultBrush, which is what its own settings
        // pages use between groups. MenuFlyoutSeparator exists but is a menu primitive with
        // menu insets, so it would be measuring the wrong thing.
        // The brush comes from a Style carrying a {ThemeResource}, NOT from
        // Application.Current.Resources. That read resolves against the application
        // dictionary once and does no element-theme resolution, so the dark pass captured the
        // LIGHT theme's translucent black over the dark tile and the rule came out darker
        // than the surface behind it. Same trap, and same fix, as TileHostStyle.
        return new Border
        {
            Height = 1,
            HorizontalAlignment = HorizontalAlignment.Stretch,
            VerticalAlignment = VerticalAlignment.Center,
            Style = (Style)Application.Current.Resources["SeparatorRuleStyle"],
        };
    }

    private static FrameworkElement MakeGroupBox()
    {
        // WinUI has no GroupBox control. Its headered-content convention is a Border with a
        // caption above it, which is what the platform's own settings pages draw and what the
        // GroupBox UIID has to match -- so that is built here rather than a control being
        // substituted from another toolkit's vocabulary.
        var caption = new TextBlock
        {
            Text = "Group",
            Style = (Style)Application.Current.Resources["CaptionTextBlockStyle"],
        };
        var body = new Border
        {
            BorderThickness = new Thickness(1),
            // See MakeSeparator: the frame brush has to come from a {ThemeResource} Style or
            // the dark capture draws it darker than the tile it sits on.
            Style = (Style)Application.Current.Resources["GroupBoxFrameStyle"],
            CornerRadius = new CornerRadius(4),
            Padding = new Thickness(8),
            Child = new TextBlock { Text = "Item" },
        };
        var panel = new StackPanel { Orientation = Orientation.Vertical, Spacing = 4 };
        panel.Children.Add(caption);
        panel.Children.Add(body);
        return panel;
    }

    private static TabView MakeTabView()
    {
        // Not closable, and no add button. A WinUI TabView is a document-tab control and
        // shows a close affordance on every tab by default; a Codename One Tabs has no such
        // thing, so leaving them on compares two tabs against two tabs plus two buttons and
        // charges the difference to the theme.
        var tv = new TabView { IsAddTabButtonVisible = false };
        tv.TabItems.Add(new TabViewItem { Header = "One", IsClosable = false });
        tv.TabItems.Add(new TabViewItem { Header = "Two", IsClosable = false });
        tv.SelectedIndex = 0;
        return tv;
    }

    private static CommandBar MakeCommandBar()
    {
        var bar = new CommandBar { DefaultLabelPosition = CommandBarDefaultLabelPosition.Right };
        bar.Content = new TextBlock
        {
            Text = "Title",
            Margin = new Thickness(12, 0, 0, 0),
            VerticalAlignment = VerticalAlignment.Center,
        };
        return bar;
    }

    private static MenuBar MakeMenuBar()
    {
        var bar = new MenuBar();
        var file = new MenuBarItem { Title = "File" };
        file.Items.Add(new MenuFlyoutItem { Text = "Open" });
        bar.Items.Add(file);
        return bar;
    }

    private static ComboBox MakeComboBox()
    {
        var c = new ComboBox();
        c.Items.Add("Option");
        c.SelectedIndex = 0;
        return c;
    }

    /// Applies one state.
    ///
    /// Two different mechanisms on purpose. Enabled and checked are real PROPERTIES, so they
    /// are set directly and WinUI resolves the visuals itself. Hover and pressed have no
    /// property -- they exist only as visual states the input system would normally drive --
    /// so they go through VisualStateManager.
    ///
    /// The state NAMES are per control and not guessable: a Button is "PointerOver", a
    /// CheckBox is "UncheckedPointerOver" or "CheckedPointerOver" because its check and
    /// interaction states are one combined group. So each is tried in turn and the result of
    /// GoToState is CHECKED -- a name that does not exist returns false and silently leaves
    /// the control in Normal, which would write a "hover" tile identical to normal and call
    /// the theme faithful when it had never been tested.
    private bool ApplyState(FrameworkElement widget, string state, string kind, string tileName)
    {
        switch (state)
        {
            case "normal":
                return true;
            case "selected":
                if (widget is ToggleSwitch ts) { ts.IsOn = true; return true; }
                if (widget is CheckBox cb) { cb.IsChecked = true; return true; }
                if (widget is RadioButton rb) { rb.IsChecked = true; return true; }
                if (widget is ListViewItem lvi) { lvi.IsSelected = true; return true; }
                _blockers.Add($"{tileName}: {kind} has no selected state");
                return false;
            case "disabled":
                if (widget is Control dc) { dc.IsEnabled = false; return true; }
                widget.IsHitTestVisible = false;
                return true;
            case "hover":
            case "pressed":
            {
                if (widget is not Control control)
                {
                    _blockers.Add($"{tileName}: {kind} is not a Control, so it has no visual states");
                    return false;
                }
                // "MouseOver" and "Dragging" are the ScrollBar template's own names: that
                // control's CommonStates predate the PointerOver vocabulary and were never
                // renamed. Tried after the modern names rather than instead of them, and a
                // name that does not exist simply returns false and falls through to the
                // next -- the blocker below is what fires when none of them matched.
                string[] candidates = state == "hover"
                    ? new[] { "PointerOver", "UncheckedPointerOver", "CheckedPointerOver",
                              "MouseOver" }
                    : new[] { "Pressed", "UncheckedPressed", "CheckedPressed", "Dragging" };
                foreach (var name in candidates)
                {
                    if (VisualStateManager.GoToState(control, name, false))
                    {
                        return true;
                    }
                }
                _blockers.Add($"{tileName}: none of [{string.Join(", ", candidates)}] is a "
                    + $"visual state of {kind}, so the tile would be a copy of normal");
                return false;
            }
            default:
                _blockers.Add($"{tileName}: unknown state '{state}'");
                return false;
        }
    }

    protected override void OnLaunched(LaunchActivatedEventArgs args)
    {
        _outDir = Environment.GetEnvironmentVariable("NATIVEREF_OUT")
                  ?? throw new InvalidOperationException("NATIVEREF_OUT is not set");
        Directory.CreateDirectory(_outDir);
        _isProbe = (Environment.GetEnvironmentVariable("NATIVEREF_MODE") ?? "probe") != "capture";

        // Before the window exists, so nothing has animated yet.
        bool off = false;
        _animationsDisabled = SystemParametersInfo(SPI_SETCLIENTAREAANIMATION, 0, ref off, SPIF_SENDCHANGE);
        if (!_animationsDisabled)
        {
            _blockers.Add("could not turn system UI animations off (SPI_SETCLIENTAREAANIMATION "
                + $"failed, GetLastError={Marshal.GetLastWin32Error()}); tiles would not be "
                + "reproducible between runs");
        }

        _window = new Window { Title = "cn1-native-ref" };

        // Mica is what makes a Fluent surface look like Windows 11 rather than like a flat
        // grey box. Requesting it tells us nothing -- the request succeeds either way -- so
        // the answer comes from IsSupported plus whether the system says transparency
        // effects are even on, and both go in the manifest.
        var backdrop = new MicaBackdrop();
        bool micaSupported = MicaController.IsSupported();
        _window.SystemBackdrop = backdrop;

        // Always-on-top as well as foreground. The foreground check happens before the
        // BitBlt, and without this a dialog appearing in between could still slide over the
        // window in the gap -- which is a race that would show up as an occasional wrong
        // capture rather than a consistent one, and those are far worse to diagnose.
        _hwnd = WinRT.Interop.WindowNative.GetWindowHandle(_window);
        var idEarly = Microsoft.UI.Win32Interop.GetWindowIdFromWindow(_hwnd);
        if (AppWindow.GetFromWindowId(idEarly).Presenter is OverlappedPresenter op)
        {
            op.IsAlwaysOnTop = true;
        }

        // The window content IS one tile: no padding, no spacing, nothing around it. That
        // is what lets the capture take the client rect as the tile region rather than
        // computing an offset into a larger window, and an offset computed wrong is a whole
        // set shifted by a few pixels that still looks entirely plausible.
        var root = new Grid
        {
            HorizontalAlignment = HorizontalAlignment.Left,
            VerticalAlignment = VerticalAlignment.Top,
        };
        _tileHost = root;
        var button = new Button { Content = "Button" };
        var probeHost = new Grid { Width = TileW, Height = TileH };
        probeHost.Children.Add(button);
        root.Children.Add(probeHost);
        _window.Content = root;

        // Resize the CLIENT area to exactly one tile. AppWindow.ResizeClient sizes the
        // client rather than the outer frame, so the title bar DWM always draws is excluded
        // rather than subtracted afterwards.
        AppWindow.GetFromWindowId(idEarly).ResizeClient(new Windows.Graphics.SizeInt32(TileW, TileH));

        _probeControl = button;
        root.Loaded += async (_, _) => await OnReadyAsync(root, button, micaSupported);
        _window.Activate();
    }

    private async Task OnReadyAsync(FrameworkElement root, Button button, bool micaSupported)
    {
        var ui = new UISettings();
        bool transparency = ui.AdvancedEffectsEnabled;
        bool animations = ui.AnimationsEnabled;
        var accent = ui.GetColorValue(UIColorType.Accent);
        double rasterScale = root.XamlRoot?.RasterizationScale ?? 0;
        string fontFamily = button.FontFamily?.Source ?? "(none)";
        bool segoeVariable = FontIsInstalled("Segoe UI Variable Text");

        // Each of these produces a reference that is subtly, silently wrong rather than
        // one that fails, which is why they are blockers and not warnings.
        if (!micaSupported)
        {
            _blockers.Add("MicaController.IsSupported() is false: this OS cannot draw the "
                + "Mica backdrop, so every Fluent surface here is a flat fallback brush.");
        }
        if (!transparency)
        {
            _blockers.Add("Transparency effects are OFF (UISettings.AdvancedEffectsEnabled "
                + "is false), which disables Mica and Acrylic regardless of OS support. "
                + "This is the Windows Server default.");
        }
        if (!segoeVariable)
        {
            _blockers.Add("Segoe UI Variable is not installed. Every WinUI control would be "
                + "measured in a substitute face, so the text residual would be font "
                + "availability rather than theme fidelity.");
        }
        // Tolerance rather than ==. RasterizationScale is a double, and a display that is
        // 1x in every way that matters can report a value a hair off it; an exact compare
        // would raise a blocker about a scale nobody set. 0 stays an exact compare because
        // it is not a measurement -- it is the sentinel for "XamlRoot was null", assigned
        // literally, and exactly representable.
        if (Math.Abs(rasterScale - 1.0) > 0.001 && rasterScale != 0)
        {
            // Not fatal, but it must be recorded and matched on the Codename One side or
            // the absolute-position metric compares tiles of different sizes.
            Console.WriteLine($"NATIVEREF:WARN rasterization scale is {rasterScale}, not 1.0");
        }

        // Waiting for XAML to paint happens FIRST, before anything awaits, because it is the
        // only part of this that touches XAML at all. CompositionTarget.Rendering must be
        // subscribed from the UI thread, and after an await the continuation is not reliably
        // on it -- the previous run proved that precisely: COMException 0x8001010E,
        // RPC_E_WRONGTHREAD, at stage 'await-frames'. Everything after this point is Win32,
        // which does not care which thread calls it.
        // Where the control actually is, taken on the UI thread while it is safe to ask. The
        // capture then checks THAT rectangle rather than guessing at coordinates.
        try
        {
            // Relative to the WINDOW, not to root. TransformToVisual(root) gives the offset
            // inside root's own coordinate space, which excludes root's margin -- so the
            // button reported 0,0 and the sampler read the page background at both points,
            // #F3F3F3 twice, and called a correctly styled control unstyled.
            var t = _probeControl.TransformToVisual(null);
            var origin = t.TransformPoint(new Windows.Foundation.Point(0, 0));
            _probeBounds = new Windows.Foundation.Rect(origin.X, origin.Y,
                _probeControl.ActualWidth, _probeControl.ActualHeight);
        }
        catch
        {
            _probeBounds = new Windows.Foundation.Rect(0, 0, 0, 0);
        }

        _stage = "await-frames";
        var drawn = new TaskCompletionSource<bool>();
        int frames = 0;
        EventHandler<object> onFrame = null;
        onFrame = (_, _) =>
        {
            if (++frames >= 3)
            {
                CompositionTarget.Rendering -= onFrame;
                drawn.TrySetResult(true);
            }
        };
        CompositionTarget.Rendering += onFrame;
        await Task.WhenAny(drawn.Task, Task.Delay(5000));
        Console.WriteLine($"NATIVEREF:INFO composed frames observed: {frames}");
        if (frames == 0)
        {
            _blockers.Add("XAML never presented a frame, so the client area would be "
                + "captured empty while DWM still draws the title bar -- which is what a "
                + "blocked UI thread looks like");
        }

        // Capture a real tile even in probe mode. A green build is not a rendered one:
        // AppxGeneratePriEnabled is off (see NativeRef.csproj), so WinUI's own .pri files
        // are not expanded into the output, and if that mattered the controls would come
        // back unstyled or absent rather than failing loudly. A picture is the only thing
        // that distinguishes "built" from "drew a Fluent button".
        // Probe verification ALWAYS runs, capture mode included: it is what distinguishes
        // "built" from "drew a Fluent button", and a 60-tile set of unstyled controls would
        // otherwise be committed as a reference.
        await CaptureWindowAsync();

        if (!_isProbe)
        {
            await CaptureMatrixAsync();
        }

        WriteManifest(micaSupported, transparency, animations, accent, rasterScale, fontFamily, segoeVariable);

        foreach (var b in _blockers)
        {
            Console.Error.WriteLine($"NATIVEREF:BLOCKER {b}");
        }
        Console.WriteLine($"NATIVEREF:DONE tiles={_tilesWritten} exit={(_blockers.Count > 0 ? 20 : 0)}");
        Console.Out.Flush();
        Environment.Exit(_blockers.Count > 0 ? 20 : 0);
    }

    /// Turns the system's UI animations off for this session.
    ///
    /// SPI_SETCLIENTAREAANIMATION is what UISettings.AnimationsEnabled reports and what
    /// XAML's theme transitions check, so setting it false makes a control snap to its
    /// state instead of animating into it.
    ///
    /// Needed because the capture must be reproducible byte for byte. With animations on,
    /// two runs of the same commit produced eight tiles that differed -- the check-box
    /// check drawing in, the switch knob sliding, the slider thumb and the progress bar --
    /// because the frame was grabbed at different points along each transition. The
    /// protocol in goldens/README.md is that nondeterminism is fixed in the app or by
    /// pinning an environment knob, never with a tolerance file, and this is the knob.
    private const uint SPI_SETCLIENTAREAANIMATION = 0x1043;
    // SPIF_SENDCHANGE is already declared below, beside the foreground-lock call that
    // also uses it.

    [DllImport("user32.dll", SetLastError = true)]
    private static extern bool SystemParametersInfo(uint action, uint param, ref bool value, uint winIni);

    /// Renders a window's own content into a DC, independent of what is on screen.
    ///
    /// PW_RENDERFULLCONTENT (2) is the flag that makes it work for a DirectComposition
    /// surface, which is what WinUI 3 draws into; without it the call returns an empty
    /// bitmap for exactly this kind of app.
    [DllImport("user32.dll")]
    private static extern bool PrintWindow(IntPtr hwnd, IntPtr hdcBlt, uint flags);

    private const uint PW_RENDERFULLCONTENT = 0x00000002;

    [DllImport("user32.dll")] private static extern IntPtr GetDC(IntPtr hWnd);
    [DllImport("user32.dll")] private static extern int ReleaseDC(IntPtr hWnd, IntPtr hDC);
    [DllImport("gdi32.dll")] private static extern IntPtr CreateCompatibleDC(IntPtr hdc);
    [DllImport("gdi32.dll")] private static extern IntPtr CreateCompatibleBitmap(IntPtr hdc, int w, int h);
    [DllImport("gdi32.dll")] private static extern IntPtr SelectObject(IntPtr hdc, IntPtr h);
    [DllImport("gdi32.dll")] private static extern bool DeleteObject(IntPtr h);
    [DllImport("gdi32.dll")] private static extern bool DeleteDC(IntPtr hdc);
    [DllImport("gdi32.dll")] private static extern bool BitBlt(IntPtr dst, int x, int y, int w, int h,
        IntPtr src, int sx, int sy, uint rop);
    [DllImport("dwmapi.dll")] private static extern int DwmFlush();
    [DllImport("user32.dll")] private static extern bool SetForegroundWindow(IntPtr hWnd);
    [DllImport("user32.dll")] private static extern IntPtr GetForegroundWindow();
    [DllImport("user32.dll")] private static extern bool BringWindowToTop(IntPtr hWnd);
    [DllImport("user32.dll")] private static extern uint GetWindowThreadProcessId(IntPtr hWnd, IntPtr pid);
    [DllImport("user32.dll")] private static extern bool AttachThreadInput(uint attach, uint attachTo, bool fAttach);
    [DllImport("kernel32.dll")] private static extern uint GetCurrentThreadId();
    [DllImport("user32.dll", SetLastError = true)]
    private static extern bool SystemParametersInfo(uint action, uint param, IntPtr pv, uint winIni);
    [DllImport("user32.dll")] private static extern bool PostMessage(IntPtr hWnd, uint msg, IntPtr w, IntPtr l);
    [DllImport("user32.dll")] private static extern bool IsWindow(IntPtr hWnd);
    [DllImport("user32.dll")] private static extern bool SetCursorPos(int x, int y);
    [DllImport("user32.dll")] private static extern void mouse_event(uint flags, uint dx, uint dy, uint data, UIntPtr extra);
    [DllImport("user32.dll")] private static extern void keybd_event(byte vk, byte scan, uint flags, UIntPtr extra);

    private const uint SPI_SETFOREGROUNDLOCKTIMEOUT = 0x2001;
    private const uint SPIF_SENDCHANGE = 0x02;
    private const uint WM_CLOSE = 0x0010;
    private const uint MOUSEEVENTF_LEFTDOWN = 0x0002;
    private const uint MOUSEEVENTF_LEFTUP = 0x0004;
    private const byte VK_ESCAPE = 0x1B;
    private const uint KEYEVENTF_KEYUP = 0x0002;
    [DllImport("user32.dll")] private static extern IntPtr WindowFromPoint(POINT p);
    [DllImport("user32.dll")] private static extern IntPtr GetAncestor(IntPtr hWnd, uint flags);

    [StructLayout(LayoutKind.Sequential)]
    private struct POINT { public int X, Y; }

    private const uint GA_ROOT = 2;
    [DllImport("user32.dll")] private static extern bool ShowWindow(IntPtr hWnd, int nCmdShow);
    [DllImport("user32.dll")] private static extern bool GetWindowRect(IntPtr hWnd, out RECT r);
    [DllImport("user32.dll")] private static extern bool GetClientRect(IntPtr hWnd, out RECT r);
    [DllImport("user32.dll")] private static extern bool ClientToScreen(IntPtr hWnd, ref POINT p);
    [DllImport("user32.dll", CharSet = CharSet.Unicode)] private static extern int GetWindowTextW(IntPtr hWnd, StringBuilder s, int max);

    [StructLayout(LayoutKind.Sequential)]
    private struct RECT { public int Left, Top, Right, Bottom; }

    private const int SW_MINIMIZE = 6;
    private const int SW_SHOW = 5;
    private const int SW_RESTORE = 9;

    private const uint SRCCOPY = 0x00CC0020;
    private const uint CAPTUREBLT = 0x40000000;

    /// Captures the whole matrix, both appearances.
    ///
    /// The window content is swapped per tile and the client area is held at exactly one
    /// tile, so the BitBlt region never has to be reasoned about: what is composited IS the
    /// tile. Each swap waits for real presented frames rather than a fixed sleep, because a
    /// tile captured before its first present is a picture of the previous one.
    private async Task CaptureMatrixAsync()
    {
        foreach (var appearance in new[] { "light", "dark" })
        {
            bool dark = appearance == "dark";
            await OnUiAsync(() =>
            {
                if (_window.Content is FrameworkElement rootEl)
                {
                    rootEl.RequestedTheme = dark ? ElementTheme.Dark : ElementTheme.Light;
                }
                return true;
            });
            await WaitForFramesAsync(3);

            foreach (var spec in Specs)
            {
                foreach (var state in spec.States)
                {
                    var name = $"{spec.Id}_{state}_{appearance}";
                    var widget = await OnUiAsync<FrameworkElement>(() =>
                    {
                        var w = MakeWidget(spec.Kind);
                        if (w is null)
                        {
                            return null;
                        }
                        w.HorizontalAlignment = IsFullWidth(spec.Kind)
                            ? HorizontalAlignment.Stretch
                            : HorizontalAlignment.Left;
                        w.VerticalAlignment = IsFullHeight(spec.Kind)
                            ? VerticalAlignment.Stretch
                            : VerticalAlignment.Top;
                        w.Margin = new Thickness(0);

                        var host = new Grid
                        {
                            Width = TileW,
                            Height = TileH,
                            HorizontalAlignment = HorizontalAlignment.Left,
                            VerticalAlignment = VerticalAlignment.Top,
                            // The tile surface. Left transparent the Mica backdrop shows
                            // through, which is a different surface from the one the CN1
                            // tiles are painted on and would be compared against the wrong
                            // thing.
                            // The backdrop comes from a Style carrying a {ThemeResource}
                            // (App.xaml, TileHostStyle) rather than from a brush read out of
                            // Application.Current.Resources here. That read resolves against
                            // the application dictionary once and does no element-theme
                            // resolution, so the dark half of the set was captured as dark
                            // controls on the LIGHT #F3F3F3 surface -- a backdrop no Windows
                            // 11 application ever shows, and one the CN1 side could never
                            // have matched.
                            Style = (Style)Application.Current.Resources["TileHostStyle"],
                            // NOT RequestedTheme here. The root already carries it and the
                            // tile inherits it.
                        };
                        host.Children.Add(w);
                        _tileHost.Children.Clear();
                        _tileHost.Children.Add(host);
                        return w;
                    });
                    if (widget is null)
                    {
                        _blockers.Add($"{name}: unknown native_win kind '{spec.Kind}'");
                        continue;
                    }

                    // After the tree is live: a visual state cannot be applied to a control
                    // that has not had its template expanded yet, and GoToState returns false
                    // if it is tried too early.
                    await WaitForFramesAsync(2);
                    bool applied = await OnUiAsync(() => ApplyState(widget, state, spec.Kind, name));
                    if (!applied)
                    {
                        continue;
                    }
                    // Applied TWICE, with frames in between, and it is not superstition.
                    // The runner reports animations_enabled: true, and WinUI drives a
                    // PointerOver transition over several frames rather than switching
                    // brushes outright -- so a capture taken too early lands on frame zero
                    // of the transition, which IS the normal appearance. That produced dark
                    // hover tiles for Button, TextBox and ComboBox byte-identical to their
                    // normal tiles while the light ones differed, a difference no platform
                    // has. Re-applying also covers the other candidate cause, a template
                    // re-application resetting the group back to Normal.
                    await WaitForFramesAsync(3);
                    await OnUiAsync(() => ApplyState(widget, state, spec.Kind, name));
                    await WaitForFramesAsync(5);
                    await CaptureSettledTileAsync(name);
                }
            }
        }

        AssertAppearancesDiffer();

        int expected = 0;
        foreach (var spec in Specs)
        {
            expected += spec.States.Length;
        }
        expected *= 2;
        if (_tilesWritten != expected)
        {
            _blockers.Add($"wrote {_tilesWritten} tiles, expected {expected}: a partial set "
                + "would be committed as if it were the whole matrix");
        }
    }

    /// Runs a piece of XAML work on the UI thread and returns its result.
    ///
    /// Every XAML touch in the capture loop goes through this, and that is not defensive
    /// style: the first capture run crashed with RPC_E_WRONGTHREAD (0x8001010E) reading
    /// _window.Content, because an await earlier in the run had resumed the continuation
    /// on a ThreadPool thread and XAML objects have hard thread affinity. Win32 is thread
    /// agnostic and deliberately stays outside this -- GetClientRect and BitBlt do not care
    /// which thread calls them.
    private Task<T> OnUiAsync<T>(Func<T> work)
    {
        var tcs = new TaskCompletionSource<T>();
        var queue = _window.DispatcherQueue;
        if (queue is null || !queue.TryEnqueue(() =>
            {
                try { tcs.TrySetResult(work()); }
                catch (Exception ex) { tcs.TrySetException(ex); }
            }))
        {
            tcs.TrySetException(new InvalidOperationException(
                "the UI dispatcher queue refused the work; the window is gone"));
        }
        return tcs.Task;
    }

    /// Fails the run when the light and dark passes were captured on the same backdrop.
    ///
    /// This is here because it happened. The tile background was read as
    /// Application.Current.Resources["SolidBackgroundFillColorBaseBrush"], which resolves
    /// against the application dictionary once and does no element-theme resolution, so the
    /// controls went dark and the surface behind them stayed light #F3F3F3. Sixty tiles,
    /// zero blockers, a manifest that said "capture", and half the set on a backdrop no
    /// Windows 11 application ever shows.
    ///
    /// Nothing downstream would have caught it either: the CN1 side renders its dark tiles
    /// on the real dark surface, so the pair would simply have scored badly and read as a
    /// theme that needed work.
    private void AssertAppearancesDiffer()
    {
        if (_backdropByAppearance.Count < 2)
        {
            return;
        }
        var distinct = new HashSet<string>(_backdropByAppearance.Values);
        if (distinct.Count == 1)
        {
            _blockers.Add("the light and dark passes were both captured on backdrop "
                + distinct.First() + ": the appearance did not actually change, so half the "
                + "set is mislabelled");
        }
        foreach (var kv in _backdropByAppearance)
        {
            Console.WriteLine($"NATIVEREF:INFO {kv.Key} backdrop {kv.Value}");
        }
    }

    /// Records whether a state tile is byte-identical to its own normal tile.
    private void NoteIfIdenticalToNormal(string name, string path)
    {
        var parts = name.Split('_');
        if (parts.Length != 3)
        {
            return;
        }
        string id = parts[0], state = parts[1], appearance = parts[2];
        string key = $"{id}_{appearance}";
        string hash;
        using (var md5 = System.Security.Cryptography.MD5.Create())
        using (var fs = File.OpenRead(path))
        {
            hash = Convert.ToHexString(md5.ComputeHash(fs));
        }
        if (state == "normal")
        {
            _normalHashes[key] = hash;
            return;
        }
        if (_normalHashes.TryGetValue(key, out var normalHash) && normalHash == hash)
        {
            _identicalToNormal.Add(name);
            Console.WriteLine($"NATIVEREF:INFO {name} is byte-identical to its normal tile; "
                + "WinUI does not restyle this control for this state");
        }
    }

    /// Waits for n genuinely composed frames. Not Task.Delay: on a loaded runner a fixed
    /// sleep is either wasteful or too short, and too short here means capturing the
    /// previous tile.
    ///
    /// The subscription itself is made on the UI thread -- CompositionTarget.Rendering is
    /// XAML and has the same affinity as everything else here.
    private async Task WaitForFramesAsync(int n)
    {
        var drawn = new TaskCompletionSource<bool>();
        int frames = 0;
        EventHandler<object> onFrame = null;
        onFrame = (_, _) =>
        {
            if (++frames >= n)
            {
                CompositionTarget.Rendering -= onFrame;
                drawn.TrySetResult(true);
            }
        };
        await OnUiAsync(() => { CompositionTarget.Rendering += onFrame; return true; });
        await Task.WhenAny(drawn.Task, Task.Delay(3000));
        await OnUiAsync(() => { CompositionTarget.Rendering -= onFrame; return true; });
    }

    /// Captures a tile only once two consecutive captures agree, so the frame written is
    /// provably settled rather than assumed to be.
    ///
    /// Turning system animations off was not enough, and the measurement is why this exists
    /// rather than a longer sleep. SPI_SETCLIENTAREAANIMATION governs theme TRANSITIONS;
    /// the check-box check, the switch knob, the slider thumb and the progress bar animate
    /// through storyboards in their own control templates, which it does not reach. Two runs
    /// of the same commit still differed on exactly those eight tiles -- by 1 to 58 pixels
    /// out of 13440, the moving edge of each one.
    ///
    /// A fixed delay would be a guess at how long each storyboard takes, wrong on a loaded
    /// runner, and silently wrong in the direction that looks fine. Comparing consecutive
    /// captures asks the question directly and answers it per tile, and a tile that never
    /// settles is a blocker rather than a coin flip written into a golden set.
    private async Task CaptureSettledTileAsync(string name)
    {
        const int MaxAttempts = 8;
        // Before the FIRST grab, not only between grabs. Splitting the old capture into
        // grab-and-compare dropped this delay, and two fast grabs then both landed before
        // the new tile had been composited -- which the loop accepted, because an unpainted
        // window is trivially stable. It wrote #E0E0E0 for both appearances and the
        // backdrop assertion caught it, which is the whole reason that assertion exists.
        await Task.Delay(120);
        byte[] previous = null;
        for (int attempt = 0; attempt < MaxAttempts; attempt++)
        {
            byte[] current = GrabClientArea(name);
            if (current is null)
            {
                return;
            }
            if (previous != null && previous.AsSpan().SequenceEqual(current))
            {
                if (_lastWrittenTile != null && _lastWrittenTile.AsSpan().SequenceEqual(current))
                {
                    // Stable AND identical to the tile before it: the window is showing the
                    // previous tile, not this one. Two different widgets cannot render the
                    // same bytes, so this is a swap that has not landed rather than a
                    // coincidence, and waiting is the right response.
                    previous = null;
                    await Task.Delay(200);
                    continue;
                }
                WriteTile(name, current);
                _lastWrittenTile = current;
                return;
            }
            previous = current;
            await Task.Delay(120);
        }
        _blockers.Add($"{name}: never produced two identical consecutive captures in "
            + $"{MaxAttempts} attempts, so whatever is still moving would be frozen at a "
            + "random point in a golden set");
    }

    /// BitBlts the client area, which is held at exactly one tile, and writes it.
    /// One BitBlt of the client area, returned as raw pixels. No file is written: the
    /// caller compares consecutive grabs and only writes when they agree.
    private byte[] GrabClientArea(string name)
    {
        DwmFlush();
        GetClientRect(_hwnd, out RECT clientRect);
        int w = clientRect.Right - clientRect.Left;
        int h = clientRect.Bottom - clientRect.Top;
        if (w <= 0 || h <= 0)
        {
            _blockers.Add($"{name}: the client area has no size ({w}x{h})");
            return null;
        }
        // PrintWindow, NOT a screen BitBlt, and this is the difference between a
        // reproducible set and a flaky one.
        //
        // A screen grab reads whatever is in front of those coordinates. The probe already
        // reports that this window cannot take the foreground on a hosted runner -- the
        // shell's own Search window holds it -- so the grab depends on nothing wandering
        // over the region in the moment it runs. Two runs of the same commit differed on
        // every tile, and one whole run came back #E0E0E0 in both appearances: not the
        // window at all.
        //
        // PrintWindow renders the window's OWN content and does not care what is on top or
        // whether it is foreground. It cannot see the Mica backdrop, which is drawn behind
        // the window by the compositor -- but the tile paints an opaque
        // SolidBackgroundFillColorBase over that region anyway, so the matrix never needed
        // it. The probe capture still uses the screen BitBlt, because Mica is precisely
        // what it is there to answer.
        // PrintWindow renders the WHOLE window, title bar included, from the window's own
        // origin -- so the bitmap has to be window sized and the client area cropped out of
        // it afterwards. Rendering into a client-sized bitmap would have captured the title
        // bar and called it a widget.
        if (!GetWindowRect(_hwnd, out RECT wr))
        {
            _blockers.Add($"{name}: GetWindowRect failed");
            return null;
        }
        int ww = wr.Right - wr.Left, wh = wr.Bottom - wr.Top;
        var clientOrigin = new POINT { X = 0, Y = 0 };
        ClientToScreen(_hwnd, ref clientOrigin);
        int offX = clientOrigin.X - wr.Left, offY = clientOrigin.Y - wr.Top;

        IntPtr screen = GetDC(IntPtr.Zero);
        IntPtr mem = CreateCompatibleDC(screen);
        IntPtr bmp = CreateCompatibleBitmap(screen, ww, wh);
        IntPtr old = SelectObject(mem, bmp);
        bool ok = PrintWindow(_hwnd, mem, PW_RENDERFULLCONTENT);
        SelectObject(mem, old);
        try
        {
            if (!ok)
            {
                _blockers.Add($"{name}: PrintWindow of the client area failed");
                return null;
            }
            using var whole = System.Drawing.Image.FromHbitmap(bmp);
            if (offX < 0 || offY < 0 || offX + w > whole.Width || offY + h > whole.Height)
            {
                _blockers.Add($"{name}: the client area ({offX},{offY} {w}x{h}) does not lie "
                    + $"inside the window bitmap ({whole.Width}x{whole.Height})");
                return null;
            }
            using var image = whole.Clone(new System.Drawing.Rectangle(offX, offY, w, h), whole.PixelFormat);
            if (IsUniform(image, 0, 0, image.Width, image.Height))
            {
                _blockers.Add($"{name}: captured a uniform image, so nothing was composited");
                return null;
            }
            using var ms = new MemoryStream();
            image.Save(ms, System.Drawing.Imaging.ImageFormat.Png);
            return ms.ToArray();
        }
        finally
        {
            DeleteObject(bmp);
            DeleteDC(mem);
            ReleaseDC(IntPtr.Zero, screen);
        }
    }

    /// Writes a settled grab and records the two things the manifest reports about it.
    private void WriteTile(string name, byte[] png)
    {
        var path = Path.Combine(_outDir, name + ".png");
        File.WriteAllBytes(path, png);
        _tilesWritten++;
        using var ms = new MemoryStream(png);
        using var image = new System.Drawing.Bitmap(ms);
        Console.WriteLine($"NATIVEREF:wrote {name} {image.Width}x{image.Height}");
        NoteIfIdenticalToNormal(name, path);
        // Bottom-right corner: every widget in the matrix anchors top-left and none is
        // as tall as the tile, so this pixel is always backdrop.
        var corner = image.GetPixel(image.Width - 1, image.Height - 1);
        var appearanceKey = name.Substring(name.LastIndexOf('_') + 1);
        _backdropByAppearance[appearanceKey] = $"#{corner.R:X2}{corner.G:X2}{corner.B:X2}";
    }

    /// Grabs what DWM actually put on the screen, which is the only way the Mica backdrop
    /// appears at all: it is drawn behind the window by the compositor and is not part of
    /// the XAML visual tree, so RenderTargetBitmap would silently return the widget without
    /// its material. This is the direct analogue of the iOS reference capturing a real
    /// UIWindow rather than re-rendering a layer off-screen.
    private async Task CaptureWindowAsync()
    {
        try
        {
            _stage = "hwnd";
            var hwnd = _hwnd;

            // Reading the SCREEN means reading whatever is on top of it. The first run that
            // got this far captured the Windows out-of-box privacy dialog sitting over this
            // app, reported zero blockers, and looked entirely plausible -- a 768x519 image
            // full of real controls. So the window is forced to the front and the result is
            // verified, rather than assumed from the fact that we asked for it.
            _stage = "show";
            ShowWindow(hwnd, SW_RESTORE);
            ShowWindow(hwnd, SW_SHOW);

            _stage = "window-rect";
            if (!GetWindowRect(hwnd, out RECT r))
            {
                _blockers.Add("GetWindowRect failed; the window region is unknown");
                return;
            }
            int w = r.Right - r.Left, h = r.Bottom - r.Top;
            if (w <= 0 || h <= 0)
            {
                _blockers.Add($"the window has no size ({w}x{h}); nothing was composited");
                return;
            }

            // Everything from here down is Win32, which is thread agnostic. The XAML part --
            // waiting for presented frames -- deliberately happens before this method is
            // called, while we are still provably on the UI thread.
            DwmFlush();
            await Task.Delay(400);

            _stage = "minimise-occluders";
            var centre = new POINT { X = r.Left + w / 2, Y = r.Top + h / 2 };
            IntPtr atCentre = GetAncestor(WindowFromPoint(centre), GA_ROOT);

            // The runner desktop ships with a "Microsoft account" out-of-box window that
            // sits above even an always-on-top presenter, so asking politely for the
            // foreground is not enough. Minimise whatever is covering us, and keep going in
            // case several are stacked. This is a throwaway CI desktop whose only purpose is
            // to photograph this window; there is nothing here to be polite to.
            for (int i = 0; i < 5 && atCentre != hwnd && atCentre != IntPtr.Zero; i++)
            {
                // Closed, not just minimised. Minimising moved it out of the frame but it
                // KEPT the foreground -- measured: "the window is unoccluded but never became
                // active (foreground is 0x1020A \"Microsoft account\")" with that same
                // window already minimised. A window that is gone cannot hold the foreground.
                // Minimise remains the fallback for anything that refuses to close.
                Console.WriteLine($"NATIVEREF:INFO clearing 0x{atCentre.ToInt64():X} "
                    + $"({DescribeWindow(atCentre)}) which is covering the capture area");
                PostMessage(atCentre, WM_CLOSE, IntPtr.Zero, IntPtr.Zero);
                await Task.Delay(400);
                if (IsWindow(atCentre))
                {
                    Console.WriteLine($"NATIVEREF:INFO 0x{atCentre.ToInt64():X} would not close; minimising");
                    ShowWindow(atCentre, SW_MINIMIZE);
                }
                await Task.Delay(250);
                BringWindowToTop(hwnd);
                atCentre = GetAncestor(WindowFromPoint(centre), GA_ROOT);
            }

            _occluded = atCentre != hwnd;
            if (_occluded)
            {
                _blockers.Add("something is covering this window, so the capture would be of "
                    + $"it and not of us ({DescribeForegroundWindow()}, "
                    + $"at centre 0x{atCentre.ToInt64():X}). Screen capture reads whatever is "
                    + "on top, and a picture of the wrong window still looks like a picture.");
                return;
            }
            // Only NOW is it worth asking for the foreground. The earlier attempt ran before
            // the occluders were minimised and could never have succeeded: Win32 refuses
            // SetForegroundWindow to a process that is not already foreground, and the
            // out-of-box window held it. Nothing retried once the obstacle was gone, so the
            // window ended up unoccluded but inactive -- visible, and rendered in the
            // inactive style.
            //
            // Activation is not cosmetic for a reference set. An inactive window renders its
            // title bar differently and does not get the Mica backdrop, so capturing one
            // would encode a look no user sees, which is the same class of error as
            // capturing with Reduce Transparency on under macOS.
            _stage = "activate";
            bool isForeground = await TryTakeForegroundAsync(hwnd);
            if (!isForeground)
            {
                // Recorded, not fatal -- and that distinction is the point of the measurement
                // that prompted it. Sampling the earlier capture's title bar found its
                // darkest glyph at (145,145,145); an active Windows 11 title bar draws that
                // text near black, so the window really is rendering inactive.
                //
                // What that costs depends on what is being photographed. The title bar and
                // the Mica backdrop are drawn differently for an inactive window. WinUI
                // CONTROLS are not: unlike AppKit, which greys every control in an inactive
                // window and is exactly why the macOS probe treats activation as fatal.
                //
                // So a widget tile is unaffected and a window-chrome tile would not be, and
                // the tiles this reference set needs first are widget tiles. Blocking on it
                // would refuse usable captures for a property they do not depend on, which is
                // the same mistake the occlusion check had to be rewritten to stop making.
                //
                // TO VERIFY before any window-chrome or Mica tile is captured: the claim that
                // WinUI controls render identically inactive is reasoned from the platform's
                // behaviour, not yet measured here. Capture the same control active and
                // inactive and diff them.
                Console.WriteLine("NATIVEREF:WARN could not take the foreground "
                    + $"({DescribeForegroundWindow()}); widget tiles are unaffected, window "
                    + "chrome and Mica would be. Recorded in the manifest.");
            }
            // Activating can re-order windows, so confirm nothing slid back over us.
            atCentre = GetAncestor(WindowFromPoint(centre), GA_ROOT);
            if (atCentre != hwnd)
            {
                // Clear it and re-check rather than giving up. Start and Search both close on
                // Esc, and the previous run failed here for a self-inflicted reason -- the
                // activation click had landed on the taskbar and opened Start, which then
                // covered the window.
                Console.WriteLine($"NATIVEREF:INFO 0x{atCentre.ToInt64():X} "
                    + $"({DescribeWindow(atCentre)}) came over the window after activating; "
                    + "dismissing");
                keybd_event(VK_ESCAPE, 0, 0, UIntPtr.Zero);
                keybd_event(VK_ESCAPE, 0, KEYEVENTF_KEYUP, UIntPtr.Zero);
                await Task.Delay(500);
                BringWindowToTop(hwnd);
                await Task.Delay(300);
                atCentre = GetAncestor(WindowFromPoint(centre), GA_ROOT);
            }
            if (atCentre != hwnd)
            {
                _occluded = true;
                _blockers.Add($"activating let 0x{atCentre.ToInt64():X} "
                    + $"({DescribeWindow(atCentre)}) back over the window, and it would not "
                    + "dismiss");
                return;
            }
            await Task.Delay(300);
            Console.WriteLine("NATIVEREF:INFO active and unoccluded");

            // The tile is the CLIENT area, not the whole window. A widget reference wants the
            // widgets; the title bar is chrome we cannot reliably activate on this image, and
            // including it would bake an inactive title bar into every tile. A window-chrome
            // tile, when there is one, is a separate capture with its own requirements.
            GetClientRect(hwnd, out RECT clientRect);
            var clientOrigin = new POINT { X = 0, Y = 0 };
            ClientToScreen(hwnd, ref clientOrigin);
            int clientW = clientRect.Right - clientRect.Left;
            int clientH = clientRect.Bottom - clientRect.Top;
            if (clientW <= 0 || clientH <= 0)
            {
                _blockers.Add($"the client area has no size ({clientW}x{clientH})");
                return;
            }
            w = clientW;
            h = clientH;
            var pos = new { X = clientOrigin.X, Y = clientOrigin.Y };
            _stage = "bitblt";
            IntPtr screen = GetDC(IntPtr.Zero);
            IntPtr mem = CreateCompatibleDC(screen);
            IntPtr bmp = CreateCompatibleBitmap(screen, w, h);
            IntPtr old = SelectObject(mem, bmp);
            bool ok = BitBlt(mem, 0, 0, w, h, screen, pos.X, pos.Y, SRCCOPY | CAPTUREBLT);
            SelectObject(mem, old);

            if (!ok)
            {
                _blockers.Add("BitBlt of the window region failed");
            }
            else
            {
                using var image = System.Drawing.Image.FromHbitmap(bmp);
                var name = _isProbe ? "probe_Button_normal_light" : "Button_normal_light";
                var path = Path.Combine(_outDir, name + ".png");
                image.Save(path, System.Drawing.Imaging.ImageFormat.Png);
                Console.WriteLine($"NATIVEREF:wrote {name} {w}x{h}");
                // Deliberately the CLIENT area, not the whole window. The title bar is drawn
                // by DWM whatever the app does, so a whole-window uniformity test passes an
                // utterly empty window -- which is exactly what it did: a captured
                // cn1-native-ref frame with a correct Windows 11 title bar and nothing at
                // all beneath it.
                // The captured tile is now exactly the client area, so these offsets are
                // simply the whole image.
                // Sample the background well away from the button and record it.
                // Whether the Mica backdrop is actually reaching the window is otherwise an
                // eyeball judgement on a PNG: a flat theme fill and a Mica surface over a
                // pale desktop look similar at a glance, and "mica_supported: true" only
                // says the OS could draw it, not that this window got it.
                int cx = 0, cy = 0, cw = w, ch = h;
                if (cw > 0 && ch > 0 && cx >= 0 && cy >= 0 && cx + cw <= w && cy + ch <= h)
                {
                    var bg = image.GetPixel(cx + cw * 3 / 4, cy + ch * 3 / 4);
                    _clientBackground = $"#{bg.R:X2}{bg.G:X2}{bg.B:X2}";
                    Console.WriteLine($"NATIVEREF:INFO client background sample {_clientBackground}");
                }
                // The definitive styling check: a Fluent Button has rounded corners, so its
                // corner pixel is the page behind it and its centre pixel is the fill. When
                // those are equal the control is a plain rectangle, which is what an unstyled
                // fallback looks like. This measures the control itself, rather than
                // inferring from whether some build artefact was produced -- which is what
                // sent the previous three attempts after the wrong cause.
                if (_probeBounds.Width > 4 && _probeBounds.Height > 4)
                {
                    int bx = (int)_probeBounds.X, by = (int)_probeBounds.Y;
                    int bw = (int)_probeBounds.Width, bh = (int)_probeBounds.Height;
                    if (bx >= 0 && by >= 0 && bx + bw <= w && by + bh <= h)
                    {
                        // One pixel in from the corner: exactly on it can land on the
                        // antialiased edge, which is neither the page nor the fill.
                        var corner = image.GetPixel(bx + 1, by + 1);
                        var mid = image.GetPixel(bx + bw / 2, by + bh / 2);
                        Console.WriteLine($"NATIVEREF:INFO probe control {bw}x{bh} at {bx},{by} "
                            + $"corner=#{corner.R:X2}{corner.G:X2}{corner.B:X2} "
                            + $"centre=#{mid.R:X2}{mid.G:X2}{mid.B:X2}");
                        if (corner == mid)
                        {
                            _blockers.Add($"the control is a plain rectangle (corner and centre "
                                + $"both #{mid.R:X2}{mid.G:X2}{mid.B:X2}); a Fluent Button has "
                                + "rounded corners, so its styles did not load");
                        }
                    }
                }

                if (cw > 0 && ch > 0 && cx >= 0 && cy >= 0 && cx + cw <= w && cy + ch <= h
                    && IsUniform(image, cx, cy, cw, ch))
                {
                    _blockers.Add($"{name} has an empty client area ({cw}x{ch} of one flat "
                        + "colour): the window chrome drew but the content did not");
                }
            }

            DeleteObject(bmp);
            DeleteDC(mem);
            ReleaseDC(IntPtr.Zero, screen);
        }
        catch (Exception e)
        {
            // COMException's Message is routinely empty, and "capture threw COMException:"
            // is worth nothing to whoever reads it next. Record where it got to, the
            // HRESULT, and the frame it came from -- a probe whose failures are not
            // self-describing just converts one unknown into another.
            _blockers.Add($"capture threw {e.GetType().Name} (0x{e.HResult:X8}) at stage "
                + $"'{_stage}': {(string.IsNullOrWhiteSpace(e.Message) ? "(no message)" : e.Message)}");
            Console.Error.WriteLine($"NATIVEREF:EXCEPTION stage={_stage} {e}");
        }
    }


    /// Takes the foreground, working around the Win32 rule that a process which is not
    /// already foreground may not call SetForegroundWindow. Attaching to the current
    /// foreground window's input thread lifts that restriction for the duration.
    private static async Task<bool> TryTakeForegroundAsync(IntPtr hwnd)
    {
        // Windows enforces a foreground LOCK TIMEOUT: after another process has been
        // activated, SetForegroundWindow is refused for a period regardless of the
        // input-thread attach. Setting it to zero is the documented way to opt out, and on a
        // CI desktop whose only job is to photograph one window there is nothing to protect
        // the user from.
        SystemParametersInfo(SPI_SETFOREGROUNDLOCKTIMEOUT, 0, IntPtr.Zero, SPIF_SENDCHANGE);

        for (int i = 0; i < 20 && GetForegroundWindow() != hwnd; i++)
        {
            IntPtr fg = GetForegroundWindow();
            uint fgThread = GetWindowThreadProcessId(fg, IntPtr.Zero);
            uint thisThread = GetCurrentThreadId();
            bool attached = fgThread != 0 && fgThread != thisThread
                            && AttachThreadInput(fgThread, thisThread, true);
            try
            {
                BringWindowToTop(hwnd);
                SetForegroundWindow(hwnd);
            }
            finally
            {
                if (attached) AttachThreadInput(fgThread, thisThread, false);
            }
            await Task.Delay(150);
        }
        if (GetForegroundWindow() == hwnd)
        {
            return true;
        }

        // Last resort: activate the way a person would, by clicking on it. The shell's
        // "Search" window holds the foreground on this image and does not respond to
        // WM_CLOSE the way an ordinary app window does, and SetForegroundWindow keeps losing
        // to it. A real click travels the normal input path, which Windows always honours --
        // there is no policy that refuses to activate the window the user just clicked.
        //
        // The point is inside our client area but far from the widgets, so the click lands
        // on empty background: activating the window must not also press the control being
        // photographed.
        // The click point comes from the CLIENT rect, not the window rect, and is verified to
        // belong to us before any button goes down. Computed from the window rect at four
        // fifths down, the previous attempt landed on the TASKBAR and opened the Start menu --
        // which then covered the window. Activating by clicking is only safe if you know what
        // you are clicking on.
        GetClientRect(hwnd, out RECT clientR);
        var clientTopLeft = new POINT { X = 0, Y = 0 };
        ClientToScreen(hwnd, ref clientTopLeft);
        if (clientR.Right > clientR.Left && clientR.Bottom > clientR.Top)
        {
            int cx = clientTopLeft.X + (clientR.Right - clientR.Left) * 3 / 4;
            int cy = clientTopLeft.Y + (clientR.Bottom - clientR.Top) / 2;
            IntPtr atClick = GetAncestor(WindowFromPoint(new POINT { X = cx, Y = cy }), GA_ROOT);
            if (atClick != hwnd)
            {
                Console.WriteLine($"NATIVEREF:WARN not clicking: {cx},{cy} belongs to "
                    + $"0x{atClick.ToInt64():X} ({DescribeWindow(atClick)}), not to us");
                return GetForegroundWindow() == hwnd;
            }
            Console.WriteLine($"NATIVEREF:INFO clicking our own client area at {cx},{cy} to activate "
                + $"(foreground was {DescribeForegroundWindow()})");
            SetCursorPos(cx, cy);
            await Task.Delay(120);
            mouse_event(MOUSEEVENTF_LEFTDOWN, 0, 0, 0, UIntPtr.Zero);
            await Task.Delay(60);
            mouse_event(MOUSEEVENTF_LEFTUP, 0, 0, 0, UIntPtr.Zero);
            await Task.Delay(500);

            // Escape first, in case the desktop popped Start or Search. Those are the two
            // surfaces that keep taking the foreground on this image, and both close on Esc.
            keybd_event(VK_ESCAPE, 0, 0, UIntPtr.Zero);
            keybd_event(VK_ESCAPE, 0, KEYEVENTF_KEYUP, UIntPtr.Zero);
            await Task.Delay(300);

            // Retry the API after the click. SetForegroundWindow is allowed when the calling
            // process received the last input event, which a real click is -- so the attempt
            // that was refused a moment ago may now be granted.
            for (int i = 0; i < 10 && GetForegroundWindow() != hwnd; i++)
            {
                SetForegroundWindow(hwnd);
                await Task.Delay(150);
            }
        }
        return GetForegroundWindow() == hwnd;
    }

    /// Names whatever is actually in front, so a capture failure says which window stole
    /// the screen instead of leaving someone to guess from the picture.
    private static string DescribeForegroundWindow()
    {
        var fg = GetForegroundWindow();
        return $"foreground is 0x{fg.ToInt64():X} \"{DescribeWindow(fg)}\"";
    }

    private static string DescribeWindow(IntPtr h)
    {
        try
        {
            var buf = new StringBuilder(256);
            int n = GetWindowTextW(h, buf, buf.Capacity);
            return n > 0 ? buf.ToString() : "(untitled)";
        }
        catch
        {
            return "(unidentifiable)";
        }
    }

    /// A uniformly coloured tile is the classic captured-before-present result, and it
    /// scores as a perfect match against another blank tile rather than as a failure.
    private static bool IsUniform(System.Drawing.Bitmap image, int x0, int y0, int w, int h)
    {
        var first = image.GetPixel(x0, y0);
        int stepX = Math.Max(1, w / 32), stepY = Math.Max(1, h / 32);
        for (int y = y0; y < y0 + h; y += stepY)
            for (int x = x0; x < x0 + w; x += stepX)
                if (image.GetPixel(x, y) != first) return false;
        return true;
    }

    private static bool FontIsInstalled(string family)
    {
        try
        {
            var dir = Environment.GetFolderPath(Environment.SpecialFolder.Windows);
            var fonts = Path.Combine(dir, "Fonts");
            // Segoe UI Variable ships as SegUIVar*.ttf; matching the file rather than
            // asking a font-fallback API is deliberate, because font fallback answers
            // "something will render" for every family name ever passed to it.
            return Directory.Exists(fonts)
                   && Directory.GetFiles(fonts, "SegUIVar*.ttf").Length > 0;
        }
        catch
        {
            return false;
        }
    }

    private void WriteManifest(bool micaSupported, bool transparency, bool animations,
        Windows.UI.Color accent, double rasterScale, string fontFamily, bool segoeVariable)
    {
        var sb = new StringBuilder();
        sb.AppendLine("{");
        sb.AppendLine("  \"schema\": 1,");
        sb.AppendLine("  \"platform\": \"windows\",");
        sb.AppendLine($"  \"golden_set\": \"{Env("CN1SS_FIDELITY_GOLDEN_SET", "windows-11-fluent")}\",");
        sb.AppendLine($"  \"mode\": \"{(_isProbe ? "probe" : "capture")}\",");
        sb.AppendLine("  \"os\": {");
        sb.AppendLine($"    \"version\": \"{Environment.OSVersion.Version}\",");
        sb.AppendLine($"    \"description\": \"{Escape(RuntimeInformation.OSDescription)}\",");
        sb.AppendLine($"    \"architecture\": \"{RuntimeInformation.OSArchitecture}\",");
        sb.AppendLine($"    \"product\": \"{Escape(ReadRegistry("ProductName"))}\",");
        sb.AppendLine($"    \"display_version\": \"{Escape(ReadRegistry("DisplayVersion"))}\",");
        sb.AppendLine($"    \"build\": \"{Escape(ReadRegistry("CurrentBuildNumber"))}\",");
        sb.AppendLine($"    \"installation_type\": \"{Escape(ReadRegistry("InstallationType"))}\"");
        sb.AppendLine("  },");
        sb.AppendLine("  \"toolkit\": {");
        sb.AppendLine("    \"name\": \"WinUI3\",");
        sb.AppendLine("    \"deployment\": \"unpackaged-self-contained\",");
        sb.AppendLine($"    \"dotnet\": \"{Escape(RuntimeInformation.FrameworkDescription)}\"");
        sb.AppendLine("  },");
        sb.AppendLine("  \"display\": {");
        sb.AppendLine($"    \"rasterization_scale\": {rasterScale.ToString(System.Globalization.CultureInfo.InvariantCulture)}");
        sb.AppendLine("  },");
        sb.AppendLine("  \"appearance\": {");
        sb.AppendLine($"    \"mica_supported\": {Json(micaSupported)},");
        sb.AppendLine($"    \"transparency_effects\": {Json(transparency)},");
        sb.AppendLine($"    \"animations_enabled\": {Json(animations)},");
        sb.AppendLine($"    \"accent_color\": \"#{accent.R:X2}{accent.G:X2}{accent.B:X2}\"");
        sb.AppendLine("  },");
        sb.AppendLine("  \"capture\": {");
        sb.AppendLine($"    \"occluded\": {Json(_occluded)},");
        sb.AppendLine($"    \"was_foreground\": {Json(GetForegroundWindow() == _hwnd)},");
        sb.AppendLine($"    \"client_background\": \"{Escape(_clientBackground)}\"");
        sb.AppendLine("  },");
        sb.AppendLine("  \"fonts\": {");
        sb.AppendLine($"    \"control_family\": \"{Escape(fontFamily)}\",");
        sb.AppendLine($"    \"segoe_ui_variable_installed\": {Json(segoeVariable)}");
        sb.AppendLine("  },");
        sb.AppendLine($"  \"tiles_written\": {_tilesWritten},");
        sb.AppendLine($"  \"animations_disabled_by_app\": {(_animationsDisabled ? "true" : "false")},");
        sb.Append("  \"backdrop_by_appearance\": {");
        sb.Append(string.Join(", ", _backdropByAppearance.Select(kv => $"\"{Escape(kv.Key)}\": \"{Escape(kv.Value)}\"")));
        sb.AppendLine("},");
        sb.Append("  \"states_identical_to_normal\": [");
        sb.Append(string.Join(", ", _identicalToNormal.Select(n => $"\"{Escape(n)}\"")));
        sb.AppendLine("],");
        sb.Append("  \"blockers\": [");
        sb.Append(string.Join(", ", _blockers.Select(b => $"\"{Escape(b)}\"")));
        sb.AppendLine("]");
        sb.AppendLine("}");

        var path = Path.Combine(_outDir, "capture-manifest.json");
        File.WriteAllText(path, sb.ToString());
        Console.WriteLine($"NATIVEREF:INFO wrote {path}");
    }

    private static string Env(string name, string fallback)
        => Environment.GetEnvironmentVariable(name) is { Length: > 0 } v ? v : fallback;

    private static string Json(bool b) => b ? "true" : "false";

    /// Escapes for JSON, control characters included. A window title arrived carrying
    /// embedded NULs and wrote them raw into the manifest, which made it unparseable -- and
    /// a raw control byte in a text file is precisely what scripts/check-control-characters.py
    /// exists to prevent, because it turns the file binary to every tool that reads it.
    private static string Escape(string s)
    {
        if (string.IsNullOrEmpty(s)) return string.Empty;
        var sb = new StringBuilder(s.Length);
        foreach (var c in s)
        {
            switch (c)
            {
                case '\\': sb.Append("\\\\"); break;
                case '"': sb.Append("\\\""); break;
                case '\n': sb.Append("\\n"); break;
                case '\r': sb.Append("\\r"); break;
                case '\t': sb.Append("\\t"); break;
                default:
                    if (char.IsControl(c)) sb.Append($"\\u{(int)c:X4}");
                    else sb.Append(c);
                    break;
            }
        }
        return sb.ToString();
    }

    private static string ReadRegistry(string name)
    {
        try
        {
            using var key = Microsoft.Win32.Registry.LocalMachine.OpenSubKey(
                @"SOFTWARE\Microsoft\Windows NT\CurrentVersion");
            return key?.GetValue(name)?.ToString() ?? "";
        }
        catch
        {
            return "";
        }
    }
}
