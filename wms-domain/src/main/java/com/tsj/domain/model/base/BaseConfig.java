package com.tsj.domain.model.base;

import com.jfinal.plugin.activerecord.IBean;
import com.jfinal.plugin.activerecord.Model;

/**
 * Generated by JFinal, do not modify this file.
 */
@SuppressWarnings({"serial", "unchecked"})
public abstract class BaseConfig<M extends BaseConfig<M>> extends Model<M> implements IBean {

	public M setId(String id) {
		set("id", id);
		return (M)this;
	}
	
	public String getId() {
		return getStr("id");
	}

	public M setType(String type) {
		set("type", type);
		return (M)this;
	}
	
	public String getType() {
		return getStr("type");
	}

	public M setName(String name) {
		set("name", name);
		return (M)this;
	}
	
	public String getName() {
		return getStr("name");
	}

	public M setValue(String value) {
		set("value", value);
		return (M)this;
	}
	
	public String getValue() {
		return getStr("value");
	}

	public M setModifyUserId(String modifyUserId) {
		set("modifyUserId", modifyUserId);
		return (M)this;
	}
	
	public String getModifyUserId() {
		return getStr("modifyUserId");
	}

	public M setModifyDate(String modifyDate) {
		set("modifyDate", modifyDate);
		return (M)this;
	}
	
	public String getModifyDate() {
		return getStr("modifyDate");
	}

}