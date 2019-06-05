package com.snow.example.thread.file;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.sfebiz.cooperator.api.HaishangChannelService;
import com.sfebiz.cooperator.config.CooperatorDynamicConfig;
import com.sfebiz.cooperator.constant.MessageConstant;
import com.sfebiz.cooperator.constant.SystemConstant;
import com.sfebiz.cooperator.constant.hsh.HshConstant;
import com.sfebiz.cooperator.dao.dto.ScMerchantSkuLogDTO;
import com.sfebiz.cooperator.dao.mapper.*;
import com.sfebiz.cooperator.exception.HttpClientException;
import com.sfebiz.cooperator.haishanghui.convertentity.Attributes;
import com.sfebiz.cooperator.haishanghui.convertentity.Images;
import com.sfebiz.cooperator.ons.MessageProducer;
import com.sfebiz.cooperator.open.api.CooperatorOpenService;
import com.sfebiz.cooperator.open.entity.ApiBaseResult;
import com.sfebiz.cooperator.open.entity.CooperatorProductEntity;
import com.sfebiz.cooperator.open.entity.CooperatorPromotionEntity;
import com.sfebiz.cooperator.open.entity.HshPromotionAttrEntity;
import com.sfebiz.cooperator.util.*;
import com.sfebiz.logistics.api.SkuService;
import com.sfebiz.logistics.entity.SkuEntity;
import com.sfebiz.msg.api.MsgService;
import com.sfebiz.msg.entity.EmailInfo;
import com.sfebiz.msg.entity.RetStatus;
import com.sfebiz.product.api.MerchantItemMgmtService;
import net.pocrd.entity.ServiceException;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.rocketmq.common.message.Message;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.io.*;
import java.text.MessageFormat;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;


public class HaishangChannelServiceImpl implements HaishangChannelService {

    //每个海尚汇任务读取处理完以后线程休眠的毫秒数
    final static long DELAY_AFTER_EACH_FILE = 5000L;
    ExecutorService fixedThreadPool = Executors.newFixedThreadPool(3);

    @Override
    public Map createChannelSkuByHaishang() throws Exception {
        return getHaishanghuiData();
    }

    private Map getHaishanghuiData() throws Exception {
        String url="https://api.askyuanfang.cn/bulk/promotions.tgz";
        List<File> fileList=null;

        int timesTry=0;
        do {
            timesTry++;
            String srcfile = getFileName(HshConstant.HSH_DOWN_FILE_NAME);
            try {
                downPromotionData(url, srcfile);
            } catch (Exception e) {
                if(2<timesTry){
                    throw new Exception("海尚汇获取所有活动服务出现异常");
                }
            }
            fileList = FileMy.getFiles(srcfile);
            logger.error("文件的数量={}",fileList.size());
            int downLatch =fileList.size()>countFile?countFile:fileList.size();
            if(CollectionUtils.isNotEmpty(fileList)) {
                CountDownLatch countDownLatch = new CountDownLatch(downLatch);//初始化countDown
                //用来获取每个活动的文件信息
                int count=0;
                for (File file:fileList) {
                    if(countFile<=count){
                        break;
                    }
                    if(!file.getName().contains("-")){
                        continue;
                    }
                    promotionIds.add(file.getName());
                    fixedThreadPool.execute(new TaskHsh(file,countDownLatch,resultMap));
                    count++;
                    Thread.sleep(DELAY_AFTER_EACH_FILE);
                }
                logger.error("线程任务的数量={}",count);
                if(downLatch>count){
                    for(int i=0;i<(downLatch-count);i++){
                        countDownLatch.countDown();
                    }
                }
                countDownLatch.await();
            }
        }while (timesTry<3&&CollectionUtils.isEmpty(fileList));
        updatePromotionForDelete(promotionIds);
        //由于解决超重超方问题，所以加了一个新的map
        Map<String,List<String>> promotionsOffProductMap=new ConcurrentHashMap<String, List<String>>();
        resultMap.put("promotionsOffProductMap",promotionsOffProductMap);
        return resultMap;
    }

    private void threadRunFile(File file,Map resultMap) throws Exception {
        //活动id
        String promotionId=file.getName();
        //活动文件的内容信息
        String promotionDetail=null;
        //活动详情的type字段值
        String promotionType = null;
        //活动详情的attributes字段值，除去products字段
        String promotionAttr = null;
        //活动详情的attributes字段值，是JSONObject类型
        JSONObject jsonObjectPromotionDetailsAtt = null;
        //活动中的商品数组
        JSONArray productArray = null;
        try{
            //读取整个文件内容
            promotionDetail = getPromotionDeteilFromFile(file, promotionDetail);
            //替换emoji
            promotionDetail = promotionDetail.replaceAll("[\\ud800\\udc00-\\udbff\\udfff\\ud800-\\udfff]", "");
            //替换广告法限制词汇
            promotionDetail = getResponseByReplace(promotionDetail);
            if(StringUtils.isNotEmpty(promotionDetail)) {
                jsonObjectPromotionDetailsAtt = JSON.parseObject(promotionDetail).getJSONObject("attributes");
                productArray = jsonObjectPromotionDetailsAtt.getJSONArray("products");
                promotionId= JSON.parseObject(promotionDetail).getString("id");
                String promotionTypeTemp = jsonObjectPromotionDetailsAtt.getString("category");
                promotionType = getPromotionType(promotionTypeTemp);
                Map<String,Object> map = JSON.parseObject(jsonObjectPromotionDetailsAtt.toJSONString());
                map.remove("products");
                promotionAttr = JSON.toJSONString(map);
                updateOrInsertByPromotion( productArray , promotionId, promotionType, promotionAttr,resultMap);
            }
        }catch (Exception e){
            ((Map<String,Exception>)resultMap.get("promotionsIdMap")).put(promotionId,e);
            insertExcetion(promotionId, promotionType, promotionAttr, e);
            logger.error("拉去海尚汇单个活动promotionId={},数据时出现异常e={}",promotionId,e);
            throw e;
        }
    }

    private void downPromotionData(String url, String srcfile) throws Exception {
        InputStream in = HttpUtil.getPromotion(url);
        OutputStream op = new FileOutputStream(getFileName(HshConstant.HSH_DOWN_FILE_NAME_TAR_GZ));
        try {
            IOUtils.copy(in,op);
        } catch (IOException e) {
            System.out.println("log");
            throw e;
        }finally {
            IOUtils.closeQuietly(in);
            IOUtils.closeQuietly(op);
        }
        //把gz文件解压成tar文件,sougz:要解压的gz,tar：解压后的tar文件
        String sougz= getFileName(HshConstant.HSH_DOWN_FILE_NAME_TAR_GZ);
        String soutar =getFileName(HshConstant.HSH_DOWN_FILE_NAME_TAR);
        GZipUtils.decompress(sougz, soutar);
        //解压tar文件，soufile:要解压的tar路径,srcfile：解压后放的路径
        TarUtils.dearchive(soutar,srcfile);
    }

    private void insertExcetion(String promotionId, String promotionType, String promotionAttr, Exception e) {
        ScMerchantSkuLogDTO scMerchantSkuLogDo = new ScMerchantSkuLogDTO();
        scMerchantSkuLogDo.setPromotionid(promotionId);
        scMerchantSkuLogDo.setException(e.getMessage());
        scMerchantSkuLogDo.setDesc1(promotionType);
        scMerchantSkuLogDo.setDesc2(promotionAttr);
        scMerchantSkuLogDo.setGmtCreate(new Date());
        scMerchantSkuLogDo.setGmtModified(new Date());
        scMerchantSkuLogWriteMapper.insertSelective(scMerchantSkuLogDo);
        logger.error("海尚汇promotionId={},发生异常e={}", promotionId,e);
    }

    private String getPromotionDeteilFromFile(File file, String promotionDetail) throws Exception {
        InputStream inputStream=null;
        if(!file.getName().contains("._")){
            try{
                inputStream = new FileInputStream(file);
                promotionDetail = IOUtils.toString(inputStream);
            }catch (Exception e){
                throw e;
            }finally {
                IOUtils.closeQuietly(inputStream);
            }
        }
        return promotionDetail;
    }


    private int updateOrInsertByPromotion(JSONArray promotionDetailArray, String promotionId, String promotionType, String promotionAttr, Map resultMap) throws Exception {
        Integer countP =0;
        List<String> productIdList = new ArrayList<String>();
        List<CooperatorProductEntity> cooperatorProductEntities = new ArrayList<CooperatorProductEntity>();
        for (int j = 0; j < promotionDetailArray.size(); j++) {
            JSONObject jsonObjectProductOk=null;
            //用于单次生成一个sku
            try{
                jsonObjectProductOk = promotionDetailArray.getJSONObject(j);
                //用于记录主商品的id
                String masterProductId=jsonObjectProductOk.getString("id");
                CooperatorProductEntity cooperatorProductEntity = createMerchantSkuByProduct(jsonObjectProductOk,null,null);
                String masterNameCn = cooperatorProductEntity.getName();
                if(null ==cooperatorProductEntity){
                    continue;
                }
                cooperatorProductEntities.add(cooperatorProductEntity);
                countP++;
                productIdList.add(masterProductId);
                JSONArray productVariants = jsonObjectProductOk.getJSONObject("attributes").getJSONArray("variants");
                if(productVariants.size()>0){
                    for(int i=0;i<productVariants.size();i++){
                        JSONObject jsonObjectProductOkVar=productVariants.getJSONObject(i);
                        String subProductId=jsonObjectProductOkVar.getString("id");
                        CooperatorProductEntity cooperatorProductEntitySub = createMerchantSkuByProduct(jsonObjectProductOkVar,masterProductId,masterNameCn);
                        if(null == cooperatorProductEntity){
                            continue;
                        }
                        cooperatorProductEntities.add(cooperatorProductEntitySub);
                        countP++;
                        productIdList.add(subProductId);
                    }
                }
            }catch (Exception e){
                logger.error("海尚汇发生Exception异常,把之前成功的数据发送消息出去,jsonObjectProductOk={},promotionId={},responseType={},e={}",jsonObjectProductOk,promotionId,promotionType,e);
                saveHshDataMessage(productIdList, promotionId, promotionType, promotionAttr,cooperatorProductEntities);
                throw e;
            }
        }

        saveHshDataMessage(productIdList, promotionId, promotionType, promotionAttr,cooperatorProductEntities);

        ((Map<String,Integer>)resultMap.get("promotionsIdAndSkuIdCountMap")).put(promotionId, countP);
        return countP;
    }

    public CooperatorProductEntity createMerchantSkuByProduct(JSONObject jsonObjectProductOk, String masterProductProductId,String masterNameCn) throws ServiceException {
        String productId=null;
        productId = jsonObjectProductOk.getString("id");
        JSONObject jsonObjectProductDetailOk = jsonObjectProductOk.getJSONObject("attributes");
        Attributes attributes = JSON.parseObject(jsonObjectProductDetailOk.toString(),Attributes.class);
        String  currency =SystemConstant.CURRENCY_USD;
        currency = CooperatorDynamicConfig.getCooperatorDynamicConfig().getRule("merchant","currency");
        String currencyTar = jsonObjectProductDetailOk.getString("currency");
        if(null ==currencyTar || !currencyTar.equals(currency)){
            logger.error("海尚汇商品productId={}的币种不是currency={}",jsonObjectProductDetailOk.getString("id"),currency);
            return null;
        }
        CooperatorProductEntity productEntity = new CooperatorProductEntity();
        if(StringUtils.isNotBlank(attributes.getNameCn())){
            productEntity.setName(attributes.getNameCn());
        }else {
            productEntity.setName(masterNameCn);
        }
        productEntity.setForeignName(attributes.getName());
        productEntity.setProductId(productId);
        productEntity.setDescriptionText(attributes.getDescription());
        productEntity.setDescriptionTextCn(attributes.getDescriptionCn());
        productEntity.setCurrency(currency);
        productEntity.setFirstCategoryName(attributes.getPrimaryCategory());
        productEntity.setSecondaryCategoryName(attributes.getSecondaryCategory());
        if(StringUtils.isNotEmpty(attributes.getTertiaryCategory())){
            productEntity.setThirdCategoryName(attributes.getTertiaryCategory());
        }else {
            productEntity.setThirdCategoryName(HshConstant.CATEGORY_DEFAULT);
        }

        productEntity.setMasterProductId(masterProductProductId);
        String gender = attributes.getGender();
        if(StringUtils.isNotEmpty(gender)&&StringUtils.equalsIgnoreCase(gender,HshConstant.MALE)){
            productEntity.setGender(1);
        }
        //如果性别是中性Unisex 也按照女性来匹配目录
        else if (StringUtils.isNotEmpty(gender)&&(StringUtils.equalsIgnoreCase(gender,HshConstant.FEMALE)||StringUtils.equalsIgnoreCase(gender,HshConstant.UNISEX))){
            productEntity.setGender(0);
        }
        //存的是真是条码
        productEntity.setBarcode(convertHshBarCodes(productId));
        productEntity.setBrandName(attributes.getBrand());
        productEntity.setColor(attributes.getColor());
        productEntity.setSize(attributes.getSize());
        productEntity.setLastUpdate(attributes.getLastScraperUpdate());
        if(null ==attributes.getAvailable()){
            logger.error("productId={}的available为null",productId);
            productEntity.setAvailable(false);
        }else {
            productEntity.setAvailable(attributes.getAvailable());
        }


        //接口里的价格不做处理直接存到localSalePrice里
        productEntity.setSupplyPrice((int)(attributes.getSalePrice()* SystemConstant.INT_ONE_HUNDRED));
        StringBuffer stringBu = new StringBuffer();
        List<Images> images = attributes.getImages();
        for (int i = 0,j=images.size();i<j&&i<10;i++){
            if(i==(j-1) || i==9){
                stringBu.append(images.get(i).getLarge());
            }else {
                stringBu.append(images.get(i).getLarge() + ",");
            }
        }
        productEntity.setImages(stringBu.toString());
        if(StringUtils.isNotBlank(attributes.getStore())){
            productEntity.setStoreName(attributes.getStore());
        }else {
            productEntity.setStoreName("Nordstrom");
        }
        productEntity.setOrigin("美国");
        if(null != attributes.getInformation().getSize()){
            productEntity.setDescriptionText(JSON.toJSONString(attributes.getInformation().getSize()));
        }
        if(null != attributes.getInformation().getDetails()){
            productEntity.setDescriptionTextExt(JSON.toJSONString(attributes.getInformation().getDetails()));
        }
        productEntity.setReferencePrice((int)(attributes.getPrice()* SystemConstant.INT_ONE_HUNDRED));
        return productEntity;

    }

    public String getFileName(String fileName) throws Exception {
        //String dir = hshDown+ File.separator+ DateUtil.formatDateStr(new Date(),DateUtil.DATE_PATTERN)+File.separator+ fileName;
        //String dir
        try {
            TarUtils.fileProberHsh(new File(dir));
        }catch (Exception e){
            logger.error("海尚汇文件创建失败，e={}",e);
            throw e;
        }
        return dir;
    }

    private String getPromotionType(String promotionTypeTemp) {
        HshConstant.PromitionType[] types =  HshConstant.PromitionType.values();
        for(HshConstant.PromitionType type:types){
            if(promotionTypeTemp.equals(type.getTypeName())){
                return type.getTypeCode();
            }
        }
        return "0";
    }

    private String getResponseByReplace(String responsePromotionDetail) {
        String replace= CooperatorDynamicConfig.getCooperatorDynamicConfig().getRule("merchant","replace");
        HashMap<String,String> replaceStr = JSON.parseObject(replace,HashMap.class);
        for(Map.Entry<String,String> entry : replaceStr.entrySet()){
            responsePromotionDetail = responsePromotionDetail.replaceAll(entry.getKey(),entry.getValue());
        }
        return responsePromotionDetail;
    }
    private void saveHshDataMessage(List<String> productIdList, String promotionId, String promotionType, String promotionAttr,List<CooperatorProductEntity> cooperatorProductEntities) throws ServiceException {
        logger.info("saveHshDataMessage---promotionAttr={}",promotionAttr);
        HshPromotionAttrEntity attrEntity = JSON.parseObject(promotionAttr,HshPromotionAttrEntity.class);
        CooperatorPromotionEntity cooperatorPromotionEntity = new CooperatorPromotionEntity();
        cooperatorPromotionEntity.setPromotionId(promotionId);
        cooperatorPromotionEntity.setPromotionTypeCn(attrEntity.getCategory());
        cooperatorPromotionEntity.setPromotionType(Integer.valueOf(promotionType));
        cooperatorPromotionEntity.setTitle(attrEntity.getTitle());
        cooperatorPromotionEntity.setStoreName(attrEntity.getStore());
        cooperatorPromotionEntity.setSkuAvailability(attrEntity.getAvailability());
        cooperatorPromotionEntity.setStatus(HshConstant.PromitionStatus.valueOf(attrEntity.getStatus()).getTypeCode());
        cooperatorPromotionEntity.setTotalMasterSkuCount(attrEntity.getTotalMasterSKUs());
        cooperatorPromotionEntity.setTotalSkuCount(attrEntity.getTotalSKUs());
        cooperatorPromotionEntity.setSquareImgUrl(attrEntity.getImages().getSquare());
        cooperatorPromotionEntity.setLandScapeImgUrl(attrEntity.getImages().getLandscape());
        cooperatorPromotionEntity.setLargeLandScapeImgUrl(attrEntity.getImages().getLargeLandscape());
        cooperatorPromotionEntity.setDiscount(attrEntity.getDiscountsFrom());
        cooperatorPromotionEntity.setDescriptionText(attrEntity.getDescription());
        cooperatorPromotionEntity.setCreateTime(attrEntity.getCreated());
        cooperatorPromotionEntity.setLastUpdate(attrEntity.getLastScraperUpdate());

        ApiBaseResult promotionResult = cooperatorOpenService.syncPromotionInfo(HshConstant.HSH_PROVIDER_ID,cooperatorPromotionEntity);
        if(!promotionResult.isSuccess){
            logger.error("saveHshDataMessage--syncPromotionInfo--cooperatorPromotionEntity={}",JSON.toJSONString(cooperatorPromotionEntity));
            logger.error("同步活动出现异常：e={}",promotionResult.getResultMessage());
        }
        ApiBaseResult relationResult = cooperatorOpenService.syncPromotionProductIdList(HshConstant.HSH_PROVIDER_ID,promotionId,productIdList);
        if(!relationResult.isSuccess){
            logger.error("同步活动跟商品出现异常：e={}",relationResult.getResultMessage());
        }
        if(CollectionUtils.isEmpty(cooperatorProductEntities)){
            return;
        }
        int size = cooperatorProductEntities.size();
        int count = size/SystemConstant.INT_SIX_HUNDRED;
        for(int i=0;i<=count;i++){
            List<CooperatorProductEntity> cooperatorProductEntityList=null;
            if(i<count){
                cooperatorProductEntityList = cooperatorProductEntities.subList(i*SystemConstant.INT_SIX_HUNDRED,(i+1)*SystemConstant.INT_SIX_HUNDRED);
            }else {
                cooperatorProductEntityList = cooperatorProductEntities.subList(i*SystemConstant.INT_SIX_HUNDRED,size);
            }
            ApiBaseResult productResult = cooperatorOpenService.syncProduct(HshConstant.PARTNERID_HSH,cooperatorProductEntityList);
            if(!productResult.isSuccess){
                logger.error("同步商品出现异常：e={}",productResult.getResultMessage());
            }
        }

    }

    private void buildOssImagesMessage(List<Long> skuIdList){
        //发送消息到商品，通知更新图片
        Message msg = new Message();
        msg.setTopic(MessageConstant.TOPIC_SUPPLY_CHAIN_COOP_PRODUCT);
        msg.setTags(MessageConstant.TAG_MERCHANT_SKU_UPDATE_OSS_URL);
        msg.setBody(" ".getBytes());
        msg.putUserProperty("skuIdList", JSON.toJSONString(skuIdList));
        cooperatorProductMessageProducer.send(msg);
    }

    /**
     * 用于下架过期活动
     * @param promotionIds
     */
    private void updatePromotionForDelete(List<String> promotionIds){
        try {
            //不能因为该操作影响主流程，所以对该操作进行异常捕获
            //该流程涉及删除数据，日志信息有error级别
            List<String> promotionIdForDelete = scMerchantPromotionsReadMapper.selectMerchantPromotionNotIn(promotionIds);
            if(promotionIdForDelete.size() <HshConstant.EXPIRED_ACTIVITIES_NUM){
                ApiBaseResult apiBaseResult =cooperatorOpenService.removePromotion(HshConstant.HSH_PROVIDER_ID,promotionIdForDelete);
                logger.error("删除数据的结果是：",JSON.toJSONString(apiBaseResult));
            }else {
                EmailInfo emailInfo = new EmailInfo();
                emailInfo.setType("email");
                emailInfo.setBiz("supplychain");
                emailInfo.setEmailFormat("html");
                emailInfo.setTitle("cooperator-过期活动超过阈值"+HshConstant.EXPIRED_ACTIVITIES_NUM);
                emailInfo.setBody("过期活动数量太多，需要手动删除");
                emailInfo.setTo("caizhengjie@ifunq.com");
                sendEmail(emailInfo);
                emailInfo.setTo("wangpengtao@ifunq.com");
                sendEmail(emailInfo);
                logger.error("无法删除数据超过阈值"+HshConstant.EXPIRED_ACTIVITIES_NUM+"，这些数据的活动id是：",JSON.toJSONString(promotionIds));
            }
        }catch (Exception e){
            logger.error("批量下架活动出现异常");
        }
    }



    private boolean sendEmail(EmailInfo emailInfo) {
        RetStatus retStatus = msgService.sendEmail(emailInfo);
        if (retStatus.isResult()) {
            logger.info(MessageFormat.format("Send AutoGetCategoryProductTask email to [{0}] success.", emailInfo.getTo()));
            return true;
        } else {
            logger.error(MessageFormat.format("Send AutoGetCategoryProductTask email to {0} failed,result is:{1},{2}", emailInfo.getTo(), retStatus.getMessage()));
            return false;
        }
    }
    private String convertHshBarCodes(String hshProductId){
         return HshConstant.HSH_BAR_PRE.concat(hshProductId);
    }

    class TaskHsh implements Runnable {
        File file;
        CountDownLatch countDownLatch;
        Map resultMap;
        public TaskHsh() {
        }
        public TaskHsh(File file,CountDownLatch countDownLatch,Map resultMap) {
            this.file = file;
            this.countDownLatch=countDownLatch;
            this.resultMap=resultMap;
        }
        @Override
        public void run() {
            try {
                threadRunFile(file,resultMap);
            } catch (Exception e) {
                logger.error("线程{}执行任务出现异常",Thread.currentThread().getName());
            }finally {
                countDownLatch.countDown();
            }
        }
    }
}