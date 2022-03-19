package com.mcmoddev.mmdbot.commander.commands;

import com.jagrosh.jdautilities.command.CommandEvent;
import com.jagrosh.jdautilities.command.SlashCommand;
import com.jagrosh.jdautilities.command.SlashCommandEvent;
import com.mcmoddev.mmdbot.commander.TheCommander;
import com.mcmoddev.mmdbot.commander.annotation.RegisterSlashCommand;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.OptionData;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Locale;

/**
 * The bulk of the Search commands functions live here to be shared between all other commands.
 *
 * @author Curle
 */
public final class SearchCommand extends SlashCommand {

    @RegisterSlashCommand
    public static final SlashCommand GOOGLE = new SearchCommand("google", "https://www.google.com/search?q=", "goog");
    @RegisterSlashCommand
    public static final SlashCommand BING = new SearchCommand("bing", "https://www.bing.com/search?q=");
    @RegisterSlashCommand
    public static final SlashCommand DUCK_DUCK_GO = new SearchCommand("duckduckgo", "https://duckduckgo.com/?q=", "ddg");
    @RegisterSlashCommand
    public static final SlashCommand LMGTFY = new SearchCommand("lmgtfy", "https://lmgtfy.com/?q=", "let-me-google-that-for-you");

    /**
     * The search provider we want to generate a URL for.
     */
    private final String baseUrl;

    /**
     * Instantiates a new Cmd search.
     *
     * @param name      The command's/search engine's name.
     * @param baseUrlIn The base URL of the search provider.
     * @param aliases   the aliases
     */
    public SearchCommand(final String name, final String baseUrlIn, final String... aliases) {
        this.name = name.toLowerCase(Locale.ROOT);
        this.aliases = aliases;
        help = "Search for something using " + name + ".";
        this.baseUrl = baseUrlIn;

        this.options = List.of(new OptionData(OptionType.STRING, "text", "The text to search").setRequired(true));
        guildOnly = false;
    }

    /**
     * Execute.
     *
     * @param event The {@link CommandEvent CommandEvent} that triggered this Command.
     */
    protected void execute(final SlashCommandEvent event) {
        try {
            final String query = URLEncoder.encode(event.getOption("text").getAsString(), StandardCharsets.UTF_8.toString());
            event.reply(baseUrl + query).mentionRepliedUser(false).queue();
        } catch (UnsupportedEncodingException ex) {
            TheCommander.LOGGER.error("Error processing search query {}: {}", event.getOption("text").getAsString(), ex);
            event.reply("There was an error processing your command.").mentionRepliedUser(false).queue();
        }

    }

}
