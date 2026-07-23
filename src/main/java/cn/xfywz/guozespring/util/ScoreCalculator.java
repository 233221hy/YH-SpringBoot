package cn.xfywz.guozespring.util;

import com.fasterxml.jackson.databind.ObjectMapper;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 题目算分工具类
 * 考试与作业共用
 */
public class ScoreCalculator {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    public static class Option {
        public String idx;
        public double scale;
        public String answer;

        public Option(String idx, double scale, String answer) {
            this.idx = idx;
            this.scale = scale;
            this.answer = answer;
        }
    }

    public static class Topic {
        public Integer id;
        public Integer score;
        public List<Option> options;

        public Topic(Integer id, Integer score, List<Option> options) {
            this.id = id;
            this.score = score;
            this.options = options;
        }
    }

    /**
     * 计算用户作答得分
     *
     * @param maps    题目列表（包含 id, score, option）
     * @param answers 用户作答列表（包含 topicId, answer）
     * @return 包含得分、状态、正确答案等信息的结果列表
     */
    public static List<Map<String, Object>> calculateAnswerScores(
            List<Map<String, Object>> maps,
            List<Map<String, Object>> answers) {

        Map<Integer, Topic> topicMap = maps.stream()
                .filter(Objects::nonNull)
                .collect(Collectors.toMap(
                        m -> parseInteger(m.get("id")),
                        m -> {
                            Integer id = parseInteger(m.get("id"));
                            Integer score = parseInteger(m.get("score"));
                            Object optionObj = m.get("option");
                            List<Option> options = new ArrayList<>();
                            if (optionObj instanceof List) {
                                @SuppressWarnings("unchecked")
                                List<Map<String, Object>> optionList = (List<Map<String, Object>>) optionObj;
                                options = optionList.stream()
                                        .map(o -> new Option(
                                                o.get("idx") != null ? o.get("idx").toString() : "",
                                                parseScale(o.get("scale")),
                                                o.get("answer") != null ? o.get("answer").toString() : null
                                        ))
                                        .collect(Collectors.toList());
                            }
                            return new Topic(id, score != null ? score : 0, options);
                        },
                        (existing, replacement) -> existing,
                        LinkedHashMap::new
                ));

        return answers.stream()
                .filter(Objects::nonNull)
                .map(answerItem -> {
                    Map<String, Object> resultItem = new HashMap<>(answerItem);
                    try {
                        Object topicIdObj = answerItem.get("topicId");
                        Integer topicId = null;
                        if (topicIdObj instanceof Number) {
                            topicId = ((Number) topicIdObj).intValue();
                        } else if (topicIdObj != null) {
                            topicId = Integer.valueOf(topicIdObj.toString());
                        }
                        if (topicId == null) {
                            return buildEmptyAnswerResult(resultItem, new Topic(null, 0, Collections.emptyList()), 3, BigDecimal.ZERO);
                        }

                        Topic topic = topicMap.get(topicId);
                        if (topic == null) {
                            return buildEmptyAnswerResult(resultItem, new Topic(null, 0, Collections.emptyList()), 3, BigDecimal.ZERO);
                        }

                        Object answerObj = answerItem.get("answer");

                        boolean isBlankAnswer = false;
                        Map<String, Object> userAnswerMap = new HashMap<>();

                        if (answerObj instanceof Map) {
                            @SuppressWarnings("unchecked")
                            Map<String, Object> map = (Map<String, Object>) answerObj;
                            if (!map.isEmpty() && map.keySet().stream().anyMatch(k ->
                                    k != null && k.toString().matches("a\\d+"))) {
                                isBlankAnswer = true;
                                userAnswerMap = map;
                            }
                        } else if (answerObj instanceof String) {
                            String str = ((String) answerObj).trim();
                            if (str.startsWith("{") && str.endsWith("}")) {
                                try {
                                    userAnswerMap = OBJECT_MAPPER.readValue(str, Map.class);
                                    if (userAnswerMap.keySet().stream().anyMatch(k -> k != null && k.toString().matches("a\\d+"))) {
                                        isBlankAnswer = true;
                                    }
                                } catch (Exception ignored) {}
                            }
                        }

                        if (isBlankAnswer) {
                            return handleBlankQuestion(resultItem, topic, userAnswerMap);
                        } else {
                            return handleSelectionQuestion(resultItem, topic, answerObj);
                        }

                    } catch (Exception e) {
                        e.printStackTrace();
                        resultItem.put("selectedAnswer", new ArrayList<>());
                        resultItem.put("correctAnswer", new ArrayList<>());
                        resultItem.put("correctStatus", 0);
                        resultItem.put("totalScore", 0);
                        resultItem.put("earnedScore", BigDecimal.ZERO);
                        return resultItem;
                    }
                })
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
    }

    public static BigDecimal extractEarnedScore(List<Map<String, Object>> scoreList) {
        for (Map<String, Object> item : scoreList) {
            Object es = item.get("earnedScore");
            if (es instanceof BigDecimal bd) return bd;
            if (es instanceof Number n) return BigDecimal.valueOf(n.doubleValue());
        }
        return BigDecimal.ZERO;
    }

    private static Map<String, Object> handleBlankQuestion(
            Map<String, Object> resultItem,
            Topic topic,
            Map<String, Object> userAnswerMap) {

        int totalBlanks = topic.options.size();
        int correctCount = 0;
        BigDecimal totalEarned = BigDecimal.ZERO;

        List<Map<String, Object>> correctAnswerList = new ArrayList<>();
        List<Map<String, Object>> userAnswerList = new ArrayList<>();

        for (Option opt : topic.options) {
            String key = "a" + opt.idx;
            String userVal = userAnswerMap.getOrDefault(key, "").toString().trim();
            String correctVal = opt.answer == null ? "" : opt.answer.trim();

            Map<String, Object> correctItem = new HashMap<>();
            correctItem.put("idx", opt.idx);
            correctItem.put("answer", correctVal);
            correctAnswerList.add(correctItem);

            Map<String, Object> userItem = new HashMap<>();
            userItem.put("idx", opt.idx);
            userItem.put("answer", userVal);
            userAnswerList.add(userItem);

            if (userVal.equalsIgnoreCase(correctVal)) {
                correctCount++;
                BigDecimal blankScore = BigDecimal.valueOf(topic.score)
                        .multiply(BigDecimal.valueOf(opt.scale))
                        .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
                totalEarned = totalEarned.add(blankScore);
            }
        }

        int correctStatus;
        if (totalBlanks == 0) {
            correctStatus = 3;
            totalEarned = BigDecimal.ZERO;
        } else if (correctCount == totalBlanks) {
            correctStatus = 1;
        } else if (correctCount > 0) {
            correctStatus = 2;
        } else {
            correctStatus = 3;
        }

        resultItem.put("selectedAnswer", userAnswerList);
        resultItem.put("correctAnswer", correctAnswerList);
        resultItem.put("correctStatus", correctStatus);
        resultItem.put("totalScore", topic.score);
        resultItem.put("earnedScore", totalEarned.setScale(2, RoundingMode.HALF_UP));

        return resultItem;
    }

    private static Map<String, Object> handleSelectionQuestion(
            Map<String, Object> resultItem,
            Topic topic,
            Object answerObj) {

        List<String> selectedOptions = parseAnswerToList(answerObj);
        if (selectedOptions.isEmpty()) {
            resultItem.put("selectedAnswer", new ArrayList<>());
            resultItem.put("correctAnswer", getCorrectAnswer(topic));
            resultItem.put("correctStatus", 3);
            resultItem.put("totalScore", topic.score);
            resultItem.put("earnedScore", BigDecimal.ZERO);
            return resultItem;
        }

        List<String> correctAnswer = getCorrectAnswer(topic);

        boolean hasWrongSelection = selectedOptions.stream()
                .anyMatch(opt -> !correctAnswer.contains(opt));

        int correctStatus;
        BigDecimal earnedScore;

        if (hasWrongSelection) {
            correctStatus = 3;
            earnedScore = BigDecimal.ZERO;
        } else if (selectedOptions.containsAll(correctAnswer) && correctAnswer.containsAll(selectedOptions)) {
            correctStatus = 1;
            earnedScore = BigDecimal.valueOf(topic.score).setScale(2, RoundingMode.HALF_UP);
        } else if (selectedOptions.size() < correctAnswer.size()) {
            correctStatus = 2;
            double ratio = (double) selectedOptions.size() / correctAnswer.size();
            earnedScore = BigDecimal.valueOf(topic.score)
                    .multiply(BigDecimal.valueOf(ratio))
                    .setScale(2, RoundingMode.HALF_UP);
        } else {
            correctStatus = 3;
            earnedScore = BigDecimal.ZERO;
        }

        Object formattedSelectedAnswer;
        if (selectedOptions.size() == 1 && answerObj instanceof String) {
            formattedSelectedAnswer = selectedOptions.get(0);
        } else {
            formattedSelectedAnswer = new ArrayList<>(selectedOptions);
        }

        resultItem.put("selectedAnswer", formattedSelectedAnswer);
        resultItem.put("correctAnswer", correctAnswer);
        resultItem.put("correctStatus", correctStatus);
        resultItem.put("totalScore", topic.score);
        resultItem.put("earnedScore", earnedScore);

        return resultItem;
    }

    private static Map<String, Object> buildEmptyAnswerResult(
            Map<String, Object> resultItem,
            Topic topic,
            int correctStatus,
            BigDecimal earnedScore) {
        resultItem.put("selectedAnswer", new ArrayList<>());
        resultItem.put("correctAnswer", new ArrayList<>());
        resultItem.put("correctStatus", correctStatus);
        resultItem.put("totalScore", topic.score);
        resultItem.put("earnedScore", earnedScore);
        return resultItem;
    }

    private static Integer parseInteger(Object obj) {
        if (obj == null) return null;
        if (obj instanceof Number) return ((Number) obj).intValue();
        try {
            return Integer.parseInt(obj.toString());
        } catch (Exception e) {
            return null;
        }
    }

    private static double parseScale(Object obj) {
        if (obj == null) return 0.0;
        if (obj instanceof Number) return ((Number) obj).doubleValue();
        try {
            return Double.parseDouble(obj.toString());
        } catch (Exception e) {
            return 0.0;
        }
    }

    private static List<String> parseAnswerToList(Object answerObj) {
        if (answerObj == null) return new ArrayList<>();
        if (answerObj instanceof List) {
            return ((List<?>) answerObj).stream()
                    .map(Object::toString)
                    .collect(Collectors.toList());
        }
        if (answerObj instanceof String) {
            String str = (String) answerObj;
            str = str.trim();
            if (str.startsWith("[") && str.endsWith("]")) {
                return Arrays.stream(str.replaceAll("[\"\\[\\]\\s]", "").split(","))
                        .filter(s -> !s.isEmpty())
                        .collect(Collectors.toList());
            } else if (!str.isEmpty()) {
                return Collections.singletonList(str);
            }
        }
        return new ArrayList<>();
    }

    private static List<String> getCorrectAnswer(Topic topic) {
        return topic.options.stream()
                .filter(opt -> opt.scale > 0)
                .map(opt -> opt.idx)
                .sorted()
                .collect(Collectors.toList());
    }
}
