package bsh.cn1.gen;

import bsh.cn1.CN1AccessException;

public final class GeneratedAccess_com_codename1_notifications {
    private GeneratedAccess_com_codename1_notifications() {
    }

    public static Class<?> findClass(String name) {
        int lastDot = name == null ? -1 : name.lastIndexOf('.');
        if (lastDot < 0 || lastDot == name.length() - 1) {
            return null;
        }
        return findClassBySimpleName(name.substring(lastDot + 1));
    }

    public static Class<?> findClassBySimpleName(String simpleName) {
        Class<?> found0 = findClassChunk0(simpleName);
        if (found0 != null) {
            return found0;
        }
        return null;
    }


    private static Class<?> findClassChunk0(String simpleName) {
        if ("LocalNotification".equals(simpleName)) {
            return com.codename1.notifications.LocalNotification.class;
        }
        if ("Action".equals(simpleName)) {
            return com.codename1.notifications.LocalNotification.Action.class;
        }
        if ("MessagingStyle".equals(simpleName)) {
            return com.codename1.notifications.LocalNotification.MessagingStyle.class;
        }
        if ("Message".equals(simpleName)) {
            return com.codename1.notifications.LocalNotification.MessagingStyle.Message.class;
        }
        if ("LocalNotificationCallback".equals(simpleName)) {
            return com.codename1.notifications.LocalNotificationCallback.class;
        }
        if ("NotificationChannelBuilder".equals(simpleName)) {
            return com.codename1.notifications.NotificationChannelBuilder.class;
        }
        if ("NotificationPermissionCallback".equals(simpleName)) {
            return com.codename1.notifications.NotificationPermissionCallback.class;
        }
        if ("NotificationPermissionRequest".equals(simpleName)) {
            return com.codename1.notifications.NotificationPermissionRequest.class;
        }
        if ("NotificationPermissionResult".equals(simpleName)) {
            return com.codename1.notifications.NotificationPermissionResult.class;
        }
        if ("AuthorizationLevel".equals(simpleName)) {
            return com.codename1.notifications.NotificationPermissionResult.AuthorizationLevel.class;
        }
        return null;
    }
    public static Object construct(Class<?> type, Object[] args) throws Exception {
        Object[] safeArgs = safeArgs(args);
        if (type == com.codename1.notifications.LocalNotification.Action.class) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class, java.lang.String.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class, java.lang.String.class}, false);
                return new com.codename1.notifications.LocalNotification.Action((java.lang.String) adaptedArgs[0], (java.lang.String) adaptedArgs[1]);
            }
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class, java.lang.String.class, java.lang.String.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class, java.lang.String.class, java.lang.String.class}, false);
                return new com.codename1.notifications.LocalNotification.Action((java.lang.String) adaptedArgs[0], (java.lang.String) adaptedArgs[1], (java.lang.String) adaptedArgs[2]);
            }
        }
        if (type == com.codename1.notifications.LocalNotification.MessagingStyle.class) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class}, false);
                return new com.codename1.notifications.LocalNotification.MessagingStyle((java.lang.String) adaptedArgs[0]);
            }
        }
        if (type == com.codename1.notifications.LocalNotification.MessagingStyle.Message.class) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class, java.lang.Long.class, java.lang.String.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class, java.lang.Long.class, java.lang.String.class}, false);
                return new com.codename1.notifications.LocalNotification.MessagingStyle.Message((java.lang.String) adaptedArgs[0], ((Number) adaptedArgs[1]).longValue(), (java.lang.String) adaptedArgs[2]);
            }
        }
        if (type == com.codename1.notifications.NotificationChannelBuilder.class) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class, java.lang.String.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class, java.lang.String.class}, false);
                return new com.codename1.notifications.NotificationChannelBuilder((java.lang.String) adaptedArgs[0], (java.lang.String) adaptedArgs[1]);
            }
        }
        if (type == com.codename1.notifications.NotificationPermissionResult.class) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.notifications.NotificationPermissionResult.AuthorizationLevel.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.notifications.NotificationPermissionResult.AuthorizationLevel.class}, false);
                return new com.codename1.notifications.NotificationPermissionResult((com.codename1.notifications.NotificationPermissionResult.AuthorizationLevel) adaptedArgs[0]);
            }
        }
        throw unsupportedConstruct(type, safeArgs);
    }

    public static Object invokeStatic(Class<?> type, String name, Object[] args) throws Exception {
        Object[] safeArgs = safeArgs(args);
        if (type == com.codename1.notifications.NotificationChannelBuilder.class) return invokeStatic0(name, safeArgs);
        throw unsupportedStatic(type, name, safeArgs);
    }

    private static Object invokeStatic0(String name, Object[] safeArgs) throws Exception {
        if ("createChannelGroup".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class, java.lang.String.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class, java.lang.String.class}, false);
                com.codename1.notifications.NotificationChannelBuilder.createChannelGroup((java.lang.String) adaptedArgs[0], (java.lang.String) adaptedArgs[1]); return null;
            }
        }
        if ("deleteChannel".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class}, false);
                com.codename1.notifications.NotificationChannelBuilder.deleteChannel((java.lang.String) adaptedArgs[0]); return null;
            }
        }
        throw unsupportedStatic(com.codename1.notifications.NotificationChannelBuilder.class, name, safeArgs);
    }

    public static Object invoke(Object target, String name, Object[] args) throws Exception {
        Object[] safeArgs = safeArgs(args);
        CN1AccessException unsupported = null;
        if (target instanceof com.codename1.notifications.LocalNotification) {
            try {
                return invoke0((com.codename1.notifications.LocalNotification) target, name, safeArgs);
            } catch (CN1AccessException ex) {
                unsupported = ex;
            }
        }
        if (target instanceof com.codename1.notifications.LocalNotification.Action) {
            try {
                return invoke1((com.codename1.notifications.LocalNotification.Action) target, name, safeArgs);
            } catch (CN1AccessException ex) {
                unsupported = ex;
            }
        }
        if (target instanceof com.codename1.notifications.LocalNotification.MessagingStyle) {
            try {
                return invoke2((com.codename1.notifications.LocalNotification.MessagingStyle) target, name, safeArgs);
            } catch (CN1AccessException ex) {
                unsupported = ex;
            }
        }
        if (target instanceof com.codename1.notifications.LocalNotification.MessagingStyle.Message) {
            try {
                return invoke3((com.codename1.notifications.LocalNotification.MessagingStyle.Message) target, name, safeArgs);
            } catch (CN1AccessException ex) {
                unsupported = ex;
            }
        }
        if (target instanceof com.codename1.notifications.NotificationChannelBuilder) {
            try {
                return invoke4((com.codename1.notifications.NotificationChannelBuilder) target, name, safeArgs);
            } catch (CN1AccessException ex) {
                unsupported = ex;
            }
        }
        if (target instanceof com.codename1.notifications.NotificationPermissionRequest) {
            try {
                return invoke5((com.codename1.notifications.NotificationPermissionRequest) target, name, safeArgs);
            } catch (CN1AccessException ex) {
                unsupported = ex;
            }
        }
        if (target instanceof com.codename1.notifications.NotificationPermissionResult) {
            try {
                return invoke6((com.codename1.notifications.NotificationPermissionResult) target, name, safeArgs);
            } catch (CN1AccessException ex) {
                unsupported = ex;
            }
        }
        if (target instanceof com.codename1.notifications.LocalNotificationCallback) {
            try {
                return invoke7((com.codename1.notifications.LocalNotificationCallback) target, name, safeArgs);
            } catch (CN1AccessException ex) {
                unsupported = ex;
            }
        }
        if (target instanceof com.codename1.notifications.NotificationPermissionCallback) {
            try {
                return invoke8((com.codename1.notifications.NotificationPermissionCallback) target, name, safeArgs);
            } catch (CN1AccessException ex) {
                unsupported = ex;
            }
        }
        if (unsupported != null) {
            throw unsupported;
        }
        throw unsupportedInstance(target, name, safeArgs);
    }

    private static Object invoke0(com.codename1.notifications.LocalNotification typedTarget, String name, Object[] safeArgs) throws Exception {
        if ("addAction".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.notifications.LocalNotification.Action.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.notifications.LocalNotification.Action.class}, false);
                return typedTarget.addAction((com.codename1.notifications.LocalNotification.Action) adaptedArgs[0]);
            }
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class, java.lang.String.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class, java.lang.String.class}, false);
                return typedTarget.addAction((java.lang.String) adaptedArgs[0], (java.lang.String) adaptedArgs[1]);
            }
        }
        if ("addInputAction".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class, java.lang.String.class, java.lang.String.class, java.lang.String.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class, java.lang.String.class, java.lang.String.class, java.lang.String.class}, false);
                return typedTarget.addInputAction((java.lang.String) adaptedArgs[0], (java.lang.String) adaptedArgs[1], (java.lang.String) adaptedArgs[2], (java.lang.String) adaptedArgs[3]);
            }
        }
        if ("asMessagingStyle".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class}, false);
                return typedTarget.asMessagingStyle((java.lang.String) adaptedArgs[0]);
            }
        }
        if ("getActions".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getActions();
            }
        }
        if ("getAlertBody".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getAlertBody();
            }
        }
        if ("getAlertImage".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getAlertImage();
            }
        }
        if ("getAlertSound".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getAlertSound();
            }
        }
        if ("getAlertTitle".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getAlertTitle();
            }
        }
        if ("getBadgeNumber".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getBadgeNumber();
            }
        }
        if ("getChannelId".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getChannelId();
            }
        }
        if ("getCustomView".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getCustomView();
            }
        }
        if ("getGroupId".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getGroupId();
            }
        }
        if ("getId".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getId();
            }
        }
        if ("getMessagingStyle".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getMessagingStyle();
            }
        }
        if ("getProgress".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getProgress();
            }
        }
        if ("getProgressMax".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getProgressMax();
            }
        }
        if ("isForeground".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.isForeground();
            }
        }
        if ("isFullScreenIntent".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.isFullScreenIntent();
            }
        }
        if ("isGroupSummary".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.isGroupSummary();
            }
        }
        if ("isOngoing".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.isOngoing();
            }
        }
        if ("isProgressIndeterminate".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.isProgressIndeterminate();
            }
        }
        if ("isTimeSensitive".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.isTimeSensitive();
            }
        }
        if ("setAlertBody".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class}, false);
                typedTarget.setAlertBody((java.lang.String) adaptedArgs[0]); return null;
            }
        }
        if ("setAlertImage".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class}, false);
                typedTarget.setAlertImage((java.lang.String) adaptedArgs[0]); return null;
            }
        }
        if ("setAlertSound".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class}, false);
                typedTarget.setAlertSound((java.lang.String) adaptedArgs[0]); return null;
            }
        }
        if ("setAlertTitle".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class}, false);
                typedTarget.setAlertTitle((java.lang.String) adaptedArgs[0]); return null;
            }
        }
        if ("setBadgeNumber".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class}, false);
                typedTarget.setBadgeNumber(toIntValue(adaptedArgs[0])); return null;
            }
        }
        if ("setChannelId".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class}, false);
                return typedTarget.setChannelId((java.lang.String) adaptedArgs[0]);
            }
        }
        if ("setCustomView".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class}, false);
                return typedTarget.setCustomView((java.lang.String) adaptedArgs[0]);
            }
        }
        if ("setForeground".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false);
                typedTarget.setForeground(((Boolean) adaptedArgs[0]).booleanValue()); return null;
            }
        }
        if ("setFullScreenIntent".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false);
                return typedTarget.setFullScreenIntent(((Boolean) adaptedArgs[0]).booleanValue());
            }
        }
        if ("setGroup".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class}, false);
                return typedTarget.setGroup((java.lang.String) adaptedArgs[0]);
            }
        }
        if ("setGroupSummary".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false);
                return typedTarget.setGroupSummary(((Boolean) adaptedArgs[0]).booleanValue());
            }
        }
        if ("setId".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class}, false);
                typedTarget.setId((java.lang.String) adaptedArgs[0]); return null;
            }
        }
        if ("setIndeterminateProgress".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false);
                return typedTarget.setIndeterminateProgress(((Boolean) adaptedArgs[0]).booleanValue());
            }
        }
        if ("setOngoing".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false);
                return typedTarget.setOngoing(((Boolean) adaptedArgs[0]).booleanValue());
            }
        }
        if ("setProgress".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class, java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class, java.lang.Integer.class}, false);
                return typedTarget.setProgress(toIntValue(adaptedArgs[0]), toIntValue(adaptedArgs[1]));
            }
        }
        if ("setSound".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class}, false);
                return typedTarget.setSound((java.lang.String) adaptedArgs[0]);
            }
        }
        if ("setTimeSensitive".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false);
                return typedTarget.setTimeSensitive(((Boolean) adaptedArgs[0]).booleanValue());
            }
        }
        throw unsupportedInstance(typedTarget, name, safeArgs);
    }

    private static Object invoke1(com.codename1.notifications.LocalNotification.Action typedTarget, String name, Object[] safeArgs) throws Exception {
        if ("getIcon".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getIcon();
            }
        }
        if ("getId".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getId();
            }
        }
        if ("getTextInputButtonText".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getTextInputButtonText();
            }
        }
        if ("getTextInputPlaceholder".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getTextInputPlaceholder();
            }
        }
        if ("getTitle".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getTitle();
            }
        }
        if ("isTextInput".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.isTextInput();
            }
        }
        throw unsupportedInstance(typedTarget, name, safeArgs);
    }

    private static Object invoke2(com.codename1.notifications.LocalNotification.MessagingStyle typedTarget, String name, Object[] safeArgs) throws Exception {
        if ("addMessage".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class, java.lang.Long.class, java.lang.String.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class, java.lang.Long.class, java.lang.String.class}, false);
                return typedTarget.addMessage((java.lang.String) adaptedArgs[0], ((Number) adaptedArgs[1]).longValue(), (java.lang.String) adaptedArgs[2]);
            }
        }
        if ("conversationTitle".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class}, false);
                return typedTarget.conversationTitle((java.lang.String) adaptedArgs[0]);
            }
        }
        if ("getConversationTitle".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getConversationTitle();
            }
        }
        if ("getMessages".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getMessages();
            }
        }
        if ("getSelfDisplayName".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getSelfDisplayName();
            }
        }
        if ("groupConversation".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false);
                return typedTarget.groupConversation(((Boolean) adaptedArgs[0]).booleanValue());
            }
        }
        if ("isGroupConversation".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.isGroupConversation();
            }
        }
        throw unsupportedInstance(typedTarget, name, safeArgs);
    }

    private static Object invoke3(com.codename1.notifications.LocalNotification.MessagingStyle.Message typedTarget, String name, Object[] safeArgs) throws Exception {
        if ("getSenderName".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getSenderName();
            }
        }
        if ("getText".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getText();
            }
        }
        if ("getTimestamp".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getTimestamp();
            }
        }
        throw unsupportedInstance(typedTarget, name, safeArgs);
    }

    private static Object invoke4(com.codename1.notifications.NotificationChannelBuilder typedTarget, String name, Object[] safeArgs) throws Exception {
        if ("description".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class}, false);
                return typedTarget.description((java.lang.String) adaptedArgs[0]);
            }
        }
        if ("enableLights".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false);
                return typedTarget.enableLights(((Boolean) adaptedArgs[0]).booleanValue());
            }
        }
        if ("enableVibration".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false);
                return typedTarget.enableVibration(((Boolean) adaptedArgs[0]).booleanValue());
            }
        }
        if ("getDescription".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getDescription();
            }
        }
        if ("getGroup".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getGroup();
            }
        }
        if ("getId".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getId();
            }
        }
        if ("getImportance".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getImportance();
            }
        }
        if ("getLightColor".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getLightColor();
            }
        }
        if ("getLockscreenVisibility".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getLockscreenVisibility();
            }
        }
        if ("getName".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getName();
            }
        }
        if ("getSound".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getSound();
            }
        }
        if ("getVibrationPattern".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getVibrationPattern();
            }
        }
        if ("group".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class}, false);
                return typedTarget.group((java.lang.String) adaptedArgs[0]);
            }
        }
        if ("importance".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class}, false);
                return typedTarget.importance(toIntValue(adaptedArgs[0]));
            }
        }
        if ("isLightsEnabled".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.isLightsEnabled();
            }
        }
        if ("isShowBadge".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.isShowBadge();
            }
        }
        if ("isVibrationEnabled".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.isVibrationEnabled();
            }
        }
        if ("lightColor".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class}, false);
                return typedTarget.lightColor(toIntValue(adaptedArgs[0]));
            }
        }
        if ("lockscreenVisibility".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class}, false);
                return typedTarget.lockscreenVisibility(toIntValue(adaptedArgs[0]));
            }
        }
        if ("register".equals(name)) {
            if (safeArgs.length == 0) {
                typedTarget.register(); return null;
            }
        }
        if ("showBadge".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false);
                return typedTarget.showBadge(((Boolean) adaptedArgs[0]).booleanValue());
            }
        }
        if ("sound".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class}, false);
                return typedTarget.sound((java.lang.String) adaptedArgs[0]);
            }
        }
        if ("vibrationPattern".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{long[].class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{long[].class}, false);
                return typedTarget.vibrationPattern((long[]) adaptedArgs[0]);
            }
        }
        throw unsupportedInstance(typedTarget, name, safeArgs);
    }

    private static Object invoke5(com.codename1.notifications.NotificationPermissionRequest typedTarget, String name, Object[] safeArgs) throws Exception {
        if ("alert".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false);
                return typedTarget.alert(((Boolean) adaptedArgs[0]).booleanValue());
            }
        }
        if ("announcement".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false);
                return typedTarget.announcement(((Boolean) adaptedArgs[0]).booleanValue());
            }
        }
        if ("badge".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false);
                return typedTarget.badge(((Boolean) adaptedArgs[0]).booleanValue());
            }
        }
        if ("carPlay".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false);
                return typedTarget.carPlay(((Boolean) adaptedArgs[0]).booleanValue());
            }
        }
        if ("critical".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false);
                return typedTarget.critical(((Boolean) adaptedArgs[0]).booleanValue());
            }
        }
        if ("isAlert".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.isAlert();
            }
        }
        if ("isAnnouncement".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.isAnnouncement();
            }
        }
        if ("isBadge".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.isBadge();
            }
        }
        if ("isCarPlay".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.isCarPlay();
            }
        }
        if ("isCritical".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.isCritical();
            }
        }
        if ("isProvidesAppSettings".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.isProvidesAppSettings();
            }
        }
        if ("isProvisional".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.isProvisional();
            }
        }
        if ("isSound".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.isSound();
            }
        }
        if ("isTimeSensitive".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.isTimeSensitive();
            }
        }
        if ("providesAppSettings".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false);
                return typedTarget.providesAppSettings(((Boolean) adaptedArgs[0]).booleanValue());
            }
        }
        if ("provisional".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false);
                return typedTarget.provisional(((Boolean) adaptedArgs[0]).booleanValue());
            }
        }
        if ("sound".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false);
                return typedTarget.sound(((Boolean) adaptedArgs[0]).booleanValue());
            }
        }
        if ("timeSensitive".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false);
                return typedTarget.timeSensitive(((Boolean) adaptedArgs[0]).booleanValue());
            }
        }
        if ("toAuthorizationOptionsMask".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.toAuthorizationOptionsMask();
            }
        }
        throw unsupportedInstance(typedTarget, name, safeArgs);
    }

    private static Object invoke6(com.codename1.notifications.NotificationPermissionResult typedTarget, String name, Object[] safeArgs) throws Exception {
        if ("getAuthorizationLevel".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getAuthorizationLevel();
            }
        }
        if ("isGranted".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.isGranted();
            }
        }
        if ("isProvisional".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.isProvisional();
            }
        }
        throw unsupportedInstance(typedTarget, name, safeArgs);
    }

    private static Object invoke7(com.codename1.notifications.LocalNotificationCallback typedTarget, String name, Object[] safeArgs) throws Exception {
        if ("localNotificationReceived".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class}, false);
                typedTarget.localNotificationReceived((java.lang.String) adaptedArgs[0]); return null;
            }
        }
        throw unsupportedInstance(typedTarget, name, safeArgs);
    }

    private static Object invoke8(com.codename1.notifications.NotificationPermissionCallback typedTarget, String name, Object[] safeArgs) throws Exception {
        if ("notificationPermissionResult".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.notifications.NotificationPermissionResult.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.notifications.NotificationPermissionResult.class}, false);
                typedTarget.notificationPermissionResult((com.codename1.notifications.NotificationPermissionResult) adaptedArgs[0]); return null;
            }
        }
        throw unsupportedInstance(typedTarget, name, safeArgs);
    }

    public static Object getStaticField(Class<?> type, String name) throws Exception {
        if (type == com.codename1.notifications.LocalNotification.class) return getStaticField0(name);
        if (type == com.codename1.notifications.NotificationChannelBuilder.class) return getStaticField1(name);
        if (type == com.codename1.notifications.NotificationPermissionResult.AuthorizationLevel.class) return getStaticField2(name);
        throw unsupportedStaticField(type, name);
    }

    private static Object getStaticField0(String name) throws Exception {
        if ("REPEAT_DAY".equals(name)) return com.codename1.notifications.LocalNotification.REPEAT_DAY;
        if ("REPEAT_HOUR".equals(name)) return com.codename1.notifications.LocalNotification.REPEAT_HOUR;
        if ("REPEAT_MINUTE".equals(name)) return com.codename1.notifications.LocalNotification.REPEAT_MINUTE;
        if ("REPEAT_NONE".equals(name)) return com.codename1.notifications.LocalNotification.REPEAT_NONE;
        if ("REPEAT_WEEK".equals(name)) return com.codename1.notifications.LocalNotification.REPEAT_WEEK;
        throw unsupportedStaticField(com.codename1.notifications.LocalNotification.class, name);
    }

    private static Object getStaticField1(String name) throws Exception {
        if ("IMPORTANCE_DEFAULT".equals(name)) return com.codename1.notifications.NotificationChannelBuilder.IMPORTANCE_DEFAULT;
        if ("IMPORTANCE_HIGH".equals(name)) return com.codename1.notifications.NotificationChannelBuilder.IMPORTANCE_HIGH;
        if ("IMPORTANCE_LOW".equals(name)) return com.codename1.notifications.NotificationChannelBuilder.IMPORTANCE_LOW;
        if ("IMPORTANCE_MAX".equals(name)) return com.codename1.notifications.NotificationChannelBuilder.IMPORTANCE_MAX;
        if ("IMPORTANCE_MIN".equals(name)) return com.codename1.notifications.NotificationChannelBuilder.IMPORTANCE_MIN;
        if ("IMPORTANCE_NONE".equals(name)) return com.codename1.notifications.NotificationChannelBuilder.IMPORTANCE_NONE;
        if ("VISIBILITY_PRIVATE".equals(name)) return com.codename1.notifications.NotificationChannelBuilder.VISIBILITY_PRIVATE;
        if ("VISIBILITY_PUBLIC".equals(name)) return com.codename1.notifications.NotificationChannelBuilder.VISIBILITY_PUBLIC;
        if ("VISIBILITY_SECRET".equals(name)) return com.codename1.notifications.NotificationChannelBuilder.VISIBILITY_SECRET;
        throw unsupportedStaticField(com.codename1.notifications.NotificationChannelBuilder.class, name);
    }

    private static Object getStaticField2(String name) throws Exception {
        if ("AUTHORIZED".equals(name)) return com.codename1.notifications.NotificationPermissionResult.AuthorizationLevel.AUTHORIZED;
        if ("DENIED".equals(name)) return com.codename1.notifications.NotificationPermissionResult.AuthorizationLevel.DENIED;
        if ("EPHEMERAL".equals(name)) return com.codename1.notifications.NotificationPermissionResult.AuthorizationLevel.EPHEMERAL;
        if ("NOT_DETERMINED".equals(name)) return com.codename1.notifications.NotificationPermissionResult.AuthorizationLevel.NOT_DETERMINED;
        if ("PROVISIONAL".equals(name)) return com.codename1.notifications.NotificationPermissionResult.AuthorizationLevel.PROVISIONAL;
        throw unsupportedStaticField(com.codename1.notifications.NotificationPermissionResult.AuthorizationLevel.class, name);
    }

    public static Object getField(Object target, String name) throws Exception {
        throw unsupportedField(target, name);
    }

    public static void setStaticField(Class<?> type, String name, Object value) throws Exception {
        throw unsupportedStaticFieldWrite(type, name, value);
    }

    public static void setField(Object target, String name, Object value) throws Exception {
        throw unsupportedFieldWrite(target, name, value);
    }

    private static Object[] safeArgs(Object[] args) {
        return args == null ? new Object[0] : args;
    }

    private static Object[] adaptArgs(Object[] args, Class<?>[] paramTypes, boolean varArgs) {
        if (args == null || args.length == 0) {
            return args == null ? new Object[0] : args;
        }
        Object[] adapted = args.clone();
        if (!varArgs) {
            for (int i = 0; i < Math.min(adapted.length, paramTypes.length); i++) {
                adapted[i] = adaptValue(adapted[i], paramTypes[i]);
            }
            return adapted;
        }
        if (paramTypes.length == 0) {
            return adapted;
        }
        int fixedCount = paramTypes.length - 1;
        for (int i = 0; i < Math.min(fixedCount, adapted.length); i++) {
            adapted[i] = adaptValue(adapted[i], paramTypes[i]);
        }
        Class<?> componentType = paramTypes[paramTypes.length - 1].getComponentType();
        for (int i = fixedCount; i < adapted.length; i++) {
            adapted[i] = adaptValue(adapted[i], componentType);
        }
        return adapted;
    }

    private static boolean isSamInterface(Class<?> type) {
        if (type == com.codename1.util.OnComplete.class) {
            return true;
        }
        if (type == com.codename1.util.SuccessCallback.class) {
            return true;
        }
        if (type == com.codename1.util.FailureCallback.class) {
            return true;
        }
        if (type == com.codename1.ui.events.ActionListener.class) {
            return true;
        }
        if (type == java.lang.Runnable.class) {
            return true;
        }
        if (type == com.codename1.ui.events.DataChangedListener.class) {
            return true;
        }
        if (type == com.codename1.ui.events.SelectionListener.class) {
            return true;
        }
        return false;
    }

    private static Object adaptLambdaValue(final bsh.cn1.CN1LambdaSupport.LambdaValue lambda, Class<?> type) {
        if (type == com.codename1.util.OnComplete.class) {
            return new com.codename1.util.OnComplete() {
                public void completed(java.lang.Object arg0) {
                    try {
                        lambda.invoke(new Object[]{arg0});
                    } catch (bsh.EvalError ex) {
                        throw new RuntimeException(ex);
                    }
                }
            };
        }
        if (type == com.codename1.util.SuccessCallback.class) {
            return new com.codename1.util.SuccessCallback() {
                public void onSucess(java.lang.Object arg0) {
                    try {
                        lambda.invoke(new Object[]{arg0});
                    } catch (bsh.EvalError ex) {
                        throw new RuntimeException(ex);
                    }
                }
            };
        }
        if (type == com.codename1.util.FailureCallback.class) {
            return new com.codename1.util.FailureCallback() {
                public void onError(java.lang.Object arg0, java.lang.Throwable arg1, int arg2, java.lang.String arg3) {
                    try {
                        lambda.invoke(new Object[]{arg0, arg1, arg2, arg3});
                    } catch (bsh.EvalError ex) {
                        throw new RuntimeException(ex);
                    }
                }
            };
        }
        if (type == com.codename1.ui.events.ActionListener.class) {
            return new com.codename1.ui.events.ActionListener() {
                public void actionPerformed(com.codename1.ui.events.ActionEvent arg0) {
                    try {
                        lambda.invoke(new Object[]{arg0});
                    } catch (bsh.EvalError ex) {
                        throw new RuntimeException(ex);
                    }
                }
            };
        }
        if (type == java.lang.Runnable.class) {
            return new java.lang.Runnable() {
                public void run() {
                    try {
                        lambda.invoke(new Object[0]);
                    } catch (bsh.EvalError ex) {
                        throw new RuntimeException(ex);
                    }
                }
            };
        }
        if (type == com.codename1.ui.events.DataChangedListener.class) {
            return new com.codename1.ui.events.DataChangedListener() {
                public void dataChanged(int arg0, int arg1) {
                    try {
                        lambda.invoke(new Object[]{arg0, arg1});
                    } catch (bsh.EvalError ex) {
                        throw new RuntimeException(ex);
                    }
                }
            };
        }
        if (type == com.codename1.ui.events.SelectionListener.class) {
            return new com.codename1.ui.events.SelectionListener() {
                public void selectionChanged(int arg0, int arg1) {
                    try {
                        lambda.invoke(new Object[]{arg0, arg1});
                    } catch (bsh.EvalError ex) {
                        throw new RuntimeException(ex);
                    }
                }
            };
        }
        return lambda;
    }

    private static Object adaptValue(Object value, Class<?> type) {
        if (!(value instanceof bsh.cn1.CN1LambdaSupport.LambdaValue)) {
            return value;
        }
        // Direct fit when LambdaValue already implements the target SAM
        // (Runnable, Function, Comparator, ...).
        if (type.isInstance(value)) {
            return value;
        }
        return adaptLambdaValue((bsh.cn1.CN1LambdaSupport.LambdaValue) value, type);
    }

    private static int toIntValue(Object value) {
        if (value instanceof Number) return ((Number) value).intValue();
        if (value instanceof Character) return (int) ((Character) value).charValue();
        throw new ClassCastException("Cannot coerce "
            + (value == null ? "null" : value.getClass().getName()) + " to int");
    }

    private static boolean matches(Object[] args, Class<?>[] paramTypes, boolean varArgs) {
        if (!varArgs) {
            if (args.length != paramTypes.length) {
                return false;
            }
            for (int i = 0; i < paramTypes.length; i++) {
                if (!matchesType(args[i], paramTypes[i])) {
                    return false;
                }
            }
            return true;
        }
        if (paramTypes.length == 0) {
            return true;
        }
        int fixedCount = paramTypes.length - 1;
        if (args.length < fixedCount) {
            return false;
        }
        for (int i = 0; i < fixedCount; i++) {
            if (!matchesType(args[i], paramTypes[i])) {
                return false;
            }
        }
        Class<?> componentType = paramTypes[paramTypes.length - 1].getComponentType();
        for (int i = fixedCount; i < args.length; i++) {
            if (!matchesType(args[i], componentType)) {
                return false;
            }
        }
        return true;
    }

    private static boolean matchesType(Object value, Class<?> type) {
        if (type == Object.class) {
            return true;
        }
        if (value == null) {
            return !type.isPrimitive();
        }
        if (type.isArray()) {
            return type.isInstance(value);
        }
        if ("boolean".equals(type.getName()) || type == Boolean.class) {
            return value instanceof Boolean;
        }
        if ("char".equals(type.getName()) || type == Character.class) {
            return value instanceof Character;
        }
        if ("byte".equals(type.getName()) || type == Byte.class || "short".equals(type.getName()) || type == Short.class
                || "int".equals(type.getName()) || type == Integer.class || "long".equals(type.getName()) || type == Long.class
                || "float".equals(type.getName()) || type == Float.class || "double".equals(type.getName()) || type == Double.class) {
            // Java widens char to int implicitly, so accept Character
            // for any int-or-larger numeric slot.
            return value instanceof Number || value instanceof Character;
        }
        if (value instanceof bsh.cn1.CN1LambdaSupport.LambdaValue) {
            // LambdaValue implements common SAMs directly (Runnable,
            // Function, Predicate, Comparator, ...). Also accept any
            // CN1 SAM the listener-bridge knows how to wrap.
            return type.isInstance(value) || isSamInterface(type);
        }
        return type.isInstance(value);
    }

    private static CN1AccessException unsupportedConstruct(Class<?> type, Object[] args) {
        return new CN1AccessException("Generated constructor dispatch not implemented for " + type.getName() + describeArgs(args));
    }

    private static CN1AccessException unsupportedStatic(Class<?> type, String name, Object[] args) {
        return new CN1AccessException("Generated static dispatch not implemented for " + type.getName() + "." + name + describeArgs(args));
    }

    private static CN1AccessException unsupportedInstance(Object target, String name, Object[] args) {
        return new CN1AccessException("Generated instance dispatch not implemented for " + target.getClass().getName() + "." + name + describeArgs(args));
    }

    private static CN1AccessException unsupportedStaticField(Class<?> type, String name) {
        return new CN1AccessException("Generated static field access not implemented for " + type.getName() + "." + name);
    }

    private static CN1AccessException unsupportedField(Object target, String name) {
        return new CN1AccessException("Generated field access not implemented for " + target.getClass().getName() + "." + name);
    }

    private static CN1AccessException unsupportedStaticFieldWrite(Class<?> type, String name, Object value) {
        return new CN1AccessException("Generated static field write not implemented for " + type.getName() + "." + name + " value=" + describeValue(value));
    }

    private static CN1AccessException unsupportedFieldWrite(Object target, String name, Object value) {
        return new CN1AccessException("Generated field write not implemented for " + target.getClass().getName() + "." + name + " value=" + describeValue(value));
    }

    private static String describeArgs(Object[] args) {
        if (args == null || args.length == 0) {
            return "()";
        }
        StringBuilder sb = new StringBuilder("(");
        for (int i = 0; i < args.length; i++) {
            if (i > 0) {
                sb.append(", ");
            }
            sb.append(describeValue(args[i]));
        }
        sb.append(')');
        return sb.toString();
    }

    private static String describeValue(Object value) {
        return value == null ? "null" : value.getClass().getName();
    }
}
