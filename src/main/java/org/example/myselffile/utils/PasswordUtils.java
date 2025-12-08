package org.example.myselffile.utils;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
public class PasswordUtils {

    private static final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    /**
     *  验证密码是否匹配
     * @param dbEncodedPassword 加密后的密码
     * @param requestPassword   请求输入的密码
     * @return 是否匹配
     */
    public static boolean verify(String dbEncodedPassword, String requestPassword) {
        return passwordEncoder.matches(requestPassword, dbEncodedPassword);
    }

    /**
     *  对明文密码进行加密
     * @param password 明文密码
     * @return 加密后的密码
     */
    public static String encrypt(String password) {
        return passwordEncoder.encode(password);
    }


}