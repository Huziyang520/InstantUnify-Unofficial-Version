package com.huziyang520.instantunify;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraftforge.registries.ForgeRegistries;

/**
 * Resolves "which item is the dominant one for this material".
 * <p>
 * The original implementation walked the whole item registry (once per tag name) on every single replacement,
 * sorting with a linear lookup inside the comparator. This index does the same work once and then answers in O(1):
 * items are grouped by their tag fingerprint and each group keeps the first item of the highest priority mod.
 * <p>
 * The index is only a cache: results are identical to a fresh scan, and it is rebuilt whenever the tags or the
 * configuration change. Setting {@code performance.indexCache = false} falls back to the original per-call scan.
 */
public final class UnifyIndex {

    private final TagNormalizer normalizer;

    private volatile Map<Item, Item> index = Collections.emptyMap();
    private volatile boolean dirty = true;
    private volatile long builtAtMillis;

    public UnifyIndex(TagNormalizer normalizer) {
        this.normalizer = normalizer;
    }

    public void invalidate() {
        dirty = true;
    }

    /** @return the dominant item for the given one, or {@code null} when there is none */
    public Item resolve(Item item) {
        if (!InstantUnifyConfig.indexCache()) {
            return scan(item);
        }
        return index().get(item);
    }

    private Map<Item, Item> index() {
        if (isStale()) {
            synchronized (this) {
                if (isStale()) {
                    Map<Item, Item> built = build();
                    builtAtMillis = System.currentTimeMillis();
                    index = built;
                    dirty = false;
                }
            }
        }
        return index;
    }

    private boolean isStale() {
        if (dirty) {
            return true;
        }
        int refreshSeconds = InstantUnifyConfig.lazyIndexRefreshSeconds();
        return refreshSeconds > 0 && System.currentTimeMillis() - builtAtMillis > refreshSeconds * 1000L;
    }

    private Map<Item, Item> build() {
        Map<String, Integer> priority = priorityMap();
        Set<String> blockedMods = new HashSet<>(InstantUnifyConfig.blacklistMods());

        // Pass one: group items by fingerprint, remembering the best candidate per group.
        Map<Item, List<ResourceLocation>> fingerprints = new HashMap<>();
        Map<List<ResourceLocation>, Item> bestPerGroup = new HashMap<>();
        for (Item item : ForgeRegistries.ITEMS.getValues()) {
            ResourceLocation id = ForgeRegistries.ITEMS.getKey(item);
            // Blacklisted mods neither get replaced nor act as a target.
            if (id == null || blockedMods.contains(id.getNamespace())) {
                continue;
            }
            List<ResourceLocation> fingerprint = normalizer.normalize(item);
            if (fingerprint.isEmpty()) {
                continue;
            }
            fingerprints.put(item, fingerprint);
            Item current = bestPerGroup.get(fingerprint);
            // Strictly better priority wins, ties keep the first item in registry order (stable, as before).
            if (current == null || priorityOf(priority, item) < priorityOf(priority, current)) {
                bestPerGroup.put(fingerprint, item);
            }
        }

        // Pass two: every item points at the winner of its own group.
        Map<Item, Item> result = new HashMap<>(fingerprints.size() * 2);
        for (Map.Entry<Item, List<ResourceLocation>> entry : fingerprints.entrySet()) {
            Item target = bestPerGroup.get(entry.getValue());
            if (target != null) {
                result.put(entry.getKey(), target);
            }
        }
        return result;
    }

    /** The original behaviour: scan the registry now. Used when the index cache is disabled. */
    private Item scan(Item item) {
        List<ResourceLocation> fingerprint = normalizer.normalize(item);
        if (fingerprint.isEmpty()) {
            return null;
        }
        Map<String, Integer> priority = priorityMap();
        Set<String> blockedMods = new HashSet<>(InstantUnifyConfig.blacklistMods());
        Item best = null;
        int bestPriority = Integer.MAX_VALUE;
        List<Item> candidates = new ArrayList<>(ForgeRegistries.ITEMS.getValues());
        for (Item candidate : candidates) {
            ResourceLocation id = ForgeRegistries.ITEMS.getKey(candidate);
            if (id == null || blockedMods.contains(id.getNamespace())) {
                continue;
            }
            if (!fingerprint.equals(normalizer.normalize(candidate))) {
                continue;
            }
            int candidatePriority = priorityOf(priority, candidate);
            if (candidatePriority < bestPriority) {
                bestPriority = candidatePriority;
                best = candidate;
            }
        }
        return best;
    }

    private static Map<String, Integer> priorityMap() {
        List<? extends String> preferred = InstantUnifyConfig.preferredMods();
        Map<String, Integer> map = new HashMap<>(preferred.size() * 2);
        for (int i = 0; i < preferred.size(); i++) {
            map.putIfAbsent(preferred.get(i), i);
        }
        return map;
    }

    private static int priorityOf(Map<String, Integer> priority, Item item) {
        ResourceLocation id = ForgeRegistries.ITEMS.getKey(item);
        if (id == null) {
            return Integer.MAX_VALUE;
        }
        Integer value = priority.get(id.getNamespace());
        return value == null ? 999 : value;
    }
}
