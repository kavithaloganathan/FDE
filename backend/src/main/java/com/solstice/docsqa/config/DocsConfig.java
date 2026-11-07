package com.solstice.docsqa.config;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.solstice.docsqa.model.DocsFile;
import com.solstice.docsqa.search.Synonyms;
import com.solstice.docsqa.search.Tokenizer;
import com.solstice.docsqa.service.DocsQaService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Map;

@Configuration
public class DocsConfig {

    @Bean
    Synonyms synonyms(ObjectMapper mapper) throws IOException {
        try (InputStream in = new ClassPathResource("data/synonyms.json").getInputStream()) {
            return new Synonyms(mapper.readValue(in, new TypeReference<Map<String, List<String>>>() {}));
        }
    }

    @Bean
    DocsQaService docsQaService(ObjectMapper mapper, Synonyms synonyms) throws IOException {
        try (InputStream in = new ClassPathResource("data/docs.json").getInputStream()) {
            return new DocsQaService(mapper.readValue(in, DocsFile.class).docs(), new Tokenizer(synonyms));
        }
    }
}
