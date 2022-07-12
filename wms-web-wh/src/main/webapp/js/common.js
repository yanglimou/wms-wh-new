//根据localstorage的数据字典初始化下拉框
function renderSelectOptions(data, settings, name) {
    settings = settings || {};
    var valueField = settings.valueField || 'value',
        textField = settings.textField || 'text',
        selectedValue = settings.selectedValue || "",
        captionField = settings.captionField || "";
    var html = [];

    //插入空白选项
    html.push('<option value="">选择' + name + '</option>');

    for (var i = 0, item; i < data.length; i++) {
        item = data[i];
        html.push('<option value="');
        html.push(item[valueField]);
        html.push('"');
        if (selectedValue && item[valueField] == selectedValue) {
            html.push(' selected="selected"');
        }
        html.push('>');
        html.push(item[textField]);
        if (captionField != "") {
            html.push(" " + item[captionField]);
        }

        html.push('</option>');
    }

    return html.join('');
}

//根据数据字典翻译ID为名称
function renderItemName(data, id) {
    return renderItem(data, id, 'name');
}

//根据数据字典翻译ID为对应的属性
function renderItem(data, id, field) {
    if (id == null || id == "") {
        return '';
    }

    for (var i = 0, item; i < data.length; i++) {
        item = data[i];

        if (item.id == id) {
            return item[field];
        }
    }

    return '未知';
}

//获取URL 参数
function getQueryString(name) {
    var reg = new RegExp('(^|&)' + name + '=([^&]*)(&|$)', 'i');
    var r = window.location.search.substr(1).match(reg);
    if (r != null) {
        return unescape(r[2]);
    }
    return null;
}

//获取contextpath
function getContextPath() {
    var pathName = document.location.pathname;
    var index = pathName.substr(1).indexOf("/");
    var result = pathName.substr(0, index + 1);
    return result;
}

//获取完整contextpath
function getFullContextPath() {
    var href = document.location.href;
    var index = href.indexOf(contextPath);
    var result = href.substr(0, index + contextPath.length);
    return result;
}

/**
 * 全局contextPath
 */
var contextPath = getContextPath();
var fullContextPath = getFullContextPath();

function outputmoney(number) {
    if (isNaN(number) || number == "") return "";
    number = Math.round(number * 100) / 100;
    if (number < 0)
        return '-' + outputdollars(Math.floor(Math.abs(number) - 0) + '') + outputcents(Math.abs(number) - 0);
    else
        return outputdollars(Math.floor(number - 0) + '') + outputcents(number - 0);
}
//格式化金额 
function outputdollars(number) {
    if (number.length <= 3)
        return (number == '' ? '0' : number);
    else {
        var mod = number.length % 3;
        var output = (mod == 0 ? '' : (number.substring(0, mod)));
        for (i = 0; i < Math.floor(number.length / 3); i++) {
            if ((mod == 0) && (i == 0))
                output += number.substring(mod + 3 * i, mod + 3 * i + 3);
            else
                output += ',' + number.substring(mod + 3 * i, mod + 3 * i + 3);
        }
        return (output);
    }
}

function outputcents(amount) {
    amount = Math.round(((amount) - Math.floor(amount)) * 100);
    return (amount < 10 ? '.0' + amount : '.' + amount);
}

function getAccessTokenUrl() {
    return "?accessToken=" + window.sessionStorage.getItem("accessToken");
}

function getCurrentEmployee() {
    return getSessionStorage("user");
}

function getSessionStorage(item) {
    var obj = eval('(' + window.sessionStorage.getItem(item) + ')');
    return obj;
}

/**************************************时间格式化处理************************************/
function dateFtt(fmt, date) { //author: meizz 
    var o = {
        "M+": date.getMonth() + 1,     //月份 
        "d+": date.getDate(),     //日 
        "h+": date.getHours(),     //小时 
        "m+": date.getMinutes(),     //分 
        "s+": date.getSeconds(),     //秒 
        "q+": Math.floor((date.getMonth() + 3) / 3), //季度 
        "S": date.getMilliseconds()    //毫秒 
    };
    if (/(y+)/.test(fmt))
        fmt = fmt.replace(RegExp.$1, (date.getFullYear() + "").substr(4 - RegExp.$1.length));
    for (var k in o)
        if (new RegExp("(" + k + ")").test(fmt))
            fmt = fmt.replace(RegExp.$1, (RegExp.$1.length == 1) ? (o[k]) : (("00" + o[k]).substr(("" + o[k]).length)));
    return fmt;
}

//弹出新增或编辑窗口()
function openDialog(edit, title, content) {
    openDialogByArea(edit, title, content, '600px', '450px');
}

//弹出新增或编辑窗口
function openDialogByArea(edit, title, content, width, height) {
    layui.layer.open({
        // title: [title, 'color:#fff;background-color:#393D49;'],
        title: title,
        type: 2,
        area: [width, height],
        content: content,
        success: function (layero, index) {
            var body = layui.layer.getChildFrame('body', index);
            var iframeWin = window[layero.find('iframe')[0]['name']];
            iframeWin.initData(edit);
        }
    });
}

//获取本月开始时间和结束时间
function getDateRange()
{
    var firstDate = new Date();
        var startDate = firstDate.getFullYear()+"-"+((firstDate.getMonth()+1)<10?"0":"")+(firstDate.getMonth()+1)+"-"+"01";
        //alert(firstDate.getFullYear()+"-"+((firstDate.getMonth()+1)<10?"0":"")+(firstDate.getMonth()+1)+"-"+"01");
        var date=new Date();
        var currentMonth=date.getMonth();
        var nextMonth=++currentMonth;
        var nextMonthFirstDay=new Date(date.getFullYear(),nextMonth,1);
        var oneDay=1000*60*60*24;
        var lastDate =  new Date(nextMonthFirstDay-oneDay);
        var  endDate = lastDate.getFullYear()+"-"+((lastDate.getMonth()+1)<10?"0":"")+(lastDate.getMonth()+1)+"-"+(lastDate.getDate()<10?"0":"")+lastDate.getDate();
        //alert(lastDate.getFullYear()+"-"+((lastDate.getMonth()+1)<10?"0":"")+(lastDate.getMonth()+1)+"-"+(lastDate.getDate()<10?"0":"")+lastDate.getDate());
        return startDate+' - '+endDate;
}