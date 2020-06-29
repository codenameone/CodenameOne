// Copyright (c) 2014 The Chromium Embedded Framework Authors. All rights
// reserved. Use of this source code is governed by a BSD-style license that
// can be found in the LICENSE file.

package com.codename1.impl.javase.cef;


import java.awt.Container;
import java.awt.Frame;
import java.util.HashMap;
import java.util.Map;
import java.util.Vector;

import org.cef.browser.CefBrowser;
import org.cef.browser.CefFrame;
import org.cef.callback.CefContextMenuParams;
import org.cef.callback.CefMenuModel;
import org.cef.callback.CefMenuModel.MenuId;
import org.cef.handler.CefContextMenuHandler;

//import tests.detailed.dialog.SearchDialog;
//import tests.detailed.dialog.ShowTextDialog;

public class ContextMenuHandler implements CefContextMenuHandler {
    private final Container owner_;
    private Map<Integer, String> suggestions_ = new HashMap<Integer, String>();

    public ContextMenuHandler(Container owner) {
        owner_ = owner;
    }

    @Override
    public void onBeforeContextMenu(
            CefBrowser browser, CefFrame frame, CefContextMenuParams params, CefMenuModel model) {
        /*
        model.clear();

        // Navigation menu
        model.addItem(MenuId.MENU_ID_BACK, "Back");
        model.setEnabled(MenuId.MENU_ID_BACK, browser.canGoBack());

        model.addItem(MenuId.MENU_ID_FORWARD, "Forward");
        model.setEnabled(MenuId.MENU_ID_FORWARD, browser.canGoForward());

        model.addSeparator();
        model.addItem(MenuId.MENU_ID_FIND, "Find...");
        if (params.hasImageContents() && params.getSourceUrl() != null)
            model.addItem(MenuId.MENU_ID_USER_FIRST, "Download Image...");
        model.addItem(MenuId.MENU_ID_VIEW_SOURCE, "View Source...");

        Vector<String> suggestions = new Vector<String>();
        params.getDictionarySuggestions(suggestions);

        // Spell checking menu
        model.addSeparator();
        if (suggestions.size() == 0) {
            model.addItem(MenuId.MENU_ID_NO_SPELLING_SUGGESTIONS, "No suggestions");
            model.setEnabled(MenuId.MENU_ID_NO_SPELLING_SUGGESTIONS, false);
            return;
        }

        int id = MenuId.MENU_ID_SPELLCHECK_SUGGESTION_0;
        for (String suggestedWord : suggestions) {
            model.addItem(id, suggestedWord);
            suggestions_.put(id, suggestedWord);
            if (++id > MenuId.MENU_ID_SPELLCHECK_SUGGESTION_LAST) break;
        }
*/
    }

    @Override
    public boolean onContextMenuCommand(CefBrowser browser, CefFrame frame,
            CefContextMenuParams params, int commandId, int eventFlags) {
        /*
        switch (commandId) {
            case MenuId.MENU_ID_VIEW_SOURCE:
                ShowTextDialog visitor =
                        new ShowTextDialog(owner_, "Source of \"" + browser.getURL() + "\"");
                browser.getSource(visitor);
                return true;
            case MenuId.MENU_ID_FIND:
                SearchDialog search = new SearchDialog(owner_, browser);
                search.setVisible(true);
                return true;
            case MenuId.MENU_ID_USER_FIRST:
                browser.startDownload(params.getSourceUrl());
                return true;
            default:
                if (commandId >= MenuId.MENU_ID_SPELLCHECK_SUGGESTION_0) {
                    String newWord = suggestions_.get(commandId);
                    if (newWord != null) {
                        System.err.println(
                                "replacing " + params.getMisspelledWord() + " with " + newWord);
                        browser.replaceMisspelling(newWord);
                        return true;
                    }
                }
                return false;
        }
        */
        return false;
    }

    @Override
    public void onContextMenuDismissed(CefBrowser browser, CefFrame frame) {
        //suggestions_.clear();
    }
}
