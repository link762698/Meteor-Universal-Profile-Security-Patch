/*
 * This file is part of Meteor Universal Profile Security Patch.
 * Copyright (c) Community Patch.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, version 3.
 */

package community.meteorprofilesecuritypatch.mixin;

import net.fabricmc.loader.api.FabricLoader;
import org.objectweb.asm.tree.ClassNode;
import org.spongepowered.asm.mixin.extensibility.IMixinConfigPlugin;
import org.spongepowered.asm.mixin.extensibility.IMixinInfo;

import java.util.List;
import java.util.Set;

public final class UniversalMixinPlugin implements IMixinConfigPlugin {
    private static final Set<String> SUPPORTED_LEGACY_VERSIONS = Set.of(
        "1.21.4",
        "1.21.5",
        "1.21.6",
        "1.21.7",
        "1.21.8"
    );
    private static final Set<String> SUPPORTED_IMPORT_VERSIONS = Set.of(
        "1.21.10",
        "1.21.11"
    );

    private String minecraftVersion;

    @Override
    public void onLoad(String mixinPackage) {
        minecraftVersion = FabricLoader.getInstance()
            .getModContainer("minecraft")
            .orElseThrow(() -> new IllegalStateException("Minecraft metadata is unavailable."))
            .getMetadata()
            .getVersion()
            .getFriendlyString();

        if (!isSupported(minecraftVersion)) {
            throw new IllegalStateException("Unsupported Minecraft version for the Meteor profile security patch.");
        }
    }

    @Override
    public String getRefMapperConfig() {
        return null;
    }

    @Override
    public boolean shouldApplyMixin(String targetClassName, String mixinClassName) {
        if (mixinClassName.endsWith(".ProfileImportPrevalidateMixin")) {
            return hasIntermediaryImportNbtReadHook(minecraftVersion);
        }

        if (mixinClassName.endsWith(".ProfileImportMixin")) {
            return requiresImportHook(minecraftVersion);
        }

        return true;
    }

    @Override
    public void acceptTargets(Set<String> myTargets, Set<String> otherTargets) {
    }

    @Override
    public List<String> getMixins() {
        return null;
    }

    @Override
    public void preApply(String targetClassName, ClassNode targetClass, String mixinClassName, IMixinInfo mixinInfo) {
    }

    @Override
    public void postApply(String targetClassName, ClassNode targetClass, String mixinClassName, IMixinInfo mixinInfo) {
    }

    private static boolean isSupported(String version) {
        return SUPPORTED_LEGACY_VERSIONS.contains(version) || requiresImportHook(version);
    }

    private static boolean requiresImportHook(String version) {
        return SUPPORTED_IMPORT_VERSIONS.contains(version) || version.equals("26.1") || version.startsWith("26.1.");
    }

    private static boolean hasIntermediaryImportNbtReadHook(String version) {
        return SUPPORTED_IMPORT_VERSIONS.contains(version);
    }
}
