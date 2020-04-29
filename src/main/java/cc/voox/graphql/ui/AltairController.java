package cc.voox.graphql.ui;

import cc.voox.graphql.GraphqlProperties;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.text.StrSubstitutor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Controller;
import org.springframework.util.StreamUtils;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import javax.annotation.PostConstruct;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

@Controller
public class AltairController {

    private static final String CDN_UNPKG = "//unpkg.com/";
    private static final String ALTAIR = "altair-static";

    @Autowired
    private GraphqlProperties graphqlProperties;

    @Autowired
    private Environment environment;

    private String template;
    private String props;
    private String headers;

    @PostConstruct
    public void onceConstructed() throws IOException {
        loadTemplate();
        loadProps();
        loadHeaders();
    }

    private void loadTemplate() throws IOException {
        try (InputStream inputStream = new ClassPathResource("altair.html").getInputStream()) {
            template = StreamUtils.copyToString(inputStream, Charset.defaultCharset());
        }
    }

    private void loadProps() throws IOException {
        props = new PropsLoader(environment).load();
    }

    private void loadHeaders() throws JsonProcessingException {
        PropertyGroupReader propertyReader = new PropertyGroupReader(environment, "graphiql.headers.");
        Properties headerProperties = propertyReader.load();
        this.headers = new ObjectMapper().writeValueAsString(headerProperties);
    }

    @RequestMapping(value = "/altair")
    public void altair(HttpServletRequest request, HttpServletResponse response, @PathVariable Map<String, String> params) throws IOException {
        response.setContentType("text/html; charset=UTF-8");

        Map<String, String> replacements = getReplacements(
                constructGraphQlEndpoint(request, params),
                request.getContextPath() + graphqlProperties.getEndpoint().getSubscriptions()
        );

        String populatedTemplate = StrSubstitutor.replace(template, replacements);
        response.getOutputStream().write(populatedTemplate.getBytes(Charset.defaultCharset()));
    }

    private Map<String, String> getReplacements(String graphqlEndpoint, String subscriptionsEndpoint) {
        Map<String, String> replacements = new HashMap<>();
        replacements.put("graphqlEndpoint", graphqlEndpoint);
        replacements.put("subscriptionsEndpoint", subscriptionsEndpoint);
        replacements.put("pageTitle", graphqlProperties.getPageTitle());
        replacements.put("pageFavicon", getResourceUrl("favicon.ico", "favicon.ico"));
        replacements.put("altairBaseUrl", getResourceUrl(StringUtils.join(graphqlProperties.getSTATIC().getBasePath(), "/vendor/altair/"),
                joinJsUnpkgPath(ALTAIR, graphqlProperties.getCdn().getVersion(), "build/dist/")));
        replacements.put("altairLogoUrl", getResourceUrl("assets/img/logo_350.svg", "assets/img/logo_350.svg"));
        replacements.put("altairCssUrl", getResourceUrl("styles.css", "styles.css"));
        replacements.put("altairMainJsUrl", getResourceUrl("main.js", "main.js"));
        replacements.put("altairPolyfillsJsUrl", getResourceUrl("polyfills.js", "polyfills.js"));
        replacements.put("altairRuntimeJsUrl", getResourceUrl("runtime.js", "runtime.js"));
        replacements.put("props", props);
        replacements.put("headers", headers);
        return replacements;
    }

    private String getResourceUrl(String staticFileName, String cdnUrl) {
        if (graphqlProperties.getCdn().isEnabled() && StringUtils.isNotBlank(cdnUrl)) {
            return cdnUrl;
        }
        return staticFileName;
    }

    private String joinJsUnpkgPath(String library, String cdnVersion, String cdnFileName) {
        return CDN_UNPKG + library + "@" + cdnVersion + "/" + cdnFileName;
    }

    private String constructGraphQlEndpoint(HttpServletRequest request, @RequestParam Map<String, String> params) {
        String endpoint = graphqlProperties.getEndpoint().getGraphql();
        for (Map.Entry<String, String> param : params.entrySet()) {
            endpoint = endpoint.replaceAll("\\{" + param.getKey() + "}", param.getValue());
        }
        if (StringUtils.isNotBlank(request.getContextPath()) && !endpoint.startsWith(request.getContextPath())) {
            return request.getContextPath() + endpoint;
        }
        return endpoint;
    }

}