// Generated from docs/developer-guide source blocks. Edit the guide snippets here, not inline.

// tag::desktop-integration-java-001[]
Display.getInstance().setProperty("desktop.titleBar", "native");
Display.getInstance().setProperty("desktop.interactiveScrollbars", "true");
// end::desktop-integration-java-001[]

// tag::desktop-integration-java-002[]
Form hi = new Form("My App", BoxLayout.y());

Command about = new Command("About My App");
about.setDesktopMenu(Command.DESKTOP_MENU_ABOUT);   // application menu

Command open = new Command("Open File...");
open.setDesktopMenu(Command.DESKTOP_MENU_FILE);     // File menu

Command quit = new Command("Quit");
quit.setDesktopMenu(Command.DESKTOP_MENU_QUIT);     // application menu

hi.addCommand(about);
hi.addCommand(open);
hi.addCommand(quit);
hi.show();
// end::desktop-integration-java-002[]

// tag::desktop-integration-java-003[]
Command save = new Command("Save");
save.setDesktopMenu(Command.DESKTOP_MENU_FILE);
save.setDesktopShortcut('S');   // Cmd+S on macOS, Ctrl+S on Windows/Linux

Command saveAs = new Command("Save As...");
saveAs.setDesktopMenu(Command.DESKTOP_MENU_FILE);
saveAs.setDesktopShortcut('S',
        Command.DESKTOP_SHORTCUT_MODIFIER_PRIMARY | Command.DESKTOP_SHORTCUT_MODIFIER_SHIFT);
// end::desktop-integration-java-003[]

// tag::desktop-integration-java-004[]
LocalNotification n = new LocalNotification();
n.setId("reminder-1");
n.setAlertTitle("Reminder");
n.setAlertBody("Your export has finished.");

// fire in 5 seconds, no repeat
Display.getInstance().scheduleLocalNotification(
        n, System.currentTimeMillis() + 5000, LocalNotification.REPEAT_NONE);
// end::desktop-integration-java-004[]
