# StackedDimensions - Minecraft 1.20.1 Forge Mod

A Minecraft mod that stacks multiple dimensions vertically in the same world, allowing seamless travel between dimensions by moving up or down.

## Features

- **Vertical Dimension Stacking**: Overworld, Nether, and End rendered in the same world at different Y-coordinates
- **Seamless Transitions**: Automatically teleport between dimensions when crossing Y-boundaries
- **Configurable Heights**: Customize the Y-ranges for each dimension layer
- **Custom World Generation**: Integrated terrain generation for all stacked dimensions
- **Performance Optimized**: Extended rendering system to handle multiple dimensions efficiently

## Default Configuration

- **Overworld**: Y -64 to 320
- **Nether**: Y 320 to 448
- **The End**: Y 448 to 576

## Installation

1. Install Minecraft Forge 1.20.1
2. Download the StackedDimensions mod jar
3. Place in your `mods` folder
4. Launch Minecraft

## Creating a Stacked World

### Method 1: Using the WorldPreset (Recommended)

**First, generate the datapack:**
```bash
./gradlew runData
```

This creates the WorldPreset in `src/generated/resources/`.

**Then create the world:**
1. Launch Minecraft with the mod installed
2. Click "Create New World"
3. Click "More World Options"
4. Click "World Type" until you see "Stacked Dimensions"
5. Create and play!

### Method 2: Testing in Development

When testing in the dev environment, the stacked dimensions will work automatically if you:
1. Use the default world generation
2. The mod will log information about dimension layers on world load

**Note:** The mod extends the Overworld's height to accommodate all three dimensions. When you create a world:
- Overworld generates from Y -64 to Y 320 (normal)
- Nether generates from Y 320 to Y 448
- End generates from Y 448 to Y 576

Walk/fly upward past Y 320 to enter the Nether layer, and past Y 448 to enter the End layer!

## Configuration

Edit `config/stackeddimensions-common.toml` to customize:

```toml
# Enable/disable stacked dimensions
enableStackedDimensions = true

# Y-coordinate ranges for each dimension
overworldMinY = -64
overworldMaxY = 320
netherMinY = 320
netherMaxY = 448
endMinY = 448
endMaxY = 576

# Dimension stacking order (bottom to top)
dimensionOrder = ["overworld", "nether", "end"]

# Enable automatic teleportation between layers
enableTeleportation = true
```

## Technical Details

### World Generation
- Uses custom `StackedDimensionChunkGenerator` that combines multiple `NoiseBasedChunkGenerator` instances
- Each dimension layer has its own biome source and generation settings
- Surface rules applied per-layer with Y-coordinate boundaries

### Rendering
- Extended `ViewArea` to support full vertical range
- Mixins into `LevelRenderer` for multi-dimensional chunk management
- Optimized frustum culling for extended height

### Player Movement
- Automatic teleportation when crossing dimension boundaries
- Smooth transitions preserve X/Z coordinates
- Configurable teleportation buffer zones

## Compatibility

- **Minecraft**: 1.20.1
- **Forge**: 47.0.0+
- **Java**: 17+

## Known Limitations

- Only works in the Overworld dimension
- Some dimension-specific features may not work correctly (e.g., Nether portals)
- Entity AI may behave unexpectedly near dimension boundaries
- Lighting may have minor artifacts at transition zones

## Credits

Based on the original StackedDimensions mod for Minecraft 1.14.4 by kmerrill285.

Ported to 1.20.1 with modern Forge APIs and completely rewritten world generation system.

## License

This mod is provided as-is for educational and entertainment purposes.
