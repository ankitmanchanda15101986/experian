/**
 * 
 */
package com.experian.dto.neo4j.response;

import java.util.List;

/**
 * @author manchanda.a
 *
 */
public class WordCategoryResponse {

	private List<WordCategory> response;

	/**
	 * @return the response
	 */
	public List<WordCategory> getResponse() {
		return response;
	}

	/**
	 * @param response the response to set
	 */
	public void setResponse(List<WordCategory> response) {
		this.response = response;
	}

	/* (non-Javadoc)
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString() {
		return "WordCategoryResponse [response=" + response + ", getResponse()=" + getResponse() + ", getClass()="
				+ getClass() + ", hashCode()=" + hashCode() + ", toString()=" + super.toString() + "]";
	}
	
	
}
