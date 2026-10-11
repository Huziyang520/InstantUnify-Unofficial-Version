package com.huziyang520.instantunify;

import java.util.Optional;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

/**
 * Applies {@link ItemUnifier} to inventories and open containers.
 * Every helper only reports a change when a stack really was replaced.
 */
public final class Inventories {

    private Inventories() {
    }

    public static boolean replaceInventory(ServerPlayer player) {
        return replaceInventory(player.getInventory());
    }

    public static boolean replaceInventory(Inventory inventory) {
        boolean changed = false;
        for (int i = 0; i < inventory.getContainerSize(); i++) {
            ItemStack slot = inventory.getItem(i);
            Optional<ItemStack> replaced = ItemUnifier.replace(slot);
            if (replaced.isPresent()) {
                inventory.setItem(i, replaced.get());
                changed = true;
            }
        }
        return changed;
    }

    /** Replaces every slot of an open menu, containers and player inventory alike. */
    public static boolean replaceMenu(AbstractContainerMenu menu) {
        boolean changed = false;
        for (Slot slot : menu.slots) {
            ItemStack current = slot.getItem();
            Optional<ItemStack> replaced = ItemUnifier.replace(current);
            if (replaced.isPresent()) {
                slot.set(replaced.get());
                changed = true;
            }
        }
        return changed;
    }
}
