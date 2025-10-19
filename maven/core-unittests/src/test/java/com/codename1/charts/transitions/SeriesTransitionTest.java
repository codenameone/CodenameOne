package com.codename1.charts.transitions;

import com.codename1.charts.ChartComponent;
import com.codename1.charts.compat.Canvas;
import com.codename1.charts.compat.Paint;
import com.codename1.charts.renderers.SimpleSeriesRenderer;
import com.codename1.charts.views.AbstractChart;
import com.codename1.test.UITestBase;
import com.codename1.ui.Form;
import com.codename1.ui.animations.Animation;
import com.codename1.ui.animations.Motion;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class SeriesTransitionTest extends UITestBase {
    private RecordingForm form;
    private TestChartComponent chartComponent;

    @BeforeEach
    void setupChartComponent() {
        form = new RecordingForm();
        chartComponent = new TestChartComponent(new RecordingChart());
        chartComponent.setWidth(100);
        chartComponent.setHeight(100);
        form.add(chartComponent);
    }

    @Test
    void animateChartRegistersAnimationAndRunsToCompletion() throws Exception {
        TestSeriesTransition transition = new TestSeriesTransition(chartComponent, SeriesTransition.EASING_LINEAR, 10);

        transition.animateChart();
        assertTrue(form.registeredAnimations.contains(transition));

        Motion motion = getMotion(transition);
        motion.finish();

        assertTrue(transition.animate());
        assertFalse(transition.animate());
        assertTrue(transition.cleanupCalled);
        assertTrue(form.deregisteredAnimations.contains(transition));
        assertEquals(100, transition.progressUpdates.get(transition.progressUpdates.size() - 1));
    }

    @Test
    void updateChartForcesRepaint() {
        TestSeriesTransition transition = new TestSeriesTransition(chartComponent, SeriesTransition.EASING_LINEAR, 10);

        transition.updateChart();

        assertTrue(chartComponent.repaintCalled);
    }

    @Test
    void settersUpdateConfiguration() {
        TestSeriesTransition transition = new TestSeriesTransition(chartComponent, SeriesTransition.EASING_LINEAR, 10);
        ChartComponent otherComponent = new TestChartComponent(new RecordingChart());

        transition.setDuration(250);
        assertEquals(250, transition.getDuration());

        transition.setEasing(SeriesTransition.EASING_IN_OUT);
        assertEquals(SeriesTransition.EASING_IN_OUT, transition.getEasing());

        transition.setChart(otherComponent);
        assertSame(otherComponent, transition.getChart());
    }

    @Test
    void initTransitionUsesExpectedMotionForEachEasing() throws Exception {
        Map<Integer, Integer> expectedMotionTypes = new HashMap<>();
        expectedMotionTypes.put(SeriesTransition.EASING_LINEAR, getMotionType(Motion.createLinearMotion(0, 100, 1)));
        expectedMotionTypes.put(SeriesTransition.EASING_IN, getMotionType(Motion.createEaseInMotion(0, 100, 1)));
        expectedMotionTypes.put(SeriesTransition.EASING_OUT, getMotionType(Motion.createEaseOutMotion(0, 100, 1)));
        expectedMotionTypes.put(SeriesTransition.EASING_IN_OUT, getMotionType(Motion.createEaseInOutMotion(0, 100, 1)));

        for (Map.Entry<Integer, Integer> entry : expectedMotionTypes.entrySet()) {
            TestSeriesTransition transition = new TestSeriesTransition(chartComponent, entry.getKey(), 10);
            transition.animateChart();
            Motion motion = getMotion(transition);
            assertEquals(entry.getValue(), getMotionType(motion));
            motion.setCurrentMotionTime(transition.getDuration() + 1);
            transition.animate();
            transition.animate();
        }
    }

    private Motion getMotion(SeriesTransition transition) throws Exception {
        Field motionField = SeriesTransition.class.getDeclaredField("motion");
        motionField.setAccessible(true);
        return (Motion) motionField.get(transition);
    }

    private int getMotionType(Motion motion) throws Exception {
        Field typeField = Motion.class.getDeclaredField("motionType");
        typeField.setAccessible(true);
        return typeField.getInt(motion);
    }

    private static class RecordingForm extends Form {
        private final List<Animation> registeredAnimations = new ArrayList<>();
        private final List<Animation> deregisteredAnimations = new ArrayList<>();

        @Override
        public void registerAnimated(Animation cmp) {
            registeredAnimations.add(cmp);
            super.registerAnimated(cmp);
        }

        @Override
        public void deregisterAnimated(Animation cmp) {
            deregisteredAnimations.add(cmp);
            super.deregisterAnimated(cmp);
        }
    }

    private static class RecordingChart extends AbstractChart {
        @Override
        public void draw(Canvas canvas, int x, int y, int width, int height, Paint paint) {
        }

        @Override
        public int getLegendShapeWidth(int seriesIndex) {
            return 0;
        }

        @Override
        public void drawLegendShape(Canvas canvas, SimpleSeriesRenderer renderer, float x, float y, int seriesIndex, Paint paint) {
        }
    }

    private static class TestChartComponent extends ChartComponent {
        private boolean repaintCalled;

        TestChartComponent(AbstractChart chart) {
            super(chart);
        }

        @Override
        public void repaint() {
            repaintCalled = true;
            super.repaint();
        }
    }

    private static class TestSeriesTransition extends SeriesTransition {
        private final List<Integer> progressUpdates = new ArrayList<>();
        private boolean cleanupCalled;

        TestSeriesTransition(ChartComponent chart, int easing, int duration) {
            super(chart, easing, duration);
        }

        @Override
        protected void update(int progress) {
            progressUpdates.add(progress);
        }

        @Override
        protected void cleanup() {
            cleanupCalled = true;
        }
    }
}
