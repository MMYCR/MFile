<template>
  <div class="admin-container">
    <el-card class="box-card">
      <template #header>
        <div class="card-header">
          <h2>⚙️ 存储源管理</h2>
          <div>
            <el-button @click="$router.push('/')">返回首页</el-button>
            <el-button type="primary" @click="handleAdd">新增存储源</el-button>
          </div>
        </div>
      </template>

      <!-- 1. 列表区域 -->
      <el-table :data="tableData" stripe style="width: 100%" v-loading="loading">
        <el-table-column prop="id" label="ID" width="80" />
        <el-table-column prop="name" label="名称" />
        <el-table-column prop="type" label="类型" width="100">
          <template #default="scope">
            <el-tag :type="scope.row.type === 'LOCAL' ? 'success' : 'warning'">
              {{ scope.row.type }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="key" label="别名 (Key)" />
        <el-table-column prop="orderNum" label="排序" width="80" />
        <el-table-column label="操作" width="180" align="center">
          <template #default="scope">
            <el-button size="small" @click="handleEdit(scope.row)">编辑</el-button>
            <el-button size="small" type="danger" @click="handleDelete(scope.row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <!-- 2. 编辑/新增 弹窗 -->
    <el-dialog
        v-model="dialogVisible"
        :title="form.id ? '编辑存储源' : '新增存储源'"
        width="600px"
    >
      <el-form :model="form" label-width="120px">
        <el-form-item label="名称">
          <el-input v-model="form.name" placeholder="例如：我的本地盘" />
        </el-form-item>
        <el-form-item label="别名 (Key)">
          <el-input v-model="form.key" placeholder="唯一标识，如 local" />
        </el-form-item>
        <el-form-item label="排序号">
          <el-input-number v-model="form.orderNum" :min="0" />
        </el-form-item>
        <el-form-item label="存储类型">
          <el-select v-model="form.type" placeholder="请选择类型">
            <el-option label="本地存储 (Local)" value="LOCAL" />
            <el-option label="阿里云 OSS" value="ALIYUN" />
          </el-select>
        </el-form-item>

        <!-- 🟢 动态表单区域 -->

        <!-- A. 本地存储字段 -->
        <template v-if="form.type === 'LOCAL'">
          <el-form-item label="本地根路径">
            <el-input v-model="form.rootPath" placeholder="例如：D:/Data 或 /root/files" />
          </el-form-item>
        </template>

        <!-- B. 阿里云 OSS 字段 -->
        <template v-if="form.type === 'ALIYUN'">
          <el-form-item label="Endpoint">
            <el-input v-model="ossConfig.endpoint" placeholder="oss-cn-hangzhou.aliyuncs.com" />
          </el-form-item>
          <el-form-item label="Bucket Name">
            <el-input v-model="ossConfig.bucketName" placeholder="存储桶名称" />
          </el-form-item>
          <el-form-item label="AccessKey">
            <el-input v-model="ossConfig.accessKey" placeholder="RAM账号 AK" />
          </el-form-item>
          <el-form-item label="SecretKey">
            <el-input v-model="ossConfig.secretKey" type="password" show-password placeholder="RAM账号 SK" />
          </el-form-item>
        </template>

      </el-form>
      <template #footer>
        <span class="dialog-footer">
          <el-button @click="dialogVisible = false">取消</el-button>
          <el-button type="primary" @click="handleSave">保存</el-button>
        </span>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, reactive } from 'vue'
import request from '../utils/request'
import { ElMessage, ElMessageBox } from 'element-plus'

const loading = ref(false)
const tableData = ref([])
const dialogVisible = ref(false)

// 表单数据模型
const form = reactive({
  id: null,
  name: '',
  key: '',
  type: 'LOCAL',
  orderNum: 0,
  rootPath: ''
})

// OSS 特有配置 (独立出来，保存时合并成 JSON)
const ossConfig = reactive({
  endpoint: '',
  bucketName: '',
  accessKey: '',
  secretKey: ''
})

onMounted(() => {
  loadList()
})

const loadList = async () => {
  loading.value = true
  try {
    const res: any = await request.get('/admin/storage/list')
    tableData.value = res
  } finally {
    loading.value = false
  }
}

const handleAdd = () => {
  // 重置表单
  form.id = null
  form.name = ''
  form.key = ''
  form.type = 'LOCAL'
  form.orderNum = 0
  form.rootPath = ''
  // 重置 OSS Config
  ossConfig.endpoint = ''
  ossConfig.bucketName = ''
  ossConfig.accessKey = ''
  ossConfig.secretKey = ''

  dialogVisible.value = true
}

const handleEdit = (row: any) => {
  // 回显基础数据
  form.id = row.id
  form.name = row.name
  form.key = row.key
  form.type = row.type.code || row.type // 兼容枚举或字符串
  form.orderNum = row.orderNum
  form.rootPath = row.rootPath

  // 回显 OSS 数据 (解析 JSON)
  if (row.type === 'ALIYUN' && row.configData) {
    try {
      const conf = JSON.parse(row.configData)
      ossConfig.endpoint = conf.endpoint
      ossConfig.bucketName = conf.bucketName
      ossConfig.accessKey = conf.accessKey
      ossConfig.secretKey = conf.secretKey
    } catch (e) {
      console.error('JSON解析失败')
    }
  }
  dialogVisible.value = true
}

const handleSave = async () => {
  // 构造提交参数
  const payload: any = { ...form }

  if (form.type === 'ALIYUN') {
    // 如果是 OSS，打包 configData
    payload.configData = JSON.stringify(ossConfig)
    payload.rootPath = null // OSS 不需要 rootPath
  } else {
    // 如果是 Local，清空 configData
    payload.configData = null
  }

  try {
    await request.post('/admin/storage/save', payload)
    ElMessage.success('保存成功')
    dialogVisible.value = false
    loadList()
  } catch (e) {
    // 错误拦截器会处理
  }
}

const handleDelete = (row: any) => {
  ElMessageBox.confirm('确定要删除该存储源吗？', '警告', {
    confirmButtonText: '删除',
    cancelButtonText: '取消',
    type: 'warning'
  }).then(async () => {
    await request.delete(`/admin/storage/delete/${row.id}`)
    ElMessage.success('删除成功')
    loadList()
  })
}
</script>

<style scoped>
.admin-container { padding: 20px; background-color: #f0f2f5; min-height: 100vh; }
.card-header { display: flex; justify-content: space-between; align-items: center; }
</style>