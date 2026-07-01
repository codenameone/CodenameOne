// Generated from docs/developer-guide source blocks. Edit the guide snippets here, not inline.

// tag::in-car-experiences-java-001[]
import com.codename1.car.*;

public class MyApp {
    public void init(Object context) {
        // ... normal app init ...
        Car.setApplication(new MyCarApplication());
    }
}

class MyCarApplication extends CarApplication {
    public CarScreen onCreateRootScreen(CarContext context) {
        return new LibraryScreen();
    }
}

class LibraryScreen extends CarScreen {
    protected CarTemplate onCreateTemplate() {
        return new CarListTemplate().setTitle("Library")
            .addRow(new CarRow("Now Playing").setText("Daft Punk — Discovery")
                .setOnAction(ctx -> play()))
            .addRow(new CarRow("Albums").setBrowsable(true)
                .setOnAction(ctx -> ctx.pushScreen(new AlbumsScreen())))
            .addRow(new CarRow("Playlists").setBrowsable(true)
                .setOnAction(ctx -> ctx.pushScreen(new PlaylistsScreen())));
    }
}
// end::in-car-experiences-java-001[]

// tag::in-car-experiences-java-002[]
int max = context.getListRowLimit(); // 0 when unknown
// end::in-car-experiences-java-002[]

// tag::in-car-experiences-java-003[]
context.pushScreen(new DetailScreen());  // drill in
context.popScreen();                     // back
screen.invalidate();                     // rebuild this screen's template after a model change
context.showToast("Added to queue");
// end::in-car-experiences-java-003[]

// tag::in-car-experiences-java-004[]
Car.addConnectionListener(new CarConnectionListener() {
    public void carConnected(CarContext ctx) { startLocationStream(); }
    public void carDisconnected()           { stopLocationStream(); }
});
// end::in-car-experiences-java-004[]
