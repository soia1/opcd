/*
 * OPCD exec shim — minimal LD_PRELOAD library.
 *
 * On Android 10+, SELinux denies execve() on every file in an app's private
 * storage ("app_data_file", neverallow on execute_no_trans for targetSdk>=29).
 * PRoot's main binary lives in nativeLibraryDir ("apk_data_file", exec-allowed),
 * but guest binaries (busybox, apk, node, python, ...) are extracted into the
 * rootfs under app_data_file and cannot be exec'd directly.
 *
 * Fix: when an execve() target is under our app data directory, rewrite it as
 *    execve("/system/bin/linker64", ["/system/bin/linker64", target, argv...])
 * The kernel only sees /system/bin/linker64 (system_file, always allowed);
 * linker64 then mmaps the real target ELF and runs it.
 *
 * Intentionally minimal — does NOTHING else:
 *   - no path prefixing
 *   - no env stripping (LD_PRELOAD is preserved so nested execves also rewrite)
 *   - no Termux-specific behavior
 *
 * This replaces libtermux-exec.so, which bundled the same rewrite with Termux
 * path-prefixing and env-stripping that conflicted with PRoot's loader.
 */

#include <dlfcn.h>
#include <stddef.h>
#include <stdlib.h>
#include <string.h>

static const char APP_DATA_PREFIX[] = "/data/data/com.opcd.android";
static const char SYSTEM_LINKER[]   = "/system/bin/linker64";

static int starts_with(const char *s, const char *prefix) {
    while (*prefix) {
        if (*s++ != *prefix++) return 0;
    }
    return 1;
}

int execve(const char *pathname, char *const argv[], char *const envp[]) {
    static int (*real)(const char *, char *const *, char *const *) = NULL;
    if (!real) real = dlsym(RTLD_NEXT, "execve");

    if (!pathname || !starts_with(pathname, APP_DATA_PREFIX)) {
        return real(pathname, argv, envp);
    }

    int argc = 0;
    if (argv) while (argv[argc]) argc++;

    char **new_argv = (char **)malloc(((size_t)argc + 2) * sizeof(char *));
    if (!new_argv) return real(pathname, argv, envp);
    new_argv[0] = (char *)SYSTEM_LINKER;
    new_argv[1] = (char *)pathname;
    for (int i = 1; i <= argc; i++) new_argv[i + 1] = argv[i];

    int r = real(SYSTEM_LINKER, new_argv, envp);
    free(new_argv);
    return r;
}