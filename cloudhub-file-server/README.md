# 文件服务器

文件服务器的部分工作流程说明。

## 文件存储

客户端上传文件后，分块发送到此文件服务器上，一般有多个块。

在接收到多个块之后，按照某种逻辑（依照文件哈希建立文件等）创建或者找到现存的一个容器文件进行写入。

一个容器文件应当由元数据和实际数据两部分组成。（分开成两个文件）

### 元数据文件

对于元数据文件来说，应当包含以下数据：
- 文件块大小
- 文件标识符（ID，可以取哈希值）
- 每个文件的起始块位置和终止块位置
- 该文件终止块的实际字节数

### 实际数据文件
对于实际数据文件来说，其格式应当与下类似：

假设块长64k。块长可以取64k或128k（4kb的倍数）。

> Linux ext3/4文件系统寻址的基本单位为4k，取倍数大概可以加快寻址速度。

结构：
```text
|-------(块数据)-------|--------(块数据)--------|
```

数据块位置：
```text
|----------0----------|-----------1-----------|
```

数据：
```text
|122311...............|.....100000000000000000|
----------------------------| 实际数据分块后一般不足以填充完整个块，后补字节0
```

这里假设最后一个块，即1号块中实际存储的字节数为2k。

则元数据文件中应当记录下以下信息：
- 块长64k
- 该文件标识符
- 该文件起始块为0，终止块为1
- 最后一个块的实际字节长度为2k

读取文件时，从起始块0号开始读取，应读取的字节数为64k + 2k。

## 文件多副本
文件服务器接收到文件后，确认文件存储完毕。
开始向元数据服务器分配的副本服务器发起复制副本请求。

传送文件副本时，只发送变化块。

也即请求需要包含变化块的容器地址、起始坐标、跨容器标记等。

### 文件副本传输
发起文件副本请求后，主机向副本服务器发送变化块数据。

副本服务器接收到请求后，将变化块写入到容器中。写入完成后计算校验码并发送给主机。
主机比对校验码，若为错误校验码则进行文件同步，整体传输容器。

## 文件服务器发起的文件同步
文件服务器向副本服务器发起文件同步请求。

将需要同步的容器发送给副本服务器，副本服务器接收写入完成后计算校验码发回主机。
主机比对校验码，若为错误校验码则重试同步，直到传输成功。

> 同步的是Container，对整个Container进行计算
>
> Container副本命名：server-id_container-id

## 元数据服务器发起的文件同步
> 文件副本传输不存在数据丢失的情况下，文件同步执行频率不需要太高。

元数据服务器定期向文件服务器发起文件同步请求，同时包含副本服务器的主机地址。

之后由持有此文件的主文件服务器向所有的副本服务器发出同步请求。
> 若主文件服务器宕机则查询所有副本服务器，获取校验码并进行比较，若全部错误则视为文件丢失不可复原。

后续流程和文件服务器发起的文件同步相同。
