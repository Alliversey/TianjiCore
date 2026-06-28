package org.tianjiserver.tianjicore.itemloreandsignature;

import net.kyori.adventure.text.Component;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;

/**
 * 负责读取与修改物品 lore。
 */
final class ItemLoreEditor {

    /**
     * 按当前模式生成编辑预览物品。
     */
    ItemStack createPreview(ItemStack source, ItemLoreOperation operation, String loreLine) {
        if (!hasUsableItem(source)) {
            return null;
        }

        if (operation.requiresText() && loreLine.isBlank()) {
            return null;
        }

        List<Component> lore = snapshot(source);
        int loreIndex = lore.size() - 1;
        if (operation != ItemLoreOperation.ADD && lore.isEmpty()) {
            return null;
        }

        ItemStack preview = source.clone();
        if (!apply(preview, operation, loreIndex, loreLine)) {
            return null;
        }
        return preview;
    }

    /**
     * 读取当前 lore 快照。
     */
    List<Component> snapshot(ItemStack item) {
        if (!hasUsableItem(item)) {
            return List.of();
        }

        ItemMeta meta = item.getItemMeta();
        if (meta == null || meta.lore() == null) {
            return List.of();
        }
        return List.copyOf(meta.lore());
    }

    /**
     * 判断物品是否可操作。
     */
    boolean hasUsableItem(ItemStack item) {
        return item != null && item.getType() != Material.AIR;
    }

    /**
     * 按操作类型写入 lore。
     */
    private boolean apply(ItemStack item, ItemLoreOperation operation, int loreIndex, String loreLine) {
        return switch (operation) {
            case ADD -> appendLoreLine(item, loreLine);
            case EDIT -> editLoreLine(item, loreIndex, loreLine);
            case REMOVE -> removeLoreLine(item, loreIndex);
        };
    }

    /**
     * 在物品末尾添加一行 lore。
     */
    private boolean appendLoreLine(ItemStack item, String loreLine) {
        ItemMeta meta = item.getItemMeta();
        if (meta == null) {
            return false;
        }

        List<Component> existingLore = meta.lore();
        List<Component> lore = existingLore == null ? new ArrayList<>() : new ArrayList<>(existingLore);
        lore.add(Component.text(loreLine));
        meta.lore(lore);
        return item.setItemMeta(meta);
    }

    /**
     * 修改指定位置的 lore。
     */
    private boolean editLoreLine(ItemStack item, int loreIndex, String loreLine) {
        ItemMeta meta = item.getItemMeta();
        if (meta == null) {
            return false;
        }

        List<Component> existingLore = meta.lore();
        if (existingLore == null || loreIndex < 0 || loreIndex >= existingLore.size()) {
            return false;
        }

        List<Component> lore = new ArrayList<>(existingLore);
        lore.set(loreIndex, Component.text(loreLine));
        meta.lore(lore);
        return item.setItemMeta(meta);
    }

    /**
     * 删除指定位置的 lore。
     */
    private boolean removeLoreLine(ItemStack item, int loreIndex) {
        ItemMeta meta = item.getItemMeta();
        if (meta == null) {
            return false;
        }

        List<Component> existingLore = meta.lore();
        if (existingLore == null || loreIndex < 0 || loreIndex >= existingLore.size()) {
            return false;
        }

        List<Component> lore = new ArrayList<>(existingLore);
        lore.remove(loreIndex);
        meta.lore(lore.isEmpty() ? null : lore);
        return item.setItemMeta(meta);
    }
}
