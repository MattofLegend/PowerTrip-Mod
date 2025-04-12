package com.powertrip.mod.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.BoolArgumentType;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.powertrip.mod.PowerTripMod;
import com.powertrip.mod.power.PowerManager;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.text.Text;

import static net.minecraft.server.command.CommandManager.argument;
import static net.minecraft.server.command.CommandManager.literal;

/**
 * Registers and handles all PowerTrip mod commands
 */
public class PowerTripCommands {
    
    /**
     * Registers all commands
     * @param dispatcher Command dispatcher
     */
    public static void register(CommandDispatcher<ServerCommandSource> dispatcher) {
        // Register the /powertrip command with start and stop subcommands
        dispatcher.register(
            literal("powertrip")
                .requires(source -> source.hasPermissionLevel(4)) // Requires permission level 4 (server operator)
                .then(literal("start")
                    .executes(PowerTripCommands::executeStart)
                )
                .then(literal("stop")
                    .executes(PowerTripCommands::executeStop)
                )
                .then(literal("status")
                    .executes(PowerTripCommands::executeStatus)
                )
                .then(argument("days", IntegerArgumentType.integer(1))
                    .executes(PowerTripCommands::executeSetDuration)
                )
                .then(literal("autostart")
                    .then(argument("enabled", BoolArgumentType.bool())
                        .executes(PowerTripCommands::executeAutostart)
                    )
                )
        );
        
        PowerTripMod.LOGGER.info("Registered PowerTrip commands");
    }
    
    /**
     * Execute the start command
     * @param context Command context
     * @return Command result
     */
    private static int executeStart(CommandContext<ServerCommandSource> context) {
        ServerCommandSource source = context.getSource();
        
        // Get the server's power manager
        PowerManager powerManager = PowerTripMod.POWER_MANAGER;
        
        // Check if already running
        if (powerManager.isRunning()) {
            source.sendFeedback(() -> Text.literal("PowerTrip cycle is already running!"), false);
            return 0;
        }
        
        // Start the power cycle
        powerManager.startCycle(source.getServer());
        // Success message removed as requested
        
        return 1;
    }
    
    /**
     * Execute the stop command
     * @param context Command context
     * @return Command result
     */
    private static int executeStop(CommandContext<ServerCommandSource> context) {
        ServerCommandSource source = context.getSource();
        
        // Get the server's power manager
        PowerManager powerManager = PowerTripMod.POWER_MANAGER;
        
        // Check if already stopped
        if (!powerManager.isRunning()) {
            source.sendFeedback(() -> Text.literal("PowerTrip cycle is not currently running!"), false);
            return 0;
        }
        
        // Stop the power cycle
        powerManager.stopCycle(source.getServer());
        source.sendFeedback(() -> Text.literal("PowerTrip cycle stopped! All players have been de-opped."), true);
        
        return 1;
    }
    
    /**
     * Execute the status command
     * @param context Command context
     * @return Command result
     */
    private static int executeStatus(CommandContext<ServerCommandSource> context) {
        ServerCommandSource source = context.getSource();
        
        // Get the server's power manager
        PowerManager powerManager = PowerTripMod.POWER_MANAGER;
        
        if (powerManager.isRunning()) {
            int daysRemaining = powerManager.getDaysRemaining();
            String currentOp = powerManager.getCurrentPowerPlayer();
            
            source.sendFeedback(() -> Text.literal("PowerTrip cycle is active:"), false);
            source.sendFeedback(() -> Text.literal("- Current Operator: " + (currentOp == null ? "None" : currentOp)), false);
            source.sendFeedback(() -> Text.literal("- Days Remaining: " + daysRemaining), false);
        } else {
            source.sendFeedback(() -> Text.literal("PowerTrip cycle is not currently running."), false);
            source.sendFeedback(() -> Text.literal("Use '/powertrip start' to begin a new cycle."), false);
        }
        
        return 1;
    }
    
    /**
     * Execute the command to set the power cycle duration
     * @param context Command context
     * @return Command result
     */
    private static int executeSetDuration(CommandContext<ServerCommandSource> context) {
        ServerCommandSource source = context.getSource();
        
        // Get the server's power manager
        PowerManager powerManager = PowerTripMod.POWER_MANAGER;
        
        // Get the days argument from the command
        int days = IntegerArgumentType.getInteger(context, "days");
        
        // Try to set the cycle duration
        if (powerManager.setCycleDuration(days)) {
            source.sendFeedback(() -> Text.literal("PowerTrip reign period set to " + days + " days."), true);
            return 1;
        } else {
            source.sendFeedback(() -> Text.literal("A powertrip must not be currently active to change the reign period"), false);
            return 0;
        }
    }
    
    /**
     * Execute the autostart command
     * @param context Command context
     * @return Command result
     */
    private static int executeAutostart(CommandContext<ServerCommandSource> context) {
        ServerCommandSource source = context.getSource();
        boolean enabled = BoolArgumentType.getBool(context, "enabled");
        
        // Get the server's power manager
        PowerManager powerManager = PowerTripMod.POWER_MANAGER;
        
        // Set the autostart setting
        powerManager.setAutostartEnabled(enabled);
        
        // Send feedback to the user
        source.sendFeedback(() -> Text.literal("PowerTrip autostart " + 
                                         (enabled ? "enabled" : "disabled")), true);
        return 1;
    }
}
