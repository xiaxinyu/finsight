var app = {
	messager : {
		show : function(obj) {
			var initParams = {
				timeout : 2000,
				showType : 'fade',
				style : {
					right : '',
					bottom : ''
				}
			};
			var finalParams = $.extend(initParams, obj);
			$.messager.show(finalParams);
		},
        success : function(msg){
            app.messager.show({ title:'Success', msg: msg || 'Operation succeeded.' });
        },
        fail : function(msg){
            app.messager.show({ title:'Fail', msg: msg || 'Operation failed.' });
        },
        error : function(msg){
            app.messager.show({ title:'Error', msg: msg || 'Unexpected error.' });
		}
	},

	date : {
		format : function(value, row, index) {
			function parse(v){
				if(!v) return null;
				if(v instanceof Date) return v;
				if(typeof v==='number') return new Date(v);
				if(typeof v==='string'){
					var d = new Date(v.replace(/-/g,'/'));
					if(isNaN(d.getTime())){
						var m = /^(\d{4})-(\d{1,2})-(\d{1,2})/.exec(v);
						if(m) return new Date(+m[1], +m[2]-1, +m[3]);
					}
					return d;
				}
				return null;
			}
			var d = parse(value);
			return d ? d.format('yyyy-MM-dd') : '';
		}
	},

	money : {
		rmb : function(value, row, index) {
			var n = Number(value);
			if(isNaN(n)) return '';
			return '<span style="font-size:12px;font-family:\'Times New Roman\';margin-right:2px;">¥</span><span class="fs-money-num">' + n.toFixed(2).replace(/\B(?=(\d{3})+(?!\d))/g, ',') + '</span>';
		},
		numberOnly : function(value) {
			var n = Number(value);
			if(isNaN(n)) return '';
			return '<span class="fs-money-num">' + n.toFixed(2).replace(/\B(?=(\d{3})+(?!\d))/g, ',') + '</span>';
		},
		deltaPercent : function(value) {
			var n = Number(value);
			if(isNaN(n)) return '';
			var cls = n < 0 ? 'fs-delta-negative' : (n > 0 ? 'fs-delta-positive' : 'fs-delta-neutral');
			var sign = n > 0 ? '+' : '';
			return '<span class="' + cls + ' fs-money-num">' + sign + n.toFixed(1) + '%</span>';
		}
	},
	
	login : function(params,sfn,efn){
		$.ajax({
			   type:"post",
	           url: app_url+"/application/login",
	           data:JSON.stringify(params),
	           contentType:'application/json',
	           dataType:'json',
	           success: sfn,
	           error: efn
	       });
		},

    api : {
        isWrappedResponse : function(obj){
            // Only treat as "wrapped" when it clearly matches our result envelope.
            // This avoids false positives for normal domain objects that may have a "code" field.
            if(!obj || typeof obj !== 'object'){ return false; }
            var hasCode = (typeof obj.code !== 'undefined');
            var hasReturnCode = (typeof obj.returnCode !== 'undefined');
            if(hasCode){
                // Typical envelope: { code, message, data }
                return (typeof obj.message !== 'undefined') || (typeof obj.data !== 'undefined');
            }
            if(hasReturnCode){
                // Legacy envelope: { returnCode, returnMessage }
                return (typeof obj.returnMessage !== 'undefined') || (typeof obj.message !== 'undefined');
            }
            return false;
        },
        normalizeResult : function(obj){
            if(!obj || typeof obj !== 'object'){
                return { wrapped: false, ok: true, code: 20000, message: '', data: obj, raw: obj };
            }
            if(typeof obj.code !== 'undefined'){
                return {
                    wrapped: true,
                    ok: Number(obj.code) === 20000 || Number(obj.code) === 200,
                    code: Number(obj.code),
                    message: obj.message || '',
                    data: typeof obj.data === 'undefined' ? null : obj.data,
                    raw: obj
                };
            }
            if(typeof obj.returnCode !== 'undefined'){
                return {
                    wrapped: true,
                    ok: obj.returnCode === 'success',
                    code: obj.returnCode === 'success' ? 20000 : 50000,
                    message: obj.returnMessage || '',
                    data: obj.returnMessage,
                    raw: obj
                };
            }
            return { wrapped: false, ok: true, code: 20000, message: '', data: obj, raw: obj };
        }
    }
}

$(function(){
    $(document).ajaxSuccess(function(event, xhr){
        try{
            var ct = xhr.getResponseHeader ? xhr.getResponseHeader('Content-Type') : '';
            var st = xhr.status;
            var rt = xhr.responseText || '';
            if(st === 200 && ct && ct.indexOf('application/json') >= 0){
                try{
                    var obj = JSON.parse(rt);
                    if(obj && typeof obj === 'object'){
                        if(!app.api.isWrappedResponse(obj)){
                            return;
                        }
                        var n = app.api.normalizeResult(obj);
                        if(n.ok){
                            return;
                        }
                        if(n.code === 40000 || n.code === 401){
                            app.messager.show({title:'Error', msg:n.message || '未授权，请登录'});
                            window.top.location.href = '/login.html';
                        }else{
                            app.messager.show({title:'Error', msg:n.message || '操作失败'});
                        }
                    }
                }catch(e){}
            }
        }catch(e){}
    });
    $(document).ajaxComplete(function(event, xhr){
        try{
            var ct = xhr.getResponseHeader ? xhr.getResponseHeader('Content-Type') : '';
            var st = xhr.status;
            var rt = xhr.responseText || '';
            if(st === 401 || st === 403){
                window.top.location.href = '/login.html';
                return;
            }
            if((ct && ct.indexOf('text/html') >= 0) && (rt.indexOf('id=\"login-form\"') >= 0)){
                window.top.location.href = '/login.html';
            }
        }catch(e){}
    });
    $(document).ajaxError(function(event, xhr, settings, thrownError){
        try{
            var st = xhr.status;
            var rt = xhr.responseText || '';
            /* Aborted (e.g. superseded request) — status 0; do not toast */
            if(st === 0 && (xhr.statusText === 'abort' || thrownError === 'abort')){
                return;
            }
            if(st === 401 || st === 403 || rt.indexOf('id=\"login-form\"') >= 0){
                window.top.location.href = '/login.html';
                return;
            }
            var msg = 'HTTP-' + st;
            try{
                var ct = xhr.getResponseHeader ? xhr.getResponseHeader('Content-Type') : '';
                if(ct && ct.indexOf('application/json') >= 0){
                    var obj = JSON.parse(rt || '{}');
                    if(obj){
                        var n = app.api.normalizeResult(obj);
                        msg = n.message || ('ERR-' + n.code);
                    }
                } else {
                    if(st >= 500){ msg = 'HTTP-' + st; }
                }
            }catch(e){}
            app.messager.show({title:'Error', msg: msg});
        }catch(e){}
    });
});
