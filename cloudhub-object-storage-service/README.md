# cloudhub-client

Cloudhub对象存储服务实现，不作为CFS的一部分部署。

Object storage service implementation, not deployed as a part of CFS.

## 用户认证管理
不是特别的重点，略。

## 桶管理
桶作为对象的容器，由用户创建并管理起来。

不同的桶拥有不同的策略，桶一般不设置容量和数量硬上限。

桶策略：
- 公共读
- 公共读写
- 私有读写

还可以让用户设置特定用户的访问权限（ACL, Access Control List），这里不作为重点。

## 对象管理
允许用户管理存储于桶中的对象，如设置元数据。
> 会反映到下载相应的Headers中，一般为一一对应，
> 比如设置了`Content-Type`，那么在响应头中就要覆盖原来的`Content-Type`。

同样还有不可变的元数据或非用户可设置的项，比如对象大小等。

对象的元数据由于并非结构化，而为键值对的形式，一般应存储在NoSQL数据库中。

### 版本控制
存储对象近几个版本所对应的`file_id`，以达到版本控制的效果。版本号从1开始递增。