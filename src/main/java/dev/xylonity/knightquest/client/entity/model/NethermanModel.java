package dev.xylonity.knightquest.client.entity.model;

import com.geckolib.renderer.base.GeoRenderState;
import dev.xylonity.knightquest.KnightQuestCommon;
import dev.xylonity.knightquest.common.entity.boss.NethermanEntity;
import net.minecraft.resources.Identifier;

/**
 * GeckoLib 5 model for the Netherman boss.
 * <p>
 * Since 26.1.2-1.9.5 the boss uses the CLASSIC (1.5.0-era) model and texture art for all
 * three phases: the redesigned 1.9.x part-sheet textures left the boss mostly black in
 * game (their painted regions do not line up with the redesigned phase models' UVs), and
 * the user-facing reference look is the classic stone-armour + gradient-legs art. The
 * classic model shares its bone names (main/Body/head/LeftArm/RightArm/LeftLeg/RightLeg)
 * with the per-phase animation files, so the existing phase animations keep driving it;
 * keyframes for the redesigned-only bones (IceArm, MagicArm, bone variants, bipedBody)
 * are skipped by GeckoLib. The dissolve transition frames only exist for the redesigned
 * layout, so the phase switch now holds the previous phase's texture until the switch
 * counter completes (same visual pacing, no 128x128 dissolve sheets).
 */
public class NethermanModel extends KQEntityModel<NethermanEntity> {

    private static final String TEXTURE_PATH = "textures/entity/";

    public NethermanModel() {
        super("netherman");
    }

    @Override
    public void addAdditionalStateData(NethermanEntity animatable, Object renderData, GeoRenderState renderState) {
        super.addAdditionalStateData(animatable, renderData, renderState);

        // Model: the classic geometry for every phase
        renderState.addGeckolibData(MODEL_OVERRIDE, Identifier.fromNamespaceAndPath(KnightQuestCommon.MOD_ID, "entity/netherman_classic"));

        // Texture: classic art per phase (hold the previous texture while the switch
        // counter runs, matching the old crossfade pacing without the dissolve sheets)
        String texture;
        if (animatable.isDeadOrDying()) {
            texture = "netherman_classic_magic";
        } else {
            texture = switch (animatable.getPhase()) {
                case 1 -> "netherman_classic_fire";
                case 2 -> animatable.getCounterSwitchPhase2() == 130 ? "netherman_classic_ice" : "netherman_classic_fire";
                default -> animatable.getCounterSwitchPhase3() == 160 ? "netherman_classic_magic" : "netherman_classic_ice";
            };
        }
        renderState.addGeckolibData(TEXTURE_OVERRIDE, Identifier.fromNamespaceAndPath(KnightQuestCommon.MOD_ID, TEXTURE_PATH + texture + ".png"));
    }

    @Override
    public Identifier getAnimationResource(NethermanEntity animatable) {
        if (animatable.isDeadOrDying()) {
            return Identifier.fromNamespaceAndPath(KnightQuestCommon.MOD_ID, "entity/netherman_magic");
        }

        if (animatable.getCounterSwitchPhase2() == 130 && animatable.getPhase() == 2
                || (animatable.getCounterSwitchPhase3() != 160) && animatable.getPhase() == 3) {
            return Identifier.fromNamespaceAndPath(KnightQuestCommon.MOD_ID, "entity/netherman_ice");
        } else if (animatable.getCounterSwitchPhase3() == 160 && animatable.getPhase() == 3) {
            return Identifier.fromNamespaceAndPath(KnightQuestCommon.MOD_ID, "entity/netherman_magic");
        }

        return Identifier.fromNamespaceAndPath(KnightQuestCommon.MOD_ID, "entity/netherman_fire");
    }
}
