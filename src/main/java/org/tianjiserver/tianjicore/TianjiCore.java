package org.tianjiserver.tianjicore;

import org.bukkit.plugin.java.JavaPlugin;
import revxrsal.commands.bukkit.BukkitLamp;

/**
 * TianjiCore 插件主入口。
 * 负责初始化配置、命令系统与模块生命周期。
 */
public class TianjiCore extends JavaPlugin {

    private static TianjiCore instance;
    private TianjiCoreCommand commandHandler;

    /**
     * 插件启用入口：初始化配置、模块与命令注册。
     */
    @Override
    public void onEnable() {
        instance = this;

        saveDefaultConfig();

        commandHandler = new TianjiCoreCommand(this);
        commandHandler.bootstrap();

        var lamp = BukkitLamp.builder(this).build();
        lamp.register(commandHandler);

        getLogger().info("TianjiCore 已启动");
    }

    /**
     * 插件关闭入口：执行模块卸载流程。
     */
    @Override
    public void onDisable() {
        if (commandHandler != null) {
            commandHandler.shutdown();
        }
    }

    /**
     * 提供全局插件实例访问入口。
     */
    public static TianjiCore getInstance() {
        return instance;
    }
}
