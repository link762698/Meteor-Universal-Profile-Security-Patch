/*
 * This file is part of Meteor Universal Profile Security Patch.
 * Copyright (c) Community Patch.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, version 3.
 */

package community.meteorprofilesecuritypatch;

import meteordevelopment.meteorclient.systems.profiles.Profiles;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;

public final class ProfilePathGuard {
    public static final String BLOCKED_DIRECTORY_NAME = ".blocked-invalid-profile";

    private ProfilePathGuard() {
    }

    public static File resolveProfileDirectory(String name) {
        if (name == null || name.isBlank() || BLOCKED_DIRECTORY_NAME.equals(name)) return null;

        try {
            File root = canonicalRoot();
            File candidate = new File(root, name).getCanonicalFile();
            File parent = candidate.getParentFile();

            if (candidate.equals(root) || parent == null || !parent.equals(root)) return null;
            return candidate;
        } catch (IOException ignored) {
            return null;
        }
    }

    public static boolean isSafeProfileName(String name) {
        return resolveProfileDirectory(name) != null;
    }

    public static File blockedDirectory() {
        try {
            return new File(canonicalRoot(), BLOCKED_DIRECTORY_NAME).getCanonicalFile();
        } catch (IOException ignored) {
            return new File(Profiles.FOLDER, BLOCKED_DIRECTORY_NAME).getAbsoluteFile();
        }
    }

    public static File validateImportTarget(File file) throws IOException {
        if (file == null) throw blockedImport();

        File rootFile = canonicalRoot();
        Path root = rootFile.toPath();
        Path raw = file.getAbsoluteFile().toPath();

        if (!raw.startsWith(root)) throw blockedImport();

        Path relative = root.relativize(raw);
        if (relative.getNameCount() != 2) throw blockedImport();

        String profileName = relative.getName(0).toString();
        String fileName = relative.getName(1).toString();
        if (isDotSegment(profileName) || isDotSegment(fileName)) throw blockedImport();

        File profileDirectory = resolveProfileDirectory(profileName);
        if (profileDirectory == null) throw blockedImport();

        File canonical = file.getCanonicalFile();
        File parent = canonical.getParentFile();
        if (parent == null || !parent.equals(profileDirectory)) throw blockedImport();
        if (canonical.equals(profileDirectory) || !canonical.toPath().startsWith(root)) throw blockedImport();

        return canonical;
    }

    public static boolean isSafeImportTarget(File file) {
        try {
            validateImportTarget(file);
            return true;
        } catch (IOException ignored) {
            return false;
        }
    }

    public static void deleteProfileDirectory(String name) {
        File directory = resolveProfileDirectory(name);
        if (directory == null || !directory.exists()) return;

        try {
            Files.walk(directory.toPath())
                .sorted(Comparator.reverseOrder())
                .forEach(path -> {
                    try {
                        Files.deleteIfExists(path);
                    } catch (IOException ignored) {
                    }
                });
        } catch (IOException ignored) {
        }
    }

    private static File canonicalRoot() throws IOException {
        return Profiles.FOLDER.getCanonicalFile();
    }

    private static boolean isDotSegment(String value) {
        return value.equals(".") || value.equals("..");
    }

    private static IOException blockedImport() {
        return new IOException("Blocked an unsafe Meteor profile import path.");
    }
}
