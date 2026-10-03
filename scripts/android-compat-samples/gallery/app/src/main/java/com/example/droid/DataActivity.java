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
import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.view.View;
import android.widget.ListView;
import android.widget.SimpleCursorAdapter;
import android.widget.TextView;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStreamReader;
import java.io.PrintWriter;

public class DataActivity extends Activity {
    private SQLiteDatabase db;
    private SimpleCursorAdapter adapter;
    private int added;

    @Override
    protected void onCreate(Bundle b) {
        super.onCreate(b);
        setContentView(R.layout.activity_data);
        TextView fileStatus = findViewById(R.id.file_status);
        try {
            File f = new File(getFilesDir(), "visits.txt");
            int visits = 0;
            if (f.exists()) {
                BufferedReader r = new BufferedReader(new InputStreamReader(new FileInputStream(f), "UTF-8"));
                visits = Integer.parseInt(r.readLine().trim());
                r.close();
            }
            visits++;
            PrintWriter w = new PrintWriter(new FileOutputStream(f));
            w.println(visits);
            w.close();
            fileStatus.setText("visits=" + visits + " files=" + getFilesDir().list().length
                    + " size=" + f.length());
        } catch (Exception e) {
            fileStatus.setText("file error: " + e);
        }
        db = new NotesDb(this).getWritableDatabase();
        adapter = new SimpleCursorAdapter(this, android.R.layout.simple_list_item_2, query(),
                new String[] {"text", "_id"}, new int[] {android.R.id.text1, android.R.id.text2}, 0);
        ListView notes = findViewById(R.id.notes);
        notes.setAdapter(adapter);
        findViewById(R.id.add_note).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                ContentValues cv = new ContentValues();
                cv.put("text", "Note " + (++added));
                cv.put("created", System.currentTimeMillis());
                db.insert("notes", null, cv);
                adapter.changeCursor(query());
            }
        });
    }

    private Cursor query() {
        return db.query("notes", new String[] {"_id", "text"}, null, null, null, null, "_id DESC");
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        adapter.changeCursor(null);
        db.close();
    }
}
