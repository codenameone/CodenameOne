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

/*
 * Steamworks (com.codename1.gaming.steam.Steam), loaded at run time.
 *
 * libsteam_api.so is dlopen()ed beside the executable and called through the
 * Steamworks *flat* API, which is plain C ABI (S_API is extern "C" and every
 * interface is an opaque pointer). That choice is what keeps this port pure C
 * and keeps every Steamworks file out of this repository:
 *  - no SDK header is compiled here; the dozen entry points used are declared
 *    below from the published flat API, the way every binding outside C++ does;
 *  - the same binary runs with and without Steam: no library, or SteamAPI init
 *    failing (not launched by Steam, no steam_appid.txt), is "unavailable" and
 *    every call is a harmless no-op;
 *  - the Steam build target only has to put the redistributable
 *    libsteam_api.so in the depot (the cloud builder injects it).
 *
 * Interface accessors are versioned (SteamAPI_SteamUserStats_v013 ...) and the
 * version moves with SDK releases, so the newest one the loaded library exports
 * is picked: the unversioned flat methods are always compiled against that
 * library's newest interface.
 *
 * Callbacks use manual dispatch, pumped by LinuxSteam's thread through
 * steamPump(); the only ones decoded are the overlay opening and closing and
 * the floating keyboard being dismissed. Call results are freed unread.
 */

#ifndef _GNU_SOURCE
#define _GNU_SOURCE
#endif
#include "cn1_linux.h"
#include <dlfcn.h>
#include <limits.h>
#include <pthread.h>
#include <stdint.h>
#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <unistd.h>

extern JAVA_OBJECT newStringFromCString(CODENAME_ONE_THREAD_STATE, const char* str);
extern const char* stringToUTF8(CODENAME_ONE_THREAD_STATE, JAVA_OBJECT str);
extern JAVA_OBJECT cn1LinuxNewByteArray(CODENAME_ONE_THREAD_STATE, const void* src, int n);

typedef unsigned char sbool;   /* C++ bool across the flat ABI */
typedef void* SIface;

typedef struct {
    int32_t user;
    int32_t callback;
    uint8_t* param;
    int32_t paramSize;
} SteamCallbackMsg;

/* Callback ids: k_iSteamFriendsCallbacks + 31, k_iSteamUtilsCallbacks + 38. */
#define CB_GAME_OVERLAY_ACTIVATED 331
#define CB_FLOATING_KEYBOARD_DISMISSED 738

/* Events steamPump() answers; mirrored in LinuxSteam. */
#define EV_OVERLAY_SHOWN 1
#define EV_OVERLAY_HIDDEN 2
#define EV_KEYBOARD_DISMISSED 3

static void* lib;
static int state;                   /* 0 untried, 1 ready, -1 unavailable */
static int32_t pipe_;
static SIface stats, friends, user, utils, apps, storage;
static pthread_mutex_t steamLock = PTHREAD_MUTEX_INITIALIZER;

static int (*pInitFlat)(char* errMsg);
static sbool (*pInit)(void);
static void (*pShutdown)(void);
static sbool (*pRestartAppIfNecessary)(uint32_t);
static void (*pManualInit)(void);
static int32_t (*pGetPipe)(void);
static void (*pRunFrame)(int32_t);
static sbool (*pNextCallback)(int32_t, SteamCallbackMsg*);
static void (*pFreeLastCallback)(int32_t);

static sbool (*pSetAchievement)(SIface, const char*);
static sbool (*pClearAchievement)(SIface, const char*);
static sbool (*pGetAchievement)(SIface, const char*, sbool*);
static sbool (*pStoreStats)(SIface);
static sbool (*pRequestCurrentStats)(SIface);
static sbool (*pGetStatInt)(SIface, const char*, int32_t*);
static sbool (*pSetStatInt)(SIface, const char*, int32_t);
static sbool (*pGetStatFloat)(SIface, const char*, float*);
static sbool (*pSetStatFloat)(SIface, const char*, float);
static const char* (*pPersonaName)(SIface);
static void (*pActivateOverlay)(SIface, const char*);
static void (*pActivateOverlayWeb)(SIface, const char*, int);
static sbool (*pSetRichPresence)(SIface, const char*, const char*);
static void (*pClearRichPresence)(SIface);
static uint64_t (*pSteamId)(SIface);
static uint32_t (*pAppId)(SIface);
static sbool (*pOnDeck)(SIface);
static sbool (*pBigPicture)(SIface);
static sbool (*pFloatingKeyboard)(SIface, int, int, int, int, int);
static sbool (*pDlcInstalled)(SIface, uint32_t);
static const char* (*pLanguage)(SIface);
static sbool (*pFileWrite)(SIface, const char*, const void*, int32_t);
static int32_t (*pFileRead)(SIface, const char*, void*, int32_t);
static int32_t (*pFileSize)(SIface, const char*);
static sbool (*pFileExists)(SIface, const char*);
static sbool (*pFileDelete)(SIface, const char*);
static sbool (*pCloudAccount)(SIface);
static sbool (*pCloudApp)(SIface);

#define SYM(var, name) (*(void**) (&(var)) = dlsym(lib, name))

/* The newest exported "SteamAPI_<base>_vNNN" accessor, called. */
static SIface steamAccessor(const char* base) {
    char name[96];
    int v;
    for (v = 40; v > 0; v--) {
        SIface (*fn)(void);
        snprintf(name, sizeof(name), "SteamAPI_%s_v%03d", base, v);
        *(void**) (&fn) = dlsym(lib, name);
        if (fn != NULL) {
            return fn();
        }
    }
    return NULL;
}

static void* steamOpen(void) {
    char exe[PATH_MAX];
    ssize_t n = readlink("/proc/self/exe", exe, sizeof(exe) - 32);
    void* h;
    if (n > 0) {
        char* slash;
        exe[n] = 0;
        slash = strrchr(exe, '/');
        if (slash != NULL) {
            strcpy(slash + 1, "libsteam_api.so");
            h = dlopen(exe, RTLD_NOW | RTLD_LOCAL);
            if (h != NULL) {
                return h;
            }
        }
    }
    return dlopen("libsteam_api.so", RTLD_NOW | RTLD_LOCAL);
}

static int steamLoadLocked(void) {
    char err[1024];
    int ok;
    if (state != 0) {
        return state == 1;
    }
    state = -1;
    if (lib == NULL) {
        lib = steamOpen();
    }
    if (lib == NULL) {
        return 0;
    }
    SYM(pInitFlat, "SteamAPI_InitFlat");
    SYM(pInit, "SteamAPI_Init");
    SYM(pShutdown, "SteamAPI_Shutdown");
    SYM(pRestartAppIfNecessary, "SteamAPI_RestartAppIfNecessary");
    SYM(pManualInit, "SteamAPI_ManualDispatch_Init");
    SYM(pGetPipe, "SteamAPI_GetHSteamPipe");
    SYM(pRunFrame, "SteamAPI_ManualDispatch_RunFrame");
    SYM(pNextCallback, "SteamAPI_ManualDispatch_GetNextCallback");
    SYM(pFreeLastCallback, "SteamAPI_ManualDispatch_FreeLastCallback");
    if (pInitFlat == NULL && pInit == NULL) {
        return 0;
    }
    err[0] = 0;
    /* SteamAPI_InitFlat (SDK 1.58 on) answers k_ESteamAPIInitResult_OK, 0;
     * SteamAPI_Init (older libraries) answers true. */
    ok = pInitFlat != NULL ? pInitFlat(err) == 0 : pInit() != 0;
    if (!ok) {
        if (err[0]) {
            fprintf(stderr, "Steamworks unavailable: %s\n", err);
        }
        return 0;
    }
    if (pManualInit != NULL) {
        pManualInit();
    }
    pipe_ = pGetPipe != NULL ? pGetPipe() : 0;

    stats = steamAccessor("SteamUserStats");
    friends = steamAccessor("SteamFriends");
    user = steamAccessor("SteamUser");
    utils = steamAccessor("SteamUtils");
    apps = steamAccessor("SteamApps");
    storage = steamAccessor("SteamRemoteStorage");

    SYM(pSetAchievement, "SteamAPI_ISteamUserStats_SetAchievement");
    SYM(pClearAchievement, "SteamAPI_ISteamUserStats_ClearAchievement");
    SYM(pGetAchievement, "SteamAPI_ISteamUserStats_GetAchievement");
    SYM(pStoreStats, "SteamAPI_ISteamUserStats_StoreStats");
    /* Gone from SDK 1.61, where stats load by themselves. */
    SYM(pRequestCurrentStats, "SteamAPI_ISteamUserStats_RequestCurrentStats");
    SYM(pGetStatInt, "SteamAPI_ISteamUserStats_GetStatInt32");
    SYM(pSetStatInt, "SteamAPI_ISteamUserStats_SetStatInt32");
    SYM(pGetStatFloat, "SteamAPI_ISteamUserStats_GetStatFloat");
    SYM(pSetStatFloat, "SteamAPI_ISteamUserStats_SetStatFloat");
    SYM(pPersonaName, "SteamAPI_ISteamFriends_GetPersonaName");
    SYM(pActivateOverlay, "SteamAPI_ISteamFriends_ActivateGameOverlay");
    SYM(pActivateOverlayWeb, "SteamAPI_ISteamFriends_ActivateGameOverlayToWebPage");
    SYM(pSetRichPresence, "SteamAPI_ISteamFriends_SetRichPresence");
    SYM(pClearRichPresence, "SteamAPI_ISteamFriends_ClearRichPresence");
    SYM(pSteamId, "SteamAPI_ISteamUser_GetSteamID");
    SYM(pAppId, "SteamAPI_ISteamUtils_GetAppID");
    SYM(pOnDeck, "SteamAPI_ISteamUtils_IsSteamRunningOnSteamDeck");
    SYM(pBigPicture, "SteamAPI_ISteamUtils_IsSteamInBigPictureMode");
    SYM(pFloatingKeyboard, "SteamAPI_ISteamUtils_ShowFloatingGamepadTextInput");
    SYM(pDlcInstalled, "SteamAPI_ISteamApps_BIsDlcInstalled");
    SYM(pLanguage, "SteamAPI_ISteamApps_GetCurrentGameLanguage");
    SYM(pFileWrite, "SteamAPI_ISteamRemoteStorage_FileWrite");
    SYM(pFileRead, "SteamAPI_ISteamRemoteStorage_FileRead");
    SYM(pFileSize, "SteamAPI_ISteamRemoteStorage_GetFileSize");
    SYM(pFileExists, "SteamAPI_ISteamRemoteStorage_FileExists");
    SYM(pFileDelete, "SteamAPI_ISteamRemoteStorage_FileDelete");
    SYM(pCloudAccount, "SteamAPI_ISteamRemoteStorage_IsCloudEnabledForAccount");
    SYM(pCloudApp, "SteamAPI_ISteamRemoteStorage_IsCloudEnabledForApp");
    /* Since SDK 1.61 Steam loads the user's stats before the game starts, so
     * the accessors work at once; the request only matters to an older
     * libsteam_api, which the builders never bundle. */
    if (pRequestCurrentStats != NULL && stats != NULL) {
        pRequestCurrentStats(stats);
    }
    state = 1;
    return 1;
}

static int steamReady(void) {
    int r;
    pthread_mutex_lock(&steamLock);
    r = steamLoadLocked();
    pthread_mutex_unlock(&steamLock);
    return r;
}

/* stringToUTF8 reuses one buffer per thread; a call taking two strings must copy
 * the first before converting the second. */
static char* steamDup(CODENAME_ONE_THREAD_STATE, JAVA_OBJECT s) {
    const char* u = s == JAVA_NULL ? "" : stringToUTF8(threadStateData, s);
    return strdup(u == NULL ? "" : u);
}

JAVA_BOOLEAN com_codename1_impl_linux_LinuxNative_steamInit___R_boolean(CODENAME_ONE_THREAD_STATE) {
    return steamReady() ? JAVA_TRUE : JAVA_FALSE;
}

/* True when the process should exit because Steam is relaunching it through the
 * Steam client (the game was started from outside Steam). Valve answers false
 * whenever a steam_appid.txt sits beside the executable, which is how local
 * development runs stay put. Loads the library without initializing it. appId
 * 0 reads CN1_STEAM_APPID. */
JAVA_BOOLEAN com_codename1_impl_linux_LinuxNative_steamRestartIfNecessary___int_R_boolean(CODENAME_ONE_THREAD_STATE, JAVA_INT appId) {
    void* h;
    sbool (*fn)(uint32_t);
    if (appId <= 0) {
        /* The Steam target's launch.sh exports the app id. */
        const char* env = getenv("CN1_STEAM_APPID");
        appId = env != NULL ? atoi(env) : 0;
    }
    if (appId <= 0) {
        return JAVA_FALSE;
    }
    pthread_mutex_lock(&steamLock);
    if (lib == NULL) {
        lib = steamOpen();
    }
    h = lib;
    pthread_mutex_unlock(&steamLock);
    if (h == NULL) {
        return JAVA_FALSE;
    }
    *(void**) (&fn) = dlsym(h, "SteamAPI_RestartAppIfNecessary");
    return fn != NULL && fn((uint32_t) appId) ? JAVA_TRUE : JAVA_FALSE;
}

JAVA_VOID com_codename1_impl_linux_LinuxNative_steamShutdown__(CODENAME_ONE_THREAD_STATE) {
    pthread_mutex_lock(&steamLock);
    if (state == 1 && pShutdown != NULL) {
        pShutdown();
    }
    state = -1;
    pthread_mutex_unlock(&steamLock);
}

/* Runs one frame of callbacks; answers the first event of interest, or 0. */
JAVA_INT com_codename1_impl_linux_LinuxNative_steamPump___R_int(CODENAME_ONE_THREAD_STATE) {
    SteamCallbackMsg msg;
    int ev = 0;
    pthread_mutex_lock(&steamLock);
    if (state != 1 || pRunFrame == NULL || pNextCallback == NULL || pFreeLastCallback == NULL) {
        pthread_mutex_unlock(&steamLock);
        return 0;
    }
    pRunFrame(pipe_);
    while (pNextCallback(pipe_, &msg)) {
        if (ev == 0 && msg.callback == CB_GAME_OVERLAY_ACTIVATED && msg.paramSize > 0) {
            ev = msg.param[0] ? EV_OVERLAY_SHOWN : EV_OVERLAY_HIDDEN;
        } else if (ev == 0 && msg.callback == CB_FLOATING_KEYBOARD_DISMISSED) {
            ev = EV_KEYBOARD_DISMISSED;
        }
        pFreeLastCallback(pipe_);
        if (ev != 0) {
            break;
        }
    }
    pthread_mutex_unlock(&steamLock);
    return ev;
}

JAVA_BOOLEAN com_codename1_impl_linux_LinuxNative_steamAchievement___java_lang_String_boolean_R_boolean(CODENAME_ONE_THREAD_STATE, JAVA_OBJECT name, JAVA_BOOLEAN set) {
    const char* n;
    if (!steamReady() || stats == NULL || name == JAVA_NULL) {
        return JAVA_FALSE;
    }
    n = stringToUTF8(threadStateData, name);
    if (set) {
        return pSetAchievement != NULL && pSetAchievement(stats, n) ? JAVA_TRUE : JAVA_FALSE;
    }
    return pClearAchievement != NULL && pClearAchievement(stats, n) ? JAVA_TRUE : JAVA_FALSE;
}

JAVA_BOOLEAN com_codename1_impl_linux_LinuxNative_steamIsAchieved___java_lang_String_R_boolean(CODENAME_ONE_THREAD_STATE, JAVA_OBJECT name) {
    sbool got = 0;
    if (!steamReady() || stats == NULL || pGetAchievement == NULL || name == JAVA_NULL) {
        return JAVA_FALSE;
    }
    return pGetAchievement(stats, stringToUTF8(threadStateData, name), &got) && got ? JAVA_TRUE : JAVA_FALSE;
}

JAVA_BOOLEAN com_codename1_impl_linux_LinuxNative_steamStoreStats___R_boolean(CODENAME_ONE_THREAD_STATE) {
    if (!steamReady() || stats == NULL || pStoreStats == NULL) {
        return JAVA_FALSE;
    }
    return pStoreStats(stats) ? JAVA_TRUE : JAVA_FALSE;
}

JAVA_INT com_codename1_impl_linux_LinuxNative_steamGetStatInt___java_lang_String_int_R_int(CODENAME_ONE_THREAD_STATE, JAVA_OBJECT name, JAVA_INT def) {
    int32_t v = def;
    if (steamReady() && stats != NULL && pGetStatInt != NULL && name != JAVA_NULL
            && pGetStatInt(stats, stringToUTF8(threadStateData, name), &v)) {
        return v;
    }
    return def;
}

JAVA_BOOLEAN com_codename1_impl_linux_LinuxNative_steamSetStatInt___java_lang_String_int_R_boolean(CODENAME_ONE_THREAD_STATE, JAVA_OBJECT name, JAVA_INT value) {
    if (!steamReady() || stats == NULL || pSetStatInt == NULL || name == JAVA_NULL) {
        return JAVA_FALSE;
    }
    return pSetStatInt(stats, stringToUTF8(threadStateData, name), value) ? JAVA_TRUE : JAVA_FALSE;
}

JAVA_FLOAT com_codename1_impl_linux_LinuxNative_steamGetStatFloat___java_lang_String_float_R_float(CODENAME_ONE_THREAD_STATE, JAVA_OBJECT name, JAVA_FLOAT def) {
    float v = def;
    if (steamReady() && stats != NULL && pGetStatFloat != NULL && name != JAVA_NULL
            && pGetStatFloat(stats, stringToUTF8(threadStateData, name), &v)) {
        return v;
    }
    return def;
}

JAVA_BOOLEAN com_codename1_impl_linux_LinuxNative_steamSetStatFloat___java_lang_String_float_R_boolean(CODENAME_ONE_THREAD_STATE, JAVA_OBJECT name, JAVA_FLOAT value) {
    if (!steamReady() || stats == NULL || pSetStatFloat == NULL || name == JAVA_NULL) {
        return JAVA_FALSE;
    }
    return pSetStatFloat(stats, stringToUTF8(threadStateData, name), value) ? JAVA_TRUE : JAVA_FALSE;
}

JAVA_OBJECT com_codename1_impl_linux_LinuxNative_steamPersonaName___R_java_lang_String(CODENAME_ONE_THREAD_STATE) {
    const char* n = steamReady() && friends != NULL && pPersonaName != NULL ? pPersonaName(friends) : NULL;
    return newStringFromCString(threadStateData, n == NULL ? "" : n);
}

JAVA_LONG com_codename1_impl_linux_LinuxNative_steamUserId___R_long(CODENAME_ONE_THREAD_STATE) {
    return steamReady() && user != NULL && pSteamId != NULL ? (JAVA_LONG) pSteamId(user) : 0;
}

JAVA_INT com_codename1_impl_linux_LinuxNative_steamAppId___R_int(CODENAME_ONE_THREAD_STATE) {
    return steamReady() && utils != NULL && pAppId != NULL ? (JAVA_INT) pAppId(utils) : 0;
}

/* Bit 1: running on a Steam Deck; bit 2: Big Picture / Gaming Mode. */
JAVA_INT com_codename1_impl_linux_LinuxNative_steamDeviceFlags___R_int(CODENAME_ONE_THREAD_STATE) {
    int f = 0;
    if (steamReady() && utils != NULL) {
        if (pOnDeck != NULL && pOnDeck(utils)) {
            f |= 1;
        }
        if (pBigPicture != NULL && pBigPicture(utils)) {
            f |= 2;
        }
    }
    return f;
}

JAVA_VOID com_codename1_impl_linux_LinuxNative_steamOverlay___java_lang_String_boolean(CODENAME_ONE_THREAD_STATE, JAVA_OBJECT target, JAVA_BOOLEAN webPage) {
    const char* t;
    if (!steamReady() || friends == NULL || target == JAVA_NULL) {
        return;
    }
    t = stringToUTF8(threadStateData, target);
    if (webPage) {
        if (pActivateOverlayWeb != NULL) {
            pActivateOverlayWeb(friends, t, 0);
        }
    } else if (pActivateOverlay != NULL) {
        pActivateOverlay(friends, t);
    }
}

JAVA_BOOLEAN com_codename1_impl_linux_LinuxNative_steamRichPresence___java_lang_String_java_lang_String_R_boolean(CODENAME_ONE_THREAD_STATE, JAVA_OBJECT key, JAVA_OBJECT value) {
    char* k;
    JAVA_BOOLEAN r;
    if (!steamReady() || friends == NULL) {
        return JAVA_FALSE;
    }
    if (key == JAVA_NULL) {
        if (pClearRichPresence != NULL) {
            pClearRichPresence(friends);
        }
        return JAVA_TRUE;
    }
    if (pSetRichPresence == NULL) {
        return JAVA_FALSE;
    }
    k = steamDup(threadStateData, key);
    r = pSetRichPresence(friends, k, value == JAVA_NULL ? NULL : stringToUTF8(threadStateData, value))
            ? JAVA_TRUE : JAVA_FALSE;
    free(k);
    return r;
}

/* mode 0 single line, 1 multiple lines, 2 email, 3 numeric. */
JAVA_BOOLEAN com_codename1_impl_linux_LinuxNative_steamFloatingKeyboard___int_int_int_int_int_R_boolean(CODENAME_ONE_THREAD_STATE, JAVA_INT mode, JAVA_INT x, JAVA_INT y, JAVA_INT w, JAVA_INT h) {
    if (!steamReady() || utils == NULL || pFloatingKeyboard == NULL) {
        return JAVA_FALSE;
    }
    return pFloatingKeyboard(utils, mode, x, y, w, h) ? JAVA_TRUE : JAVA_FALSE;
}

JAVA_BOOLEAN com_codename1_impl_linux_LinuxNative_steamDlcInstalled___int_R_boolean(CODENAME_ONE_THREAD_STATE, JAVA_INT appId) {
    return steamReady() && apps != NULL && pDlcInstalled != NULL && pDlcInstalled(apps, (uint32_t) appId)
            ? JAVA_TRUE : JAVA_FALSE;
}

JAVA_OBJECT com_codename1_impl_linux_LinuxNative_steamLanguage___R_java_lang_String(CODENAME_ONE_THREAD_STATE) {
    const char* l = steamReady() && apps != NULL && pLanguage != NULL ? pLanguage(apps) : NULL;
    return newStringFromCString(threadStateData, l == NULL ? "" : l);
}

JAVA_BOOLEAN com_codename1_impl_linux_LinuxNative_steamCloudEnabled___R_boolean(CODENAME_ONE_THREAD_STATE) {
    if (!steamReady() || storage == NULL || pCloudAccount == NULL || pCloudApp == NULL) {
        return JAVA_FALSE;
    }
    return pCloudAccount(storage) && pCloudApp(storage) ? JAVA_TRUE : JAVA_FALSE;
}

JAVA_BOOLEAN com_codename1_impl_linux_LinuxNative_steamCloudWrite___java_lang_String_byte_1ARRAY_R_boolean(CODENAME_ONE_THREAD_STATE, JAVA_OBJECT name, JAVA_OBJECT data) {
    int len;
    if (!steamReady() || storage == NULL || pFileWrite == NULL || name == JAVA_NULL || data == JAVA_NULL) {
        return JAVA_FALSE;
    }
    len = (int) (*(JAVA_ARRAY) data).length;
    return pFileWrite(storage, stringToUTF8(threadStateData, name), CN1_ARRAY_DATA(data), len)
            ? JAVA_TRUE : JAVA_FALSE;
}

JAVA_OBJECT com_codename1_impl_linux_LinuxNative_steamCloudRead___java_lang_String_R_byte_1ARRAY(CODENAME_ONE_THREAD_STATE, JAVA_OBJECT name) {
    char* n;
    int32_t size, got;
    void* buf;
    JAVA_OBJECT out;
    if (!steamReady() || storage == NULL || pFileSize == NULL || pFileRead == NULL
            || pFileExists == NULL || name == JAVA_NULL) {
        return JAVA_NULL;
    }
    n = steamDup(threadStateData, name);
    if (!pFileExists(storage, n)) {
        free(n);
        return JAVA_NULL;
    }
    size = pFileSize(storage, n);
    if (size < 0) {
        free(n);
        return JAVA_NULL;
    }
    buf = malloc(size > 0 ? (size_t) size : 1);
    if (buf == NULL) {
        free(n);
        return JAVA_NULL;
    }
    got = size > 0 ? pFileRead(storage, n, buf, size) : 0;
    free(n);
    out = got == size ? cn1LinuxNewByteArray(threadStateData, buf, size) : JAVA_NULL;
    free(buf);
    return out;
}

JAVA_BOOLEAN com_codename1_impl_linux_LinuxNative_steamCloudExists___java_lang_String_R_boolean(CODENAME_ONE_THREAD_STATE, JAVA_OBJECT name) {
    return steamReady() && storage != NULL && pFileExists != NULL && name != JAVA_NULL
            && pFileExists(storage, stringToUTF8(threadStateData, name)) ? JAVA_TRUE : JAVA_FALSE;
}

JAVA_BOOLEAN com_codename1_impl_linux_LinuxNative_steamCloudDelete___java_lang_String_R_boolean(CODENAME_ONE_THREAD_STATE, JAVA_OBJECT name) {
    return steamReady() && storage != NULL && pFileDelete != NULL && name != JAVA_NULL
            && pFileDelete(storage, stringToUTF8(threadStateData, name)) ? JAVA_TRUE : JAVA_FALSE;
}
