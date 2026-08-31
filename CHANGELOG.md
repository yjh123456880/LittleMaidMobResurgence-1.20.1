# Changelog

All notable changes to **LittleMaidMobResurgence (小女仆：归来)** are documented in this file.

## [0.11] - 2026-08-31

Initial public release of **LittleMaidMobResurgence (小女仆：归来)** for Minecraft 1.20.1 (Forge).

### Added

- Maid Stick reworked into the Work Range / Maid Binder: bind a work range on the top face of a block, switch between "bind range" and "bind maid" modes, shift+right-click to clear, with an orange range ring and directional arrow preview.
- Maid Capture Egg: right-click your contracted maid to store her (full NBT preserved), right-click a block to release; empty/filled dual textures; the item drop glows, is indestructible and never despawns.
- Maid Souvenir: a maid drops a glowing, unbreakable, permanent souvenir on death; right-click a block to revive her at 1 HP in the Rest state, with mood restored to at least 10 and no leftover rebellion.
- Rest state: maids below 5% HP with no nearby enemies periodically sit down and recover until they reach 50% HP.
- Evade state (battle modes only): below 5% HP with enemies nearby, the maid keeps away from hostile targets until HP recovers above 30%; after 10s without enemies she switches to Rest.
- Force chunk loading: a beacon button in the maid GUI keeps the maid's chunk always loaded.
- Leash detection: leashed maids complain with mood-specific bubble lines and lose 2 mood and 2 favorability per bubble (no loss at max favorability).
- Sugar interaction: feed sugar to restore 4% satiety with a proper eating animation; the maid visibly holds the sugar while eating.
- Unified Work Range config (replacing block/container/farm search distances) and a separate Follow Range config.
- Trilingual UI, config and source comments (简体中文 / English / 日本語).
- Salary Box now shows a sugar texture on its four sides.

### Changed

- Farming mode: search radius unchanged (work range); the maid actively walks to mature crops and harvests & replants only within her 6-block interaction shape; no dropped items during the process.
- Movement-mode confinement: Freedom is limited to the bound center (or current position) within Work Range; Follow is limited to a player-centered Follow Range; Redstone Patrol is unchanged.
- Rebel shield behavior now fully matches normal mode: the shield is only raised in battle with a target, and shield-break attacks disable blocking for a while.
- The rebellion proxy entity is hidden from crosshair targeting and HUD info mods, and no longer blocks player interactions.
- Mood now only decreases when a player attacks the maid; maids flee from the attacker only while Sad or Calm.
- Revival now uses the Rest state instead of the Strike state; sugar healing of strike maids was removed.
- The maid GUI was simplified to show only the current work mode and hunger value.
- Death now plays the full death animation before the souvenir drops, with no knockback residue after revival.
- The old emergency state was replaced by Rest/Evade; these two states ignore movement-mode restrictions while active.

### Fixed

- Rebel shield users no longer repeatedly block shield-break axe attacks.
- The proxy entity no longer interferes with left/right clicks or shows up in info mods.
- Ranged maid lock-on versus return-to-range pathfinding conflict (wobbling) resolved.
- Ammo and ammo-box detection now searches the inventory and expanded backpack (not just the offhand) for TACZ; spellbook and polyglot detection covers inventory and backpack for magic modes; offhand auto-swap removed.
- Eating animation now plays fully (32 ticks) before re-shielding.
- Bubble text no longer loses characters during rapid updates.
- Sitting position and instant sit animation fixed.
- Fixed various crashes, including one when attacking a maid.

### Compatibility

- TACZ, Curios API, Ars Nouveau, Iron's Spells 'n Spellbooks and Goety (all optional; the maid trinket UI is available even without Curios).

### Known Issues

- Under some shader packs, the maid's speech bubble text may render incorrectly (missing or garbled characters). Vanilla rendering is unaffected; please switch shaders or disable the relevant post-processing if you encounter this.

## [0.10] - 2026-08-31

### Added

- Maid Stick reworked into the Work Range / Maid Binder: bind a work range on the top face of a block, switch between "bind range" and "bind maid" modes, shift+right-click to clear, with an orange range ring and directional arrow preview.
- Maid Capture Egg: right-click your contracted maid to store her (full NBT preserved), right-click a block to release; empty/filled dual textures; the item drop glows, is indestructible and never despawns.
- Maid Souvenir: a maid drops a glowing, unbreakable, permanent souvenir on death; right-click a block to revive her at 1 HP in the Rest state, with mood restored to at least 10 and no leftover rebellion.
- Rest state: maids below 5% HP with no nearby enemies periodically sit down and recover until they reach 50% HP.
- Evade state (battle modes only): below 5% HP with enemies nearby, the maid keeps away from hostile targets until HP recovers above 30%; after 10s without enemies she switches to Rest.
- Force chunk loading: a beacon button in the maid GUI keeps the maid's chunk always loaded.
- Leash detection: leashed maids complain with mood-specific bubble lines and lose 2 mood and 2 favorability per bubble (no loss at max favorability).
- Sugar interaction: feed sugar to restore 4% satiety with a proper eating animation; the maid visibly holds the sugar while eating.
- Unified Work Range config (replacing block/container/farm search distances) and a separate Follow Range config.
- Trilingual UI, config and source comments (简体中文 / English / 日本語).
- Salary Box now shows a sugar texture on its four sides.

### Changed

- Farming mode: search radius unchanged (work range); the maid actively walks to mature crops and harvests & replants only within her 6-block interaction shape; no dropped items during the process.
- Movement-mode confinement: Freedom is limited to the bound center (or current position) within Work Range; Follow is limited to a player-centered Follow Range; Redstone Patrol is unchanged.
- Rebel shield behavior now fully matches normal mode: the shield is only raised in battle with a target, and shield-break attacks disable blocking for a while.
- The rebellion proxy entity is hidden from crosshair targeting and HUD info mods, and no longer blocks player interactions.
- Mood now only decreases when a player attacks the maid; maids flee from the attacker only while Sad or Calm.
- Revival now uses the Rest state instead of the Strike state; sugar healing of strike maids was removed.
- The maid GUI was simplified to show only the current work mode and hunger value.
- Death now plays the full death animation before the souvenir drops, with no knockback residue after revival.
- The old emergency state was replaced by Rest/Evade; these two states ignore movement-mode restrictions while active.

### Fixed

- Rebel shield users no longer repeatedly block shield-break axe attacks.
- The proxy entity no longer interferes with left/right clicks or shows up in info mods.
- Ranged maid lock-on versus return-to-range pathfinding conflict (wobbling) resolved.
- Ammo and ammo-box detection now searches the inventory and expanded backpack (not just the offhand) for TACZ; spellbook and polyglot detection covers inventory and backpack for magic modes; offhand auto-swap removed.
- Eating animation now plays fully (32 ticks) before re-shielding.
- Bubble text no longer loses characters during rapid updates.
- Sitting position and instant sit animation fixed.
- Fixed various crashes, including one when attacking a maid.

### Compatibility

- TACZ, Curios API, Ars Nouveau, Iron's Spells 'n Spellbooks and Goety (all optional; the maid trinket UI is available even without Curios).

### Known Issues

- Under some shader packs, the maid's speech bubble text may render incorrectly (missing or garbled characters). Vanilla rendering is unaffected; please switch shaders or disable the relevant post-processing if you encounter this.
