<template>
  <div class="app-container">
    <el-container class="layout-container">

      <!-- 1. 顶部导航栏 -->
      <el-header class="app-header">
        <div class="brand">
          <div class="logo-icon">☁️</div>
          <span class="logo-text">Cloud Drive</span>
        </div>

        <!-- 右侧操作区 -->
        <div class="header-right">
          <!-- 后台管理入口 -->
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

            <!-- 工具栏 & 面包屑 -->
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
                <el-tooltip content="刷新列表" placement="top">
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

            <!-- 文件列表表格 -->
            <el-table
                :data="fileList"
                style="width: 100%"
                @row-click="handleRowClick"
                v-loading="loading"
                element-loading-text="加载中..."
                :header-cell-style="{background:'#f8f9fa', color:'#606266'}"
                class="custom-table"
            >
              <el-table-column label="文件名" min-width="250">
                <template #default="scope">
                  <div class="file-name-wrapper">
                    <div class="icon-box">
                      <span v-if="scope.row.type === 'FOLDER'">📂</span>
                      <span v-else>📄</span>
                    </div>
                    <span class="text">{{ scope.row.name }}</span>
                  </div>
                </template>
              </el-table-column>

              <el-table-column prop="time" label="修改时间" width="200" sortable>
                <template #default="scope">
                  <span style="color: #909399; font-size: 13px;">
                    {{ new Date(scope.row.time).toLocaleString() }}
                  </span>
                </template>
              </el-table-column>

              <el-table-column prop="size" label="大小" width="120" sortable>
                <template #default="scope">
                  <span style="color: #606266;">
                    {{ scope.row.type === 'FOLDER' ? '-' : formatSize(scope.row.size) }}
                  </span>
                </template>
              </el-table-column>

              <el-table-column label="操作" width="220" align="center" fixed="right">
                <template #default="scope">
                  <div class="row-actions" @click.stop>
                    <el-button
                        v-if="scope.row.type === 'FILE'"
                        type="primary"
                        link
                        @click="handleDownload(scope.row)"
                    >
                      下载
                    </el-button>
                    <!-- 分享按钮 -->
                    <el-button
                        v-if="scope.row.type === 'FILE'"
                        type="warning"
                        link
                        @click="handleShare(scope.row)"
                    >
                      分享
                    </el-button>
                    <el-button
                        type="danger"
                        link
                        @click="handleDelete(scope.row)"
                    >
                      删除
                    </el-button>
                  </div>
                </template>
              </el-table-column>

              <template #empty>
                <div class="empty-state">
                  <el-empty description="暂无文件，快去上传吧" :image-size="100" />
                </div>
              </template>
            </el-table>
          </div>
        </el-main>
      </el-container>
    </el-container>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, computed } from 'vue'
import { useRouter } from 'vue-router'
import request from '../utils/request'
import UploadButton from '../components/UploadButton.vue'
import { ElMessage, ElMessageBox } from 'element-plus'
// 🟢 修改点：引入 Setting 图标
import { DataBoard, SwitchButton, Refresh, FolderAdd, Setting } from '@element-plus/icons-vue'

const router = useRouter()
const storageList = ref<any[]>([])
const fileList = ref<any[]>([])
const currentStorageKey = ref('')
const currentPath = ref('/')
const loading = ref(false)
const passwordCache = ref<Record<string, string>>({})

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

const loadStorageList = async () => {
  try {
    const res: any = await request.get('/storage/list')
    storageList.value = res
    if (storageList.value.length > 0) {
      currentStorageKey.value = storageList.value[0].key
      loadFiles('/')
    }
  } catch (e) {
    console.error(e)
  }
}

const loadFiles = async (path: string) => {
  if (!currentStorageKey.value) return
  loading.value = true
  const cachedPwd = passwordCache.value[path] || ''

  try {
    const res: any = await request.get('/list', {
      params: {
        storageKey: currentStorageKey.value,
        path: path,
        password: cachedPwd
      }
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

const promptPassword = (path: string) => {
  ElMessageBox.prompt('🔒 该文件夹已加密，请输入访问密码', '安全验证', {
    confirmButtonText: '确认',
    cancelButtonText: '取消',
    inputType: 'password',
    inputErrorMessage: '密码不能为空',
    inputValidator: (val) => !!val
  }).then(({ value }) => {
    passwordCache.value[path] = value
    loadFiles(path)
  }).catch(() => {
    if (currentPath.value !== '/') {
      loadFiles(currentPath.value)
    } else {
      currentPath.value = '/'
    }
  })
}

const handleStorageSelect = (key: string) => {
  currentStorageKey.value = key
  loadFiles('/')
}

const handleRowClick = (row: any) => {
  if (row.type === 'FOLDER') {
    loadFiles(row.path)
  }
}

const handleDownload = async (row: any) => {
  try {
    const url: any = await request.get('/download/url', {
      params: { storageKey: currentStorageKey.value, path: row.path }
    })
    window.open(url)
  } catch (e) {
    ElMessage.error('获取链接失败')
  }
}

// 分享逻辑
const handleShare = (row: any) => {
  ElMessageBox.confirm(`确定要为 "${row.name}" 创建有效期 1 天的分享链接吗？`, '创建分享', {
    confirmButtonText: '生成链接',
    cancelButtonText: '取消'
  }).then(async () => {
    try {
      const res: any = await request.post('/share/create', null, {
        params: {
          storageKey: currentStorageKey.value,
          path: row.path,
          days: 1
        }
      })

      ElMessageBox.alert(
          `<div style="word-break: break-all;">${res}</div>`,
          '🎉 分享链接已生成',
          {
            dangerouslyUseHTMLString: true,
            confirmButtonText: '复制并关闭',
            callback: () => {
              navigator.clipboard.writeText(res)
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
  ElMessageBox.prompt('请输入新文件夹名称', '新建文件夹', {
    confirmButtonText: '创建',
    cancelButtonText: '取消'
  }).then(async ({ value }) => {
    await request.post('/mkdir', null, {
      params: {
        storageKey: currentStorageKey.value,
        path: currentPath.value,
        name: value
      }
    })
    ElMessage.success('创建成功')
    refresh()
  })
}

const handleDelete = (row: any) => {
  ElMessageBox.confirm(`确定要删除 "${row.name}" 吗？`, '警告', {
    confirmButtonText: '确定删除',
    cancelButtonText: '取消',
    type: 'warning'
  }).then(async () => {
    await request.delete('/delete', {
      params: {
        storageKey: currentStorageKey.value,
        path: row.path
      }
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

const formatSize = (bytes: number) => {
  if (bytes === 0) return '0 B'
  const k = 1024, sizes = ['B', 'KB', 'MB', 'GB', 'TB']
  const i = Math.floor(Math.log(bytes) / Math.log(k))
  return parseFloat((bytes / Math.pow(k, i)).toFixed(2)) + ' ' + sizes[i]
}
</script>

<style scoped>
.app-container { height: 100vh; background-color: #f0f2f5; display: flex; flex-direction: column; }
.layout-container { height: 100%; }

/* Header */
.app-header {
  background: white;
  border-bottom: 1px solid #e1e3e6;
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 0 24px;
  height: 60px;
  box-shadow: 0 1px 4px rgba(0,21,41,0.04);
  z-index: 10;
}
.brand { display: flex; align-items: center; gap: 10px; cursor: default; }
.logo-icon { font-size: 24px; }
.logo-text { font-size: 20px; font-weight: 600; color: #303133; letter-spacing: 0.5px; }
.header-right { display: flex; align-items: center; }
.user-welcome { font-size: 14px; color: #606266; margin-right: 15px; }

/* Aside */
.app-aside {
  background: #ffffff;
  border-right: 1px solid #f0f0f0;
  display: flex;
  flex-direction: column;
}
.storage-title {
  padding: 20px;
  font-size: 12px;
  color: #909399;
  text-transform: uppercase;
  letter-spacing: 1px;
}
.storage-menu { border-right: none; }
.storage-item {
  margin: 4px 10px;
  border-radius: 8px;
  height: 44px;
  line-height: 44px;
}
.storage-item.is-active {
  background-color: #e6f7ff;
  color: #1890ff;
  font-weight: 500;
}

/* Main */
.content-body { overflow: hidden; }
.app-main { padding: 16px; display: flex; flex-direction: column; overflow: hidden; }

.file-browser-card {
  background: white;
  border-radius: 12px;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.05);
  flex: 1;
  display: flex;
  flex-direction: column;
  overflow: hidden;
}

.browser-header {
  padding: 16px 24px;
  border-bottom: 1px solid #f0f0f0;
  display: flex;
  justify-content: space-between;
  align-items: center;
  background-color: #fafafa;
}
.breadcrumb-wrapper { flex: 1; overflow: hidden; }
.crumb-link { font-weight: 500; color: #606266; transition: all 0.2s; cursor: pointer; }
.crumb-link:hover { color: #409EFF; }
.action-group { display: flex; gap: 12px; align-items: center; }
.icon-btn { border: none; background: transparent; color: #606266; font-size: 18px; }
.icon-btn:hover { background: #e6f7ff; color: #409EFF; }

.custom-table { flex: 1; }
:deep(.el-table__row) { cursor: pointer; transition: background 0.2s; }
:deep(.el-table__row:hover) { background-color: #f5f7fa !important; }
.file-name-wrapper { display: flex; align-items: center; gap: 12px; }
.icon-box { font-size: 22px; width: 24px; text-align: center; }
.text { font-size: 14px; color: #303133; font-weight: 500; }
.row-actions { opacity: 0; transition: opacity 0.2s; }
.el-table__row:hover .row-actions { opacity: 1; }
.empty-state { padding: 60px 0; }
</style>