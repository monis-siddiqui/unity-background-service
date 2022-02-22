using System;
using System.Collections;
using System.Collections.Generic;
using UnityEngine;
using UnityEngine.UI;

public class UIHandler : MonoBehaviour
{
    [SerializeField]
    Text[] daysText;

    public BackgroundService backgroundService;
    // Start is called before the first frame update

    public void getCount() {
        getStepCountforDays(daysText.Length);
    }

    public void startService() {
        backgroundService.StartService();
    }

    public void stopService() {
        backgroundService.StopService();
    }


    //public int[] getStepCountforDays(int days)
    //{
    //    Debug.Log("Calling");
    //    int[] daysArray = new int[days];
    //    for (int i = 0; i < days; i++)
    //    {
    //        DateTime start;
    //        DateTime end;

    //        end = DateTime.Today.AddDays(i*-1).AddDays(1);
    //        Debug.Log("Added");
    //        int sub = -1 * (i + 1);
    //        Debug.Log("Added"+sub);
    //        start = DateTime.Today.AddDays(sub);
    //        Debug.Log("Added");
    //        daysArray[i] = backgroundService.GetStepCount(start, end);
    //        //daysArray[i] = backgroundService.GetStepCount("Select * from StepCount WHERE recordedOn BETWEEN datetime('2021-12-12') AND datetime('2021-12-14')");
    //        daysText[i].text = daysArray[i].ToString();
    //    }
    //    return daysArray;
    //}

    public int[] getStepCountforDays(int days)
    {
        Debug.Log("Calling");
        int[] daysArray = new int[days];
        for (int i = 0; i < days; i++)
        {
            DateTime date = DateTime.Now.Date;
            date = date.AddDays(-1*(i));
            Debug.Log(date.ToString("yyyy-MM-dd"));
            daysArray[i] = backgroundService.GetStepCount("Select * from StepCount WHERE recordedOn = date('" + date.ToString("yyyy-MM-dd") + "')");
            daysText[i].text = daysArray[i].ToString();
        }
        return daysArray;
    }



}
