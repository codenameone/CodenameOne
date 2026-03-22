package bsh.cn1.gen;

import bsh.cn1.CN1AccessException;

public final class GeneratedAccess_com_codename1_push {
    private GeneratedAccess_com_codename1_push() {
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
        if ("Push".equals(simpleName)) {
            return com.codename1.push.Push.class;
        }
        if ("PushAction".equals(simpleName)) {
            return com.codename1.push.PushAction.class;
        }
        if ("PushActionCategory".equals(simpleName)) {
            return com.codename1.push.PushActionCategory.class;
        }
        if ("PushActionsProvider".equals(simpleName)) {
            return com.codename1.push.PushActionsProvider.class;
        }
        if ("PushBuilder".equals(simpleName)) {
            return com.codename1.push.PushBuilder.class;
        }
        if ("PushCallback".equals(simpleName)) {
            return com.codename1.push.PushCallback.class;
        }
        if ("PushContent".equals(simpleName)) {
            return com.codename1.push.PushContent.class;
        }
        return null;
    }
    public static Object construct(Class<?> type, Object[] args) throws Exception {
        Object[] safeArgs = safeArgs(args);
        if (type == com.codename1.push.Push.class) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class, java.lang.String.class, java.lang.String[].class}, true)) {
                java.lang.String[] varArgs = new java.lang.String[safeArgs.length - 2];
                for (int i = 2; i < safeArgs.length; i++) {
                    varArgs[i - 2] = (java.lang.String) safeArgs[i];
                }
                return new com.codename1.push.Push((java.lang.String) safeArgs[0], (java.lang.String) safeArgs[1], varArgs);
            }
        }
        if (type == com.codename1.push.PushAction.class) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class}, false)) {
                return new com.codename1.push.PushAction((java.lang.String) safeArgs[0]);
            }
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class, java.lang.String.class}, false)) {
                return new com.codename1.push.PushAction((java.lang.String) safeArgs[0], (java.lang.String) safeArgs[1]);
            }
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class, java.lang.String.class, java.lang.String.class}, false)) {
                return new com.codename1.push.PushAction((java.lang.String) safeArgs[0], (java.lang.String) safeArgs[1], (java.lang.String) safeArgs[2]);
            }
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class, java.lang.String.class, java.lang.String.class, java.lang.String.class, java.lang.String.class}, false)) {
                return new com.codename1.push.PushAction((java.lang.String) safeArgs[0], (java.lang.String) safeArgs[1], (java.lang.String) safeArgs[2], (java.lang.String) safeArgs[3], (java.lang.String) safeArgs[4]);
            }
        }
        if (type == com.codename1.push.PushActionCategory.class) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class, com.codename1.push.PushAction[].class}, true)) {
                com.codename1.push.PushAction[] varArgs = new com.codename1.push.PushAction[safeArgs.length - 1];
                for (int i = 1; i < safeArgs.length; i++) {
                    varArgs[i - 1] = (com.codename1.push.PushAction) safeArgs[i];
                }
                return new com.codename1.push.PushActionCategory((java.lang.String) safeArgs[0], varArgs);
            }
        }
        throw unsupportedConstruct(type, safeArgs);
    }

    public static Object invokeStatic(Class<?> type, String name, Object[] args) throws Exception {
        Object[] safeArgs = safeArgs(args);
        if (type == com.codename1.push.Push.class) return invokeStatic0(name, safeArgs);
        if (type == com.codename1.push.PushActionCategory.class) return invokeStatic1(name, safeArgs);
        if (type == com.codename1.push.PushContent.class) return invokeStatic2(name, safeArgs);
        throw unsupportedStatic(type, name, safeArgs);
    }

    private static Object invokeStatic0(String name, Object[] safeArgs) throws Exception {
        if ("getDeviceKey".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return com.codename1.push.Push.getDeviceKey();
            }
        }
        if ("getPushKey".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return com.codename1.push.Push.getPushKey();
            }
        }
        if ("sendPushMessage".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class, java.lang.String.class, java.lang.Boolean.class, java.lang.String.class, java.lang.String.class, java.lang.String.class}, false)) {
                return com.codename1.push.Push.sendPushMessage((java.lang.String) safeArgs[0], (java.lang.String) safeArgs[1], ((Boolean) safeArgs[2]).booleanValue(), (java.lang.String) safeArgs[3], (java.lang.String) safeArgs[4], (java.lang.String) safeArgs[5]);
            }
        }
        if ("sendPushMessage".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class, java.lang.String.class, java.lang.String.class, java.lang.Boolean.class, java.lang.String.class, java.lang.String.class, java.lang.String.class}, false)) {
                return com.codename1.push.Push.sendPushMessage((java.lang.String) safeArgs[0], (java.lang.String) safeArgs[1], (java.lang.String) safeArgs[2], ((Boolean) safeArgs[3]).booleanValue(), (java.lang.String) safeArgs[4], (java.lang.String) safeArgs[5], (java.lang.String) safeArgs[6]);
            }
        }
        if ("sendPushMessage".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class, java.lang.String.class, java.lang.Boolean.class, java.lang.String.class, java.lang.String.class, java.lang.String.class, java.lang.Integer.class, java.lang.String[].class}, true)) {
                java.lang.String[] varArgs = new java.lang.String[safeArgs.length - 7];
                for (int i = 7; i < safeArgs.length; i++) {
                    varArgs[i - 7] = (java.lang.String) safeArgs[i];
                }
                return com.codename1.push.Push.sendPushMessage((java.lang.String) safeArgs[0], (java.lang.String) safeArgs[1], ((Boolean) safeArgs[2]).booleanValue(), (java.lang.String) safeArgs[3], (java.lang.String) safeArgs[4], (java.lang.String) safeArgs[5], ((Number) safeArgs[6]).intValue(), varArgs);
            }
        }
        if ("sendPushMessage".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class, java.lang.String.class, java.lang.Boolean.class, java.lang.String.class, java.lang.String.class, java.lang.String.class, java.lang.String.class, java.lang.String.class, java.lang.String.class, java.lang.String.class}, false)) {
                return com.codename1.push.Push.sendPushMessage((java.lang.String) safeArgs[0], (java.lang.String) safeArgs[1], ((Boolean) safeArgs[2]).booleanValue(), (java.lang.String) safeArgs[3], (java.lang.String) safeArgs[4], (java.lang.String) safeArgs[5], (java.lang.String) safeArgs[6], (java.lang.String) safeArgs[7], (java.lang.String) safeArgs[8], (java.lang.String) safeArgs[9]);
            }
        }
        if ("sendPushMessage".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class, java.lang.String.class, java.lang.String.class, java.lang.Boolean.class, java.lang.String.class, java.lang.String.class, java.lang.String.class, java.lang.String.class, java.lang.String.class, java.lang.String.class, java.lang.String.class}, false)) {
                return com.codename1.push.Push.sendPushMessage((java.lang.String) safeArgs[0], (java.lang.String) safeArgs[1], (java.lang.String) safeArgs[2], ((Boolean) safeArgs[3]).booleanValue(), (java.lang.String) safeArgs[4], (java.lang.String) safeArgs[5], (java.lang.String) safeArgs[6], (java.lang.String) safeArgs[7], (java.lang.String) safeArgs[8], (java.lang.String) safeArgs[9], (java.lang.String) safeArgs[10]);
            }
        }
        if ("sendPushMessageAsync".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class, java.lang.String.class, java.lang.Boolean.class, java.lang.String.class, java.lang.String.class, java.lang.String.class}, false)) {
                com.codename1.push.Push.sendPushMessageAsync((java.lang.String) safeArgs[0], (java.lang.String) safeArgs[1], ((Boolean) safeArgs[2]).booleanValue(), (java.lang.String) safeArgs[3], (java.lang.String) safeArgs[4], (java.lang.String) safeArgs[5]); return null;
            }
        }
        if ("sendPushMessageAsync".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class, java.lang.String.class, java.lang.String.class, java.lang.Boolean.class, java.lang.String.class, java.lang.String.class, java.lang.String.class}, false)) {
                com.codename1.push.Push.sendPushMessageAsync((java.lang.String) safeArgs[0], (java.lang.String) safeArgs[1], (java.lang.String) safeArgs[2], ((Boolean) safeArgs[3]).booleanValue(), (java.lang.String) safeArgs[4], (java.lang.String) safeArgs[5], (java.lang.String) safeArgs[6]); return null;
            }
        }
        if ("sendPushMessageAsync".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class, java.lang.String.class, java.lang.Boolean.class, java.lang.String.class, java.lang.String.class, java.lang.String.class, java.lang.String.class, java.lang.String.class, java.lang.String.class, java.lang.String.class}, false)) {
                com.codename1.push.Push.sendPushMessageAsync((java.lang.String) safeArgs[0], (java.lang.String) safeArgs[1], ((Boolean) safeArgs[2]).booleanValue(), (java.lang.String) safeArgs[3], (java.lang.String) safeArgs[4], (java.lang.String) safeArgs[5], (java.lang.String) safeArgs[6], (java.lang.String) safeArgs[7], (java.lang.String) safeArgs[8], (java.lang.String) safeArgs[9]); return null;
            }
        }
        if ("sendPushMessageAsync".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class, java.lang.String.class, java.lang.String.class, java.lang.Boolean.class, java.lang.String.class, java.lang.String.class, java.lang.String.class, java.lang.String.class, java.lang.String.class, java.lang.String.class, java.lang.String.class}, false)) {
                com.codename1.push.Push.sendPushMessageAsync((java.lang.String) safeArgs[0], (java.lang.String) safeArgs[1], (java.lang.String) safeArgs[2], ((Boolean) safeArgs[3]).booleanValue(), (java.lang.String) safeArgs[4], (java.lang.String) safeArgs[5], (java.lang.String) safeArgs[6], (java.lang.String) safeArgs[7], (java.lang.String) safeArgs[8], (java.lang.String) safeArgs[9], (java.lang.String) safeArgs[10]); return null;
            }
        }
        throw unsupportedStatic(com.codename1.push.Push.class, name, safeArgs);
    }

    private static Object invokeStatic1(String name, Object[] safeArgs) throws Exception {
        if ("getAllActions".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.push.PushActionCategory[].class}, true)) {
                com.codename1.push.PushActionCategory[] varArgs = new com.codename1.push.PushActionCategory[safeArgs.length - 0];
                for (int i = 0; i < safeArgs.length; i++) {
                    varArgs[i - 0] = (com.codename1.push.PushActionCategory) safeArgs[i];
                }
                return com.codename1.push.PushActionCategory.getAllActions(varArgs);
            }
        }
        throw unsupportedStatic(com.codename1.push.PushActionCategory.class, name, safeArgs);
    }

    private static Object invokeStatic2(String name, Object[] safeArgs) throws Exception {
        if ("exists".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return com.codename1.push.PushContent.exists();
            }
        }
        if ("get".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return com.codename1.push.PushContent.get();
            }
        }
        if ("reset".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                com.codename1.push.PushContent.reset(); return null;
            }
        }
        if ("setActionId".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class}, false)) {
                com.codename1.push.PushContent.setActionId((java.lang.String) safeArgs[0]); return null;
            }
        }
        if ("setBody".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class}, false)) {
                com.codename1.push.PushContent.setBody((java.lang.String) safeArgs[0]); return null;
            }
        }
        if ("setCategory".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class}, false)) {
                com.codename1.push.PushContent.setCategory((java.lang.String) safeArgs[0]); return null;
            }
        }
        if ("setImageUrl".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class}, false)) {
                com.codename1.push.PushContent.setImageUrl((java.lang.String) safeArgs[0]); return null;
            }
        }
        if ("setMetaData".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class}, false)) {
                com.codename1.push.PushContent.setMetaData((java.lang.String) safeArgs[0]); return null;
            }
        }
        if ("setTextResponse".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class}, false)) {
                com.codename1.push.PushContent.setTextResponse((java.lang.String) safeArgs[0]); return null;
            }
        }
        if ("setTitle".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class}, false)) {
                com.codename1.push.PushContent.setTitle((java.lang.String) safeArgs[0]); return null;
            }
        }
        if ("setType".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class}, false)) {
                com.codename1.push.PushContent.setType(((Number) safeArgs[0]).intValue()); return null;
            }
        }
        throw unsupportedStatic(com.codename1.push.PushContent.class, name, safeArgs);
    }

    public static Object invoke(Object target, String name, Object[] args) throws Exception {
        Object[] safeArgs = safeArgs(args);
        CN1AccessException unsupported = null;
        if (target instanceof com.codename1.push.Push) {
            try {
                return invoke0((com.codename1.push.Push) target, name, safeArgs);
            } catch (CN1AccessException ex) {
                unsupported = ex;
            }
        }
        if (target instanceof com.codename1.push.PushAction) {
            try {
                return invoke1((com.codename1.push.PushAction) target, name, safeArgs);
            } catch (CN1AccessException ex) {
                unsupported = ex;
            }
        }
        if (target instanceof com.codename1.push.PushActionCategory) {
            try {
                return invoke2((com.codename1.push.PushActionCategory) target, name, safeArgs);
            } catch (CN1AccessException ex) {
                unsupported = ex;
            }
        }
        if (target instanceof com.codename1.push.PushBuilder) {
            try {
                return invoke3((com.codename1.push.PushBuilder) target, name, safeArgs);
            } catch (CN1AccessException ex) {
                unsupported = ex;
            }
        }
        if (target instanceof com.codename1.push.PushContent) {
            try {
                return invoke4((com.codename1.push.PushContent) target, name, safeArgs);
            } catch (CN1AccessException ex) {
                unsupported = ex;
            }
        }
        if (target instanceof com.codename1.push.PushActionsProvider) {
            try {
                return invoke5((com.codename1.push.PushActionsProvider) target, name, safeArgs);
            } catch (CN1AccessException ex) {
                unsupported = ex;
            }
        }
        if (target instanceof com.codename1.push.PushCallback) {
            try {
                return invoke6((com.codename1.push.PushCallback) target, name, safeArgs);
            } catch (CN1AccessException ex) {
                unsupported = ex;
            }
        }
        if (unsupported != null) {
            throw unsupported;
        }
        throw unsupportedInstance(target, name, safeArgs);
    }

    private static Object invoke0(com.codename1.push.Push typedTarget, String name, Object[] safeArgs) throws Exception {
        if ("apnsAuth".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class, java.lang.String.class, java.lang.Boolean.class}, false)) {
                return typedTarget.apnsAuth((java.lang.String) safeArgs[0], (java.lang.String) safeArgs[1], ((Boolean) safeArgs[2]).booleanValue());
            }
        }
        if ("gcmAuth".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class}, false)) {
                return typedTarget.gcmAuth((java.lang.String) safeArgs[0]);
            }
        }
        if ("pushType".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class}, false)) {
                return typedTarget.pushType(((Number) safeArgs[0]).intValue());
            }
        }
        if ("send".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.send();
            }
        }
        if ("sendAsync".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                typedTarget.sendAsync(); return null;
            }
        }
        if ("wnsAuth".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class, java.lang.String.class}, false)) {
                return typedTarget.wnsAuth((java.lang.String) safeArgs[0], (java.lang.String) safeArgs[1]);
            }
        }
        throw unsupportedInstance(typedTarget, name, safeArgs);
    }

    private static Object invoke1(com.codename1.push.PushAction typedTarget, String name, Object[] safeArgs) throws Exception {
        if ("getIcon".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.getIcon();
            }
        }
        if ("getId".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.getId();
            }
        }
        if ("getTextInputButtonText".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.getTextInputButtonText();
            }
        }
        if ("getTextInputPlaceholder".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.getTextInputPlaceholder();
            }
        }
        if ("getTitle".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.getTitle();
            }
        }
        throw unsupportedInstance(typedTarget, name, safeArgs);
    }

    private static Object invoke2(com.codename1.push.PushActionCategory typedTarget, String name, Object[] safeArgs) throws Exception {
        if ("getActions".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.getActions();
            }
        }
        if ("getId".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.getId();
            }
        }
        throw unsupportedInstance(typedTarget, name, safeArgs);
    }

    private static Object invoke3(com.codename1.push.PushBuilder typedTarget, String name, Object[] safeArgs) throws Exception {
        if ("badge".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class}, false)) {
                return typedTarget.badge(((Number) safeArgs[0]).intValue());
            }
        }
        if ("body".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class}, false)) {
                return typedTarget.body((java.lang.String) safeArgs[0]);
            }
        }
        if ("build".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.build();
            }
        }
        if ("category".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class}, false)) {
                return typedTarget.category((java.lang.String) safeArgs[0]);
            }
        }
        if ("getType".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.getType();
            }
        }
        if ("imageUrl".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class}, false)) {
                return typedTarget.imageUrl((java.lang.String) safeArgs[0]);
            }
        }
        if ("isRichPush".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.isRichPush();
            }
        }
        if ("metaData".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class}, false)) {
                return typedTarget.metaData((java.lang.String) safeArgs[0]);
            }
        }
        if ("title".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class}, false)) {
                return typedTarget.title((java.lang.String) safeArgs[0]);
            }
        }
        if ("type".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class}, false)) {
                return typedTarget.type(((Number) safeArgs[0]).intValue());
            }
        }
        throw unsupportedInstance(typedTarget, name, safeArgs);
    }

    private static Object invoke4(com.codename1.push.PushContent typedTarget, String name, Object[] safeArgs) throws Exception {
        if ("getActionId".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.getActionId();
            }
        }
        if ("getBody".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.getBody();
            }
        }
        if ("getCategory".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.getCategory();
            }
        }
        if ("getImageUrl".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.getImageUrl();
            }
        }
        if ("getMetaData".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.getMetaData();
            }
        }
        if ("getTextResponse".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.getTextResponse();
            }
        }
        if ("getTitle".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.getTitle();
            }
        }
        if ("getType".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.getType();
            }
        }
        throw unsupportedInstance(typedTarget, name, safeArgs);
    }

    private static Object invoke5(com.codename1.push.PushActionsProvider typedTarget, String name, Object[] safeArgs) throws Exception {
        if ("getPushActionCategories".equals(name)) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                return typedTarget.getPushActionCategories();
            }
        }
        throw unsupportedInstance(typedTarget, name, safeArgs);
    }

    private static Object invoke6(com.codename1.push.PushCallback typedTarget, String name, Object[] safeArgs) throws Exception {
        if ("push".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class}, false)) {
                typedTarget.push((java.lang.String) safeArgs[0]); return null;
            }
        }
        if ("pushRegistrationError".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class, java.lang.Integer.class}, false)) {
                typedTarget.pushRegistrationError((java.lang.String) safeArgs[0], ((Number) safeArgs[1]).intValue()); return null;
            }
        }
        if ("registeredForPush".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class}, false)) {
                typedTarget.registeredForPush((java.lang.String) safeArgs[0]); return null;
            }
        }
        throw unsupportedInstance(typedTarget, name, safeArgs);
    }

    public static Object getStaticField(Class<?> type, String name) throws Exception {
        if (type == com.codename1.push.Push.class) {
            if ("GOOGLE_PUSH_KEY".equals(name)) return com.codename1.push.Push.GOOGLE_PUSH_KEY;
        }
        if (type == com.codename1.push.PushCallback.class) {
            if ("REGISTRATION_ACCOUNT_MISSING".equals(name)) return com.codename1.push.PushCallback.REGISTRATION_ACCOUNT_MISSING;
            if ("REGISTRATION_AUTHENTICATION_FAILED".equals(name)) return com.codename1.push.PushCallback.REGISTRATION_AUTHENTICATION_FAILED;
            if ("REGISTRATION_ERROR_SERVICE_NOT_AVAILABLE".equals(name)) return com.codename1.push.PushCallback.REGISTRATION_ERROR_SERVICE_NOT_AVAILABLE;
            if ("REGISTRATION_INVALID_SENDER".equals(name)) return com.codename1.push.PushCallback.REGISTRATION_INVALID_SENDER;
            if ("REGISTRATION_PHONE_REGISTRATION_ERROR".equals(name)) return com.codename1.push.PushCallback.REGISTRATION_PHONE_REGISTRATION_ERROR;
            if ("REGISTRATION_TOO_MANY_REGISTRATIONS".equals(name)) return com.codename1.push.PushCallback.REGISTRATION_TOO_MANY_REGISTRATIONS;
        }
        throw unsupportedStaticField(type, name);
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
            return value instanceof Number;
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
