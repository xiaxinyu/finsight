$(function(){
    try{ console.log('[upload] init'); }catch(e){}
    $('#cmbBank').combobox({
        data: statement_datasource.banks,
        valueField:'id', textField:'text', editable:false, prompt:'Please select bank',
        onSelect: function(rec){ 
            try{ console.log('[upload] bank selected', rec, 'current type', $('#cmbCardType').combobox('getValue')); }catch(e){}
            refreshCardNos({ bank: rec.id, type: $('#cmbCardType').combobox('getValue') });
        }
    });

    $('#cmbCardType').combobox({
        data: statement_datasource.cardTypes,
        valueField:'id', textField:'text', editable:false, prompt:'Please select type',
        onSelect: function(rec){ 
            try{ console.log('[upload] type selected', rec, 'current bank', $('#cmbBank').combobox('getValue')); }catch(e){}
            refreshCardNos({ bank: $('#cmbBank').combobox('getValue'), type: rec.id }); 
        },
        onChange: function(newVal, oldVal){
            try{ console.log('[upload] type changed', {newVal:newVal, oldVal:oldVal, text: $('#cmbCardType').combobox('getText')}); }catch(e){}
            refreshCardNos({ bank: $('#cmbBank').combobox('getValue'), type: newVal });
        }
    });

    $('#cmbCardNo').combobox({
        valueField:'id', textField:'text', editable:false, prompt:'Select Card No'
    });

    try{ console.log('[upload] combobox ready'); }catch(e){}
    try{ console.log('[upload] skip prefetch, use backend numbers API'); }catch(e){}
    initTempGrid();
    reloadTemp();
});

var _card_cache = {};
var _pending_xhr = null;
var _refreshing = false;
function isSuccessResult(res){
    try{ return !!(res && app.api && app.api.normalizeResult && app.api.normalizeResult(res).ok); }catch(e){}
    return !!(res && (res.returnCode === 'success' || res.code === 20000 || res.code === 200));
}
function getResultData(res){
    try{
        if(res && app.api && app.api.normalizeResult){
            return app.api.normalizeResult(res).data;
        }
    }catch(e){}
    return res ? res.returnMessage : null;
}
function getResultMessage(res, fallback){
    try{
        if(res && app.api && app.api.normalizeResult){
            var n = app.api.normalizeResult(res);
            if(n && n.message){ return n.message; }
        }
    }catch(e){}
    return (res && res.returnMessage) ? res.returnMessage : (fallback || 'Operation failed.');
}
function _key(b,t){ return (b||'')+':'+(t||''); }

function fetchAllCards(){
    try{ console.log('[upload] fetchAllCards start'); }catch(e){}
    $.get('/api/v1/cards', function(cards){
        cards = cards || [];
        _card_cache = {};
        try{ console.log('[upload] fetched cards count', cards.length); }catch(e){}
        for(var i=0;i<cards.length;i++){
            var c = cards[i] || {};
            var bank = (c.bankCode||'').toUpperCase();
            var type = (c.cardTypeCode||'').toLowerCase();
            var k = _key(bank, type);
            var name = c.cardName;
            if(!name || $.trim(name).length===0){
                var no = c.cardNo || '';
                var masked = no.length>4 ? ('****'+no.substring(no.length-4)) : no;
                name = $.trim([bank, masked].join(' '));
            }
            var item = { id: c.cardNo, text: name };
            (_card_cache[k] = _card_cache[k] || []).push(item);
        }
        try{ console.log('[upload] cache keys', Object.keys(_card_cache)); }catch(e){}
        // no auto refresh here
    });
}

function refreshCardNos(ctx){
    if(_refreshing){ try{ console.log('[upload] skip refresh (busy)'); }catch(e){} return; }
    _refreshing = true;
    var bank = (ctx && ctx.bank) || $('#cmbBank').combobox('getValue');
    var type = (ctx && ctx.type) || $('#cmbCardType').combobox('getValue');
    var typeText = $('#cmbCardType').combobox('getText');
    if(!type && typeText){
        var m = String(typeText||'').toLowerCase();
        if(m.indexOf('debit') >= 0){ type = 'debit'; }
        else if(m.indexOf('credit') >= 0){ type = 'credit'; }
        try{ console.log('[upload] type fallback from text', {text:typeText, mapped:type}); }catch(e){}
    }
    try{ console.log('[upload] refreshCardNos', {bank:bank, type:type}); }catch(e){}
    
    $('#cmbCardNo').combobox('clear');
    $('#cmbCardNo').combobox('loadData', []);
    
    if(!bank || !type){ _refreshing = false; return; }

    if(_pending_xhr){ try{ _pending_xhr.abort(); }catch(e){} _pending_xhr=null; }
    var params = { bankCode: bank, cardTypeCode: type };
    try{ console.log('[upload] numbers request params', params); }catch(e){}
    _pending_xhr = $.get('/api/v1/cards/numbers', params, function(list){
        var data = $.map(list || [], function(kv){ return { id: kv.key, text: kv.value || kv.key }; });
        $('#cmbCardNo').combobox('loadData', data);
        try{ 
            console.log('[upload] numbers response length', data.length); 
            console.log('[upload] numbers sample', $.map(data.slice(0,5), function(it){ return it.id; })); 
        }catch(e){}
    }).always(function(){ _pending_xhr=null; _refreshing=false; });
    _refreshing = false;
}

function submitUpload(){
    var bank = $('#cmbBank').combobox('getValue');
    var type = $('#cmbCardType').combobox('getValue');
    var file = $('#fileBill').filebox('getValue');
    
    if(!bank || !type){ showWarn('Please select bank and card type.'); return; }
    if(!file){ showWarn('Please select a file.'); return; }

    var formData = new FormData($('#uploadForm')[0]);
    $.messager.progress({ title:'Uploading', msg:'Processing file...' });
    $.ajax({
        url: '/statement/upload',
        type: 'POST',
        data: formData,
        processData: false,
        contentType: false,
        success: function(res){
            $.messager.progress('close');
            if(isSuccessResult(res)){
                var responseData = getResultData(res);
                var payload = parseReturnMessage(responseData);
                var statementId = payload.statementId || responseData;
                $('#currentStatementId').val(statementId);
                reloadTemp(statementId);
                var rows = payload.rows || 0;
                var parsed = payload.parsed || 0;
                if(rows || parsed){
                    showInfo('Parsed ' + rows + ' rows, ' + parsed + ' transactions.');
                }else{
                    showInfo('File uploaded and parsed successfully.');
                }
            } else {
                showWarn(getResultMessage(res, 'Upload failed.'));
            }
        },
        error: function(){
            $.messager.progress('close');
            showError('Network error.');
        }
    });
}

function reloadTemp(statementId){
    var sid = statementId || $('#currentStatementId').val();
    if(!sid){
        $('#dgTempUpload').datagrid('loadData', []);
        try{ console.log('[upload] preview grid ready, no statementId'); }catch(e){}
        return;
    }
    // Update URL and reload, preserving other options like loadFilter
    var opts = $('#dgTempUpload').datagrid('options');
    opts.url = '/statement/preview?statementId=' + sid;
    $('#dgTempUpload').datagrid('load');
    try{ console.log('[upload] preview grid reload with statementId', sid); }catch(e){}
}

function clientSideFilter(data){
    if (typeof data.length == 'number' && typeof data.splice == 'function'){    // is array
        data = {
            total: data.length,
            rows: data
        }
    }
    var dg = $(this);
    var opts = dg.datagrid('options');
    var pager = dg.datagrid('getPager');
    pager.pagination({
        onSelectPage:function(pageNum, pageSize){
            opts.pageNumber = pageNum;
            opts.pageSize = pageSize;
            pager.pagination('refresh',{
                pageNumber:pageNum,
                pageSize:pageSize
            });
            dg.datagrid('loadData',data);
        }
    });
    if (!data.originalRows){
        data.originalRows = (data.rows);
    }
    var start = (opts.pageNumber-1)*parseInt(opts.pageSize);
    var end = start + parseInt(opts.pageSize);
    data.rows = (data.originalRows.slice(start, end));
    return data;
}

function initTempGrid(){
    $('#dgTempUpload').datagrid({
        fit: true,
        striped: true,
        rownumbers: true,
        pagination: true,
        pageSize: 50,
        pageList: [50, 100, 200, 500],
        loadFilter: clientSideFilter,
        nowrap: false,
        method: 'get',
        singleSelect: true,
        fitColumns: true,
        remoteSort: false,
        onDblClickRow: function(i,r){ $(this).datagrid('beginEdit',i); },
        columns: [[
            {field:'bankCardName', title:'Card Name', width:160, halign:'center'},
            {field:'bookKeepingDate', title:'Posting Date', width:100, align:'center', halign:'center', formatter: formatDateOnly, sortable:true, sorter: dateSorter},
            {field:'transactionDateTime', title:'TXN Date', width:160, align:'center', halign:'center', sortable:true, sorter: dateSorter},
            {field:'transactionDesc', title:'Narration', width:260, halign:'center'},
            {field:'balanceCurrency', title:'Currency', width:80, align:'center', halign:'center'},
            {field:'incomeMoney', title:'Income', width:100, align:'right', halign:'center'},
            {field:'balanceMoney', title:'Expense', width:100, align:'right', halign:'center'},
            {field:'accountBalance', title:'Balance', width:120, align:'right', halign:'center'},
            {field:'consumeCode', title:'Category', width:220, halign:'center',
                formatter: function(v,r){ return r.consumeName || v; },
                editor:{
                    type:'combotree',
                    options:{
                        url:'/api/v1/consume/tree',
                        method:'get',
                        onLoadSuccess: function(){ try{$(this).tree('collapseAll');}catch(e){} }
                    }
                }
            },
            {field:'opponentName', title:'Opponent Name', width:180, halign:'center'},
            {field:'opponentAccount', title:'Opponent Acc', width:220, halign:'center'}
        ]]
    });
    $('#dgTempUpload').datagrid('loadData', []);
    try{ console.log('[upload] preview grid initialized'); }catch(e){}
}

function formatDateOnly(val){
    var dateStr = '';
    if(val){
        var d = new Date(val);
        if(!isNaN(d.getTime())){
            dateStr = d.toISOString().split('T')[0];
        } else {
            dateStr = val;
        }
    }
    return dateStr;
}

function dateSorter(a, b){
    function toTime(x){
        if(!x) return -Infinity;
        var d = new Date(x);
        if(!isNaN(d.getTime())) return d.getTime();
        var s = String(x);
        if(/^\d{4}-\d{2}-\d{2}(?:[ T]\d{2}:\d{2}:\d{2})?$/.test(s)){
            var d2 = new Date(s);
            if(!isNaN(d2.getTime())) return d2.getTime();
        }
        return 0;
    }
    return toTime(a) - toTime(b);
}

// transactionDateTime is provided by backend

function saveAll(){
    var sid = $('#currentStatementId').val();
    if(!sid){ showWarn('No statement uploaded.'); return; }

    $.messager.confirm('Confirm', 'Are you sure you want to commit these transactions?', function(r){
        if(r){
            $.messager.progress({ title:'Saving', msg:'Committing transactions...' });
            $.post('/statement/commit', {statementId: sid}, function(res){
                $.messager.progress('close');
                if(isSuccessResult(res)){
                    var payload = parseReturnMessage(getResultData(res));
                    var imported = payload.imported || 0;
                    var total = payload.total || imported;
                    var failed = (typeof payload.failed !== 'undefined') ? payload.failed : Math.max(0, total - imported);
                    showInfo('Commit done: total ' + total + ', imported ' + imported + ', failed ' + failed + '.');
                    // Clear UI
                    $('#dgTempUpload').datagrid('loadData', []);
                    $('#currentStatementId').val('');
                    $('#fileBill').filebox('clear');
                } else {
                    showWarn(getResultMessage(res, 'Commit failed.'));
                }
            });
        }
    });
}

function showWarn(msg){ app.messager.fail(msg || 'Operation failed.'); }
function showInfo(msg){ app.messager.success(msg || 'Operation succeeded.'); }
function showError(msg){ app.messager.error(msg || 'Unexpected error.'); }

function parseReturnMessage(msg){
    try{
        if(typeof msg === 'string'){
            var t = msg.trim();
            if(t.indexOf('{') === 0){
                return JSON.parse(t);
            }
            if(t.indexOf('"') === 0){
                var once = JSON.parse(t);
                if(typeof once === 'string'){
                    var twice = once.trim();
                    if(twice.indexOf('{') === 0){
                        return JSON.parse(twice);
                    }
                }
                return once;
            }
        }
        if(typeof msg === 'object' && msg){
            return msg;
        }
    }catch(e){}
    return {};
}

function autoClassifySelected(){
    // ... Implement if needed, similar to original ...
    showInfo('Auto classify logic can be migrated here.');
}

function exportPreview(){
    var sid = $('#currentStatementId').val();
    if(!sid){ showWarn('No statement uploaded.'); return; }
    window.open('/statement/export?statementId=' + encodeURIComponent(sid), '_blank');
}
