# Voxel Grove 0.3 — mobile gameplay overhaul

This is an original block sandbox inspired by the voxel-survival genre, **not** the complete Minecraft game.

## Implemented

- Floating left virtual joystick with separate camera-swipe touch region.
- Hold on the scene (or pickaxe button) to break blocks; tap scene / build button to place blocks.
- Continuous jump while the button is held, toggle sprint, and crouch movement with lower camera and safer ledge movement.
- Better pixel art: 32 custom 16x16 tile designs upscaled to a crisp 256x128 atlas; side/top variants for grass, logs and snow.
- Animated, distinct voxel models for sheep, pigs and a hostile zombie-like creature with eyes, noses and articulated limbs.
- More legible Android landscape HUD with true texture thumbnails, selected hotbar item, survival hearts/food and overhauled creative inventory/crafting panels.
- Existing procedural terrain, ores, cave pockets, huts, ruins, save/load (including 0.1 saves), 12 crafting recipes and creature behavior retained.
- Automated 29 pure Java logic checks on every Android APK build.

## Honest limitations

The world is currently 48×48×28 blocks. This prototype does **not** yet contain the full set of Minecraft biomes, items, dimensions, mobs, villages, redstone, multiplayer, or a fully featured survival/crafting system. Android gameplay should be tested on a device after the debug build.

## APK

Open **Actions → Build Android → latest successful run → Artifacts → VoxelGrove-Android**. Unzip the artifact, then install the included debug APK.
