/**
 * 
 */
package net.sourceforge.retroweaver.harmony.runtime.java.net;

/**
 * @author Eric Coolman
 *
 */
public class URISyntaxException extends Exception {
	private int index;
	private String input;
	private String reason;
	
	/**
	 * 
	 * @param input
	 * @param reason
	 */
	public URISyntaxException(String input, String reason) {
		this(input, reason, -1);
	}

	/**
	 * 
	 * @param input
	 * @param reason
	 * @param index
	 */
	public URISyntaxException(String input, String reason, int index) {
		super(reason);
		this.reason = reason;
		this.index = index;
		this.input = input;
	}

	/**
	 * @return the index
	 */
	public int getIndex() {
		return index;
	}

	/**
	 * @param index the index to set
	 */
	public void setIndex(int index) {
		this.index = index;
	}

	/**
	 * @return the input
	 */
	public String getInput() {
		return input;
	}

	/**
	 * @param input the input to set
	 */
	public void setInput(String input) {
		this.input = input;
	}

	/**
	 * @return the reason
	 */
	public String getReason() {
		return reason;
	}

	/**
	 * @param reason the reason to set
	 */
	public void setReason(String reason) {
		this.reason = reason;
	}
}
