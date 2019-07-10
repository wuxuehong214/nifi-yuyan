/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.nifi.yuyan;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.commons.io.IOUtils;
import org.apache.nifi.annotation.behavior.ReadsAttribute;
import org.apache.nifi.annotation.behavior.ReadsAttributes;
import org.apache.nifi.annotation.behavior.WritesAttribute;
import org.apache.nifi.annotation.behavior.WritesAttributes;
import org.apache.nifi.annotation.documentation.CapabilityDescription;
import org.apache.nifi.annotation.documentation.SeeAlso;
import org.apache.nifi.annotation.documentation.Tags;
import org.apache.nifi.annotation.lifecycle.OnScheduled;
import org.apache.nifi.components.PropertyDescriptor;
import org.apache.nifi.distributed.cache.client.Deserializer;
import org.apache.nifi.distributed.cache.client.DistributedMapCacheClient;
import org.apache.nifi.distributed.cache.client.Serializer;
import org.apache.nifi.distributed.cache.client.exception.DeserializationException;
import org.apache.nifi.distributed.cache.client.exception.SerializationException;
import org.apache.nifi.expression.AttributeExpression.ResultType;
import org.apache.nifi.expression.ExpressionLanguageScope;
import org.apache.nifi.flowfile.FlowFile;
import org.apache.nifi.logging.ComponentLog;
import org.apache.nifi.processor.AbstractProcessor;
import org.apache.nifi.processor.ProcessContext;
import org.apache.nifi.processor.ProcessSession;
import org.apache.nifi.processor.ProcessorInitializationContext;
import org.apache.nifi.processor.Relationship;
import org.apache.nifi.processor.exception.ProcessException;
import org.apache.nifi.processor.util.StandardValidators;

@Tags({"keyword","compare","yuyan","关键词","舆情策略","爬取策略"})
@CapabilityDescription("将爬取到的资源内容与关键词进行比对，判断是否包含关键词")
@SeeAlso({})
@ReadsAttributes({@ReadsAttribute(attribute="keyword_id", description="匹配的关键词ID")})
@WritesAttributes({@WritesAttribute(attribute="", description="")})
public class KeywordCompareProcessor extends AbstractProcessor {

	/**
	 * 需要缓存服务信息
	 */
    public static final PropertyDescriptor PROP_DISTRIBUTED_CACHE_SERVICE = new PropertyDescriptor
            .Builder().name("缓存服务")
            .displayName("缓存服务")
            .description("通过缓存服务来获取缓存信息")
            .required(true)
            .identifiesControllerService(DistributedMapCacheClient.class)
            .build();
    
    /**
     * 从缓存服务中获取关键词列表 的 键值
     */
    public static final PropertyDescriptor PROP_CACHE_ENTRY_IDENTIFIER = new PropertyDescriptor.Builder()
    		.name("获取关键词的键值GLOBAL_KEYWORD")
    		.description("用于获取爬取关键词的键值")
    		.required(true)
    		.addValidator(StandardValidators.createAttributeExpressionLanguageValidator(ResultType.STRING,true))
    		.defaultValue("GLOBAL_KEYWORD")
    		.expressionLanguageSupported(ExpressionLanguageScope.FLOWFILE_ATTRIBUTES)
    		.build();
    
    /**
     * 
     */
    public static final Relationship REL_MATCHED = new Relationship.Builder()
            .name("匹配关键字")
            .description("爬取的资源内容与关键词匹配了")
            .build();
        		
    /**
     * 
     */
    public static final Relationship REL_UNMATCHED = new Relationship.Builder()
            .name("未匹配关键字")
            .description("爬取的资源内容未能与任意关键词匹配")
            .build();
    
    /**
     * 
     */
    public static final Relationship REL_FAILURE = new Relationship.Builder()
            .name("失败")
            .description("处理异常")
            .build();
    
    /**
     * 
     */
    public static final Relationship REL_ORIGINAL = new Relationship.Builder()
            .name("原始记录")
            .description("原始数据流")
            .build();


    private List<PropertyDescriptor> descriptors;

    private Set<Relationship> relationships;

    private final Serializer<String> keySerializer = new StringSerializer();
    private final Deserializer<String> valueSerializer = new CacheValueDeserializer();
    
    @Override
    protected void init(final ProcessorInitializationContext context) {
        final List<PropertyDescriptor> descriptors = new ArrayList<PropertyDescriptor>();
        descriptors.add(PROP_DISTRIBUTED_CACHE_SERVICE);
        descriptors.add(PROP_CACHE_ENTRY_IDENTIFIER);
        this.descriptors = Collections.unmodifiableList(descriptors);

        final Set<Relationship> relationships = new HashSet<Relationship>();
        relationships.add(REL_MATCHED);
        relationships.add(REL_UNMATCHED);
        relationships.add(REL_FAILURE);
        relationships.add(REL_ORIGINAL);
        this.relationships = Collections.unmodifiableSet(relationships);
    }

    @Override
    public Set<Relationship> getRelationships() {
        return this.relationships;
    }

    @Override
    public final List<PropertyDescriptor> getSupportedPropertyDescriptors() {
        return descriptors;
    }

    @OnScheduled
    public void onScheduled(final ProcessContext context) {
    	
    }

    @Override
    public void onTrigger(final ProcessContext context, final ProcessSession session) throws ProcessException {
        FlowFile flowFile = session.get();
        if ( flowFile == null ) {
            return;
        }
        final ComponentLog logger = getLogger();
        final String key = context.getProperty(PROP_CACHE_ENTRY_IDENTIFIER).evaluateAttributeExpressions(flowFile).getValue();
        final DistributedMapCacheClient cache = context.getProperty(PROP_DISTRIBUTED_CACHE_SERVICE).asControllerService(DistributedMapCacheClient.class);
        
        try{
        	String cacheValue = cache.get(key, keySerializer, valueSerializer);
        	if(org.apache.nifi.util.StringUtils.isEmpty(cacheValue)){
        		session.transfer(flowFile,REL_UNMATCHED);
        	}else{
        		List<Keyword> list = com.alibaba.fastjson.JSONArray.parseArray(cacheValue, Keyword.class);
        		InputStream is = session.read(flowFile);
        		String content = IOUtils.toString(is);
        		is.close();
        		
        		boolean matched = false;
        		for(Keyword word:list){
        			 if(content.contains(word.getKeyword())){
        				 matched = true;
        				 FlowFile flow = session.create(flowFile);
        				 flow = session.write(flow, (out)->{
        					 out.write(content.getBytes(StandardCharsets.UTF_8));
        				 });
        				 flow = session.putAttribute(flow, "keyword_id", String.valueOf(word.getId()));
        				 flow = session.putAttribute(flow, "obj_id", String.valueOf(word.getObj_id()));
//        				 ffs.add(flow);
        				 session.transfer(flow, REL_MATCHED);
        			 }
        		}
        		if(!matched){
        			session.transfer(flowFile,REL_UNMATCHED);
        		}else{
        			session.transfer(flowFile,REL_ORIGINAL);
        		}
        	}
        }catch(IOException e){
        	flowFile = session.penalize(flowFile);
            session.transfer(flowFile, REL_FAILURE);
            logger.error("Unable to communicate with cache when processing {} due to {}", new Object[]{flowFile, e});
        }
    }
    
    public static class StringSerializer implements Serializer<String> {

        @Override
        public void serialize(final String value, final OutputStream out) throws SerializationException, IOException {
            out.write(value.getBytes(StandardCharsets.UTF_8));
        }
    }
    
    public static class CacheValueDeserializer implements Deserializer<String> {

        @Override
        public String deserialize(final byte[] input) throws DeserializationException, IOException {
            if (input == null || input.length == 0) {
                return null;
            }
            return new String(input,StandardCharsets.UTF_8);
        }
    }
}
