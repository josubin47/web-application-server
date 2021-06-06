package db;

import java.util.Collection;
import java.util.Map;

import com.google.common.collect.Maps;

import model.User;
import org.apache.commons.lang3.StringUtils;

public class DataBase {
    private static Map<String, User> users = Maps.newHashMap();

    public static void addUser(User user) {
        users.put(user.getUserId(), user);
    }

    public static User findUserById(String userId) {
        return users.get(userId);
    }

    public static boolean checkUserInfo(String userId, String password) {
        User user = findUserById(userId);
        return user != null && StringUtils.equals(password, user.getPassword());
    }

    public static Collection<User> findAll() {
        return users.values();
    }
}
