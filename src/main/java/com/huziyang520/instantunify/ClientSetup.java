package com.huziyang520.instantunify;

import com.huziyang.neoconfigurationscreenport.forge.ConfigScreenRegistration;

/**
 * Client only entry point of the in-game screens.
 * <p>
 * The "Config" button of the mod list opens {@link ConfigHubScreen}, which leads to the settings screen of the
 * prerequisite mod and to the list editor. Nothing registers that for other mods, so it happens here.
 * <p>
 * Only the client side branch of the mod constructor calls this, so no client only class of the prerequisite is
 * ever loaded on a dedicated server.
 */
public final class ClientSetup {

    private ClientSetup() {
    }

    public static void init() {
        ConfigScreenRegistration.register(InstantUnify.MOD_ID,
                (modContainer, parent) -> new ConfigHubScreen(parent, modContainer));
    }
}
