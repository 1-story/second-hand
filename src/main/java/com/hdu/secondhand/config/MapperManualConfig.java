package com.hdu.secondhand.config;

import com.baomidou.mybatisplus.annotation.DbType;
import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.extension.plugins.MybatisPlusInterceptor;
import com.baomidou.mybatisplus.extension.plugins.inner.PaginationInnerInterceptor;
import com.baomidou.mybatisplus.extension.spring.MybatisSqlSessionFactoryBean;
import com.hdu.secondhand.mapper.AiEstimateLogMapper;
import com.hdu.secondhand.mapper.AiPublishDraftMapper;
import com.hdu.secondhand.mapper.BrowseHistoryMapper;
import com.hdu.secondhand.mapper.CategoryMapper;
import com.hdu.secondhand.mapper.FavoriteMapper;
import com.hdu.secondhand.mapper.ProductImageMapper;
import com.hdu.secondhand.mapper.ProductMapper;
import com.hdu.secondhand.mapper.UserMapper;
import com.zaxxer.hikari.HikariDataSource;
import org.apache.ibatis.session.SqlSessionFactory;
import org.mybatis.spring.mapper.MapperFactoryBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;

import javax.sql.DataSource;

/**
 * 本地离线环境 Mapper 手动注册（profile=local）
 *
 * <p>背景：本机离线仓库只有 mybatis-spring 2.1.2（与 Spring 6 不兼容，@MapperScan 会触发
 * factoryBeanObjectType 错误）；mybatis-spring 3.x 需联网下载。
 * 本配置绕过扫描器，手动注册 SqlSessionFactory 与全部 Mapper，使本地能启动服务供前端联调。</p>
 *
 * <p>生产环境（联网，mybatis-plus-spring-boot3-starter + mybatis-spring 3.x）继续使用
 * @MapperScan 自动扫描，不受本配置影响（本配置仅 local profile 生效）。</p>
 */
@Configuration
@Profile("local")
public class MapperManualConfig {

    /** 本地数据源（密码从环境变量 DB_PASSWORD 读取，勿硬编码；自动配置不生效时兜底） */
    @Bean
    public DataSource dataSource() {
        HikariDataSource ds = new HikariDataSource();
        // stringtype=unspecified：允许字符串参数写入 JSONB 列
        ds.setJdbcUrl("jdbc:postgresql://localhost:5432/second_hand?stringtype=unspecified");
        ds.setUsername("postgres");
        ds.setPassword(System.getenv().getOrDefault("DB_PASSWORD", "123456"));
        ds.setDriverClassName("org.postgresql.Driver");
        ds.setMaximumPoolSize(10);
        return ds;
    }

    @Bean
    public SqlSessionFactory sqlSessionFactory(DataSource dataSource) throws Exception {
        MybatisSqlSessionFactoryBean factory = new MybatisSqlSessionFactoryBean();
        factory.setDataSource(dataSource);

        MybatisConfiguration configuration = new MybatisConfiguration();
        configuration.setMapUnderscoreToCamelCase(true);
        factory.setConfiguration(configuration);

        MybatisPlusInterceptor interceptor = new MybatisPlusInterceptor();
        interceptor.addInnerInterceptor(new PaginationInnerInterceptor(DbType.POSTGRE_SQL));
        factory.setPlugins(interceptor);

        factory.setMapperLocations(new PathMatchingResourcePatternResolver()
                .getResources("classpath*:/mapper/**/*.xml"));

        return factory.getObject();
    }

    @Bean
    public UserMapper userMapper(SqlSessionFactory factory) {
        return mapper(factory, UserMapper.class);
    }

    @Bean
    public CategoryMapper categoryMapper(SqlSessionFactory factory) {
        return mapper(factory, CategoryMapper.class);
    }

    @Bean
    public ProductMapper productMapper(SqlSessionFactory factory) {
        return mapper(factory, ProductMapper.class);
    }

    @Bean
    public ProductImageMapper productImageMapper(SqlSessionFactory factory) {
        return mapper(factory, ProductImageMapper.class);
    }

    @Bean
    public FavoriteMapper favoriteMapper(SqlSessionFactory factory) {
        return mapper(factory, FavoriteMapper.class);
    }

    @Bean
    public BrowseHistoryMapper browseHistoryMapper(SqlSessionFactory factory) {
        return mapper(factory, BrowseHistoryMapper.class);
    }

    @Bean
    public AiEstimateLogMapper aiEstimateLogMapper(SqlSessionFactory factory) {
        return mapper(factory, AiEstimateLogMapper.class);
    }

    @Bean
    public AiPublishDraftMapper aiPublishDraftMapper(SqlSessionFactory factory) {
        return mapper(factory, AiPublishDraftMapper.class);
    }

    private <T> T mapper(SqlSessionFactory factory, Class<T> mapperType) {
        MapperFactoryBean<T> bean = new MapperFactoryBean<>(mapperType);
        bean.setSqlSessionFactory(factory);
        try {
            bean.afterPropertiesSet();
            return bean.getObject();
        } catch (Exception e) {
            throw new IllegalStateException("初始化 Mapper 失败: " + mapperType.getName(), e);
        }
    }
}
