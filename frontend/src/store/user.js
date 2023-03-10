import $ from 'jquery';
import url from '@/store/api'
import router from "@/router";
export default {
    state: {
        id: "",
        username: "",
        email: "",
        role: "",
        is_login: false,
    },
    getters: {},
    mutations: {
        updateUser(state, user) {
            state.id = user.id;
            state.username = user.username;
            state.email = user.email;
            state.role = user.role;
            state.is_login = user.is_login;
        },
    },

    actions: {
        current(context, data) {
            $.ajax({
                url: url.url_getCurrent,
                type: "get",
                xhrFields: {
                    withCredentials: true
                },
                crossDomain: true,
                success(resp) {
                    if (resp.errorCode === "00000") {
                        context.commit("updateUser", {
                            ...resp.data,
                            is_login: true,
                        })
                        data.success(resp);
                    } else {
                        data.error(resp);
                    }
                },
                error(resp) {
                    data.error(resp);
                }
            });
        },
        login(context, data) {
            $.ajax({
                url: url.url_login,
                type: "post",
                contentType: "application/json;charset=UTF-8",
                data: JSON.stringify({
                    username: data.username,
                    password: data.password,
                }),
                xhrFields: {
                    withCredentials: true
                },
                crossDomain: true,
                success(resp) {
                    if (resp.errorCode === "00000") {
                        context.commit("updateUser", {
                            ...resp.data,
                            is_login: true,
                        })
                        data.success(resp);
                    } else {
                        data.error(resp);
                    }
                },
                error(resp) {
                    data.error(resp);
                }
            });
        },
        logout() {
            $.ajax({
                url:url.url_logout,
                type:"post",
                xhrFields: {
                    withCredentials: true // ????????????cookie  //????????????
                },
                crossDomain:true,
                success(){
                    router.push({name:"login_index"});
                },
                error() {
                    console.log("????????????");
                }
            })
        }
    },

    modules: {}
}