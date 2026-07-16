package org.tianjiserver.tianjicore.itemloreandsignature;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;

/**
 * 负责读取与修改物品 lore。
 */
final class ItemLoreEditor {

    private static final LegacyComponentSerializer LORE_SERIALIZER = LegacyComponentSerializer.builder()
            .character('&')
            .hexCharacter('#')
            .hexColors()
            .build();
    private static final PlainTextComponentSerializer PLAIN_TEXT_SERIALIZER = PlainTextComponentSerializer.plainText();

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
        int loreIndex = lastVisibleLoreIndex(lore);
        if (operation != ItemLoreOperation.ADD && loreIndex < 0) {
            return null;
        }

        ItemStack preview = source.clone();
        int targetIndex = operation == ItemLoreOperation.ADD ? loreIndex + 1 : loreIndex;
        if (!apply(preview, operation, targetIndex, loreLine)) {
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
     * 统计玩家可见的 lore 行数。
     */
    int visibleLoreEntryCount(ItemStack item) {
        return visibleLoreEntryCount(snapshot(item));
    }

    /**
     * 统计玩家可见的 lore 行数。
     */
    int visibleLoreEntryCount(List<Component> lore) {
        int count = 0;
        for (Component line : lore) {
            if (isVisibleLoreLine(line)) {
                count++;
            }
        }
        return count;
    }

    /**
     * 查找最后一条玩家可见的 lore 下标。
     */
    int lastVisibleLoreIndex(List<Component> lore) {
        for (int index = lore.size() - 1; index >= 0; index--) {
            if (isVisibleLoreLine(lore.get(index))) {
                return index;
            }
        }
        return -1;
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
            case ADD -> appendLoreLine(item, loreIndex, loreLine);
            case EDIT -> editLoreLine(item, loreIndex, loreLine);
            case REMOVE -> removeLoreLine(item, loreIndex);
        };
    }

    /**
     * 在最后一条可见 lore 后添加一行 lore。
     */
    private boolean appendLoreLine(ItemStack item, int loreIndex, String loreLine) {
        ItemMeta meta = item.getItemMeta();
        if (meta == null) {
            return false;
        }

        List<Component> existingLore = meta.lore();
        List<Component> lore = existingLore == null ? new ArrayList<>() : new ArrayList<>(existingLore);
        int insertionIndex = Math.max(0, Math.min(loreIndex, lore.size()));
        lore.add(insertionIndex, parseLoreLine(loreLine));
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
        lore.set(loreIndex, parseLoreLine(loreLine));
        meta.lore(lore);
        return item.setItemMeta(meta);
    }

    /**
     * 将玩家输入的格式代码转换为 lore 组件。
     */
    private Component parseLoreLine(String loreLine) {
        return LORE_SERIALIZER.deserialize(loreLine.replace('§', '&'))
                .decorationIfAbsent(TextDecoration.ITALIC, TextDecoration.State.FALSE);
    }

    /**
     * 判断 lore 行是否包含实际可见字符。
     */
    private boolean isVisibleLoreLine(Component line) {
        return PLAIN_TEXT_SERIALIZER.serialize(line)
                .codePoints()
                .anyMatch(ItemLoreEditor::isVisibleCodePoint);
    }

    /**
     * 过滤空白、控制符、零宽字符和孤立组合符。
     */
    private static boolean isVisibleCodePoint(int codePoint) {
        return !Character.isWhitespace(codePoint)
                && switch (Character.getType(codePoint)) {
                    case Character.CONTROL,
                            Character.FORMAT,
                            Character.NON_SPACING_MARK,
                            Character.ENCLOSING_MARK,
                            Character.COMBINING_SPACING_MARK -> false;
                    default -> true;
                };
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
