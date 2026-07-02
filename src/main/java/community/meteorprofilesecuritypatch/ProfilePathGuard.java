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
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.Locale;

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
        } catch (IOException | RuntimeException ignored) {
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

    public static File validateImportTarget(File file, File expectedProfileDirectory) throws IOException {
        if (file == null || expectedProfileDirectory == null) throw blockedImport();

        File rootFile = canonicalRoot();
        Path root = rootFile.toPath();
        File expectedDirectory = expectedProfileDirectory.getCanonicalFile();
        File expectedParent = expectedDirectory.getParentFile();

        if (expectedDirectory.equals(rootFile) || expectedParent == null || !expectedParent.equals(rootFile)) {
            throw blockedImport();
        }

        Path expected = expectedDirectory.toPath();
        Path raw;

        try {
            raw = file.getAbsoluteFile().toPath();
        } catch (InvalidPathException exception) {
            throw blockedImport();
        }

        if (!raw.startsWith(expected)) throw blockedImport();

        Path relative = expected.relativize(raw);
        if (relative.getNameCount() != 1) throw blockedImport();

        String fileName = relative.getFileName().toString();
        if (!isSafeImportFilename(fileName)) throw blockedImport();

        File canonical = file.getCanonicalFile();
        File parent = canonical.getParentFile();
        if (parent == null || !parent.equals(expectedDirectory)) throw blockedImport();
        if (canonical.equals(expectedDirectory) || !canonical.toPath().startsWith(root)) throw blockedImport();

        return canonical;
    }

    public static boolean isSafeImportTarget(File file, File expectedProfileDirectory) {
        try {
            validateImportTarget(file, expectedProfileDirectory);
            return true;
        } catch (IOException | RuntimeException ignored) {
            return false;
        }
    }

    public static boolean isSafeImportFilename(String filename) {
        if (filename == null || filename.isBlank() || isDotSegment(filename)) return false;
        if (filename.indexOf('/') >= 0 || filename.indexOf('\\') >= 0) return false;
        if (filename.endsWith(".") || filename.endsWith(" ")) return false;

        for (int i = 0; i < filename.length(); i++) {
            char character = filename.charAt(i);
            if (character < 32 || ":*?\"<>|".indexOf(character) >= 0) return false;
        }

        String normalized = filename.toLowerCase(Locale.ROOT);
        int dot = normalized.indexOf('.');
        String baseName = dot >= 0 ? normalized.substring(0, dot) : normalized;

        return !baseName.equals("con")
            && !baseName.equals("prn")
            && !baseName.equals("aux")
            && !baseName.equals("nul")
            && !baseName.matches("com[1-9]")
            && !baseName.matches("lpt[1-9]");
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
