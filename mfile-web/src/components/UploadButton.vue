<template>
  <div>
    <input type="file" ref="fileInput" style="display: none" @change="handleFileChange" />

    <el-button type="primary" @click="triggerSelect" :loading="uploading">
      <el-icon style="margin-right: 5px"><Upload /></el-icon>
      {{ uploading ? '上传中...' : '上传文件' }}
    </el-button>
  </div>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import request from '../utils/request' // 我们封装的带 Token 的 axios
import axios from 'axios' // 原生 axios，用于直传（不带 Token）
import { ElMessage } from 'element-plus'
import { Upload } from '@element-plus/icons-vue'

// 接收参数
const props = defineProps<{
  storageKey: string
  currentPath: string
}>()

const emit = defineEmits(['success'])
const fileInput = ref<HTMLInputElement | null>(null)
const uploading = ref(false)

const triggerSelect = () => fileInput.value?.click()

const handleFileChange = async (e: Event) => {
  const target = e.target as HTMLInputElement
  if (!target.files || target.files.length === 0) return

  const file = target.files[0]
  uploading.value = true

  try {
    // 1. 核心：请求上传策略
    const infoRes: any = await request.get('/upload/info', {
      params: {
        storageKey: props.storageKey,
        path: props.currentPath,
        fileName: file.name
      }
    })

    const { uploadType, postUrl, formData } = infoRes

    // 2. 根据策略上传
    const data = new FormData()

    if (uploadType === 'DIRECT') {
      console.log('🚀 策略路由：OSS 直传模式')
      // 填入签名参数
      if (formData) {
        for (const key in formData) {
          data.append(key, formData[key])
        }
      }
      data.append('file', file)
      // 直传不带 Token
      await axios.post(postUrl, data)
    } else {
      console.log('🚚 策略路由：后端代理模式')
      data.append('file', file)
      data.append('storageKey', props.storageKey)
      data.append('path', props.currentPath)
      // 代理上传带 Token
      await request.post(postUrl, data)
    }

    ElMessage.success('上传成功')
    emit('success') // 通知刷新列表

  } catch (e: any) {
    console.error(e)
    ElMessage.error('上传失败')
  } finally {
    uploading.value = false
    if (fileInput.value) fileInput.value.value = ''
  }
}
</script>