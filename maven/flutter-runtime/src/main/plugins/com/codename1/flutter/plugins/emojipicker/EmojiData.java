/*
 * Copyright (c) 2026, Codename One and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.  Codename One designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Oracle in the LICENSE file that accompanied this code.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 2 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 *
 * You should have received a copy of the GNU General Public License version
 * 2 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Please contact Codename One through http://www.codenameone.com/ if you
 * need additional information or have any questions.
 */
package com.codename1.flutter.plugins.emojipicker;

import java.util.ArrayList;
import java.util.List;

import com.codename1.io.Preferences;
import com.codename1.ui.Display;

import dart.core.DartList;

/**
 * The built-in emoji set and the recents. Every emoji is treated as supported:
 * there is no portable way to ask a font whether it has a glyph, so
 * checkPlatformCompatibility is not applied.
 */
final class EmojiData {
    private static final String RECENTS = "cn1.emoji_picker.recent";
    private static final int MAX_RECENTS = 28;
    private static DartList<CategoryEmoji> all;

    private static final String[] SMILEYS = {
            "\uD83D\uDE00", "Grinning Face",
            "\uD83D\uDE01", "Grinning Face With Smiling Eyes",
            "\uD83D\uDE02", "Face With Tears Of Joy",
            "\uD83D\uDE03", "Smiling Face With Open Mouth",
            "\uD83D\uDE04", "Smiling Face With Open Mouth And Smiling Eyes",
            "\uD83D\uDE05", "Smiling Face With Open Mouth And Cold Sweat",
            "\uD83D\uDE06", "Smiling Face With Open Mouth And Tightly-closed Eyes",
            "\uD83D\uDE07", "Smiling Face With Halo",
            "\uD83D\uDE08", "Smiling Face With Horns",
            "\uD83D\uDE09", "Winking Face",
            "\uD83D\uDE0A", "Smiling Face With Smiling Eyes",
            "\uD83D\uDE0B", "Face Savouring Delicious Food",
            "\uD83D\uDE0C", "Relieved Face",
            "\uD83D\uDE0D", "Smiling Face With Heart-shaped Eyes",
            "\uD83D\uDE0E", "Smiling Face With Sunglasses",
            "\uD83D\uDE0F", "Smirking Face",
            "\uD83D\uDE10", "Neutral Face",
            "\uD83D\uDE11", "Expressionless Face",
            "\uD83D\uDE12", "Unamused Face",
            "\uD83D\uDE13", "Face With Cold Sweat",
            "\uD83D\uDE14", "Pensive Face",
            "\uD83D\uDE15", "Confused Face",
            "\uD83D\uDE16", "Confounded Face",
            "\uD83D\uDE17", "Kissing Face",
            "\uD83D\uDE18", "Face Throwing A Kiss",
            "\uD83D\uDE19", "Kissing Face With Smiling Eyes",
            "\uD83D\uDE1A", "Kissing Face With Closed Eyes",
            "\uD83D\uDE1B", "Face With Stuck-out Tongue",
            "\uD83D\uDE1C", "Face With Stuck-out Tongue And Winking Eye",
            "\uD83D\uDE1D", "Face With Stuck-out Tongue And Tightly-closed Eyes",
            "\uD83D\uDE1E", "Disappointed Face",
            "\uD83D\uDE1F", "Worried Face",
            "\uD83D\uDE20", "Angry Face",
            "\uD83D\uDE21", "Pouting Face",
            "\uD83D\uDE22", "Crying Face",
            "\uD83D\uDE23", "Persevering Face",
            "\uD83D\uDE24", "Face With Look Of Triumph",
            "\uD83D\uDE25", "Disappointed But Relieved Face",
            "\uD83D\uDE26", "Frowning Face With Open Mouth",
            "\uD83D\uDE27", "Anguished Face",
            "\uD83D\uDE28", "Fearful Face",
            "\uD83D\uDE29", "Weary Face",
            "\uD83D\uDE2A", "Sleepy Face",
            "\uD83D\uDE2B", "Tired Face",
            "\uD83D\uDE2C", "Grimacing Face",
            "\uD83D\uDE2D", "Loudly Crying Face",
            "\uD83D\uDE2E", "Face With Open Mouth",
            "\uD83D\uDE2F", "Hushed Face",
            "\uD83D\uDE30", "Face With Open Mouth And Cold Sweat",
            "\uD83D\uDE31", "Face Screaming In Fear",
            "\uD83D\uDE32", "Astonished Face",
            "\uD83D\uDE33", "Flushed Face",
            "\uD83D\uDE34", "Sleeping Face",
            "\uD83D\uDE35", "Dizzy Face",
            "\uD83D\uDE36", "Face Without Mouth",
            "\uD83D\uDE37", "Face With Medical Mask",
            "\uD83D\uDE38", "Grinning Cat Face With Smiling Eyes",
            "\uD83D\uDE39", "Cat Face With Tears Of Joy",
            "\uD83D\uDE3A", "Smiling Cat Face With Open Mouth",
            "\uD83D\uDE3B", "Smiling Cat Face With Heart-shaped Eyes",
            "\uD83D\uDE3C", "Cat Face With Wry Smile",
            "\uD83D\uDE3D", "Kissing Cat Face With Closed Eyes",
            "\uD83D\uDE3E", "Pouting Cat Face",
            "\uD83D\uDE3F", "Crying Cat Face",
            "\uD83D\uDE40", "Weary Cat Face",
            "\uD83D\uDE41", "Slightly Frowning Face",
            "\uD83D\uDE42", "Slightly Smiling Face",
            "\uD83D\uDE43", "Upside-down Face",
            "\uD83D\uDE44", "Face With Rolling Eyes",
            "\uD83D\uDE45", "Face With No Good Gesture",
            "\uD83D\uDE46", "Face With Ok Gesture",
            "\uD83D\uDE47", "Person Bowing Deeply",
            "\uD83D\uDE48", "See-no-evil Monkey",
            "\uD83D\uDE49", "Hear-no-evil Monkey",
            "\uD83D\uDE4A", "Speak-no-evil Monkey",
            "\uD83D\uDE4B", "Happy Person Raising One Hand",
            "\uD83D\uDE4C", "Person Raising Both Hands In Celebration",
            "\uD83D\uDE4D", "Person Frowning",
            "\uD83D\uDE4E", "Person With Pouting Face",
            "\uD83D\uDE4F", "Person With Folded Hands",
    };

    private static final String[] ANIMALS = {
            "\uD83D\uDC00", "Rat",
            "\uD83D\uDC01", "Mouse",
            "\uD83D\uDC02", "Ox",
            "\uD83D\uDC03", "Water Buffalo",
            "\uD83D\uDC04", "Cow",
            "\uD83D\uDC05", "Tiger",
            "\uD83D\uDC06", "Leopard",
            "\uD83D\uDC07", "Rabbit",
            "\uD83D\uDC08", "Cat",
            "\uD83D\uDC09", "Dragon",
            "\uD83D\uDC0A", "Crocodile",
            "\uD83D\uDC0B", "Whale",
            "\uD83D\uDC0C", "Snail",
            "\uD83D\uDC0D", "Snake",
            "\uD83D\uDC0E", "Horse",
            "\uD83D\uDC0F", "Ram",
            "\uD83D\uDC10", "Goat",
            "\uD83D\uDC11", "Sheep",
            "\uD83D\uDC12", "Monkey",
            "\uD83D\uDC13", "Rooster",
            "\uD83D\uDC14", "Chicken",
            "\uD83D\uDC15", "Dog",
            "\uD83D\uDC16", "Pig",
            "\uD83D\uDC17", "Boar",
            "\uD83D\uDC18", "Elephant",
            "\uD83D\uDC19", "Octopus",
            "\uD83D\uDC1A", "Spiral Shell",
            "\uD83D\uDC1B", "Bug",
            "\uD83D\uDC1C", "Ant",
            "\uD83D\uDC1D", "Honeybee",
            "\uD83D\uDC1E", "Lady Beetle",
            "\uD83D\uDC1F", "Fish",
            "\uD83D\uDC20", "Tropical Fish",
            "\uD83D\uDC21", "Blowfish",
            "\uD83D\uDC22", "Turtle",
            "\uD83D\uDC23", "Hatching Chick",
            "\uD83D\uDC24", "Baby Chick",
            "\uD83D\uDC25", "Front-facing Baby Chick",
            "\uD83D\uDC26", "Bird",
            "\uD83D\uDC27", "Penguin",
            "\uD83D\uDC28", "Koala",
            "\uD83D\uDC29", "Poodle",
            "\uD83D\uDC2A", "Dromedary Camel",
            "\uD83D\uDC2B", "Bactrian Camel",
            "\uD83D\uDC2C", "Dolphin",
            "\uD83D\uDC2D", "Mouse Face",
            "\uD83D\uDC2E", "Cow Face",
            "\uD83D\uDC2F", "Tiger Face",
            "\uD83D\uDC30", "Rabbit Face",
            "\uD83D\uDC31", "Cat Face",
            "\uD83D\uDC32", "Dragon Face",
            "\uD83D\uDC33", "Spouting Whale",
            "\uD83D\uDC34", "Horse Face",
            "\uD83D\uDC35", "Monkey Face",
            "\uD83D\uDC36", "Dog Face",
            "\uD83D\uDC37", "Pig Face",
            "\uD83D\uDC38", "Frog Face",
            "\uD83D\uDC39", "Hamster Face",
            "\uD83D\uDC3A", "Wolf Face",
            "\uD83D\uDC3B", "Bear Face",
            "\uD83D\uDC3C", "Panda Face",
            "\uD83D\uDC3D", "Pig Nose",
            "\uD83D\uDC3E", "Paw Prints",
    };

    private static final String[] FOODS = {
            "\uD83C\uDF45", "Tomato",
            "\uD83C\uDF46", "Aubergine",
            "\uD83C\uDF47", "Grapes",
            "\uD83C\uDF48", "Melon",
            "\uD83C\uDF49", "Watermelon",
            "\uD83C\uDF4A", "Tangerine",
            "\uD83C\uDF4B", "Lemon",
            "\uD83C\uDF4C", "Banana",
            "\uD83C\uDF4D", "Pineapple",
            "\uD83C\uDF4E", "Red Apple",
            "\uD83C\uDF4F", "Green Apple",
            "\uD83C\uDF50", "Pear",
            "\uD83C\uDF51", "Peach",
            "\uD83C\uDF52", "Cherries",
            "\uD83C\uDF53", "Strawberry",
            "\uD83C\uDF54", "Hamburger",
            "\uD83C\uDF55", "Slice Of Pizza",
            "\uD83C\uDF56", "Meat On Bone",
            "\uD83C\uDF57", "Poultry Leg",
            "\uD83C\uDF58", "Rice Cracker",
            "\uD83C\uDF59", "Rice Ball",
            "\uD83C\uDF5A", "Cooked Rice",
            "\uD83C\uDF5B", "Curry And Rice",
            "\uD83C\uDF5C", "Steaming Bowl",
            "\uD83C\uDF5D", "Spaghetti",
            "\uD83C\uDF5E", "Bread",
            "\uD83C\uDF5F", "French Fries",
            "\uD83C\uDF60", "Roasted Sweet Potato",
            "\uD83C\uDF61", "Dango",
            "\uD83C\uDF62", "Oden",
            "\uD83C\uDF63", "Sushi",
            "\uD83C\uDF64", "Fried Shrimp",
            "\uD83C\uDF65", "Fish Cake With Swirl Design",
            "\uD83C\uDF66", "Soft Ice Cream",
            "\uD83C\uDF67", "Shaved Ice",
            "\uD83C\uDF68", "Ice Cream",
            "\uD83C\uDF69", "Doughnut",
            "\uD83C\uDF6A", "Cookie",
            "\uD83C\uDF6B", "Chocolate Bar",
            "\uD83C\uDF6C", "Candy",
            "\uD83C\uDF6D", "Lollipop",
            "\uD83C\uDF6E", "Custard",
            "\uD83C\uDF6F", "Honey Pot",
            "\uD83C\uDF70", "Shortcake",
            "\uD83C\uDF71", "Bento Box",
            "\uD83C\uDF72", "Pot Of Food",
            "\uD83C\uDF73", "Cooking",
            "\uD83C\uDF74", "Fork And Knife",
            "\uD83C\uDF75", "Teacup Without Handle",
            "\uD83C\uDF76", "Sake Bottle And Cup",
            "\uD83C\uDF77", "Wine Glass",
            "\uD83C\uDF78", "Cocktail Glass",
            "\uD83C\uDF79", "Tropical Drink",
            "\uD83C\uDF7A", "Beer Mug",
            "\uD83C\uDF7B", "Clinking Beer Mugs",
            "\uD83C\uDF7C", "Baby Bottle",
            "\uD83C\uDF7D", "Fork And Knife With Plate",
            "\uD83C\uDF7E", "Bottle With Popping Cork",
            "\uD83C\uDF7F", "Popcorn",
    };

    private static final String[] ACTIVITIES = {
            "\uD83C\uDF80", "Ribbon",
            "\uD83C\uDF81", "Wrapped Present",
            "\uD83C\uDF82", "Birthday Cake",
            "\uD83C\uDF83", "Jack-o-lantern",
            "\uD83C\uDF84", "Christmas Tree",
            "\uD83C\uDF85", "Father Christmas",
            "\uD83C\uDF86", "Fireworks",
            "\uD83C\uDF87", "Firework Sparkler",
            "\uD83C\uDF88", "Balloon",
            "\uD83C\uDF89", "Party Popper",
            "\uD83C\uDF8A", "Confetti Ball",
            "\uD83C\uDF8B", "Tanabata Tree",
            "\uD83C\uDF8C", "Crossed Flags",
            "\uD83C\uDF8D", "Pine Decoration",
            "\uD83C\uDF8E", "Japanese Dolls",
            "\uD83C\uDF8F", "Carp Streamer",
            "\uD83C\uDF90", "Wind Chime",
            "\uD83C\uDF91", "Moon Viewing Ceremony",
            "\uD83C\uDF92", "School Satchel",
            "\uD83C\uDF93", "Graduation Cap",
            "\uD83C\uDFA0", "Carousel Horse",
            "\uD83C\uDFA1", "Ferris Wheel",
            "\uD83C\uDFA2", "Roller Coaster",
            "\uD83C\uDFA3", "Fishing Pole And Fish",
            "\uD83C\uDFA4", "Microphone",
            "\uD83C\uDFA5", "Movie Camera",
            "\uD83C\uDFA6", "Cinema",
            "\uD83C\uDFA7", "Headphone",
            "\uD83C\uDFA8", "Artist Palette",
            "\uD83C\uDFA9", "Top Hat",
            "\uD83C\uDFAA", "Circus Tent",
            "\uD83C\uDFAB", "Ticket",
            "\uD83C\uDFAC", "Clapper Board",
            "\uD83C\uDFAD", "Performing Arts",
            "\uD83C\uDFAE", "Video Game",
            "\uD83C\uDFAF", "Direct Hit",
            "\uD83C\uDFB0", "Slot Machine",
            "\uD83C\uDFB1", "Billiards",
            "\uD83C\uDFB2", "Game Die",
            "\uD83C\uDFB3", "Bowling",
            "\uD83C\uDFB4", "Flower Playing Cards",
            "\uD83C\uDFB5", "Musical Note",
            "\uD83C\uDFB6", "Multiple Musical Notes",
            "\uD83C\uDFB7", "Saxophone",
            "\uD83C\uDFB8", "Guitar",
            "\uD83C\uDFB9", "Musical Keyboard",
            "\uD83C\uDFBA", "Trumpet",
            "\uD83C\uDFBB", "Violin",
            "\uD83C\uDFBC", "Musical Score",
            "\uD83C\uDFBD", "Running Shirt With Sash",
            "\uD83C\uDFBE", "Tennis Racquet And Ball",
            "\uD83C\uDFBF", "Ski And Ski Boot",
            "\uD83C\uDFC0", "Basketball And Hoop",
            "\uD83C\uDFC1", "Chequered Flag",
            "\uD83C\uDFC2", "Snowboarder",
            "\uD83C\uDFC3", "Runner",
            "\uD83C\uDFC4", "Surfer",
            "\uD83C\uDFC5", "Sports Medal",
            "\uD83C\uDFC6", "Trophy",
            "\uD83C\uDFC7", "Horse Racing",
            "\uD83C\uDFC8", "American Football",
            "\uD83C\uDFC9", "Rugby Football",
            "\uD83C\uDFCA", "Swimmer",
    };

    private static final String[] TRAVEL = {
            "\uD83D\uDE80", "Rocket",
            "\uD83D\uDE81", "Helicopter",
            "\uD83D\uDE82", "Steam Locomotive",
            "\uD83D\uDE83", "Railway Car",
            "\uD83D\uDE84", "High-speed Train",
            "\uD83D\uDE85", "High-speed Train With Bullet Nose",
            "\uD83D\uDE86", "Train",
            "\uD83D\uDE87", "Metro",
            "\uD83D\uDE88", "Light Rail",
            "\uD83D\uDE89", "Station",
            "\uD83D\uDE8A", "Tram",
            "\uD83D\uDE8B", "Tram Car",
            "\uD83D\uDE8C", "Bus",
            "\uD83D\uDE8D", "Oncoming Bus",
            "\uD83D\uDE8E", "Trolleybus",
            "\uD83D\uDE8F", "Bus Stop",
            "\uD83D\uDE90", "Minibus",
            "\uD83D\uDE91", "Ambulance",
            "\uD83D\uDE92", "Fire Engine",
            "\uD83D\uDE93", "Police Car",
            "\uD83D\uDE94", "Oncoming Police Car",
            "\uD83D\uDE95", "Taxi",
            "\uD83D\uDE96", "Oncoming Taxi",
            "\uD83D\uDE97", "Automobile",
            "\uD83D\uDE98", "Oncoming Automobile",
            "\uD83D\uDE99", "Recreational Vehicle",
            "\uD83D\uDE9A", "Delivery Truck",
            "\uD83D\uDE9B", "Articulated Lorry",
            "\uD83D\uDE9C", "Tractor",
            "\uD83D\uDE9D", "Monorail",
            "\uD83D\uDE9E", "Mountain Railway",
            "\uD83D\uDE9F", "Suspension Railway",
            "\uD83D\uDEA0", "Mountain Cableway",
            "\uD83D\uDEA1", "Aerial Tramway",
            "\uD83D\uDEA2", "Ship",
            "\uD83D\uDEA3", "Rowboat",
            "\uD83D\uDEA4", "Speedboat",
            "\uD83D\uDEA5", "Horizontal Traffic Light",
            "\uD83D\uDEA6", "Vertical Traffic Light",
            "\uD83D\uDEA7", "Construction Sign",
            "\uD83D\uDEA8", "Police Cars Revolving Light",
            "\uD83D\uDEA9", "Triangular Flag On Post",
            "\uD83D\uDEAA", "Door",
            "\uD83D\uDEAB", "No Entry Sign",
            "\uD83D\uDEAC", "Smoking Symbol",
            "\uD83D\uDEAD", "No Smoking Symbol",
            "\uD83D\uDEAE", "Put Litter In Its Place Symbol",
            "\uD83D\uDEAF", "Do Not Litter Symbol",
            "\uD83D\uDEB0", "Potable Water Symbol",
            "\uD83D\uDEB1", "Non-potable Water Symbol",
            "\uD83D\uDEB2", "Bicycle",
            "\uD83D\uDEB3", "No Bicycles",
            "\uD83D\uDEB4", "Bicyclist",
            "\uD83D\uDEB5", "Mountain Bicyclist",
            "\uD83D\uDEB6", "Pedestrian",
            "\uD83D\uDEB7", "No Pedestrians",
            "\uD83D\uDEB8", "Children Crossing",
            "\uD83D\uDEB9", "Mens Symbol",
            "\uD83D\uDEBA", "Womens Symbol",
            "\uD83D\uDEBB", "Restroom",
            "\uD83D\uDEBC", "Baby Symbol",
            "\uD83D\uDEBD", "Toilet",
            "\uD83D\uDEBE", "Water Closet",
            "\uD83D\uDEBF", "Shower",
            "\uD83D\uDEC0", "Bath",
            "\uD83D\uDEC1", "Bathtub",
            "\uD83D\uDEC2", "Passport Control",
            "\uD83D\uDEC3", "Customs",
            "\uD83D\uDEC4", "Baggage Claim",
            "\uD83D\uDEC5", "Left Luggage",
    };

    private static final String[] OBJECTS = {
            "\uD83D\uDCA0", "Diamond Shape With A Dot Inside",
            "\uD83D\uDCA1", "Electric Light Bulb",
            "\uD83D\uDCA2", "Anger Symbol",
            "\uD83D\uDCA3", "Bomb",
            "\uD83D\uDCA4", "Sleeping Symbol",
            "\uD83D\uDCA5", "Collision Symbol",
            "\uD83D\uDCA6", "Splashing Sweat Symbol",
            "\uD83D\uDCA7", "Droplet",
            "\uD83D\uDCA8", "Dash Symbol",
            "\uD83D\uDCA9", "Pile Of Poo",
            "\uD83D\uDCAA", "Flexed Biceps",
            "\uD83D\uDCAB", "Dizzy Symbol",
            "\uD83D\uDCAC", "Speech Balloon",
            "\uD83D\uDCAD", "Thought Balloon",
            "\uD83D\uDCAE", "White Flower",
            "\uD83D\uDCAF", "Hundred Points Symbol",
            "\uD83D\uDCB0", "Money Bag",
            "\uD83D\uDCB1", "Currency Exchange",
            "\uD83D\uDCB2", "Heavy Dollar Sign",
            "\uD83D\uDCB3", "Credit Card",
            "\uD83D\uDCB4", "Banknote With Yen Sign",
            "\uD83D\uDCB5", "Banknote With Dollar Sign",
            "\uD83D\uDCB6", "Banknote With Euro Sign",
            "\uD83D\uDCB7", "Banknote With Pound Sign",
            "\uD83D\uDCB8", "Money With Wings",
            "\uD83D\uDCB9", "Chart With Upwards Trend And Yen Sign",
            "\uD83D\uDCBA", "Seat",
            "\uD83D\uDCBB", "Personal Computer",
            "\uD83D\uDCBC", "Briefcase",
            "\uD83D\uDCBD", "Minidisc",
            "\uD83D\uDCBE", "Floppy Disk",
            "\uD83D\uDCBF", "Optical Disc",
            "\uD83D\uDCC0", "Dvd",
            "\uD83D\uDCC1", "File Folder",
            "\uD83D\uDCC2", "Open File Folder",
            "\uD83D\uDCC3", "Page With Curl",
            "\uD83D\uDCC4", "Page Facing Up",
            "\uD83D\uDCC5", "Calendar",
            "\uD83D\uDCC6", "Tear-off Calendar",
            "\uD83D\uDCC7", "Card Index",
            "\uD83D\uDCC8", "Chart With Upwards Trend",
            "\uD83D\uDCC9", "Chart With Downwards Trend",
            "\uD83D\uDCCA", "Bar Chart",
            "\uD83D\uDCCB", "Clipboard",
            "\uD83D\uDCCC", "Pushpin",
            "\uD83D\uDCCD", "Round Pushpin",
            "\uD83D\uDCCE", "Paperclip",
            "\uD83D\uDCCF", "Straight Ruler",
            "\uD83D\uDCD0", "Triangular Ruler",
            "\uD83D\uDCD1", "Bookmark Tabs",
            "\uD83D\uDCD2", "Ledger",
            "\uD83D\uDCD3", "Notebook",
            "\uD83D\uDCD4", "Notebook With Decorative Cover",
            "\uD83D\uDCD5", "Closed Book",
            "\uD83D\uDCD6", "Open Book",
            "\uD83D\uDCD7", "Green Book",
            "\uD83D\uDCD8", "Blue Book",
            "\uD83D\uDCD9", "Orange Book",
            "\uD83D\uDCDA", "Books",
            "\uD83D\uDCDB", "Name Badge",
            "\uD83D\uDCDC", "Scroll",
            "\uD83D\uDCDD", "Memo",
            "\uD83D\uDCDE", "Telephone Receiver",
            "\uD83D\uDCDF", "Pager",
            "\uD83D\uDCE0", "Fax Machine",
            "\uD83D\uDCE1", "Satellite Antenna",
            "\uD83D\uDCE2", "Public Address Loudspeaker",
            "\uD83D\uDCE3", "Cheering Megaphone",
            "\uD83D\uDCE4", "Outbox Tray",
            "\uD83D\uDCE5", "Inbox Tray",
            "\uD83D\uDCE6", "Package",
            "\uD83D\uDCE7", "E-mail Symbol",
            "\uD83D\uDCE8", "Incoming Envelope",
            "\uD83D\uDCE9", "Envelope With Downwards Arrow Above",
            "\uD83D\uDCEA", "Closed Mailbox With Lowered Flag",
            "\uD83D\uDCEB", "Closed Mailbox With Raised Flag",
            "\uD83D\uDCEC", "Open Mailbox With Raised Flag",
            "\uD83D\uDCED", "Open Mailbox With Lowered Flag",
            "\uD83D\uDCEE", "Postbox",
            "\uD83D\uDCEF", "Postal Horn",
            "\uD83D\uDCF0", "Newspaper",
            "\uD83D\uDCF1", "Mobile Phone",
            "\uD83D\uDCF2", "Mobile Phone With Rightwards Arrow At Left",
            "\uD83D\uDCF3", "Vibration Mode",
            "\uD83D\uDCF4", "Mobile Phone Off",
            "\uD83D\uDCF5", "No Mobile Phones",
            "\uD83D\uDCF6", "Antenna With Bars",
            "\uD83D\uDCF7", "Camera",
            "\uD83D\uDCF8", "Camera With Flash",
            "\uD83D\uDCF9", "Video Camera",
            "\uD83D\uDCFA", "Television",
            "\uD83D\uDCFB", "Radio",
            "\uD83D\uDCFC", "Videocassette",
    };

    private static final String[] SYMBOLS = {
            "\u2764", "Heavy Black Heart",
            "\uD83D\uDC93", "Beating Heart",
            "\uD83D\uDC94", "Broken Heart",
            "\uD83D\uDC95", "Two Hearts",
            "\uD83D\uDC96", "Sparkling Heart",
            "\uD83D\uDC97", "Growing Heart",
            "\uD83D\uDC98", "Heart With Arrow",
            "\uD83D\uDC99", "Blue Heart",
            "\uD83D\uDC9A", "Green Heart",
            "\uD83D\uDC9B", "Yellow Heart",
            "\uD83D\uDC9C", "Purple Heart",
            "\uD83D\uDC9D", "Heart With Ribbon",
            "\uD83D\uDC9E", "Revolving Hearts",
            "\uD83D\uDC9F", "Heart Decoration",
            "\u2648", "Aries",
            "\u2649", "Taurus",
            "\u264A", "Gemini",
            "\u264B", "Cancer",
            "\u264C", "Leo",
            "\u264D", "Virgo",
            "\u264E", "Libra",
            "\u264F", "Scorpius",
            "\u2650", "Sagittarius",
            "\u2651", "Capricorn",
            "\u2652", "Aquarius",
            "\u2653", "Pisces",
    };

    private static final String[] FLAGS = {
            "\uD83C\uDDFA\uD83C\uDDF8", "Flag US",
            "\uD83C\uDDEC\uD83C\uDDE7", "Flag GB",
            "\uD83C\uDDE8\uD83C\uDDE6", "Flag CA",
            "\uD83C\uDDE6\uD83C\uDDFA", "Flag AU",
            "\uD83C\uDDEB\uD83C\uDDF7", "Flag FR",
            "\uD83C\uDDE9\uD83C\uDDEA", "Flag DE",
            "\uD83C\uDDEE\uD83C\uDDF9", "Flag IT",
            "\uD83C\uDDEA\uD83C\uDDF8", "Flag ES",
            "\uD83C\uDDF5\uD83C\uDDF9", "Flag PT",
            "\uD83C\uDDF3\uD83C\uDDF1", "Flag NL",
            "\uD83C\uDDF8\uD83C\uDDEA", "Flag SE",
            "\uD83C\uDDF3\uD83C\uDDF4", "Flag NO",
            "\uD83C\uDDE9\uD83C\uDDF0", "Flag DK",
            "\uD83C\uDDEB\uD83C\uDDEE", "Flag FI",
            "\uD83C\uDDEE\uD83C\uDDEA", "Flag IE",
            "\uD83C\uDDEE\uD83C\uDDF1", "Flag IL",
            "\uD83C\uDDEF\uD83C\uDDF5", "Flag JP",
            "\uD83C\uDDF0\uD83C\uDDF7", "Flag KR",
            "\uD83C\uDDE8\uD83C\uDDF3", "Flag CN",
            "\uD83C\uDDEE\uD83C\uDDF3", "Flag IN",
            "\uD83C\uDDE7\uD83C\uDDF7", "Flag BR",
            "\uD83C\uDDF2\uD83C\uDDFD", "Flag MX",
            "\uD83C\uDDE6\uD83C\uDDF7", "Flag AR",
            "\uD83C\uDDFF\uD83C\uDDE6", "Flag ZA",
            "\uD83C\uDDF3\uD83C\uDDEC", "Flag NG",
            "\uD83C\uDDEA\uD83C\uDDEC", "Flag EG",
            "\uD83C\uDDF9\uD83C\uDDF7", "Flag TR",
            "\uD83C\uDDEC\uD83C\uDDF7", "Flag GR",
            "\uD83C\uDDF5\uD83C\uDDF1", "Flag PL",
            "\uD83C\uDDFA\uD83C\uDDE6", "Flag UA",
    };

    private EmojiData() {
    }

    static DartList<CategoryEmoji> all() {
        if (all == null) {
            all = new DartList<CategoryEmoji>();
            add(Category.SMILEYS, SMILEYS);
            add(Category.ANIMALS, ANIMALS);
            add(Category.FOODS, FOODS);
            add(Category.ACTIVITIES, ACTIVITIES);
            add(Category.TRAVEL, TRAVEL);
            add(Category.OBJECTS, OBJECTS);
            add(Category.SYMBOLS, SYMBOLS);
            add(Category.FLAGS, FLAGS);
        }
        return all;
    }

    private static void add(Category c, String[] pairs) {
        DartList<Emoji> list = new DartList<Emoji>();
        for (int i = 0; i + 1 < pairs.length; i += 2) {
            list.add(new Emoji(pairs[i], pairs[i + 1]));
        }
        all.add(new CategoryEmoji(c, list));
    }

    static Emoji find(String e) {
        for (CategoryEmoji c : all()) {
            for (Emoji x : c.emoji()) {
                if (x.emoji().equals(e)) {
                    return x;
                }
            }
        }
        return null;
    }

    /** Recents, newest first; empty without a display (Preferences needs storage). */
    static DartList<Emoji> recents() {
        DartList<Emoji> out = new DartList<Emoji>();
        if (!Display.isInitialized()) {
            return out;
        }
        String s = Preferences.get(RECENTS, "");
        int start = 0;
        while (start < s.length()) {
            int end = s.indexOf('\n', start);
            if (end < 0) {
                end = s.length();
            }
            Emoji e = find(s.substring(start, end));
            if (e != null) {
                out.add(e);
            }
            start = end + 1;
        }
        return out;
    }

    static void remember(Emoji e) {
        if (!Display.isInitialized()) {
            return;
        }
        List<String> keep = new ArrayList<String>();
        keep.add(e.emoji());
        for (Emoji r : recents()) {
            if (!r.emoji().equals(e.emoji()) && keep.size() < MAX_RECENTS) {
                keep.add(r.emoji());
            }
        }
        StringBuilder b = new StringBuilder();
        for (String k : keep) {
            if (b.length() > 0) {
                b.append('\n');
            }
            b.append(k);
        }
        Preferences.set(RECENTS, b.toString());
    }

    static void clearRecents() {
        if (Display.isInitialized()) {
            Preferences.delete(RECENTS);
        }
    }
}
