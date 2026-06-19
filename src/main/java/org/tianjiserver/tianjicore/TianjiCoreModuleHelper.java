package org.tianjiserver.tianjicore;

import java.util.List;

/**
 * 模块管理辅助类。
 * 负责模块操作的封装，向命令层提供简洁的接口。
 */
class TianjiCoreModuleHelper {

    private final TianjiCoreModuleManager moduleManager;

    TianjiCoreModuleHelper(TianjiCore plugin) {
        this.moduleManager = new TianjiCoreModuleManager(plugin);
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

    /**
     * 指定模块当前是否启用
     */
    boolean isModuleEnabled(String moduleInput) {
        return moduleManager.isModuleEnabled(moduleInput);
    }
}
