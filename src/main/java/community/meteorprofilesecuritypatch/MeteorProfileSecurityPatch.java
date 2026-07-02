/*
 * This file is part of Meteor Universal Profile Security Patch.
 * Copyright (c) Community Patch.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, version 3.
 */

package community.meteorprofilesecuritypatch;

import net.fabricmc.api.ClientModInitializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class MeteorProfileSecurityPatch implements ClientModInitializer {
    public static final String MOD_ID = "meteor-universal-profile-security-patch";
    public static final Logger LOG = LoggerFactory.getLogger(MOD_ID);

    @Override
    public void onInitializeClient() {
        LOG.info("Meteor Universal Profile Security Patch 1.0.0-rc.1 initialized.");
    }
}
