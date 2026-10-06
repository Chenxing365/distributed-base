package com.distributed.base.config;

import com.distributed.base.webservice.SystemIntegrationWs;
import com.distributed.base.webservice.impl.SystemIntegrationWsImpl;
import jakarta.xml.ws.Endpoint;
import org.apache.cxf.Bus;
import org.apache.cxf.jaxws.EndpointImpl;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Apache CXF WebService 配置类
 *
 * <p>架构说明：
 * <ul>
 *   <li>CXF Servlet 默认挂载在 {@code /ws} 路径（由 application.yml cxf.path 配置）</li>
 *   <li>WSDL 访问地址：http://localhost:8080/ws/SystemIntegrationService?wsdl</li>
 *   <li>WsImpl 注入了底层 Service Bean，与 REST Controller 共享同一业务层</li>
 * </ul>
 */
@Configuration
public class CxfWebServiceConfig {

    private final Bus bus;
    private final SystemIntegrationWsImpl systemIntegrationWsImpl;

    public CxfWebServiceConfig(Bus bus, SystemIntegrationWsImpl systemIntegrationWsImpl) {
        this.bus = bus;
        this.systemIntegrationWsImpl = systemIntegrationWsImpl;
    }

    /**
     * 发布 SystemIntegrationService 端点
     * 完整 SOAP 地址：http://host:8080/ws/SystemIntegrationService
     */
    @Bean
    public Endpoint systemIntegrationEndpoint() {
        EndpointImpl endpoint = new EndpointImpl(bus, systemIntegrationWsImpl);
        endpoint.publish("/SystemIntegrationService");
        return endpoint;
    }
}
