<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { api } from './api'

type Definition = { key:string; category:string; type:string; defaultValue:string; secret:boolean; active:boolean; min:number; max:number; options:string[] }
const categories = [
  ['network','网络','配置代理、连接超时和请求重试。'], ['providers','元数据源','管理提供器、API 密钥和自定义端点。'],
  ['naming','命名','选择媒体库预设并调整目标路径模板。'], ['metadata','匹配','设置语言优先级、候选数量和匹配规则。'],
  ['postprocess','后处理','控制 NFO、封面下载和空目录清理。'], ['files','文件操作','设置默认操作、冲突策略和历史保留时间。'],
  ['scan','扫描','控制扫描范围、文件数量和识别规则。'], ['system','系统','管理时区、日志级别和数据库备份。'], ['notification','通知','配置整理完成通知（Webhook）。']
].map(([id,label,description]) => ({ id,label,description }))
const labels:Record<string,string> = {
  'network.proxyType':'代理类型','network.proxyHost':'代理地址','network.proxyPort':'代理端口','network.proxyUsername':'代理账号','network.proxyPassword':'代理密码','network.timeoutSeconds':'请求超时（秒）','network.retryCount':'重试次数',
  'naming.preset':'命名预设','naming.seriesTemplate':'剧集模板','naming.movieTemplate':'电影模板','naming.unknownTemplate':'未知文件模板','naming.titlePreference':'标题偏好','naming.unknownTitle':'未知标题兜底名',
  'metadata.languagePriority':'语言优先级','metadata.matchThreshold':'自动匹配阈值','metadata.candidateLimit':'候选数量','metadata.defaultMatchMode':'默认匹配方式',
  'postprocess.generateNfo':'生成 NFO','postprocess.downloadArtwork':'下载封面','postprocess.artworkType':'封面类型','postprocess.cleanEmptyDirectories':'清理空目录',
  'provider.omdb.dailyLimit':'OMDb 每日请求上限（0 为不限）','files.defaultOperation':'默认文件操作','files.conflictPolicy':'冲突策略','files.historyRetentionDays':'历史保留天数','scan.maxDepth':'最大扫描深度','scan.maxFiles':'最大文件数量','scan.ignorePatterns':'忽略规则','scan.minimumFileSizeMb':'最小文件大小（MB）','scan.extensions':'扩展名白名单','system.timezone':'时区','system.logLevel':'日志级别','system.databaseBackupRetention':'备份保留数量','notification.webhookUrl':'Webhook 地址'
}
const optionLabels:Record<string,string> = { NONE:'不使用代理',HTTP:'HTTP',JELLYFIN:'Jellyfin',EMBY:'Emby',PLEX:'Plex',CUSTOM:'自定义',LOCALIZED:'本地化标题',ORIGINAL:'原始标题',ENGLISH:'英文标题',MANUAL:'手动匹配',AUTO:'自动匹配',POSTER:'海报',FANART:'背景图',BOTH:'全部',MOVE:'移动',COPY:'复制',HARDLINK:'硬链接',FAIL:'遇到冲突时停止',SKIP:'跳过冲突',ERROR:'错误',WARN:'警告',INFO:'信息',DEBUG:'调试' }
const activeCategory = ref('network'), definitions = ref<Definition[]>([]), values = ref<Record<string,string>>({}), loading = ref(true), saving = ref(false), notice = ref('')
const testingProvider=ref(''), providerTests=ref<Record<string,{success:boolean;message:string}>>({})
const diagnostics=ref<any>(null), diagnosing=ref(false)
const backups=ref<any[]>([]), backingUp=ref(false), testingNotification=ref(false), notificationResult=ref('')
const currentCategory = computed(() => categories.find(c => c.id === activeCategory.value)!)
const fields = computed(() => definitions.value.filter(field => field.category === activeCategory.value))
const activeCount = computed(() => fields.value.filter(field => field.active).length)
function fieldLabel(field:Definition) {
  if (labels[field.key]) return labels[field.key]
  const match = field.key.match(/^provider\.([^.]+)\.(.+)$/); if (!match) return field.key
  const suffix:Record<string,string> = { enabled:'启用',endpoint:'自定义 API 地址',apiKey:'API Key',pin:'PIN',clientName:'客户端名称' }
  return `${match[1].toUpperCase()} · ${suffix[match[2]] || match[2]}`
}
function changeCategory(id:string) { activeCategory.value=id; notice.value=''; if(id==='system'){loadDiagnostics();loadBackups()} if(id==='notification')notificationResult.value='' }
function setBoolean(field:Definition,value:boolean) { values.value[field.key]=String(value) }
async function load() {
  try { const [schema,settings]=await Promise.all([api<Definition[]>('/api/v1/settings/schema'),api<Record<string,string>>('/api/v1/settings')]); definitions.value=schema; values.value=settings }
  catch(error){ notice.value=error instanceof Error?error.message:'设置加载失败' } finally { loading.value=false }
}
async function save() {
  saving.value=true; notice.value=''
  try { const payload=Object.fromEntries(fields.value.map(field=>[field.key,values.value[field.key]??field.defaultValue])); values.value=await api('/api/v1/settings',{method:'PUT',headers:{'Content-Type':'application/json'},body:JSON.stringify(payload)}); notice.value='设置已保存' }
  catch(error){ notice.value=error instanceof Error?error.message:'保存失败' } finally { saving.value=false }
}
async function testProvider(id:string){testingProvider.value=id;try{providerTests.value[id]=await api(`/api/v1/metadata/providers/${id}/test`,{method:'POST'})}catch(error){providerTests.value[id]={success:false,message:error instanceof Error?error.message:'连接失败'}}finally{testingProvider.value=''}}
async function loadDiagnostics(){diagnosing.value=true;try{diagnostics.value=await api('/api/v1/system/diagnostics')}catch(error){notice.value=error instanceof Error?error.message:'诊断失败'}finally{diagnosing.value=false}}
async function createBackup(){backingUp.value=true;try{await api('/api/v1/system/backup',{method:'POST'});await loadBackups();ElMessage.success('备份完成')}catch(error){ElMessage.error(error instanceof Error?error.message:'备份失败')}finally{backingUp.value=false}}
async function loadBackups(){try{backups.value=await api('/api/v1/system/backups')}catch{backups.value=[]}}
async function testNotification(){testingNotification.value=true;notificationResult.value='';try{const result=await api<{success:boolean;status:number;error?:string}>('/api/v1/system/test-notification',{method:'POST'});notificationResult.value=result.success?`发送成功（HTTP ${result.status}）`:(result.error||'发送失败')}catch(error){notificationResult.value=error instanceof Error?error.message:'发送失败'}finally{testingNotification.value=false}}
onMounted(load)
</script>

<template>
  <div class="settings-layout">
    <aside class="settings-nav surface" aria-label="设置分类">
      <button v-for="category in categories" :key="category.id" :class="{active:activeCategory===category.id}" @click="changeCategory(category.id)">{{ category.label }}</button>
    </aside>
    <section class="settings-content surface" v-loading="loading">
      <div class="settings-title">
        <div><h2>{{ currentCategory.label }}</h2><p>{{ currentCategory.description }}</p></div>
        <span v-if="fields.length && activeCount===fields.length" class="active-badge">已接入运行时</span>
        <span v-else-if="activeCount" class="mixed-badge">部分已生效</span>
        <span v-else class="pending-badge">保存后暂不影响运行</span>
      </div>
      <div v-if="activeCategory==='providers'" class="provider-tests">
        <div v-for="id in ['tmdb','tvdb','omdb','tvmaze','anidb']" :key="id"><span><i :class="providerTests[id]?.success?'ok':'idle'"></i>{{ id.toUpperCase() }}</span><small v-if="providerTests[id]">{{ providerTests[id].message }}</small><el-button size="small" :loading="testingProvider===id" @click="testProvider(id)">测试连接</el-button></div>
      </div>
      <div v-if="activeCategory==='providers'" class="provider-credits">
        <strong>数据来源与许可</strong>
        <p>This product uses the TMDB API but is not endorsed or certified by TMDB.</p>
        <p>Metadata provided by <a href="https://www.themoviedb.org" target="_blank" rel="noopener noreferrer">TMDB</a> and <a href="https://thetvdb.com" target="_blank" rel="noopener noreferrer">TheTVDB</a>. TV data from <a href="https://www.tvmaze.com" target="_blank" rel="noopener noreferrer">TVMaze</a> is available under CC BY-SA.</p>
        <small>第三方图片的权利不等同于元数据 API 许可；下载和使用前请确认适用权利。</small>
      </div>
      <div v-if="activeCategory==='system'" class="diagnostics" v-loading="diagnosing">
        <div><span :class="diagnostics?.databaseWritable?'healthy':'failed'"></span><strong>SQLite 数据库</strong><small>{{ diagnostics?.databaseWritable?'可写':'不可写' }}</small></div>
        <div><span :class="diagnostics?.ffprobeAvailable?'healthy':'failed'"></span><strong>FFprobe</strong><small>{{ diagnostics?.ffprobeAvailable?'可用':'不可用' }}</small></div>
        <div v-for="root in diagnostics?.roots||[]" :key="root.id"><span :class="root.healthy?'healthy':'failed'"></span><strong>{{ root.id }}</strong><small>{{ root.detail }}</small></div>
        <el-button size="small" @click="loadDiagnostics">重新检查</el-button>
      </div>
      <div v-if="activeCategory==='system'" class="backup-panel">
        <div class="backup-actions"><el-button size="small" type="primary" :loading="backingUp" @click="createBackup">立即备份数据库</el-button><span v-if="backups.length" class="backup-count">共 {{ backups.length }} 份备份</span></div>
        <ul v-if="backups.length" class="backup-list"><li v-for="b in backups" :key="b.file"><span>{{ b.file }}</span><small>{{ (b.size/1024).toFixed(1) }} KB · {{ new Date(b.time).toLocaleString() }}</small></li></ul>
      </div>
      <div v-if="activeCategory==='notification'" class="notification-test">
        <el-button :loading="testingNotification" @click="testNotification">发送测试通知</el-button>
        <span v-if="notificationResult" class="notification-result">{{ notificationResult }}</span>
      </div>
      <div class="settings-form">
        <label v-for="field in fields" :key="field.key" class="field" :class="{wide:field.type==='TEMPLATE'||field.key.includes('Patterns')||field.key.includes('extensions')}">
          <span>{{ fieldLabel(field) }} <i v-if="field.active" class="live-dot"></i></span>
          <el-switch v-if="field.type==='BOOLEAN'" :model-value="values[field.key]==='true'" @update:model-value="setBoolean(field, Boolean($event))" />
          <el-select v-else-if="field.type==='ENUM'" v-model="values[field.key]"><el-option v-for="option in field.options" :key="option" :label="optionLabels[option]||option" :value="option" /></el-select>
          <el-input v-else v-model="values[field.key]" :type="field.secret?'password':'text'" :show-password="field.secret" :placeholder="field.defaultValue" />
          <small v-if="field.type==='INTEGER'||field.type==='DECIMAL'">允许范围：{{ field.min }}–{{ field.max }}</small>
        </label>
      </div>
      <div class="settings-footer"><span :class="{error:notice&&notice!=='设置已保存'}">{{ notice||`当前分类共 ${fields.length} 项设置` }}</span><el-button type="primary" :loading="saving" @click="save">保存更改</el-button></div>
    </section>
  </div>
</template>
