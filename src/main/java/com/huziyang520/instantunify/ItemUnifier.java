package com.huziyang520.instantunify;

import java.util.List;
import java.util.Optional;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

/**
 * Turns an item stack into the "dominant" stack of its material group, and owns the shared derived state
 * (the compiled tag lists and the material index).
 */
public final class ItemUnifier {

    private static final TagNormalizer NORMALIZER = new TagNormalizer();
    private static final UnifyIndex INDEX = new UnifyIndex(NORMALIZER);

    private ItemUnifier() {
    }

    public static UnifyIndex index() {
        return INDEX;
    }

    /** Drops every cached value; the next call rebuilds them from the current configuration. */
    public static void reload() {
        NORMALIZER.invalidate();
        INDEX.invalidate();
    }

    /**
     * @return the replacement stack, or an empty optional when nothing has to be changed
     */
    public static Optional<ItemStack> replace(ItemStack orig) {
        if (orig.isEmpty()) {
            return Optional.empty();
        }
        // Items carrying custom data (enchants, damage, custom name, ...) are left alone, same as the original mod.
        // hasTag() is gone in the component world: a stack with no components beyond the defaults has an empty patch.
        if (!orig.isComponentsPatchEmpty()) {
            return Optional.empty();
        }
        Identifier id = BuiltInRegistries.ITEM.getKey(orig.getItem());
        if (id == null || InstantUnifyConfig.blacklistMods().contains(id.getNamespace())) {
            return Optional.empty();
        }
        // Item level blacklist: lets a single variant stay as it is (e.g. a charged crystal next to the plain one).
        List<? extends String> blockedItems = InstantUnifyConfig.blacklistItems();
        if (!blockedItems.isEmpty() && blockedItems.contains(id.toString())) {
            return Optional.empty();
        }
        Item target = INDEX.resolve(orig.getItem());
        // No dominant item, or the item already is the dominant one: nothing to write.
        if (target == null || target == orig.getItem()) {
            return Optional.empty();
        }
        return Optional.of(new ItemStack(target, orig.getCount()));
    }
}
