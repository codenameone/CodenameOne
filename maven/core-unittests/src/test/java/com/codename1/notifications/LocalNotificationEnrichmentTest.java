package com.codename1.notifications;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/// Exercises the backward-compatible enrichment of LocalNotification (actions,
/// messaging style, channel, grouping, progress and the fluent setters) without
/// requiring a running Display.
class LocalNotificationEnrichmentTest {

    @Test
    void existingFieldsRemainDefaulted() {
        LocalNotification n = new LocalNotification();
        assertEquals("", n.getId());
        assertEquals(-1, n.getBadgeNumber());
        assertTrue(n.getActions().isEmpty());
        assertNull(n.getChannelId());
        assertNull(n.getGroupId());
        assertNull(n.getMessagingStyle());
        assertFalse(n.isOngoing());
        assertFalse(n.isTimeSensitive());
        assertFalse(n.isFullScreenIntent());
        assertEquals(0, n.getProgressMax());
    }

    @Test
    void fluentSettersChainAndStore() {
        LocalNotification n = new LocalNotification()
                .setChannelId("messages")
                .setGroup("chat-42")
                .setGroupSummary(true)
                .setFullScreenIntent(true)
                .setTimeSensitive(true)
                .setOngoing(true)
                .setCustomView("my_layout")
                .setProgress(100, 40);
        assertEquals("messages", n.getChannelId());
        assertEquals("chat-42", n.getGroupId());
        assertTrue(n.isGroupSummary());
        assertTrue(n.isFullScreenIntent());
        assertTrue(n.isTimeSensitive());
        assertTrue(n.isOngoing());
        assertEquals("my_layout", n.getCustomView());
        assertEquals(100, n.getProgressMax());
        assertEquals(40, n.getProgress());
        assertFalse(n.isProgressIndeterminate());
    }

    @Test
    void indeterminateProgressOverridesDeterminate() {
        LocalNotification n = new LocalNotification().setProgress(100, 50).setIndeterminateProgress(true);
        assertTrue(n.isProgressIndeterminate());
    }

    @Test
    void setProgressClearsIndeterminate() {
        LocalNotification n = new LocalNotification().setIndeterminateProgress(true).setProgress(10, 5);
        assertFalse(n.isProgressIndeterminate());
    }

    @Test
    void actionsAreStoredInOrder() {
        LocalNotification n = new LocalNotification()
                .addAction("accept", "Accept")
                .addAction(new LocalNotification.Action("decline", "Decline", "ic_decline"));
        List<LocalNotification.Action> actions = n.getActions();
        assertEquals(2, actions.size());
        assertEquals("accept", actions.get(0).getId());
        assertEquals("Accept", actions.get(0).getTitle());
        assertFalse(actions.get(0).isTextInput());
        assertEquals("decline", actions.get(1).getId());
        assertEquals("ic_decline", actions.get(1).getIcon());
    }

    @Test
    void inputActionIsMarkedAsTextInput() {
        LocalNotification n = new LocalNotification()
                .addInputAction("reply", "Reply", "Type a message", "Send");
        LocalNotification.Action a = n.getActions().get(0);
        assertTrue(a.isTextInput());
        assertEquals("Type a message", a.getTextInputPlaceholder());
        assertEquals("Send", a.getTextInputButtonText());
    }

    @Test
    void messagingStyleCollectsMessages() {
        LocalNotification n = new LocalNotification();
        LocalNotification.MessagingStyle style = n.asMessagingStyle("Me")
                .conversationTitle("Team")
                .groupConversation(true)
                .addMessage("hi", 1000L, "Alice")
                .addMessage("hello", 2000L, null);
        assertSame(style, n.getMessagingStyle());
        assertEquals("Me", style.getSelfDisplayName());
        assertEquals("Team", style.getConversationTitle());
        assertTrue(style.isGroupConversation());
        assertEquals(2, style.getMessages().size());
        assertEquals("Alice", style.getMessages().get(0).getSenderName());
        assertEquals(2000L, style.getMessages().get(1).getTimestamp());
        assertNull(style.getMessages().get(1).getSenderName());
    }
}
