package de.slimecloud.april.features.april;

import club.minnced.discord.webhook.external.JDAWebhookClient;
import club.minnced.discord.webhook.send.WebhookMessageBuilder;
import de.slimecloud.april.main.Bot;
import de.slimecloud.april.main.SlimeEmoji;
import lombok.RequiredArgsConstructor;
import net.dv8tion.jda.api.entities.channel.attribute.IWebhookContainer;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.requests.RestAction;
import org.jetbrains.annotations.NotNull;

import java.util.Arrays;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@RequiredArgsConstructor
public class AprilListener extends ListenerAdapter {
	private final Bot bot;

	@Override
	public void onMessageReceived(@NotNull MessageReceivedEvent event) {
		if(!event.isFromGuild() || event.getAuthor().isBot() || !event.getMessage().getAttachments().isEmpty() || !event.getMessage().getEmbeds().isEmpty()) return;

		bot.loadGuild(event.getGuild()).getApril().ifPresent(config -> {
			if(config.getMessage() == null) return;

			String content = event.getMessage().getContentRaw().toLowerCase();

			List<String> message = Arrays.asList(content.split(" "));
			List<String> words = Arrays.asList(config.getMessage().toLowerCase().split(" "));

			if(message.stream().noneMatch(words::contains)) return;
			event.getMessage().delete().queue();

			Matcher matcher = Pattern.compile("(?<=\\s|^)(?<word>" + words.stream().map(Pattern::quote).collect(Collectors.joining("|")) + ")(?=\\s|$)", Pattern.CASE_INSENSITIVE).matcher(content);
			StringBuilder result = new StringBuilder();

			while(matcher.find()) {
				matcher.appendReplacement(result, " " + Matcher.quoteReplacement(SlimeEmoji.SUS.getEmoji(event.getGuild()).getFormatted().repeat(matcher.group("word").length())) + " ");
			}

			matcher.appendTail(result);

			getWebhook(event.getChannel().asTextChannel()).queue(webhook ->
					webhook.send(new WebhookMessageBuilder()
							.setUsername(event.getMember().getEffectiveName())
							.setAvatarUrl(event.getMember().getEffectiveAvatarUrl())
							.setContent(result.toString())
							.build()
					)
			);
		});
	}

	@NotNull
	private RestAction<JDAWebhookClient> getWebhook(@NotNull IWebhookContainer channel) {
		return channel.retrieveWebhooks().flatMap(hooks -> hooks.stream()
				.filter(w -> w.getName().equals("1. April"))
				.findAny()
				.map(bot::wrap)
				.orElse(channel.createWebhook("1. April"))
		).map(w -> JDAWebhookClient.withUrl(w.getUrl()));
	}
}