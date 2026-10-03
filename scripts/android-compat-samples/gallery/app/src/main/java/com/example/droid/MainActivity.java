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
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import java.io.BufferedReader;
import java.io.InputStreamReader;

public class MainActivity extends Activity {
    private int clicks;
    private TextView counter;
    private EditText name;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        counter = findViewById(R.id.counter);
        name = findViewById(R.id.name);
        SharedPreferences prefs = getSharedPreferences("main", MODE_PRIVATE);
        clicks = prefs.getInt("clicks", 0);
        update();
        Button count = findViewById(R.id.count);
        count.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                clicks++;
                getSharedPreferences("main", MODE_PRIVATE).edit().putInt("clicks", clicks).apply();
                update();
            }
        });
        TextView footer = findViewById(R.id.footer);
        try {
            BufferedReader r = new BufferedReader(new InputStreamReader(getAssets().open("readme.txt")));
            footer.setText(r.readLine());
            r.close();
        } catch (Exception e) {
            footer.setText("asset missing: " + e);
        }
        Log.i("Droid", "onCreate done");
    }

    private void update() {
        if (clicks == 0) {
            counter.setText("Not clicked yet");
        } else {
            counter.setText(getResources().getQuantityString(R.plurals.clicks, clicks, clicks));
        }
    }

    public void openDetails(View v) {
        Intent i = new Intent(this, DetailActivity.class);
        i.putExtra("name", name.getText().toString());
        startActivityForResult(i, 7);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == 7 && resultCode == RESULT_OK) {
            Toast.makeText(this, "Back from details", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.action_material) {
            startActivity(new Intent(this, MaterialActivity.class));
            return true;
        }
        if (item.getItemId() == R.id.action_recycler) {
            startActivity(new Intent(this, RecyclerActivity.class));
            return true;
        }
        if (item.getItemId() == R.id.action_star) {
            startActivity(new Intent(this, WidgetsActivity.class));
            return true;
        }
        if (item.getItemId() == R.id.action_fragments) {
            startActivity(new Intent(this, FragmentsActivity.class));
            return true;
        }
        if (item.getItemId() == R.id.action_inputs) {
            startActivity(new Intent(this, InputsActivity.class));
            return true;
        }
        if (item.getItemId() == R.id.action_jetpack) {
            startActivity(new Intent(this, JetpackActivity.class));
            return true;
        }
        if (item.getItemId() == R.id.action_constraint) {
            startActivity(new Intent(this, ConstraintActivity.class));
            return true;
        }
        if (item.getItemId() == R.id.action_compat) {
            startActivity(new Intent(this, CompatActivity.class));
            return true;
        }
        if (item.getItemId() == R.id.action_anim) {
            startActivity(new Intent(this, AnimActivity.class));
            return true;
        }
        if (item.getItemId() == R.id.action_kotlin) {
            startActivity(new Intent(this, KotlinActivity.class));
            return true;
        }
        if (item.getItemId() == R.id.action_data) {
            startActivity(new Intent(this, DataActivity.class));
            return true;
        }
        if (item.getItemId() == R.id.action_list) {
            startActivity(new Intent(this, FruitListActivity.class));
            return true;
        }
        if (item.getItemId() == R.id.action_reset) {
            clicks = 0;
            update();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
}
