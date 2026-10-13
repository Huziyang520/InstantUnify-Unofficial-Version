package com.huziyang520.instantunify;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerListener;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

/**
 * Unifies the player inventory, either when a slot changes ({@code change}) or on a one second interval
 * ({@code second}).
 * <p>
 * Both paths only write when a stack really has to be replaced, so they cannot ping-pong with the container
 * synchronisation (the original version rewrote and rebroadcast every slot on every tick).
 */
public final class InventoryUnifyEventHandler {

    // The single tick event of the older versions is split into a pre and a post event here, this is the
    // end-of-tick one, which is what the original handler listened to.
    @SubscribeEvent
    public void onPlayerTick(PlayerTickEvent.Post event) {
        if (!InstantUnifyConfig.second()) {
            return;
        }
        if (event.getEntity().level().isClientSide()) {
            return;
        }
        if (event.getEntity().level().getGameTime() % 20 != 18) {
            return;
        }
        ServerPlayer player = (ServerPlayer) event.getEntity();
        if (Inventories.replaceInventory(player)) {
            player.containerMenu.broadcastChanges();
        }
    }

    @SubscribeEvent
    public void onPlayerLoggedIn(PlayerEvent.PlayerLoggedInEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player) || player.level().isClientSide()) {
            return;
        }
        player.containerMenu.addSlotListener(new ContainerListener() {
            @Override
            public void slotChanged(AbstractContainerMenu menu, int index, ItemStack stack) {
                if (!InstantUnifyConfig.change() || index < 0 || index >= menu.getItems().size()) {
                    return;
                }
                ItemUnifier.replace(menu.getItems().get(index))
                        .ifPresent(replaced -> menu.setItem(index, menu.getStateId(), replaced));
            }

            @Override
            public void dataChanged(AbstractContainerMenu menu, int index, int value) {
                // Nothing to do for data slots.
            }
        });
    }
}
