# 生产级 Kafka 消费者完整示例（Java）
这个示例完全遵循「**异步提交为主，同步提交兜底**」的核心策略，同时覆盖了生产环境必备的优雅关闭、Rebalance 安全处理、完善的异常捕获、可观测性等能力，所有关键设计都对应之前讲解的核心知识点，可直接修改后用于线上环境。

## 一、前置依赖
基于 Kafka 官方 Java 客户端，推荐使用稳定版，Maven 依赖如下：
```xml
<dependency>
    <groupId>org.apache.kafka</groupId>
    <artifactId>kafka-clients</artifactId>
    <version>3.6.1</version> <!-- 稳定兼容版，支持2.8.x及以上Kafka集群 -->
</dependency>
```

## 二、完整生产级代码
```java
import org.apache.kafka.clients.consumer.*;
import org.apache.kafka.common.TopicPartition;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.util.Collections;
import java.util.Properties;

/**
 * 生产级Kafka消费者示例
 * 核心策略：正常消费异步提交提吞吐，异常/退出/Rebalance同步提交兜底
 * 核心目标：杜绝消息丢失，最小化重复消费，保证业务最终一致性
 */
public class ProductionKafkaConsumer {
    // 基础配置（生产环境建议放到配置中心/配置文件）
    private static final String BOOTSTRAP_SERVERS = "127.0.0.1:9092"; // 集群地址
    private static final String TOPIC_NAME = "your_business_topic";     // 业务Topic
    private static final String CONSUMER_GROUP = "your_business_group"; // 消费组
    private static final Logger log = LoggerFactory.getLogger(ProductionKafkaConsumer.class);

    // 消费者运行状态标志位：用于优雅关闭
    private volatile boolean isRunning = true;
    private KafkaConsumer<String, String> consumer;

    public static void main(String[] args) {
        ProductionKafkaConsumer consumerApp = new ProductionKafkaConsumer();
        consumerApp.start();
    }

    /**
     * 启动消费者核心流程
     */
    public void start() {
        // 1. 初始化消费者配置
        Properties props = initConsumerConfig();
        consumer = new KafkaConsumer<>(props);

        // 2. 注册关闭钩子：处理kill -15 优雅关闭信号（生产环境必备）
        Runtime.getRuntime().addShutdownHook(new Thread(this::shutdown));

        try {
            // 3. 订阅Topic，绑定Rebalance监听器（核心兜底能力之一）
            consumer.subscribe(Collections.singletonList(TOPIC_NAME), new ConsumerRebalanceListener() {
                /**
                 * 分区撤销前触发：Rebalance前、消费者关闭前都会调用
                 * 核心作用：在交出分区所有权前，同步提交已处理完的Offset，兜底防重复消费
                 */
                @Override
                public void onPartitionsRevoked(TopicPartition partitions) {
                    log.info("分区即将被撤销，同步提交当前已处理的Offset，分区列表：{}", partitions);
                    try {
                        // 必须用同步提交：阻塞直到提交成功，确保交出分区前Offset已持久化
                        consumer.commitSync();
                    } catch (CommitFailedException e) {
                        log.error("分区撤销时Offset同步提交失败", e);
                    }
                }

                /**
                 * 分区分配完成后触发：可用于自定义Offset重置、初始化等操作
                 */
                @Override
                public void onPartitionsAssigned(TopicPartition partitions) {
                    log.info("成功分配到分区：{}", partitions);
                }
            });

            // 4. 核心消费主循环
            log.info("消费者启动成功，开始订阅Topic：{}", TOPIC_NAME);
            while (isRunning) {
                // 拉取消息：最长阻塞100ms，无消息则返回空集合
                ConsumerRecords<String, String> records = consumer.poll(Duration.ofMillis(100));

                // 无消息时直接跳过，不做无效操作
                if (records.isEmpty()) {
                    continue;
                }

                // 5. 业务消息处理（单条异常隔离，避免单条坏消息卡死整个消费流程）
                try {
                    for (ConsumerRecord<String, String> record : records) {
                        // ===================== 你的核心业务逻辑 =====================
                        // 示例：订单处理、数据入库、事件分发等
                        processBusiness(record);
                        // ============================================================
                    }
                } catch (Exception e) {
                    // 业务处理异常捕获：根据业务场景选择重试/死信队列/告警
                    log.error("业务消息处理发生异常", e);
                    // 【重要】业务处理失败时，绝对不能提交Offset！！！
                    // 直接continue，下次poll会重新拉取这批消息，保证消息不丢失
                    continue;
                }

                // 6. 正常流程：异步提交Offset（主流程提吞吐，不阻塞消费）
                consumer.commitAsync((offsets, exception) -> {
                    // 异步提交回调：仅做日志记录/告警，绝对不要在这里盲目重试！！！
                    if (exception != null) {
                        log.error("异步Offset提交失败，待提交Offset：{}", offsets, exception);
                    } else {
                        log.debug("异步Offset提交成功，已提交Offset：{}", offsets);
                    }
                });
            }

        } catch (Exception e) {
            // 捕获消费者级别的异常（如集群不可用、配置错误等）
            log.error("消费者运行发生致命异常", e);
        } finally {
            // 7. 最终兜底：无论循环是正常退出/异常退出，都会执行
            log.info("消费者即将关闭，执行最终同步Offset提交兜底");
            try {
                // 【核心兜底】阻塞式提交，直到成功/不可恢复错误，确保最后一批已处理消息的Offset不丢失
                consumer.commitSync();
            } catch (Exception e) {
                log.error("最终兜底同步Offset提交失败", e);
            } finally {
                // 关闭消费者，释放资源
                consumer.close();
                log.info("消费者已安全关闭");
            }
        }
    }

    /**
     * 优雅关闭：修改运行标志位，触发主循环退出，进入finally兜底逻辑
     */
    public void shutdown() {
        log.info("收到关闭信号，开始优雅关闭消费者");
        isRunning = false;
        // 唤醒阻塞的poll方法，加速循环退出
        consumer.wakeup();
    }

    /**
     * 初始化消费者核心配置（生产环境关键参数调优）
     */
    private Properties initConsumerConfig() {
        Properties props = new Properties();
        // 基础连接配置
        props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, BOOTSTRAP_SERVERS);
        props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName());
        props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName());
        props.put(ConsumerConfig.GROUP_ID_CONFIG, CONSUMER_GROUP);

        // ===================== 核心可靠性配置（重中之重） =====================
        // 1. 关闭自动Offset提交，完全手动控制，杜绝提前提交导致的消息丢失
        props.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, false);

        // 2. 单次拉取最大消息数：根据业务处理能力调整，避免拉取过多导致处理超时
        // 生产环境建议100-500，处理耗时越长，数值越小
        props.put(ConsumerConfig.MAX_POLL_RECORDS_CONFIG, 200);

        // 3. 单次拉取消息的最大处理超时时间：必须大于业务处理一批消息的最大耗时
        // 超过这个时间没发起poll，消费者会被踢出组，触发Rebalance
        props.put(ConsumerConfig.MAX_POLL_INTERVAL_MS_CONFIG, 300000); // 5分钟

        // 4. 消费者会话超时时间：超过这个时间没发心跳，Broker认为消费者宕机
        props.put(ConsumerConfig.SESSION_TIMEOUT_MS_CONFIG, 10000); // 10秒

        // 5. 首次消费策略：无已提交Offset时，从最早/最新开始消费
        // earliest：从头消费（避免消息丢失），latest：从最新开始消费（避免重复处理历史消息）
        props.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");

        // 6. 隔离级别：只读取已提交的事务消息（如果用了Kafka事务，必须配置）
        props.put(ConsumerConfig.ISOLATION_LEVEL_CONFIG, "read_committed");
        // =====================================================================

        return props;
    }

    /**
     * 业务消息处理逻辑示例
     * 【重要】业务处理必须保证幂等性！！！这是最终一致性的最终兜底
     */
    private void processBusiness(ConsumerRecord<String, String> record) {
        log.info("处理消息，分区：{}，偏移量：{}，key：{}，value：{}",
                record.partition(), record.offset(), record.key(), record.value());

        // 你的业务逻辑：比如写入数据库、调用下游服务、数据计算等
        // 幂等性建议：用消息唯一ID（订单号/消息全局ID）做数据库唯一键约束/分布式锁
    }
}
```

## 三、核心设计拆解（对应你的核心疑问）
### 1. 「异步提交为主」的设计逻辑
- 正常消费循环中，**只有业务处理完全成功后，才会发起`commitAsync`**，从根源上避免了“未处理完就提交Offset”的消息丢失问题。
- 异步提交不阻塞线程，提交请求发出去后，消费者可以立即拉取下一批消息，最大化消费吞吐量，完全适配高并发业务场景。
- 异步提交的回调仅做日志记录，不做重试：避免前一个提交失败、后一个提交已成功时，重试旧Offset导致的进度回退、重复消费。

### 2. 「同步提交兜底」的三重保障
代码里做了三层同步提交兜底，覆盖了所有可能导致Offset丢失的场景，完全解决你之前的疑问：
| 兜底位置 | 触发场景 | 核心作用 |
|----------|----------|----------|
| `finally` 代码块 | 消费者正常关闭、业务异常崩溃、致命错误导致循环退出 | 最后一道关卡，无论消费者因什么原因要退出，都会阻塞式提交最后一批已处理的Offset，确保提交成功后再关闭消费者 |
| Rebalance监听器`onPartitionsRevoked` | 消费组Rebalance（消费者扩缩容、Topic分区变更） | 在交出分区所有权之前，同步提交Offset，确保新消费者拿到分区后，从正确的位置开始消费，避免Rebalance导致的大规模重复消费 |
| 关闭钩子`ShutdownHook` | 服务发布、运维操作执行`kill -15`关闭进程 | 触发优雅关闭，修改运行标志位，让主循环正常退出，最终进入`finally`块执行同步提交，避免进程强制退出导致的Offset丢失 |

### 3. 生产环境异常隔离设计
- 单条消息的业务异常，只会被捕获记录，不会中断整个消费流程，也不会提交Offset，下次拉取会重新处理，保证消息不丢失。
- 整批消息处理失败时，直接跳过Offset提交，确保异常消息不会被标记为“已消费”，杜绝消息丢失。
- 所有提交异常都有日志记录，方便线上问题排查和告警配置。

## 四、生产环境最终补充提醒
1.  **业务幂等性是最终一致性的终极兜底**：无论Offset提交做的多完善，极端场景（如提交Offset成功后Broker宕机、副本未同步）仍可能出现重复消费，**必须在业务逻辑中实现幂等性**（唯一键约束、状态机控制、分布式锁等）。
2.  **异常消息处理**：对于业务处理永远失败的脏消息，不要无限重试，建议引入死信队列（DLQ），超过重试次数后写入死信队列，避免阻塞正常消费流程。
3.  **监控告警**：必须对消费者的积压量（Lag）、Offset提交失败次数、业务处理异常量、消费TPS配置监控告警，及时发现线上问题。
4.  **资源隔离**：核心业务Topic和非核心业务Topic使用独立的消费组，避免互相影响。
