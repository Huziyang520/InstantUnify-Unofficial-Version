package com.huziyang520.instantunify;

import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.event.config.ModConfigEvent;

/**
 * Rebuilds the derived state when our configuration file is (re)loaded, which covers hand edits, the in-game
 * screen and "/reload". Registered on the mod event bus.
 */
public final class ConfigReloadEventHandler {

    @SubscribeEvent
    public void onConfigLoading(ModConfigEvent.Loading event) {
        if (isOurConfig(event)) {
            ItemUnifier.reload();
        }
    }

    @SubscribeEvent
    public void onConfigReloading(ModConfigEvent.Reloading event) {
        if (isOurConfig(event)) {
            ItemUnifier.reload();
        }
    }

    private static boolean isOurConfig(ModConfigEvent event) {
        return event.getConfig().getSpec() == InstantUnifyConfig.SPEC;
    }
}
