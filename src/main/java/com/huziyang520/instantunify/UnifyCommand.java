package com.huziyang520.instantunify;

import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

/**
 * {@code /instantunify reload} rebuilds the derived caches after the configuration file was edited and applies
 * the new configuration to the inventories of the players that are online right now.
 * <p>
 * This is the server side counterpart of the in-game screen: an in-game screen can only edit the file of the
 * game it runs in, while a dedicated server has to be told about the change.
 */
public final class UnifyCommand {

    @SubscribeEvent
    public void onRegisterCommands(RegisterCommandsEvent event) {
        event.getDispatcher().register(Commands.literal("instantunify")
                .requires(source -> source.hasPermission(2))
                .then(Commands.literal("reload").executes(context -> {
                    ItemUnifier.reload();
                    int updated = 0;
                    for (ServerPlayer player : context.getSource().getServer().getPlayerList().getPlayers()) {
                        if (Inventories.replaceInventory(player)) {
                            player.containerMenu.broadcastChanges();
                            updated++;
                        }
                    }
                    int count = updated;
                    context.getSource().sendSuccess(
                            () -> Component.translatable("instantunify.command.reload", count), true);
                    return 1;
                })));
    }
}
