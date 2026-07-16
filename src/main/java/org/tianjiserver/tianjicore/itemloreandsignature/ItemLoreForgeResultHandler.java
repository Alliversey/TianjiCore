package org.tianjiserver.tianjicore.itemloreandsignature;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;
import org.tianjiserver.tianjicore.tianjicoreutil.VaultUtil;

import java.util.List;
import java.util.Map;

/**
 * 处理玩家确认锻造后的校验、扣费与结果发放。
 */
final class ItemLoreForgeResultHandler {

    private final MiniMessage mini = MiniMessage.miniMessage();
    private final ItemLoreForgeConfig forgeConfig;
    private final ItemLoreEditor loreEditor;
    private final ItemLoreForgeViewFactory viewFactory;

    ItemLoreForgeResultHandler(
            ItemLoreForgeConfig forgeConfig,
            ItemLoreEditor loreEditor,
            ItemLoreForgeViewFactory viewFactory
    ) {
        this.forgeConfig = forgeConfig;
        this.loreEditor = loreEditor;
        this.viewFactory = viewFactory;
    }

    /**
     * 处理预览物品取出：校验输入、扣费、交付结果，失败则回滚退款。
     */
    void handleConfirm(Player player, ItemLoreForgeSession session, InventoryClickEvent event) {
        ItemLoreOperation operation = session.operation();
        String loreLine = viewFactory.resolveRenameText(session).trim();
        ItemStack item = session.originalInput();

        if (!loreEditor.hasUsableItem(item)) {
            player.sendMessage(mini.deserialize("<red>请先将要修改 lore 的物品放入第一格"));
            return;
        }

        List<Component> lore = loreEditor.snapshot(item);
        int visibleLoreCount = loreEditor.visibleLoreEntryCount(lore);
        int loreIndex = loreEditor.lastVisibleLoreIndex(lore);
        if (operation.requiresText() && loreLine.isBlank()) {
            player.sendMessage(mini.deserialize("<red>请输入 lore 内容"));
            return;
        }
        if (viewFactory.isUnchangedPlaceholder(session, loreLine)) {
            player.sendMessage(mini.deserialize("<red>请先把默认提示改成要写入的 lore 内容"));
            return;
        }
        if (operation != ItemLoreOperation.ADD && !validateLoreIndex(player, visibleLoreCount, loreIndex)) {
            return;
        }

        ItemStack result = loreEditor.createPreview(item, operation, loreLine);
        if (!loreEditor.hasUsableItem(result)) {
            player.sendMessage(mini.deserialize("<red>无法生成编辑预览，请检查输入内容"));
            return;
        }

        double cost = forgeConfig.resolveCost(visibleLoreCount);
        VaultUtil.TransactionResult withdrawResult = VaultUtil.withdraw(player, cost);
        if (!withdrawResult.success()) {
            player.sendMessage(mini.deserialize("<red>余额不足或扣费失败，操作已取消"));
            return;
        }

        if (!giveResultToPlayer(player, event, result)) {
            VaultUtil.deposit(player, cost);
            player.sendMessage(mini.deserialize("<red>请先清空鼠标上的物品或背包空间"));
            return;
        }

        session.inventory().setFirstItem(null);
        session.setOriginalInput(null);
        session.setPlaceholderText("");
        session.inventory().setResult(null);
        viewFactory.updateTitle(session);
        player.sendMessage(mini.deserialize(
                "<green>" + operation.successVerb() + "成功，已扣除 <gold>" + forgeConfig.formatCost(cost) + "</gold>"
        ));
    }

    /**
     * 将第三格结果交给玩家。
     */
    private boolean giveResultToPlayer(Player player, InventoryClickEvent event, ItemStack result) {
        if (event.isShiftClick()) {
            Map<Integer, ItemStack> overflow = player.getInventory().addItem(result.clone());
            return overflow.isEmpty();
        }

        ItemStack cursor = event.getCursor();
        if (!loreEditor.hasUsableItem(cursor)) {
            event.setCursor(result.clone());
            return true;
        }

        if (!cursor.isSimilar(result)) {
            return false;
        }

        int maxStackSize = Math.min(cursor.getMaxStackSize(), result.getMaxStackSize());
        if (cursor.getAmount() + result.getAmount() > maxStackSize) {
            return false;
        }

        cursor.setAmount(cursor.getAmount() + result.getAmount());
        event.setCursor(cursor);
        return true;
    }

    /**
     * 校验目标 lore 行是否存在。
     */
    private boolean validateLoreIndex(Player player, int visibleLoreCount, int loreIndex) {
        if (visibleLoreCount == 0) {
            player.sendMessage(mini.deserialize("<red>当前物品还没有 lore"));
            return false;
        }
        if (loreIndex < 0) {
            player.sendMessage(mini.deserialize(
                    "<red>无效的 lore 行号，当前共有 <gold>" + visibleLoreCount + "</gold> 行"
            ));
            return false;
        }
        return true;
    }
}
