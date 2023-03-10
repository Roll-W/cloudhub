# 前端

Cloudhub的前端部分。

## Project setup
```bash
npm install
```

### Compiles and hot-reloads for development
```bash
npm run serve
```

### Compiles and minifies for production
```bash
npm run build
```

## 文件服务器
- 服务器信息查看
  - 剩余磁盘空间
  - CPU之类的使用信息，运行环境信息
  - 实时流量*
- 当前存储信息
  - 容器使用信息

## 元数据服务器
- 已连接文件服务器节点信息
- 文件查看
  - 查看文件位置、文件信息、副本信息。
  - 文件信息可视化*
- 本机服务器信息（和文件服务器类似）
- 实时流量信息*

## 客户端
### 管理员
- 用户管理
  - 用户添加、删除、禁用等
- 桶管理
  - 所有用户创建桶的信息(可视化*)
  - 查看单个用户创建的所有或单个桶信息等
  - 修改用户桶权限
  - 文件管理
    - 查看、修改桶内的文件信息
    - 用户上传文件信息可视化*

### 普通用户
- 桶管理
  - 创建桶
  - 修改桶权限
  - 文件管理
    - 查看文件数据信息
    - 上传、下载、删除
  - 桶内上传文件信息可视化*
- 登录页面
- 注册页面
- 用户信息页面
