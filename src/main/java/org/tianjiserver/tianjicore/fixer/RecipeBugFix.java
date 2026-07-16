package org.tianjiserver.tianjicore.fixer;

import org.bukkit.Bukkit;
import org.bukkit.Keyed;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.server.ServerLoadEvent;
import org.bukkit.inventory.Recipe;
import org.tianjiserver.tianjicore.TianjiCore;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.logging.Level;

/**
 * 配方同步修复模块。
 * 玩家加入时主动下发已收集的配方，规避部分客户端显示异常。
 */
public class RecipeBugFix implements Listener {

    private final TianjiCore plugin;
    private List<NamespacedKey> allRecipeKeys = List.of();

    /**
     * 初始化时缓存当前服务端所有可识别配方。
     */
    public RecipeBugFix() {
        plugin = TianjiCore.getInstance();
        refreshRecipeKeys();
        plugin.getLogger().info("RecipeBugFix 模块已加载");
    }

    /**
     * 服务端及其他插件完成加载后重新收集配方。
     */
    @EventHandler
    public void onServerLoad(ServerLoadEvent event) {
        refreshRecipeKeys();
    }

    /**
     * 刷新当前服务端全部带命名空间的配方 key。
     */
    private void refreshRecipeKeys() {
        List<NamespacedKey> recipeKeys = new ArrayList<>();
        Iterator<Recipe> iterator = Bukkit.recipeIterator();
        while (iterator.hasNext()) {
            Recipe recipe = iterator.next();
            if (recipe instanceof Keyed keyed) {
                recipeKeys.add(keyed.getKey());
            }
        }
        allRecipeKeys = List.copyOf(recipeKeys);
        plugin.getLogger().info("RecipeBugFix 已收集 " + allRecipeKeys.size() + " 个配方");
    }

    /**
     * 玩家加入后补发已缓存配方，降低客户端配方状态不同步概率。
     */
    @EventHandler(ignoreCancelled = true)
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        // 等待服务端完成原生登录同步，避免补发结果被后续登录数据覆盖。
        Bukkit.getScheduler().runTask(plugin, () -> synchronizeRecipes(player));
    }

    /**
     * 为已完成登录的玩家补发服务端配方。
     */
    private void synchronizeRecipes(Player player) {
        if (!player.isOnline()) {
            return;
        }

        try {
            // 统一为玩家解锁已收集配方，修复客户端“需管理员授予配方”的错误提示。
            int discoveredRecipeCount = player.discoverRecipes(allRecipeKeys);
            plugin.getLogger().info(
                    "RecipeBugFix 已为玩家 " + player.getName()
                            + " 同步 " + allRecipeKeys.size() + " 个配方，其中新解锁 "
                            + discoveredRecipeCount + " 个"
            );
        } catch (Exception exception) {
            plugin.getLogger().log(
                    Level.SEVERE,
                    "RecipeBugFix 为玩家 " + player.getName() + " 同步配方失败",
                    exception
            );
        }
    }

}
