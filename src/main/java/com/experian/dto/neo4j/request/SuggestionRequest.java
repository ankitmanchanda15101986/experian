/**
 * 
 */
package com.experian.dto.neo4j.request;

import java.util.List;

import com.experian.dto.neo4j.RequirementStatement;



/**
 * @author manchanda.a
 *
 */
public class SuggestionRequest {

	private List<RequirementStatement> requirements;

	/**
	 * @return the requirements
	 */
	public List<RequirementStatement> getRequirements() {
		return requirements;
	}

	/**
	 * @param requirements the requirements to set
	 */
	public void setRequirements(List<RequirementStatement> requirements) {
		this.requirements = requirements;
	}
	
	
}
