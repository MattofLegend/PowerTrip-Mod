# PowerTrip Mod

A Minecraft 1.21.4 mod that creates a democratic rotation of power on multiplayer servers.

## What Does It Do?

PowerTrip mod creates a democratic rotation of power on your server through a 7-day cycle:

1. Teleports all players back to the world spawn point
2. Displays a roulette animation that cycles through all online player names
3. Randomly selects one player to become the server operator (OP)
4. Grants the selected player operator privileges for 7 days
5. After 7 days, all players are de-opped and the cycle begins again

This creates a fun rotation of power where different players get to be in charge for limited periods! The power cycle must be started manually using the mod's commands.

## Features

- **Command-driven Cycle Management**: Start and stop power cycles as needed
- **Visual Roulette Effect**: Exciting selection animation shown to all players
- **Daily Reminders**: Players are reminded how many days remain in the current cycle
- **Fair Selection**: Random selection from all online players
- **Server Spawn Focus**: Brings everyone together at the start of each cycle

## Setup for Development

1. Install Java Development Kit (JDK) 17 or later
2. Install an IDE like IntelliJ IDEA or Visual Studio Code with Java support
3. Import this project as a Gradle project in your IDE
4. Run the `genSources` Gradle task to generate the Minecraft sources:
   ```
   gradlew.bat genSources
   ```

## Building and Testing

To build the mod:

```
gradlew.bat build
```

The compiled JAR file will be in `build/libs/`.

To test the mod:

```
gradlew.bat runClient  # For client testing
gradlew.bat runServer  # For server testing
```

## Project Structure

- **Main Mod Class**: `PowerTripMod.java` - Core initialization and setup
- **Power Management**: `PowerManager.java` - Handles OP status changes
- **Time Tracking**: `TimeTracker.java` - Monitors Minecraft day cycle
- **Roulette Display**: `RouletteDisplay.java` - Client-side animation
- **Network Handler**: `NetworkHandler.java` - Server-client communication
- **Server Tick Handler**: `ServerTickHandler.java` - Main event processing

## Commands

PowerTrip mod provides the following commands for server administrators (requires operator permission level 4):

### `/powertrip start`
Starts a new power cycle, which:
- Teleports all players to spawn
- Selects a random player as the operator
- Displays the roulette animation
- Begins the 7-day countdown

### `/powertrip stop`
Stops the current power cycle:
- Removes operator status from all players
- Prevents the countdown from continuing
- Notifies all players that the cycle has been stopped

### `/powertrip status`
Shows information about the current cycle:
- Whether a cycle is currently active
- Who the current operator is
- How many days remain in the cycle

## Installation for Players

1. Install [Fabric Loader](https://fabricmc.net/use/) for Minecraft 1.21.4
2. Download and install [Fabric API](https://www.curseforge.com/minecraft/mc-mods/fabric-api) for 1.21.4
3. Place the PowerTrip mod JAR into your Minecraft `mods` folder

**Note**: The mod should be installed on both the server and client for the full experience. Clients need the mod to see the roulette animation.

## License

This project is licensed under the MIT License - see the LICENSE file for details.
