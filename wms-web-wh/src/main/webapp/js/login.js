layui.use(['form', 'layer', 'jquery'], function () {
    var form = layui.form,
        layer = parent.layer === undefined ? layui.layer : top.layer
    $ = layui.jquery;

    $(".loginBody .seraph").click(function () {
        layer.msg("这只是做个样式，至于功能，你见过哪个后台能这样登录的？还是老老实实的找管理员去注册吧", {
            time: 5000
        });
    })

    //登录按钮
    form.on("submit(login)", function (data) {
        var index = layer.load(2);
        $.ajax({
            url: contextPath + '/login',
            type: 'post',
            dataType: 'json',
            data: data.field,
            success: function (data) {
                layer.close(index);
                if (data.code == 0) {

                    //用户登录后保存AccessToken
                    window.sessionStorage.setItem("accessToken", data.data.accessToken);
                    window.sessionStorage.setItem("menus", data.data.menus);
                    window.sessionStorage.setItem("user", JSON.stringify(data.data.user));
                    window.sessionStorage.setItem("version", JSON.stringify(data.data.version));

                    //本地存储数据字典
                    var wmsInitData = data.data.wmsInitData;
                    window.sessionStorage.setItem("deptList", JSON.stringify(wmsInitData.deptList));
                    window.sessionStorage.setItem("cabinetList", JSON.stringify(wmsInitData.cabinetList));
                    window.sessionStorage.setItem("userList", JSON.stringify(wmsInitData.userList));
                    window.sessionStorage.setItem("goodsList", JSON.stringify(wmsInitData.goodsList));
                    window.sessionStorage.setItem("manufacturerList", JSON.stringify(wmsInitData.manufacturerList));
                    window.sessionStorage.setItem("supplierList", JSON.stringify(wmsInitData.supplierList));

                    var loginState = data.data.loginState;
                    if (loginState == true) {
                        layer.msg('该账户已登录', { icon: 7, time: 1000, shade: [0.6, '#000', true] });

                        setTimeout(function () {
                            window.location.href = "index.html";
                        }, 1000);
                    } else {
                        window.location.href = "index.html";
                    }
                } else {
                    layer.msg(data.msg);
                }
            }
        });
        return false;
    })

    //表单输入效果
    $(".loginBody .input-item").click(function (e) {
        e.stopPropagation();
        $(this).addClass("layui-input-focus").find(".layui-input").focus();
    })
    $(".loginBody .layui-form-item .layui-input").focus(function () {
        $(this).parent().addClass("layui-input-focus");
    })
    $(".loginBody .layui-form-item .layui-input").blur(function () {
        $(this).parent().removeClass("layui-input-focus");
        if ($(this).val() != '') {
            $(this).parent().addClass("layui-input-active");
        } else {
            $(this).parent().removeClass("layui-input-active");
        }
    })

    //显示欢迎信息
    $(".welcome").html("欢迎使用医疗耗材管理系统<br><br><span style='color:blue'>请联系管理员添加账户</span>");


    //初始化用户名密码
    function initLoginBody() {
        $(".loginBody .layui-form-item .layui-input")[0].value = 'admin';
        $(".loginBody .layui-form-item .layui-input")[1].value = '123456';
        $(".loginBody .layui-form-item .layui-input").blur();
    }

    // initLoginBody();

})
