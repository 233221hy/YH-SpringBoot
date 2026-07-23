package cn.xfywz.guozespring.service.student.consumer;

import cn.xfywz.guozespring.config.RabbitMQConfig;
import cn.xfywz.guozespring.entity.dto.ExamScoreMessage;
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
public class ExamScoreConsumer {

    @Autowired
    private DatabaseUtil databaseUtil;

    private final Jackson2JsonMessageConverter converter = new Jackson2JsonMessageConverter();

    @RabbitListener(queues = RabbitMQConfig.QUEUE_NAME,
            containerFactory = "batchContainerFactory")
    public void handleBatch(List<Message> messages, Channel channel) throws Exception {
        // 1. 反序列化
        List<ExamScoreMessage> msgList = messages.stream()
                .map(m -> (ExamScoreMessage) converter.fromMessage(m))
                .toList();

        log.info("【批量算分】收到 {} 条消息", msgList.size());

        // 2. 按 schoolId + recordId 分组聚合
        Map<String, List<ExamScoreMessage>> grouped = msgList.stream()
                .collect(Collectors.groupingBy(
                        m -> m.getSchoolId() + "_" + m.getRecordId()));

        // 3. 逐组事务更新
        for (Map.Entry<String, List<ExamScoreMessage>> entry : grouped.entrySet()) {
            ExamScoreMessage first = entry.getValue().get(0);
            BigDecimal sum = entry.getValue().stream()
                    .map(ExamScoreMessage::getEarnedScore)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            try {
                databaseUtil.executeInTransaction(first.getSchoolId(), conn -> {
                    // 从 yee_exam_answer 重新汇总总分（避免 MQ 重复消息导致增量累加超出满分）
                    databaseUtil.executeUpdate(conn, BuiltSql.of(
                            "UPDATE yee_exam_record SET score = COALESCE((SELECT SUM(score) FROM yee_exam_answer WHERE recordId = yee_exam_record.id), 0), obScore = COALESCE((SELECT SUM(score) FROM yee_exam_answer WHERE recordId = yee_exam_record.id), 0) WHERE id = ? AND schoolId = ?",
                            first.getRecordId(), first.getSchoolId()));

                    // 查询最新总分 + examId + userId
                    long totalScore = databaseUtil.executeScalar(conn,
                            "SELECT score FROM yee_exam_record WHERE id = ? AND schoolId = ?",
                            first.getRecordId(), first.getSchoolId());

                    // 查询 examId 和 userId
                    Integer examId = first.getExamId();
                    Integer userId = first.getUserId();
                    if (examId == null || userId == null) {
                        Map<String, Object> recordInfo = databaseUtil.executeQuery(conn,
                                BuiltSql.of("SELECT examId, userId FROM yee_exam_record WHERE id = ? AND schoolId = ?",
                                        first.getRecordId(), first.getSchoolId()),
                                rs -> {
                                    try {
                                        if (rs.next()) {
                                            return Map.of(
                                                    "examId", rs.getInt("examId"),
                                                    "userId", rs.getInt("userId"));
                                        }
                                    } catch (Exception e) {
                                        // fall through
                                    }
                                    return Map.of();
                                });
                        if (recordInfo.containsKey("examId")) {
                            examId = (Integer) recordInfo.get("examId");
                            userId = (Integer) recordInfo.get("userId");
                        }
                    }

                    // 更新 yee_exam_score
                    if (examId != null && userId != null) {
                        databaseUtil.executeUpdate(conn, BuiltSql.of(
                                "UPDATE yee_exam_score SET finalScore = ?, state = 1, scored = 1 WHERE examId = ? AND userId = ? AND schoolId = ?",
                                BigDecimal.valueOf(totalScore), examId, userId, first.getSchoolId()));
                    }
                    return null;
                });
            } catch (Exception e) {
                log.error("【批量算分】事务执行失败 schoolId={} recordId={}", first.getSchoolId(), first.getRecordId(), e);
            }
        }

        // 4. 批量 ACK
        long lastTag = messages.get(messages.size() - 1).getMessageProperties().getDeliveryTag();
        channel.basicAck(lastTag, true);
        log.info("【批量算分】完成，ACK deliveryTag={}", lastTag);
    }
}
