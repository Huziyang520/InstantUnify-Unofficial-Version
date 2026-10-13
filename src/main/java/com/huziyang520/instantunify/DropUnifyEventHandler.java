package com.huziyang520.instantunify;

import net.minecraft.world.entity.item.ItemEntity;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;

/**
 * Unifies items the moment they enter a level, which covers block drops, mob loot, items dropped by players
 * and pre-existing item entities that are loaded with a chunk.
 */
public final class DropUnifyEventHandler {

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public void onEntityJoinLevel(EntityJoinLevelEvent event) {
        if (!InstantUnifyConfig.drop() || event.getLevel().isClientSide()) {
            return;
        }
        if (!(event.getEntity() instanceof ItemEntity itemEntity)) {
            return;
        }
        ItemUnifier.replace(itemEntity.getItem()).ifPresent(itemEntity::setItem);
    }
}
