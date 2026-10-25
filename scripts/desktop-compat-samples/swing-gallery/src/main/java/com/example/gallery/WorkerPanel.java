/*
 * Copyright (c) 2026, Codename One and/or its affiliates. All rights reserved.
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
package com.example.gallery;

import java.awt.BorderLayout;
import java.util.List;
import java.util.concurrent.ExecutionException;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.SwingWorker;

/**
 * Work done off the event dispatch thread, reporting back as it goes.
 */
public class WorkerPanel extends JPanel {

    private final JProgressBar progress = new JProgressBar(0, 100);
    private final JTextArea log = new JTextArea(10, 40);
    private final JButton start = new JButton("Find primes");
    private final JButton cancel = new JButton("Cancel");
    private PrimeFinder worker;

    public WorkerPanel() {
        super(new BorderLayout(6, 6));
        setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));
        log.setEditable(false);
        cancel.setEnabled(false);
        start.addActionListener(e -> start());
        cancel.addActionListener(e -> worker.cancel(true));

        JPanel top = new JPanel(new BorderLayout(6, 6));
        JPanel buttons = new JPanel();
        buttons.add(start);
        buttons.add(cancel);
        top.add(buttons, BorderLayout.WEST);
        top.add(progress, BorderLayout.CENTER);
        add(top, BorderLayout.NORTH);
        add(new JScrollPane(log), BorderLayout.CENTER);
    }

    private void start() {
        log.setText("");
        start.setEnabled(false);
        cancel.setEnabled(true);
        worker = new PrimeFinder(20000);
        worker.addPropertyChangeListener(e -> {
            if ("progress".equals(e.getPropertyName())) {
                progress.setValue((Integer) e.getNewValue());
            }
        });
        worker.execute();
    }

    /** Counts the primes below a limit, publishing each thousandth one. */
    private class PrimeFinder extends SwingWorker<Integer, Integer> {

        private final int limit;

        PrimeFinder(int limit) {
            this.limit = limit;
        }

        @Override
        protected Integer doInBackground() throws Exception {
            int found = 0;
            for (int n = 2; n < limit && !isCancelled(); n++) {
                if (isPrime(n)) {
                    found++;
                    if (found % 250 == 0) {
                        publish(n);
                    }
                }
                setProgress(n * 100 / limit);
            }
            return found;
        }

        private boolean isPrime(int n) {
            for (int d = 2; (long) d * d <= n; d++) {
                if (n % d == 0) {
                    return false;
                }
            }
            return true;
        }

        @Override
        protected void process(List<Integer> chunks) {
            for (Integer prime : chunks) {
                log.append("... " + prime + "\n");
            }
        }

        @Override
        protected void done() {
            start.setEnabled(true);
            cancel.setEnabled(false);
            if (isCancelled()) {
                log.append("Cancelled\n");
                return;
            }
            try {
                progress.setValue(100);
                log.append(get() + " primes below " + limit + "\n");
            } catch (InterruptedException | ExecutionException e) {
                log.append("Failed: " + e.getMessage() + "\n");
            }
        }
    }
}
