package de.tuberlin.cit.freamon.yarnclient;


import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.yarn.api.records.*;
import org.apache.hadoop.yarn.client.api.YarnClient;
import org.apache.hadoop.yarn.conf.YarnConfiguration;
import org.apache.hadoop.yarn.exceptions.YarnException;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;


public class yarnClient {
    YarnClient yarnClient;
    private ArrayList<ApplicationId> runningApplications = new ArrayList<ApplicationId>();

    public yarnClient() {
        initYarnClient();
    }

    private void initYarnClient() {
        Configuration conf = new YarnConfiguration();
        //TODO fixed host name! We could parse this from yarn-site.xml in the future?
        conf.set("yarn.resourcemanager.hostname", "wally089.cit.tu-berlin.de");
        yarnClient = YarnClient.createYarnClient();
        yarnClient.init(conf);
        yarnClient.start();
    }

    public ApplicationId evalYarnForNewApp() {
        try {
            for (ApplicationReport applicationReport : yarnClient.getApplications()) {
                final YarnApplicationState applicationState = applicationReport.getYarnApplicationState();
                final ApplicationId applicationId = applicationReport.getApplicationId();
                int runningContainers = applicationReport.getApplicationResourceUsageReport().getNumUsedContainers();
                int resContainers = applicationReport.getApplicationResourceUsageReport().getNumReservedContainers();
                applicationReport.getApplicationResourceUsageReport().getReservedResources().getVirtualCores();
                //TODO runningContainers>1 is not a good solution
                if (applicationState == YarnApplicationState.RUNNING && !runningApplications.contains(applicationId) && runningContainers > 1) {
                    runningApplications.add(applicationId);
                    System.out.println("Yarn Client: New Application with ID: " + applicationId);
                    return applicationId;
                }
            }
        } catch (IOException e) {
            System.err.println("IOException: " + e.getMessage());

        } catch (YarnException e) {
            System.err.println("YarnException: " + e.getMessage());
        }
        return null;
    }

    public ApplicationId evalYarnForReleasedApp() {
        try {
            for (ApplicationReport applicationReport : yarnClient.getApplications()) {
                final YarnApplicationState applicationState = applicationReport.getYarnApplicationState();
                final ApplicationId applicationId = applicationReport.getApplicationId();
                if (applicationState != YarnApplicationState.RUNNING && runningApplications.contains(applicationId)) {
                    runningApplications.remove((Object) applicationId);
                    System.out.println("Yarn Client: Finished Application with ID " + applicationId);
                    return applicationId;
                }
            }
        } catch (IOException e) {
            System.err.println("IOException: " + e.getMessage());

        } catch (YarnException e) {
            System.err.println("YarnException: " + e.getMessage());
        }
        return null;
    }

    public long[] getApplicationContainerIds(ApplicationId applicationId) {

        try {
            //TODO assume first application attempt always works
            List<ApplicationAttemptReport> applicationAttemptReports = yarnClient.getApplicationAttempts(applicationId);
            if (applicationAttemptReports.size() == 1) {
                ApplicationAttemptId applicationAttemptid = applicationAttemptReports.get(0).getApplicationAttemptId();
                final List<ContainerReport> containerReportList = yarnClient.getContainers(applicationAttemptid);
                long containerIds[] = new long[containerReportList.size()];
                System.out.print("Yarn Client: Found " + containerIds.length + " with IDs: (");
                for (int i = 0; i < containerIds.length; i++) {
                    //TODO Covers also the AM
                    //containerIds[i] = "container_" + applicationId.getClusterTimestamp() + "_" + String.format("%04d", applicationId.getId()) + "_01_" + String.format("%06d", containerReportList.get(i).getContainerId().getContainerId());
                    containerIds[i] = containerReportList.get(i).getContainerId().getContainerId();
                    System.out.print(containerIds[i] + ", ");
                }
                System.out.println(")");

                return containerIds;
            } else {
                System.err.println("Yarn Client: ERROR - could not fetch containerIds, because of too many application attempts ");
            }
        } catch (IOException e) {
            System.err.println("IOException: " + e.getMessage());

        } catch (YarnException e) {
            System.err.println("YarnException: " + e.getMessage());
        }
        return null;
    }

    //todo implement this into scala MonitorAgent
    private void startPolling(int intervalInSec) {
        System.out.println("Yarn Client: Start Polling for applications every: " + intervalInSec + " second(s)");
        Timer timer = new Timer();
        timer.schedule(new TimerTask() {
            public void run() {
                ApplicationId x = evalYarnForNewApp();
                if (x != null)
                    getApplicationContainerIds(x);
                evalYarnForReleasedApp();
            }
        }, 0, 1000 * intervalInSec);
    }

    public static void main(String[] args) {
        new yarnClient().startPolling(1);
    }
}