package bd.ac.kuet.swms.service;

import bd.ac.kuet.swms.model.AuditEntry;
import bd.ac.kuet.swms.model.Permission;
import bd.ac.kuet.swms.model.User;
import bd.ac.kuet.swms.repository.AuditRepository;

import java.util.List;

public class AuditService {
    private final AuditRepository entries;
    private final Session session;

    public AuditService(AuditRepository entries, Session session) {
        this.entries = entries;
        this.session = session;
    }

    public void record(AuditEntry.Category category, String action) {
        User user = session.user();
        entries.save(new AuditEntry(user == null ? null : user.getId(), session.username(), category, action));
    }

    public List<AuditEntry> latest(int limit) {
        session.require(Permission.CONFIGURE_SYSTEM);
        return entries.latest(limit);
    }
}
