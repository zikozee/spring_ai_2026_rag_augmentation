
## RAG AUGMENTATION
### three key pieces of a typical RAG system
- The document loader is responsible for loading documents into a vector store.
- The vector store is where documents are stored and can be later looked up.
- The RAG-enabled application submits queries to the vector store to find documents that are similar (and presumably relevant) to a question.

### here’s what happens in such a RAG system:
1. Documents of any size are loaded and split into smaller documents.
   The splitting strategy employed is up to you, but a common approach is to ensure that no document chunk exceeds a certain number of tokens.
2. The contents of each chunk are assigned a set of coordinates in multidimensional space based on attributes of the content.
   These coordinates are called embeddings.
3. The individual document chunks are written to a vector store. A vector store is a kind of database that enables you to search for document chunks based on their embeddings.
4. When a question is asked, embeddings are calculated for the question itself and the question’s embeddings are sent as query parameters to the vector store to locate only the document chunks that are closest to the question in multidimensional space.
5. The top handful of document chunks that are returned from the vector store will go into the prompt as context.

### Spring Libraries:
- Spring AI OpenAI—This will be used primarily to access OpenAI’s embedding API. You can optionally use a different AI service instead of OpenAI. If you do, then it’s important to understand that different embedding models aren’t cross-compatible. As such, you’ll want to be sure that you use the same or a compatible embedding API in the game rules application.
- Spring MVC—You’ll need Spring MVC because Spring AI requires it.
- Spring AI Qdrant—This is the client library for the vector store that the loader will be writing documents to.
- Spring AI Tika Document Reader—This is a Spring AI document reader based on Apache Tika and is capable of reading many different types of files.
- Spring Function Catalog—Specifically, you’ll use the fileSupplier function from the Spring Function Catalog (https://mng.bz/pZaR) to watch a directory for new files and send them to a custom consumer that writes them to the vector store. The file supplier is shown in the leftmost box in figure 4.2. The other box is a custom component you’ll build in this section.
- Spring Cloud Function—Spring Cloud Function (https://spring.io/projects/spring-cloud-function) will be used to coordinate the interaction between the file supplier and the custom consumer.

### Defining the loader pipeline 
- Note we could have achieved same with **Spring Batch** passing from one step to the next, 
  - but Spring Cloud Function is more lightweight and easier to set up for this use case
  
- Spring Cloud Function lets you define a function that is composed of one or more other functions by setting 
  - the spring.cloud.function.definition with a pipe-delimiter separating the individual functions that make up the composed function
  - example: `spring.cloud.function.definition=fileSupplier|documentReader|splitter|titleDeterminer|vectorStoreConsumer`
  
- _**file.supplier.directory**_ where to read the files from 
- _**file.supplier.filename-regex**_ to specify a regular expression that will be used to filter the files in the directory. 
  - Only files whose names match the regular expression will be processed by the file supplier. 
    - For example, if you only want to process text files, you could set this property to `.*\.txt$` to match any file that ends with .txt.

- note the name of the functions that are pipe delimited correspond beans that should be in the Spring application context.    
  - **fileSupplier** bean is provided by Spring Function Catalog and autoconfigured for you
  - **documentReader**, which is an implementation of Spring AI’s DocumentReader interface that takes the file it is given and reads it into a Document
    - the document object is then handed over to the next function in the pipeline, which is the splitter
  - **splitter** function is an implementation of Spring AI’s TextSplitter. 
    - It receives the Document and splits it into smaller chunks, returned as a list of Document objects
  - **vectorConsumer** receives the list of Document and which writes the document chunks into the vector store via Spring AI’s VectorStore interface