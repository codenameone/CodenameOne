package com.codename1.samples;

import com.codename1.gaming.GameView;
import com.codename1.gaming.Sprite;
import com.codename1.io.Log;
import com.codename1.ui.CN;
import com.codename1.ui.Display;
import com.codename1.ui.Form;
import com.codename1.ui.Graphics;
import com.codename1.ui.Image;
import com.codename1.ui.Toolbar;
import com.codename1.ui.layouts.BorderLayout;
import com.codename1.ui.plaf.UIManager;
import com.codename1.ui.util.Resources;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/// A faux-3D board game: checkers (draughts) on an isometric board. The board is plain
/// 2D -- every tile and piece is a `Sprite` -- but laying them out in an isometric
/// projection (a 2:1 diamond grid) and raising the pieces off their tiles gives the
/// scene a convincing 3D look without any GPU 3D, perspective camera or models. That
/// is the trick "faux 3D" names: a 3D feel from flat sprites and clever positioning.
///
/// You play red (bottom) against a small built-in AI (black). Tap one of your pieces to
/// select it, then tap a highlighted square to move or jump. Captures chain, men crown
/// when they reach the far side. All art is generated at runtime, so there are no
/// assets. The interesting bit for a game author is the isometric mapping
/// (`#tileCenterX(int,int)` / `#tileCenterY(int,int)` and its inverse `#pick(int,int)`)
/// which converts between board cells and screen pixels -- the same math underlies any
/// isometric strategy or tycoon game.
public class BoardGameSample {
    private Form current;
    private Resources theme;

    public void init(Object context) {
        theme = UIManager.initFirstTheme("/theme");
        Toolbar.setGlobalToolbar(true);
        Log.bindCrashProtection(true);
    }

    public void start() {
        if (current != null) {
            current.show();
            return;
        }
        Form f = new Form("Checkers", new BorderLayout());
        BoardView game = new BoardView();
        f.add(BorderLayout.CENTER, game);
        f.show();
        game.start();
    }

    public void stop() {
        current = Display.getInstance().getCurrent();
    }

    public void destroy() {
    }

    /// The isometric board surface and game logic.
    static class BoardView extends GameView {
        private static final int N = 8;
        // cell contents
        private static final int EMPTY = 0;
        private static final int RED_MAN = 1;
        private static final int RED_KING = 2;
        private static final int BLACK_MAN = 3;
        private static final int BLACK_KING = 4;
        // owners
        private static final int RED = 1;     // human, starts at the bottom, moves up
        private static final int BLACK = 3;   // AI, starts at the top, moves down

        private final Random rnd = new Random(3);
        private final int[][] board = new int[N][N];
        // cached art
        private Image tileLight;
        private Image tileDark;
        private Image redMan;
        private Image redKing;
        private Image blackMan;
        private Image blackKing;
        private Image shadow;
        private Image moveMark;
        private Image selectMark;
        // sprites that change with the board
        private final List<Sprite> dynamic = new ArrayList<Sprite>();

        private float tileW;
        private float tileH;
        private float originX;
        private float originY;
        private boolean ready;

        private int turn = RED;
        private int selR = -1;
        private int selC = -1;
        private boolean chaining;       // selected piece is mid multi-jump
        private double aiTimer;         // delay before the AI plays, for readability
        private boolean gameOver;

        BoardView() {
            setClearColor(0xff10212b);
            setupBoard();
        }

        private void setupBoard() {
            for (int r = 0; r < N; r++) {
                for (int c = 0; c < N; c++) {
                    board[r][c] = EMPTY;
                    if (isPlayable(r, c)) {
                        if (r < 3) {
                            board[r][c] = BLACK_MAN;
                        } else if (r > 4) {
                            board[r][c] = RED_MAN;
                        }
                    }
                }
            }
        }

        /// Pieces live on the dark squares only.
        private static boolean isPlayable(int r, int c) {
            return ((r + c) & 1) == 1;
        }

        // ---- isometric projection -------------------------------------------

        private float tileCenterX(int r, int c) {
            return originX + (c - r) * (tileW / 2f);
        }

        private float tileCenterY(int r, int c) {
            return originY + (c + r) * (tileH / 2f);
        }

        /// Inverse projection: which board cell does a screen pixel fall on? Returns
        /// {row, col} or null if the tap missed the board.
        private int[] pick(int px, int py) {
            float a = (px - originX) / (tileW / 2f);     // = c - r
            float b = (py - originY) / (tileH / 2f);     // = c + r
            int c = Math.round((a + b) / 2f);
            int r = Math.round((b - a) / 2f);
            if (r < 0 || r >= N || c < 0 || c >= N) {
                return null;
            }
            return new int[]{r, c};
        }

        private void layout() {
            int w = getWidth();
            int h = getHeight();
            // board spans 8*tileW wide and 4*tileW tall; fit within the view
            tileW = Math.min(w * 0.95f / N, h * 0.78f / (N / 2f));
            tileH = tileW / 2f;
            originX = w / 2f;
            originY = h / 2f - (N - 1) * tileH / 2f;

            tileLight = makeTile(0xff3f6d52, 0xff345d45);
            tileDark = makeTile(0xff274539, 0xff1f372d);
            int disc = Math.round(tileW * 0.62f);
            redMan = makeDisc(disc, 0xffe7503a, 0xffb5341f, false);
            redKing = makeDisc(disc, 0xffe7503a, 0xffb5341f, true);
            blackMan = makeDisc(disc, 0xff37414a, 0xff1c242b, false);
            blackKing = makeDisc(disc, 0xff37414a, 0xff1c242b, true);
            shadow = makeShadow(Math.round(tileW * 0.62f));
            moveMark = makeDiamond(0x88ffe88a);
            selectMark = makeDiamond(0x99ffffff);

            // static tiles, laid out once
            for (int r = 0; r < N; r++) {
                for (int c = 0; c < N; c++) {
                    Sprite t = new Sprite(isPlayable(r, c) ? tileDark : tileLight);
                    t.setAnchor(0.5, 0.5);
                    t.setPosition(tileCenterX(r, c), tileCenterY(r, c));
                    t.setZOrder((r + c) * 4);
                    getScene().add(t);
                }
            }
            ready = true;
            syncPieces();
            title();
        }

        /// Rebuilds the dynamic sprites (pieces, shadows, selection and move markers)
        /// from the current board state. Cheap enough to redo on every move.
        private void syncPieces() {
            for (int i = 0; i < dynamic.size(); i++) {
                getScene().remove(dynamic.get(i));
            }
            dynamic.clear();

            List<int[]> highlights = selR >= 0 ? destinations(selR, selC) : null;
            for (int r = 0; r < N; r++) {
                for (int c = 0; c < N; c++) {
                    int v = board[r][c];
                    if (v == EMPTY) {
                        continue;
                    }
                    float cx = tileCenterX(r, c);
                    float cy = tileCenterY(r, c);
                    // a flat shadow on the tile...
                    addDynamic(shadow, cx, cy + tileH * 0.10f, (r + c) * 4 + 1, 0.5, 0.5);
                    // ...and the piece raised off it to read as 3D height
                    Image img = pieceImage(v);
                    addDynamic(img, cx, cy - tileH * 0.30f, (r + c) * 4 + 3, 0.5, 0.62);
                }
            }
            if (selR >= 0) {
                addDynamic(selectMark, tileCenterX(selR, selC), tileCenterY(selR, selC),
                        (selR + selC) * 4 + 2, 0.5, 0.5);
                if (highlights != null) {
                    for (int i = 0; i < highlights.size(); i++) {
                        int[] m = highlights.get(i);
                        addDynamic(moveMark, tileCenterX(m[0], m[1]), tileCenterY(m[0], m[1]),
                                (m[0] + m[1]) * 4 + 2, 0.5, 0.5);
                    }
                }
            }
        }

        private void addDynamic(Image img, float x, float y, int z, double ax, double ay) {
            Sprite s = new Sprite(img);
            s.setAnchor(ax, ay);
            s.setPosition(x, y);
            s.setZOrder(z);
            dynamic.add(s);
            getScene().add(s);
        }

        private Image pieceImage(int v) {
            switch (v) {
                case RED_MAN: return redMan;
                case RED_KING: return redKing;
                case BLACK_MAN: return blackMan;
                default: return blackKing;
            }
        }

        // ---- game rules ------------------------------------------------------

        private static int owner(int v) {
            if (v == RED_MAN || v == RED_KING) {
                return RED;
            }
            if (v == BLACK_MAN || v == BLACK_KING) {
                return BLACK;
            }
            return EMPTY;
        }

        private static boolean isKing(int v) {
            return v == RED_KING || v == BLACK_KING;
        }

        /// The legal destination cells for the piece at (r,c): each entry is
        /// {toRow, toCol, capturedRow, capturedCol} where the captured pair is -1,-1
        /// for a plain step. If any capture exists, only captures are returned (forced
        /// capture), which is also what makes chaining work.
        private List<int[]> destinations(int r, int c) {
            int v = board[r][c];
            List<int[]> steps = new ArrayList<int[]>();
            List<int[]> caps = new ArrayList<int[]>();
            int[] dr;
            int[] dc = {-1, 1, -1, 1};
            if (isKing(v)) {
                dr = new int[]{-1, -1, 1, 1};
            } else if (owner(v) == RED) {
                dr = new int[]{-1, -1, -1, -1};   // red moves up (row decreases)
            } else {
                dr = new int[]{1, 1, 1, 1};        // black moves down
            }
            for (int i = 0; i < 4; i++) {
                int nr = r + dr[i];
                int nc = c + dc[i];
                if (!isKing(v) && ((owner(v) == RED && dr[i] > 0) || (owner(v) == BLACK && dr[i] < 0))) {
                    continue;
                }
                if (!inBounds(nr, nc)) {
                    continue;
                }
                if (board[nr][nc] == EMPTY) {
                    steps.add(new int[]{nr, nc, -1, -1});
                } else if (owner(board[nr][nc]) != EMPTY && owner(board[nr][nc]) != owner(v)) {
                    int jr = nr + dr[i];
                    int jc = nc + dc[i];
                    if (inBounds(jr, jc) && board[jr][jc] == EMPTY) {
                        caps.add(new int[]{jr, jc, nr, nc});
                    }
                }
            }
            return caps.isEmpty() ? steps : caps;
        }

        private static boolean inBounds(int r, int c) {
            return r >= 0 && r < N && c >= 0 && c < N;
        }

        private boolean hasAnyCapture(int side) {
            for (int r = 0; r < N; r++) {
                for (int c = 0; c < N; c++) {
                    if (owner(board[r][c]) == side) {
                        List<int[]> d = destinations(r, c);
                        if (!d.isEmpty() && d.get(0)[2] >= 0) {
                            return true;
                        }
                    }
                }
            }
            return false;
        }

        /// Applies a move and returns true if the same piece must keep jumping.
        private boolean applyMove(int fr, int fc, int[] move) {
            int tr = move[0];
            int tc = move[1];
            int v = board[fr][fc];
            board[fr][fc] = EMPTY;
            boolean captured = move[2] >= 0;
            if (captured) {
                board[move[2]][move[3]] = EMPTY;
            }
            board[tr][tc] = v;
            // crown on reaching the far row
            boolean crowned = false;
            if (v == RED_MAN && tr == 0) {
                board[tr][tc] = RED_KING;
                crowned = true;
            } else if (v == BLACK_MAN && tr == N - 1) {
                board[tr][tc] = BLACK_KING;
                crowned = true;
            }
            // a capture may chain into another jump (but not right after crowning)
            if (captured && !crowned) {
                List<int[]> next = destinations(tr, tc);
                if (!next.isEmpty() && next.get(0)[2] >= 0) {
                    selR = tr;
                    selC = tc;
                    return true;
                }
            }
            return false;
        }

        // ---- input & turn flow ----------------------------------------------

        protected void update(double dt) {
            if (!ready) {
                if (getWidth() > 0 && getHeight() > 0) {
                    layout();
                }
                return;
            }
            if (gameOver) {
                if (getInput().wasPointerPressed()) {
                    restart();
                }
                return;
            }
            if (turn == BLACK) {
                aiTimer -= dt;
                if (aiTimer <= 0) {
                    aiMove();
                }
                return;
            }
            handleTap();
        }

        private void handleTap() {
            if (!getInput().wasPointerPressed()) {
                return;
            }
            int[] cell = pick(getInput().getPointerX(), getInput().getPointerY());
            if (cell == null) {
                return;
            }
            int r = cell[0];
            int c = cell[1];
            if (selR >= 0) {
                // tapping a highlighted destination plays the move
                List<int[]> dests = destinations(selR, selC);
                for (int i = 0; i < dests.size(); i++) {
                    int[] m = dests.get(i);
                    if (m[0] == r && m[1] == c) {
                        boolean again = applyMove(selR, selC, m);
                        if (again) {
                            chaining = true;   // locked to this piece until the jumps run out
                        } else {
                            endTurn();
                        }
                        syncPieces();
                        return;
                    }
                }
            }
            // otherwise (re)select one of our own pieces, unless locked mid-chain
            if (!chaining && owner(board[r][c]) == RED) {
                selR = r;
                selC = c;
                syncPieces();
            }
        }

        private void endTurn() {
            selR = -1;
            selC = -1;
            chaining = false;
            turn = turn == RED ? BLACK : RED;
            aiTimer = 0.55;
            if (!hasAnyMove(turn)) {
                gameOver = true;
            }
            title();
        }

        private boolean hasAnyMove(int side) {
            for (int r = 0; r < N; r++) {
                for (int c = 0; c < N; c++) {
                    if (owner(board[r][c]) == side && !destinations(r, c).isEmpty()) {
                        return true;
                    }
                }
            }
            return false;
        }

        /// A small AI: prefer captures (and chain them), otherwise a random legal step.
        private void aiMove() {
            boolean mustCapture = hasAnyCapture(BLACK);
            List<int[]> pool = new ArrayList<int[]>();   // {r, c, destIndex}
            for (int r = 0; r < N; r++) {
                for (int c = 0; c < N; c++) {
                    if (owner(board[r][c]) != BLACK) {
                        continue;
                    }
                    List<int[]> d = destinations(r, c);
                    for (int i = 0; i < d.size(); i++) {
                        boolean cap = d.get(i)[2] >= 0;
                        if (mustCapture == cap) {
                            pool.add(new int[]{r, c, i});
                        }
                    }
                }
            }
            if (pool.isEmpty()) {
                gameOver = true;
                title();
                return;
            }
            int[] pickMove = pool.get(rnd.nextInt(pool.size()));
            int fr = pickMove[0];
            int fc = pickMove[1];
            boolean again = applyMove(fr, fc, destinations(fr, fc).get(pickMove[2]));
            if (again) {
                // chain the next jump after another short beat
                aiTimer = 0.45;
            } else {
                endTurn();
            }
            syncPieces();
        }

        private void restart() {
            setupBoard();
            selR = -1;
            selC = -1;
            chaining = false;
            gameOver = false;
            turn = RED;
            aiTimer = 0;
            syncPieces();
            title();
        }

        private void title() {
            final String msg;
            if (gameOver) {
                msg = (turn == RED ? "Black wins" : "Red wins") + " - tap to restart";
            } else {
                msg = turn == RED ? "Checkers - your move (red)" : "Checkers - black thinking";
            }
            CN.callSerially(new Runnable() {
                public void run() {
                    Form f = Display.getInstance().getCurrent();
                    if (f != null) {
                        f.setTitle(msg);
                    }
                }
            });
        }

        // ---- runtime art -----------------------------------------------------

        /// An isometric diamond tile with a subtle top/bottom shade for depth.
        private Image makeTile(int top, int bottom) {
            int w = Math.max(2, Math.round(tileW));
            int h = Math.max(2, Math.round(tileH));
            Image img = Image.createImage(w, h, 0);
            Graphics g = img.getGraphics();
            g.setAntiAliased(true);
            int[] xs = {w / 2, w - 1, w / 2, 0};
            int[] ys = {0, h / 2, h - 1, h / 2};
            g.setColor(top);
            g.fillPolygon(xs, ys, 4);
            // darker lower half for a hint of thickness
            g.setColor(bottom);
            int[] lx = {0, w / 2, w - 1, w / 2};
            int[] ly = {h / 2, h - 1, h / 2, h - 1};
            g.fillPolygon(lx, ly, 4);
            g.setColor(0x33000000);
            g.drawPolygon(xs, ys, 4);
            return img;
        }

        /// A checker piece: an elliptical top, a side rim for height, and a crown ring
        /// when it is a king.
        private Image makeDisc(int size, int top, int side, boolean king) {
            int h = Math.round(size * 0.92f);
            Image img = Image.createImage(size, h, 0);
            Graphics g = img.getGraphics();
            g.setAntiAliased(true);
            int ellH = Math.round(size * 0.42f);   // height of the top ellipse
            int rim = Math.round(size * 0.22f);     // vertical thickness
            // side rim (a darker band under the top ellipse)
            g.setColor(side);
            g.fillArc(0, h - ellH - rim, size - 1, ellH, 180, 180);
            g.fillRect(0, h - ellH / 2 - rim, size, rim);
            g.fillArc(0, h - ellH - rim + rim, size - 1, ellH, 180, 180);
            // top face
            g.setColor(top);
            g.fillArc(0, h - ellH - rim, size - 1, ellH, 0, 360);
            // a soft highlight
            g.setColor(0x55ffffff);
            g.fillArc(size / 4, h - ellH - rim + ellH / 6, size / 3, ellH / 3, 0, 360);
            if (king) {
                g.setColor(0xfff4c542);
                int cx = size / 2;
                int cy = h - ellH - rim + ellH / 2;
                g.drawArc(cx - size / 5, cy - ellH / 5, size * 2 / 5, ellH * 2 / 5, 0, 360);
                g.fillArc(cx - 3, cy - 3, 6, 6, 0, 360);
            }
            return img;
        }

        private Image makeShadow(int size) {
            int h = Math.max(2, Math.round(size * 0.5f));
            Image img = Image.createImage(size, h, 0);
            Graphics g = img.getGraphics();
            g.setAntiAliased(true);
            g.setColor(0x44000000);
            g.fillArc(0, 0, size - 1, h - 1, 0, 360);
            return img;
        }

        private Image makeDiamond(int argb) {
            int w = Math.max(2, Math.round(tileW * 0.8f));
            int h = Math.max(2, Math.round(tileH * 0.8f));
            Image img = Image.createImage(w, h, 0);
            Graphics g = img.getGraphics();
            g.setAntiAliased(true);
            g.setColor(argb & 0xffffff);
            g.setAlpha((argb >>> 24) & 0xff);
            int[] xs = {w / 2, w - 1, w / 2, 0};
            int[] ys = {0, h / 2, h - 1, h / 2};
            g.fillPolygon(xs, ys, 4);
            g.setAlpha(255);
            return img;
        }
    }
}
