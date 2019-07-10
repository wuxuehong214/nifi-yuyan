package org.apache.nifi.yuyan;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.Vector;
import java.util.concurrent.TimeUnit;

import org.apache.nifi.annotation.behavior.ReadsAttribute;
import org.apache.nifi.annotation.behavior.ReadsAttributes;
import org.apache.nifi.annotation.behavior.WritesAttribute;
import org.apache.nifi.annotation.behavior.WritesAttributes;
import org.apache.nifi.annotation.documentation.CapabilityDescription;
import org.apache.nifi.annotation.documentation.SeeAlso;
import org.apache.nifi.annotation.documentation.Tags;
import org.apache.nifi.annotation.lifecycle.OnRemoved;
import org.apache.nifi.components.PropertyDescriptor;
import org.apache.nifi.components.PropertyValue;
import org.apache.nifi.expression.AttributeExpression.ResultType;
import org.apache.nifi.expression.ExpressionLanguageScope;
import org.apache.nifi.flowfile.FlowFile;
import org.apache.nifi.processor.AbstractProcessor;
import org.apache.nifi.processor.ProcessContext;
import org.apache.nifi.processor.ProcessSession;
import org.apache.nifi.processor.ProcessorInitializationContext;
import org.apache.nifi.processor.Relationship;
import org.apache.nifi.processor.exception.ProcessException;
import org.apache.nifi.processor.util.StandardValidators;

import com.alibaba.fastjson.JSONObject;

/**
 * IP代理池服务
 * 
 * @author xueho
 *
 */

@Tags({ "proxy", "http", "https", "舆眼", "爬虫", "代理池", "IP池" })
@CapabilityDescription("提供http/https代理池")
@SeeAlso({})
@ReadsAttributes({ @ReadsAttribute(attribute = "type", description = "代理类型http/https") })
@WritesAttributes({ @WritesAttribute(attribute = "host", description = "代理主机"),
		@WritesAttribute(attribute = "port", description = "代理端口") })
public class ProxyPoolProcessor extends AbstractProcessor {

	/**
	 * 获取 代理的类型 http/https
	 */
	public static final PropertyDescriptor PROP_REQUEST_TYPE = new PropertyDescriptor.Builder().name("代理类型")
			.description("代理类型http/https").required(true)
			.addValidator(StandardValidators.createAttributeExpressionLanguageValidator(ResultType.STRING, true))
			.defaultValue("http").expressionLanguageSupported(ExpressionLanguageScope.FLOWFILE_ATTRIBUTES).build();

	/**
	 * 
	 */
	public static final Relationship REL_SUCCESS = new Relationship.Builder().name("获取成功").description("成功获取到了代理IP服务")
			.build();

	/**
	 * 
	 */
	public static final Relationship REL_FAIL = new Relationship.Builder().name("获取失败").description("获取代理IP服务失败")
			.build();

	private List<PropertyDescriptor> descriptors;

	private Set<Relationship> relationships;

	private Vector<Proxy> proxies = new Vector<Proxy>();

	private List<Proxy> http = new ArrayList<>();

	private boolean start = false;

	@Override
	protected void init(final ProcessorInitializationContext context) {
		final List<PropertyDescriptor> descriptors = new ArrayList<PropertyDescriptor>();
		descriptors.add(PROP_REQUEST_TYPE);
		this.descriptors = Collections.unmodifiableList(descriptors);

		final Set<Relationship> relationships = new HashSet<Relationship>();
		relationships.add(REL_SUCCESS);
		relationships.add(REL_FAIL);
		this.relationships = Collections.unmodifiableSet(relationships);
		start = true;

		new Thread(new Runnable() {
			@Override
			public void run() {
				while (start) {
					proxies.clear();
					proxy();
					try {
						TimeUnit.SECONDS.sleep(60);
					} catch (InterruptedException e) {
						e.printStackTrace();
					}
					
				}
			}
		}).start();
	}

	@Override
	public void onTrigger(ProcessContext context, ProcessSession session) throws ProcessException {

		FlowFile flowFile = session.get();

		PropertyValue pv = context.getProperty(PROP_REQUEST_TYPE);
		String type = pv.getValue();
		Proxy proxy = getProxy(type);
		
		if(proxy == null){
			session.transfer(flowFile, REL_FAIL);
		}else{
			flowFile = session.putAttribute(flowFile, "host", proxy.getHost());
			flowFile = session.putAttribute(flowFile, "port", String.valueOf(proxy.getPort()));
			session.transfer(flowFile, REL_SUCCESS);
		}
	}

	@Override
	public Set<Relationship> getRelationships() {
		return this.relationships;
	}

	@Override
	public final List<PropertyDescriptor> getSupportedPropertyDescriptors() {
		return descriptors;
	}

	@OnRemoved
	public void destroy() {
		start = false;
	}

	Random random = new Random();
	public Proxy getProxy(String type){
		synchronized (proxies) {
			
			while(proxies.isEmpty()){
				try {
					System.out.println("等待新地址》。。。。。。。。。。。。。");
					proxies.wait();
				} catch (InterruptedException e) {
				}
			}
			
			int size = proxies.size();
			int index = random.nextInt(size);
			Proxy tmp = proxies.get(index);
			int count = 0;
			while(!(tmp = proxies.get(index)).getType().equals(type)){
				index = random.nextInt(size);
				count++;
				if(count>10){
				  tmp = null;
				  break;
				}
			}
			return tmp;
		}
	}
	
	/**
	 * 获取代理IP池
	 * @return
	 */
	private void proxy() {
		String url = "https://raw.githubusercontent.com/fate0/proxylist/master/proxy.list";
		BufferedReader read = null;// 读取访问结果
		try {
			URL realurl = new URL(url);
			URLConnection connection = realurl.openConnection();
			connection.connect();
			// 定义 BufferedReader输入流来读取URL的响应
			read = new BufferedReader(new InputStreamReader(connection.getInputStream(), "UTF-8"));
			String line;// 循环读取
			while ((line = read.readLine()) != null) {
				try {
					 Proxy p = JSONObject.parseObject(line, Proxy.class);
					 synchronized (proxies) {
						 proxies.add(p);
						 proxies.notifyAll();
//						 System.out.println("获取到一个新的地址");
					}
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
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
	}

	public static void main(String args[]) {
		ProxyPoolProcessor pp = new ProxyPoolProcessor();
		pp.init(null);
		
		for(int i=0;i<10;i++){
			Proxy p = pp.getProxy("http");
			System.out.println(JSONObject.toJSONString(p));
			try {
				TimeUnit.SECONDS.sleep(1);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
		
	}

}
