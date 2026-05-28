package com.dm.cn.openai.tuple;

import org.springframework.ai.chat.model.ChatModel;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;


@RestController
@RequestMapping("/rag/test")
public class TestController {

	private final ChatModel chatModel;

	public TestController(ChatModel chatModel) {
		this.chatModel = chatModel;
	}

	@GetMapping("/basic")
	public List<AggregatedRelation> basic() {
		// Step 1: 文本分块 + 分配chunk_id
		List<TextChunk> chunks = new ArrayList<>();

//		chunks.add(new TextChunk("印度的健康指标近年来有所改善，但仍落后于同类国家。"));
//		chunks.add(new TextChunk("该国人口约 13 亿，1 万人中医生和护士/助产士的活跃医务密度分别为 5.0 和 6.0，远低于世卫组织规定的每万人 44.5 名医生、护士和助产士的门槛。"));
//		chunks.add(new TextChunk("问题因州际、城乡及公私部门的分歧而更加复杂。紧急增加熟练医疗人才的呼声强化了人力资源在医疗中的核心角色，而医疗体系已演变成一个复杂的多因素问题。"));
//		chunks.add(new TextChunk("如果印度要加快实现全民健康覆盖和可持续发展目标（SDGs）的进展，必须解决技术人才短缺的问题。"));

//		chunks.add(new TextChunk("马斯克参加2026达沃斯论坛专访时说:"));
//		chunks.add(new TextChunk("我旗下公司的总体目标是最大限度地提升人类文明的未来，也就是最大限度地提高人类文明拥有美好未来的概率，并将意识扩展到地球之外。"));
//		chunks.add(new TextChunk("SpaceX的目标是推进火箭技术的发展，最终使我们能够将生命和意识从地球扩展到月球、火星，最终扩展到其他恒星系统。"));
//		chunks.add(new TextChunk("如果可以让我选择，我情愿死在火星上， 但不是因为撞击而死。"));
//		chunks.add(new TextChunk("拥有马斯克，是美国和人类的幸运。"));

		chunks.add(new TextChunk("26岁，左宗棠写给妻子的信中说：他自己又落榜了，只觉得年轻时那份想一鸣惊人的心气，早就被岁月磨没了。"));
		chunks.add(new TextChunk("32岁，左宗棠依然没有中科举，前途无望，他写给致胡林翼书信说：午夜独思，百忧攒集，茫茫世宇，将焉厝此身矣！翻译过来就是：夜里一个人琢磨，一堆烦心事全堵在心头，这么大的天地，哪里才有我能安身的地方啊！"));
		chunks.add(new TextChunk("到了38岁，左宗棠还蛰居湘阴柳庄务农读书，此时，他写给朋友的信就更丧了，有一封信中他写道：自分老死山中，不与世接，为干萤、为寒蝉，乃所愿耳。"));

		// Step 2: 提取语义关系（W1权重）
		LLMRelationExtractor llmRelationExtractor = new LLMRelationExtractor();
		List<ConceptRelation> semanticRelations = llmRelationExtractor.batchExtractSemanticRelations(chunks, chatModel);
		System.out.println("语义关系提取完成，共提取 " + semanticRelations.size() + " 条");

		// Step 3: 生成上下文接近性关系（W2权重）
		ContextProximityUtil contextProximityUtil = new ContextProximityUtil();
		List<ConceptRelation> proximityRelations = contextProximityUtil.generateProximityRelations(chunks, semanticRelations);
		System.out.println("上下文关系生成完成，共生成 " + proximityRelations.size() + " 条");

		// Step 4: 合并所有关系
		List<ConceptRelation> allRelations = Stream.concat(
				semanticRelations.stream(),
				proximityRelations.stream()
		).collect(Collectors.toList());

		// Step 5: 聚合关系（求和权重 + 拼接关系）
		RelationAggregationUtil relationAggregationUtil = new RelationAggregationUtil();
		List<AggregatedRelation> aggregatedRelations = relationAggregationUtil.aggregateRelations(allRelations);
		System.out.println("关系聚合完成，最终生成 " + aggregatedRelations.size() + " 条唯一边");

		return aggregatedRelations;
	}

}
