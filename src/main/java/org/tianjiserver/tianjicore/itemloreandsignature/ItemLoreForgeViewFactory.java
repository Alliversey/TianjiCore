package org.tianjiserver.tianjicore.itemloreandsignature;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.tianjiserver.tianjicore.TianjiCore;

import java.util.List;

/**
 * 生成锻造界面中的按钮、占位物品、预览物品与标题。
 */
final class ItemLoreForgeViewFactory {

    private static final String PLACEHOLDER_MARKER_VALUE = "true";
    private static final String TEXT_PLACEHOLDER = "（请输入文本）";
    private static final String NO_TEXT_PLACEHOLDER = "（无需输入）";

    private final MiniMessage mini = MiniMessage.miniMessage();
    private final NamespacedKey placeholderKey;
    private final ItemLoreForgeConfig forgeConfig;
    private final ItemLoreEditor loreEditor;

    ItemLoreForgeViewFactory(TianjiCore plugin, ItemLoreForgeConfig forgeConfig, ItemLoreEditor loreEditor) {
        this.placeholderKey = new NamespacedKey(plugin, "lore_input_placeholder");
        this.forgeConfig = forgeConfig;
        this.loreEditor = loreEditor;
    }

    /**
     * 生成当前模式按钮。
     */
    ItemStack createModeButton(ItemLoreOperation operation) {
        ItemStack item = new ItemStack(operation.buttonMaterial());
        ItemMeta meta = item.getItemMeta();
        if (meta == null) {
            return item;
        }

        meta.displayName(mini.deserialize(operation.buttonName()));
        List<Component> lore = operation.requiresText()
                ? List.of(
                        mini.deserialize("<gray>左键切换下一种模式"),
                        mini.deserialize("<gray>右键切换上一种模式"),
                        mini.deserialize("<gray>文本支持 <white>&a</white>、<white>&l</white>、<white>&#66ccff</white> 格式"),
                        mini.deserialize(operation.buttonHint())
                )
                : List.of(
                        mini.deserialize("<gray>左键切换下一种模式"),
                        mini.deserialize("<gray>右键切换上一种模式"),
                        mini.deserialize(operation.buttonHint())
                );
        meta.lore(lore);
        item.setItemMeta(meta);
        return item;
    }

    /**
     * 生成第一格临时展示物品。
     */
    ItemStack createPlaceholderInputItem(ItemStack source, String placeholderText) {
        ItemStack displayItem = source.clone();
        ItemMeta meta = displayItem.getItemMeta();
        if (meta == null) {
            return displayItem;
        }

        meta.displayName(Component.text(placeholderText));
        meta.getPersistentDataContainer().set(placeholderKey, PersistentDataType.STRING, PLACEHOLDER_MARKER_VALUE);
        displayItem.setItemMeta(meta);
        return displayItem;
    }

    /**
     * 生成文本框占位提示。
     */
    String createPlaceholderText(ItemLoreOperation operation) {
        if (!operation.requiresText()) {
            return NO_TEXT_PLACEHOLDER;
        }
        return TEXT_PLACEHOLDER;
    }

    /**
     * 生成带费用的铁砧标题。
     */
    String createTitle(ItemLoreOperation operation, ItemStack item) {
        int loreEntryCount = loreEditor.hasUsableItem(item) ? loreEditor.visibleLoreEntryCount(item) : 0;
        return operation.successVerb() + "费用" + forgeConfig.formatCost(forgeConfig.resolveCost(loreEntryCount));
    }

    /**
     * 刷新铁砧标题中的费用。
     */
    void updateTitle(ItemLoreForgeSession session) {
        session.view().setTitle(createTitle(session.operation(), session.originalInput()));
    }

    /**
     * 生成编辑预览物品。
     */
    ItemStack createPreviewItem(ItemLoreForgeSession session) {
        String loreLine = resolveRenameText(session).trim();
        if (isUnchangedPlaceholder(session, loreLine)) {
            return null;
        }

        return loreEditor.createPreview(
                session.originalInput(),
                session.operation(),
                loreLine
        );
    }

    /**
     * 读取铁砧输入框文本。
     */
    String resolveRenameText(ItemLoreForgeSession session) {
        String renameText = session.inventory().getRenameText();
        return renameText == null ? "" : renameText;
    }

    /**
     * 判断玩家是否仍保留默认提示文本。
     */
    boolean isUnchangedPlaceholder(ItemLoreForgeSession session, String loreLine) {
        return session.operation().requiresText() && loreLine.equals(session.placeholderText());
    }

    /**
     * 判断物品是否为第一格临时展示物品。
     */
    boolean isPlaceholderItem(ItemStack item) {
        if (!loreEditor.hasUsableItem(item)) {
            return false;
        }

        ItemMeta meta = item.getItemMeta();
        return meta != null
                && meta.getPersistentDataContainer().has(placeholderKey, PersistentDataType.STRING);
    }
}
