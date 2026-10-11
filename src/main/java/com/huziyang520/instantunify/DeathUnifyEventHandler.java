package com.huziyang520.instantunify;

import net.minecraftforge.event.entity.living.LivingDropsEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;

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
