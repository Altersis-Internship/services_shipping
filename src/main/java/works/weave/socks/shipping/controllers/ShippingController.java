package works.weave.socks.shipping.controllers;

import com.rabbitmq.client.Channel;
import org.springframework.amqp.AmqpException;
import org.springframework.amqp.rabbit.core.ChannelCallback;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import works.weave.socks.shipping.entities.HealthCheck;
import works.weave.socks.shipping.entities.Shipment;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;

@RestController
@RequestMapping("/shipping")
public class ShippingController {

    private static final Logger logger = LoggerFactory.getLogger(ShippingController.class);

    private final RabbitTemplate rabbitTemplate;

    @Value("${http.timeout:5}")
    private long timeout;

    // Simulation config flags
    @Value("${simulate.latency:false}")
    private boolean simulateLatency;

    @Value("${simulate.cpu:false}")
    private boolean simulateCpu;

    @Value("${simulate.leak:false}")
    private boolean simulateLeak;

    @Value("${simulate.thread:false}")
    private boolean simulateThread;

    @Value("${simulate.deadlock:false}")
    private boolean simulateDeadlock;

    @Value("${simulate.error:false}")
    private boolean simulateError;

    private static int successfulOrderCount = 0;
    private static final List<byte[]> memoryLeakList = new CopyOnWriteArrayList<>();

    public ShippingController(RabbitTemplate rabbitTemplate) {
        this.rabbitTemplate = rabbitTemplate;
    }

    @GetMapping
    public String getShipping() {
        return "GET ALL Shipping Resource.";
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public Shipment postShipping(@RequestBody Shipment shipment) {
        logger.info("Processing POST request. Current order count: {}", successfulOrderCount);
        try {
            simulateProblemsIfEnabled();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            logger.warn("Thread was interrupted during simulation");
        }

        try {
            logger.info("Adding shipment to queue...");
            rabbitTemplate.convertAndSend("shipping-task", shipment);
        } catch (Exception e) {
            logger.error("Unable to add to queue (the queue may be down). Accepting anyway.");
        }

        successfulOrderCount++;
        logger.info("Order count incremented to: {}", successfulOrderCount);

        return shipment;
    }

    @GetMapping("/count")
    public Map<String, Integer> getSuccessfulOrderCount() {
        Map<String, Integer> response = new HashMap<>();
        response.put("successfulOrderCount", successfulOrderCount);
        return response;
    }

    @GetMapping("/health")
    @ResponseStatus(HttpStatus.OK)
    public Map<String, List<HealthCheck>> getHealth() {
        Map<String, List<HealthCheck>> map = new HashMap<>();
        List<HealthCheck> healthChecks = new ArrayList<>();
        Date dateNow = Calendar.getInstance().getTime();

        HealthCheck rabbitmq = new HealthCheck("shipping-rabbitmq", "OK", dateNow);
        HealthCheck app = new HealthCheck("shipping", "OK", dateNow);

        try {
            rabbitTemplate.execute((ChannelCallback<String>) channel -> {
                Map<String, Object> serverProperties = channel.getConnection().getServerProperties();
                return serverProperties.get("version").toString();
            });
        } catch (AmqpException e) {
            rabbitmq.setStatus("err");
        }

        healthChecks.add(rabbitmq);
        healthChecks.add(app);
        map.put("health", healthChecks);
        return map;
    }

    private void simulateProblemsIfEnabled() throws InterruptedException {
        if (simulateLatency) {
            Thread.sleep(3000); // 3 seconds
            logger.warn("🕒 Simulated latency (3s)");
        }

        if (simulateCpu) {
            for (int i = 0; i < 5_000_0000; i++) {
                Math.log(Math.sqrt(i + 1));
            }
            logger.warn("🔥 Simulated CPU spike");
        }

        if (simulateLeak) {
            try {
                byte[] leak = new byte[1024 * 1024 * 1024];
                memoryLeakList.add(leak);
                logger.warn("💾 Simulated memory leak ({} blocks)", memoryLeakList.size());
            } catch (OutOfMemoryError e) {
                logger.error("💣 Memory leak simulation caused OOM");
            }
        }

        if (simulateThread) {
            new Thread(() -> {
                while (true) {
                    try {
                        Thread.sleep(100000);
                    } catch (InterruptedException ignored) {
                    }
                }
            }).start();
            logger.warn("🧵 Simulated thread creation (WARNING: Thread leak!)");
        }

        if (simulateDeadlock) {
            final Object lock1 = new Object();
            final Object lock2 = new Object();

            Thread t1 = new Thread(() -> {
                synchronized (lock1) {
                    try {
                        Thread.sleep(100);
                    } catch (InterruptedException ignored) {}
                    synchronized (lock2) {
                        logger.warn("🔒 Thread 1 acquired both locks");
                    }
                }
            });

            Thread t2 = new Thread(() -> {
                synchronized (lock2) {
                    try {
                        Thread.sleep(100);
                    } catch (InterruptedException ignored) {}
                    synchronized (lock1) {
                        logger.warn("🔒 Thread 2 acquired both locks");
                    }
                }
            });

            t1.start();
            t2.start();

            logger.warn("🔒 Simulated deadlock launched with 2 threads");
        }

        if (simulateError && successfulOrderCount >= 4) {
            logger.warn("💥 Simulating endpoint failure (6th request or more)");
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "Endpoint is down");
        }
    }
}