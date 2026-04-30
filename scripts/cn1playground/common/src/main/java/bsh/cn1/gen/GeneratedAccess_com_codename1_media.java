package bsh.cn1.gen;

import bsh.cn1.CN1AccessException;

public final class GeneratedAccess_com_codename1_media {
    private GeneratedAccess_com_codename1_media() {
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
        if ("AbstractMedia".equals(simpleName)) {
            return com.codename1.media.AbstractMedia.class;
        }
        if ("AsyncMedia".equals(simpleName)) {
            return com.codename1.media.AsyncMedia.class;
        }
        if ("MediaErrorType".equals(simpleName)) {
            return com.codename1.media.AsyncMedia.MediaErrorType.class;
        }
        if ("State".equals(simpleName)) {
            return com.codename1.media.AsyncMedia.State.class;
        }
        if ("AudioBuffer".equals(simpleName)) {
            return com.codename1.media.AudioBuffer.class;
        }
        if ("AudioBufferCallback".equals(simpleName)) {
            return com.codename1.media.AudioBuffer.AudioBufferCallback.class;
        }
        if ("Media".equals(simpleName)) {
            return com.codename1.media.Media.class;
        }
        if ("MediaManager".equals(simpleName)) {
            return com.codename1.media.MediaManager.class;
        }
        if ("MediaMetaData".equals(simpleName)) {
            return com.codename1.media.MediaMetaData.class;
        }
        if ("MediaRecorderBuilder".equals(simpleName)) {
            return com.codename1.media.MediaRecorderBuilder.class;
        }
        if ("RemoteControlListener".equals(simpleName)) {
            return com.codename1.media.RemoteControlListener.class;
        }
        if ("WAVWriter".equals(simpleName)) {
            return com.codename1.media.WAVWriter.class;
        }
        return null;
    }
    public static Object construct(Class<?> type, Object[] args) throws Exception {
        Object[] safeArgs = safeArgs(args);
        if (type == com.codename1.media.AudioBuffer.class) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class}, false);
                return new com.codename1.media.AudioBuffer(toIntValue(adaptedArgs[0]));
            }
        }
        if (type == com.codename1.media.WAVWriter.class) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.io.File.class, java.lang.Integer.class, java.lang.Integer.class, java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.io.File.class, java.lang.Integer.class, java.lang.Integer.class, java.lang.Integer.class}, false);
                return new com.codename1.media.WAVWriter((com.codename1.io.File) adaptedArgs[0], toIntValue(adaptedArgs[1]), toIntValue(adaptedArgs[2]), toIntValue(adaptedArgs[3]));
            }
        }
        throw unsupportedConstruct(type, safeArgs);
    }

    public static Object invokeStatic(Class<?> type, String name, Object[] args) throws Exception {
        Object[] safeArgs = safeArgs(args);
        if (type == com.codename1.media.MediaManager.class) return invokeStatic0(name, safeArgs);
        throw unsupportedStatic(type, name, safeArgs);
    }

    private static Object invokeStatic0(String name, Object[] safeArgs) throws Exception {
        if ("addCompletionHandler".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.media.Media.class, java.lang.Runnable.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.media.Media.class, java.lang.Runnable.class}, false);
                com.codename1.media.MediaManager.addCompletionHandler((com.codename1.media.Media) adaptedArgs[0], (java.lang.Runnable) adaptedArgs[1]); return null;
            }
        }
        if ("createBackgroundMedia".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class}, false);
                return com.codename1.media.MediaManager.createBackgroundMedia((java.lang.String) adaptedArgs[0]);
            }
        }
        if ("createBackgroundMediaAsync".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class}, false);
                return com.codename1.media.MediaManager.createBackgroundMediaAsync((java.lang.String) adaptedArgs[0]);
            }
        }
        if ("createMedia".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class, java.lang.Boolean.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class, java.lang.Boolean.class}, false);
                return com.codename1.media.MediaManager.createMedia((java.lang.String) adaptedArgs[0], ((Boolean) adaptedArgs[1]).booleanValue());
            }
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class, java.lang.Boolean.class, java.lang.Runnable.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class, java.lang.Boolean.class, java.lang.Runnable.class}, false);
                return com.codename1.media.MediaManager.createMedia((java.lang.String) adaptedArgs[0], ((Boolean) adaptedArgs[1]).booleanValue(), (java.lang.Runnable) adaptedArgs[2]);
            }
        }
        if ("createMediaAsync".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class, java.lang.Boolean.class, java.lang.Runnable.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class, java.lang.Boolean.class, java.lang.Runnable.class}, false);
                return com.codename1.media.MediaManager.createMediaAsync((java.lang.String) adaptedArgs[0], ((Boolean) adaptedArgs[1]).booleanValue(), (java.lang.Runnable) adaptedArgs[2]);
            }
        }
        if ("createMediaRecorder".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.media.MediaRecorderBuilder.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.media.MediaRecorderBuilder.class}, false);
                return com.codename1.media.MediaManager.createMediaRecorder((com.codename1.media.MediaRecorderBuilder) adaptedArgs[0]);
            }
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class}, false);
                return com.codename1.media.MediaManager.createMediaRecorder((java.lang.String) adaptedArgs[0]);
            }
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class, java.lang.String.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class, java.lang.String.class}, false);
                return com.codename1.media.MediaManager.createMediaRecorder((java.lang.String) adaptedArgs[0], (java.lang.String) adaptedArgs[1]);
            }
        }
        if ("deleteAudioBuffer".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class}, false);
                com.codename1.media.MediaManager.deleteAudioBuffer((java.lang.String) adaptedArgs[0]); return null;
            }
        }
        if ("getAsyncMedia".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.media.Media.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.media.Media.class}, false);
                return com.codename1.media.MediaManager.getAsyncMedia((com.codename1.media.Media) adaptedArgs[0]);
            }
        }
        if ("getAudioBuffer".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class}, false);
                return com.codename1.media.MediaManager.getAudioBuffer((java.lang.String) adaptedArgs[0]);
            }
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class, java.lang.Boolean.class, java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class, java.lang.Boolean.class, java.lang.Integer.class}, false);
                return com.codename1.media.MediaManager.getAudioBuffer((java.lang.String) adaptedArgs[0], ((Boolean) adaptedArgs[1]).booleanValue(), toIntValue(adaptedArgs[2]));
            }
        }
        if ("getAvailableRecordingMimeTypes".equals(name)) {
            if (safeArgs.length == 0) {
                return com.codename1.media.MediaManager.getAvailableRecordingMimeTypes();
            }
        }
        if ("getMediaRecorderingMimeType".equals(name)) {
            if (safeArgs.length == 0) {
                return com.codename1.media.MediaManager.getMediaRecorderingMimeType();
            }
        }
        if ("getRemoteControlListener".equals(name)) {
            if (safeArgs.length == 0) {
                return com.codename1.media.MediaManager.getRemoteControlListener();
            }
        }
        if ("releaseAudioBuffer".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class}, false);
                com.codename1.media.MediaManager.releaseAudioBuffer((java.lang.String) adaptedArgs[0]); return null;
            }
        }
        if ("removeCompletionHandler".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.media.Media.class, java.lang.Runnable.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.media.Media.class, java.lang.Runnable.class}, false);
                com.codename1.media.MediaManager.removeCompletionHandler((com.codename1.media.Media) adaptedArgs[0], (java.lang.Runnable) adaptedArgs[1]); return null;
            }
        }
        if ("setRemoteControlListener".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.media.RemoteControlListener.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.media.RemoteControlListener.class}, false);
                com.codename1.media.MediaManager.setRemoteControlListener((com.codename1.media.RemoteControlListener) adaptedArgs[0]); return null;
            }
        }
        throw unsupportedStatic(com.codename1.media.MediaManager.class, name, safeArgs);
    }

    public static Object invoke(Object target, String name, Object[] args) throws Exception {
        Object[] safeArgs = safeArgs(args);
        CN1AccessException unsupported = null;
        if (target instanceof com.codename1.media.AbstractMedia) {
            try {
                return invoke0((com.codename1.media.AbstractMedia) target, name, safeArgs);
            } catch (CN1AccessException ex) {
                unsupported = ex;
            }
        }
        if (target instanceof com.codename1.media.AudioBuffer) {
            try {
                return invoke1((com.codename1.media.AudioBuffer) target, name, safeArgs);
            } catch (CN1AccessException ex) {
                unsupported = ex;
            }
        }
        if (target instanceof com.codename1.media.MediaMetaData) {
            try {
                return invoke2((com.codename1.media.MediaMetaData) target, name, safeArgs);
            } catch (CN1AccessException ex) {
                unsupported = ex;
            }
        }
        if (target instanceof com.codename1.media.MediaRecorderBuilder) {
            try {
                return invoke3((com.codename1.media.MediaRecorderBuilder) target, name, safeArgs);
            } catch (CN1AccessException ex) {
                unsupported = ex;
            }
        }
        if (target instanceof com.codename1.media.RemoteControlListener) {
            try {
                return invoke4((com.codename1.media.RemoteControlListener) target, name, safeArgs);
            } catch (CN1AccessException ex) {
                unsupported = ex;
            }
        }
        if (target instanceof com.codename1.media.WAVWriter) {
            try {
                return invoke5((com.codename1.media.WAVWriter) target, name, safeArgs);
            } catch (CN1AccessException ex) {
                unsupported = ex;
            }
        }
        if (target instanceof com.codename1.media.AsyncMedia) {
            try {
                return invoke6((com.codename1.media.AsyncMedia) target, name, safeArgs);
            } catch (CN1AccessException ex) {
                unsupported = ex;
            }
        }
        if (target instanceof com.codename1.media.AudioBuffer.AudioBufferCallback) {
            try {
                return invoke7((com.codename1.media.AudioBuffer.AudioBufferCallback) target, name, safeArgs);
            } catch (CN1AccessException ex) {
                unsupported = ex;
            }
        }
        if (target instanceof com.codename1.media.Media) {
            try {
                return invoke8((com.codename1.media.Media) target, name, safeArgs);
            } catch (CN1AccessException ex) {
                unsupported = ex;
            }
        }
        if (unsupported != null) {
            throw unsupported;
        }
        throw unsupportedInstance(target, name, safeArgs);
    }

    private static Object invoke0(com.codename1.media.AbstractMedia typedTarget, String name, Object[] safeArgs) throws Exception {
        if ("addMediaCompletionHandler".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Runnable.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Runnable.class}, false);
                typedTarget.addMediaCompletionHandler((java.lang.Runnable) adaptedArgs[0]); return null;
            }
        }
        if ("addMediaErrorListener".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.events.ActionListener.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.ui.events.ActionListener.class}, false);
                typedTarget.addMediaErrorListener((com.codename1.ui.events.ActionListener) adaptedArgs[0]); return null;
            }
        }
        if ("addMediaStateChangeListener".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.events.ActionListener.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.ui.events.ActionListener.class}, false);
                typedTarget.addMediaStateChangeListener((com.codename1.ui.events.ActionListener) adaptedArgs[0]); return null;
            }
        }
        if ("cleanup".equals(name)) {
            if (safeArgs.length == 0) {
                typedTarget.cleanup(); return null;
            }
        }
        if ("getDuration".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getDuration();
            }
        }
        if ("getState".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getState();
            }
        }
        if ("getTime".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getTime();
            }
        }
        if ("getVariable".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class}, false);
                return typedTarget.getVariable((java.lang.String) adaptedArgs[0]);
            }
        }
        if ("getVideoComponent".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getVideoComponent();
            }
        }
        if ("getVolume".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getVolume();
            }
        }
        if ("isFullScreen".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.isFullScreen();
            }
        }
        if ("isNativePlayerMode".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.isNativePlayerMode();
            }
        }
        if ("isPlaying".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.isPlaying();
            }
        }
        if ("isVideo".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.isVideo();
            }
        }
        if ("pause".equals(name)) {
            if (safeArgs.length == 0) {
                typedTarget.pause(); return null;
            }
        }
        if ("pauseAsync".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.pauseAsync();
            }
        }
        if ("play".equals(name)) {
            if (safeArgs.length == 0) {
                typedTarget.play(); return null;
            }
        }
        if ("playAsync".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.playAsync();
            }
        }
        if ("prepare".equals(name)) {
            if (safeArgs.length == 0) {
                typedTarget.prepare(); return null;
            }
        }
        if ("removeMediaErrorListener".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.events.ActionListener.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.ui.events.ActionListener.class}, false);
                typedTarget.removeMediaErrorListener((com.codename1.ui.events.ActionListener) adaptedArgs[0]); return null;
            }
        }
        if ("removeMediaStateChangeListener".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.events.ActionListener.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.ui.events.ActionListener.class}, false);
                typedTarget.removeMediaStateChangeListener((com.codename1.ui.events.ActionListener) adaptedArgs[0]); return null;
            }
        }
        if ("setFullScreen".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false);
                typedTarget.setFullScreen(((Boolean) adaptedArgs[0]).booleanValue()); return null;
            }
        }
        if ("setNativePlayerMode".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false);
                typedTarget.setNativePlayerMode(((Boolean) adaptedArgs[0]).booleanValue()); return null;
            }
        }
        if ("setTime".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class}, false);
                typedTarget.setTime(toIntValue(adaptedArgs[0])); return null;
            }
        }
        if ("setVariable".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class, java.lang.Object.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class, java.lang.Object.class}, false);
                typedTarget.setVariable((java.lang.String) adaptedArgs[0], (java.lang.Object) adaptedArgs[1]); return null;
            }
        }
        if ("setVolume".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class}, false);
                typedTarget.setVolume(toIntValue(adaptedArgs[0])); return null;
            }
        }
        throw unsupportedInstance(typedTarget, name, safeArgs);
    }

    private static Object invoke1(com.codename1.media.AudioBuffer typedTarget, String name, Object[] safeArgs) throws Exception {
        if ("addCallback".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.media.AudioBuffer.AudioBufferCallback.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.media.AudioBuffer.AudioBufferCallback.class}, false);
                typedTarget.addCallback((com.codename1.media.AudioBuffer.AudioBufferCallback) adaptedArgs[0]); return null;
            }
        }
        if ("copyFrom".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.media.AudioBuffer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.media.AudioBuffer.class}, false);
                typedTarget.copyFrom((com.codename1.media.AudioBuffer) adaptedArgs[0]); return null;
            }
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class, java.lang.Integer.class, float[].class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class, java.lang.Integer.class, float[].class}, false);
                typedTarget.copyFrom(toIntValue(adaptedArgs[0]), toIntValue(adaptedArgs[1]), (float[]) adaptedArgs[2]); return null;
            }
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class, java.lang.Integer.class, float[].class, java.lang.Integer.class, java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class, java.lang.Integer.class, float[].class, java.lang.Integer.class, java.lang.Integer.class}, false);
                typedTarget.copyFrom(toIntValue(adaptedArgs[0]), toIntValue(adaptedArgs[1]), (float[]) adaptedArgs[2], toIntValue(adaptedArgs[3]), toIntValue(adaptedArgs[4])); return null;
            }
        }
        if ("copyTo".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.media.AudioBuffer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.media.AudioBuffer.class}, false);
                typedTarget.copyTo((com.codename1.media.AudioBuffer) adaptedArgs[0]); return null;
            }
            if (matches(safeArgs, new Class<?>[]{float[].class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{float[].class}, false);
                typedTarget.copyTo((float[]) adaptedArgs[0]); return null;
            }
            if (matches(safeArgs, new Class<?>[]{float[].class, java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{float[].class, java.lang.Integer.class}, false);
                typedTarget.copyTo((float[]) adaptedArgs[0], toIntValue(adaptedArgs[1])); return null;
            }
        }
        if ("downSample".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class}, false);
                typedTarget.downSample(toIntValue(adaptedArgs[0])); return null;
            }
        }
        if ("getMaxSize".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getMaxSize();
            }
        }
        if ("getNumChannels".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getNumChannels();
            }
        }
        if ("getSampleRate".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getSampleRate();
            }
        }
        if ("getSize".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getSize();
            }
        }
        if ("removeCallback".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.media.AudioBuffer.AudioBufferCallback.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.media.AudioBuffer.AudioBufferCallback.class}, false);
                typedTarget.removeCallback((com.codename1.media.AudioBuffer.AudioBufferCallback) adaptedArgs[0]); return null;
            }
        }
        throw unsupportedInstance(typedTarget, name, safeArgs);
    }

    private static Object invoke2(com.codename1.media.MediaMetaData typedTarget, String name, Object[] safeArgs) throws Exception {
        if ("getAlbumArt".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getAlbumArt();
            }
        }
        if ("getArt".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getArt();
            }
        }
        if ("getDisplayIcon".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getDisplayIcon();
            }
        }
        if ("getNumTracks".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getNumTracks();
            }
        }
        if ("getSubtitle".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getSubtitle();
            }
        }
        if ("getTitle".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getTitle();
            }
        }
        if ("getTrackNumber".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getTrackNumber();
            }
        }
        if ("setAlbumArt".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.Image.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.ui.Image.class}, false);
                typedTarget.setAlbumArt((com.codename1.ui.Image) adaptedArgs[0]); return null;
            }
        }
        if ("setArt".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.Image.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.ui.Image.class}, false);
                typedTarget.setArt((com.codename1.ui.Image) adaptedArgs[0]); return null;
            }
        }
        if ("setDisplayIcon".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.Image.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.ui.Image.class}, false);
                typedTarget.setDisplayIcon((com.codename1.ui.Image) adaptedArgs[0]); return null;
            }
        }
        if ("setNumTracks".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class}, false);
                typedTarget.setNumTracks(toIntValue(adaptedArgs[0])); return null;
            }
        }
        if ("setSubtitle".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class}, false);
                typedTarget.setSubtitle((java.lang.String) adaptedArgs[0]); return null;
            }
        }
        if ("setTitle".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class}, false);
                typedTarget.setTitle((java.lang.String) adaptedArgs[0]); return null;
            }
        }
        if ("setTrackNumber".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class}, false);
                typedTarget.setTrackNumber(toIntValue(adaptedArgs[0])); return null;
            }
        }
        throw unsupportedInstance(typedTarget, name, safeArgs);
    }

    private static Object invoke3(com.codename1.media.MediaRecorderBuilder typedTarget, String name, Object[] safeArgs) throws Exception {
        if ("audioChannels".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class}, false);
                return typedTarget.audioChannels(toIntValue(adaptedArgs[0]));
            }
        }
        if ("bitRate".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class}, false);
                return typedTarget.bitRate(toIntValue(adaptedArgs[0]));
            }
        }
        if ("build".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.build();
            }
        }
        if ("getAudioChannels".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getAudioChannels();
            }
        }
        if ("getBitRate".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getBitRate();
            }
        }
        if ("getMimeType".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getMimeType();
            }
        }
        if ("getPath".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getPath();
            }
        }
        if ("getSamplingRate".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getSamplingRate();
            }
        }
        if ("isRedirectToAudioBuffer".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.isRedirectToAudioBuffer();
            }
        }
        if ("mimeType".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class}, false);
                return typedTarget.mimeType((java.lang.String) adaptedArgs[0]);
            }
        }
        if ("path".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class}, false);
                return typedTarget.path((java.lang.String) adaptedArgs[0]);
            }
        }
        if ("redirectToAudioBuffer".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false);
                return typedTarget.redirectToAudioBuffer(((Boolean) adaptedArgs[0]).booleanValue());
            }
        }
        if ("samplingRate".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class}, false);
                return typedTarget.samplingRate(toIntValue(adaptedArgs[0]));
            }
        }
        throw unsupportedInstance(typedTarget, name, safeArgs);
    }

    private static Object invoke4(com.codename1.media.RemoteControlListener typedTarget, String name, Object[] safeArgs) throws Exception {
        if ("fastForward".equals(name)) {
            if (safeArgs.length == 0) {
                typedTarget.fastForward(); return null;
            }
        }
        if ("getMetaData".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getMetaData();
            }
        }
        if ("isPlaying".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.isPlaying();
            }
        }
        if ("pause".equals(name)) {
            if (safeArgs.length == 0) {
                typedTarget.pause(); return null;
            }
        }
        if ("play".equals(name)) {
            if (safeArgs.length == 0) {
                typedTarget.play(); return null;
            }
        }
        if ("rewind".equals(name)) {
            if (safeArgs.length == 0) {
                typedTarget.rewind(); return null;
            }
        }
        if ("seekTo".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Long.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Long.class}, false);
                typedTarget.seekTo(((Number) adaptedArgs[0]).longValue()); return null;
            }
        }
        if ("setVolume".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Float.class, java.lang.Float.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Float.class, java.lang.Float.class}, false);
                typedTarget.setVolume(((Number) adaptedArgs[0]).floatValue(), ((Number) adaptedArgs[1]).floatValue()); return null;
            }
        }
        if ("skipToNext".equals(name)) {
            if (safeArgs.length == 0) {
                typedTarget.skipToNext(); return null;
            }
        }
        if ("skipToPrevious".equals(name)) {
            if (safeArgs.length == 0) {
                typedTarget.skipToPrevious(); return null;
            }
        }
        if ("stop".equals(name)) {
            if (safeArgs.length == 0) {
                typedTarget.stop(); return null;
            }
        }
        if ("togglePlayPause".equals(name)) {
            if (safeArgs.length == 0) {
                typedTarget.togglePlayPause(); return null;
            }
        }
        throw unsupportedInstance(typedTarget, name, safeArgs);
    }

    private static Object invoke5(com.codename1.media.WAVWriter typedTarget, String name, Object[] safeArgs) throws Exception {
        if ("close".equals(name)) {
            if (safeArgs.length == 0) {
                typedTarget.close(); return null;
            }
        }
        if ("write".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{float[].class, java.lang.Integer.class, java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{float[].class, java.lang.Integer.class, java.lang.Integer.class}, false);
                typedTarget.write((float[]) adaptedArgs[0], toIntValue(adaptedArgs[1]), toIntValue(adaptedArgs[2])); return null;
            }
        }
        throw unsupportedInstance(typedTarget, name, safeArgs);
    }

    private static Object invoke6(com.codename1.media.AsyncMedia typedTarget, String name, Object[] safeArgs) throws Exception {
        if ("addMediaCompletionHandler".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Runnable.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Runnable.class}, false);
                typedTarget.addMediaCompletionHandler((java.lang.Runnable) adaptedArgs[0]); return null;
            }
        }
        if ("addMediaErrorListener".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.events.ActionListener.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.ui.events.ActionListener.class}, false);
                typedTarget.addMediaErrorListener((com.codename1.ui.events.ActionListener) adaptedArgs[0]); return null;
            }
        }
        if ("addMediaStateChangeListener".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.events.ActionListener.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.ui.events.ActionListener.class}, false);
                typedTarget.addMediaStateChangeListener((com.codename1.ui.events.ActionListener) adaptedArgs[0]); return null;
            }
        }
        if ("cleanup".equals(name)) {
            if (safeArgs.length == 0) {
                typedTarget.cleanup(); return null;
            }
        }
        if ("getDuration".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getDuration();
            }
        }
        if ("getState".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getState();
            }
        }
        if ("getTime".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getTime();
            }
        }
        if ("getVariable".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class}, false);
                return typedTarget.getVariable((java.lang.String) adaptedArgs[0]);
            }
        }
        if ("getVideoComponent".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getVideoComponent();
            }
        }
        if ("getVolume".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getVolume();
            }
        }
        if ("isFullScreen".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.isFullScreen();
            }
        }
        if ("isNativePlayerMode".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.isNativePlayerMode();
            }
        }
        if ("isPlaying".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.isPlaying();
            }
        }
        if ("isVideo".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.isVideo();
            }
        }
        if ("pause".equals(name)) {
            if (safeArgs.length == 0) {
                typedTarget.pause(); return null;
            }
        }
        if ("pauseAsync".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.pauseAsync();
            }
        }
        if ("play".equals(name)) {
            if (safeArgs.length == 0) {
                typedTarget.play(); return null;
            }
        }
        if ("playAsync".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.playAsync();
            }
        }
        if ("prepare".equals(name)) {
            if (safeArgs.length == 0) {
                typedTarget.prepare(); return null;
            }
        }
        if ("removeMediaErrorListener".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.events.ActionListener.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.ui.events.ActionListener.class}, false);
                typedTarget.removeMediaErrorListener((com.codename1.ui.events.ActionListener) adaptedArgs[0]); return null;
            }
        }
        if ("removeMediaStateChangeListener".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.events.ActionListener.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.ui.events.ActionListener.class}, false);
                typedTarget.removeMediaStateChangeListener((com.codename1.ui.events.ActionListener) adaptedArgs[0]); return null;
            }
        }
        if ("setFullScreen".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false);
                typedTarget.setFullScreen(((Boolean) adaptedArgs[0]).booleanValue()); return null;
            }
        }
        if ("setNativePlayerMode".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false);
                typedTarget.setNativePlayerMode(((Boolean) adaptedArgs[0]).booleanValue()); return null;
            }
        }
        if ("setTime".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class}, false);
                typedTarget.setTime(toIntValue(adaptedArgs[0])); return null;
            }
        }
        if ("setVariable".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class, java.lang.Object.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class, java.lang.Object.class}, false);
                typedTarget.setVariable((java.lang.String) adaptedArgs[0], (java.lang.Object) adaptedArgs[1]); return null;
            }
        }
        if ("setVolume".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class}, false);
                typedTarget.setVolume(toIntValue(adaptedArgs[0])); return null;
            }
        }
        throw unsupportedInstance(typedTarget, name, safeArgs);
    }

    private static Object invoke7(com.codename1.media.AudioBuffer.AudioBufferCallback typedTarget, String name, Object[] safeArgs) throws Exception {
        if ("frameReceived".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.media.AudioBuffer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.media.AudioBuffer.class}, false);
                typedTarget.frameReceived((com.codename1.media.AudioBuffer) adaptedArgs[0]); return null;
            }
        }
        throw unsupportedInstance(typedTarget, name, safeArgs);
    }

    private static Object invoke8(com.codename1.media.Media typedTarget, String name, Object[] safeArgs) throws Exception {
        if ("cleanup".equals(name)) {
            if (safeArgs.length == 0) {
                typedTarget.cleanup(); return null;
            }
        }
        if ("getDuration".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getDuration();
            }
        }
        if ("getTime".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getTime();
            }
        }
        if ("getVariable".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class}, false);
                return typedTarget.getVariable((java.lang.String) adaptedArgs[0]);
            }
        }
        if ("getVideoComponent".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getVideoComponent();
            }
        }
        if ("getVolume".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getVolume();
            }
        }
        if ("isFullScreen".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.isFullScreen();
            }
        }
        if ("isNativePlayerMode".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.isNativePlayerMode();
            }
        }
        if ("isPlaying".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.isPlaying();
            }
        }
        if ("isVideo".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.isVideo();
            }
        }
        if ("pause".equals(name)) {
            if (safeArgs.length == 0) {
                typedTarget.pause(); return null;
            }
        }
        if ("play".equals(name)) {
            if (safeArgs.length == 0) {
                typedTarget.play(); return null;
            }
        }
        if ("prepare".equals(name)) {
            if (safeArgs.length == 0) {
                typedTarget.prepare(); return null;
            }
        }
        if ("setFullScreen".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false);
                typedTarget.setFullScreen(((Boolean) adaptedArgs[0]).booleanValue()); return null;
            }
        }
        if ("setNativePlayerMode".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false);
                typedTarget.setNativePlayerMode(((Boolean) adaptedArgs[0]).booleanValue()); return null;
            }
        }
        if ("setTime".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class}, false);
                typedTarget.setTime(toIntValue(adaptedArgs[0])); return null;
            }
        }
        if ("setVariable".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class, java.lang.Object.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class, java.lang.Object.class}, false);
                typedTarget.setVariable((java.lang.String) adaptedArgs[0], (java.lang.Object) adaptedArgs[1]); return null;
            }
        }
        if ("setVolume".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class}, false);
                typedTarget.setVolume(toIntValue(adaptedArgs[0])); return null;
            }
        }
        throw unsupportedInstance(typedTarget, name, safeArgs);
    }

    public static Object getStaticField(Class<?> type, String name) throws Exception {
        if (type == com.codename1.media.AbstractMedia.class) return getStaticField0(name);
        if (type == com.codename1.media.AsyncMedia.class) return getStaticField1(name);
        if (type == com.codename1.media.AsyncMedia.MediaErrorType.class) return getStaticField2(name);
        if (type == com.codename1.media.AsyncMedia.State.class) return getStaticField3(name);
        if (type == com.codename1.media.Media.class) return getStaticField4(name);
        throw unsupportedStaticField(type, name);
    }

    private static Object getStaticField0(String name) throws Exception {
        if ("VARIABLE_ANDROID_SEEK_PREVIEW_WORKAROUND".equals(name)) return com.codename1.media.AbstractMedia.VARIABLE_ANDROID_SEEK_PREVIEW_WORKAROUND;
        if ("VARIABLE_BACKGROUND_ALBUM_COVER".equals(name)) return com.codename1.media.AbstractMedia.VARIABLE_BACKGROUND_ALBUM_COVER;
        if ("VARIABLE_BACKGROUND_ARTIST".equals(name)) return com.codename1.media.AbstractMedia.VARIABLE_BACKGROUND_ARTIST;
        if ("VARIABLE_BACKGROUND_DURATION".equals(name)) return com.codename1.media.AbstractMedia.VARIABLE_BACKGROUND_DURATION;
        if ("VARIABLE_BACKGROUND_POSITION".equals(name)) return com.codename1.media.AbstractMedia.VARIABLE_BACKGROUND_POSITION;
        if ("VARIABLE_BACKGROUND_SUPPORTED".equals(name)) return com.codename1.media.AbstractMedia.VARIABLE_BACKGROUND_SUPPORTED;
        if ("VARIABLE_BACKGROUND_TITLE".equals(name)) return com.codename1.media.AbstractMedia.VARIABLE_BACKGROUND_TITLE;
        if ("VARIABLE_NATIVE_CONTRLOLS_EMBEDDED".equals(name)) return com.codename1.media.AbstractMedia.VARIABLE_NATIVE_CONTRLOLS_EMBEDDED;
        throw unsupportedStaticField(com.codename1.media.AbstractMedia.class, name);
    }

    private static Object getStaticField1(String name) throws Exception {
        if ("VARIABLE_ANDROID_SEEK_PREVIEW_WORKAROUND".equals(name)) return com.codename1.media.AsyncMedia.VARIABLE_ANDROID_SEEK_PREVIEW_WORKAROUND;
        if ("VARIABLE_BACKGROUND_ALBUM_COVER".equals(name)) return com.codename1.media.AsyncMedia.VARIABLE_BACKGROUND_ALBUM_COVER;
        if ("VARIABLE_BACKGROUND_ARTIST".equals(name)) return com.codename1.media.AsyncMedia.VARIABLE_BACKGROUND_ARTIST;
        if ("VARIABLE_BACKGROUND_DURATION".equals(name)) return com.codename1.media.AsyncMedia.VARIABLE_BACKGROUND_DURATION;
        if ("VARIABLE_BACKGROUND_POSITION".equals(name)) return com.codename1.media.AsyncMedia.VARIABLE_BACKGROUND_POSITION;
        if ("VARIABLE_BACKGROUND_SUPPORTED".equals(name)) return com.codename1.media.AsyncMedia.VARIABLE_BACKGROUND_SUPPORTED;
        if ("VARIABLE_BACKGROUND_TITLE".equals(name)) return com.codename1.media.AsyncMedia.VARIABLE_BACKGROUND_TITLE;
        if ("VARIABLE_NATIVE_CONTRLOLS_EMBEDDED".equals(name)) return com.codename1.media.AsyncMedia.VARIABLE_NATIVE_CONTRLOLS_EMBEDDED;
        throw unsupportedStaticField(com.codename1.media.AsyncMedia.class, name);
    }

    private static Object getStaticField2(String name) throws Exception {
        if ("Aborted".equals(name)) return com.codename1.media.AsyncMedia.MediaErrorType.Aborted;
        if ("Decode".equals(name)) return com.codename1.media.AsyncMedia.MediaErrorType.Decode;
        if ("Encode".equals(name)) return com.codename1.media.AsyncMedia.MediaErrorType.Encode;
        if ("LineUnavailable".equals(name)) return com.codename1.media.AsyncMedia.MediaErrorType.LineUnavailable;
        if ("Network".equals(name)) return com.codename1.media.AsyncMedia.MediaErrorType.Network;
        if ("SrcNotSupported".equals(name)) return com.codename1.media.AsyncMedia.MediaErrorType.SrcNotSupported;
        if ("Unknown".equals(name)) return com.codename1.media.AsyncMedia.MediaErrorType.Unknown;
        throw unsupportedStaticField(com.codename1.media.AsyncMedia.MediaErrorType.class, name);
    }

    private static Object getStaticField3(String name) throws Exception {
        if ("Paused".equals(name)) return com.codename1.media.AsyncMedia.State.Paused;
        if ("Playing".equals(name)) return com.codename1.media.AsyncMedia.State.Playing;
        throw unsupportedStaticField(com.codename1.media.AsyncMedia.State.class, name);
    }

    private static Object getStaticField4(String name) throws Exception {
        if ("VARIABLE_ANDROID_SEEK_PREVIEW_WORKAROUND".equals(name)) return com.codename1.media.Media.VARIABLE_ANDROID_SEEK_PREVIEW_WORKAROUND;
        if ("VARIABLE_BACKGROUND_ALBUM_COVER".equals(name)) return com.codename1.media.Media.VARIABLE_BACKGROUND_ALBUM_COVER;
        if ("VARIABLE_BACKGROUND_ARTIST".equals(name)) return com.codename1.media.Media.VARIABLE_BACKGROUND_ARTIST;
        if ("VARIABLE_BACKGROUND_DURATION".equals(name)) return com.codename1.media.Media.VARIABLE_BACKGROUND_DURATION;
        if ("VARIABLE_BACKGROUND_POSITION".equals(name)) return com.codename1.media.Media.VARIABLE_BACKGROUND_POSITION;
        if ("VARIABLE_BACKGROUND_SUPPORTED".equals(name)) return com.codename1.media.Media.VARIABLE_BACKGROUND_SUPPORTED;
        if ("VARIABLE_BACKGROUND_TITLE".equals(name)) return com.codename1.media.Media.VARIABLE_BACKGROUND_TITLE;
        if ("VARIABLE_NATIVE_CONTRLOLS_EMBEDDED".equals(name)) return com.codename1.media.Media.VARIABLE_NATIVE_CONTRLOLS_EMBEDDED;
        throw unsupportedStaticField(com.codename1.media.Media.class, name);
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
