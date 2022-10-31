package NetSDKDemo;


import com.sun.jna.Pointer;

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;

//布防回调函数
public class FMSGCallBack_V31 implements HCNetSDK.FMSGCallBack_V31 {
    //报警信息回调函数
    public void invoke(int lCommand, HCNetSDK.NET_DVR_ALARMER pAlarmer, Pointer pAlarmInfo, int dwBufLen, Pointer pUser) {
        AlarmDataHandle(lCommand, pAlarmer, pAlarmInfo, dwBufLen, pUser);
        return;
    }
    public void AlarmDataHandle(int lCommand, HCNetSDK.NET_DVR_ALARMER pAlarmer, Pointer pAlarmInfo, int dwBufLen, Pointer pUser) {
        System.out.println("报警类型 lCommand:" + Integer.toHexString(lCommand));
        switch (lCommand) {
            case HCNetSDK.COMM_ALARM_ACS: //门禁主机报警信息
                HCNetSDK.NET_DVR_ACS_ALARM_INFO strACSInfo = new HCNetSDK.NET_DVR_ACS_ALARM_INFO();
                strACSInfo.write();
                Pointer pACSInfo = strACSInfo.getPointer();
                pACSInfo.write(0, pAlarmInfo.getByteArray(0, strACSInfo.size()), 0, strACSInfo.size());
                strACSInfo.read();
                /**门禁事件的详细信息解析，通过主次类型的可以判断当前的具体门禁类型，例如（主类型：0X5 次类型：0x4b 表示人脸认证通过，
                        主类型：0X5 次类型：0x4c 表示人脸认证失败）*/
                System.out.println("【门禁主机报警信息】卡号：" + new String(strACSInfo.struAcsEventInfo.byCardNo).trim() + "，卡类型：" +
                        strACSInfo.struAcsEventInfo.byCardType + "，报警主类型：" + Integer.toHexString(strACSInfo.dwMajor) + "，报警次类型：" + Integer.toHexString(strACSInfo.dwMinor));
                System.out.println("工号1："+strACSInfo.struAcsEventInfo.dwEmployeeNo);
                //温度信息（如果设备支持测温功能，人脸温度信息从NET_DVR_ACS_EVENT_INFO_EXTEND_V20结构体获取）
                if (strACSInfo.byAcsEventInfoExtendV20 == 1) {
                    HCNetSDK.NET_DVR_ACS_EVENT_INFO_EXTEND_V20 strAcsInfoExV20 = new HCNetSDK.NET_DVR_ACS_EVENT_INFO_EXTEND_V20();
                    strAcsInfoExV20.write();
                    Pointer pAcsInfoExV20 = strAcsInfoExV20.getPointer();
                    pAcsInfoExV20.write(0, strACSInfo.pAcsEventInfoExtendV20.getByteArray(0, strAcsInfoExV20.size()), 0, strAcsInfoExV20.size());
                    strAcsInfoExV20.read();
                    System.out.println("实时温度值：" + strAcsInfoExV20.fCurrTemperature);
                }
                //考勤状态
                if (strACSInfo.byAcsEventInfoExtend == 1) {
                    HCNetSDK.NET_DVR_ACS_EVENT_INFO_EXTEND strAcsInfoEx = new HCNetSDK.NET_DVR_ACS_EVENT_INFO_EXTEND();
                    strAcsInfoEx.write();
                    Pointer pAcsInfoEx = strAcsInfoEx.getPointer();
                    pAcsInfoEx.write(0, strACSInfo.pAcsEventInfoExtend.getByteArray(0, strAcsInfoEx.size()), 0, strAcsInfoEx.size());
                    strAcsInfoEx.read();
                    System.out.println("考勤状态：" + strAcsInfoEx.byAttendanceStatus);
                    System.out.println("工号2："+new String(strAcsInfoEx.byEmployeeNo).trim());
                }

                /**
                 * 报警时间
                 */
                String year=Integer.toString(strACSInfo.struTime.dwYear);
                String SwipeTime=year+strACSInfo.struTime.dwMonth+strACSInfo.struTime.dwDay+strACSInfo.struTime.dwHour+strACSInfo.struTime.dwMinute+
                        strACSInfo.struTime.dwSecond;
                if (strACSInfo.dwPicDataLen > 0) {
//                    SimpleDateFormat sf = new SimpleDateFormat("yyyyMMddHHmmss");
//                    String newName = sf.format(new Date());
                    FileOutputStream fout;
                    try {
                        /**
                         * 人脸保存路径
                         */
                        String filename = "..\\pic\\" + SwipeTime + "_ACS_Event_" + new String(strACSInfo.struAcsEventInfo.byCardNo).trim() + ".jpg";
                        fout = new FileOutputStream(filename);
                        //将字节写入文件
                        long offset = 0;
                        ByteBuffer buffers = strACSInfo.pPicData.getByteBuffer(offset, strACSInfo.dwPicDataLen);
                        byte[] bytes = new byte[strACSInfo.dwPicDataLen];
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
            case HCNetSDK.COMM_ID_INFO_ALARM: //身份证信息
                HCNetSDK.NET_DVR_ID_CARD_INFO_ALARM strIDCardInfo = new HCNetSDK.NET_DVR_ID_CARD_INFO_ALARM();
                strIDCardInfo.write();
                Pointer pIDCardInfo = strIDCardInfo.getPointer();
                pIDCardInfo.write(0, pAlarmInfo.getByteArray(0, strIDCardInfo.size()), 0, strIDCardInfo.size());
                strIDCardInfo.read();
                System.out.println("报警主类型：" + Integer.toHexString(strIDCardInfo.dwMajor) + "，报警次类型：" + Integer.toHexString(strIDCardInfo.dwMinor));
                /**
                 * 身份证信息
                 */
                String IDnum=new String(strIDCardInfo.struIDCardCfg.byIDNum).trim();
                System.out.println("【身份证信息】：身份证号码：" + IDnum+ "，姓名：" +
                        new String(strIDCardInfo.struIDCardCfg.byName).trim() + "，住址："+new String(strIDCardInfo.struIDCardCfg.byAddr));

                /**
                 * 报警时间
                 */
                String year1=Integer.toString(strIDCardInfo.struSwipeTime.wYear);
                String SwipeTime1=year1+strIDCardInfo.struSwipeTime.byMonth+strIDCardInfo.struSwipeTime.byDay+strIDCardInfo.struSwipeTime.byHour+strIDCardInfo.struSwipeTime.byMinute+
                        strIDCardInfo.struSwipeTime.bySecond;
                /**
                 * 保存图片
                 */
                //身份证图片
                if (strIDCardInfo.dwPicDataLen>0||strIDCardInfo.pPicData!=null)
                {
//                    SimpleDateFormat sf = new SimpleDateFormat("yyyyMMddHHmmss");
//                    String nowtime = sf.format(new Date());
                    FileOutputStream fout;
                    try {
                        String filename = "..\\pic\\" + SwipeTime1 + "_"+IDnum  + ".jpg";
                        fout = new FileOutputStream(filename);
                        //将字节写入文件
                        long offset = 0;
                        ByteBuffer buffers = strIDCardInfo.pPicData.getByteBuffer(offset, strIDCardInfo.dwPicDataLen);
                        byte[] bytes = new byte[strIDCardInfo.dwPicDataLen];
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

                if (strIDCardInfo.dwCapturePicDataLen>0||strIDCardInfo.pCapturePicData!=null)
                {
//                    SimpleDateFormat sf = new SimpleDateFormat("yyyyMMddHHmmss");
//                    String nowtime = sf.format(new Date());
                    FileOutputStream fout;
                    try {
                        /**
                         * 人脸图片保存路径
                         */
                        String filename = "..\\pic\\" + SwipeTime1 + "_CapturePic_" +".jpg";
                        fout = new FileOutputStream(filename);
                        //将字节写入文件
                        long offset = 0;
                        ByteBuffer buffers = strIDCardInfo.pCapturePicData.getByteBuffer(offset, strIDCardInfo.dwCapturePicDataLen);
                        byte[] bytes = new byte[strIDCardInfo.dwCapturePicDataLen];
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
                //门禁仅测温事件解析
            case HCNetSDK.COMM_VCA_ALARM:
                ByteBuffer dataBuffer = pAlarmInfo.getByteBuffer(0, dwBufLen);
                byte[] dataByte = new byte[dwBufLen];
                dataBuffer.rewind();
                dataBuffer.get(dataByte);
                //报警事件json报文
                System.out.println("仅测温报警事件："+new String(dataByte));
                break;
            default:
                System.out.println("报警类型" + Integer.toHexString(lCommand));
                break;
        }
    }
}
