<template>
  <div class="app-container">
    <el-container class="layout-container">

      <!-- 1. 顶部导航栏 -->
      <el-header class="app-header">
        <div class="brand">
          <div class="logo-icon">☁️</div>
          <span class="logo-text">Cloud Drive</span>
        </div>

        <div class="header-right">
          <el-button type="primary" link @click="$router.push('/admin')" style="margin-right: 15px">
            <el-icon style="margin-right: 4px"><Setting /></el-icon> 后台管理
          </el-button>
          <span class="user-welcome">管理员</span>
          <el-button type="danger" plain round size="small" @click="logout">
            <el-icon style="margin-right: 4px"><SwitchButton /></el-icon> 退出
          </el-button>
        </div>
      </el-header>

      <el-container class="content-body">
        <!-- 2. 左侧侧边栏 -->
        <el-aside width="240px" class="app-aside">
          <div class="storage-title">存储源</div>
          <el-menu
              :default-active="currentStorageKey"
              class="storage-menu"
              @select="handleStorageSelect"
          >
            <el-menu-item
                v-for="item in storageList"
                :key="item.key"
                :index="item.key"
                class="storage-item"
            >
              <el-icon><DataBoard /></el-icon>
              <span>{{ item.name }}</span>
            </el-menu-item>
          </el-menu>
        </el-aside>

        <!-- 3. 右侧主体 -->
        <el-main class="app-main">
          <div class="file-browser-card">

            <!-- 工具栏 -->
            <div class="browser-header">
              <div class="breadcrumb-wrapper">
                <el-breadcrumb separator-class="el-icon-arrow-right">
                  <el-breadcrumb-item>
                    <a class="crumb-link" @click="loadFiles('/')">🏠 根目录</a>
                  </el-breadcrumb-item>
                  <el-breadcrumb-item v-for="(p, index) in breadcrumbList" :key="index">
                    <a class="crumb-link" @click="loadFiles(p.fullPath)">{{ p.name }}</a>
                  </el-breadcrumb-item>
                </el-breadcrumb>
              </div>

              <div class="action-group">
                <UploadButton
                    v-if="currentStorageKey"
                    :storage-key="currentStorageKey"
                    :current-path="currentPath"
                    @success="refresh"
                />
                <el-tooltip content="刷新" placement="top">
                  <el-button class="icon-btn" circle @click="refresh">
                    <el-icon><Refresh /></el-icon>
                  </el-button>
                </el-tooltip>
                <el-tooltip content="新建文件夹" placement="top">
                  <el-button class="icon-btn" circle @click="handleMkdir">
                    <el-icon><FolderAdd /></el-icon>
                  </el-button>
                </el-tooltip>
              </div>
            </div>

            <!-- 文件列表 -->
            <el-table
                :data="fileList"
                style="width: 100%"
                @row-click="handleRowClick"
                v-loading="loading"
                class="custom-table"
            >
              <el-table-column label="文件名" min-width="250">
                <template #default="scope">
                  <div class="file-name-wrapper">
                    <div class="icon-box">
                      <!-- 🟢 简单的图标判断逻辑 -->
                      <span v-if="scope.row.type === 'FOLDER'">📂</span>
                      <span v-else-if="isImage(scope.row.name)">🖼️</span>
                      <span v-else-if="isVideo(scope.row.name)">🎬</span>
                      <span v-else-if="isText(scope.row.name)">📝</span>
                      <span v-else>📄</span>
                    </div>
                    <span class="text">{{ scope.row.name }}</span>
                  </div>
                </template>
              </el-table-column>

              <el-table-column prop="time" label="修改时间" width="200">
                <template #default="scope">
                  <span style="color: #909399; font-size: 13px;">
                    {{ new Date(scope.row.time).toLocaleString() }}
                  </span>
                </template>
              </el-table-column>

              <el-table-column prop="size" label="大小" width="120">
                <template #default="scope">
                  <span style="color: #606266;">
                    {{ scope.row.type === 'FOLDER' ? '-' : formatSize(scope.row.size) }}
                  </span>
                </template>
              </el-table-column>

              <el-table-column label="操作" width="250" align="center" fixed="right">
                <template #default="scope">
                  <div class="row-actions" @click.stop>
                    <!-- 预览/下载 -->
                    <el-button
                        v-if="scope.row.type === 'FILE'"
                        type="primary" link
                        @click="handlePreviewOrDownload(scope.row)"
                    >
                      {{ isImage(scope.row.name) ? '预览' : '下载' }}
                    </el-button>

                    <el-button type="primary" link @click="handleOpenRename(scope.row)">
                      重命名
                    </el-button>

                    <el-button type="warning" link @click="handleShare(scope.row)">
                      分享
                    </el-button>

                    <el-button type="danger" link @click="handleDelete(scope.row)">
                      删除
                    </el-button>
                  </div>
                </template>
              </el-table-column>
            </el-table>
          </div>
        </el-main>
      </el-container>

      <!--  重命名弹窗 -->
      <el-dialog v-model="renameVisible" title="重命名" width="400px">
        <el-form @submit.prevent>
          <el-form-item label="新名称">
            <el-input v-model="renameForm.newName" @keyup.enter="submitRename" />
          </el-form-item>
        </el-form>
        <template #footer>
          <el-button @click="renameVisible = false">取消</el-button>
          <el-button type="primary" @click="submitRename" :loading="renameLoading">确定</el-button>
        </template>
      </el-dialog>

      <!--  图片预览组件 -->
      <el-image-viewer
          v-if="showImageViewer"
          :url-list="[previewUrl]"
          @close="showImageViewer = false"
      />

    </el-container>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, computed, reactive } from 'vue'
import { useRouter } from 'vue-router'
import request from '../utils/request'
import UploadButton from '../components/UploadButton.vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { DataBoard, SwitchButton, Refresh, FolderAdd, Setting } from '@element-plus/icons-vue'

const router = useRouter()
const storageList = ref<any[]>([])
const fileList = ref<any[]>([])
const currentStorageKey = ref('')
const currentPath = ref('/')
const loading = ref(false)
const passwordCache = ref<Record<string, string>>({})

// 🟢 新增状态
const renameVisible = ref(false)
const renameLoading = ref(false)
const renameForm = reactive({ oldName: '', newName: '' })
const showImageViewer = ref(false)
const previewUrl = ref('')

const breadcrumbList = computed(() => {
  if (currentPath.value === '/') return []
  const parts = currentPath.value.split('/').filter(p => p)
  let full = ''
  return parts.map(p => {
    full += '/' + p
    return { name: p, fullPath: full }
  })
})

onMounted(async () => {
  await loadStorageList()
})

// --- 基础加载逻辑 ---
const loadStorageList = async () => {
  try {
    const res: any = await request.get('/storage/list')
    storageList.value = res
    if (storageList.value.length > 0) {
      currentStorageKey.value = storageList.value[0].key
      loadFiles('/')
    }
  } catch (e) { console.error(e) }
}

const loadFiles = async (path: string) => {
  if (!currentStorageKey.value) return
  loading.value = true
  const cachedPwd = passwordCache.value[path] || ''

  try {
    const res: any = await request.get('/list', {
      params: { storageKey: currentStorageKey.value, path, password: cachedPwd }
    })
    fileList.value = res
    currentPath.value = path
  } catch (err: any) {
    if (err.message && err.message.includes('密码')) {
      promptPassword(path)
    } else {
      fileList.value = []
    }
  } finally {
    loading.value = false
  }
}

// --- 交互逻辑 ---

// 1. 智能判断：图片预览 / 文件下载
const handlePreviewOrDownload = async (row: any) => {
  try {
    // 先获取带签名的 URL (如果是 OSS) 或 后端代理 URL (如果是 Local)
    const url: any = await request.get('/download/url', {
      params: { storageKey: currentStorageKey.value, path: row.path }
    })

    if (isImage(row.name)) {
      previewUrl.value = url
      showImageViewer.value = true
    } else {
      window.open(url)
    }
  } catch (e) {
    ElMessage.error('获取文件失败')
  }
}

// 2. 重命名逻辑
const handleOpenRename = (row: any) => {
  renameForm.oldName = row.name
  renameForm.newName = row.name
  renameVisible.value = true
}

const submitRename = async () => {
  if (!renameForm.newName) return ElMessage.warning('名称不能为空')
  if (renameForm.newName === renameForm.oldName) {
    renameVisible.value = false
    return
  }

  renameLoading.value = true
  try {
    //  调用后端 /api/rename 接口
    await request.post('/rename', null, {
      params: {
        storageKey: currentStorageKey.value,
        path: currentPath.value, // 父目录
        name: renameForm.oldName,
        newName: renameForm.newName
      }
    })
    ElMessage.success('重命名成功')
    renameVisible.value = false
    refresh()
  } catch (e) {
    // 拦截器已处理错误
  } finally {
    renameLoading.value = false
  }
}

const promptPassword = (path: string) => {
  ElMessageBox.prompt('🔒 该文件夹已加密', '验证', {
    inputType: 'password',
    inputValidator: (val) => !!val,
    inputErrorMessage: '密码不能为空'
  }).then(({ value }) => {
    passwordCache.value[path] = value
    loadFiles(path)
  }).catch(() => {
    if(currentPath.value !== '/') loadFiles(currentPath.value)
    else currentPath.value = '/'
  })
}

const handleStorageSelect = (key: string) => {
  currentStorageKey.value = key
  loadFiles('/')
}

const handleRowClick = (row: any) => {
  if (row.type === 'FOLDER') loadFiles(row.path)
}

const handleShare = (row: any) => {
  ElMessageBox.confirm(`确定要为 "${row.name}" 创建有效期 1 天的分享链接吗？`, '创建分享', {
    confirmButtonText: '生成链接',
    cancelButtonText: '取消'
  }).then(async () => {
    try {
      // 后端返回的 uuid
      const uuid: any = await request.post('/share/create', null, {
        params: {
          storageKey: currentStorageKey.value,
          path: row.path,
          days: 1
        }
      })

      // 前端自动拼接当前域名 + /s/ + uuid
      const fullLink = `${window.location.origin}/s/${uuid}`

      ElMessageBox.alert(
          `<div style="word-break: break-all;">${fullLink}</div>`,
          '🎉 分享链接已生成',
          {
            dangerouslyUseHTMLString: true,
            confirmButtonText: '复制并关闭',
            callback: () => {
              navigator.clipboard.writeText(fullLink)
              ElMessage.success('已复制到剪贴板')
            }
          }
      )
    } catch (e) {
      ElMessage.error('生成失败')
    }
  })
}

const handleMkdir = () => {
  ElMessageBox.prompt('文件夹名称', '新建', { confirmButtonText: '创建' })
      .then(async ({ value }) => {
        await request.post('/mkdir', null, {
          params: { storageKey: currentStorageKey.value, path: currentPath.value, name: value }
        })
        ElMessage.success('创建成功')
        refresh()
      })
}

const handleDelete = (row: any) => {
  ElMessageBox.confirm(`确认删除 "${row.name}" ？`, '警告', { type: 'warning' })
      .then(async () => {
        await request.delete('/delete', {
          params: { storageKey: currentStorageKey.value, path: row.path }
        })
        ElMessage.success('已删除')
        refresh()
      })
}

const refresh = () => loadFiles(currentPath.value)
const logout = () => {
  localStorage.removeItem('satoken')
  router.push('/login')
}

// --- 工具函数 ---
const formatSize = (bytes: number) => {
  if (bytes === 0) return '0 B'
  const k = 1024, sizes = ['B', 'KB', 'MB', 'GB', 'TB']
  const i = Math.floor(Math.log(bytes) / Math.log(k))
  return parseFloat((bytes / Math.pow(k, i)).toFixed(2)) + ' ' + sizes[i]
}

// 简单的文件类型判断
const isImage = (name: string) => /\.(jpg|jpeg|png|gif|bmp|webp)$/i.test(name)
const isVideo = (name: string) => /\.(mp4|mkv|mov|avi)$/i.test(name)
const isText = (name: string) => /\.(txt|md|json|java|js|html|xml|yml|properties)$/i.test(name)

</script>

<style scoped>
.app-container { height: 100vh; background-color: #f0f2f5; display: flex; flex-direction: column; }
.layout-container { height: 100%; }

/* Header */
.app-header {
  background: white; border-bottom: 1px solid #e1e3e6;
  display: flex; justify-content: space-between; align-items: center;
  padding: 0 24px; height: 60px; box-shadow: 0 1px 4px rgba(0,21,41,0.04); z-index: 10;
}
.brand { display: flex; align-items: center; gap: 10px; cursor: default; }
.logo-icon { font-size: 24px; }
.logo-text { font-size: 20px; font-weight: 600; color: #303133; }
.header-right { display: flex; align-items: center; }
.user-welcome { font-size: 14px; color: #606266; margin-right: 15px; }

/* Aside */
.app-aside {
  background: #ffffff; border-right: 1px solid #f0f0f0; display: flex; flex-direction: column;
}
.storage-title { padding: 20px; font-size: 12px; color: #909399; text-transform: uppercase; }
.storage-menu { border-right: none; }
.storage-item { margin: 4px 10px; border-radius: 8px; height: 44px; line-height: 44px; }
.storage-item.is-active { background-color: #e6f7ff; color: #1890ff; font-weight: 500; }

/* Main */
.app-main { padding: 16px; display: flex; flex-direction: column; overflow: hidden; }
.file-browser-card {
  background: white; border-radius: 12px; box-shadow: 0 4px 12px rgba(0, 0, 0, 0.05);
  flex: 1; display: flex; flex-direction: column; overflow: hidden;
}
.browser-header {
  padding: 16px 24px; border-bottom: 1px solid #f0f0f0;
  display: flex; justify-content: space-between; align-items: center; background-color: #fafafa;
}
.crumb-link { font-weight: 500; color: #606266; cursor: pointer; }
.crumb-link:hover { color: #409EFF; }
.action-group { display: flex; gap: 12px; align-items: center; }
.icon-btn { border: none; background: transparent; color: #606266; font-size: 18px; }
.icon-btn:hover { background: #e6f7ff; color: #409EFF; }

.custom-table { flex: 1; }
:deep(.el-table__row) { cursor: pointer; transition: background 0.2s; }
.file-name-wrapper { display: flex; align-items: center; gap: 12px; }
.icon-box { font-size: 22px; width: 24px; text-align: center; }
.row-actions { opacity: 0; transition: opacity 0.2s; }
.el-table__row:hover .row-actions { opacity: 1; }
.empty-state { padding: 60px 0; }
</style>