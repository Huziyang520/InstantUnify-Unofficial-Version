package com.huziyang520.instantunify;

import java.util.List;

import net.neoforged.fml.ModContainer;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.neoforge.common.ModConfigSpec;

/**
 * The single source of truth for every setting. Business code never touches {@link ModConfigSpec.ConfigValue}
 * directly, it asks this class, so a configuration change (file edit, in-game screen, "/reload", "/instantunify
 * reload") is picked up by the very next event without restarting anything.
 * <p>
 * The default values are declared once as constants and used both for the spec and for the in-game screen, so
 * "reset" on the screen always restores the factory default and can never drift from the file.
 */
public final class InstantUnifyConfig {

    /** How the whitelist and the blacklist are combined. */
    public enum ListMode {
        USE_WHITELIST,
        USE_BLACKLIST,
        USE_BOTH_LISTS,
        USE_NO_LIST
    }

    // --- Default values (single source of truth) -------------------------------------------------------

    public static final List<String> DEFAULT_BLACKLIST = List.of("minecraft:.+", "forge:glass.+");

    public static final List<String> DEFAULT_WHITELIST = List.of(
            "forge:ores\\/.+",
            "forge:ingots\\/.+",
            "forge:nuggets\\/.+",
            "forge:raw_materials\\/.+",
            "forge:storage_blocks\\/.+",
            "forge:gems\\/.+",
            "forge:dusts\\/.+",
            "forge:gears\\/.+",
            "forge:plates\\/.+",
            "forge:rods\\/.+",
            // Distinguishes the blocks an ore is placed in (stone, deepslate, netherrack, end stone, ...).
            // Without this every variant of the same material shares one fingerprint and they all collapse
            // into a single item, which turns every ore into the deepslate one.
            "forge:ores_in_ground\\/.+",
            // Silicon and silver style materials are tag categories of their own, none of the rows above covers
            // them. The "<name>\\/.+" form also accepts the bare tag (see TagNormalizer), which is what catches
            // items that are tagged with forge:silicon and nothing else.
            "forge:silicon\\/.+",
            "c:ores\\/.+",
            "c:ingots\\/.+",
            "c:nuggets\\/.+",
            "c:raw_materials\\/.+",
            "c:storage_blocks\\/.+",
            "c:gems\\/.+",
            "c:dusts\\/.+",
            "c:gears\\/.+",
            "c:plates\\/.+",
            "c:rods\\/.+",
            "c:ores_in_ground\\/.+",
            "c:silicon\\/.+");

    public static final ListMode DEFAULT_LIST_MODE = ListMode.USE_BOTH_LISTS;

    public static final List<String> DEFAULT_PREFERRED_MODS = List.of(
            "minecraft",
            "alltheores",
            "allthemodium",
            "ftbmaterials",
            "kubejs",
            "tconstruct",
            "thermal",
            "thermal_foundation",
            "mekanism",
            "create",
            "immersiveengineering",
            "occultism",
            "ae2",
            "chemlib",
            "biggerreactors",
            "silentgear",
            "botania",
            "thermal_extra",
            "industrialforegoing",
            "pneumaticcraft",
            "embers",
            "iceandfire",
            "refinedstorage",
            "enderio");

    public static final List<String> DEFAULT_BLACKLIST_MODS = List.of("chisel", "astralsorcery");

    /**
     * Mods whose items are left alone but may still be the item everything else turns into. Used for mods that
     * own a material (AE2 and its silicon) and must not be replaced, while the copies other mods ship should
     * collapse into it.
     */
    public static final List<String> DEFAULT_EXEMPT_MODS = List.of("ae2");

    /** Keeps items of one mod from being merged into each other, which is the safe default. */
    public static final boolean DEFAULT_SAME_MOD_NO_UNIFY = true;

    public static final List<String> DEFAULT_BLACKLIST_ITEMS = List.of();

    public static final List<List<String>> DEFAULT_ALTERNATIVES =
            List.of(List.of("aluminum", "aluminium", "bauxite"));

    /**
     * Namespaces that mean the same thing. An increasing number of mods ship the {@code c:} names, while most
     * still use the older {@code forge:} ones; both spellings of the same material must end up in the same
     * fingerprint. The first entry of a group is the canonical one.
     */
    public static final List<List<String>> DEFAULT_NAMESPACE_ALIASES = List.of(List.of("forge", "c"));

    public static final boolean DEFAULT_DROP = true;
    public static final boolean DEFAULT_DEATH = true;
    public static final boolean DEFAULT_SECOND = false;
    public static final boolean DEFAULT_CHANGE = true;
    public static final boolean DEFAULT_GUI = false;
    public static final boolean DEFAULT_HARVEST = false;
    public static final boolean DEFAULT_INDEX_CACHE = true;
    public static final int DEFAULT_LAZY_INDEX_REFRESH_SECONDS = 0;

    // --- Spec -----------------------------------------------------------------------------------------

    public static final ModConfigSpec SPEC;

    private static final ModConfigSpec.ConfigValue<List<? extends String>> BLACKLIST;
    private static final ModConfigSpec.ConfigValue<List<? extends String>> WHITELIST;
    private static final ModConfigSpec.EnumValue<ListMode> LIST_MODE;
    private static final ModConfigSpec.ConfigValue<List<? extends String>> PREFERRED_MODS;
    private static final ModConfigSpec.ConfigValue<List<? extends String>> BLACKLIST_MODS;
    private static final ModConfigSpec.ConfigValue<List<? extends String>> EXEMPT_MODS;
    private static final ModConfigSpec.ConfigValue<List<? extends String>> BLACKLIST_ITEMS;
    private static final ModConfigSpec.ConfigValue<List<? extends List<? extends String>>> ALTERNATIVES;
    private static final ModConfigSpec.ConfigValue<List<? extends List<? extends String>>> NAMESPACE_ALIASES;

    private static final ModConfigSpec.BooleanValue DROP;
    private static final ModConfigSpec.BooleanValue DEATH;
    private static final ModConfigSpec.BooleanValue SECOND;
    private static final ModConfigSpec.BooleanValue CHANGE;
    private static final ModConfigSpec.BooleanValue GUI;
    private static final ModConfigSpec.BooleanValue HARVEST;
    private static final ModConfigSpec.BooleanValue SAME_MOD_NO_UNIFY;

    private static final ModConfigSpec.BooleanValue PERFORMANCE_INDEX_CACHE;
    private static final ModConfigSpec.IntValue PERFORMANCE_LAZY_INDEX_REFRESH_SECONDS;

    private static final java.util.function.Predicate<Object> STRING_ELEMENT = o -> o instanceof String;
    private static final java.util.function.Predicate<Object> STRING_LIST_ELEMENT =
            o -> o instanceof List && (((List<?>) o).isEmpty() || ((List<?>) o).get(0) instanceof String);

    static {
        ModConfigSpec.Builder b = new ModConfigSpec.Builder();

        BLACKLIST = b.comment("Tag names that shouldn't be unified (supports regex)",
                "A name written as '<tag>\\/.+' covers the tag itself and everything below it")
                .defineList("blacklist", DEFAULT_BLACKLIST, STRING_ELEMENT);

        WHITELIST = b.comment("Tag names that should be unified (supports regex)",
                "A name written as '<tag>\\/.+' covers the tag itself and everything below it")
                .defineList("whitelist", DEFAULT_WHITELIST, STRING_ELEMENT);

        LIST_MODE = b.comment("How the two lists above are combined")
                .defineEnum("listMode", DEFAULT_LIST_MODE);

        PREFERRED_MODS = b.comment("Preferred Mods: the first mod in this list that provides a material wins")
                .defineList("preferredMods", DEFAULT_PREFERRED_MODS, STRING_ELEMENT);

        BLACKLIST_MODS = b.comment("Blacklisted Mods: their items are never replaced and never used as a target")
                .defineList("blacklistMods", DEFAULT_BLACKLIST_MODS, STRING_ELEMENT);

        EXEMPT_MODS = b.comment(
                "Exempt Mods: their items are never replaced, but other mods' items may still turn into them",
                "Use this for a mod that owns a material and should stay the way it is (AE2 silicon)")
                .defineList("exemptMods", DEFAULT_EXEMPT_MODS, STRING_ELEMENT);

        BLACKLIST_ITEMS = b.comment("Blacklisted Items: these item ids are never replaced (empty by default)")
                .defineList("blacklistItems", DEFAULT_BLACKLIST_ITEMS, STRING_ELEMENT);

        ALTERNATIVES = b.comment("Tag names that should be unified even if they are different")
                .defineList("alternatives", DEFAULT_ALTERNATIVES, STRING_LIST_ELEMENT);

        NAMESPACE_ALIASES = b.comment(
                "Namespaces that mean the same thing, the first entry of a group is the canonical one",
                "Keeps items that only ship one of the two spellings (forge: and c:) in the same group")
                .defineList("namespaceAliases", DEFAULT_NAMESPACE_ALIASES, STRING_LIST_ELEMENT);

        SAME_MOD_NO_UNIFY = b.comment(
                "Never unify two items of the same mod with each other",
                "On by default: mods ship variants that only make sense next to each other")
                .define("sameModNoUnify", DEFAULT_SAME_MOD_NO_UNIFY);

        b.push("Unify event");
        DROP = b.comment("Unify when items drop").define("drop", DEFAULT_DROP);
        DEATH = b.comment("Unify drops when entities die").define("death", DEFAULT_DEATH);
        SECOND = b.comment("Unify every second items in player's inventory").define("second", DEFAULT_SECOND);
        CHANGE = b.comment("Unify items in player's inventory when they change").define("change", DEFAULT_CHANGE);
        GUI = b.comment("Unify items when a container is opened (writes into containers)").define("gui", DEFAULT_GUI);
        HARVEST = b.comment("Unify loot table output (block drops, mob drops, chests, fishing)")
                .define("harvest", DEFAULT_HARVEST);
        b.pop();

        b.push("performance");
        PERFORMANCE_INDEX_CACHE = b.comment(
                "Cache the material index (tag fingerprint -> dominant item) instead of scanning the item registry on every drop",
                "Turning this off restores the slow behaviour, useful for troubleshooting")
                .define("indexCache", DEFAULT_INDEX_CACHE);
        PERFORMANCE_LAZY_INDEX_REFRESH_SECONDS = b.comment(
                "Rebuild the material index automatically after this many seconds, 0 = rebuild only on tag/config changes")
                .defineInRange("lazyIndexRefreshSeconds", DEFAULT_LAZY_INDEX_REFRESH_SECONDS, 0, 86400);
        b.pop();

        SPEC = b.build();
    }

    private InstantUnifyConfig() {
    }

    public static void register(ModContainer modContainer) {
        modContainer.registerConfig(ModConfig.Type.COMMON, SPEC);
    }

    /** Writes the current values back to disk, used by the in-game screen and by the reload command. */
    public static void save() {
        SPEC.save();
        ConfigFileFormatter.format();
    }

    public static List<? extends String> blacklist() {
        return BLACKLIST.get();
    }

    public static List<? extends String> whitelist() {
        return WHITELIST.get();
    }

    public static List<? extends String> exemptMods() {
        return EXEMPT_MODS.get();
    }

    public static boolean sameModNoUnify() {
        return SAME_MOD_NO_UNIFY.get();
    }

    public static ListMode listMode() {
        return LIST_MODE.get();
    }

    public static List<? extends String> preferredMods() {
        return PREFERRED_MODS.get();
    }

    public static List<? extends String> blacklistMods() {
        return BLACKLIST_MODS.get();
    }

    public static List<? extends String> blacklistItems() {
        return BLACKLIST_ITEMS.get();
    }

    public static List<? extends List<? extends String>> alternatives() {
        return ALTERNATIVES.get();
    }

    public static List<? extends List<? extends String>> namespaceAliases() {
        return NAMESPACE_ALIASES.get();
    }

    public static boolean drop() {
        return DROP.get();
    }

    public static boolean death() {
        return DEATH.get();
    }

    public static boolean second() {
        return SECOND.get();
    }

    public static boolean change() {
        return CHANGE.get();
    }

    public static boolean gui() {
        return GUI.get();
    }

    public static boolean harvest() {
        return HARVEST.get();
    }

    public static boolean indexCache() {
        return PERFORMANCE_INDEX_CACHE.get();
    }

    public static int lazyIndexRefreshSeconds() {
        return PERFORMANCE_LAZY_INDEX_REFRESH_SECONDS.get();
    }

    // --- Setters used by the in-game screen -------------------------------------------------------------
    // Each setter writes into memory, persists the file and drops the derived caches, so the change is
    // effective immediately and never depends on the order of the screen's callbacks.

    public static void setBlacklist(List<String> value) {
        BLACKLIST.set(value);
        persist();
    }

    public static void setWhitelist(List<String> value) {
        WHITELIST.set(value);
        persist();
    }

    public static void setListMode(ListMode value) {
        LIST_MODE.set(value);
        persist();
    }

    public static void setPreferredMods(List<String> value) {
        PREFERRED_MODS.set(value);
        persist();
    }

    public static void setBlacklistMods(List<String> value) {
        BLACKLIST_MODS.set(value);
        persist();
    }

    public static void setExemptMods(List<String> value) {
        EXEMPT_MODS.set(value);
        persist();
    }

    public static void setSameModNoUnify(boolean value) {
        SAME_MOD_NO_UNIFY.set(value);
        persist();
    }

    public static void setBlacklistItems(List<String> value) {
        BLACKLIST_ITEMS.set(value);
        persist();
    }

    public static void setAlternatives(List<List<String>> value) {
        ALTERNATIVES.set(value);
        persist();
    }

    public static void setNamespaceAliases(List<List<String>> value) {
        NAMESPACE_ALIASES.set(value);
        persist();
    }

    public static void setDrop(boolean value) {
        DROP.set(value);
        persist();
    }

    public static void setDeath(boolean value) {
        DEATH.set(value);
        persist();
    }

    public static void setSecond(boolean value) {
        SECOND.set(value);
        persist();
    }

    public static void setChange(boolean value) {
        CHANGE.set(value);
        persist();
    }

    public static void setGui(boolean value) {
        GUI.set(value);
        persist();
    }

    public static void setHarvest(boolean value) {
        HARVEST.set(value);
        persist();
    }

    public static void setIndexCache(boolean value) {
        PERFORMANCE_INDEX_CACHE.set(value);
        persist();
    }

    public static void setLazyIndexRefreshSeconds(int value) {
        PERFORMANCE_LAZY_INDEX_REFRESH_SECONDS.set(value);
        persist();
    }

    /**
     * A change only updates the value in memory and drops the caches. Writing the file is left to the caller
     * (the screen writes once when it closes), because the loader and the mod would otherwise write the same
     * file at the same time and the file ends up truncated or locked.
     */
    private static void persist() {
        ItemUnifier.reload();
    }
}
