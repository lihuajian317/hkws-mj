package Acs;


import NetSDKDemo.HCNetSDK;
import com.sun.jna.Pointer;
import com.sun.jna.ptr.IntByReference;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.*;
import java.nio.ByteBuffer;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * 功能模块：人员管理，下发、查询、删除人脸图片，注：下发人脸图片前，先下发人员工号。
 */
public final class FaceManage {


    /**
     * 功能：按照二进制方式下发人脸图片
     *
     * @param userID     用户注册ID
     * @param employeeNo 人员工号
     * @throws JSONException
     * @throws InterruptedException
     */
    public static void AddFaceByBinary(int userID, String employeeNo) throws JSONException, InterruptedException {
        HCNetSDK.BYTE_ARRAY ptrByteArray = new HCNetSDK.BYTE_ARRAY(1024);    //数组
        String strInBuffer = "PUT /ISAPI/Intelligent/FDLib/FDSetUp?format=json";
        System.arraycopy(strInBuffer.getBytes(), 0, ptrByteArray.byValue, 0, strInBuffer.length());//字符串拷贝到数组中
        ptrByteArray.write();

        int lHandler = AcsMain.hCNetSDK.NET_DVR_StartRemoteConfig(userID, HCNetSDK.NET_DVR_FACE_DATA_RECORD, ptrByteArray.getPointer(), strInBuffer.length(), null, null);
        if (lHandler < 0) {
            System.out.println("Addface NET_DVR_StartRemoteConfig 失败,错误码为" + AcsMain.hCNetSDK.NET_DVR_GetLastError());
            return;
        } else {
            System.out.println("Addface NET_DVR_StartRemoteConfig 成功!");

            HCNetSDK.NET_DVR_JSON_DATA_CFG struAddFaceDataCfg = new HCNetSDK.NET_DVR_JSON_DATA_CFG();
            struAddFaceDataCfg.read();

            JSONObject jsonObject = new JSONObject();
            jsonObject.put("faceLibType", "blackFD"); // 可选参数，人脸库类型,blackFD-名单库
            jsonObject.put("FDID", "1"); // 人脸库id，门禁设备默认为1
            jsonObject.put("FPID", employeeNo);//人脸下发关联的工号

            String strJsonData = jsonObject.toString();
            System.arraycopy(strJsonData.getBytes(), 0, ptrByteArray.byValue, 0, strJsonData.length());//字符串拷贝到数组中
            ptrByteArray.write();
            struAddFaceDataCfg.dwSize = struAddFaceDataCfg.size();
            struAddFaceDataCfg.lpJsonData = ptrByteArray.getPointer();
            struAddFaceDataCfg.dwJsonDataSize = strJsonData.length();

            /*****************************************
             * 从本地文件里面读取JPEG图片二进制数据
             *****************************************/
            FileInputStream picfile = null;
            int picdataLength = 0;
            try {

                picfile = new FileInputStream(new File("C:\\ideawork\\HK-entrance-guard\\AddFacePicture\\lhj.jpg"));
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            }

            try {
                picdataLength = picfile.available();
            } catch (IOException e1) {
                e1.printStackTrace();
            }
            if (picdataLength < 0) {
                System.out.println("input file dataSize < 0");
                return;
            }

            HCNetSDK.BYTE_ARRAY ptrpicByte = new HCNetSDK.BYTE_ARRAY(picdataLength);
            try {
                picfile.read(ptrpicByte.byValue);
            } catch (IOException e2) {
                e2.printStackTrace();
            }
            ptrpicByte.write();
            struAddFaceDataCfg.dwPicDataSize = picdataLength;
            struAddFaceDataCfg.lpPicData = ptrpicByte.getPointer();
            struAddFaceDataCfg.write();

            HCNetSDK.BYTE_ARRAY ptrOutuff = new HCNetSDK.BYTE_ARRAY(1024);

            IntByReference pInt = new IntByReference(0);

            while (true) {
                int dwState = AcsMain.hCNetSDK.NET_DVR_SendWithRecvRemoteConfig(lHandler, struAddFaceDataCfg.getPointer(), struAddFaceDataCfg.dwSize, ptrOutuff.getPointer(), 1024, pInt);
                //读取返回的json并解析
                ptrOutuff.read();
                String strResult = new String(ptrOutuff.byValue).trim();
                System.out.println("dwState:" + dwState + ",strResult:" + strResult);

                if (dwState == -1) {
                    System.out.println("NET_DVR_SendWithRecvRemoteConfig接口调用失败，错误码：" + AcsMain.hCNetSDK.NET_DVR_GetLastError());
                    break;
                } else if (dwState == HCNetSDK.NET_SDK_CONFIG_STATUS_NEED_WAIT) {
                    System.out.println("配置等待");
                    Thread.sleep(10);
                    continue;
                }

                JSONObject jsonResult = new JSONObject(strResult);
                int statusCode = jsonResult.getInt("statusCode");
                String statusString = jsonResult.getString("statusString");

                if (dwState == HCNetSDK.NET_SDK_CONFIG_STATUS_FAILED) {
                    System.out.println("下发人脸失败, json retun:" + jsonResult.toString());
                    break;
                } else if (dwState == HCNetSDK.NET_SDK_CONFIG_STATUS_EXCEPTION) {
                    System.out.println("下发人脸异常, json retun:" + jsonResult.toString());
                    break;
                } else if (dwState == HCNetSDK.NET_SDK_CONFIG_STATUS_SUCCESS) {//返回NET_SDK_CONFIG_STATUS_SUCCESS代表流程走通了，但并不代表下发成功，比如人脸图片不符合设备规范等原因，所以需要解析Json报文
                    if (statusCode != 1) {
                        System.out.println("下发人脸成功,但是有异常情况:" + jsonResult.toString());
                    } else {
                        System.out.println("下发人脸成功,  json retun:" + jsonResult.toString());
                    }
                    break;
                } else if (dwState == HCNetSDK.NET_SDK_CONFIG_STATUS_FINISH) {
                    //下发人脸时：dwState其实不会走到这里，因为设备不知道我们会下发多少个人，所以长连接需要我们主动关闭
                    System.out.println("下发人脸完成");
                    break;
                }


            }
            if (!AcsMain.hCNetSDK.NET_DVR_StopRemoteConfig(lHandler)) {
                System.out.println("NET_DVR_StopRemoteConfig接口调用失败，错误码：" + AcsMain.hCNetSDK.NET_DVR_GetLastError());
            } else {
                System.out.println("NET_DVR_StopRemoteConfig接口成功");
            }
        }

    }


    /**
     * 按URL方式下发人脸图片
     *
     * @param userID     用户注销ID
     * @param employeeNo 人员工号
     * @throws JSONException
     */
    public static void AddFaceByUrl(int userID, String employeeNo) throws JSONException {
        HCNetSDK.BYTE_ARRAY ptrByteArray = new HCNetSDK.BYTE_ARRAY(1024);    //数组
        String strInBuffer = "PUT /ISAPI/Intelligent/FDLib/FDSetUp?format=json";
        System.arraycopy(strInBuffer.getBytes(), 0, ptrByteArray.byValue, 0, strInBuffer.length());//字符串拷贝到数组中
        ptrByteArray.write();

        int lHandler = AcsMain.hCNetSDK.NET_DVR_StartRemoteConfig(userID, HCNetSDK.NET_DVR_FACE_DATA_RECORD, ptrByteArray.getPointer(), strInBuffer.length(), null, null);
        if (lHandler < 0) {
            System.out.println("Addface NET_DVR_StartRemoteConfig 失败,错误码为" + AcsMain.hCNetSDK.NET_DVR_GetLastError());
            return;
        } else {
            System.out.println("Addface NET_DVR_StartRemoteConfig 成功!");

            HCNetSDK.NET_DVR_JSON_DATA_CFG struAddFaceDataCfg = new HCNetSDK.NET_DVR_JSON_DATA_CFG();
            struAddFaceDataCfg.read();

            JSONObject jsonObject = new JSONObject();
            jsonObject.put("faceURL", "http://10.17.34.106:6011/pic?7DD9D70207A9D7F576F99AC197B2D6CAface.jpg"); //人脸图片URL
            jsonObject.put("faceLibType", "blackFD");
            jsonObject.put("FDID", "1");
            jsonObject.put("FPID", employeeNo);//人脸下发关联的工号

            String strJsonData = jsonObject.toString();
            System.arraycopy(strJsonData.getBytes(), 0, ptrByteArray.byValue, 0, strJsonData.length());//字符串拷贝到数组中
            ptrByteArray.write();
            struAddFaceDataCfg.dwSize = struAddFaceDataCfg.size();
            struAddFaceDataCfg.lpJsonData = ptrByteArray.getPointer();
            struAddFaceDataCfg.dwJsonDataSize = strJsonData.length();
            struAddFaceDataCfg.lpPicData = null;
            struAddFaceDataCfg.dwPicDataSize = 0;
            struAddFaceDataCfg.write();
            HCNetSDK.BYTE_ARRAY ptrOutuff = new HCNetSDK.BYTE_ARRAY(1024);

            IntByReference pInt = new IntByReference(0);

            while (true) {
                int dwState = AcsMain.hCNetSDK.NET_DVR_SendWithRecvRemoteConfig(lHandler, struAddFaceDataCfg.getPointer(), struAddFaceDataCfg.dwSize, ptrOutuff.getPointer(), 1024, pInt);
                //读取返回的json并解析
                ptrOutuff.read();
                String strResult = new String(ptrOutuff.byValue).trim();
                System.out.println("dwState:" + dwState + ",strResult:" + strResult);

                JSONObject jsonResult = new JSONObject(strResult);
                int statusCode = jsonResult.getInt("statusCode");
                String statusString = jsonResult.getString("statusString");


                if (dwState == -1) {
                    System.out.println("NET_DVR_SendWithRecvRemoteConfig接口调用失败，错误码：" + AcsMain.hCNetSDK.NET_DVR_GetLastError());
                    break;
                } else if (dwState == HCNetSDK.NET_SDK_CONFIG_STATUS_NEED_WAIT) {
                    System.out.println("配置等待");
                    try {
                        Thread.sleep(10);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                    continue;
                } else if (dwState == HCNetSDK.NET_SDK_CONFIG_STATUS_FAILED) {
                    System.out.println("下发人脸失败, json retun:" + jsonResult.toString());
                    break;
                } else if (dwState == HCNetSDK.NET_SDK_CONFIG_STATUS_EXCEPTION) {
                    System.out.println("下发人脸异常, json retun:" + jsonResult.toString());
                    break;
                } else if (dwState == HCNetSDK.NET_SDK_CONFIG_STATUS_SUCCESS) {//返回NET_SDK_CONFIG_STATUS_SUCCESS代表流程走通了，但并不代表下发成功，比如人脸图片不符合设备规范等原因，所以需要解析Json报文
                    if (statusCode != 1) {
                        System.out.println("下发人脸成功,但是有异常情况:" + jsonResult.toString());
                    } else {
                        System.out.println("下发人脸成功,  json retun:" + jsonResult.toString());
                    }
                    break;
                } else if (dwState == HCNetSDK.NET_SDK_CONFIG_STATUS_FINISH) {
                    //下发人脸时：dwState其实不会走到这里，因为设备不知道我们会下发多少个人，所以长连接需要我们主动关闭
                    System.out.println("下发人脸完成");
                    break;
                }
            }
            if (!AcsMain.hCNetSDK.NET_DVR_StopRemoteConfig(lHandler)) {
                System.out.println("NET_DVR_StopRemoteConfig接口调用失败，错误码：" + AcsMain.hCNetSDK.NET_DVR_GetLastError());
            } else {
                System.out.println("NET_DVR_StopRemoteConfig接口成功");
            }
        }
    }


    /**
     * 查询人脸
     *
     * @param userID
     * @param employeeNo
     * @throws JSONException
     */
    public static void SearchFaceInfo(int userID, String employeeNo) throws JSONException {
        HCNetSDK.BYTE_ARRAY ptrByteArray = new HCNetSDK.BYTE_ARRAY(1024);    //数组
        String strInBuffer = "POST /ISAPI/Intelligent/FDLib/FDSearch?format=json";
        System.arraycopy(strInBuffer.getBytes(), 0, ptrByteArray.byValue, 0, strInBuffer.length());//字符串拷贝到数组中
        ptrByteArray.write();

        int lHandler = AcsMain.hCNetSDK.NET_DVR_StartRemoteConfig(userID, HCNetSDK.NET_DVR_FACE_DATA_SEARCH, ptrByteArray.getPointer(), strInBuffer.length(), null, null);
        if (lHandler < 0) {
            System.out.println("SearchFaceInfo NET_DVR_StartRemoteConfig 失败,错误码为" + AcsMain.hCNetSDK.NET_DVR_GetLastError());
            return;
        } else {
            System.out.println("SearchFaceInfo NET_DVR_StartRemoteConfig成功!");

            JSONObject jsonObject = new JSONObject();
            jsonObject.put("searchResultPosition", 0);
            jsonObject.put("maxResults", 1);
            jsonObject.put("faceLibType", "blackFD");
            jsonObject.put("FDID", "1");
            jsonObject.put("FPID", employeeNo);//人脸关联的工号，同下发人员时的employeeNo字段

            String strInbuff = jsonObject.toString();
            System.out.println("查询的json报文:" + strInbuff);

            //把string传递到Byte数组中，后续用.getPointer()方法传入指针地址中。
            HCNetSDK.BYTE_ARRAY ptrInbuff = new HCNetSDK.BYTE_ARRAY(strInbuff.length());
            System.arraycopy(strInbuff.getBytes(), 0, ptrInbuff.byValue, 0, strInbuff.length());
            ptrInbuff.write();

            HCNetSDK.NET_DVR_JSON_DATA_CFG m_struJsonData = new HCNetSDK.NET_DVR_JSON_DATA_CFG();
            m_struJsonData.write();

            IntByReference pInt = new IntByReference(0);

            while (true) {
                int dwState = AcsMain.hCNetSDK.NET_DVR_SendWithRecvRemoteConfig(lHandler, ptrInbuff.getPointer(), strInbuff.length(), m_struJsonData.getPointer(), m_struJsonData.size(), pInt);
                m_struJsonData.read();
                System.out.println(dwState);
                if (dwState == -1) {
                    System.out.println("NET_DVR_SendWithRecvRemoteConfig接口调用失败，错误码：" + AcsMain.hCNetSDK.NET_DVR_GetLastError());
                    break;
                } else if (dwState == HCNetSDK.NET_SDK_CONFIG_STATUS_NEED_WAIT) {
                    System.out.println("配置等待");
                    try {
                        Thread.sleep(10);
                    } catch (InterruptedException e) {
                        // TODO Auto-generated catch block
                        e.printStackTrace();
                    }
                    continue;
                } else if (dwState == HCNetSDK.NET_SDK_CONFIG_STATUS_FAILED) {
                    System.out.println("查询人脸失败");
                    break;
                } else if (dwState == HCNetSDK.NET_SDK_CONFIG_STATUS_EXCEPTION) {
                    System.out.println("查询人脸异常");
                    break;
                } else if (dwState == HCNetSDK.NET_SDK_CONFIG_STATUS_SUCCESS) {
                    System.out.println("查询人脸成功");

                    //解析JSON字符串
                    HCNetSDK.BYTE_ARRAY pJsonData = new HCNetSDK.BYTE_ARRAY(m_struJsonData.dwJsonDataSize);
                    pJsonData.write();
                    Pointer pPlateInfo = pJsonData.getPointer();
                    pPlateInfo.write(0, m_struJsonData.lpJsonData.getByteArray(0, pJsonData.size()), 0, pJsonData.size());
                    pJsonData.read();
                    String strResult = new String(pJsonData.byValue).trim();
                    System.out.println("strResult:" + strResult);
                    JSONObject jsonResult = new JSONObject(strResult);

                    int numOfMatches = jsonResult.getInt("numOfMatches");
                    if (numOfMatches != 0) {//确认有人脸
                        JSONArray MatchList = jsonResult.getJSONArray("MatchList");
                        JSONObject MatchList_1 = MatchList.optJSONObject(0);
                        String FPID = MatchList_1.getString("FPID"); //获取json中人脸关联的工号

                        FileOutputStream fout;
                        try {
                            fout = new FileOutputStream("..//AddFacePicture//[" + FPID + "]_FacePic.jpg");
                            //将字节写入文件
                            long offset = 0;
                            ByteBuffer buffers = m_struJsonData.lpPicData.getByteBuffer(offset, m_struJsonData.dwPicDataSize);
                            byte[] bytes = new byte[m_struJsonData.dwPicDataSize];
                            buffers.rewind();
                            buffers.get(bytes);
                            fout.write(bytes);
                            fout.close();
                        } catch (FileNotFoundException e) {
                            // TODO Auto-generated catch block
                            e.printStackTrace();
                        } catch (IOException e) {
                            // TODO Auto-generated catch block
                            e.printStackTrace();
                        }
                    }
                    break;
                } else if (dwState == HCNetSDK.NET_SDK_CONFIG_STATUS_FINISH) {
                    System.out.println("获取人脸完成");
                    break;
                }
            }
            if (!AcsMain.hCNetSDK.NET_DVR_StopRemoteConfig(lHandler)) {
                System.out.println("NET_DVR_StopRemoteConfig接口调用失败，错误码：" + AcsMain.hCNetSDK.NET_DVR_GetLastError());
            } else {
                System.out.println("NET_DVR_StopRemoteConfig接口成功");
                lHandler = -1;
            }

        }

    }

    /**
     * 人脸删除，支持批量删除，json中添加多个工号
     *
     * @param userID
     * @param employeeNo
     */

    public static void DeleteFaceInfo(int userID, String employeeNo) {
        String deleteFaceUrl = "PUT /ISAPI/Intelligent/FDLib/FDSearch/Delete?format=json&FDID=1&faceLibType=blackFD";
        String deleteFaceJson = "{\n" +
                "    \"FPID\": [{\n" +
                "        \"value\": \"" + employeeNo + "\"\n" +
                "    }]\n" +
                "}";
        String result = transIsapi.put_isapi(userID, deleteFaceUrl, deleteFaceJson);
        System.out.println("删除人员结果：" + result);
    }

    /**
     * 人脸采集，下发人脸采集命令，从设备中采集人脸图片保存到本地
     *
     * @param userID 用户注册ID
     */
    public static void CaptureFaceInfo(int userID) {
        HCNetSDK.NET_DVR_CAPTURE_FACE_COND struCapCond = new HCNetSDK.NET_DVR_CAPTURE_FACE_COND();
        struCapCond.read();
        struCapCond.dwSize = struCapCond.size();
        struCapCond.write();
        int lCaptureFaceHandle = AcsMain.hCNetSDK.NET_DVR_StartRemoteConfig(userID, HCNetSDK.NET_DVR_CAPTURE_FACE_INFO, struCapCond.getPointer(), struCapCond.size(), null, null);
        if (lCaptureFaceHandle == -1) {
            System.out.println("建立采集人脸长连接失败，错误码为" + AcsMain.hCNetSDK.NET_DVR_GetLastError());
            return;
        } else {
            System.out.println("建立采集人脸长连接成功！");
        }
        //采集的人脸信息
        HCNetSDK.NET_DVR_CAPTURE_FACE_CFG struFaceInfo = new HCNetSDK.NET_DVR_CAPTURE_FACE_CFG();
        struFaceInfo.read();
        while (true) {
            int dwState = AcsMain.hCNetSDK.NET_DVR_GetNextRemoteConfig(lCaptureFaceHandle, struFaceInfo.getPointer(), struFaceInfo.size());
            struFaceInfo.read();
            if (dwState == -1) {
                System.out.println("NET_DVR_GetNextRemoteConfig采集人脸失败，错误码：" + AcsMain.hCNetSDK.NET_DVR_GetLastError());
                break;
            } else if (dwState == HCNetSDK.NET_SDK_CONFIG_STATUS_NEED_WAIT) {
                System.out.println("正在采集中,请等待...");
                try {
                    Thread.sleep(10);
                } catch (InterruptedException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }
                continue;
            } else if (dwState == HCNetSDK.NET_SDK_CONFIG_STATUS_FAILED) {
                System.out.println("采集人脸失败");
                break;
            } else if (dwState == HCNetSDK.NET_SDK_CONFIG_STATUS_EXCEPTION) {
                //超时时间5秒内设备本地人脸采集失败就会返回失败,连接会断开
                System.out.println("采集人脸异常, 网络异常导致连接断开 ");
                break;
            } else if (dwState == HCNetSDK.NET_SDK_CONFIG_STATUS_SUCCESS) {
                if ((struFaceInfo.dwFacePicSize > 0) && (struFaceInfo.pFacePicBuffer != null)) {
                    SimpleDateFormat sf = new SimpleDateFormat("yyyyMMddHHmmss");
                    String newName = sf.format(new Date());
                    FileOutputStream fout;
                    try {
                        String filename = "..\\pic\\" + newName + "_capFaceInfo.jpg";
                        fout = new FileOutputStream(filename);
                        //将字节写入文件
                        long offset = 0;
                        ByteBuffer buffers = struFaceInfo.pFacePicBuffer.getByteBuffer(offset, struFaceInfo.dwFacePicSize);
                        byte[] bytes = new byte[struFaceInfo.dwFacePicSize];
                        buffers.rewind();
                        buffers.get(bytes);
                        fout.write(bytes);
                        fout.close();
                        System.out.println("采集人脸成功, 图片保存路径: " + filename);
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
                System.out.println("其他异常, dwState: " + dwState);
                break;
            }
        }
        //采集成功之后断开连接、释放资源
        if (!AcsMain.hCNetSDK.NET_DVR_StopRemoteConfig(lCaptureFaceHandle)) {
            System.out.println("NET_DVR_StopRemoteConfig接口调用失败，错误码：" + AcsMain.hCNetSDK.NET_DVR_GetLastError());
        } else {
            System.out.println("NET_DVR_StopRemoteConfig接口成功");
        }
    }


}
