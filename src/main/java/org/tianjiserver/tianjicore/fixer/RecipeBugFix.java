package org.tianjiserver.tianjicore.fixer;

import org.bukkit.Bukkit;
import org.bukkit.Keyed;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.inventory.Recipe;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.tianjiserver.tianjicore.TianjiCore;

/**
 * 配方同步修复模块。
 * 玩家加入时主动下发已收集的配方，规避部分客户端显示异常。
 */
public class RecipeBugFix implements Listener {

    // 启动阶段收集全量配方 key，避免每次玩家加入时重复遍历。
    List<NamespacedKey> allRecipeKeys;

    /**
     * 初始化时缓存当前服务端所有可识别配方。
     */
    public RecipeBugFix() {
        allRecipeKeys = new ArrayList<>();
        Iterator<Recipe> iterator = Bukkit.recipeIterator();
        while (iterator.hasNext()) {
            Recipe recipe = iterator.next();
            if (recipe instanceof Keyed) {
                NamespacedKey key = ((Keyed) recipe).getKey();
                allRecipeKeys.add(key);
            }
        }
        TianjiCore.getInstance().getLogger().info("Recipe collected");
        TianjiCore.getInstance().getLogger().info("RecipeBugFix feature is loaded");
    }

    /**
     * 玩家加入后补发已缓存配方，降低客户端配方状态不同步概率。
     */
    @EventHandler(ignoreCancelled = true)
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        try {
            // 统一为玩家解锁已收集配方，修复客户端“需管理员授予配方”的错误提示。
            player.discoverRecipes(allRecipeKeys);
            Bukkit.getLogger().info("[RecipeBugFix] Player " + player.getName() + " received recipe");
        } catch (Exception e) {
            Bukkit.getLogger().severe("[RecipeBugFix] " + e.getMessage());
        }
    }

}
