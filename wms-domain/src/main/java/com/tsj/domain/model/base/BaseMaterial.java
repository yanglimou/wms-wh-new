package com.tsj.domain.model.base;

import com.jfinal.plugin.activerecord.IBean;
import com.jfinal.plugin.activerecord.Model;

/**
 * Generated by JFinal, do not modify this file.
 */
@SuppressWarnings({"serial", "unchecked"})
public abstract class BaseMaterial<M extends BaseMaterial<M>> extends Model<M> implements IBean {

	public M setSpdCode(String spdCode) {
		set("spdCode", spdCode);
		return (M)this;
	}
	
	public String getSpdCode() {
		return getStr("spdCode");
	}

	public M setDeptId(String deptId) {
		set("deptId", deptId);
		return (M)this;
	}
	
	public String getDeptId() {
		return getStr("deptId");
	}

	public M setOrderCode(String orderCode) {
		set("orderCode", orderCode);
		return (M)this;
	}

	public String getOrderCode() {
		return getStr("orderCode");
	}

	public M setGoodsId(String goodsId) {
		set("goodsId", goodsId);
		return (M)this;
	}
	
	public String getGoodsId() {
		return getStr("goodsId");
	}

	public M setBatchNo(String batchNo) {
		set("batchNo", batchNo);
		return (M)this;
	}
	
	public String getBatchNo() {
		return getStr("batchNo");
	}

	public M setExpireDate(String expireDate) {
		set("expireDate", expireDate);
		return (M)this;
	}
	
	public String getExpireDate() {
		return getStr("expireDate");
	}

	public M setCreateDate(String createDate) {
		set("createDate", createDate);
		return (M)this;
	}
	
	public String getCreateDate() {
		return getStr("createDate");
	}

}
