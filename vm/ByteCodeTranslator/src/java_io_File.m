/*
 * Copyright (c) 2012, Codename One and/or its affiliates. All rights reserved.
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
#include "cn1_globals.h"
#include "java_io_File.h"
#include "java_lang_String.h"

#if defined(__APPLE__) && defined(__OBJC__)
#import <Foundation/Foundation.h>
// realpath and PATH_MAX, named rather than left to whatever Foundation happens to pull in.
#include <limits.h>
#include <stdlib.h>
#include <string.h>

JAVA_BOOLEAN java_io_File_existsImpl___java_lang_String_R_boolean(CODENAME_ONE_THREAD_STATE, JAVA_OBJECT  __cn1ThisObject, JAVA_OBJECT path) {
    if(path == JAVA_NULL) return JAVA_FALSE;
    NSAutoreleasePool* pool = [[NSAutoreleasePool alloc] init];
    NSString* p = toNSString(CN1_THREAD_STATE_PASS_ARG path);
    BOOL res = [[NSFileManager defaultManager] fileExistsAtPath:p];
    [pool release];
    return res;
}

JAVA_BOOLEAN java_io_File_isDirectoryImpl___java_lang_String_R_boolean(CODENAME_ONE_THREAD_STATE, JAVA_OBJECT  __cn1ThisObject, JAVA_OBJECT path) {
    if(path == JAVA_NULL) return JAVA_FALSE;
    NSAutoreleasePool* pool = [[NSAutoreleasePool alloc] init];
    NSString* p = toNSString(CN1_THREAD_STATE_PASS_ARG path);
    BOOL isDir = NO;
    BOOL exists = [[NSFileManager defaultManager] fileExistsAtPath:p isDirectory:&isDir];
    [pool release];
    return exists && isDir;
}

JAVA_BOOLEAN java_io_File_isFileImpl___java_lang_String_R_boolean(CODENAME_ONE_THREAD_STATE, JAVA_OBJECT  __cn1ThisObject, JAVA_OBJECT path) {
    if(path == JAVA_NULL) return JAVA_FALSE;
    NSAutoreleasePool* pool = [[NSAutoreleasePool alloc] init];
    NSString* p = toNSString(CN1_THREAD_STATE_PASS_ARG path);
    BOOL isDir = NO;
    BOOL exists = [[NSFileManager defaultManager] fileExistsAtPath:p isDirectory:&isDir];
    [pool release];
    return exists && !isDir;
}

JAVA_BOOLEAN java_io_File_isHiddenImpl___java_lang_String_R_boolean(CODENAME_ONE_THREAD_STATE, JAVA_OBJECT  __cn1ThisObject, JAVA_OBJECT path) {
    if(path == JAVA_NULL) return JAVA_FALSE;
    NSAutoreleasePool* pool = [[NSAutoreleasePool alloc] init];
    NSString* p = toNSString(CN1_THREAD_STATE_PASS_ARG path);
    BOOL hidden = [[p lastPathComponent] hasPrefix:@"."];
    [pool release];
    return hidden;
}

JAVA_LONG java_io_File_lastModifiedImpl___java_lang_String_R_long(CODENAME_ONE_THREAD_STATE, JAVA_OBJECT  __cn1ThisObject, JAVA_OBJECT path) {
    if(path == JAVA_NULL) return 0;
    NSAutoreleasePool* pool = [[NSAutoreleasePool alloc] init];
    NSString* p = toNSString(CN1_THREAD_STATE_PASS_ARG path);
    NSDictionary *attrs = [[NSFileManager defaultManager] attributesOfItemAtPath:p error:NULL];
    JAVA_LONG time = 0;
    if (attrs) {
        NSDate *date = [attrs fileModificationDate];
        time = (JAVA_LONG)([date timeIntervalSince1970] * 1000);
    }
    [pool release];
    return time;
}

JAVA_LONG java_io_File_lengthImpl___java_lang_String_R_long(CODENAME_ONE_THREAD_STATE, JAVA_OBJECT  __cn1ThisObject, JAVA_OBJECT path) {
    if(path == JAVA_NULL) return 0;
    NSAutoreleasePool* pool = [[NSAutoreleasePool alloc] init];
    NSString* p = toNSString(CN1_THREAD_STATE_PASS_ARG path);
    NSDictionary *attrs = [[NSFileManager defaultManager] attributesOfItemAtPath:p error:NULL];
    JAVA_LONG len = 0;
    if (attrs) {
        len = [attrs fileSize];
    }
    [pool release];
    return len;
}

JAVA_BOOLEAN java_io_File_createNewFileImpl___java_lang_String_R_boolean(CODENAME_ONE_THREAD_STATE, JAVA_OBJECT  __cn1ThisObject, JAVA_OBJECT path) {
    if(path == JAVA_NULL) return JAVA_FALSE;
    NSAutoreleasePool* pool = [[NSAutoreleasePool alloc] init];
    NSString* p = toNSString(CN1_THREAD_STATE_PASS_ARG path);
    BOOL res = [[NSFileManager defaultManager] createFileAtPath:p contents:nil attributes:nil];
    [pool release];
    return res;
}

JAVA_BOOLEAN java_io_File_deleteImpl___java_lang_String_R_boolean(CODENAME_ONE_THREAD_STATE, JAVA_OBJECT  __cn1ThisObject, JAVA_OBJECT path) {
    if(path == JAVA_NULL) return JAVA_FALSE;
    NSAutoreleasePool* pool = [[NSAutoreleasePool alloc] init];
    NSString* p = toNSString(CN1_THREAD_STATE_PASS_ARG path);
    BOOL res = [[NSFileManager defaultManager] removeItemAtPath:p error:NULL];
    [pool release];
    return res;
}

JAVA_OBJECT java_io_File_listImpl___java_lang_String_R_java_lang_String_1ARRAY(CODENAME_ONE_THREAD_STATE, JAVA_OBJECT  __cn1ThisObject, JAVA_OBJECT path) {
    if(path == JAVA_NULL) return JAVA_NULL;
    enteringNativeAllocations();
    NSAutoreleasePool* pool = [[NSAutoreleasePool alloc] init];
    NSString* p = toNSString(CN1_THREAD_STATE_PASS_ARG path);
    NSArray* files = [[NSFileManager defaultManager] contentsOfDirectoryAtPath:p error:NULL];
    if (files == nil) {
        [pool release];
        finishedNativeAllocations();
        return JAVA_NULL;
    }

    /* class_array1__java_lang_String, not class__java_lang_String: allocArray
       installs whatever class it is given as the ARRAY object's own class, so the
       element class here made File.list() return something that reported itself as
       a String rather than a String[] -- wrong for getClass() and for any array
       type check, and it hands the collector String metadata for an array payload.
       cn1MainArgs has always used the array class; these three did not. Fixed on
       all of them, including the two that predate the Windows arm. */
    JAVA_OBJECT arr = allocArray(threadStateData, [files count], &class_array1__java_lang_String, sizeof(JAVA_OBJECT), 1);

    for (int i=0; i<[files count]; i++) {
        NSString* f = [files objectAtIndex:i];
        JAVA_OBJECT s = fromNSString(CN1_THREAD_STATE_PASS_ARG f);
        CN1_SET_ARRAY_ELEMENT_OBJECT(arr, i, s);
    }

    [pool release];
    finishedNativeAllocations();
    return arr;
}

JAVA_BOOLEAN java_io_File_mkdirImpl___java_lang_String_R_boolean(CODENAME_ONE_THREAD_STATE, JAVA_OBJECT  __cn1ThisObject, JAVA_OBJECT path) {
    if(path == JAVA_NULL) return JAVA_FALSE;
    NSAutoreleasePool* pool = [[NSAutoreleasePool alloc] init];
    NSString* p = toNSString(CN1_THREAD_STATE_PASS_ARG path);
    BOOL res = [[NSFileManager defaultManager] createDirectoryAtPath:p withIntermediateDirectories:NO attributes:nil error:NULL];
    [pool release];
    return res;
}

JAVA_BOOLEAN java_io_File_renameToImpl___java_lang_String_java_lang_String_R_boolean(CODENAME_ONE_THREAD_STATE, JAVA_OBJECT  __cn1ThisObject, JAVA_OBJECT path, JAVA_OBJECT dest) {
    if(path == JAVA_NULL || dest == JAVA_NULL) return JAVA_FALSE;
    NSAutoreleasePool* pool = [[NSAutoreleasePool alloc] init];
    NSString* p = toNSString(CN1_THREAD_STATE_PASS_ARG path);
    NSString* d = toNSString(CN1_THREAD_STATE_PASS_ARG dest);
    BOOL res = [[NSFileManager defaultManager] moveItemAtPath:p toPath:d error:NULL];
    [pool release];
    return res;
}

JAVA_BOOLEAN java_io_File_setReadOnlyImpl___java_lang_String_R_boolean(CODENAME_ONE_THREAD_STATE, JAVA_OBJECT  __cn1ThisObject, JAVA_OBJECT path) {
    if(path == JAVA_NULL) return JAVA_FALSE;
    NSAutoreleasePool* pool = [[NSAutoreleasePool alloc] init];
    NSString* p = toNSString(CN1_THREAD_STATE_PASS_ARG path);
    NSDictionary* attrs = [NSDictionary dictionaryWithObject:[NSNumber numberWithBool:YES] forKey:NSFileImmutable];
    BOOL res = [[NSFileManager defaultManager] setAttributes:attrs ofItemAtPath:p error:NULL];
    [pool release];
    return res;
}

JAVA_BOOLEAN java_io_File_setWritableImpl___java_lang_String_boolean_R_boolean(CODENAME_ONE_THREAD_STATE, JAVA_OBJECT  __cn1ThisObject, JAVA_OBJECT path, JAVA_BOOLEAN writable) {
    if(path == JAVA_NULL) return JAVA_FALSE;
    NSAutoreleasePool* pool = [[NSAutoreleasePool alloc] init];
    NSString* p = toNSString(CN1_THREAD_STATE_PASS_ARG path);
    NSDictionary* attrs = [NSDictionary dictionaryWithObject:[NSNumber numberWithBool:(!writable)] forKey:NSFileImmutable];
    BOOL res = [[NSFileManager defaultManager] setAttributes:attrs ofItemAtPath:p error:NULL];
    [pool release];
    return res;
}

JAVA_BOOLEAN java_io_File_setReadableImpl___java_lang_String_boolean_R_boolean(CODENAME_ONE_THREAD_STATE, JAVA_OBJECT  __cn1ThisObject, JAVA_OBJECT path, JAVA_BOOLEAN readable) {
    // Basic implementation for iOS/sandbox (mostly ignored/always true if exists)
    return java_io_File_existsImpl___java_lang_String_R_boolean(threadStateData, __cn1ThisObject, path);
}

JAVA_BOOLEAN java_io_File_setExecutableImpl___java_lang_String_boolean_R_boolean(CODENAME_ONE_THREAD_STATE, JAVA_OBJECT  __cn1ThisObject, JAVA_OBJECT path, JAVA_BOOLEAN executable) {
    return java_io_File_existsImpl___java_lang_String_R_boolean(threadStateData, __cn1ThisObject, path);
}

JAVA_BOOLEAN java_io_File_canReadImpl___java_lang_String_R_boolean(CODENAME_ONE_THREAD_STATE, JAVA_OBJECT  __cn1ThisObject, JAVA_OBJECT path) {
    if(path == JAVA_NULL) return JAVA_FALSE;
    NSAutoreleasePool* pool = [[NSAutoreleasePool alloc] init];
    NSString* p = toNSString(CN1_THREAD_STATE_PASS_ARG path);
    BOOL res = [[NSFileManager defaultManager] isReadableFileAtPath:p];
    [pool release];
    return res;
}

JAVA_BOOLEAN java_io_File_canWriteImpl___java_lang_String_R_boolean(CODENAME_ONE_THREAD_STATE, JAVA_OBJECT  __cn1ThisObject, JAVA_OBJECT path) {
    if(path == JAVA_NULL) return JAVA_FALSE;
    NSAutoreleasePool* pool = [[NSAutoreleasePool alloc] init];
    NSString* p = toNSString(CN1_THREAD_STATE_PASS_ARG path);
    BOOL res = [[NSFileManager defaultManager] isWritableFileAtPath:p];
    [pool release];
    return res;
}

JAVA_BOOLEAN java_io_File_canExecuteImpl___java_lang_String_R_boolean(CODENAME_ONE_THREAD_STATE, JAVA_OBJECT  __cn1ThisObject, JAVA_OBJECT path) {
    if(path == JAVA_NULL) return JAVA_FALSE;
#ifdef CN1_ENABLE_FILE_SYSTEM_STATS
    NSAutoreleasePool* pool = [[NSAutoreleasePool alloc] init];
    NSString* p = toNSString(CN1_THREAD_STATE_PASS_ARG path);
    BOOL res = [[NSFileManager defaultManager] isExecutableFileAtPath:p];
    [pool release];
    return res;
#else
    return java_io_File_existsImpl___java_lang_String_R_boolean(threadStateData, __cn1ThisObject, path);
#endif
}

JAVA_LONG java_io_File_getTotalSpaceImpl___java_lang_String_R_long(CODENAME_ONE_THREAD_STATE, JAVA_OBJECT  __cn1ThisObject, JAVA_OBJECT path) {
#ifdef CN1_ENABLE_FILE_SYSTEM_STATS
    if(path == JAVA_NULL) return 0;
    NSAutoreleasePool* pool = [[NSAutoreleasePool alloc] init];
    NSString* p = toNSString(CN1_THREAD_STATE_PASS_ARG path);
    NSDictionary *attrs = [[NSFileManager defaultManager] attributesOfFileSystemForPath:p error:NULL];
    JAVA_LONG size = 0;
    if(attrs) {
        size = [[attrs objectForKey:NSFileSystemSize] longLongValue];
    }
    [pool release];
    return size;
#else
    return 0;
#endif
}

JAVA_LONG java_io_File_getFreeSpaceImpl___java_lang_String_R_long(CODENAME_ONE_THREAD_STATE, JAVA_OBJECT  __cn1ThisObject, JAVA_OBJECT path) {
#ifdef CN1_ENABLE_FILE_SYSTEM_STATS
    if(path == JAVA_NULL) return 0;
    NSAutoreleasePool* pool = [[NSAutoreleasePool alloc] init];
    NSString* p = toNSString(CN1_THREAD_STATE_PASS_ARG path);
    NSDictionary *attrs = [[NSFileManager defaultManager] attributesOfFileSystemForPath:p error:NULL];
    JAVA_LONG size = 0;
    if(attrs) {
        size = [[attrs objectForKey:NSFileSystemFreeSize] longLongValue];
    }
    [pool release];
    return size;
#else
    return 0;
#endif
}

JAVA_LONG java_io_File_getUsableSpaceImpl___java_lang_String_R_long(CODENAME_ONE_THREAD_STATE, JAVA_OBJECT  __cn1ThisObject, JAVA_OBJECT path) {
    return java_io_File_getFreeSpaceImpl___java_lang_String_R_long(threadStateData, __cn1ThisObject, path);
}

JAVA_OBJECT java_io_File_getAbsolutePathImpl___java_lang_String_R_java_lang_String(CODENAME_ONE_THREAD_STATE, JAVA_OBJECT  __cn1ThisObject, JAVA_OBJECT path) {
    if(path == JAVA_NULL) return JAVA_NULL;
    enteringNativeAllocations();
    NSAutoreleasePool* pool = [[NSAutoreleasePool alloc] init];
    NSString* p = toNSString(CN1_THREAD_STATE_PASS_ARG path);

    NSString* absPath;
    if ([p isAbsolutePath]) {
        absPath = p;
    } else {
        NSString* cwd = [[NSFileManager defaultManager] currentDirectoryPath];
        absPath = [cwd stringByAppendingPathComponent:p];
    }
    JAVA_OBJECT res = fromNSString(CN1_THREAD_STATE_PASS_ARG absPath);
    [pool release];
    finishedNativeAllocations();
    return res;
}

JAVA_OBJECT java_io_File_getCanonicalPathImpl___java_lang_String_R_java_lang_String(CODENAME_ONE_THREAD_STATE, JAVA_OBJECT  __cn1ThisObject, JAVA_OBJECT path) {
    if(path == JAVA_NULL) return JAVA_NULL;
    enteringNativeAllocations();
    NSAutoreleasePool* pool = [[NSAutoreleasePool alloc] init];
    NSString* p = toNSString(CN1_THREAD_STATE_PASS_ARG path);
    NSString* absPath;
     if ([p isAbsolutePath]) {
        absPath = p;
    } else {
        NSString* cwd = [[NSFileManager defaultManager] currentDirectoryPath];
        absPath = [cwd stringByAppendingPathComponent:p];
    }
    // realpath first, which is what getCanonicalPath is specified to do and what the platform
    // itself resolves a path to. -stringByStandardizingPath is neither: it leaves a symbolic
    // link in the middle of a path alone, and it rewrites /private/tmp and /private/var back to
    // /tmp and /var -- the opposite direction from every other resolver on the system. SQLite
    // reports the realpath form for an attached database, so a key built from the standardized
    // form named a file the engine had never heard of.
    //
    // Falls back for a path that does not exist yet, where realpath cannot answer at all.
    NSString* canon = nil;
    char resolved[PATH_MAX];
    const char* utf8 = [absPath fileSystemRepresentation];
    if (utf8 != NULL && realpath(utf8, resolved) != NULL) {
        canon = [[NSFileManager defaultManager] stringWithFileSystemRepresentation:resolved
                                                                           length:strlen(resolved)];
    }
    if (canon == nil) {
        canon = [absPath stringByStandardizingPath];
    }
    JAVA_OBJECT res = fromNSString(CN1_THREAD_STATE_PASS_ARG canon);
    [pool release];
    finishedNativeAllocations();
    return res;
}

#else
// Implementation for non-ObjC environments: Linux CI, the native Windows port and
// the clean target. Windows reaches this branch under clang-cl, which is neither
// __OBJC__ nor POSIX.
#include <stdio.h>
#include <sys/stat.h>
#include <string.h>
#include <limits.h>
/* Shared, not per-arm: cn1NameList below uses malloc/realloc/free on BOTH, and it
   sits outside the platform blocks. */
#include <stdlib.h>
#ifdef _WIN32
/* clang-cl ships no <unistd.h> and no <dirent.h>. Only two things in this file
   actually need them -- access() and the directory walk -- and the MSVC CRT
   provides everything else (stat, remove, rename, mkdir) under the same names.
   Without these guards the whole file stopped at "'unistd.h' file not found",
   which is what every Windows clean-target build did the moment an app first
   reached java.io.File. */
#include <io.h>
#include <direct.h>
/* WIN32_LEAN_AND_MEAN keeps <winsock.h> out of <windows.h>. Without it winsock's
   own `struct timeval` collides with the one cn1_win_compat.h defines, and the
   file fails on "redefinition of 'timeval'" rather than on anything it does. */
#ifndef WIN32_LEAN_AND_MEAN
#define WIN32_LEAN_AND_MEAN
#endif
#include <windows.h>
/* The MSVC CRT has the st_mode BITS but not the POSIX macros that test them. */
#ifndef S_ISDIR
#define S_ISDIR(m) (((m) & _S_IFMT) == _S_IFDIR)
#endif
#ifndef S_ISREG
#define S_ISREG(m) (((m) & _S_IFMT) == _S_IFREG)
#endif
/* PATH_MAX is POSIX; MAX_PATH is the Win32 spelling. realpath's counterpart is
   _fullpath, which takes (destination, source) -- the REVERSE of realpath's
   (source, destination) -- so the macro swaps them; getting that backwards
   compiles and silently canonicalizes the wrong string. Both return NULL on
   failure. _fullpath also resolves a path that does not exist rather than
   failing, which is the more useful answer for getCanonicalPath. */
#ifndef PATH_MAX
#define PATH_MAX MAX_PATH
#endif
#define realpath(path, resolved) _fullpath((resolved), (path), MAX_PATH)
#define CN1_FILE_SEP '\\'

#ifndef F_OK
#define F_OK 0
#endif
#ifndef R_OK
#define R_OK 4
#endif
#ifndef W_OK
#define W_OK 2
#endif
/* No execute bit exists in the Win32 access() model, and _access REJECTS a mode
   of 1 rather than reporting "not executable". Ask whether the file exists, which
   is the closest true answer and what the JDK reports for a readable file. */
#ifndef X_OK
#define X_OK 0
#endif
#define CN1_FILE_ACCESS(p, m) _access((p), (m))
#else
#include <unistd.h>
#include <dirent.h>
#define CN1_FILE_ACCESS(p, m) access((p), (m))
#define CN1_FILE_SEP '/'
#endif

/*
 * A growable list of names, so a directory is enumerated exactly ONCE.
 *
 * The two-pass shape this replaces -- count, allocate, enumerate again -- assumed
 * the two walks see the same directory. They do not: a file created between them
 * overruns the array (CN1_SET_ARRAY_ELEMENT_OBJECT then raises
 * ArrayIndexOutOfBoundsException) and a file removed leaves trailing nulls in a
 * String[] that Java code has no reason to expect. Directories change under
 * readers all the time, so this was a real race on every platform, not just the
 * newly added Windows arm.
 *
 * The names are held in C memory on purpose: allocArray and newStringFromCString
 * can both collect, and nothing here may be holding a directory handle when that
 * happens.
 */
struct cn1NameList { char** names; int count; int cap; };

static int cn1NameListAdd(struct cn1NameList* l, const char* name) {
    size_t n;
    char* copy;
    if(l->count == l->cap) {
        int cap = l->cap == 0 ? 16 : l->cap * 2;
        char** grown = (char**)realloc(l->names, (size_t)cap * sizeof(char*));
        if(grown == NULL) {
            return 0;
        }
        l->names = grown;
        l->cap = cap;
    }
    n = strlen(name) + 1;
    copy = (char*)malloc(n);
    if(copy == NULL) {
        return 0;
    }
    memcpy(copy, name, n);
    l->names[l->count++] = copy;
    return 1;
}

static void cn1NameListFree(struct cn1NameList* l) {
    int i;
    for(i = 0 ; i < l->count ; i++) {
        free(l->names[i]);
    }
    free(l->names);
    l->names = 0;
    l->count = 0;
    l->cap = 0;
}

/* Turns a completed name list into the String[] File.list returns. */
static JAVA_OBJECT cn1NameListToArray(CODENAME_ONE_THREAD_STATE, struct cn1NameList* l) {
    JAVA_OBJECT arr = allocArray(threadStateData, l->count, &class_array1__java_lang_String, sizeof(JAVA_OBJECT), 1);
    int i;
    for(i = 0 ; i < l->count ; i++) {
        JAVA_OBJECT s = newStringFromCString(threadStateData, l->names[i]);
        CN1_SET_ARRAY_ELEMENT_OBJECT(arr, i, s);
    }
    return arr;
}

/*
 * "Absolute" is not the same question on the two platforms, and getting it wrong
 * CORRUPTS a path rather than merely misreporting one: the caller prepends the
 * working directory to anything this rejects, so "C:\\data" came back as
 * "C:\\cwd\\C:\\data".
 *
 * NOTE the matching Java-side gap, deliberately not changed here:
 * java.io.File.isAbsolute() tests path.startsWith(File.separator) and
 * File.separator is "/" on every target, so it still answers false for a drive or
 * UNC path. Fixing that means giving JavaAPI a per-platform separator, which is a
 * change to shared Java for every port -- out of scope for making the clean target
 * build. The native above is what stops a wrong answer from producing a wrong
 * PATH; isAbsolute() returning false is a wrong answer that corrupts nothing.
 */
static int cn1FileIsAbsolute(const char* p) {
    if (p == NULL || p[0] == '\0') {
        return 0;
    }
#ifdef _WIN32
    /* A UNC path ("\\server\share") and a rooted "\path" both start at a root. */
    if (p[0] == '/' || p[0] == '\\') {
        return 1;
    }
    /* "C:\x" or "C:/x". A bare "C:x" is drive-RELATIVE, and is not absolute. */
    return p[1] == ':' && (p[2] == '\\' || p[2] == '/');
#else
    return p[0] == '/';
#endif
}

// Helper: assumes stringToUTF8 is available (implemented in test stubs or runtime)
extern const char* stringToUTF8(CODENAME_ONE_THREAD_STATE, JAVA_OBJECT str);
extern JAVA_OBJECT newStringFromCString(CODENAME_ONE_THREAD_STATE, const char *str);

JAVA_BOOLEAN java_io_File_existsImpl___java_lang_String_R_boolean(CODENAME_ONE_THREAD_STATE, JAVA_OBJECT  __cn1ThisObject, JAVA_OBJECT path) {
    if(path == JAVA_NULL) return JAVA_FALSE;
    const char* p = stringToUTF8(threadStateData, path);
    return CN1_FILE_ACCESS(p, F_OK) != -1;
}

JAVA_BOOLEAN java_io_File_isDirectoryImpl___java_lang_String_R_boolean(CODENAME_ONE_THREAD_STATE, JAVA_OBJECT  __cn1ThisObject, JAVA_OBJECT path) {
    if(path == JAVA_NULL) return JAVA_FALSE;
    const char* p = stringToUTF8(threadStateData, path);
    struct stat s;
    if (stat(p, &s) == 0) {
        return S_ISDIR(s.st_mode);
    }
    return JAVA_FALSE;
}

JAVA_BOOLEAN java_io_File_isFileImpl___java_lang_String_R_boolean(CODENAME_ONE_THREAD_STATE, JAVA_OBJECT  __cn1ThisObject, JAVA_OBJECT path) {
    if(path == JAVA_NULL) return JAVA_FALSE;
    const char* p = stringToUTF8(threadStateData, path);
    struct stat s;
    if (stat(p, &s) == 0) {
        return S_ISREG(s.st_mode);
    }
    return JAVA_FALSE;
}

JAVA_BOOLEAN java_io_File_isHiddenImpl___java_lang_String_R_boolean(CODENAME_ONE_THREAD_STATE, JAVA_OBJECT  __cn1ThisObject, JAVA_OBJECT path) {
    if(path == JAVA_NULL) return JAVA_FALSE;
    const char* p = stringToUTF8(threadStateData, path);
#ifdef _WIN32
    /* Windows has a real hidden ATTRIBUTE; a leading dot means nothing there. */
    {
        DWORD attr = GetFileAttributesA(p);
        return (attr != INVALID_FILE_ATTRIBUTES && (attr & FILE_ATTRIBUTE_HIDDEN))
                ? JAVA_TRUE : JAVA_FALSE;
    }
#else
    // This is a naive check, checking if filename starts with dot
    // We need to find the last slash
    const char* lastSlash = strrchr(p, '/');
    const char* name = lastSlash ? lastSlash + 1 : p;
    return name[0] == '.';
#endif
}

JAVA_LONG java_io_File_lastModifiedImpl___java_lang_String_R_long(CODENAME_ONE_THREAD_STATE, JAVA_OBJECT  __cn1ThisObject, JAVA_OBJECT path) {
    if(path == JAVA_NULL) return 0;
    const char* p = stringToUTF8(threadStateData, path);
    struct stat s;
    if (stat(p, &s) == 0) {
#ifdef __APPLE__
        return (JAVA_LONG)s.st_mtimespec.tv_sec * 1000;
#else
        return (JAVA_LONG)s.st_mtime * 1000;
#endif
    }
    return 0;
}

JAVA_LONG java_io_File_lengthImpl___java_lang_String_R_long(CODENAME_ONE_THREAD_STATE, JAVA_OBJECT  __cn1ThisObject, JAVA_OBJECT path) {
    if(path == JAVA_NULL) return 0;
    const char* p = stringToUTF8(threadStateData, path);
    struct stat s;
    if (stat(p, &s) == 0) {
        return (JAVA_LONG)s.st_size;
    }
    return 0;
}

JAVA_BOOLEAN java_io_File_createNewFileImpl___java_lang_String_R_boolean(CODENAME_ONE_THREAD_STATE, JAVA_OBJECT  __cn1ThisObject, JAVA_OBJECT path) {
    if(path == JAVA_NULL) return JAVA_FALSE;
    const char* p = stringToUTF8(threadStateData, path);
    if (CN1_FILE_ACCESS(p, F_OK) != -1) return JAVA_FALSE;
    FILE* f = fopen(p, "w");
    if (f) {
        fclose(f);
        return JAVA_TRUE;
    }
    return JAVA_FALSE;
}

JAVA_BOOLEAN java_io_File_deleteImpl___java_lang_String_R_boolean(CODENAME_ONE_THREAD_STATE, JAVA_OBJECT  __cn1ThisObject, JAVA_OBJECT path) {
    if(path == JAVA_NULL) return JAVA_FALSE;
    const char* p = stringToUTF8(threadStateData, path);
    if (remove(p) == 0) return JAVA_TRUE;
    return JAVA_FALSE;
}

JAVA_OBJECT java_io_File_listImpl___java_lang_String_R_java_lang_String_1ARRAY(CODENAME_ONE_THREAD_STATE, JAVA_OBJECT  __cn1ThisObject, JAVA_OBJECT path) {
    if(path == JAVA_NULL) return JAVA_NULL;
    enteringNativeAllocations();
    const char* p = stringToUTF8(threadStateData, path);
#ifdef _WIN32
    /* FindFirstFile rather than opendir, and it wants a wildcard appended. ONE
       enumeration into cn1NameList -- see the note there for why two walks of the
       same directory is a race rather than a shortcut. */
    {
        char pattern[MAX_PATH];
        WIN32_FIND_DATAA fd;
        HANDLE h;
        struct cn1NameList list;
        size_t plen = strlen(p);
        list.names = 0; list.count = 0; list.cap = 0;
        if (plen == 0 || plen + 3 > sizeof(pattern)) {
            finishedNativeAllocations();
            return JAVA_NULL;
        }
        memcpy(pattern, p, plen);
        /* Do not double a separator the caller already supplied. */
        if (p[plen - 1] == '\\' || p[plen - 1] == '/') {
            pattern[plen] = '*';
            pattern[plen + 1] = '\0';
        } else {
            pattern[plen] = '\\';
            pattern[plen + 1] = '*';
            pattern[plen + 2] = '\0';
        }
        h = FindFirstFileA(pattern, &fd);
        if (h == INVALID_HANDLE_VALUE) {
            finishedNativeAllocations();
            return JAVA_NULL;
        }
        do {
            if (strcmp(fd.cFileName, ".") == 0 || strcmp(fd.cFileName, "..") == 0) continue;
            if (!cn1NameListAdd(&list, fd.cFileName)) {
                FindClose(h);
                cn1NameListFree(&list);
                finishedNativeAllocations();
                return JAVA_NULL;
            }
        } while (FindNextFileA(h, &fd));
        FindClose(h);
        {
            JAVA_OBJECT arr = cn1NameListToArray(threadStateData, &list);
            cn1NameListFree(&list);
            finishedNativeAllocations();
            return arr;
        }
    }
#else
    {
        DIR* d = opendir(p);
        struct dirent* entry;
        struct cn1NameList list;
        list.names = 0; list.count = 0; list.cap = 0;
        if (d == NULL) {
            finishedNativeAllocations();
            return JAVA_NULL;
        }
        while ((entry = readdir(d)) != NULL) {
            if (strcmp(entry->d_name, ".") == 0 || strcmp(entry->d_name, "..") == 0) continue;
            if (!cn1NameListAdd(&list, entry->d_name)) {
                closedir(d);
                cn1NameListFree(&list);
                finishedNativeAllocations();
                return JAVA_NULL;
            }
        }
        closedir(d);
        {
            JAVA_OBJECT arr = cn1NameListToArray(threadStateData, &list);
            cn1NameListFree(&list);
            finishedNativeAllocations();
            return arr;
        }
    }
#endif
}

JAVA_BOOLEAN java_io_File_mkdirImpl___java_lang_String_R_boolean(CODENAME_ONE_THREAD_STATE, JAVA_OBJECT  __cn1ThisObject, JAVA_OBJECT path) {
    if(path == JAVA_NULL) return JAVA_FALSE;
    const char* p = stringToUTF8(threadStateData, path);
#ifdef _WIN32
    if (mkdir(p) == 0) return JAVA_TRUE;
#else
    if (mkdir(p, 0755) == 0) return JAVA_TRUE;
#endif
    return JAVA_FALSE;
}

JAVA_BOOLEAN java_io_File_renameToImpl___java_lang_String_java_lang_String_R_boolean(CODENAME_ONE_THREAD_STATE, JAVA_OBJECT  __cn1ThisObject, JAVA_OBJECT path, JAVA_OBJECT dest) {
    if(path == JAVA_NULL || dest == JAVA_NULL) return JAVA_FALSE;
    const char* p = stringToUTF8(threadStateData, path);
    const char* d = stringToUTF8(threadStateData, dest);
    if (rename(p, d) == 0) return JAVA_TRUE;
    return JAVA_FALSE;
}

JAVA_BOOLEAN java_io_File_setReadOnlyImpl___java_lang_String_R_boolean(CODENAME_ONE_THREAD_STATE, JAVA_OBJECT  __cn1ThisObject, JAVA_OBJECT path) {
    return JAVA_FALSE; // Not implemented for POSIX here
}

JAVA_BOOLEAN java_io_File_setWritableImpl___java_lang_String_boolean_R_boolean(CODENAME_ONE_THREAD_STATE, JAVA_OBJECT  __cn1ThisObject, JAVA_OBJECT path, JAVA_BOOLEAN writable) {
    return JAVA_FALSE;
}

JAVA_BOOLEAN java_io_File_setReadableImpl___java_lang_String_boolean_R_boolean(CODENAME_ONE_THREAD_STATE, JAVA_OBJECT  __cn1ThisObject, JAVA_OBJECT path, JAVA_BOOLEAN readable) {
    return JAVA_FALSE;
}

JAVA_BOOLEAN java_io_File_setExecutableImpl___java_lang_String_boolean_R_boolean(CODENAME_ONE_THREAD_STATE, JAVA_OBJECT  __cn1ThisObject, JAVA_OBJECT path, JAVA_BOOLEAN executable) {
    return JAVA_FALSE;
}

JAVA_BOOLEAN java_io_File_canReadImpl___java_lang_String_R_boolean(CODENAME_ONE_THREAD_STATE, JAVA_OBJECT  __cn1ThisObject, JAVA_OBJECT path) {
    if(path == JAVA_NULL) return JAVA_FALSE;
    const char* p = stringToUTF8(threadStateData, path);
    return CN1_FILE_ACCESS(p, R_OK) != -1;
}

JAVA_BOOLEAN java_io_File_canWriteImpl___java_lang_String_R_boolean(CODENAME_ONE_THREAD_STATE, JAVA_OBJECT  __cn1ThisObject, JAVA_OBJECT path) {
    if(path == JAVA_NULL) return JAVA_FALSE;
    const char* p = stringToUTF8(threadStateData, path);
    return CN1_FILE_ACCESS(p, W_OK) != -1;
}

JAVA_BOOLEAN java_io_File_canExecuteImpl___java_lang_String_R_boolean(CODENAME_ONE_THREAD_STATE, JAVA_OBJECT  __cn1ThisObject, JAVA_OBJECT path) {
    if(path == JAVA_NULL) return JAVA_FALSE;
    const char* p = stringToUTF8(threadStateData, path);
    return CN1_FILE_ACCESS(p, X_OK) != -1;
}

JAVA_LONG java_io_File_getTotalSpaceImpl___java_lang_String_R_long(CODENAME_ONE_THREAD_STATE, JAVA_OBJECT  __cn1ThisObject, JAVA_OBJECT path) {
    return 0;
}

JAVA_LONG java_io_File_getFreeSpaceImpl___java_lang_String_R_long(CODENAME_ONE_THREAD_STATE, JAVA_OBJECT  __cn1ThisObject, JAVA_OBJECT path) {
    return 0;
}

JAVA_LONG java_io_File_getUsableSpaceImpl___java_lang_String_R_long(CODENAME_ONE_THREAD_STATE, JAVA_OBJECT  __cn1ThisObject, JAVA_OBJECT path) {
    return 0;
}

JAVA_OBJECT java_io_File_getAbsolutePathImpl___java_lang_String_R_java_lang_String(CODENAME_ONE_THREAD_STATE, JAVA_OBJECT  __cn1ThisObject, JAVA_OBJECT path) {
    if(path == JAVA_NULL) return JAVA_NULL;
    const char* p = stringToUTF8(threadStateData, path);
    if (cn1FileIsAbsolute(p)) return path;
    {
        char buf[PATH_MAX];
        char joined[PATH_MAX];
#ifdef _WIN32
        if (_getcwd(buf, (int)sizeof(buf)) != NULL) {
#else
        if (getcwd(buf, sizeof(buf)) != NULL) {
#endif
            /* snprintf, not strcat: the original wrote the separator and the whole
               relative path onto a PATH_MAX buffer already holding the cwd, with no
               room left to check. */
            if (snprintf(joined, sizeof(joined), "%s%c%s", buf, CN1_FILE_SEP, p) < (int)sizeof(joined)) {
                return newStringFromCString(threadStateData, joined);
            }
        }
    }
    return path;
}

JAVA_OBJECT java_io_File_getCanonicalPathImpl___java_lang_String_R_java_lang_String(CODENAME_ONE_THREAD_STATE, JAVA_OBJECT  __cn1ThisObject, JAVA_OBJECT path) {
    if(path == JAVA_NULL) return JAVA_NULL;
    const char* p = stringToUTF8(threadStateData, path);
    char buf[PATH_MAX];
    if (realpath(p, buf) != NULL) {
        return newStringFromCString(threadStateData, buf);
    }
    // Fallback
    return java_io_File_getAbsolutePathImpl___java_lang_String_R_java_lang_String(threadStateData, __cn1ThisObject, path);
}

#endif
