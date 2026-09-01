<script setup lang="ts">
import { computed, onMounted, ref, watch } from 'vue'
import { ElMessage } from 'element-plus'
import { api, type MediaFile, type Preview, type Root } from './api'
import { Clock, Files, FolderOpened, Moon, MoreFilled, Operation, Refresh, Search, Setting, Sunny, Warning } from '@element-plus/icons-vue'
import SettingsPanel from './SettingsPanel.vue'
import DirectoryBrowser from './DirectoryBrowser.vue'

type Page = 'organize' | 'history' | 'settings'
type Theme = 'system' | 'light' | 'dark'
const page = ref<Page>('organize')
const theme = ref<Theme>((localStorage.getItem('filemaid-theme') as Theme) || 'system')
const roots = ref<Root[]>([])
const rootId = ref('')
const relativePath = ref('')
const loading = ref(false)
const message = ref('选择服务器目录，然后开始扫描')
const previews = ref<Preview[]>([])
const selected = ref<Preview[]>([])
const operation = ref('MOVE')
const directoryBrowserOpen = ref(false)
const confirmationToken = ref('')
const validationProblems = ref<string[]>([])
const validating = ref(false)
const executing = ref(false)
const confirmVisible = ref(false)
const executionResults = ref<{source:string;target:string;success:boolean;error?:string}[]>([])
type Candidate = { provider:string;id:string;type:string;title:string;year?:number;overview?:string;artworkUrl?:string }
const metadataDrawer = ref(false), metadataLoading = ref(false), metadataQuery = ref(''), metadataType = ref('SERIES')
const metadataCandidates = ref<Candidate[]>([]), activeSource = ref(''), selections = ref<Record<string,any>>({})
type HistoryItem = { id:number;source:string;target:string;type:string;success:boolean;error?:string;timestamp:string }
const history = ref<HistoryItem[]>([]), historyLoading = ref(false), historyQuery = ref('')
const generateNfo=ref(false),downloadArtwork=ref(false),artworkType=ref('POSTER'),artworkUrls=ref<Record<string,string>>({})
const candidateLimit=ref(10),matchThreshold=ref(.72)
const filteredHistory = computed(() => history.value.filter(item => !historyQuery.value || `${item.source} ${item.target}`.toLowerCase().includes(historyQuery.value.toLowerCase())))
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

async function scan() {
  if (!rootId.value) return
  loading.value = true
  message.value = '正在扫描并生成目标路径…'
  try {
    const path = encodeURIComponent(relativePath.value.trim())
    const files = await api<MediaFile[]>(`/api/v1/roots/${encodeURIComponent(rootId.value)}/scan?path=${path}`)
    const media = files.filter(file => ['VIDEO', 'SUBTITLE'].includes(file.kind))
    previews.value = media.length ? await api<Preview[]>('/api/v1/rename-plans/preview', {
      method: 'POST', headers: { 'Content-Type': 'application/json' }, body: JSON.stringify({ paths: media.map(file => file.path) })
    }) : []
    message.value = `扫描完成，共生成 ${previews.value.length} 条整理预览`
  } catch (error) { message.value = error instanceof Error ? error.message : '扫描失败' }
  finally { loading.value = false }
}

async function refreshPreviews() {
  previews.value=await api('/api/v1/rename-plans/preview',{method:'POST',headers:{'Content-Type':'application/json'},body:JSON.stringify({rootId:rootId.value,paths:previews.value.map(row=>row.source),selections:Object.values(selections.value)})})
  selected.value=[]; invalidatePlan()
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
  metadataDrawer.value=false; await refreshPreviews(); ElMessage.success('匹配结果已应用')
}
async function autoMatch() {
  const rows=selected.value.length?selected.value:previews.value
  if(!rows.length)return
  metadataLoading.value=true
  try {
    const results=await api<any[]>('/api/v1/metadata/match',{method:'POST',headers:{'Content-Type':'application/json'},body:JSON.stringify({limit:candidateLimit.value,names:rows.map(row=>row.source)})})
    results.forEach((result,index)=>{const ranked=result.candidates?.[0];const top=ranked?.candidate;if(top&&ranked.score>=matchThreshold.value){const source=rows[index].source;selections.value[source]={source,provider:top.provider,id:top.id,type:top.type,title:top.title,year:top.year??null};if(top.artworkUrl)artworkUrls.value[source]=top.artworkUrl}})
    await refreshPreviews(); ElMessage.success('自动匹配完成')
  } catch(error){ElMessage.error(error instanceof Error?error.message:'自动匹配失败')} finally {metadataLoading.value=false}
}

function selectionChanged(rows: Preview[]) { selected.value = rows }
function invalidatePlan() { confirmationToken.value=''; validationProblems.value=[] }
function operations() { return selected.value.map(row => ({ source:row.source, target:row.target, type:operation.value })) }
async function validateSelected() {
  if (!selected.value.length) return
  validating.value=true; invalidatePlan()
  try {
    const postProcess = {
      generateNfo: generateNfo.value,
      downloadArtwork: downloadArtwork.value,
      artworkType: artworkType.value,
      items: selected.value.map(row => ({ source: row.source, metadata: selections.value[row.source] || null, artworkUrl: artworkUrls.value[row.source] || null }))
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
    executionResults.value=await api('/api/v1/rename-plans/execute',{method:'POST',headers:{'Content-Type':'application/json'},body:JSON.stringify({confirmationToken:confirmationToken.value})})
    confirmVisible.value=false; confirmationToken.value=''
    const failed=executionResults.value.filter(item=>!item.success).length
    ElMessage[failed?'warning':'success'](failed?`执行完成，${failed} 项失败`:'整理完成')
    await scan()
  } catch(error){ElMessage.error(error instanceof Error?error.message:'执行失败');confirmationToken.value=''} finally {executing.value=false}
}
function statusOf(row: Preview) { return row.warnings.length ? '待确认' : '可执行' }
function themeTitle() { return theme.value === 'system' ? '跟随系统' : theme.value === 'light' ? '浅色模式' : '深色模式' }
async function loadHistory(){historyLoading.value=true;try{history.value=await api('/api/v1/operations')}catch(error){ElMessage.error(error instanceof Error?error.message:'历史加载失败')}finally{historyLoading.value=false}}
async function logout(){try{await api('/api/v1/auth/logout',{method:'POST'})}finally{location.reload()}}
async function undo(item:HistoryItem){try{const result=await api<{success:boolean;error?:string}>(`/api/v1/operations/${item.id}/undo?rootId=${encodeURIComponent(rootId.value)}`,{method:'POST'});result.success?ElMessage.success('撤销完成'):ElMessage.warning(result.error||'无法撤销');await loadHistory()}catch(error){ElMessage.error(error instanceof Error?error.message:'撤销失败')}}

onMounted(async() => { applyTheme(); loadRoots();try{const settings=await api<Record<string,string>>('/api/v1/settings');generateNfo.value=settings['postprocess.generateNfo']==='true';downloadArtwork.value=settings['postprocess.downloadArtwork']==='true';artworkType.value=settings['postprocess.artworkType']||'POSTER';operation.value=settings['files.defaultOperation']||'MOVE';candidateLimit.value=Number(settings['metadata.candidateLimit'])||10;matchThreshold.value=Number(settings['metadata.matchThreshold'])||.72}catch{} })
watch(page,value=>{if(value==='history')loadHistory()})
</script>

<template>
  <div class="app-shell">
    <header class="topbar">
      <button class="brand" @click="page = 'organize'" aria-label="返回整理页">
        <span class="brand-mark"><Files /></span><span>FileMaid</span>
      </button>
      <nav class="main-nav" aria-label="主导航">
        <button :class="{ active: page === 'organize' }" @click="page = 'organize'"><Operation />整理</button>
        <button :class="{ active: page === 'history' }" @click="page = 'history'"><Clock />历史</button>
        <button :class="{ active: page === 'settings' }" @click="page = 'settings'"><Setting />设置</button>
      </nav>
      <div class="top-actions">
        <span class="service-status"><i></i>服务正常</span>
        <el-tooltip :content="themeTitle()"><button class="icon-button" @click="cycleTheme"><Moon v-if="themeIsDark" /><Sunny v-else /></button></el-tooltip><el-button text @click="logout">退出</el-button>
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

      <section class="status-strip">
        <span><Refresh :class="{ spin: loading }" />{{ message }}</span>
        <div><span><i class="legend ready"></i>{{ previews.filter(p => !p.warnings.length).length }} 可执行</span><span><i class="legend warn"></i>{{ previews.filter(p => p.warnings.length).length }} 待确认</span></div>
      </section>

      <section class="table-card surface">
        <div class="table-toolbar">
          <div><h2>路径对照</h2><span>源文件和整理后的目标位置</span></div>
          <div class="toolbar-actions"><el-button :loading="metadataLoading" :disabled="!previews.length" @click="autoMatch">自动匹配</el-button><el-button disabled>批量编辑</el-button><el-button text><MoreFilled /></el-button></div>
        </div>
        <el-table :data="previews" class="compare-table" height="calc(100vh - 390px)" empty-text="扫描目录后，这里会显示源文件与新文件名" @selection-change="selectionChanged">
          <el-table-column type="selection" width="48" />
          <el-table-column label="源文件" min-width="310"><template #default="{ row }"><button class="path-cell path-button" @click="openMatch(row)"><span class="file-icon"><Files /></span><div><strong>{{ row.source.split('/').pop() }}</strong><small>{{ row.source.includes('/') ? row.source.slice(0, row.source.lastIndexOf('/')) : '根目录' }}</small></div></button></template></el-table-column>
          <el-table-column width="54" align="center"><template #default><span class="route-arrow">→</span></template></el-table-column>
          <el-table-column label="新文件名" min-width="370"><template #default="{ row }"><div class="target-cell"><el-input v-model="row.target" @input="invalidatePlan"/><small>{{ row.media.title }}<template v-if="row.media.season != null"> · 第 {{ row.media.season }} 季</template></small></div></template></el-table-column>
          <el-table-column label="状态" width="120"><template #default="{ row }"><span class="row-status" :class="row.warnings.length ? 'warning' : 'success'"><Warning v-if="row.warnings.length" />{{ statusOf(row) }}</span></template></el-table-column>
        </el-table>
      </section>

      <footer class="action-bar surface">
        <div><strong>已选择 {{ selected.length }} 项</strong><span>修改目标路径后需要重新校验</span></div>
        <div class="postprocess-options"><el-checkbox v-model="generateNfo" @change="invalidatePlan">生成 NFO</el-checkbox><el-checkbox v-model="downloadArtwork" @change="invalidatePlan">下载封面</el-checkbox><el-select v-if="downloadArtwork" v-model="artworkType" size="small" @change="invalidatePlan"><el-option label="海报" value="POSTER"/><el-option label="背景图" value="FANART"/></el-select></div>
        <div><el-button :loading="validating" :disabled="!selected.length" @click="validateSelected">干跑校验</el-button><el-button type="primary" :disabled="!confirmationToken" @click="confirmVisible=true">执行整理</el-button></div>
      </footer>
      <DirectoryBrowser v-model="directoryBrowserOpen" :root-id="rootId" :initial-path="relativePath" @select="relativePath = $event" />
      <el-dialog v-model="confirmVisible" width="560px" title="确认执行整理">
        <div class="confirm-summary"><strong>即将{{ operation==='MOVE'?'移动':operation==='COPY'?'复制':'创建硬链接' }} {{ selected.length }} 个文件</strong><p>执行前服务器会再次检查源文件、目标冲突和路径安全，默认不会覆盖已有文件。</p><ul><li v-for="row in selected.slice(0,5)" :key="row.source">{{ row.source }} → {{ row.target }}</li></ul><small v-if="selected.length>5">另外还有 {{ selected.length-5 }} 项</small></div>
        <template #footer><el-button @click="confirmVisible=false">取消</el-button><el-button type="primary" :loading="executing" @click="executeSelected">确认执行</el-button></template>
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
        <div class="table-toolbar"><div><h2>操作记录</h2><span>共 {{ filteredHistory.length }} 条</span></div><el-input v-model="historyQuery" clearable placeholder="搜索源路径或目标路径" style="width:300px"><template #prefix><Search/></template></el-input></div>
        <el-table :data="filteredHistory" v-loading="historyLoading" empty-text="还没有整理记录">
          <el-table-column label="时间" width="180"><template #default="{row}">{{ new Date(row.timestamp).toLocaleString() }}</template></el-table-column>
          <el-table-column prop="type" label="操作" width="100" />
          <el-table-column label="源文件" min-width="250"><template #default="{row}"><span class="history-path">{{ row.source }}</span></template></el-table-column>
          <el-table-column label="目标文件" min-width="250"><template #default="{row}"><span class="history-path">{{ row.target }}</span></template></el-table-column>
          <el-table-column label="结果" width="100"><template #default="{row}"><span class="row-status" :class="row.success?'success':'warning'">{{ row.success?'成功':'失败' }}</span></template></el-table-column>
          <el-table-column label="操作" width="90"><template #default="{row}"><el-button text type="primary" :disabled="!row.success" @click="undo(row)">撤销</el-button></template></el-table-column>
        </el-table>
      </section>
    </main>

    <main v-else class="page settings-page">
      <section class="page-heading"><div><p class="eyebrow">偏好与连接</p><h1>设置</h1><p>管理网络、元数据、命名和文件操作规则。</p></div></section>
      <SettingsPanel />
    </main>
  </div>
</template>
