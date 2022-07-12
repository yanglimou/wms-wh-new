package com.tsj.common.generator;

import com.jfinal.kit.PathKit;
import com.jfinal.kit.PropKit;
import com.jfinal.plugin.activerecord.generator.Generator;
import com.jfinal.plugin.activerecord.generator.MetaBuilder;
import com.jfinal.plugin.druid.DruidPlugin;

import javax.sql.DataSource;

public class _JFinalDemoGenerator {

    public static DataSource getDataSource() {
        PropKit.use("config/jfinal.txt");
        DruidPlugin druidPlugin = new DruidPlugin(PropKit.get("wms.url"), PropKit.get("wms.user"), PropKit.get("wms.password"));
        druidPlugin.start();
        return druidPlugin.getDataSource();
    }

    public static void main(String[] args) {
        // base model 所使用的包名
        String baseModelPackageName = "com.bjt.model.base";
        // base model 文件保存路径
        String baseModelOutputDir = PathKit.getWebRootPath() + "/src/main/java/com/bjt/model/base";

        // model 所使用的包名 (MappingKit 默认使用的包名)
        String modelPackageName = "com.bjt.model";
        // model 文件保存路径 (MappingKit 与 DataDictionary 文件默认保存路径)
        String modelOutputDir = baseModelOutputDir + "/..";


        DataSource dataSource = getDataSource();
        // 创建生成器
        Generator generator = new Generator(dataSource, baseModelPackageName, baseModelOutputDir, modelPackageName, modelOutputDir);
        //generator.setMetaBuilder(new _MetaBuilder(getDataSource()));
        // 设置是否生成链式 setter 方法
        generator.setGenerateChainSetter(false);
        // 添加不需要生成的表名
//        generator.addExcludedTable("sys_", "wms_");

        //定制过滤规则
        generator.setMetaBuilder(new _MetaBuilder(dataSource));

        // 设置是否在 Model 中生成 dao 对象
        generator.setGenerateDaoInModel(true);
        // 设置是否生成链式 setter 方法
        generator.setGenerateChainSetter(true);
        // 设置是否生成字典文件
        generator.setGenerateDataDictionary(false);
        // 设置需要被移除的表名前缀用于生成modelName。例如表名 "osc_user"，移除前缀 "osc_"后生成的model名为 "User"而非 OscUser
        generator.setRemovedTableNamePrefixes("base_", "com_", "sys_");

        // 生成
        generator.generate();
    }

    public static class _MetaBuilder extends MetaBuilder {
        public _MetaBuilder(DataSource dataSource) {
            super(dataSource);
        }

        @Override
        protected boolean isSkipTable(String tableName) {
            return !tableName.startsWith("com_") && !tableName.startsWith("base_") && !tableName.startsWith("sys_");
        }
    }
}