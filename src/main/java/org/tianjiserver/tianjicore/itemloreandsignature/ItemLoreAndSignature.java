package org.tianjiserver.tianjicore.itemloreandsignature;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.NamespacedKey;
import org.bukkit.Material;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.inventory.PrepareAnvilEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.AnvilInventory;
import org.bukkit.inventory.MenuType;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.inventory.view.AnvilView;
import org.tianjiserver.tianjicore.TianjiCore;
import org.tianjiserver.tianjicore.tianjicoreutil.VaultUtil;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

/**
 * 让玩家通过付费方式添加、修改、删除铁砧中物品的 lore。
 */
public class ItemLoreAndSignature implements Listener {

    private static final int ANVIL_MODE_SLOT = 1;
    private static final int ANVIL_OUTPUT_SLOT = 2;
    private static final String PLACEHOLDER_MARKER_VALUE = "true";

    private static final String CONFIG_BASE_COST_PATH = "itemloreandsignature.forge.cost";
    private static final String CONFIG_LINEAR_INCREASE_MULTIPLIER_PATH =
            "itemloreandsignature.forge.linear-increase-multiplier";
    private static final String LEGACY_ADD_COST_PATH = "itemloreandsignature.forge.add.cost";
    private static final String LEGACY_EDIT_COST_PATH = "itemloreandsignature.forge.edit.cost";
    private static final String LEGACY_REMOVE_COST_PATH = "itemloreandsignature.forge.remove.cost";

    private static final double DEFAULT_BASE_COST = 1000.0D;
    private static final double DEFAULT_LINEAR_INCREASE_MULTIPLIER = 1.5D;
    private static final String TEXT_PLACEHOLDER = "（请输入文本）";
    private static final String NO_TEXT_PLACEHOLDER = "（无需输入）";

    private final TianjiCore plugin;
    private final MiniMessage mini = MiniMessage.miniMessage();
    private final NamespacedKey placeholderKey;
    private final Map<UUID, ForgeSession> sessions = new HashMap<>();

    /**
     * 创建 lore 模块并写入默认配置。
     */
    public ItemLoreAndSignature(TianjiCore plugin) {
        this.plugin = plugin;
        this.placeholderKey = new NamespacedKey(plugin, "lore_input_placeholder");
        registerDefaults();
    }

    /**
     * 打开 lore 锻造铁砧界面。
     */
    public void openForgeUi(Player player) {
        if (!VaultUtil.isAvailable()) {
            player.sendMessage(mini.deserialize("<red>经济系统不可用，无法修改 lore"));
            return;
        }

        openLoreInputUi(
                player,
                LoreOperation.ADD
        );
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

        ForgeSession session = sessions.get(player.getUniqueId());
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

        ForgeSession session = sessions.get(player.getUniqueId());
        if (session == null || !event.getInventory().equals(session.inventory())) {
            return;
        }

        if (event.getRawSlot() >= 0 && event.getRawSlot() <= ANVIL_OUTPUT_SLOT) {
            event.setCancelled(event.getRawSlot() != 0);
        }
        if (event.isShiftClick()) {
            event.setCancelled(true);
        }
        if (event.getRawSlot() == 0 && isPlaceholderItem(event.getCurrentItem())) {
            event.setCurrentItem(cloneOrNull(session.originalInput()));
        }

        if (event.getRawSlot() == ANVIL_MODE_SLOT) {
            int direction = resolveModeSwitchDirection(event.getClick());
            if (direction != 0) {
                switchMode(player, session, session.operation().shift(direction));
            }
            return;
        }

        if (event.getRawSlot() == 0) {
            plugin.getServer().getScheduler().runTask(plugin, () -> syncInputItem(session));
            return;
        }

        if (event.getRawSlot() == ANVIL_OUTPUT_SLOT) {
            handleConfirm(player, session, event);
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

        ForgeSession session = sessions.get(player.getUniqueId());
        if (session == null || !event.getInventory().equals(session.inventory())) {
            return;
        }

        event.getView().setRepairCost(0);
        event.setResult(createPreviewItem(session));
    }

    /**
     * 避免拖拽覆盖模式按钮和结果预览。
     */
    @EventHandler
    public void onInventoryDrag(InventoryDragEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) {
            return;
        }

        ForgeSession session = sessions.get(player.getUniqueId());
        if (session == null || !event.getInventory().equals(session.inventory())) {
            return;
        }

        if (event.getRawSlots().stream().anyMatch(slot -> slot == ANVIL_MODE_SLOT || slot == ANVIL_OUTPUT_SLOT)) {
            event.setCancelled(true);
            return;
        }

        if (event.getRawSlots().contains(0)) {
            plugin.getServer().getScheduler().runTask(plugin, () -> syncInputItem(session));
        }
    }

    /**
     * 打开共用的 lore 文本输入界面。
     */
    private void openLoreInputUi(Player player, LoreOperation operation) {
        sessions.remove(player.getUniqueId());

        AnvilView view = MenuType.ANVIL.create(player, Component.text(createTitle(operation, null)));
        AnvilInventory inventory = view.getTopInventory();
        inventory.setSecondItem(createModeButton(operation));
        view.setRepairCost(0);
        player.openInventory(view);
        sessions.put(player.getUniqueId(), new ForgeSession(view, inventory, operation));
    }

    /**
     * 处理预览物品取出：校验输入、扣费、交付结果，失败则回滚退款。
     */
    private void handleConfirm(Player player, ForgeSession session, InventoryClickEvent event) {
        LoreOperation operation = session.operation();
        String loreLine = resolveRenameText(session).trim();
        ItemStack item = session.originalInput();

        if (!hasUsableItem(item)) {
            player.sendMessage(mini.deserialize("<red>请先将要修改 lore 的物品放入第一格"));
            return;
        }
        List<Component> lore = loreSnapshot(item);

        int loreIndex = lore.size() - 1;
        if (operation.requiresText() && loreLine.isBlank()) {
            player.sendMessage(mini.deserialize("<red>请输入 lore 内容"));
            return;
        }
        if (operation.requiresText() && isUnchangedPlaceholder(session, loreLine)) {
            player.sendMessage(mini.deserialize("<red>请先把默认提示改成要写入的 lore 内容"));
            return;
        }
        if (operation != LoreOperation.ADD && !validateLoreIndex(player, lore, loreIndex)) {
            return;
        }

        ItemStack result = createPreviewItem(item, operation, loreLine);
        if (!hasUsableItem(result)) {
            player.sendMessage(mini.deserialize("<red>无法生成编辑预览，请检查输入内容"));
            return;
        }

        double cost = resolveCost(lore.size());
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
        updateViewTitle(session);
        player.sendMessage(mini.deserialize(
                "<green>" + operation.successVerb + "成功，已扣除 <gold>" + formatCost(cost) + "</gold>"
        ));
    }

    /**
     * 按操作类型写入 lore。
     */
    private boolean applyLoreChange(ItemStack item, LoreOperation operation, int loreIndex, String loreLine) {
        return switch (operation) {
            case ADD -> appendLoreLine(item, loreLine);
            case EDIT -> editLoreLine(item, loreIndex, loreLine);
            case REMOVE -> removeLoreLine(item, loreIndex);
        };
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
    private void switchMode(Player player, ForgeSession session, LoreOperation operation) {
        session.setOperation(operation);
        refreshInputPlaceholder(session);
        session.inventory().setSecondItem(createModeButton(operation));
        session.inventory().setResult(createPreviewItem(session));
        session.view().setRepairCost(0);
        updateViewTitle(session);
    }

    /**
     * 同步第一格物品：保存原始物品，并用临时显示名驱动铁砧文本框提示。
     */
    private void syncInputItem(ForgeSession session) {
        ItemStack input = session.inventory().getFirstItem();
        if (!hasUsableItem(input)) {
            session.setOriginalInput(null);
            session.setPlaceholderText("");
            session.inventory().setResult(null);
            updateViewTitle(session);
            return;
        }

        if (isPlaceholderItem(input)) {
            session.inventory().setResult(createPreviewItem(session));
            updateViewTitle(session);
            return;
        }

        session.setOriginalInput(input.clone());
        refreshInputPlaceholder(session);
        session.inventory().setResult(createPreviewItem(session));
    }

    /**
     * 刷新第一格占位文本。
     */
    private void refreshInputPlaceholder(ForgeSession session) {
        ItemStack original = session.originalInput();
        if (!hasUsableItem(original)) {
            return;
        }

        String placeholderText = createPlaceholderText(original, session.operation());
        session.setPlaceholderText(placeholderText);
        session.inventory().setFirstItem(createPlaceholderInputItem(original, placeholderText));
        updateViewTitle(session);
    }

    /**
     * 生成编辑预览物品。
     */
    private ItemStack createPreviewItem(ForgeSession session) {
        String loreLine = resolveRenameText(session).trim();
        if (isUnchangedPlaceholder(session, loreLine)) {
            return null;
        }

        return createPreviewItem(
                session.originalInput(),
                session.operation(),
                loreLine
        );
    }

    /**
     * 生成第一格临时展示物品。
     */
    private ItemStack createPlaceholderInputItem(ItemStack source, String placeholderText) {
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
    private String createPlaceholderText(ItemStack item, LoreOperation operation) {
        if (!operation.requiresText()) {
            return NO_TEXT_PLACEHOLDER;
        }
        return TEXT_PLACEHOLDER;
    }

    /**
     * 刷新铁砧标题中的费用。
     */
    private void updateViewTitle(ForgeSession session) {
        session.view().setTitle(createTitle(session.operation(), session.originalInput()));
    }

    /**
     * 生成带费用的铁砧标题。
     */
    private String createTitle(LoreOperation operation, ItemStack item) {
        int loreEntryCount = hasUsableItem(item) ? loreSnapshot(item).size() : 0;
        return operation.successVerb + "费用" + formatCost(resolveCost(loreEntryCount));
    }

    /**
     * 按当前模式生成编辑预览物品。
     */
    private ItemStack createPreviewItem(ItemStack source, LoreOperation operation, String loreLine) {
        if (!hasUsableItem(source)) {
            return null;
        }

        if (operation.requiresText() && loreLine.isBlank()) {
            return null;
        }

        List<Component> lore = loreSnapshot(source);
        int loreIndex = lore.size() - 1;
        if (operation != LoreOperation.ADD && lore.isEmpty()) {
            return null;
        }

        ItemStack preview = source.clone();
        if (!applyLoreChange(preview, operation, loreIndex, loreLine)) {
            return null;
        }
        return preview;
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
        if (!hasUsableItem(cursor)) {
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
     * 生成当前模式按钮。
     */
    private ItemStack createModeButton(LoreOperation operation) {
        ItemStack item = new ItemStack(operation.buttonMaterial);
        ItemMeta meta = item.getItemMeta();
        if (meta == null) {
            return item;
        }

        meta.displayName(mini.deserialize(operation.buttonName));
        meta.lore(List.of(
                mini.deserialize("<gray>左键切换下一种模式"),
                mini.deserialize("<gray>右键切换上一种模式"),
                mini.deserialize(operation.buttonHint)
        ));
        item.setItemMeta(meta);
        return item;
    }

    /**
     * 读取铁砧输入框文本。
     */
    private String resolveRenameText(ForgeSession session) {
        String renameText = session.inventory().getRenameText();
        return renameText == null ? "" : renameText;
    }

    /**
     * 判断玩家是否仍保留默认提示文本。
     */
    private boolean isUnchangedPlaceholder(ForgeSession session, String loreLine) {
        return session.operation().requiresText() && loreLine.equals(session.placeholderText());
    }

    /**
     * 判断物品是否为第一格临时展示物品。
     */
    private boolean isPlaceholderItem(ItemStack item) {
        if (!hasUsableItem(item)) {
            return false;
        }

        ItemMeta meta = item.getItemMeta();
        return meta != null
                && meta.getPersistentDataContainer().has(placeholderKey, PersistentDataType.STRING);
    }

    /**
     * 关闭或取回时还原第一格原始物品。
     */
    private void restoreInputItem(ForgeSession session) {
        if (isPlaceholderItem(session.inventory().getFirstItem())) {
            session.inventory().setFirstItem(cloneOrNull(session.originalInput()));
        }
    }

    /**
     * 安全克隆物品。
     */
    private ItemStack cloneOrNull(ItemStack item) {
        return item == null ? null : item.clone();
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

    /**
     * 校验目标 lore 行是否存在。
     */
    private boolean validateLoreIndex(Player player, List<Component> lore, int loreIndex) {
        if (lore.isEmpty()) {
            player.sendMessage(mini.deserialize("<red>当前物品还没有 lore"));
            return false;
        }
        if (loreIndex < 0 || loreIndex >= lore.size()) {
            player.sendMessage(mini.deserialize(
                    "<red>无效的 lore 行号，当前共有 <gold>" + lore.size() + "</gold> 行"
            ));
            return false;
        }
        return true;
    }

    /**
     * 读取当前 lore 快照。
     */
    private List<Component> loreSnapshot(ItemStack item) {
        ItemMeta meta = item.getItemMeta();
        if (meta == null || meta.lore() == null) {
            return List.of();
        }
        return List.copyOf(meta.lore());
    }

    /**
     * 判断物品是否可操作。
     */
    private boolean hasUsableItem(ItemStack item) {
        return item != null && item.getType() != Material.AIR;
    }

    /**
     * 按当前 lore 条目数计算操作费用。
     */
    private double resolveCost(int loreEntryCount) {
        int safeLoreEntryCount = Math.max(0, loreEntryCount);
        return resolveBaseCost() * (1D + safeLoreEntryCount * resolveLinearIncreaseMultiplier());
    }

    /**
     * 读取统一基础费用并兜底为合法默认值。
     */
    private double resolveBaseCost() {
        double configured = plugin.getConfig().getDouble(CONFIG_BASE_COST_PATH, DEFAULT_BASE_COST);
        return Double.isFinite(configured) && configured > 0D ? configured : DEFAULT_BASE_COST;
    }

    /**
     * 读取线性增加倍率并兜底为合法默认值。
     */
    private double resolveLinearIncreaseMultiplier() {
        double configured = plugin.getConfig().getDouble(
                CONFIG_LINEAR_INCREASE_MULTIPLIER_PATH,
                DEFAULT_LINEAR_INCREASE_MULTIPLIER
        );
        return Double.isFinite(configured) && configured >= 0D
                ? configured
                : DEFAULT_LINEAR_INCREASE_MULTIPLIER;
    }

    /**
     * 格式化费用回显。
     */
    private String formatCost(double cost) {
        return String.format(Locale.ROOT, "%.2f", cost);
    }

    /**
     * 写入 lore 模块默认配置项。
     */
    private void registerDefaults() {
        FileConfiguration config = plugin.getConfig();
        double baseCost = resolveDefaultBaseCost(config);

        config.addDefault(CONFIG_BASE_COST_PATH, baseCost);
        config.addDefault(CONFIG_LINEAR_INCREASE_MULTIPLIER_PATH, DEFAULT_LINEAR_INCREASE_MULTIPLIER);
        config.options().copyDefaults(true);
        plugin.saveConfig();
    }

    /**
     * 兼容旧配置中的费用项，统一迁移为基础费用默认值。
     */
    private double resolveDefaultBaseCost(FileConfiguration config) {
        if (config.isSet(CONFIG_BASE_COST_PATH)) {
            return positiveOrDefault(config.getDouble(CONFIG_BASE_COST_PATH, DEFAULT_BASE_COST));
        }
        if (config.isSet(LEGACY_ADD_COST_PATH)) {
            return positiveOrDefault(config.getDouble(LEGACY_ADD_COST_PATH, DEFAULT_BASE_COST));
        }
        if (config.isSet(LEGACY_EDIT_COST_PATH)) {
            return positiveOrDefault(config.getDouble(LEGACY_EDIT_COST_PATH, DEFAULT_BASE_COST));
        }
        if (config.isSet(LEGACY_REMOVE_COST_PATH)) {
            return positiveOrDefault(config.getDouble(LEGACY_REMOVE_COST_PATH, DEFAULT_BASE_COST));
        }
        return DEFAULT_BASE_COST;
    }

    /**
     * 将非法费用兜底为默认基础费用。
     */
    private double positiveOrDefault(double value) {
        return Double.isFinite(value) && value > 0D ? value : DEFAULT_BASE_COST;
    }

    /**
     * lore 操作类型。
     */
    private enum LoreOperation {
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

        LoreOperation(
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

        private LoreOperation shift(int direction) {
            LoreOperation[] operations = values();
            int index = (ordinal() + direction + operations.length) % operations.length;
            return operations[index];
        }

        private boolean requiresText() {
            return this != REMOVE;
        }
    }

    /**
     * 单个玩家打开的锻造 UI 状态。
     */
    private static class ForgeSession {
        private final AnvilView view;
        private final AnvilInventory inventory;
        private ItemStack originalInput;
        private LoreOperation operation;
        private String placeholderText;

        private ForgeSession(AnvilView view, AnvilInventory inventory, LoreOperation operation) {
            this.view = view;
            this.inventory = inventory;
            this.operation = operation;
            this.placeholderText = "";
        }

        private AnvilView view() {
            return view;
        }

        private AnvilInventory inventory() {
            return inventory;
        }

        private LoreOperation operation() {
            return operation;
        }

        private ItemStack originalInput() {
            return originalInput;
        }

        private void setOriginalInput(ItemStack originalInput) {
            this.originalInput = originalInput;
        }

        private void setOperation(LoreOperation operation) {
            this.operation = operation;
        }

        private String placeholderText() {
            return placeholderText;
        }

        private void setPlaceholderText(String placeholderText) {
            this.placeholderText = placeholderText;
        }
    }
}
