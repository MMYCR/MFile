import axios from 'axios'
import { ElMessage } from 'element-plus'

// 创建 axios 实例
const service = axios.create({
    baseURL: '/api', // 配合 vite.config.ts 的代理
    timeout: 50000 // 请求超时时间
})

// 1. 请求拦截器：发送请求前，自动带上 Token
service.interceptors.request.use(
    (config) => {
        const token = localStorage.getItem('satoken')
        if (token) {
            config.headers['satoken'] = token
        }
        return config
    },
    (error) => {
        return Promise.reject(error)
    }
)

// 2. 响应拦截器：收到响应后，统一处理错误
service.interceptors.response.use(
    (response) => {
        const res = response.data
        // 假设后端 AjaxJson 成功状态码是 200
        if (res.code !== 200) {
            ElMessage.error(res.message || '系统错误')

            // 401 代表未登录或 Token 过期
            if (res.code === 401) {
                localStorage.removeItem('satoken')
                // 这里暂时用原生跳转，后期有了路由用 router.push('/login')
                window.location.href = '/login'
            }
            return Promise.reject(new Error(res.message || 'Error'))
        } else {
            return res.data
        }
    },
    (error) => {
        // 处理 HTTP 状态码错误 (如 404, 500)
        console.error('err' + error)
        ElMessage.error(error.message || '网络请求失败')
        return Promise.reject(error)
    }
)

export default service