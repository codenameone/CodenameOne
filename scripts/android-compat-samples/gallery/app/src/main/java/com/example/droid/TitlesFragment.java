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

import android.app.ListFragment;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.Toast;

public class TitlesFragment extends ListFragment {
    @Override
    public void onActivityCreated(Bundle b) {
        super.onActivityCreated(b);
        setHasOptionsMenu(true);
        setListAdapter(new ArrayAdapter<String>(getActivity(), android.R.layout.simple_list_item_1,
                new String[] {"One", "Two", "Three"}));
    }

    @Override
    public void onListItemClick(ListView l, View v, int pos, long id) {
        getFragmentManager().beginTransaction().replace(R.id.detail, DetailFragment.newInstance(pos))
                .addToBackStack(null).commit();
    }

    @Override
    public void onCreateOptionsMenu(Menu m, MenuInflater i) {
        m.add(0, 42, 0, "Depth");
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem it) {
        if (it.getItemId() != 42) {
            return false;
        }
        Toast.makeText(getActivity(), "depth=" + getFragmentManager().getBackStackEntryCount(),
                Toast.LENGTH_SHORT).show();
        return true;
    }
}
