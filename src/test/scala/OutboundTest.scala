import java.util.Locale

import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import akka.stream.scaladsl.Sink
import esl.OutboundServer
import esl.domain.EventNames.All
import esl.domain._
import org.apache.logging.log4j.scala.Logging

import scala.concurrent.Future
import scala.concurrent.duration.{Duration, MILLISECONDS, SECONDS}
import scala.util.{Failure, Success}

/**
  * Created by abdhesh on 28/06/17.
  */


object OutboundTest extends App with Logging {

  implicit val system = ActorSystem("esl-system")
  implicit val actorMaterializer = ActorMaterializer()
  implicit val ec = system.dispatcher
  implicit val timeout = Duration(2, SECONDS)

  OutboundServer("127.0.0.1", 8084).startWith(
    fsConnection => {
      fsConnection.onComplete {
        case Success(conn) =>
          conn.subscribeEvents(EventNames.All).foreach {
            _ =>
              conn.play("/usr/share/freeswitch/sounds/en/us/callie/conference/8000/conf-pin.wav").foreach {
                commandResponse =>
                  commandResponse.commandReply.foreach(f => logger.info(s"Got command reply: ${f}"))
                  commandResponse.executeEvent.foreach(f => logger.info(s"Got ChannelExecute event: ${f}"))
                  commandResponse.executeComplete.foreach(f => logger.info(s"ChannelExecuteComplete event: ${f}"))
              }
          }
        case Failure(ex) => logger.info("failed to make outbound socket connection", ex)
      }
      Sink.foreach[List[FSMessage]] { fsMessages => logger.info(fsMessages) }
    },
    result => result onComplete {
      case resultTry => logger.info(s"Connection has closed with result${resultTry}")
    }
  ).onComplete {
    case result => logger.info(s"Stream completed with result ${result}")
  }
}