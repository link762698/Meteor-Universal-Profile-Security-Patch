/*
 * This file is part of Meteor Universal Profile Security Patch.
 * Copyright (c) Community Patch.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, version 3.
 */

package community.meteorprofilesecuritypatch.mixin;

import community.meteorprofilesecuritypatch.ProfilePathGuard;
import meteordevelopment.meteorclient.settings.Setting;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

@Mixin(targets = "meteordevelopment.meteorclient.gui.tabs.builtin.ProfilesTab$ProfilesScreen", remap = false)
public abstract class ProfileImportMixin {
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
    private boolean meteorProfilePatch$validateImportedName(Setting<Object> setting, Object value) throws IOException {
        if (!(value instanceof String name) || !ProfilePathGuard.isSafeProfileName(name)) {
            throw new IOException("Blocked an unsafe Meteor profile name.");
        }

        if (!setting.set(value)) {
            throw new IOException("Blocked a Meteor profile name rejected by its configured filter.");
        }

        return true;
    }

    @Redirect(
        method = "importProfile",
        at = @At(value = "NEW", target = "Ljava/io/FileOutputStream;", remap = false),
        remap = false
    )
    private FileOutputStream meteorProfilePatch$openContainedOutput(File file) throws IOException {
        return new FileOutputStream(ProfilePathGuard.validateImportTarget(file));
    }
}
