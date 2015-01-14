package net.montoyo.mcef.api;

/**
 * Use this interface to answer to JavaScript queries.
 * @author montoyo
 *
 */
public interface IJSQueryCallback {
	
	/**
	 * If the query succeeded, call this.
	 * @param response Whatever you want.
	 */
	public void success(String response);
	
	/**
	 * If the query failed, call this.
	 * 
	 * @param errId Whatever you want.
	 * @param errMsg Whatever you want.
	 */
	public void failure(int errId, String errMsg);

}
