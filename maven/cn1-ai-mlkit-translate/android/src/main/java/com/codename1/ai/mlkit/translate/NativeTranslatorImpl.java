package com.codename1.ai.mlkit.translate;


public class NativeTranslatorImpl {
    public String translate(String text, String sourceLang, String targetLang) {
        com.google.mlkit.nl.translate.TranslatorOptions opts =
            new com.google.mlkit.nl.translate.TranslatorOptions.Builder()
                .setSourceLanguage(sourceLang)
                .setTargetLanguage(targetLang)
                .build();
        com.google.mlkit.nl.translate.Translator t =
                com.google.mlkit.nl.translate.Translation.getClient(opts);
        final java.util.concurrent.atomic.AtomicReference<String> out =
                new java.util.concurrent.atomic.AtomicReference<String>("");
        final java.util.concurrent.CountDownLatch latch = new java.util.concurrent.CountDownLatch(1);
        t.downloadModelIfNeeded()
          .addOnSuccessListener(new com.google.android.gms.tasks.OnSuccessListener<Void>() {
              public void onSuccess(Void v) {
                  t.translate(text)
                   .addOnSuccessListener(new com.google.android.gms.tasks.OnSuccessListener<String>() {
                       public void onSuccess(String r) { out.set(r); latch.countDown(); }
                   })
                   .addOnFailureListener(new com.google.android.gms.tasks.OnFailureListener() {
                       public void onFailure(Exception e) { latch.countDown(); }
                   });
              }
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
