package wifi.pojie;

import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

/**
 * 密码生成器，用于生成各种类型的密码
 */
public class PasswordGenerator {
    private static final String LOWERCASE_LETTERS = "abcdefghijklmnopqrstuvwxyz";
    private static final String UPPERCASE_LETTERS = "ABCDEFGHIJKLMNOPQRSTUVWXYZ";
    private static final String DIGITS = "0123456789";
    private static final String SPECIAL_CHARACTERS = "!@#$%^&*()_+-=[]{}|;:,.<>?";
    private static final String ALL_CHARACTERS = LOWERCASE_LETTERS + UPPERCASE_LETTERS + DIGITS + SPECIAL_CHARACTERS;
    
    private final Random random;
    
    public PasswordGenerator() {
        // 使用SecureRandom确保密码的随机性
        this.random = new SecureRandom();
    }
    
    /**
     * 生成单个密码
     * @param length 密码长度
     * @param includeLowercase 是否包含小写字母
     * @param includeUppercase 是否包含大写字母
     * @param includeDigits 是否包含数字
     * @param includeSpecial 是否包含特殊字符
     * @return 生成的密码
     */
    public String generatePassword(int length, boolean includeLowercase, boolean includeUppercase, 
                                  boolean includeDigits, boolean includeSpecial) {
        if (length <= 0) {
            throw new IllegalArgumentException("密码长度必须大于0");
        }
        
        // 构建字符集
        StringBuilder charset = new StringBuilder();
        if (includeLowercase) charset.append(LOWERCASE_LETTERS);
        if (includeUppercase) charset.append(UPPERCASE_LETTERS);
        if (includeDigits) charset.append(DIGITS);
        if (includeSpecial) charset.append(SPECIAL_CHARACTERS);
        
        if (charset.length() == 0) {
            throw new IllegalArgumentException("至少需要选择一种字符类型");
        }
        
        // 确保密码包含至少一种选中的字符类型
        StringBuilder password = new StringBuilder(length);
        
        // 先生成每种类型的至少一个字符
        if (includeLowercase) {
            password.append(LOWERCASE_LETTERS.charAt(random.nextInt(LOWERCASE_LETTERS.length())));
        }
        if (includeUppercase) {
            password.append(UPPERCASE_LETTERS.charAt(random.nextInt(UPPERCASE_LETTERS.length())));
        }
        if (includeDigits) {
            password.append(DIGITS.charAt(random.nextInt(DIGITS.length())));
        }
        if (includeSpecial) {
            password.append(SPECIAL_CHARACTERS.charAt(random.nextInt(SPECIAL_CHARACTERS.length())));
        }
        
        // 填充剩余长度
        for (int i = password.length(); i < length; i++) {
            password.append(charset.charAt(random.nextInt(charset.length())));
        }
        
        // 打乱密码字符顺序，增加随机性
        return shuffleString(password.toString());
    }
    
    /**
     * 生成多个密码
     * @param count 密码数量
     * @param length 密码长度
     * @param includeLowercase 是否包含小写字母
     * @param includeUppercase 是否包含大写字母
     * @param includeDigits 是否包含数字
     * @param includeSpecial 是否包含特殊字符
     * @return 生成的密码列表
     */
    public List<String> generateMultiplePasswords(int count, int length, boolean includeLowercase, 
                                                 boolean includeUppercase, boolean includeDigits, 
                                                 boolean includeSpecial) {
        List<String> passwords = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            passwords.add(generatePassword(length, includeLowercase, includeUppercase, 
                                           includeDigits, includeSpecial));
        }
        return passwords;
    }
    
    /**
     * 生成简单密码（只包含字母和数字）
     * @param length 密码长度
     * @return 生成的密码
     */
    public String generateSimplePassword(int length) {
        return generatePassword(length, true, true, true, false);
    }
    
    /**
     * 生成强密码（包含大小写字母、数字和特殊字符）
     * @param length 密码长度
     * @return 生成的密码
     */
    public String generateStrongPassword(int length) {
        return generatePassword(length, true, true, true, true);
    }
    
    /**
     * 生成只包含数字的密码
     * @param length 密码长度
     * @return 生成的密码
     */
    public String generateNumericPassword(int length) {
        return generatePassword(length, false, false, true, false);
    }
    
    /**
     * 生成只包含字母的密码
     * @param length 密码长度
     * @return 生成的密码
     */
    public String generateAlphabeticPassword(int length) {
        return generatePassword(length, true, true, false, false);
    }
    
    /**
     * 打乱字符串顺序
     * @param input 输入字符串
     * @return 打乱后的字符串
     */
    private String shuffleString(String input) {
        List<Character> characters = new ArrayList<>();
        for (char c : input.toCharArray()) {
            characters.add(c);
        }
        Collections.shuffle(characters, random);
        StringBuilder result = new StringBuilder();
        for (char c : characters) {
            result.append(c);
        }
        return result.toString();
    }
    
    /**
     * 评估密码强度
     * @param password 要评估的密码
     * @return 密码强度（1-5，5为最强）
     */
    public int evaluatePasswordStrength(String password) {
        if (password == null || password.isEmpty()) {
            return 0;
        }
        
        int strength = 0;
        
        // 长度评分
        if (password.length() >= 8) strength++;
        if (password.length() >= 12) strength++;
        if (password.length() >= 16) strength++;
        
        // 字符类型评分
        boolean hasLower = !password.matches("^[^a-z]*$");
        boolean hasUpper = !password.matches("^[^A-Z]*$");
        boolean hasDigit = !password.matches("^[^0-9]*$");
        boolean hasSpecial = !password.matches("^[a-zA-Z0-9]*$");
        
        if (hasLower) strength++;
        if (hasUpper) strength++;
        if (hasDigit) strength++;
        if (hasSpecial) strength++;
        
        // 去重评分（简单检查重复字符）
        if (password.chars().distinct().count() >= password.length() * 0.8) {
            strength++;
        }
        
        // 限制强度在1-5之间
        return Math.max(1, Math.min(5, strength));
    }
    
    /**
     * 获取密码强度描述
     * @param strength 密码强度评分（1-5）
     * @return 强度描述
     */
    public String getPasswordStrengthDescription(int strength) {
        switch (strength) {
            case 1:
                return "非常弱";
            case 2:
                return "弱";
            case 3:
                return "中等";
            case 4:
                return "强";
            case 5:
                return "非常强";
            default:
                return "未知";
        }
    }
    
    /**
     * 生成符合常见模式的密码（示例：包含生日）
     * @param length 密码长度
     * @param birthYear 出生年份（可选）
     * @param birthMonth 出生月份（可选）
     * @param birthDay 出生日期（可选）
     * @return 生成的密码
     */
    public String generatePatternPassword(int length, Integer birthYear, Integer birthMonth, Integer birthDay) {
        StringBuilder password = new StringBuilder();
        
        // 生成随机部分
        int randomPartLength = length / 2;
        String randomPart = generateSimplePassword(randomPartLength);
        password.append(randomPart);
        
        // 添加生日部分（如果提供）
        if (birthYear != null && birthMonth != null && birthDay != null) {
            String birthPart = String.format("%02d%02d%02d", birthYear % 100, birthMonth, birthDay);
            password.append(birthPart);
        } else {
            // 否则添加更多随机字符
            password.append(generateSimplePassword(length - randomPartLength));
        }
        
        // 确保长度正确
        if (password.length() > length) {
            password.setLength(length);
        } else if (password.length() < length) {
            password.append(generateSimplePassword(length - password.length()));
        }
        
        return shuffleString(password.toString());
    }
    
    /**
     * 批量生成符合特定规则的密码列表
     * @param count 密码数量
     * @param minLength 最小长度
     * @param maxLength 最大长度
     * @param includeLowercase 是否包含小写字母
     * @param includeUppercase 是否包含大写字母
     * @param includeDigits 是否包含数字
     * @param includeSpecial 是否包含特殊字符
     * @return 生成的密码列表
     */
    public List<String> generatePasswordList(int count, int minLength, int maxLength, 
                                            boolean includeLowercase, boolean includeUppercase, 
                                            boolean includeDigits, boolean includeSpecial) {
        List<String> passwords = new ArrayList<>();
        
        for (int i = 0; i < count; i++) {
            // 在minLength和maxLength之间随机选择长度
            int length = minLength + random.nextInt(maxLength - minLength + 1);
            String password = generatePassword(length, includeLowercase, includeUppercase, 
                                             includeDigits, includeSpecial);
            passwords.add(password);
        }
        
        return passwords;
    }
}