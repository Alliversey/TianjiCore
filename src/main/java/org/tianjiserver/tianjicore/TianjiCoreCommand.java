package org.tianjiserver.tianjicore;

import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.tianjiserver.tianjicore.itemloreandsignature.ItemLoreAndSignature;
import revxrsal.commands.annotation.Command;
import revxrsal.commands.annotation.Subcommand;
import revxrsal.commands.bukkit.annotation.CommandPermission;


/**
 * TianjiCore 命令入口。
 * 负责对接命令框架，并将模块操作委托给模块辅助层。
 */
@Command({"tianjicore", "tc"})
public class TianjiCoreCommand {

    private final TianjiCoreModuleHelper moduleHelper;
    private final ItemLoreAndSignature itemLoreAndSignature;
    private final MiniMessage mini = MiniMessage.miniMessage();

    public TianjiCoreCommand(TianjiCore plugin) {
        this.moduleHelper = new TianjiCoreModuleHelper(plugin);
        this.itemLoreAndSignature = new ItemLoreAndSignature(plugin);
    }

    /**
     * 插件启动时初始化模块管理器。
     */
    public void bootstrap() {
        moduleHelper.bootstrap();
    }

    /**
     * 插件关闭时执行模块下线流程。
     */
    public void shutdown() {
        moduleHelper.shutdown();
    }

    /**
     * 开关指定模块（仅允许可切换模块）。
     */
    @CommandPermission("tianjicore.command.admin")
    @Subcommand("toggle")
    public void handleToggleCommand(CommandSender sender, String moduleInput) {
        var result = moduleHelper.toggleModule(moduleInput);

        switch (result.status()) {
            case UNKNOWN_MODULE:
                sender.sendMessage(mini.deserialize("<red>未知模块: " + moduleInput));
                sender.sendMessage(mini.deserialize(
                        "<yellow>可开关模块: <aqua>" + String.join(", ", moduleHelper.getToggleableModuleKeys())));
                break;

            case NOT_TOGGLEABLE:
                var moduleInfo = result.moduleInfo();
                sender.sendMessage(mini.deserialize(
                        "<red>该模块不支持 toggle: " + moduleInfo.key()));
                sender.sendMessage(mini.deserialize(
                        "<yellow>可开关模块: <aqua>" + String.join(", ", moduleHelper.getToggleableModuleKeys())));
                break;

            case FAILED:
                moduleInfo = result.moduleInfo();
                sender.sendMessage(mini.deserialize(
                        "<red>模块切换失败: " + moduleInfo.displayName()));
                break;

            case SUCCESS:
                moduleInfo = result.moduleInfo();
                sender.sendMessage(mini.deserialize(
                        "<green>" + moduleInfo.displayName() + " 已" + (moduleInfo.enabled() ? "开启" : "关闭")));
                break;
        }
    }

    /**
     * 重载指定模块，或执行插件级整体重载。
     */
    @CommandPermission("tianjicore.command.admin")
    @Subcommand("reload")
    public void handleReloadCommand(CommandSender sender, String moduleInput) {
        var result = moduleHelper.reloadModule(moduleInput);

        switch (result.status()) {
            case SUCCESS_PLUGIN:
                sender.sendMessage(mini.deserialize("<green>插件与模块配置已重载"));
                break;

            case UNKNOWN_MODULE:
                sender.sendMessage(mini.deserialize("<red>未知模块: " + moduleInput));
                sender.sendMessage(mini.deserialize(
                        "<yellow>可用模块: <aqua>" + String.join(", ", moduleHelper.getModuleKeys())));
                break;

            case FAILED:
                var moduleInfo = result.moduleInfo();
                sender.sendMessage(mini.deserialize(
                        "<red>模块重载失败: " + moduleInfo.displayName()));
                break;

            case SUCCESS_MODULE:
                moduleInfo = result.moduleInfo();
                sender.sendMessage(mini.deserialize(
                        "<green>" + moduleInfo.displayName() + " 已重载，当前状态: "
                                + (moduleInfo.enabled() ? "<green>开启" : "<red>关闭")));
                break;
        }
    }


    /**
     * 打开锻造 UI，允许玩家为手中物品追加一行 lore。
     */
    @CommandPermission("tianjicore.command.forge")
    @Subcommand("forge")
    public void handleForgeCommand(Player player) {
        if (!moduleHelper.isModuleEnabled("itemloreandsignature")) {
            player.sendMessage(mini.deserialize("<red>物品签名锻造模块未启用"));
            return;
        }
        itemLoreAndSignature.openForgeUi(player);
    }

    /**
     * 输出当前可用子命令与模块参数提示。
     */
    @Subcommand("help")
    public void handleHelpCommand(CommandSender sender) {
        sender.sendMessage(mini.deserialize("<yellow>命令帮助:"));
        sender.sendMessage(mini.deserialize("<gray>/tc forge <white>打开锻造铁砧，为物品添加一行 lore"));
        sender.sendMessage(mini.deserialize("<gray>/tc toggle <module> <white>开关指定模块"));
        sender.sendMessage(mini.deserialize("<gray>/tc reload <module|plugin> <white>重载插件或指定模块"));
        sender.sendMessage(mini.deserialize("<gray>可开关模块: <aqua>" + String.join(", ", moduleHelper.getToggleableModuleKeys())));
        sender.sendMessage(mini.deserialize("<gray>可重载模块: <aqua>" + String.join(", ", moduleHelper.getModuleKeys())));
        sender.sendMessage(mini.deserialize("<gray>插件重载参数: <aqua>" + moduleHelper.getReloadPluginTarget()));
    }
}
