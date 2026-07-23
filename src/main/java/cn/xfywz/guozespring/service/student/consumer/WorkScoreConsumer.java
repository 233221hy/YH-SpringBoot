package cn.xfywz.guozespring.service.student.consumer;

import cn.xfywz.guozespring.config.RabbitMQConfig;
import cn.xfywz.guozespring.entity.dto.WorkScoreMessage;
import cn.xfywz.guozespring.util.db.BuiltSql;
import cn.xfywz.guozespring.util.db.DatabaseUtil;
import com.rabbitmq.client.Channel;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Component
public class WorkScoreConsumer {

    @Autowired
    private DatabaseUtil databaseUtil;

    private final Jackson2JsonMessageConverter converter = new Jackson2JsonMessageConverter();

    @RabbitListener(queues = RabbitMQConfig.WORK_QUEUE_NAME,
            containerFactory = "batchContainerFactory")
    public void handleBatch(List<Message> messages, Channel channel) throws Exception {
        List<WorkScoreMessage> msgList = messages.stream()
                .map(m -> (WorkScoreMessage) converter.fromMessage(m))
                .toList();

        log.info("【批量算分-作业】收到 {} 条消息", msgList.size());

        Map<String, List<WorkScoreMessage>> grouped = msgList.stream()
                .collect(Collectors.groupingBy(
                        m -> m.getSchoolId() + "_" + m.getRecordId()));

        for (Map.Entry<String, List<WorkScoreMessage>> entry : grouped.entrySet()) {
            WorkScoreMessage first = entry.getValue().get(0);
            BigDecimal sum = entry.getValue().stream()
                    .map(WorkScoreMessage::getEarnedScore)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            try {
                databaseUtil.executeInTransaction(first.getSchoolId(), conn -> {
                    // 从 yee_work_answer 重新汇总总分（避免 MQ 重复消息导致增量累加超出满分）
                    databaseUtil.executeUpdate(conn, BuiltSql.of(
                            "UPDATE yee_work_record SET score = COALESCE((SELECT SUM(score) FROM yee_work_answer WHERE recordId = yee_work_record.id), 0), obScore = COALESCE((SELECT SUM(score) FROM yee_work_answer WHERE recordId = yee_work_record.id), 0) WHERE id = ? AND schoolId = ?",
                            first.getRecordId(), first.getSchoolId()));

                    long totalScore = databaseUtil.executeScalar(conn,
                            "SELECT score FROM yee_work_record WHERE id = ? AND schoolId = ?",
                            first.getRecordId(), first.getSchoolId());

                    Integer workId = first.getWorkId();
                    Integer userId = first.getUserId();
                    if (workId == null || userId == null) {
                        Map<String, Object> recordInfo = databaseUtil.executeQuery(conn,
                                BuiltSql.of("SELECT workId, userId FROM yee_work_record WHERE id = ? AND schoolId = ?",
                                        first.getRecordId(), first.getSchoolId()),
                                rs -> {
                                    try {
                                        if (rs.next()) {
                                            return Map.of(
                                                    "workId", rs.getInt("workId"),
                                                    "userId", rs.getInt("userId"));
                                        }
                                    } catch (Exception e) {
                                        // fall through
                                    }
                                    return Map.of();
                                });
                        if (recordInfo.containsKey("workId")) {
                            workId = (Integer) recordInfo.get("workId");
                            userId = (Integer) recordInfo.get("userId");
                        }
                    }

                    if (workId != null && userId != null) {
                        databaseUtil.executeUpdate(conn, BuiltSql.of(
                                "UPDATE yee_work_score SET finalScore = ?, state = 1, scored = 1 WHERE workId = ? AND userId = ? AND schoolId = ?",
                                BigDecimal.valueOf(totalScore), workId, userId, first.getSchoolId()));
                    }
                    return null;
                });
            } catch (Exception e) {
                log.error("【批量算分-作业】事务执行失败 schoolId={} recordId={}", first.getSchoolId(), first.getRecordId(), e);
            }
        }

        long lastTag = messages.get(messages.size() - 1).getMessageProperties().getDeliveryTag();
        channel.basicAck(lastTag, true);
        log.info("【批量算分-作业】完成，ACK deliveryTag={}", lastTag);
    }
}
