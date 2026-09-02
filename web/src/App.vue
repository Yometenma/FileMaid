<script setup lang="ts">
import { computed, nextTick, onMounted, onUnmounted, ref, watch } from 'vue'
import { ElMessage } from 'element-plus'
import { api, type MediaFile, type Preview, type Root } from './api'
import { Clock, Document, Files, FolderOpened, Moon, MoreFilled, Operation, Refresh, Search, Setting, Sunny, Warning } from '@element-plus/icons-vue'
import SettingsPanel from './SettingsPanel.vue'
import DirectoryBrowser from './DirectoryBrowser.vue'

type Page = 'organize' | 'history' | 'logs' | 'settings'
type Theme = 'system' | 'light' | 'dark'
function pageFromHash(): Page {
  const h = window.location.hash.replace(/^#\/?/, '')
  return h === 'history' ? 'history' : h === 'logs' ? 'logs' : h === 'settings' ? 'settings' : 'organize'
}
function navigate(p: Page) {
  if (page.value !== p) page.value = p
  const target = '#/' + p
  if (window.location.hash !== target) window.history.pushState(null, '', target)
}
window.addEventListener('hashchange', () => { page.value = pageFromHash() })
const page = ref<Page>(pageFromHash())
const theme = ref<Theme>((localStorage.getItem('filemaid-theme') as Theme) || 'system')
const roots = ref<Root[]>([])
const rootId = ref('')
const relativePath = ref('')
const loading = ref(false)
const message = ref('选择服务器目录，然后开始扫描')
const previews = ref<Preview[]>([])
const companions = ref<Record<string,{path:string;kind:string}[]>>({})
const selected = ref<Preview[]>([])
const operation = ref('MOVE')
const directoryBrowserOpen = ref(false)
const confirmationToken = ref('')
const validationProblems = ref<string[]>([])
const validating = ref(false)
const executing = ref(false)
const confirmVisible = ref(false)
const executionResults = ref<{source:string;target:string;success:boolean;error?:string}[]>([])
type Task = { id:string; type:string; status:string; progress:number; message:string; error?:string; result?:any }
const runningTasks = ref<Task[]>([])
type Candidate = { provider:string;id:string;type:string;title:string;year?:number;overview?:string;artworkUrl?:string;fanartUrl?:string }
const metadataDrawer = ref(false), metadataLoading = ref(false), metadataQuery = ref(''), metadataType = ref('SERIES')
const metadataCandidates = ref<Candidate[]>([]), activeSource = ref(''), selections = ref<Record<string,any>>({})
type HistoryItem = { id:number;batchId?:string;source:string;target:string;type:string;success:boolean;error?:string;timestamp:string }
const history = ref<HistoryItem[]>([]), historyLoading = ref(false), historyQuery = ref(''), historyStatus = ref('ALL'), historyType = ref('ALL')
const generateNfo=ref(false),downloadArtwork=ref(false),artworkType=ref('POSTER'),artworkUrls=ref<Record<string,string>>({}),fanartUrls=ref<Record<string,string>>({})
const candidateLimit=ref(10),matchThreshold=ref(.72),defaultMatchMode=ref('MANUAL')
type LogEntry = { id:number;timestamp:string;level:string;logger:string;message:string;thread:string }
const logs=ref<LogEntry[]>([]), logLevel=ref('ALL'), logQuery=ref(''), logsPaused=ref(false), logsLoading=ref(false)
const logView=ref<HTMLElement|null>(null)
let lastLogId=0, logTimer:number|undefined
const filteredHistory = computed(() => history.value.filter(item => {
  const matchesQuery = !historyQuery.value || `${item.source} ${item.target}`.toLowerCase().includes(historyQuery.value.toLowerCase())
  const matchesStatus = historyStatus.value === 'ALL' || (historyStatus.value === 'SUCCESS' ? item.success : !item.success)
  const matchesType = historyType.value === 'ALL' || item.type === historyType.value
  return matchesQuery && matchesStatus && matchesType
}))
const historyGroups = computed(() => {
  const groups: { key: string; time: string; items: HistoryItem[] }[] = []
  const batches = new Map<string, { key: string; time: string; items: HistoryItem[] }>()
  for (const item of filteredHistory.value) {
    if (item.batchId) {
      const existing = batches.get(item.batchId)
      if (existing) existing.items.push(item)
      else {
        const group = { key: item.batchId, time: item.timestamp, items: [item] }
        batches.set(item.batchId, group)
        groups.push(group)
      }
      continue
    }
    const last = groups[groups.length - 1]
    if (last && !batches.has(last.key) && Math.abs(new Date(last.time).getTime() - new Date(item.timestamp).getTime()) < 5000) {
      last.items.push(item)
    } else {
      groups.push({ key: `legacy-${item.timestamp}-${item.id}`, time: item.timestamp, items: [item] })
    }
  }
  return groups
})
const activeTasks = computed(() => runningTasks.value.filter(t => !['COMPLETED','FAILED','CANCELLED'].includes(t.status)))
const themeIsDark = computed(() => theme.value === 'dark' || (theme.value === 'system' && matchMedia('(prefers-color-scheme: dark)').matches))

function applyTheme() {
  document.documentElement.classList.toggle('dark', themeIsDark.value)
  document.documentElement.dataset.theme = theme.value
  localStorage.setItem('filemaid-theme', theme.value)
}
watch(theme, applyTheme)
matchMedia('(prefers-color-scheme: dark)').addEventListener('change', applyTheme)

function cycleTheme() {
  theme.value = theme.value === 'system' ? 'light' : theme.value === 'light' ? 'dark' : 'system'
}

async function loadRoots() {
  try {
    roots.value = await api<Root[]>('/api/v1/roots')
    rootId.value = roots.value[0]?.id || ''
  } catch { message.value = '服务尚未连接，可先查看界面布局' }
}

function upsertTask(task: Task) {
  const index = runningTasks.value.findIndex(item => item.id === task.id)
  if (index >= 0) runningTasks.value[index] = task
  else runningTasks.value.push(task)
}

async function pollTask(taskId: string): Promise<any> {
  while (true) {
    const task = await api<Task>(`/api/v1/tasks/${taskId}`)
    upsertTask(task)
    if (task.status === 'COMPLETED') return task.result
    if (task.status === 'FAILED') throw new Error(task.error || '任务失败')
    if (task.status === 'CANCELLED') throw new Error('任务已取消')
    message.value = `${task.message}（${task.progress}%）`
    await new Promise(resolve => setTimeout(resolve, 600))
  }
}

async function cancelTask(id: string) {
  try {
    const task = await api<Task>(`/api/v1/tasks/${id}/cancel`, { method: 'POST' })
    upsertTask(task)
    ElMessage.warning('已请求取消任务')
  } catch (error) { ElMessage.error(error instanceof Error ? error.message : '取消失败') }
}

let taskRefreshTimer: number | undefined
function startTaskRefresh() {
  stopTaskRefresh()
  taskRefreshTimer = window.setInterval(async () => {
    try {
      const tasks = await api<Task[]>('/api/v1/tasks')
      runningTasks.value = tasks.filter(t => !['COMPLETED','FAILED','CANCELLED'].includes(t.status))
    } catch { /* 忽略轮询失败 */ }
  }, 3000)
}
function stopTaskRefresh() {
  if (taskRefreshTimer) { clearInterval(taskRefreshTimer); taskRefreshTimer = undefined }
}

async function loadLogs(reset=false) {
  if (logsPaused.value && !reset) return
  if (reset) { logs.value=[]; lastLogId=0 }
  logsLoading.value=true
  try {
    const rows=await api<LogEntry[]>(`/api/v1/logs?after=${lastLogId}&level=${logLevel.value}&query=${encodeURIComponent(logQuery.value)}&limit=500`)
    if(rows.length){logs.value.push(...rows);lastLogId=Math.max(lastLogId,...rows.map(row=>row.id));if(logs.value.length>1000)logs.value.splice(0,logs.value.length-1000);await nextTick();if(logView.value)logView.value.scrollTop=logView.value.scrollHeight}
  } catch {} finally { logsLoading.value=false }
}
function startLogRefresh(){if(logTimer)clearInterval(logTimer);loadLogs(true);logTimer=window.setInterval(()=>loadLogs(),2000)}
function stopLogRefresh(){if(logTimer){clearInterval(logTimer);logTimer=undefined}}
function clearLogView(){logs.value=[]}

async function scan() {
  if (!rootId.value) return
  loading.value = true
  message.value = '正在提交扫描任务…'
  try {
    const { taskId } = await api<{taskId:string}>(`/api/v1/roots/${encodeURIComponent(rootId.value)}/scan`, {
      method: 'POST', headers: { 'Content-Type': 'application/json' }, body: JSON.stringify({ path: relativePath.value.trim() })
    })
    const files = await pollTask(taskId) as MediaFile[]
    const groups = await api<any[]>('/api/v1/media/groups/analyze', { method:'POST', headers:{'Content-Type':'application/json'}, body: JSON.stringify({ paths: files.map(f => f.path) }) })
    const companionMap: Record<string,{path:string;kind:string}[]> = {}
    for (const group of groups) {
      for (const member of group.members) {
        if (member.companionOf) (companionMap[member.companionOf] ||= []).push({ path: member.path, kind: member.kind })
      }
    }
    companions.value = companionMap
    const videoPaths = files.filter(file => file.kind === 'VIDEO').map(file => file.path)
    previews.value = videoPaths.length ? await generatePreviews(videoPaths, []) : []
    message.value = `扫描完成，共生成 ${previews.value.length} 条整理预览`
    if (defaultMatchMode.value === 'AUTO' && previews.value.length) await autoMatch()
    persistDraft()
  } catch (error) { message.value = error instanceof Error ? error.message : '扫描失败' }
  finally { loading.value = false }
}

async function refreshPreviews() {
  previews.value=await generatePreviews(previews.value.map(row=>row.source),Object.values(selections.value))
  selected.value=[]; invalidatePlan()
}
async function generatePreviews(paths:string[], metadataSelections:any[]) {
  const { taskId } = await api<{taskId:string}>('/api/v1/rename-plans/preview-task', {
    method:'POST', headers:{'Content-Type':'application/json'},
    body:JSON.stringify({rootId:rootId.value,paths,selections:metadataSelections})
  })
  return await pollTask(taskId) as Preview[]
}
async function openMatch(row:Preview) {
  activeSource.value=row.source; metadataQuery.value=row.media.title; metadataType.value=row.media.type==='MOVIE'?'MOVIE':'SERIES'; metadataDrawer.value=true
  await searchMetadata()
}
async function searchMetadata() {
  metadataLoading.value=true
  try { metadataCandidates.value=await api(`/api/v1/metadata/search?query=${encodeURIComponent(metadataQuery.value)}&type=${metadataType.value}&limit=${candidateLimit.value}`) }
  catch(error){ElMessage.error(error instanceof Error?error.message:'元数据搜索失败')} finally {metadataLoading.value=false}
}
async function applyCandidate(candidate:Candidate) {
  selections.value[activeSource.value]={source:activeSource.value,provider:candidate.provider,id:candidate.id,type:candidate.type,title:candidate.title,year:candidate.year??null}
  if(candidate.artworkUrl)artworkUrls.value[activeSource.value]=candidate.artworkUrl
  if(candidate.fanartUrl)fanartUrls.value[activeSource.value]=candidate.fanartUrl
  metadataDrawer.value=false; await refreshPreviews(); ElMessage.success('匹配结果已应用')
}
async function autoMatch() {
  const rows=selected.value.length?selected.value:previews.value
  if(!rows.length)return
  metadataLoading.value=true
  try {
    const results=await api<any[]>('/api/v1/metadata/match',{method:'POST',headers:{'Content-Type':'application/json'},body:JSON.stringify({limit:candidateLimit.value,names:rows.map(row=>row.source)})})
    results.forEach((result,index)=>{const ranked=result.candidates?.[0];const top=ranked?.candidate;if(top&&ranked.score>=matchThreshold.value){const source=rows[index].source;selections.value[source]={source,provider:top.provider,id:top.id,type:top.type,title:top.title,year:top.year??null};if(top.artworkUrl)artworkUrls.value[source]=top.artworkUrl;if(top.fanartUrl)fanartUrls.value[source]=top.fanartUrl}})
    await refreshPreviews(); ElMessage.success('自动匹配完成')
  } catch(error){ElMessage.error(error instanceof Error?error.message:'自动匹配失败')} finally {metadataLoading.value=false}
}

function selectionChanged(rows: Preview[]) { selected.value = rows }
function invalidatePlan() { confirmationToken.value=''; validationProblems.value=[] }
const batchEditVisible = ref(false), batchFind = ref(''), batchReplace = ref('')
function openBatchEdit() {
  if (!selected.value.length) { ElMessage.warning('请先选择要编辑的文件'); return }
  batchFind.value = ''; batchReplace.value = ''
  batchEditVisible.value = true
}
function applyBatchEdit() {
  const find = batchFind.value
  if (!find) { ElMessage.warning('请输入要查找的内容'); return }
  selected.value.forEach(row => { row.target = row.target.split(find).join(batchReplace.value) })
  invalidatePlan()
  batchEditVisible.value = false
  ElMessage.success(`已应用到 ${selected.value.length} 项`)
}
function operations() {
  const ops = selected.value.map(row => ({ source:row.source, target:row.target, type:operation.value }))
  for (const row of selected.value) {
    for (const comp of companions.value[row.source] || []) {
      const videoDir = row.target.includes('/') ? row.target.slice(0, row.target.lastIndexOf('/')) : ''
      const compName = comp.path.split('/').pop() || comp.path
      ops.push({ source: comp.path, target: (videoDir ? videoDir + '/' : '') + compName, type: operation.value })
    }
  }
  return ops
}
async function validateSelected() {
  if (!selected.value.length) return
  validating.value=true; invalidatePlan()
  try {
    const postProcess = {
      generateNfo: generateNfo.value,
      downloadArtwork: downloadArtwork.value,
      artworkType: artworkType.value,
      items: selected.value.map(row => ({ source: row.source, metadata: selections.value[row.source] || null, artworkUrl: artworkUrls.value[row.source] || null, fanartUrl: fanartUrls.value[row.source] || null }))
    }
    const result=await api<{valid:boolean;problems:string[];confirmationToken?:string}>('/api/v1/rename-plans/validate',{method:'POST',headers:{'Content-Type':'application/json'},body:JSON.stringify({rootId:rootId.value,operations:operations(),postProcess})})
    validationProblems.value=result.problems
    if(result.valid&&result.confirmationToken){confirmationToken.value=result.confirmationToken;confirmVisible.value=true}
    else ElMessage.warning(result.problems[0]||'校验未通过')
  } catch(error){ElMessage.error(error instanceof Error?error.message:'校验失败')} finally {validating.value=false}
}
async function executeSelected() {
  if(!confirmationToken.value)return
  executing.value=true
  try {
    const { taskId } = await api<{taskId:string}>('/api/v1/rename-plans/execute',{method:'POST',headers:{'Content-Type':'application/json'},body:JSON.stringify({confirmationToken:confirmationToken.value})})
    confirmVisible.value=false; confirmationToken.value=''
    executionResults.value = await pollTask(taskId)
    const failed=executionResults.value.filter(item=>!item.success).length
    ElMessage[failed?'warning':'success'](failed?`执行完成，${failed} 项失败`:'整理完成')
    await scan()
  } catch(error){ElMessage.error(error instanceof Error?error.message:'执行失败');confirmationToken.value=''} finally {executing.value=false}
}
function statusOf(row: Preview) { return row.warnings.length ? '待确认' : '可执行' }
function themeTitle() { return theme.value === 'system' ? '跟随系统' : theme.value === 'light' ? '浅色模式' : '深色模式' }
async function loadHistory(){historyLoading.value=true;try{history.value=await api('/api/v1/operations')}catch(error){ElMessage.error(error instanceof Error?error.message:'历史加载失败')}finally{historyLoading.value=false}}
async function logout(){try{await api('/api/v1/auth/logout',{method:'POST'})}finally{location.reload()}}
const changePasswordVisible = ref(false), currentPassword = ref(''), newPassword = ref(''), confirmPassword = ref(''), changingPassword = ref(false)
async function submitChangePassword() {
  if (newPassword.value !== confirmPassword.value) { ElMessage.warning('两次输入的新密码不一致'); return }
  if (newPassword.value.length < 12) { ElMessage.warning('新密码至少 12 个字符'); return }
  changingPassword.value = true
  try {
    await api('/api/v1/auth/change-password', { method:'POST', headers:{'Content-Type':'application/json'}, body: JSON.stringify({ currentPassword: currentPassword.value, newPassword: newPassword.value }) })
    ElMessage.success('密码已修改')
    changePasswordVisible.value = false
    currentPassword.value = ''; newPassword.value = ''; confirmPassword.value = ''
  } catch (error) { ElMessage.error(error instanceof Error ? error.message : '修改失败') }
  finally { changingPassword.value = false }
}
async function undo(item:HistoryItem){try{const result=await api<{success:boolean;error?:string}>(`/api/v1/operations/${item.id}/undo?rootId=${encodeURIComponent(rootId.value)}`,{method:'POST'});result.success?ElMessage.success('撤销完成'):ElMessage.warning(result.error||'无法撤销');await loadHistory()}catch(error){ElMessage.error(error instanceof Error?error.message:'撤销失败')}}

const DRAFT_KEY = 'filemaid-draft'
window.addEventListener('storage', (event) => {
  if (event.key === DRAFT_KEY) ElMessage.warning('检测到其他标签页修改了整理草稿，可刷新加载最新内容')
})

function persistDraft() {
  try {
    localStorage.setItem(DRAFT_KEY, JSON.stringify({
      rootId: rootId.value,
      relativePath: relativePath.value,
      operation: operation.value,
      previews: previews.value.map(p => ({ source: p.source, target: p.target, media: p.media, warnings: p.warnings })),
      selections: selections.value,
      artworkUrls: artworkUrls.value,
      fanartUrls: fanartUrls.value,
      generateNfo: generateNfo.value,
      downloadArtwork: downloadArtwork.value,
      artworkType: artworkType.value
    }))
  } catch { /* 忽略存储失败 */ }
}

function restoreDraft() {
  try {
    const raw = localStorage.getItem(DRAFT_KEY)
    if (!raw) return
    const draft = JSON.parse(raw)
    if (draft.rootId && roots.value.some(r => r.id === draft.rootId)) rootId.value = draft.rootId
    if (draft.relativePath != null) relativePath.value = draft.relativePath
    if (draft.operation) operation.value = draft.operation
    if (Array.isArray(draft.previews) && draft.previews.length) previews.value = draft.previews
    if (draft.selections) selections.value = draft.selections
    if (draft.artworkUrls) artworkUrls.value = draft.artworkUrls
    if (draft.fanartUrls) fanartUrls.value = draft.fanartUrls
    if (draft.generateNfo != null) generateNfo.value = draft.generateNfo
    if (draft.downloadArtwork != null) downloadArtwork.value = draft.downloadArtwork
    if (draft.artworkType) artworkType.value = draft.artworkType
    if (draft.previews?.length) message.value = '已恢复上次的整理草稿'
  } catch { /* 忽略恢复失败 */ }
}

watch([previews, selections, relativePath, operation, artworkUrls, fanartUrls, generateNfo, downloadArtwork, artworkType], persistDraft, { deep: true })

onMounted(async() => {
  applyTheme()
  await loadRoots()
  try {
    const settings = await api<Record<string,string>>('/api/v1/settings')
    generateNfo.value = settings['postprocess.generateNfo']==='true'
    downloadArtwork.value = settings['postprocess.downloadArtwork']==='true'
    artworkType.value = settings['postprocess.artworkType']||'POSTER'
    operation.value = settings['files.defaultOperation']||'MOVE'
    candidateLimit.value = Number(settings['metadata.candidateLimit'])||10
    matchThreshold.value = Number(settings['metadata.matchThreshold'])||.72
    defaultMatchMode.value = settings['metadata.defaultMatchMode']||'MANUAL'
  } catch {}
  restoreDraft()
  startTaskRefresh()
})
onUnmounted(()=>{stopTaskRefresh();stopLogRefresh()})
watch(page,value=>{if(value==='history')loadHistory();if(value==='logs')startLogRefresh();else stopLogRefresh()})
watch([logLevel,logQuery],()=>{if(page.value==='logs')loadLogs(true)})
</script>

<template>
  <div class="app-shell">
    <header class="topbar">
      <button class="brand" @click="navigate('organize')" aria-label="返回整理页">
        <span class="brand-mark"><Files /></span><span>FileMaid</span>
      </button>
      <nav class="main-nav" aria-label="主导航">
        <button :class="{ active: page === 'organize' }" @click="navigate('organize')"><Operation />整理</button>
        <button :class="{ active: page === 'history' }" @click="navigate('history')"><Clock />历史</button>
        <button :class="{ active: page === 'logs' }" @click="navigate('logs')"><Document />日志</button>
        <button :class="{ active: page === 'settings' }" @click="navigate('settings')"><Setting />设置</button>
      </nav>
      <div class="top-actions">
        <span class="service-status"><i></i>服务正常</span>
        <el-tooltip :content="themeTitle()"><button class="icon-button" @click="cycleTheme"><Moon v-if="themeIsDark" /><Sunny v-else /></button></el-tooltip><el-button text @click="changePasswordVisible=true">修改密码</el-button><el-button text @click="logout">退出</el-button>
      </div>
    </header>

    <main v-if="page === 'organize'" class="page organize-page">
      <section class="page-heading">
        <div><p class="eyebrow">媒体整理</p><h1>整理工作区</h1><p>检查源文件与目标路径，确认无误后再执行。</p></div>
        <div class="heading-state"><span>草稿</span><strong>{{ previews.length }} 项</strong></div>
      </section>

      <section class="control-panel surface">
        <label class="field compact"><span>服务器目录</span><el-select v-model="rootId" placeholder="选择目录"><el-option v-for="root in roots" :key="root.id" :label="`${root.id} · ${root.writable ? '可写' : '只读'}`" :value="root.id" /></el-select></label>
        <label class="field path-field"><span>媒体目录</span><el-input v-model="relativePath" placeholder="选择或输入服务器目录"><template #prefix><FolderOpened /></template><template #append><el-button @click="directoryBrowserOpen = true">浏览</el-button></template></el-input></label>
        <label class="field compact"><span>文件操作</span><el-select v-model="operation" @change="invalidatePlan"><el-option label="移动" value="MOVE"/><el-option label="复制" value="COPY"/><el-option label="硬链接" value="HARDLINK"/></el-select></label>
        <el-button type="primary" :loading="loading" @click="scan"><Search />扫描目录</el-button>
      </section>

      <section v-if="activeTasks.length" class="task-strip surface">
        <div v-for="task in activeTasks" :key="task.id" class="task-item">
          <span class="task-type">{{ task.type }}</span>
          <el-progress :percentage="task.progress" :status="task.status==='FAILED'?'exception':undefined" :stroke-width="6" style="flex:1;min-width:120px" />
          <span class="task-message">{{ task.message }}</span>
          <el-button v-if="task.status==='PENDING'||task.status==='RUNNING'" size="small" @click="cancelTask(task.id)">取消</el-button>
        </div>
      </section>

      <section class="status-strip">
        <span><Refresh :class="{ spin: loading }" />{{ message }}</span>
        <div><span><i class="legend ready"></i>{{ previews.filter(p => !p.warnings.length).length }} 可执行</span><span><i class="legend warn"></i>{{ previews.filter(p => p.warnings.length).length }} 待确认</span></div>
      </section>

      <section class="table-card surface">
        <div class="table-toolbar">
          <div><h2>路径对照</h2><span>源文件和整理后的目标位置</span></div>
          <div class="toolbar-actions"><el-button :loading="metadataLoading" :disabled="!previews.length" @click="autoMatch">自动匹配</el-button><el-button :disabled="!selected.length" @click="openBatchEdit">批量编辑</el-button><el-button text><MoreFilled /></el-button></div>
        </div>
        <el-table :data="previews" class="compare-table" height="calc(100vh - 390px)" empty-text="扫描目录后，这里会显示源文件与新文件名" @selection-change="selectionChanged">
          <el-table-column type="selection" width="48" />
          <el-table-column type="expand" width="40">
            <template #default="{ row }">
              <div v-if="companions[row.source]?.length" class="companion-list">
                <div v-for="comp in companions[row.source]" :key="comp.path" class="companion-item">
                  <span class="companion-kind">{{ comp.kind }}</span>
                  <span>{{ comp.path.split('/').pop() }}</span>
                  <small>跟随视频一起{{ operation==='MOVE'?'移动':operation==='COPY'?'复制':'链接' }}</small>
                </div>
              </div>
            </template>
          </el-table-column>
          <el-table-column label="源文件" min-width="310"><template #default="{ row }"><button class="path-cell path-button" @click="openMatch(row)"><span class="file-icon"><Files /></span><div><strong>{{ row.source.split('/').pop() }}</strong><small>{{ row.source.includes('/') ? row.source.slice(0, row.source.lastIndexOf('/')) : '根目录' }}</small></div></button></template></el-table-column>
          <el-table-column width="54" align="center"><template #default><span class="route-arrow">→</span></template></el-table-column>
          <el-table-column label="新文件名" min-width="370"><template #default="{ row }"><div class="target-cell"><el-input v-model="row.target" @input="invalidatePlan"/><small>{{ row.media.title }}<template v-if="row.media.season != null"> · 第 {{ row.media.season }} 季</template></small></div></template></el-table-column>
          <el-table-column label="状态" width="120"><template #default="{ row }"><span class="row-status" :class="row.warnings.length ? 'warning' : 'success'"><Warning v-if="row.warnings.length" />{{ statusOf(row) }}</span></template></el-table-column>
        </el-table>
      </section>

      <footer class="action-bar surface">
        <div><strong>已选择 {{ selected.length }} 项</strong><span>修改目标路径后需要重新校验</span></div>
        <div class="postprocess-options"><el-checkbox v-model="generateNfo" @change="invalidatePlan">生成 NFO</el-checkbox><el-checkbox v-model="downloadArtwork" @change="invalidatePlan">下载封面</el-checkbox><el-select v-if="downloadArtwork" v-model="artworkType" size="small" @change="invalidatePlan"><el-option label="海报" value="POSTER"/><el-option label="背景图" value="FANART"/><el-option label="全部" value="BOTH"/></el-select></div>
        <div><el-button :loading="validating" :disabled="!selected.length" @click="validateSelected">干跑校验</el-button><el-button type="primary" :disabled="!confirmationToken" @click="confirmVisible=true">执行整理</el-button></div>
      </footer>
      <DirectoryBrowser v-model="directoryBrowserOpen" :root-id="rootId" :initial-path="relativePath" @select="relativePath = $event" />
      <el-dialog v-model="confirmVisible" width="560px" title="确认执行整理">
        <div class="confirm-summary"><strong>即将{{ operation==='MOVE'?'移动':operation==='COPY'?'复制':'创建硬链接' }} {{ selected.length }} 个文件</strong><p>执行前服务器会再次检查源文件、目标冲突和路径安全，默认不会覆盖已有文件。</p><ul><li v-for="row in selected.slice(0,5)" :key="row.source">{{ row.source }} → {{ row.target }}</li></ul><small v-if="selected.length>5">另外还有 {{ selected.length-5 }} 项</small></div>
        <template #footer><el-button @click="confirmVisible=false">取消</el-button><el-button type="primary" :loading="executing" @click="executeSelected">确认执行</el-button></template>
      </el-dialog>
      <el-dialog v-model="changePasswordVisible" width="420px" title="修改密码">
        <div class="password-form">
          <el-input v-model="currentPassword" type="password" show-password placeholder="当前密码" />
          <el-input v-model="newPassword" type="password" show-password placeholder="新密码（至少 12 位）" />
          <el-input v-model="confirmPassword" type="password" show-password placeholder="确认新密码" />
        </div>
        <template #footer><el-button @click="changePasswordVisible=false">取消</el-button><el-button type="primary" :loading="changingPassword" @click="submitChangePassword">确认修改</el-button></template>
      </el-dialog>
      <el-dialog v-model="batchEditVisible" width="480px" title="批量编辑目标路径">
        <div class="batch-edit-form">
          <p>对已选 {{ selected.length }} 项的目标路径做查找替换：</p>
          <el-input v-model="batchFind" placeholder="查找内容（如：Episode）" />
          <el-input v-model="batchReplace" placeholder="替换为（留空则删除）" />
        </div>
        <template #footer><el-button @click="batchEditVisible=false">取消</el-button><el-button type="primary" @click="applyBatchEdit">应用</el-button></template>
      </el-dialog>
      <el-drawer v-model="metadataDrawer" size="460px" title="选择元数据">
        <div class="metadata-search"><el-input v-model="metadataQuery" placeholder="输入标题" @keyup.enter="searchMetadata"><template #prefix><Search/></template></el-input><el-button type="primary" :loading="metadataLoading" @click="searchMetadata">搜索</el-button></div>
        <p class="drawer-source">应用到：{{ activeSource }}</p>
        <div class="candidate-list" v-loading="metadataLoading">
          <button v-for="candidate in metadataCandidates" :key="`${candidate.provider}:${candidate.id}`" class="candidate-row" @click="applyCandidate(candidate)"><span class="candidate-provider">{{ candidate.provider.toUpperCase() }}</span><span><strong>{{ candidate.title }}</strong><small>{{ candidate.year||'年份未知' }} · {{ candidate.type }}</small><p>{{ candidate.overview||'暂无简介' }}</p></span></button>
          <div v-if="!metadataLoading&&!metadataCandidates.length" class="directory-empty">没有找到候选结果</div>
        </div>
      </el-drawer>
    </main>

    <main v-else-if="page === 'history'" class="page secondary-page">
      <section class="page-heading"><div><p class="eyebrow">操作记录</p><h1>整理历史</h1><p>按批次查看结果，并在目标路径仍然安全时撤销。</p></div></section>
      <section class="history-card surface">
        <div class="table-toolbar"><div><h2>操作记录</h2><span>共 {{ historyGroups.length }} 批 · {{ filteredHistory.length }} 条</span></div><div class="history-filters"><el-select v-model="historyStatus" style="width:110px"><el-option label="全部结果" value="ALL"/><el-option label="成功" value="SUCCESS"/><el-option label="失败" value="FAILED"/></el-select><el-select v-model="historyType" style="width:130px"><el-option label="全部操作" value="ALL"/><el-option label="移动" value="MOVE"/><el-option label="复制" value="COPY"/><el-option label="硬链接" value="HARDLINK"/><el-option label="NFO" value="NFO"/><el-option label="封面" value="ARTWORK"/></el-select><el-input v-model="historyQuery" clearable placeholder="搜索源路径或目标路径" style="width:260px"><template #prefix><Search/></template></el-input></div></div>
        <div class="history-batches" v-loading="historyLoading">
          <div v-if="!historyGroups.length" class="directory-empty">还没有整理记录</div>
          <el-collapse v-else>
            <el-collapse-item v-for="group in historyGroups" :key="group.key">
              <template #title>
                <div class="batch-title">
                  <strong>{{ new Date(group.time).toLocaleString() }}</strong>
                  <span class="row-status" :class="group.items.every(i=>i.success)?'success':'warning'">{{ group.items.filter(i=>i.success).length }}/{{ group.items.length }} 成功</span>
                  <small>{{ group.items.length }} 项 · {{ group.items.map(i=>i.type).filter((v,i,a)=>a.indexOf(v)===i).join('、') }}</small>
                </div>
              </template>
              <div class="batch-items">
                <div v-for="item in group.items" :key="item.id" class="batch-item">
                  <span class="history-path">{{ item.source }}</span>
                  <span class="route-arrow">→</span>
                  <span class="history-path">{{ item.target }}</span>
                  <span class="row-status" :class="item.success?'success':'warning'">{{ item.type }} · {{ item.success?'成功':'失败' }}</span>
                  <el-button text type="primary" size="small" :disabled="!item.success" @click="undo(item)">撤销</el-button>
                </div>
              </div>
            </el-collapse-item>
          </el-collapse>
        </div>
      </section>
    </main>

    <main v-else-if="page === 'logs'" class="page secondary-page">
      <section class="page-heading"><div><p class="eyebrow">运行记录</p><h1>实时日志</h1><p>查看扫描、匹配、整理和系统运行信息。</p></div><div class="heading-state"><span>{{ logsPaused?'已暂停':'自动刷新' }}</span><strong>{{ logs.length }} 条</strong></div></section>
      <section class="log-card surface">
        <div class="table-toolbar"><div><h2>服务日志</h2><span>内存最多保留 2000 条，页面显示最近 1000 条</span></div><div class="history-filters"><el-select v-model="logLevel" style="width:120px"><el-option label="全部级别" value="ALL"/><el-option label="信息" value="INFO"/><el-option label="警告" value="WARN"/><el-option label="错误" value="ERROR"/></el-select><el-input v-model="logQuery" clearable placeholder="搜索日志" style="width:220px"><template #prefix><Search/></template></el-input><el-button @click="logsPaused=!logsPaused">{{ logsPaused?'继续':'暂停' }}</el-button><el-button @click="clearLogView">清空视图</el-button></div></div>
        <div ref="logView" class="log-view" v-loading="logsLoading&&!logs.length">
          <div v-if="!logs.length" class="directory-empty">暂无符合条件的日志</div>
          <div v-for="entry in logs" :key="entry.id" class="log-line"><time>{{ new Date(entry.timestamp).toLocaleTimeString() }}</time><span class="log-level" :class="entry.level.toLowerCase()">{{ entry.level }}</span><span class="log-logger">{{ entry.logger }}</span><code>{{ entry.message }}</code></div>
        </div>
      </section>
    </main>

    <main v-else class="page settings-page">
      <section class="page-heading"><div><p class="eyebrow">偏好与连接</p><h1>设置</h1><p>管理网络、元数据、命名和文件操作规则。</p></div></section>
      <SettingsPanel />
    </main>
  </div>
</template>
