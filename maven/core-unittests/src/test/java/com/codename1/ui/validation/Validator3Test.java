package com.codename1.ui.validation;

import com.codename1.junit.FormTest;
import com.codename1.junit.UITestBase;
import com.codename1.ui.Component;
import com.codename1.ui.Container;
import com.codename1.ui.Form;
import com.codename1.ui.FontImage;
import com.codename1.ui.layouts.BoxLayout;
import com.codename1.ui.validation.Constraint;
import com.codename1.ui.validation.Validator;

public class Validator3Test extends UITestBase {

    public static class TestContainer extends Container {
        @Override
        public void setScrollX(int scrollX) {
            super.setScrollX(scrollX);
        }
        @Override
        public boolean isScrollable() {
            return true;
        }
        @Override
        public Component getScrollable() {
            return this;
        }
    }

    @FormTest
    public void testValidatorScrollListener() {
        Form f = new Form("Validator Test");
        f.setLayout(new BoxLayout(BoxLayout.Y_AXIS));

        TestContainer scrollableCmp = new TestContainer();
        scrollableCmp.setFocusable(true);
        f.add(scrollableCmp);

        Validator v = new Validator();
        v.setShowErrorMessageForFocusedComponent(true);

        v.addConstraint(scrollableCmp, new Constraint() {
            @Override
            public boolean isValid(Object value) {
                return false;
            }

            @Override
            public String getDefaultFailMessage() {
                return "Failed";
            }
        });

        v.setValidationFailureHighlightMode(Validator.HighlightMode.EMBLEM);
        v.setValidationFailedEmblem(FontImage.createMaterial(FontImage.MATERIAL_ERROR, "Error", 4));

        f.show();
        scrollableCmp.requestFocus();

        // Trigger scroll
        scrollableCmp.setScrollX(50);
    }
}
