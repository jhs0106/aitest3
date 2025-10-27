package edu.sm.app.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.document.Document;
import org.springframework.ai.reader.TextReader;
import org.springframework.ai.reader.pdf.PagePdfDocumentReader;
import org.springframework.ai.reader.tika.TikaDocumentReader;
import org.springframework.ai.transformer.splitter.TokenTextSplitter;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@Service
@Slf4j
@RequiredArgsConstructor
public class LawDocEtlService {

    private final VectorStore vectorStore;
    private final JdbcTemplate jdbcTemplate;

    public String ingest(String lawType, MultipartFile file) throws IOException {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("업로드할 파일이 없습니다.");
        }

        String normalizedType = StringUtils.hasText(lawType) ? lawType : "unknown";
        String originalFilename = file.getOriginalFilename();
        log.info("법률 문서 ETL 시작 - 파일: {}, 유형: {}", originalFilename, normalizedType);

        Resource resource = new ByteArrayResource(file.getBytes());
        List<Document> documents = readDocuments(resource, file.getContentType(), originalFilename);
        if (documents == null || documents.isEmpty()) {
            throw new IllegalArgumentException("지원하지 않는 파일 형식입니다. PDF, TXT, DOCX 파일만 업로드 가능합니다.");
        }

        for (Document document : documents) {
            Map<String, Object> metadata = document.getMetadata();
            metadata.put("type", normalizedType);
            metadata.put("source", originalFilename);
        }

        TokenTextSplitter splitter = new TokenTextSplitter();
        List<Document> chunks = splitter.apply(documents);
        log.info("문서 청크 생성 완료 - 총 {}건", chunks.size());

        vectorStore.add(chunks);
        log.info("pgvector에 {}건 저장 완료", chunks.size());

        return String.format(Locale.KOREA,
                "✅ %s 파일이 성공적으로 업로드되었습니다. (%d개 청크 저장)",
                originalFilename,
                chunks.size());
    }

    public void clearVectorStore() {
        jdbcTemplate.update("TRUNCATE TABLE vector_store");
        log.info("pgvector 테이블 초기화 완료");
    }

    public Map<String, Object> getVectorStats() {
        Map<String, Object> stats = new HashMap<>();
        Integer total = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM vector_store", Integer.class);
        Integer criminal = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM vector_store WHERE metadata ->> 'type' = 'criminal_law'", Integer.class);
        Integer civil = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM vector_store WHERE metadata ->> 'type' = 'civil_law'", Integer.class);

        stats.put("totalDocuments", total != null ? total : 0);
        stats.put("criminalLaw", criminal != null ? criminal : 0);
        stats.put("civilLaw", civil != null ? civil : 0);
        stats.put("totalEmbeddings", total != null ? total : 0);
        return stats;
    }

    private List<Document> readDocuments(Resource resource, String contentType, String filename) throws IOException {
        String lowerContentType = contentType != null ? contentType.toLowerCase(Locale.ROOT) : "";
        if (lowerContentType.equals("text/plain")) {
            return new TextReader(resource).read();
        }
        if (lowerContentType.equals("application/pdf")) {
            return new PagePdfDocumentReader(resource).read();
        }
        if (lowerContentType.contains("wordprocessingml")) {
            return new TikaDocumentReader(resource).read();
        }

        // content-type이 비어 있거나 예외적인 경우 확장자로 판별
        if (filename != null) {
            String lowerCaseName = filename.toLowerCase(Locale.ROOT);
            if (lowerCaseName.endsWith(".txt")) {
                return new TextReader(resource).read();
            }
            if (lowerCaseName.endsWith(".pdf")) {
                return new PagePdfDocumentReader(resource).read();
            }
            if (lowerCaseName.endsWith(".doc") || lowerCaseName.endsWith(".docx")) {
                return new TikaDocumentReader(resource).read();
            }
        }

        return List.of();
    }
}