package org.tianjiserver.tianjicore.itemloreandsignature;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.wesjd.anvilgui.AnvilGUI;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.Listener;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.tianjiserver.tianjicore.TianjiCore;
import org.tianjiserver.tianjicore.tianjicoreutil.VaultUtil;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * 让玩家通过铁砧界面为物品追加一行 lore，并扣除对应货币。
 */
public class ItemLoreAndSignature implements Listener {

    private static final String CONFIG_FORGE_COST_PATH = "itemloreandsignature.forge.cost";
    private static final String CONFIG_FORGE_TITLE_PATH = "itemloreandsignature.forge.title";
    private static final String CONFIG_FORGE_TEXT_PATH = "itemloreandsignature.forge.text";
    private static final double DEFAULT_FORGE_COST = 1000.0D;
    private static final String DEFAULT_FORGE_TITLE = "物品锻造";
    private static final String DEFAULT_FORGE_TEXT = "输入要添加的 lore";

    private final TianjiCore plugin;
    private final MiniMessage mini = MiniMessage.miniMessage();

    /**
     * 创建锻造模块并写入默认配置。
     */
    public ItemLoreAndSignature(TianjiCore plugin) {
        this.plugin = plugin;
        registerDefaults();
    }

    /**
     * 打开锻造铁砧界面。
     */
    public void openForgeUi(Player player) {
        if (!VaultUtil.isAvailable()) {
            player.sendMessage(mini.deserialize("<red>经济系统不可用，无法进行锻造"));
            return;
        }

        new AnvilGUI.Builder()
                .plugin(plugin)
                .title(plugin.getConfig().getString(CONFIG_FORGE_TITLE_PATH, DEFAULT_FORGE_TITLE))
                .text(plugin.getConfig().getString(CONFIG_FORGE_TEXT_PATH, DEFAULT_FORGE_TEXT))
                .onClick((slot, snapshot) -> onClickForge(slot, snapshot))
                .open(player);
    }

    /**
     * 处理锻造点击：校验输入、扣费、写入 lore，失败则回滚退款。
     */
    private List<AnvilGUI.ResponseAction> onClickForge(int slot, AnvilGUI.StateSnapshot snapshot) {
        if (slot != AnvilGUI.Slot.OUTPUT) {
            return List.of();
        }

        Player player = snapshot.getPlayer();
        ItemStack item = snapshot.getLeftItem();
        String loreLine = snapshot.getText() == null ? "" : snapshot.getText().trim();

        if (item == null || item.getType() == Material.AIR) {
            player.sendMessage(mini.deserialize("<red>请先放入要锻造的物品"));
            return List.of();
        }
        if (loreLine.isBlank()) {
            player.sendMessage(mini.deserialize("<red>请输入要添加的 lore 内容"));
            return List.of();
        }

        double cost = resolveForgeCost();
        VaultUtil.TransactionResult withdrawResult = VaultUtil.withdraw(player, cost);
        if (!withdrawResult.success()) {
            player.sendMessage(mini.deserialize("<red>余额不足或扣费失败，锻造已取消"));
            return List.of();
        }

        if (!appendLore(item, loreLine)) {
            VaultUtil.deposit(player, cost);
            player.sendMessage(mini.deserialize("<red>锻造失败，已自动退款"));
            return List.of();
        }

        String costText = String.format(Locale.ROOT, "%.2f", cost);
        player.sendMessage(mini.deserialize("<green>锻造成功，已扣除 <gold>" + costText + "</gold>"));
        return List.of(AnvilGUI.ResponseAction.close());
    }

    /**
     * 在物品末尾追加一行 lore。
     */
    private boolean appendLore(ItemStack item, String loreLine) {
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
     * 读取锻造费用并兜底为合法默认值。
     */
    private double resolveForgeCost() {
        double configured = plugin.getConfig().getDouble(CONFIG_FORGE_COST_PATH, DEFAULT_FORGE_COST);
        return configured > 0D ? configured : DEFAULT_FORGE_COST;
    }

    /**
     * 写入锻造模块默认配置项。
     */
    private void registerDefaults() {
        plugin.getConfig().addDefault(CONFIG_FORGE_COST_PATH, DEFAULT_FORGE_COST);
        plugin.getConfig().addDefault(CONFIG_FORGE_TITLE_PATH, DEFAULT_FORGE_TITLE);
        plugin.getConfig().addDefault(CONFIG_FORGE_TEXT_PATH, DEFAULT_FORGE_TEXT);
        plugin.getConfig().options().copyDefaults(true);
        plugin.saveConfig();
    }
}
