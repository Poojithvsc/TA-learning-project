package com.example.kafka;

import com.example.kafka.config.KafkaConfig;
import com.example.kafka.consumer.KafkaConsumer;
import com.example.kafka.model.Message;
import com.example.kafka.producer.KafkaProducer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Scanner;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class KafkaApplication {
    private static final Logger logger = LoggerFactory.getLogger(KafkaApplication.class);

    private static final String DEMO_MODE = "demo";
    private static final String PRODUCER_MODE = "producer";
    private static final String CONSUMER_MODE = "consumer";
    private static final String INTERACTIVE_MODE = "interactive";

    public static void main(String[] args) {
        logger.info("Starting Kafka Producer-Consumer Application");

        String mode = args.length > 0 ? args[0] : INTERACTIVE_MODE;

        switch (mode.toLowerCase()) {
            case DEMO_MODE:
                runDemo();
                break;
            case PRODUCER_MODE:
                runProducerOnly();
                break;
            case CONSUMER_MODE:
                runConsumerOnly();
                break;
            case INTERACTIVE_MODE:
            default:
                runInteractive();
                break;
        }
    }

    private static void runDemo() {
        logger.info("Running demo mode - automated producer and consumer");

        ExecutorService executor = Executors.newFixedThreadPool(2);

        try (KafkaProducer producer = new KafkaProducer();
             KafkaConsumer consumer = new KafkaConsumer()) {

            executor.submit(() -> {
                consumer.start((message, record) -> {
                    logger.info("DEMO - Processed message: ID={}, Content={}, Sender={}",
                            message.getId(), message.getContent(), message.getSender());
                });
            });

            Thread.sleep(2000);

            executor.submit(() -> {
                try {
                    int messagesPerSecond = KafkaConfig.getMessagesPerSecond();
                    int delayMs = 1000 / messagesPerSecond;

                    for (int i = 1; i <= 10; i++) {
                        Message message = new Message(
                                UUID.randomUUID().toString(),
                                "Demo message " + i + " - Hello from Kafka!",
                                "DemoApp"
                        );

                        producer.sendMessage(message);
                        logger.info("DEMO - Sent message {}: {}", i, message.getContent());

                        if (i < 10) {
                            Thread.sleep(delayMs);
                        }
                    }

                    logger.info("DEMO - All messages sent");

                } catch (Exception e) {
                    logger.error("Error in demo producer", e);
                }
            });

            Thread.sleep(15000);

        } catch (Exception e) {
            logger.error("Error in demo mode", e);
        } finally {
            shutdown(executor);
        }
    }

    private static void runProducerOnly() {
        logger.info("Running producer-only mode");

        try (KafkaProducer producer = new KafkaProducer();
             Scanner scanner = new Scanner(System.in)) {

            System.out.println("Producer started. Type messages (or 'quit' to exit):");

            String input;
            while (!(input = scanner.nextLine()).equals("quit")) {
                if (!input.trim().isEmpty()) {
                    Message message = new Message(
                            UUID.randomUUID().toString(),
                            input,
                            "Console"
                    );

                    producer.sendMessage(message);
                    System.out.println("Message sent: " + input);
                }
            }

        } catch (Exception e) {
            logger.error("Error in producer mode", e);
        }
    }

    private static void runConsumerOnly() {
        logger.info("Running consumer-only mode");

        try (KafkaConsumer consumer = new KafkaConsumer()) {
            System.out.println("Consumer started. Listening for messages (Ctrl+C to exit):");

            consumer.start((message, record) -> {
                System.out.printf("Received: [%s] %s (from %s)%n",
                        message.getId().substring(0, 8), message.getContent(), message.getSender());
            });

        } catch (Exception e) {
            logger.error("Error in consumer mode", e);
        }
    }

    private static void runInteractive() {
        logger.info("Running interactive mode");

        try (Scanner scanner = new Scanner(System.in)) {
            while (true) {
                System.out.println("\n=== Kafka Producer-Consumer Demo ===");
                System.out.println("1. Run Demo (automated)");
                System.out.println("2. Producer Only");
                System.out.println("3. Consumer Only");
                System.out.println("4. Exit");
                System.out.print("Choose an option: ");

                String choice = scanner.nextLine().trim();

                switch (choice) {
                    case "1":
                        runDemo();
                        break;
                    case "2":
                        runProducerOnly();
                        break;
                    case "3":
                        runConsumerOnly();
                        break;
                    case "4":
                        System.out.println("Goodbye!");
                        return;
                    default:
                        System.out.println("Invalid option. Please try again.");
                }
            }

        } catch (Exception e) {
            logger.error("Error in interactive mode", e);
        }
    }

    private static void shutdown(ExecutorService executor) {
        try {
            logger.info("Shutting down executor service...");
            executor.shutdown();
            if (!executor.awaitTermination(5, TimeUnit.SECONDS)) {
                executor.shutdownNow();
            }
        } catch (InterruptedException e) {
            executor.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }
}