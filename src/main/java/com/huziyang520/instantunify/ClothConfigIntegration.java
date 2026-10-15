package com.huziyang520.instantunify;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Supplier;

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
 * Only the switches and the numbers are shown here. The ordered lists are edited by {@link ListEditorScreen},
 * which is reached from the hub screen, so every list lives in exactly one place.
 * <p>
 * The categories are shown as buttons along the top, which is Cloth Config's plain layout: its "globalized"
 * layout moves them into a tree on the left, and that layout clips and covers the tooltips of this screen.
 */
public final class ClothConfigIntegration {

    /** Width a tooltip line is wrapped at, the same value vanilla uses for item tooltips. */
    private static final int TOOLTIP_WIDTH = 200;

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
        builder.setSavingRunnable(InstantUnifyConfig::save);
        ConfigEntryBuilder entries = builder.entryBuilder();

        ConfigCategory lists = builder.getOrCreateCategory(Component.translatable("instantunify.config.category.lists"));
        add(lists, entries, "listMode", entries
                .startEnumSelector(field("listMode"), InstantUnifyConfig.ListMode.class, InstantUnifyConfig.listMode())
                .setDefaultValue(InstantUnifyConfig.DEFAULT_LIST_MODE)
                .setEnumNameProvider(value -> Component.translatable(
                        "instantunify.config.listMode." + value.name().toLowerCase(Locale.ROOT)))
                .setTooltipSupplier(tooltipSupplier("listMode"))
                .setSaveConsumer(InstantUnifyConfig::setListMode)
                .build());

        ConfigCategory mods = builder.getOrCreateCategory(Component.translatable("instantunify.config.category.general"));
        // The ordered lists are not shown here: they are edited by the list editor screen, so a list can never be
        // maintained in two places.
        // Kept at the end of the category on purpose: it is a switch about the lists.
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
                .setTooltipSupplier(tooltipSupplier("lazyIndexRefreshSeconds"))
                .setSaveConsumer(InstantUnifyConfig::setLazyIndexRefreshSeconds)
                .build());

        return builder.build();
    }

    private static void add(ConfigCategory category, ConfigEntryBuilder entries, String key,
            AbstractConfigListEntry control) {
        category.addEntry(control);
    }

    private static AbstractConfigListEntry booleanEntry(ConfigEntryBuilder entries, String key, boolean current,
            boolean defaultValue, Consumer<Boolean> saveConsumer) {
        return entries.startBooleanToggle(field(key), current)
                .setDefaultValue(defaultValue)
                .setTooltipSupplier(tooltipSupplier(key))
                .setSaveConsumer(saveConsumer)
                .build();
    }

    private static Supplier<Optional<Component[]>> tooltipSupplier(String key) {
        return () -> Optional.of(wrapped(tooltip(key)).toArray(new Component[0]));
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
}
