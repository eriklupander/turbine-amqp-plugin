/*
 * Copyright 2017 eriklupander
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package se.lup.turbine.discovery.amqp;

import com.rabbitmq.client.AMQP;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;
import com.rabbitmq.client.DefaultConsumer;
import com.rabbitmq.client.Envelope;
import org.codehaus.jackson.map.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import rx.Observable;
import rx.subjects.PublishSubject;

import java.io.IOException;
import java.net.URISyntaxException;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.util.concurrent.TimeoutException;

final class AmqpInstanceDiscovery {

    private static final Logger logger = LoggerFactory.getLogger(AmqpInstanceDiscovery.class);

    private static final String AMQP_BROKER_KEY = "amqp.broker.url";
    private static final String DEFAULT_AMQP_BROKER_URL = "amqp://guest:guest@192.168.99.100:5672";

    private static final String DISCOVERY_QUEUE_KEY = "discovery.queue";
    private static final String DEFAULT_DISCOVERY_QUEUE_NAME = "discovery";

    private static final String CLUSTER_NAME_KEY = "cluster.name";
    private static final String DEFAULT_CLUSTER_NAME = "swarm";

    private static final String CONSUMER_TAG_KEY = "consumer.tag";
    private static final String DEFAULT_CONSUMER_TAG = "myConsumerTag";

    private String amqpBrokerUrl;
    private String discoveryQueue;
    private String clusterName;
    private String consumerTag;

    private PublishSubject<AmqpInstance> subject;

    AmqpInstanceDiscovery() {

        configureFromEnv();

        subject = PublishSubject.create();

        try {
            ConnectionFactory factory = new ConnectionFactory();
            factory.setUri(amqpBrokerUrl);
            Connection conn = factory.newConnection();
            Channel channel = conn.createChannel();
            boolean autoAck = true;
            channel.basicConsume(discoveryQueue, autoAck, consumerTag,
                    new DefaultConsumer(channel) {
                        @Override
                        public void handleDelivery(String consumerTag,
                                                   Envelope envelope,
                                                   AMQP.BasicProperties properties,
                                                   byte[] body)
                                throws IOException {
                            DiscoveryToken token = new ObjectMapper().readValue(body, DiscoveryToken.class);
                            AmqpInstance.Status status = AmqpInstance.Status.valueOf(token.getState());

                            AmqpInstance amqpInstance = new AmqpInstance(clusterName, status, token.getAddress(), 8181);
                            if (subject != null) {
                                subject.onNext(amqpInstance);
                            }
                        }
                    });
        } catch (URISyntaxException | KeyManagementException | NoSuchAlgorithmException | IOException | TimeoutException e) {
            e.printStackTrace();
        }
    }

    private void configureFromEnv() {
        amqpBrokerUrl = System.getProperty(AMQP_BROKER_KEY) != null ? System.getProperty(AMQP_BROKER_KEY).trim() : DEFAULT_AMQP_BROKER_URL;
        discoveryQueue = System.getProperty(DISCOVERY_QUEUE_KEY) != null ? System.getProperty(DISCOVERY_QUEUE_KEY).trim() : DEFAULT_DISCOVERY_QUEUE_NAME;
        clusterName = System.getProperty(CLUSTER_NAME_KEY) != null ? System.getProperty(CLUSTER_NAME_KEY).trim() : DEFAULT_CLUSTER_NAME;
        consumerTag = System.getProperty(CONSUMER_TAG_KEY) != null ? System.getProperty(CONSUMER_TAG_KEY).trim() : DEFAULT_CONSUMER_TAG;

        System.out.println("Starting Turbine with the following configuration:");
        System.out.println(AMQP_BROKER_KEY + "\t\t" + amqpBrokerUrl);
        System.out.println(DISCOVERY_QUEUE_KEY + "\t\t" + discoveryQueue);
        System.out.println(CLUSTER_NAME_KEY + "\t\t" + clusterName);
        System.out.println(CONSUMER_TAG_KEY + "\t\t" + consumerTag);
    }

    Observable<AmqpInstance> getInstanceEvents() {
        subject.subscribe(System.out::println);
        return subject;
    }
}