/*
 * Copyright (c) 2012, Codename One and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.  Codename One designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Oracle in the LICENSE file that accompanied this code.
 *  
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 2 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 * 
 * You should have received a copy of the GNU General Public License version
 * 2 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 * 
 * Please contact Codename One through http://www.codenameone.com/ if you 
 * need additional information or have any questions.
 */
package com.codename1.testing;

import com.codename1.components.ToastBar;
import com.codename1.io.Log;
import com.codename1.ui.*;

import static com.codename1.ui.ComponentSelector.$;

import com.codename1.ui.events.ActionEvent;
import com.codename1.ui.events.ActionListener;
import com.codename1.ui.layouts.BorderLayout;
import com.codename1.ui.layouts.BoxLayout;
import java.util.ArrayList;

/**
 * A UI component for running unit tests and displaying the results.
 * 
 * <p>The sample code below demonstrates usage.</p>
 * <script src="https://gist.github.com/shannah/c54df11845911f928a76b71992a0f76b.js"></script>
 * <img src="https://www.codenameone.com/img/developer-guide/testrunnercomponent.png" alt="TestRunnerComponnet" />
 * @author Steve Hannah
 * @since 7.0
 * 
 */
public class TestRunnerComponent extends Container {
    private ArrayList<AbstractTest> tests = new ArrayList<AbstractTest>();
    private Container resultsPane = new Container(BoxLayout.y());
    public TestRunnerComponent() {
        super(new BorderLayout());
        Button btn = new Button("Run Tests");
        btn.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent evt) {
                CN.callSerially(new Runnable() {
                    public void run() {
                        runTests();
                    }
                });
            }
        });
        add(CN.NORTH, btn);
        resultsPane.setScrollableY(true);
        add(CN.CENTER, resultsPane);
    }
    
    /**
     * Adds tests to the test runner.
     * @param tests The tests to add.
     * @return Self for chaining.
     */
    public TestRunnerComponent add(AbstractTest... tests) {
        for (AbstractTest test : tests) {
            this.tests.add(test);
        }
        return this;
    }
    
    /**
     * Shows a form with the testrunner embedded in it.
     * @return The form.
     */
    public Form showForm() {
        Form f = getComponentForm();
        if (f == null) {
            f = new Form("Test Runner", new BorderLayout());
            f.add(CN.CENTER, this);
        }
        f.show();
        return f;
    }
    
    
    private void runTest(final AbstractTest test, final Button statusLabel) {
        try {
            
            if (test.runTest()) {
                CN.callSerially(new Runnable() {
                    public void run() {
                        statusLabel.setText(test+": Passed");
                        $(statusLabel).selectAllStyles().setBgColor(0x00ff00).revalidate();
                    }
                });
               
            } else {
                CN.callSerially(new Runnable() {
                    public void run() {
                        statusLabel.setText(test+": Failed");
                        $(statusLabel).selectAllStyles().setBgColor(0xff0000).revalidate();
                    }
                });
                
            }

        } catch (final Throwable t) {
            Log.e(t);
            CN.callSerially(new Runnable() {
                public void run() {
                    statusLabel.setText(test+": Failed");
                    $(statusLabel).selectAllStyles().setBgColor(0xff0000).revalidate();
                    
                    statusLabel.addActionListener(new ActionListener() {
                        public void actionPerformed(ActionEvent ev) {
                            ToastBar.showInfoMessage(t.getMessage());
                            Log.e(t);
                        }
                    });
                }
            });
            



        }
    }
    
    /**
     * Runs all of the tests in the test running.
     */
    public void runTests() {
        Form f = getComponentForm();
        resultsPane.removeAll();
        resultsPane.revalidate();
        resultsPane.add(new Label("Running "+tests.size()+" tests"));
        for (final AbstractTest test : tests) {
            final Button statusLabel = new Button(test+": Running...");
            if (f != CN.getCurrentForm()) {
                
                f.showBack();
            }
            $(statusLabel).selectAllStyles().setBgColor(0xffff00).setBgTransparency(0xff);
            resultsPane.add(statusLabel);
            resultsPane.revalidate();
            if (test.shouldExecuteOnEDT()) {
                runTest(test, statusLabel);
            } else {
                CN.invokeAndBlock(new Runnable() {
                    public void run() {
                        runTest(test, statusLabel);
                    }
                });
            }
            resultsPane.revalidate();
        }
        if (f != CN.getCurrentForm()) {
            f.showBack();
        }
    }
}
