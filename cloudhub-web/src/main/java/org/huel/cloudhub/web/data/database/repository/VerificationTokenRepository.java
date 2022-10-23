package org.huel.cloudhub.web.data.database.repository;

import org.huel.cloudhub.web.data.database.CloudhubDatabase;
import org.huel.cloudhub.web.data.database.dao.VerificationTokenDao;
import org.huel.cloudhub.web.data.entity.RegisterVerificationToken;
import org.huel.cloudhub.web.data.entity.User;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Repository;

/**
 * @author RollW
 */
@Repository
public class VerificationTokenRepository {
    private final VerificationTokenDao dao;

    public VerificationTokenRepository(CloudhubDatabase database) {
        dao = database.getVerificationTokenDao();
    }

    public RegisterVerificationToken findByToken(String token) {
        return dao.findByToken(token);
    }

    public RegisterVerificationToken findByUser(User user) {
        return dao.findByUserId(user.getId());
    }

    public RegisterVerificationToken findByUser(long id) {
        return dao.findByUserId(id);
    }

    @Async
    public void insert(RegisterVerificationToken verificationToken) {
        dao.insert(verificationToken);
    }

    @Async
    public void update(RegisterVerificationToken verificationToken) {
        dao.update(verificationToken);
    }

    @Async
    public void update(String token, boolean used) {
        dao.update();
    }
}
