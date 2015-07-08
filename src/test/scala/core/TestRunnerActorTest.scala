package core

import akka.actor._
import akka.testkit._
import org.specs2.mutable._
import org.specs2.time.NoTimeConversions

/* A tiny class that can be used as a Specs2 'context'. */
abstract class AkkaTestkitSpecs2Support extends TestKit(ActorSystem()) with After with ImplicitSender {
  // make sure we shut down the actor system after all tests have run
  def after = system.shutdown()
}

class TestRunnerActorTest extends Specification with NoTimeConversions {
  // sequential

  "A TestRunnerActor" should {
    "execute commands in order" in new AkkaTestkitSpecs2Support {
      val yamlStr =
        """
        before_jobs:
          - true 1
          - true 2

        jobs:
          - name: job1
            metric: ignore
            script:
              - true 3
          - name: job2
            metric: ignore
            before_script:
              - true 4
            script:
              - true 5
            after_script:
              - true 6

        after_jobs:
          - true 7
        """.stripMargin

      val actor = system.actorOf(Props(new TestRunnerActor))
      actor ! Run(yamlStr)

      expectMsg(CommandExecuted("true 1"))
      expectMsg(CommandExecuted("true 2"))
      expectMsg(CommandExecuted("true 3"))
      expectMsg(CommandExecuted("true 4"))
      expectMsg(CommandExecuted("true 5"))
      expectMsg(CommandExecuted("true 6"))
      expectMsg(CommandExecuted("true 7"))
    }

    "fail on empty yaml" in new AkkaTestkitSpecs2Support {
      val actor = system.actorOf(Props(new TestRunnerActor))
      actor ! Run("")

      expectMsg(TestError(BadConfiguration(Seq("YamlObject expected in field 'jobs'"))))
    }

    "fail on invalid metrics" in new AkkaTestkitSpecs2Support {
      val actor = system.actorOf(Props(new TestRunnerActor))
      actor ! Run(
        """
          jobs:
            - name: job1
              metric: bad
              script:
                - true
        """.stripMargin)

      expectMsg(TestError(BadConfiguration(Seq("Unknown metric bad in job1"))))
    }

    "fail on 2 jobs with unknown metrics" in new AkkaTestkitSpecs2Support {
      val actor = system.actorOf(Props(new TestRunnerActor))
      actor ! Run(
        """
          jobs:
            - name: job1
              metric: bad1
              script:
                - true
            - name: job2
              metric: bad2
              script:
                - true
        """.stripMargin)

      expectMsg(TestError(BadConfiguration(Seq(
        "Unknown metric bad1 in job1",
        "Unknown metric bad2 in job2"))))
    }

    "fail on missing required job name" in new AkkaTestkitSpecs2Support {
      val actor = system.actorOf(Props(new TestRunnerActor))
      actor ! Run(
        """
          jobs:
            - script:
              - true
        """.stripMargin)

      expectMsg(TestError(BadConfiguration(Seq("YamlObject is missing required member 'name'"))))
    }

    "fail on missing required job metric" in new AkkaTestkitSpecs2Support {
      val actor = system.actorOf(Props(new TestRunnerActor))
      actor ! Run(
        """
          jobs:
            - name: job1
              script:
                - true
        """.stripMargin)

      expectMsg(TestError(BadConfiguration(Seq("YamlObject is missing required member 'metric'"))))
    }

    "fail on missing jobs" in new AkkaTestkitSpecs2Support {
      val actor = system.actorOf(Props(new TestRunnerActor))
      actor ! Run(
        """
          before_jobs:
            - true
        """.stripMargin)

      expectMsg(TestError(BadConfiguration(Seq("Test has no jobs"))))
    }

    "fail on garbish yaml" in new AkkaTestkitSpecs2Support {
      val actor = system.actorOf(Props(new TestRunnerActor))
      actor ! Run("garbishblala")

      expectMsgClass(classOf[TestError])
    }

    "tears down correctly" in new AkkaTestkitSpecs2Support {
      val actor = system.actorOf(Props(new TestRunnerActor))
      actor ! Run(
        """
          before_jobs:
            - mkdir ttt
          jobs:
            - name: job1
              metric: ignore
              script:
                - true
          after_jobs:
            - rmdir ttt
        """.stripMargin)

      expectMsg(CommandExecuted("mkdir ttt"))
      expectMsg(CommandExecuted("true"))
      expectMsg(CommandExecuted("rmdir ttt"))
      expectMsg(Finished)
    }
  }
}
