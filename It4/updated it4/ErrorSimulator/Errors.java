package ErrorSimulator;

public enum Errors {
	INVALID_TID("2: error 5"),
	ILLEGAL_TFTP_OP("1: error 4"),
	NORMAL_MODE("0: normal");
	
	private String name;
	
	Errors(String name) {
		this.name = name;
	}
	
	@Override
	public String toString() {
		return name;
	}
	
}
