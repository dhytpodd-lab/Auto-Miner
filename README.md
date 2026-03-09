# Auto Miner Fabric Mod

Client-side Fabric mod for Minecraft 1.21.8 focused on configurable auto-mining with a runtime controller, GUI, HUD, presets, pathing, target search, and safety rules.

## Stack

- Minecraft `1.21.8`
- Fabric Loader `0.17.3`
- Fabric API `0.136.0+1.21.8`
- Cloth Config `19.0.147`
- Java `21`

## Build

### Windows

```powershell
.\gradlew.bat build
```

### macOS / Linux

```bash
./gradlew build
```

Resulting jar:

```text
build/libs/autominer-1.0.0.jar
```

## Run In Dev

### Windows

```powershell
.\gradlew.bat runClient
```

### macOS / Linux

```bash
./gradlew runClient
```

## Install

1. Install Java 21.
2. Install Minecraft Fabric Loader `0.17.3` for `1.21.8`.
3. Copy `build/libs/autominer-1.0.0.jar` into `.minecraft/mods`.
4. Start the Fabric profile.

## Main Controls

- `` ` ``: Toggle mod
- `P`: Pause / resume
- `Right Shift`: Open GUI
- `V`: Select block under crosshair as the only target
- `B`: Add block under crosshair to targets
- `N`: Remove block under crosshair from targets
- `H`: Toggle HUD
- `K`: Quick-save preset
- `L`: Load active preset
- `Delete`: Emergency stop

All keybinds are editable in the GUI and persisted to config.

## Project Layout

- `src/main/java/dev/danik/autominer/core`: bootstrap and runtime wiring
- `src/main/java/dev/danik/autominer/config`: JSON config model and manager
- `src/main/java/dev/danik/autominer/input`: keybind definitions and manager
- `src/main/java/dev/danik/autominer/mining`: controller, session state, mining executor, tools, inventory
- `src/main/java/dev/danik/autominer/targeting`: target search, caching, candidate scoring
- `src/main/java/dev/danik/autominer/navigation`: local path planning and waypoint walking
- `src/main/java/dev/danik/autominer/safety`: stop/pause conditions and hazard checks
- `src/main/java/dev/danik/autominer/gui`: multi-tab runtime GUI
- `src/main/java/dev/danik/autominer/hud`: on-screen HUD renderer
- `src/main/java/dev/danik/autominer/render`: target / area overlays
- `src/main/java/dev/danik/autominer/preset`: built-in and user preset system
- `src/main/java/dev/danik/autominer/util`: math, registry, color, block helpers

## Extension Points

- Add new targeting heuristics in `TargetSearchService`
- Add richer navigation rules in `NavigationService`
- Add more stop rules in `SafetyMonitor`
- Add new runtime actions or automation states in `AutoMinerController`
- Add GUI tabs or setting widgets in `AutoMinerScreen`
- Add built-in profiles in `BuiltInPresets`
