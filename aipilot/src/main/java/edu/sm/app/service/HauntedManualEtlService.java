package edu.sm.app.service;

import org.springframework.ai.chat.client.advisor.vectorstore.QuestionAnswerAdvisor;
import org.springframework.ai.document.Document;
import org.springframework.ai.document.DocumentReader;
import org.springframework.ai.reader.TextReader;
import org.springframework.ai.reader.pdf.PagePdfDocumentReader;
import org.springframework.ai.reader.tika.TikaDocumentReader;
import org.springframework.ai.transformer.splitter.TokenTextSplitter;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;

@Service
public class HauntedManualEtlService {

    private static final List<String> SUPPORTED_EXTENSIONS = List.of(".txt", ".pdf", ".doc", ".docx");


    private final VectorStore vectorStore;
    private final JdbcTemplate jdbcTemplate;

    public HauntedManualEtlService(VectorStore vectorStore,
                                   JdbcTemplate jdbcTemplate) {
        this.vectorStore = vectorStore;
        this.jdbcTemplate = jdbcTemplate;
    }

    public String ingestManual(String scenario, MultipartFile file) throws IOException {
        List<Document> documents = extract(file);
        if (documents == null) {
            return String.format("지원하는 파일 형식(%s)을 업로드해주세요.", String.join(", ", SUPPORTED_EXTENSIONS));
        }

        String normalizedScenario = StringUtils.hasText(scenario) ? StringUtils.trimWhitespace(scenario) : "";

        for (Document document : documents) {
            if (StringUtils.hasText(normalizedScenario)) {
                document.getMetadata().put("scenario", normalizedScenario);
            }
        }

        List<Document> chunks = transform(documents);
        vectorStore.add(chunks);

        StringBuilder message = new StringBuilder("괴담 규칙 문서를 벡터 저장소에 적재했습니다.");
        if (StringUtils.hasText(normalizedScenario)) {
            message.append(" (시나리오: ").append(normalizedScenario).append(")");
        }
        return message.toString();
    }

    public void clearAll() {
        jdbcTemplate.update("TRUNCATE TABLE vector_store");
    }

    public QuestionAnswerAdvisor createAdvisor(String scenario) {
        SearchRequest.Builder requestBuilder = SearchRequest.builder()
                .similarityThreshold(0.0)
                .topK(3);

        StringBuilder filterBuilder = new StringBuilder();
        if (StringUtils.hasText(scenario)) {
            filterBuilder.append("scenario == '")
                    .append(escapeValue(StringUtils.trimWhitespace(scenario)))
                    .append("'");
        }
        if (filterBuilder.length() > 0) {
            requestBuilder.filterExpression(filterBuilder.toString());
        }

        return QuestionAnswerAdvisor.builder(vectorStore)
                .searchRequest(requestBuilder.build())
                .build();
    }

    /**
     * 업로드 화면에서 datalist 옵션을 채우기 위해 벡터스토어에 이미 적재된 시나리오 이름을 반환한다.
     * <p>
     * 사용자가 과거에 등록했던 시나리오를 다시 선택할 수 있도록 프런트엔드가 호출한다.
     * </p>
     */
    public List<String> listScenarios() {
        return jdbcTemplate.query(
                "SELECT DISTINCT TRIM(metadata->>'scenario') AS scenario " +
                        "FROM vector_store " +
                        "WHERE metadata->>'scenario' IS NOT NULL " +
                        "AND TRIM(metadata->>'scenario') <> '' " +
                        "ORDER BY scenario",
                (rs, rowNum) -> {
                    String value = rs.getString("scenario");
                    return value != null ? StringUtils.trimWhitespace(value) : value;
                }
        );
    }

    public List<String> supportedExtensions() {
        return SUPPORTED_EXTENSIONS;
    }

    private List<Document> extract(MultipartFile file) throws IOException {
        Resource resource = new ByteArrayResource(file.getBytes());
        List<Document> documents = null;
        String contentType = file.getContentType();
        if ("text/plain".equals(contentType)) {
            DocumentReader reader = new TextReader(resource);
            documents = reader.read();
        } else if ("application/pdf".equals(contentType)) {
            DocumentReader reader = new PagePdfDocumentReader(resource);
            documents = reader.read();
        } else if (contentType != null && (contentType.contains("wordprocessingml") || contentType.contains("msword"))) {
            DocumentReader reader = new TikaDocumentReader(resource);
            documents = reader.read();
        } else {
            String originalFilename = file.getOriginalFilename();
            if (StringUtils.hasText(originalFilename)) {
                String lowered = originalFilename.toLowerCase();
                if (lowered.endsWith(".txt")) {
                    DocumentReader reader = new TextReader(resource);
                    documents = reader.read();
                } else if (lowered.endsWith(".pdf")) {
                    DocumentReader reader = new PagePdfDocumentReader(resource);
                    documents = reader.read();
                } else if (lowered.endsWith(".doc") || lowered.endsWith(".docx")) {
                    DocumentReader reader = new TikaDocumentReader(resource);
                    documents = reader.read();
                }
            }
        }
        return documents;
    }

    private List<Document> transform(List<Document> documents) {
        TokenTextSplitter splitter = new TokenTextSplitter();
        return splitter.apply(documents);
    }

    private String escapeValue(String value) {
        return value.replace("'", "\\'");
    }
}