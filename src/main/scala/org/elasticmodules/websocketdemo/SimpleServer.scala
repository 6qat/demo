/*
 * Copyright 2014 Elastic Modules Team
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.elasticmodules.websocketdemo

import akka.actor._
import akka.io.IO
import spray.can.Http
import spray.can.server.UHttp
import spray.can.websocket
import spray.can.websocket.frame.{BinaryFrame, TextFrame}
import spray.can.websocket.FrameCommandFailed
import spray.json._
import spray.routing.HttpServiceActor
import scala.io.Source

case class Sort(property: String, direction: String)
case class Data(page: Int, limit: Int, start: Int, sort: Option[Sort])
case class User(name: String, age: Int, id: Option[String])

trait Request
case class WsCreate(data: List[User]) extends Request
case class WsRead(data: Data) extends Request
case class WsUpdate(data: List[User]) extends Request
case class WsDelete(data: List[User]) extends Request

// !!! IMPORTANT, else `convertTo` and `toJson` won't work correctly
import DefaultJsonProtocol._


//{"event":"read","data":{"page":1,"limit":25,"start":0}}
object RequestJsonProtocol extends DefaultJsonProtocol {

  implicit object RequestJsonFormat extends RootJsonFormat[Request] {
    // this isn't very DRY!!
    override def write(obj: Request): JsValue = obj match {
      case p@(_: WsCreate) =>
        JsObject(
          "event" -> JsString("create"),
          "data" -> p.data.toJson
        )
      case p@(_: WsRead) =>
        JsObject(
          "event" -> JsString("read"),
          "data" -> p.data.toJson
        )
      case p@(_: WsUpdate) =>
        JsObject(
          "event" -> JsString("update"),
          "data" -> p.data.toJson
        )
      case p@(_: WsDelete) =>
        JsObject(
          "event" -> JsString("delete"),
          "data" -> p.data.toJson
        )
    }

    def read(value: JsValue) = value match {
      case JsObject(fields) =>
        (fields("event"), fields("data")) match {
          case (JsString("create"), element) =>
            WsCreate(element.convertTo[List[User]])
          case (JsString("read"), element) =>
            WsRead(element.convertTo[Data])
          case (JsString("update"), element) =>
            WsUpdate(element.convertTo[List[User]])
          case (JsString("delete"), element) =>
            WsDelete(element.convertTo[List[User]])
          case _ => throw new DeserializationException("Unhandled Request")
        }
      case _ => throw new DeserializationException("Request Expected")
    }
  }

  implicit val sortFormat : JsonFormat[Sort] = jsonFormat2(Sort)
  implicit val dataFormat : JsonFormat[Data] = jsonFormat4(Data)
  implicit val userFormat : JsonFormat[User] = jsonFormat3(User)
}

object SimpleServer extends App with MySslConfiguration {
  var users = Map[String, User]()

  object WebSocketServer {
    def props() = Props(classOf[WebSocketServer])
  }

  class WebSocketServer extends Actor with ActorLogging {
    def receive = {
      // when a new connection comes in we register a WebSocketConnection actor as the per connection handler
      case Http.Connected(remoteAddress, localAddress) =>
        val serverConnection = sender()
        val conn = context.actorOf(WebSocketWorker.props(serverConnection))
        serverConnection ! Http.Register(conn)
    }
  }

  object WebSocketWorker {
    def props(serverConnection: ActorRef) = Props(classOf[WebSocketWorker], serverConnection)
  }

  class WebSocketWorker(val serverConnection: ActorRef) extends HttpServiceActor with websocket.WebSocketServerConnection {
//    import RequestJsonProtocol._
//    import DefaultJsonProtocol._

    override def receive = handshaking orElse businessLogicNoUpgrade orElse closeLogic

    def businessLogic: Receive = {
      case x@(_: TextFrame) =>
        import RequestJsonProtocol._
        val obj = x.payload.utf8String.parseJson.convertTo[Request]
        self ! obj

      case x: WsCreate =>
        log.debug("Got WsCreate: {}", x)
      case x: WsRead =>
        log.debug("Got WsRead: {}", x)
        import RequestJsonProtocol._
        send(TextFrame(users.values.toList.toJson.compactPrint))
      case x: WsUpdate =>
        log.debug("Got WsUpdate: {}", x)
      case x: WsDelete =>
        log.debug("Got WsDelete: {}", x)

      case UHttp.Upgraded =>
        self ! WsRead(Data(1, 25, 0, None))
      case x: FrameCommandFailed =>
        log.error("frame command failed", x)

    }

    def businessLogicNoUpgrade: Receive = {
      implicit val refFactory: ActorRefFactory = context
      runRoute {
        getFromResourceDirectory("webapp")
      }
    }
  }

  def doMain() {
    import RequestJsonProtocol.userFormat
    import DefaultJsonProtocol._

    val res = this.getClass.getClassLoader.getResourceAsStream("Users.json")
    users = Source.fromInputStream(res).mkString.parseJson.convertTo[List[User]].map(
      u => (u.id.get, u)
    ).toMap

    implicit val system = ActorSystem()

    val server = system.actorOf(WebSocketServer.props(), "websocket")

    IO(UHttp) ! Http.Bind(server, "localhost", 8080)

    readLine("Hit ENTER to exit ...\n")
    system.shutdown()
    system.awaitTermination()
  }

  // because otherwise we get an ambiguous implicit if doMain is inlined
  doMain()
}



