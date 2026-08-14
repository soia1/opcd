package com.opcd.android;

import android.content.Context;
import android.os.Build;
import android.util.Log;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.zip.GZIPInputStream;

/**
 * Manages the Linux runtime (PRoot + Alpine Linux) and OpenCode installation.
 * All runtime files are stored in the app's private storage.
 */
public class RuntimeManager {

    private static final String TAG = "RuntimeManager";

    // PRoot ships inside the APK as a native library (see scripts/fetch-proot.sh
    // and the jniLibs/arm64-v8a/libproot.so fetched in CI). Android extracts it
    // to nativeLibraryDir, the only app-owned storage SELinux allows execve()
    // from on Android 10+ (app_data_file neverallow on execute_no_trans).

    private static final String ALPINE_BRANCH = "v3.19";
    private static final String ALPINE_VERSION = "3.19.9";
    private static final String ALPINE_ARCH = "aarch64";
    private static final String ALPINE_TARBALL =
            "alpine-minirootfs-" + ALPINE_VERSION + "-" + ALPINE_ARCH + ".tar.gz";
    private static final String ALPINE_URL =
            "https://dl-cdn.alpinelinux.org/alpine/" + ALPINE_BRANCH +
                    "/releases/" + ALPINE_ARCH + "/" + ALPINE_TARBALL;

    private static final String OPENCODE_VERSION = "1.18.16";
    private static final String OPENCODE_BINARY = "opencode-linux-arm64-musl";
    private static final String OPENCODE_URL =
            "https://github.com/anomalyco/opencode/releases/download/opencode-v" +
                    OPENCODE_VERSION + "/" + OPENCODE_BINARY + ".tar.gz";

    private final Context context;
    private final File baseDir;
    private final File runtimeDir;
    private final File rootfsDir;
    private final File prootBin;
    private final ExecutorService executor = Executors.newSingleThreadExecutor();

    public interface SetupListener {
        void onProgress(String message);
        void onSetupError(String error);
        void onComplete();
    }

    public RuntimeManager(Context context) {
        this.context = context.getApplicationContext();
        this.baseDir = context.getFilesDir();
        this.runtimeDir = new File(baseDir, "runtime");
        this.rootfsDir = new File(runtimeDir, "alpine-rootfs");
        // Execute PRoot from the APK's native library directory (apk_data_file),
        // not from app private storage (app_data_file) which Android 10+ forbids.
        this.prootBin = new File(context.getApplicationInfo().nativeLibraryDir, "libproot.so");
    }

    public File getBaseDir() { return baseDir; }
    public File getRuntimeDir() { return runtimeDir; }
    public File getRootfsDir() { return rootfsDir; }
    public File getProotBin() { return prootBin; }

    /** Directory PRoot uses for its host-side temporary files (the loader). */
    public File getProotTmpDir() {
        return new File(runtimeDir, "proot-tmp");
    }

    /**
     * Builds the common PRoot argument list up to (but not including) the guest
     * program. Callers append their own guest argv (e.g. /bin/sh -c ...).
     */
    public List<String> buildProotBaseCommand() {
        File projectsDir = new File(baseDir, "projects");
        projectsDir.mkdirs();
        getProotTmpDir().mkdirs();

        List<String> cmd = new ArrayList<>();
        cmd.add(prootBin.getAbsolutePath());
        cmd.add("-0");
        cmd.add("-r"); cmd.add(rootfsDir.getAbsolutePath());
        cmd.add("-b"); cmd.add("/dev");
        cmd.add("-b"); cmd.add("/proc");
        cmd.add("-b"); cmd.add("/sys");
        // Android 10+: the dynamic linker /system/bin/linker64 is a symlink to
        // /apex/com.android.runtime/bin/linker64, so /apex must be visible.
        if (new File("/apex").exists()) {
            cmd.add("-b"); cmd.add("/apex");
        }
        cmd.add("-b"); cmd.add(projectsDir.getAbsolutePath() + ":/root/projects");
        cmd.add("-w"); cmd.add("/root");
        return cmd;
    }

    /** Applies the shared environment variables needed by PRoot + the guest. */
    public void configureProotEnv(ProcessBuilder pb) {
        pb.redirectErrorStream(true);
        pb.environment().put("HOME", "/root");
        pb.environment().put("PATH", "/usr/local/bin:/usr/bin:/bin:/sbin");
        // PRoot 5.x reads PROOT_TMP_DIR (the name it prints in its error
        // message). Older builds recognize PROOT_TMPDIR. Set both so the
        // writable app-private temp dir is used; otherwise proot falls back to
        // host /tmp, which apps cannot write to.
        File tmp = getProotTmpDir();
        if (!tmp.isDirectory()) tmp.mkdirs();
        pb.environment().put("PROOT_TMP_DIR", tmp.getAbsolutePath());
        pb.environment().put("PROOT_TMPDIR", tmp.getAbsolutePath());

        String nativeLibDir = context.getApplicationInfo().nativeLibraryDir;
        // libtermux-exec.so is preloaded into every process proot spawns and
        // intercepts execve() of any path under our app's data directory,
        // rewriting it as execve("/system/bin/linker64", ...). The kernel then
        // only ever sees the system linker execute, which is allowed; linker64
        // mmaps the actual guest ELF. This is the same technique Termux uses
        // to run its proot-distro on Android 10+, and it is the only way to
        // execute guest binaries that live in app_data_file (the rootfs).
        pb.environment().put("LD_PRELOAD", nativeLibDir + "/libtermux-exec.so");
        // Tell proot to use the loader ELF we ship inside nativeLibraryDir
        // (apk_data_file) instead of writing one to PROOT_TMP_DIR and trying
        // to exec it from app_data_file -- which the kernel denies.
        pb.environment().put("PROOT_LOADER", nativeLibDir + "/libproot-loader.so");
    }

    /**
     * Returns true if PRoot binary exists and is executable.
     */
    public boolean isProotInstalled() {
        return prootBin.exists() && prootBin.canExecute();
    }

    /**
     * Returns true if Alpine rootfs has been extracted and configured.
     */
    public boolean isAlpineInstalled() {
        File marker = new File(rootfsDir, ".setup-done");
        return marker.exists();
    }

    /**
     * Returns true if OpenCode binary exists inside the rootfs.
     */
    public boolean isOpenCodeInstalled() {
        File openCodeBin = new File(rootfsDir, "usr/local/bin/opencode");
        return openCodeBin.exists() && openCodeBin.canExecute();
    }

    /**
     * Returns true if the entire runtime is ready to use.
     */
    public boolean isRuntimeReady() {
        return isProotInstalled() && isAlpineInstalled() && isOpenCodeInstalled();
    }

    /**
     * Starts the full setup process in background.
     */
    public void setupRuntime(SetupListener listener) {
        executor.execute(() -> {
            try {
                emitProgress(listener, "Creating directories...");
                mkdirs();

                if (!isProotInstalled()) {
                    // PRoot is shipped inside the APK as a native library and must
                    // already exist, executable, in nativeLibraryDir. It cannot be
                    // downloaded at runtime because Android 10+ SELinux forbids
                    // executing any file from app private storage.
                    throw new IOException("PRoot native library missing: "
                            + prootBin.getAbsolutePath()
                            + ". Reinstall the app (APK may be wrong ABI).");
                }
                emitProgress(listener, "PRoot ready (" + prootBin.getAbsolutePath() + ").");

                if (!isAlpineInstalled()) {
                    emitProgress(listener, "Downloading Alpine Linux...");
                    File alpineTar = new File(runtimeDir, ALPINE_TARBALL);
                    downloadFile(ALPINE_URL, alpineTar);

                    emitProgress(listener, "Extracting Alpine Linux...");
                    extractTarGz(alpineTar, rootfsDir);
                    alpineTar.delete();

                    emitProgress(listener, "Configuring Alpine...");
                    configureAlpine();

                    new File(rootfsDir, ".setup-done").createNewFile();
                    emitProgress(listener, "Alpine Linux ready.");
                }

                if (!areBaseToolsInstalled()) {
                    emitProgress(listener, "Installing base tools (Node.js, npm, Python, Git)...");
                    runInAlpine("apk update && apk add --no-cache nodejs npm python3 py3-pip git bash curl wget", listener);
                    emitProgress(listener, "Base tools installed.");
                }

                if (!isOpenCodeInstalled()) {
                    emitProgress(listener, "Installing OpenCode...");
                    installOpenCode(listener);
                    emitProgress(listener, "OpenCode installed.");
                }

                emitProgress(listener, "Runtime setup complete.");
                emitComplete(listener);

            } catch (Exception e) {
                Log.e(TAG, "Setup failed", e);
                logDiagnostics();
                emitError(listener, "Setup failed: " + e.getMessage());
            }
        });
    }

    /**
     * Runs a command inside the Alpine environment via PRoot.
     */
    public Process runInAlpine(String command) throws IOException {
        if (!isProotInstalled() || !isAlpineInstalled()) {
            throw new IOException("Linux runtime is not installed");
        }

        List<String> cmd = buildProotBaseCommand();
        cmd.add("/bin/sh");
        cmd.add("-c");
        cmd.add(command);

        ProcessBuilder pb = new ProcessBuilder(cmd);
        configureProotEnv(pb);
        return pb.start();
    }

    /**
     * Runs a command and streams output to the listener.
     * On non-zero exit, throws an IOException that includes the captured output
     * so the user can see why apk/proot/etc. failed (e.g. "Could not resolve host").
     */
    public void runInAlpine(String command, SetupListener listener) throws IOException, InterruptedException {
        Process process = runInAlpine(command);
        StringBuilder output = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
            String line;
            while ((line = reader.readLine()) != null) {
                emitProgress(listener, line);
                output.append(line).append('\n');
            }
        }
        int exitCode = process.waitFor();
        if (exitCode != 0) {
            String msg = "Command exited with code " + exitCode + ": " + command;
            String captured = output.toString().trim();
            if (!captured.isEmpty()) {
                int max = 2000;
                if (captured.length() > max) captured = "..." + captured.substring(captured.length() - max);
                msg += "\n\n--- output ---\n" + captured;
            }
            throw new IOException(msg);
        }
    }

    private void mkdirs() {
        runtimeDir.mkdirs();
        rootfsDir.mkdirs();
        new File(runtimeDir, "proot-tmp").mkdirs();
    }

    private void configureAlpine() throws IOException {
        // DNS
        File resolv = new File(rootfsDir, "etc/resolv.conf");
        writeText(resolv, "nameserver 8.8.8.8\nnameserver 1.1.1.1\n");

        // APK repositories
        File repos = new File(rootfsDir, "etc/apk/repositories");
        writeText(repos,
                "https://dl-cdn.alpinelinux.org/alpine/" + ALPINE_BRANCH + "/main\n" +
                        "https://dl-cdn.alpinelinux.org/alpine/" + ALPINE_BRANCH + "/community\n");
    }

    private boolean areBaseToolsInstalled() {
        return new File(rootfsDir, "usr/bin/node").exists()
                && new File(rootfsDir, "usr/bin/npm").exists()
                && new File(rootfsDir, "usr/bin/git").exists();
    }

    private void installOpenCode(SetupListener listener) throws IOException, InterruptedException {
        File tempTar = new File(runtimeDir, "opencode.tar.gz");

        // Try GitHub release first, fallback to npm registry.
        boolean downloaded = false;
        try {
            emitProgress(listener, "Downloading OpenCode from GitHub...");
            downloadFile(OPENCODE_URL, tempTar);
            downloaded = true;
        } catch (IOException e) {
            emitProgress(listener, "GitHub download failed, trying npm registry...");
            String npmUrl = "https://registry.npmjs.org/opencode-ai/-/" + OPENCODE_BINARY + "-" + OPENCODE_VERSION + ".tgz";
            downloadFile(npmUrl, tempTar);
            downloaded = true;
        }

        if (!downloaded || !tempTar.exists() || tempTar.length() == 0) {
            throw new IOException("OpenCode download failed from all sources");
        }

        // Extract to /usr/local/bin inside rootfs.
        File binDir = new File(rootfsDir, "usr/local/bin");
        binDir.mkdirs();
        extractTarGz(tempTar, binDir);
        tempTar.delete();

        File openCodeBin = new File(binDir, "opencode");
        if (!openCodeBin.exists()) {
            // Maybe the tarball contained the binary with platform name.
            File platformBin = new File(binDir, OPENCODE_BINARY);
            if (platformBin.exists()) {
                platformBin.renameTo(openCodeBin);
            } else {
                throw new IOException("OpenCode binary not found after extraction");
            }
        }
        openCodeBin.setExecutable(true, false);
    }

    private void downloadFile(String urlString, File destination) throws IOException {
        URL url = new URL(urlString);
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection.setConnectTimeout(30000);
        connection.setReadTimeout(120000);
        connection.setRequestProperty("User-Agent", "OPCD-Android/0.1.0");

        try (InputStream in = connection.getInputStream();
             OutputStream out = new FileOutputStream(destination)) {
            byte[] buffer = new byte[8192];
            int read;
            while ((read = in.read(buffer)) != -1) {
                out.write(buffer, 0, read);
            }
        } finally {
            connection.disconnect();
        }
    }

    private void extractTarGz(File tarGz, File destination) throws IOException {
        try (FileInputStream fis = new FileInputStream(tarGz);
             GZIPInputStream gzis = new GZIPInputStream(fis)) {
            // Simple tar extraction (no external tar binary needed).
            extractTar(gzis, destination);
        }
    }

    /** Reads exactly buf.length bytes; throws EOFException if the stream ends early. */
    private static void readFully(InputStream in, byte[] buf) throws IOException {
        int off = 0;
        while (off < buf.length) {
            int r = in.read(buf, off, buf.length - off);
            if (r == -1) throw new IOException("Unexpected EOF in tar stream");
            off += r;
        }
    }

    /** Reads up to 512 bytes into buf; returns 512 on success, -1 at clean EOF, or a partial count. */
    private static int readBlockOrEof(InputStream in, byte[] buf) throws IOException {
        int off = 0;
        while (off < 512) {
            int r = in.read(buf, off, 512 - off);
            if (r == -1) return off == 0 ? -1 : off;
            off += r;
        }
        return 512;
    }

    private void extractTar(InputStream in, File destination) throws IOException {
        // Minimal tar extractor: supports regular files, directories, and symlinks.
        byte[] header = new byte[512];
        while (readBlockOrEof(in, header) == 512) {
            if (isNullBlock(header)) {
                continue;
            }

            String name = parseTarName(header);
            long size = parseTarSize(header, 124, 12);
            int typeFlag = header[156] & 0xFF;
            String linkName = parseLinkName(header);

            // Handle long names via GNU ././@LongLink.
            if (name.equals("././@LongLink")) {
                byte[] longNameBytes = new byte[(int) size];
                readFully(in, longNameBytes);
                name = new String(longNameBytes, 0, indexOfNull(longNameBytes));
                long padding = (512 - (size % 512)) % 512;
                if (padding > 0) in.skip(padding);
                // Read the actual header next.
                if (readBlockOrEof(in, header) != 512) break;
                size = parseTarSize(header, 124, 12);
                typeFlag = header[156] & 0xFF;
                linkName = parseLinkName(header);
            }

            // Guard against path traversal (../) outside the rootfs.
            if (name.startsWith("/") || name.contains("..")) {
                // Skip absolute or escaping entries safely.
                long pad = (512 - (size % 512)) % 512;
                if (pad > 0) in.skip(pad);
                continue;
            }

            File outFile = new File(destination, name);
            if (typeFlag == '5') {
                outFile.mkdirs();
            } else if (typeFlag == '2') {
                // Symbolic link: create a real symlink (needed for busybox applets
                // like /bin/sh -> /bin/busybox). Anchoring absolute targets inside
                // the rootfs keeps extraction within the app's tree.
                outFile.getParentFile().mkdirs();
                if (outFile.exists()) outFile.delete();
                Path link = outFile.toPath();
                Path target;
                if (linkName.startsWith("/")) {
                    Path resolved = destination.toPath().resolve(linkName.substring(1));
                    target = link.getParent().relativize(resolved);
                } else {
                    target = Paths.get(linkName);
                }
                try {
                    Files.createSymbolicLink(link, target);
                } catch (IOException e) {
                    writeText(outFile, linkName);
                }
            } else if (typeFlag == '0' || typeFlag == '\0') {
                outFile.getParentFile().mkdirs();
                try (OutputStream out = new FileOutputStream(outFile)) {
                    long remaining = size;
                    byte[] buffer = new byte[8192];
                    while (remaining > 0) {
                        int toRead = (int) Math.min(buffer.length, remaining);
                        int read = in.read(buffer, 0, toRead);
                        if (read == -1) break;
                        out.write(buffer, 0, read);
                        remaining -= read;
                    }
                }
                int mode = (int) parseTarSize(header, 100, 8);
                if ((mode & 0100) != 0) {
                    // Android W^X: executable files kept read-only + executable.
                    outFile.setWritable(false, false);
                    outFile.setReadable(true, false);
                    outFile.setExecutable(true, false);
                }
            }

            // Skip padding to next 512-byte boundary.
            long padding = (512 - (size % 512)) % 512;
            if (padding > 0) {
                in.skip(padding);
            }
        }
    }

    private String parseLinkName(byte[] header) {
        StringBuilder sb = new StringBuilder();
        for (int i = 157; i < 257; i++) {
            if (header[i] == 0) break;
            sb.append((char) header[i]);
        }
        return sb.toString();
    }

    private int indexOfNull(byte[] bytes) {
        for (int i = 0; i < bytes.length; i++) {
            if (bytes[i] == 0) return i;
        }
        return bytes.length;
    }

    private boolean isNullBlock(byte[] header) {
        for (byte b : header) {
            if (b != 0) return false;
        }
        return true;
    }

    private String parseTarName(byte[] header) {
        // Basic name parsing (null-terminated string at offset 0, length 100).
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < 100; i++) {
            if (header[i] == 0) break;
            sb.append((char) header[i]);
        }
        return sb.toString();
    }

    private long parseTarSize(byte[] header, int offset, int length) {
        long size = 0;
        for (int i = offset; i < offset + length; i++) {
            byte b = header[i];
            if (b == 0 || b == ' ') break;
            size = (size << 3) + (b - '0');
        }
        return size;
    }

    private void writeText(File file, String text) throws IOException {
        file.getParentFile().mkdirs();
        try (FileOutputStream out = new FileOutputStream(file)) {
            out.write(text.getBytes());
        }
    }

    private void emitProgress(SetupListener listener, String message) {
        Log.i(TAG, message);
        if (listener != null) {
            context.getMainExecutor().execute(() -> listener.onProgress(message));
        }
    }

    private void emitError(SetupListener listener, String error) {
        Log.e(TAG, error);
        if (listener != null) {
            context.getMainExecutor().execute(() -> listener.onSetupError(error));
        }
    }

    private void emitComplete(SetupListener listener) {
        if (listener != null) {
            context.getMainExecutor().execute(() -> listener.onComplete());
        }
    }

    /** Logs PRoot path + SELinux context to logcat to aid debugging exec failures. */
    private void logDiagnostics() {
        try {
            Log.e(TAG, "diag: nativeLibraryDir=" + context.getApplicationInfo().nativeLibraryDir);
            Log.e(TAG, "diag: prootBin=" + prootBin.getAbsolutePath()
                    + " exists=" + prootBin.exists() + " canExec=" + prootBin.canExecute());
            File parent = prootBin.getParentFile();
            File prootTmp = getProotTmpDir();
            File rootfsBin = new File(rootfsDir, "bin/busybox");
            File rootfsSh = new File(rootfsDir, "bin/sh");
            ProcessBuilder pb = new ProcessBuilder("/system/bin/ls", "-ldZ",
                    prootBin.getAbsolutePath(),
                    parent != null ? parent.getAbsolutePath() : "/",
                    prootTmp.getAbsolutePath(),
                    rootfsBin.getAbsolutePath(),
                    rootfsSh.getAbsolutePath());
            pb.redirectErrorStream(true);
            try (BufferedReader r = new BufferedReader(
                    new InputStreamReader(pb.start().getInputStream()))) {
                String line;
                while ((line = r.readLine()) != null) Log.e(TAG, "diag ls -ldZ: " + line);
            }
        } catch (Exception ignored) {
            // Best-effort diagnostics.
        }
    }

    public void shutdown() {
        executor.shutdown();
    }
}
