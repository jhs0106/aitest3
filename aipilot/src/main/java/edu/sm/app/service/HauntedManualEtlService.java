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
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.Objects;

@Service
public class HauntedManualEtlService {

    private static final List<String> SUPPORTED_EXTENSIONS = List.of(".txt", ".pdf", ".doc", ".docx");
    private static final DocumentContentAccessor DOCUMENT_CONTENT_ACCESSOR = DocumentContentAccessor.resolve();
    private static final DocumentMetadataAccessor DOCUMENT_METADATA_ACCESSOR = DocumentMetadataAccessor.resolve();


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
        if (documents.isEmpty()) {
            return "문서에서 추출한 내용이 없습니다. 다른 파일을 시도해 주세요.";
        }

        String normalizedScenario = StringUtils.hasText(scenario) ? StringUtils.trimWhitespace(scenario) : "";
        String trimmedName = null;
        String originalFilename = file.getOriginalFilename();
        if (StringUtils.hasText(originalFilename)) {
            trimmedName = StringUtils.trimWhitespace(originalFilename);
        }

        List<Document> chunks = transform(documents);
        if (chunks == null || chunks.isEmpty()) {
            return "문서를 분할할 수 없습니다. 다른 파일을 시도해 주세요.";
        }

        if (StringUtils.hasText(normalizedScenario) || StringUtils.hasText(trimmedName)) {
            ListIterator<Document> iterator = chunks.listIterator();
            while (iterator.hasNext()) {
                Document chunk = iterator.next();
                Map<String, Object> metadata = extractDocumentMetadata(chunk);
                Map<String, Object> enrichedMetadata = metadata != null ? new HashMap<>(metadata) : new HashMap<>();
                if (StringUtils.hasText(normalizedScenario)) {
                    enrichedMetadata.put("scenario", normalizedScenario);
                }
                if (StringUtils.hasText(trimmedName)) {
                    enrichedMetadata.put("source", trimmedName);
                }
                if (!Objects.equals(metadata, enrichedMetadata)) {
                    iterator.set(new Document(extractDocumentContent(chunk), enrichedMetadata));
                }
            }
        }

        vectorStore.add(chunks);

        StringBuilder message = new StringBuilder("괴담 규칙 문서를 벡터 저장소에 적재했습니다.");
        message.append(" (총 ").append(chunks.size()).append("개 청크");
        boolean hasDetails = false;
        if (StringUtils.hasText(trimmedName)) {
            message.append(hasDetails ? ", " : ": ");
            message.append("파일: ").append(trimmedName);
            hasDetails = true;
        }
        if (StringUtils.hasText(normalizedScenario)) {
            message.append(hasDetails ? ", " : ": ");
            message.append("시나리오: ").append(normalizedScenario);
            hasDetails = true;
        }
        message.append(")");
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

    private String extractDocumentContent(Document document) {
        return DOCUMENT_CONTENT_ACCESSOR.read(document);
    }

    private Map<String, Object> extractDocumentMetadata(Document document) {
        return DOCUMENT_METADATA_ACCESSOR.read(document);
    }

    private static final class DocumentContentAccessor {
        private final Method getContentMethod;
        private final Method contentMethod;
        private final Field contentField;

        private DocumentContentAccessor(Method getContentMethod, Method contentMethod, Field contentField) {
            this.getContentMethod = getContentMethod;
            this.contentMethod = contentMethod;
            this.contentField = contentField;
        }

        private static DocumentContentAccessor resolve() {
            Method getContent = resolveDocumentContentMethod("getContent");
            Method content = resolveDocumentContentMethod("content");
            Method getText = resolveDocumentContentMethod("getText");
            Method text = resolveDocumentContentMethod("text");
            Field field = resolveDocumentContentField("content");
            Field textField = resolveDocumentContentField("text");
            if (getContent == null && content == null && getText == null && text == null && field == null && textField == null) {
                throw new IllegalStateException("문서 내용을 읽을 수 있는 접근자가 없습니다.");
            }
            Method primaryMethod = firstNonNull(getContent, content, getText, text);
            Method secondaryMethod = primaryMethod == getContent
                    ? firstNonNull(content, getText, text)
                    : primaryMethod == content
                    ? firstNonNull(getText, text)
                    : primaryMethod == getText
                    ? text
                    : null;
            Field resolvedField = field != null ? field : textField;
            return new DocumentContentAccessor(primaryMethod, secondaryMethod, resolvedField);
        }

        private String read(Document document) {
            try {
                if (getContentMethod != null) {
                    return (String) getContentMethod.invoke(document);
                }
                if (contentMethod != null) {
                    return (String) contentMethod.invoke(document);
                }
                return (String) contentField.get(document);
            } catch (IllegalAccessException | InvocationTargetException ex) {
                throw new IllegalStateException("문서 내용을 읽을 수 없습니다.", ex);
            }
        }

        private static Method resolveDocumentContentMethod(String methodName) {
            try {
                return Document.class.getMethod(methodName);
            } catch (NoSuchMethodException ex) {
                return null;
            }
        }

        private static Field resolveDocumentContentField(String fieldName) {
            try {
                Field field = Document.class.getDeclaredField(fieldName);
                field.setAccessible(true);
                return field;
            } catch (NoSuchFieldException | SecurityException ex) {
                return null;
            }
        }

        @SafeVarargs
        private static <T> T firstNonNull(T... values) {
            for (T value : values) {
                if (value != null) {
                    return value;
                }
            }
            return null;
        }
    }

    private static final class DocumentMetadataAccessor {
        private final Method getMetadataMethod;
        private final Method metadataMethod;
        private final Field metadataField;

        private DocumentMetadataAccessor(Method getMetadataMethod, Method metadataMethod, Field metadataField) {
            this.getMetadataMethod = getMetadataMethod;
            this.metadataMethod = metadataMethod;
            this.metadataField = metadataField;
        }

        private static DocumentMetadataAccessor resolve() {
            Method getMetadata = resolveDocumentMetadataMethod("getMetadata");
            Method metadata = resolveDocumentMetadataMethod("metadata");
            Field field = resolveDocumentMetadataField();
            if (getMetadata == null && metadata == null && field == null) {
                throw new IllegalStateException("문서 메타데이터를 읽을 수 있는 접근자가 없습니다.");
            }
            return new DocumentMetadataAccessor(getMetadata, metadata, field);
        }

        @SuppressWarnings("unchecked")
        private Map<String, Object> read(Document document) {
            try {
                if (getMetadataMethod != null) {
                    return (Map<String, Object>) getMetadataMethod.invoke(document);
                }
                if (metadataMethod != null) {
                    return (Map<String, Object>) metadataMethod.invoke(document);
                }
                return (Map<String, Object>) metadataField.get(document);
            } catch (IllegalAccessException | InvocationTargetException ex) {
                throw new IllegalStateException("문서 메타데이터를 읽을 수 없습니다.", ex);
            }
        }

        private static Method resolveDocumentMetadataMethod(String methodName) {
            try {
                return Document.class.getMethod(methodName);
            } catch (NoSuchMethodException ex) {
                return null;
            }
        }

        private static Field resolveDocumentMetadataField() {
            try {
                Field field = Document.class.getDeclaredField("metadata");
                field.setAccessible(true);
                return field;
            } catch (NoSuchFieldException | SecurityException ex) {
                return null;
            }
        }
    }
}
