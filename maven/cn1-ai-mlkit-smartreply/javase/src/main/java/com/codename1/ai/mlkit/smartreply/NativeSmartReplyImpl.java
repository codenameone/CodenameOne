package com.codename1.ai.mlkit.smartreply;

public class NativeSmartReplyImpl {
    public String[] suggest(String conversationJson) {
        return new String[]{"Sounds good", "Thanks!", "Got it"};
    }

    public boolean isSupported() {
        return true;
    }
}
