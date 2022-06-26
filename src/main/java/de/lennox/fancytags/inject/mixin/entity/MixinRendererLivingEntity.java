/*
 * Copyright (c) 2021 Lennox
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated
 * documentation files (the "Software"), to deal in the Software without restriction, including without limitation the
 * rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software,
 * and to permit persons to whom the Software is furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or substantial portions
 * of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED,
 * INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A
 * PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT
 * HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF
 * CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE
 * USE OR OTHER DEALINGS IN THE SOFTWARE.
 */
package de.lennox.fancytags.inject.mixin.entity;

import de.lennox.fancytags.render.LivingLabelRenderer;
import net.minecraft.client.renderer.entity.RendererLivingEntity;
import net.minecraft.entity.EntityLivingBase;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(RendererLivingEntity.class)
public abstract class MixinRendererLivingEntity<T extends EntityLivingBase> {

  private final LivingLabelRenderer<T> livingLabelRenderer = new LivingLabelRenderer<>();

  @Shadow
  protected abstract boolean canRenderName(T entity);

  /**
   * @author Lennox
   * @reason Overwrite the normal labels to render custom ones
   */
  @Overwrite
  public void renderName(T entity, double x, double y, double z) {
    // Check if the name of the entity should be rendered
    if (canRenderName(entity)) {
      livingLabelRenderer.prepare(entity, x, y, z);
      livingLabelRenderer.renderLabel(entity);
      livingLabelRenderer.finish();
    }
  }


}
