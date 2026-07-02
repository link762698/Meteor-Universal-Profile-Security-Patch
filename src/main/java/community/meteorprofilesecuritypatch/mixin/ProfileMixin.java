/*
 * This file is part of Meteor Universal Profile Security Patch.
 * Copyright (c) Community Patch.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, version 3.
 */

package community.meteorprofilesecuritypatch.mixin;

import community.meteorprofilesecuritypatch.MeteorProfileSecurityPatch;
import community.meteorprofilesecuritypatch.ProfilePathGuard;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.systems.profiles.Profile;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.io.File;

@Mixin(value = Profile.class, remap = false)
public abstract class ProfileMixin {
    @Shadow
    public Setting<String> name;

    @Inject(method = { "load", "save", "delete" }, at = @At("HEAD"), cancellable = true, remap = false)
    private void meteorProfilePatch$blockUnsafeOperation(CallbackInfo info) {
        if (ProfilePathGuard.isSafeProfileName(name.get())) return;

        MeteorProfileSecurityPatch.LOG.warn("Blocked a profile file operation with an unsafe profile name.");
        info.cancel();
    }

    @Inject(method = "getFile", at = @At("RETURN"), cancellable = true, remap = false)
    private void meteorProfilePatch$containProfilePath(CallbackInfoReturnable<File> info) {
        File directory = ProfilePathGuard.resolveProfileDirectory(name.get());
        info.setReturnValue(directory != null ? directory : ProfilePathGuard.blockedDirectory());
    }
}
