package services.notification

import org.quartz.{Job, JobExecutionContext}

/** Quartz job wrapper for [[ScheduledNotificationRunner]] */
class NotificationJob extends Job {

  override def execute(context: JobExecutionContext): Unit = {
    ScheduledNotificationRunner.run()
  }
}
