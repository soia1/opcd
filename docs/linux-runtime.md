# OPCD Android — Linux Runtime Integration

## Overview

OPCD Android embeds a complete Linux runtime using PRoot + Alpine Linux. The runtime is downloaded and installed on the first launch.

## Components

### RuntimeManager

RuntimeManager is the central class responsible for:

- Downloading and installing PRoot.
- Downloading and extracting Alpine Linux rootfs.
- Installing base tools: Node.js, npm, Python, pip, Git.
- Installing OpenCode.
- Running commands inside the Alpine environment.

### CommandRunner

CommandRunner provides a convenient API to execute commands inside the runtime.

Example:

    CommandRunner runner = new CommandRunner(runtimeManager);
    CommandRunner.CommandResult result = runner.execute("node --version");
    if (result.isSuccess()) {
        Log.i(TAG, result.output);
    }

### Storage Layout

All runtime files are stored in the app's private storage:

    /data/data/com.opcd.android/files/
    ├── runtime/
    │   ├── bin/
    │   │   └── proot
    │   └── alpine-rootfs/
    │       ├── usr/bin/node
    │       ├── usr/bin/npm
    │       ├── usr/bin/python3
    │       ├── usr/bin/git
    │       └── usr/local/bin/opencode
    └── projects/

## First-Run Setup Flow

1. MainActivity checks if RuntimeManager.isRuntimeReady() is true.
2. If not, it shows a setup screen.
3. User taps Setup Linux Runtime.
4. RuntimeManager.setupRuntime() runs in background.
5. Once complete, MainActivity starts OpenCodeService.

## Download Sizes

Approximate download sizes on first run:

- PRoot: ~200 KB
- Alpine rootfs: ~5 MB
- Node.js + npm: ~30 MB
- Python + pip: ~15 MB
- Git: ~5 MB
- OpenCode: ~50 MB

Total: ~105 MB (downloaded on first run)

## Package Managers Supported

- apk: Alpine package manager.
- npm: Node.js package manager.
- pip: Python package manager.

## Known Limitations

- The tar extractor is minimal and may not handle all tar features perfectly.
- Symlinks are created as placeholder files in the current implementation.
- Downloads require internet on first run.
