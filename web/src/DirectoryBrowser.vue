<script setup lang="ts">
import { computed, ref, watch } from 'vue'
import { api } from './api'
import { ArrowRight, Folder, Search } from '@element-plus/icons-vue'

const props = defineProps<{ modelValue:boolean; rootId:string; initialPath:string }>()
const emit = defineEmits<{ 'update:modelValue':[value:boolean]; select:[path:string] }>()
type Listing = { current:string; parent:string|null; entries:{name:string;path:string}[] }
const listing = ref<Listing>({current:'',parent:null,entries:[]}), query=ref(''), loading=ref(false), error=ref('')
const crumbs = computed(() => listing.value.current ? listing.value.current.split('/').map((name,index,all)=>({name,path:all.slice(0,index+1).join('/')})) : [])
async function browse(path='', search='') {
  if (!props.rootId) return
  loading.value=true; error.value=''
  try { listing.value=await api(`/api/v1/roots/${encodeURIComponent(props.rootId)}/directories?path=${encodeURIComponent(path)}&query=${encodeURIComponent(search)}`) }
  catch(failure){ error.value=failure instanceof Error?failure.message:'目录读取失败' } finally { loading.value=false }
}
function close(){emit('update:modelValue',false)}
function choose(){emit('select',listing.value.current);close()}
function search(){browse(listing.value.current,query.value)}
watch(()=>props.modelValue,open=>{if(open){query.value='';browse(props.initialPath)}})
</script>

<template>
  <el-dialog :model-value="modelValue" width="720px" class="directory-dialog" title="选择服务器目录" @close="close">
    <div class="browser-top">
      <div class="breadcrumbs"><button @click="browse('')">{{ rootId }}</button><template v-for="crumb in crumbs" :key="crumb.path"><ArrowRight/><button @click="browse(crumb.path)">{{ crumb.name }}</button></template></div>
      <el-input v-model="query" clearable placeholder="在当前目录下搜索文件夹" @keyup.enter="search" @clear="browse(listing.current)"><template #prefix><Search/></template></el-input>
    </div>
    <div class="directory-list" v-loading="loading">
      <button v-if="listing.parent !== null && !query" class="directory-row muted" @dblclick="browse(listing.parent)"><Folder/><span><strong>..</strong><small>返回上一级</small></span></button>
      <button v-for="entry in listing.entries" :key="entry.path" class="directory-row" @dblclick="query='';browse(entry.path)"><Folder/><span><strong>{{ entry.name }}</strong><small>{{ entry.path }}</small></span><span class="open-hint">双击打开</span></button>
      <div v-if="!loading && !listing.entries.length && !error" class="directory-empty">{{ query ? '没有找到匹配的文件夹' : '这个目录中没有子文件夹' }}</div>
      <div v-if="error" class="directory-empty error">{{ error }}</div>
    </div>
    <div class="selected-directory"><span>当前选择</span><strong>{{ rootId }}<template v-if="listing.current"> / {{ listing.current }}</template></strong></div>
    <template #footer><el-button @click="close">取消</el-button><el-button type="primary" @click="choose">选择此目录</el-button></template>
  </el-dialog>
</template>
