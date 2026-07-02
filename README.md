# Meteor Universal Profile Security Patch

An unofficial companion mod that prevents Meteor Client profile paths from escaping the profile directory.

This repository contains a **release candidate**. Runtime and exploit testing will be completed under a separate test plan before a stable release is published.

## Target compatibility

- Minecraft and Meteor Client 26.1.x
- Minecraft and Meteor Client 1.21.11
- Minecraft and Meteor Client 1.21.10
- Minecraft and Meteor Client 1.21.8
- Minecraft and Meteor Client 1.21.7
- Minecraft and Meteor Client 1.21.6
- Minecraft and Meteor Client 1.21.5
- Minecraft and Meteor Client 1.21.4

Meteor 1.21.4 through 1.21.8 do not contain file-based profile import. On those versions, the patch still enforces string filters and guards profile load, save, delete, and path resolution.

## What it protects

- Enforces Meteor's configured string filters when settings are loaded programmatically.
- Rejects blank, root-targeting, or traversing profile names.
- Prevents profile load, save, and delete operations from escaping `Profiles.FOLDER`.
- On versions with file import, accepts only a direct file inside the selected profile directory.
- Rejects absolute, nested, traversal, cross-profile, symlink, and junction destinations.
- Fails closed if a supported version no longer contains an expected security hook.

The import fix is based on the intent of [MeteorDevelopment/meteor-client#6500](https://github.com/MeteorDevelopment/meteor-client/pull/6500), with additional containment for profile names and all profile file operations.

## Build

Requirements:

- Java 21 or newer
- Internet access for Gradle dependencies

Windows:

```text
gradlew.bat clean build
```

Linux or macOS:

```text
./gradlew clean build
```

The installable mod JAR is written to `build/libs`.

## Local security fixture test

Local-only profile import regression fixtures can be generated with `tools/generate-local-security-fixtures.py`. See `docs/local-security-fixture-test.md` for the disposable-instance test procedure.

## Candidate status

The current artifact is `meteor-universal-profile-security-patch-1.0.0-rc.3.jar`. It is not a stable release and should not be represented as fully validated until the follow-up runtime test plan is complete.

## License

GPL-3.0-only. This project is unofficial and is not affiliated with Meteor Development.
