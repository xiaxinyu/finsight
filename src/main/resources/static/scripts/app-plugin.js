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
			return '<span style="font-size:12px;font-family:\'Times New Roman\';margin-right:2px;">¥</span><span>' + n.toFixed(2) + '</span>';
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
                        var bizCode = obj.code;
                        var retCode = obj.returnCode;
                        // 只展示错误代码，不展示详细信息
                        if(typeof bizCode !== 'undefined'){
                            if(bizCode === 30000){
                                $.messager.alert('Error', 'ERR-' + bizCode, 'error');
                            } else if(bizCode === 40000){
                                $.messager.alert('Error', 'ERR-' + bizCode, 'warning');
                            }
                        } else if(retCode === 'fail'){
                            $.messager.alert('Error', 'BIZ-FAIL', 'error');
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
            // 在 ajaxSuccess 已经处理业务错误，这里不再重复弹出内容
        }catch(e){}
    });
    $(document).ajaxError(function(event, xhr){
        try{
            var st = xhr.status;
            var rt = xhr.responseText || '';
            if(st === 401 || st === 403 || rt.indexOf('id=\"login-form\"') >= 0){
                window.top.location.href = '/login.html';
                return;
            }
            // 只展示错误代码
            var msg = 'HTTP-' + st;
            try{
                var ct = xhr.getResponseHeader ? xhr.getResponseHeader('Content-Type') : '';
                if(ct && ct.indexOf('application/json') >= 0){
                    var obj = JSON.parse(rt || '{}');
                    if(obj){
                        if(typeof obj.code !== 'undefined'){ msg = 'ERR-' + obj.code; }
                        else if(obj.returnCode === 'fail'){ msg = 'BIZ-FAIL'; }
                    }
                } else {
                    if(st >= 500){ msg = 'HTTP-' + st; }
                }
            }catch(e){}
            $.messager.alert('Error', msg, 'error');
        }catch(e){}
    });
});
