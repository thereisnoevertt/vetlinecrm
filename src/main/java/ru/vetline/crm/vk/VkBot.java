package ru.vetline.crm.vk;

import com.vk.api.sdk.client.VkApiClient;
import com.vk.api.sdk.client.actors.GroupActor;
import com.vk.api.sdk.httpclient.HttpTransportClient;
import com.vk.api.sdk.objects.messages.Message;
import com.vk.api.sdk.objects.groups.responses.GetLongPollServerResponse;
import com.vk.api.sdk.objects.groups.LongPollEvents;
import com.vk.api.sdk.queries.messages.MessagesSendQuery;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;

/**
 * VK-бот сообщества «Ветлайн ВК». Реализует:
 *  • рассылку уведомлений клиентам через messages.send;
 *  • приём входящих сообщений через Bots Long Poll API
 *    (выдача user_id клиенту по команде /start или «Старт»);
 *  • автоматический рестарт LongPoll-сессии при ошибках.
 *
 * Активируется только если задан VK_GROUP_TOKEN ≠ REPLACE_ME.
 */
@Component
@ConditionalOnExpression("!'${vk.group.token}'.equals('REPLACE_ME')")
public class VkBot {

    private static final Logger LOG = LoggerFactory.getLogger(VkBot.class);

    private final VkApiClient vk = new VkApiClient(new HttpTransportClient());
    private final ObjectMapper json = new ObjectMapper();
    private final RestTemplate http = new RestTemplate();

    private final GroupActor actor;
    private final int groupId;

    private volatile boolean running = false;
    private Thread pollerThread;

    public VkBot(@Value("${vk.group.token}") String token,
                 @Value("${vk.group.id}") int groupId) {
        this.groupId = groupId;
        this.actor = new GroupActor(groupId, token);
    }

    @PostConstruct
    public void start() {
        running = true;
        pollerThread = new Thread(this::runLongPoll, "vk-longpoll");
        pollerThread.setDaemon(true);
        pollerThread.start();
        LOG.info("VK-бот запущен для сообщества id={}", groupId);
    }

    @PreDestroy
    public void stop() {
        running = false;
        if (pollerThread != null) pollerThread.interrupt();
        LOG.info("VK-бот остановлен");
    }

    /** Цикл Long Poll API. При ошибках 1/2/3 переинициализирует ts/key/server. */
    private void runLongPoll() {
        String server = null, key = null, ts = null;
        while (running) {
            try {
                if (server == null) {
                    GetLongPollServerResponse cfg = vk.groupsLongPoll()
                            .getLongPollServer(actor, groupId).execute();
                    server = cfg.getServer();
                    key    = cfg.getKey();
                    ts     = cfg.getTs();
                    LOG.info("VK LongPoll: server инициализирован, ts={}", ts);
                }
                String url = server + "?act=a_check&key=" + key + "&ts=" + ts + "&wait=25";
                String body = http.getForObject(url, String.class);
                JsonNode resp = json.readTree(body);

                if (resp.has("failed")) {
                    int failed = resp.get("failed").asInt();
                    if (failed == 1) {        // ts устарел
                        ts = resp.get("ts").asText();
                    } else {                  // 2, 3 — нужны новые key/server
                        server = null;
                    }
                    continue;
                }

                ts = resp.get("ts").asText();
                JsonNode updates = resp.get("updates");
                if (updates != null && updates.isArray()) {
                    for (JsonNode upd : updates) {
                        handleUpdate(upd);
                    }
                }
            } catch (Exception e) {
                if (e instanceof InterruptedException) {
                    Thread.currentThread().interrupt();
                    return;
                }
                LOG.error("VK LongPoll ошибка: {}", e.getMessage());
                try { Thread.sleep(5000); } catch (InterruptedException ie) { return; }
                server = null; // переинициализация
            }
        }
    }

    private void handleUpdate(JsonNode upd) {
        String type = upd.path("type").asText();
        if (!"message_new".equals(type)) return;
        JsonNode message = upd.path("object").path("message");
        long fromId = message.path("from_id").asLong();
        String text = message.path("text").asText("");

        if (text.equalsIgnoreCase("/start") ||
            text.equalsIgnoreCase("Старт") ||
            text.equalsIgnoreCase("Начать")) {
            send(fromId,
                "Здравствуйте! Это бот сообщества «Ветлайн ВК».\n\n" +
                "Здесь вы будете получать уведомления о статусе ваших заявок.\n" +
                "Ваш VK ID: " + fromId + "\n\n" +
                "Сообщите этот номер вашему менеджеру, чтобы подключить уведомления.");
        }
    }

    /** Отправить сообщение клиенту. Бросает RuntimeException при сбое. */
    public void sendMessage(long vkUserId, String text) {
        try {
            MessagesSendQuery q = vk.messages().send(actor)
                    .userId((int) vkUserId)
                    .message(text)
                    .randomId(ThreadLocalRandom.current().nextInt());
            Object result = q.execute();
            LOG.debug("VK → user_id={} ({})", vkUserId, result);
        } catch (Exception e) {
            throw new RuntimeException(
                    "VK send error to " + vkUserId + ": " + e.getMessage(), e);
        }
    }

    /** Тихая отправка (для команды /start). */
    private void send(long vkUserId, String text) {
        try { sendMessage(vkUserId, text); }
        catch (Exception e) { LOG.error("send: {}", e.getMessage()); }
    }
}
