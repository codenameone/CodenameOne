// Generated from docs/developer-guide source blocks. Edit the guide snippets here, not inline.

// tag::game-builder-java-001[]
GameLevel level = GameLevel.load(
        Display.getInstance().getResourceAsStream(MyScene.class, "/games/Level1.game"));
GameSceneView view = new GameSceneView(level, AssetCatalog.load(packsJson));
form.add(BorderLayout.CENTER, view);
form.show();
view.start();
// end::game-builder-java-001[]

// tag::game-builder-java-002[]
@Override
protected void onUpdate(double dt) {
    GameInput in = getInput();
    Scene scene = getScene();
    for (int i = 0; i < scene.size(); i++) {
        Sprite s = scene.get(i);
        GameElement el = (GameElement) s.getUserData();
        if (el == null) continue;
        switch (el.getAssetId()) {
            case "player" -> {
                if (in.isGameKeyDown(Display.GAME_RIGHT)) s.setX(s.getX() + 200 * dt);
                // jump height, lives, gravity... all read from el.getInt(...)/getDouble(...)
            }
            case "slime" -> s.setX(s.getX() + el.getDouble("speed", 1.5)); // patrol
            case "coin"  -> { if (s.intersects(player)) score += el.getInt("value", 10); }
        }
    }
}
// end::game-builder-java-002[]

// tag::game-builder-java-003[]
Form game = new Form("Play", new BorderLayout());
Level1 scene = new Level1(StarterPacks.loadCatalog());
game.add(BorderLayout.CENTER, scene);
game.show();
scene.start();
// end::game-builder-java-003[]
