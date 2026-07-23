# Evidence map

Source: `docs/website/content/blog/widgets-live-activities-dynamic-island.md`
Canonical: https://www.codenameone.com/blog/widgets-live-activities-dynamic-island/

## Thesis

How one timeline model feeds widgets, Live Activities, Dynamic Island, Android notifications, and desktop widgets

## Supported beats

- **The dead-process rule:** An external surface is a piece of application state that the operating system can render outside the app. The app publishes a serializable layout and a timeline of state maps. The platform persists that data, then renders it with its own surface technology.
- **Widget kinds exist at build time:** iOS and Android compile widget galleries into the native application. The kinds must therefore be known during the build. Add a surfaces.json resource.
- **A timeline carries future state:** A widget publishes one layout and dated entries. Placeholders such as ${status} resolve from each entry's state map. The operating system advances to the next entry without waking the app.
- **Dynamic Island is another layout region:** A Live Activity uses the same nodes and state maps. Its descriptor adds the regions ActivityKit needs: compact leading and trailing content, minimal content, and the expanded leading, trailing, center, and bottom areas.
- **Widgets on Linux are not a typo:** The same publish call reaches desktop targets. A JavaSE or native desktop build can expose kinds from a tray menu and pin them as frameless, always-on-top windows. Windows can also generate a Windows 11 Widgets Board provider when windows.msix=true.
- **The common model has a floor:** The surface node catalog is deliberately smaller than the Codename One component set. Android RemoteViews is the constrained renderer, so it defines several compromises.

## Referenced evidence

- https://github.com/codenameone/CodenameOne/pull/5365

## Independent problem evidence

- Apple WidgetKit timeline documentation: https://developer.apple.com/documentation/widgetkit/keeping-a-widget-up-to-date
- Apple ActivityKit documentation: https://developer.apple.com/documentation/activitykit
- Android app widgets overview: https://developer.android.com/develop/ui/views/appwidgets/overview
- Android RemoteViews documentation: https://developer.android.com/reference/android/widget/RemoteViews

## Product proof

- `samples/samples/SurfacesSample/SurfacesSample.java`
- `samples/samples/SurfacesSample/surfaces.json`
- `CodenameOne/src/com/codename1/surfaces/`
- `ports/JavaSE/src/com/codename1/impl/javase/JavaSEWidgetBridge.java`
