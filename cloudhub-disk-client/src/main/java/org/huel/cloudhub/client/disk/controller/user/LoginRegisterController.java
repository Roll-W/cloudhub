/*
 * Copyright (C) 2023 RollW
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.huel.cloudhub.client.disk.controller.user;

import com.google.common.base.Strings;
import org.huel.cloudhub.client.disk.common.ApiContextHolder;
import org.huel.cloudhub.client.disk.common.ParamValidate;
import org.huel.cloudhub.client.disk.domain.authentication.token.AuthenticationTokenService;
import org.huel.cloudhub.client.disk.domain.user.dto.UserInfo;
import org.huel.cloudhub.client.disk.domain.user.dto.UserInfoSignature;
import org.huel.cloudhub.client.disk.domain.user.service.LoginRegisterService;
import org.huel.cloudhub.client.disk.domain.user.vo.LoginResponse;
import org.huel.cloudhub.common.Result;
import org.huel.cloudhub.web.HttpResponseEntity;
import org.huel.cloudhub.web.RequestMetadata;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

import javax.servlet.http.HttpServletRequest;

/**
 * @author RollW
 */
@UserApi
public class LoginRegisterController {
    private static final Logger logger = LoggerFactory.getLogger(LoginRegisterController.class);

    private final LoginRegisterService loginRegisterService;
    private final AuthenticationTokenService authenticationTokenService;

    public LoginRegisterController(LoginRegisterService loginRegisterService,
                                   AuthenticationTokenService authenticationTokenService) {
        this.loginRegisterService = loginRegisterService;
        this.authenticationTokenService = authenticationTokenService;
    }

    @PostMapping("/login/password")
    public HttpResponseEntity<LoginResponse> loginByPassword(
            RequestMetadata requestMetadata,
            @RequestBody UserLoginRequest loginRequest) {
        // account login, account maybe the username or email
        // needs to check the account type and get the user id
        ParamValidate.notEmpty(loginRequest.identity(), "identity cannot be null or empty.");
        ParamValidate.notEmpty(loginRequest.token(), "token cannot be null or empty.");

        Result<UserInfoSignature> res = loginRegisterService.loginUser(
                loginRequest.identity(),
                loginRequest.token(),
                requestMetadata);
        return loginResponse(res);
    }

    private HttpResponseEntity<LoginResponse> loginResponse(Result<UserInfoSignature> res) {
        if (res.errorCode().failed()) {
            return HttpResponseEntity.of(
                    res.toResponseBody(LoginResponse::nullResponse)
            );
        }
        UserInfoSignature infoSignature = res.data();
        String token = authenticationTokenService.generateAuthToken(
                infoSignature.id(), infoSignature.signature()
        );
        LoginResponse response = new LoginResponse(token,
                res.extract(UserInfoSignature::toUserInfo));
        return HttpResponseEntity.success(response);
    }

    @PostMapping("/register")
    public HttpResponseEntity<Void> registerUser(@RequestBody UserRegisterRequest request) {
        Result<UserInfo> res = loginRegisterService.registerUser(
                request.username(),
                request.password(),
                request.email()
        );
        return HttpResponseEntity.of(res.toResponseBody(() -> null));
    }

    @PostMapping("/logout")
    public HttpResponseEntity<Void> logout(HttpServletRequest request) {
        String auth = request.getHeader("Authorization");
        if (Strings.isNullOrEmpty(auth)) {
            return HttpResponseEntity.success();
        }
        ApiContextHolder.ApiContext context = ApiContextHolder.getContext();
        if (context == null || context.userInfo() == null) {
            return HttpResponseEntity.success();
        }
        loginRegisterService.logout();
        return HttpResponseEntity.success();
    }
}
