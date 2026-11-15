package dev.xylonity.knightquest.client.entity.renderer;

import com.geckolib.renderer.GeoEntityRenderer;
import dev.xylonity.knightquest.client.entity.model.NethermanCloneModel;
import dev.xylonity.knightquest.common.entity.boss.NethermanCloneEntity;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.resources.Identifier;

/**
 * GeckoLib 5 renderer for NethermanCloneEntity.
 * <p>
 * Port note: the v4 render() scaling is applied via {@code withScale}; the v4
 * setCustomAnimations head tracking is not yet ported (GeckoLib 5 bone adjustment needs
 * the runtime-injected GeoRenderState; see README).
 * <p>
 * {@code entityTranslucent} render type ported from the 1.21.1 renderer (the clone texture
 * is a semi-transparent part sheet like the boss's).
 */
public class NethermanCloneRenderer extends GeoEntityRenderer<NethermanCloneEntity, EntityRenderState> {

    public NethermanCloneRenderer(EntityRendererProvider.Context renderManager) {
        super(renderManager, new NethermanCloneModel());

        this.withScale(1.2f, 1.2f);
    }

    @Override
    public RenderType getRenderType(EntityRenderState renderState, Identifier texture) {
        return RenderTypes.entityTranslucent(texture);
    }
}
