package com.xhzb.test;

import org.junit.jupiter.api.Test;
import org.springframework.ai.document.Document;
import org.springframework.ai.rag.Query;
import org.springframework.ai.rag.retrieval.search.DocumentRetriever;
import org.springframework.ai.rag.retrieval.search.VectorStoreDocumentRetriever;
import org.springframework.ai.reader.ExtractedTextFormatter;
import org.springframework.ai.reader.pdf.PagePdfDocumentReader;
import org.springframework.ai.reader.pdf.ParagraphPdfDocumentReader;
import org.springframework.ai.reader.pdf.config.PdfDocumentReaderConfig;
import org.springframework.ai.transformer.splitter.TextSplitter;
import org.springframework.ai.transformer.splitter.TokenTextSplitter;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.io.InputStreamResource;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.List;

@SpringBootTest
public class RedisVectorTest {

    @Autowired
    private VectorStore vectorStore;

    @Test
    public void test(){
        Document doc1 = new Document("延庆区位于北京西北部，以山地为主（山区占72.8%），拥有海陀山等自然景观，空气质量优异（2025年7月AQI达优级），是北京市生态涵养核心区。" );
        Document doc2 = new Document("北京八达岭长城是世界文化遗产，明代长城最精华段，素有“北门锁钥”之称，是万里长城的重要关隘与代表性景观。" );
        List<Document> list = new ArrayList<>();
        list.add(doc1);
        list.add(doc2);
        vectorStore.add(list);
    }

    @Test
    public void testPDFByPage() throws FileNotFoundException {
        // 读取文件
        InputStreamResource resource = new InputStreamResource(new FileInputStream("D:\\护理员工工作手册.pdf"));

        PagePdfDocumentReader pdfReader = new PagePdfDocumentReader(resource,
                PdfDocumentReaderConfig.builder()
                        .withPageTopMargin(0) // 设置页眉边距
                        .withPageExtractedTextFormatter(ExtractedTextFormatter.builder()
                                .withNumberOfTopTextLinesToDelete(0) // 删除页眉顶部的文本行数
                                .build())
                        .withPagesPerDocument(1) // 每个文档的页数
                        .build());

        System.out.println(pdfReader.read());
    }

    @Test
    public void testPDFByParagraph() throws FileNotFoundException {
        // 读取文件
        InputStreamResource resource = new InputStreamResource(new FileInputStream("D:\\护理员工工作手册.pdf"));

        ParagraphPdfDocumentReader pdfReader = new ParagraphPdfDocumentReader(resource,
                PdfDocumentReaderConfig.builder()
                        .withPageTopMargin(0) // 设置页眉边距
                        .withPageExtractedTextFormatter(ExtractedTextFormatter.builder()
                                .withNumberOfTopTextLinesToDelete(0) // 删除页眉顶部的文本行数
                                .build())
                        .withPagesPerDocument(1) // 每个文档的页数
                        .build());

        System.out.println(pdfReader.read());
    }

    @Test
    public void testTestSplitter() throws FileNotFoundException {

        // 读取文件
        InputStreamResource resource = new InputStreamResource(new FileInputStream("D:\\护理员工工作手册.pdf"));

        PagePdfDocumentReader pdfReader = new PagePdfDocumentReader(resource,
                PdfDocumentReaderConfig.builder()
                        .withPageTopMargin(0) // 设置页眉边距
                        .withPageExtractedTextFormatter(ExtractedTextFormatter.builder()
                                .withNumberOfTopTextLinesToDelete(0) // 删除页眉顶部的文本行数
                                .build())
                        .withPagesPerDocument(1) // 每个文档的页数
                        .build());


        // 创建TextSplitter
        TokenTextSplitter textSplitter = TokenTextSplitter.builder()
                .withChunkSize(1000)
                .withMinChunkSizeChars(400)
                .withMinChunkLengthToEmbed(10)
                .withMaxNumChunks(5000)
                .withKeepSeparator(true)
                .build();
        System.out.println("分隔之前的文档数："+pdfReader.read().size());
        List<Document> documents = textSplitter.apply(pdfReader.read());
        System.out.println("分隔之后的文档数："+documents.size());
        System.out.println(documents);
//存储到向量数据库中，分批添加，不然会报错
        for (int i = 0; i < documents.size(); i += 10) {
            vectorStore.add(documents.subList(i, Math.min(i + 10, documents.size())));
        }
    }

    @Test
    public void testRetriever() {
        DocumentRetriever retriever = VectorStoreDocumentRetriever.builder()
                .vectorStore(vectorStore)
                .similarityThreshold(0.5) // 设置相似度阈值
                .topK(5) // 设置返回的文档数量
                .build();
        List<Document> documents = retriever.retrieve(new Query("护理服务宗旨与核心价值是什么"));
        System.out.println(documents);
    }
}
