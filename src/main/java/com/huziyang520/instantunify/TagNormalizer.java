package com.huziyang520.instantunify;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraftforge.registries.ForgeRegistries;

/**
 * Turns an item into its "material fingerprint": the sorted list of tag names that survive the alternatives
 * expansion and the white/black list filtering. Two items are interchangeable when their fingerprints are equal.
 * <p>
 * Regexes, the alternatives map and the namespace aliases are compiled once and rebuilt only when the
 * configuration changes.
 */
public final class TagNormalizer {

    /** Entries written as {@code <tag>\/.+} cover the tag itself as well as everything below it. */
    private static final String SUB_TAG_SUFFIX = "\\/.+";

    /** Shapes that make an item a material, most specific first. */
    private static final List<String> SHAPES = List.of("ores", "raw_materials", "ingots", "nuggets", "gems", "dusts",
            "gears", "plates", "rods", "storage_blocks");

    /** Item name suffixes, paired with the shape they mean. Used for items that carry no material tag at all. */
    private static final List<String> NAME_SHAPE_SUFFIXES = List.of("_ore", "_ingot", "_nugget", "_gem", "_dust",
            "_gear", "_plate", "_rod", "_block");
    private static final List<String> NAME_SHAPE_VALUES = List.of("ores", "ingots", "nuggets", "gems", "dusts", "gears",
            "plates", "rods", "storage_blocks");

    /** Item name prefixes that play the same role as the ores_in_ground tag. */
    private static final List<String> NAME_GROUND_PREFIXES = List.of("deepslate_", "nether_", "end_", "raw_");
    private static final List<String> NAME_GROUND_VALUES =
            List.of("deepslate", "netherrack", "end_stone", "raw");

    /** An ore without a ground tag is treated as the stone variant, which is what the tag convention means. */
    private static final String DEFAULT_GROUND = "stone";

    /** Tag name of the ore variant marker, it is a modifier and never a material of its own. */
    private static final String ORES_IN_GROUND = "ores_in_ground";

    /** Prefix of the key of a material that is only ever named by a bare tag, e.g. forge:silicon. */
    private static final String BARE_MATERIAL_PREFIX = "material#";

    /**
     * Alternate spellings of the same ground, folded into the spelling the tag convention uses before the key is
     * built. The convention writes the ground of an ore as {@code ores_in_ground/<word>} with a word that is
     * usually "<something>stone" (stone, end_stone) or "netherrack"; some mods ship the short form of those two.
     * This is a rule about the tag vocabulary, not about a mod: whatever uses the short spelling is grouped the
     * same way. Grounds that are genuinely different (deepslate, the stone of another dimension) are untouched.
     */
    private static final Map<String, String> GROUND_ALIASES = Map.of(
            "end", "end_stone",
            "nether", "netherrack");

    private volatile boolean dirty = true;
    private volatile Pattern[] whitelist = new Pattern[0];
    private volatile Pattern[] blacklist = new Pattern[0];
    private volatile Map<String, List<String>> alternatives = Collections.emptyMap();
    private volatile Map<String, String> namespaceAliases = Collections.emptyMap();

    public void invalidate() {
        dirty = true;
    }

    // ResourceLocation(String, String) is the constructor this Minecraft version offers.
    @SuppressWarnings("removal")
    public List<ResourceLocation> normalize(Item item) {
        ensureUpToDate();

        Set<ResourceLocation> raw = item.builtInRegistryHolder().tags()
                .map(TagKey::location)
                .collect(Collectors.toSet());

        // Tag namespaces that mean the same thing (forge: and c:) are folded into one spelling first, otherwise
        // an item that ships both spellings has more tag names than an item that ships only one of them and the
        // two can never end up in the same group.
        Map<String, String> aliases = this.namespaceAliases;
        Set<ResourceLocation> own;
        if (aliases.isEmpty()) {
            own = raw;
        } else {
            own = new HashSet<>(raw.size());
            for (ResourceLocation name : raw) {
                String canonical = aliases.getOrDefault(name.getNamespace(), name.getNamespace());
                own.add(canonical.equals(name.getNamespace())
                        ? name
                        : new ResourceLocation(canonical, name.getPath()));
            }
        }

        Set<ResourceLocation> names = new HashSet<>(own);

        // "aluminum" / "aluminium" / "bauxite" are spelled differently by different mods, so every known
        // spelling of a path segment is added as an extra tag name. The namespace is kept, same as the original.
        Map<String, List<String>> alts = this.alternatives;
        if (!alts.isEmpty()) {
            for (ResourceLocation name : own) {
                for (Map.Entry<String, List<String>> entry : alts.entrySet()) {
                    if (!name.getPath().contains(entry.getKey())) {
                        continue;
                    }
                    for (String alt : entry.getValue()) {
                        names.add(new ResourceLocation(name.getNamespace(), name.getPath().replace(entry.getKey(), alt)));
                    }
                }
            }
        }

        List<ResourceLocation> result = new ArrayList<>(names.size());
        for (ResourceLocation name : names) {
            if (!passesLists(name)) {
                continue;
            }
            result.add(name);
        }
        Collections.sort(result);
        return result;
    }

    /**
     * Turns an item into its "material key", the value that decides which items are interchangeable.
     * <p>
     * A key is {@code <shape>#<material>} (ores also carry {@code @<ground>}) and comes from the tags first.
     * Two items that both say {@code forge:ores/silver} therefore land in the same group even when one of them
     * also carries unrelated tags, which is what the old "the whole tag set has to be equal" rule could not do.
     * <p>
     * Items without any material tag - Ice and Fire ships its ores and gems with block tags only - fall back to
     * the registry name, so no per mod list has to be maintained: {@code iceandfire:silver_ore} simply means
     * "silver ore in stone" like everywhere else.
     *
     * @return the key, or {@code null} when the item is not a material this mod knows about
     */
    public String materialKey(Item item) {
        String ground = null;
        String best = null;
        int bestRank = Integer.MAX_VALUE;
        String bare = null;
        for (ResourceLocation tag : normalize(item)) {
            String path = tag.getPath();
            int slash = path.indexOf('/');
            if (slash < 0) {
                // A tag without a category names a material on its own: forge:silicon. Shape words such as
                // forge:ingots are categories and must not be read as a material.
                if (bare == null && !SHAPES.contains(path) && !path.equals(ORES_IN_GROUND)) {
                    bare = path;
                }
                continue;
            }
            if (slash == path.length() - 1) {
                continue;
            }
            String shape = path.substring(0, slash);
            String material = path.substring(slash + 1);
            if (shape.equals(ORES_IN_GROUND)) {
                ground = canonicalGround(material);
                continue;
            }
            int rank = SHAPES.indexOf(shape);
            if (rank >= 0 && rank < bestRank) {
                bestRank = rank;
                best = shape + "#" + material;
            }
        }
        if (ground == null) {
            ground = blockGround(item);
        }
        if (best != null) {
            // Ores keep the same stone / deepslate split the ores_in_ground tag expresses.
            return bestRank == 0 ? best + "@" + (ground == null ? DEFAULT_GROUND : ground) : best;
        }
        if (bare != null) {
            // Everything that carries that tag is the same material, so the shape plays no part here.
            return BARE_MATERIAL_PREFIX + bare;
        }
        return nameKey(item);
    }

    private static String canonicalGround(String word) {
        return GROUND_ALIASES.getOrDefault(word, word);
    }

    /**
     * The ground of an ore can also live on the block the item places: several mods tag only the block with
     * {@code ores_in_ground/...} and leave the item itself without it. An item without a ground tag is read as
     * the stone variant, which puts an end or a nether ore into the overworld group - so the ground is looked up
     * on the block as well, and only the ground: no other block tag takes part in the fingerprint.
     *
     * @return the ground of the block, or {@code null} when the item places no block or the block has none
     */
    private String blockGround(Item item) {
        if (!(item instanceof BlockItem blockItem)) {
            return null;
        }
        String ground = null;
        for (ResourceLocation name : blockItem.getBlock().builtInRegistryHolder().tags()
                .map(TagKey::location)
                .sorted()
                .collect(Collectors.toList())) {
            if (!passesLists(name)) {
                continue;
            }
            String path = name.getPath();
            int slash = path.indexOf('/');
            if (slash <= 0 || slash == path.length() - 1 || !path.substring(0, slash).equals(ORES_IN_GROUND)) {
                continue;
            }
            ground = canonicalGround(path.substring(slash + 1));
        }
        return ground;
    }

    /** The white / black list decision for one tag name, shared by the fingerprint and the block ground. */
    private boolean passesLists(ResourceLocation name) {
        InstantUnifyConfig.ListMode mode = InstantUnifyConfig.listMode();
        boolean useWhitelist = mode == InstantUnifyConfig.ListMode.USE_WHITELIST
                || mode == InstantUnifyConfig.ListMode.USE_BOTH_LISTS;
        boolean useBlacklist = mode == InstantUnifyConfig.ListMode.USE_BLACKLIST
                || mode == InstantUnifyConfig.ListMode.USE_BOTH_LISTS;
        if (useWhitelist && !matchesAny(whitelist, name)) {
            return false;
        }
        return !useBlacklist || !matchesAny(blacklist, name);
    }

    private static String nameKey(Item item) {
        ResourceLocation id = ForgeRegistries.ITEMS.getKey(item);
        if (id == null) {
            return null;
        }
        String path = id.getPath();
        for (int i = 0; i < NAME_SHAPE_SUFFIXES.size(); i++) {
            String suffix = NAME_SHAPE_SUFFIXES.get(i);
            if (!path.endsWith(suffix)) {
                continue;
            }
            String material = path.substring(0, path.length() - suffix.length());
            String ground = null;
            for (int j = 0; j < NAME_GROUND_PREFIXES.size(); j++) {
                String prefix = NAME_GROUND_PREFIXES.get(j);
                if (material.startsWith(prefix) && material.length() > prefix.length()) {
                    ground = NAME_GROUND_VALUES.get(j);
                    material = material.substring(prefix.length());
                    break;
                }
            }
            // Only single word materials are accepted, guessing longer names merges unrelated items.
            if (material.isEmpty() || material.indexOf('_') >= 0) {
                continue;
            }
            String shape = NAME_SHAPE_VALUES.get(i);
            return shape.equals("ores")
                    ? shape + "#" + material + "@" + (ground == null ? DEFAULT_GROUND : ground)
                    : shape + "#" + material;
        }
        return null;
    }

    private void ensureUpToDate() {
        if (!dirty) {
            return;
        }
        synchronized (this) {
            if (!dirty) {
                return;
            }
            whitelist = compileAll(InstantUnifyConfig.whitelist());
            blacklist = compileAll(InstantUnifyConfig.blacklist());
            alternatives = buildAlternatives(InstantUnifyConfig.alternatives());
            namespaceAliases = buildNamespaceAliases(InstantUnifyConfig.namespaceAliases());
            dirty = false;
        }
    }

    private static Pattern[] compileAll(List<? extends String> regexes) {
        List<Pattern> patterns = new ArrayList<>(regexes.size());
        for (String regex : regexes) {
            try {
                patterns.add(Pattern.compile(regex));
            } catch (RuntimeException e) {
                // A broken regex must not take the whole event handler down, it is skipped with a warning instead.
                InstantUnify.LOG.warn("Skipping invalid regex '{}': {}", regex, e.getMessage());
                continue;
            }
            if (regex.endsWith(SUB_TAG_SUFFIX)) {
                String bare = regex.substring(0, regex.length() - SUB_TAG_SUFFIX.length());
                if (!bare.isEmpty()) {
                    // "forge:ores\/.+" reads as "forge:ores and everything below it", so the bare tag has to be
                    // accepted as well: mods tag some items with the bare name only (forge:silicon), and without
                    // this such an item keeps no material tag at all and can never be unified.
                    patterns.add(Pattern.compile(Pattern.quote(bare)));
                }
            }
        }
        return patterns.toArray(new Pattern[0]);
    }

    private static boolean matchesAny(Pattern[] patterns, ResourceLocation value) {
        String text = value.toString();
        for (Pattern pattern : patterns) {
            if (pattern.matcher(text).matches()) {
                return true;
            }
        }
        return false;
    }

    private static Map<String, List<String>> buildAlternatives(List<? extends List<? extends String>> configured) {
        Map<String, List<String>> map = new HashMap<>();
        for (List<? extends String> group : configured) {
            for (String name : group) {
                if (name.indexOf(':') >= 0) {
                    InstantUnify.LOG.warn("Skipping alternative '{}': ':' is not allowed in an alternative tag name", name);
                    continue;
                }
                List<String> others = new ArrayList<>(group);
                others.remove(name);
                if (!others.isEmpty()) {
                    map.put(name, others);
                }
            }
        }
        return map;
    }

    private static Map<String, String> buildNamespaceAliases(List<? extends List<? extends String>> configured) {
        Map<String, String> map = new HashMap<>();
        for (List<? extends String> group : configured) {
            if (group.isEmpty()) {
                continue;
            }
            String canonical = group.get(0);
            for (String alias : group) {
                if (!alias.equals(canonical)) {
                    map.put(alias, canonical);
                }
            }
        }
        return map;
    }
}
