var $, tab, dataStr, layer;
layui.config({
	base: "js/"
}).extend({
	"bodyTab": "bodyTab"
})
layui.use(['bodyTab', 'form', 'element', 'layer', 'jquery'], function () {
	var form = layui.form,
		element = layui.element;
	$ = layui.$;
	layer = parent.layer === undefined ? layui.layer : top.layer;
	tab = layui.bodyTab({
		openTabNum: "50",  //最大可打开窗口数量
		url: "json/navs.json" //获取菜单json地址
	});

	//显示当前登录用户
	$(".adminName").html(getCurrentEmployee().name);

	// 窗口自适应    
	// $(window).on('resize', function () {
	// 	AdminInit();
	// 	// iframe窗口自适应
	// 	var $content = $('#nav_xbs_tab .layui-tab-content');
	// 	$content.height($(this).height() - 125);
	// 	$content.find('iframe').each(function () {
	// 		$(this).height($content.height());
	// 	});
	// }).resize();

	// function AdminInit() {
	// 	//layui-fluid 为外层div
	// 	$('.layui-fluid').height($(window).height());
	// 	$('body').height($(window).height());
	// }

	//通过顶部菜单获取左侧二三级菜单   注：此处只做演示之用，实际开发中通过接口传参的方式获取导航数据
	function getData(json) {
		$.getJSON(contextPath + "/sys/getMenuTree" + getAccessTokenUrl(), function (data) {
			if (json == "levelOne") {
				dataStr = data.data.levelOne;
			} else if (json == "levelTwo") {
				dataStr = data.data.levelTwo;
			} else if (json == "sys") {
				dataStr = data.data.sys;
			}
			tab.render();
		})
	}

	//页面加载时判断左侧菜单是否显示
	//通过顶部菜单获取左侧菜单
	$(".topLevelMenus li,.mobileTopLevelMenus dd").click(function () {
		if ($(this).parents(".mobileTopLevelMenus").length != "0") {
			$(".topLevelMenus li").eq($(this).index()).addClass("layui-this").siblings().removeClass("layui-this");
		} else {
			$(".mobileTopLevelMenus dd").eq($(this).index()).addClass("layui-this").siblings().removeClass("layui-this");
		}
		$(".layui-layout-admin").removeClass("showMenu");
		$("body").addClass("site-mobile");
		getData($(this).data("menu"));
		//渲染顶部窗口
		tab.tabMove();
	})

	//隐藏左侧导航
	$(".hideMenu").click(function () {
		if ($(".topLevelMenus li.layui-this a").data("url")) {
			layer.msg("此栏目状态下左侧菜单不可展开");  //主要为了避免左侧显示的内容与顶部菜单不匹配
			return false;
		}
		$(".layui-layout-admin").toggleClass("showMenu");
		//渲染顶部窗口
		tab.tabMove();
	})

	//手机设备的简单适配
	$('.site-tree-mobile').on('click', function () {
		$('body').addClass('site-mobile');
	});
	$('.site-mobile-shade').on('click', function () {
		$('body').removeClass('site-mobile');
	});

	// 添加新窗口
	$("body").on("click", ".layui-nav .layui-nav-item a:not('.mobileTopLevelMenus .layui-nav-item a')", function () {
		//如果不存在子级
		if ($(this).siblings().length == 0) {
			addTab($(this));
			$('body').removeClass('site-mobile');  //移动端点击菜单关闭菜单层
		}
		$(this).parent("li").siblings().removeClass("layui-nav-itemed");
	})

	//刷新后还原打开的窗口
	if (cacheStr == "true") {
		if (window.sessionStorage.getItem("menu") != null) {
			menu = JSON.parse(window.sessionStorage.getItem("menu"));
			curmenu = window.sessionStorage.getItem("curmenu");
			var openTitle = '';
			for (var i = 0; i < menu.length; i++) {
				openTitle = '';
				if (menu[i].icon) {
					if (menu[i].icon.split("-")[0] == 'icon') {
						openTitle += '<i class="seraph ' + menu[i].icon + '"></i>';
					} else {
						openTitle += '<i class="layui-icon">' + menu[i].icon + '</i>';
					}
				}
				openTitle += '<cite>' + menu[i].title + '</cite>';
				openTitle += '<i class="layui-icon layui-unselect layui-tab-close" data-id="' + menu[i].layId + '">&#x1006;</i>';
				element.tabAdd("bodyTab", {
					title: openTitle,
					content: "<iframe src='" + menu[i].href + "' data-id='" + menu[i].layId + "'></frame>",
					id: menu[i].layId
				})
				//定位到刷新前的窗口
				if (curmenu != "undefined") {
					if (curmenu == '' || curmenu == "null") {  //定位到系统主页面
						element.tabChange("bodyTab", '');
					} else if (JSON.parse(curmenu).title == menu[i].title) {  //定位到刷新前的页面
						element.tabChange("bodyTab", menu[i].layId);
					}
				} else {
					element.tabChange("bodyTab", menu[menu.length - 1].layId);
				}
			}
			//渲染顶部窗口
			tab.tabMove();
		}
	} else {
		window.sessionStorage.removeItem("menu");
		window.sessionStorage.removeItem("curmenu");
	}

	//通过顶部菜单获取左侧二三级菜单
	var menus = window.sessionStorage.getItem('menus').split(',');
	if (menus.indexOf("levelOne") == -1) {
		$(".levelOne").hide();
	}
	if (menus.indexOf("levelTwo") == -1) {
		$(".levelTwo").hide();
	}
	if (menus.indexOf("sys") == -1) {
		$(".sys").hide();
	}
	getData(menus[0]);
})

//打开新窗口
function addTab(_this) {
	tab.tabAdd(_this);
}

//联系作者
function contact() {
	layer.open({
		type: 1
		, title: false //不显示标题栏
		, closeBtn: false
		, area: '500px;'
		, shade: 0.8
		, id: 'LAY_layuipro' //设定一个id，防止重复弹出
		, resize: false
		, btn: ['访问官网', '不感兴趣']
		, btnAlign: 'c'
		, moveType: 1 //拖拽模式，0或者1
		, content: '<div style="padding: 50px; line-height: 22px; background-color: #393D49; color: #fff; font-weight: 300;">'
			+ '<i class="layui-icon">&#xe677;</i>   地址：南京经济技术开发区恒达路3号高通科创基地<br><br>'
			+ '<i class="layui-icon">&#xe676;</i>   电话：025-83126066<br><br>'
			+ '<i class="layui-icon">&#xe675;</i>   邮箱：info@graphenesecurity.co<br><br>'
		, success: function (layero) {
			var btn = layero.find('.layui-layer-btn');
			btn.find('.layui-layer-btn0').attr({
				href: 'http://bjtiot.com/'
				, target: '_blank'
			});
		}
	});
}

//图片管理弹窗
function showImg() {
	$.getJSON('json/images.json', function (json) {
		var res = json;
		layer.photos({
			photos: res,
			anim: 5,
			closeBtn: 1
		});
	});
}

function showImg(title, photos) {
	layer.photos({
		shade: [0.85, '#000'],
		photos: { "title": title, "data": photos },
		anim: 5,
		closeBtn: 1
	});

}

//判断当前页面是否已登录
function checkLoginState() {
	if (window.sessionStorage.getItem("user") == null) {
		window.location.href = contextPath;
	}
}

checkLoginState();