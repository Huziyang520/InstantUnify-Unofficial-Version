package com.huziyang520.instantunify;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;

import com.mojang.blaze3d.systems.RenderSystem;

import me.shedaniel.clothconfig2.api.AbstractConfigEntry;
import me.shedaniel.clothconfig2.gui.ClothConfigScreen;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractSelectionList;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.client.event.ScreenEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.util.ObfuscationReflectionHelper;

/**
 * Draws the hover tooltips of our own configuration screen.
 * <p>
 * Cloth Config's own tooltips are unusable here, and the reason is in its scissors implementation: the scissor
 * box is computed with {@code windowHeight - height - y} multiplied by the gui scale again, although the window
 * height is already in pixels. The clip region therefore lands somewhere else than the code intends, which cuts
 * the box off or hides it completely, and which is why the result changes as soon as the category tree is
 * collapsed and the layout shifts.
 * <p>
 * So this screen does not ask Cloth Config for tooltips at all. Every entry is registered here with its text,
 * the entry under the cursor is looked up through the list widget, and the box is painted after everything else,
 * with the scissor switched off. Long text is wrapped by the screen, so the box stays readable.
 */
@OnlyIn(Dist.CLIENT)
public final class ConfigTooltipRenderer {

    private static final int LINE_HEIGHT = 10;
    private static final int BACKGROUND = 0xF0100010;
    private static final int BORDER = 0x505000FF;
    private static final int TEXT = 0xFFFFFF;

    /** The screen the tooltips below belong to, and its tooltip text per entry. */
    private static volatile Screen owner;
    private static final Map<AbstractConfigEntry<?>, List<Component>> TOOLTIPS = new IdentityHashMap<>();

    /** The list widget's own look-up of the entry under the mouse, which is protected in the game's list class. */
    private static final Method entryAtPosition = findEntryAtPosition();

    private ConfigTooltipRenderer() {
    }

    public static void register() {
        MinecraftForge.EVENT_BUS.register(ConfigTooltipRenderer.class);
    }

    /** Called once per screen build: the tooltips of the previous screen are dropped with it. */
    static void own(Screen screen, Map<AbstractConfigEntry<?>, List<Component>> tooltips) {
        synchronized (TOOLTIPS) {
            TOOLTIPS.clear();
            TOOLTIPS.putAll(tooltips);
        }
        owner = screen;
    }

    @SubscribeEvent
    public static void onScreenRender(ScreenEvent.Render.Post event) {
        Screen screen = event.getScreen();
        if (screen != owner || !(screen instanceof ClothConfigScreen clothScreen)) {
            return;
        }
        if (entryAtPosition == null) {
            return;
        }
        Object hovered;
        try {
            hovered = entryAtPosition.invoke(clothScreen.listWidget, (double) event.getMouseX(),
                    (double) event.getMouseY());
        } catch (ReflectiveOperationException | RuntimeException e) {
            return;
        }
        if (!(hovered instanceof AbstractConfigEntry<?> entry)) {
            return;
        }
        List<Component> lines;
        synchronized (TOOLTIPS) {
            lines = TOOLTIPS.get(entry);
        }
        if (lines == null || lines.isEmpty()) {
            return;
        }
        try {
            draw(event.getGuiGraphics(), Minecraft.getInstance().font, event.getMouseX(), event.getMouseY(), lines);
        } catch (RuntimeException e) {
            if (!drawingFailed) {
                drawingFailed = true;
                InstantUnify.LOG.warn("Unable to draw a configuration tooltip", e);
            }
        }
    }

    private static void draw(GuiGraphics graphics, Font font, int mouseX, int mouseY, List<Component> lines) {
        List<FormattedCharSequence> sequences = new ArrayList<>(lines.size());
        for (Component line : lines) {
            sequences.add(line.getVisualOrderText());
        }
        int width = 0;
        for (FormattedCharSequence line : sequences) {
            width = Math.max(width, font.width(line));
        }
        int height = sequences.size() * LINE_HEIGHT;
        int x = mouseX + 12;
        int y = mouseY - 12;

        int left = x - 4;
        int top = y - 4;
        int right = x + width + 4;
        int bottom = y + height + 4;
        int limitX = graphics.guiWidth() - 2;
        int limitY = graphics.guiHeight() - 2;
        if (right > limitX) {
            int shift = right - limitX;
            left -= shift;
            right -= shift;
            x -= shift;
        }
        if (left < 2) {
            int shift = 2 - left;
            left += shift;
            right += shift;
            x += shift;
        }
        if (top < 2) {
            int shift = 2 - top;
            top += shift;
            bottom += shift;
            y += shift;
        }
        if (bottom > limitY) {
            int shift = bottom - limitY;
            top -= shift;
            bottom -= shift;
            y -= shift;
        }

        // The scissor is switched off instead of being asked to reset: Cloth Config's own reset path applies the
        // wrong box again (see the class comment).
        RenderSystem.disableScissor();
        graphics.fill(left, top, right, bottom, BACKGROUND);
        graphics.fill(left, top, right, top + 1, BORDER);
        graphics.fill(left, bottom - 1, right, bottom, BORDER);
        graphics.fill(left, top + 1, left + 1, bottom - 1, BORDER);
        graphics.fill(right - 1, top + 1, right, bottom - 1, BORDER);
        // The interface is drawn with the depth test off and its batches are flushed by render type rather than in
        // the order shapes were queued, so the frame is forced out before the text is queued.
        graphics.flush();
        for (int i = 0; i < sequences.size(); i++) {
            graphics.drawString(font, sequences.get(i), x, y + i * LINE_HEIGHT, TEXT);
        }
    }

    private static volatile boolean drawingFailed;

    private static Method findEntryAtPosition() {
        try {
            Method method = ObfuscationReflectionHelper.findMethod(AbstractSelectionList.class, "getEntryAtPosition",
                    double.class, double.class);
            method.setAccessible(true);
            return method;
        } catch (RuntimeException e) {
            InstantUnify.LOG.warn("Unable to look up the entry under the cursor, the configuration tooltips stay "
                    + "hidden", e);
            return null;
        }
    }
}
