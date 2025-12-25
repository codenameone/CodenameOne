#include "cn1_globals.h"
#include "java_io_FileInputStream.h"
#include "java_io_FileOutputStream.h"
#include "java_lang_String.h"

#if defined(__APPLE__) && defined(__OBJC__)
#import <Foundation/Foundation.h>
#include <stdint.h>
#include <limits.h>
#include <string.h>

static NSFileHandle* toHandle(JAVA_LONG handle) {
    return (NSFileHandle*)(uintptr_t)handle;
}

static JAVA_LONG retainHandle(NSFileHandle* handle) {
    if (handle == nil) {
        return 0;
    }
    [handle retain];
    return (JAVA_LONG)(uintptr_t)handle;
}

JAVA_LONG java_io_FileInputStream_openImpl___java_lang_String_R_long(CODENAME_ONE_THREAD_STATE, JAVA_OBJECT __cn1ThisObject, JAVA_OBJECT path) {
    if (path == JAVA_NULL) {
        return 0;
    }
    NSAutoreleasePool* pool = [[NSAutoreleasePool alloc] init];
    NSString* p = toNSString(CN1_THREAD_STATE_PASS_ARG path);
    NSFileHandle* handle = [NSFileHandle fileHandleForReadingAtPath:p];
    JAVA_LONG result = retainHandle(handle);
    [pool release];
    return result;
}

JAVA_INT java_io_FileInputStream_readImpl___long_byte_1ARRAY_int_int_R_int(CODENAME_ONE_THREAD_STATE, JAVA_OBJECT __cn1ThisObject, JAVA_LONG handle, JAVA_OBJECT buffer, JAVA_INT off, JAVA_INT len) {
    NSFileHandle* h = toHandle(handle);
    if (h == nil) {
        return -1;
    }
    NSAutoreleasePool* pool = [[NSAutoreleasePool alloc] init];
    NSData* data = [h readDataOfLength:len];
    NSUInteger count = [data length];
    if (count > 0 && buffer != JAVA_NULL) {
        memcpy(((JAVA_ARRAY_BYTE*)(*(JAVA_ARRAY)buffer).data) + off, [data bytes], count);
    }
    [pool release];
    return count == 0 ? -1 : (JAVA_INT)count;
}

JAVA_LONG java_io_FileInputStream_skipImpl___long_long_R_long(CODENAME_ONE_THREAD_STATE, JAVA_OBJECT __cn1ThisObject, JAVA_LONG handle, JAVA_LONG n) {
    NSFileHandle* h = toHandle(handle);
    if (h == nil) {
        return 0;
    }
    unsigned long long current = [h offsetInFile];
    unsigned long long target = current + (unsigned long long)n;
    [h seekToFileOffset:target];
    unsigned long long updated = [h offsetInFile];
    if (updated < current) {
        return 0;
    }
    return (JAVA_LONG)(updated - current);
}

JAVA_INT java_io_FileInputStream_availableImpl___long_R_int(CODENAME_ONE_THREAD_STATE, JAVA_OBJECT __cn1ThisObject, JAVA_LONG handle) {
    NSFileHandle* h = toHandle(handle);
    if (h == nil) {
        return 0;
    }
    NSAutoreleasePool* pool = [[NSAutoreleasePool alloc] init];
    unsigned long long current = [h offsetInFile];
    unsigned long long end = [h seekToEndOfFile];
    [h seekToFileOffset:current];
    [pool release];
    if (end <= current) {
        return 0;
    }
    unsigned long long diff = end - current;
    if (diff > (unsigned long long)INT_MAX) {
        return INT_MAX;
    }
    return (JAVA_INT)diff;
}

JAVA_VOID java_io_FileInputStream_closeImpl___long(CODENAME_ONE_THREAD_STATE, JAVA_OBJECT __cn1ThisObject, JAVA_LONG handle) {
    NSFileHandle* h = toHandle(handle);
    if (h == nil) {
        return;
    }
    NSAutoreleasePool* pool = [[NSAutoreleasePool alloc] init];
    [h closeFile];
    [h release];
    [pool release];
}

JAVA_LONG java_io_FileOutputStream_openImpl___java_lang_String_boolean_R_long(CODENAME_ONE_THREAD_STATE, JAVA_OBJECT __cn1ThisObject, JAVA_OBJECT path, JAVA_BOOLEAN append) {
    if (path == JAVA_NULL) {
        return 0;
    }
    NSAutoreleasePool* pool = [[NSAutoreleasePool alloc] init];
    NSString* p = toNSString(CN1_THREAD_STATE_PASS_ARG path);
    NSFileManager* manager = [NSFileManager defaultManager];
    if (![manager fileExistsAtPath:p]) {
        [manager createFileAtPath:p contents:nil attributes:nil];
    } else if (!append) {
        [manager removeItemAtPath:p error:NULL];
        [manager createFileAtPath:p contents:nil attributes:nil];
    }
    NSFileHandle* handle = [NSFileHandle fileHandleForWritingAtPath:p];
    if (append && handle != nil) {
        [handle seekToEndOfFile];
    }
    JAVA_LONG result = retainHandle(handle);
    [pool release];
    return result;
}

JAVA_VOID java_io_FileOutputStream_writeImpl___long_byte_1ARRAY_int_int(CODENAME_ONE_THREAD_STATE, JAVA_OBJECT __cn1ThisObject, JAVA_LONG handle, JAVA_OBJECT buffer, JAVA_INT off, JAVA_INT len) {
    NSFileHandle* h = toHandle(handle);
    if (h == nil || buffer == JAVA_NULL) {
        return;
    }
    NSAutoreleasePool* pool = [[NSAutoreleasePool alloc] init];
    NSData* data = [NSData dataWithBytes:((JAVA_ARRAY_BYTE*)(*(JAVA_ARRAY)buffer).data) + off length:len];
    [h writeData:data];
    [pool release];
}

JAVA_VOID java_io_FileOutputStream_flushImpl___long(CODENAME_ONE_THREAD_STATE, JAVA_OBJECT __cn1ThisObject, JAVA_LONG handle) {
    NSFileHandle* h = toHandle(handle);
    if (h == nil) {
        return;
    }
    NSAutoreleasePool* pool = [[NSAutoreleasePool alloc] init];
    [h synchronizeFile];
    [pool release];
}

JAVA_VOID java_io_FileOutputStream_closeImpl___long(CODENAME_ONE_THREAD_STATE, JAVA_OBJECT __cn1ThisObject, JAVA_LONG handle) {
    NSFileHandle* h = toHandle(handle);
    if (h == nil) {
        return;
    }
    NSAutoreleasePool* pool = [[NSAutoreleasePool alloc] init];
    [h closeFile];
    [h release];
    [pool release];
}

#else

#include <fcntl.h>
#include <limits.h>
#include <string.h>
#include <sys/stat.h>
#include <unistd.h>
#include "xmlvm.h"

static int toFd(JAVA_LONG handle) {
    return (int)handle;
}

JAVA_LONG java_io_FileInputStream_openImpl___java_lang_String_R_long(CODENAME_ONE_THREAD_STATE, JAVA_OBJECT __cn1ThisObject, JAVA_OBJECT path) {
    if (path == JAVA_NULL) {
        return 0;
    }
    const char* p = stringToUTF8(threadStateData, path);
    int fd = open(p, O_RDONLY);
    if (fd < 0) {
        return 0;
    }
    return (JAVA_LONG)fd;
}

JAVA_INT java_io_FileInputStream_readImpl___long_byte_1ARRAY_int_int_R_int(CODENAME_ONE_THREAD_STATE, JAVA_OBJECT __cn1ThisObject, JAVA_LONG handle, JAVA_OBJECT buffer, JAVA_INT off, JAVA_INT len) {
    int fd = toFd(handle);
    if (fd < 0 || buffer == JAVA_NULL) {
        return -1;
    }
    ssize_t count = read(fd, ((JAVA_ARRAY_BYTE*)(*(JAVA_ARRAY)buffer).data) + off, len);
    if (count <= 0) {
        return -1;
    }
    return (JAVA_INT)count;
}

JAVA_LONG java_io_FileInputStream_skipImpl___long_long_R_long(CODENAME_ONE_THREAD_STATE, JAVA_OBJECT __cn1ThisObject, JAVA_LONG handle, JAVA_LONG n) {
    int fd = toFd(handle);
    if (fd < 0 || n <= 0) {
        return 0;
    }
    off_t current = lseek(fd, 0, SEEK_CUR);
    if (current == (off_t)-1) {
        return 0;
    }
    off_t target = lseek(fd, (off_t)n, SEEK_CUR);
    if (target == (off_t)-1 || target < current) {
        return 0;
    }
    return (JAVA_LONG)(target - current);
}

JAVA_INT java_io_FileInputStream_availableImpl___long_R_int(CODENAME_ONE_THREAD_STATE, JAVA_OBJECT __cn1ThisObject, JAVA_LONG handle) {
    int fd = toFd(handle);
    if (fd < 0) {
        return 0;
    }
    off_t current = lseek(fd, 0, SEEK_CUR);
    if (current == (off_t)-1) {
        return 0;
    }
    off_t end = lseek(fd, 0, SEEK_END);
    if (end == (off_t)-1) {
        return 0;
    }
    lseek(fd, current, SEEK_SET);
    if (end <= current) {
        return 0;
    }
    off_t diff = end - current;
    if (diff > (off_t)INT_MAX) {
        return INT_MAX;
    }
    return (JAVA_INT)diff;
}

JAVA_VOID java_io_FileInputStream_closeImpl___long(CODENAME_ONE_THREAD_STATE, JAVA_OBJECT __cn1ThisObject, JAVA_LONG handle) {
    int fd = toFd(handle);
    if (fd >= 0) {
        close(fd);
    }
}

JAVA_LONG java_io_FileOutputStream_openImpl___java_lang_String_boolean_R_long(CODENAME_ONE_THREAD_STATE, JAVA_OBJECT __cn1ThisObject, JAVA_OBJECT path, JAVA_BOOLEAN append) {
    if (path == JAVA_NULL) {
        return 0;
    }
    const char* p = stringToUTF8(threadStateData, path);
    int flags = O_WRONLY | O_CREAT;
    if (append) {
        flags |= O_APPEND;
    } else {
        flags |= O_TRUNC;
    }
    int fd = open(p, flags, 0666);
    if (fd < 0) {
        return 0;
    }
    return (JAVA_LONG)fd;
}

JAVA_VOID java_io_FileOutputStream_writeImpl___long_byte_1ARRAY_int_int(CODENAME_ONE_THREAD_STATE, JAVA_OBJECT __cn1ThisObject, JAVA_LONG handle, JAVA_OBJECT buffer, JAVA_INT off, JAVA_INT len) {
    int fd = toFd(handle);
    if (fd < 0 || buffer == JAVA_NULL || len <= 0) {
        return;
    }
    ssize_t written = write(fd, ((JAVA_ARRAY_BYTE*)(*(JAVA_ARRAY)buffer).data) + off, len);
    (void)written;
}

JAVA_VOID java_io_FileOutputStream_flushImpl___long(CODENAME_ONE_THREAD_STATE, JAVA_OBJECT __cn1ThisObject, JAVA_LONG handle) {
    int fd = toFd(handle);
    if (fd >= 0) {
        fsync(fd);
    }
}

JAVA_VOID java_io_FileOutputStream_closeImpl___long(CODENAME_ONE_THREAD_STATE, JAVA_OBJECT __cn1ThisObject, JAVA_LONG handle) {
    int fd = toFd(handle);
    if (fd >= 0) {
        close(fd);
    }
}

#endif
