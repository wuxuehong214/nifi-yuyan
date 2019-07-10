package org.apache.nifi.yuyan;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.InetSocketAddress;
import java.net.URL;
import java.net.URLConnection;
import java.util.concurrent.TimeUnit;

import com.alibaba.fastjson.JSONObject;

public class Test {

	
	public static void main(String args[]){
		
		ProxyPoolProcessor ppp = new ProxyPoolProcessor();
		ppp.init(null);
		
		String url = "http://icanhazip.com/";
		int count = 0;
		while(true){
			System.out.println("=======================================================================:"+(++count));
			Proxy p = ppp.getProxy("http");
			if(p == null){
				System.out.println("获取代理地址失败！！");
				try {
					TimeUnit.SECONDS.sleep(1);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
				continue;
			}
			System.out.println("获得代理:"+JSONObject.toJSONString(p));
			
			BufferedReader read = null;// 读取访问结果
			try {
				URL realurl = new URL(url);
				
				InetSocketAddress addr = new InetSocketAddress(p.getHost(),p.getPort());
				java.net.Proxy proxy = new java.net.Proxy(java.net.Proxy.Type.HTTP, addr);
				URLConnection connection = realurl.openConnection(proxy);
				
	//			connection.addRequestProperty("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,image/webp,image/apng,*/*;q=0.8,application/signed-exchange;v=b3");
	//			connection.addRequestProperty("Accept-Encoding", "gzip, deflate, br");
	//			connection.addRequestProperty("Accept-Language", "zh-CN,zh;q=0.9");
	//			connection.addRequestProperty("Cache-Control", "max-age=0");
				connection.addRequestProperty("Cookie", "SUID=AB8268DF5018910A000000005D1C9982; SUV=1562155395546804; SNUID=DB04F36911149C1780145D0B119F5FAF");
	//			connection.addRequestProperty("Host", "weixin.sogou.com");
	//			connection.addRequestProperty("Referer", "https://weixin.sogou.com/antispider/?from=%2fweixin%3Ftype%3d2%26query%3d%E5%B9%BC%E5%84%BF%E5%9B%AD");
	//			connection.addRequestProperty("Upgrade-Insecure-Requests", "1");
	//			connection.addRequestProperty("User-Agent", "Mozilla/5.0 (Windows NT 10.0; WOW64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/75.0.3770.100 Safari/537.36");
				connection.connect();
				// 定义 BufferedReader输入流来读取URL的响应
				read = new BufferedReader(new InputStreamReader(connection.getInputStream(), "utf-8"));
				String line;// 循环读取
				StringBuffer sb = new StringBuffer();
				while ((line = read.readLine()) != null) {
					sb.append(line);
				}
				System.out.println("############读取内容长度:"+sb.length());
				System.out.println(sb.toString());
				read.close();
			} catch (Exception e) {
				e.printStackTrace();
			} finally {
				if (read != null) {
					try {
						read.close();
					} catch (IOException e) {
						e.printStackTrace();
					}
				}
			}
		
			try {
				TimeUnit.SECONDS.sleep(1);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
			
		}
	}
	
}
