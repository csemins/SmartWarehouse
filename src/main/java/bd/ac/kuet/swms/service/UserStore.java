package bd.ac.kuet.swms.service;

import bd.ac.kuet.swms.model.Role;
import bd.ac.kuet.swms.model.User;
import bd.ac.kuet.swms.model.UserFactory;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

// experiment: keep the accounts in users.json next to the db so they can be edited by hand
public class UserStore {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    private record Account(String username, String fullName, Role role, String salt, String passwordHash, boolean active) { }

    private final Path file;
    private List<Account> accounts = new ArrayList<>();

    public UserStore(Path file) {
        this.file = file;
        load();
    }

    private void load() {
        if (!Files.exists(file)) return;
        try (Reader reader = Files.newBufferedReader(file, StandardCharsets.UTF_8)) {
            List<Account> loaded = GSON.fromJson(reader, new TypeToken<List<Account>>() { }.getType());
            if (loaded != null) accounts = new ArrayList<>(loaded);
        } catch (IOException e) {
            throw new BusinessException("Cannot read " + file + ": " + e.getMessage());
        }
    }

    public void add(User user) {
        accounts.add(new Account(user.getUsername(), user.getFullName(), user.getRole(), user.getSalt(), user.getPasswordHash(), user.isActive()));
        save();
    }

    public Optional<User> find(String username) {
        return accounts.stream().filter(a -> a.username().equalsIgnoreCase(username)).findFirst().map(a -> {
            User u = UserFactory.create(a.role());
            u.setUsername(a.username());
            u.setFullName(a.fullName());
            u.setSalt(a.salt());
            u.setPasswordHash(a.passwordHash());
            u.setActive(a.active());
            return u;
        });
    }

    // FIXME ids don't match the users table -> audit / foreign keys will be a mess
    private void save() {
        try (Writer writer = Files.newBufferedWriter(file, StandardCharsets.UTF_8)) {
            GSON.toJson(accounts, writer);
        } catch (IOException e) {
            throw new BusinessException("Cannot write " + file + ": " + e.getMessage());
        }
    }
}
