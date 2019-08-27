package com.axibase.collector;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.javaprop.JavaPropsMapper;
import com.fasterxml.jackson.dataformat.javaprop.JavaPropsSchema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.paho.client.mqttv3.IMqttClient;
import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence;

import java.io.IOException;
import java.io.InputStream;
import java.time.Instant;
import java.util.Arrays;
import java.util.List;
import java.util.Properties;
import java.util.Random;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static java.nio.charset.StandardCharsets.UTF_8;

@Slf4j
public class RioSimulations {
    public static void main(String[] args) throws MqttException {
        final Configuration configuration = readConfiguration();
        final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(configuration.getClientsCount());
        for (int i = 0; i < configuration.getClientsCount(); i++) {
            final MqttClient demoPublisher = getClient(configuration.getBroker(), Integer.toString(i));
            final RioSimulationMessageSender pub = new RioSimulationMessageSender(demoPublisher, i, configuration);
            scheduler.scheduleAtFixedRate(pub, 0, configuration.getSimulation().getPublishPeriodMs(), TimeUnit.MILLISECONDS);
        }
    }

    private static Configuration readConfiguration() {
        final JavaPropsMapper mapper = new JavaPropsMapper();
        try {
            final Configuration configuration = mapper.readSystemPropertiesAs(JavaPropsSchema.emptySchema(), Configuration.class);
            log.info("Configuration {} successfully loaded from system properties", configuration);
            return configuration;
        } catch (IOException e) {
            log.warn("Failed to load configuration from system.properties. app.properties file will be used");
            try (InputStream input = RioSimulations.class.getResourceAsStream("/app.properties")) {
                Properties prop = new Properties();
                prop.load(input);
                final Configuration configuration = mapper.readPropertiesAs(prop, Configuration.class);
                log.info("Configuration {} successfully loaded from app.properties file", configuration);
                return configuration;
            } catch (IOException ex) {
                log.error("Failed to load App configuration");
                throw new IllegalStateException(ex);
            }
        }

    }


    private static MqttClient getClient(final BrokerConfiguration brokerConfiguration, final String id) throws MqttException {
        MqttClient demoPublisher = new MqttClient(brokerConfiguration.getUrl(),
                "demo-axibase" + id, new MemoryPersistence());
        final MqttConnectOptions options = new MqttConnectOptions();
        options.setCleanSession(false);
        options.setUserName(brokerConfiguration.getUsername());
        options.setPassword(brokerConfiguration.getPassword().toCharArray());
        options.setAutomaticReconnect(true);
        demoPublisher.connect(options);
        return demoPublisher;
    }

    public static class RioSimulationMessageSender implements Runnable {
        private final IMqttClient client;
        private final int index;
        private final String topic;
        private final AtomicInteger count = new AtomicInteger(0);
        private final int elementsCount;
        private final int randomLimit;
        private ObjectMapper objectMapper = new ObjectMapper();
        private Random random = new Random();

        public RioSimulationMessageSender(IMqttClient client, int index, Configuration configuration) {
            this.client = client;
            this.index = index;
            this.topic = configuration.getTopic();
            this.elementsCount = configuration.getSimulation().getElements();
            this.randomLimit = configuration.getSimulation().getRandomLimit();
        }

        @Override
        public void run() {
            if (!client.isConnected()) {
                return;
            }
            final long time = System.currentTimeMillis();

            try {
                final List<RioData> data = IntStream.range(0, random.nextInt(elementsCount - 1) + 1)
                        .mapToObj(i -> randomRioData(time))
                        .collect(Collectors.toList());

                final String json = objectMapper.writeValueAsString(data);
                MqttMessage msg = new MqttMessage(json.getBytes(UTF_8));
                msg.setQos(1);
                msg.setRetained(true);
                client.publish(topic, msg);
                count.incrementAndGet();
                if (log.isDebugEnabled()) {
                    log.debug("JSON Messages sent {}", count.get());
                }
                if (count.get() % 100 == 0) {
                    log.info("JSON Messages sent {}", count.get());
                }
            } catch (MqttException | JsonProcessingException e) {
                log.error("", e);
            }
        }

        private RioData randomRioData(final long time) {
            final int shift = random.nextInt(200);
            final int randomMax = random.nextInt(this.randomLimit / 2) + this.randomLimit / 2;
            final int value = random.nextInt(randomMax);
            final List<RioTag> tags = Arrays.asList(
                    new RioTag("Top.Server", "Random-" + randomMax),
                    new RioTag("Simulate", String.format("Channel1.Device%d.Random(1000,1,%d)", index, randomMax))
            );
            return RioData.builder()
                    .tags(tags)
                    .time(Instant.ofEpochMilli(time + shift).toString())
                    .value(value)
                    .type("int32")
                    .build();
        }
    }


    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    @Data
    public static class RioTag {
        String ns;
        String s;
    }

    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    @Data
    private static class RioData {
        private List<RioTag> tags;
        private int value;
        private String type = "int32";
        private String time;
    }
}
