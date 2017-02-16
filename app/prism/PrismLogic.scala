package prism

import models._
import org.joda.time.DateTime
import utils.{DateUtils, Percentiles}

object PrismLogic {
  def oldInstances(instanceAmis: List[(Instance, Option[AMI])]): List[Instance] = {
    instanceAmis
      .filter { case (_, amiOpt) => amiOpt.exists(amiIsOld) }
      .map(_._1)
  }

  def stacks(instances: List[Instance]): List[String] = {
    (for {
      instance <- instances
      stack <- instance.stack
    } yield stack).distinct
  }

  def amiArns(instances: List[Instance]): List[String] =
    instances.flatMap(_.amiArn).distinct

  /**
    * Associates instances with their AMI
    */
  def instanceAmis(instances: List[Instance], amis: List[AMI]): List[(Instance, Option[AMI])] = {
    instances.map { instance =>
      instance -> instance.amiArn.flatMap { amiArn =>
        amis.find(_.arn == amiArn)
      }
    }
  }

  /**
    * Associates AMIs with instances that use them
    */
  def amiInstances(amis: List[AMI], instances: List[Instance]): List[AMI] = {
    amis.map { ami =>
      val amiInstances = instances.filter { instance =>
        instance.amiArn.fold(false)(_ == ami.arn)
      }
      ami.copy(instances = Some(amiInstances))
    }
  }

  /**
    * @return all SSA for a given list of instances
    */
  def instanceSSAs(instances: List[Instance]): List[SSA] = {
    val allInstanceSSAs = for {
      instance <- instances
      ssa <- {
        if (instance.app.isEmpty) List(SSA(instance.stack, instance.stage, None))
        else instance.app.map(app => SSA(instance.stack, instance.stage, Some(app)))
      }
    } yield ssa
    allInstanceSSAs.distinct
  }

  /**
    * All SSAs associated with the instances of the given AMIs
    */
  def allSSAs(amis: List[AMI]): Map[SSA, List[AMI]] = {
    val allSSACombos = for {
      ami <- amis
      ssa <- instanceSSAs(ami.instances.getOrElse(Nil))
    } yield ssa -> ami

    allSSACombos
      .groupBy { case (ssa, _) => ssa }
      .map { case (ssa, ssaAmis) =>
        ssa -> ssaAmis.map { case (_, t) => t }
      }
  }

  /**
    * SSAs are sorted by their oldest AMI, except for the empty SSA which
    * always appears last.
    */
  def allSSAsSortedByAge(allAmis: List[AMI]): List[(SSA, List[AMI])] = {
    allSSAs(allAmis).toList.sortBy { case (ssa, amis) =>
      if (ssa.isEmpty) {
        // put empty SSA last
        DateTime.now.getMillis
      } else {
        val creationDates = for {
          ami <- amis
          creationDate <- ami.creationDate
        } yield creationDate.getMillis
        creationDates.headOption.getOrElse(0L)
      }
    }
  }

  def amiIsOld(ami: AMI): Boolean = {
    ami.creationDate.flatMap { creationDate =>
      DateUtils.getAge(creationDate).map {
        case Fresh | Turning => false
        case _ => true
      }
    }.getOrElse(true)
  }

  /**
    * @return Percentiles of instance AMIs age
    */
  def instancesAmisAgePercentiles(amis: List[AMI]): Percentiles = {
    val ages = amis.flatMap { ami =>
      val amiAge = ami.creationDate.map(DateUtils.daysAgo).getOrElse(0)
      List.fill(ami.instances.getOrElse(Nil).length)(amiAge)
    }
    Percentiles(ages)
  }

  /**
    * @return the number of instances from the list of AMIs that match a given SSA
    */
  def instancesCountForSSA(amis: List[AMI], ssa: SSA): Int = {
    amis
      .flatMap(_.instances.getOrElse(Nil))
      .filter(i => i.stack == ssa.stack && i.stage == ssa.stage && ssa.app.exists(i.app.contains))
      .length
  }

}
