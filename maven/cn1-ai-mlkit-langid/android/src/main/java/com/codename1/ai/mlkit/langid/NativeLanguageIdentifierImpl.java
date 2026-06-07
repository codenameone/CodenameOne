package com.codename1.ai.mlkit.langid;


public class NativeLanguageIdentifierImpl {
    public String identify(String input) {
        com.google.mlkit.nl.languageid.LanguageIdentifier id =
                com.google.mlkit.nl.languageid.LanguageIdentification.getClient();
        final java.util.concurrent.atomic.AtomicReference<String> out =
                new java.util.concurrent.atomic.AtomicReference<String>("und");
        final java.util.concurrent.CountDownLatch latch = new java.util.concurrent.CountDownLatch(1);
        id.identifyLanguage(input)
          .addOnSuccessListener(new com.google.android.gms.tasks.OnSuccessListener<String>() {
              public void onSuccess(String s) { if (s != null) out.set(s); latch.countDown(); }
          })
          .addOnFailureListener(new com.google.android.gms.tasks.OnFailureListener() {
              public void onFailure(Exception e) { latch.countDown(); }
          });
        try { latch.await(); } catch (InterruptedException ie) { Thread.currentThread().interrupt(); }
        return out.get();
    }

    public boolean isSupported() {
        return true;
    }
}
