package com.huziyang520.instantunify;

import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.Consumer;

import me.shedaniel.clothconfig2.api.AbstractConfigEntry;
import me.shedaniel.clothconfig2.api.AbstractConfigListEntry;
import me.shedaniel.clothconfig2.api.ConfigBuilder;
import me.shedaniel.clothconfig2.api.ConfigCategory;
import me.shedaniel.clothconfig2.api.ConfigEntryBuilder;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.FormattedText;
import net.minecraft.network.chat.Style;
import net.minecraftforge.client.ConfigScreenHandler;
import net.minecraftforge.fml.ModLoadingContext;

/**
 * The in-game configuration screen, provided through Cloth Config and reachable from the "Config" button of the
 * mod list.
 * <p>
 * Cloth Config is optional: this class is only loaded when the dependency is present (see {@link ClientSetup}),
 * and it is never shaded into the jar. Every entry reads its current value and resets to the value declared in
 * {@link InstantUnifyConfig}, so the screen can never drift from the file.
 * <p>
 * Cloth Config's list widget can only append entries, so a button in the hub screen opens {@link ListEditorScreen}
 * for inserting entries anywhere and for reordering them.
 * <p>
 * Every entry carries both a hover tooltip and a description line printed under it. Tooltips of configuration
 * screens are drawn by third party user interface mods and can end up blank, while a line in the list is always
 * readable.
 */
public final class ClothConfigIntegration {

    /** Width a tooltip line is wrapped at, the same value vanilla uses for item tooltips. */
    private static final int TOOLTIP_WIDTH = 200;

    /**
     * Tooltips of the screen being built. They are kept here instead of being handed to Cloth Config, whose own
     * tooltip drawing cannot be used in this layout (see {@link ConfigTooltipRenderer}).
     */
    private static final Map<AbstractConfigEntry<?>, List<Component>> PENDING_TOOLTIPS = new IdentityHashMap<>();

    private ClothConfigIntegration() {
    }

    @SuppressWarnings("removal")
    public static void register() {
        ModLoadingContext.get().registerExtensionPoint(ConfigScreenHandler.ConfigScreenFactory.class,
                () -> new ConfigScreenHandler.ConfigScreenFactory((minecraft, parent) -> new ConfigHubScreen(parent)));
        ConfigTooltipRenderer.register();
    }

    static Screen createScreen(Minecraft minecraft, Screen parent) {
        ConfigBuilder builder = ConfigBuilder.create()
                .setParentScreen(parent)
                .setTitle(Component.translatable("instantunify.config.title"));
        builder.setGlobalized(true);
        builder.setGlobalizedExpanded(true);
        builder.setSavingRunnable(InstantUnifyConfig::save);
        ConfigEntryBuilder entries = builder.entryBuilder();
        PENDING_TOOLTIPS.clear();

        ConfigCategory lists = builder.getOrCreateCategory(Component.translatable("instantunify.config.category.lists"));
        add(lists, entries, "blacklist", stringList(entries, "blacklist", InstantUnifyConfig.blacklist(),
                InstantUnifyConfig.DEFAULT_BLACKLIST, InstantUnifyConfig::setBlacklist));
        add(lists, entries, "whitelist", stringList(entries, "whitelist", InstantUnifyConfig.whitelist(),
                InstantUnifyConfig.DEFAULT_WHITELIST, InstantUnifyConfig::setWhitelist));
        add(lists, entries, "listMode", entries
                .startEnumSelector(field("listMode"), InstantUnifyConfig.ListMode.class, InstantUnifyConfig.listMode())
                .setDefaultValue(InstantUnifyConfig.DEFAULT_LIST_MODE)
                .setEnumNameProvider(value -> Component.translatable(
                        "instantunify.config.listMode." + value.name().toLowerCase(Locale.ROOT)))
                .setSaveConsumer(InstantUnifyConfig::setListMode)
                .build());

        ConfigCategory mods = builder.getOrCreateCategory(Component.translatable("instantunify.config.category.general"));
        add(mods, entries, "preferredMods", stringList(entries, "preferredMods", InstantUnifyConfig.preferredMods(),
                InstantUnifyConfig.DEFAULT_PREFERRED_MODS, InstantUnifyConfig::setPreferredMods));
        add(mods, entries, "blacklistMods", stringList(entries, "blacklistMods", InstantUnifyConfig.blacklistMods(),
                InstantUnifyConfig.DEFAULT_BLACKLIST_MODS, InstantUnifyConfig::setBlacklistMods));
        add(mods, entries, "exemptMods", stringList(entries, "exemptMods", InstantUnifyConfig.exemptMods(),
                InstantUnifyConfig.DEFAULT_EXEMPT_MODS, InstantUnifyConfig::setExemptMods));
        add(mods, entries, "blacklistItems", stringList(entries, "blacklistItems", InstantUnifyConfig.blacklistItems(),
                InstantUnifyConfig.DEFAULT_BLACKLIST_ITEMS, InstantUnifyConfig::setBlacklistItems));
        add(mods, entries, "alternatives", stringList(entries, "alternatives", toLines(InstantUnifyConfig.alternatives()),
                toLines(InstantUnifyConfig.DEFAULT_ALTERNATIVES),
                lines -> InstantUnifyConfig.setAlternatives(fromLines(lines))));
        add(mods, entries, "namespaceAliases", stringList(entries, "namespaceAliases",
                toLines(InstantUnifyConfig.namespaceAliases()),
                toLines(InstantUnifyConfig.DEFAULT_NAMESPACE_ALIASES),
                lines -> InstantUnifyConfig.setNamespaceAliases(fromLines(lines))));
        // Kept at the end of the category on purpose: it is a switch about the lists above.
        add(mods, entries, "sameModNoUnify",
                booleanEntry(entries, "sameModNoUnify", InstantUnifyConfig.sameModNoUnify(),
                        InstantUnifyConfig.DEFAULT_SAME_MOD_NO_UNIFY, InstantUnifyConfig::setSameModNoUnify));

        ConfigCategory events = builder.getOrCreateCategory(Component.translatable("instantunify.config.category.events"));
        add(events, entries, "drop",
                booleanEntry(entries, "drop", InstantUnifyConfig.drop(), InstantUnifyConfig.DEFAULT_DROP, InstantUnifyConfig::setDrop));
        add(events, entries, "death",
                booleanEntry(entries, "death", InstantUnifyConfig.death(), InstantUnifyConfig.DEFAULT_DEATH, InstantUnifyConfig::setDeath));
        add(events, entries, "second",
                booleanEntry(entries, "second", InstantUnifyConfig.second(), InstantUnifyConfig.DEFAULT_SECOND, InstantUnifyConfig::setSecond));
        add(events, entries, "change",
                booleanEntry(entries, "change", InstantUnifyConfig.change(), InstantUnifyConfig.DEFAULT_CHANGE, InstantUnifyConfig::setChange));
        add(events, entries, "gui",
                booleanEntry(entries, "gui", InstantUnifyConfig.gui(), InstantUnifyConfig.DEFAULT_GUI, InstantUnifyConfig::setGui));
        add(events, entries, "harvest",
                booleanEntry(entries, "harvest", InstantUnifyConfig.harvest(), InstantUnifyConfig.DEFAULT_HARVEST, InstantUnifyConfig::setHarvest));

        ConfigCategory performance = builder
                .getOrCreateCategory(Component.translatable("instantunify.config.category.performance"));
        add(performance, entries, "indexCache",
                booleanEntry(entries, "indexCache", InstantUnifyConfig.indexCache(),
                        InstantUnifyConfig.DEFAULT_INDEX_CACHE, InstantUnifyConfig::setIndexCache));
        add(performance, entries, "lazyIndexRefreshSeconds", entries
                .startIntField(field("lazyIndexRefreshSeconds"), InstantUnifyConfig.lazyIndexRefreshSeconds())
                .setDefaultValue(InstantUnifyConfig.DEFAULT_LAZY_INDEX_REFRESH_SECONDS)
                .setMin(0)
                .setMax(86400)
                .setSaveConsumer(InstantUnifyConfig::setLazyIndexRefreshSeconds)
                .build());

        Screen screen = builder.build();
        ConfigTooltipRenderer.own(screen, PENDING_TOOLTIPS);
        return screen;
    }

    private static void add(ConfigCategory category, ConfigEntryBuilder entries, String key,
            AbstractConfigListEntry control) {
        PENDING_TOOLTIPS.put(control, wrapped(tooltip(key)));
        category.addEntry(control);
    }

    private static AbstractConfigListEntry stringList(ConfigEntryBuilder entries, String key, List<? extends String> current,
            List<String> defaults, Consumer<List<String>> saveConsumer) {
        return entries.startStrList(field(key), new ArrayList<>(current))
                .setDefaultValue(new ArrayList<>(defaults))
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
                .setSaveConsumer(saveConsumer)
                .build();
    }

    /**
     * Breaks a long description into several lines, otherwise the box stretches across the whole screen and is
     * unreadable. The same width vanilla uses for item tooltips.
     */
    private static List<Component> wrapped(Component text) {
        Font font = Minecraft.getInstance().font;
        List<Component> lines = new ArrayList<>();
        for (FormattedText line : font.getSplitter().splitLines(text, TOOLTIP_WIDTH, Style.EMPTY)) {
            lines.add(Component.literal(line.getString()));
        }
        return lines.isEmpty() ? List.of(text) : lines;
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
