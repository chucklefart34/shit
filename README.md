# Drop Zone: Battle Royale Arsenal
Forge **1.20.1** mod (Forge 47.x). It's built to drop into **Better MC [FORGE] BMC4** and also works in any other 1.20.1 Forge pack.
Install it on the client and on the server.

## Download
Grab the jar from the [latest release](../../releases/tag/latest). GitHub Actions builds it automatically on every push to `main`.

## Build the jar yourself
1. Install **JDK 17** (e.g. Eclipse Temurin 17).
2. In this folder run `gradlew build` (Windows) or `./gradlew build` (macOS/Linux).
   The first build downloads Minecraft + Forge and takes a few minutes.
3. The mod is at `build/libs/dropzone-1.0.0.jar`. Drop it in your BMC4 instance's `mods` folder.

Test in a dev client with `gradlew runClient`.

## Controls
| Action | Default |
|---|---|
| Fire | Left mouse (hold for automatic weapons) |
| Aim down sights / scope | Right mouse (hold) |
| Reload | **R** |
| Emote wheel | **G**: hold, point, release (or tap, then click / press 1-8) |
| Deploy / stow glider | **Jump** while falling (glider anywhere in inventory) |
| Saddle a mob | Right-click it with a **Cannon Saddle** |
| Ride / unsaddle | Empty hand right-click / **Sneak** + right-click |
| Fire saddle cannon | Left mouse while riding (not holding a gun) |
| Cannon Destroy / Damage mode | **V** |

All keys are rebindable under *Options → Controls → Drop Zone*.

## Content
**Guns.** All use hitscan with spread, falloff and headshots. Ammo is magazine-fed and reloads from your inventory.
| Gun | Tier | Damage | Rate | Mag | Ammo |
|---|---|---|---|---|---|
| Pistol | Common | 5 | 5/s semi | 16 | Light |
| Tactical SMG | Uncommon | 3.5 | 10/s auto | 30 | Light |
| Pump Shotgun | Uncommon | 9 × 2.4 | 1/s | 5 | Shells |
| Assault Rifle | Rare | 6.5 | 5/s auto | 30 | Medium |
| Bolt-Action Sniper | Epic | 22 (×2 head) | bolt | 1 | Heavy |
| Rocket Launcher | Legendary | explosive | 0.8/s | 1 | Rockets |

**Other gear**
- **Frag Grenades.** They bounce, then explode after 2.5 s.
- **Healing.** Bandages heal 2 hearts, up to 75% health. Med Kits fully heal you.
- **Shields.** These give absorption hearts. Mini Shields add 2.5 hearts, up to a 5-heart cap. Shield Potions add 5 hearts, up to a 10-heart cap.
- **Gliders.** Four colours: Azure, Crimson, Emerald and Sunset. You take no fall damage while gliding, and the camera switches to third person.
- **Emotes.** Wave, Flex, Cheer, Salute, Facepalm, Robot, Disco and Swing Dance. Each dance plays its own note-block beat that nearby players hear. An emote stops when you move or take damage.

**Cannon Saddle.** Works on any mob, including the Ender Dragon. Mobs with no riding controls of their own become steerable: WASD plus where you look, and Jump climbs on flying mobs. Mounts that already have riding controls (tamed horses, modded mounts) keep them. The cannon fires a volley of fireballs: **Destroy** mode breaks blocks and starts fires, **Damage** mode only hurts mobs. Your own blasts and your mount can't hurt you.

**Day counter.** A "Day N" title appears for everyone each time a new day starts, and the current day shows in the top-left corner.

**Crafting and loot.** Everything is craftable, and JEI shows every recipe. Recipes use Forge tags, so modded iron, copper and other ingots work. Gear also shows up in dungeon, mineshaft, outpost, temple, mansion, shipwreck, ancient city and bastion chests.

## Config
`config/dropzone-common.toml` has these settings:
- damage multiplier
- whether ammo is required
- creative infinite ammo
- whether bullets break glass
- whether explosions break blocks
- rocket and grenade power
- whether gear spawns in chest loot

Block breaking from bullets and explosions respects claim and protection mods.

## Compatibility notes
- Mod id `dropzone`. There's one client-only mixin, which injects at the end of `PlayerModel#setupAnim` to pose players during emotes and gliding. It only takes over the pose while an emote or glide is active.
- An equipped elytra takes priority over the glider.
- Sounds and textures are original. The sniper scope reuses Minecraft's own spyglass overlay.
