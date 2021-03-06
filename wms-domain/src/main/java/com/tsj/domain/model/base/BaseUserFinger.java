package com.tsj.domain.model.base;

import com.jfinal.plugin.activerecord.IBean;
import com.jfinal.plugin.activerecord.Model;

/**
 * Generated by JFinal, do not modify this file.
 */
@SuppressWarnings({"serial", "unchecked"})
public abstract class BaseUserFinger<M extends BaseUserFinger<M>> extends Model<M> implements IBean {

	public M setId(String id) {
		set("id", id);
		return (M)this;
	}
	
	public String getId() {
		return getStr("id");
	}

	public M setDeptId(String deptId) {
		set("deptId", deptId);
		return (M)this;
	}
	
	public String getDeptId() {
		return getStr("deptId");
	}

	public M setFingerUserId(String fingerUserId) {
		set("fingerUserId", fingerUserId);
		return (M)this;
	}
	
	public String getFingerUserId() {
		return getStr("fingerUserId");
	}

	public M setFingerNo(Integer fingerNo) {
		set("fingerNo", fingerNo);
		return (M)this;
	}
	
	public Integer getFingerNo() {
		return getInt("fingerNo");
	}

	public M setFingerValue(String fingerValue) {
		set("fingerValue", fingerValue);
		return (M)this;
	}
	
	public String getFingerValue() {
		return getStr("fingerValue");
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

}
