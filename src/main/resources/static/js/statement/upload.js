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
    
    if(!bank || !type){ showWarn('Please select Bank and Card Type'); return; }
    if(!file){ showWarn('Please select a file'); return; }

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
            if(res.returnCode === 'success'){
                var payload = parseReturnMessage(res.returnMessage);
                var statementId = payload.statementId || res.returnMessage;
                $('#currentStatementId').val(statementId);
                reloadTemp(statementId);
                var rows = payload.rows || 0;
                var parsed = payload.parsed || 0;
                if(rows || parsed){
                    showInfo('提示', '解析完成：共 ' + rows + ' 行，成功解析 ' + parsed + ' 条。');
                }else{
                    showInfo('提示', '文件上传并解析成功。');
                }
            } else {
                showWarn(res.returnMessage || 'Upload failed');
            }
        },
        error: function(){
            $.messager.progress('close');
            showWarn('Network Error');
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
    $('#dgTempUpload').datagrid({ url: '/statement/preview?statementId=' + sid });
    $('#dgTempUpload').datagrid('reload');
    try{ console.log('[upload] preview grid reload with statementId', sid); }catch(e){}
}

function initTempGrid(){
    $('#dgTempUpload').datagrid({
        fit: true,
        striped: true,
        rownumbers: true,
        pagination: false,
        nowrap: false,
        method: 'get',
        singleSelect: true,
        fitColumns: true,
        remoteSort: false,
        onDblClickRow: function(i,r){ $(this).datagrid('beginEdit',i); },
        columns: [[
            {field:'bankCardName', title:'Card Name', width:160},
            {field:'bookKeepingDate', title:'Posting Date', width:100, align:'center', formatter: formatDateOnly, sortable:true, sorter: dateSorter},
            {field:'transactionDateTime', title:'TXN Date', width:160, align:'center', sortable:true, sorter: dateSorter},
            {field:'transactionDesc', title:'Narration', width:260},
            {field:'balanceCurrency', title:'Currency', width:80, align:'center'},
            {field:'incomeMoney', title:'Income', width:100, align:'right'},
            {field:'balanceMoney', title:'Expense', width:100, align:'right'},
            {field:'accountBalance', title:'Balance', width:120, align:'right'},
            {field:'consumeCode', title:'Category', width:220, 
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
            {field:'opponentName', title:'Opponent Name', width:180},
            {field:'opponentAccount', title:'Opponent Acc', width:220}
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
    if(!sid){ showWarn('No statement uploaded'); return; }

    $.messager.confirm('Confirm', 'Are you sure you want to commit these transactions?', function(r){
        if(r){
            $.messager.progress({ title:'Saving', msg:'Committing transactions...' });
            $.post('/statement/commit', {statementId: sid}, function(res){
                $.messager.progress('close');
                if(res.returnCode === 'success'){
                    var payload = parseReturnMessage(res.returnMessage);
                    var imported = payload.imported || 0;
                    var total = payload.total || imported;
                    var failed = (typeof payload.failed !== 'undefined') ? payload.failed : Math.max(0, total - imported);
                    showInfo('提示', '提交完成：共 ' + total + ' 条，成功导入 ' + imported + ' 条，失败 ' + failed + ' 条。');
                    // Clear UI
                    $('#dgTempUpload').datagrid('loadData', []);
                    $('#currentStatementId').val('');
                    $('#fileBill').filebox('clear');
                } else {
                    showWarn(res.returnMessage || 'Commit failed');
                }
            });
        }
    });
}

function showWarn(msg){ $.messager.alert('提示', msg, 'warning'); }
function showInfo(title, msg){ $.messager.alert(title, msg, 'info'); }

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
    showInfo('Info', 'Auto Classify logic can be migrated here.');
}

function exportPreview(){
    var sid = $('#currentStatementId').val();
    if(!sid){ showWarn('No statement uploaded'); return; }
    window.open('/statement/export?statementId=' + encodeURIComponent(sid), '_blank');
}
