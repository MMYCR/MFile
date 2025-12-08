import { createApp } from 'vue'
import './style.css'
import App from './App.vue'

// 1. 引入 Element Plus 及其样式
import ElementPlus from 'element-plus'
import 'element-plus/dist/index.css'

// 2. 引入图标库
import * as ElementPlusIconsVue from '@element-plus/icons-vue'

// 3. 引入 Pinia 和 Router
import { createPinia } from 'pinia'
import router from './router' // 如果还没有 router 文件，先注释掉这行

const app = createApp(App)

// 注册所有图标
for (const [key, component] of Object.entries(ElementPlusIconsVue)) {
    app.component(key, component)
}

app.use(ElementPlus)
app.use(createPinia())
 app.use(router)

app.mount('#app')