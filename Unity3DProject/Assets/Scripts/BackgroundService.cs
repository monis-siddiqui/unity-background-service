using System;
using System.Timers;
using System.Collections;
using System.Collections.Generic;
using TMPro;
using UnityEngine;
using UnityEngine.UI;

public class BackgroundService : MonoBehaviour
{
    AndroidJavaClass unityClass;
    AndroidJavaObject unityActivity;
    AndroidJavaClass customClass;    

    private void Awake()
    {
        SendActivityReference("com.kdg.toast.plugin.Bridge");
    }

    void Start() {
        StartService();
    }

    void SendActivityReference(string packageName)
    {
        unityClass = new AndroidJavaClass("com.unity3d.player.UnityPlayer");
        unityActivity = unityClass.GetStatic<AndroidJavaObject>("currentActivity");
        customClass = new AndroidJavaClass(packageName);
        customClass.CallStatic("receiveActivityInstance", unityActivity);
    }

    public void StartService()
    {
        customClass.CallStatic("StartCheckerService");
    }
    public void StopService()
    {
        customClass.CallStatic("StopCheckerService");
    }

    public int GetStepCount(DateTime start, DateTime end) {            //This function gives you steps recorded from and to specific date
        object[] args = new System.Object[2];
        
        args[0] = start.ToString("yyyy-MM-dd");
        args[1] = end.ToString("yyyy-MM-dd");
        int? stepsCount = customClass.CallStatic<int>("getStepCountData",args);
        return (int)stepsCount;
    }

    //E.G: String sql ="Select * from StepCount where recordedOn BETWEEN \''"+startDate+"\' and \'"+endDate+"\'";
    public int GetStepCount(String query)
    {            //This function gives you steps recorded from and to specific date
        int? stepsCount = customClass.CallStatic<int>("getStepCountData", query);
        return (int)stepsCount;
    }
}