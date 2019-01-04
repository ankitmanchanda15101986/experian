/**
 * 
 */
package com.experian.service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.StringHttpMessageConverter;
import org.springframework.web.client.RestTemplate;

import com.experian.dto.ExperianFileRefreshRequest;
import com.experian.dto.FileUploadResponse;
import com.experian.dto.FileUploadResponseList;
import com.experian.dto.aiml.request.AIMLFileRequest;
import com.experian.dto.aiml.response.AimlFileFinalResponse;
import com.experian.dto.aiml.response.AimlFileResponse;
import com.experian.dto.neo4j.RequirementStatement;
import com.experian.dto.neo4j.RequirementSuggestions;
import com.experian.dto.neo4j.Suggestions;
import com.experian.dto.neo4j.request.FinalNeo4JRequest;
import com.experian.dto.neo4j.request.SuggestionRequest;
import com.experian.dto.neo4j.request.TaxationBasedSuggestionRequest;
import com.experian.dto.neo4j.response.SuggestionResponse;
import com.experian.dto.neo4j.response.TaxationResponse;
import com.experian.dto.neo4j.response.WordCategoryResponse;
import com.experian.mapper.ExperianNeo4JMapper;

import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.stereotype.Service;

/**
 * @author manchanda.a
 *
 */
@Service
public class ExternalService {
	private static final Logger logger = LoggerFactory.getLogger(ExternalService.class);

	@Autowired
	private RestTemplate template;

	@Autowired
	private ServiceHelper helper;
	
	@Autowired
	private ExperianNeo4JMapper neo4jMapper;

	/**
	 * This method will call AI/ML service and pass requirement statement , in
	 * return it will get level 1,2,3,4 and quality score of that statement.
	 * 
	 * @return
	 */
	public AimlFileFinalResponse processFileToAiml(AIMLFileRequest request) {
		String uri = new String("http://localhost:8080/api/process/file");
		ResponseEntity<AimlFileFinalResponse> response = getRestTemplate().postForEntity(uri, request,
				AimlFileFinalResponse.class);
		return response.getBody();

	}

	/**
	 * This method will call neo 4j service and pass requirement statement in
	 * return it will get suggestions and match % from neo4j which internally
	 * uses elastic search.
	 * suggestion
	 * @param taxationBasedSuggestionRequest
	 * @return
	 */
	public SuggestionResponse processFileToNeo4jToGetSuggestions(TaxationBasedSuggestionRequest taxationBasedSuggestionRequest) {
		String uri = new String("http://localhost:8080/api/taxation/suggestion");
		ResponseEntity<SuggestionResponse> response = getRestTemplate().postForEntity(uri, taxationBasedSuggestionRequest,
				SuggestionResponse.class);
		return response.getBody();
	}
	
	/**
	 * This method will call neo 4j service and pass requirement statement in
	 * return it will get suggestions and match % from neo4j which internally
	 * uses elastic search.
	 * 
	 * @param taxationBasedSuggestionRequest
	 * @return
	 */
	public SuggestionResponse searchRequirementToGetSuggestions(SuggestionRequest request) {
		String uri = new String("http://localhost:8080/api/search/suggestion");
		ResponseEntity<SuggestionResponse> response = getRestTemplate().postForEntity(uri, request,
				SuggestionResponse.class);
		return response.getBody();
	}

	/**
	 * This method will call neo4j with final request that will be saved in
	 * neo4j database.
	 * 
	 * @param request
	 * @return
	 */
	public boolean processFinalResponse(FinalNeo4JRequest request) {
		String uri = new String("http://localhost:8080/api/save");
		ResponseEntity<Boolean> response = getRestTemplate().postForEntity(uri, request, Boolean.class);
		return response.getBody();
	}

	/**
	 * This method will call neo4j service to get list of word category and
	 * process response.
	 * 
	 * @return
	 */
	public WordCategoryResponse getWordCategoryFromNeo4j() {
		String uri = new String("http://localhost:8080/api/word/count");
		ResponseEntity<WordCategoryResponse> response = getRestTemplate().getForEntity(uri, WordCategoryResponse.class);
		return response.getBody();
	}

	/**
	 * This method will call ai/ml and pass taxation level 1,2,3,4 and
	 * requirement statement, in return it will get score.
	 * 
	 * @param request
	 * @return
	 */
	public Integer calculateScore(ExperianFileRefreshRequest request) {
		String uri = new String("http://localhost:8080/api/calculate/score");
		ResponseEntity<Integer> response = getRestTemplate().postForEntity(uri, request, Integer.class);
		return response.getBody();
	}

	/**
	 * This method will take string as an request parameter and then call ai/ml
	 * to get taxation levels and quality score, and then neo4j to get
	 * suggestions based on that it will pass combine it and prepare response.
	 * 
	 * @param requirement
	 * @return
	 */
	public FileUploadResponse addNewRequirement(String requirement) {
		// Get Word count.
		WordCategoryResponse wordCategoryResponse = getWordCategoryFromNeo4j();
		if (wordCategoryResponse != null) {
			AIMLFileRequest aIMLFileRequest = new AIMLFileRequest();
			List<RequirementStatement> requirementStatementList = new ArrayList<RequirementStatement>();
			RequirementStatement requirementStatement = new RequirementStatement();
			requirementStatement.setId(1);
			requirementStatement.setRequirementStatement(requirement);
			requirementStatementList.add(requirementStatement);

			aIMLFileRequest.setRequirementStatement(requirementStatementList);
			aIMLFileRequest.setWordCategory(wordCategoryResponse);

			// Call AIML to process request
			AimlFileFinalResponse aimlResponse = processFileToAiml(aIMLFileRequest);
			if (aimlResponse != null) {
				// Call to get suggestion.
				TaxationBasedSuggestionRequest taxationBasedSuggestionRequest = neo4jMapper.getTaxationBasedSuggestionFromAimlResponse(aimlResponse);
				SuggestionResponse suggestionResponse = processFileToNeo4jToGetSuggestions(taxationBasedSuggestionRequest);
				if (suggestionResponse != null) {
					Map<AimlFileResponse, RequirementSuggestions> map = helper
							.fetchMapBasedOnRequirementId(aimlResponse, suggestionResponse);
					FileUploadResponseList responseList = helper.createFinalUploadResponseList(map);
					if (!responseList.getResponse().isEmpty()) {
						return responseList.getResponse().get(0);
					}
				}
			}
		}
		return null;
	}

	/**
	 * This method will return response for matched case requirements. So Neo4j
	 * wont be called again we have got any matching for that requirement.
	 * 
	 * @param suggestionList
	 * @return
	 */
	public FileUploadResponse addMatchedRequirement(List<Suggestions> suggestionList) {
		// Get Word count.
		List<RequirementSuggestions> requirementSuggestionsList = new ArrayList<>();
		WordCategoryResponse wordCategoryResponse = getWordCategoryFromNeo4j();
		if (wordCategoryResponse != null) {
			AIMLFileRequest aIMLFileRequest = new AIMLFileRequest();
			List<RequirementStatement> requirementStatementList = new ArrayList<RequirementStatement>();
			int count = 1;
			for (Suggestions suggestions : suggestionList) {
				RequirementStatement requirementStatement = new RequirementStatement();
				requirementStatement.setId(count);
				requirementStatement.setRequirementStatement(suggestions.getSuggestion());
				count++;
				requirementStatementList.add(requirementStatement);
				
				// Creating requirement suggestions object.
				RequirementSuggestions requirementSuggestions = new RequirementSuggestions();
				requirementSuggestions.setRequirements(requirementStatement);
				List<Suggestions> suggest = new ArrayList<>();
				suggest.add(suggestions);
				requirementSuggestions.setSuggestionResponse(suggest);
			}
			aIMLFileRequest.setRequirementStatement(requirementStatementList);
			aIMLFileRequest.setWordCategory(wordCategoryResponse);

			// Call AIML to process request
			AimlFileFinalResponse aimlResponse = processFileToAiml(aIMLFileRequest);
			if(aimlResponse != null) {
				SuggestionResponse suggestionResponse = new SuggestionResponse();
				suggestionResponse.setSuggestions(requirementSuggestionsList);
				Map<AimlFileResponse, RequirementSuggestions> map = helper
						.fetchMapBasedOnRequirementId(aimlResponse, suggestionResponse);
				FileUploadResponseList responseList = helper.createFinalUploadResponseList(map);
				if (!responseList.getResponse().isEmpty()) {
					return responseList.getResponse().get(0);
				}
			}
		}
		return null;
	}

	/**
	 * This method will return response for no match case requirements. So Neo4j
	 * wont be called again as we already have got requirements from matched
	 * set.
	 * 
	 * @param requirement
	 * @return
	 */
	public FileUploadResponse addNoMatchedRequirement(String requirement) {
		// Get Word count.
		WordCategoryResponse wordCategoryResponse = getWordCategoryFromNeo4j();
		if (wordCategoryResponse != null) {
			AIMLFileRequest aIMLFileRequest = new AIMLFileRequest();
			List<RequirementStatement> requirementStatementList = new ArrayList<RequirementStatement>();
			RequirementStatement requirementStatement = new RequirementStatement();
			requirementStatement.setId(1);
			requirementStatement.setRequirementStatement(requirement);
			requirementStatementList.add(requirementStatement);
			aIMLFileRequest.setRequirementStatement(requirementStatementList);
			aIMLFileRequest.setWordCategory(wordCategoryResponse);

			// Call AIML to process request
			AimlFileFinalResponse aimlResponse = processFileToAiml(aIMLFileRequest);
			if(aimlResponse != null) {
				SuggestionResponse suggestionResponse = new SuggestionResponse();
				suggestionResponse.setSuggestions(new ArrayList<>());
				Map<AimlFileResponse, RequirementSuggestions> map = helper
						.fetchMapBasedOnRequirementId(aimlResponse, suggestionResponse);
				FileUploadResponseList responseList = helper.createFinalUploadResponseList(map);
				if (!responseList.getResponse().isEmpty()) {
					return responseList.getResponse().get(0);
				}
			}
		}
		return null;

	}

	/**
	 * This method will call neo4j service to get taxation levels and process
	 * response.
	 * 
	 * @return
	 */
	public TaxationResponse getTaxation() {
		String uri = new String("http://localhost:8080/api/fetch/taxation");
		ResponseEntity<TaxationResponse> response = getRestTemplate().getForEntity(uri, TaxationResponse.class);
		return response.getBody();
	}
	
	private RestTemplate getRestTemplate() {
		template.getMessageConverters().add(new MappingJackson2HttpMessageConverter());
		template.getMessageConverters().add(new StringHttpMessageConverter());
		return template;
	}
}
