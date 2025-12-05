package com.blibli.gdn.gateway.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.gateway.server.mvc.common.MvcUtils;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestClient;
import org.springframework.web.servlet.function.RouterFunction;
import org.springframework.web.servlet.function.ServerRequest;
import org.springframework.web.servlet.function.ServerResponse;

import java.net.URI;

import static org.springframework.cloud.gateway.server.mvc.filter.FilterFunctions.addRequestHeader;
import static org.springframework.cloud.gateway.server.mvc.handler.GatewayRouterFunctions.route;
import static org.springframework.cloud.gateway.server.mvc.handler.HandlerFunctions.http;
import static org.springframework.cloud.gateway.server.mvc.predicate.GatewayRequestPredicates.path;

@Configuration
@Slf4j
public class GatewayConfig {

    @Value("${services.member.url}")
    private String memberServiceUrl;

    @Value("${services.product.url}")
    private String productServiceUrl;

    @Value("${services.cart.url}")
    private String cartServiceUrl;

    @Bean
    public RestClient restClient() {
        return RestClient.builder().build();
    }

    @Bean
    public RouterFunction<ServerResponse> gatewayRouterFunctions() {
        log.info("Configuring gateway routes...");
        log.info("Member Service URL: {}", memberServiceUrl);
        log.info("Product Service URL: {}", productServiceUrl);
        log.info("Cart Service URL: {}", cartServiceUrl);
        log.info("Auth endpoints are now handled locally in Gateway");

        return route("member_service_members")
                .route(path("/api/v1/members/**"), http())
                .filter((request, next) -> {
                    URI uri = URI.create(memberServiceUrl + request.uri().getPath());
                    request.attributes().put(MvcUtils.GATEWAY_REQUEST_URL_ATTR, uri);
                    return next.handle(request);
                })
                .filter(addRequestHeader("X-Gateway", "API-Gateway"))
                .build()
                .and(route("product_service_internal")
                        .route(path("/api/v1/internal/products/**"), http())
                        .filter((request, next) -> {
                            URI uri = URI.create(productServiceUrl + request.uri().getPath());
                            request.attributes().put(MvcUtils.GATEWAY_REQUEST_URL_ATTR, uri);
                            return next.handle(request);
                        })
                        .filter(addRequestHeader("X-Gateway", "API-Gateway"))
                        .build())
                .and(route("product_service")
                        .route(path("/api/v1/products/**"), http())
                        .filter((request, next) -> {
                            URI uri = URI.create(productServiceUrl + request.uri().getPath());
                            request.attributes().put(MvcUtils.GATEWAY_REQUEST_URL_ATTR, uri);
                            return next.handle(request);
                        })
                        .filter(addRequestHeader("X-Gateway", "API-Gateway"))
                        .build())
                .and(route("cart_service")
                        .route(path("/api/v1/cart/**"), http())
                        .filter((request, next) -> {
                            URI uri = URI.create(cartServiceUrl + request.uri().getPath());
                            request.attributes().put(MvcUtils.GATEWAY_REQUEST_URL_ATTR, uri);
                            return next.handle(request);
                        })
                        .filter(addRequestHeader("X-Gateway", "API-Gateway"))
                        .filter((request, next) -> {
                            Object userId = request.servletRequest().getAttribute("X-User-Id");
                            Object userEmail = request.servletRequest().getAttribute("X-User-Email");
                            Object userRole = request.servletRequest().getAttribute("X-User-Role");
                            Object userType = request.servletRequest().getAttribute("X-User-Type");
                            Object hasValidToken = request.servletRequest().getAttribute("X-Has-Valid-Token");

                            log.debug("Forwarding to Cart Service - userId: {}, userType: {}", userId, userType);

                            var modifiedRequest = ServerRequest.from(request)
                                    .header("X-User-Id", userId != null ? userId.toString() : "")
                                    .header("X-User-Email", userEmail != null ? userEmail.toString() : "")
                                    .header("X-User-Role", userRole != null ? userRole.toString() : "")
                                    .header("X-User-Type", userType != null ? userType.toString() : "")
                                    .header("X-Has-Valid-Token", hasValidToken != null ? hasValidToken.toString() : "false")
                                    .build();

                            return next.handle(modifiedRequest);
                        })
                        .build());
    }
}

@RestController
@Slf4j
class ApiDocsProxyController {

    private final RestClient restClient;

    @Value("${services.member.url}")
    private String memberServiceUrl;

    @Value("${services.product.url}")
    private String productServiceUrl;

    @Value("${services.cart.url}")
    private String cartServiceUrl;

    @Value("${server.port}")
    private String gatewayPort;

    public ApiDocsProxyController(RestClient restClient) {
        this.restClient = restClient;
    }

    @GetMapping("/member/v3/api-docs")
    public ResponseEntity<String> getMemberApiDocs() {
        log.debug("Proxying request to Member service API docs: {}/v3/api-docs", memberServiceUrl);
        try {
            String response = restClient.get()
                    .uri(memberServiceUrl + "/v3/api-docs")
                    .retrieve()
                    .body(String.class);

            String modifiedResponse = rewriteServerUrls(response, memberServiceUrl);

            return ResponseEntity.ok()
                    .header("Content-Type", "application/json")
                    .body(modifiedResponse);
        } catch (Exception e) {
            log.error("Error proxying to Member service: {}", e.getMessage());
            throw e;
        }
    }

    @GetMapping("/product/v3/api-docs")
    public ResponseEntity<String> getProductApiDocs() {
        log.debug("Proxying request to Product service API docs: {}/v3/api-docs", productServiceUrl);
        try {
            String response = restClient.get()
                    .uri(productServiceUrl + "/v3/api-docs")
                    .retrieve()
                    .body(String.class);

            String modifiedResponse = rewriteServerUrls(response, productServiceUrl);

            return ResponseEntity.ok()
                    .header("Content-Type", "application/json")
                    .body(modifiedResponse);
        } catch (Exception e) {
            log.error("Error proxying to Product service: {}", e.getMessage());
            throw e;
        }
    }

    @GetMapping("/cart/v3/api-docs")
    public ResponseEntity<String> getCartApiDocs() {
        log.debug("Proxying request to Cart service API docs: {}/v3/api-docs", cartServiceUrl);
        try {
            String response = restClient.get()
                    .uri(cartServiceUrl + "/v3/api-docs")
                    .retrieve()
                    .body(String.class);

            String modifiedResponse = rewriteServerUrls(response, cartServiceUrl);

            return ResponseEntity.ok()
                    .header("Content-Type", "application/json")
                    .body(modifiedResponse);
        } catch (Exception e) {
            log.error("Error proxying to Cart service: {}", e.getMessage());
            throw e;
        }
    }


    private String rewriteServerUrls(String apiDocsJson, String originalServiceUrl) {
        try {
            String gatewayUrl = "http://localhost:" + gatewayPort;

            String modified = apiDocsJson.replace(originalServiceUrl, gatewayUrl);

            modified = modified.replaceAll(
                    "\"url\"\\s*:\\s*\"" + originalServiceUrl.replace(":", "\\:") + "\"",
                    "\"url\":\"" + gatewayUrl + "\""
            );

            log.debug("Rewrote server URL from {} to {}", originalServiceUrl, gatewayUrl);
            return modified;
        } catch (Exception e) {
            log.error("Error rewriting server URLs: {}", e.getMessage());
            return apiDocsJson;
        }
    }
}
