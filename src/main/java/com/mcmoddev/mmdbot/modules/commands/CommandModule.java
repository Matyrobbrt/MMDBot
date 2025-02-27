/*
 * MMDBot - https://github.com/MinecraftModDevelopment/MMDBot
 * Copyright (C) 2016-2022 <MMD - MinecraftModDevelopment>
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301
 * USA
 * https://www.gnu.org/licenses/old-licenses/lgpl-2.1.html
 */
package com.mcmoddev.mmdbot.modules.commands;

import com.jagrosh.jdautilities.command.CommandClient;
import com.jagrosh.jdautilities.command.CommandClientBuilder;
import com.jagrosh.jdautilities.command.ContextMenu;
import com.jagrosh.jdautilities.command.SlashCommand;
import com.mcmoddev.mmdbot.MMDBot;
import com.mcmoddev.mmdbot.modules.commands.bot.info.CmdAbout;
import com.mcmoddev.mmdbot.modules.commands.bot.info.CmdHelp;
import com.mcmoddev.mmdbot.modules.commands.bot.management.CmdAvatar;
import com.mcmoddev.mmdbot.modules.commands.bot.management.CmdRefreshScamLinks;
import com.mcmoddev.mmdbot.modules.commands.bot.management.CmdRename;
import com.mcmoddev.mmdbot.modules.commands.bot.management.CmdRestart;
import com.mcmoddev.mmdbot.modules.commands.bot.management.CmdShutdown;
import com.mcmoddev.mmdbot.modules.commands.community.CmdEvaluate;
import com.mcmoddev.mmdbot.modules.commands.community.contextmenu.GuildOnlyMenu;
import com.mcmoddev.mmdbot.modules.commands.community.contextmenu.message.ContextMenuAddQuote;
import com.mcmoddev.mmdbot.modules.commands.community.contextmenu.message.ContextMenuGist;
import com.mcmoddev.mmdbot.modules.commands.community.contextmenu.user.ContextMenuUserInfo;
import com.mcmoddev.mmdbot.modules.commands.community.development.CmdFabricVersion;
import com.mcmoddev.mmdbot.modules.commands.community.development.CmdForgeVersion;
import com.mcmoddev.mmdbot.modules.commands.community.development.CmdGist;
import com.mcmoddev.mmdbot.modules.commands.community.development.CmdMinecraftVersion;
import com.mcmoddev.mmdbot.modules.commands.community.information.CmdCatFacts;
import com.mcmoddev.mmdbot.modules.commands.community.information.CmdDictionary;
import com.mcmoddev.mmdbot.modules.commands.community.information.CmdGuild;
import com.mcmoddev.mmdbot.modules.commands.community.information.CmdInvite;
import com.mcmoddev.mmdbot.modules.commands.community.information.CmdSearch;
import com.mcmoddev.mmdbot.modules.commands.community.information.CmdUser;
import com.mcmoddev.mmdbot.modules.commands.community.server.CmdRoles;
import com.mcmoddev.mmdbot.modules.commands.community.server.CmdToggleEventPings;
import com.mcmoddev.mmdbot.modules.commands.community.server.CmdToggleMcServerPings;
import com.mcmoddev.mmdbot.modules.commands.community.server.DeletableCommand;
import com.mcmoddev.mmdbot.modules.commands.community.server.quotes.CmdQuote;
import com.mcmoddev.mmdbot.modules.commands.community.server.tricks.CmdAddTrick;
import com.mcmoddev.mmdbot.modules.commands.community.server.tricks.CmdEditTrick;
import com.mcmoddev.mmdbot.modules.commands.community.server.tricks.CmdListTricks;
import com.mcmoddev.mmdbot.modules.commands.community.server.tricks.CmdRawTrick;
import com.mcmoddev.mmdbot.modules.commands.community.server.tricks.CmdRunTrick;
import com.mcmoddev.mmdbot.modules.commands.community.server.tricks.CmdTrick;
import com.mcmoddev.mmdbot.modules.commands.moderation.CmdCommunityChannel;
import com.mcmoddev.mmdbot.modules.commands.moderation.CmdMute;
import com.mcmoddev.mmdbot.modules.commands.moderation.CmdOldChannels;
import com.mcmoddev.mmdbot.modules.commands.moderation.CmdReact;
import com.mcmoddev.mmdbot.modules.commands.moderation.CmdRolePanel;
import com.mcmoddev.mmdbot.modules.commands.moderation.CmdUnmute;
import com.mcmoddev.mmdbot.modules.commands.moderation.CmdWarning;
import com.mcmoddev.mmdbot.utilities.ThreadedEventListener;
import com.mcmoddev.mmdbot.utilities.Utils;
import com.mcmoddev.mmdbot.utilities.tricks.Tricks;
import net.dv8tion.jda.api.hooks.EventListener;
import net.dv8tion.jda.api.interactions.commands.build.CommandData;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

/**
 * This is the main class for setting up commands before they are loaded in by the bot,
 * this way we can disable and enable them at will. Or at least that is the hope.
 *
 * @author KiriCattus
 */
public class CommandModule {

    /**
     * The constant commandClient.
     */
    private static CommandClient commandClient;

    /**
     * Gets command client.
     *
     * @return the command client
     */
    public static CommandClient getCommandClient() {
        return commandClient;
    }

    public static final Executor COMMAND_LISTENER_THREAD_POOL = Executors.newFixedThreadPool(2, r -> Utils.setThreadDaemon(new Thread(r, "CommandListener"), true));
    public static final Executor BUTTON_LISTENER_THREAD_POOL = Executors.newSingleThreadExecutor(r -> Utils.setThreadDaemon(new Thread(r, "ButtonListener"), true));

    /**
     * Setup and load the bots command module.
     */
    public static void setupCommandModule() {

        commandClient = new CommandClientBuilder()
            .setOwnerId(MMDBot.getConfig().getOwnerID())
            .setPrefix(MMDBot.getConfig().getMainPrefix())
            .setAlternativePrefix(MMDBot.getConfig().getAlternativePrefix())
            .useHelpBuilder(false).setManualUpsert(true).build();

        addSlashCommand(new CmdHelp(),
            new CmdGuild(),
            new CmdAbout(),
            new CmdUser(),
            new CmdRoles(),
            new CmdCatFacts(),
            new CmdSearch("google", "https://www.google.com/search?q=", "goog"),
            new CmdSearch("bing", "https://www.bing.com/search?q="),
            new CmdSearch("duckduckgo", "https://duckduckgo.com/?q=", "ddg"),
            new CmdSearch("lmgtfy", "https://lmgtfy.com/?q=", "let-me-google-that-for-you"),
            new CmdToggleMcServerPings(),
            new CmdToggleEventPings(),
            new CmdForgeVersion(),
            new CmdMinecraftVersion(),
            new CmdFabricVersion(),
            new CmdMute(),
            new CmdUnmute(),
            new CmdCommunityChannel(),
            new CmdOldChannels(),
            new CmdAvatar(),
            new CmdRename(),
            //TODO Setup DB storage for tricks and polish them off/add permission restrictions for when needed.
            new CmdShutdown(),
            new CmdRestart(),
            new CmdQuote(),
            new CmdRolePanel(),
            new CmdWarning(),
            new CmdTrick(),
            new CmdInvite(),
            new CmdDictionary(),
            new CmdEvaluate(),
            new CmdRawTrick());

        // addSlashCommand(Tricks.getTricks().stream().map(CmdRunTrickSeparated::new).toArray(SlashCommand[]::new));

        commandClient.addCommand(new CmdRefreshScamLinks());
        commandClient.addCommand(new CmdReact());
        commandClient.addCommand(new CmdGist());
        commandClient.addCommand(new CmdEvaluate());

        commandClient.addCommand(new CmdAddTrick.Prefix());
        commandClient.addCommand(new CmdEditTrick.Prefix());

        addContextMenu(new ContextMenuGist());
        addContextMenu(new ContextMenuAddQuote());
        addContextMenu(new ContextMenuUserInfo());

        if (MMDBot.getConfig().prefixTricksEnabled()) {
            Tricks.getTricks().stream().map(CmdRunTrick.Prefix::new).forEach(commandClient::addCommand);
        }

        if (MMDBot.getConfig().isCommandModuleEnabled()) {
            // Wrap the command and button listener in another thread, so that if a runtime exception
            // occurs while executing a command, the event thread will not be stopped
            // Commands and buttons are separated so that they do not interfere with each other
            MMDBot.getInstance().addEventListener(new ThreadedEventListener((EventListener) commandClient, COMMAND_LISTENER_THREAD_POOL));
            MMDBot.getInstance().addEventListener(buttonListener(CmdRoles.getListener()));
            MMDBot.getInstance().addEventListener(buttonListener(CmdHelp.getListener()));
            MMDBot.getInstance().addEventListener(buttonListener(CmdListTricks.getListListener()));
            MMDBot.getInstance().addEventListener(buttonListener(CmdQuote.ListQuotes.getQuoteListener()));
            MMDBot.getInstance().addEventListener(buttonListener(CmdInvite.ListCmd.getButtonListener()));
            MMDBot.getInstance().addEventListener(buttonListener(CmdDictionary.listener));
            MMDBot.getInstance().addEventListener(buttonListener(new DismissListener()));
            MMDBot.LOGGER.warn("Command module enabled and loaded.");
        } else {
            MMDBot.LOGGER.warn("Command module disabled via config, commands will not work at this time!");
        }
    }

    private static EventListener buttonListener(final EventListener listener) {
        return new ThreadedEventListener(listener, BUTTON_LISTENER_THREAD_POOL);
    }

    /**
     * Removes a slash command. If the command is a {@link DeletableCommand}, this also marks it as deleted so that if
     * somehow it is run (which should be impossible) nothing will happen.
     *
     * @param name the name of the command to remove
     */
    public static void removeCommand(final String name, final boolean guildOnly) {
        if (guildOnly) {
            var guild = MMDBot.getInstance().getGuildById(MMDBot.getConfig().getGuildID());
            if (guild == null) {
                throw new NullPointerException("No Guild found!");
            }

            commandClient.getSlashCommands().stream()
                .filter(cmd -> cmd.getName().equals(name))
                .filter(cmd -> cmd instanceof DeletableCommand && cmd.isGuildOnly())
                .map(cmd -> (DeletableCommand) cmd)
                .forEach(DeletableCommand::delete);

            guild.retrieveCommands()
                .flatMap(list -> list.stream().filter(cmd -> cmd.getName().equals(name)).findAny().map(cmd -> guild.deleteCommandById(cmd.getId())).orElseThrow())
                .queue();
        } else {
            commandClient.getSlashCommands().stream()
                .filter(cmd -> cmd.getName().equals(name))
                .filter(cmd -> cmd instanceof DeletableCommand && !cmd.isGuildOnly())
                .map(cmd -> (DeletableCommand) cmd)
                .forEach(DeletableCommand::delete);

            MMDBot.getInstance().retrieveCommands()
                .flatMap(list -> list.stream().filter(cmd -> cmd.getName().equals(name)).findAny().map(cmd ->
                    MMDBot.getInstance().deleteCommandById(cmd.getId())).orElseThrow()).queue();
        }
    }

    // This is a temporary fix for something broken in chewtils, whose fix is not yet published
    private static final Map<String, ContextMenu> MENUS = Collections.synchronizedMap(new HashMap<>());

    public static ContextMenu getMenu(final String name) {
        return MENUS.get(name);
    }

    public static void addContextMenu(final ContextMenu menu) {
        commandClient.addContextMenu(menu);
        MENUS.put(menu.getName(), menu);
        upsertCommand(menu.buildCommandData(), menu instanceof GuildOnlyMenu);
    }

    /**
     * Adds and upserts a slash command.
     *
     * @param cmds the command(s) to upsert
     */
    public static void addSlashCommand(final SlashCommand... cmds) {
        for (final var cmd : cmds) {
            commandClient.addSlashCommand(cmd);
            upsertCommand(cmd.buildCommandData(), cmd.isGuildOnly());
        }
    }

    /**
     * Upserts a slash command.
     *
     * @param cmd the command
     */
    public static void upsertCommand(final SlashCommand cmd) {
        if (cmd.isGuildOnly()) {
            var guild = MMDBot.getInstance().getGuildById(MMDBot.getConfig().getGuildID());
            if (guild == null) {
                throw new NullPointerException("No Guild found!");
            }

            guild.upsertCommand(cmd.buildCommandData()).queue(cmd1 -> {
                cmd1.updatePrivileges(guild, cmd.buildPrivileges(commandClient)).queue();
            });
        } else {
            MMDBot.getInstance().updateCommands().addCommands(cmd.buildCommandData()).queue();
        }
    }

    public static void upsertCommand(final CommandData data, boolean guildOnly) {
        if (guildOnly) {
            var guild = MMDBot.getInstance().getGuildById(MMDBot.getConfig().getGuildID());
            if (guild == null) {
                throw new NullPointerException("No Guild found!");
            }

            guild.upsertCommand(data).queue();
        } else {
            MMDBot.getInstance().upsertCommand(data).queue();
        }
    }

    /**
     * Upserts a context menu.
     *
     * @param menu the menu
     */
    public static void upsertContextMenu(final ContextMenu menu) {
        if (menu instanceof GuildOnlyMenu) {
            var guild = MMDBot.getInstance().getGuildById(MMDBot.getConfig().getGuildID());
            if (guild == null) {
                throw new NullPointerException("No Guild found!");
            }

            guild.upsertCommand(menu.buildCommandData()).queue(cmd1 -> cmd1.updatePrivileges(guild, menu.buildPrivileges(commandClient)).queue());
        } else {
            MMDBot.getInstance().upsertCommand(menu.buildCommandData()).queue();
        }
    }
}
