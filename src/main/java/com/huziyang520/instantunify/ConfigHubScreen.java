package com.huziyang520.instantunify;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

/**
 * Entry point of the in-game configuration: either the regular Cloth Config screen or the list editor.
 * <p>
 * Cloth Config's screen does not allow adding buttons from the outside, so both screens are reached from here
 * instead.
 */
public class ConfigHubScreen extends Screen {

    private final Screen parent;

    public ConfigHubScreen(Screen parent) {
        super(Component.translatable("instantunify.config.title"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        int centerX = this.width / 2;
        addRenderableWidget(Button
                .builder(Component.translatable("instantunify.config.openSettings"),
                        button -> this.minecraft.setScreen(ClothConfigIntegration.createScreen(this.minecraft, this)))
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
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        renderBackground(graphics);
        graphics.drawCenteredString(this.font, this.title, this.width / 2, this.height / 2 - 50, 0xFFFFFF);
        super.render(graphics, mouseX, mouseY, partialTick);
    }

    @Override
    public void onClose() {
        if (this.minecraft != null) {
            this.minecraft.setScreen(this.parent);
        }
    }
}
