package com.huziyang520.instantunify;

import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.TagsUpdatedEvent;

/**
 * Tag data can change when a data pack is reloaded, so the material index is dropped and rebuilt on demand.
 */
public final class TagReloadEventHandler {

    @SubscribeEvent
    public void onTagsUpdated(TagsUpdatedEvent event) {
        ItemUnifier.reload();
    }
}
