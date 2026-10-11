package com.huziyang520.instantunify;

import net.minecraftforge.fml.ModList;

/**
 * Gate for the optional client only integration.
 * <p>
 * This class deliberately references no class of the optional dependency, so the JVM never has to load any of
 * them when the dependency is missing. {@link ClothConfigIntegration} is only touched after the presence check.
 */
public final class ClientSetup {

    private static final String CLOTH_CONFIG_MOD_ID = "cloth_config";

    private ClientSetup() {
    }

    public static void init() {
        if (!ModList.get().isLoaded(CLOTH_CONFIG_MOD_ID)) {
            return;
        }
        ClothConfigIntegration.register();
    }
}
