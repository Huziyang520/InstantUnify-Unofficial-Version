package com.huziyang520.instantunify;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerListener;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

/**
 * Unifies the player inventory, either when a slot changes ({@code change}) or on a one second interval
 * ({@code second}).
 * <p>
 * Both paths only write when a stack really has to be replaced, so they cannot ping-pong with the container
 * synchronisation (the original version rewrote and rebroadcast every slot on every tick).
 */
public final class InventoryUnifyEventHandler {

    @SubscribeEvent
    public void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (!InstantUnifyConfig.second() || event.phase != TickEvent.Phase.END) {
            return;
        }
        if (event.player.level().isClientSide()) {
            return;
        }
        if (event.player.level().getGameTime() % 20 != 18) {
            return;
        }
        ServerPlayer player = (ServerPlayer) event.player;
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
