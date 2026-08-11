# OPCD Android — Architecture

## High-Level Architecture

```text
                    OPCD Android
                         │
          ┌──────────────┴──────────────┐
          │                             │
 Android Native Layer           Linux Runtime
          │                             │
          │                    ┌────────┴────────┐
          │                    │                 │
          │                 Node.js           Tools
          │                    │
          │                 OpenCode
          │                    │
          └──────────────► Local Server
                               │
                               ▼
                          OpenCode UI
```

## Android Native Layer

Responsible for:

- Application lifecycle.
- Android permissions (storage, internet, notifications).
- Storage access via Android Storage Access Framework (SAF).
- Linux runtime lifecycle management.
- Process management.
- WebView integration to display OpenCode UI.
- File access and project selection.

## Linux Runtime Layer

Responsible for:

- Shell environment.
- Node.js and npm.
- Python and pip.
- Git.
- OpenCode binary.
- User-installed packages.

### Runtime Engine: PRoot

We use [PRoot](https://github.com/proot-me/proot) as the user-space chroot engine. It does not require root access, making it suitable for Android.

### Base Distribution: Alpine Linux

We use Alpine Linux rootfs because:

- Small size (~5 MB base).
- Uses musl libc.
- OpenCode provides a `musl` ARM64 binary (`opencode-linux-arm64-musl`).
- Fast package manager (`apk`).

## OpenCode Layer

Responsible for:

- AI agent logic.
- Coding sessions.
- AI providers and models.
- Tools integration.
- Project management.
- OpenCode web UI.

OpenCode is started with:

```bash
opencode serve --hostname 127.0.0.1 --port 4096
```

The web UI is then loaded in the Android WebView via `http://127.0.0.1:4096`.

## Communication Flow

1. User opens OPCD Android.
2. Android layer checks Linux runtime.
3. Android layer starts PRoot + Alpine if needed.
4. Linux layer checks Node.js and OpenCode.
5. Install or update OpenCode if needed.
6. Start OpenCode server on localhost.
7. Health check.
8. Load OpenCode UI in WebView.

## File Access

The Android app uses the Storage Access Framework (SAF) to allow users to select project folders. The selected URI is persisted and exposed to the Linux runtime via bind mounts in PRoot.
