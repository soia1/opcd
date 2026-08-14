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
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.zip.GZIPInputStream;

/**
 * Manages the Linux runtime (PRoot + Alpine Linux) and OpenCode installation.
 * All runtime files are stored in the app's private storage.
 */
public class RuntimeManager {

    private static final String TAG = "RuntimeManager";

    // URLs and versions
    private static final String PROOT_VERSION = "5.3.0";
    private static final String PROOT_URL =
            "https://github.com/proot-me/proot/releases/download/v" + PROOT_VERSION +
                    "/proot-v" + PROOT_VERSION + "-aarch64-static";

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
    private final File binDir;
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
        this.binDir = new File(runtimeDir, "bin");
        this.prootBin = new File(binDir, "proot");
    }

    public File getBaseDir() { return baseDir; }
    public File getRuntimeDir() { return runtimeDir; }
    public File getRootfsDir() { return rootfsDir; }
    public File getProotBin() { return prootBin; }

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
                    emitProgress(listener, "Downloading PRoot...");
                    downloadFile(PROOT_URL, prootBin);
                    // Android W^X policy: executable files must not be writable.
                    prootBin.setWritable(false, false);
                    prootBin.setReadable(true, false);
                    prootBin.setExecutable(true, false);
                    emitProgress(listener, "PRoot ready.");
                }

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

        File projectsDir = new File(baseDir, "projects");
        projectsDir.mkdirs();

        ProcessBuilder pb = new ProcessBuilder(
                prootBin.getAbsolutePath(),
                "-0",
                "-r", rootfsDir.getAbsolutePath(),
                "-b", "/dev",
                "-b", "/proc",
                "-b", "/sys",
                "-b", projectsDir.getAbsolutePath() + ":/root/projects",
                "-w", "/root",
                "/bin/sh",
                "-c",
                command
        );
        pb.redirectErrorStream(true);
        pb.environment().put("HOME", "/root");
        pb.environment().put("PATH", "/usr/local/bin:/usr/bin:/bin:/sbin");
        return pb.start();
    }

    /**
     * Runs a command and streams output to the listener.
     */
    public void runInAlpine(String command, SetupListener listener) throws IOException, InterruptedException {
        Process process = runInAlpine(command);
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
            String line;
            while ((line = reader.readLine()) != null) {
                emitProgress(listener, line);
            }
        }
        int exitCode = process.waitFor();
        if (exitCode != 0) {
            throw new IOException("Command exited with code " + exitCode + ": " + command);
        }
    }

    private void mkdirs() {
        runtimeDir.mkdirs();
        rootfsDir.mkdirs();
        binDir.mkdirs();
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

    private void extractTar(InputStream in, File destination) throws IOException {
        // Minimal tar extractor: supports regular files, directories, and symlinks.
        byte[] header = new byte[512];
        while (in.read(header) == 512) {
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
                in.read(longNameBytes);
                name = new String(longNameBytes, 0, indexOfNull(longNameBytes));
                long padding = (512 - (size % 512)) % 512;
                if (padding > 0) in.skip(padding);
                // Read the actual header next.
                in.read(header);
                size = parseTarSize(header, 124, 12);
                typeFlag = header[156] & 0xFF;
                linkName = parseLinkName(header);
            }

            File outFile = new File(destination, name);
            if (typeFlag == '5') {
                outFile.mkdirs();
            } else if (typeFlag == '2') {
                // Symbolic link: create a small file as placeholder.
                // In a real implementation, use NDK or Runtime.exec("ln -s").
                outFile.getParentFile().mkdirs();
                writeText(outFile, "SYMLINK:" + linkName);
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
                    // Android W^X policy: executable files must not be writable.
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

    public void shutdown() {
        executor.shutdown();
    }
}
