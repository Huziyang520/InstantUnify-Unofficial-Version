package com.huziyang520.instantunify;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;

import me.shedaniel.clothconfig2.api.ConfigScreen;
import me.shedaniel.clothconfig2.api.Tooltip;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.util.FormattedCharSequence;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.client.event.ScreenEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.eventbus.api.SubscribeEvent;

/**
 * Draws the hover tooltips of our own configuration screen.
 * <p>
 * Cloth Config hands its tooltips to the vanilla tooltip renderer, whose text is submitted through the deferred
 * render buffer. In some client setups that buffer comes out empty, so the tooltip is drawn with its frame but
 * without any text. Instead of depending on that path, the tooltips Cloth Config collected are taken over here
 * and painted directly, which uses the very same draw call the (visible) entry labels use.
 * <p>
 * Only the screen we created is affected, and everything fails back to the default behaviour.
 */
@OnlyIn(Dist.CLIENT)
public final class ConfigTooltipRenderer {

    private static final int WRAP_LIMIT = 200;
    private static final int LINE_HEIGHT = 10;
    private static final int BACKGROUND = 0xF0100010;
    private static final int BORDER = 0x505000FF;
    private static final int TEXT = 0xFFFFFF;

    private static volatile Screen owner;
    private static volatile Field tooltips;
    private static volatile boolean unavailable;

    private ConfigTooltipRenderer() {
    }

    public static void register() {
        MinecraftForge.EVENT_BUS.register(ConfigTooltipRenderer.class);
    }

    /** Marks the screen whose tooltips are drawn here. */
    static void own(Screen screen) {
        owner = screen;
    }

    @SubscribeEvent
    public static void onScreenRender(ScreenEvent.Render.Post event) {
        Screen screen = event.getScreen();
        if (screen != owner || !(screen instanceof ConfigScreen)) {
            return;
        }
        List<Tooltip> collected = takeTooltips(screen);
        if (collected.isEmpty()) {
            return;
        }
        GuiGraphics graphics = event.getGuiGraphics();
        Font font = Minecraft.getInstance().font;
        for (Tooltip tooltip : collected) {
            draw(graphics, font, tooltip);
        }
    }

    /**
     * Removes the collected tooltips from the screen and returns them. Taking them away is what stops Cloth
     * Config from drawing its frameless-but-textless tooltip on top of ours.
     */
    @SuppressWarnings("unchecked")
    private static List<Tooltip> takeTooltips(Screen screen) {
        Field field = tooltips;
        if (field == null) {
            if (unavailable) {
                return List.of();
            }
            field = findTooltipsField(screen.getClass());
            if (field == null) {
                unavailable = true;
                return List.of();
            }
            tooltips = field;
        }
        try {
            Object value = field.get(screen);
            if (!(value instanceof List<?> list) || list.isEmpty()) {
                return List.of();
            }
            List<Tooltip> copy = new ArrayList<>();
            for (Object element : list) {
                if (element instanceof Tooltip tooltip) {
                    copy.add(tooltip);
                }
            }
            ((List<Object>) value).clear();
            return copy;
        } catch (ReflectiveOperationException | RuntimeException e) {
            unavailable = true;
            return List.of();
        }
    }

    private static Field findTooltipsField(Class<?> type) {
        for (Class<?> current = type; current != null; current = current.getSuperclass()) {
            try {
                Field field = current.getDeclaredField("tooltips");
                field.setAccessible(true);
                return field;
            } catch (NoSuchFieldException ignored) {
                // Keep walking up the hierarchy.
            } catch (RuntimeException e) {
                return null;
            }
        }
        return null;
    }

    private static void draw(GuiGraphics graphics, Font font, Tooltip tooltip) {
        List<FormattedCharSequence> lines = tooltip.getText();
        if (lines == null || lines.isEmpty()) {
            return;
        }
        int width = 0;
        for (FormattedCharSequence line : lines) {
            width = Math.max(width, Math.min(WRAP_LIMIT * 2, font.width(line)));
        }
        int height = lines.size() * LINE_HEIGHT;
        int x = tooltip.getX() + 12;
        int y = tooltip.getY() - 12;

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

        graphics.fill(left, top, right, bottom, BACKGROUND);
        graphics.fill(left, top, right, top + 1, BORDER);
        graphics.fill(left, bottom - 1, right, bottom, BORDER);
        graphics.fill(left, top + 1, left + 1, bottom - 1, BORDER);
        graphics.fill(right - 1, top + 1, right, bottom - 1, BORDER);
        for (int i = 0; i < lines.size(); i++) {
            graphics.drawString(font, lines.get(i), x, y + i * LINE_HEIGHT, TEXT);
        }
    }
}
