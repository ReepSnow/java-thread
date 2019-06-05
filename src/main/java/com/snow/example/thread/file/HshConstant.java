package com.snow.example.thread.file;

/**
 * @author wangpengtao
 * @date 2017/11/10  14:42
 * @EMAIL wptxc@foxmail.com
 */

public interface HshConstant {

    /**
     * 海尚汇常量
     * */
    long HSH_BRAND_ID =3664L;
    String HSH_BRAND_NAME ="诺德斯特龙Nordstrom";
    String HSH_BRAND_ORIGIN="Nordstrom";
    String MALE = "Male";
    String FEMALE = "Female";
    String UNISEX = "Unisex";
    long HSH_PROVIDER_ID=5L;
    String HSH_PROVIDER_NAME="海尚汇";
    String HSH_DOWN_FILE_NAME_TAR_GZ="promotions.tgz";
    String HSH_DOWN_FILE_NAME_TAR="promotions.tar";
    String HSH_DOWN_FILE_NAME="promotions";
    //一个活动待补偿的最大sku数量
    int COUNT_SKU_COMP=20;
    //一次发送消息的实体个数
    int COUNT_ENTITY_MESSAGE=800;
    //海尚汇默认的颜色
    String COLOR_HSH_DEF="No color";
    //海上海默认的尺寸
    String SIZE_HSH_DEF="One Size";
    String SIZE_HSH_DEF_OS="OS";
    //海尚汇条码前缀
    String HSH_BAR_PRE="HSH_";
    public enum PromitionType {
        GENERAL1("一般","0"),
        GENERAL2("服装","0"),
        GENERAL3("鞋履","0"),
        GENERAL4("包袋","0"),
        GENERAL5("腕表","0"),
        GENERAL6("配饰","0"),
        GENERAL7("美妆","0"),
        GENERAL8("母婴","0"),
        GENERAL9("家居","0"),
        DISCOUNT("最新折扣","1"),
        TOP("TOP排行榜","2");
        private String typeName;
        private String typeCode;
        PromitionType(String typeName,String typeCode){
            this.typeName=typeName;
            this.typeCode=typeCode;
        }

        public String getTypeCode() {
            return typeCode;
        }

        public String getTypeName() {
            return typeName;
        }
    }
    String UpdateSkuOnUpdate ="UpdateSkuOnUpdate";
    String InsertSkuOnUpdate ="InsertSkuOnUpdate";
    String InsertSkuOnInsert ="InsertSkuOnInsert";

    String PROMOTION_ACTIVE = "active";
    String PROMOTION_INACTIVE ="inactive";
    enum PromitionStatus {
        active("active",1),
        unactive("inactive",0);
        private String typeName;
        private int typeCode;
        PromitionStatus(String typeName,int typeCode){
            this.typeName=typeName;
            this.typeCode=typeCode;
        }

        public int getTypeCode() {
            return typeCode;
        }

        public String getTypeName() {
            return typeName;
        }
    }
    String SKU_OPTION_TYPE_INSERT = "insert";
    String SKU_OPTION_TYPE_UPDATE = "update";
    String CATEGORY_DEFAULT="其他";
    String SKU_CREATE_NAME = "导入";

    String PARTNERID_HSH ="HSH";

    int EXPIRED_ACTIVITIES_NUM =70;

}
