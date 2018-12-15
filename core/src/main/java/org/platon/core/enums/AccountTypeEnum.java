package org.platon.core.enums;

/**
 * AccountTypeEnum
 *
 * @author yanze
 * @desc account type
 * @create 2018-07-26 17:36
 **/
public enum AccountTypeEnum {
    ACCOUNT_TYPE_EXTERNAL(1,"external account"),
    ACCOUNT_TYPE_CONTRACT(2,"contract account");

    private String name;
    private int code;

    private AccountTypeEnum(int code, String name){
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
        AccountTypeEnum[] allEnums = values();
        for(AccountTypeEnum enableStatus : allEnums){
            if(enableStatus.getCode()==code){
                return enableStatus.getName();
            }
        }
        return null;
    }
}
