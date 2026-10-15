package com.huziyang520.instantunify;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.neoforged.fml.ModContainer;

/**
 * Entry point of the in-game configuration: the loader's own settings screen or the list editor.
 * <p>
 * Both screens belong to the mod rather than to a screen library, and the loader's screen cannot take extra
 * buttons from the outside, so they are reached from here instead.
 */
public class ConfigHubScreen extends Screen {

    private final Screen parent;
    private final ModContainer modContainer;

    public ConfigHubScreen(Screen parent, ModContainer modContainer) {
        super(Component.translatable("instantunify.config.title"));
        this.parent = parent;
        this.modContainer = modContainer;
    }

    @Override
    protected void init() {
        int centerX = this.width / 2;
        addRenderableWidget(Button
                .builder(Component.translatable("instantunify.config.openSettings"),
                        button -> this.minecraft.setScreen(SettingsScreen.create(this.modContainer, this)))
                .bounds(centerX - 100, this.height / 2 - 26, 200, 20)
                .build());
        addRenderableWidget(Button
                .builder(Component.translatable("instantunify.config.openListEditor"),
                        button -> this.minecraft.setScreen(new ListEditorScreen(this)))
                .bounds(centerX - 100, this.height / 2 - 2, 200, 20)
                .build());
        addRenderableWidget(Button
                .builder(Component.translatable("instantunify.editor.cancel"), button -> onClose())
                .bounds(centerX - 100, this.height / 2 + 22, 200, 20)
                .build());
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        // The framework already extracted the background (and its blur) before calling this method, and this
        // version allows that blur exactly once per frame: asking for it a second time crashed the client
        // with "Can only blur once per frame". The text is drawn here, on top of that one background.
        graphics.centeredText(this.font, this.title, this.width / 2, this.height / 2 - 50, 0xFFFFFF);
        super.extractRenderState(graphics, mouseX, mouseY, partialTick);
    }

    @Override
    public void onClose() {
        if (this.minecraft != null) {
            this.minecraft.setScreen(this.parent);
        }
    }
}
