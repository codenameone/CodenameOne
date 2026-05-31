package com.codename1.notifications;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/// Tests for the notification permission request/result objects and the channel builder.
class NotificationApiTest {

    @Test
    void defaultPermissionRequestRequestsAlertSoundBadge() {
        NotificationPermissionRequest req = new NotificationPermissionRequest();
        // alert=4, sound=2, badge=1 -> 7
        assertEquals(7, req.toAuthorizationOptionsMask());
        assertTrue(req.isAlert());
        assertTrue(req.isSound());
        assertTrue(req.isBadge());
        assertFalse(req.isProvisional());
    }

    @Test
    void permissionRequestMaskMatchesUNAuthorizationOptionBits() {
        NotificationPermissionRequest req = new NotificationPermissionRequest()
                .alert(false).sound(false).badge(false)
                .provisional(true);
        // provisional bit is 64
        assertEquals(64, req.toAuthorizationOptionsMask());
        req.critical(true); // 16
        assertEquals(64 + 16, req.toAuthorizationOptionsMask());
    }

    @Test
    void permissionResultGrantedSemantics() {
        NotificationPermissionResult authorized =
                new NotificationPermissionResult(true, NotificationPermissionResult.AUTH_AUTHORIZED);
        assertTrue(authorized.isGranted());
        assertFalse(authorized.isProvisional());

        NotificationPermissionResult provisional =
                new NotificationPermissionResult(true, NotificationPermissionResult.AUTH_PROVISIONAL);
        assertTrue(provisional.isProvisional());

        NotificationPermissionResult denied =
                new NotificationPermissionResult(false, NotificationPermissionResult.AUTH_DENIED);
        assertFalse(denied.isGranted());
        assertEquals(NotificationPermissionResult.AUTH_DENIED, denied.getAuthorizationLevel());
    }

    @Test
    void channelBuilderStoresConfiguration() {
        long[] pattern = new long[]{0, 200, 100, 200};
        NotificationChannelBuilder b = new NotificationChannelBuilder("messages", "Messages")
                .description("Chat messages")
                .importance(NotificationChannelBuilder.IMPORTANCE_HIGH)
                .sound("/notification_sound_ping.mp3")
                .vibrationPattern(pattern)
                .lightColor(0xff0000)
                .lockscreenVisibility(NotificationChannelBuilder.VISIBILITY_PUBLIC)
                .group("chats")
                .showBadge(false);
        assertEquals("messages", b.getId());
        assertEquals("Messages", b.getName());
        assertEquals("Chat messages", b.getDescription());
        assertEquals(NotificationChannelBuilder.IMPORTANCE_HIGH, b.getImportance());
        assertEquals("/notification_sound_ping.mp3", b.getSound());
        assertTrue(b.isVibrationEnabled());
        assertArrayEquals(pattern, b.getVibrationPattern());
        assertTrue(b.isLightsEnabled());
        assertEquals(0xff0000, b.getLightColor());
        assertEquals(NotificationChannelBuilder.VISIBILITY_PUBLIC, b.getLockscreenVisibility());
        assertEquals("chats", b.getGroup());
        assertFalse(b.isShowBadge());
    }

    @Test
    void channelBuilderDefaults() {
        NotificationChannelBuilder b = new NotificationChannelBuilder("c", "C");
        assertEquals(NotificationChannelBuilder.IMPORTANCE_DEFAULT, b.getImportance());
        assertEquals(NotificationChannelBuilder.VISIBILITY_PRIVATE, b.getLockscreenVisibility());
        assertTrue(b.isShowBadge());
        assertFalse(b.isVibrationEnabled());
        assertFalse(b.isLightsEnabled());
    }
}
