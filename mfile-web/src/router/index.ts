import { createRouter, createWebHistory } from 'vue-router'

import Login from '../views/Login.vue'
import Home from '../views/Home.vue'
import ShareDownload from '../views/ShareDownload.vue'
import Admin from "../views/Admin.vue";
const routes = [
    {
        path: '/login',
        name: 'Login',
        component: Login
    },
    {
        path: '/',
        name: 'Home',
        component: Home
    },
    {   path: '/s/:uuid',
        name: 'Share',
        component: ShareDownload
    },
    {   path: '/admin',
        name: 'Admin',
        component: Admin },
]

const router = createRouter({
    // 使用 HTML5 History 模式 (没有 # 号)
    history: createWebHistory(),
    routes
})

// 没有 Token 严禁访问主页
router.beforeEach((to, from, next) => {
    const token = localStorage.getItem('satoken')

    if (to.path === '/login' || to.path.startsWith('/s/')) {
        next()
    } else if (!token) {
        next('/login')
    } else {
        next()
    }
})

export default router