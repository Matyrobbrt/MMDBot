package com.mcmoddev.bot.listener;

import java.util.HashMap;
import java.util.Map;

import com.mcmoddev.bot.MMDBot;
import com.mcmoddev.bot.command.*;
import com.mcmoddev.bot.util.Utilities;
import sx.blah.discord.handle.obj.IMessage;

public class CommandHandler {

    private static final Map<String, Command> commands = new HashMap<>();

    public static void initHandlers () {

        registerCommand("help", new CommandHelp());
        registerCommand("members", new CommandMemberCount());
        registerCommand("server", new CommandServerInfo());
        registerCommand("reload", new CommandReload());
        registerCommand("rename", new CommandRename());
        registerCommand("avatar", new CommandAvatar());
        registerCommand("prune", new CommandPruneChannels());
        registerCommand("curse", new CommandCurse());
        registerCommand("probe", new CommandProbe());
    }

    /**
     * Registers a command with the command registry.
     *
     * @param key The key that will trigger the command.
     * @param command The command that is triggered by the key phrase.
     */
    public static void registerCommand (String key, Command command) {

        if (!commands.containsKey(key))
            commands.put(key, command);
    }

    /**
     * Provides access the the Map used to store all registered commands.
     *
     * @return Map<String, CommandBase> The Map used to store all commands.
     */
    public static Map<String, Command> getCommands () {

        return commands;
    }

    /**
     * Attempts to trigger a command in the command registry.
     *
     * @param message The message to parse.
     */
    public static void attemptCommandTriggers (IMessage message) {

        final String key = getCommandKeyFromMessage(message.getContent());
        final Command command = commands.get(key);

        if (command == null) {
            return;
        }

        if (!command.isValidUsage(message)) {

            Utilities.sendPrivateMessage(message.getAuthor(), "You do not have permission to use the " + key + " command. Please try again, or look into getting elevated permissions.");
            return;
        }

        command.processCommand(message, getParameters(message.getContent()));
    }

    /**
     * Retrieves a command key from an IMessage. This method assumes that the message passed
     * starts with a command character.
     *
     * @param message The contents of the message to retrieve the key from.
     *
     * @return String The command key being used.
     */
    public static String getCommandKeyFromMessage (String message) {

        return getParameters(message)[0].toLowerCase();
    }

    /**
     * Generates a list of String parameters used for a command, based on a String message.
     * This method assumes that the message passed starts with a command character. This method
     * will also use the command key as the first parameter.
     *
     * @param message The message to sort through.
     *
     * @return String[] An array of all parameters and the command key.
     */
    public static String[] getParameters (String message) {

        return message.substring(MMDBot.COMMAND_KEY.length() + 1).split(" ");
    }

    /**
     * Gets a command with the passed key name.
     *
     * @param keyName The name of the command to look for.
     *
     * @return Command The found command, or null if it doesn't exist.
     */
    public static Command getCommand (String keyName) {

        return commands.get(keyName.toLowerCase());
    }
}
