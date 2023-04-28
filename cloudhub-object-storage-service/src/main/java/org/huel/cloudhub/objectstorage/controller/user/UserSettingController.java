package org.huel.cloudhub.objectstorage.controller.user;

import com.fasterxml.jackson.annotation.JsonProperty;
import org.huel.cloudhub.objectstorage.data.dto.user.UserInfo;
import org.huel.cloudhub.objectstorage.data.dto.user.UserPasswordResetRequest;
import org.huel.cloudhub.objectstorage.service.user.UserGetter;
import org.huel.cloudhub.objectstorage.service.user.UserSettingService;
import org.huel.cloudhub.web.BusinessRuntimeException;
import org.huel.cloudhub.web.HttpResponseEntity;
import org.huel.cloudhub.web.UserErrorCode;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletRequest;


/**
 * @author Cheng
 */
@RestController
@UserSettingApi
public class UserSettingController {
    private final UserSettingService userSettingService;

    private final UserGetter userGetter;

    public UserSettingController(UserSettingService userSettingService,
                                 UserGetter userGetter) {
        this.userSettingService = userSettingService;
        this.userGetter = userGetter;
    }

    //    更新密码，首先判断是否登录和获取老密码,更改userInfo
    @PostMapping("/password/reset")
    public HttpResponseEntity<UserInfo> resetPassword(
            HttpServletRequest request,
            @RequestBody UserPasswordResetRequest passwordResetRequest) {
        UserInfo userInfo = userGetter.getCurrentUser(request);
        if (userInfo == null) {
            throw new BusinessRuntimeException(UserErrorCode.ERROR_USER_NOT_LOGIN);
        }

        var res = userSettingService.resetPassword(
                userInfo.id(),
                passwordResetRequest.oldPassword(),
                passwordResetRequest.newPassword());

        return HttpResponseEntity.of(res.toResponseBody());
    }

    //  忘记密码,首先判断用户是否存在
    @PostMapping("/password/lost")
    public HttpResponseEntity<UserInfo> lostPassword(
            HttpServletRequest request,
            @RequestBody UserPasswordResetRequest userPasswordResetRequest) {
        UserInfo userInfo = userGetter.getCurrentUser(request);
        // 使用userInfo.id判断空值会警告
        if (userInfo == null) {
            throw new BusinessRuntimeException(UserErrorCode.ERROR_USER_NOT_LOGIN);
        }
        var res = userSettingService.resetPassword(
                userInfo.id(),
                userPasswordResetRequest.newPassword());
        return HttpResponseEntity.of(res.toResponseBody());
    }

    //    更新用户名
    @PostMapping("/username")
    public HttpResponseEntity<UserInfo> resetUsername(
            HttpServletRequest request,
            @RequestBody UsernameResetRequest usernameResetRequest) {
        UserInfo userInfo = userGetter.getCurrentUser(request);
        if (userInfo == null) {
            throw new BusinessRuntimeException(UserErrorCode.ERROR_USER_NOT_LOGIN);
        }
        var res = userSettingService.resetUsername(userInfo.id(),
                usernameResetRequest.newUsername());
        return HttpResponseEntity.of(res.toResponseBody());
    }

    public record UsernameResetRequest(
            @JsonProperty("username")
            String newUsername
    ) {
    }
}