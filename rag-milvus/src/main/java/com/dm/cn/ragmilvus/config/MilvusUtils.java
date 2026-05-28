package com.dm.cn.ragmilvus.config;

import com.dm.cn.ragmilvus.model.MilvusVo;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.milvus.client.MilvusServiceClient;
import io.milvus.common.clientenum.ConsistencyLevelEnum;
import io.milvus.grpc.MutationResult;
import io.milvus.grpc.SearchResults;
import io.milvus.param.IndexType;
import io.milvus.param.MetricType;
import io.milvus.param.R;
import io.milvus.param.collection.CreateCollectionParam;
import io.milvus.param.collection.FieldType;
import io.milvus.param.collection.FlushParam;
import io.milvus.param.dml.InsertParam;
import io.milvus.param.dml.SearchParam;
import io.milvus.param.dml.UpsertParam;
import io.milvus.param.highlevel.dml.DeleteIdsParam;
import io.milvus.param.highlevel.dml.response.DeleteResponse;
import io.milvus.param.index.CreateIndexParam;
import io.milvus.response.SearchResultsWrapper;
import org.springframework.ai.document.Document;
import org.springframework.ai.embedding.EmbeddingModel;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class MilvusUtils {

    public void createCollection(MilvusServiceClient milvusClient, String milvusDatabaseName, String milvusCollectionName) {

        // 检查Collection是否存在，不存在则创建
        boolean exists = milvusClient.hasCollection(
                io.milvus.param.collection.HasCollectionParam.newBuilder()
                        .withCollectionName(milvusCollectionName)
                        .withDatabaseName(milvusDatabaseName)
                        .build()
        ).getData();

        if (!exists) {
            // ========== 1. 主键字段：id ==========
            FieldType idField = FieldType.newBuilder()
                    .withName("id")
                    .withDataType(io.milvus.grpc.DataType.VarChar)
                    .withPrimaryKey(true)
                    .withAutoID(false)
                    .withMaxLength(64)
                    .build();

            // ========== 2. 向量字段：embedding ==========
            FieldType vectorField = FieldType.newBuilder()
                    .withName("embedding")
                    .withDataType(io.milvus.grpc.DataType.FloatVector)
                    .withDimension(1024)
                    .build();

            // ========== 3. 标量字段：chunk ==========
            FieldType chunkField = FieldType.newBuilder()
                    .withName("chunk")
                    .withDataType(io.milvus.grpc.DataType.VarChar)
                    .withMaxLength(16384)
                    .build();

            // ========== 4. 补充其他字段 ==========
            FieldType docIdField = FieldType.newBuilder()
                    .withName("doc_id")
                    .withDataType(io.milvus.grpc.DataType.VarChar)
                    .withMaxLength(64)
                    .build();

            FieldType ownerIdField = FieldType.newBuilder()
                    .withName("owner_id")
                    .withDataType(io.milvus.grpc.DataType.Int64)
                    .build();

            FieldType visibilityField = FieldType.newBuilder()
                    .withName("visibility")
                    .withDataType(io.milvus.grpc.DataType.Int8)
                    .build();

            FieldType groupIdsCsvField = FieldType.newBuilder()
                    .withName("group_ids_csv")
                    .withDataType(io.milvus.grpc.DataType.VarChar)
                    .withMaxLength(512)
                    .build();

            FieldType sourceField = FieldType.newBuilder()
                    .withName("source")
                    .withDataType(io.milvus.grpc.DataType.VarChar)
                    .withMaxLength(256)
                    .build();

            FieldType chunkIndexField = FieldType.newBuilder()
                    .withName("chunk_index")
                    .withDataType(io.milvus.grpc.DataType.Int32)
                    .build();


            // ========== 5. 创建Collection（加入所有字段） ==========
            milvusClient.createCollection(
                    CreateCollectionParam.newBuilder()
                            .withCollectionName(milvusCollectionName)
                            .withDatabaseName(milvusDatabaseName)
                            .addFieldType(idField)
                            .addFieldType(vectorField)
                            .addFieldType(docIdField)
                            .addFieldType(ownerIdField)
                            .addFieldType(visibilityField)
                            .addFieldType(groupIdsCsvField)
                            .addFieldType(sourceField)
                            .addFieldType(chunkIndexField)
                            .addFieldType(chunkField)
                            .withShardsNum(1)
                            .build()
            );

            // ========== 6. 创建向量索引 ==========
            milvusClient.createIndex(
                    CreateIndexParam.newBuilder()
                            .withCollectionName(milvusCollectionName)
                            .withDatabaseName(milvusDatabaseName)
                            .withFieldName("embedding")
                            .withIndexType(IndexType.HNSW)
                            .withMetricType(MetricType.COSINE)
                            .withExtraParam("{\"M\":16, \"efConstruction\":256}")
                            .build()
            );

            // ========== 7. 加载Collection到内存 ==========
            milvusClient.loadCollection(
                    io.milvus.param.collection.LoadCollectionParam.newBuilder()
                            .withCollectionName(milvusCollectionName)
                            .withDatabaseName(milvusDatabaseName)
                            .build()
            );
        }
    }

    public void insert(MilvusServiceClient milvusClient, String milvusCollectionName, EmbeddingModel embeddingModel, List<Document> documents) {
        // 构建参数
        List<InsertParam.Field> fields = getFields(embeddingModel, documents);
        InsertParam insertParam = InsertParam.newBuilder()
                .withCollectionName(milvusCollectionName)
                .withFields(fields)
                .build();

        // 执行插入
        R<MutationResult> response = milvusClient.insert(insertParam);
        if (response.getStatus() != R.Status.Success.getCode()) {
            System.out.println(response.getMessage());
        }

        // 刷新集合
        flush(milvusClient, milvusCollectionName);
    }

    public void update(MilvusServiceClient milvusClient, String milvusCollectionName, EmbeddingModel embeddingModel, List<Document> documents) {
        // 构建参数
        List<InsertParam.Field> fields = getFields(embeddingModel, documents);
        UpsertParam insertParam = UpsertParam.newBuilder()
                .withCollectionName(milvusCollectionName)
                .withFields(fields)
                .build();

        // 执行更新
        R<MutationResult> response = milvusClient.upsert(insertParam);
        if (response.getStatus() != R.Status.Success.getCode()) {
            System.out.println(response.getMessage());
        }

        // 刷新集合
        flush(milvusClient, milvusCollectionName);
    }

    public List<MilvusVo> search(MilvusServiceClient milvusClient, String milvusCollectionName, EmbeddingModel embeddingModel,
                                 String keyword, Integer topK, String filter) {
        List<MilvusVo> result = new ArrayList<>();

        float[] floatArray = embeddingModel.embed(keyword);
        List<List<Float>> targetVectors = List.of(IntStream.range(0, floatArray.length)
                .mapToObj(i -> floatArray[i])
                .collect(Collectors.toList()));

        SearchParam param = SearchParam.newBuilder()
                .withCollectionName(milvusCollectionName)
                .withMetricType(MetricType.COSINE)
                .withVectors(targetVectors)
                .withTopK(topK)
                .withVectorFieldName("embedding")
                .withConsistencyLevel(ConsistencyLevelEnum.EVENTUALLY)
                .withOutFields(Arrays.asList("*"))
                .withExpr(filter)
                .withParams("{\"ef\":20}")
                .build();

        R<SearchResults> response = milvusClient.search(param);
        if (response.getStatus() != R.Status.Success.getCode()) {
            System.out.println(response.getMessage());
        }

        SearchResultsWrapper wrapper = new SearchResultsWrapper(response.getData().getResults());
        for (int i = 0; i < targetVectors.size(); ++i) {
            List<SearchResultsWrapper.IDScore> scores = wrapper.getIDScore(i);
            for (SearchResultsWrapper.IDScore score : scores) {
                Float scoreValue = score.getScore();
                Map<String, Object> fieldValues = score.getFieldValues();
                MilvusVo vo = new ObjectMapper().convertValue(fieldValues, MilvusVo.class);
                vo.setSimilarityScore(scoreValue);
                result.add(vo);
            }
        }
        return result;
    }

    public void delete(MilvusServiceClient milvusClient, String milvusCollectionName, List<String> idList) {
        // 构建参数
        DeleteIdsParam param = DeleteIdsParam.newBuilder()
                .withCollectionName(milvusCollectionName)
                .withPrimaryIds(idList)
                .build();

        // 执行删除
        R<DeleteResponse> response = milvusClient.delete(param);
        if (response.getStatus() != R.Status.Success.getCode()) {
            System.out.println(response.getMessage());
        }

        // 刷新集合
        flush(milvusClient, milvusCollectionName);
    }

    private List<InsertParam.Field> getFields(EmbeddingModel embeddingModel, List<Document> documents) {
        // 准备插入数据：按字段分组
        List<String> idList = new ArrayList<>();
        List<List<Float>> vectorList = new ArrayList<>();
        List<String> docIdList = new ArrayList<>();
        List<Long> ownerIdList = new ArrayList<>();
        List<Integer> visibilityList = new ArrayList<>();
        List<String> groupIdsCsvList = new ArrayList<>();
        List<String> sourceList = new ArrayList<>();
        List<Integer> chunkIndexList = new ArrayList<>();
        List<String> chunkList = new ArrayList<>();

        for (Document doc : documents) {
            idList.add(doc.getMetadata().get("id").toString());
            docIdList.add(doc.getMetadata().get("doc_id").toString());
            ownerIdList.add((Long) doc.getMetadata().get("owner_id"));
            visibilityList.add((Integer) doc.getMetadata().get("visibility"));
            groupIdsCsvList.add(doc.getMetadata().get("group_ids_csv").toString());
            sourceList.add(doc.getMetadata().get("source").toString());
            chunkIndexList.add((Integer) doc.getMetadata().get("chunk_index"));
            chunkList.add(doc.getText());
            // 文本转向量
            float[] floatArray = embeddingModel.embed(doc.getText());
            List<Float> embeddings = IntStream.range(0, floatArray.length)
                    .mapToObj(i -> floatArray[i])
                    .collect(Collectors.toList());
            vectorList.add(embeddings);
        }

        // 构建参数
        List<InsertParam.Field> fields = new ArrayList<>();
        fields.add(new InsertParam.Field("id", idList));
        fields.add(new InsertParam.Field("embedding", vectorList));
        fields.add(new InsertParam.Field("doc_id", docIdList));
        fields.add(new InsertParam.Field("owner_id", ownerIdList));
        fields.add(new InsertParam.Field("visibility", visibilityList));
        fields.add(new InsertParam.Field("group_ids_csv", groupIdsCsvList));
        fields.add(new InsertParam.Field("source", sourceList));
        fields.add(new InsertParam.Field("chunk_index", chunkIndexList));
        fields.add(new InsertParam.Field("chunk", chunkList));

        return fields;
    }

    private void flush(MilvusServiceClient milvusClient, String milvusCollectionName) {
        // 刷新集合（确保数据立即可查）
        FlushParam flushParam = FlushParam.newBuilder()
                .addCollectionName(milvusCollectionName)
                .build();
        milvusClient.flush(flushParam);
    }
}