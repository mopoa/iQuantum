package org.iquantum.policies.qtasks;

import java.util.*;
import org.iquantum.backends.quantum.QNode;
import org.iquantum.backends.quantum.QNodeMQ;
import org.iquantum.core.iQuantum;
import org.iquantum.policies.qubitMapping.QubitMappingBackTracking;
import org.iquantum.tasks.QTask;
import org.iquantum.tasks.ResQTask;
import org.iquantum.utils.DataFormat;

public class QTaskSchedulerSJNMQ extends QTaskScheduler {
    protected int currentQPUs = 1;

    public QTaskSchedulerSJNMQ() {
    }

    public Map<String, String> qtaskMapping(QTask QTask, QNode qNode) {
        return QubitMappingBackTracking.findMapping(qNode.getQubitTopology(), QTask.getQubitTopology());
    }

    public Map<String, String> qtaskMapping(QTask QTask, QNodeMQ qNode) {
        return QubitMappingBackTracking.findMapping(qNode.getQPUList().getQubitTopologyOfQPUById(0), QTask.getQubitTopology());
    }

    public double updateQNodeProcessing(double currentTime, double clops) {
        this.setCurrentClops(clops);
        double timeSpam = currentTime - this.getPreviousTime();

        Iterator<ResQTask> iterator = this.getQTaskExecList().iterator();
        while (iterator.hasNext()) {
            ResQTask rql = iterator.next();
            rql.updateQTaskFinishedSoFar((long) (this.getCurrentClops() * timeSpam));
        }

        if (this.getQTaskExecList().isEmpty()) {
            this.setPreviousTime(currentTime);
            return 0.0;
        } else {
            List<ResQTask> toRemove = new ArrayList<>();
            Iterator<ResQTask> execIterator = this.getQTaskExecList().iterator();

            while (execIterator.hasNext()) {
                ResQTask rql = execIterator.next();
                if (rql.getRemainingQTaskLength() == 0L) {
                    toRemove.add(rql);
                    this.qtaskFinish(rql);
                }
            }

            this.getQTaskExecList().removeAll(toRemove);

            if (!this.getQTaskWaitingList().isEmpty()) {
                toRemove.clear();
                this.getQTaskWaitingList().sort(Comparator.comparingLong(ResQTask::getRemainingQTaskLength));

                for (int i = 0; i < toRemove.size() && !this.getQTaskWaitingList().isEmpty(); i++) {
                    ResQTask shortestTask = this.getQTaskWaitingList().remove(0);
                    shortestTask.setQTaskStatus(3);
                    this.getQTaskExecList().add(shortestTask);
                }
            }

            double nextEvent = Double.MAX_VALUE;
            for (ResQTask rql : this.getQTaskExecList()) {
                double estimatedFinishTime = currentTime + (double) rql.getRemainingQTaskLength() / this.getCurrentClops();
                estimatedFinishTime = DataFormat.roundDouble(estimatedFinishTime, 2);
                if (estimatedFinishTime - currentTime < iQuantum.getMinTimeBetweenEvents()) {
                    estimatedFinishTime = currentTime + iQuantum.getMinTimeBetweenEvents();
                }

                if (estimatedFinishTime < nextEvent) {
                    nextEvent = estimatedFinishTime;
                }
            }

            this.setPreviousTime(currentTime);
            return nextEvent;
        }
    }

    protected double getCapacity(double clops) {
        return this.getQTaskExecList().isEmpty() ? clops : 0.0;
    }

    public double qtaskSubmit(QTask QTask, double fileTransferTime) {
        ResQTask rql = new ResQTask(QTask);

        if (this.getQTaskExecList().size() < 1) {
            rql.setQTaskStatus(3);
            this.getQTaskExecList().add(rql);
            double estimatedCompletionTime = (double) QTask.getNumLayers() / this.getCurrentClops() * QTask.getNumShots();
            return estimatedCompletionTime;
        } else {
            rql.setQTaskStatus(2);
            this.getQTaskWaitingList().add(rql);
            this.getQTaskWaitingList().sort(Comparator.comparingLong(ResQTask::getRemainingQTaskLength));
            return 0.0;
        }
    }

    public double qtaskSubmit(QTask QTask) {
        return this.qtaskSubmit(QTask, 0.0);
    }

    public QTask qtaskCancel(int qtaskId) {
        return null; // Implement cancellation logic if required
    }

    public boolean qtaskPause(int qtaskId) {
        return false;
    }

    public double qtaskResume(int qtaskId) {
        return 0.0;
    }
}

