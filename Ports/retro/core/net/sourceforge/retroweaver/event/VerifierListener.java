package net.sourceforge.retroweaver.event;

public interface VerifierListener {

	void verifyPathStarted(String msg);

	void verifyClassStarted(String msg);

	void acceptWarning(String msg);

	void displaySummary(int warningCount);

}
