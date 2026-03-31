var transaction = {
	deleteTransaction : function(data, sfn, efn) {
		$.ajax({
			type : "post",
			url : "/transaction/delete",
			data : data,
			success : sfn,
			error : efn,
			dataType : 'json'
		});
	},
	updateTransaction : function(params, sfn, efn) {
		$.ajax({
		   type:"post",
           url: "/transaction/update",
           data:params,
           dataType:'json',
           success: sfn,
           error: efn
       });
	},
	incomeToExpense : function(params, sfn, efn) {
		$.ajax({
		   type:"post",
           url: "/transaction/income-to-expense",
           data:params,
           dataType:'json',
           success: sfn,
           error: efn
       });
	},
	expenseToIncome : function(params, sfn, efn) {
		$.ajax({
		   type:"post",
           url: "/transaction/expense-to-income",
           data:params,
           dataType:'json',
           success: sfn,
           error: efn
       });
	},
	consumeReport : function(params, sfn, efn){
		$.ajax({
		   type:"post",
           url: "/transaction-report/consume",
           data:params,
           dataType:'json',
           success: sfn,
           error: efn
       });
	},
	weekConsumeReport : function(params, sfn, efn){
		$.ajax({
		   type:"post",
           url: "/transaction-report/week-consume",
           data:params,
           dataType:'json',
           success: sfn,
           error: efn
       });
	},
	monthConsumeReport : function(params, sfn, efn){
		$.ajax({
			   type:"post",
	           url: "/transaction-report/month-consume",
	           data:params,
	           dataType:'json',
	           success: sfn,
	           error: efn
	       });
	},
	monthIncomeReport : function(params, sfn, efn){
		$.ajax({
			   type:"post",
	           url: "/transaction-report/month-income",
	           data:params,
	           dataType:'json',
	           success: sfn,
	           error: efn
	       });
	},
	homeSummary : function(year, sfn, efn){
		$.ajax({
			type: 'get',
			url: '/transaction-report/home-summary?year=' + encodeURIComponent(year),
			dataType: 'json',
			success: sfn,
			error: efn
		});
	}
		,
		classify : function(params, sfn, efn){
			$.ajax({
			   type:"post",
	           url: "/transaction/classify",
	           data:params,
	           dataType:'json',
	           success: sfn,
	           error: efn
	       });
		}
		,
		keywords : function(params, sfn, efn){
			$.ajax({
				type:"post",
				url:"/transaction/keywords",
				data:params,
				dataType:'json',
				success:sfn,
				error: efn
			});
		}
		,
		updateTransactionsBatch : function(transactions, sfn, efn){
			$.ajax({
				type:"post",
				url:"/transaction/update-batch",
				data:JSON.stringify(transactions||[]),
				contentType:"application/json; charset=UTF-8",
				dataType:"json",
				success:sfn,
				error: efn
			});
		}
		,
		rules_add : function(rule, sfn, efn){
			$.ajax({
				type:"post",
				url:"/api/v1/consume/rules",
				data:JSON.stringify(rule),
				contentType:"application/json; charset=UTF-8",
				dataType:"json",
				success:sfn,
				error: efn
			});
		}
		,
		rules_reload : function(sfn, efn){
			$.ajax({
				type:"post",
				url:"/api/v1/consume/rules/reload",
				dataType:"text",
				success:sfn,
				error: efn
			});
		}
};
