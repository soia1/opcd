# OPCD Android — Milestone 1 Research Report

## 1. Reference Project Analysis: `Victozee26/acode-opencode`

- **Type:** Acode plugin.
- **Workflow:** Check → Install → Serve → Render.
- **How it works:**
  - Runs inside Acode's built-in Alpine Linux terminal.
  - Installs OpenCode via `npm install -g opencode-ai`.
  - Starts OpenCode server with `opencode serve --port 4096 --hostname 127.0.0.1`.
  - Embeds the OpenCode web UI in a full-page iframe inside Acode.
- **Key insight:** The plugin proves that OpenCode can run locally on Android if a Linux environment with Node.js is available.
- **Our goal:** Remove the Acode dependency and provide the Linux runtime ourselves.

## 2. OpenCode Analysis

### Official Source

- **Repository:** `anomalyco/opencode`
- **License:** MIT
- **Latest npm package:** `opencode-ai@1.18.16`
- **Web UI package:** `packages/console/app` (SolidJS + Vite)
- **CLI package:** `packages/cli`

### Serve Command

OpenCode 2.0 includes a `serve` command:

```typescript
Spec.make("serve", {
  description: "Start the v2 API server",
  params: {
    hostname: Flag.string("hostname").pipe(Flag.withDefault("127.0.0.1")),
    port: Flag.integer("port").pipe(Flag.optional()),
    register: Flag.boolean("register").pipe(Flag.withDefault(false)),
  },
})
```

This confirms the command used by `acode-opencode` is valid:

```bash
opencode serve --hostname 127.0.0.1 --port 4096
```

### Binary Distribution

The `opencode-ai` npm package is a wrapper that downloads platform-specific binaries:

- `opencode-linux-arm64`
- `opencode-linux-arm64-musl`
- `opencode-darwin-arm64`
- `opencode-windows-arm64`
- etc.

For Android ARM64, the target binary is `opencode-linux-arm64-musl` because:

- Android uses Bionic libc, not glibc.
- The glibc binary (`opencode-linux-arm64`) will likely fail on Android.
- The musl binary is statically linked against musl libc and has a higher chance of running on Android.

**Risk:** Android is not officially supported by the npm package. The musl binary must be tested on Android to confirm it runs.

### Other OpenCode Projects

- `opencode-ai/opencode` — archived on Sep 18, 2025, moved to Crush.
- `charmbracelet/crush` — continuation under FSL-1.1-MIT license, TUI-first.
- We will use `anomalyco/opencode` as the official source.

## 3. Linux Runtime on Android

### Problem

Android is not a standard Linux distribution:

- Uses Bionic libc.
- Has different file system layout.
- No root access by default.
- Strict SELinux policies.

### Solutions

| Approach | Requires Root | Notes |
|----------|--------------|-------|
| chroot | Yes | Not suitable for most users. |
| PRoot | No | User-space chroot, works on Android. |
| Termux | No | External app, excluded by PRD. |
| UserLAnd | No | External app, excluded by PRD. |
| Andronix | No | Uses Termux + PRoot, excluded. |

### Recommended Approach

Use **PRoot** + **Alpine Linux rootfs** embedded inside the app.

**Why Alpine Linux:**

- Very small (~5 MB base rootfs).
- Uses musl libc, matching the OpenCode musl binary.
- Fast package manager (`apk`).
- Easy to get Node.js, npm, Python, pip, Git.

**Why PRoot:**

- No root required.
- Can chroot into an extracted rootfs.
- Used successfully by Termux, UserLAnd, Andronix, AnLinux.

### Runtime Components

Inside Alpine Linux rootfs, install via `apk`:

- `nodejs`
- `npm`
- `python3`
- `py3-pip`
- `git`
- `bash`
- `curl` / `wget`

OpenCode will be installed/updated separately by downloading the musl ARM64 binary or the npm package.

## 4. Android WebView

- Android WebView is based on Chromium.
- Supports modern JavaScript, WebSocket, localStorage, etc.
- Can load `http://localhost:4096` served by OpenCode.
- For Android 8.0+ (API 26), WebView is updated independently via Play Store.
- Required settings:
  - Enable JavaScript.
  - Enable DOM storage.
  - Allow mixed content if needed.
  - Configure WebViewClient to handle navigation within the app.

## 5. Target Platform

- **Minimum Android version:** API 26 (Android 8.0).
  - Ensures modern WebView.
  - Aligns with current Android ecosystem.
- **Primary architecture:** ARM64.
  - Most modern Android phones use ARM64.
  - OpenCode provides ARM64 musl binary.
- **Future support:** ARMv7 and x86_64 can be considered later.

## 6. Project Structure

Created initial structure:

```
/public/1
├── AGENTS.md
├── OPCD_Android_PRD.md
├── task.md
├── README.md
├── LICENSE
├── CHANGELOG.md
├── CONTRIBUTING.md
├── docs/
│   ├── architecture.md
│   └── research.md
├── src/
│   ├── android/
│   ├── linux-runtime/
│   ├── opencode-integration/
│   ├── file-manager/
│   ├── code-editor/
│   └── terminal/
├── android/
└── scripts/
```

## 7. Open Questions & Risks

1. **OpenCode musl binary on Android:** Must verify it runs on Android via PRoot + Alpine.
2. **PRoot compilation for Android:** Need to build or bundle PRoot binary for Android ARM64.
3. **Storage Access Framework integration:** Need to expose selected project folders to PRoot.
4. **Android background process limits:** Android 12+ may kill background processes. Need foreground service.
5. **APK size:** Rootfs + tools may be large. Consider downloading on first run.
6. **WebView compatibility:** Some OpenCode UI features may need testing on mobile WebView.

## 8. Recommended Next Steps

1. Build a Proof of Concept (POC) that:
   - Bundles or downloads PRoot binary for Android.
   - Extracts Alpine Linux rootfs.
   - Starts Alpine shell via PRoot.
   - Installs Node.js.
   - Installs OpenCode musl binary.
   - Runs `opencode serve`.
   - Loads the UI in Android WebView.
2. Validate OpenCode web UI works on Android WebView.
3. Measure APK size and first-run download size.
