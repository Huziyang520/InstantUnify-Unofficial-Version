package com.huziyang520.instantunify;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Supplier;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.FormattedText;
import net.minecraft.network.chat.Style;

/**
 * Editor for the ordered configuration lists: add an entry anywhere, move it up or down, delete it.
 * <p>
 * The lists are the only thing edited here (the settings screen shows the switches and the numbers only), and the
 * rows are built from plain vanilla widgets because the order of the entries has to be fully under our control.
 * <p>
 * The lists are grouped into categories. What the selected list is for is written along the bottom, which is the
 * one thing the settings screen used to explain as a tooltip.
 * <p>
 * All widget changes are queued to the next tick: rebuilding the rows from inside a button callback would remove
 * and re-add the very widget that is being dispatched.
 */
public class ListEditorScreen extends Screen {

    private static final int TAB_HEIGHT = 20;
    private static final int TAB_TOP = 22;
    private static final int TAB_GAP = 4;
    private static final int TAB_MARGIN = 10;
    /** Categories on the first row, the lists of the selected category on the second one. */
    private static final int TAB_ROWS = 2;
    private static final int ROW_HEIGHT = 24;
    private static final int FIELD_HEIGHT = 18;
    private static final int ROW_BUTTON_SIZE = 20;
    private static final int ROW_BUTTON_GAP = 2;
    private static final int CONTENT_GAP = 6;
    private static final int BOTTOM_BUTTONS_HEIGHT = 20;
    private static final int PAGE_BUTTON_SIZE = 20;
    private static final int PAGE_LEFT_X = 10;
    private static final int PAGE_GAP = 64;
    private static final int PAGE_RIGHT_X = PAGE_LEFT_X + PAGE_BUTTON_SIZE + PAGE_GAP;

    /** The categories, in the order their tabs are shown in. */
    private static final String[] CATEGORY_KEYS = {
            "instantunify.editor.category.tagLists",
            "instantunify.editor.category.modLists",
            "instantunify.editor.category.names"
    };

    private final Screen parent;
    private final List<Editor> editors = new ArrayList<>();
    private final List<AbstractWidget> rowWidgets = new ArrayList<>();
    private final List<AbstractWidget> tabWidgets = new ArrayList<>();

    private List<String> working = new ArrayList<>();
    private int category;
    private int selected;
    private int page;
    private boolean initialised;
    private boolean rebuildQueued;

    public ListEditorScreen(Screen parent) {
        super(Component.translatable("instantunify.editor.title"));
        this.parent = parent;
    }

    /** One editable list: the category it belongs to plus accessors working with one entry per line. */
    private static final class Editor {
        private final int category;
        private final String key;
        private final Supplier<List<String>> getter;
        private final Consumer<List<String>> setter;

        private Editor(int category, String key, Supplier<List<String>> getter, Consumer<List<String>> setter) {
            this.category = category;
            this.key = key;
            this.getter = getter;
            this.setter = setter;
        }
    }

    private void collectEditors() {
        if (!editors.isEmpty()) {
            return;
        }
        editors.add(new Editor(0, "blacklist", () -> copy(InstantUnifyConfig.blacklist()), InstantUnifyConfig::setBlacklist));
        editors.add(new Editor(0, "whitelist", () -> copy(InstantUnifyConfig.whitelist()), InstantUnifyConfig::setWhitelist));
        editors.add(new Editor(1, "preferredMods", () -> copy(InstantUnifyConfig.preferredMods()), InstantUnifyConfig::setPreferredMods));
        editors.add(new Editor(1, "blacklistMods", () -> copy(InstantUnifyConfig.blacklistMods()), InstantUnifyConfig::setBlacklistMods));
        editors.add(new Editor(1, "exemptMods", () -> copy(InstantUnifyConfig.exemptMods()), InstantUnifyConfig::setExemptMods));
        editors.add(new Editor(2, "blacklistItems", () -> copy(InstantUnifyConfig.blacklistItems()), InstantUnifyConfig::setBlacklistItems));
        editors.add(new Editor(2, "alternatives", () -> toLines(InstantUnifyConfig.alternatives()),
                lines -> InstantUnifyConfig.setAlternatives(fromLines(lines))));
        editors.add(new Editor(2, "namespaceAliases", () -> toLines(InstantUnifyConfig.namespaceAliases()),
                lines -> InstantUnifyConfig.setNamespaceAliases(fromLines(lines))));
    }

    @Override
    protected void init() {
        collectEditors();
        if (!initialised) {
            initialised = true;
            category = 0;
            selected = firstOfCategory(0);
            working = new ArrayList<>(editors.get(selected).getter.get());
        }

        // Resizing clears the widgets but keeps this screen, so our own bookkeeping is reset here as well.
        tabWidgets.clear();
        rowWidgets.clear();

        int bottom = bottomY();
        addRenderableWidget(Button.builder(Component.literal("<"), button -> changePage(-1))
                .bounds(PAGE_LEFT_X, bottom, PAGE_BUTTON_SIZE, BOTTOM_BUTTONS_HEIGHT).build());
        addRenderableWidget(Button.builder(Component.literal(">"), button -> changePage(1))
                .bounds(PAGE_RIGHT_X, bottom, PAGE_BUTTON_SIZE, BOTTOM_BUTTONS_HEIGHT).build());
        addRenderableWidget(Button.builder(Component.translatable("instantunify.editor.done"), button -> {
            applyWorking();
            onClose();
        }).bounds(this.width / 2 - 105, bottom, 100, BOTTOM_BUTTONS_HEIGHT).build());
        addRenderableWidget(Button.builder(Component.translatable("instantunify.editor.cancel"), button -> onClose())
                .bounds(this.width / 2 + 5, bottom, 100, BOTTOM_BUTTONS_HEIGHT).build());

        rebuildTabs();
        rebuildRows();
    }

    // --- Layout: one grid shared by the widgets and the labels -------------------------------------------

    private int tabWidth(int columns) {
        return Math.max(40, (this.width - 2 * TAB_MARGIN - (columns - 1) * TAB_GAP) / columns);
    }

    private int tabX(int column, int columns) {
        return TAB_MARGIN + column * (tabWidth(columns) + TAB_GAP);
    }

    private int tabY(int row) {
        return TAB_TOP + row * (TAB_HEIGHT + TAB_GAP);
    }

    private int rowsTop() {
        return tabY(TAB_ROWS - 1) + TAB_HEIGHT + CONTENT_GAP;
    }

    private int bottomY() {
        return this.height - BOTTOM_BUTTONS_HEIGHT - 8;
    }

    private int descriptionWidth() {
        return Math.max(120, this.width - 2 * TAB_MARGIN - 20);
    }

    /** What the selected list is for, wrapped to the width it is drawn at. */
    private List<FormattedText> descriptionLines() {
        Component text = Component.translatable("instantunify.config." + editors.get(selected).key + ".tooltip");
        List<FormattedText> lines = this.font.getSplitter().splitLines(text, descriptionWidth(), Style.EMPTY);
        return lines.isEmpty() ? List.of(text) : lines;
    }

    private int descriptionHeight() {
        return descriptionLines().size() * (this.font.lineHeight + 1);
    }

    private int descriptionTop() {
        return bottomY() - CONTENT_GAP - descriptionHeight();
    }

    private int rowsPerPage() {
        return Math.max(1, (descriptionTop() - CONTENT_GAP - rowsTop()) / ROW_HEIGHT);
    }

    // --- Widgets -----------------------------------------------------------------------------------------

    private void addTab(AbstractWidget widget) {
        addRenderableWidget(widget);
        tabWidgets.add(widget);
    }

    private void register(AbstractWidget widget) {
        addRenderableWidget(widget);
        rowWidgets.add(widget);
    }

    private void rebuildTabs() {
        for (AbstractWidget widget : tabWidgets) {
            removeWidget(widget);
        }
        tabWidgets.clear();

        for (int i = 0; i < CATEGORY_KEYS.length; i++) {
            final int index = i;
            Button tab = Button
                    .builder(Component.translatable(CATEGORY_KEYS[i]), button -> selectCategory(index))
                    .bounds(tabX(i, CATEGORY_KEYS.length), tabY(0), tabWidth(CATEGORY_KEYS.length), TAB_HEIGHT)
                    .build();
            tab.active = i != category;
            addTab(tab);
        }

        List<Integer> lists = editorsOfCategory(category);
        for (int column = 0; column < lists.size(); column++) {
            final int index = lists.get(column);
            Button tab = Button
                    .builder(Component.translatable("instantunify.config." + editors.get(index).key),
                            button -> selectList(index))
                    .bounds(tabX(column, lists.size()), tabY(1), tabWidth(lists.size()), TAB_HEIGHT)
                    .build();
            tab.active = index != selected;
            addTab(tab);
        }
    }

    private void rebuildRows() {
        for (AbstractWidget widget : rowWidgets) {
            removeWidget(widget);
        }
        rowWidgets.clear();

        int perPage = rowsPerPage();
        int fieldWidth = Math.max(60,
                this.width - 2 * TAB_MARGIN - 20 - 4 * (ROW_BUTTON_SIZE + ROW_BUTTON_GAP));
        int y = rowsTop();
        for (int slot = 0; slot < perPage; slot++) {
            int index = page * perPage + slot;
            if (index >= working.size()) {
                break;
            }
            final int row = index;

            EditBox box = new EditBox(this.font, 20, y, fieldWidth, FIELD_HEIGHT, Component.empty());
            box.setMaxLength(Integer.MAX_VALUE);
            box.setValue(working.get(row));
            box.setResponder(text -> {
                if (row < working.size()) {
                    working.set(row, text);
                }
            });
            register(box);

            int buttonX = 20 + fieldWidth + ROW_BUTTON_GAP;
            register(Button.builder(Component.literal("+"), button -> insertAfter(row))
                    .bounds(buttonX, y, ROW_BUTTON_SIZE, FIELD_HEIGHT).build());
            register(Button.builder(Component.literal("^"), button -> move(row, -1))
                    .bounds(buttonX + ROW_BUTTON_SIZE, y, ROW_BUTTON_SIZE, FIELD_HEIGHT).build());
            register(Button.builder(Component.literal("v"), button -> move(row, 1))
                    .bounds(buttonX + 2 * ROW_BUTTON_SIZE, y, ROW_BUTTON_SIZE, FIELD_HEIGHT).build());
            register(Button.builder(Component.literal("x"), button -> removeAt(row))
                    .bounds(buttonX + 3 * ROW_BUTTON_SIZE, y, ROW_BUTTON_SIZE, FIELD_HEIGHT).build());

            y += ROW_HEIGHT;
        }
    }

    /**
     * Rebuilds the widgets on the next tick. Removing the widget that is currently being pressed, from inside its
     * own callback, is what this avoids.
     */
    private void requestRebuild() {
        Minecraft minecraft = this.minecraft;
        if (minecraft == null) {
            rebuildTabs();
            rebuildRows();
            return;
        }
        if (rebuildQueued) {
            return;
        }
        rebuildQueued = true;
        minecraft.execute(() -> {
            rebuildQueued = false;
            if (minecraft.screen == this) {
                rebuildTabs();
                rebuildRows();
            }
        });
    }

    // --- State -------------------------------------------------------------------------------------------

    private List<Integer> editorsOfCategory(int category) {
        List<Integer> lists = new ArrayList<>();
        for (int i = 0; i < editors.size(); i++) {
            if (editors.get(i).category == category) {
                lists.add(i);
            }
        }
        return lists;
    }

    private int firstOfCategory(int category) {
        List<Integer> lists = editorsOfCategory(category);
        return lists.isEmpty() ? 0 : lists.get(0);
    }

    private void selectCategory(int index) {
        if (index == category) {
            return;
        }
        applyWorking();
        category = index;
        selected = firstOfCategory(index);
        page = 0;
        working = new ArrayList<>(editors.get(selected).getter.get());
        requestRebuild();
    }

    private void selectList(int index) {
        if (index == selected) {
            return;
        }
        applyWorking();
        selected = index;
        page = 0;
        working = new ArrayList<>(editors.get(selected).getter.get());
        requestRebuild();
    }

    private void applyWorking() {
        if (editors.isEmpty()) {
            return;
        }
        editors.get(selected).setter.accept(new ArrayList<>(working));
        InstantUnifyConfig.save();
    }

    private void insertAfter(int index) {
        working.add(Math.min(index + 1, working.size()), "");
        requestRebuild();
    }

    private void removeAt(int index) {
        if (index >= 0 && index < working.size()) {
            working.remove(index);
            requestRebuild();
        }
    }

    private void move(int index, int delta) {
        int target = index + delta;
        if (index < 0 || index >= working.size() || target < 0 || target >= working.size()) {
            return;
        }
        String value = working.get(index);
        working.set(index, working.get(target));
        working.set(target, value);
        requestRebuild();
    }

    private void changePage(int delta) {
        int pages = pageCount();
        page = Math.max(0, Math.min(pages - 1, page + delta));
        requestRebuild();
    }

    private int pageCount() {
        return Math.max(1, (working.size() + rowsPerPage() - 1) / rowsPerPage());
    }

    // --- Rendering ---------------------------------------------------------------------------------------

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        // The background and its blur pass belong to super (see ConfigHubScreen); drawing them here as well
        // blurred everything drawn in between, these labels included.
        super.render(graphics, mouseX, mouseY, partialTick);
        graphics.drawCenteredString(this.font, this.title, this.width / 2, 6, 0xFFFFFF);

        int y = descriptionTop();
        for (FormattedText line : descriptionLines()) {
            graphics.drawCenteredString(this.font, Component.literal(line.getString()), this.width / 2, y, 0xA0A0A0);
            y += this.font.lineHeight + 1;
        }

        if (working.isEmpty()) {
            graphics.drawCenteredString(this.font, Component.literal("-"), this.width / 2, rowsTop() + 4, 0x808080);
        }
        // Page indicator between the two paging buttons, e.g. "2/3".
        int pageCenterX = PAGE_LEFT_X + PAGE_BUTTON_SIZE + PAGE_GAP / 2;
        graphics.drawCenteredString(this.font, Component.literal((page + 1) + "/" + pageCount()),
                pageCenterX, bottomY() + 6, 0xFFFFFF);
    }

    @Override
    public void onClose() {
        if (this.minecraft != null) {
            this.minecraft.setScreen(this.parent);
        }
    }

    private static List<String> copy(List<? extends String> source) {
        return new ArrayList<>(source);
    }

    /** One line per group, the names of a group separated by commas. */
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
