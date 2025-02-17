package com.sap.tamagotchi.publisher;


import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.api.core.ApiFuture;
import com.google.api.core.ApiFutures;
import com.google.cloud.ServiceOptions;
import com.google.cloud.pubsub.v1.Publisher;
import com.google.protobuf.ByteString;
import com.google.pubsub.v1.ProjectTopicName;
import com.google.pubsub.v1.PubsubMessage;
import com.sap.tamagotchi.model.IoTMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.lang.invoke.MethodHandles;
import java.util.ArrayList;
import java.util.List;

@Service
public class PublisherService {

    private static final Logger LOGGER = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    // use the default project id
    private static final String PROJECT_ID = ServiceOptions.getDefaultProjectId();
    private final ObjectMapper mapper;

    @Autowired
    public PublisherService(ObjectMapper mapper) {
        this.mapper = mapper;
    }

    public void publish(IoTMessage message) throws Exception {
        if (message == null) {
            LOGGER.info("received null message");
            return;
        }
        String topicId = message.getTopic();
        ProjectTopicName topicName = ProjectTopicName.of(PROJECT_ID, topicId);
        Publisher publisher = null;
        List<ApiFuture<String>> futures = new ArrayList<>();

        try {
            String stringMessage = mapper.writeValueAsString(message);
            // Create a publisher instance with default settings bound to the topic
            publisher = Publisher.newBuilder(topicName).build();
            LOGGER.info("publish to topic" + publisher.getTopicNameString());

            // convert message to bytes
            ByteString data = ByteString.copyFromUtf8(stringMessage);
            PubsubMessage pubsubMessage = PubsubMessage.newBuilder()
                    .setData(data)
                    .build();

            LOGGER.info("publish to message" + stringMessage);

            // Schedule a message to be published. Messages are automatically batched.
            ApiFuture<String> future = publisher.publish(pubsubMessage);
            futures.add(future);

        } finally {
            // Wait on any pending requests
            List<String> messageIds = ApiFutures.allAsList(futures).get();

            for (String messageId : messageIds) {
                System.out.println(messageId);
                LOGGER.info("publish successful : " + messageId);
            }

            if (publisher != null) {
                // When finished with the publisher, shutdown to free up resources.
                publisher.shutdown();
            }

            if (messageIds.isEmpty())
                LOGGER.info("no messages published ");
        }
    }
}
