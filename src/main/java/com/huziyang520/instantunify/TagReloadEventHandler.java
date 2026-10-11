package com.huziyang520.instantunify;

import net.minecraftforge.event.TagsUpdatedEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

/**
 * Tag data can change when a data pack is reloaded, so the material index is dropped and rebuilt on demand.
 */
public final class TagReloadEventHandler {

    @SubscribeEvent
    public void onTagsUpdated(TagsUpdatedEvent event) {
        ItemUnifier.reload();
    }
}
