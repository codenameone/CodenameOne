package net.sourceforge.retroweaver;

public class RetroWeaverException extends RuntimeException {

	public RetroWeaverException(String message) {
		super(message);
	}

	public RetroWeaverException(String message, Throwable cause) {
		super(message, cause);
	}

	public RetroWeaverException(Throwable cause) {
		super(cause);
	}

}
