package org.tianjiserver.tianjicore.feature;

import org.bukkit.entity.EntityType;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.CreatureSpawnEvent;

/**
 * 幻翼生成拦截模块。
 */
public class PhantomSpawnBlocker implements Listener {

  // 在高优先级阶段拦截幻翼生成事件。
  @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
  public void onCreatureSpawn(CreatureSpawnEvent event) {
    if (event.getEntityType() == EntityType.PHANTOM) {
      event.setCancelled(true);
    }
  }
}
