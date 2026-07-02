/*
 * This file is part of Meteor Universal Profile Security Patch.
 * Copyright (c) Community Patch.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, version 3.
 */

package community.meteorprofilesecuritypatch.mixin;

import meteordevelopment.meteorclient.gui.utils.CharFilter;
import meteordevelopment.meteorclient.settings.StringSetting;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value = StringSetting.class, remap = false)
public abstract class StringSettingMixin {
    @Shadow
    public CharFilter filter;

    @Inject(method = "isValueValid(Ljava/lang/String;)Z", at = @At("HEAD"), cancellable = true, remap = false)
    private void meteorProfilePatch$enforceFilter(String value, CallbackInfoReturnable<Boolean> info) {
        if (value == null) {
            info.setReturnValue(false);
            return;
        }

        if (filter == null) return;

        for (int i = 0; i < value.length(); i++) {
            if (!filter.filter(value, value.charAt(i))) {
                info.setReturnValue(false);
                return;
            }
        }
    }
}
