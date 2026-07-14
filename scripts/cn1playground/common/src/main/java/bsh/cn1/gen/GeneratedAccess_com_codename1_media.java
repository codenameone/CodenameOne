package bsh.cn1.gen;

import bsh.cn1.CN1AccessException;

public final class GeneratedAccess_com_codename1_media {
    private GeneratedAccess_com_codename1_media() {
    }

    public static Class<?> findClass(String name) {
        if (name == null) {
            return null;
        }
        int dot = name.lastIndexOf('.');
        int dollar = name.lastIndexOf('$');
        int sep = dot > dollar ? dot : dollar;
        if (sep < 0 || sep == name.length() - 1) {
            return null;
        }
        return findClassBySimpleName(name.substring(sep + 1));
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
        if ("AudioEffects".equals(simpleName)) {
            return com.codename1.media.AudioEffects.class;
        }
        if ("AudioMixer".equals(simpleName)) {
            return com.codename1.media.AudioMixer.class;
        }
        if ("CompletionAwareSoundPoolPeer".equals(simpleName)) {
            return com.codename1.media.CompletionAwareSoundPoolPeer.class;
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
        if ("RecognitionCallback".equals(simpleName)) {
            return com.codename1.media.RecognitionCallback.class;
        }
        if ("RecognitionOptions".equals(simpleName)) {
            return com.codename1.media.RecognitionOptions.class;
        }
        if ("RemoteControlListener".equals(simpleName)) {
            return com.codename1.media.RemoteControlListener.class;
        }
        if ("SoundPoolPeer".equals(simpleName)) {
            return com.codename1.media.SoundPoolPeer.class;
        }
        if ("SpeechRecognizer".equals(simpleName)) {
            return com.codename1.media.SpeechRecognizer.class;
        }
        if ("TextToSpeech".equals(simpleName)) {
            return com.codename1.media.TextToSpeech.class;
        }
        if ("TimedRecognitionCallback".equals(simpleName)) {
            return com.codename1.media.TimedRecognitionCallback.class;
        }
        if ("Transcriber".equals(simpleName)) {
            return com.codename1.media.Transcriber.class;
        }
        if ("TranscriptionRequest".equals(simpleName)) {
            return com.codename1.media.TranscriptionRequest.class;
        }
        if ("TranscriptionResult".equals(simpleName)) {
            return com.codename1.media.TranscriptionResult.class;
        }
        if ("TranscriptionSegment".equals(simpleName)) {
            return com.codename1.media.TranscriptionSegment.class;
        }
        if ("TtsOptions".equals(simpleName)) {
            return com.codename1.media.TtsOptions.class;
        }
        if ("VideoCodec".equals(simpleName)) {
            return com.codename1.media.VideoCodec.class;
        }
        if ("VideoFrame".equals(simpleName)) {
            return com.codename1.media.VideoFrame.class;
        }
        if ("VideoIO".equals(simpleName)) {
            return com.codename1.media.VideoIO.class;
        }
        if ("VideoReader".equals(simpleName)) {
            return com.codename1.media.VideoReader.class;
        }
        if ("FrameCallback".equals(simpleName)) {
            return com.codename1.media.VideoReader.FrameCallback.class;
        }
        if ("VideoWriter".equals(simpleName)) {
            return com.codename1.media.VideoWriter.class;
        }
        if ("VideoWriterBuilder".equals(simpleName)) {
            return com.codename1.media.VideoWriterBuilder.class;
        }
        if ("VoiceCompletionListener".equals(simpleName)) {
            return com.codename1.media.VoiceCompletionListener.class;
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
        if (type == com.codename1.media.AudioMixer.class) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class, java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class, java.lang.Integer.class}, false);
                return new com.codename1.media.AudioMixer(toIntValue(adaptedArgs[0]), toIntValue(adaptedArgs[1]));
            }
        }
        if (type == com.codename1.media.MediaMetaData.class) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[0], false);
                return new com.codename1.media.MediaMetaData();
            }
        }
        if (type == com.codename1.media.MediaRecorderBuilder.class) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[0], false);
                return new com.codename1.media.MediaRecorderBuilder();
            }
        }
        if (type == com.codename1.media.RecognitionOptions.class) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[0], false);
                return new com.codename1.media.RecognitionOptions();
            }
        }
        if (type == com.codename1.media.RemoteControlListener.class) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[0], false);
                return new com.codename1.media.RemoteControlListener();
            }
        }
        if (type == com.codename1.media.TranscriptionRequest.class) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class}, false);
                return new com.codename1.media.TranscriptionRequest((java.lang.String) adaptedArgs[0]);
            }
        }
        if (type == com.codename1.media.TranscriptionResult.class) {
            if (matches(safeArgs, new Class<?>[]{java.util.List.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.util.List.class}, false);
                return new com.codename1.media.TranscriptionResult((java.util.List) adaptedArgs[0]);
            }
        }
        if (type == com.codename1.media.TranscriptionSegment.class) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Long.class, java.lang.Long.class, java.lang.String.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Long.class, java.lang.Long.class, java.lang.String.class}, false);
                return new com.codename1.media.TranscriptionSegment(((Number) adaptedArgs[0]).longValue(), ((Number) adaptedArgs[1]).longValue(), (java.lang.String) adaptedArgs[2]);
            }
        }
        if (type == com.codename1.media.TtsOptions.class) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[0], false);
                return new com.codename1.media.TtsOptions();
            }
        }
        if (type == com.codename1.media.VideoCodec.class) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class, java.lang.String.class, java.lang.String.class, java.lang.Boolean.class, java.lang.Boolean.class, java.lang.Boolean.class, java.lang.Boolean.class, java.lang.Integer.class, java.lang.Integer.class, java.lang.String[].class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class, java.lang.String.class, java.lang.String.class, java.lang.Boolean.class, java.lang.Boolean.class, java.lang.Boolean.class, java.lang.Boolean.class, java.lang.Integer.class, java.lang.Integer.class, java.lang.String[].class}, false);
                return new com.codename1.media.VideoCodec((java.lang.String) adaptedArgs[0], (java.lang.String) adaptedArgs[1], (java.lang.String) adaptedArgs[2], ((Boolean) adaptedArgs[3]).booleanValue(), ((Boolean) adaptedArgs[4]).booleanValue(), ((Boolean) adaptedArgs[5]).booleanValue(), ((Boolean) adaptedArgs[6]).booleanValue(), toIntValue(adaptedArgs[7]), toIntValue(adaptedArgs[8]), (java.lang.String[]) adaptedArgs[9]);
            }
        }
        if (type == com.codename1.media.VideoFrame.class) {
            if (matches(safeArgs, new Class<?>[]{int[].class, java.lang.Integer.class, java.lang.Integer.class, java.lang.Long.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{int[].class, java.lang.Integer.class, java.lang.Integer.class, java.lang.Long.class}, false);
                return new com.codename1.media.VideoFrame((int[]) adaptedArgs[0], toIntValue(adaptedArgs[1]), toIntValue(adaptedArgs[2]), ((Number) adaptedArgs[3]).longValue());
            }
        }
        if (type == com.codename1.media.VideoWriterBuilder.class) {
            if (matches(safeArgs, new Class<?>[0], false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[0], false);
                return new com.codename1.media.VideoWriterBuilder();
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
        if (type == com.codename1.media.AudioEffects.class) return invokeStatic0(name, safeArgs);
        if (type == com.codename1.media.AudioMixer.class) return invokeStatic1(name, safeArgs);
        if (type == com.codename1.media.MediaManager.class) return invokeStatic2(name, safeArgs);
        if (type == com.codename1.media.SpeechRecognizer.class) return invokeStatic3(name, safeArgs);
        if (type == com.codename1.media.TextToSpeech.class) return invokeStatic4(name, safeArgs);
        if (type == com.codename1.media.TranscriptionRequest.class) return invokeStatic5(name, safeArgs);
        if (type == com.codename1.media.TranscriptionResult.class) return invokeStatic6(name, safeArgs);
        if (type == com.codename1.media.VideoIO.class) return invokeStatic7(name, safeArgs);
        throw unsupportedStatic(type, name, safeArgs);
    }

    private static Object invokeStatic0(String name, Object[] safeArgs) throws Exception {
        if ("equalize".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.media.AudioBuffer.class, java.lang.Float.class, java.lang.Float.class, java.lang.Float.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.media.AudioBuffer.class, java.lang.Float.class, java.lang.Float.class, java.lang.Float.class}, false);
                return com.codename1.media.AudioEffects.equalize((com.codename1.media.AudioBuffer) adaptedArgs[0], ((Number) adaptedArgs[1]).floatValue(), ((Number) adaptedArgs[2]).floatValue(), ((Number) adaptedArgs[3]).floatValue());
            }
            if (matches(safeArgs, new Class<?>[]{com.codename1.media.AudioBuffer.class, java.lang.Float.class, java.lang.Float.class, java.lang.Float.class, java.lang.Float.class, java.lang.Float.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.media.AudioBuffer.class, java.lang.Float.class, java.lang.Float.class, java.lang.Float.class, java.lang.Float.class, java.lang.Float.class}, false);
                return com.codename1.media.AudioEffects.equalize((com.codename1.media.AudioBuffer) adaptedArgs[0], ((Number) adaptedArgs[1]).floatValue(), ((Number) adaptedArgs[2]).floatValue(), ((Number) adaptedArgs[3]).floatValue(), ((Number) adaptedArgs[4]).floatValue(), ((Number) adaptedArgs[5]).floatValue());
            }
        }
        if ("gain".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.media.AudioBuffer.class, java.lang.Float.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.media.AudioBuffer.class, java.lang.Float.class}, false);
                return com.codename1.media.AudioEffects.gain((com.codename1.media.AudioBuffer) adaptedArgs[0], ((Number) adaptedArgs[1]).floatValue());
            }
        }
        if ("isSimdOptimizationsEnabled".equals(name)) {
            if (safeArgs.length == 0) {
                return com.codename1.media.AudioEffects.isSimdOptimizationsEnabled();
            }
        }
        if ("isolateCenter".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.media.AudioBuffer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.media.AudioBuffer.class}, false);
                return com.codename1.media.AudioEffects.isolateCenter((com.codename1.media.AudioBuffer) adaptedArgs[0]);
            }
        }
        if ("midSide".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.media.AudioBuffer.class, java.lang.Float.class, java.lang.Float.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.media.AudioBuffer.class, java.lang.Float.class, java.lang.Float.class}, false);
                return com.codename1.media.AudioEffects.midSide((com.codename1.media.AudioBuffer) adaptedArgs[0], ((Number) adaptedArgs[1]).floatValue(), ((Number) adaptedArgs[2]).floatValue());
            }
        }
        if ("normalize".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.media.AudioBuffer.class, java.lang.Float.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.media.AudioBuffer.class, java.lang.Float.class}, false);
                return com.codename1.media.AudioEffects.normalize((com.codename1.media.AudioBuffer) adaptedArgs[0], ((Number) adaptedArgs[1]).floatValue());
            }
        }
        if ("removeCenter".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.media.AudioBuffer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.media.AudioBuffer.class}, false);
                return com.codename1.media.AudioEffects.removeCenter((com.codename1.media.AudioBuffer) adaptedArgs[0]);
            }
        }
        if ("resetSimdOptimizationsEnabled".equals(name)) {
            if (safeArgs.length == 0) {
                com.codename1.media.AudioEffects.resetSimdOptimizationsEnabled(); return null;
            }
        }
        if ("setSimdOptimizationsEnabled".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false);
                com.codename1.media.AudioEffects.setSimdOptimizationsEnabled(((Boolean) adaptedArgs[0]).booleanValue()); return null;
            }
        }
        throw unsupportedStatic(com.codename1.media.AudioEffects.class, name, safeArgs);
    }

    private static Object invokeStatic1(String name, Object[] safeArgs) throws Exception {
        if ("isSimdOptimizationsEnabled".equals(name)) {
            if (safeArgs.length == 0) {
                return com.codename1.media.AudioMixer.isSimdOptimizationsEnabled();
            }
        }
        if ("resetSimdOptimizationsEnabled".equals(name)) {
            if (safeArgs.length == 0) {
                com.codename1.media.AudioMixer.resetSimdOptimizationsEnabled(); return null;
            }
        }
        if ("setSimdOptimizationsEnabled".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false);
                com.codename1.media.AudioMixer.setSimdOptimizationsEnabled(((Boolean) adaptedArgs[0]).booleanValue()); return null;
            }
        }
        throw unsupportedStatic(com.codename1.media.AudioMixer.class, name, safeArgs);
    }

    private static Object invokeStatic2(String name, Object[] safeArgs) throws Exception {
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
        if ("createFallbackSoundPoolPeer".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class}, false);
                return com.codename1.media.MediaManager.createFallbackSoundPoolPeer(toIntValue(adaptedArgs[0]));
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

    private static Object invokeStatic3(String name, Object[] safeArgs) throws Exception {
        if ("isSupported".equals(name)) {
            if (safeArgs.length == 0) {
                return com.codename1.media.SpeechRecognizer.isSupported();
            }
        }
        if ("recognize".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.media.RecognitionOptions.class, com.codename1.media.RecognitionCallback.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.media.RecognitionOptions.class, com.codename1.media.RecognitionCallback.class}, false);
                com.codename1.media.SpeechRecognizer.recognize((com.codename1.media.RecognitionOptions) adaptedArgs[0], (com.codename1.media.RecognitionCallback) adaptedArgs[1]); return null;
            }
        }
        if ("recognizeOnce".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.media.RecognitionCallback.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.media.RecognitionCallback.class}, false);
                com.codename1.media.SpeechRecognizer.recognizeOnce((com.codename1.media.RecognitionCallback) adaptedArgs[0]); return null;
            }
        }
        if ("stop".equals(name)) {
            if (safeArgs.length == 0) {
                com.codename1.media.SpeechRecognizer.stop(); return null;
            }
        }
        throw unsupportedStatic(com.codename1.media.SpeechRecognizer.class, name, safeArgs);
    }

    private static Object invokeStatic4(String name, Object[] safeArgs) throws Exception {
        if ("getAvailableVoices".equals(name)) {
            if (safeArgs.length == 0) {
                return com.codename1.media.TextToSpeech.getAvailableVoices();
            }
        }
        if ("isSupported".equals(name)) {
            if (safeArgs.length == 0) {
                return com.codename1.media.TextToSpeech.isSupported();
            }
        }
        if ("speak".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class}, false);
                com.codename1.media.TextToSpeech.speak((java.lang.String) adaptedArgs[0]); return null;
            }
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class, com.codename1.media.TtsOptions.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class, com.codename1.media.TtsOptions.class}, false);
                com.codename1.media.TextToSpeech.speak((java.lang.String) adaptedArgs[0], (com.codename1.media.TtsOptions) adaptedArgs[1]); return null;
            }
        }
        if ("stop".equals(name)) {
            if (safeArgs.length == 0) {
                com.codename1.media.TextToSpeech.stop(); return null;
            }
        }
        throw unsupportedStatic(com.codename1.media.TextToSpeech.class, name, safeArgs);
    }

    private static Object invokeStatic5(String name, Object[] safeArgs) throws Exception {
        if ("file".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class}, false);
                return com.codename1.media.TranscriptionRequest.file((java.lang.String) adaptedArgs[0]);
            }
        }
        throw unsupportedStatic(com.codename1.media.TranscriptionRequest.class, name, safeArgs);
    }

    private static Object invokeStatic6(String name, Object[] safeArgs) throws Exception {
        if ("textOnly".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class}, false);
                return com.codename1.media.TranscriptionResult.textOnly((java.lang.String) adaptedArgs[0]);
            }
        }
        throw unsupportedStatic(com.codename1.media.TranscriptionResult.class, name, safeArgs);
    }

    private static Object invokeStatic7(String name, Object[] safeArgs) throws Exception {
        if ("getVideoIO".equals(name)) {
            if (safeArgs.length == 0) {
                return com.codename1.media.VideoIO.getVideoIO();
            }
        }
        if ("isSupported".equals(name)) {
            if (safeArgs.length == 0) {
                return com.codename1.media.VideoIO.isSupported();
            }
        }
        throw unsupportedStatic(com.codename1.media.VideoIO.class, name, safeArgs);
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
        if (target instanceof com.codename1.media.AudioMixer) {
            try {
                return invoke2((com.codename1.media.AudioMixer) target, name, safeArgs);
            } catch (CN1AccessException ex) {
                unsupported = ex;
            }
        }
        if (target instanceof com.codename1.media.MediaMetaData) {
            try {
                return invoke3((com.codename1.media.MediaMetaData) target, name, safeArgs);
            } catch (CN1AccessException ex) {
                unsupported = ex;
            }
        }
        if (target instanceof com.codename1.media.MediaRecorderBuilder) {
            try {
                return invoke4((com.codename1.media.MediaRecorderBuilder) target, name, safeArgs);
            } catch (CN1AccessException ex) {
                unsupported = ex;
            }
        }
        if (target instanceof com.codename1.media.RecognitionOptions) {
            try {
                return invoke5((com.codename1.media.RecognitionOptions) target, name, safeArgs);
            } catch (CN1AccessException ex) {
                unsupported = ex;
            }
        }
        if (target instanceof com.codename1.media.RemoteControlListener) {
            try {
                return invoke6((com.codename1.media.RemoteControlListener) target, name, safeArgs);
            } catch (CN1AccessException ex) {
                unsupported = ex;
            }
        }
        if (target instanceof com.codename1.media.Transcriber) {
            try {
                return invoke7((com.codename1.media.Transcriber) target, name, safeArgs);
            } catch (CN1AccessException ex) {
                unsupported = ex;
            }
        }
        if (target instanceof com.codename1.media.TranscriptionRequest) {
            try {
                return invoke8((com.codename1.media.TranscriptionRequest) target, name, safeArgs);
            } catch (CN1AccessException ex) {
                unsupported = ex;
            }
        }
        if (target instanceof com.codename1.media.TranscriptionResult) {
            try {
                return invoke9((com.codename1.media.TranscriptionResult) target, name, safeArgs);
            } catch (CN1AccessException ex) {
                unsupported = ex;
            }
        }
        if (target instanceof com.codename1.media.TranscriptionSegment) {
            try {
                return invoke10((com.codename1.media.TranscriptionSegment) target, name, safeArgs);
            } catch (CN1AccessException ex) {
                unsupported = ex;
            }
        }
        if (target instanceof com.codename1.media.TtsOptions) {
            try {
                return invoke11((com.codename1.media.TtsOptions) target, name, safeArgs);
            } catch (CN1AccessException ex) {
                unsupported = ex;
            }
        }
        if (target instanceof com.codename1.media.VideoCodec) {
            try {
                return invoke12((com.codename1.media.VideoCodec) target, name, safeArgs);
            } catch (CN1AccessException ex) {
                unsupported = ex;
            }
        }
        if (target instanceof com.codename1.media.VideoFrame) {
            try {
                return invoke13((com.codename1.media.VideoFrame) target, name, safeArgs);
            } catch (CN1AccessException ex) {
                unsupported = ex;
            }
        }
        if (target instanceof com.codename1.media.VideoIO) {
            try {
                return invoke14((com.codename1.media.VideoIO) target, name, safeArgs);
            } catch (CN1AccessException ex) {
                unsupported = ex;
            }
        }
        if (target instanceof com.codename1.media.VideoReader) {
            try {
                return invoke15((com.codename1.media.VideoReader) target, name, safeArgs);
            } catch (CN1AccessException ex) {
                unsupported = ex;
            }
        }
        if (target instanceof com.codename1.media.VideoWriter) {
            try {
                return invoke16((com.codename1.media.VideoWriter) target, name, safeArgs);
            } catch (CN1AccessException ex) {
                unsupported = ex;
            }
        }
        if (target instanceof com.codename1.media.VideoWriterBuilder) {
            try {
                return invoke17((com.codename1.media.VideoWriterBuilder) target, name, safeArgs);
            } catch (CN1AccessException ex) {
                unsupported = ex;
            }
        }
        if (target instanceof com.codename1.media.WAVWriter) {
            try {
                return invoke18((com.codename1.media.WAVWriter) target, name, safeArgs);
            } catch (CN1AccessException ex) {
                unsupported = ex;
            }
        }
        if (target instanceof com.codename1.media.AsyncMedia) {
            try {
                return invoke19((com.codename1.media.AsyncMedia) target, name, safeArgs);
            } catch (CN1AccessException ex) {
                unsupported = ex;
            }
        }
        if (target instanceof com.codename1.media.AudioBuffer.AudioBufferCallback) {
            try {
                return invoke20((com.codename1.media.AudioBuffer.AudioBufferCallback) target, name, safeArgs);
            } catch (CN1AccessException ex) {
                unsupported = ex;
            }
        }
        if (target instanceof com.codename1.media.CompletionAwareSoundPoolPeer) {
            try {
                return invoke21((com.codename1.media.CompletionAwareSoundPoolPeer) target, name, safeArgs);
            } catch (CN1AccessException ex) {
                unsupported = ex;
            }
        }
        if (target instanceof com.codename1.media.Media) {
            try {
                return invoke22((com.codename1.media.Media) target, name, safeArgs);
            } catch (CN1AccessException ex) {
                unsupported = ex;
            }
        }
        if (target instanceof com.codename1.media.RecognitionCallback) {
            try {
                return invoke23((com.codename1.media.RecognitionCallback) target, name, safeArgs);
            } catch (CN1AccessException ex) {
                unsupported = ex;
            }
        }
        if (target instanceof com.codename1.media.SoundPoolPeer) {
            try {
                return invoke24((com.codename1.media.SoundPoolPeer) target, name, safeArgs);
            } catch (CN1AccessException ex) {
                unsupported = ex;
            }
        }
        if (target instanceof com.codename1.media.TimedRecognitionCallback) {
            try {
                return invoke25((com.codename1.media.TimedRecognitionCallback) target, name, safeArgs);
            } catch (CN1AccessException ex) {
                unsupported = ex;
            }
        }
        if (target instanceof com.codename1.media.VideoReader.FrameCallback) {
            try {
                return invoke26((com.codename1.media.VideoReader.FrameCallback) target, name, safeArgs);
            } catch (CN1AccessException ex) {
                unsupported = ex;
            }
        }
        if (target instanceof com.codename1.media.VoiceCompletionListener) {
            try {
                return invoke27((com.codename1.media.VoiceCompletionListener) target, name, safeArgs);
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

    private static Object invoke2(com.codename1.media.AudioMixer typedTarget, String name, Object[] safeArgs) throws Exception {
        if ("addTrack".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.media.AudioBuffer.class, java.lang.Long.class, java.lang.Float.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.media.AudioBuffer.class, java.lang.Long.class, java.lang.Float.class}, false);
                return typedTarget.addTrack((com.codename1.media.AudioBuffer) adaptedArgs[0], ((Number) adaptedArgs[1]).longValue(), ((Number) adaptedArgs[2]).floatValue());
            }
            if (matches(safeArgs, new Class<?>[]{float[].class, java.lang.Long.class, java.lang.Float.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{float[].class, java.lang.Long.class, java.lang.Float.class}, false);
                return typedTarget.addTrack((float[]) adaptedArgs[0], ((Number) adaptedArgs[1]).longValue(), ((Number) adaptedArgs[2]).floatValue());
            }
            if (matches(safeArgs, new Class<?>[]{float[].class, java.lang.Integer.class, java.lang.Integer.class, java.lang.Long.class, java.lang.Float.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{float[].class, java.lang.Integer.class, java.lang.Integer.class, java.lang.Long.class, java.lang.Float.class}, false);
                return typedTarget.addTrack((float[]) adaptedArgs[0], toIntValue(adaptedArgs[1]), toIntValue(adaptedArgs[2]), ((Number) adaptedArgs[3]).longValue(), ((Number) adaptedArgs[4]).floatValue());
            }
        }
        if ("addTrackAtFrame".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.media.AudioBuffer.class, java.lang.Integer.class, java.lang.Float.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.media.AudioBuffer.class, java.lang.Integer.class, java.lang.Float.class}, false);
                return typedTarget.addTrackAtFrame((com.codename1.media.AudioBuffer) adaptedArgs[0], toIntValue(adaptedArgs[1]), ((Number) adaptedArgs[2]).floatValue());
            }
            if (matches(safeArgs, new Class<?>[]{float[].class, java.lang.Integer.class, java.lang.Integer.class, java.lang.Integer.class, java.lang.Float.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{float[].class, java.lang.Integer.class, java.lang.Integer.class, java.lang.Integer.class, java.lang.Float.class}, false);
                return typedTarget.addTrackAtFrame((float[]) adaptedArgs[0], toIntValue(adaptedArgs[1]), toIntValue(adaptedArgs[2]), toIntValue(adaptedArgs[3]), ((Number) adaptedArgs[4]).floatValue());
            }
        }
        if ("clear".equals(name)) {
            if (safeArgs.length == 0) {
                typedTarget.clear(); return null;
            }
        }
        if ("getNumChannels".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getNumChannels();
            }
        }
        if ("getOutputFrameCount".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getOutputFrameCount();
            }
        }
        if ("getOutputSize".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getOutputSize();
            }
        }
        if ("getSampleRate".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getSampleRate();
            }
        }
        if ("getTrackCount".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getTrackCount();
            }
        }
        if ("mix".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.mix();
            }
        }
        throw unsupportedInstance(typedTarget, name, safeArgs);
    }

    private static Object invoke3(com.codename1.media.MediaMetaData typedTarget, String name, Object[] safeArgs) throws Exception {
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

    private static Object invoke4(com.codename1.media.MediaRecorderBuilder typedTarget, String name, Object[] safeArgs) throws Exception {
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

    private static Object invoke5(com.codename1.media.RecognitionOptions typedTarget, String name, Object[] safeArgs) throws Exception {
        if ("getLanguageTag".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getLanguageTag();
            }
        }
        if ("getMaxResults".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getMaxResults();
            }
        }
        if ("isContinuous".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.isContinuous();
            }
        }
        if ("isPartialResults".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.isPartialResults();
            }
        }
        if ("setContinuous".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false);
                return typedTarget.setContinuous(((Boolean) adaptedArgs[0]).booleanValue());
            }
        }
        if ("setLanguageTag".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class}, false);
                return typedTarget.setLanguageTag((java.lang.String) adaptedArgs[0]);
            }
        }
        if ("setMaxResults".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class}, false);
                return typedTarget.setMaxResults(toIntValue(adaptedArgs[0]));
            }
        }
        if ("setPartialResults".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false);
                return typedTarget.setPartialResults(((Boolean) adaptedArgs[0]).booleanValue());
            }
        }
        throw unsupportedInstance(typedTarget, name, safeArgs);
    }

    private static Object invoke6(com.codename1.media.RemoteControlListener typedTarget, String name, Object[] safeArgs) throws Exception {
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

    private static Object invoke7(com.codename1.media.Transcriber typedTarget, String name, Object[] safeArgs) throws Exception {
        if ("getProvider".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getProvider();
            }
        }
        if ("transcribe".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.media.TranscriptionRequest.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.media.TranscriptionRequest.class}, false);
                return typedTarget.transcribe((com.codename1.media.TranscriptionRequest) adaptedArgs[0]);
            }
        }
        throw unsupportedInstance(typedTarget, name, safeArgs);
    }

    private static Object invoke8(com.codename1.media.TranscriptionRequest typedTarget, String name, Object[] safeArgs) throws Exception {
        if ("getAudioPath".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getAudioPath();
            }
        }
        if ("getLanguageTag".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getLanguageTag();
            }
        }
        if ("getOption".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class}, false);
                return typedTarget.getOption((java.lang.String) adaptedArgs[0]);
            }
        }
        if ("getOptions".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getOptions();
            }
        }
        if ("getPrompt".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getPrompt();
            }
        }
        if ("setLanguageTag".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class}, false);
                return typedTarget.setLanguageTag((java.lang.String) adaptedArgs[0]);
            }
        }
        if ("setOption".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class, java.lang.String.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class, java.lang.String.class}, false);
                return typedTarget.setOption((java.lang.String) adaptedArgs[0], (java.lang.String) adaptedArgs[1]);
            }
        }
        if ("setPrompt".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class}, false);
                return typedTarget.setPrompt((java.lang.String) adaptedArgs[0]);
            }
        }
        throw unsupportedInstance(typedTarget, name, safeArgs);
    }

    private static Object invoke9(com.codename1.media.TranscriptionResult typedTarget, String name, Object[] safeArgs) throws Exception {
        if ("getSegments".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getSegments();
            }
        }
        if ("getText".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getText();
            }
        }
        if ("toSrt".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.toSrt();
            }
        }
        if ("toVtt".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.toVtt();
            }
        }
        throw unsupportedInstance(typedTarget, name, safeArgs);
    }

    private static Object invoke10(com.codename1.media.TranscriptionSegment typedTarget, String name, Object[] safeArgs) throws Exception {
        if ("getEndTimeMs".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getEndTimeMs();
            }
        }
        if ("getStartTimeMs".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getStartTimeMs();
            }
        }
        if ("getText".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getText();
            }
        }
        throw unsupportedInstance(typedTarget, name, safeArgs);
    }

    private static Object invoke11(com.codename1.media.TtsOptions typedTarget, String name, Object[] safeArgs) throws Exception {
        if ("getLanguageTag".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getLanguageTag();
            }
        }
        if ("getPitch".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getPitch();
            }
        }
        if ("getRate".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getRate();
            }
        }
        if ("getVoiceId".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getVoiceId();
            }
        }
        if ("getVolume".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getVolume();
            }
        }
        if ("setLanguageTag".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class}, false);
                return typedTarget.setLanguageTag((java.lang.String) adaptedArgs[0]);
            }
        }
        if ("setPitch".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Float.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Float.class}, false);
                return typedTarget.setPitch(((Number) adaptedArgs[0]).floatValue());
            }
        }
        if ("setRate".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Float.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Float.class}, false);
                return typedTarget.setRate(((Number) adaptedArgs[0]).floatValue());
            }
        }
        if ("setVoiceId".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class}, false);
                return typedTarget.setVoiceId((java.lang.String) adaptedArgs[0]);
            }
        }
        if ("setVolume".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Float.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Float.class}, false);
                return typedTarget.setVolume(((Number) adaptedArgs[0]).floatValue());
            }
        }
        throw unsupportedInstance(typedTarget, name, safeArgs);
    }

    private static Object invoke12(com.codename1.media.VideoCodec typedTarget, String name, Object[] safeArgs) throws Exception {
        if ("getId".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getId();
            }
        }
        if ("getMaxHeight".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getMaxHeight();
            }
        }
        if ("getMaxWidth".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getMaxWidth();
            }
        }
        if ("getMimeType".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getMimeType();
            }
        }
        if ("getName".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getName();
            }
        }
        if ("getSupportedContainers".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getSupportedContainers();
            }
        }
        if ("isDecoder".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.isDecoder();
            }
        }
        if ("isEncoder".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.isEncoder();
            }
        }
        if ("isHardwareAccelerated".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.isHardwareAccelerated();
            }
        }
        if ("isVideo".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.isVideo();
            }
        }
        if ("toString".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.toString();
            }
        }
        throw unsupportedInstance(typedTarget, name, safeArgs);
    }

    private static Object invoke13(com.codename1.media.VideoFrame typedTarget, String name, Object[] safeArgs) throws Exception {
        if ("getARGB".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getARGB();
            }
        }
        if ("getHeight".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getHeight();
            }
        }
        if ("getRGBA".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{byte[].class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{byte[].class}, false);
                typedTarget.getRGBA((byte[]) adaptedArgs[0]); return null;
            }
        }
        if ("getTimestampMillis".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getTimestampMillis();
            }
        }
        if ("getWidth".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getWidth();
            }
        }
        if ("toImage".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.toImage();
            }
        }
        throw unsupportedInstance(typedTarget, name, safeArgs);
    }

    private static Object invoke14(com.codename1.media.VideoIO typedTarget, String name, Object[] safeArgs) throws Exception {
        if ("createWriter".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.media.VideoWriterBuilder.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.media.VideoWriterBuilder.class}, false);
                return typedTarget.createWriter((com.codename1.media.VideoWriterBuilder) adaptedArgs[0]);
            }
        }
        if ("getAvailableDecoders".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getAvailableDecoders();
            }
        }
        if ("getAvailableEncoders".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getAvailableEncoders();
            }
        }
        if ("isDecoderSupported".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class}, false);
                return typedTarget.isDecoderSupported((java.lang.String) adaptedArgs[0]);
            }
        }
        if ("isEncoderSupported".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class}, false);
                return typedTarget.isEncoderSupported((java.lang.String) adaptedArgs[0]);
            }
        }
        if ("openReader".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class}, false);
                return typedTarget.openReader((java.lang.String) adaptedArgs[0]);
            }
        }
        throw unsupportedInstance(typedTarget, name, safeArgs);
    }

    private static Object invoke15(com.codename1.media.VideoReader typedTarget, String name, Object[] safeArgs) throws Exception {
        if ("close".equals(name)) {
            if (safeArgs.length == 0) {
                typedTarget.close(); return null;
            }
        }
        if ("frameAt".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Long.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Long.class}, false);
                return typedTarget.frameAt(((Number) adaptedArgs[0]).longValue());
            }
        }
        if ("getAudioChannels".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getAudioChannels();
            }
        }
        if ("getAudioSampleRate".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getAudioSampleRate();
            }
        }
        if ("getDurationMillis".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getDurationMillis();
            }
        }
        if ("getFrameRate".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getFrameRate();
            }
        }
        if ("getHeight".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getHeight();
            }
        }
        if ("getWidth".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getWidth();
            }
        }
        if ("hasAudio".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.hasAudio();
            }
        }
        if ("hasVideo".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.hasVideo();
            }
        }
        if ("readAudio".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.readAudio();
            }
        }
        if ("readFrames".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Float.class, com.codename1.media.VideoReader.FrameCallback.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Float.class, com.codename1.media.VideoReader.FrameCallback.class}, false);
                typedTarget.readFrames(((Number) adaptedArgs[0]).floatValue(), (com.codename1.media.VideoReader.FrameCallback) adaptedArgs[1]); return null;
            }
        }
        throw unsupportedInstance(typedTarget, name, safeArgs);
    }

    private static Object invoke16(com.codename1.media.VideoWriter typedTarget, String name, Object[] safeArgs) throws Exception {
        if ("close".equals(name)) {
            if (safeArgs.length == 0) {
                typedTarget.close(); return null;
            }
        }
        if ("getFrameRate".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getFrameRate();
            }
        }
        if ("getHeight".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getHeight();
            }
        }
        if ("getWidth".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getWidth();
            }
        }
        if ("writeAudio".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.media.AudioBuffer.class, java.lang.Long.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.media.AudioBuffer.class, java.lang.Long.class}, false);
                typedTarget.writeAudio((com.codename1.media.AudioBuffer) adaptedArgs[0], ((Number) adaptedArgs[1]).longValue()); return null;
            }
            if (matches(safeArgs, new Class<?>[]{short[].class, java.lang.Integer.class, java.lang.Integer.class, java.lang.Long.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{short[].class, java.lang.Integer.class, java.lang.Integer.class, java.lang.Long.class}, false);
                typedTarget.writeAudio((short[]) adaptedArgs[0], toIntValue(adaptedArgs[1]), toIntValue(adaptedArgs[2]), ((Number) adaptedArgs[3]).longValue()); return null;
            }
        }
        if ("writeFrame".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ui.Image.class, java.lang.Long.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.ui.Image.class, java.lang.Long.class}, false);
                typedTarget.writeFrame((com.codename1.ui.Image) adaptedArgs[0], ((Number) adaptedArgs[1]).longValue()); return null;
            }
            if (matches(safeArgs, new Class<?>[]{int[].class, java.lang.Integer.class, java.lang.Integer.class, java.lang.Long.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{int[].class, java.lang.Integer.class, java.lang.Integer.class, java.lang.Long.class}, false);
                typedTarget.writeFrame((int[]) adaptedArgs[0], toIntValue(adaptedArgs[1]), toIntValue(adaptedArgs[2]), ((Number) adaptedArgs[3]).longValue()); return null;
            }
        }
        throw unsupportedInstance(typedTarget, name, safeArgs);
    }

    private static Object invoke17(com.codename1.media.VideoWriterBuilder typedTarget, String name, Object[] safeArgs) throws Exception {
        if ("audioBitRate".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class}, false);
                return typedTarget.audioBitRate(toIntValue(adaptedArgs[0]));
            }
        }
        if ("audioChannels".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class}, false);
                return typedTarget.audioChannels(toIntValue(adaptedArgs[0]));
            }
        }
        if ("audioCodec".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class}, false);
                return typedTarget.audioCodec((java.lang.String) adaptedArgs[0]);
            }
        }
        if ("build".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.build();
            }
        }
        if ("container".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class}, false);
                return typedTarget.container((java.lang.String) adaptedArgs[0]);
            }
        }
        if ("frameRate".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Float.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Float.class}, false);
                return typedTarget.frameRate(((Number) adaptedArgs[0]).floatValue());
            }
        }
        if ("getAudioBitRate".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getAudioBitRate();
            }
        }
        if ("getAudioChannels".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getAudioChannels();
            }
        }
        if ("getAudioCodec".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getAudioCodec();
            }
        }
        if ("getContainer".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getContainer();
            }
        }
        if ("getFrameRate".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getFrameRate();
            }
        }
        if ("getHeight".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getHeight();
            }
        }
        if ("getKeyFrameInterval".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getKeyFrameInterval();
            }
        }
        if ("getPath".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getPath();
            }
        }
        if ("getSampleRate".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getSampleRate();
            }
        }
        if ("getVideoBitRate".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getVideoBitRate();
            }
        }
        if ("getVideoCodec".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getVideoCodec();
            }
        }
        if ("getWidth".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getWidth();
            }
        }
        if ("hasAudio".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false);
                return typedTarget.hasAudio(((Boolean) adaptedArgs[0]).booleanValue());
            }
        }
        if ("hasVideo".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Boolean.class}, false);
                return typedTarget.hasVideo(((Boolean) adaptedArgs[0]).booleanValue());
            }
        }
        if ("height".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class}, false);
                return typedTarget.height(toIntValue(adaptedArgs[0]));
            }
        }
        if ("isHasAudio".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.isHasAudio();
            }
        }
        if ("isHasVideo".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.isHasVideo();
            }
        }
        if ("keyFrameInterval".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Float.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Float.class}, false);
                return typedTarget.keyFrameInterval(((Number) adaptedArgs[0]).floatValue());
            }
        }
        if ("path".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class}, false);
                return typedTarget.path((java.lang.String) adaptedArgs[0]);
            }
        }
        if ("sampleRate".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class}, false);
                return typedTarget.sampleRate(toIntValue(adaptedArgs[0]));
            }
        }
        if ("videoBitRate".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class}, false);
                return typedTarget.videoBitRate(toIntValue(adaptedArgs[0]));
            }
        }
        if ("videoCodec".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class}, false);
                return typedTarget.videoCodec((java.lang.String) adaptedArgs[0]);
            }
        }
        if ("width".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class}, false);
                return typedTarget.width(toIntValue(adaptedArgs[0]));
            }
        }
        throw unsupportedInstance(typedTarget, name, safeArgs);
    }

    private static Object invoke18(com.codename1.media.WAVWriter typedTarget, String name, Object[] safeArgs) throws Exception {
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

    private static Object invoke19(com.codename1.media.AsyncMedia typedTarget, String name, Object[] safeArgs) throws Exception {
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

    private static Object invoke20(com.codename1.media.AudioBuffer.AudioBufferCallback typedTarget, String name, Object[] safeArgs) throws Exception {
        if ("frameReceived".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.media.AudioBuffer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.media.AudioBuffer.class}, false);
                typedTarget.frameReceived((com.codename1.media.AudioBuffer) adaptedArgs[0]); return null;
            }
        }
        throw unsupportedInstance(typedTarget, name, safeArgs);
    }

    private static Object invoke21(com.codename1.media.CompletionAwareSoundPoolPeer typedTarget, String name, Object[] safeArgs) throws Exception {
        if ("autoPause".equals(name)) {
            if (safeArgs.length == 0) {
                typedTarget.autoPause(); return null;
            }
        }
        if ("autoResume".equals(name)) {
            if (safeArgs.length == 0) {
                typedTarget.autoResume(); return null;
            }
        }
        if ("loadSound".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class}, false);
                return typedTarget.loadSound((java.lang.String) adaptedArgs[0]);
            }
        }
        if ("pauseVoice".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class}, false);
                typedTarget.pauseVoice(toIntValue(adaptedArgs[0])); return null;
            }
        }
        if ("play".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Object.class, java.lang.Float.class, java.lang.Float.class, java.lang.Float.class, java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Object.class, java.lang.Float.class, java.lang.Float.class, java.lang.Float.class, java.lang.Integer.class}, false);
                return typedTarget.play((java.lang.Object) adaptedArgs[0], ((Number) adaptedArgs[1]).floatValue(), ((Number) adaptedArgs[2]).floatValue(), ((Number) adaptedArgs[3]).floatValue(), toIntValue(adaptedArgs[4]));
            }
        }
        if ("release".equals(name)) {
            if (safeArgs.length == 0) {
                typedTarget.release(); return null;
            }
        }
        if ("resumeVoice".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class}, false);
                typedTarget.resumeVoice(toIntValue(adaptedArgs[0])); return null;
            }
        }
        if ("setPan".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class, java.lang.Float.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class, java.lang.Float.class}, false);
                typedTarget.setPan(toIntValue(adaptedArgs[0]), ((Number) adaptedArgs[1]).floatValue()); return null;
            }
        }
        if ("setRate".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class, java.lang.Float.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class, java.lang.Float.class}, false);
                typedTarget.setRate(toIntValue(adaptedArgs[0]), ((Number) adaptedArgs[1]).floatValue()); return null;
            }
        }
        if ("setVoiceCompletionListener".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.media.VoiceCompletionListener.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.media.VoiceCompletionListener.class}, false);
                typedTarget.setVoiceCompletionListener((com.codename1.media.VoiceCompletionListener) adaptedArgs[0]); return null;
            }
        }
        if ("setVolume".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class, java.lang.Float.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class, java.lang.Float.class}, false);
                typedTarget.setVolume(toIntValue(adaptedArgs[0]), ((Number) adaptedArgs[1]).floatValue()); return null;
            }
        }
        if ("stopAll".equals(name)) {
            if (safeArgs.length == 0) {
                typedTarget.stopAll(); return null;
            }
        }
        if ("stopVoice".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class}, false);
                typedTarget.stopVoice(toIntValue(adaptedArgs[0])); return null;
            }
        }
        if ("unloadSound".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Object.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Object.class}, false);
                typedTarget.unloadSound((java.lang.Object) adaptedArgs[0]); return null;
            }
        }
        throw unsupportedInstance(typedTarget, name, safeArgs);
    }

    private static Object invoke22(com.codename1.media.Media typedTarget, String name, Object[] safeArgs) throws Exception {
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

    private static Object invoke23(com.codename1.media.RecognitionCallback typedTarget, String name, Object[] safeArgs) throws Exception {
        if ("onEnd".equals(name)) {
            if (safeArgs.length == 0) {
                typedTarget.onEnd(); return null;
            }
        }
        if ("onError".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Throwable.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Throwable.class}, false);
                typedTarget.onError((java.lang.Throwable) adaptedArgs[0]); return null;
            }
        }
        if ("onPartialResult".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class}, false);
                typedTarget.onPartialResult((java.lang.String) adaptedArgs[0]); return null;
            }
        }
        if ("onResult".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class, java.lang.Float.class, java.lang.String[].class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class, java.lang.Float.class, java.lang.String[].class}, false);
                typedTarget.onResult((java.lang.String) adaptedArgs[0], ((Number) adaptedArgs[1]).floatValue(), (java.lang.String[]) adaptedArgs[2]); return null;
            }
        }
        throw unsupportedInstance(typedTarget, name, safeArgs);
    }

    private static Object invoke24(com.codename1.media.SoundPoolPeer typedTarget, String name, Object[] safeArgs) throws Exception {
        if ("autoPause".equals(name)) {
            if (safeArgs.length == 0) {
                typedTarget.autoPause(); return null;
            }
        }
        if ("autoResume".equals(name)) {
            if (safeArgs.length == 0) {
                typedTarget.autoResume(); return null;
            }
        }
        if ("loadSound".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class}, false);
                return typedTarget.loadSound((java.lang.String) adaptedArgs[0]);
            }
        }
        if ("pauseVoice".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class}, false);
                typedTarget.pauseVoice(toIntValue(adaptedArgs[0])); return null;
            }
        }
        if ("play".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Object.class, java.lang.Float.class, java.lang.Float.class, java.lang.Float.class, java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Object.class, java.lang.Float.class, java.lang.Float.class, java.lang.Float.class, java.lang.Integer.class}, false);
                return typedTarget.play((java.lang.Object) adaptedArgs[0], ((Number) adaptedArgs[1]).floatValue(), ((Number) adaptedArgs[2]).floatValue(), ((Number) adaptedArgs[3]).floatValue(), toIntValue(adaptedArgs[4]));
            }
        }
        if ("release".equals(name)) {
            if (safeArgs.length == 0) {
                typedTarget.release(); return null;
            }
        }
        if ("resumeVoice".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class}, false);
                typedTarget.resumeVoice(toIntValue(adaptedArgs[0])); return null;
            }
        }
        if ("setPan".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class, java.lang.Float.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class, java.lang.Float.class}, false);
                typedTarget.setPan(toIntValue(adaptedArgs[0]), ((Number) adaptedArgs[1]).floatValue()); return null;
            }
        }
        if ("setRate".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class, java.lang.Float.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class, java.lang.Float.class}, false);
                typedTarget.setRate(toIntValue(adaptedArgs[0]), ((Number) adaptedArgs[1]).floatValue()); return null;
            }
        }
        if ("setVolume".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class, java.lang.Float.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class, java.lang.Float.class}, false);
                typedTarget.setVolume(toIntValue(adaptedArgs[0]), ((Number) adaptedArgs[1]).floatValue()); return null;
            }
        }
        if ("stopAll".equals(name)) {
            if (safeArgs.length == 0) {
                typedTarget.stopAll(); return null;
            }
        }
        if ("stopVoice".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class}, false);
                typedTarget.stopVoice(toIntValue(adaptedArgs[0])); return null;
            }
        }
        if ("unloadSound".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Object.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Object.class}, false);
                typedTarget.unloadSound((java.lang.Object) adaptedArgs[0]); return null;
            }
        }
        throw unsupportedInstance(typedTarget, name, safeArgs);
    }

    private static Object invoke25(com.codename1.media.TimedRecognitionCallback typedTarget, String name, Object[] safeArgs) throws Exception {
        if ("onEnd".equals(name)) {
            if (safeArgs.length == 0) {
                typedTarget.onEnd(); return null;
            }
        }
        if ("onError".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Throwable.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Throwable.class}, false);
                typedTarget.onError((java.lang.Throwable) adaptedArgs[0]); return null;
            }
        }
        if ("onPartialResult".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.media.TranscriptionResult.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.media.TranscriptionResult.class}, false);
                typedTarget.onPartialResult((com.codename1.media.TranscriptionResult) adaptedArgs[0]); return null;
            }
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class}, false);
                typedTarget.onPartialResult((java.lang.String) adaptedArgs[0]); return null;
            }
        }
        if ("onResult".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.media.TranscriptionResult.class, java.lang.Float.class, java.lang.String[].class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.media.TranscriptionResult.class, java.lang.Float.class, java.lang.String[].class}, false);
                typedTarget.onResult((com.codename1.media.TranscriptionResult) adaptedArgs[0], ((Number) adaptedArgs[1]).floatValue(), (java.lang.String[]) adaptedArgs[2]); return null;
            }
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class, java.lang.Float.class, java.lang.String[].class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class, java.lang.Float.class, java.lang.String[].class}, false);
                typedTarget.onResult((java.lang.String) adaptedArgs[0], ((Number) adaptedArgs[1]).floatValue(), (java.lang.String[]) adaptedArgs[2]); return null;
            }
        }
        throw unsupportedInstance(typedTarget, name, safeArgs);
    }

    private static Object invoke26(com.codename1.media.VideoReader.FrameCallback typedTarget, String name, Object[] safeArgs) throws Exception {
        if ("frame".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.media.VideoFrame.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.media.VideoFrame.class}, false);
                return typedTarget.frame((com.codename1.media.VideoFrame) adaptedArgs[0]);
            }
        }
        throw unsupportedInstance(typedTarget, name, safeArgs);
    }

    private static Object invoke27(com.codename1.media.VoiceCompletionListener typedTarget, String name, Object[] safeArgs) throws Exception {
        if ("onVoiceComplete".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class}, false);
                typedTarget.onVoiceComplete(toIntValue(adaptedArgs[0])); return null;
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
        if (type == com.codename1.media.VideoIO.class) return getStaticField5(name);
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

    private static Object getStaticField5(String name) throws Exception {
        if ("CODEC_AAC".equals(name)) return com.codename1.media.VideoIO.CODEC_AAC;
        if ("CODEC_AV1".equals(name)) return com.codename1.media.VideoIO.CODEC_AV1;
        if ("CODEC_H264".equals(name)) return com.codename1.media.VideoIO.CODEC_H264;
        if ("CODEC_HEVC".equals(name)) return com.codename1.media.VideoIO.CODEC_HEVC;
        if ("CODEC_OPUS".equals(name)) return com.codename1.media.VideoIO.CODEC_OPUS;
        if ("CODEC_PCM".equals(name)) return com.codename1.media.VideoIO.CODEC_PCM;
        if ("CODEC_VP8".equals(name)) return com.codename1.media.VideoIO.CODEC_VP8;
        if ("CODEC_VP9".equals(name)) return com.codename1.media.VideoIO.CODEC_VP9;
        if ("CONTAINER_MKV".equals(name)) return com.codename1.media.VideoIO.CONTAINER_MKV;
        if ("CONTAINER_MOV".equals(name)) return com.codename1.media.VideoIO.CONTAINER_MOV;
        if ("CONTAINER_MP4".equals(name)) return com.codename1.media.VideoIO.CONTAINER_MP4;
        if ("CONTAINER_WEBM".equals(name)) return com.codename1.media.VideoIO.CONTAINER_WEBM;
        throw unsupportedStaticField(com.codename1.media.VideoIO.class, name);
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
        if (type == com.codename1.printing.PrintResultListener.class) {
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
        if (type == com.codename1.printing.PrintResultListener.class) {
            return new com.codename1.printing.PrintResultListener() {
                public void onResult(com.codename1.printing.PrintResult arg0) {
                    try {
                        lambda.invoke(new Object[]{arg0});
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
