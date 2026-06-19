package org.tianjiserver.tianjicore.tianjicoreutil;

import net.milkbowl.vault.economy.Economy;
import net.milkbowl.vault.economy.EconomyResponse;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.plugin.RegisteredServiceProvider;

/**
 * Vault 金额操作工具。
 * 封装对接 Vault 的完整存取款流程。
 */
public final class VaultUtil {

    private static final double EPSILON = 1.0E-6D;
    private static final String VAULT_PLUGIN_NAME = "Vault";

    private VaultUtil() {
    }

    /**
     * 给玩家账户存入指定金额。
     */
    public static TransactionResult deposit(OfflinePlayer player, double amount) {
        TransactionResult invalidResult = validateRequest(player, amount);
        if (invalidResult != null) {
            return invalidResult;
        }

        if (!isVaultInstalled()) {
            return failure(
                    TransactionStatus.VAULT_NOT_FOUND,
                    "Vault 插件未安装或未加载",
                    amount,
                    Double.NaN,
                    Double.NaN
            );
        }

        Economy economy = resolveEconomy();
        if (economy == null) {
            return failure(
                    TransactionStatus.ECONOMY_PROVIDER_NOT_FOUND,
                    "未找到可用经济系统提供方",
                    amount,
                    Double.NaN,
                    Double.NaN
            );
        }

        if (!ensureAccount(economy, player)) {
            return failure(
                    TransactionStatus.ACCOUNT_CREATE_FAILED,
                    "玩家经济账户不存在且创建失败",
                    amount,
                    Double.NaN,
                    Double.NaN
            );
        }

        double before = economy.getBalance(player);
        EconomyResponse response = economy.depositPlayer(player, amount);
        double after = economy.getBalance(player);

        if (!response.transactionSuccess()) {
            return failure(
                    TransactionStatus.TRANSACTION_FAILED,
                    "存款失败: " + errorMessage(response),
                    amount,
                    before,
                    after
            );
        }

        return success("存款成功", amount, before, after);
    }

    /**
     * 从玩家账户扣除指定金额。
     */
    public static TransactionResult withdraw(OfflinePlayer player, double amount) {
        TransactionResult invalidResult = validateRequest(player, amount);
        if (invalidResult != null) {
            return invalidResult;
        }

        if (!isVaultInstalled()) {
            return failure(
                    TransactionStatus.VAULT_NOT_FOUND,
                    "Vault 插件未安装或未加载",
                    amount,
                    Double.NaN,
                    Double.NaN
            );
        }

        Economy economy = resolveEconomy();
        if (economy == null) {
            return failure(
                    TransactionStatus.ECONOMY_PROVIDER_NOT_FOUND,
                    "未找到可用经济系统提供方",
                    amount,
                    Double.NaN,
                    Double.NaN
            );
        }

        if (!ensureAccount(economy, player)) {
            return failure(
                    TransactionStatus.ACCOUNT_CREATE_FAILED,
                    "玩家经济账户不存在且创建失败",
                    amount,
                    Double.NaN,
                    Double.NaN
            );
        }

        double before = economy.getBalance(player);
        if (before + EPSILON < amount) {
            return failure(
                    TransactionStatus.INSUFFICIENT_FUNDS,
                    "余额不足，当前余额: " + before,
                    amount,
                    before,
                    before
            );
        }

        EconomyResponse response = economy.withdrawPlayer(player, amount);
        double after = economy.getBalance(player);

        if (!response.transactionSuccess()) {
            return failure(
                    TransactionStatus.TRANSACTION_FAILED,
                    "取款失败: " + errorMessage(response),
                    amount,
                    before,
                    after
            );
        }

        return success("取款成功", amount, before, after);
    }

    /**
     * 判断当前是否具备可用的 Vault + Economy 提供方。
     */
    public static boolean isAvailable() {
        return isVaultInstalled() && resolveEconomy() != null;
    }

    /**
     * 参数校验：玩家与金额格式。
     */
    private static TransactionResult validateRequest(OfflinePlayer player, double amount) {
        if (player == null) {
            return failure(
                    TransactionStatus.INVALID_PLAYER,
                    "玩家不能为空",
                    amount,
                    Double.NaN,
                    Double.NaN
            );
        }

        if (!Double.isFinite(amount) || amount <= 0D) {
            return failure(
                    TransactionStatus.INVALID_AMOUNT,
                    "金额必须为大于 0 的有效数字",
                    amount,
                    Double.NaN,
                    Double.NaN
            );
        }

        return null;
    }

    /**
     * 确保玩家已存在经济账户。
     */
    private static boolean ensureAccount(Economy economy, OfflinePlayer player) {
        if (economy.hasAccount(player)) {
            return true;
        }
        return economy.createPlayerAccount(player);
    }

    /**
     * 检查 Vault 插件是否已加载。
     */
    private static boolean isVaultInstalled() {
        return Bukkit.getPluginManager().getPlugin(VAULT_PLUGIN_NAME) != null;
    }

    /**
     * 从 Bukkit 服务管理器解析 Economy 实现。
     */
    private static Economy resolveEconomy() {
        RegisteredServiceProvider<Economy> registration =
                Bukkit.getServicesManager().getRegistration(Economy.class);
        if (registration == null) {
            return null;
        }
        return registration.getProvider();
    }

    /**
     * 提取可读错误信息，避免空字符串日志。
     */
    private static String errorMessage(EconomyResponse response) {
        if (response == null || response.errorMessage == null || response.errorMessage.isBlank()) {
            return "未知错误";
        }
        return response.errorMessage;
    }

    private static TransactionResult success(String message, double amount, double before, double after) {
        return new TransactionResult(TransactionStatus.SUCCESS, message, amount, before, after);
    }

    private static TransactionResult failure(
            TransactionStatus status,
            String message,
            double amount,
            double before,
            double after
    ) {
        return new TransactionResult(status, message, amount, before, after);
    }

    /**
     * 交易状态枚举。
     */
    public enum TransactionStatus {
        SUCCESS,
        INVALID_PLAYER,
        INVALID_AMOUNT,
        VAULT_NOT_FOUND,
        ECONOMY_PROVIDER_NOT_FOUND,
        ACCOUNT_CREATE_FAILED,
        INSUFFICIENT_FUNDS,
        TRANSACTION_FAILED
    }

    /**
     * 交易结果数据。
     */
    public record TransactionResult(
            TransactionStatus status,
            String message,
            double amount,
            double beforeBalance,
            double afterBalance
    ) {
        public boolean success() {
            return status == TransactionStatus.SUCCESS;
        }
    }
}
