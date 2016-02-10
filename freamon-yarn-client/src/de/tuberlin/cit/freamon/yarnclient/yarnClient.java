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


public class yarnClient {
    public static void main(String[] args) throws IOException, YarnException {

        System.out.println("start");

        Configuration conf = new YarnConfiguration();
        YarnClient yarnClient = YarnClient.createYarnClient();

        yarnClient.init(conf);
        yarnClient.start();
        ArrayList<Integer> runningApplications = new ArrayList<Integer>();

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
                System.out.println("new App" + applicationId);

            } else if (applicationState != YarnApplicationState.RUNNING && runningApplications.contains(applicationId)) {
                runningApplications.remove((Object) applicationId);
                //todo: finished application message to akka


        }
    }
}


}
