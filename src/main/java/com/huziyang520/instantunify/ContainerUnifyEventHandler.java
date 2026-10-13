package com.huziyang520.instantunify;

import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.entity.player.PlayerContainerEvent;

/**
 * Unifies the contents of a container when it is opened, including chests, machines and the player inventory.
 * Disabled by default because it writes into containers the player did not touch.
 */
public final class ContainerUnifyEventHandler {

    @SubscribeEvent
    public void onContainerOpen(PlayerContainerEvent.Open event) {
        if (!InstantUnifyConfig.gui() || !(event.getEntity() instanceof ServerPlayer player)
                || player.level().isClientSide()) {
            return;
        }
        if (Inventories.replaceMenu(event.getContainer())) {
            event.getContainer().broadcastChanges();
        }
    }
}
