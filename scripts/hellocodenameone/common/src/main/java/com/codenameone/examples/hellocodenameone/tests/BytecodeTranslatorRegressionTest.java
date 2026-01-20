package com.codenameone.examples.hellocodenameone.tests;

import com.codename1.ui.CN;
import com.codename1.ui.Component;
import com.codename1.ui.Font;
import com.codename1.ui.Form;
import com.codename1.ui.Graphics;
import com.codename1.ui.geom.Dimension;
import com.codename1.ui.geom.Rectangle;
import java.util.ArrayList;

public class BytecodeTranslatorRegressionTest extends BaseTest {
    private interface Sketchable {
    }

    private interface SlotProvider {
        int lastSlot(boolean empty);
    }

    private interface TokenMenuHost {
        default void drawToken(Graphics gc, SlotProvider source, SlotProvider dest, int cellSize, int x, int y,
                Font font, Hue color, String label) {
            GameViewer.log("draw-token:" + label + "@" + x);
        }

        default void drawToken(Graphics gc, SlotProvider source, SlotProvider dest, int cellSize, int x, int y,
                Font font, Hue color, Sketchable label) {
            GameViewer.log("draw-token:" + label + "@" + y);
        }

        default void drawArrow(Graphics gc, SlotProvider src, SlotProvider dest, int x1, int y1, int x2, int y2,
                Hue color, double arrowOpacity, int tickSize, double lineWidth) {
            GameViewer.log("draw-arrow:" + x1 + ":" + tickSize + ":" + lineWidth);
        }

        default int lastSlot() {
            throw new Error("lastSlot must be overridden");
        }
    }

    private static final class Hue {
        private final int r;
        private final int g;
        private final int b;

        private Hue(int r, int g, int b) {
            this.r = r;
            this.g = g;
            this.b = b;
        }

        private int asRgb() {
            return (r << 16) | (g << 8) | b;
        }
    }

    private static final class Marker implements Sketchable {
        private final int weight;

        private Marker(int weight) {
            this.weight = weight;
        }

        private int score() {
            return weight * 2;
        }

        @Override
        public String toString() {
            return "Marker:" + weight;
        }
    }

    private static final class TokenMenu extends Rectangle {
        private String render(TokenMenuHost host) {
            String info = "slot:" + host.lastSlot();
            host.drawToken(null, null, null, 1, 2, 3, null, new Hue(12, 34, 56), info);
            return info;
        }
    }

    private abstract static class ShellComponent extends Component {
    }

    private abstract static class ProxyCanvas extends ShellComponent {
    }

    private abstract static class CommonCanvas extends ProxyCanvas implements TokenMenuHost {
    }

    private interface BoardModel {
    }

    private static class Tile<CHIPTYPE> {
    }

    private interface GameConstants {
    }

    private static class GameChip implements GameConstants {
    }

    private static class GameBoard implements BoardModel {
    }

    private abstract static class PanelCanvas<CELLTYPE extends Tile<?>, BOARDTYPE extends BoardModel>
            extends CommonCanvas {
    }

    private static class StackTile<FINALTYPE, CHIPTYPE> extends Tile<CHIPTYPE> {
    }

    private static final class GameCell extends StackTile<GameCell, GameChip> implements GameConstants, SlotProvider {
        @Override
        public int lastSlot(boolean empty) {
            return empty ? -1 : 3;
        }
    }

    private static final class GameViewer extends PanelCanvas<GameCell, GameBoard>
            implements GameConstants, TokenMenuHost, Runnable {
        private final Form form;
        private static final ArrayList<String> messages = new ArrayList<>();

        private GameViewer(Form form) {
            this.form = form;
            setPreferredSize(new Dimension(16, 16));
        }

        private static void log(String message) {
            messages.add(message);
        }

        private String buildStatus(int total) {
            int width = form == null ? 0 : form.getWidth();
            return "status-" + total + "-" + width + "-" + messages.size();
        }

        @Override
        public int lastSlot() {
            return 5;
        }

        @Override
        public void drawToken(Graphics gc, SlotProvider source, SlotProvider dest, int cellSize, int x, int y,
                Font font, Hue color, String label) {
            SlotProvider cell = dest;
            int xx = x - cellSize;
            int yy = y + cellSize;
            int mix = color.asRgb() ^ (xx + yy);
            if (mix % 2 == 0) {
                log("even:" + label + ":" + cell);
            } else {
                super.drawToken(gc, source, dest, cellSize, xx + cellSize, yy - cellSize, font, color, label);
            }
        }

        @Override
        public void run() {
            int loops = 0;
            while (loops < 2) {
                loops++;
                try {
                    Thread.sleep(5);
                } catch (InterruptedException e) {
                    log("interrupted");
                }
            }
        }
    }

    @Override
    public boolean runTest() {
        Thread worker = new Thread(() -> {
            try {
                Form form = new Form();
                GameViewer viewer = new GameViewer(form);
                TokenMenu menu = new TokenMenu();
                String summary = menu.render(viewer);
                GameCell cell = new GameCell();
                Marker marker = new Marker(7);
                viewer.drawToken(null, cell, cell, 2, 5, 7, null, new Hue(10, 20, 30), marker);
                viewer.drawArrow(null, cell, cell, 1, 2, 3, 4, new Hue(4, 5, 6), 0.5, 2, 1.5);
                int total = summary.length() + viewer.lastSlot() + cell.lastSlot(false) + marker.score();
                String status = viewer.buildStatus(total);
                if (!status.startsWith("status-")) {
                    fail("Unexpected status: " + status);
                    return;
                }
                if (GameViewer.messages.isEmpty()) {
                    fail("No messages recorded.");
                    return;
                }
                CN.callSerially(this::done);
            } catch (Throwable t) {
                fail("Regression test failed: " + t);
            }
        }, "cn1-bytecode-regression");
        worker.start();
        return true;
    }

    @Override
    public boolean shouldTakeScreenshot() {
        return false;
    }
}
