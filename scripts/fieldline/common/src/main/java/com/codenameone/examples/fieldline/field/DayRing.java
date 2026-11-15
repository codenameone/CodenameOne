package com.codenameone.examples.fieldline.field;

import com.codename1.ui.CN;
import com.codename1.ui.Component;
import com.codename1.ui.Font;
import com.codename1.ui.Graphics;
import com.codename1.ui.geom.Dimension;
import com.codename1.ui.plaf.Style;
import com.codename1.ui.plaf.UIManager;

/// How far through the day a technician is: a ring that fills as jobs are
/// signed off, with the count in the middle. The arc eases to a new value
/// rather than jumping, so finishing a job is felt as well as seen.
public final class DayRing extends Component {
    private int done;
    private int total;
    private float shown;
    private float target;

    public DayRing() {
        setUIID("FlRing");
        setFocusable(false);
    }

    public void set(int doneJobs, int allJobs) {
        done = doneJobs;
        total = allJobs;
        target = allJobs == 0 ? 0 : doneJobs / (float) allJobs;
        if (getComponentForm() != null) {
            getComponentForm().registerAnimated(this);
        } else {
            shown = target;
        }
        repaint();
    }

    @Override
    protected void initComponent() {
        super.initComponent();
        if (shown != target) {
            getComponentForm().registerAnimated(this);
        }
    }

    @Override
    protected void deinitialize() {
        if (getComponentForm() != null) {
            getComponentForm().deregisterAnimated(this);
        }
        super.deinitialize();
    }

    @Override
    public boolean animate() {
        float delta = target - shown;
        if (Math.abs(delta) < 0.004f) {
            shown = target;
            if (getComponentForm() != null) {
                getComponentForm().deregisterAnimated(this);
            }
            return true;
        }
        shown += delta * 0.18f;
        return true;
    }

    @Override
    protected Dimension calcPreferredSize() {
        int side = CN.convertToPixels(15f);
        return new Dimension(side, side);
    }

    @Override
    public void paint(Graphics g) {
        Style s = getStyle();
        int side = Math.min(getWidth(), getHeight());
        int x = getX() + (getWidth() - side) / 2;
        int y = getY() + (getHeight() - side) / 2;
        int stroke = Math.max(4, side / 9);
        g.setAntiAliased(true);
        int track = UIManager.getInstance().getComponentStyle("FlRingTrack").getFgColor();
        g.setColor(track);
        g.fillArc(x, y, side, side, 0, 360);
        g.setColor(s.getFgColor());
        int sweep = Math.round(360 * shown);
        if (sweep > 0) {
            g.fillArc(x, y, side, side, 90, -sweep);
        }
        int inner = side - 2 * stroke;
        g.setColor(s.getBgColor());
        g.fillArc(x + stroke, y + stroke, inner, inner, 0, 360);
        Font big = Font.createTrueTypeFont(Font.NATIVE_MAIN_BOLD, Font.NATIVE_MAIN_BOLD)
                .derive(side * 0.26f, Font.STYLE_BOLD);
        Font small = Font.createTrueTypeFont(Font.NATIVE_MAIN_REGULAR, Font.NATIVE_MAIN_REGULAR)
                .derive(side * 0.12f, Font.STYLE_PLAIN);
        String count = done + "/" + total;
        String caption = com.codenameone.examples.fieldline.ui.Lang.tr("done");
        int h = big.getHeight() + small.getHeight();
        int top = y + (side - h) / 2;
        g.setColor(s.getFgColor());
        g.setFont(big);
        g.drawString(count, x + (side - big.stringWidth(count)) / 2, top);
        g.setColor(UIManager.getInstance().getComponentStyle("FlRingCaption").getFgColor());
        g.setFont(small);
        g.drawString(caption, x + (side - small.stringWidth(caption)) / 2, top + big.getHeight());
    }
}
