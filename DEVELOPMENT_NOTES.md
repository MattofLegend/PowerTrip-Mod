# PowerTrip Mod Development Notes

## Version Compatibility & Framework Changes

This document outlines the key learnings, changes, and challenges encountered while developing and maintaining the PowerTrip mod for Minecraft 1.21.4.

## Minecraft & Fabric Version Info

| Component | Version | Notes |
|-----------|---------|-------|
| Minecraft | 1.21.4 | Target game version |
| Fabric API | 0.114.1+1.21.4 | Required for 1.21.4 compatibility |
| Fabric Loader | Latest | Compatible with 1.21.4 |
| Gradle | 8.11 | Required for newer builds |
| Loom | 1.9-SNAPSHOT | Required for 1.21.4 modding |

## Major API Changes in Minecraft 1.21.4

### 1. Networking API

The most significant change in 1.21.4 was the complete overhaul of the networking system. The old `PacketType`-based system has been replaced with a modern `CustomPayload`-based system.

#### Old Implementation (pre-1.20.5):
```java
// Old packet definition
public static final PacketType<RoulettePacket> ROULETTE_PACKET_TYPE = 
    PacketType.create(ROULETTE_ID, RoulettePacket::read);
    
// Old packet handling
ServerPlayNetworking.send(player, packet);
```

#### New Implementation (1.20.5+):
```java
// New payload definition with Record class
public static record RoulettePayload(List<String> playerNames, String selectedPlayer) implements CustomPayload {
    public static final CustomPayload.Id<RoulettePayload> ID = new CustomPayload.Id<>(ROULETTE_PACKET_ID);
    
    // Custom codec for serialization/deserialization
    public static final PacketCodec<PacketByteBuf, RoulettePayload> CODEC = new PacketCodec<>() {
        @Override
        public void encode(PacketByteBuf buf, RoulettePayload payload) {
            // Encoding logic
        }
        
        @Override
        public RoulettePayload decode(PacketByteBuf buf) {
            // Decoding logic
        }
    };
    
    @Override
    public Id<? extends CustomPayload> getId() {
        return ID;
    }
}

// Registration
PayloadTypeRegistry.playS2C().register(RoulettePayload.ID, RoulettePayload.CODEC);

// Sending packets
ServerPlayNetworking.send(player, payload);

// Receiving packets (client-side)
ClientPlayNetworking.registerGlobalReceiver(RoulettePayload.ID, (payload, context) -> {
    context.client().execute(() -> {
        // Handle payload
    });
});
```

### 2. Rendering API

The HUD rendering system has changed its parameter types. The important change is that `tickDelta` is now passed as a `RenderTickCounter` object instead of a float value.

#### Old Implementation:
```java
HudRenderCallback.EVENT.register((drawContext, tickDelta) -> {
    // tickDelta is a float
    renderHud(drawContext, tickDelta);
});
```

#### New Implementation:
```java
HudRenderCallback.EVENT.register((drawContext, tickCounter) -> {
    // tickCounter is a RenderTickCounter object
    float tickDelta = tickCounter.getTickDelta(false);
    renderHud(drawContext, tickDelta);
});
```

### 3. World Data Storage

Access to world data has changed from `LevelData` to `LevelProperties`.

#### Old Implementation:
```java
LevelData worldData = (LevelData)server.getOverworld().getLevelProperties();
```

#### New Implementation:
```java
LevelProperties worldData = (LevelProperties)server.getOverworld().getLevelProperties();
```

## Common Problems & Solutions

### Compilation Errors

1. **Missing Classes/Methods**: Many classes and methods have been renamed or relocated in 1.21.4. Always check the latest Fabric API documentation.

2. **Parameter Type Mismatches**: Many methods have changed their parameter types (like the HUD rendering callback). Be sure to check the method signatures.

3. **API Version Incompatibility**: Make sure you're using the correct Fabric API version (0.114.1+1.21.4) and Loom version (1.9-SNAPSHOT).

### Runtime Errors

1. **Networking Errors**: The networking system is completely different in 1.21.4. Make sure to use the CustomPayload API properly.

2. **Rendering Glitches**: The rendering system has subtle changes. Verify all rendering code against the latest documentation.

## Best Practices for 1.21.4 Modding

1. **Use Records for Data Packets**: Java records simplify the creation of immutable data containers, making them perfect for network payloads.

2. **Leverage PacketCodec**: The new PacketCodec system provides a cleaner API for serialization/deserialization.

3. **Follow Separation of Concerns**: Keep networking, rendering, and game logic in separate classes.

4. **Stay Updated on Fabric Wiki**: The Fabric Wiki is frequently updated with the latest API changes.

## Upgrading Tips

When upgrading a mod from earlier versions to 1.21.4:

1. First update the build files (gradle.properties, build.gradle) with the correct dependencies.
2. Fix any import statements for renamed or relocated classes.
3. Focus on the networking implementation as it's the most significantly changed area.
4. Update rendering callbacks to account for parameter changes.
5. Test extensively, as some changes have subtle effects on gameplay.

## Resources

- [Fabric Wiki](https://wiki.fabricmc.net/)
- [Fabric API Documentation](https://maven.fabricmc.net/docs/fabric-api-0.110.5+1.21.4/)
- [Minecraft Yarn Mappings](https://maven.fabricmc.net/docs/yarn-1.21.4-rc3+build.3/)
