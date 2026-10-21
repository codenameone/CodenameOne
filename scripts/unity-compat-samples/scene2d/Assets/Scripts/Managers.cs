/*
 * Copyright (c) 2026, Codename One and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
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
using UnityEngine;

// The singleton base class a Unity project usually has one of, and two
// managers made from it. `Singleton<GameManager>` and
// `Singleton<AudioManager>` are two types in C#, each with its own
// `instance`: the game manager must never be handed the audio manager.
public abstract class Singleton<T> : MonoBehaviour where T : MonoBehaviour
{
    private static T instance;
    public static int Awakened;

    public static T Instance
    {
        get
        {
            if (instance == null)
            {
                instance = FindObjectOfType<T>();
            }
            return instance;
        }
    }

    protected virtual void Awake()
    {
        Awakened++;
        if (instance == null)
        {
            instance = this as T;
        }
        else
        {
            Destroy(gameObject);
        }
    }
}

public class GameManager : Singleton<GameManager>
{
    public int score = 3;
}

public class AudioManager : Singleton<AudioManager>
{
    public string clip = "beep";

    protected override void Awake()
    {
        base.Awake();
        clip += "!";
    }
}

// Looks an object up once and remembers it, per type: the `T` of
// `FindObjectOfType<T>()` is the type argument of the class it is called in.
public static class Locator<T> where T : Object
{
    private static T found;
    public static int Searches;

    public static T Get()
    {
        if (found == null)
        {
            Searches++;
            found = Object.FindObjectOfType<T>();
        }
        return found;
    }
}
