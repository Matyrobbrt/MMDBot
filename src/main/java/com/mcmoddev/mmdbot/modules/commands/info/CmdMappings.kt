package com.mcmoddev.mmdbot.modules.commands.info

import com.jagrosh.jdautilities.command.Command
import com.jagrosh.jdautilities.command.CommandEvent
import com.mcmoddev.mmdbot.core.Utils
import kotlinx.coroutines.*
import me.shedaniel.linkie.*
import me.shedaniel.linkie.namespaces.MCPNamespace
import me.shedaniel.linkie.namespaces.MojangNamespace
import me.shedaniel.linkie.namespaces.YarnNamespace
import me.shedaniel.linkie.utils.MappingsQuery
import me.shedaniel.linkie.utils.QueryContext
import net.dv8tion.jda.api.EmbedBuilder
import net.dv8tion.jda.api.entities.MessageEmbed
import net.dv8tion.jda.api.events.interaction.ButtonClickEvent
import net.dv8tion.jda.api.hooks.ListenerAdapter
import net.dv8tion.jda.api.interactions.components.Button
import java.util.*

class CmdMappings(name: String, private val namespace: Namespace, vararg aliases: String?) : Command() {
    init {
        this.name = name.lowercase(Locale.ROOT)
        this.aliases = aliases
        help = "Search for something using $name."
    }

    /**
     * @param event The [CommandEvent] that triggered this Command.
     */
    override fun execute(event: CommandEvent) {
        if (!Utils.checkCommand(this, event)) return
        if (event.args.isEmpty()) {
            event.channel.sendMessage("No arguments given!").queue()
            return
        }

        val args = event.args.split(' ');

        val query = args[0];
        val version = args.getOrElse(1) { namespace.getDefaultVersion() }

        scope.launch {
            val provider = namespace.getProvider(version)
            var hasPerfectMatch = false
            var embeds = (MappingsQuery.queryClasses(
                QueryContext(
                    provider,
                    query
                )
            ).value.asSequence() + MappingsQuery.queryMember(
                QueryContext(provider, query)
            ) { it.members.asSequence() }.value.asSequence())
                .sortedBy { it.score }
                .also { seq ->
                    hasPerfectMatch = hasPerfectMatch || seq.any { it.score == 1.0 }
                }
                .filter { if (hasPerfectMatch) it.score == 1.0 else true }
                .mapIndexed { idx, it ->
                    async {
                        @Suppress("UNCHECKED_CAST")
                        when (it.value) {
                            is Class -> {
                                val value = it.value as Class
                                EmbedBuilder()
                                    .setTitle("$namespace Class mapping for $version:")
                                    .run {
                                        if (value.mappedName != null)
                                            addField("Mapped Name", "`${value.mappedName}`", false)
                                        else this
                                    }
                                    .addField("Intermediary/SRG Name", "`${value.intermediaryName}`", false)
                                    .addField("Obfuscated Name", "`${value.obfName.merged}`", false)
                            }
                            is Pair<*, *> -> {
                                val value = it.value as Pair<Class, MappingsMember>
                                EmbedBuilder()
                                    .setTitle(
                                        "$namespace ${
                                            when (value.second) {
                                                is Field -> "Field"
                                                is Method -> "Method"
                                                else -> "Member"
                                            }
                                        } mapping for $version:"
                                    )
                                    .addField("Mapped Name", "`${value.second.mappedName}`", false)
                                    .addField(
                                        "Intermediary/SRG Name",
                                        "`${value.second.intermediaryName}`",
                                        false
                                    )
                                    .addField("Obfuscated Name", "`${value.second.obfName.merged}`", false)
                                    .addField(
                                        "Member of Class",
                                        "`${value.first.mappedName ?: value.first.intermediaryName}`",
                                        false
                                    )
                                    .addField(
                                        "Descriptor",
                                        "`${value.second.getMappedDesc(provider.get())}`",
                                        false
                                    )
                                    .addField(
                                        "Mixin Target",
                                        "`L${value.first.optimumName};${value.second.optimumName}:${
                                            value.second.getMappedDesc(
                                                provider.get()
                                            )
                                        }`",
                                        false
                                    )
                            }
                            else -> {
                                EmbedBuilder().setDescription("???")
                            }
                        }.setFooter("Page ${idx + 1} | Powered by linkie-core").build()
                    }
                }.iterator()

            if (!embeds.hasNext()) {
                embeds = listOf(async { EmbedBuilder().setTitle("$namespace mapping for $version:").setDescription("No results found.").setFooter("Powered by linkie-core").build() }).iterator()
            }

            val msg = event.channel.sendMessageEmbeds(embeds.next().await()).apply {
                if (embeds.hasNext()) {
                    setActionRow(Button.primary("mappings-next", "Next"))
                }
            }.complete()

            ButtonListener.embedsForMessage[msg.idLong] = embeds

            delay(180000L)

            ButtonListener.embedsForMessage.remove(msg.idLong)
            event.channel.editMessageById(msg.id, msg).setActionRows().complete()
        }
    }

    companion object {
        val mappings = LinkieConfig.DEFAULT.copy(namespaces = listOf(YarnNamespace, MCPNamespace, MojangNamespace))
        val scope = CoroutineScope(Dispatchers.Default)

        @JvmStatic
        fun createCommands(): Array<CmdMappings> = arrayOf(
            CmdMappings("yarn", YarnNamespace, "y"),
            CmdMappings("mcp", MCPNamespace, "mcp"),
            CmdMappings("mojmap", MojangNamespace, "mm")
        )
    }

    object ButtonListener : ListenerAdapter() {
        val embedsForMessage: MutableMap<Long, Iterator<Deferred<MessageEmbed>>> = mutableMapOf()

        override fun onButtonClick(event: ButtonClickEvent) {
            scope.launch {
                if (event.componentId == "mappings-next") {
                    embedsForMessage[event.messageIdLong]?.let {
                        val newEmbed = it.next().await()
                        event.editMessageEmbeds(newEmbed).apply {
                            if (!it.hasNext()) {
                                setActionRow()
                            }
                        }.queue()
                    }
                }
            }
        }
    }
}
