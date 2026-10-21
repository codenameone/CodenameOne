-- Levels, daily quests, chests and achievements. Portable, like V1: counters
-- are INTEGER, a yes/no is a SMALLINT 0/1 and a day is epoch milliseconds.
CREATE TABLE progress (
    username VARCHAR(190) NOT NULL PRIMARY KEY,
    xp INTEGER NOT NULL,
    tutorial_done SMALLINT NOT NULL,
    chests INTEGER NOT NULL,
    chests_opened INTEGER NOT NULL,
    matches INTEGER NOT NULL,
    wins INTEGER NOT NULL,
    cards INTEGER NOT NULL,
    spells INTEGER NOT NULL,
    towers INTEGER NOT NULL,
    streak INTEGER NOT NULL,
    best_streak INTEGER NOT NULL,
    achievements VARCHAR(500) NOT NULL,
    quest_day BIGINT NOT NULL,
    quest0 INTEGER NOT NULL,
    quest1 INTEGER NOT NULL,
    quest2 INTEGER NOT NULL,
    claimed INTEGER NOT NULL
);
