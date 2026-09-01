import { createApp } from 'vue'
import { createPinia } from 'pinia'
import { ElButton, ElCheckbox, ElDialog, ElDrawer, ElForm, ElInput, ElLoading, ElOption, ElSelect, ElSwitch, ElTable, ElTableColumn, ElTooltip } from 'element-plus'
import 'element-plus/dist/index.css'
import 'element-plus/theme-chalk/dark/css-vars.css'
import Root from './Root.vue'
import './styles.css'

const app=createApp(Root).use(createPinia()).use(ElLoading)
for(const component of [ElButton,ElCheckbox,ElDialog,ElDrawer,ElForm,ElInput,ElOption,ElSelect,ElSwitch,ElTable,ElTableColumn,ElTooltip])app.component(component.name!,component)
app.mount('#app')
