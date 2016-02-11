package de.tuberlin.cit.freamon.yarnclient;


import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.yarn.api.records.ApplicationAttemptId;
import org.apache.hadoop.yarn.api.records.ApplicationReport;
import org.apache.hadoop.yarn.api.records.ContainerReport;
import org.apache.hadoop.yarn.api.records.YarnApplicationState;
import org.apache.hadoop.yarn.client.api.YarnClient;
import org.apache.hadoop.yarn.conf.YarnConfiguration;
import org.apache.hadoop.yarn.exceptions.YarnException;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;


//TODO: Timer einbauen

public class yarnClient {
    final YarnClient yarnClient;
    private ArrayList<Integer> runningApplications = new ArrayList<Integer>();

    public yarnClient() {
        this.yarnClient = YarnClient.createYarnClient();
        initYarnClient();
    }

    private void initYarnClient() {
        Configuration conf = new YarnConfiguration();
        yarnClient.init(conf);
        yarnClient.start();
    }

    private void evalApplications() {
        try {
            for (ApplicationReport applicationReport : yarnClient.getApplications()) {
                final YarnApplicationState applicationState = applicationReport.getYarnApplicationState();
                final int applicationId = applicationReport.getApplicationId().getId();
                if (applicationState == YarnApplicationState.RUNNING && !runningApplications.contains(applicationId)) {
                    final ApplicationAttemptId applicationAttemptId = applicationReport.getCurrentApplicationAttemptId();
                    final List<ContainerReport> containerReportList = yarnClient.getContainers(applicationAttemptId);
                    long containerIds[] = new long[containerReportList.size()];
                    for (int i = 0; i < containerIds.length; i++) {
                        containerIds[i] = containerReportList.get(i).getContainerId().getContainerId();
                    }
                    runningApplications.add(applicationId);
                    //todo: new application message to akka
                    System.out.println("Yarn Client: New Application with ID: " + applicationId + " on ContainerIDs: " + containerIds);

                } else if (applicationState != YarnApplicationState.RUNNING && runningApplications.contains(applicationId)) {
                    runningApplications.remove((Object) applicationId);
                    //todo: finished application message to akka
                    System.out.println("Yarn Client: Finished Application with ID" + applicationId);
                }
            }

        } catch (IOException e) {
            System.err.println("IOException: " + e.getMessage());

        } catch (YarnException e) {
            System.err.println("YarnException: " + e.getMessage());
        }
    }

    public static void main(String[] args) {
        new yarnClient().evalApplications();

    }


}
