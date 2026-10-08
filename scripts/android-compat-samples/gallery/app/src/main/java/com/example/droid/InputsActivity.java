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
package com.example.droid;

import android.app.Activity;
import android.app.DatePickerDialog;
import android.app.ProgressDialog;
import android.app.TimePickerDialog;
import android.os.Bundle;
import android.os.Handler;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.DatePicker;
import android.widget.NumberPicker;
import android.widget.TextView;
import android.widget.TimePicker;

public class InputsActivity extends Activity {
    private TextView status;

    @Override
    protected void onCreate(Bundle b) {
        super.onCreate(b);
        setContentView(R.layout.activity_inputs);
        status = findViewById(R.id.inputs_status);
        AutoCompleteTextView fruit = findViewById(R.id.fruit);
        fruit.setAdapter(new ArrayAdapter<String>(this, android.R.layout.simple_dropdown_item_1line,
                new String[] {"Apple", "Apricot", "Avocado", "Banana", "Blueberry"}));
        NumberPicker number = findViewById(R.id.number);
        number.setMinValue(1);
        number.setMaxValue(10);
        number.setValue(5);
        number.setOnValueChangedListener(new NumberPicker.OnValueChangeListener() {
            @Override
            public void onValueChange(NumberPicker p, int oldVal, int newVal) {
                status.setText("number=" + newVal);
            }
        });
        findViewById(R.id.pick_date).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                new DatePickerDialog(InputsActivity.this, new DatePickerDialog.OnDateSetListener() {
                    @Override
                    public void onDateSet(DatePicker view, int y, int m, int d) {
                        status.setText("date=" + y + "-" + (m + 1) + "-" + d);
                    }
                }, 2026, 9, 2).show();
            }
        });
        findViewById(R.id.pick_time).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                new TimePickerDialog(InputsActivity.this, new TimePickerDialog.OnTimeSetListener() {
                    @Override
                    public void onTimeSet(TimePicker view, int h, int min) {
                        status.setText("time=" + h + ":" + min);
                    }
                }, 9, 30, false).show();
            }
        });
        findViewById(R.id.progress).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                final ProgressDialog d = ProgressDialog.show(InputsActivity.this, "Working", "Please wait...");
                new Handler().postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        d.dismiss();
                        status.setText("done waiting");
                    }
                }, 1500);
            }
        });
    }
}
