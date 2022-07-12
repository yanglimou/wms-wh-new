package com.tsj.web.common;

import com.alibaba.druid.filter.stat.StatFilter;
import com.alibaba.druid.util.JdbcConstants;
import com.alibaba.druid.wall.WallFilter;
import com.jfinal.aop.Aop;
import com.jfinal.config.*;
import com.jfinal.core.Const;
import com.jfinal.kit.PropKit;
import com.jfinal.log.Log;
import com.jfinal.plugin.activerecord.ActiveRecordPlugin;
import com.jfinal.plugin.activerecord.dialect.MysqlDialect;
import com.jfinal.plugin.cron4j.Cron4jPlugin;
import com.jfinal.plugin.druid.DruidPlugin;
import com.jfinal.plugin.druid.DruidStatViewHandler;
import com.jfinal.plugin.ehcache.EhCachePlugin;
import com.jfinal.template.Engine;
import com.tsj.common.config.CommonConfig;
import com.tsj.common.constant.FileConstant;
import com.tsj.common.druid.DruidConfig;
import com.tsj.common.log.LogBackLogFactory;
import com.tsj.common.threadpool.ThreadPoolPlugin;
import com.tsj.domain.model.Dept;
import com.tsj.domain.model._MappingKit;
import com.tsj.service.CacheService;
import com.tsj.service.interceptor.AuthInterceptor;
import com.tsj.service.interceptor.EmptyInterceptor;
import com.tsj.service.interceptor.OperateLogInterceptor;
import com.tsj.service.spdStockTag.SpdStockTagContainer;
import com.tsj.tcp.tcpserver.TcpServer;
import com.tsj.web.controller.*;
import com.tsj.web.controller.api.ApiBaseController;
import com.tsj.web.controller.api.ApiComController;
import com.tsj.web.controller.api.ApiSysController;
import com.tsj.web.controller.api.DevelopmentToolsController;
import net.dreamlu.event.EventPlugin;
import org.apache.commons.lang3.StringUtils;

/**
 * @className: CoreConfig
 * @description: API 引导式配置
 * @author: Frank
 * @create: 2020-03-24 10:49
 */
public class MyConfig extends JFinalConfig {
    public static final Log logger = Log.getLog(MyConfig.class);

    /**
     * 配置常量
     */
    @Override
    public void configConstant(Constants me) {
        /**
         * PropKit.useFirstFound(...) 使用参数中从左到右最先被找到的配置文件
         * 从左到右依次去找配置，找到则立即加载并立即返回，后续配置将被忽略
         */
        if (CommonConfig.prop == null) {
            CommonConfig.prop = PropKit.useFirstFound("config/jfinal.txt");
        }

        me.setDevMode(CommonConfig.prop.getBoolean("jfinal.devMode", false));

        /**
         * 支持 Controller、Interceptor、Validator 之中使用 @Inject 注入业务层，并且自动实现 AOP
         * 注入动作支持任意深度并自动处理循环注入
         */
        me.setInjectDependency(true);

        // 配置对超类中的属性进行注入
        me.setInjectSuperClass(true);

        me.setBaseUploadPath(FileConstant.TEMP_PATH);
        me.setBaseDownloadPath(FileConstant.TEMP_PATH);

        me.setEncoding(Const.DEFAULT_ENCODING);
        //默认10M,此处设置为最大100M
        me.setMaxPostSize(100 * Const.DEFAULT_MAX_POST_SIZE);

        //配置jfinal日志采用logback
        me.setLogFactory(new LogBackLogFactory());

        me.setError404View("/page/404.html");
    }

    /**
     * 配置路由
     */
    @Override
    public void configRoute(Routes me) {
        me.add("/", IndexController.class);
        me.add("/base", BaseController.class);
        me.add("/com", ComController.class);
        me.add("/sys", SysController.class);
        me.add("/file", FileController.class);
        me.add("/print", PrintController.class);

        me.add("/api/sys", ApiSysController.class);
        me.add("/api/com", ApiComController.class);
        me.add("/api/developmentTools", DevelopmentToolsController.class);
        me.add("/api/base", ApiBaseController.class);
    }

    @Override
    public void configEngine(Engine me) {

    }

    /**
     * 配置插件
     */
    @Override
    public void configPlugin(Plugins me) {
        //配置数据库
        DruidPlugin druidPlugin = createWmsDruidPlugin();
        me.add(druidPlugin);

        ActiveRecordPlugin activeRecordPlugin = new ActiveRecordPlugin(druidPlugin);
        activeRecordPlugin.setDialect(new MysqlDialect());
        activeRecordPlugin.setShowSql(true);
        me.add(activeRecordPlugin);

        _MappingKit.mapping(activeRecordPlugin);

        //配置EhCachePlugin插件
        EhCachePlugin ehCachePlugin = new EhCachePlugin(getClass().getClassLoader().getResource("config/ehcache.xml"));
        me.add(ehCachePlugin);

        //配置Cron4jPlugin插件
        me.add(new Cron4jPlugin(PropKit.use("config/cron4j.txt")));

        //配置EventPlugin插件
        EventPlugin eventPlugin = new EventPlugin().async();
        eventPlugin.scanJar().scanPackage("com.tsj.service.event");
        me.add(eventPlugin);

        //配置ThreadPoolPlugin
        me.add(new ThreadPoolPlugin(8, 32, 1024, 10));
    }

    /**
     * 配置全局拦截器
     */
    @Override
    public void configInterceptor(Interceptors me) {
        me.add(new AuthInterceptor());
        me.add(new EmptyInterceptor());
        me.add(new OperateLogInterceptor());
    }

    /**
     * 配置处理器
     */
    @Override
    public void configHandler(Handlers me) {
        me.add(new DruidStatViewHandler("/druid", new DruidConfig()));
    }

    public static DruidPlugin createWmsDruidPlugin() {
        DruidPlugin druidPlugin = new DruidPlugin(CommonConfig.prop.get("wms.url"), CommonConfig.prop.get("wms.user"), CommonConfig.prop.get("wms.password"));
        druidPlugin.setInitialSize(1);
        druidPlugin.setMinIdle(1);
        druidPlugin.setMaxActive(2000);
        druidPlugin.setTimeBetweenEvictionRunsMillis(5000);
        druidPlugin.setValidationQuery("select 1");
        druidPlugin.setTimeBetweenEvictionRunsMillis(60000);
        druidPlugin.setMinEvictableIdleTimeMillis(30000);
        druidPlugin.setFilters("stat,wall");

        druidPlugin.addFilter(new StatFilter());

        WallFilter wallFilter = new WallFilter();
        wallFilter.setDbType(JdbcConstants.MYSQL);
        druidPlugin.addFilter(wallFilter);
        return druidPlugin;
    }


    private TcpServer tcpServer;
    @Override
    public void onStart() {
        super.onStart();

        //清空本地缓存
        CacheService cacheService = Aop.get(CacheService.class);
        cacheService.clearCache();

        //初始化本地缓存
        cacheService.initCache();

        //同步spd库存
        SpdStockTagContainer.syncSpd();

        //启动tcpServer
        tcpServer = new TcpServer(CommonConfig.prop.get("tcp.host"), CommonConfig.prop.getInt("tcp.port"));
        tcpServer.onStart();
        //连接海康摄像头
//        Enumeration<?> enu = CommonConfig.prop.getProperties().keys();
//        while (enu.hasMoreElements()) {
//            String key = enu.nextElement().toString();
//            if (key.startsWith("hk.")) {
//                String[] strings = CommonConfig.prop.get(key).split(";");
//
//                List<String> addressList = new ArrayList<>();
//                for (String string : strings) {
//                    String[] params = string.split(",");
//                    boolean res = HikVision.RealPlay(params[0], Short.parseShort(params[1]), params[2], params[3]);
//                    if (res) {
//                        addressList.add(params[0]);
//                    }
//                }
//
//                CommonConfig.cabinetCameraAddressMap.put(key.substring(3), addressList);
//            }
//        }

//        String deptNames = CommonConfig.prop.get("spd.deptName");
//        if (StringUtils.isNotEmpty(deptNames)) {
//            for (String deptName : deptNames.split(",")) {
//                Dept dept = cacheService.getDeptByName(deptName);
//                logger.info(deptName + "--" + dept.getId());
//            }
//        }
    }

    @Override
    public void onStop() {
        super.onStop();

        //关闭tcpServer
        tcpServer.onStop();
        //断开海康摄像头
//        HikVision.StopRealPlay();
    }
}
