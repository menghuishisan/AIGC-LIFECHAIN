package com.lifechain.infra.milvus;

import com.alibaba.fastjson.JSONObject;
import io.milvus.v2.client.ConnectConfig;
import io.milvus.v2.client.MilvusClientV2;
import io.milvus.v2.common.DataType;
import io.milvus.v2.common.IndexParam;
import io.milvus.v2.service.collection.request.AddFieldReq;
import io.milvus.v2.service.collection.request.CreateCollectionReq;
import io.milvus.v2.service.collection.request.HasCollectionReq;
import io.milvus.v2.service.collection.request.LoadCollectionReq;
import io.milvus.v2.service.vector.request.InsertReq;
import io.milvus.v2.service.vector.request.SearchReq;
import io.milvus.v2.service.vector.response.SearchResp;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.nio.ByteBuffer;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;

/**
 * Milvus 向量数据库服务
 * <p>
 * 按作品类型路由到 5 个 BinaryVector(256) collection：
 * IMAGE → feat_image_pdq、VIDEO → feat_video_pdq、AUDIO → feat_audio_minhash、
 * TEXT → feat_text_minhash、MODEL → feat_model_d2。
 * 索引统一使用 BIN_IVF_FLAT + HAMMING 距离，相似度分数 = 1 - distance/256。
 * </p>
 *
 * @author LifeChain
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class MilvusService {

    private final MilvusConfig milvusConfig;
    private MilvusClientV2 client;

    /** 统一二进制向量位数 */
    public static final int VECTOR_BITS = 256;
    /** 256 bits = 32 bytes */
    public static final int VECTOR_BYTES = VECTOR_BITS / 8;

    /** 作品类型 → collection 名称映射 */
    private static final Map<String, String> COLLECTION_OF = Map.of(
            "IMAGE", "feat_image_pdq",
            "VIDEO", "feat_video_pdq",
            "AUDIO", "feat_audio_minhash",
            "TEXT", "feat_text_minhash",
            "MODEL", "feat_model_d2"
    );

    @PostConstruct
    public void init() {
        ConnectConfig config = ConnectConfig.builder()
                .uri("http://" + milvusConfig.getHost() + ":" + milvusConfig.getPort())
                .build();
        client = new MilvusClientV2(config);

        for (String collection : COLLECTION_OF.values()) {
            ensureCollection(collection);
        }
        log.info("Milvus 初始化完成: host={}:{}, collections={}",
                milvusConfig.getHost(), milvusConfig.getPort(), COLLECTION_OF.values());
    }

    /**
     * 确保指定 collection 存在并已加载
     * <p>
     * Schema：work_id (Int64 主键) + feature_vector (BinaryVector 256 bit)。
     * 索引：BIN_IVF_FLAT + HAMMING + nlist=128。
     * </p>
     */
    private void ensureCollection(String collectionName) {
        boolean exists = client.hasCollection(HasCollectionReq.builder()
                .collectionName(collectionName).build());
        if (!exists) {
            CreateCollectionReq.CollectionSchema schema = CreateCollectionReq.CollectionSchema.builder().build();
            schema.addField(AddFieldReq.builder()
                    .fieldName("work_id").dataType(DataType.Int64).isPrimaryKey(true).build());
            schema.addField(AddFieldReq.builder()
                    .fieldName("feature_vector").dataType(DataType.BinaryVector).dimension(VECTOR_BITS).build());

            List<IndexParam> indexes = List.of(
                    IndexParam.builder().fieldName("feature_vector")
                            .indexType(IndexParam.IndexType.BIN_IVF_FLAT)
                            .metricType(IndexParam.MetricType.HAMMING)
                            .extraParams(Map.of("nlist", 128))
                            .build()
            );

            client.createCollection(CreateCollectionReq.builder()
                    .collectionName(collectionName)
                    .collectionSchema(schema)
                    .indexParams(indexes)
                    .build());
            log.info("Milvus collection 创建: {}", collectionName);
        }
        client.loadCollection(LoadCollectionReq.builder()
                .collectionName(collectionName).build());
    }

    /**
     * 插入作品特征
     *
     * @param workId          作品ID
     * @param workType        作品类型（决定 collection 路由）
     * @param perceptualHash  256-bit 指纹的 hex 编码（64 字符）
     */
    public void insert(Long workId, String workType, String perceptualHash) {
        String collection = collectionFor(workType);
        ByteBuffer vector = hexToByteBuffer(perceptualHash);

        JSONObject row = new JSONObject();
        row.put("work_id", workId);
        row.put("feature_vector", vector);

        client.insert(InsertReq.builder()
                .collectionName(collection)
                .data(List.of(row))
                .build());
    }

    /**
     * 检索相似作品
     *
     * @param workType        作品类型
     * @param perceptualHash  待查指纹 hex
     * @param topK            返回 Top-K
     * @return Milvus 搜索结果（distance 即汉明距离）
     */
    public List<SearchResp.SearchResult> searchSimilar(String workType, String perceptualHash, int topK) {
        String collection = collectionFor(workType);
        ByteBuffer vector = hexToByteBuffer(perceptualHash);

        SearchResp resp = client.search(SearchReq.builder()
                .collectionName(collection)
                .data(List.of(vector))
                .topK(topK)
                .outputFields(List.of("work_id"))
                .build());

        if (resp.getSearchResults().isEmpty()) {
            return List.of();
        }
        return resp.getSearchResults().get(0);
    }

    /** 将 hex 字符串转换为 Milvus BinaryVector 所需的 ByteBuffer */
    private ByteBuffer hexToByteBuffer(String hex) {
        byte[] bytes = HexFormat.of().parseHex(hex);
        if (bytes.length != VECTOR_BYTES) {
            throw new IllegalArgumentException(
                    "感知哈希长度必须为 " + VECTOR_BYTES + " 字节（hex " + (VECTOR_BYTES * 2) + " 字符），实际：" + bytes.length);
        }
        ByteBuffer buf = ByteBuffer.allocate(VECTOR_BYTES);
        buf.put(bytes);
        buf.rewind();
        return buf;
    }

    /** 作品类型对应的 collection 名 */
    private String collectionFor(String workType) {
        String collection = COLLECTION_OF.get(workType);
        if (collection == null) {
            throw new IllegalArgumentException("不支持的作品类型: " + workType);
        }
        return collection;
    }
}
