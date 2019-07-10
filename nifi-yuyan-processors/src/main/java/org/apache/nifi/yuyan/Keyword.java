package org.apache.nifi.yuyan;

public class Keyword {
	
	private Integer id;
	private Integer user_id;
	private String keyword;
	private Integer company_id;
	private String create_time;
	private String description;
	private boolean pushable;
	private Long obj_id;
	
	public Long getObj_id() {
		return obj_id;
	}
	public void setObj_id(Long obj_id) {
		this.obj_id = obj_id;
	}
	public Integer getId() {
		return id;
	}
	public void setId(Integer id) {
		this.id = id;
	}
	public Integer getUser_id() {
		return user_id;
	}
	public void setUser_id(Integer user_id) {
		this.user_id = user_id;
	}
	public String getKeyword() {
		return keyword;
	}
	public void setKeyword(String keyword) {
		this.keyword = keyword;
	}
	public Integer getCompany_id() {
		return company_id;
	}
	public void setCompany_id(Integer company_id) {
		this.company_id = company_id;
	}
	public String getCreate_time() {
		return create_time;
	}
	public void setCreate_time(String create_time) {
		this.create_time = create_time;
	}
	public String getDescription() {
		return description;
	}
	public void setDescription(String description) {
		this.description = description;
	}
	public boolean isPushable() {
		return pushable;
	}
	public void setPushable(boolean pushable) {
		this.pushable = pushable;
	}
	
	

}
