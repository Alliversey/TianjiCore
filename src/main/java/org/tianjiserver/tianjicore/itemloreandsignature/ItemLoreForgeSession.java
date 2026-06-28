package org.tianjiserver.tianjicore.itemloreandsignature;

import org.bukkit.inventory.AnvilInventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.view.AnvilView;

/**
 * 单个玩家打开的锻造 UI 状态。
 */
final class ItemLoreForgeSession {

    private final AnvilView view;
    private final AnvilInventory inventory;
    private ItemStack originalInput;
    private ItemLoreOperation operation;
    private String placeholderText;

    ItemLoreForgeSession(AnvilView view, AnvilInventory inventory, ItemLoreOperation operation) {
        this.view = view;
        this.inventory = inventory;
        this.operation = operation;
        this.placeholderText = "";
    }

    AnvilView view() {
        return view;
    }

    AnvilInventory inventory() {
        return inventory;
    }

    ItemLoreOperation operation() {
        return operation;
    }

    ItemStack originalInput() {
        return originalInput;
    }

    void setOriginalInput(ItemStack originalInput) {
        this.originalInput = originalInput;
    }

    void setOperation(ItemLoreOperation operation) {
        this.operation = operation;
    }

    String placeholderText() {
        return placeholderText;
    }

    void setPlaceholderText(String placeholderText) {
        this.placeholderText = placeholderText;
    }
}
