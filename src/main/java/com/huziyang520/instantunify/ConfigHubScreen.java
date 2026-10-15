package com.huziyang520.instantunify;

import net.minecraft.client.gui.GuiGraphics;
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
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        // Screen#render already draws the background, and in this version that call is what runs the blur
        // pass. Calling renderBackground here as well ran the blur a second time - and the second pass runs
        // over everything drawn in between, which is why the title used to come out smeared while the
        // buttons (drawn after the second blur) stayed sharp. Text goes after super so it lands on top of a
        // single, correctly ordered background.
        super.render(graphics, mouseX, mouseY, partialTick);
        graphics.drawCenteredString(this.font, this.title, this.width / 2, this.height / 2 - 50, 0xFFFFFF);
    }

    @Override
    public void onClose() {
        if (this.minecraft != null) {
            this.minecraft.setScreen(this.parent);
        }
    }
}
