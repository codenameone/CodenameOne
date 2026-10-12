/* Portable child fixture for the JavaSE/native process conformance test. */
#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#ifdef _WIN32
#include <windows.h>
#include <io.h>
#include <fcntl.h>
#include <direct.h>
#define getcwd _getcwd
#define nap() Sleep(100)
#else
#include <unistd.h>
#define nap() usleep(100000)
#endif
static int run(int argc, char **argv) {
    int i;
    if (argc < 2) return 99;
    if (!strcmp(argv[1], "args")) {
        for (i = 2; i < argc; ++i) { fwrite(argv[i], 1, strlen(argv[i]), stdout); fputc(0, stdout); }
    } else if (!strcmp(argv[1], "env")) {
        const char *value = getenv("CN1_PROCESS_TEST");
        printf("%s", value ? value : "<absent>");
    } else if (!strcmp(argv[1], "cwd")) {
        char path[4096];
        if (!getcwd(path, sizeof(path))) return 98;
        printf("%s", path);
    } else if (!strcmp(argv[1], "duplex")) {
        unsigned char data[4096]; size_t n;
        memset(data, 37, sizeof(data));
        for (i = 0; i < 128; ++i) { fwrite(data, 1, sizeof(data), stdout); fwrite(data, 1, sizeof(data), stderr); }
        while ((n = fread(data, 1, sizeof(data), stdin)) != 0) fwrite(data, 1, n, stdout);
    } else if (!strcmp(argv[1], "sleep")) {
        for (;;) nap();
    } else if (!strcmp(argv[1], "exit")) {
        return argc > 2 ? atoi(argv[2]) : 7;
    } else return 97;
    return 0;
}
#ifdef _WIN32
int wmain(int argc, wchar_t **wide) {
    int i, code;
    char **argv = (char **)calloc((size_t)argc + 1, sizeof(char *));
    _setmode(_fileno(stdin), _O_BINARY); _setmode(_fileno(stdout), _O_BINARY); _setmode(_fileno(stderr), _O_BINARY);
    for (i = 0; i < argc; ++i) {
        int n = WideCharToMultiByte(CP_UTF8, 0, wide[i], -1, NULL, 0, NULL, NULL);
        argv[i] = (char *)malloc((size_t)n);
        WideCharToMultiByte(CP_UTF8, 0, wide[i], -1, argv[i], n, NULL, NULL);
    }
    code = run(argc, argv);
    for (i = 0; i < argc; ++i) free(argv[i]);
    free(argv); return code;
}
#else
int main(int argc, char **argv) { return run(argc, argv); }
#endif
