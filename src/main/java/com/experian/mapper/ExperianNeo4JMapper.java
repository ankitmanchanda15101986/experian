package com.experian.mapper;

import java.util.ArrayList;
import java.util.List;

import com.experian.dto.aiml.response.AimlFileFinalResponse;
import com.experian.dto.aiml.response.AimlFileResponse;
import com.experian.dto.neo4j.RequirementStatement;
import com.experian.dto.neo4j.TaxationBasedSuggestion;
import com.experian.dto.neo4j.request.SuggestionRequest;
import com.experian.dto.neo4j.request.TaxationBasedSuggestionRequest;

public class ExperianNeo4JMapper {
	
	/**
	 * This method will convert search request into suggestion request object.
	 * @param requirement
	 * @return
	 */
	public SuggestionRequest convertRequirementStringToSuggestionRequest(String requirement) {
		SuggestionRequest suggestionRequest = new SuggestionRequest();
		List<RequirementStatement> listRequirement = new ArrayList<RequirementStatement>();
		RequirementStatement statement = new RequirementStatement();
		statement.setId(1);
		statement.setRequirementStatement(requirement);
		listRequirement.add(statement);
		suggestionRequest.setRequirements(listRequirement);
		return suggestionRequest;
	}
	
	/**
	 * This method will get list of taxation based suggestion from aiml file response.
	 * @param aimlResponse
	 * @return
	 */
	public TaxationBasedSuggestionRequest getTaxationBasedSuggestionFromAimlResponse(AimlFileFinalResponse aimlResponse) {
		TaxationBasedSuggestionRequest taxationBasedSuggestionRequest = new TaxationBasedSuggestionRequest();
		List<TaxationBasedSuggestion> taxationBasedSuggestionlist = new ArrayList<>();
		if(!aimlResponse.getResponse().isEmpty()) {
			for (AimlFileResponse response : aimlResponse.getResponse()) {
				TaxationBasedSuggestion taxationBasedSuggestion = new TaxationBasedSuggestion();
				taxationBasedSuggestion.setId(response.getRequirementStatement().getId());
				taxationBasedSuggestion.setRequirementStatement(response.getRequirementStatement().getRequirementStatement());
				taxationBasedSuggestion.setTaxation(response.getTaxonomy_Level_1());
				taxationBasedSuggestionlist.add(taxationBasedSuggestion);
			}
		}
		taxationBasedSuggestionRequest.setRequest(taxationBasedSuggestionlist);
		return taxationBasedSuggestionRequest;
	}

}
