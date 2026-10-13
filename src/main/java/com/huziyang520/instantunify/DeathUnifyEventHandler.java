package com.huziyang520.instantunify;

import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.entity.living.LivingDropsEvent;

/**
 * Unifies the drops of a dying entity before the item entities are created.
 */
public final class DeathUnifyEventHandler {

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public void onLivingDrops(LivingDropsEvent event) {
        if (!InstantUnifyConfig.death()) {
            return;
        }
        event.getDrops().forEach(drop -> ItemUnifier.replace(drop.getItem()).ifPresent(drop::setItem));
    }
}
