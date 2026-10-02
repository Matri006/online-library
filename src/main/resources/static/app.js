'use strict';
const $ = s => document.querySelector(s);
const esc = v => String(v ?? '').replace(/[&<>"']/g, c => ({'&':'&amp;','<':'&lt;','>':'&gt;','"':'&quot;',"'":'&#39;'}[c]));
const money = v => Number(v).toLocaleString('ru-RU',{style:'currency',currency:'RUB',maximumFractionDigits:2});
const date = v => v ? new Date(v).toLocaleString('ru-RU') : '—';
const roles = {ADMIN:'Администратор',LIBRARIAN:'Библиотекарь',VIEWER:'Сотрудник факультета'};
let session, view='books', page=0, query='', tableRows=[], tableColumns=[], tableActions, saveEditor, lookup={};
let renderVersion=0;
const views = {
 books:['Каталог книг','Издания, авторы и сведения о книгах в одном месте.','▤'],
 branches:['Филиалы','Библиотеки и книгохранилища организации.','⌂'],
 stock:['Книжный фонд','Общее число экземпляров, выдачи и доступный остаток.','▦'],
 usage:['Учебное использование','Связи книг с факультетами в каждом филиале.','◇'],
 loans:['Выдачи и возвраты','История работы с читателями.','⇄'],
 students:['Студенты','Читатели и их текущие факультеты.','♙'],
 reports:['Отчёты','Наличие книг и использование учебной литературы.','▥'],
 references:['Справочники','Авторы, издательства и факультеты.','☷'],
 locations:['Места хранения','Залы и склады внутри филиалов.','▧'],
 users:['Пользователи','Учётные записи и права доступа.','♧'],
 audit:['Журнал событий','Изменения данных и отклонённые операции.','◷']
};
function writable() { return session?.role === 'ADMIN' || session?.role === 'LIBRARIAN'; }
function admin() { return session?.role === 'ADMIN'; }
async function api(path,method='GET',data) {
 const headers={}; if(session?.csrfToken) headers[session.csrfHeader]=session.csrfToken;
 let body; if(data instanceof URLSearchParams) body=data; else if(data!==undefined){headers['Content-Type']='application/json';body=JSON.stringify(data);}
 const r=await fetch('/api'+path,{method,headers,body});
 const result=r.status===204?null:await r.json();
 if(!r.ok){if(r.status===401 && path!='/login') {$('#workspace').hidden=true;$('#login-screen').hidden=false;} const e=new Error(result.message||'Не удалось выполнить запрос');e.fields=result.fields;throw e;}
 return result;
}
function notice(message,error=false){const el=$('#notice');el.hidden=false;el.textContent=message;el.className=error?'error':'';}
function bind(id,fn){$(id)?.addEventListener('click',()=>Promise.resolve().then(fn).catch(e=>notice(e.message,true)));}
function active(v){return `<span class="badge ${v?'':'off'}">${v?'Активен':'Неактивен'}</span>`;}
async function refreshSession(){session=await api('/session');return session;}
async function start(){
 await refreshSession();$('#login-screen').hidden=session.authenticated;$('#workspace').hidden=!session.authenticated;
 if(!session.authenticated)return;
 $('#account-name').textContent=session.login;$('#account-role').textContent=roles[session.role];
 $('#navigation').innerHTML=Object.entries(views).filter(([key])=>admin()||!['users','audit'].includes(key)).map(([key,[title,,icon]])=>`<a href="#${key}" data-view="${key}"><span class="nav-icon">${icon}</span>${title}</a>`).join('');
 await navigate();
}
$('#login-form').addEventListener('submit',async e=>{e.preventDefault();const btn=e.submitter;btn.disabled=true;$('#login-error').textContent='';try{await refreshSession();await api('/login','POST',new URLSearchParams(new FormData(e.target)));e.target.reset();await start();}catch(ex){$('#login-error').textContent=ex.message;}finally{btn.disabled=false;}});
bind('#logout',async()=>{await api('/logout','POST');await start();});
window.addEventListener('hashchange',()=>navigate().catch(e=>notice(e.message,true)));
async function navigate(){if(!session?.authenticated)return;view=location.hash.slice(1)||'books';if(!views[view]||(!admin()&&['audit','users'].includes(view)))view='books';page=0;query='';await render();}
function table(items,columns,actions){
 tableRows=items;tableColumns=columns;tableActions=actions;
 $('#content').innerHTML=`<div class="table-wrap"><table><thead><tr>${columns.map(c=>`<th data-key="${esc(c.key)}">${esc(c.label)} ↕</th>`).join('')}${actions?'<th></th>':''}</tr></thead><tbody>${items.length?items.map((row,i)=>`<tr>${columns.map(c=>`<td>${c.render?c.render(row[c.key],row):esc(row[c.key])}</td>`).join('')}${actions?`<td><div class="row-actions">${actions(row,i)}</div></td>`:''}</tr>`).join(''):`<tr><td colspan="${columns.length+1}"><div class="empty">Записей пока нет</div></td></tr>`}</tbody></table></div>`;
 document.querySelectorAll('th[data-key]').forEach(th=>th.addEventListener('click',()=>{const key=th.dataset.key;const reverse=th.dataset.sorted==='asc';tableRows.sort((a,b)=>(typeof a[key]==='number'?a[key]-b[key]:String(a[key]??'').localeCompare(String(b[key]??''),'ru'))*(reverse?-1:1));table(tableRows,tableColumns,tableActions);document.querySelector(`th[data-key="${key}"]`).dataset.sorted=reverse?'desc':'asc';}));
 $('#content').querySelectorAll('[data-delete-book]').forEach(b=>b.addEventListener('click',async()=>{if(!confirm('Удалить книгу из каталога? Это действие нельзя отменить.'))return;b.disabled=true;try{await api('/books/'+b.dataset.deleteBook,'DELETE');await render();notice('Книга удалена.');}catch(e){notice(e.message,true);b.disabled=false;}}));
 $('#content').querySelectorAll('[data-edit]').forEach(b=>b.addEventListener('click',()=>edit(items[Number(b.dataset.edit)]).catch(e=>notice(e.message,true))));
 $('#content').querySelectorAll('[data-return]').forEach(b=>b.addEventListener('click',async()=>{if(!confirm('Подтвердить возврат книги?'))return;b.disabled=true;try{await api(`/loans/${b.dataset.return}/return`,'POST');await render();notice('Возврат зарегистрирован.');}catch(e){notice(e.message,true);b.disabled=false;}}));
 $('#content').querySelectorAll('[data-remove]').forEach(b=>b.addEventListener('click',async()=>{if(!confirm('Удалить связь книги с факультетом?'))return;const r=items[Number(b.dataset.remove)];try{await api('/usage','DELETE',{branchId:r.branch_id,bookId:r.book_id,facultyId:r.faculty_id});await render();}catch(e){notice(e.message,true);}}));
}
const bookActions=(r,i)=>`${editAction(r,i)}<button class="danger" data-delete-book="${r.book_id}">Удалить</button>`;
const editAction=(_,i)=>`<button class="secondary" data-edit="${i}">Изменить</button>`;
function pager(total,size){const more=total===null?tableRows.length===size:(page+1)*size<total;$('#content').insertAdjacentHTML('beforeend',`<div class="pager"><span>${total===null?'Страница '+(page+1):`Всего: ${total} · Страница ${page+1}`}</span><button id="prev" class="secondary" ${page===0?'disabled':''}>←</button><button id="next" class="secondary" ${more?'':'disabled'}>→</button></div>`);bind('#prev',async()=>{page--;await render();});bind('#next',async()=>{page++;await render();});}
let refType='authors';
async function render(){
 const version=++renderVersion;const [title,description]=views[view];$('#page-title').textContent=title;$('#section-label').textContent=title.toUpperCase();$('#page-description').textContent=description;$('#notice').hidden=true;
 document.querySelectorAll('nav a').forEach(a=>a.classList.toggle('active',a.dataset.view===view));
 $('#add-button').hidden=!(writable()&&['books','stock','usage','loans','students'].includes(view)||admin()&&['branches','references','locations','users'].includes(view));
 $('#add-button').textContent=view==='loans'?'+ Выдать книгу':view==='stock'?'+ Разместить книгу':'+ Добавить';
 $('#toolbar').innerHTML=view==='reports'?'':`<input id="search" placeholder="Поиск в разделе…" value="${esc(query)}" aria-label="Поиск"><button id="search-button" class="secondary">Найти</button>`;
 bind('#search-button',async()=>{query=$('#search').value;page=0;await render();});$('#search')?.addEventListener('keydown',e=>{if(e.key==='Enter')$('#search-button').click();});
 if(view==='references'){$('#toolbar').insertAdjacentHTML('beforeend',`<select id="ref-type" aria-label="Справочник"><option value="authors">Авторы</option><option value="publishers">Издательства</option><option value="faculties">Факультеты</option></select>`);$('#ref-type').value=refType;$('#ref-type').onchange=async e=>{refType=e.target.value;page=0;query='';await render();};}
 $('#content').innerHTML='<div class="empty">Загрузка…</div>';
 if(view==='reports'){await reports();return;}
 let data,cols,actions;
 const name={key:'name',label:'Наименование'},status={key:'is_active',label:'Статус',render:active};
 switch(view){
 case 'books':{
   const result=await api(`/books?q=${encodeURIComponent(query)}&page=${page}&size=20`);if(version!==renderVersion)return;
   table(result.items,[{key:'title',label:'Книга',render:(v,r)=>`${esc(v)}<small>${esc(r.authors)}</small>`},{key:'publisher_name',label:'Издательство'},{key:'publication_year',label:'Год'},{key:'pages_count',label:'Страниц'},{key:'illustrations_count',label:'Иллюстраций'},{key:'price',label:'Стоимость',render:money}],writable()?bookActions:null);pager(result.total,20);return;}
 case 'branches':data=await api('/branches');cols=[name,{key:'branch_type',label:'Тип',render:v=>v==='BRANCH'?'Филиал':'Книгохранилище'},{key:'address',label:'Адрес'},{key:'phone',label:'Телефон'},status];actions=admin()?editAction:null;break;
 case 'stock':data=await api('/stock');cols=[{key:'title',label:'Книга'},{key:'branch_name',label:'Филиал'},{key:'location_name',label:'Место'},{key:'copies_count',label:'Всего'},{key:'loaned',label:'Выдано'},{key:'available',label:'Доступно',render:v=>`<span class="badge">${esc(v)}</span>`}];actions=writable()?editAction:null;break;
 case 'usage':data=await api('/usage');cols=[{key:'title',label:'Книга'},{key:'branch_name',label:'Филиал'},{key:'faculty_name',label:'Факультет'}];actions=writable()?(_,i)=>`<button class="danger" data-remove="${i}">Удалить связь</button>`:null;break;
 case 'loans':data=await api(`/loans?offset=${page*1000}`);cols=[{key:'title',label:'Книга',render:(v,r)=>`${esc(v)}<small>${esc(r.branch_name)} · ${esc(r.location_name)}</small>`},{key:'full_name',label:'Читатель',render:(v,r)=>`${esc(v)}<small>${esc(r.student_card_number)} · ${esc(r.faculty_name)}</small>`},{key:'issued_at',label:'Выдана',render:date},{key:'returned_at',label:'Возвращена',render:date}];actions=writable()?r=>r.returned_at?'':`<button class="secondary" data-return="${r.loan_id}">Вернуть</button>`:null;break;
 case 'students':data=await api(`/students?q=${encodeURIComponent(query)}&offset=${page*1000}`);cols=[{key:'full_name',label:'ФИО'},{key:'student_card_number',label:'Студенческий билет'},{key:'faculty_name',label:'Факультет'},status];actions=writable()?editAction:null;break;
 case 'locations':data=await api('/locations');cols=[name,{key:'code',label:'Код'},{key:'branch_name',label:'Филиал'},status];actions=admin()?editAction:null;break;
 case 'references':data=await api(`/references/${refType}?q=${encodeURIComponent(query)}&offset=${page*1000}`);cols=[name,...(refType==='faculties'?[status]:[])];actions=admin()?editAction:null;break;
 case 'users':data=await api('/users');cols=[{key:'login',label:'Логин'},{key:'role',label:'Роль',render:v=>esc(roles[v])},status];actions=editAction;break;
 case 'audit':data=await api(`/audit?offset=${page*100}&q=${encodeURIComponent(query)}`);cols=[{key:'event_time',label:'Время',render:date},{key:'login',label:'Пользователь',render:v=>esc(v||'Система')},{key:'operation',label:'Операция'},{key:'entity_type',label:'Объект',render:(v,r)=>`${esc(v||'—')}<small>${esc(JSON.stringify(r.entity_key))}</small>`},{key:'details',label:'Детали',render:v=>`<details><summary>Посмотреть</summary><pre>${esc(JSON.stringify(v,null,2))}</pre></details>`}];break;
 }
 if(version!==renderVersion)return;
 if(!['audit','references','students'].includes(view)&&query)data=data.filter(r=>JSON.stringify(r).toLocaleLowerCase('ru').includes(query.toLocaleLowerCase('ru')));
 table(data,cols,actions);if(['loans','students','references','audit'].includes(view))pager(null,view==='audit'?100:1000);
}
async function loadLookups(){
 const [books,branches,authors,publishers,faculties,locations,students]=await Promise.all([api('/books?size=1000'),api('/branches'),api('/references/authors'),api('/references/publishers'),api('/references/faculties'),api('/locations'),api('/students')]);
 lookup={books:books.items,branches,authors,publishers,faculties,locations,students};
}
function options(kind){
 const map={books:['book_id','title'],branches:['branch_id','name'],authors:['author_id','name'],publishers:['publisher_id','name'],faculties:['faculty_id','name'],locations:['location_id','name'],students:['student_id','full_name']};
 const [id,label]=map[kind];return lookup[kind].map(r=>({value:r[id],label:kind==='locations'?`${r.branch_name} / ${r.name}`:kind==='students'?`${r.full_name} · ${r.student_card_number}`:kind==='books'?`${r.title} (${r.publication_year})`:r[label]}));
}
const f=(name,label,type='text',extra={})=>({name,label,type,...extra});
function sel(name,label,kind,extra={}){return f(name,label,'select',{kind,options:options(kind),...extra});}
function fieldHtml(d,values){
 const value=values[d.name]??d.default??'';const required=d.optional?'':'required';
 let control;
 if(d.type==='select')control=`${['books','authors','publishers','students'].includes(d.kind)?`<input data-search-select="${d.name}" placeholder="Поиск по названию / ФИО…" aria-label="Поиск: ${esc(d.label)}">`:''}<select name="${d.name}" ${required} ${d.multiple?'multiple':''} ${d.disabled?'disabled':''}>${!d.multiple?'<option value="">Выберите…</option>':''}${d.options.map(o=>`<option value="${esc(o.value)}" ${(d.multiple?(Array.isArray(value)?value:[]).map(String).includes(String(o.value)):String(value)===String(o.value))?'selected':''}>${esc(o.label)}</option>`).join('')}</select>${d.multiple?'<small>Несколько авторов: удерживайте ⌘ (Mac) или Ctrl (Windows).</small>':''}`;
 else if(d.type==='checkbox')control=`<input name="${d.name}" type="checkbox" ${value?'checked':''}>`;
 else control=`<input name="${d.name}" type="${d.type}" value="${esc(value)}" ${required} ${d.min!==undefined?`min="${d.min}"`:''} ${d.max!==undefined?`max="${d.max}"`:''} ${d.step?`step="${d.step}"`:''} ${d.type==='password'?'autocomplete="new-password"':''}>`;
 return `<label class="${d.wide?'wide':''}">${esc(d.label)}${!d.optional&&d.type!=='checkbox'?' *':''}${control}</label>`;
}
function editor(title,fields,values,save){
 $('#editor-title').textContent=title;$('#editor-fields').innerHTML=fields.map(d=>fieldHtml(d,values)).join('');$('#editor-error').textContent='';
 saveEditor=async()=>{const result={};for(const d of fields){const el=$('#edit-form').elements[d.name];result[d.name]=d.type==='checkbox'?el.checked:d.multiple?Array.from(el.selectedOptions,o=>Number(o.value)):d.type==='number'||d.kind?Number(el.value):el.value;}await save(result);};
 $('#editor').showModal();attachSelectSearch($('#editor-fields'),fields);
}
function attachSelectSearch(root,fields){
 root.querySelectorAll('[data-search-select]').forEach(input=>{let timer,generation=0;input.addEventListener('input',()=>{clearTimeout(timer);const g=++generation;timer=setTimeout(async()=>{const d=fields.find(f=>f.name===input.dataset.searchSelect);const term=encodeURIComponent(input.value);try{const endpoint=d.kind==='books'?`/books?q=${term}&size=1000`:d.kind==='students'?`/students?q=${term}`:`/references/${d.kind}?q=${term}`;const result=await api(endpoint);if(g!==generation)return;lookup[d.kind]=d.kind==='books'?result.items:result;const select=root.querySelector(`select[name="${d.name}"]`);const selected=Array.from(select.selectedOptions).filter(o=>o.value).map(o=>({value:o.value,label:o.textContent}));const opts=options(d.kind);for(const o of selected)if(!opts.some(x=>String(x.value)===String(o.value)))opts.push(o);select.innerHTML=(!d.multiple?'<option value="">Выберите…</option>':'')+opts.map(o=>`<option value="${esc(o.value)}" ${selected.some(x=>String(x.value)===String(o.value))?'selected':''}>${esc(o.label)}</option>`).join('');}catch(e){$('#editor-error').textContent=e.message;}},300);});});
}
$('#edit-form').addEventListener('submit',async e=>{e.preventDefault();$('#save-button').disabled=true;$('#editor-error').textContent='';try{await saveEditor();$('#editor').close();await render();notice('Данные сохранены.');}catch(ex){$('#editor-error').textContent=ex.message;Object.keys(ex.fields||{}).forEach(name=>{const el=e.target.elements[name];if(el)el.setAttribute('aria-invalid','true');});}finally{$('#save-button').disabled=false;}});
bind('#close-editor',()=>$('#editor').close());bind('#cancel-editor',()=>$('#editor').close());bind('#add-button',()=>edit());
async function edit(row={}){
 await loadLookups();let fields,values={},endpoint,method='POST',title='Добавить запись';
 const activeField=f('active','Активен','checkbox',{default:true,optional:true});
 const saveAt=(base,id)=>{endpoint=base+(id?'/'+id:'');method=id?'PUT':'POST';title=id?'Изменить запись':'Добавить запись';};
 switch(view){
 case 'books':saveAt('/books',row.book_id);fields=[f('title','Название','text',{wide:true}),sel('authorIds','Авторы','authors',{multiple:true,wide:true}),sel('publisherId','Издательство','publishers'),f('publicationYear','Год издания','number',{min:1450,max:new Date().getFullYear()+1,default:new Date().getFullYear()}),f('pagesCount','Число страниц','number',{min:1}),f('illustrationsCount','Число иллюстраций','number',{min:0,default:0}),f('price','Стоимость, ₽','number',{min:0,step:'0.01',default:0})];values={title:row.title,authorIds:row.author_ids,publisherId:row.publisher_id,publicationYear:row.publication_year,pagesCount:row.pages_count,illustrationsCount:row.illustrations_count,price:row.price};break;
 case 'branches':saveAt('/branches',row.branch_id);fields=[f('name','Наименование','text',{wide:true}),f('branchType','Тип','select',{options:[{value:'BRANCH',label:'Филиал'},{value:'DEPOSITORY',label:'Книгохранилище'}],default:'BRANCH'}),f('phone','Телефон','tel',{optional:true}),f('address','Адрес','text',{wide:true,optional:true}),activeField];values={...row,branchType:row.branch_type,active:row.is_active};break;
 case 'references':{const id=row.author_id||row.publisher_id||row.faculty_id;saveAt('/references/'+refType,id);fields=[f('name','Наименование / ФИО','text',{wide:true}),...(refType==='faculties'?[activeField]:[])];values={name:row.name,active:row.is_active};break;}
 case 'locations':saveAt('/locations',row.location_id);fields=[sel('branchId','Филиал','branches',{wide:true}),f('code','Код места'),f('name','Название'),activeField];values={...row,branchId:row.branch_id,active:row.is_active};break;
 case 'students':saveAt('/students',row.student_id);fields=[f('fullName','ФИО','text',{wide:true}),f('studentCardNumber','Студенческий билет'),sel('facultyId','Факультет','faculties'),activeField];values={fullName:row.full_name,studentCardNumber:row.student_card_number,facultyId:row.faculty_id,active:row.is_active};break;
 case 'stock':endpoint='/stock';method='PUT';title='Учёт экземпляров';fields=[sel('bookId','Книга','books',{wide:true,disabled:!!row.book_id}),sel('locationId','Место хранения','locations',{wide:true,disabled:!!row.book_id}),f('copiesCount','Общий фонд (включая выданные)','number',{min:0,default:0,wide:true})];values={bookId:row.book_id,locationId:row.location_id,copiesCount:row.copies_count};break;
 case 'usage':endpoint='/usage';fields=[sel('bookId','Книга','books',{wide:true}),sel('branchId','Филиал','branches',{wide:true}),sel('facultyId','Факультет','faculties',{wide:true})];break;
 case 'loans':endpoint='/loans';title='Выдать книгу';fields=[sel('bookId','Книга','books',{wide:true}),sel('locationId','Место хранения','locations',{wide:true}),sel('studentId','Студент','students',{wide:true})];break;
 case 'users':saveAt('/users',row.user_id);fields=[f('login','Логин'),f('password',row.user_id?'Новый пароль (пусто — прежний)':'Пароль (от 8 символов)','password',{optional:!!row.user_id}),f('role','Роль','select',{options:Object.entries(roles).map(([value,label])=>({value,label})),default:'VIEWER'}),activeField];values={...row,active:row.is_active};break;
 }
 // Preserve selections outside the initial lookup page when editing a record.
 for(const d of fields.filter(d=>d.type==='select'&&d.kind)){
   const chosen=d.multiple?(values[d.name]||[]):[values[d.name]];
   for(const id of chosen)if(id!=null&&!d.options.some(o=>String(o.value)===String(id)))d.options.push({value:id,label:`ID ${id}`});
 }
 let expected=row.copies_count??0;
 editor(title,fields,values,async data=>{
   if(view==='stock')data.expectedCopiesCount=expected;
   if(view==='references'&&data.active===undefined)data.active=true;
   if(row.is_active===true&&data.active===false&&!confirm('Деактивировать запись? История будет сохранена.'))return Promise.reject(new Error('Деактивация отменена.'));
   await api(endpoint,method,data);
 });
 if(view==='stock'&&!row.book_id){const updateExpected=async()=>{const form=$('#edit-form');const book=Number(form.elements.bookId.value),locationId=Number(form.elements.locationId.value);if(book&&locationId){const stock=await api('/stock?bookId='+book);expected=stock.find(r=>r.location_id===locationId)?.copies_count??0;form.elements.copiesCount.value=expected;}};['bookId','locationId'].forEach(name=>$('#edit-form').elements[name].addEventListener('change',()=>updateExpected().catch(e=>{$('#editor-error').textContent=e.message;})));}
}
async function reports(){
 await loadLookups();const fields=[sel('bookId','Книга','books'),sel('branchId','Филиал','branches')];
 $('#content').innerHTML=`<div class="panel"><form id="report-form" class="report-form">${fields.map(d=>fieldHtml(d,{})).join('')}<button>Сформировать отчёт</button></form></div><div id="report-result"></div><div class="panel"><h2>Уникальные студенты</h2><p class="muted">Повторные выдачи одному студенту считаются один раз. Даты ограничивают момент выдачи: начало включительно, конец не включается.</p><form id="student-report" class="report-form">${fieldHtml(sel('facultyId','Факультет','faculties',{optional:true}),{})}<label>С даты<input name="from" type="datetime-local"></label><label>До даты<input name="to" type="datetime-local"></label><button class="secondary">Посчитать</button></form><p id="student-result"></p></div>`;
 attachSelectSearch($('#report-form'),fields);
 $('#report-form').addEventListener('submit',async e=>{e.preventDefault();const btn=e.submitter;btn.disabled=true;try{const p=new URLSearchParams(new FormData(e.target));const r=await api('/reports/book?'+p);$('#report-result').innerHTML=`<div class="stats">${[['Всего экземпляров',r.copies],['Доступно',r.available],['Выдано',r.loaned],['Уникальных студентов',r.studentCount]].map(([label,v])=>`<div class="stat"><small>${label}</small><strong>${esc(v)}</strong></div>`).join('')}</div><div class="panel"><h2>Используют факультеты · ${r.facultyCount}</h2>${r.faculties.length?`<ul>${r.faculties.map(f=>`<li>${esc(f.name)}</li>`).join('')}</ul>`:'<p class="muted">Учебное использование не зарегистрировано.</p>'}${r.message?`<p class="muted">${esc(r.message)}</p>`:''}</div>`;}catch(ex){notice(ex.message,true);}finally{btn.disabled=false;}});
 $('#student-report').addEventListener('submit',async e=>{e.preventDefault();try{const bookId=$('#report-form').elements.bookId.value;if(!bookId)throw new Error('Сначала выберите книгу выше.');const p=new URLSearchParams({bookId});const branch=$('#report-form').elements.branchId.value;if(branch)p.set('branchId',branch);const data=new FormData(e.target);for(const key of ['facultyId','from','to'])if(data.get(key))p.set(key,key==='facultyId'?data.get(key):new Date(data.get(key)).toISOString());const r=await api('/reports/students?'+p);$('#student-result').textContent=`Уникальных студентов: ${r.count}`;}catch(ex){notice(ex.message,true);}});
}
start().catch(e=>{$('#login-error').textContent=e.message;});
