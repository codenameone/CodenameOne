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

package bsh.cn1.gen;

import bsh.cn1.CN1AccessException;

public final class GeneratedAccess_com_codename1_ai {
    private GeneratedAccess_com_codename1_ai() {
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
        if ("ChatMessage".equals(simpleName)) {
            return com.codename1.ai.ChatMessage.class;
        }
        if ("ChatRequest".equals(simpleName)) {
            return com.codename1.ai.ChatRequest.class;
        }
        if ("Builder".equals(simpleName)) {
            return com.codename1.ai.ChatRequest.Builder.class;
        }
        if ("ChatResponse".equals(simpleName)) {
            return com.codename1.ai.ChatResponse.class;
        }
        if ("ConversationStore".equals(simpleName)) {
            return com.codename1.ai.ConversationStore.class;
        }
        if ("Embedding".equals(simpleName)) {
            return com.codename1.ai.Embedding.class;
        }
        if ("EmbeddingRequest".equals(simpleName)) {
            return com.codename1.ai.EmbeddingRequest.class;
        }
        if ("Builder".equals(simpleName)) {
            return com.codename1.ai.EmbeddingRequest.Builder.class;
        }
        if ("EmbeddingResponse".equals(simpleName)) {
            return com.codename1.ai.EmbeddingResponse.class;
        }
        if ("GenerateImageRequest".equals(simpleName)) {
            return com.codename1.ai.GenerateImageRequest.class;
        }
        if ("ImageGenerator".equals(simpleName)) {
            return com.codename1.ai.ImageGenerator.class;
        }
        if ("ImagePart".equals(simpleName)) {
            return com.codename1.ai.ImagePart.class;
        }
        if ("LlmChatBinding".equals(simpleName)) {
            return com.codename1.ai.LlmChatBinding.class;
        }
        if ("LlmClient".equals(simpleName)) {
            return com.codename1.ai.LlmClient.class;
        }
        if ("LlmException".equals(simpleName)) {
            return com.codename1.ai.LlmException.class;
        }
        if ("ErrorType".equals(simpleName)) {
            return com.codename1.ai.LlmException.ErrorType.class;
        }
        if ("MessagePart".equals(simpleName)) {
            return com.codename1.ai.MessagePart.class;
        }
        if ("PromptTemplate".equals(simpleName)) {
            return com.codename1.ai.PromptTemplate.class;
        }
        if ("ResponseFormat".equals(simpleName)) {
            return com.codename1.ai.ResponseFormat.class;
        }
        if ("RetryPolicy".equals(simpleName)) {
            return com.codename1.ai.RetryPolicy.class;
        }
        if ("Role".equals(simpleName)) {
            return com.codename1.ai.Role.class;
        }
        if ("SafetyFilter".equals(simpleName)) {
            return com.codename1.ai.SafetyFilter.class;
        }
        if ("StreamingListener".equals(simpleName)) {
            return com.codename1.ai.StreamingListener.class;
        }
        if ("TextPart".equals(simpleName)) {
            return com.codename1.ai.TextPart.class;
        }
        if ("Tokenizer".equals(simpleName)) {
            return com.codename1.ai.Tokenizer.class;
        }
        if ("Tool".equals(simpleName)) {
            return com.codename1.ai.Tool.class;
        }
        if ("ToolCall".equals(simpleName)) {
            return com.codename1.ai.ToolCall.class;
        }
        if ("ToolChoice".equals(simpleName)) {
            return com.codename1.ai.ToolChoice.class;
        }
        if ("ToolHandler".equals(simpleName)) {
            return com.codename1.ai.ToolHandler.class;
        }
        if ("ToolResultPart".equals(simpleName)) {
            return com.codename1.ai.ToolResultPart.class;
        }
        if ("Usage".equals(simpleName)) {
            return com.codename1.ai.Usage.class;
        }
        return null;
    }
    public static Object construct(Class<?> type, Object[] args) throws Exception {
        Object[] safeArgs = safeArgs(args);
        if (type == com.codename1.ai.ChatMessage.class) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ai.Role.class, java.util.List.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.ai.Role.class, java.util.List.class}, false);
                return new com.codename1.ai.ChatMessage((com.codename1.ai.Role) adaptedArgs[0], (java.util.List) adaptedArgs[1]);
            }
            if (matches(safeArgs, new Class<?>[]{com.codename1.ai.Role.class, java.util.List.class, java.util.List.class, java.lang.String.class, java.lang.String.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.ai.Role.class, java.util.List.class, java.util.List.class, java.lang.String.class, java.lang.String.class}, false);
                return new com.codename1.ai.ChatMessage((com.codename1.ai.Role) adaptedArgs[0], (java.util.List) adaptedArgs[1], (java.util.List) adaptedArgs[2], (java.lang.String) adaptedArgs[3], (java.lang.String) adaptedArgs[4]);
            }
        }
        if (type == com.codename1.ai.ChatResponse.class) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ai.ChatMessage.class, java.util.List.class, java.lang.String.class, com.codename1.ai.Usage.class, java.lang.String.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.ai.ChatMessage.class, java.util.List.class, java.lang.String.class, com.codename1.ai.Usage.class, java.lang.String.class}, false);
                return new com.codename1.ai.ChatResponse((com.codename1.ai.ChatMessage) adaptedArgs[0], (java.util.List) adaptedArgs[1], (java.lang.String) adaptedArgs[2], (com.codename1.ai.Usage) adaptedArgs[3], (java.lang.String) adaptedArgs[4]);
            }
        }
        if (type == com.codename1.ai.ConversationStore.class) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class}, false);
                return new com.codename1.ai.ConversationStore((java.lang.String) adaptedArgs[0]);
            }
        }
        if (type == com.codename1.ai.Embedding.class) {
            if (matches(safeArgs, new Class<?>[]{float[].class, java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{float[].class, java.lang.Integer.class}, false);
                return new com.codename1.ai.Embedding((float[]) adaptedArgs[0], toIntValue(adaptedArgs[1]));
            }
        }
        if (type == com.codename1.ai.EmbeddingResponse.class) {
            if (matches(safeArgs, new Class<?>[]{java.util.List.class, com.codename1.ai.Usage.class, java.lang.String.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.util.List.class, com.codename1.ai.Usage.class, java.lang.String.class}, false);
                return new com.codename1.ai.EmbeddingResponse((java.util.List) adaptedArgs[0], (com.codename1.ai.Usage) adaptedArgs[1], (java.lang.String) adaptedArgs[2]);
            }
        }
        if (type == com.codename1.ai.GenerateImageRequest.class) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class}, false);
                return new com.codename1.ai.GenerateImageRequest((java.lang.String) adaptedArgs[0]);
            }
        }
        if (type == com.codename1.ai.ImagePart.class) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class}, false);
                return new com.codename1.ai.ImagePart((java.lang.String) adaptedArgs[0]);
            }
            if (matches(safeArgs, new Class<?>[]{byte[].class, java.lang.String.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{byte[].class, java.lang.String.class}, false);
                return new com.codename1.ai.ImagePart((byte[]) adaptedArgs[0], (java.lang.String) adaptedArgs[1]);
            }
        }
        if (type == com.codename1.ai.LlmException.class) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class}, false);
                return new com.codename1.ai.LlmException((java.lang.String) adaptedArgs[0]);
            }
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class, java.lang.Throwable.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class, java.lang.Throwable.class}, false);
                return new com.codename1.ai.LlmException((java.lang.String) adaptedArgs[0], (java.lang.Throwable) adaptedArgs[1]);
            }
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class, java.lang.Integer.class, java.lang.String.class, java.lang.String.class, java.lang.Throwable.class, com.codename1.ai.LlmException.ErrorType.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class, java.lang.Integer.class, java.lang.String.class, java.lang.String.class, java.lang.Throwable.class, com.codename1.ai.LlmException.ErrorType.class}, false);
                return new com.codename1.ai.LlmException((java.lang.String) adaptedArgs[0], toIntValue(adaptedArgs[1]), (java.lang.String) adaptedArgs[2], (java.lang.String) adaptedArgs[3], (java.lang.Throwable) adaptedArgs[4], (com.codename1.ai.LlmException.ErrorType) adaptedArgs[5]);
            }
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class, java.lang.Integer.class, java.lang.String.class, java.lang.String.class, java.lang.Throwable.class, com.codename1.ai.LlmException.ErrorType.class, java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class, java.lang.Integer.class, java.lang.String.class, java.lang.String.class, java.lang.Throwable.class, com.codename1.ai.LlmException.ErrorType.class, java.lang.Integer.class}, false);
                return new com.codename1.ai.LlmException((java.lang.String) adaptedArgs[0], toIntValue(adaptedArgs[1]), (java.lang.String) adaptedArgs[2], (java.lang.String) adaptedArgs[3], (java.lang.Throwable) adaptedArgs[4], (com.codename1.ai.LlmException.ErrorType) adaptedArgs[5], toIntValue(adaptedArgs[6]));
            }
        }
        if (type == com.codename1.ai.TextPart.class) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class}, false);
                return new com.codename1.ai.TextPart((java.lang.String) adaptedArgs[0]);
            }
        }
        if (type == com.codename1.ai.Tool.class) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class, java.lang.String.class, java.lang.String.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class, java.lang.String.class, java.lang.String.class}, false);
                return new com.codename1.ai.Tool((java.lang.String) adaptedArgs[0], (java.lang.String) adaptedArgs[1], (java.lang.String) adaptedArgs[2]);
            }
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class, java.lang.String.class, java.lang.String.class, com.codename1.ai.ToolHandler.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class, java.lang.String.class, java.lang.String.class, com.codename1.ai.ToolHandler.class}, false);
                return new com.codename1.ai.Tool((java.lang.String) adaptedArgs[0], (java.lang.String) adaptedArgs[1], (java.lang.String) adaptedArgs[2], (com.codename1.ai.ToolHandler) adaptedArgs[3]);
            }
        }
        if (type == com.codename1.ai.ToolCall.class) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class, java.lang.String.class, java.lang.String.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class, java.lang.String.class, java.lang.String.class}, false);
                return new com.codename1.ai.ToolCall((java.lang.String) adaptedArgs[0], (java.lang.String) adaptedArgs[1], (java.lang.String) adaptedArgs[2]);
            }
        }
        if (type == com.codename1.ai.ToolResultPart.class) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class, java.lang.String.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class, java.lang.String.class}, false);
                return new com.codename1.ai.ToolResultPart((java.lang.String) adaptedArgs[0], (java.lang.String) adaptedArgs[1]);
            }
        }
        if (type == com.codename1.ai.Usage.class) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class, java.lang.Integer.class, java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class, java.lang.Integer.class, java.lang.Integer.class}, false);
                return new com.codename1.ai.Usage(toIntValue(adaptedArgs[0]), toIntValue(adaptedArgs[1]), toIntValue(adaptedArgs[2]));
            }
        }
        throw unsupportedConstruct(type, safeArgs);
    }

    public static Object invokeStatic(Class<?> type, String name, Object[] args) throws Exception {
        Object[] safeArgs = safeArgs(args);
        if (type == com.codename1.ai.ChatMessage.class) return invokeStatic0(name, safeArgs);
        if (type == com.codename1.ai.ChatRequest.class) return invokeStatic1(name, safeArgs);
        if (type == com.codename1.ai.EmbeddingRequest.class) return invokeStatic2(name, safeArgs);
        if (type == com.codename1.ai.ImageGenerator.class) return invokeStatic3(name, safeArgs);
        if (type == com.codename1.ai.LlmChatBinding.class) return invokeStatic4(name, safeArgs);
        if (type == com.codename1.ai.LlmClient.class) return invokeStatic5(name, safeArgs);
        if (type == com.codename1.ai.PromptTemplate.class) return invokeStatic6(name, safeArgs);
        if (type == com.codename1.ai.RetryPolicy.class) return invokeStatic7(name, safeArgs);
        if (type == com.codename1.ai.Tokenizer.class) return invokeStatic8(name, safeArgs);
        if (type == com.codename1.ai.ToolChoice.class) return invokeStatic9(name, safeArgs);
        throw unsupportedStatic(type, name, safeArgs);
    }

    private static Object invokeStatic0(String name, Object[] safeArgs) throws Exception {
        if ("assistant".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class}, false);
                return com.codename1.ai.ChatMessage.assistant((java.lang.String) adaptedArgs[0]);
            }
        }
        if ("system".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class}, false);
                return com.codename1.ai.ChatMessage.system((java.lang.String) adaptedArgs[0]);
            }
        }
        if ("toolResult".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class, java.lang.String.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class, java.lang.String.class}, false);
                return com.codename1.ai.ChatMessage.toolResult((java.lang.String) adaptedArgs[0], (java.lang.String) adaptedArgs[1]);
            }
        }
        if ("user".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class}, false);
                return com.codename1.ai.ChatMessage.user((java.lang.String) adaptedArgs[0]);
            }
        }
        if ("userWithImage".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class, com.codename1.ai.ImagePart.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class, com.codename1.ai.ImagePart.class}, false);
                return com.codename1.ai.ChatMessage.userWithImage((java.lang.String) adaptedArgs[0], (com.codename1.ai.ImagePart) adaptedArgs[1]);
            }
        }
        throw unsupportedStatic(com.codename1.ai.ChatMessage.class, name, safeArgs);
    }

    private static Object invokeStatic1(String name, Object[] safeArgs) throws Exception {
        if ("builder".equals(name)) {
            if (safeArgs.length == 0) {
                return com.codename1.ai.ChatRequest.builder();
            }
        }
        throw unsupportedStatic(com.codename1.ai.ChatRequest.class, name, safeArgs);
    }

    private static Object invokeStatic2(String name, Object[] safeArgs) throws Exception {
        if ("builder".equals(name)) {
            if (safeArgs.length == 0) {
                return com.codename1.ai.EmbeddingRequest.builder();
            }
        }
        if ("of".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class, java.lang.String.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class, java.lang.String.class}, false);
                return com.codename1.ai.EmbeddingRequest.of((java.lang.String) adaptedArgs[0], (java.lang.String) adaptedArgs[1]);
            }
        }
        throw unsupportedStatic(com.codename1.ai.EmbeddingRequest.class, name, safeArgs);
    }

    private static Object invokeStatic3(String name, Object[] safeArgs) throws Exception {
        if ("onDevice".equals(name)) {
            if (safeArgs.length == 0) {
                return com.codename1.ai.ImageGenerator.onDevice();
            }
        }
        if ("openai".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class}, false);
                return com.codename1.ai.ImageGenerator.openai((java.lang.String) adaptedArgs[0]);
            }
        }
        if ("replicate".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class}, false);
                return com.codename1.ai.ImageGenerator.replicate((java.lang.String) adaptedArgs[0]);
            }
        }
        throw unsupportedStatic(com.codename1.ai.ImageGenerator.class, name, safeArgs);
    }

    private static Object invokeStatic4(String name, Object[] safeArgs) throws Exception {
        if ("bind".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.components.ChatView.class, com.codename1.ai.LlmClient.class, com.codename1.ai.ChatRequest.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.components.ChatView.class, com.codename1.ai.LlmClient.class, com.codename1.ai.ChatRequest.class}, false);
                com.codename1.ai.LlmChatBinding.bind((com.codename1.components.ChatView) adaptedArgs[0], (com.codename1.ai.LlmClient) adaptedArgs[1], (com.codename1.ai.ChatRequest) adaptedArgs[2]); return null;
            }
        }
        throw unsupportedStatic(com.codename1.ai.LlmChatBinding.class, name, safeArgs);
    }

    private static Object invokeStatic5(String name, Object[] safeArgs) throws Exception {
        if ("anthropic".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class}, false);
                return com.codename1.ai.LlmClient.anthropic((java.lang.String) adaptedArgs[0]);
            }
        }
        if ("gemini".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class}, false);
                return com.codename1.ai.LlmClient.gemini((java.lang.String) adaptedArgs[0]);
            }
        }
        if ("localOpenAiCompatible".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class, java.lang.String.class, java.lang.String.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class, java.lang.String.class, java.lang.String.class}, false);
                return com.codename1.ai.LlmClient.localOpenAiCompatible((java.lang.String) adaptedArgs[0], (java.lang.String) adaptedArgs[1], (java.lang.String) adaptedArgs[2]);
            }
        }
        if ("ollama".equals(name)) {
            if (safeArgs.length == 0) {
                return com.codename1.ai.LlmClient.ollama();
            }
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class}, false);
                return com.codename1.ai.LlmClient.ollama((java.lang.String) adaptedArgs[0]);
            }
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class, java.lang.String.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class, java.lang.String.class}, false);
                return com.codename1.ai.LlmClient.ollama((java.lang.String) adaptedArgs[0], (java.lang.String) adaptedArgs[1]);
            }
        }
        if ("openai".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class}, false);
                return com.codename1.ai.LlmClient.openai((java.lang.String) adaptedArgs[0]);
            }
        }
        throw unsupportedStatic(com.codename1.ai.LlmClient.class, name, safeArgs);
    }

    private static Object invokeStatic6(String name, Object[] safeArgs) throws Exception {
        if ("of".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class}, false);
                return com.codename1.ai.PromptTemplate.of((java.lang.String) adaptedArgs[0]);
            }
        }
        throw unsupportedStatic(com.codename1.ai.PromptTemplate.class, name, safeArgs);
    }

    private static Object invokeStatic7(String name, Object[] safeArgs) throws Exception {
        if ("custom".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class, java.lang.Long.class, java.lang.Long.class, java.lang.Double.class, java.lang.Boolean.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class, java.lang.Long.class, java.lang.Long.class, java.lang.Double.class, java.lang.Boolean.class}, false);
                return com.codename1.ai.RetryPolicy.custom(toIntValue(adaptedArgs[0]), ((Number) adaptedArgs[1]).longValue(), ((Number) adaptedArgs[2]).longValue(), ((Number) adaptedArgs[3]).doubleValue(), ((Boolean) adaptedArgs[4]).booleanValue());
            }
        }
        if ("exponentialBackoff".equals(name)) {
            if (safeArgs.length == 0) {
                return com.codename1.ai.RetryPolicy.exponentialBackoff();
            }
        }
        if ("none".equals(name)) {
            if (safeArgs.length == 0) {
                return com.codename1.ai.RetryPolicy.none();
            }
        }
        throw unsupportedStatic(com.codename1.ai.RetryPolicy.class, name, safeArgs);
    }

    private static Object invokeStatic8(String name, Object[] safeArgs) throws Exception {
        if ("estimate".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class}, false);
                return com.codename1.ai.Tokenizer.estimate((java.lang.String) adaptedArgs[0]);
            }
        }
        if ("estimateMessages".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.util.List.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.util.List.class}, false);
                return com.codename1.ai.Tokenizer.estimateMessages((java.util.List) adaptedArgs[0]);
            }
        }
        throw unsupportedStatic(com.codename1.ai.Tokenizer.class, name, safeArgs);
    }

    private static Object invokeStatic9(String name, Object[] safeArgs) throws Exception {
        if ("named".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class}, false);
                return com.codename1.ai.ToolChoice.named((java.lang.String) adaptedArgs[0]);
            }
        }
        throw unsupportedStatic(com.codename1.ai.ToolChoice.class, name, safeArgs);
    }

    public static Object invoke(Object target, String name, Object[] args) throws Exception {
        Object[] safeArgs = safeArgs(args);
        CN1AccessException unsupported = null;
        if (target instanceof com.codename1.ai.ImagePart) {
            try {
                return invoke0((com.codename1.ai.ImagePart) target, name, safeArgs);
            } catch (CN1AccessException ex) {
                unsupported = ex;
            }
        }
        if (target instanceof com.codename1.ai.TextPart) {
            try {
                return invoke1((com.codename1.ai.TextPart) target, name, safeArgs);
            } catch (CN1AccessException ex) {
                unsupported = ex;
            }
        }
        if (target instanceof com.codename1.ai.ToolResultPart) {
            try {
                return invoke2((com.codename1.ai.ToolResultPart) target, name, safeArgs);
            } catch (CN1AccessException ex) {
                unsupported = ex;
            }
        }
        if (target instanceof com.codename1.ai.ChatMessage) {
            try {
                return invoke3((com.codename1.ai.ChatMessage) target, name, safeArgs);
            } catch (CN1AccessException ex) {
                unsupported = ex;
            }
        }
        if (target instanceof com.codename1.ai.ChatRequest) {
            try {
                return invoke4((com.codename1.ai.ChatRequest) target, name, safeArgs);
            } catch (CN1AccessException ex) {
                unsupported = ex;
            }
        }
        if (target instanceof com.codename1.ai.ChatRequest.Builder) {
            try {
                return invoke5((com.codename1.ai.ChatRequest.Builder) target, name, safeArgs);
            } catch (CN1AccessException ex) {
                unsupported = ex;
            }
        }
        if (target instanceof com.codename1.ai.ChatResponse) {
            try {
                return invoke6((com.codename1.ai.ChatResponse) target, name, safeArgs);
            } catch (CN1AccessException ex) {
                unsupported = ex;
            }
        }
        if (target instanceof com.codename1.ai.ConversationStore) {
            try {
                return invoke7((com.codename1.ai.ConversationStore) target, name, safeArgs);
            } catch (CN1AccessException ex) {
                unsupported = ex;
            }
        }
        if (target instanceof com.codename1.ai.Embedding) {
            try {
                return invoke8((com.codename1.ai.Embedding) target, name, safeArgs);
            } catch (CN1AccessException ex) {
                unsupported = ex;
            }
        }
        if (target instanceof com.codename1.ai.EmbeddingRequest) {
            try {
                return invoke9((com.codename1.ai.EmbeddingRequest) target, name, safeArgs);
            } catch (CN1AccessException ex) {
                unsupported = ex;
            }
        }
        if (target instanceof com.codename1.ai.EmbeddingRequest.Builder) {
            try {
                return invoke10((com.codename1.ai.EmbeddingRequest.Builder) target, name, safeArgs);
            } catch (CN1AccessException ex) {
                unsupported = ex;
            }
        }
        if (target instanceof com.codename1.ai.EmbeddingResponse) {
            try {
                return invoke11((com.codename1.ai.EmbeddingResponse) target, name, safeArgs);
            } catch (CN1AccessException ex) {
                unsupported = ex;
            }
        }
        if (target instanceof com.codename1.ai.GenerateImageRequest) {
            try {
                return invoke12((com.codename1.ai.GenerateImageRequest) target, name, safeArgs);
            } catch (CN1AccessException ex) {
                unsupported = ex;
            }
        }
        if (target instanceof com.codename1.ai.ImageGenerator) {
            try {
                return invoke13((com.codename1.ai.ImageGenerator) target, name, safeArgs);
            } catch (CN1AccessException ex) {
                unsupported = ex;
            }
        }
        if (target instanceof com.codename1.ai.LlmClient) {
            try {
                return invoke14((com.codename1.ai.LlmClient) target, name, safeArgs);
            } catch (CN1AccessException ex) {
                unsupported = ex;
            }
        }
        if (target instanceof com.codename1.ai.LlmException) {
            try {
                return invoke15((com.codename1.ai.LlmException) target, name, safeArgs);
            } catch (CN1AccessException ex) {
                unsupported = ex;
            }
        }
        if (target instanceof com.codename1.ai.PromptTemplate) {
            try {
                return invoke16((com.codename1.ai.PromptTemplate) target, name, safeArgs);
            } catch (CN1AccessException ex) {
                unsupported = ex;
            }
        }
        if (target instanceof com.codename1.ai.RetryPolicy) {
            try {
                return invoke17((com.codename1.ai.RetryPolicy) target, name, safeArgs);
            } catch (CN1AccessException ex) {
                unsupported = ex;
            }
        }
        if (target instanceof com.codename1.ai.Tool) {
            try {
                return invoke18((com.codename1.ai.Tool) target, name, safeArgs);
            } catch (CN1AccessException ex) {
                unsupported = ex;
            }
        }
        if (target instanceof com.codename1.ai.ToolCall) {
            try {
                return invoke19((com.codename1.ai.ToolCall) target, name, safeArgs);
            } catch (CN1AccessException ex) {
                unsupported = ex;
            }
        }
        if (target instanceof com.codename1.ai.ToolChoice) {
            try {
                return invoke20((com.codename1.ai.ToolChoice) target, name, safeArgs);
            } catch (CN1AccessException ex) {
                unsupported = ex;
            }
        }
        if (target instanceof com.codename1.ai.Usage) {
            try {
                return invoke21((com.codename1.ai.Usage) target, name, safeArgs);
            } catch (CN1AccessException ex) {
                unsupported = ex;
            }
        }
        if (target instanceof com.codename1.ai.SafetyFilter) {
            try {
                return invoke22((com.codename1.ai.SafetyFilter) target, name, safeArgs);
            } catch (CN1AccessException ex) {
                unsupported = ex;
            }
        }
        if (target instanceof com.codename1.ai.StreamingListener) {
            try {
                return invoke23((com.codename1.ai.StreamingListener) target, name, safeArgs);
            } catch (CN1AccessException ex) {
                unsupported = ex;
            }
        }
        if (target instanceof com.codename1.ai.ToolHandler) {
            try {
                return invoke24((com.codename1.ai.ToolHandler) target, name, safeArgs);
            } catch (CN1AccessException ex) {
                unsupported = ex;
            }
        }
        if (unsupported != null) {
            throw unsupported;
        }
        throw unsupportedInstance(target, name, safeArgs);
    }

    private static Object invoke0(com.codename1.ai.ImagePart typedTarget, String name, Object[] safeArgs) throws Exception {
        if ("getData".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getData();
            }
        }
        if ("getMimeType".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getMimeType();
            }
        }
        if ("getUrl".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getUrl();
            }
        }
        if ("isUrl".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.isUrl();
            }
        }
        throw unsupportedInstance(typedTarget, name, safeArgs);
    }

    private static Object invoke1(com.codename1.ai.TextPart typedTarget, String name, Object[] safeArgs) throws Exception {
        if ("getText".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getText();
            }
        }
        throw unsupportedInstance(typedTarget, name, safeArgs);
    }

    private static Object invoke2(com.codename1.ai.ToolResultPart typedTarget, String name, Object[] safeArgs) throws Exception {
        if ("getResultJson".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getResultJson();
            }
        }
        if ("getToolCallId".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getToolCallId();
            }
        }
        throw unsupportedInstance(typedTarget, name, safeArgs);
    }

    private static Object invoke3(com.codename1.ai.ChatMessage typedTarget, String name, Object[] safeArgs) throws Exception {
        if ("getName".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getName();
            }
        }
        if ("getParts".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getParts();
            }
        }
        if ("getRole".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getRole();
            }
        }
        if ("getText".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getText();
            }
        }
        if ("getToolCallId".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getToolCallId();
            }
        }
        if ("getToolCalls".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getToolCalls();
            }
        }
        throw unsupportedInstance(typedTarget, name, safeArgs);
    }

    private static Object invoke4(com.codename1.ai.ChatRequest typedTarget, String name, Object[] safeArgs) throws Exception {
        if ("getMaxTokens".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getMaxTokens();
            }
        }
        if ("getMessages".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getMessages();
            }
        }
        if ("getMetadata".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getMetadata();
            }
        }
        if ("getModel".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getModel();
            }
        }
        if ("getResponseFormat".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getResponseFormat();
            }
        }
        if ("getSafetyFilter".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getSafetyFilter();
            }
        }
        if ("getSeed".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getSeed();
            }
        }
        if ("getStopSequences".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getStopSequences();
            }
        }
        if ("getTemperature".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getTemperature();
            }
        }
        if ("getToolChoice".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getToolChoice();
            }
        }
        if ("getTools".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getTools();
            }
        }
        if ("getTopP".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getTopP();
            }
        }
        if ("toBuilder".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.toBuilder();
            }
        }
        throw unsupportedInstance(typedTarget, name, safeArgs);
    }

    private static Object invoke5(com.codename1.ai.ChatRequest.Builder typedTarget, String name, Object[] safeArgs) throws Exception {
        if ("addMessage".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ai.ChatMessage.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.ai.ChatMessage.class}, false);
                return typedTarget.addMessage((com.codename1.ai.ChatMessage) adaptedArgs[0]);
            }
        }
        if ("build".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.build();
            }
        }
        if ("maxTokens".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class}, false);
                return typedTarget.maxTokens(Integer.valueOf(toIntValue(adaptedArgs[0])));
            }
        }
        if ("messages".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.util.List.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.util.List.class}, false);
                return typedTarget.messages((java.util.List) adaptedArgs[0]);
            }
        }
        if ("metadata".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.util.Map.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.util.Map.class}, false);
                return typedTarget.metadata((java.util.Map) adaptedArgs[0]);
            }
        }
        if ("model".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class}, false);
                return typedTarget.model((java.lang.String) adaptedArgs[0]);
            }
        }
        if ("responseFormat".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ai.ResponseFormat.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.ai.ResponseFormat.class}, false);
                return typedTarget.responseFormat((com.codename1.ai.ResponseFormat) adaptedArgs[0]);
            }
        }
        if ("safetyFilter".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ai.SafetyFilter.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.ai.SafetyFilter.class}, false);
                return typedTarget.safetyFilter((com.codename1.ai.SafetyFilter) adaptedArgs[0]);
            }
        }
        if ("seed".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Long.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Long.class}, false);
                return typedTarget.seed(Long.valueOf(((Number) adaptedArgs[0]).longValue()));
            }
        }
        if ("stopSequences".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.util.List.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.util.List.class}, false);
                return typedTarget.stopSequences((java.util.List) adaptedArgs[0]);
            }
        }
        if ("temperature".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Float.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Float.class}, false);
                return typedTarget.temperature(Float.valueOf(((Number) adaptedArgs[0]).floatValue()));
            }
        }
        if ("toolChoice".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ai.ToolChoice.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.ai.ToolChoice.class}, false);
                return typedTarget.toolChoice((com.codename1.ai.ToolChoice) adaptedArgs[0]);
            }
        }
        if ("tools".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.util.List.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.util.List.class}, false);
                return typedTarget.tools((java.util.List) adaptedArgs[0]);
            }
        }
        if ("topP".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Float.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Float.class}, false);
                return typedTarget.topP(Float.valueOf(((Number) adaptedArgs[0]).floatValue()));
            }
        }
        throw unsupportedInstance(typedTarget, name, safeArgs);
    }

    private static Object invoke6(com.codename1.ai.ChatResponse typedTarget, String name, Object[] safeArgs) throws Exception {
        if ("getAssistantMessage".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getAssistantMessage();
            }
        }
        if ("getFinishReason".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getFinishReason();
            }
        }
        if ("getModelUsed".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getModelUsed();
            }
        }
        if ("getText".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getText();
            }
        }
        if ("getToolCalls".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getToolCalls();
            }
        }
        if ("getUsage".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getUsage();
            }
        }
        throw unsupportedInstance(typedTarget, name, safeArgs);
    }

    private static Object invoke7(com.codename1.ai.ConversationStore typedTarget, String name, Object[] safeArgs) throws Exception {
        if ("clear".equals(name)) {
            if (safeArgs.length == 0) {
                typedTarget.clear(); return null;
            }
        }
        if ("getStorageKey".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getStorageKey();
            }
        }
        if ("load".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.load();
            }
        }
        if ("save".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.util.List.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.util.List.class}, false);
                typedTarget.save((java.util.List) adaptedArgs[0]); return null;
            }
        }
        throw unsupportedInstance(typedTarget, name, safeArgs);
    }

    private static Object invoke8(com.codename1.ai.Embedding typedTarget, String name, Object[] safeArgs) throws Exception {
        if ("getDimensions".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getDimensions();
            }
        }
        if ("getIndex".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getIndex();
            }
        }
        if ("getVector".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getVector();
            }
        }
        throw unsupportedInstance(typedTarget, name, safeArgs);
    }

    private static Object invoke9(com.codename1.ai.EmbeddingRequest typedTarget, String name, Object[] safeArgs) throws Exception {
        if ("getDimensions".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getDimensions();
            }
        }
        if ("getInputs".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getInputs();
            }
        }
        if ("getModel".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getModel();
            }
        }
        throw unsupportedInstance(typedTarget, name, safeArgs);
    }

    private static Object invoke10(com.codename1.ai.EmbeddingRequest.Builder typedTarget, String name, Object[] safeArgs) throws Exception {
        if ("addInput".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class}, false);
                return typedTarget.addInput((java.lang.String) adaptedArgs[0]);
            }
        }
        if ("build".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.build();
            }
        }
        if ("dimensions".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class}, false);
                return typedTarget.dimensions(Integer.valueOf(toIntValue(adaptedArgs[0])));
            }
        }
        if ("inputs".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.util.List.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.util.List.class}, false);
                return typedTarget.inputs((java.util.List) adaptedArgs[0]);
            }
        }
        if ("model".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class}, false);
                return typedTarget.model((java.lang.String) adaptedArgs[0]);
            }
        }
        throw unsupportedInstance(typedTarget, name, safeArgs);
    }

    private static Object invoke11(com.codename1.ai.EmbeddingResponse typedTarget, String name, Object[] safeArgs) throws Exception {
        if ("getData".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getData();
            }
        }
        if ("getModelUsed".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getModelUsed();
            }
        }
        if ("getUsage".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getUsage();
            }
        }
        throw unsupportedInstance(typedTarget, name, safeArgs);
    }

    private static Object invoke12(com.codename1.ai.GenerateImageRequest typedTarget, String name, Object[] safeArgs) throws Exception {
        if ("getCount".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getCount();
            }
        }
        if ("getModel".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getModel();
            }
        }
        if ("getPrompt".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getPrompt();
            }
        }
        if ("getQuality".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getQuality();
            }
        }
        if ("getSeed".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getSeed();
            }
        }
        if ("getSize".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getSize();
            }
        }
        if ("getStyle".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getStyle();
            }
        }
        if ("setCount".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class}, false);
                return typedTarget.setCount(toIntValue(adaptedArgs[0]));
            }
        }
        if ("setModel".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class}, false);
                return typedTarget.setModel((java.lang.String) adaptedArgs[0]);
            }
        }
        if ("setQuality".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class}, false);
                return typedTarget.setQuality((java.lang.String) adaptedArgs[0]);
            }
        }
        if ("setSeed".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Long.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Long.class}, false);
                return typedTarget.setSeed(Long.valueOf(((Number) adaptedArgs[0]).longValue()));
            }
        }
        if ("setSize".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class}, false);
                return typedTarget.setSize((java.lang.String) adaptedArgs[0]);
            }
        }
        if ("setStyle".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class}, false);
                return typedTarget.setStyle((java.lang.String) adaptedArgs[0]);
            }
        }
        throw unsupportedInstance(typedTarget, name, safeArgs);
    }

    private static Object invoke13(com.codename1.ai.ImageGenerator typedTarget, String name, Object[] safeArgs) throws Exception {
        if ("generate".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ai.GenerateImageRequest.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.ai.GenerateImageRequest.class}, false);
                return typedTarget.generate((com.codename1.ai.GenerateImageRequest) adaptedArgs[0]);
            }
        }
        throw unsupportedInstance(typedTarget, name, safeArgs);
    }

    private static Object invoke14(com.codename1.ai.LlmClient typedTarget, String name, Object[] safeArgs) throws Exception {
        if ("chat".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ai.ChatRequest.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.ai.ChatRequest.class}, false);
                return typedTarget.chat((com.codename1.ai.ChatRequest) adaptedArgs[0]);
            }
        }
        if ("chatStream".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ai.ChatRequest.class, com.codename1.ai.StreamingListener.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.ai.ChatRequest.class, com.codename1.ai.StreamingListener.class}, false);
                return typedTarget.chatStream((com.codename1.ai.ChatRequest) adaptedArgs[0], (com.codename1.ai.StreamingListener) adaptedArgs[1]);
            }
        }
        if ("embed".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ai.EmbeddingRequest.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.ai.EmbeddingRequest.class}, false);
                return typedTarget.embed((com.codename1.ai.EmbeddingRequest) adaptedArgs[0]);
            }
        }
        if ("getBaseUrl".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getBaseUrl();
            }
        }
        if ("getHttpTimeoutMs".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getHttpTimeoutMs();
            }
        }
        if ("getProvider".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getProvider();
            }
        }
        if ("setBaseUrl".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class}, false);
                typedTarget.setBaseUrl((java.lang.String) adaptedArgs[0]); return null;
            }
        }
        if ("setHttpTimeoutMs".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class}, false);
                typedTarget.setHttpTimeoutMs(toIntValue(adaptedArgs[0])); return null;
            }
        }
        throw unsupportedInstance(typedTarget, name, safeArgs);
    }

    private static Object invoke15(com.codename1.ai.LlmException typedTarget, String name, Object[] safeArgs) throws Exception {
        if ("addSuppressed".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Throwable.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Throwable.class}, false);
                typedTarget.addSuppressed((java.lang.Throwable) adaptedArgs[0]); return null;
            }
        }
        if ("getCause".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getCause();
            }
        }
        if ("getHttpStatus".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getHttpStatus();
            }
        }
        if ("getLocalizedMessage".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getLocalizedMessage();
            }
        }
        if ("getMessage".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getMessage();
            }
        }
        if ("getProviderErrorCode".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getProviderErrorCode();
            }
        }
        if ("getRawBody".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getRawBody();
            }
        }
        if ("getRetryAfterSeconds".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getRetryAfterSeconds();
            }
        }
        if ("getStackTrace".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getStackTrace();
            }
        }
        if ("getSuppressed".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getSuppressed();
            }
        }
        if ("getType".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getType();
            }
        }
        if ("initCause".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Throwable.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Throwable.class}, false);
                return typedTarget.initCause((java.lang.Throwable) adaptedArgs[0]);
            }
        }
        if ("printStackTrace".equals(name)) {
            if (safeArgs.length == 0) {
                typedTarget.printStackTrace(); return null;
            }
        }
        if ("setStackTrace".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.StackTraceElement[].class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.StackTraceElement[].class}, false);
                typedTarget.setStackTrace((java.lang.StackTraceElement[]) adaptedArgs[0]); return null;
            }
        }
        if ("toString".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.toString();
            }
        }
        throw unsupportedInstance(typedTarget, name, safeArgs);
    }

    private static Object invoke16(com.codename1.ai.PromptTemplate typedTarget, String name, Object[] safeArgs) throws Exception {
        if ("asSystem".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.asSystem();
            }
        }
        if ("asUser".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.asUser();
            }
        }
        if ("build".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.build();
            }
        }
        if ("put".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class, java.lang.String.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class, java.lang.String.class}, false);
                return typedTarget.put((java.lang.String) adaptedArgs[0], (java.lang.String) adaptedArgs[1]);
            }
        }
        if ("putAll".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.util.Map.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.util.Map.class}, false);
                return typedTarget.putAll((java.util.Map) adaptedArgs[0]);
            }
        }
        throw unsupportedInstance(typedTarget, name, safeArgs);
    }

    private static Object invoke17(com.codename1.ai.RetryPolicy typedTarget, String name, Object[] safeArgs) throws Exception {
        if ("computeDelayMs".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Throwable.class, java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Throwable.class, java.lang.Integer.class}, false);
                return typedTarget.computeDelayMs((java.lang.Throwable) adaptedArgs[0], toIntValue(adaptedArgs[1]));
            }
        }
        if ("getMaxAttempts".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getMaxAttempts();
            }
        }
        if ("shouldRetry".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Throwable.class, java.lang.Integer.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Throwable.class, java.lang.Integer.class}, false);
                return typedTarget.shouldRetry((java.lang.Throwable) adaptedArgs[0], toIntValue(adaptedArgs[1]));
            }
        }
        throw unsupportedInstance(typedTarget, name, safeArgs);
    }

    private static Object invoke18(com.codename1.ai.Tool typedTarget, String name, Object[] safeArgs) throws Exception {
        if ("getDescription".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getDescription();
            }
        }
        if ("getHandler".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getHandler();
            }
        }
        if ("getName".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getName();
            }
        }
        if ("getParametersJsonSchema".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getParametersJsonSchema();
            }
        }
        if ("invoke".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class}, false);
                return typedTarget.invoke((java.lang.String) adaptedArgs[0]);
            }
        }
        throw unsupportedInstance(typedTarget, name, safeArgs);
    }

    private static Object invoke19(com.codename1.ai.ToolCall typedTarget, String name, Object[] safeArgs) throws Exception {
        if ("execute".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.util.List.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.util.List.class}, false);
                return typedTarget.execute((java.util.List) adaptedArgs[0]);
            }
        }
        if ("findTool".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.util.List.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.util.List.class}, false);
                return typedTarget.findTool((java.util.List) adaptedArgs[0]);
            }
        }
        if ("getArgumentsJson".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getArgumentsJson();
            }
        }
        if ("getId".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getId();
            }
        }
        if ("getName".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getName();
            }
        }
        throw unsupportedInstance(typedTarget, name, safeArgs);
    }

    private static Object invoke20(com.codename1.ai.ToolChoice typedTarget, String name, Object[] safeArgs) throws Exception {
        if ("getForcedToolName".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getForcedToolName();
            }
        }
        if ("getMode".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getMode();
            }
        }
        throw unsupportedInstance(typedTarget, name, safeArgs);
    }

    private static Object invoke21(com.codename1.ai.Usage typedTarget, String name, Object[] safeArgs) throws Exception {
        if ("getCompletionTokens".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getCompletionTokens();
            }
        }
        if ("getPromptTokens".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getPromptTokens();
            }
        }
        if ("getTotalTokens".equals(name)) {
            if (safeArgs.length == 0) {
                return typedTarget.getTotalTokens();
            }
        }
        throw unsupportedInstance(typedTarget, name, safeArgs);
    }

    private static Object invoke22(com.codename1.ai.SafetyFilter typedTarget, String name, Object[] safeArgs) throws Exception {
        if ("check".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.util.List.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.util.List.class}, false);
                return typedTarget.check((java.util.List) adaptedArgs[0]);
            }
        }
        throw unsupportedInstance(typedTarget, name, safeArgs);
    }

    private static Object invoke23(com.codename1.ai.StreamingListener typedTarget, String name, Object[] safeArgs) throws Exception {
        if ("onContentDelta".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class}, false);
                typedTarget.onContentDelta((java.lang.String) adaptedArgs[0]); return null;
            }
        }
        if ("onError".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Throwable.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Throwable.class}, false);
                typedTarget.onError((java.lang.Throwable) adaptedArgs[0]); return null;
            }
        }
        if ("onToolCallDelta".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.Integer.class, java.lang.String.class, java.lang.String.class, java.lang.String.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.Integer.class, java.lang.String.class, java.lang.String.class, java.lang.String.class}, false);
                typedTarget.onToolCallDelta(toIntValue(adaptedArgs[0]), (java.lang.String) adaptedArgs[1], (java.lang.String) adaptedArgs[2], (java.lang.String) adaptedArgs[3]); return null;
            }
        }
        if ("onUsage".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{com.codename1.ai.Usage.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{com.codename1.ai.Usage.class}, false);
                typedTarget.onUsage((com.codename1.ai.Usage) adaptedArgs[0]); return null;
            }
        }
        throw unsupportedInstance(typedTarget, name, safeArgs);
    }

    private static Object invoke24(com.codename1.ai.ToolHandler typedTarget, String name, Object[] safeArgs) throws Exception {
        if ("invoke".equals(name)) {
            if (matches(safeArgs, new Class<?>[]{java.lang.String.class}, false)) {
                Object[] adaptedArgs = adaptArgs(safeArgs, new Class<?>[]{java.lang.String.class}, false);
                return typedTarget.invoke((java.lang.String) adaptedArgs[0]);
            }
        }
        throw unsupportedInstance(typedTarget, name, safeArgs);
    }

    public static Object getStaticField(Class<?> type, String name) throws Exception {
        if (type == com.codename1.ai.LlmClient.class) return getStaticField0(name);
        if (type == com.codename1.ai.LlmException.ErrorType.class) return getStaticField1(name);
        if (type == com.codename1.ai.ResponseFormat.class) return getStaticField2(name);
        if (type == com.codename1.ai.Role.class) return getStaticField3(name);
        if (type == com.codename1.ai.SafetyFilter.class) return getStaticField4(name);
        if (type == com.codename1.ai.ToolChoice.class) return getStaticField5(name);
        throw unsupportedStaticField(type, name);
    }

    private static Object getStaticField0(String name) throws Exception {
        if ("DEFAULT_ANTHROPIC_URL".equals(name)) return com.codename1.ai.LlmClient.DEFAULT_ANTHROPIC_URL;
        if ("DEFAULT_GEMINI_URL".equals(name)) return com.codename1.ai.LlmClient.DEFAULT_GEMINI_URL;
        if ("DEFAULT_OLLAMA_URL".equals(name)) return com.codename1.ai.LlmClient.DEFAULT_OLLAMA_URL;
        if ("DEFAULT_OPENAI_URL".equals(name)) return com.codename1.ai.LlmClient.DEFAULT_OPENAI_URL;
        throw unsupportedStaticField(com.codename1.ai.LlmClient.class, name);
    }

    private static Object getStaticField1(String name) throws Exception {
        if ("AUTH".equals(name)) return com.codename1.ai.LlmException.ErrorType.AUTH;
        if ("CONTEXT_LENGTH".equals(name)) return com.codename1.ai.LlmException.ErrorType.CONTEXT_LENGTH;
        if ("INVALID_REQUEST".equals(name)) return com.codename1.ai.LlmException.ErrorType.INVALID_REQUEST;
        if ("MODEL_OVERLOADED".equals(name)) return com.codename1.ai.LlmException.ErrorType.MODEL_OVERLOADED;
        if ("NETWORK".equals(name)) return com.codename1.ai.LlmException.ErrorType.NETWORK;
        if ("RATE_LIMIT".equals(name)) return com.codename1.ai.LlmException.ErrorType.RATE_LIMIT;
        if ("SERVER".equals(name)) return com.codename1.ai.LlmException.ErrorType.SERVER;
        if ("UNKNOWN".equals(name)) return com.codename1.ai.LlmException.ErrorType.UNKNOWN;
        throw unsupportedStaticField(com.codename1.ai.LlmException.ErrorType.class, name);
    }

    private static Object getStaticField2(String name) throws Exception {
        if ("JSON_OBJECT".equals(name)) return com.codename1.ai.ResponseFormat.JSON_OBJECT;
        if ("TEXT".equals(name)) return com.codename1.ai.ResponseFormat.TEXT;
        throw unsupportedStaticField(com.codename1.ai.ResponseFormat.class, name);
    }

    private static Object getStaticField3(String name) throws Exception {
        if ("ASSISTANT".equals(name)) return com.codename1.ai.Role.ASSISTANT;
        if ("SYSTEM".equals(name)) return com.codename1.ai.Role.SYSTEM;
        if ("TOOL".equals(name)) return com.codename1.ai.Role.TOOL;
        if ("USER".equals(name)) return com.codename1.ai.Role.USER;
        throw unsupportedStaticField(com.codename1.ai.Role.class, name);
    }

    private static Object getStaticField4(String name) throws Exception {
        if ("ALLOW_ALL".equals(name)) return com.codename1.ai.SafetyFilter.ALLOW_ALL;
        throw unsupportedStaticField(com.codename1.ai.SafetyFilter.class, name);
    }

    private static Object getStaticField5(String name) throws Exception {
        if ("AUTO".equals(name)) return com.codename1.ai.ToolChoice.AUTO;
        if ("NONE".equals(name)) return com.codename1.ai.ToolChoice.NONE;
        if ("REQUIRED".equals(name)) return com.codename1.ai.ToolChoice.REQUIRED;
        throw unsupportedStaticField(com.codename1.ai.ToolChoice.class, name);
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
