package com.uuzu.log.config;

import com.mongodb.MongoClient;
import com.mongodb.MongoClientOptions;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.NoSuchBeanDefinitionException;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.mongodb.MongoDbFactory;
import org.springframework.data.mongodb.core.SimpleMongoDbFactory;
import org.springframework.data.mongodb.core.convert.*;
import org.springframework.data.mongodb.core.mapping.MongoMappingContext;

/**
 * Created by lixing on 2017/5/16.
 */
@Configuration
public class MongoConfig {

    /*@Bean
    public MongoDbFactory mongoDbFactory(){
        MongoClientOptions.Builder mongoClientOptions = MongoClientOptions.builder().
                                                                            socketKeepAlive(Boolean.TRUE).
                                                                            connectionsPerHost(20);
        MongoClientOptions clientOptions = mongoClientOptions.build();
        MongoClient mongoClient = new MongoClient("mongodb:27017",clientOptions);
        SimpleMongoDbFactory simpleMongoDbFactory = new SimpleMongoDbFactory(mongoClient,"dmpcallback");
        return simpleMongoDbFactory;
    }*/


    @Bean
    public MappingMongoConverter mappingMongoConverter(MongoDbFactory mongoDbFactory, MongoMappingContext context, BeanFactory beanFactory) {
        DbRefResolver dbRefResolver = new DefaultDbRefResolver(mongoDbFactory);
        MappingMongoConverter mappingConverter = new MappingMongoConverter(dbRefResolver, context);
        try {
            mappingConverter.setCustomConversions(beanFactory.getBean(CustomConversions.class));
        }
        catch (NoSuchBeanDefinitionException ignore) {
            ignore.printStackTrace();
        }
        mappingConverter.setTypeMapper(new DefaultMongoTypeMapper(null)); //去除_class字段
        return mappingConverter;
    }

}
