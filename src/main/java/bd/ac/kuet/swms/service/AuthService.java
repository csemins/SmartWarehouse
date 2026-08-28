package bd.ac.kuet.swms.service;

import bd.ac.kuet.swms.model.Role;
import bd.ac.kuet.swms.model.User;
import bd.ac.kuet.swms.model.UserFactory;
import bd.ac.kuet.swms.repository.UserRepository;
import bd.ac.kuet.swms.util.PasswordHasher;

import java.util.Arrays;
import java.util.Optional;

public class AuthService {
    private final UserRepository users;
    private final Session session;

    public AuthService(UserRepository users, Session session) {
        this.users = users;
        this.session = session;
    }

    // first start only. TODO change these passwords before the demo
    public void createDefaultUsersIfNone() {
        if (!users.isEmpty()) return;
        users.save(newUser("admin", "System Administrator", Role.ADMIN, "admin123"));
        users.save(newUser("inventory", "Inventory Manager", Role.INVENTORY_MANAGER, "inventory123"));
        users.save(newUser("finance", "Finance Manager", Role.FINANCE_MANAGER, "finance123"));
    }

    public Optional<User> login(String username, char[] password) {
        try {
            Optional<User> user = users.findByUsername(username.trim())
                    .filter(User::isActive)
                    .filter(u -> PasswordHasher.matches(password, u.getSalt(), u.getPasswordHash()));
            user.ifPresent(u -> {
                session.setUser(u);
            });
            return user;
        } finally {
            // wipe the char[] after use
            Arrays.fill(password, '\0');
        }
    }

    public void logout() {
        session.setUser(null);
    }

    public static User newUser(String username, String fullName, Role role, String password) {
        if (password.length() < 6) throw new BusinessException("Password must have at least 6 characters");
        User user = UserFactory.create(role);
        user.setUsername(username);
        user.setFullName(fullName);
        user.setSalt(PasswordHasher.newSalt());
        user.setPasswordHash(PasswordHasher.hash(password.toCharArray(), user.getSalt()));
        return user;
    }
}
