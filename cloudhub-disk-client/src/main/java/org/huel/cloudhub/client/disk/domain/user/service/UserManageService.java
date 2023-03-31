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

package org.huel.cloudhub.client.disk.domain.user.service;


import org.huel.cloudhub.client.disk.domain.user.Role;
import org.huel.cloudhub.client.disk.domain.user.User;
import org.huel.cloudhub.client.disk.domain.user.common.UserViewException;
import org.huel.cloudhub.client.disk.domain.user.dto.UserInfo;
import org.huel.cloudhub.common.Result;

import java.util.List;

/**
 * @author RollW
 */
public interface UserManageService {
    Result<UserInfo> createUser(String username, String password,
                                String email, Role role, boolean enable);

    User getUser(long userId) throws UserViewException;

    List<User> getUsers(int page, int size);

    List<User> getUsers();

    void deleteUser(long userId);

    void setUserEnable(long userId, boolean enable);

    void setBlockUser(long userId, boolean block);
}