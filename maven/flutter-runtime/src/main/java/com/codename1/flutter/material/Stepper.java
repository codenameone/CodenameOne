package com.codename1.flutter.material;

import com.codename1.flutter.BuildContext;
import com.codename1.flutter.CrossAxisAlignment;
import com.codename1.flutter.MainAxisSize;
import com.codename1.flutter.StatelessWidget;
import com.codename1.flutter.Widget;
import com.codename1.flutter.widgets.Column;

import dart.core.DartList;
import dart.runtime.Funcs;

/**
 * A vertical (or horizontal) sequence of {@link Step}s — Flutter's
 * {@code Stepper}. This milestone renders every step's title followed by its
 * content in a {@link Column}; the collapse-to-current-step behavior and the
 * continue/cancel controls are deferred.
 */
public class Stepper extends StatelessWidget {

    private DartList<Step> steps;
    private StepperType type;
    private long currentStep;
    private Funcs.VoidFunc1<Long> onStepTapped;
    private Funcs.VoidFunc0 onStepContinue;
    private Funcs.VoidFunc0 onStepCancel;

    public void steps(DartList<Step> v) {
        this.steps = v;
    }

    public void physics(Object v) {
    }

    public void type(StepperType v) {
        this.type = v;
    }

    public void currentStep(long v) {
        this.currentStep = v;
    }

    public void onStepTapped(Funcs.VoidFunc1<Long> v) {
        this.onStepTapped = v;
    }

    public void onStepContinue(Funcs.VoidFunc0 v) {
        this.onStepContinue = v;
    }

    public void onStepCancel(Funcs.VoidFunc0 v) {
        this.onStepCancel = v;
    }

    public void controlsBuilder(Object v) {
    }

    public void elevation(double v) {
    }

    public void margin(Object v) {
    }

    @Override
    public Widget build(BuildContext context) {
        DartList<Widget> kids = new DartList<Widget>();
        if (steps != null) {
            for (int i = 0; i < steps.size(); i++) {
                Step s = steps.get(i);
                if (s.getTitle() != null) {
                    kids.add(s.getTitle());
                }
                if (s.getSubtitle() != null) {
                    kids.add(s.getSubtitle());
                }
                if (s.getContent() != null) {
                    kids.add(s.getContent());
                }
            }
        }
        Column col = new Column();
        col.crossAxisAlignment(CrossAxisAlignment.stretch);
        col.mainAxisSize(MainAxisSize.min);
        col.children(kids);
        return col;
    }
}
