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
 * and it is never shaded into the jar. Every entry reads its current value and writes it back immediately, so
 * the screen never shows a stale value and the change is applied on save.
 * <p>
 * Registered through Forge's own {@code ConfigScreenHandler} extension point, which is what puts the button into
 * the mod list.
 */
public final class ClothConfigIntegration {

    private ClothConfigIntegration() {
    }

    public static void register() {
        ModLoadingContext.get().registerExtensionPoint(ConfigScreenHandler.ConfigScreenFactory.class,
                () -> new ConfigScreenHandler.ConfigScreenFactory(ClothConfigIntegration::createScreen));
    }

    private static Screen createScreen(Minecraft minecraft, Screen parent) {
        ConfigBuilder builder = ConfigBuilder.create()
                .setParentScreen(parent)
                .setTitle(Component.translatable("instantunify.config.title"));
        builder.setGlobalized(true);
        builder.setGlobalizedExpanded(true);
        builder.setSavingRunnable(InstantUnifyConfig::save);
        ConfigEntryBuilder entries = builder.entryBuilder();

        ConfigCategory lists = builder.getOrCreateCategory(Component.translatable("instantunify.config.category.lists"));
        lists.addEntry(entries.startStrList(field("blacklist"), new ArrayList<>(InstantUnifyConfig.blacklist()))
                .setDefaultValue(List.of("minecraft:.+", "forge:glass.+"))
                .setTooltip(tooltip("blacklist"))
                .setSaveConsumer(InstantUnifyConfig::setBlacklist)
                .build());
        lists.addEntry(entries.startStrList(field("whitelist"), new ArrayList<>(InstantUnifyConfig.whitelist()))
                .setDefaultValue(List.of("forge:ingots\\/.+", "forge:ores\\/.+"))
                .setTooltip(tooltip("whitelist"))
                .setSaveConsumer(InstantUnifyConfig::setWhitelist)
                .build());
        lists.addEntry(entries.startEnumSelector(field("listMode"), InstantUnifyConfig.ListMode.class,
                        InstantUnifyConfig.listMode())
                .setEnumNameProvider(value -> Component.translatable(
                        "instantunify.config.listMode." + value.name().toLowerCase(Locale.ROOT)))
                .setTooltip(tooltip("listMode"))
                .setSaveConsumer(InstantUnifyConfig::setListMode)
                .build());

        ConfigCategory general = builder.getOrCreateCategory(Component.translatable("instantunify.config.category.general"));
        general.addEntry(entries.startStrList(field("preferredMods"), new ArrayList<>(InstantUnifyConfig.preferredMods()))
                .setDefaultValue(List.of("minecraft", "alltheores"))
                .setTooltip(tooltip("preferredMods"))
                .setSaveConsumer(InstantUnifyConfig::setPreferredMods)
                .build());
        general.addEntry(entries.startStrList(field("blacklistMods"), new ArrayList<>(InstantUnifyConfig.blacklistMods()))
                .setDefaultValue(List.of("chisel", "astralsorcery"))
                .setTooltip(tooltip("blacklistMods"))
                .setSaveConsumer(InstantUnifyConfig::setBlacklistMods)
                .build());
        general.addEntry(entries.startStrList(field("alternatives"), toGroups(InstantUnifyConfig.alternatives()))
                .setDefaultValue(List.of("aluminum,aluminium,bauxite"))
                .setTooltip(tooltip("alternatives"))
                .setSaveConsumer(value -> InstantUnifyConfig.setAlternatives(fromGroups(value)))
                .build());

        ConfigCategory events = builder.getOrCreateCategory(Component.translatable("instantunify.config.category.events"));
        events.addEntry(booleanEntry(entries, "drop", InstantUnifyConfig.drop(), InstantUnifyConfig::setDrop));
        events.addEntry(booleanEntry(entries, "death", InstantUnifyConfig.death(), InstantUnifyConfig::setDeath));
        events.addEntry(booleanEntry(entries, "second", InstantUnifyConfig.second(), InstantUnifyConfig::setSecond));
        events.addEntry(booleanEntry(entries, "change", InstantUnifyConfig.change(), InstantUnifyConfig::setChange));
        events.addEntry(booleanEntry(entries, "gui", InstantUnifyConfig.gui(), InstantUnifyConfig::setGui));
        events.addEntry(booleanEntry(entries, "harvest", InstantUnifyConfig.harvest(), InstantUnifyConfig::setHarvest));

        ConfigCategory performance = builder
                .getOrCreateCategory(Component.translatable("instantunify.config.category.performance"));
        performance.addEntry(booleanEntry(entries, "indexCache", InstantUnifyConfig.indexCache(),
                InstantUnifyConfig::setIndexCache));
        performance.addEntry(entries.startIntField(field("lazyIndexRefreshSeconds"),
                        InstantUnifyConfig.lazyIndexRefreshSeconds())
                .setDefaultValue(0)
                .setMin(0)
                .setMax(86400)
                .setTooltip(tooltip("lazyIndexRefreshSeconds"))
                .setSaveConsumer(InstantUnifyConfig::setLazyIndexRefreshSeconds)
                .build());

        return builder.build();
    }

    private static AbstractConfigListEntry booleanEntry(ConfigEntryBuilder entries, String key, boolean current,
            Consumer<Boolean> saveConsumer) {
        return entries.startBooleanToggle(field(key), current)
                .setDefaultValue(true)
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
    private static List<String> toGroups(List<? extends List<? extends String>> configured) {
        List<String> groups = new ArrayList<>(configured.size());
        for (List<? extends String> group : configured) {
            groups.add(String.join(",", group));
        }
        return groups;
    }

    private static List<List<String>> fromGroups(List<String> lines) {
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
