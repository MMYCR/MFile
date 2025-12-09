<template>
  <div class="login-container">
    <el-card class="login-card">
      <template #header>
        <div class="header-title">
          <h2>📂 MFile 个人网盘</h2>
          <p>{{ isRegister ? '创建新账号' : '欢迎回来' }}</p>
        </div>
      </template>

      <el-form :model="form" @submit.prevent="handleSubmit" size="large">
        <el-form-item>
          <el-input
              v-model="form.username"
              placeholder="请输入用户名"
              :prefix-icon="User"
          />
        </el-form-item>

        <el-form-item>
          <el-input
              v-model="form.password"
              type="password"
              placeholder="请输入密码"
              show-password
              :prefix-icon="Lock"
          />
        </el-form-item>

        <el-form-item v-if="isRegister">
          <el-input
              v-model="form.confirmPassword"
              type="password"
              placeholder="请确认密码"
              show-password
              :prefix-icon="Lock"
              @keyup.enter="handleSubmit"
          />
        </el-form-item>

        <el-button type="primary" class="login-btn" :loading="loading" @click="handleSubmit">
          {{ isRegister ? '立即注册' : '登录' }}
        </el-button>

        <div class="footer-links">
          <el-link type="info" :underline="false" @click="toggleMode">
            {{ isRegister ? '已有账号？去登录' : '没有账号？去注册' }}
          </el-link>
        </div>
      </el-form>
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import { useRouter } from 'vue-router'
import request from '../utils/request'
import { ElMessage } from 'element-plus'
import { User, Lock } from '@element-plus/icons-vue'

const router = useRouter()
const loading = ref(false)
const isRegister = ref(false) // 控制当前模式

const form = ref({
  username: '',
  password: '',
  confirmPassword: ''
})

// 切换模式
const toggleMode = () => {
  isRegister.value = !isRegister.value
  form.value = { username: '', password: '', confirmPassword: '' } // 清空表单
}

const handleSubmit = async () => {
  if (!form.value.username || !form.value.password) {
    return ElMessage.warning('请输入用户名和密码')
  }

  if (isRegister.value && form.value.password !== form.value.confirmPassword) {
    return ElMessage.warning('两次输入的密码不一致')
  }

  loading.value = true

  try {
    if (isRegister.value) {
      // === 注册逻辑 ===
      await request.post('/register', {
        username: form.value.username,
        password: form.value.password
      })
      ElMessage.success('注册成功，请登录')
      toggleMode() // 切回登录模式
    } else {
      // === 登录逻辑 ===
      const token = await request.post('/login', {
        username: form.value.username,
        password: form.value.password
      })
      localStorage.setItem('satoken', token as unknown as string)
      ElMessage.success('登录成功')
      router.push('/')
    }
  } catch (e: any) {
    // 错误处理由拦截器统一完成
  } finally {
    loading.value = false
  }
}
</script>

<style scoped>
.login-container {
  height: 100vh;
  width: 100vw;
  display: flex;
  justify-content: center;
  align-items: center;
  background-color: #f0f2f5;
  background-image: url('https://gw.alipayobjects.com/zos/rmsportal/TVYTbAXWheQpRcWDaDMu.svg');
}

.login-card {
  width: 400px;
  border-radius: 8px;
  box-shadow: 0 4px 12px rgba(0,0,0,0.1);
}

.header-title { text-align: center; }
.header-title h2 { margin: 0; color: #303133; }
.header-title p { margin: 5px 0 0; color: #909399; font-size: 14px; }

.login-btn { width: 100%; font-weight: bold; margin-bottom: 10px; }
.footer-links { text-align: center; margin-top: 10px; }
</style>