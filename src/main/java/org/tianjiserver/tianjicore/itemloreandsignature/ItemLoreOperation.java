package org.tianjiserver.tianjicore.itemloreandsignature;

import org.bukkit.Material;

/**
 * lore 操作类型。
 */
enum ItemLoreOperation {
    ADD(
            Material.NAME_TAG,
            "<green>添加 Lore",
            "<gray>确认后会追加一行 lore",
            "添加"
    ),
    EDIT(
            Material.WRITABLE_BOOK,
            "<yellow>修改 Lore",
            "<gray>确认后会修改最后一行 lore",
            "修改"
    ),
    REMOVE(
            Material.BARRIER,
            "<red>删除 Lore",
            "<gray>确认后会删除最后一行 lore",
            "删除"
    );

    private final Material buttonMaterial;
    private final String buttonName;
    private final String buttonHint;
    private final String successVerb;

    ItemLoreOperation(
            Material buttonMaterial,
            String buttonName,
            String buttonHint,
            String successVerb
    ) {
        this.buttonMaterial = buttonMaterial;
        this.buttonName = buttonName;
        this.buttonHint = buttonHint;
        this.successVerb = successVerb;
    }

    Material buttonMaterial() {
        return buttonMaterial;
    }

    String buttonName() {
        return buttonName;
    }

    String buttonHint() {
        return buttonHint;
    }

    String successVerb() {
        return successVerb;
    }

    ItemLoreOperation shift(int direction) {
        ItemLoreOperation[] operations = values();
        int index = (ordinal() + direction + operations.length) % operations.length;
        return operations[index];
    }

    boolean requiresText() {
        return this != REMOVE;
    }
}
