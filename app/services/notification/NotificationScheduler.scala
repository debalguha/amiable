package services.notification

import config.AMIableConfig
import models.NotifySchedule
import org.quartz.CronScheduleBuilder.cronSchedule
import org.quartz.JobBuilder.newJob
import org.quartz.TriggerBuilder.newTrigger
import org.quartz.{JobKey, TriggerKey}
import org.quartz.impl.StdSchedulerFactory
import play.api.Logger

import scala.concurrent.ExecutionContext

object NotificationScheduler {
  private val notifyOwnersSchedule = "0 0 16 ? * FRI *"
  private val scheduler = StdSchedulerFactory.getDefaultScheduler

  def start(): Unit = scheduler.start()

  def shutdown(): Unit = scheduler.shutdown()

  def setupSchedule(mailClient: MailClient)(implicit config: AMIableConfig, ec: ExecutionContext): Unit = {
    scheduler.getContext.put("MailClient", mailClient)
    scheduler.getContext.put("AMIableConfig", config)
    scheduler.getContext.put("ExecutionContext", ec)
    val jobDetail = newJob(classOf[NotificationJob])
      .withIdentity(jobKey("notificationJob"))
      .build()
    val trigger = newTrigger()
      .withIdentity(triggerKey("notificationTrigger"))
      .withSchedule(cronSchedule(notifyOwnersSchedule))
      .build()
    scheduler.scheduleJob(jobDetail, trigger)
    Logger.info(s"Scheduled owner notification with schedule [$notifyOwnersSchedule]")
  }

  private def triggerKey(id: String): TriggerKey = new TriggerKey(id)
  private def jobKey(id: String): JobKey = new JobKey(id)
}
