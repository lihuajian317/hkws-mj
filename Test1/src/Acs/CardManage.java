package Acs;


import NetSDKDemo.HCNetSDK;
import com.sun.jna.ptr.IntByReference;
import org.json.JSONException;
import org.json.JSONObject;

/**
 * 卡管理，以人为中心，要先下发工号，卡号要关联在人员工号上
 */
public final class CardManage {


    /**
     * 下发卡号，
     * @param userID  用户注册ID
     * @param cardNo 卡号
     * @throws JSONException
     */
    public static void addCardInfo(int userID,String employeeNo,String cardNo) throws JSONException {
        HCNetSDK.BYTE_ARRAY ptrByteArray = new HCNetSDK.BYTE_ARRAY(1024);    //数组
        String strInBuffer = "POST /ISAPI/AccessControl/CardInfo/Record?format=json";
        System.arraycopy(strInBuffer.getBytes(), 0, ptrByteArray.byValue, 0, strInBuffer.length());//字符串拷贝到数组中
        ptrByteArray.write();

        int lHandler = AcsMain.hCNetSDK.NET_DVR_StartRemoteConfig(userID, HCNetSDK.NET_DVR_JSON_CONFIG, ptrByteArray.getPointer(), strInBuffer.length(), null, null);
        if (lHandler < 0)
        {
            System.out.println("AddCardInfo NET_DVR_StartRemoteConfig 失败,错误码为"+AcsMain.hCNetSDK.NET_DVR_GetLastError());
            return;
        }
        else{
            System.out.println("AddCardInfo NET_DVR_StartRemoteConfig 成功!");
            HCNetSDK.BYTE_ARRAY lpInput = new HCNetSDK.BYTE_ARRAY(1024);    //数组
            String strJsonData = "{\n" +
                    "    \"CardInfo\" : {\n" +
                    "        \"employeeNo\":\""+employeeNo+"\", \n" +
                    "        \"cardNo\":\""+cardNo+"\", \n" +
                    "        \"cardType\":\"normalCard\"\n" +
                    "        } \n" +
                    "}";
            System.arraycopy(strJsonData.getBytes(), 0, lpInput.byValue, 0, strJsonData.length());//字符串拷贝到数组中
            lpInput.write();
            HCNetSDK.BYTE_ARRAY ptrOutuff = new HCNetSDK.BYTE_ARRAY(1024);
            IntByReference pInt = new IntByReference(0);
            while(true){
                /*
                    如果需要批量下发，循环调用NET_DVR_SendWithRecvRemoteConfig接口进行下发不同的卡号，下发结束完成后关闭下发卡号长连接
                 */
                int dwState = AcsMain.hCNetSDK.NET_DVR_SendWithRecvRemoteConfig(lHandler, lpInput.getPointer(), lpInput.byValue.length ,ptrOutuff.getPointer(), 1024,  pInt);
                //读取返回的json并解析
                ptrOutuff.read();
                String strResult = new String(ptrOutuff.byValue).trim();
                System.out.println("dwState:" + dwState + ",strResult:" + strResult);

                JSONObject jsonResult = new JSONObject(strResult);
                int statusCode = jsonResult.getInt("statusCode");
                String statusString = jsonResult.getString("statusString");

                if(dwState == -1){
                    System.out.println("NET_DVR_SendWithRecvRemoteConfig接口调用失败，错误码：" + AcsMain.hCNetSDK.NET_DVR_GetLastError());
                    break;
                }
                else if(dwState == HCNetSDK.NET_SDK_CONFIG_STATUS_NEED_WAIT)
                {
                    System.out.println("配置等待");
                    try {
                        Thread.sleep(10);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                    continue;
                }
                else if(dwState == HCNetSDK.NET_SDK_CONFIG_STATUS_FAILED)
                {
                    System.out.println("下发卡号失败, json retun:" + jsonResult.toString());
                    break;
                }
                else if(dwState == HCNetSDK.NET_SDK_CONFIG_STATUS_EXCEPTION)
                {
                    System.out.println("下发卡号异常, json retun:" + jsonResult.toString());
                    break;
                }
                else if(dwState == HCNetSDK.NET_SDK_CONFIG_STATUS_SUCCESS)
                {
                    if (statusCode != 1){
                        System.out.println("下发卡号成功,但是有异常情况:" + jsonResult.toString());
                    }
                    else{
                        System.out.println("下发卡号成功,  json retun:" + jsonResult.toString());
                    }
                    break;
                }
                else if(dwState == HCNetSDK.NET_SDK_CONFIG_STATUS_FINISH) {

                    System.out.println("下发卡号完成");
                    break;
                }
            }
            if(!AcsMain.hCNetSDK.NET_DVR_StopRemoteConfig(lHandler)){
                System.out.println("NET_DVR_StopRemoteConfig接口调用失败，错误码：" + AcsMain.hCNetSDK.NET_DVR_GetLastError());
            }
            else{
                System.out.println("NET_DVR_StopRemoteConfig接口成功");
            }
        }
    }


    /**
     * 按照人员工号查询卡号
     * @param userID
     * @param employeeNo
     */
    public static void searchCardInfo(int userID, String employeeNo)
    {
        String searchCardInfoUrl="POST /ISAPI/AccessControl/CardInfo/Search?format=json";
        String searchCardInfojson="{\n" +
                "    \"CardInfoSearchCond\": {\n" +
                "        \"searchID\": \"20201014001\",\n" +
                "        \"searchResultPosition\": 0,\n" +
                "        \"maxResults\": 30,\n" +
                "        \"EmployeeNoList\" : [\n" +
                "            {\n" +
                "                \"employeeNo\": \""+employeeNo+"\"\n" +
                "            }\n" +
                "        ]\n" +
                "    }\n" +
                "}";
        String result=transIsapi.put_isapi(userID,searchCardInfoUrl,searchCardInfojson);
        System.out.println(result);

    }

    /**
     * 查询所有卡号
     * @param userID
     */
    public static void searchAllCardInfo(int userID)
    {
        String searchCardInfoUrl="POST /ISAPI/AccessControl/CardInfo/Search?format=json";
        /*
        "searchID": "",    //必填,搜索记录唯一标识,用来确认上层客户端是否为同一个(倘若是同一个,设备记录内存,
                              下次搜索加快速度),string类型
        "searchResultPosition": 0,    //必填,查询结果在结果列表中的起始位置,integer32类型;当记录条数很多时,
                                一次查询不能获取所有的记录,下一次查询时指定位置可以查询后面的记录
        "maxResults": 30,    //必填,本次协议调用可获取的最大记录数,integer32类型（如maxResults值大于设备能力集返回的范围，
                              则设备按照能力集最大值返回，设备不进行报错
         */
        String searchCardInfojson="{\n" +
                "    \"CardInfoSearchCond\": {\n" +
                "        \"searchID\": \"20211129001\",\n" +
                "        \"searchResultPosition\": 0,\n" +
                "        \"maxResults\": 30\n" +
                "    }\n" +
                "}";
        String result=transIsapi.put_isapi(userID,searchCardInfoUrl,searchCardInfojson);
        System.out.println(result);
    }


    /**
     * 根据工号删除卡号，
     * @param userID
     * @param employeeNo
     */
    public static void deleteCardInfo(int userID,String employeeNo )
    {
        String deleteCardInfoURL="PUT /ISAPI/AccessControl/CardInfo/Delete?format=json ";
        String deleteCardInfojson="{\n" +
                "    \"CardInfoDelCond\" : {\n" +
                "        \"EmployeeNoList\" : [    \n" +
                "            {\n" +
                "                \"employeeNo\": \""+employeeNo+"\"    \n" +
                "            }\n" +
                "        ]\n" +
                "    }\n" +
                "}\n";
        String result=transIsapi.put_isapi(userID,deleteCardInfoURL,deleteCardInfojson);
        System.out.println(result);
    }


    /**
     * 删除全部卡号信息，
     * @param userID
     */
    public static void deleteAllCardInfo(int userID)
    {
         String deleteAllCardInfoURL="PUT /ISAPI/AccessControl/CardInfo/Delete?format=json";
         String deleteAllCardInfoJson="{\"CardInfoDelCond\" : {}}";
         /*
         如果涉及大批量卡号删除，设备需要一定的时间去处理，需要将超时时间设置成60s，put_isapi中的NET_DVR_XML_CONFIG_INPUT中dwRecvTimeOut
         参数设置成60000
          */
         String result=transIsapi.put_isapi(userID,deleteAllCardInfoURL,deleteAllCardInfoJson);
         System.out.println(result);
    }


    /**
     * 获取所有卡数量
     * @param userID
     * @return
     * @throws JSONException
     */
    public static int getAllCardNumber(int userID) throws JSONException {
        String getAllCardNumberUrl="GET /ISAPI/AccessControl/CardInfo/Count?format=json";
        String result=transIsapi.get_isapi(userID,getAllCardNumberUrl);
        System.out.println(result);
        JSONObject jsonObject=new JSONObject(result);
        int num=jsonObject.optJSONObject("CardInfoCount").getInt("cardNumber");
        return  num;
    }



}
