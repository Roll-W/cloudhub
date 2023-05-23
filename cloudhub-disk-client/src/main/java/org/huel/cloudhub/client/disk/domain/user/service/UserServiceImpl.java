package org.huel.cloudhub.client.disk.domain.user.service;

import org.huel.cloudhub.client.disk.domain.user.AttributedUser;
import org.huel.cloudhub.client.disk.domain.user.Role;
import org.huel.cloudhub.client.disk.domain.user.User;
import org.huel.cloudhub.client.disk.domain.user.UserIdentity;
import org.huel.cloudhub.client.disk.domain.user.common.UserException;
import org.huel.cloudhub.client.disk.domain.user.dto.UserInfo;
import org.huel.cloudhub.client.disk.domain.user.event.OnUserCreateEvent;
import org.huel.cloudhub.client.disk.domain.user.filter.UserFilteringInfo;
import org.huel.cloudhub.client.disk.domain.user.filter.UserFilteringInfoType;
import org.huel.cloudhub.client.disk.domain.user.filter.UserInfoFilter;
import org.huel.cloudhub.client.disk.domain.user.repository.UserRepository;
import org.huel.cloudhub.web.CommonErrorCode;
import org.huel.cloudhub.web.ErrorCode;
import org.huel.cloudhub.web.UserErrorCode;
import org.huel.cloudhub.web.data.page.PageHelper;
import org.huel.cloudhub.web.data.page.Pageable;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * @author RollW
 */
@Service
public class UserServiceImpl implements UserSignatureProvider,
        UserManageService, UserSearchService {
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final UserInfoFilter userInfoFilter;
    private final ApplicationEventPublisher eventPublisher;

    public UserServiceImpl(UserRepository userRepository,
                           PasswordEncoder passwordEncoder,
                           UserInfoFilter userInfoFilter,
                           ApplicationEventPublisher eventPublisher) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.userInfoFilter = userInfoFilter;
        this.eventPublisher = eventPublisher;
    }

    @Override
    public String getSignature(long userId) {
        User user = userRepository.getById(userId);
        if (user == null) {
            return null;
        }
        return "CloudhubUserSignature-" +
                getSignature(user);
    }

    @Override
    public String getSignature(UserIdentity userIdentity) {
        return getSignature(userIdentity.getUserId());
    }

    private String getSignature(User user) {
        return user.getPassword() + "-" +
                user.getUsername() + "-" +
                user.getRole();
    }

    @Override
    public AttributedUser createUser(String username, String password,
                                     String email, Role role, boolean enable) {
        if (userRepository.isExistByEmail(username)) {
            throw new UserException(UserErrorCode.ERROR_EMAIL_EXISTED);
        }
        if (userRepository.isExistByEmail(email)) {
            throw new UserException(UserErrorCode.ERROR_EMAIL_EXISTED);
        }
        ErrorCode validateUser = validate(username, password, email);
        if (validateUser.failed()) {
            throw new UserException(validateUser);
        }
        long now = System.currentTimeMillis();
        User user = User.builder()
                .setUsername(username)
                .setPassword(passwordEncoder.encode(password))
                .setRole(role)
                .setEnabled(enable)
                .setAccountExpired(false)
                .setRegisterTime(now)
                .setUpdateTime(now)
                .setEmail(email)
                .build();
        long id = userRepository.insertUser(user);
        UserInfo userInfo = new UserInfo(id, username, email, role);
        OnUserCreateEvent onUserCreateEvent =
                new OnUserCreateEvent(userInfo);
        eventPublisher.publishEvent(onUserCreateEvent);
        return user;
    }

    private ErrorCode validate(String username,
                               String password,
                               String email) {
        List<UserFilteringInfo> filteringInfos = List.of(
                new UserFilteringInfo(username, UserFilteringInfoType.USERNAME),
                new UserFilteringInfo(password, UserFilteringInfoType.PASSWORD),
                new UserFilteringInfo(email, UserFilteringInfoType.EMAIL)
        );
        for (UserFilteringInfo filteringInfo : filteringInfos) {
            ErrorCode errorCode = userInfoFilter.filter(filteringInfo);
            if (errorCode.failed()) {
                return errorCode;
            }
        }
        return CommonErrorCode.SUCCESS;
    }

    @Override
    public AttributedUser getUser(long userId) throws UserException {
        User user = userRepository.getById(userId);
        if (user == null) {
            throw new UserException(UserErrorCode.ERROR_USER_NOT_EXIST);
        }
        return user;
    }

    @Override
    public List<User> getUsers(int page, int size) {
        return userRepository.get(PageHelper.offset(page, size));
    }

    @Override
    public List<User> getUsers() {
        return userRepository.getAll();
    }

    @Override
    public AttributedUser findUser(long userId) throws UserException {
        User user = userRepository.getById(userId);
        if (user == null) {
            throw new UserException(UserErrorCode.ERROR_USER_NOT_EXIST);
        }
        if (!user.isEnabled()) {
            throw new UserException(UserErrorCode.ERROR_USER_DISABLED);
        }
        if (user.isCanceled()) {
            throw new UserException(UserErrorCode.ERROR_USER_CANCELED);
        }
        return user;
    }

    @Override
    public AttributedUser findUser(String username) throws UserException {
        return null;
    }

    @Override
    public List<? extends AttributedUser> findUsers(String username) {
        return null;
    }

    @Override
    public AttributedUser findUser(UserIdentity userIdentity) throws UserException {
        if (userIdentity == null) {
            throw new UserException(UserErrorCode.ERROR_USER_NOT_EXIST);
        }
        return findUser(userIdentity.getUserId());
    }

    @Override
    public List<? extends AttributedUser> findUsers(Pageable pageable) {
        // TODO: filter canceled user
        return userRepository.get(pageable.toOffset());
    }

    @Override
    public List<? extends AttributedUser> findUsers() {
        // TODO: filter canceled user
        return userRepository.getAll();
    }

    @Override
    public List<? extends AttributedUser> findUsers(List<Long> ids) {
        return userRepository.getByIds(ids);
    }

    @Override
    public void deleteUser(long userId) {

    }

    @Override
    public void setUserEnable(long userId, boolean enable) {

    }
}
