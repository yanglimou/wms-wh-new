package com.tsj.service;

import com.jfinal.log.Log;
import com.jfinal.plugin.ehcache.CacheKit;
import com.tsj.common.utils.DateUtils;
import com.tsj.domain.model.*;
import com.tsj.common.config.CommonConfig;
import com.tsj.service.common.MyService;
import org.apache.commons.lang3.StringUtils;

import java.util.List;

/**
 * @className: CacheService
 * @description: 缓存数据服务
 * @author: Frank
 * @create: 2020-03-13 15:23
 */
public class CacheService extends MyService {

    public static final Log logger = Log.getLog(CacheService.class);

    public void initCache() {
        //初始化耗材缓存
        List<Goods> goodsList = Goods.dao.findAll();
        if (goodsList.size() > 0) {
            goodsList.forEach(goods -> {
                CacheKit.put(CacheBase, GoodsById + goods.getId(), goods);
            });
            logger.info("初始化耗材缓存数量：{%d}", goodsList.size());
        }

        //初始化人员缓存
        List<User> userList = User.dao.findAll();
        if (userList.size() > 0) {
            userList.forEach(user -> {
                CacheKit.put(CacheBase, UserById + user.getId(), user);
            });
            logger.info("初始化人员缓存数量：{%d}", userList.size());
        }

        //初始化科室缓存
        List<Dept> deptList = Dept.dao.findAll();
        if (deptList.size() > 0) {
            deptList.forEach(dept -> {
                CacheKit.put(CacheBase, DeptById + dept.getId(), dept);
            });
            logger.info("初始化科室缓存数量：{%d}", deptList.size());
        }

        //初始化制标缓存
        int lastDays = CommonConfig.prop.getInt("spd.lastDays", 7);
        String QueryEndDate = DateUtils.getCurrentTime();
        String QueryBeginDate = DateUtils.addDay(QueryEndDate, -1 * lastDays);
        List<Material> materialList = Material.dao.find("select * from base_material where createDate > ?", QueryBeginDate);
        if (materialList.size() > 0) {
            materialList.forEach(material -> {
                CacheKit.put(CacheBase, MaterialById + material.getSpdCode(), material);
            });
            logger.info("初始化制标缓存数量：{%d}", materialList.size());
        }
    }

    public void clearCache() {
        removeCache(CacheBase, null, null);
        removeCache(CacheCom, null, null);
        removeCache(CacheSys, null, null);
    }

    public void removeCache(String cacheName, String cacheKeyPrefix, String cacheKeyValue) {
        logger.debug("{}-{}-{}", cacheName, cacheKeyPrefix, cacheKeyValue);

        if (StringUtils.isEmpty(cacheKeyPrefix)) {
            CacheKit.removeAll(cacheName);
        } else {
            if (StringUtils.isEmpty(cacheKeyValue)) {
                //删除指定KEY前缀的缓存对象
                for (Object key : CacheKit.getKeys(cacheName)) {
                    if (key.toString().startsWith(cacheKeyPrefix)) {
                        CacheKit.remove(cacheName, key);
                    }
                }
            } else {
                //删除指定KEY值的缓存对象
                CacheKit.remove(cacheName, cacheKeyPrefix + cacheKeyValue);
            }
        }
    }

    private <T> T getObjectFromCache(String cacheName, String cacheKeyPrefix, String cacheKeyValue, GetObjectFromDb<T> func) {
        T object = CacheKit.get(cacheName, cacheKeyPrefix + cacheKeyValue);
        if (object == null && func != null) {
            object = func.getObjectFromDb();
            if (object != null) {
                CacheKit.put(cacheName, cacheKeyPrefix + cacheKeyValue, object);
                logger.debug("{}-{}-{}", cacheName, cacheKeyPrefix, cacheKeyValue);
            }
        }
        return object;
    }

    private interface GetObjectFromDb<T> {
        T getObjectFromDb();
    }

    /////////////////////////////////////////////////////////////////////

    public StockBase getStockBase(String deptId, String goodsId) {
        return getObjectFromCache(CacheCom, StockBaseByDeptIdAndGoodsId, deptId + goodsId,
                () -> StockBase.dao.findFirst("select * from com_stock_base where deptId=? and goodsId=?", deptId, goodsId));
    }

    public Tag getTagById(String spdCode) {
        return getObjectFromCache(CacheCom, TagById, spdCode,
                () -> Tag.dao.findById(spdCode));
    }

    public Tag getTagByEpc(String epc) {
        return getObjectFromCache(CacheCom, TagByEpc, epc,
                () -> Tag.dao.findFirst("select * from com_tag where epc=?", epc));
    }

    public Manufacturer getManufacturerById(String manufacturerId) {
        return getObjectFromCache(CacheBase, ManufacturerById, manufacturerId,
                () -> Manufacturer.dao.findById(manufacturerId));
    }

    public Supplier getSupplierById(String supplierId) {
        return getObjectFromCache(CacheBase, SupplierById, supplierId,
                () -> Supplier.dao.findById(supplierId));
    }

    public Goods getGoodsById(String goodsId) {
        return getObjectFromCache(CacheBase, GoodsById, goodsId,
                () -> Goods.dao.findById(goodsId));
    }

    public Print getPrintById(String id) {
        return getObjectFromCache(CacheBase, PrintById, id,
                () -> Print.dao.findById(id));
    }

    public Dept getDeptById(String deptId) {
        return getObjectFromCache(CacheBase, DeptById, deptId,
                () -> Dept.dao.findById(deptId));
    }

    public Cabinet getCabinetById(String cabinetId) {
        return getObjectFromCache(CacheBase, CabinetById, cabinetId,
                () -> Cabinet.dao.findById(cabinetId));
    }

    public Material getMaterialById(String spdCode) {
        return getObjectFromCache(CacheBase, MaterialById, spdCode,
                () -> Material.dao.findById(spdCode));
    }

    public User getUserById(String userId) {
        return getObjectFromCache(CacheBase, UserById, userId,
                () -> User.dao.findById(userId));
    }

    public Dept getDeptByName(String name) {
        return getObjectFromCache(CacheBase, DeptByName, name,
                () -> Dept.dao.findFirst("select * from base_dept where name=?", name));
    }

    public Cabinet getCabinetByName(String name) {
        return getObjectFromCache(CacheBase, CabinetByName, name,
                () -> Cabinet.dao.findFirst("select * from base_cabinet where name=?", name));
    }

    public Manufacturer getManufacturerByName(String name) {
        return getObjectFromCache(CacheBase, ManufacturerByName, name,
                () -> Manufacturer.dao.findFirst("select * from base_manufacturer where name=?", name));
    }

    public Supplier getSupplierByName(String name) {
        return getObjectFromCache(CacheBase, SupplierByName, name,
                () -> Supplier.dao.findFirst("select * from base_supplier where name=?", name));
    }

    public Order getOrderByCode(String code) {
        return getObjectFromCache(CacheCom, OrderByCode, code,
                () -> Order.dao.findFirst("select * from com_order where code=?", code));
    }

    /////////////////////////////////////////////////////////////////////

    public User getUserByToken(String accessToken) {
        return CacheKit.get(CacheSys, UserByToken + accessToken);
    }

    public void saveAccessToken(String accessToken, User user) {
        CacheKit.put(CacheSys, UserByToken + accessToken, user);
    }

    public void removeAccessToken(String accessToken) {
        CacheKit.remove(CacheSys, UserByToken + accessToken);
    }

    public boolean getLoginState(String username) {
        List<String> keys = CacheKit.getKeys(CacheSys);

        for (String key : keys) {
            if (key.startsWith(UserByToken)) {
                User user = CacheKit.get(CacheSys, key);
                if (user != null && user.getUsername().equals(username)) {
                    return true;
                }
            }
        }
        return false;
    }
}
