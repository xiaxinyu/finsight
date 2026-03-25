var adminUi = (function(){
    var C = {
        confirmTitle: 'Confirm',
        confirmDeletionTitle: 'Confirm Deletion',
        confirmMigrationTitle: 'Confirm Migration',
        opFailed: 'Unable to complete the operation.',
        selectUserFirst: 'Please select a user first.',
        selectCardFirst: 'Please select a card first.',
        selectCardTypeFirst: 'Please select a card type from the left panel first.',
        selectCategoryFirst: 'Please select a category from the left panel first.',
        selectRuleFirst: 'Please select at least one rule.',
        userCreated: 'User created successfully.',
        userUpdated: 'User updated successfully.',
        userDeleted: 'User deleted successfully.',
        userRolesUpdated: 'User roles updated successfully.',
        cardSaved: 'Card saved successfully.',
        cardDeleted: 'Card deleted successfully.',
        categorySaved: 'Category saved successfully.',
        categoryDeleted: 'Category deleted successfully.',
        categoryMigrated: 'Category migration completed successfully.',
        ruleDeleted: 'Rule deleted successfully.',
        rulesCacheReloaded: 'Rule cache reloaded successfully.'
    };

    function fail(message){
        if(window.app && app.messager && typeof app.messager.fail === 'function'){
            app.messager.fail(message || C.opFailed);
        }else{
            $.messager.alert('Error', message || C.opFailed);
        }
    }

    function success(message){
        if(window.app && app.messager && typeof app.messager.success === 'function'){
            app.messager.success(message || 'Completed successfully.');
        }else{
            $.messager.alert('Success', message || 'Completed successfully.');
        }
    }

    function extractMessage(xhr, fallback){
        try{
            var raw = xhr && xhr.responseText ? xhr.responseText : '';
            if(raw){
                var obj = JSON.parse(raw);
                if(obj && window.app && app.api && app.api.normalizeResult){
                    var n = app.api.normalizeResult(obj);
                    if(n && n.message){ return n.message; }
                }
                if(obj && (obj.message || obj.returnMessage)){
                    return obj.message || obj.returnMessage;
                }
            }
        }catch(e){}
        return fallback || C.opFailed;
    }

    return {
        C: C,
        fail: fail,
        success: success,
        extractMessage: extractMessage
    };
})();
