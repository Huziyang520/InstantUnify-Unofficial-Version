package com.huziyang520.instantunify;

import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.neoforge.client.gui.ConfigurationScreen;
import net.neoforged.neoforge.client.gui.ConfigurationScreen.ConfigurationSectionScreen;

/**
 * The settings screen: the configuration UI the loader itself ships, with the ordered lists left out.
 * <p>
 * The lists are edited by {@link ListEditorScreen}, so everything shown here is a switch or a number and no list
 * can be maintained in two places. The loader offers no way to leave a single value out of its screen, but its
 * section screen accepts a filter callback for exactly that.
 * <p>
 * The loader writes the file when its screen closes, and that write puts every list back on one line, so the file
 * is reflowed once afterwards (see {@link ConfigFileFormatter}).
 */
public final class SettingsScreen {

    /** Returning null leaves the element out. The loader calls this for values only, which is all we need. */
    private static final ConfigurationSectionScreen.Filter HIDE_LISTS =
            (context, key, element) -> InstantUnifyConfig.LIST_KEYS.contains(key) ? null : element;

    private SettingsScreen() {
    }

    public static Screen create(ModContainer modContainer, Screen parent) {
        return new ConfigurationScreen(modContainer, parent, SettingsSection::new);
    }

    /** The section screen, with the lists filtered out and the file reflowed after the loader saved it. */
    private static final class SettingsSection extends ConfigurationSectionScreen {

        private SettingsSection(Screen parent, ModConfig.Type type, ModConfig modConfig, Component title) {
            // The title handed in here is the config file's own name; the screen is called "Settings" everywhere
            // else, and the loader puts its parent's title in front of it as a breadcrumb.
            super(parent, type, modConfig, Component.translatable("instantunify.config.openSettings"), HIDE_LISTS);
        }

        @Override
        public void onClose() {
            super.onClose();
            ConfigFileFormatter.format();
        }
    }
}
