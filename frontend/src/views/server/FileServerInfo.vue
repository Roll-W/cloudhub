<template>
  <ContentBase>
    <div class="mb-4 ms-2 d-flex flex-grow-1 flex-fill">
      <h3>文件服务器：{{ fileServerId }}</h3>
      <div class="d-flex flex-fill justify-content-end">
        <button type="button" class="btn btn-outline-primary ms-2" @click="back">返回集群信息</button>
      </div>
    </div>
    <div class="row">
      <div class="row">
        <ContentBase>
          <div>Runtime</div>
          <hr>
          <!-- 展示运行环境信息 -->
          <div class="col-12">
            <div class="row">
              <div class="col-6">
                <div class="row">
                  <div class="col">
                    主机名
                  </div>
                  <div class="col">
                    {{ meta.env.hostName }}
                  </div>
                </div>
                <div class="row">
                  <div class="col">
                    主机地址
                  </div>
                  <div class="col">
                    {{ meta.env.hostAddress }}
                  </div>
                </div>
                <div class="row">
                  <div class="col">
                    运行用户
                  </div>
                  <div class="col">
                    {{ meta.env.runUser }}
                  </div>
                </div>
                <div class="row">
                  <div class="col">
                    OS 架构
                  </div>
                  <div class="col">
                    {{ meta.env.osArch }}
                  </div>
                </div>
              </div>

              <div class="col-6">
                <div class="row">
                  <div class="col">
                    工作文件夹
                  </div>
                  <div class="col">
                    {{ meta.env.workDir }}
                  </div>
                </div>
                <div class="row">
                  <div class="col">
                    Java 版本
                  </div>
                  <div class="col">
                    {{ meta.env.javaVersion }}
                  </div>
                </div>
                <div class="row">
                  <div class="col">
                    Java Home
                  </div>
                  <div class="col">
                    {{ meta.env.javaHome }}
                  </div>
                </div>
                <div class="row">
                  <div class="col">
                    OS 名称
                  </div>
                  <div class="col">
                    {{ meta.env.osName }}
                  </div>
                </div>
              </div>
            </div>

          </div>
        </ContentBase>
      </div>
      <div class="row">
        <div class="col-4 h-100">
          <ContentBase>
            <div>JVM</div>

            <hr>
            <div class="col">
              <div class="row">
                <div class="col">
                  max (MB)
                </div>
                <div class="col">
                  {{ (meta.jvm.max / (1024 * 1024.0)).toFixed(2) }}
                </div>
              </div>
              <div class="row">
                <div class="col">
                  used (byte)
                </div>
                <div class="col">
                  {{ (meta.jvm.used / (1024 * 1024.0)).toFixed(2) }}
                </div>
              </div>
              <div class="row">
                <div class="col">
                  free (MB)
                </div>
                <div class="col">
                  {{ (meta.jvm.free / (1024 * 1024.0)).toFixed(2) }}
                </div>
              </div>

              <div class="row" >
                <!-- 展示内存使用率 -->
                <JvmEcharts :Info="meta.jvm" style="width: 500px; height: 80px"></JvmEcharts>
              </div>
            </div>
          </ContentBase>
        </div>
        <div class="col-4">
          <ContentBase>
            <div>System Memory</div>
            <hr>
            <div class="col">
              <div class="row">
                <div class="col">
                  total (MB)
                </div>
                <div class="col">
                  {{ (meta.mem.total / (1024 * 1024.0)).toFixed(2) }}
                </div>
              </div>
              <div class="row">
                <div class="col">
                  used (MB)
                </div>
                <div class="col">
                  {{ (meta.mem.used / (1024 * 1024.0)).toFixed(2) }}
                </div>
              </div>
              <div class="row">
                <div class="col">
                  free (MB)
                </div>
                <div class="col">
                  {{ (meta.mem.free / (1024 * 1024.0)).toFixed(2) }}
                </div>
              </div>
              <div class="row">
                <!-- 展示内存使用率 -->
                <CategoryEchartsBase :Info="meta.mem" style="width: 500px; height: 80px"></CategoryEchartsBase>
              </div>
            </div>
          </ContentBase>
        </div>

        <div class="col-4">
          <ContentBase>
            <div>System Disk</div>
            <hr>
            <div class="col">
              <div class="row">
                <div class="col">
                  total (MB)
                </div>
                <div class="col">
                  {{ (meta.disk.total / (1024 * 1024.0)).toFixed(2) }}
                </div>
              </div>
              <div class="row">
                <div class="col">
                  used (MB)
                </div>
                <div class="col">
                  {{ ((meta.disk.total - meta.disk.free) / (1024 * 1024.0)).toFixed(2) }}
                </div>
              </div>
              <div class="row">
                <div class="col">
                  free (MB)
                </div>
                <div class="col">
                  {{ (meta.disk.free / (1024 * 1024.0)).toFixed(2) }}
                </div>
              </div>
              <div class="row">
                <diskEcharts :Info="meta.disk" style="width: 500px; height: 80px"></diskEcharts>
              </div>
            </div>
          </ContentBase>
        </div>
      </div>
    </div>
    <!-- CPU运行信息 -->
    <div class="row">
      <ContentBase>
        <div>CPU {{ meta.cpu.cpuCores }} 核</div>
        <hr>
        <div class="row">
          <div class="col">
            <!-- 系统使用率 -->
            <PieEchartsBase :Info="meta.cpu"></PieEchartsBase>
          </div>
          <div class="col">
            <!-- 用户使用率 -->
            <userEcharts :Info="meta.cpu"></userEcharts>
          </div>
          <div class="col">
            <!-- IO 等待率 -->
            <freeEcharts :Info="meta.cpu"></freeEcharts>
          </div>
          <div class="col">
            <!-- 空闲率 -->
            <waitEcharts :Info="meta.cpu"></waitEcharts>
          </div>
        </div>
      </ContentBase>
    </div>
    <!-- 网络信息 -->
    <div class="row">
      <ContentBase>
        <div>Speed</div>
        <hr>
        <div class="row">
          <div class="col">
            <!-- 网络IO -->
            <ReceiveAndSend :server="fileServerId" class="w-100 vh-50"></ReceiveAndSend>
          </div>
          <div class="col">
            <!-- 存储器IO -->
            <StorageIO :server="fileServerId" class="w-100 vh-50"></StorageIO>
          </div>
        </div>
      </ContentBase>
    </div>

  </ContentBase>
</template>

<script>
import ContentBase from "@/components/common/ContentBase";
import {useRouter} from "vue-router";
import JvmEcharts from "@/components/echarts/storage/JvmEcharts";
import PieEchartsBase from "@/components/echarts/cpu/PieEchartsBase";
import CategoryEchartsBase from "@/components/echarts/storage/CategoryEchartsBase";
import diskEcharts from "@/components/echarts/storage/diskEcharts";
import ReceiveAndSend from "@/components/echarts/network/ReceiveAndSend";
import StorageIO from "@/components/echarts/storage/StorageIO";
import freeEcharts from "@/components/echarts/cpu/freeEcharts";
import userEcharts from "@/components/echarts/cpu/userEcharts";
import waitEcharts from "@/components/echarts/cpu/waitEcharts";
import {reactive, ref} from "vue";
import $ from "jquery";
import url from "@/store/api";

export default {
  name: "FileServerInfo",
  components: {
    JvmEcharts,
    PieEchartsBase,
    CategoryEchartsBase,
    ContentBase,
    diskEcharts,
    ReceiveAndSend,
    StorageIO,
    freeEcharts,
    userEcharts,
    waitEcharts,
  },
  setup() {
    const router = useRouter()
    const fileServerId = router.currentRoute.value.params.id

    const back = () => {
      router.push({name: "cluster_index"})
    }

    const data = ref({
      // CPU 返回使用率的百分比
      cpu: {
        cpuCores: 16,// CPU核数
        sysUsed: 3.16,// 系统使用率
        userUsed: 14.81,// 用户使用率
        wait: 0.0,// IO等待率
        free: 80.97// 空闲率
      },
      jvm: {// JVM信息 均为字节数
        total: 71303168,// JVM总内存
        max: 4240441344,
        free: 35657160,
        used: 35646008
      },
      mem: {// 系统内存 均为字节数
        total: 16953597952,
        used: 14245462016,
        free: 2708135936,
        write: 1122// 写速率
      },
      disk: {// 磁盘信息
        total: 330966233088,// 当前分区总空间
        free: 86880907264,// 剩余空间
        read: 16384,// 读速率 均为bytes/s
        write: 1234// 写速率
      },
      net: {// 网络信息 均为bytes/s
        recv: 18178.0,// 接收
        sent: 450020.0,// 发送
        speed: 1000000000// 理论最大速度
      },
      env: {// 运行环境信息
        hostName: "Dawn",// 主机名
        hostAddress: "10.100.159.31",
        runUser: "user",
        userHome: "C:\\Users\\user",
        workDir: "D:\\Code\\Java\\cloudhub",
        javaVersion: "17.0.1",
        javaHome: "D:\\tools\\jdk17",
        osName: "Windows 10",
        osVersion: "10.0",
        osArch: "amd64"
      },
    })


    // 将 data 中的键对数据进行再封装用于父子传参

    // 4 个 CPU 使用率
    const sysUsed = {title: "系统使用率", ratio: data.value.cpu.sysUsed}
    const userUsed = {title: "用户使用率", ratio: data.value.cpu.userUsed}
    const wait = {title: "IO等待率", ratio: data.value.cpu.wait}
    const free = {title: "空闲率", ratio: data.value.cpu.free}

    // 2 个 存储器使用率
    const memory = {title: "内存使用率", ratio: (data.value.mem.used) / (data.value.mem.total)}
    const disk = {title: "磁盘使用率", ratio: (data.value.disk.total - data.value.disk.free) / (data.value.disk.total)}

    const meta = reactive({
      cpu: {
        free: null
      },
      jvm: {},
      mem: {},
      disk: {},
      net: {},
      env: {},
    })

    //获取元数据服务器信息
    const getNet = () => {
      $.ajax({
        url: url.url_metaServer,
        type: "GET",
        data: {
          serverId: fileServerId
        },
        xhrFields: {
          withCredentials: true
        },
        crossDomain: true,
        success(resp) {
          if (resp.errorCode === "00000") {
            meta.cpu = resp.data.cpu
            meta.jvm = resp.data.jvm
            meta.mem = resp.data.mem
            meta.disk = resp.data.disk
            meta.net = resp.data.net
            meta.env = resp.data.env
            console.log("file server="+ fileServerId)
            console.log(meta.jvm)
            console.log(meta.cpu)
          }
        },
        error() {
          console.log("获取失败")
        }
      })
    };

    getNet();


    return {
      fileServerId,
      back,
      data,
      sysUsed,
      userUsed,
      wait,
      free,
      memory,
      disk,
      meta,
    }
  }
}

</script>
