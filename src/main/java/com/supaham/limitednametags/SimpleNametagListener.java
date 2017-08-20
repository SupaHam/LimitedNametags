package com.supaham.limitednametags;

import com.google.common.base.Preconditions;

import org.bukkit.entity.Entity;

/**
 * Simple implementation of {@link NametagListener}.
 */
public abstract class SimpleNametagListener<T extends Entity> implements NametagListener<T> {

  private final Class<T> clazz;

  /**
   * Constructs a new {@link SimpleNametagListener} with a type extending entity.
   *
   * @param entityClazz class of the entity to care for
   */
  public SimpleNametagListener(Class<T> entityClazz) {
    Preconditions.checkNotNull(entityClazz, "entity class cannot be null.");
    this.clazz = entityClazz;
  }

  @Override
  public void onEnable(NametagManager manager) {
  }

  @Override
  public void onReload(NametagManager manager) {
  }

  @Override
  public void onDisable(NametagManager manager) {
  }

  @Override
  public Class<T> getClassType() {
    return this.clazz;
  }
}
