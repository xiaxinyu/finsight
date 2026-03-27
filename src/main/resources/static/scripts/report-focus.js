(function(window, $){
    'use strict';

    var hintMap = {
        fixed: 'Focus: fixed-cost pressure (rent, utilities, subscriptions).',
        variable: 'Focus: variable spending pressure (shopping + entertainment).',
        savings: 'Focus: low savings rate and surplus trend.',
        deficit: 'Focus: cash deficit months and overspending.',
        yoy: 'Focus: YoY deterioration categories.',
        steady: 'Focus: maintain stable spending structure.'
    };

    var ruleMap = {
        fixed: {
            exactNames: ['固定支出','房租','房贷','按揭','贷款','物业','水电','燃气','通信','保险'],
            exactCodes: ['FIXED','RENT','MORTGAGE','LOAN','UTILITY','INSURANCE'],
            fallback: ['fixed','rent','loan','mortgage','utility','subscription','insurance','租','房租','按揭','贷款','水电','物业','通信','保险']
        },
        variable: {
            exactNames: ['购物','娱乐','餐饮','外卖','服饰','网购','消费'],
            exactCodes: ['SHOPPING','ENTERTAINMENT','FOOD','DINING','CONSUME'],
            fallback: ['variable','shopping','entertainment','food','dining','购物','娱乐','餐饮','外卖','服饰','网购']
        }
    };

    function getQueryParam(name){
        var re = new RegExp('[?&]' + name + '=([^&]+)');
        var m = re.exec(window.location.search || '');
        if(!m || !m[1]) return '';
        try{ return decodeURIComponent(m[1]); }catch(e){ return m[1]; }
    }

    function hasKw(text, code, rule){
        var t = String(text || '').toLowerCase();
        var c = String(code || '').toLowerCase();
        var i;
        for(i=0;i<(rule.exactNames||[]).length;i++){
            var en = String(rule.exactNames[i] || '').toLowerCase();
            if(en && (t === en || t.indexOf(en) >= 0)) return true;
        }
        for(i=0;i<(rule.exactCodes||[]).length;i++){
            var ec = String(rule.exactCodes[i] || '').toLowerCase();
            if(ec && (c === ec || c.indexOf(ec) === 0)) return true;
        }
        for(i=0;i<(rule.fallback||[]).length;i++){
            var fb = String(rule.fallback[i] || '').toLowerCase();
            if(fb && (t.indexOf(fb) >= 0 || c.indexOf(fb) >= 0)) return true;
        }
        return false;
    }

    function collectAllIds(node, out){
        if(!node) return;
        var id = node.id || node.code || node.value;
        if(id) out.push(String(id));
        var ch = node.children || node.childrens || [];
        for(var i=0;i<ch.length;i++){ collectAllIds(ch[i], out); }
    }

    function debounce(fn, wait){
        var timer = null;
        return function(){
            var args = arguments;
            if(timer){ clearTimeout(timer); }
            timer = setTimeout(function(){ fn.apply(null, args); }, wait || 120);
        };
    }

    function sameValues(a, b){
        if(!a || !b) return false;
        if(a.length !== b.length) return false;
        var i;
        var sa = a.slice().sort();
        var sb = b.slice().sort();
        for(i=0;i<sa.length;i++){
            if(String(sa[i]) !== String(sb[i])) return false;
        }
        return true;
    }

    function autoSelectConsume(cmbConsumeSelector, focus, onApplied){
        if(!focus) return;
        var rule = ruleMap[focus];
        if(!rule || !cmbConsumeSelector) return;
        var notify = (typeof onApplied === 'function') ? debounce(onApplied, 160) : null;
        $.get('/api/v1/consume/tree', function(nodes){
            try{
                var topNodes = [];
                var leafIds = [];
                function walk(arr, depth){
                    if(!arr || !arr.length) return;
                    for(var i=0;i<arr.length;i++){
                        var n = arr[i] || {};
                        var txt = n.text || n.name || n.label || '';
                        var code = n.code || '';
                        var hit = hasKw(txt, code, rule);
                        if(hit && depth <= 1){
                            topNodes.push(n);
                        }else if(hit){
                            var nid = n.id || n.code || n.value;
                            if(nid) leafIds.push(String(nid));
                        }
                        walk(n.children || n.childrens || [], depth + 1);
                    }
                }
                walk(nodes || [], 0);

                var ids = [];
                if(topNodes.length){
                    for(var t=0;t<topNodes.length;t++){ collectAllIds(topNodes[t], ids); }
                }else{
                    ids = leafIds.slice();
                }
                var uniq = [];
                var seen = {};
                for(var j=0;j<ids.length;j++){
                    var idv = ids[j];
                    if(!seen[idv]){ seen[idv] = true; uniq.push(idv); }
                }
                if(uniq.length){
                    var next = uniq.slice(0, 12);
                    var old = [];
                    try{ old = $(cmbConsumeSelector).combotree('getValues') || []; }catch(ignore){}
                    var changed = !sameValues(old, next);
                    if(changed){
                        $(cmbConsumeSelector).combotree('setValues', next);
                    }
                    if(notify){
                        try{ notify({ changed: changed, values: next }); }catch(e){}
                    }
                }
            }catch(e){}
        });
    }

    function applyHint(afterSelector, focus){
        if(!focus) return;
        var text = hintMap[focus] || ('Focus: ' + focus);
        if($('#focusHint').length) return;
        var style = 'margin:6px 10px 0;padding:6px 10px;border:1px solid #dbeafe;background:#eff6ff;color:#1e40af;border-radius:6px;font-size:12px;';
        if(afterSelector === '.filter-bar'){
            style = 'margin:6px 0 0;padding:6px 10px;border:1px solid #dbeafe;background:#eff6ff;color:#1e40af;border-radius:6px;font-size:12px;';
        }
        $('<div id="focusHint" style="' + style + '">' + text + '</div>').insertAfter(afterSelector);
    }

    function applyToPage(options){
        options = options || {};
        var focus = getQueryParam('focus');
        if(!focus) return;
        applyHint(options.afterSelector || '.top', focus);
        autoSelectConsume(
            options.cmbConsumeSelector || '#cmbConsume',
            focus,
            options.onFilterApplied
        );
    }

    window.reportFocus = {
        getFocus: function(){ return getQueryParam('focus'); },
        applyToPage: applyToPage
    };
})(window, jQuery);
