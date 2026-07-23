package cn.xfywz.guozespring.util;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;

public class CodeImgUtil {
    // 验证码类型：数字、字母、混合
    public enum CodeType {
        NUM, CHAR, MIX
    }

    // 默认验证码长度
    private static final int CODE_LENGTH = 4;
    // 默认图片宽度
    private static final int WIDTH = 100;
    // 默认图片高度
    private static final int HEIGHT = 40;
    // 干扰线数量
    private static final int LINE_COUNT = 20;

    /**
     * 生成验证码
     * @param codeLength 验证码长度
     * @param width 图片宽度
     * @param height 图片高度
     * @param codeType 验证码类型
     * @return 包含验证码和图片的Map
     */
    public static Map<String, Object> generateCode(int codeLength, int width, int height, CodeType codeType) {
        // 创建 BufferedImage 对象
        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        // 获取 Graphics 对象
        Graphics g = image.getGraphics();

        // 背景色设为白色
        g.setColor(Color.WHITE);
        g.fillRect(0, 0, width, height);

        // 绘制干扰线
        drawInterferenceLine(g, width, height);

        // 生成验证码
        String code = generateRandomCode(codeLength, codeType);

        // 绘制验证码
        drawCode(g, code, width, height);

        // 释放资源
        g.dispose();

        // 返回结果
        Map<String, Object> result = new HashMap<>();
        result.put("code", code);
        result.put("image", image);

        return result;
    }

    /**
     * 生成默认设置的验证码
     * @return 包含验证码和图片的Map
     */
    public static Map<String, Object> generateCode() {
        return generateCode(CODE_LENGTH, WIDTH, HEIGHT, CodeType.MIX);
    }

    /**
     * 绘制干扰线
     * @param g Graphics对象
     * @param width 图片宽度
     * @param height 图片高度
     */
    private static void drawInterferenceLine(Graphics g, int width, int height) {
        Random random = new Random();
        g.setColor(getRandomColor(160, 255));

        for (int i = 0; i < LINE_COUNT; i++) {
            int x1 = random.nextInt(width);
            int y1 = random.nextInt(height);
            int x2 = random.nextInt(12) + x1;
            int y2 = random.nextInt(12) + y1;

            g.drawLine(x1, y1, x2, y2);
        }
    }

    /**
     * 生成随机验证码
     * @param length 验证码长度
     * @param codeType 验证码类型
     * @return 验证码字符串
     */
    private static String generateRandomCode(int length, CodeType codeType) {
        StringBuilder code = new StringBuilder();
        Random random = new Random();

        String charSet = "";
        switch (codeType) {
            case NUM:
                charSet = "2345678";
                break;
            case CHAR:
                charSet = "ABCDEFGHIJKLMNPQRSTUVWXYZabcdefhjkmnpqrstuvwxyz";
                break;
            case MIX:
                charSet = "23456789ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefhjkmnpqrstuvwxyz";
                break;
        }

        for (int i = 0; i < length; i++) {
            int index = random.nextInt(charSet.length());
            code.append(charSet.charAt(index));
        }

        return code.toString();
    }

    /**
     * 绘制验证码文本
     * @param g Graphics对象
     * @param code 验证码
     * @param width 图片宽度
     * @param height 图片高度
     */
    private static void drawCode(Graphics g, String code, int width, int height) {
        Random random = new Random();

        // 设置字体
        g.setFont(new Font("Arial", Font.BOLD, 30));

        // 计算字符的垂直中心位置（考虑字体大小和行间距）
        int fontHeight = g.getFont().getSize();
        int lineSpacing = fontHeight / 5; // 行间距（可以根据需要调整）
        int verticalCenter = height / 2 - (fontHeight + lineSpacing) / 2;

        // 添加噪点并绘制每个字符
        for (int i = 0; i < code.length(); i++) {
            // 随机颜色
            g.setColor(new Color(20 + random.nextInt(120), 20 + random.nextInt(120), 20 + random.nextInt(110)));

            // 随机位置（水平方向）
            int x = width / (code.length() + 1) * (i + 1) + random.nextInt(5)-10 ;

            // 垂直位置（固定在垂直中心附近，允许少量随机偏移）
            int y = verticalCenter + random.nextInt(10) + 24;

            // 旋转角度
            Graphics2D g2d = (Graphics2D) g;
            double rotation = random.nextDouble() * 0.3 - 0.15;
            g2d.rotate(rotation, x, y);

            // 绘制字符
            g2d.drawString(String.valueOf(code.charAt(i)), x, y);

            // 恢复旋转
            g2d.rotate(-rotation, x, y);
        }
    }

//    private static void drawCode(Graphics g, String code, int width, int height) {
//        Random random = new Random();
//
//        // 设置字体
//        g.setFont(new Font("Arial", Font.BOLD, 36));
//
//        // 添加噪点并绘制每个字符
//        for (int i = 0; i < code.length(); i++) {
//            // 随机颜色
//            g.setColor(new Color(20 + random.nextInt(110), 20 + random.nextInt(110), 20 + random.nextInt(110)));
//
//            // 随机位置
//            int x = width / (code.length() + 1) * (i + 1) + random.nextInt(5) - 2;
//            int y = height / 2 + random.nextInt(10) - 5;
//
//            // 旋转角度
//            Graphics2D g2d = (Graphics2D) g;
//            double rotation = random.nextDouble() * 0.3 - 0.15;
//            g2d.rotate(rotation, x, y);
//
//            // 绘制字符
//            g2d.drawString(String.valueOf(code.charAt(i)), x, y);
//
//            // 恢复旋转
//            g2d.rotate(-rotation, x, y);
//        }
//    }

    /**
     * 生成随机颜色
     * @param fc 前景色
     * @param bc 背景色
     * @return 随机颜色
     */
    private static Color getRandomColor(int fc, int bc) {
        Random random = new Random();
        fc = Math.min(fc, 255);
        bc = Math.min(bc, 255);

        int r = fc + random.nextInt(bc - fc);
        int g = fc + random.nextInt(bc - fc);
        int b = fc + random.nextInt(bc - fc);

        return new Color(r, g, b);
    }
}
