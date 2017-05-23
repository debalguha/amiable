import com.amazonaws.services.simpleemail.AmazonSimpleEmailServiceAsync
import com.google.inject.AbstractModule
import services.notification.{AWSMailClient, AmazonSimpleEmailServiceAsyncFactory, Notifications}
import services.{Agents, Metrics}


/**
  * This class is a Guice module that tells Guice how to bind several
  * different types. This Guice module is created when the Play
  * application starts.

  * Play will automatically use any class called `Module` that is in
  * the root package. You can create modules in other locations by
  * adding `play.modules.enabled` settings to the `application.conf`
  * configuration file.
  */
class Module extends AbstractModule {
  override def configure() = {
    bind(classOf[Agents]).asEagerSingleton()
    bind(classOf[Metrics]).asEagerSingleton()
    bind(classOf[AmazonSimpleEmailServiceAsync]).toProvider(classOf[AmazonSimpleEmailServiceAsyncFactory])
    bind(classOf[AWSMailClient]).asEagerSingleton()
    bind(classOf[Notifications]).asEagerSingleton()
  }
}
