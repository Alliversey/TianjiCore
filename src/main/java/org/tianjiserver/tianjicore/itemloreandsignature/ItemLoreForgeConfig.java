package org.tianjiserver.tianjicore.itemloreandsignature;

import org.bukkit.configuration.file.FileConfiguration;
import org.tianjiserver.tianjicore.TianjiCore;

import java.util.Locale;

/**
 * 管理物品 lore 锻造配置与费用计算。
 */
final class ItemLoreForgeConfig {

    private static final String CONFIG_BASE_COST_PATH = "itemloreandsignature.forge.cost";
    private static final String CONFIG_LINEAR_INCREASE_MULTIPLIER_PATH =
            "itemloreandsignature.forge.linear-increase-multiplier";
    private static final String LEGACY_ADD_COST_PATH = "itemloreandsignature.forge.add.cost";
    private static final String LEGACY_EDIT_COST_PATH = "itemloreandsignature.forge.edit.cost";
    private static final String LEGACY_REMOVE_COST_PATH = "itemloreandsignature.forge.remove.cost";

    private static final double DEFAULT_BASE_COST = 1000.0D;
    private static final double DEFAULT_LINEAR_INCREASE_MULTIPLIER = 1.5D;

    private final TianjiCore plugin;

    ItemLoreForgeConfig(TianjiCore plugin) {
        this.plugin = plugin;
    }

    /**
     * 写入 lore 模块默认配置项。
     */
    void registerDefaults() {
        FileConfiguration config = plugin.getConfig();
        double baseCost = resolveDefaultBaseCost(config);

        config.addDefault(CONFIG_BASE_COST_PATH, baseCost);
        config.addDefault(CONFIG_LINEAR_INCREASE_MULTIPLIER_PATH, DEFAULT_LINEAR_INCREASE_MULTIPLIER);
        config.options().copyDefaults(true);
        plugin.saveConfig();
    }

    /**
     * 按当前 lore 条目数计算操作费用。
     */
    double resolveCost(int loreEntryCount) {
        int safeLoreEntryCount = Math.max(0, loreEntryCount);
        return resolveBaseCost() * (1D + safeLoreEntryCount * resolveLinearIncreaseMultiplier());
    }

    /**
     * 格式化费用回显。
     */
    String formatCost(double cost) {
        return String.format(Locale.ROOT, "%.2f", cost);
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
}
