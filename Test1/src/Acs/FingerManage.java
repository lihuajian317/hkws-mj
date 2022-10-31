package Acs;


import NetSDKDemo.HCNetSDK;
import com.sun.jna.Pointer;
import com.sun.jna.ptr.IntByReference;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.*;
import java.nio.ByteBuffer;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * 指纹管理：指纹采集，指纹下发，指纹信息查询，指纹删除
 */
public final class FingerManage {
        /**
         * 采集指纹模块，采集指纹数据为二进制
         * @param userID
         */
        public static  void fingerCapture(int userID)
        {

                HCNetSDK.NET_DVR_CAPTURE_FINGERPRINT_COND strFingerCond = new HCNetSDK.NET_DVR_CAPTURE_FINGERPRINT_COND();
                strFingerCond.read();
                strFingerCond.dwSize = strFingerCond.size();
                strFingerCond.byFingerPrintPicType = 1;  //指纹读卡器
                strFingerCond.byFingerNo = 1;   //指纹编号
                strFingerCond.write();
                int lGetFingerHandle = AcsMain.hCNetSDK.NET_DVR_StartRemoteConfig(userID, HCNetSDK.NET_DVR_CAPTURE_FINGERPRINT_INFO, strFingerCond.getPointer(), strFingerCond.dwSize, null, null);
                if (lGetFingerHandle == -1) {
                        System.out.println("建立采集指纹长连接失败，错误码为：" + AcsMain.hCNetSDK.NET_DVR_GetLastError());
                        return;
                } else {
                        System.out.println("建立采集指纹长连接成功！");
                }
                HCNetSDK.NET_DVR_CAPTURE_FINGERPRINT_CFG strFingerCfg = new HCNetSDK.NET_DVR_CAPTURE_FINGERPRINT_CFG();
                strFingerCfg.dwSize=strFingerCfg.size();
                strFingerCfg.write();
                while (true) {
                        int dwFingerState = AcsMain.hCNetSDK.NET_DVR_GetNextRemoteConfig(lGetFingerHandle, strFingerCfg.getPointer(), strFingerCfg.size());

                        if (dwFingerState == -1) {
                                System.out.println("NET_DVR_GetNextRemoteConfig采集指纹失败，错误码：" + AcsMain.hCNetSDK.NET_DVR_GetLastError());
                                break;
                        } else if (dwFingerState == HCNetSDK.NET_SDK_GET_NEXT_STATUS_FAILED) {
                                System.out.println("采集指纹失败");
                                break;
                        } else if (dwFingerState == HCNetSDK.NET_SDK_GET_NEXT_STATUS_NEED_WAIT) {
                                System.out.println("正在采集指纹中,请等待...");
                                try {
                                        Thread.sleep(10);
                                } catch (InterruptedException e) {
                                        // TODO Auto-generated catch block
                                        e.printStackTrace();
                                }
                                continue;
                        } else if (dwFingerState == HCNetSDK.NET_SDK_CONFIG_STATUS_EXCEPTION) {
                                //超时时间5秒内设备本地人脸采集失败就会返回失败,连接会断开
                                System.out.println("采集指纹异常, 网络异常导致连接断开 ");
                                break;
                        } else if (dwFingerState == HCNetSDK.NET_SDK_GET_NEXT_STATUS_SUCCESS) {
                                strFingerCfg.read();
                                SimpleDateFormat sf = new SimpleDateFormat("yyyyMMddHHmmss");
                                String newName = sf.format(new Date());
                                String fileName = newName + "_capFinger.data";
                                String filePath = "..\\pic\\" + fileName;
                                BufferedOutputStream bos = null;
                                FileOutputStream fos = null;
                                File file = null;
                                try {
                                        File dir = new File(filePath);
                                        if (!dir.exists() && dir.isDirectory()) {//判断文件目录是否存在
                                                dir.mkdirs();
                                        }
                                        file = new File(filePath);
                                        fos = new FileOutputStream(file);
                                        bos = new BufferedOutputStream(fos);
                                        bos.write(strFingerCfg.byFingerData);
                                        System.out.println("采集指纹成功！");
                                } catch (Exception e) {
                                        e.printStackTrace();
                                } finally {
                                        if (bos != null) {
                                                try {
                                                        bos.close();
                                                } catch (IOException e1) {
                                                        e1.printStackTrace();
                                                }
                                        }
                                        if (fos != null) {
                                                try {
                                                        fos.close();
                                                } catch (IOException e1) {
                                                        e1.printStackTrace();
                                                }
                                        }
                                }
                                if ((strFingerCfg.dwFingerPrintPicSize > 0) && (strFingerCfg.pFingerPrintPicBuffer != null)) {
                                        FileOutputStream fout;
                                        try {
                                                String filename = "..\\pic\\" + newName + "_FingerPrintPic.jpg";
                                                fout = new FileOutputStream(filename);
                                                //将字节写入文件
                                                long offset = 0;
                                                ByteBuffer buffers = strFingerCfg.pFingerPrintPicBuffer.getByteBuffer(offset, strFingerCfg.dwFingerPrintPicSize);
                                                byte[] bytes = new byte[strFingerCfg.dwFingerPrintPicSize];
                                                buffers.rewind();
                                                buffers.get(bytes);
                                                fout.write(bytes);
                                                fout.close();
                                                System.out.println("采集指纹成功, 图片保存路径: " + filename);
                                        } catch (FileNotFoundException e) {
                                                // TODO Auto-generated catch block
                                                e.printStackTrace();
                                        } catch (IOException e) {
                                                // TODO Auto-generated catch block
                                                e.printStackTrace();
                                        }
                                }
                                break;
                        } else {
                                System.out.println("其他异常, dwState: " + dwFingerState);
                                break;
                        }
                }
                //采集成功之后断开连接、释放资源
                if (!AcsMain.hCNetSDK.NET_DVR_StopRemoteConfig(lGetFingerHandle)) {
                        System.out.println("NET_DVR_StopRemoteConfig接口调用失败，错误码：" + AcsMain.hCNetSDK.NET_DVR_GetLastError());
                } else {
                        System.out.println("NET_DVR_StopRemoteConfig接口成功");
                }
        }

        /**
         * 采用透传ISAPI协议方式采集指纹，获取的指纹信息为BASE64编码，
         */
        public static void  fingerCpaureByisapi(int userID)
        {
                //采集指纹URL
              String fingerCapUrl="POST /ISAPI/AccessControl/CaptureFingerPrint";
              String XmlInput="<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
                      "<CaptureFingerPrintCond xmlns=\"http://www.isapi.org/ver20/XMLSchema\" version=\"2.0\">\n" +
                      "  <fingerNo>1</fingerNo>\n" +
                      "</CaptureFingerPrintCond>";
              String result=transIsapi.put_isapi(userID,fingerCapUrl,XmlInput);
                System.out.println("采集指纹结果:"+result);
        }



        public static void setOneFinger(int userID, String employeeNo,String figerdata) throws JSONException {
                HCNetSDK.BYTE_ARRAY ptrByteArray = new HCNetSDK.BYTE_ARRAY(1024);    //数组
                String strInBuffer = "POST /ISAPI/AccessControl/FingerPrint/SetUp?format=json";
                System.arraycopy(strInBuffer.getBytes(), 0, ptrByteArray.byValue, 0, strInBuffer.length());//字符串拷贝到数组中
                ptrByteArray.write();

                int lHandler = AcsMain.hCNetSDK.NET_DVR_StartRemoteConfig(userID, HCNetSDK.NET_DVR_JSON_CONFIG, ptrByteArray.getPointer(), strInBuffer.length(), null, null);
                if (lHandler < 0)
                {
                        System.out.println("SearchFaceInfo NET_DVR_StartRemoteConfig 失败,错误码为"+AcsMain.hCNetSDK.NET_DVR_GetLastError());
                        return;
                }
                else{
                        System.out.println("SearchFaceInfo NET_DVR_StartRemoteConfig成功!");

                        JSONObject jsonObject = new JSONObject();
                        JSONObject j_FingerPrintCond=new JSONObject();
                        j_FingerPrintCond.put("employeeNo", employeeNo);
                        int[] CardReader = {1};
                        j_FingerPrintCond.put("enableCardReader",CardReader); //人员工号
                        j_FingerPrintCond.put("fingerPrintID", 1);
                        j_FingerPrintCond.put("fingerType", "normalFP");
                        j_FingerPrintCond.put("fingerData",figerdata);
                        jsonObject.put("FingerPrintCfg",j_FingerPrintCond);

                        String strInbuff = jsonObject.toString();
                        System.out.println("查询的json报文:" + strInbuff);

                        //把string传递到Byte数组中，后续用.getPointer()方法传入指针地址中。
                        HCNetSDK.BYTE_ARRAY ptrInbuff = new HCNetSDK.BYTE_ARRAY(strInbuff.length());
                        System.arraycopy(strInbuff.getBytes(), 0, ptrInbuff.byValue, 0, strInbuff.length());
                        ptrInbuff.write();

                        HCNetSDK.NET_DVR_STRING_POINTER ptrOutbuff = new HCNetSDK.NET_DVR_STRING_POINTER(3*1024);
                        ptrOutbuff.write();

                        IntByReference pInt = new IntByReference(0);

                        while(true){
                                int dwState =AcsMain.hCNetSDK.NET_DVR_SendWithRecvRemoteConfig(lHandler, ptrInbuff.getPointer(), strInbuff.length(), ptrOutbuff.getPointer(), ptrOutbuff.size(), pInt);
                                ptrOutbuff.read();
                                System.out.println(dwState);
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
                                                // TODO Auto-generated catch block
                                                e.printStackTrace();
                                        }
                                        continue;
                                }
                                else if(dwState == HCNetSDK.NET_SDK_CONFIG_STATUS_FAILED)
                                {
                                        System.out.println("下发指纹失败");
                                        break;
                                }
                                else if(dwState == HCNetSDK.NET_SDK_CONFIG_STATUS_EXCEPTION)
                                {
                                        System.out.println("下发指纹异常");
                                        break;
                                }
                                else if(dwState == HCNetSDK.NET_SDK_CONFIG_STATUS_SUCCESS)
                                {
                                        System.out.println("下发指纹成功");
                                        //解析JSON字符串
                                        ptrOutbuff.read();
                                        System.out.println("返回的报文："+new String(ptrOutbuff.byString));

                                        break;
                                }
                                else if(dwState == HCNetSDK.NET_SDK_CONFIG_STATUS_FINISH) {
                                        System.out.println("下发指纹完成");
                                        break;
                                }
                        }
                        if(!AcsMain.hCNetSDK.NET_DVR_StopRemoteConfig(lHandler)){
                                System.out.println("NET_DVR_StopRemoteConfig接口调用失败，错误码：" + AcsMain.hCNetSDK.NET_DVR_GetLastError());
                        }
                        else{
                                System.out.println("NET_DVR_StopRemoteConfig接口成功");
                                lHandler = -1;
                        }
                }
        }

        public static void SearchFingerInfo(int userID,String employeeNo) throws JSONException {
                HCNetSDK.BYTE_ARRAY ptrByteArray = new HCNetSDK.BYTE_ARRAY(1024);    //数组
                String strInBuffer = "POST /ISAPI/AccessControl/FingerPrintUpload?format=json";
                System.arraycopy(strInBuffer.getBytes(), 0, ptrByteArray.byValue, 0, strInBuffer.length());//字符串拷贝到数组中
                ptrByteArray.write();

                int lHandler = AcsMain.hCNetSDK.NET_DVR_StartRemoteConfig(userID, HCNetSDK.NET_DVR_JSON_CONFIG, ptrByteArray.getPointer(), strInBuffer.length(), null, null);
                if (lHandler < 0)
                {
                        System.out.println("SearchFaceInfo NET_DVR_StartRemoteConfig 失败,错误码为"+AcsMain.hCNetSDK.NET_DVR_GetLastError());
                        return;
                }
                else{
                        System.out.println("SearchFaceInfo NET_DVR_StartRemoteConfig成功!");

                        JSONObject jsonObject = new JSONObject();
                        JSONObject j_FingerPrintCond=new JSONObject();
                        j_FingerPrintCond.put("searchID", "20211223");
                        j_FingerPrintCond.put("employeeNo", employeeNo); //人员工号
                        j_FingerPrintCond.put("cardReaderNo", 1);
                        j_FingerPrintCond.put("fingerPrintID", 1);
                        jsonObject.put("FingerPrintCond",j_FingerPrintCond);

                        String strInbuff = jsonObject.toString();
                        System.out.println("查询的json报文:" + strInbuff);

                        //把string传递到Byte数组中，后续用.getPointer()方法传入指针地址中。
                        HCNetSDK.BYTE_ARRAY ptrInbuff = new HCNetSDK.BYTE_ARRAY(strInbuff.length());
                        System.arraycopy(strInbuff.getBytes(), 0, ptrInbuff.byValue, 0, strInbuff.length());
                        ptrInbuff.write();

                        HCNetSDK.NET_DVR_STRING_POINTER ptrOutbuff = new HCNetSDK.NET_DVR_STRING_POINTER(3*1024);
                        ptrOutbuff.write();

                        IntByReference pInt = new IntByReference(0);

                        while(true){
                                int dwState =AcsMain.hCNetSDK.NET_DVR_SendWithRecvRemoteConfig(lHandler, ptrInbuff.getPointer(), strInbuff.length(), ptrOutbuff.getPointer(), ptrOutbuff.size(), pInt);
                                ptrOutbuff.read();
                                System.out.println(dwState);
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
                                                // TODO Auto-generated catch block
                                                e.printStackTrace();
                                        }
                                        continue;
                                }
                                else if(dwState == HCNetSDK.NET_SDK_CONFIG_STATUS_FAILED)
                                {
                                        System.out.println("查询指纹失败");
                                        break;
                                }
                                else if(dwState == HCNetSDK.NET_SDK_CONFIG_STATUS_EXCEPTION)
                                {
                                        System.out.println("查询指纹异常");
                                        break;
                                }
                                else if(dwState == HCNetSDK.NET_SDK_CONFIG_STATUS_SUCCESS)
                                {
                                        System.out.println("查询指纹成功");
                                        //解析JSON字符串
                                        ptrOutbuff.read();
                                        System.out.println("查询的报文："+new String(ptrOutbuff.byString));

                                        break;
                                }
                                else if(dwState == HCNetSDK.NET_SDK_CONFIG_STATUS_FINISH) {
                                        System.out.println("获取指纹完成");
                                        break;
                                }
                        }
                        if(!AcsMain.hCNetSDK.NET_DVR_StopRemoteConfig(lHandler)){
                                System.out.println("NET_DVR_StopRemoteConfig接口调用失败，错误码：" + AcsMain.hCNetSDK.NET_DVR_GetLastError());
                        }
                        else{
                                System.out.println("NET_DVR_StopRemoteConfig接口成功");
                                lHandler = -1;
                        }

                }
        }

        /**
         * 按工号和读卡器删除指纹，按工号是逐个删除，按读卡器是批量删除指定读卡器上所有的指纹数据
         * @param userID
         * @param employeeNo
         */
        public static void deleteFinger(int userID,String employeeNo )
        {
                int iErr = 0;
                HCNetSDK.NET_DVR_FINGER_PRINT_INFO_CTRL_V50 m_struFingerDelInfoParam = new HCNetSDK.NET_DVR_FINGER_PRINT_INFO_CTRL_V50();
                m_struFingerDelInfoParam.dwSize = m_struFingerDelInfoParam.size();
                m_struFingerDelInfoParam.byMode = 0;// 删除方式，0-按卡号（人员ID）方式删除，1-按读卡器删除
                m_struFingerDelInfoParam.struProcessMode.setType(HCNetSDK.NET_DVR_FINGER_PRINT_BYCARD_V50.class);
                for (int i = 0; i < employeeNo.length(); i++) {
                        m_struFingerDelInfoParam.struProcessMode.struByCard.byEmployeeNo[i] = employeeNo.getBytes()[i];
                }
                m_struFingerDelInfoParam.struProcessMode.struByCard.byEnableCardReader[0] = 1;//指纹的读卡器信息，按位表示
                m_struFingerDelInfoParam.struProcessMode.struByCard.byFingerPrintID[0] = 1;//需要删除的指纹编号，按数组下标，值表示0-不删除，1-删除该指纹 ,指纹编号1删除


                Pointer lpInBuffer1 = m_struFingerDelInfoParam.getPointer();
                m_struFingerDelInfoParam.write();


                int lHandle = AcsMain.hCNetSDK.NET_DVR_StartRemoteConfig(userID, HCNetSDK.NET_DVR_DEL_FINGERPRINT, lpInBuffer1, m_struFingerDelInfoParam.size(), null, null);
                if (lHandle < 0) {
                        iErr = AcsMain.hCNetSDK.NET_DVR_GetLastError();
                        System.out.println("NET_DVR_DEL_FINGERPRINT_CFG_V50 建立长连接失败，错误号：" + iErr);
                        return;
                }
                while (true) {
                        HCNetSDK.NET_DVR_FINGER_PRINT_INFO_CTRL_V50 v50 = new HCNetSDK.NET_DVR_FINGER_PRINT_INFO_CTRL_V50();
                        v50.dwSize = v50.size();
                        v50.write();
                        int res = AcsMain.hCNetSDK.NET_DVR_GetNextRemoteConfig(lHandle, v50.getPointer(), v50.size());
                        if (res == 1002) {
                                AcsMain.hCNetSDK.NET_DVR_StopRemoteConfig(lHandle);
                                System.out.println("删除指纹成功！！！");
                                break;
                        } else if (res == 1003) {
                                System.out.println("接口失败，错误码：" + AcsMain.hCNetSDK.NET_DVR_GetLastError());
                                AcsMain.hCNetSDK.NET_DVR_StopRemoteConfig(lHandle);
                                break;
                        }
                }
        }
}
