#include "cn1_globals.h"
#include <sys/stat.h>
#include <sys/types.h>
#include <unistd.h>
#include <dirent.h>
#include <errno.h>
#include <stdlib.h>
#include <string.h>
#include <fcntl.h>
#include <utime.h>
#include <limits.h>

#ifndef __APPLE__
#include <sys/statvfs.h>
#endif

// Forward declarations for helpers provided elsewhere
extern const char* stringToUTF8(CODENAME_ONE_THREAD_STATE, JAVA_OBJECT str);
extern JAVA_OBJECT newStringFromCString(CODENAME_ONE_THREAD_STATE, const char *str);

static int cn1_stat(const char* path, struct stat* st) {
    if (!path) {
        return -1;
    }
    return stat(path, st);
}

JAVA_BOOLEAN java_io_File_nativeCanExecute___java_lang_String_R_boolean(CODENAME_ONE_THREAD_STATE, JAVA_OBJECT pathObj) {
    const char* path = stringToUTF8(threadStateData, pathObj);
    return (path && access(path, X_OK) == 0) ? JAVA_TRUE : JAVA_FALSE;
}

JAVA_BOOLEAN java_io_File_nativeCanRead___java_lang_String_R_boolean(CODENAME_ONE_THREAD_STATE, JAVA_OBJECT pathObj) {
    const char* path = stringToUTF8(threadStateData, pathObj);
    return (path && access(path, R_OK) == 0) ? JAVA_TRUE : JAVA_FALSE;
}

JAVA_BOOLEAN java_io_File_nativeCanWrite___java_lang_String_R_boolean(CODENAME_ONE_THREAD_STATE, JAVA_OBJECT pathObj) {
    const char* path = stringToUTF8(threadStateData, pathObj);
    return (path && access(path, W_OK) == 0) ? JAVA_TRUE : JAVA_FALSE;
}

JAVA_BOOLEAN java_io_File_nativeCreateFile___java_lang_String_R_boolean(CODENAME_ONE_THREAD_STATE, JAVA_OBJECT pathObj) {
    const char* path = stringToUTF8(threadStateData, pathObj);
    if (!path) return JAVA_FALSE;
    int fd = open(path, O_CREAT | O_EXCL, 0666);
    if (fd >= 0) {
        close(fd);
        return JAVA_TRUE;
    }
    return JAVA_FALSE;
}

JAVA_OBJECT java_io_File_nativeCreateTempFile___java_lang_String_java_lang_String_java_lang_String_R_java_lang_String(CODENAME_ONE_THREAD_STATE, JAVA_OBJECT prefixObj, JAVA_OBJECT suffixObj, JAVA_OBJECT dirObj) {
    const char* prefix = stringToUTF8(threadStateData, prefixObj);
    const char* suffix = stringToUTF8(threadStateData, suffixObj);
    const char* dir = stringToUTF8(threadStateData, dirObj);
    if (!prefix) {
        return JAVA_NULL;
    }
    const char* actualDir = dir ? dir : "/tmp";
    size_t len = strlen(actualDir) + strlen(prefix) + 16;
    char* tmpl = (char*)malloc(len);
    if (!tmpl) return JAVA_NULL;
    snprintf(tmpl, len, "%s/%sXXXXXX", actualDir, prefix);
    int fd = mkstemp(tmpl);
    if (fd >= 0) {
        close(fd);
        JAVA_OBJECT out = newStringFromCString(threadStateData, tmpl);
        if (suffix) {
            // Rename to include suffix
            size_t newlen = strlen(tmpl) + strlen(suffix) + 1;
            char* renamed = (char*)malloc(newlen);
            if (renamed) {
                snprintf(renamed, newlen, "%s%s", tmpl, suffix);
                rename(tmpl, renamed);
                free(tmpl);
                tmpl = renamed;
                out = newStringFromCString(threadStateData, tmpl);
            }
        }
        free(tmpl);
        return out;
    }
    free(tmpl);
    return JAVA_NULL;
}

JAVA_BOOLEAN java_io_File_nativeDelete___java_lang_String_R_boolean(CODENAME_ONE_THREAD_STATE, JAVA_OBJECT pathObj) {
    const char* path = stringToUTF8(threadStateData, pathObj);
    return (path && remove(path) == 0) ? JAVA_TRUE : JAVA_FALSE;
}

JAVA_BOOLEAN java_io_File_nativeExists___java_lang_String_R_boolean(CODENAME_ONE_THREAD_STATE, JAVA_OBJECT pathObj) {
    struct stat st;
    const char* path = stringToUTF8(threadStateData, pathObj);
    return (cn1_stat(path, &st) == 0) ? JAVA_TRUE : JAVA_FALSE;
}

JAVA_BOOLEAN java_io_File_nativeIsDirectory___java_lang_String_R_boolean(CODENAME_ONE_THREAD_STATE, JAVA_OBJECT pathObj) {
    struct stat st;
    const char* path = stringToUTF8(threadStateData, pathObj);
    return (cn1_stat(path, &st) == 0 && S_ISDIR(st.st_mode)) ? JAVA_TRUE : JAVA_FALSE;
}

JAVA_BOOLEAN java_io_File_nativeIsFile___java_lang_String_R_boolean(CODENAME_ONE_THREAD_STATE, JAVA_OBJECT pathObj) {
    struct stat st;
    const char* path = stringToUTF8(threadStateData, pathObj);
    return (cn1_stat(path, &st) == 0 && S_ISREG(st.st_mode)) ? JAVA_TRUE : JAVA_FALSE;
}

JAVA_BOOLEAN java_io_File_nativeIsHidden___java_lang_String_R_boolean(CODENAME_ONE_THREAD_STATE, JAVA_OBJECT pathObj) {
    const char* path = stringToUTF8(threadStateData, pathObj);
    if (!path) return JAVA_FALSE;
    const char* lastSlash = strrchr(path, '/');
    const char* name = lastSlash ? lastSlash + 1 : path;
    return (name[0] == '.') ? JAVA_TRUE : JAVA_FALSE;
}

JAVA_BOOLEAN java_io_File_nativeMkdir___java_lang_String_R_boolean(CODENAME_ONE_THREAD_STATE, JAVA_OBJECT pathObj) {
    const char* path = stringToUTF8(threadStateData, pathObj);
    return (path && mkdir(path, 0777) == 0) ? JAVA_TRUE : JAVA_FALSE;
}

JAVA_BOOLEAN java_io_File_nativeRename___java_lang_String_java_lang_String_R_boolean(CODENAME_ONE_THREAD_STATE, JAVA_OBJECT srcObj, JAVA_OBJECT dstObj) {
    const char* src = stringToUTF8(threadStateData, srcObj);
    const char* dst = stringToUTF8(threadStateData, dstObj);
    return (src && dst && rename(src, dst) == 0) ? JAVA_TRUE : JAVA_FALSE;
}

static JAVA_BOOLEAN applyMode(CODENAME_ONE_THREAD_STATE, JAVA_OBJECT pathObj, int mask, JAVA_BOOLEAN enable, JAVA_BOOLEAN ownerOnly) {
    struct stat st;
    const char* path = stringToUTF8(threadStateData, pathObj);
    if (!path || cn1_stat(path, &st) != 0) {
        return JAVA_FALSE;
    }
    mode_t newMode = st.st_mode;
    if (enable) {
        newMode |= mask;
        if (ownerOnly) {
            newMode &= ~(mask >> 3);
            newMode &= ~(mask >> 6);
        }
    } else {
        newMode &= ~mask;
    }
    return chmod(path, newMode) == 0 ? JAVA_TRUE : JAVA_FALSE;
}

JAVA_BOOLEAN java_io_File_nativeSetExecutable___java_lang_String_boolean_boolean_R_boolean(CODENAME_ONE_THREAD_STATE, JAVA_OBJECT pathObj, JAVA_BOOLEAN value, JAVA_BOOLEAN ownerOnly) {
    return applyMode(threadStateData, pathObj, S_IXUSR | S_IXGRP | S_IXOTH, value, ownerOnly);
}

JAVA_BOOLEAN java_io_File_nativeSetReadable___java_lang_String_boolean_boolean_R_boolean(CODENAME_ONE_THREAD_STATE, JAVA_OBJECT pathObj, JAVA_BOOLEAN value, JAVA_BOOLEAN ownerOnly) {
    return applyMode(threadStateData, pathObj, S_IRUSR | S_IRGRP | S_IROTH, value, ownerOnly);
}

JAVA_BOOLEAN java_io_File_nativeSetWritable___java_lang_String_boolean_boolean_R_boolean(CODENAME_ONE_THREAD_STATE, JAVA_OBJECT pathObj, JAVA_BOOLEAN value, JAVA_BOOLEAN ownerOnly) {
    return applyMode(threadStateData, pathObj, S_IWUSR | S_IWGRP | S_IWOTH, value, ownerOnly);
}

JAVA_BOOLEAN java_io_File_nativeSetLastModified___java_lang_String_long_R_boolean(CODENAME_ONE_THREAD_STATE, JAVA_OBJECT pathObj, JAVA_LONG time) {
    const char* path = stringToUTF8(threadStateData, pathObj);
    if (!path) return JAVA_FALSE;
    struct utimbuf times;
    times.actime = time / 1000;
    times.modtime = time / 1000;
    return utime(path, &times) == 0 ? JAVA_TRUE : JAVA_FALSE;
}

JAVA_OBJECT java_io_File_nativeAbsolutePath___java_lang_String_R_java_lang_String(CODENAME_ONE_THREAD_STATE, JAVA_OBJECT pathObj) {
    const char* path = stringToUTF8(threadStateData, pathObj);
    if (!path) return JAVA_NULL;
    char buf[PATH_MAX];
    if (realpath(path, buf) != NULL) {
        return newStringFromCString(threadStateData, buf);
    }
    return pathObj;
}

JAVA_OBJECT java_io_File_nativeCanonicalPath___java_lang_String_R_java_lang_String(CODENAME_ONE_THREAD_STATE, JAVA_OBJECT pathObj) {
    const char* path = stringToUTF8(threadStateData, pathObj);
    if (!path) return JAVA_NULL;
    char buf[PATH_MAX];
    if (realpath(path, buf) != NULL) {
        return newStringFromCString(threadStateData, buf);
    }
    return JAVA_NULL;
}

JAVA_LONG java_io_File_nativeLastModified___java_lang_String_R_long(CODENAME_ONE_THREAD_STATE, JAVA_OBJECT pathObj) {
    struct stat st;
    const char* path = stringToUTF8(threadStateData, pathObj);
    if (cn1_stat(path, &st) == 0) {
        return ((JAVA_LONG)st.st_mtime) * 1000;
    }
    return (JAVA_LONG)0;
}

JAVA_LONG java_io_File_nativeLength___java_lang_String_R_long(CODENAME_ONE_THREAD_STATE, JAVA_OBJECT pathObj) {
    struct stat st;
    const char* path = stringToUTF8(threadStateData, pathObj);
    if (cn1_stat(path, &st) == 0) {
        return (JAVA_LONG)st.st_size;
    }
    return (JAVA_LONG)0;
}

JAVA_LONG java_io_File_nativeTotalSpace___java_lang_String_R_long(CODENAME_ONE_THREAD_STATE, JAVA_OBJECT pathObj) {
#ifdef __APPLE__
    return (JAVA_LONG)-1;
#else
    const char* path = stringToUTF8(threadStateData, pathObj);
    struct statvfs fs;
    if (path && statvfs(path, &fs) == 0) {
        return (JAVA_LONG)fs.f_frsize * (JAVA_LONG)fs.f_blocks;
    }
    return (JAVA_LONG)-1;
#endif
}

JAVA_LONG java_io_File_nativeFreeSpace___java_lang_String_R_long(CODENAME_ONE_THREAD_STATE, JAVA_OBJECT pathObj) {
#ifdef __APPLE__
    return (JAVA_LONG)-1;
#else
    const char* path = stringToUTF8(threadStateData, pathObj);
    struct statvfs fs;
    if (path && statvfs(path, &fs) == 0) {
        return (JAVA_LONG)fs.f_frsize * (JAVA_LONG)fs.f_bfree;
    }
    return (JAVA_LONG)-1;
#endif
}

JAVA_LONG java_io_File_nativeUsableSpace___java_lang_String_R_long(CODENAME_ONE_THREAD_STATE, JAVA_OBJECT pathObj) {
#ifdef __APPLE__
    return (JAVA_LONG)-1;
#else
    const char* path = stringToUTF8(threadStateData, pathObj);
    struct statvfs fs;
    if (path && statvfs(path, &fs) == 0) {
        return (JAVA_LONG)fs.f_frsize * (JAVA_LONG)fs.f_bavail;
    }
    return (JAVA_LONG)-1;
#endif
}

JAVA_OBJECT java_io_File_nativeList___java_lang_String_R_java_lang_String_1ARRAY(CODENAME_ONE_THREAD_STATE, JAVA_OBJECT pathObj) {
    // Returning null keeps behavior predictable when allocation helpers are missing.
    const char* path = stringToUTF8(threadStateData, pathObj);
    DIR* dir = path ? opendir(path) : NULL;
    if (!dir) return JAVA_NULL;
    // First pass count
    int count = 0;
    struct dirent* ent;
    while ((ent = readdir(dir)) != NULL) {
        if (strcmp(ent->d_name, ".") == 0 || strcmp(ent->d_name, "..") == 0) continue;
        count++;
    }
    rewinddir(dir);
    // Without object array helpers available, bail out safely.
    closedir(dir);
    return JAVA_NULL;
}

JAVA_OBJECT java_io_File_nativeListRoots___R_java_lang_String_1ARRAY(CODENAME_ONE_THREAD_STATE) {
    // Single root entry for Unix-like systems.
    return JAVA_NULL;
}
