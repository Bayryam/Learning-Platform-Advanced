package course.spring.elearningplatform.service.impl;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class ExternalNewsService {

    private final WebClient webClient;
    
    @Value("${newsapi.key}")
    private String apiKey;

    public ExternalNewsService(@Value("${newsapi.base-url}") String baseUrl) {
        this.webClient = WebClient.builder()
                .baseUrl(baseUrl)
                .build();
    }

    public Map<String, Object> getEducationalNews(int pageSize) {
        try {
            Map<String, Object> response = webClient.get()
                    .uri(uriBuilder -> uriBuilder
                            .path("/everything")
                            .queryParam("q", "education OR learning OR university OR online courses")
                            .queryParam("language", "en")
                            .queryParam("sortBy", "publishedAt")
                            .queryParam("pageSize", pageSize)
                            .queryParam("apiKey", apiKey)
                            .build())
                    .retrieve()
                    .bodyToMono(Map.class)
                    .block();

            if (response != null && "ok".equals(response.get("status"))) {
                List<Map<String, Object>> articles = (List<Map<String, Object>>) response.get("articles");
                
                // Filter out articles with removed content
                List<Map<String, Object>> validArticles = articles.stream()
                        .filter(article -> {
                            String title = (String) article.get("title");
                            return title != null && !title.contains("[Removed]");
                        })
                        .limit(pageSize)
                        .toList();

                Map<String, Object> result = new HashMap<>();
                result.put("articles", validArticles);
                result.put("totalResults", validArticles.size());
                return result;
            }

            return Map.of("articles", List.of(), "totalResults", 0);
        } catch (Exception e) {
            // Return empty results on error
            return Map.of("articles", List.of(), "totalResults", 0, "error", e.getMessage());
        }
    }
}