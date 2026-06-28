package org.tianjiserver.tianjicore;

import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.entity.Player;
import org.tianjiserver.tianjicore.itemloreandsignature.ItemLoreAndSignature;

import java.util.List;

/**
 * 模块管理辅助类。
 * 负责模块操作的封装，向命令层提供简洁的接口。
 */
class TianjiCoreModuleHelper {

    private final ItemLoreAndSignature itemLoreAndSignature;
    private final TianjiCoreModuleManager moduleManager;
    private final MiniMessage mini = MiniMessage.miniMessage();

    TianjiCoreModuleHelper(TianjiCore plugin) {
        this.itemLoreAndSignature = new ItemLoreAndSignature(plugin);
        this.moduleManager = new TianjiCoreModuleManager(plugin, itemLoreAndSignature);
    }

    /**
     * 启动模块系统。
     */
    void bootstrap() {
        moduleManager.bootstrap();
    }

    /**
     * 关闭模块系统。
     */
    void shutdown() {
        moduleManager.shutdown();
    }

    /**
     * 打开物品 lore 锻造界面。
     */
    void openItemLoreForge(Player player) {
        if (!moduleManager.isModuleEnabled(ItemLoreAndSignature.MODULE_KEY)) {
            player.sendMessage(mini.deserialize("<red>物品签名锻造模块未启用"));
            return;
        }

        itemLoreAndSignature.openForgeUi(player);
    }

    /**
     * 切换（开关）指定模块
     */
    TianjiCoreModuleManager.ToggleResult toggleModule(String moduleInput) {
        return moduleManager.toggle(moduleInput);
    }

    /**
     * 重载指定模块或插件
     */
    TianjiCoreModuleManager.ReloadResult reloadModule(String moduleInput) {
        return moduleManager.reload(moduleInput);
    }

    /**
     * 获取所有可切换模块的键
     */
    List<String> getToggleableModuleKeys() {
        return moduleManager.getToggleableModuleKeys();
    }

    /**
     * 获取所有模块的键
     */
    List<String> getModuleKeys() {
        return moduleManager.getModuleKeys();
    }

    /**
     * 获取插件重载的目标参数名
     */
    String getReloadPluginTarget() {
        return moduleManager.getReloadPluginTarget();
    }

}
