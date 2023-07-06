import {createRouter, createWebHistory} from 'vue-router'
import {useUserStore} from "@/stores/user";

const layout = "layout"
const adminLayout = "layout-admin"
const headerLayout = "layout-header"
const loginLayout = "layout-login"
const userLayout = "layout-user"

export const index = "index"
export const login = "login-page"
export const register = "register-page"

export const passwordResetPage = "password-reset-page"

export const driveFilePage = "drive-file-page"
export const driveFilePageFolder = "drive-file-page-folder"
export const driveFilePageTypeImage = "drive-file-page-type-image"
export const driveFilePageTypeVideo = "drive-file-page-type-video"
export const driveFilePageTypeAudio = "drive-file-page-type-audio"
export const driveFilePageTypeDocument = "drive-file-page-type-document"

export const driveFileSearchPage = "drive-file-search-page"
export const driveFileRecycleBinPage = "drive-file-recycle-bin-page"
export const driveFileAttrsPage = "drive-file-attrs-page"
export const driveFilePermissionPage = "drive-file-permission-page"

export const driveShareTokenPage = "drive-share-token-page"

export const page404 = "page-404"
export const driveTagPage = "drive-tag-page"

export const userPersonalPage = "user-personal-page"
export const userPersonalPageWithFolder = "user-personal-page-with-folder"
export const userSettingPage = "user-setting-page"
export const userSharePage = "user-share-page"
export const userStatsPage = "user-stats-page"

export const adminIndex = "admin-index"

export const adminUserLists = "admin-user-lists"
export const adminUserGroupLists = "admin-user-group-lists"
export const adminUserGroupDetails = "admin-user-group-details"
export const adminUserDetails = "admin-user-details"

export const adminFileLists = "admin-file-lists"
export const adminFileDetails = "admin-file-details"
export const adminFolderLists = "admin-folder-lists"
export const adminFolderDetails = "admin-folder-details"

export const adminTagGroups = "admin-tag-groups"
export const adminTags = "admin-tags"
export const adminTagInfo = "admin-tag-info"
export const adminTagGroupInfo = "admin-tag-group-info"

export const adminVisualData = "admin-visual-data"

export const adminSystemLogs = "admin-system-logs"
export const adminLoginLogs = "admin-login-logs"
export const adminOperationLogs = "admin-operation-logs"
export const adminSystemMonitor = "admin-system-monitor"
export const adminClusterMonitor = "admin-cluster-monitor"
export const adminFileServerMonitor = "admin-file-server-monitor"

const router = createRouter({
    history: createWebHistory(import.meta.env.BASE_URL),
    routes: [
        {
            path: '/layout/side',
            name: layout,
            redirect: '/',
            component: () => import("@/views/Layout.vue"),
            children: [
                {
                    path: '/drive',
                    redirect: '/drive/files'
                },
                {
                    path: '/drive/files/folder/0',
                    redirect: '/drive/files'
                },
                {
                    path: '/drive/files',
                    name: driveFilePage,
                    component: () => import("@/views/file/FileView.vue"),
                    meta: {
                        title: "文件",
                        requireLogin: true
                    }
                },
                {
                    path: '/drive/files/folder/:folder',
                    name: driveFilePageFolder,
                    component: () => import("@/views/file/FileView.vue"),
                    meta: {
                        title: "文件",
                        requireLogin: true
                    }
                },
                {
                    path: '/drive/files/image',
                    name: driveFilePageTypeImage,
                    component: () => import("@/views/file/FileView.vue"),
                    meta: {
                        title: "图片",
                        requireLogin: true
                    }
                },
                {
                    path: '/drive/files/video',
                    name: driveFilePageTypeVideo,
                    component: () => import("@/views/file/FileView.vue"),
                    meta: {
                        title: "视频",
                        requireLogin: true
                    }
                },
                {
                    path: '/drive/files/audio',
                    name: driveFilePageTypeAudio,
                    component: () => import("@/views/file/FileView.vue"),
                    meta: {
                        title: "音频",
                        requireLogin: true
                    }
                },
                {
                    path: '/drive/files/documents',
                    name: driveFilePageTypeDocument,
                    component: () => import("@/views/file/FileView.vue"),
                    meta: {
                        title: "文档",
                        requireLogin: true
                    }
                },
                {
                    path: '/drive/files/recycles',
                    name: driveFileRecycleBinPage,
                    component: () => import("@/views/file/FileRecycleView.vue"),
                    meta: {
                        title: "回收站",
                        requireLogin: true
                    }
                },
                {
                    path: '/drive/files/search',
                    name: driveFileSearchPage,
                    component: () => import("@/views/file/FileSearchView.vue"),
                    meta: {
                        title: "文件搜索",
                        requireLogin: true
                    }
                },
                {
                    path: '/:ownerType/:ownerId/drive/files/:type/:id/attrs',
                    name: driveFileAttrsPage,
                    component: () => import("@/views/file/FileAttrsView.vue"),
                    meta: {
                        title: "文件属性",
                        requireLogin: true
                    }
                },
                {
                    path: '/:ownerType/:ownerId/drive/files/:type/:id/permission',
                    name: driveFilePermissionPage,
                    component: () => import("@/views/file/FilePermissionView.vue"),
                    meta: {
                        title: "文件权限",
                        requireLogin: true
                    }
                },
                {
                    path: '/drive/tags',
                    name: driveTagPage,
                    component: () => import("@/views/tag/FileTagsView.vue"),
                    meta: {
                        title: "标签",
                        requireLogin: true
                    }
                },

                {
                    path: '/user/setting',
                    name: userSettingPage,
                    component: () => import("@/views/user/UserSettingView.vue"),
                    meta: {
                        title: "用户设置",
                        requireLogin: true
                    }
                },
                {
                    path: '/user/statistics',
                    name: userStatsPage,
                    component: () => import("@/views/user/UserStatsView.vue"),
                    meta: {
                        title: "个人统计数据",
                        requireLogin: true
                    }
                },
                {
                    path: '/user/shares',
                    name: userSharePage,
                    component: () => import("@/views/user/PersonalShareView.vue"),
                    meta: {
                        title: "用户分享",
                        requireLogin: true
                    }
                },
            ]
        },
        {
            path: '/layout/admin',
            name: adminLayout,
            redirect: '/admin',
            component: () => import("@/views/AdminLayout.vue"),
            children: [
                {
                    path: '/admin',
                    name: adminIndex,
                    component: () => import("@/views/admin/AdminIndex.vue"),
                    meta: {
                        title: "管理首页",
                        requireLogin: true
                    }
                },
                {
                    path: '/admin/users',
                    name: adminUserLists,
                    component: () => import("@/views/admin/user/UsersList.vue"),
                    meta: {
                        title: "用户列表",
                        requireLogin: true
                    }
                },
                {
                    path: '/admin/users/groups',
                    name: adminUserGroupLists,
                    component: () => import("@/views/admin/user/UserGroupListsView.vue"),
                    meta: {
                        title: "用户组列表",
                        requireLogin: true
                    }
                },
                {
                    path: '/admin/storages/files',
                    name: adminFileLists,
                    component: () => import("@/views/admin/file/FileListsView.vue"),
                    meta: {
                        title: "文件列表",
                        requireLogin: true
                    }
                },
                {
                    path: '/admin/storages/folders',
                    name: adminFolderLists,
                    component: () => import("@/views/admin/file/FolderListsView.vue"),
                    meta: {
                        title: "文件夹列表",
                        requireLogin: true
                    }
                },
                {
                    path: '/admin/tags/groups',
                    name: adminTagGroups,
                    component: () => import("@/views/admin/tag/AdminTagGroupsView.vue"),
                    meta: {
                        title: "标签组列表",
                        requireLogin: true
                    }
                },
                {
                    path: '/admin/tags/groups/:id',
                    name: adminTagGroupInfo,
                    component: () => import("@/views/admin/tag/AdminTagGroupInfoView.vue"),
                    meta: {
                        title: "标签组信息",
                        requireLogin: true
                    }
                },
                {
                    path: '/admin/tags',
                    name: adminTags,
                    component: () => import("@/views/admin/tag/AdminTagsView.vue"),
                    meta: {
                        title: "标签列表",
                        requireLogin: true
                    }
                },
                {
                    path: '/admin/tags/:id',
                    name: adminTagInfo,
                    component: () => import("@/views/admin/tag/AdminTagInfoView.vue"),
                    meta: {
                        title: "标签信息",
                        requireLogin: true
                    }
                },
                {
                    path: '/admin/data',
                    name: adminVisualData,
                    component: () => import("@/views/EchartsIndexView.vue"),
                    meta: {
                        title: "数据分析",
                        requireLogin: true
                    }
                },
                {
                    path: '/admin/system/logs',
                    name: adminSystemLogs,
                    component: () => import("@/views/admin/system/SystemLogs.vue"),
                    meta: {
                        title: "系统日志",
                        requireLogin: true
                    }
                },
                {
                    path: '/admin/users/login/logs',
                    name: adminLoginLogs,
                    component: () => import("@/views/admin/system/LoginLogs.vue"),
                    meta: {
                        title: "用户登录日志",
                        requireLogin: true
                    }
                },
                {
                    path: '/admin/users/:userId',
                    name: adminUserDetails,
                    component: () => import("@/views/admin/user/AdminUserDetails.vue"),
                    meta: {
                        title: "用户信息",
                        requireLogin: true
                    }
                },
                {
                    path: '/admin/system/operations',
                    name: adminOperationLogs,
                    component: () => import("@/views/admin/system/OperationLogs.vue"),
                    meta: {
                        title: "系统操作日志",
                        requireLogin: true
                    }
                },
                {
                    path: '/admin/system/monitor',
                    name: adminSystemMonitor,
                    component: () => import("@/views/admin/system/ServerMonitorView.vue"),
                    meta: {
                        title: "系统监控",
                        requireLogin: true
                    }
                },
                {
                    path: '/admin/system/cfs/meta/monitor',
                    name: adminClusterMonitor,
                    component: () => import("@/views/admin/system/ServerMonitorView.vue"),
                    meta: {
                        title: "集群监控",
                        requireLogin: true
                    }
                },
                {
                    path: '/admin/system/cfs/:serverId/monitor',
                    name: adminFileServerMonitor,
                    component: () => import("@/views/admin/system/ServerMonitorView.vue"),
                    meta: {
                        title: "文件服务器监控",
                        requireLogin: true
                    }
                },
            ]
        },

        {
            path: '/layout/side/user',
            name: userLayout,
            redirect: '/drive/files',
            component: () => import("@/views/user/UserHomeHeaderLayout.vue"),
            children: [
                {
                    path: '/user/:id/home',
                    name: userPersonalPage,
                    component: () => import("@/views/user/PersonalHomeView.vue"),
                    meta: {
                        title: "个人主页",
                        requireLogin: true
                    }
                },
                {
                    path: '/user/:id/home/:folder',
                    name: userPersonalPageWithFolder,
                    component: () => import("@/views/user/PersonalHomeView.vue"),
                    meta: {
                        title: "个人主页",
                        requireLogin: true
                    }
                },
            ],
        },
        {
            path: '/',
            name: index,
            component: () => import("@/views/user/HomeView.vue"),
            meta: {
                title: "Cloudhub 法律案件资料库 - 可靠、专业的法律案件资料库",
                originalTitle: true
            },
        },
        {
            path: '/layout/header/login',
            name: loginLayout,
            redirect: '/error/404',
            component: () => import("@/views/user/LoginBackgroundLayout.vue"),
            children: [
                {
                    path: '/user/login',
                    name: login,
                    component: () => import("@/views/user/LoginView.vue"),
                    meta: {
                        title: "登录"
                    }
                },
                {
                    path: '/user/register',
                    name: register,
                    component: () => import("@/views/user/LoginView.vue"),
                    meta: {
                        title: "注册"
                    }
                },
            ]
        },
        {
            path: '/layout/header',
            name: headerLayout,
            redirect: '/',
            component: () => import("@/views/HeaderLayout.vue"),
            children: [
                {
                    path: '/user/reset/password',
                    name: passwordResetPage,
                    component: () => import("@/views/user/PasswordResetView.vue"),
                    meta: {
                        title: "重置密码"
                    }
                },
                {
                    path: '/about',
                    name: 'about',
                    component: () => import("@/views/system/About.vue"),
                    meta: {
                        title: "关于"
                    }
                },
                {
                    path: '/error/404',
                    name: page404,
                    component: () => import('@/views/NotFound.vue'),
                    meta: {
                        title: "404"
                    }
                },
                {
                    path: '/s/:token',
                    name: driveShareTokenPage,
                    component: () => import("@/views/share/ShareView.vue"),
                    meta: {
                        title: "分享",
                        requireLogin: false
                    }
                },
                {
                    path: '/:path(.*)*',
                    redirect: '/error/404'
                },
            ]
        }
    ]
})

const defaultTitle = "Cloudhub 法律案件资料库";

export const getTitleSuffix = () => {
    return " | Cloudhub 法律案件资料库 "
}

router.afterEach((to, from) => {
    if (to.meta.originalTitle) {
        document.title = to.meta.title
        return
    }

    document.title = to.meta.title ? to.meta.title + getTitleSuffix() : defaultTitle
})


router.beforeEach((to, from, next) => {
    const userStore = useUserStore()

    if (to.meta.requireLogin && !userStore.isLogin) {
        return next({
            name: login,
        })
    }

    if (!to.name.startsWith("admin")) {
        return next()
    }
    const role = userStore.user.role
    if (!userStore.isLogin || !role || role === "USER") {
        return next({
            name: page404
        })
    }
    return next()
})


export default router
