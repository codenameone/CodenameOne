/*
 * Copyright (c) 2026 Codename One and contributors.
 * Licensed under the PolyForm Noncommercial License 1.0.0.
 * You may use this file only in compliance with that license.
 * The license notice for this subtree is available in Ports/JavaScriptPort/LICENSE.md.
 */

package com.codename1.impl.html5;

import com.codename1.push.PushAction;
import com.codename1.push.PushActionCategory;
import com.codename1.push.PushActionsProvider;
import com.codename1.push.PushCallback;
import com.codename1.ui.Display;
import com.codename1.html5.js.JSBody;
import com.codename1.html5.js.JSFunctor;
import com.codename1.html5.js.JSObject;
import com.codename1.html5.js.core.JSArray;
import com.codename1.html5.js.core.JSString;


/**
 *
 * @author shannah
 */
public class HTML5Push {
    private static PushCallback pushCallback;
    
    static void setPushCallback(PushCallback cb) {
        pushCallback = cb;
    }
    
    
    @JSFunctor
    public static interface PushRegisterSuccessCallback extends JSObject {
        void onSuccess(JSString id);
    }
    
    @JSFunctor
    public static interface PushRegisterFailCallback extends JSObject {
        void onFail(JSString message);
    }
    
    @JSFunctor
    public static interface OnPushCallback extends JSObject {
        void onPush(JSObject message);
    }
    @JSBody(params={"o", "key"}, script="return o[key] || null;")
    private static native String getString(JSObject o, String key);
    
    @JSBody(params={"o", "key"}, script="return o[key] || 0;")
    private static native int getInt(JSObject o, String key);
    
    @JSBody(params={"o", "key"}, script="return o[key] || 0;")
    private static native boolean getBoolean(JSObject o, String key);
    
    @JSBody(params={"onSuccess", "onFail", "onPush", "pushActionCategories"}, script="window.cn1_registerPush(onSuccess, onFail, onPush, pushActionCategories)")
    private static native void registerPushNative(PushRegisterSuccessCallback onSuccess, PushRegisterFailCallback onFail, OnPushCallback onPush, JSArray pushActionCategories);
    
    @JSBody(params={"id", "title", "icon"}, script="return {action:id, title:title}")
    private static native JSObject createPushAction(String id, String title, String icon);
    
    @JSBody(params={"id", "actions"}, script="return {id:id, actions:actions}")
    private static native JSObject createPushActionCategory(String id, JSArray actions);
        
    private static JSObject convertPushActionToJSObject(PushAction action) {
        return createPushAction(action.getId(), action.getTitle(), action.getIcon());
    }
    
    private static JSObject convertPushActionCategoryToJSObject(PushActionCategory category) {
        PushAction[] actions = category.getActions();
        int len = actions.length;
        JSArray<JSObject> jactions = JSArray.create(len);
        for (int i=0; i<len; i++) {
            jactions.set(i, convertPushActionToJSObject(actions[i]));
        }
        return createPushActionCategory(category.getId(), jactions);
        
    }
    
    private static JSArray<JSObject> convertPushActionCategoriesToJSArray(PushActionCategory[] categories) {
        int len = categories.length;
        JSArray<JSObject> out = JSArray.create(len);
        for (int i=0; i<len; i++) {
            out.set(i, convertPushActionCategoryToJSObject(categories[i]));
        }
        return out;
    }
    
    private static void initPushContent(String message, String image, String messageType, String category, String action) {
        com.codename1.push.PushContent.reset();
        if (image != null) {
            com.codename1.push.PushContent.setImageUrl(image);
        }
        if (category != null) {
            com.codename1.push.PushContent.setCategory(category);
        }
        if (action != null) {
            com.codename1.push.PushContent.setActionId(action);
        }
        int iMessageType = 1;
        try {iMessageType = Integer.parseInt(messageType);}catch(Throwable t){}
        com.codename1.push.PushContent.setType(iMessageType);
        switch (iMessageType) {
            case 1:
            case 5:
                com.codename1.push.PushContent.setBody(message);
                com.codename1.push.PushContent.setType(1);
                break;
                
            case 2: com.codename1.push.PushContent.setMetaData(message);break;
            case 3: {
                String[] parts = message.split(";");
                com.codename1.push.PushContent.setMetaData(parts[1]);
                com.codename1.push.PushContent.setBody(parts[0]);
                break;
            }
            case 4: {
                String[] parts = message.split(";");
                com.codename1.push.PushContent.setTitle(parts[0]);
                com.codename1.push.PushContent.setBody(parts[1]);
                break;
            }
            case 101: {
                com.codename1.push.PushContent.setBody(message.substring(message.indexOf(" ") + 1));
                com.codename1.push.PushContent.setType(1);
                break;
            }
            case 102: {
                String[] parts = message.split(";");
                com.codename1.push.PushContent.setTitle(parts[1]);
                com.codename1.push.PushContent.setBody(parts[2]);
                com.codename1.push.PushContent.setType(2);
                break;
            }
        }
    }
    
    static void registerPush() {
        PushRegisterSuccessCallback onSuccess = new PushRegisterSuccessCallback() {

            @Override
            public void onSuccess(final JSString _id) {
                final String id = _id.stringValue();
                HTML5Implementation.callSerially(new Runnable() {
                    public void run() {
                        HTML5Implementation.getInstance()._registerServerPush(id);
                    }
                });
            }
        };
        
        OnPushCallback onPush = new OnPushCallback() {

            @Override
            public void onPush(final JSObject data) {
                //System.out.println("In onPush");
                final int messageType = getInt(data, "messageType");
                final String image = getString(data, "image");
                final String dataCategory = getString(data, "category");
                final String dataAction = getString(data, "action");
                String _message = null;
                final String pushType = String.valueOf(messageType);
                boolean isVisual = getBoolean(data, "visual");
                switch (messageType) {
                    
                    case 2:
                    
                        // Hidden... will be stored in "meta"
                        _message = getString(data, "meta");
                        break;
                    case 3:
                        if (isVisual) {
                            _message = getString(data, "alertBody");
                        } else {
                            _message = getString(data, "meta");
                        }
                        break;
                    case 4:
                        _message = getString(data, "alertTitle") + ";" + getString(data, "alertBody");
                        break;
                    case 100:
                        HTML5Implementation.callSerially(new Runnable() {
                                public void run() {
                                    int num = getInt(data, "badge");
                                    Display.getInstance().setBadgeNumber(num);
                                }
                        });
                        return;
                    case 101:
                        _message = getString(data, "alertBody");
                        HTML5Implementation.callSerially(new Runnable() {
                                public void run() {
                                    int num = getInt(data, "badge");
                                    Display.getInstance().setBadgeNumber(num);
                                }
                        });
                        break;
                    case 102:
                        if (isVisual) {
                            _message = getString(data, "alertBody");
                            
                        } else {
                            _message = getString(data, "meta");
                        }
                        HTML5Implementation.callSerially(new Runnable() {
                                public void run() {
                                    int num = getInt(data, "badge");
                                    Display.getInstance().setBadgeNumber(num);
                                }
                        });
                        break;
                    default :
                        _message = getString(data, "alertBody");
                        break;
                        
                }
                //System.out.println("About to try to push to callback");
                if (_message != null) {
                    //System.out.println("Message is "+_message);
                    final String message = _message;
                    HTML5Implementation.callSerially(new Runnable() {
                        public void run() {
                            //System.out.println("Now on EDT about to call _pushReceived");
                            Display.getInstance().setProperty("pushType", pushType);
                            initPushContent(message, image, String.valueOf(messageType), dataCategory, dataAction);
                            HTML5Implementation.getInstance()._pushReceived(message);
                        }
                    });
                }
            }
        };
        
        PushRegisterFailCallback onFail = new PushRegisterFailCallback() {

            @Override
            public void onFail(final JSString _message) {
                final String message = _message.stringValue();
                HTML5Implementation.callSerially(new Runnable() {
                    public void run() {
                        HTML5Implementation.getInstance()._sendPushRegistrationError(message, -1);
                    }
                });
                
            }
        };
        
        JSArray<JSObject> pushActionCategories = null;
        if (pushCallback instanceof PushActionsProvider) {
            pushActionCategories = convertPushActionCategoriesToJSArray(((PushActionsProvider)pushCallback).getPushActionCategories());
        }
        registerPushNative(onSuccess, onFail, onPush, pushActionCategories);
    }
}
