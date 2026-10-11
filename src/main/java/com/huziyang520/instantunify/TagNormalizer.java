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
import net.minecraft.world.item.Item;

/**
 * Turns an item into its "material fingerprint": the sorted list of tag names that survive the alternatives
 * expansion and the white/black list filtering. Two items are interchangeable when their fingerprints are equal.
 * <p>
 * Regexes, the alternatives map and the namespace aliases are compiled once and rebuilt only when the
 * configuration changes.
 */
public final class TagNormalizer {

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

        InstantUnifyConfig.ListMode mode = InstantUnifyConfig.listMode();
        boolean useWhitelist = mode == InstantUnifyConfig.ListMode.USE_WHITELIST
                || mode == InstantUnifyConfig.ListMode.USE_BOTH_LISTS;
        boolean useBlacklist = mode == InstantUnifyConfig.ListMode.USE_BLACKLIST
                || mode == InstantUnifyConfig.ListMode.USE_BOTH_LISTS;

        List<ResourceLocation> result = new ArrayList<>(names.size());
        for (ResourceLocation name : names) {
            if (useWhitelist && !matchesAny(whitelist, name)) {
                continue;
            }
            if (useBlacklist && matchesAny(blacklist, name)) {
                continue;
            }
            result.add(name);
        }
        Collections.sort(result);
        return result;
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
