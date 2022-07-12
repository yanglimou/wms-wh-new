package com.tsj.domain.model.base;

import com.jfinal.plugin.activerecord.IBean;
import com.jfinal.plugin.activerecord.Model;

/**
 * Generated by JFinal, do not modify this file.
 */
@SuppressWarnings({"serial", "unchecked"})
public abstract class BaseRecordIn<M extends BaseRecordIn<M>> extends Model<M> implements IBean {

	public M setId(String id) {
		set("id", id);
		return (M)this;
	}
	
	public String getId() {
		return getStr("id");
	}

	public M setCabinetId(String cabinetId) {
		set("cabinetId", cabinetId);
		return (M)this;
	}
	
	public String getCabinetId() {
		return getStr("cabinetId");
	}

	public M setGoodsId(String goodsId) {
		set("goodsId", goodsId);
		return (M)this;
	}
	
	public String getGoodsId() {
		return getStr("goodsId");
	}

	public M setSpdCode(String spdCode) {
		set("spdCode", spdCode);
		return (M)this;
	}
	
	public String getSpdCode() {
		return getStr("spdCode");
	}

	public M setConfirmed(String confirmed) {
		set("confirmed", confirmed);
		return (M)this;
	}
	
	public String getConfirmed() {
		return getStr("confirmed");
	}

	public M setCreateUserId(String createUserId) {
		set("createUserId", createUserId);
		return (M)this;
	}
	
	public String getCreateUserId() {
		return getStr("createUserId");
	}

	public M setCreateDate(String createDate) {
		set("createDate", createDate);
		return (M)this;
	}
	
	public String getCreateDate() {
		return getStr("createDate");
	}

	public M setUpload(String upload) {
		set("upload", upload);
		return (M)this;
	}
	
	public String getUpload() {
		return getStr("upload");
	}

	public M setType(String type) {
		set("type", type);
		return (M)this;
	}
	
	public String getType() {
		return getStr("type");
	}

	public M setExceptionDescId(String exceptionDescId) {
		set("exceptionDescId", exceptionDescId);
		return (M)this;
	}
	
	public String getExceptionDescId() {
		return getStr("exceptionDescId");
	}

}
