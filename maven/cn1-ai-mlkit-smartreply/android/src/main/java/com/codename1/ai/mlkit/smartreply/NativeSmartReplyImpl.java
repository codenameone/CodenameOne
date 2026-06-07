package com.codename1.ai.mlkit.smartreply;


public class NativeSmartReplyImpl {
    public String[] suggest(String conversationJson) {
        java.util.List<com.google.mlkit.nl.smartreply.TextMessage> msgs =
            new java.util.ArrayList<com.google.mlkit.nl.smartreply.TextMessage>();
        try {
            org.json.JSONArray a = new org.json.JSONArray(conversationJson);
            for (int i = 0; i < a.length(); i++) {
                org.json.JSONObject o = a.getJSONObject(i);
                String role = o.optString("role", "user");
                long ts = o.optLong("timestamp", 0);
                String text = o.optString("message", "");
                String userId = o.optString("userId", "u");
                if ("user".equals(role)) {
                    msgs.add(com.google.mlkit.nl.smartreply.TextMessage.createForLocalUser(text, ts));
                } else {
                    msgs.add(com.google.mlkit.nl.smartreply.TextMessage.createForRemoteUser(text, ts, userId));
                }
            }
        } catch (org.json.JSONException jex) {
            return new String[0];
        }
        final java.util.List<String> out = new java.util.ArrayList<String>();
        final java.util.concurrent.CountDownLatch latch = new java.util.concurrent.CountDownLatch(1);
        com.google.mlkit.nl.smartreply.SmartReplyGenerator gen =
                com.google.mlkit.nl.smartreply.SmartReply.getClient();
        gen.suggestReplies(msgs)
            .addOnSuccessListener(new com.google.android.gms.tasks.OnSuccessListener<
                    com.google.mlkit.nl.smartreply.SmartReplySuggestionResult>() {
                public void onSuccess(com.google.mlkit.nl.smartreply.SmartReplySuggestionResult r) {
                    for (com.google.mlkit.nl.smartreply.SmartReplySuggestion s : r.getSuggestions()) {
                        out.add(s.getText());
                    }
                    latch.countDown();
                }
            })
            .addOnFailureListener(new com.google.android.gms.tasks.OnFailureListener() {
                public void onFailure(Exception e) { latch.countDown(); }
            });
        try { latch.await(); } catch (InterruptedException ie) { Thread.currentThread().interrupt(); }
        return out.toArray(new String[0]);
    }

    public boolean isSupported() {
        return true;
    }
}
