# PORTING.md — Knight Quest: NeoForge 1.21.1 → NeoForge 26.1.2

Memoria de trabajo del port. **Léer siempre al empezar o retomar sesión.**

## Estado actual (2026-11-15, sesión 2 — partículas, Netherman, drops, creeper)

Fase 7 (pruebas/ajustes visuales) sobre el port funcional de la sesión 1. Esta sesión
corrige los 6 issues reportados por el usuario con capturas. Compilación en curso.

### Hecho en la sesión 2

1. **Partículas de eldbomb/gremlin en blanco** (`2026-11-14_17.37.37/38.png`): las 6
   clases de partículas del port usaban `SingleQuadParticle.Layer.OPAQUE_TERRAIN` /
   `TRANSLUCENT_TERRAIN` — el **atlas de terreno/bloques**, que no contiene los sprites
   de `knightquest` → sprite ausente → quad blanco. El enum real de 26.1.2 (leído del
   client jar, `SingleQuadParticle$Layer`) es un record con instancias `OPAQUE` /
   `TRANSLUCENT` (atlas de partículas; `OPAQUE_PARTICLE`/`TRANSLUCENT_PARTICLE` son nombres de
   RenderPipelines, no del enum). Fix: `PoisonParticle`,
   `PoisonCloudParticle`, `GhostyParticle`, `SnowflakeParticle` → `Layer.OPAQUE`;
   `GremlinParticle`, `YellowParticle` → `Layer.TRANSLUCENT`. (1.21.1:
   `PARTICLE_SHEET_OPAQUE`/`PARTICLE_SHEET_TRANSLUCENT`.)
   - Nota: el humo del ratman se veía bien porque usa `ParticleTypes.CAMPFIRE_COSY_SMOKE`
     vanilla (no afectado). El eldbomb suelta `POISON_PARTICLE`×6 + `POISON_CLOUD_PARTICLE`×3
     (código de `EldBombEntity.explode`), ambos corregidos.

2. **Música del Netherman**: el port tocaba `playSound(RECORDS)` posicional cada
   `177*20` ticks → empezaba a los ~3,5 min (cuando el boss ya estaba en fase 3) y
   quedaba anclado al punto de invocación mientras el boss se alejaba. KnightLib
   original (`AbstractLoopSound` + `BossMusicManager`): música **global**
   (`relative = true`, `Attenuation.NONE`), en bucle, fade in/out de 40 ticks, que
   arranca en cuanto existe el boss (fase 1) y se pausa cuando el boss muere o el
   oyente supera 64 bloques. Fix: nueva clase cliente
   `client/sound/NethermanBossMusicSound` + `client/sound/KnightQuestBossMusicManager`
   (un `AbstractTickableSoundInstance` global por boss, `ClientTickEvent.Post`,
   reinicia al volver a cumplirse la condición). Eliminado el `playSound` del tick
   servidor. APIs verificadas en el client jar 26.1.2: constructor
   `AbstractTickableSoundInstance(SoundEvent, SoundSource, RandomSource)`,
   `SoundInstance.createUnseededRandom()`, `Attenuation.NONE`, `canStartSilent()`,
   `stop()`.

3. **Skin fase 3 del Netherman negra/corrupta**: los assets (geo fire/ice/magic +
   texturas + animaciones) son byte-idénticos al origen y el switching por
   `DataTicket` (`MODEL_OVERRIDE`/`TEXTURE_OVERRIDE` en `NethermanModel`) replica al
   origen. La diferencia real: el renderer 1.21.1 usaba
   `RenderType.entityTranslucent` y el port usaba el cutout por defecto de GeckoLib 5.
   Las hojas de textura del boss son "part sheets" mayormente translúcidas: con cutout
   las piezas semitransparentes de la túnica se hornearon **negro sólido** (con
   `entityTranslucent` se funden, el look fantasmal original). Fix: override
   `getRenderType(renderState, texture)` → `RenderTypes.entityTranslucent(texture)` en
   `NethermanRenderer` y `NethermanCloneRenderer` (26.1.2: `RenderTypes.entityTranslucent(Identifier)`
   verificado por decompilación de la clase vanilla).

4. **Casco creeper con "zonas transparentes" (ojos/boca)**: la textura es idéntica en
   origen y destino (ojos/boca negros opacos) — la imagen de referencia del usuario
   (`armaduras knight quest original.png`) muestra ojos/boca **dorados** (píxeles
   emisivos que su shader pack hacía brillar en 1.21.1). Fix: recoloreados los 44
   píxeles negros del patrón de cara a dorado mostaza `(210, 170, 65)` en las 4
   texturas del set (`textures/models/armor/creeper_layer_1/2.png` y sus copias de
   `textures/entity/equipment/humanoid(leggings)/creeper.png`, que son las que
   renderiza 26.1.2). El borde inferior del casco se mantiene negro.

5. **Los mobs no soltan esencias al morir**: KnightLib lo hacía con un Global Loot
   Modifier (`knightlib:essence_all_hostiles` → 12,5 % esencia pequeña + 5 % esencia
   grande, solo `MobCategory.MONSTER`, tabla `entities/`, blacklist por config). El
   port tenía la clase `KQAddItemModifier` y el provider datagen, pero
   `src/generated/resources` estaba vacío (el datagen nunca corrió) → **cero GLM en el
   jar** (ni siquiera los de ojo de ratman/escama de lizzy). Fix triple:
   - Rama de esencias en `KQAddItemModifier.doApply` (mismas reglas que KnightLib;
     chances desde config con los valores originales 0.125/0.05, blacklist
     `ESSENCE_ENTITY_BLACKLIST` nueva en `KQConfigValues`).
   - JSONs escritos a mano en `data/knightquest/loot_modifiers/`
     (`essence_all_hostiles.json`, `entities_ratman_ratman_eye.json`,
     `entities_lizzy_lizzy_scale.json`). IMPORTANTE (26.1.2): el índice
     `data/neoforge/loot_modifiers/global_loot_modifiers.json` YA NO EXISTE —
     `LootModifierManager` es un `SimpleJsonResourceReloadListener` sobre la carpeta
     `loot_modifiers` y carga cada JSON directamente como modificador; el runServer
     validó el parseo (0 errores).
   - El runServer valida el parseo de los GLM (errores de codec se loguean alto).

6. **Nomenclatura del jar**: `base.archivesName = "${mod_id}-neoforge-${minecraft_version}"`
   → `knightquest-neoforge-26.1.2-1.9.4.jar`.

### Hecho en la sesión 1 (1.9.4, re-aplicado tras reciclar el sandbox)

1. 7 `equipment/*.json` con listas de capa vacías (`chainmail`, `tengu`,
   `tunic_{blue,green,red,sea,yellow}`) — el codec `EquipmentClientInfo` de 26.1.2
   rechaza listas vacías y el fichero entero dejaba de parsear (armadura sin
   textura). Capas vacías omitidas (vanilla-style). Verificado contra el client jar
   vanilla 26.1.2.
2. 5 modelos geo de piezas de armadura movidos de `geckolib/models/entity/` a
   `geckolib/models/item/` (`forze/silver/spider/veteran_chestplate` +
   `veteran_leggings`): `DefaultedItemGeoModel` resuelve `ns:item/<pieza>` en
   `BakedModelCache`; el MISS renderizaba el cubo MISSINGNO (piernas magenta del
   FallenKnight) y spameaba `Unable to find model` (GeckoLib 5.5.2, confirmado con
   CFR).
3. 5 mcmeta de texturas animadas con un frame de más (bug heredado del 1.21.1):
   índices inválidos recortados.
4. Auditoría de paridad por script: lang 10/10, sounds.json completo, 0 assets/data
   faltantes, 44 materiales ↔ 44 equipment JSON. Conclusión: paridad 1:1 salvo los
   items KnightLib documentados y `momma_lizzy` (código muerto en el origen).

## Cambios de API encontrados (1.21.1 → 26.1.2)

| Tema | 1.21.1 | 26.1.2 |
| --- | --- | --- |
| Equipment assets | `textures/models/armor/<mat>_layer_N.png` en código | JSON `assets/<ns>/equipment/<asset>.json` + `textures/entity/equipment/<layer>/`; **capa sin textura se OMITE, nunca lista vacía** |
| Item models | `models/item/*.json` | + definiciones `items/*.json` (`ClientItem.CODEC`) |
| GeckoLib 5 rutas item/armor | `geo/<key>.geo.json` explícito | convención `geckolib/models/item/<key>.geo.json`; caché keyed `ns:item/<key>` |
| GeckoLib 5 rutas entity | `geo/<key>.geo.json` | `geckolib/models/entity/<key>.geo.json`; caché keyed `ns:entity/<key>` |
| Render type entidad (v4 `getRenderType(animatable, texture, ...)`) | `RenderType.entityTranslucent(rl)` | override `getRenderType(RenderState, Identifier)` → `RenderTypes.entityTranslucent(Identifier)` |
| Partículas (`ParticleRenderType`) | `PARTICLE_SHEET_OPAQUE`/`PARTICLE_SHEET_TRANSLUCENT` | `SingleQuadParticle.Layer.OPAQUE`/`TRANSLUCENT` (los `*_TERRAIN` usan el atlas de bloques → sprites ausentes → quads blancos) |
| Texturas animadas | frame de más tolerado con warning | igual (warning `Invalid frame index`), corregido |
| Boss music | KnightLib `IBossMusicProvider` (global loop + fade) | reimplementado cliente: `AbstractTickableSoundInstance` global + manager en `ClientTickEvent.Post` |
| Global Loot Modifiers | serde índice `data/neoforge/loot_modifiers/global_loot_modifiers.json` + condición `loot_table` | **sin índice**: cada JSON de `loot_modifiers/` ES el modificador (cargado por ruta); condición con campo `loot_table_id`; `LootContext.getParamOrNull` → `getOptionalParameter`; `AbstractTickableSoundInstance.stop()` es protected |

## Notas de build en el sandbox (4 GB RAM, sin swap, no-root)

1. Entorno: `_JAVA_OPTIONS="-Xmx2000m -XX:+UseSerialGC"`,
   `GRADLE_OPTS="-Dorg.gradle.jvmargs=-Xmx1024m -XX:+UseSerialGC
   -Dorg.gradle.parallel=false"`, `JAVA_HOME` = JDK 21 (launcher); el toolchain Java
   25 lo descarga foojay.
2. La descompilación NFRT/Vineflower OOMea (NFRT la lanza con `-Xmx4096m` fijo).
   Estrategia: descompilar manualmente con memoria controlada e **inyectar el
   resultado por caché**:
   - Bulk: Vineflower 1.11.2 con flags de NFRT pero SIN `--include-classpath`,
     `-Xmx2600m -XX:+UseSerialGC
     -Djava.util.concurrent.ForkJoinPool.common.parallelism=1` → ~25.631 fuentes,
     solo 2 stubs (`Blocks.<clinit>`, `BlockStateData.bootstrap3_2`).
   - Quirúrgico: esas 2 clases solas con `-Xmx3000m` → fuentes completas.
   - `Blocks.java` quirúrgico retocado para que el patch de NeoForge aplique
     (`BlockBehaviour.Properties.of()` + bloque `static {}` for-each).
   - Splice al jar bulk → `decompile_output_fixed.jar`.
   - Wrapper `java`/`javac` (heap ≤2300m, SerialGC, inyección del output al paso
     `decompile`) en un JDK 25 enlazado (`tools/jdk25-wrapped`, seleccionado con
     `-Porg.gradle.java.installations.paths=...`).
   - Invalidar `~/.gradle/caches/neoformruntime/intermediate_results/{decompile,patch}_*`
     tras cambiar el contenido inyectado.
3. Tras el primer éxito, el artefacto parcheado queda cacheado: builds siguientes en
   segundos. Scripts persistidos en `/home/z/my-project/scripts/`.

## Errores abiertos

- Ninguno conocido del lado del mod. Los `hs_err_pid*.log` del run/ de Windows son
  crashes de memoria de la máquina del usuario, no del mod.

## Pendiente

- [ ] Verificación visual del usuario en Windows (`runClient`) — checklist:
      1) partículas de eldbomb/gremlin en verde (no blancas); 2) música del Netherman
      arranca con la invocación (fase 1), suena global y se pausa lejos; 3) fase 3
      con túnica translúcida (no negra sólida); 4) casco creeper con ojos/boca
      dorados; 5) esencias pequeñas/grandes caen de cualquier hostil (12,5 %/5 %)
      y ojo de ratman/escama de lizzy; 6) armaduras equipadas renderizan (fixes
      sesión 1).
- [ ] Opcional: `runData` para regenerar los GLM JSONs con datagen (los actuales son
      escritos a mano y funcionales).
