/*
 * This file is part of Meteor Universal Profile Security Patch.
 * Copyright (c) Community Patch.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, version 3.
 */

package community.meteorprofilesecuritypatch.mixin;

import community.meteorprofilesecuritypatch.ProfileImportGuard;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.systems.profiles.Profile;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

@Mixin(targets = "meteordevelopment.meteorclient.gui.tabs.builtin.ProfilesTab$ProfilesScreen", remap = false)
public abstract class ProfileImportMixin {
    @Inject(method = "importProfile", at = @At("HEAD"), remap = false)
    private void meteorProfilePatch$beginImport(CallbackInfoReturnable<Profile> info) {
        ProfileImportGuard.begin();
    }

    @Redirect(
        method = "importProfile",
        at = @At(
            value = "INVOKE",
            target = "Lmeteordevelopment/meteorclient/settings/Setting;set(Ljava/lang/Object;)Z",
            ordinal = 0,
            remap = false
        ),
        remap = false
    )
    private boolean meteorProfilePatch$validateImportedName(Setting<Object> setting, Object value) {
        if (!(value instanceof String name) || !ProfileImportGuard.isProfileNameSafe(name)) {
            ProfileImportGuard.block("Blocked an unsafe Meteor profile name during import.");
            return setting.set(ProfileImportGuard.fallbackProfileName());
        }

        if (!setting.set(value)) {
            ProfileImportGuard.block("Blocked a Meteor profile name rejected by its configured filter during import.");
            return setting.set(ProfileImportGuard.fallbackProfileName());
        }

        ProfileImportGuard.rememberExpectedProfileDirectory(name);
        return true;
    }

    @Redirect(
        method = "importProfile",
        at = @At(value = "NEW", target = "Ljava/io/FileOutputStream;", remap = false),
        remap = false
    )
    private FileOutputStream meteorProfilePatch$openContainedOutput(File file) throws IOException {
        File target;

        try {
            target = ProfileImportGuard.validateImportOutputFile(file);
        } catch (IOException | RuntimeException ignored) {
            ProfileImportGuard.block("Blocked an unsafe Meteor profile import path during write.");
            return new FileOutputStream(ProfileImportGuard.fallbackOutputFile());
        }

        return new FileOutputStream(target);
    }

    @Inject(method = "importProfile", at = @At("RETURN"), cancellable = true, remap = false)
    private void meteorProfilePatch$finishImport(CallbackInfoReturnable<Profile> info) {
        if (ProfileImportGuard.finish(info.getReturnValue())) {
            info.setReturnValue(null);
        }
    }
}
