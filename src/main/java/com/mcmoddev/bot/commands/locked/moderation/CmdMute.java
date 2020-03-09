package com.mcmoddev.bot.commands.locked.moderation;

import com.jagrosh.jdautilities.command.Command;
import com.jagrosh.jdautilities.command.CommandEvent;
import com.mcmoddev.bot.MMDBot;
import com.mcmoddev.bot.misc.Utils;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Role;
import net.dv8tion.jda.api.entities.TextChannel;

import java.util.concurrent.TimeUnit;

public class CmdMute extends Command {

	@Override
	protected void execute(CommandEvent event) {
		final Guild guild = event.getGuild();
		final TextChannel channel = guild.getTextChannelById(MMDBot.getConfig().getChannelIDConsole());
		final String[] args = event.getArgs().split(" ");
		final Member member = Utils.getMemberFromString(args[0], event.getGuild());
		final Role mutedRole = guild.getRoleById(MMDBot.getConfig().getRoleMuted());

		if (member == null) {
			if (channel != null)
				channel.sendMessage(String.format("User %s not found.", event.getArgs())).queue();
			else
				MMDBot.LOGGER.error("Unable to find console channel!");
			return;
		}
		if (mutedRole == null) {
			MMDBot.LOGGER.error("Unable to find muted role!");
			return;
		}

		final long time;
		final TimeUnit unit;
		if (args.length > 1) {
			long time1;
			try {
				time1 = Long.parseLong(args[1]);
			} catch (NumberFormatException e) {
				time1 = -1;
			}
			time = time1;
		} else time = -1;
		if (args.length > 2) {
			TimeUnit unit1;
			try {
				unit1 = TimeUnit.valueOf(args[2]);
			} catch (IllegalArgumentException e) {
				unit1 = TimeUnit.MINUTES;
			}
			unit = unit1;
		} else unit = TimeUnit.MINUTES;

		guild.addRoleToMember(member, mutedRole).queue();

		if (time > 0)
			guild.removeRoleFromMember(member, mutedRole).queueAfter(time, unit);

		final String timeString;
		if (time > 0) {
			timeString = " " + time + " " + unit.toString();
		} else timeString = "ever";

		if (channel != null)
			channel.sendMessageFormat("Muted user %s for%s.", member.getAsMention(), timeString).queue();
		else
			MMDBot.LOGGER.error("Unable to find console channel!");
	}

}
