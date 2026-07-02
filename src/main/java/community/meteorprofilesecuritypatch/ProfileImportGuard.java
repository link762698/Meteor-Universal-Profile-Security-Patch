/*
 * This file is part of Meteor Universal Profile Security Patch.
 * Copyright (c) Community Patch.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, version 3.
 */

package community.meteorprofilesecuritypatch;

import meteordevelopment.meteorclient.systems.profiles.Profile;
import meteordevelopment.meteorclient.systems.profiles.Profiles;

import java.io.File;
import java.io.IOException;

public final class ProfileImportGuard {
    private static final ThreadLocal<ImportState> IMPORT_STATE = new ThreadLocal<>();

    private ProfileImportGuard() {
    }

    public static void begin() {
        IMPORT_STATE.set(new ImportState());
    }

    public static void block(String message) {
        state().blocked = true;
        MeteorProfileSecurityPatch.LOG.warn(message);
    }

    public static boolean finish(Profile profile) {
        ImportState state = IMPORT_STATE.get();

        try {
            if (state == null || !state.blocked) return false;

            if (profile != null) {
                Profiles.get().getAll().remove(profile);
                Profiles.get().save();
            }

            ProfilePathGuard.deleteProfileDirectory(state.fallbackProfileName);
            if (state.expectedProfileDirectory != null && !state.expectedProfileDirectoryExistedBeforeImport) {
                ProfilePathGuard.deleteProfileDirectory(state.expectedProfileDirectory.getName());
            }
            return true;
        } finally {
            IMPORT_STATE.remove();
        }
    }

    public static String fallbackProfileName() {
        return state().fallbackProfileName;
    }

    public static File fallbackOutputFile() throws IOException {
        File fallbackDirectory = ProfilePathGuard.resolveProfileDirectory(fallbackProfileName());
        if (fallbackDirectory == null) throw new IOException("Blocked an unsafe Meteor profile import path.");

        if (!fallbackDirectory.exists() && !fallbackDirectory.mkdirs()) {
            throw new IOException("Could not create blocked import fallback directory.");
        }

        return new File(fallbackDirectory, "blocked-import.nbt");
    }

    public static boolean isProfileNameSafe(String name) {
        return ProfilePathGuard.isSafeProfileName(name);
    }

    public static boolean rememberExpectedProfileDirectory(String profileName) {
        File directory = ProfilePathGuard.resolveProfileDirectory(profileName);
        if (directory == null) return false;

        rememberExpectedProfileDirectory(directory);
        return true;
    }

    public static void rememberExpectedProfileDirectory(File directory) {
        ImportState state = state();
        state.expectedProfileDirectory = directory;
        state.expectedProfileDirectoryExistedBeforeImport = directory.exists();
    }

    public static File validateImportOutputFile(File file) throws IOException {
        ImportState state = state();
        if (state.expectedProfileDirectory == null) throw new IOException("Blocked an unsafe Meteor profile import path.");

        return ProfilePathGuard.validateImportTarget(file, state.expectedProfileDirectory);
    }

    private static ImportState state() {
        ImportState state = IMPORT_STATE.get();
        if (state == null) {
            state = new ImportState();
            IMPORT_STATE.set(state);
        }

        return state;
    }

    private static final class ImportState {
        private final String fallbackProfileName = "profile-security-patch-blocked-" + Long.toUnsignedString(System.nanoTime(), 36);
        private File expectedProfileDirectory;
        private boolean expectedProfileDirectoryExistedBeforeImport;
        private boolean blocked;
    }
}
