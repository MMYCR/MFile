<template>
  <div class="share-container">
    <el-card class="share-card">
      <template #header>
        <div class="header">
          <h2>🎁 文件分享</h2>
          <p>有人给你分享了一个文件</p>
        </div>
      </template>

      <div class="content">
        <div v-if="loading" class="status">
          <el-icon class="is-loading"><Loading /></el-icon>
          <p>正在获取文件信息...</p>
        </div>

        <div v-else-if="error" class="status error">
          <el-icon><CircleCloseFilled /></el-icon>
          <p>{{ error }}</p>
        </div>

        <div v-else class="success">
          <div class="file-icon">📄</div>
          <p class="tip">文件链接已生成，请点击下载</p>
          <el-button type="primary" size="large" round @click="download" class="dl-btn">
            <el-icon><Download /></el-icon> 立即下载
          </el-button>
        </div>
      </div>
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useRoute } from 'vue-router'
import request from '../utils/request'
import { Loading, CircleCloseFilled, Download } from '@element-plus/icons-vue'

const route = useRoute()
const loading = ref(true)
const error = ref('')
const downloadUrl = ref('')

// 初始化：拿着 uuid 去找后端要下载链接
onMounted(async () => {
  const uuid = route.params.uuid
  if (!uuid) {
    error.value = '无效的分享链接'
    loading.value = false
    return
  }

  try {
    // 调用后端公开接口
    const url: any = await request.get(`/share/download/${uuid}`)
    downloadUrl.value = url
    loading.value = false
  } catch (e: any) {
    error.value = e.message || '链接已过期或不存在'
    loading.value = false
  }
})

const download = () => {
  if (downloadUrl.value) {
    window.location.href = downloadUrl.value
  }
}
</script>

<style scoped>
.share-container {
  height: 100vh;
  display: flex;
  justify-content: center;
  align-items: center;
  background-color: #f0f2f5;
}
.share-card { width: 400px; text-align: center; border-radius: 12px; }
.header h2 { margin: 0; color: #303133; }
.header p { margin: 5px 0 0; color: #909399; font-size: 14px; }
.content { padding: 30px 0; }
.file-icon { font-size: 60px; margin-bottom: 20px; }
.status { color: #606266; }
.error { color: #F56C6C; font-size: 16px; }
.dl-btn { width: 80%; margin-top: 20px; font-weight: bold; }
</style>