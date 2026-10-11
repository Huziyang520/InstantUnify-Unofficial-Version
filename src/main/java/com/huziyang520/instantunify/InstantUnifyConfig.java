package com.huziyang520.instantunify;

import java.util.List;

import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.config.ModConfig;

/**
 * The single source of truth for every setting. Business code never touches {@link ForgeConfigSpec.ConfigValue}
 * directly, it asks this class, so a configuration change (file edit, in-game screen, "/reload", "/instantunify
 * reload") is picked up by the very next event without restarting anything.
 */
public final class InstantUnifyConfig {

    /** How the whitelist and the blacklist are combined. */
    public enum ListMode {
        USE_WHITELIST,
        USE_BLACKLIST,
        USE_BOTH_LISTS,
        USE_NO_LIST
    }

    public static final ForgeConfigSpec SPEC;

    private static final ForgeConfigSpec.ConfigValue<List<? extends String>> BLACKLIST;
    private static final ForgeConfigSpec.ConfigValue<List<? extends String>> WHITELIST;
    private static final ForgeConfigSpec.EnumValue<ListMode> LIST_MODE;
    private static final ForgeConfigSpec.ConfigValue<List<? extends String>> PREFERRED_MODS;
    private static final ForgeConfigSpec.ConfigValue<List<? extends String>> BLACKLIST_MODS;
    private static final ForgeConfigSpec.ConfigValue<List<? extends List<? extends String>>> ALTERNATIVES;

    private static final ForgeConfigSpec.BooleanValue DROP;
    private static final ForgeConfigSpec.BooleanValue DEATH;
    private static final ForgeConfigSpec.BooleanValue SECOND;
    private static final ForgeConfigSpec.BooleanValue CHANGE;
    private static final ForgeConfigSpec.BooleanValue GUI;
    private static final ForgeConfigSpec.BooleanValue HARVEST;

    private static final ForgeConfigSpec.BooleanValue PERFORMANCE_INDEX_CACHE;
    private static final ForgeConfigSpec.IntValue PERFORMANCE_LAZY_INDEX_REFRESH_SECONDS;

    static {
        ForgeConfigSpec.Builder b = new ForgeConfigSpec.Builder();

        BLACKLIST = b.comment("Tag names that shouldn't be unified (supports regex)")
                .defineList("blacklist", List.of("minecraft:.+", "forge:glass.+"), o -> o instanceof String);

        WHITELIST = b.comment("Tag names that should be unified (supports regex)")
                .defineList("whitelist", List.of(
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
                        "c:ores\\/.+",
                        "c:ingots\\/.+",
                        "c:nuggets\\/.+",
                        "c:raw_materials\\/.+",
                        "c:storage_blocks\\/.+",
                        "c:gems\\/.+",
                        "c:dusts\\/.+",
                        "c:gears\\/.+",
                        "c:plates\\/.+",
                        "c:rods\\/.+"), o -> o instanceof String);

        LIST_MODE = b.comment("How the two lists above are combined")
                .defineEnum("listMode", ListMode.USE_BOTH_LISTS);

        PREFERRED_MODS = b.comment("Preferred Mods: the first mod in this list that provides a material wins")
                .defineList("preferredMods", List.of(
                        "minecraft",
                        "alltheores",
                        "allthemodium",
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
                        "enderio"), o -> o instanceof String);

        BLACKLIST_MODS = b.comment("Blacklisted Mods: their items are never replaced and never used as a target")
                .defineList("blacklistMods", List.of("chisel", "astralsorcery", "ae2"), o -> o instanceof String);

        ALTERNATIVES = b.comment("Tag names that should be unified even if they are different")
                .defineList("alternatives", List.of(List.of("aluminum", "aluminium", "bauxite")),
                        o -> o instanceof List && (((List<?>) o).isEmpty() || ((List<?>) o).get(0) instanceof String));

        b.push("Unify event");
        DROP = b.comment("Unify when items drop").define("drop", true);
        DEATH = b.comment("Unify drops when entities die").define("death", true);
        SECOND = b.comment("Unify every second items in player's inventory").define("second", false);
        CHANGE = b.comment("Unify items in player's inventory when they change").define("change", true);
        GUI = b.comment("Unify items when a container is opened (writes into containers)").define("gui", false);
        HARVEST = b.comment("Unify loot table output (block drops, mob drops, chests, fishing)")
                .define("harvest", false);
        b.pop();

        b.push("performance");
        PERFORMANCE_INDEX_CACHE = b.comment(
                "Cache the material index (tag fingerprint -> dominant item) instead of scanning the item registry on every drop",
                "Turning this off restores the slow behaviour, useful for troubleshooting")
                .define("indexCache", true);
        PERFORMANCE_LAZY_INDEX_REFRESH_SECONDS = b.comment(
                "Rebuild the material index automatically after this many seconds, 0 = rebuild only on tag/config changes")
                .defineInRange("lazyIndexRefreshSeconds", 0, 0, 86400);
        b.pop();

        SPEC = b.build();
    }

    private InstantUnifyConfig() {
    }

    public static void register() {
        ModLoadingContext.get().registerConfig(ModConfig.Type.COMMON, SPEC);
    }

    /** Writes the current values back to disk, used by the in-game screen and by the reload command. */
    public static void save() {
        SPEC.save();
    }

    public static List<? extends String> blacklist() {
        return BLACKLIST.get();
    }

    public static List<? extends String> whitelist() {
        return WHITELIST.get();
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

    public static List<? extends List<? extends String>> alternatives() {
        return ALTERNATIVES.get();
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

    public static void setAlternatives(List<List<String>> value) {
        ALTERNATIVES.set(value);
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

    private static void persist() {
        SPEC.save();
        ItemUnifier.reload();
    }
}
