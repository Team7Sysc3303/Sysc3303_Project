package ErrorSimulator;

public enum NetworkErrors {
	NORMAL("0: normal operation"),
	DELAY("1: delay"),
	DUPLICATE("2: duplicate"),
	LOSE("3: lose");
	
	private String name;
	
	NetworkErrors(String name) {
		this.name = name;
	}
	
	public String toString() {
		return name;
	}
}
