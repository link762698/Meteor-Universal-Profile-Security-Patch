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
import community.meteorprofilesecuritypatch.ProfilePathGuard;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtIo;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.Map;

@Mixin(targets = "meteordevelopment.meteorclient.gui.tabs.builtin.ProfilesTab$ProfilesScreen", remap = false)
public abstract class ProfileImportPrevalidateMixin {
    @Redirect(
        method = "importProfile",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/nbt/NbtIo;read(Ljava/nio/file/Path;)Lnet/minecraft/nbt/NbtCompound;",
            remap = true
        ),
        remap = false
    )
    private NbtCompound meteorProfilePatch$readAndValidateImport(Path path) throws IOException {
        NbtCompound nbt = NbtIo.read(path);

        if (!meteorProfilePatch$isImportSafe(nbt, path.toFile())) {
            ProfileImportGuard.block("Blocked an unsafe Meteor profile import before profile files were written.");

            NbtCompound replacement = new NbtCompound();
            replacement.putString("name", ProfileImportGuard.fallbackProfileName());
            return replacement;
        }

        return nbt;
    }

    private static boolean meteorProfilePatch$isImportSafe(NbtCompound nbt, File profileFile) {
        String profileName = nbt.getString("name", profileFile.getName());
        if (!ProfileImportGuard.isProfileNameSafe(profileName)) return false;

        File profileDirectory = ProfilePathGuard.resolveProfileDirectory(profileName);
        if (profileDirectory == null) return false;

        for (Map.Entry<String, ?> entry : nbt.entrySet()) {
            String filename = entry.getKey();
            if ("name".equals(filename)) continue;

            if (!ProfilePathGuard.isSafeImportTarget(new File(profileDirectory, filename))) {
                return false;
            }
        }

        return true;
    }
}
