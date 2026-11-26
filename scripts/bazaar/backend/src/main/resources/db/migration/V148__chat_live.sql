-- The floating storefront chat: the signed-in customer a conversation follows,
-- and how far each side has read it (the widget's delivered/read ticks).
ALTER TABLE chat_conversation ADD COLUMN customer VARCHAR(200) NOT NULL DEFAULT '';
ALTER TABLE chat_conversation ADD COLUMN visitor_read BIGINT NOT NULL DEFAULT 0;
ALTER TABLE chat_conversation ADD COLUMN staff_read BIGINT NOT NULL DEFAULT 0;
