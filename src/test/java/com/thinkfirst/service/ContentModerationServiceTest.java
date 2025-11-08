package com.thinkfirst.service;

import com.thinkfirst.dto.ModerationResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

/**
 * Comprehensive unit tests for ContentModerationService
 * Tests content moderation with OpenAI Moderation API
 */
@ExtendWith(MockitoExtension.class)
class ContentModerationServiceTest {

    @Mock
    private WebClient openAIWebClient;

    @Mock
    private WebClient.RequestBodyUriSpec requestBodyUriSpec;

    @Mock
    private WebClient.RequestBodySpec requestBodySpec;

    @Mock
    private WebClient.RequestHeadersSpec requestHeadersSpec;

    @Mock
    private WebClient.ResponseSpec responseSpec;

    @InjectMocks
    private ContentModerationService contentModerationService;

    @BeforeEach
    void setUp() {
        // Enable moderation by default
        ReflectionTestUtils.setField(contentModerationService, "moderationEnabled", true);
    }

    @Test
    void testModerationWithSexualContent_ShouldFlagContent() {
        // Arrange
        String inappropriateContent = "This contains sexual content";
        ModerationResponse mockResponse = createModerationResponse(true, "sexual");

        setupWebClientMock(mockResponse);

        // Act
        ModerationResult result = contentModerationService.moderateContent(inappropriateContent);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.isFlagged()).isTrue();
        assertThat(result.getCategory()).isEqualTo("sexual");
        assertThat(result.getReason()).contains("inappropriate");
    }

    @Test
    void testModerationWithHateContent_ShouldFlagContent() {
        // Arrange
        String hateContent = "This contains hate speech";
        ModerationResponse mockResponse = createModerationResponse(true, "hate");

        setupWebClientMock(mockResponse);

        // Act
        ModerationResult result = contentModerationService.moderateContent(hateContent);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.isFlagged()).isTrue();
        assertThat(result.getCategory()).isEqualTo("hate");
    }

    @Test
    void testModerationWithHarassmentContent_ShouldFlagContent() {
        // Arrange
        String harassmentContent = "This contains harassment";
        ModerationResponse mockResponse = createModerationResponse(true, "harassment");

        setupWebClientMock(mockResponse);

        // Act
        ModerationResult result = contentModerationService.moderateContent(harassmentContent);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.isFlagged()).isTrue();
        assertThat(result.getCategory()).isEqualTo("harassment");
    }

    @Test
    void testModerationWithSelfHarmContent_ShouldFlagContent() {
        // Arrange
        String selfHarmContent = "This contains self-harm content";
        ModerationResponse mockResponse = createModerationResponse(true, "self-harm");

        setupWebClientMock(mockResponse);

        // Act
        ModerationResult result = contentModerationService.moderateContent(selfHarmContent);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.isFlagged()).isTrue();
        assertThat(result.getCategory()).isEqualTo("self-harm");
    }

    @Test
    void testModerationWithViolenceContent_ShouldFlagContent() {
        // Arrange
        String violenceContent = "This contains violence";
        ModerationResponse mockResponse = createModerationResponse(true, "violence");

        setupWebClientMock(mockResponse);

        // Act
        ModerationResult result = contentModerationService.moderateContent(violenceContent);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.isFlagged()).isTrue();
        assertThat(result.getCategory()).isEqualTo("violence");
    }

    @Test
    void testModerationWithSafeContent_ShouldApproveContent() {
        // Arrange
        String safeContent = "What is 2 + 2?";
        ModerationResponse mockResponse = createModerationResponse(false, null);

        setupWebClientMock(mockResponse);

        // Act
        ModerationResult result = contentModerationService.moderateContent(safeContent);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.isFlagged()).isFalse();
        assertThat(result.getCategory()).isNull();
    }

    @Test
    void testModerationWhenServiceDown_ShouldFailOpen() {
        // Arrange
        String content = "Any content";
        when(openAIWebClient.post()).thenReturn(requestBodyUriSpec);
        when(requestBodyUriSpec.uri(anyString())).thenReturn(requestBodySpec);
        when(requestBodySpec.bodyValue(any())).thenReturn(requestHeadersSpec);
        when(requestHeadersSpec.retrieve()).thenReturn(responseSpec);
        when(responseSpec.bodyToMono(ModerationResponse.class))
                .thenReturn(Mono.error(new RuntimeException("Service unavailable")));

        // Act
        ModerationResult result = contentModerationService.moderateContent(content);

        // Assert - Should fail open (allow content)
        assertThat(result).isNotNull();
        assertThat(result.isFlagged()).isFalse();
    }

    @Test
    void testModerationWhenDisabled_ShouldApproveAllContent() {
        // Arrange
        ReflectionTestUtils.setField(contentModerationService, "moderationEnabled", false);
        String content = "Any content, even inappropriate";

        // Act
        ModerationResult result = contentModerationService.moderateContent(content);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.isFlagged()).isFalse();
        verify(openAIWebClient, never()).post();
    }

    // Helper methods

    private void setupWebClientMock(ModerationResponse response) {
        when(openAIWebClient.post()).thenReturn(requestBodyUriSpec);
        when(requestBodyUriSpec.uri(anyString())).thenReturn(requestBodySpec);
        when(requestBodySpec.bodyValue(any())).thenReturn(requestHeadersSpec);
        when(requestHeadersSpec.retrieve()).thenReturn(responseSpec);
        when(responseSpec.bodyToMono(ModerationResponse.class))
                .thenReturn(Mono.just(response));
    }

    private ModerationResponse createModerationResponse(boolean flagged, String category) {
        ModerationResponse response = new ModerationResponse();
        ModerationResponse.Result result = new ModerationResponse.Result();
        result.setFlagged(flagged);
        
        if (flagged && category != null) {
            ModerationResponse.Categories categories = new ModerationResponse.Categories();
            switch (category) {
                case "sexual" -> categories.setSexual(true);
                case "hate" -> categories.setHate(true);
                case "harassment" -> categories.setHarassment(true);
                case "self-harm" -> categories.setSelfHarm(true);
                case "violence" -> categories.setViolence(true);
            }
            result.setCategories(categories);
        }
        
        response.setResults(new ModerationResponse.Result[]{result});
        return response;
    }

    // Mock response classes (these should match the actual API response structure)
    
    static class ModerationResponse {
        private Result[] results;

        public Result[] getResults() {
            return results;
        }

        public void setResults(Result[] results) {
            this.results = results;
        }

        static class Result {
            private boolean flagged;
            private Categories categories;

            public boolean isFlagged() {
                return flagged;
            }

            public void setFlagged(boolean flagged) {
                this.flagged = flagged;
            }

            public Categories getCategories() {
                return categories;
            }

            public void setCategories(Categories categories) {
                this.categories = categories;
            }
        }

        static class Categories {
            private boolean sexual;
            private boolean hate;
            private boolean harassment;
            private boolean selfHarm;
            private boolean violence;

            public boolean isSexual() {
                return sexual;
            }

            public void setSexual(boolean sexual) {
                this.sexual = sexual;
            }

            public boolean isHate() {
                return hate;
            }

            public void setHate(boolean hate) {
                this.hate = hate;
            }

            public boolean isHarassment() {
                return harassment;
            }

            public void setHarassment(boolean harassment) {
                this.harassment = harassment;
            }

            public boolean isSelfHarm() {
                return selfHarm;
            }

            public void setSelfHarm(boolean selfHarm) {
                this.selfHarm = selfHarm;
            }

            public boolean isViolence() {
                return violence;
            }

            public void setViolence(boolean violence) {
                this.violence = violence;
            }
        }
    }
}

