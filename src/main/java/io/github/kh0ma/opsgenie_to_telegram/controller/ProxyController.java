package io.github.kh0ma.opsgenie_to_telegram.controller;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestTemplate;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequiredArgsConstructor
@Slf4j
public class ProxyController {

    private static List<String> TELEGRAM_RESERVED_CHARACTERS = Arrays.asList("_", "*", "[", "]", "(", ")", "~", "`", ">", "#", "+", "-", "=", "|", "{", "}", ".", "!");

    private final ObjectMapper objectMapper;

    private static final String DEFAULT_OPSGENIE_APP_URL = "https://app.opsgenie.com";

    private static final String MARKDOWNV_2_FORMAT = """
            %s *__%s__*

            *%s*

            Find details at [opsgenie](%s)

            **> *Description*
            %s ||
            """;

    @PostMapping("/webhook")
    public ResponseEntity<String> postOperation(
            @RequestHeader("x-telegram-chat-id") String chatId,
            @RequestHeader("x-telegram-bot-token") String botToken,
            @RequestBody JsonNode requestPayload
    ) throws JsonProcessingException {
        String action = requestPayload.get("action").asText();
        String alertMessage = requestPayload.get("alert").get("message").asText();
        String alertId = requestPayload.get("alert").get("alertId").asText();
        String description = requestPayload.get("alert").get("description").asText();

        RestTemplate restTemplate = new RestTemplate();
        String telegramApiUrl = "https://api.telegram.org/bot" + botToken + "/sendMessage";

        ObjectNode objectNode = objectMapper.createObjectNode();
        objectNode.put("chat_id", chatId);
        objectNode.put("parse_mode", "MarkdownV2");
        objectNode.put("text", String.format(
                MARKDOWNV_2_FORMAT,
                getEmoji(action),
                action,
                escapeTelegramReservedCharacters(alertMessage),
                DEFAULT_OPSGENIE_APP_URL + "/alert/detail/" + alertId,
                prependStringLinesWithString(escapeTelegramReservedCharacters(description), "> "))
        );

        log.info(objectNode.toString());

        ResponseEntity<String> telegramResponse = restTemplate.postForEntity(telegramApiUrl, objectNode, String.class);

        if (!telegramResponse.getStatusCode().is2xxSuccessful()) {
            throw new RuntimeException("Failed to send message to telegram");
        }
        // Your handling logic here
        return ResponseEntity.ok("Success");
    }

    private static String getEmoji(String action) {
        return switch (action) {
            case "Create":
                yield "⭕";
            case "Acknowledge":
                yield "👀";
            case "Close":
                yield "✅";
            default:
                yield "🧐";
        };
    }

    private static String escapeTelegramReservedCharacters(String string) {
        for (String telegramReservedCharacter : TELEGRAM_RESERVED_CHARACTERS) {
            string = string.replace(telegramReservedCharacter, "\\" + telegramReservedCharacter);
        }
        return string;
    }

    private static String prependStringLinesWithString(String string, String prefix) {
        return string.lines()
                .map(line -> prefix + line)
                .collect(Collectors.joining(System.lineSeparator()));
    }
}
