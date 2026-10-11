package com.huziyang520.instantunify;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.function.Consumer;

import me.shedaniel.clothconfig2.api.AbstractConfigListEntry;
import me.shedaniel.clothconfig2.api.ConfigBuilder;
import me.shedaniel.clothconfig2.api.ConfigCategory;
import me.shedaniel.clothconfig2.api.ConfigEntryBuilder;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraftforge.client.ConfigScreenHandler;
import net.minecraftforge.fml.ModLoadingContext;

/**
 * The in-game configuration screen, provided through Cloth Config and reachable from the "Config" button of the
 * mod list.
 * <p>
 * Cloth Config is optional: this class is only loaded when the dependency is present (see {@link ClientSetup}),
 * and it is never shaded into the jar. Every entry reads its current value, writes it back immediately and
 * resets to the value declared in {@link InstantUnifyConfig}, so the screen can never drift from the file.
 * <p>
 * Cloth Config's list widget can only append entries, so a button is added to the screen that opens
 * {@link ListEditorScreen} for inserting entries anywhere and for reordering them.
 */
public final class ClothConfigIntegration {

    private ClothConfigIntegration() {
    }

    @SuppressWarnings("removal")
    public static void register() {
        ModLoadingContext.get().registerExtensionPoint(ConfigScreenHandler.ConfigScreenFactory.class,
                () -> new ConfigScreenHandler.ConfigScreenFactory((minecraft, parent) -> new ConfigHubScreen(parent)));
    }

    static Screen createScreen(Minecraft minecraft, Screen parent) {
        ConfigBuilder builder = ConfigBuilder.create()
                .setParentScreen(parent)
                .setTitle(Component.translatable("instantunify.config.title"));
        builder.setGlobalized(true);
        builder.setGlobalizedExpanded(true);
        builder.setSavingRunnable(InstantUnifyConfig::save);
        ConfigEntryBuilder entries = builder.entryBuilder();

        ConfigCategory lists = builder.getOrCreateCategory(Component.translatable("instantunify.config.category.lists"));
        lists.addEntry(stringList(entries, "blacklist", InstantUnifyConfig.blacklist(),
                InstantUnifyConfig.DEFAULT_BLACKLIST, InstantUnifyConfig::setBlacklist));
        lists.addEntry(stringList(entries, "whitelist", InstantUnifyConfig.whitelist(),
                InstantUnifyConfig.DEFAULT_WHITELIST, InstantUnifyConfig::setWhitelist));
        lists.addEntry(entries.startEnumSelector(field("listMode"), InstantUnifyConfig.ListMode.class,
                        InstantUnifyConfig.listMode())
                .setDefaultValue(InstantUnifyConfig.DEFAULT_LIST_MODE)
                .setEnumNameProvider(value -> Component.translatable(
                        "instantunify.config.listMode." + value.name().toLowerCase(Locale.ROOT)))
                .setTooltip(tooltip("listMode"))
                .setSaveConsumer(InstantUnifyConfig::setListMode)
                .build());

        ConfigCategory mods = builder.getOrCreateCategory(Component.translatable("instantunify.config.category.general"));
        mods.addEntry(stringList(entries, "preferredMods", InstantUnifyConfig.preferredMods(),
                InstantUnifyConfig.DEFAULT_PREFERRED_MODS, InstantUnifyConfig::setPreferredMods));
        mods.addEntry(stringList(entries, "blacklistMods", InstantUnifyConfig.blacklistMods(),
                InstantUnifyConfig.DEFAULT_BLACKLIST_MODS, InstantUnifyConfig::setBlacklistMods));
        mods.addEntry(stringList(entries, "blacklistItems", InstantUnifyConfig.blacklistItems(),
                InstantUnifyConfig.DEFAULT_BLACKLIST_ITEMS, InstantUnifyConfig::setBlacklistItems));
        mods.addEntry(stringList(entries, "alternatives", toLines(InstantUnifyConfig.alternatives()),
                toLines(InstantUnifyConfig.DEFAULT_ALTERNATIVES),
                lines -> InstantUnifyConfig.setAlternatives(fromLines(lines))));
        mods.addEntry(stringList(entries, "namespaceAliases", toLines(InstantUnifyConfig.namespaceAliases()),
                toLines(InstantUnifyConfig.DEFAULT_NAMESPACE_ALIASES),
                lines -> InstantUnifyConfig.setNamespaceAliases(fromLines(lines))));

        ConfigCategory events = builder.getOrCreateCategory(Component.translatable("instantunify.config.category.events"));
        events.addEntry(booleanEntry(entries, "drop", InstantUnifyConfig.drop(), InstantUnifyConfig.DEFAULT_DROP,
                InstantUnifyConfig::setDrop));
        events.addEntry(booleanEntry(entries, "death", InstantUnifyConfig.death(), InstantUnifyConfig.DEFAULT_DEATH,
                InstantUnifyConfig::setDeath));
        events.addEntry(booleanEntry(entries, "second", InstantUnifyConfig.second(), InstantUnifyConfig.DEFAULT_SECOND,
                InstantUnifyConfig::setSecond));
        events.addEntry(booleanEntry(entries, "change", InstantUnifyConfig.change(), InstantUnifyConfig.DEFAULT_CHANGE,
                InstantUnifyConfig::setChange));
        events.addEntry(booleanEntry(entries, "gui", InstantUnifyConfig.gui(), InstantUnifyConfig.DEFAULT_GUI,
                InstantUnifyConfig::setGui));
        events.addEntry(booleanEntry(entries, "harvest", InstantUnifyConfig.harvest(), InstantUnifyConfig.DEFAULT_HARVEST,
                InstantUnifyConfig::setHarvest));

        ConfigCategory performance = builder
                .getOrCreateCategory(Component.translatable("instantunify.config.category.performance"));
        performance.addEntry(booleanEntry(entries, "indexCache", InstantUnifyConfig.indexCache(),
                InstantUnifyConfig.DEFAULT_INDEX_CACHE, InstantUnifyConfig::setIndexCache));
        performance.addEntry(entries.startIntField(field("lazyIndexRefreshSeconds"),
                        InstantUnifyConfig.lazyIndexRefreshSeconds())
                .setDefaultValue(InstantUnifyConfig.DEFAULT_LAZY_INDEX_REFRESH_SECONDS)
                .setMin(0)
                .setMax(86400)
                .setTooltip(tooltip("lazyIndexRefreshSeconds"))
                .setSaveConsumer(InstantUnifyConfig::setLazyIndexRefreshSeconds)
                .build());

        return builder.build();
    }

    private static AbstractConfigListEntry stringList(ConfigEntryBuilder entries, String key, List<? extends String> current,
            List<String> defaults, Consumer<List<String>> saveConsumer) {
        return entries.startStrList(field(key), new ArrayList<>(current))
                .setDefaultValue(new ArrayList<>(defaults))
                .setTooltip(tooltip(key))
                .setInsertButtonEnabled(true)
                .setDeleteButtonEnabled(true)
                // Lets a new entry be placed in front of the row instead of only at the end of the list.
                .setInsertInFront(true)
                .setSaveConsumer(saveConsumer)
                .build();
    }

    private static AbstractConfigListEntry booleanEntry(ConfigEntryBuilder entries, String key, boolean current,
            boolean defaultValue, Consumer<Boolean> saveConsumer) {
        return entries.startBooleanToggle(field(key), current)
                .setDefaultValue(defaultValue)
                .setTooltip(tooltip(key))
                .setSaveConsumer(saveConsumer)
                .build();
    }

    private static Component field(String key) {
        return Component.translatable("instantunify.config." + key);
    }

    private static Component tooltip(String key) {
        return Component.translatable("instantunify.config." + key + ".tooltip");
    }

    /** One line per group, the names of a group separated by commas: {@code aluminum,aluminium,bauxite}. */
    private static List<String> toLines(List<? extends List<? extends String>> groups) {
        List<String> lines = new ArrayList<>(groups.size());
        for (List<? extends String> group : groups) {
            lines.add(String.join(",", group));
        }
        return lines;
    }

    private static List<List<String>> fromLines(List<String> lines) {
        List<List<String>> groups = new ArrayList<>(lines.size());
        for (String line : lines) {
            List<String> names = new ArrayList<>();
            for (String name : line.split(",")) {
                String trimmed = name.trim();
                if (!trimmed.isEmpty()) {
                    names.add(trimmed);
                }
            }
            if (!names.isEmpty()) {
                groups.add(names);
            }
        }
        return groups;
    }
}
