#include "cn1_globals.h"
#include "java_io_File.h"
#include "java_lang_String.h"

#if defined(__APPLE__) && defined(__OBJC__)
#import <Foundation/Foundation.h>

// Helper to convert Java String to NSString (assuming toNSString is available or declared)
// In nativeMethods.m, toNSString is defined. Here we might need to declare it extern if we link against it,
// or implement a local version if we are standalone.
// Since this file is part of the VM build, we assume toNSString is available via cn1_globals.h or similar.
// But cn1_globals.h doesn't declare it. nativeMethods.m implements it.
// We should declare it.
@class NSString;
extern NSString* toNSString(CODENAME_ONE_THREAD_STATE, JAVA_OBJECT o);
extern JAVA_OBJECT fromNSString(CODENAME_ONE_THREAD_STATE, NSString* str);

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

    JAVA_OBJECT arr = allocArray(threadStateData, [files count], &class__java_lang_String, sizeof(JAVA_OBJECT), 1);

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
    NSAutoreleasePool* pool = [[NSAutoreleasePool alloc] init];
    NSString* p = toNSString(CN1_THREAD_STATE_PASS_ARG path);
    BOOL res = [[NSFileManager defaultManager] isExecutableFileAtPath:p];
    [pool release];
    return res;
}

JAVA_LONG java_io_File_getTotalSpaceImpl___java_lang_String_R_long(CODENAME_ONE_THREAD_STATE, JAVA_OBJECT  __cn1ThisObject, JAVA_OBJECT path) {
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
}

JAVA_LONG java_io_File_getFreeSpaceImpl___java_lang_String_R_long(CODENAME_ONE_THREAD_STATE, JAVA_OBJECT  __cn1ThisObject, JAVA_OBJECT path) {
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
    NSString* canon = [absPath stringByStandardizingPath];
    JAVA_OBJECT res = fromNSString(CN1_THREAD_STATE_PASS_ARG canon);
    [pool release];
    finishedNativeAllocations();
    return res;
}

#else
// POSIX implementation for non-ObjC environments (e.g. Linux CI)
#include <stdio.h>
#include <sys/stat.h>
#include <unistd.h>
#include <dirent.h>
#include <string.h>
#include <limits.h>

// Helper: assumes stringToUTF8 is available (implemented in test stubs or runtime)
extern const char* stringToUTF8(CODENAME_ONE_THREAD_STATE, JAVA_OBJECT str);
extern JAVA_OBJECT newStringFromCString(CODENAME_ONE_THREAD_STATE, const char *str);

JAVA_BOOLEAN java_io_File_existsImpl___java_lang_String_R_boolean(CODENAME_ONE_THREAD_STATE, JAVA_OBJECT  __cn1ThisObject, JAVA_OBJECT path) {
    if(path == JAVA_NULL) return JAVA_FALSE;
    const char* p = stringToUTF8(threadStateData, path);
    return access(p, F_OK) != -1;
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
    // This is a naive check, checking if filename starts with dot
    // We need to find the last slash
    const char* lastSlash = strrchr(p, '/');
    const char* name = lastSlash ? lastSlash + 1 : p;
    return name[0] == '.';
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
    if (access(p, F_OK) != -1) return JAVA_FALSE;
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
    DIR* d = opendir(p);
    if (d == NULL) {
        finishedNativeAllocations();
        return JAVA_NULL;
    }

    // First count
    int count = 0;
    struct dirent *dir;
    while ((dir = readdir(d)) != NULL) {
        if (strcmp(dir->d_name, ".") == 0 || strcmp(dir->d_name, "..") == 0) continue;
        count++;
    }
    closedir(d);

    JAVA_OBJECT arr = allocArray(threadStateData, count, &class__java_lang_String, sizeof(JAVA_OBJECT), 1);

    d = opendir(p);
    count = 0;
    while ((dir = readdir(d)) != NULL) {
        if (strcmp(dir->d_name, ".") == 0 || strcmp(dir->d_name, "..") == 0) continue;
        JAVA_OBJECT s = newStringFromCString(threadStateData, dir->d_name);
        CN1_SET_ARRAY_ELEMENT_OBJECT(arr, count, s);
        count++;
    }
    closedir(d);

    finishedNativeAllocations();
    return arr;
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
    return access(p, R_OK) != -1;
}

JAVA_BOOLEAN java_io_File_canWriteImpl___java_lang_String_R_boolean(CODENAME_ONE_THREAD_STATE, JAVA_OBJECT  __cn1ThisObject, JAVA_OBJECT path) {
    if(path == JAVA_NULL) return JAVA_FALSE;
    const char* p = stringToUTF8(threadStateData, path);
    return access(p, W_OK) != -1;
}

JAVA_BOOLEAN java_io_File_canExecuteImpl___java_lang_String_R_boolean(CODENAME_ONE_THREAD_STATE, JAVA_OBJECT  __cn1ThisObject, JAVA_OBJECT path) {
    if(path == JAVA_NULL) return JAVA_FALSE;
    const char* p = stringToUTF8(threadStateData, path);
    return access(p, X_OK) != -1;
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
    if (p[0] == '/') return path;
    char buf[PATH_MAX];
    if (getcwd(buf, sizeof(buf)) != NULL) {
        strcat(buf, "/");
        strcat(buf, p);
        return newStringFromCString(threadStateData, buf);
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
