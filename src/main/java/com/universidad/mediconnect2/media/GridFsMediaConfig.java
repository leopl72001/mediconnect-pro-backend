package com.universidad.mediconnect2.media;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.data.mongodb.MongoDatabaseFactory;
import org.springframework.data.mongodb.core.convert.MongoConverter;
import org.springframework.data.mongodb.gridfs.GridFsTemplate;

/** Explicit GridFS bean for Spring Data MongoDB 5 / Boot 4. */
@Configuration
@Profile({"prod", "docker"})
public class GridFsMediaConfig {
    @Bean
    GridFsTemplate gridFsTemplate(MongoDatabaseFactory factory, MongoConverter converter) {
        return new GridFsTemplate(factory, converter);
    }
}
