---
title: "Splat: A Team Paintball Shooter Template in Java, Refereed by Its Own Server"
slug: build-a-paintball-shooter-in-java
url: /blog/build-a-paintball-shooter-in-java/
date: '2026-11-05'
author: Shai Almog
draft: true
description: "Splat is a full-stack, first-person team paintball shooter in Java: 3D matches four against four, a server that referees every match while the phone predicts, lag-compensated shots, parties with voice chat, replays, a shop and an admin console. Generate it from the Initializr and make it your own, royalty free."
---

Skirmish was a card battler and Dash a runner. Splat is the third game template, and it is the kind of game people assume needs a commercial engine: a first-person team shooter. Four against four, in 3D, online. The twist is that it's paintball, and it never stops making fun of itself. Every match opens with a mission briefing read in the gravest voice available ("Operation: Borrowed Bedsheet"). Every hit lands a big red **PAINTED** stamp across your goggles. A player who is out can try to wipe the paint off, and the referee catches them two times in three. You can generate it from the [Initializr](/initializr/) today, under your own name and package, and the source is MIT-0.

## The server is the referee

In a shooter you can't trust the phone. Splat's server runs every online match: each phone sends only its inputs (move, look, fire) and the server steps the match and sends a snapshot back every other tick. A modified client can send whatever it likes, but the server's rules decide what its marker does.

Waiting for the server would make the marker feel like it fires through mud, so the phone doesn't wait. It runs the same rules module, applies its own input at once, and when a snapshot arrives it rewinds to the server's state and replays the inputs the server hasn't seen yet. When the guess was right, nothing moves. When someone else's ball got there first, the correction is one snapshot old.

## Shots that land where you aimed

Paintballs in Splat are real projectiles with an arc and a drop, not instant hits. That keeps the server honest, but it means a player on a slow connection would always aim at where people *were*. Every input carries the tick the player was looking at, and the server flies the ball forward the ticks it lost on the way. The catch-up is capped at a fifth of a second, so a very slow connection can't paint people around corners.

## One rules module, three ways to play

The rules are a plain Java module in fixed-point integers, with no floating point and no dependencies. The phone predicts with it, the server runs it as the authority, and the replay viewer runs it again from the seed and the human inputs. The bots think inside the rules too, which is why a replay doesn't need to record them: they make the same choices again. The same code runs a practice match on a plane, a quick match the server fills with bots, and a party of friends talking over WebRTC voice.

## Make it your own

A field is a table of boxes, mirrored so both sides are fair. A marker is six numbers. The briefings, the killfeed excuses and the emotes are strings in one place each. Change a field and the mission list, the server's playlist and the bots all pick it up. Around the game is everything a live game needs and every template here shares: accounts, a shop with server-checked receipts, rewarded ads, pushes, crews, quests, seasons and an operations console with flags for clients that send too many inputs or turn faster than a wrist can.

The [Splat guide](/developer-guide/splat-implementors-guide/) walks through each part, and the [Splat page](/splat/) has the screenshots.
