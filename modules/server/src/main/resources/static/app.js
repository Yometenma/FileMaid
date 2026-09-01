const rootSelect = document.querySelector('#root');
const pathInput = document.querySelector('#path');
const scanButton = document.querySelector('#scan');
const message = document.querySelector('#message');
const results = document.querySelector('#results');
const metadataQuery = document.querySelector('#metadataQuery');
const metadataType = document.querySelector('#metadataType');
const metadataSearch = document.querySelector('#metadataSearch');
const metadataMessage = document.querySelector('#metadataMessage');
const metadataResults = document.querySelector('#metadataResults');
const groupResults = document.querySelector('#groupResults');
let scannedFiles = [];
let currentPreviews = [];
let activeSource = null;
let currentCandidates = [];
const selections = new Map();

async function request(url, options) {
  const response = await fetch(url, options);
  if (!response.ok) {
    const body = await response.json().catch(() => ({}));
    throw new Error(body.error || `请求失败 (${response.status})`);
  }
  return response.json();
}

async function loadRoots() {
  const roots = await request('/api/v1/roots');
  rootSelect.innerHTML = roots.map(root => `<option value="${escapeHtml(root.id)}">${escapeHtml(root.id)}${root.writable ? ' · 可写' : ' · 只读'}</option>`).join('');
}

scanButton.addEventListener('click', async () => {
  scanButton.disabled = true;
  message.className = 'message';
  message.textContent = '正在扫描和生成预览…';
  try {
    const root = encodeURIComponent(rootSelect.value);
    const path = encodeURIComponent(pathInput.value.trim());
    const files = await request(`/api/v1/roots/${root}/scan?path=${path}`);
    const media = files.filter(file => ['VIDEO', 'SUBTITLE'].includes(file.kind));
    const previews = media.length ? await request('/api/v1/rename-plans/preview', {
      method:'POST', headers:{'Content-Type':'application/json'}, body:JSON.stringify({paths:media.map(file => file.path)})
    }) : [];
    const groups = media.length ? await request('/api/v1/media/groups/analyze', {
      method:'POST', headers:{'Content-Type':'application/json'}, body:JSON.stringify({paths:media.map(file => file.path)})
    }) : [];
    scannedFiles = files;
    currentPreviews = previews;
    selections.clear();
    render(files, previews);
    renderGroups(groups);
    message.textContent = `完成：扫描到 ${files.length} 个文件，生成 ${previews.length} 条只读预览`;
  } catch (error) {
    message.className = 'message error';
    message.textContent = error.message;
  } finally { scanButton.disabled = false; }
});

function render(files, previews) {
  const episodes = previews.filter(item => item.media.type === 'EPISODE').length;
  const movies = previews.filter(item => item.media.type === 'MOVIE').length;
  const warnings = previews.filter(item => item.warnings.length).length;
  document.querySelector('#fileCount').textContent = files.length;
  document.querySelector('#episodeCount').textContent = episodes;
  document.querySelector('#movieCount').textContent = movies;
  document.querySelector('#warningCount').textContent = warnings;
  results.innerHTML = previews.length ? previews.map(item => `<tr>
    <td>${escapeHtml(item.source)}</td>
    <td><button class="title-link" data-title="${escapeHtml(item.media.title)}" data-type="${item.media.type === 'MOVIE' ? 'MOVIE' : 'SERIES'}"><span class="kind">${escapeHtml(item.media.type)}</span><br>${escapeHtml(item.media.title)}${item.media.year ? ` · ${item.media.year}` : ''}</button></td>
    <td>${escapeHtml(item.target)}</td>
    <td class="${item.warnings.length ? 'warn' : 'ok'}">${item.metadata ? `已确认 · TMDB #${escapeHtml(item.metadata.id)}` : (item.warnings.length ? escapeHtml(item.warnings.join('；')) : '待确认')}</td>
  </tr>`).join('') : '<tr class="empty"><td colspan="4">没有发现可预览的视频或字幕文件</td></tr>';
  results.querySelectorAll('.title-link').forEach(button => button.addEventListener('click', () => {
    metadataQuery.value = button.dataset.title;
    metadataType.value = button.dataset.type;
    activeSource = button.closest('tr').querySelector('td').textContent;
    metadataMessage.textContent = `正在为 ${activeSource} 选择元数据`;
    metadataQuery.scrollIntoView({behavior:'smooth', block:'center'});
    metadataQuery.focus();
  }));
}

function renderGroups(groups) {
  groupResults.innerHTML = groups.length ? groups.map(group => {
    const videos = group.members.filter(item => item.kind === 'VIDEO').length;
    const subtitles = group.members.filter(item => item.kind === 'SUBTITLE').length;
    const linked = group.members.filter(item => item.companionOf).length;
    return `<article class="media-group ${group.warnings.length ? 'has-warning' : ''}">
      <div><span class="kind">${escapeHtml(group.type)}</span><h3>${escapeHtml(group.title || '未识别媒体')}</h3></div>
      <p>${videos} 视频 · ${subtitles} 字幕 · ${linked} 已关联</p>
      ${group.warnings.length ? `<small>${escapeHtml(group.warnings.join('；'))}</small>` : ''}
    </article>`;
  }).join('') : '<div class="group-empty">没有发现可分析的媒体组</div>';
}

async function loadProviderStatus() {
  const provider = (await request('/api/v1/metadata/providers'))[0];
  const badge = document.querySelector('#providerStatus');
  badge.textContent = provider.available ? 'TMDB 已就绪' : 'TMDB 未配置';
  badge.classList.toggle('available', provider.available);
  badge.title = provider.message;
  metadataSearch.disabled = !provider.available;
  if (!provider.available) metadataMessage.textContent = provider.message;
}

metadataSearch.addEventListener('click', async () => {
  metadataSearch.disabled = true;
  metadataMessage.className = 'message';
  metadataMessage.textContent = '正在查询 TMDB…';
  try {
    const query = encodeURIComponent(metadataQuery.value.trim());
    currentCandidates = await request(`/api/v1/metadata/search?query=${query}&type=${metadataType.value}&locale=zh-CN&limit=12`);
    metadataMessage.textContent = `找到 ${currentCandidates.length} 个候选；选择后会重新生成只读预览`;
    metadataResults.innerHTML = currentCandidates.length ? currentCandidates.map((item, index) => `<article class="candidate">
      <div class="candidate-id">TMDB #${escapeHtml(item.id)}</div>
      <h3>${escapeHtml(item.title)}${item.year ? ` <span>${item.year}</span>` : ''}</h3>
      <p>${escapeHtml(item.overview || item.alternativeTitles?.join(' · ') || '暂无简介')}</p>
      <button class="choose-candidate" data-index="${index}">选用这个结果</button>
    </article>`).join('') : '<div class="candidate-empty">没有找到匹配候选</div>';
    metadataResults.querySelectorAll('.choose-candidate').forEach(button => button.addEventListener('click', () => chooseCandidate(currentCandidates[Number(button.dataset.index)])));
  } catch (error) {
    metadataMessage.className = 'message error';
    metadataMessage.textContent = error.message;
  } finally { metadataSearch.disabled = false; }
});

async function chooseCandidate(candidate) {
  if (!activeSource) {
    metadataMessage.className = 'message error';
    metadataMessage.textContent = '请先在扫描结果中点击一个识别标题';
    return;
  }
  selections.set(activeSource, {source:activeSource, provider:candidate.provider, id:candidate.id, type:candidate.type, title:candidate.title, year:candidate.year});
  metadataMessage.textContent = `已选择 ${candidate.title}，正在更新预览…`;
  try {
    currentPreviews = await request('/api/v1/rename-plans/preview', {
      method:'POST', headers:{'Content-Type':'application/json'},
      body:JSON.stringify({paths:currentPreviews.map(item => item.source), selections:[...selections.values()]})
    });
    render(scannedFiles, currentPreviews);
    metadataMessage.textContent = `已确认 ${candidate.title}；目标路径已更新，但尚未执行任何文件操作`;
  } catch (error) {
    metadataMessage.className = 'message error';
    metadataMessage.textContent = error.message;
  }
}

function escapeHtml(value) {
  return String(value ?? '').replace(/[&<>'"]/g, char => ({'&':'&amp;','<':'&lt;','>':'&gt;',"'":'&#39;','"':'&quot;'}[char]));
}

loadRoots().catch(error => { message.className='message error'; message.textContent=error.message; });
loadProviderStatus().catch(error => { metadataMessage.className='message error'; metadataMessage.textContent=error.message; });
