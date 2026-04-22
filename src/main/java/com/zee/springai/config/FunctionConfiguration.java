package com.zee.springai.config;


import com.zee.springai.dto.GameTitle;
import io.qdrant.client.QdrantClient;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.document.Document;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.reader.tika.TikaDocumentReader;
import org.springframework.ai.transformer.splitter.TokenTextSplitter;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.ai.vectorstore.qdrant.QdrantVectorStore;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationRunner;
import org.springframework.cloud.function.context.FunctionCatalog;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import reactor.core.publisher.Flux;
import reactor.core.scheduler.Schedulers;

import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.function.Function;

/**
 * @dev : Ezekiel Eromosei
 * @date : 21 Apr, 2026
 */

@Slf4j
@Configuration
public class FunctionConfiguration {

    @Bean
    ApplicationRunner go(FunctionCatalog catalog){
        Runnable composedFunction = catalog.lookup(null);
        return args -> {
            composedFunction.run();
        };
    }

    // see pipeline flow in spring.cloud.function.definition property in application.yml
    // file supplier which is autoconfigured produces a Flux<byte[]> output
    @Bean
    Function<Flux<byte[]>, Flux<Document>> documentReader(){ // second bean in the pipeline name must match
        return resourceFlux -> resourceFlux
                .map(fileBytes -> new TikaDocumentReader(new ByteArrayResource(fileBytes))
                        .get()
                        .getFirst())
                .subscribeOn(Schedulers.boundedElastic());
    }

    // documentReader produces a Flux<Document> output
    @Bean
    Function<Flux<Document>, Flux<List<Document>>> splitter(){ // third bean in the pipeline name must match
        TokenTextSplitter splitter = TokenTextSplitter.builder()
                .withChunkSize(500)
                .withMaxNumChunks(2000)
                .build(); // keep the default
        return documentFlux -> documentFlux
                .map(incoming -> splitter.apply(List.of(incoming)))
                .subscribeOn(Schedulers.boundedElastic());
    }


    //@Qualifier("openAiChatModel")
    //What if instead of relying on a naming convention to determine the title of a game,
    // we use generative AI to figure it out based on the content of the document itself
    @Bean
    Function<Flux<List<Document>>, Flux<List<Document>>> titleDeterminer(
            @Qualifier("ollamaChatModel") ChatModel chatModel,
            @Value("classpath:/promptTemplates/namOfTheGame.st") Resource nameOfTheGamePromptTemplate){ // fourth bean in the pipeline name must match

        ChatClient chatClient = ChatClient.create(chatModel);

        return documentListFlux -> documentListFlux
                .map(documents -> {
                  if(!documents.isEmpty()){
                      Document firstDocument = documents.getFirst();

                      GameTitle gameTitle = chatClient.prompt()
                              .user(userSpec -> userSpec
                                      .text(nameOfTheGamePromptTemplate)
                                      .param("document", firstDocument.getText() != null ? firstDocument.getText(): ""))
                              .call()
                              .entity(GameTitle.class);

                      if(Objects.requireNonNull(gameTitle).title().equals("UNKNOWN")){
                          log.warn("Unable to determine the name of a game; not adding to the vector store");
                          documents = Collections.emptyList();
                          return documents;
                      }

                      log.info("Determined game title: {}", gameTitle.title());
                      documents = documents.stream()
                              .peek(document -> document.getMetadata()
                                      .put("gameTitle", gameTitle.getNormalizedTitle()))
                              .toList();
                  }

                  return documents;
                });
    }


    @Bean
    Consumer<Flux<List<Document>>> vectorStoreConsumer(@Qualifier("customVectorStore") VectorStore vectorStore){ // final step in the pipeline
        return documentFlux -> documentFlux
                .doOnNext(documents -> {
                    if(!documents.isEmpty()){
                        int docCount = documents.size();
                        log.info("Writing {} documents to the vector store.", docCount);
                        vectorStore.accept(documents);

                        log.info("{} documents have been written to the vector store.", docCount);
                    }
                }).subscribe();
    }

    @Primary
    @Bean
    QdrantVectorStore customVectorStore(QdrantClient client, @Qualifier("ollamaEmbeddingModel") EmbeddingModel embeddingModel){

        return QdrantVectorStore
                .builder(client, embeddingModel)
                .initializeSchema(true)
                .collectionName("GameRules")
                .build();
    }

}
