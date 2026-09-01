<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { api } from './api'
import App from './App.vue'
import { Files, Lock } from '@element-plus/icons-vue'

type Status={enabled:boolean;configured:boolean;authenticated:boolean;username?:string}
const status=ref<Status|null>(null),username=ref('admin'),password=ref(''),confirmPassword=ref(''),loading=ref(false),error=ref('')
async function refresh(){try{status.value=await api('/api/v1/auth/status')}catch(failure){error.value=failure instanceof Error?failure.message:'无法连接服务'}}
async function submit(){error.value='';if(!status.value)return;if(!status.value.configured&&password.value!==confirmPassword.value){error.value='两次输入的密码不一致';return}loading.value=true;try{if(!status.value.configured)await api('/api/v1/auth/setup',{method:'POST',headers:{'Content-Type':'application/json'},body:JSON.stringify({username:username.value,password:password.value})});const body=new URLSearchParams({username:username.value,password:password.value});await api('/api/v1/auth/login',{method:'POST',headers:{'Content-Type':'application/x-www-form-urlencoded'},body});password.value='';confirmPassword.value='';await refresh()}catch(failure){error.value=failure instanceof Error?failure.message:'登录失败'}finally{loading.value=false}}
onMounted(refresh)
</script>
<template>
  <App v-if="status && (!status.enabled || status.authenticated)" />
  <div v-else class="auth-screen">
    <section class="auth-card surface" v-if="status">
      <span class="auth-logo"><Files/></span><p class="eyebrow">FileMaid</p><h1>{{ status.configured?'登录':'首次设置' }}</h1><p>{{ status.configured?'登录后继续管理你的媒体文件。':'创建唯一的管理员账号。密码至少需要 12 个字符。' }}</p>
      <el-form @submit.prevent="submit"><label class="field"><span>用户名</span><el-input v-model="username" autocomplete="username"/></label><label class="field"><span>密码</span><el-input v-model="password" type="password" show-password autocomplete="current-password" @keyup.enter="submit"/></label><label v-if="!status.configured" class="field"><span>确认密码</span><el-input v-model="confirmPassword" type="password" show-password autocomplete="new-password" @keyup.enter="submit"/></label><div v-if="error" class="auth-error">{{ error }}</div><el-button type="primary" native-type="submit" :loading="loading"><Lock/>{{ status.configured?'登录':'创建账号并登录' }}</el-button></el-form>
    </section>
    <section v-else class="auth-card surface">正在连接 FileMaid…</section>
  </div>
</template>
