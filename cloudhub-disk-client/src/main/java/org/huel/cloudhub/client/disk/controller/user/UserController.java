package org.huel.cloudhub.client.disk.controller.user;

import org.huel.cloudhub.client.disk.common.ApiContextHolder;
import org.huel.cloudhub.client.disk.controller.Api;
import org.huel.cloudhub.client.disk.domain.user.UserIdentity;
import org.huel.cloudhub.client.disk.domain.user.dto.UserInfo;
import org.huel.cloudhub.client.disk.domain.user.service.UserSearchService;
import org.huel.cloudhub.client.disk.domain.user.vo.UserCommonDetailsVo;
import org.huel.cloudhub.web.BusinessRuntimeException;
import org.huel.cloudhub.web.AuthErrorCode;
import org.huel.cloudhub.web.HttpResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;

/**
 * @author RollW
 */
@Api
public class UserController {
    private final UserSearchService userSearchService;

    public UserController(UserSearchService userSearchService) {
        this.userSearchService = userSearchService;
    }

    @GetMapping("/user")
    public HttpResponseEntity<UserCommonDetailsVo> getAuthenticatedUser() {
        ApiContextHolder.ApiContext context = ApiContextHolder.getContext();
        UserInfo userInfo = context.userInfo();
        if (userInfo == null) {
            throw new BusinessRuntimeException(AuthErrorCode.ERROR_UNAUTHORIZED_USE);
        }
        return HttpResponseEntity.success(
                UserCommonDetailsVo.of(userInfo)
        );
    }

    @GetMapping("/user/{userId}")
    public HttpResponseEntity<UserCommonDetailsVo> getUserInfo(
            @PathVariable("userId") Long userId) {
        UserIdentity userIdentity = userSearchService.findUser(userId);
        return HttpResponseEntity.success(
                UserCommonDetailsVo.of(userIdentity)
        );
    }

    @PutMapping("/user/{userId}/blocks")
    public void blockUser(@PathVariable("userId") Long userId) {

    }

    @DeleteMapping("/user/{userId}/blocks")
    public void unblockUser(@PathVariable("userId") Long userId) {

    }
}
