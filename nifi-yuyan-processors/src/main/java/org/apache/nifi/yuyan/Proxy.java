package org.apache.nifi.yuyan;

/**
 * 代理地址信息
 * @author xueho
 *
 */
public class Proxy {
	
	//类型 http  https
	private String type;
	//来源
	private String from;
	//匿名性 high_anonymous
	private String anonymity;
	//主机
	private String host;
	//端口
	private int port;
	//国家
	private String country;
	//相应时间
	private float response_time;
	//相关地址
//	private List<String> export_address;
	
	public String getType() {
		return type;
	}
	public void setType(String type) {
		this.type = type;
	}
	public String getFrom() {
		return from;
	}
	public void setFrom(String from) {
		this.from = from;
	}
	public String getAnonymity() {
		return anonymity;
	}
	public void setAnonymity(String anonymity) {
		this.anonymity = anonymity;
	}
	public String getHost() {
		return host;
	}
	public void setHost(String host) {
		this.host = host;
	}
	public int getPort() {
		return port;
	}
	public void setPort(int port) {
		this.port = port;
	}
	public String getCountry() {
		return country;
	}
	public void setCountry(String country) {
		this.country = country;
	}
	public float getResponse_time() {
		return response_time;
	}
	public void setResponse_time(float response_time) {
		this.response_time = response_time;
	}
//	public List<String> getExport_address() {
//		return export_address;
//	}
//	public void setExport_address(List<String> export_address) {
//		this.export_address = export_address;
//	}
	

}
