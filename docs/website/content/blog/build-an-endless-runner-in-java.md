---
title: "Dash: An Endless Runner Template in Java, With Races and a Level Editor"
slug: build-an-endless-runner-in-java
url: /blog/build-an-endless-runner-in-java/
date: '2026-10-30'
author: Shai Almog
draft: true
description: "Dash is a full-stack endless runner in Java: levels that turn from 2D into 3D mid-run, leaderboards the server re-simulates, live races between friends with voice chat, a track editor, crews, in-app purchases and an admin console. Generate it from the Initializr and make it your own, royalty free."
---

Skirmish showed that most of a mobile game is app: the sign-in, the menus, the shop, the friends list, the pushes and the server. Dash is the second game template, and it is a different kind of game: an endless runner. A runner dashes down a track of lanes, jumps hurdles, slides under barriers and changes lanes around traffic. You can generate it from the [Initializr](/initializr/) today, under your own name and package, and the source is MIT-0.

## A level that turns into 3D

The trick I like most in Dash is that a level can start as a side-scroller and become a 3D runner halfway through. The first stretch of *Neon Causeway* looks like a flat platformer: one row of hurdles and barriers, seen from the side. At 300 metres the camera swings behind the runner, the lanes open up, and the rest is a chase-camera runner.

There is no second game behind that. The simulation always has lanes, heights and distances, and a hurdle is cleared the same way from either angle. A level only carries *camera cues*, and the renderer blends between the two framings when the runner reaches one. Because the rules never see the camera, a level can change its view anywhere without changing who wins.

## Leaderboards you can trust

A run is never reported as a number. The app sends the seed and the input log, a few hundred bytes, and the server plays it through the same Java rules module the app runs. A typed-in score, or a log edited to dodge an obstacle the player hit, simply does not reproduce. The rules use no floating point and no clock, so the phone, the server and the replay viewer agree to the bit.

Every level has all-time, weekly, daily, friends and crew boards. Beat a friend's best and they get a push that opens that level's board.

## Racing your friends

Open a party, share the link, and everyone runs the same track at the same moment. The server runs the race in lockstep: each phone sends its inputs, the room broadcasts each tick's inputs to every seat, and its own copy of the race decides the places. A client whose state drifts from the server's is caught by a hash it sends every second.

While you race you can talk. Voice uses Codename One's WebRTC API, peer to peer between each pair of players, with the game's own socket carrying the signalling. The server never touches the audio.

## Build your own levels

The track editor is in the app, on every platform. A level is a strip you paint on: tap a lane to drop a hurdle or a coin, tap the top row for a barrier across every lane, and drop camera cues where the view should change. *Playtest* runs the draft on the real rules. Publishing requires the bot to finish the level, so nobody can publish one that cannot be beaten. Published packs can be rated, raced, remixed and moderated from the admin console.

## Get it

Choose **Dash** in the [Initializr](/initializr/), or read the [guide chapter](/developer-guide/dash-implementors-guide/) first. It walks through the rules, the camera, the referee, races, voice and the editor, and what to change to make the game yours.
