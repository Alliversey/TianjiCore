package org.tianjiserver.tianjicore.fixer;

import org.bukkit.Material;
import org.bukkit.entity.Enderman;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityChangeBlockEvent;

/**
 * 末影人蘑菇搬运修复模块。
 * 拦截末影人携带蘑菇相关方块，避免异常行为引发问题。
 */
public class EndermanMushroomBugFix implements Listener {

  // 当末影人尝试放置方块时，检查并拦截蘑菇相关类型。
  @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
  public void onEndermanPutMushroom(EntityChangeBlockEvent event) {
    if (!(event.getEntity() instanceof Enderman enderman)) {
      return;
    }
    if (isMushroom(event.getTo()) || isMushroom(event.getBlockData().getMaterial())) {
      event.setCancelled(true);
      enderman.setCarriedBlock(null);
    }
  }

  private boolean isMushroom(Material type) {
    // 覆盖普通蘑菇与下界菌类。
    return type == Material.BROWN_MUSHROOM
        || type == Material.RED_MUSHROOM
        || type == Material.CRIMSON_FUNGUS
        || type == Material.WARPED_FUNGUS;
  }
}
