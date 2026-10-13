package com.huziyang520.instantunify;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Supplier;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

/**
 * Editor for the ordered configuration lists: add an entry anywhere, move it up or down, delete it.
 * <p>
 * Cloth Config's list widget can only append, so this screen is built from plain vanilla widgets where the row
 * order is fully under our control. It edits a working copy and writes it back through the usual config setters,
 * which also persists the file and rebuilds the derived state.
 */
public class ListEditorScreen extends Screen {

    private static final int ROW_HEIGHT = 24;
    private static final int TAB_COLUMNS = 4;
    private static final int PAGE_BUTTON_SIZE = 20;
    private static final int PAGE_LEFT_X = 10;
    private static final int PAGE_GAP = 64;
    private static final int PAGE_RIGHT_X = PAGE_LEFT_X + PAGE_BUTTON_SIZE + PAGE_GAP;

    private final Screen parent;
    private final List<Editor> editors = new ArrayList<>();
    private final List<AbstractWidget> rowWidgets = new ArrayList<>();
    private final List<Button> tabButtons = new ArrayList<>();

    private List<String> working = new ArrayList<>();
    private int selected;
    private int page;

    public ListEditorScreen(Screen parent) {
        super(Component.translatable("instantunify.editor.title"));
        this.parent = parent;
    }

    /** One editable list: the translation key of its label plus accessors working with one entry per line. */
    private static final class Editor {
        private final String key;
        private final Supplier<List<String>> getter;
        private final Consumer<List<String>> setter;

        private Editor(String key, Supplier<List<String>> getter, Consumer<List<String>> setter) {
            this.key = key;
            this.getter = getter;
            this.setter = setter;
        }
    }

    private void collectEditors() {
        if (!editors.isEmpty()) {
            return;
        }
        editors.add(new Editor("blacklist", () -> copy(InstantUnifyConfig.blacklist()), InstantUnifyConfig::setBlacklist));
        editors.add(new Editor("whitelist", () -> copy(InstantUnifyConfig.whitelist()), InstantUnifyConfig::setWhitelist));
        editors.add(new Editor("preferredMods", () -> copy(InstantUnifyConfig.preferredMods()), InstantUnifyConfig::setPreferredMods));
        editors.add(new Editor("blacklistMods", () -> copy(InstantUnifyConfig.blacklistMods()), InstantUnifyConfig::setBlacklistMods));
        editors.add(new Editor("blacklistItems", () -> copy(InstantUnifyConfig.blacklistItems()), InstantUnifyConfig::setBlacklistItems));
        editors.add(new Editor("alternatives", () -> toLines(InstantUnifyConfig.alternatives()),
                lines -> InstantUnifyConfig.setAlternatives(fromLines(lines))));
        editors.add(new Editor("namespaceAliases", () -> toLines(InstantUnifyConfig.namespaceAliases()),
                lines -> InstantUnifyConfig.setNamespaceAliases(fromLines(lines))));
    }

    @Override
    protected void init() {
        collectEditors();
        if (working.isEmpty() && page == 0) {
            working = new ArrayList<>(editors.get(selected).getter.get());
        }

        tabButtons.clear();
        int tabWidth = (this.width - 20) / TAB_COLUMNS;
        for (int i = 0; i < editors.size(); i++) {
            int column = i % TAB_COLUMNS;
            int row = i / TAB_COLUMNS;
            final int index = i;
            Button tab = Button
                    .builder(Component.translatable("instantunify.config." + editors.get(i).key), button -> select(index))
                    .bounds(10 + column * tabWidth, 22 + row * 22, tabWidth - 2, 20)
                    .build();
            tabButtons.add(tab);
            addRenderableWidget(tab);
        }
        updateTabs();

        int bottom = this.height - 28;
        addRenderableWidget(Button.builder(Component.literal("<"), button -> changePage(-1))
                .bounds(10, bottom, 20, 20).build());
        addRenderableWidget(Button.builder(Component.literal(">"), button -> changePage(1))
                .bounds(PAGE_RIGHT_X, bottom, 20, 20).build());
        addRenderableWidget(Button.builder(Component.translatable("instantunify.editor.done"), button -> {
            applyWorking();
            onClose();
        }).bounds(this.width / 2 - 105, bottom, 100, 20).build());
        addRenderableWidget(Button.builder(Component.translatable("instantunify.editor.cancel"), button -> onClose())
                .bounds(this.width / 2 + 5, bottom, 100, 20).build());

        rebuildRows();
    }

    private int rowsTop() {
        int rows = (editors.size() + TAB_COLUMNS - 1) / TAB_COLUMNS;
        return 22 + rows * 22 + 6;
    }

    private int rowsPerPage() {
        return Math.max(1, (this.height - 28 - 6 - rowsTop()) / ROW_HEIGHT);
    }

    private void rebuildRows() {
        for (AbstractWidget widget : rowWidgets) {
            removeWidget(widget);
        }
        rowWidgets.clear();

        int perPage = rowsPerPage();
        int fieldWidth = Math.max(60, this.width - 40 - 4 * 22);
        int y = rowsTop();
        for (int slot = 0; slot < perPage; slot++) {
            int index = page * perPage + slot;
            if (index >= working.size()) {
                break;
            }
            final int row = index;

            EditBox box = new EditBox(this.font, 20, y, fieldWidth, 18, Component.empty());
            box.setMaxLength(Integer.MAX_VALUE);
            box.setValue(working.get(row));
            box.setResponder(text -> {
                if (row < working.size()) {
                    working.set(row, text);
                }
            });
            register(box);

            int buttonX = 20 + fieldWidth + 2;
            register(Button.builder(Component.literal("+"), button -> insertAfter(row))
                    .bounds(buttonX, y, 20, 18).build());
            register(Button.builder(Component.literal("^"), button -> move(row, -1))
                    .bounds(buttonX + 20, y, 20, 18).build());
            register(Button.builder(Component.literal("v"), button -> move(row, 1))
                    .bounds(buttonX + 40, y, 20, 18).build());
            register(Button.builder(Component.literal("x"), button -> removeAt(row))
                    .bounds(buttonX + 60, y, 20, 18).build());

            y += ROW_HEIGHT;
        }
    }

    private void register(AbstractWidget widget) {
        addRenderableWidget(widget);
        rowWidgets.add(widget);
    }

    private void select(int index) {
        if (index == selected) {
            return;
        }
        applyWorking();
        selected = index;
        page = 0;
        working = new ArrayList<>(editors.get(selected).getter.get());
        updateTabs();
        rebuildRows();
    }

    /** The list that is being edited is shown as a disabled button, like the active tab of a notebook. */
    private void updateTabs() {
        for (int i = 0; i < tabButtons.size(); i++) {
            tabButtons.get(i).active = i != selected;
        }
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
        rebuildRows();
    }

    private void removeAt(int index) {
        if (index >= 0 && index < working.size()) {
            working.remove(index);
            rebuildRows();
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
        rebuildRows();
    }

    private void changePage(int delta) {
        int pages = pageCount();
        page = Math.max(0, Math.min(pages - 1, page + delta));
        rebuildRows();
    }

    private int pageCount() {
        return Math.max(1, (working.size() + rowsPerPage() - 1) / rowsPerPage());
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        // The background and its blur pass belong to super (see ConfigHubScreen); drawing them here as well
        // blurred everything drawn in between, this label included.
        super.render(graphics, mouseX, mouseY, partialTick);
        graphics.drawCenteredString(this.font, this.title, this.width / 2, 6, 0xFFFFFF);
        graphics.drawCenteredString(this.font,
                Component.translatable("instantunify.config." + editors.get(selected).key),
                this.width / 2, this.height - 40, 0xA0A0A0);
        if (working.isEmpty()) {
            graphics.drawCenteredString(this.font, Component.literal("-"), this.width / 2, rowsTop() + 4, 0x808080);
        }
        // Page indicator between the two paging buttons, e.g. "2/3".
        int pageCenterX = PAGE_LEFT_X + PAGE_BUTTON_SIZE + PAGE_GAP / 2;
        graphics.drawCenteredString(this.font, Component.literal((page + 1) + "/" + pageCount()),
                pageCenterX, this.height - 22, 0xFFFFFF);
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
