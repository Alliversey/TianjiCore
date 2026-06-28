package org.tianjiserver.tianjicore.itemloreandsignature;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.inventory.PrepareAnvilEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.AnvilInventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.MenuType;
import org.bukkit.inventory.view.AnvilView;
import org.tianjiserver.tianjicore.TianjiCore;
import org.tianjiserver.tianjicore.tianjicoreutil.VaultUtil;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * 让玩家通过付费方式添加、修改、删除铁砧中物品的 lore。
 */
public class ItemLoreAndSignature implements Listener {

    public static final String MODULE_KEY = "itemloreandsignature";

    private static final int ANVIL_INPUT_SLOT = 0;
    private static final int ANVIL_MODE_SLOT = 1;
    private static final int ANVIL_OUTPUT_SLOT = 2;

    private final TianjiCore plugin;
    private final MiniMessage mini = MiniMessage.miniMessage();
    private final ItemLoreForgeConfig forgeConfig;
    private final ItemLoreEditor loreEditor;
    private final ItemLoreForgeViewFactory viewFactory;
    private final ItemLoreForgeResultHandler resultHandler;
    private final Map<UUID, ItemLoreForgeSession> sessions = new HashMap<>();

    /**
     * 创建 lore 模块并写入默认配置。
     */
    public ItemLoreAndSignature(TianjiCore plugin) {
        this.plugin = plugin;
        this.forgeConfig = new ItemLoreForgeConfig(plugin);
        this.loreEditor = new ItemLoreEditor();
        this.viewFactory = new ItemLoreForgeViewFactory(plugin, forgeConfig, loreEditor);
        this.resultHandler = new ItemLoreForgeResultHandler(forgeConfig, loreEditor, viewFactory);
        forgeConfig.registerDefaults();
    }

    /**
     * 打开 lore 锻造铁砧界面。
     */
    public void openForgeUi(Player player) {
        if (!VaultUtil.isAvailable()) {
            player.sendMessage(mini.deserialize("<red>经济系统不可用，无法修改 lore"));
            return;
        }

        openLoreInputUi(player, ItemLoreOperation.ADD);
    }

    /**
     * 清理离线玩家的 UI 状态。
     */
    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        sessions.remove(event.getPlayer().getUniqueId());
    }

    /**
     * 玩家关闭锻造 UI 时清理状态。
     */
    @EventHandler
    public void onInventoryClose(InventoryCloseEvent event) {
        if (!(event.getPlayer() instanceof Player player)) {
            return;
        }

        ItemLoreForgeSession session = sessions.get(player.getUniqueId());
        if (session != null && event.getInventory().equals(session.inventory())) {
            restoreInputItem(session);
            session.inventory().setSecondItem(null);
            session.inventory().setResult(null);
            sessions.remove(player.getUniqueId());
        }
    }

    /**
     * 处理模式按钮点击：左键下一种，右键上一种。
     */
    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) {
            return;
        }

        ItemLoreForgeSession session = sessions.get(player.getUniqueId());
        if (session == null || !event.getInventory().equals(session.inventory())) {
            return;
        }

        if (event.getRawSlot() >= ANVIL_INPUT_SLOT && event.getRawSlot() <= ANVIL_OUTPUT_SLOT) {
            event.setCancelled(event.getRawSlot() != ANVIL_INPUT_SLOT);
        }
        if (event.isShiftClick()) {
            event.setCancelled(true);
        }
        if (event.getRawSlot() == ANVIL_INPUT_SLOT && viewFactory.isPlaceholderItem(event.getCurrentItem())) {
            event.setCurrentItem(cloneOrNull(session.originalInput()));
        }

        if (event.getRawSlot() == ANVIL_MODE_SLOT) {
            int direction = resolveModeSwitchDirection(event.getClick());
            if (direction != 0) {
                switchMode(session, session.operation().shift(direction));
            }
            return;
        }

        if (event.getRawSlot() == ANVIL_INPUT_SLOT) {
            plugin.getServer().getScheduler().runTask(plugin, () -> syncInputItem(session));
            return;
        }

        if (event.getRawSlot() == ANVIL_OUTPUT_SLOT) {
            resultHandler.handleConfirm(player, session, event);
        }
    }

    /**
     * 保持第三格始终展示当前模式的编辑预览。
     */
    @EventHandler
    public void onPrepareAnvil(PrepareAnvilEvent event) {
        if (!(event.getView().getPlayer() instanceof Player player)) {
            return;
        }

        ItemLoreForgeSession session = sessions.get(player.getUniqueId());
        if (session == null || !event.getInventory().equals(session.inventory())) {
            return;
        }

        event.getView().setRepairCost(0);
        event.setResult(viewFactory.createPreviewItem(session));
    }

    /**
     * 避免拖拽覆盖模式按钮和结果预览。
     */
    @EventHandler
    public void onInventoryDrag(InventoryDragEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) {
            return;
        }

        ItemLoreForgeSession session = sessions.get(player.getUniqueId());
        if (session == null || !event.getInventory().equals(session.inventory())) {
            return;
        }

        if (event.getRawSlots().stream().anyMatch(slot -> slot == ANVIL_MODE_SLOT || slot == ANVIL_OUTPUT_SLOT)) {
            event.setCancelled(true);
            return;
        }

        if (event.getRawSlots().contains(ANVIL_INPUT_SLOT)) {
            plugin.getServer().getScheduler().runTask(plugin, () -> syncInputItem(session));
        }
    }

    /**
     * 打开共用的 lore 文本输入界面。
     */
    private void openLoreInputUi(Player player, ItemLoreOperation operation) {
        sessions.remove(player.getUniqueId());

        AnvilView view = MenuType.ANVIL.create(player, Component.text(viewFactory.createTitle(operation, null)));
        AnvilInventory inventory = view.getTopInventory();
        inventory.setSecondItem(viewFactory.createModeButton(operation));
        view.setRepairCost(0);
        player.openInventory(view);
        sessions.put(player.getUniqueId(), new ItemLoreForgeSession(view, inventory, operation));
    }

    /**
     * 根据点击类型决定模式切换方向。
     */
    private int resolveModeSwitchDirection(ClickType clickType) {
        return switch (clickType) {
            case LEFT, SHIFT_LEFT -> 1;
            case RIGHT, SHIFT_RIGHT -> -1;
            default -> 0;
        };
    }

    /**
     * 切换当前锻造模式并刷新按钮。
     */
    private void switchMode(ItemLoreForgeSession session, ItemLoreOperation operation) {
        session.setOperation(operation);
        refreshInputPlaceholder(session);
        session.inventory().setSecondItem(viewFactory.createModeButton(operation));
        session.inventory().setResult(viewFactory.createPreviewItem(session));
        session.view().setRepairCost(0);
        viewFactory.updateTitle(session);
    }

    /**
     * 同步第一格物品：保存原始物品，并用临时显示名驱动铁砧文本框提示。
     */
    private void syncInputItem(ItemLoreForgeSession session) {
        ItemStack input = session.inventory().getFirstItem();
        if (!loreEditor.hasUsableItem(input)) {
            session.setOriginalInput(null);
            session.setPlaceholderText("");
            session.inventory().setResult(null);
            viewFactory.updateTitle(session);
            return;
        }

        if (viewFactory.isPlaceholderItem(input)) {
            session.inventory().setResult(viewFactory.createPreviewItem(session));
            viewFactory.updateTitle(session);
            return;
        }

        session.setOriginalInput(input.clone());
        refreshInputPlaceholder(session);
        session.inventory().setResult(viewFactory.createPreviewItem(session));
    }

    /**
     * 刷新第一格占位文本。
     */
    private void refreshInputPlaceholder(ItemLoreForgeSession session) {
        ItemStack original = session.originalInput();
        if (!loreEditor.hasUsableItem(original)) {
            return;
        }

        String placeholderText = viewFactory.createPlaceholderText(session.operation());
        session.setPlaceholderText(placeholderText);
        session.inventory().setFirstItem(viewFactory.createPlaceholderInputItem(original, placeholderText));
        viewFactory.updateTitle(session);
    }

    /**
     * 关闭或取回时还原第一格原始物品。
     */
    private void restoreInputItem(ItemLoreForgeSession session) {
        if (viewFactory.isPlaceholderItem(session.inventory().getFirstItem())) {
            session.inventory().setFirstItem(cloneOrNull(session.originalInput()));
        }
    }

    /**
     * 安全克隆物品。
     */
    private ItemStack cloneOrNull(ItemStack item) {
        return item == null ? null : item.clone();
    }
}
