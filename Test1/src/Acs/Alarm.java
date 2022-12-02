package Acs;

import NetSDKDemo.FMSGCallBack_V31;
import NetSDKDemo.HCNetSDK;
import com.sun.jna.Pointer;

import static Acs.AcsMain.fMSFCallBack_V31;

/**
 * 报警模块，实现功能：1、设备报警事件实时上传，报警事件包括刷脸、刷卡等认证事件和设备的操作事件
 * 2、事件主动获取（获取保存在设备上的事件）
 */
public final class Alarm {

    //报警布防 （布防和监听选其一）
    public static void SetAlarm(int userID) {
        if (AcsMain.lAlarmHandle < 0)//尚未布防,需要布防
        {

            //报警布防参数设置
            HCNetSDK.NET_DVR_SETUPALARM_PARAM m_strAlarmInfo = new HCNetSDK.NET_DVR_SETUPALARM_PARAM();
            m_strAlarmInfo.dwSize = m_strAlarmInfo.size();
            m_strAlarmInfo.byLevel = 1;  //布防等级
            m_strAlarmInfo.byAlarmInfoType = 1;   // 智能交通报警信息上传类型：0- 老报警信息（NET_DVR_PLATE_RESULT），1- 新报警信息(NET_ITS_PLATE_RESULT)
            m_strAlarmInfo.byDeployType = 1;   //布防类型 0：客户端布防 1：实时布防
            m_strAlarmInfo.write();
            AcsMain.lAlarmHandle = AcsMain.hCNetSDK.NET_DVR_SetupAlarmChan_V41(userID, m_strAlarmInfo);
            System.out.println("lAlarmHandle: " + AcsMain.lAlarmHandle);
            if (AcsMain.lAlarmHandle == -1) {
                System.out.println("布防失败，错误码为" + AcsMain.hCNetSDK.NET_DVR_GetLastError());
            } else {
                System.out.println("布防成功");
            }
        }
    }
    //报警监听（布防和监听选其一）
    public static void StartListen() {

        AcsMain.lListenHandle = AcsMain.hCNetSDK.NET_DVR_StartListen_V30("10.17.36.14", (short) 7201, fMSFCallBack_V31, null);
        if (AcsMain.lListenHandle == -1) {
            System.out.println("监听失败" + AcsMain.hCNetSDK.NET_DVR_GetLastError());
        } else {
            System.out.println("监听成功");
        }
    }
}
