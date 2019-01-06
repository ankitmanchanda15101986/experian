/**
 * 
 */
package com.experian.dto.neo4j.request;



/**
 * @author manchanda.a
 *
 */
public class SuggestionRequest {

	private String requirementStatement;
	private String taxation;
	
	
	public String getRequirementStatement() {
		return requirementStatement;
	}
	public void setRequirementStatement(String requirementStatement) {
		this.requirementStatement = requirementStatement;
	}
	public String getTaxation() {
		return taxation;
	}
	public void setTaxation(String taxation) {
		this.taxation = taxation;
	}
	
	
	
}
