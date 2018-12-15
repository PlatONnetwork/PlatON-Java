package org.platon.storage.enums;

public enum  OverlimitStrategyEnum {

	OVERLIMIT_STRATEGY_COMMIT(1,"超限策略-提交"),
	OVERLIMIT_STRATEGY_THROW_EXCEPTION(2,"超限策略-报错");

	private String name;
	private int code;

	private OverlimitStrategyEnum(int code, String name){
		this.name = name;
		this.code = code;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public int getCode() {
		return code;
	}

	public void setCode(int code) {
		this.code = code;
	}

	public static String getNameByCodeValue(int code){
		OverlimitStrategyEnum[] allEnums = values();
		for(OverlimitStrategyEnum enableStatus : allEnums){
			if(enableStatus.getCode()==code){
				return enableStatus.getName();
			}
		}
		return null;
	}
}
